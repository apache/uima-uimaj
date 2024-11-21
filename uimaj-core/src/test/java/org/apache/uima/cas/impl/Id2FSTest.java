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

import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.test.CASInitializer;
import org.apache.uima.cas.test.CASTestSetup;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Id2FSTest {

  CASImpl cas;
  JCas jcas;
  TypeSystem ts;

  @BeforeEach
  void setUp() throws Exception {
    cas = (CASImpl) CASInitializer.initCas(new CASTestSetup(), null);
    jcas = cas.getJCas();
    ts = cas.getTypeSystem();
  }

  @Test
  void testId2fs() throws InterruptedException {

    TOP fs1 = new TOP(jcas);
    cas.setId2FSsMaybeUnconditionally(fs1);

    int lastUsedId = fs1._id();
    assertThat(cas.<TOP> getFsFromId(lastUsedId)).isEqualTo(fs1);
    // make 20 more that could be gc'd

    for (int i = 0; i < 20; i++) {
      cas.setId2FSsMaybeUnconditionally(new TOP(jcas));
    }

    // verify they can be found by id #
    TOP fsh = null;
    for (int i = 0; i < 20; i++) {
      TOP fs = cas.getFsFromId(i + 2);
      if (i == 0) { // hold onto one so gc doesn't get it
        fsh = fs;
      }
      assertThat(fs._id).isEqualTo(i + 2);
    }

    // // remove 19 of them
    // System.gc();
    // Thread.sleep(10); // in case gc needs time to finish
    // assertTrue(fsh == cas.getFsFromId(2));
    //
    // for (int i = 3; i < 21; i++) { //TOP:21 is held onto by the cas svd cache; might change if we
    // don't cache non corruptable FSs
    // TOP fs = null; // for debugging
    // boolean caught = false;
    // try {
    // fs = cas.getFsFromId_checked(i);
    // } catch (LowLevelException e) {
    // caught = true;
    // }
    // assertTrue( Id2FS.IS_DISABLE_FS_GC || caught);
    // }

    cas.reset();
    fs1 = new TOP(jcas);
    cas.setId2FSsMaybeUnconditionally(fs1);

    lastUsedId = fs1._id();
    assertThat((TOP) cas.getFsFromId(lastUsedId)).isEqualTo(fs1);

    for (int i = 0; i < 20; i++) {
      cas.setId2FSsMaybeUnconditionally(new TOP(jcas));
    }
    // verify they can be found by id #
    for (int i = 0; i < 20; i++) {
      TOP fs = cas.getFsFromId(i + 2);
      assertThat(fs._id).isEqualTo(i + 2);
    }

    // // remove 20 of them
    // System.gc();
    // Thread.sleep(10); // in case gc needs time to finish
    // if (!Id2FS.IS_DISABLE_FS_GC) {
    // for (int i = 0; i < 19; i++) { // last TOP is held by cas.svd.cache_not_in_index
    // TOP fs = cas.getFsFromId(i + 2);
    // assertNull(fs);
    // }
    // }
  }
}
