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
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.apache.uima.UIMAFramework.getXMLParser;
import static org.apache.uima.UIMAFramework.newDefaultResourceManager;
import static org.apache.uima.test.junit_extension.JUnitExtension.getFile;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.impl.ResourceManager_impl;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLizable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class ImportResolverTest {
  private final Logger log = LoggerFactory.getLogger(getClass());

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
                    t -> Paths.get(URI.create(t.getSourceUrlString())).getFileName().toString(),
                    TypeDescription::getName) //
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
                    .toURI().toURL().toString();
    String typeSystemImportedFromDataPath = new File(
            "target/test-classes/TypeSystemDescriptionImplTest/dataPathDir/TypeSystemImportedFromDataPath.xml")
                    .toURI().toURL().toString();
    String typeSystemImportedByName = new File(
            "target/test-classes/org/apache/uima/resource/metadata/impl/TypeSystemImportedByName.xml")
                    .toURI().toURL().toString();

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
  public void testTransitiveResolveImports() throws Exception {
    File descriptor = getFile("TypeSystemDescriptionImplTest/Transitive-with-3-nodes-1.xml");
    TypeSystemDescription ts = xmlParser.parseTypeSystemDescription(new XMLInputSource(descriptor));

    assertThat(ts.getTypes()).as("Type count before resolving the descriptor").hasSize(1);

    ResourceManager resMgr = newDefaultResourceManager();
    ts.resolveImports(resMgr);

    assertThat(ts.getTypes()).as("Type count after resolving the descriptor").hasSize(3);
    assertThat(ts.getImports()).hasSize(0);

    String typeSystem2 = new File(descriptor.getParent(), "Transitive-with-3-nodes-2.xml").toURI()
            .toURL().toString();
    String typeSystem3 = new File(descriptor.getParent(), "Transitive-with-3-nodes-3.xml").toURI()
            .toURL().toString();

    Map<String, XMLizable> cache = resMgr.getImportCache();
    assertThat(cache).containsOnlyKeys(typeSystem2, typeSystem3);

    TypeSystemDescription typeSystem2TSD = (TypeSystemDescription) cache.get(typeSystem2);
    assertThat(typeSystem2TSD.getTypes()).hasSize(1);
    assertThat(typeSystem2TSD.getImports()).hasSize(1);

    TypeSystemDescription typeSystem3TSD = (TypeSystemDescription) cache.get(typeSystem3);
    assertThat(typeSystem3TSD.getTypes()).hasSize(1);
    assertThat(typeSystem3TSD.getImports()).hasSize(0);
  }

  @Test
  public void thatComplexImportScenario1Works() throws Exception {

    List<String> entryPoints = asList("tsd0.xml", "tsd5.xml");

    List<String> files = new ArrayList<>();
    try (Stream<Path> fs = Files.list(
            Paths.get("src/test/resources/TypeSystemDescriptionImplTest/complexImportScenario1"))) {
      fs.filter(f -> entryPoints.contains(f.getFileName().toString())) //
              .map(Object::toString).sorted().forEach(files::add);
    }

    Map<String, List<String>> expectedResults = new LinkedHashMap<>();
    expectedResults.put("tsd0.xml", //
            asList("Type0_0", "Type1_0", "Type2_0", "Type3_0", "Type4_0", "Type5_0", "Type6_0"));
    expectedResults.put("tsd5.xml", //
            asList("Type1_0", "Type2_0", "Type3_0", "Type5_0", "Type6_0"));

    Map<String, List<String>> actualResults = new LinkedHashMap<>();

    ResourceManager resMgr = new ResourceManager_impl();
    for (String f : files) {
      TypeSystemDescription tsd = getXMLParser()
              .parseTypeSystemDescription(new XMLInputSource(new File(f)));
      tsd.resolveImports(resMgr);
      List<String> actualUniqueTypeNames = Stream.of(tsd.getTypes()) //
              .map(TypeDescription::getName) //
              .sorted() //
              .distinct() //
              .collect(toList());
      log.debug("{} {}", f, actualUniqueTypeNames);
      actualResults.put(Paths.get(f).getFileName().toString(), actualUniqueTypeNames);
    }

    assertThat(actualResults).containsExactlyEntriesOf(expectedResults);
  }

  @Test
  public void thatResolvingMultipleComplexImportScenariosWithSingleResourceManagerWorks()
          throws Exception {
    final int maxTsCount = 10;
    final int maxPasses = 5;
    final int maxImportsPerPass = 3;
    final int maxTypesPerTypeSystem = 10;
    final int maxFeaturesPerType = 10;
    final int runs = 250;

    log.info("Running {} incrementally growing import test scenarios. This may take a moment...",
            runs);
    log.info("Max. type systems               : {}", maxTsCount);
    log.info("Max. import generation passes   : {}", maxPasses);
    log.info("Max. imports generated per pass : {}", maxImportsPerPass);
    log.info("Max. types per type system      : {}", maxTypesPerTypeSystem);
    log.info("Max. features per type          : {}", maxFeaturesPerType);

    long typeSystemsProcessed = 0;
    long totalDurationAllRuns = 0;
    for (int i = 0; i < runs; i++) {
      final int tsCount = ((i * maxTsCount) / runs) + 1;
      final int passes = ((i * maxPasses) / runs) + 1;
      final int importsPerPass = ((i * maxImportsPerPass) / runs) + 1;
      final int typesPerTypeSystem = ((i * maxTypesPerTypeSystem) / runs) + 1;
      final int featuresPerType = ((i * maxFeaturesPerType) / runs) + 1;

      log.debug(
              "Run {}: TS: {}  types-per-TS: {}  features-per-type: {}  passes: {}  imports-per-pass: {}",
              i + 1, tsCount, typesPerTypeSystem, featuresPerType, passes, importsPerPass);

      List<Entry<File, Set<TypeDescription>>> data = prepareComplexImportScenario(tsCount,
              typesPerTypeSystem, featuresPerType, passes, importsPerPass);

      long totalDurationThisRun = 0;
      ResourceManager resMgr = newDefaultResourceManager();
      for (Entry<File, Set<TypeDescription>> e : data) {
        TypeSystemDescription tsd = getXMLParser()
                .parseTypeSystemDescription(new XMLInputSource(e.getKey()));

        long startTime = System.currentTimeMillis();
        tsd.resolveImports(resMgr);
        long duration = System.currentTimeMillis() - startTime;
        totalDurationThisRun += duration;

        String[] expectedUniqueTypeNames = e.getValue().stream() //
                .map(TypeDescription::getName) //
                .sorted() //
                .distinct() //
                .toArray(String[]::new);

        String[] actualUniqueTypeNames = Stream.of(tsd.getTypes()) //
                .map(TypeDescription::getName) //
                .sorted() //
                .distinct() //
                .toArray(String[]::new);

        log.debug("Types: {}  unique types: {} ({}ms)", tsd.getTypes().length,
                actualUniqueTypeNames.length, duration);

        assertThat(tsd.getTypes())
                // Note that in the general case, there may still be duplicates if the same type is
                // declared in more than one type system description. However, the scenario
                // generator does not cover this case, so we can make this assertion here to check
                // if the deduplication during resolving works
                .as("Deduplication of same type reachable through different paths")
                .hasSameSizeAs(actualUniqueTypeNames);
        assertThat(actualUniqueTypeNames) //
                .as("Mismatch in %s", e.getKey()) //
                .containsExactly(expectedUniqueTypeNames);
        assertThat(tsd.getTypes()) //
                .as("Mismatch in %s", e.getKey()) //
                .containsAll(e.getValue());
      }

      log.debug("Total time spent resolving imports in {} type systems: {}ms", data.size(),
              totalDurationThisRun);
      totalDurationAllRuns += totalDurationThisRun;
      typeSystemsProcessed += data.size();
    }

    log.info("Average import resolving time in {} type systems: {}ms", typeSystemsProcessed,
            totalDurationAllRuns / (double) typeSystemsProcessed);
  }

  private List<Entry<File, Set<TypeDescription>>> prepareComplexImportScenario(int tsCount,
          int typesPerTypeSystem, int featuresPerType, int passes, int importsPerPass)
          throws IOException, SAXException {
    File workDir = new File(
            "target/test-output/TypeSystemDescription_implTest/thatConcurrentImportResolvingWorks");
    FileUtils.deleteQuietly(workDir);
    workDir.mkdirs();

    // Generate random type systems
    Map<File, TypeSystemDescription> allTypeSystems = new LinkedHashMap<>();
    Map<File, Set<TypeDescription>> filesWithTransitiveTypes = new LinkedHashMap<>();
    Map<File, Set<File>> transitiveImportsByFile = new LinkedHashMap<>();
    for (int i = 0; i < tsCount; i++) {
      TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
      for (int t = 0; t < typesPerTypeSystem; t++) {
        TypeDescription type = tsd.addType("Type" + i + "_" + t, "", CAS.TYPE_NAME_TOP);
        for (int f = 0; f < featuresPerType; f++) {
          type.addFeature("Feature_" + i + "_" + t + "_" + f, "", CAS.TYPE_NAME_INTEGER);
        }
      }
      File tsdFile = new File(workDir, "tsd" + i + ".xml");
      allTypeSystems.put(tsdFile, tsd);
      filesWithTransitiveTypes.put(tsdFile, new LinkedHashSet<>(asList(tsd.getTypes())));
      transitiveImportsByFile.put(tsdFile, new LinkedHashSet<>());
    }

    // Add random imports to the type systems
    Map<File, Set<TypeDescription>> transitiveTypesByFile = new LinkedHashMap<>();
    for (int p = 0; p < passes; p++) {
      // System.out.println("===============");
      List<Entry<File, TypeSystemDescription>> allTypeSystemEntries = new ArrayList<>(
              allTypeSystems.entrySet());
      Collections.shuffle(allTypeSystemEntries);
      Iterator<Entry<File, TypeSystemDescription>> allTypeSystemEntriesIterator = allTypeSystemEntries
              .iterator();

      while (allTypeSystemEntriesIterator.hasNext()) {
        Entry<File, TypeSystemDescription> thisTsd = allTypeSystemEntriesIterator.next();
        TypeSystemDescription tsdDesc = thisTsd.getValue();
        File tsdFile = thisTsd.getKey();

        Set<TypeDescription> importedTypes = filesWithTransitiveTypes.get(tsdFile);
        Set<File> importedFiles = transitiveImportsByFile.get(tsdFile);
        Set<Import> imports = new LinkedHashSet<>(asList(tsdDesc.getImports()));
        for (int i = 0; i < importsPerPass && allTypeSystemEntriesIterator.hasNext(); i++) {
          Entry<File, TypeSystemDescription> otherTsd = allTypeSystemEntriesIterator.next();
          Import tsdImport = getResourceSpecifierFactory().createImport();
          // toURL is used intentionally here because we do not want the chars to get escaped
          tsdImport.setLocation(otherTsd.getKey().toURI().toURL().toString());
          imports.add(tsdImport);
          importedTypes.addAll(filesWithTransitiveTypes.get(otherTsd.getKey()));
          importedFiles.add(otherTsd.getKey());
          importedFiles.addAll(transitiveImportsByFile.computeIfAbsent(otherTsd.getKey(),
                  $ -> new LinkedHashSet<>()));
        }

        // System.out.printf("%s imports %s%n", tsdFile.getName(), imports);
        tsdDesc.setImports(imports.stream().toArray(Import[]::new));
        transitiveTypesByFile.put(tsdFile, importedTypes);
      }

      for (Entry<File, TypeSystemDescription> e : allTypeSystemEntries) {
        for (File f : new ArrayList<>(transitiveImportsByFile.get(e.getKey()))) {
          transitiveImportsByFile.get(e.getKey()).addAll(transitiveImportsByFile.get(f));
        }
      }

      for (Entry<File, TypeSystemDescription> e : allTypeSystemEntries) {
        Set<TypeDescription> importedTypes = filesWithTransitiveTypes.get(e.getKey());
        for (File f : transitiveImportsByFile.get(e.getKey())) {
          importedTypes.addAll(filesWithTransitiveTypes.get(f));
        }
      }
    }

    // Write all the type systems to disk
    for (Entry<File, TypeSystemDescription> e : allTypeSystems.entrySet()) {
      try (OutputStream os = Files.newOutputStream(e.getKey().toPath())) {
        e.getValue().toXML(os);
      }
    }

    List<Entry<File, Set<TypeDescription>>> transitiveTypesByFileList = new ArrayList<>(
            transitiveTypesByFile.entrySet());
    transitiveTypesByFileList.sort(comparing(e -> e.getKey().getName()));

    // for (Entry<File, Set<TypeDescription>> e : transitiveTypesByFileList) {
    // Set<TypeDescription> types = e.getValue();
    // System.out.printf("%s %3d Types : %s%n", e.getKey(), types.size(),
    // types.stream().map(TypeDescription::getName).sorted().collect(joining(", ")));
    // Set<File> imports = transitiveImportsByFile.get(e.getKey());
    // System.out.printf("%s %3d Imports: %s%n", e.getKey(), imports.size(),
    // imports.stream().map(File::getName).sorted().collect(joining(", ")));
    // }

    return transitiveTypesByFileList;
  }

  @Test
  public void thatCircularImportsDoNotCrash() throws Exception {
    File descriptor = getFile("TypeSystemDescriptionImplTest/Loop-with-2-nodes-1.xml");
    TypeSystemDescription ts = xmlParser.parseTypeSystemDescription(new XMLInputSource(descriptor));
    ts.resolveImports();
    assertEquals(2, ts.getTypes().length);
  }

  @Test
  public void thatLoopWithTwoNodesDoNotConfuseResourceManagerCache() throws Exception {
    ResourceManager resMgr = newDefaultResourceManager();
    File circular1 = getFile("TypeSystemDescriptionImplTest/Loop-with-2-nodes-1.xml");
    File circular2 = getFile("TypeSystemDescriptionImplTest/Loop-with-2-nodes-2.xml");
    TypeSystemDescription ts = xmlParser.parseTypeSystemDescription(new XMLInputSource(circular1));
    ts.resolveImports(resMgr);
    assertThat(ts.getTypes()).hasSize(2);

    Map<String, XMLizable> cache = resMgr.getImportCache();
    assertThat(cache).containsOnlyKeys(circular2.toURI().toURL().toString());

    TypeSystemDescription cachedCircular2Tsd = (TypeSystemDescription) cache
            .get(circular2.toURI().toURL().toString());
    assertThat(ts.getTypes()).hasSize(2);
    assertThat(cachedCircular2Tsd.getTypes()).hasSize(1);
  }

  @Test
  public void thatLoopWithThreeNodesDoNotConfuseResourceManagerCache() throws Exception {
    ResourceManager resMgr = newDefaultResourceManager();
    File circular1 = getFile("TypeSystemDescriptionImplTest/Loop-with-3-nodes-1.xml");
    File circular2 = getFile("TypeSystemDescriptionImplTest/Loop-with-3-nodes-2.xml");
    File circular3 = getFile("TypeSystemDescriptionImplTest/Loop-with-3-nodes-3.xml");
    TypeSystemDescription ts = xmlParser.parseTypeSystemDescription(new XMLInputSource(circular1));
    ts.resolveImports(resMgr);
    assertThat(ts.getTypes()).hasSize(3);

    Map<String, XMLizable> cache = resMgr.getImportCache();
    assertThat(cache).containsOnlyKeys(circular2.toURI().toURL().toString(),
            circular3.toURI().toURL().toString());

    TypeSystemDescription cachedCircular2Tsd = (TypeSystemDescription) cache
            .get(circular2.toURI().toURL().toString());
    TypeSystemDescription cachedCircular3Tsd = (TypeSystemDescription) cache
            .get(circular3.toURI().toURL().toString());
    assertThat(ts.getTypes()).hasSize(3);
    assertThat(cachedCircular2Tsd.getTypes()).hasSize(1);
    assertThat(cachedCircular3Tsd.getTypes()).hasSize(1);
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
    URL url = getFile("TypeSystemDescriptionImplTest").toURI().toURL();

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
}
