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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureValuePath;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * Contains CAS Type and Feature objects to represent a feature path of the form
 * feature1/.../featureN. Each part that is enclosed within / is referred to as "path snippet"
 * below. Also contains the necessary evaluation logic to yield the value of the feature path. For
 * leaf snippets, the following "special features" are defined:
 * <ul>
 * <li><code>coveredText()</code> can be accessed using <code>evaluateAsString</code>
 * <li><code>typeName()</code> can be accessed using <code>evaluateAsString</code>
 * <li><code>fsId()</code> can be accessed using <code>evaluateAsInt</code>. Its result can be
 * used to retrieve an FS from the current LowLevel-CAS.
 * <li><code>uniqueId()</code> can be accessed using <code>evaluateAsInt</code>. Its result
 * can be used to uniquely identify an FS for a document (even if the document is split over several
 * CAS chunks)
 * </ul>
 * 
 * <b>Handling of Arrays </b> <br>
 * <ul>
 * <li>A feature path may contain 0 or more features of type <code>FSArray</code>, but not as
 * the last path snippet. The next path snippet must contain the fully qualified type name, example:
 * <code>family/members[0]/somepackage.Person:name</code></li>
 * <li>A feature path may also contain 0 or 1 feature of type
 * <code>IntArray, StringArray, FloatArray</code>, but only as the last path snippet.</li>
 * </ul>
 * For array-valued features, the following access operators are defined:
 * <ul>
 * <li><code>[index]</code> returns the array entry at <code>index</code>
 * <li><code>[last]</code> returns the last entry of the array
 * <li><code>[]</code> returns an array of values. <code>[]</code> is only allowed 0 or 1 time
 * in a feature path. If it is used, <code>getValueType</code> will return one of the following:
 * <code>CAS.TYPE_NAME_STRING_ARRAY ,CAS.TYPE_NAME_INTEGER_ARRAY,CAS.TYPE_NAME_FLOAT_ARRAY</code>.
 * </ul>
 * If the feature path is defined directly for an <code>FSArray</code>, an actual feature name
 * can be omitted, and only the array access operator can be used. Examples:
 * 
 * <pre>
 *                        
 *                         
 *                          
 *                           
 *                            
 *                             
 *                              
 *                               
 *                                
 *                                 
 *                                                                            []/somepackage.Person:coveredText()
 *                                                                             [last]/somepackage.Person:fsId()
 *                                  
 *                                 
 *                                
 *                               
 *                              
 *                             
 *                            
 *                           
 *                          
 *                         
 * </pre>
 * 
 * <br><b>Usage </b>
 * <ul>
 * <li>To create the feature path, use <code>FeaturePath.getFeaturePath</code>. Note that the
 * client code needs to keep track of the "start type" of the feature path, that is, the type that
 * contains the attribute used in the first snippet of the path.
 * <li>At <code>typeSystemInit</code> of your component (CAS consumer or TAE), call
 * <code>typeSystemInit</code> of the feature path.
 * <li>Call <code>getValueType</code> to find out whether the feature path evaluates to a String,
 * and int, a float, or their array counterparts.
 * <li>Depending on the leaf type, call the appropriate <code>evaluateAs </code> methods
 * </ul>
 * 
 */
public class FeatureValuePathImpl implements FeatureValuePath {

	private static final boolean CAS_TYPE_CHECKS = true;

	private static final Map<String, String> CONTAINER_TO_ELEMENTYPE_MAP;

	private static final Map<String, String> LIST_TO_ARRAYTYPE_MAP;

	private static final String COVERED_TEXT = "coveredText()";

	private static final String[] EMPTY_LIST_TYPE_NAMES = new String[] { CAS.TYPE_NAME_EMPTY_FS_LIST,
			CAS.TYPE_NAME_EMPTY_INTEGER_LIST, CAS.TYPE_NAME_EMPTY_FLOAT_LIST,
			CAS.TYPE_NAME_EMPTY_STRING_LIST };

	private static final String FS_ID = "fsId()";

	private static final int LAST_ARRAY_ENTRY = -1;

	private static final String LAST_ARRAY_ENTRY_MARKER = "last";

	private static final String[] LIST_TYPE_NAMES = new String[] { CAS.TYPE_NAME_FS_LIST,
			CAS.TYPE_NAME_INTEGER_LIST, CAS.TYPE_NAME_FLOAT_LIST, CAS.TYPE_NAME_STRING_LIST };

	private static final String[] SIMPLE_VAL_TYPES = { CAS.TYPE_NAME_STRING,
			CAS.TYPE_NAME_STRING_ARRAY, CAS.TYPE_NAME_INTEGER, CAS.TYPE_NAME_INTEGER_ARRAY,
			CAS.TYPE_NAME_FLOAT, CAS.TYPE_NAME_FLOAT_ARRAY, CAS.TYPE_NAME_INTEGER_LIST,
			CAS.TYPE_NAME_FLOAT_LIST, CAS.TYPE_NAME_STRING_LIST };

	private static final int TYPE_CLASS_FLOATLIST = 2;

	// MUST reflect the order of elements in the list_type_names array
	private static final int TYPE_CLASS_FSLIST = 0;

	private static final int TYPE_CLASS_INTEGERLIST = 1;

	private static final int TYPE_CLASS_STRINGLIST = 3;

	private static final String TYPE_NAME = "typeName()";

	private static final String UNIQUE_ID = "uniqueId()";

	private static final int USE_ALL_ENTRIES = -2;

