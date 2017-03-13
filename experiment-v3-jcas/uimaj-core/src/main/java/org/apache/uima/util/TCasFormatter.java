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

package org.apache.uima.util;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSMatchConstraint;

/**
 * Interface for components that generate a String representation of a CAS.
 * 
 * 
 */
public interface TCasFormatter {

  /**
   * Produces a string representation of a CAS.
   * 
   * @param aCAS
   *          the CAS to be formatted
   * 
   * @return a string representation of <code>aCAS</code>.
   * 
   * @exception CASException
   *              if an exception occurs reading from the CAS
   */
  public String format(CAS aCAS) throws CASException;

  /**
   * Produces a string representation of a CAS. Only those feature structures that satisfy the
   * specified filter will appear in the string representation.
   * 
   * @param aCAS
   *          the CAS to be formatted
   * @param aFilter
   *          a constraint which FeatureStructures must satisfy in order to be included in the
   *          resulting string
   * 
   * @return a string representation of <code>aCAS</code>.
   * 
   * @exception CASException
   *              if an exception occurs reading from the CAS
   */
  public String format(CAS aCAS, FSMatchConstraint aFilter) throws CASException;
}
