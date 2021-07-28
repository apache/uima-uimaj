package org.apache.uima.json.json3.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = { "types", "views", "featureStructures" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class Json3Cas {
  Map<String, Json3Type> types;
  Map<String, Json3View> views;
  List<Json3FeatureStructure> featureStructures;
}
