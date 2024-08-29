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

/**
 * <p>
 * Interface for a feature path. A feature path is a sequence of features, specified as
 * shortFeatureNames (strings). The feature path can either be initialized using the addFeature() or
 * the initialize() method.
 * </p>
 * 
 * <p>
 * A single feature path object can apply to many different types. The method
 * <code>typeInit()</code> is (optionally) used to bind a feature path object to a particular
 * starting Type. This binding remains until another call to <code>typeInit()</code> is made. It is
 * used to speed up application of the featurePath to an instance, but is not required - the
 * application will dynamically lookup features along the path as needed.
 * </p>
 * 
 * <p>
 * If no typeInit call is made, or if the current binding is not correct for the Feature Structure,
 * then the mapping of the features in the feature path specification to actual features is
 * recomputed during the the evaluation of the particular getter invocation.
 * </p>
 * 
 * <p>
 * After these calls the feature path value can be applied to a particular Feature Structure and a
 * value can be extracted using the provided getter methods.
 * </p>
 * <p>
 * The feature path elements are separated by "/". So a valid feature path is /my/feature/path.
 * </p>
 * <p>
 * The feature path syntax also allows some built-in functions on the last feature path element.
 * Built-in functions are added with a ":" followed by the function name. E.g. "/my/path:fsId()".
 * The allowed built-in functions are:
 * </p>
 * <ul>
 * <li>coveredText()</li>
 * <li>fsId()</li>
 * <li>typeName()</li>
 * </ul>
 * <p>
 * Built-in functions are only evaluated if getValueAsString() is called.
 * </p>
 * 
 * <p>
 * All the intervening features except for the last must be features whose values are other Feature
 * Structures.
 * </p>
 * 
 * <p>
 * Features whose range is an Array are not permitted, unless they occur at the end of the Feature
 * Path. In that case, the value returned must be returned by one of the getValueAsString methods.
 * </p>
 */
public interface FeaturePath {

  /**
   * Get length of path.
   * 
   * @return An integer <code>&gt;= 0</code>.
   */
  int size();

  /**
   * Get feature at position.
   * 
   * @param i
   *          The position in the path (starting at <code>0</code>).
   * @return The feature, or <code>null</code> if there is no such feature.
   */
  Feature getFeature(int i);

  /**
   * Add a new feature at the end of the path.
   * 
   * @param feat
   *          The feature to be added.
   */
  void addFeature(Feature feat);

  /**
   * Initialize the feature path object with the given feature path string.
   * 
   * @param featurePath
   *          The featurePath that is used for this feature path object.
   * 
   * @throws CASException
   *           Throws an exception if the feature path syntax is invalid.
   */
  void initialize(String featurePath) throws CASException;

  /**
   * Check the feature path for the given type and initialize internal structures for faster access
   * to the feature path value.
   * 
   * @param featurePathType
   *          The type the feature path should be used on.
   * 
   * @throws CASException
   *           Throws an exception if the feature path is not valid for the given type
   */
  void typeInit(Type featurePathType) throws CASException;

  /**
   * Returns the feature path value as string for the given FeatureStructure.
   * 
   * If the feature path ends in a built-in function it is evaluated and the built-in function value
   * is returned; this is the only method which evaluates built-in functions
   * 
   * If the feature path ends with an array the array is converted to a comma separated string.
   * 
   * @param fs
   *          FeatureStructure to evaluate the feature path value
   * 
   * @return Returns the value of the feature path as String or null if the feature path was not set
   *         or some features along the path were null.
   */
  String getValueAsString(FeatureStructure fs);

  /**
   * Returns the feature path value as string for the given FeatureStructure.
   * 
   * If the feature path contains a built-in function it is evaluated and the built-in function
   * value is returned.
   * 
   * If the feature path ends with an array the array is converted to a comma separated string.
   * 
   * @param fsRef
   *          FeatureStructure reference (LowLevel API) to evaluate the feature path value
   * 
   * @param llCas
   *          LowLevelCAS for the fsRef
   * 
   * @return Returns the value of the feature path as String
   */
  String ll_getValueAsString(int fsRef, LowLevelCAS llCas);

