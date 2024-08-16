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

package org.apache.uima.analysis_engine.metadata.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;
import org.w3c.dom.Element;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Declares which FlowController is used by the Aggregate Analysis Engine.
 */
public class FlowControllerDeclaration_impl extends MetaDataObject_impl
        implements FlowControllerDeclaration {
  private static final long serialVersionUID = 1526130202197517743L;

  private String mKey;

  private Import mImport;

  private ResourceSpecifier mSpecifier;

  @Override
  public String getKey() {
    return mKey;
  }

  @Override
  public void setKey(String aKey) {
    mKey = aKey;
  }

  @Override
  public Import getImport() {
    return mImport;
  }

  @Override
  public void setImport(Import aImport) {
    mImport = aImport;
  }

  @Override
  public ResourceSpecifier getSpecifier() {
    return mSpecifier;
  }

  @Override
  public void setSpecifier(ResourceSpecifier aSpecifier) {
    mSpecifier = aSpecifier;
  }

  @Override
  public void resolveImports() throws InvalidXMLException {
    if (getImport() != null) {
      resolveImports(UIMAFramework.newDefaultResourceManager());
    }
  }

  /*
   * @see FlowControllerDeclaration#resolveImports(org.apache.uima.resource.ResourceManager)
   * Synchronized to support parallel initialization calls on primitive AEs, sharing a common
   * Resource Manager, and perhaps common UIMA Contexts
   */
  @Override
  public synchronized void resolveImports(ResourceManager aResourceManager)
          throws InvalidXMLException {
    Import theImport = getImport();
    if (theImport != null) {
      URL url = theImport.findAbsoluteUrl(aResourceManager);
      InputStream stream = null;
      try {
        stream = url.openStream();
        XMLInputSource input = new XMLInputSource(url);
        ResourceSpecifier spec = UIMAFramework.getXMLParser().parseResourceSpecifier(input);
        setSpecifier(spec);
        setImport(null);
      } catch (IOException e) {
        throw new InvalidXMLException(e);
      } finally {
        try {
          if (stream != null) {
            stream.close();
          }
        } catch (IOException e1) {
          UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, "", e1);
        }
      }
    }
  }

  @Override
  public void buildFromXMLElement(Element aElement, XMLParser aParser, ParsingOptions aOptions)
          throws InvalidXMLException {
    super.buildFromXMLElement(aElement, aParser, aOptions);
    // set key from attributes
    mKey = aElement.getAttribute("key");
  }

  @Override
  protected AttributesImpl getXMLAttributes() {
    // write key as attribute
    AttributesImpl attrs = new AttributesImpl();
    if (mKey != null && mKey.length() > 0) {
      attrs.addAttribute("", "key", "key", "string", mKey);
    }
    return attrs;
  }

  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("flowController",
          new PropertyXmlInfo[] { new PropertyXmlInfo("import", null),
              new PropertyXmlInfo("specifier", null) });
}
