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

import static org.apache.uima.util.CasLoadMode.DEFAULT;
import static org.apache.uima.util.CasLoadMode.REINIT;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.CasLoadMode;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.Test;

public class UimaV2CasCompatibilityTest {

  @Test
  public void thatFileSerializedByUimaV2() throws Exception {
    // Loading these formats from UIMAv2 does not work
    // load("src/test/resources/uima-v2-serialized-cas/simpleCas.bins", REINIT);
    // load("src/test/resources/uima-v2-serialized-cas/simpleCas.bins0", REINIT);
    // load("src/test/resources/uima-v2-serialized-cas/simpleCas.binsp", REINIT);
    
    // Loading these formats from UIMAv2 works
    assertCasLoadsCorrectly("src/test/resources/uima-v2-serialized-cas/simpleCas.bins4", DEFAULT);
    assertCasLoadsCorrectly("src/test/resources/uima-v2-serialized-cas/simpleCas.bins4", REINIT);
    assertCasLoadsCorrectly("src/test/resources/uima-v2-serialized-cas/simpleCas.bins6", DEFAULT);
    assertCasLoadsCorrectly("src/test/resources/uima-v2-serialized-cas/simpleCas.bins6", REINIT);
    assertCasLoadsCorrectly("src/test/resources/uima-v2-serialized-cas/simpleCas.bins6p", DEFAULT);
    assertCasLoadsCorrectly("src/test/resources/uima-v2-serialized-cas/simpleCas.bins6p", REINIT);
    assertCasLoadsCorrectly("src/test/resources/uima-v2-serialized-cas/simpleCas.bins6pTs", DEFAULT);
    assertCasLoadsCorrectly("src/test/resources/uima-v2-serialized-cas/simpleCas.bins6pTs", REINIT);
    assertCasLoadsCorrectly("src/test/resources/uima-v2-serialized-cas/simpleCas.xcas", DEFAULT);
    assertCasLoadsCorrectly("src/test/resources/uima-v2-serialized-cas/simpleCas.xcas", REINIT);
    assertCasLoadsCorrectly("src/test/resources/uima-v2-serialized-cas/simpleCas.xmi", DEFAULT);
    assertCasLoadsCorrectly("src/test/resources/uima-v2-serialized-cas/simpleCas.xmi", REINIT);
  }

  @Test
  public void thatReadingMixedV2andV3FilesWorks() throws Exception {
    assertCasMixedLoadingWorks("simpleCas.bins4", DEFAULT);
    assertCasMixedLoadingWorks("simpleCas.bins4", REINIT);
    assertCasMixedLoadingWorks("simpleCas.bins6", DEFAULT);
    assertCasMixedLoadingWorks("simpleCas.bins6", REINIT);
    assertCasMixedLoadingWorks("simpleCas.bins6p", DEFAULT);
    assertCasMixedLoadingWorks("simpleCas.bins6p", REINIT);
    assertCasMixedLoadingWorks("simpleCas.bins6pTs", DEFAULT);
    assertCasMixedLoadingWorks("simpleCas.bins6pTs", REINIT);
    assertCasMixedLoadingWorks("simpleCas.xcas", DEFAULT);
    assertCasMixedLoadingWorks("simpleCas.xcas", REINIT);
    assertCasMixedLoadingWorks("simpleCas.xmi", DEFAULT);
    assertCasMixedLoadingWorks("simpleCas.xmi", REINIT);
  }

  private CAS makeCas() throws InvalidXMLException, IOException, ResourceInitializationException
  {
    File indexesFile = JUnitExtension.getFile("ExampleCas/testIndexes.xml");
    FsIndexDescription[] indexes = UIMAFramework.getXMLParser()
            .parseFsIndexCollection(new XMLInputSource(indexesFile)).getFsIndexes();

    File typeSystemFile = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
    TypeSystemDescription typeSystem = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(typeSystemFile));

    return CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
  }

  private void assertCasMixedLoadingWorks(String aFile, CasLoadMode aMode)
          throws InvalidXMLException, ResourceInitializationException, IOException {
    CAS cas = makeCas();

    try (InputStream is = new FileInputStream(
            "src/test/resources/uima-v2-serialized-cas/" + aFile)) {
      cas.reset();
      CasIOUtils.load(is, null, cas, aMode);
      assertThat(cas.getAnnotationIndex().size()).isEqualTo(7);
    }

    try (InputStream is = new FileInputStream(
            "src/test/resources/uima-v3-serialized-cas/" + aFile)) {
      cas.reset();
      CasIOUtils.load(is, null, cas, aMode);
      assertThat(cas.getAnnotationIndex().size()).isEqualTo(7);
    }

    try (InputStream is = new FileInputStream(
            "src/test/resources/uima-v2-serialized-cas/" + aFile)) {
      cas.reset();
      CasIOUtils.load(is, null, cas, aMode);
      assertThat(cas.getAnnotationIndex().size()).isEqualTo(7);
    }

    try (InputStream is = new FileInputStream(
            "src/test/resources/uima-v3-serialized-cas/" + aFile)) {
      cas.reset();
      CasIOUtils.load(is, null, cas, aMode);
      assertThat(cas.getAnnotationIndex().size()).isEqualTo(7);
    }
  }

  private void assertCasLoadsCorrectly(String aFile, CasLoadMode aMode)
          throws IOException, InvalidXMLException, ResourceInitializationException {
    CAS cas = makeCas();
    
    try (InputStream is = new FileInputStream(aFile)) {
      CasIOUtils.load(is, null, cas, aMode);
    }
    
    assertThat(cas.getAnnotationIndex().size()).isEqualTo(7);
  }
}
