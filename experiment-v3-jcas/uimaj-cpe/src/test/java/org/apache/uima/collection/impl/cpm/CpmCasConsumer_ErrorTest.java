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

import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.EntityProcessStatus;
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
 * Test CasConsumer Error Handling<br>
 * 
 * <p>
 * The TestCase aims to test the important methods normally used within the CasConsumer (initialize
 * and processCas). In each function different Exceptions are thrown to test the behaviour of the
 * system in such a situation. Therefore special helper classes located in the
 * {@link org.apache.uima.collection.impl.cpm.utils} package are used. For instance
 * {@link DecriptorMakeUtil} generates the customized descriptors which throws the predefined
 * Exceptions. {@link FunctionErrorStore} is the class where all data about methodcalls and counts
 * are kept. That's just to point out some important classes.
 * </p>
 * <p>
 * To offer a short introduction into the generell mode of operation have a look at the following
 * list:
 * </p>
 * <ul>
 * <li> generate the descriptors, with fit to the testcase. For instance an annotator which throws a
 * (runtime) exception every 5th document. </li>
 * <li> [optional] add some mechanism to handle errors in the tests (timeouts or try-catch blocks)
 * </li>
 * <li> run the test and check for the results </li>
 * </ul>
 * 
 * Also have a look at <br>
 * 
 * @see org.apache.uima.collection.impl.cpm.CpmAE_ErrorTest
 * @see org.apache.uima.collection.impl.cpm.CpmCollectionReader_ErrorTest
 */
public class CpmCasConsumer_ErrorTest extends TestCase {

  private static final String FS = System.getProperties().getProperty("file.separator");

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
   * <b>testcase:</b> the initialize method throws a ResourceInitializationException.<br>
   * <b>expected behaviour:</b><br>
   * The cpm should not finish. Instead, the exception is passed back to the testscript. Neither the
   * collectionProcessComplete-, nor the aborted- method of the listener is called.
   * 
   * @throws Exception -
   */
  public void testInitializeWithResourceInitializationException() throws Exception {
    int documentCount = 20; // number of documents processed
    int exceptionSequence = 1; // the sequence in which errors are produced
    boolean exceptionThrown = false;
    TestStatusCallbackListener listener = null;
    ManageOutputDevice.setAllSystemOutputToNirvana();
    try {
      // setup CPM
      CollectionProcessingEngine cpe = setupCpm(documentCount, "ResourceInitializationException",
              exceptionSequence, "initialize");

      // Create and register a Status Callback Listener
      listener = new CollectionReaderStatusCallbackListener(cpe);
      cpe.addStatusCallbackListener(listener);
      cpeProcessNoMsg(cpe, listener);
    } catch (NullPointerException e) {
      exceptionThrown = true;
    } finally {
      // check the results, if everything worked as expected
      ManageOutputDevice.setAllSystemOutputToDefault();
      assertEquals("The expected NullPointerException wasn't thrown!", true, exceptionThrown);
      assertEquals(
              "The cpm called the listener, that the cpm has finished - which normally could not be.",
              false, listener.isFinished());
      assertEquals("The aborted-method of the listener was called. (new behaviour?)", false,
              listener.isAborted());
      assertEquals("There are not as much exceptions as expected! ", 1, FunctionErrorStore
              .getCount());
    }
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
    int documentCount = 20; // number of documents processed
    int exceptionSequence = 1; // the sequence in which errors are produced
    boolean exceptionThrown = false;
    // setup CPM
    TestStatusCallbackListener listener = null;
    ManageOutputDevice.setAllSystemOutputToNirvana();
    try {
      CollectionProcessingEngine cpe = setupCpm(documentCount, "NullPointerException",
              exceptionSequence, "initialize");

      // Create and register a Status Callback Listener
      listener = new CollectionReaderStatusCallbackListener(cpe);
      cpe.addStatusCallbackListener(listener);
      cpeProcessNoMsg(cpe, listener);
    } catch (NullPointerException e) {
      // e.printStackTrace();
      exceptionThrown = true;
    } finally {
      // check the results, if everything worked as expected
      ManageOutputDevice.setAllSystemOutputToDefault();
      assertEquals("The expected NullPointerException wasn't thrown!", true, exceptionThrown);
      assertEquals(
              "The cpm called the listener, that the cpm has finished - which normally could not be.",
              false, listener.isFinished());
      assertEquals("The aborted-method of the listener was called. (new behaviour?)", false,
              listener.isAborted());
      assertEquals("There are not as much exceptions as expected! ", 1, FunctionErrorStore
              .getCount());
    }
  }

