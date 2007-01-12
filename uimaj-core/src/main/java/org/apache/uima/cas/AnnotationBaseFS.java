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
 * Annotation Base API.
 * 
 * <p>
 * The <code>AnnotationBase</code> object holds one feature, the Sofa, which is a reference to the
 * SofaFS object associated with subtypes of this type.
 * 
 * <p>
 * It implements the methods to get the CAS view associated with this sofa.
 * 
 */
public interface AnnotationBaseFS extends FeatureStructure {

  /**
   * Gets the CAS view associated with the Sofa that this Annotation is over.
   * 
   * @return the CAS view associated with the Annotation's Sofa
   */
  public CAS getView();

}
