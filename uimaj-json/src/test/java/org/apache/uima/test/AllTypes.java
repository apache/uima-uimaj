
/* Apache UIMA v3 - First created by JCasGen Tue Mar 08 10:58:32 EST 2016 */

package org.apache.uima.test;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.IntegerList;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Updated by JCasGen Tue Mar 08 10:58:32 EST 2016 XML source:
 * C:/au/svnCheckouts/branches/uimaj/experiment-v3-jcas/uimaj-json/src/test/resources/CasSerialization/desc/allTypes.xml
 * 
 * @generated
 */
public class AllTypes extends Annotation {
  /**
   * @generated
   * @ordered
   */
  @SuppressWarnings("hiding")
  public final static int typeIndexID = JCasRegistry.register(AllTypes.class);
  /**
   * @generated
   * @ordered
   */
  @SuppressWarnings("hiding")
  public final static int type = typeIndexID;

  /**
   * @generated
   * @return index of the type
   */
  @Override
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /*
   * ******************* Feature Offsets *
   *******************/

  /* Feature Adjusted Offsets */
  private final static CallSite _FC_aBoolean = TypeSystemImpl.createCallSite(AllTypes.class,
          "aBoolean");
  private final static MethodHandle _FH_aBoolean = _FC_aBoolean.dynamicInvoker();
  private final static CallSite _FC_aByte = TypeSystemImpl.createCallSite(AllTypes.class, "aByte");
  private final static MethodHandle _FH_aByte = _FC_aByte.dynamicInvoker();
  private final static CallSite _FC_aShort = TypeSystemImpl.createCallSite(AllTypes.class,
          "aShort");
  private final static MethodHandle _FH_aShort = _FC_aShort.dynamicInvoker();
  private final static CallSite _FC_aInteger = TypeSystemImpl.createCallSite(AllTypes.class,
          "aInteger");
  private final static MethodHandle _FH_aInteger = _FC_aInteger.dynamicInvoker();
  private final static CallSite _FC_aLong = TypeSystemImpl.createCallSite(AllTypes.class, "aLong");
  private final static MethodHandle _FH_aLong = _FC_aLong.dynamicInvoker();
  private final static CallSite _FC_aFloat = TypeSystemImpl.createCallSite(AllTypes.class,
          "aFloat");
  private final static MethodHandle _FH_aFloat = _FC_aFloat.dynamicInvoker();
  private final static CallSite _FC_aDouble = TypeSystemImpl.createCallSite(AllTypes.class,
          "aDouble");
  private final static MethodHandle _FH_aDouble = _FC_aDouble.dynamicInvoker();
  private final static CallSite _FC_aString = TypeSystemImpl.createCallSite(AllTypes.class,
          "aString");
  private final static MethodHandle _FH_aString = _FC_aString.dynamicInvoker();
  private final static CallSite _FC_aFS = TypeSystemImpl.createCallSite(AllTypes.class, "aFS");
  private final static MethodHandle _FH_aFS = _FC_aFS.dynamicInvoker();
  private final static CallSite _FC_aArrayBoolean = TypeSystemImpl.createCallSite(AllTypes.class,
          "aArrayBoolean");
  private final static MethodHandle _FH_aArrayBoolean = _FC_aArrayBoolean.dynamicInvoker();
  private final static CallSite _FC_aArrayMrBoolean = TypeSystemImpl.createCallSite(AllTypes.class,
          "aArrayMrBoolean");
  private final static MethodHandle _FH_aArrayMrBoolean = _FC_aArrayMrBoolean.dynamicInvoker();
  private final static CallSite _FC_aArrayMrByte = TypeSystemImpl.createCallSite(AllTypes.class,
          "aArrayMrByte");
  private final static MethodHandle _FH_aArrayMrByte = _FC_aArrayMrByte.dynamicInvoker();
  private final static CallSite _FC_aArrayByte = TypeSystemImpl.createCallSite(AllTypes.class,
          "aArrayByte");
  private final static MethodHandle _FH_aArrayByte = _FC_aArrayByte.dynamicInvoker();
  private final static CallSite _FC_aArrayShort = TypeSystemImpl.createCallSite(AllTypes.class,
          "aArrayShort");
  private final static MethodHandle _FH_aArrayShort = _FC_aArrayShort.dynamicInvoker();
  private final static CallSite _FC_aArrayMrShort = TypeSystemImpl.createCallSite(AllTypes.class,
          "aArrayMrShort");
  private final static MethodHandle _FH_aArrayMrShort = _FC_aArrayMrShort.dynamicInvoker();
  private final static CallSite _FC_aArrayString = TypeSystemImpl.createCallSite(AllTypes.class,
          "aArrayString");
  private final static MethodHandle _FH_aArrayString = _FC_aArrayString.dynamicInvoker();
  private final static CallSite _FC_aArrayMrString = TypeSystemImpl.createCallSite(AllTypes.class,
          "aArrayMrString");
  private final static MethodHandle _FH_aArrayMrString = _FC_aArrayMrString.dynamicInvoker();
  private final static CallSite _FC_aListInteger = TypeSystemImpl.createCallSite(AllTypes.class,
          "aListInteger");
  private final static MethodHandle _FH_aListInteger = _FC_aListInteger.dynamicInvoker();
  private final static CallSite _FC_aListMrInteger = TypeSystemImpl.createCallSite(AllTypes.class,
          "aListMrInteger");
  private final static MethodHandle _FH_aListMrInteger = _FC_aListMrInteger.dynamicInvoker();
  private final static CallSite _FC_aListString = TypeSystemImpl.createCallSite(AllTypes.class,
          "aListString");
  private final static MethodHandle _FH_aListString = _FC_aListString.dynamicInvoker();
  private final static CallSite _FC_aListMrString = TypeSystemImpl.createCallSite(AllTypes.class,
          "aListMrString");
  private final static MethodHandle _FH_aListMrString = _FC_aListMrString.dynamicInvoker();
  private final static CallSite _FC_aListFs = TypeSystemImpl.createCallSite(AllTypes.class,
          "aListFs");
  private final static MethodHandle _FH_aListFs = _FC_aListFs.dynamicInvoker();
  private final static CallSite _FC_aListMrFs = TypeSystemImpl.createCallSite(AllTypes.class,
          "aListMrFs");
  private final static MethodHandle _FH_aListMrFs = _FC_aListMrFs.dynamicInvoker();
  private final static CallSite _FC_aArrayFS = TypeSystemImpl.createCallSite(AllTypes.class,
          "aArrayFS");
  private final static MethodHandle _FH_aArrayFS = _FC_aArrayFS.dynamicInvoker();

