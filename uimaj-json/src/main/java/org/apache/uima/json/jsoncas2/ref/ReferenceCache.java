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
package org.apache.uima.json.jsoncas2.ref;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;

import com.fasterxml.jackson.databind.DatabindContext;

public class ReferenceCache {
  public static final String KEY = "UIMA.ReferenceCache";

  private final ToIntFunction<FeatureStructure> idRefGenerator;
  private Map<FeatureStructure, Integer> idRefCache = new HashMap<>();

  private final Function<Type, String> typeRefGenerator;
  private Map<Type, String> typeRefCache = new HashMap<>();

  private ReferenceCache(Builder builder) {
    idRefGenerator = builder.idRefGeneratorSupplier.get();
    typeRefGenerator = builder.typeRefGeneratorSupplier.get();
  }

  public int fsRef(FeatureStructure aFs) {
    return idRefCache.computeIfAbsent(aFs, _fs -> idRefGenerator.applyAsInt(_fs));
  }

  public String typeRef(Type aType) {
    return typeRefCache.computeIfAbsent(aType, typeRefGenerator);
  }

  /**
   * Creates builder to build {@link ReferenceCache}.
   * 
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link ReferenceCache}.
   */
  public static final class Builder {
    private Supplier<ToIntFunction<FeatureStructure>> idRefGeneratorSupplier;
    private Supplier<Function<Type, String>> typeRefGeneratorSupplier;

    private Builder() {
      idRefGeneratorSupplier = SequentialIdRefGenerator::new;
      typeRefGeneratorSupplier = FullyQualifiedTypeRefGenerator::new;
    }

    public Builder withIdRefGeneratorSupplier(
            Supplier<ToIntFunction<FeatureStructure>> idRefGeneratorSupplier) {
      this.idRefGeneratorSupplier = idRefGeneratorSupplier;
      return this;
    }

    public Builder withTypeRefGeneratorSupplier(
            Supplier<Function<Type, String>> typeRefGeneratorSupplier) {
      this.typeRefGeneratorSupplier = typeRefGeneratorSupplier;
      return this;
    }

    public ReferenceCache build() {
      return new ReferenceCache(this);
    }
  }

  public static void set(DatabindContext aProvider, ReferenceCache aRefCache) {
    aProvider.setAttribute(KEY, aRefCache);
  }

  public static ReferenceCache get(DatabindContext aProvider) {
    return (ReferenceCache) aProvider.getAttribute(KEY);
  }
}
