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

import java.util.ArrayList;

import org.apache.uima.collection.metadata.CasProcessorExecArg;
import org.apache.uima.collection.metadata.CasProcessorExecArgs;
import org.apache.uima.collection.metadata.CasProcessorExecutable;
import org.apache.uima.collection.metadata.CasProcessorRuntimeEnvParam;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class CasProcessorExecutableImpl extends MetaDataObject_impl implements
        CasProcessorExecutable {
  private static final long serialVersionUID = 6897788743141912586L;

  private String executable;

  private String dir;

  private CasProcessorExecArgs args = new CasProcessorExecArgsImpl();

  private ArrayList envs = new ArrayList();

  public CasProcessorExecutableImpl() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorExecutable#setCASProcessorExecArgs(org.apache.uima.collection.metadata.CASProcessorExecArgs)
   */
  public void setCASProcessorExecArgs(CasProcessorExecArgs aArgs) {
    args = aArgs;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorExecutable#getCASProcessorExecArgs()
   */
  public CasProcessorExecArgs getCASProcessorExecArgs() {
    return args;
  }

  public void addCasProcessorExecArg(CasProcessorExecArg aArg) {
    args.add(aArg);
  }

  public void addCasProcessorRuntimeEnvParam(CasProcessorRuntimeEnvParam aParam) {
    envs.add(aParam);
  }

  public CasProcessorExecArg getCasProcessorExecArg(int aIndex) {
    try {
      return args.get(aIndex);
    } catch (Exception e) {
      // ignore
    }
    return null;
  }

  public CasProcessorExecArg[] getAllCasProcessorExecArgs() {
    return args.getAll();
  }

  public void removeCasProcessorExecArg(int aIndex) {
    args.remove(aIndex);
  }

  /**
   * @return the directory
   */
  public String getDir() {
    return dir;
  }

  /**
   * @return the executable
   */
  public String getExecutable() {
    return executable;
  }

  /**
   * @param string
   */
  public void setDir(String string) {
    dir = string;
  }

  /**
   * @param string
   */
  public void setExecutable(String string) {
    executable = string;
  }

  /**
   * @return an array of arguments
   */
  protected CasProcessorExecArg[] getArgs() {
    return args.getAll();
  }

  /**
   * @param aargs
   */
  protected void setArgs(CasProcessorExecArg[] aargs) {
    for (int i = 0; aargs != null && i < aargs.length; i++) {
      args.add(aargs[i]);
    }
  }

  /**
   * Overridden to read "name" and "value" attributes.
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser, org.apache.uima.util.XMLParser.ParsingOptions)
   */
  public void buildFromXMLElement(Element aElement, XMLParser aParser, ParsingOptions aOptions)
          throws InvalidXMLException {
    // TODO Auto-generated method stub
    setExecutable(aElement.getAttribute("executable"));
    setDir(aElement.getAttribute("dir"));

    NodeList nodes = aElement.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      if (node instanceof Element) {
        if ("arg".equals(node.getNodeName())) {
          // assumes all children are CasProcessor elements
          CasProcessorExecArg arg = (CasProcessorExecArg) aParser.buildObject((Element) node,
                  aOptions);
          args.add(arg);
        } else if ("env".equals(node.getNodeName())) {
          // assumes all children are CasProcessor elements
          CasProcessorRuntimeEnvParam env = (CasProcessorRuntimeEnvParam) aParser.buildObject(
                  (Element) node, aOptions);
          envs.add(env);
        }
      }
    }

  }

  public void toXML(ContentHandler aContentHandler, boolean aWriteDefaultNamespaceAttribute)
          throws SAXException {
    XmlizationInfo inf = getXmlizationInfo();

    // write the element's start tag
    // get attributes (can be provided by subclasses)
    AttributesImpl attrs = getXMLAttributes();
    // add default namespace attr if desired
    if (aWriteDefaultNamespaceAttribute) {
      if (inf.namespace != null) {
        attrs.addAttribute("", "xmlns", "xmlns", null, inf.namespace);
      }
    }

    // start element
    aContentHandler.startElement(inf.namespace, inf.elementTagName, inf.elementTagName, attrs);
    // write child elements
    for (int i = 0; i < envs.size(); i++) {
      ((CasProcessorRuntimeEnvParam) envs.get(i)).toXML(aContentHandler,
              aWriteDefaultNamespaceAttribute);
    }
    CasProcessorExecArg[] argList = args.getAll();
    // write child elements
    for (int i = 0; i < argList.length; i++) {
      argList[i].toXML(aContentHandler, aWriteDefaultNamespaceAttribute);
    }

    // end element
    aContentHandler.endElement(inf.namespace, inf.elementTagName, inf.elementTagName);
  }

  /**
   * Overridden to handle "name" and "value" attributes.
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXMLAttributes()
   */
  protected AttributesImpl getXMLAttributes() {
    AttributesImpl attrs = super.getXMLAttributes();

    attrs.addAttribute("", "executable", "executable", "CDATA", getExecutable());
    if (dir != null && dir.trim().length() > 0) {
      attrs.addAttribute("", "dir", "dir", "CDATA", getDir());
    }
    return attrs;
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("exec",
          new PropertyXmlInfo[] { new PropertyXmlInfo("arg"), new PropertyXmlInfo("env"), });

  /**
   * @return the environment items
   */
  public ArrayList getEnvs() {
    return envs;
  }

  /**
   * @param params
   */
  public void setEnvs(ArrayList params) {
    envs = params;
  }

}
