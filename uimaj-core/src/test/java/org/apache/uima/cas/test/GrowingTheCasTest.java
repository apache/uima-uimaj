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
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas_data.impl.CasComparer;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCopier;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;

/**
 * Class comment for IteratorTest.java goes here.
 * 
 */
public class GrowingTheCasTest extends TestCase {

  private AnalysisEngine ae = null;

  private JCas smallHeapCas = null;

  private JCas jcas;

  public GrowingTheCasTest(String arg0) {
    super(arg0);
  }

  public void setUp() {
    File descriptorFile = JUnitExtension.getFile("CASTests/desc/TokensAndSentences.xml");
    assertTrue("Descriptor must exist: " + descriptorFile.getAbsolutePath(), descriptorFile
	.exists());

    try {
      XMLParser parser = UIMAFramework.getXMLParser();
      AnalysisEngineDescription spec = (AnalysisEngineDescription) parser.parse(new XMLInputSource(
	  descriptorFile));
      this.ae = UIMAFramework.produceAnalysisEngine(spec);
      Properties props = new Properties();
      props.setProperty(UIMAFramework.CAS_INITIAL_HEAP_SIZE, "0");
      this.smallHeapCas = CasCreationUtils.createCas(spec, props).getJCas();
    } catch (IOException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (InvalidXMLException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (ResourceInitializationException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (CASException e) {
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
  
  // rename to test to run this test
  public void tstIteratorPerf() {
//    Properties props = System.getProperties();
//    for (Map.Entry es : props.entrySet()) {
//      System.out.format("JVM Prop %s: %s%n",  es.getKey(), es.getValue());
//    }

//    System.out.format("JVM total memory: %,d, JVM Max Mem: %,d%n", Runtime.getRuntime().totalMemory(), Runtime.getRuntime().maxMemory());
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
    jcas = null;
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
//      long time = System.currentTimeMillis();
      this.ae.process(jcas);
//      time = System.currentTimeMillis() - time;
//      System.out.println("Time for large CAS: " + new TimeSpan(time));
      numberOfSentences = jcas.getAnnotationIndex(Sentence.type).size();
      numberOfTokens = jcas.getAnnotationIndex(Token.type).size();
      System.out.println("Moby * 10, nbr of sentences = " + numberOfSentences);
      System.out.println("Moby * 10, nbr of tokens = " + numberOfTokens);
    } catch (AnalysisEngineProcessException e) {
      e.printStackTrace();
      assertTrue(false);
    }
    
    // performance testing of "unordered" iterators
    for (int i = 0; i < 10; i++) {
      timeIt(i);
    }
    
    // performance testing of CasCopier
    
    // create a destination CAS
    CAS destCas;
    try {
      destCas = this.ae.newCAS();
    } catch (ResourceInitializationException e) {
      e.printStackTrace();
      assertTrue(false);
      return;  // to avoid compile problems
    }
    CAS srcCas = jcas.getCas();

    CasCopier copier;
    // do the copy
    long shortest = Long.MAX_VALUE;
    int i = 0;
    for (; i < 200; i++) {  // uncomment for perf test.  was more than 5x faster than version 2.6.0
      destCas.reset();
      long startTime = System.nanoTime();
      copier = new CasCopier(srcCas, destCas);
      copier.copyCasView(srcCas, true);
      long time = (System.nanoTime() - startTime)/ 1000;
      if (time < shortest) {
        shortest = time;
        System.out.format("CasCopier speed for Moby is %,d microseconds on iteration %,d%n", shortest, i);
      }
    }
    
    // verify copy
    CasComparer.assertEquals(srcCas, destCas);

//    ((JCasImpl)jcas).showJfsFromCaddrHistogram();
    jcas= null;
  }

  private void timeIt(int i) {
    FSIterator<FeatureStructure> it = jcas.getIndexRepository().getAllIndexedFS(jcas.getCasType(Annotation.type));   
    int c = 0;
    long startTime = System.nanoTime();
    while (it.hasNext()) {
      it.next();
//      it.ll_get();
//      it.moveToNext();
      c ++;
    }
//    if ((i % 2) == 0) {
      System.out.format("%,d Moby * 10, nbr of annotations = %,d; took %,d microsec%n",
          i, c, (System.nanoTime() - startTime) / 1000);
//    }
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
    for (int i = 0; i < 1; i++) {
      numberOfSentences = 0;
      numberOfTokens = 0;
      try {
  //      long time = System.currentTimeMillis();
        this.ae.process(jcas);
  //      time = System.currentTimeMillis() - time;
  //      System.out.println("Time for large CAS: " + new TimeSpan(time));
        numberOfSentences = jcas.getAnnotationIndex(Sentence.type).size();
        numberOfTokens = jcas.getAnnotationIndex(Token.type).size();
  //      System.out.println(numberOfSentences);
  //      System.out.println(numberOfTokens);
      } catch (AnalysisEngineProcessException e) {
        e.printStackTrace();
        assertTrue(false);
      }
      jcas.reset();
      jcas.setDocumentText(text);
    }
    jcas = null;
    
    this.smallHeapCas.setDocumentText(text);
    try {
//      long time = System.currentTimeMillis();
      this.ae.process(this.smallHeapCas);
//      time = System.currentTimeMillis() - time;
//      System.out.println("Time for small CAS: " + new TimeSpan(time));
      assertTrue(this.getClass().toString() + ": number of sentences does not match",
	  numberOfSentences == this.smallHeapCas.getAnnotationIndex(Sentence.type).size());
      assertTrue(this.getClass().toString() + ": number of tokens does not match",
	  numberOfTokens == this.smallHeapCas.getAnnotationIndex(Token.type).size());
//    try {  // uncomment for memory use profiling
//      Thread.sleep(10000000);
//    } catch (InterruptedException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }  // debug
//    Runtime r = Runtime.getRuntime();
//    System.out.format("Size of 2 heaps: %,d%n", r.totalMemory() - r.freeMemory());
//    System.gc();
//    System.gc();
//    System.out.format("Size of 2 heaps: %,d after 2 gcs%n", r.totalMemory() - r.freeMemory());
//
    } catch (AnalysisEngineProcessException e) {
      e.printStackTrace();
      assertTrue(false);
    } finally {
      smallHeapCas = null;  // some junit runners hold onto instances of the test class after the test is run
    }
//    jcas = null;
//    System.out.format("Size of 0 heaps: %,d%n", r.totalMemory() - r.freeMemory());
//    System.gc();
//    System.gc();
//    System.out.format("Size of 0 heaps: %,d after 2 gcs%n", r.totalMemory() - r.freeMemory());   
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(GrowingTheCasTest.class);
  }

}
