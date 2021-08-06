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

import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_FS_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_FS_LIST;
import static org.apache.uima.cas.CAS.TYPE_NAME_INTEGER;
import static org.apache.uima.cas.CAS.TYPE_NAME_INTEGER_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.json.jsoncas2.ref.ReferenceCache;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.TypeSystemUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class FeatureDeSerializerTest {
  private Logger log = LoggerFactory.getLogger(getClass());

  private static final String TYPE = "Type";
  private static final String FEATURE = "feature";

  private TypeSystemDescription tsd;
  private TypeDescription td;

  @BeforeEach
  public void setup() {
    tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
    td = tsd.addType(TYPE, null, CAS.TYPE_NAME_ANNOTATION);
  }

  @Test
  public void testIntegerFeature() throws Exception {
    FeatureDescription expected = td.addFeature(FEATURE, null, TYPE_NAME_INTEGER);
    FeatureDescription actual = serdes(tsd);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testStringFeature() throws Exception {
    FeatureDescription expected = td.addFeature(FEATURE, null, TYPE_NAME_STRING);
    FeatureDescription actual = serdes(tsd);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testFSArrayFeature() throws Exception {
    FeatureDescription expected = td.addFeature(FEATURE, null, TYPE_NAME_FS_ARRAY,
            TYPE_NAME_ANNOTATION, null);
    FeatureDescription actual = serdes(tsd);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testIntegerArrayFeature() throws Exception {
    FeatureDescription expected = td.addFeature(FEATURE, null, TYPE_NAME_INTEGER_ARRAY,
            TYPE_NAME_INTEGER, null);
    FeatureDescription actual = serdes(tsd);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testFSListFeature() throws Exception {
    FeatureDescription expected = td.addFeature(FEATURE, null, TYPE_NAME_FS_LIST,
            TYPE_NAME_ANNOTATION, null);
    FeatureDescription actual = serdes(tsd);

    // HACK: UIMA does not preserve the element type for FSList features!!! Bug?
    expected.setElementType(null);

    assertThat(actual).isEqualTo(expected);
  }

  private FeatureDescription serdes(TypeSystemDescription aTsd) throws Exception {
    ObjectMapper mapper = getMapper();
    CAS cas = CasCreationUtils.createCas(aTsd, null, null, null);
    TypeSystem ts = cas.getTypeSystem();

    StringWriter buf = new StringWriter();
    TypeSystemUtil.type2TypeDescription(ts.getType(TYPE), ts).toXML(buf);
    log.info("{}", buf);

    String json = mapper.writer() //
            .withAttribute(ReferenceCache.KEY, ReferenceCache.builder().build()) //
            .writeValueAsString(ts.getType(TYPE).getFeatureByBaseName(FEATURE));

    log.info(json);

    FeatureDescription fdActual = mapper.reader() //
            .forType(FeatureDescription.class) //
            .readValue(json);

    return fdActual;
  }

  private ObjectMapper getMapper() {
    SimpleModule module = new SimpleModule("UIMA CAS JSON", new Version(1, 0, 0, null, null, null));

    module.addSerializer(Feature.class, new FeatureSerializer());
    module.addDeserializer(FeatureDescription.class, new FeatureDeserializer());

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(module);
    return mapper;
  }
}
