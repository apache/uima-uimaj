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

import static org.apache.uima.cas.CAS.TYPE_NAME_FS_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_TOP;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.ARRAY_SUFFIX;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.ELEMENT_TYPE_FIELD;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.NAME_FIELD;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.RANGE_FIELD;
import static org.apache.uima.json.jsoncas2.mode.ArrayTypeMode.AS_ARRAY_TYPED_RANGE;

import java.io.IOException;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.json.jsoncas2.mode.ArrayTypeMode;
import org.apache.uima.json.jsoncas2.ref.ReferenceCache;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class FeatureSerializer extends StdSerializer<Feature> {
  private static final long serialVersionUID = 6706550270922386356L;

  private ArrayTypeMode arrayMode = AS_ARRAY_TYPED_RANGE;

  public FeatureSerializer() {
    super(Feature.class);
  }

  public void setArrayMode(ArrayTypeMode aArrayMode) {
    arrayMode = aArrayMode;
  }

  public ArrayTypeMode getArrayMode() {
    return arrayMode;
  }

  @Override
  public void serialize(Feature aFeature, JsonGenerator jg, SerializerProvider aProvider)
          throws IOException {
    ReferenceCache refCache = ReferenceCache.get(aProvider);

    jg.writeStartObject(aFeature);

    jg.writeStringField(NAME_FIELD, aFeature.getShortName());

    // special check for array range types, which are represented in the CAS as
    // elementType[] but in the descriptor as an FSArray with an <elementType>
    Type rangeType = aFeature.getRange();
    if (rangeType.isArray() && !rangeType.getComponentType().isPrimitive()) {
      switch (arrayMode) {
        case AS_ARRAY_TYPED_RANGE:
          serializeArrayFieldAsRangeWithArrayMarker(refCache, jg, aFeature);
          break;
        case AS_RANGE_AND_ELEMENT:
          serializeArrayFieldAsTypeAndElementType(refCache, jg, aFeature);
          break;
      }
    } else {
      serializeField(refCache, jg, aFeature);
    }

    jg.writeEndObject();
  }

  private void serializeField(ReferenceCache refCache, JsonGenerator jg, Feature aFeature)
          throws IOException {
    jg.writeStringField(RANGE_FIELD, refCache.typeRef(aFeature.getRange()));
    Type componentType = aFeature.getRange().getComponentType();
    if (componentType != null) {
      jg.writeStringField(ELEMENT_TYPE_FIELD, refCache.typeRef(componentType));
    }
  }

  /**
   * @see ArrayTypeMode#AS_ARRAY_TYPED_RANGE
   */
  private void serializeArrayFieldAsRangeWithArrayMarker(ReferenceCache refCache, JsonGenerator jg,
          Feature aFeature) throws IOException {
    jg.writeStringField(RANGE_FIELD,
            refCache.typeRef(aFeature.getRange().getComponentType()) + ARRAY_SUFFIX);
  }

  /**
   * @see ArrayTypeMode#AS_RANGE_AND_ELEMENT
   */
  private void serializeArrayFieldAsTypeAndElementType(ReferenceCache refCache, JsonGenerator jg,
          Feature aFeature) throws IOException {
    Type rangeType = aFeature.getRange();
    jg.writeStringField(RANGE_FIELD, TYPE_NAME_FS_ARRAY);

    // Component type can be omitted if it is the default (TOP)
    if (!TYPE_NAME_TOP.equals(rangeType.getComponentType().getName())) {
      jg.writeStringField(ELEMENT_TYPE_FIELD, refCache.typeRef(rangeType.getComponentType()));
    }
  }
}
