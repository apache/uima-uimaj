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
package org.apache.uima.fit.util;

import org.apache.uima.cas.text.AnnotationFS;

public final class AnnotationPredicates {
  private AnnotationPredicates() {
    // No instances
  }
  
  public static boolean coveredBy(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return aYBegin <= aXBegin && aXEnd <= aYEnd && (aXBegin == aYBegin || aXBegin != aYEnd);
  }

  public static boolean coveredBy(AnnotationFS aX, int aYBegin, int aYEnd) {
    int xBegin = aX.getBegin();
    return aYBegin <= xBegin && (xBegin == aYBegin || xBegin != aYEnd) && aX.getEnd() <= aYEnd;
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
    int xBegin = aX.getBegin();
    int yBegin = aY.getBegin();
    int yEnd = aY.getEnd();
    return yBegin <= xBegin && (xBegin == yBegin || xBegin != yEnd) && aX.getEnd() <= yEnd;
  }

  public static boolean covers(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return aXBegin <= aYBegin && aYEnd <= aXEnd && (aYBegin == aXBegin || aYBegin != aXEnd);
  }

  public static boolean covers(AnnotationFS aX, int aYBegin, int aYEnd) {
    int xBegin = aX.getBegin();
    int xEnd = aX.getEnd();
    return xBegin <= aYBegin && aYEnd <= xEnd && (aYBegin == xBegin || aYBegin != xEnd);
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
  public static boolean covers(AnnotationFS aX, AnnotationFS aY) {
    int xBegin = aX.getBegin();
    int xEnd = aX.getEnd();
    int yBegin = aY.getBegin();
    return xBegin <= yBegin && (yBegin == xBegin || yBegin != xEnd) && aY.getEnd() <= xEnd;
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

  public static boolean overlaps(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return aYBegin == aXBegin || (aXBegin < aYEnd && aYBegin < aXEnd);
  }

  public static boolean overlaps(AnnotationFS aX, int aYBegin, int aYEnd) {
    int xBegin = aX.getBegin();
    return aYBegin == xBegin || (xBegin < aYEnd && aYBegin < aX.getEnd());
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
  public static boolean overlaps(AnnotationFS aX, AnnotationFS aY) {
    int xBegin = aX.getBegin();
    int yBegin = aY.getBegin();
    return yBegin == xBegin || (xBegin < aY.getEnd() && yBegin < aX.getEnd());
  }

  public static boolean overlapsLeft(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return aXBegin < aYBegin && aYBegin < aXEnd && aXEnd < aYEnd;
  }

  public static boolean overlapsLeft(AnnotationFS aX, int aYBegin, int aYEnd) {
    int xEnd = aX.getEnd();
    return aYBegin < xEnd && xEnd < aYEnd && aX.getBegin() < aYBegin;
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
  public static boolean overlapsLeft(AnnotationFS aX, AnnotationFS aY) {
    int xEnd = aX.getEnd();
    int yBegin = aY.getBegin();
    return yBegin < xEnd && xEnd < aY.getEnd() && aX.getBegin() < yBegin;
  }

  public static boolean overlapsRight(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return aYBegin < aXBegin && aXBegin < aYEnd && aYEnd < aXEnd;
  }

  public static boolean overlapsRight(AnnotationFS aX, int aYBegin, int aYEnd) {
    int xBegin = aX.getBegin();
    return aYBegin < xBegin && xBegin < aYEnd && aYEnd < aX.getEnd();
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
  public static boolean overlapsRight(AnnotationFS aX, AnnotationFS aY) {
    int xBegin = aX.getBegin();
    int yEnd = aY.getEnd();
    return xBegin < yEnd && aY.getBegin() < xBegin && yEnd < aX.getEnd();
  }

  public static boolean rightOf(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return aXBegin >= aYEnd && aXBegin != aYBegin;
  }

  public static boolean rightOf(AnnotationFS aX, int aYBegin, int aYEnd) {
    int xBegin = aX.getBegin();
    return xBegin >= aYEnd && xBegin != aYBegin;
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
  public static boolean rightOf(AnnotationFS aX, AnnotationFS aY) {
    int xBegin = aX.getBegin();
    return xBegin >= aY.getEnd() && xBegin != aY.getBegin();
  }

  public static boolean leftOf(int aXBegin, int aXEnd, int aYBegin, int aYEnd) {
    return aYBegin >= aXEnd && aXBegin != aYBegin;
  }

  public static boolean leftOf(AnnotationFS aX, int aYBegin, int aYEnd) {
    return aYBegin >= aX.getEnd() && aX.getBegin() != aYBegin;
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
  public static boolean leftOf(AnnotationFS aX, AnnotationFS aY) {
    return aY.getBegin() >= aX.getEnd() && aX.getBegin() != aY.getBegin();
  }
}
