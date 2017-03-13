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

package org.apache.uima.collection.impl.cpm;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.annotator.JTextAnnotator;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.impl.cpm.utils.DescriptorMakeUtil;
import org.apache.uima.collection.impl.cpm.utils.FunctionErrorStore;
import org.apache.uima.collection.impl.cpm.utils.TestStatusCallbackListener;
import org.apache.uima.collection.impl.metadata.cpe.CpeDescriptorFactory;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.collection.metadata.CpeIntegratedCasProcessor;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.test.junit_extension.ManageOutputDevice;
import org.apache.uima.util.Level;

/**
 * The TestCase aims to test the behaviour of the cpm faced with different error scenarios<br>
 * 
 * In detail the TestCase currently comprises the following test scenarios:<br>
 * <ul>
 * <li> Function tests with errorhandling of the methods defined in
 * {@link org.apache.uima.analysis_engine.annotator.BaseAnnotator} and
 * {@link org.apache.uima.analysis_engine.annotator.JTextAnnotator. </li>
 * <li> CPM function tests concerning errorhandling </li>
 * </ul>
 * <p>
 * The first section of tests analyse the behaviour of the different methods implemented with the
 * {@link JTextAnnotator} Interface while throwing predefined exceptions. Therefore special helper
 * classes located in the {@link org.apache.uima.collection.impl.cpm.utils} are used. For instance
 * {@link DecriptorMakeUtil} generates the customized descriptors which throw the predefined
 * Exceptions. {@link FunctionErrorStore} is the class where all data about method calls and counts
 * are kept. That's just to point out some important once.
 * </p>
 * <p>
 * To offer a short introduction into the generell mode of operation have a look at the following
 * list:
 * </p>
 * <ul>
 * <li> generate the descriptors, with fit to the testcase. For instance an annotator which throws a
 * (runtime) exception after every 5th document. </li>
 * <li> create the cpe an set all needed values (error handling params f. instance) </li>
 * <li> [optional] add some mechanism to stop the cpm if it crashes in the test (timeouts or so)
 * </li>
 * <li> run the test and check for the results </li>
 * </ul>
 * 
 * Also have a look at <br>
 * 
 * @see org.apache.uima.collection.impl.cpm.CpmCasConsumer_ErrorTest
 * @see org.apache.uima.collection.impl.cpm.CpmCollectionReader_ErrorTest
 */
public class CpmAE_ErrorTest extends TestCase {

  private static final String FS = System.getProperties().getProperty("file.separator");

  public CpmAE_ErrorTest() {
    System.gc();
  }

  private void cpeProcessNoMsg(CollectionProcessingEngine cpe, TestStatusCallbackListener listener) throws Exception {
    UIMAFramework.getLogger().setLevel(Level.OFF);
    try {
      cpe.process();
      while (!listener.isFinished() && !listener.isAborted()) {
        Thread.sleep(5);
      }
    } finally {
      UIMAFramework.getLogger().setLevel(Level.INFO);
    }
  }
  
  /**
   * <b>testcase:</b> the process method throws multiple AnnotatorProcessException.<br>
   * <b>expected behaviour:</b><br>
   * the cpm should regular finish after processing all documents. No error and no abort should
   * occur.
   * 
   * @throws Exception -
   */
  public void testProcessWithAnnotatorProcessException() throws Exception {
    int documentCount = 20; // number of document to process
    int exceptionSequence = 4; // the sequence in which errors are produced
    ManageOutputDevice.setAllSystemOutputToNirvana();
    // setup CPM
    CollectionProcessingEngine cpe = setupCpm(documentCount, "AnnotatorProcessException",
            exceptionSequence, "process");

    // Create and register a Status Callback Listener
    TestStatusCallbackListener listener = new CollectionReaderStatusCallbackListener(cpe);
    cpe.addStatusCallbackListener(listener);
    cpeProcessNoMsg(cpe, listener);

    ManageOutputDevice.setAllSystemOutputToDefault();
    // check the results, if everything worked as expected
    assertEquals(
            "The cpm is still working or the collectionProcessComplete-method of the listener was not called.",
            true, listener.isFinished());
    assertEquals(
            "The cpm propably didn't finish correctly! The aborted method of the listener was called.",
            false, listener.isAborted());
    assertEquals("There are not as much exceptions as expected! ", countExceptions(documentCount,
            exceptionSequence), FunctionErrorStore.getCount());

  }

