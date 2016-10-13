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

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Stream;

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.cas.TOP;

/**
 * Collection of builder style methods to specify selection of FSs from indexes
 * Comment codes:
 *   AI = implies AnnotationIndex
 *   Ordered = implies an ordered index not necessarily AnnotationIndex
 *   BI = bounded iterator (boundedBy or bounding)
 */
public interface SelectFSs<T extends FeatureStructure> extends Iterable<T>, Stream<T> {
  
//  // If not specified, defaults to all FSs (unordered) unless AnnotationIndex implied
//    // Methods take their generic type from the variable to which they are assigned except for
//    // index(class) which takes it from its argument.
//  <N extends FeatureStructure> SelectFSs<N> index(String indexName);  
//  <N extends FeatureStructure> SelectFSs<N> index(FSIndex<N> index);
//
//  // If not specified defaults to the index's uppermost type.
//  // Methods take their generic type from the variable to which they are assigned except for
//  // type(class) which takes it from its argument.
//  <N extends FeatureStructure> SelectFSs<N> type(Type uimaType);
//  <N extends FeatureStructure> SelectFSs<N> type(String fullyQualifiedTypeName);
//  <N extends FeatureStructure> SelectFSs<N> type(int jcasClass_dot_type);
//  <N extends FeatureStructure> SelectFSs<N> type(Class<N> jcasClass_dot_class);
    
//  SelectFSs<T> shift(int amount); // incorporated into startAt 
  
  // ---------------------------------
  // boolean operations
  // ---------------------------------

//  SelectFSs<T> matchType();      // exact type match (no subtypes)
//  SelectFSs<T> matchType(boolean matchType); // exact type match (no subtypes)
  
  // only for AnnotationIndex
  SelectFSs<T> typePriority();
  SelectFSs<T> typePriority(boolean typePriority);

  SelectFSs<T> positionUsesType();           // ignored if not ordered index
  SelectFSs<T> positionUsesType(boolean positionUsesType); // ignored if not ordered index
  
  // Filters while iterating over Annotations
  SelectFSs<T> nonOverlapping();  // AI known as unambiguous
  SelectFSs<T> nonOverlapping(boolean nonOverlapping); // AI
  
  SelectFSs<T> endWithinBounds();  // AI known as "strict"
  SelectFSs<T> endWithinBounds(boolean endWithinBounds); // AI

  SelectFSs<T> skipEquals();                 
  SelectFSs<T> skipEquals(boolean skipEquals);
  
  // Miscellaneous
  SelectFSs<T> allViews();
  SelectFSs<T> allViews(boolean allViews);
  
  SelectFSs<T> nullOK();  // applies to get()
  SelectFSs<T> nullOK(boolean nullOk);  // applies to get()
    
  SelectFSs<T> unordered();                  // ignored if not ordered index
  SelectFSs<T> unordered(boolean unordered); // ignored if not ordered index
  
  SelectFSs<T> backwards();                  // ignored if not ordered index
  SelectFSs<T> backwards(boolean backwards); // ignored if not ordered index

//  SelectFSs<T> noSubtypes();
//  SelectFSs<T> noSubtypes(boolean noSubtypes);

  
  // ---------------------------------
  // starting position specification
  // 
  // Variations, controlled by: 
  //   * typePriority
  //   * positionUsesType
  //   
  // The positional specs imply starting at the 
  //   - left-most (if multiple) FS at that position, or
  //   - if no FS at the position, the next higher FS
  //   - if !typePriority, equal test is only begin/end 
  //     -- types ignored or not depending on positionUsesType 
  //    
  // shifts, if any, occur afterwards
  //   - can be positive or negative
  // ---------------------------------
  SelectFSs<T> shifted(int shiftAmount); 
  
  SelectFSs<T> startAt(TOP fs);  // Ordered
  SelectFSs<T> startAt(int begin, int end);   // AI
  
  SelectFSs<T> startAt(TOP fs, int shift);        // Ordered
  SelectFSs<T> startAt(int begin, int end, int shift);   // AI
    