  /**
   * Returns the type of the feature path.
   * 
   * @param fs
   *          FeatureStructure to evaluate the feature path type
   * 
   * @return Returns the type of the feature path or null if the feature path is not set.
   */
  Type getType(FeatureStructure fs);

  /**
   * Returns the type class of the feature path.
   * 
   * @param fs
   *          FeatureStructure to evaluate the feature path type class
   * 
   * @return Returns the type class of the feature path or null if the feature path is not set
   * @deprecated use getTypeClass (spelling fix)
   * @forRemoval 4.0.0
   */
  @Deprecated(since = "3.0.0")
  TypeClass getTypClass(FeatureStructure fs);

  /**
   * Returns the type class of the feature path.
   * 
   * @param fs
   *          FeatureStructure to evaluate the feature path type class
   * 
   * @return Returns the type class of the feature path or null if the feature path is not set
   */
  TypeClass getTypeClass(FeatureStructure fs);

  /**
   * Returns the feature path as string.
   * 
   * @return Returns the feature path as string.
   */
  String getFeaturePath();

  /**
   * Returns the String value of a string valued feature path.
   * 
   * @param fs
   *          FeatureStructure to evaluate the feature path value
   * 
   * @return Returns the String value of a string valued feature path or null if the feature path
   *         was not set
   */
  String getStringValue(FeatureStructure fs);

  /**
   * Returns the Integer value of an integer valued feature path.
   * 
   * @param fs
   *          FeatureStructure to evaluate the feature path value
   * 
   * @return Returns the Integer value of a integer valued feature path or null if the feature path
   *         was not set
   */
  Integer getIntValue(FeatureStructure fs);

  /**
   * Returns the Boolean value of a boolean valued feature path.
   * 
   * @param fs
   *          FeatureStructure to evaluate the feature path value
   * 
   * @return Returns the Boolean value of a boolean valued feature path or null if the feature path
   *         was not set
   */
  Boolean getBooleanValue(FeatureStructure fs);

  /**
   * Returns the Byte value of a byte valued feature path.
   * 
   * @param fs
   *          FeatureStructure to evaluate the feature path value
   * 
   * @return Returns the Byte value of a byte valued feature path or null if the feature path was
   *         not set
   */
  Byte getByteValue(FeatureStructure fs);

  /**
   * Returns the Double value of a double valued feature path.
   * 
   * @param fs
   *          FeatureStructure to evaluate the feature path value
   * 
   * @return Returns the Double value of a double valued feature path or null if the feature path
   *         was not set
   */
  Double getDoubleValue(FeatureStructure fs);

  /**
   * Returns the Float value of a float valued feature path.
   * 
   * @param fs
   *          FeatureStructure to evaluate the feature path value
   * 
   * @return Returns the Float value of a float valued feature path or null if the feature path was
   *         not set
   */
  Float getFloatValue(FeatureStructure fs);

  /**
   * Returns the Long value of a long valued feature path.
   * 
   * @param fs
   *          FeatureStructure to evaluate the feature path value
   * 
   * @return Returns the Long value of a long valued feature path or null if the feature path was
   *         not set
   */
  Long getLongValue(FeatureStructure fs);

  /**
   * Returns the Short value of a short valued feature path.
   * 
   * @param fs
   *          FeatureStructure to evaluate the feature path value
   * 
   * @return Returns the Short value of a short valued feature path or null if the feature path was
   *         not set
   */
  Short getShortValue(FeatureStructure fs);

  /**
   * Returns the FeatureStructure of a FeatureStructure valued feature path.
   * 
   * @param fs
   *          FeatureStructure to evaluate the feature path value
   * 
   * @return Returns the FeatureStructure value of a FeatureStructure valued feature path or null if
   *         the feature path was not set
   */
  FeatureStructure getFSValue(FeatureStructure fs);

  // /**
  // * Returns the Java Object value of a JavaObject valued feature path.
  // *
  // * @param fs
  // * FeatureStructure to evaluate the feature path value
  // *
  // * @return Returns the Java Object value of a JavaObject valued feature path
  // * or null if the feature path was not set
  // */
  // public Object getJavaObjectValue(FeatureStructure fs);

}
