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

import java.util.ArrayList;

import org.apache.uima.resource.metadata.MetaDataObject;

/**
 * An object containing configuration for a program that the CPE will use to launch CasProcessor. It
 * provides the means to define an executable program and its arguments
 * 
 * 
 */
public interface CasProcessorExecutable extends MetaDataObject {
  /**
   * Sets an executable program that the CPE will use for launching CasProcessor.
   * 
   * @param aExecutable -
   *          program name (like java.exe)
   */
  public void setExecutable(String aExecutable);

  /**
   * Returns an executable program that the CPE will use for launching CasProcessor.
   * 
   * @return - exec program as String
   */
  public String getExecutable();

  /**
   * Adds a {@link org.apache.uima.collection.metadata.CasProcessorExecArg} argument to be supplied
   * when launching a program.
   * 
   * @param aArgs -
   *          argument for the executable program
   */
  public void addCasProcessorExecArg(CasProcessorExecArg aArgs);

  /**
   * Returns a {@link org.apache.uima.collection.metadata.CasProcessorExecArg} argument identified by
   * a given position in the list.
   * 
   * @param aIndex -
   *          position of argument to return
   * @return {@link org.apache.uima.collection.metadata.CasProcessorExecArg} argument
   */
  public CasProcessorExecArg getCasProcessorExecArg(int aIndex);

  /**
   * Returns ALL {@link org.apache.uima.collection.metadata.CasProcessorExecArg} arguments
   * 
   * @return array of {@link org.apache.uima.collection.metadata.CasProcessorExecArg}
   */
  public CasProcessorExecArg[] getAllCasProcessorExecArgs();

  /**
   * Removes program argument from the list. The argument for deletion is identified by provided
   * position in the list.
   * 
   * @param aIndex -
   *          position of argument to delete
   */
  public void removeCasProcessorExecArg(int aIndex);

  public ArrayList<CasProcessorRuntimeEnvParam> getEnvs();

  /**
   * @param params the CAS Processor Runtime Environment parameters
   */
  public void setEnvs(ArrayList<CasProcessorRuntimeEnvParam> params);

}
