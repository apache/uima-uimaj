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
package org.apache.uima.json.jsoncas2.ser;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.json.jsoncas2.ref.ReferenceCache;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class TypeDeSerializerTest {
  private Logger log = LoggerFactory.getLogger(getClass());

  @Test
  public void thatTypeDeSerializationWorks() throws Exception {
    ObjectMapper mapper = getMapper();

    TypeSystemDescription tsd = UIMAFramework.getResourceSpecifierFactory()
            .createTypeSystemDescription();
    TypeDescription tdExpected = tsd.addType("Type1", null, CAS.TYPE_NAME_ANNOTATION);

    TypeSystem ts = CasCreationUtils.createCas(tsd, null, null, null).getTypeSystem();

    String json = mapper.writer() //
            .withAttribute(ReferenceCache.KEY, ReferenceCache.builder().build()) //
            .writeValueAsString(ts.getType("Type1"));

    TypeDescription tdActual = mapper.reader() //
            .forType(TypeDescription.class) //
            .readValue(json);

    assertThat(tdActual).isEqualTo(tdExpected);
  }

  private ObjectMapper getMapper() {
    SimpleModule module = new SimpleModule("UIMA CAS JSON", new Version(1, 0, 0, null, null, null));

    module.addSerializer(Type.class, new TypeSerializer());
    module.addDeserializer(TypeDescription.class, new TypeDeserializer());
    module.addSerializer(Feature.class, new FeatureSerializer());
    module.addDeserializer(FeatureDescription.class, new FeatureDeserializer());

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(module);
    return mapper;
  }
}
