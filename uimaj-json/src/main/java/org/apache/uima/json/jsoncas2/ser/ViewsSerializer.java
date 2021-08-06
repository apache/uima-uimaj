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

import static org.apache.uima.json.jsoncas2.JsonCas2Names.VIEW_INDEX_FIELD;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.VIEW_SOFA_FIELD;

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.json.jsoncas2.mode.SofaMode;
import org.apache.uima.json.jsoncas2.model.Views;
import org.apache.uima.json.jsoncas2.ref.ReferenceCache;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ViewsSerializer extends StdSerializer<Views> {
  private static final long serialVersionUID = 7530813663936058935L;

  public ViewsSerializer() {
    super(Views.class);
  }

  @Override
  public void serialize(Views aViews, JsonGenerator jg, SerializerProvider aProvider)
          throws IOException {
    ReferenceCache refCache = ReferenceCache.get(aProvider);
    SofaMode sofaMode = SofaMode.get(aProvider);

    jg.writeStartObject();

    for (CAS view : aViews) {
      jg.writeFieldName(view.getViewName());

      jg.writeStartObject();

      switch (sofaMode) {
        case AS_PART_OF_VIEW:
          jg.writeFieldName(VIEW_SOFA_FIELD);
          aProvider.defaultSerializeValue(view.getSofa(), jg);
          break;
        case AS_REGULAR_FEATURE_STRUCTURE:
          jg.writeNumberField(VIEW_SOFA_FIELD, refCache.fsRef(view.getSofa()));
          break;
      }

      jg.writeFieldName(VIEW_INDEX_FIELD);
      jg.writeStartArray();
      for (TOP fs : view.getIndexedFSs()) {
        jg.writeNumber(refCache.fsRef(fs));
      }
      jg.writeEndArray();

      jg.writeEndObject();
    }

    jg.writeEndObject();
  }
}
