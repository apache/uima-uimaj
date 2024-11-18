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

import org.apache.uima.fit.maven.util.Util;
import org.junit.jupiter.api.Test;

class ComponentDescriptionExtractorTest {

  @Test
  void test() throws Exception {
    // Create the Java parser and parse the source code into an abstract syntax tree
    var source = Util.parseSource("src/test/resources/TestComponent.java", "UTF-8");

    var javadoc = Util.getComponentDocumentation(source, "some.test.mypackage.TestComponent");

    assertThat(javadoc).as("JavaDoc for TestComponent.java")
            .isEqualTo("A test component used to test JavadocTextExtractor.");
  }
}
