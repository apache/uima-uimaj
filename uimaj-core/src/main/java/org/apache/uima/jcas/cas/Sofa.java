/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.jcas.cas;

import java.io.InputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.SofaID;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.impl.JCasImpl;

/**
 * Updated by JCasGen Fri Apr 29 16:05:04 EDT 2005
 */
public class Sofa extends TOP implements SofaFS {
	/**
   * @generated
   * @ordered
   */
	public final static int typeIndexID = JCasRegistry.register(Sofa.class);

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
	protected Sofa() {
	}

	/**
   * Internal - constructor used by generator
   * 
   * @generated
   * @param addr the address
   * @param type the type
   */
	public Sofa(int addr, TOP_Type type) {
		super(addr, type);
		readObject();
	}

	/**
	 * 
	 * @param jcas JCas
	 * @param ID the sofa ID
	 * @param mimeType the mime type
	 * 
   * @deprecated As of v2.0, use {@link JCasImpl#createView(String)} to create a view, which will
   *             also create the Sofa for that view.

	 */
	@Deprecated
	public Sofa(JCas jcas, SofaID ID, String mimeType) {
		super(jcas);
		final CASImpl casImpl = jcasType.casImpl;
		casImpl.addSofa(casImpl.createFS(this.addr), ID.getSofaID(), mimeType);
    casImpl.getView(this); // needed to make reset work
	}

	/**
   * <!-- begin-user-doc --> Write your own initialization here <!-- end-user-doc -->
   * 
   * @generated modifiable
   */

	private void readObject() {
	}

	// *--------------*
	// * Feature: sofaNum
  // ** Note: this gets the same feature, sofaNum, as getSofaRef, below
	/**
   * getter for sofaNum - gets
   * 
   * @generated
   * @return the sofa number
   */
	public int getSofaNum() {
		if (Sofa_Type.featOkTst && ((Sofa_Type) jcasType).casFeat_sofaNum == null)
			this.jcasType.jcas.throwFeatMissing("sofaNum", "uima.cas.Sofa");
		return jcasType.ll_cas.ll_getIntValue(addr, ((Sofa_Type) jcasType).casFeatCode_sofaNum);
	}

	// *--------------*
	// * Feature: sofaID

	/**
   * getter for sofaID - gets
   * 
   * @generated
   */
	public String getSofaID() {
		if (Sofa_Type.featOkTst && ((Sofa_Type) jcasType).casFeat_sofaID == null)
			this.jcasType.jcas.throwFeatMissing("sofaID", "uima.cas.Sofa");
		return jcasType.ll_cas.ll_getStringValue(addr, ((Sofa_Type) jcasType).casFeatCode_sofaID);
	}

	// *--------------*
	// * Feature: mimeType

	/**
   * getter for mimeType - gets
   * 
   * @generated
   * @return the mime type
   */
	public String getMimeType() {
		if (Sofa_Type.featOkTst && ((Sofa_Type) jcasType).casFeat_mimeType == null)
			this.jcasType.jcas.throwFeatMissing("mimeType", "uima.cas.Sofa");
		return jcasType.ll_cas.ll_getStringValue(addr, ((Sofa_Type) jcasType).casFeatCode_mimeType);
	}

	// IMPORTANT: Methods below here are duplicated in SofaFSImpl. If they are
	// changed here they must be changed in SofaFSImpl as well.

