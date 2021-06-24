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

import org.apache.uima.cas.impl.FeatureStructureImplC;

/**
 * This interface is implemented by JCas classes that need to be called by the framework when a
 * serialization is about to happen.
 */
public interface UimaSerializable {
  /**
   * This method is called by the framework before serialization of an instance of this JCas class.
   * The implementation should save whatever data is needed into Features of this JCas class that
   * can be serialized by UIMA.
   */
  void _save_to_cas_data();

  void _init_from_cas_data();

  FeatureStructureImplC _superClone();

  default void _reset_cas_data() {
  } // just for efficiency

  default FeatureStructureImplC clone() {
    _save_to_cas_data();
    FeatureStructureImplC r = _superClone();
    _reset_cas_data();
    ((UimaSerializable) r)._init_from_cas_data();
    return r;
  }
}
