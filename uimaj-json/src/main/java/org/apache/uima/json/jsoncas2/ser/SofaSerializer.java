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

import static org.apache.uima.json.jsoncas2.JsonCas2Names.VIEW_MEMBERS_FIELD;

import java.io.IOException;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.json.jsoncas2.ref.ReferenceCache;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

@Deprecated
public class SofaSerializer extends FeatureStructureSerializer {
  private static final long serialVersionUID = -5346232657650250679L;

  public SofaSerializer() {
    super(Sofa.class);
  }

  @Override
  protected void writeBody(SerializerProvider aProvider, JsonGenerator jg, FeatureStructure aFs)
          throws IOException {
    super.writeBody(aProvider, jg, aFs);

    ReferenceCache refCache = ReferenceCache.get(aProvider);
    Sofa sofa = (Sofa) aFs;

    jg.writeFieldName(VIEW_MEMBERS_FIELD);
    jg.writeStartArray();
    for (TOP fs : sofa.getCAS().getView(sofa.getSofaID()).getIndexedFSs()) {
      jg.writeNumber(refCache.fsRef(fs));
    }
    jg.writeEndArray();
  }
}
