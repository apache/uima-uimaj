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

import org.apache.uima.UIMA_UnsupportedOperationException;

/**
 * A precondition on the language of the document. This interface is a kind of
 * <code>SimplePrecondition</code> to be used as a convenience.
 * 
 * 
 */
public interface LanguagePrecondition extends SimplePrecondition {
  /**
   * Gets the languages that satisfy this precondition. This will be an array of ISO language
   * identifiers. For this precondition to be satisfied, the document's language must be subsumed by
   * one of these identifiers (for example, en-GB is subsumed by en).
   * <p>
   * Note that if the document's language is unknown, the value of the
   * {@link #setDefault(boolean) default} property determines whether this precondition is
   * satisfied.
   * 
   * @return the languages that satisfy this precondition
   */
  public String[] getLanguages();

  /**
   * Sets the languages that satisfy this precondition. This will be an array of ISO language
   * identifiers. For this precondition to be satisfied, the document's language must be subsumed by
   * one of these identifiers (for example, en-GB is subsumed by en).
   * <p>
   * Note that if the document's language is unknown, the value of the
   * {@link #setDefault(boolean) default} property determines whether this precondition is
   * satisfied.
   * 
   * The ISO language identifiers are cannonicalized by lower-casing them and replacing
   * underscores with hyphens.  Also, if any of the identifiers are x-unspecified, the array
   * is replaced with an array of just one element: x-unspecified, since all languages are defined
   * to match that one.
   * 
   * @param aLanguages
   *          the languages that satisfy this precondition
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this <code>MetaDataObject</code> is not modifiable.
   */
  public void setLanguages(String[] aLanguages);
}
