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

package org.apache.uima.klt;

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.impl.JCas;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Anything that may be referred to Updated by JCasGen Thu Apr 21 11:20:08 EDT 2005 XML source:
 * descriptors/types/hutt.xml
 * 
 * @generated
 */
public class Referent extends TOP {
  public final static String INDEX = "kltReferentIndex";

  public java.util.LinkedList getOccurrences() {
    return Link.getToValues(getLinks(), HasOccurrence.class);
  }

  public java.util.LinkedList getEvidence() {
    return Link.getToValues(getLinks(), HasEvidence.class);
  }

  public void addOccurrence(Annotation o, String aComponentId) {
    Link.addLink(this, o, HasOccurrence.class, aComponentId);
  }

  public void addEvidence(TOP o, String aComponentId) {
    Link.addLink(this, o, HasEvidence.class, aComponentId);
  }

  public void setClass(String typeName) {
    try {
      setClasses(new StringArray(getCAS().getJCas(), 1));
      setClasses(0, typeName);
    } catch (CASException e) {
    }
  }

  /**
   * Determines if the given type is one of the types for the referent. Unlike
   * {@link #hasSubsumedClass(String)}, this method only returns true if the given string is
   * literally in the type list; it does not investigate whether the entity is some subtype of the
   * specified type.
   * 
   * @param typeName
   *          The fully qualified name of a type.
   * @return True if and only if typeName is in the types list
   */
  public boolean hasClass(String typeName) {
    StringArray ts = getClasses();
    if (ts == null)
      return false;
    for (int i = 0; i < ts.size(); i++) {
      String refTypeName = ts.get(i);
      if (typeName.equals(refTypeName))
        return true;
    }
    return false;
  }

  /**
   * Determines if the given type subsumes one of the types for the referent. For example, if
   * referent iso1 has a referent type org.Example.IsoscelesTriangle, and the type system of the CAS
   * that iso1 is in states that org.Example.IsoscelesTriangle is a subtype of org.Example.Triangle,
   * then the following call would return true: iso1.hasSubsumedType("org.Example.Triangle").
   * 
   * @param typeName
   *          The fully qualified name of a type.
   * @return True if and only if typeName is in the types list
   */
  public boolean hasSubsumedClass(String typeName) {
    StringArray ts = getClasses();
    if (ts == null)
      return false;
    for (int i = 0; i < ts.size(); i++) {
      String refTypeName = ts.get(i);
      TypeSystem sys = getCAS().getTypeSystem();
      Type inType = sys.getType(typeName);
      Type refType = sys.getType(refTypeName);

      if (sys.subsumes(inType, refType))
        return true;
    }
    return false;
  }

  public String toString() {
    return toString("REFERENT");
  }

  protected String toString(String className) {
    return "(" + className + " " + getId() + " " + getComponentId() + ")";
  }

  /**
   * @generated
   * @ordered
   */
  public final static int typeIndexID = JCas.getNextIndex();

  /**
   * @generated
   * @ordered
   */
  public final static int type = typeIndexID;

  /** @generated */
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /**
   * Never called. Disable default constructor
   * 
   * @generated
   */
  protected Referent() {
  }

  /**
   * Internal - constructor used by generator
   * 
   * @generated
   */
  public Referent(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }

  /** @generated */
  public Referent(JCas jcas) {
    super(jcas);
    readObject();
  }

  /**
   * <!-- begin-user-doc --> Write your own initialization here <!-- end-user-doc -->
   * 
   * @generated modifiable
   */
  private void readObject() {
  }

  // *--------------*
  // * Feature: links

