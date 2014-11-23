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

import java.util.ArrayList;

import org.apache.uima.cas.FSBooleanConstraint;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;

/**
 * Implementation of boolean match constraint.  See interface for documentation.
 */
public class BooleanConstraint extends PathConstraint implements FSMatchConstraint {

  private final FSBooleanConstraint cons;
  
  BooleanConstraint(ArrayList<String> path, FSBooleanConstraint constraint) {
    super(path);
    this.cons = constraint;
  }

  public boolean match(FeatureStructure fs) {
    final int max = this.featNames.size() - 1; // The last position in the
    // path!
    if (max < 0) {
      // If the path is empty, we can't get a boolean, and therefore the
      // constraint is not satisfied.
      return false;
    }
    Feature feat;
    for (int i = 0; i < max; i++) {
      feat = fs.getType().getFeatureByBaseName(this.featNames.get(i));
      if (feat == null) {
        return false;
      }
      fs = fs.getFeatureValue(feat);
    }
    feat = fs.getType().getFeatureByBaseName(this.featNames.get(max));
    if (feat == null) {
      return false;
    }
    return this.cons.match(fs.getBooleanValue(feat));
  }

}
