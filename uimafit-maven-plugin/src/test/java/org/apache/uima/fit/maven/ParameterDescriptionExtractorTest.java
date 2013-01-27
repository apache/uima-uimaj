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
package org.apache.uima.fit.maven;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.apache.uima.fit.maven.javadoc.ParameterDescriptionExtractor;
import org.apache.uima.fit.maven.util.Util;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.Test;

public class ParameterDescriptionExtractorTest {

  @Test
  public void testDocOnName() throws Exception {
    ParameterDescriptionExtractor visitor = getExtractor("value1", "PARAM_VALUE_1");

    assertNotNull(visitor.getJavadoc());
    assertNull(visitor.getParameterJavadoc());
    assertNotNull(visitor.getNameConstantJavadoc());
  }

  @Test
  public void testDocOnParameterWithName() throws Exception {
    ParameterDescriptionExtractor visitor = getExtractor("value2", "PARAM_VALUE_2");

    assertNotNull(visitor.getJavadoc());
    assertNotNull(visitor.getParameterJavadoc());
    assertNull(visitor.getNameConstantJavadoc());

  }

  @Test
  public void testDocOnParameterWithoutName() throws Exception {
    ParameterDescriptionExtractor visitor = getExtractor("value3", "PARAM_VALUE_3");

    assertNotNull(visitor.getJavadoc());
    assertNotNull(visitor.getParameterJavadoc());
    assertNull(visitor.getNameConstantJavadoc());
  }

  @Test
  public void testWithoutDoc() throws Exception {
    ParameterDescriptionExtractor visitor = getExtractor("value4", "PARAM_VALUE_4");

    assertNull(visitor.getJavadoc());
    assertNull(visitor.getParameterJavadoc());
    assertNull(visitor.getNameConstantJavadoc());
  }

  @Test
  public void testDocOnParameterWithNonLiteralName() throws Exception {
    ParameterDescriptionExtractor visitor = getExtractor("value5", "PARAM_VALUE_5");

    assertNotNull(visitor.getJavadoc());
    assertNull(visitor.getParameterJavadoc());
    assertNotNull(visitor.getNameConstantJavadoc());
  }

  public static ParameterDescriptionExtractor getExtractor(String aParameter, String aNameConstant)
          throws IOException {
    // Create the Java parser and parse the source code into an abstract syntax tree
    CompilationUnit result = Util.parseSource("src/test/resources/TestComponent.java", "UTF-8");

    // Generate JavaDoc related annotations
    ParameterDescriptionExtractor visitor = new ParameterDescriptionExtractor(aParameter,
            aNameConstant);
    result.accept(visitor);

    return visitor;
  }
}
