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

package org.apache.uima.caseditor.editor.util;

import org.apache.uima.cas.FSConstraint;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;

/**
 * Matches all annotations of an added type.
 */
public class StrictTypeConstraint implements FSConstraint, FSMatchConstraint {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1;

  /** The m match. */
  private Type mMatch;

  /**
   * Initializes the current instance.
   *
   * @param match
   *          the match
   */
  public StrictTypeConstraint(Type match) {
    mMatch = match;
  }

  /**
   * Checks if the given {@link FeatureStructure} matches this constraint.
   *
   * @param candidateFS
   *          the candidate FS
   * @return true, if successful
   */
  @Override
  public boolean match(FeatureStructure candidateFS) {
    return candidateFS.getType().getName().equals(mMatch.getName());
  }
}
