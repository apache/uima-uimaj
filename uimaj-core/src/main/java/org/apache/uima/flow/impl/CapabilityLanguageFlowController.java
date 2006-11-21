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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAFramework;
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
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.Capability;

/**
 * FlowController for the CapabilityLanguageFlow, which uses a linear fow but may skip some of the
 * AEs in the flow if they do not handle the language of the current document or if their outputs
 * have already been produced by a previous AE in the flow.
 */
public class CapabilityLanguageFlowController extends CasFlowController_ImplBase {
  private ArrayList mStaticSequence;

  private Map mComponentMetaDataMap;

  private Map mFlowTable;

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
    mStaticSequence = new ArrayList();
    CapabilityLanguageFlow flowConstraints = (CapabilityLanguageFlow) aContext
                    .getAggregateMetadata().getFlowConstraints();
    String[] flow = flowConstraints.getCapabilityLanguageFlow();
    for (int i = 0; i < flow.length; i++) {
      AnalysisEngineMetaData md = (AnalysisEngineMetaData) mComponentMetaDataMap.get(flow[i]);
      mStaticSequence.add(new AnalysisSequenceCapabilityNode(flow[i], md.getCapabilities(), null));
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
    CapabilityLanguageFlowObject flow = new CapabilityLanguageFlowObject(mFlowTable);
    flow.setCas(aCAS);
    return flow;
  }

  /**
   * method computeFlowTable create the flow table for faster processing. The flow table inlcudes
   * for all languages in the capabilities the coresponding flow sequence
   * 
   * @param aCapabilities
   *          aggregate engine capabilities
   * @return Map - flow table includes all sequences for all languages
   */
  protected Map computeFlowTable(Capability[] aCapabilities) {
    // create flowTable
    Map flowTable = new HashMap();

    // get all languages from the capabilities
    HashSet languages = new HashSet();
    for (int i = 0; i < aCapabilities.length; i++) {
      // get languages from current capability
      aCapabilities[i].getLanguagesSupported();
      String language;
      for (int y = 0; y < aCapabilities[i].getLanguagesSupported().length; y++) {
        language = aCapabilities[i].getLanguagesSupported()[y];
        languages.add(language);
      }
    }

    // create flow table with sequences for all languages
    Iterator it = languages.iterator();
    while (it.hasNext()) {
      // add sequence for the current language
      String language = (String) it.next();
      flowTable.put(language, computeSequence(language, aCapabilities));
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
  protected List computeSequence(String language, Capability[] aCapabilities) {
    language = Language.normalize(language);

    // create resultSpec from the current aggregate capabilities
    ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
                    .createResultSpecification();

    if (aCapabilities != null) {
      resultSpec.addCapabilities(aCapabilities);
    } else {
      return null;
    }

    // create array list for the current sequence
    List newSequence = new ArrayList();

    // loop pver all annotators that should be called
    for (int sequenceIndex = 0; sequenceIndex < mStaticSequence.size(); sequenceIndex++) {
      // get array of ouput capabilities for the current languge from the current result spec
      TypeOrFeature[] ouputCapabilities = resultSpec.getResultTypesAndFeatures(language);

      // strip language extension if available
      int index = language.indexOf(LANGUAGE_SEPARATOR);

      // if country extension is available
      if (index >= 0) {
        // create HashSet for outputSpec
        HashSet outputSpec = new HashSet();

        // add language with country extension output capabilities to the outputSpec
        if (ouputCapabilities.length > 0) {
          for (int i = 0; i < ouputCapabilities.length; i++) {
            outputSpec.add(ouputCapabilities[i]);
          }

          // get array of output capabilities only for the language without country extension
          ouputCapabilities = resultSpec.getResultTypesAndFeatures(language.substring(0, index));

          // add language output capabilities to the outputSpec
          for (int i = 0; i < ouputCapabilities.length; i++) {
            outputSpec.add(ouputCapabilities[i]);
          }

          // convert all output capabilities to a outputCapabilities array
          ouputCapabilities = new TypeOrFeature[outputSpec.size()];
          outputSpec.toArray(ouputCapabilities);
        } else
        // for language with country extension was noting found
        {
          // get array of output capabilities with the new main language without country extension
          ouputCapabilities = resultSpec.getResultTypesAndFeatures(language.substring(0, index));
        }
      }

      // current analysis node which contains the current analysis engine
      AnalysisSequenceCapabilityNode node;

      // result spec for the current analysis engine
      ResultSpecification currentAnalysisResultSpec = null;

      // flag if current analysis engine should be called or not
      boolean shouldEngineBeCalled = false;

      // check output capabilites from the current result spec

      // get next analysis engine from the sequence node
      node = (AnalysisSequenceCapabilityNode) mStaticSequence.get(sequenceIndex);

      // get capability container from the current analysis engine
      CapabilityContainer capabilityContainer = node.getCapabilityContainer();

      // create current analysis result spec without any language information
      currentAnalysisResultSpec = UIMAFramework.getResourceSpecifierFactory()
                      .createResultSpecification();

      // check if engine should be called - loop over all ouput capabilities of the result spec
      for (int i = 0; i < ouputCapabilities.length; i++) {
        // check if current ToF can be produced by the current analysis engine
        if (capabilityContainer.hasOutputTypeOrFeature(ouputCapabilities[i], language, true)) {
          currentAnalysisResultSpec.addResultTypeOrFeature(ouputCapabilities[i]);
          shouldEngineBeCalled = true;

          // remove current ToF from the result spec
          resultSpec.removeTypeOrFeature(ouputCapabilities[i]);
        }
      }
      // skip engine if not output capability match

      // check if current engine should be called
      if (shouldEngineBeCalled == true) {
        // set result spec for current analysis engine
        node.setResultSpec(currentAnalysisResultSpec);

        // add note to the current sequence
        newSequence.add(node.clone());
      } else
      // engine should not be called, but add null to the sequence to track that
      // engine should not be called
      {
        newSequence.add(null);
      }
    }

    return newSequence;
  }
}
