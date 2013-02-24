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

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.uima.fit.maven.util.Util;
import org.junit.Test;

import com.thoughtworks.qdox.model.JavaSource;

public class JavadocTextExtractorTest {

  @Test
  public void testDocOnName() throws Exception {
    String doc = getJavadoc("value1", "PARAM_VALUE_1");
    assertEquals("Documentation for value 1", doc);
  }
  
  @Test
  public void testDocOnParameterWithName() throws Exception {
    String doc = getJavadoc("value2", "PARAM_VALUE_2");
    assertEquals("Documentation for value 2", doc);
  }

  @Test
  public void testDocOnParameterWithoutName() throws Exception {
    String doc = getJavadoc("value3", null);
    assertEquals("Documentation for value 3", doc);
  }

  @Test
  public void testWithoutDoc() throws Exception {
    String doc = getJavadoc("value4", null);
    assertEquals(null, doc);
  }

  @Test
  public void testDocOnParameterWithNonLiteralName() throws Exception {
    String doc = getJavadoc("value5", "PARAM_VALUE_5");
    assertEquals("Documentation for value 5", doc);
  }

  private String getJavadoc(String aParameter, String aNameConstant) throws IOException {
    // Create the Java parser and parse the source code into an abstract syntax tree
    JavaSource source = Util.parseSource("src/test/resources/TestComponent.java", "UTF-8");
    
    return Util.getParameterDocumentation(source, aParameter, aNameConstant);
  }
}
