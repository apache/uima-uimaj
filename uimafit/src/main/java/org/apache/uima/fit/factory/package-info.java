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
/**
 * Factories to create different kinds of UIMA resource specifiers.
 * 
 * <h3><a name="InstancesVsDescriptors">Why are descriptors better than component instances?</a></h3>
 * 
 * It is recommended to avoid instantiating components with uimaFIT outside of a running pipeline,
 * unless necessary and unless you are aware of the consequences.
 * 
 * When run within a pipeline, such as {@link org.apache.uima.fit.pipeline.SimplePipeline} or
 * within a Collection Processing Engine, the pipeline logic takes care of invoking the life-cycle
 * methods on a component, such as:
 * 
 * <ul>
 * <li>initialize</li>
 * <li>collectionProcessComplete</li>
 * <li>destroy</li>
 * <li>...</li>
 * </ul>
 * 
 * When components are created manually, it is the responsability of the caller to explicitly invoke
 * the life-cycle methods. The only method that uimaFIT may call is <em>initialize</em> to provide
 * an {@link org.apache.uima.UimaContext} with the desired parametrization of the component. 
 * 
 * Not letting UIMA/uimaFIT manage the life-cycle of a component can, thus, have some unexpected
 * effects. For example, a {@link org.apache.uima.collection.CollectionReader} cannot be reused
 * after it has been passed to a {@link org.apache.uima.fit.pipeline.SimplePipeline#runPipeline(org.apache.uima.collection.CollectionReader, org.apache.uima.analysis_engine.AnalysisEngine...)}.
 * The pipeline reads all files from the reader instance, and when it is complete, the reader does
 * not have any more data to produce. Passing the reader to subsequent {@code runPipeline} methods will not
 * produce any results. When a {@link org.apache.uima.collection.CollectionReaderDescription}
 * is passed instead, the reader is created, initalized, and destroyed inside the
 * {@code runPipeline} method. The description can be passed to multiple {@code runPipeline} calls
 * and each time, it will behave the same way, producing all its data.
 */
package org.apache.uima.fit.factory;

