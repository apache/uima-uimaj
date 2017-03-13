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

import org.apache.uima.cas.FSIntConstraint;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;

/**
 * Implement an embedded int constraint.
 * 
 * 
 * @version $Revision: 1.1 $
 */
class IntConstraint extends PathConstraint implements FSMatchConstraint {

  private static final long serialVersionUID = 1661917047353431836L;

  private FSIntConstraint intConstraint;

  IntConstraint(ArrayList<String> path, FSIntConstraint cons) {
    super(path);
    this.intConstraint = cons;
  }

  public boolean match(FeatureStructure fs) {
    // compile(((FeatureStructureImpl) fs).getCAS().getTypeSystem());
    final int max = this.featNames.size() - 1; // The last position in the
    // path!
    if (max < 0) {
      // If the path is empty, we can't get an int, and therefore the
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
    return this.intConstraint.match(fs.getIntValue(feat));
  }

  public String toString() {
    return super.toString() + " " + this.intConstraint.toString();
  }

}
