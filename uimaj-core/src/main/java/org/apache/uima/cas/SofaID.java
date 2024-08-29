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
 * Interface that contains the handle to a Sofa FS in the CAS.
 * 
 * The SofaID is obtained from the UimaContext by calling
 * {@link org.apache.uima.UimaContext#mapToSofaID UimaContext.mapToSofaID()}.
 * 
 * @deprecated As of v2.0, this is no longer needed. CAS views can now be obtained directly using
 *             String identifiers, through the method {@link CAS#getView(String)}.
 * @forRemoval 4.0.0
 */
@Deprecated(since = "2.0.0")
public interface SofaID {
  /**
   * Set the Sofa ID of a Sofa FS in the CAS.
   * 
   * @param aSofaID
   *          -
   */
  void setSofaID(String aSofaID);

  /**
   * Get the Sofa ID.
   * 
   * @return Sofa ID
   */
  String getSofaID();

  /**
   * Set the component Sofa name that was used to obtain this SofaID by calling
   * {@link org.apache.uima.UimaContext#mapToSofaID UimaContext.mapToSofaID()}.
   * 
   * @param aSofaName
   *          -
   */
  void setComponentSofaName(String aSofaName);

  /**
   * Get the component Sofa name that was used to obtain this SofaID by calling {
   * 
   * @see org.apache.uima.UimaContext#mapToSofaID UimaContext.mapToSofaID()}.
   * @return Component Sofa Name
   */
  String getComponentSofaName();
}
