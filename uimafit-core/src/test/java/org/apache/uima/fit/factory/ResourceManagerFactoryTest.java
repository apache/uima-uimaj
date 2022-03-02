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

import static org.apache.uima.fit.factory.ExternalResourceFactory.createResource;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.junit.jupiter.api.Test;

public class ResourceManagerFactoryTest {
  public static class SimpleResource extends Resource_ImplBase {
    // Nothing to do
  }

  @Test
  public void thatResourceCanBeCreated() throws Exception {
    SimpleResource sut = createResource(SimpleResource.class);

    assertThat(sut).isInstanceOf(SimpleResource.class);
  }

  public static class ResourceWithParameters extends Resource_ImplBase {
    public @ConfigurationParameter int intValue;
    public @ConfigurationParameter String stringValue;
  }

  @Test
  public void thatResourceCanBeParametrized() throws Exception {
    ResourceWithParameters sut = createResource(ResourceWithParameters.class, "intValue", "1",
            "stringValue", "test");

    assertThat(sut).isInstanceOf(ResourceWithParameters.class);
    assertThat(sut.intValue).isEqualTo(1);
    assertThat(sut.stringValue).isEqualTo("test");
  }
}
