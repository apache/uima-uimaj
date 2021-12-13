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

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// tests without initializing JCas
public class SelectFsNoJCasTest {

  private static TypeSystemDescription typeSystemDescription;

  static private CASImpl cas;

  static File typeSystemFile1 = JUnitExtension
          .getFile("ExampleCas/testTypeSystem_token_sentence_no_jcas.xml");

  @BeforeAll
  public static void setUpClass() throws Exception {
    typeSystemDescription = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(typeSystemFile1));
    cas = (CASImpl) CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(),
            null);
  }

  @Test
  public void testOpsNeedingAnnotation() {
    Type type = cas.getTypeSystem().getType("x.y.z.SentenceNoJCas");
    FeatureStructure s = cas.createAnnotation(type, 0, 4);
    cas.indexRepository.addFS(s);

    boolean b = cas.<Annotation> select(type).covering(1, 2).map(f -> f.getBegin()).findFirst()
            .isPresent();

    assertTrue(b);
  }

}
