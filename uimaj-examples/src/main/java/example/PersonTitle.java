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
package example;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * A Personal Title. Updated by JCasGen Mon May 23 17:48:43 EDT 2005 XML source:
 * c:\a\eclipse\301jxe\jedii_examples\descriptors\analysis_engine\NamesAndPersonTitles_TAE.xml
 *
 * @generated
 */
public class PersonTitle extends Annotation {

  /**
   * @generated
   * @ordered
   */
  @SuppressWarnings("hiding")
  public final static String _TypeName = "example.PersonTitle";

  /**
   * The Constant typeIndexID.
   *
   * @generated
   * @ordered
   */
  public static final int typeIndexID = JCasRegistry.register(PersonTitle.class);

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

  /*
   * ******************* Feature Offsets *
   *******************/

  public final static String _FeatName_Kind = "Kind";

  /* Feature Adjusted Offsets */
  private final static CallSite _FC_Kind = TypeSystemImpl.createCallSite(PersonTitle.class, "Kind");
  private final static MethodHandle _FH_Kind = _FC_Kind.dynamicInvoker();

  /**
   * Never called. Disable default constructor
   *
   * @generated
   */
  protected PersonTitle() {
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
  public PersonTitle(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }

  /**
   * Instantiates a new person title.
   *
   * @param jcas
   *          the jcas
   * @generated
   */
  public PersonTitle(JCas jcas) {
    super(jcas);
    readObject();
  }

  /**
   * Instantiates a new person title.
   *
   * @param jcas
   *          the jcas
   * @param begin
   *          the begin
   * @param end
   *          the end
   */
  public PersonTitle(JCas jcas, int begin, int end) {
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

  // *--------------*
  // * Feature: Kind
  /**
   * getter for Kind - gets The kind of title - Civilian, Military, or Government.
   *
   * @return the kind
   * @generated
   */
  public String getKind() {
    return _getStringValueNc(wrapGetIntCatchException(_FH_Kind));
  }

  /**
   * setter for Kind - sets The kind of title - Civilian, Military, or Government.
   *
   * @param v
   *          the new kind
   * @generated
   */
  public void setKind(String v) {
    _setStringValueNfc(wrapGetIntCatchException(_FH_Kind), v);
  }

}
