

   
/* Apache UIMA v3 - First created by JCasGen Wed Mar 02 13:49:48 EST 2016 */

package sofa.test;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Wed Mar 02 13:49:48 EST 2016
 * XML source: C:/au/svnCheckouts/branches/uimaj/experiment-v3-jcas/uimaj-core/src/test/resources/ExampleCas/testTypeSystem.xml
 * @generated */
public class CrossAnnotation extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(CrossAnnotation.class);
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
  public final static int _FI_otherAnnotation = TypeSystemImpl.getAdjustedFeatureOffset("otherAnnotation");

   
  /** Never called.  Disable default constructor
   * @generated */
  protected CrossAnnotation() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public CrossAnnotation(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public CrossAnnotation(JCas jcas) {
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
  //* Feature: otherAnnotation

  /** getter for otherAnnotation - gets 
   * @generated
   * @return value of the feature 
   */
  public Annotation getOtherAnnotation() { return (Annotation)(_getFeatureValueNc(_FI_otherAnnotation));}
    
  /** setter for otherAnnotation - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setOtherAnnotation(Annotation v) {
    _setFeatureValueNcWj(_FI_otherAnnotation, v);
  }    
    
  }

    