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

package org.apache.uima.util.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.internal.util.XIncluder;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.SaxDeserializer;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLSerializer;
import org.apache.uima.util.XMLizable;

/**
 * Reference implementation of {@link SaxDeserializer}.
 * 
 * 
 */
public class SaxDeserializer_impl implements SaxDeserializer
{
  static final String JAXP_SCHEMA_LANGUAGE =
    "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
  static final String W3C_XML_SCHEMA =
    "http://www.w3.org/2001/XMLSchema";
  static final String JAXP_SCHEMA_SOURCE =
    "http://java.sun.com/xml/jaxp/properties/schemaSource";
  
/**
   * Creates a new SAX Deserializer.
   * 
   * @param aUimaXmlParser the UIMA XML parser that knows the XML element to
   *   Java class mappings and which is used to assist in the serialization.
   * @param aNamespaceForSchema XML namespace for elements to be validated 
   *    against XML schema.  If null, no schema will be used.
   * @param aSchemaUrl URL to XML schema that will be used to validate the
   *    XML document.  If null, no schema will be used.
   * @param aOptions option settings
   */
  public SaxDeserializer_impl(XMLParser aUimaXmlParser,
       String aNamespaceForSchema, URL aSchemaUrl, XMLParser.ParsingOptions aOptions)
  {
    mUimaXmlParser = aUimaXmlParser;
    mOptions = aOptions;
    
    //are we doing schema validation?
    mValidate = (aNamespaceForSchema != null && aSchemaUrl != null);

    //Setup XInclude Transformer    
    try
    {
      if (aOptions.expandXIncludes)
      {
        mXIncludeTransformerHandler = XIncluder.newXIncludeHandler();    
      }
      else
      {
        //use identity transformer
        mXIncludeTransformerHandler =  transformerFactory.newTransformerHandler();
      }
      
      //if no validation, go straight to DOM.  Otherwise we have to go back
      //to string so we can do validation.
      if (!mValidate)
      {
        mDOMResult = new DOMResult();
        mXIncludeTransformerHandler.setResult(mDOMResult); 
      }
      else
      {
        //create new ByteArrayOutputStream to which we will write the parsed XML
        mBytes = new ByteArrayOutputStream();    
        // Set up a Serializer to serialize the Result to a byte stream
        XMLSerializer serializer = new XMLSerializer(mBytes);
        // The Serializer functions as a SAX ContentHandler.
        Result result =  new SAXResult(serializer.getContentHandler());
        mXIncludeTransformerHandler.setResult(result);

        //Create DOM parser that is used to parse the string produced by the
        //XIncluder 
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        //attempt to enable schema validation
        try
        {
          factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
          factory.setAttribute(JAXP_SCHEMA_SOURCE, aSchemaUrl.toString());
          factory.setValidating(true);
        }
        catch(Exception e)
        {
          UIMAFramework.getLogger().log(Level.INFO, 
              "The installed XML Parser does not support schema validation.  No validation will occur.");
          factory.setValidating(false);
        }
        mDocumentBuilder = factory.newDocumentBuilder();
        mErrorHandler =  new ParseErrorHandler();
        mDocumentBuilder.setErrorHandler(mErrorHandler);
      }  
    }
    catch(ParserConfigurationException e)
    {
      throw new UIMARuntimeException(e);  
    } 
    catch (SecurityException e)
    {
      throw new UIMARuntimeException(e);  
    }
    catch (TransformerConfigurationException e)
    {
      throw new UIMARuntimeException(e);  
    }

  }

  /**
   * @see org.apache.uima.util.SaxDeserializer#getObject()
   */
  public XMLizable getObject() throws InvalidXMLException
  {
    Node rootDomNode;
    
    if (mValidate)
    {
      //parese intermediate data from byte array
      InputStream in = new ByteArrayInputStream(mBytes.toByteArray());

      //parse it with DOM parser
      try
      {
        InputSource input = new InputSource(in);
        rootDomNode = mDocumentBuilder.parse(input).getDocumentElement();
      }
      catch(IOException e)
      {
        throw new InvalidXMLException(e);        
      }
      catch(SAXException e)
      {
        throw new InvalidXMLException(e);        
      }      
  
      if (mErrorHandler.getException() != null)
      {
        throw new InvalidXMLException(mErrorHandler.getException());
      }    
    }
    else
    {
      //should have already parsed to DOM
      rootDomNode = ((Document)mDOMResult.getNode()).getDocumentElement();
    }
    
    //build the object
    XMLizable result = mUimaXmlParser.buildObject((Element)rootDomNode, mOptions);
    
    //clear state to prepare for another parse
    if (mValidate)
    {
      mBytes = new ByteArrayOutputStream();
      mErrorHandler.clear(); 
    }
    else
    {
      mDOMResult = new DOMResult();  
      mXIncludeTransformerHandler.setResult(mDOMResult); 
    }

    return result;
  }
  
