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

import java.util.HashSet;
import java.util.Set;

import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSTypeConstraint;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.internal.util.SortedIntSet;

/**
 * An implementation of the type constraint interface.
 */
class FSTypeConstraintImpl implements FSTypeConstraint {

	private static final long serialVersionUID = 7557683109761796280L;

	private Set<String> nameSet = new HashSet<String>();

	private transient SortedIntSet typeSet = new SortedIntSet();

	private transient TypeSystem ts;

	public boolean match(FeatureStructure fs) {
		compile(((FeatureStructureImpl) fs).getCAS().getTypeSystem());
		final FeatureStructureImpl fsi = (FeatureStructureImpl) fs;
		final int typeCode = fsi.getCASImpl().getHeapValue(fsi.getAddress());
		TypeSystemImpl tsi = (TypeSystemImpl) this.ts;
		for (int i = 0; i < typeSet.size(); i++) {
			if (tsi.subsumes(typeSet.get(i), typeCode)) {
				return true;
			}
		}
		return false;
	}

	private final void compile(TypeSystem ts1) {
		if (ts == ts1) {
			return;
		}
		ts = ts1;
		TypeSystemImpl tsi = (TypeSystemImpl) ts1;
		int typeCode;
		for (String typeName : nameSet) {
			typeCode = tsi.ll_getCodeForTypeName(typeName);
			if (typeCode < tsi.getSmallestType()) {
				CASRuntimeException e = new CASRuntimeException(
						CASRuntimeException.UNKNOWN_CONSTRAINT_TYPE, new String[] { typeName });
				throw e;
			}
			typeSet.add(typeCode);
		}
	}

	public void add(Type type) {
		this.ts = null; // This will force a recompile.
		nameSet.add(type.getName());
	}

	public void add(String type) {
		ts = null; // Will force recompile.
		nameSet.add(type);
	}

	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("isa ( ");
		boolean start = true;
		for (String name : nameSet) {
			if (start) {
				start = false;
			} else {
				buf.append("| ");
			}
			buf.append(name);
			buf.append(' ');
		}
		buf.append(')');
		return buf.toString();
	}

}
