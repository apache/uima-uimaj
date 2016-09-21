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

import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Collection of builder style methods to specify selection of FSs from indexes
 * Comment codes:
 *   AI = implies AnnotationIndex
 *   Ordered = implies an ordered index not necessarily AnnotationIndex
 */
public interface SelectFSs<T extends FeatureStructure> {
  
  /**
   * If not specified, defaults to all FSs (unordered) unless AnnotationIndex implied
   */
  SelectFSs<T> index(String indexName);  
  SelectFSs<T> index(FSIndex<T> index);

  /**
   * if not specified defaults to the index's uppermost type.  
   */
  SelectFSs<T> type(Type uimaType);
  SelectFSs<T> type(String fullyQualifiedTypeName);
  SelectFSs<T> type(int jcasClass_dot_type);
  SelectFSs<T> type(Class<? extends TOP> jcasClass_dot_class);
    
//  SelectFSs<T> shift(int amount); // incorporated into startAt 
  
  /*********************************
   * boolean operations
   *********************************/
  
  /**
   * Filters while iterating
   **/
  
  SelectFSs<T> nonOverlapping();  // AI known as unambiguous
  SelectFSs<T> nonOverlapping(boolean nonOverlapping); // AI
  
  SelectFSs<T> endWithinBounds();  // AI known as "strict"
  SelectFSs<T> endWithinBounds(boolean endWithinBounds); // AI
  
//  SelectFSs<T> useTypePriorities();
//  SelectFSs<T> useTypePriorities(boolean useTypePriorities);
  
  /**
   * Miscellaneous
   **/
  
  SelectFSs<T> allViews();
  SelectFSs<T> allViews(boolean allViews);
  
  SelectFSs<T> nullOK();  // applies to get() and single()
  SelectFSs<T> nullOK(boolean nullOk);  // applies to get() and single()
    
  SelectFSs<T> unordered();                  // ignored if not ordered index
  SelectFSs<T> unordered(boolean unordered); // ignored if not ordered index
  
  SelectFSs<T> backwards();                  // ignored if not ordered index
  SelectFSs<T> backwards(boolean backwards); // ignored if not ordered index
  
//  SelectFSs<T> noSubtypes();
//  SelectFSs<T> noSubtypes(boolean noSubtypes);

  /*********************************
   * bounding limits specified
   * as part of subselection style
   *********************************/
  
  /*********************************
   * starting position specification
   *********************************/
  SelectFSs<T> startAt(TOP fs);  // Ordered
  SelectFSs<T> startAt(int begin, int end);   // AI
  
  SelectFSs<T> startAt(TOP fs, int shift);        // Ordered
  SelectFSs<T> startAt(int begin, int end, int shift);   // AI
    
  /*********************************
   * subselection based on bounds
   *********************************/
  SelectFSs<T> at(Annotation fs);  // AI
  SelectFSs<T> at(int begin, int end);  // AI
  
  SelectFSs<T> coveredBy(Annotation fs);       // AI
  SelectFSs<T> coveredBy(int begin, int end);       // AI
  SelectFSs<T> covering(Annotation fs);      // AI
  SelectFSs<T> covering(int begin, int end);      // AI
  
  SelectFSs<T> between(Annotation fs1, Annotation fs2);  // AI implies a coveredBy style
  
  /*********************************
   * terminal operations
   * returning other than SelectFSs
   *********************************/
  <T extends FeatureStructure> FSIterator<T> fsIterator();
  <T extends FeatureStructure> Iterator<T> iterator();
  <T extends FeatureStructure> List<T> asList();
  <T extends FeatureStructure> Spliterator<T> spliterator();
  <T extends FeatureStructure> T get();
  <T extends FeatureStructure> T single();
  
  /********************************************
   * The methods below are alternatives 
   * to the methods above, that combine
   * frequently used patterns into more
   * concise forms using positional arguments
   ********************************************/
  
}
