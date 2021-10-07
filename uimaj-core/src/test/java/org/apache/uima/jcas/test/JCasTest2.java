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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.PrimitiveIterator.OfInt;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.LowLevelIndexRepository;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.EmptyFloatList;
import org.apache.uima.jcas.cas.EmptyIntegerList;
import org.apache.uima.jcas.cas.EmptyStringList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.FloatList;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.IntegerArrayList;
import org.apache.uima.jcas.cas.IntegerList;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.NonEmptyFloatList;
import org.apache.uima.jcas.cas.NonEmptyIntegerList;
import org.apache.uima.jcas.cas.NonEmptyStringList;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.junit.After;
import org.junit.Test;

import aa.ConcreteType;
import aa.Root;
import junit.framework.TestCase;
import x.y.z.EndOfSentence;
import x.y.z.Sentence;
import x.y.z.Token;

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
public class JCasTest2 {

	private CAS cas;

	private JCas jcas;

	private TypeSystem ts;

/**
 * This is a work in progress
 */
}
