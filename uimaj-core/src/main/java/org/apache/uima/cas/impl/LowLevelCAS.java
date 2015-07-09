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

import org.apache.uima.cas.FeatureStructure;

/**
 * Defines the low-level CAS APIs. The low-level CAS APIs provide no access to feature structure
 * objects. All access to CAS data is through integer IDs that represent handles to the CAS data.
 * 
 * <p>
 * The basic concepts of the low-level APIs is very simple. The CAS does not internally hold its
 * data as Java objects, for performance reasons. The low-level APIs give you access to this data
 * more or less directly. Instead of creating Java objects that you can manipulate in an OO fashion,
 * you only obtain int values that are used as references to internal data. Thus, if you would like
 * to change values on a particular piece of data, you need to hand the FS reference to the API, as
 * opposed to calling methods on a FS object. The tricky part about the low-level APIs is that it's
 * all just ints, and it is very easy to confuse an int that represents a type code with an int that
 * represents a feature code or a FS reference. Particular care is therefore necessary when using
 * the low-level APIs. Please follow the guidelines for turning on a minimum of checks below.
 * 
 * <p>
 * This API represents the supported access to low-level features of the UIMA framework CAS
 * implementation. Other public APIs in the implementation are not supported and are subject to
 * change without notice.
 * 
 * <p>
 * Please note that you really need to know what you're doing when using these APIs. Incorrect usage
 * of these APIs can and will cause completely unpredictable results; likely your application will
 * crash, and it will do so much later than where the incorrect usage occured. The low-level APIs
 * are very hard to debug. You should only use the low-level APIs if you have carefully profiled
 * your application and are sure that the high-level CAS APIs or the JCAS represent a performance
 * bottleneck.
 * 
 * <p>
 * Note that most low-level APIs have versions that allow you to turn some form of parameter
 * checking on. We strongly encourage you to use those versions during development, they may save
 * you a lot of time. One way you can use the type checking switch is by having a constant
 * 
 * <pre>
 * 
 * static final boolean DO_TYPE_CHECK = true;
 * </pre>
 * 
 * <p>
 * which you can use during development. For production level code, you can later change the
 * constant to <code>false</code>. The performance difference to the non-parametrized versions of
 * the getters and setters is probably negligible or may not even exist, depending on the Java
 * runtime.
 * 
 * <p>
 * Please note that even with the check switches turned on, it is still possible to make mistakes
 * that only show up much later in processing. The main problem is that it is simply impossible to
 * determine for certain whether a given FS reference is valid or not. The implementation can only
 * determine that it looks like a reference that points at a valid piece of data, but this could be
 * accidental.
 * 
 */
public interface LowLevelCAS {
  /**
   * Not a valid type. Type class constant returned by
   * {@link #ll_getTypeClass(int) ll_getTypeClass()}.
   */
  public static final int TYPE_CLASS_INVALID = 0;

  /**
   * Integer type. Type class constant returned by {@link #ll_getTypeClass(int) ll_getTypeClass()}.
   */
  public static final int TYPE_CLASS_INT = 1;

  /**
   * Float type. Type class constant returned by {@link #ll_getTypeClass(int) ll_getTypeClass()}.
   */
  public static final int TYPE_CLASS_FLOAT = 2;

  /**
   * String type. Type class constant returned by {@link #ll_getTypeClass(int) ll_getTypeClass()}.
   */
  public static final int TYPE_CLASS_STRING = 3;

  /**
   * Integer type. Type class constant returned by {@link #ll_getTypeClass(int) ll_getTypeClass()}.
   */
  public static final int TYPE_CLASS_INTARRAY = 4;

  /**
   * Float array type. Type class constant returned by
   * {@link #ll_getTypeClass(int) ll_getTypeClass()}.
   */
  public static final int TYPE_CLASS_FLOATARRAY = 5;

