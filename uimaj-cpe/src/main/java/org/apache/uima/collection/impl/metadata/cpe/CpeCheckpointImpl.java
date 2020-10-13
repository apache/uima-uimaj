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

import org.apache.uima.collection.metadata.CpeCheckpoint;
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
 * The Class CpeCheckpointImpl.
 */
public class CpeCheckpointImpl extends MetaDataObject_impl implements CpeCheckpoint {
  
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 9155094513948815121L;

  /** The file. */
  private String file;

  /** The time. */
  private String time;

  /** The batch. */
  private int batch;

  /**
   * Instantiates a new cpe checkpoint impl.
   */
  public CpeCheckpointImpl() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCheckpoint#setFilePath(java.lang.String)
   */
  @Override
  public void setFilePath(String aCheckpointFilePath) throws CpeDescriptorException {
    file = aCheckpointFilePath;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCheckpoint#getFilePath()
   */
  @Override
  public String getFilePath() {
    return file;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCheckpoint#setFrequency(int, boolean)
   */
  @Override
  public void setFrequency(int aFrequency, boolean aTimeBased) {
    time = String.valueOf(aFrequency) + "ms";
  }

  /**
   * Convert 2 number.
   *
   * @param anObject the an object
   * @return the int
   */
  private int convert2Number(Object anObject) {
    int convertedTime = 1;

    if (anObject != null && anObject instanceof String) {
      int len = ((String) anObject).length();
      int i = 0;
      for (; i < len; i++) {
        if (Character.isDigit(((String) anObject).charAt(i))) {
          continue;
        }
        else {
          break; // non-digit char
        }
      }
      if (i > 0) {
        convertedTime = Integer.parseInt(((String) anObject).substring(0, i));
      }
    }
    return convertedTime;

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCheckpoint#getFrequency()
   */
  @Override
  public int getFrequency() {
    return convert2Number(time);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCheckpoint#isTimeBased()
   */
  @Override
  public boolean isTimeBased() {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCheckpoint#setBatchSize(int)
   */
  @Override
  public void setBatchSize(int aBatchSize) {
    batch = aBatchSize;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCheckpoint#getBatchSize()
   */
  @Override
  public int getBatchSize() {
    return batch;
  }

  /**
   * Overridden to read Checkpoint attributes.
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
    setFile(aElement.getAttribute("file"));
    String batch = aElement.getAttribute("batch");
    if (batch != null && batch.trim().length() > 0) {
      setBatch(Integer.parseInt(batch));
    }
    setTime(aElement.getAttribute("time"));

  }

  /**
   * Overridden to handle Checkpoint attributes.
   *
   * @return the XML attributes
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXMLAttributes()
   */
  @Override
  protected AttributesImpl getXMLAttributes() {
    AttributesImpl attrs = super.getXMLAttributes();
    attrs.addAttribute("", "batch", "batch", "CDATA", String.valueOf(getBatch()));
    if (getFile() != null && getFile().trim().length() > 0) {
      attrs.addAttribute("", "file", "file", "CDATA", getFile());
    }
    if (getTime() != null && getTime().trim().length() > 0) {
      attrs.addAttribute("", "time", "time", "CDATA", getTime());
    }
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
  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("checkpoint",
          new PropertyXmlInfo[0]);

  // METHODS CALLED BY THE PARSER
  
  /**
   * @return the batch size
   */
  public int getBatch() {
    return batch;
  }

  /**
   * Gets the file.
   *
   * @return a file
   */
  public String getFile() {
    return file;
  }

  /**
   * Gets the time.
   *
   * @return a time
   */
  public String getTime() {
    return time;
  }

  /**
   * Sets the batch.
   *
   * @param i the new batch
   */
  public void setBatch(int i) {
    batch = i;
  }

  /**
   * Sets the file.
   *
   * @param string the new file
   */
  public void setFile(String string) {
    file = string;
  }

  /**
   * Sets the time.
   *
   * @param i the new time
   */
  public void setTime(String i) {
    time = i;
  }
}
