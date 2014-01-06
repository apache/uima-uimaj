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

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XCASParsingException;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.cas_data.CasData;
import org.apache.uima.cas_data.PrimitiveArrayFS;
import org.apache.uima.cas_data.ReferenceArrayFS;
import org.apache.uima.internal.util.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX ContentHandler that reads XCAS and creates a CasData.
 * 
 * 
 */
public class XCasToCasDataSaxHandler extends DefaultHandler {
  // ///////////////////////////////////////////////////////////////////////
  // Internal states for the parser.

  // Expect the start of the XML document.
  private static final int DOC_STATE = 0;

  // At the top level. Expect a FS.
  private static final int FS_STATE = 1;

  // Inside a FS. Expect features, or the end of the FS.
  private static final int FEAT_STATE = 2;

  // Inside FS. We have seen a _content attribute, and expect text.
  private static final int CONTENT_STATE = 3;

  // Inside a feature element. We expect the feature value.
  private static final int FEAT_CONTENT_STATE = 4;

  // Inside an array element. Expect array element value.
  private static final int ARRAY_ELE_CONTENT_STATE = 5;

  // Inside an array FS. Expect an array element, or the end of the FS.
  private static final int ARRAY_ELE_STATE = 6;

  // End parser states.
  // ///////////////////////////////////////////////////////////////////////

  private static final String reservedAttrPrefix = "_";

  // For error message printing, if the Locator object can't provide source
  // of XML input.
  private static final String unknownXMLSource = "<unknown>";

  private static final String DEFAULT_CONTENT_FEATURE = "value";

  // private long time;

  // SAX locator. Used for error message generation.
  private Locator locator;

  // The CasData we're filling.
  private CasData cas;

  // What we expect next.
  private int state;

  // StringBuffer to accumulate text.
  private StringBuffer buffer;

  // Most recently created FS. Needed for array elements
  // and embedded feature values.
  private FeatureStructureImpl currentFS;

  // The name of the content feature, if we've seen one.
  private String currentContentFeat;

  // The current position when parsing array elements.
  private int arrayPos;

  // The type of the array we're currently reading. Needed for proper
  // treatment of array element values.
  private int arrayType;

  /**
   * Create new XCasToCasDataSaxHandler.
   * 
   * @param aCasData
   *          the CasData to which FeatureStructures parsed from XCAS will be appended
   */
  public XCasToCasDataSaxHandler(CasData aCasData) {
    super();
    this.buffer = new StringBuffer();
    this.cas = aCasData;
  }

