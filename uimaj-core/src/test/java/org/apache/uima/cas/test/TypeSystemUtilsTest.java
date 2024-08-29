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

package org.apache.uima.cas.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.TypeSystemUtils;
import org.apache.uima.cas.impl.TypeSystemUtils.PathValid;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TypeSystemUtilsTest {

  private CAS cas;

  @BeforeEach
  public void setUp() {

    File descriptorFile = JUnitExtension.getFile("CASTests/desc/pathValidationTS.xml");
    assertThat(descriptorFile).as("Descriptor must exist: " + descriptorFile.getAbsolutePath()).exists();

    try {
      XMLParser parser = UIMAFramework.getXMLParser();
      TypeSystemDescription spec = (TypeSystemDescription) parser
              .parse(new XMLInputSource(descriptorFile));
      cas = CasCreationUtils.createCas(spec, null, new FsIndexDescription[] {});
    } catch (ResourceInitializationException e) {
      e.printStackTrace();
      assertThat(false).isTrue();
    } catch (InvalidXMLException e) {
      e.printStackTrace();
      assertThat(false).isTrue();
    } catch (IOException e) {
      e.printStackTrace();
      assertThat(false).isTrue();
    }

  }

  @Test
  void testPathValidation() {
    Type type1 = cas.getTypeSystem().getType("Type1");
    // Type1, f0/begin, always
    List<String> path = new ArrayList<>();
    path.add("f0");
    path.add("begin");
    assertThat(TypeSystemUtils.isPathValid(type1, path) == PathValid.ALWAYS).isTrue();
    // Type1, f1, possible
    path = new ArrayList<>();
    path.add("f1");
    assertThat(TypeSystemUtils.isPathValid(type1, path) == PathValid.POSSIBLE).isTrue();
    // Type1, f1/tail/tail, possible
    path = new ArrayList<>();
    path.add("f1");
    path.add("tail");
    path.add("tail");
    assertThat(TypeSystemUtils.isPathValid(type1, path) == PathValid.POSSIBLE).isTrue();
    // Type1, f2, possible
    path = new ArrayList<>();
    path.add("f2");
    assertThat(TypeSystemUtils.isPathValid(type1, path) == PathValid.POSSIBLE).isTrue();
    // Type1, nosuchfeature, never
    path = new ArrayList<>();
    path.add("nosuchfeature");
    assertThat(TypeSystemUtils.isPathValid(type1, path) == PathValid.NEVER).isTrue();
    // Type1, <empty path>, always
    path = new ArrayList<>();
    assertThat(TypeSystemUtils.isPathValid(type1, path) == PathValid.ALWAYS).isTrue();
    // t1, f1/f2/f3, always
    Type t1 = cas.getTypeSystem().getType("t1");
    path = new ArrayList<>();
    path.add("f1");
    path.add("f2");
    path.add("f3");
    assertThat(TypeSystemUtils.isPathValid(t1, path) == PathValid.ALWAYS).isTrue();
  }

  @AfterEach
  public void tearDown() {
    cas = null;
  }
}
