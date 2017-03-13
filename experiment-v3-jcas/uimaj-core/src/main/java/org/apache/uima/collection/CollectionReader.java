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

package org.apache.uima.collection;

import java.io.IOException;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.collection.base_cpm.BaseCollectionReader;
import org.apache.uima.resource.ConfigurableResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;

/**
 * A <code>CollectionReader</code> is used to iterate over the elements of a Collection. Iteration
 * is done using the {@link #hasNext()} and {@link #getNext(CAS)} methods. Each element of the
 * collection is returned in a {@link CAS}.
 * <p>
 * A <i>consuming</i> <code>CollectionReader</code> is one that removes each element from the
 * collection as it is read. To find out whether a <code>CollectionReader</code> will consume
 * elements in this way, call the {@link #isConsuming()} method.
 * <p>
 * Users of a <code>CollectionReader</code> should always {@link #close() close} it when they are
 * finished using it.
 * <p>
 * <code>CollectionReader</code>s are also {@link ConfigurableResource}s, and can be
 * instantiated from descriptors. See
 * {@link org.apache.uima.util.XMLParser#parseCollectionReaderDescription(XMLInputSource)} and
 * {@link org.apache.uima.UIMAFramework#produceCollectionReader(ResourceSpecifier,Map)} for more
 * information.
 * 
 * 
 */
public interface CollectionReader extends BaseCollectionReader, ConfigurableResource {
  /**
   * Informs this CollectionReader that the CAS TypeSystem has changed. The CPM calls this method
   * immediately following the call to {@link #initialize(ResourceSpecifier,Map)}, and will call it
   * again whenever the CAS TypeSystem changes.
   * <p>
   * In this method, the CollectionReader should use the {@link TypeSystem} to resolve the names of
   * Type and Features to the actual {@link org.apache.uima.cas.Type} and
   * {@link org.apache.uima.cas.Feature} objects, which can then be used during processing.
   * 
   * @param aTypeSystem
   *          the CAS TypeSystem
   * 
   * @throws ResourceInitializationException
   *           if the type system is not compatible with this Collection Reader
   */
  public void typeSystemInit(TypeSystem aTypeSystem) throws ResourceInitializationException;

  /**
   * Gets the next element of the collection. The element will be stored in the provided CAS object.
   * If this is a consuming <code>CollectionReader</code> (see {@link #isConsuming()}), this
   * element will also be removed from the collection.
   * 
   * @param aCAS
   *          the CAS to populate with the next element of the collection
   * 
   * @throws org.apache.uima.UIMA_IllegalStateException
   *           if there are no more elements left in the collection
   * @throws IOException
   *           if an I/O failure occurs
   * @throws CollectionException
   *           if there is some other problem with reading from the Collection
   */
  public void getNext(CAS aCAS) throws IOException, CollectionException;

  /**
   * Gets the CAS Initializer that has been assigned to this Collection Reader. Note that
   * CollectionReader implementations are not required to make use of the CAS Initializer - refer to
   * the documentation for your specific Collection Reader.
   * 
   * @return the CAS Initializer for this Collection Reader
   * 
   * @deprecated As of v2.0 CAS Initializers are deprecated.
   */
  @Deprecated
  public CasInitializer getCasInitializer();

  /**
   * Assigns a CAS Initializer for this Collection Reader to use. Note that CollectionReader
   * implementations are not required to make use of the CAS Initializer - refer to the
   * documentation for your specific Collection Reader.
   * 
   * @param aCasInitializer
   *          the CAS Initializer for this Collection Reader
   * 
   * @deprecated As of v2.0 CAS Initializers are deprecated.
   */
  @Deprecated
  public void setCasInitializer(CasInitializer aCasInitializer);

}
