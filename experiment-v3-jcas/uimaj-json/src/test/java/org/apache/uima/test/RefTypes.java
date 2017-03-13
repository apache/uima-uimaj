

   
/* Apache UIMA v3 - First created by JCasGen Tue Mar 08 10:58:47 EST 2016 */

package org.apache.uima.test;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;


import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Tue Mar 08 10:58:47 EST 2016
 * XML source: C:/au/svnCheckouts/branches/uimaj/experiment-v3-jcas/uimaj-json/src/test/resources/CasSerialization/desc/refTypes.xml
 * @generated */
public class RefTypes extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(RefTypes.class);
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
  public final static int _FI_aFS = TypeSystemImpl.getAdjustedFeatureOffset("aFS");
  public final static int _FI_aListFs = TypeSystemImpl.getAdjustedFeatureOffset("aListFs");
  public final static int _FI_aArrayFS = TypeSystemImpl.getAdjustedFeatureOffset("aArrayFS");

   
  /** Never called.  Disable default constructor
   * @generated */
  protected RefTypes() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public RefTypes(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public RefTypes(JCas jcas) {
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
  //* Feature: aFS

  /** getter for aFS - gets 
   * @generated
   * @return value of the feature 
   */
  public Annotation getAFS() { return (Annotation)(_getFeatureValueNc(_FI_aFS));}
    
  /** setter for aFS - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAFS(Annotation v) {
    _setFeatureValueNcWj(_FI_aFS, v);
  }    
    
   
    
  //*--------------*
  //* Feature: aListFs

  /** getter for aListFs - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getAListFs() { return (FSList)(_getFeatureValueNc(_FI_aListFs));}
    
  /** setter for aListFs - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAListFs(FSList v) {
    _setFeatureValueNcWj(_FI_aListFs, v);
  }    
    
   
    
  //*--------------*
  //* Feature: aArrayFS

  /** getter for aArrayFS - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getAArrayFS() { return (FSArray)(_getFeatureValueNc(_FI_aArrayFS));}
    
  /** setter for aArrayFS - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayFS(FSArray v) {
    _setFeatureValueNcWj(_FI_aArrayFS, v);
  }    
    
    
  /** indexed getter for aArrayFS - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public Annotation getAArrayFS(int i) {
     return (Annotation)(((FSArray)(_getFeatureValueNc(_FI_aArrayFS))).get(i));} 

  /** indexed setter for aArrayFS - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayFS(int i, Annotation v) {
    ((FSArray)(_getFeatureValueNc(_FI_aArrayFS))).set(i, v);
  }  
  }

    