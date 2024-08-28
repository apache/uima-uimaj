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
import static org.assertj.core.api.Assertions.entry;

import org.apache.uima.UIMAFramework;
import org.apache.uima.spi.SpiSentence;
import org.apache.uima.spi.SpiToken;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FSClassRegistryTest {

  @BeforeEach
  void setup() {
    System.setProperty(FSClassRegistry.RECORD_JCAS_CLASSLOADERS, "true");

    // Calls to FSClassRegistry will fail unless the static initializer block in TypeSystemImpl
    // has previously been triggered! During normal UIMA operations, this should not happen,
    // in particular because FSClassRegistry is not really part of the public UIMA API -
    // but in the minimal setup here, we need to make sure TypeSystemImpl has been initialized
    // first.
    new TypeSystemImpl();
  }

  @Test
  void thatCreatingResourceManagersWithExtensionClassloaderDoesNotFillUpCache() throws Exception {
    int numberOfCachedClassloadersAtStart = FSClassRegistry.clToType2JCasSize();
    for (int i = 0; i < 5; i++) {
      var resMgr = UIMAFramework.newDefaultResourceManager();
      resMgr.setExtensionClassLoader(getClass().getClassLoader(), true);

      var jcas = CasCreationUtils.createCas(null, null, null, resMgr).getJCas();

      var cl = jcas.getCasImpl().getJCasClassLoader();
      assertThat(cl.getResource(FSClassRegistryTest.class.getName().replace(".", "/") + ".class")) //
              .isNotNull();

      assertRegisteredClassLoaders(numberOfCachedClassloadersAtStart + 1,
              "Only initial classloaders + the one owned by our ResourceManager");

      resMgr.destroy();

      assertRegisteredClassLoaders(numberOfCachedClassloadersAtStart, "Only initial classloaders");
    }
  }

  @Test
  void thatCreatingResourceManagersWithExtensionPathDoesNotFillUpCache() throws Exception {
    var numberOfCachedClassloadersAtStart = FSClassRegistry.clToType2JCasSize();

    for (int i = 0; i < 5; i++) {
      var resMgr = UIMAFramework.newDefaultResourceManager();
      resMgr.setExtensionClassPath("src/test/java", true);
      var jcas = CasCreationUtils.createCas(null, null, null, resMgr).getJCas();

      var cl = jcas.getCasImpl().getJCasClassLoader();
      assertThat(cl.getResource(FSClassRegistryTest.class.getName().replace(".", "/") + ".java")) //
              .isNotNull();

      assertRegisteredClassLoaders(numberOfCachedClassloadersAtStart + 1,
              "Only initial classloaders + the one owned by our ResourceManager");

      resMgr.destroy();

      assertRegisteredClassLoaders(numberOfCachedClassloadersAtStart, "Only initial classloaders");
    }
  }

  @Test
  void thatJCasClassesCanBeLoadedThroughSPI() throws Exception {
    var jcasClasses = FSClassRegistry.loadJCasClassesFromSPI(getClass().getClassLoader());

    assertThat(jcasClasses).containsOnly( //
            entry(SpiToken.class.getName(), SpiToken.class), //
            entry(SpiSentence.class.getName(), SpiSentence.class));
  }

  private void assertRegisteredClassLoaders(int aExpectedCount, String aDescription) {
    if (FSClassRegistry.clToType2JCasSize() > aExpectedCount) {
      FSClassRegistry.log_registered_classloaders(Level.INFO);
    }

    assertThat(FSClassRegistry.clToType2JCasSize()) //
            .as(aDescription) //
            .isEqualTo(aExpectedCount);
  }
}
