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

import java.util.Collection;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;

/**
 * Reference implementation of {@link TypeSystemDescription}.
 */
public class TypeSystemDescription_impl extends MetaDataObject_impl
        implements TypeSystemDescription {

  static final long serialVersionUID = -3372766232454730201L;

  private String mName;

  private String mVersion;

  private String mDescription;

  private String mVendor;

  private Import[] mImports = Import.EMPTY_IMPORTS;

  /** Descriptions of all Types in this type system. */
  private TypeDescription[] mTypes = TypeDescription.EMPTY_TYPE_DESCRIPTIONS;

  /**
   * Creates a new TypeSystemDescription_impl.
   */
  public TypeSystemDescription_impl() {
  }

  @Override
  public String getName() {
    return mName;
  }

  @Override
  public void setName(String aName) {
    mName = aName;
  }

  @Override
  public String getVersion() {
    return mVersion;
  }

  @Override
  public void setVersion(String aVersion) {
    mVersion = aVersion;
  }

  @Override
  public String getDescription() {
    return mDescription;
  }

  @Override
  public void setDescription(String aDescription) {
    mDescription = aDescription;
  }

  @Override
  public String getVendor() {
    return mVendor;
  }

  @Override
  public void setVendor(String aVendor) {
    mVendor = aVendor;
  }

  @Override
  public Import[] getImports() {
    return mImports;
  }

  @Override
  public void setImports(Import... aImports) {
    if (aImports == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aImports", "setImports" });
    }
    mImports = aImports;
  }

  @Override
  public TypeDescription[] getTypes() {
    return mTypes;
  }

  @Override
  public void setTypes(TypeDescription... aTypes) {
    if (aTypes == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aTypes", "setTypes" });
    }
    mTypes = aTypes;
  }

  @Override
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

  @Override
  public TypeDescription getType(String aTypeName) {
    for (int i = 0; i < mTypes.length; i++) {
      if (aTypeName.equals(mTypes[i].getName())) {
        return mTypes[i];
      }
    }
    return null;
  }

  // allow these calls to be done multiple times on this same object, in different threads
  @Override
  public synchronized void resolveImports() throws InvalidXMLException {
    resolveImports(null, UIMAFramework.newDefaultResourceManager());
  }

  @Override
  public synchronized void resolveImports(ResourceManager aResourceManager)
          throws InvalidXMLException {
    resolveImports(null, aResourceManager);
  }

  @Deprecated
  @Override
  public synchronized void resolveImports(Collection<String> aAlreadyImportedTypeSystemURLs,
          ResourceManager aResourceManager) throws InvalidXMLException {
    ImportResolver<TypeSystemDescription, TypeDescription> resolver = new ImportResolver<>(
            TypeSystemDescriptionImportResolverAdapter::new);
    resolver.resolveImports(this, aAlreadyImportedTypeSystemURLs, aResourceManager);
  }

  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("typeSystemDescription",
          new PropertyXmlInfo[] { //
              new PropertyXmlInfo("name", true), //
              new PropertyXmlInfo("description", true), //
              new PropertyXmlInfo("version", true), //
              new PropertyXmlInfo("vendor", true), //
              new PropertyXmlInfo("imports", true), //
              new PropertyXmlInfo("types", true) //
          });
}
