// @formatter:off
/* Apache UIMA v3 - First created by JCasGen Sun Oct 08 19:06:27 EDT 2017 */

package org.apache.lang;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP;


/** 
 * Updated by JCasGen Sun Oct 08 19:06:27 EDT 2017
 * XML source: C:/au/svnCheckouts/uv3/trunk/uimaj-v3/uimaj-core/src/test/java/org/apache/uima/jcas/test/generatedx.xml
 * @generated */
public class LanguagePair extends TOP {
 
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding") public static final String _TypeName = "org.apache.lang.LanguagePair";
  
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding") public static final int typeIndexID = JCasRegistry.register(LanguagePair.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding") public static final int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
 
  /* *******************
   *   Feature Offsets *
   * *******************/ 
   
  public static final String _FeatName_lang1 = "lang1";
  public static final String _FeatName_lang2 = "lang2";
  public static final String _FeatName_description = "description";


  /* Feature Adjusted Offsets */
  private static final CallSite _FC_lang1 = TypeSystemImpl.createCallSite(LanguagePair.class, "lang1");
  private static final MethodHandle _FH_lang1 = _FC_lang1.dynamicInvoker();
  private static final CallSite _FC_lang2 = TypeSystemImpl.createCallSite(LanguagePair.class, "lang2");
  private static final MethodHandle _FH_lang2 = _FC_lang2.dynamicInvoker();
  private static final CallSite _FC_description = TypeSystemImpl.createCallSite(LanguagePair.class, "description");
  private static final MethodHandle _FH_description = _FC_description.dynamicInvoker();

   
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
  public String getLang1() { return _getStringValueNc(wrapGetIntCatchException(_FH_lang1));}
    
  /** setter for lang1 - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLang1(String v) {
    _setStringValueNfc(wrapGetIntCatchException(_FH_lang1), v);
  }    
    
   
    
  //*--------------*
  //* Feature: lang2

  /** getter for lang2 - gets 
   * @generated
   * @return value of the feature 
   */
  public String getLang2() { return _getStringValueNc(wrapGetIntCatchException(_FH_lang2));}
    
  /** setter for lang2 - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLang2(String v) {
    _setStringValueNfc(wrapGetIntCatchException(_FH_lang2), v);
  }    
    
   
    
  //*--------------*
  //* Feature: description

  /** getter for description - gets 
   * @generated
   * @return value of the feature 
   */
  public String getDescription() { return _getStringValueNc(wrapGetIntCatchException(_FH_description));}
    
  /** setter for description - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setDescription(String v) {
    _setStringValueNfc(wrapGetIntCatchException(_FH_description), v);
  }    
    
  }
