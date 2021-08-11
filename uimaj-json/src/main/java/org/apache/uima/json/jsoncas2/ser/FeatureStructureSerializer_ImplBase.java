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

import static java.util.Arrays.sort;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.ID_FIELD;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.TYPE_FIELD;

import java.io.IOException;
import java.util.Set;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.json.jsoncas2.JsonCas2Names;
import org.apache.uima.json.jsoncas2.mode.ViewsMode;
import org.apache.uima.json.jsoncas2.ref.FeatureStructureToViewIndex;
import org.apache.uima.json.jsoncas2.ref.ReferenceCache;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public abstract class FeatureStructureSerializer_ImplBase<T extends FeatureStructure>
        extends StdSerializer<T> {
  private static final long serialVersionUID = -7548009399965871335L;

  public FeatureStructureSerializer_ImplBase(Class<T> aClazz) {
    super(aClazz);
  }

  @Override
  public void serialize(T aFs, JsonGenerator jg, SerializerProvider aProvider) throws IOException {
    ReferenceCache refCache = ReferenceCache.get(aProvider);
    FeatureStructureToViewIndex fsToViewIndex = FeatureStructureToViewIndex.get(aProvider);
    ViewsMode viewsMode = ViewsMode.get(aProvider);

    jg.writeStartObject();
    jg.writeNumberField(ID_FIELD, refCache.fsRef(aFs));
    jg.writeStringField(TYPE_FIELD, refCache.typeRef(aFs.getType()));

    if (viewsMode == ViewsMode.INLINE) {
      Set<String> views = fsToViewIndex.getViewsContainingFs(aFs);

      if (views != null && !views.isEmpty()) {
        String[] viewsArray = views.toArray(new String[views.size()]);
        sort(viewsArray);
        jg.writeArrayFieldStart(JsonCas2Names.VIEWS_FIELD);
        for (String view : viewsArray) {
          jg.writeString(view);
        }
        jg.writeEndArray();
      }
    }

    // List<String> flags = new ArrayList<>();
    // if (((CASImpl) aFs.getCAS()).getDocumentAnnotationNoCreate() == aFs) {
    // flags.add(FLAG_DOCUMENT_ANNOTATION);
    // }
    //
    // if (!flags.isEmpty()) {
    // jg.writeArrayFieldStart(JsonCas2Names.FLAGS_FIELD);
    // for (String flag : flags) {
    // jg.writeString(flag);
    // }
    // jg.writeEndArray();
    // }

    writeBody(refCache, jg, aFs);

    jg.writeEndObject();
  }

  protected abstract void writeBody(ReferenceCache refCache, JsonGenerator jg, FeatureStructure aFs)
          throws IOException;
}
