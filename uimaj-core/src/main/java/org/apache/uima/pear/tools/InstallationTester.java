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

package org.apache.uima.pear.tools;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.collection.CasInitializer;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.internal.util.I18nUtil;
import org.apache.uima.pear.tools.InstallationController.TestStatus;
import org.apache.uima.pear.util.UIMAUtil;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

/**
 * The <code>InstallationTester</code> application allows performing simple verification of the
 * installed UIMA compliant component by using standard UIMA framework interface.
 * 
 */

public class InstallationTester {

  // main component descriptor type of the pear package
  private String uimaCategory = null;

  // package browser object of the installed pear package
  private PackageBrowser pkgBrowser = null;

  private Properties systemProps = null;

  private static final String PEAR_MESSAGE_RESOURCE_BUNDLE = "org.apache.uima.pear.pear_messages";

  /**
   * Creates new instance of the <code>InstallationTester</code> class, identifies a specified
   * component using UIMA API.
   * 
   * @param pkgBrowser
   *          packageBrowser object of an installed PEAR package
   * @throws java.io.IOException
   *           if any I/O exception occurred.
   * @throws org.apache.uima.util.InvalidXMLException
   *           if component descriptor is invalid.
   * @throws org.apache.uima.resource.ResourceInitializationException
   *           if the specified component cannot be instantiated.
   * @throws org.apache.uima.UIMAException
   *           if this exception occurred while identifying UIMA component category.
   * @throws org.apache.uima.UIMARuntimeException
   *           if this exception occurred while identifying UIMA component category.
   */
  public InstallationTester(PackageBrowser pkgBrowser) throws IOException, InvalidXMLException,
          ResourceInitializationException, UIMAException, UIMARuntimeException {

    // set PackageBrowser
    this.pkgBrowser = pkgBrowser;

    // save System properties
    systemProps = System.getProperties();

    // check UIMA category of the main component
    File compDescFile = new File(
            this.pkgBrowser.getInstallationDescriptor().getMainComponentDesc());
    uimaCategory = UIMAUtil.identifyUimaComponentCategory(compDescFile);
    if (uimaCategory == null) {
      Exception err = UIMAUtil.getLastErrorForXmlDesc(compDescFile);
      if (err != null) {
        if (err instanceof UIMAException)
          throw (UIMAException) err;
        else if (err instanceof UIMARuntimeException)
          throw (UIMARuntimeException) err;
        else
          throw new RuntimeException(err);
      }
    }
  }

  public TestStatus doTest()
          throws IOException, InvalidXMLException, ResourceInitializationException {

    if (uimaCategory.equals(UIMAUtil.TYPE_SYSTEM_CTG)) {
      return testTypeSystem();
    }

    List<String> kinds = Arrays.asList(new String[] { UIMAUtil.ANALYSIS_ENGINE_CTG,
        UIMAUtil.CAS_CONSUMER_CTG, UIMAUtil.CAS_INITIALIZER_CTG, UIMAUtil.COLLECTION_READER_CTG,
        UIMAUtil.CPE_CONFIGURATION_CTG });
    if (!kinds.contains(uimaCategory)) {
      TestStatus status = new TestStatus();
      status.setMessage(I18nUtil.localizeMessage(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "installation_verification_type_not_detected",
              new Object[] { pkgBrowser.getInstallationDescriptor().getMainComponentId() }, null));

      return status;
    }

    // set system properties
    setSystemProperties(pkgBrowser);

    // create analysis engine
    XMLInputSource xmlIn = null;
    ResourceManager resource_manager = null;

    try {
      xmlIn = new XMLInputSource(pkgBrowser.getInstallationDescriptor().getMainComponentDesc());
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(xmlIn);

      resource_manager = getResourceManager(pkgBrowser);

      TestStatus status = new TestStatus();

      if (uimaCategory.equals(UIMAUtil.ANALYSIS_ENGINE_CTG)) {
        testAnalysisEngine(specifier, resource_manager, status);
      } else if (uimaCategory.equals(UIMAUtil.CAS_CONSUMER_CTG)) {
        testCasConsumer(specifier, resource_manager, status);
      } else if (uimaCategory.equals(UIMAUtil.CAS_INITIALIZER_CTG)) {
        testCasInitializer(specifier, resource_manager, status);
      } else if (uimaCategory.equals(UIMAUtil.COLLECTION_READER_CTG)) {
        testCollectionReader(specifier, resource_manager, status);
      } else if (uimaCategory.equals(UIMAUtil.CPE_CONFIGURATION_CTG)) {
        testCpeCongifuration(specifier, resource_manager, status);
      }

      // reset system properties
      resetSystemProperties();

      // return status object
      return status;
    } finally {
      if (xmlIn != null) {
        xmlIn.close();
      }
      if (resource_manager != null) {
        resource_manager.destroy();
      }
    }
  }

