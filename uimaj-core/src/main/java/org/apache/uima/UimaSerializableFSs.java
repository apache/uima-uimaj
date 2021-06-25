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
package org.apache.uima;

/**
 * This interface is implemented by JCas classes that need to be called by the framework when a
 * serialization is about to happen where the _save_to_cas_data() method update Features which have
 * references to Feature Structures
 */
public interface UimaSerializableFSs extends UimaSerializable {

  /**
   * This method is called by the framework when the framework needs to locate referenced FSs. The
   * implementation should save FS references into normal Features of this JCas class
   */
  default void _save_fsRefs_to_cas_data() {
    _save_to_cas_data();
  }
}
