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

import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.FeatureStructure;

/**
 * Implements a conjunctive constraint.
 * 
 * 
 * @version $Revision: 1.1 $
 */
class ConjunctiveConstraint implements FSMatchConstraint {

  private static final long serialVersionUID = 2306345325747964023L;

  private FSMatchConstraint c1;

  private FSMatchConstraint c2;

  private ConjunctiveConstraint() {
    super();
  }

  /**
   * Create a conjunctive constraint from two FSMatchConstraints.
   * 
   * @param c1
   *          First conjunct.
   * @param c2
   *          Second conjunct.
   */
  ConjunctiveConstraint(FSMatchConstraint c1, FSMatchConstraint c2) {
    this();
    this.c1 = c1;
    this.c2 = c2;
  }

  public boolean match(FeatureStructure fs) {
    return (this.c1.match(fs) && this.c2.match(fs));
  }

  public String toString() {
    return "(" + this.c1.toString() + " & " + this.c2.toString() + ")";
  }

}
