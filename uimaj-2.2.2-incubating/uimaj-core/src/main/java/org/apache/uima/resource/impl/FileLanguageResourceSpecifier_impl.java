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

package org.apache.uima.resource.impl;

import org.apache.uima.resource.FileLanguageResourceSpecifier;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

/**
 * Reference implementation of {@link org.apache.uima.resource.FileLanguageResourceSpecifier}.
 * 
 * 
 */
public class FileLanguageResourceSpecifier_impl extends MetaDataObject_impl implements
        FileLanguageResourceSpecifier {

  static final long serialVersionUID = 4660680936104675527L;

  /** URL prefix for the file. */
  private String mFileUrlPrefix;

  /** URL suffix for the file. */
  private String mFileUrlSuffix;

  /**
   * Creates a new <code>FileLanguageResourceSpecifier_impl</code>.
   */
  public FileLanguageResourceSpecifier_impl() {
  }

  /**
   * @see org.apache.uima.resource.FileLanguageResourceSpecifier#getFileUrlPrefix()
   */
  public String getFileUrlPrefix() {
    return mFileUrlPrefix;
  }

  /**
   * @see org.apache.uima.resource.FileLanguageResourceSpecifier#getFileUrlSuffix()
   */
  public String getFileUrlSuffix() {
    return mFileUrlSuffix;
  }

  /**
   * @see org.apache.uima.resource.FileLanguageResourceSpecifier#setFileUrlPrefix(java.lang.String)
   */
  public void setFileUrlPrefix(String aPrefix) {
    mFileUrlPrefix = aPrefix;
  }

  /**
   * @see org.apache.uima.resource.FileLanguageResourceSpecifier#setFileUrlSuffix(java.lang.String)
   */
  public void setFileUrlSuffix(String aSuffix) {
    mFileUrlSuffix = aSuffix;
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo(
          "fileLanguageResourceSpecifier", new PropertyXmlInfo[] {
              new PropertyXmlInfo("fileUrlPrefix"), new PropertyXmlInfo("fileUrlSuffix"), });
}
