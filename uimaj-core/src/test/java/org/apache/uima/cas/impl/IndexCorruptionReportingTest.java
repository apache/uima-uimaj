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

import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndexCorruptionReportingTest {

  private static boolean oldReportFsUpdateCorruptsIndex;
  private static boolean disableAutoProtectIndexes;
  private static boolean exceptionWhenFsUpdateCorruptsIndex;

  private TypeSystemDescription typeSystemDescription;

  private FsIndexDescription[] indexes;

  private CAS cas;

  File typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
  File indexesFile = JUnitExtension.getFile("ExampleCas/testIndexes.xml");

  @BeforeAll
  static void setupClass() {
    oldReportFsUpdateCorruptsIndex = CASImpl.IS_REPORT_FS_UPDATE_CORRUPTS_INDEX = true;
    disableAutoProtectIndexes = CASImpl.IS_DISABLED_PROTECT_INDEXES = true;
    exceptionWhenFsUpdateCorruptsIndex = CASImpl.IS_THROW_EXCEPTION_CORRUPT_INDEX = true;
  }

  @AfterAll
  static void tearDownClass() {
    CASImpl.IS_REPORT_FS_UPDATE_CORRUPTS_INDEX = oldReportFsUpdateCorruptsIndex;
    CASImpl.IS_DISABLED_PROTECT_INDEXES = disableAutoProtectIndexes;
    CASImpl.IS_THROW_EXCEPTION_CORRUPT_INDEX = exceptionWhenFsUpdateCorruptsIndex;
  }

  @BeforeEach
  void setUp() throws Exception {
    var xmlParser = UIMAFramework.getXMLParser();
    typeSystemDescription = xmlParser
            .parseTypeSystemDescription(new XMLInputSource(typeSystemFile1));
    indexes = xmlParser.parseFsIndexCollection(new XMLInputSource(indexesFile)).getFsIndexes();
    cas = createCas(typeSystemDescription, new TypePriorities_impl(), indexes);
  }

  @Test
  void testReport() throws Exception {
    var jcas = cas.getJCas();
    var ann = new Annotation(jcas, 0, 10);
    ann.addToIndexes();

    assertThatExceptionOfType(RuntimeException.class) //
            .isThrownBy(() -> ann.setBegin(2)) //
            .withCauseInstanceOf(UIMARuntimeException.class) //
            .extracting(e -> (UIMARuntimeException) e.getCause()) //
            .satisfies(e -> assertThat(e.getMessageKey())
                    .isEqualTo(UIMARuntimeException.ILLEGAL_FS_FEAT_UPDATE));
  }
}