  /**
   * <b>testcase:</b> the process method throws multiple OutOfMemoryErrors.<br>
   * <b>expected behaviour:</b><br>
   * the cpm should regular finish after processing all documents. No error and no abort should
   * occur.
   * 
   * @throws Exception -
   */
  public void testProcessWithOutOfMemoryException() throws Exception {
    int documentCount = 20; // number of documents to process
    int exceptionSequence = 3; // the sequence in which errors are produced
    ManageOutputDevice.setAllSystemOutputToNirvana();

    // setup CPM
    CollectionProcessingEngine cpe = setupCpm(documentCount, "OutOfMemoryException",
            exceptionSequence, "process");

    // Create and register a Status Callback Listener
    TestStatusCallbackListener listener = new CollectionReaderStatusCallbackListener(cpe);
    cpe.addStatusCallbackListener(listener);
    cpeProcessNoMsg(cpe, listener);

    ManageOutputDevice.setAllSystemOutputToDefault();
    // check the results, if everything worked as expected
    assertEquals(
            "The cpm is still working or the collectionProcessComplete-method of the listener was not called.",
            true, listener.isFinished());
    assertEquals(
            "The cpm propably didn't finish correctly! The aborted method of the listener was called.",
            false, listener.isAborted());
    assertEquals("There are not as much exceptions as expected! ",
            (documentCount / exceptionSequence), FunctionErrorStore.getCount());
    // that's it.
  }

  /**
   * <b>testcase:</b> the process method throws multiple NullPointerExceptions.<br>
   * <b>expected behaviour:</b><br>
   * the cpm should regular finish after processing all documents. No error and no abort should
   * occur.
   * 
   * @throws Exception -
   */
  public void testProcessWithNullPointerException() throws Exception {
    int documentCount = 20; // number of documents to process
    int exceptionSequence = 3; // the sequence in which errors are produced
    ManageOutputDevice.setAllSystemOutputToNirvana();

    // setup CPM
    CollectionProcessingEngine cpe = setupCpm(documentCount, "NullPointerException",
            exceptionSequence, "process");

    // Create and register a Status Callback Listener
    TestStatusCallbackListener listener = new CollectionReaderStatusCallbackListener(cpe);
    cpe.addStatusCallbackListener(listener);
    cpeProcessNoMsg(cpe, listener);

    ManageOutputDevice.setAllSystemOutputToDefault();
    // check the results, if everything worked as expected
    assertEquals(
            "The cpm is still working or the collectionProcessComplete-method of the listener was not called.",
            true, listener.isFinished());
    assertEquals(
            "The cpm propably didn't finish correctly! The aborted method of the listener was called.",
            false, listener.isAborted());
    assertEquals("There are not as much exceptions as expected! ", countExceptions(documentCount,
            exceptionSequence), FunctionErrorStore.getCount());
    // that's it.
  }

  /**
   * <b>testcase:</b> the initialize method throws a NullPointerException.<br>
   * <b>expected behaviour:</b><br>
   * The cpm should not finish. Instead, the exception is passed back to the testscript. Neither the
   * collectionProcessComplete-, nor the aborted- method of the listener is called.
   * 
   * @throws Exception -
   */
  public void testInitializeWithNullPointerException() throws Exception {
    int documentCount = 20; // number of document to process
    int exceptionSequence = 1; // the sequence in which errors are produced
    boolean exceptionThrown = false;
    ManageOutputDevice.setAllSystemOutputToNirvana();

    TestStatusCallbackListener listener = null;
    try {
      // setup CPM
      CollectionProcessingEngine cpe = setupCpm(documentCount, "NullPointerException",
              exceptionSequence, "initialize");

      // Create and register a Status Callback Listener
      listener = new CollectionReaderStatusCallbackListener(cpe);
      cpe.addStatusCallbackListener(listener);
      cpeProcessNoMsg(cpe, listener);
    } catch (NullPointerException e) {
      // e.printStackTrace();
      exceptionThrown = true;

    }

    ManageOutputDevice.setAllSystemOutputToDefault();
    // check the results, if everything worked as expected
    assertEquals("The expected NullPointerException wasn't thrown!", true, exceptionThrown);
    assertEquals(
            "The cpm called the listener, that the cpm has finished - which normally could not be.",
            false, listener.isFinished());
    assertEquals("The aborted-method of the listener was called. (new behaviour?)", false, listener
            .isAborted());
    assertEquals("There are not as much exceptions as expected! ", 1, FunctionErrorStore.getCount());
  }

