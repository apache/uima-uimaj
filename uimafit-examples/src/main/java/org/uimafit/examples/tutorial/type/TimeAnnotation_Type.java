/* First created by JCasGen Fri Apr 02 09:47:21 MDT 2010 */
package org.uimafit.examples.tutorial.type;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/**
 * Updated by JCasGen Fri Jun 11 20:10:52 MDT 2010
 * 
 * @generated
 */
public class TimeAnnotation_Type extends DateTimeAnnotation_Type {
	/** @generated */
	protected FSGenerator getFSGenerator() {
		return fsGenerator;
	}

	/** @generated */
	private final FSGenerator fsGenerator = new FSGenerator() {
		public FeatureStructure createFS(int addr, CASImpl cas) {
			if (TimeAnnotation_Type.this.useExistingInstance) {
				// Return eq fs instance if already created
				FeatureStructure fs = TimeAnnotation_Type.this.jcas.getJfsFromCaddr(addr);
				if (null == fs) {
					fs = new TimeAnnotation(addr, TimeAnnotation_Type.this);
					TimeAnnotation_Type.this.jcas.putJfsFromCaddr(addr, fs);
					return fs;
				}
				return fs;
			}
			else return new TimeAnnotation(addr, TimeAnnotation_Type.this);
		}
	};

	/** @generated */
	public final static int typeIndexID = TimeAnnotation.typeIndexID;

	/**
	 * @generated
	 * @modifiable
	 */
	public final static boolean featOkTst = JCasRegistry.getFeatOkTst("org.uimafit.examples.tutorial.type.TimeAnnotation");

	/**
	 * initialize variables to correspond with Cas Type and Features
	 * 
	 * @generated
	 */
	public TimeAnnotation_Type(JCas jcas, Type casType) {
		super(jcas, casType);
		casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

	}
}
