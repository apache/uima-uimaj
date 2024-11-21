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
package org.apache.uima.cas.text;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;

/**
 * This is a different version of {@link AnnotationPredicates} which implements all predicates in
 * terms of the {@link #coveredBy(int, int, int, int)} method. This is slower than the normal
 * {@link AnnotationPredicates} but can be used to track down any potential issues with the
 * rationale behind the definition of the predicates and with the consistency of their definition.
 */
public final class AxiomaticAnnotationPredicates {
  public static boolean coveredBy(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return aYBegin <= aXBegin && aXEnd <= aYEnd;
  }

  public static boolean covering(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return coveredBy(aYBegin, aYEnd, aXBegin, aXEnd);
  }

  public static boolean colocated(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return coveredBy(aXBegin, aXEnd, aYBegin, aYEnd) && covering(aXBegin, aXEnd, aYBegin, aYEnd);
  }

  public static boolean overlapping(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return overlappingAtBegin(aXBegin, aXEnd, aYBegin, aYEnd)
            || overlappingAtEnd(aXBegin, aXEnd, aYBegin, aYEnd)
            || covering(aXBegin, aXEnd, aYBegin, aYEnd)
            || coveredBy(aXBegin, aXEnd, aYBegin, aYEnd);
  }

  public static boolean overlappingAtBegin(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return !coveredBy(aXBegin, aXBegin, aYBegin, aYEnd) && coveredBy(aXEnd, aXEnd, aYBegin, aYEnd)
            && !colocated(aXEnd, aXEnd, aYBegin, aYBegin);
  }

  public static boolean overlappingAtEnd(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return coveredBy(aXBegin, aXBegin, aYBegin, aYEnd) && !coveredBy(aXEnd, aXEnd, aYBegin, aYEnd)
            && !colocated(aXBegin, aXBegin, aYEnd, aYEnd);
  }

  @SuppressWarnings("unused")
  public static boolean following(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return coveredBy(aXBegin, aXEnd, aYEnd, MAX_VALUE);
  }

  @SuppressWarnings("unused")
  public static boolean preceding(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return coveredBy(aXBegin, aXEnd, MIN_VALUE, aYBegin);
  }

  @SuppressWarnings("unused")
  public static boolean beginningWith(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return colocated(aXBegin, aXBegin, aYBegin, aYBegin);
  }

  @SuppressWarnings("unused")
  public static boolean endingWith(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return colocated(aXEnd, aXEnd, aYEnd, aYEnd);
  }
}
