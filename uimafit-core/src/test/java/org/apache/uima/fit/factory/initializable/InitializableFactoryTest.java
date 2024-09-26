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
package org.apache.uima.fit.factory.initializable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.initialize.ConfigurationParameterInitializer;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.UimaContextFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.Test;

public class InitializableFactoryTest {

  @Test
  public void testInitializableFactory() throws Exception {
    UimaContext context = UimaContextFactory
            .createUimaContext(InitializableClass.PARAM_BOOLEAN_PARAMETER, true);
    InitializableClass ic = InitializableFactory.create(context, InitializableClass.class);
    assertThat(ic.booleanParameter).isTrue();

    NotInitializableClass nic = InitializableFactory.create(context, NotInitializableClass.class);
    assertThat(nic.booleanParameter).isFalse();
  }

  @Test
  public void testBadConstructor() throws ResourceInitializationException {
    UimaContext context = UimaContextFactory
            .createUimaContext(InitializableClass.PARAM_BOOLEAN_PARAMETER, true);

    assertThatExceptionOfType(ResourceInitializationException.class)
            .isThrownBy(() -> InitializableFactory.create(context, NoDefaultConstructor.class));
  }

  public static class InitializableClass implements Initializable {

    public static final String PARAM_BOOLEAN_PARAMETER = "booleanParameter";

    @ConfigurationParameter
    public boolean booleanParameter = false;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
      ConfigurationParameterInitializer.initialize(this, context);
    }
  }

  public static class NotInitializableClass {

    public static final String PARAM_BOOLEAN_PARAMETER = "booleanParameter";

    @ConfigurationParameter
    public boolean booleanParameter = false;

    public void initialize(UimaContext context) throws ResourceInitializationException {
      ConfigurationParameterInitializer.initialize(this, context);
    }
  }

  public static class InitializableFileNamer implements Initializable {

    public static final String PARAM_STRING_PARAMETER = "stringParameter";

    @ConfigurationParameter
    public String stringParameter = "hello";

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
      ConfigurationParameterInitializer.initialize(this, context);
    }

    public String nameFile(JCas jCas) {
      return "some_name_for_this_jcas.xmi";
    }
  }

  public static class NotInitializableFileNamer {

    public static final String PARAM_STRING_PARAMETER = "stringParameter";

    @ConfigurationParameter
    public String stringParameter = "hello";

    public void initialize(UimaContext context) throws ResourceInitializationException {
      ConfigurationParameterInitializer.initialize(this, context);
    }

    public String nameFile(JCas jCas) {
      return "some_name_for_this_jcas.xmi";
    }
  }

  public static class NoDefaultConstructor {
    public NoDefaultConstructor(String s) {
      // do nothing
    }
  }
}
