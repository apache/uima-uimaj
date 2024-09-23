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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.Subiterator;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Utility methods for convenient access to the {@link JCas}.
 * 
 */
public final class JCasUtil {
  private JCasUtil() {
    // No instances
  }

  /**
   * Convenience method to iterator over all annotations of a given type occurring within the scope
   * of a provided annotation (sub-iteration).
   * 
   * @param <T>
   *          the iteration type.
   * @param jCas
   *          a JCas.
   * @param container
   *          the containing annotation.
   * @param type
   *          the type.
   * @param ambiguous
   *          If set to <code>false</code>, resulting iterator will be unambiguous.
   * @param strict
   *          Controls if annotations that overlap to the right are considered in or out.
   * @return A sub-iterator iterable.
   * @see AnnotationIndex#subiterator(AnnotationFS, boolean, boolean)
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends Annotation> Iterable<T> subiterate(final JCas jCas, final Class<T> type,
          final AnnotationFS container, final boolean ambiguous, final boolean strict) {
    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return JCasUtil.iterator(container, type, ambiguous, strict);
      }
    };
  }

  /**
   * Get an iterator over the given feature structure type.
   * 
   * @param <T>
   *          the JCas type.
   * @param jCas
   *          a JCas.
   * @param type
   *          a type.
   * @return a return value.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   * @deprecated Use {@code jcas.select(type).iterator()}
   */
  @Deprecated
  public static <T extends TOP> Iterator<T> iterator(JCas jCas, Class<T> type) {
    return jCas.select(type).iterator();
  }

  /**
   * Convenience method to get a sub-iterator for the specified type.
   * 
   * @param <T>
   *          the iteration type.
   * @param container
   *          the containing annotation.
   * @param type
   *          the type.
   * @param ambiguous
   *          If set to <code>false</code>, resulting iterator will be unambiguous.
   * @param strict
   *          Controls if annotations that overlap to the right are considered in or out.
   * @return A sub-iterator.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  @SuppressWarnings("unchecked")
  public static <T extends AnnotationFS> Iterator<T> iterator(AnnotationFS container, Class<T> type,
          boolean ambiguous, boolean strict) {
    CAS cas = container.getCAS();
    return ((AnnotationIndex<T>) cas.getAnnotationIndex(CasUtil.getType(cas, type)))
            .subiterator(container, ambiguous, strict);
  }

  /**
   * Get the CAS type for the given JCas wrapper class type.
   * 
   * @param jCas
   *          the JCas containing the type system.
   * @param type
   *          the JCas wrapper class type.
   * @return the CAS type.
   */
  public static Type getType(JCas jCas, Class<?> type) {
    return CasUtil.getType(jCas.getCas(), type);
  }

  /**
   * Get the CAS type for the given JCas wrapper class type making sure it inherits from
   * {@link Annotation}.
   * 
   * @param jCas
   *          the JCas containing the type system.
   * @param type
   *          the JCas wrapper class type.
   * @return the CAS type.
   */
  public static Type getAnnotationType(JCas jCas, Class<?> type) {
    return CasUtil.getAnnotationType(jCas.getCas(), type);
  }