  /**
   * String array type. Type class constant returned by
   * {@link #ll_getTypeClass(int) ll_getTypeClass()}.
   */
  public static final int TYPE_CLASS_STRINGARRAY = 6;

  /**
   * FS array type. Type class constant returned by {@link #ll_getTypeClass(int) ll_getTypeClass()}.
   */
  public static final int TYPE_CLASS_FSARRAY = 7;

  /**
   * FS type (all other types, include all user-defined ones). Type class constant returned by
   * {@link #ll_getTypeClass(int) ll_getTypeClass()}.
   */
  public static final int TYPE_CLASS_FS = 8;

  public static final int TYPE_CLASS_BOOLEAN = 9;

  public static final int TYPE_CLASS_BYTE = 10;

  public static final int TYPE_CLASS_SHORT = 11;

  public static final int TYPE_CLASS_LONG = 12;

  public static final int TYPE_CLASS_DOUBLE = 13;

  public static final int TYPE_CLASS_BOOLEANARRAY = 14;

  public static final int TYPE_CLASS_BYTEARRAY = 15;

  public static final int TYPE_CLASS_SHORTARRAY = 16;

  public static final int TYPE_CLASS_LONGARRAY = 17;

  public static final int TYPE_CLASS_DOUBLEARRAY = 18;

  static final int NULL_FS_REF = 0;

  /**
   * Get the low-level version of the type system object. It provides access to the low-level type
   * and feature codes you need to use the data creation and access APIs.
   * 
   * @return The low-level type system.
   */
  LowLevelTypeSystem ll_getTypeSystem();

  /**
   * Get the low-level version of the index repository. Use it to gain access to low-level indexes,
   * and thus, low-level iterators.
   * 
   * @return A low-level version of the index repository.
   */
  LowLevelIndexRepository ll_getIndexRepository();

  /**
   * Create a new FS on the heap.
   * 
   * @param typeCode
   *          The low-level code of the type of the FS that should be created. If the
   *          <code>typeCode</code> is not a valid type code, the results of this call are
   *          undefined.
   * @return The reference of the newly created FS.
   */
  int ll_createFS(int typeCode);

  /**
   * Create a new FS on the heap.
   * 
   * @param typeCode
   *          The low-level code of the type of the FS that should be created. If the
   *          <code>typeCode</code> is not a valid type code and the type check flag is not set,
   *          the results of this call are undefined.
   * @param doTypeCheck -         
   * @return The reference of the newly created FS.
   * @exception LowLevelException
   *              If the type checking switch is set and the type code argument is not valid.
   */
  int ll_createFS(int typeCode, boolean doTypeCheck);

  /**
   * Create a new array.
   * 
   * @param typeCode
   *          The type code of the array type. If this is not a valid array type code, the behavior
   *          of this call is undefined. Only works for arrays where a value is kept in the main
   *          heap (use other ll_createXxxArray for boolean, byte, short, long, and double)
   * @param arrayLength
   *          The length of the array to be created.
   * @return The address of the newly created array.
   */
  int ll_createArray(int typeCode, int arrayLength);

  /**
   * Create a new array.
   * 
   * @param typeCode
   *          The type code of the array to be created.
   * @param arrayLength
   *          The length of the array to be created.
   * @param doChecks
   *          Switch to turn on various sanity checks.
   * @return The address of the newly created array.
   */
  int ll_createArray(int typeCode, int arrayLength, boolean doChecks);

  int ll_createBooleanArray(int arrayLength);

  int ll_createByteArray(int arrayLength);

  int ll_createShortArray(int arrayLength);

  int ll_createLongArray(int arrayLength);

  int ll_createDoubleArray(int arrayLength);

  /**
   * Get the size of an array.
   * 
   * @param arrayFsRef
   *          The array reference.
   * @return The size of the array.
   */
  int ll_getArraySize(int arrayFsRef);

