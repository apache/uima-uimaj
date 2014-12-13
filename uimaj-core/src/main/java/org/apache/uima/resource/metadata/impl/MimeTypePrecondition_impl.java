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
import org.apache.uima.UIMA_UnsupportedOperationException;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.resource.metadata.MimeTypePrecondition;

/**
 * Precondition that tests the MIME type of the Entity's content.
 * 
 * 
 */
public class MimeTypePrecondition_impl extends SimplePrecondition_impl implements
        MimeTypePrecondition {
  private static final long serialVersionUID = -2496834003359218342L;

  public MimeTypePrecondition_impl() {
    super.setFeatureName("MimeType");
    super.setPredicate(ELEMENT_OF);
  }

  /**
   * @see org.apache.uima.resource.metadata.MimeTypePrecondition#getMimeTypes()
   */
  public String[] getMimeTypes() {
    return (String[]) getComparisonValue();
  }

  /**
   * @see org.apache.uima.resource.metadata.MimeTypePrecondition#setMimeTypes(java.lang.String[])
   */
  public void setMimeTypes(String[] aMimeTypes) {
    setComparisonValue(aMimeTypes);
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
    super.setComparisonValue(aValue);
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
   * This method throws UNSUPPORTED_METHOD
   * @param aKey -
   * @throws UIMA_UnsupportedOperationException - 
   */
  public void setMetaDataKeyName(String aKey) {
    throw new UIMA_UnsupportedOperationException(
            UIMA_UnsupportedOperationException.UNSUPPORTED_METHOD, new Object[] {
                this.getClass().getName(), "setMetaDataKeyName" });
  }

  /**
   * @see org.apache.uima.resource.metadata.SimplePrecondition#setPredicate(java.lang.String)
   */
  public void setPredicate(String aPredicate) {
    throw new UIMA_UnsupportedOperationException(
            UIMA_UnsupportedOperationException.UNSUPPORTED_METHOD, new Object[] {
                this.getClass().getName(), "setPredicate" });
  }

}
