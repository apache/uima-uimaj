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

import java.io.InputStream;

/**
 * Interface for "Subject of Analysis" (Sofa) feature structures. A Sofa is implemented as a
 * built-in CAS type uima.cas.Sofa. The features of the Sofa type include:
 * <ul>
 * <li>SofaID: Every Sofa in a CAS must have a unique SofaID. SoaIDs are the primary handle or
 * access.
 * <li>Mime type: This string feature can be used to describe the type of the data represented by
 * the Sofa.
 * <li>Sofa Data: The data itself. This data can be resident in the CAS or it can be a reference to
 * data outside the CAS.
 * </ul>
 * <p>
 * SofaFS (the feature structure that represents a SofA) are created as a side effect of
 * creating a new CAS view.  To create a new CAS view, use 
 * {@link org.apache.uima.cas.CAS#createView CAS.createView(string-view-name)}. 
 * From the returned CAS view, you can get the associated SofaFS instance, using
 * {@link org.apache.uima.cas.CAS#getSofa CAS.getSofa()}.  
 * The SofaFS interface provides methods to set the values of the features of the Sofa FS. Generic CAS APIs
 * should never be used to create Sofas or set their features.
 * <p>
 * Sofa data can be contained locally in the CAS itself or it can be remote from CAS. To set the
 * local Sofa data in the Sofa FS use:
 * {@link org.apache.uima.cas.SofaFS#setLocalSofaData(FeatureStructure) SofaFS.setLocalSofaData()}. If the data is
 * remote from the CAS use:
 * {@link org.apache.uima.cas.SofaFS#setRemoteSofaURI SofaFS.setRemoteSofaURI()}.
 * <p>
 * Once set, the Sofa data cannot be set again until the CAS has been reset. This is so that
 * annotators cannot change the subject of analysis during processing.
 */
// ** Reserved for future use. */
public interface SofaFS extends FeatureStructure {

  /**
   * Set the URI for a Remote Subject of Analysis. Once set, this URI may not be changed.
   * @param aURI the URI for a remote Sofa 
   * @throws CASRuntimeException
   *           if the Sofa data has already been set
   */
  void setRemoteSofaURI(String aURI) throws CASRuntimeException;

  /**
   * Set the Local Subject of Analysis to be a predefined ArrayFS. Once set, the Sofa data cannot be
   * changed.
   * @param aFS the SofA
   * @throws CASRuntimeException
   *           if given FS is not an ArrayFS, or if the Sofa data has already been set
   */
  void setLocalSofaData(FeatureStructure aFS) throws CASRuntimeException;

  /**
   * Set the Local Subject of Analysis to be a String. Once set, the Sofa data cannot be changed.
   * @param aString  The subject of analysis 
   * @throws CASRuntimeException
   *           if the Sofa data has already been set
   */
  void setLocalSofaData(String aString) throws CASRuntimeException;

  /**
   * Get the Local Subject of Analysis returns null if not previously set.
   * @return the local SofA 
   */
  FeatureStructure getLocalFSData();

  /**
   * Get the Local Subject of Analysis returns null if not previously set.
   * @return the SofA 
   */
  String getLocalStringData();

  /**
   * Get the Sofa mime type.
   * 
   * @return SofA mime type
   */
  String getSofaMime();

  /**
   * Get the Sofa globally unique name, after mapping.
   * 
   * @return Sofa globally unique name, after mapping
   */
  String getSofaID();

  /**
   * Get the Sofa URI value.
   * 
   * @return Sofa URI or null if not valid
   */
  String getSofaURI();

  /**
   * Get the Sofa Ref value.
   * @return the Sofa Reference value
   */
  int getSofaRef();

  /**
   * Provides stream access to both local and remote Sofa data.
   * 
   * For remote SofA data, a custom URLStreamHandler may be registered for a protocol via the
   * java.protocol.handler.pkgs system property.
   * 
   * @return an InputStream for reading Sofa data. null returned if there is no Sofa data.
   */
  InputStream getSofaDataStream();

}
