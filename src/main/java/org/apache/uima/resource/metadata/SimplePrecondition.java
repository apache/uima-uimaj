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

package org.apache.uima.resource.metadata;

import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.UIMA_UnsupportedOperationException;
import org.apache.uima.cas.FSMatchConstraint;

/**
 * <code>SimplePrecondition</code> defines a few simple conditions that can be evaluated against a
 * {@link org.apache.uima.cas.CAS}.
 * <p>
 * A value in the CAS (see below) will be compared against the precondition's
 * {@link #getComparisonValue() comparisonValue} property using the specified
 * {@link #getPredicate() predicate}.
 * <p>
 * The value in the CAS is specified by providing values for
 * {@link #setFsIndexName(String) indexName},
 * {@link #setFsMatchConstraint(FSMatchConstraint) fsMatchConstraint}, and
 * {@link #setFeatureName(String) featureName}. If any FeatureStructure in the specified index
 * matches the FS match constraint and has a feature value that satisfies this precondition's
 * precicate, the precondition will be considered satisfied. If the index name is omitted, the
 * default Annotation index (if present) will be used.
 * <p>
 * The possible predicates are defined by constants on this interface. The currently avaialble
 * predicates are:
 * <ul>
 * <li><code>EQUAL</code> - evaluates to true if and only if the values are equal (according to
 * the equality rules of their data type).</li>
 * <li><code>ELEMENT_OF</code> - the comparison value must be an array. Evaluates to true if and
 * only if the test value is equal to one of the elements of the comparison array.</li>
 * <li><code>LANGUAGE_SUBSUMED</code> - the comparison value must be an array of Strings, each of
 * which is an ISO language identifier. Evaluates to true if and only if the test value is an ISO
 * language identifier that is subsumed by one of the values of the comparison array. (For example,
 * "en_US" is subsumed by "en.")</li>
 * </ul>
 * 
 * As with all {@link MetaDataObject}s, a <code>SimplePrecondition</code> may or may not be
 * modifiable. An application can find out by calling the {@link #isModifiable()} method.
 */
public interface SimplePrecondition extends Precondition {

  /**
   * Gets the type of this precondition. Each sub-interface of <code>Precondition</code> has its
   * own standard type identifier String. These identifier Strings are used instead of Java class
   * names in order to ease portability of metadata to other languages.
   * 
   * @return {@link #PRECONDITION_TYPE}
   */
  public String getPreconditionType();

  /**
   * Retrieves the name of the FeatureStructure index containing FeatureStructures to be tested by
   * this precondition.
   * 
   * @return the name of the FS index, <code>null</code> if the default annotation index should be
   *         used
   */
  public String getFsIndexName();

  /**
   * Sets the name of the FeatureStructure index containing FeatureStructures to be tested by this
   * precondition.
   * 
   * @param aIndexName
   *          the name of the FS index, <code>null</code> if the default annotation index should
   *          be used
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this <code>MetaDataObject</code> is not modifiable.
   */
  public void setFsIndexName(String aIndexName);

  /**
   * Retrieves the FSMatchConstraint that determines which CAS feature structures will be tested by
   * this precondition.
   * 
   * @return the FS match constraint, <code>null</code> if none
   */
  public FSMatchConstraint getFsMatchConstraint();

  /**
   * Sets the FSMatchConstraint that determines which CAS feature structures will be tested by this
   * precondition.
   * 
   * @param aConstraint
   *          the FS match constraint, <code>null</code> if none
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this <code>MetaDataObject</code> is not modifiable.
   */
  public void setFsMatchConstraint(FSMatchConstraint aConstraint);

  /**
   * Gets the name of the feature to be tested.
   * 
   * @return the feature name, <code>null</code> if none
   */
  public String getFeatureName();

  /**
   * Sets the name of the feature to be tested.
   * 
   * @param aFeatureName
   *          the feature name, <code>null</code> if none
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this <code>MetaDataObject</code> is not modifiable.
   */
  public void setFeatureName(String aFeatureName);

  /**
   * Retrieves the literal value to which features' values will be compared.
   * 
   * @return the value, which must be a String, Integer, Float, Boolean or an array of one of those
   *         four types.
   */
  public Object getComparisonValue();

  /**
   * Sets the literal value to which features' values will be compared.
   * 
   * @param aValue
   *          the comparison value, which must be a String, Integer, Float, Boolean, or an array of
   *          one of those four types.
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this <code>MetaDataObject</code> is not modifiable.
   */
  public void setComparisonValue(Object aValue);

  /**
   * Retrieves the predicate used to compare the test value to the comparison value.
   * 
   * @return a String that identifies the predicate used. This will always match one of the
   *         constants defined on this interface.
   */
  public String getPredicate();

  /**
   * Sets the predicate used to compare the test value to the comparison value.
   * 
   * @param aPredicate
   *          a String that identifies the predicate used. This must match one of the constants
   *          defined on this interface.
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this <code>MetaDataObject</code> is not modifiable.
   * @throws UIMA_IllegalArgumentException
   *           if the given value is not appropriate for the given attribute.
   */
  public void setPredicate(String aPredicate);

  /**
   * Retrieves the default value for this precondition. This is the value returned if there is no
   * applicable test value in the CAS.
   * 
   * @return the default value
   */
  public boolean getDefault();

  /**
   * Sets the default value for this precondition. This is the value returned if there is no
   * applicable test value in the CAS.
   * 
   * @param aDefault
   *          the default value
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this <code>MetaDataObject</code> is not modifiable.
   */
  public void setDefault(boolean aDefault);

  /**
   * The type identifier for this class of precondition. This is the return value of
   * {@link #getPreconditionType()}.
   */
  public static final String PRECONDITION_TYPE = "SIMPLE";

  /**
   * Identifies the EQUAL predicate. This predicate evaluates to true if and only if the test value
   * and the comparison value are of the same data type and are equal, according to the equality
   * rules of that data type.
   */
  public static final String EQUAL = "Equal";

  /**
   * Identifies the ELEMENT_OF predicate. For this predicate, the comparison value must be an array.
   * ELEMENT_OF evaluates to true if and only if the test value is equal to one of the elements of
   * the array. (If the comparison value is not an array, ELEMENT_OF always evaluates to false.)
   */
  public static final String ELEMENT_OF = "ElementOf";

  /**
   * Identifies the LANGUAGE_SUBSUMED predicate. For this predicate, the comparison value must be an
   * array of ISO language identifiers. LANGUAGE_SUBSUMED evaluates to true if and only if the test
   * value is an ISO language identifier that is subsumed by one of the values of the comparison
   * array. (For example, "en_US" is subsumed by "en.")
   */
  public static final String LANGUAGE_SUBSUMED = "LanguageSubsumed";
}
