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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.apache.uima.resource.metadata.ConfigurationParameter.TYPE_BOOLEAN;
import static org.apache.uima.resource.metadata.ConfigurationParameter.TYPE_DOUBLE;
import static org.apache.uima.resource.metadata.ConfigurationParameter.TYPE_FLOAT;
import static org.apache.uima.resource.metadata.ConfigurationParameter.TYPE_INTEGER;
import static org.apache.uima.resource.metadata.ConfigurationParameter.TYPE_LONG;
import static org.apache.uima.resource.metadata.ConfigurationParameter.TYPE_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.InstanceOfAssertFactories.DOUBLE;
import static org.assertj.core.api.InstanceOfAssertFactories.array;
import static org.assertj.core.util.Arrays.array;

import java.io.File;
import java.net.URL;
import java.util.Locale;
import java.util.function.BiFunction;

import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.junit.jupiter.api.Test;

public class ConfigurationParameterFactory_ParameterValueConversionTest {
  @Test
  public void thatValueConversionWorks() throws Exception {
    BiFunction<ConfigurationParameter, Object, Object> conv = ConfigurationParameterFactory::convertParameterValue;

    assertThat(conv.apply(param(TYPE_STRING, false), null)).isEqualTo(null);
    assertThat(conv.apply(param(TYPE_STRING, false), "a")).isEqualTo("a");
    assertThat(conv.apply(param(TYPE_STRING, false), 1)).isEqualTo("1");
    assertThat(conv.apply(param(TYPE_STRING, false), 1.1f)).isEqualTo("1.1");
    assertThat(conv.apply(param(TYPE_STRING, false), 1.1d)).isEqualTo("1.1");
    assertThat(conv.apply(param(TYPE_STRING, false), Double.NEGATIVE_INFINITY))
            .isEqualTo("-Infinity");
    assertThat(conv.apply(param(TYPE_STRING, false), Double.NaN)).isEqualTo("NaN");
    assertThat(conv.apply(param(TYPE_STRING, false), new File("test"))).isEqualTo("test");
    assertThat(conv.apply(param(TYPE_STRING, false), Object.class)).isEqualTo("java.lang.Object");
    assertThat(conv.apply(param(TYPE_STRING, false), Locale.GERMANY)).isEqualTo("de_DE");
    assertThat(conv.apply(param(TYPE_STRING, false), UTF_8)).isEqualTo("UTF-8");
    assertThat(conv.apply(param(TYPE_STRING, false), new URL("http://dummy.net")))
            .isEqualTo("http://dummy.net");
    assertThat(conv.apply(param(TYPE_STRING, true), array(1.1d, "1.2")))
            .asInstanceOf(array(String[].class)) //
            .containsExactly("1.1", "1.2");
    assertThat(conv.apply(param(TYPE_STRING, true), asList(1.1d, "1.2")))
            .asInstanceOf(array(String[].class)) //
            .containsExactly("1.1", "1.2");

    assertThat(conv.apply(param(TYPE_FLOAT, false), null)).isEqualTo(null);
    assertThat(conv.apply(param(TYPE_FLOAT, false), "1.1")).isEqualTo(1.1f);
    assertThat(conv.apply(param(TYPE_FLOAT, false), "1.1f")).isEqualTo(1.1f);
    assertThat(conv.apply(param(TYPE_FLOAT, false), "1.1d")).isEqualTo(1.1f);
    assertThat(conv.apply(param(TYPE_FLOAT, false), "-Infinity"))
            .isEqualTo(Float.NEGATIVE_INFINITY);
    assertThat(conv.apply(param(TYPE_FLOAT, false), "NaN")).isEqualTo(Float.NaN);
    assertThat(conv.apply(param(TYPE_FLOAT, false), 1.1f)).isEqualTo(1.1f);
    assertThat(conv.apply(param(TYPE_FLOAT, false), 1.1d)).isEqualTo(1.1f);
    assertThat(conv.apply(param(TYPE_FLOAT, true), array(1.1d, "1.2")))
            .asInstanceOf(array(Float[].class)) //
            .containsExactly(1.1f, 1.2f);
    assertThat(conv.apply(param(TYPE_FLOAT, true), asList(1.1d, "1.2")))
            .asInstanceOf(array(Float[].class)) //
            .containsExactly(1.1f, 1.2f);

    assertThat(conv.apply(param(TYPE_DOUBLE, false), null)).isEqualTo(null);
    assertThat(conv.apply(param(TYPE_DOUBLE, false), "1.1")).isEqualTo(1.1d);
    assertThat(conv.apply(param(TYPE_DOUBLE, false), "1.1f")).isEqualTo(1.1d);
    assertThat(conv.apply(param(TYPE_DOUBLE, false), "1.1d")).isEqualTo(1.1d);
    assertThat(conv.apply(param(TYPE_DOUBLE, false), "-Infinity"))
            .isEqualTo(Double.NEGATIVE_INFINITY);
    assertThat(conv.apply(param(TYPE_DOUBLE, false), "NaN")).isEqualTo(Double.NaN);
    assertThat(conv.apply(param(TYPE_DOUBLE, false), 1.1f)) //
            .asInstanceOf(DOUBLE) //
            .isEqualTo(1.1d, offset(1e-4d));
    assertThat(conv.apply(param(TYPE_DOUBLE, false), 1.1d)).isEqualTo(1.1d);
    assertThat(conv.apply(param(TYPE_DOUBLE, true), array(1.1d, "1.2")))
            .asInstanceOf(array(Double[].class)) //
            .containsExactly(1.1d, 1.2d);
    assertThat(conv.apply(param(TYPE_DOUBLE, true), asList(1.1d, "1.2")))
            .asInstanceOf(array(Double[].class)) //
            .containsExactly(1.1d, 1.2d);

    assertThat(conv.apply(param(TYPE_INTEGER, false), null)).isEqualTo(null);
    assertThat(conv.apply(param(TYPE_INTEGER, false), 1)).isEqualTo(1);
    assertThat(conv.apply(param(TYPE_INTEGER, false), "1")).isEqualTo(1);
    assertThat(conv.apply(param(TYPE_INTEGER, false), 1.0)).isEqualTo(1);
    assertThat(conv.apply(param(TYPE_INTEGER, true), array(1, "2")))
            .asInstanceOf(array(Integer[].class)) //
            .containsExactly(1, 2);
    assertThat(conv.apply(param(TYPE_INTEGER, true), asList(1, "2")))
            .asInstanceOf(array(Integer[].class)) //
            .containsExactly(1, 2);

    assertThat(conv.apply(param(TYPE_LONG, false), null)).isEqualTo(null);
    assertThat(conv.apply(param(TYPE_LONG, false), 1)).isEqualTo(1l);
    assertThat(conv.apply(param(TYPE_LONG, false), "1")).isEqualTo(1l);
    assertThat(conv.apply(param(TYPE_LONG, false), 1.0)).isEqualTo(1l);
    assertThat(conv.apply(param(TYPE_LONG, true), array(1, "2"))).asInstanceOf(array(Long[].class)) //
            .containsExactly(1l, 2l);
    assertThat(conv.apply(param(TYPE_LONG, true), asList(1, "2"))).asInstanceOf(array(Long[].class)) //
            .containsExactly(1l, 2l);

    assertThat(conv.apply(param(TYPE_BOOLEAN, false), null)).isEqualTo(null);
    assertThat(conv.apply(param(TYPE_BOOLEAN, false), true)).isEqualTo(true);
    assertThat(conv.apply(param(TYPE_BOOLEAN, false), "true")).isEqualTo(true);
    assertThat(conv.apply(param(TYPE_BOOLEAN, false), "false")).isEqualTo(false);
    assertThat(conv.apply(param(TYPE_BOOLEAN, false), "True")).isEqualTo(true);
    assertThat(conv.apply(param(TYPE_BOOLEAN, false), "False")).isEqualTo(false);
    assertThat(conv.apply(param(TYPE_BOOLEAN, false), "TRUE")).isEqualTo(true);
    assertThat(conv.apply(param(TYPE_BOOLEAN, false), "FALSE")).isEqualTo(false);
    assertThat(conv.apply(param(TYPE_BOOLEAN, true), array(true, "false")))
            .asInstanceOf(array(Boolean[].class)) //
            .containsExactly(true, false);
    assertThat(conv.apply(param(TYPE_BOOLEAN, true), asList(true, "false")))
            .asInstanceOf(array(Boolean[].class)) //
            .containsExactly(true, false);
  }

  private ConfigurationParameter param(String aType, boolean aMultiValued) {
    ConfigurationParameter p = getResourceSpecifierFactory().createConfigurationParameter();
    p.setName("p");
    p.setMultiValued(aMultiValued);
    p.setType(aType);
    return p;
  }
}
