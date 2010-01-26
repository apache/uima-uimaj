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

	public FSTypeConstraint createTypeConstraint() {
		return new FSTypeConstraintImpl();
	}

	public FSIntConstraint createIntConstraint() {
		return new FSIntConstraintImpl();
	}

	public FSFloatConstraint createFloatConstraint() {
		return new FSFloatConstraintImpl();
	}

	public FSStringConstraint createStringConstraint() {
		return new FSStringConstraintImpl();
	}

	public FSBooleanConstraint createBooleanConstraint() {
		return new FSBooleanConstraintImpl();
	}

	public FSMatchConstraint embedConstraint(FeaturePath featPath,
			FSConstraint constraint) {
		ArrayList<String> path = new ArrayList<String>();
		for (int i = 0; i < featPath.size(); i++) {
			path.add(featPath.getFeature(i).getShortName());
		}
		if (constraint instanceof FSMatchConstraint) {
			return new EmbeddedConstraint(path, constraint);
		} else if (constraint instanceof FSIntConstraint) {
			return new IntConstraint(path, (FSIntConstraint) constraint);
		} else if (constraint instanceof FSFloatConstraint) {
			return new FloatConstraint(path, (FSFloatConstraint) constraint);
		} else if (constraint instanceof FSStringConstraint) {
			return new StringConstraint(path, (FSStringConstraint) constraint);
		} else if (constraint instanceof FSBooleanConstraint) {
			return new BooleanConstraint(path, (FSBooleanConstraint) constraint);
		} else {
			return null;
		}
	}

	public FSMatchConstraint embedConstraint(ArrayList<String> path,
			FSConstraint constraint) {
		if (constraint instanceof FSMatchConstraint) {
			return new EmbeddedConstraint(path, constraint);
		} else if (constraint instanceof FSIntConstraint) {
			return new IntConstraint(path, (FSIntConstraint) constraint);
		} else if (constraint instanceof FSFloatConstraint) {
			return new FloatConstraint(path, (FSFloatConstraint) constraint);
		} else if (constraint instanceof FSStringConstraint) {
			return new StringConstraint(path, (FSStringConstraint) constraint);
		} else if (constraint instanceof FSBooleanConstraint) {
			return new BooleanConstraint(path, (FSBooleanConstraint) constraint);
		} else {
			return null;
		}
	}

	public FSMatchConstraint and(FSMatchConstraint c1, FSMatchConstraint c2) {
		return new ConjunctiveConstraint(c1, c2);
	}

	public FSMatchConstraint or(FSMatchConstraint c1, FSMatchConstraint c2) {
		return new DisjunctiveConstraint(c1, c2);
	}

}
