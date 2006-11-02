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

package org.apache.uima.impl.analysis_engine.service.vinci.util;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import org.apache.vinci.transport.Attributes;
import org.apache.vinci.transport.FrameComponent;
import org.apache.vinci.transport.FrameLeaf;
import org.apache.vinci.transport.KeyValuePair;
import org.apache.vinci.transport.VinciFrame;
import org.apache.vinci.transport.document.AFrame;
import org.apache.vinci.transport.document.AFrameLeaf;

/**
 * Takes a Vinci frame and generates SAX events that correspond to the data 
 * in the frame.
 * 
 * 
 */
public class VinciSaxParser
{
  public void setContentHandler(ContentHandler aHandler)
  {
    mHandler = aHandler;
  }
  
  public void parse(VinciFrame aFrame)
    throws SAXException
  {
    parse(aFrame,true);
  }
  
  public void parse(VinciFrame aFrame, boolean aSendStartAndEndDocEvents)
    throws SAXException
  {
    if (aSendStartAndEndDocEvents)
    {
      mHandler.startDocument();
    }  
    
    _parse(aFrame);

    if (aSendStartAndEndDocEvents)
    {
      mHandler.endDocument();
    }  
  }
  
  protected void _parse(VinciFrame aFrame)
    throws SAXException
  {
    int count = aFrame.getKeyValuePairCount();

    for (int i = 0; i < count; i++)
    {
      KeyValuePair kvp = aFrame.getKeyValuePair(i);
	  boolean isIndexed = false;
      
      FrameComponent val = kvp.getValue();
      
      //read attributes
      AttributesImpl attrs = new AttributesImpl();

      Attributes vinciAttrs = null;
      if (val instanceof AFrame)
      {
        vinciAttrs =((AFrame)val).getAttributes();
      }
      else if (val instanceof AFrameLeaf)
      {
        vinciAttrs = ((AFrameLeaf)val).getAttributes();
      }
      
      if (vinciAttrs != null)
      {
        for (int j = 0; j < vinciAttrs.getKeyValuePairCount(); j++)
        {
          KeyValuePair attr = vinciAttrs.getKeyValuePair(j);
          String attrName = attr.getKey();
          String attrVal = attr.getValueAsString();
          attrs.addAttribute("",attrName,attrName,"CDATA",attrVal);
		  if (attrName.equals("_indexed")) {
			  isIndexed = true;
		  }
        }
      }

      //Kludge: all annotations returned from Vinci service are "indexed"
      //(but not array elements!)
      if (!isIndexed && !kvp.getKey().equals("i"))
      {
        attrs.addAttribute("","_indexed","_indexed","CDATA","true");
      }  
      
      mHandler.startElement("",kvp.getKey(),kvp.getKey(),attrs);
      if (val instanceof FrameLeaf)
      {
        String leafString = ((FrameLeaf)val).toString();
        mHandler.characters(leafString.toCharArray(), 0 ,leafString.length());
      }
      else if (val instanceof VinciFrame)
      {
        _parse((VinciFrame)val);  
      }
      else
      {
        throw new SAXException("Expected FrameLeaf or VinciFrame, found " + val.getClass());
      }
      mHandler.endElement("",kvp.getKey(),kvp.getKey());
    }
  }
  
  
  private ContentHandler mHandler;
    
}
