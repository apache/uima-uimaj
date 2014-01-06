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

/**
 * Interface for feature structures.
 * 
 * <p>
 * Note that object identity is not meaningful for feature structures. You may ask the CAS for the
 * same feature structure two times in a row, and get different object references. Use
 * {@link #equals equals()} instead.
 * 
 * 
 */
public interface FeatureStructure {

  /**
   * Get the type of this FS.
   * 
   * @return The type.
   */
  Type getType();

  /**
   * Set a feature value to another FS.
   * 
   * @param feat
   *          The feature whose value should be set.
   * @param fs
   *          The value FS.
   * @exception CASRuntimeException
   *              If there is a typing violation, i.e., if <code>feat</code> is not defined for
   *              the type of this FS, or the range type of <code>feat</code> is not a supertype
   *              of <code>fs.getType()</code>.
   */
  void setFeatureValue(Feature feat, FeatureStructure fs) throws CASRuntimeException;

  /**
   * Get a feature value.
   * 
   * @param feat
   *          The feature whose value we want to get.
   * @return The value; may be <code>null</code> if the value has not been set.
   * @exception CASRuntimeException
   *              If there is a typing violation, i.e., if <code>feat</code> is not defined for
   *              the type of this FS, or the range type of <code>feat</code> is Float, Integer or
   *              String.
   */
  FeatureStructure getFeatureValue(Feature feat) throws CASRuntimeException;

  /**
   * Set the string value of a feature.
   * 
   * @param feat
   *          The feature whose value we want to set.
   * @param s
   *          The string we're setting the feature to.
   * @exception CASRuntimeException
   *              If there is a typing violation, i.e., if <code>feat</code> is not defined for
   *              <code>this.getType()</code> or <code>feat.getRange()</code> is not
   *              <code>CAS.STRING_TYPE</code>.
   */
  void setStringValue(Feature feat, String s) throws CASRuntimeException;

  /**
   * Get the string value under a feature.
   * 
   * @param f
   *          The feature for which we want the value.
   * @return The value of this feature; may be <code>null</code> if the value has not been set.
   * @exception CASRuntimeException
   *              If there is a typing violation, i.e., if <code>f</code> is not defined for the
   *              type of this feature structure, or if the range type of <code>f</code> is not
   *              String.
   */
  String getStringValue(Feature f) throws CASRuntimeException;

  /**
   * Get the float value of a feature. This method will throw an exception if the feature is not
   * float valued.
   * 
   * @param feat
   *          The feature whose value we want to get.
   * @return The value float; <code>0.0</code> if the value has not been set.
   * @exception CASRuntimeException
   *              If <code>feat</code> is not defined for the type of this FS, or if it is not
   *              float valued.
   */
  float getFloatValue(Feature feat) throws CASRuntimeException;

  /**
   * Set the float value of a feature.
   * 
   * @param feat
   *          The feature whose value we want to set.
   * @param f
   *          The float we're setting the feature to.
   * @exception CASRuntimeException
   *              If <code>feat</code> is not defined for the type of this FS, or if it is not
   *              float valued.
   */
  void setFloatValue(Feature feat, float f) throws CASRuntimeException;

  /**
   * Get the int value of a feature. This method will throw an exception if the feature is not int
   * valued.
   * 
   * @param feat
   *          The feature whose value we want to get.
   * @return The value int; <code>0</code> if the value has not been set.
   * @exception CASRuntimeException
   *              If <code>feat</code> is not defined for the type of this FS, or if it is not int
   *              valued.
   */
  int getIntValue(Feature feat) throws CASRuntimeException;

  /**
   * Set the int value of a feature.
   * 
   * @param feat
   *          The feature whose value we want to set.
   * @param i
   *          The int we're setting the feature to.
   */
  void setIntValue(Feature feat, int i) throws CASRuntimeException;

  /**
   * 
   * Get the byte value of a feature. This method will throw an exception if the feature is not byte
   * valued.
   * 
   * @param feat
   *          The feature whose value we want to set.
   * @return The value byte; <code>0</code> if the value has not been set.
   * @throws CASRuntimeException tbd
   */
  byte getByteValue(Feature feat) throws CASRuntimeException;

  /**
   * Set the byte (8 bit) value of a feature.
   * 
   * @param feat
   *          The feature whose value we want to set.
   * @param i
   *          The 8bit value we're setting the feature to.
   * @throws CASRuntimeException tbd
   */
  void setByteValue(Feature feat, byte i) throws CASRuntimeException;

