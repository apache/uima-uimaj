

   
/* Apache UIMA v3 - First created by JCasGen Wed Oct 28 14:10:07 EDT 2015 */

package org.apache.uima.cas.test;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Wed Oct 28 14:10:07 EDT 2015
 * XML source: C:/au/svnCheckouts/branches/uimaj/experiment-v3-jcas/uimaj-core/src/test/resources/CASTests/desc/StringSubtypeTest.xml
 * @generated */
public class StringSubtypeAnnotation extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(StringSubtypeAnnotation.class);
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
  private String _F_stringSetFeature;  // 
 
  /** Never called.  Disable default constructor
   * @generated */
  protected StringSubtypeAnnotation() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public StringSubtypeAnnotation(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public StringSubtypeAnnotation(JCas jcas) {
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
  //* Feature: stringSetFeature

  /** getter for stringSetFeature - gets 
   * @generated
   * @return value of the feature 
   */
  public String getStringSetFeature() { return _F_stringSetFeature;}
    
  /** setter for stringSetFeature - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setStringSetFeature(String v) {
          _typeImpl.getFeatureByBaseName("stringSetFeature").validateIsInAllowedValue(v);
         
      _casView.setWithJournal(this, _typeImpl.getFeatureByBaseName("stringSetFeature"), () -> _F_stringSetFeature = v);
      }    
  }

    