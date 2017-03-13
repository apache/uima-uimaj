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

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.collection.impl.cpm.utils.Execute;
import org.apache.uima.resource.ResourceConfigurationException;

/**
 * 
 * Interface for setter and getter methods for the Cas Processor configuration as defined in the CPE
 * descriptor. Through these methods an implmentation gets access to individual Cas Processor
 * configuration settings.
 */
public interface CasProcessorConfiguration {
  public String getName();

  public long getTimeout();

  public boolean runInSeparateProcess();

  public boolean isJavaProcess();

  public int getErrorRate();

  public int getMaxErrorCount();

  public long getErrorSampleSize();

  public String getActionOnError();

  public String getActionOnMaxRestart();

  public int getMaxRestartCount();

  public int getMaxRetryCount();

  public List getDeploymentParameters();

  public String getDeploymentParameter(String aDeployParameter);

  public String getDeploymentType();

  public String getFilterString();

  public LinkedList getFilter() throws ResourceConfigurationException;

  public String[] getKeysToDrop() throws ResourceConfigurationException;

  public int getBatchSize();

  public URL getDescriptorUrl() throws ResourceConfigurationException;

  public Execute getExecSpec();

  public int getMaxTimeToWaitBetweenRetries();

}
