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

import java.util.ArrayList;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;

public abstract class FsIterator_subtypes_list_unused <T extends FeatureStructure>  extends FsIterator_multiple_indexes<T> {
 
  public FsIterator_subtypes_list_unused(FsIndex_iicp<T> iicp) {
    super(iicp.getIterators());
  }
  
  
}