  /**
   * <b>testcase:</b> the initialize method throws an OutOfMemoryError.<br>
   * <b>expected behaviour:</b><br>
   * the cpm should regular finish, but don't process the documents. No error and no abort should
   * occur. the collectionProcessComplete -method of the listener is called.
   * 
   * @throws Exception -
   */
  public void testInitializeWithOutOfMemoryException() throws Exception {
    int documentCount = 20; // number of document to process
    int exceptionSequence = 1; // the sequence in which errors are produced
    ManageOutputDevice.setAllSystemOutputToNirvana();

    TestStatusCallbackListener listener = null;

    // setup CPM
    CollectionProcessingEngine cpe = setupCpm(documentCount, "OutOfMemoryException",
            exceptionSequence, "initialize");

    // Create and register a Status Callback Listener
    listener = new CollectionReaderStatusCallbackListener(cpe);
    cpe.addStatusCallbackListener(listener);
    cpeProcessNoMsg(cpe, listener);

    ManageOutputDevice.setAllSystemOutputToDefault();
    // check the results, if everything worked as expected
    assertEquals(
            "The cpm is still working or the collectionProcessComplete-method of listener was not called.",
            true, listener.isFinished());
    assertEquals(
            "The cpm propably didn't finish correctly! The aborted method of the listener was called.",
            false, listener.isAborted());
    assertEquals("There are not as much exceptions as expected! ", 1, FunctionErrorStore.getCount());
    // that's it.
  }

  /**
   * <b>testcase:</b> the initialize method throws an AnnotatorInitializationException.<br>
   * <b>expected behaviour:</b><br>
   * The cpm should not finish. Instead, the exception (a NullPointerException) is passed back to
   * the testclass. Neither the collectionProcessComplete-, nor the aborted- method of the listener
   * is called.
   * 
   * @throws Exception -
   */
  public void testInitializeWithAnnotatorInitializationException() throws Exception {
    int documentCount = 20; // number of document to process
    int exceptionSequence = 1; // the sequence in which errors are produced
    boolean exceptionThrown = false;
    ManageOutputDevice.setAllSystemOutputToNirvana();

    TestStatusCallbackListener listener = null;
    try {
      // setup CPM
      CollectionProcessingEngine cpe = setupCpm(documentCount, "AnnotatorInitializationException",
              exceptionSequence, "initialize");

      // Create and register a Status Callback Listener
      listener = new CollectionReaderStatusCallbackListener(cpe);
      cpe.addStatusCallbackListener(listener);
      cpeProcessNoMsg(cpe, listener);
    } catch (NullPointerException e) {
      // e.printStackTrace();
      exceptionThrown = true;

    }

    ManageOutputDevice.setAllSystemOutputToDefault();
    // check the results, if everything worked as expected
    assertEquals("The expected NullPointerException wasn't thrown!", true, exceptionThrown);
    assertEquals(
            "The cpm called the listener, that the cpm has finished - which normally could not be.",
            false, listener.isFinished());
    assertEquals("The aborted-method of the listener was called. (new behaviour?)", false, listener
            .isAborted());
    assertEquals("There are not as much exceptions as expected! ", 1, FunctionErrorStore.getCount());
  }

  /**
   * <b>testcase:</b> the initialize method throws an AnnotatorConfigurationException.<br>
   * <b>expected behaviour:</b><br>
   * The cpm should not finish. Instead, the exception (a NullPointerException) is passed back to
   * the testclass. Neither the collectionProcessComplete-, nor the aborted- method of the listener
   * is called.
   * 
   * @throws Exception -
   */
  public void testInitializeWithAnnotatorConfigurationException() throws Exception {
    int documentCount = 20; // number of documents processed
    int exceptionSequence = 1; // the sequence in which errors are produced
    boolean exceptionThrown = false;
    ManageOutputDevice.setAllSystemOutputToNirvana();

    TestStatusCallbackListener listener = null;
    try {
      // setup CPM
      CollectionProcessingEngine cpe = setupCpm(documentCount, "AnnotatorConfigurationException",
              exceptionSequence, "initialize");

      // Create and register a Status Callback Listener
      listener = new CollectionReaderStatusCallbackListener(cpe);
      cpe.addStatusCallbackListener(listener);
      cpeProcessNoMsg(cpe, listener);
    } catch (NullPointerException e) {
      // e.printStackTrace();
      exceptionThrown = true;

    }

    ManageOutputDevice.setAllSystemOutputToDefault();
    // check the results, if everything worked as expected
    assertEquals("The expected NullPointerException wasn't thrown!", true, exceptionThrown);
    assertEquals(
            "The cpm called the listener, that the cpm has finished - which normally could not be.",
            false, listener.isFinished());
    assertEquals("The aborted-method of the listener was called. (new behaviour?)", false, listener
            .isAborted());
    assertEquals("There are not as much exceptions as expected! ", 1, FunctionErrorStore.getCount());
  }