  /**
   * getter for links - gets
   * 
   * @generated
   */
  public FSList getLinks() {
    if (Referent_Type.featOkTst && ((Referent_Type) jcasType).casFeat_links == null)
      JCas.throwFeatMissing("links", "org.apache.uima.klt.Referent");
    return (FSList) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr,
                    ((Referent_Type) jcasType).casFeatCode_links)));
  }

  /**
   * setter for links - sets
   * 
   * @generated
   */
  public void setLinks(FSList v) {
    if (Referent_Type.featOkTst && ((Referent_Type) jcasType).casFeat_links == null)
      JCas.throwFeatMissing("links", "org.apache.uima.klt.Referent");
    jcasType.ll_cas.ll_setRefValue(addr, ((Referent_Type) jcasType).casFeatCode_links,
                    jcasType.ll_cas.ll_getFSRef(v));
  }

  // *--------------*
  // * Feature: types

  /**
   * getter for types - gets
   * 
   * @generated
   */
  public StringArray getClasses() {
    if (Referent_Type.featOkTst && ((Referent_Type) jcasType).casFeat_classes == null)
      JCas.throwFeatMissing("classes", "org.apache.uima.klt.Referent");
    return (StringArray) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr,
                    ((Referent_Type) jcasType).casFeatCode_classes)));
  }

  /**
   * setter for types - sets
   * 
   * @generated
   */
  public void setClasses(StringArray v) {
    if (Referent_Type.featOkTst && ((Referent_Type) jcasType).casFeat_classes == null)
      JCas.throwFeatMissing("classes", "org.apache.uima.klt.Referent");
    jcasType.ll_cas.ll_setRefValue(addr, ((Referent_Type) jcasType).casFeatCode_classes,
                    jcasType.ll_cas.ll_getFSRef(v));
  }

  /**
   * indexed getter for types - gets an indexed value -
   * 
   * @generated
   */
  public String getClasses(int i) {
    if (Referent_Type.featOkTst && ((Referent_Type) jcasType).casFeat_classes == null)
      JCas.throwFeatMissing("classes", "org.apache.uima.klt.Referent");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr,
                    ((Referent_Type) jcasType).casFeatCode_classes), i);
    return jcasType.ll_cas.ll_getStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr,
                    ((Referent_Type) jcasType).casFeatCode_classes), i);
  }

  /**
   * indexed setter for types - sets an indexed value -
   * 
   * @generated
   */
  public void setClasses(int i, String v) {
    if (Referent_Type.featOkTst && ((Referent_Type) jcasType).casFeat_classes == null)
      JCas.throwFeatMissing("classes", "org.apache.uima.klt.Referent");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr,
                    ((Referent_Type) jcasType).casFeatCode_classes), i);
    jcasType.ll_cas.ll_setStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr,
                    ((Referent_Type) jcasType).casFeatCode_classes), i, v);
  }

  // *--------------*
  // * Feature: canonicalForm

  /**
   * getter for canonicalForm - gets
   * 
   * @generated
   */
  public String getCanonicalForm() {
    if (Referent_Type.featOkTst && ((Referent_Type) jcasType).casFeat_canonicalForm == null)
      JCas.throwFeatMissing("canonicalForm", "org.apache.uima.klt.Referent");
    return jcasType.ll_cas.ll_getStringValue(addr,
                    ((Referent_Type) jcasType).casFeatCode_canonicalForm);
  }

  /**
   * setter for canonicalForm - sets
   * 
   * @generated
   */
  public void setCanonicalForm(String v) {
    if (Referent_Type.featOkTst && ((Referent_Type) jcasType).casFeat_canonicalForm == null)
      JCas.throwFeatMissing("canonicalForm", "org.apache.uima.klt.Referent");
    jcasType.ll_cas
                    .ll_setStringValue(addr, ((Referent_Type) jcasType).casFeatCode_canonicalForm,
                                    v);
  }

  // *--------------*
  // * Feature: variantForms

  /**
   * getter for variantForms - gets
   * 
   * @generated
   */
  public StringList getVariantForms() {
    if (Referent_Type.featOkTst && ((Referent_Type) jcasType).casFeat_variantForms == null)
      JCas.throwFeatMissing("variantForms", "org.apache.uima.klt.Referent");
    return (StringList) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr,
                    ((Referent_Type) jcasType).casFeatCode_variantForms)));
  }

  /**
   * setter for variantForms - sets
   * 
   * @generated
   */
  public void setVariantForms(StringList v) {
    if (Referent_Type.featOkTst && ((Referent_Type) jcasType).casFeat_variantForms == null)
      JCas.throwFeatMissing("variantForms", "org.apache.uima.klt.Referent");
    jcasType.ll_cas.ll_setRefValue(addr, ((Referent_Type) jcasType).casFeatCode_variantForms,
                    jcasType.ll_cas.ll_getFSRef(v));
  }

  // *--------------*
  // * Feature: componentId

  /**
   * getter for componentId - gets
   * 
   * @generated
   */
  public String getComponentId() {
    if (Referent_Type.featOkTst && ((Referent_Type) jcasType).casFeat_componentId == null)
      JCas.throwFeatMissing("componentId", "org.apache.uima.klt.Referent");
    return jcasType.ll_cas.ll_getStringValue(addr,
                    ((Referent_Type) jcasType).casFeatCode_componentId);
  }

  /**
   * setter for componentId - sets
   * 
   * @generated
   */
  public void setComponentId(String v) {
    if (Referent_Type.featOkTst && ((Referent_Type) jcasType).casFeat_componentId == null)
      JCas.throwFeatMissing("componentId", "org.apache.uima.klt.Referent");
    jcasType.ll_cas.ll_setStringValue(addr, ((Referent_Type) jcasType).casFeatCode_componentId, v);
  }

  // *--------------*
  // * Feature: id

  /**
   * getter for id - gets
   * 
   * @generated
   */
  public String getId() {
    if (Referent_Type.featOkTst && ((Referent_Type) jcasType).casFeat_id == null)
      JCas.throwFeatMissing("id", "org.apache.uima.klt.Referent");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Referent_Type) jcasType).casFeatCode_id);
  }

  /**
   * setter for id - sets
   * 
   * @generated
   */
  public void setId(String v) {
    if (Referent_Type.featOkTst && ((Referent_Type) jcasType).casFeat_id == null)
      JCas.throwFeatMissing("id", "org.apache.uima.klt.Referent");
    jcasType.ll_cas.ll_setStringValue(addr, ((Referent_Type) jcasType).casFeatCode_id, v);
  }
}