	static {
		Arrays.sort(SIMPLE_VAL_TYPES);
		CONTAINER_TO_ELEMENTYPE_MAP = new HashMap<String, String>();
		CONTAINER_TO_ELEMENTYPE_MAP.put(CAS.TYPE_NAME_INTEGER_ARRAY, CAS.TYPE_NAME_INTEGER);
		CONTAINER_TO_ELEMENTYPE_MAP.put(CAS.TYPE_NAME_STRING_ARRAY, CAS.TYPE_NAME_STRING);
		CONTAINER_TO_ELEMENTYPE_MAP.put(CAS.TYPE_NAME_FLOAT_ARRAY, CAS.TYPE_NAME_FLOAT);
		CONTAINER_TO_ELEMENTYPE_MAP.put(CAS.TYPE_NAME_INTEGER_LIST, CAS.TYPE_NAME_INTEGER);
		CONTAINER_TO_ELEMENTYPE_MAP.put(CAS.TYPE_NAME_STRING_LIST, CAS.TYPE_NAME_STRING);
		CONTAINER_TO_ELEMENTYPE_MAP.put(CAS.TYPE_NAME_FLOAT_LIST, CAS.TYPE_NAME_FLOAT);

		LIST_TO_ARRAYTYPE_MAP = new HashMap<String, String>();
		LIST_TO_ARRAYTYPE_MAP.put(CAS.TYPE_NAME_INTEGER_LIST, CAS.TYPE_NAME_INTEGER_ARRAY);
		LIST_TO_ARRAYTYPE_MAP.put(CAS.TYPE_NAME_STRING_LIST, CAS.TYPE_NAME_STRING_ARRAY);
		LIST_TO_ARRAYTYPE_MAP.put(CAS.TYPE_NAME_FLOAT_LIST, CAS.TYPE_NAME_FLOAT_ARRAY);
	}

	public static FeatureValuePathImpl getFeaturePath(String featurePath) throws CASRuntimeException {
		if (featurePath == null || featurePath.length() == 0) {
			return null;
		}
		featurePath = featurePath.trim();
		// cut the path into individual snippets
		StringTokenizer strTok = new StringTokenizer(featurePath, "/");
		String[] snippets = new String[strTok.countTokens()];
		int i = 0;
		while (strTok.hasMoreElements()) {
			String snippet = (String) strTok.nextElement();
			snippets[i] = snippet;
			i++;
		}
		// build the feature path structure
		// the leaf element
		FeatureValuePathImpl result = new FeatureValuePathImpl(snippets[snippets.length - 1], null);
		// iterate backwards through the snippets, each one getting the
		// remainder of
		// the path as child element
		int j = snippets.length - 2;
		for (; j >= 0; j--) {
			result = new FeatureValuePathImpl(snippets[j], result);
		}

		return result;
	}

	/**
   * @param pathSnippet feature path part
   * @param child a feature value path object
   * @throws CASRuntimeException if the feature path is invalid
   */
	private FeatureValuePathImpl(String pathSnippet, FeatureValuePathImpl child)
			throws CASRuntimeException {
		if (pathSnippet == null || pathSnippet.length() == 0) {
			CASRuntimeException exception = new CASRuntimeException(
					CASRuntimeException.INVALID_FEATURE_PATH, new String[] { pathSnippet });
			throw exception;
		}

		this.childPath = child;
		this.typeNameInSnippet = getTypeInSnippet(pathSnippet);
		this.featureName = getFeatureInSnippet(pathSnippet);
		this.isArrayOrList = false;
		this.isArrayType = false;
		determineArray();
		this.isListType = false;

		this.isCoveredTextFeature = COVERED_TEXT.equals(this.featureName);
		this.isFsIdFeature = FS_ID.equals(this.featureName);
		this.isTypeNameFeature = TYPE_NAME.equals(this.featureName);
		this.isUniqueIdFeature = UNIQUE_ID.equals(this.featureName);
		this.isBracketsOnly = (this.isArrayOrList && this.featureName.length() == 0);

		// coveredText() asf. is only valid as the last snippet in a feature
		// path
		if ((this.isCoveredTextFeature || this.isFsIdFeature || this.isUniqueIdFeature || this.isTypeNameFeature)
				&& child != null) {
			CASRuntimeException exception = new CASRuntimeException(
					CASRuntimeException.INVALID_FEATURE_PATH, new String[] { pathSnippet });
			throw exception;
		}

	}

	public Object evaluate(int currentFS, LowLevelCAS cas) {
		String valueType = getValueType();
		if (CAS.TYPE_NAME_FLOAT.equals(valueType)) {
			return evaluateAsFloat(currentFS, cas);
		} else if (CAS.TYPE_NAME_FLOAT_ARRAY.equals(valueType)) {
			return evaluateAsFloatArray(currentFS, cas);
		} else if (CAS.TYPE_NAME_INTEGER.equals(valueType)) {
			return evaluateAsInt(currentFS, cas);
		} else if (CAS.TYPE_NAME_INTEGER_ARRAY.equals(valueType)) {
			return evaluateAsIntArray(currentFS, cas);
		} else if (CAS.TYPE_NAME_STRING.equals(valueType)) {
			return evaluateAsString(currentFS, cas);
		} else if (CAS.TYPE_NAME_STRING_ARRAY.equals(valueType)) {
			return evaluateAsStringArray(currentFS, cas);
		} else {
			throw new IllegalStateException("unknown value type");
		}
	}

	public Float evaluateAsFloat(int currentFS, LowLevelCAS cas) {
		if (currentFS == 0) {
			return null;
		}
		if (this.isArrayType) {
			if (this.arrayIndex == USE_ALL_ENTRIES) {
				throw new IllegalStateException("feature path denotes an array");
			}
			int arrayFS = (this.isBracketsOnly ? currentFS : cas.ll_getRefValue(currentFS,
					this.featureCode));

			int arraySize = ((CASImpl) cas).ll_getArraySize(arrayFS);
			// if the user specified name[1000], but the array has only 5
			// entries for name...
			if (this.arrayIndex >= arraySize) {
				return null;
			}

			int typeClass = cas.ll_getTypeClass(this.featureRangeType);
			switch (typeClass) {
			case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
				int position = getArrayIndex(arraySize);
				return Float.valueOf(cas.ll_getFloatArrayValue(arrayFS, position, false));
			case LowLevelCAS.TYPE_CLASS_FSARRAY:
				int childFS = getFsAtIndex(arrayFS, cas, arraySize);
				return this.childPath.evaluateAsFloat(childFS, cas);
			default:
				throw new IllegalStateException("feature path snippet is neither float nor fs array");
			}
		}

		if (this.isListType) {
			if (this.arrayIndex == USE_ALL_ENTRIES) {
				throw new IllegalStateException("feature path denotes an array");
			}
			int listFS = (this.isBracketsOnly ? currentFS : cas.ll_getRefValue(currentFS,
					this.featureCode));

			switch (this.listType) {
			case TYPE_CLASS_FLOATLIST:
				return (Float) getValueAtListIndex(cas, listFS);
			case TYPE_CLASS_FSLIST:
				int childFs = getFsAtListIndex(cas, listFS);
				if (childFs != 0) {
					return this.childPath.evaluateAsFloat(childFs, cas);
				}
				return null;
			default:
				throw new IllegalStateException("feature path snippet is neither float nor fs list");
			}
		}

		if (this.childPath != null) {
			int childFS = cas.ll_getRefValue(currentFS, this.featureCode, CAS_TYPE_CHECKS);
			return this.childPath.evaluateAsFloat(childFS, cas);
		} else if (this.isCoveredTextFeature || this.isUniqueIdFeature || this.isFsIdFeature
				|| this.isTypeNameFeature) {
			throw new IllegalStateException("feature path does not denote a float");
		} else {
			int typeClass = cas.ll_getTypeClass(this.featureRangeType);
			switch (typeClass) {
			case LowLevelCAS.TYPE_CLASS_FLOAT:
				return Float.valueOf(cas.ll_getFloatValue(currentFS, this.featureCode, CAS_TYPE_CHECKS));
			default:
				throw new IllegalStateException("feature path does not denote a float");
			}
		}
	}

