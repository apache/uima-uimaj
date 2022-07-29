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

package org.apache.uima.fit.factory;

import static org.apache.uima.fit.factory.TypePrioritiesFactory.createTypePriorities;
import static org.apache.uima.fit.factory.TypePrioritiesFactory.loadTypePrioritiesFromSPIs;
import static org.apache.uima.fit.factory.TypePrioritiesFactory.loadTypePrioritiesFromScannedLocations;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.spi.TypePrioritiesProviderForTesting;
import org.apache.uima.fit.type.Sentence;
import org.apache.uima.fit.type.Token;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link TypePrioritiesFactory}.
 */
public class TypePrioritiesFactoryTest {

  @Test
  public void testCreateTypePrioritiesFromTypeNames() throws Exception {
    TypePriorities prio = createTypePriorities(CAS.TYPE_NAME_ANNOTATION);

    CasCreationUtils.createCas(createTypeSystemDescription(), prio, null);

    assertThat(prio.getPriorityLists()).hasSize(1);
    assertThat(prio.getPriorityLists()[0].getTypes()).containsExactly(CAS.TYPE_NAME_ANNOTATION);
  }

  @Test
  public void testCreateTypePrioritiesFromClasses() throws Exception {
    TypePriorities prio = createTypePriorities(Annotation.class);

    CasCreationUtils.createCas(createTypeSystemDescription(), prio, null);

    assertThat(prio.getPriorityLists()).hasSize(1);
    assertThat(prio.getPriorityLists()[0].getTypes()).containsExactly(CAS.TYPE_NAME_ANNOTATION);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCreateTypePrioritiesFromBadClasses() throws Exception {
    assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> createTypePriorities((Class) Integer.class));
  }

  @Test
  public void testAutoDetectTypePriorities() throws Exception {
    TypePriorities prio = createTypePriorities();

    TypePriorityList[] typePrioritiesLists = prio.getPriorityLists();
    assertThat(typePrioritiesLists.length).isEqualTo(2);
    assertThat(typePrioritiesLists[0].getTypes()) //
            .containsExactly( //
                    Sentence.class.getName(), //
                    Token.class.getName());
    assertThat(typePrioritiesLists[1].getTypes()) //
            .containsExactly( //
                    TypePrioritiesProviderForTesting.TEST_TYPE_A);
  }

  @Test
  public void testLoadingFromScannedLocations() throws Exception {
    List<TypePriorities> allPrios = new ArrayList<>();
    loadTypePrioritiesFromScannedLocations(allPrios);
    TypePriorities prios = CasCreationUtils.mergeTypePriorities(allPrios, null);

    assertThat(prios.getPriorityLists().length).isEqualTo(1);
    assertThat(prios.getPriorityLists()[0].getTypes()) //
            .containsExactly(Sentence.class.getName(), Token.class.getName());
  }

  @Test
  public void testLoadingFromSPIs() throws Exception {
    List<TypePriorities> allPrios = new ArrayList<>();
    loadTypePrioritiesFromSPIs(allPrios);
    TypePriorities prios = CasCreationUtils.mergeTypePriorities(allPrios, null);

    assertThat(prios.getPriorityLists().length).isEqualTo(1);
    assertThat(prios.getPriorityLists()[0].getTypes()) //
            .containsExactly(TypePrioritiesProviderForTesting.TEST_TYPE_A);
  }
}
