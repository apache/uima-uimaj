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

import org.apache.uima.collection.metadata.OutputQueue;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;
import org.w3c.dom.Element;
import org.xml.sax.helpers.AttributesImpl;


/**
 * The Class OutputQueue_impl.
 */
public class OutputQueue_impl extends MetaDataObject_impl implements OutputQueue {
  
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -3001016349004832820L;

  /** The dequeue timeout. */
  private int dequeueTimeout;

  /** The queue class. */
  private String queueClass;

  /**
   * Overridden to read "queueClass" and "dequeueTimeout" attributes.
   *
   * @param aElement the a element
   * @param aParser the a parser
   * @param aOptions the a options
   * @throws InvalidXMLException the invalid XML exception
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser, org.apache.uima.util.XMLParser.ParsingOptions)
   */
  @Override
  public void buildFromXMLElement(Element aElement, XMLParser aParser, ParsingOptions aOptions)
          throws InvalidXMLException {
    setQueueClass(aElement.getAttribute("queueClass"));
    String timeout = aElement.getAttribute("dequeueTimeout");
    if (timeout != null && timeout.trim().length() > 0) {
      setDequeueTimeout(Integer.parseInt(timeout));
    }

  }

  /**
   * Overridden to handle "queueClass" and "dequeueTimeout" attributes.
   *
   * @return the XML attributes
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXMLAttributes()
   */
  @Override
  protected AttributesImpl getXMLAttributes() {
    AttributesImpl attrs = super.getXMLAttributes();
    attrs.addAttribute("", "dequeueTimeout", "dequeueTimeout", "CDATA", String
            .valueOf(getDequeueTimeout()));
    attrs.addAttribute("", "queueClass", "queueClass", "CDATA", String.valueOf(getQueueClass()));
    return attrs;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  /** The Constant XMLIZATION_INFO. */
  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("outputQueue",
          new PropertyXmlInfo[0]);

  /**
   * Gets the dequeue timeout.
   *
   * @return dequeue timeout
   */
  @Override
  public int getDequeueTimeout() {
    return dequeueTimeout;
  }

  /**
   * Gets the queue class.
   *
   * @return queue class
   */
  @Override
  public String getQueueClass() {
    return queueClass;
  }

  /**
   * Sets the dequeue timeout.
   *
   * @param i the new dequeue timeout
   */
  @Override
  public void setDequeueTimeout(int i) {
    dequeueTimeout = i;
  }

  /**
   * Sets the queue class.
   *
   * @param string the new queue class
   */
  @Override
  public void setQueueClass(String string) {
    queueClass = string;
  }

}
