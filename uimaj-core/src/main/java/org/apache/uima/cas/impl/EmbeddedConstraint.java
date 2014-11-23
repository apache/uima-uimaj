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

import org.apache.uima.cas.FSConstraint;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;

/**
 * Implement an embedded constraint. Basically just dispatches to specialized implementation.
 * 
 * 
 * @version $Revision: 1.1 $
 */
class EmbeddedConstraint extends PathConstraint implements FSMatchConstraint {

  private static final long serialVersionUID = -4938331720540592033L;

  private FSMatchConstraint cons;

  EmbeddedConstraint(ArrayList<String> path, FSConstraint cons) {
    super(path);
    this.cons = (FSMatchConstraint) cons;
  }

  public boolean match(FeatureStructure fs) {
    // compile(((FeatureStructureImpl) fs).getCAS().getTypeSystem());
    final int max = this.featNames.size();
    for (int i = 0; i < max; i++) {
      Feature feat = fs.getType().getFeatureByBaseName(this.featNames.get(i));
      if (feat == null) {
        return false;
      }
      fs = fs.getFeatureValue(feat);
    }
    return this.cons.match(fs);
  }

  public String toString() {
    return super.toString() + ".( " + this.cons.toString() + " )";
  }

}