  /**
   * <b>testcase:</b> the initialize method throws an OutOfMemoryException.<br>
   * <b>expected behaviour:</b><br>
   * The cpm should not finish. Instead, the exception is passed back to the testscript. Neither the
   * collectionProcessComplete-, nor the aborted- method of the listener is called.
   * 
   * @throws Exception -
   */
  public void testInitializeWithOutOfMemoryError() throws Exception {
    int documentCount = 20; // number of documents processed
    int exceptionSequence = 1; // the sequence in which errors are produced
    boolean errorThrown = false;
    // setup CPM
    TestStatusCallbackListener listener = new TestStatusCallbackListener();
    ManageOutputDevice.setAllSystemOutputToNirvana();
    try {
      CollectionProcessingEngine cpe = setupCpm(documentCount, "OutOfMemoryError",
              exceptionSequence, "initialize");

      // Create and register a Status Callback Listener
      listener = new CollectionReaderStatusCallbackListener(cpe);
      cpe.addStatusCallbackListener(listener);
      cpeProcessNoMsg(cpe, listener);
    } catch (OutOfMemoryError er) {
      errorThrown = true;
    } finally {
      // check the results, if everything worked as expected
      ManageOutputDevice.setAllSystemOutputToDefault();
      assertEquals(
              "The cpm called the listener, that the cpm has finished - which normally could not be.",
              false, listener.isFinished());
      assertEquals("The aborted-method of the listener was called. (new behaviour?)", false,
              listener.isAborted());
      assertEquals("There are not as much exceptions as expected! ", 1, FunctionErrorStore
              .getCount());
      assertEquals("The expected Error wasn't thrown! ", true, errorThrown);
    }
  }

  /**
   * <b>testcase:</b> the processCas method throws multiple IOExceptions.<br>
   * <b>expected behaviour:</b><br>
   * The cpm should finish correctly.
   * 
   * @throws Exception -
   */
  public void testProcessCasWithIOException() throws Exception {
    int documentCount = 20; // number of documents processed
    int exceptionSequence = 3; // the sequence in which errors are produced
    ManageOutputDevice.setAllSystemOutputToNirvana();
    // setup CPM
    CollectionProcessingEngine cpe = setupCpm(documentCount, "IOException", exceptionSequence,
            "processCas");

    // Create and register a Status Callback Listener
    TestStatusCallbackListener listener = new CollectionReaderStatusCallbackListener(cpe);
    cpe.addStatusCallbackListener(listener);
    cpeProcessNoMsg(cpe, listener);
    // check the results, if everything worked as expected
    ManageOutputDevice.setAllSystemOutputToDefault();
    assertEquals(
            "The cpm is still working or the collectionProcessComplete-method of the listener was not called.",
            true, listener.isFinished());
    assertEquals("The aborted-method of the listener was called. (new behaviour?)", false, listener
            .isAborted());
    assertEquals("There are not as much exceptions  thrown as expected! ",
            ((documentCount) / exceptionSequence), FunctionErrorStore.getCount());
    assertEquals(
            "The CAS which causes the error wasn't given to the process methode. Null was returned.",
            false, null == listener.getLastCas());
  }

  /**
   * <b>testcase:</b> the processCas method throws one or multiple ResourceProcessExceptions.<br>
   * <b>expected behaviour:</b><br>
   * The cpm should finish correctly. The aborted- method of the listener is not called.
   * 
   * @throws Exception -
   */
  public void testProcessCasWithResourceProcessException() throws Exception {
    int documentCount = 20; // number of documents processed
    int exceptionSequence = 3; // the sequence in which errors are produced
    ManageOutputDevice.setAllSystemOutputToNirvana();
    // setup CPM
    CollectionProcessingEngine cpe = setupCpm(documentCount, "ResourceProcessException",
            exceptionSequence, "processCas");

    // Create and register a Status Callback Listener
    TestStatusCallbackListener listener = new CollectionReaderStatusCallbackListener(cpe);
    cpe.addStatusCallbackListener(listener);
    cpeProcessNoMsg(cpe, listener);
    // check the results, if everything worked as expected
    ManageOutputDevice.setAllSystemOutputToDefault();
    assertEquals("The cpm did not call the listener, that the cpm has finished.", true, listener
            .isFinished());
    assertEquals("The aborted-method of the listener was called!", false, listener.isAborted());
    assertEquals("There are not as much exceptions as expected! ", countExceptions(documentCount,
            exceptionSequence), FunctionErrorStore.getCount());
  }

