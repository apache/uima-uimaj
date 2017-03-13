

   
/* Apache UIMA v3 - First created by JCasGen Fri Dec 16 10:23:12 EST 2016 */

package x.y.z;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;


import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Fri Dec 16 10:23:12 EST 2016
 * XML source: C:/au/svnCheckouts/branches/uimaj/v3-alpha/uimaj-core/src/test/java/org/apache/uima/jcas/test/generatedx.xml
 * @generated */
public class Token extends Annotation {
 
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static String _TypeName = "x.y.z.Token";
  
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Token.class);
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
   
  public final static String _FeatName_ttype = "ttype";
  public final static String _FeatName_tokenFloatFeat = "tokenFloatFeat";
  public final static String _FeatName_lemma = "lemma";
  public final static String _FeatName_lemmaList = "lemmaList";


  /* Feature Adjusted Offsets */
  public final static int _FI_ttype = TypeSystemImpl.getAdjustedFeatureOffset("ttype");
  public final static int _FI_tokenFloatFeat = TypeSystemImpl.getAdjustedFeatureOffset("tokenFloatFeat");
  public final static int _FI_lemma = TypeSystemImpl.getAdjustedFeatureOffset("lemma");
  public final static int _FI_lemmaList = TypeSystemImpl.getAdjustedFeatureOffset("lemmaList");

   
  /** Never called.  Disable default constructor
   * @generated */
  protected Token() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public Token(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Token(JCas jcas) {
    super(jcas);
    readObject();   
  } 


  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Token(JCas jcas, int begin, int end) {
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
  //* Feature: ttype

  /** getter for ttype - gets 
   * @generated
   * @return value of the feature 
   */
  public TokenType getTtype() { return (TokenType)(_getFeatureValueNc(_FI_ttype));}
    
  /** setter for ttype - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTtype(TokenType v) {
    _setFeatureValueNcWj(_FI_ttype, v);
  }    
    
   
    
  //*--------------*
  //* Feature: tokenFloatFeat

  /** getter for tokenFloatFeat - gets 
   * @generated
   * @return value of the feature 
   */
  public float getTokenFloatFeat() { return _getFloatValueNc(_FI_tokenFloatFeat);}
    
  /** setter for tokenFloatFeat - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTokenFloatFeat(float v) {
    _setFloatValueNfc(_FI_tokenFloatFeat, v);
  }    
    
   
    
  //*--------------*
  //* Feature: lemma

  /** getter for lemma - gets 
   * @generated
   * @return value of the feature 
   */
  public String getLemma() { return _getStringValueNc(_FI_lemma);}
    
  /** setter for lemma - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLemma(String v) {
    _setStringValueNfc(_FI_lemma, v);
  }    
    
   
    
  //*--------------*
  //* Feature: lemmaList

  /** getter for lemmaList - gets 
   * @generated
   * @return value of the feature 
   */
  public StringArray getLemmaList() { return (StringArray)(_getFeatureValueNc(_FI_lemmaList));}
    
  /** setter for lemmaList - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLemmaList(StringArray v) {
    _setFeatureValueNcWj(_FI_lemmaList, v);
  }    
    
    
  /** indexed getter for lemmaList - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public String getLemmaList(int i) {
     return ((StringArray)(_getFeatureValueNc(_FI_lemmaList))).get(i);} 

  /** indexed setter for lemmaList - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setLemmaList(int i, String v) {
    ((StringArray)(_getFeatureValueNc(_FI_lemmaList))).set(i, v);
  }  
  }

    