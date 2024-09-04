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

/**
 * The Class CasProcessorExecutableImpl.
 */
public class CasProcessorExecutableImpl extends MetaDataObject_impl
        implements CasProcessorExecutable {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 6897788743141912586L;

  /** The executable. */
  private String executable;

  /** The dir. */
  private String dir;

  /** The args. */
  private CasProcessorExecArgs args = new CasProcessorExecArgsImpl();

  /** The envs. */
  private ArrayList envs = new ArrayList();

  /**
   * Instantiates a new cas processor executable impl.
   */
  public CasProcessorExecutableImpl() {
  }

  /**
   * Sets the CAS processor exec args.
   *
   * @param aArgs
   *          the new CAS processor exec args
   */
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.collection.metadata.CasProcessorExecutable#setCASProcessorExecArgs(org.apache.
   * uima.collection.metadata.CASProcessorExecArgs)
   */
  public void setCASProcessorExecArgs(CasProcessorExecArgs aArgs) {
    args = aArgs;
  }

  /**
   * Gets the CAS processor exec args.
   *
   * @return the CAS processor exec args
   */
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorExecutable#getCASProcessorExecArgs()
   */
  public CasProcessorExecArgs getCASProcessorExecArgs() {
    return args;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.collection.metadata.CasProcessorExecutable#addCasProcessorExecArg(org.apache.
   * uima.collection.metadata.CasProcessorExecArg)
   */
  @Override
  public void addCasProcessorExecArg(CasProcessorExecArg aArg) {
    args.add(aArg);
  }

  /**
   * Adds the cas processor runtime env param.
   *
   * @param aParam
   *          the a param
   */
  public void addCasProcessorRuntimeEnvParam(CasProcessorRuntimeEnvParam aParam) {
    envs.add(aParam);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorExecutable#getCasProcessorExecArg(int)
   */
  @Override
  public CasProcessorExecArg getCasProcessorExecArg(int aIndex) {
    try {
      return args.get(aIndex);
    } catch (Exception e) {
      // ignore
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorExecutable#getAllCasProcessorExecArgs()
   */
  @Override
  public CasProcessorExecArg[] getAllCasProcessorExecArgs() {
    return args.getAll();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorExecutable#removeCasProcessorExecArg(int)
   */
  @Override
  public void removeCasProcessorExecArg(int aIndex) {
    args.remove(aIndex);
  }

  /**
   * Gets the dir.
   *
   * @return the directory
   */
  public String getDir() {
    return dir;
  }

  /**
   * Gets the executable.
   *
   * @return the executable
   */
  @Override
  public String getExecutable() {
    return executable;
  }

  /**
   * Sets the dir.
   *
   * @param string
   *          the new dir
   */
  public void setDir(String string) {
    dir = string;
  }

  /**
   * Sets the executable.
   *
   * @param string
   *          the new executable
   */
  @Override
  public void setExecutable(String string) {
    executable = string;
  }

  /**
   * Gets the args.
   *
   * @return an array of arguments
   */
  protected CasProcessorExecArg[] getArgs() {
    return args.getAll();
  }

  /**
   * Sets the args.
   *
   * @param aargs
   *          the new args
   */
  protected void setArgs(CasProcessorExecArg[] aargs) {
    for (int i = 0; aargs != null && i < aargs.length; i++) {
      args.add(aargs[i]);
    }
  }

  /**
   * Overridden to read "name" and "value" attributes.
   *
   * @param aElement
   *          the a element
   * @param aParser
   *          the a parser
   * @param aOptions
   *          the a options
   * @throws InvalidXMLException
   *           the invalid XML exception
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser, org.apache.uima.util.XMLParser.ParsingOptions)
   */
  @Override
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
          CasProcessorRuntimeEnvParam env = (CasProcessorRuntimeEnvParam) aParser
                  .buildObject((Element) node, aOptions);
          envs.add(env);
        }
      }
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.resource.metadata.impl.MetaDataObject_impl#toXML(org.xml.sax.ContentHandler,
   * boolean)
   */
  @Override
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
   * @return the XML attributes
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXMLAttributes()
   */
  @Override
  protected AttributesImpl getXMLAttributes() {
    AttributesImpl attrs = super.getXMLAttributes();

    attrs.addAttribute("", "executable", "executable", "CDATA", getExecutable());
    if (dir != null && dir.trim().length() > 0) {
      attrs.addAttribute("", "dir", "dir", "CDATA", getDir());
    }
    return attrs;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  /** The Constant XMLIZATION_INFO. */
  private static final XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("exec",
          new PropertyXmlInfo[] { new PropertyXmlInfo("arg"), new PropertyXmlInfo("env"), });

  /**
   * Gets the envs.
   *
   * @return the environment items
   */
  @Override
  public ArrayList getEnvs() {
    return envs;
  }

  /**
   * Sets the envs.
   *
   * @param params
   *          the new envs
   */
  @Override
  public void setEnvs(ArrayList params) {
    envs = params;
  }

}
