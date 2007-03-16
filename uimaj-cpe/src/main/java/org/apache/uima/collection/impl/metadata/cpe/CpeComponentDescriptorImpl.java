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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.uima.collection.metadata.CpeComponentDescriptor;
import org.apache.uima.collection.metadata.CpeInclude;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;

public class CpeComponentDescriptorImpl extends MetaDataObject_impl implements
        CpeComponentDescriptor {
  private static final long serialVersionUID = 1607312024379882416L;

  private CpeInclude mInclude;
  
  private Import mImport;

  public CpeComponentDescriptorImpl() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeComponentDescriptor#setInclude(org.apache.uima.collection.metadata.CpeInclude)
   */
  public void setInclude(CpeInclude aInclude) {
    mInclude = aInclude;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeComponentDescriptor#getInclude()
   */
  public CpeInclude getInclude() {
    return mInclude;
  }
  
  

  /* (non-Javadoc)
   * @see org.apache.uima.collection.metadata.CpeComponentDescriptor#getImport()
   */
  public Import getImport() {
    return mImport;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.collection.metadata.CpeComponentDescriptor#setImport(org.apache.uima.resource.metadata.Import)
   */
  public void setImport(Import aImport) {
    mImport = aImport;
  }
  
  /**
   * @see CpeComponentDescriptor#findAbsoluteUrl(ResourceManager)
   */
  public URL findAbsoluteUrl(ResourceManager aResourceManager) throws ResourceConfigurationException {
    try {
      if (mImport != null) {
        return mImport.findAbsoluteUrl(aResourceManager);
      }
      else {
        String path = mInclude.get();
        //replace ${CPM_HOME}
        if (path.startsWith("${CPM_HOME}")) {
          String cpmHome = System.getProperty("CPM_HOME");
          path = cpmHome + path.substring("${CPM_HOME}".length());
        }
        try {
          //try path as a URL, then if that fails try it as a File
          //TODO: is there a good way to tell if it's a valid URL without
          //having to catch MalformedURLException?
          return new URL(path);         
        } catch (MalformedURLException e) {
          try {
            return new File(path).getAbsoluteFile().toURI().toURL();
          }
          catch(MalformedURLException e2) {
            throw new InvalidXMLException(InvalidXMLException.MALFORMED_IMPORT_URL, new Object[] {
                  path, getSourceUrlString() }, e);
          }
        }
      }
    } catch (InvalidXMLException e) {
      throw new ResourceConfigurationException(e);
    }
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("descriptor",
          new PropertyXmlInfo[] { 
           new PropertyXmlInfo("include", null), 
           new PropertyXmlInfo("import", null)});

}
