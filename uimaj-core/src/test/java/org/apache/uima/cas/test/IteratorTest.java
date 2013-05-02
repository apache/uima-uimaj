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
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;

/**
 * Class comment for IteratorTest.java goes here.
 * 
 */
public class IteratorTest extends TestCase {

  private CASImpl cas;

  private TypeSystem ts;

  private Type annotationType;

  private Type stringType;

  private Type tokenType;

  private Type intType;

  private Type tokenTypeType;

  private Type wordType;

  private Feature tokenTypeFeat;

  private Feature lemmaFeat;

  private Feature sentLenFeat;

  private Feature tokenFloatFeat;

  private Feature startFeature;

  private Type sentenceType;
  
  private Type subsentenceType;

  /**
   * Constructor for FilteredIteratorTest.
   * 
   * @param arg0
   */
  public IteratorTest(String arg0) {
    super(arg0);
  }

  public void setUp() {
    // try {
    // this.cas = (CASImpl) CASInitializer.initCas(new CASTestSetup());
    // assertTrue(this.cas != null);
    // this.ts = this.cas.getTypeSystem();
    // assertTrue(this.ts != null);
    // } catch (Exception e) {
    // e.printStackTrace();
    // assertTrue(false);
    // }

    File descriptorFile = JUnitExtension.getFile("CASTests/desc/casTestCaseDescriptor.xml");
    assertTrue("Descriptor must exist: " + descriptorFile.getAbsolutePath(),
        descriptorFile.exists());

    try {
      XMLParser parser = UIMAFramework.getXMLParser();
      ResourceSpecifier spec = (ResourceSpecifier) parser.parse(new XMLInputSource(descriptorFile));
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(spec);
      this.cas = (CASImpl) ae.newCAS();
      assertTrue(this.cas != null);
      this.ts = this.cas.getTypeSystem();
      assertTrue(this.ts != null);
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

    this.stringType = this.ts.getType(CAS.TYPE_NAME_STRING);
    assertTrue(this.stringType != null);
    this.tokenType = this.ts.getType(CASTestSetup.TOKEN_TYPE);
    assertTrue(this.stringType != null);
    this.intType = this.ts.getType(CAS.TYPE_NAME_INTEGER);
    assertTrue(this.intType != null);
    this.tokenTypeType = this.ts.getType(CASTestSetup.TOKEN_TYPE_TYPE);
    assertTrue(this.tokenTypeType != null);
    this.wordType = this.ts.getType(CASTestSetup.WORD_TYPE);
    assertTrue(this.wordType != null);
    this.tokenTypeFeat = this.ts.getFeatureByFullName(CASTestSetup.TOKEN_TYPE_FEAT_Q);
    assertTrue(this.tokenTypeFeat != null);
    this.lemmaFeat = this.ts.getFeatureByFullName(CASTestSetup.LEMMA_FEAT_Q);
    assertTrue(this.lemmaFeat != null);
    this.sentLenFeat = this.ts.getFeatureByFullName(CASTestSetup.SENT_LEN_FEAT_Q);
    assertTrue(this.sentLenFeat != null);
    this.tokenFloatFeat = this.ts.getFeatureByFullName(CASTestSetup.TOKEN_FLOAT_FEAT_Q);
    assertTrue(this.tokenFloatFeat != null);
    this.startFeature = this.ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_BEGIN);
    assertTrue(this.startFeature != null);
    this.sentenceType = this.ts.getType(CASTestSetup.SENT_TYPE);
    assertTrue(this.sentenceType != null);
    this.annotationType = this.ts.getType(CAS.TYPE_NAME_ANNOTATION);
    assertTrue(this.annotationType != null);
    this.subsentenceType = this.ts.getType("SubTypeOfSentence");
    assertTrue(this.subsentenceType != null);
  }

  public void tearDown() {
    this.cas = null;
    this.ts = null;
    this.stringType = null;
    this.tokenType = null;
    this.intType = null;
    this.tokenTypeType = null;
    this.wordType = null;
    this.tokenTypeFeat = null;
    this.lemmaFeat = null;
    this.sentLenFeat = null;
    this.tokenFloatFeat = null;
    this.startFeature = null;
    this.sentenceType = null;
    this.annotationType = null;
  }

