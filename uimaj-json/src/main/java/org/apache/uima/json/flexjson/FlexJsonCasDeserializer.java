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
package org.apache.uima.json.flexjson;

import static com.fasterxml.jackson.core.JsonTokenId.ID_END_ARRAY;
import static com.fasterxml.jackson.core.JsonTokenId.ID_END_OBJECT;
import static com.fasterxml.jackson.core.JsonTokenId.ID_START_ARRAY;
import static com.fasterxml.jackson.core.JsonTokenId.ID_START_OBJECT;
import static com.fasterxml.jackson.core.JsonTokenId.ID_STRING;
import static org.apache.uima.cas.CAS.FEATURE_BASE_NAME_LANGUAGE;
import static org.apache.uima.cas.CAS.FEATURE_BASE_NAME_SOFAID;
import static org.apache.uima.cas.CAS.FEATURE_BASE_NAME_SOFASTRING;
import static org.apache.uima.cas.CAS.TYPE_NAME_INTEGER;
import static org.apache.uima.cas.CAS.TYPE_NAME_SOFA;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.json.flexjson.FlexJsonNames.FEATURE_STRUCTURES_FIELD;
import static org.apache.uima.json.flexjson.FlexJsonNames.FLAG_DOCUMENT_ANNOTATION;
import static org.apache.uima.json.flexjson.FlexJsonNames.TYPES_FIELD;
import static org.apache.uima.json.flexjson.FlexJsonNames.VIEWS_FIELD;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.json.flexjson.model.Json2FeatureStructure;
import org.apache.uima.json.flexjson.model.Json2Type;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;

@Deprecated
public class FlexJsonCasDeserializer {
  private final JsonParser parser;

  private Map<String, Json2Type> types;

  private Map<String, CAS> views;

  public FlexJsonCasDeserializer(JsonParser aParser) {
    parser = aParser;
  }

  public void read(CAS aCas) throws IOException {
    views = new HashMap<>();

    aCas.getViewIterator().forEachRemaining(view -> views.put(view.getViewName(), view));

    boolean isFirst = true;

    Map<String, Json2FeatureStructure> jfses = null;

    while (parser.nextToken() != null) {
      if (isFirst) {
        isFirst = false;
        switch (parser.currentTokenId()) {
          case ID_STRING:
            readDocumentText(aCas, parser.getValueAsString());
            return;
          case ID_START_ARRAY:
            jfses = readFeatureStructuresAsArray(parser);
            return;
          case ID_START_OBJECT:
            // In this case, we continue in the main loop and process the type system and
            // feature structures if we find them.
            parser.nextFieldName();
            break;
          default:
            throw new IOException("JSON must start with an object, array or string value");
        }
      }

      if (parser.currentTokenId() == ID_END_ARRAY || parser.currentTokenId() == ID_END_OBJECT) {
        break;
      }

      // If we get here, we are operating on an object-type representation of the full CAS
      switch (parser.getValueAsString()) {
        case TYPES_FIELD:
          parser.nextValue();
          types = readTypeSystem(parser);
          break;
        case VIEWS_FIELD:
          readViews(aCas, parser);
          break;
        case FEATURE_STRUCTURES_FIELD:
          switch (parser.nextToken().id()) {
            case ID_START_ARRAY:
              jfses = readFeatureStructuresAsArray(parser);
              break;
            case ID_START_OBJECT:
              jfses = readFeatureStructuresAsObject(parser);
              break;
            default:
              throw new IOException("Feature structures must be an object or array");
          }
          break;
      }
    }

    if (jfses != null) {
      for (Json2FeatureStructure jfs : jfses.values()) {
        createFeatureStructure(aCas, jfs);
      }
    }
  }

