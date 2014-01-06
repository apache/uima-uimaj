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
 * Represents the "owner" of a CAS. A CAS may have a reference to its owner, which allows the CAS to
 * be released back to the owner when it is no longer needed. A CasOwner may implement a pool of CAS
 * instances.
 */
public interface CasOwner {

  /**
   * Releases a CAS back to its owner. After calling this method, the caller should no longer access
   * <code>aCAS</code>.
   * 
   * @param aCAS the CAS to release
   */
  void releaseCas(AbstractCas aCAS);

}
