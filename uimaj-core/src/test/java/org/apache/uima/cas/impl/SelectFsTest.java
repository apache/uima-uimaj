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

package org.apache.uima.cas.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.junit.BeforeClass;
import org.junit.Test;

import x.y.z.Token;

public class SelectFsTest  {

  private static TypeSystemDescription typeSystemDescription;
  
  static private CASImpl cas;

  static File typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem_token_no_features.xml"); 
  
  @BeforeClass
  public static void setUpClass() throws Exception {
    typeSystemDescription  = UIMAFramework.getXMLParser().parseTypeSystemDescription(
        new XMLInputSource(typeSystemFile1));
    cas = (CASImpl) CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(), null);    
  }
  
  
  @Test
  public void testSelect_asList() {
  
    JCas jcas = cas.getJCas();
    
    Token p1 = new Token(jcas, 0, 1); 
    p1.addToIndexes();

    Token p2 = new Token(jcas, 1, 2);
    p2 .addToIndexes();

    Token c1 = new Token(jcas, 2, 3);
    c1.addToIndexes();

    new Token(jcas, 3, 4).addToIndexes();

    new Token(jcas, 4, 5).addToIndexes();

    Token p3 = new Token(jcas, 1, 3);
    p3.addToIndexes();
    
    Token c = jcas.select(Token.class).at(2, 3).get(0);
    assertTrue(c == c1);
    
    /* preceding -> backwards iteration, starting at annot whose end <= c's begin,
     *                therefore starts  
     */
    Iterator<Token> it = jcas.select(Token.class).preceding(c).iterator();
    assertTrue(it.hasNext()); 
    Token x = it.next();
    assertTrue(x == p2);
    assertTrue(it.hasNext()); 
    x = it.next();
    assertTrue(x == p1);
    assertFalse(it.hasNext()); 
    
    
    List<Token> preceedingTokens = jcas.select(Token.class).preceding(c).limit(2).asList();
    
    assertEquals(2, preceedingTokens.size());
    assertTrue(preceedingTokens.get(0) == p2);
    assertTrue(preceedingTokens.get(1) == p1);
     
  }
}