  /**
   * Convenience method select all feature structure from the given type from an array.
   * 
   * @param <T>
   *          the JCas type.
   * @param array
   *          a feature structure array.
   * @param type
   *          the type.
   * @return A collection of the selected type.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends TOP> List<T> select(final FSArray array, final Class<T> type) {
    return cast(CasUtil.selectFS(array, CasUtil.getType(array.getCAS(), type.getName())));
  }

  /**
   * Convenience method select all feature structure from the given type from a list.
   * 
   * @param <T>
   *          the JCas type.
   * @param list
   *          a feature structure list.
   * @param type
   *          the type.
   * @return A collection of the selected type.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends TOP> List<T> select(final FSList list, final Class<T> type) {
    return cast(FSCollectionFactory.create(list, CasUtil.getType(list.getCAS(), type.getName())));
  }

  /**
   * Convenience method to iterator over all features structures of a given type.
   * 
   * @param <T>
   *          the iteration type.
   * @param jCas
   *          the JCas containing the type system.
   * @param type
   *          the type.
   * @return A collection of the selected type.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends TOP> Collection<T> select(final JCas jCas, final Class<T> type) {
    return cast(CasUtil.selectFS(jCas.getCas(), getType(jCas, type)));
  }

  /**
   * Get all annotations of the given type at the specified offsets.
   * 
   * @param <T>
   *          the type of annotations to fetch.
   * @param jCas
   *          the CAS containing the annotations.
   * @param type
   *          the type of annotations to fetch.
   * @param aBegin
   *          the begin offset.
   * @param aEnd
   *          the end offset.
   * @return the annotations at the specified offsets.
   */
  public static <T extends Annotation> List<T> selectAt(final JCas jCas, final Class<T> type,
          int aBegin, int aEnd) {
    return cast(CasUtil.selectAt(jCas.getCas(), getType(jCas, type), aBegin, aEnd));
  }

