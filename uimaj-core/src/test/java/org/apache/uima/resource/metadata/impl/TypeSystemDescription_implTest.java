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

import static java.lang.System.identityHashCode;
import static java.util.Arrays.asList;
import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.apache.uima.UIMAFramework.newDefaultResourceManager;
import static org.apache.uima.test.junit_extension.JUnitExtension.getFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TypeSystemDescription_implTest {
  private XMLParser xmlParser;

  @BeforeEach
  public void setUp() throws Exception {
    xmlParser = UIMAFramework.getXMLParser();
    xmlParser.enableSchemaValidation(true);
  }

  @AfterEach
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
  public void testResolveImports() throws Exception {
    File descriptor = getFile("TypeSystemDescriptionImplTest/TestTypeSystem.xml");
    TypeSystemDescription ts = xmlParser.parseTypeSystemDescription(new XMLInputSource(descriptor));

    assertThat(ts.getTypes()).as("Type count before resolving the descriptor").hasSize(6);

    assertThatThrownBy(() -> ts.resolveImports()).isInstanceOf(InvalidXMLException.class);
    assertThat(ts.getTypes()).as(
            "Type count after resolving failed should be same as before / no side effect on exception")
            .hasSize(6);

    // set data path correctly and it should work
    ResourceManager resMgr = newDefaultResourceManager();
    resMgr.setDataPath(
            JUnitExtension.getFile("TypeSystemDescriptionImplTest/dataPathDir").getAbsolutePath());
    ts.resolveImports(resMgr);

    assertThat(ts.getTypes()).as("Type count after resolving the descriptor").hasSize(13);
  }

  @Test
  public void thatCircularImportsDoNotCrash() throws Exception {
    // test that circular imports don't crash
    File descriptor = getFile("TypeSystemDescriptionImplTest/Circular1.xml");
    TypeSystemDescription ts = xmlParser.parseTypeSystemDescription(new XMLInputSource(descriptor));
    ts.resolveImports();
    assertEquals(2, ts.getTypes().length);
  }

  @Test
  public void thatResolveImportsDoesNothingWhenThereAreNoImports() throws Exception {
    // calling resolveImports when there are none should do nothing
    File descriptor = getFile("TypeSystemDescriptionImplTest/TypeSystemImportedByLocation.xml");

    TypeSystemDescription ts = xmlParser.parseTypeSystemDescription(new XMLInputSource(descriptor));

    assertThat(ts.getTypes()).hasSize(2);

    ts.resolveImports();

    assertThat(ts.getTypes()).hasSize(2);
  }

  @Test
  public void thatImportFromProgrammaticallyCreatedTypeSystemDescriptionWorks() throws Exception {
    ResourceManager resMgr = newDefaultResourceManager();
    URL url = getFile("TypeSystemDescriptionImplTest").toURL();

    // test import from programatically created TypeSystemDescription
    Import_impl[] imports = { new Import_impl() };
    imports[0].setSourceUrl(url);
    imports[0].setLocation("TypeSystemImportedByLocation.xml");

    TypeSystemDescription typeSystemDescription = getResourceSpecifierFactory()
            .createTypeSystemDescription();
    typeSystemDescription.setImports(imports);
    TypeSystemDescription typeSystemWithResolvedImports = (TypeSystemDescription) typeSystemDescription
            .clone();
    typeSystemWithResolvedImports.resolveImports(resMgr);

    assertThat(typeSystemWithResolvedImports.getTypes()).isNotEmpty();

    // test that importing the same descriptor twice (using the same ResourceManager) caches
    // the result of the first import and does not create new objects
    TypeSystemDescription typeSystemDescription2 = getResourceSpecifierFactory()
            .createTypeSystemDescription();

    Import_impl[] imports2 = { new Import_impl() };
    imports2[0].setSourceUrl(url);
    imports2[0].setLocation("TypeSystemImportedByLocation.xml");

    typeSystemDescription2.setImports(imports2);
    TypeSystemDescription typeSystemWithResolvedImports2 = (TypeSystemDescription) typeSystemDescription2
            .clone();
    typeSystemWithResolvedImports2.resolveImports(resMgr);

    assertThat(typeSystemWithResolvedImports.getTypes())
            .as("Resolved imports in second type system are the same as in the first (cached)")
            .usingElementComparator((a, b) -> identityHashCode(a) - identityHashCode(b))
            .containsExactlyElementsOf(asList(typeSystemWithResolvedImports2.getTypes()));
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
