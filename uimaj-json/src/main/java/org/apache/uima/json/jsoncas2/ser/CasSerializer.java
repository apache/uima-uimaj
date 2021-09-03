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

import static org.apache.uima.json.jsoncas2.JsonCas2Names.FEATURE_STRUCTURES_FIELD;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.HEADER_FIELD;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.TYPES_FIELD;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.VIEWS_FIELD;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.json.jsoncas2.mode.OffsetConversionMode;
import org.apache.uima.json.jsoncas2.model.FeatureStructures;
import org.apache.uima.json.jsoncas2.model.Header;
import org.apache.uima.json.jsoncas2.model.Views;
import org.apache.uima.json.jsoncas2.ref.FeatureStructureToViewIndex;
import org.apache.uima.json.jsoncas2.ref.ReferenceCache;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class CasSerializer extends StdSerializer<CAS> {
  private static final long serialVersionUID = 6779774576723692343L;

  private final Supplier<ReferenceCache> refCacheSupplier;

  public CasSerializer() {
    this(ReferenceCache.builder()::build);
  }

  public CasSerializer(Supplier<ReferenceCache> aRefCacheSupplier) {
    super(CAS.class);
    refCacheSupplier = aRefCacheSupplier;
  }

  @Override
  public void serialize(CAS aCas, JsonGenerator aJg, SerializerProvider aProvider)
          throws IOException {
    ReferenceCache.set(aProvider, refCacheSupplier.get());

    initOffsetConversion(aCas, aProvider);

    aJg.writeStartObject(aCas);

    serializeHeader(aCas, aJg, aProvider);

    serializeTypes(aCas, aJg, aProvider);

    serializeFeatureStructures(aCas, aJg, aProvider);

    serializeViews(aCas, aJg, aProvider);

    aJg.writeEndObject();
  }

  private void serializeHeader(CAS aCas, JsonGenerator aJg, SerializerProvider aProvider)
          throws IOException {
    Header header = new Header(aProvider);
    if (header.requiresSerialization()) {
      aJg.writeFieldName(HEADER_FIELD);
      aProvider.defaultSerializeValue(header, aJg);
    }
  }

  private void serializeTypes(CAS aCas, JsonGenerator aJg, SerializerProvider aProvider)
          throws IOException {
    aJg.writeFieldName(TYPES_FIELD);
    aProvider.defaultSerializeValue(aCas.getTypeSystem(), aJg);
  }

  private void serializeFeatureStructures(CAS aCas, JsonGenerator aJg, SerializerProvider aProvider)
          throws IOException {
    FeatureStructures allFSes = findAllFeatureStructures(aCas);
    FeatureStructureToViewIndex.set(aProvider, new FeatureStructureToViewIndex(allFSes));
    if (!allFSes.isEmpty()) {
      aJg.writeFieldName(FEATURE_STRUCTURES_FIELD);
      aProvider.defaultSerializeValue(allFSes, aJg);
    }
  }

  private void serializeViews(CAS aCas, JsonGenerator aJg, SerializerProvider aProvider)
          throws IOException {
    Views views = new Views(aCas);
    if (!views.isEmpty()) {
      aJg.writeFieldName(VIEWS_FIELD);
      aProvider.defaultSerializeValue(views, aJg);
    }
  }

  private void initOffsetConversion(CAS aCas, SerializerProvider aProvider) {
    for (CAS view : new Views(aCas)) {
      OffsetConversionMode.initConverter(aProvider, view.getViewName(), view.getDocumentText());
    }
  }

  private FeatureStructures findAllFeatureStructures(CAS aCas) {
    Set<FeatureStructure> allFSes = new LinkedHashSet<>();
    ((CASImpl) aCas).walkReachablePlusFSsSorted(allFSes::add, null, null, null);
    return new FeatureStructures(allFSes);
  }
}
