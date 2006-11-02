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

import org.apache.uima.collection.metadata.CasProcessorFilter;
import org.apache.uima.impl.resource.metadata.MetaDataObject_impl;
import org.apache.uima.impl.resource.metadata.PropertyXmlInfo;
import org.apache.uima.impl.resource.metadata.XmlizationInfo;
import org.apache.uima.impl.util.XMLUtils;
import org.xml.sax.ContentHandler;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;

public class CasProcessorFilterImpl extends MetaDataObject_impl implements CasProcessorFilter
{
	private String filter;
	
	public CasProcessorFilterImpl() { }
	

	public void setFilterString( String aFilterString)
	{
		filter = aFilterString;
	}
	public String getFilterString()
	{
		return filter;
	}
	/**
	 * Overridden to read "name" and "value" attributes.
	 * @see org.apache.uima.impl.resource.metadata.MetaDataObject_impl#buildFromXMLElement(org.w3c.dom.Element, org.apache.uima.util.XMLParser, org.apache.uima.util.XMLParser.ParsingOptions)
	 */
	public void buildFromXMLElement(Element aElement, XMLParser aParser, ParsingOptions aOptions) throws InvalidXMLException
	{
	  filter = XMLUtils.getText(aElement);
	  
	}

	public void toXML(ContentHandler aContentHandler,
		boolean aWriteDefaultNamespaceAttribute)
		throws SAXException
	  {
		XmlizationInfo inf = getXmlizationInfo();
    
		//write the element's start tag
		//get attributes (can be provided by subclasses)
		AttributesImpl attrs = getXMLAttributes();
		//add default namespace attr if desired
		if (aWriteDefaultNamespaceAttribute)
		{
		  if (inf.namespace != null)
		  {
			attrs.addAttribute("","xmlns","xmlns",null,inf.namespace);
		  }        
		}
    
		//start element
		aContentHandler.startElement(inf.namespace, inf.elementTagName, 
			inf.elementTagName, attrs);
        
		aContentHandler.characters(filter.toCharArray(),0, filter.length());
		   
		//end element
		aContentHandler.endElement(inf.namespace, inf.elementTagName, 
			inf.elementTagName);
	  }	

	protected XmlizationInfo getXmlizationInfo()
	{
	  return XMLIZATION_INFO;
	}
  
  
	static final private XmlizationInfo XMLIZATION_INFO =
	new XmlizationInfo("filter",
		new PropertyXmlInfo[0]);	
	/**
	 * @return
	 */
	public String getFilter()
	{
		return filter;
	}

	/**
	 * @param string
	 */
	public void setFilter(String string)
	{
		filter = string;
	}

}
