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

package org.apache.uima.collection.base_cpm;

import java.io.IOException;

import org.apache.uima.cas_data.CasData;
import org.apache.uima.collection.CollectionException;

/**
 * A <code>CasDataCollectionReader</code> is used to iterate over the elements of a Collection.
 * Iteration is done using the {@link #hasNext()} and {@link #getNext()} methods. Each element of
 * the collection is returned as a {@link CasData}.
 * 
 * 
 */
public interface CasDataCollectionReader extends BaseCollectionReader {
  /**
   * Gets the next <code>CasData</code> from this <code>CollectionReader</code>. If this is a
   * consuming Collection Reader (see {@link #isConsuming()}), this element will also be removed
   * from the collection.
   * 
   * @return the next <code>Entity</code>
   * 
   * @throws org.apache.uima.UIMA_IllegalStateException
   *           if there are no more elements left in the collection
   * @throws IOException
   *           if an I/O failure occurs
   * @throws CollectionException
   *           if there is some other problem with reading from the Collection
   */
  public CasData getNext() throws IOException, CollectionException;

  /**
   * Gets multiple <code>CasData</code> objects from this <code>CasDataCollectionReader</code>.
   * If this is a consuming Collection Reader (see {@link #isConsuming()}), these entities will
   * also be removed from the collection.
   * 
   * @param aNumToGet
   *          the number of <code>CasData</code> objects to get
   * 
   * @return an array containing the <code>CasData</code> objects. The length of this array will
   *         be at most <code>aNumToGet</code>, although it may be less (if there are not enough
   *         elements left in the collection).
   * 
   * @throws org.apache.uima.UIMA_IllegalStateException
   *           if there is no more elements in the collection
   * @throws IOException
   *           if an I/O failure occurs
   * @throws CollectionException
   *           if there is some other problem with reading from the Collection
   */
  public CasData[] getNext(int aNumToGet) throws IOException, CollectionException;

  /**
   * Gets the CAS Data Initializer that has been assigned to this Collection Reader. Note that
   * CollectionReader implementations are not required to make use of the CAS Initializer - refer to
   * the documentation for your specific Collection Reader.
   * 
   * @return the CAS Data Initializer for this Collection Reader
   * 
   * @deprecated As of v2.0 CAS Initializers are deprecated.
   */
  @Deprecated
  public CasDataInitializer getCasDataInitializer();

  /**
   * Assigns a CAS Data Initializer for this Collection Reader to use. Note that* CollectionReader
   * implementations are not required to make use of the CAS Initializer - refer to the
   * documentation for your specific Collection Reader.
   * 
   * @param aCasDataInitializer
   *          the CAS Data Initializer for this Collection Reader
   * 
   * @deprecated As of v2.0 CAS Initializers are deprecated.
   */
  @Deprecated
  public void setCasInitializer(CasDataInitializer aCasDataInitializer);

}
