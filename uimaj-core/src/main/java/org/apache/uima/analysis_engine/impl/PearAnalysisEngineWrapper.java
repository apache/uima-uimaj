/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.uima.analysis_engine.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.impl.ChildUimaContext_impl;
import org.apache.uima.pear.tools.PackageBrowser;
import org.apache.uima.resource.PearSpecifier;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceManagerPearWrapper;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.XMLInputSource;

/**
 * UIMA pear runtime analysis engine wrapper. With this wrapper implementation
 * it is possible to run installed pear files out of the box in UIMA.
 * 
 */
public class PearAnalysisEngineWrapper extends AnalysisEngineImplBase {

   // a hash map where the entries will be reclaimed when the keys are no longer
   // referenced by anything (other than this hash map)
   // key = resourceManager instance associated with this call
   // value = map <String_Pair, ResourceManager>
   // value = resourceManager instance created by this class
  
   // The reason we do this: For cases involving Cas Pools and multiple
   //  threads, we want to share the resource manager - otherwise
   //  there could be multiple instances of the classes for this pear
   //  loaded.
   // The map is a double map.  The first one maps between the
   // incoming Resource Manager, and a second map.
   // The second map (allows for multiple Pears in a pipeline)
   // maps (for the given incoming Resource Manager), using a key
   // consisting of the "class path" and "data path", the 
   // Resource Manager for that combination.

   static private Map<ResourceManager, Map<StringPair, ResourceManager>> cachedResourceManagers = Collections
         .synchronizedMap(new WeakHashMap<ResourceManager, Map<StringPair, ResourceManager>>(4));

   private AnalysisEngine ae = null;

