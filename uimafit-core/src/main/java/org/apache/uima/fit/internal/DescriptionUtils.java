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
package org.apache.uima.fit.internal;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;

/**
 * INTERNAL API - Helper functions for operatin with descriptions.
 */
public final class DescriptionUtils {

  private DescriptionUtils() {
    // No instances
  }

  /**
   * Consolidate duplicate information in the AE description. 
   * 
   * @param aDesc an AE description
   * @return a consolidated AE description
   */
  public static AnalysisEngineDescription consolidate(AnalysisEngineDescription aDesc)
          throws ResourceInitializationException, InvalidXMLException {
    // First we clone, because we perform changes within the descriptor.
    AnalysisEngineDescription desc = (AnalysisEngineDescription) aDesc.clone();

    consolidateAggregate(desc, UIMAFramework.newDefaultResourceManager());

    return desc;
  }

  private static void consolidateAggregate(AnalysisEngineDescription aDesc, ResourceManager aResMgr)
          throws ResourceInitializationException, InvalidXMLException {
    if (aDesc.isPrimitive() || aDesc.getDelegateAnalysisEngineSpecifiers().isEmpty()) {
      return;
    }

    // Depth-first
    for (ResourceSpecifier delegate : aDesc.getDelegateAnalysisEngineSpecifiers().values()) {
      consolidateAggregate((AnalysisEngineDescription) delegate, aResMgr);
    }

    // Consolidate type system, indexes and type priorities
    ProcessingResourceMetaData meta = CasCreationUtils.mergeDelegateAnalysisEngineMetaData(aDesc,
            aResMgr, null, null);

    AnalysisEngineDescription firstDelegate = null;
    for (ResourceSpecifier delegate : aDesc.getDelegateAnalysisEngineSpecifiers().values()) {
      AnalysisEngineDescription aeDelegate = (AnalysisEngineDescription) delegate;
      aeDelegate.getAnalysisEngineMetaData().setTypeSystem(null);
      aeDelegate.getAnalysisEngineMetaData().setTypePriorities(null);
      aeDelegate.getAnalysisEngineMetaData().setFsIndexCollection(null);
      
      if (firstDelegate == null) {
        firstDelegate = aeDelegate;
      }
    }
    
    // Type systems cannot be set on aggregates, so set it on the first delegate instead
    firstDelegate.getAnalysisEngineMetaData().setTypeSystem(meta.getTypeSystem());
    // The rest can be set on the aggregate
    aDesc.getAnalysisEngineMetaData().setTypePriorities(meta.getTypePriorities());
    aDesc.getAnalysisEngineMetaData().setFsIndexCollection(meta.getFsIndexCollection());

    // Consolidate external resources
    Map<String, ExternalResourceDescription> resources = new LinkedHashMap<String, ExternalResourceDescription>();
    for (ResourceSpecifier delegate : aDesc.getDelegateAnalysisEngineSpecifiers().values()) {
      AnalysisEngineDescription aeDelegate = (AnalysisEngineDescription) delegate;
      ResourceManagerConfiguration delegateResMgrCfg = aeDelegate.getResourceManagerConfiguration();
      for (ExternalResourceDescription res : delegateResMgrCfg.getExternalResources()) {
        resources.put(res.getName(), res);
      }

      delegateResMgrCfg.setExternalResources(null);
    }

    ResourceManagerConfiguration resMgrCfg = aDesc.getResourceManagerConfiguration();
    if (resMgrCfg == null) {
      resMgrCfg = UIMAFramework.getResourceSpecifierFactory().createResourceManagerConfiguration();
      aDesc.setResourceManagerConfiguration(resMgrCfg);
    }

    resMgrCfg.setExternalResources(resources.values().toArray(
            new ExternalResourceDescription[resources.size()]));
  }
}
