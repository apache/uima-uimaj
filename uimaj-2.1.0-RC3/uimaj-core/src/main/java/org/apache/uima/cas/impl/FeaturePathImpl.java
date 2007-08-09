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

import java.util.Vector;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeaturePath;

/**
 * Implements a feature path (finite sequence of features).
 * 
 * 
 * @version $Revision: 1.2 $
 */
class FeaturePathImpl implements FeaturePath {

  private Vector path;

  FeaturePathImpl() {
    super();
    this.path = new Vector();
  }

  public int size() {
    return this.path.size();
  }

  public Feature getFeature(int i) {
    return (Feature) this.path.get(i);
  }

  public void addFeature(Feature feat) {
    this.path.add(feat);
  }

}
