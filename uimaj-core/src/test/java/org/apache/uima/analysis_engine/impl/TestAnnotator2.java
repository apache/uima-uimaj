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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextHolder;
import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.impl.UimaContext_ImplBase;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Settings;
import org.apache.uima.util.UimaContextHolderTest;
import org.apache.uima.util.XMLInputSource;

/**
 * Annotator class used for testing.
 * 
 */
public class TestAnnotator2 extends CasAnnotator_ImplBase {
  // Initialize and process methods save information to these static fields,
  // which are queried by the unit test.
  public static String lastDocument;

  public static String stringParamValue;

  public static boolean typeSystemInitCalled;

  public static String allContexts = "";

  public static synchronized String getLastDocument() {
    return lastDocument;
  }

  /*
   * @throws ResourceInitializationException tbd
   * 
   * @see org.apache.uima.analysis_component.CasAnnotator_ImplBase#initialize(UimaContext)
   */
  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    typeSystemInitCalled = false;
    lastDocument = null;
    stringParamValue = (String) aContext.getConfigParameterValue("StringParam");

    // Check if can get an arbitrary external parameter from the override settings
    // Note: this annotator launched with external overrides loaded from
    // testExternalOverride2.settings
    String contextName = ((UimaContext_ImplBase) aContext).getQualifiedContextName();
    if ("/ExternalOverrides/".equals(contextName)) {
      String expected = "Context Holder Test";

      // Test getting a (0-length) array of strings
      assertThatNoException().isThrownBy(() -> assertThat(
              UimaContextHolder.getContext().getSharedSettingArray("test.externalFloatArray"))
                      .isEmpty());

      // Test assigning an array to a string and vice-versa
      // prefix-suffix Prefix-${suffix}
      // suffix = should be ignored
      assertThatNoException().isThrownBy(() -> assertThat(
              UimaContextHolder.getContext().getSharedSettingValue("context-holder"))
                      .isEqualTo(expected));

      // Test assigning an array to a string and vice-versa
      assertThatExceptionOfType(ResourceConfigurationException.class)
              .isThrownBy(() -> UimaContextHolder.getContext()
                      .getSharedSettingValue("test.externalFloatArray"));

      assertThatExceptionOfType(ResourceConfigurationException.class).isThrownBy(
              () -> UimaContextHolder.getContext().getSharedSettingArray("prefix-suffix"));

      // Test a stand-alone settings object
      Settings testSettings = UIMAFramework.getResourceSpecifierFactory().createSettings();
      String lines = "foo = ${bar} \n" + "bar : [ok \n OK] \n" + "bad = ${missing} \n"
              + "loop1 = one ${loop2} \n" + "loop2 = two ${loop3} \n" + "loop3 = three ${loop1} \n";

      assertThatNoException().isThrownBy(() -> {
        try (InputStream is = new ByteArrayInputStream(lines.getBytes(StandardCharsets.UTF_8))) {
          testSettings.load(is);
          String val = testSettings.lookUp("foo");
          assertThat(val).isEqualTo("[ok,OK]");

          assertThatExceptionOfType(ResourceConfigurationException.class)
                  .as("\"bad\" should create an error")
                  .isThrownBy(() -> testSettings.lookUp("bad"));

          assertThatExceptionOfType(ResourceConfigurationException.class)
                  .as("\"loop2\" should create an error")
                  .isThrownBy(() -> testSettings.lookUp("loop2"));
        }
      });

      // Test POFO access via UimaContextHolder
      long threadId = Thread.currentThread().getId();
      UimaContextHolderTest testPojoAccess = new UimaContextHolderTest();
      assertThatNoException()
              .isThrownBy(() -> assertThat(testPojoAccess.testSettings()).isEqualTo(expected));
      assertThat(testPojoAccess.threadId).isEqualTo(threadId);

      // Try from a child thread - should work
      testPojoAccess.result = null;
      Thread thrd = new Thread(testPojoAccess);
      thrd.start();
      synchronized (thrd) {
        assertThatNoException().isThrownBy(() -> thrd.wait());
        assertThat(testPojoAccess.result).isEqualTo(expected);
        assertThat(testPojoAccess.threadId).isNotSameAs(threadId);
      }

      // Try from a process - should fail
      String[] args = { System.getProperty("java.home") + "/bin/java", "-cp",
          System.getProperty("java.class.path"), UimaContextHolderTest.class.getName() };
      ProcessBuilder pb = new ProcessBuilder(args);
      try {
        Process proc = pb.start();
        assertThat(proc.waitFor()).isZero();
      } catch (IOException | InterruptedException e) {
        fail();
      }

      // Test getting a string value
      assertThatNoException().isThrownBy(() -> assertThat(
              UimaContextHolder.getContext().getSharedSettingValue("context-holder"))
                      .isEqualTo(expected));

      // Create a nested engine with a different settings
      String resDir = "src/test/resources/TextAnalysisEngineImplTest/";
      try {
        // XMLInputSource in = new
        // XMLInputSource(JUnitExtension.getFile("TextAnalysisEngineImplTest/AnnotatorWithExternalOverrides.xml"));
        XMLInputSource in = new XMLInputSource(
                new File(resDir, "AnnotatorWithExternalOverrides.xml"));
        AnalysisEngineDescription desc = UIMAFramework.getXMLParser()
                .parseAnalysisEngineDescription(in);
        Map<String, Object> additionalParams = new HashMap<>();
        Settings extSettings = UIMAFramework.getResourceSpecifierFactory().createSettings();
        FileInputStream fis = new FileInputStream(
                new File(resDir, "testExternalOverride2.settings"));
        extSettings.load(fis);
        fis.close();
        additionalParams.put(Resource.PARAM_EXTERNAL_OVERRIDE_SETTINGS, extSettings);
        UIMAFramework.produceAnalysisEngine(desc, additionalParams);
      } catch (Exception e) {
        fail();
      }

      assertThatNoException().isThrownBy(() -> assertThat(
              UimaContextHolder.getContext().getSharedSettingValue("context-holder"))
                      .isEqualTo(expected));
    }

    // Used to check initialization order by testManyDelegates
    allContexts = allContexts + contextName.substring(1);
  }

  @Override
  public void typeSystemInit(TypeSystem aTypeSystem) {
    typeSystemInitCalled = true;
  }

  @Override
  public void process(CAS aCAS) {
    // set static fields to contain document text, result spec,
    // and value of StringParam configuration parameter.
    lastDocument = aCAS.getDocumentText();
  }
}
