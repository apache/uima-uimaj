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

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.Type;
import org.apache.uima.internal.util.IntSet;
import org.apache.uima.internal.util.StringUtils;
import org.apache.uima.internal.util.rb_trees.RedBlackTree;

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
		return this.getCASImpl().getTypeSystemImpl().ll_getTypeForCode(
				this.getCASImpl().getHeapValue(this.getAddress()));
	}
	
	public int getavoidcollisionTypeCode() {
	  return this.getCASImpl().getHeapValue(this.getAddress());
	}

	public void setFeatureValue(Feature feat, FeatureStructure fs) {
		final int valueAddr = this.getCASImpl().ll_getFSRef(fs);
		final int featCode = ((FeatureImpl) feat).getCode();
		final int rangeType = this.getCASImpl().getTypeSystemImpl().range(featCode);
		if (valueAddr == CASImpl.NULL) {
			setNullValue(featCode, rangeType);
			return;
		}
		final int thisType = this.getCASImpl().getHeapValue(this.getAddress());
		final int valueType = this.getCASImpl().getHeapValue(valueAddr);
		if (!this.getCASImpl().getTypeSystemImpl().isApprop(thisType, featCode)) {
			CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_TYPE,
					new String[] { feat.getName(), this.getType().getName() });
			throw e;
		}
		if (!this.getCASImpl().getTypeSystemImpl().subsumes(rangeType, valueType)) {
			CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_RANGE,
					new String[] { feat.getName(), feat.getRange().getName(), fs.getType().getName() });
			throw e;
		}
		// keys are not fsRefs
		this.getCASImpl().setFeatureValueNoIndexCorruptionCheck(this.getAddress(), featCode, valueAddr);
	}

	private final void setNullValue(int featCode, int rangeType) {
		if (this.getCASImpl().isIntType(rangeType) || this.getCASImpl().isFloatType(rangeType)
				|| this.getCASImpl().isStringType(rangeType)) {
			CASRuntimeException e = new CASRuntimeException(CASRuntimeException.PRIMITIVE_VAL_FEAT,
					new String[] { this.getCASImpl().getTypeSystemImpl().ll_getFeatureForCode(featCode).getName() });
			throw e;
		}
		// a null fsref is never an index key
		this.getCASImpl().setFeatureValueNoIndexCorruptionCheck(this.getAddress(), featCode, CASImpl.NULL);
	}

	public void setIntValue(Feature feat, int val) {
		final TypeSystemImpl ts = this.getCASImpl().getTypeSystemImpl();
		if (!ts.subsumes(((TypeImpl) feat.getDomain()).getCode(), this.getCASImpl().getHeapValue(
				this.getAddress()))) {
			throwUndefinedFeatureExc(feat, getType());
		}
		if (!ts.subsumes(((TypeImpl) feat.getRange()).getCode(), ((TypeImpl) ts
				.getType(CAS.TYPE_NAME_INTEGER)).getCode())) {
			throwIllegalRangeExc(feat, ts.getType(CAS.TYPE_NAME_INTEGER));
		}
		final int featCode = ((FeatureImpl) feat).getCode();
		this.getCASImpl().setFeatureValue(this.getAddress(), featCode, val);
	}

	public void setFloatValue(Feature feat, float val) {
		final TypeSystemImpl ts = this.getCASImpl().getTypeSystemImpl();
		if (!ts.subsumes(((TypeImpl) feat.getDomain()).getCode(), this.getCASImpl().getHeapValue(
				this.getAddress()))) {
			throwUndefinedFeatureExc(feat, getType());
		}
		if (!ts.subsumes(((TypeImpl) feat.getRange()).getCode(), ((TypeImpl) ts
				.getType(CAS.TYPE_NAME_FLOAT)).getCode())) {
			throwIllegalRangeExc(feat, ts.getType(CAS.TYPE_NAME_FLOAT));
		}
		final int featCode = ((FeatureImpl) feat).getCode();
		this.getCASImpl().setFloatValue(this.getAddress(), featCode, val);
	}

	public void setStringValue(Feature feat, String val) {
		final TypeSystemImpl ts = this.getCASImpl().getTypeSystemImpl();
		final int featCode = ((FeatureImpl) feat).getCode();
		final int rangeType = ts.range(featCode);
		final int thisType = this.getCASImpl().getHeapValue(this.getAddress());
		final int stringType = ((TypeImpl) this.getCASImpl().getTypeSystem().getType(
				CAS.TYPE_NAME_STRING)).getCode();
		if (!ts.isApprop(thisType, featCode)) {
			CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_FEAT,
					new String[] { feat.getName(), this.getType().getName() });
			throw e;
		}
		if (!ts.subsumes(stringType, rangeType)) {
			CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_TYPE,
					new String[] { feat.getRange().getName(),
							this.getCAS().getTypeSystem().getType(CAS.TYPE_NAME_STRING).getName() });
			throw e;
		}
		this.getCAS().getLowLevelCAS().ll_setStringValue(this.getAddress(), featCode, val);
	}

	public void setByteValue(Feature feat, byte val) throws CASRuntimeException {

		final TypeSystemImpl ts = this.getCASImpl().getTypeSystemImpl();
		if (!ts.subsumes(((TypeImpl) feat.getDomain()).getCode(), this.getCASImpl().getHeapValue(
				this.getAddress()))) {
			throwUndefinedFeatureExc(feat, getType());
		}
		if (!ts.subsumes(((TypeImpl) feat.getRange()).getCode(), ((TypeImpl) ts
				.getType(CAS.TYPE_NAME_BYTE)).getCode())) {
			throwIllegalRangeExc(feat, ts.getType(CAS.TYPE_NAME_BYTE));
		}
		final int featCode = ((FeatureImpl) feat).getCode();

		this.getCASImpl().setFeatureValue(this.getAddress(), featCode, val);
	}

	public void setBooleanValue(Feature feat, boolean b) throws CASRuntimeException {

		final TypeSystemImpl ts = this.getCASImpl().getTypeSystemImpl();
		if (!ts.subsumes(((TypeImpl) feat.getDomain()).getCode(), this.getCASImpl().getHeapValue(
				this.getAddress()))) {
			throwUndefinedFeatureExc(feat, getType());
		}
		if (!ts.subsumes(((TypeImpl) feat.getRange()).getCode(), ((TypeImpl) ts
				.getType(CAS.TYPE_NAME_BOOLEAN)).getCode())) {
			throwIllegalRangeExc(feat, ts.getType(CAS.TYPE_NAME_BOOLEAN));
		}
		final int featCode = ((FeatureImpl) feat).getCode();
		this.getCASImpl().setFeatureValue(this.getAddress(), featCode, b);

	}

	public void setShortValue(Feature feat, short val) throws CASRuntimeException {
		final TypeSystemImpl ts = this.getCASImpl().getTypeSystemImpl();
		if (!ts.subsumes(((TypeImpl) feat.getDomain()).getCode(), this.getCASImpl().getHeapValue(
				this.getAddress()))) {
			throwUndefinedFeatureExc(feat, getType());
		}
		if (!ts.subsumes(((TypeImpl) feat.getRange()).getCode(), ((TypeImpl) ts
				.getType(CAS.TYPE_NAME_SHORT)).getCode())) {
			throwIllegalRangeExc(feat, ts.getType(CAS.TYPE_NAME_SHORT));
		}
		final int featCode = ((FeatureImpl) feat).getCode();
		this.getCASImpl().setFeatureValue(this.getAddress(), featCode, val);
	}

	public void setLongValue(Feature feat, long val) throws CASRuntimeException {
		final TypeSystemImpl ts = this.getCASImpl().getTypeSystemImpl();
		if (!ts.subsumes(((TypeImpl) feat.getDomain()).getCode(), this.getCASImpl().getHeapValue(
				this.getAddress()))) {
			throwUndefinedFeatureExc(feat, getType());
		}
		if (!ts.subsumes(((TypeImpl) feat.getRange()).getCode(), ((TypeImpl) ts
				.getType(CAS.TYPE_NAME_LONG)).getCode())) {
			throwIllegalRangeExc(feat, ts.getType(CAS.TYPE_NAME_LONG));
		}
		final int featCode = ((FeatureImpl) feat).getCode();
		this.getCASImpl().setFeatureValue(this.getAddress(), featCode, val);
	}

	public void setDoubleValue(Feature feat, double val) throws CASRuntimeException {
		final TypeSystemImpl ts = this.getCASImpl().getTypeSystemImpl();
		if (!ts.subsumes(((TypeImpl) feat.getDomain()).getCode(), this.getCASImpl().getHeapValue(
				this.getAddress()))) {
			throwUndefinedFeatureExc(feat, getType());
		}
		if (!ts.subsumes(((TypeImpl) feat.getRange()).getCode(), ((TypeImpl) ts
				.getType(CAS.TYPE_NAME_DOUBLE)).getCode())) {
			throwIllegalRangeExc(feat, ts.getType(CAS.TYPE_NAME_DOUBLE));
		}
		final int featCode = ((FeatureImpl) feat).getCode();
		this.getCASImpl().setFeatureValue(this.getAddress(), featCode, val);
	}

	public void setFeatureValueFromString(Feature feat, String s) throws CASRuntimeException {

		this.getCASImpl().setFeatureValueFromString(this.getAddress(), ((FeatureImpl) feat).getCode(),
				s);

	}

	public FeatureStructure getFeatureValue(Feature feat) throws CASRuntimeException {
		final TypeSystemImpl ts = this.getCASImpl().getTypeSystemImpl();
		// final int featCode = ts.getFeatureCode(feat.getName());
		final int featCode = ((FeatureImpl) feat).getCode();
		// assert(featCode > 0);
		if (!ts.isApprop(this.getCASImpl().getHeapValue(this.getAddress()), featCode)) {
			CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_FEAT,
					new String[] { feat.getName(), this.getType().getName() });
			throw e;
		}
		// Check that feature value is not primitive.
		final int rangeTypeCode = ts.ll_getRangeType(featCode);
		if (!this.getCASImpl().ll_isRefType(rangeTypeCode)) {
			CASRuntimeException e = new CASRuntimeException(CASRuntimeException.PRIMITIVE_VAL_FEAT,
					new String[] { feat.getName() });
			throw e;
		}
		final int valAddr = this.getCASImpl().getFeatureValue(this.getAddress(), featCode);
		// assert(valAddr > 0);
		return this.getCASImpl().createFS(valAddr);
	}

	public int getIntValue(Feature feat) {
		final TypeSystemImpl ts = this.getCASImpl().getTypeSystemImpl();
		if (!ts.subsumes(feat.getDomain(), getType())) {
			// System.out.println(
			// "Domain: "
			// + feat.getDomain().getName()
			// + ", type: "
			// + getType().getName());
			throwUndefinedFeatureExc(feat, getType());
		}
		if (!ts.subsumes(feat.getRange(), ts.getType(CAS.TYPE_NAME_INTEGER))) {
			throwIllegalRangeExc(feat, ts.getType(CAS.TYPE_NAME_INTEGER));
		}
		final int featCode = ((FeatureImpl) feat).getCode();
		return this.getCASImpl().getFeatureValue(this.getAddress(), featCode);
	}

	public float getFloatValue(Feature feat) throws CASRuntimeException {
		final TypeSystemImpl ts = this.getCASImpl().getTypeSystemImpl();
		if (!ts.subsumes(feat.getDomain(), getType())) {
			throwUndefinedFeatureExc(feat, getType());
		}
		if (!ts.subsumes(feat.getRange(), ts.getType(CAS.TYPE_NAME_FLOAT))) {
			throwIllegalRangeExc(feat, ts.getType(CAS.TYPE_NAME_FLOAT));
		}
		final int featCode = ((FeatureImpl) feat).getCode();
		return this.getCASImpl().getFloatValue(this.getAddress(), featCode);
	}

	public String getStringValue(Feature f) throws CASRuntimeException {
		final int thisType = ((TypeImpl) this.getType()).getCode();
		final int domType = ((TypeImpl) f.getDomain()).getCode();
		final int stringType = ((TypeImpl) this.getCASImpl().getTypeSystem().getType(
				CAS.TYPE_NAME_STRING)).getCode();
		final int rangeType = ((TypeImpl) f.getRange()).getCode();
		final TypeSystemImpl ts = (TypeSystemImpl) this.getCASImpl().getTypeSystem();
		if (!ts.subsumes(domType, thisType)) {
			throwUndefinedFeatureExc(f, getType());
		}
		if (!ts.subsumes(stringType, rangeType)) {
			throwIllegalRangeExc(f, this.getCASImpl().getTypeSystem().getType(CAS.TYPE_NAME_STRING));
		}
		// final int stringAddr = casImpl.getFeatureValue(this.getAddress(),
		// ((FeatureImpl)f).getCode());
		// return casImpl.getStringValue(stringAddr);
		return this.getCASImpl().getStringValue(this.getAddress(), ((FeatureImpl) f).getCode());
	}

	public byte getByteValue(Feature feat) throws CASRuntimeException {
		final TypeSystemImpl ts = this.getCASImpl().getTypeSystemImpl();
		if (!ts.subsumes(feat.getDomain(), getType())) {
			throwUndefinedFeatureExc(feat, getType());
		}
		if (!ts.subsumes(feat.getRange(), ts.getType(CAS.TYPE_NAME_BYTE))) {
			throwIllegalRangeExc(feat, ts.getType(CAS.TYPE_NAME_BYTE));
		}
		final int featCode = ((FeatureImpl) feat).getCode();
		return this.getCASImpl().getByteValue(this.getAddress(), featCode);
	}

	public boolean getBooleanValue(Feature feat) throws CASRuntimeException {
		final TypeSystemImpl ts = this.getCASImpl().getTypeSystemImpl();
		if (!ts.subsumes(feat.getDomain(), getType())) {
			throwUndefinedFeatureExc(feat, getType());
		}
		if (!ts.subsumes(feat.getRange(), ts.getType(CAS.TYPE_NAME_BOOLEAN))) {
			throwIllegalRangeExc(feat, ts.getType(CAS.TYPE_NAME_BOOLEAN));
		}
		final int featCode = ((FeatureImpl) feat).getCode();
		return this.getCASImpl().getBooleanValue(this.getAddress(), featCode);

	}

	public short getShortValue(Feature feat) throws CASRuntimeException {
		final TypeSystemImpl ts = this.getCASImpl().getTypeSystemImpl();
		if (!ts.subsumes(feat.getDomain(), getType())) {
			throwUndefinedFeatureExc(feat, getType());
		}
		if (!ts.subsumes(feat.getRange(), ts.getType(CAS.TYPE_NAME_SHORT))) {
			throwIllegalRangeExc(feat, ts.getType(CAS.TYPE_NAME_SHORT));
		}
		final int featCode = ((FeatureImpl) feat).getCode();
		return this.getCASImpl().getShortValue(this.getAddress(), featCode);
	}

	public long getLongValue(Feature feat) throws CASRuntimeException {
		final TypeSystemImpl ts = this.getCASImpl().getTypeSystemImpl();
		if (!ts.subsumes(feat.getDomain(), getType())) {
			throwUndefinedFeatureExc(feat, getType());
		}
		if (!ts.subsumes(feat.getRange(), ts.getType(CAS.TYPE_NAME_LONG))) {
			throwIllegalRangeExc(feat, ts.getType(CAS.TYPE_NAME_LONG));
		}
		final int featCode = ((FeatureImpl) feat).getCode();
		return this.getCASImpl().getLongValue(this.getAddress(), featCode);
	}

	public double getDoubleValue(Feature feat) throws CASRuntimeException {
		final TypeSystemImpl ts = this.getCASImpl().getTypeSystemImpl();
		if (!ts.subsumes(feat.getDomain(), getType())) {
			throwUndefinedFeatureExc(feat, getType());
		}
		if (!ts.subsumes(feat.getRange(), ts.getType(CAS.TYPE_NAME_DOUBLE))) {
			throwIllegalRangeExc(feat, ts.getType(CAS.TYPE_NAME_DOUBLE));
		}
		final int featCode = ((FeatureImpl) feat).getCode();
		return this.getCASImpl().getDoubleValue(this.getAddress(), featCode);
	}

	public String getFeatureValueAsString(Feature feat) throws CASRuntimeException {

		return this.getCASImpl().getFeatureValueAsString(this.getAddress(),
				((FeatureImpl) feat).getCode());

	}

	private static final void throwIllegalRangeExc(Feature f, Type t) throws CASRuntimeException {
		CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_RANGE,
				new String[] { f.getName(), t.getName(), f.getRange().getName() });
		throw e;
	}

	private static final void throwUndefinedFeatureExc(Feature f, Type t) throws CASRuntimeException {
		CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_FEAT,
				new String[] { f.getName(), t.getName() });
		throw e;
	}

	// ///////////////////////////////////////////////////////////////////////////
	// Pretty printing.

	private static class PrintReferences {

		static final int NO_LABEL = 0;

		static final int WITH_LABEL = 1;

		static final int JUST_LABEL = 2;

		private static final String refNamePrefix = "#";

		/**
		 * map from int (the FS) to Strings
		 *   3 states: key not in map
		 *             key in map, value of string = "seen once"
		 *             key in map, value of string = #nnnn  when value seen more than once
		 */
		private RedBlackTree<String> tree;

		private IntSet seen;

		private int count;

		private PrintReferences() {
			super();
			this.count = 0;
			this.tree = new RedBlackTree<String>();
			this.seen = new IntSet();
		}

		boolean addReference(int ref) {
			if (this.tree.containsKey(ref)) {
				String refName = this.tree.get(ref);
				if (refName == null) {
					refName = refNamePrefix + Integer.toString(this.count);
					++this.count;
					this.tree.put(ref, refName);
				}
				return true;
			}
			this.tree.put(ref, null);
			return false;
		}

		String getLabel(int ref) {
			return this.tree.get(ref);
		}

		int printInfo(int ref) {
			if (this.tree.get(ref) == null) {
				return NO_LABEL;
			}
			if (this.seen.contains(ref)) {
				return JUST_LABEL;
			}
			this.seen.add(ref);
			return WITH_LABEL;
		}

	}

	private final void getPrintRefs(PrintReferences printRefs) {
		getPrintRefs(printRefs, this.getAddress());
	}

	private final void getPrintRefs(PrintReferences printRefs, int ref) {
		boolean seenBefore = printRefs.addReference(ref);
		if (seenBefore) {
			return;
		}
		LowLevelCAS llcas = this.getCASImpl().getLowLevelCAS();
		LowLevelTypeSystem llts = llcas.ll_getTypeSystem();
		final int typeCode;
		try {
		  typeCode = llcas.ll_getFSRefType(ref, true);
		} catch (LowLevelException e) {
		  return;  // can't find ref, may be invalid or null
		}
		int[] feats = llts.ll_getAppropriateFeatures(typeCode);
		for (int i = 0; i < feats.length; i++) {
			if (llcas.ll_isRefType(llts.ll_getRangeType(feats[i]))) {
				int valRef = llcas.ll_getRefValue(ref, feats[i]);
				if (valRef != LowLevelCAS.NULL_FS_REF) {
					getPrintRefs(printRefs, valRef);
				}
			}
		}
	}

	public String toString() {
		return toString(3);
	}

	public String toString(int indent) {
		StringBuffer buf = new StringBuffer();
		prettyPrint(0, indent, buf, true, null);
		return buf.toString();
	}

	public void prettyPrint(int indent, int incr, StringBuffer buf, boolean useShortNames) {
		prettyPrint(indent, incr, buf, useShortNames, null);
	}

	public void prettyPrint(int indent, int incr, StringBuffer buf, boolean useShortNames, String s) {
		PrintReferences printRefs = new PrintReferences();
		getPrintRefs(printRefs);
		prettyPrint(indent, incr, buf, useShortNames, s, printRefs);
	}

	public void prettyPrint(int indent, int incr, StringBuffer buf, boolean useShortNames, String s,
			PrintReferences printRefs) {
	  final Type stringType = this.getCASImpl().getTypeSystem().getType(CAS.TYPE_NAME_STRING);
	  
		indent += incr;
		final int printInfo = printRefs.printInfo(this.getAddress());
		if (printInfo != PrintReferences.NO_LABEL) {
			buf.append(printRefs.getLabel(this.getAddress()));
			if (printInfo == PrintReferences.JUST_LABEL) {
				buf.append('\n');
				return;
			}
			buf.append(' ');
		}
		if (useShortNames) {
			buf.append(getType().getShortName());
		} else {
			buf.append(getType().getName());
		}
		if (s != null) {
			buf.append(" \"" + s + "\"");
		}
		buf.append('\n');
		
		CommonAuxArrayFSImpl arrayFS = null;
		LowLevelTypeSystem llts = this.getCASImpl().ll_getTypeSystem();
		final int typeClass = this.getCASImpl().ll_getTypeClass(llts.ll_getCodeForType(this.getType()));

		if (typeClass == LowLevelCAS.TYPE_CLASS_STRINGARRAY) {
			final int arrayLen = getCASImpl().ll_getArraySize(this.getAddress());
			StringUtils.printSpaces(indent, buf);
			buf.append("Array length: " + arrayLen + "\n");
			if (arrayLen > 0) {
				StringUtils.printSpaces(indent, buf);
				buf.append("Array elements: [");
				for (int i = 0; i < arrayLen; i++) {
					if (i > 0) {
						buf.append(", ");
					}
					String element = this.getCASImpl().ll_getStringArrayValue(this.getAddress(), i);
					buf.append("\"" + element + "\"");
				}
				buf.append("]\n");
			}

		} else if (typeClass == LowLevelCAS.TYPE_CLASS_INTARRAY) {
			final int arrayLen = getCASImpl().ll_getArraySize(this.getAddress());
			StringUtils.printSpaces(indent, buf);
			buf.append("Array length: " + arrayLen + "\n");
			if (arrayLen > 0) {
				StringUtils.printSpaces(indent, buf);
				buf.append("Array elements: [");
				for (int i = 0; i < arrayLen; i++) {
					if (i > 0) {
						buf.append(", ");
					}
					int element = this.getCASImpl().ll_getIntArrayValue(this.getAddress(), i);
					buf.append(element);
				}
				buf.append("]\n");
			}
		} else if (typeClass == LowLevelCAS.TYPE_CLASS_FLOATARRAY) {
			final int arrayLen = getCASImpl().ll_getArraySize(this.getAddress());
			StringUtils.printSpaces(indent, buf);
			buf.append("Array length: " + arrayLen + "\n");
			if (arrayLen > 0) {
				StringUtils.printSpaces(indent, buf);
				buf.append("Array elements: [");
				for (int i = 0; i < arrayLen; i++) {
					if (i > 0) {
						buf.append(", ");
					}
					float element = this.getCASImpl().ll_getFloatArrayValue(this.getAddress(), i);
					buf.append(element);
				}
				buf.append("]\n");
			}
		} else if (typeClass == LowLevelCAS.TYPE_CLASS_BYTEARRAY
				|| typeClass == LowLevelCAS.TYPE_CLASS_BOOLEANARRAY
				|| typeClass == LowLevelCAS.TYPE_CLASS_SHORTARRAY
				|| typeClass == LowLevelCAS.TYPE_CLASS_LONGARRAY
				|| typeClass == LowLevelCAS.TYPE_CLASS_DOUBLEARRAY) {
			if (typeClass == LowLevelCAS.TYPE_CLASS_BOOLEANARRAY)
				arrayFS = new ByteArrayFSImpl(this.getAddress(), this.getCASImpl());
			else if (typeClass == LowLevelCAS.TYPE_CLASS_BYTEARRAY)
				arrayFS = new ByteArrayFSImpl(this.getAddress(), this.getCASImpl());
			else if (typeClass == LowLevelCAS.TYPE_CLASS_SHORTARRAY)
				arrayFS = new ShortArrayFSImpl(this.getAddress(), this.getCASImpl());
			else if (typeClass == LowLevelCAS.TYPE_CLASS_LONGARRAY)
				arrayFS = new LongArrayFSImpl(this.getAddress(), this.getCASImpl());
			else if (typeClass == LowLevelCAS.TYPE_CLASS_DOUBLEARRAY)
				arrayFS = new DoubleArrayFSImpl(this.getAddress(), this.getCASImpl());

			final int arrayLen = getCASImpl().ll_getArraySize(this.getAddress());
			StringUtils.printSpaces(indent, buf);
			buf.append("Array length: " + arrayLen + "\n");
			if (arrayLen > 0) {
				int numToPrint = arrayLen;
				// /print max 15 array elements
				if (arrayLen > 15) {
					numToPrint = 15;
				}
				String[] dest = new String[numToPrint];
				arrayFS.copyToArray(0, dest, 0, numToPrint);
				StringUtils.printSpaces(indent, buf);
				buf.append("Array elements: [");
				for (int i = 0; i < numToPrint; i++) {
					if (i > 0) {
						buf.append(", ");
					}
					buf.append(dest[i]);
				}
				if (arrayLen > numToPrint) {
					buf.append(", ...");
				}
				buf.append("]\n");
			}
		} else {
			// Do nothing.

		}

		List<Feature> feats = getType().getFeatures();
		Feature feat;
		Type approp;
		FeatureStructureImpl val = null;
		String stringVal;
		for (int i = 0; i < feats.size(); i++) {
			StringUtils.printSpaces(indent, buf);
			feat = feats.get(i);
			buf.append(feat.getShortName() + ": ");
			approp = feat.getRange();
			// System.out.println("Range type: " + approp);
			
			// test if range is string type, or sub-string type (whose super type is string type)
			if (approp.equals(stringType)
					|| (this.getCAS().getTypeSystem().getParent(approp) != null && 
					    this.getCAS().getTypeSystem().getParent(approp).equals(stringType))) {
				stringVal = getStringValue(feat);
				if (stringVal == null) {
				  buf.append("<null>");
				} else {
				  buf.append('"');
				  buf.append(stringVal);
				  buf.append('"');
				}
				buf.append('\n');
				
			} else if (!approp.isPrimitive()) {
			  Exception e = null;
			  try {
			    val = (FeatureStructureImpl) getFeatureValue(feat);
			  } catch (Exception ee) {
			    e = ee;
			  }
			  if (e != null) {
			    buf.append("<exception").append(e.getMessage()).append('>');
			  } else if (val == null) {
					buf.append("<null>\n");
				} else {
					if (!approp.getName().equals("uima.cas.Sofa")) {
						val.prettyPrint(indent, incr, buf, useShortNames, null, printRefs);
					} else {
						buf.append(((SofaFS) val).getSofaID() + "\n");
					}
				}
			} else {
				buf.append(this.getFeatureValueAsString(feat) + "\n");
			}

			/*********************************************************************************************
       * if (approp.equals(this.getCASImpl().getTypeSystem().getType( CAS.TYPE_NAME_INTEGER))) {
       * buf.append(getIntValue(feat) + "\n"); } else if
       * (approp.equals(this.getCASImpl().getTypeSystem().getType( CAS.TYPE_NAME_FLOAT))) {
       * buf.append(getFloatValue(feat) + "\n"); } else if
       * (approp.equals(this.getCASImpl().getTypeSystem().getType( CAS.TYPE_NAME_STRING)) ||
       * (this.getCAS().getTypeSystem().getParent(approp) != null && this
       * .getCAS().getTypeSystem().getParent(approp).equals(
       * this.getCASImpl().getTypeSystem().getType( CAS.TYPE_NAME_STRING)))) { stringVal =
       * getStringValue(feat); if (stringVal == null) { stringVal = "<null>"; } else { stringVal =
       * "\"" + stringVal + "\""; } buf.append(stringVal + "\n"); } else { val =
       * (FeatureStructureImpl) getFeatureValue(feat); if (val == null) { buf.append("<null>\n"); }
       * else { val.prettyPrint(indent, incr, buf, useShortNames, null, printRefs); } }
       ********************************************************************************************/
		}

	}

	public Object clone() throws CASRuntimeException {
		if (getType().getName().equals(CAS.TYPE_NAME_SOFA)) {
			throw new CASRuntimeException(CASRuntimeException.CANNOT_CLONE_SOFA);
		}

		CASImpl casImpl = this.getCASImpl();
		FeatureStructure newFS = getCAS().createFS(getType());
		casImpl.copyFeatures(((FeatureStructureImpl) newFS).getAddress(), this.getAddress());
		return newFS;
	}

}
