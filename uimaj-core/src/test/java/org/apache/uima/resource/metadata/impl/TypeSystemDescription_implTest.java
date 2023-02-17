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

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.UIMAFramework.newDefaultResourceManager;
import static org.apache.uima.test.junit_extension.JUnitExtension.getFile;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
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
import org.apache.uima.util.XMLizable;
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

    assertThat(ts.getTypes()) //
            .as("Types after resolving the descriptor.") //
            .extracting( //
                    t -> FilenameUtils.getName(t.getSourceUrlString()), TypeDescription::getName) //
            .containsExactlyInAnyOrder( //
                    tuple("TestTypeSystem.xml", "NamedEntity"), //
                    tuple("TestTypeSystem.xml", "Person"), //
                    tuple("TestTypeSystem.xml", "Place"), //
                    tuple("TestTypeSystem.xml", "DocumentStructure"), //
                    tuple("TestTypeSystem.xml", "Paragraph"), //
                    tuple("TestTypeSystem.xml", "Sentence"), //
                    tuple("TypeSystemImportedByName.xml", "TestType1"), //
                    tuple("TypeSystemImportedByName.xml", "NamedEntity"), //
                    tuple("TypeSystemImportedByName.xml", "TestType2"), //
                    tuple("TypeSystemImportedByLocation.xml", "TestType1"), //
                    tuple("TypeSystemImportedByLocation.xml", "TestType3"), //
                    tuple("TypeSystemImportedFromDataPath.xml", "TestType4"), //
                    tuple("TypeSystemImportedFromDataPath.xml", "TestType3"));
    List<String> uniqueTypeNames = Stream.of(ts.getTypes()).map(TypeDescription::getName).distinct()
            .sorted().collect(toList());
    assertThat(uniqueTypeNames) //
            .as("Unique type names after resolving the descriptor") //
            .containsExactly("DocumentStructure", "NamedEntity", "Paragraph", "Person", "Place",
                    "Sentence", "TestType1", "TestType2", "TestType3", "TestType4");
    TypeSystemDescription mergedTsd = mergeTypeSystems(asList(ts));
    assertThat(Stream.of(mergedTsd.getTypes()).map(TypeDescription::getName).sorted()
            .collect(toList())).as("Type count after merging all the types ").hasSize(10);

    String typeSystemImportedByLocation = new File(
            "target/test-classes/TypeSystemDescriptionImplTest/TypeSystemImportedByLocation.xml")
                    .toURL().toString();
    String typeSystemImportedFromDataPath = new File(
            "target/test-classes/TypeSystemDescriptionImplTest/dataPathDir/TypeSystemImportedFromDataPath.xml")
                    .toURL().toString();
    String typeSystemImportedByName = new File(
            "target/test-classes/org/apache/uima/resource/metadata/impl/TypeSystemImportedByName.xml")
                    .toURL().toString();

    Map<String, XMLizable> cache = resMgr.getImportCache();
    assertThat(cache).containsOnlyKeys(typeSystemImportedByLocation, typeSystemImportedByName,
            typeSystemImportedFromDataPath);

    TypeSystemDescription typeSystemImportedByLocationTSD = (TypeSystemDescription) cache
            .get(typeSystemImportedByLocation);
    assertThat(typeSystemImportedByLocationTSD.getTypes()).hasSize(2);

    TypeSystemDescription typeSystemImportedFromDataPathTSD = (TypeSystemDescription) cache
            .get(typeSystemImportedFromDataPath);
    assertThat(typeSystemImportedFromDataPathTSD.getTypes()).hasSize(2);

    TypeSystemDescription typeSystemImportedByNameTSD = (TypeSystemDescription) cache
            .get(typeSystemImportedByName);
    assertThat(typeSystemImportedByNameTSD.getTypes()).hasSize(3);
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
