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
package org.apache.uima.cas;

import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Collection of builder style methods to specify selection of FSs from indexes Documentation is in
 * a chapter in the UIMA Version 3 User's Guide.
 */
public interface SelectFSs<T extends FeatureStructure> extends Iterable<T>, Stream<T> {

  /**
   * Specify that type priority should be included when comparing two Feature Structures when moving
   * to the leftmost among otherwise equal items for moveTo(fs).
   * <p>
   * Default is to not include type priority.
   * 
   * @return the updated SelectFSs object
   */
  SelectFSs<T> typePriority();

  /**
   * Specify that type priority should or should not be included when comparing two Feature
   * Structures while positioning an iterator
   * <p>
   * Default is to not include type priority.
   * 
   * @param typePriority
   *          if true says to include the type priority
   * @return the updated SelectFSs object
   */
  SelectFSs<T> typePriority(boolean typePriority);

  // Filters while iterating over Annotations

  /**
   * Meaningful only for Annotation Indexes, specifies that iteration should return only annotations
   * which don't overlap with each other. Also known as "unambiguous".
   * <p>
   * Default is to not have this filter.
   * 
   * @return the updated SelectFSs object
   */
  SelectFSs<T> nonOverlapping(); // requires Annotation Index, known as unambiguous

  /**
   * Meaningful only for Annotation Indexes, specifies that iteration should or should not return
   * only annotations which don't overlap with each other. Also known as "unambiguous".
   * <p>
   * Default is to not have this filter.
   * 
   * @param nonOverlapping
   *          true to specify filtering for only non-overlapping annotations.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> nonOverlapping(boolean nonOverlapping); // requires Annotation Index

  /**
   * Meaningful only for coveredBy, includes annotations where the end exceeds the bounding
   * annotation's end.
   * <p>
   * Default is to NOT include annotations whose end exceeds the bounding annotation's end.
   * 
   * @return the updated SelectFSs object
   */
  SelectFSs<T> includeAnnotationsWithEndBeyondBounds(); // requires Annotation Index, known as "not
                                                        // strict"

  /**
   * Meaningful only for coveredBy, includes or filters out annotations where the end exceeds the
   * bounding annotation's end.
   * <p>
   * Default is to NOT include annotations whose end exceeds the bounding annotation's end.
   * 
   * @param includeAnnotationsWithEndBeyondBounds
   *          false to filter out annotations whose end exceeds the bounding annotation's end
   * @return the updated SelectFSs object
   */
  SelectFSs<T> includeAnnotationsWithEndBeyondBounds(boolean includeAnnotationsWithEndBeyondBounds); // requires
                                                                                                     // Annotation
                                                                                                     // Index

  /**
   * Meaningful only for coveredBy and covering: if true, then returned annotations are compared
   * equal to the bounding annotation, and if equal, they are skipped.
   * <p>
   * Default is to use feature structure identity comparison (same id()s), not equals, when doing
   * the test to see if an annotation should be skipped.
   * <p>
   * This is identical to useAnnotationEquals(true).
   * 
   * @return the updated SelectFSs object
   */
  SelectFSs<T> skipWhenSameBeginEndType();

  /**
   * Meaningful only for coveredBy: if true, then returned annotations are compared to the bounding
   * annotation using the specified kind of equal comparison, and if equal, they are skipped.
   * <p>
   * Default is to use feature structure identity comparison (same id()s), not equals, when doing
   * the test to see if an annotation should be skipped.
   * 
   * @param useAnnotationEquals
   *          if true, use equals, if false, use id() ==.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> useAnnotationEquals(boolean useAnnotationEquals);

  // Miscellaneous
  /**
   * Extend the selection to be over all the CAS views, not just a single view.
   * <p>
   * Default is that the selection is just for one CAS view
   * 
   * @return the updated SelectFSs object
   */
  SelectFSs<T> allViews();

