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

package org.apache.uima.resource.metadata.impl;

import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.w3c.dom.Element;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * 
 * 
 */
public class FsIndexKeyDescription_impl extends MetaDataObject_impl implements
                FsIndexKeyDescription {

  static final long serialVersionUID = -4015997042353963398L;

  /**
   * String representations of comparator values used in XML import/export.
   */
  static final String[] COMPARATOR_STRINGS = { "standard", "reverse" };

  private boolean mTypePriority;

  private String mFeatureName;

  private int mComparator;

  /**
   * @see org.apache.uima.analysis_engine.metadata.FsIndexKeyDescription#isTypePriority()
   */
  public boolean isTypePriority() {
    return mTypePriority;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.FsIndexKeyDescription#setTypePriority(boolean)
   */
  public void setTypePriority(boolean aTypePriority) {
    mTypePriority = aTypePriority;
  }

  /**
   * @see org.apache.uima.cas.FsIndexKeyDescription#getFeatureName()
   */
  public String getFeatureName() {
    return mFeatureName;
  }

  /**
   * @see org.apache.uima.cas.FsIndexKeyDescription#setFeatureName(String)
   */
  public void setFeatureName(String aName) {
    mFeatureName = aName;
  }

  /**
   * @see org.apache.uima.cas.FsIndexKeyDescription#getComparator()
   */
  public int getComparator() {
    return mComparator;
  }

  /**
   * @see org.apache.uima.cas.FsIndexKeyDescription#setComparator(int)
   */
  public void setComparator(int aComparator) {
    mComparator = aComparator;
  }

  /**
   * Overridden to handle XML export of the <code>typePriority</code> and <code>comparator</code>
   * properties.
   * 
   * @see org.apache.uima.MetaDataObject_impl#writeAttributeAsElement(String,Class,Object,String,ContentHandler)
   */
  protected void writePropertyAsElement(PropertyXmlInfo aPropInfo, String aNamespace,
                  ContentHandler aContentHandler) throws SAXException {
    if ("typePriority".equals(aPropInfo.propertyName)) {
      // if property is true, just write an empty tag, if false omit
      if (isTypePriority()) {
        aContentHandler.startElement(getXmlizationInfo().namespace, "typePriority", "typePriority",
                        new AttributesImpl());
        aContentHandler.endElement(getXmlizationInfo().namespace, "typePriority", "typePriority");
      }
    } else if (!isTypePriority()) // don't write other properties for a type priority key
    {
      if ("comparator".equals(aPropInfo.propertyName)) {
        // This property has an interger-encoded value which is written to XML
        // as a more user-friendly string.

        aContentHandler.startElement(getXmlizationInfo().namespace, "comparator", "comparator",
                        new AttributesImpl());

        // write value as string
        String str = COMPARATOR_STRINGS[getComparator()];
        aContentHandler.characters(str.toCharArray(), 0, str.length());

        aContentHandler.endElement(getXmlizationInfo().namespace, "comparator", "comparator");
      } else {
        // for all other attributes, use the default superclass behavior
        super.writePropertyAsElement(aPropInfo, aNamespace, aContentHandler);
      }
    }
  }

  /**
   * Overridden to handle XML import of the <code>typePriority</code> and <code>comparator</code>
   * properties.
   * 
   * @see org.apache.uima.resource.impl.MetaDataObject_impl#readPropertyValueFromXMLElement(org.apache.uima.resource.impl.PropertyXmlInfo,
   *      org.w3c.dom.Element, org.apache.uima.util.XMLParser)
   */
  protected void readPropertyValueFromXMLElement(PropertyXmlInfo aPropXmlInfo, Element aElement,
                  XMLParser aParser, XMLParser.ParsingOptions aOptions) throws InvalidXMLException {
    if ("typePriority".equals(aPropXmlInfo.propertyName)) {
      // The mere presence of a <typePriority/> element in the XML indicates
      // that this property is true
      mTypePriority = true;
    } else if ("comparator".equals(aPropXmlInfo.propertyName)) {
      // This property has an interger-encoded value which is written to XML as
      // a more user-friendly string.
      boolean success = false;
      String comparatorStr = XMLUtils.getText(aElement);
      for (int i = 0; i < COMPARATOR_STRINGS.length; i++) {
        if (COMPARATOR_STRINGS[i].equals(comparatorStr)) {
          setComparator(i);
          success = true;
          break;
        }
      }
      if (!success) {
        throw new InvalidXMLException(InvalidXMLException.INVALID_ELEMENT_TEXT, new Object[] {
            comparatorStr, "comparator" });
      }
    } else {
      // for all other attributes, use the default superclass behavior
      super.readPropertyValueFromXMLElement(aPropXmlInfo, aElement, aParser, aOptions);
    }
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("fsIndexKey",
                  new PropertyXmlInfo[] { new PropertyXmlInfo("typePriority"), // NOTE: custom
                                                                                // XMLization
                      new PropertyXmlInfo("featureName"), new PropertyXmlInfo("comparator") // NOTE:
                                                                                            // custom
                                                                                            // XMLization
                  });

}
