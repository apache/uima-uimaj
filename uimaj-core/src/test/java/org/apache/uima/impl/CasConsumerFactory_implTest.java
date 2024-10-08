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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Collections;

import org.apache.uima.collection.CasConsumer;
import org.apache.uima.collection.impl.CasConsumerDescription_impl;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CasConsumerFactory_implTest {

  private CasConsumerFactory_impl ccFactory;

  @BeforeEach
  void setUp() throws Exception {
    ccFactory = new CasConsumerFactory_impl();
  }

  @Test
  void testInvalidFrameworkImplementation() {
    var desc = new CasConsumerDescription_impl();
    desc.setFrameworkImplementation("foo");

    assertThatExceptionOfType(ResourceInitializationException.class).isThrownBy(
            () -> ccFactory.produceResource(CasConsumer.class, desc, Collections.emptyMap()))
            .satisfies(e -> {
              assertThat(e.getMessage()).isNotNull();
              assertThat(e.getMessage()).doesNotStartWith("EXCEPTION MESSAGE LOCALIZATION FAILED");
              assertThat(e.getMessageKey()).isEqualTo(
                      ResourceInitializationException.UNSUPPORTED_FRAMEWORK_IMPLEMENTATION);
            });
  }
}