  /**
   * Never called. Disable default constructor
   * 
   * @generated
   */
  protected AllTypes() {
    /* intentionally empty block */}

  /**
   * Internal - constructor used by generator
   * 
   * @generated
   * @param casImpl
   *          the CAS this Feature Structure belongs to
   * @param type
   *          the type of this Feature Structure
   */
  public AllTypes(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }

  /**
   * @generated
   * @param jcas
   *          JCas to which this Feature Structure belongs
   */
  public AllTypes(JCas jcas) {
    super(jcas);
    readObject();
  }

  /**
   * <!-- begin-user-doc --> Write your own initialization here <!-- end-user-doc -->
   *
   * @generated modifiable
   */
  private void readObject() {
    /* default - does nothing empty block */}

  // *--------------*
  // * Feature: aBoolean

  /**
   * getter for aBoolean - gets
   * 
   * @generated
   * @return value of the feature
   */
  public boolean getABoolean() {
    return _getBooleanValueNc(wrapGetIntCatchException(_FH_aBoolean));
  }

  /**
   * setter for aBoolean - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setABoolean(boolean v) {
    _setBooleanValueNfc(wrapGetIntCatchException(_FH_aBoolean), v);
  }

  // *--------------*
  // * Feature: aByte

  /**
   * getter for aByte - gets
   * 
   * @generated
   * @return value of the feature
   */
  public byte getAByte() {
    return _getByteValueNc(wrapGetIntCatchException(_FH_aByte));
  }

