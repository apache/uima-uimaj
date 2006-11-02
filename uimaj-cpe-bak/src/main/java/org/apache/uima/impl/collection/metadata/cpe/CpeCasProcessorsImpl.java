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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;

import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.collection.metadata.CpeCasProcessors;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.collection.metadata.CpeIntegratedCasProcessor;
import org.apache.uima.collection.metadata.CpeLocalCasProcessor;
import org.apache.uima.collection.metadata.CpeRemoteCasProcessor;
import org.apache.uima.impl.collection.cpm.Constants;
import org.apache.uima.impl.collection.cpm.utils.CPMUtils;
import org.apache.uima.impl.collection.cpm.utils.CpmLocalizedMessage;
import org.apache.uima.impl.resource.metadata.MetaDataObject_impl;
import org.apache.uima.impl.resource.metadata.PropertyXmlInfo;
import org.apache.uima.impl.resource.metadata.XmlizationInfo;

public class CpeCasProcessorsImpl extends MetaDataObject_impl implements CpeCasProcessors
{
	private static final int DEFAULT_POOL_SIZE = 1;

	private ArrayList casProcessors = new ArrayList();
	;

	private boolean dropCasOnException;

	private int casPoolSize=DEFAULT_POOL_SIZE;

	private int processingUnitThreadCount=1;

	private int inputQueueSize;

	private int outputQueueSize;

