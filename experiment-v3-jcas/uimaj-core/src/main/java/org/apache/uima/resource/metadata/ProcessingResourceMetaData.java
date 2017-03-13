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

package org.apache.uima.resource.metadata;

import org.apache.uima.UIMA_UnsupportedOperationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.InvalidXMLException;

/**
 * Metadata that describes a "Processing" {@link org.apache.uima.resource.Resource} - that is, one
 * that reads or writes to the {@link org.apache.uima.cas.CAS}.
 * <p>
 * As with all {@link MetaDataObject}s, a <code>ProcessingResourceMetaData</code> may or may not
 * be modifiable. An application can find out by calling the {@link #isModifiable()} method.
 * 
 * 
 */
public interface ProcessingResourceMetaData extends ResourceMetaData {

  /**
   * Retrieves the Type System used by this Processing Resource. The Type System contains
   * {@link TypeDescription}s and {@link FeatureDescription}s that are the inputs and/or outputs
   * of this Resource.  Some Processing Resources, such as aggregate analysis engines, may not
   * contain a type system and return <code>null</code>.
   * 
   * @return a description of the type system used by this Resource
   */
  public TypeSystemDescription getTypeSystem();

  /**
   * Retrieves the Type System used by this Processing Resource. The Type System contains
   * {@link TypeDescription}s and {@link FeatureDescription}s that are the inputs and/or outputs
   * of this Resource.
   * 
   * @param aTypeSystem
   *          a description of the type system used by this Resource.
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this <code>MetaDataObject</code> is not modifiable.
   */
  public void setTypeSystem(TypeSystemDescription aTypeSystem);

  /**
   * Retrieves the Type Priorites for this Processing Resource. Type Priorities may be used to
   * determine the sort order of CAS indexes - see {@link #getFsIndexes()}.
   * 
   * @return the Type Priorities for Resource
   */
  public TypePriorities getTypePriorities();

  /**
   * Retrieves the Type Priorites for this Processing Resource. Type Priorities may be used to
   * determine the sort order of CAS indexes - see {@link #getFsIndexes()}.
   * 
   * @param aTypePriorities
   *          the Type Priorities for this Resource
   */
  public void setTypePriorities(TypePriorities aTypePriorities);

  /**
   * Retrieves the FS Index collection used by this Processing Resource. FS Indexes are used to
   * iterate over annotations in the {@link org.apache.uima.cas.CAS}.
   * 
   * @return a description of the Feature Structure indexes used by this Resource.
   */
  public FsIndexCollection getFsIndexCollection();

  /**
   * Sets the Feature Structure Index collection used by this Processing Resource. FS Indexes are
   * used to iterate over annotations in the {@link org.apache.uima.cas.CAS}.
   * 
   * @param aFsIndexCollection
   *          a description of the Feature Structure indexes used by this Resource.
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this <code>MetaDataObject</code> is not modifiable.
   */
  public void setFsIndexCollection(FsIndexCollection aFsIndexCollection);

  /**
   * Retrieves the Feature Structure indexes by this Processing Resource. These are used to iterate
   * over annotations in the {@link org.apache.uima.cas.CAS}.
   * <p>
   * NOTE: this method predates the {@link FsIndexCollection} object, which may define additional
   * information (name, description, vendor, version) and import other FsIndexCollections, and
   * provides direct access to the {@link FsIndexDescription} objects. To access the
   * {@link FsIndexCollection} object, call {@link #getFsIndexCollection()}.
   * 
   * @return a description of the Feature Structure indexes used by this Resource.
   */
  public FsIndexDescription[] getFsIndexes();

  /**
   * Sets the Feature Structure indexes by this Processing Resource. These are used to iterate over
   * annotations in the {@link org.apache.uima.cas.CAS}.
   * <p>
   * NOTE: this method predates the {@link FsIndexCollection} object, which may define additional
   * information (name, description, vendor, version) and import other FsIndexCollections, and
   * provides direct access to the {@link FsIndexDescription} objects. To access the
   * {@link FsIndexCollection} object, call {@link #getFsIndexCollection()}.
   * 
   * @param aFSIndexes
   *          a description of the Feature Structure indexes used by this Resource.
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this <code>MetaDataObject</code> is not modifiable.
   */
  public void setFsIndexes(FsIndexDescription[] aFSIndexes);

  /**
   * Retrieves this Processing Resource's {@link Capability Capabilities}. Each capability consists
   * of a set of features or types that this Resource inputs and outputs, along with the
   * preconditions (e.g. language or mime type) on the input Entity.
   * 
   * @return an array of <code>Capabilities</code>.
   */
  public Capability[] getCapabilities();

  /**
   * Sets this Processing Resource's {@link Capability Capabilities}. Each capability consists of a
   * set of features or types that this Resource inputs and outputs, along with the preconditions
   * (e.g. language or mime type) on the input Entity.
   * 
   * @param aCapabilities
   *          an array of <code>Capabilities</code>.
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this <code>MetaDataObject</code> is not modifiable.
   */
  public void setCapabilities(Capability[] aCapabilities);

  /**
   * Gets this Processing Resource's {@link OperationalProperties}. This includes information such
   * as whether this component will modify the CAS, and whether multiple instances of this component
   * can be run in parallel.
   * 
   * @return operational properties for this component
   */
  public OperationalProperties getOperationalProperties();

  /**
   * Sets this Processing Resource's {@link OperationalProperties}. This includes information such
   * as whether this component will modify the CAS, and whether multiple instances of this component
   * can be run in parallel.
   * 
   * @param aOperationalProperties
   *          operational properties for this component
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this <code>MetaDataObject</code> is not modifiable.
   */
  public void setOperationalProperties(OperationalProperties aOperationalProperties);

  /**
   * Resolves any import declarations. This includes imports of type systems, type priorities, and
   * FS index collections. The imported types, type priorities, and FS index collections are added
   * directly onto their respective lists, and the import elements are deleted, so this results in a
   * structure that is equivalent to the imported elements having been defined locally.
   * 
   * @throws InvalidXMLException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports() throws InvalidXMLException;

  /**
   * Resolves any import declarations. This includes imports of type systems, type priorities, and
   * FS index collections. The imported types, type priorities, and FS index collections are added
   * directly onto their respective lists, and the import elements are deleted, so this results in a
   * structure that is equivalent to the imported elements having been defined locally.
   * 
   * @param aResourceManager
   *          the Resource Manager used to locate descriptors imported by name. For example, the
   *          path in which to locate these descriptors can be set via the
   *          {@link ResourceManager#setDataPath(String)} method.
   * 
   * @throws InvalidXMLException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports(ResourceManager aResourceManager) throws InvalidXMLException;

  /**
   * Gets whether this AE is sofa-aware. This is a derived property that cannot be set directly. An
   * AE is sofa-aware if and only if it declares at least one input sofa or output sofa.
   * 
   * @return true if this component is sofa-aware, false if it is sofa-unaware.
   */
  public boolean isSofaAware();
}
