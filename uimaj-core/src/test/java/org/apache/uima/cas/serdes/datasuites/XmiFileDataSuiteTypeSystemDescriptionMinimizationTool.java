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
package org.apache.uima.cas.serdes.datasuites;

import static java.nio.file.Files.isDirectory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.XMLInputSource;

/**
 * Goes through the data case folders of the {@link XmiFileDataSuite}, loads the type systems and
 * the XMIs, creates a new type system that contains only the types and features actually used in
 * the data case (primitive features and arrays are always retained) checks then if the XMI can
 * still be loaded with the minimized type system and then replaces the original type system with
 * the minimized version.
 */
public class XmiFileDataSuiteTypeSystemDescriptionMinimizationTool {
  private static TypeSystemDescription minimizeTypeSystem(CAS aCAS, TypeSystemDescription aSource) {
    TypeSystem ts = aCAS.getTypeSystem();

    // Collect all the reachable FSes and their types
    List<FeatureStructure> reachableFSes = new ArrayList<>();
    ((CASImpl) aCAS).walkReachablePlusFSsSorted(fs -> reachableFSes.add(fs), null, null, null);
    Set<Type> reachableTypes = reachableFSes.stream().map(fs -> fs.getType())
            .collect(Collectors.toCollection(LinkedHashSet::new));

    // Collect element types of arrays
    for (Type t : new ArrayList<>(reachableTypes)) {
      if (t.isArray()) {
        reachableTypes.add(t.getComponentType());
      }

      for (Feature f : t.getFeatures()) {
        if (f.getRange().isArray()) {
          reachableTypes.add(f.getRange().getComponentType());
        }
      }
    }

    // Collect all the super-types of the reachable types
    for (Type t : new ArrayList<>(reachableTypes)) {
      Type st = ts.getParent(t);
      Type prevSt = null;
      while (st != null && st != prevSt) {
        reachableTypes.add(st);
        prevSt = st;
        st = ts.getParent(t);
      }
    }

    TypeSystemDescription result = UIMAFramework.getResourceSpecifierFactory()
            .createTypeSystemDescription();
    for (Type t : reachableTypes) {
      Type sourceType = aCAS.getTypeSystem().getType(t.getName());
      TypeDescription sourceTypeDesc = aSource.getType(t.getName());
      if (sourceTypeDesc != null) {
        TypeDescription targetTypeDesc = result.addType(sourceTypeDesc.getName(),
                sourceTypeDesc.getDescription(), sourceTypeDesc.getSupertypeName());
        targetTypeDesc.setAllowedValues(sourceTypeDesc.getAllowedValues());

        for (FeatureDescription sourceFeatDesc : sourceTypeDesc.getFeatures()) {
          Feature sourceFeature = sourceType.getFeatureByBaseName(sourceFeatDesc.getName());

          Type range = sourceFeature.getRange();
          if (!range.isPrimitive() && !range.isArray() && !range.equals(aCAS.getAnnotationType())
                  && !reachableTypes.contains(range)) {
            // if (sourceFeature.getShortName().equals("Governor")) {
            // System.out.println("");
            // }

            // Skip features referencing types of which we have no feature structures in the CAS
            continue;
          }

          targetTypeDesc.addFeature(sourceFeatDesc.getName(), sourceFeatDesc.getDescription(),
                  sourceFeatDesc.getRangeTypeName(), sourceFeatDesc.getElementType(),
                  sourceFeatDesc.getMultipleReferencesAllowed());
        }
      }
    }

    return result;
  }

  public static void main(String[] args) throws Exception {
    List<Path> caseFolders = new ArrayList<>();
    try (Stream<Path> fileStream = Files.list(XmiFileDataSuite.XMI_SUITE_BASE_PATH)
            .filter(p -> isDirectory(p) && !p.toFile().isHidden())) {
      fileStream.forEach(caseFolders::add);
    }

    for (Path caseFolder : caseFolders) {
      Path typeSystemXmlPath = caseFolder.resolve("typesystem.xml");

      TypeSystemDescription tsd = UIMAFramework.getXMLParser()
              .parseTypeSystemDescription(new XMLInputSource(typeSystemXmlPath.toFile()));

      CAS cas = CasCreationUtils.createCas(tsd, null, null, null);
      try (InputStream is = Files.newInputStream(caseFolder.resolve("data.xmi"))) {
        CasIOUtils.load(is, cas);
      }

      TypeSystemDescription minimizedTsd = minimizeTypeSystem(cas, tsd);

      System.out.printf("%n= %s%n", caseFolder);
      System.out.printf("  Source type system has %d types.%n", tsd.getTypes().length);
      System.out.printf("  Target type system has %d types.%n", minimizedTsd.getTypes().length);
      for (TypeDescription td : minimizedTsd.getTypes()) {
        System.out.printf("    %s%n", td.getName());
      }

      // Verify that the XMI can still be loaded with the minimized type system
      CAS cas2 = CasCreationUtils.createCas(minimizedTsd, null, null, null);
      try (InputStream is = Files.newInputStream(caseFolder.resolve("data.xmi"))) {
        CasIOUtils.load(is, cas2);
      }

      // Replace the original
      try (OutputStream os = Files.newOutputStream(typeSystemXmlPath)) {
        minimizedTsd.toXML(os);
      }
    }
  }
}
