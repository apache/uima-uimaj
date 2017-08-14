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

import java.io.File;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSIndexRepositoryImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;

/**
 * Class comment for FilteredIteratorTest.java goes here.
 * 
 */
public class AnnotationIndexTest extends TestCase {
  
  static File typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
  static int SZ = 59;
  static int SZp2 = 64;
  
  static long seed =  
new Random().nextLong();
//      -5710808747691817430L; 
  //         -5704695699165084238L; 
                     // 1783099358091571349L;
  static {System.out.println("AnnotationIndexTest random seed: " + seed);}
  static Random r = new Random(seed);
  
  CASImpl cas;
  JCas jcas;
  FSIndexRepositoryImpl ir;
  AnnotationIndex<Annotation> ai;
  TypeSystemImpl tsi;
  Type topType;
  int[] rns;
  
  private Annotation[] as = new Annotation[SZp2];
  private long valTime = 0L;

  protected void setUp() throws Exception {
    long startTime = System.nanoTime();
    TypeSystemDescription typeSystemDescription = UIMAFramework.getXMLParser().parseTypeSystemDescription(
        new XMLInputSource(typeSystemFile1));
    System.out.format("debug time to parse ts: %,d%n", (System.nanoTime() - startTime)/ 1000000);
    startTime = System.nanoTime();
    cas = (CASImpl) CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(), null);
    jcas = cas.getJCas();
    ir = (FSIndexRepositoryImpl) cas.getIndexRepository();
    ai = cas.getAnnotationIndex(); 
    tsi = cas.getTypeSystemImpl();
    topType = tsi.getTopType();
    System.out.format("debug time to create CAS: %,d%n", (System.nanoTime() - startTime)/ 1000000);
    startTime = System.nanoTime();
    //prefill
    int[] ttt = new int[SZp2];
    for (int i = 0; i < SZp2; i++) { ttt[i] = i;}
    ttt = shuffle(ttt);  
      
    
    for (int i = 0; i < SZp2; i++) {
      as[ttt[i]] = new Annotation(jcas, i, i + 200);
      ir.addFS(as[ttt[i]]);
    }

    System.out.format("debug time to create Annotations, add to indexes: %,d%n", (System.nanoTime() - startTime)/ 1000000);
    startTime = System.nanoTime();
  }

  /**
   * 
   * Do a lot of inserts / removes in patterns:
   *   remove n, insert n in random order
   * 
   */
  public void testInsert() {
    insert1(0); 
    valTime = 0L;
    long startTime = System.nanoTime();
    
    int i = 0;
//    for (; i < 100; i++) {  //enable for lots of iterationss, disable for normal test case
      insert1(i);
//    }  
//    System.out.println("debug end");
      long v1 = (System.nanoTime() - startTime) / 1000000;
      long v2 = valTime / 1000000;
      System.out.format("Test SZ: %d  SZp2: %d took %,d milliseconds%n", SZ, SZp2, v1);
      System.out.format("val iter time: %,d insert/remove time: %d%n", v2, v1 - v2);  }
  
  private void insert1(int iter) {
    
//    ir.removeAllIncludingSubtypes(topType);
    for (int i = 0; i < SZ; i++) { ir.removeFS(as[i]); }
    
    add(3, SZ);    
    ai.size();
    
    add(0, 3);
    
    ai.size();  // force batch add
        
    rr(0, 3);
    rr(1, 3);
        
    for (int i = 0; i < 100_000; i++) {
      removeAndReinsertRandom(i);
//      if ((i % 100000) == 0) {
//        System.out.println("random testing OrderedFsSet_array, count: " + i);
//      }
    }

  }
  
  private void add(int ... is) {
    for (int i : is) {
      ir.addFS(as[i]);
    }
  }
  
  private void add(int start, int end) {
    for (int i = start; i < end; i++) {
      ir.addFS(as[i]);
    }
  }
  
  private void rmv(int ... is) {
    for (int i : is) {
      ir.removeFS(as[i]);
    }
  }
  
  private void rmv(int start, int end) {
    for (int i = start; i < end; i++) {
      ir.removeFS(as[i]);
    }
  }
 
  private void removeAndReinsertRandom(int iteration) {
    int n_remove = r.nextInt(SZ);
    if (n_remove == 0) return; 
//    System.out.println(n_remove);
//    int[] rmvd_i = r.ints(0, SZ - 1).distinct().limit(n_remove).toArray();  // a java 8 construct
    
    rns = new int[SZ];
    for (int i = 0; i < SZ; i++) { rns[i] = i; }
    rns = shuffle(rns);
    
    int[] rmvd_i = new int[n_remove];
    System.arraycopy(rns, 0, rmvd_i, 0, n_remove);
    
    int[] adds_i = shuffle(rmvd_i);
//    if (iteration == 12685) {
//      System.out.println("debug 12685");
//    }
    rr(rmvd_i, adds_i);
  }
  
  private void vall() {
    long start = System.nanoTime();
    FSIterator<Annotation> it = ai.iterator();
    for (int i = 0; i < SZ; i ++) {
      Annotation fs = as[i];
      it.moveTo(fs);
      it.moveToPrevious();
      if (it.isValid()) {
        if (fs.getBegin() != it.get().getBegin() + 1) {
          System.out.println("debug mismatch");
        }
      } else {
        if (fs.getBegin() != 0) {
          System.out.println("debug mismatch");
        }
      }
      
    }
    
    valTime += System.nanoTime() - start;
    
//    TOP[] cc = a.getInternalArrayDebug();
//    for (int i = 0; i < SZ; i++) {
//      if (cc[i] == null) {
//        System.out.println("debug found null");
//      }
//    }    
  }
  
  private void rr(int start, int end) {
    rmv(start, end);
    add(start, end);
    ai.size();
    vall();
  }
  
  private void rr(int[] rmv, int[] add) {
    for (int i : rmv) {
      ir.removeFS(as[i]);
    }
    int splt = r.nextInt(add.length) + 1;
    
    for (int i = 0; i < splt; i++) {
      ir.addFS(as[add[i]]);
    }
    ai.size(); 
    
    for (int i = splt; i < add.length; i++) {
      ir.addFS(as[add[i]]);
    }
    ai.size();
    
    vall();
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
