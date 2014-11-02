

/* First created by JCasGen Sat Nov 01 07:15:36 EDT 2014 */
package org.apache.uima.test;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.jcas.cas.IntegerList;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.jcas.cas.BooleanArray;


/** 
 * Updated by JCasGen Sat Nov 01 07:15:36 EDT 2014
 * XML source: C:/au/svnCheckouts/trunk/uimaj/uimaj-json/src/test/resources/CasSerialization/desc/allTypes.xml
 * @generated */
public class AllTypes extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(AllTypes.class);
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
  protected AllTypes() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public AllTypes(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public AllTypes(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public AllTypes(JCas jcas, int begin, int end) {
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
  //* Feature: aBoolean

  /** getter for aBoolean - gets 
   * @generated
   * @return value of the feature 
   */
  public boolean getABoolean() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aBoolean == null)
      jcasType.jcas.throwFeatMissing("aBoolean", "org.apache.uima.test.AllTypes");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aBoolean);}
    
  /** setter for aBoolean - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setABoolean(boolean v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aBoolean == null)
      jcasType.jcas.throwFeatMissing("aBoolean", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aBoolean, v);}    
   
    
  //*--------------*
  //* Feature: aByte

  /** getter for aByte - gets 
   * @generated
   * @return value of the feature 
   */
  public byte getAByte() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aByte == null)
      jcasType.jcas.throwFeatMissing("aByte", "org.apache.uima.test.AllTypes");
    return jcasType.ll_cas.ll_getByteValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aByte);}
    
  /** setter for aByte - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAByte(byte v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aByte == null)
      jcasType.jcas.throwFeatMissing("aByte", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setByteValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aByte, v);}    
   
    
  //*--------------*
  //* Feature: aShort

  /** getter for aShort - gets 
   * @generated
   * @return value of the feature 
   */
  public short getAShort() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aShort == null)
      jcasType.jcas.throwFeatMissing("aShort", "org.apache.uima.test.AllTypes");
    return jcasType.ll_cas.ll_getShortValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aShort);}
    
  /** setter for aShort - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAShort(short v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aShort == null)
      jcasType.jcas.throwFeatMissing("aShort", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setShortValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aShort, v);}    
   
    
  //*--------------*
  //* Feature: aInteger

  /** getter for aInteger - gets 
   * @generated
   * @return value of the feature 
   */
  public int getAInteger() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aInteger == null)
      jcasType.jcas.throwFeatMissing("aInteger", "org.apache.uima.test.AllTypes");
    return jcasType.ll_cas.ll_getIntValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aInteger);}
    
  /** setter for aInteger - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAInteger(int v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aInteger == null)
      jcasType.jcas.throwFeatMissing("aInteger", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setIntValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aInteger, v);}    
   
    
  //*--------------*
  //* Feature: aLong

  /** getter for aLong - gets 
   * @generated
   * @return value of the feature 
   */
  public long getALong() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aLong == null)
      jcasType.jcas.throwFeatMissing("aLong", "org.apache.uima.test.AllTypes");
    return jcasType.ll_cas.ll_getLongValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aLong);}
    
  /** setter for aLong - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setALong(long v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aLong == null)
      jcasType.jcas.throwFeatMissing("aLong", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setLongValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aLong, v);}    
   
    
  //*--------------*
  //* Feature: aFloat

  /** getter for aFloat - gets 
   * @generated
   * @return value of the feature 
   */
  public float getAFloat() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aFloat == null)
      jcasType.jcas.throwFeatMissing("aFloat", "org.apache.uima.test.AllTypes");
    return jcasType.ll_cas.ll_getFloatValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aFloat);}
    
  /** setter for aFloat - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAFloat(float v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aFloat == null)
      jcasType.jcas.throwFeatMissing("aFloat", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setFloatValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aFloat, v);}    
   
    
  //*--------------*
  //* Feature: aDouble

  /** getter for aDouble - gets 
   * @generated
   * @return value of the feature 
   */
  public double getADouble() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aDouble == null)
      jcasType.jcas.throwFeatMissing("aDouble", "org.apache.uima.test.AllTypes");
    return jcasType.ll_cas.ll_getDoubleValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aDouble);}
    
  /** setter for aDouble - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setADouble(double v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aDouble == null)
      jcasType.jcas.throwFeatMissing("aDouble", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setDoubleValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aDouble, v);}    
   
    
  //*--------------*
  //* Feature: aString

  /** getter for aString - gets 
   * @generated
   * @return value of the feature 
   */
  public String getAString() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aString == null)
      jcasType.jcas.throwFeatMissing("aString", "org.apache.uima.test.AllTypes");
    return jcasType.ll_cas.ll_getStringValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aString);}
    
  /** setter for aString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAString(String v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aString == null)
      jcasType.jcas.throwFeatMissing("aString", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setStringValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aString, v);}    
   
    
  //*--------------*
  //* Feature: aFS

  /** getter for aFS - gets 
   * @generated
   * @return value of the feature 
   */
  public Annotation getAFS() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aFS == null)
      jcasType.jcas.throwFeatMissing("aFS", "org.apache.uima.test.AllTypes");
    return (Annotation)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aFS)));}
    
  /** setter for aFS - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAFS(Annotation v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aFS == null)
      jcasType.jcas.throwFeatMissing("aFS", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aFS, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: aArrayBoolean

  /** getter for aArrayBoolean - gets 
   * @generated
   * @return value of the feature 
   */
  public BooleanArray getAArrayBoolean() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayBoolean == null)
      jcasType.jcas.throwFeatMissing("aArrayBoolean", "org.apache.uima.test.AllTypes");
    return (BooleanArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayBoolean)));}
    
  /** setter for aArrayBoolean - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayBoolean(BooleanArray v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayBoolean == null)
      jcasType.jcas.throwFeatMissing("aArrayBoolean", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayBoolean, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for aArrayBoolean - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public boolean getAArrayBoolean(int i) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayBoolean == null)
      jcasType.jcas.throwFeatMissing("aArrayBoolean", "org.apache.uima.test.AllTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayBoolean), i);
    return jcasType.ll_cas.ll_getBooleanArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayBoolean), i);}

  /** indexed setter for aArrayBoolean - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayBoolean(int i, boolean v) { 
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayBoolean == null)
      jcasType.jcas.throwFeatMissing("aArrayBoolean", "org.apache.uima.test.AllTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayBoolean), i);
    jcasType.ll_cas.ll_setBooleanArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayBoolean), i, v);}
   
    
  //*--------------*
  //* Feature: aArrayMrBoolean

  /** getter for aArrayMrBoolean - gets 
   * @generated
   * @return value of the feature 
   */
  public BooleanArray getAArrayMrBoolean() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayMrBoolean == null)
      jcasType.jcas.throwFeatMissing("aArrayMrBoolean", "org.apache.uima.test.AllTypes");
    return (BooleanArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrBoolean)));}
    
  /** setter for aArrayMrBoolean - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayMrBoolean(BooleanArray v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayMrBoolean == null)
      jcasType.jcas.throwFeatMissing("aArrayMrBoolean", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrBoolean, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for aArrayMrBoolean - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public boolean getAArrayMrBoolean(int i) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayMrBoolean == null)
      jcasType.jcas.throwFeatMissing("aArrayMrBoolean", "org.apache.uima.test.AllTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrBoolean), i);
    return jcasType.ll_cas.ll_getBooleanArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrBoolean), i);}

  /** indexed setter for aArrayMrBoolean - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayMrBoolean(int i, boolean v) { 
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayMrBoolean == null)
      jcasType.jcas.throwFeatMissing("aArrayMrBoolean", "org.apache.uima.test.AllTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrBoolean), i);
    jcasType.ll_cas.ll_setBooleanArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrBoolean), i, v);}
   
    
  //*--------------*
  //* Feature: aArrayMrByte

  /** getter for aArrayMrByte - gets 
   * @generated
   * @return value of the feature 
   */
  public ByteArray getAArrayMrByte() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayMrByte == null)
      jcasType.jcas.throwFeatMissing("aArrayMrByte", "org.apache.uima.test.AllTypes");
    return (ByteArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrByte)));}
    
  /** setter for aArrayMrByte - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayMrByte(ByteArray v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayMrByte == null)
      jcasType.jcas.throwFeatMissing("aArrayMrByte", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrByte, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for aArrayMrByte - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public byte getAArrayMrByte(int i) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayMrByte == null)
      jcasType.jcas.throwFeatMissing("aArrayMrByte", "org.apache.uima.test.AllTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrByte), i);
    return jcasType.ll_cas.ll_getByteArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrByte), i);}

  /** indexed setter for aArrayMrByte - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayMrByte(int i, byte v) { 
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayMrByte == null)
      jcasType.jcas.throwFeatMissing("aArrayMrByte", "org.apache.uima.test.AllTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrByte), i);
    jcasType.ll_cas.ll_setByteArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrByte), i, v);}
   
    
  //*--------------*
  //* Feature: aArrayByte

  /** getter for aArrayByte - gets 
   * @generated
   * @return value of the feature 
   */
  public ByteArray getAArrayByte() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayByte == null)
      jcasType.jcas.throwFeatMissing("aArrayByte", "org.apache.uima.test.AllTypes");
    return (ByteArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayByte)));}
    
  /** setter for aArrayByte - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayByte(ByteArray v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayByte == null)
      jcasType.jcas.throwFeatMissing("aArrayByte", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayByte, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for aArrayByte - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public byte getAArrayByte(int i) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayByte == null)
      jcasType.jcas.throwFeatMissing("aArrayByte", "org.apache.uima.test.AllTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayByte), i);
    return jcasType.ll_cas.ll_getByteArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayByte), i);}

  /** indexed setter for aArrayByte - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayByte(int i, byte v) { 
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayByte == null)
      jcasType.jcas.throwFeatMissing("aArrayByte", "org.apache.uima.test.AllTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayByte), i);
    jcasType.ll_cas.ll_setByteArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayByte), i, v);}
   
    
  //*--------------*
  //* Feature: aArrayShort

  /** getter for aArrayShort - gets 
   * @generated
   * @return value of the feature 
   */
  public ShortArray getAArrayShort() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayShort == null)
      jcasType.jcas.throwFeatMissing("aArrayShort", "org.apache.uima.test.AllTypes");
    return (ShortArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayShort)));}
    
  /** setter for aArrayShort - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayShort(ShortArray v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayShort == null)
      jcasType.jcas.throwFeatMissing("aArrayShort", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayShort, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for aArrayShort - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public short getAArrayShort(int i) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayShort == null)
      jcasType.jcas.throwFeatMissing("aArrayShort", "org.apache.uima.test.AllTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayShort), i);
    return jcasType.ll_cas.ll_getShortArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayShort), i);}

  /** indexed setter for aArrayShort - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayShort(int i, short v) { 
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayShort == null)
      jcasType.jcas.throwFeatMissing("aArrayShort", "org.apache.uima.test.AllTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayShort), i);
    jcasType.ll_cas.ll_setShortArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayShort), i, v);}
   
    
  //*--------------*
  //* Feature: aArrayMrShort

  /** getter for aArrayMrShort - gets 
   * @generated
   * @return value of the feature 
   */
  public ShortArray getAArrayMrShort() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayMrShort == null)
      jcasType.jcas.throwFeatMissing("aArrayMrShort", "org.apache.uima.test.AllTypes");
    return (ShortArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrShort)));}
    
  /** setter for aArrayMrShort - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayMrShort(ShortArray v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayMrShort == null)
      jcasType.jcas.throwFeatMissing("aArrayMrShort", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrShort, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for aArrayMrShort - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public short getAArrayMrShort(int i) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayMrShort == null)
      jcasType.jcas.throwFeatMissing("aArrayMrShort", "org.apache.uima.test.AllTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrShort), i);
    return jcasType.ll_cas.ll_getShortArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrShort), i);}

  /** indexed setter for aArrayMrShort - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayMrShort(int i, short v) { 
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayMrShort == null)
      jcasType.jcas.throwFeatMissing("aArrayMrShort", "org.apache.uima.test.AllTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrShort), i);
    jcasType.ll_cas.ll_setShortArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrShort), i, v);}
   
    
  //*--------------*
  //* Feature: aArrayString

  /** getter for aArrayString - gets 
   * @generated
   * @return value of the feature 
   */
  public StringArray getAArrayString() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayString == null)
      jcasType.jcas.throwFeatMissing("aArrayString", "org.apache.uima.test.AllTypes");
    return (StringArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayString)));}
    
  /** setter for aArrayString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayString(StringArray v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayString == null)
      jcasType.jcas.throwFeatMissing("aArrayString", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayString, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for aArrayString - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public String getAArrayString(int i) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayString == null)
      jcasType.jcas.throwFeatMissing("aArrayString", "org.apache.uima.test.AllTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayString), i);
    return jcasType.ll_cas.ll_getStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayString), i);}

  /** indexed setter for aArrayString - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayString(int i, String v) { 
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayString == null)
      jcasType.jcas.throwFeatMissing("aArrayString", "org.apache.uima.test.AllTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayString), i);
    jcasType.ll_cas.ll_setStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayString), i, v);}
   
    
  //*--------------*
  //* Feature: aArrayMrString

  /** getter for aArrayMrString - gets 
   * @generated
   * @return value of the feature 
   */
  public StringArray getAArrayMrString() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayMrString == null)
      jcasType.jcas.throwFeatMissing("aArrayMrString", "org.apache.uima.test.AllTypes");
    return (StringArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrString)));}
    
  /** setter for aArrayMrString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayMrString(StringArray v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayMrString == null)
      jcasType.jcas.throwFeatMissing("aArrayMrString", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrString, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for aArrayMrString - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public String getAArrayMrString(int i) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayMrString == null)
      jcasType.jcas.throwFeatMissing("aArrayMrString", "org.apache.uima.test.AllTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrString), i);
    return jcasType.ll_cas.ll_getStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrString), i);}

  /** indexed setter for aArrayMrString - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayMrString(int i, String v) { 
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayMrString == null)
      jcasType.jcas.throwFeatMissing("aArrayMrString", "org.apache.uima.test.AllTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrString), i);
    jcasType.ll_cas.ll_setStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayMrString), i, v);}
   
    
  //*--------------*
  //* Feature: aListInteger

  /** getter for aListInteger - gets 
   * @generated
   * @return value of the feature 
   */
  public IntegerList getAListInteger() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aListInteger == null)
      jcasType.jcas.throwFeatMissing("aListInteger", "org.apache.uima.test.AllTypes");
    return (IntegerList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aListInteger)));}
    
  /** setter for aListInteger - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAListInteger(IntegerList v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aListInteger == null)
      jcasType.jcas.throwFeatMissing("aListInteger", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aListInteger, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: aListMrInteger

  /** getter for aListMrInteger - gets 
   * @generated
   * @return value of the feature 
   */
  public IntegerList getAListMrInteger() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aListMrInteger == null)
      jcasType.jcas.throwFeatMissing("aListMrInteger", "org.apache.uima.test.AllTypes");
    return (IntegerList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aListMrInteger)));}
    
  /** setter for aListMrInteger - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAListMrInteger(IntegerList v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aListMrInteger == null)
      jcasType.jcas.throwFeatMissing("aListMrInteger", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aListMrInteger, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: aListString

  /** getter for aListString - gets 
   * @generated
   * @return value of the feature 
   */
  public StringList getAListString() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aListString == null)
      jcasType.jcas.throwFeatMissing("aListString", "org.apache.uima.test.AllTypes");
    return (StringList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aListString)));}
    
  /** setter for aListString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAListString(StringList v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aListString == null)
      jcasType.jcas.throwFeatMissing("aListString", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aListString, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: aListMrString

  /** getter for aListMrString - gets 
   * @generated
   * @return value of the feature 
   */
  public StringList getAListMrString() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aListMrString == null)
      jcasType.jcas.throwFeatMissing("aListMrString", "org.apache.uima.test.AllTypes");
    return (StringList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aListMrString)));}
    
  /** setter for aListMrString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAListMrString(StringList v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aListMrString == null)
      jcasType.jcas.throwFeatMissing("aListMrString", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aListMrString, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: aListFs

  /** getter for aListFs - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getAListFs() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aListFs == null)
      jcasType.jcas.throwFeatMissing("aListFs", "org.apache.uima.test.AllTypes");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aListFs)));}
    
  /** setter for aListFs - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAListFs(FSList v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aListFs == null)
      jcasType.jcas.throwFeatMissing("aListFs", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aListFs, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: aListMrFs

  /** getter for aListMrFs - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getAListMrFs() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aListMrFs == null)
      jcasType.jcas.throwFeatMissing("aListMrFs", "org.apache.uima.test.AllTypes");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aListMrFs)));}
    
  /** setter for aListMrFs - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAListMrFs(FSList v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aListMrFs == null)
      jcasType.jcas.throwFeatMissing("aListMrFs", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aListMrFs, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: aArrayFS

  /** getter for aArrayFS - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getAArrayFS() {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayFS == null)
      jcasType.jcas.throwFeatMissing("aArrayFS", "org.apache.uima.test.AllTypes");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayFS)));}
    
  /** setter for aArrayFS - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayFS(FSArray v) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayFS == null)
      jcasType.jcas.throwFeatMissing("aArrayFS", "org.apache.uima.test.AllTypes");
    jcasType.ll_cas.ll_setRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayFS, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for aArrayFS - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public Annotation getAArrayFS(int i) {
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayFS == null)
      jcasType.jcas.throwFeatMissing("aArrayFS", "org.apache.uima.test.AllTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayFS), i);
    return (Annotation)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayFS), i)));}

  /** indexed setter for aArrayFS - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayFS(int i, Annotation v) { 
    if (AllTypes_Type.featOkTst && ((AllTypes_Type)jcasType).casFeat_aArrayFS == null)
      jcasType.jcas.throwFeatMissing("aArrayFS", "org.apache.uima.test.AllTypes");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayFS), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AllTypes_Type)jcasType).casFeatCode_aArrayFS), i, jcasType.ll_cas.ll_getFSRef(v));}
  }

    