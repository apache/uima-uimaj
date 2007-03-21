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
import java.net.URI;
import java.net.URISyntaxException;
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
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.URISpecifier;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.TypeSystemUtil;
import org.apache.uima.util.XMLInputSource;

/**
 * UIMA pear runtime analysis engine wrapper. With this wrapper implementation it is possible to run
 * installed pear files out of the box in UIMA.
 * 
 */
public class PearAnalysisEngineWrapper extends AnalysisEngineImplBase {

  /**
   * PEAR protocol constant
   */
  public static final String PEAR_PROTOCOL = "PEAR";

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

    // aSpecifier must be a URISpecifier
    if (!(aSpecifier instanceof URISpecifier)) {
      return false;
    }

    URISpecifier uriSpec = (URISpecifier) aSpecifier;
    // URISpecifier protocol must be "PEAR"
    if (!uriSpec.getProtocol().equals(PEAR_PROTOCOL)) {
      return false;
    }

    try {
      // get installed pear root directory - specified as URI of the descriptor
      File pearRootDir = new File(new URI(uriSpec.getUri()));

      // create pear package browser to get the pear meta data 
      PackageBrowser pkgBrowser = new PackageBrowser(pearRootDir);

      // create UIMA resource manager and apply pear settings
      ResourceManager rsrcMgr = null;
      rsrcMgr = UIMAFramework.newDefaultResourceManager();
      rsrcMgr.setExtensionClassPath(pkgBrowser.buildComponentClassPath(), true);

      // Create an XML input source from the specifier file
      XMLInputSource in = new XMLInputSource(pkgBrowser.getInstallationDescriptor()
              .getMainComponentDesc());

      // Parse the resource specifier
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);

      // create analysis engine
      this.ae = UIMAFramework.produceAnalysisEngine(specifier, rsrcMgr, null);
    } catch (IOException ex) {
      throw new ResourceInitializationException(ex);
    } catch (URISyntaxException ex) {
      throw new ResourceInitializationException(ex);
    } catch (InvalidXMLException ex) {
      throw new ResourceInitializationException(ex);
    }

    super.initialize(aSpecifier, aAdditionalParams);

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

    return new EmptyCasIterator();
  }

  /**
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy() {
    this.ae.destroy();
    this.cas = null;
  }

}
