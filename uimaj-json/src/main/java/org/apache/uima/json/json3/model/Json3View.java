package org.apache.uima.json.json3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = { "name" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class Json3View {

}
