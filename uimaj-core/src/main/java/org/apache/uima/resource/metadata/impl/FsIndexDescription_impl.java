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

import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;

public class FsIndexDescription_impl extends MetaDataObject_impl implements FsIndexDescription {

  static final long serialVersionUID = 8939000196947456114L;

  static final FsIndexKeyDescription[] EMPTY_FS_INDEX_KEY_DESCRIPTION_ARRAY = new FsIndexKeyDescription[0];

  private String mLabel;

  private String mTypeName;

  private String mKind;

  private FsIndexKeyDescription[] mKeys = EMPTY_FS_INDEX_KEY_DESCRIPTION_ARRAY;

  /* Doesn't override a super method */
  protected String getXMLElementTagName() {
    return "fsIndexDescription";
  }

  @Override
  public String getLabel() {
    return mLabel;
  }

  @Override
  public void setLabel(String aLabel) {
    mLabel = aLabel;
  }

  @Override
  public String getTypeName() {
    return mTypeName;
  }

  @Override
  public void setTypeName(String aTypeName) {
    mTypeName = aTypeName;
  }

  @Override
  public String getKind() {
    return mKind;
  }

  @Override
  public void setKind(String aKind) {
    mKind = aKind;
  }

  @Override
  public FsIndexKeyDescription[] getKeys() {
    return mKeys;
  }

  @Override
  public void setKeys(FsIndexKeyDescription... aKeys) {
    if (aKeys == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aKeys", "setKeys" });
    }
    mKeys = aKeys;
  }

  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  private static final XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("fsIndexDescription",
          new PropertyXmlInfo[] { new PropertyXmlInfo("label"), new PropertyXmlInfo("typeName"),
              new PropertyXmlInfo("kind"), new PropertyXmlInfo("keys", true)

          });
}
