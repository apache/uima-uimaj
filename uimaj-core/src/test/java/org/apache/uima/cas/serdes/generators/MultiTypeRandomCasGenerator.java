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
package org.apache.uima.cas.serdes.generators;

import static java.lang.System.identityHashCode;
import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;

public class MultiTypeRandomCasGenerator implements CasGenerator {
  private final Random rnd;
  private final int size;
  private final int minimumWidth;
  private final boolean writeLog;
  private final int typeCount;

  private MultiTypeRandomCasGenerator(Builder builder) {
    this.rnd = builder.randomGenerator;
    this.size = builder.size;
    this.minimumWidth = builder.minimumWidth;
    this.writeLog = builder.writeLog;
    this.typeCount = builder.typeCount;
  }

  @Override
  public TypeSystemDescription generateTypeSystem() {
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    Map<String, Type> types = new LinkedHashMap<>();

    if (writeLog) {
      System.out.println();
    }

    for (int ti = 0; ti < typeCount; ti++) {
      String typeName = "test.Type" + (ti + 1);
      TypeDescription newType;
      if (rnd.nextInt() % 2 == 0 || types.size() == 0) {
        newType = tsd.addType(typeName, "", CAS.TYPE_NAME_ANNOTATION);
      } else {
        newType = tsd.addType(typeName, "",
                new ArrayList<>(types.keySet()).get(rnd.nextInt(types.size())));
      }

      if (writeLog) {
        System.out.printf("tsd.addType(\"%s\", \"\", \"%s\");%n", newType.getName(),
                newType.getSupertypeName());
      }

      types.put(typeName, null);
    }

    return tsd;
  }

  @Override
  public CAS generateCas(TypeSystemDescription aTsd) throws ResourceInitializationException {
    CAS cas = CasCreationUtils.createCas(aTsd, null, null, null);
    cas.reset();

    List<Type> types = new ArrayList<>();
    for (TypeDescription td : aTsd.getTypes()) {
      types.add(cas.getTypeSystem().getType(td.getName()));
    }

    // Shuffle the types
    for (int n = 0; n < 10; n++) {
      Type t = types.remove(rnd.nextInt(types.size()));
      types.add(t);
    }

    if (writeLog) {
      System.out.println();
    }

    // Randomly generate annotations
    for (int n = 0; n < size; n++) {
      for (Type t : types) {
        int begin = rnd.nextInt(100);
        int end = begin + rnd.nextInt(30) + minimumWidth;
        AnnotationFS ann = cas.createAnnotation(t, begin, end);
        if (writeLog) {
          System.out.printf("cas.createAnnotation(%s, %d, %d)\t[%d]%n",
                  t.getShortName().toLowerCase(), begin, end, identityHashCode(ann));
        }
        cas.addFsToIndexes(ann);
      }
    }

    return cas;
  }

  /**
   * Creates builder to build {@link MultiTypeRandomCasGenerator}.
   * 
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link MultiTypeRandomCasGenerator}.
   */
  public static final class Builder {
    private Random randomGenerator;
    private int size;
    private int minimumWidth;
    private boolean writeLog;
    private int typeCount;

    private Builder() {
    }

    public Builder withRandomGenerator(Random rnd) {
      this.randomGenerator = rnd;
      return this;
    }

    public Builder withSize(int size) {
      this.size = size;
      return this;
    }

    public Builder withMinimumAnnotationLength(int minimumWidth) {
      this.minimumWidth = minimumWidth;
      return this;
    }

    public Builder withAnnotationLogOutput(boolean writeLog) {
      this.writeLog = writeLog;
      return this;
    }

    public Builder withTypeCount(int typeCount) {
      this.typeCount = typeCount;
      return this;
    }

    public MultiTypeRandomCasGenerator build() {
      if (randomGenerator == null) {
        randomGenerator = new Random();
      }

      return new MultiTypeRandomCasGenerator(this);
    }
  }
}
