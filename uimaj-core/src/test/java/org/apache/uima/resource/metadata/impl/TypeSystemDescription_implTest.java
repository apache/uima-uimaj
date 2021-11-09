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
package org.apache.uima.resource.metadata.impl;

import static org.apache.uima.test.junit_extension.JUnitExtension.getFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.io.File;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.PrintExceptionsWhenRunFromCommandLineRule;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

public class TypeSystemDescription_implTest {
  public @Rule TestRule exceptingHandlingRule = new PrintExceptionsWhenRunFromCommandLineRule();

  private XMLParser xmlParser;

  @Before
  public void setUp() throws Exception {
    xmlParser = UIMAFramework.getXMLParser();
    xmlParser.enableSchemaValidation(true);
  }

  @After
  public void tearDown() throws Exception {
    // Note that the XML parser is a singleton in the framework, so we have to set this back to the
    // default.
    xmlParser.enableSchemaValidation(false);
  }

  @Test
  public void testBuildFromXmlElement() throws Exception {
    File descriptor = getFile("TypeSystemDescriptionImplTest/TestTypeSystem.xml");
    TypeSystemDescription ts = xmlParser.parseTypeSystemDescription(new XMLInputSource(descriptor));

    assertThat(ts.getName()).isEqualTo("TestTypeSystem");
    assertThat(ts.getDescription()).isEqualTo("This is a test.");
    assertThat(ts.getVendor()).isEqualTo("The Apache Software Foundation");
    assertThat(ts.getVersion()).isEqualTo("0.1");

    assertThat(ts.getImports()).extracting(Import::getName, Import::getLocation).containsExactly(
            tuple("org.apache.uima.resource.metadata.impl.TypeSystemImportedByName", null),
            tuple(null, "TypeSystemImportedByLocation.xml"),
            tuple("TypeSystemImportedFromDataPath", null));

    assertThat(ts.getTypes())
            .extracting(TypeDescription::getName, TypeDescription::getDescription,
                    TypeDescription::getSupertypeName)
            .containsExactly(
                    tuple("NamedEntity", "Anything that has a name.", "uima.tcas.Annotation"),
                    tuple("Person", "A person.", "NamedEntity"),
                    tuple("Place", "A place.", "NamedEntity"),
                    tuple("DocumentStructure",
                            "Identifies document structure, such as sentence or paragraph.",
                            "uima.tcas.Annotation"),
                    tuple("Paragraph", "A paragraph.", "DocumentStructure"),
                    tuple("Sentence", "A sentence.", "DocumentStructure"));

    assertThat(ts.getTypes()[4].getFeatures())
            .extracting(FeatureDescription::getName, FeatureDescription::getDescription,
                    FeatureDescription::getRangeTypeName, FeatureDescription::getElementType,
                    FeatureDescription::getMultipleReferencesAllowed)
            .containsExactly(
                    tuple("sentences", "Direct references to sentences in this paragraph",
                            "uima.cas.FSArray", "Sentence", false),
                    tuple("testMultiRefAllowedFeature",
                            "A test feature that allows multiple references.", "uima.cas.FSArray",
                            null, true));
  }

  @Test
  public void testInvalidTypeSystem() throws Exception {
    File file = getFile("TypeSystemDescriptionImplTest/InvalidTypeSystem1.xml");

    TypeSystemDescription tsDesc = xmlParser.parseTypeSystemDescription(new XMLInputSource(file));

    assertThatThrownBy(() -> CasCreationUtils.createCas(tsDesc, null, null))
            .isInstanceOf(ResourceInitializationException.class)
            .hasMessageContaining("uima.cas.String");
  }
}
