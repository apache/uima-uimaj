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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.MultiThreadUtils;
import org.apache.uima.internal.util.MultiThreadUtils.ThreadM;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.Capability_impl;
import org.apache.uima.resource.metadata.impl.ConfigurationParameter_impl;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MultiprocessingAnalysisEngine_implTest {

  private final static boolean doSleeps = true; // running w/ dosleeps = false should show 100% cpu

  private AnalysisEngineDescription mSimpleDesc;

  private AnalysisEngineDescription mAggDesc;

  public volatile TypeSystem mLastTypeSystem;

  @BeforeEach
  public void setUp() throws Exception {
    try {
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
      mSimpleDesc.getAnalysisEngineMetaData().setCapabilities(cap);

      mAggDesc = new AnalysisEngineDescription_impl();
      mAggDesc.setPrimitive(false);
      mAggDesc.getMetaData().setName("Simple Test Aggregate");
      mAggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", mSimpleDesc);
      FixedFlow_impl flow = new FixedFlow_impl();
      flow.setFixedFlow("Test");
      mAggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
      mAggDesc.getAnalysisEngineMetaData().setCapabilities(cap);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testInitialize() throws Exception {
    try {
      // initialize MultiprocesingTextAnalysisEngine
      MultiprocessingAnalysisEngine_impl mtae = new MultiprocessingAnalysisEngine_impl();
      boolean result = mtae.initialize(mSimpleDesc, null);
      assertThat(result).isTrue();

      // initialize again - should fail
      Exception ex = null;
      try {
        mtae.initialize(mSimpleDesc, null);
      } catch (UIMA_IllegalStateException e) {
        ex = e;
      }
      assertThat(ex).isNotNull();

      // initialize a new TAE with parameters
      Map<String, Object> map = new HashMap<>();
      map.put(AnalysisEngine.PARAM_NUM_SIMULTANEOUS_REQUESTS, 5);
      map.put(AnalysisEngine.PARAM_TIMEOUT_PERIOD, 60000);
      MultiprocessingAnalysisEngine_impl mtae2 = new MultiprocessingAnalysisEngine_impl();
      result = mtae2.initialize(mSimpleDesc, map);
      assertThat(result).isTrue();
      // check parameter values
      assertThat(mtae2.getPool().getSize()).isEqualTo(5);
      assertThat(mtae2.getTimeout()).isEqualTo(60000);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testGetAnalysisEngineMetaData() throws Exception {
    try {
      MultiprocessingAnalysisEngine_impl mtae = new MultiprocessingAnalysisEngine_impl();
      boolean result = mtae.initialize(mSimpleDesc, null);
      assertThat(result).isTrue();

      AnalysisEngineMetaData md = mtae.getAnalysisEngineMetaData();
      assertThat(md).isNotNull();
      assertThat(md.getName()).isEqualTo("Simple Test");
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testNewCAS() throws Exception {
    try {
      MultiprocessingAnalysisEngine_impl mtae = new MultiprocessingAnalysisEngine_impl();
      boolean result = mtae.initialize(mSimpleDesc, null);
      assertThat(result).isTrue();

      CAS cas1 = mtae.newCAS();
      // should have the type foo.Bar
      assertNotNull(cas1.getTypeSystem().getType("foo.Bar"));

      // should be able to get as many as we want and they should all be different
      CAS cas2 = mtae.newCAS();
      assertThat(cas2).isNotNull();
      assertThat(cas1 != cas2).isTrue();
      CAS cas3 = mtae.newCAS();
      assertThat(cas3).isNotNull();
      assertThat(cas1 != cas3).isTrue();
      assertThat(cas2 != cas3).isTrue();
      CAS cas4 = mtae.newCAS();
      assertThat(cas4).isNotNull();
      assertThat(cas1 != cas4).isTrue();
      assertThat(cas2 != cas4).isTrue();
      assertThat(cas3 != cas4).isTrue();

      // try aggregate
      MultiprocessingAnalysisEngine_impl mtae2 = new MultiprocessingAnalysisEngine_impl();
      result = mtae2.initialize(mAggDesc, null);
      assertThat(result).isTrue();

      CAS cas5 = mtae2.newCAS();
      // should have the type foo.Bar
      assertNotNull(cas5.getTypeSystem().getType("foo.Bar"));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testProcess() throws Exception {
    try {
      // test simple primitive MultiprocessingTextAnalysisEngine
      // (using TestAnnotator class)
      _testProcess(mSimpleDesc, 0);

      // test simple aggregate MultiprocessingTextAnalysisEngine
      // (again using TestAnnotator class)

      _testProcess(mAggDesc, 0);

      /**
       * Multi threading ae processing tests
       * 
       * The goal is to run multiple AE's together at the same time, and repeat this.
       * 
       * The threads the AE's run on are created and started, once, ahead of time, since that takes
       * a while. When they start their run() method executes a wait on a waitnotify object; and
       * they sit until notified.
       * 
       * At the beginning, all threads are notified quickly, and start running their individual
       * test(s).
       * 
       * When the test is finished, the thread goes back into the wait4go state.
       * 
       */
      MultiprocessingAnalysisEngine_impl ae = new MultiprocessingAnalysisEngine_impl();
      Map<String, Object> params = new HashMap<>();
      params.put(AnalysisEngine.PARAM_NUM_SIMULTANEOUS_REQUESTS, 8);
      ae.initialize(mAggDesc, params);

      final int NUM_THREADS = Math.min(50, Runtime.getRuntime().availableProcessors() * 5);
      ProcessThread[] threads = new ProcessThread[NUM_THREADS];
      Random random = new Random();

      for (int i = 0; i < NUM_THREADS; i++) {
        threads[i] = new ProcessThread(ae);
        threads[i].start();
      }

      for (int repetitions = 0; repetitions < 4; repetitions++) {

        MultiThreadUtils.kickOffThreads(threads);

        // wait for all threads to finish and check if they got exceptions
        MultiThreadUtils.waitForAllReady(threads);

        for (ProcessThread pt : threads) {
          // try {
          // threads[i].join();
          // } catch (InterruptedException ie) {
          // System.err.println("got unexpected Interrupted exception " + ie);
          // }
          // if (threads[i].isAlive()) {
          // System.err.println("timeout waiting for thread to complete " + i);
          // fail("timeout waiting for thread to complete " + i);
          // }

          Throwable failure = pt.getFailure();
          if (failure != null) {
            if (failure instanceof Exception) {
              throw (Exception) failure;
            } else {
              fail(failure.getMessage());
            }
          }
        }
      }

      // Check TestAnnotator fields only at the very end of processing,
      // we can't test from the threads themselves since the state of
      // these fields is nondeterministic during the multithreaded processing.
      assertEquals("testing...", TestAnnotator.getLastDocument());
      ResultSpecification lastResultSpec = TestAnnotator.getLastResultSpec();
      ResultSpecification resultSpec = new ResultSpecification_impl(lastResultSpec.getTypeSystem());
      resultSpec.addResultType("NamedEntity", true);
      assertEquals(resultSpec, lastResultSpec);
      MultiThreadUtils.terminateThreads(threads);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testProcessManyCM() throws Exception {
    // get Resource Specifier from XML file
    XMLInputSource in = new XMLInputSource("src/test/resources/ExampleTae/SimpleCasGenerator.xml");
    ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
    for (int i = 0; i < 10; i++) {
      processMany(specifier);
    }
  }

  @Test
  public void testProcessManyAgg() throws Exception {
    // get Resource Specifier from XML file
    XMLInputSource in = new XMLInputSource("src/test/resources/ExampleTae/SimpleTestAggregate.xml");
    ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
    for (int i = 0; i < 10; i++) {
      processMany(specifier);
    }
  }

  // rename to run this in a loop
  public void tstLoopProcessManyAgg() throws Exception {
    XMLInputSource in = new XMLInputSource("src/test/resources/ExampleTae/SimpleTestAggregate.xml");
    ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
    Misc.timeLoops("ProcessManyAgg", 1000, () -> processMany(specifier));
  }

  final int NUM_THREADS = Math.min(50, Runtime.getRuntime().availableProcessors() * 5);
  final int NUM_INSTANCES = (int) (NUM_THREADS * .7);

  public void processMany(ResourceSpecifier specifier) throws Exception {
    try {

      // multiple threads!
      MultiprocessingAnalysisEngine_impl ae = new MultiprocessingAnalysisEngine_impl();
      Map<String, Object> params = new HashMap<>();
      params.put(AnalysisEngine.PARAM_NUM_SIMULTANEOUS_REQUESTS, NUM_INSTANCES);

      ae.initialize(specifier, params);

      ProcessThreadMany[] threads = new ProcessThreadMany[NUM_THREADS];
      for (int i = 0; i < NUM_THREADS; i++) {
        threads[i] = new ProcessThreadMany(ae);
        threads[i].start();
      }

      MultiThreadUtils.kickOffThreads(threads);

      // wait for all threads to finish and check if they got exceptions
      MultiThreadUtils.waitForAllReady(threads);

      for (ProcessThreadMany ptm : threads) {

        Throwable failure = ptm.getFailure();
        if (failure != null) {
          if (failure instanceof Exception) {
            throw (Exception) failure;
          } else {
            fail(failure.getMessage());
          }
        }
      }

      // Check TestAnnotator fields only at the very end of processing,
      // we can't test from the threads themselves since the state of
      // these fields is nondeterministic during the multithreaded processing.
      assertEquals("testing...", SimpleCasGenerator.getLastDocument());
      ResultSpecification lastResultSpec = SimpleCasGenerator.getLastResultSpec();
      ResultSpecification resultSpec = new ResultSpecification_impl(lastResultSpec.getTypeSystem());
      resultSpec.addResultType("NamedEntity", true);
      assertEquals(resultSpec, lastResultSpec);
      MultiThreadUtils.terminateThreads(threads);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  void testReconfigure() throws Exception {
    try {
      // create simple primitive TextAnalysisEngine descriptor (using TestAnnotator class)
      var primitiveDesc = new AnalysisEngineDescription_impl();
      primitiveDesc.setPrimitive(true);
      primitiveDesc
              .setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      primitiveDesc.getMetaData().setName("Reconfigure Test 1");

      var p1 = new ConfigurationParameter_impl();
      p1.setName("StringParam");
      p1.setDescription("parameter with String data type");
      p1.setType(ConfigurationParameter.TYPE_STRING);
      primitiveDesc.getMetaData().getConfigurationParameterDeclarations()
              .setConfigurationParameters(p1);
      primitiveDesc.getMetaData().getConfigurationParameterSettings()
              .setParameterSettings(new NameValuePair_impl("StringParam", "Test1"));

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
      var aggDesc = new AnalysisEngineDescription_impl();
      aggDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
      aggDesc.setPrimitive(false);
      aggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", primitiveDesc);
      aggDesc.getMetaData().setName("Reconfigure Test 2");

      var flow = new FixedFlow_impl();
      flow.setFixedFlow("Test");
      aggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);

      var p2 = new ConfigurationParameter_impl();
      p2.setName("StringParam");
      p2.setDescription("parameter with String data type");
      p2.setType(ConfigurationParameter.TYPE_STRING);
      p2.setOverrides("Test/StringParam");
      aggDesc.getMetaData().getConfigurationParameterDeclarations().setConfigurationParameters(p2);
      aggDesc.getMetaData().getConfigurationParameterSettings()
              .setParameterSettings(new NameValuePair_impl("StringParam", "Test3"));
      // instantiate TextAnalysisEngine
      var aggTae = new MultiprocessingAnalysisEngine_impl();
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

  class ProcessThread extends ThreadM {

    Throwable mFailure = null;

    AnalysisEngine mAE;

    ProcessThread(AnalysisEngine aAE) {
      mAE = aAE;
    }

    @Override
    public void run() {
      Random r = new Random();
      while (true) {

        if (!MultiThreadUtils.wait4go(this)) { // wait for go signal after all threads are setup.
          break; // time to terminate
        }

        try {

          // Test each form of the process method. When TestAnnotator executes, it
          // stores in static fields the document text and the ResultSpecification.
          // We use thse to make sure the information propogates correctly to the
          // annotator. (However, we can't check these until after the threads are
          // finished, as their state is nondeterministic during multithreaded
          // processing.)

          // process(CAS)
          CAS tcas = mAE.newCAS();
          // for (int i = 0; i < 1000; i++) { // uncomment to debug
          mLastTypeSystem = tcas.getTypeSystem();
          tcas.setDocumentText("new test");
          mAE.process(tcas);
          // System.out.println("Debug finished processing a cas");
          if (doSleeps) {
            Thread.sleep(0, r.nextInt(1000)); // 0 to 1 microseconds
          }
          tcas.reset();

          // process(CAS,ResultSpecification)
          ResultSpecification resultSpec = new ResultSpecification_impl(tcas.getTypeSystem());
          resultSpec.addResultType("NamedEntity", true);

          tcas.setDocumentText("testing...");
          if (doSleeps) {
            Thread.sleep(0, r.nextInt(1000)); // 0 to 1 microseconds
          }
          mAE.process(tcas, resultSpec);
          if (doSleeps) {
            Thread.sleep(0, r.nextInt(1000)); // 0 to 1 microseconds
          }
          tcas.reset();
          // }
        } catch (Throwable t) {
          t.printStackTrace();
          // can't cause unit test to fail by throwing exception from thread.
          // record the failure and the main thread will check for it later.
          mFailure = t;
        }
      }
    }

    public synchronized Throwable getFailure() {
      return mFailure;
    }

    public void resetFailure() {
      mFailure = null;
    }
  }

  class ProcessThreadMany extends ProcessThread {

    ProcessThreadMany(AnalysisEngine aAE) {
      super(aAE);
    }

    @Override
    public void run() {

      while (true) {

        if (!MultiThreadUtils.wait4go(this)) {
          break;
        }

        try {

          Random r = new Random();

          // Test each form of the process method. When TestAnnotator executes, it
          // stores in static fields the document text and the ResultSpecification.
          // We use thse to make sure the information propagates correctly to the
          // annotator. (However, we can't check these until after the threads are
          // finished, as their state is nondeterministic during multithreaded
          // processing.)

          // process(CAS)
          for (int i = 0; i < 5; i++) {
            CAS tcas = mAE.newCAS();
            mLastTypeSystem = tcas.getTypeSystem();
            tcas.setDocumentText("new test");
            mAE.process(tcas);
            Thread.sleep(0, r.nextInt(1000)); // between 0 and 1 microseconds
            tcas.reset();

            // process(CAS,ResultSpecification)
            ResultSpecification resultSpec = new ResultSpecification_impl(tcas.getTypeSystem());
            resultSpec.addResultType("NamedEntity", true);

            tcas.setDocumentText("testing...");
            Thread.sleep(0, r.nextInt(1000)); // between 0 and 1 microseconds
            mAE.process(tcas, resultSpec);
            Thread.sleep(0, r.nextInt(1000)); // between 0 and 1 microseconds
            tcas.reset();
          }
        } catch (Throwable t) {
          t.printStackTrace();
          // can't cause unit test to fail by throwing exception from thread.
          // record the failure and the main thread will check for it later.
          mFailure = t;
        }
      }
    }

    @Override
    public synchronized Throwable getFailure() {
      return mFailure;
    }

  }

}
