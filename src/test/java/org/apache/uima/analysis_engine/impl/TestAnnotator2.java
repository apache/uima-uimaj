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
import java.io.InputStream;

import junit.framework.Assert;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.impl.UimaContext_ImplBase;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Settings;
import org.apache.uima.util.impl.Settings_impl;

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

  /**
   * @throws ResourceInitializationException 
   * @see org.apache.uima.analysis_component.CasAnnotator_ImplBase#initialize(UimaContext)
   */
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    typeSystemInitCalled = false;
    lastDocument = null;
    stringParamValue = (String) aContext.getConfigParameterValue("StringParam");
    
    // Check if can get an arbitrary external parameter from the override settings
    String contextName = ((UimaContext_ImplBase) aContext).getQualifiedContextName();
    if ("/ExternalOverrides/".equals(contextName)) {
      String actual = null;
      try {
        Settings settings = ((UimaContext_ImplBase)aContext).getExternalOverrides();
        actual = settings.lookUp("test.externalFloatArray");
      } catch (ResourceConfigurationException e) {
        Assert.fail(e.getMessage());
      }
      String expected = "[]";
      Assert.assertEquals(expected, actual);
      
      // Test a stand-alone settings object
      Settings testSettings = new Settings_impl();
      String lines = "foo = ${bar} \n bar : [ok \n OK] \n bad = ${missing}";
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
      } catch (Exception e) {
        Assert.fail(e.toString());
      }
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
