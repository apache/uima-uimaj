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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Random;

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FsIndex_annotation;
import org.apache.uima.cas.impl.LowLevelIterator;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// @formatter:off
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
 */
//@formatter:on
public class IteratorSortedTest {
  static final int REPETITIONS = // 100_000_000; 1000 secs = 17 min
          100_000; // 1 second + startup time ~ .8 sec
  // 1000000;
  static final int NBR_FSS_PER_LEVEL = 5;
  static final int MAX_LEVELS = 6; // max is 6 unless adding to the types and JCas class for them

  JCas jcas;

  Level_1 firstItem;
  Level_1 lastItem;
  int maxBegin;
  int firstBegin;
  boolean isWithoutTypeOrder;

  final ArrayList<Integer> levels = new ArrayList<>();
  FSIterator<Level_1> it;

  final static Random r = new Random();
  final static long seed = r.nextLong();
  // 6658836455455474098L;
  // 4811614709790403903L;
  static {
    r.setSeed(seed);
    System.out.println("Iterator Test Sorted, random seed = " + seed);
  }

  @BeforeEach
  public void setUp() {
    CASImpl casMgr = (CASImpl) CASFactory.createCAS();
    TypeSystemImpl tsi = (TypeSystemImpl) casMgr.getTypeSystemMgr();
    TypeImpl level_1_type = tsi.addType("org.apache.uima.cas.test.Level_1", tsi.annotType);
    tsi.addFeature("id", level_1_type, tsi.floatType);
    TypeImpl level_2_type = tsi.addType("org.apache.uima.cas.test.Level_2", level_1_type);
    TypeImpl level_3_type = tsi.addType("org.apache.uima.cas.test.Level_3", level_2_type);
    TypeImpl level_4_type = tsi.addType("org.apache.uima.cas.test.Level_4", level_3_type);
    TypeImpl level_5_type = tsi.addType("org.apache.uima.cas.test.Level_5", level_4_type);
    TypeImpl level_6_type = tsi.addType("org.apache.uima.cas.test.Level_6", level_5_type);

    casMgr.commitTypeSystem();
    try {
      FSIndexRepositoryMgr irm = casMgr.getIndexRepositoryMgr();
      casMgr.initCASIndexes();
      irm.commit();

      jcas = casMgr.getCurrentView().getJCas();
    } catch (CASException e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testIterator() {

    for (int i = 0; i < REPETITIONS; i++) {
      try {
        if (0 == i % 100000) {
          long seed2 = r.nextLong();
          // 5680709196975735850L;

          // -4764445956829722324L;
          // 2151669209502835073L;
          System.out.format("iteration: %,d seed: %d%n", i, seed2);
          r.setSeed(seed2);
          jcas.reset(); // every so often. got a fs id internal value overflow after looping 45
                        // million times.
        }
        jcas.removeAllIncludingSubtypes(TOP.type);
        makeFSs(r.nextInt(MAX_LEVELS - 1) + 2); // 2 - 6
        it = ((FsIndex_annotation) jcas.getAnnotationIndex(Level_1.class))
                .iterator(LowLevelIterator.IS_ORDERED, isWithoutTypeOrder = r.nextBoolean());
        if (!isWithoutTypeOrder) {
          firstItem = it.get();
          firstItem.setId(0.0f);
        }
        validate();

        // current design only allows one of the next two
        if (r.nextBoolean()) {
          addRandomFs(randomLevel());
          validate();
        } else {
          if (addRandomFsInUnusedType()) {
            validate();
          }
        }
      } catch (Exception e) {
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
    int begin = it.getNvc().getBegin();
    // it is at leftmost item with begin value.
    float so = it.getNvc().getId();
    // make a new fs, just before this begin
    Level_1 fs = makeLevel(level_to_make, begin - 1, so - 0.1f);
    if (Float.compare(0.0f, so) == 0) {
      firstItem = fs;
    }
  }

  private Level_1 randomSpot() {
    return makeLevel_inner(0, r.nextInt(maxBegin + 1)); // between 0 and maxBegin
  }

  private void verifyFirst() {
    if (isWithoutTypeOrder) {
      assertTrue(it.get() == firstItem);
    } else {
      assertTrue(it.get().getBegin() == firstItem.getBegin()); // because typepriority sort order
    }
  }

  private void verifyLast() {
    if (isWithoutTypeOrder) {
      assertTrue(it.get() == lastItem);
    } else {
      assertTrue(it.get().getBegin() == lastItem.getBegin()); // because typepriority sort order
    }
  }

  private void verifyLeftmost() {
    Level_1 item = it.get();
    // if (item != firstItem) {
    it.moveToPreviousNvc();
    if (it.isValid()) {
      if (it.get().getBegin() == item.getBegin()) {
        fail();
      }
    } else {
      it.moveToFirst();
    }
  }

  private void verifySeqToEnd() {
    Level_1 prev = it.get();
    while (true) {
      it.moveToNextNvc();
      if (!it.isValid()) {
        it.moveToLast();
        break;
      }
      if (isWithoutTypeOrder) {
        assertTrue(it.getNvc().getId() > prev.getId());
      }
      validateEqualOrdering(prev, it.getNvc());
      prev = it.getNvc();
    }
  }

  private void validateEqualOrdering(Level_1 before, Level_1 after) {
    if (before.getBegin() == after.getBegin() && before.getEnd() == after.getEnd()) {
      if (before._getTypeImpl() == after._getTypeImpl() || isWithoutTypeOrder) {
        assertTrue(before._id() < after._id());
      }
    }
  }

  private void verifySeqToBegin() {
    Level_1 prev = it.get();
    while (true) {
      it.moveToPreviousNvc();
      if (!it.isValid()) {
        it.moveToFirst();
        break;
      }
      if (isWithoutTypeOrder) {
        assertTrue(it.getNvc().getId() < prev.getId());
      }
      validateEqualOrdering(it.getNvc(), prev);
      prev = it.getNvc();
    }
  }

//@formatter:off
  /**
   * Make a random collection of FSs to iterate over
   *   Random order among types/subtypes
   *     random number of types/subtypes used, from 2 - 6
   *     if number of types < max, the type(s) omitted chosen randomly
   *   Random creation of equals fss to check moving to "leftmost"
   * 
   * @param lvls
   */
//@formatter:on
  void makeFSs(int lvls) {
    int begin = 0;

    int totNbr = NBR_FSS_PER_LEVEL * lvls;
    levels.clear();
    for (int i = 0; i < MAX_LEVELS; i++)
      levels.add(i);
    for (int i = MAX_LEVELS - lvls; i > 0; i--)
      levels.remove(randomLevel_index());

    float sortOrder = 0;
    for (int i = 0; i < totNbr; i++) {
      int lastBegin = begin;

      if (r.nextBoolean()) { // make 50% equal fss
        begin++;
      }

      Level_1 fs = makeLevel(randomLevel(), begin, sortOrder++);
      if (i == 0) {
        firstItem = fs;
        firstBegin = begin; // 0 or 1
      }
      if (i == totNbr - 1) {
        lastItem = fs;
        maxBegin = lastBegin;
      }
    }
  }

  Level_1 makeLevel_inner(int level, int begin) {
    switch (level) {
      case 0:
        return new Level_1(jcas, begin, 0);
      case 1:
        return new Level_2(jcas, begin, 0);
      case 2:
        return new Level_3(jcas, begin, 0);
      case 3:
        return new Level_4(jcas, begin, 0);
      case 4:
        return new Level_5(jcas, begin, 0);
      case 5:
        return new Level_6(jcas, begin, 0);
      default:
        fail();
        return null;
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
