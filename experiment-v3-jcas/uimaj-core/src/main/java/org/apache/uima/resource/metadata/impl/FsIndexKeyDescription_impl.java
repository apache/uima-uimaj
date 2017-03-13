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
import org.w3c.dom.Node;
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
   * @see FsIndexKeyDescription#isTypePriority()
   */
  public boolean isTypePriority() {
    return mTypePriority;
  }

  /**
   * @see FsIndexKeyDescription#setTypePriority(boolean)
   */
  public void setTypePriority(boolean aTypePriority) {
    mTypePriority = aTypePriority;
  }

  /**
   * @see FsIndexKeyDescription#getFeatureName()
   */
  public String getFeatureName() {
    return mFeatureName;
  }

  /**
   * @see FsIndexKeyDescription#setFeatureName(String)
   */
  public void setFeatureName(String aName) {
    mFeatureName = aName;
  }

  /**
   * @see FsIndexKeyDescription#getComparator()
   */
  public int getComparator() {
    return mComparator;
  }

  /**
   * @see FsIndexKeyDescription#setComparator(int)
   */
  public void setComparator(int aComparator) {
    mComparator = aComparator;
  }

  /**
   * Overridden to handle XML export of the <code>typePriority</code> and <code>comparator</code>
   * properties.
   * 
   * @see MetaDataObject_impl#writePropertyAsElement(PropertyXmlInfo, String)
   */
  @Override
  protected void writePropertyAsElement(PropertyXmlInfo aPropInfo, String aNamespace) throws SAXException {
    final SerialContext sc = serialContext.get();
    final Serializer serializer = sc.serializer;

    String namespace = getXmlizationInfo().namespace;
    Node node = serializer.findMatchingSubElement("type");
    if ("typePriority".equals(aPropInfo.propertyName)) {
      // if property is true, just write an empty tag, if false omit
      if (isTypePriority()) {
        serializer.outputStartElement(node, namespace, "typePriority", "typePriority", new AttributesImpl());
//        aContentHandler.startElement(getXmlizationInfo().namespace, "typePriority", "typePriority",
//                new AttributesImpl());
        serializer.outputEndElement(node, namespace, "typePriority", "typePriority");
//        aContentHandler.endElement(getXmlizationInfo().namespace, "typePriority", "typePriority");
      }
    } else if (!isTypePriority()) // don't write other properties for a type priority key
    {
      if ("comparator".equals(aPropInfo.propertyName)) {
        // This property has an interger-encoded value which is written to XML
        // as a more user-friendly string.

        serializer.outputStartElement(node, namespace, "comparator", "comparator", new AttributesImpl());
//        aContentHandler.startElement(getXmlizationInfo().namespace, "comparator", "comparator",
//                new AttributesImpl());

        // write value as string
        String str = COMPARATOR_STRINGS[getComparator()];
        serializer.writeSimpleValue(str);
        serializer.outputEndElement(node, namespace, "comparator", "comparator");
//        aContentHandler.endElement(getXmlizationInfo().namespace, "comparator", "comparator");
      } else {
        // for all other attributes, use the default superclass behavior
        super.writePropertyAsElement(aPropInfo, aNamespace);
      }
    }
  }

  /**
   * Overridden to handle XML import of the <code>typePriority</code> and <code>comparator</code>
   * properties.
   * 
   * @see MetaDataObject_impl#readPropertyValueFromXMLElement(PropertyXmlInfo, Element, XMLParser, org.apache.uima.util.XMLParser.ParsingOptions)
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
