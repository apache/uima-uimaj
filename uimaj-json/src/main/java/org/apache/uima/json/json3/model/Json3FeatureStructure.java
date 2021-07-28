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
package org.apache.uima.json.json3.model;

import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.json.json3.Json3Names.ID_FIELD;
import static org.apache.uima.json.json3.Json3Names.TYPE_FIELD;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.uima.json.json3.Json3Names;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = { "id", "type", "views" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class Json3FeatureStructure {
  @JsonProperty(value = ID_FIELD, required = false)
  private String id;

  @JsonProperty(value = TYPE_FIELD, required = true, defaultValue = TYPE_NAME_ANNOTATION)
  private String type;

  @JsonProperty(value = Json3Names.FLAGS_FIELD, required = false)
  private LinkedHashSet<String> flags;

  @JsonProperty(value = Json3Names.VIEWS_FIELD, required = false)
  private LinkedHashSet<String> views;

  private Map<String, Object> features = new LinkedHashMap<>();

  public String getId() {
    return id;
  }

  public void setId(String aId) {
    id = aId;
  }

  public String getType() {
    return type;
  }

  public void setType(String aType) {
    type = aType;
  }

  public LinkedHashSet<String> getViews() {
    return views;
  }

  public void setViews(LinkedHashSet<String> aViews) {
    views = aViews;
  }

  public LinkedHashSet<String> getFlags() {
    return flags;
  }

  public void setFlags(LinkedHashSet<String> aFlags) {
    flags = aFlags;
  }

  @JsonAnySetter
  public void setFeature(String name, Object value) {
    if (name.startsWith(Json3Names.REF)) {
      name = name.substring(1);
    }

    features.put(name, value);
  }

  @JsonAnyGetter
  public Map<String, Object> getFeatures() {
    return features;
  }
}
