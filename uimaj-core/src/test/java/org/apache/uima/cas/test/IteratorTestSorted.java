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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.LinearTypeOrderBuilder;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Test many variations of rattling iterators (sorted)
 * 
 * Types arranged in levels, Annotation at top, Level_2, Level_3, etc. inheriting to the left
 * 
 * Total number of levels: more than 3 (for sorted part of rattler), something like 5 or 6
 * 
 * NBR_INSTANCES instances per level.
 * 
 * Levels may be skipped.
 * 
 * FSs scattered among levels.  
 * 
 * Each FS: has simple incrementing begin / end (or not - for testing equality operations)
 * 
 * Movement alternatives:
 *   at start, move to first/last/FS template.  
 *   move from arbitrary starting spot to end (detect end, don't move past), 
 *     then reverse to beginning.
 *     -- or reverse of above.
 *     
 * validation: incremental moves: check next id is sequentially +- 1.
 * need map from begin to left-most FS of equal set, for testing moveTo.
 * need to record 1st and last id.
 * validate leftmost for moveTo
 * 
 * Variations:  
 *   number of levels, 
 *   levels that are "skipped"
 *   adding random new elements while iterating, then moveTo
 *   
 *    
 * 
 */
public class IteratorTestSorted extends TestCase {
  static final int REPETITIONS = // 100_000_000; 1000 secs = 17 min
                                  100_000;  // 1 second + startup time ~ .8 sec
  static final int NBR_FSS_PER_LEVEL = 5;
  static final int MAX_LEVELS = 6;  // max is 6 unless adding to the types and JCas class for them
  
  static final String LEVEL_1_BEGIN = "level 1 begin";
  
  JCas jcas;
  Level_1 firstItem;
  Level_1 lastItem;
  int maxBegin;
  int firstBegin;
  
  final int[] nbrElements = new int[MAX_LEVELS];
  
  final ArrayList<Integer> levels = new ArrayList<>();
  FSIterator<Level_1> it;
  
  final static long seed = new Random().nextLong();
//      6658836455455474098L;
//  4811614709790403903L;
  
  static { System.out.println("Iterator Test Sorted, random seed = " + seed); }
  final static Random r = new Random(seed);
    
  public void setUp() {
    CASImpl casMgr = (CASImpl) CASFactory.createCAS();
    TypeSystemImpl tsi = (TypeSystemImpl) casMgr.getTypeSystemMgr();
    TypeImpl annotType = (TypeImpl) tsi.getType(CAS.TYPE_NAME_ANNOTATION);
    TypeImpl level_1_type = (TypeImpl) tsi.addType("org.apache.uima.cas.test.Level_1", annotType);
    tsi.addFeature("id", level_1_type, tsi.floatType);
    TypeImpl level_2_type = (TypeImpl) tsi.addType("org.apache.uima.cas.test.Level_2",  level_1_type);
    TypeImpl level_3_type = (TypeImpl) tsi.addType("org.apache.uima.cas.test.Level_3",  level_2_type);
    TypeImpl level_4_type = (TypeImpl) tsi.addType("org.apache.uima.cas.test.Level_4",  level_3_type);
    TypeImpl level_5_type = (TypeImpl) tsi.addType("org.apache.uima.cas.test.Level_5",  level_4_type);
                                       tsi.addType("org.apache.uima.cas.test.Level_6",  level_5_type);
    
    casMgr.commitTypeSystem();
    try {
      FSIndexRepositoryMgr irm = casMgr.getIndexRepositoryMgr();
      casMgr.initCASIndexes();
      
      FSIndexComparator comp = irm.createComparator();
      comp.setType(level_1_type);
      comp.addKey(level_1_type.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_BEGIN),
              FSIndexComparator.STANDARD_COMPARE);      
      
      irm.createIndex(comp, LEVEL_1_BEGIN, FSIndex.SORTED_INDEX);
      
      irm.commit();
      
      jcas = casMgr.getCurrentView().getJCas();
    } catch (CASException e) {
      e.printStackTrace();
      fail();
    }
  }
  
  public void tearDown() {
    
  }
  
 
  public IteratorTestSorted(String arg) {
    super(arg);
  }
  
  public void testIterator() {
    
    for (int i = 0; i < REPETITIONS; i++) {
      try {
      if (0 == i % 100000) {
        long seed2 =  r.nextLong();
//            5680709196975735850L;

//            2151669209502835073L;
        System.out.format("iteration: %,d seed: %d%n", i, seed2);
        r.setSeed(seed2);
        jcas.reset();  // every so often.  got a fs id internal value overflow after looping 45 million times.
      }
      jcas.removeAllIncludingSubtypes(TOP.type);
      makeFSs(r.nextInt(MAX_LEVELS - 1) + 2);  // 2 - 6
      it = jcas.getIndex(LEVEL_1_BEGIN, Level_1.class).iterator();     
      validate();
      
      // current test case design only allows one of the next two
      // in v2, no support for noticing empty iterators transitioned to non-empty
//      if (r.nextBoolean()) {
        int random_level;
        while (true) {  // this loop because v2 doesn't notice new elements added to previously empty indexes (known limitation)
          random_level = randomLevel();
          if (nbrElements[random_level] != 0) break;
        }
        addRandomFs(random_level);    
        validate();
//      } else {      
//        if (addRandomFsInUnusedType()) {
//          validate();
//        }
//      }
      } catch (AssertionFailedError | IllegalArgumentException e) {
        System.err.format("exception, i = %,d%n", i);
        throw e;
      }
    }
  }
  
  private void validate() {
    it.moveToFirst(); 
    verifyFirst();
    it.moveToLast();
    verifyLast();
    it.moveTo(randomSpot());
    verifyLeftmost();
    
    if (r.nextBoolean()) {
      verifySeqToEnd();
      verifySeqToBegin();
    } else {
      verifySeqToBegin();
      verifySeqToEnd();
    }    
  }
  
  private boolean addRandomFsInUnusedType() {
    if (levels.size() < MAX_LEVELS) {
      int unused;
      while (true) {
        if (levels.contains(unused = r.nextInt(MAX_LEVELS))) {
          break;
        }
      }
      // unused in an index not currently used
      addRandomFs(unused);
      return true;
    }
    return false;
  }
  
  private void addRandomFs(int level_to_make) {
    it.moveTo(randomSpot());
    int begin = it.get().getBegin();
    // it is at leftmost item with begin value.
    float so = it.get().getId();
    // make a new fs, just before this begin  
    Level_1 fs = makeLevel(level_to_make, begin - 1, so - 0.1f);
//    if (Float.compare(0.0f, so) == 0) {
    if (firstItem.getBegin() == begin) {
      firstItem = fs;
    }
  }
    
  
  private Level_1 randomSpot() {
    return makeLevel_inner(0, r.nextInt(maxBegin + 1));  // between 0 and maxBegin
  }
  
  private void verifyFirst() {
    assertTrue(it.get().getBegin() == firstItem.getBegin());  
  }
  
  private void verifyLast() {
    assertTrue(it.get().getBegin() == lastItem.getBegin());
  }
  
  private void verifyLeftmost() {
    Level_1 item = it.get();
    if (item != firstItem) {
      it.moveToPrevious();
      if (it.get().getBegin() == item.getBegin()) {
        fail();
      }
    }
  }
  
  private void verifySeqToEnd() {
    Level_1 prev = it.get();
    while (prev != lastItem) {
      it.moveToNext();
//      assertTrue(it.get().getId() > prev.getId());
      assertTrue(it.get().getBegin() >= prev.getBegin());
      prev = it.get();
    }
  }
  
  private void verifySeqToBegin() {
    Level_1 prev = it.get();
    while (prev != firstItem) {
      it.moveToPrevious();
      assertTrue(it.get().getBegin() <= prev.getBegin());
      prev = it.get();
    }
  }

  
  /**
   * Make a random collection of FSs to iterate over
   *   Random order among types/subtypes
   *     random number of types/subtypes used, from 2 - 6
   *     if number of types < max, the type(s) omitted chosen randomly
   *   Random creation of equals fss to check moving to "leftmost"
   *   
   * @param lvls
   */
  void makeFSs(int lvls) {
    int begin = 0;
    
    int totNbr = NBR_FSS_PER_LEVEL * lvls;
    levels.clear();
    for (int i = 0; i < MAX_LEVELS; i++) levels.add(i);
    for (int i = MAX_LEVELS - lvls; i > 0; i--) levels.remove(randomLevel_index());
    Arrays.fill(nbrElements,  0);
    
    float sortOrder = 0;
    for (int i = 0; i < totNbr; i++) {
      int lastBegin = begin;

      if (r.nextBoolean()) {  // make 50% equal fss
        begin ++;
      }
      
      int random_level = randomLevel();
      nbrElements[random_level] ++;
      Level_1 fs = makeLevel(random_level, begin, sortOrder++);
      if (i == 0) {
        firstItem = fs;
        firstBegin = begin;  // 0 or 1
      }
      if (i == totNbr - 1) {
        lastItem = fs;
        maxBegin = lastBegin;
      }
    }
  }
  
  Level_1 makeLevel_inner(int level, int begin) {
    switch (level) {
    case 0: return new Level_1(jcas, begin, 0);
    case 1: return new Level_2(jcas, begin, 0);
    case 2: return new Level_3(jcas, begin, 0);
    case 3: return new Level_4(jcas, begin, 0);
    case 4: return new Level_5(jcas, begin, 0);
    case 5: return new Level_6(jcas, begin, 0);
    default: fail(); return null;
    }    
  }
  
  Level_1 makeLevel(int level, int begin, float sortOrder) {
    Level_1 fs = makeLevel_inner(level, begin);
    fs.setId(sortOrder);
    fs.addToIndexes();
    return fs;
  }
  
  private int randomLevel_index() {
    return r.nextInt(levels.size());
  }
  
  private int randomLevel() {
    return levels.get(randomLevel_index());
  }
}
