

   
/* Apache UIMA v3 - First created by JCasGen Tue Mar 08 10:58:47 EST 2016 */

package org.apache.uima.test;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
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
  private final static CallSite _FC_aFS = TypeSystemImpl.createCallSite(RefTypes.class, "aFS");
  private final static MethodHandle _FH_aFS = _FC_aFS.dynamicInvoker();
  private final static CallSite _FC_aListFs = TypeSystemImpl.createCallSite(RefTypes.class, "aListFs");
  private final static MethodHandle _FH_aListFs = _FC_aListFs.dynamicInvoker();
  private final static CallSite _FC_aArrayFS = TypeSystemImpl.createCallSite(RefTypes.class, "aArrayFS");
  private final static MethodHandle _FH_aArrayFS = _FC_aArrayFS.dynamicInvoker();

   
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
  public Annotation getAFS() { return (Annotation)(_getFeatureValueNc(wrapGetIntCatchException(_FH_aFS)));}
    
  /** setter for aFS - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAFS(Annotation v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aFS), v);
  }    
    
   
    
  //*--------------*
  //* Feature: aListFs

  /** getter for aListFs - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getAListFs() { return (FSList)(_getFeatureValueNc(wrapGetIntCatchException(_FH_aListFs)));}
    
  /** setter for aListFs - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAListFs(FSList v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aListFs), v);
  }    
       
  //*--------------*
  //* Feature: aArrayFS

  /** getter for aArrayFS - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getAArrayFS() { return (FSArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayFS)));}
    
  /** setter for aArrayFS - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAArrayFS(FSArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_aArrayFS), v);
  }    
    
    
  /** indexed getter for aArrayFS - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public Annotation getAArrayFS(int i) {
     return (Annotation)(((FSArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayFS)))).get(i));} 

  /** indexed setter for aArrayFS - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setAArrayFS(int i, Annotation v) {
    ((FSArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_aArrayFS)))).set(i, v);
  }  
  }

    