   private Map<StringPair, ResourceManager> createRMmap(StringPair sp, ResourceManager rm) {
      Map<StringPair, ResourceManager> result = new HashMap<StringPair, ResourceManager>(4);
      result.put(sp, rm);
      UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG,
            this.getClass().getName(), "createRMmap", LOG_RESOURCE_BUNDLE,
            "UIMA_pear_runtime_create_RM_map",
            new Object[] { sp.classPath, sp.dataPath });
      return result;
   }

   private ResourceManager createRM(StringPair sp, PackageBrowser pkgBrowser, ResourceManager parentResourceManager)
         throws MalformedURLException {
      // create UIMA resource manager and apply pear settings
//      ResourceManager rsrcMgr = UIMAFramework.newDefaultResourceManager();
     ResourceManager rsrcMgr;
     if (null == parentResourceManager) {
       // could be null for top level Pear not in an aggregate
       rsrcMgr = UIMAFramework.newDefaultResourceManager();
     } else {
       rsrcMgr = UIMAFramework.newDefaultResourceManagerPearWrapper();
       ((ResourceManagerPearWrapper)rsrcMgr).initializeFromParentResourceManager(parentResourceManager);
     }
     rsrcMgr.setExtensionClassPath(sp.classPath, true);
     UIMAFramework.getLogger(this.getClass()).logrb(
            Level.CONFIG,
            this.getClass().getName(),
            "createRM",
            LOG_RESOURCE_BUNDLE,
            "UIMA_pear_runtime_set_classpath__CONFIG",
            new Object[] { sp.classPath,
                  pkgBrowser.getRootDirectory().getName() });

      // get and set uima.datapath if specified
      if (sp.dataPath != null) {
         rsrcMgr.setDataPath(sp.dataPath);
         UIMAFramework.getLogger(this.getClass()).logrb(
               Level.CONFIG,
               this.getClass().getName(),
               "createRM",
               LOG_RESOURCE_BUNDLE,
               "UIMA_pear_runtime_set_datapath__CONFIG",
               new Object[] { sp.dataPath,
                     pkgBrowser.getRootDirectory().getName() });
      }
      return rsrcMgr;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase#initialize(org.apache.uima.resource.ResourceSpecifier,
    *      java.util.Map)
    *      
    * (Nov 2008) initialize is called as a normal part of produceResource.  
    * There are 2 cases: 
    *   1) The Pear is the top level component
    *   2) The Pear is contained in an aggregate.
    *   
    *   In Case (1), the aAdditionalParams passed in does *not* contain a UIMA_CONTEXT
    *   In Case (2), the aAdditionalParams passed in contains a child UIMA_CONTEXT 
    *     created for this component.
    */
   public boolean initialize(ResourceSpecifier aSpecifier, Map aAdditionalParams)
         throws ResourceInitializationException {

      // aSpecifier must be a pearSpecifier
      if (!(aSpecifier instanceof PearSpecifier)) {
         return false;
      }

      // cast resource specifier to a pear specifier
      PearSpecifier pearSpec = (PearSpecifier) aSpecifier;

      // get pear path
      String pearRootDirPath = pearSpec.getPearPath();

      try {
         // get installed pear root directory - specified as URI of the
         // descriptor
         File pearRootDir = new File(pearRootDirPath);

         // create pear package browser to get the pear meta data
         PackageBrowser pkgBrowser = new PackageBrowser(pearRootDir);

         // get pear env variables and set them as system properties
         Properties props = pkgBrowser.getComponentEnvVars();
         Iterator keyIterator = props.keySet().iterator();
         Properties systemProps = System.getProperties();
         while (keyIterator.hasNext()) {
            String key = (String) keyIterator.next();
            String value = (String) props.get(key);

            // log warning if system property already exist and does not have
            // the same value
            if (systemProps.containsKey(key)) {
               String systemPropValue = (String) systemProps.get(key);
               if (!systemPropValue.equals(value)) {
                  UIMAFramework.getLogger(this.getClass()).logrb(
                        Level.WARNING,
                        this.getClass().getName(),
                        "initialize",
                        LOG_RESOURCE_BUNDLE,
                        "UIMA_pear_runtime_system_var_already_set__WARNING",
                        new Object[] { (key + "=" + systemPropValue),
                              (key + "=" + value),
                              pkgBrowser.getRootDirectory().getName() });
               }
            }
            // set new system property
            System.setProperty(key, value);

            UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.CONFIG,
                  this.getClass().getName(),
                  "initialize",
                  LOG_RESOURCE_BUNDLE,
                  "UIMA_pear_runtime_set_system_var__CONFIG",
                  new Object[] { key + "=" + value,
                        pkgBrowser.getRootDirectory().getName() });

         }
         
         // Caller's Resource Manager obtained from the additional parameters
         // Note: UimaContext can be null for a top level call to produceAnalysisEngine,
         //       where the descriptor is a Pear Resource.
         
         ResourceManager applicationRM = (ResourceManager) aAdditionalParams.get(Resource.PARAM_RESOURCE_MANAGER);
         if (null == applicationRM) {  
           UimaContextAdmin uimaContext = (UimaContextAdmin)aAdditionalParams.get(Resource.PARAM_UIMA_CONTEXT);
           if (null != uimaContext) {
             applicationRM = uimaContext.getResourceManager();
           }
         }
         
         String classPath = pkgBrowser.buildComponentClassPath();
         String dataPath = pkgBrowser.getComponentDataPath();
         StringPair sp = new StringPair(classPath, dataPath);
         ResourceManager innerRM;

         Map<StringPair, ResourceManager> c1 = cachedResourceManagers.get(applicationRM);
         if (null == c1) {
            innerRM = createRM(sp, pkgBrowser, applicationRM);
            cachedResourceManagers.put(applicationRM, createRMmap(sp, innerRM));
         } else {
            innerRM = (ResourceManager) c1.get(sp);
            if (null == innerRM) {
               innerRM = createRM(sp, pkgBrowser, applicationRM);
               c1.put(sp, innerRM);
               UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG,
                     this.getClass().getName(), "initialize",
                     LOG_RESOURCE_BUNDLE, "UIMA_pear_runtime_add_RM_map",
                     new Object[] { sp.classPath, sp.dataPath });
            }
         }

         // Create an XML input source from the specifier file
         XMLInputSource in = new XMLInputSource(pkgBrowser
               .getInstallationDescriptor().getMainComponentDesc());

         // Parse the resource specifier
         ResourceSpecifier specifier = UIMAFramework.getXMLParser()
               .parseResourceSpecifier(in);

         UimaContextAdmin uimaContext = (UimaContextAdmin) aAdditionalParams.get(Resource.PARAM_UIMA_CONTEXT);
         if (null != uimaContext) {
           ((ChildUimaContext_impl)uimaContext).setPearResourceManager(innerRM);
         }
         // create analysis engine
         // Cloning is needed because the aAdditionalParameters, if 
         //  passed without cloning to produceAnalysisEngine, gets
         //  modified, and the aAdditionalParameters original object
         //  is re-used by the ASB_impl - a caller of this method,
         //  for other delegates.
         Map clonedAdditionalParameters = new HashMap(aAdditionalParams);
