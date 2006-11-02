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


import org.w3c.dom.Element;
import org.xml.sax.helpers.AttributesImpl;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;
import org.apache.uima.collection.metadata.CpeComponentDescriptor;
import org.apache.uima.collection.metadata.CpeInclude;
import org.apache.uima.impl.resource.metadata.MetaDataObject_impl;
import org.apache.uima.impl.resource.metadata.PropertyXmlInfo;
import org.apache.uima.impl.resource.metadata.XmlizationInfo;


public class CpeComponentDescriptorImpl extends MetaDataObject_impl implements CpeComponentDescriptor
{
	private CpeInclude include;
	
	public CpeComponentDescriptorImpl() { }
	
	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeComponentDescriptor#setInclude(org.apache.uima.collection.metadata.CpeInclude)
	 */
	public void setInclude(CpeInclude aInclude)
	{
		include = aInclude;
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeComponentDescriptor#getInclude()
	 */
	public CpeInclude getInclude()
	{
		return include;	
	}

	protected XmlizationInfo getXmlizationInfo()
	{
	  return XMLIZATION_INFO;
	}
  
  
	static final private XmlizationInfo XMLIZATION_INFO =
	  new XmlizationInfo("descriptor",
		new PropertyXmlInfo[]{
					new PropertyXmlInfo("include",null),
		});

}
