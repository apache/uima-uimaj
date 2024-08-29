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
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.SofaFSImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCasRegistry;

public class Sofa extends TOP implements SofaFSImpl {

  /* public static string for use where constants are needed, e.g. in some Java Annotations */
  public static final String _TypeName = CAS.TYPE_NAME_SOFA;
  public static final String _FeatName_sofaNum = "sofaNum"; // int
  public static final String _FeatName_sofaID = "sofaID"; // string
  public static final String _FeatName_mimeType = "mimeType"; // string
  public static final String _FeatName_sofaArray = "sofaArray"; // TOP
  public static final String _FeatName_sofaString = "sofaString"; // string
  public static final String _FeatName_sofaURI = "sofaURI"; // string

  public static final int typeIndexID = JCasRegistry.register(Sofa.class);

  public static final int type = typeIndexID;

  @Override
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /* local data */
  // these static ints are for fast index corruption checking
  // public final static int _FI_sofaNum = TypeSystemImpl.getAdjustedFeatureOffset("sofaNum");
  // public final static int _FI_sofaID = TypeSystemImpl.getAdjustedFeatureOffset("sofaID");
  // public final static int _FI_mimeType = TypeSystemImpl.getAdjustedFeatureOffset("mimeType");
  // public final static int _FI_sofaArray = TypeSystemImpl.getAdjustedFeatureOffset("sofaArray");
  // public final static int _FI_sofaString = TypeSystemImpl.getAdjustedFeatureOffset("sofaString");
  // public final static int _FI_sofaURI = TypeSystemImpl.getAdjustedFeatureOffset("sofaURI");

  private static final CallSite _FC_sofaNum = TypeSystemImpl.createCallSiteForBuiltIn(Sofa.class,
          "sofaNum");
  private static final CallSite _FC_sofaID = TypeSystemImpl.createCallSiteForBuiltIn(Sofa.class,
          "sofaID");
  private static final CallSite _FC_mimeType = TypeSystemImpl.createCallSiteForBuiltIn(Sofa.class,
          "mimeType");
  private static final CallSite _FC_sofaArray = TypeSystemImpl.createCallSiteForBuiltIn(Sofa.class,
          "sofaArray");
  private static final CallSite _FC_sofaString = TypeSystemImpl.createCallSiteForBuiltIn(Sofa.class,
          "sofaString");
  private static final CallSite _FC_sofaURI = TypeSystemImpl.createCallSiteForBuiltIn(Sofa.class,
          "sofaURI");

  private static final MethodHandle _FH_sofaNum = _FC_sofaNum.dynamicInvoker();
  private static final MethodHandle _FH_sofaID = _FC_sofaID.dynamicInvoker();
  private static final MethodHandle _FH_mimeType = _FC_mimeType.dynamicInvoker();
  private static final MethodHandle _FH_sofaArray = _FC_sofaArray.dynamicInvoker();
  private static final MethodHandle _FH_sofaString = _FC_sofaString.dynamicInvoker();
  private static final MethodHandle _FH_sofaURI = _FC_sofaURI.dynamicInvoker();

  // private final int _F_sofaNum;
  // private final String _F_sofaID; // view name or _InitialView
  // private String _F_mimeType; // may be changed
  // private TOP _F_sofaArray;
  // private String _F_sofaString;
  // private String _F_sofaURI;

  protected Sofa() {
    // _F_sofaNum = 0;
    // _F_sofaID = null;
    // _F_mimeType = null;
  }

  /**
   * used by generator, not used Make a new Sofa
   * 
   * @param c
   *          -
   * @param t
   *          -
   */
  public Sofa(TypeImpl t, CASImpl c) {
    super(t, c);
    // _F_sofaNum = 0;
    // _F_sofaID = null;
  }

  public Sofa(TypeImpl t, CASImpl c, int sofaNum, String viewName, String mimeType) {
    super(t, c);
    _setIntValueNcNj(wrapGetIntCatchException(_FH_sofaNum), sofaNum);
    _setRefValueCommon(wrapGetIntCatchException(_FH_sofaID), viewName);
    _setRefValueCommon(wrapGetIntCatchException(_FH_mimeType), mimeType);
    // _F_sofaNum = sofaNum;
    // _F_sofaID = viewName;
    // _F_mimeType = mimeType;
  }

  // no constructor for Sofa for users
  // use cas createSofa instead

  // /**
  // *
  // * @param jcas JCas
  // * @param ID the sofa ID
  // * @param mimeType the mime type
  // *
  // * @deprecated As of v2.0, use {@link JCasImpl#createView(String)} to create a view, which will
  // * also create the Sofa for that view.
  //
  // */
  // @Deprecated
  // public Sofa(JCas jcas, SofaID ID, String mimeType) {
  // super(jcas);
  // final CASImpl casImpl = jcasType.casImpl;
  // casImpl.addSofa(this, ID.getSofaID(), mimeType);
  // casImpl.getView(this); // needed to make reset work
  // }

