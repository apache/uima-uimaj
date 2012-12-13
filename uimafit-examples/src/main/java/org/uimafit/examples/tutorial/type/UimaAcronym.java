
/* First created by JCasGen Fri Apr 02 09:36:38 MDT 2010 */
package org.uimafit.examples.tutorial.type;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;

/**
 * Updated by JCasGen Fri Jun 11 20:10:52 MDT 2010 XML source:
 * C:/Users/Philip/Documents
 * /Academic/workspace/uimaFIT-examples/src/main/resources
 * /org/uimafit/examples/TypeSystem.xml
 * 
 * @generated
 */
public class UimaAcronym extends Annotation {
	/**
	 * @generated
	 * @ordered
	 */
	public final static int typeIndexID = JCasRegistry.register(UimaAcronym.class);

	/**
	 * @generated
	 * @ordered
	 */
	public final static int type = typeIndexID;

	/** @generated */
	public int getTypeIndexID() {
		return typeIndexID;
	}

	/**
	 * Never called. Disable default constructor
	 * 
	 * @generated
	 */
	protected UimaAcronym() {
	}

	/**
	 * Internal - constructor used by generator
	 * 
	 * @generated
	 */
	public UimaAcronym(int addr, TOP_Type type) {
		super(addr, type);
		readObject();
	}

	/** @generated */
	public UimaAcronym(JCas jcas) {
		super(jcas);
		readObject();
	}

	/** @generated */
	public UimaAcronym(JCas jcas, int begin, int end) {
		super(jcas);
		setBegin(begin);
		setEnd(end);
		readObject();
	}

	/**
	 * <!-- begin-user-doc --> Write your own initialization here <!--
	 * end-user-doc -->
	 * 
	 * @generated modifiable
	 */
	private void readObject() {
	}

	// *--------------*
	// * Feature: expandedForm

	/**
	 * getter for expandedForm - gets
	 * 
	 * @generated
	 */
	public String getExpandedForm() {
		if (UimaAcronym_Type.featOkTst && ((UimaAcronym_Type) jcasType).casFeat_expandedForm == null) jcasType.jcas.throwFeatMissing("expandedForm",
				"org.uimafit.examples.tutorial.type.UimaAcronym");
		return jcasType.ll_cas.ll_getStringValue(addr, ((UimaAcronym_Type) jcasType).casFeatCode_expandedForm);
	}

	/**
	 * setter for expandedForm - sets
	 * 
	 * @generated
	 */
	public void setExpandedForm(String v) {
		if (UimaAcronym_Type.featOkTst && ((UimaAcronym_Type) jcasType).casFeat_expandedForm == null) jcasType.jcas.throwFeatMissing("expandedForm",
				"org.uimafit.examples.tutorial.type.UimaAcronym");
		jcasType.ll_cas.ll_setStringValue(addr, ((UimaAcronym_Type) jcasType).casFeatCode_expandedForm, v);
	}
}