	public CpeCasProcessorsImpl()
	{
		super();
	};

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeCasProcessors#setOutputQueueSize(int)
	 */
	public void setOutputQueueSize(int aOutputQueueSize) throws CpeDescriptorException
	{
		outputQueueSize = aOutputQueueSize;
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeCasProcessors#getOutputQueueSize()
	 */
	public int getOutputQueueSize()
	{
		return outputQueueSize;
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeCasProcessors#setInputQueueSize(int)
	 */
	public void setInputQueueSize(int aInputQueueSize) throws CpeDescriptorException
	{
		inputQueueSize = aInputQueueSize;
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeCasProcessors#getInputQueueSize()
	 */
	public int getInputQueueSize()
	{
		return inputQueueSize;
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeCasProcessors#setConcurrentPUCount(int)
	 */
	public void setConcurrentPUCount(int aConcurrentPUCount) throws CpeDescriptorException
	{
		processingUnitThreadCount = aConcurrentPUCount;
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeCasProcessors#getConcurrentPUCount()
	 */
	public int getConcurrentPUCount()
	{
		return processingUnitThreadCount;
	}
	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeCasProcessors#addCpeCasProcessor(org.apache.uima.collection.metadata.CpeCasProcessor, int)
	 */
	public void addCpeCasProcessor(CpeCasProcessor aCasProcessor, int aInsertPosition) throws CpeDescriptorException
	{
		casProcessors.add(aInsertPosition, aCasProcessor);
	}
	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeCasProcessors#addCpeCasProcessor(org.apache.uima.collection.metadata.CpeCasProcessor, int)
	 */
	public void addCpeCasProcessor(CpeCasProcessor aCasProcessor) throws CpeDescriptorException
	{
		casProcessors.add(aCasProcessor);
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeCasProcessors#getCpeCasProcessor(int)
	 */
	public CpeCasProcessor getCpeCasProcessor(int aPosition) throws CpeDescriptorException
	{
		if (aPosition <= casProcessors.size())
		{
			return (CpeCasProcessor) casProcessors.get(aPosition);
		}
		
		throw new CpeDescriptorException( CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
				"UIMA_CPM_EXP_invalid_array_index__WARNING",new Object[] {Thread.currentThread().getName(),"CpeCasProcessor"} ));
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeCasProcessors#getAllCpeCasProcessors()
	 */
	public CpeCasProcessor[] getAllCpeCasProcessors() throws CpeDescriptorException
	{
		CpeCasProcessor[] processors = new CpeCasProcessor[casProcessors.size()];
		casProcessors.toArray(processors);
		return processors;
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeCasProcessors#getAllCpeCasProcessors()
	 */
	public void setAllCpeCasProcessors(CpeCasProcessor[] aCpeProcessors) throws CpeDescriptorException
	{

		for (int i = 0; aCpeProcessors != null && i < aCpeProcessors.length; i++)
		{
			casProcessors.add(aCpeProcessors[i]);
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeCasProcessors#removeCpeCasProcessor(int)
	 */
	public void removeCpeCasProcessor(int aPosition) throws CpeDescriptorException
	{
		if (aPosition <= casProcessors.size())
		{
			casProcessors.remove(aPosition);
		}
			throw new CpeDescriptorException(CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
					"UIMA_CPM_EXP_invalid_array_index__WARNING",new Object[] {Thread.currentThread().getName()}) );
	}

	/**
	 * New API 01/06/2006
	 */
	public CpeCasProcessor[] removeCpeCasProcessor(int aPosition, boolean flag) throws CpeDescriptorException
	{
		if (aPosition <= casProcessors.size())
		{
			casProcessors.remove(aPosition);
			return getAllCpeCasProcessors();
		}
			throw new CpeDescriptorException(CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
					"UIMA_CPM_EXP_invalid_array_index__WARNING",new Object[] {Thread.currentThread().getName()}) );
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeCasProcessors#removeAllCpeCasProcessors()
	 */
	public void removeAllCpeCasProcessors() throws CpeDescriptorException
	{
		casProcessors.clear();
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeCasProcessors#setOutputQueueSize(int)
	 */
	public void setPoolSize(int aPoolSize) throws CpeDescriptorException
	{
		casPoolSize = aPoolSize;
	}
	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeCasProcessors#setOutputQueueSize(int)
	 */
	public int getPoolSize() throws CpeDescriptorException
	{
		return casPoolSize;
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeCasProcessors#setOutputQueueSize(int)
	 */
	public void setDropCasOnException(boolean aDropCasOnException) throws CpeDescriptorException
	{
		dropCasOnException = aDropCasOnException;
	}
	/* (non-Javadoc)
	 * @see org.apache.uima.collection.metadata.CpeCasProcessors#setOutputQueueSize(int)
	 */
	public boolean getDropCasOnException()
	{
		return dropCasOnException;
	}
	/**
	 * Overridden to read Cas Processor attributes.
	 * @see org.apache.uima.impl.resource.metadata.MetaDataObject_impl#buildFromXMLElement(org.w3c.dom.Element, org.apache.uima.util.XMLParser, org.apache.uima.util.XMLParser.ParsingOptions)
	 */
	public void buildFromXMLElement(Element aElement, XMLParser aParser, ParsingOptions aOptions) throws InvalidXMLException
	{
		try
		{
			setDropCasOnException(Boolean.valueOf(aElement.getAttribute("dropCasOnException")).booleanValue());
		}
		catch(Exception e)
		{
			throw new InvalidXMLException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
					"UIMA_CPM_EXP_missing_attribute_from_xml_element__WARNING",new Object[] {Thread.currentThread().getName(),"casProcessors","dropCasOnException","casProcessors"});
			
		}
		String cps = aElement.getAttribute("casPoolSize");
		if ( cps != null && cps.trim().length() > 0 )
		{
			try
			{
				setPoolSize(Integer.parseInt(cps));
			}
			catch(Exception e)
			{
				throw new InvalidXMLException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
						"UIMA_CPM_EXP_missing_attribute_from_xml_element__WARNING",new Object[] {Thread.currentThread().getName(),"casProcessors","casPoolSize","casProcessors"});
				
			}
		}
		if ( aElement.getAttribute("processingUnitThreadCount") != null)
		{
//			String tc = aElement.getAttribute("processingUnitThreadCount");
			try
			{
				setConcurrentPUCount(Integer.parseInt(aElement.getAttribute("processingUnitThreadCount")));
			}
			catch(Exception e)
			{
				throw new InvalidXMLException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
						"UIMA_CPM_EXP_missing_attribute_from_xml_element__WARNING",new Object[] {Thread.currentThread().getName(),"casProcessors","processingUnitThreadCount","casProcessors"});
				
			}
		}
		else
		{
			throw new InvalidXMLException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
							"UIMA_CPM_EXP_missing_attribute_from_xml_element__WARNING",new Object[] {Thread.currentThread().getName(),"casProcessors","processingUnitThreadCount","casProcessors"}) ;
		}
		String field="casPoolSize";
			//	populate inputQueueSize and outputQueueSize ONLY if casPoolSize is not defined.
			//	Both of these attributes have been deprecated and should not be used
		try
		{
			if ( getPoolSize() == 0 )
			{
				field = "inputQueueSize";
				String iqs = aElement.getAttribute("inputQueueSize");
				if (iqs != null && iqs.length() > 0)
				{
					setInputQueueSize(Integer.parseInt(iqs));
				}
				field = "outputQueueSize";
				String oqs = aElement.getAttribute("outputQueueSize");
				if (oqs != null && oqs.length() > 0)
				{
					setOutputQueueSize(Integer.parseInt(oqs));
				}
			}
		}
		catch(Exception e)
		{
			throw new InvalidXMLException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
					"UIMA_CPM_EXP_missing_attribute_from_xml_element__WARNING",new Object[] {Thread.currentThread().getName(),"casProcessors","field","casProcessors"});
			
		}

		//	Now handle all children of the <casProcessors> element. We expect to find
		//	<casProcessor> elements as children. This code instantiates different types
		//	of Cas Processor objects based on their types. Three types of Cas Processors
		//	can be instantiated here: 
		//	   - CpeIntegratedCasProcessorImpl
		//	   - CpeRemoteCasProcessorImpl
		//	   - CpeLocalCasProcessorImpl
		//  Each one of the above inherits from CasProcessorCpeObject
		NodeList nodes = aElement.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++)
		{
			Node node = nodes.item(i);
			if (node instanceof Element)
			{
				CasProcessorCpeObject cp = null;
				if (node.getNodeName().equals("casProcessor"))
				{
					NamedNodeMap lMap = node.getAttributes();
					Node depNode = lMap.getNamedItem("deployment");

					if (depNode != null)
					{
						//	Based on deployment mode instantiate different types of Cas Processor
						String lDeployMode = depNode.getNodeValue();
						if ("integrated".equals(lDeployMode))
						{
							cp = new CpeIntegratedCasProcessorImpl();
						}
						else if ("remote".equals(lDeployMode))
						{
							cp = new CpeRemoteCasProcessorImpl();
						}
						else if ("local".equals(lDeployMode))
						{
							cp = new CpeLocalCasProcessorImpl(false); // Dont initialize with defaults
						}
					}
				}
				//	Continue to parse
				cp.buildFromXMLElement((Element) node, aParser, aOptions);
				
				//	Aggregate Cas Processors
				casProcessors.add(cp);
			}
		}
	}

	public void toXML(ContentHandler aContentHandler, boolean aWriteDefaultNamespaceAttribute) throws SAXException
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
				attrs.addAttribute("", "xmlns", "xmlns", null, inf.namespace);
			}
		}

		//start element
		aContentHandler.startElement(inf.namespace, inf.elementTagName, inf.elementTagName, attrs);

		//write child elements
		for (int i = 0; i < casProcessors.size(); i++)
		{
			((CpeCasProcessor) casProcessors.get(i)).toXML(aContentHandler, aWriteDefaultNamespaceAttribute);
		}

		//end element
		aContentHandler.endElement(inf.namespace, inf.elementTagName, inf.elementTagName);
	}
	/**
	 * Overridden to handle Cas Processor attributes.
	 * @see org.apache.uima.impl.resource.metadata.MetaDataObject_impl#getXMLAttributes()
	 */
	protected AttributesImpl getXMLAttributes()
	{
		AttributesImpl attrs = super.getXMLAttributes();
		if ( isDropCasOnException() == true)
		{
			attrs.addAttribute("", "dropCasOnException", "dropCasOnException", "CDATA", String.valueOf(isDropCasOnException()));
		}
		attrs.addAttribute("", "casPoolSize", "casPoolSize", "CDATA", String.valueOf(getCasPoolSize()));
		attrs.addAttribute("", "processingUnitThreadCount", "processingUnitThreadCount", "CDATA", String.valueOf(getConcurrentPUCount()));
		//	populate inputQueueSize and outputQueueSize ONLY if casPoolSize is not defined.
		//	Both of these attributes have been deprecated and should not be used
		if ( getCasPoolSize() == 0 )
		{
			attrs.addAttribute("", "inputQueueSize", "inputQueueSize", "CDATA", String.valueOf(getInputQueueSize()));
			attrs.addAttribute("", "outputQueueSize", "outputQueueSize", "CDATA", String.valueOf(getOutputQueueSize()));
		}

		return attrs;
	}

	protected XmlizationInfo getXmlizationInfo()
	{
		return XMLIZATION_INFO;
	}
	static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("casProcessors", new PropertyXmlInfo[] { new PropertyXmlInfo("allCpeCasProcessors", null), });

	public int getCasPoolSize()
	{
		//	Indirection needed to handle the exception. xsdbeans based implementation of this API
		//	was throwing it, if there was a problem. With the current approach this exception never
		//	happens. For backwards compatibility the signature of this API hasnt changed.
		try
		{
			return getPoolSize();
		}
		catch (CpeDescriptorException e)
		{
			//
		}
		return 1;
	}
	public boolean isDropCasOnException()
	{
		
		//	Indirection needed to handle the exception. xsdbeans based implementation of this API
		//	was throwing it, if there was a problem. With the current approach this exception never
		//	happens. For backwards compatibility the signature of this API hasnt changed.
		return getDropCasOnException();
//		try
//		{
//			return getDropCasOnException();
//		}
//		catch (CpeDescriptorException e)
//		{
//			//
//		}
//		return true;
	}
}
