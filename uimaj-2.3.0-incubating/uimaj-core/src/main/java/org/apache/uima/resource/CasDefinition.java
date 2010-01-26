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

package org.apache.uima.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;

/**
 * Encapsulates information that defines how to create a CAS.
 */
public class CasDefinition {
  private TypeSystemDescription typeSystemDescription;

  private TypePriorities typePriorities;

  private FsIndexDescription[] fsIndexDescriptions;

  private ResourceManager resourceManager;

  public CasDefinition(TypeSystemDescription aTypeSystem, TypePriorities aTypePriorities,
          FsIndexDescription[] aFsIndexes, ResourceManager aResourceManager,
          Properties aPerformanceTuningSettings) {
    this.typeSystemDescription = aTypeSystem;
    this.typePriorities = aTypePriorities;
    this.fsIndexDescriptions = aFsIndexes;
    this.resourceManager = aResourceManager;
  }

  public CasDefinition(Collection<? extends ProcessingResourceMetaData> aMetaDataToMerge, ResourceManager aResourceManager)
          throws ResourceInitializationException {
    // extract TypeSystems, TypePriorities, and FsIndexes from metadata
    List<TypeSystemDescription> typeSystems = new ArrayList<TypeSystemDescription>();
    List<TypePriorities> typePrioritiesList = new ArrayList<TypePriorities>();
    List<FsIndexCollection> fsIndexes = new ArrayList<FsIndexCollection>();
    Iterator<? extends ProcessingResourceMetaData> it = aMetaDataToMerge.iterator();
    while (it.hasNext()) {
      ProcessingResourceMetaData md = it.next();
      if (md.getTypeSystem() != null)
        typeSystems.add(md.getTypeSystem());
      if (md.getTypePriorities() != null)
        typePrioritiesList.add(md.getTypePriorities());
      if (md.getFsIndexCollection() != null)
        fsIndexes.add(md.getFsIndexCollection());
    }

    // merge TypePriorities and FsIndexes
    TypePriorities aggTypePriorities = CasCreationUtils.mergeTypePriorities(typePrioritiesList,
            aResourceManager);
    FsIndexCollection aggIndexColl = CasCreationUtils.mergeFsIndexes(fsIndexes, aResourceManager);
    TypeSystemDescription aggTypeDesc = CasCreationUtils.mergeTypeSystems(typeSystems,
            aResourceManager);

    this.typeSystemDescription = aggTypeDesc;
    this.typePriorities = aggTypePriorities;
    this.fsIndexDescriptions = aggIndexColl.getFsIndexes();
    this.resourceManager = aResourceManager;
  }

  /**
   * @return Returns the fsIndexDescriptions.
   */
  public FsIndexDescription[] getFsIndexDescriptions() {
    return fsIndexDescriptions;
  }

  /**
   * @param fsIndexDescriptions
   *          The fsIndexDescriptions to set.
   */
  public void setFsIndexDescriptions(FsIndexDescription[] fsIndexDescriptions) {
    this.fsIndexDescriptions = fsIndexDescriptions;
  }

  /**
   * @return Returns the resourceManager.
   */
  public ResourceManager getResourceManager() {
    return resourceManager;
  }

  /**
   * @param resourceManager
   *          The resourceManager to set.
   */
  public void setResourceManager(ResourceManager resourceManager) {
    this.resourceManager = resourceManager;
  }

  /**
   * @return Returns the typePriorities.
   */
  public TypePriorities getTypePriorities() {
    return typePriorities;
  }

  /**
   * @param typePriorities
   *          The typePriorities to set.
   */
  public void setTypePriorities(TypePriorities typePriorities) {
    this.typePriorities = typePriorities;
  }

  /**
   * @return Returns the typeSystemDescription.
   */
  public TypeSystemDescription getTypeSystemDescription() {
    return typeSystemDescription;
  }

  /**
   * @param typeSystemDescription
   *          The typeSystemDescription to set.
   */
  public void setTypeSystemDescription(TypeSystemDescription typeSystemDescription) {
    this.typeSystemDescription = typeSystemDescription;
  }

  /**
   * Gets the CasManager associated with this CAS Definition.
   * 
   * @return this CAS Definition's CasManager
   */
  public CasManager getCasManager() {
    if (this.resourceManager != null) {
      return this.resourceManager.getCasManager();
    } else {
      return null;
    }
  }
  
  /**
   * Constructs and returns a <code>ProcessingResourceMetaData</code> object
   * that contains the type system, indexes, and type priorities definitions
   * for the CAS.
   * 
   * @return processing resource metadata object containing the 
   *   relevant parts of the CAS definition
   */
  public ProcessingResourceMetaData getProcessingResourceMetaData() {
    ProcessingResourceMetaData md = UIMAFramework.getResourceSpecifierFactory().createProcessingResourceMetaData();
    md.setTypeSystem(getTypeSystemDescription());
    md.setTypePriorities(getTypePriorities());
    FsIndexCollection indColl = UIMAFramework.getResourceSpecifierFactory().createFsIndexCollection();
    indColl.setFsIndexes(getFsIndexDescriptions());
    md.setFsIndexCollection(indColl);
    return md;
  }
}
