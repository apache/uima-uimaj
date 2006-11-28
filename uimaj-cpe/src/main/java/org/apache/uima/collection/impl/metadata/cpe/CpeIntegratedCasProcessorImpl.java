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

package org.apache.uima.collection.impl.metadata.cpe;

import org.apache.uima.collection.metadata.CasProcessorDeploymentParams;
import org.apache.uima.collection.metadata.CpeIntegratedCasProcessor;

public class CpeIntegratedCasProcessorImpl extends CasProcessorCpeObject implements
        CpeIntegratedCasProcessor {
  private static final long serialVersionUID = 6076012896926381047L;

  public CpeIntegratedCasProcessorImpl()

  {
    super();
    try {
      addDefaults();
      super.setDeployment("integrated");
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  protected void addDefaults() {
    try {
      CasProcessorDeploymentParams deployParams = CpeDescriptorFactory.produceDeployParams();
      super.setDeploymentParams(deployParams);
      super.addDefaults();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
