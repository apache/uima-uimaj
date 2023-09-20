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
package org.apache.uima.internal.util;

import static org.apache.uima.internal.util.SerializationUtils.deserialize;
import static org.apache.uima.internal.util.SerializationUtils.deserializeCASCompleteSerializer;
import static org.apache.uima.internal.util.SerializationUtils.deserializeCASMgrSerializer;
import static org.apache.uima.internal.util.SerializationUtils.deserializeCASSerializer;
import static org.apache.uima.internal.util.SerializationUtils.deserializeCASSerializerOrCASCompleteSerializer;
import static org.apache.uima.internal.util.SerializationUtils.serialize;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;

import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.impl.CASSerializer;
import org.junit.jupiter.api.Test;

class SerializationUtilsTest {

  @Test
  void testDeserializingExpectedClass() throws Exception {
    assertThatNoException().isThrownBy(() -> {
      deserialize(serialize(new CASCompleteSerializer()));
      deserialize(serialize(new CASSerializer()));
      deserialize(serialize(new CASMgrSerializer()));
    });

    assertThatNoException().isThrownBy(() -> {
      deserializeCASCompleteSerializer(serialize(new CASCompleteSerializer()));
    });

    assertThatNoException().isThrownBy(() -> {
      deserializeCASMgrSerializer(serialize(new CASMgrSerializer()));
    });

    assertThatNoException().isThrownBy(() -> {
      try (var bis = new ByteArrayInputStream(serialize(new CASMgrSerializer()))) {
        deserializeCASMgrSerializer(bis);
      }
    });

    assertThatNoException().isThrownBy(() -> {
      deserializeCASSerializer(serialize(new CASSerializer()));
    });

    assertThatNoException().isThrownBy(() -> {
      try (var bis = new ByteArrayInputStream(serialize(new CASCompleteSerializer()))) {
        deserializeCASSerializerOrCASCompleteSerializer(bis);
      }
      try (var bis = new ByteArrayInputStream(serialize(new CASSerializer()))) {
        deserializeCASSerializerOrCASCompleteSerializer(bis);
      }
    });
  }

  @Test
  void testDeserializingUnexpectedClass() throws Exception {
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      deserialize(serialize("Hello world"));
    }).withMessageContaining("Unexpected object type");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      deserializeCASCompleteSerializer(serialize("Hello world"));
    }).withMessageContaining("Unexpected object type");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      deserializeCASCompleteSerializer(serialize(new CASMgrSerializer()));
    }).withMessageContaining("Unexpected object type");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      deserializeCASCompleteSerializer(serialize(new CASSerializer()));
    }).withMessageContaining("Unexpected object type");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      deserializeCASMgrSerializer(serialize("Hello world"));
    }).withMessageContaining("Unexpected object type");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      try (var bis = new ByteArrayInputStream(serialize("Hello world"))) {
        deserializeCASMgrSerializer(bis);
      }
    }).withMessageContaining("Unexpected object type");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      deserializeCASSerializer(serialize("Hello world"));
    }).withMessageContaining("Unexpected object type");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      try (var bis = new ByteArrayInputStream(serialize("Hello world"))) {
        deserializeCASSerializerOrCASCompleteSerializer(bis);
      }
    }).withMessageContaining("Unexpected object type");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      try (var bis = new ByteArrayInputStream(serialize(new CASMgrSerializer()))) {
        deserializeCASSerializerOrCASCompleteSerializer(bis);
      }
    }).withMessageContaining("Unexpected object type");
  }

  @Test
  void testDeserializingIllegalClass() throws Exception {
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      deserialize(serialize(new IllegalClass()));
    }).withMessageContaining("filter status: REJECTED");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      deserializeCASCompleteSerializer(serialize(new IllegalClass()));
    }).withMessageContaining("filter status: REJECTED");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      deserializeCASMgrSerializer(serialize(new IllegalClass()));
    }).withMessageContaining("filter status: REJECTED");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      deserializeCASMgrSerializer(serialize(new CASSerializer()));
    }).withMessageContaining("filter status: REJECTED");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      deserializeCASMgrSerializer(serialize(new CASCompleteSerializer()));
    }).withMessageContaining("filter status: REJECTED");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      try (var bis = new ByteArrayInputStream(serialize(new IllegalClass()))) {
        deserializeCASMgrSerializer(bis);
      }
    }).withMessageContaining("filter status: REJECTED");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      try (var bis = new ByteArrayInputStream(serialize(new CASSerializer()))) {
        deserializeCASMgrSerializer(bis);
      }
    }).withMessageContaining("filter status: REJECTED");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      try (var bis = new ByteArrayInputStream(serialize(new CASCompleteSerializer()))) {
        deserializeCASMgrSerializer(bis);
      }
    }).withMessageContaining("filter status: REJECTED");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      deserializeCASSerializer(serialize(new IllegalClass()));
    }).withMessageContaining("filter status: REJECTED");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      deserializeCASSerializer(serialize(new CASMgrSerializer()));
    }).withMessageContaining("filter status: REJECTED");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      deserializeCASSerializer(serialize(new CASCompleteSerializer()));
    }).withMessageContaining("filter status: REJECTED");

    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      try (var bis = new ByteArrayInputStream(serialize(new IllegalClass()))) {
        deserializeCASSerializerOrCASCompleteSerializer(bis);
      }
    }).withMessageContaining("filter status: REJECTED");
  }

  private static final class IllegalClass implements Serializable {

    private static final long serialVersionUID = 3043542127122574250L;

    // Nothing inside
  }
}
