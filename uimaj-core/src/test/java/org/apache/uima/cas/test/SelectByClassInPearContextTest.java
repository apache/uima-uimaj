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
package org.apache.uima.cas.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;

import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.internal.util.UIMAClassLoader;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.test.IsolatingClassloader;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;

/**
 * Regression test for
 * <a href="https://github.com/apache/uima-uimaj/issues/384">issue #384</a>.
 * <p>
 * In a PEAR scenario, the PEAR's classloader may redefine the JCas wrapper for a super-type while
 * the wrappers for sub-types remain visible only via the parent classloader. Those sub-type
 * wrappers therefore do not extend the PEAR's copy of the super-type. When PEAR code calls
 * {@code select(superType.class)}, the iterator must not return sub-type instances that cannot be
 * cast to the PEAR's super-type class -- otherwise idiomatic UIMA code such as
 * {@code for (T t : cas.select(T.class))} fails with a {@link ClassCastException}.
 */
class SelectByClassInPearContextTest {

  @Test
  void thatSelectByClassFiltersIncompatibleSubtypeInstancesInPearContext() throws Exception {
    var rootCl = getClass().getClassLoader();

    // The PEAR redefines only the super-type (Level_1). The sub-type (Level_2) is NOT redefined,
    // so when looked up via the PEAR classloader it is delegated to the parent classloader -- and
    // that root-classloader Level_2 extends the root-classloader Level_1, not the PEAR's Level_1.
    var clForLevel1 = new IsolatingClassloader("Level_1", rootCl)
            .redefining("org\\.apache\\.uima\\.cas\\.test\\.Level_1(_Type)?.*");

    var casImpl = (CASImpl) CasCreationUtils.createCas(buildLevelsTypeSystem(), null, null, null);

    var level2Type = casImpl.getTypeSystem().getType(Level_2.class.getName());
    var level2 = casImpl.createAnnotation(level2Type, 0, 1);
    level2.addToIndexes();

    casImpl.switchClassLoaderLockCasCL(new UIMAClassLoader(new URL[0], clForLevel1));

    @SuppressWarnings({ "rawtypes", "unchecked" })
    Class<? extends TOP> pearLevel1Class = (Class) clForLevel1.loadClass(Level_1.class.getName());
    assertThat(pearLevel1Class.getClassLoader())
            .as("Sanity check: the PEAR's Level_1 class is loaded by the PEAR classloader")
            .isSameAs(clForLevel1);
    assertThat(pearLevel1Class)
            .as("Sanity check: the PEAR's Level_1 class is a different Class object than the "
                    + "one loaded by the root classloader")
            .isNotSameAs(Level_1.class);

    // Sanity check: the Level_2 instance is reachable in the CAS via a type-based selector.
    assertThat(casImpl.select(level2Type).asList())
            .as("The Level_2 FS must be present in the CAS")
            .hasSize(1);

    // The actual contract under test: select(SuperType.class) must only return FSes that are
    // assignable to the requested class. The Level_2 instance's JCas wrapper is loaded by the
    // parent classloader and is not assignable to the PEAR's Level_1, so it must be filtered out.
    assertThat(casImpl.select(pearLevel1Class).asList())
            .as("select(PEAR_Level_1.class) must only return instances assignable to "
                    + "the PEAR's copy of Level_1")
            .allMatch(pearLevel1Class::isInstance);
  }

  /**
   * Builds a minimal type system with a Level_1 super-type and a Level_2 sub-type. JCas wrappers
   * for both already exist in this package; they extend each other with matching {@code _TypeName}
   * fields, so they are picked up automatically when the type system is committed.
   */
  private static TypeSystemImpl buildLevelsTypeSystem() throws ResourceInitializationException {
    var casMgr = (CASImpl) CASFactory.createCAS();
    var tsi = (TypeSystemImpl) casMgr.getTypeSystemMgr();
    TypeImpl level1 = tsi.addType(Level_1.class.getName(), tsi.annotType);
    tsi.addType(Level_2.class.getName(), level1);
    casMgr.commitTypeSystem();
    try {
      FSIndexRepositoryMgr irm = casMgr.getIndexRepositoryMgr();
      casMgr.initCASIndexes();
      irm.commit();
    } catch (Exception e) {
      throw new ResourceInitializationException(e);
    }
    return tsi;
  }
}
