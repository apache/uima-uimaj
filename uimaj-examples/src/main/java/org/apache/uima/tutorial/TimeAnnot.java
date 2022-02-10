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

/**
 * Updated by JCasGen Sun Oct 08 19:34:17 EDT 2017 XML source:
 * C:/au/svnCheckouts/uv3/trunk/uimaj-v3/uimaj-examples/src/main/descriptors/tutorial/ex6/TutorialTypeSystem.xml
 * 
 * @generated
 */
public class TimeAnnot extends DateTimeAnnot {

  /**
   * @generated
   * @ordered
   */
  @SuppressWarnings("hiding")
  public final static String _TypeName = "org.apache.uima.tutorial.TimeAnnot";

  /**
   * The Constant typeIndexID.
   *
   * @generated
   * @ordered
   */
  public static final int typeIndexID = JCasRegistry.register(TimeAnnot.class);

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
  protected TimeAnnot() {
    /* intentionally empty block */}

  /**
   * Internal - constructor used by generator.
   *
   * @param type
   *          the type
   * @param casImpl
   *          the cas impl
   * @generated
   */
  public TimeAnnot(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }

  /**
   * Instantiates a new time annot.
   *
   * @param jcas
   *          the jcas
   * @generated
   */
  public TimeAnnot(JCas jcas) {
    super(jcas);
    readObject();
  }

  /**
   * Instantiates a new time annot.
   *
   * @param jcas
   *          the jcas
   * @param begin
   *          the begin
   * @param end
   *          the end
   */
  public TimeAnnot(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }

  /**
   * <!-- begin-user-doc --> Write your own initialization here <!-- end-user-doc -->*
   * 
   * @generated modifiable
   */
  private void readObject() {
  }
}