  /**
   * Get the boolean value of a feature. This method will throw an exception if the feature is not
   * boolean valued.
   * 
   * @param feat
   *          The feature whose value we want to get.
   * @return The value int; <code>0</code> if the value has not been set.
   * @exception CASRuntimeException
   *              If <code>feat</code> is not defined for the type of this FS, or if it is not
   *              boolean valued.
   */
  boolean getBooleanValue(Feature feat) throws CASRuntimeException;

  /**
   * Set the boolean value of a feature.
   * 
   * @param feat
   *          The feature whose value we want to set.
   * @param i
   *          The boolean value we're setting the feature to.
   */
  void setBooleanValue(Feature feat, boolean i) throws CASRuntimeException;

  /**
   * Get the short value of a feature. This method will throw an exception if the feature is not
   * short valued.
   * 
   * @param feat
   *          The feature whose value we want to get.
   * @return The value int; <code>0</code> if the value has not been set.
   * @exception CASRuntimeException
   *              If <code>feat</code> is not defined for the type of this FS, or if it is not
   *              short valued.
   */
  short getShortValue(Feature feat) throws CASRuntimeException;

  /**
   * Set the short (16 bit) value of a feature.
   * 
   * @param feat
   *          The feature whose value we want to set.
   * @param i
   *          The short (16bit) value we're setting the feature to.
   */
  void setShortValue(Feature feat, short i) throws CASRuntimeException;

  /**
   * Get the long value of a feature. This method will throw an exception if the feature is not long
   * valued.
   * 
   * @param feat
   *          The feature whose value we want to get.
   * @return The value int; <code>0</code> if the value has not been set.
   * @exception CASRuntimeException
   *              If <code>feat</code> is not defined for the type of this FS, or if it is not
   *              long valued.
   */
  long getLongValue(Feature feat) throws CASRuntimeException;

  /**
   * Set the long (64 bit) value of a feature.
   * 
   * @param feat
   *          The feature whose value we want to set.
   * @param i
   *          The long (64bit) value we're setting the feature to.
   */
  void setLongValue(Feature feat, long i) throws CASRuntimeException;

  /**
   * Get the double value of a feature. This method will throw an exception if the feature is not
   * double valued.
   * 
   * @param feat
   *          The feature whose value we want to get.
   * @return The value int; <code>0</code> if the value has not been set.
   * @exception CASRuntimeException
   *              If <code>feat</code> is not defined for the type of this FS, or if it is not
   *              double valued.
   */
  double getDoubleValue(Feature feat) throws CASRuntimeException;

  /**
   * Set the double value of a feature.
   * 
   * @param feat
   *          The feature whose value we want to set.
   * @param i
   *          The double value we're setting the feature to.
   */
  void setDoubleValue(Feature feat, double i) throws CASRuntimeException;

  /**
   * Get the value of the feature as a string if the type of the feature is one of the primitive
   * type.
   * 
   * @param feat
   *          The feature whose value we want to get and whose type is one of the primitve types.
   * @return A string representation of the feature value.
   * @throws CASRuntimeException
   *           If <code>feat</code> is not defined for the type of this FS, or if the type is not
   *           a primitive type.
   */
  String getFeatureValueAsString(Feature feat) throws CASRuntimeException;

  /**
   * Sets the value of a feature from a string input if the feature type is one of the primitive
   * types.
   * 
   * @param feat 
   *          The feature whose value we want to set.
   * @param s
   *          The string value that the feature will be set to.
   * @throws CASRuntimeException
   *           If <code>feat</code> is not a primitive type or the value cannot be converted to
   *           this type.
   */
  void setFeatureValueFromString(Feature feat, String s) throws CASRuntimeException;

  /**
   * A feature structure is equal to another feature structure iff it is identical in the underlying
   * representation.
   * 
   * @exception ClassCastException
   *              If <code>o</code> is not a FS.
   */
  boolean equals(Object o) throws ClassCastException;

  /**
   * Creates a copy of this feature structure. The returned feature structure is a new and separate
   * object but all features of the feature structure which are not of builtin types (integer,
   * float, string) will be shared between the clone and it's source FS.
   * 
   * @return a FeatureStructure that is the cloned copy of this FeatureStructure.
   * @throws CASRuntimeException passthru
   */
  Object clone() throws CASRuntimeException;

  /**
   * Will return a hash code that's consistent with equality, i.e., if two FSs are equal, they will
   * also return the same hash code.
   * 
   * @return The hash code.
   */
  int hashCode();

  /**
   * Return the CAS that this FS belongs to.
   * 
   * @return The CAS.
   */
  CAS getCAS();

}
