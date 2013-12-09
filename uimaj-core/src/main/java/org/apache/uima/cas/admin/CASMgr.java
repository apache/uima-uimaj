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

package org.apache.uima.cas.admin;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;

/**
 * Class comment for CASMgr.java goes here.
 * 
 * 
 */
public interface CASMgr {

  /**
   * Return a writable version of the type system. This may be used to add new types and features.
   * 
   * @return A writable version of the type system.
   */
  TypeSystemMgr getTypeSystemMgr();

  /**
   * @return a writable version of the index repository. Note that the type system must be committed
   * before this method can be called.
   * @exception CASAdminException
   *              If the type system has not been committed.
   */
  FSIndexRepositoryMgr getIndexRepositoryMgr() throws CASAdminException;

  /**
   * Flush this CAS instance of all transient data. This will delete all feature structures, but not
   * the type system, the indexes etc. Call before processing a new document.
   * 
   * @deprecated Use {@link #reset reset()} instead.
   */
  @Deprecated
  void flush() throws CASAdminException;

  /**
   * Flush this CAS instance of all transient data. This will delete all feature structures, but not
   * the type system, the indexes etc. Call before processing a new document.
   */
  void reset() throws CASAdminException;

  /**
   * Return a non-admin version of the CAS.
   * 
   * @return The CAS corresponding to this CASMgr.
   * @exception CASAdminException
   *              If the index repository has not been committed.
   */
  CAS getCAS() throws CASAdminException;

  /**
   * Enable/disable resetting the CAS with {@link CAS#reset CAS.reset()}.
   * 
   * @param flag true to enable reset
   */
  void enableReset(boolean flag);

  void setCAS(CAS cas);

  /**
   * Install the standard built-in indexes into the base CAS
   * @throws CASException if an error occurs
   */
  void initCASIndexes() throws CASException;

  /**
   * Gets the ClassLoader that should be used by the JCas to load the generated FS cover classes for
   * this CAS.
   * 
   * @return the JCas ClassLoder for this CAS
   */
  ClassLoader getJCasClassLoader();

  /**
   * Sets the ClassLoader that should be used by the JCas to load the generated FS cover classes for
   * this CAS.
   * 
   * @param classLoader
   *          the JCas ClassLoder for this CAS
   */
  void setJCasClassLoader(ClassLoader classLoader);

}