	public Float[] evaluateAsFloatArray(int currentFS, LowLevelCAS cas) {
		if (!getValueType().equals(CAS.TYPE_NAME_FLOAT_ARRAY)) {
			throw new IllegalStateException("Feature path does not denote a float array");
		}
		if (currentFS == 0) {
			return null;
		}

		if (this.isArrayType) {

			// if the path snippet is [], the currentFS itself is the aray. If
			// the path snippet is like authors[], the "authors" feature
			// contains the array
			int arrayFS = (this.isBracketsOnly ? currentFS : cas.ll_getRefValue(currentFS,
					this.featureCode));

			int arraySize = ((CASImpl) cas).ll_getArraySize(arrayFS);

			// if the user specified name[1000], but the array has only 5
			// entries for name...
			if (this.arrayIndex >= arraySize) {
				return null;
			}

			if (this.arrayIndex == USE_ALL_ENTRIES) {
				// we currently assume that there can only be one [] in the path
				// hence, it's safe to say that we can collect floats here
				Float[] result = new Float[arraySize];

				if (this.childPath != null) { // this snippet denotes an
					// FSArray
					// iterate through the snippets, which will return floats
					for (int i = 0; i < arraySize; i++) {
						int childFS = cas.ll_getRefArrayValue(arrayFS, i, CAS_TYPE_CHECKS);
						result[i] = this.childPath.evaluateAsFloat(childFS, cas);
					}
				} else {
					// this snippet denotes a float array, just collect it
					for (int i = 0; i < arraySize; i++) {
						result[i] = Float.valueOf(cas.ll_getFloatArrayValue(arrayFS, i));
					}
				}
				return result;
			}
			// resume evaluation at the correct array entry
			int childFS = getFsAtIndex(arrayFS, cas, arraySize);
			return this.childPath.evaluateAsFloatArray(childFS, cas);
		} else if (this.isListType) {
			// if the path snippet is [], the currentFS itself is the list. If
			// the path snippet is like authors[], the "authors" feature
			// contains the list
			int listFS = (this.isBracketsOnly ? currentFS : cas.ll_getRefValue(currentFS,
					this.featureCode));

			if (this.arrayIndex == USE_ALL_ENTRIES) {
				ArrayList resultList = (ArrayList) getValueAtListIndex(cas, listFS);

				if (resultList == null) {
					return null;
				}

				// we currently assume that there can only be one [] in the path
				// hence, it's safe to say that we can collect floats here
				Float[] result = new Float[resultList.size()];

				if (this.childPath != null) { // this snippet denotes an
					// FSList
					// iterate through the results , which will return floats
					for (int i = 0; i < resultList.size(); i++) {
						int childFS = ((Integer) resultList.get(i)).intValue();
						result[i] = this.childPath.evaluateAsFloat(childFS, cas);
					}
				} else {
					// this snippet denotes a float list, just collect it
					resultList.toArray(result);
				}
				return result;
			}
			// resume evaluation at the correct list entry
			int childFS = getFsAtListIndex(cas, listFS);
			return this.childPath.evaluateAsFloatArray(childFS, cas);
		} else {
			// resume evaulation with the next snippet
			int childFS = cas.ll_getRefValue(currentFS, this.featureCode, CAS_TYPE_CHECKS);
			return this.childPath.evaluateAsFloatArray(childFS, cas);
		}
	}

	public Integer evaluateAsInt(int currentFS, LowLevelCAS cas) {
		if (currentFS == 0) {
			return null;
		}

		if (this.isArrayType) {
			if (this.arrayIndex == USE_ALL_ENTRIES) {
				throw new IllegalStateException("feature path denotes an array");
			}
			int arrayFS = (this.isBracketsOnly ? currentFS : cas.ll_getRefValue(currentFS,
					this.featureCode));

			int arraySize = ((CASImpl) cas).ll_getArraySize(arrayFS);
			// if the user specified name[1000], but the array has only 5
			// entries for name...
			if (this.arrayIndex >= arraySize) {
				return null;
			}
			int typeClass = cas.ll_getTypeClass(this.featureRangeType);
			switch (typeClass) {
			case LowLevelCAS.TYPE_CLASS_INTARRAY:
				int position = getArrayIndex(arraySize);
				return Integer.valueOf(cas.ll_getIntArrayValue(arrayFS, position, false));
			case LowLevelCAS.TYPE_CLASS_FSARRAY:
				int childFS = getFsAtIndex(arrayFS, cas, arraySize);
				return this.childPath.evaluateAsInt(childFS, cas);
			default:
				throw new IllegalStateException("feature path snippet is neither int nor fs array");
			}
		}

		if (this.isListType) {
			if (this.arrayIndex == USE_ALL_ENTRIES) {
				throw new IllegalStateException("feature path denotes an array");
			}
			int listFS = (this.isBracketsOnly ? currentFS : cas.ll_getRefValue(currentFS,
					this.featureCode));

			switch (this.listType) {
			case TYPE_CLASS_INTEGERLIST:
				return (Integer) getValueAtListIndex(cas, listFS);
			case TYPE_CLASS_FSLIST:
				int childFs = getFsAtListIndex(cas, listFS);
				if (childFs != 0) {
					return this.childPath.evaluateAsInt(childFs, cas);
				}
				return null;
			default:
				throw new IllegalStateException("feature path snippet is neither int nor fs list");
			}
		}

		if (this.childPath != null) {
			int childFS = cas.ll_getRefValue(currentFS, this.featureCode, CAS_TYPE_CHECKS);
			return this.childPath.evaluateAsInt(childFS, cas);
		} else if (this.isCoveredTextFeature || this.isTypeNameFeature) {
			throw new IllegalStateException("feature path does not denote an int");
		} else if (this.isFsIdFeature) {
			return Integer.valueOf(currentFS);
		} else if (this.isUniqueIdFeature) {
			return Integer.valueOf(currentFS); // TODO: return currentFs + chunkId
		} else {
			int typeClass = cas.ll_getTypeClass(this.featureRangeType);
			switch (typeClass) {
			case LowLevelCAS.TYPE_CLASS_INT:
				return Integer.valueOf(cas.ll_getIntValue(currentFS, this.featureCode, CAS_TYPE_CHECKS));
			default:
				throw new IllegalStateException("feature path does not denote an int");
			}
		}
	}

