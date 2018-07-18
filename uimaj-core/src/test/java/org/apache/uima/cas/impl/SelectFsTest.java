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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.junit.BeforeClass;
import org.junit.Test;

import x.y.z.Sentence;
import x.y.z.Token;

public class SelectFsTest  {

  private static TypeSystemDescription typeSystemDescription;
  
  static private CASImpl cas;

  static File typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem_token_sentence_no_features.xml"); 
  
  @BeforeClass
  public static void setUpClass() throws Exception {
    typeSystemDescription  = UIMAFramework.getXMLParser().parseTypeSystemDescription(
        new XMLInputSource(typeSystemFile1));
    cas = (CASImpl) CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(), null);    
  }
  
  
  @Test
  public void testSelect_asList() {
    cas.reset();
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
    assertTrue(x == p1);
    assertTrue(it.hasNext()); 
    x = it.next();
    assertTrue(x == p2);
    assertFalse(it.hasNext()); 
    
    
    
    List<Token> preceedingTokens = jcas.select(Token.class).preceding(c).limit(2).asList();
    
    assertEquals(2, preceedingTokens.size());
    assertTrue(preceedingTokens.get(0) == p1);
    assertTrue(preceedingTokens.get(1) == p2);
     
  }

  @Test
  public void testPrecedingAndShifted() {
    cas.reset();
    JCas jCas = cas.getJCas();
    Annotation a = new Annotation(jCas, 0, 1);
    Annotation b = new Annotation(jCas, 2, 3);
    Annotation c = new Annotation(jCas, 4, 5);

    for (Annotation ann : Arrays.asList(a, b, c)) {
      ann.addToIndexes();
    }

    // uimaFIT: Arrays.asList(a, b), selectPreceding(this.jCas, Annotation.class, c, 2));
    // Produces reverse order
    assertEquals(Arrays.asList(a, b), jCas.select(Annotation.class).preceding(c).limit(2).asList());
    // Produces: java.lang.IllegalArgumentException: Strict requires BoundsUse.coveredBy
    assertEquals(Arrays.asList(a, b), jCas.select(Annotation.class).startAt(c).shifted(-2).limit(2).asList());
  }
  @Test
  public void testBetween() {
    cas.reset();
    JCas jCas = cas.getJCas();
    Token t1 = new Token(jCas, 45, 57);
    t1.addToIndexes();
    Token t2 = new Token(jCas, 52, 52);
    t2.addToIndexes();

    new Sentence(jCas, 52, 52).addToIndexes();

    // uimaFIT: selectBetween(jCas, Sentence.class, t1, t2);
    List<Sentence> stem1 = jCas.select(Sentence.class).between(t1, t2).asList();
    assertTrue(stem1.isEmpty());
    
    t1 = new Token(jCas, 45, 52);
    stem1 = jCas.select(Sentence.class).between(t1, t2).asList();
    assertEquals(1, stem1.size());
  }
  
  @Test
  public void testBackwards() {
    cas.reset();
    JCas jcas = cas.getJCas();
    cas.setDocumentText("t1 t2 t3 t4");
    
    Token p1 = new Token(jcas, 0, 2); 
    p1.addToIndexes();

    Token p2 = new Token(jcas, 3, 5);
    p2 .addToIndexes();

    Token p3 = new Token(jcas, 6, 8);
    p3 .addToIndexes();

    Token p4 = new Token(jcas, 9, 11);
    p4 .addToIndexes();

    // uimaFIT: JCasUtil.selectByIndex(jCas, Token.class, -1).getCoveredText()
    assertEquals("t4", jcas.select(Token.class).backwards().get(0).getCoveredText());
  }
  
  @Test
  public void testempty() {
    cas.reset();
    JCas jcas = cas.getJCas();
    cas.setDocumentText("t1 t2 t3 t4");
    
    Token p1 = new Token(jcas, 0, 2); 
    p1.addToIndexes();
    assertFalse(jcas.select(Token.class).isEmpty());
    cas.reset();
    assertTrue(jcas.select(Token.class).isEmpty());
    
  }
}
