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

/**
 * Collection of builder style methods to specify selection of FSs from indexes
 * Comment codes:
 *   AI = implies AnnotationIndex
 */
public interface SelectFSs {
  
  SelectFSs index(String indexName);  
  SelectFSs index(FSIndex<?> index);

  
  SelectFSs type(Type uimaType);
  SelectFSs type(String fullyQualifiedTypeName);
  SelectFSs type(int jcasClass_dot_type);
  SelectFSs type(Class<?> jcasClass_dot_class);
    
  SelectFSs shift(int amount); 
  
  
  /*********************************
   * boolean operations
   *********************************/
  SelectFSs nonOverlapping();  // AI known as unambiguous
  SelectFSs nonOverlapping(boolean nonOverlapping); // AI
  
  SelectFSs endWithinBounds();  // AI known as "strict"
  SelectFSs endWithinBounds(boolean endWithinBounds); // AI
  
//  SelectFSs useTypePriorities();
//  SelectFSs useTypePriorities(boolean useTypePriorities);
  
  SelectFSs allViews();
  SelectFSs allViews(boolean allViews);
  
  SelectFSs nullOK();
  SelectFSs nullOK(boolean nullOk);
  
//  SelectFSs noSubtypes();
//  SelectFSs noSubtypes(boolean noSubtypes);
  
  SelectFSs unordered();
  SelectFSs unordered(boolean unordered);
  
  SelectFSs backwards();
  SelectFSs backwards(boolean backwards);
  
    

  /*********************************
   * bounding limits
   *********************************/
  SelectFSs at(FeatureStructure fs);  // AI
  SelectFSs at(int begin, int end);   // AI
  SelectFSs between(FeatureStructure fs1, FeatureStructure fs2);  // AI
  
  /*********************************
   * subselection based on bounds
   *********************************/
  SelectFSs sameBeginEnd();  // AI
  SelectFSs covered();       // AI
  SelectFSs covering();      // AI
  
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
  
  // empty for now
}
