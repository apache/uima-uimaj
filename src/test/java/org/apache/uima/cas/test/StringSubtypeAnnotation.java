

/* First created by JCasGen Fri Dec 22 14:02:31 CET 2006 */
package org.apache.uima.cas.test;

import org.apache.uima.jcas.impl.JCas; 
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Fri Dec 22 14:02:31 CET 2006
 * XML source: C:/code/trunk/uimaj-core/src/test/resources/CASTests/desc/StringSubtypeTest.xml
 * @generated */
public class StringSubtypeAnnotation extends Annotation {
  /** @generated
   * @ordered 
   */
  public final static int typeIndexID = JCas.getNextIndex();
  /** @generated
   * @ordered 
   */
  public final static int type = typeIndexID;
  /** @generated  */
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected StringSubtypeAnnotation() {}
    
  /** Internal - constructor used by generator 
   * @generated */
  public StringSubtypeAnnotation(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public StringSubtypeAnnotation(JCas jcas) {
    super(jcas);
    readObject();   
  } 
  
  /** @generated */
  public StringSubtypeAnnotation(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** <!-- begin-user-doc -->
    * Write your own initialization here
    * <!-- end-user-doc -->
  @generated modifiable */
  private void readObject() {}
     
 
    
  //*--------------*
  //* Feature: stringSetFeature

  /** getter for stringSetFeature - gets 
   * @generated */
  public String getStringSetFeature() {
    if (StringSubtypeAnnotation_Type.featOkTst && ((StringSubtypeAnnotation_Type)jcasType).casFeat_stringSetFeature == null)
      JCas.throwFeatMissing("stringSetFeature", "org.apache.uima.cas.test.StringSubtypeAnnotation");
    return jcasType.ll_cas.ll_getStringValue(addr, ((StringSubtypeAnnotation_Type)jcasType).casFeatCode_stringSetFeature);}
    
  /** setter for stringSetFeature - sets  
   * @generated */
  public void setStringSetFeature(String v) {
    if (StringSubtypeAnnotation_Type.featOkTst && ((StringSubtypeAnnotation_Type)jcasType).casFeat_stringSetFeature == null)
      JCas.throwFeatMissing("stringSetFeature", "org.apache.uima.cas.test.StringSubtypeAnnotation");
    jcasType.ll_cas.ll_setStringValue(addr, ((StringSubtypeAnnotation_Type)jcasType).casFeatCode_stringSetFeature, v);}    
  }

    