  /**
   * Get the low-level reference from an existing FS object. Use this API if you already have a FS
   * object from somewhere, and want to apply low-level APIs to it.
   * 
   * @param fsImpl
   *          The FS object for which we want the reference.
   * @return The low-level reference of the FS object parameter.
   */
  int ll_getFSRef(FeatureStructure fsImpl);

  /**
   * Return a FS object that corresponds to a low-level reference. Note that this <b>must</b> be a
   * valid reference that has been obtained from the low-level APIs. If the input reference
   * parameter does not represent a valid reference, the results of this call are undefined.
   * 
   * @param fsRef
   *          The FS reference.
   * @param <T> the Java class for the Feature Structure
   * @return A FS object corresponding to the input reference.
   */
  <T extends FeatureStructure> T ll_getFSForRef(int fsRef);

  /**
   * Get the value of an integer valued feature.
   * 
   * @param fsRef
   *          The reference to the FS from which to obtain the feature value.
   * @param featureCode
   *          The low-level code of the feature whose value is to be returned.
   * @return The value of the feature.
   */
  int ll_getIntValue(int fsRef, int featureCode);

  /**
   * Get the value of a float valued feature.
   * 
   * @param fsRef
   *          The reference to the FS from which to obtain the feature value.
   * @param featureCode
   *          The low-level code of the feature whose value is to be returned.
   * @return The value of the feature.
   */
  float ll_getFloatValue(int fsRef, int featureCode);

  /**
   * Get the value of a string valued feature.
   * 
   * @param fsRef
   *          The reference to the FS from which to obtain the feature value.
   * @param featureCode
   *          The low-level code of the feature whose value is to be returned.
   * @return The value of the feature.
   */
  String ll_getStringValue(int fsRef, int featureCode);

  /**
   * Get the value of a FS reference valued feature.
   * 
   * @param fsRef
   *          The reference to the FS from which to obtain the feature value.
   * @param featureCode
   *          The low-level code of the feature whose value is to be returned.
   * @return The value of the feature.
   */
  int ll_getRefValue(int fsRef, int featureCode);

  /**
   * Get the value of an integer valued feature.
   * 
   * @param fsRef
   *          The reference to the FS from which to obtain the feature value.
   * @param featureCode
   *          The low-level code of the feature whose value is to be returned.
   * @param doTypeChecks
   *          Switch to turn on type checking.
   * @return The value of the feature.
   */
  int ll_getIntValue(int fsRef, int featureCode, boolean doTypeChecks);

  /**
   * Get the value of a float valued feature.
   * 
   * @param fsRef
   *          The reference to the FS from which to obtain the feature value.
   * @param featureCode
   *          The low-level code of the feature whose value is to be returned.
   * @param doTypeChecks
   *          Switch to turn on type checking.
   * @return The value of the feature.
   */
  float ll_getFloatValue(int fsRef, int featureCode, boolean doTypeChecks);

  /**
   * Get the value of a string valued feature.
   * 
   * @param fsRef
   *          The reference to the FS from which to obtain the feature value.
   * @param featureCode
   *          The low-level code of the feature whose value is to be returned.
   * @param doTypeChecks
   *          Switch to turn on type checking.
   * @return The value of the feature.
   */
  String ll_getStringValue(int fsRef, int featureCode, boolean doTypeChecks);

  /**
   * Get the value of a FS reference valued feature.
   * 
   * @param fsRef
   *          The reference to the FS from which to obtain the feature value.
   * @param featureCode
   *          The low-level code of the feature whose value is to be returned.
   * @param doTypeChecks
   *          Switch to turn on type checking.
   * @return The value of the feature.
   */
  int ll_getRefValue(int fsRef, int featureCode, boolean doTypeChecks);

  /**
   * Set the value of an integer feature.
   * 
   * @param fsRef
   *          The reference of the FS on which the feature should be set.
   * @param featureCode
   *          The low-level feature code for the feature that should be set.
   * @param value
   *          The value to be assigned to the feature.
   */
  void ll_setIntValue(int fsRef, int featureCode, int value);

