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

import org.apache.uima.collection.metadata.CasProcessorConfigurationParameterSettings;
import org.apache.uima.collection.metadata.CpeCollectionReader;
import org.apache.uima.collection.metadata.CpeCollectionReaderCasInitializer;
import org.apache.uima.collection.metadata.CpeCollectionReaderIterator;
import org.apache.uima.collection.metadata.CpeComponentDescriptor;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

public class CpeCollectionReaderImpl extends MetaDataObject_impl implements CpeCollectionReader {
  private static final long serialVersionUID = -7663775553359776495L;

  private CpeCollectionReaderIterator collectionIterator;

  private CpeCollectionReaderCasInitializer casInitializer;

  public CpeCollectionReaderImpl() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCollectionReader#setCasInitializer(org.apache.uima.collection.metadata.CpeCollectionReaderCasInitializer)
   */
  public void setCasInitializer(CpeCollectionReaderCasInitializer aCasInitializer)
          throws CpeDescriptorException {
    casInitializer = aCasInitializer;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCollectionReader#getCasInitializer()
   */
  public CpeCollectionReaderCasInitializer getCasInitializer() throws CpeDescriptorException {
    return casInitializer;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCollectionReader#removeCasInitializer()
   */
  public void removeCasInitializer() {
    casInitializer = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCollectionReaderCasInitializer#setDescriptorPath(java.lang.String)
   */
  public void setDescriptor(CpeComponentDescriptor aDescriptor) {
    collectionIterator.setDescriptor(aDescriptor);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCollectionReaderCasInitializer#getDescriptorPath()
   */
  public CpeComponentDescriptor getDescriptor() {
    return collectionIterator.getDescriptor();
  }

  /**
   * Returns configuration parameter settings for this CollectionReader.
   */
  public CasProcessorConfigurationParameterSettings getConfigurationParameterSettings() {
    return collectionIterator.getConfigurationParameterSettings();
  }

  /**
   * Sets configuration parameter settings for this CollectionReader.
   */
  public void setConfigurationParameterSettings(CasProcessorConfigurationParameterSettings aParams)
          throws CpeDescriptorException {
    collectionIterator.setConfigurationParameterSettings(aParams);
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("collectionReader",
          new PropertyXmlInfo[] { new PropertyXmlInfo("collectionIterator", null),
              new PropertyXmlInfo("casInitializer", null), });

  /**
   * @param iterator
   */
  public void setCollectionIterator(CpeCollectionReaderIterator iterator) {
    collectionIterator = iterator;
  }

  public CpeCollectionReaderIterator getCollectionIterator() {
    return collectionIterator;
  }

}