  /**
   * @see org.xml.sax.ContentHandler#characters(char[], int, int)
   */
  public void characters(char[] ch, int start, int length) throws SAXException
  {
    //System.out.println("SaxDeserializer_impl::characters");
    mXIncludeTransformerHandler.characters(ch,start,length);
  }

  /**
   * @see org.xml.sax.ContentHandler#endDocument()
   */
  public void endDocument() throws SAXException
  {
    //System.out.println("SaxDeserializer_impl::endDocument");
    mXIncludeTransformerHandler.endDocument();
  }

  /**
   * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
   */
  public void endElement(String namespaceURI, String localName, String qName)
    throws SAXException
  {
    //System.out.println("SaxDeserializer_impl::endElement");
    mXIncludeTransformerHandler.endElement(namespaceURI, localName, qName);
  }

  /**
   * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
   */
  public void endPrefixMapping(String prefix) throws SAXException
  {
    //System.out.println("SaxDeserializer_impl::endPrefixMapping");
    mXIncludeTransformerHandler.endPrefixMapping(prefix);
  }

  /**
   * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
   */
  public void ignorableWhitespace(char[] ch, int start, int length)
    throws SAXException
  {
    mXIncludeTransformerHandler.ignorableWhitespace(ch,start,length);
  }

  /**
   * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
   */
  public void processingInstruction(String target, String data)
    throws SAXException
  {
    //System.out.println("SaxDeserializer_impl::processingInstruction");
    mXIncludeTransformerHandler.processingInstruction(target,data);
  }

  /**
   * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
   */
  public void setDocumentLocator(Locator locator)
  {
    //System.out.println("SaxDeserializer_impl::setDocumentLocator");
    mXIncludeTransformerHandler.setDocumentLocator(locator);
  }

  /**
   * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
   */
  public void skippedEntity(String name) throws SAXException
  {
    mXIncludeTransformerHandler.skippedEntity(name);    
  }

  /**
   * @see org.xml.sax.ContentHandler#startDocument()
   */
  public void startDocument() throws SAXException
  {
    //System.out.println("SaxDeserializer_impl::startDocument");
    mXIncludeTransformerHandler.startDocument();
  }

  /**
   * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
   */
  public void startElement(String namespaceURI, String localName,
    String qName, Attributes atts)
    throws SAXException  
  {
    //System.out.println("SaxDeserializer_impl::startElement("+namespaceURI+","+localName+","+qName+","+atts+")");
    mXIncludeTransformerHandler.startElement(namespaceURI, localName, qName, atts);
  }

  /**
   * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
   */
  public void startPrefixMapping(String prefix, String uri) throws SAXException
  {
    //System.out.println("SaxDeserializer_impl::startPrefixMapping("+prefix+","+uri+")");
    mXIncludeTransformerHandler.startPrefixMapping(prefix,uri);
  }
       
   private static final SAXTransformerFactory transformerFactory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();

   private boolean mValidate;     
   private DocumentBuilder mDocumentBuilder;
   private DOMResult mDOMResult;
   private ByteArrayOutputStream mBytes;
   private TransformerHandler mXIncludeTransformerHandler;
   private XMLParser mUimaXmlParser;
   private XMLParser.ParsingOptions mOptions;
    
   //private DomParserWrapper mDomParser;
   private ParseErrorHandler mErrorHandler;

  
  /**
   * Error handler for XML parsing.  Stores first error in <code>exception</code>
   * field for later retrieval.
   */
   static class ParseErrorHandler extends DefaultHandler
   {
     private SAXParseException mException = null;
    
     public void error(SAXParseException aError)
     {
       if (mException == null)
         mException = aError;
     }

     public void fatalError(SAXParseException aError)
     {
       if (mException == null)
         mException = aError;
     }
    
     public void warning(SAXParseException aWarning)
     {
       System.err.println("XML Warning: " + aWarning.getMessage());
     }
    
     public SAXParseException getException()
     {
       return mException;
     }
     
     public void clear()
     {
       mException = null;
     }
   }      
}
