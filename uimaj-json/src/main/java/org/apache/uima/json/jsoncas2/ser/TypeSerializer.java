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

import static java.util.stream.Collectors.toList;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.ELEMENT_TYPE_FIELD;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.SUPER_TYPE_FIELD;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.json.jsoncas2.JsonCas2Names;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class TypeSerializer extends StdSerializer<Type> {
  private static final long serialVersionUID = 8549058399080277660L;

  public TypeSerializer() {
    super(Type.class);
  }

  @Override
  public void serialize(Type aType, JsonGenerator aJg, SerializerProvider aProvider)
          throws IOException {
    aJg.writeStartObject(aType);

    aJg.writeStringField(JsonCas2Names.NAME_FIELD, aType.getName());

    Type parent = ((TypeImpl) aType).getSuperType();
    if (parent != null) {
      aJg.writeStringField(SUPER_TYPE_FIELD, parent.getName());
    }

    if (aType.getComponentType() != null) {
      aJg.writeStringField(ELEMENT_TYPE_FIELD, aType.getComponentType().getName());
    }

    List<Feature> newFeatures = aType.getFeatures().stream().filter(f -> f.getDomain() == aType)
            .collect(toList());
    if (!newFeatures.isEmpty()) {
      for (Feature feature : newFeatures) {
        aJg.writeFieldName(feature.getShortName());
        aProvider.defaultSerializeValue(feature, aJg);
      }
    }

    aJg.writeEndObject();
  }
}
