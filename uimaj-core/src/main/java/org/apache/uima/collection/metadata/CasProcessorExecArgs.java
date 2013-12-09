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

import java.io.Serializable;

/**
 * An object containing all {@link org.apache.uima.collection.metadata.CasProcessorExecArg}
 * instances. It provides the means of adding new program arguments, retrieving them, and removing
 * them.
 */
public interface CasProcessorExecArgs extends Serializable// extends MetaDataObject
{
  /**
   * Adds new {@link org.apache.uima.collection.metadata.CasProcessorExecArg} instance to the list.
   * 
   * @param aArg -
   *          new argument
   */
  public void add(CasProcessorExecArg aArg);

  /**
   * Returns an {@link org.apache.uima.collection.metadata.CasProcessorExecArg} instance located
   * with provided index.
   * 
   * @param aIndex -
   *          position of argument in the list
   * 
   * @return - {@link org.apache.uima.collection.metadata.CasProcessorExecArg} instance
   * @throws CpeDescriptorException tbd
   */
  public CasProcessorExecArg get(int aIndex) throws CpeDescriptorException;

  /**
   * Returns ALL {@link org.apache.uima.collection.metadata.CasProcessorExecArg} instances.
   * 
   * @return array of {@link org.apache.uima.collection.metadata.CasProcessorExecArg}
   */
  public CasProcessorExecArg[] getAll();

  /**
   * Removes {@link org.apache.uima.collection.metadata.CasProcessorExecArg} instance found in the
   * list in a given position.
   * 
   * @param aIndex -
   *          position of argument to remove.
   */
  public void remove(int aIndex);
}
