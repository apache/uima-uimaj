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

import org.apache.uima.cas.FeatureStructure;

/**
 * For backwards compatibility with Version 2 - unused in v3
 * V3 has a functional interface equivalent, called FsGenerator3, 
 * with a V3 style signature for createFS
 * @param <T> - 
 * @deprecated unused in v3, only present to avoid compile errors in unused v2 classes
 */
@Deprecated
public interface FSGenerator<T extends FeatureStructure> {

  T createFS(int addr, CASImpl cas);

}
