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
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;

/**
 * Feature structure implementation.
 * 
 * This is the common super class of all Feature Structures
 * 
 *   including the JCAS (derived from TOP)
 *   and non JCas FSs
 * 
 * 
 * @version $Revision: 1.6 $
 */
public abstract class FeatureStructureImpl implements FeatureStructure, Cloneable {

	public abstract int getAddress();

	protected abstract CASImpl getCASImpl();

	public Type getType() {
	  return ((FeatureStructureImplC) this).getType();
	}
	
	public int getavoidcollisionTypeCode() {
    return ((FeatureStructureImplC) this)._getTypeImpl().getCode();
	}

	public void setFeatureValue(Feature feat, FeatureStructure fs) {
	  ((FeatureStructureImplC) this).setFeatureValue(feat, fs);
	}

	public void setIntValue(Feature feat, int val) {
    ((FeatureStructureImplC) this).setIntValue(feat, val);
  }

	public void setFloatValue(Feature feat, float val) {
    ((FeatureStructureImplC) this).setFloatValue(feat, val);
  }

	public void setStringValue(Feature feat, String val) {
    ((FeatureStructureImplC) this).setStringValue(feat, val);
  }

	public void setByteValue(Feature feat, byte val) throws CASRuntimeException {
    ((FeatureStructureImplC) this).setByteValue(feat, val);
  }

	public void setBooleanValue(Feature feat, boolean b) throws CASRuntimeException {
    ((FeatureStructureImplC) this).setBooleanValue(feat, b);
  }

	public void setShortValue(Feature feat, short val) throws CASRuntimeException {
    ((FeatureStructureImplC) this).setShortValue(feat, val);
  }

	public void setLongValue(Feature feat, long val) throws CASRuntimeException {
    ((FeatureStructureImplC) this).setLongValue(feat, val);
  }

	public void setDoubleValue(Feature feat, double val) throws CASRuntimeException {
    ((FeatureStructureImplC) this).setDoubleValue(feat, val);
  }

	public void setFeatureValueFromString(Feature feat, String s) throws CASRuntimeException {
    ((FeatureStructureImplC) this).setFeatureValueFromString(feat, s);
  }

	public FeatureStructure getFeatureValue(Feature feat) throws CASRuntimeException {
    return ((FeatureStructureImplC) this).getFeatureValue(feat);
  }

	public int getIntValue(Feature feat) {
    return ((FeatureStructureImplC) this).getIntValue(feat);
  }

	public float getFloatValue(Feature feat) throws CASRuntimeException {
    return ((FeatureStructureImplC) this).getFloatValue(feat);
  }
	
	public String getStringValue(Feature f) throws CASRuntimeException {
    return ((FeatureStructureImplC) this).getStringValue(f);
  }
	  
	public byte getByteValue(Feature feat) throws CASRuntimeException {
    return ((FeatureStructureImplC) this).getByteValue(feat);
  }

	public boolean getBooleanValue(Feature feat) throws CASRuntimeException {
    return ((FeatureStructureImplC) this).getBooleanValue(feat);
  }

	public short getShortValue(Feature feat) throws CASRuntimeException {
    return ((FeatureStructureImplC) this).getShortValue(feat);
  }

	public long getLongValue(Feature feat) throws CASRuntimeException {
    return ((FeatureStructureImplC) this).getLongValue(feat);
  }

	public double getDoubleValue(Feature feat) throws CASRuntimeException {
    return ((FeatureStructureImplC) this).getDoubleValue(feat);
  }

	public String getFeatureValueAsString(Feature feat) throws CASRuntimeException {
    return ((FeatureStructureImplC) this).getFeatureValueAsString(feat);
  }


	public String toString() {
    return ((FeatureStructureImplC) this).toString();
  }

	public String toString(int indent) {
    return ((FeatureStructureImplC) this).toString(indent);
  }

	public void prettyPrint(int indent, int incr, StringBuffer buf, boolean useShortNames) {
    ((FeatureStructureImplC) this).prettyPrint(indent, incr, buf, useShortNames);
  }

	public void prettyPrint(int indent, int incr, StringBuffer buf, boolean useShortNames, String s) {
    ((FeatureStructureImplC) this).prettyPrint(indent, incr, buf, useShortNames, s);
  }

	public void prettyPrint(int indent, int incr, StringBuffer buf, boolean useShortNames, String s,
			FeatureStructureImplC.PrintReferences printRefs) {
    ((FeatureStructureImplC) this).prettyPrint(
        indent, incr, new StringBuilder(buf), useShortNames, s, printRefs);
  }

	public Object clone() throws CASRuntimeException {
	  return ((FeatureStructureImplC) this).clone();
	}

}
