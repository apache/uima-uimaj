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
package org.apache.uima.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.CustomResourceSpecifier;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomResourceSpecifierFactory_implTest {

  private CustomResourceFactory_impl crFactory;

  @BeforeEach
  void setUp() throws Exception {
    crFactory = new CustomResourceFactory_impl();
  }

  @Test
  void testProduceResource() throws Exception {
    CustomResourceSpecifier specifier = UIMAFramework.getResourceSpecifierFactory()
            .createCustomResourceSpecifier();
    specifier.setResourceClassName("org.apache.uima.impl.SomeCustomResource");
    Parameter[] parameters = new Parameter[2];
    parameters[0] = UIMAFramework.getResourceSpecifierFactory().createParameter();
    parameters[0].setName("param1");
    parameters[0].setValue("val1");
    parameters[1] = UIMAFramework.getResourceSpecifierFactory().createParameter();
    parameters[1].setName("param2");
    parameters[1].setValue("val2");
    specifier.setParameters(parameters);

    Resource res = crFactory.produceResource(Resource.class, specifier, Collections.emptyMap());
    assertThat(res instanceof SomeCustomResource).isTrue();
    assertThat(((SomeCustomResource) res).paramMap.get("param1")).isEqualTo("val1");
    assertThat(((SomeCustomResource) res).paramMap.get("param2")).isEqualTo("val2");

    // also UIMAFramework.produceResource should do the same thing
    res = UIMAFramework.produceResource(specifier, Collections.emptyMap());
    assertThat(res instanceof SomeCustomResource).isTrue();
    assertThat(((SomeCustomResource) res).paramMap.get("param1")).isEqualTo("val1");
    assertThat(((SomeCustomResource) res).paramMap.get("param2")).isEqualTo("val2");
  }
}
