

   
/* Apache UIMA v3 - First created by JCasGen Wed Mar 02 13:45:02 EST 2016 */

package aa;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;


import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.IntegerArray;


/** 
 * Updated by JCasGen Wed Mar 02 13:45:02 EST 2016
 * XML source: C:/au/svnCheckouts/branches/uimaj/experiment-v3-jcas/uimaj-core/src/test/java/org/apache/uima/jcas/test/generatedx.xml
 * @generated */
public class Root extends TOP {
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
   
  /* Feature Adjusted Offsets */
  public final static int _FI_arrayInt = TypeSystemImpl.getAdjustedFeatureOffset("arrayInt");
  public final static int _FI_arrayRef = TypeSystemImpl.getAdjustedFeatureOffset("arrayRef");
  public final static int _FI_arrayFloat = TypeSystemImpl.getAdjustedFeatureOffset("arrayFloat");
  public final static int _FI_arrayString = TypeSystemImpl.getAdjustedFeatureOffset("arrayString");
  public final static int _FI_plainInt = TypeSystemImpl.getAdjustedFeatureOffset("plainInt");
  public final static int _FI_plainFloat = TypeSystemImpl.getAdjustedFeatureOffset("plainFloat");
  public final static int _FI_plainString = TypeSystemImpl.getAdjustedFeatureOffset("plainString");
  public final static int _FI_plainRef = TypeSystemImpl.getAdjustedFeatureOffset("plainRef");

   
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
    _setFeatureValueNcWj(_getFeatFromAdjOffset(_FI_arrayInt, false), v);
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
    _setFeatureValueNcWj(_getFeatFromAdjOffset(_FI_arrayRef, false), v);
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
    _setFeatureValueNcWj(_getFeatFromAdjOffset(_FI_arrayFloat, false), v);
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
    _setFeatureValueNcWj(_getFeatFromAdjOffset(_FI_arrayString, false), v);
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
    _setIntValueNfc(_getFeatFromAdjOffset(_FI_plainInt, true), v);
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
    _setFloatValueNfc(_getFeatFromAdjOffset(_FI_plainFloat, true), v);
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
    _setStringValueNfc(_getFeatFromAdjOffset(_FI_plainString, false), v);
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
    _setFeatureValueNcWj(_getFeatFromAdjOffset(_FI_plainRef, false), v);
  }    
    
  }

    