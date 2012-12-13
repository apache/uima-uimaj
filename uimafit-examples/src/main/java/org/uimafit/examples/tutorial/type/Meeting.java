
/* First created by JCasGen Fri Apr 02 09:50:10 MDT 2010 */
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
public class Meeting extends Annotation {
	/**
	 * @generated
	 * @ordered
	 */
	public final static int typeIndexID = JCasRegistry.register(Meeting.class);

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
	protected Meeting() {
	}

	/**
	 * Internal - constructor used by generator
	 * 
	 * @generated
	 */
	public Meeting(int addr, TOP_Type type) {
		super(addr, type);
		readObject();
	}

	/** @generated */
	public Meeting(JCas jcas) {
		super(jcas);
		readObject();
	}

	/** @generated */
	public Meeting(JCas jcas, int begin, int end) {
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
	// * Feature: room

	/**
	 * getter for room - gets
	 * 
	 * @generated
	 */
	public RoomNumber getRoom() {
		if (Meeting_Type.featOkTst && ((Meeting_Type) jcasType).casFeat_room == null) jcasType.jcas.throwFeatMissing("room",
				"org.uimafit.examples.tutorial.type.Meeting");
		return (RoomNumber) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Meeting_Type) jcasType).casFeatCode_room)));
	}

	/**
	 * setter for room - sets
	 * 
	 * @generated
	 */
	public void setRoom(RoomNumber v) {
		if (Meeting_Type.featOkTst && ((Meeting_Type) jcasType).casFeat_room == null) jcasType.jcas.throwFeatMissing("room",
				"org.uimafit.examples.tutorial.type.Meeting");
		jcasType.ll_cas.ll_setRefValue(addr, ((Meeting_Type) jcasType).casFeatCode_room, jcasType.ll_cas.ll_getFSRef(v));
	}

	// *--------------*
	// * Feature: date

	/**
	 * getter for date - gets
	 * 
	 * @generated
	 */
	public DateAnnotation getDate() {
		if (Meeting_Type.featOkTst && ((Meeting_Type) jcasType).casFeat_date == null) jcasType.jcas.throwFeatMissing("date",
				"org.uimafit.examples.tutorial.type.Meeting");
		return (DateAnnotation) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Meeting_Type) jcasType).casFeatCode_date)));
	}

	/**
	 * setter for date - sets
	 * 
	 * @generated
	 */
	public void setDate(DateAnnotation v) {
		if (Meeting_Type.featOkTst && ((Meeting_Type) jcasType).casFeat_date == null) jcasType.jcas.throwFeatMissing("date",
				"org.uimafit.examples.tutorial.type.Meeting");
		jcasType.ll_cas.ll_setRefValue(addr, ((Meeting_Type) jcasType).casFeatCode_date, jcasType.ll_cas.ll_getFSRef(v));
	}

	// *--------------*
	// * Feature: startTime

	/**
	 * getter for startTime - gets
	 * 
	 * @generated
	 */
	public TimeAnnotation getStartTime() {
		if (Meeting_Type.featOkTst && ((Meeting_Type) jcasType).casFeat_startTime == null) jcasType.jcas.throwFeatMissing("startTime",
				"org.uimafit.examples.tutorial.type.Meeting");
		return (TimeAnnotation) (jcasType.ll_cas
				.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Meeting_Type) jcasType).casFeatCode_startTime)));
	}

	/**
	 * setter for startTime - sets
	 * 
	 * @generated
	 */
	public void setStartTime(TimeAnnotation v) {
		if (Meeting_Type.featOkTst && ((Meeting_Type) jcasType).casFeat_startTime == null) jcasType.jcas.throwFeatMissing("startTime",
				"org.uimafit.examples.tutorial.type.Meeting");
		jcasType.ll_cas.ll_setRefValue(addr, ((Meeting_Type) jcasType).casFeatCode_startTime, jcasType.ll_cas.ll_getFSRef(v));
	}

	// *--------------*
	// * Feature: endTime

	/**
	 * getter for endTime - gets
	 * 
	 * @generated
	 */
	public TimeAnnotation getEndTime() {
		if (Meeting_Type.featOkTst && ((Meeting_Type) jcasType).casFeat_endTime == null) jcasType.jcas.throwFeatMissing("endTime",
				"org.uimafit.examples.tutorial.type.Meeting");
		return (TimeAnnotation) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Meeting_Type) jcasType).casFeatCode_endTime)));
	}

	/**
	 * setter for endTime - sets
	 * 
	 * @generated
	 */
	public void setEndTime(TimeAnnotation v) {
		if (Meeting_Type.featOkTst && ((Meeting_Type) jcasType).casFeat_endTime == null) jcasType.jcas.throwFeatMissing("endTime",
				"org.uimafit.examples.tutorial.type.Meeting");
		jcasType.ll_cas.ll_setRefValue(addr, ((Meeting_Type) jcasType).casFeatCode_endTime, jcasType.ll_cas.ll_getFSRef(v));
	}
}
