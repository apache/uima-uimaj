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

import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCasRegistry;

public class Sofa extends TOP implements SofaFS {
  
  /* public static string for use where constants are needed, e.g. in some Java Annotations */
  public final static String _TypeName = "org.apache.uima.jcas.cas.Sofa";
  public final static String _FeatName_sofaNum    = "sofaNum";
  public final static String _FeatName_sofaID     = "sofaID";
  public final static String _FeatName_mimeType   = "mimeType";
  public final static String _FeatName_sofaArray  = "sofaArray";
  public final static String _FeatName_sofaString = "sofaString";
  public final static String _FeatName_sofaURI    = "sofaURI";

	public final static int typeIndexID = JCasRegistry.register(Sofa.class);

	public final static int type = typeIndexID;
  @Override
  public int getTypeIndexID() {
    return typeIndexID;
  }
  
  /* local data */
  // these static ints are for fast index corruption checking
  public final static int _FI_sofaNum    = TypeSystemImpl.getAdjustedFeatureOffset("sofaNum");
  public final static int _FI_sofaID     = TypeSystemImpl.getAdjustedFeatureOffset("sofaID");
  public final static int _FI_mimeType   = TypeSystemImpl.getAdjustedFeatureOffset("mimeType");
  public final static int _FI_sofaArray  = TypeSystemImpl.getAdjustedFeatureOffset("sofaArray");
  public final static int _FI_sofaString = TypeSystemImpl.getAdjustedFeatureOffset("sofaString");
  public final static int _FI_sofaURI    = TypeSystemImpl.getAdjustedFeatureOffset("sofaURI");
  
//  private final int _F_sofaNum;
//  private final String _F_sofaID;  // view name or _InitialView
//  private String _F_mimeType;      // may be changed
//  private TOP _F_sofaArray;
//  private String _F_sofaString;
//  private String _F_sofaURI;

  protected Sofa() {
//    _F_sofaNum = 0;
//    _F_sofaID = null;
//    _F_mimeType = null;
	}

	 /**
   * used by generator, not used
   * Make a new Sofa
   * @param c -
   * @param t -
   */
  public Sofa(TypeImpl t, CASImpl c) {
    super(t, c);
//    _F_sofaNum = 0;
//    _F_sofaID = null; 
  }
  
  public Sofa(TypeImpl t, CASImpl c, int sofaNum, String viewName, String mimeType) {
    super(t, c);
    _setIntValueNcNj(_FI_sofaNum, sofaNum);
    _setRefValueCommon(_FI_sofaID, viewName);
    _setRefValueCommon(_FI_mimeType, mimeType);
//    _F_sofaNum = sofaNum;
//    _F_sofaID = viewName;
//    _F_mimeType = mimeType;
  }

  // no constructor for Sofa for users
  // use cas createSofa instead
  
	
//	/**
//	 * 
//	 * @param jcas JCas
//	 * @param ID the sofa ID
//	 * @param mimeType the mime type
//	 * 
//   * @deprecated As of v2.0, use {@link JCasImpl#createView(String)} to create a view, which will
//   *             also create the Sofa for that view.
//
//	 */
//	@Deprecated
//	public Sofa(JCas jcas, SofaID ID, String mimeType) {
//		super(jcas);
//		final CASImpl casImpl = jcasType.casImpl;
//		casImpl.addSofa(this, ID.getSofaID(), mimeType);
//    casImpl.getView(this); // needed to make reset work
//	}

	// *--------------*
	// * Feature: sofaNum
  // ** Note: this gets the same feature, sofaNum, as getSofaRef, below
	/**
   * getter for sofaNum
   * @return the sofa number
   */
	public int getSofaNum() { return _getIntValueNc(_FI_sofaNum); }
	
	// *--------------*
	// * Feature: sofaID

	/**
   * getter for sofaID
   * @return the sofaID, which is the same as the view name
   */
	@Override
  public String getSofaID() { return _getStringValueNc(_FI_sofaID); }

	// *--------------*
	// * Feature: mimeType

	/**
   * getter for mimeType - gets
   * @return the mime type
   */
	public String getMimeType() { return _getStringValueNc(_FI_mimeType); }

