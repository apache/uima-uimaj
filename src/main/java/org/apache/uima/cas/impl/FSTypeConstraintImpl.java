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

import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSTypeConstraint;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.internal.util.SortedIntSet;

import java.util.HashMap;
import java.util.Iterator;

/**
 * An implementation of the type constraint interface.
 * 
 * 
 * @version $Revision: 1.2 $
 */
class FSTypeConstraintImpl implements FSTypeConstraint {

  private HashMap nameMap;

  private transient SortedIntSet typeSet = new SortedIntSet();

  private transient TypeSystem ts;

  FSTypeConstraintImpl() {
    super();
    this.nameMap = new HashMap();
  }

  // FSTypeConstraintImpl(TypeSystem ts) {
  // this();
  // this.ts = ts;
  // }

  // FSTypeConstraintImpl(String typeName) {
  // super();
  // this.typeName = typeName;
  // this.ts = null;
  // this.type = null;
  // }
  //
  // FSTypeConstraintImpl(Type type, TypeSystem ts) {
  // super();
  // this.type = type;
  // this.ts = ts;
  // }

  public boolean match(FeatureStructure fs) {
    compile(((FeatureStructureImpl) fs).getCAS().getTypeSystem());
    final FeatureStructureImpl fsi = (FeatureStructureImpl) fs;
    final int typeCode = fsi.getCASImpl().getHeapValue(fsi.getAddress());
    TypeSystemImpl tsi = (TypeSystemImpl) this.ts;
    for (int i = 0; i < this.typeSet.size(); i++) {
      if (tsi.subsumes(this.typeSet.get(i), typeCode)) {
        return true;
      }
    }
    return false;
  }

  private final void compile(TypeSystem ts1) {
    if (this.ts == ts1) {
      return;
    }
    this.ts = ts1;
    TypeSystemImpl tsi = (TypeSystemImpl) ts1;
    Iterator it = this.nameMap.keySet().iterator();
    String typeName;
    int typeCode;
    while (it.hasNext()) {
      typeName = (String) it.next();
      typeCode = tsi.getTypeCode(typeName);
      if (typeCode < tsi.getSmallestType()) {
        CASRuntimeException e = new CASRuntimeException(CASRuntimeException.UNKNOWN_CONSTRAINT_TYPE);
        e.addArgument(typeName);
        throw e;
      }
      this.typeSet.add(typeCode);
    }
  }

  public void add(Type type) {
    this.ts = null; // This will force a recompile.
    this.nameMap.put(type.getName(), null);
  }

  public void add(String type) {
    this.ts = null; // Will force recompile.
    this.nameMap.put(type, null);
  }

  public String toString() {
    Iterator it = this.nameMap.keySet().iterator();
    StringBuffer buf = new StringBuffer();
    buf.append("isa ( ");
    boolean start = true;
    while (it.hasNext()) {
      if (start) {
        start = false;
      } else {
        buf.append("| ");
      }
      buf.append((String) it.next());
      buf.append(" ");
    }
    buf.append(")");
    return buf.toString();
  }

}
