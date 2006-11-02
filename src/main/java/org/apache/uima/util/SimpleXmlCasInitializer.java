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

package org.apache.uima.util;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CasInitializerDescription;
import org.apache.uima.collection.CasInitializer_ImplBase;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * A simple example of a CAS Initializer that operates on XML documents.
 * The CollectionReader must pass an InputStream to this class's
 * {@link #initializeCas(Object, CAS)} method, and this InputStream must
 * point to a valid XML document.
 * <p>
 * This CAS Initializer has one optional parameter, <code>XmlTagContainingText</code>.
 * If a value is specified for this parameter, then only the portion of the XML
 * document within the named tag will be put into the CAS.
 * </p>
 * A SAX Parser is used to parse the document from the InputStream.  This should
 * correctly handle any encoding that is specified properly in the XML header.
 */
public class SimpleXmlCasInitializer extends CasInitializer_ImplBase
{
  private String mXmlTagContainingText;
  private SAXParserFactory mParserFactory;
  
	/**
	 * Name of optional configuration parameter that contains the name of an XML
	 * tag that appears in the input file.  Only text that falls within this XML
	 * tag will be considered part of the "document" that it is added to the CAS by
	 * this CAS Initializer.  If not specified, the entire file will be considered
	 * the document.
	 */
	public static final String PARAM_XMLTAG = "XmlTagContainingText";
	
	/* (non-Javadoc)
	 * @see org.apache.uima.collection.CasInitializer_ImplBase#initialize()
	 */
	public void initialize() throws ResourceInitializationException
	{
		super.initialize();
		mParserFactory = SAXParserFactory.newInstance();
		//Get config param setting
		mXmlTagContainingText = (String)getUimaContext().getConfigParameterValue(PARAM_XMLTAG);
	}


  /* (non-Javadoc)
   * @see org.apache.uima.collection.CasInitializer#initializeCas(java.lang.Object, org.apache.uima.cas.CAS)
   */
  public void initializeCas(Object aObj, CAS aCAS)
    throws CollectionException, IOException
  {
  	//build SAX InputSource object from InputStream supplied by the CollectionReader
    InputSource inputSource;
    if (aObj instanceof InputStream)
    {
    	inputSource = new InputSource((InputStream)aObj);
    }
		else
		{
			throw new CollectionException(CollectionException.INCORRECT_INPUT_TO_CAS_INITIALIZER,
				new Object[]{InputStream.class.getName(), aObj.getClass().getName()});
		}
    //create SAX ContentHandler that populates CAS
    SaxHandler handler = new SaxHandler(aCAS);
    //parse
    try
    {
      SAXParser parser = mParserFactory.newSAXParser();      
      XMLReader reader = parser.getXMLReader();
      reader.setContentHandler(handler);
      reader.parse(inputSource);  
    } 
    catch (Exception e)
    {
      throw new CollectionException(e);
    }  
  }
  

	/**
	 * Parses and returns the descriptor for this CAS Initializer.  The
	 * descriptor is stored in the uima.jar file and located using the
	 * ClassLoader.
	 * 
	 * @return an object containing all of the information parsed from the
	 *   descriptor.
	 * 
	 * @throws InvalidXMLException if the descriptor is invalid or missing
	 */
	public static CasInitializerDescription getDescription()
		throws InvalidXMLException
	{
		InputStream descStream = SimpleXmlCasInitializer.class.
			 getResourceAsStream("SimpleXmlCasInitializer.xml");
		return UIMAFramework.getXMLParser().parseCasInitializerDescription(
			new XMLInputSource(descStream, null));  
	}
	
  /**
   * Inner class implementing the SAX ContentHandler interface.
   * Parses the XML to extract the text from the specified element
   *
   */
  private class SaxHandler extends DefaultHandler
  {
  	private CAS mCas;
  	private boolean mInsideTextTag;
  	private StringBuffer mBuf;
  	
  	SaxHandler(CAS aCas)
  	{
  		mCas = aCas;
  		mInsideTextTag = (mXmlTagContainingText == null);
  		mBuf = new StringBuffer();
  	}
  	
		/* (non-Javadoc)
		 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		public void startElement(
			String uri,
			String localName,
			String qName,
			Attributes attributes)
			throws SAXException
		{
			if(mInsideTextTag)
			{
				mBuf.append('<').append(qName).append('>');
			}
			else if (qName.equalsIgnoreCase(mXmlTagContainingText))
			{
				mInsideTextTag = true;
			}
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
		 */
		public void endElement(String uri, String localName, String qName)
			throws SAXException
		{
			if (qName.equalsIgnoreCase(mXmlTagContainingText))
			{
				mInsideTextTag = false;
			}		
			if(mInsideTextTag)
			{
				mBuf.append("</").append(qName).append('>');
			}
		}
		
    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length)
      throws SAXException
    {
    	if(mInsideTextTag)
    	{
    		mBuf.append(ch,start,length);
    	}
    }

		/* (non-Javadoc)
		 * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
		 */
		public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException
		{
			if(mInsideTextTag)
			{
				mBuf.append(ch,start,length);
			}
		}
	
	  /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    public void endDocument() throws SAXException
    {
    	mCas.setDocumentText(mBuf.toString());
    }

  }
}
