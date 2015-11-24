

   
/* Apache UIMA v3 - First created by JCasGen Mon Oct 19 15:16:52 EDT 2015 */

package org.apache.lang;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP;


/** 
 * Updated by JCasGen Mon Oct 19 15:16:52 EDT 2015
 * XML source: C:/au/svnCheckouts/branches/uimaj/experiment-v3-jcas/uimaj-core/src/test/java/org/apache/uima/jcas/test/generatedx.xml
 * @generated */
public class LanguagePair extends TOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(LanguagePair.class);
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
   private String _F_lang1;  // 
   private String _F_lang2;  // 
   private String _F_description;  // 
   
//   private final int lang1_featCode;
//   private final int lang2_featCode;
//   private final int description_featCode;
 
   /** Never called.  Disable default constructor
   * @generated */
  protected LanguagePair() {}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public LanguagePair(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public LanguagePair(JCas jcas) {
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
  //* Feature: lang1

  /** getter for lang1 - gets 
   * @generated
   * @return value of the feature 
   */
  public String getLang1() { return _F_lang1;}
    
  /** setter for lang1 - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLang1(String v) {
          _casView.setWithJournal(this, _typeImpl.getFeatureByBaseName("lang1"), () -> _F_lang1 = v); 
      }    
   
    
  //*--------------*
  //* Feature: lang2

  /** getter for lang2 - gets 
   * @generated
   * @return value of the feature 
   */
  public String getLang2() { return _F_lang2;}
    
  /** setter for lang2 - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLang2(String v) {
          _casView.setWithJournal(this, _typeImpl.getFeatureByBaseName("lang2"), () -> _F_lang2 = v); 
      }    
   
    
  //*--------------*
  //* Feature: description

  /** getter for description - gets 
   * @generated
   * @return value of the feature 
   */
  public String getDescription() { return _F_description;}
    
  /** setter for description - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setDescription(String v) {
    _casView.setWithCheckAndJournal(this, _typeImpl.getFeatureByBaseName("description").getCode(), () -> _F_description = v);
  }
}
    