	public Integer[] evaluateAsIntArray(int currentFS, LowLevelCAS cas) {
		if (!getValueType().equals(CAS.TYPE_NAME_INTEGER_ARRAY)) {
			throw new IllegalStateException("Feature path does not denote an int array");
		}
		if (currentFS == 0) {
			return null;
		}

		if (this.isArrayType) {

			// if the path snippet is [], the currentFS itself is the aray. If
			// the path snippet is like authors[], the "authors" feature
			// contains the array
			int arrayFS = (this.isBracketsOnly ? currentFS : cas.ll_getRefValue(currentFS,
					this.featureCode));

			int arraySize = ((CASImpl) cas).ll_getArraySize(arrayFS);

			// if the user specified name[1000], but the array has only 5
			// entries for name...
			if (this.arrayIndex >= arraySize) {
				return null;
			}

			if (this.arrayIndex == USE_ALL_ENTRIES) {
				// we currently assume that there can only be one [] in the path
				// hence, it's safe to say that we can collect integers here
				Integer[] result = new Integer[arraySize];

				if (this.childPath != null) { // this snippet denotes an
					// FSArray
					// iterate through the snippets, which will return integers
					for (int i = 0; i < arraySize; i++) {
						int childFS = cas.ll_getRefArrayValue(arrayFS, i, CAS_TYPE_CHECKS);
						result[i] = this.childPath.evaluateAsInt(childFS, cas);
					}
				} else {
					// this snippet denotes an int array, just collect it
					for (int i = 0; i < arraySize; i++) {
						result[i] = Integer.valueOf(cas.ll_getIntArrayValue(arrayFS, i));
					}
				}
				return result;
			}
			// resume evaluation at the correct array entry
			int childFS = getFsAtIndex(arrayFS, cas, arraySize);
			return this.childPath.evaluateAsIntArray(childFS, cas);
		} else if (this.isListType) {
			// if the path snippet is [], the currentFS itself is the list. If
			// the path snippet is like authors[], the "authors" feature
			// contains the list
			int listFS = (this.isBracketsOnly ? currentFS : cas.ll_getRefValue(currentFS,
					this.featureCode));

			if (this.arrayIndex == USE_ALL_ENTRIES) {
				ArrayList resultList = (ArrayList) getValueAtListIndex(cas, listFS);

				if (resultList == null) {
					return null;
				}

				// we currently assume that there can only be one [] in the path
				// hence, it's safe to say that we can collect floats here
				Integer[] result = new Integer[resultList.size()];

				if (this.childPath != null) { // this snippet denotes an
					// FSList
					// iterate through the results , which will return floats
					for (int i = 0; i < resultList.size(); i++) {
						int childFS = ((Integer) resultList.get(i)).intValue();
						result[i] = this.childPath.evaluateAsInt(childFS, cas);
					}
				} else {
					// this snippet denotes a float list, just collect it
					resultList.toArray(result);
				}
				return result;
			}
			// resume evaluation at the correct list entry
			int childFS = getFsAtListIndex(cas, listFS);
			return this.childPath.evaluateAsIntArray(childFS, cas);
		} else {
			// resume evaulation with the next snippet
			int childFS = cas.ll_getRefValue(currentFS, this.featureCode, CAS_TYPE_CHECKS);
			return this.childPath.evaluateAsIntArray(childFS, cas);
		}
	}

