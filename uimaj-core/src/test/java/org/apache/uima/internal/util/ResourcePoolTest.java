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

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.impl.AnalysisEngineDescription_impl;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.Level;

/**
 * Tests the ResourcePool_impl class.
 * 
 */
public class ResourcePoolTest extends TestCase {
  /**
   * Constructor for ResourcePool_implTest.
   * 
   * @param arg0
   */
  public ResourcePoolTest(String arg0) {
    super(arg0);
  }

  /**
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    try {
      super.setUp();
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
  
  public void tearDown() {
    mDesc = null;
    pool1.destroy();
    pool1 = null;
  }

  /*
   * Test for Resource_impl getResource()
   */
  public void testGetResource() throws Exception {
    try {
      Assert.assertEquals(3, pool1.getFreeInstances().size());

      // get two resources
      Resource foo = pool1.getResource();
      Assert.assertNotNull(foo);
      Assert.assertEquals(2, pool1.getFreeInstances().size());

      Resource bar = pool1.getResource();
      Assert.assertNotNull(bar);
      Assert.assertTrue(!foo.equals(bar));
      Assert.assertEquals(1, pool1.getFreeInstances().size());

      // get two more resources (should exhaust pool)
      Resource a = pool1.getResource();
      Assert.assertNotNull(a);
      Assert.assertEquals(0, pool1.getFreeInstances().size());

      Resource b = pool1.getResource();
      Assert.assertNull(b);
      Assert.assertEquals(0, pool1.getFreeInstances().size());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  /*
   * Test for Resource_impl getResource(long)
   */
  public void testGetResourceJ() throws Exception {
    try {
      // ask for resources with timeout of 1 second. should respond quickly
      // until resources are exhausted, then it will pause 1 second before
      // returning null
      long startTime = System.currentTimeMillis();
      Resource foo = pool1.getResource(1000);
      Assert.assertNotNull(foo);
      Assert.assertTrue(System.currentTimeMillis() - startTime < 500);

      startTime = System.currentTimeMillis();
      Resource bar = pool1.getResource(1000);
      Assert.assertNotNull(bar);
      Assert.assertTrue(!foo.equals(bar));
      Assert.assertTrue(System.currentTimeMillis() - startTime < 500);

      startTime = System.currentTimeMillis();
      Resource a = pool1.getResource(1000);
      Assert.assertNotNull(a);
      Assert.assertTrue(System.currentTimeMillis() - startTime < 500);

      startTime = System.currentTimeMillis();
      Resource b = pool1.getResource(1000);
      Assert.assertNull(b);
      Assert.assertTrue(System.currentTimeMillis() - startTime >= 1000);

      // Start a thread that will release "foo" in .2 second. Demonstrate that
      // getResource() will not acquire a resource but getResource(1000) will.
      Thread releaserThread = new ReleaserThread(foo);
      exceptionFromReleaserThread[0] = null;
      releaserThread.start();

      b = pool1.getResource();
      Assert.assertNull(b);
      b = pool1.getResource(5000);  // wait up to 5 seconds in case machine is sluggish :-(
      // observe occasional failures - it appears the test machine gets pre-empted for several seconds
      // and the .2 second delay to release the resource didn't actually get that thread started before
      // the checking thread timed out. 
      // Changing the time the main thread waits for the release from 1 sec to 5 secs to reduce spurious failures.
      Assert.assertNotNull(b);
      releaserThread.join();
      assertEquals(null, exceptionFromReleaserThread[0]);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testReleaseResource() throws Exception {
    try {
      // acquire all the resources
      Assert.assertEquals(3, pool1.getFreeInstances().size());
      Resource foo = pool1.getResource();
      Resource bar = pool1.getResource();
      Resource blah = pool1.getResource();
      Assert.assertEquals(0, pool1.getFreeInstances().size());

      // release one
      pool1.releaseResource(foo);
      Assert.assertEquals(1, pool1.getFreeInstances().size());

      // try to release "foo" again - should not change the free instances count
      // this will log a warning - first we log that this is expected
      UIMAFramework.getLogger().log(Level.WARNING, "Unit test is expecting to log ResourcePool warning.");
      pool1.releaseResource(foo);
      Assert.assertEquals(1, pool1.getFreeInstances().size());

      // show that we can then check out a new one
      Resource test = pool1.getResource();
      Assert.assertNotNull(test);
      Assert.assertEquals(0, pool1.getFreeInstances().size());

      // release the others
      pool1.releaseResource(test);
      pool1.releaseResource(bar);
      pool1.releaseResource(blah);
      Assert.assertEquals(3, pool1.getFreeInstances().size());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

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
      Assert.assertTrue(!pool1.getFreeInstances().isEmpty());

      // destroy the pool
      pool1.destroy();

      // check that everything is gone
      Assert.assertTrue(pool1.getFreeInstances().isEmpty());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetMetaData() throws Exception {
    try {
      ResourceMetaData descMetaData = mDesc.getMetaData();
      ResourceMetaData poolMetaData = pool1.getMetaData();
      // only UUID should be different
      descMetaData.setUUID(poolMetaData.getUUID());
      Assert.assertEquals(descMetaData, poolMetaData);
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
