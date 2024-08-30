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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FSBagIndexTest {

  private TypeSystemDescription typeSystemDescription;

  private TypeSystem ts;

  private FsIndexDescription[] indexes;

  private CASImpl cas;

  File typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
  File indexesFile = JUnitExtension.getFile("ExampleCas/testIndexes.xml");

  FsIndex_bag<TOP> bi;

  @BeforeEach
  void setUp() throws Exception {
    typeSystemDescription = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(typeSystemFile1));
    indexes = UIMAFramework.getXMLParser().parseFsIndexCollection(new XMLInputSource(indexesFile))
            .getFsIndexes();
    cas = (CASImpl) CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(),
            indexes);
    ts = cas.getTypeSystem();

    bi = cbi();
  }

  private FsIndex_bag<TOP> cbi() {
    FSIndexComparator comparatorForIndexSpecs = new FSIndexComparatorImpl();
    comparatorForIndexSpecs.setType(ts.getTopType());
    return new FsIndex_bag<>(cas, ts.getType("uima.cas.TOP"), 16, FSIndex.BAG_INDEX,
            comparatorForIndexSpecs);
  }

  @Test
  void testInsert() {
    JCas jcas = cas.getJCas();
    // starts out as bit set;
    TOP[] ns = new TOP[] { new TOP(jcas), new TOP(jcas), new TOP(jcas) };
    tc(ns);

    bi = cbi();
    ns = new TOP[] { new TOP(jcas), new TOP(jcas), new TOP(jcas), new TOP(jcas) };
    tc(ns, 1);

    bi = cbi();
    ns = new TOP[] { new TOP(jcas), new TOP(jcas), new TOP(jcas), new TOP(jcas) };
    tc(ns, 1);

  }

  private void tc(TOP[] ns) {
    tc(ns, 0);
  }

  private void tc(TOP[] ns, int sortEnd) {
    bi.flush();
    for (TOP n : ns) {
      bi.insert(n);
    }

    FSIterator<TOP> it = bi.iterator();
    List<TOP> r = new ArrayList<>();
    for (TOP n : ns) {
      assertThat(it.isValid()).isTrue();
      r.add(it.get());
      it.moveToNext();
    }
    r.sort(FeatureStructureImplC::compare);
    Arrays.sort(ns, FeatureStructureImplC::compare);
    assertThat(Arrays.equals(ns, r.toArray())).isTrue();
    assertThat(it.isValid()).isFalse();
  }
}