	/**
   * Evaluates each snippet of the feature path. Returns a String representation of the leaf value
   * of the path. Returns <code>null</code> if some feature within the path is not set. If the
   * leaf snippet is <code>COVERED_TEXT</code>, returns the covered text of
   * <code>currentFS</code>.
   * 
   * @param currentFS the current Feature Structure
   * @param cas CAS
   * @return A string representation of the leaf value.
   */
	public String evaluateAsString(int currentFS, LowLevelCAS cas) {
		if (currentFS == 0) {
			return null;
		}

		if (this.isArrayType) {
			if (this.arrayIndex == USE_ALL_ENTRIES) {
				throw new IllegalStateException("feature path denotes an array");
			}
			int arrayFS = (this.isBracketsOnly ? currentFS : cas.ll_getRefValue(currentFS,
					this.featureCode));

			int arraySize = ((CASImpl) cas).ll_getArraySize(arrayFS);
			// if the user specified name[1000], but the array has only 5
			// entries for name...
			if (this.arrayIndex >= arraySize) {
				return null;
			}
			int typeClass = cas.ll_getTypeClass(this.featureRangeType);
			switch (typeClass) {
			case LowLevelCAS.TYPE_CLASS_STRINGARRAY:
				int position = getArrayIndex(arraySize);
				return cas.ll_getStringArrayValue(arrayFS, position, false);
			case LowLevelCAS.TYPE_CLASS_FSARRAY:
				int childFS = getFsAtIndex(arrayFS, cas, arraySize);
				return this.childPath.evaluateAsString(childFS, cas);
			default:
				throw new IllegalStateException("feature path snippet is neither string nor fs array");
			}
		}

		if (this.isListType) {
			if (this.arrayIndex == USE_ALL_ENTRIES) {
				throw new IllegalStateException("feature path denotes an array");
			}
			int listFS = (this.isBracketsOnly ? currentFS : cas.ll_getRefValue(currentFS,
					this.featureCode));

			switch (this.listType) {
			case TYPE_CLASS_STRINGLIST:
				return (String) getValueAtListIndex(cas, listFS);
			case TYPE_CLASS_FSLIST:
				int childFs = getFsAtListIndex(cas, listFS);
				if (childFs != 0) {
					return this.childPath.evaluateAsString(childFs, cas);
				}
				return null;
			default:
				throw new IllegalStateException("feature path snippet is neither float nor fs list");
			}
		}

		if (this.childPath != null) {
			int childFS = cas.ll_getRefValue(currentFS, this.featureCode, CAS_TYPE_CHECKS);
			return this.childPath.evaluateAsString(childFS, cas);
		} else if (this.isCoveredTextFeature) {
			AnnotationFS annotation = (AnnotationFS) cas.ll_getFSForRef(currentFS);
			return annotation.getCoveredText();
		} else if (this.isTypeNameFeature) {
			Type type = cas.ll_getTypeSystem().ll_getTypeForCode(cas.ll_getFSRefType(currentFS));
			return type.getName();
		} else if (this.isFsIdFeature) {
			throw new IllegalStateException("feature path denotes fsId()");
		} else if (this.isUniqueIdFeature) {
			throw new IllegalStateException("feature path denotes uniqueId()");
		} else {
			int typeClass = cas.ll_getTypeClass(this.featureRangeType);
			switch (typeClass) {
			case LowLevelCAS.TYPE_CLASS_STRING:
				return cas.ll_getStringValue(currentFS, this.featureCode, CAS_TYPE_CHECKS);
			case LowLevelCAS.TYPE_CLASS_FLOAT:
				throw new IllegalStateException("feature path denotes a float");
			case LowLevelCAS.TYPE_CLASS_INT:
				throw new IllegalStateException("feature path denotes an int");
			default:
				throw new IllegalStateException("feature path does not denote a string");
			}
		}
	}

	public String[] evaluateAsStringArray(int currentFS, LowLevelCAS cas) {
		if (!getValueType().equals(CAS.TYPE_NAME_STRING_ARRAY)) {
			throw new IllegalStateException("Feature path does not denote a String array");
		}
		if (currentFS == 0) {
			return null;
		}

		if (this.isArrayType) {
			// if the path snippet is [], the currentFS itself is the aray. If
			// the path snippet is like authors[], the "authors" feature
			// contains the array
			int arrayFS = (this.isBracketsOnly ? currentFS : cas.ll_getRefValue(currentFS,
					this.featureCode));

			int arraySize = ((CASImpl) cas).ll_getArraySize(arrayFS);

			// if the user specified name[1000], but the array has only 5
			// entries for name...
			if (this.arrayIndex >= arraySize) {
				return null;
			}

			if (this.arrayIndex == USE_ALL_ENTRIES) {
				// we currently assume that there can only be one [] in the path
				// hence, it's safe to say that we can collect strings here
				String[] result = new String[arraySize];

				if (this.childPath != null) { // this snippet denotes an
					// FSArray
					// iterate through the snippets, which will return Strings
					// example author[]/name: author points to an FSArray of
					// authors,
					// the loop below collects their names as strings
					for (int i = 0; i < arraySize; i++) {
						int childFS = cas.ll_getRefArrayValue(arrayFS, i, CAS_TYPE_CHECKS);
						result[i] = this.childPath.evaluateAsString(childFS, cas);
					}
				} else {
					// arrayFS itself denotes a String array, just collect it
					for (int i = 0; i < arraySize; i++) {
						result[i] = cas.ll_getStringArrayValue(arrayFS, i);
					}
				}
				return result;
			}
			// the snippet is like ...[1] or ...[last]
			// resume evaluation at the correct array entry
			int childFS = getFsAtIndex(arrayFS, cas, arraySize);
			return this.childPath.evaluateAsStringArray(childFS, cas);
		} else if (this.isListType) {
			// if the path snippet is [], the currentFS itself is the list. If
			// the path snippet is like authors[], the "authors" feature
			// contains the list
			int listFS = (this.isBracketsOnly ? currentFS : cas.ll_getRefValue(currentFS,
					this.featureCode));

			if (this.arrayIndex == USE_ALL_ENTRIES) {
				ArrayList resultList = (ArrayList) getValueAtListIndex(cas, listFS);

				if (resultList == null) {
					return null;
				}

				// we currently assume that there can only be one [] in the path
				// hence, it's safe to say that we can collect floats here
				String[] result = new String[resultList.size()];

				if (this.childPath != null) { // this snippet denotes an
					// FSList
					// iterate through the results , which will return floats
					for (int i = 0; i < resultList.size(); i++) {
						int childFS = ((Integer) resultList.get(i)).intValue();
						result[i] = this.childPath.evaluateAsString(childFS, cas);
					}
				} else {
					// this snippet denotes a float list, just collect it
					resultList.toArray(result);
				}
				return result;
			}
			// resume evaluation at the correct list entry
			int childFS = getFsAtListIndex(cas, listFS);
			return this.childPath.evaluateAsStringArray(childFS, cas);
		} else {
			// this is just an intermediate feature (like "metadata" in
			// metadata/author[]/name)
			// resume evaulation with the next snippet
			int childFS = cas.ll_getRefValue(currentFS, this.featureCode, CAS_TYPE_CHECKS);
			return this.childPath.evaluateAsStringArray(childFS, cas);
		}
	}

	/**
   * Returns the type for which the last feature in the feature path is defined. Assumes that
   * <code>typeSystemInit</code> has been called prior to this method.
   * <ul>
   * <li>For a feature path <code>feature1/.../featureN-1/featureN</code>, returns the type of
   * featureN.
   * <li>For a feature path <code>feature1/.../featureN-1/typeN:featureN</code>, returns the
   * type code for typeN. (For example, if the range type of featureN-1 is FSList or FSArray)
   * <li>For a feature path <code>feature1</code>, where feature1 is simple-valued, returns the
   * type that was used in <code>typeSystemInit</code>
   * </ul>
   * 
   * @return the type for which the last feature in the feature path is defined.
   */
	public int getFSType() {
		if (this.isSimpleRangeType) {
			return this.typeCode;
		}
		return this.childPath.getFSType();
	}

