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

import static com.fasterxml.jackson.core.JsonToken.END_OBJECT;
import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;
import static java.lang.Integer.MIN_VALUE;
import static org.apache.uima.cas.CAS.FEATURE_BASE_NAME_SOFAARRAY;
import static org.apache.uima.cas.CAS.FEATURE_BASE_NAME_SOFAID;
import static org.apache.uima.cas.CAS.FEATURE_BASE_NAME_SOFAMIME;
import static org.apache.uima.cas.CAS.FEATURE_BASE_NAME_SOFANUM;
import static org.apache.uima.cas.CAS.FEATURE_BASE_NAME_SOFASTRING;
import static org.apache.uima.cas.CAS.FEATURE_BASE_NAME_SOFAURI;
import static org.apache.uima.cas.CAS.TYPE_NAME_BOOLEAN_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_BYTE;
import static org.apache.uima.cas.CAS.TYPE_NAME_BYTE_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_DOCUMENT_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_DOUBLE;
import static org.apache.uima.cas.CAS.TYPE_NAME_DOUBLE_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_FLOAT;
import static org.apache.uima.cas.CAS.TYPE_NAME_FLOAT_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_INTEGER;
import static org.apache.uima.cas.CAS.TYPE_NAME_INTEGER_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_LONG;
import static org.apache.uima.cas.CAS.TYPE_NAME_LONG_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_SHORT;
import static org.apache.uima.cas.CAS.TYPE_NAME_SHORT_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_SOFA;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.ANCHOR_FEATURE_PREFIX;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.ID_FIELD;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.NUMBER_FLOAT_NAN;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.NUMBER_FLOAT_NEGATIVE_INFINITY;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.NUMBER_FLOAT_NEGATIVE_INFINITY_ABBR;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.NUMBER_FLOAT_POSITIVE_INFINITY;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.NUMBER_FLOAT_POSITIVE_INFINITY_ABBR;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.NUMERIC_FEATURE_PREFIX;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.REF_FEATURE_PREFIX;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.RESERVED_FIELD_PREFIX;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.TYPE_FIELD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.json.jsoncas2.mode.OffsetConversionMode;
import org.apache.uima.json.jsoncas2.ref.FeatureStructureToIdIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;

public class FeatureStructureDeserializer extends CasDeserializer_ImplBase<FeatureStructure> {
  private static final long serialVersionUID = -5937326876753347248L;

  private final Logger log = LoggerFactory.getLogger(getClass());

  private enum FieldType {
    REGULAR, REFERENCE, NUMBER, ANCHOR
  }

  public FeatureStructureDeserializer() {
    super(FeatureStructure.class);
  }

