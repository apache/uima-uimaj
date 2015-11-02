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
package org.apache.uima.cas.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.test.CASInitializer;
import org.apache.uima.cas.test.CASTestSetup;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;

import junit.framework.TestCase;

public class Id2FSTest extends TestCase {

  Id2FS id2fs;
  CASImpl cas;
  JCas jcas;
  TypeSystem ts;

  public void setUp() {
    try {
      this.cas = (CASImpl) CASInitializer.initCas(new CASTestSetup());
      this.jcas = cas.getJCas();
      this.ts = this.cas.getTypeSystem();
    } catch (Exception e) {
      assertTrue(false);
    }
  }

  public void testStuff() throws InterruptedException {
    // the normal cas APIs use the gc mode
    
    TOP fs1 = new TOP(jcas);

    int lastUsedId = fs1.id()();
    assertEquals(fs1, cas.<TOP>getFsFromId(lastUsedId));
    // make 20 more that could be gc'd
    
    for (int i = 0; i < 20; i++) {
      new TOP(jcas);
    }
   
    // verify they can be found by id #
    TOP fsh = null;
    for (int i = 0; i < 20; i++) {
      TOP fs = cas.getFsFromId(i + 2);
      if (i == 0) {  // hold onto one so gc doesn't get it
        fsh = fs;
      }
      assertEquals(fs._id, i + 2);      
    }
    
    // remove 19 of them
    System.gc();
    Thread.sleep(10);  // in case gc needs time to finish
    assertTrue(fsh == cas.getFsFromId(2));
    
    for (int i = 1; i < 20; i++) {
      boolean caught = false;
      try {
        TOP fs = cas.getFsFromId(i + 2);
      } catch (CASRuntimeException e) {
        caught = true;
      }
      assertTrue(caught);
      
      caught = false;
      try {
        TOP fs = cas.getFsFromId_checked(i + 2);
      } catch (CASRuntimeException e) {
        caught = true;
      }
      assertTrue(caught);
    }
    
    Id2FS id2fs = new Id2FS(false);  // non gc mode
    cas.reset();
    fs1 = new TOP(jcas);
    
    id2fs.add(fs1);    
    lastUsedId = fs1.id()();
    assertEquals(fs1, id2fs.get(lastUsedId));
    
    for (int i = 0; i < 20; i++) {
      id2fs.add(new TOP(jcas));
    }
    // verify they can be found by id #
    for (int i = 0; i < 20; i++) {
      TOP fs = id2fs.get(i + 2);
      assertEquals(fs._id, i + 2);      
    } 
    
    // remove 20 of them
    System.gc();
    Thread.sleep(10);  // in case gc needs time to finish 
    for (int i = 0; i < 20; i++) {
      TOP fs = id2fs.get(i + 2);
      assertEquals(fs._id, i + 2);
    }    
  }

}
