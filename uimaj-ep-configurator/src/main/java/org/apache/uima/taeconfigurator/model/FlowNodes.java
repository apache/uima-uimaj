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

package org.apache.uima.taeconfigurator.model;

import org.apache.uima.analysis_engine.metadata.CapabilityLanguageFlow;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.analysis_engine.metadata.FlowConstraints;

/**
 * Instances of this class model the varients of flow nodes in a uniform way.
 * 
 */
public class FlowNodes {

  private FixedFlow fixedFlow;

  private CapabilityLanguageFlow capabilityLanguageFlow;

  public FlowNodes(FlowConstraints flow) {
    if (flow instanceof FixedFlow)
      fixedFlow = (FixedFlow) flow;
    else if (flow instanceof CapabilityLanguageFlow)
      capabilityLanguageFlow = (CapabilityLanguageFlow) flow;
    else
      ;// can be null if omitted
  }

  public String[] getFlow() {
    if (fixedFlow != null)
      return fixedFlow.getFixedFlow();
    if (capabilityLanguageFlow != null)
      return capabilityLanguageFlow.getCapabilityLanguageFlow();
    return null;
    // throw new InternalErrorCDE("invalid state");
  }

  public void setFlow(String[] newFlow) {
    if (fixedFlow != null) {
      fixedFlow.setFixedFlow(newFlow);
      return;
    }
    if (capabilityLanguageFlow != null) {
      capabilityLanguageFlow.setCapabilityLanguageFlow(newFlow);
      return;
    }
    // throw new InternalErrorCDE("invalid state");
    return; // ignore in null case
  }
}
