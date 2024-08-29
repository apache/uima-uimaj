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
 * Text Common Annotation System (TCAS) Interfaces.
 * <p>
 * The TCAS defines some convenience APIs for using the basic CAS system for text analysis. It
 * defines the notion of a document, and of annotations spanning parts of documents. It also
 * provides a standard way of ordering annotations, and an annotation index with respect to that
 * ordering.
 * </p>
 * <p>
 * With the Sofa related extensions, a TCAS is now a "view" of a CAS tied to a Sofa in the CAS. Most
 * of the CAS structures in a <code>TCAS</code> are references to those in the "base CAS" from which
 * it was instantiated. Each TCAS contains the Sofa it is tied to and a Index Repository that is
 * distinct from the Index Repository in the "base CAS".
 * </p>
 * <p>
 * The standard sequence for annotations is as follows. If annotation a1 starts before annotation
 * a2, then a1 is smaller than a2. If a1 and a2 start at the same place and a2 ends before a1, then
 * also a1 is smaller than a2. If a1 and a2 start and end at the same place, then they are currently
 * considered to be equal, and their sequence in the index is undefined.
 * </p>
 */
package org.apache.uima.cas.text;