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
 * <p>
 * The Annotator Interfaces, along with supporting interfaces and exception classes.
 * </p>
 * <p>
 * The annotator interfaces are as follows:
 * <ul>
 * <li><code>{@link org.apache.uima.analysis_engine.annotator.BaseAnnotator}</code>: the base
 * interface for all annotators. Annotators should not implement this directly.</li>
 * <li><code>{@link org.apache.uima.analysis_engine.annotator.GenericAnnotator}</code>: an annotator
 * that can process any type of entity.</li>
 * <li><code>{@link org.apache.uima.analysis_engine.annotator.TextAnnotator}</code>: an annotator
 * that processes text documents.</li>
 * <li><code>{@link org.apache.uima.analysis_engine.annotator.JTextAnnotator}</code>: similar to
 * <code>TextAnnotator</code>, but uses the Java-object-based {@link org.apache.uima.jcas.JCas}
 * interface instead of the standard {@link org.apache.uima.cas.CAS} interface.</li>
 * </ul>
 */
package org.apache.uima.analysis_engine.annotator;