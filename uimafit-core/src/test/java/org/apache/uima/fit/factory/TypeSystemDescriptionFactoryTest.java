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

import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.loadTypeSystemDescriptionsFromSPIs;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.loadTypeSystemDescriptionsFromScannedLocations;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.fit.factory.spi.TypeSystemDescriptionProviderForTesting;
import org.apache.uima.fit.type.AnalyzedText;
import org.apache.uima.fit.type.Sentence;
import org.apache.uima.fit.type.Token;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;

public class TypeSystemDescriptionFactoryTest {
  @Test
  public void testFromPath() throws Exception {
    TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(
            "src/test/resources/org/apache/uima/fit/type/AnalyzedText.xml",
            "src/test/resources/org/apache/uima/fit/type/Sentence.xml",
            "src/test/resources/org/apache/uima/fit/type/Token.xml").resolveImports();
  }

  @Test
  public void testScanning() throws Exception {
    TypeSystemDescription tsd = createTypeSystemDescription();
    assertNotNull(tsd.getType(Token.class.getName()));
    assertNotNull(tsd.getType(Sentence.class.getName()));
    assertNotNull(tsd.getType(AnalyzedText.class.getName()));
    assertNotNull(tsd.getType(TypeSystemDescriptionProviderForTesting.TEST_TYPE_A));
  }

  @Test
  public void testLoadingFromScannedLocations() throws Exception {
    List<TypeSystemDescription> tsds = new ArrayList<>();
    loadTypeSystemDescriptionsFromScannedLocations(tsds);
    TypeSystemDescription tsd = CasCreationUtils.mergeTypeSystems(tsds);

    assertNotNull(tsd.getType(Token.class.getName()));
    assertNotNull(tsd.getType(Sentence.class.getName()));
    assertNotNull(tsd.getType(AnalyzedText.class.getName()));
  }

  @Test
  public void testLoadingFromSPIs() throws Exception {
    List<TypeSystemDescription> tsds = new ArrayList<>();
    loadTypeSystemDescriptionsFromSPIs(tsds);
    TypeSystemDescription tsd = CasCreationUtils.mergeTypeSystems(tsds);

    assertNotNull(tsd.getType(TypeSystemDescriptionProviderForTesting.TEST_TYPE_A));
  }
}