	/**
   * Returns the type that this feature path will evaluate to. Can be used to select the correct
   * "evaluateAs" method.
   * 
   * @return String the type that this feature path will evaluate to. Will be one of the following:
   *         <ul>
   *         <li>CAS.TYPE_NAME_STRING
   *         <li>CAS.TYPE_NAME_STRING_ARRAY
   *         <li>CAS.TYPE_NAME_INTEGER
   *         <li>CAS.TYPE_NAME_INTEGER_ARRAY
   *         <li>CAS.TYPE_NAME_FLOAT
   *         <li>CAS.TYPE_NAME_FLOAT_ARRAY
   *         </ul>
   */
	public String getValueType() {
		if (this.valueTypeName == null) {
			this.valueTypeName = this.childPath.getValueType();
			if (this.arrayIndex == USE_ALL_ENTRIES) {
				if (CAS.TYPE_NAME_STRING.equals(this.valueTypeName)) {
					this.valueTypeName = CAS.TYPE_NAME_STRING_ARRAY;
				} else if (CAS.TYPE_NAME_INTEGER.equals(this.valueTypeName)) {
					this.valueTypeName = CAS.TYPE_NAME_INTEGER_ARRAY;
				} else if (CAS.TYPE_NAME_FLOAT.equals(this.valueTypeName)) {
					this.valueTypeName = CAS.TYPE_NAME_FLOAT_ARRAY;
				}
			}
		}
		return this.valueTypeName;
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		if (this.typeNameInSnippet != null) {
			result.append(this.typeNameInSnippet);
			result.append(TypeSystem.FEATURE_SEPARATOR);
		}
		result.append(this.featureName);
		if (this.isArrayOrList) {
			result.append('[');
			if (!this.isBracketsOnly && this.arrayIndex >= 0) {
				result.append(this.arrayIndex);
			}
			if (this.arrayIndex == LAST_ARRAY_ENTRY) {
				result.append(LAST_ARRAY_ENTRY_MARKER);
			}
			result.append(']');
		}
		if (this.childPath != null) {
			result.append('/');
			result.append(this.childPath.toString());
		}
		return result.toString();
	}