  /**
   * Extend or not extend the selection to be over all the CAS views, not just a single view.
   * <p>
   * Default is that the selection is just for one CAS view
   * 
   * @param allViews
   *          true to extend the selection.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> allViews(boolean allViews);

  /**
   * Applies to the various argument forms of the get and single methods. Indicates that a null
   * value should not throw an exception.
   * <p>
   * Calling this method is equivalent to nullOK(true). If never called, nulls are not OK by
   * default.
   * 
   * @return the updated SelectFSs object
   */
  SelectFSs<T> nullOK();

  /**
   * Applies to the various argument forms of the get and single methods. Indicates that a null
   * value should or should not throw an exception.
   * <p>
   * Default: null is not OK as a value
   * 
   * @param nullOk
   *          true if null is an ok value.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> nullOK(boolean nullOk); // applies to get()

  /**
   * Specifies that order is not required while iterating over an otherwise ordered index. This can
   * be a performance boost for hierarchically nested types.
   * <p>
   * Default: order is required by default, when iterating over an ordered index.
   * 
   * @return the updated SelectFSs object
   */
  SelectFSs<T> orderNotNeeded(); // ignored if not ordered index

  /**
   * Specifies that order is or is not required while iterating over an otherwise ordered index.
   * This can be a performance boost for hierarchically nested types.
   * <p>
   * Default: order is required by default, when iterating over an ordered index.
   * 
   * @param unordered
   *          true means order is not needed.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> orderNotNeeded(boolean unordered); // ignored if not ordered index

  /**
   * Specifies that the iteration should run in reverse order from normal. Note that this does not
   * compose; two calls to this will still result in the iteration running in reverse order.
   * 
   * @return the updated SelectFSs object
   */
  SelectFSs<T> backwards(); // ignored if not ordered index

  /**
   * Specifies that the iteration should run in the normal or reverse order. Note that this does not
   * compose.
   * 
   * @param backwards
   *          true to run in reverse order
   * @return the updated SelectFSs object
   */
  SelectFSs<T> backwards(boolean backwards); // ignored if not ordered index

  // SelectFSs<T> noSubtypes();
  // SelectFSs<T> noSubtypes(boolean noSubtypes);

  // ---------------------------------
  // starting position specification
  //
  // Variations, controlled by:
  // * typePriority
  // * useAnnotationEquals
  // * positionUsesType
  //
  // The positional specs imply starting at the
  // - left-most (if multiple) FS at that position, or
  // - if no FS at the position, the next higher FS
  // - if !typePriority, equal test is only begin/end
  //
  // shifts, if any, occur afterwards
  // - can be positive or negative
  // ---------------------------------
  /**
   * Starting Position specification - Shifts the normal start position by the shiftAmount, which
   * may be negative. Repeated calls to this just replaces the requested shift amount; a single
   * shift only occurs when a result is obtained.
   * 
   * @param shiftAmount
   *          the amount to shift; this many Feature Structures which normally would be returned are
   *          instead skipped.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> shifted(int shiftAmount);

  /**
   * Starting Position specification - For ordered sources, specifies which FS to start at. Requires
   * an ordered index not necessarily AnnotationIndex, not necessarily sorted
   * 
   * @param fs
   *          a Feature Structure specifying a starting position.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> startAt(TOP fs); // an ordered index not necessarily AnnotationIndex, not necessarily
                                // sorted

  /**
   * Starting Position specification - For ordered sources, specifies which FS to start at. Requires
   * an ordered index not necessarily AnnotationIndex, not necessarily sorted
   * 
   * @param fs
   *          a Feature Structure specifying a starting position.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> startAt(FeatureStructure fs);

  /**
   * Starting Position specification - For Annotation Indexes, specifies which FS to start at. This
   * method is incompatible with {@link #typePriority()} and turns type priorities off implicitly.
   * Do not turn type priorities back on. Positions to the leftmost (if there are multiple equal
   * ones) Annotation whose begin is &gt;= begin
   * 
   * @param begin
   *          the begin bound
   * @return the updated SelectFSs object
   */
  SelectFSs<T> startAt(int begin); // requires Annotation Index, no type priorities

  /**
   * Starting Position specification - For Annotation Indexes, specifies which FS to start at.
   * 
   * @param begin
   *          the begin bound
   * @param end
   *          the end bound
   * @return the updated SelectFSs object
   */
  SelectFSs<T> startAt(int begin, int end); // requires Annotation Index

