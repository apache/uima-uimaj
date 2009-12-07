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

package org.apache.uima.cas_data.impl.vinci;

import java.io.IOException;

import org.xml.sax.SAXException;

import org.apache.uima.adapter.vinci.util.Constants;
import org.apache.uima.adapter.vinci.util.SaxVinciFrameBuilder;
import org.apache.uima.adapter.vinci.util.VinciSaxParser;
import org.apache.uima.cas_data.CasData;
import org.apache.uima.cas_data.impl.CasDataImpl;
import org.apache.uima.cas_data.impl.CasDataToXCas;
import org.apache.uima.cas_data.impl.CasDataUtils;
import org.apache.uima.cas_data.impl.XCasToCasDataSaxHandler;
import org.apache.vinci.transport.document.AFrame;

/**
 * Utilities for converting a VinciFrame to and from a CasData.
 * 
 * 
 */
public class VinciCasDataConverter {
  private String mUeidType;

  private String mUeidFeature;

  private String mCasDataDocTextType;

  private String mCasDataDocTextFeature;

  private String mXCasDocTextTag;

  private boolean mIncludeAnnotationSpannedText;

  /**
   * Creates a new VinciCasDataConverter
   * 
   * @param aUeidType
   *          CasData type that contains the UEID (may be null)
   * @param aUeidFeature
   *          CasData feature that contains the UEID (may be null)
   * @param aCasDataDocTextType
   *          CasData type that contains the document text
   * @param aCasDataDocTextFeature
   *          CasData feature that contains the document text
   * @param aXCasDocTextTag
   *          XCas tag representing the document text
   * @param aIncludeAnnotationSpannedText
   *          if true, when generating XCas for an annotation, the spanned text of the annotation
   *          will be included as the content of the XCas element.
   */
  public VinciCasDataConverter(String aUeidType, String aUeidFeature, String aCasDataDocTextType,
          String aCasDataDocTextFeature, String aXCasDocTextTag,
          boolean aIncludeAnnotationSpannedText) {
    mUeidType = aUeidType;
    mUeidFeature = aUeidFeature;
    mCasDataDocTextType = aCasDataDocTextType;
    mCasDataDocTextFeature = aCasDataDocTextFeature;
    mXCasDocTextTag = aXCasDocTextTag;
    mIncludeAnnotationSpannedText = aIncludeAnnotationSpannedText;
  }

  /**
   * Converts a CasData to a VinciFrame
   * 
   * @param aCasData
   *          CasData to convert
   * @param aParentFrame
   *          VinciFrame to be the parent of the frame created from the CasData
   */
  public void casDataToVinciFrame(CasData aCasData, AFrame aParentFrame) throws IOException,
          SAXException {
    // get UEID if necessary
    String ueid = null;
    if (mUeidType != null && mUeidFeature != null) {
      ueid = CasDataUtils.getFeatureValueByType(aCasData, mUeidType, mUeidFeature);
    }

    // Serialize CasData to XCAS
    // Would be nice to serialize straight to parent frame frame, but we have
    // to change the tag name to KEYS to satisfy the TAE interface
    // spec - sigh.
    AFrame xcasHolder = new AFrame();
    SaxVinciFrameBuilder vinciFrameBuilder = new SaxVinciFrameBuilder();
    vinciFrameBuilder.setParentFrame(xcasHolder);
    CasDataToXCas xcasGenerator = new CasDataToXCas();
    xcasGenerator.setDocumentTextTypeName(mCasDataDocTextType);
    xcasGenerator.setDocumentTextFeatureName(mCasDataDocTextFeature);
    xcasGenerator.setXCasDocumentTextTagName(mXCasDocTextTag);
    xcasGenerator.setIncludeAnnotationSpannedText(mIncludeAnnotationSpannedText);

    xcasGenerator.setContentHandler(vinciFrameBuilder);
    xcasGenerator.generateXCas(aCasData, ueid);
    AFrame xcasFrame = xcasHolder.fgetAFrame("CAS");
    aParentFrame.aadd(Constants.KEYS, xcasFrame);
  }

  /**
   * Converts a VinciFrame to a CasData, appending to an existing CasData.
   * 
   * @param aCasFrame
   *          VinciFrame containing XCAS
   * @param aCasData
   *          CasData to which FeatureStructures from XCAS will be appended
   * 
   * @deprecated Use appendVinciFrameToCasData(Aframe, CasData) or vinciFrameToCasData(AFrame)
   */
  @Deprecated
public void vinciFrameToCasData(AFrame aCasFrame, CasData aCasData) throws SAXException {
    appendVinciFrameToCasData(aCasFrame, aCasData);
  }

  /**
   * Converts a VinciFrame to a CasData.
   * 
   * @param aCasFrame
   *          VinciFrame containing XCAS
   * 
   * @return a new CasData corrsponding to the XCAS in aCasFrame
   */
  public CasData vinciFrameToCasData(AFrame aCasFrame) throws SAXException {
    CasData casData = new CasDataImpl();
    appendVinciFrameToCasData(aCasFrame, casData);
    return casData;
  }

  /**
   * Converts a VinciFrame to a CasData, appending to an existing CasData.
   * 
   * @param aCasFrame
   *          VinciFrame containing XCAS
   * @param aCasData
   *          CasData to which FeatureStructures from XCAS will be appended
   */
  public void appendVinciFrameToCasData(AFrame aCasFrame, CasData aCasData) throws SAXException {
    // Use VinciSaxParser to generate SAX events from VinciFrame, and send
    // them to XCasToCasDataSaxHandler
    VinciSaxParser vinciSaxParser = new VinciSaxParser();
    XCasToCasDataSaxHandler handler = new XCasToCasDataSaxHandler(aCasData);
    handler.startDocument();
    handler.startElement("", "CAS", "CAS", null);
    vinciSaxParser.setContentHandler(handler);
    vinciSaxParser.parse(aCasFrame, false);
    handler.endElement("", "CAS", "CAS");
    handler.endDocument();
  }
}