	/**
   * @see org.apache.uima.cas.SofaFS#setLocalSofaData(FeatureStructure) This method is duplicated in
   *      SofaFSImpl. Any changes should be made in both places.
   * aFS must be an array
   */
  @Override
  public void setLocalSofaData(FeatureStructure aFS) {   
    if (isSofaDataSet()) { throwAlreadySet("setLocalSofaData()"); }
    _setFeatureValueNcWj(_FI_sofaArray, aFS);
  }

	public void setLocalSofaData(FeatureStructure aFS, String mimeType) {
	  setLocalSofaData(aFS);
		setMimeType(mimeType);
	}

	/**
   * @see org.apache.uima.cas.SofaFS#setLocalSofaData(String) This method is duplicated in
   *      SofaFSImpl. Any changes should be made in both places.
   */
  @Override
  public void setLocalSofaData(String aString) {
    if (isSofaDataSet()) { throwAlreadySet("setLocalSofaData()"); }
    _setStringValueNcWj(_FI_sofaString, aString);

    // create or update the document annotation for this Sofa's view
    ((CASImpl)(_casView.getView(this))).updateDocumentAnnotation();
  }

  public void setLocalSofaData(String aString, String mimeType) {
    setLocalSofaData(aString);
    setMimeType(mimeType);
	}

	/**
   * @see org.apache.uima.cas.SofaFS#getLocalFSData()
   * returns an UIMA Array whose data represents the sofa
   */
	@Override
  public FeatureStructure getLocalFSData() { return _getFeatureValueNc(_FI_sofaArray); }

	/**
   * @see org.apache.uima.cas.SofaFS#getLocalStringData() 
   */
	@Override
  public String getLocalStringData() { return _getStringValueNc(_FI_sofaString); }

	/**
   * @see org.apache.uima.cas.SofaFS#setRemoteSofaURI(String) This method is duplicated in
   *      SofaFSImpl. Any changes should be made in both places.
   */
  @Override
  public void setRemoteSofaURI(String aURI) {
    if (isSofaDataSet()) { throwAlreadySet("setRemoteSofaURI()"); }
    _setStringValueNcWj(_FI_sofaURI, aURI);
  }
	
	public void setRemoteSofaURI(String aURI, String mimeType) {
    setRemoteSofaURI(aURI);
    setMimeType(mimeType);
	}

	public boolean isSofaDataSet() {
	  return getLocalStringData() != null || // string data
	         getLocalFSData()     != null || // array data 
	         getSofaURI()         != null;   // remote data
	}
	
  @Override
  public String getSofaMime() { return _getStringValueNc(_FI_mimeType); }

  @Override
  public String getSofaURI() { return _getStringValueNc(_FI_sofaURI); }

  // ** Note: this gets the feature named "sofaNum"
  @Override
  public int getSofaRef() { return _getIntValueNc(_FI_sofaNum); }

  @Override
  public InputStream getSofaDataStream() {
    return _casView.getSofaDataStream(this);
  }
	
  /**
   * These getter methods are for creating method handle access
   * The getter name must match the feature name + transformation
   *   - used in generic pretty printing routines
   * @return -
   */
  public TOP getSofaArray() { return _getFeatureValueNc(_FI_sofaArray); }
  
  public String getSofaString() { return _getStringValueNc(_FI_sofaString); }
    

	// override setStringValue for SofaFS to prohibit setting in this manner!
	@Override
  public void setStringValue(Feature feat, String val) {
	  throw new CASRuntimeException(CASRuntimeException.PROTECTED_SOFA_FEATURE);
	}

	// override setFeatureValue for SofaFS to prohibit setting in this manner!
	@Override
  public void setFeatureValue(Feature feat, FeatureStructure fs) {
		throw new CASRuntimeException(CASRuntimeException.PROTECTED_SOFA_FEATURE);
	}

  // override setIntValue for SofaFS to prohibit setting in this manner!
  public void setIntValue(Feature feat, Integer val) {
    throw new CASRuntimeException(CASRuntimeException.PROTECTED_SOFA_FEATURE);
  }

	
	private void throwAlreadySet(String msg) {
	  throw new CASRuntimeException(CASRuntimeException.SOFADATA_ALREADY_SET, msg);
	}

	public void setMimeType(String v) { _setStringValueNcWj(_FI_mimeType, v); }
}