  /**
   * Starting Position specification - A combination of startAt followed by a shift
   * 
   * Requires an ordered index not necessarily AnnotationIndex, not necessarily sorted
   * 
   * @param fs
   *          a Feature Structure specifying a starting position.
   * @param shift
   *          the amount to shift; this many Feature Structures which normally would be returned are
   *          instead skipped.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> startAt(TOP fs, int shift); // an ordered index not necessarily AnnotationIndex, not
                                           // necessarily sorted

  /**
   * Starting Position specification - A combination of startAt followed by a shift
   * 
   * Requires an ordered index not necessarily AnnotationIndex, not necessarily sorted This versions
   * avoids a runtime cast check.
   * 
   * @param fs
   *          a Feature Structure specifying a starting position.
   * @param shift
   *          the amount to shift; this many Feature Structures which normally would be returned are
   *          instead skipped.
   * @return the updated SelectFSs object
   */

  SelectFSs<T> startAt(FeatureStructure fs, int shift); // an ordered index not necessarily
                                                        // AnnotationIndex, not necessarily sorted

  /**
   * Starting Position specification - A combination of startAt followed by a shift Requires an
   * Annotation Index.
   * 
   * @param begin
   *          the begin bound
   * @param end
   *          the end bound
   * @param shift
   *          the amount to shift; this many Feature Structures which normally would be returned are
   *          instead skipped.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> startAt(int begin, int end, int shift); // requires Annotation Index

  /**
   * Limits the number of Feature Structures returned by this select
   * 
   * @param n
   *          the maximum number of feature structures returned. This must be a value &gt;= 0.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> limit(int n);

  // ---------------------------------
  // subselection based on bounds
  // - uses
  // -- typePriority,
  // -- positionUsesType,
  // -- useAnnotationEquals
  // ---------------------------------
  /**
   * Subselection - specifies selecting Feature Structures having the same begin and end -
   * influenced by typePriority, positionUsesType, and useAnnotationEquals Requires an Annotation
   * Index.
   * 
   * @param fs
   *          specifies the bounds.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> at(AnnotationFS fs); // requires Annotation Index

  /**
   * Subselection - specifies selecting Feature Structures having the same begin and end Requires an
   * Annotation Index. - influenced by typePriority, positionUsesType, and useAnnotationEquals
   * 
   * @param begin
   *          the begin bound
   * @param end
   *          the end bound
   * @return the updated SelectFSs object
   */
  SelectFSs<T> at(int begin, int end); // requires Annotation Index

  /**
   * Subselection - specifies selecting Feature Structures starting (and maybe ending) within a
   * bounding Feature Structure - influenced by typePriority, positionUsesType, useAnnotationEquals,
   * includeAnnotationsWithEndBeyondBounds Requires an Annotation Index.
   * 
   * @param fs
   *          specifies the bounds.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> coveredBy(AnnotationFS fs); // requires Annotation Index

  /**
   * Subselection - specifies selecting Feature Structures starting (and maybe ending) within a
   * bounding Feature Structure Requires an Annotation Index.
   * 
   * @param begin
   *          the begin bound
   * @param end
   *          the end bound
   * @return the updated SelectFSs object
   */
  SelectFSs<T> coveredBy(int begin, int end); // requires Annotation Index

  /**
   * Subselection - specifies selecting Feature Structures starting before or equal to bounding
   * Feature Structure and ending at or beyond the bounding Feature Structure - influenced by
   * typePriority, positionUsesType, useAnnotationEquals Requires an Annotation Index.
   * 
   * @param fs
   *          specifies the bounds.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> covering(AnnotationFS fs); // requires Annotation Index

  /**
   * Subselection - specifies selecting Feature Structures starting before or equal to bounding
   * Feature Structure's begin and ending at or beyond the bounding Feature Structure's end Requires
   * an Annotation Index.
   * 
   * @param begin
   *          the begin bound
   * @param end
   *          the end bound
   * @return the updated SelectFSs object
   */
  SelectFSs<T> covering(int begin, int end); // requires Annotation Index

