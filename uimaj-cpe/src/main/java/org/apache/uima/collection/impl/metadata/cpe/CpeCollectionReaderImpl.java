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

/**
 * The Class CpeCollectionReaderImpl.
 */
public class CpeCollectionReaderImpl extends MetaDataObject_impl implements CpeCollectionReader {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -7663775553359776495L;

  /** The collection iterator. */
  private CpeCollectionReaderIterator collectionIterator;

  /** The cas initializer. */
  private CpeCollectionReaderCasInitializer casInitializer;

  /**
   * Instantiates a new cpe collection reader impl.
   */
  public CpeCollectionReaderImpl() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCollectionReader#setCasInitializer(org.apache.uima.
   * collection.metadata.CpeCollectionReaderCasInitializer)
   */
  @Override
  public void setCasInitializer(CpeCollectionReaderCasInitializer aCasInitializer)
          throws CpeDescriptorException {
    casInitializer = aCasInitializer;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCollectionReader#getCasInitializer()
   */
  @Override
  public CpeCollectionReaderCasInitializer getCasInitializer() throws CpeDescriptorException {
    return casInitializer;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCollectionReader#removeCasInitializer()
   */
  @Override
  public void removeCasInitializer() {
    casInitializer = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.collection.metadata.CpeCollectionReaderCasInitializer#setDescriptorPath(java.
   * lang.String)
   */
  @Override
  public void setDescriptor(CpeComponentDescriptor aDescriptor) {
    collectionIterator.setDescriptor(aDescriptor);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCollectionReaderCasInitializer#getDescriptorPath()
   */
  @Override
  public CpeComponentDescriptor getDescriptor() {
    return collectionIterator.getDescriptor();
  }

  /**
   * Returns configuration parameter settings for this CollectionReader.
   *
   * @return the configuration parameter settings
   */
  @Override
  public CasProcessorConfigurationParameterSettings getConfigurationParameterSettings() {
    return collectionIterator.getConfigurationParameterSettings();
  }

  /**
   * Sets configuration parameter settings for this CollectionReader.
   *
   * @param aParams
   *          the new configuration parameter settings
   * @throws CpeDescriptorException
   *           the cpe descriptor exception
   */
  @Override
  public void setConfigurationParameterSettings(CasProcessorConfigurationParameterSettings aParams)
          throws CpeDescriptorException {
    collectionIterator.setConfigurationParameterSettings(aParams);
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
  private static final XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("collectionReader",
          new PropertyXmlInfo[] { new PropertyXmlInfo("collectionIterator", null),
              new PropertyXmlInfo("casInitializer", null), });

  /**
   * Sets the collection iterator.
   *
   * @param iterator
   *          the new collection iterator
   */
  @Override
  public void setCollectionIterator(CpeCollectionReaderIterator iterator) {
    collectionIterator = iterator;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCollectionReader#getCollectionIterator()
   */
  @Override
  public CpeCollectionReaderIterator getCollectionIterator() {
    return collectionIterator;
  }

}
