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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.pear.tools.PackageBrowser;
import org.apache.uima.resource.CustomResourceSpecifier;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.TypeSystemUtil;
import org.apache.uima.util.XMLInputSource;

/**
 * UIMA pear runtime analysis engine wrapper. With this wrapper implementation it is possible to run
 * installed pear files out of the box in UIMA.
 * 
 */
public class PearAnalysisEngineWrapper extends AnalysisEngineImplBase {

  /**
   * installed pear root directory parameter key
   */
  public static final String INSTALLED_PEAR_ROOT_DIR_PARAMETER = "installedPearRoot";

  private AnalysisEngine ae = null;

  private CAS cas = null;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase#initialize(org.apache.uima.resource.ResourceSpecifier,
   *      java.util.Map)
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map aAdditionalParams)
          throws ResourceInitializationException {

    // aSpecifier must be a CustomResourceSpecifier
    if (!(aSpecifier instanceof CustomResourceSpecifier)) {
      return false;
    }

    CustomResourceSpecifier customSpec = (CustomResourceSpecifier) aSpecifier;
    
    //get custom resource specifier parameters
    Parameter[] params = customSpec.getParameters();
    String pearRootDirPath = null;
    for(int i = 0; i < params.length; i++) {
      if(params[i].getName().equals(INSTALLED_PEAR_ROOT_DIR_PARAMETER)){
        pearRootDirPath = params[i].getValue();
      }
    }

    //if INSTALLED_PEAR_ROOT_DIR_PARAMETER was not available, return false. 
    //The Wrapper cannot start the pear file wihtout knowing the installed pear root directory.
    if(pearRootDirPath == null) {
      //log that INSTALLED_PEAR_ROOT_DIR_PARAMETER parameter is missing in the descriptor
      UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
              "initialize", LOG_RESOURCE_BUNDLE,
              "UIMA_pear_runtime_param_not_available__SEVERE",
              new Object[] { INSTALLED_PEAR_ROOT_DIR_PARAMETER });
      return false;
    }
    
    try {
      // get installed pear root directory - specified as URI of the descriptor
      File pearRootDir = new File(pearRootDirPath);

      // create pear package browser to get the pear meta data 
      PackageBrowser pkgBrowser = new PackageBrowser(pearRootDir);

      // create UIMA resource manager and apply pear settings
      ResourceManager rsrcMgr = null;
      rsrcMgr = UIMAFramework.newDefaultResourceManager();
      rsrcMgr.setExtensionClassPath(pkgBrowser.buildComponentClassPath(), true);
      //get and set uima.datapath if specified
      String dataPath = pkgBrowser.getComponentDataPath();
      if(dataPath != null) {
        rsrcMgr.setDataPath(dataPath);  
      }
     
      // Create an XML input source from the specifier file
      XMLInputSource in = new XMLInputSource(pkgBrowser.getInstallationDescriptor()
              .getMainComponentDesc());

      // Parse the resource specifier
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);

      // create analysis engine
      this.ae = UIMAFramework.produceAnalysisEngine(specifier, rsrcMgr, null);
    } catch (IOException ex) {
      throw new ResourceInitializationException(ex);
    } catch (InvalidXMLException ex) {
      throw new ResourceInitializationException(ex);
    }

    super.initialize(aSpecifier, aAdditionalParams);
    
    UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
            "initialize", LOG_RESOURCE_BUNDLE,
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
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    this.ae.collectionProcessComplete();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.AnalysisEngine#processAndOutputNewCASes(org.apache.uima.cas.CAS)
   */
  public CasIterator processAndOutputNewCASes(CAS aCAS) throws AnalysisEngineProcessException {

    UIMAFramework.getLogger(this.getClass()).logrb(Level.FINE, this.getClass().getName(),
            "processAndOutputNewCASes", LOG_RESOURCE_BUNDLE,
            "UIMA_analysis_engine_process_begin__FINE",
            new Object[] { this.ae.getAnalysisEngineMetaData().getName() });

    // create CAS with the type system of the first document
    if (this.cas == null) {
      try {
        // Timer casTimer = new Timer();
        // casTimer.start();
        TypeSystemDescription tsDescription = TypeSystemUtil.typeSystem2TypeSystemDescription(aCAS
                .getTypeSystem());

        this.cas = CasCreationUtils.createCas(tsDescription, super.getAnalysisEngineMetaData()
                .getTypePriorities(), super.getAnalysisEngineMetaData().getFsIndexes());
        // casTimer.stop();
        // System.out.println("\nCAS creation time:" + casTimer.getTimeSpan());
      } catch (ResourceInitializationException ex) {
        throw new AnalysisEngineProcessException(ex);
      }
    }

      //DEBUG INFORMATION
//    Timer globalTimer = new Timer();
//    Timer serialize1 = new Timer();
//    Timer serialize2 = new Timer();
//    Timer processing = new Timer();
//    globalTimer.start();
//    serialize1.start();

    this.cas.reset();

    // serialize aggregate CAS into the pear CAS

    // SLOWEST CAS SERIALIZATION/DESERIALIZATION
    // CASCompleteSerializer serializer = Serialization.serializeCASComplete((CASMgr)aCAS);
    // aCAS.reset();
    // Serialization.deserializeCASComplete(serializer, (CASMgr) this.cas);

    // FASTER CAS SERIALIZATION/DESERIALIZATION
    // this.cas.reset();
    // CASSerializer serializer = new CASSerializer();
    // serializer.addCAS((CASImpl) aCAS);
    // ((CASImpl) this.cas).reinit(serializer);

    //FASTEST CAS SERIALIZATION/DESERIALIZATION
    try {
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream(aCAS.size());
      BufferedOutputStream bufOut = new BufferedOutputStream(byteOut);
      CASSerializer serializer = new CASSerializer();
      serializer.addCAS((CASImpl) aCAS, bufOut);
      bufOut.flush();
      byte[] content = byteOut.toByteArray();
      bufOut.close();
      aCAS.reset();
      BufferedInputStream bufIn = new BufferedInputStream(new ByteArrayInputStream(content));
      Serialization.deserializeCAS(this.cas, bufIn);
    } catch (IOException ex) {
      throw new AnalysisEngineProcessException(ex);
    }

      // DEBUG INFORMATION
//    serialize1.stop();
//    processing.start();

    // process pear ae
    this.ae.process(this.cas);

      // DEBUG INFORMATION
//    processing.stop();
//    serialize2.start();

    // serialize pear CAS into the aggregate CAS
    
    // SLOWEST CAS SERIALIZATION/DESERIALIZATION
    // serializer = Serialization.serializeCASComplete((CASMgr)this.cas);
    // this.cas.reset();
    // Serialization.deserializeCASComplete(serializer, (CASMgr) aCAS);
    
    // FASTER CAS SERIALIZATION/DESERIALIZATION
    // aCAS.reset();
    // serializer = new CASSerializer();
    // serializer.addCAS((CASImpl) this.cas);
    // ((CASImpl) aCAS).reinit(serializer);

    // FASTEST CAS SERIALIZATION/DESERIALIZATION
    try {
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream(this.cas.size());
      BufferedOutputStream bufOut = new BufferedOutputStream(byteOut);
      CASSerializer serializer = new CASSerializer();
      serializer.addCAS((CASImpl) this.cas, bufOut);
      bufOut.flush();
      byte[] content = byteOut.toByteArray();
      bufOut.close();
      aCAS.reset();
      BufferedInputStream bufIn = new BufferedInputStream(new ByteArrayInputStream(content));
      Serialization.deserializeCAS(aCAS, bufIn);
    } catch (IOException ex) {
      throw new AnalysisEngineProcessException(ex);
    }

      // DEBUG INFORMATION
//    serialize2.stop();
//    globalTimer.stop();
//    System.out.print("\n" + serialize1.getTimeSpan());
//    System.out.print(" : " + processing.getTimeSpan());
//    System.out.print(" : " + serialize2.getTimeSpan());
//    System.out.print(" : " + globalTimer.getTimeSpan());

    UIMAFramework.getLogger(this.getClass()).logrb(Level.FINE, this.getClass().getName(),
            "processAndOutputNewCASes", LOG_RESOURCE_BUNDLE,
            "UIMA_analysis_engine_process_end__FINE",
            new Object[] { this.ae.getAnalysisEngineMetaData().getName() });

    return new EmptyCasIterator();
  }

  /**
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy() {
      
    UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
            "destroy", LOG_RESOURCE_BUNDLE,
            "UIMA_analysis_engine_destroyed__CONFIG",
            new Object[] { this.ae.getAnalysisEngineMetaData().getName() });
    
    this.ae.destroy();
    this.cas = null;
  }

}
