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

import java.util.Map;

import org.apache.uima.resource.metadata.MetaDataObject;

/**
 * A <code>FlowConstraints</code> object represents constraints on the order of execution of
 * delegate Analysis Engines within an aggregate Analysis Engine. In the most constrained case, this
 * is a fixed flow.
 * <p>
 * This interface implements no methods for flow constraint specification. It serves as a common
 * superclass for different ways of specifying flow constraints.
 * <p>
 * Flow constraints must refer to delegate AnalysisEngines; the <code>FlowConstraints</code>
 * object refers to these AEs using String identifiers. The <code>FlowConstraints</code> object
 * does not assign any particular meaning to these Strings - it is the user of the
 * <code>FlowConstraints</code> object that must understand how to map them to the AnalysisEngines
 * themselves. It may be desirable to remap these identifiers; FlowConstraints implementations must
 * support this via the {@link #remapIDs(Map)} method.
 * 
 * 
 */
public interface FlowConstraints extends MetaDataObject {
  /**
   * Gets the type of this <code>FlowConstraints</code> object. Each sub-interface of
   * <code>FlowConstraints</code> has its own standard type identifier String. These identifier
   * Strings are used instead of Java class names in order to ease portability of metadata to other
   * languages.
   * 
   * @return the type identifier String for this <code>FlowConstraints</code> object
   */
  public String getFlowConstraintsType();

  /**
   * Remaps the AE identifier Strings used in this FlowConstraints object. This method is
   * destructive.
   * 
   * @param aIDMap
   *          a Map with String keys (the AE identifiers currently used by this object) and String
   *          values (the new IDs). Any identifiers that do not appear in this Map will not be
   *          remapped.
   */
  public void remapIDs(Map<String, String> aIDMap);
}
