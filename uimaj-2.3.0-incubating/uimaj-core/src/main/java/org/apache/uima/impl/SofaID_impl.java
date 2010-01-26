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

package org.apache.uima.impl;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.SofaID;

/**
 * Implementation of {@link SofaID}.
 * 
 * @deprecated
 */
@Deprecated
public class SofaID_impl implements SofaID {

  String mSofaID;

  String mComponentSofaName;

  /**
   * Creates an empty Sofa ID.
   */
  public SofaID_impl() {
  }

  /**
   * Creates a new Sofa ID from the given String. Note that this constructor should be used only by
   * applications. Analysis components should use the {@link UimaContext#mapToSofaID(String)} method
   * instead.
   * 
   * @param aID
   *          the String identifier of the Sofa
   */
  public SofaID_impl(String aID) {
    mSofaID = aID;
    mComponentSofaName = aID;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.SofaID#setAbsoluteSofaName(java.lang.String)
   */
  public void setSofaID(String aSofaID) {
    mSofaID = aSofaID;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.SofaID#getAbsoluteSofaName()
   */
  public String getSofaID() {
    return mSofaID;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.SofaID#setComponentSofaName(java.lang.String)
   */
  public void setComponentSofaName(String aSofaName) {
    mComponentSofaName = aSofaName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.SofaID#getComponentSofaName()
   */
  public String getComponentSofaName() {
    return mComponentSofaName;
  }
}
