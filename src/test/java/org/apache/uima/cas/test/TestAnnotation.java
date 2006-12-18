

/* First created by JCasGen Mon Dec 18 11:22:10 CET 2006 */
package org.apache.uima.cas.test;

import org.apache.uima.jcas.impl.JCas; 
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Mon Dec 18 11:22:10 CET 2006
 * XML source: C:/code/trunk/uimaj-core/src/test/resources/CASTests/desc/StringSubtypeTest.xml
 * @generated */
public class TestAnnotation extends Annotation {
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
  protected TestAnnotation() {}
    
  /** Internal - constructor used by generator 
   * @generated */
  public TestAnnotation(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public TestAnnotation(JCas jcas) {
    super(jcas);
    readObject();   
  } 
  
  /** @generated */
  public TestAnnotation(JCas jcas, int begin, int end) {
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
    if (TestAnnotation_Type.featOkTst && ((TestAnnotation_Type)jcasType).casFeat_stringSetFeature == null)
      JCas.throwFeatMissing("stringSetFeature", "org.apache.uima.cas.test.TestAnnotation");
    return jcasType.ll_cas.ll_getStringValue(addr, ((TestAnnotation_Type)jcasType).casFeatCode_stringSetFeature);}
    
  /** setter for stringSetFeature - sets  
   * @generated */
  public void setStringSetFeature(String v) {
    if (TestAnnotation_Type.featOkTst && ((TestAnnotation_Type)jcasType).casFeat_stringSetFeature == null)
      JCas.throwFeatMissing("stringSetFeature", "org.apache.uima.cas.test.TestAnnotation");
    jcasType.ll_cas.ll_setStringValue(addr, ((TestAnnotation_Type)jcasType).casFeatCode_stringSetFeature, v);}    
  }

    