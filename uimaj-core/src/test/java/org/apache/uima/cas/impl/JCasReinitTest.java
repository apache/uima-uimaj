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

import java.io.File;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.Test;

import aa.T;

public class JCasReinitTest {

  private TypeSystemDescription typeSystemDescription;

  private CASImpl cas_no_features;
  private CASImpl cas;

  public void setup() throws Exception {
  }

  /**
   * Make a type system having type T with a features f1
   * 
   * Make a type system having type T with no features
   * 
   * Have a JCas class for that type, with f1 defined
   * 
   * Create a Cas with a type system with the T(no features)
   * 
   * confirm that the JCas class getter for f1 offset returns -1
   * 
   * Create a Cas with a type system with the T(with f1)
   * 
   * confirm that the JCas class getter for f1 offset returns 0
   * 
   * @throws Throwable
   */

  @Test
  public void testReinit() throws Throwable {
    File typeSystemFile1;

    // // x.y.z.Token with no features
    // typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem_token_no_features.xml");
    // typeSystemDescription = UIMAFramework.getXMLParser().parseTypeSystemDescription(
    // new XMLInputSource(typeSystemFile1));
    //
    // cas = (CASImpl) CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(),
    // null);
    // T.dumpOffset();
    //

    typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem_t_nofeatures.xml");
    typeSystemDescription = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(typeSystemFile1));

    cas_no_features = (CASImpl) CasCreationUtils.createCas(typeSystemDescription,
            new TypePriorities_impl(), null);

    T.dumpOffset();

    typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem_t_1_feature.xml");
    typeSystemDescription = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(typeSystemFile1));

    cas = (CASImpl) CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(),
            null);

    T.dumpOffset();
  }
}
