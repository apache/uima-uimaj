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
package org.apache.uima.fit.factory;

import static org.apache.uima.fit.factory.ResourceCreationSpecifierFactory.createResourceCreationSpecifier;
import static org.apache.uima.fit.factory.ResourceCreationSpecifierFactory.setConfigurationParameters;
import static org.apache.uima.fit.factory.UimaContextFactory.createUimaContext;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.awt.Point;

import org.apache.uima.fit.factory.testAes.ParameterizedAE;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.junit.jupiter.api.Test;

/**
 */

public class ResourceCreationSpecifierFactoryTest {

  @Test
  public void test4() throws Exception {
    assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> createResourceCreationSpecifier(
                    "src/main/resources/org/apache/uima/fit/component/NoOpAnnotator.xml",
                    new String[] { "test" }));
  }

  @Test
  public void test3() throws Exception {
    assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> createUimaContext("test"));
  }

  @Test
  public void test2() throws Exception {
    assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> createResourceCreationSpecifier(
                    "src/main/resources/org/apache/uima/fit/component/NoOpAnnotator.xml",
                    new Object[] { "test", new Point(0, 5) }));
  }

  @Test
  public void test1() {
    assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> setConfigurationParameters((ResourceCreationSpecifier) null,
                    ParameterizedAE.PARAM_BOOLEAN_1));
  }
}
