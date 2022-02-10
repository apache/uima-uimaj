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

import org.apache.uima.collection.metadata.CasProcessorErrorRateThreshold;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;
import org.w3c.dom.Element;
import org.xml.sax.helpers.AttributesImpl;

/**
 * The Class CasProcessorErrorRateThresholdImpl.
 */
public class CasProcessorErrorRateThresholdImpl extends MetaDataObject_impl
        implements CasProcessorErrorRateThreshold {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -9214395691914383261L;

  /** The value. */
  private String value;

  /** The action. */
  private String action;

  /**
   * Instantiates a new cas processor error rate threshold impl.
   */
  public CasProcessorErrorRateThresholdImpl() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorErrorRateThreshold#setMaxErrorCount(int)
   */
  @Override
  public void setMaxErrorCount(int aErrorCount) {
    int sampleSize;
    try {
      sampleSize = getMaxErrorSampleSize();
    } catch (NumberFormatException e) {
      sampleSize = 1; // default
    }

    setValue(String.valueOf(aErrorCount) + "/" + String.valueOf(sampleSize));

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorErrorRateThreshold#getMaxErrorCount()
   */
  @Override
  public int getMaxErrorCount() {
    String errorCount;

    if ((errorCount = getValue()) == null) {
      return 1;
    }

    int pos = 0;
    if ((pos = errorCount.indexOf("/")) > -1) {
      errorCount = errorCount.trim().substring(0, pos);
    }
    return Integer.parseInt(errorCount);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.collection.metadata.CasProcessorErrorRateThreshold#setMaxErrorSampleSize(int)
   */
  @Override
  public void setMaxErrorSampleSize(int aSampleSize) {
    int errorCount;
    try {
      errorCount = getMaxErrorCount();
    } catch (NumberFormatException e) {
      errorCount = 1; // default
    }

    setValue(String.valueOf(errorCount) + "/" + String.valueOf(aSampleSize));

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorErrorRateThreshold#getMaxErrorSampleSize()
   */
  @Override
  public int getMaxErrorSampleSize() {
    String errorSample = getValue();
    if (errorSample == null) {
      return 1; // default
    }
    int pos = 0;
    if ((pos = errorSample.indexOf("/")) > -1) {
      errorSample = errorSample.trim().substring(pos + 1);
    }
    return Integer.parseInt(errorSample);

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.collection.metadata.CasProcessorErrorRateThreshold#setAction(java.lang.String)
   */
  @Override
  public void setAction(String aAction) {
    action = aAction;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorErrorRateThreshold#getAction()
   */
  @Override
  public String getAction() {
    return action;
  }

  /**
   * Overridden to read "name" and "value" attributes.
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
    setAction(aElement.getAttribute("action"));
    setValue(aElement.getAttribute("value"));
  }

  /**
   * Overridden to handle "name" and "value" attributes.
   *
   * @return the XML attributes
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXMLAttributes()
   */
  @Override
  protected AttributesImpl getXMLAttributes() {
    AttributesImpl attrs = super.getXMLAttributes();
    attrs.addAttribute("", "action", "action", "CDATA", String.valueOf(getAction()));
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
  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("errorRateThreshold",
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

}
