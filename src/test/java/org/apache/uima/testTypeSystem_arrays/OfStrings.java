

/* First created by JCasGen Wed May 23 14:54:19 EDT 2012 */
package org.apache.uima.testTypeSystem_arrays;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Wed May 23 14:55:02 EDT 2012
 * XML source: C:/au/svnCheckouts/trunks/uimaj/uimaj-core/src/test/resources/ExampleCas/testTypeSystem_arrays.xml
 * @generated */
public class OfStrings extends Annotation {
  /** @generated
   * @ordered 
   */
  public final static int typeIndexID = JCasRegistry.register(OfStrings.class);
  /** @generated
   * @ordered 
   */
  public final static int type = typeIndexID;
  /** @generated  */
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected OfStrings() {}
    
  /** Internal - constructor used by generator 
   * @generated */
  public OfStrings(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public OfStrings(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public OfStrings(JCas jcas, int begin, int end) {
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
  //* Feature: f1Strings

  /** getter for f1Strings - gets 
   * @generated */
  public StringArray getF1Strings() {
    if (OfStrings_Type.featOkTst && ((OfStrings_Type)jcasType).casFeat_f1Strings == null)
      jcasType.jcas.throwFeatMissing("f1Strings", "org.apache.uima.testTypeSystem_arrays.OfStrings");
    return (StringArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((OfStrings_Type)jcasType).casFeatCode_f1Strings)));}
    
  /** setter for f1Strings - sets  
   * @generated */
  public void setF1Strings(StringArray v) {
    if (OfStrings_Type.featOkTst && ((OfStrings_Type)jcasType).casFeat_f1Strings == null)
      jcasType.jcas.throwFeatMissing("f1Strings", "org.apache.uima.testTypeSystem_arrays.OfStrings");
    jcasType.ll_cas.ll_setRefValue(addr, ((OfStrings_Type)jcasType).casFeatCode_f1Strings, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for f1Strings - gets an indexed value - 
   * @generated */
  public String getF1Strings(int i) {
    if (OfStrings_Type.featOkTst && ((OfStrings_Type)jcasType).casFeat_f1Strings == null)
      jcasType.jcas.throwFeatMissing("f1Strings", "org.apache.uima.testTypeSystem_arrays.OfStrings");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((OfStrings_Type)jcasType).casFeatCode_f1Strings), i);
    return jcasType.ll_cas.ll_getStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((OfStrings_Type)jcasType).casFeatCode_f1Strings), i);}

  /** indexed setter for f1Strings - sets an indexed value - 
   * @generated */
  public void setF1Strings(int i, String v) { 
    if (OfStrings_Type.featOkTst && ((OfStrings_Type)jcasType).casFeat_f1Strings == null)
      jcasType.jcas.throwFeatMissing("f1Strings", "org.apache.uima.testTypeSystem_arrays.OfStrings");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((OfStrings_Type)jcasType).casFeatCode_f1Strings), i);
    jcasType.ll_cas.ll_setStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((OfStrings_Type)jcasType).casFeatCode_f1Strings), i, v);}
  }

    