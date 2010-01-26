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

package org.apache.uima.analysis_engine;

import org.apache.uima.resource.metadata.MetaDataObject;

/**
 * A reference to a Type or Feature in the Analysis Engine's TypeSystem. This interface is used by
 * {@link org.apache.uima.resource.metadata.Capability} to declare which Types and Features the
 * Analysis Engine inputs and outputs, and it is also used by {@link ResultSpecification} to declare
 * the outputs that are desired by the application.
 * 
 * 
 */
public interface TypeOrFeature extends MetaDataObject, Comparable<TypeOrFeature> {

  /**
   * Gets whether this object refers to a Type or to a Feature.
   * 
   * @return true if Type, false if Feature
   */
  public boolean isType();

  /**
   * Sets whether this object refers to a Type or to a Feature.
   * 
   * @param aType
   *          true if Type, false if Feature
   */
  public void setType(boolean aType);

  /**
   * Gets the fully-qualified Type or Feature name.
   * 
   * @return the fully-qualified name
   */
  public String getName();

  /**
   * Sets the fully-qualified Type or Feature name.
   * 
   * @param aName
   *          the fully-qualified name
   */
  public void setName(String aName);

  /**
   * For Type references, this method determines whether this reference should be considered to also
   * refer to all features of the Type that are known to the annotator. This field is not used for
   * Feature references.
   * 
   * @return true if this is a reference to all features, false if it is only a reference to the
   *         type
   */
  public boolean isAllAnnotatorFeatures();

  /**
   * For Type references, sets whether this reference should be considered to also refer to all
   * features of the Type that are known to the annotator. This field is not used for Feature
   * references.
   * 
   * @param aAllAnnotatorFeatures
   *          true if this is a reference to all features, false if it is only a reference to the
   *          type
   */
  public void setAllAnnotatorFeatures(boolean aAllAnnotatorFeatures);
}
