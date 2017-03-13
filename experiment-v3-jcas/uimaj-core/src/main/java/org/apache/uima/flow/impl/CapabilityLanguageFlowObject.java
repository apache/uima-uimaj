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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.Language;
import org.apache.uima.flow.CasFlow_ImplBase;
import org.apache.uima.flow.FinalStep;
import org.apache.uima.flow.SimpleStepWithResultSpec;
import org.apache.uima.flow.Step;

/**
 * The <code>CapabilityLanguageAnalysisSequence</code> is used for a
 * <code>CapabilityLanguageFlow</code>. The sequence contains all analysis engines included in
 * the <code>CapabilityLanguageFlow</code>.
 * 
 * Within this sequence skipping of analysis engines is possible if the document language of the
 * current document does not match to the analysis engine capabilities or the output capabilities
 * are already done by another analysis engine.
 * 
 */
public class CapabilityLanguageFlowObject extends CasFlow_ImplBase implements Cloneable {

  private static final String UNSPECIFIED_LANGUAGE = "x-unspecified";

  /**
   * save the last type system
   */
  private TypeSystem mLastTypeSystem;

//  /**
//   * The static list of nodes.
//   */
//  private List mNodeList;

  private final CapabilityLanguageFlowController mParentController;
  
  /**
   * Current index in the sequence list.
   */
  private int mIndex;

//  /**
//   * mResultSpec provides the current result specification which has to be processed. After every
//   * analysis run, the processed ouput result are removed from the mResultSpec.
//   */
//  private ResultSpecification mResultSpec;

  /**
   * flowTable includes all languages with their flow sequence
   */
  private Map<String, List<AnalysisSequenceCapabilityNode>> mFlowTable;

  /**
   * main language separator e.g 'en' and 'en-US'
   */
  private static final char LANGUAGE_SEPARATOR = '-';

  static final long serialVersionUID = -5879514955935785660L;

  
  // Next constructor is never referenced - try removing it :-)  MIS 1/2008
//  /**
//   * Creates a new CapabilityLanguageAnalysisSequence.
//   * 
//   * @param aNodeList
//   *          a List of {@link AnalysisSequenceNode} objects. These will be returned in order by
//   *          {@link #getNext(CAS)}.
//   * @param resultSpec
//   *          result specification of the top level aggregate AE
//   */
//  public CapabilityLanguageFlowObject(List aNodeList, ResultSpecification resultSpec) {
//    mNodeList = aNodeList;
//    mIndex = 0;
//    // clone result specification
//    mResultSpec = (ResultSpecification) resultSpec.clone();
//    mFlowTable = null;
//    mLastTypeSystem = null;
//
//  }

  /**
   * Create a new CapabilityLangaugeAnalysisSequence with the flowTable
   * 
   * @param aFlowTable
   *          a flow table
   * @param aParentController -         
   */
  public CapabilityLanguageFlowObject(Map<String, List<AnalysisSequenceCapabilityNode>> aFlowTable, 
      CapabilityLanguageFlowController aParentController) {
//    mNodeList = null;
    mIndex = 0;
//    mResultSpec = null;
    mFlowTable = aFlowTable;
    mLastTypeSystem = null;
    mParentController = aParentController;
  }

