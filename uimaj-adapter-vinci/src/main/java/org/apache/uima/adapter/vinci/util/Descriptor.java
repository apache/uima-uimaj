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

package org.apache.uima.adapter.vinci.util;

import java.io.File;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.uima.UIMAFramework;
import org.apache.uima.util.Level;

/**
 * Instance of this class handles parsing of the xml descriptor files. It also retrieves values of
 * the named attributes of the named elements.
 * 
 * 
 * 
 */
public class Descriptor extends DefaultHandler {

  private String serviceName = "";

  private int instanceCount = 0;

  private String resourceSpecifierPath = "";

  private String filterString = "";

  private String namingServiceHost = "localhost";

  private int serverSocketTimeout = 300000; // 5 minute timeout on the service socket
  
  private int threadPoolMinSize = 1;
  
  private int threadPoolMaxSize = 20;

  /**
   * Constructor responsible for parsing the descriptor file named in filePath.
   * 
   * @param filePath Fully qualified path the xml descriptor.
   */
  public Descriptor(String filePath) {
    try {
      parse(filePath);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private Document parse(String configFile) {
    Document doc = null;
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
      factory.setValidating(false);

      // Create the builder and parse the file
      SAXParser parser = factory.newSAXParser();
      parser.parse(configFile, this);

      UIMAFramework.getLogger().log(Level.CONFIG, "Resource::" + getResourceSpecifierPath());
      UIMAFramework.getLogger().log(Level.CONFIG, "Instance Count::" + getInstanceCount());
      UIMAFramework.getLogger().log(Level.CONFIG, "Service Name::" + getServiceName());
      UIMAFramework.getLogger().log(Level.CONFIG, "Filter String::" + getFilterString());
      UIMAFramework.getLogger().log(Level.CONFIG, "Naming Service Host::" + getNamingServiceHost());
      UIMAFramework.getLogger().log(Level.CONFIG,
              "Server Socket Timeout::" + getServerSocketTimeout());

    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return doc;
  }

  private String getAttribute(String attrName, Attributes attribs) {
    String attributeValue = null;

    // Add all attributes to the Attribute container
    for (int i = 0; attribs != null && i < attribs.getLength(); i++) {
      String attributeName = getName(attribs.getLocalName(i), attribs.getQName(i));
      if (attrName.equals(attributeName)) {
        attributeValue = attribs.getValue(i);
      }
    }

    return attributeValue;
  }

  private String getName(String s1, String s2) {
    if (s1 == null || "".equals(s1))
      return s2;
    else
      return s1;
  }

  public void startElement(String uri, String localName, String qName, Attributes attribs) {
    String elementName = getName(localName, qName);
    // if next element is complex, push a new instance on the stack
    // if element has attributes, set them in the new instance
    if (elementName.equals("service")) {
      setServiceName(getAttribute("name", attribs));
    } else if (elementName.equals("parameter")) {
      String att = getAttribute("name", attribs);
      String value = getAttribute("value", attribs);
      if ("resourceSpecifierPath".equals(att)) {
        setResourceSpecifierPath(value);
      } else if ("filterString".equals(att)) {
        setFilterString(value);
      } else if ("numInstances".equals(att)) {
        try {
          setInstanceCount(Integer.parseInt(value));
        } catch (NumberFormatException nbe) {
          setInstanceCount(1);
        }
      } else if ("serverSocketTimeout".equals(att)) {
        try {
          setServerSocketTimeout(Integer.parseInt(value));
        } catch (NumberFormatException nbe) {
          setServerSocketTimeout(300000); // Default is 5 minutes
        }
      } else if ("namingServiceHost".equals(att)) {
        try {
          setNamingServiceHost(value);
        } catch (NumberFormatException nbe) {
          setNamingServiceHost("localhost");
        }
      } else if ("threadPoolMinSize".equals(att)) {
        try {
          setThreadPoolMinSize(Integer.parseInt(value));
        } catch (NumberFormatException nbe) {
          setThreadPoolMinSize(1);
        }
      } else if ("threadPoolMaxSize".equals(att)) {
        try {
          setThreadPoolMaxSize(Integer.parseInt(value));
        } catch (NumberFormatException nbe) {
          setThreadPoolMaxSize(20);
        }
      }
    }
    // if none of the above, it is an unexpected element. we ignore these for now
  }

  public int getThreadPoolMaxSize() {
    return threadPoolMaxSize;
  }

  public void setThreadPoolMaxSize(int threadPoolMaxSize) {
    this.threadPoolMaxSize = threadPoolMaxSize;
  }

  public int getThreadPoolMinSize() {
    return threadPoolMinSize;
  }

  public void setThreadPoolMinSize(int threadPoolMinSize) {
    this.threadPoolMinSize = threadPoolMinSize;
  }

  /**
   * Returns the instanceCount.
   * 
   * @return int
   */
  public int getInstanceCount() {
    return instanceCount;
  }

  /**
   * Returns the resourceSpecifierPath.
   * 
   * @return String
   */
  public String getResourceSpecifierPath() {
    return resourceSpecifierPath;
  }

  /**
   * Returns the serviceName.
   * 
   * @return String
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Sets the instanceCount.
   * 
   * @param instanceCount
   *          The instanceCount to set
   */
  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }

  /**
   * Sets the resourceSpecifierPath.
   * 
   * @param resourceSpecifierPath
   *          The resourceSpecifierPath to set
   */
  public void setResourceSpecifierPath(String resourceSpecifierPath) {
    this.resourceSpecifierPath = resourceSpecifierPath;
  }

  /**
   * Sets the serviceName.
   * 
   * @param serviceName
   *          The serviceName to set
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * Returns the filterString.
   * 
   * @return String
   */
  public String getFilterString() {
    return filterString;
  }

  /**
   * Sets the filterString.
   * 
   * @param filterString
   *          The filterString to set
   */
  public void setFilterString(String filterString) {
    this.filterString = filterString;
  }

  public String getNamingServiceHost() {
    return namingServiceHost;
  }

  public void setNamingServiceHost(String namingServiceHost) {
    this.namingServiceHost = namingServiceHost;
  }

  public int getServerSocketTimeout() {
    return serverSocketTimeout;
  }

  public void setServerSocketTimeout(int serverSocketTimeout) {
    this.serverSocketTimeout = serverSocketTimeout;
  }

}
