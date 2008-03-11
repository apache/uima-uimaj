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

package org.apache.uima.cas.impl;

import org.apache.uima.cas.FeatureStructure;

/**
 * Interface to compare two feature structures.
 * 
 * 
 * @version $Revision: 1.1 $
 */
public interface FSComparator {

  /**
   * Compare two FSs.
   * 
   * @param fs1
   *          First feature structure.
   * @param fs2
   *          Second feature structure.
   * @return <code>-1</code>, if <code>fs1</code> is "smaller" than <code>fs2</code>;
   *         <code>1</code>, if <code>fs2</code> is smaller than <code>fs1</code>; and
   *         <code>0</code>, if <code>fs1</code> equals <code>fs2</code>.
   */
  int compare(FeatureStructure fs1, FeatureStructure fs2);

}
