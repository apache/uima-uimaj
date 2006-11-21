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

/**
 * Interface to compare two feature structures, represented by their addresses.
 * 
 * 
 * @version $Revision: 1.1 $
 */
public interface FSImplComparator {

  /**
   * Compare two FSs.
   * 
   * @param addr1
   *          Address of FS1.
   * @param addr2
   *          Address of FS2.
   * @return <code>-1</code>, if FS1 is "smaller" than FS2; <code>1</code>, if FS2 is smaller
   *         than FS1; and <code>0</code>, if FS1 equals FS2.
   */
  int compare(int addr1, int addr2);

}