  // *--------------*
  // * Feature: sofaNum
  // ** Note: this gets the same feature, sofaNum, as getSofaRef, below
  /**
   * getter for sofaNum
   * 
   * @return the sofa number
   */
  @Override
  public int getSofaNum() {
    return _getIntValueNc(wrapGetIntCatchException(_FH_sofaNum));
  }

  // *--------------*
  // * Feature: sofaID

  /**
   * getter for sofaID
   * 
   * @return the sofaID, which is the same as the view name
   */
  @Override
  public String getSofaID() {
    return _getStringValueNc(wrapGetIntCatchException(_FH_sofaID));
  }

  // *--------------*
  // * Feature: mimeType

  /**
   * getter for mimeType - gets
   * 
   * @return the mime type
   */
  public String getMimeType() {
    return _getStringValueNc(wrapGetIntCatchException(_FH_mimeType));
  }

  /**
   * @see org.apache.uima.cas.SofaFS#setLocalSofaData(FeatureStructure) This method is duplicated in
   *      SofaFSImpl. Any changes should be made in both places. aFS must be an array
   */
  @Override
  public void setLocalSofaData(FeatureStructure aFS) {
    if (isSofaDataSet()) {
      throwAlreadySet("setLocalSofaData()");
    }
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_sofaArray), aFS);
  }

  public void setLocalSofaData(FeatureStructure aFS, String mimeType) {
    setLocalSofaData(aFS);
    setMimeType(mimeType);
  }

  /**
   * @see org.apache.uima.cas.SofaFS#setLocalSofaData(String)
   */
  @Override
  public void setLocalSofaData(String aString) {
    setLocalSofaDataNoDocAnnotUpdate(aString);

    // create or update the document annotation for this Sofa's view
    ((CASImpl) (_casView.getView(this))).updateDocumentAnnotation();
  }

  /**
   * Internal use: used by deserializers
   * 
   * @param aString
   *          the string to update
   */
  public void setLocalSofaDataNoDocAnnotUpdate(String aString) {
    if (isSofaDataSet()) {
      throwAlreadySet("setLocalSofaData()");
    }
    _setStringValueNcWj(wrapGetIntCatchException(_FH_sofaString), aString);
  }

  public void setLocalSofaData(String aString, String mimeType) {
    setLocalSofaData(aString);
    setMimeType(mimeType);
  }

  /**
   * @see org.apache.uima.cas.SofaFS#getLocalFSData() returns an UIMA Array whose data represents
   *      the sofa
   */
  @Override
  public FeatureStructure getLocalFSData() {
    return _getFeatureValueNc(wrapGetIntCatchException(_FH_sofaArray));
  }

  /**
   * @see org.apache.uima.cas.SofaFS#getLocalStringData()
   */
  @Override
  public String getLocalStringData() {
    return _getStringValueNc(wrapGetIntCatchException(_FH_sofaString));
  }

  /**
   * @see org.apache.uima.cas.SofaFS#setRemoteSofaURI(String) This method is duplicated in
   *      SofaFSImpl. Any changes should be made in both places.
   */
  @Override
  public void setRemoteSofaURI(String aURI) {
    if (isSofaDataSet()) {
      throwAlreadySet("setRemoteSofaURI()");
    }
    _setStringValueNcWj(wrapGetIntCatchException(_FH_sofaURI), aURI);
  }

  public void setRemoteSofaURI(String aURI, String mimeType) {
    setRemoteSofaURI(aURI);
    setMimeType(mimeType);
  }

  public boolean isSofaDataSet() {
    return getLocalStringData() != null || // string data
            getLocalFSData() != null || // array data
            getSofaURI() != null; // remote data
  }

  @Override
  public String getSofaMime() {
    return _getStringValueNc(wrapGetIntCatchException(_FH_mimeType));
  }

  @Override
  public String getSofaURI() {
    return _getStringValueNc(wrapGetIntCatchException(_FH_sofaURI));
  }

  // ** Note: this gets the feature named "sofaNum"
  @Override
  public int getSofaRef() {
    return _getIntValueNc(wrapGetIntCatchException(_FH_sofaNum));
  }

  @Override
  public InputStream getSofaDataStream() {
    return _casView.getSofaDataStream(this);
  }

  /**
   * These getter methods are for creating method handle access The getter name must match the
   * feature name + transformation - used in generic pretty printing routines
   * 
   * @return -
   */
  public TOP getSofaArray() {
    return _getFeatureValueNc(wrapGetIntCatchException(_FH_sofaArray));
  }

  public String getSofaString() {
    return _getStringValueNc(wrapGetIntCatchException(_FH_sofaString));
  }

  // override setStringValue for SofaFS to prohibit setting in this manner!
  @Override
  public void setStringValue(Feature feat, String val) {
    throw new CASRuntimeException(CASRuntimeException.PROTECTED_SOFA_FEATURE);
  }

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

  public void setMimeType(String v) {
    _setStringValueNcWj(wrapGetIntCatchException(_FH_mimeType), v);
  }
}
