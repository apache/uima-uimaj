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
package org.apache.uima.json.jsoncas2.mode;

import java.util.Optional;

import org.apache.uima.json.jsoncas2.encoding.OffsetConverter;
import org.apache.uima.json.jsoncas2.encoding.Utf16CodeunitOffsetConverter;
import org.apache.uima.json.jsoncas2.encoding.Utf32CodepointOffsetConverter;
import org.apache.uima.json.jsoncas2.encoding.Utf8ByteOffsetConverter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DatabindContext;

public enum OffsetConversionMode {
  @JsonProperty("UTF-8") //
  UTF_8, //
  @JsonProperty("UTF-16") //
  UTF_16, //
  @JsonProperty("UTF-32") //
  UTF_32;

  private static final String SEPARATOR = "::";
  public static final String KEY = "UIMA.OffsetConversionMode";

  public static void set(DatabindContext aProvider, OffsetConversionMode aMode) {
    aProvider.setAttribute(KEY, aMode);
  }

  public static OffsetConversionMode getDefault() {
    return UTF_16;
  }

  public static OffsetConversionMode get(DatabindContext aProvider) {
    return (OffsetConversionMode) aProvider.getAttribute(KEY);
  }

  public static OffsetConversionMode getOrDefault(DatabindContext aProvider) {
    OffsetConversionMode mode = get(aProvider);
    if (mode != null) {
      return mode;
    }
    return getDefault();
  }

  public static OffsetConverter initConverter(DatabindContext aProvider, String aView,
          String aText) {
    OffsetConverter converter;
    switch (getOrDefault(aProvider)) {
      case UTF_8:
        converter = new Utf8ByteOffsetConverter(aText);
        break;
      case UTF_16:
        converter = new Utf16CodeunitOffsetConverter(aText);
        break;
      case UTF_32:
        converter = new Utf32CodepointOffsetConverter(aText);
        break;
      default:
        throw new IllegalArgumentException("Unsupported conversion mode: [" + aProvider + "]");
    }

    aProvider.setAttribute(KEY + SEPARATOR + aView, converter);

    return converter;
  }

  public static Optional<OffsetConverter> getConverter(DatabindContext aProvider, String aSofaId) {
    return Optional.ofNullable((OffsetConverter) aProvider.getAttribute(KEY + SEPARATOR + aSofaId));
  }
}
