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
 * Implementation and Low-Level API for the CAS Interfaces.
 * <p>
 * These are Internal APIs. Use these APIs at your own risk. APIs in this package are subject to
 * change without notice, even in minor releases. Use of this package is not supported. If you think
 * you have found a bug in this package, please try to reproduce it with the officially supported
 * APIs before reporting it.
 * </p>
 * <hr>
 * <h2>Internals documentation</h2>
 * <p>
 * NOTE: This documentation is plain HTML, generated from a WYSIWIG editor "tinymce".&nbsp;&nbsp;
 * The way to work on this:&nbsp; after setting up a small web page with the tinymce (running from a
 * local file), use the Tools - source code to cut/paste between this file's source and that editor.
 * </p>
 * <h3>Java Cover Objects for version 3</h3>
 * <p>
 * The Java Cover Objects are no longer cover objects; instead, these objects <b>are</b> the Feature
 * Structures. &nbsp;The Java classes for these objects are in a hierarchy that corresponds to the
 * UIMA type hierarchy. &nbsp;JCasGen continues to serve to generate (for user, not for built-in
 * types) particular Java Classes for particular UIMA Types. &nbsp;And, as before, JCasGen'd classes
 * are optional. &nbsp;If there was not a JCasGen'd class for "MyType" (assume a subtype of
 * "Annotation"), then the most specific supertype of "MyType" which has a particular corresponding
 * Java cover class, is used. &nbsp;(This is how it works in V2, also).&nbsp;
 * </p>
 * <p>
 * There is one definition of these objects per UIMA Type System. &nbsp;Support for PEARs having
 * different "customizations" of the same JCas classname is not supported in v3.
 * </p>
 * <ul>
 * <li>This loss of capability is mitigated by the addition of more kinds of Java types as built-in
 * values.</li>
 * <li>The reason for this not being supported is that there's no solution figured out for sharing
 * types between the outer and PEAR pipelines, without encountering class-cast exceptions.</li>
 * <li>The PEAR can still define customizations for types only it defines (that is, not used by the
 * outer pipeline).</li>
 * </ul>
 * <p>
 * Much of the infrastructure is kept as-is in version 3 to support backwards compatibility.
 * </p>
 * <h4>Format of a JCas class version 3</h4>
 * <p>
 * The _Type is not used. &nbsp;May revisit this if users are using the low-level access made
 * possible by _Type.
 * </p>
 * <p>
 * There is one definition of the class per type system. &nbsp;Type systems are often shared among
 * multiple CASes. &nbsp;Each definition is loaded under a specific loader for that type system.
 * &nbsp;
 * </p>
 * <p>
 * (Not implemented) <span style="text-decoration: line-through;">The loader is set up to delegate
 * to the parent for all classes except the JCas types, and for those, it generates them using ASM
 * byte code generation from the fully merged TypeSystem information and existing
 * "customizations".</span>
 * </p>
 * <p>
 * Each feature is stored in one of two arrays, kept per Java Object Feature Structure Instance: an
 * "int" array, holding boolean/byte/short/int/long/float/double values, and a "Object" array
 * holding strings/refs-to-other-FSs. &nbsp;Longs and Doubles take 2 int slots.
 * </p>
 * <p>
 * Built-in arrays have their array parts represented by native Java Arrays. &nbsp;Getters and
 * Setters are provided as before. &nbsp;Constructors are provided as before.
 * </p>
 * <p>
 * Extra fields in the Feature Structure include both instance and class fields:
 * </p>
 * <ul>
 * <li>(static class fields) a set of fields representing the int offset in the "int" and "object"
 * arrays for all the features</li>
 * <li>(instance field) a reference to the TypeImpl for this class - initialized by a reference to a
 * TypeSystemImpl thread local value, at load time. &nbsp;This is updatable to handle two edge
 * cases.</li>
 * <li>(instance field) a reference to the CAS View used when this feature structure was
 * created</li>
 * </ul>
 * <p>
 * Extra methods in the FeatureStructure
 * </p>
 * <ul>
 * <li>a set of generic getters and setters, one per incompatible value type.
 * <ul>
 * <li>All references to non-primitive FeatureStructures values are collapsed into a single TOP
 * ref.</li>
 * <li>These are used for generic access, including serialization/deserialization</li>
 * <li>more: see
 * <a href="../../../uimaj-tools/src/main/java/org/apache/uima/tools/jcasgen/package.html">
 * package.html for uimaj-tools jcasgen</a> (link only works if all sources checked out)</li>
 * </ul>
 * </li>
 * </ul>
 * <h3>UIMA Indexes</h3>
 * <p>
 * Indexes are defined for a pipeline, and are kept as part of the general CAS definition.
 * </p>
 * <p>
 * Each CAS View has its own instantiation of the defined indexes (there's one definition for all
 * views), and as a result, a particular FS may be added-to-indexes and indexed in some views, and
 * not in others.
 * </p>
 * <p>
 * There are 3 kinds of indexes: Sorted, Set, and Bag.&nbsp; The basic object type for an index is
 * <code>FsIndex_singleType</code>. This has 3 subtypes, one for each of the index types:
 * </p>
 * <ul>
 * <li>FsIndex_bag</li>
 * <li>FsIndex_set_sorted (used for both Sets and Sorted indexes</li>
 * <li>FsIndex_flat (used for flattened indexes, for instance, with snapshot iterators)</li>
 * </ul>
 * <p>
 * The FsIndex_singleType index is just for one type (and doesn't include entries for any subtypes).
 * </p>
 * <p>
 * The Set and Sorted implementations are combined; the only difference is in the comparator used.
 * &nbsp;For sets, the comparator is what the index definition specifies. &nbsp;For sorted, the
 * specified comparator is augmented with an least significant extra key which is the Feature
 * Structure id.
 * </p>
 * <p>
 * Indexes are connected to specific index definitions; these definitions include a type which is
 * the top type for elements of this index. The index definition logically includes that type and
 * all of its subtypes.
 * </p>
 * <p>
 * An additional data struction, the IndexIteratorCachePair, is associated with each index
 * definition.&nbsp; It holds references to the subtype
 * FsIndex_singleType&nbsp;implementations&nbsp;for all subtypes of an index; this list is created
 * lazily, only when an iterator is created over this index at a particular type level (which can be
 * the type the index was defined for, or any subtype).&nbsp; This lazy aspect is important, because
 * UIMA is often used in cases where there's a giant type system, with lots of subtypes, only a few
 * of which are used in a particular pipeline instance.
 * </p>
 * <p>
 * There are two tasks that indexes accomplish:
 * </p>
 * <ul>
 * <li>updating the index with adds and removes of FSs.&nbsp; This update operation is optimized by
 * <ul>
 * <li>keeping each type indexed separately, so only that data structure for the particular type
 * need be updated (this design choice has a cost in iteration, though)</li>
 * <li>treating more common use cases efficiently - the main one being that of adding something "to
 * the end" of the items in the index.</li>
 * </ul>
 * </li>
 * <li>iterating over an index for a type and its subtypes.&nbsp;
 * <ul>
 * <li>For indexes having no subtypes, this is done by iterating over the FSLeafIndexImpl for that
 * index and type.&nbsp;</li>
 * <li>For indexing with subtypes, this is done by creating individual iterators for the type and
 * all of its subtypes, each iterating over the FSLeafIndexImpl for that type.&nbsp; These iterators
 * are then logically combined into one iterator.</li>
 * </ul>
 * </li>
 * </ul>
 * <h2>Iterators</h2>
 * <p>
 * There are two main kinds of iterators:
 * </p>
 * <ul>
 * <li>Iterators over UIMA Indexes</li>
 * <li>Iterators over other UIMA objects, such as Views, or internal structures.</li>
 * </ul>
 * <h3>Iterators over UIMA indexes</h3>
 * <p>
 * There are two main kinds of iterators over UIMA indexes:
 * </p>
 * <ul>
 * <li>those returning Java cover objects representing the FS.</li>
 * <li>those returning int values representing the location of the FS in the heap. &nbsp;These are
 * the so-called low level iterators; they are less efficient in V3.&nbsp;&nbsp;</li>
 * </ul>
 * <p>
 * The basic iterator over a single type is implemented by <strong>FsIterator_singletype</strong>.
 * &nbsp;This has
 * subtypes&nbsp;<strong>FsIterator_bag&nbsp;</strong>and&nbsp;<strong>FsIterator_set_sorted</strong>.
 * </p>
 * <p>
 * &nbsp;
 * </p>
 * <p>
 * &nbsp;
 * </p>
 */
package org.apache.uima.cas.impl;