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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;

import org.apache.uima.collection.metadata.CasProcessorErrorHandling;
import org.apache.uima.collection.metadata.CasProcessorErrorRateThreshold;
import org.apache.uima.collection.metadata.CasProcessorMaxRestarts;
import org.apache.uima.collection.metadata.CasProcessorTimeout;
import org.apache.uima.impl.resource.metadata.MetaDataObject_impl;
import org.apache.uima.impl.resource.metadata.PropertyXmlInfo;
import org.apache.uima.impl.resource.metadata.XmlizationInfo;

public class CasProcessorErrorHandlingImpl extends MetaDataObject_impl implements CasProcessorErrorHandling
{
	
	private CasProcessorTimeout timeout; 
	private CasProcessorErrorRateThreshold errorRateThrashold;
	private CasProcessorMaxRestarts maxRestarts;

	public CasProcessorErrorHandlingImpl() {}
	
	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CasProcessorErrorHandling#setMaxConsecutiveRestarts(org.apache.uima.collection.metadata.CasProcessorMaxRestarts)
	 */
	public void setMaxConsecutiveRestarts(CasProcessorMaxRestarts aCasPRestarts)
	{
		maxRestarts = aCasPRestarts;
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CasProcessorErrorHandling#getMaxConsecutiveRestarts()
	 */
	public CasProcessorMaxRestarts getMaxConsecutiveRestarts()
	{
		return maxRestarts;
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CasProcessorErrorHandling#setErrorRateThreshold(org.apache.uima.collection.metadata.CasProcessorErrorRateThreshold)
	 */
	public void setErrorRateThreshold(CasProcessorErrorRateThreshold aCasPErrorThreshold)
	{
		errorRateThrashold = aCasPErrorThreshold;
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CasProcessorErrorHandling#getErrorRateThreshold()
	 */
	public CasProcessorErrorRateThreshold getErrorRateThreshold()
	{
		return errorRateThrashold;
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CasProcessorErrorHandling#setTimeout(org.apache.uima.collection.metadata.CasProcessorTimeout)
	 */
	public void setTimeout(CasProcessorTimeout aTimeout)
	{
		timeout = aTimeout;
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CasProcessorErrorHandling#setTimeout()
	 */
	public CasProcessorTimeout getTimeout()
	{
		return timeout;
	}

	protected XmlizationInfo getXmlizationInfo()
	{
	  return XMLIZATION_INFO;
	}
	static final private XmlizationInfo XMLIZATION_INFO =
	  new XmlizationInfo("errorHandling",
	  new PropertyXmlInfo[]{
		new PropertyXmlInfo("errorRateThreshold",null),
		new PropertyXmlInfo("maxConsecutiveRestarts",null),
		new PropertyXmlInfo("timeout",null),
	  });
}