  /**
   * Set the value of a float feature.
   * 
   * @param fsRef
   *          The reference of the FS on which the feature should be set.
   * @param featureCode
   *          The low-level feature code for the feature that should be set.
   * @param value
   *          The value to be assigned to the feature.
   */
  void ll_setFloatValue(int fsRef, int featureCode, float value);

  /**
   * Set the value of a string feature.
   * 
   * @param fsRef
   *          The reference of the FS on which the feature should be set.
   * @param featureCode
   *          The low-level feature code for the feature that should be set.
   * @param value
   *          The value to be assigned to the feature.
   */
  void ll_setStringValue(int fsRef, int featureCode, String value);

  /**
   * Set the value of a FS reference feature.
   * 
   * @param fsRef
   *          The reference of the FS on which the feature should be set.
   * @param featureCode
   *          The low-level feature code for the feature that should be set.
   * @param value
   *          The value to be assigned to the feature.
   */
  void ll_setRefValue(int fsRef, int featureCode, int value);

  /**
   * Set the value of an integer feature.
   * 
   * @param fsRef
   *          The reference of the FS on which the feature should be set.
   * @param featureCode
   *          The low-level feature code for the feature that should be set.
   * @param value
   *          The value to be assigned to the feature.
   * @param doTypeChecks
   *          Switch to turn on type checking.
   */
  void ll_setIntValue(int fsRef, int featureCode, int value, boolean doTypeChecks);

  /**
   * Set the value of a float feature.
   * 
   * @param fsRef
   *          The reference of the FS on which the feature should be set.
   * @param featureCode
   *          The low-level feature code for the feature that should be set.
   * @param value
   *          The value to be assigned to the feature.
   * @param doTypeChecks
   *          Switch to turn on type checking.
   */
  void ll_setFloatValue(int fsRef, int featureCode, float value, boolean doTypeChecks);

  /**
   * Set the value of a string feature.
   * 
   * @param fsRef
   *          The reference of the FS on which the feature should be set.
   * @param featureCode
   *          The low-level feature code for the feature that should be set.
   * @param value
   *          The value to be assigned to the feature.
   * @param doTypeChecks
   *          Switch to turn on type checking.
   */
  void ll_setStringValue(int fsRef, int featureCode, String value, boolean doTypeChecks);

  void ll_setCharBufferValue(int fsRef, int featureCode, char[] buffer, int start, int length,
          boolean doChecks);

  void ll_setCharBufferValue(int fsRef, int featureCode, char[] buffer, int start, int length);

  int ll_getCharBufferValueSize(int fsRef, int featureCode);

  int ll_copyCharBufferValue(int fsRef, int featureCode, char[] buffer, int start);

  /**
   * Set the value of a FS reference feature.
   * 
   * @param fsRef
   *          The reference of the FS on which the feature should be set.
   * @param featureCode
   *          The low-level feature code for the feature that should be set.
   * @param value
   *          The value to be assigned to the feature.
   * @param doTypeChecks
   *          Switch to turn on type checking.
   */
  void ll_setRefValue(int fsRef, int featureCode, int value, boolean doTypeChecks);

  /**
   * Get the value of an array at a certain position.
   * 
   * @param fsRef
   *          The reference to the array FS.
   * @param position
   *          The position whose value should be returned.
   * @return The value at <code>position</code>.
   */
  int ll_getIntArrayValue(int fsRef, int position);

  /**
   * Get the value of a float array at a certain position.
   * 
   * @param fsRef
   *          The reference to the array FS.
   * @param position
   *          The position whose value should be returned.
   * @return The value at <code>position</code>.
   */
  float ll_getFloatArrayValue(int fsRef, int position);

  /**
   * Get the value of a string array at a certain position.
   * 
   * @param fsRef
   *          The reference to the array FS.
   * @param position
   *          The position whose value should be returned.
   * @return The value at <code>position</code>.
   */
  String ll_getStringArrayValue(int fsRef, int position);

