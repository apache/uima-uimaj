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
package org.apache.uima.cas.test;

import junit.framework.TestCase;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * 
 */
public class IndexRepositoryTest extends TestCase {

  CAS cas;

  TypeSystem typeSystem;

  FSIndexRepository indexRep;

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    this.cas = CASInitializer.initCas(new CASTestSetup());
    this.typeSystem = this.cas.getTypeSystem();
    this.indexRep = this.cas.getIndexRepository();
  }

  public void testDefaultBagIndex() throws Exception {
    // create an instance of a non-annotation type
    Type tokenTypeType = this.typeSystem.getType(CASTestSetup.TOKEN_TYPE_TYPE);
    FeatureStructure tokenTypeFs1 = this.cas.createFS(tokenTypeType);
    assertFalse(tokenTypeFs1 instanceof AnnotationFS);

    // add to indexes
    this.indexRep.addFS(tokenTypeFs1);

    // now try to retrieve
    FSIterator<FeatureStructure> iter = this.indexRep.getAllIndexedFS(tokenTypeType);
    assertTrue(iter.hasNext());
    assertEquals(tokenTypeFs1, iter.next());
    assertFalse(iter.hasNext());

    // add a second instance
    FeatureStructure tokenTypeFs2 = this.cas.createFS(tokenTypeType);
    assertFalse(tokenTypeFs2 instanceof AnnotationFS);
    this.indexRep.addFS(tokenTypeFs2);

    // now there should be two instances in the index
    FSIterator<FeatureStructure> iter2 = this.indexRep.getAllIndexedFS(tokenTypeType);
    assertTrue(iter2.hasNext());
    iter2.next();
    assertTrue(iter2.hasNext());
    iter2.next();
    assertFalse(iter.hasNext());
  }
}
