
/* First created by JCasGen Sat Nov 01 07:15:36 EDT 2014 */
package org.apache.uima.test;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** 
 * Updated by JCasGen Sat Nov 01 07:15:36 EDT 2014
 * @generated */
public class AllTypes_Type extends Annotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (AllTypes_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = AllTypes_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new AllTypes(addr, AllTypes_Type.this);
  			   AllTypes_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new AllTypes(addr, AllTypes_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = AllTypes.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("org.apache.uima.test.AllTypes");
 
  /** @generated */
  final Feature casFeat_aBoolean;
  /** @generated */
  final int     casFeatCode_aBoolean;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public boolean getABoolean(int addr) {
        if (featOkTst && casFeat_aBoolean == null)
      jcas.throwFeatMissing("aBoolean", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getBooleanValue(addr, casFeatCode_aBoolean);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setABoolean(int addr, boolean v) {
        if (featOkTst && casFeat_aBoolean == null)
      jcas.throwFeatMissing("aBoolean", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setBooleanValue(addr, casFeatCode_aBoolean, v);}
    
  
 
  /** @generated */
  final Feature casFeat_aByte;
  /** @generated */
  final int     casFeatCode_aByte;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public byte getAByte(int addr) {
        if (featOkTst && casFeat_aByte == null)
      jcas.throwFeatMissing("aByte", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getByteValue(addr, casFeatCode_aByte);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAByte(int addr, byte v) {
        if (featOkTst && casFeat_aByte == null)
      jcas.throwFeatMissing("aByte", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setByteValue(addr, casFeatCode_aByte, v);}
    
  
 
  /** @generated */
  final Feature casFeat_aShort;
  /** @generated */
  final int     casFeatCode_aShort;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public short getAShort(int addr) {
        if (featOkTst && casFeat_aShort == null)
      jcas.throwFeatMissing("aShort", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getShortValue(addr, casFeatCode_aShort);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAShort(int addr, short v) {
        if (featOkTst && casFeat_aShort == null)
      jcas.throwFeatMissing("aShort", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setShortValue(addr, casFeatCode_aShort, v);}
    
  
 
  /** @generated */
  final Feature casFeat_aInteger;
  /** @generated */
  final int     casFeatCode_aInteger;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAInteger(int addr) {
        if (featOkTst && casFeat_aInteger == null)
      jcas.throwFeatMissing("aInteger", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getIntValue(addr, casFeatCode_aInteger);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAInteger(int addr, int v) {
        if (featOkTst && casFeat_aInteger == null)
      jcas.throwFeatMissing("aInteger", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setIntValue(addr, casFeatCode_aInteger, v);}
    
  
 
  /** @generated */
  final Feature casFeat_aLong;
  /** @generated */
  final int     casFeatCode_aLong;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public long getALong(int addr) {
        if (featOkTst && casFeat_aLong == null)
      jcas.throwFeatMissing("aLong", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getLongValue(addr, casFeatCode_aLong);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setALong(int addr, long v) {
        if (featOkTst && casFeat_aLong == null)
      jcas.throwFeatMissing("aLong", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setLongValue(addr, casFeatCode_aLong, v);}
    
  
 
  /** @generated */
  final Feature casFeat_aFloat;
  /** @generated */
  final int     casFeatCode_aFloat;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public float getAFloat(int addr) {
        if (featOkTst && casFeat_aFloat == null)
      jcas.throwFeatMissing("aFloat", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getFloatValue(addr, casFeatCode_aFloat);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAFloat(int addr, float v) {
        if (featOkTst && casFeat_aFloat == null)
      jcas.throwFeatMissing("aFloat", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setFloatValue(addr, casFeatCode_aFloat, v);}
    
  
 
  /** @generated */
  final Feature casFeat_aDouble;
  /** @generated */
  final int     casFeatCode_aDouble;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public double getADouble(int addr) {
        if (featOkTst && casFeat_aDouble == null)
      jcas.throwFeatMissing("aDouble", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getDoubleValue(addr, casFeatCode_aDouble);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setADouble(int addr, double v) {
        if (featOkTst && casFeat_aDouble == null)
      jcas.throwFeatMissing("aDouble", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setDoubleValue(addr, casFeatCode_aDouble, v);}
    
  
 
  /** @generated */
  final Feature casFeat_aString;
  /** @generated */
  final int     casFeatCode_aString;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getAString(int addr) {
        if (featOkTst && casFeat_aString == null)
      jcas.throwFeatMissing("aString", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getStringValue(addr, casFeatCode_aString);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAString(int addr, String v) {
        if (featOkTst && casFeat_aString == null)
      jcas.throwFeatMissing("aString", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setStringValue(addr, casFeatCode_aString, v);}
    
  
 
  /** @generated */
  final Feature casFeat_aFS;
  /** @generated */
  final int     casFeatCode_aFS;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAFS(int addr) {
        if (featOkTst && casFeat_aFS == null)
      jcas.throwFeatMissing("aFS", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aFS);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAFS(int addr, int v) {
        if (featOkTst && casFeat_aFS == null)
      jcas.throwFeatMissing("aFS", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aFS, v);}
    
  
 
  /** @generated */
  final Feature casFeat_aArrayBoolean;
  /** @generated */
  final int     casFeatCode_aArrayBoolean;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAArrayBoolean(int addr) {
        if (featOkTst && casFeat_aArrayBoolean == null)
      jcas.throwFeatMissing("aArrayBoolean", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aArrayBoolean);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAArrayBoolean(int addr, int v) {
        if (featOkTst && casFeat_aArrayBoolean == null)
      jcas.throwFeatMissing("aArrayBoolean", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aArrayBoolean, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public boolean getAArrayBoolean(int addr, int i) {
        if (featOkTst && casFeat_aArrayBoolean == null)
      jcas.throwFeatMissing("aArrayBoolean", "org.apache.uima.test.AllTypes");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getBooleanArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayBoolean), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayBoolean), i);
	return ll_cas.ll_getBooleanArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayBoolean), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setAArrayBoolean(int addr, int i, boolean v) {
        if (featOkTst && casFeat_aArrayBoolean == null)
      jcas.throwFeatMissing("aArrayBoolean", "org.apache.uima.test.AllTypes");
    if (lowLevelTypeChecks)
      ll_cas.ll_setBooleanArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayBoolean), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayBoolean), i);
    ll_cas.ll_setBooleanArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayBoolean), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_aArrayMrBoolean;
  /** @generated */
  final int     casFeatCode_aArrayMrBoolean;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAArrayMrBoolean(int addr) {
        if (featOkTst && casFeat_aArrayMrBoolean == null)
      jcas.throwFeatMissing("aArrayMrBoolean", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrBoolean);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAArrayMrBoolean(int addr, int v) {
        if (featOkTst && casFeat_aArrayMrBoolean == null)
      jcas.throwFeatMissing("aArrayMrBoolean", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aArrayMrBoolean, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public boolean getAArrayMrBoolean(int addr, int i) {
        if (featOkTst && casFeat_aArrayMrBoolean == null)
      jcas.throwFeatMissing("aArrayMrBoolean", "org.apache.uima.test.AllTypes");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getBooleanArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrBoolean), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrBoolean), i);
	return ll_cas.ll_getBooleanArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrBoolean), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setAArrayMrBoolean(int addr, int i, boolean v) {
        if (featOkTst && casFeat_aArrayMrBoolean == null)
      jcas.throwFeatMissing("aArrayMrBoolean", "org.apache.uima.test.AllTypes");
    if (lowLevelTypeChecks)
      ll_cas.ll_setBooleanArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrBoolean), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrBoolean), i);
    ll_cas.ll_setBooleanArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrBoolean), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_aArrayMrByte;
  /** @generated */
  final int     casFeatCode_aArrayMrByte;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAArrayMrByte(int addr) {
        if (featOkTst && casFeat_aArrayMrByte == null)
      jcas.throwFeatMissing("aArrayMrByte", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrByte);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAArrayMrByte(int addr, int v) {
        if (featOkTst && casFeat_aArrayMrByte == null)
      jcas.throwFeatMissing("aArrayMrByte", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aArrayMrByte, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public byte getAArrayMrByte(int addr, int i) {
        if (featOkTst && casFeat_aArrayMrByte == null)
      jcas.throwFeatMissing("aArrayMrByte", "org.apache.uima.test.AllTypes");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getByteArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrByte), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrByte), i);
	return ll_cas.ll_getByteArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrByte), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setAArrayMrByte(int addr, int i, byte v) {
        if (featOkTst && casFeat_aArrayMrByte == null)
      jcas.throwFeatMissing("aArrayMrByte", "org.apache.uima.test.AllTypes");
    if (lowLevelTypeChecks)
      ll_cas.ll_setByteArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrByte), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrByte), i);
    ll_cas.ll_setByteArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrByte), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_aArrayByte;
  /** @generated */
  final int     casFeatCode_aArrayByte;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAArrayByte(int addr) {
        if (featOkTst && casFeat_aArrayByte == null)
      jcas.throwFeatMissing("aArrayByte", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aArrayByte);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAArrayByte(int addr, int v) {
        if (featOkTst && casFeat_aArrayByte == null)
      jcas.throwFeatMissing("aArrayByte", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aArrayByte, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public byte getAArrayByte(int addr, int i) {
        if (featOkTst && casFeat_aArrayByte == null)
      jcas.throwFeatMissing("aArrayByte", "org.apache.uima.test.AllTypes");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getByteArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayByte), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayByte), i);
	return ll_cas.ll_getByteArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayByte), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setAArrayByte(int addr, int i, byte v) {
        if (featOkTst && casFeat_aArrayByte == null)
      jcas.throwFeatMissing("aArrayByte", "org.apache.uima.test.AllTypes");
    if (lowLevelTypeChecks)
      ll_cas.ll_setByteArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayByte), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayByte), i);
    ll_cas.ll_setByteArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayByte), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_aArrayShort;
  /** @generated */
  final int     casFeatCode_aArrayShort;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAArrayShort(int addr) {
        if (featOkTst && casFeat_aArrayShort == null)
      jcas.throwFeatMissing("aArrayShort", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aArrayShort);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAArrayShort(int addr, int v) {
        if (featOkTst && casFeat_aArrayShort == null)
      jcas.throwFeatMissing("aArrayShort", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aArrayShort, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public short getAArrayShort(int addr, int i) {
        if (featOkTst && casFeat_aArrayShort == null)
      jcas.throwFeatMissing("aArrayShort", "org.apache.uima.test.AllTypes");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getShortArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayShort), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayShort), i);
	return ll_cas.ll_getShortArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayShort), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setAArrayShort(int addr, int i, short v) {
        if (featOkTst && casFeat_aArrayShort == null)
      jcas.throwFeatMissing("aArrayShort", "org.apache.uima.test.AllTypes");
    if (lowLevelTypeChecks)
      ll_cas.ll_setShortArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayShort), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayShort), i);
    ll_cas.ll_setShortArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayShort), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_aArrayMrShort;
  /** @generated */
  final int     casFeatCode_aArrayMrShort;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAArrayMrShort(int addr) {
        if (featOkTst && casFeat_aArrayMrShort == null)
      jcas.throwFeatMissing("aArrayMrShort", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrShort);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAArrayMrShort(int addr, int v) {
        if (featOkTst && casFeat_aArrayMrShort == null)
      jcas.throwFeatMissing("aArrayMrShort", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aArrayMrShort, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public short getAArrayMrShort(int addr, int i) {
        if (featOkTst && casFeat_aArrayMrShort == null)
      jcas.throwFeatMissing("aArrayMrShort", "org.apache.uima.test.AllTypes");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getShortArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrShort), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrShort), i);
	return ll_cas.ll_getShortArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrShort), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setAArrayMrShort(int addr, int i, short v) {
        if (featOkTst && casFeat_aArrayMrShort == null)
      jcas.throwFeatMissing("aArrayMrShort", "org.apache.uima.test.AllTypes");
    if (lowLevelTypeChecks)
      ll_cas.ll_setShortArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrShort), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrShort), i);
    ll_cas.ll_setShortArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrShort), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_aArrayString;
  /** @generated */
  final int     casFeatCode_aArrayString;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAArrayString(int addr) {
        if (featOkTst && casFeat_aArrayString == null)
      jcas.throwFeatMissing("aArrayString", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aArrayString);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAArrayString(int addr, int v) {
        if (featOkTst && casFeat_aArrayString == null)
      jcas.throwFeatMissing("aArrayString", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aArrayString, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public String getAArrayString(int addr, int i) {
        if (featOkTst && casFeat_aArrayString == null)
      jcas.throwFeatMissing("aArrayString", "org.apache.uima.test.AllTypes");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayString), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayString), i);
	return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayString), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setAArrayString(int addr, int i, String v) {
        if (featOkTst && casFeat_aArrayString == null)
      jcas.throwFeatMissing("aArrayString", "org.apache.uima.test.AllTypes");
    if (lowLevelTypeChecks)
      ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayString), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayString), i);
    ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayString), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_aArrayMrString;
  /** @generated */
  final int     casFeatCode_aArrayMrString;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAArrayMrString(int addr) {
        if (featOkTst && casFeat_aArrayMrString == null)
      jcas.throwFeatMissing("aArrayMrString", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrString);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAArrayMrString(int addr, int v) {
        if (featOkTst && casFeat_aArrayMrString == null)
      jcas.throwFeatMissing("aArrayMrString", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aArrayMrString, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public String getAArrayMrString(int addr, int i) {
        if (featOkTst && casFeat_aArrayMrString == null)
      jcas.throwFeatMissing("aArrayMrString", "org.apache.uima.test.AllTypes");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrString), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrString), i);
	return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrString), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setAArrayMrString(int addr, int i, String v) {
        if (featOkTst && casFeat_aArrayMrString == null)
      jcas.throwFeatMissing("aArrayMrString", "org.apache.uima.test.AllTypes");
    if (lowLevelTypeChecks)
      ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrString), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrString), i);
    ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayMrString), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_aListInteger;
  /** @generated */
  final int     casFeatCode_aListInteger;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAListInteger(int addr) {
        if (featOkTst && casFeat_aListInteger == null)
      jcas.throwFeatMissing("aListInteger", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aListInteger);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAListInteger(int addr, int v) {
        if (featOkTst && casFeat_aListInteger == null)
      jcas.throwFeatMissing("aListInteger", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aListInteger, v);}
    
  
 
  /** @generated */
  final Feature casFeat_aListMrInteger;
  /** @generated */
  final int     casFeatCode_aListMrInteger;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAListMrInteger(int addr) {
        if (featOkTst && casFeat_aListMrInteger == null)
      jcas.throwFeatMissing("aListMrInteger", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aListMrInteger);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAListMrInteger(int addr, int v) {
        if (featOkTst && casFeat_aListMrInteger == null)
      jcas.throwFeatMissing("aListMrInteger", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aListMrInteger, v);}
    
  
 
  /** @generated */
  final Feature casFeat_aListString;
  /** @generated */
  final int     casFeatCode_aListString;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAListString(int addr) {
        if (featOkTst && casFeat_aListString == null)
      jcas.throwFeatMissing("aListString", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aListString);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAListString(int addr, int v) {
        if (featOkTst && casFeat_aListString == null)
      jcas.throwFeatMissing("aListString", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aListString, v);}
    
  
 
  /** @generated */
  final Feature casFeat_aListMrString;
  /** @generated */
  final int     casFeatCode_aListMrString;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAListMrString(int addr) {
        if (featOkTst && casFeat_aListMrString == null)
      jcas.throwFeatMissing("aListMrString", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aListMrString);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAListMrString(int addr, int v) {
        if (featOkTst && casFeat_aListMrString == null)
      jcas.throwFeatMissing("aListMrString", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aListMrString, v);}
    
  
 
  /** @generated */
  final Feature casFeat_aListFs;
  /** @generated */
  final int     casFeatCode_aListFs;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAListFs(int addr) {
        if (featOkTst && casFeat_aListFs == null)
      jcas.throwFeatMissing("aListFs", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aListFs);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAListFs(int addr, int v) {
        if (featOkTst && casFeat_aListFs == null)
      jcas.throwFeatMissing("aListFs", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aListFs, v);}
    
  
 
  /** @generated */
  final Feature casFeat_aListMrFs;
  /** @generated */
  final int     casFeatCode_aListMrFs;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAListMrFs(int addr) {
        if (featOkTst && casFeat_aListMrFs == null)
      jcas.throwFeatMissing("aListMrFs", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aListMrFs);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAListMrFs(int addr, int v) {
        if (featOkTst && casFeat_aListMrFs == null)
      jcas.throwFeatMissing("aListMrFs", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aListMrFs, v);}
    
  
 
  /** @generated */
  final Feature casFeat_aArrayFS;
  /** @generated */
  final int     casFeatCode_aArrayFS;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAArrayFS(int addr) {
        if (featOkTst && casFeat_aArrayFS == null)
      jcas.throwFeatMissing("aArrayFS", "org.apache.uima.test.AllTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aArrayFS);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAArrayFS(int addr, int v) {
        if (featOkTst && casFeat_aArrayFS == null)
      jcas.throwFeatMissing("aArrayFS", "org.apache.uima.test.AllTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aArrayFS, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getAArrayFS(int addr, int i) {
        if (featOkTst && casFeat_aArrayFS == null)
      jcas.throwFeatMissing("aArrayFS", "org.apache.uima.test.AllTypes");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayFS), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayFS), i);
	return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayFS), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setAArrayFS(int addr, int i, int v) {
        if (featOkTst && casFeat_aArrayFS == null)
      jcas.throwFeatMissing("aArrayFS", "org.apache.uima.test.AllTypes");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayFS), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayFS), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayFS), i, v);
  }
 



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public AllTypes_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_aBoolean = jcas.getRequiredFeatureDE(casType, "aBoolean", "uima.cas.Boolean", featOkTst);
    casFeatCode_aBoolean  = (null == casFeat_aBoolean) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aBoolean).getCode();

 
    casFeat_aByte = jcas.getRequiredFeatureDE(casType, "aByte", "uima.cas.Byte", featOkTst);
    casFeatCode_aByte  = (null == casFeat_aByte) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aByte).getCode();

 
    casFeat_aShort = jcas.getRequiredFeatureDE(casType, "aShort", "uima.cas.Short", featOkTst);
    casFeatCode_aShort  = (null == casFeat_aShort) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aShort).getCode();

 
    casFeat_aInteger = jcas.getRequiredFeatureDE(casType, "aInteger", "uima.cas.Integer", featOkTst);
    casFeatCode_aInteger  = (null == casFeat_aInteger) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aInteger).getCode();

 
    casFeat_aLong = jcas.getRequiredFeatureDE(casType, "aLong", "uima.cas.Long", featOkTst);
    casFeatCode_aLong  = (null == casFeat_aLong) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aLong).getCode();

 
    casFeat_aFloat = jcas.getRequiredFeatureDE(casType, "aFloat", "uima.cas.Float", featOkTst);
    casFeatCode_aFloat  = (null == casFeat_aFloat) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aFloat).getCode();

 
    casFeat_aDouble = jcas.getRequiredFeatureDE(casType, "aDouble", "uima.cas.Double", featOkTst);
    casFeatCode_aDouble  = (null == casFeat_aDouble) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aDouble).getCode();

 
    casFeat_aString = jcas.getRequiredFeatureDE(casType, "aString", "uima.cas.String", featOkTst);
    casFeatCode_aString  = (null == casFeat_aString) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aString).getCode();

 
    casFeat_aFS = jcas.getRequiredFeatureDE(casType, "aFS", "uima.tcas.Annotation", featOkTst);
    casFeatCode_aFS  = (null == casFeat_aFS) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aFS).getCode();

 
    casFeat_aArrayBoolean = jcas.getRequiredFeatureDE(casType, "aArrayBoolean", "uima.cas.BooleanArray", featOkTst);
    casFeatCode_aArrayBoolean  = (null == casFeat_aArrayBoolean) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aArrayBoolean).getCode();

 
    casFeat_aArrayMrBoolean = jcas.getRequiredFeatureDE(casType, "aArrayMrBoolean", "uima.cas.BooleanArray", featOkTst);
    casFeatCode_aArrayMrBoolean  = (null == casFeat_aArrayMrBoolean) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aArrayMrBoolean).getCode();

 
    casFeat_aArrayMrByte = jcas.getRequiredFeatureDE(casType, "aArrayMrByte", "uima.cas.ByteArray", featOkTst);
    casFeatCode_aArrayMrByte  = (null == casFeat_aArrayMrByte) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aArrayMrByte).getCode();

 
    casFeat_aArrayByte = jcas.getRequiredFeatureDE(casType, "aArrayByte", "uima.cas.ByteArray", featOkTst);
    casFeatCode_aArrayByte  = (null == casFeat_aArrayByte) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aArrayByte).getCode();

 
    casFeat_aArrayShort = jcas.getRequiredFeatureDE(casType, "aArrayShort", "uima.cas.ShortArray", featOkTst);
    casFeatCode_aArrayShort  = (null == casFeat_aArrayShort) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aArrayShort).getCode();

 
    casFeat_aArrayMrShort = jcas.getRequiredFeatureDE(casType, "aArrayMrShort", "uima.cas.ShortArray", featOkTst);
    casFeatCode_aArrayMrShort  = (null == casFeat_aArrayMrShort) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aArrayMrShort).getCode();

 
    casFeat_aArrayString = jcas.getRequiredFeatureDE(casType, "aArrayString", "uima.cas.StringArray", featOkTst);
    casFeatCode_aArrayString  = (null == casFeat_aArrayString) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aArrayString).getCode();

 
    casFeat_aArrayMrString = jcas.getRequiredFeatureDE(casType, "aArrayMrString", "uima.cas.StringArray", featOkTst);
    casFeatCode_aArrayMrString  = (null == casFeat_aArrayMrString) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aArrayMrString).getCode();

 
    casFeat_aListInteger = jcas.getRequiredFeatureDE(casType, "aListInteger", "uima.cas.IntegerList", featOkTst);
    casFeatCode_aListInteger  = (null == casFeat_aListInteger) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aListInteger).getCode();

 
    casFeat_aListMrInteger = jcas.getRequiredFeatureDE(casType, "aListMrInteger", "uima.cas.IntegerList", featOkTst);
    casFeatCode_aListMrInteger  = (null == casFeat_aListMrInteger) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aListMrInteger).getCode();

 
    casFeat_aListString = jcas.getRequiredFeatureDE(casType, "aListString", "uima.cas.StringList", featOkTst);
    casFeatCode_aListString  = (null == casFeat_aListString) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aListString).getCode();

 
    casFeat_aListMrString = jcas.getRequiredFeatureDE(casType, "aListMrString", "uima.cas.StringList", featOkTst);
    casFeatCode_aListMrString  = (null == casFeat_aListMrString) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aListMrString).getCode();

 
    casFeat_aListFs = jcas.getRequiredFeatureDE(casType, "aListFs", "uima.cas.FSList", featOkTst);
    casFeatCode_aListFs  = (null == casFeat_aListFs) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aListFs).getCode();

 
    casFeat_aListMrFs = jcas.getRequiredFeatureDE(casType, "aListMrFs", "uima.cas.FSList", featOkTst);
    casFeatCode_aListMrFs  = (null == casFeat_aListMrFs) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aListMrFs).getCode();

 
    casFeat_aArrayFS = jcas.getRequiredFeatureDE(casType, "aArrayFS", "uima.cas.FSArray", featOkTst);
    casFeatCode_aArrayFS  = (null == casFeat_aArrayFS) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aArrayFS).getCode();

  }
}



    