  /**
   * Get the value of a FS reference array at a certain position.
   * 
   * @param fsRef
   *          The reference to the array FS.
   * @param position
   *          The position whose value should be returned.
   * @return The value at <code>position</code>.
   */
  int ll_getRefArrayValue(int fsRef, int position);

  /**
   * Get the value of an integer array at a certain position.
   * 
   * @param fsRef
   *          The reference to the array FS.
   * @param position
   *          The position whose value should be returned.
   * @param doTypeChecks
   *          Switch to turn on type checking.
   * @return The value at <code>position</code>.
   */
  int ll_getIntArrayValue(int fsRef, int position, boolean doTypeChecks);

  /**
   * Get the value of a float array at a certain position.
   * 
   * @param fsRef
   *          The reference to the array FS.
   * @param position
   *          The position whose value should be returned.
   * @param doTypeChecks
   *          Switch to turn on type checking.
   * @return The value at <code>position</code>.
   */
  float ll_getFloatArrayValue(int fsRef, int position, boolean doTypeChecks);

  /**
   * Get the value of a string array at a certain position.
   * 
   * @param fsRef
   *          The reference to the array FS.
   * @param position
   *          The position whose value should be returned.
   * @param doTypeChecks
   *          Switch to turn on type checking.
   * @return The value at <code>position</code>.
   */
  String ll_getStringArrayValue(int fsRef, int position, boolean doTypeChecks);

  /**
   * Get the value of a FS reference array at a certain position.
   * 
   * @param fsRef
   *          The reference to the array FS.
   * @param position
   *          The position whose value should be returned.
   * @param doTypeChecks
   *          Switch to turn on type checking.
   * @return The value at <code>position</code>.
   */
  int ll_getRefArrayValue(int fsRef, int position, boolean doTypeChecks);

  /**
   * Set the value of an integer array at a certain position.
   * 
   * @param fsRef
   *          The FS reference of the array.
   * @param position
   *          The position whose value will be changed.
   * @param doTypeChecks
   *          Switch to turn on type and bounds checking.
   * @param value
   *          The new value.
   */
  void ll_setIntArrayValue(int fsRef, int position, int value, boolean doTypeChecks);

  /**
   * Set the value of an integer array at a certain position.
   * 
   * @param fsRef
   *          The FS reference of the array.
   * @param position
   *          The position whose value will be changed.
   * @param doTypeChecks
   *          Switch to turn on type and bounds checking.
   * @param value
   *          The new value.
   */
  void ll_setFloatArrayValue(int fsRef, int position, float value, boolean doTypeChecks);

  /**
   * Set the value of an integer array at a certain position.
   * 
   * @param fsRef
   *          The FS reference of the array.
   * @param position
   *          The position whose value will be changed.
   * @param doTypeChecks
   *          Switch to turn on type and bounds checking.
   * @param value
   *          The new value.
   */
  void ll_setStringArrayValue(int fsRef, int position, String value, boolean doTypeChecks);

  /**
   * Set the value of an integer array at a certain position.
   * 
   * @param fsRef
   *          The FS reference of the array.
   * @param position
   *          The position whose value will be changed.
   * @param doTypeChecks
   *          Switch to turn on type and bounds checking.
   * @param value
   *          The new value.
   */
  void ll_setRefArrayValue(int fsRef, int position, int value, boolean doTypeChecks);

  /**
   * Set the value of an integer array at a certain position.
   * 
   * @param fsRef
   *          The FS reference of the array.
   * @param position
   *          The position whose value will be changed.
   * @param value
   *          The new value.
   */
  void ll_setIntArrayValue(int fsRef, int position, int value);

  /**
   * Set the value of an integer array at a certain position.
   * 
   * @param fsRef
   *          The FS reference of the array.
   * @param position
   *          The position whose value will be changed.
   * @param value
   *          The new value.
   */
  void ll_setFloatArrayValue(int fsRef, int position, float value);

