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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.uima.Constants;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.ae.multiplier.SimpleCasGenerator;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.impl.FixedFlow_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.Capability_impl;
import org.apache.uima.resource.metadata.impl.ConfigurationParameter_impl;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;


public class MultiprocessingAnalysisEngine_implTest extends TestCase {

  private AnalysisEngineDescription mSimpleDesc;

  private AnalysisEngineDescription mAggDesc;

  public volatile TypeSystem mLastTypeSystem;

  /**
   * Constructor for MultiprocessingAnalysisEngine_implTest.
   * 
   * @param arg0
   */
  public MultiprocessingAnalysisEngine_implTest(String arg0) {
    super(arg0);
  }

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    try {
      super.setUp();
      mSimpleDesc = new AnalysisEngineDescription_impl();
      mSimpleDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
      mSimpleDesc.setPrimitive(true);
      mSimpleDesc
              .setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      mSimpleDesc.getMetaData().setName("Simple Test");
      TypeSystemDescription typeSys = new TypeSystemDescription_impl();
      typeSys.addType("foo.Bar", "test", "uima.tcas.Annotation");
      typeSys.addType("NamedEntity", "test", "uima.tcas.Annotation");
      typeSys.addType("DocumentStructure", "test", "uima.tcas.Annotation");
      mSimpleDesc.getAnalysisEngineMetaData().setTypeSystem(typeSys);
      Capability cap = new Capability_impl();
      cap.addOutputType("NamedEntity", true);
      cap.addOutputType("DocumentStructure", true);
      Capability[] caps = new Capability[] {cap};
       mSimpleDesc.getAnalysisEngineMetaData().setCapabilities(caps);

      mAggDesc = new AnalysisEngineDescription_impl();
      mAggDesc.setPrimitive(false);
      mAggDesc.getMetaData().setName("Simple Test Aggregate");
      mAggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", mSimpleDesc);
      FixedFlow_impl flow = new FixedFlow_impl();
      flow.setFixedFlow(new String[] { "Test" });
      mAggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
      mAggDesc.getAnalysisEngineMetaData().setCapabilities(caps);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testInitialize() throws Exception {
    try {
      // initialize MultiprocesingTextAnalysisEngine
      MultiprocessingAnalysisEngine_impl mtae = new MultiprocessingAnalysisEngine_impl();
      boolean result = mtae.initialize(mSimpleDesc, null);
      Assert.assertTrue(result);

      // initialize again - should fail
      Exception ex = null;
      try {
        mtae.initialize(mSimpleDesc, null);
      } catch (UIMA_IllegalStateException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // initialize a new TAE with parameters
      Map<String, Object> map = new HashMap<String, Object>();
      map.put(AnalysisEngine.PARAM_NUM_SIMULTANEOUS_REQUESTS, Integer.valueOf(5));
      map.put(AnalysisEngine.PARAM_TIMEOUT_PERIOD, Integer.valueOf(60000));
      MultiprocessingAnalysisEngine_impl mtae2 = new MultiprocessingAnalysisEngine_impl();
      result = mtae2.initialize(mSimpleDesc, map);
      Assert.assertTrue(result);
      // check parameter values
      Assert.assertEquals(5, mtae2.getPool().getSize());
      Assert.assertEquals(60000, mtae2.getTimeout());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetAnalysisEngineMetaData() throws Exception {
    try {
      MultiprocessingAnalysisEngine_impl mtae = new MultiprocessingAnalysisEngine_impl();
      boolean result = mtae.initialize(mSimpleDesc, null);
      Assert.assertTrue(result);

      AnalysisEngineMetaData md = mtae.getAnalysisEngineMetaData();
      Assert.assertNotNull(md);
      Assert.assertEquals("Simple Test", md.getName());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testNewCAS() throws Exception {
    try {
      MultiprocessingAnalysisEngine_impl mtae = new MultiprocessingAnalysisEngine_impl();
      boolean result = mtae.initialize(mSimpleDesc, null);
      Assert.assertTrue(result);

      CAS cas1 = mtae.newCAS();
      // should have the type foo.Bar
      assertNotNull(cas1.getTypeSystem().getType("foo.Bar"));

      // should be able to get as many as we want and they should all be different
      CAS cas2 = mtae.newCAS();
      Assert.assertNotNull(cas2);
      Assert.assertTrue(cas1 != cas2);
      CAS cas3 = mtae.newCAS();
      Assert.assertNotNull(cas3);
      Assert.assertTrue(cas1 != cas3);
      Assert.assertTrue(cas2 != cas3);
      CAS cas4 = mtae.newCAS();
      Assert.assertNotNull(cas4);
      Assert.assertTrue(cas1 != cas4);
      Assert.assertTrue(cas2 != cas4);
      Assert.assertTrue(cas3 != cas4);

      // try aggregate
      MultiprocessingAnalysisEngine_impl mtae2 = new MultiprocessingAnalysisEngine_impl();
      result = mtae2.initialize(mAggDesc, null);
      Assert.assertTrue(result);

      CAS cas5 = mtae2.newCAS();
      // should have the type foo.Bar
      assertNotNull(cas5.getTypeSystem().getType("foo.Bar"));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  
  public void testProcess() throws Exception {
    try {
      // test simple primitive MultiprocessingTextAnalysisEngine
      // (using TestAnnotator class)
      _testProcess(mSimpleDesc, 0);

      // test simple aggregate MultiprocessingTextAnalysisEngine
      // (again using TestAnnotator class)

      _testProcess(mAggDesc, 0);

      // multiple threads!
      MultiprocessingAnalysisEngine_impl ae = new MultiprocessingAnalysisEngine_impl();
      Map<String, Object> params = new HashMap<String, Object>();
      params.put(AnalysisEngine.PARAM_NUM_SIMULTANEOUS_REQUESTS, Integer.valueOf(3));
      ae.initialize(mAggDesc, params);
      
      final int NUM_THREADS = 4;
      ProcessThread[] threads = new ProcessThread[NUM_THREADS];
      Random random = new Random();
      for (int repetitions = 0; repetitions < 4; repetitions++) {
        for (int i = 0; i < NUM_THREADS; i++) {
          threads[i] = new ProcessThread(ae);
          threads[i].start();
          Thread.sleep(random.nextInt(2));  // delay between 0 and 2 milliseconds
        }

        // wait for threads to finish and check if they got exceptions
        for (int i = 0; i < NUM_THREADS; i++) {
          try {
            threads[i].join(10000);
          } catch (InterruptedException ie) {
            System.err.println("got unexpected Interrupted exception " + ie);
          }
          if (threads[i].isAlive()) {
            System.err.println("timeout waiting for thread to complete " + i);
            fail("timeout waiting for thread to complete " + i);
          }

          Throwable failure = threads[i].getFailure();
          if (failure != null) {
            if (failure instanceof Exception) {
              throw (Exception) failure;
            } else {
              fail(failure.getMessage());
            }
          }
        }
      }
      
      //Check TestAnnotator fields only at the very end of processing,
      //we can't test from the threads themselves since the state of
      //these fields is nondeterministic during the multithreaded processing.
      assertEquals("testing...", TestAnnotator.getLastDocument());
      ResultSpecification lastResultSpec = TestAnnotator.getLastResultSpec();
      ResultSpecification resultSpec = new ResultSpecification_impl(lastResultSpec.getTypeSystem());
      resultSpec.addResultType("NamedEntity", true);
      assertEquals(resultSpec, lastResultSpec);

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testProcessManyCM() throws Exception {
    //get Resource Specifier from XML file
    XMLInputSource in = new XMLInputSource("src/test/resources/ExampleTae/SimpleCasGenerator.xml");
    ResourceSpecifier specifier = 
        UIMAFramework.getXMLParser().parseResourceSpecifier(in);
    for (int i = 0; i < 10; i++) {
      processMany(specifier);
    }
  }
  
  public void testProcessManyAgg() throws Exception {
    //get Resource Specifier from XML file
    XMLInputSource in = new XMLInputSource("src/test/resources/ExampleTae/SimpleTestAggregate.xml");
    ResourceSpecifier specifier = 
        UIMAFramework.getXMLParser().parseResourceSpecifier(in);
    for (int i = 0; i < 10; i++) {
      processMany(specifier);
    }
  }
  
  final int NUM_THREADS = 5;
  final int NUM_INSTANCES = 3;
  
  public void processMany(ResourceSpecifier specifier) throws Exception {
    try {

      // multiple threads!
      MultiprocessingAnalysisEngine_impl ae = new MultiprocessingAnalysisEngine_impl();
      Map<String, Object> params = new HashMap<String, Object>();
      params.put(AnalysisEngine.PARAM_NUM_SIMULTANEOUS_REQUESTS, NUM_INSTANCES);

      ae.initialize(specifier , params);
      
      ProcessThreadMany[] threads = new ProcessThreadMany[NUM_THREADS];
      for (int i = 0; i < NUM_THREADS; i++) {
        threads[i] = new ProcessThreadMany(ae);
        threads[i].start();
      }

        // wait for threads to finish and check if they got exceptions
      for (int i = 0; i < NUM_THREADS; i++) {
        try {
          threads[i].join(10000);
        } catch (InterruptedException ie) {
          System.err.println("got unexpected Interrupted exception " + ie);
        }
        if (threads[i].isAlive()) {
          System.err.println("timeout waiting for thread to complete " + i);
          fail("timeout waiting for thread to complete " + i);
        }

        Throwable failure = threads[i].getFailure();
        if (failure != null) {
          if (failure instanceof Exception) {
            throw (Exception) failure;
          } else {
            fail(failure.getMessage());
          }
        }
      }
          
      //Check TestAnnotator fields only at the very end of processing,
      //we can't test from the threads themselves since the state of
      //these fields is nondeterministic during the multithreaded processing.
      assertEquals("testing...", SimpleCasGenerator.getLastDocument());
      ResultSpecification lastResultSpec = SimpleCasGenerator.getLastResultSpec();
      ResultSpecification resultSpec = new ResultSpecification_impl(lastResultSpec.getTypeSystem());
      resultSpec.addResultType("NamedEntity", true);
      assertEquals(resultSpec, lastResultSpec);

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testReconfigure() throws Exception {
    try {
      // create simple primitive TextAnalysisEngine descriptor (using TestAnnotator class)
      AnalysisEngineDescription primitiveDesc = new AnalysisEngineDescription_impl();
      primitiveDesc.setPrimitive(true);
      primitiveDesc
              .setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      primitiveDesc.getMetaData().setName("Reconfigure Test 1");
      ConfigurationParameter p1 = new ConfigurationParameter_impl();
      p1.setName("StringParam");
      p1.setDescription("parameter with String data type");
      p1.setType(ConfigurationParameter.TYPE_STRING);
      primitiveDesc.getMetaData().getConfigurationParameterDeclarations()
              .setConfigurationParameters(new ConfigurationParameter[] { p1 });
      primitiveDesc.getMetaData().getConfigurationParameterSettings().setParameterSettings(
              new NameValuePair[] { new NameValuePair_impl("StringParam", "Test1") });

      // instantiate MultiprocessingTextAnalysisEngine
      MultiprocessingAnalysisEngine_impl tae = new MultiprocessingAnalysisEngine_impl();
      tae.initialize(primitiveDesc, null);

      // check value of string param (TestAnnotator saves it in a static field)
      assertEquals("Test1", TestAnnotator.stringParamValue);

      // reconfigure
      tae.setConfigParameterValue("StringParam", "Test2");
      tae.reconfigure();

      // test again
      assertEquals("Test2", TestAnnotator.stringParamValue);

      // test aggregate TAE
      AnalysisEngineDescription aggDesc = new AnalysisEngineDescription_impl();
      aggDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
      aggDesc.setPrimitive(false);
      aggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", primitiveDesc);
      aggDesc.getMetaData().setName("Reconfigure Test 2");
      FixedFlow_impl flow = new FixedFlow_impl();
      flow.setFixedFlow(new String[] { "Test" });
      aggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
      ConfigurationParameter p2 = new ConfigurationParameter_impl();
      p2.setName("StringParam");
      p2.setDescription("parameter with String data type");
      p2.setType(ConfigurationParameter.TYPE_STRING);
      aggDesc.getMetaData().getConfigurationParameterDeclarations().setConfigurationParameters(
              new ConfigurationParameter[] { p2 });
      aggDesc.getMetaData().getConfigurationParameterSettings().setParameterSettings(
              new NameValuePair[] { new NameValuePair_impl("StringParam", "Test3") });
      // instantiate TextAnalysisEngine
      MultiprocessingAnalysisEngine_impl aggTae = new MultiprocessingAnalysisEngine_impl();
      aggTae.initialize(aggDesc, null);

      assertEquals("Test3", TestAnnotator.stringParamValue);

      // reconfigure
      aggTae.setConfigParameterValue("StringParam", "Test4");
      aggTae.reconfigure();

      // test again
      assertEquals("Test4", TestAnnotator.stringParamValue);

      // reconfigure WITHOUT setting that parameter
      aggTae.reconfigure();
      // test again
      assertEquals("Test4", TestAnnotator.stringParamValue);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  /**
   * Auxilliary method used by testProcess()
   * 
   * @param aTaeDesc
   *          description of TextAnalysisEngine to test
   * @param i
   *          thread identifier for multithreaded testing
   */
  protected void _testProcess(AnalysisEngineDescription aTaeDesc, int i) throws UIMAException {
    // create and initialize MultiprocessingTextAnalysisEngine
    MultiprocessingAnalysisEngine_impl tae = new MultiprocessingAnalysisEngine_impl();
    tae.initialize(aTaeDesc, null);

    // Test each form of the process method. When TestAnnotator executes, it
    // stores in static fields the document text and the ResultSpecification.
    // We use thse to make sure the information propogates correctly to the annotator.

    // process(CAS)
    CAS tcas = tae.newCAS();
    tcas.setDocumentText("new test");
    tae.process(tcas);
    assertEquals("new test", TestAnnotator.lastDocument);
    tcas.reset();

    // process(CAS,ResultSpecification)
    ResultSpecification resultSpec = new ResultSpecification_impl(tcas.getTypeSystem());
    resultSpec.addResultType("NamedEntity", true);

    tcas.setDocumentText("testing...");
    tae.process(tcas, resultSpec);
    assertEquals("testing...", TestAnnotator.lastDocument);
    assertEquals(resultSpec, TestAnnotator.lastResultSpec);
    tcas.reset();
  }

  class ProcessThread extends Thread {
    ProcessThread(AnalysisEngine aAE) {
      mAE = aAE;
    }

    public void run() {
      try {

        // Test each form of the process method. When TestAnnotator executes, it
        // stores in static fields the document text and the ResultSpecification.
        // We use thse to make sure the information propogates correctly to the 
        // annotator. (However, we can't check these until after the threads are
        // finished, as their state is nondeterministic during multithreaded
        // processing.)

        // process(CAS)
        CAS tcas = mAE.newCAS();
        mLastTypeSystem = tcas.getTypeSystem();
        tcas.setDocumentText("new test");
        mAE.process(tcas);
        tcas.reset();

        // process(CAS,ResultSpecification)
        ResultSpecification resultSpec = new ResultSpecification_impl(tcas.getTypeSystem());
        resultSpec.addResultType("NamedEntity", true);

        tcas.setDocumentText("testing...");
        mAE.process(tcas, resultSpec);
        tcas.reset();

      } catch (Throwable t) {
        t.printStackTrace();
        //can't cause unit test to fail by throwing exception from thread.
        //record the failure and the main thread will check for it later.
        mFailure = t;
      }
    }

    public synchronized Throwable getFailure() {
      return mFailure;
    }

    Throwable mFailure = null;
    
    AnalysisEngine mAE;
  }
  
  class ProcessThreadMany extends Thread {
    
    Throwable mFailure = null;
    
    AnalysisEngine mAE;
    
    ProcessThreadMany(AnalysisEngine aAE) {
      mAE = aAE;
    }

    public void run() {
      try {

        // Test each form of the process method. When TestAnnotator executes, it
        // stores in static fields the document text and the ResultSpecification.
        // We use thse to make sure the information propogates correctly to the 
        // annotator. (However, we can't check these until after the threads are
        // finished, as their state is nondeterministic during multithreaded
        // processing.)

        // process(CAS)
        for (int i = 0; i < 5; i++) {
          CAS tcas = mAE.newCAS();
          mLastTypeSystem = tcas.getTypeSystem();
          tcas.setDocumentText("new test");
          mAE.process(tcas);
          tcas.reset();
  
          // process(CAS,ResultSpecification)
          ResultSpecification resultSpec = new ResultSpecification_impl(tcas.getTypeSystem());
          resultSpec.addResultType("NamedEntity", true);
  
          tcas.setDocumentText("testing...");
          mAE.process(tcas, resultSpec);
          tcas.reset();
        }
      } catch (Throwable t) {
        t.printStackTrace();
        //can't cause unit test to fail by throwing exception from thread.
        //record the failure and the main thread will check for it later.
        mFailure = t;
      }
    }

    public synchronized Throwable getFailure() {
      return mFailure;
    }

  }

}
