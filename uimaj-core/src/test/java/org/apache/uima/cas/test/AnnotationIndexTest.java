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

import static org.junit.Assert.fail;

import java.io.File;
import java.util.Random;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSIndexRepositoryImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class comment for FilteredIteratorTest.java goes here.
 */
class AnnotationIndexTest {

  // static class Miter {
  // final int outerIter;
  // final int innerIter;
  // final int itemNbr;
  // final long time;
  //
  // Miter(int out, int in, int item, long time) {
  // this.outerIter = out;
  // this.innerIter = in;
  // this.itemNbr = item;
  // this.time = time;
  // }
  //
  // int getOuterIter() { return outerIter;}
  // int getInnerIter() { return outerIter;}
  // int getItemNbr() { return outerIter;}
  // long getTime() { return time; }
  // }

  static File typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
  // static int SZ = 1111;
  // static int SZp2 = 2048;
  static int SZ = 59;
  static int SZp2 = 64;

  static Random r = new Random();
  static long seed = r.nextLong();
  // -5710808747691817430L;
  // -898838165734156852L;
  // 1783099358091571349L;
  static {
    r.setSeed(seed);
    System.out.println("AnnotationIndexTest random seed: " + seed);
  }

  CASImpl cas;
  JCas jcas;
  FSIndexRepositoryImpl ir;
  AnnotationIndex<Annotation> ai;
  TypeSystemImpl tsi;
  Type topType;
  int[] rns;

  private Annotation[] as = new Annotation[SZp2];
  private long valTime = 0L;

  // ArrayList<Miter> iterTimes = new ArrayList<>(110000);
  //
  // public static ThreadLocal<long[]> startIter = ThreadLocal.withInitial(() -> new long[1]);

