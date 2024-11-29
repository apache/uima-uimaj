

   
/* Apache UIMA v3 - First created by JCasGen Wed Jun 01 09:28:33 CEST 2022 */

package org.apache.uima.test.jcasfeatureoffsettest;
 


import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;




/** 
 * Updated by JCasGen Wed Jun 01 09:31:48 CEST 2022
 * XML source: /Users/bluefire/git/uima-uimaj/uimaj-core/src/test/resources/JCasFeatureOffsetTest/SubtypeDescriptor1.xml
 * @generated */
public class Subtype extends Base {
 
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static String _TypeName = "org.apache.uima.test.jcasfeatureoffsettest.Subtype";
  
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Subtype.class);
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
   
  public final static String _FeatName_featureX = "featureX";


  /* Feature Adjusted Offsets */
  private final static CallSite _FC_featureX = TypeSystemImpl.createCallSite(Subtype.class, "featureX");
  private final static MethodHandle _FH_featureX = _FC_featureX.dynamicInvoker();

   
  /* *******************
   *   Feature Offsets *
   * *******************/ 
   


  /* Feature Adjusted Offsets */

   
  /** Never called.  Disable default constructor
   * @generated */
  @Deprecated
  @SuppressWarnings ("deprecation")
  protected Subtype() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public Subtype(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Subtype(JCas jcas) {
    super(jcas);
    readObject();   
  } 


  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Subtype(JCas jcas, int begin, int end) {
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
  //* Feature: featureX

  /** getter for featureX - gets 
   * @generated
   * @return value of the feature 
   */
  public String getFeatureX() { 
    return _getStringValueNc(wrapGetIntCatchException(_FH_featureX));
  }
    
  /** setter for featureX - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setFeatureX(String v) {
    _setStringValueNfc(wrapGetIntCatchException(_FH_featureX), v);
  }    
    
  }

    