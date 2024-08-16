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

import org.apache.uima.cas.FSMatchConstraint;

/**
 * Implements a constraint embedded under a path. Optimize later.
 */
abstract class PathConstraint implements FSMatchConstraint {

  private static final long serialVersionUID = -866548380590006704L;
  protected ArrayList<String> featNames;

  protected PathConstraint() {
  }

  PathConstraint(ArrayList<String> featNames) {
    this();
    this.featNames = featNames;
  }

  @Override
  public String toString() {
    if (featNames == null) {
      return "";
    }
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < featNames.size(); i++) {
      if (i > 0) {
        buf.append('.');
      }
      buf.append(featNames.get(i));
    }
    return buf.toString();
  }
}
