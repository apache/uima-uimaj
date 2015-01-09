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
package org.apache.uima.cas.impl;

import java.util.IdentityHashMap;

import org.apache.uima.internal.util.PositiveIntSet;
import org.apache.uima.internal.util.PositiveIntSet_impl;



  /**
   * for XCAS and XMI deserialization, need to remember
   * what's being added to the indexes and/or removed, because
   * the actual FSs are not yet "fixed up" (adjusted for 
   * reference id's &rarr; actual addresses, including the sofa refs)
   * for non-delta updates.  
   * 
   * Workaround (2014) is to remember the information, and do the
   * adds / removes after the fixups.
   * 
   * The information to be remembered is:
   *   1) the View reference (a ref to the FSIndexRepository
   *       a) for each of these, the list of FSaddrs to be added or removed
   *       
   * The list of FSaddrs ought to be a set with no duplicates, but because
   * it could be sourced from a hand-edited source, we cannot depend on that
   * so we store the list as a "set" to prevent duplicates.
   * 
   * The remove operation only removes 1 instance (in case multiple instances
   * of the same FS are in the indexes). 
   * 
   * Currently only used by XMI deserialization
   *
   * Constructor - done by caller - constructs IdentityHashMap
   */
  
@SuppressWarnings("serial")  
class DeferredIndexUpdates extends IdentityHashMap<FSIndexRepositoryImpl, PositiveIntSet> {
    
  void addTodo(FSIndexRepositoryImpl ir, int fsAddr) {
    getTodos(ir).add(fsAddr);
  }
  
  /**
   * Does just-in-time creation of PositiveIntSet if needed before adding
   * @param ir
   * @param fsAddr
   */
  PositiveIntSet getTodos(FSIndexRepositoryImpl ir) {
    PositiveIntSet s = get(ir);
    if (null == s) {
      put(ir, s = new PositiveIntSet_impl());
    }
    return s;
  }
}