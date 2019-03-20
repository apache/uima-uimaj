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
import static org.apache.commons.lang3.ArrayUtils.toObject;
import static org.apache.uima.fit.util.FSCollectionFactory.create;
import static org.apache.uima.fit.util.FSCollectionFactory.*;
import static org.apache.uima.fit.util.FSCollectionFactory.createBooleanArrayFS;
import static org.apache.uima.fit.util.FSCollectionFactory.createByteArrayFS;
import static org.apache.uima.fit.util.FSCollectionFactory.createDoubleArrayFS;
import static org.apache.uima.fit.util.FSCollectionFactory.createFSArray;
import static org.apache.uima.fit.util.FSCollectionFactory.createFSList;
import static org.apache.uima.fit.util.FSCollectionFactory.createFloatArrayFS;
import static org.apache.uima.fit.util.FSCollectionFactory.createFloatList;
import static org.apache.uima.fit.util.FSCollectionFactory.createIntArrayFS;
import static org.apache.uima.fit.util.FSCollectionFactory.createIntegerList;
import static org.apache.uima.fit.util.FSCollectionFactory.createLongArrayFS;
import static org.apache.uima.fit.util.FSCollectionFactory.createShortArrayFS;
import static org.apache.uima.fit.util.FSCollectionFactory.createStringArrayFS;
import static org.apache.uima.fit.util.FSCollectionFactory.createStringList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.type.Token;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
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
            asList(toObject(createBooleanArrayFS(jcas.getCas(), asList(true, false)).toArray())));
    assertEquals(asList(true, false),
            asList(toObject(createBooleanArrayFS(jcas.getCas(), new boolean[] { true, false })
                    .toArray())));
    assertEquals(asList(true, false), asList(toObject(createBooleanArray(jcas, asList(true, false))
            .toArray())));
    assertEquals(asList(true, false),
            asList(toObject(createBooleanArray(jcas, new boolean[] { true, false }).toArray())));
  }

  @Test
  public void testCreateByteArray() {
    assertEquals(asList((byte) 0, (byte) 1),
            asList(toObject(createByteArrayFS(jcas.getCas(), asList((byte) 0, (byte) 1)).toArray())));
    assertEquals(asList((byte) 0, (byte) 1),
            asList(toObject(createByteArrayFS(jcas.getCas(), new byte[] { 0, 1 }).toArray())));
    assertEquals(asList((byte) 0, (byte) 1),
            asList(toObject(createByteArray(jcas, asList((byte) 0, (byte) 1)).toArray())));
    assertEquals(asList((byte) 0, (byte) 1),
            asList(toObject(createByteArray(jcas, new byte[] { 0, 1 }).toArray())));
  }

  @Test
  public void testCreateDoubleArray() {
    assertEquals(asList(0.0, 1.0),
            asList(toObject(createDoubleArrayFS(jcas.getCas(), asList(0.0, 1.0)).toArray())));
    assertEquals(asList(0.0, 1.0),
            asList(toObject(createDoubleArrayFS(jcas.getCas(), new double[] { 0.0, 1.0 }).toArray())));
    assertEquals(asList(0.0, 1.0), asList(toObject(createDoubleArray(jcas, asList(0.0, 1.0))
            .toArray())));
    assertEquals(asList(0.0, 1.0),
            asList(toObject(createDoubleArray(jcas, new double[] { 0.0, 1.0 }).toArray())));
  }

  @Test
  public void testCreateFloatArray() {
    assertEquals(asList(0.0f, 1.0f),
            asList(toObject(createFloatArrayFS(jcas.getCas(), asList(0.0f, 1.0f)).toArray())));
    assertEquals(asList(0.0f, 1.0f),
            asList(toObject(createFloatArrayFS(jcas.getCas(), new float[] { 0.0f, 1.0f }).toArray())));
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
    assertEquals(asList(0, 1), asList(toObject(createIntArrayFS(jcas.getCas(), asList(0, 1))
            .toArray())));
    assertEquals(asList(0, 1), asList(toObject(createIntArrayFS(jcas.getCas(), new int[] { 0, 1 })
            .toArray())));
    assertEquals(asList(0, 1), asList(toObject(createIntArray(jcas, asList(0, 1)).toArray())));
    assertEquals(asList(0, 1), asList(toObject(createIntArray(jcas, new int[] { 0, 1 }).toArray())));
  }

  @Test
  public void testFillIntegerArray() {
    assertArrayEquals(new int[] {0, 1}, fillArray(new IntegerArray(jcas, 2), asList(0, 1)).toArray());
    assertArrayEquals(new int[] {0, 1}, fillArray(new IntegerArray(jcas, 2), 0, 1).toArray());
    assertArrayEquals(new int[] {0, 1}, fillArray(new IntegerArray(jcas, 2), asList(0, 1)).toArray());
    assertArrayEquals(new int[] {0, 1}, fillArray(new IntegerArray(jcas, 2), 0, 1).toArray());
  }

  @Test
  public void testCreateIntegerList() {
    assertEquals(asList(0, 1), create(createIntegerList(jcas, asList(0, 1))));
  }

  @Test
  public void testCreateLongArray() {
    assertEquals(asList(0l, 1l), asList(toObject(createLongArrayFS(jcas.getCas(), asList(0l, 1l))
            .toArray())));
    assertEquals(asList(0l, 1l),
            asList(toObject(createLongArrayFS(jcas.getCas(), new long[] { 0l, 1l }).toArray())));
    assertEquals(asList(0l, 1l), asList(toObject(createLongArray(jcas, asList(0l, 1l)).toArray())));
    assertEquals(asList(0l, 1l), asList(toObject(createLongArray(jcas, new long[] { 0l, 1l })
            .toArray())));
  }

  @Test
  public void testCreateShortArray() {
    assertEquals(
            asList((short) 0, (short) 1),
            asList(toObject(createShortArrayFS(jcas.getCas(), asList((short) 0, (short) 1)).toArray())));
    assertEquals(asList((short) 0, (short) 1),
            asList(toObject(createShortArrayFS(jcas.getCas(), new short[] { 0, 1 }).toArray())));
    assertEquals(asList((short) 0, (short) 1),
            asList(toObject(createShortArray(jcas, asList((short) 0, (short) 1)).toArray())));
    assertEquals(asList((short) 0, (short) 1),
            asList(toObject(createShortArray(jcas, new short[] { 0, 1 }).toArray())));
  }

  @Test
  public void testCreateStringArray() {
    assertEquals(asList("0", "1"), asList(createStringArrayFS(jcas.getCas(), asList("0", "1"))
            .toArray()));
    assertEquals(asList("0", "1"),
            asList(createStringArrayFS(jcas.getCas(), new String[] { "0", "1" }).toArray()));
    assertEquals(asList("0", "1"), asList(createStringArray(jcas, asList("0", "1")).toArray()));
    assertEquals(asList("0", "1"), asList(createStringArray(jcas, "0", "1").toArray()));
  }

  @Test
  public void testFillStringArray() {
    assertEquals(asList("0", "1"),
            asList(fillArray(new StringArray(jcas, 2), asList("0", "1")).toArray()));
    assertEquals(asList("0", "1"), asList(fillArray(new StringArray(jcas, 2), "0", "1").toArray()));
    assertEquals(asList("0", "1"),
            asList(fillArray(new StringArray(jcas, 2), asList("0", "1")).toArray()));
    assertEquals(asList("0", "1"), asList(fillArray(new StringArray(jcas, 2), "0", "1").toArray()));
  }

  @Test
  public void testCreateStringList() {
    assertEquals(asList("0", "1"), create(createStringList(jcas, asList("0", "1"))));
  }
}
