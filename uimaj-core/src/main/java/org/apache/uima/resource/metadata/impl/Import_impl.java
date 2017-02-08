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

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.XMLParser;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * 
 * 
 */
public class Import_impl extends MetaDataObject_impl implements Import {
  
  private static final long serialVersionUID = 6876757002913848998L;
  
  /**
   * resource bundle for log messages
   */
  protected static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";

  private String mName;

  private String mLocation;
  
  private String byNameSuffix = ".xml";

  /*
   * UIMA-5274  Expand any references to external overrides when name and location are fetched.
   * Cache the value if the evaluation succeeds (later fetches may not have the settings defined!)
   * Leave value unmodified if any settings are undefined.
   */

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.Import#getName()
   */
  public String getName() {
    if (mName != null && mName.contains("${")) {
      String value = resolveSettings(mName);
      if (value != null) {  // Success!
        mName = value;
      }
    }
    return mName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.Import#setName(java.lang.String)
   */
  public void setName(String aName) {
    mName = aName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.Import#getLocation()
   */
  public String getLocation() {
    if (mLocation != null && mLocation.contains("${")) {
      String value = resolveSettings(mLocation);
      if (value != null) {
        mLocation = value;
      }
    }
    return mLocation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.Import#setLocation(java.lang.String)
   */
  public void setLocation(String aUri) {
    mLocation = aUri;
  }

  /*
   * Called when importing by name non-xml files, e.g. external override settings
   */
  public void setSuffix(String suffix) {
    byNameSuffix = suffix;
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.Import#findAbsoluteUrl(org.apache.uima.resource.ResourceManager)
   */
  public URL findAbsoluteUrl(ResourceManager aResourceManager) throws InvalidXMLException {
    String location, name;
    if ((location=getLocation()) != null) {
      try {
        URL url = new URL(this.getRelativePathBase(), location);
        UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
                "findAbsoluteUrl", LOG_RESOURCE_BUNDLE, "UIMA_import_by__CONFIG",
                new Object[] {"location", url});
        return url;
      } catch (MalformedURLException e) {
        throw new InvalidXMLException(InvalidXMLException.MALFORMED_IMPORT_URL, new Object[] {
            location, getSourceUrlString() }, e);
      }
    } else if ((name=getName()) != null) {
      String filename = name.replace('.', '/') + byNameSuffix;
      URL url;
      try {
        url = aResourceManager.resolveRelativePath(filename);
        UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
                "findAbsoluteUrl", LOG_RESOURCE_BUNDLE, "UIMA_import_by__CONFIG",
                new Object[] {"name", url});
      } catch (MalformedURLException e) {
        throw new InvalidXMLException(InvalidXMLException.IMPORT_BY_NAME_TARGET_NOT_FOUND,
                new Object[] { filename, getSourceUrlString() }, e);
      }
      if (url == null) {
        throw new InvalidXMLException(InvalidXMLException.IMPORT_BY_NAME_TARGET_NOT_FOUND,
                new Object[] { filename, getSourceUrlString() });
      }
      return url;
    } else {
      // no name or location -- this should be caught at XML parse time but we still need to
      // check here in case object was modified or was created progrmatically.
      throw new InvalidXMLException(InvalidXMLException.IMPORT_MUST_HAVE_NAME_XOR_LOCATION,
              new Object[] { getSourceUrlString() });
    }
  }

  /**
   * Overridden to provide custom XML representation.
   * 
   * @see org.apache.uima.util.XMLizable#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser)
   */
  public void buildFromXMLElement(Element aElement, XMLParser aParser,
          XMLParser.ParsingOptions aOptions) throws InvalidXMLException {
    String name = aElement.getAttribute("name");
    setName(name.length() == 0 ? null : name);
    String location = aElement.getAttribute("location");
    setLocation(location.length() == 0 ? null : location);

    // validate that at exactly one of name or location is specified
    if (!((getName() == null) ^ (getLocation() == null))) {
      throw new InvalidXMLException(InvalidXMLException.IMPORT_MUST_HAVE_NAME_XOR_LOCATION,
              new Object[0]);
    }
  }

  
  /**
   * Overridden to provide custom XML representation.
   * 
   * @see org.apache.uima.util.XMLizable#toXML(ContentHandler)
   */
  
  public void toXML(ContentHandler aContentHandler, boolean aWriteDefaultNamespaceAttribute) throws SAXException {
    if (null == serialContext.get()) {
      getSerialContext(aContentHandler);  
      try {
        toXMLinner(aWriteDefaultNamespaceAttribute);
      } finally {
        serialContext.remove();
      }
    } else {
      toXMLinner(aWriteDefaultNamespaceAttribute);
    }
  }
  
  private void toXMLinner(boolean aWriteDefaultNamespaceAttribute)
          throws SAXException {
    SerialContext sc = serialContext.get();
    Serializer serializer = sc.serializer;   
    
    String namespace = getXmlizationInfo().namespace;
    AttributesImpl attrs = new AttributesImpl();
    String name = getName();
    if (name != null) {
      attrs.addAttribute("", "name", "name", "", name);
    }
    String location = getLocation();
    if (location != null) {
      attrs.addAttribute("", "location", "location", "", location);
    }
    Node node = serializer.findMatchingSubElement("import");
    serializer.outputStartElement(node, namespace, "import", "import", attrs);
//    aContentHandler.startElement(getXmlizationInfo().namespace, "import", "import", attrs);
    serializer.outputEndElement(node, namespace, "import", "import");
//    aContentHandler.endElement(getXmlizationInfo().namespace, "import", "import");
  }

  /**
   * @see MetaDataObject_impl#getXmlizationInfo()
   */
  protected XmlizationInfo getXmlizationInfo() {
    return new XmlizationInfo(null, null);
    // this object has custom XMLization routines
  }

}