  /**
   * Subselection - specifies selecting Feature Structures which lie between two annotations. A
   * bounding Annotation is constructed whose begin is the end of fs1, and whose end is the begin of
   * fs2. Requires an Annotation Index.
   * <p>
   * If fs1 &gt; fs2, they are swapped.
   * 
   * @param fs1
   *          the beginning bound
   * @param fs2
   *          the ending bound
   * @return the updated SelectFSs object
   */
  SelectFSs<T> between(AnnotationFS fs1, AnnotationFS fs2); // requires Annotation Index, implies a
                                                            // coveredBy style

  // ---------------------------------
  // Semantics:
  // - following uimaFIT
  // - must be annotation subtype, annotation index
  // - following: move to first fs where begin > pos.end
  // - preceding: move to first fs where end < pos.begin
  //
  // - return the limit() or all
  // - for preceding, return in forward order (unless backward is specified)
  // - for preceding, skips FSs whose end >= begin (adjusted by offset)
  // ---------------------------------
  /**
   * For AnnotationIndex, position to first Annotation whose begin &gt;= fs.getEnd();
   * 
   * @param annotation
   *          the Annotation to follow
   * @return the updated SelectFSs object
   */
  SelectFSs<T> following(Annotation annotation);

  /**
   * Select {@link Annotation annotations} that follow the specified document position (i.e.
   * character offset). This is equivalent to performing a
   * {@code following(new Annotation(jcas, 0, position)}, so all annotations starting at
   * {@code position} or after are returned, including zero-width annotations.
   * 
   * @param position
   *          start following this position
   * @return the updated SelectFSs object
   */
  SelectFSs<T> following(int position);

  /**
   * For AnnotationIndex, position to first Annotation whose begin &gt;= fs.getEnd() and then adjust
   * position by the offset
   * 
   * @param annotation
   *          start following this Annotation, adjusted for the offset
   * @param offset
   *          positive or negative shift amount to adjust starting position
   * @return the updated SelectFSs object
   */
  SelectFSs<T> following(Annotation annotation, int offset);

  /**
   * For AnnotationIndex, position to first Annotation whose begin &gt;= position and then adjust
   * position by the offset.
   * 
   * @param position
   *          start following this position, adjusted for the offset
   * @param offset
   *          positive or negative shift amount to adjust starting position
   * @return the updated SelectFSs object
   */
  SelectFSs<T> following(int position, int offset);

  /**
   * For AnnotationIndex, set up a selection that will go from the beginning to the first Annotation
   * to the left of the specified position, whose end &lt;= fs.getBegin(). Annotations whose end
   * &gt; fs.getBegin() are skipped.
   * 
   * @param annotation
   *          the Annotation to use as the position to start before.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> preceding(Annotation annotation);

  /**
   * Select {@link Annotation annotations} that precede the specified document position (i.e.
   * character offset). This is equivalent to performing a
   * {@code preceding(new Annotation(jcas, position, Integer.MAX_VALUE)}, so all annotations ending
   * at {@code position} or before are returned, including zero-width annotations.
   * 
   * @param position
   *          start following this position
   * @return the updated SelectFSs object
   */
  SelectFSs<T> preceding(int position);

  /**
   * For AnnotationIndex, set up a selection that will go from the beginning to the first Annotation
   * to the left of the specified position, ending at the last Annotation whose end &lt;=
   * fs.getBegin(), after adjusting by offset items. Annotations whose end &gt; fs.getBegin() are
   * skipped (including during the offset positioning)
   * 
   * @param annotation
   *          the Annotation to use as the position to start before.
   * @param offset
   *          the offset adjustment, positive or negative. Positive moves backwards.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> preceding(Annotation annotation, int offset);

  /**
   * For AnnotationIndex, set up a selection that will go from the beginning to the first Annotation
   * to the left of the specified position, ending at the last Annotation whose end &lt;= position.
   * after adjusting by offset items. Annotations whose end &gt; position are skipped (including
   * during the offset positioning)
   * 
   * @param position
   *          the position to start before.
   * @param offset
   *          the offset adjustment, positive or negative. Positive moves backwards.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> preceding(int position, int offset);

  // ---------------------------------
  // terminal operations
  // returning other than SelectFSs
  //
  // ---------------------------------
  /**
   * @return an FSIterator over the selection. The iterator is set up depending on preceding
   *         configuration calls to this SelectFSs instance.
   */
  FSIterator<T> fsIterator();

