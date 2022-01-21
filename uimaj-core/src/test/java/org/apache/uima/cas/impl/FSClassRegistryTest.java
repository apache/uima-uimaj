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

import org.apache.uima.UIMAFramework;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Test;

public class FSClassRegistryTest {
  @Test
  public void thatCreatingResourceManagersWithExtensionClassloaderDoesNotFillUpCache()
          throws Exception {
    for (int i = 0; i < 5; i++) {
      ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
      resMgr.setExtensionClassLoader(getClass().getClassLoader(), true);
      JCas jcas = CasCreationUtils.createCas(null, null, null, resMgr).getJCas();

      ClassLoader cl = jcas.getCasImpl().getJCasClassLoader();
      assertThat(cl.getResource(FSClassRegistryTest.class.getName().replace(".", "/") + ".class")) //
              .isNotNull();

      assertThat(FSClassRegistry.clToType2JCasSize()) //
              .as("System classloader + UIMAClassLoader") //
              .isEqualTo(2);

      resMgr.destroy();

      assertThat(FSClassRegistry.clToType2JCasSize()) //
              .as("System classloader only") //
              .isEqualTo(1);
    }
  }

  @Test
  public void thatCreatingResourceManagersWithExtensionPathDoesNotFillUpCache() throws Exception {
    for (int i = 0; i < 5; i++) {
      ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
      resMgr.setExtensionClassPath("src/test/java", true);
      JCas jcas = CasCreationUtils.createCas(null, null, null, resMgr).getJCas();

      ClassLoader cl = jcas.getCasImpl().getJCasClassLoader();
      assertThat(cl.getResource(FSClassRegistryTest.class.getName().replace(".", "/") + ".java")) //
              .isNotNull();

      assertThat(FSClassRegistry.clToType2JCasSize()) //
              .as("System classloader + UIMAClassLoader") //
              .isEqualTo(2);

      resMgr.destroy();

      assertThat(FSClassRegistry.clToType2JCasSize()) //
              .as("System classloader only") //
              .isEqualTo(1);
    }
  }
}
