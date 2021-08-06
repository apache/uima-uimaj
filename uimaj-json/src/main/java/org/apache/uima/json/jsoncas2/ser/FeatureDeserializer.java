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

import static org.apache.uima.json.jsoncas2.JsonCas2Names.ARRAY_SUFFIX;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.ELEMENT_TYPE_FIELD;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.NAME_FIELD;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.RANGE_FIELD;

import java.io.IOException;
import java.util.Optional;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.FeatureDescription;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;

public class FeatureDeserializer extends CasDeserializer_ImplBase<FeatureDescription> {
  private static final long serialVersionUID = 1L;

  public FeatureDeserializer() {
    super(FeatureDescription.class);
  }

  @Override
  public FeatureDescription deserialize(JsonParser aParser, DeserializationContext aCtxt)
          throws IOException, JsonProcessingException {
    JsonNode node = aParser.readValueAsTree();

    if (!node.isObject()) {
      throw new JsonParseException(aParser, "Feature declaration must be a JSON object");
    }

    String featureName = node.get(NAME_FIELD).asText();
    String featureRangeType = node.get(RANGE_FIELD).asText();
    Optional<String> componentType;
    if (featureRangeType.endsWith(ARRAY_SUFFIX)) {
      componentType = Optional
              .of(featureRangeType.substring(0, featureRangeType.length() - ARRAY_SUFFIX.length()));
      featureRangeType = CAS.TYPE_NAME_FS_ARRAY;
    } else {
      componentType = Optional.ofNullable(node.get(ELEMENT_TYPE_FIELD)).map(JsonNode::asText);
    }

    FeatureDescription fd = UIMAFramework.getResourceSpecifierFactory().createFeatureDescription();
    fd.setName(featureName);
    fd.setRangeTypeName(featureRangeType);
    fd.setElementType(componentType.orElse(null));
    // fd.setMultipleReferencesAllowed();
    // fd.setDescription("");
    // td.setSourceUrl(aParser.getTokenLocation().sourceDescription());
    return fd;
  }
}
