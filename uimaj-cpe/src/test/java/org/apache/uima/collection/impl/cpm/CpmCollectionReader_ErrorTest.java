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

import java.util.Date;
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
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.test.junit_extension.ManageOutputDevice;
import org.apache.uima.util.Level;

/**
 * Test CollectionReader Error Handling<br>
 * 
 * <p>
 * The TestCase aims to test the important methods normally used within the
 * CollectionReader (initialize, getNext, hasNext and getProgress). In each
 * function different Exceptions are thrown to test the behavior of the system
 * in such a situation.
 * </p>
 * <p>
 * To offer a short introduction into the general mode of operation have a look
 * at the following list:
 * </p>
 * <ul>
 * <li> generate the descriptors, with fit to the test case. For instance a
 * CollectionReader which throws a (runtime) exception every 5th document. </li>
 * <li> [optional] add some mechanism to handle errors in the tests (timeouts or
 * try-catch blocks) </li>
 * <li> run the test and check for the results </li>
 * </ul>
 * 
 * Also have a look at <br>
 * 
 * @see org.apache.uima.collection.impl.cpm.CpmAE_ErrorTest
 * @see org.apache.uima.collection.impl.cpm.CpmCasConsumer_ErrorTest
 */
public class CpmCollectionReader_ErrorTest extends TestCase {

   private static final String FS = System.getProperties().getProperty(
         "file.separator");

   private void cpeProcessNoMsg(CollectionProcessingEngine cpe,
         TestStatusCallbackListener listener) throws Exception {
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
    * <b>test case:</b> the getNext method throws an OutOfMemoryError.<br>
    * <b>expected behavior:</b><br>
    * The cpm notify the entityProcessComplete method of the listener and
    * propagate the error in the EntityProcessStatus. After that, the cpm is
    * shut down and the abort method is called.
    * 
    * @throws Exception -
    */
   public void testGetNextWithOutOfMemoryError() throws Exception {
      int documentCount = 20; // number of documents processed
      int exceptionSequence = 5; // the sequence in which errors are produced
      ManageOutputDevice.setAllSystemOutputToNirvana();

      // setup CPM
      CollectionProcessingEngine cpe = setupCpm(documentCount,
            "OutOfMemoryError", exceptionSequence, "getNext");

      // Create and register a Status Callback Listener
      CollectionReaderStatusCallbackListener listener = new CollectionReaderStatusCallbackListener(
            cpe);
      cpe.addStatusCallbackListener(listener);

      cpe.process();

      // wait until cpm has finished
      while (!listener.isFinished() && !listener.isAborted()) {
         Thread.sleep(5);
      }
      ManageOutputDevice.setAllSystemOutputToDefault();
      assertEquals("Abort was not called as expected.", true, listener
            .isAborted());
      assertEquals(
            "The cpm called the listener, that the cpm has finished - which normally could not be.",
            false, listener.isFinished());
      assertEquals("There are not as much exceptions as expected! ", 1,
            FunctionErrorStore.getCount());
      checkForOutOfMemoryError(listener);
   }

   /**
    * <b>test case:</b> the getNext method throws multiple
    * CollectionExceptions.<br>
    * <b>expected behavior:</b><br>
    * The cpm should finish. The cpm by itself is shut down, and the
    * collectionProcessComplete-method of the listener was called
    * 
    * @throws Exception -
    */
   public void testGetNextWithCollectionException() throws Exception {
      int documentCount = 20; // number of documents processed
      int exceptionSequence = 2; // the sequence in which errors are produced
      ManageOutputDevice.setAllSystemOutputToNirvana();

      // setup CPM
      CollectionProcessingEngine cpe = setupCpm(documentCount,
            "CollectionException", exceptionSequence, "getNext");

      // Create and register a Status Callback Listener
      TestStatusCallbackListener listener = new CollectionReaderStatusCallbackListener(
            cpe);
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
            documentCount / exceptionSequence, FunctionErrorStore.getCount());
   }

