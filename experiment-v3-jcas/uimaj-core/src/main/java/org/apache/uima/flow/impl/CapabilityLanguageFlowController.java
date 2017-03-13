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

package org.apache.uima.flow.impl;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.CapabilityLanguageFlow;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.Language;
import org.apache.uima.flow.CasFlowController_ImplBase;
import org.apache.uima.flow.Flow;
import org.apache.uima.flow.FlowControllerContext;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

/**
 * FlowController for the CapabilityLanguageFlow, which uses a linear flow but may skip some of the
 * AEs in the flow if they do not handle the language of the current document or if their outputs
 * have already been produced by a previous AE in the flow.
 */
public class CapabilityLanguageFlowController extends CasFlowController_ImplBase {
  private List<AnalysisSequenceCapabilityNode> mStaticSequence;

  private Map<String, AnalysisEngineMetaData> mComponentMetaDataMap;

  private Map<String, List<AnalysisSequenceCapabilityNode>> mFlowTable;
  
  private final Map<String, ResultSpecification> lastResultSpecForComponent = 
    new HashMap<String, ResultSpecification>();

  /**
   * main language separator e.g 'en' and 'en-US'
   */
  private static final char LANGUAGE_SEPARATOR = '-';

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.flow.FlowController#initialize(FlowControllerContext)
   */
  public void initialize(FlowControllerContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    mComponentMetaDataMap = aContext.getAnalysisEngineMetaDataMap();

    // build a list of AnalysisSequenceNodes from the capabilityLanguageFlow
    mStaticSequence = new ArrayList<AnalysisSequenceCapabilityNode>();
    CapabilityLanguageFlow flowConstraints = (CapabilityLanguageFlow) aContext
            .getAggregateMetadata().getFlowConstraints();
    for (String aeKey : flowConstraints.getCapabilityLanguageFlow()) {
      mStaticSequence.add(
          new AnalysisSequenceCapabilityNode(
              aeKey, 
              mComponentMetaDataMap.get(aeKey).getCapabilities(), 
              null));
    }

    // compute flow table with the specified capabilities
    mFlowTable = computeFlowTable(aContext.getAggregateMetadata().getCapabilities());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.flow.CasFlowController_ImplBase#computeFlow(org.apache.uima.cas.CAS)
   */
  public Flow computeFlow(CAS aCAS) throws AnalysisEngineProcessException {
    CapabilityLanguageFlowObject flow = new CapabilityLanguageFlowObject(mFlowTable, this);
    flow.setCas(aCAS);
    return flow;
  }

  /**
   * method computeFlowTable create the flow table for faster processing. The flow table includes
   * the corresponding flow sequence for all languages in the capabilities 
   * 
   * @param aCapabilities
   *          aggregate engine capabilities
   * @return Map - flow table includes all sequences for all languages
   */
  protected Map<String, List<AnalysisSequenceCapabilityNode>> computeFlowTable(Capability[] aCapabilities) {
    // create flowTable
    Map<String, List<AnalysisSequenceCapabilityNode>> flowTable = 
      new HashMap<String, List<AnalysisSequenceCapabilityNode>>();

    // get all languages from the capabilities
    Set<String> languages = new HashSet<String>();
    for (Capability capability : aCapabilities) {
      for (String capabilityLanguage : capability.getLanguagesSupported()) {
        languages.add(capabilityLanguage);
      }
    }

    // create flow table with sequences for all languages
    for (String capabilityLanguage : languages) {
      flowTable.put(capabilityLanguage, computeSequence(capabilityLanguage, aCapabilities));
    }
    
    return flowTable;
  }

  /**
   * method computeSequence creates a capabilityLanguageAnalysisSequence for the given language
   * 
   * @param language
   *          current language
   * @param aCapabilities
   *          output capabilities of the aggregate engine
   * 
   * @return List - capabilityLanguageAnalysisSequence for the current language
   */
  protected List<AnalysisSequenceCapabilityNode> computeSequence(String language, Capability[] aCapabilities) {
    language = Language.normalize(language);  // lower-cases, replaces _ with -, changes null to x-unspecified

    // create resultSpec from the current aggregate capabilities
    ResultSpecification aggrResultsToProduce = UIMAFramework.getResourceSpecifierFactory()
            .createResultSpecification();

    if (aCapabilities != null) {
      aggrResultsToProduce.addCapabilities(aCapabilities);
    } else {
      return null;
    }

    // create array list for the current sequence
    List<AnalysisSequenceCapabilityNode> newSequence = new ArrayList<AnalysisSequenceCapabilityNode>();

    // loop over all annotators that should be called
    // In this loop we will gradually reduce the set of output capabilities 
    for (int sequenceIndex = 0; sequenceIndex < mStaticSequence.size(); sequenceIndex++) {
      // get array of output capabilities for the current language from the current result spec
      TypeOrFeature[] tofsNeeded = aggrResultsToProduce.getResultTypesAndFeatures(language);

      // Augment these outputCapabilities if the language-spec is for a country, to 
      // include the outputCapabilities for the language without the country-spec.
      
      // strip language extension if available
      int index = language.indexOf(LANGUAGE_SEPARATOR);

      // if country extension is available
      if (index >= 0) {
        // create Set for outputSpecs, so we can eliminate duplicates
        Set<TypeOrFeature> outputSpec = new HashSet<TypeOrFeature>();

        // add language with country extension removed, 
        // to the existing output capabilities (or if non exist, just use
        // the capabilities for the language without the country extension)
        if (tofsNeeded.length > 0) {
          // copy all existing capabilities to the Set
          for (TypeOrFeature outputCapability : tofsNeeded) {
            outputSpec.add(outputCapability);
          }

          // get array of output capabilities only for the language without country extension
          tofsNeeded = aggrResultsToProduce.getResultTypesAndFeatures(language.substring(0, index));

          // add language output capabilities to the Set
          for (TypeOrFeature outputCapability : tofsNeeded) {
            outputSpec.add(outputCapability);
          }

          // convert all output capabilities to a outputCapabilities array
          tofsNeeded = new TypeOrFeature[outputSpec.size()];
          outputSpec.toArray(tofsNeeded);
        } else { // for language with country extension was noting found        
          // get array of output capabilities with the new main language without country extension
          tofsNeeded = aggrResultsToProduce.getResultTypesAndFeatures(language.substring(0, index));
        }
      }

      // current analysis node which contains the current analysis engine
      AnalysisSequenceCapabilityNode node;

      // result spec for the current analysis engine
      ResultSpecification currentAnalysisResultSpec = null;

      // flag if current analysis engine should be called or not
      boolean shouldEngineBeCalled = false;

      // check output capabilities from the current result spec

      // get next analysis engine from the sequence node
      node = mStaticSequence.get(sequenceIndex);

      // get capability container from the current analysis engine
      ResultSpecification delegateProduces = node.getCapabilityContainer();

      // create current analysis result spec without any language information
      currentAnalysisResultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();

      // check if engine should be called - 
      //   loop over all remaining output capabilities of the aggregate's result spec
      //     to see if this component of the aggregate produces that type or feature,
      //     for this language
      for (TypeOrFeature tof : tofsNeeded) {
        if ((tof.isType() && delegateProduces.containsType(tof.getName(), language)) ||
            (!tof.isType() && delegateProduces.containsFeature(tof.getName(), language))) {
//        if (capabilityContainer.hasOutputTypeOrFeature(tof, language, true)) {
          currentAnalysisResultSpec.addResultTypeOrFeature(tof);
          shouldEngineBeCalled = true;
          // remove current ToF from the result spec
          aggrResultsToProduce.removeTypeOrFeature(tof);
        }
      }
      
      // skip engine if not output capability match

      // should be called is false if this engine produces none of the 
      //   needed outputs of the aggregate
      if (shouldEngineBeCalled) {
        // tell this component which output types/features need to be produced
        //   note: As an exception to the way normal result-specifications are produced,
        //         here we *don't* add the types/features which are input to
        //         other delegates need to be produced.
        //         This is for backward compatibility.
        node.setResultSpec(currentAnalysisResultSpec);

        // add note to the current sequence
        newSequence.add((AnalysisSequenceCapabilityNode)node.clone());
      } else {
      // engine should not be called, but add null to the sequence to track that
      // engine should not be called
        newSequence.add(null);
      }
    } // loop over all delegates in the flow sequence

    return newSequence;
  }

  public static FlowControllerDescription getDescription() {
    URL descUrl = FixedFlowController.class
            .getResource("/org/apache/uima/flow/CapabilityLanguageFlowController.xml");
    FlowControllerDescription desc;
    try {
      desc = (FlowControllerDescription) UIMAFramework.getXMLParser().parse(
              new XMLInputSource(descUrl));
    } catch (InvalidXMLException e) {
      throw new UIMARuntimeException(e);
    } catch (IOException e) {
      throw new UIMARuntimeException(e);
    }
    return desc;
  }

  public Map<String, ResultSpecification> getLastResultSpecForComponent() {
    return lastResultSpecForComponent;
  }
}