  private final void resetBuffer() {
    // this.buffer.delete(0, this.buffer.length());
    this.buffer = new StringBuffer();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#startDocument()
   */
  public void startDocument() throws SAXException {
    // Do setup work in the constructor.
    this.state = DOC_STATE;
    // System.out.println("Starting to read document.");
    // time = System.currentTimeMillis();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String,
   *      java.lang.String, org.xml.sax.Attributes)
   */
  public void startElement(String nameSpaceURI, String localName, String qualifiedName,
          Attributes attrs) throws SAXException {
    resetBuffer();
    switch (state) {
      case DOC_STATE: {
        if (!qualifiedName.equals(XCASSerializer.casTagName)) {
          throw createException(XCASParsingException.WRONG_ROOT_TAG, qualifiedName);
        }
        this.state = FS_STATE;
        break;
      }
      case FS_STATE: {
        this.currentContentFeat = DEFAULT_CONTENT_FEATURE;
        readFS(qualifiedName, attrs);
        break;
      }
      case ARRAY_ELE_STATE: {
        readArrayElement(qualifiedName, attrs);
        break;
      }
      default: {
        // If we're not in an element expecting state, raise an error.
        throw createException(XCASParsingException.TEXT_EXPECTED, qualifiedName);
      }
    }
  }

  // Get ready to read array element.
  private void readArrayElement(String eleName, Attributes attrs) throws SAXParseException {
    if (!eleName.equals(XCASSerializer.ARRAY_ELEMENT_TAG)) {
      throw createException(XCASParsingException.ARRAY_ELE_EXPECTED, eleName);
    }
    if (attrs.getLength() > 0) {
      throw createException(XCASParsingException.ARRAY_ELE_ATTRS);
    }
    this.state = ARRAY_ELE_CONTENT_STATE;
    resetBuffer();
  }

  // Create a new FS.
  private void readFS(String qualifiedName, Attributes attrs) throws SAXParseException {
    if (isArrayType(qualifiedName)) {
      readArray(qualifiedName, attrs);
    } else {
      this.currentFS = new FeatureStructureImpl();
      this.currentFS.setType(getCasTypeName(qualifiedName));
      readFS(this.currentFS, attrs);
    }
    this.cas.addFeatureStructure(this.currentFS);
  }

  /**
   * Gets the CAS type name corresponding to an XCAS tag name. The type name is usually equal to the
   * tag name, but the characters : and - are translated into the sequences _colon_ and _dash_,
   * respectively.
   * 
   * @param aTagName
   *          XCAS tag name
   * @return CAS type name corresponding to this tag
   */
  private String getCasTypeName(String aTagName) {
    return StringUtils.replaceAll(StringUtils.replaceAll(aTagName, ":", "_colon_"), "-", "_dash_");
  }

  private void readFS(FeatureStructureImpl fsImpl, Attributes attrs) throws SAXParseException {
    String attrName, attrValue;
    for (int i = 0; i < attrs.getLength(); i++) {
      attrName = attrs.getQName(i);
      attrValue = attrs.getValue(i);
      if (attrName.startsWith(reservedAttrPrefix)) {
        if (attrName.equals(XCASSerializer.ID_ATTR_NAME)) {
          fsImpl.setId(attrValue);
        } else if (attrName.equals(XCASSerializer.CONTENT_ATTR_NAME)) {
          this.currentContentFeat = attrValue;
        } else if (attrName.equals(XCASSerializer.INDEXED_ATTR_NAME)) {
          if (attrValue.equals(XCASSerializer.TRUE_VALUE)) {
            fsImpl.setIndexed(new int[] { 1 }); // Backwards compatible CAS, has one default text
            // Sofa
          } else if (!attrValue.equals("false")) {
            fsImpl.setIndexed(parseIntArray(attrValue));
          }
        } else {
          handleFeature(fsImpl, attrName, attrValue);
        }
      } else {
        handleFeature(fsImpl, attrName, attrValue);
      }
    }

    // Set the state; we're either expecting features, or _content.
    // APL - 6/28/04 - even if _content attr is not specified, we can still have content, which
    // would
    // be assigned to the "value" feature, as per XCAS spec. FEAT_STATE did not really seem to be
    // working, anyway.
    this.state = CONTENT_STATE;
    // if (this.state != CONTENT_STATE)
    // {
    // this.state = FEAT_STATE;
    // }
  }

  /**
   * Parse a space-separated string into an integer array.
   */
  private int[] parseIntArray(String val) {
    String[] strVals;
    val = val.trim();
    if ("".equals(val)) {
      strVals = new String[0];
    } else {
      strVals = val.split("\\s+");
    }
    int[] intVals = new int[strVals.length];
    for (int i = 0; i < strVals.length; i++) {
      intVals[i] = Integer.parseInt(strVals[i]);
    }
    return intVals;
  }

  // Create a new array FS.
  private void readArray(String type, Attributes attrs) throws SAXParseException {
    String attrName, attrVal;
    int[] indexed = new int[0];
    int size = 0;
    String id = null;
    for (int i = 0; i < attrs.getLength(); i++) {
      attrName = attrs.getQName(i);
      attrVal = attrs.getValue(i);
      if (attrName.equals(XCASSerializer.ID_ATTR_NAME)) {
        id = attrVal;
      } else if (attrName.equals(XCASSerializer.ARRAY_SIZE_ATTR)) {
        try {
          size = Integer.parseInt(attrVal);
          if (size < 0) {
            throw createException(XCASParsingException.ILLEGAL_ARRAY_SIZE, attrVal);
          }
        } catch (NumberFormatException e) {
          throw createException(XCASParsingException.INTEGER_EXPECTED, attrVal);
        }
      } else if (attrName.equals(XCASSerializer.INDEXED_ATTR_NAME)) {
        if (attrVal.equals(XCASSerializer.TRUE_VALUE)) {
          indexed = new int[] { 1 }; // Backwards compatible CAS, has one default text Sofa
        } else if (!attrVal.equals("false")) {
          indexed = parseIntArray(attrVal);
        }
      } else {
        throw createException(XCASParsingException.ILLEGAL_ARRAY_ATTR, attrName);
      }
    }
    // Hang on to those for setting array values.
    this.arrayPos = 0;
    if (CAS.TYPE_NAME_INTEGER_ARRAY.equals(type)) {
      this.currentFS = new PrimitiveArrayFSImpl(new int[size]);
      this.arrayType = INT_TYPE;
    } else if (CAS.TYPE_NAME_FLOAT_ARRAY.equals(type)) {
      this.currentFS = new PrimitiveArrayFSImpl(new float[size]);
      this.arrayType = FLOAT_TYPE;
    } else if (CAS.TYPE_NAME_STRING_ARRAY.equals(type)) {
      this.currentFS = new PrimitiveArrayFSImpl(new String[size]);
      this.arrayType = STRING_TYPE;
    } else {
      this.currentFS = new ReferenceArrayFSImpl(new String[size]);
      this.arrayType = FS_TYPE;
    }
    this.currentFS.setId(id);
    this.currentFS.setType(type);
    this.currentFS.setIndexed(indexed);
    this.currentFS.setFeatureValue(XCASSerializer.ARRAY_SIZE_ATTR, new PrimitiveValueImpl(size));
    this.state = ARRAY_ELE_STATE;
  }

  // The definition of a null value. Any other value must be in the expected
  // format.
  private final boolean emptyVal(String val) {
    return ((val == null) || (val.length() == 0));
  }

  // Create a feature value from a string representation.
  private void handleFeature(FeatureStructureImpl fsImpl, String featName, String featVal)
          throws SAXParseException {
    if (featName.startsWith(XCASSerializer.REF_PREFIX)) {
      String realFeatName = featName.substring(XCASSerializer.REF_PREFIX.length());
      fsImpl.setFeatureValue(realFeatName, new ReferenceValueImpl(featVal));
    } else {
      fsImpl.setFeatureValue(featName, new PrimitiveValueImpl(featVal));
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#characters(char[], int, int)
   */
  public void characters(char[] chars, int start, int length) throws SAXException {
    if ((this.state == CONTENT_STATE)
            || (this.state == ARRAY_ELE_CONTENT_STATE) || (this.state == FEAT_CONTENT_STATE)) {
      // When we're in a text expecting state, add the characters to the
      // text buffer. Else, do nothing.
      buffer.append(chars, start, length);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  public void endElement(String nsURI, String localName, String qualifiedName) throws SAXException {
    switch (this.state) {
      case DOC_STATE: {
        // Do nothing.
        break;
      }
      case FS_STATE: {
        this.state = DOC_STATE;
        break;
      }
      case FEAT_STATE: {
        this.state = FS_STATE;
        break;
      }
      case CONTENT_STATE: {
        if (!isAllWhitespace(buffer)) {
          // Set the value of the content feature.
          handleFeature(this.currentFS, currentContentFeat, buffer.toString());
        }
        this.state = FS_STATE;
        break;
      }
      case FEAT_CONTENT_STATE: {
        // Create a feature value from an element.
        handleFeature(this.currentFS, qualifiedName, buffer.toString());
        this.state = FEAT_STATE;
        break;
      }
      case ARRAY_ELE_CONTENT_STATE: {
        // Create an array value.
        addArrayElement(buffer.toString());
        this.state = ARRAY_ELE_STATE;
        break;
      }
      case ARRAY_ELE_STATE: {
        this.state = FS_STATE;
        break;
      }
    }
  }

  boolean isAllWhitespace(StringBuffer b) {
    final int len = b.length();
    for (int i = 0; i < len; i++) {
      if (!Character.isWhitespace(b.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private void addArrayElement(String content) throws SAXParseException {
    switch (arrayType) {
      case INT_TYPE: {
        if (!emptyVal(content)) {
          try {
            ((PrimitiveArrayFS) this.currentFS).toIntArray()[arrayPos] = Integer.parseInt(content);
          } catch (NumberFormatException e) {
            throw createException(XCASParsingException.INTEGER_EXPECTED, content);
          }
        }
        break;
      }
      case FLOAT_TYPE: {
        if (!emptyVal(content)) {
          try {
            ((PrimitiveArrayFS) this.currentFS).toFloatArray()[arrayPos] = Float
                    .parseFloat(content);
          } catch (NumberFormatException e) {
            throw createException(XCASParsingException.FLOAT_EXPECTED, content);
          }
        }
        break;
      }
      case STRING_TYPE: {
        ((PrimitiveArrayFS) this.currentFS).toStringArray()[arrayPos] = content;
        break;
      }
      case FS_TYPE: {
        if (!emptyVal(content)) {
          ((ReferenceArrayFS) this.currentFS).getIdRefArray()[arrayPos] = content;
        }
        break;
      }
    }
    ++arrayPos;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#endDocument()
   */
  public void endDocument() throws SAXException {
    // nothing to do
  }

  private XCASParsingException createException(int code) {
    XCASParsingException e = new XCASParsingException(code);
    String source = unknownXMLSource;
    String line = unknownXMLSource;
    String col = unknownXMLSource;
    if (locator != null) {
      source = locator.getSystemId();
      if (source == null) {
        source = locator.getPublicId();
      }
      if (source == null) {
        source = unknownXMLSource;
      }
      line = Integer.toString(locator.getLineNumber());
      col = Integer.toString(locator.getColumnNumber());
    }
    e.addArgument(source);
    e.addArgument(line);
    e.addArgument(col);
    return e;
  }

  private XCASParsingException createException(int code, String arg) {
    XCASParsingException e = createException(code);
    e.addArgument(arg);
    return e;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
   */
  public void error(SAXParseException e) throws SAXException {
    throw e;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
   */
  public void fatalError(SAXParseException e) throws SAXException {
    throw e;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
   */
  public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {
    // Since we're not validating, we don't need to do anything; this won't
    // be called.
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
   */
  public void setDocumentLocator(Locator loc) {
    // System.out.println("Setting document locator.");
    this.locator = loc;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
   */
  public void warning(SAXParseException e) throws SAXException {
    throw e;
  }

  private static final int INT_TYPE = 0;

  private static final int FLOAT_TYPE = 1;

  private static final int STRING_TYPE = 2;

  private static final int FS_TYPE = 3;

  private boolean isArrayType(String typeName) {
    return CAS.TYPE_NAME_INTEGER_ARRAY.equals(typeName)
            || CAS.TYPE_NAME_FLOAT_ARRAY.equals(typeName)
            || CAS.TYPE_NAME_STRING_ARRAY.equals(typeName)
            || CAS.TYPE_NAME_FS_ARRAY.equals(typeName);
  }

}