  /**
   * setter for aByte - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAByte(byte v) {
    _setByteValueNfc(wrapGetIntCatchException(_FH_aByte), v);
  }

  // *--------------*
  // * Feature: aShort

  /**
   * getter for aShort - gets
   * 
   * @generated
   * @return value of the feature
   */
  public short getAShort() {
    return _getShortValueNc(wrapGetIntCatchException(_FH_aShort));
  }

  /**
   * setter for aShort - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAShort(short v) {
    _setShortValueNfc(wrapGetIntCatchException(_FH_aShort), v);
  }

  // *--------------*
  // * Feature: aInteger

  /**
   * getter for aInteger - gets
   * 
   * @generated
   * @return value of the feature
   */
  public int getAInteger() {
    return _getIntValueNc(wrapGetIntCatchException(_FH_aInteger));
  }

  /**
   * setter for aInteger - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAInteger(int v) {
    _setIntValueNfc(wrapGetIntCatchException(_FH_aInteger), v);
  }

  // *--------------*
  // * Feature: aLong

  /**
   * getter for aLong - gets
   * 
   * @generated
   * @return value of the feature
   */
  public long getALong() {
    return _getLongValueNc(wrapGetIntCatchException(_FH_aLong));
  }

  /**
   * setter for aLong - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setALong(long v) {
    _setLongValueNfc(wrapGetIntCatchException(_FH_aLong), v);
  }

  // *--------------*
  // * Feature: aFloat

  /**
   * getter for aFloat - gets
   * 
   * @generated
   * @return value of the feature
   */
  public float getAFloat() {
    return _getFloatValueNc(wrapGetIntCatchException(_FH_aFloat));
  }

  /**
   * setter for aFloat - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAFloat(float v) {
    _setFloatValueNfc(wrapGetIntCatchException(_FH_aFloat), v);
  }

  // *--------------*
  // * Feature: aDouble

  /**
   * getter for aDouble - gets
   * 
   * @generated
   * @return value of the feature
   */
  public double getADouble() {
    return _getDoubleValueNc(wrapGetIntCatchException(_FH_aDouble));
  }

  /**
   * setter for aDouble - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setADouble(double v) {
    _setDoubleValueNfc(wrapGetIntCatchException(_FH_aDouble), v);
  }

  // *--------------*
  // * Feature: aString

  /**
   * getter for aString - gets
   * 
   * @generated
   * @return value of the feature
   */
  public String getAString() {
    return _getStringValueNc(wrapGetIntCatchException(_FH_aString));
  }

