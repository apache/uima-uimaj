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
import static org.junit.Assert.assertEquals;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.impl.AnalysisEngineDescription_impl;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the ResourcePool_impl class.
 * 
 */
public class ResourcePoolTest {
  @BeforeEach
  public void setUp() throws Exception {
    try {
      // create resource specifier and a pool containing 3 instances
      mDesc = new AnalysisEngineDescription_impl();
      mDesc.setPrimitive(true);
      mDesc.setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      mDesc.getMetaData().setName("Test TAE");
      mDesc.getMetaData().setDescription("This is a test.");
      pool1 = new ResourcePool(3, mDesc, AnalysisEngine.class);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @AfterEach
  public void tearDown() {
    mDesc = null;
    pool1.destroy();
    pool1 = null;
  }

  /*
   * Test for Resource_impl getResource()
   */
  @Test
  public void testGetResource() throws Exception {
    try {
      assertThat(pool1.getFreeInstances()).hasSize(3);

      // get two resources
      Resource foo = pool1.getResource();
      assertThat(foo).isNotNull();
      assertThat(pool1.getFreeInstances()).hasSize(2);

      Resource bar = pool1.getResource();
      assertThat(bar).isNotNull();
      assertThat(!foo.equals(bar)).isTrue();
      assertThat(pool1.getFreeInstances()).hasSize(1);

      // get two more resources (should exhaust pool)
      Resource a = pool1.getResource();
      assertThat(a).isNotNull();
      assertThat(pool1.getFreeInstances()).hasSize(0);

      Resource b = pool1.getResource();
      assertThat(b).isNull();
      assertThat(pool1.getFreeInstances()).hasSize(0);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  /*
   * Test for Resource_impl getResource(long)
   */
  @Test
  public void testGetResourceJ() throws Exception {
    try {
      // ask for resources with timeout of 1 second. should respond quickly
      // until resources are exhausted, then it will pause 1 second before
      // returning null
      long startTime = System.currentTimeMillis();
      Resource foo = pool1.getResource(1000);
      assertThat(foo).isNotNull();
      assertThat(System.currentTimeMillis() - startTime < 500).isTrue();

      startTime = System.currentTimeMillis();
      Resource bar = pool1.getResource(1000);
      assertThat(bar).isNotNull();
      assertThat(!foo.equals(bar)).isTrue();
      assertThat(System.currentTimeMillis() - startTime < 500).isTrue();

      startTime = System.currentTimeMillis();
      Resource a = pool1.getResource(1000);
      assertThat(a).isNotNull();
      assertThat(System.currentTimeMillis() - startTime < 500).isTrue();

      startTime = System.currentTimeMillis();
      Resource b = pool1.getResource(1000);
      assertThat(b).isNull();
      assertThat(System.currentTimeMillis() - startTime >= 1000).isTrue();

      // Start a thread that will release "foo" in .2 second. Demonstrate that
      // getResource() will not acquire a resource but getResource(1000) will.
      Thread releaserThread = new ReleaserThread(foo);
      exceptionFromReleaserThread[0] = null;
      releaserThread.start();

      b = pool1.getResource();
      assertThat(b).isNull();
      b = pool1.getResource(5000); // wait up to 5 seconds in case machine is sluggish :-(
      // observe occasional failures - it appears the test machine gets pre-empted for several
      // seconds
      // and the .2 second delay to release the resource didn't actually get that thread started
      // before
      // the checking thread timed out.
      // Changing the time the main thread waits for the release from 1 sec to 5 secs to reduce
      // spurious failures.
      assertThat(b).isNotNull();
      releaserThread.join();
      assertEquals(null, exceptionFromReleaserThread[0]);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testReleaseResource() throws Exception {
    try {
      // acquire all the resources
      assertThat(pool1.getFreeInstances()).hasSize(3);
      Resource foo = pool1.getResource();
      Resource bar = pool1.getResource();
      Resource blah = pool1.getResource();
      assertThat(pool1.getFreeInstances()).hasSize(0);

      // release one
      pool1.releaseResource(foo);
      assertThat(pool1.getFreeInstances()).hasSize(1);

      // try to release "foo" again - should not change the free instances count
      // this will log a warning - first we log that this is expected
      UIMAFramework.getLogger().log(Level.WARNING,
              "Unit test is expecting to log ResourcePool warning.");
      pool1.releaseResource(foo);
      assertThat(pool1.getFreeInstances()).hasSize(1);

      // show that we can then check out a new one
      Resource test = pool1.getResource();
      assertThat(test).isNotNull();
      assertThat(pool1.getFreeInstances()).hasSize(0);

      // release the others
      pool1.releaseResource(test);
      pool1.releaseResource(bar);
      pool1.releaseResource(blah);
      assertThat(pool1.getFreeInstances()).hasSize(3);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testDestroy() throws Exception {
    try {
      // do some stuff
      Resource foo = pool1.getResource();
      Resource bar = pool1.getResource();
      Resource a = pool1.getResource();
      pool1.releaseResource(foo);
      Resource b = pool1.getResource();
      pool1.releaseResource(b);

      // now some stuff should be recorded in the pool
      assertThat(!pool1.getFreeInstances().isEmpty()).isTrue();

      // destroy the pool
      pool1.destroy();

      // check that everything is gone
      assertThat(pool1.getFreeInstances().isEmpty()).isTrue();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testGetMetaData() throws Exception {
    try {
      ResourceMetaData descMetaData = mDesc.getMetaData();
      ResourceMetaData poolMetaData = pool1.getMetaData();
      // only UUID should be different
      descMetaData.setUUID(poolMetaData.getUUID());
      assertThat(poolMetaData).isEqualTo(descMetaData);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  private AnalysisEngineDescription mDesc;

  private ResourcePool pool1;

  private final Exception[] exceptionFromReleaserThread = new Exception[1];

  class ReleaserThread extends Thread {
    private Resource r;

    ReleaserThread(Resource r) {
      this.r = r;
    }

    @Override
    public void run() {
      try {
        synchronized (this) {
          wait(200);
        }
        pool1.releaseResource(r);
      } catch (Exception e) {
        exceptionFromReleaserThread[0] = e;
        throw new RuntimeException();
      }
    }
  }

}
