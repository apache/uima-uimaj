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
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import x.y.z.EndOfSentence;
import x.y.z.Token;

class FSArrayListTest {

  private CAS cas;

  private JCas jcas;

  private TypeSystem ts;

  public EndOfSentence endOfSentenceInstance;

  @BeforeEach
  public void setUp() throws Exception {
    cas = CASInitializer.initCas(new CASTestSetup(), null // FsArrayList type setup in
                                                          // CASTestSetup's initTypeSystem
    // (tsm -> {
    // Type fsat = tsm.addType("org.apache.uima.jcas.cas.FSArrayList", tsm.getTopType());
    // tsm.addFeature("fsArray", fsat, tsm.getType("uima.cas.FSArray"));
    // }
    // )
    );
    ts = cas.getTypeSystem();
    jcas = cas.getJCas();
  }

  @Test
  void testBasic() {
    FSArrayList<Token> al = new FSArrayList<>(jcas);
    Token t1 = new Token(jcas);
    Token t2 = new Token(jcas);
    al.add(t1);
    al.add(t2);
    al.remove(t1);

    assertEquals(1, al.size());

    Iterator<Token> it = al.iterator();
    Token k = null;
    while (it.hasNext()) {
      assertNotNull(k = it.next());
    }
    assertNotNull(k);
    al._save_fsRefs_to_cas_data();
    FSArray fa = (FSArray) al.getFeatureValue(al.getType().getFeatureByBaseName("fsArray"));
    assertNotNull(fa);
    assertEquals(fa.get(0), k);
  }
}