  // // TODO: Find a way to call the reconfigure-method for testing perpose.
  //	
  // public void testReconfigureWithAnnotatorConfigurationException() throws Exception{
  // int documentCount = 20; // number of documents processed
  // int exceptionSequence = 1; // the sequence in which errors are produced
  // boolean exceptionThrown = false;
  //
  // TestStatusCallbackListener listener = null;
  // try {
  // //setup CPM
  // CollectionProcessingEngine cpe =
  // setupCpm(
  // documentCount,
  // "AnnotatorConfigurationException",
  // exceptionSequence,
  // "reconfigure");
  //   
  // //Create and register a Status Callback Listener
  // listener = new CollectionReaderStatusCallbackListener(cpe);
  // cpe.addStatusCallbackListener(listener);
  // cpe.process();
  // //wait until cpm has finished
  // while (!listener.isFinished() && !listener.isAborted()) {
  // Thread.sleep(5);
  // }
  // } catch (NullPointerException e) {
  // // e.printStackTrace();
  // exceptionThrown = true;
  //   
  // }
  // // check the results, if everything worked as expected
  // assertEquals("The cpm didn't finish correctly! Abort was called.", false,
  // listener.isAborted());
  // assertEquals("The cpm did finish by calling the Listener.", false, listener.isFinished());
  // assertEquals("There are not as much exceptions as expected! ", 1,
  // FunctionErrorStore.getCount());
  // // that's it.
  // }

  /**
   * <b>testcase:</b> the initialize method throws an AnnotatorConfigurationException.<br>
   * <b>expected behaviour:</b><br>
   * The cpm should not finish. Instead, the exception (a NullPointerException) is passed back to
   * the testclass. Neither the collectionProcessComplete-, nor the aborted- method of the listener
   * is called.
   * 
   * @throws Exception -
   */
  public void testAeProcessingUnitThreadCount() throws Exception {
    int documentCount = 20; // number of documents processed
    int count = 5; // thread number
    TestStatusCallbackListener listener = null;
    // setup CPM
    Object[] objs = setupConfigurableCpm(documentCount);

    CpeDescription cpeDesc = (CpeDescription) objs[0];
    cpeDesc.setProcessingUnitThreadCount(count);

    CollectionProcessingEngine cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc, null,
            null);

