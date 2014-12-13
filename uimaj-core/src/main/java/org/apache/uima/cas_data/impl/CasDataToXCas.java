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

package org.apache.uima.cas_data.impl;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.cas_data.CasData;
import org.apache.uima.cas_data.FeatureStructure;
import org.apache.uima.cas_data.FeatureValue;
import org.apache.uima.cas_data.PrimitiveArrayFS;
import org.apache.uima.cas_data.PrimitiveValue;
import org.apache.uima.cas_data.ReferenceArrayFS;
import org.apache.uima.cas_data.ReferenceValue;
import org.apache.uima.internal.util.StringUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Takes a CasData and generates XCAS SAX events.
 * 
 * 
 */
public class CasDataToXCas {
  private String mDocumentTextTypeName = "uima.cpm.DocumentText";

  private String mDocumentTextFeatureName = "value";

  private String mXCasDocTextTag = "uima.cpm.DocumentText";

  private boolean mIncludeAnnotationSpannedText = false;

  private List<String> mTypesToFilter = null;

  /**
   * Gets the name of the CASData FeatureStructure Type that stores the document text.
   * 
   * @return the document text type name
   */
  public String getDocumentTextTypeName() {
    return mDocumentTextTypeName;
  }

  /**
   * Sets the name of the CASData FeatureStructure Type that stores the document text.
   * 
   * @param aDocumentTextTypeName the document text type name
   */
  public void setDocumentTextTypeName(String aDocumentTextTypeName) {
    mDocumentTextTypeName = aDocumentTextTypeName;
  }

  /**
   * Gets the name of the CASData Feature that stores the document text.
   * 
   * @return the document text feature name
   */
  public String getDocumentTextFeatureName() {
    return mDocumentTextFeatureName;
  }

  /**
   * Sets the name of the CASData Feature that stores the document text.
   * 
   * @param aDocumentTextFeatureName
   *          the document text feature name
   */
  public void setDocumentTextFeatureName(String aDocumentTextFeatureName) {
    mDocumentTextFeatureName = aDocumentTextFeatureName;
  }

  /**
   * Sets the name of the XCAS tag that will contain the document text.
   * 
   * @param aXCasDocTextTag
   *          the document text tag
   */
  public void setXCasDocumentTextTagName(String aXCasDocTextTag) {
    mXCasDocTextTag = aXCasDocTextTag;
  }

  /**
   * @param aIncludeAnnotationSpannedText -
   */
  public void setIncludeAnnotationSpannedText(boolean aIncludeAnnotationSpannedText) {
    mIncludeAnnotationSpannedText = aIncludeAnnotationSpannedText;
  }

  /**
   * Specifies names of types that will not be included in the XCAS
   * 
   * @param aTypesToFilter -
   */
  public void setTypesToFilter(String[] aTypesToFilter) {
    mTypesToFilter = Arrays.asList(aTypesToFilter);
  }

  /**
   * Sets the ContentHandler to receive the SAX events.
   * 
   * @param aHandler -
   */
  public void setContentHandler(ContentHandler aHandler) {
    mHandler = aHandler;
  }

  /**
   * Generates XCAS for a CasData. SAX events representing the XCAS will be sent to the
   * ContentHandler registered via {@link #setContentHandler(ContentHandler)}.
   * 
   * @param aCasData
   *          the CasData from which XCAS will be generated
   * 
   * @throws SAXException
   *           if the ContentHandler throws a SAX Exception
   */
  public void generateXCas(CasData aCasData) throws SAXException {
    generateXCas(aCasData, null, true);
  }

  /**
   * Special form of {@link #generateXCas(CasData)} that allows a UEID (Universal Entity ID) element
   * to be added as the first element in the XCAS.
   * 
   * @param aCasData
   *          the CasData from which XCAS will be generated
   * @param aUEID
   *          the UEID to add to the XCAS
   * 
   * @throws SAXException
   *           if the ContentHandler throws a SAX Exception
   */
  public void generateXCas(CasData aCasData, String aUEID) throws SAXException {
    generateXCas(aCasData, aUEID, true);
  }

  /**
   * Special form of {@link #generateXCas(CasData)} that allows a UEID (Universal Entity ID) element
   * to be added as the first element in the XCAS and also allows start/end document SAX calls to be
   * supressed.
   * 
   * @param aCasData
   *          the CasData from which XCAS will be generated
   * @param aUEID
   *          the UEID to add to the XCAS
   * @param aSendStartAndEndDocEvents
   *          true to send SAX events for start and end of document, false to supress them.
   * 
   * @throws SAXException
   *           if the ContentHandler throws a SAX Exception
   */
  public void generateXCas(CasData aCasData, String aUEID, boolean aSendStartAndEndDocEvents)
          throws SAXException {
    if (aSendStartAndEndDocEvents) {
      mHandler.startDocument();
    }

    DocTextHolder docTextHolder = new DocTextHolder();

    // start enclosing CAS tag
    mHandler.startElement("", "CAS", "CAS", new AttributesImpl());

    // add UEID if specified
    if (aUEID != null) {
      mHandler.startElement("", "UEID", "UEID", new AttributesImpl());
      mHandler.characters(aUEID.toCharArray(), 0, aUEID.length());
      mHandler.endElement("", "UEID", "UEID");
    }

    // iterate over FSs and generate XCAS
    Iterator<FeatureStructure> iter = aCasData.getFeatureStructures();
    while (iter.hasNext()) {
      FeatureStructure fs = iter.next();
      if (mTypesToFilter == null || !mTypesToFilter.contains(fs.getType())) {
        _generate(fs, docTextHolder);
      }
    }

    // end enclosing CAS tag
    mHandler.endElement("", "CAS", "CAS");

    if (aSendStartAndEndDocEvents) {
      mHandler.endDocument();
    }
  }

