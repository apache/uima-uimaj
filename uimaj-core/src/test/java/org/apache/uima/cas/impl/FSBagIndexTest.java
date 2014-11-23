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

import java.io.File;
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.internal.util.IntPointerIterator;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;

public class FSBagIndexTest extends TestCase {

  private TypeSystemDescription typeSystemDescription;
  
  private TypeSystem ts;

  private FsIndexDescription[] indexes;
  
  private CASImpl cas;

  File typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
  File indexesFile = JUnitExtension.getFile("ExampleCas/testIndexes.xml");

  
  FSBagIndex bi;
  
  
  protected void setUp() throws Exception {
    typeSystemDescription  = UIMAFramework.getXMLParser().parseTypeSystemDescription(
        new XMLInputSource(typeSystemFile1));
    indexes = UIMAFramework.getXMLParser().parseFsIndexCollection(new XMLInputSource(indexesFile))
        .getFsIndexes();
    cas = (CASImpl) CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(), indexes);
    ts = cas.getTypeSystem();
    
    bi = cbi();
  }
  
  private FSBagIndex cbi() {
    return new FSBagIndex(cas, ts.getType("uima.cas.TOP"), 16, FSIndex.BAG_INDEX);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testInsert() {
    // starts out as bit set;
    int[] ns = new int[] {1,15, 33};
    tc(ns);
        
    bi = cbi();
    ns = new int[] {1, 100000, 15, 4};
    tc(ns, 1);
   
    bi = cbi();
    ns = new int[] {1, 100000, 15, 4};
    tc(ns, 1);
    
  }
  
  private void tc(int[] ns) {
    tc(ns, 0);
  }
  
  private void tc(int[] ns, int sortEnd) {
    bi.flush();
    for (int n : ns) {
      bi.insert(n);
    }
    
    if (sortEnd > 0) {
      Arrays.sort(ns, 0, sortEnd);
    }
    
    IntPointerIterator it = bi.getIntIterator();
    for (int n : ns) {
      assertTrue(it.isValid());
      assertEquals(n, it.get());
      it.inc();
    }
    assertFalse(it.isValid());
  }

//  public void testRemove() {
//    fail("Not yet implemented");
//  }
//
//  public void testPointerIterator() {
//    fail("Not yet implemented");
//  }
//
//  public void testGetVector() {
//    fail("Not yet implemented");
//  }
//
//  public void testFlush() {
//    fail("Not yet implemented");
//  }
//
//  public void testLl_iterator() {
//    fail("Not yet implemented");
//  }
//
//  public void testContains() {
//    fail("Not yet implemented");
//  }
//
//  public void testFind() {
//    fail("Not yet implemented");
//  }
//
//  public void testSize() {
//    fail("Not yet implemented");
//  }
//
//  public void testGetIntIterator() {
//    fail("Not yet implemented");
//  }
//
//  public void testIterator() {
//    fail("Not yet implemented");
//  }
//
//  public void testIteratorFeatureStructure() {
//    fail("Not yet implemented");
//  }
//
//  public void testLl_iteratorBoolean() {
//    fail("Not yet implemented");
//  }
//
//  public void testLl_rootIterator() {
//    fail("Not yet implemented");
//  }

}
