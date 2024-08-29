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
 * <p>Common Analysis System(CAS) Interfaces</p>
 * <h2>Common Analysis System (CAS) Interfaces</h2>
 * <p>The <a href="CAS.html">CAS</a> provides</p>
 * <ul>
 * <li>a set of methods for creating Feature Structures and setting / getting their Feature values, based on parameters referencing Types and Features.</li>
 * <li>a link to the <a href="TypeSystem.html">type system</a> being used</li>
 * <li>a container for the set of one or more "Views" - each view corresponding to a separate set of indexes, contained in a <a href="FSIndexRepository.html">index repository</a>. These indexes can be used to retrieve the Feature Structures that have already been created.
 * <ul>
 * <li>For each view:
 * <ul>
 * <li>a link to the indexes used to index Feature Structures</li>
 * <li>information about that view's (optional) Subject of Analysis (SofA).</li>
 * <li>convenience methods for adding Feature Structures to the view's indexes</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * </ul>
 * <h3>Type System</h3>
 * <p>The Type System is a collection of types and features of each type, where the types are in a single type hierarchy. The type/feature information is collected from possibly multiple annotators that make up a UIMA pipeline, and the definitions are merged.</p>
 * <h2>Index Repository</h2>
 * <p>Indexes provide a way to access those Feature Structures which have been indexed (added to the index, and not subsequently removed). Each CAS view has a separate set of indexes.</p>
 * <ul>
 * <li>FSIndexRepository - UIMA pipelines specify a set of index definitions to be used; these definitions are used for all views. In addition to user-specified indexes, there are two built-in indexes: the Annotation Index, and a default "bag" index that is used whenever no other index is defined, to enable retrieval of all indexed Feature Structures. When users add instances to the indexes, they do so for the indexes in just one view. Users may choose to index the same Feature Structure in multiple views, with one restriction: Feature Structures which are subtypes of AnnotationBase may only be added to the view where the Feature Structure was created. The FSIndexRepository instance per view allows access to the Feature Structures indexed in that view.</li>
 * <li>FSIndex - represents a particular index over a type and its subtypes. There are three underlying kinds of indexes: Bag, Set, and Sorted. The Set and Sorted include a "comparator" which defines a compare order which is also used as the definition of "equal" for Sets.</li>
 * </ul>
 * <h3>Built-in Feature Structure classes</h3>
 * <p>This package holds the definition for many of the built-in Feature Structures.</p>
 * <p>The following classes are alternate interfaces for built-in Feature Structures; they remain for backwards compatibility.org.apache.uima.cas</p>
 * <table style="height: 247px;" border="1" >
 * <caption>alternate interfaces</caption>
 * <tbody>
 * <tr>
 * <td style="text-align: center;">UIMA v2 name<br>org.apache.uima.cas</td>
 * <td style="text-align: center;">preferred<br>org.apache.uima.jcas.cas</td>
 * </tr>
 * <tr>
 * <td>BooleanArrayFS</td>
 * <td>BooleanArray</td>
 * </tr>
 * <tr>
 * <td>ByteArrayFS</td>
 * <td>ByteArray</td>
 * </tr>
 * <tr>
 * <td>ShortArrayFS</td>
 * <td>ShortArray</td>
 * </tr>
 * <tr>
 * <td>IntegerArrayFS</td>
 * <td>IntegerArray</td>
 * </tr>
 * <tr>
 * <td>FloatArrayFS</td>
 * <td>FloatArray</td>
 * </tr>
 * <tr>
 * <td>LongArrayFS</td>
 * <td>LongArray</td>
 * </tr>
 * <tr>
 * <td>DoubleArrayFS</td>
 * <td>DoubleArray</td>
 * </tr>
 * <tr>
 * <td>SofaFS</td>
 * <td>Sofa</td>
 * </tr>
 * <tr>
 * <td>AnnotationBaseFS</td>
 * <td>AnnotationBase</td>
 * </tr>
 * </tbody>
 * </table>
 * <h3>Constraints - used by filtered iterators</h3>
 * <p>Iterators may be filtered, using constraints, specified using these interfaces.</p>
 * <ul>
 * <li>ConstraintFactor</li>
 * <li>FeaturePath</li>
 * <li>FeatureValuePath</li>
 * <li>FSBooleanConstraint</li>
 * <li>FSConstraint</li>
 * <li>FSFloatConstraint</li>
 * <li>FSIntConstraint</li>
 * <li>FSMatchConstraint</li>
 * <li>FSStringConstraint</li>
 * <li>FSTypeConstraint</li>
 * </ul>
 * <h3>Exception collections</h3>
 * <p>Many of the exceptions that UIMA may throw are collected into groups here. These classes provide one level of indirection that permit IDE environments to conveniently locate and work with these.</p>
 * <table>
 * <caption>exceptions</caption>
 * <tbody>
 * <tr>
 * <td>&nbsp;</td>
 * <td style="text-align: center;">Extends</td>
 * </tr>
 * <tr>
 * <td>CASException</td>
 * <td>UIMAException (checked)</td>
 * </tr>
 * <tr>
 * <td>CASRuntimeException</td>
 * <td>UIMARuntimeException</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>&nbsp;</p>
 */
package org.apache.uima.cas;