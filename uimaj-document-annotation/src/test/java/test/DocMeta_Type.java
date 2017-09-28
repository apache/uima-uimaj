
/* First created by JCasGen Sun Sep 24 15:24:44 EDT 2017 */
package test;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.DocumentAnnotation_Type;

/** 
 * Updated by JCasGen Sun Sep 24 15:24:44 EDT 2017
 * @generated */
public class DocMeta_Type extends DocumentAnnotation_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = DocMeta.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("test.DocMeta");
 
  /** @generated */
  final Feature casFeat_feat;
  /** @generated */
  final int     casFeatCode_feat;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getFeat(int addr) {
        if (featOkTst && casFeat_feat == null)
      jcas.throwFeatMissing("feat", "test.DocMeta");
    return ll_cas.ll_getStringValue(addr, casFeatCode_feat);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setFeat(int addr, String v) {
        if (featOkTst && casFeat_feat == null)
      jcas.throwFeatMissing("feat", "test.DocMeta");
    ll_cas.ll_setStringValue(addr, casFeatCode_feat, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public DocMeta_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_feat = jcas.getRequiredFeatureDE(casType, "feat", "uima.cas.String", featOkTst);
    casFeatCode_feat  = (null == casFeat_feat) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_feat).getCode();

  }
}



    