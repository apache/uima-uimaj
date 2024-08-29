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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.impl.ChildUimaContext_impl;
import org.apache.uima.impl.RootUimaContext_impl;
import org.apache.uima.impl.UimaContext_ImplBase;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.MultiThreadUtils;
import org.apache.uima.resource.CasManager;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLizable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CasPoolTest {

  private static XMLizable parseNoException() {
    try {
      return UIMAFramework.getXMLParser().parse(new XMLInputSource(
              JUnitExtension.getFile("TextAnalysisEngineImplTest/TestPrimitiveTae1.xml")));
    } catch (InvalidXMLException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static final AnalysisEngineDescription aed = (AnalysisEngineDescription) parseNoException();

  private AnalysisEngine analysisEngine;
  private CasManager casManager;

  @BeforeEach
  void setUp() {
    try {
      analysisEngine = UIMAFramework.produceAnalysisEngine(aed);
    } catch (ResourceInitializationException e) {
      throw new RuntimeException(e);
    }
    casManager = ((UimaContext_ImplBase) analysisEngine.getUimaContext()).getResourceManager()
            .getCasManager();
  }

  @Test
  void testCasReleaseNotAllowed() throws Exception {
    final Properties p = new Properties();
    p.put(UIMAFramework.CAS_INITIAL_HEAP_SIZE, 200);
    casManager.defineCasPool("id", 2, p);
    CASImpl c = (CASImpl) casManager.getCas("id");
    c.setCasState(CasState.UIMA_AS_WAIT_4_RESPONSE);
    Exception ex = null;
    try {
      c.release();
    } catch (UIMARuntimeException e) {
      ex = e;
    }
    assertThat(ex != null && ex.getMessage().equals(
        "Illegal invocation of casRelease() while awaiting response from a UIMA-AS Service.")).isTrue();
    c.clearCasState(CasState.UIMA_AS_WAIT_4_RESPONSE);
    assertThatNoException().isThrownBy(() -> c.release() );
  }

  @Test
  void testMultiThread() throws Exception {
    final Properties p = new Properties();
    p.put(UIMAFramework.CAS_INITIAL_HEAP_SIZE, 200);
    int numberOfThreads = Math.min(50, Misc.numberOfCores * 10);
    final int casPoolSize = numberOfThreads / 3;
    System.out.format("test CasPools with %d threads and %d CASes", numberOfThreads, casPoolSize);
    casManager.defineCasPool("id", casPoolSize, p);

    MultiThreadUtils.Run2isb run2isb = new MultiThreadUtils.Run2isb() {

      @Override
      public void call(int i, int r, StringBuilder sb) {
        Random random = new Random();
        for (int k = 0; k < 5; k++) {
          getAndRelease(sb, random);
        }
      }
    };
    MultiThreadUtils.tstMultiThread("CasPoolTest", numberOfThreads, 10, run2isb, new Runnable() {
      @Override
      public void run() {
        try {
          analysisEngine = UIMAFramework.produceAnalysisEngine(aed);
        } catch (ResourceInitializationException e) {
          throw new RuntimeException(e);
        }
        casManager = ((UimaContext_ImplBase) analysisEngine.getUimaContext()).getResourceManager()
                .getCasManager();
        try {
          casManager.defineCasPool("id", casPoolSize, p);
        } catch (ResourceInitializationException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  private void getAndRelease(StringBuilder sb, Random r) {
    CAS c1 = getCas(sb);
    try {
      Thread.sleep(0, r.nextInt(100000));
    } catch (InterruptedException e) {
    }
    releaseCas(c1, sb);
  }

  private AtomicInteger nc = new AtomicInteger(0);

  private CAS getCas(StringBuilder sb) {
    CAS c = casManager.getCas("id");
    sb.append(" ").append(nc.incrementAndGet());
    assertThat(c).isNotNull();
    return c;
  }

  private void releaseCas(CAS c, StringBuilder sb) {
    casManager.releaseCas(c);
    sb.append(" ").append(nc.decrementAndGet());
  }

  // verify that several CASes in a pool in different views share the same type system

  @Test
  void testPool() throws Exception {
      casManager.defineCasPool("uniqueString", 2, null);

      CAS c1 = casManager.getCas("uniqueString");
      CAS c2 = casManager.getCas("uniqueString");
      c1.getJCas();

      CAS c1v2 = c1.createView("view2");
      CAS c2v2 = c2.createView("view3");
      c2v2.getJCas();

      TypeSystem ts = c1.getTypeSystem();

      assertThat(ts).isSameAs(c2.getTypeSystem());
      assertThat(ts).isSameAs(c1v2.getTypeSystem());
      assertThat(ts).isSameAs(c2v2.getTypeSystem());

      casManager.releaseCas(c1v2);
      casManager.releaseCas(c2);

      c1 = casManager.getCas("uniqueString");
      c1.createView("mappedName");
      RootUimaContext_impl rootContext = new RootUimaContext_impl();
      ChildUimaContext_impl context = new ChildUimaContext_impl(rootContext, "abc",
              Collections.singletonMap(CAS.NAME_DEFAULT_SOFA, "mappedName"));
      c1.setCurrentComponentInfo(context.getComponentInfo());
      casManager.releaseCas(c1);
  }
}
