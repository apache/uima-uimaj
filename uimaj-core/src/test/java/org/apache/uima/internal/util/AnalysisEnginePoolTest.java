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

package org.apache.uima.internal.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.lang.invoke.MethodHandles;

import org.apache.uima.Constants;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.impl.AnalysisEngineDescription_impl;
import org.apache.uima.analysis_engine.impl.ResultSpecification_impl;
import org.apache.uima.analysis_engine.impl.TestAnnotator;
import org.apache.uima.analysis_engine.metadata.impl.FixedFlow_impl;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.impl.Capability_impl;
import org.apache.uima.resource.metadata.impl.ConfigurationParameter_impl;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AnalysisEnginePoolTest {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @BeforeEach
  void setUp() throws Exception {
    mSimpleDesc = new AnalysisEngineDescription_impl();
    mSimpleDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
    mSimpleDesc.setPrimitive(true);
    mSimpleDesc.getMetaData().setName("Test Primitive TAE");
    mSimpleDesc
            .setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
    mSimpleDesc.getMetaData().setName("Simple Test");
    Capability cap = new Capability_impl();
    cap.addOutputType("NamedEntity", true);
    cap.addOutputType("DocumentStructure", true);
    var caps = new Capability[] { cap };
    mSimpleDesc.getAnalysisEngineMetaData().setCapabilities(caps);
  }

  @Test
  void testGetAnalysisEngineMetaData() throws Exception {
    AnalysisEnginePool pool = null;
    try {
      // create pool
      pool = new AnalysisEnginePool("taePool", 3, mSimpleDesc);

      var tae = pool.getAnalysisEngine();
      var md = tae.getAnalysisEngineMetaData();
      assertThat(md).isNotNull();
      assertThat(md.getName()).isEqualTo("Simple Test");
    } finally {
      if (pool != null) {
        pool.destroy();
      }
    }
  }

  @Test
  void testProcess() throws Exception {
    // test simple primitive MultithreadableTextAnalysisEngine
    // (using TestAnnotator class)
    var pool = new AnalysisEnginePool("taePool", 3, mSimpleDesc);
    _testProcess(pool, 0);

    // test simple aggregate MultithreadableTextAnalysisEngine
    // (again using TestAnnotator class)
    AnalysisEngineDescription aggDesc = new AnalysisEngineDescription_impl();
    aggDesc.setPrimitive(false);
    aggDesc.getMetaData().setName("Test Aggregate TAE");
    aggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", mSimpleDesc);
    var flow = new FixedFlow_impl();
    flow.setFixedFlow("Test");
    aggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
    pool = new AnalysisEnginePool("taePool", 3, aggDesc);
    _testProcess(pool, 0);

    // multiple threads!
    final var NUM_THREADS = 4;
    var threads = new ProcessThread[NUM_THREADS];
    for (var i = 0; i < NUM_THREADS; i++) {
      threads[i] = new ProcessThread(pool, i);
      threads[i].start();
    }

    MultiThreadUtils.kickOffThreads(threads);

    MultiThreadUtils.waitForAllReady(threads);

    for (var i = 0; i < NUM_THREADS; i++) {
      var failure = threads[i].getFailure();
      if (failure != null) {
        if (failure instanceof Exception) {
          throw (Exception) failure;
        } else {
          fail(failure.getMessage());
        }
      }
    }

    // Check TestAnnotator fields only at the very end of processing,
    // we can't test from the threads themsleves since the state of
    // these fields is nondeterministic during the multithreaded processing.
    assertThat(TestAnnotator.getLastDocument()).isEqualTo("testing...");
    var resultSpec = new ResultSpecification_impl(
            TestAnnotator.getLastResultSpec().getTypeSystem());
    resultSpec.addResultType("NamedEntity", true);
    assertThat(TestAnnotator.getLastResultSpec()).isEqualTo(resultSpec);

    MultiThreadUtils.terminateThreads(threads);
  }

  @Test
  void testReconfigure() throws Exception {
    // create simple primitive TextAnalysisEngine descriptor (using TestAnnotator class)
    var primitiveDesc = new AnalysisEngineDescription_impl();
    primitiveDesc.setPrimitive(true);
    primitiveDesc.getMetaData().setName("Test Primitive TAE");
    primitiveDesc
            .setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
    ConfigurationParameter p1 = new ConfigurationParameter_impl();
    p1.setName("StringParam");
    p1.setDescription("parameter with String data type");
    p1.setType(ConfigurationParameter.TYPE_STRING);
    primitiveDesc.getMetaData().getConfigurationParameterDeclarations()
            .setConfigurationParameters(p1);
    primitiveDesc.getMetaData().getConfigurationParameterSettings().setParameterSettings(
            new NameValuePair[] { new NameValuePair_impl("StringParam", "Test1") });

    // create pool
    var pool = new AnalysisEnginePool("taePool", 3, primitiveDesc);

    var tae = pool.getAnalysisEngine();
    try {
      // check value of string param (TestAnnotator saves it in a static field)
      assertThat(TestAnnotator.stringParamValue).isEqualTo("Test1");

      // reconfigure
      tae.setConfigParameterValue("StringParam", "Test2");
      tae.reconfigure();

      // test again
      assertThat(TestAnnotator.stringParamValue).isEqualTo("Test2");

      // check pool metadata
      pool.getMetaData().setUUID(tae.getMetaData().getUUID());
      assertThat(pool.getMetaData()).isEqualTo(tae.getMetaData());
    } finally {
      pool.releaseAnalysisEngine(tae);
    }
  }

  protected void _testProcess(AnalysisEnginePool aPool, int i) throws UIMAException {
    var tae = aPool.getAnalysisEngine(0);
    try {
      // Test each form of the process method. When TestAnnotator executes, it
      // stores in static fields the document text and the ResultSpecification.
      // We use those to make sure the information propagates correctly to the annotator.

      // process(CAS)
      var tcas = tae.newCAS();
      tcas.setDocumentText("new test");
      tae.process(tcas);
      tcas.reset();

      // process(CAS,ResultSpecification)
      ResultSpecification resultSpec = new ResultSpecification_impl(tcas.getTypeSystem());
      resultSpec.addResultType("NamedEntity", true);

      tcas.setDocumentText("testing...");
      tae.process(tcas, resultSpec);
      tcas.reset();
    } finally {
      aPool.releaseAnalysisEngine(tae);
    }
  }

  class ProcessThread extends MultiThreadUtils.ThreadM {
    ProcessThread(AnalysisEnginePool aPool, int aId) {
      mPool = aPool;
      mId = aId;
    }

    @Override
    public void run() {
      while (true) {
        try {
          if (!MultiThreadUtils.wait4go(this)) {
            break;
          }
          LOG.trace("thread started");
          _testProcess(mPool, mId);
          LOG.trace("thread finished");
        } catch (Throwable t) {
          LOG.error("thread failed", t);
          // can't cause unit test to fail by throwing exception from thread.
          // record the failure and the main thread will check for it later.
          mFailure = t;
        }
      }
    }

    public synchronized Throwable getFailure() {
      return mFailure;
    }

    int mId;

    AnalysisEnginePool mPool;

    boolean mIsAggregate;

    Throwable mFailure = null;
  }

  private AnalysisEngineDescription mSimpleDesc;
}
