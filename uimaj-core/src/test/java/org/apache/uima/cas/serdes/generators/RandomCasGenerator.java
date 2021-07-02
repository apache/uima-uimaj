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

public class RandomCasGenerator {
  private Random rnd = new Random();
  private int size;
  private int minimumWidth;
  private boolean writeLog = false;
  private int typeCount;

  public RandomCasGenerator randomSeed(long aRandomSeed) {
    rnd = new Random(aRandomSeed);
    return this;
  }

  public RandomCasGenerator randomGenerator(Random aRandom) {
    rnd = aRandom;
    return this;
  }

  public RandomCasGenerator typeCount(int aTypeCount) {
    typeCount = aTypeCount;
    return this;
  }

  public RandomCasGenerator annotationsToGenerate(int aSize) {
    size = aSize;
    return this;
  }

  public RandomCasGenerator minimumAnnotationLength(int aMinimumWidth) {
    minimumWidth = aMinimumWidth;
    return this;
  }

  public RandomCasGenerator logAnnotationCreation(boolean aLogAnnotationCreation) {
    writeLog = aLogAnnotationCreation;
    return this;
  }

  public TypeSystemDescription generateRandomTypeSystem() {
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

  public CAS generateRandomCas(TypeSystemDescription aTsd) throws ResourceInitializationException {
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
}
