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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.pear.tools.PackageBrowser;
import org.apache.uima.resource.PearSpecifier;
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

  private AnalysisEngine ae = null;

  private CAS cas = null;
  
  private ResourceManager rsrcMgr = null;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase#initialize(org.apache.uima.resource.ResourceSpecifier,
   *      java.util.Map)
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map aAdditionalParams)
          throws ResourceInitializationException {

    // aSpecifier must be a pearSpecifier
    if (!(aSpecifier instanceof PearSpecifier)) {
      return false;
    }

    //cast resource specifier to a pear specifier
    PearSpecifier pearSpec = (PearSpecifier) aSpecifier;

    // get pear path
    String pearRootDirPath = pearSpec.getPearPath();
 
    try {
      // get installed pear root directory - specified as URI of the descriptor
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

        // log warning if system property already exist and does not have the same value
        if (systemProps.containsKey(key)) {
          String systemPropValue = (String) systemProps.get(key);
          if (!systemPropValue.equals(value)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.WARNING,
                    this.getClass().getName(),
                    "initialize",
                    LOG_RESOURCE_BUNDLE,
                    "UIMA_pear_runtime_system_var_already_set__WARNING",
                    new Object[] { new String(key + "=" + systemPropValue),
                        new String(key + "=" + value), pkgBrowser.getRootDirectory().getName() });
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
                new Object[] { new String(key + "=" + value), pkgBrowser.getRootDirectory().getName() });

      }

      // create UIMA resource manager and apply pear settings
      this.rsrcMgr = UIMAFramework.newDefaultResourceManager();
      String classpath = pkgBrowser.buildComponentClassPath();
      this.rsrcMgr.setExtensionClassPath(classpath, true);
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.CONFIG,
              this.getClass().getName(),
              "initialize",
              LOG_RESOURCE_BUNDLE,
              "UIMA_pear_runtime_set_classpath__CONFIG",
              new Object[] { classpath, pkgBrowser.getRootDirectory().getName() });
      
      // get and set uima.datapath if specified
      String dataPath = pkgBrowser.getComponentDataPath();
      if (dataPath != null) {
        this.rsrcMgr.setDataPath(dataPath);
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.CONFIG,
                this.getClass().getName(),
                "initialize",
                LOG_RESOURCE_BUNDLE,
                "UIMA_pear_runtime_set_datapath__CONFIG",
                new Object[] { dataPath, pkgBrowser.getRootDirectory().getName() });
      }

      // Create an XML input source from the specifier file
      XMLInputSource in = new XMLInputSource(pkgBrowser.getInstallationDescriptor()
              .getMainComponentDesc());

      // Parse the resource specifier
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);

      // create analysis engine
      this.ae = UIMAFramework.produceAnalysisEngine(specifier, this.rsrcMgr, null);
    } catch (IOException ex) {
      throw new ResourceInitializationException(ex);
    } catch (InvalidXMLException ex) {
      throw new ResourceInitializationException(ex);
    }

    super.initialize(aSpecifier, aAdditionalParams);

    UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
            "initialize", LOG_RESOURCE_BUNDLE, "UIMA_analysis_engine_init_successful__CONFIG",
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
                .getTypePriorities(), super.getAnalysisEngineMetaData().getFsIndexes(), null, this.rsrcMgr);
        // casTimer.stop();
        // System.out.println("\nCAS creation time:" + casTimer.getTimeSpan());
      } catch (ResourceInitializationException ex) {
        throw new AnalysisEngineProcessException(ex);
      }
    }

    // DEBUG INFORMATION
    // Timer globalTimer = new Timer();
    // Timer serialize1 = new Timer();
    // Timer serialize2 = new Timer();
    // Timer processing = new Timer();
    // globalTimer.start();
    // serialize1.start();

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

    // FASTEST CAS SERIALIZATION/DESERIALIZATION
    ByteArrayOutputStream fos = new ByteArrayOutputStream();
    Serialization.serializeCAS(aCAS, fos);
    ByteArrayInputStream fis = new ByteArrayInputStream(fos.toByteArray());
    Serialization.deserializeCAS(this.cas, fis);

    // DEBUG INFORMATION
    // serialize1.stop();
    // processing.start();

    // process pear ae
    this.ae.process(this.cas);

    // DEBUG INFORMATION
    // processing.stop();
    // serialize2.start();

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
    fos = new ByteArrayOutputStream();
    Serialization.serializeCAS(this.cas, fos);
    fis = new ByteArrayInputStream(fos.toByteArray());
    Serialization.deserializeCAS(aCAS, fis);

    // DEBUG INFORMATION
    // serialize2.stop();
    // globalTimer.stop();
    // System.out.print("\n" + serialize1.getTimeSpan());
    // System.out.print(" : " + processing.getTimeSpan());
    // System.out.print(" : " + serialize2.getTimeSpan());
    // System.out.print(" : " + globalTimer.getTimeSpan());

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
            "destroy", LOG_RESOURCE_BUNDLE, "UIMA_analysis_engine_destroyed__CONFIG",
            new Object[] { this.ae.getAnalysisEngineMetaData().getName() });

    this.ae.destroy();
    this.cas = null;
  }

}
