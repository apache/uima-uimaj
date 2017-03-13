

   
/* Apache UIMA v3 - First created by JCasGen Tue Mar 08 10:58:32 EST 2016 */

package org.apache.uima.test;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;


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
 * Updated by JCasGen Tue Mar 08 10:58:32 EST 2016
 * XML source: C:/au/svnCheckouts/branches/uimaj/experiment-v3-jcas/uimaj-json/src/test/resources/CasSerialization/desc/allTypes.xml
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
 
 
  /* *******************
   *   Feature Offsets *
   * *******************/ 
   
  /* Feature Adjusted Offsets */
  public final static int _FI_aBoolean = TypeSystemImpl.getAdjustedFeatureOffset("aBoolean");
  public final static int _FI_aByte = TypeSystemImpl.getAdjustedFeatureOffset("aByte");
  public final static int _FI_aShort = TypeSystemImpl.getAdjustedFeatureOffset("aShort");
  public final static int _FI_aInteger = TypeSystemImpl.getAdjustedFeatureOffset("aInteger");
  public final static int _FI_aLong = TypeSystemImpl.getAdjustedFeatureOffset("aLong");
  public final static int _FI_aFloat = TypeSystemImpl.getAdjustedFeatureOffset("aFloat");
  public final static int _FI_aDouble = TypeSystemImpl.getAdjustedFeatureOffset("aDouble");
  public final static int _FI_aString = TypeSystemImpl.getAdjustedFeatureOffset("aString");
  public final static int _FI_aFS = TypeSystemImpl.getAdjustedFeatureOffset("aFS");
  public final static int _FI_aArrayBoolean = TypeSystemImpl.getAdjustedFeatureOffset("aArrayBoolean");
  public final static int _FI_aArrayMrBoolean = TypeSystemImpl.getAdjustedFeatureOffset("aArrayMrBoolean");
  public final static int _FI_aArrayMrByte = TypeSystemImpl.getAdjustedFeatureOffset("aArrayMrByte");
  public final static int _FI_aArrayByte = TypeSystemImpl.getAdjustedFeatureOffset("aArrayByte");
  public final static int _FI_aArrayShort = TypeSystemImpl.getAdjustedFeatureOffset("aArrayShort");
  public final static int _FI_aArrayMrShort = TypeSystemImpl.getAdjustedFeatureOffset("aArrayMrShort");
  public final static int _FI_aArrayString = TypeSystemImpl.getAdjustedFeatureOffset("aArrayString");
  public final static int _FI_aArrayMrString = TypeSystemImpl.getAdjustedFeatureOffset("aArrayMrString");
  public final static int _FI_aListInteger = TypeSystemImpl.getAdjustedFeatureOffset("aListInteger");
  public final static int _FI_aListMrInteger = TypeSystemImpl.getAdjustedFeatureOffset("aListMrInteger");
  public final static int _FI_aListString = TypeSystemImpl.getAdjustedFeatureOffset("aListString");
  public final static int _FI_aListMrString = TypeSystemImpl.getAdjustedFeatureOffset("aListMrString");
  public final static int _FI_aListFs = TypeSystemImpl.getAdjustedFeatureOffset("aListFs");
  public final static int _FI_aListMrFs = TypeSystemImpl.getAdjustedFeatureOffset("aListMrFs");
  public final static int _FI_aArrayFS = TypeSystemImpl.getAdjustedFeatureOffset("aArrayFS");

   
  /** Never called.  Disable default constructor
   * @generated */
  protected AllTypes() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public AllTypes(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public AllTypes(JCas jcas) {
    super(jcas);
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
  public boolean getABoolean() { return _getBooleanValueNc(_FI_aBoolean);}
    
  /** setter for aBoolean - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setABoolean(boolean v) {
    _setBooleanValueNfc(_FI_aBoolean, v);
  }    
    
   
    
  //*--------------*
  //* Feature: aByte

  /** getter for aByte - gets 
   * @generated
   * @return value of the feature 
   */
  public byte getAByte() { return _getByteValueNc(_FI_aByte);}
    
  /** setter for aByte - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAByte(byte v) {
    _setByteValueNfc(_FI_aByte, v);
  }    
    
   
    
  //*--------------*
  //* Feature: aShort

  /** getter for aShort - gets 
   * @generated
   * @return value of the feature 
   */
  public short getAShort() { return _getShortValueNc(_FI_aShort);}
    
  /** setter for aShort - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAShort(short v) {
    _setShortValueNfc(_FI_aShort, v);
  }    
    
   
    
  //*--------------*
  //* Feature: aInteger

  /** getter for aInteger - gets 
   * @generated
   * @return value of the feature 
   */
  public int getAInteger() { return _getIntValueNc(_FI_aInteger);}
    
  /** setter for aInteger - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAInteger(int v) {
    _setIntValueNfc(_FI_aInteger, v);
  }    
    
   
    
  //*--------------*
  //* Feature: aLong

  /** getter for aLong - gets 
   * @generated
   * @return value of the feature 
   */
  public long getALong() { return _getLongValueNc(_FI_aLong);}
    
  /** setter for aLong - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setALong(long v) {
    _setLongValueNfc(_FI_aLong, v);
  }    
    
   
    
  //*--------------*
  //* Feature: aFloat

  /** getter for aFloat - gets 
   * @generated
   * @return value of the feature 
   */
  public float getAFloat() { return _getFloatValueNc(_FI_aFloat);}
    
  /** setter for aFloat - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAFloat(float v) {
    _setFloatValueNfc(_FI_aFloat, v);
  }    
    
   
    
  //*--------------*
  //* Feature: aDouble

  /** getter for aDouble - gets 
   * @generated
   * @return value of the feature 
   */
  public double getADouble() { return _getDoubleValueNc(_FI_aDouble);}
    
  /** setter for aDouble - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setADouble(double v) {
    _setDoubleValueNfc(_FI_aDouble, v);
  }    
    
   
    
  //*--------------*
  //* Feature: aString

  /** getter for aString - gets 
   * @generated
   * @return value of the feature 
   */
  public String getAString() { return _getStringValueNc(_FI_aString);}
    
  /** setter for aString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAString(String v) {
    _setStringValueNfc(_FI_aString, v);
  }    
    
   
    
  //*--------------*
  //* Feature: aFS

  /** getter for aFS - gets 
   * @generated
   * @return value of the feature 
   */
  public Annotation getAFS() { return (Annotation)(_getFeatureValueNc(_FI_aFS));}
    
  /** setter for aFS - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAFS(Annotation v) {
    _setFeatureValueNcWj(_FI_aFS, v);
  }    
    
   
    
  //*--------------*
  //* Feature: aArrayBoolean

  /** getter for aArrayBoolean - gets 
   * @generated
   * @return value of the feature 
   */
  public BooleanArray getAArrayBoolean() { return (BooleanArray)(_getFeatureValueNc(_FI_aArrayBoolean));}
    
  /** setter for aArrayBoolean - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayBoolean(BooleanArray v) {
    _setFeatureValueNcWj(_FI_aArrayBoolean, v);
  }    
    
    
  /** indexed getter for aArrayBoolean - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public boolean getAArrayBoolean(int i) {
     return ((BooleanArray)(_getFeatureValueNc(_FI_aArrayBoolean))).get(i);} 

  /** indexed setter for aArrayBoolean - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayBoolean(int i, boolean v) {
    ((BooleanArray)(_getFeatureValueNc(_FI_aArrayBoolean))).set(i, v);
  }  
   
    
  //*--------------*
  //* Feature: aArrayMrBoolean

  /** getter for aArrayMrBoolean - gets 
   * @generated
   * @return value of the feature 
   */
  public BooleanArray getAArrayMrBoolean() { return (BooleanArray)(_getFeatureValueNc(_FI_aArrayMrBoolean));}
    
  /** setter for aArrayMrBoolean - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayMrBoolean(BooleanArray v) {
    _setFeatureValueNcWj(_FI_aArrayMrBoolean, v);
  }    
    
    
  /** indexed getter for aArrayMrBoolean - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public boolean getAArrayMrBoolean(int i) {
     return ((BooleanArray)(_getFeatureValueNc(_FI_aArrayMrBoolean))).get(i);} 

  /** indexed setter for aArrayMrBoolean - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayMrBoolean(int i, boolean v) {
    ((BooleanArray)(_getFeatureValueNc(_FI_aArrayMrBoolean))).set(i, v);
  }  
   
    
  //*--------------*
  //* Feature: aArrayMrByte

  /** getter for aArrayMrByte - gets 
   * @generated
   * @return value of the feature 
   */
  public ByteArray getAArrayMrByte() { return (ByteArray)(_getFeatureValueNc(_FI_aArrayMrByte));}
    
  /** setter for aArrayMrByte - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayMrByte(ByteArray v) {
    _setFeatureValueNcWj(_FI_aArrayMrByte, v);
  }    
    
    
  /** indexed getter for aArrayMrByte - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public byte getAArrayMrByte(int i) {
     return ((ByteArray)(_getFeatureValueNc(_FI_aArrayMrByte))).get(i);} 

  /** indexed setter for aArrayMrByte - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayMrByte(int i, byte v) {
    ((ByteArray)(_getFeatureValueNc(_FI_aArrayMrByte))).set(i, v);
  }  
   
    
  //*--------------*
  //* Feature: aArrayByte

  /** getter for aArrayByte - gets 
   * @generated
   * @return value of the feature 
   */
  public ByteArray getAArrayByte() { return (ByteArray)(_getFeatureValueNc(_FI_aArrayByte));}
    
  /** setter for aArrayByte - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayByte(ByteArray v) {
    _setFeatureValueNcWj(_FI_aArrayByte, v);
  }    
    
    
  /** indexed getter for aArrayByte - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public byte getAArrayByte(int i) {
     return ((ByteArray)(_getFeatureValueNc(_FI_aArrayByte))).get(i);} 

  /** indexed setter for aArrayByte - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayByte(int i, byte v) {
    ((ByteArray)(_getFeatureValueNc(_FI_aArrayByte))).set(i, v);
  }  
   
    
  //*--------------*
  //* Feature: aArrayShort

  /** getter for aArrayShort - gets 
   * @generated
   * @return value of the feature 
   */
  public ShortArray getAArrayShort() { return (ShortArray)(_getFeatureValueNc(_FI_aArrayShort));}
    
  /** setter for aArrayShort - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayShort(ShortArray v) {
    _setFeatureValueNcWj(_FI_aArrayShort, v);
  }    
    
    
  /** indexed getter for aArrayShort - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public short getAArrayShort(int i) {
     return ((ShortArray)(_getFeatureValueNc(_FI_aArrayShort))).get(i);} 

  /** indexed setter for aArrayShort - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayShort(int i, short v) {
    ((ShortArray)(_getFeatureValueNc(_FI_aArrayShort))).set(i, v);
  }  
   
    
  //*--------------*
  //* Feature: aArrayMrShort

  /** getter for aArrayMrShort - gets 
   * @generated
   * @return value of the feature 
   */
  public ShortArray getAArrayMrShort() { return (ShortArray)(_getFeatureValueNc(_FI_aArrayMrShort));}
    
  /** setter for aArrayMrShort - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayMrShort(ShortArray v) {
    _setFeatureValueNcWj(_FI_aArrayMrShort, v);
  }    
    
    
  /** indexed getter for aArrayMrShort - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public short getAArrayMrShort(int i) {
     return ((ShortArray)(_getFeatureValueNc(_FI_aArrayMrShort))).get(i);} 

  /** indexed setter for aArrayMrShort - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayMrShort(int i, short v) {
    ((ShortArray)(_getFeatureValueNc(_FI_aArrayMrShort))).set(i, v);
  }  
   
    
  //*--------------*
  //* Feature: aArrayString

  /** getter for aArrayString - gets 
   * @generated
   * @return value of the feature 
   */
  public StringArray getAArrayString() { return (StringArray)(_getFeatureValueNc(_FI_aArrayString));}
    
  /** setter for aArrayString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayString(StringArray v) {
    _setFeatureValueNcWj(_FI_aArrayString, v);
  }    
    
    
  /** indexed getter for aArrayString - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public String getAArrayString(int i) {
     return ((StringArray)(_getFeatureValueNc(_FI_aArrayString))).get(i);} 

  /** indexed setter for aArrayString - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayString(int i, String v) {
    ((StringArray)(_getFeatureValueNc(_FI_aArrayString))).set(i, v);
  }  
   
    
  //*--------------*
  //* Feature: aArrayMrString

  /** getter for aArrayMrString - gets 
   * @generated
   * @return value of the feature 
   */
  public StringArray getAArrayMrString() { return (StringArray)(_getFeatureValueNc(_FI_aArrayMrString));}
    
  /** setter for aArrayMrString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayMrString(StringArray v) {
    _setFeatureValueNcWj(_FI_aArrayMrString, v);
  }    
    
    
  /** indexed getter for aArrayMrString - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public String getAArrayMrString(int i) {
     return ((StringArray)(_getFeatureValueNc(_FI_aArrayMrString))).get(i);} 

  /** indexed setter for aArrayMrString - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayMrString(int i, String v) {
    ((StringArray)(_getFeatureValueNc(_FI_aArrayMrString))).set(i, v);
  }  
   
    
  //*--------------*
  //* Feature: aListInteger

  /** getter for aListInteger - gets 
   * @generated
   * @return value of the feature 
   */
  public IntegerList getAListInteger() { return (IntegerList)(_getFeatureValueNc(_FI_aListInteger));}
    
  /** setter for aListInteger - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAListInteger(IntegerList v) {
    _setFeatureValueNcWj(_FI_aListInteger, v);
  }    
    
   
    
  //*--------------*
  //* Feature: aListMrInteger

  /** getter for aListMrInteger - gets 
   * @generated
   * @return value of the feature 
   */
  public IntegerList getAListMrInteger() { return (IntegerList)(_getFeatureValueNc(_FI_aListMrInteger));}
    
  /** setter for aListMrInteger - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAListMrInteger(IntegerList v) {
    _setFeatureValueNcWj(_FI_aListMrInteger, v);
  }    
    
   
    
  //*--------------*
  //* Feature: aListString

  /** getter for aListString - gets 
   * @generated
   * @return value of the feature 
   */
  public StringList getAListString() { return (StringList)(_getFeatureValueNc(_FI_aListString));}
    
  /** setter for aListString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAListString(StringList v) {
    _setFeatureValueNcWj(_FI_aListString, v);
  }    
    
   
    
  //*--------------*
  //* Feature: aListMrString

  /** getter for aListMrString - gets 
   * @generated
   * @return value of the feature 
   */
  public StringList getAListMrString() { return (StringList)(_getFeatureValueNc(_FI_aListMrString));}
    
  /** setter for aListMrString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAListMrString(StringList v) {
    _setFeatureValueNcWj(_FI_aListMrString, v);
  }    
    
   
    
  //*--------------*
  //* Feature: aListFs

  /** getter for aListFs - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getAListFs() { return (FSList)(_getFeatureValueNc(_FI_aListFs));}
    
  /** setter for aListFs - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAListFs(FSList v) {
    _setFeatureValueNcWj(_FI_aListFs, v);
  }    
    
   
    
  //*--------------*
  //* Feature: aListMrFs

  /** getter for aListMrFs - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getAListMrFs() { return (FSList)(_getFeatureValueNc(_FI_aListMrFs));}
    
  /** setter for aListMrFs - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAListMrFs(FSList v) {
    _setFeatureValueNcWj(_FI_aListMrFs, v);
  }    
    
   
    
  //*--------------*
  //* Feature: aArrayFS

  /** getter for aArrayFS - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getAArrayFS() { return (FSArray)(_getFeatureValueNc(_FI_aArrayFS));}
    
  /** setter for aArrayFS - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayFS(FSArray v) {
    _setFeatureValueNcWj(_FI_aArrayFS, v);
  }    
    
    
  /** indexed getter for aArrayFS - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public Annotation getAArrayFS(int i) {
     return (Annotation)(((FSArray)(_getFeatureValueNc(_FI_aArrayFS))).get(i));} 

  /** indexed setter for aArrayFS - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayFS(int i, Annotation v) {
    ((FSArray)(_getFeatureValueNc(_FI_aArrayFS))).set(i, v);
  }  
  }

    