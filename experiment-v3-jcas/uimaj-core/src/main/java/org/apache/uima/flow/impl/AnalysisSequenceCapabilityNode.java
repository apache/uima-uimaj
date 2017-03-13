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

import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.impl.ResultSpecification_impl;
import org.apache.uima.collection.base_cpm.CasObjectProcessor;
import org.apache.uima.resource.metadata.Capability;

/**
 * A <code>AnalysisSequenceCapabilityNode</code> is a node element of the
 * {@link CapabilityLanguageFlowObject}. A <code>AnalysisSequenceCapabilityNode</code> has a
 * <code>AnalysisEngine</code>, a <code>ResultSpecification</code> which should be processed
 * from the <code>AnalysisEngine</code>. Also a <code>AnalysisSequenceCapabilityNode</code> has
 * a {@link ResultSpecification} which inculdes the capabilities of the <code>AnalysisEngine</code>.
 * 
 */
public class AnalysisSequenceCapabilityNode implements Cloneable {
  private static final long serialVersionUID = -1471125199227401514L;

  /**
   * The reference to the AnalysisEngine to be executed at this point in the sequence. If this is
   * null, the Key should be used to find the AnalysisEngine. This field is transient and so does
   * not persist when this AnalysisEngineSequence is serialized.
   */
  private transient CasObjectProcessor mCasProcessor = null;

  /**
   * The Key of the AnalysisEngine to be executed at this point in the sequence.
   */
  private String mCasProcessorKey;

  /**
   * The ResultSpecification to provide to the AnalysisEngine at this point in the sequence. May be
   * null, indicating that the AnalysisEngine should produce all possible results.
   */
  private ResultSpecification mResultSpec;

  /**
   * The mCapabilityContainer hold the capabilities of the current AnalyseEngine. The capabilities
   * are held in a ResultSpecification for quick access to ToFs or languages
   */
//  private CapabilityContainer mCapabilityContainer;
  
  private ResultSpecification mCapabilityContainer;

  /**
   * Creates a new AnalysisSequenceCapabilityNode from an AnalysisEngine reference
   * 
   * @param aKey
   *          key for AnalysisEngine to be executed at this point in sequence
   * @param aCasProcessor
   *          reference to the AnalysisEngine instance
   * @param aResultSpec
   *          result specification to be passed to this AnalysisEngine
   */
  public AnalysisSequenceCapabilityNode(String aKey, CasObjectProcessor aCasProcessor,
          ResultSpecification aResultSpec) {
    mCasProcessorKey = aKey;
    mCasProcessor = aCasProcessor;
    mResultSpec = aResultSpec;
    mCapabilityContainer = null;

    // check if analysis engine is available
    if (mCasProcessor != null) {
      // get capabilities of the current analysis engine
      Capability[] capabilities = mCasProcessor.getProcessingResourceMetaData().getCapabilities();

      // create capability container and compile only output capabilities
//      mCapabilityContainer = new CapabilityContainer(capabilities, false, true);
      mCapabilityContainer = new ResultSpecification_impl();
      mCapabilityContainer.addCapabilities(capabilities);
    }
  }

  /**
   * Creates a new AnalysisSequenceCapabilityNode from a AnalysisEngine Key. This is to be used when
   * a direct reference to a AnalysisEngine is not available.
   * 
   * @param aCasProcessorKey
   *          Key of a AnalysisEngine
   * @param aCasProcessorCapabilities
   *          Capabilities for this AnalysisEngine
   * @param aResultSpec
   *          result specification to be passed to this AnalysisEngine
   */
  public AnalysisSequenceCapabilityNode(String aCasProcessorKey,
          Capability[] aCasProcessorCapabilities, ResultSpecification aResultSpec) {
    mCasProcessorKey = aCasProcessorKey;
    mResultSpec = aResultSpec;
    mCasProcessor = null;

    // analysis engine is not set, so we cannot create capabilityContainer
//    mCapabilityContainer = new CapabilityContainer(aCasProcessorCapabilities, false, true);
    mCapabilityContainer = new ResultSpecification_impl();
    mCapabilityContainer.addCapabilities(aCasProcessorCapabilities);
  }

  public String getCasProcessorKey() {
    return mCasProcessorKey;
  }

  public CasObjectProcessor getCasProcessor() {
    return mCasProcessor;
  }

  public ResultSpecification getResultSpec() {
    return mResultSpec;
  }

  /**
   * Sets this node's Result Specificatoin.
   * @param aResultSpec -
   */
  public void setResultSpec(ResultSpecification aResultSpec) {
    mResultSpec = aResultSpec;
  }

  /**
   * Returns a clone of this <code>AnalysisSequenceNode</code>.
   * 
   * @return a new <code>AnalysisSequenceNode</code> object that is an exact clone of this one.
   */
  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      assert false : "AnalysisSequenceNode is cloneable";
      return null;
    }
  }

  /**
   * Returns the capabilityContainer reference.
   * 
   * @return CapabilityContainer - returns the reference to the capability container
   */
//  public CapabilityContainer getCapabilityContainer() {
  public ResultSpecification getCapabilityContainer() {
    return mCapabilityContainer;
  }

}