  @Override
  public FeatureStructure deserialize(JsonParser aParser, DeserializationContext aCtxt)
          throws IOException, JsonProcessingException {

    CAS cas = getCas(aCtxt);

    if (aParser.currentToken() != START_OBJECT) {
      throw new JsonParseException(aParser, "Expected feature structure to start with "
              + START_OBJECT + " but found " + aParser.currentToken() + " instead");
    }

    int fsId = MIN_VALUE;
    // Handle case where feature structures section is represented as a map instead of an array
    if (aParser.getCurrentName() != null) {
      fsId = Integer.parseInt(aParser.getCurrentName());
    }

    FeatureStructure fs = null;
    aParser.nextValue();
    while (aParser.currentToken() != END_OBJECT) {
      String fieldName = aParser.currentName();

      // log.trace("Deserializing {}: {}", fieldName, aParser.getText());

      if (fieldName.startsWith(RESERVED_FIELD_PREFIX)) {
        switch (fieldName) {
          case ID_FIELD:
            // Handle case where feature structures section is represented as an array
            fsId = aParser.getIntValue();
            break;
          case TYPE_FIELD:
            if (fsId == MIN_VALUE) {
              throw new JsonParseException(aParser, TYPE_FIELD + " must come after " + ID_FIELD);
            }
            String typeName = aParser.getValueAsString();

            switch (typeName) {
              case TYPE_NAME_BOOLEAN_ARRAY:
                fs = deserializeBooleanArray(aParser, cas);
                FeatureStructureToIdIndex.get(aCtxt).put(fsId, fs);
                continue;
              case TYPE_NAME_BYTE_ARRAY:
                fs = deserializeByteArray(aParser, cas);
                FeatureStructureToIdIndex.get(aCtxt).put(fsId, fs);
                continue;
              case TYPE_NAME_DOUBLE_ARRAY:
                fs = deserializeDoubleArray(aParser, cas);
                FeatureStructureToIdIndex.get(aCtxt).put(fsId, fs);
                continue;
              case TYPE_NAME_FLOAT_ARRAY:
                fs = deserializeFloatArray(aParser, cas);
                FeatureStructureToIdIndex.get(aCtxt).put(fsId, fs);
                continue;
              case TYPE_NAME_INTEGER_ARRAY:
                fs = deserializeIntegerArray(aParser, cas);
                FeatureStructureToIdIndex.get(aCtxt).put(fsId, fs);
                continue;
              case TYPE_NAME_LONG_ARRAY:
                fs = deserializeLongArray(aParser, cas);
                FeatureStructureToIdIndex.get(aCtxt).put(fsId, fs);
                continue;
              case TYPE_NAME_SHORT_ARRAY:
                fs = deserializeShortArray(aParser, cas);
                FeatureStructureToIdIndex.get(aCtxt).put(fsId, fs);
                continue;
              case TYPE_NAME_STRING_ARRAY:
                fs = deserializeStringArray(aParser, cas);
                FeatureStructureToIdIndex.get(aCtxt).put(fsId, fs);
                continue;
              case CAS.TYPE_NAME_FS_ARRAY:
                fs = deserializeFsArray(aParser, cas, aCtxt);
                FeatureStructureToIdIndex.get(aCtxt).put(fsId, fs);
                continue;
              case TYPE_NAME_SOFA:
                fs = createSofaFS(cas, aParser, aCtxt);
                FeatureStructureToIdIndex.get(aCtxt).put(fsId, fs);
                continue;
              default:
                fs = createFS(aParser, aCtxt, fsId, cas);
                break;
            }
            break;
          // case FLAGS_FIELD:
          // // FIXME: We probably don't need the flags field at all.
          // aParser.nextToken();
          // aParser.skipChildren();
          // aParser.nextToken();
          // break;
        }

        aParser.nextValue();
        continue;
      }

      if (fs == null || fsId == MIN_VALUE) {
        throw new JsonParseException(aParser,
                "Features must come after " + ID_FIELD + "" + TYPE_FIELD);
      }

      FieldType fieldType = FieldType.REGULAR;
      if (fieldName.startsWith(REF_FEATURE_PREFIX)) {
        fieldName = fieldName.substring(REF_FEATURE_PREFIX.length());
        fieldType = FieldType.REFERENCE;
      } else if (fieldName.startsWith(NUMERIC_FEATURE_PREFIX)) {
        fieldName = fieldName.substring(NUMERIC_FEATURE_PREFIX.length());
        fieldType = FieldType.NUMBER;
      } else if (fieldName.startsWith(ANCHOR_FEATURE_PREFIX)) {
        fieldName = fieldName.substring(ANCHOR_FEATURE_PREFIX.length());
        fieldType = FieldType.ANCHOR;
      }

      if (CAS.FEATURE_FULL_NAME_SOFA
              .equals(fs.getType().getFeatureByBaseName(fieldName).getName())) {
        // Ignore the SofA feature of AnnotationBase-derived types - this feature cannot be set
        // manually - this happens (hopefully) when adding the AnnotationBase FS to the indexes of
        // the particular SofA.
        aParser.nextValue();
        continue;
      }

      if (fieldType == FieldType.REFERENCE) {
        deserializeFsReference(aParser, aCtxt, fs, fieldName);
        aParser.nextValue();
        continue;
      }

      deserializePrimitive(aParser, aCtxt, fs, fieldName, fieldType);
      aParser.nextValue();
    }

    // Special handling of the document annotation
    handleDocumentAnnotation(aCtxt, cas, fs);

    // Register the loaded FS
    FeatureStructureToIdIndex.get(aCtxt).put(fsId, fs);

    return fs;
  }

