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

package org.apache.uima.analysis_engine.metadata;

/**
 * A <code>CapabilityLanguageFlow</code> is a simple type of {@link FlowConstraints} that
 * specifies the complete flow as a capabilityLanguage sequence.
 * <p>
 * Each element in the sequence is specified as a String identifier. In a
 * <code>CapabilityLanguageFlow</code> skipping of the included AnalysisEngines is possible if the
 * document language does not map to the capabilities or the output capability was already done by
 * another AnalysisEngine.
 */
public interface CapabilityLanguageFlow extends FlowConstraints {
  public static final String FLOW_CONSTRAINTS_TYPE = "CAPABILITY_LANGUAGE";

  /**
   * Gets the type of this <code>FlowConstraints</code> object. Each sub-interface of
   * <code>FlowConstraints</code> has its own standard type identifier String. These identifier
   * Strings are used instead of Java class names in order to ease portability of metadata to other
   * languages.
   * 
   * @return {@link #FLOW_CONSTRAINTS_TYPE}
   */
  public String getFlowConstraintsType();

  /**
   * Returns the flow as an array. Each element of the array is a String that identifies the
   * AnalysisEngine to invoke at that position in the flow.
   * 
   * @return an array of AE identifiers.
   */
  public String[] getCapabilityLanguageFlow();

  /**
   * Sets the CapabilityLanguageFlow.
   * 
   * @param aFlow
   *          an array of Strings, each of which identifies the AnalysisEngine to invoke at that
   *          position in the flow.
   * 
   * @throws org.apache.uima.UIMA_UnsupportedOperationException
   *           if this <code>MetaDataObject</code> is not modifiable.
   */
  public void setCapabilityLanguageFlow(String[] aFlow);

}
