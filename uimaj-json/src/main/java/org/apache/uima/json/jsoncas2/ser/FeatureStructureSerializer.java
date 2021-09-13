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

import static org.apache.uima.json.jsoncas2.JsonCas2Names.NUMBER_FLOAT_NAN;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.NUMBER_FLOAT_NEGATIVE_INFINITY;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.NUMBER_FLOAT_POSITIVE_INFINITY;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.NUMERIC_FEATURE_PREFIX;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.REF_FEATURE_PREFIX;

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.json.jsoncas2.mode.OffsetConversionMode;
import org.apache.uima.json.jsoncas2.ref.ReferenceCache;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.SerializerProvider;

public class FeatureStructureSerializer
        extends FeatureStructureSerializer_ImplBase<FeatureStructure> {
  private static final long serialVersionUID = -5346232657650250679L;

  public FeatureStructureSerializer() {
    super(FeatureStructure.class);
  }

  protected FeatureStructureSerializer(Class<? extends FeatureStructure> aClazz) {
    super((Class<FeatureStructure>) aClazz);
  }

  @Override
  protected void writeBody(SerializerProvider aProvider, JsonGenerator aJg, FeatureStructure aFs)
          throws IOException {
    ReferenceCache refCache = ReferenceCache.get(aProvider);
    for (Feature feature : aFs.getType().getFeatures()) {
      writeFeature(aProvider, refCache, aJg, aFs, feature);
    }
  }

  protected void writeFeature(SerializerProvider aProvider, ReferenceCache aRefCache,
          JsonGenerator aJg, FeatureStructure aFs, Feature aFeature) throws IOException {
    if (!aFeature.getRange().isPrimitive()) {
      FeatureStructure target = aFs.getFeatureValue(aFeature);
      if (target != null) {
        aJg.writeNumberField(REF_FEATURE_PREFIX + aFeature.getShortName(),
                aRefCache.fsRef(aFs.getFeatureValue(aFeature)));
      }
      return;
    }

    if (aFeature.getRange().isStringOrStringSubtype()) {
      String value = aFs.getStringValue(aFeature);
      if (value != null) {
        aJg.writeStringField(aFeature.getShortName(), value);
      }

      return;
    }

    String rangeTypeName = aFeature.getRange().getName();
    switch (rangeTypeName) {
      case CAS.TYPE_NAME_BOOLEAN:
        aJg.writeBooleanField(aFeature.getShortName(), aFs.getBooleanValue(aFeature));
        break;
      case CAS.TYPE_NAME_BYTE:
        aJg.writeNumberField(aFeature.getShortName(), aFs.getByteValue(aFeature));
        break;
      case CAS.TYPE_NAME_DOUBLE:
        writeFloatingPointField(aJg, aFeature.getShortName(), aFs.getDoubleValue(aFeature));
        break;
      case CAS.TYPE_NAME_FLOAT:
        writeFloatingPointField(aJg, aFeature.getShortName(), aFs.getFloatValue(aFeature));
        break;
      case CAS.TYPE_NAME_INTEGER: {
        aJg.writeFieldName(aFeature.getShortName());
        int value = aFs.getIntValue(aFeature);
        value = convertOffsetsIfNecessary(aProvider, aFs, aFeature, value);
        aJg.writeNumber(value);
        break;
      }
      case CAS.TYPE_NAME_LONG:
        aJg.writeNumberField(aFeature.getShortName(), aFs.getLongValue(aFeature));
        break;
      case CAS.TYPE_NAME_SHORT:
        aJg.writeNumberField(aFeature.getShortName(), aFs.getShortValue(aFeature));
        break;
      default:
        throw new IOException("Unsupported primitive type [" + rangeTypeName + "]");
    }
  }

  private void writeFloatingPointField(JsonGenerator aJg, String aFeatureName, double aValue)
          throws IOException {
    if (Double.isNaN(aValue)) {
      aJg.writeStringField(NUMERIC_FEATURE_PREFIX + aFeatureName, NUMBER_FLOAT_NAN);
    } else if (aValue == Double.NEGATIVE_INFINITY) {
      aJg.writeStringField(NUMERIC_FEATURE_PREFIX + aFeatureName, NUMBER_FLOAT_NEGATIVE_INFINITY);
    } else if (aValue == Double.POSITIVE_INFINITY) {
      aJg.writeStringField(NUMERIC_FEATURE_PREFIX + aFeatureName, NUMBER_FLOAT_POSITIVE_INFINITY);
    } else {
      aJg.writeNumberField(aFeatureName, aValue);
    }
  }

  private int convertOffsetsIfNecessary(DatabindContext aCtxt, FeatureStructure aFs,
          Feature aFeature, int aValue) {
    if (aFs instanceof Annotation && (CAS.FEATURE_FULL_NAME_BEGIN.equals(aFeature.getName())
            || CAS.FEATURE_FULL_NAME_END.equals(aFeature.getName()))) {
      Annotation ann = (Annotation) aFs;
      return OffsetConversionMode.getConverter(aCtxt, ann.getSofa().getSofaID()) //
              .map(conv -> conv.mapInternal(aValue)) //
              .orElse(aValue);
    }

    return aValue;
  }
}
