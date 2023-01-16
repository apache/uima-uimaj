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

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SelectFsFSListTest {
  private JCas jcas;

  @BeforeEach
  void setup() throws Exception {
    jcas = CasCreationUtils.createCas().getJCas();
    jcas.setDocumentText("This is a test.");
  }

  @Test
  void thatSelectCountWithTypeIsConsistentWithArraySize() throws Exception {
    int size = 10;

    NonEmptyFSList<TOP> list = new NonEmptyFSList<TOP>(jcas, new Annotation(jcas));
    for (int i = 1; i < size; i++) {
      list = new NonEmptyFSList<TOP>(jcas, new Annotation(jcas), list);
    }

    assertThat(list) //
            .hasSize(size) //
            .hasSize((int) list.select(Annotation.class).count()) //
            .hasSameSizeAs(list.select(Annotation.class));

    list.addToIndexes();

    assertThat(list) //
            .hasSize(size) //
            .hasSize((int) list.select(Annotation.class).count()) //
            .hasSameSizeAs(list.select(Annotation.class));
  }

  @Test
  void thatSelectCountWithSubTypeIsConsistentWithArraySize() throws Exception {
    int size = 10;

    NonEmptyFSList<TOP> list = new NonEmptyFSList<TOP>(jcas, new Annotation(jcas));
    for (int i = 1; i < size; i++) {
      switch (i % 2) {
        case 0:
          list = new NonEmptyFSList<TOP>(jcas, new Annotation(jcas), list);
          break;
        case 1:
          list = new NonEmptyFSList<TOP>(jcas, new AnnotationBase(jcas), list);
          break;
      }
    }

    assertThat(list) //
            .hasSize(size) //
            .hasSize((int) list.select(AnnotationBase.class).count())
            .hasSameSizeAs(list.select(AnnotationBase.class));

    assertThat(list.select(Annotation.class).count()).isEqualTo(5);
  }
}