	public void typeSystemInit(int fsType, LowLevelTypeSystem ts) throws CASRuntimeException {
		if (this.typeNameInSnippet != null) { // if the feature path snippet
			// defines
			// its own
			// type,
			// we use that one instead of the one that's passed in
			fsType = ts.ll_getCodeForTypeName(this.typeNameInSnippet);
		}

		if (fsType == LowLevelTypeSystem.UNKNOWN_TYPE_CODE) {
			CASRuntimeException exception = new CASRuntimeException(
					CASRuntimeException.INVALID_FEATURE_PATH, new String[] { this.typeNameInSnippet });
			throw exception;
		}

		// the range type denotes what type of FSes (or
		// built-in type) is contained in this feature
		int rangeTypeCode = LowLevelTypeSystem.UNKNOWN_TYPE_CODE;

		if (!(isBuiltInFeature() || this.isBracketsOnly)) {
			// find the feature in fsType that corresponds to this path snippet
			int[] features = ts.ll_getAppropriateFeatures(fsType);
			boolean found = false;

			int i = 0;
			for (; i < features.length && (!found); i++) {
				Feature feature = ts.ll_getFeatureForCode(features[i]);
				found = feature.getShortName().equals(this.featureName);
			}

			if (found) {
				// store the feature code that corresponds to this path snippet
				this.featureCode = features[i - 1];
				rangeTypeCode = ts.ll_getRangeType(this.featureCode);
			} else {
				Type type = ts.ll_getTypeForCode(fsType);
				CASRuntimeException exception = new CASRuntimeException(CASRuntimeException.INAPPROP_FEAT,
						new String[] { this.featureName, type.getName() });
				throw exception;
			}
		}

		if (this.isBracketsOnly) {
			rangeTypeCode = fsType;
		}

		this.typeCode = fsType;

		// TODO: check mismatch between isArray and the actual rangeTypeCode

		// find out whether the type is a "simple" type that may only be used in
		// the last snippet of a path
		Type type = ts.ll_getTypeForCode(rangeTypeCode);
		this.featureRangeType = rangeTypeCode;

		if (isBuiltInFeature()) {
			this.isSimpleRangeType = true;
		} else {
			this.isSimpleRangeType = Arrays.binarySearch(SIMPLE_VAL_TYPES, type.getName()) >= 0;
		}

		if (this.isArrayOrList) {
			int arrayType = ts.ll_getCodeForTypeName(CAS.TYPE_NAME_ARRAY_BASE);
			this.isArrayType = ((TypeSystemImpl) ts).subsumes(arrayType, rangeTypeCode);
			if (!this.isArrayType) {
				// check whether the feature points to a list
				for (int i = 0; i < LIST_TYPE_NAMES.length && !this.isListType; i++) {
					int candidateType = ts.ll_getCodeForTypeName(LIST_TYPE_NAMES[i]);
					this.isListType = ((TypeSystemImpl) ts).subsumes(candidateType, rangeTypeCode);
					if (this.isListType) {
						// determine the type class of the list
						this.listType = i;
					}
				}

				// determine the right head and tail feature, depending on the
				// list type
				switch (this.listType) {
				case TYPE_CLASS_FSLIST:
					this.headFeature = ts.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_FS_LIST_HEAD);
					this.tailFeature = ts.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_FS_LIST_TAIL);
					break;
				case TYPE_CLASS_STRINGLIST:
					this.headFeature = ts.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_STRING_LIST_HEAD);
					this.tailFeature = ts.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_STRING_LIST_TAIL);
					break;
				case TYPE_CLASS_INTEGERLIST:
					this.headFeature = ts.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_INTEGER_LIST_HEAD);
					this.tailFeature = ts.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_INTEGER_LIST_TAIL);
					break;
				case TYPE_CLASS_FLOATLIST:
					this.headFeature = ts.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_FLOAT_LIST_HEAD);
					this.tailFeature = ts.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_FLOAT_LIST_TAIL);
					break;
				default:
					break;
				}

				this.emptyListTypes = new Type[EMPTY_LIST_TYPE_NAMES.length];
				for (int i = 0; i < EMPTY_LIST_TYPE_NAMES.length; i++) {
					this.emptyListTypes[i] = ts.ll_getTypeForCode(ts
							.ll_getCodeForTypeName(EMPTY_LIST_TYPE_NAMES[i]));
				}

			}

		}

		if (this.childPath != null) {
			// for simple range types, only [] and fsId() are allowed as child
			// path
			if (this.isSimpleRangeType
					&& !(this.childPath.isBracketsOnly() || 
					     this.childPath.isFsIdFeature)) {
				CASRuntimeException exception = new CASRuntimeException(
						CASRuntimeException.INVALID_FEATURE_PATH, new String[] { this.featureName });
				throw exception;
			}

			// continue with the child path
			this.childPath.typeSystemInit(rangeTypeCode, ts);
		} else if (this.isCoveredTextFeature) {
			// make sure that the type is a subtype of annotation
			int annotationType = ts.ll_getCodeForTypeName(CAS.TYPE_NAME_ANNOTATION);
			if (!((TypeSystemImpl) ts).subsumes(annotationType, fsType)) {
				CASRuntimeException exception = new CASRuntimeException(
						CASRuntimeException.INVALID_FEATURE_PATH, new String[] { this.featureName });
				throw exception;
			}

			this.valueTypeName = SIMPLE_VAL_TYPES[Arrays.binarySearch(SIMPLE_VAL_TYPES,
					CAS.TYPE_NAME_STRING)];
		} else if (this.isFsIdFeature) {
			this.valueTypeName = SIMPLE_VAL_TYPES[Arrays.binarySearch(SIMPLE_VAL_TYPES,
					CAS.TYPE_NAME_INTEGER)];
		} else if (this.isUniqueIdFeature) {
			this.valueTypeName = SIMPLE_VAL_TYPES[Arrays.binarySearch(SIMPLE_VAL_TYPES,
					CAS.TYPE_NAME_INTEGER)];
		} else if (this.isTypeNameFeature) {
			this.valueTypeName = SIMPLE_VAL_TYPES[Arrays.binarySearch(SIMPLE_VAL_TYPES,
					CAS.TYPE_NAME_STRING)];
		} else {
			if (!this.isSimpleRangeType) {
				CASRuntimeException exception = new CASRuntimeException(
						CASRuntimeException.NO_PRIMITIVE_TAIL);
				throw exception;
			}

			if (this.isArrayOrList && (this.arrayIndex != USE_ALL_ENTRIES)) {
				// in the case of, say, authornames[0], the feature is of type
				// string array, but it will evaluate to a string.
				this.valueTypeName = CONTAINER_TO_ELEMENTYPE_MAP.get(type.getName());
			} else if (this.isListType) { // here, we can assume that
				// arrayIndex =
				// USE_ALL_ENTRIES
				// we don't return lists, but arrays, so we need to map the type
				// accordingly
				this.valueTypeName = LIST_TO_ARRAYTYPE_MAP.get(type.getName());
			} else {
				this.valueTypeName = SIMPLE_VAL_TYPES[Arrays.binarySearch(SIMPLE_VAL_TYPES, type.getName())];
			}
		}
	}

	private boolean isBuiltInFeature() {
		return this.isFsIdFeature || this.isUniqueIdFeature || this.isCoveredTextFeature
				|| this.isTypeNameFeature;
	}

	/**
   * Checks whether the feature snippet denotes array access (i.e., has [..] attached to it). If so,
   * determines the arrayIndex to use within evaluation, which can be a number, the special element
   * "last" or simple [], which means "all elements"
   * 
   * @throws CASRuntimeException
   *           If the closing ] is missing, or the number is not an integer
   */
	private final void determineArray() throws CASRuntimeException {
		int startIndex = this.featureName.indexOf('[');
		if (startIndex == -1) {
			return;
		}
		int endIndex = this.featureName.indexOf(']');
		if (endIndex == -1) { // we're missing the ending bracket
			CASRuntimeException exception = new CASRuntimeException(
					CASRuntimeException.INVALID_FEATURE_PATH, new String[] { this.toString() });
			throw exception;
		}

		this.isArrayOrList = true;

		String arrayIndexString = this.featureName.substring(startIndex + 1, endIndex);
		// cut off the array markers from the actual feature name
		this.featureName = this.featureName.substring(0, startIndex);

		// determine the array index to use

		if (arrayIndexString.equals("")) { // empty brackets, denotes "all
			// elements"
			this.arrayIndex = USE_ALL_ENTRIES;
		} else if (LAST_ARRAY_ENTRY_MARKER.equalsIgnoreCase(arrayIndexString)) {
			// [last],denotes "take the last array element"
			this.arrayIndex = LAST_ARRAY_ENTRY;
		} else {
			try {
				this.arrayIndex = Integer.parseInt(arrayIndexString);
			} catch (NumberFormatException e) {
				CASRuntimeException exception = new CASRuntimeException(
						CASRuntimeException.INVALID_FEATURE_PATH, new String[] { this.toString() });
				throw exception;
			}
		}

	}

	private int getArrayIndex(int arraySize) {
		return (LAST_ARRAY_ENTRY != this.arrayIndex ? this.arrayIndex : arraySize - 1);
	}

	private final String getFeatureInSnippet(String pathSnippet) {
		int typeIndex = pathSnippet.indexOf(TypeSystem.FEATURE_SEPARATOR);
		if (typeIndex == -1) {
			return pathSnippet;
		}
		return pathSnippet.substring(typeIndex + 1);
	}

	private int getFsAtIndex(int currentFS, LowLevelCAS cas, int arraySize) {
		if (!this.isArrayType) {
			throw new IllegalStateException("FeaturePath is not an array");
		}

		// if the user specified [last], we need to determine the actual
		// index to use. Otherwise, we just use the index given in the
		// snippet
		int arrayInd = getArrayIndex(arraySize);

		// locate the right FS
		int childFS = cas.ll_getRefArrayValue(currentFS, arrayInd, CAS_TYPE_CHECKS);
		return childFS;
	}

	/**
   * Assumes that <code>arrayIndex</code>!=<code>USE_ALL_ENTRIES</code>, and that the
   * <code>listType</code> is <code>TYPE_CLASS_FSLIST</code>
   * 
   * @param cas the low level CAS
   * @param the list feature structure
   * @return int A reference to the fs given in <code>arrayIndex</code>, or 0 if the list does
   *         not contain an entry for that index.
   */
	private int getFsAtListIndex(LowLevelCAS cas, int listFS) {
		if (this.arrayIndex == USE_ALL_ENTRIES || this.listType != TYPE_CLASS_FSLIST) {
			throw new IllegalStateException(
					"feature does not denote an fs list, or does not denote a singel array entry");
		}
		Object valueAtListIndex = getValueAtListIndex(cas, listFS);
		if (valueAtListIndex != null) {
			return ((Integer) valueAtListIndex).intValue();
		}
		return 0;
	}

	private Object getHeadValue(LowLevelCAS cas, int listFS) {
		if (listFS == 0) {
			return null;
		}
		switch (this.listType) {
		case TYPE_CLASS_STRINGLIST:
			return cas.ll_getStringValue(listFS, this.headFeature);
		case TYPE_CLASS_INTEGERLIST:
			return Integer.valueOf(cas.ll_getIntValue(listFS, this.headFeature));
		case TYPE_CLASS_FLOATLIST:
			return Float.valueOf(cas.ll_getFloatValue(listFS, this.headFeature));
		case TYPE_CLASS_FSLIST:
			return Integer.valueOf(cas.ll_getRefValue(listFS, this.headFeature));
		default:
			return null;
		}
	}

	private final String getTypeInSnippet(String pathSnippet) {
		if (pathSnippet == null) {
			return null;
		}
		int index = pathSnippet.indexOf(TypeSystem.FEATURE_SEPARATOR);
		if (index == -1) {
			return null;
		}
		return pathSnippet.substring(0, index);
	}

	private Object getValueAtIndexRec(LowLevelCAS cas, int listFS, ArrayList list, int count) {
		if (listFS == 0) {
			return null;
		}
		int type = cas.ll_getFSRefType(listFS);
		if (isEmptyList(cas, type)) {
			return null;
		}
		int listTail = cas.ll_getRefValue(listFS, this.tailFeature);
		switch (this.arrayIndex) {
		case USE_ALL_ENTRIES:
			list.add(getHeadValue(cas, listFS));
			return getValueAtIndexRec(cas, listTail, list, ++count);
		case LAST_ARRAY_ENTRY:
			Object result = getValueAtIndexRec(cas, listTail, list, ++count);
			if (result != null) {
				return result;
			}
			return getHeadValue(cas, listFS);
		default:
			if (count == this.arrayIndex) {
				return getHeadValue(cas, listFS);
			}
			return getValueAtIndexRec(cas, listTail, list, ++count);
		}
	}

	/**
	 * @param cas CAS
   * @param listFS the list Feature Structure
   * @return Object If arrayIndex = USE_ALL_ENTRIES, returns an <code>ArrayList</code> containing
   *         all entries. Otherwise, returns a String, Integer, or Float. Returns <code>null</code>
   *         if the list does not contain an entry for arrayIndex, or is empty.
   */
	private Object getValueAtListIndex(LowLevelCAS cas, int listFS) {
		ArrayList arrayRes = new ArrayList();
		Object result = getValueAtIndexRec(cas, listFS, arrayRes, 0);
		if (arrayRes.isEmpty()) {
			return result;
		}
		return arrayRes;
	}

	private boolean isBracketsOnly() {
		return this.isBracketsOnly;
	}

	private boolean isEmptyList(LowLevelCAS cas, int type) {
		Type candidateType = cas.ll_getTypeSystem().ll_getTypeForCode(type);
		TypeSystem typeSystem = ((CASImpl) cas).getTypeSystem();
		boolean isEmpty = false;
		for (int i = 0; i < this.emptyListTypes.length && (!isEmpty); i++) {
			isEmpty = typeSystem.subsumes(this.emptyListTypes[i], candidateType);
		}
		return isEmpty;
	}

	// only returns a sensible value if isArray returns true
	private int arrayIndex;

	// the next path snippet (may be null)
	private final FeatureValuePathImpl childPath;

	private Type[] emptyListTypes;

	// the feature code that corresponds to featureName
	private int featureCode;

	// the name of the feature contained in this path snippet
	private String featureName;

	// the range type of the feature (may be simple of complex)
	private int featureRangeType;

	private int headFeature;

	// true iff the snippet contains a feature name and brackets []
	private boolean isArrayOrList;

	// true iff this path snippet points to an FSArray
	private boolean isArrayType;

	// true iff the snippet only contains []. This is only valid if the
	// currentFS that's passed in for evaluatiuon
	// is an array or a list
	private boolean isBracketsOnly;

	// true iff the snippet contains COVERED_TEXT. This is only valid as the
	// last path snippet. Moreover, the preceding snippet must return an FS of
	// type Annotation.
	private boolean isCoveredTextFeature;

	// true iff the snippet contains FS_ID. This is only valid as the
	// last path snippet.
	private boolean isFsIdFeature;

	// true iff this path snippet points to an FSList
	private boolean isListType;

	// true iff featureRangeType is one of SIMPLE_VAL_TYPES
	private boolean isSimpleRangeType;

	// true iff the snippet contains TYPE_NAME. This is only valid as the
	// last path snippet.
	private boolean isTypeNameFeature;

	// true iff the snippet contains UNIQUE_ID. This is only valid as the
	// last path snippet.
	private boolean isUniqueIdFeature;

	private int listType;

	private int tailFeature;

	// the type for which featureName is defined
	private int typeCode;

	// Is only set if the feature path explictity contains the typeName, e.g.
	// ../TypeName:featureX/... Is mandatory for features that are accessed
	// through an array. Rationale: one can use the name to "narrow down" the
	// type of a feature, and we need the type name for arrays, as they don't
	// have information about the type of FS that they contain
	private final String typeNameInSnippet;

	// the type this feature path will evaluate to. Is one of SIMPLE_VAL_TYPES.
	private String valueTypeName;
}
