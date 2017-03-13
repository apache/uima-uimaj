

   
/* Apache UIMA v3 - First created by JCasGen Fri Dec 16 10:23:12 EST 2016 */

package aa;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;




/** 
 * Updated by JCasGen Fri Dec 16 10:23:12 EST 2016
 * XML source: C:/au/svnCheckouts/branches/uimaj/v3-alpha/uimaj-core/src/test/java/org/apache/uima/jcas/test/generatedx.xml
 * @generated */
public class ConcreteType extends AbstractType {
 
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static String _TypeName = "aa.ConcreteType";
  
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(ConcreteType.class);
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
   
  public final static String _FeatName_concreteString = "concreteString";


  /* Feature Adjusted Offsets */
  public final static int _FI_concreteString = TypeSystemImpl.getAdjustedFeatureOffset("concreteString");

   
  /** Never called.  Disable default constructor
   * @generated */
  protected ConcreteType() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public ConcreteType(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public ConcreteType(JCas jcas) {
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
  //* Feature: concreteString

  /** getter for concreteString - gets 
   * @generated
   * @return value of the feature 
   */
  public String getConcreteString() { return _getStringValueNc(_FI_concreteString);}
    
  /** setter for concreteString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setConcreteString(String v) {
    _setStringValueNfc(_FI_concreteString, v);
  }    
    
  }

    