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

package org.apache.uima.analysis_engine.metadata.impl;

import java.util.Map;

import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.analysis_engine.metadata.CapabilityLanguageFlow;
import org.apache.uima.analysis_engine.metadata.FlowConstraints;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

/**
 * A <code>CapabilityLanguageFlow</code> is a simple type of {@link FlowConstraints} that
 * specifies the complete flow as a capabilityLanguage sequence.
 * <p>
 * Each element in the sequence is specified as a String identifier. In a
 * <code>CapabilityLanguageFlow</code> skipping of the included AnalysisEngines is possible if the
 * document language does not map to the capabilities or the output capability was already done by
 * another AnalysisEngine.
 * 
 */
public class CapabilityLanguageFlow_impl extends MetaDataObject_impl implements
        CapabilityLanguageFlow {

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo(
          "capabilityLanguageFlow", new PropertyXmlInfo[] { new PropertyXmlInfo(
                  "capabilityLanguageFlow", null, true, "node") });

  /** Array of AnalysisEngine identifiers indicating the capabilityLanguage flow. */
  private String[] mCapabilityLanguageFlow = new String[0];

  static final long serialVersionUID = -3582926806264514233L;

  /**
   * @see org.apache.uima.analysis_engine.metadata.FlowConstraints#getFlowConstraintsType()
   */
  public String getFlowConstraintsType() {
    return FLOW_CONSTRAINTS_TYPE;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.CapabilityLanguageFlow#getCapabilityLanguageFlow()
   */
  public String[] getCapabilityLanguageFlow() {
    return mCapabilityLanguageFlow;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.CapabilityLanguageFlow#setCapabilityLanguageFlow(String[])
   */
  public void setCapabilityLanguageFlow(String[] aFlow) {
    if (aFlow == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aFlow", "setCapabilityLanguageFlow" });
    }
    mCapabilityLanguageFlow = aFlow;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.FlowConstraints#remapIDs(Map)
   */
  public void remapIDs(Map<String, String> aIDMap) {
    // Can't just overwrite existing array because cloned CapabilityLanguageFlow objects
    // share the same array. Needs more thought.
    String[] oldFlow = getCapabilityLanguageFlow();
    String[] newFlow = new String[oldFlow.length];

    for (int i = 0; i < oldFlow.length; i++) {
      String newID = aIDMap.get(oldFlow[i]);
      if (newID != null) {
        newFlow[i] = newID;
      } else {
        newFlow[i] = oldFlow[i];
      }
    }

    setCapabilityLanguageFlow(newFlow);
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }
}
