

   
/* Apache UIMA v3 - First created by JCasGen Fri Sep 22 09:43:59 EDT 2017 */

package org.apache.uima.jcas.tcas;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;




/** 
 * Updated by JCasGen Fri Sep 22 09:43:59 EDT 2017
 * XML source: C:/au/svnCheckouts/uv3/trunk/uimaj-v3/uimaj-document-annotation/src/test/resources/ExampleCas/testTypeSystem_docmetadata.xml
 * @generated */
public class DocMeta extends DocumentAnnotation {
 
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static String _TypeName = "org.apache.uima.jcas.tcas.DocMeta";
  
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(DocMeta.class);
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
   
  public final static String _FeatName_feat = "feat";


  /* Feature Adjusted Offsets */
  public final static int _FI_feat = TypeSystemImpl.getAdjustedFeatureOffset("feat");

   
  /** Never called.  Disable default constructor
   * @generated */
  protected DocMeta() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public DocMeta(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public DocMeta(JCas jcas) {
    super(jcas);
    readObject();   
  } 


  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public DocMeta(JCas jcas, int begin, int end) {
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
  //* Feature: feat

  /** getter for feat - gets 
   * @generated
   * @return value of the feature 
   */
  public String getFeat() { return _getStringValueNc(_FI_feat);}
    
  /** setter for feat - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setFeat(String v) {
    _setStringValueNfc(_FI_feat, v);
  }    
    
  }

    