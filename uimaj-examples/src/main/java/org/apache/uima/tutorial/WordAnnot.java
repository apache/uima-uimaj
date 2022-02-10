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
package org.apache.uima.tutorial;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Updated by JCasGen Mon Nov 29 15:02:38 EST 2004 XML source: C:/Program
 * Files/apache/uima/examples/descriptors/tutorial/ex6/TutorialTypeSystem.xml
 *
 * @generated
 */
public class WordAnnot extends Annotation {

  /**
   * The Constant typeIndexID.
   *
   * @generated
   * @ordered
   */
  public static final int typeIndexID = JCasRegistry.register(WordAnnot.class);

  /**
   * The Constant type.
   *
   * @generated
   * @ordered
   */
  public static final int type = typeIndexID;

  /**
   * Gets the type index ID.
   *
   * @return the type index ID
   * @generated
   */
  @Override
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /**
   * Never called. Disable default constructor
   *
   * @generated
   */
  protected WordAnnot() {
  }

  /**
   * Internal - constructor used by generator.
   *
   * @param type
   *          the type
   * @param casImpl
   *          the cas impl
   * @generated
   */
  public WordAnnot(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }

  /**
   * Instantiates a new word annot.
   *
   * @param jcas
   *          the jcas
   * @generated
   */
  public WordAnnot(JCas jcas) {
    super(jcas);
    readObject();
  }

  /**
   * Instantiates a new word annot.
   *
   * @param jcas
   *          the jcas
   * @param begin
   *          the begin
   * @param end
   *          the end
   */
  public WordAnnot(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }

  /**
   * <!-- begin-user-doc --> Write your own initialization here <!-- end-user-doc -->.
   *
   * @generated modifiable
   */
  private void readObject() {
  }
}
