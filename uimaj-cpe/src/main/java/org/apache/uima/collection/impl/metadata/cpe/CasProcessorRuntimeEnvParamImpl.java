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

import org.apache.uima.collection.metadata.CasProcessorRuntimeEnvParam;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;
import org.w3c.dom.Element;
import org.xml.sax.helpers.AttributesImpl;

/**
 * The Class CasProcessorRuntimeEnvParamImpl.
 */
public class CasProcessorRuntimeEnvParamImpl extends MetaDataObject_impl
        implements CasProcessorRuntimeEnvParam {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -6750487360818463790L;

  /** The key. */
  private String key;

  /** The value. */
  private String value;

  /**
   * Instantiates a new cas processor runtime env param impl.
   */
  public CasProcessorRuntimeEnvParamImpl() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorRuntimeEnvParam#setEnvParamName(java.lang.
   * String)
   */
  @Override
  public void setEnvParamName(String aEnvParamName) throws CpeDescriptorException {
    key = aEnvParamName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorRuntimeEnvParam#getEnvParamName()
   */
  @Override
  public String getEnvParamName() throws CpeDescriptorException {
    return key;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.collection.metadata.CasProcessorRuntimeEnvParam#setEnvParamValue(java.lang.
   * String)
   */
  @Override
  public void setEnvParamValue(String aEnvParamValue) throws CpeDescriptorException {
    value = aEnvParamValue;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorRuntimeEnvParam#getEnvParamValue()
   */
  @Override
  public String getEnvParamValue() throws CpeDescriptorException {
    return value;
  }

  /**
   * Overridden to read "key" and "value" attributes.
   *
   * @param aElement
   *          the a element
   * @param aParser
   *          the a parser
   * @param aOptions
   *          the a options
   * @throws InvalidXMLException
   *           the invalid XML exception
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser, org.apache.uima.util.XMLParser.ParsingOptions)
   */
  @Override
  public void buildFromXMLElement(Element aElement, XMLParser aParser, ParsingOptions aOptions)
          throws InvalidXMLException {
    setKey(aElement.getAttribute("key"));
    setValue(aElement.getAttribute("value"));

  }

  /**
   * Overridden to handle "key" and "value" attributes.
   *
   * @return the XML attributes
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXMLAttributes()
   */
  @Override
  protected AttributesImpl getXMLAttributes() {
    AttributesImpl attrs = super.getXMLAttributes();

    attrs.addAttribute("", "key", "key", "CDATA", getKey());
    attrs.addAttribute("", "value", "value", "CDATA", getValue());
    return attrs;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  /** The Constant XMLIZATION_INFO. */
  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("env",
          new PropertyXmlInfo[0]);

  /**
   * Gets the value.
   *
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * Sets the value.
   *
   * @param string
   *          the new value
   */
  public void setValue(String string) {
    value = string;
  }

  /**
   * Gets the key.
   *
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * Sets the key.
   *
   * @param string
   *          the new key
   */
  public void setKey(String string) {
    key = string;
  }

}
