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
import java.io.OutputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.json.jsoncas2.mode.FeatureStructuresMode;
import org.apache.uima.json.jsoncas2.mode.SofaMode;
import org.apache.uima.json.jsoncas2.ref.ReferenceCache;
import org.apache.uima.json.jsoncas2.ser.CasSerializer;
import org.apache.uima.json.jsoncas2.ser.CommonArrayFSSerializer;
import org.apache.uima.json.jsoncas2.ser.FeatureSerializer;
import org.apache.uima.json.jsoncas2.ser.FeatureStructureSerializer;
import org.apache.uima.json.jsoncas2.ser.FeatureStructuresAsArraySerializer;
import org.apache.uima.json.jsoncas2.ser.FeatureStructuresAsObjectSerializer;
import org.apache.uima.json.jsoncas2.ser.HeaderSerializer;
import org.apache.uima.json.jsoncas2.ser.SofaSerializer;
import org.apache.uima.json.jsoncas2.ser.TypeSerializer;
import org.apache.uima.json.jsoncas2.ser.TypeSystemSerializer;
import org.apache.uima.json.jsoncas2.ser.ViewsSerializer;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JsonCas2Serializer {

  private FeatureStructuresMode fsMode = FeatureStructuresMode.AS_ARRAY;
  private SofaMode sofaMode = SofaMode.AS_REGULAR_FEATURE_STRUCTURE;
  private ObjectMapper cachedMapper;

  public void setFsMode(FeatureStructuresMode aFsMode) {
    fsMode = aFsMode;
  }

  public FeatureStructuresMode getFsMode() {
    return fsMode;
  }

  public void setSofaMode(SofaMode aSofaMode) {
    sofaMode = aSofaMode;
  }

  public SofaMode getSofaMode() {
    return sofaMode;
  }

  private synchronized ObjectMapper getMapper() {
    if (cachedMapper == null) {
      SimpleModule module = new SimpleModule("UIMA CAS JSON",
              new Version(1, 0, 0, null, null, null));

      ReferenceCache.Builder refCacheBuilder = ReferenceCache.builder();
      module.addSerializer(new CasSerializer(refCacheBuilder::build));
      module.addSerializer(new TypeSystemSerializer());
      module.addSerializer(new TypeSerializer());
      module.addSerializer(new FeatureSerializer());
      module.addSerializer(new CommonArrayFSSerializer());

      switch (sofaMode) {
        case AS_PART_OF_VIEW:
          module.addSerializer(new SofaSerializer());
          break;
        case AS_REGULAR_FEATURE_STRUCTURE:
          // Nothing to do
          break;
      }

      module.addSerializer(new FeatureStructureSerializer());

      switch (fsMode) {
        case AS_ARRAY:
          module.addSerializer(new FeatureStructuresAsArraySerializer());
          break;
        case AS_OBJECT:
          module.addSerializer(new FeatureStructuresAsObjectSerializer());
          break;
      }

      module.addSerializer(new ViewsSerializer());
      module.addSerializer(new HeaderSerializer());

      cachedMapper = new ObjectMapper();
      cachedMapper.registerModule(module);
    }

    return cachedMapper;
  }

  private ObjectWriter getWriter() {
    return getMapper().writerWithDefaultPrettyPrinter() //
            .withAttribute(SofaMode.KEY, sofaMode) //
            .withAttribute(FeatureStructuresMode.KEY, fsMode);
  }

  public void serialize(CAS aCas, File aTargetFile) throws IOException {
    getWriter().writeValue(aTargetFile, aCas);
  }

  public void serialize(CAS aCas, OutputStream aTargetStream) throws IOException {
    getWriter().writeValue(aTargetStream, aCas);
  }
}