    // Create and register a Status Callback Listener
    listener = new CollectionReaderStatusCallbackListener(cpe);
    cpe.addStatusCallbackListener(listener);
    cpe.process();
    // wait until cpm has finished
    while (!listener.isFinished() && !listener.isAborted()) {
      Thread.sleep(5);
    }
    // check the results, if everything worked as expected
    assertEquals("The cpm didn't finish correctly! Abort was called.", false, listener.isAborted());
    assertEquals("The cpm didn't finish by calling the Listener.", true, listener.isFinished());
    assertEquals("There are not as much ae's running as expected ", count, FunctionErrorStore
            .getAnnotatorCount());
    // that's it.
  }

  /**
   * <b>testcase:</b> set the errorRateThreshold to terminate and leave the default value at
   * 100/1000. Every 5th document an AnnotatorProcessException is thrown.
   * <code>&lt;errorRateThreshold action="terminate" value="100/1000"/&gt;</code><br>
   * <b>expected behaviour:</b><br>
   * The cpm should not finish. Instead, after 100 Exceptions + 1 the aborted -method is called by
   * the cpm. The cpm shut down.
   * 
   * @throws Exception -
   */
  public void testAeErrorRateThresholdTerminateDefault() throws Exception {
    int documentCount = 1000; // number of documents to process
    TestStatusCallbackListener listener = new TestStatusCallbackListener();
    int exceptionSequence = 5;
    String functionName = "process";
    String exceptionName = "AnnotatorProcessException";
    ManageOutputDevice.setAllSystemOutputToNirvana();

    // setup CPM
    Object[] objs = setupConfigurableCpm(documentCount, exceptionName, exceptionSequence,
            functionName);

    CpeDescription cpeDesc = (CpeDescription) objs[0];
    cpeDesc.setProcessingUnitThreadCount(1);
    CpeIntegratedCasProcessor integratedProcessor = (CpeIntegratedCasProcessor) objs[1];
    integratedProcessor.setActionOnMaxError("terminate");
    CollectionProcessingEngine cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc, null,
            null);

    // register a Status Callback Listener
    cpe.addStatusCallbackListener(listener);
    cpeProcessNoMsg(cpe, listener);

    ManageOutputDevice.setAllSystemOutputToDefault();
    // check the results, if everything worked as expected
    assertEquals(
            "The cpm called the listener, that the cpm has finished - which normally could not be.",
            false, listener.isFinished());
    assertEquals("The aborted-method of the listener wasn't called.", true, listener.isAborted());
    assertEquals("There are not as much exceptions as expected! ", (100 + 1), FunctionErrorStore
            .getCount());
    // that's it.
  }

  /**
   * <b>testcase:</b> set the errorRateThreshold to action:terminate and set the value to 5/1000.
   * Every 5th document an AnnotatorProcessException is thrown.
   * <code>&lt;errorRateThreshold action="terminate" value="5/1000"/&gt;</code><br>
   * <b>expected behaviour:</b><br>
   * The cpm should not finish. Instead, after 5 Exceptions + 1 the aborted -method is called by the
   * cpm. The cpm shut down.
   * 
   * @throws Exception -
   */
  public void testAeErrorRateThresholdTerminateModified1() throws Exception {
    int documentCount = 500; // number of documents to process
    TestStatusCallbackListener listener = new TestStatusCallbackListener();
    int exceptionSequence = 5;
    String functionName = "process";
    String exceptionName = "AnnotatorProcessException";
    ManageOutputDevice.setAllSystemOutputToNirvana();

    Object[] objs = setupConfigurableCpm(documentCount, exceptionName, exceptionSequence,
            functionName);

    CpeDescription cpeDesc = (CpeDescription) objs[0];
    cpeDesc.setProcessingUnitThreadCount(1);
    CpeIntegratedCasProcessor integratedProcessor = (CpeIntegratedCasProcessor) objs[1];
    integratedProcessor.setActionOnMaxError("terminate");
    integratedProcessor.setMaxErrorCount(5);
    CollectionProcessingEngine cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc, null,
            null);

    // Create and register a Status Callback Listener
    // listener = new CollectionReaderStatusCallbackListener(cpe);
    cpe.addStatusCallbackListener(listener);
    cpeProcessNoMsg(cpe, listener);

    ManageOutputDevice.setAllSystemOutputToDefault();
    // check the results, if everything worked as expected
    assertEquals(
            "The cpm called the listener, that the cpm has finished - which normally could not be.",
            false, listener.isFinished());
    assertEquals("The aborted-method of the listener wasn't called.", true, listener.isAborted());
    assertEquals("There are not as much exceptions as expected! ", (5 + 1), FunctionErrorStore
            .getCount());
  }

  /**
   * <b>testcase:</b> set the errorRateThreshold to action:terminate and set the value to 5/10.
   * Every 5th document an AnnotatorProcessException is thrown.
   * <code>&lt;errorRateThreshold action="terminate" value="5/10"/&gt;</code><br>
   * <b>expected behaviour:</b><br>
   * The cpm should finish, because this failure-rate is in the accepted range.
   * 
   * @throws Exception -
   */
  public void testAeErrorRateThresholdTerminateModified2() throws Exception {
    int exceptionSequence = 5;
    int documentCount = 100; // number of documents processed
    TestStatusCallbackListener listener = new TestStatusCallbackListener();
    String functionName = "process";
    String exceptionName = "AnnotatorProcessException";
    ManageOutputDevice.setAllSystemOutputToNirvana();

    // setup CPM
    Object[] objs = setupConfigurableCpm(documentCount, exceptionName, exceptionSequence,
            functionName);

    CpeDescription cpeDesc = (CpeDescription) objs[0];
    cpeDesc.setProcessingUnitThreadCount(1);
    CpeIntegratedCasProcessor integratedProcessor = (CpeIntegratedCasProcessor) objs[1];
    integratedProcessor.setActionOnMaxError("terminate");
    integratedProcessor.setMaxErrorCount(5);
    integratedProcessor.setMaxErrorSampleSize(10);
    CollectionProcessingEngine cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc, null,
            null);

    // Create and register a Status Callback Listener
    // listener = new CollectionReaderStatusCallbackListener(cpe);
    cpe.addStatusCallbackListener(listener);
    cpeProcessNoMsg(cpe, listener);

    ManageOutputDevice.setAllSystemOutputToDefault();
    // check the results, if everything worked as expected
    assertEquals(
            "The cpm is still working or the collectionProcessComplete-method of the listener was not called.",
            true, listener.isFinished());
    assertEquals("The cpm didn't finish correctly! Abort in the listener was called.", false,
            listener.isAborted());
    assertEquals("There are not as much exceptions as expected! ", countExceptions(documentCount,
            exceptionSequence), FunctionErrorStore.getCount());
    // that's it.
  }

  /**
   * <b>testcase:</b> set the errorRateThreshold to action:continue and set the value to 5/15.
   * Every 2nd document an AnnotatorProcessException is thrown.
   * <code>&lt;errorRateThreshold action="continue" value="5/15"/&gt;</code><br>
   * <b>expected behaviour:</b><br>
   * The cpm should finish, because of the continue action.
   * 
   * @throws Exception -
   */
  public void testAeErrorRateThresholdContinue() throws Exception {
    int exceptionSequence = 4;
    int documentCount = 20; // number of documents processed
    TestStatusCallbackListener listener = new TestStatusCallbackListener();
    String functionName = "process";
    String exceptionName = "AnnotatorProcessException";
    ManageOutputDevice.setAllSystemOutputToNirvana();

    Object[] objs = setupConfigurableCpm(documentCount, exceptionName, exceptionSequence,
            functionName);

    CpeDescription cpeDesc = (CpeDescription) objs[0];
    cpeDesc.setProcessingUnitThreadCount(1);
    CpeIntegratedCasProcessor integratedProcessor = (CpeIntegratedCasProcessor) objs[1];
    integratedProcessor.setActionOnMaxError("continue");
    integratedProcessor.setMaxErrorCount(5);
    integratedProcessor.setMaxErrorSampleSize(15);
    CollectionProcessingEngine cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc, null,
            null);

    // Create and register a Status Callback Listener
    // listener = new CollectionReaderStatusCallbackListener(cpe);
    cpe.addStatusCallbackListener(listener);
    cpeProcessNoMsg(cpe, listener);

    ManageOutputDevice.setAllSystemOutputToDefault();
    // check the results, if everything worked as expected
    assertEquals(
            "The cpm propably didn't finish correctly! The aborted method of the listener was called.",
            false, listener.isAborted());
    assertEquals("The cpm didn't finish by calling the Listener.", true, listener.isFinished());
    // TODO: write a function which calculates the expected number of occuring errors
    // assertEquals("There are not as much exceptions as expected! ", 44,
    // FunctionErrorStore.getCount());
  }

  /**
   * <b>testcase:</b> set the errorRateThreshold to action:disable and set the value to 5/15. Every
   * 2nd document an AnnotatorProcessException is thrown.
   * <code>&lt;errorRateThreshold action="disable" value="5/15"/&gt;</code><br>
   * <b>expected behaviour:</b><br>
   * The annotator should stop working. The cpm changes to the "isFinished" state.
   * 
   * @throws Exception -
   */
  public void testAeErrorRateThresholdDisable() throws Exception {
    int exceptionSequence = 2;
    int documentCount = 50; // number of documents processed
    TestStatusCallbackListener listener = new TestStatusCallbackListener();
    String functionName = "process";
    String exceptionName = "AnnotatorProcessException";
    ManageOutputDevice.setAllSystemOutputToNirvana();

    // setup CPM
    Object[] objs = setupConfigurableCpm(documentCount, exceptionName, exceptionSequence,
            functionName);

    CpeDescription cpeDesc = (CpeDescription) objs[0];
    cpeDesc.setProcessingUnitThreadCount(1);
    CpeIntegratedCasProcessor integratedProcessor = (CpeIntegratedCasProcessor) objs[1];
    integratedProcessor.setActionOnMaxError("disable");
    integratedProcessor.setMaxErrorCount(5);
    integratedProcessor.setMaxErrorSampleSize(15);
    CollectionProcessingEngine cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc, null,
            null);

    // Create and register a Status Callback Listener
    // listener = new CollectionReaderStatusCallbackListener(cpe);
    cpe.addStatusCallbackListener(listener);
    cpeProcessNoMsg(cpe, listener);

    ManageOutputDevice.setAllSystemOutputToDefault();
    // check the results, if everything worked as expected
    assertEquals(
            "The cpm propably didn't finish correctly! The aborted method of the listener was called.",
            false, listener.isAborted());
    assertEquals("The cpm didn't finish by calling the Listener.", true, listener.isFinished());
    assertEquals("There are not as much exceptions as expected! ", 6, FunctionErrorStore.getCount());
  }

  public void testAeErrorRateActionOnMaxRestarts() throws Exception {
    int exceptionSequence = 1;
    int documentCount = 10; // number of documents processed
    TestStatusCallbackListener listener = new TestStatusCallbackListener();
    String functionName = "process";
    String exceptionName = "AnnotatorProcessException";
    // disable System.out
    ManageOutputDevice.setAllSystemOutputToNirvana();
    Object[] objs = setupConfigurableCpm(documentCount, exceptionName, exceptionSequence,
            functionName);

    CpeDescription cpeDesc = (CpeDescription) objs[0];
    cpeDesc.setProcessingUnitThreadCount(1);
    CpeIntegratedCasProcessor integratedProcessor = (CpeIntegratedCasProcessor) objs[1];
    integratedProcessor.setActionOnMaxError("continue");
    integratedProcessor.setMaxErrorCount(3);
    integratedProcessor.setMaxErrorSampleSize(100);

    integratedProcessor.setActionOnMaxRestart("continue");
    integratedProcessor.setMaxRestartCount(0);
    CollectionProcessingEngine cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc, null,
            null);

    // Create and register a Status Callback Listener
    cpe.addStatusCallbackListener(listener);
    cpeProcessNoMsg(cpe, listener);
    // enable System.out
    ManageOutputDevice.setAllSystemOutputToDefault();
    // check the results, if everything worked as expected
    assertEquals(
            "The cpm propably didn't finish correctly! The aborted method of the listener was called.",
            false, listener.isAborted());
    assertEquals("The cpm didn't finish by calling the Listener.", true, listener.isFinished());
    assertEquals("There are not as many exceptions as expected:", 40, FunctionErrorStore.getCount());
  }

  /**
   * <b>testcase:</b> limit the process count of documents to a specific number
   * <code>&lt;numToProcess&gt;20&lt;/numToProcess&gt;</code><br>
   * <b>expected behaviour:</b><br>
   * After havening successfully processed the given number of documents the process should finish.
   * 
   * @throws Exception -
   */
  public void testNumToProcess() throws Exception {
    int exceptionSequence = 25;
    int documentCount = 50; // number of documents processed
    TestStatusCallbackListener listener = new TestStatusCallbackListener();
    String functionName = "process";
    String exceptionName = "AnnotatorProcessException";
    ManageOutputDevice.setAllSystemOutputToNirvana();

    // setup CPM
    Object[] objs = setupConfigurableCpm(documentCount, exceptionName, exceptionSequence,
            functionName);

    CpeDescription cpeDesc = (CpeDescription) objs[0];
    cpeDesc.setProcessingUnitThreadCount(1);
    cpeDesc.setNumToProcess(20);
    CpeIntegratedCasProcessor integratedProcessor = (CpeIntegratedCasProcessor) objs[1];
    integratedProcessor.setActionOnMaxError("terminate");
    CollectionProcessingEngine cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc, null,
            null);

    // Create and register a Status Callback Listener
    // listener = new CollectionReaderStatusCallbackListener(cpe);
    cpe.addStatusCallbackListener(listener);
    cpeProcessNoMsg(cpe, listener);

    ManageOutputDevice.setAllSystemOutputToDefault();
    // check the results, if everything worked as expected
    assertEquals(
            "The cpm propably didn't finish correctly! The aborted method of the listener was called.",
            false, listener.isAborted());
    assertEquals("The cpm didn't finish by calling the Listener.", true, listener.isFinished());
    assertEquals("There are not as much exceptions as expected! ", 0, FunctionErrorStore.getCount());
    assertEquals("There is a difference between the expected and the processed document number. ",
            20, FunctionErrorStore.getAnnotatorProcessCount());
  }

  /**
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
    FunctionErrorStore.resetCount();
//    System.gc();
  }

  /**
   * get the number of Exception that should be thrown by the component.
   * 
   * @param totalCount
   *          all documents that should be processed
   * @param errorSequence
   *          iteration rate of occuring errors
   * 
   * @return number of handled Exceptions an Annotator should throw.
   */
  private int countExceptions(int totalCount, int errorSequence) {
    int count = totalCount / errorSequence;
    int rest = totalCount % errorSequence;
    if ((rest + count) < errorSequence) {
      return count;
    }
    return count + countExceptions((rest + count), errorSequence);
  }

  /**
   * setup the CPM with base functionality.
   * 
   * @param documentCount
   *          how many documents should be processed
   * @param exceptionName
   *          the exception to be thrown
   * @param exceptionSequence
   *          the iteration rate of the exceptions
   * @param functionName
   *          the name of the function/method that throws the exception
   * 
   * @return CollectionProcessingEngine - initialized cpe
   */
  private CollectionProcessingEngine setupCpm(int documentCount, String exceptionName,
          int exceptionSequence, String functionName) {
    CpeDescription cpeDesc = null;
    CollectionProcessingEngine cpe = null;

    try {
      String colReaderBase = JUnitExtension.getFile(
              "CpmTests" + FS + "ErrorTestCollectionReader.xml").getAbsolutePath();
      String taeBase = JUnitExtension.getFile("CpmTests" + FS + "ErrorTestAnnotator.xml")
              .getAbsolutePath();
      String casConsumerBase = JUnitExtension.getFile("CpmTests" + FS + "ErrorTestCasConsumer.xml")
              .getAbsolutePath();

      // first, prepare all descriptors as needed
      String colReaderDesc = DescriptorMakeUtil.makeCollectionReader(colReaderBase, documentCount);
      String taeDesc = DescriptorMakeUtil.makeAnalysisEngine(taeBase, true, functionName,
              exceptionSequence, exceptionName);
      String casConsumerDesc = DescriptorMakeUtil.makeCasConsumer(casConsumerBase);

      // secondly, create the cpm based on the descriptors
      cpeDesc = CpeDescriptorFactory.produceDescriptor();

      // managing the default behaviour of this client
      CpeIntegratedCasProcessor integratedProcessor = CpeDescriptorFactory
              .produceCasProcessor("ErrorTestAnnotator");
      integratedProcessor.setDescriptor(taeDesc);

      CpeIntegratedCasProcessor casConsumer = CpeDescriptorFactory
              .produceCasProcessor("ErrorTest CasConsumer");
      casConsumer.setDescriptor(casConsumerDesc);

      // - add all descriptors
      cpeDesc.addCollectionReader(colReaderDesc);
      cpeDesc.addCasProcessor(integratedProcessor);
      cpeDesc.addCasProcessor(casConsumer);
      cpeDesc.setInputQueueSize(2);
      cpeDesc.setOutputQueueSize(2);
      cpeDesc.setProcessingUnitThreadCount(1);
      // - Create a new CPE
      cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc, null, null);
    } catch (Exception e) {
      // e.printStackTrace();
    }
    return cpe;
  }

  /**
   * a configuration method to setup a generell cpm without errors.
   * 
   * @param document
   *          number of how many documents are been read.
   * 
   * @return Object[] with the cpe-Descriptor at index 0 and the integratedProcessor at index 1
   * 
   * @see CpmAE_ErrorTest#setupConfigurableCpm(int, String, int , String)
   */
  private Object[] setupConfigurableCpm(int documentCount) {
    return setupConfigurableCpm(documentCount, null, 0, null);
  }

  /**
   * a configuration method to setup a generell cpm (with errors).
   * 
   * for errorhandling, the given error arguments are passed to the
   * {@link org.apache.uima.collection.impl.cpm.utils.ErrorTestAnnotator} class, which producess the
   * expected errors.
   * 
   * @param documentCount
   *          number of documents that are processed
   * @param exceptionName
   *          name of the the exception that should be thrown
   * @param exceptionSequence
   *          the iteration rate of the given exception or error
   * @param functionName
   *          the function in which the exception should be thrown
   * 
   * @return Object[] with the cpe-Descriptor at index 0 and the integratedProcessor at index 1
   */
  private Object[] setupConfigurableCpm(int documentCount, String exceptionName,
          int exceptionSequence, String functionName) {
    CpeDescription cpeDesc = null;
    CpeIntegratedCasProcessor integratedProcessor = null;
    try {
      String colReaderBase = JUnitExtension.getFile(
              "CpmTests" + FS + "ErrorTestCollectionReader.xml").getAbsolutePath();
      String taeBase = JUnitExtension.getFile("CpmTests" + FS + "ErrorTestAnnotator.xml")
              .getAbsolutePath();
      String casConsumerBase = JUnitExtension.getFile("CpmTests" + FS + "ErrorTestCasConsumer.xml")
              .getAbsolutePath();

      // first, prepare all descriptors as needed
      String colReaderDesc = DescriptorMakeUtil.makeCollectionReader(colReaderBase, documentCount);
      String taeDesc = null;
      if (exceptionName == null || exceptionSequence <= 0 || functionName == null) {
        taeDesc = DescriptorMakeUtil.makeAnalysisEngine(taeBase);
      } else {
        taeDesc = DescriptorMakeUtil.makeAnalysisEngine(taeBase, true, functionName,
                exceptionSequence, exceptionName);
      }
      String casConsumerDesc = DescriptorMakeUtil.makeCasConsumer(casConsumerBase);

      // secondly, create the cpm based on the descriptors
      cpeDesc = CpeDescriptorFactory.produceDescriptor();

      // managing the default behaviour of this client
      integratedProcessor = CpeDescriptorFactory.produceCasProcessor("ErrorTestAnnotator");
      integratedProcessor.setDescriptor(taeDesc);

      CpeIntegratedCasProcessor casConsumer = CpeDescriptorFactory
              .produceCasProcessor("ErrorTest CasConsumer");
      casConsumer.setDescriptor(casConsumerDesc);

      // - add all descriptors
      cpeDesc.addCollectionReader(colReaderDesc);
      cpeDesc.addCasProcessor(integratedProcessor);
      cpeDesc.addCasProcessor(casConsumer);
      cpeDesc.setInputQueueSize(2);
      cpeDesc.setOutputQueueSize(2);
      // - Create a new CPE

    } catch (Exception e) {
      e.printStackTrace();
    }
    return new Object[] { cpeDesc, integratedProcessor };
  }

  class CollectionReaderStatusCallbackListener extends TestStatusCallbackListener {
    protected CollectionProcessingEngine cpe = null;

    public CollectionReaderStatusCallbackListener(CollectionProcessingEngine cpe) {
      this.cpe = cpe;
    }

    /**
     * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#aborted()
     */
    public void aborted() {
      super.aborted();
      System.out.println("abort was called.");
      this.cpe.stop();
    }
  }
}
