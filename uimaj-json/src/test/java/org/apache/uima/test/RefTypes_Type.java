
/* First created by JCasGen Sat Nov 01 07:38:59 EDT 2014 */
package org.apache.uima.test;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** 
 * Updated by JCasGen Sat Nov 01 07:38:59 EDT 2014
 * @generated */
public class RefTypes_Type extends Annotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (RefTypes_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = RefTypes_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new RefTypes(addr, RefTypes_Type.this);
  			   RefTypes_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new RefTypes(addr, RefTypes_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = RefTypes.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("org.apache.uima.test.RefTypes");
 
  /** @generated */
  final Feature casFeat_aFS;
  /** @generated */
  final int     casFeatCode_aFS;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAFS(int addr) {
        if (featOkTst && casFeat_aFS == null)
      jcas.throwFeatMissing("aFS", "org.apache.uima.test.RefTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aFS);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAFS(int addr, int v) {
        if (featOkTst && casFeat_aFS == null)
      jcas.throwFeatMissing("aFS", "org.apache.uima.test.RefTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aFS, v);}
    
  
 
  /** @generated */
  final Feature casFeat_aListFs;
  /** @generated */
  final int     casFeatCode_aListFs;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAListFs(int addr) {
        if (featOkTst && casFeat_aListFs == null)
      jcas.throwFeatMissing("aListFs", "org.apache.uima.test.RefTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aListFs);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAListFs(int addr, int v) {
        if (featOkTst && casFeat_aListFs == null)
      jcas.throwFeatMissing("aListFs", "org.apache.uima.test.RefTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aListFs, v);}
    
  
 
  /** @generated */
  final Feature casFeat_aArrayFS;
  /** @generated */
  final int     casFeatCode_aArrayFS;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAArrayFS(int addr) {
        if (featOkTst && casFeat_aArrayFS == null)
      jcas.throwFeatMissing("aArrayFS", "org.apache.uima.test.RefTypes");
    return ll_cas.ll_getRefValue(addr, casFeatCode_aArrayFS);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAArrayFS(int addr, int v) {
        if (featOkTst && casFeat_aArrayFS == null)
      jcas.throwFeatMissing("aArrayFS", "org.apache.uima.test.RefTypes");
    ll_cas.ll_setRefValue(addr, casFeatCode_aArrayFS, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getAArrayFS(int addr, int i) {
        if (featOkTst && casFeat_aArrayFS == null)
      jcas.throwFeatMissing("aArrayFS", "org.apache.uima.test.RefTypes");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayFS), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayFS), i);
	return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayFS), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setAArrayFS(int addr, int i, int v) {
        if (featOkTst && casFeat_aArrayFS == null)
      jcas.throwFeatMissing("aArrayFS", "org.apache.uima.test.RefTypes");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayFS), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayFS), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_aArrayFS), i, v);
  }
 



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public RefTypes_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_aFS = jcas.getRequiredFeatureDE(casType, "aFS", "uima.tcas.Annotation", featOkTst);
    casFeatCode_aFS  = (null == casFeat_aFS) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aFS).getCode();

 
    casFeat_aListFs = jcas.getRequiredFeatureDE(casType, "aListFs", "uima.cas.FSList", featOkTst);
    casFeatCode_aListFs  = (null == casFeat_aListFs) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aListFs).getCode();

 
    casFeat_aArrayFS = jcas.getRequiredFeatureDE(casType, "aArrayFS", "uima.cas.FSArray", featOkTst);
    casFeatCode_aArrayFS  = (null == casFeat_aArrayFS) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_aArrayFS).getCode();

  }
}



    