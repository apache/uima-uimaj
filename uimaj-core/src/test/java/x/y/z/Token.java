

   
/* Apache UIMA v3 - First created by JCasGen Tue Nov 03 17:48:38 EST 2015 */

package x.y.z;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;

import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Tue Nov 03 17:48:38 EST 2015
 * XML source: C:/au/svnCheckouts/branches/uimaj/experiment-v3-jcas/uimaj-core/src/test/java/org/apache/uima/jcas/test/generatedx.xml
 * @generated */
public class Token extends Annotation {
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
 
 
  /* *****************
   *    Local Data   *
   * *****************/ 
   
  /* Register Features */
  public final static int _FI_ttype = JCasRegistry.registerFeature(typeIndexID);
  public final static int _FI_tokenFloatFeat = JCasRegistry.registerFeature(typeIndexID);
  public final static int _FI_lemma = JCasRegistry.registerFeature(typeIndexID);
  public final static int _FI_sentenceLength = JCasRegistry.registerFeature(typeIndexID);
  public final static int _FI_lemmaList = JCasRegistry.registerFeature(typeIndexID);

   
  private TokenType _F_ttype;  // 
  private float _F_tokenFloatFeat;  // 
  private String _F_lemma;  // 
  private int _F_sentenceLength;  // 
  private StringArray _F_lemmaList;  // 
 
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
  public TokenType getTtype() { return _F_ttype;}
    
  /** setter for ttype - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTtype(TokenType v) {
         
      _casView.setWithJournalJFRI(this, _FI_ttype, () -> _F_ttype = v);
      }    
   
    
  //*--------------*
  //* Feature: tokenFloatFeat

  /** getter for tokenFloatFeat - gets 
   * @generated
   * @return value of the feature 
   */
  public float getTokenFloatFeat() { return _F_tokenFloatFeat;}
    
  /** setter for tokenFloatFeat - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTokenFloatFeat(float v) {
              _casView.setWithCheckAndJournalJFRI(this, _FI_tokenFloatFeat, () -> _F_tokenFloatFeat = v);
      }    
   
    
  //*--------------*
  //* Feature: lemma

  /** getter for lemma - gets 
   * @generated
   * @return value of the feature 
   */
  public String getLemma() { return _F_lemma;}
    
  /** setter for lemma - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLemma(String v) {
              _casView.setWithCheckAndJournalJFRI(this, _FI_lemma, () -> _F_lemma = v);
      }    
   
    
  //*--------------*
  //* Feature: sentenceLength

  /** getter for sentenceLength - gets 
   * @generated
   * @return value of the feature 
   */
  public int getSentenceLength() { return _F_sentenceLength;}
    
  /** setter for sentenceLength - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setSentenceLength(int v) {
              _casView.setWithCheckAndJournalJFRI(this, _FI_sentenceLength, () -> _F_sentenceLength = v);
      }    
   
    
  //*--------------*
  //* Feature: lemmaList

  /** getter for lemmaList - gets 
   * @generated
   * @return value of the feature 
   */
  public StringArray getLemmaList() { return _F_lemmaList;}
    
  /** setter for lemmaList - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLemmaList(StringArray v) {
         
      _casView.setWithJournalJFRI(this, _FI_lemmaList, () -> _F_lemmaList = v);
      }    
    
  /** indexed getter for lemmaList - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public String getLemmaList(int i) {
     return getLemmaList().get(i);} 

  /** indexed setter for lemmaList - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setLemmaList(int i, String v) {
    getLemmaList().set(i, v);}  
  }

    