  // Iterator<T> iterator(); // inherited, not needed here
  /**
   * @return a List object whose elements represent the selection.
   */
  List<T> asList();

  /**
   * @param clazz
   *          the class of the type of the elements
   * @return a Array object representation of the elements of the selection.
   */
  T[] asArray(Class<? super T> clazz);
  // Spliterator<T> spliterator(); // inherited, not needed here

  // returning one item

  /**
   * Get the first element or null if empty or the element at the first position is null. if nullOK
   * is false, then throws CASRuntimeException if null would have been returned.
   * 
   * @return first element or null if empty
   * @throws CASRuntimeException
   *           conditioned on nullOK == false, and null being returned or the selection is empty.
   */
  T get(); // returns first element or null if empty (unless nullOK(false) specified)

  /**
   * @return first element, verifying that the size of the selection is 1 (or maybe 0)
   * @throws CASRuntimeException
   *           (conditioned on nullOK == false ) if element is null or if there is more than 1
   *           element in the selection, or if the selection is empty
   */
  T single(); // throws if not exactly 1 element, throws if null

  /**
   * @return first element, which may be null, or null if selection is empty.
   * @throws CASRuntimeException
   *           if there is more than 1 element in the selection.
   */
  T singleOrNull(); // throws if more than 1 element, returns single or null

  // next are positioning alternatives
  // get(...) throws if null (unless nullOK specified)
  /**
   * Get the offset element or null if empty or the offset went outside the the selected elements.
   * <p>
   * If nullOK is false, then throws CASRuntimeException if null would have been returned, or the
   * selection is empty, or doesn't have enough elements to satisfy the positioning.
   * 
   * @param offset
   *          the offset adjustment, positive or negative.
   * @return the selected element or null
   * @throws CASRuntimeException
   *           conditioned on nullOK == false, and null being returned or the selection is empty, or
   *           the offset positioning going outside the elements in the selection.
   */
  T get(int offset); // returns first element or null if empty after positioning

  /**
   * Get the offset element or null if empty or the offset went outside the the selected elements.
   * <p>
   * If, after positioning, there is another element next to the one being returned (in the forward
   * direction if offset is positive, reverse direction if offset is negative) then throw an
   * exception.
   * <p>
   * If nullOK is false, then throws CASRuntimeException if null would have been returned, or the
   * selection is empty, or doesn't have enough elements to satisfy the positioning.
   * 
   * @param offset
   *          the offset adjustment, positive or negative.
   * @return the selected element or null
   * @throws CASRuntimeException
   *           if, after positioning, there is another element next to the one being returned (in
   *           the forward direction if offset is positive, reverse direction if offset is negative)
   *           or (conditioned on nullOK == false) null being returned or the selection is empty, or
   *           the offset positioning going outside the elements in the selection.
   */
  T single(int offset); // throws if not exactly 1 element

  /**
   * Get the offset element or null if empty or the offset went outside the the selected elements.
   * <p>
   * If, after positioning, there is another element next to the one being returned (in the forward
   * direction if offset is positive, reverse direction if offset is negative) then throw an
   * exception.
   * <p>
   * 
   * @param offset
   *          the offset adjustment, positive or negative.
   * @return the selected element or null
   * @throws CASRuntimeException
   *           if, after positioning, there is another element next to the one being returned (in
   *           the forward direction if offset is positive, reverse direction if offset is negative)
   */
  T singleOrNull(int offset); // throws if more than 1 element, returns single or null

  /**
   * Positions to the fs using moveTo(fs). Get the element at that position or null if empty or the
   * element at that position is null. if nullOK is false, then throws CASRuntimeException if null
   * would have been returned. This versions avoids a runtime cast check.
   * 
   * @param fs
   *          the positioning Feature Structure
   * @return first element or null if empty
   * @throws CASRuntimeException
   *           (conditioned on nullOK == false) null being returned or the selection is empty.
   */
  T get(TOP fs); // returns first element or null if empty after positioning

