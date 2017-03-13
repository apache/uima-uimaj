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

import org.apache.uima.cas.CAS;

/**
 * A <code>Precondition</code> of a Resource is a condition that an Entity and/or its analysis in
 * the {@link CAS} must satisfy before that Entity can be processed by the Resource.
 * <p>
 * Currently the framework does not fully support Preconditions. Only the
 * {@link org.apache.uima.resource.metadata.LanguagePrecondition} subinterface is used.
 * <p>
 * As with all {@link MetaDataObject}s, a <code>Precondition</code> may or may not be modifiable.
 * An application can find out by calling the {@link #isModifiable()} method.
 * 
 * 
 */
public interface Precondition extends MetaDataObject {

  /**
   * Gets the type of this precondition. Each sub-interface of <code>Precondition</code> has its
   * own standard type identifier String. These identifier Strings are used instead of Java class
   * names in order to ease portability of metadata to other languages.
   * 
   * @return the type identifier String for this precondition
   */
  public String getPreconditionType();

  /**
   * Determines if this precondition is satisfied by a CAS.
   * 
   * @param aCAS
   *          the CAS against which to evaluate this precondition
   * 
   * @return true if and only if the CAS satisfies this precondition.
   */
  public boolean evaluate(CAS aCAS);
}
