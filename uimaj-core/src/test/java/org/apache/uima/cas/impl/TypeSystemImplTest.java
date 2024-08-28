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

import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.InstanceOfAssertFactories.throwable;

import java.lang.management.ManagementFactory;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.impl.ResourceManager_impl;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import x.y.z.Sentence;

class TypeSystemImplTest {
  private TypeSystemDescription tsd;

  @BeforeEach
  void setup() {
    tsd = getResourceSpecifierFactory().createTypeSystemDescription();
  }

  @Test
  void thatTypeUsedInJavaAndDeclaredInTypeSytemDoesNotThrowException() throws Exception {
    tsd.addType(Sentence._TypeName, "", CAS.TYPE_NAME_ANNOTATION);

    var localJcas = createCas(tsd, null, null).getJCas();
    localJcas.setDocumentText("This is a test.");

    assertThatNoException() //
            .isThrownBy(() -> localJcas.getCasType(Sentence.type));
  }

  @Test
  void thatTypeUsedInJavaButNotDeclaredInTypeSytemThrowsException() throws Exception {
    var localJcas = createCas(tsd, null, null).getJCas();
    localJcas.setDocumentText("This is a test.");

    assertThat(JCasRegistry.getClassForIndex(Sentence.type)).isSameAs(Sentence.class);

    assertThatExceptionOfType(CASRuntimeException.class) //
            .isThrownBy(() -> localJcas.getCasType(Sentence.type)) //
            .asInstanceOf(throwable(CASRuntimeException.class)) //
            .extracting(CASRuntimeException::getMessageKey) //
            .isEqualTo(CASRuntimeException.JCAS_TYPE_NOT_IN_CAS);

    sanityCheckForCasConsistencyUIMA_738(localJcas);
  }

  @Test
  void thatTypeNotInTypeSystemAndWithoutJCasClassThrowsException() throws Exception {
    var localJcas = createCas(tsd, null, null).getJCas();
    localJcas.setDocumentText("This is a test.");

    assertThatExceptionOfType(CASRuntimeException.class) //
            .isThrownBy(() -> localJcas.getCasType(Integer.MAX_VALUE)) //
            .asInstanceOf(throwable(CASRuntimeException.class)) //
            .extracting(CASRuntimeException::getMessageKey) //
            .isEqualTo(CASRuntimeException.JCAS_UNKNOWN_TYPE_NOT_IN_CAS);

    sanityCheckForCasConsistencyUIMA_738(localJcas);
  }

  void sanityCheckForCasConsistencyUIMA_738(JCas localJcas) {
    // check that this does not leave JCAS in an inconsistent state
    // (a check for bug UIMA-738)
    var iter = localJcas.getAnnotationIndex().iterator();

    assertThat(iter).hasNext();
    assertThat(iter.next()) //
            .extracting(Annotation::getCoveredText) //
            .isEqualTo("This is a test.");
  }

  @Test
  void testMetaspaceExhaustion() throws Exception {
    var threshold = 2_500;

    var classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
    var classesLoadedAtStart = classLoadingMXBean.getLoadedClassCount();

    var type = tsd.addType(Sentence._TypeName, "", CAS.TYPE_NAME_ANNOTATION);
    type.addFeature(Sentence._FeatName_sentenceLength, null, CAS.TYPE_NAME_INTEGER);

    for (var i = 0; i < threshold * 2; i++) {
      var resMgr = new ResourceManager_impl();
      resMgr.setExtensionClassPath(".", false);
      createCas(tsd, null, null, null, resMgr).getJCas();
      System.runFinalization();
      // Make sure the consolidated type system is evicted from the weak hashmap cache
      System.gc();

      assertThat(classLoadingMXBean.getLoadedClassCount()) //
              .as("High number of new loaded classes during test indicates leak")
              .isLessThan(classesLoadedAtStart + threshold);
    }
  }
}
