

   
/* Apache UIMA v3 - First created by JCasGen Fri Dec 16 10:23:12 EST 2016 */

package aa;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;


import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.IntegerArray;


/** 
 * Updated by JCasGen Fri Dec 16 10:23:12 EST 2016
 * XML source: C:/au/svnCheckouts/branches/uimaj/v3-alpha/uimaj-core/src/test/java/org/apache/uima/jcas/test/generatedx.xml
 * @generated */
public class Root extends TOP {
 
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static String _TypeName = "aa.Root";
  
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Root.class);
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
   
  public final static String _FeatName_arrayInt = "arrayInt";
  public final static String _FeatName_arrayRef = "arrayRef";
  public final static String _FeatName_arrayFloat = "arrayFloat";
  public final static String _FeatName_arrayString = "arrayString";
  public final static String _FeatName_plainInt = "plainInt";
  public final static String _FeatName_plainFloat = "plainFloat";
  public final static String _FeatName_plainString = "plainString";
  public final static String _FeatName_plainRef = "plainRef";
  public final static String _FeatName_plainLong = "plainLong";
  public final static String _FeatName_plainDouble = "plainDouble";
  public final static String _FeatName_arrayLong = "arrayLong";
  public final static String _FeatName_arrayDouble = "arrayDouble";


  /* Feature Adjusted Offsets */
  public final static int _FI_arrayInt = TypeSystemImpl.getAdjustedFeatureOffset("arrayInt");
  public final static int _FI_arrayRef = TypeSystemImpl.getAdjustedFeatureOffset("arrayRef");
  public final static int _FI_arrayFloat = TypeSystemImpl.getAdjustedFeatureOffset("arrayFloat");
  public final static int _FI_arrayString = TypeSystemImpl.getAdjustedFeatureOffset("arrayString");
  public final static int _FI_plainInt = TypeSystemImpl.getAdjustedFeatureOffset("plainInt");
  public final static int _FI_plainFloat = TypeSystemImpl.getAdjustedFeatureOffset("plainFloat");
  public final static int _FI_plainString = TypeSystemImpl.getAdjustedFeatureOffset("plainString");
  public final static int _FI_plainRef = TypeSystemImpl.getAdjustedFeatureOffset("plainRef");
  public final static int _FI_plainLong = TypeSystemImpl.getAdjustedFeatureOffset("plainLong");
  public final static int _FI_plainDouble = TypeSystemImpl.getAdjustedFeatureOffset("plainDouble");
  public final static int _FI_arrayLong = TypeSystemImpl.getAdjustedFeatureOffset("arrayLong");
  public final static int _FI_arrayDouble = TypeSystemImpl.getAdjustedFeatureOffset("arrayDouble");

   
  /** Never called.  Disable default constructor
   * @generated */
  protected Root() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public Root(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Root(JCas jcas) {
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
  //* Feature: arrayInt

  /** getter for arrayInt - gets 
   * @generated
   * @return value of the feature 
   */
  public IntegerArray getArrayInt() { return (IntegerArray)(_getFeatureValueNc(_FI_arrayInt));}
    
  /** setter for arrayInt - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayInt(IntegerArray v) {
    _setFeatureValueNcWj(_FI_arrayInt, v);
  }    
    
    
  /** indexed getter for arrayInt - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public int getArrayInt(int i) {
     return ((IntegerArray)(_getFeatureValueNc(_FI_arrayInt))).get(i);} 

  /** indexed setter for arrayInt - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayInt(int i, int v) {
    ((IntegerArray)(_getFeatureValueNc(_FI_arrayInt))).set(i, v);
  }  
   
    
  //*--------------*
  //* Feature: arrayRef

  /** getter for arrayRef - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getArrayRef() { return (FSArray)(_getFeatureValueNc(_FI_arrayRef));}
    
  /** setter for arrayRef - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayRef(FSArray v) {
    _setFeatureValueNcWj(_FI_arrayRef, v);
  }    
    
    
  /** indexed getter for arrayRef - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public TOP getArrayRef(int i) {
     return (TOP)(((FSArray)(_getFeatureValueNc(_FI_arrayRef))).get(i));} 

  /** indexed setter for arrayRef - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayRef(int i, TOP v) {
    ((FSArray)(_getFeatureValueNc(_FI_arrayRef))).set(i, v);
  }  
   
    
  //*--------------*
  //* Feature: arrayFloat

  /** getter for arrayFloat - gets 
   * @generated
   * @return value of the feature 
   */
  public FloatArray getArrayFloat() { return (FloatArray)(_getFeatureValueNc(_FI_arrayFloat));}
    
  /** setter for arrayFloat - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayFloat(FloatArray v) {
    _setFeatureValueNcWj(_FI_arrayFloat, v);
  }    
    
    
  /** indexed getter for arrayFloat - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public float getArrayFloat(int i) {
     return ((FloatArray)(_getFeatureValueNc(_FI_arrayFloat))).get(i);} 

  /** indexed setter for arrayFloat - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayFloat(int i, float v) {
    ((FloatArray)(_getFeatureValueNc(_FI_arrayFloat))).set(i, v);
  }  
   
    
  //*--------------*
  //* Feature: arrayString

  /** getter for arrayString - gets 
   * @generated
   * @return value of the feature 
   */
  public StringArray getArrayString() { return (StringArray)(_getFeatureValueNc(_FI_arrayString));}
    
  /** setter for arrayString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayString(StringArray v) {
    _setFeatureValueNcWj(_FI_arrayString, v);
  }    
    
    
  /** indexed getter for arrayString - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public String getArrayString(int i) {
     return ((StringArray)(_getFeatureValueNc(_FI_arrayString))).get(i);} 

  /** indexed setter for arrayString - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayString(int i, String v) {
    ((StringArray)(_getFeatureValueNc(_FI_arrayString))).set(i, v);
  }  
   
    
  //*--------------*
  //* Feature: plainInt

  /** getter for plainInt - gets 
   * @generated
   * @return value of the feature 
   */
  public int getPlainInt() { return _getIntValueNc(_FI_plainInt);}
    
  /** setter for plainInt - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainInt(int v) {
    _setIntValueNfc(_FI_plainInt, v);
  }    
    
   
    
  //*--------------*
  //* Feature: plainFloat

  /** getter for plainFloat - gets 
   * @generated
   * @return value of the feature 
   */
  public float getPlainFloat() { return _getFloatValueNc(_FI_plainFloat);}
    
  /** setter for plainFloat - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainFloat(float v) {
    _setFloatValueNfc(_FI_plainFloat, v);
  }    
    
   
    
  //*--------------*
  //* Feature: plainString

  /** getter for plainString - gets 
   * @generated
   * @return value of the feature 
   */
  public String getPlainString() { return _getStringValueNc(_FI_plainString);}
    
  /** setter for plainString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainString(String v) {
    _setStringValueNfc(_FI_plainString, v);
  }    
    
   
    
  //*--------------*
  //* Feature: plainRef

  /** getter for plainRef - gets TokenType testMissingImport;
   * @generated
   * @return value of the feature 
   */
  public Root getPlainRef() { return (Root)(_getFeatureValueNc(_FI_plainRef));}
    
  /** setter for plainRef - sets TokenType testMissingImport; 
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainRef(Root v) {
    _setFeatureValueNcWj(_FI_plainRef, v);
  }    
    
   
    
  //*--------------*
  //* Feature: plainLong

  /** getter for plainLong - gets 
   * @generated
   * @return value of the feature 
   */
  public long getPlainLong() { return _getLongValueNc(_FI_plainLong);}
    
  /** setter for plainLong - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainLong(long v) {
    _setLongValueNfc(_FI_plainLong, v);
  }    
    
   
    
  //*--------------*
  //* Feature: plainDouble

  /** getter for plainDouble - gets 
   * @generated
   * @return value of the feature 
   */
  public double getPlainDouble() { return _getDoubleValueNc(_FI_plainDouble);}
    
  /** setter for plainDouble - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainDouble(double v) {
    _setDoubleValueNfc(_FI_plainDouble, v);
  }    
    
   
    
  //*--------------*
  //* Feature: arrayLong

  /** getter for arrayLong - gets 
   * @generated
   * @return value of the feature 
   */
  public LongArray getArrayLong() { return (LongArray)(_getFeatureValueNc(_FI_arrayLong));}
    
  /** setter for arrayLong - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayLong(LongArray v) {
    _setFeatureValueNcWj(_FI_arrayLong, v);
  }    
    
    
  /** indexed getter for arrayLong - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public long getArrayLong(int i) {
     return ((LongArray)(_getFeatureValueNc(_FI_arrayLong))).get(i);} 

  /** indexed setter for arrayLong - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayLong(int i, long v) {
    ((LongArray)(_getFeatureValueNc(_FI_arrayLong))).set(i, v);
  }  
   
    
  //*--------------*
  //* Feature: arrayDouble

  /** getter for arrayDouble - gets 
   * @generated
   * @return value of the feature 
   */
  public DoubleArray getArrayDouble() { return (DoubleArray)(_getFeatureValueNc(_FI_arrayDouble));}
    
  /** setter for arrayDouble - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayDouble(DoubleArray v) {
    _setFeatureValueNcWj(_FI_arrayDouble, v);
  }    
    
    
  /** indexed getter for arrayDouble - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public double getArrayDouble(int i) {
     return ((DoubleArray)(_getFeatureValueNc(_FI_arrayDouble))).get(i);} 

  /** indexed setter for arrayDouble - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayDouble(int i, double v) {
    ((DoubleArray)(_getFeatureValueNc(_FI_arrayDouble))).set(i, v);
  }  
  }

    