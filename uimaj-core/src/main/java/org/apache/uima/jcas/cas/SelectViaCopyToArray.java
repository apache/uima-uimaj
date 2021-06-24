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

package org.apache.uima.jcas.cas;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SelectFSs;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.SelectFSs_impl;

/**
 * Classes which provide a toArrayForSelect() method that returns a FeatureStructure[] can implement
 * this to enable the class to be used as a "select" source T extends FeatureStructure because
 * FSArray with no typing needs to default to FeatureStructure for backwards compatibility
 * 
 * @param <T>
 *          the type of the element
 */

public interface SelectViaCopyToArray<T extends FeatureStructure> {

  FeatureStructure[] _toArrayForSelect();

  CASImpl _getView();

  /**
   * @return a new instance of SelectFSs
   */
  default SelectFSs_impl<T> select() {
    return new SelectFSs_impl<>(_toArrayForSelect(), this._getView());
  }

  /**
   * Treat an FSArray as a source for SelectFSs.
   * 
   * @param filterByType
   *          only includes elements of this type
   * @param <U>
   *          generic type being selected
   * @return a new instance of SelectFSs
   */
  default <U extends T> SelectFSs<U> select(Type filterByType) {
    return select().type(filterByType);
  }

  /**
   * Treat an FSArray as a source for SelectFSs.
   * 
   * @param filterByType
   *          only includes elements of this JCas class
   * @param <U>
   *          generic type being selected
   * @return a new instance of SelectFSs
   */
  default <U extends T> SelectFSs<U> select(Class<U> filterByType) {
    return select().type(filterByType);
  }

  /**
   * Treat an FSArray as a source for SelectFSs.
   * 
   * @param filterByType
   *          only includes elements of this JCas class's type
   * @param <U>
   *          generic type being selected
   * @return a new instance of SelectFSs
   */
  default <U extends T> SelectFSs<U> select(int filterByType) {
    return select().type(filterByType);
  }

  /**
   * Treat an FSArray as a source for SelectFSs.
   * 
   * @param filterByType
   *          only includes elements of this type (fully qualifined type name)
   * @param <U>
   *          generic type being selected
   * @return a new instance of SelectFSs
   */
  default <U extends T> SelectFSs<U> select(String filterByType) {
    return select().type(filterByType);
  }
}
