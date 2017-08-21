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
package org.apache.uima.cas.test;

import junit.framework.TestCase;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSIndexRepositoryImpl;
import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;


public class IndexRepositoryTest extends TestCase {

  CAS cas;

  TypeSystem typeSystem;

  FSIndexRepository indexRep;

  private String running;

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    this.cas = CASInitializer.initCas(new CASTestSetup());
    this.typeSystem = this.cas.getTypeSystem();
    this.indexRep = this.cas.getIndexRepository();
  }

  public void tearDown() {
    cas = null;
    typeSystem = null;
    indexRep = null;
  }
  
  public void testMissingSofaRef() throws Exception {
    Type sentType = this.typeSystem.getType(CASTestSetup.SENT_TYPE);
    FeatureStructure sentence = this.cas.createFS(sentType);
    ((CASImpl)cas).ll_setIntValue(((FeatureStructureImpl)sentence).getAddress(),  TypeSystemImpl.annotSofaFeatCode, 0);
    try {
      this.indexRep.addFS(sentence);
    } catch (CASRuntimeException e) {
      assertEquals("SOFAREF_NOT_SET", e.getMessageKey());
      return;
    }
    fail("required exception not thrown"); // fail
  }
  
  public void testDefaultBagIndex() throws Exception {
    // create an instance of a non-annotation type
    Type tokenTypeType = this.typeSystem.getType(CASTestSetup.TOKEN_TYPE_TYPE);
    FeatureStructure tokenTypeFs1 = this.cas.createFS(tokenTypeType);
    assertFalse(tokenTypeFs1 instanceof AnnotationFS);

    // add to indexes
    this.indexRep.addFS(tokenTypeFs1);

    // now try to retrieve
    FSIterator<FeatureStructure> iter = this.indexRep.getAllIndexedFS(tokenTypeType);
    assertTrue(iter.hasNext());
    assertEquals(tokenTypeFs1, iter.next());
    assertFalse(iter.hasNext());

    // add a second instance
    FeatureStructure tokenTypeFs2 = this.cas.createFS(tokenTypeType);
    assertFalse(tokenTypeFs2 instanceof AnnotationFS);
    this.indexRep.addFS(tokenTypeFs2);

    // now there should be two instances in the index
    FSIterator<FeatureStructure> iter2 = this.indexRep.getAllIndexedFS(tokenTypeType);
    assertTrue(iter2.hasNext());
    iter2.next();
    assertTrue(iter2.hasNext());
    iter2.next();
    assertFalse(iter.hasNext());
  }
  
  public void testSetIndex() throws Exception {
    Feature beginFeat = this.typeSystem.getFeatureByFullName(CASTestSetup.TOKEN_TYPE + ":begin");
    // create an instance of an annotation type
    Type tokenTypeType = this.typeSystem.getType(CASTestSetup.TOKEN_TYPE);
    FeatureStructure tokenTypeFs1 = this.cas.createFS(tokenTypeType);
    assertTrue(tokenTypeFs1 instanceof AnnotationFS);
    tokenTypeFs1.setIntValue(beginFeat, 17);
    
    FeatureStructure tokenTypeFs2 = this.cas.createFS(tokenTypeType);
    assertTrue(tokenTypeFs2 instanceof AnnotationFS);
    tokenTypeFs2.setIntValue(beginFeat, 17);
    
    cas.addFsToIndexes(tokenTypeFs1);
    cas.addFsToIndexes(tokenTypeFs2);
    
    FSIndexRepository ir = cas.getIndexRepository();
    FSIndex<FeatureStructure> index = ir.getIndex(CASTestSetup.ANNOT_SET_INDEX);
    assertEquals(1, index.size());

    index = ir.getIndex(CASTestSetup.ANNOT_SORT_INDEX);
    assertEquals(2, index.size());

    // Annotation is supertype of token
    // test if set observes implicit key of type
    Type annotType = this.typeSystem.getType(CAS.TYPE_NAME_ANNOTATION);
    Feature annotBeginFeat = this.typeSystem.getFeatureByFullName(CAS.TYPE_NAME_ANNOTATION + ":begin");
    cas.getIndexRepository().removeAllIncludingSubtypes(annotType);

    FeatureStructure annotTypeFs3 = this.cas.createFS(annotType);
    annotTypeFs3.setIntValue(annotBeginFeat, 17);

    cas.addFsToIndexes(tokenTypeFs1);
    cas.addFsToIndexes(annotTypeFs3);

    index = ir.getIndex(CASTestSetup.ANNOT_SET_INDEX);
    assertEquals(2, index.size());
    
    // shows type is implicit key for set compares
    index = ir.getIndex(CASTestSetup.ANNOT_SET_INDEX_NO_TYPEORDER);
    assertEquals(2, index.size());
    
    
    
    
  }
  
  /**
   * To test non-normal case, change Eclipse run config by adding the jvm arg:
   *   -Duima.allow_duplicate_add_to_indexes
   * @throws CASException
   */
  public void testDupFsIndex() throws CASException {
    JCas jcas = cas.getJCas();
    Annotation a = new Annotation(jcas, 0, 4);
    cas.addFsToIndexes(a);
    cas.addFsToIndexes(a);
    int expected = FSIndexRepositoryImpl.IS_ALLOW_DUP_ADD_2_INDEXES ? 2 : 1;
    assertEquals(expected, cas.getIndexRepository().getIndex(CASTestSetup.ANNOT_SORT_INDEX).size());
    assertEquals(expected, cas.getIndexRepository().getIndex(CASTestSetup.ANNOT_BAG_INDEX).size());
    assertEquals(expected, cas.getIndexRepository().getIndex(CAS.STD_ANNOTATION_INDEX).size());
  }
  
  public static int NBR_ITEMS = 40000;
  
  public void testRemovalSpeed() throws Exception {
    // create an instance of an annotation type
    Feature beginFeat = this.typeSystem.getFeatureByFullName(CASTestSetup.TOKEN_TYPE + ":begin");
    Type fsType = this.typeSystem.getType(CASTestSetup.TOKEN_TYPE);
    FeatureStructure[] fsa = new FeatureStructure[NBR_ITEMS];
    // create 40000 tokens
    for (int i = 0; i < fsa.length; i++) {
      fsa[i] = this.cas.createFS(fsType);
      fsa[i].setIntValue(beginFeat,  i);
    }
    
    // warmup and jit
    timeAdd2Indexes(fsa, false);
    timeRemoveFromIndexes(fsa);
    
    long a2i = timeAdd2Indexes(fsa, false);
    long rfi = timeRemoveFromIndexes(fsa);
    
    long a2i2 = timeAdd2Indexes(fsa, false);
    long rfir = timeRemoveFromIndexesReverse(fsa);
    
    System.out.format("Timing add/remv from indexes: add1: %,d microsec, add2: %,d microsec, rmv: %,d microsec, rmvReversed: %,d microsec%n", 
        a2i/1000, a2i2/1000, rfi/1000, rfir/1000);
// big loop for doing profiling by hand and checking space recovery by hand   
    
//    for (int i = 0; i < 10000; i++) {
//      timeAdd2Indexes(fsa);
//      timeRemoveFromIndexesReverse(fsa);
//    }
  }
  
  public void testAddSpeed() { 
    running = "testAddSpeed - 2 sorted, 1 set, 1 bag";
    runAddSpeed();
  }
  
  // commented out because this was copied from uv3, and uv2 is missing the ir.removeIndex method
