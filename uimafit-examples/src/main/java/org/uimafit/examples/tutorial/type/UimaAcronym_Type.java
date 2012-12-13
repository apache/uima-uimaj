/* First created by JCasGen Fri Apr 02 09:36:39 MDT 2010 */
package org.uimafit.examples.tutorial.type;

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
 * Updated by JCasGen Fri Jun 11 20:10:52 MDT 2010
 * 
 * @generated
 */
public class UimaAcronym_Type extends Annotation_Type {
	/** @generated */
	protected FSGenerator getFSGenerator() {
		return fsGenerator;
	}

	/** @generated */
	private final FSGenerator fsGenerator = new FSGenerator() {
		public FeatureStructure createFS(int addr, CASImpl cas) {
			if (UimaAcronym_Type.this.useExistingInstance) {
				// Return eq fs instance if already created
				FeatureStructure fs = UimaAcronym_Type.this.jcas.getJfsFromCaddr(addr);
				if (null == fs) {
					fs = new UimaAcronym(addr, UimaAcronym_Type.this);
					UimaAcronym_Type.this.jcas.putJfsFromCaddr(addr, fs);
					return fs;
				}
				return fs;
			}
			else return new UimaAcronym(addr, UimaAcronym_Type.this);
		}
	};

	/** @generated */
	public final static int typeIndexID = UimaAcronym.typeIndexID;

	/**
	 * @generated
	 * @modifiable
	 */
	public final static boolean featOkTst = JCasRegistry.getFeatOkTst("org.uimafit.examples.tutorial.type.UimaAcronym");

	/** @generated */
	final Feature casFeat_expandedForm;

	/** @generated */
	final int casFeatCode_expandedForm;

	/** @generated */
	public String getExpandedForm(int addr) {
		if (featOkTst && casFeat_expandedForm == null) jcas.throwFeatMissing("expandedForm", "org.uimafit.examples.tutorial.type.UimaAcronym");
		return ll_cas.ll_getStringValue(addr, casFeatCode_expandedForm);
	}

	/** @generated */
	public void setExpandedForm(int addr, String v) {
		if (featOkTst && casFeat_expandedForm == null) jcas.throwFeatMissing("expandedForm", "org.uimafit.examples.tutorial.type.UimaAcronym");
		ll_cas.ll_setStringValue(addr, casFeatCode_expandedForm, v);
	}

	/**
	 * initialize variables to correspond with Cas Type and Features
	 * 
	 * @generated
	 */
	public UimaAcronym_Type(JCas jcas, Type casType) {
		super(jcas, casType);
		casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

		casFeat_expandedForm = jcas.getRequiredFeatureDE(casType, "expandedForm", "uima.cas.String", featOkTst);
		casFeatCode_expandedForm = (null == casFeat_expandedForm) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl) casFeat_expandedForm).getCode();

	}
}
