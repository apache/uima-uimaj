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

package org.apache.uima.collection.impl.cpm;

import java.util.ArrayList;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.collection.CollectionProcessingManager;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.base_cpm.CasProcessor;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;

public class CPMImpl extends BaseCPMImpl implements CollectionProcessingManager {
  private CasConsumer consumers[];

  public CPMImpl() throws Exception {
    this(UIMAFramework.newDefaultResourceManager());
  }

  public CPMImpl(ResourceManager aResourceManager) throws Exception {
    super(null, null, aResourceManager);
  }

  
  public AnalysisEngine getAnalysisEngine() {
    if (super.getCasProcessors()[0] != null) {
      try {
        if (super.getCasProcessors()[0] instanceof AnalysisEngine) {
          return (AnalysisEngine) super.getCasProcessors()[0];
        }
      } catch (ClassCastException cE) {
        cE.printStackTrace();
        return null;
      }
    }
    return null;
  }

  
  public void setAnalysisEngine(AnalysisEngine aAnalysisEngine)
          throws ResourceConfigurationException {
    if (super.getCasProcessors().length > 0
            && super.getCasProcessors()[0] instanceof AnalysisEngine) {
      super.removeCasProcessor(super.getCasProcessors()[0]);
      super.addCasProcessor(aAnalysisEngine, 0);
    } else {
      super.addCasProcessor(aAnalysisEngine, 0);
    }
  }

  
  public CasConsumer[] getCasConsumers() {
    if (consumers != null) {
      return consumers;
    }
    ArrayList consumerList = new ArrayList();
    CasProcessor[] casProcs = getCasProcessors();
    for (int i = 0; i < casProcs.length; i++) {
      if (casProcs[i] instanceof CasConsumer) {
        consumerList.add(casProcs[i]);
      }
    }
    consumers = new CasConsumer[consumerList.size()];
    consumerList.toArray(consumers);
    return consumers;
  }

  
  public void addCasConsumer(CasConsumer aCasConsumer) throws ResourceConfigurationException {
    super.addCasProcessor(aCasConsumer);

  }

  
  public void removeCasConsumer(CasConsumer aCasConsumer) {
    super.removeCasProcessor(aCasConsumer);

  }

  
  public void addStatusCallbackListener(StatusCallbackListener aListener) {
    super.addStatusCallbackListener(aListener);

  }

  
  public void removeStatusCallbackListener(StatusCallbackListener aListener) {
    super.removeStatusCallbackListener(aListener);
  }

  
  public void process(CollectionReader aCollectionReader) throws ResourceInitializationException {
    super.process(aCollectionReader);

  }

  
  public void process(CollectionReader aCollectionReader, int aBatchSize)
          throws ResourceInitializationException {
    super.process(aCollectionReader, aBatchSize);

  }

}
