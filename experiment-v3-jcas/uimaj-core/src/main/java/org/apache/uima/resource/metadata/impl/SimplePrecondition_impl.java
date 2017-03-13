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

package org.apache.uima.resource.metadata.impl;

import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.resource.metadata.Precondition;
import org.apache.uima.resource.metadata.SimplePrecondition;

/**
 * Reference implementation of {@link SimplePrecondition}.
 * 
 * 
 */
public class SimplePrecondition_impl extends MetaDataObject_impl implements SimplePrecondition {

  static final long serialVersionUID = -3736364411654445630L;

  /**
   * Value to compare against the feature.
   */
  private Object mComparisonValue;

  /**
   * Predicate used to compare.
   */
  private String mPredicate;

  private FSMatchConstraint mFsMatchConstraint;

  private String mFsIndexName;

  private String mFeatureName;

  private boolean mDefault;

  /**
   * @see Precondition#getPreconditionType()
   */
  public String getPreconditionType() {
    return PRECONDITION_TYPE;
  }

  /**
   * @see org.apache.uima.resource.metadata.SimplePrecondition#getDefault()
   */
  public boolean getDefault() {
    return mDefault;
  }

  /**
   * @see org.apache.uima.resource.metadata.SimplePrecondition#getFeatureName()
   */
  public String getFeatureName() {
    return mFeatureName;
  }

  /**
   * @see org.apache.uima.resource.metadata.SimplePrecondition#getFsIndexName()
   */
  public String getFsIndexName() {
    return mFsIndexName;
  }

  /**
   * @see org.apache.uima.resource.metadata.SimplePrecondition#getFsMatchConstraint()
   */
  public FSMatchConstraint getFsMatchConstraint() {
    return mFsMatchConstraint;
  }

  /**
   * @see org.apache.uima.resource.metadata.SimplePrecondition#setDefault(boolean)
   */
  public void setDefault(boolean aDefault) {
    mDefault = aDefault;
  }

  /**
   * @see org.apache.uima.resource.metadata.SimplePrecondition#setFeatureName(java.lang.String)
   */
  public void setFeatureName(String aFeatureName) {
    mFeatureName = aFeatureName;
  }

  /**
   * @see org.apache.uima.resource.metadata.SimplePrecondition#setFsIndexName(java.lang.String)
   */
  public void setFsIndexName(String aIndexName) {
    mFsIndexName = aIndexName;
  }

  /**
   * @see org.apache.uima.resource.metadata.SimplePrecondition#setFsMatchConstraint(org.apache.uima.cas.FSMatchConstraint)
   */
  public void setFsMatchConstraint(FSMatchConstraint aConstraint) {
    mFsMatchConstraint = aConstraint;

  }

  /**
   * @see org.apache.uima.resource.metadata.Precondition#evaluate(org.apache.uima.cas.CAS)
   */
  public boolean evaluate(CAS aCAS) {
    return false;
  }

  /**
   * @see SimplePrecondition#getComparisonValue()
   */
  public Object getComparisonValue() {
    return mComparisonValue;
  }

  /**
   * @see SimplePrecondition#setComparisonValue(Object)
   */
  public void setComparisonValue(Object aValue) {
    mComparisonValue = aValue;
  }

  /**
   * @see SimplePrecondition#getPredicate()
   */
  public String getPredicate() {
    return mPredicate;
  }

  /**
   * @see SimplePrecondition#setPredicate(String)
   */
  public void setPredicate(String aPredicate) {
    // check to make sure value is legal
    if (!isValidPredicateName(aPredicate)) {
      throw new UIMA_IllegalArgumentException(
              UIMA_IllegalArgumentException.METADATA_ATTRIBUTE_TYPE_MISMATCH, new Object[] {
                  aPredicate, "predicate" });
    }
    mPredicate = aPredicate;
  }

  /**
   * Determines whether the given String is a valid name a predicate defined by this class. Valid
   * predicate names are legal arguments to the {@link #setPredicate(String)} method, and are defined
   * by constants on the {@link SimplePrecondition} interface.
   * 
   * @param aName
   *          an Object to test
   * 
   * @return true if and only if <code>aName</code> is a <code>String</code> that is a valid
   *         predicate name.
   */
  protected static boolean isValidPredicateName(Object aName) {
    return EQUAL.equals(aName) || ELEMENT_OF.equals(aName) || LANGUAGE_SUBSUMED.equals(aName);
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo(
          "simplePrecondition",
          new PropertyXmlInfo[] { new PropertyXmlInfo("featureDescription", null),
              new PropertyXmlInfo("comparisonValue"), new PropertyXmlInfo("predicate", "predicate") });
}
