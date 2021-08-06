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

import static org.apache.uima.json.jsoncas2.JsonCas2Names.ELEMENTS_FIELD;

import java.io.IOException;

import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.json.jsoncas2.ref.ReferenceCache;

import com.fasterxml.jackson.core.JsonGenerator;

@SuppressWarnings("rawtypes")
public class CommonArrayFSSerializer extends FeatureStructureSerializer_ImplBase<CommonArrayFS> {
  private static final long serialVersionUID = 4842019532480552884L;

  public CommonArrayFSSerializer() {
    super(CommonArrayFS.class);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void writeBody(ReferenceCache aRefCache, JsonGenerator aJg, FeatureStructure aFs)
          throws IOException {
    aJg.writeFieldName(ELEMENTS_FIELD);
    switch (aFs.getType().getName()) {
      case CAS.TYPE_NAME_BYTE_ARRAY: {
        aJg.writeBinary(((ByteArrayFS) aFs).toArray());
        break;
      }
      case CAS.TYPE_NAME_DOUBLE_ARRAY: {
        double[] values = ((DoubleArrayFS) aFs).toArray();
        aJg.writeArray(values, 0, values.length);
        break;
      }
      case CAS.TYPE_NAME_FLOAT_ARRAY: {
        float[] fValues = ((FloatArrayFS) aFs).toArray();
        double[] dValues = new double[fValues.length];
        for (int i = 0; i < fValues.length; i++) {
          dValues[i] = fValues[i];
        }
        aJg.writeArray(dValues, 0, dValues.length);
        break;
      }
      case CAS.TYPE_NAME_INTEGER_ARRAY: {
        int[] values = ((IntArrayFS) aFs).toArray();
        aJg.writeArray(values, 0, values.length);
        break;
      }
      case CAS.TYPE_NAME_LONG_ARRAY: {
        long[] values = ((LongArrayFS) aFs).toArray();
        aJg.writeArray(values, 0, values.length);
        break;
      }
      case CAS.TYPE_NAME_SHORT_ARRAY: {
        short[] sValues = ((ShortArrayFS) aFs).toArray();
        int[] iValues = new int[sValues.length];
        for (int i = 0; i < sValues.length; i++) {
          iValues[i] = sValues[i];
        }
        aJg.writeArray(iValues, 0, iValues.length);
        break;
      }
      case CAS.TYPE_NAME_STRING_ARRAY: {
        String[] values = ((StringArrayFS) aFs).toArray();
        aJg.writeArray(values, 0, values.length);
        break;
      }
      case CAS.TYPE_NAME_FS_ARRAY: // fall-through
      default: {
        aJg.writeStartArray();
        for (FeatureStructure fs : ((FSArray<FeatureStructure>) aFs)) {
          aJg.writeNumber(aRefCache.fsRef(fs));
        }
        aJg.writeEndArray();
        break;
      }
    }
  }
}
