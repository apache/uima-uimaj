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

import org.apache.uima.collection.metadata.CasProcessorMaxRestarts;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;
import org.w3c.dom.Element;
import org.xml.sax.helpers.AttributesImpl;

/**
 * The Class CasProcessorMaxRestartsImpl.
 */
public class CasProcessorMaxRestartsImpl extends MetaDataObject_impl
        implements CasProcessorMaxRestarts {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 2863741219504239020L;

  /** The value. */
  private String value = "1";

  /** The action. */
  private String action;

  /** The wait time between retries. */
  private int waitTimeBetweenRetries;

  /**
   * Instantiates a new cas processor max restarts impl.
   */
  public CasProcessorMaxRestartsImpl() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorMaxRestarts#setRestartCount(int)
   */
  @Override
  public void setRestartCount(int aRestartCount) {
    value = String.valueOf(aRestartCount);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorMaxRestarts#getRestartCount()
   */
  @Override
  public int getRestartCount() {
    return Integer.parseInt(value);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorMaxRestarts#setAction(java.lang.String)
   */
  @Override
  public void setAction(String aAction) {
    action = aAction;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorMaxRestarts#getAction()
   */
  @Override
  public String getAction() {
    return action;
  }

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

    String waitTime = aElement.getAttribute("waitTimeBetweenRetries");
    if (waitTime != null && waitTime.trim().length() > 0) {
      try {
        setWaitTimeBetweenRetries(Integer.parseInt(waitTime));
      } catch (NumberFormatException e) {
        // ignore
      }
    }
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
    if (getWaitTimeBetweenRetries() != 0) {
      attrs.addAttribute("", "waitTimeBetweenRetries", "waitTimeBetweenRetries", "CDATA",
              String.valueOf(getWaitTimeBetweenRetries()));
    }
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
  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("maxConsecutiveRestarts",
          new PropertyXmlInfo[0]);

  /**
   * Gets the wait time between retries.
   *
   * @return the wait time between retries
   */
  @Override
  public int getWaitTimeBetweenRetries() {
    return waitTimeBetweenRetries;
  }

  /**
   * Sets the wait time between retries.
   *
   * @param i
   *          the new wait time between retries
   */
  @Override
  public void setWaitTimeBetweenRetries(int i) {
    waitTimeBetweenRetries = i;
  }

}
