
/* First created by JCasGen Wed Jul 14 10:08:01 MDT 2010 */
package org.uimafit.examples.type;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;

/**
 * Updated by JCasGen Wed Jul 14 10:08:01 MDT 2010 XML source:
 * C:/Users/Philip/Documents
 * /Academic/workspace/uimafit-parent/uimaFIT-examples/src
 * /main/resources/org/uimafit/examples/TypeSystem.xml
 * 
 * @generated
 */
public class Sentence extends Annotation {
	/**
	 * @generated
	 * @ordered
	 */
	public final static int typeIndexID = JCasRegistry.register(Sentence.class);

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
	protected Sentence() {
	}

	/**
	 * Internal - constructor used by generator
	 * 
	 * @generated
	 */
	public Sentence(int addr, TOP_Type type) {
		super(addr, type);
		readObject();
	}

	/** @generated */
	public Sentence(JCas jcas) {
		super(jcas);
		readObject();
	}

	/** @generated */
	public Sentence(JCas jcas, int begin, int end) {
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

}
