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

package org.apache.uima.collection.metadata;

/**
 * An object that holds configuration that is part of the CPE descriptor. Provides the means of
 * defining and obtaining configuration for CasProcessors deployed locally, ie same machine as CPE
 * but different process.
 * <p>
 * Its basic functionality is inherited from
 * {@link org.apache.uima.collection.metadata.CpeCasProcessor}
 * 
 * 
 */
public interface CpeLocalCasProcessor extends CpeCasProcessor {
  /**
   * Returns true if the local Cas Processor is a java program
   * 
   * @return true if java program, false otherwise
   * @throws CpeDescriptorException -
   */
  public boolean isJava() throws CpeDescriptorException;

  /**
   * Defines if this Cas Processor is a java program
   * 
   * @param aJava -
   *          true if java, false otherwise
   * @throws CpeDescriptorException -
   */
  public void setIsJava(boolean aJava) throws CpeDescriptorException;

  /**
   * Defines the name of a program to run when launching this Cas Processor. A program can be a java
   * exec, a shell script, or any program that can run in a separate process.
   * 
   * @param aCasProcessorExecutable -
   *          name of a program
   * 
   * @throws CpeDescriptorException tbd
   */
  public void setExecutable(String aCasProcessorExecutable) throws CpeDescriptorException;

  /**
   * Returns a name of a program to use when launching this CasProcessor
   * 
   * @return the name of the program to execute
   * @throws CpeDescriptorException tbd
   */
  public String getExecutable() throws CpeDescriptorException;

  public void addExecEnv(String aEnvParamName, String aEnvParamValue) throws CpeDescriptorException;

  public void addExecArg(String aArgValue) throws CpeDescriptorException;

  public void setRunInSeperateProcess(CasProcessorRunInSeperateProcess aSepProcess)
          throws CpeDescriptorException;

  public CasProcessorRunInSeperateProcess getRunInSeperateProcess() throws CpeDescriptorException;

}
