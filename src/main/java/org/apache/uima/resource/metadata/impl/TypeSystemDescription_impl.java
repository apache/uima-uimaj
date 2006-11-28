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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

/**
 * Reference implementation of {@link TypeSystemDescription}.
 * 
 * 
 */
public class TypeSystemDescription_impl extends MetaDataObject_impl implements
        TypeSystemDescription {

  static final long serialVersionUID = -3372766232454730201L;

  private String mName;

  private String mVersion;

  private String mDescription;

  private String mVendor;

  private Import[] mImports = new Import[0];

  /** Descriptions of all Types in this type system. */
  private TypeDescription[] mTypes = new TypeDescription[0];

  /**
   * Creates a new TypeSystemDescription_impl.
   */
  public TypeSystemDescription_impl() {
  }

  /**
   * @see org.apache.uima.resource.ResourceMetaData#getName()
   */
  public String getName() {
    return mName;
  }

  /**
   * @see org.apache.uima.resource.ResourceMetaData#setName(String)
   */
  public void setName(String aName) {
    mName = aName;
  }

  /**
   * @see org.apache.uima.resource.ResourceMetaData#getVersion()
   */
  public String getVersion() {
    return mVersion;
  }

  /**
   * @see org.apache.uima.resource.ResourceMetaData#setVersion(String)
   */
  public void setVersion(String aVersion) {
    mVersion = aVersion;
  }

  /**
   * @see org.apache.uima.resource.ResourceMetaData#getDescription()
   */
  public String getDescription() {
    return mDescription;
  }

  /**
   * @see org.apache.uima.resource.ResourceMetaData#setDescription(String)
   */
  public void setDescription(String aDescription) {
    mDescription = aDescription;
  }

  /**
   * @see org.apache.uima.resource.ResourceMetaData#getVendor()
   */
  public String getVendor() {
    return mVendor;
  }

  /**
   * @see org.apache.uima.resource.ResourceMetaData#setVendor(String)
   */
  public void setVendor(String aVendor) {
    mVendor = aVendor;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.TypeSystemDescription#getImports()
   */
  public Import[] getImports() {
    return mImports;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.TypeSystemDescription#setImports(org.apache.uima.resource.metadata.Import[])
   */
  public void setImports(Import[] aImports) {
    if (aImports == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aImports", "setImports" });
    }
    mImports = aImports;
  }

  /**
   * @see org.apache.uima.cas.TypeSystemDescription#getTypes()
   */
  public TypeDescription[] getTypes() {
    return mTypes;
  }

  /**
   * @see org.apache.uima.cas.TypeSystemDescription#setTypes(TypeDescription[])
   */
  public void setTypes(TypeDescription[] aTypes) {
    if (aTypes == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aTypes", "setTypes" });
    }
    mTypes = aTypes;
  }

  /**
   * @see org.apache.uima.cas.TypeSystemDescription#addType(String, String, String)
   */
  public TypeDescription addType(String aTypeName, String aDescription, String aSupertypeName) {
    // create new type description
    TypeDescription newType = new TypeDescription_impl(aTypeName, aDescription, aSupertypeName);

    // add to array
    TypeDescription[] types = getTypes();
    if (types == null) {
      setTypes(new TypeDescription[] { newType });
    } else {
      TypeDescription[] newArray = new TypeDescription[types.length + 1];
      System.arraycopy(types, 0, newArray, 0, types.length);
      newArray[types.length] = newType;
      setTypes(newArray);
    }

    return newType;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.TypeSystemDescription#getType(java.lang.String)
   */
  public TypeDescription getType(String aTypeName) {
    for (int i = 0; i < mTypes.length; i++) {
      if (aTypeName.equals(mTypes[i].getName()))
        return mTypes[i];
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.TypeSystemDescription#resolveImports()
   */
  public void resolveImports() throws InvalidXMLException {
    resolveImports(new TreeSet(), UIMAFramework.newDefaultResourceManager());
  }

  public void resolveImports(ResourceManager aResourceManager) throws InvalidXMLException {
    resolveImports(new TreeSet(), aResourceManager);
  }

  public void resolveImports(Collection aAlreadyImportedTypeSystemURLs,
          ResourceManager aResourceManager) throws InvalidXMLException {
    List importedTypes = new ArrayList();
    Import[] imports = getImports();
    for (int i = 0; i < imports.length; i++) {
      // make sure Import's relative path base is set, to allow for users who create
      // new import objects
      if (imports[i] instanceof Import_impl) {
        ((Import_impl) imports[i]).setSourceUrlIfNull(this.getSourceUrl());
      }
      URL url = imports[i].findAbsoluteUrl(aResourceManager);
      if (!aAlreadyImportedTypeSystemURLs.contains(url.toString())) {
        aAlreadyImportedTypeSystemURLs.add(url.toString());
        try {
          resolveImport(url, aAlreadyImportedTypeSystemURLs, importedTypes, aResourceManager);
        } catch (IOException e) {
          throw new InvalidXMLException(InvalidXMLException.IMPORT_FAILED_COULD_NOT_READ_FROM_URL,
                  new Object[] { url, imports[i].getSourceUrlString() }, e);
        }
      }
    }
    // update this object
    TypeDescription[] existingTypes = this.getTypes();
    if (existingTypes == null) {
      existingTypes = new TypeDescription[0];
    }
    TypeDescription[] newTypes = new TypeDescription[existingTypes.length + importedTypes.size()];
    System.arraycopy(existingTypes, 0, newTypes, 0, existingTypes.length);
    for (int i = 0; i < importedTypes.size(); i++) {
      newTypes[existingTypes.length + i] = (TypeDescription) importedTypes.get(i);
    }
    this.setTypes(newTypes);

    // clear imports
    this.setImports(new Import[0]);
  }

  private void resolveImport(URL aURL, Collection aAlreadyImportedTypeSystemURLs,
          Collection aResults, ResourceManager aResourceManager) throws InvalidXMLException,
          IOException {
    XMLInputSource input;
    input = new XMLInputSource(aURL);
    TypeSystemDescription desc = UIMAFramework.getXMLParser().parseTypeSystemDescription(input);
    desc.resolveImports(aAlreadyImportedTypeSystemURLs, aResourceManager);
    aResults.addAll(Arrays.asList(desc.getTypes()));
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("typeSystemDescription",
          new PropertyXmlInfo[] { new PropertyXmlInfo("name", true),
              new PropertyXmlInfo("description", true), new PropertyXmlInfo("version", true),
              new PropertyXmlInfo("vendor", true), new PropertyXmlInfo("imports", true),
              new PropertyXmlInfo("types", true) });
}