  /**
   * returns a valid ResourceManager with the information from the PackageBrowser object.
   * 
   * @param pkgBrowser
   *          packageBrowser object of an installed PEAR package
   * 
   * @return a ResourceManager object with the information from the PackageBrowser object.
   * 
   * @throws IOException
   *           passthru
   */
  private static ResourceManager getResourceManager(PackageBrowser pkgBrowser) throws IOException {
    ResourceManager resourceMgr = UIMAFramework.newDefaultResourceManager();
    // set component data path
    if (pkgBrowser.getComponentDataPath() != null) {
      resourceMgr.setDataPath(pkgBrowser.getComponentDataPath());
    }
    // set component classpath
    if (pkgBrowser.buildComponentClassPath() != null) {
      resourceMgr.setExtensionClassPath(pkgBrowser.buildComponentClassPath(), true);
    }

    return resourceMgr;
  }

  /**
   * Set the environment variables that are specified in the PackageBrowser object as System
   * properties
   * 
   * @param pkgBrowser
   *          packageBrowser object of an installed PEAR package
   * 
   * @throws IOException
   *           passthru
   */
  private static void setSystemProperties(PackageBrowser pkgBrowser) throws IOException {

    // get pear env variables and set them as system properties
    Properties props = pkgBrowser.getComponentEnvVars();
    Iterator<Object> keyIterator = props.keySet().iterator();
    while (keyIterator.hasNext()) {
      String key = (String) keyIterator.next();
      String value = (String) props.get(key);

      // set new system property
      System.setProperty(key, value);
    }
  }

  /**
   * reset the System properties as it was before the pear verification is executed.
   */
  private void resetSystemProperties() {
    // reset system properties
    System.setProperties(systemProps);
  }

  /**
   * Checks if a given analysis engine specifier file can be used to produce an instance of analysis
   * engine. Returns <code>true</code>, if an analysis engine can be instantiated,
   * <code>false</code> otherwise.
   * 
   * @param specifier
   *          the resource specifier
   * @param resource_manager
   *          a new resource_manager
   * @param status
   *          the place where to put the results
   *
   * @throws IOException
   *           If an I/O exception occurred while creating <code>XMLInputSource</code>.
   * @throws InvalidXMLException
   *           If the XML parser failed to parse the given input file.
   * @throws ResourceInitializationException
   *           If the specified AE cannot be instantiated.
   */
  private void testAnalysisEngine(ResourceSpecifier specifier, ResourceManager resource_manager,
          TestStatus status)
          throws IOException, InvalidXMLException, ResourceInitializationException {

    AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(specifier, resource_manager, null);

    // create CAS from the analysis engine
    CAS cas = null;
    if (ae != null) {
      cas = ae.newCAS();
    }

    // check test result
    if (ae != null && cas != null) {
      status.setRetCode(TestStatus.TEST_SUCCESSFUL);
    } else {
      status.setRetCode(TestStatus.TEST_NOT_SUCCESSFUL);
      status.setMessage(I18nUtil.localizeMessage(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "installation_verification_ae_not_created",
              new Object[] { pkgBrowser.getInstallationDescriptor().getMainComponentId() }, null));
    }
  }

  /**
   * Checks if a given CC specifier file can be used to produce an instance of CC. Returns
   * <code>true</code>, if a CC can be instantiated, <code>false</code> otherwise.
   * 
   * @param specifier
   *          the resource specifier
   * @param resource_manager
   *          a new resource_manager
   * @param status
   *          the place where to put the results
   *
   * @throws IOException
   *           If an I/O exception occurred while creating <code>XMLInputSource</code>.
   * @throws InvalidXMLException
   *           If the XML parser failed to parse the given input file.
   * @throws ResourceInitializationException
   *           If the specified CC cannot be instantiated.
   */
  private void testCasConsumer(ResourceSpecifier specifier, ResourceManager resource_manager,
          TestStatus status)
          throws IOException, InvalidXMLException, ResourceInitializationException {

    CasConsumer cc = UIMAFramework.produceCasConsumer(specifier, resource_manager, null);

    if (cc != null) {
      status.setRetCode(TestStatus.TEST_SUCCESSFUL);
    } else {
      status.setRetCode(TestStatus.TEST_NOT_SUCCESSFUL);
      status.setMessage(I18nUtil.localizeMessage(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "installation_verification_cc_not_created",
              new Object[] { pkgBrowser.getInstallationDescriptor().getMainComponentId() }, null));
    }
  }

  /**
   * Checks if a given CI specifier file can be used to produce an instance of CI. Returns
   * <code>true</code>, if a CI can be instantiated, <code>false</code> otherwise.
   * 
   * @param specifier
   *          the resource specifier
   * @param resource_manager
   *          a new resource_manager
   * @param status
   *          the place where to put the results
   *
   * @throws IOException
   *           If an I/O exception occurred while creating <code>XMLInputSource</code>.
   * @throws InvalidXMLException
   *           If the XML parser failed to parse the given input file.
   * @throws ResourceInitializationException
   *           If the specified CI cannot be instantiated.
   */
  private void testCasInitializer(ResourceSpecifier specifier, ResourceManager resource_manager,
          TestStatus status)
          throws IOException, InvalidXMLException, ResourceInitializationException {

    CasInitializer ci = UIMAFramework.produceCasInitializer(specifier, resource_manager, null);

    if (ci != null) {
      status.setRetCode(TestStatus.TEST_SUCCESSFUL);
    } else {
      status.setRetCode(TestStatus.TEST_NOT_SUCCESSFUL);
      status.setMessage(I18nUtil.localizeMessage(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "installation_verification_ci_not_created",
              new Object[] { pkgBrowser.getInstallationDescriptor().getMainComponentId() }, null));
    }
  }