//         clonedAdditionalParameters.remove(Resource.PARAM_UIMA_CONTEXT);
         clonedAdditionalParameters.remove(Resource.PARAM_RESOURCE_MANAGER);
         this.ae = UIMAFramework
               .produceAnalysisEngine(specifier, innerRM, clonedAdditionalParameters);
      } catch (IOException ex) {
         throw new ResourceInitializationException(ex);
      } catch (InvalidXMLException ex) {
         throw new ResourceInitializationException(ex);
      }

      // note - this call must follow the setting of this.ae to a non-null value.
      super.initialize(aSpecifier, aAdditionalParams);

      UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG,
            this.getClass().getName(), "initialize", LOG_RESOURCE_BUNDLE,
            "UIMA_analysis_engine_init_successful__CONFIG",
            new Object[] { this.ae.getAnalysisEngineMetaData().getName() });

      return true;
   }

   /*
    * @see org.apache.uima.analysis_engine.AnalysisEngine#getAnalysisEngineMetaData()
    */
   public AnalysisEngineMetaData getAnalysisEngineMetaData() {
      return (AnalysisEngineMetaData) getMetaData();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.resource.Resource_ImplBase#getMetaData()
    */
   public ResourceMetaData getMetaData() {
      return this.ae.getMetaData();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.analysis_engine.AnalysisEngine#batchProcessComplete()
    */
   public void batchProcessComplete() throws AnalysisEngineProcessException {
      this.ae.batchProcessComplete();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.analysis_engine.AnalysisEngine#collectionProcessComplete()
    */
   public void collectionProcessComplete()
         throws AnalysisEngineProcessException {
      this.ae.collectionProcessComplete();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.analysis_engine.AnalysisEngine#processAndOutputNewCASes(org.apache.uima.cas.CAS)
    */
   public CasIterator processAndOutputNewCASes(CAS aCAS)
         throws AnalysisEngineProcessException {

      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINE,
            this.getClass().getName(), "processAndOutputNewCASes",
            LOG_RESOURCE_BUNDLE, "UIMA_analysis_engine_process_begin__FINE",
            new Object[] { this.ae.getAnalysisEngineMetaData().getName() });

      this.ae.process(aCAS);

      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINE,
            this.getClass().getName(), "processAndOutputNewCASes",
            LOG_RESOURCE_BUNDLE, "UIMA_analysis_engine_process_end__FINE",
            new Object[] { this.ae.getAnalysisEngineMetaData().getName() });

      return new EmptyCasIterator();
   }

   /**
    * @see org.apache.uima.resource.Resource#destroy()
    */
   public void destroy() {

      UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG,
            this.getClass().getName(), "destroy", LOG_RESOURCE_BUNDLE,
            "UIMA_analysis_engine_destroyed__CONFIG",
            new Object[] { this.ae.getAnalysisEngineMetaData().getName() });

      this.ae.destroy();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase#setResultSpecification(org.apache.uima.analysis_engine.ResultSpecification)
    */
   public void setResultSpecification(ResultSpecification resultSpec) {
      this.ae.setResultSpecification(resultSpec);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase#batchProcessComplete(org.apache.uima.util.ProcessTrace)
    */
   public void batchProcessComplete(ProcessTrace trace)
         throws ResourceProcessException, IOException {
      this.ae.batchProcessComplete(trace);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase#collectionProcessComplete(org.apache.uima.util.ProcessTrace)
    */
   public void collectionProcessComplete(ProcessTrace trace)
         throws ResourceProcessException, IOException {
      this.ae.collectionProcessComplete(trace);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase#createResultSpecification()
    */
   public ResultSpecification createResultSpecification() {
      return this.ae.createResultSpecification();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase#createResultSpecification(org.apache.uima.cas.TypeSystem)
    */
   public ResultSpecification createResultSpecification(TypeSystem typeSystem) {
      return this.ae.createResultSpecification(typeSystem);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase#getProcessingResourceMetaData()
    */
   public ProcessingResourceMetaData getProcessingResourceMetaData() {
      return this.ae.getProcessingResourceMetaData();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase#newCAS()
    */
   public synchronized CAS newCAS() throws ResourceInitializationException {
      return this.ae.newCAS();
   }

   /* (non-Javadoc)
    * @see org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase#typeSystemInit(org.apache.uima.cas.TypeSystem)
    */
   public void typeSystemInit(TypeSystem typeSystem)
         throws ResourceInitializationException {
      this.ae.typeSystemInit(typeSystem);
   }

   /**
    * inner class StringPair
    * 
    */
   static private class StringPair {

      private String classPath;

      private String dataPath;

      public StringPair(String classPath, String dataPath) {
         this.classPath = classPath;
         this.dataPath = dataPath;
      }

      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result
               + ((this.classPath == null) ? 0 : this.classPath.hashCode());
         result = prime * result
               + ((this.dataPath == null) ? 0 : this.dataPath.hashCode());
         return result;
      }

      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         final StringPair other = (StringPair) obj;
         if (this.classPath == null) {
            if (other.classPath != null)
               return false;
         } else if (!this.classPath.equals(other.classPath))
            return false;
         if (this.dataPath == null) {
            if (other.dataPath != null)
               return false;
         } else if (!this.dataPath.equals(other.dataPath))
            return false;
         return true;
      }
   }
}
