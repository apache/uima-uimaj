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
package org.apache.uima.examples.tokenizer;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * The Class Sentence.
 */
public class Sentence extends Annotation {

  /**
   * @generated
   * @ordered
   */
  @SuppressWarnings("hiding")
  public final static String _TypeName = "org.apache.uima.examples.tokenizer.Sentence";

  /** The Constant typeIndexID. */
  public static final int typeIndexID = JCasRegistry.register(Sentence.class);

  /** The Constant type. */
  public static final int type = typeIndexID;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.tcas.Annotation#getTypeIndexID()
   */
  @Override
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /**
   * Instantiates a new sentence.
   */
  // Never called. Disable default constructor
  protected Sentence() {
  }

  /**
   * Internal - Constructor used by generator.
   *
   * @param type
   *          the type
   * @param casImpl
   *          the cas impl
   */
  public Sentence(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
  }

  /**
   * Instantiates a new sentence.
   *
   * @param jcas
   *          the jcas
   */
  public Sentence(JCas jcas) {
    super(jcas);
  }

  /**
   * Instantiates a new sentence.
   *
   * @param jcas
   *          the jcas
   * @param start
   *          the start
   * @param end
   *          the end
   */
  public Sentence(JCas jcas, int start, int end) {
    super(jcas, start, end);
  }

  /**
   * <!-- begin-user-doc --> Write your own initialization here <!-- end-user-doc -->
   *
   * @generated modifiable
   */
  private void readObject() {
    /* default - does nothing empty block */}

}
