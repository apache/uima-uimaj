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

package org.apache.uima.jcas.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.IntegerArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import x.y.z.EndOfSentence;

/**
 * Test FSArrayList
 * 
 */
public class IntegerArrayListTest {

  private CAS cas;

  private JCas jcas;

  private TypeSystem ts;

  public EndOfSentence endOfSentenceInstance;

  @BeforeEach
  public void setUp() throws Exception {
    this.cas = CASInitializer.initCas(new CASTestSetup(), null
    // (tsm -> {
    // Type fsat = tsm.addType("org.apache.uima.jcas.cas.FSArrayList", tsm.getTopType());
    // tsm.addFeature("fsArray", fsat, tsm.getType("uima.cas.FSArray"));
    // }
    // )
    );
    this.ts = this.cas.getTypeSystem();
    this.jcas = cas.getJCas();
  }

  @Test
  public void testBasic() {
    IntegerArrayList al = new IntegerArrayList(jcas);
    al.add(1);
    al.add(2);
    assertFalse(al.remove(3));
    assertTrue(al.remove(1));

    assertEquals(1, al.size());

    Iterator<Integer> it = al.iterator();
    Integer k = null;
    while (it.hasNext()) {
      assertNotNull(k = it.next());
    }
    assertNotNull(k);
    al._save_to_cas_data();
    IntegerArray fa = (IntegerArray) al
            .getFeatureValue(al.getType().getFeatureByBaseName("intArray"));
    assertNotNull(fa);
    assertEquals(fa.get(0), (int) k);
  }

}