  /**
   * Set the value of an integer array at a certain position.
   * 
   * @param fsRef
   *          The FS reference of the array.
   * @param position
   *          The position whose value will be changed.
   * @param value
   *          The new value.
   */
  void ll_setStringArrayValue(int fsRef, int position, String value);

  /**
   * Set the value of an integer array at a certain position.
   * 
   * @param fsRef
   *          The FS reference of the array.
   * @param position
   *          The position whose value will be changed.
   * @param value
   *          The new value.
   */
  void ll_setRefArrayValue(int fsRef, int position, int value);

  /**
   * Get the type code for a FS reference. No bounds checks are performed. If <code>fsRef</code>
   * is not a fs reference, the results are undefined. There is also a checked version of this call,
   * which will give better error messages in case of problems.
   * 
   * @param fsRef
   *          The FS reference.
   * @return The type code for the FS reference; a return value of <code>0</code> means that the
   *         fsRef is invalid, i.e., <code>NULL_FS_REF</code> (but see remarks on bounds checking
   *         for this method).
   */
  int ll_getFSRefType(int fsRef);

  /**
   * Get the type code for a FS reference.
   * 
   * @param fsRef
   *          The FS reference.
   * @param doChecks
   *          Check fsRef for out-of-range errors. If this switch is not set, and the input
   *          reference is not a valid reference, the results are undefined.
   * @return The type code for the FS reference; a return value of <code>0</code> means that the
   *         fsRef is invalid, i.e., <code>NULL_FS_REF</code> (but see remarks on bounds checking
   *         for this method).
   */
  int ll_getFSRefType(int fsRef, boolean doChecks);

  /**
   * Determine the type class of a type. This is useful for generic CAS exploiters to determine what
   * kind of data they're looking at. The type classes currently defined are:
   * <ul>
   * <li><code>TYPE_CLASS_INVALID</code> -- Not a valid type code.</li>
   * <li><code>TYPE_CLASS_INT</code> -- Integer type. </li>
   * <li><code>TYPE_CLASS_FLOAT</code> -- Float type.</li>
   * <li><code>TYPE_CLASS_STRING</code> -- String type.</li>
   * <li><code>TYPE_CLASS_BOOLEAN</code> -- Boolean type.</li>
   * <li><code>TYPE_CLASS_BYTE</code> -- Byte type.</li>
   * <li><code>TYPE_CLASS_SHORT</code> -- Short type.</li>
   * <li><code>TYPE_CLASS_LONG</code> -- Long type.</li>
   * <li><code>TYPE_CLASS_DOUBLE</code> -- Double type.</li>
   * <li><code>TYPE_CLASS_INTARRAY</code> -- Integer array.</li>
   * <li><code>TYPE_CLASS_FLOATARRAY</code> -- Float array.</li>
   * <li><code>TYPE_CLASS_STRINGARRAY</code> -- String array.</li>
   * <li><code>TYPE_CLASS_BOOLEANARRAY</code> -- Boolean array.</li>
   * <li><code>TYPE_CLASS_BYTEARRAY</code> -- Byte array.</li>
   * <li><code>TYPE_CLASS_SHORTARRAY</code> -- Short array.</li>
   * <li><code>TYPE_CLASS_LONGARRAY</code> -- Long array.</li>
   * <li><code>TYPE_CLASS_DOUBLEARRAY</code> -- Double array.</li>
   * <li><code>TYPE_CLASS_FSARRAY</code> -- FS array.</li>
   * <li><code>TYPE_CLASS_FS</code> -- FS type, i.e., all other types, including all user-defined
   * types.</li>
   * </ul>
   * This method is on the CAS, not the type system, since the specific properties of types are
   * specific to the CAS. The type system does not know, for example, that the CAS treats arrays
   * specially.
   * 
   * @param typeCode
   *          The type code.
   * @return A type class for the type code. <code>TYPE_CLASS_INVALID</code> if the type code
   *         argument does not represent a valid type code.
   */
  int ll_getTypeClass(int typeCode);

