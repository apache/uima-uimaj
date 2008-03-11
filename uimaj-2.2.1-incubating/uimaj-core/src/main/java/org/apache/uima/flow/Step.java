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

package org.apache.uima.flow;

/**
 * Represents the next destination or destinations to which a CAS should be routed. A
 * <code>Step</code> is the output of the {@link Flow#next()} method. A Flow should output an
 * instance of a concrete class that extends Step. Currently, these are:
 * <ul>
 * <li>{@link SimpleStep} - specifies a single AnalysisEngine to which the CAS should next be
 * routed</li>
 * <li>{@link ParallelStep} - specifies multiple AnalysisEngine to which the CAS should next be
 * routed, where the relative order in which these Analysis Engines execute does not matter.</li>
 * <li>{@link FinalStep} - indicates that there are no more destinations for this CAS.</li>
 * </ul>
 */
public abstract class Step {
}