  private FeatureStructure createFS(JsonParser aParser, DeserializationContext aCtxt, int aFsId,
          CAS aCas) throws IOException {
    String typeName = aParser.getValueAsString();
    TypeSystem ts = aCas.getTypeSystem();
    Type t = ts.getType(typeName);
    if (t == null) {
      throw new JsonParseException(aParser, "Type not found in type system: " + typeName);
    }

    return aCas.createFS(t);
  }

  // Case 1: there is no document annotation yet (unlikely since the document text has probably
  // been set already and this implicitly triggers the creation of a document annotation)
  // -> the document annotation that was created became the primary document annotation,
  // nothing else to do
  //
  // Case 2: there is already a document annotation of the same type as what we deserialize
  // 2a> if it is the first time we deserialize a document annotation, then fill it
  // 2b> otherwise add the new document annotation
  //
  // Case 3: there is already a document annotation but is has a different type
  // 3a> if it is the first time we deserialize a document annotation, then replace it
  // 3b> otherwise add the new document annotation
  private void handleDocumentAnnotation(DeserializationContext aCtxt, CAS aCas,
          FeatureStructure aFS) throws JsonParseException {
    TypeSystem ts = aCas.getTypeSystem();
    Type docAnnoType = ts.getType(TYPE_NAME_DOCUMENT_ANNOTATION);
    if (!ts.subsumes(docAnnoType, aFS.getType())) {
      return;
    }

    String viewName = ((Annotation) aFS).getSofa().getSofaID();

    // Case 2b/3b: we already have handled the primary document annotation, this one is an extra
    if (isDocumentAnnotationCreated(aCtxt, viewName)) {
      // Nothing extra to do
      return;
    }

    Collection<TOP> docAnnotations = aCas.getIndexedFSs(docAnnoType);
    // Case 1: was no document annotation yet
    if (docAnnotations.isEmpty()) {
      markDocumentAnnotationCreated(aCtxt, viewName);
      return;
    }

    // Case 3a: need to replace the existing document annotation because it has a different type
    docAnnotations.forEach(aCas::removeFsFromIndexes);
    aCas.addFsToIndexes(aFS);
    markDocumentAnnotationCreated(aCtxt, viewName);
  }

  /**
   * Parses SofaFS entries from the feature structure list. If no data has been set in the SofaFS,
   * this method will return null.
   */
  private FeatureStructure createSofaFS(CAS aCas, JsonParser aParser, DeserializationContext aCtxt)
          throws IOException {
    int sofaNum = -1;
    String sofaID = null;
    String mimeType = null;
    String sofaURI = null;
    String sofaString = null;
    FeatureStructure sofaArray = null;

    aParser.nextValue();
    while (aParser.currentToken() != JsonToken.END_OBJECT) {
      String fieldName = aParser.currentName();

      switch (fieldName) {
        case FEATURE_BASE_NAME_SOFANUM:
          sofaNum = aParser.getValueAsInt();
          break;
        case FEATURE_BASE_NAME_SOFAID:
          sofaID = aParser.getValueAsString();
          break;
        case FEATURE_BASE_NAME_SOFAMIME:
          mimeType = aParser.getValueAsString();
          break;
        case FEATURE_BASE_NAME_SOFAURI:
          sofaURI = aParser.getValueAsString();
          break;
        case FEATURE_BASE_NAME_SOFASTRING:
          sofaString = aParser.getValueAsString();
          break;
        case REF_FEATURE_PREFIX + FEATURE_BASE_NAME_SOFAARRAY: {
          FeatureStructureToIdIndex fsIdx = FeatureStructureToIdIndex.get(aCtxt);
          int sofaArrayId = aParser.getValueAsInt();
          sofaArray = fsIdx.get(sofaArrayId)
                  .orElseThrow(() -> new JsonParseException(aParser, "The SofA array with ID "
                          + sofaArrayId + " must come before the SofAFS referencing it."));
          break;
        }
        default:
          throw new JsonParseException(aParser, "Unexpeced field in SofA: " + fieldName);
      }

      aParser.nextValue();
    }

    if (sofaID == null) {
      throw new JsonParseException(aParser, "SofA must have a sofaID");
    }

    CAS view = createOrGetView(aCas, sofaID);

    if (sofaURI != null) {
      view.setSofaDataURI(sofaURI, mimeType);
    } else if (sofaString != null) {
      view.setSofaDataString(sofaString, mimeType);
      OffsetConversionMode.initConverter(aCtxt, sofaID, sofaString);
    } else if (sofaArray != null) {
      view.setSofaDataArray(sofaArray, mimeType);
    }

    SofaFS sofa = view.getSofa();
    if (sofa != null) {
      return sofa;
    }

    return ((CASImpl) view).getSofaRef();
  }

