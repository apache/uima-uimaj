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

import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;

/**
 * An annotation index provides additional iterator functionality that applies only to instances of
 * <code>uima.tcas.Annotation</code> (or its subtypes). You can obtain an AnnotationIndex by calling:
 * <p>
 * <code>AnnotationIndex idx = cas.getAnnotationIndex();</code> or <br>
 * <code>AnnotationIndex&lt;SomeJCasType&gt; idx = jcas.getAnnotationIndex(SomeJCasType.class);</code> 
 * </p>
 * <p>
 * Note that the AnnotationIndex defines the following sort order between two annotations:
 * <ul>
 * <li>Annotations are sorted in increasing order of their start offset. That is, for any
 * annotations a and b, if <code>a.start &lt; b.start</code> then <code> a &lt; b</code>.</li>
 * <li>Annotations whose start offsets are equal are next sorted by <i>decreasing</i> order of
 * their end offsets. That is, if <code>a.start = b.start</code> and <code>a.end &gt; b.end</code>,
 * then <code> a &lt; b</code>. This causes annotations with larger spans to be sorted before
 * annotations with smaller spans, which produces an iteration order similar to a preorder tree
 * traversal.</li>
 * <li>Annotations whose start offsets are equal and whose end offsets are equal are sorted based
 * on {@link org.apache.uima.resource.metadata.TypePriorities} (which is an element of the component
 * descriptor). That is, if <code>a.start = b.start</code>, <code>a.end = b.end</code>, and
 * the type of <code>a</code> is defined before the type of <code>b</code> in the type
 * priorities, then <code>a &lt; b</code>.
 * <li>
 * <li>If none of the above rules apply, then the ordering is arbitrary. This will occur if you
 * have two annotations of the exact same type that also have the same span. It will also occur if
 * you have not defined any type priority between two annotations that have the same span.</li>
 * </ul>
 * <p>
 * In the method descriptions below, the notation <code>a &lt; b</code>, where <code>a</code>
 * and <code>b</code> are annotations, should be taken to mean <code>a</code> comes before
 * <code>b</code> in the index, according to the above rules.</p>
 * 
 * @param <T> The top most Java cover class (usually a JCas Class) specified for the underlying index.
 */

public interface AnnotationIndex<T extends AnnotationFS> extends FSIndex<T> {
  /**
   * Return an iterator over annotations that can be constrained to be unambiguous.
   * <p>
   * A disambiguated iterator is defined as follows. The first annotation returned is the same as
   * would be returned by the corresponding ambiguous iterator. If the unambiguous iterator has
   * returned <code>a</code> previously, it will next return the smallest <code>b</code> s.t. a &lt; b and a.getEnd() &lt;=
   * b.getBegin().  In other words, the <code>b</code> annotation's start will be large enough to not
   * overlap the span of <code>a</code>.
   * </p>
   * 
   * <p>An unambiguous iterator makes a snapshot copy of the index containing just the disambiguated items, and 
   * iterates over that.  It doesn't check for concurrent index modifications (the ambiguous iterator does check for this).
   * 
   * @param ambiguous
   *          If set to false, iterator will be unambiguous.
   * @return A annotation iterator.
   */
  FSIterator<T> iterator(boolean ambiguous);

  /**
   * Return a subiterator whose bounds are defined by the input annotation.
   * 
   * <p>
   * The <code>annot</code> is used for 3 purposes:</p>
   * <ul><li>It is used to compute the position in the index where the iteration starts.</li>
   * <li>It is used to compute end point where the iterator stops when moving forward.</li>
   * <li>It is used to specify which annotations will be skipped while iterating.</li>
   * </ul>
   * 
   * <p>The starting position is computed by first finding a position 
   * whose annotation compares equal with the <code>annot</code> (this might be one of several), and then
   * advancing until reaching a position where the annotation there is not equal to the 
   * <code>annot</code>.
   * If no item in the index is equal (meaning it has the same begin, the same end, and is the same type
   * as the <code>annot</code>) 
   * then the iterator is positioned to the first annotation 
   * which is greater than the <code>annot</code>, or
   * if there are no annotations greater than the <code>annot</code>, the iterator is marked invalid.
   * </p>
   * <p>The iterator will stop (become invalid) when
   * <ul><li>it runs out of items in the index going forward or backwards, or</li>
   * <li>while moving forward, it reaches a point where the annotation at that position has a 
   * start is beyond the <code>annot's</code> end position, or</li>
   * <li>while moving backwards, it reaches a position in front of its original starting position.</li>
   * </ul>
   * <p>While iterating, it operates like a <code>strict</code> iterator; 
   * annotations whose end positions are &gt; the end position of <code>annot</code> are skipped.
   * </p>

     * <p>This is equivalent to returning annotations <code>b</code> such that</p> 
   * <ul><li><code>annot &lt; b</code>, and</li>
   * <li><code>annot.getEnd() &gt;= b.getBegin()</code>, skipping <code>b's</code>
   * whose end position is &gt; annot.getEnd().</li>
   * </ul>
   * 
   * <p>For annotations x, y, <code>x &lt; y</code>
   * here is to be interpreted as "x comes before y in the index", according to the rules defined in
   * the description of {@link AnnotationIndex this class}.
   * </p>
   * 
   * <p>
   * This definition implies that annotations <code>b</code> that have the same span as
   * <code>annot</code> may or may not be returned by the subiterator. This is determined by the
   * type priorities; the subiterator will only return such an annotation <code>b</code> if the
   * type of <code>annot</code> precedes the type of <code>b</code> in the type priorities
   * definition. If you have not specified the priority, or if <code>annot</code> and
   * <code>b</code> are of the same type, then the behavior is undefined.
   * </p>
   *
   * <p>
   * For example, if you have an annotation <code>S</code> of type <code>Sentence</code> and an
   * annotation <code>P</code> of type <code>Paragraph</code> that have the same span, and you
   * have defined <code>Paragraph</code> before <code>Sentence</code> in your type priorities,
   * then <code>subiterator(P)</code> will give you an iterator that will return <code>S</code>,
   * but <code>subiterator(S)</code> will give you an iterator that will NOT return <code>P</code>.
   * The intuition is that a Paragraph is conceptually larger than a Sentence, as defined by the
   * type priorities.
   * </p>
   * 
   * <p>
   * Calling <code>subiterator(a)</code> is equivalent to calling
   * <code>subiterator(a, true, true).</code>. See
   * {@link #subiterator(AnnotationFS, boolean, boolean) subiterator(AnnotationFS, boolean, boolean)}.
   * </p>
   * 
   * @param annot
   *          Defines the boundaries of the subiterator.
   * @return A subiterator.
   */
  FSIterator<T> subiterator(AnnotationFS annot);