  /**
   * <b>testcase:</b> the processCas method throws one or multiple OutOfMemoryErrors.<br>
   * <b>expected behaviour:</b><br>
   * The cpm should not finish correctly. In the listener, the entityProcessComplete methode is
   * called and the error is passed to the EntityProcessStatus value. In this methode, the
   * cpe.kill() methode is invoked, which organises the shut down of the cpm. In the end, the
   * abort-methode is called, to comunicate the status of the cpm to everyone who is listening for
   * errors.
   * 
   * @throws Exception -
   */
  public void testProcessCasWithOutOfMemoryError() throws Exception {
    int documentCount = 20; // number of documents processed
    int exceptionSequence = 3; // the sequence in which errors are produced
    ManageOutputDevice.setAllSystemOutputToNirvana();
    // setup CPM
    CollectionReaderStatusCallbackListener listener = null;
    CollectionProcessingEngine cpe = setupCpm(documentCount, "OutOfMemoryError", exceptionSequence,
            "processCas");

    // Create and register a Status Callback Listener
    listener = new CollectionReaderStatusCallbackListener(cpe);
    cpe.addStatusCallbackListener(listener);
    cpeProcessNoMsg(cpe, listener);

    // check the results, if everything worked as expected
    ManageOutputDevice.setAllSystemOutputToDefault();
    // System.out.println(FunctionErrorStore.printStats());
    assertEquals("Abort was not called!", true, listener.isAborted());
    assertEquals("There are not as much exceptions as expected! ", 1, FunctionErrorStore.getCount());
    assertEquals("There is no Error thrown! ", true, listener.hasError());
  }

  /**
   * <b>testcase:</b> the processCas method throws one NullPointerException.<br>
   * <b>expected behaviour:</b><br>
   * The cpm should finish correctly. The aborted- method of the listener is not called.
   * 
   * @throws Exception -
   */
  public void testProcessCasWithNullPointerException() throws Exception {
    int documentCount = 20; // number of documents processed
    int exceptionSequence = 3; // the sequence in which errors are produced
    ManageOutputDevice.setAllSystemOutputToNirvana();
    // setup CPM
    CollectionProcessingEngine cpe = setupCpm(documentCount, "NullPointerException",
            exceptionSequence, "processCas");

    // Create and register a Status Callback Listener
    TestStatusCallbackListener listener = new CollectionReaderStatusCallbackListener(cpe);
    cpe.addStatusCallbackListener(listener);
    cpeProcessNoMsg(cpe, listener);
    // check the results, if everything worked as expected
    ManageOutputDevice.setAllSystemOutputToDefault();
    assertEquals("The cpm did not call the listener, that the cpm has finished.", true, listener
            .isFinished());
    assertEquals("The aborted-method of the listener was called!", false, listener.isAborted());
    assertEquals("There are not as much exceptions as expected! ", countExceptions(documentCount,
            exceptionSequence), FunctionErrorStore.getCount());
  }

  /**
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
    FunctionErrorStore.resetCount();
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
      String colReaderBase = JUnitExtension.getFile("CpmTests" + FS + "ErrorTestCollectionReader.xml").getAbsolutePath();
      String taeBase = JUnitExtension.getFile("CpmTests" + FS + "ErrorTestAnnotator.xml").getAbsolutePath();
      String casConsumerBase = JUnitExtension.getFile("CpmTests" + FS + "ErrorTestCasConsumer.xml").getAbsolutePath();

      // first, prepare all descriptors as needed
      String colReaderDesc = DescriptorMakeUtil.makeCollectionReader(colReaderBase, documentCount);
      String taeDesc = DescriptorMakeUtil.makeAnalysisEngine(taeBase);
      String casConsumerDesc = DescriptorMakeUtil.makeCasConsumer(casConsumerBase, true,
              functionName, exceptionSequence, exceptionName);

      // secondly, create the cpm based on the descriptors
      cpeDesc = CpeDescriptorFactory.produceDescriptor();

      // managing the default behaviour of this client
      CpeIntegratedCasProcessor integratedProcessor = CpeDescriptorFactory
              .produceCasProcessor("ErrorTestAnnotator");
      integratedProcessor.setDescriptor(taeDesc);
      integratedProcessor.setActionOnMaxError("terminate");

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
      e.printStackTrace();
    }
    return cpe;
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
   * Special Listener for reacting on the different Exceptions and Errors to ensure a secure shut
   * down during the whole test.
   */
  class CollectionReaderStatusCallbackListener extends TestStatusCallbackListener {
    protected CollectionProcessingEngine cpe = null;

    private boolean errorThrown = false;

    public CollectionReaderStatusCallbackListener(CollectionProcessingEngine cpe) {
      this.cpe = cpe;
    }

    /**
     * This methode is modified, to react on OutOfMemoryErrors in the correct way.
     * 
     * @see org.apache.uima.collection.StatusCallbackListener#entityProcessComplete(org.apache.uima.cas.CAS,
     *      org.apache.uima.collection.EntityProcessStatus)
     */
    public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
      super.entityProcessComplete(aCas, aStatus);
      // check for a failure in processing...
      if (aStatus.getStatusMessage().equals("failed")) {
        Iterator iter = aStatus.getExceptions().iterator();
        while (iter.hasNext()) {
          // if there is an error ... call the cpm to kill and check for a null CAS
          if (iter.next() instanceof java.lang.Error) {
            this.cpe.kill();
            this.errorThrown = true;
            assertEquals("The cas is not null, as expected.", null, aCas);
          }
        }
      }
    }

    public boolean hasError() {
      return this.errorThrown;
    }
  }
}