  private void _generate(FeatureStructure aFS, DocTextHolder aDocTextHolder) throws SAXException {
    // document text is special case
    if (aFS.getType().equals(this.getDocumentTextTypeName())) {
      _generateDocFS(aFS, aDocTextHolder);
    } else {
      // generate attributes for features (except "value" feature, which is represented in element
      // text)
      AttributesImpl attrs = new AttributesImpl();
      String contentValue = null;

      if (aFS.getId() != null) {
        attrs.addAttribute("", "_id", "_id", "CDATA", aFS.getId());
      }

      int[] indexed = aFS.getIndexed();
      if (indexed.length > 0) {
        StringBuffer indexedStr = new StringBuffer();
        indexedStr.append(indexed[0]);
        for (int i = 1; i < indexed.length; i++) {
          indexedStr.append(' ').append(indexed[i]);
        }
        attrs.addAttribute("", "_indexed", "_indexed", "CDATA", indexedStr.toString());
      }

      String[] features = aFS.getFeatureNames();
      for (int i = 0; i < features.length; i++) {
        FeatureValue featVal = aFS.getFeatureValue(features[i]);
        if (featVal instanceof PrimitiveValue) {
          if (!"value".equals(features[i])) {
            attrs.addAttribute("", features[i], features[i], "CDATA", featVal.toString());
          } else {
            contentValue = featVal.toString();
          }
        } else {
          if (!"value".equals(features[i])) {
            attrs.addAttribute("", "_ref_" + features[i], "_ref_" + features[i], "CDATA",
                    ((ReferenceValue) featVal).getTargetId());
          } else {
            contentValue = ((ReferenceValue) featVal).getTargetId();
          }
        }
      }

      String xcasElementName = getXCasElementName(aFS);
      mHandler.startElement("", xcasElementName, xcasElementName, attrs);

      // encode array subelements
      String[] arrayElems = null;
      if (aFS instanceof PrimitiveArrayFS) {
        arrayElems = ((PrimitiveArrayFS) aFS).toStringArray();
      } else if (aFS instanceof ReferenceArrayFS) {
        arrayElems = ((ReferenceArrayFS) aFS).getIdRefArray();
      }
      if (arrayElems != null) {
        for (int j = 0; j < arrayElems.length; j++) {
          mHandler.startElement("", "i", "i", new AttributesImpl());
          if (arrayElems[j] != null) {
            mHandler.characters(arrayElems[j].toCharArray(), 0, arrayElems[j].length());
          }
          mHandler.endElement("", "i", "i");
        }
      }

      // encode "value" feature, if specified, as content
      if (contentValue != null) {
        mHandler.characters(contentValue.toCharArray(), 0, contentValue.length());
      }
      // encode annotation spanned text, if this FS has valid begin and end features
      else if (mIncludeAnnotationSpannedText && aDocTextHolder.docText != null
              && aDocTextHolder.docText.length > 0) {
        FeatureValue begin = aFS.getFeatureValue("begin");
        FeatureValue end = aFS.getFeatureValue("end");
        if (begin instanceof PrimitiveValue && end instanceof PrimitiveValue) {
          int beginChar = ((PrimitiveValue) begin).toInt();
          int endChar = ((PrimitiveValue) end).toInt();
          if (beginChar >= 0 && endChar > beginChar && endChar <= aDocTextHolder.docText.length) {
            // special case: do not include text of annotations spanning entire document
            if (beginChar > 0 || endChar < aDocTextHolder.docText.length) {
              mHandler.characters(aDocTextHolder.docText, beginChar, endChar - beginChar);
            }
          }
        }
      }

      mHandler.endElement("", xcasElementName, xcasElementName);
    }
  }

  /**
   * Gets the XCAS element name for a FS. This is usually the same as the type name, but the
   * sequences _colon_ and _dash_ are translated to the characters : and -, respectively.
   * 
   * @param aFS
   *          feature structures
   * @return XCAS element name for this feature structure
   */
  private String getXCasElementName(FeatureStructure aFS) {
    return StringUtils.replaceAll(StringUtils.replaceAll(aFS.getType(), "_colon_", ":"), "_dash_",
            "-");
  }

  /**
   * @param aFS
   */
  private void _generateDocFS(FeatureStructure aFS, DocTextHolder aDocTextHolder)
          throws SAXException {
    AttributesImpl attrs = new AttributesImpl();
    String textFeature = this.getDocumentTextFeatureName();
    FeatureValue docTextValue = aFS.getFeatureValue(textFeature);
    if (docTextValue != null) {
      String text = docTextValue.toString();
      aDocTextHolder.docText = text.toCharArray();
      if (!textFeature.equals("value")) {
        attrs.addAttribute("", "_content", "_content", "CDATA", textFeature);
      }
      mHandler.startElement("", mXCasDocTextTag, mXCasDocTextTag, attrs);
      mHandler.characters(aDocTextHolder.docText, 0, aDocTextHolder.docText.length);
      mHandler.endElement("", mXCasDocTextTag, mXCasDocTextTag);
    } else {
      mHandler.startElement("", mXCasDocTextTag, mXCasDocTextTag, attrs);
      mHandler.endElement("", mXCasDocTextTag, mXCasDocTextTag);
    }
  }

  private ContentHandler mHandler;

  private static class DocTextHolder {
    char[] docText;
  }

}
