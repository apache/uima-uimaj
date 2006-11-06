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

package org.apache.uima.adapter.soap;

import java.io.IOException;
import java.io.Serializable;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;

import org.apache.axis.Constants;
import org.apache.axis.Message;
import org.apache.axis.Part;
import org.apache.axis.attachments.Attachments;
import org.apache.axis.attachments.OctetStream;
import org.apache.axis.attachments.OctetStreamDataSource;
import org.apache.axis.encoding.Base64;
import org.apache.axis.encoding.SerializationContext;
import org.apache.axis.encoding.Serializer;
import org.apache.axis.soap.SOAPConstants;
import org.apache.axis.wsdl.fromJava.Types;
import org.apache.uima.internal.util.SerializationUtils;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * An Axis serializer for {@link Serializable} objects.  The serialized bytes
 * are Base-64 encoded for transport via SOAP.
 * 
 * 
 */
public class BinarySerializer implements Serializer
{
  
      

  /**
   *  Whether attachments should be used to send binary-serialized data
   */
  private boolean mUseAttachments;

  public BinarySerializer()
  {
    this(true);
  }
  
  public BinarySerializer(boolean aUseAttachments)
  {
    mUseAttachments = aUseAttachments;
  }

  /**
   * Serialize an element named name, with the indicated attributes
   * and value.
   * 
   * @param name is the element name
   * @param attributes are the attributes...serializer is free to add more.
   * @param value is the value
   * @param context is the SerializationContext
   */
  public void serialize(QName name, Attributes attributes,
                        Object value, SerializationContext context)
      throws IOException
  {
    if (value instanceof Serializable)
    {    
      byte[] bytes = SerializationUtils.serialize((Serializable)value);
 
      //Should we use an attachment?  Do so if:
      //(a) attachment support exists and mUseAttachments == true and
      //(b) if we are the server, the client sent us an attachment
      //(so we know client wants attachment support as well)]
      Message msg = context.getCurrentMessage();
      Attachments attachments = msg.getAttachmentsImpl();
      boolean useAttachments = (attachments != null) && mUseAttachments;
      if (useAttachments)
      {
        useAttachments = !context.getMessageContext().getPastPivot() ||
            context.getMessageContext().getRequestMessage().getAttachments().hasNext();  
      }
      //if we have attachment support, do this as an attachment
      if (useAttachments)
      {
//        System.out.println("Creating attachment"); //DEBUG
        SOAPConstants soapConstants = context.getMessageContext().getSOAPConstants();
        DataHandler dataHandler = new DataHandler(new OctetStreamDataSource("test", new OctetStream(bytes)));
        Part attachmentPart= attachments.createAttachmentPart(dataHandler);

        AttributesImpl attrs = new AttributesImpl();
        if (attributes != null && 0 < attributes.getLength())
            attrs.setAttributes(attributes); //copy the existing ones.

        int typeIndex=-1;
        if((typeIndex = attrs.getIndex(Constants.URI_DEFAULT_SCHEMA_XSI,
                                "type")) != -1){
            //Found a xsi:type which should not be there for attachments.
            attrs.removeAttribute(typeIndex);
        }

        attrs.addAttribute("", soapConstants.getAttrHref(), soapConstants.getAttrHref(),
                               "CDATA", attachmentPart.getContentIdRef() );
        context.startElement(name,attrs);
        context.endElement();
      }
      else
      {
        //no attachment support - Base64 encode
//        System.out.println("No attachment support"); //DEBUG      
        context.startElement(name,attributes);

        String base64str = Base64.encode(bytes);
        context.writeChars(base64str.toCharArray(),0,base64str.length());
        context.endElement();
     }        

    }
    else
    {
      throw new IOException(value.getClass().getName() + " is not serializable."); 
    }  
  }
  
  public String getMechanismType() { return Constants.AXIS_SAX; }

  /**
   * Old Axis 1.0 style - now obolete.
   */
  public boolean writeSchema(Types types) throws Exception 
  {
      return false;
  }  

  /**
   * @see org.apache.axis.encoding.Serializer#writeSchema(java.lang.Class, org.apache.axis.wsdl.fromJava.Types)
   */
  public Element writeSchema(Class javaType, Types types) throws Exception
  {
    return null;
  }
}
