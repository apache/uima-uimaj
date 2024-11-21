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

public final class AnnotationPredicates {
  public static boolean coveredBy(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return aYBegin <= aXBegin && aXEnd <= aYEnd;
  }

  public static boolean coveredBy(AnnotationFS aX, int aYBegin, int aYEnd) {
    return aYBegin <= aX.getBegin() && aX.getEnd() <= aYEnd;
  }

  /**
   * Y is starting before or at the same position as A and ends after or at the same position as X.
   * 
   * @param aX
   *          X
   * @param aY
   *          Y
   * @return whether X is covered by Y.
   */
  public static boolean coveredBy(AnnotationFS aX, AnnotationFS aY) {
    return aY.getBegin() <= aX.getBegin() && aX.getEnd() <= aY.getEnd();
  }

  public static boolean covering(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return aXBegin <= aYBegin && aYEnd <= aXEnd;
  }

  public static boolean covering(AnnotationFS aX, int aYBegin, int aYEnd) {
    return aX.getBegin() <= aYBegin && aYEnd <= aX.getEnd();
  }

  /**
   * X is starting before or at the same position as Y and ends after or at the same position as Y.
   * 
   * @param aX
   *          X
   * @param aY
   *          Y
   * @return whether X is covering Y.
   */
  public static boolean covering(AnnotationFS aX, AnnotationFS aY) {
    return aX.getBegin() <= aY.getBegin() && aY.getEnd() <= aX.getEnd();
  }

  public static boolean colocated(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return aXBegin == aYBegin && aXEnd == aYEnd;
  }

  public static boolean colocated(AnnotationFS aX, int aYBegin, int aYEnd) {
    return aX.getBegin() == aYBegin && aX.getEnd() == aYEnd;
  }

  /**
   * X starts and ends at the same position as Y.
   * 
   * @param aX
   *          X
   * @param aY
   *          Y
   * @return whether X is at the same location as Y.
   */
  public static boolean colocated(AnnotationFS aX, AnnotationFS aY) {
    return aX.getBegin() == aY.getBegin() && aX.getEnd() == aY.getEnd();
  }

  public static boolean overlapping(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return aYBegin == aXBegin || aYEnd == aXEnd || (aXBegin < aYEnd && aYBegin < aXEnd);
  }

  public static boolean overlapping(AnnotationFS aX, int aYBegin, int aYEnd) {
    int xBegin = aX.getBegin();
    int xEnd = aX.getEnd();
    return aYBegin == xBegin || aYEnd == xEnd || (xBegin < aYEnd && aYBegin < xEnd);
  }

  /**
   * The intersection of the spans X and Y is non-empty. If either X or Y have a zero-width, then
   * the intersection is considered to be non-empty if the begin of X is either within Y or the same
   * as the begin of Y - and vice versa.
   * 
   * @param aX
   *          X
   * @param aY
   *          Y
   * @return whether X overlaps with Y in any way.
   */
  public static boolean overlapping(AnnotationFS aX, AnnotationFS aY) {
    int xBegin = aX.getBegin();
    int xEnd = aX.getEnd();
    int yBegin = aY.getBegin();
    int yEnd = aY.getEnd();
    return yBegin == xBegin || yEnd == xEnd || (xBegin < yEnd && yBegin < xEnd);
  }

  public static boolean overlappingAtBegin(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return aXBegin < aYBegin && aYBegin < aXEnd && aXEnd <= aYEnd;
  }

  public static boolean overlappingAtBegin(AnnotationFS aX, int aYBegin, int aYEnd) {
    int xEnd = aX.getEnd();
    return aYBegin < xEnd && xEnd <= aYEnd && aX.getBegin() < aYBegin;
  }

  /**
   * X is starting before or at the same position as Y and ends before Y ends.
   * 
   * @param aX
   *          X
   * @param aY
   *          Y
   * @return whether X overlaps Y on the left.
   */
  public static boolean overlappingAtBegin(AnnotationFS aX, AnnotationFS aY) {
    int xEnd = aX.getEnd();
    int yBegin = aY.getBegin();
    return yBegin < xEnd && xEnd <= aY.getEnd() && aX.getBegin() < yBegin;
  }

  public static boolean overlappingAtEnd(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return aYBegin <= aXBegin && aXBegin < aYEnd && aYEnd < aXEnd;
  }

  public static boolean overlappingAtEnd(AnnotationFS aX, int aYBegin, int aYEnd) {
    int xBegin = aX.getBegin();
    return aYBegin <= xBegin && xBegin < aYEnd && aYEnd < aX.getEnd();
  }

  /**
   * X is starting after Y starts and ends after or at the same position as Y.
   * 
   * @param aX
   *          X
   * @param aY
   *          Y
   * @return whether X overlaps Y on the right.
   */
  public static boolean overlappingAtEnd(AnnotationFS aX, AnnotationFS aY) {
    int xBegin = aX.getBegin();
    int yEnd = aY.getEnd();
    return xBegin < yEnd && aY.getBegin() <= xBegin && yEnd < aX.getEnd();
  }

  @SuppressWarnings("unused")
  public static boolean following(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return aXBegin >= aYEnd;
  }

  @SuppressWarnings("unused")
  public static boolean following(AnnotationFS aX, int aYBegin, int aYEnd) {
    return aX.getBegin() >= aYEnd;
  }

  /**
   * X starts at or after the position that Y ends.
   * 
   * @param aX
   *          X
   * @param aY
   *          Y
   * @return whether X is right of Y.
   */
  public static boolean following(AnnotationFS aX, AnnotationFS aY) {
    return aX.getBegin() >= aY.getEnd();
  }

  @SuppressWarnings("unused")
  public static boolean preceding(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return aYBegin >= aXEnd;
  }

  @SuppressWarnings("unused")
  public static boolean preceding(AnnotationFS aX, int aYBegin, int aYEnd) {
    return aYBegin >= aX.getEnd();
  }

  /**
   * X ends before or at the position that Y starts.
   * 
   * @param aX
   *          X
   * @param aY
   *          Y
   * @return whether X left of Y.
   */
  public static boolean preceding(AnnotationFS aX, AnnotationFS aY) {
    return aY.getBegin() >= aX.getEnd();
  }

  @SuppressWarnings("unused")
  public static boolean beginningWith(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return aXBegin == aYBegin;
  }

  @SuppressWarnings("unused")
  public static boolean beginningWith(AnnotationFS aX, int aYBegin, int aYEnd) {
    return aX.getBegin() == aYBegin;
  }

  public static boolean beginningWith(AnnotationFS aX, AnnotationFS aY) {
    return aX.getBegin() == aY.getBegin();
  }

  @SuppressWarnings("unused")
  public static boolean endingWith(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return aXEnd == aYEnd;
  }

  @SuppressWarnings("unused")
  public static boolean endingWith(AnnotationFS aX, int aYBegin, int aYEnd) {
    return aX.getEnd() == aYEnd;
  }

  public static boolean endingWith(AnnotationFS aX, AnnotationFS aY) {
    return aX.getEnd() == aY.getEnd();
  }
}