  /**
   * Get a single annotations of the given type at the specified offsets.
   * 
   * @param <T>
   *          the type of annotations to fetch.
   * @param jCas
   *          the CAS containing the annotations.
   * @param type
   *          the type of annotations to fetch.
   * @param aBegin
   *          the begin offset.
   * @param aEnd
   *          the end offset.
   * @return the annotation at the specified offsets.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Annotation> T selectSingleAt(final JCas jCas, final Class<T> type,
          int aBegin, int aEnd) {
    return (T) CasUtil.selectSingleAt(jCas.getCas(), getType(jCas, type), aBegin, aEnd);
  }

  /**
   * Convenience method to iterator over all features structures.
   * 
   * @param jCas
   *          the JCas containing the type system. the type.
   * @return A collection of the selected type.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static Collection<TOP> selectAll(final JCas jCas) {
    return select(jCas, TOP.class);
  }

  /**
   * Get a list of annotations of the given annotation type located between two annotations. Does
   * not use subiterators and does not respect type priorities. Zero-width annotations what lie on
   * the borders are included in the result, e.g. if the boundary annotations are [1..2] and [2..3]
   * then an annotation [2..2] is returned. If there is a non-zero overlap between the boundary
   * annotations, the result is empty. The method properly handles cases where the second boundary
   * annotations occurs before the first boundary annotation by switching their roles.
   * 
   * @param <T>
   *          the JCas type.
   * @param type
   *          a UIMA type.
   * @param ann1
   *          the first boundary annotation.
   * @param ann2
   *          the second boundary annotation.
   * @return a return value.
   * @see Subiterator
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends Annotation> List<T> selectBetween(final Class<T> type, AnnotationFS ann1,
          AnnotationFS ann2) {
    return cast(CasUtil.selectBetween(CasUtil.getType(ann1.getCAS(), type), ann1, ann2));
  }

  /**
   * Get a list of annotations of the given annotation type located between two annotations. Does
   * not use subiterators and does not respect type priorities. Zero-width annotations what lie on
   * the borders are included in the result, e.g. if the boundary annotations are [1..2] and [2..3]
   * then an annotation [2..2] is returned. If there is a non-zero overlap between the boundary
   * annotations, the result is empty. The method properly handles cases where the second boundary
   * annotations occurs before the first boundary annotation by switching their roles.
   * 
   * @param <T>
   *          the JCas type.
   * @param jCas
   *          a JCas containing the annotation.
   * @param type
   *          a UIMA type.
   * @param ann1
   *          the first boundary annotation.
   * @param ann2
   *          the second boundary annotation.
   * @return a return value.
   * @see Subiterator
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends Annotation> List<T> selectBetween(JCas jCas, final Class<T> type,
          AnnotationFS ann1, AnnotationFS ann2) {
    return cast(CasUtil.selectBetween(jCas.getCas(), getType(jCas, type), ann1, ann2));
  }

  /**
   * Get a list of annotations of the given annotation type constrained by a 'covering' annotation.
   * Iterates over all annotations of the given type to find the covered annotations. Does not use
   * subiterators.
   * <p>
   * The covering annotation is never returned itself, even if it is of the queried-for type or a
   * subtype of that type.
   * <p>
   * The method only returns properly covered annotations, that is annotations where the begin/end
   * offsets are equal to the 'covering' annotation or where both the begin/end are included in the
   * span of the 'covering' annotation. Partially overlapping annotations are not returned.
   * 
   * @param <T>
   *          the JCas type.
   * @param type
   *          a UIMA type.
   * @param coveringAnnotation
   *          the covering annotation.
   * @return a return value.
   * @see Subiterator
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends AnnotationFS> List<T> selectCovered(Class<T> type,
          AnnotationFS coveringAnnotation) {
    return cast(CasUtil.selectCovered(CasUtil.getType(coveringAnnotation.getCAS(), type),
            coveringAnnotation));
  }

  /**
   * Get a list of annotations of the given annotation type constrained by a 'covering' annotation.
   * Iterates over all annotations of the given type to find the covered annotations. Does not use
   * subiterators.
   * <p>
   * The covering annotation is never returned itself, even if it is of the queried-for type or a
   * subtype of that type.
   * <p>
   * The method only returns properly covered annotations, that is annotations where the begin/end
   * offsets are equal to the 'covering' annotation or where both the begin/end are included in the
   * span of the 'covering' annotation. Partially overlapping annotations are not returned.
   * 
   * @param <T>
   *          the JCas type.
   * @param jCas
   *          a JCas containing the annotation.
   * @param type
   *          a UIMA type.
   * @param coveringAnnotation
   *          the covering annotation.
   * @return a return value.
   * @see Subiterator
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends Annotation> List<T> selectCovered(JCas jCas, final Class<T> type,
          AnnotationFS coveringAnnotation) {
    return cast(CasUtil.selectCovered(jCas.getCas(), getType(jCas, type), coveringAnnotation));
  }

  /**
   * Get a list of annotations of the given annotation type constrained by a 'covering' annotation.
   * Iterates over all annotations of the given type to find the covered annotations. Does not use
   * subiterators.
   * <p>
   * <b>Note:</b> this is significantly slower than using
   * {@link #selectCovered(JCas, Class, AnnotationFS)}. It is possible to use
   * {@code  selectCovered(jCas, cls, new Annotation(jCas, int, int))}, but that will allocate
   * memory in the jCas for the new annotation. If you do that repeatedly many times, memory may
   * fill up.
   * <p>
   * The method only returns properly covered annotations, that is annotations where the begin/end
   * offsets are equal to the given begin/end or where both the begin/end are included in the span
   * of the given span. Partially overlapping annotations are not returned.
   * 
   * @param <T>
   *          the JCas type.
   * @param jCas
   *          a JCas containing the annotation.
   * @param type
   *          a UIMA type.
   * @param begin
   *          begin offset.
   * @param end
   *          end offset.
   * @return a return value.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends Annotation> List<T> selectCovered(JCas jCas, final Class<T> type,
          int begin, int end) {
    return cast(CasUtil.selectCovered(jCas.getCas(), getType(jCas, type), begin, end));
  }

  /**
   * Get a list of annotations of the given annotation type constraint by a certain annotation.
   * Iterates over all annotations to find the covering annotations.
   * <p>
   * The method only returns properly covering annotations, that is annotations where the begin/end
   * offsets are equal to the begin/end of the given annotation or where given 'covered' annotation
   * is properly contained within the span of the 'covering' annotation. Partially overlapping
   * annotations are not returned.
   * 
   * <p>
   * <b>Note:</b> this is <b>REALLY SLOW!</b> You don't want to use this. Instead, consider using
   * {@link #indexCovering(JCas, Class, Class)} or a {@link ContainmentIndex}.
   * 
   * @param <T>
   *          the JCas type.
   * @param type
   *          a UIMA type.
   * @param coveredAnnotation
   *          the covered annotation.
   * @return a return value.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends Annotation> List<T> selectCovering(Class<T> type,
          AnnotationFS coveredAnnotation) {

    return cast(CasUtil.selectCovering(CasUtil.getType(coveredAnnotation.getCAS(), type),
            coveredAnnotation));
  }

  /**
   * Get a list of annotations of the given annotation type constraint by a certain annotation.
   * Iterates over all annotations to find the covering annotations.
   * <p>
   * The method only returns properly covering annotations, that is annotations where the begin/end
   * offsets are equal to the begin/end of the given annotation or where given 'covered' annotation
   * is properly contained within the span of the 'covering' annotation. Partially overlapping
   * annotations are not returned.
   * 
   * <p>
   * <b>Note:</b> this is <b>REALLY SLOW!</b> You don't want to use this. Instead, consider using
   * {@link #indexCovering(JCas, Class, Class)} or a {@link ContainmentIndex}.
   * 
   * @param <T>
   *          the JCas type.
   * @param jCas
   *          a CAS.
   * @param type
   *          a UIMA type.
   * @param coveredAnnotation
   *          the covered annotation.
   * @return a return value.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends Annotation> List<T> selectCovering(JCas jCas, Class<T> type,
          AnnotationFS coveredAnnotation) {

    return cast(CasUtil.selectCovering(jCas.getCas(), getType(jCas, type), coveredAnnotation));
  }

  /**
   * Get a list of annotations of the given annotation type constraint by a certain annotation.
   * Iterates over all annotations to find the covering annotations.
   * <p>
   * The method only returns properly covering annotations, that is annotations where the begin/end
   * offsets are equal to the given begin/end to or where given span is properly contained within
   * the span of the 'covering' annotation. Partially overlapping annotations are not returned.
   * 
   * <p>
   * <b>Note:</b> this is <b>REALLY SLOW!</b> You don't want to use this. Instead, consider using
   * {@link #indexCovering(JCas, Class, Class)} or a {@link ContainmentIndex}.
   * 
   * @param <T>
   *          the JCas type.
   * @param jCas
   *          a CAS.
   * @param type
   *          a UIMA type.
   * @param begin
   *          begin offset.
   * @param end
   *          end offset.
   * @return a return value.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends Annotation> List<T> selectCovering(JCas jCas, Class<T> type, int begin,
          int end) {

    return cast(CasUtil.selectCovering(jCas.getCas(), getType(jCas, type), begin, end));
  }

  /**
   * Create an index for quickly lookup up the annotations covering a particular annotation. This is
   * preferable to using {@link #selectCovering(JCas, Class, int, int)} because the overhead of
   * scanning the CAS occurs only when the index is build. Subsequent lookups to the index are fast.
   * <p>
   * The method only returns properly covering annotations, that is annotations where the begin/end
   * offsets are equal to the begin/end of the given annotation or where given 'covered' annotation
   * is properly contained within the span of the 'covering' annotation. Partially overlapping
   * annotations are not returned.
   * <p>
   * When querying for the annotations covering a given annotation, the given annotation itself is
   * never returned, even if it is of the queried type.
   * 
   * @param <T>
   *          the covered JCAs type.
   * @param <S>
   *          the covering JCas type.
   * @param jCas
   *          a JCas.
   * @param type
   *          type to create the index for - this is used in lookups.
   * @param coveringType
   *          type of covering annotations.
   * @return the index.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends Annotation, S extends Annotation> Map<T, List<S>> indexCovering(
          JCas jCas, Class<? extends T> type, Class<? extends S> coveringType) {
    return cast(
            CasUtil.indexCovering(jCas.getCas(), getType(jCas, type), getType(jCas, coveringType)));
  }

  /**
   * Create an index for quickly lookup up the annotations covered by a particular annotation. This
   * is preferable to using {@link #selectCovered(JCas, Class, int, int)} because the overhead of
   * scanning the CAS occurs only when the index is build. Subsequent lookups to the index are fast.
   * The order of entries in the map is not defined. However, lists of covered annotations in the
   * map are guaranteed to be in the same order as in the UIMA default annotation index.
   * <p>
   * The method only returns properly covered annotations, that is annotations where the begin/end
   * offsets are equal to the 'covering' annotation or where both the begin/end are included in the
   * span of the 'covering' annotation. Partially overlapping annotations are not returned.
   * <p>
   * When querying for the annotations covered by a given annotation, the given annotation itself is
   * never returned, even if it is of the queried type.
   * 
   * @param <T>
   *          the covering JCas type.
   * @param <S>
   *          the covered JCas type.
   * @param jCas
   *          a JCas.
   * @param type
   *          type to create the index for - this is used in lookups.
   * @param coveredType
   *          type of covered annotations.
   * @return the index.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends Annotation, S extends Annotation> Map<T, List<S>> indexCovered(JCas jCas,
          Class<? extends T> type, Class<? extends S> coveredType) {
    return cast(
            CasUtil.indexCovered(jCas.getCas(), getType(jCas, type), getType(jCas, coveredType)));
  }

  /**
   * Get a list of annotations of the given annotation type overlapping the given annotation. Does
   * not use subiterators and does not respect type prioritites.
   * 
   * @param aType
   *          a UIMA type.
   * @param aBoundaryAnnotation
   *          the covering annotation.
   * @return a list of overlapping annotations.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends Annotation> List<T> selectOverlapping(Class<T> aType,
          AnnotationFS aBoundaryAnnotation) {
    return cast(CasUtil.selectOverlapping(getType(aBoundaryAnnotation.getJCas(), aType),
            aBoundaryAnnotation));
  }

  /**
   * Get a list of annotations of the given annotation type overlapping the given annotation. Does
   * not use subiterators and does not respect type prioritites.
   * 
   * @param aJCas
   *          a CAS.
   * @param aType
   *          a UIMA type.
   * @param aBoundaryAnnotation
   *          the covering annotation.
   * @return a list of overlapping annotations.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends Annotation> List<T> selectOverlapping(JCas aJCas, Class<T> aType,
          AnnotationFS aBoundaryAnnotation) {
    return cast(
            CasUtil.selectOverlapping(aJCas.getCas(), getType(aJCas, aType), aBoundaryAnnotation));
  }

  /**
   * Get a list of annotations of the given annotation type overlapping the given span. Does not use
   * subiterators and does not respect type prioritites.
   * 
   * @param aJCas
   *          a CAS.
   * @param aType
   *          a UIMA type.
   * @param aBegin
   *          begin offset.
   * @param aEnd
   *          end offset.
   * @return a list of overlapping annotations.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends Annotation> List<T> selectOverlapping(JCas aJCas, Class<T> aType,
          int aBegin, int aEnd) {
    return cast(CasUtil.selectOverlapping(aJCas.getCas(), getType(aJCas, aType), aBegin, aEnd));
  }

  /**
   * Check if the given annotation contains any annotation of the given type.
   * 
   * @param jCas
   *          a JCas containing the annotation.
   * @param coveringAnnotation
   *          the covering annotation.
   * @param type
   *          a UIMA type.
   * @return if an annotation of the given type is present.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static boolean contains(JCas jCas, AnnotationFS coveringAnnotation,
          Class<? extends Annotation> type) {
    return selectCovered(jCas, type, coveringAnnotation).size() > 0;
  }

  /**
   * This method exists simply as a convenience method for unit testing. It is not very efficient
   * and should not, in general be used outside the context of unit testing.
   * <p>
   * It is intentional that this method only allows annotation types. The CAS indexing mechanisms
   * are not well defined for non-annotation types. There are no reliably, built-in indexes for
   * non-annotation types.
   * 
   * @param <T>
   *          JCas wrapper type.
   * @param jCas
   *          a JCas containing the annotation.
   * @param cls
   *          a UIMA type.
   * @param index
   *          this can be either positive (0 corresponds to the first annotation of a type) or
   *          negative (-1 corresponds to the last annotation of a type.)
   * @return an annotation of the given type
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  @SuppressWarnings("unchecked")
  public static <T extends Annotation> T selectByIndex(JCas jCas, Class<T> cls, int index) {
    return (T) CasUtil.selectByIndex(jCas.getCas(), getType(jCas, cls), index);
  }

  /**
   * Get the single instance of the specified type from the JCas.
   * 
   * @param <T>
   *          JCas wrapper type.
   * @param jCas
   *          a JCas containing the annotation.
   * @param type
   *          a UIMA type.
   * @return the single instance of the given type. throws IllegalArgumentException if not exactly
   *         one instance if the given type is present.
   */
  @SuppressWarnings("unchecked")
  public static <T extends TOP> T selectSingle(JCas jCas, Class<T> type) {
    return (T) CasUtil.selectSingleFS(jCas.getCas(), getType(jCas, type));
  }

