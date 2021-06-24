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

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.uima.UIMAFramework;
import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.util.Level;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;


// TODO: Auto-generated Javadoc
/**
 * Instance of this class handles parsing of the xml descriptor files. It also retrieves values of
 * the named attributes of the named elements.
 * 
 * 
 * 
 */
public class Descriptor extends DefaultHandler {

  /** The service name. */
  private String serviceName = "";

  /** The instance count. */
  private int instanceCount = 0;

  /** The resource specifier path. */
  private String resourceSpecifierPath = "";

  /** The filter string. */
  private String filterString = "";

  /** The naming service host. */
  private String namingServiceHost = "localhost";

  /** The server socket timeout. */
  private int serverSocketTimeout = 300000; // 5 minute timeout on the service socket
  
  /** The thread pool min size. */
  private int threadPoolMinSize = 1;
  
  /** The thread pool max size. */
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

  /**
   * Parses the.
   *
   * @param configFile the config file
   * @return the document
   */
  private Document parse(String configFile) {
    Document doc = null;
    try {
      SAXParserFactory factory = XMLUtils.createSAXParserFactory();
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

  /**
   * Gets the attribute.
   *
   * @param attrName the attr name
   * @param attribs the attribs
   * @return the attribute
   */
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

  /**
   * Gets the name.
   *
   * @param s1 the s 1
   * @param s2 the s 2
   * @return the name
   */
  private String getName(String s1, String s2) {
    if (s1 == null || "".equals(s1))
      return s2;
    else
      return s1;
  }

  /* (non-Javadoc)
   * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
   */
  @Override
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

  /**
   * Gets the thread pool max size.
   *
   * @return the thread pool max size
   */
  public int getThreadPoolMaxSize() {
    return threadPoolMaxSize;
  }

  /**
   * Sets the thread pool max size.
   *
   * @param threadPoolMaxSize the new thread pool max size
   */
  public void setThreadPoolMaxSize(int threadPoolMaxSize) {
    this.threadPoolMaxSize = threadPoolMaxSize;
  }

  /**
   * Gets the thread pool min size.
   *
   * @return the thread pool min size
   */
  public int getThreadPoolMinSize() {
    return threadPoolMinSize;
  }

  /**
   * Sets the thread pool min size.
   *
   * @param threadPoolMinSize the new thread pool min size
   */
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

  /**
   * Gets the naming service host.
   *
   * @return the naming service host
   */
  public String getNamingServiceHost() {
    return namingServiceHost;
  }

  /**
   * Sets the naming service host.
   *
   * @param namingServiceHost the new naming service host
   */
  public void setNamingServiceHost(String namingServiceHost) {
    this.namingServiceHost = namingServiceHost;
  }

  /**
   * Gets the server socket timeout.
   *
   * @return the server socket timeout
   */
  public int getServerSocketTimeout() {
    return serverSocketTimeout;
  }

  /**
   * Sets the server socket timeout.
   *
   * @param serverSocketTimeout the new server socket timeout
   */
  public void setServerSocketTimeout(int serverSocketTimeout) {
    this.serverSocketTimeout = serverSocketTimeout;
  }

}
