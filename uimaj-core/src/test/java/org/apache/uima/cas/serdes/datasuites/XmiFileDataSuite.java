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
import static java.nio.file.Files.newInputStream;
import static org.apache.uima.UIMAFramework.getXMLParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.serdes.transitions.CasSourceTargetConfiguration;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

public class XmiFileDataSuite {
  public static final String TYPESYSTEM_XML = "typesystem.xml";
  public static final String DATA_XMI = "data.xmi";

  private static final Path XMI_SUITE_BASE_PATH = Paths.get("src", "test", "resources",
          "XmiFileDataSuite");

  public static List<CasSourceTargetConfiguration> configurations() throws IOException {
    List<CasSourceTargetConfiguration> confs = new ArrayList<>();

    try (Stream<Path> fileStream = Files.list(XMI_SUITE_BASE_PATH)
            .filter(p -> isDirectory(p) && !p.toFile().isHidden())) {

      fileStream.forEach(testSuiteFolder -> confs.add(buildConfiguration(testSuiteFolder)));
    }

    return confs;
  }

  private static CasSourceTargetConfiguration buildConfiguration(Path aTestSuiteFolder) {
    try {
      Path casFile = aTestSuiteFolder.resolve(DATA_XMI);
      Path tsFile = aTestSuiteFolder.resolve(TYPESYSTEM_XML);

      TypeSystemDescription typeSystemDescription = loadTypeSystem(tsFile);

      return CasSourceTargetConfiguration.builder() //
              .withTitle(aTestSuiteFolder.getFileName().toString()) //
              .withSourceCasSupplier(() -> loadXmi(casFile, typeSystemDescription))
              .withTargetCasSupplier(() -> createCas(typeSystemDescription)) //
              .build();
    } catch (InvalidXMLException | IOException e) {
      throw new Error("Unable to build test configuration for " + aTestSuiteFolder, e);
    }
  }

  private static CAS loadXmi(Path aXmiFile, TypeSystemDescription aTsd)
          throws IOException, InvalidXMLException, ResourceInitializationException {
    CAS cas = createCas(aTsd);

    try (InputStream is = newInputStream(aXmiFile)) {
      CasIOUtils.load(is, cas);
    }

    return cas;
  }

  private static CAS createCas(TypeSystemDescription aTsd)
          throws InvalidXMLException, IOException, ResourceInitializationException {
    return CasCreationUtils.createCas(aTsd, null, null, null);
  }

  private static TypeSystemDescription loadTypeSystem(Path aTypeSystemFile)
          throws IOException, InvalidXMLException {
    try (InputStream is = newInputStream(aTypeSystemFile)) {
      return getXMLParser().parseTypeSystemDescription(new XMLInputSource(is, null));
    }
  }
}