  /**
   * Return an annotation preceding or following of a given reference annotation.
   * 
   * @param <T>
   *          the JCas type.
   * @param aType
   *          a type.
   * @param annotation
   *          anchor annotation
   * @param index
   *          relative position to access. A negative value selects a preceding annotation while a
   *          positive number selects a following annotation.
   * @return the addressed annotation.
   * @throws IndexOutOfBoundsException
   *           if the relative index points beyond the type index bounds.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  @SuppressWarnings("unchecked")
  public static <T extends Annotation> T selectSingleRelative(Class<T> aType,
          AnnotationFS annotation, int index) {
    CAS cas = annotation.getCAS();
    Type t = CasUtil.getType(cas, aType);
    return (T) CasUtil.selectSingleRelative(cas, t, annotation, index);
  }

  /**
   * Return an annotation preceding or following of a given reference annotation. If the type
   * parameter corresponds to the type or a subtype of the anchor annotation and the relative
   * position is 0, then the anchor annotation is returned.
   * 
   * @param <T>
   *          the JCas type.
   * @param aJCas
   *          a JCas.
   * @param aType
   *          type of the annotation to be returned.
   * @param aAnchor
   *          anchor annotation
   * @param aPosition
   *          relative position to access. A negative value selects a preceding annotation while a
   *          positive number selects a following annotation.
   * @return the addressed annotation.
   * @throws IndexOutOfBoundsException
   *           if the relative position points beyond the type index bounds.
   * @throws IllegalArgumentException
   *           if the relative position is {@code 0} and the anchor type does not subsume the
   *           selected type.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  @SuppressWarnings("unchecked")
  public static <T extends Annotation> T selectSingleRelative(JCas aJCas, Class<T> aType,
          AnnotationFS aAnchor, int aPosition) {
    Type t = getType(aJCas, aType);
    return (T) CasUtil.selectSingleRelative(aJCas.getCas(), t, aAnchor, aPosition);
  }

  /**
   * Returns the n annotations preceding the given annotation
   * 
   * @param <T>
   *          the JCas type.
   * @param aType
   *          a type.
   * @param annotation
   *          anchor annotation
   * @param count
   *          number of annotations to collect
   * @return List of aType annotations preceding anchor annotation
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends Annotation> List<T> selectPreceding(Class<T> aType,
          AnnotationFS annotation, int count) {
    Type t = CasUtil.getType(annotation.getCAS(), aType);
    return cast(CasUtil.selectPreceding(annotation.getView(), t, annotation, count));
  }

  /**
   * Returns the n annotations preceding the given annotation
   * 
   * @param <T>
   *          the JCas type.
   * @param aJCas
   *          a JCas.
   * @param aType
   *          a type.
   * @param annotation
   *          anchor annotation
   * @param count
   *          number of annotations to collect
   * @return List of aType annotations preceding anchor annotation
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends Annotation> List<T> selectPreceding(JCas aJCas, Class<T> aType,
          AnnotationFS annotation, int count) {
    Type t = getType(aJCas, aType);
    return cast(CasUtil.selectPreceding(aJCas.getCas(), t, annotation, count));
  }

  /**
   * Returns the n annotations following the given annotation
   * 
   * @param <T>
   *          the JCas type.
   * @param aType
   *          a type.
   * @param annotation
   *          anchor annotation
   * @param count
   *          number of annotations to collect
   * @return List of aType annotations following anchor annotation
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends Annotation> List<T> selectFollowing(Class<T> aType,
          AnnotationFS annotation, int count) {
    Type t = CasUtil.getType(annotation.getCAS(), aType);
    return cast(CasUtil.selectFollowing(annotation.getView(), t, annotation, count));
  }

  /**
   * Returns the n annotations following the given annotation
   * 
   * @param <T>
   *          the JCas type.
   * @param aJCas
   *          a JCas.
   * @param aType
   *          a type.
   * @param annotation
   *          anchor annotation
   * @param count
   *          number of annotations to collect
   * @return List of aType annotations following anchor annotation
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends Annotation> List<T> selectFollowing(JCas aJCas, Class<T> aType,
          AnnotationFS annotation, int count) {
    Type t = getType(aJCas, aType);
    return cast(CasUtil.selectFollowing(aJCas.getCas(), t, annotation, count));
  }

  /**
   * Test if a JCas contains an annotation of the given type.
   * 
   * @param <T>
   *          the annotation type.
   * @param aJCas
   *          a JCas.
   * @param aType
   *          a annotation class.
   * @return {@code true} if there is at least one annotation of the given type in the JCas.
   */
  public static <T extends TOP> boolean exists(JCas aJCas, Class<T> aType) {
    return JCasUtil.iterator(aJCas, aType).hasNext();
  }

