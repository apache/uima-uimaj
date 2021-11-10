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
package org.apache.uima.cas.impl;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.apache.uima.cas.SerialFormat.BINARY_TSI;
import static org.apache.uima.cas.SerialFormat.COMPRESSED_FILTERED_TSI;
import static org.apache.uima.cas.SerialFormat.COMPRESSED_TSI;
import static org.apache.uima.cas.SerialFormat.SERIALIZED_TSI;
import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.apache.uima.util.CasIOUtils.load;
import static org.apache.uima.util.CasIOUtils.save;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.Test;

public class ConcurrentBinarySerializationTest {
  /**
   * Serialization of the CAS is not inherently thread-safe. This test tries to run multiple
   * serializations of the CAS in parallel to trigger a situation where an invalid serialization is
   * generated - to be found by deserializing again. To fix this, an internal synchronization in the
   * CAS was added.
   * 
   * @see <a href="https://issues.apache.org/jira/browse/UIMA-6162">UIMA 6162</a>
   */
  @Test
  public void thatConcurrentSerializationWorks() throws Exception {
    final SerialFormat[] formats = { BINARY_TSI, SERIALIZED_TSI, COMPRESSED_FILTERED_TSI,
        COMPRESSED_TSI };
    final int typeCount = 10;

    // Set up a couple of custom types
    TypeSystemDescription tsd = UIMAFramework.getResourceSpecifierFactory()
            .createTypeSystemDescription();
    for (int n = 0; n < typeCount; n++) {
      tsd.addType("Type" + n, "", CAS.TYPE_NAME_ANNOTATION);
    }

    CAS cas = createCas(tsd, null, null);
    cas.setDocumentText("This is a test.");

    Random rnd = new Random();

    // Set up a couple of random annotations
    for (int i = 0; i < 1000; i++) {
      String type = "Type" + rnd.nextInt(typeCount);
      int a = rnd.nextInt(cas.getDocumentText().length());
      int b = rnd.nextInt(cas.getDocumentText().length());
      AnnotationFS ann = cas.createAnnotation(cas.getTypeSystem().getType(type), min(a, b),
              max(a, b));
      cas.addFsToIndexes(ann);
    }

    // Schedulable task which serializes a CAS and then deserializes it again to test that it
    // was serialized correctly. We randomly alternate between different binary serialization
    // formats which all include type system information.
    Callable<Boolean> casSerDeser = () -> {
      try {
        SerialFormat fmt = formats[rnd.nextInt(formats.length)];

        // System.out.printf("Serializing as %s...%n", fmt);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        save(cas, bos, fmt);

        // System.out.printf("Deserializing...%n");
        CAS outCas = createCas((TypeSystemDescription) null, null, null);
        load(new ByteArrayInputStream(bos.toByteArray()), outCas);
      } catch (Exception e) {
        // System.out.printf("Failure: %s%n", e.getMessage());
        return false;
      }
      return true;
    };

    // Schedule concurrent serializations
    List<Future<Boolean>> results = new ArrayList<>();
    ExecutorService executor = Executors.newFixedThreadPool(4);
    for (int n = 0; n < 100; n++) {
      results.add(executor.submit(casSerDeser));
    }

    // All futures must complete without returning an exception
    assertTrue(results.stream().allMatch(r -> {
      try {
        return r.get() == true;
      } catch (Exception e) {
        return false;
      }
    }));
  }
}
