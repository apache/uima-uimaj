

   
/* Apache UIMA v3 - First created by JCasGen Fri Dec 16 10:23:12 EST 2016 */

package aa;

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
public class MissingFeatureInCas extends TOP {
 
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static String _TypeName = "aa.MissingFeatureInCas";
  
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(MissingFeatureInCas.class);
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
   
  public final static String _FeatName_haveThisOne = "haveThisOne";
  public final static String _FeatName_missingThisOne = "missingThisOne";
  public final static String _FeatName_changedFType = "changedFType";


  /* Feature Adjusted Offsets */
  public final static int _FI_haveThisOne = TypeSystemImpl.getAdjustedFeatureOffset("haveThisOne");
  public final static int _FI_missingThisOne = TypeSystemImpl.getAdjustedFeatureOffset("missingThisOne");
  public final static int _FI_changedFType = TypeSystemImpl.getAdjustedFeatureOffset("changedFType");

   
  /** Never called.  Disable default constructor
   * @generated */
  protected MissingFeatureInCas() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public MissingFeatureInCas(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public MissingFeatureInCas(JCas jcas) {
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
  //* Feature: haveThisOne

  /** getter for haveThisOne - gets 
   * @generated
   * @return value of the feature 
   */
  public int getHaveThisOne() { return _getIntValueNc(_FI_haveThisOne);}
    
  /** setter for haveThisOne - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setHaveThisOne(int v) {
    _setIntValueNfc(_FI_haveThisOne, v);
  }    
    
   
    
  //*--------------*
  //* Feature: missingThisOne

  /** getter for missingThisOne - gets 
   * @generated
   * @return value of the feature 
   */
  public float getMissingThisOne() { return _getFloatValueNc(_FI_missingThisOne);}
    
  /** setter for missingThisOne - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setMissingThisOne(float v) {
    _setFloatValueNfc(_FI_missingThisOne, v);
  }    
    
   
    
  //*--------------*
  //* Feature: changedFType

  /** getter for changedFType - gets 
   * @generated
   * @return value of the feature 
   */
  public String getChangedFType() { return _getStringValueNc(_FI_changedFType);}
    
  /** setter for changedFType - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setChangedFType(String v) {
    _setStringValueNfc(_FI_changedFType, v);
  }    
    
  }

    