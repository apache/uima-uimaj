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
 * common APIs supporting the copy on write aspect of index parts
 */
public interface CopyOnWriteIndexPart {
  
  void makeReadOnlyCopy();
  
  /**
   * @param original an instance of ObjectHashSet or OrderedFsSet_sorted
   * @return true if this cow version is the same as the original.
   *              true means the index has not been updated
   */
  boolean isOriginal();
}
