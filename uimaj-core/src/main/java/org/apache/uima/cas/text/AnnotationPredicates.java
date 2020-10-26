package org.apache.uima.cas.text;

public interface AnnotationPredicates {

  boolean coveredBy(int aXBegin, int aXEnd, int aYBegin, int aYEnd);

  boolean coveredBy(AnnotationFS aX, int aYBegin, int aYEnd);

  /**
   * Y is starting before or at the same position as A and ends after or at the same position as X.
   * 
   * @param aX
   *          X
   * @param aY
   *          Y
   * @return whether X is covered by Y.
   */
  boolean coveredBy(AnnotationFS aX, AnnotationFS aY);

  boolean covers(int aXBegin, int aXEnd, int aYBegin, int aYEnd);

  boolean covers(AnnotationFS aX, int aYBegin, int aYEnd);

  /**
   * X is starting before or at the same position as Y and ends after or at the same position as Y.
   * 
   * @param aX
   *          X
   * @param aY
   *          Y
   * @return whether X is covering Y.
   */
  boolean covers(AnnotationFS aX, AnnotationFS aY);

  boolean colocated(int aXBegin, int aXEnd, int aYBegin, int aYEnd);

  boolean colocated(AnnotationFS aX, int aYBegin, int aYEnd);

  /**
   * X starts and ends at the same position as Y.
   * 
   * @param aX
   *          X
   * @param aY
   *          Y
   * @return whether X is at the same location as Y.
   */
  boolean colocated(AnnotationFS aX, AnnotationFS aY);

  boolean overlaps(int aXBegin, int aXEnd, int aYBegin, int aYEnd);

  boolean overlaps(AnnotationFS aX, int aYBegin, int aYEnd);

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
  boolean overlaps(AnnotationFS aX, AnnotationFS aY);

  boolean overlapsLeft(int aXBegin, int aXEnd, int aYBegin, int aYEnd);

  boolean overlapsLeft(AnnotationFS aX, int aYBegin, int aYEnd);

  /**
   * X is starting before or at the same position as Y and ends before Y ends.
   * 
   * @param aX
   *          X
   * @param aY
   *          Y
   * @return whether X overlaps Y on the left.
   */
  boolean overlapsLeft(AnnotationFS aX, AnnotationFS aY);

  boolean overlapsRight(int aXBegin, int aXEnd, int aYBegin, int aYEnd);

  boolean overlapsRight(AnnotationFS aX, int aYBegin, int aYEnd);

  /**
   * X is starting after Y starts and ends after or at the same position as Y.
   * 
   * @param aX
   *          X
   * @param aY
   *          Y
   * @return whether X overlaps Y on the right.
   */
  boolean overlapsRight(AnnotationFS aX, AnnotationFS aY);

  boolean rightOf(int aXBegin, int aXEnd, int aYBegin, int aYEnd);

  boolean rightOf(AnnotationFS aX, int aYBegin, int aYEnd);

  /**
   * X starts at or after the position that Y ends.
   * 
   * @param aX
   *          X
   * @param aY
   *          Y
   * @return whether X is right of Y.
   */
  boolean rightOf(AnnotationFS aX, AnnotationFS aY);

  boolean leftOf(int aXBegin, int aXEnd, int aYBegin, int aYEnd);

  boolean leftOf(AnnotationFS aX, int aYBegin, int aYEnd);

  /**
   * X ends before or at the position that Y starts.
   * 
   * @param aX
   *          X
   * @param aY
   *          Y
   * @return whether X left of Y.
   */
  boolean leftOf(AnnotationFS aX, AnnotationFS aY);

}