  private BooleanArrayFS deserializeBooleanArray(JsonParser aParser, CAS aCas) throws IOException {
    // Skip array opening and go to first value (or end of array if there is no value)
    aParser.nextValue();
    aParser.nextValue();
    List<Boolean> values = new ArrayList<>();
    while (aParser.currentToken() != JsonToken.END_ARRAY) {
      values.add(aParser.getBooleanValue());
      aParser.nextValue();
    }
    BooleanArrayFS arrayFs = aCas.createBooleanArrayFS(values.size());
    for (int i = 0; i < values.size(); i++) {
      arrayFs.set(i, values.get(i));
    }
    return arrayFs;
  }

  private ByteArrayFS deserializeByteArray(JsonParser aParser, CAS aCas) throws IOException {
    aParser.nextValue();
    byte[] bytes = aParser.getBinaryValue();
    ByteArrayFS arrayFs = aCas.createByteArrayFS(bytes.length);
    arrayFs.copyFromArray(bytes, 0, 0, bytes.length);
    aParser.nextToken();
    return arrayFs;
  }

  private DoubleArrayFS deserializeDoubleArray(JsonParser aParser, CAS aCas) throws IOException {
    // Skip array opening and go to first value (or end of array if there is no value)
    aParser.nextValue();
    aParser.nextValue();
    List<Double> values = new ArrayList<>();
    while (aParser.currentToken() != JsonToken.END_ARRAY) {
      values.add(readDoubleValue(aParser));
      aParser.nextValue();
    }
    DoubleArrayFS arrayFs = aCas.createDoubleArrayFS(values.size());
    for (int i = 0; i < values.size(); i++) {
      arrayFs.set(i, values.get(i));
    }
    return arrayFs;
  }

  private FloatArrayFS deserializeFloatArray(JsonParser aParser, CAS aCas) throws IOException {
    // Skip array opening and go to first value (or end of array if there is no value)
    aParser.nextValue();
    aParser.nextValue();
    List<Float> values = new ArrayList<>();
    while (aParser.currentToken() != JsonToken.END_ARRAY) {
      values.add((float) readDoubleValue(aParser));
      aParser.nextValue();
    }
    FloatArrayFS arrayFs = aCas.createFloatArrayFS(values.size());
    for (int i = 0; i < values.size(); i++) {
      arrayFs.set(i, values.get(i));
    }
    return arrayFs;
  }

  private double readDoubleValue(JsonParser aParser) throws IOException {
    if (aParser.currentToken() == JsonToken.VALUE_NUMBER_FLOAT) {
      return aParser.getDoubleValue();
    }

    if (aParser.currentToken() == JsonToken.VALUE_STRING) {
      switch (aParser.getValueAsString()) {
        case NUMBER_FLOAT_NAN:
          return Double.NaN;
        case NUMBER_FLOAT_POSITIVE_INFINITY:
        case NUMBER_FLOAT_POSITIVE_INFINITY_ABBR:
          return Double.POSITIVE_INFINITY;
        case NUMBER_FLOAT_NEGATIVE_INFINITY:
        case NUMBER_FLOAT_NEGATIVE_INFINITY_ABBR:
          return Double.NEGATIVE_INFINITY;
        default:
          throw new JsonParseException(aParser,
                  "Expected special floating point value (NaN, -Inf, -Infinity, Inf, Infinity), "
                          + "but got [" + aParser.getValueAsString() + "]");
      }
    }

    throw new JsonParseException(aParser,
            "Expected floating point value as VALUE_NUMBER_FLOAT or VALUE_STRING for special "
                    + "values (Nan, -Infinity, +Infinity), but got [" + aParser.currentToken()
                    + "]");
  }

