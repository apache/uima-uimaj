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
import static org.junit.Assert.assertNotNull;

import java.util.Iterator;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSHashSet;
import org.apache.uima.jcas.cas.FSLinkedHashSet;
import org.apache.uima.jcas.cas.Int2FS;
import org.apache.uima.util.IntEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import x.y.z.EndOfSentence;
import x.y.z.Token;

/**
 * Test FSHashSet
 * 
 */
public class FSHashSetTest {

  private CAS cas;

  private JCas jcas;

  public EndOfSentence endOfSentenceInstance;

  @BeforeEach
  public void setUp() throws Exception {
    cas = CASInitializer.initCas(new CASTestSetup(), null
    // (tsm -> {
    // Type fsat = tsm.addType("org.apache.uima.jcas.cas.FSHashSet", tsm.getTopType());
    // tsm.addFeature("fsArray", fsat, tsm.getType("uima.cas.FSArray"));
    // }
    // )
    );
    jcas = cas.getJCas();
  }

  private void basic(FSHashSet<Token> s) {
    FSHashSet<Token> set = s;
    Token t1 = new Token(jcas);
    Token t2 = new Token(jcas);
    set.add(t1);
    set.add(t2);
    set.remove(t1);

    assertEquals(1, set.size());

    Iterator<Token> it = set.iterator();
    Token k = null;
    while (it.hasNext()) {
      assertNotNull(k = it.next());
    }
    assertNotNull(k);
    set._save_fsRefs_to_cas_data();
    FSArray fa = (FSArray) set.getFeatureValue(set.getType().getFeatureByBaseName("fsArray"));
    assertNotNull(fa);
    assertEquals(fa.get(0), k);
  }

  @Test
  public void testBasic() {
    basic(new FSHashSet<>(jcas));
    basic(new FSLinkedHashSet<>(jcas));
  }

  @Test
  public void testBasicInt2FS() {
    Int2FS<Token> m = new Int2FS<>(jcas);
    Int2FS<Token> m2 = new Int2FS<>(jcas, 11);

    Token t1 = new Token(jcas);
    Token t2 = new Token(jcas);
    m.put(t1._id(), t1);
    m.put(t2._id(), t2);
    m.remove(t1._id());

    assertEquals(1, m.size());

    Iterator<IntEntry<Token>> it = m.iterator();
    IntEntry<Token> k = null;
    while (it.hasNext()) {
      assertNotNull(k = it.next());
    }
    assertNotNull(k);
    m._save_fsRefs_to_cas_data();
    FSArray fa = (FSArray) m.getFeatureValue(m.getType().getFeatureByBaseName("fsArray"));
    assertNotNull(fa);
    assertEquals(fa.get(0), k.getValue());

  }

}
