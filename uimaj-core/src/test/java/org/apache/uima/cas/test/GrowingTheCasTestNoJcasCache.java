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

package org.apache.uima.cas.test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;

/**
 * Class comment for IteratorTest.java goes here.
 * 
 */
public class GrowingTheCasTestNoJcasCache extends TestCase {

  private AnalysisEngine ae = null;

  private JCas smallHeapCas = null;

  public GrowingTheCasTestNoJcasCache(String arg0) {
    super(arg0);
  }

  public void setUp() {
    File descriptorFile = JUnitExtension.getFile("CASTests/desc/TokensAndSentences.xml");
    assertTrue("Descriptor must exist: " + descriptorFile.getAbsolutePath(), descriptorFile
        .exists());

    try {
      XMLParser parser = UIMAFramework.getXMLParser();
      ResourceSpecifier spec = parser.parseResourceSpecifier(new XMLInputSource(
          descriptorFile));
      // Create a new properties object to hold the settings.
      Properties performanceTuningSettings = new Properties();
      // Set the initial CAS heap size.
      performanceTuningSettings.setProperty(
            UIMAFramework.CAS_INITIAL_HEAP_SIZE, 
            "1000000");
      // Disable JCas cache.
      performanceTuningSettings.setProperty(
            UIMAFramework.JCAS_CACHE_ENABLED, 
            "false");
      // Create a wrapper properties object that can
      // be passed to the framework.
      Map<String, Object> additionalParams = new HashMap<String, Object>();
      // Set the performance tuning properties as value to
      // the appropriate parameter.
      additionalParams.put(
            Resource.PARAM_PERFORMANCE_TUNING_SETTINGS, 
            performanceTuningSettings);
      // Create the analysis engine with the parameters.
      // The second, unused argument here is a custom 
      // resource manager.
      this.ae = UIMAFramework.produceAnalysisEngine(
          spec, null, additionalParams);
      this.smallHeapCas = this.ae.newJCas();
    } catch (IOException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (InvalidXMLException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (ResourceInitializationException e) {
      e.printStackTrace();
      assertTrue(false);
    }

  }

  public void tearDown() {
    if (this.ae != null) {
      this.ae.destroy();
      this.ae = null;
    }
  }

  public void testAnnotator() {
    File textFile = JUnitExtension.getFile("data/moby.txt");
    String text = null;
    try {
      text = FileUtils.file2String(textFile, "utf-8");
    } catch (IOException e) {
      e.printStackTrace();
      assertTrue(false);
    }
    StringBuffer buf = new StringBuffer(text.length() * 10);
    for (int i = 0; i < 10; i++) {
      buf.append(text);
    }
    JCas jcas = null;
    try {
      jcas = this.ae.newJCas();
    } catch (ResourceInitializationException e) {
      e.printStackTrace();
      assertTrue(false);
    }
    text = buf.toString();
    jcas.setDocumentText(text);
    int numberOfSentences = 0;
    int numberOfTokens = 0;
    try {
      // long time = System.currentTimeMillis();
      this.ae.process(jcas);
      // time = System.currentTimeMillis() - time;
      // System.out.println("Time for large CAS: " + new TimeSpan(time));
      numberOfSentences = jcas.getAnnotationIndex(Sentence.type).size();
      numberOfTokens = jcas.getAnnotationIndex(Token.type).size();
      // System.out.println(numberOfSentences);
      // System.out.println(numberOfTokens);
    } catch (AnalysisEngineProcessException e) {
      e.printStackTrace();
      assertTrue(false);
    }
    this.smallHeapCas.setDocumentText(text);
    try {
      // long time = System.currentTimeMillis();
      this.ae.process(this.smallHeapCas);
      // time = System.currentTimeMillis() - time;
      // System.out.println("Time for small CAS: " + new TimeSpan(time));
      assertTrue(this.getClass().toString() + ": number of sentences does not match",
          numberOfSentences == this.smallHeapCas.getAnnotationIndex(Sentence.type).size());
      assertTrue(this.getClass().toString() + ": number of tokens does not match",
          numberOfTokens == this.smallHeapCas.getAnnotationIndex(Token.type).size());
    } catch (AnalysisEngineProcessException e) {
      e.printStackTrace();
      assertTrue(false);
    } finally {
      smallHeapCas = null;  // some junit runners hold onto instances of test classes after the test finishes
    }
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(GrowingTheCasTestNoJcasCache.class);
  }

}
