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
package org.apache.uima.json.jsoncas2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.json.jsoncas2.mode.FeatureStructuresMode;
import org.apache.uima.json.jsoncas2.model.FeatureStructures;
import org.apache.uima.json.jsoncas2.model.Views;
import org.apache.uima.json.jsoncas2.ser.CasDeserializer;
import org.apache.uima.json.jsoncas2.ser.FeatureDeserializer;
import org.apache.uima.json.jsoncas2.ser.FeatureStructureDeserializer;
import org.apache.uima.json.jsoncas2.ser.FeatureStructuresAsArrayDeserializer;
import org.apache.uima.json.jsoncas2.ser.FeatureStructuresAsObjectDeserializer;
import org.apache.uima.json.jsoncas2.ser.TypeDeserializer;
import org.apache.uima.json.jsoncas2.ser.TypeSystemDeserializer;
import org.apache.uima.json.jsoncas2.ser.ViewsDeserializer;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JsonCas2Deserializer {
  private FeatureStructuresMode fsMode = FeatureStructuresMode.AS_ARRAY;
  private ObjectMapper cachedMapper;

  public void setFsMode(FeatureStructuresMode aFsMode) {
    fsMode = aFsMode;
  }

  public FeatureStructuresMode getFsMode() {
    return fsMode;
  }

  private synchronized ObjectMapper getMapper() {
    if (cachedMapper == null) {
      SimpleModule module = new SimpleModule("UIMA CAS JSON",
              new Version(1, 0, 0, null, null, null));

      module.addDeserializer(CAS.class, new CasDeserializer());
      module.addDeserializer(FeatureStructure.class, new FeatureStructureDeserializer());

      switch (fsMode) {
        case AS_ARRAY:
          module.addDeserializer(FeatureStructures.class,
                  new FeatureStructuresAsArrayDeserializer());
          break;
        case AS_OBJECT:
          module.addDeserializer(FeatureStructures.class,
                  new FeatureStructuresAsObjectDeserializer());
          break;
      }

      module.addDeserializer(FeatureDescription.class, new FeatureDeserializer());
      module.addDeserializer(TypeDescription.class, new TypeDeserializer());
      module.addDeserializer(TypeSystemDescription.class, new TypeSystemDeserializer());
      module.addDeserializer(Views.class, new ViewsDeserializer());

      cachedMapper = new ObjectMapper();
      cachedMapper.registerModule(module);
    }
    return cachedMapper;
  }

  public void deserialize(File aSourceFile, CAS aTargetCas) throws IOException {
    getMapper().reader().forType(CAS.class) //
            .withAttribute(CasDeserializer.CONTEXT_CAS, aTargetCas) //
            .readValue(aSourceFile);
  }

  public void deserialize(InputStream aSourceStream, CAS aTargetCas) throws IOException {
    getMapper().reader().forType(CAS.class) //
            .withAttribute(CasDeserializer.CONTEXT_CAS, aTargetCas) //
            .readValue(aSourceStream);
  }
}