  private void createFeatureStructure(CAS aCas, Json2FeatureStructure aJfs) {
    Type type = aCas.getTypeSystem().getType(aJfs.getType());

    if (TYPE_NAME_SOFA.equals(type.getName())) {
      CAS view = getView(aCas, (String) aJfs.getFeatures().get(FEATURE_BASE_NAME_SOFAID));
      view.setDocumentText((String) aJfs.getFeatures().get(FEATURE_BASE_NAME_SOFASTRING));
      view.setDocumentLanguage((String) aJfs.getFeatures().get(FEATURE_BASE_NAME_LANGUAGE));
      return;
    }

    FeatureStructure fs = getFSCreationView(aCas, aJfs).createFS(type);

    if (aJfs.getFlags() != null && aJfs.getFlags().contains(FLAG_DOCUMENT_ANNOTATION)) {
      fs.getCAS().removeFsFromIndexes(fs.getCAS().getDocumentAnnotation());
    }

    for (Entry<String, Object> entry : aJfs.getFeatures().entrySet()) {
      Feature feature = type.getFeatureByBaseName(entry.getKey());
      if (feature.getRange().isPrimitive()) {
        switch (feature.getRange().getName()) {
          case TYPE_NAME_STRING:
            fs.setStringValue(feature, (String) entry.getValue());
            break;
          case TYPE_NAME_INTEGER:
            fs.setIntValue(feature, (Integer) entry.getValue());
            break;
          default:
            throw new IllegalArgumentException(
                    "Unsupported primitive type [" + feature.getRange().getName() + "]");
        }
      }
    }

    addToIndexes(fs, aJfs);
  }

  private CAS getFSCreationView(CAS aCas, Json2FeatureStructure aJfs) {
    if (aJfs.getViews().isEmpty()) {
      return null;
    }

    if (aJfs.getViews() != null) {
      return getView(aCas, aJfs.getViews().iterator().next());
    }

    // FIXME: case when the views are not inline but separate or when FS is simply
    // not indexed
    throw new UnsupportedOperationException();
  }

  private CAS getView(CAS aCas, String aViewName) {
    CAS view = views.get(aViewName);
    if (view == null) {
      view = aCas.createView(aViewName);
      views.put(aViewName, view);
    }
    return view;
  }

  private void addToIndexes(FeatureStructure aFS, Json2FeatureStructure aJfs) {
    if (aJfs.getViews() != null) {
      for (String viewName : aJfs.getViews()) {
        getView(aFS.getCAS(), viewName).addFsToIndexes(aFS);
      }
    }
  }

  private void readViews(CAS aCas, JsonParser aParser) throws IOException {
    while (aParser.currentTokenId() != ID_END_OBJECT) {
      aParser.nextFieldName();
      String viewName = aParser.readValueAs(String.class);
      aCas.createView(viewName);
    }
  }

  private Map<String, Json2Type> readTypeSystem(JsonParser aParser) throws IOException {
    return aParser.readValueAs(TYPES_MAP_TYPE_REF);
  }

  private Map<String, Json2FeatureStructure> readFeatureStructuresAsObject(JsonParser aParser)
          throws IOException {
    Map<String, Json2FeatureStructure> jfses = new LinkedHashMap<>();
    while (aParser.currentTokenId() != ID_END_OBJECT) {
      String id = aParser.nextFieldName();
      Json2FeatureStructure jfs = aParser.readValueAs(Json2FeatureStructure.class);
      jfs.setId(id);
      jfses.put(id, jfs);
    }

    return jfses;
  }

  private Map<String, Json2FeatureStructure> readFeatureStructuresAsArray(JsonParser aParser)
          throws IOException {
    Map<String, Json2FeatureStructure> jfses = new LinkedHashMap<>();
    parser.nextValue();
    while (aParser.currentTokenId() != ID_END_ARRAY) {
      Json2FeatureStructure jfs = aParser.readValueAs(Json2FeatureStructure.class);
      aParser.nextToken();
      jfses.put(jfs.getId(), jfs);
    }

    return jfses;
  }

  private void readDocumentText(CAS aCas, String aString) {
    aCas.setDocumentText(aString);
  }

  public static final TypeReference<LinkedHashMap<String, Json2Type>> TYPES_MAP_TYPE_REF = //
          new TypeReference<LinkedHashMap<String, Json2Type>>() {
          };
}
