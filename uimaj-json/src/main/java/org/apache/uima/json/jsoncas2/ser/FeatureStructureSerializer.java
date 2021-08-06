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

import static org.apache.uima.json.jsoncas2.JsonCas2Names.REF_FEATURE_PREFIX;

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.json.jsoncas2.ref.ReferenceCache;

import com.fasterxml.jackson.core.JsonGenerator;

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
  protected void writeBody(ReferenceCache refCache, JsonGenerator jg, FeatureStructure aFs)
          throws IOException {
    for (Feature feature : aFs.getType().getFeatures()) {
      writeFeature(refCache, jg, aFs, feature);
    }
  }

  protected void writeFeature(ReferenceCache refCache, JsonGenerator jg, FeatureStructure aFs,
          Feature aFeature) throws IOException {
    if (!aFeature.getRange().isPrimitive()) {
      FeatureStructure target = aFs.getFeatureValue(aFeature);
      if (target != null) {
        jg.writeNumberField(REF_FEATURE_PREFIX + aFeature.getShortName(),
                refCache.fsRef(aFs.getFeatureValue(aFeature)));
      }
      return;
    }

    if (aFeature.getRange().isStringOrStringSubtype()) {
      String value = aFs.getStringValue(aFeature);
      if (value != null) {
        jg.writeStringField(aFeature.getShortName(), value);
      }

      return;
    }

    jg.writeFieldName(aFeature.getShortName());
    String rangeTypeName = aFeature.getRange().getName();
    switch (rangeTypeName) {
      case CAS.TYPE_NAME_BOOLEAN:
        jg.writeBoolean(aFs.getBooleanValue(aFeature));
        break;
      case CAS.TYPE_NAME_BYTE:
        jg.writeNumber(aFs.getByteValue(aFeature));
        break;
      case CAS.TYPE_NAME_DOUBLE:
        jg.writeNumber(aFs.getDoubleValue(aFeature));
        break;
      case CAS.TYPE_NAME_FLOAT:
        jg.writeNumber(aFs.getFloatValue(aFeature));
        break;
      case CAS.TYPE_NAME_INTEGER:
        jg.writeNumber(aFs.getIntValue(aFeature));
        break;
      case CAS.TYPE_NAME_LONG:
        jg.writeNumber(aFs.getLongValue(aFeature));
        break;
      case CAS.TYPE_NAME_SHORT:
        jg.writeNumber(aFs.getShortValue(aFeature));
        break;
      default:
        throw new IOException("Unsupported primitive type [" + rangeTypeName + "]");
    }
  }
}