  SelectFSs<T> limit(int n); 
  // ---------------------------------
  // subselection based on bounds
  //   - uses 
  //     -- typePriority, 
  //     -- positionUsesType, 
  //     -- skipEquals
  // ---------------------------------
  SelectFSs<T> at(AnnotationFS fs);  // AI
  SelectFSs<T> at(int begin, int end);  // AI
  
  SelectFSs<T> coveredBy(AnnotationFS fs);       // AI
  SelectFSs<T> coveredBy(int begin, int end);       // AI
  
  SelectFSs<T> covering(AnnotationFS fs);      // AI
  SelectFSs<T> covering(int begin, int end);      // AI
  
  SelectFSs<T> between(AnnotationFS fs1, AnnotationFS fs2);  // AI implies a coveredBy style
 
  /* ---------------------------------
  * Semantics: 
  *   - following uimaFIT
  *   - must be annotation subtype, annotation index
  *   - following: move to first fs where begin > pos.end
  *   - preceding: move to first fs where end < pos.begin
  *   
  *   - return the limit() or all
  *   - for preceding, return in forward order (unless backward is specified)
  *   - for preceding, skips FSs whose end >= begin (adjusted by offset)
  * ---------------------------------*/
  SelectFSs<T> following(TOP fs);
  SelectFSs<T> following(int begin, int end);
  SelectFSs<T> following(TOP fs, int offset);
  SelectFSs<T> following(int begin, int end, int offset);

  SelectFSs<T> preceding(TOP fs);
  SelectFSs<T> preceding(int begin, int end);
  SelectFSs<T> preceding(TOP fs, int offset);
  SelectFSs<T> preceding(int begin, int end, int offset);
  // ---------------------------------
  // terminal operations
  // returning other than SelectFSs
  // 
  // ---------------------------------
  FSIterator<T> fsIterator();
  Iterator<T> iterator();
  <N extends T> List<N> asList();
  <N extends T> N[] asArray(Class<N> clazz);
  Spliterator<T> spliterator();
  
  // returning one item
  
  T get();          // returns first element or null if empty (unless nullOK(false) specified)
  T single();       // throws if not exactly 1 element, throws if null
  T singleOrNull(); // throws if more than 1 element, returns single or null
   // next are positioning alternatives
   // get(...) throws if null (unless nullOK specified)
  T get(int offset);          // returns first element or null if empty after positioning
  T single(int offset);       // throws if not exactly 1 element
  T singleOrNull(int offset); // throws if more than 1 element, returns single or null  
  T get(TOP fs);          // returns first element or null if empty after positioning
  T single(TOP fs);       // throws if not exactly 1 element
  T singleOrNull(TOP fs); // throws if more than 1 element, returns single or null
  T get(TOP fs, int offset);          // returns first element or null if empty
  T single(TOP fs, int offset);       // throws if not exactly 1 element
  T singleOrNull(TOP fs, int offset); // throws if more than 1 element, returns single or null
  T get(int begin, int end);          // returns first element or null if empty
  T single(int begin, int end);       // throws if not exactly 1 element
  T singleOrNull(int begin, int end); // throws if more than 1 element, returns single or null
  T get(int begin, int end, int offset);          // returns first element or null if empty
  T single(int begin, int end, int offset);       // throws if not exactly 1 element
  T singleOrNull(int begin, int end, int offset); // throws if more than 1 element, returns single or null
  
  
  /**
   * static methods that more effectively capture the generic argument
   */
  /**
   * @param index
   * @return
   */
  static <U extends FeatureStructure, V extends U> SelectFSs<V> sselect(FSIndex<U> index) {
    return index.select();    
  } 
  
//  /**
//   * DON'T USE THIS, use index.select(XXX.class) instead
//   * @param index the index to use
//   * @param clazz the JCas class
//   * @return a select instance for this index and type
//   */
//  static <U extends FeatureStructure, V extends U> SelectFSs<V> sselect(FSIndex<U> index, Class<V> clazz) {
//    return index.select(clazz);
//  }
 

}
