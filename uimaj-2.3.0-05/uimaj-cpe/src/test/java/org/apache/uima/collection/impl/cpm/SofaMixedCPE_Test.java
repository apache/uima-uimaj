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

import java.io.File;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;

public class SofaMixedCPE_Test extends TestCase {

  private File cpeSpecifierFile = null;

  CpeDescription cpeDesc = null;

  CollectionProcessingEngine cpe = null;

  public long elapsedTime = 0;

  public long startTime = 0;

  StatusCallbackListenerImpl1 statCbL1;

  boolean debug = false;

  Throwable firstFailure;

  public SofaMixedCPE_Test(String arg0) {
    super(arg0);
  }

  protected void setUp() throws Exception {
    UIMAFramework.getXMLParser().enableSchemaValidation(true);
    cpeSpecifierFile = JUnitExtension.getFile("CpeSofaTest/SofaMixedCPE.xml");
    // Use the specifier file to determine where the specifiers live.
    System.setProperty("CPM_HOME", cpeSpecifierFile.getParentFile().getParentFile().getAbsolutePath());
    cpeDesc = UIMAFramework.getXMLParser()
            .parseCpeDescription(new XMLInputSource(cpeSpecifierFile));
    // instantiate a cpe
    cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc, null);
    // add status callback
    statCbL1 = new StatusCallbackListenerImpl1();
    cpe.addStatusCallbackListener(statCbL1);
    firstFailure = null;
  }

  protected void tearDown() throws Exception {
    cpeDesc = null;
    cpe = null;
    cpeSpecifierFile = null;
    System.gc();
    System.gc();
  }

  public void testProcess() throws Throwable {
    try {
      cpe.process();
      while ( cpe.isProcessing() ) {
        // wait till cpe finishes
        synchronized (statCbL1) {
          statCbL1.wait(100);
        }
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
    if (firstFailure != null)
      throw firstFailure;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(SofaCPE_Test.class);
  }

  /**
   * Callback Listener.
   */
  class StatusCallbackListenerImpl1 implements StatusCallbackListener {

    int entityCount = 0;

    long size = 0;

    int statUnit = 100;

    /**
     * Called when the initialization is completed.
     * 
     * @see org.apache.uima.collection.processing.StatusCallbackListener#initializationComplete()
     */
    public void initializationComplete() {
      if (debug)
        System.out.println(" Collection Processsing managers initialization " + "is complete ");
    }

    /**
     * Called when the batchProcessing is completed.
     * 
     * @see org.apache.uima.collection.processing.StatusCallbackListener#batchProcessComplete()
     * 
     */
    public synchronized void batchProcessComplete() {
    }

    /**
     * Called when the collection processing is completed.
     * 
     * @see org.apache.uima.collection.processing.StatusCallbackListener#collectionProcessComplete()
     */
    public synchronized void collectionProcessComplete() {
      if (debug)
        System.out.println(" Completed " + entityCount + " documents  ; " + size / 1000 + " kB");
      elapsedTime = System.currentTimeMillis() - startTime;
      if (debug)
        System.out.println(" Time Elapsed : " + elapsedTime + " ms ");
      notifyAll();
    }

    /**
     * Called when the CPM is paused.
     * 
     * @see org.apache.uima.collection.processing.StatusCallbackListener#paused()
     */
    public synchronized void paused() {
      if (debug)
        System.out.println("Paused");
    }

    /**
     * Called when the CPM is resumed after a pause.
     * 
     * @see org.apache.uima.collection.processing.StatusCallbackListener#resumed()
     */
    public synchronized void resumed() {
      if (debug)
        System.out.println("Resumed");
    }

    /**
     * Called when the CPM is stopped abruptly due to errors.
     * 
     * @see org.apache.uima.collection.processing.StatusCallbackListener#aborted()
     */
    public void aborted() {
      if (debug)
        System.out.println("Stopped");
    }

    /**
     * Called when the processing of a Document is completed. <br>
     * The process status can be looked at and corresponding actions taken.
     * 
     * @param aCas
     *          CAS corresponding to the completed processing
     * @param aStatus
     *          EntityProcessStatus that holds the status of all the events for aEntity
     */

    public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
      // if there is an error, record and we will fail on CPE completion
      if (aStatus.getExceptions().size() > 0) {
        if (firstFailure == null)
          firstFailure = (Throwable) aStatus.getExceptions().get(0);
      }
    }
  } // StatusCallbackListenerImpl1
}