

   
/* Apache UIMA v3 - First created by JCasGen Tue Nov 03 17:48:38 EST 2015 */

package aa;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;


/** 
 * Updated by JCasGen Tue Nov 03 17:48:38 EST 2015
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
 
 
  /* *****************
   *    Local Data   *
   * *****************/ 
   
  /* Register Features */
  public final static int _FI_arrayInt = JCasRegistry.registerFeature(typeIndexID);
  public final static int _FI_arrayRef = JCasRegistry.registerFeature(typeIndexID);
  public final static int _FI_arrayFloat = JCasRegistry.registerFeature(typeIndexID);
  public final static int _FI_arrayString = JCasRegistry.registerFeature(typeIndexID);
  public final static int _FI_plainInt = JCasRegistry.registerFeature(typeIndexID);
  public final static int _FI_plainFloat = JCasRegistry.registerFeature(typeIndexID);
  public final static int _FI_plainString = JCasRegistry.registerFeature(typeIndexID);
  public final static int _FI_plainRef = JCasRegistry.registerFeature(typeIndexID);

   
  private IntegerArray _F_arrayInt;  // 
  private FSArray _F_arrayRef;  // 
  private FloatArray _F_arrayFloat;  // 
  private StringArray _F_arrayString;  // 
  private int _F_plainInt;  // 
  private float _F_plainFloat;  // 
  private String _F_plainString;  // 
  private Root _F_plainRef;  // TokenType testMissingImport;
 
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
  public IntegerArray getArrayInt() { return _F_arrayInt;}
    
  /** setter for arrayInt - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayInt(IntegerArray v) {
         
      _casView.setWithJournalJFRI(this, _FI_arrayInt, () -> _F_arrayInt = v);
      }    
    
  /** indexed getter for arrayInt - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public int getArrayInt(int i) {
     return getArrayInt().get(i);} 

  /** indexed setter for arrayInt - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayInt(int i, int v) {
    getArrayInt().set(i, v);}  
   
    
  //*--------------*
  //* Feature: arrayRef

  /** getter for arrayRef - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getArrayRef() { return _F_arrayRef;}
    
  /** setter for arrayRef - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayRef(FSArray v) {
         
      _casView.setWithJournalJFRI(this, _FI_arrayRef, () -> _F_arrayRef = v);
      }    
    
  /** indexed getter for arrayRef - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public TOP getArrayRef(int i) {
     return getArrayRef().get(i);} 

  /** indexed setter for arrayRef - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayRef(int i, TOP v) {
    getArrayRef().set(i, v);}  
   
    
  //*--------------*
  //* Feature: arrayFloat

  /** getter for arrayFloat - gets 
   * @generated
   * @return value of the feature 
   */
  public FloatArray getArrayFloat() { return _F_arrayFloat;}
    
  /** setter for arrayFloat - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayFloat(FloatArray v) {
         
      _casView.setWithJournalJFRI(this, _FI_arrayFloat, () -> _F_arrayFloat = v);
      }    
    
  /** indexed getter for arrayFloat - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public float getArrayFloat(int i) {
     return getArrayFloat().get(i);} 

  /** indexed setter for arrayFloat - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayFloat(int i, float v) {
    getArrayFloat().set(i, v);}  
   
    
  //*--------------*
  //* Feature: arrayString

  /** getter for arrayString - gets 
   * @generated
   * @return value of the feature 
   */
  public StringArray getArrayString() { return _F_arrayString;}
    
  /** setter for arrayString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayString(StringArray v) {
         
      _casView.setWithJournalJFRI(this, _FI_arrayString, () -> _F_arrayString = v);
      }    
    
  /** indexed getter for arrayString - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public String getArrayString(int i) {
     return getArrayString().get(i);} 

  /** indexed setter for arrayString - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayString(int i, String v) {
    getArrayString().set(i, v);}  
   
    
  //*--------------*
  //* Feature: plainInt

  /** getter for plainInt - gets 
   * @generated
   * @return value of the feature 
   */
  public int getPlainInt() { return _F_plainInt;}
    
  /** setter for plainInt - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainInt(int v) {
              _casView.setWithCheckAndJournalJFRI(this, _FI_plainInt, () -> _F_plainInt = v);
      }    
   
    
  //*--------------*
  //* Feature: plainFloat

  /** getter for plainFloat - gets 
   * @generated
   * @return value of the feature 
   */
  public float getPlainFloat() { return _F_plainFloat;}
    
  /** setter for plainFloat - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainFloat(float v) {
              _casView.setWithCheckAndJournalJFRI(this, _FI_plainFloat, () -> _F_plainFloat = v);
      }    
   
    
  //*--------------*
  //* Feature: plainString

  /** getter for plainString - gets 
   * @generated
   * @return value of the feature 
   */
  public String getPlainString() { return _F_plainString;}
    
  /** setter for plainString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainString(String v) {
              _casView.setWithCheckAndJournalJFRI(this, _FI_plainString, () -> _F_plainString = v);
      }    
   
    
  //*--------------*
  //* Feature: plainRef

  /** getter for plainRef - gets TokenType testMissingImport;
   * @generated
   * @return value of the feature 
   */
  public Root getPlainRef() { return _F_plainRef;}
    
  /** setter for plainRef - sets TokenType testMissingImport; 
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainRef(Root v) {
         
      _casView.setWithJournalJFRI(this, _FI_plainRef, () -> _F_plainRef = v);
      }    
  }

    