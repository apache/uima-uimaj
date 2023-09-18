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
package org.apache.uima.collection.impl.cpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;

import org.apache.uima.collection.base_cpm.SynchPoint;
import org.junit.jupiter.api.Test;

class CheckpointTest {

  @Test
  void testDeserializingCheckpointWithoutData() throws Exception {
    assertThatNoException().isThrownBy(() -> {
      var original = new CheckpointData();
      try (var bis = new ByteArrayInputStream(toBytes(original))) {
        var clone = Checkpoint.deserializeCheckpoint(bis);
        assertThat(clone).usingRecursiveComparison().isEqualTo(original);
      }
    });
  }

  void testDeserializingCheckpointWithIllegalData() throws Exception {
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      var original = new CheckpointData();
      var originalSynchPoint = new CustomSynchPoint();
      originalSynchPoint.set("foo");
      original.setSynchPoint(originalSynchPoint);
      try (var bis = new ByteArrayInputStream(toBytes(original))) {
        var clone = Checkpoint.deserializeCheckpoint(bis);
        assertThat(clone).usingRecursiveComparison().isEqualTo(original);
      }
    }).withMessageContaining("filter").withMessageContaining("REJECTED");

  }

  void testDeserializingCheckpointWithCustomSerialFilter() throws Exception {
    System.setProperty(Checkpoint.PROP_CPE_CHECKPOINT_SERIAL_FILTER,
            CustomSynchPoint.class.getName());
    try {
      assertThatNoException().isThrownBy(() -> {
        var original = new CheckpointData();
        var originalSynchPoint = new CustomSynchPoint();
        originalSynchPoint.set("foo");
        original.setSynchPoint(originalSynchPoint);
        try (var bis = new ByteArrayInputStream(toBytes(original))) {
          var clone = Checkpoint.deserializeCheckpoint(bis);
          assertThat(clone).usingRecursiveComparison().isEqualTo(original);
        }
      });
    } finally {
      System.getProperties().remove(Checkpoint.PROP_CPE_CHECKPOINT_SERIAL_FILTER);
    }
  }

  private byte[] toBytes(CheckpointData aOriginal) throws IOException {
    try (var bos = new ByteArrayOutputStream(); var oos = new ObjectOutputStream(bos)) {
      oos.writeObject(aOriginal);
      oos.flush();
      return bos.toByteArray();
    }
  }

  public static final class CustomSynchPoint implements SynchPoint {

    private static final long serialVersionUID = -6794583862575232021L;
    private Object data;

    @Override
    public void set(Object aSynchPointData) throws InvalidClassException {
      data = aSynchPointData;
    }

    @Override
    public Object get() {
      return data;
    }

    @Override
    public String serializeToXML() {
      throw new RuntimeException("Not implemented");
    }

    @Override
    public void deserialize(InputStream aInputStream) throws Exception {
      throw new RuntimeException("Not implemented");
    }
  }
}
