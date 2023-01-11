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

import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.throwable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aa.ConcreteType;
import aa.Root;
import x.y.z.EndOfSentence;
import x.y.z.Sentence;
import x.y.z.Token;

/**
 * Class comment for CASTest.java goes here.
 * 
 */
public class JCasTest {

  private CAS cas;

  private JCas jcas;

  // private TypeSystem ts;

  public EndOfSentence endOfSentenceInstance;

  @BeforeEach
  public void setUp() throws Exception {
    try {
      try {
        this.cas = CASInitializer.initCas(new CASTestSetup(), null);
        // this.ts = this.cas.getTypeSystem();
        this.jcas = cas.getJCas();
        endOfSentenceInstance = new EndOfSentence(jcas);
      } catch (Exception e1) {
        checkOkMissingImport(e1);
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void checkOkMissingImport(Exception e1) {
    if (e1 instanceof CASRuntimeException) {
      System.out.print("setup caught CAS Exception with message: ");
      String m = e1.getMessage();
      System.out.println(m);
      assertEquals("The JCas cannot be initialized.  The following errors occurred: "
              + "\nUnable to find required getPlainRef method for JCAS type aa.Root with return type of org.apache.uima.jcas.cas.TOP."
              + "\nUnable to find required setPlainRef method for JCAS type aa.Root with argument type of org.apache.uima.jcas.cas.TOP.\n",
              m);
      // if (!m
      // .equals("Error initializing JCas: Error: can't access feature information from CAS in
      // initializing JCas type: aa.Root, feature: testMissingImport\n")) {
      // assertTrue(false);
      // }
    } else {
      assertTrue(false);
    }
  }

  public void checkExpectedBadCASError(Exception e1, String err) {
    if (e1 instanceof UIMARuntimeException) {
      UIMARuntimeException e = (UIMARuntimeException) e1;
      System.out.print("\nCaught CAS Exception with message: ");
      String m = e1.getMessage();
      System.out.println(m);
      if (!(e.getMessageKey().equals(err))) {
        assertTrue(false);
      }
    } else {
      assertTrue(false);
    }
  }

  @AfterEach
  public void tearDown() {
    this.cas = null;
    // this.ts = null;
    this.jcas = null;
    this.endOfSentenceInstance = null;
  }

  @Test
  public void testMissingFeatureInCas() throws Exception {
    try {
      // jcasCasMisMatch(CASTestSetup.BAD_MISSING_FEATURE_IN_CAS, CASException.JCAS_INIT_ERROR);
      // CAS localCas;
      // JCas localJcas = null;
      // boolean errFound = false;
      try {
        // error happens during setup
        /* localCas = */ CASInitializer
                .initCas(new CASTestSetup(CASTestSetup.BAD_MISSING_FEATURE_IN_CAS), null);
      } catch (CASRuntimeException e) {
        assertTrue(e.getMessageKey().equals(CASException.JCAS_INIT_ERROR));
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testChangedFType() throws Exception {
    try {
      jcasCasMisMatch(CASTestSetup.BAD_CHANGED_FEATURE_TYPE, UIMARuntimeException.INTERNAL_ERROR);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void jcasCasMisMatch(int testId, String expectedErr) throws Exception {
    try {
      // CAS localCas;
      // JCas localJcas;
      boolean errFound = false;
      try {
        /* localCas = */CASInitializer.initCas(new CASTestSetup(testId), null);
        // ts = this.cas.getTypeSystem();
        // try {
        // localJcas = localCas.getJCas();
        // } catch (Exception e1) {
        // checkExpectedBadCASError(e1, expectedErr);
        // errFound = true;
        // }
      } catch (Exception e) {
        checkExpectedBadCASError(e, expectedErr);
        errFound = true;
      }
      assertTrue(errFound);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testIteratorCopy() {
    Annotation something = new Annotation(jcas);
    something.addToIndexes();

    JFSIndexRepository ir = jcas.getJFSIndexRepository();
    FSIterator<Annotation> i1 = ir.getAnnotationIndex().iterator();
    FSIterator<Annotation> i2 = i1.copy();
    FSIterator<Annotation> i3 = i2.copy();
    assertTrue(i3 != null);
  }

  @Test
  public void testGetFSIndexRepository() throws Exception {
    try {
      FSIndexRepository ir = jcas.getFSIndexRepository();
      LowLevelIndexRepository ll_ir = jcas.getLowLevelIndexRepository();

      assertTrue(ir != null);
      assertTrue(ir == cas.getIndexRepository());
      assertTrue(ll_ir != null);
      assertTrue(ll_ir == cas.getLowLevelCAS().ll_getIndexRepository());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testMisc() throws Exception {
    try {
      try {
        jcas.getRequiredType("uima.tcas.Annotation");
      } catch (CASException e) {
        assertTrue(false);
      }
      try {
        jcas.getRequiredType("missing.type");
        assertTrue(false);
      } catch (CASException e1) {
        System.out.print("This error msg expected: ");
        System.out.println(e1);
      }

      try {
        jcas.getRequiredFeature(jcas.getCasType(Annotation.type), "begin");
      } catch (CASException e2) {
        assertTrue(false);
      }
      try {
        jcas.getRequiredFeature(jcas.getCasType(Annotation.type), "Begin");
        assertTrue(false);
      } catch (CASException e2) {
        System.out.print("This error msg expected: ");
        System.out.println(e2);
      }
      CAS localCas = jcas.getCas();
      assertTrue(localCas == this.cas);
      LowLevelCAS ll_cas = jcas.getLowLevelCas();
      assertTrue(ll_cas == this.cas);
      CASImpl casImpl = jcas.getCasImpl();
      assertTrue(casImpl == this.cas);

      /* Annotation a1 = */ new Annotation(jcas, 4, 5);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testJCasAccessing() throws Exception {
    try {
      Root r1 = new Root(jcas);
      r1.setArrayFloat(new FloatArray(jcas, 2));
      r1.setArrayFloat(0, (float) 3.0);
      r1.setArrayFloat(1, (float) 2.5);
      assertTrue(3.0 == r1.getArrayFloat(0));
      assertTrue(2.5 == r1.getArrayFloat(1));

      Root r2 = new Root(jcas);
      r2.setArrayRef(new FSArray(jcas, 3));
      EndOfSentence eos1 = new EndOfSentence(jcas);
      EndOfSentence eos2 = new EndOfSentence(jcas);

      r2.setArrayRef(0, eos1);
      r2.setArrayRef(1, eos2);
      assertTrue(r2.getArrayRef(0).equals(eos1));
      assertTrue(r2.getArrayRef(1).equals(eos2));

      r2.setArrayInt(new IntegerArray(jcas, 1));
      r2.setArrayInt(0, 17);
      assertTrue(r2.getArrayInt(0) == 17);
      IntegerArray ia = r2.getArrayInt();
      assertTrue(ia.get(0) == 17);

      r2.setArrayString(new StringArray(jcas, 2));
      r2.setArrayString(0, "zero");
      r2.setArrayString(1, "one");
      assertTrue(r2.getArrayString(0).equals("zero"));
      assertTrue(r2.getArrayString(1).equals("one"));

      // error paths
      // array out of bounds
      boolean caught = false;
      try {
        r2.getArrayString(2);
      } catch (ArrayIndexOutOfBoundsException e) {
        caught = true;
      }
      assertTrue(caught);
      caught = false;
      try {
        r2.setArrayString(-1, "should fail");
      } catch (ArrayIndexOutOfBoundsException e) {
        caught = true;
      }
      assertTrue(caught);

      // float values
      r1 = new Root(jcas);
      r1.setPlainFloat(1247.3F);
      r1.setArrayFloat(new FloatArray(jcas, 3));
      r1.setArrayFloat(2, 321.4F);
      Assertions.assertThat(r1.getPlainFloat()).isEqualTo(1247.3F);
      Assertions.assertThat(r1.getArrayFloat(2)).isEqualTo(321.4F);

      // double values
      r1 = new Root(jcas);
      r1.setPlainDouble(2247.3D);
      r1.setArrayDouble(new DoubleArray(jcas, 3));
      r1.setArrayDouble(2, 421.4D);
      Assertions.assertThat(r1.getPlainDouble()).isEqualTo(2247.3D);
      Assertions.assertThat(r1.getArrayDouble(2)).isEqualTo(421.4D);

      // null values
      r2.setArrayString(0, null);
      r2.setArrayRef(0, null);
      r2.setArrayRef(null);
      r2.setArrayString(null);
      r2.setPlainRef(null);
      r2.setPlainString(null);
      assertTrue(null == r2.getPlainString());
      assertTrue(null == r2.getPlainRef());
      caught = false;
      try {
        r2.getArrayRef(0);
      } catch (NullPointerException e) {
        caught = true;
      }
      assertTrue(caught);
      assertTrue(null == r2.getArrayString());
      assertTrue(null == r2.getArrayRef());

      r2.addToIndexes();
      r1.addToIndexes();

      JFSIndexRepository jfsi = jcas.getJFSIndexRepository();
      FSIndex<Root> fsi1 = jfsi.getIndex("all", Root.type);
      FSIterator<Root> fsit1 = fsi1.iterator();
      assertTrue(fsit1.isValid());
      Root[] fetched = new Root[2];

      fetched[0] = (Root) fsit1.get();
      fsit1.moveToNext();
      assertTrue(fsit1.isValid());

      fetched[1] = (Root) fsit1.get();
      assertTrue(fsit1.isValid());
      fsit1.moveToNext();
      assertFalse(fsit1.isValid());

      // is bag index, order may be arbitrary
      assertTrue((fetched[0] == r1 && fetched[1] == r2) || (fetched[1] == r1 && fetched[0] == r2));

      /*
       * while (fsit1.isValid()) { System.out.println("Iterator getting: " +
       * fsit1.get().toString()); fsit1.moveToNext(); }
       */

      // test new objects with old iterators
      // NOT SUPPORTED
      // JFSIndex oIndex = jcas.getIndex("all");
      // JFSIterator oI = oIndex.iterator();
      // assertTrue(oI.isValid());
      // assertTrue(r2 == oI.get());
      // oI.moveToNext();
      // assertTrue(oI.isValid());
      // assertTrue(r1 == oI.get());
      ((CASImpl) cas).traceFSflush();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  /*
   * Tests for Memory Leaks and Performance
   * 
   * Core: randomly creating lots of CAS objects of all kinds with a simple computation for what
   * they should contain, followed by iterating over the objects and checking they contain the
   * proper values. Measure heap use and time. Heap use measure: System.gc();
   * System.out.println("FreeMem: " + Runtime.getRuntime().freeMemory()); Creating cas objects: Use
   * random number to pick one of n types of objects to create For each object type, create specific
   * field values based on that objects' ID in ID-Hash sense. System.identityHashCode(object)
   * 
   * Timing: System.currentTimeMillis()
   * 
   */

  @Test
  public void testRandom() throws Exception {
    try {
      // System.out.print("Making Random: ");
      for (int i = 0; i < 50; i++) {
        root1.make();
        // System.out.print("m");
      }
      JFSIndexRepository jir = jcas.getJFSIndexRepository();
      FSIterator<Root> it = jir.<Root> getIndex("all", Root.type).iterator();
      // System.out.print("\nTesting Random: ");
      while (it.isValid()) {
        root1.test(it.get());
        // System.out.print("t");
        it.moveToNext();
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  interface MakeAndTest {
    void make();

    @Test
    void test(Object o);
  }

  MakeAndTest root1 = new MakeAndTest() {
    @Override
    public void make() {
      Root r1 = new Root(jcas);
      // Note: Java 7 can return -ve hashcodes so must avoid -ve remainders.
      int k = System.identityHashCode(r1);
      int imax = 1 + Math.abs(k % 10);
      r1.setArrayFloat(new FloatArray(jcas, imax));
      for (int i = 0; i < imax; i++) {
        r1.setArrayFloat(i, (float) k / (i + 1));
      }
      int imaxFS = 1 + Math.abs((k % 3));
      r1.setArrayRef(new FSArray(jcas, imaxFS));
      for (int i = 1; i < imaxFS; i++) {
        r1.setArrayRef(i, endOfSentenceInstance);
      }
      r1.setPlainString("" + k);
      r1.addToIndexes();
    }

    @Override
    @Test
    public void test(Object o1) {
      assertTrue(o1 instanceof Root);
      Root r1 = (Root) o1;
      int k = System.identityHashCode(r1);
      int imax = 1 + Math.abs(k % 10);
      for (int i = 0; i < imax; i++) {
        assertTrue(r1.getArrayFloat(i) == ((float) k / (i + 1)));
      }
      int imaxFS = 1 + Math.abs(k % 3);
      for (int i = 1; i < imaxFS; i++) {
        assertTrue(endOfSentenceInstance == r1.getArrayRef(i));
      }
      assertTrue(r1.getPlainString().equals("" + k));
    }
  };

  @Test
  public void test2CASs() throws Exception {
    try {
      try {
        CAS cas2 = CASInitializer.initCas(new CASTestSetup(), null);
        // TypeSystem ts2 = cas2.getTypeSystem();
        JCas jcas2 = cas2.getJCas();
        if (TypeSystemImpl.IS_DISABLE_TYPESYSTEM_CONSOLIDATION) {
          assertTrue(jcas.getCasType(Annotation.type).equals(jcas2.getCasType(Annotation.type)));
          assertFalse(jcas.getCasType(Annotation.type) == jcas2.getCasType(Annotation.type));
        } else {
          assertTrue(jcas.getCasType(Annotation.type) == jcas2.getCasType(Annotation.type));
        }
      } catch (Exception e) {
        checkOkMissingImport(e);
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testAbstract() throws Exception {
    try {
      boolean caughtExc = true;
      try {
        ConcreteType concreteType = new ConcreteType(jcas);
        concreteType.setAbstractInt(7);
        concreteType.setConcreteString("sss");
        assertTrue(7 == concreteType.getAbstractInt());
        assertTrue("sss".equals(concreteType.getConcreteString()));

        jcas.getCas().createFS(jcas.getCas().getTypeSystem().getType("aa.AbstractType"));

      } catch (CASRuntimeException e) {
        caughtExc = false;
        // assertTrue(e.getError() == CASRuntimeException.JCAS_MAKING_ABSTRACT_INSTANCE);
        System.out.print("This error msg expected: ");
        System.out.println(e);
      }
      assertTrue(caughtExc);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testNonJCasCoveredByJCas() throws Exception {
    try {
      CAS localCas = jcas.getCas();
      Type subTok = localCas.getTypeSystem().getType("SubToken");
      Annotation a1 = new Annotation(jcas);
      a1.addToIndexes();
      FeatureStructure f1 = localCas.createFS(subTok);
      localCas.getIndexRepository().addFS(f1);

      JFSIndexRepository ir = jcas.getJFSIndexRepository();
      FSIndex<Annotation> index = ir.getAnnotationIndex();
      FSIterator<Annotation> it = index.iterator();

      try {

        while (it.isValid()) {
          Object o = it.get();
          assertTrue(o instanceof Annotation);
          it.moveToNext();
        }
      } catch (Exception e) {
        System.out.println("failed: nonJCasCovered by JCas");
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testFSListNotPromoted() throws Exception {
    try {
      CAS localCas = jcas.getCas();
      TypeSystem ts = localCas.getTypeSystem();
      Type fsl = ts.getType("uima.cas.NonEmptyFSList");
      FeatureStructure fs = localCas.createFS(fsl);
      assertTrue(fs instanceof NonEmptyFSList);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testCreateFSafterReset() throws Exception {
    try {
      // CAS localCas = jcas.getCas();
      cas.reset();
      TypeSystem ts = cas.getTypeSystem();
      Type fsl = ts.getType("uima.cas.NonEmptyFSList");
      cas.createFS(fsl);
      assertTrue(true);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testIteratorGetsJCasType() throws Exception {
    try {
      Token tok1 = new Token(jcas);
      tok1.addToIndexes();
      FSIterator<Token> it = jcas.getJFSIndexRepository().<Token> getIndex("all", Token.type)
              .iterator();
      while (it.hasNext()) {
        Token token = (Token) it.next();
        token.addToIndexes(); // something to do to keep Java from optimizing this away.
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  // THis test is Java impl specific, works with Oracle Java 7 and 8, not tested with other Javas
  // uncomment to run, but normally commented out.
  // public void testSubiterator() throws Exception {
  // for (int i = 0; i < 5; i++) { // tokens: 0,1, 1,3 2,5, 3,7 4,9
  // Token tok1 = new Token(jcas, i, 1 + 2*i);
  // tok1.addToIndexes();
  // }
  // FSIndexRepositoryMgr irm = (FSIndexRepositoryMgr)jcas.getIndexRepository();
  // TypeSystem ts = cas.getTypeSystem();
  // LinearTypeOrder lo = irm.getDefaultTypeOrder();
  // Type tokenType = ts.getType("x.y.z.Token");
  // Type sentenceType = ts.getType("x.y.z.Sentence");
  // Type annotType = ts.getType(CAS.TYPE_NAME_ANNOTATION);
  //
  // boolean java7 = System.getProperty("java.version").startsWith("1.7");
  //
  // /*********************************************************
  // * Surprise: Type Order is different between Java 7 and 8
  // *********************************************************/
  // assertEquals(java7, lo.lessThan(annotType, tokenType)); // annotation < token for java 7,
  // opposite for Java 8
  // assertTrue(lo.lessThan(sentenceType, tokenType));
  //
  //
  // /********************************************************
  // * This iterator is always empty
  // ********************************************************/
  // Iterator<Annotation> iter = jcas.getAnnotationIndex(Token.type).subiterator(new Token(jcas, 2,
  // 5));
  // assertFalse(iter.hasNext()); // because is empty because of definition of where to start, step
  // #2 (see above)
  //
  // /*********************************************************
  // * Surprise: This iterator is empty only for Java 8
  // * due to type ordering
  // *********************************************************/
  // iter = jcas.getAnnotationIndex(Token.type).subiterator(new Annotation(jcas, 2, 5));
  // assertEquals(java7, iter.hasNext()); // Ok for java 7, empty for java 8 because of type order
  // difference
  //
  // /*********************************************************
  // * This iterator is never empty because type order for
  // * Sentence is before Token in both Java 7 and 8
  // *********************************************************/
  // iter = jcas.getAnnotationIndex(Token.type).subiterator(new Sentence(jcas, 2, 5));
  // assertTrue(iter.hasNext()); // OK for both, because Sentence is before Token in both Java 7 and
  // 8
  // }

  @Test
  public void testGetNthFSList() throws Exception {
    try {
      Token tok1 = new Token(jcas);
      Token tok2 = new Token(jcas);

      NonEmptyFSList<Token> fsList1 = new NonEmptyFSList<>(jcas);
      fsList1.setHead(tok2);
      fsList1.setTail(new EmptyFSList<>(jcas));
      NonEmptyFSList<Token> fsList = new NonEmptyFSList<>(jcas);
      fsList.setHead(tok1);
      fsList.setTail(fsList1);
      /* EmptyFSList emptyFsList = */ new EmptyFSList<Token>(jcas);

      // try {
      // emptyFsList.getNthElement(0);
      // assertTrue(false); // error if we get here
      // } catch (CASRuntimeException e) {
      // assertTrue(e.getMessageKey().equals(CASRuntimeException.JCAS_GET_NTH_ON_EMPTY_LIST));
      // System.out.print("Expected Error: ");
      // System.out.println(e.getMessage());
      // }

      try {
        fsList.getNthElement(-1);
        assertTrue(false); // error if we get here
      } catch (CASRuntimeException e) {
        assertTrue(e.getMessageKey().equals(CASRuntimeException.JCAS_GET_NTH_NEGATIVE_INDEX));
        System.out.print("Expected Error: ");
        System.out.println(e.getMessage());
      }

      try {
        fsList.getNthElement(2);
        assertTrue(false); // error if we get here
      } catch (CASRuntimeException e) {
        assertTrue(e.getMessageKey().equals(CASRuntimeException.JCAS_GET_NTH_PAST_END));
        System.out.print("Expected Error: ");
        System.out.println(e.getMessage());
      }

      assertTrue(tok1 == fsList.getNthElement(0));
      assertTrue(tok2 == fsList.getNthElement(1));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testGetNthIntegerList() throws Exception {
    try {

      NonEmptyIntegerList intList1 = new NonEmptyIntegerList(jcas);
      intList1.setHead(2);
      intList1.setTail(new EmptyIntegerList(jcas));
      NonEmptyIntegerList intList = new NonEmptyIntegerList(jcas);
      intList.setHead(1);
      intList.setTail(intList1);
      EmptyIntegerList emptyFsList = new EmptyIntegerList(jcas);

      try {
        emptyFsList.getNthElement(0);
        assertTrue(false); // error if we get here
      } catch (CASRuntimeException e) {
        assertTrue(e.getMessageKey().equals(CASRuntimeException.JCAS_GET_NTH_ON_EMPTY_LIST));
        System.out.print("Expected Error: ");
        System.out.println(e.getMessage());
      }

      try {
        intList.getNthElement(-1);
        assertTrue(false); // error if we get here
      } catch (CASRuntimeException e) {
        assertTrue(e.getMessageKey().equals(CASRuntimeException.JCAS_GET_NTH_NEGATIVE_INDEX));
        System.out.print("Expected Error: ");
        System.out.println(e.getMessage());
      }

      try {
        intList.getNthElement(2);
        assertTrue(false); // error if we get here
      } catch (CASRuntimeException e) {
        assertTrue(e.getMessageKey().equals(CASRuntimeException.JCAS_GET_NTH_PAST_END));
        System.out.print("Expected Error: ");
        System.out.println(e.getMessage());
      }

      assertTrue(1 == intList.getNthElement(0));
      assertTrue(2 == intList.getNthElement(1));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testGetNthFloatList() throws Exception {
    try {

      NonEmptyFloatList floatList1 = new NonEmptyFloatList(jcas);
      floatList1.setHead((float) 2.0);
      floatList1.setTail(new EmptyFloatList(jcas));
      NonEmptyFloatList floatList = new NonEmptyFloatList(jcas);
      floatList.setHead((float) 1.0);
      floatList.setTail(floatList1);
      EmptyFloatList emptyFsList = new EmptyFloatList(jcas);

      try {
        emptyFsList.getNthElement(0);
        assertTrue(false); // error if we get here
      } catch (CASRuntimeException e) {
        assertTrue(e.getMessageKey().equals(CASRuntimeException.JCAS_GET_NTH_ON_EMPTY_LIST));
        System.out.print("Expected Error: ");
        System.out.println(e.getMessage());
      }

      try {
        floatList.getNthElement(-1);
        assertTrue(false); // error if we get here
      } catch (CASRuntimeException e) {
        assertTrue(e.getMessageKey().equals(CASRuntimeException.JCAS_GET_NTH_NEGATIVE_INDEX));
        System.out.print("Expected Error: ");
        System.out.println(e.getMessage());
      }

      try {
        floatList.getNthElement(2);
        assertTrue(false); // error if we get here
      } catch (CASRuntimeException e) {
        assertTrue(e.getMessageKey().equals(CASRuntimeException.JCAS_GET_NTH_PAST_END));
        System.out.print("Expected Error: ");
        System.out.println(e.getMessage());
      }

      assertTrue(1.0 == floatList.getNthElement(0));
      assertTrue(2.0 == floatList.getNthElement(1));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testGetNthStringList() throws Exception {
    try {
      NonEmptyStringList stringList1 = new NonEmptyStringList(jcas);
      stringList1.setHead("2");
      stringList1.setTail(new EmptyStringList(jcas));
      NonEmptyStringList stringList = new NonEmptyStringList(jcas);
      stringList.setHead("1");
      stringList.setTail(stringList1);
      EmptyStringList emptyFsList = new EmptyStringList(jcas);

      try {
        emptyFsList.getNthElement(0);
        assertTrue(false); // error if we get here
      } catch (CASRuntimeException e) {
        assertTrue(e.getMessageKey().equals(CASRuntimeException.JCAS_GET_NTH_ON_EMPTY_LIST));
        System.out.print("Expected Error: ");
        System.out.println(e.getMessage());
      }

      try {
        stringList.getNthElement(-1);
        assertTrue(false); // error if we get here
      } catch (CASRuntimeException e) {
        assertTrue(e.getMessageKey().equals(CASRuntimeException.JCAS_GET_NTH_NEGATIVE_INDEX));
        System.out.print("Expected Error: ");
        System.out.println(e.getMessage());
      }

      try {
        stringList.getNthElement(2);
        assertTrue(false); // error if we get here
      } catch (CASRuntimeException e) {
        assertTrue(e.getMessageKey().equals(CASRuntimeException.JCAS_GET_NTH_PAST_END));
        System.out.print("Expected Error: ");
        System.out.println(e.getMessage());
      }

      assertTrue("1".equals(stringList.getNthElement(0)));
      assertTrue("2".equals(stringList.getNthElement(1)));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testStringListAPI() {
    StringList sl = new EmptyStringList(jcas);
    sl = sl.push("2");
    sl = sl.push("1");

    String[] sa = new String[2];
    int i = 0;
    for (String s : sl) {
      sa[i++] = s;
    }

    String[] expected = { "1", "2" };
    assert (Arrays.equals(expected, sa));
  }

  @Test
  public void testStringArrayAPI() {
    StringArray sa = new StringArray(jcas, 3);
    String[] values = { "1", "2", "3" };
    sa.copyFromArray(values, 0, 0, 3);

    int i = 0;
    for (String s : sa) {
      assert (s.equals(values[i++]));
    }
  }

  @Test
  public void testFSListAPI() {
    FSList<TOP> sl = new EmptyFSList<>(jcas);
    TOP fs1 = new TOP(jcas);
    TOP fs2 = new TOP(jcas);
    sl = sl.push(fs2);
    sl = sl.push(fs1);

    TOP[] fss = new TOP[2];
    int i = 0;
    Iterator<TOP> it = sl.iterator();
    while (it.hasNext()) {
      fss[i++] = it.next();
    }

    i = 0;
    for (TOP s : sl) {
      fss[i++] = s;
    }

    TOP[] expected = { fs1, fs2 };
    assert (Arrays.equals(expected, fss));
  }

  @Test
  public void testFSArrayAPI() {
    FSArray sa = new FSArray<>(jcas, 2);
    TOP fs1 = new TOP(jcas);
    TOP fs2 = new TOP(jcas);
    TOP[] values = { fs1, fs2 };
    sa.copyFromArray(values, 0, 0, 2);

    int i = 0;
    sa.iterator();

    for (Object s : sa) {
      assert (s.equals(values[i++]));
    }
  }

  @Test
  public void testOtherListAPI() {
    // float and integer
    IntegerList sl = new EmptyIntegerList(jcas);
    sl = sl.push(2);
    sl = sl.push(1);

    int[] fss = new int[2];
    int i = 0;
    for (int s : sl) {
      fss[i++] = s;
    }

    int[] expected = { 1, 2 };
    assert (Arrays.equals(expected, fss));

    FloatList fl = new EmptyFloatList(jcas);
    fl = fl.push(2.0F);
    fl = fl.push(1.0F);
    float[] fls = new float[2];
    i = 0;
    for (float f : fl) {
      fls[i++] = f;
    }

    float[] expectedFloats = { 1.0f, 2.0f };
    assert (Arrays.equals(expectedFloats, fls));

    BooleanArray boa = new BooleanArray(jcas, 2);
    boa.set(0, true);
    boa.set(1, false);
    boolean[] expectedBa = { true, false };
    i = 0;
    for (boolean bov : boa) {
      assertEquals(expectedBa[i++], bov);
    }

    ByteArray bya = new ByteArray(jcas, 2);
    bya.set(0, (byte) 15);
    bya.set(1, (byte) 22);
    byte[] expectedBya = { 15, 22 };
    i = 0;
    for (byte v : bya) {
      assertEquals(expectedBya[i++], v);
    }

    ShortArray sha = new ShortArray(jcas, 2);
    sha.set(0, (short) 15);
    sha.set(1, (short) 22);
    short[] expectedSha = { 15, 22 };
    i = 0;
    for (short v : sha) {
      assertEquals(expectedSha[i++], v);
    }

    IntegerArray ina = new IntegerArray(jcas, 2);
    ina.set(0, (int) 15);
    ina.set(1, (int) 22);
    int[] expectedIna = { 15, 22 };
    i = 0;
    for (int v : ina) {
      assertEquals(expectedIna[i++], v);
    }

    IntegerArrayList inal = new IntegerArrayList(jcas, 2);
    inal.add(15);
    inal.add(22);

    OfInt ialit = inal.iterator();
    i = 0;
    while (ialit.hasNext()) {
      assertEquals(expectedIna[i++], ialit.nextInt());
    }

    i = 0;
    for (int v : inal) {
      assertEquals(expectedIna[i++], v);
    }

    LongArray loa = new LongArray(jcas, 2);
    loa.set(0, (long) 15);
    loa.set(1, (long) 22);
    long[] expectedLoa = { 15, 22 };
    i = 0;
    for (long v : loa) {
      assertEquals(expectedLoa[i++], v);
    }

    DoubleArray doa = new DoubleArray(jcas, 2);
    doa.set(0, (double) 15);
    doa.set(1, (double) 22);
    double[] expectedDoa = { 15d, 22d };
    i = 0;
    for (double v : doa) {
      Assertions.assertThat(expectedDoa[i++]).isEqualTo(v);
    }

  }

  @Test
  public void testUndefinedType() throws Exception {
    // create jcas with no type system
    JCas localJcas = createCas(new TypeSystemDescription_impl(), null, null).getJCas();
    localJcas.setDocumentText("This is a test.");

    assertThatExceptionOfType(CASRuntimeException.class) //
            .isThrownBy(() -> localJcas.getCasType(Sentence.type)) //
            .asInstanceOf(throwable(CASRuntimeException.class)) //
            .extracting(CASRuntimeException::getMessageKey) //
            .isEqualTo(CASRuntimeException.JCAS_TYPE_NOT_IN_CAS_REGISTRY);

    // check that this does not leave JCAS in an inconsistent state
    // (a check for bug UIMA-738)
    Iterator<Annotation> iter = localJcas.getAnnotationIndex().iterator();
    assertThat(iter).hasNext();
    assertThat(iter.next()) //
            .extracting(Annotation::getCoveredText) //
            .isEqualTo("This is a test.");
  }

  /*
   * skip this - takes too long private static final int largeN = 10000; public void testPerf()
   * throws Exception { try { long tFs = 0, tJCas = 0, td1 = 0, td2 = 0, tllFs = 0, td3 = 0; long
   * tFsIndex = 0, tJCasIndex = 0, tllFsIndex = 0, td4=0, td5=0, td6=0; long [] timeResults = new
   * long [12]; for (int i = 0; i < 20; i++) { timeResults = innertimingperf(); } for (int i = 0; i
   * < 10; i++) { timeResults = innertimingperf(); System.out.println(timeResults[0] + " " +
   * timeResults[1] + " " + timeResults[2] + " " + timeResults[6] + " " + timeResults[7] + " " +
   * timeResults[8]); tFs += timeResults[0]; tJCas += timeResults[1]; tllFs += timeResults[2]; td1
   * += timeResults[3]; td2 += timeResults[4]; td3 += timeResults[5]; tFsIndex += timeResults[6];
   * //System.out.print ("tFsIndex incr = " + timeResults[6]) ; tJCasIndex += timeResults[7];
   * //System.out.println(", tJCasIndex incr = " + timeResults[7]) ; tllFsIndex += timeResults[8];
   * td4 += timeResults[9]; td5 += timeResults[10]; td6 += timeResults[11]; }
   * System.out.println("Timing Test, no indexing, av createFS took: " + tFs/10 + " dummy " + td1);
   * System.out.println("Timing Test, no indexing, av JCas cr took: " + tJCas/10 + " dummy " + td2);
   * System.out.println("Timing Test, no indexing, av llCrFs took: " + tllFs/10 + " dummy " + td3);
   * System.out.println("Timing Test, indexing, av createFS took: " + tFsIndex/10 + " dummy " +
   * td4); System.out.println("Timing Test, indexing, av JCas cr took: " + tJCasIndex/10 + " dummy "
   * + td5); System.out.println("Timing Test, indexing, av llCrFs took: " + tllFsIndex/10 +
   * " dummy " + td6);
   * 
   * assertTrue ((tFs / 2) > tJCas); // JCas time should be over 2x faster than non-JCas } catch
   * (Exception e) {JUnitExtension.handleException(e); } }
   * 
   * private static final boolean DO_CHECKS = true; public long[] innertimingperf() {
   * 
   * long [] times = new long [12]; CAS cas = jcas.getCas(); FSIndexRepository ir =
   * cas.getIndexRepository(); TypeSystem ts = cas.getTypeSystem(); Type tokenType =
   * ts.getType(CASTestSetup.TOKEN_TYPE); // make a lot of FS, without adding to indexes // save in
   * array final FeatureStructure [] results = new FeatureStructure [largeN]; jcas.reset();
   * System.gc(); long startTime = System.currentTimeMillis();
   * 
   * for (int i = 0; i < results.length; i++) { results[i] = cas.createFS(tokenType); } times[0] =
   * System.currentTimeMillis() - startTime; int j = 0; for (int i = 0; i < results.length; i++) {
   * if (results[i].equals(results[0])) j ++; } times[4] = j; // this code an attempt to fool JIT
   * into keeping the results
   * 
   * jcas.reset(); System.gc(); startTime = System.currentTimeMillis(); for (int i = 0; i <
   * results.length; i++) { results[i] = new Token(jcas); } times[1] = System.currentTimeMillis() -
   * startTime; j = 0; for (int i = 0; i < results.length; i++) { if (results[i].equals(results[0]))
   * j ++; } times[3] = j; // this code an attempt to fool JIT into keeping the results // run with
   * low-level jcas.reset(); System.gc(); final int [] iresults = new int[largeN]; int tokenTypeCode
   * = ((TypeImpl)tokenType).getCode(); LowLevelCAS llCas = (LowLevelCAS)cas;
   * LowLevelIndexRepository llir = llCas.ll_getIndexRepository(); startTime =
   * System.currentTimeMillis();
   * 
   * for (int i = 0; i < iresults.length; i++) { iresults[i] = llCas.ll_createFS(tokenTypeCode); }
   * times[2] = System.currentTimeMillis() - startTime; j = 0; for (int i = 0; i < iresults.length;
   * i++) { if (iresults[i] == iresults[0]) j ++; } times[5] = j; // this code an attempt to fool
   * JIT into keeping the results
   * 
   * //***************** // with indexing //***************** jcas.reset(); System.gc(); startTime =
   * System.currentTimeMillis();
   * 
   * for (int i = 0; i < results.length; i++) { results[i] = cas.createFS(tokenType);
   * ir.addFS(results[i]); } times[6] = System.currentTimeMillis() - startTime; j = 0; for (int i =
   * 0; i < results.length; i++) { if (results[i].equals(results[0])) j ++; } times[10] = j; // this
   * code an attempt to fool JIT into keeping the results
   * 
   * jcas.reset(); System.gc(); startTime = System.currentTimeMillis();
   * 
   * for (int i = 0; i < results.length; i++) { results[i] = new Token(jcas);
   * ((Token)results[i]).addToIndexes(); } times[7] = System.currentTimeMillis() - startTime; j = 0;
   * for (int i = 0; i < results.length; i++) { if (results[i].equals(results[0])) j ++; } times[9]
   * = j; // this code an attempt to fool JIT into keeping the results // run with low-level
   * jcas.reset(); System.gc();
   * 
   * startTime = System.currentTimeMillis();
   * 
   * for (int i = 0; i < iresults.length; i++) { iresults[i] = llCas.ll_createFS(tokenTypeCode);
   * llir.ll_addFS(iresults[i], !DO_CHECKS); } times[8] = System.currentTimeMillis() - startTime; j
   * = 0; for (int i = 0; i < iresults.length; i++) { if (iresults[i] == iresults[0]) j ++; }
   * times[11] = j; // this code an attempt to fool JIT into keeping the results return times; }
   */

  /*
   * public void testCreateFS() { // Can create FS of type "Top"
   * assertTrue(this.cas.createFS(this.ts.getType(CAS.TYPE_NAME_TOP)) != null); boolean caughtExc =
   * false; // Can't create int FS. try { this.cas.createFS(this.ts.getType(CAS.TYPE_NAME_INTEGER));
   * } catch (CASRuntimeException e) { caughtExc = true; assertTrue(e.getError() ==
   * CASRuntimeException.NON_CREATABLE_TYPE); } assertTrue(caughtExc); caughtExc = false; // Can't
   * create array with CAS.createFS(). try {
   * this.cas.createFS(this.ts.getType(CAS.TYPE_NAME_FS_ARRAY)); } catch (CASRuntimeException e) {
   * caughtExc = true; assertTrue(e.getError() == CASRuntimeException.NON_CREATABLE_TYPE); }
   * assertTrue(caughtExc); caughtExc = false; // Can't create array subtype with CAS.createFS(). //
   * try { // this.cas.createFS(this.ts.getType(CASTestSetup.INT_ARRAY_SUB)); // } catch
   * (CASRuntimeException e) { // caughtExc = true; // assertTrue(e.getError() ==
   * CASRuntimeException.NON_CREATABLE_TYPE); // } // assertTrue(caughtExc); }
   * 
   * public void testCreateArrayFS() { // Has its own test class. }
   * 
   * public void testCreateIntArrayFS() { // Has its own test class. }
   * 
   * public void testCreateStringArrayFS() { // Has its own test class. } // public void
   * testCreateFilteredIterator() { // } // // public void testCommitFS() { // } // // public void
   * testGetConstraintFactory() { // } // // public void testCreateFeaturePath() { // } // // public
   * void testGetIndexRepository() { // } // // public void testFs2listIterator() { // } //
   * 
   */
}
