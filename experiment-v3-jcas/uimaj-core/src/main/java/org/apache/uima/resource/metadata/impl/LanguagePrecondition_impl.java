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

import java.util.Arrays;

import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.UIMA_UnsupportedOperationException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.ConstraintFactory;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.FSTypeConstraint;
import org.apache.uima.cas.text.Language;
import org.apache.uima.resource.metadata.LanguagePrecondition;

/**
 * Precondition that tests language of the document.
 * 
 * 
 */
public class LanguagePrecondition_impl extends SimplePrecondition_impl implements
        LanguagePrecondition {

  private static final long serialVersionUID = -5526826405334750929L;

  public LanguagePrecondition_impl() {
    FSTypeConstraint typeCon = ConstraintFactory.instance().createTypeConstraint();
    typeCon.add("uima.tcas.DocumentAnnotation");
    super.setFsMatchConstraint(typeCon);
    super.setFeatureName(CAS.FEATURE_BASE_NAME_LANGUAGE);
    super.setPredicate(LANGUAGE_SUBSUMED);
  }

  /**
   * @see org.apache.uima.resource.metadata.LanguagePrecondition#getLanguages()
   */
  public String[] getLanguages() {
    return (String[]) getComparisonValue();
  }

  /**
   * @see org.apache.uima.resource.metadata.LanguagePrecondition#setLanguages(java.lang.String[])
   */
  public void setLanguages(String[] aLanguages) {
    setComparisonValue(aLanguages);
  }

  /**
   * @see org.apache.uima.resource.metadata.SimplePrecondition#setComparisonValue(java.lang.Object)
   */
  public void setComparisonValue(Object aValue) {
    // value must be a string array
    if (!(aValue instanceof String[])) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { aValue, "aValue", "setComparisonValue" });
    }
    String [] languages = (String []) aValue;
    String [] normalizedLanguages = new String[languages.length];
    int i = 0;
    for (String language : languages) {
      normalizedLanguages[i++] = Language.normalize(language);
      if (Language.UNSPECIFIED_LANGUAGE.equals(normalizedLanguages[i-1])) {
        // return new object to guard against modifications
        super.setComparisonValue(new String[]{Language.UNSPECIFIED_LANGUAGE});
        return;
      }
    }
    super.setComparisonValue(normalizedLanguages);
  }

  /**
   * @see org.apache.uima.resource.metadata.SimplePrecondition#setFeatureName(java.lang.String)
   */
  public void setFeatureName(String aFeatureName) {
    throw new UIMA_UnsupportedOperationException(
            UIMA_UnsupportedOperationException.UNSUPPORTED_METHOD, new Object[] {
                this.getClass().getName(), "setFeatureName" });
  }

  /**
   * @see org.apache.uima.resource.metadata.SimplePrecondition#setFsIndexName(java.lang.String)
   */
  public void setFsIndexName(String aIndexName) {
    throw new UIMA_UnsupportedOperationException(
            UIMA_UnsupportedOperationException.UNSUPPORTED_METHOD, new Object[] {
                this.getClass().getName(), "setFsIndexName" });
  }

  /**
   * @see org.apache.uima.resource.metadata.SimplePrecondition#setFsMatchConstraint(org.apache.uima.cas.FSMatchConstraint)
   */
  public void setFsMatchConstraint(FSMatchConstraint aConstraint) {
    throw new UIMA_UnsupportedOperationException(
            UIMA_UnsupportedOperationException.UNSUPPORTED_METHOD, new Object[] {
                this.getClass().getName(), "setFsMatchConstraint" });
  }

  /**
   * @see org.apache.uima.resource.metadata.SimplePrecondition#setPredicate(java.lang.String)
   */
  public void setPredicate(String aPredicate) {
    throw new UIMA_UnsupportedOperationException(
            UIMA_UnsupportedOperationException.UNSUPPORTED_METHOD, new Object[] {
                this.getClass().getName(), "setPredicate" });
  }

  public boolean equals(Object aObject) {
    if (!(aObject instanceof LanguagePrecondition))
      return false;
    LanguagePrecondition p = (LanguagePrecondition) aObject;
    Object cv1 = getComparisonValue();
    Object cv2 = p.getComparisonValue();
    if (cv1.equals(cv2))
      return true;
    if (cv1 instanceof Object[]) {
      if (!(cv2 instanceof Object[]))
        return false;
      if (!Arrays.equals((Object[]) cv1, (Object[]) cv2))
        return false;
    }
    return true;
  }

  public int hashCode() {
    int h = getFeatureName().hashCode();
    if (getComparisonValue() != null)
      h += getComparisonValue().hashCode();
    return h;
  }

}