  /**
   * setter for aString - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAString(String v) {
    _setStringValueNfc(wrapGetIntCatchException(_FH_aString), v);
  }

  // *--------------*
  // * Feature: aFS

  /**
   * getter for aFS - gets
   * 
   * @generated
   * @return value of the feature
   */
  public Annotation getAFS() {
    return (Annotation) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aFS)));
  }

  /**
   * setter for aFS - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAFS(Annotation v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aFS), v);
  }

  // *--------------*
  // * Feature: aArrayBoolean

  /**
   * getter for aArrayBoolean - gets
   * 
   * @generated
   * @return value of the feature
   */
  public BooleanArray getAArrayBoolean() {
    return (BooleanArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayBoolean)));
  }

  /**
   * setter for aArrayBoolean - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAArrayBoolean(BooleanArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aArrayBoolean), v);
  }

  /**
   * indexed getter for aArrayBoolean - gets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to get
   * @return value of the element at index i
   */
  public boolean getAArrayBoolean(int i) {
    return ((BooleanArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayBoolean))))
            .get(i);
  }

  /**
   * indexed setter for aArrayBoolean - sets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to set
   * @param v
   *          value to set into the array
   */
  public void setAArrayBoolean(int i, boolean v) {
    ((BooleanArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayBoolean)))).set(i, v);
  }

  // *--------------*
  // * Feature: aArrayMrBoolean

  /**
   * getter for aArrayMrBoolean - gets
   * 
   * @generated
   * @return value of the feature
   */
  public BooleanArray getAArrayMrBoolean() {
    return (BooleanArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayMrBoolean)));
  }

  /**
   * setter for aArrayMrBoolean - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAArrayMrBoolean(BooleanArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aArrayMrBoolean), v);
  }

  /**
   * indexed getter for aArrayMrBoolean - gets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to get
   * @return value of the element at index i
   */
  public boolean getAArrayMrBoolean(int i) {
    return ((BooleanArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayMrBoolean))))
            .get(i);
  }

  /**
   * indexed setter for aArrayMrBoolean - sets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to set
   * @param v
   *          value to set into the array
   */
  public void setAArrayMrBoolean(int i, boolean v) {
    ((BooleanArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayMrBoolean)))).set(i, v);
  }

  // *--------------*
  // * Feature: aArrayMrByte

  /**
   * getter for aArrayMrByte - gets
   * 
   * @generated
   * @return value of the feature
   */
  public ByteArray getAArrayMrByte() {
    return (ByteArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayMrByte)));
  }

  /**
   * setter for aArrayMrByte - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAArrayMrByte(ByteArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aArrayMrByte), v);
  }

  /**
   * indexed getter for aArrayMrByte - gets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to get
   * @return value of the element at index i
   */
  public byte getAArrayMrByte(int i) {
    return ((ByteArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayMrByte)))).get(i);
  }

  /**
   * indexed setter for aArrayMrByte - sets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to set
   * @param v
   *          value to set into the array
   */
  public void setAArrayMrByte(int i, byte v) {
    ((ByteArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayMrByte)))).set(i, v);
  }

  // *--------------*
  // * Feature: aArrayByte

  /**
   * getter for aArrayByte - gets
   * 
   * @generated
   * @return value of the feature
   */
  public ByteArray getAArrayByte() {
    return (ByteArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayByte)));
  }

  /**
   * setter for aArrayByte - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAArrayByte(ByteArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aArrayByte), v);
  }

  /**
   * indexed getter for aArrayByte - gets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to get
   * @return value of the element at index i
   */
  public byte getAArrayByte(int i) {
    return ((ByteArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayByte)))).get(i);
  }

  /**
   * indexed setter for aArrayByte - sets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to set
   * @param v
   *          value to set into the array
   */
  public void setAArrayByte(int i, byte v) {
    ((ByteArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayByte)))).set(i, v);
  }

  // *--------------*
  // * Feature: aArrayShort

  /**
   * getter for aArrayShort - gets
   * 
   * @generated
   * @return value of the feature
   */
  public ShortArray getAArrayShort() {
    return (ShortArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayShort)));
  }

  /**
   * setter for aArrayShort - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAArrayShort(ShortArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aArrayShort), v);
  }

  /**
   * indexed getter for aArrayShort - gets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to get
   * @return value of the element at index i
   */
  public short getAArrayShort(int i) {
    return ((ShortArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayShort)))).get(i);
  }

  /**
   * indexed setter for aArrayShort - sets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to set
   * @param v
   *          value to set into the array
   */
  public void setAArrayShort(int i, short v) {
    ((ShortArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayShort)))).set(i, v);
  }

  // *--------------*
  // * Feature: aArrayMrShort

  /**
   * getter for aArrayMrShort - gets
   * 
   * @generated
   * @return value of the feature
   */
  public ShortArray getAArrayMrShort() {
    return (ShortArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayMrShort)));
  }

  /**
   * setter for aArrayMrShort - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAArrayMrShort(ShortArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aArrayMrShort), v);
  }

  /**
   * indexed getter for aArrayMrShort - gets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to get
   * @return value of the element at index i
   */
  public short getAArrayMrShort(int i) {
    return ((ShortArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayMrShort)))).get(i);
  }

  /**
   * indexed setter for aArrayMrShort - sets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to set
   * @param v
   *          value to set into the array
   */
  public void setAArrayMrShort(int i, short v) {
    ((ShortArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayMrShort)))).set(i, v);
  }

  // *--------------*
  // * Feature: aArrayString

  /**
   * getter for aArrayString - gets
   * 
   * @generated
   * @return value of the feature
   */
  public StringArray getAArrayString() {
    return (StringArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayString)));
  }

  /**
   * setter for aArrayString - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAArrayString(StringArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aArrayString), v);
  }

  /**
   * indexed getter for aArrayString - gets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to get
   * @return value of the element at index i
   */
  public String getAArrayString(int i) {
    return ((StringArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayString)))).get(i);
  }

  /**
   * indexed setter for aArrayString - sets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to set
   * @param v
   *          value to set into the array
   */
  public void setAArrayString(int i, String v) {
    ((StringArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayString)))).set(i, v);
  }

  // *--------------*
  // * Feature: aArrayMrString

  /**
   * getter for aArrayMrString - gets
   * 
   * @generated
   * @return value of the feature
   */
  public StringArray getAArrayMrString() {
    return (StringArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayMrString)));
  }

  /**
   * setter for aArrayMrString - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAArrayMrString(StringArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aArrayMrString), v);
  }

  /**
   * indexed getter for aArrayMrString - gets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to get
   * @return value of the element at index i
   */
  public String getAArrayMrString(int i) {
    return ((StringArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayMrString))))
            .get(i);
  }

  /**
   * indexed setter for aArrayMrString - sets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to set
   * @param v
   *          value to set into the array
   */
  public void setAArrayMrString(int i, String v) {
    ((StringArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayMrString)))).set(i, v);
  }

  // *--------------*
  // * Feature: aListInteger

  /**
   * getter for aListInteger - gets
   * 
   * @generated
   * @return value of the feature
   */
  public IntegerList getAListInteger() {
    return (IntegerList) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aListInteger)));
  }

  /**
   * setter for aListInteger - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAListInteger(IntegerList v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aListInteger), v);
  }

  // *--------------*
  // * Feature: aListMrInteger

  /**
   * getter for aListMrInteger - gets
   * 
   * @generated
   * @return value of the feature
   */
  public IntegerList getAListMrInteger() {
    return (IntegerList) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aListMrInteger)));
  }

  /**
   * setter for aListMrInteger - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAListMrInteger(IntegerList v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aListMrInteger), v);
  }

  // *--------------*
  // * Feature: aListString

  /**
   * getter for aListString - gets
   * 
   * @generated
   * @return value of the feature
   */
  public StringList getAListString() {
    return (StringList) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aListString)));
  }

  /**
   * setter for aListString - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAListString(StringList v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aListString), v);
  }

  // *--------------*
  // * Feature: aListMrString

  /**
   * getter for aListMrString - gets
   * 
   * @generated
   * @return value of the feature
   */
  public StringList getAListMrString() {
    return (StringList) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aListMrString)));
  }

  /**
   * setter for aListMrString - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAListMrString(StringList v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aListMrString), v);
  }

  // *--------------*
  // * Feature: aListFs

  /**
   * getter for aListFs - gets
   * 
   * @generated
   * @return value of the feature
   */
  public FSList getAListFs() {
    return (FSList) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aListFs)));
  }

  /**
   * setter for aListFs - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAListFs(FSList v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aListFs), v);
  }

  // *--------------*
  // * Feature: aListMrFs

  /**
   * getter for aListMrFs - gets
   * 
   * @generated
   * @return value of the feature
   */
  public FSList getAListMrFs() {
    return (FSList) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aListMrFs)));
  }

  /**
   * setter for aListMrFs - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAListMrFs(FSList v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aListMrFs), v);
  }

  // *--------------*
  // * Feature: aArrayFS

  /**
   * getter for aArrayFS - gets
   * 
   * @generated
   * @return value of the feature
   */
  public FSArray getAArrayFS() {
    return (FSArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayFS)));
  }

  /**
   * setter for aArrayFS - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setAArrayFS(FSArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aArrayFS), v);
  }

  /**
   * indexed getter for aArrayFS - gets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to get
   * @return value of the element at index i
   */
  public Annotation getAArrayFS(int i) {
    return (Annotation) (((FSArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayFS))))
            .get(i));
  }

  /**
   * indexed setter for aArrayFS - sets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to set
   * @param v
   *          value to set into the array
   */
  public void setAArrayFS(int i, Annotation v) {
    ((FSArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayFS)))).set(i, v);
  }
}
