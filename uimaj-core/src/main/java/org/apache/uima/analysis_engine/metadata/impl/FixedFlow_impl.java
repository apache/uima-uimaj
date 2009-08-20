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
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

/**
 * Reference implementation of {@link FixedFlow}.
 * 
 * 
 */
public class FixedFlow_impl extends MetaDataObject_impl implements FixedFlow {

  static final long serialVersionUID = -3582926806264514233L;

  /** Array of AnalysisEngine identifiers indicating the fixed flow. */
  private String[] mFixedFlow = new String[0];

  /**
   * @see org.apache.uima.analysis_engine.metadata.FixedFlow#getFlowConstraintsType()
   */
  public String getFlowConstraintsType() {
    return FLOW_CONSTRAINTS_TYPE;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.FixedFlow#getFixedFlow()
   */
  public String[] getFixedFlow() {
    return mFixedFlow;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.FixedFlow#setFixedFlow(String[])
   */
  public void setFixedFlow(String[] aFlow) {
    if (aFlow == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aFlow", "setFixedFlow" });
    }
    mFixedFlow = aFlow;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.FlowConstraints#remapIDs(Map)
   */
  public void remapIDs(Map<String, String> aIDMap) {
    // Can't just overwrite existing array because cloned FixedFlow_impl objects
    // share the same array. Needs more thought.
    String[] oldFlow = getFixedFlow();
    String[] newFlow = new String[oldFlow.length];

    for (int i = 0; i < oldFlow.length; i++) {
      String newID = aIDMap.get(oldFlow[i]);
      if (newID != null) {
        newFlow[i] = newID;
      } else {
        newFlow[i] = oldFlow[i];
      }
    }

    setFixedFlow(newFlow);
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("fixedFlow",
          new PropertyXmlInfo[] { new PropertyXmlInfo("fixedFlow", null, true, "node") });

}
