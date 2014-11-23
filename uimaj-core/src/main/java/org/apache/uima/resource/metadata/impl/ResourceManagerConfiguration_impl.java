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
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.ExternalResourceBinding;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.xml.sax.SAXException;

/**
 * 
 * 
 */
public class ResourceManagerConfiguration_impl extends MetaDataObject_impl implements
        ResourceManagerConfiguration {
  private static final long serialVersionUID = -8326190554827990517L;

  private String mName;

  private String mVersion;

  private String mDescription;

  private String mVendor;

  private Import[] mImports = Import.EMPTY_IMPORTS;

  private ExternalResourceBinding[] mBindings = new ExternalResourceBinding[0];

  private ExternalResourceDescription[] mExternalResources = new ExternalResourceDescription[0];

  /**
   * @see org.apache.uima.resource.metadata.ResourceManagerConfiguration#getName()
   */
  public String getName() {
    return mName;
  }

  /**
   * @see org.apache.uima.resource.metadata.ResourceManagerConfiguration#setName(String)
   */
  public void setName(String aName) {
    mName = aName;
  }

  /**
   * @see org.apache.uima.resource.metadata.ResourceManagerConfiguration#getVersion()
   */
  public String getVersion() {
    return mVersion;
  }

  /**
   * @see org.apache.uima.resource.metadata.ResourceManagerConfiguration#setVersion(String)
   */
  public void setVersion(String aVersion) {
    mVersion = aVersion;
  }

  /**
   * @see org.apache.uima.resource.metadata.ResourceManagerConfiguration#getDescription()
   */
  public String getDescription() {
    return mDescription;
  }

  /**
   * @see org.apache.uima.resource.metadata.ResourceManagerConfiguration#setDescription(String)
   */
  public void setDescription(String aDescription) {
    mDescription = aDescription;
  }

  /**
   * @see org.apache.uima.resource.metadata.ResourceManagerConfiguration#getVendor()
   */
  public String getVendor() {
    return mVendor;
  }

  /**
   * @see org.apache.uima.resource.metadata.ResourceManagerConfiguration#setVendor(String)
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

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ResourceManagerConfiguration#getExternalResources()
   */
  public ExternalResourceDescription[] getExternalResources() {
    return mExternalResources;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ResourceManagerConfiguration#setExternalResources(org.apache.uima.resource.ExternalResourceDescription[])
   */
  public void setExternalResources(ExternalResourceDescription[] aDescriptions) {
    mExternalResources = (aDescriptions != null) ? aDescriptions
            : new ExternalResourceDescription[0];
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ResourceManagerConfiguration#getExternalResourceBindings()
   */
  public ExternalResourceBinding[] getExternalResourceBindings() {
    return mBindings;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ResourceManagerConfiguration#setExternalResourceBindings(org.apache.uima.resource.metadata.ExternalResourceBinding[])
   */
  public void setExternalResourceBindings(ExternalResourceBinding[] aBindings) {
    mBindings = (aBindings != null) ? aBindings : new ExternalResourceBinding[0];
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ResourceManagerConfiguration#addExternalResource(org.apache.uima.resource.ExternalResourceDescription)
   */
  public void addExternalResource(ExternalResourceDescription aExternalResourceDescription) {
    ExternalResourceDescription[] current = getExternalResources();
    ExternalResourceDescription[] newArr = new ExternalResourceDescription[current.length + 1];
    System.arraycopy(current, 0, newArr, 0, current.length);
    newArr[current.length] = aExternalResourceDescription;
    setExternalResources(newArr);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ResourceManagerConfiguration#addExternalResourceBinding(org.apache.uima.resource.metadata.ExternalResourceBinding)
   */
  public void addExternalResourceBinding(ExternalResourceBinding aExternalResourceBinding) {
    ExternalResourceBinding[] current = getExternalResourceBindings();
    ExternalResourceBinding[] newArr = new ExternalResourceBinding[current.length + 1];
    System.arraycopy(current, 0, newArr, 0, current.length);
    newArr[current.length] = aExternalResourceBinding;
    setExternalResourceBindings(newArr);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ResourceManagerConfiguration#removeExternalResource(org.apache.uima.resource.ExternalResourceDescription)
   */
  public void removeExternalResource(ExternalResourceDescription aExternalResourceDescription) {
    ExternalResourceDescription[] current = getExternalResources();
    for (int i = 0; i < current.length; i++) {
      if (current[i] == aExternalResourceDescription) {
        ExternalResourceDescription[] newArr = new ExternalResourceDescription[current.length - 1];
        System.arraycopy(current, 0, newArr, 0, i);
        System.arraycopy(current, i + 1, newArr, i, current.length - i - 1);
        setExternalResources(newArr);
        break;
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ResourceManagerConfiguration#removeExternalResourceBinding(org.apache.uima.resource.metadata.ExternalResourceBinding)
   */
  public void removeExternalResourceBinding(ExternalResourceBinding aExternalResourceBinding) {
    ExternalResourceBinding[] current = getExternalResourceBindings();
    for (int i = 0; i < current.length; i++) {
      if (current[i] == aExternalResourceBinding) {
        ExternalResourceBinding[] newArr = new ExternalResourceBinding[current.length - 1];
        System.arraycopy(current, 0, newArr, 0, i);
        System.arraycopy(current, i + 1, newArr, i, current.length - i - 1);
        setExternalResourceBindings(newArr);
        break;
      }
    }
  }

  /**
   * @see org.apache.uima.resource.metadata.ResourceManagerConfiguration#getImport()
   * @deprecated
   */
  @Deprecated
  public Import getImport() {
    if (mImports.length > 0) {
      return mImports[0];
    } else {
      return null;
    }
  }

  /**
   * @see org.apache.uima.resource.metadata.ResourceManagerConfiguration#setImport(org.apache.uima.resource.metadata.Import)
   * @deprecated
   */
  @Deprecated
  public void setImport(Import aImport) {
    mImports = new Import[] { aImport };
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.TypeSystemDescription#resolveImports()
   */
  // support multi-threading, avoid object creation if no imports
  public synchronized void resolveImports() throws InvalidXMLException {
    if (getImports().length == 0) {
      resolveImports(null, null);
    } else {
      resolveImports(new TreeSet<String>(), UIMAFramework.newDefaultResourceManager());
    }
  }

  public synchronized void resolveImports(ResourceManager aResourceManager) throws InvalidXMLException {
    resolveImports((getImports().length == 0) ? null : new TreeSet<String>(), aResourceManager);
  }

  public synchronized void resolveImports(Collection<String> aAlreadyImportedURLs, ResourceManager aResourceManager)
          throws InvalidXMLException {
    List<ExternalResourceDescription> importedResources = null;
    List<ExternalResourceBinding> importedBindings = null;
    if (getImports().length != 0) {
      // add our own URL, if known, to the collection of already imported URLs
      if (getSourceUrl() != null) {
        aAlreadyImportedURLs.add(getSourceUrl().toString());
      }
      importedResources = new ArrayList<ExternalResourceDescription>();
      importedBindings = new ArrayList<ExternalResourceBinding>();
      Import[] imports = getImports();
      for (int i = 0; i < imports.length; i++) {
        // make sure Import's relative path base is set, to allow for users who create
        // new import objects
        if (imports[i] instanceof Import_impl) {
          ((Import_impl) imports[i]).setSourceUrlIfNull(this.getSourceUrl());
        }
        URL url = imports[i].findAbsoluteUrl(aResourceManager);
        if (!aAlreadyImportedURLs.contains(url.toString())) {
          aAlreadyImportedURLs.add(url.toString());
          try {
            resolveImport(url, aAlreadyImportedURLs, importedResources, importedBindings,
                    aResourceManager);
          } catch (IOException e) {
            throw new InvalidXMLException(InvalidXMLException.IMPORT_FAILED_COULD_NOT_READ_FROM_URL,
                    new Object[] { url, imports[i].getSourceUrlString() }, e);
          }
        }
      }
    }
    
    // update this object
    ExternalResourceDescription[] existingResources = this.getExternalResources();
    if (existingResources == null) {
      this.setExternalResources(existingResources = ExternalResourceDescription.EMPTY_EXTERNAL_RESORUCE_DESCRIPTIONS);
    }
    if (importedResources != null) {
      ExternalResourceDescription[] newResources = new ExternalResourceDescription[existingResources.length
              + importedResources.size()];
      System.arraycopy(existingResources, 0, newResources, 0, existingResources.length);
      for (int i = 0; i < importedResources.size(); i++) {
        newResources[existingResources.length + i] = importedResources
                .get(i);
      }
      this.setExternalResources(newResources);
    }
    
    ExternalResourceBinding[] existingBindings = this.getExternalResourceBindings();
    if (existingBindings == null) {
      this.setExternalResourceBindings(existingBindings = ExternalResourceBinding.EMPTY_RESOURCE_BINDINGS);
    }
    if (null != importedBindings) {
      ExternalResourceBinding[] newBindings = new ExternalResourceBinding[existingBindings.length
              + importedBindings.size()];
      System.arraycopy(existingBindings, 0, newBindings, 0, existingBindings.length);
      for (int i = 0; i < importedBindings.size(); i++) {
        newBindings[existingBindings.length + i] = importedBindings.get(i);
      }
      this.setExternalResourceBindings(newBindings);
    }
    // clear imports
    this.setImports(Import.EMPTY_IMPORTS);
  }

  private void resolveImport(URL aURL, Collection<String> aAlreadyImportedURLs,
          Collection<ExternalResourceDescription> aResultResources, Collection<ExternalResourceBinding> aResultBindings, ResourceManager aResourceManager)
          throws InvalidXMLException, IOException {
    XMLInputSource input = new XMLInputSource(aURL);
    ResourceManagerConfiguration desc = UIMAFramework.getXMLParser()
            .parseResourceManagerConfiguration(input);
    desc.resolveImports(aAlreadyImportedURLs, aResourceManager);
    aResultResources.addAll(Arrays.asList(desc.getExternalResources()));
    aResultBindings.addAll(Arrays.asList(desc.getExternalResourceBindings()));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#writePropertyAsElement(org.apache.uima.resource.metadata.impl.PropertyXmlInfo,
   *      java.lang.String, org.xml.sax.ContentHandler)
   */
  @Override
  protected void writePropertyAsElement(PropertyXmlInfo aPropInfo, String aNamespace) throws SAXException {
    // Prevent the import property from being written to XML - it exists only so old-style XML
    // can be read.
    if (!"import".equals(aPropInfo.propertyName)) {
      super.writePropertyAsElement(aPropInfo, aNamespace);
    }
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo(
          "resourceManagerConfiguration", new PropertyXmlInfo[] {
              new PropertyXmlInfo("import", null), new PropertyXmlInfo("name", true),
              new PropertyXmlInfo("description", true), new PropertyXmlInfo("version", true),
              new PropertyXmlInfo("vendor", true),
              new PropertyXmlInfo("imports", true),
              new PropertyXmlInfo("import", true), // for backwards compatibility; not
              // written to XML
              new PropertyXmlInfo("externalResources"),
              new PropertyXmlInfo("externalResourceBindings"), });

}