  /**
   * Convenience method to get the specified view or a default view if the requested view does not
   * exist. The default can also be {@code null}.
   * 
   * @param jcas
   *          a JCas
   * @param viewName
   *          the requested view.
   * @param fallback
   *          the default view if the requested view does not exist.
   * @return the requested view or the default if the requested view does not exist.
   * @throws IllegalStateException
   *           if the JCas wrapper cannot be obtained.
   */
  public static JCas getView(JCas jcas, String viewName, JCas fallback) {
    try {
      CAS fallbackCas = fallback != null ? fallback.getCas() : null;
      CAS view = CasUtil.getView(jcas.getCas(), viewName, fallbackCas);
      return view != null ? view.getJCas() : null;
    } catch (CASException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Convenience method to get the specified view or create a new view if the requested view does
   * not exist.
   * 
   * @param jcas
   *          a JCas
   * @param viewName
   *          the requested view.
   * @param create
   *          the view is created if it does not exist.
   * @return the requested view
   * @throws IllegalStateException
   *           if the JCas wrapper cannot be obtained.
   */
  public static JCas getView(JCas jcas, String viewName, boolean create) {
    try {
      CAS view = CasUtil.getView(jcas.getCas(), viewName, create);
      return view != null ? view.getJCas() : null;
    } catch (CASException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Fetch the text covered by the specified annotations and return it as a list of strings.
   * 
   * @param <T>
   *          UIMA JCas type.
   * @param iterable
   *          annotation container.
   * @return list of covered strings.
   */
  public static <T extends AnnotationFS> List<String> toText(Iterable<T> iterable) {
    return CasUtil.toText(iterable);
  }

  @SuppressWarnings({ "cast", "unchecked", "rawtypes" })
  private static <T> Collection<T> cast(Collection aCollection) {
    return (Collection<T>) aCollection;
  }

  @SuppressWarnings({ "cast", "unchecked", "rawtypes" })
  private static <T> List<T> cast(List aCollection) {
    return (List<T>) aCollection;
  }

  @SuppressWarnings({ "cast", "unchecked", "rawtypes" })
  private static <K, V> Map<K, V> cast(Map aCollection) {
    return (Map<K, V>) aCollection;
  }
}
