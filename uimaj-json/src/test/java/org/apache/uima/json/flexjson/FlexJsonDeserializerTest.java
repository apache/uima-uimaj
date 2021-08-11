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
package org.apache.uima.json.flexjson;

import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.uima.cas.CAS;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

@Deprecated
public class FlexJsonDeserializerTest {
  private static CAS cas;

  private JsonFactory jsonFactory;

  @BeforeClass
  public static void setupOnce() throws Exception {
    cas = createCas();
  }

  @Before
  public void setup() throws Exception {
    jsonFactory = new JsonFactory();
    jsonFactory.setCodec(new ObjectMapper());
  }

  @Test
  public void thatQuotedStringCanBeParsed() throws Exception {
    FlexJsonCasDeserializer deser = new FlexJsonCasDeserializer(jsonFactory
            .createParser(new File("src/test/resources/FlexJsonDeserializer/text_only.json")));

    deser.read(cas);

    assertThat(cas.getDocumentText()).isEqualTo("Hello world.");
  }

  @Test
  public void thatFeatureStructureArrayCanBeParsed() throws Exception {
    FlexJsonCasDeserializer deser = new FlexJsonCasDeserializer(jsonFactory.createParser(
            new File("src/test/resources/FlexJsonDeserializer/feature_structures_only.json")));

    deser.read(cas);

    assertThat(cas.getDocumentText()).isEqualTo("Hello world.");
  }
}