  /**
   * Positions to the fs using moveTo(fs). Get the element at that position or null if empty or the
   * element at that position is null. if nullOK is false, then throws CASRuntimeException if null
   * would have been returned.
   * 
   * @param fs
   *          the positioning Feature Structure
   * @return first element or null if empty
   * @throws CASRuntimeException
   *           (conditioned on nullOK == false) null being returned or the selection is empty.
   */
  T get(FeatureStructure fs); // returns first element or null if empty after positioning

  /**
   * Positions to the fs using moveTo(fs). Get the element at that position or null if empty or the
   * element at that position is null. if nullOK is false, then throws CASRuntimeException if null
   * would have been returned. This versions avoids a runtime cast check.
   * 
   * @param fs
   *          the positioning Feature Structure
   * @return first element or null if empty
   * @throws CASRuntimeException
   *           if, after positioning, there is another element following the one being returned or
   *           (conditioned on nullOK == false) and null being returned or the selection is empty.
   */
  T single(TOP fs); // throws if not exactly 1 element

  /**
   * Positions to the fs using moveTo(fs). Get the element at that position or null if empty or the
   * element at that position is null. if nullOK is false, then throws CASRuntimeException if null
   * would have been returned.
   * 
   * @param fs
   *          the positioning Feature Structure
   * @return first element or null if empty
   * @throws CASRuntimeException
   *           if, after positioning, there is another element following the one being returned or
   *           (conditioned on nullOK == false) and null being returned or the selection is empty.
   */
  T single(FeatureStructure fs); // throws if not exactly 1 element

  /**
   * Positions to the fs using moveTo(fs). Get the element at that position or null if empty or the
   * element at that position is null. This versions avoids a runtime cast check.
   * 
   * @param fs
   *          the positioning Feature Structure
   * @return first element or null if empty
   * @throws CASRuntimeException
   *           if, after positioning, there is another element following the one being returned
   */
  T singleOrNull(TOP fs); // throws if more than 1 element, returns single or null

  /**
   * Positions to the fs using moveTo(fs). Get the element at that position or null if empty or the
   * element at that position is null.
   * 
   * @param fs
   *          the positioning Feature Structure
   * @return first element or null if empty
   * @throws CASRuntimeException
   *           if, after positioning, there is another element following the one being returned
   */
  T singleOrNull(FeatureStructure fs); // throws if more than 1 element, returns single or null

  /**
   * Positions to the fs using moveTo(fs), followed by a shifted(offset). Gets the element at that
   * position or null if empty or the element at that position is null. if nullOK is false, then
   * throws CASRuntimeException if null would have been returned. This versions avoids a runtime
   * cast check.
   * 
   * @param fs
   *          where to move to
   * @param offset
   *          the offset move after positioning to fs, may be 0 or positive or negative
   * @return the selected element or null if empty
   * @throws CASRuntimeException
   *           (conditioned on nullOK == false) null being returned or the selection is empty.
   */
  T get(TOP fs, int offset); // returns first element or null if empty

  /**
   * Positions to the fs using moveTo(fs), followed by a shifted(offset). Gets the element at that
   * position or null if empty or the element at that position is null. if nullOK is false, then
   * throws CASRuntimeException if null would have been returned.
   * 
   * @param fs
   *          where to move to
   * @param offset
   *          the offset move after positioning to fs, may be 0 or positive or negative
   * @return the selected element or null if empty
   * @throws CASRuntimeException
   *           (conditioned on nullOK == false) null being returned or the selection is empty.
   */
  T get(FeatureStructure fs, int offset); // returns first element or null if empty

