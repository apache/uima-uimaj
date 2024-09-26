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
package org.apache.uima.fit.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.type.Sentence;
import org.apache.uima.fit.type.Token;

public final class CasInitializationUtils {
  private CasInitializationUtils() {
    // No instances
  }

  public static void initRandomCas(CAS cas, int typeCount, int annotationCount,
          int maxAnnotationLength, int approxDocLength, long seed) {
    cas.reset();
    Random rnd = new Random(seed);
    List<Type> types = new ArrayList<Type>();
    types.add(cas.getTypeSystem().getType(Token.class.getName()));
    types.add(cas.getTypeSystem().getType(Sentence.class.getName()));

    // Shuffle the types
    for (int n = 0; n < typeCount; n++) {
      Type t = types.remove(rnd.nextInt(types.size()));
      types.add(t);
    }

    // Randomly generate annotations
    for (int n = 0; n < annotationCount; n++) {
      for (Type t : types) {
        int begin = rnd.nextInt(approxDocLength);
        int end = begin + rnd.nextInt(maxAnnotationLength);
        cas.addFsToIndexes(cas.createAnnotation(t, begin, end));
      }
    }

    try {
      cas.getJCas();
    } catch (CASException e) {
      throw new RuntimeException(e);
    }
  }

}
