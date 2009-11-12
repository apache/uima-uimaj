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

package org.apache.uima.analysis_engine;

/**
 * An <code>AnalysisEngineDescription</code> specific to a Text Analysis Engine (TAE).
 * 
 * @deprecated As of v2.0, {@link AnalysisEngineDescription} should be used instead. This interface
 *             no longer defines any additional methods beyond those in AnalysisEngineDescription.
 */
@Deprecated
public interface TaeDescription extends AnalysisEngineDescription {
  // As of 2.0, no longer defines any additional methods beyond those in AnalysisEngineDescription
}
