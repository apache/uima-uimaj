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

package org.apache.uima.analysis_engine.metadata;

import org.apache.uima.resource.metadata.ProcessingResourceMetaData;

/**
 * Encapsulates all of the metadata for an Analysis Engine.
 * 
 * As with all {@link org.apache.uima.resource.metadata.MetaDataObject}s, an
 * <code>AnalysisEngineMetaData</code> may or may not be modifiable. An application can find out
 * by calling the {@link #isModifiable()} method.
 * 
 * 
 */
public interface AnalysisEngineMetaData extends ProcessingResourceMetaData {

  /**
   * Determines if this AnalysisEngine supports asynchronous communication. Not yet implemented;
   * reserved for future use.
   * 
   * @return true if and only if this AnalysisEngine supports asynchronous communication
   */
  public boolean isAsynchronousModeSupported();

  /**
   * Sets whether this AnalysisEngine supports asynchronous communication. If this is set to true
   * then the AnalysisEngine should implement the <code>AsynchronousAnalysisEngine</code>
   * interface (not yet implemented).
   * 
   * @param aSupported
   *          true if and only if this AnalysisEngine supports asynchronous communication
   * 
   * @throws org.apache.uima.UIMA_UnsupportedOperationException
   *           if this <code>MetaDataObject</code> is not modifiable.
   */
  public void setAsynchronousModeSupported(boolean aSupported);

  /**
   * For an aggregate AnalysisEngine only, gets the constraints on the execution sequence of the
   * delegate AnalysisEngines within the aggregate. Flow constraints are optional. If provided they
   * may be used by the {@link org.apache.uima.flow.FlowController}, the component which ultimately
   * determines the flow.
   * <p>
   * The returned <code>FlowConstraints</code> object refers to the delegate AnalysisEngines using
   * String keys. These are the keys used to refer to the delegate AnalysisEngines in the
   * {@link org.apache.uima.analysis_engine.AnalysisEngineDescription#getDelegateAnalysisEngineSpecifiers()}
   * map.
   * 
   * @return the flow constraints for the AnalysisEngine, or <code>null</code> if no flow
   *         constraints are published by this AnalysisEngine.
   */
  public FlowConstraints getFlowConstraints();

  /**
   * For an aggregate AnalysisEngine only, sets the constraints on the execution sequence of the
   * delegate AnalysisEngines within the aggregate.Flow constraints are optional. If provided they
   * may be used by the {@link org.apache.uima.flow.FlowController}, the component which ultimately
   * determines the flow.
   * <p>
   * The returned <code>FlowConstraints</code> object refers to the delgate AnalysisEngines using
   * String keys. These are the keys used to refer to the delegate AnalysisEngines in the
   * {@link org.apache.uima.analysis_engine.AnalysisEngineDescription#getDelegateAnalysisEngineSpecifiers()}
   * map.
   * 
   * @param aFlowConstraints
   *          the flow constraints for the AnalysisEngine, or <code>null</code> if there are no
   *          flow constraints
   * 
   * @throws org.apache.uima.UIMA_UnsupportedOperationException
   *           if this <code>MetaDataObject</code> is not modifiable.
   */
  public void setFlowConstraints(FlowConstraints aFlowConstraints);

  /**
   * For an aggregate AnalysisEngine only, gets the metadata of the delegate AnalysisEngines.
   * <p>
   * Publishing this information is optional; some implementations may always return null here.
   * 
   * @return an array of delegate AnalysisEngine metadata, or <code>null</code> if that
   *         information is not available.
   */
  public AnalysisEngineMetaData[] getDelegateAnalysisEngineMetaData();
}