  /**
   * Positions to the fs using moveTo(fs), followed by a shifted(offset). Gets the element at that
   * position or null if empty or the element at that position is null.
   * <p>
   * If, after positioning, there is another element next to the one being returned (in the forward
   * direction if offset is positive or 0, reverse direction if offset is negative) then throw an
   * exception.
   * <p>
   * If nullOK is false, then throws CASRuntimeException if null would have been returned. This
   * versions avoids a runtime cast check.
   * 
   * @param fs
   *          the positioning Feature Structure
   * @param offset
   *          the offset adjustment, positive or negative.
   * @return the selected element or null if empty
   * @throws CASRuntimeException
   *           if, after positioning, there is another element following the one being returned or
   *           (conditioned on nullOK == false) null being returned or the selection is empty.
   */
  T single(TOP fs, int offset); // throws if not exactly 1 element

  /**
   * Positions to the fs using moveTo(fs), followed by a shifted(offset). Gets the element at that
   * position or null if empty or the element at that position is null.
   * <p>
   * If, after positioning, there is another element next to the one being returned (in the forward
   * direction if offset is positive or 0, reverse direction if offset is negative) then throw an
   * exception.
   * <p>
   * If nullOK is false, then throws CASRuntimeException if null would have been returned.
   * 
   * @param fs
   *          the positioning Feature Structure
   * @param offset
   *          the offset adjustment, positive or negative.
   * @return the selected element or null if empty
   * @throws CASRuntimeException
   *           if, after positioning, there is another element following the one being returned or
   *           (conditioned on nullOK == false) null being returned or the selection is empty.
   */
  T single(FeatureStructure fs, int offset); // throws if not exactly 1 element

  /**
   * Positions to the fs using moveTo(fs), followed by a shifted(offset). Gets the element at that
   * position or null if empty or the element at that position is null.
   * <p>
   * If, after positioning, there is another element next to the one being returned (in the forward
   * direction if offset is positive or 0, reverse direction if offset is negative) then throw an
   * exception.
   * <p>
   * This versions avoids a runtime cast check.
   * 
   * @param fs
   *          the positioning Feature Structure
   * @param offset
   *          the offset adjustment, positive or negative.
   * @return the selected element or null if empty
   * @throws CASRuntimeException
   *           if, after positioning, there is another element next to the one being returned
   */
  T singleOrNull(TOP fs, int offset); // throws if more than 1 element, returns single or null

  /**
   * Positions to the fs using moveTo(fs), followed by a shifted(offset). Gets the element at that
   * position or null if empty or the element at that position is null.
   * <p>
   * If, after positioning, there is another element next to the one being returned (in the forward
   * direction if offset is positive or 0, reverse direction if offset is negative) then throw an
   * exception.
   * <p>
   * 
   * @param fs
   *          the positioning Feature Structure
   * @param offset
   *          the offset adjustment, positive or negative.
   * @return the selected element or null if empty
   * @throws CASRuntimeException
   *           if, after positioning, there is another element next to the one being returned
   */
  T singleOrNull(FeatureStructure fs, int offset); // throws if more than 1 element, returns single
                                                   // or null

  /**
   * Position using a temporary Annotation with its begin and end set to the arguments. Gets the
   * element at that position or null if empty or the element at that position is null. if nullOK is
   * false, then throws CASRuntimeException if null would have been returned. This versions avoids a
   * runtime cast check.
   * 
   * @param begin
   *          the begin position of the temporary Annotation
   * @param end
   *          the end position of the temporary Annotation
   * @return the selected element or null if empty
   * @throws CASRuntimeException
   *           conditioned on nullOK == false, and null being returned or the selection is empty.
   */
  T get(int begin, int end); // returns first element or null if empty

  /**
   * Position using a temporary Annotation with its begin and end set to the arguments. Gets the
   * element at that position or null if empty or the element at that position is null.
   * <p>
   * If, after positioning, there is another element following the one being returned then throw an
   * exception.
   * <p>
   * if nullOK is false, then throws CASRuntimeException if null would have been returned.
   * 
   * @param begin
   *          the begin position of the temporary Annotation
   * @param end
   *          the end position of the temporary Annotation
   * @return the selected element or null if empty
   * @throws CASRuntimeException
   *           if, after positioning, there is another element following the one being returned or
   *           (conditioned on nullOK == false) null being returned or the selection is empty.
   */
  T single(int begin, int end); // throws if not exactly 1 element