   /**
    * <b>test case:</b> the getNext method throws multiple IOExceptions.<br>
    * <b>expected behavior:</b><br>
    * The cpm should finish. The cpm by itself is shut down, and the
    * collectionProcessComplete-method of the listener was called
    * 
    * @throws Exception -
    */
   public void testGetNextWithIOException() throws Exception {
      int TIMEOUT = 10; // seconds, till the test is aborted
      int documentCount = 20; // number of documents processed
      int exceptionSequence = 3; // the sequence in which errors are produced
      ManageOutputDevice.setAllSystemOutputToNirvana();

      // setup CPM
      CollectionProcessingEngine cpe = setupCpm(documentCount, "IOException",
            exceptionSequence, "getNext");

      // Create and register a Status Callback Listener
      TestStatusCallbackListener listener = new CollectionReaderStatusCallbackListener(
            cpe);
      cpe.addStatusCallbackListener(listener);

      cpe.process();

      // wait until cpm has finished
      Date d = new Date();
      long time = d.getTime() + 1000 * TIMEOUT;
      while (!listener.isFinished() && !listener.isAborted()) {
         Thread.sleep(5);
         d = new Date();
         // timeout mechanism
         if (time < d.getTime()) {
            System.out.println("CPM manually aborted!");
            cpe.stop();
            // wait until CPM has aborted
            while (!listener.isAborted()) {
               Thread.sleep(5);
            }
         }
      }

      ManageOutputDevice.setAllSystemOutputToDefault();
      // check the results, if everything worked as expected
      assertEquals(
            "The cpm is still working or the collectionProcessComplete-method of the listener was not called.",
            true, listener.isFinished());
      assertEquals(
            "The cpm propably didn't finish correctly! The aborted method of the listener was called.",
            false, listener.isAborted());
      assertEquals("There are not as much exceptions as expected! ",
            documentCount / exceptionSequence, FunctionErrorStore.getCount());
   }

