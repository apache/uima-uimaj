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

package org.apache.uima.analysis_engine.asb;

import java.util.Map;

import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.analysis_engine.asb.impl.FlowControllerContainer;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;

/**
 * The Analysis Structure Broker (<code>ASB</code>) is the component responsible for the details
 * of communicating with Analysis Engines that may potentially be distributed across different
 * physical machines. The ASB hides all such details from the rest of the AnalysisEngine, which
 * should be able to operate with no knowledge of whether Analysis Engines are co-located or
 * distributed.
 */
public interface ASB extends Resource {
  /**
   * Key for the initialization parameter whose value is the name of the aggregate analysis engine
   * that owns this ASB. This is used for recording ProcessTrace events.
   */
  public static final String PARAM_AGGREGATE_ANALYSIS_ENGINE_NAME = "PARAM_AGGREGATE_ANALYSIS_ENGINE_NAME";

  /**
   * Called by the Aggregate Analysis Engine to provide this ASB with information it needs to
   * operate.
   * <p>
   * This includes a collection of {@link org.apache.uima.resource.ResourceSpecifier} objects that
   * describe how to create or locate the component AnalysisEngines within the aggregate. Each
   * <code>ResourceSpecifier</code> has an associated key, which the aggregate Analysis Engine and
   * the FlowController use to identify that component.
   * <p>
   * This method is where the component AnalysisEngines and the FlowController are instantiated.
   * 
   * @param aComponentSpecifiers
   *          a Map from String keys to <code>ResourceSpecifier</code> values, which specify how
   *          to create or locate the component CasObjectProcessors.
   * @param aParentContext
   *          the UIMA Context of the parent AnalysisEngine, used to construct the subcontexts for
   *          the components.
   * @param aFlowControllerDeclaration
   *          declaration (key and specifier) of FlowController to be used for this aggregate.
   * @param aAggregateMetadata
   *          metadata for the Aggregate AE, needed by the FlowController
   * 
   * @throws ResourceInitializationException
   *           if the {@link org.apache.uima.ResourceFactory} could not create or acquire a
   *           CasObjectProcessor instance for one of the specifiers in
   *           <code>aComponentSpecifiers</code>.
   */
  public void setup(Map<String, ResourceSpecifier> aComponentSpecifiers, UimaContextAdmin aParentContext,
          FlowControllerDeclaration aFlowControllerDeclaration,
          AnalysisEngineMetaData aAggregateMetadata) throws ResourceInitializationException;

  /**
   * Gets metadata for all of the component AnalysisEngines known to this <code>ASB</code>.
   * 
   * @return a Map from String keys (the same keys used in
   *         {@link AnalysisEngineDescription#getDelegateAnalysisEngineSpecifiers()} to
   *         {@link AnalysisEngineMetaData} values.
   * 
   * @throws org.apache.uima.UIMA_IllegalStateException
   *           if {@link #setup(Map, UimaContextAdmin, FlowControllerDeclaration, AnalysisEngineMetaData)} has not been called yet.
   */
  public Map<String, AnalysisEngineMetaData> getComponentAnalysisEngineMetaData();

  /**
   * Gets the metadata for all components known to this ASB. This includes the FlowController as
   * well as the component AnalysisEngines.
   * 
   * @return a Map from String keys (the same keys used in the aggregate AE descriptor) to
   *         {@link ProcessingResourceMetaData} values.
   */
  public Map<String, ProcessingResourceMetaData> getAllComponentMetaData();

  /**
   * Gets references to the component AnalysisEngines known to this <code>ASB</code>.
   * 
   * @return a Map from String keys (the same keys used in
   *         {@link  AnalysisEngineDescription#getDelegateAnalysisEngineSpecifiers()} to
   *         {@link AnalysisEngine} objects.
   * 
   * @throws org.apache.uima.UIMA_IllegalStateException
   *           if {@link #setup(Map, UimaContextAdmin, FlowControllerDeclaration, AnalysisEngineMetaData)} has not been called yet.
   */
  public Map<String, AnalysisEngine> getComponentAnalysisEngines();

  /**
   * Invokes the processing of the aggregate on the given input CAS. This returns a CasIterator that
   * provides access to the one or more output CASes generated from the processing. The input CAS,
   * including any modifications made to it during processing, will always be the very last element
   * returned by the <code>CasIterator</code>.
   * <p>
   * If the aggregate does not generate any output CASes of the input CAS, an empty
   * <code>CasIterator</code> will be returned.
   * 
   * @param aCAS
   *          the CAS to process
   * 
   * @return an iterator over all output CASes
   * @throws AnalysisEngineProcessException -
   */
  public CasIterator process(CAS aCAS) throws AnalysisEngineProcessException;
  
  
  /**
   * Gets the <code>FlowControllerContainer</code> known to this ASB. This includes the FlowController as
   * well as the component AnalysisEngines.
   * 
   * @return an instance of {@link FlowControllerContainer}
   */
  public FlowControllerContainer getFlowControllerContainer();
}
