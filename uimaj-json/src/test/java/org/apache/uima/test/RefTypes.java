

/* First created by JCasGen Sat Nov 01 07:38:59 EDT 2014 */
package org.apache.uima.test;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Sat Nov 01 07:38:59 EDT 2014
 * XML source: C:/au/svnCheckouts/trunk/uimaj/uimaj-json/src/test/resources/CasSerialization/desc/refTypes.xml
 * @generated */
public class RefTypes extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(RefTypes.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected RefTypes() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public RefTypes(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public RefTypes(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public RefTypes(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: aFS

  /** getter for aFS - gets 
   * @generated
   * @return value of the feature 
   */
  public Annotation getAFS() {
    if (RefTypes_Type.featOkTst && ((RefTypes_Type)jcasType).casFeat_aFS == null)
      jcasType.jcas.throwFeatMissing("aFS", "org.apache.uima.test.RefTypes");
    return (Annotation)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((RefTypes_Type)jcasType).casFeatCode_aFS)));}
    
  /** setter for aFS - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAFS(Annotation v) {
    if (RefTypes_Type.featOkTst && ((RefTypes_Type)jcasType).casFeat_aFS == null)
      jcasType.jcas.throwFeatMissing("aFS", "org.apache.uima.test.RefTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((RefTypes_Type)jcasType).casFeatCode_aFS, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: aListFs

  /** getter for aListFs - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getAListFs() {
    if (RefTypes_Type.featOkTst && ((RefTypes_Type)jcasType).casFeat_aListFs == null)
      jcasType.jcas.throwFeatMissing("aListFs", "org.apache.uima.test.RefTypes");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((RefTypes_Type)jcasType).casFeatCode_aListFs)));}
    
  /** setter for aListFs - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAListFs(FSList v) {
    if (RefTypes_Type.featOkTst && ((RefTypes_Type)jcasType).casFeat_aListFs == null)
      jcasType.jcas.throwFeatMissing("aListFs", "org.apache.uima.test.RefTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((RefTypes_Type)jcasType).casFeatCode_aListFs, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: aArrayFS

  /** getter for aArrayFS - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getAArrayFS() {
    if (RefTypes_Type.featOkTst && ((RefTypes_Type)jcasType).casFeat_aArrayFS == null)
      jcasType.jcas.throwFeatMissing("aArrayFS", "org.apache.uima.test.RefTypes");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((RefTypes_Type)jcasType).casFeatCode_aArrayFS)));}
    
  /** setter for aArrayFS - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayFS(FSArray v) {
    if (RefTypes_Type.featOkTst && ((RefTypes_Type)jcasType).casFeat_aArrayFS == null)
      jcasType.jcas.throwFeatMissing("aArrayFS", "org.apache.uima.test.RefTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((RefTypes_Type)jcasType).casFeatCode_aArrayFS, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for aArrayFS - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public Annotation getAArrayFS(int i) {
    if (RefTypes_Type.featOkTst && ((RefTypes_Type)jcasType).casFeat_aArrayFS == null)
      jcasType.jcas.throwFeatMissing("aArrayFS", "org.apache.uima.test.RefTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((RefTypes_Type)jcasType).casFeatCode_aArrayFS), i);
    return (Annotation)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((RefTypes_Type)jcasType).casFeatCode_aArrayFS), i)));}

  /** indexed setter for aArrayFS - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayFS(int i, Annotation v) { 
    if (RefTypes_Type.featOkTst && ((RefTypes_Type)jcasType).casFeat_aArrayFS == null)
      jcasType.jcas.throwFeatMissing("aArrayFS", "org.apache.uima.test.RefTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((RefTypes_Type)jcasType).casFeatCode_aArrayFS), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((RefTypes_Type)jcasType).casFeatCode_aArrayFS), i, jcasType.ll_cas.ll_getFSRef(v));}
  }

    