  public Step next() {
    // check if CAS is set
    CAS cas = getCas();
    assert cas != null; // CapabilityLanguageFlowController ensures this

    // if type system has changed, recompile flow table to pick up 
    //   potentially different type system inheritances
    if (mLastTypeSystem != cas.getTypeSystem()) {
      // set new type system
      mLastTypeSystem = cas.getTypeSystem();

      // recompile all result specs
      recompileFlowTable();
    }

    // get current document language from the CAS
    String documentLanguage = Language.normalize(cas.getDocumentLanguage());

//    if (mNodeList != null) {
//      // check if another engine is available
//      if (mIndex >= mNodeList.size()) {
//        return new FinalStep();
//      } else {
//        // get array of ouput capabilities for the current languge from the current result spec
//        TypeOrFeature[] ouputCapabilities = mResultSpec.getResultTypesAndFeatures(documentLanguage);
//
//        // strip language extension if available
//        int index = documentLanguage.indexOf(LANGUAGE_SEPARATOR);
//
//        // if country extension was available
//        if (index >= 0) {
//          // create HashSet for outputSpec
//          HashSet outputSpec = new HashSet();
//
//          // add language with country extension output capabilities to the outputSpec
//          if (ouputCapabilities.length > 0) {
//            for (int i = 0; i < ouputCapabilities.length; i++) {
//              outputSpec.add(ouputCapabilities[i]);
//            }
//
//            // get array of output capabilities only for the language without country extension
//            ouputCapabilities = mResultSpec.getResultTypesAndFeatures(documentLanguage.substring(0,
//                    index));
//
//            // add language output capabilities to the outputSpec
//            for (int i = 0; i < ouputCapabilities.length; i++) {
//              outputSpec.add(ouputCapabilities[i]);
//            }
//
//            // convert all output capabilities to a outputCapabilities array
//            ouputCapabilities = new TypeOrFeature[outputSpec.size()];
//            outputSpec.toArray(ouputCapabilities);
//          } else // for language with country extension was noting found
//          {
//            // get array of output capabilities with the new main language without country extension
//            ouputCapabilities = mResultSpec.getResultTypesAndFeatures(documentLanguage.substring(0,
//                    index));
//          }
//        }
//
//        // current analysis node which contains the current analysis engine
//        AnalysisSequenceCapabilityNode node;
//
//        // result spec for the current analysis engine
//        ResultSpecification currentAnalysisResultSpec = null;
//
//        // flag if current analysis engine should be called or not
//        boolean shouldEngineBeCalled = false;
//
//        // check output capabilites from the current result spec
//        do {
//          // get next analysis engine from the sequence node
//          node = (AnalysisSequenceCapabilityNode) mNodeList.get(mIndex++);
//
//          // get capability container from the current analysis engine
//          CapabilityContainer capabilityContainer = node.getCapabilityContainer();
//
//          // create current analysis result spec without any language information
//          currentAnalysisResultSpec = UIMAFramework.getResourceSpecifierFactory()
//                  .createResultSpecification();
//
//          // check if engine should be called - loop over all ouput capabilities of the result spec
//          for (int i = 0; i < ouputCapabilities.length; i++) {
//            // check if current ToF can be produced by the current analysis engine
//            if (capabilityContainer.hasOutputTypeOrFeature(ouputCapabilities[i], documentLanguage,
//                    true)) {
//              currentAnalysisResultSpec.addResultTypeOrFeature(ouputCapabilities[i]);
//              shouldEngineBeCalled = true;
//
//              // remove current ToF from the result spec
//              mResultSpec.removeTypeOrFeature(ouputCapabilities[i]);
//            }
//
//          }
//          // skip engine if not output capability match
//        } while (shouldEngineBeCalled == false && mIndex < mNodeList.size());
//
//        // check if current engine should be called
//        if (shouldEngineBeCalled == true) {
//          // set result spec for current analysis engine
//          node.setResultSpec(currentAnalysisResultSpec);
//
//          // return current analysis engine node
//          return new SimpleStepWithResultSpec(node.getCasProcessorKey(), currentAnalysisResultSpec);
//        } else // no engine left which can be called
//        {
//          return new FinalStep();
//        }
//      }
//    } else if (mFlowTable != null) {
    
      // in this impl, mFlowTable is never null
      AnalysisSequenceCapabilityNode node = null;

      // check if document language is included in the flowTable
      List<AnalysisSequenceCapabilityNode> flow = mFlowTable.get(documentLanguage);

      if (flow == null) { // try to get flow without language extension or with x-unspecified
        // strip language extension if available
        int index = documentLanguage.indexOf(LANGUAGE_SEPARATOR);

        // if country extension is available
        if (index >= 0) {
          // check if document language is included in the flowTable
          flow = mFlowTable.get(documentLanguage.substring(0, index));
          // If the language was not found, use flow for unspecified lang instead.
          if (flow == null) {
            flow = mFlowTable.get(UNSPECIFIED_LANGUAGE);
          }
        } else {// try to get flow for language x-unspecified
          flow = mFlowTable.get(UNSPECIFIED_LANGUAGE);
        }
      }

      // if flow is available get next node
      if (flow != null) {
        if (flow.size() > mIndex) {
          node = flow.get(mIndex++);
          while (node == null && flow.size() > mIndex) {
            node = flow.get(mIndex++);
          }
        }
      }
      if (node != null) {
        // see if this next cas processor was previously given this result spec, and 
        // if so, set a flag indicating this
        
        Map<String, ResultSpecification> lastResultSpecForComponent = mParentController.getLastResultSpecForComponent();
        String component = node.getCasProcessorKey();
        ResultSpecification neededResultSpec = node.getResultSpec();      
        ResultSpecification previousResultSpec = lastResultSpecForComponent.get(component);
        
        if (null == previousResultSpec || previousResultSpec != neededResultSpec) {
          lastResultSpecForComponent.put(component, neededResultSpec);
          return new SimpleStepWithResultSpec(component, neededResultSpec); 
        } 
        // null is a special flag saying the previous component result spec is still good
        return new SimpleStepWithResultSpec(node.getCasProcessorKey(), null);
//        return new SimpleStepWithResultSpec(component, neededResultSpec); // for testing with caching disabled
      } 
//    }
    return new FinalStep();
  }

  /**
   * Returns a clone of this <code>AnalysisSequence</code>.
   * 
   * @return a new <code>AnalysisSequence</code> object that is an exact clone of this one.
   */
  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  /**
   * reset index of the sequence to 0
   */
  public void resetIndex() {
    mIndex = 0;
  }

  /**
   * recompiles all result specs in the flow table with the current type system
   * Actual recompiling is done later when first needed; what happens now is that
   * the type system is set into the result spec, which the compile will need.
   */
  protected void recompileFlowTable() {

    if (mFlowTable != null) {
      
      // drop any caching that may be happening
      //   to force sending new result specs down
      mParentController.getLastResultSpecForComponent().clear();

      // get all language key from the table
      Set<String> keys = mFlowTable.keySet();

      // loop over all languages
      Iterator<String> it = keys.iterator();
      while (it.hasNext()) {

        // get sequence for current language
        List<AnalysisSequenceCapabilityNode> sequence = mFlowTable.get(it.next());

        // loop over all nodes in the sequence
        for (int i = 0; i < sequence.size(); i++) {
          // get current annotator node
          AnalysisSequenceCapabilityNode node = sequence.get(i);
          if (node != null) {
            // recompile result spec
            node.getResultSpec().setTypeSystem(mLastTypeSystem);
          }
        }
      }
    }
  }
}