  /**
   * Position using a temporary Annotation with its begin and end set to the arguments. Gets the
   * element at that position or null if empty or the element at that position is null.
   * <p>
   * If, after positioning, there is another element following the one being returned then throw an
   * exception.
   * 
   * @param begin
   *          the begin position of the temporary Annotation
   * @param end
   *          the end position of the temporary Annotation
   * @return the selected element or null if empty
   * @throws CASRuntimeException
   *           if, after positioning, there is another element following the one being returned
   */
  T singleOrNull(int begin, int end); // throws if more than 1 element, returns single or null

  /**
   * Position using a temporary Annotation with its begin and end set to the arguments, followed by
   * shifted(offset). Gets the element at that position or null if empty or the element at that
   * position is null.
   * <p>
   * 
   * @param begin
   *          the begin position of the temporary Annotation
   * @param end
   *          the end position of the temporary Annotation
   * @param offset
   *          the amount (positive or negative or 0) passed as an argument to shifted(int)
   * @return the selected element or null if empty
   * @throws CASRuntimeException
   *           (conditioned on nullOK == false) if null being returned or the selection is empty.
   */
  T get(int begin, int end, int offset); // returns first element or null if empty

  /**
   * Position using a temporary Annotation with its begin and end set to the arguments, followed by
   * shifted(offset). Gets the element at that position or null if empty or the element at that
   * position is null.
   * <p>
   * If, after positioning, there is another element next to the one being returned (in the forward
   * direction if offset is positive or 0, reverse direction if offset is negative) then throw an
   * exception.
   * 
   * @param begin
   *          the begin position of the temporary Annotation
   * @param end
   *          the end position of the temporary Annotation
   * @param offset
   *          the amount (positive or negative or 0) passed as an argument to shifted(int)
   * @return the selected element or null if empty
   * @throws CASRuntimeException
   *           if, after positioning, there is another element next to the one being returned or
   *           (conditioned on nullOK == false) if null being returned or the selection is empty.
   */
  T single(int begin, int end, int offset); // throws if not exactly 1 element

  /**
   * Position using a temporary Annotation with its begin and end set to the arguments, followed by
   * shifted(offset). Gets the element at that position or null if empty or the element at that
   * position is null.
   * <p>
   * If, after positioning, there is another element next to the one being returned (in the forward
   * direction if offset is positive or 0, reverse direction if offset is negative) then throw an
   * exception.
   * 
   * @param begin
   *          the begin position of the temporary Annotation
   * @param end
   *          the end position of the temporary Annotation
   * @param offset
   *          the amount (positive or negative or 0) passed as an argument to shifted(int)
   * @return the selected element or null if empty
   * @throws CASRuntimeException
   *           if, after positioning, there is another element next to the one being returned
   */
  T singleOrNull(int begin, int end, int offset); // throws if more than 1 element, returns single
                                                  // or null

  @Override
  default Spliterator<T> spliterator() {
    // TODO Auto-generated method stub
    return Iterable.super.spliterator();
  }

  /**
   * Use this static method to capture the generic argument
   * 
   * @param index
   *          - the index to select over as a source
   * @param <U>
   *          generic type of index
   * @return - a SelectFSs instance
   */

  static <U extends FeatureStructure> SelectFSs<U> select(FSIndex<U> index) {
    return index.select();
  }

  /**
   * This method is required, because the 2 interfaces inherited both define this and this is needed
   * to disambiguate Otherwise, get a compile error (but not on Eclipse...)
   */
  @Override
  default void forEach(Consumer<? super T> action) {
    Iterable.super.forEach(action);
  }

  /**
   * @return true if the selection is empty
   */
  boolean isEmpty();

  // /**
  // * DON'T USE THIS, use index.select(XXX.class) instead
  // * @param index the index to use
  // * @param clazz the JCas class
  // * @return a select instance for this index and type
  // */
  // static <U extends FeatureStructure, V extends U> SelectFSs<V> sselect(FSIndex<U> index,
  // Class<V> clazz) {
  // return index.select(clazz);
  // }

}