	/**
   * @see org.apache.uima.cas.SofaFS#setLocalSofaData(FeatureStructure) This method is duplicated in
   *      SofaFSImpl. Any changes should be made in both places.
   */
	public void setLocalSofaData(FeatureStructure aFS) {
		final Feature arrayFeat = jcasType.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFAARRAY);
		if (isSofaDataSet()) {
			CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFADATA_ALREADY_SET,
					new String[] { "setLocalSofaData()" });
			throw e;
		}
		super.setFeatureValue(arrayFeat, aFS);
	}

	/**
   * @see org.apache.uima.cas.SofaFS#setLocalSofaData(String) This method is duplicated in
   *      SofaFSImpl. Any changes should be made in both places.
   */
	public void setLocalSofaData(String aString) {
		final Feature stringFeat = jcasType.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFASTRING);
		if (isSofaDataSet()) {
			CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFADATA_ALREADY_SET,
					new String[] { "setLocalSofaData()" });
			throw e;
		}
		super.setStringValue(stringFeat, aString);
		// create or update the document annotation for this Sofa's view
		CAS view = this.getCASImpl().getView(this);
		((CASImpl) view).updateDocumentAnnotation();
	}

	/**
   * @see org.apache.uima.cas.SofaFS#getLocalFSData() This method is duplicated in SofaFSImpl. Any
   *      changes should be made in both places.
   */
	public FeatureStructure getLocalFSData() {
		final Feature arrayFeat = jcasType.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFAARRAY);
		return this.getFeatureValue(arrayFeat);
	}

	/**
   * @see org.apache.uima.cas.SofaFS#getLocalStringData() This method is duplicated in SofaFSImpl.
   *      Any changes should be made in both places.
   */
	public String getLocalStringData() {
		final Feature stringFeat = jcasType.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFASTRING);
		return this.getStringValue(stringFeat);
	}

	/**
   * @see org.apache.uima.cas.SofaFS#setRemoteSofaURI(String) This method is duplicated in
   *      SofaFSImpl. Any changes should be made in both places.
   */
	public void setRemoteSofaURI(String aURI) {
		final Feature uriFeat = jcasType.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFAURI);
		if (isSofaDataSet()) {
			CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFADATA_ALREADY_SET,
					new String[] { "setRemoteSofaURI()" });
			throw e;
		}
		super.setStringValue(uriFeat, aURI);
	}

	private boolean isSofaDataSet() {
		final Feature uriFeat = jcasType.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFAURI);
		final Feature arrayFeat = jcasType.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFAARRAY);
		final Feature stringFeat = jcasType.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFASTRING);

		if (null != this.getStringValue(uriFeat) || null != this.getFeatureValue(arrayFeat)
				|| null != this.getStringValue(stringFeat)) {
			return true;
		}
		return false;
	}

	// override setStringValue for SofaFS to not work!
	// This method is duplicated in SofaFSImpl. Any changes should be made in both places.
	public void setStringValue(Feature feat, String val) {
		CASRuntimeException e = new CASRuntimeException(CASRuntimeException.PROTECTED_SOFA_FEATURE);
		throw e;
	}

	// override setFeatureValue for SofaFS to not work!
	// This method is duplicated in SofaFSImpl. Any changes should be made in both places.
	public void setFeatureValue(Feature feat, FeatureStructure fs) {
		CASRuntimeException e = new CASRuntimeException(CASRuntimeException.PROTECTED_SOFA_FEATURE);
		throw e;
	}

  // override setIntValue for SofaFS to not work!
  // This method is duplicated in SofaFSImpl. Any changes should be made in both places.
  public void setIntValue(Feature feat, Integer val) {
    CASRuntimeException e = new CASRuntimeException(CASRuntimeException.PROTECTED_SOFA_FEATURE);
    throw e;
  }

	// This method is duplicated in SofaFSImpl. Any changes should be made in both places.
	public String getSofaMime() {
		final Feature mimeFeat = jcasType.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFAMIME);
		return jcasType.casImpl.getLowLevelCAS().ll_getStringValue(super.addr,
				((FeatureImpl) mimeFeat).getCode());
	}

	// This method is duplicated in SofaFSImpl. Any changes should be made in both places.
	public String getSofaURI() {
		final Feature uriFeat = jcasType.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFAURI);
		return jcasType.casImpl.getLowLevelCAS().ll_getStringValue(super.addr,
				((FeatureImpl) uriFeat).getCode());
	}

	// This method is duplicated in SofaFSImpl. Any changes should be made in both places.
  // ** Note: this gets the feature named "sofaNum"
	public int getSofaRef() {
		final Feature numFeat = jcasType.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFANUM);
		return jcasType.casImpl.getLowLevelCAS().ll_getIntValue(super.addr,
				((FeatureImpl) numFeat).getCode());
	}

	public InputStream getSofaDataStream() {
		return jcasType.casImpl.getSofaDataStream(this);
	}

}
