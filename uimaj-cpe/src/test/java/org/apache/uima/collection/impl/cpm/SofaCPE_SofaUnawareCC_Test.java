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

import static org.junit.Assert.fail;

import java.io.File;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SofaCPE_SofaUnawareCC_Test {

  private File cpeSpecifierFile = null;

  CpeDescription cpeDesc = null;

  CollectionProcessingEngine cpe = null;

  public long elapsedTime = 0;

  public long startTime = 0;

  StatusCallbackListenerImpl1 statCbL1;

  boolean debug = false;

  Throwable firstFailure;

    @BeforeEach
    public void setUp() throws Exception {
    UIMAFramework.getXMLParser().enableSchemaValidation(true);
    cpeSpecifierFile = JUnitExtension.getFile("CpeSofaTest/SofaCPE_SofaUnawareCC.xml");
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

    @AfterEach
    public void tearDown() throws Exception {
    cpeDesc = null;
    cpe = null;
    cpeSpecifierFile = null;
//    System.gc();
//    System.gc();
  }

    @Test
    public void testProcess() throws Throwable {
    // System.out.println("method testProcess");
    try {
      cpe.process();
      // wait till cpe finishes
      synchronized (statCbL1) {
        while (!statCbL1.finished) {
          try {
            statCbL1.wait();
          } catch (InterruptedException e) {
          }
        }
      }
      // printStats(cpe.getPerformanceReport());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
    if (firstFailure != null)
      throw firstFailure;
  }

  class StatusCallbackListenerImpl1 implements StatusCallbackListener {

    int entityCount = 0;

    long size = 0;

    int statUnit = 100;

    volatile boolean finished = false;

    @Override
    public void initializationComplete() {
      if (debug)
        System.out.println(" Collection Processsing managers initialization " + "is complete ");
    }

    @Override
    public synchronized void batchProcessComplete() {
    }

    @Override
    public synchronized void collectionProcessComplete() {
      if (debug)
        System.out.println(" Completed " + entityCount + " documents  ; " + size / 1000 + " kB");
      elapsedTime = System.currentTimeMillis() - startTime;
      if (debug)
        System.out.println(" Time Elapsed : " + elapsedTime + " ms ");
      finished = true;
      notifyAll();
    }

    @Override
    public synchronized void paused() {
      if (debug)
        System.out.println("Paused");
    }

    @Override
    public synchronized void resumed() {
      if (debug)
        System.out.println("Resumed");
    }

    @Override
    public void aborted() {
      if (debug)
        System.out.println("Stopped");
      fail();
    }

    @Override
    public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
      // if there is an error, record and we will fail on CPE completion
      if (aStatus.getExceptions().size() > 0) {
        if (firstFailure == null)
          firstFailure = (Throwable) aStatus.getExceptions().get(0);
      }
    }

  } // StatusCallbackListenerImpl1
}
