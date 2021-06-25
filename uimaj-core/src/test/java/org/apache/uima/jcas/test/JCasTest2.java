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

package org.apache.uima.jcas.test;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.jcas.JCas;

//@formatter:off
/**
 * Test for adjusted offset computation between varieties of JCas implementations and type systems.
 *
 *  The supertype hierarchy for JCas and Type System:
 *      1
 *     /\ 
 *    2  3
 *   /\ 
 *  4  5
 *    /|\    
 *   6 7 8
 *     |
 *     9
 *     |
 *     10
 * 
 *  This test tests for correct operation when one of (JCas, Type System) has the above full hierarchy, and the other
 *  implements a partial view.
 * 
 *     1
 *     2
 *     5
 *     10
 * 
 *  The intent is to support use cases:
 *     Within one class loader (loading JCas classes), 
 *       - the JCas classes are missing many types
 *           -- the type system loaded with these defines various subsets of the types, but
 *               --- starting with the type system with the most types and features
 *       - the JCas class impl all the types, 
 *           -- the type system loaded with these defines various subsets of the types
 *               --- not starting with the type system with the most types and features
 *                   (types/features should be picked up from reflection on JCas classes;
 *                    no support for new types or features not defined in the JCas hierarchy)
 * 
 */
//@formatter:on
public class JCasTest2 {

  private CAS cas;

  private JCas jcas;

  private TypeSystem ts;

  /**
   * This is a work in progress
   */
}
