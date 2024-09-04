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

package org.apache.uima.cas.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Implements a low level ambiguous or unambiguous iterator over some type T which doesn't need to
 * be a subtype of Annotation. - This iterator skips types which are not Annotation or a subtype of
 * Annotation.
 *
 * @param <T>
 *          the type this iterator is over (including subtypes)
 */
public class LLUnambiguousIteratorImpl<T extends FeatureStructure>
        extends FsIterator_subtypes_snapshot<T> {

  public LLUnambiguousIteratorImpl(LowLevelIterator<T> it) {
    super((T[]) createItemsArray((LowLevelIterator<FeatureStructure>) it), it.ll_getIndex(),
            IS_ORDERED, it.getComparator());
  }

  // this is static because can't have instance method call before super call in constructor
  private static Annotation[] createItemsArray(LowLevelIterator<FeatureStructure> it) {
    List<Annotation> items = new ArrayList<>();
    int lastSeenEnd = 0;
    it.moveToFirst();
    // Iterate over the input iterator.
    while (it.isValid()) {
      FeatureStructure fs = it.nextNvc();
      if (!(fs instanceof Annotation)) {
        continue; // skip until get an annotation
      }

      Annotation annot = (Annotation) fs;
      if (annot.getBegin() >= lastSeenEnd) {
        items.add(annot);
        lastSeenEnd = annot.getEnd();
      }
    }

    return items.toArray(new Annotation[items.size()]);
  }

}
