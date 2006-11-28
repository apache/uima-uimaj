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

package org.apache.uima.collection.impl.base_cpm.container;

import java.io.IOException;
import java.util.HashMap;

import org.apache.uima.collection.base_cpm.CasProcessor;
import org.apache.uima.collection.impl.base_cpm.container.deployer.CasProcessorDeployer;
import org.apache.uima.collection.impl.cpm.container.ServiceProxyPool;
import org.apache.uima.resource.ConfigurableResource;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.Resource_ImplBase;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;

/**
 * Base interface for implementing a Container object responsible for managing Cas Processor
 * instances at runtime. Aggregates stats and totals and helps with error recovery.
 */
public abstract class ProcessingContainer extends Resource_ImplBase implements
        ConfigurableResource, CasProcessorController {
  public abstract boolean processCas(Object[] aCas);

  public abstract void releaseCasProcessor(CasProcessor aCasProcessor);

  public abstract String getName();

  public abstract boolean isEndOfBatch(CasProcessor aCasProcessor, int aProcessedSize)
          throws ResourceProcessException, IOException;

  public abstract boolean abortCPMOnError();

  public abstract void incrementCasProcessorErrors(Throwable aThrowable) throws Exception;

  public abstract CasProcessorConfiguration getCasProcessorConfiguration();

  public abstract long getProcessed();

  public abstract void setProcessed(long aProcessedCount);

  public abstract long getBytesIn();

  public abstract void addBytesIn(long aBytesIn);

  public abstract long getBytesOut();

  public abstract void addBytesOut(long aBytesOut);

  public abstract void incrementRestartCount(int aCount);

  public abstract int getRestartCount();

  public abstract void incrementRetryCount(int aCount);

  public abstract int getRetryCount();

  public abstract void incrementAbortCount(int aCount);

  public abstract int getAbortCount();

  public abstract void incrementFilteredCount(int aCount);

  public abstract int getFilteredCount();

  public abstract long getRemaining();

  public abstract void setRemaining(long aRemainingCount);

  public abstract void setLastProcessedEntityId(String entityId);

  public abstract String getLastProcessedEntityId();

  public abstract void incrementTotalTime(long aTime);

  public abstract long getTotalTime();

  public abstract void logAbortedCases(Object[] abortedCasList);

  public abstract void setLastCas(Object aCasObject);

  public abstract Object getLastCas();

  public abstract void setCasProcessorDeployer(CasProcessorDeployer aDeployer);

  public abstract CasProcessorDeployer getDeployer();

  public abstract void setMetadata(ProcessingResourceMetaData aMetadata);

  public abstract HashMap getAllStats();

  public abstract void addStat(String aStatName, Object aStat);

  public abstract void incrementStat(String aStatName, Integer aStat);

  public abstract Object getStat(String aStatName);

  public abstract void resetRestartCount();

  public abstract void pause();

  public abstract void resume();

  public abstract boolean isPaused();

  public abstract ServiceProxyPool getPool();

  public abstract void setSingleFencedService(boolean aSingleFencedInstance);

  public abstract boolean isSingleFencedService();
}
