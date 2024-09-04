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
 * Describes the format of the generated JCas cover classes for UIMA Version 3.
 * <p>In UIMA version 3</p>
 * <ul>
 * <li>There are no xxx_Type classes.</li>
 * <li>As in v2, multiple type systems may be sequentially loaded into a (reset) CAS, under one class loader.</li>
 * <li>Change: one consistent JCas gen'd set of classes is loaded per class loader.
 * </li>
 * </ul>
 */
package org.apache.uima.tools.jcasgen;