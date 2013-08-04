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


 getCoveredAnnotations() contains code adapted from the UIMA Subiterator class.
 */
package org.apache.uima.fit.util;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.ArrayUtils.toObject;
import static org.apache.uima.fit.util.FSCollectionFactory.create;
import static org.apache.uima.fit.util.FSCollectionFactory.createArrayFS;
import static org.apache.uima.fit.util.FSCollectionFactory.createBooleanArray;
import static org.apache.uima.fit.util.FSCollectionFactory.createByteArray;
import static org.apache.uima.fit.util.FSCollectionFactory.createDoubleArray;
import static org.apache.uima.fit.util.FSCollectionFactory.createFSArray;
import static org.apache.uima.fit.util.FSCollectionFactory.createFSList;
import static org.apache.uima.fit.util.FSCollectionFactory.createFloatArray;
import static org.apache.uima.fit.util.FSCollectionFactory.createFloatList;
import static org.apache.uima.fit.util.FSCollectionFactory.createIntArray;
import static org.apache.uima.fit.util.FSCollectionFactory.createIntegerList;
import static org.apache.uima.fit.util.FSCollectionFactory.createLongArray;
import static org.apache.uima.fit.util.FSCollectionFactory.createShortArray;
import static org.apache.uima.fit.util.FSCollectionFactory.createStringArray;
import static org.apache.uima.fit.util.FSCollectionFactory.createStringList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.type.Token;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class FSCollectionFactoryTest {
  private JCas jcas;

  private Collection<FeatureStructure> tokenFSs;

  private Collection<Annotation> tokens;

  @Before
  public void init() throws Exception {
    jcas = JCasFactory.createJCas();

    tokenFSs = new ArrayList<FeatureStructure>();
    tokens = new ArrayList<Annotation>();

    Token t1 = new Token(jcas, 0, 1);
    tokenFSs.add(t1);
    tokens.add(t1);
    t1.addToIndexes();

    Token t2 = new Token(jcas, 2, 3);
    tokenFSs.add(t2);
    tokens.add(t2);
    t2.addToIndexes();
  }

  @Test
  public void testCreateFSList() {
    assertEquals(tokens, create(createFSList(jcas, tokens)));
    assertEquals(tokens, create(createFSList(jcas, tokens), Token.class));
  }

  @Test
  public void testCreateFSArray() {
    assertEquals(tokenFSs, create(createArrayFS(jcas.getCas(), tokenFSs)));
    assertEquals(
            tokenFSs,
            create(createArrayFS(jcas.getCas(),
                    tokenFSs.toArray(new FeatureStructure[tokenFSs.size()]))));
    assertEquals(tokens, create(createArrayFS(jcas.getCas(), tokens)));
    assertEquals(tokens, create(createFSArray(jcas, tokens)));
    assertEquals(tokens,
            create(createArrayFS(jcas.getCas(), tokens.toArray(new Annotation[tokens.size()]))));
    assertEquals(tokens, create(createFSArray(jcas, tokens.toArray(new Annotation[tokens.size()]))));
    assertEquals(tokens,
            create(createFSArray(jcas, tokens.toArray(new Annotation[tokens.size()])), Token.class));
  }

  @Test
  public void testCreateBooleanArray() {
    assertEquals(asList(true, false),
            asList(toObject(createBooleanArray(jcas.getCas(), asList(true, false)).toArray())));
    assertEquals(asList(true, false),
            asList(toObject(createBooleanArray(jcas.getCas(), new boolean[] { true, false })
                    .toArray())));
    assertEquals(asList(true, false), asList(toObject(createBooleanArray(jcas, asList(true, false))
            .toArray())));
    assertEquals(asList(true, false),
            asList(toObject(createBooleanArray(jcas, new boolean[] { true, false }).toArray())));
  }

  @Test
  public void testCreateByteArray() {
    assertEquals(asList((byte) 0, (byte) 1),
            asList(toObject(createByteArray(jcas.getCas(), asList((byte) 0, (byte) 1)).toArray())));
    assertEquals(asList((byte) 0, (byte) 1),
            asList(toObject(createByteArray(jcas.getCas(), new byte[] { 0, 1 }).toArray())));
    assertEquals(asList((byte) 0, (byte) 1),
            asList(toObject(createByteArray(jcas, asList((byte) 0, (byte) 1)).toArray())));
    assertEquals(asList((byte) 0, (byte) 1),
            asList(toObject(createByteArray(jcas, new byte[] { 0, 1 }).toArray())));
  }

  @Test
  public void testCreateDoubleArray() {
    assertEquals(asList(0.0, 1.0),
            asList(toObject(createDoubleArray(jcas.getCas(), asList(0.0, 1.0)).toArray())));
    assertEquals(asList(0.0, 1.0),
            asList(toObject(createDoubleArray(jcas.getCas(), new double[] { 0.0, 1.0 }).toArray())));
    assertEquals(asList(0.0, 1.0), asList(toObject(createDoubleArray(jcas, asList(0.0, 1.0))
            .toArray())));
    assertEquals(asList(0.0, 1.0),
            asList(toObject(createDoubleArray(jcas, new double[] { 0.0, 1.0 }).toArray())));
  }

  @Test
  public void testCreateFloatArray() {
    assertEquals(asList(0.0f, 1.0f),
            asList(toObject(createFloatArray(jcas.getCas(), asList(0.0f, 1.0f)).toArray())));
    assertEquals(asList(0.0f, 1.0f),
            asList(toObject(createFloatArray(jcas.getCas(), new float[] { 0.0f, 1.0f }).toArray())));
    assertEquals(asList(0.0f, 1.0f), asList(toObject(createFloatArray(jcas, asList(0.0f, 1.0f))
            .toArray())));
    assertEquals(asList(0.0f, 1.0f),
            asList(toObject(createFloatArray(jcas, new float[] { 0.0f, 1.0f }).toArray())));
  }

  @Test
  public void testCreateFloatList() {
    assertEquals(asList(0.0f, 1.0f), create(createFloatList(jcas, asList(0.0f, 1.0f))));
  }

  @Test
  public void testCreateIntArray() {
    assertEquals(asList(0, 1), asList(toObject(createIntArray(jcas.getCas(), asList(0, 1))
            .toArray())));
    assertEquals(asList(0, 1), asList(toObject(createIntArray(jcas.getCas(), new int[] { 0, 1 })
            .toArray())));
    assertEquals(asList(0, 1), asList(toObject(createIntArray(jcas, asList(0, 1)).toArray())));
    assertEquals(asList(0, 1), asList(toObject(createIntArray(jcas, new int[] { 0, 1 }).toArray())));
  }

  @Test
  public void testCreateIntegerList() {
    assertEquals(asList(0, 1), create(createIntegerList(jcas, asList(0, 1))));
  }

  @Test
  public void testCreateLongArray() {
    assertEquals(asList(0l, 1l), asList(toObject(createLongArray(jcas.getCas(), asList(0l, 1l))
            .toArray())));
    assertEquals(asList(0l, 1l),
            asList(toObject(createLongArray(jcas.getCas(), new long[] { 0l, 1l }).toArray())));
    assertEquals(asList(0l, 1l), asList(toObject(createLongArray(jcas, asList(0l, 1l)).toArray())));
    assertEquals(asList(0l, 1l), asList(toObject(createLongArray(jcas, new long[] { 0l, 1l })
            .toArray())));
  }

  @Test
  public void testCreateShortArray() {
    assertEquals(
            asList((short) 0, (short) 1),
            asList(toObject(createShortArray(jcas.getCas(), asList((short) 0, (short) 1)).toArray())));
    assertEquals(asList((short) 0, (short) 1),
            asList(toObject(createShortArray(jcas.getCas(), new short[] { 0, 1 }).toArray())));
    assertEquals(asList((short) 0, (short) 1),
            asList(toObject(createShortArray(jcas, asList((short) 0, (short) 1)).toArray())));
    assertEquals(asList((short) 0, (short) 1),
            asList(toObject(createShortArray(jcas, new short[] { 0, 1 }).toArray())));
  }

  @Test
  public void testCreateStringArray() {
    assertEquals(asList("0", "1"), asList(createStringArray(jcas.getCas(), asList("0", "1"))
            .toArray()));
    assertEquals(asList("0", "1"),
            asList(createStringArray(jcas.getCas(), new String[] { "0", "1" }).toArray()));
    assertEquals(asList("0", "1"), asList(createStringArray(jcas, asList("0", "1")).toArray()));
    assertEquals(asList("0", "1"), asList(createStringArray(jcas, new String[] { "0", "1" })
            .toArray()));
  }

  @Test
  public void testCreateStringList() {
    assertEquals(asList("0", "1"), create(createStringList(jcas, asList("0", "1"))));
  }
}