  private IntArrayFS deserializeIntegerArray(JsonParser aParser, CAS aCas) throws IOException {
    // Skip array opening and go to first value (or end of array if there is no value)
    aParser.nextValue();
    aParser.nextValue();
    List<Integer> values = new ArrayList<>();
    while (aParser.currentToken() != JsonToken.END_ARRAY) {
      values.add(aParser.getIntValue());
      aParser.nextValue();
    }
    IntArrayFS arrayFs = aCas.createIntArrayFS(values.size());
    for (int i = 0; i < values.size(); i++) {
      arrayFs.set(i, values.get(i));
    }
    return arrayFs;
  }

  private LongArrayFS deserializeLongArray(JsonParser aParser, CAS aCas) throws IOException {
    // Skip array opening and go to first value (or end of array if there is no value)
    aParser.nextValue();
    aParser.nextValue();
    List<Long> values = new ArrayList<>();
    while (aParser.currentToken() != JsonToken.END_ARRAY) {
      values.add(aParser.getLongValue());
      aParser.nextValue();
    }
    LongArrayFS arrayFs = aCas.createLongArrayFS(values.size());
    for (int i = 0; i < values.size(); i++) {
      arrayFs.set(i, values.get(i));
    }
    return arrayFs;
  }

  private ShortArrayFS deserializeShortArray(JsonParser aParser, CAS aCas) throws IOException {
    // Skip array opening and go to first value (or end of array if there is no value)
    aParser.nextValue();
    aParser.nextValue();
    List<Short> values = new ArrayList<>();
    while (aParser.currentToken() != JsonToken.END_ARRAY) {
      values.add((short) aParser.getIntValue());
      aParser.nextValue();
    }
    ShortArrayFS arrayFs = aCas.createShortArrayFS(values.size());
    for (int i = 0; i < values.size(); i++) {
      arrayFs.set(i, values.get(i));
    }
    return arrayFs;
  }

  private StringArrayFS deserializeStringArray(JsonParser aParser, CAS aCas) throws IOException {
    // Go to array opening
    aParser.nextValue();
    // Go to first value if any or to end of array
    aParser.nextValue();
    List<String> values = new ArrayList<>();
    while (aParser.currentToken() != JsonToken.END_ARRAY) {
      values.add(aParser.getValueAsString());
      aParser.nextValue();
    }
    StringArrayFS arrayFs = aCas.createStringArrayFS(values.size());
    for (int i = 0; i < values.size(); i++) {
      arrayFs.set(i, values.get(i));
    }
    return arrayFs;
  }

  private ArrayFS<FeatureStructure> deserializeFsArray(JsonParser aParser, CAS aCas,
          DeserializationContext aCtxt) throws IOException {
    // Go to array opening
    aParser.nextValue();
    // Go to first value if any or to end of array
    aParser.nextValue();
    List<Integer> values = new ArrayList<>();
    while (aParser.currentToken() != JsonToken.END_ARRAY) {
      values.add(aParser.getIntValue());
      aParser.nextValue();
    }

    @SuppressWarnings("unchecked")
    ArrayFS<FeatureStructure> arrayFs = aCas.createArrayFS(values.size());
    FeatureStructureToIdIndex idToFsIdx = FeatureStructureToIdIndex.get(aCtxt);
    for (int i = 0; i < values.size(); i++) {
      int targetFsId = values.get(i);
      Optional<FeatureStructure> targetFs = idToFsIdx.get(targetFsId);
      if (targetFs.isPresent()) {
        arrayFs.set(i, targetFs.get());
      } else {
        int finalIndex = i;
        schedulePostprocessing(aCtxt, () -> {
          arrayFs.set(finalIndex,
                  idToFsIdx.get(targetFsId)
                          .orElseThrow(() -> new NoSuchElementException("Unable to resolve ID ["
                                  + targetFsId + "] during array post-processing")));
        });
      }
    }
    return arrayFs;
  }

