

   
/* Apache UIMA v3 - First created by JCasGen Fri Dec 16 10:23:12 EST 2016 */

package org.apache.lang;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;


import org.apache.uima.jcas.cas.TOP;


/** 
 * Updated by JCasGen Fri Dec 16 10:23:12 EST 2016
 * XML source: C:/au/svnCheckouts/branches/uimaj/v3-alpha/uimaj-core/src/test/java/org/apache/uima/jcas/test/generatedx.xml
 * @generated */
public class LanguagePair extends TOP {
 
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static String _TypeName = "org.apache.lang.LanguagePair";
  
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
 
 
  /* *******************
   *   Feature Offsets *
   * *******************/ 
   
  public final static String _FeatName_lang1 = "lang1";
  public final static String _FeatName_lang2 = "lang2";
  public final static String _FeatName_description = "description";


  /* Feature Adjusted Offsets */
  public final static int _FI_lang1 = TypeSystemImpl.getAdjustedFeatureOffset("lang1");
  public final static int _FI_lang2 = TypeSystemImpl.getAdjustedFeatureOffset("lang2");
  public final static int _FI_description = TypeSystemImpl.getAdjustedFeatureOffset("description");

   
  /** Never called.  Disable default constructor
   * @generated */
  protected LanguagePair() {/* intentionally empty block */}
    
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
  public String getLang1() { return _getStringValueNc(_FI_lang1);}
    
  /** setter for lang1 - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLang1(String v) {
    _setStringValueNfc(_FI_lang1, v);
  }    
    
   
    
  //*--------------*
  //* Feature: lang2

  /** getter for lang2 - gets 
   * @generated
   * @return value of the feature 
   */
  public String getLang2() { return _getStringValueNc(_FI_lang2);}
    
  /** setter for lang2 - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLang2(String v) {
    _setStringValueNfc(_FI_lang2, v);
  }    
    
   
    
  //*--------------*
  //* Feature: description

  /** getter for description - gets 
   * @generated
   * @return value of the feature 
   */
  public String getDescription() { return _getStringValueNc(_FI_description);}
    
  /** setter for description - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setDescription(String v) {
    _setStringValueNfc(_FI_description, v);
  }    
    
  }

    