  @BeforeEach
  public void setUp() throws Exception {
    long startTime = System.nanoTime();
    TypeSystemDescription typeSystemDescription = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(typeSystemFile1));
    System.out.format("time to parse ts: %,d%n", (System.nanoTime() - startTime) / 1000000);
    startTime = System.nanoTime();
    cas = (CASImpl) CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(),
            null);
    jcas = cas.getJCas();
    ir = (FSIndexRepositoryImpl) cas.getIndexRepository();
    ai = cas.getAnnotationIndex();
    tsi = cas.getTypeSystemImpl();
    topType = tsi.getTopType();
    System.out.format("time to create CAS: %,d%n", (System.nanoTime() - startTime) / 1000000);
    startTime = System.nanoTime();

    // prefill
    int[] ttt = new int[SZp2];
    for (int i = 0; i < SZp2; i++) {
      ttt[i] = i;
    }
    ttt = shuffle(ttt);

    for (int i = 0; i < SZp2; i++) {
      as[ttt[i]] = new Annotation(jcas, i, i + 200);
      ir.addFS(as[ttt[i]]);
    }

    System.out.format("time to create Annotations, add to indexes: %,d%n",
            (System.nanoTime() - startTime) / 1000000);
    startTime = System.nanoTime();

  }

  /**
   * 
   * Do a lot of inserts / removes in patterns: remove n, insert n in random order
   * 
   */
  @Test
  void testInsert() {
    insert1(0);
    valTime = 0L;
    long startTime = System.nanoTime();
    int ii = 0;
    // for (; ii < 100; ii++) { //enable for lots of iterationss, disable for normal test case
    // System.out.println("testInsert outer loop: " + ii);
    insert1(ii);
    // }
    // System.out.println("debug end");
    long v1 = (System.nanoTime() - startTime) / 1000000;
    long v2 = valTime / 1000000;
    System.out.format("Test SZ: %d  SZp2: %d took %,d milliseconds%n", SZ, SZp2, v1);
    System.out.format("val iter time: %,d insert/remove time: %,d%n", v2, v1 - v2);

    // Collections.sort(iterTimes, Comparator.comparingInt(Miter::getOuterIter)
    // .thenComparing(Comparator.comparingLong(Miter::getTime).reversed()));
    //
    // for (int i = 0; i < 20; i++) {
    // Miter m = iterTimes.get(i);
    // System.out.format("outer: %d, time: %,d inner: %,d itemNbr: %,d%n", m.outerIter, m.time,
    // m.innerIter, m.itemNbr);
    // }
    // Miter m = iterTimes.get(99999);
    // System.out.format("outer: %d, time: %,d inner: %,d itemNbr: %,d ref 99999%n", m.outerIter,
    // m.time, m.innerIter, m.itemNbr);
  }

  private void insert1(int iter) {

    // ir.removeAllIncludingSubtypes(topType);
    for (int i = 0; i < SZ; i++) {
      ir.removeFS(as[i]);
    }

    add(3, SZ);
    ai.size();

    add(0, 3);

    ai.size(); // force batch add

    rr(0, 3, -1, -1);
    rr(1, 3, -1, -1);

    for (int i = 0; i < 100_000; i++) {
      // if (i % 1000 == 0) System.out.println("insert test iteration: " + i);
      removeAndReinsertRandom(iter, i);
      // if ((i % 100000) == 0) {
      // System.out.println("random testing OrderedFsSet_array, count: " + i);
      // }
    }

    // System.out.println("debug");

  }

  private void add(int... is) {
    for (int i : is) {
      ir.addFS(as[i]);
    }
  }

  private void add(int start, int end) {
    for (int i = start; i < end; i++) {
      ir.addFS(as[i]);
    }
  }

  private void rmv(int... is) {
    for (int i : is) {
      ir.removeFS(as[i]);
    }
  }

  private void rmv(int start, int end) {
    for (int i = start; i < end; i++) {
      ir.removeFS(as[i]);
    }
  }

  private void removeAndReinsertRandom(int outer, int iteration) {
    int n_remove = r.nextInt(SZ);
    if (n_remove == 0)
      return;
    // System.out.println(n_remove);
    // int[] rmvd_i = r.ints(0, SZ - 1).distinct().limit(n_remove).toArray(); // a java 8 construct

    rns = new int[SZ];
    for (int i = 0; i < SZ; i++) {
      rns[i] = i;
    }
    rns = shuffle(rns);

    int[] rmvd_i = new int[n_remove];
    System.arraycopy(rns, 0, rmvd_i, 0, n_remove);

    int[] adds_i = shuffle(rmvd_i);
    rr(rmvd_i, adds_i, outer, iteration);
  }

  private void vall(int outerIter, int innerIter) {
    long start = System.nanoTime();
    FSIterator<Annotation> it = ai.iterator();

    // if (innerIter == 55555) {
    // System.out.println("debug 55555");
    // }

    for (int i = 0; i < SZ; i++) {
      Annotation fs = as[i];
      // startIter.get()[0] = innerIter > 10000 ? System.nanoTime() : -1;
      // long iterStart = System.nanoTime();
      it.moveTo(fs);
      // long inter2 = System.nanoTime();
      // long inter = inter2 - iterStart;
      // if (innerIter == 55555) {
      // System.out.format("moveTo for innerIter: %,d item: %d took: %,5d %s%n", innerIter, i,
      // inter, fs);
      // }
      // inter2 = System.nanoTime();
      it.moveToPrevious();
      // inter = System.nanoTime() - inter2;
      // if (innerIter == 55555) {
      // System.out.format("moveToPrevious for innerIter: %,d item: %d took: %,5d %s%n", innerIter,
      // i, inter, fs);
      // }
      // if (innerIter > 10000) {
      // iterTimes.add(new Miter(outerIter, innerIter, i, System.nanoTime() - startIter.get()[0]));
      // }
      if (it.isValid()) {
        if (fs.getBegin() != it.get().getBegin() + 1) {
          System.out.println("debug mismatch");
          fail();
        }
      } else {
        if (fs.getBegin() != 0) {
          System.out.println("debug mismatch");
          fail();
        }
      }
    }
    long inc = System.nanoTime() - start;
    valTime += inc;

    // TOP[] cc = a.getInternalArrayDebug();
    // for (int i = 0; i < SZ; i++) {
    // if (cc[i] == null) {
    // System.out.println("debug found null");
    // }
    // }
  }

  private void rr(int start, int end, int outerIter, int innerIter) {
    rmv(start, end);
    add(start, end);
    ai.size();
    vall(outerIter, innerIter);
  }

  private void rr(int[] rmv, int[] add, int outerIter, int innerIter) {
    for (int i : rmv) {
      ir.removeFS(as[i]);
    }
    int splt = r.nextInt(add.length) + 1;

    for (int i = 0; i < splt; i++) {
      ir.addFS(as[add[i]]);
    }
    // if (innerIter == 15) {
    // System.out.println("debug");
    // }
    ai.size();
    // debug

    for (int i = splt; i < add.length; i++) {
      ir.addFS(as[add[i]]);
    }
    ai.size();

    vall(outerIter, innerIter);
  }

  private int[] shuffle(int[] a) {
    int[] b = a.clone();
    for (int i = 0; i < b.length; i++) {
      int j = r.nextInt(b.length);
      int tmp = b[i];
      b[i] = b[j];
      b[j] = tmp;
    }
    return b;
  }
}
