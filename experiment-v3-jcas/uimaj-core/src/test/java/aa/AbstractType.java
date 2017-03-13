

   
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
public class AbstractType extends TOP {
 
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static String _TypeName = "aa.AbstractType";
  
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(AbstractType.class);
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
   
  public final static String _FeatName_abstractInt = "abstractInt";


  /* Feature Adjusted Offsets */
  public final static int _FI_abstractInt = TypeSystemImpl.getAdjustedFeatureOffset("abstractInt");

   
  /** Never called.  Disable default constructor
   * @generated */
  protected AbstractType() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public AbstractType(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public AbstractType(JCas jcas) {
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
  //* Feature: abstractInt

  /** getter for abstractInt - gets 
   * @generated
   * @return value of the feature 
   */
  public int getAbstractInt() { return _getIntValueNc(_FI_abstractInt);}
    
  /** setter for abstractInt - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAbstractInt(int v) {
    _setIntValueNfc(_FI_abstractInt, v);
  }    
    
  }

    