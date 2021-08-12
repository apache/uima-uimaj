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

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.json.jsoncas2.model.FeatureStructures;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;

public class FeatureStructuresAsObjectDeserializer
        extends CasDeserializer_ImplBase<FeatureStructures> {
  private static final long serialVersionUID = -5937326876753347248L;

  public FeatureStructuresAsObjectDeserializer() {
    super(FeatureStructures.class);
  }

  @Override
  public FeatureStructures deserialize(JsonParser aParser, DeserializationContext aCtxt)
          throws IOException, JsonProcessingException {

    // Consume object begin
    aParser.nextValue();

    List<FeatureStructure> featureStructures = new ArrayList<>();
    while (aParser.currentToken() != JsonToken.END_OBJECT) {
      featureStructures.add(aCtxt.readValue(aParser, FeatureStructure.class));
      aParser.nextValue();
    }

    runPostprocessors(aCtxt);

    // Consume object end
    aParser.nextValue();

    return new FeatureStructures(featureStructures);
  }
}
