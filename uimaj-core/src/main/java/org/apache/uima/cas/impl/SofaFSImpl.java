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

package org.apache.uima.cas.impl;

import java.io.InputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.Type;

/**
 * Implementation of the {@link org.apache.uima.cas.SofaFS SofaFS} interface.
 * 
 * 
 */
public class SofaFSImpl extends FeatureStructureImplC implements SofaFS {

	private static class SofaFSGenerator implements FSGenerator<SofaFSImpl> {

		private SofaFSGenerator() {
			super();
		}

		/**
     * @see org.apache.uima.cas.impl.FSGenerator#createFS(int, LowLevelCAS)
     */
    public SofaFSImpl createFS(int addr, CASImpl cas) {
			return new SofaFSImpl(addr, cas);
		}

	}

	static FSGenerator<SofaFSImpl> getSofaFSGenerator() {
		return new SofaFSGenerator();
	}

	public SofaFSImpl(int addr, CASImpl cas) {
		super(cas, addr);
	}

	// IMPORTANT: Methods below here are duplicated in
	// org.apache.uima.jcas.cas.Sofa.
	// Any changes should be made in both places.

	/**
   * @see org.apache.uima.cas.ArrayFS#size() This method is duplicated in
   *      org.apache.uima.jcas.cas.Sofa. Any changes should be made in both places.
   */
	public void setLocalSofaData(FeatureStructure aFS) {
		final Feature arrayFeat = this.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFAARRAY);
		if (isSofaDataSet()) {
			CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFADATA_ALREADY_SET,
					new String[] { "SetLocalSofaData(FeatureStructure)" });
			throw e;
		}
		Type type = aFS.getType();
		if (!type.isArray()) {
			CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_TYPE,
					new String[] { "Array FS", type.getName() });
			throw e;
		}
		if (!type.getName().equals(CAS.TYPE_NAME_BYTE_ARRAY)
				&& !type.getName().equals(CAS.TYPE_NAME_DOUBLE_ARRAY)
				&& !type.getName().equals(CAS.TYPE_NAME_FLOAT_ARRAY)
				&& !type.getName().equals(CAS.TYPE_NAME_INTEGER_ARRAY)
				&& !type.getName().equals(CAS.TYPE_NAME_LONG_ARRAY)
				&& !type.getName().equals(CAS.TYPE_NAME_SHORT_ARRAY)
				&& !type.getName().equals(CAS.TYPE_NAME_STRING_ARRAY)) {
			CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_TYPE,
					new String[] { "Byte/Float/Integer/Short/String/Long/Double Array", type.getName() });
			throw e;
		}

		super.setFeatureValue(arrayFeat, aFS);
	}

	/**
   * @see org.apache.uima.cas.ArrayFS#size() This method is duplicated in
   *      org.apache.uima.jcas.cas.Sofa. Any changes should be made in both places.
   */
	public void setLocalSofaData(String aString) {
		final Feature stringFeat = this.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFASTRING);
		if (isSofaDataSet()) {
			CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFADATA_ALREADY_SET,
					new String[] { "setLocalSofaData(String)" });
			throw e;
		}
		super.setStringValue(stringFeat, aString);
		// create or update the document annotation for this Sofa's view
		CAS view = this.casImpl.getView(this);
		((CASImpl) view).updateDocumentAnnotation();
	}

	/**
   * @see org.apache.uima.cas.ArrayFS#size() This method is duplicated in
   *      org.apache.uima.jcas.cas.Sofa. Any changes should be made in both places.
   */
	public void setRemoteSofaURI(String aURI) {
		final Feature uriFeat = this.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFAURI);

		if (isSofaDataSet()) {
			CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFADATA_ALREADY_SET,
					new String[] { "setRemoteSofaURI(String)" });
			throw e;
		}
		super.setStringValue(uriFeat, aURI);
	}

	private boolean isSofaDataSet() {
		final Feature uriFeat = this.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFAURI);
		final Feature arrayFeat = this.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFAARRAY);
		final Feature stringFeat = this.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFASTRING);

		if (null != this.getStringValue(uriFeat) || null != this.getFeatureValue(arrayFeat)
				|| null != this.getStringValue(stringFeat)) {
			return true;
		}
		return false;
	}

	/**
   * @see org.apache.uima.cas.ArrayFS#size() This method is duplicated in
   *      org.apache.uima.jcas.cas.Sofa. Any changes should be made in both places.
   */
	public FeatureStructure getLocalFSData() {
		final Feature arrayFeat = this.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFAARRAY);
		return this.getFeatureValue(arrayFeat);
	}

	/**
   * @see org.apache.uima.cas.ArrayFS#size() This method is duplicated in
   *      org.apache.uima.jcas.cas.Sofa. Any changes should be made in both places.
   */
	public String getLocalStringData() {
		final Feature stringFeat = this.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFASTRING);
		return this.getStringValue(stringFeat);
	}

	// override setStringValue for SofaFS to not work!
	// This method is duplicated in org.apache.uima.jcas.cas.Sofa. Any changes
	// should be made in both places.
	public void setStringValue(Feature feat, String val) {
		CASRuntimeException e = new CASRuntimeException(CASRuntimeException.PROTECTED_SOFA_FEATURE);
		throw e;
	}

	// override setFeatureValue for SofaFS to not work!
	// This method is duplicated in org.apache.uima.jcas.cas.Sofa. Any changes
	// should be made in both places.
	public void setFeatureValue(Feature feat, FeatureStructure fs) {
		CASRuntimeException e = new CASRuntimeException(CASRuntimeException.PROTECTED_SOFA_FEATURE);
		throw e;
	}

  // override setIntValue for SofaFS to not work!
  // This method is duplicated in org.apache.uima.jcas.cas.Sofa. Any changes
  // should be made in both places.
  public void setIntValue(Feature feat, Integer val) {
    CASRuntimeException e = new CASRuntimeException(CASRuntimeException.PROTECTED_SOFA_FEATURE);
    throw e;
  }

	// This method is duplicated in org.apache.uima.jcas.cas.Sofa. Any changes
	// should be made in both places.
	public String getSofaMime() {
		final Feature mimeFeat = this.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFAMIME);
		return this.casImpl.getLowLevelCAS().ll_getStringValue(super.addr,
				((FeatureImpl) mimeFeat).getCode());
	}

	// This method is duplicated in org.apache.uima.jcas.cas.Sofa. Any changes
	// should be made in both places.
	public String getSofaURI() {
		final Feature uriFeat = this.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFAURI);
		return this.casImpl.getLowLevelCAS().ll_getStringValue(super.addr,
				((FeatureImpl) uriFeat).getCode());
	}

	// This method is duplicated in org.apache.uima.jcas.cas.Sofa. Any changes
	// should be made in both places.
  // ** Note: This gets the feature named "sofaNum".
	public int getSofaRef() {
		final Feature numFeat = this.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFANUM);
		return this.casImpl.getLowLevelCAS().ll_getIntValue(super.addr,
				((FeatureImpl) numFeat).getCode());
	}

	public InputStream getSofaDataStream() {
		return this.casImpl.getSofaDataStream(this);
	}

	/**
   * @see org.apache.uima.cas.SofaFS#getSofaID()  This method is duplicated in
   *      org.apache.uima.jcas.cas.Sofa. Any changes should be made in both places.
   */

	public String getSofaID() {
		final Feature sofaIDFeat = this.casImpl.getTypeSystem().getFeatureByFullName(
				CAS.FEATURE_FULL_NAME_SOFAID);
		return this.getStringValue(sofaIDFeat);
	}
}
