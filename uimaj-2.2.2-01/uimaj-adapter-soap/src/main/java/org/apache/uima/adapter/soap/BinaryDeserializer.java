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

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;

import org.apache.axis.AxisFault;
import org.apache.axis.attachments.AttachmentUtils;
import org.apache.axis.encoding.Base64;
import org.apache.axis.encoding.DeserializationContext;
import org.apache.axis.encoding.DeserializerImpl;
import org.apache.axis.message.SOAPBodyElement;
import org.apache.axis.soap.SOAPConstants;
import org.apache.axis.utils.Messages;
import org.apache.uima.internal.util.SerializationUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * An Axis deserializer for {@link Serializable} objects. The serialized bytes are Base-64 encoded
 * for transport via SOAP.
 * 
 * 
 */
public class BinaryDeserializer extends DeserializerImpl {

  private static final long serialVersionUID = 1351090281481173811L;

  private StringBuffer buf = new StringBuffer();

  public void startElement(String namespace, String localName, String prefix,
          Attributes attributes, DeserializationContext context) throws SAXException {

    // System.out.println("startElement(" + namespace + "," + localName + "," + prefix + ")");
    if (!context.isDoneParsing()) {
      if (myElement == null) {
        try {
          myElement = makeNewElement(namespace, localName, prefix, attributes, context);
        } catch (AxisFault axisFault) {
          throw new SAXException(axisFault);
        }
        context.pushNewElement(myElement);
      }
    }

    SOAPConstants soapConstants = context.getMessageContext().getSOAPConstants();

    QName type = context.getTypeFromAttributes(namespace, localName, attributes);
    if (log.isDebugEnabled()) {
      log.debug(Messages.getMessage("gotType00", "Deser", "" + type));
    }

    String href = attributes.getValue(soapConstants.getAttrHref());
    // System.out.println(soapConstants.getAttrHref() + " = " + href);
    if (href != null) {
      Object ref = context.getObjectByRef(href);
      // System.out.println(ref.getClass().getName());
      // System.out.println(ref);
      if (ref instanceof SOAPBodyElement) // multiref, deref. again
      {
        SOAPBodyElement bodyElem = (SOAPBodyElement) ref;
        href = bodyElem.getAttributeValue(soapConstants.getAttrHref());
        ref = context.getObjectByRef(href);
        // System.out.println(ref.getClass().getName());
        // System.out.println(ref);
      }
      if (ref instanceof org.apache.axis.Part) {
        try {
          DataHandler dataHandler = AttachmentUtils
                  .getActivationDataHandler((org.apache.axis.Part) ref);
          Object content = dataHandler.getContent();
          // System.out.println(content.getClass().getName());
          ObjectInputStream objStream = new ObjectInputStream((InputStream) content);
          try {
            setValue(objStream.readObject());
          } finally {
            objStream.close();
          }
        } catch (org.apache.axis.AxisFault e) {
          throw new SAXException(e.getMessage());
        } catch (java.io.IOException e) {
          throw new SAXException(e.getMessage());
        } catch (ClassNotFoundException e) {
          throw new SAXException(e.getMessage());
        }
      }
    }

    // if we didn't set a value, call default implementation
    if (getValue() == null) {
      super.startElement(namespace, localName, prefix, attributes, context);
    }
    // buf.setLength(0);
  }

  /**
   * @see org.apache.axis.message.SOAPHandler#onStartElement(java.lang.String, java.lang.String,
   *      java.lang.String, org.xml.sax.Attributes, org.apache.axis.encoding.DeserializationContext)
   */
  public void onStartElement(String namespace, String localName, String prefix,
          Attributes attributes, DeserializationContext context) throws SAXException {
    buf.setLength(0);
  }

  /**
   * @see org.apache.axis.encoding.Deserializer#onEndElement(java.lang.String, java.lang.String,
   *      org.apache.axis.encoding.DeserializationContext)
   */
  public void onEndElement(String arg0, String arg1, DeserializationContext arg2)
          throws SAXException {
    try {
      // System.out.println("onEndElement(" + arg0 + "," + arg1 + ")");

      // deserialize (if not done already via attachments)
      if (this.getValue() == null) {
        // System.out.println("deserializing - no attachments found"); //DEBUG
        String base64str = buf.toString();
        // System.out.println("Base64str: " + base64str);
        if (base64str.length() > 0) {
          byte[] bytes = Base64.decode(base64str);
          setValue(SerializationUtils.deserialize(bytes));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new SAXException(e);
    }
  }

  /**
   * @see org.xml.sax.ContentHandler#characters(char[], int, int)
   */
  public void characters(char[] ch, int start, int length) throws SAXException {
    // System.out.println("characters(" + new String(ch,start,length) + ")");
    buf.append(ch, start, length);
  }

}
