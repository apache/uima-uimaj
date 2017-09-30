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

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;

/**
 * Class comment for IteratorTest.java goes here.
 * 
 */
public class SubiteratorTest extends TestCase {

  private AnalysisEngine ae = null;

  public SubiteratorTest(String arg0) {
    super(arg0);
  }

  public void setUp() {
    File descriptorFile = JUnitExtension.getFile("CASTests/desc/TokensAndSentences.xml");
    assertTrue("Descriptor must exist: " + descriptorFile.getAbsolutePath(), descriptorFile
        .exists());

    try {
      XMLParser parser = UIMAFramework.getXMLParser();
      ResourceSpecifier spec = (ResourceSpecifier) parser.parse(new XMLInputSource(descriptorFile));
      this.ae = UIMAFramework.produceAnalysisEngine(spec);
    } catch (IOException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (InvalidXMLException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (ResourceInitializationException e) {
      e.printStackTrace();
      assertTrue(false);
    }

  }

  public void tearDown() {
    if (this.ae != null) {
      this.ae.destroy();
      this.ae = null;
    }
  }

  public void testAnnotator() {
    File textFile = JUnitExtension.getFile("CASTests/verjuice.txt");
    String text = null;
    try {
      text = FileUtils.file2String(textFile, "utf-8");
    } catch (IOException e) {
      e.printStackTrace();
      assertTrue(false);
    }
    JCas jcas = null;
    try {
      jcas = this.ae.newJCas();
    } catch (ResourceInitializationException e) {
      e.printStackTrace();
      assertTrue(false);
    }
    jcas.setDocumentText(text);
    try {
      this.ae.process(jcas);
      
      iterateAndcheck(jcas);
          
      iterateAndcheck(jcas);
    } catch (AnalysisEngineProcessException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (ClassCastException e) {
      // UIMA-464: Subiterator.moveTo() throws ClassCastException.
      assertTrue(false);
    }
  }
  
  private void iterateAndcheck(JCas jcas) {
    AnnotationIndex<Token> tokenIndex = jcas.getAnnotationIndex(Token.class);
    Annotation sentence = jcas.getAnnotationIndex(Sentence.class).iterator().next();
    FSIterator<Token> tokenIterator = tokenIndex.subiterator(sentence);
    Annotation token = tokenIndex.iterator().next();
    // debug token.toString();
    tokenIterator.moveTo(token); //throws ClassCastException 
    
    // check unambiguous iterator creation
    
    FSIterator<Token> it = tokenIndex.iterator(false);
    it.moveTo(token);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(SubiteratorTest.class);
  }

}
