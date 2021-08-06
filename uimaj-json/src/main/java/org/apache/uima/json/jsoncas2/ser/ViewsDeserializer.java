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

import org.apache.uima.cas.CAS;
import org.apache.uima.json.jsoncas2.JsonCas2Names;
import org.apache.uima.json.jsoncas2.model.Views;
import org.apache.uima.json.jsoncas2.ref.FeatureStructureIdToViewIndex;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;

public class ViewsDeserializer extends CasDeserializer_ImplBase<Views> {
  private static final long serialVersionUID = -2976455559005753544L;

  public ViewsDeserializer() {
    super(Views.class);
  }

  @Override
  public Views deserialize(JsonParser aParser, DeserializationContext aCtxt)
          throws IOException, JsonProcessingException {
    if (aParser.currentToken() != JsonToken.START_OBJECT) {
      throw new JsonParseException(aParser, "Views declaration must be a JSON object");
    }

    CAS cas = getCas(aCtxt);

    aParser.nextValue();
    while (aParser.currentToken() != JsonToken.END_OBJECT) {
      String viewName = aParser.getCurrentName();
      deserializeView(aParser, aCtxt, viewName);
    }

    return new Views(cas);
  }

  private void deserializeView(JsonParser aParser, DeserializationContext aCtxt, String aViewName)
          throws IOException {
    while (aParser.currentToken() != JsonToken.END_OBJECT) {
      aParser.nextValue();
      String fieldName = aParser.getCurrentName();

      switch (fieldName) {
        case JsonCas2Names.VIEW_SOFA_FIELD:
          // Ignore
          break;
        case JsonCas2Names.VIEW_INDEX_FIELD:
          deserializeIndex(aParser, aCtxt, aViewName);
          break;
      }
    }

    aParser.nextToken();
  }

  private void deserializeIndex(JsonParser aParser, DeserializationContext aCtxt, String aViewName)
          throws IOException {
    FeatureStructureIdToViewIndex fsIdToViewIdx = FeatureStructureIdToViewIndex.get(aCtxt);
    aParser.nextToken();
    while (aParser.currentToken() != JsonToken.END_ARRAY) {
      fsIdToViewIdx.assignFsToView(aParser.getIntValue(), aViewName);
      aParser.nextValue();
    }
    aParser.nextToken();
  }
}