  /**
   * Return a subiterator whose bounds are defined by the <code>annot</code>.
   * <p>
   * The <code>annot</code> is used in 2 or 3 ways.</p>
   * <ul><li>It specifies the left-most position in the index where the iteration starts.</li>
   * <li>It specifies an end point where the iterator stops.</li>
   * <li>If <code>strict</code> is specified, the end point also specifies which annotations 
   * will be skipped while iterating.</li>
   * </ul>
   * <p>The starting position is computed by first finding the position 
   * whose annotation compares equal with the <code>annot</code>, and then
   * advancing until reaching a position where the annotation there is not equal to the 
   * <code>annot</code>.
   * If no item in the index is equal (meaning it has the same begin, the same end, and is the same type
   * as the <code>annot</code>) 
   * then the iterator is positioned to the first annotation 
   * which is greater than the <code>annot</code>, or
   * if there are no annotations greater than the <code>annot</code>, the iterator is marked invalid.
   * </p>
   * <p>The iterator will stop (become invalid) when</p>
   * <ul><li>it runs out of items in the index going forward or backwards, or</li>
   * <li>while moving forward, it reaches a point where the annotation at that position has a 
   * start is beyond the <code>annot's</code> end position, or</li>
   * <li>while moving backwards, it reaches a position in front of its original starting position</li>
   * </ul>
   *   
   * <p>Ignoring <code>strict</code> and <code>ambiguous</code> for a moment, 
   * this is equivalent  to returning annotations <code>b</code> such that</p> 
   * <ul><li><code>annot &lt; b</code> using the standard annotation comparator, and</li>
   * <li><code>annot.getEnd() &gt;= b.getBegin()</code>, and also bounded by the index itself.</li>
   * </ul>
   * <p>
   * A <code>strict</code> subiterator skips annotations where 
   * <code>annot.getEnd() &lt; b.getEnd()</code>.
   * </p>
   * <p>
   * A <code>ambiguous = false</code> specification produces an unambiguous iterator, which 
   * computes a subset of the annotations, going forward, such that annotations whose <code>begin</code>
   * is contained within the previous returned annotation's span, are skipped.
   * </p>
   * <p>For annotations x,y, <code>x &lt; y</code>
   * here is to be interpreted as "x comes before y in the index", according to the rules defined in
   * the description of {@link AnnotationIndex this class}.
   * <p>
   * If <code>strict = true</code> then annotations whose end is &gt; <code>annot.getEnd()</code>
   * are skipped.
   * </p> 
   * <p>
   * These definitions imply that annotations <code>b</code> that have the same span as
   * <code>annot</code> may or may not be returned by the subiterator. This is determined by the
   * type priorities; the subiterator will only return such an annotation <code>b</code> if the
   * type of <code>annot</code> precedes the type of <code>b</code> in the type priorities
   * definition. If you have not specified the priority, or if <code>annot</code> and
   * <code>b</code> are of the same type, then the behavior is undefined.
   * </p>
   * <p>
   * For example, if you have an annotation <code>S</code> of type <code>Sentence</code> and an
   * annotation <code>P</code> of type <code>Paragraph</code> that have the same span, and you
   * have defined <code>Paragraph</code> before <code>Sentence</code> in your type priorities,
   * then <code>subiterator(P)</code> will give you an iterator that will return <code>S</code>,
   * but <code>subiterator(S)</code> will give you an iterator that will NOT return <code>P</code>.
   * The intuition is that a Paragraph is conceptually larger than a Sentence, as defined by the
   * type priorities.
   * </p>
    * 
   * @param annot
   *          Annotation setting boundary conditions for subiterator.
   * @param ambiguous
   *          If set to <code>false</code>, resulting iterator will be unambiguous.
   * @param strict
   *          Controls if annotations that overlap to the right are considered in or out.
   * @return A subiterator.
   */
  FSIterator<T> subiterator(AnnotationFS annot, boolean ambiguous, boolean strict);

  /**
   * Create an annotation tree with <code>annot</code> as root node. The tree is defined as
   * follows: for each node in the tree, the children are the sequence of annotations that would be
   * obtained from a strict, unambiguous subiterator of the node's annotation.
   * 
   * @param annot
   *          The annotation at the root of the tree.  This must be of type T or a subtype
   * @return The annotation tree rooted at <code>annot</code>.
   */
   AnnotationTree<T> tree(T annot);
}
