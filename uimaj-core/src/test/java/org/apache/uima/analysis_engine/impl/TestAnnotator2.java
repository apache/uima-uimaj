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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextHolder;
import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.impl.UimaContext_ImplBase;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.Settings;
import org.apache.uima.util.UimaContextHolderTest;
import org.apache.uima.util.XMLInputSource;
import org.junit.Assert;

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
   * @see org.apache.uima.analysis_component.CasAnnotator_ImplBase#initialize(UimaContext)
   */
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    typeSystemInitCalled = false;
    lastDocument = null;
    stringParamValue = (String) aContext.getConfigParameterValue("StringParam");
    
    // Check if can get an arbitrary external parameter from the override settings
    // Note: this annotator launched with external overrides loaded from testExternalOverride2.settings 
    String contextName = ((UimaContext_ImplBase) aContext).getQualifiedContextName();
    if ("/ExternalOverrides/".equals(contextName)) {
      // Test getting a (0-length) array of strings
      String[] actuals = null;
      try {
        actuals = UimaContextHolder.getContext().getSharedSettingArray("test.externalFloatArray");
      } catch (ResourceConfigurationException e) {
        Assert.fail(e.getMessage());
      }
      Assert.assertEquals(0, actuals.length);
      
      // Test assigning an array to a string and vice-versa
      String actual = null;
      try {
        actual = UimaContextHolder.getContext().getSharedSettingValue("test.externalFloatArray");
        Assert.fail("\"bad\" should create an error");
      } catch (ResourceConfigurationException e) {
        System.err.println("Expected exception: " + e.toString());
      }
      try {
        actuals = UimaContextHolder.getContext().getSharedSettingArray("prefix-suffix");
        Assert.fail("\"bad\" should create an error");
      } catch (ResourceConfigurationException e) {
        System.err.println("Expected exception: " + e.toString());
      }

      // Test a stand-alone settings object
      Settings testSettings = UIMAFramework.getResourceSpecifierFactory().createSettings();
      String lines = "foo = ${bar} \n" +
                "bar : [ok \n OK] \n" +
                "bad = ${missing} \n" +
                "loop1 = one ${loop2} \n" +
                "loop2 = two ${loop3} \n" +
                "loop3 = three ${loop1} \n" ;
      InputStream is;
      try {
        is = new ByteArrayInputStream(lines.getBytes("UTF-8"));
        testSettings.load(is);
        is.close();
        String val = testSettings.lookUp("foo");
        Assert.assertEquals("[ok,OK]", val);
        try {
          val = testSettings.lookUp("bad");
          Assert.fail("\"bad\" should create an error");
        } catch (ResourceConfigurationException e) {
          System.err.println("Expected exception: " + e.toString());
        }
        try {
          val = testSettings.lookUp("loop2");
          Assert.fail("\"loop2\" should create an error");
        } catch (ResourceConfigurationException e) {
          System.err.println("Expected exception: " + e.toString());
        }
      } catch (Exception e) {
        Assert.fail(e.toString());
      }
      
      // Test POFO access via UimaContextHolder
      String expected = "Context Holder Test";
      long threadId = Thread.currentThread().getId();
      UimaContextHolderTest testPojoAccess = new UimaContextHolderTest();
      try {
        actual = testPojoAccess.testSettings();
        Assert.assertEquals(expected, actual);
        Assert.assertEquals(threadId, testPojoAccess.threadId);
      } catch (ResourceConfigurationException e) {
        Assert.fail();
      }
      // Try from a child thread - should work
      testPojoAccess.result = null;
      Thread thrd = new Thread(testPojoAccess);
      thrd.start();
      synchronized(thrd) {
        try {
          thrd.wait();
          Assert.assertEquals(expected, testPojoAccess.result);
          Assert.assertNotSame(threadId, testPojoAccess.threadId);
        } catch (InterruptedException e) {
          Assert.fail();        }
      }
      // Try from a process - should fail
      String[] args = {
              System.getProperty("java.home")+"/bin/java",
              "-cp", 
              System.getProperty("java.class.path"), 
              UimaContextHolderTest.class.getName()};
      ProcessBuilder pb = new ProcessBuilder(args);
      try {
        Process proc = pb.start();
        int rc = proc.waitFor();
        Assert.assertEquals(0, rc);
      } catch (IOException | InterruptedException e) {
        Assert.fail();
      }
      
      
      // Test getting a string value
      try {
        actual = UimaContextHolder.getContext().getSharedSettingValue("context-holder");
      } catch (ResourceConfigurationException e) {
        Assert.fail(e.getMessage());
      }
      Assert.assertEquals(expected, actual);
      
      // Create a nested engine with a different settings
      String resDir = "src/test/resources/TextAnalysisEngineImplTest/";
      try {
        //XMLInputSource in = new XMLInputSource(JUnitExtension.getFile("TextAnalysisEngineImplTest/AnnotatorWithExternalOverrides.xml"));
        XMLInputSource in = new XMLInputSource(new File(resDir, "AnnotatorWithExternalOverrides.xml"));
        AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
        Map<String, Object> additionalParams = new HashMap<String, Object>();
        Settings extSettings = UIMAFramework.getResourceSpecifierFactory().createSettings();
        FileInputStream fis = new FileInputStream(new File(resDir, "testExternalOverride2.settings"));
        extSettings.load(fis);
        fis.close();
        additionalParams.put(Resource.PARAM_EXTERNAL_OVERRIDE_SETTINGS, extSettings);
        UIMAFramework.produceAnalysisEngine(desc, additionalParams);
      } catch (Exception e) {
        Assert.fail();
      }
      
      try {
        actual = UimaContextHolder.getContext().getSharedSettingValue("context-holder");
      } catch (ResourceConfigurationException e) {
        Assert.fail(e.getMessage());
      }
      Assert.assertEquals(expected, actual);

    }
    // Used to check initialization order by testManyDelegates
    allContexts  = allContexts + contextName.substring(1);
  }

  public void typeSystemInit(TypeSystem aTypeSystem) {
    typeSystemInitCalled = true;
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.TextAnnotator#process(CAS,ResultSpecification)
   */
  public void process(CAS aCAS) {
    // set static fields to contain document text, result spec,
    // and value of StringParam configuration parameter.
    lastDocument = aCAS.getDocumentText();
  }
}
