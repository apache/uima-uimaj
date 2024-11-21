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
package org.apache.uima.resource.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.PearSpecifier;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.Test;

/**
 * PearSpecifier creation and Xmlization test
 */
class PearSpecifier_implTest {

  @Test
  void thatSerializationWorks() throws Exception {

    var original = makePearSpecifier();

    var copy = UIMAFramework.getXMLParser().parsePearSpecifier(
            new XMLInputSource(getClass().getResource("/XmlParserTest/TestPearSpecifier.xml")));

    assertThat(original.getPearPath()).isEqualTo(copy.getPearPath());

    assertThat(original.getParameters()) //
            .usingRecursiveFieldByFieldElementComparatorOnFields("name", "value")
            .containsExactly(copy.getParameters());

    assertThat(original.getPearParameters()) //
            .usingRecursiveFieldByFieldElementComparatorOnFields("mName", "mValue")
            .containsExactly(copy.getPearParameters());
  }

  @Test
  void thatComparisonAgainstManuallyCreatedSpecifierWorks() throws Exception {
    var spec1 = makePearSpecifier();
    var spec2 = makePearSpecifier();

    assertThat(spec2.getParameters())
            .usingRecursiveFieldByFieldElementComparatorOnFields("name", "value")
            .containsExactly(spec1.getParameters());

    assertThat(spec2.getPearParameters())
            .usingRecursiveFieldByFieldElementComparatorOnFields("mName", "mValue")
            .containsExactly(spec1.getPearParameters());
  }

  @Test
  void testXmlization() throws Exception {
    var spec = new PearSpecifier_impl();
    spec.setPearPath("/home/user/uimaApp/installedPears/testpear");
    spec.setPearParameters( //
            new NameValuePair_impl("param1", "val1"), //
            new NameValuePair_impl("param2", "val2"));

    var sw = new StringWriter();
    spec.toXML(sw);

    try (var is = new ByteArrayInputStream(sw.toString().getBytes(UTF_8))) {
      var copy = (PearSpecifier) UIMAFramework.getXMLParser().parse(new XMLInputSource(is));
      assertEquals(spec, copy);
    }
  }

  PearSpecifier makePearSpecifier() {
    var spec = UIMAFramework.getResourceSpecifierFactory().createPearSpecifier();
    spec.setPearPath("/home/user/uimaApp/installedPears/testpear");
    spec.setParameters( //
            new Parameter_impl("legacyParam1", "legacyVal1"), //
            new Parameter_impl("legacyParam2", "legacyVal2"));
    spec.setPearParameters( //
            new NameValuePair_impl("param1", "stringVal1"), //
            new NameValuePair_impl("param2", true));
    return spec;
  }
}
