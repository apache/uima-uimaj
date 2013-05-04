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
 * Utility classes like {@link org.apache.uima.fit.util.CasUtil} and {@link org.apache.uima.fit.util.JCasUtil}.
 * 
 * <h3><a name="SortOrder">Sort order</a></h3>
 * 
 * The various <em>select</em> methods in {@link org.apache.uima.fit.util.CasUtil} and 
 * {@link org.apache.uima.fit.util.JCasUtil} rely on the UIMA feature structure indexes. Their
 * behaviour differs depending on the type of feature structure being selected and where they are
 * selected from:
 * 
 * <h4>Selecting from a {@link org.apache.uima.cas.CAS}/{@link org.apache.uima.jcas.JCas}</h4>
 * <ol>
 * <li><b>Annotations</b> - if the type being selected is {@link org.apache.uima.jcas.tcas.Annotation}
 * or a sub-type thereof, the built-in annotation index is used. This index has the keys 
 * <em>begin</em> (Ascending), <em>end</em> (Descending) and <em>TYPE_PRIORITY</em>. There are no
 * built-in type priorities, so this last sort item does not play a role in the index unless 
 * type priorities are specified. uimaFIT uses {@link org.apache.uima.cas.CAS#getAnnotationIndex(org.apache.uima.cas.Type)}
 * to access annotation feature structures.</li>
 * <li><b>Other feature structures</b> - if feature structures are selected that are not 
 * {@link org.apache.uima.jcas.tcas.Annotation annotations}, the order should be considered
 * undefined. uimaFIT uses {@link org.apache.uima.cas.FSIndexRepository#getAllIndexedFS(org.apache.uima.cas.Type)}
 * to access these feature structures.</li>
 * </ol>
 * 
 * <h4>Selecting from a {@link org.apache.uima.cas.ArrayFS ArrayFS/FSArray}/{@link FSList}</h4>
 * 
 * When selecting from a feature structure list or array, the order is determined by the order of
 * the annotations inside the list/array.
 */
package org.apache.uima.fit.util;