  /**
   * Checks if a given CR specifier file can be used to produce an instance of CR. Returns
   * <code>true</code>, if a CR can be instantiated, <code>false</code> otherwise.
   * 
   * @param specifier
   *          the resource specifier
   * @param resource_manager
   *          a new resource_manager
   * @param status
   *          the place where to put the results
   *
   * @throws IOException
   *           If an I/O exception occurred while creating <code>XMLInputSource</code>.
   * @throws InvalidXMLException
   *           If the XML parser failed to parse the given input file.
   * @throws ResourceInitializationException
   *           If the specified CR cannot be instantiated.
   */
  private void testCollectionReader(ResourceSpecifier specifier, ResourceManager resource_manager,
          TestStatus status)
          throws IOException, InvalidXMLException, ResourceInitializationException {
    CollectionReader cr = UIMAFramework.produceCollectionReader(specifier, resource_manager, null);

    if (cr != null) {
      status.setRetCode(TestStatus.TEST_SUCCESSFUL);
    } else {
      status.setRetCode(TestStatus.TEST_NOT_SUCCESSFUL);
      status.setMessage(I18nUtil.localizeMessage(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "installation_verification_cr_not_created",
              new Object[] { pkgBrowser.getInstallationDescriptor().getMainComponentId() }, null));
    }
  }

  /**
   * Checks if a given CPE specifier file can be used to produce an instance of CPE. Returns
   * <code>true</code>, if a CPE can be instantiated, <code>false</code> otherwise.
   * 
   * @param specifier
   *          the resource specifier
   * @param resource_manager
   *          a new resource_manager
   * @param status
   *          the place where to put the results
   *
   * @throws IOException
   *           If an I/O exception occurred while creating <code>XMLInputSource</code>.
   * @throws InvalidXMLException
   *           If the XML parser failed to parse the given input file.
   * @throws ResourceInitializationException
   *           If the specified CPE cannot be instantiated.
   */
  private void testCpeCongifuration(ResourceSpecifier specifier, ResourceManager resource_manager,
          TestStatus status)
          throws IOException, InvalidXMLException, ResourceInitializationException {

    CollectionProcessingEngine cpe = UIMAFramework
            .produceCollectionProcessingEngine((CpeDescription) specifier, resource_manager, null);

    if (cpe != null) {
      status.setRetCode(TestStatus.TEST_SUCCESSFUL);
    } else {
      status.setRetCode(TestStatus.TEST_NOT_SUCCESSFUL);
      status.setMessage(I18nUtil.localizeMessage(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "installation_verification_cpe_not_created",
              new Object[] { pkgBrowser.getInstallationDescriptor().getMainComponentId() }, null));
    }

  }

  /**
   * Checks if a given TS specifier file can be used to create an instance of CAS. Returns
   * <code>true</code>, if a CAS can be created for a given TS, <code>false</code> otherwise.
   * 
   * @return the result of the testing
   * @throws IOException
   *           If an I/O exception occurred while creating <code>XMLInputSource</code>.
   * @throws InvalidXMLException
   *           If the XML parser failed to parse the given input file.
   * @throws ResourceInitializationException
   *           If the specified TS cannot be used to create a CAS.
   */
  private TestStatus testTypeSystem()
          throws IOException, InvalidXMLException, ResourceInitializationException {
    // set system properties
    setSystemProperties(pkgBrowser);

    XMLInputSource xmlIn = null;

    try {
      xmlIn = new XMLInputSource(pkgBrowser.getInstallationDescriptor().getMainComponentDesc());
      TypeSystemDescription tsDescription = UIMAFramework.getXMLParser()
              .parseTypeSystemDescription(xmlIn);

      TypePriorities tPriorities = UIMAFramework.getResourceSpecifierFactory()
              .createTypePriorities();
      FsIndexDescription[] fsIndexes = new FsIndexDescription[0];
      CAS cas = CasCreationUtils.createCas(tsDescription, tPriorities, fsIndexes);

      // create Test status object
      TestStatus status = new TestStatus();

      if (cas != null) {
        status.setRetCode(TestStatus.TEST_SUCCESSFUL);
      } else {
        status.setRetCode(TestStatus.TEST_NOT_SUCCESSFUL);
        status.setMessage(I18nUtil.localizeMessage(PEAR_MESSAGE_RESOURCE_BUNDLE,
                "installation_verification_cas_not_created",
                new Object[] { pkgBrowser.getInstallationDescriptor().getMainComponentId() },
                null));
      }

      // reset system properties
      resetSystemProperties();

      // return status object
      return status;
    } finally {
      if (xmlIn != null) {
        xmlIn.close();
      }
    }
  }

}
