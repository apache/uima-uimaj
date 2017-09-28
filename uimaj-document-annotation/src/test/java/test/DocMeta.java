

/* First created by JCasGen Sun Sep 24 15:24:44 EDT 2017 */
package test;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.DocumentAnnotation;


/** 
 * Updated by JCasGen Sun Sep 24 15:24:44 EDT 2017
 * XML source: C:/au/svnCheckouts/trunk/uimaj-current/uimaj/uimaj-document-annotation/src/test/resources/ExampleCas/testTypeSystem_docmetadata.xml
 * @generated */
public class DocMeta extends DocumentAnnotation {
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
 
  /** Never called.  Disable default constructor
   * @generated */
  protected DocMeta() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public DocMeta(int addr, TOP_Type type) {
    super(addr, type);
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
  public String getFeat() {
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_feat == null)
      jcasType.jcas.throwFeatMissing("feat", "test.DocMeta");
    return jcasType.ll_cas.ll_getStringValue(addr, ((DocMeta_Type)jcasType).casFeatCode_feat);}
    
  /** setter for feat - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setFeat(String v) {
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_feat == null)
      jcasType.jcas.throwFeatMissing("feat", "test.DocMeta");
    jcasType.ll_cas.ll_setStringValue(addr, ((DocMeta_Type)jcasType).casFeatCode_feat, v);}    
  }

    