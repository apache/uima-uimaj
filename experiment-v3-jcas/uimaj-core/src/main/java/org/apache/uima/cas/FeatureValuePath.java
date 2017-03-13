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

package org.apache.uima.cas;

import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.LowLevelTypeSystem;

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
 *        []/somepackage.Person:coveredText()
 *         [last]/somepackage.Person:fsId()
 *    
 * </pre>
 * 
 * If the feature path is defined directly, for a String, integer or float array, the array access
 * operator can be used directly. Unlike FSArray, this access operator must be the only entry in the
 * path.
 * 
 * <br><b>Usage </b>
 * <ol>
 * <li>To create the feature path, use <code>FeaturePath.getFeaturePath</code>. Note that the
 * client code needs to keep track of the "start type" of the feature path, that is, the type that
 * contains the attribute used in the first snippet of the path.
 * <li>At <code>typeSystemInit</code> of your component (CAS consumer or TAE), call
 * <code>typeSystemInit</code> of the feature path.
 * <li>Call <code>getValueType</code> to find out whether the feature path evaluates to a String,
 * and int, a float, or their array counterparts.
 * <li>Depending on the leaf type, call the appropriate <code>evaluateAs </code> methods
 * </ol>
 * 
 * @deprecated use {@link org.apache.uima.cas.FeaturePath FeaturePath}
 */
@Deprecated
public interface FeatureValuePath {

  public Object evaluate(int currentFS, LowLevelCAS cas);

  public Float evaluateAsFloat(int currentFS, LowLevelCAS cas);

  public Float[] evaluateAsFloatArray(int currentFS, LowLevelCAS cas);

  public Integer evaluateAsInt(int currentFS, LowLevelCAS cas);

  public Integer[] evaluateAsIntArray(int currentFS, LowLevelCAS cas);

  /**
   * Evaluates each snippet of the feature path. Returns a String representation of the leaf value
   * of the path. Returns <code>null</code> if some feature within the path is not set. If the
   * leaf snippet is <code>COVERED_TEXT</code>, returns the covered text of
   * <code>currentFS</code>.
   * 
   * @param currentFS -
   * @param cas -
   * @return A string representation of the leaf value.
   */
  public String evaluateAsString(int currentFS, LowLevelCAS cas);

  public String[] evaluateAsStringArray(int currentFS, LowLevelCAS cas);

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
   * @return int the type for which the last feature in the feature path is defined.
   */
  public int getFSType();

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
  public String getValueType();

  public void typeSystemInit(int fsType, LowLevelTypeSystem ts) throws CASRuntimeException;

}
