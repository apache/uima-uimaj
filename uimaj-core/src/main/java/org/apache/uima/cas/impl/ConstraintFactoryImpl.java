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

import org.apache.uima.cas.ConstraintFactory;
import org.apache.uima.cas.FSBooleanConstraint;
import org.apache.uima.cas.FSConstraint;
import org.apache.uima.cas.FSFloatConstraint;
import org.apache.uima.cas.FSIntConstraint;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.FSStringConstraint;
import org.apache.uima.cas.FSTypeConstraint;
import org.apache.uima.cas.FeaturePath;

/**
 * Implementation of the ConstraintFactory interface.
 * 
 * 
 */
public class ConstraintFactoryImpl extends ConstraintFactory {

  @Override
  public FSTypeConstraint createTypeConstraint() {
    return new FSTypeConstraintImpl();
  }

  @Override
  public FSIntConstraint createIntConstraint() {
    return new FSIntConstraintImpl();
  }

  @Override
  public FSFloatConstraint createFloatConstraint() {
    return new FSFloatConstraintImpl();
  }

  @Override
  public FSStringConstraint createStringConstraint() {
    return new FSStringConstraintImpl();
  }

  @Override
  public FSBooleanConstraint createBooleanConstraint() {
    return new FSBooleanConstraintImpl();
  }

  @Override
  public FSMatchConstraint embedConstraint(FeaturePath featPath, FSConstraint constraint) {
    ArrayList<String> path = new ArrayList<>();
    for (int i = 0; i < featPath.size(); i++) {
      path.add(featPath.getFeature(i).getShortName());
    }
    if (constraint instanceof FSMatchConstraint) {
      return new EmbeddedConstraint(path, constraint);
    } else if (constraint instanceof FSIntConstraint fsIntConstraint) {
      return new IntConstraint(path, fsIntConstraint);
    } else if (constraint instanceof FSFloatConstraint fsFloatConstraint) {
      return new FloatConstraint(path, fsFloatConstraint);
    } else if (constraint instanceof FSStringConstraint fsStringConstraint) {
      return new StringConstraint(path, fsStringConstraint);
    } else if (constraint instanceof FSBooleanConstraint fsBooleanConstraint) {
      return new BooleanConstraint(path, fsBooleanConstraint);
    } else {
      return null;
    }
  }

  @Override
  public FSMatchConstraint embedConstraint(ArrayList<String> path, FSConstraint constraint) {
    if (constraint instanceof FSMatchConstraint) {
      return new EmbeddedConstraint(path, constraint);
    } else if (constraint instanceof FSIntConstraint fsIntConstraint) {
      return new IntConstraint(path, fsIntConstraint);
    } else if (constraint instanceof FSFloatConstraint fsFloatConstraint) {
      return new FloatConstraint(path, fsFloatConstraint);
    } else if (constraint instanceof FSStringConstraint fsStringConstraint) {
      return new StringConstraint(path, fsStringConstraint);
    } else if (constraint instanceof FSBooleanConstraint fsBooleanConstraint) {
      return new BooleanConstraint(path, fsBooleanConstraint);
    } else {
      return null;
    }
  }

  @Override
  public FSMatchConstraint and(FSMatchConstraint c1, FSMatchConstraint c2) {
    return new ConjunctiveConstraint(c1, c2);
  }

  @Override
  public FSMatchConstraint or(FSMatchConstraint c1, FSMatchConstraint c2) {
    return new DisjunctiveConstraint(c1, c2);
  }

}
