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
import org.assertj.core.api.AbstractBooleanAssert;
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
    for (int i = 0; i < 5; i++) {
      var resMgr = UIMAFramework.newDefaultResourceManager();
      resMgr.setExtensionClassLoader(getClass().getClassLoader(), true);

      var jcas = CasCreationUtils.createCas(null, null, null, resMgr).getJCas();

      var cl = jcas.getCasImpl().getJCasClassLoader();
      assertThat(cl.getResource(FSClassRegistryTest.class.getName().replace(".", "/") + ".class")) //
              .isNotNull();

      assertClassLoaderRegistered(cl,
              "Class loader owned by our ResourceManager should be registered after CAS creation")
              .isTrue();

      resMgr.destroy();

      assertClassLoaderRegistered(cl,
              "Class loader owned by our ResourceManager should be unregistered after destroy")
              .isFalse();
    }
  }

  @Test
  void thatCreatingResourceManagersWithExtensionPathDoesNotFillUpCache() throws Exception {
    for (int i = 0; i < 5; i++) {
      var resMgr = UIMAFramework.newDefaultResourceManager();
      resMgr.setExtensionClassPath("src/test/java", true);
      var jcas = CasCreationUtils.createCas(null, null, null, resMgr).getJCas();

      var cl = jcas.getCasImpl().getJCasClassLoader();
      assertThat(cl.getResource(FSClassRegistryTest.class.getName().replace(".", "/") + ".java")) //
              .isNotNull();

      assertClassLoaderRegistered(cl,
              "Class loader owned by our ResourceManager should be registered after CAS creation")
              .isTrue();

      resMgr.destroy();

      assertClassLoaderRegistered(cl,
              "Class loader owned by our ResourceManager should be unregistered after destroy")
              .isFalse();
    }
  }

  @Test
  void thatJCasClassesCanBeLoadedThroughSPI() throws Exception {
    var jcasClasses = FSClassRegistry.loadJCasClassesFromSPI(getClass().getClassLoader());

    assertThat(jcasClasses).containsOnly( //
            entry(SpiToken.class.getName(), SpiToken.class), //
            entry(SpiSentence.class.getName(), SpiSentence.class));
  }

  // Note: we deliberately do not assert on the absolute number of registered class loaders
  // (FSClassRegistry.clToType2JCasSize()). That cache is a process-wide static weak map shared with
  // all other tests running in the same JVM. Other test classes leave class loaders registered
  // there, and because the keys are weakly referenced they are reaped asynchronously by the GC -
  // which makes the absolute size non-deterministic and was the cause of flaky failures. Instead we
  // assert on the registration state of *our own* class loader, which we keep strongly reachable, so
  // it is immune to other class loaders being reaped.

  private AbstractBooleanAssert<?> assertClassLoaderRegistered(ClassLoader aClassLoader,
          String aDescription) {
    return assertThat(FSClassRegistry.isClToType2JCasRegistered(aClassLoader)) //
            .as(aDescription);
  }
}