  public void testGetIndexes() {
    Iterator<FSIndex<FeatureStructure>> it = this.cas.getIndexRepository().getIndexes();
    while (it.hasNext()) {
      assertNotNull(it.next());
    }
  }

  public void testMoveTo() {
    // Add some arbitrary annotations
    for (int i = 0; i < 10; i++) {
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.annotationType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.sentenceType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.tokenType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.tokenType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.tokenType, i * 2, (i * 2) + 1));
    }
    final int start = 5;
    final int end = 7;
    FSIndexRepository repo = this.cas.getIndexRepository();
    for (int i = 0; i < 10; i++) {
      AnnotationFS annotation = this.cas.createAnnotation(this.annotationType, start, end);
      repo.addFS(annotation);
    }
    AnnotationFS match = this.cas.createAnnotation(this.annotationType, start, end);
    FSIndex<AnnotationFS> index = this.cas.getAnnotationIndex();
    FSIterator<AnnotationFS> it = index.iterator();
    it.moveTo(match);
    assertTrue(index.compare(match, it.get()) == 0);
    // The contract of moveTo() says that any preceding FS must be smaller.
    it.moveToPrevious();
    assertTrue(index.compare(match, it.get()) > 0);
  }

  public void testIterator() {
    for (int i = 0; i < 10; i++) {
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.annotationType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.sentenceType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.tokenType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.tokenType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.tokenType, i * 2, (i * 2) + 1));
    }
    for (int i = 19; i >= 10; i--) {
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.annotationType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.sentenceType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.tokenType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.tokenType, i * 2, (i * 2) + 1));
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.tokenType, i * 2, (i * 2) + 1));
    }

    // /////////////////////////////////////////////////////////////////////////
    // Create a reverse iterator for the set index and check that the result
    // is the same as for forward iteration.
    IntVector v = new IntVector();
    FSIndex<FeatureStructure> bagIndex = this.cas.getIndexRepository().getIndex(
        CASTestSetup.ANNOT_BAG_INDEX);
    FSIndex<FeatureStructure> setIndex = this.cas.getIndexRepository().getIndex(
        CASTestSetup.ANNOT_SET_INDEX);
    FSIndex<FeatureStructure> sortedIndex = this.cas.getIndexRepository().getIndex(
        CASTestSetup.ANNOT_SORT_INDEX);

    FSIterator<FeatureStructure> it = setIndex.iterator();
    AnnotationFS a, b = null;
    while (it.isValid()) {
      a = (AnnotationFS) it.get();
      if (b != null) {
        assertTrue(setIndex.compare(b, a) <= 0);
      }
      b = a;
      // System.out.println(
      // a.getType().getName() + " - " + a.getStart() + " - " +
      // a.getEnd());
      v.add(it.get().hashCode());
      it.moveToNext();
    }
    // System.out.println("Number of annotations: " + v.size());
    assertTrue(v.size() == ((10 * 3) + (10 * 3)));

    it = setIndex.iterator();
    it.moveToLast();
    int current = v.size() - 1;
    while (it.isValid() && (current >= 0)) {
      // System.out.println("Current: " + current);
      a = (AnnotationFS) it.get();
      // System.out.println(
      // a.getType().getName() + " - " + a.getStart() + " - " +
      // a.getEnd());
      assertTrue(it.get().hashCode() == v.get(current));
      it.moveToPrevious();
      --current;
    }
    assertTrue(current == -1);
    assertFalse(it.isValid());

    // /////////////////////////////////////////////////////////////////////////
    // Use an iterator to move forwards and backwards and make sure the
    // sequence
    // remains constant.
    it = setIndex.iterator();
    it.moveToFirst(); // This is redundant.
    current = 1;
    // System.out.println("Codes: " + v);
    while (current < (v.size() - 1)) {
      it.moveToNext();
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(current));
      it.moveToNext();
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(current + 1));
      it.moveToPrevious();
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(current));
      ++current;
    }

    // also test Java-style iteration
    Iterator<FeatureStructure> javaIt = setIndex.iterator();
    current = 0;
    while (javaIt.hasNext()) {
      assertEquals(javaIt.next().hashCode(), v.get(current++));
    }

    // test find()
    AnnotationFS annot = (AnnotationFS) setIndex.iterator().get();
    assertNotNull(setIndex.find(annot));
    assertNull(setIndex.find(this.cas.createAnnotation(this.annotationType, -1, -1)));

    // do same for JCas
    JCas jcas = null;
    try {
      jcas = this.cas.getJCas();
    } catch (CASException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
      assertTrue(false);
    }
    FSIndex jcasSetIndex = jcas.getJFSIndexRepository().getIndex(CASTestSetup.ANNOT_SET_INDEX);
    Annotation jcasAnnotation = (Annotation) jcasSetIndex.find(annot);
    assertNotNull(jcasAnnotation);
    assertNull(jcasSetIndex.find(this.cas.createAnnotation(this.annotationType, -1, -1)));

    // /////////////////////////////////////////////////////////////////////////
    // Test fast fail.

    it = bagIndex.iterator(); // use bag index, remove add last one
    // (preserves order for other tests).
    it.moveToLast();
    a = (AnnotationFS) it.get();
    it = setIndex.iterator(); // back to set iterator to do testing
    this.cas.getIndexRepository().removeFS(a);
    this.cas.getIndexRepository().addFS(a);

    boolean ok = false;
    try {
      it.next(); // should throw
    } catch (ConcurrentModificationException e) {
      ok = true;
    }
    assertTrue(ok);
    ok = false;
    try {
      it.next(); // should throw
    } catch (ConcurrentModificationException e) {
      ok = true;
    }
    assertTrue(ok);
    it.moveTo(a);
    ok = false;
    try {
      it.next(); // should not throw
      ok = true;
    } catch (ConcurrentModificationException e) {
      // checking this with the ok variable
    }
    assertTrue(ok);

    // Test find()
    Type wType = this.cas.getTypeSystem().getType("org.apache.uima.cas.test.types.Word");

    Feature wordFeat = wType.getFeatureByBaseName("word");

    for (int i = 0; i < 20; i++) {
      FeatureStructure fs = this.cas.createFS(wType);
      fs.setStringValue(wordFeat, "word" + i);
      this.cas.getIndexRepository().addFS(fs);
    }

    FSIndex<FeatureStructure> wordSetIndex = this.cas.getIndexRepository().getIndex(
        "Word Set Index");

    it = wordSetIndex.iterator();

    FeatureStructure fs = this.cas.createFS(wType);
    fs.setStringValue(wordFeat, "word1");

    // TEST moveTo() and get()
    it.moveTo(fs);

    assertSame(fs.getType(), it.get().getType());

    Type t1 = fs.getType();
    Type t2 = wordSetIndex.find(fs).getType();
    assertSame(t1, t2);

    // /////////////////////////////////////////////////////////////////////////
    // Test sorted index.

    // FSIndex sortedIndex = cas.getAnnotationIndex(); // using different
    // typeOrder
    // System.out.println("Number of annotations: " + sortedIndex.size());
    // for (it = sortedIndex.iterator(); it.hasNext(); it.next()) {
    // System.out.println(it.get());
    // }

    assertTrue(sortedIndex.size() == 100);
    v = new IntVector();
    it = sortedIndex.iterator();
    it.moveToFirst();
    b = null;
    while (it.isValid()) {
      a = (AnnotationFS) it.get();
      // System.out.println(a);
      assertTrue(a != null);
      if (b != null) {
        // System.out.println("b = " + b);
        assertTrue(sortedIndex.compare(b, a) <= 0);
      }
      b = a;
      v.add(a.hashCode());
      it.moveToNext();
    }
    assertTrue(sortedIndex.size() == v.size());

    // Test moveTo()
    List<AnnotationFS> list = new ArrayList<AnnotationFS>();
    FSIterator<AnnotationFS> it2 = this.cas.getAnnotationIndex().iterator();
    for (it2.moveToFirst(); it2.isValid(); it2.moveToNext()) {
      list.add(it2.get());
    }
    // AnnotationFS an;
    for (int i = 0; i < list.size(); i++) {
      // System.out.println("Iteration: " + i);
      it2.moveToFirst();
      it2.moveTo(list.get(i));
      assertTrue(((AnnotationFS) it2.get()).getBegin() == ((AnnotationFS) list.get(i)).getBegin());
      assertTrue(((AnnotationFS) it2.get()).getEnd() == ((AnnotationFS) list.get(i)).getEnd());
    }

    // Check that reverse iterator produces reverse sequence.
    // Note: this test is not valid. It is by no means guaranteed that reverse iteration of a
    // sorted index will produce the reverse result of forward iteration. I no two annotations
    // are equal wrt the sort order, this works of course. However, if some FSs are equal wrt
    // the sort order, those may be returned in any order.
    // it.moveToLast();
    // System.out.println(it.get());
    // for (int i = v.size() - 1; i >= 0; i--) {
    // assertTrue(it.isValid());
    // assertTrue(it.get().hashCode() == v.get(i));
    // it.moveToPrevious();
    // }

    // /////////////////////////////////////////////////////////////////////////
    // Use an iterator to move forwards and backwards and make sure the
    // sequence
    // remains constant.
    it = sortedIndex.iterator();
    it.moveToFirst(); // This is redundant.
    current = 1;
    // System.out.println("Codes: " + v);
    while (current < (v.size() - 1)) {
      it.moveToNext();
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(current));
      it.moveToNext();
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(current + 1));
      it.moveToPrevious();
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(current));
      ++current;
    }

    // also test Java-style iteration
    javaIt = sortedIndex.iterator();
    current = 0;
    while (javaIt.hasNext()) {
      assertEquals(javaIt.next().hashCode(), v.get(current++));
    }
    // /////////////////////////////////////////////////////////////////////////
    // Test fast fail.

    it = bagIndex.iterator(); // use bag index, remove add last one
    // (preserves order for other tests).
    it.moveToLast();
    a = (AnnotationFS) it.get();
    // for (it = sortedIndex.iterator(); it.hasNext(); it.next()) {
    // System.out.println(it.get());
    // }
    it = sortedIndex.iterator();
    it.next();
    it.next();
    this.cas.getIndexRepository().removeFS(a);
    this.cas.getIndexRepository().addFS(a);
    ok = false;
    try {
      it.get(); // should throw
    } catch (ConcurrentModificationException e) {
      ok = true;
    }
    assertTrue(ok);
    ok = false;
    try {
      it.next(); // should throw
    } catch (ConcurrentModificationException e) {
      ok = true;
    }
    assertTrue(ok);
    ok = false;
    try {
      it.moveToNext(); // should throw
    } catch (ConcurrentModificationException e) {
      ok = true;
    }
    assertTrue(ok);
    it.moveTo(a);
    ok = false;
    try {
      it.next(); // should not throw
      ok = true;
    } catch (ConcurrentModificationException e) {
      // checking with boolean "ok"
    }
    assertTrue(ok);

    sortedIndex = null;

    // /////////////////////////////////////////////////////////////////////////
    // Test bag index.
    // System.out.println("Number of annotations: " + sortedIndex.size());
    assertTrue(bagIndex.size() == 100);
    v = new IntVector();
    it = bagIndex.iterator();
    b = null;
    while (it.isValid()) {
      a = (AnnotationFS) it.get();
      assertTrue(a != null);
      if (b != null) {
        assertTrue(bagIndex.compare(b, a) <= 0);
      }
      b = a;
      v.add(a.hashCode());
      it.moveToNext();
    }
    assertTrue(bagIndex.size() == v.size());

    // Check that reverse iterator produces reverse sequence.
    it.moveToLast();
    for (int i = v.size() - 1; i >= 0; i--) {
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(i));
      it.moveToPrevious();
    }

    // /////////////////////////////////////////////////////////////////////////
    // Use an iterator to move forwards and backwards and make sure the
    // sequence
    // remains constant.
    it = bagIndex.iterator();
    it.moveToFirst(); // This is redundant.
    current = 1;
    // System.out.println("Codes: " + v);
    while (current < (v.size() - 1)) {
      it.moveToNext();
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(current));
      it.moveToNext();
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(current + 1));
      it.moveToPrevious();
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(current));
      ++current;
    }

    // also test Java-style iteration
    javaIt = bagIndex.iterator();
    current = 0;
    while (javaIt.hasNext()) {
      assertEquals(javaIt.next().hashCode(), v.get(current++));
    }

    // Test iterator copy.
    FSIterator<AnnotationFS> source, copy;
    source = this.cas.getAnnotationIndex().iterator();
    // Count items.
    int count = 0;
    for (source.moveToFirst(); source.isValid(); source.moveToNext()) {
      ++count;
    }

    final int max = count;
    count = 0;
    source.moveToFirst();
    copy = source.copy();
    copy.moveToFirst();
    // System.out.println("Max: " + max);
    while (count < max) {
      // System.out.println("Count: " + count);
      assertTrue(source.isValid());
      assertTrue(copy.isValid());
      String out = source.get().toString() + copy.get().toString();
      assertTrue(out, source.get().equals(copy.get()));
      source.moveToNext();
      copy.moveToNext();
      ++count;
    }

    // /////////////////////////////////////////////////////////////////////////
    // Test fast fail.

    it = bagIndex.iterator(); // use bag index, remove add last one
    // (preserves order for other tests).
    it.moveToLast();
    a = (AnnotationFS) it.get();
    this.cas.getIndexRepository().removeFS(a);
    this.cas.getIndexRepository().addFS(a);

    ok = false;
    try {
      it.get(); // should throw
    } catch (ConcurrentModificationException e) {
      ok = true;
    }
    assertTrue(ok);
    it.moveToLast();
    it.moveToPrevious();
    it.moveToPrevious();
    it.moveToPrevious();
    it.moveToNext();
    this.cas.getIndexRepository().removeFS(a);
    this.cas.getIndexRepository().addFS(a);

    ok = false;
    try {
      it.moveToNext(); // should throw
    } catch (ConcurrentModificationException e) {
      ok = true;
    }
    assertTrue(ok);
    ok = false;
    try {
      it.moveToPrevious(); // should throw
    } catch (ConcurrentModificationException e) {
      ok = true;
    }
    assertTrue(ok);
    it.moveTo(a);
    ok = false;
    try {
      it.next(); // should not throw
      ok = true;
    } catch (ConcurrentModificationException e) {
      // checking with boolean "ok"
    }
    assertTrue(ok);

  }
  
  private void addAnnotations(AnnotationFS[] fsArray, Type type) {
    FSIndexRepository ir = this.cas.getIndexRepository();    
    for (int i = 0; i < fsArray.length; i++) {
      // key order:
      //   0 ... 50 200 ... 160  66 66 66 ..
      // item order
      //   0 - 50, 90 - 99, 89 - 60
      int j = (i >= 90) ? 66 :           // some constant keys
              (i > 50) ? 200 - i :       // some decreasing keys
              i;                         // some increasing keys
      fsArray[i] = this.cas.createAnnotation(type, j * 5, (j * 5) + 4);
      ir.addFS(fsArray[i]);
    }
  }

  /**
   * Test deleting FSs from indexes.
   */
  public void testDelete() {
    // Create a bunch of FSs.
    // have 10% of them be the same key
    // have the order be scrambled somewhat, not strictly increasing
    AnnotationFS[] fsArray = new AnnotationFS[100];
    FSIndexRepository ir = this.cas.getIndexRepository();
    addAnnotations(fsArray, this.tokenType);
    FSIndex<FeatureStructure> setIndex = this.cas.getIndexRepository().getIndex(
        CASTestSetup.ANNOT_SET_INDEX, this.tokenType);
    FSIterator<FeatureStructure> setIt = setIndex.iterator();
    FSIndex<AnnotationFS> sortedIndex = this.cas.getAnnotationIndex(this.tokenType);
    FSIterator<AnnotationFS> sortedIt = sortedIndex.iterator();
    FSIndex<FeatureStructure> bagIndex = ir.getIndex(CASTestSetup.ANNOT_BAG_INDEX, this.tokenType);
    FSIterator<FeatureStructure> bagIt = bagIndex.iterator();
    // For each index, check that the FSs are actually in the index.
    for (int i = 0; i < fsArray.length; i++) {
      setIt.moveTo(fsArray[i]);
      assertTrue(setIt.isValid());
      assertTrue(setIt.get().equals(fsArray[(i < 90) ? i : 90]));
      bagIt.moveTo(fsArray[i]);
      assertTrue(bagIt.isValid());
      assertTrue(bagIt.get().equals(fsArray[i]));
      sortedIt.moveTo(fsArray[i]);
      assertTrue(sortedIt.isValid());
      fsBeginEndEqual(sortedIt.get(), fsArray[i]);
    }
    sortedIt.moveToFirst();
    // item order
    //   0 - 50, 90 - 99, 89 - 60    
    for (int i = 0; i < fsArray.length; i++) {
      int j = (i >= 61) ? (89 - (i - 61)) :
              (i >= 51) ? 90 :
              i;
      fsBeginEndEqual(sortedIt.get(), fsArray[j]);
      sortedIt.moveToNext();
    }
    assertFalse(sortedIt.isValid());
    
    // Remove an annotation, then add it again. Try setting the iterators to
    // that FS. The iterator should either be invalid, or point to a
    // different FS.
    for (int i = 0; i < fsArray.length; i++) {
      ir.removeFS(fsArray[i]);
      setIt.moveTo(fsArray[i]);
      if (setIt.isValid()) {
        int oldRef = this.cas.ll_getFSRef(fsArray[i]);
        int newRef = this.cas.ll_getFSRef(setIt.get());
        assertTrue(oldRef != newRef);
        assertTrue(!setIt.get().equals(fsArray[i]));
      }
      bagIt.moveTo(fsArray[i]);
      if (bagIt.isValid()) {
        assertTrue(!bagIt.get().equals(fsArray[i]));
      }
      sortedIt.moveTo(fsArray[i]);
      if (sortedIt.isValid()) {
        assertTrue(!sortedIt.get().equals(fsArray[i]));
      }
      ir.addFS(fsArray[i]);
    }
    // Remove all annotations.
    for (int i = 0; i < fsArray.length; i++) {
      ir.removeFS(fsArray[i]);
    }
    // All iterators should be invalidated when being reset.
    bagIt.moveToFirst();
    assertFalse(bagIt.isValid());
    setIt.moveToFirst();
    assertFalse(setIt.isValid());
    sortedIt.moveToFirst();
    assertFalse(sortedIt.isValid());
  }
  
  private void verifyMoveToFirst(FSIterator it, boolean expected) {
    it.moveToFirst();
    assertEquals(it.isValid(), expected);
  }
  
  private void verifyHaveSubset(FSIterator x, int nbr, Type type) {
    x.moveToFirst();
    int i = 0;
    while (x.hasNext()) {
      i++;
      assertEquals(type, x.get().getType());
      x.moveToNext();
    }
    assertEquals(nbr, i);
  }

  public void testRemoveAll() {
    AnnotationFS[] fsArray = new AnnotationFS[100];
    AnnotationFS[] subFsArray = new AnnotationFS[100];
    FSIndexRepository ir = this.cas.getIndexRepository();
    
    addAnnotations(fsArray, ts.getType("Sentence"));
    addAnnotations(subFsArray, ts.getType("SubTypeOfSentence"));
    
    FSIndex<FeatureStructure> setIndex = ir.getIndex(CASTestSetup.ANNOT_SET_INDEX, this.sentenceType);
    FSIndex<FeatureStructure> bagIndex = ir.getIndex(CASTestSetup.ANNOT_BAG_INDEX, this.sentenceType);
    FSIndex<AnnotationFS> sortedIndex = this.cas.getAnnotationIndex(this.sentenceType);

    FSIndex<FeatureStructure> subsetIndex = ir.getIndex(CASTestSetup.ANNOT_SET_INDEX, this.subsentenceType);
    FSIndex<FeatureStructure> subbagIndex = ir.getIndex(CASTestSetup.ANNOT_BAG_INDEX, this.subsentenceType);
    FSIndex<AnnotationFS>     subsortedIndex = this.cas.getAnnotationIndex(this.subsentenceType);

    ir.removeAllIncludingSubtypes(sentenceType);

    FSIterator<FeatureStructure> setIt = setIndex.iterator();
    FSIterator<FeatureStructure> bagIt = bagIndex.iterator();
    FSIterator<AnnotationFS> sortedIt = sortedIndex.iterator();
    
    // subindexes

    FSIterator<FeatureStructure> subsetIt = subsetIndex.iterator();
    FSIterator<FeatureStructure> subbagIt = subbagIndex.iterator();
    FSIterator<AnnotationFS> subsortedIt = subsortedIndex.iterator();

    verifyMoveToFirst(setIt, false);
    verifyMoveToFirst(bagIt, false);
    verifyMoveToFirst(sortedIt, false);
    verifyMoveToFirst(subsetIt, false);
    verifyMoveToFirst(subbagIt, false);
    verifyMoveToFirst(subsortedIt, false);

//    addAnnotations(fsArray, ts.getType("Sentence"));
//    addAnnotations(subFsArray, ts.getType("SubTypeOfSentence"));
    for (AnnotationFS fs : fsArray) {
      ir.addFS(fs);
    }
    for (AnnotationFS fs : subFsArray) {
      ir.addFS(fs);
    }

    verifyMoveToFirst(setIt, true);
    verifyMoveToFirst(bagIt, true);
    verifyMoveToFirst(sortedIt, true);
    verifyMoveToFirst(subsetIt, true);
    verifyMoveToFirst(subbagIt, true);
    verifyMoveToFirst(subsortedIt, true);

    ir.removeAllExcludingSubtypes(this.sentenceType);
    
    setIt = setIndex.iterator();
    bagIt = bagIndex.iterator();
    sortedIt = sortedIndex.iterator();

    subsetIt = subsetIndex.iterator();
    subbagIt = subbagIndex.iterator();
    subsortedIt = subsortedIndex.iterator();

    verifyHaveSubset(setIt, 91, subsentenceType);
    verifyHaveSubset(bagIt, 100, subsentenceType);
    verifyHaveSubset(sortedIt, 100, subsentenceType);
    verifyHaveSubset(subsetIt, 91, subsentenceType);
    verifyHaveSubset(subbagIt, 100, subsentenceType);
    verifyHaveSubset(subsortedIt, 100, subsentenceType);
    
    for (AnnotationFS fs : fsArray) {
      ir.addFS(fs);
    }
    
    ir.removeAllExcludingSubtypes(subsentenceType);

    setIt = setIndex.iterator();
    bagIt = bagIndex.iterator();
    sortedIt = sortedIndex.iterator();

    subsetIt = subsetIndex.iterator();
    subbagIt = subbagIndex.iterator();
    subsortedIt = subsortedIndex.iterator();

    verifyHaveSubset(setIt, 91, sentenceType);
    verifyHaveSubset(bagIt, 100, sentenceType);
    verifyHaveSubset(sortedIt, 100, sentenceType);
    verifyMoveToFirst(subsetIt, false);
    verifyMoveToFirst(subbagIt, false);
    verifyMoveToFirst(subsortedIt, false);
  }
  
  
  public void testInvalidIndexRequest() {
    boolean exc = false;
    try {
      this.cas.getIndexRepository().getIndex(CASTestSetup.ANNOT_BAG_INDEX, this.stringType);
    } catch (CASRuntimeException e) {
      exc = true;
    }
    assertTrue(exc);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(IteratorTest.class);
  }

  private void fsBeginEndEqual(AnnotationFS fs1, AnnotationFS fs2) {
    assertEquals(fs1.getBegin(), fs2.getBegin());
    assertEquals(fs1.getEnd(), fs2.getEnd());     
  }
}