   /**
    * <b>test case:</b> the getNext method throws multiple
    * NullPointerExceptions.<br>
    * <b>expected behavior:</b><br>
    * The cpm should finish. The cpm by itself is shut down, and the
    * collectionProcessComplete-method of the listener was called
    * 
    * @throws Exception -
    */
   public void testGetNextWithNullPointerException() throws Exception {
      int documentCount = 20; // number of documents processed
      int exceptionSequence = 2; // the sequence in which errors are produced
      ManageOutputDevice.setAllSystemOutputToNirvana();

      // setup CPM
      CollectionProcessingEngine cpe = setupCpm(documentCount,
            "NullPointerException", exceptionSequence, "getNext");

      // Create and register a Status Callback Listener
      TestStatusCallbackListener listener = new CollectionReaderStatusCallbackListener(
            cpe);
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
            documentCount / exceptionSequence, FunctionErrorStore.getCount());
   }

   /**
    * <b>test case:</b> the hasNext method throws a OutOfMemoryError.<br>
    * <b>expected behavior:</b><br>
    * The cpm notifies the entityProcessComplete method of the listener and
    * propagate the error in the EntityProcessStatus. After that, the cpm is
    * shut down and the abort method is called.
    * 
    * @throws Exception -
    */
   public void testHasNextWithOutOfMemoryError() throws Exception {
      int documentCount = 20; // number of documents processed
      int exceptionSequence = 4; // the sequence in which errors are produced
      ManageOutputDevice.setAllSystemOutputToNirvana();

      // setup CPM
      CollectionProcessingEngine cpe = setupCpm(documentCount,
            "OutOfMemoryError", exceptionSequence, "hasNext");

      // Create and register a Status Callback Listener
      CollectionReaderStatusCallbackListener listener = new CollectionReaderStatusCallbackListener(
            cpe);
      cpe.addStatusCallbackListener(listener);

      cpeProcessNoMsg(cpe, listener);

      ManageOutputDevice.setAllSystemOutputToDefault();
      // check the results, if everything worked as expected
      assertEquals(
            "The cpm called the listener, that the cpm has finished - which normally could not be.",
            false, listener.isFinished());
      assertEquals("Abort was not called.", true, listener.isAborted());
      assertEquals("There are not as much exceptions as expected! ", 1,
            FunctionErrorStore.getCount());
      checkForOutOfMemoryError(listener);
   }

   /**
    * <b>test case:</b> the hasNext method throws a NullPointerException.<br>
    * <b>expected behavior:</b><br>
    * The cpm should automatically finish. No error should be reported and the
    * finished method
    * 
    * @throws Exception -
    */
   public void testHasNextWithNullPointerException() throws Exception {
      int TIMEOUT = 20; // seconds, till the test is aborted
      int documentCount = 30; // number of documents processed
      int exceptionSequence = 4; // the sequence in which errors are produced
      boolean manuallyAborted = false; // flag, if we shut down the cpm by hand.
      ManageOutputDevice.setAllSystemOutputToNirvana();

      // setup CPM
      CollectionProcessingEngine cpe = setupCpm(documentCount,
            "NullPointerException", exceptionSequence, "hasNext");

      // Create and register a Status Callback Listener
      TestStatusCallbackListener listener = new CollectionReaderStatusCallbackListener(
            cpe);
      cpe.addStatusCallbackListener(listener);

      cpe.process();

      // wait until cpm has finished
      Date d = new Date();
      long time = d.getTime() + 1000 * TIMEOUT;
      while (!listener.isFinished() && !listener.isAborted()) {
         Thread.sleep(5);
         d = new Date();
         // timeout mechanism
         if (time < d.getTime()) {
            manuallyAborted = true;
            cpe.stop();
            // wait until CPM has aborted
            while (!listener.isAborted()) {
               Thread.sleep(5);
            }
         }
      }

      ManageOutputDevice.setAllSystemOutputToDefault();
      // check the results, if everything worked as expected
      assertEquals("The cpm didn't finish correctly! Abort was called.", false,
            listener.isAborted());
      assertEquals("The cpm didn't finish correctly! Finish was not called.",
            true, listener.isFinished());
      assertEquals("The cpm was manually aborted.", false, manuallyAborted);
   }

   /**
    * <b>test case:</b> the hasNext method throws a
    * ResourceInitializationException.<br>
    * <b>expected behavior:</b><br>
    * The cpm should not finish. An exception is thrown to the caller class.
    * 
    * @throws Exception -
    */
   public void testInitializeWithResourceInitializationException()
         throws Exception {
      int documentCount = 20; // number of documents processed
      int exceptionSequence = 1; // the sequence in which errors are produced
      TestStatusCallbackListener listener = null; // listener with which the
      // status information are
      // made available
      boolean exceptionThrown = false; // flag, if the expected exception was
      // thrown
      ManageOutputDevice.setAllSystemOutputToNirvana();

      try {
         // setup CPM
         CollectionProcessingEngine cpe = setupCpm(documentCount,
               "ResourceInitializationException", exceptionSequence,
               "initialize");

         // Create and register a Status Callback Listener
         listener = new CollectionReaderStatusCallbackListener(cpe);
         cpe.addStatusCallbackListener(listener);

         cpe.process();

         // wait until cpm has finished

         while (!listener.isFinished() && !listener.isAborted()) {
            Thread.sleep(5);
         }
      } catch (ResourceInitializationException e) {
         exceptionThrown = true;
      } finally {
         ManageOutputDevice.setAllSystemOutputToDefault();
         // check the results, if everything worked as expected
         assertEquals("The cpm didn't finish correctly! Abort was called.",
               false, listener.isAborted());
         assertEquals(
               "The cpm called the listener, that the cpm has finished - which normally could not be.",
               false, listener.isFinished());
         assertEquals("There are not as much exceptions as expected! ", 1,
               FunctionErrorStore.getCount());
         assertEquals(
               "The expected ResourceInitializationException was not fiven back to the programm. ",
               true, exceptionThrown);
         // that's it.
      }
   }

   /**
    * <b>test case:</b> the initialize method throws a NullPointerException.<br>
    * <b>expected behavior:</b><br>
    * The cpm should not finish. An exception is thrown to the caller class.
    * 
    * @throws Exception -
    */
   public void testInitializeWithNullPointerException() throws Exception {
      int documentCount = 20; // number of documents processed
      int exceptionSequence = 1; // the sequence in which errors are produced
      boolean exceptionThrown = false; // flag, if the expected exception was
      // thrown
      ManageOutputDevice.setAllSystemOutputToNirvana();
      TestStatusCallbackListener listener = null;

      try {
         // setup CPM
         CollectionProcessingEngine cpe = setupCpm(documentCount,
               "NullPointerException", exceptionSequence, "initialize");

         // Create and register a Status Callback Listener
         listener = new CollectionReaderStatusCallbackListener(cpe);
         cpe.addStatusCallbackListener(listener);

         cpe.process();

         // wait until cpm has finished
         while (!listener.isFinished() && !listener.isAborted()) {
            Thread.sleep(5);
         }
      } catch (ResourceInitializationException e) {
         // e.printStackTrace();
         exceptionThrown = true;
      } finally {
         ManageOutputDevice.setAllSystemOutputToDefault();
         // check the results, if everything worked as expected
         assertEquals("Abort was called.", false, listener.isAborted());
         assertEquals(
               "The cpm called the listener, that the cpm has finished - which normally could not be.",
               false, listener.isFinished());
         assertEquals("There are not as much exceptions as expected! ", 1,
               FunctionErrorStore.getCount());
         assertEquals(
               "The expected ResourceInitializationException was not fiven back to the programm. ",
               true, exceptionThrown);
      }
   }

   /**
    * <b>test case:</b> the initialize method throws a OutOfMemoryError.<br>
    * <b>expected behavior:</b><br>
    * The cpm should not finish correctly. An exception is thrown to the caller
    * class.
    * 
    * @throws Exception -
    */
   public void testInitializeWithOutOfMemoryError() throws Exception {
      boolean outOfMemoryError = false;
      int documentCount = 20; // number of documents processed
      int exceptionSequence = 1; // the sequence in which errors are produced
      ManageOutputDevice.setAllSystemOutputToNirvana();
      CollectionReaderStatusCallbackListener listener = null; // listener with
      // which the
      // statusinformation are made avaiable

      try {
         // setup CPM
         CollectionProcessingEngine cpe = setupCpm(documentCount,
               "OutOfMemoryError", exceptionSequence, "initialize");

         // Create and register a Status Callback Listener
         listener = new CollectionReaderStatusCallbackListener(cpe);
         cpe.addStatusCallbackListener(listener);

         cpe.process();

         // wait until cpm has finished
         while (!listener.isFinished() && !listener.isAborted()) {
            Thread.sleep(5);
         }
      } catch (OutOfMemoryError e) {
         outOfMemoryError = true;
      } finally {
         ManageOutputDevice.setAllSystemOutputToDefault();
         // check the results, if everything worked as expected
         assertEquals("The OutOfMemoryError is not given back to the caller.",
               true, outOfMemoryError);
         assertEquals("Abort was called.", false, listener.isAborted());
         assertEquals(
               "The cpm called the listener, that the cpm has finished - which normally could not be.",
               false, listener.isFinished());
         assertEquals("There are not as much exceptions as expected! ", 1,
               FunctionErrorStore.getCount());
      }

   }

   /**
    * <b>test case:</b> the getProgress method throws multiple IOExceptions.<br>
    * <b>expected behavior:</b><br>
    * The cpm should finish. The cpm by itself is shut down, and the
    * collectionProcessComplete-method of the listener was called
    * 
    * @throws Exception -
    */
   public void testGetProgressWithIOException() throws Exception {
      int documentCount = 20; // number of documents processed
      int exceptionSequence = 3; // the sequence in which errors are produced
      ManageOutputDevice.setAllSystemOutputToNirvana();

      // setup CPM
      CollectionProcessingEngine cpe = setupCpm(documentCount, "IOException",
            exceptionSequence, "getProgress");

      // Create and register a Status Callback Listener
      TestStatusCallbackListener listener = new CollectionReaderStatusCallbackListener(
            cpe);
      cpe.addStatusCallbackListener(listener);

      cpe.process();

      // wait until cpm has finished
      while (!listener.isFinished() && !listener.isAborted()) {
         Thread.sleep(5);
      }

      ManageOutputDevice.setAllSystemOutputToDefault();
      // check the results, if everything worked as expected
      assertEquals("Abort was called.", false, listener.isAborted());
      assertEquals(
            "The cpm is still working or the collectionProcessComplete-method of the listener was not called.",
            true, listener.isFinished());
      assertEquals("There are not as much exceptions as expected! ",
            (documentCount / exceptionSequence), FunctionErrorStore.getCount());
      // that's it.
   }

   /**
    * <b>test case:</b> the getProgress method throws one OutOfMemoryError.<br>
    * <b>expected behavior:</b><br>
    * The cpm notifies the entityProcessComplete method of the listener and
    * propagate the error in the EntityProcessStatus. After that, the cpm is
    * shut down and the abort method is called.
    * 
    * @throws Exception -
    */
   public void testGetProcessWithOutOfMemoryError() throws Exception {
      int documentCount = 20; // number of documents processed
      int exceptionSequence = 3; // the sequence in which errors are produced
      ManageOutputDevice.setAllSystemOutputToNirvana();

      // setup CPM
      CollectionProcessingEngine cpe = setupCpm(documentCount,
            "OutOfMemoryError", exceptionSequence, "getProgress");

      // Create and register a Status Callback Listener
      CollectionReaderStatusCallbackListener listener = new CollectionReaderStatusCallbackListener(
            cpe);
      cpe.addStatusCallbackListener(listener);

      cpe.process();

      // wait until cpm has finished
      while (!listener.isFinished() && !listener.isAborted()) {
         Thread.sleep(5);
      }

      ManageOutputDevice.setAllSystemOutputToDefault();
      // check the results, if everything worked as expected
      assertEquals("Abort was not called.", true, listener.isAborted());
      assertEquals(
            "The collectionProcessComplete-method of the listener was called. - Unexpected!",
            false, listener.isFinished());
      assertEquals("There are not as much exceptions as expected! ", 1,
            FunctionErrorStore.getCount());
      checkForOutOfMemoryError(listener);
   }

   /**
    * <b>test case:</b> the getProgress method throws multiple
    * NullPointerExceptions.<br>
    * <b>expected behavior:</b><br>
    * The cpm should finish. The cpm by itself is shut down, and the
    * collectionProcessComplete-method of the listener was called
    * 
    * @throws Exception -
    */
   public void testGetProgressWithNullPointerException() throws Exception {
      int documentCount = 20; // number of documents processed
      int exceptionSequence = 3; // the sequence in which errors are produced
      ManageOutputDevice.setAllSystemOutputToNirvana();

      // setup CPM
      CollectionProcessingEngine cpe = setupCpm(documentCount,
            "NullPointerException", exceptionSequence, "getProgress");

      // Create and register a Status Callback Listener
      TestStatusCallbackListener listener = new CollectionReaderStatusCallbackListener(
            cpe);
      cpe.addStatusCallbackListener(listener);

      cpe.process();

      // wait until cpm has finished
      while (!listener.isFinished() && !listener.isAborted()) {
         Thread.sleep(5);
      }

      ManageOutputDevice.setAllSystemOutputToDefault();
      // check the results, if everything worked as expected
      assertEquals("Abort was called.", false, listener.isAborted());
      assertEquals(
            "The cpm is still working or the collectionProcessComplete-method of the listener was not called.",
            true, listener.isFinished());
      assertEquals("There are not as much exceptions as expected! ",
            (documentCount / exceptionSequence), FunctionErrorStore.getCount());

   }

   // INFO: the close -methode could not be invoked by an external action
   // public void testCloseWithNullPointerException() throws Exception{
   // int TIMEOUT = 15; // seconds, till the test is aborted
   // int documentCount = 20; // number of documents processed
   // int exceptionSequence = 1; // the sequence in which errors are produced
   //
   // //setup CPM
   // CollectionProcessingEngine cpe =
   // setupCpm(
   // documentCount,
   // "NullPointerException",
   // exceptionSequence,
   // "close");
   //
   // //Create and register a Status Callback Listener
   // TestStatusCallbackListener listener =
   // new CollectionReaderStatusCallbackListener(cpe);
   // cpe.addStatusCallbackListener(listener);
   //
   // cpe.process();
   //
   // //wait until cpm has finished
   // while (!listener.isFinished() && !listener.isAborted()) {
   // Thread.sleep(5);
   //			
   // }
   // // check the results, if everything worked as expected
   // assertEquals("The cpm didn't finish correctly! Abort was called.", false,
   // listener.isAborted());
   // assertEquals("The cpm didn't should down correctly", true,
   // listener.isFinished());
   // assertEquals("The cpm crashed.", false, manuallyAborted);
   // assertEquals("No Exception should be thrown!", 0,
   // FunctionErrorStore.getCount());
   // // that's it.
   // }

   /**
    * @see junit.framework.TestCase#tearDown()
    */
   protected void tearDown() throws Exception {
      super.tearDown();
      FunctionErrorStore.resetCount();
//      System.gc();
//      System.gc();
   }

   /**
    * @param listener
    */
   private void checkForOutOfMemoryError(
         CollectionReaderStatusCallbackListener listener) {
      assertEquals("The indication failed, that an error was thrown.", true,
            listener.isAborted());
      assertEquals(
            "No Error was thrown (and no Exception) - expected was an OutOfMemoryError.",
            true, listener.hasError());

      // the last CAS is maybe not the error CAS, since it is possible that the
      // status listener gets another CAS after the error occurred.
      // This checking is done in the Status listener
      // assertEquals("The expected null for the failed cas is missing.", true,
      // (listener.getLastCas() == null));
      assertEquals("There are not as much exceptions as expected! ", 1,
            FunctionErrorStore.getCount());
      assertEquals(
            "The cpm called the listener, that the cpm has finished - which normally could not be.",
            false, listener.isFinished());

   }

   /**
    * setup the CPM with base functionality.
    * 
    * @param documentCount
    *           how many documents should be processed
    * @param exceptionName
    *           the exception to be thrown
    * @param exceptionSequence
    *           the iteration rate of the exceptions
    * @param functionName
    *           the name of the function/method that throws the exception
    * 
    * @return CollectionProcessingEngine - initialized cpe
    */
   private CollectionProcessingEngine setupCpm(int documentCount,
         String exceptionName, int exceptionSequence, String functionName) {
      CpeDescription cpeDesc = null;
      CollectionProcessingEngine cpe = null;

      try {
         String colReaderBase = JUnitExtension.getFile(
               "CpmTests" + FS + "ErrorTestCollectionReader.xml")
               .getAbsolutePath();
         String taeBase = JUnitExtension.getFile(
               "CpmTests" + FS + "ErrorTestAnnotator.xml").getAbsolutePath();
         String casConsumerBase = JUnitExtension.getFile(
               "CpmTests" + FS + "ErrorTestCasConsumer.xml").getAbsolutePath();

         // first, prepare all descriptors as needed
         String colReaderDesc = DescriptorMakeUtil.makeCollectionReader(
               colReaderBase, true, functionName, exceptionSequence,
               exceptionName, documentCount);
         String taeDesc = DescriptorMakeUtil.makeAnalysisEngine(taeBase);
         String casConsumerDesc = DescriptorMakeUtil
               .makeCasConsumer(casConsumerBase);

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
         cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc, null,
               null);
      } catch (Exception e) {
         e.printStackTrace();
      }
      return cpe;
   }

   class CollectionReaderStatusCallbackListener extends
         TestStatusCallbackListener {
      protected CollectionProcessingEngine cpe = null;

      private boolean errorThrown = false; // indicates, if the

      // OutOfMemoryError is thrown

      public CollectionReaderStatusCallbackListener(
            CollectionProcessingEngine cpe) {
         this.cpe = cpe;
      }

      /**
       * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#aborted()
       */
      public void aborted() {
         super.aborted();
         // System.out.println("abort was called.");
         this.cpe.stop();
      }

      /**
       * This method is modified, to react on OutOfMemoryErrors in the correct
       * way.
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
               // if there is an error ... call the cpm to kill and check for a
               // null CAS
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