  private void deserializePrimitive(JsonParser aParser, DeserializationContext aCtxt,
          FeatureStructure aFs, String aFeatureName, FieldType fieldType)
          throws CASRuntimeException, IOException {
    Feature feature = aFs.getType().getFeatureByBaseName(aFeatureName);
    switch (aParser.currentToken()) {
      case VALUE_NULL:
        // No need do to anything really - we just leave the feature alone
        break;
      case VALUE_TRUE: // fall-through
      case VALUE_FALSE:
        aFs.setBooleanValue(feature, aParser.getBooleanValue());
        break;
      case VALUE_STRING:
        aFs.setStringValue(feature, aParser.getValueAsString());
        break;
      case VALUE_NUMBER_FLOAT: // JSON does not distinguish between double and float
        deserializeFloatingPointValue(aParser, aFs, feature, fieldType);
        break;
      case VALUE_NUMBER_INT:
        deserializeIntegerValue(aParser, aCtxt, aFs, feature, fieldType);
        break;
      default:
        throw new JsonParseException(aParser,
                "Expected a feature value as null, a boolean, string, or number but got "
                        + aParser.currentToken());
    }
  }

  private void deserializeFsReference(JsonParser aParser, DeserializationContext aCtxt,
          FeatureStructure aFs, String aFieldName) throws IOException {
    FeatureStructureToIdIndex idToFsIdx = FeatureStructureToIdIndex.get(aCtxt);
    int targetFsId = aParser.getIntValue();
    Optional<FeatureStructure> targetFs = idToFsIdx.get(targetFsId);
    Feature feature = aFs.getType().getFeatureByBaseName(aFieldName);
    if (targetFs.isPresent()) {
      aFs.setFeatureValue(feature, targetFs.get());
    } else {
      FeatureStructure finalFs = aFs;
      schedulePostprocessing(aCtxt, () -> {
        finalFs.setFeatureValue(feature,
                idToFsIdx.get(targetFsId).orElseThrow(() -> new NoSuchElementException(
                        "Unable to resolve ID [" + targetFsId + "] during post-processing")));
      });
    }
  }

  private void deserializeFloatingPointValue(JsonParser aParser, FeatureStructure aFs,
          Feature aFeature, FieldType fieldType) throws CASRuntimeException, IOException {
    switch (aFeature.getRange().getName()) {
      case TYPE_NAME_DOUBLE:
        aFs.setDoubleValue(aFeature, readDoubleValue(aParser));
        break;
      case TYPE_NAME_FLOAT:
        aFs.setFloatValue(aFeature, (float) readDoubleValue(aParser));
        break;
      default:
        throw new JsonParseException(aParser, "Feature of type " + aFeature.getRange().getName()
                + " cannot be set from a JSON value of type " + aParser.currentToken());
    }
  }

  private void deserializeIntegerValue(JsonParser aParser, DeserializationContext aCtxt,
          FeatureStructure aFs, Feature aFeature, FieldType fieldType)
          throws CASRuntimeException, IOException {
    switch (aFeature.getRange().getName()) {
      case TYPE_NAME_BYTE:
        aFs.setByteValue(aFeature, (byte) aParser.getValueAsInt());
        break;
      case TYPE_NAME_INTEGER:
        int value = aParser.getValueAsInt();
        value = convertOffsetsIfNecessary(aCtxt, aFs, aFeature, value, fieldType);
        aFs.setIntValue(aFeature, value);
        break;
      case TYPE_NAME_LONG:
        aFs.setLongValue(aFeature, aParser.getValueAsLong());
        break;
      case TYPE_NAME_SHORT:
        aFs.setShortValue(aFeature, (short) aParser.getValueAsInt());
        break;
      default:
        throw new JsonParseException(aParser, "Feature of type " + aFeature.getRange().getName()
                + " cannot be set from a JSON value of type " + aParser.currentToken());
    }
  }

  private int convertOffsetsIfNecessary(DeserializationContext aCtxt, FeatureStructure aFs,
          Feature aFeature, int aValue, FieldType fieldType) {
    if (aFs instanceof Annotation && (CAS.FEATURE_FULL_NAME_BEGIN.equals(aFeature.getName())
            || CAS.FEATURE_FULL_NAME_END.equals(aFeature.getName())
            || fieldType == FieldType.ANCHOR)) {
      Annotation ann = (Annotation) aFs;
      return OffsetConversionMode.getConverter(aCtxt, ann.getSofa().getSofaID()) //
              .map(conv -> conv.mapExternal(aValue)) //
              .orElse(aValue);
    }

    return aValue;
  }
}
