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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.uima.UIMAFramework;
import org.apache.uima.json.jsoncas2.JsonCas2Names;
import org.apache.uima.resource.metadata.TypeDescription;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;

public class TypeDeserializer extends CasDeserializer_ImplBase<TypeDescription> {
  private static final long serialVersionUID = -3406515095847310810L;

  public TypeDeserializer() {
    super(TypeDescription.class);
  }

  @Override
  public TypeDescription deserialize(JsonParser aParser, DeserializationContext aCtxt)
          throws IOException, JsonProcessingException {
    JsonNode node = aParser.readValueAsTree();

    if (!node.isObject()) {
      throw new JsonParseException(aParser, "Type system declaration must be a JSON object");
    }

    String typeName = node.get(JsonCas2Names.NAME_FIELD).asText();
    String parentTypeName = node.get(JsonCas2Names.SUPER_TYPE_FIELD).asText();
    Optional<String> componentType = Optional.ofNullable(node.get(JsonCas2Names.ELEMENT_TYPE_FIELD))
            .map(JsonNode::asText);

    List<String> featureNames = new ArrayList<>();
    node.fieldNames().forEachRemaining(name -> {
      if (!name.startsWith(JsonCas2Names.RESERVED_FIELD_PREFIX)) {
        featureNames.add(name);
      }
    });

    TypeDescription td = UIMAFramework.getResourceSpecifierFactory().createTypeDescription();
    td.setName(typeName);
    td.setSupertypeName(parentTypeName);
    // td.setDescription("");
    // td.setAllowedValues(null);
    // td.setFeatures(null);
    // td.setSourceUrl(aParser.getTokenLocation().sourceDescription());
    return td;
  }
}
