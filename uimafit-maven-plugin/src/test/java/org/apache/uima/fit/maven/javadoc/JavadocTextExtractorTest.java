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
package org.apache.uima.fit.maven.javadoc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.apache.uima.fit.maven.util.Util;
import org.junit.jupiter.api.Test;

class JavadocTextExtractorTest {

  @Test
  void testDocOnName() throws Exception {
    assertThat(getJavadoc("value1", "PARAM_VALUE_1")).as("JavaDoc for parameter 'value1'")
            .isEqualTo("Documentation for value 1");
  }

  @Test
  void testDocOnParameterWithName() throws Exception {
    assertThat(getJavadoc("value2", "PARAM_VALUE_2")).as("JavaDoc for parameter 'PARAM_VALUE_2'")
            .isEqualTo("Documentation for value 2");
  }

  @Test
  void testDocOnParameterWithoutName() throws Exception {
    assertThat(getJavadoc("value3", null)).as("JavaDoc for parameter 'value3'")
            .isEqualTo("Documentation for value 3");
  }

  @Test
  void testWithoutDoc() throws Exception {
    assertThat(getJavadoc("value4", null)).as("JavaDoc for parameter 'value4'").isNull();
  }

  @Test
  void testDocOnParameterWithNonLiteralName() throws Exception {
    assertThat(getJavadoc("value5", "PARAM_VALUE_5")).as("JavaDoc for parameter 'value5'")
            .isEqualTo("Documentation for value 5");
  }

  private String getJavadoc(String aParameter, String aNameConstant) throws IOException {
    // Create the Java parser and parse the source code into an abstract syntax tree
    var source = Util.parseSource("src/test/resources/TestComponent.java", "UTF-8");

    return Util.getParameterDocumentation(source, aParameter, aNameConstant);
  }
}
