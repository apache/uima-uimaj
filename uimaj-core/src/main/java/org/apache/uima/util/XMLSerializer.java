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

import java.io.OutputStream;
import java.io.Writer;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

import org.apache.uima.UIMARuntimeException;

/**
 * Utility class that generates XML output from SAX events or DOM nodes.
 */
public class XMLSerializer 
{
  private static final SAXTransformerFactory transformerFactory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
  
  private TransformerHandler mHandler;
  private Transformer mTransformer;
  private Result mResult;
  
  public XMLSerializer()
  {
  	this(true);
  }
  
  public XMLSerializer(boolean isFormattedOutput)
  {
	 try
	 {
	  mHandler = transformerFactory.newTransformerHandler();
	  mTransformer = mHandler.getTransformer();
	  
	  if(isFormattedOutput) {
	     //set default output format
	     mTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
	     mTransformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	     mTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
	     mTransformer.setOutputProperty(OutputKeys.METHOD, "xml");
	  }
      
	 }
	 catch (TransformerConfigurationException e)
	 {
		throw new UIMARuntimeException(e);
	 }
  }
  
  public XMLSerializer(OutputStream aOutputStream)
  {
    this();
    setOutputStream(aOutputStream);
  }

  public XMLSerializer(OutputStream aOutputStream, boolean isFormattedOutput)
  {
	 this(isFormattedOutput);
	 setOutputStream(aOutputStream);
  }

  public XMLSerializer(Writer aWriter)
  {
    this();
    setWriter(aWriter);
  }

  public XMLSerializer(Writer aWriter, boolean isFormattedOutput)
  {
	 this(isFormattedOutput);
	 setWriter(aWriter);
  }
  
  public void setOutputStream(OutputStream aOutputStream)
  {
    mResult = new StreamResult(aOutputStream);
    mHandler.setResult(mResult);      
  }
  
  public void setWriter(Writer aWriter)
  {
    mResult = new StreamResult(aWriter);  
    mHandler.setResult(mResult);       
  }
  
  public ContentHandler getContentHandler()
  {
    return mHandler;
  }
  
  public void serialize(Node node)
  {
    try
    {
      mTransformer.transform(new DOMSource(node), mResult);
    }
    catch (TransformerException e)
    {
      throw new UIMARuntimeException(e);
    }
  }  
  
  public void dom2sax(Node node, ContentHandler handler)
  {
    try
    {
      mTransformer.transform(new DOMSource(node), new SAXResult(handler));
    }
    catch (TransformerException e)
    {
      throw new UIMARuntimeException(e);
    }
  }
  
  public void setOutputProperty(String name, String value) 
  {
	try
	 {
		mTransformer.setOutputProperty(name, value);
	 }
	 catch (IllegalArgumentException e)
	 {
		throw new UIMARuntimeException(e);
	 }	
  }	
}
