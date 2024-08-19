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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.util.InvalidXMLException;
import org.xml.sax.SAXException;

/**
 * Reference implementation of {@link TypePriorities}.
 */
public class TypePriorities_impl extends MetaDataObject_impl implements TypePriorities {

  private static final long serialVersionUID = -4773863151055424438L;

  private volatile String mName;

  private volatile String mVersion;

  private volatile String mDescription;

  private volatile String mVendor;

  private volatile Import[] mImports = Import.EMPTY_IMPORTS;

  // not final or volatile because clone() copies a ref to shared value, and we need that value to
  // be a new instance
  // Threading: all access synchronized except initial creation during cloning
  private List<TypePriorityList> mPriorityLists = new ArrayList<>();

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
  public TypePriorityList[] getPriorityLists() {
    // synchronized to prevent concurrent mod exceptions
    synchronized (mPriorityLists) { // saw concurrent mod exception 3/2014
      TypePriorityList[] result = new TypePriorityList[mPriorityLists.size()];
      mPriorityLists.toArray(result);
      return result;
    }
  }

  @Override
  public void setPriorityLists(TypePriorityList... aPriorityLists) {
    // could be called by thread doing resolve imports, while another thread was iterating over them
    synchronized (mPriorityLists) { // saw concurrent mod exceptions 3/2014
      mPriorityLists.clear();
      for (int i = 0; i < aPriorityLists.length; i++) {
        mPriorityLists.add(aPriorityLists[i]);
      }
    }
  }

  @Override
  public void addPriorityList(TypePriorityList aPriorityList) {
    synchronized (mPriorityLists) { // saw concurrent mod exceptions 3/2014
      mPriorityLists.add(aPriorityList);
    }
  }

  @Override
  public TypePriorityList addPriorityList() {
    TypePriorityList newPriorityList = new TypePriorityList_impl();
    synchronized (mPriorityLists) { // saw concurrent mod exceptions while iterating on this 3/2014
      mPriorityLists.add(newPriorityList);
    }
    return newPriorityList;
  }

  @Override
  public void removePriorityList(TypePriorityList aPriorityList) {
    synchronized (mPriorityLists) { // saw concurrent mod exceptions while iterating on this 3/2014
      mPriorityLists.remove(aPriorityList);
    }
  }

  // support multithreading,
  // avoid object creation if already resolved
  @Override
  public synchronized void resolveImports() throws InvalidXMLException {
    resolveImports(null, null);
  }

  @Override
  public synchronized void resolveImports(ResourceManager aResourceManager)
          throws InvalidXMLException {
    resolveImports(null, aResourceManager);
  }

  @Deprecated
  @Override
  public synchronized void resolveImports(Collection<String> aAlreadyImportedTypePrioritiesURLs,
          ResourceManager aResourceManager) throws InvalidXMLException {
    ImportResolver<TypePriorities, TypePriorityList> resolver = new ImportResolver<>(
            TypePrioritiesImportResolverAdapter::new);
    resolver.resolveImports(this, aAlreadyImportedTypePrioritiesURLs, aResourceManager);
  }

  /**
   * Overridden to suppress &lt;priorityLists&gt; tag for TAF compatibility
   * 
   * @see MetaDataObject_impl#writePropertyAsElement(org.apache.uima.resource.metadata.impl.PropertyXmlInfo,
   *      java.lang.String)
   */
  @Override
  protected void writePropertyAsElement(PropertyXmlInfo aPropInfo, String aNamespace)
          throws SAXException {
    if ("priorityLists".equals(aPropInfo.propertyName)) {
      // call writeArrayPropertyAsElement directly, which will not generate the
      // <priorityLists> tag
      writeArrayPropertyAsElement(aPropInfo.propertyName, TypePriorityList[].class,
              getPriorityLists(), null, aNamespace);
    } else // normal handling
    {
      super.writePropertyAsElement(aPropInfo, aNamespace);
    }
  }

  /*
   * (non-Javadoc) Special purpose clone method to deal with ArrayList.
   */
  @Override
  public Object clone() {
    TypePriorities_impl clone = (TypePriorities_impl) super.clone();
    clone.mPriorityLists = new ArrayList<>();
    final List<TypePriorityList> origPriorityLists = mPriorityLists;
    // saw concurrent mod exceptions while iterating on this 3/2014
    synchronized (origPriorityLists) {
      for (TypePriorityList priList : origPriorityLists) {
        clone.addPriorityList((TypePriorityList) priList.clone());
      }
    }
    return clone;
  }

  /**
   * @see MetaDataObject_impl#getXmlizationInfo()
   */
  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return new XmlizationInfo("typePriorities", new PropertyXmlInfo[] { //
        new PropertyXmlInfo("name", true), //
        new PropertyXmlInfo("description", true), //
        new PropertyXmlInfo("version", true), //
        new PropertyXmlInfo("vendor", true), //
        new PropertyXmlInfo("imports", true), //
        new PropertyXmlInfo("priorityLists", true) //
    });
  }
}