  /**
   * Checks if the type code is that of a reference type (anything that's not a basic type,
   * currently Integer, String and Float).
   * 
   * @param typeCode
   *          The type code to check.
   * @return <code>true</code> iff <code>typeCode</code> is the type code of a reference type.
   */
  boolean ll_isRefType(int typeCode);

  byte ll_getByteValue(int fsRef, int featureCode);

  boolean ll_getBooleanValue(int fsRef, int featureCode);

  short ll_getShortValue(int fsRef, int featureCode);

  long ll_getLongValue(int fsRef, int featureCode);

  double ll_getDoubleValue(int fsRef, int featureCode);

  byte ll_getByteValue(int fsRef, int featureCode, boolean doTypeChecks);

  boolean ll_getBooleanValue(int fsRef, int featureCode, boolean doTypeChecks);

  short ll_getShortValue(int fsRef, int featureCode, boolean doTypeChecks);

  long ll_getLongValue(int fsRef, int featureCode, boolean doTypeChecks);

  double ll_getDoubleValue(int fsRef, int featureCode, boolean doTypeChecks);

  void ll_setBooleanValue(int fsRef, int featureCode, boolean value);

  void ll_setByteValue(int fsRef, int featureCode, byte value);

  void ll_setShortValue(int fsRef, int featureCode, short value);

  void ll_setLongValue(int fsRef, int featureCode, long value);

  void ll_setDoubleValue(int fsRef, int featureCode, double value);

  void ll_setBooleanValue(int fsRef, int featureCode, boolean value, boolean doTypeChecks);

  void ll_setByteValue(int fsRef, int featureCode, byte value, boolean doTypeChecks);

  void ll_setShortValue(int fsRef, int featureCode, short value, boolean doTypeChecks);

  void ll_setLongValue(int fsRef, int featureCode, long value, boolean doTypeChecks);

  void ll_setDoubleValue(int fsRef, int featureCode, double value, boolean doTypeChecks);

  byte ll_getByteArrayValue(int fsRef, int position);

  boolean ll_getBooleanArrayValue(int fsRef, int position);

  short ll_getShortArrayValue(int fsRef, int position);

  long ll_getLongArrayValue(int fsRef, int position);

  double ll_getDoubleArrayValue(int fsRef, int position);

  /* for jcas / featurepath * */
  byte ll_getByteArrayValue(int fsRef, int position, boolean doTypeChecks);

  boolean ll_getBooleanArrayValue(int fsRef, int position, boolean doTypeChecks);

  short ll_getShortArrayValue(int fsRef, int position, boolean doTypeChecks);

  long ll_getLongArrayValue(int fsRef, int position, boolean doTypeChecks);

  double ll_getDoubleArrayValue(int fsRef, int position, boolean doTypeChecks);

  void ll_setByteArrayValue(int fsRef, int position, byte value);

  void ll_setBooleanArrayValue(int fsRef, int position, boolean b);

  void ll_setShortArrayValue(int fsRef, int position, short value);

  void ll_setLongArrayValue(int fsRef, int position, long value);

  void ll_setDoubleArrayValue(int fsRef, int position, double d);

  /* for jcas / featurepath * */
  void ll_setByteArrayValue(int fsRef, int position, byte value, boolean doTypeChecks);

  void ll_setBooleanArrayValue(int fsRef, int position, boolean b, boolean doTypeChecks);

  void ll_setShortArrayValue(int fsRef, int position, short value, boolean doTypeChecks);

  void ll_setLongArrayValue(int fsRef, int position, long value, boolean doTypeChecks);

  void ll_setDoubleArrayValue(int fsRef, int position, double d, boolean doTypeChecks);

  CASImpl ll_getSofaCasView(int addr);
  
  int ll_getSofa();
}

