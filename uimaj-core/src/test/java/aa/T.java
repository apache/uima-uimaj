package aa;
//@formatter:off
/* Apache UIMA v3 - First created by JCasGen Thu Jan 04 17:25:52 EST 2018 */


import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP;


/** 
 * Updated by JCasGen Thu Jan 04 17:25:52 EST 2018
 * XML source: C:/au/svnCheckouts/uv3/trunk/uimaj-v3/uimaj-core/src/test/resources/ExampleCas/testTypeSystem_t_1_feature.xml
 * @generated */
public class T extends TOP {
 
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static String _TypeName = "T";
  
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(T.class);
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
   
  public final static String _FeatName_f1 = "f1";


  /* Feature Adjusted Offsets */
  private final static CallSite _FC_f1 = TypeSystemImpl.createCallSite(T.class, "f1");
  private final static MethodHandle _FH_f1 = _FC_f1.dynamicInvoker();

  public static void dumpOffset() throws Throwable {
    System.out.println("_FC_f1 offset value is " + ( (int)_FH_f1.invokeExact()));
  }
   
  /** Never called.  Disable default constructor
   * @generated */
  protected T() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public T(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public T(JCas jcas) {
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
  //* Feature: f1

  /** getter for f1 - gets 
   * @generated
   * @return value of the feature 
   */
  public int getF1() { return _getIntValueNc(wrapGetIntCatchException(_FH_f1));}
    
  /** setter for f1 - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setF1(int v) {
    _setIntValueNfc(wrapGetIntCatchException(_FH_f1), v);
  }    
    
  }
