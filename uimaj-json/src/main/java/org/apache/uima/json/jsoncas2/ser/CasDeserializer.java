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

import static com.fasterxml.jackson.core.JsonTokenId.ID_END_ARRAY;
import static com.fasterxml.jackson.core.JsonTokenId.ID_END_OBJECT;
import static com.fasterxml.jackson.core.JsonTokenId.ID_START_OBJECT;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.FEATURE_STRUCTURES_FIELD;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.HEADER_FIELD;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.TYPES_FIELD;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.VIEWS_FIELD;

import java.io.IOException;
import java.util.Map.Entry;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.json.jsoncas2.mode.OffsetConversionMode;
import org.apache.uima.json.jsoncas2.model.FeatureStructures;
import org.apache.uima.json.jsoncas2.model.Header;
import org.apache.uima.json.jsoncas2.model.Views;
import org.apache.uima.json.jsoncas2.ref.FeatureStructureIdToViewIndex;
import org.apache.uima.json.jsoncas2.ref.FeatureStructureToIdIndex;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;

public class CasDeserializer extends CasDeserializer_ImplBase<CAS> {
  private static final long serialVersionUID = -5937326876753347248L;

  public CasDeserializer() {
    super(CAS.class);
  }

  @Override
  public CAS deserialize(JsonParser aParser, DeserializationContext aCtxt)
          throws IOException, JsonProcessingException {

    FeatureStructureIdToViewIndex.set(aCtxt, new FeatureStructureIdToViewIndex());
    FeatureStructureToIdIndex.set(aCtxt, new FeatureStructureToIdIndex());

    boolean isFirst = true;
    CAS cas = null;
    TypeSystemDescription types = null;

    while (aParser.currentToken() != null) {
      if (isFirst) {
        isFirst = false;
        switch (aParser.currentTokenId()) {
          // case ID_STRING:
          // readDocumentText(aCas, parser.getValueAsString());
          // return;
          // case ID_START_ARRAY:
          // jfses = readFeatureStructuresAsArray(parser);
          // return;
          case ID_START_OBJECT:
            // In this case, we continue in the main loop and process the type system and
            // feature structures if we find them.
            aParser.nextFieldName();
            break;
          default:
            throw new IOException("JSON must start with an object, array or string value, but was ["
                    + aParser.currentTokenId() + "]");
        }
      }

      if (aParser.currentTokenId() == ID_END_ARRAY || aParser.currentTokenId() == ID_END_OBJECT) {
        break;
      }

      // If we get here, we are operating on an object-type representation of the full CAS
      switch (aParser.getCurrentName()) {
        case HEADER_FIELD: {
          aParser.nextValue();
          Header header = aCtxt.readValue(aParser, Header.class);
          OffsetConversionMode.set(aCtxt, header.getOffsetEncoding());
          aParser.nextToken();
          break;
        }
        case TYPES_FIELD:
          aParser.nextValue();
          types = aCtxt.readValue(aParser, TypeSystemDescription.class);
          aParser.nextValue();
          cas = createCasOrGetFromContext(aCtxt, types);
          break;
        case VIEWS_FIELD:
          aCtxt.readValue(aParser, Views.class);
          break;
        case FEATURE_STRUCTURES_FIELD:
          aCtxt.readValue(aParser, FeatureStructures.class);
          break;
      }
    }

    // Index FS in the respective views
    FeatureStructureIdToViewIndex fsIdToViewIndex = FeatureStructureIdToViewIndex.get(aCtxt);
    for (Entry<Integer, FeatureStructure> fsEntry : FeatureStructureToIdIndex.get(aCtxt)
            .getAllFeatureStructures()) {
      for (String viewName : fsIdToViewIndex.getViewsContainingFs(fsEntry.getKey())) {
        cas.getView(viewName).addFsToIndexes(fsEntry.getValue());
      }
    }

    return cas;
  }

  private CAS createCasOrGetFromContext(DeserializationContext aCtxt, TypeSystemDescription aTypes)
          throws IOException {
    CAS cas = getCas(aCtxt);
    if (cas != null) {
      return cas;
    }

    try {
      return CasCreationUtils.createCas();
    } catch (ResourceInitializationException e) {
      throw new IOException(e);
    }
  }
}
