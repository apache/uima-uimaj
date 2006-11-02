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

package org.apache.uima.impl.collection.metadata.cpe;

import org.apache.uima.collection.metadata.CpeResourceManagerConfiguration;
import org.apache.uima.impl.resource.metadata.MetaDataObject_impl;
import org.apache.uima.impl.resource.metadata.PropertyXmlInfo;
import org.apache.uima.impl.resource.metadata.XmlizationInfo;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;

public class CpeResourceManagerConfigurationImpl extends MetaDataObject_impl implements CpeResourceManagerConfiguration 
{
	private String href;
	
	public CpeResourceManagerConfigurationImpl() {}
	
	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeResourceManagerConfiguration#set(java.lang.String)
	 */
	public void set(String aPath)
	{
		setHref(aPath);
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeResourceManagerConfiguration#get()
	 */
	public String get()
	{
		return getHref();
	}
	/**
	 * Overridden to read "href" attributes.
	 * @see org.apache.uima.impl.resource.metadata.MetaDataObject_impl#buildFromXMLElement(org.w3c.dom.Element, org.apache.uima.util.XMLParser, org.apache.uima.util.XMLParser.ParsingOptions)
	 */
	public void buildFromXMLElement(Element aElement, XMLParser aParser, ParsingOptions aOptions) throws InvalidXMLException
	{
	  setHref(aElement.getAttribute("href"));
	}
	/**
	  * Overridden to handle "href" attributes.
	  * @see org.apache.uima.impl.resource.metadata.MetaDataObject_impl#getXMLAttributes()
	  */
	 protected AttributesImpl getXMLAttributes()
	 {
	   AttributesImpl attrs = super.getXMLAttributes();
	   attrs.addAttribute("","href","href","CDATA",getHref());
	   return attrs;
	 }
	static final private XmlizationInfo XMLIZATION_INFO =
	new XmlizationInfo("resourceManagerConfiguration",
		new PropertyXmlInfo[0]);

	 
	protected XmlizationInfo getXmlizationInfo()
	{
	  return XMLIZATION_INFO;
	}

	/**
	 * @return
	 */
	public String getHref()
	{
		return href;
	}

	/**
	 * @param string
	 */
	public void setHref(String string)
	{
		href = string;
	}

}
