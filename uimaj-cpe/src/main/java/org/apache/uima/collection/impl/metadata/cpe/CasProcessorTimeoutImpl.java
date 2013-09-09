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

package org.apache.uima.collection.impl.metadata.cpe;

import org.apache.uima.collection.metadata.CasProcessorTimeout;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;
import org.w3c.dom.Element;
import org.xml.sax.helpers.AttributesImpl;

public class CasProcessorTimeoutImpl extends MetaDataObject_impl implements CasProcessorTimeout {
  private static final long serialVersionUID = -8276573951395652039L;

  private String defaultTimeout = "-1";

  private String max;

  public CasProcessorTimeoutImpl() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorTimeout#set(int)
   */
  public void set(int aFrequency) {
    max = String.valueOf(aFrequency);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorTimeout#get()
   */
  public int get() {
    return Integer.parseInt(max);
  }

  /**
   * Overridden to read "max" and "default" attributes.
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser, org.apache.uima.util.XMLParser.ParsingOptions)
   */
  public void buildFromXMLElement(Element aElement, XMLParser aParser, ParsingOptions aOptions)
          throws InvalidXMLException {
    setMax(aElement.getAttribute("max"));
    setDefaultTimeout(aElement.getAttribute("default"));
  }

  /**
   * Overridden to handle "max" and "default" attributes.
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXMLAttributes()
   */
  protected AttributesImpl getXMLAttributes() {
    AttributesImpl attrs = super.getXMLAttributes();
    attrs.addAttribute("", "max", "max", "CDATA", getMax());
    if (getDefaultTimeout() != null && getDefaultTimeout().trim().length() > 0) {
      attrs.addAttribute("", "default", "default", "CDATA", getDefaultTimeout());
    }
    return attrs;
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("timeout",
          new PropertyXmlInfo[0]);

  /**
   * @return the XMLization info
   */
  public static XmlizationInfo getXMLIZATION_INFO() {
    return XMLIZATION_INFO;
  }

  /** PROTECTED METHODS USED BY THE PARSER */
  /**
   * @return the max
   */
  public String getMax() {
    return max;
  }

  /**
   * @return the default timeout
   */
  public String getDefaultTimeout() {
    return defaultTimeout;
  }

  /**
   * @param string
   */
  public void setDefaultTimeout(String string) {
    defaultTimeout = string;
  }

  /**
   * @param string
   */
  public void setMax(String string) {
    max = string;
  }

}