//  public void testAddSpeedSorted() {
//    FSIndexRepositoryImpl ir = (FSIndexRepositoryImpl) cas.getIndexRepository();
//    ir.removeIndex(CASTestSetup.ANNOT_SET_INDEX);
//    ir.removeIndex(CASTestSetup.ANNOT_SORT_INDEX);
//    ir.removeIndex(CASTestSetup.ANNOT_BAG_INDEX);
////   ir.removeIndex(CAS.STD_ANNOTATION_INDEX);
//    running = "testAddSpeedSorted";
//    runAddSpeed();
//  }
    
  private void runAddSpeed() { 
    // create an instance of an annotation type
    Feature beginFeat = this.typeSystem.getFeatureByFullName(CASTestSetup.TOKEN_TYPE + ":begin");
    Type fsType = this.typeSystem.getType(CASTestSetup.TOKEN_TYPE);
    FeatureStructure[] fsa = new FeatureStructure[NBR_ITEMS];
    // create 40000 tokens
    for (int i = 0; i < fsa.length; i++) {
      fsa[i] = this.cas.createFS(fsType);
      fsa[i].setIntValue(beginFeat,  i);
    }
    
    // warmup and jit
    long prev = Long.MAX_VALUE;
    for (int i = 0; i < 5 /* 1000 */; i++) {
      cas.getIndexRepository().removeAllIncludingSubtypes(cas.getTypeSystem().getTopType());
      long t = timeAdd2Indexes(fsa, false);
      if (t < prev) {
        System.out.format("%s Iteration %,d Add Forward 40K took  %,d microsec%n", running, i, t/1000);
        prev = t;
      }
    }
    
    prev = Long.MAX_VALUE;
    for (int i = 0; i < 5 /* 10 */; i++) {
      cas.getIndexRepository().removeAllIncludingSubtypes(cas.getTypeSystem().getTopType());
      long t = timeAdd2Indexes(fsa, true);
      if (t < prev) {
        System.out.format("%s Iteration %,d Add Reverse 40K took  %,d microsec%n", running, i, t/1000);
        prev = t;
      }
    }
    
  }
  
  public void testRemovalSpeedBagAlone() throws Exception {
    // create an instance of an non-annotation type
   

    for (int iii = 0; iii < 3 /*10000*/; iii++) { // change to 10000 for iterations
      
//      this.cas = CASInitializer.initCas(new CASTestSetup());
//      this.typeSystem = this.cas.getTypeSystem();
//      this.indexRep = this.cas.getIndexRepository();
      
      // create 40000 token-types
      Type fsType = this.typeSystem.getType(CASTestSetup.TOKEN_TYPE_TYPE);
//      Feature beginFeat = typeSystem.getFeatureByFullName("Token:begin");
      FeatureStructure[] fsa = new FeatureStructure[NBR_ITEMS];
      for (int i = 0; i < fsa.length; i++) {
        fsa[i] = this.cas.createFS(fsType);
//        fsa[i].setIntValue(beginFeat,  i);
      }
    // warmup and jit
    timeAdd2Indexes(fsa, false);
    timeRemoveFromIndexes(fsa);
//    timeAdd2Indexes(fsa);
//    timeRemoveFromIndexes(fsa);
    System.gc();
    long a2i = timeAdd2Indexes(fsa, false);
//    Thread.currentThread().sleep(1000*60*60);  // for using yourkit to investigate memory sizes
    long rfi = timeRemoveFromIndexes(fsa);
    
    long a2i2 = timeAdd2Indexes(fsa, false);
    long rfir = timeRemoveFromIndexesReverse(fsa);
    
//    if (iii == 600) {
//      System.out.println("debug stop");
//    }
    if (iii < 10 || (iii % 200) == 0) {
    System.out.format("%,d Timing add/remv from bag indexes: add1: %,d microsec, add2: %,d microsec, rmv: %,d microsec, rmvReversed: %,d microsec%n", 
        iii, a2i/1000, a2i2/1000, rfi/1000, rfir/1000);
    }
    }
  }

  private long timeAdd2Indexes (FeatureStructure[] fsa, boolean reverse) {
    long start = System.nanoTime();
    if (reverse) {
      AnnotationIndex<AnnotationFS> annotIndex = cas.getAnnotationIndex();
      for (int i = fsa.length - 1; i >= 0; i--) {
        cas.addFsToIndexes(fsa[i]);
        if ((i % 10000) == 9999) {
          annotIndex.size();  // forces batch add to indexes
        }
      }      
    } else {
    for (int i = 0; i < fsa.length; i++) {
      cas.addFsToIndexes(fsa[i]);
    }
  }
    return System.nanoTime() - start;
  }

  private long timeRemoveFromIndexes (FeatureStructure[] fsa) {
    long start = System.nanoTime();
    for (int i = 0; i < fsa.length; i++) {
      cas.removeFsFromIndexes(fsa[i]);
    }
    return System.nanoTime() - start;
  }

  private long timeRemoveFromIndexesReverse (FeatureStructure[] fsa) {
    long start = System.nanoTime();
    for (int i = fsa.length -1; i >= 0; i--) {
      cas.removeFsFromIndexes(fsa[i]);
    }
    return System.nanoTime() - start;
  }
  
}
