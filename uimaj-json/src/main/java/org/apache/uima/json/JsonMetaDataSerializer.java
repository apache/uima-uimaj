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

package org.apache.uima.json;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.apache.uima.json.impl.JsonContentHandlerJacksonWrapper;
import org.apache.uima.json.impl.MetaDataObjectSerializer_json;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl.SerialContext;
import org.apache.uima.util.XMLizable;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

public class JsonMetaDataSerializer {

  /**
   * Serialize to a writer
   * @param object - the instance of an XMLizable to serialize
   * @param aWriter - where the output goes
   * @throws SAXException - wrapping an IOException, probably
   */
  public static void toJSON(XMLizable object, Writer aWriter) throws SAXException {
    toJSON(object, aWriter, false);
  }

  /**
   * Serialize to a writer
   * @param object - the instance of an XMLizable to serialize
   * @param aWriter - where the output goes
   * @param isFormattedOutput true for pretty printing
   * @throws SAXException - wrapping an IOException, probably
   */
  public static void toJSON(XMLizable object, Writer aWriter, boolean isFormattedOutput) throws SAXException {
    JsonGenerator jg;
    try {
      jg = new JsonFactory().createGenerator(aWriter);
    } catch (IOException e) {
      throw new SAXException(e);
    }
    
    toJSON(object, jg, isFormattedOutput);
  }
 
  /**
   * Serialize use a specific instance of a JsonGenerator which encapsulates where the output goes
   * @param object - the instance of an XMLizable to serialize
   * @param jg the generator to use
   * @param isFormattedOutput true for pretty printing
   * @throws SAXException - wrapping an IOException, probably
   */
  public static void toJSON(XMLizable object, JsonGenerator jg, boolean isFormattedOutput) throws SAXException {
    JsonContentHandlerJacksonWrapper jch;
    SerialContext sc = MetaDataObject_impl.serialContext.get();
    
    boolean setContext = false;
    if (null == sc) {
      jch = new JsonContentHandlerJacksonWrapper(jg, isFormattedOutput);
      sc = new SerialContext(jch, new MetaDataObjectSerializer_json(jch));
      MetaDataObject_impl.serialContext.set(sc);
      setContext = true;
    } else {
      jch = (JsonContentHandlerJacksonWrapper) sc.ch;
    }
    
    try {
      jch.withoutNl();
    
      object.toXML(jch);
    
      jg.flush();
    } catch (IOException e) {
      throw new SAXException(e);
    } finally {
      if (setContext) {
        MetaDataObject_impl.serialContext.remove();
      }
    }
  }

  /**
   * Writes out this object's JSON representation.
   * @param object - the instance of an XMLizable to serialize
   * @param aOutputStream
   *          an OutputStream to which the JSON will be written
   * @throws SAXException - wrapping an IOException, probably
   */
  public static void toJSON(XMLizable object, OutputStream aOutputStream) throws SAXException {
    toJSON(object, aOutputStream, false);
  }

  /**
   * 
   * @param object - the instance of an XMLizable to serialize
   * @param aOutputStream
   *          an OutputStream to which the JSON will be written
   * @param isFormattedOutput true for pretty printing
   * @throws SAXException - wrapping an IOException, probably
   */
  public static void toJSON(XMLizable object, OutputStream aOutputStream, boolean isFormattedOutput) throws SAXException {
    try {
      JsonGenerator jg = new JsonFactory().createGenerator(aOutputStream);
      toJSON(object, jg, isFormattedOutput);
    } catch (IOException e) {
      throw new SAXException(e);
    }
  }

  /**
   * 
   * @param object - the instance of an XMLizable to serialize
   * @param file where the output goes
   * @throws SAXException - wrapping an IOException, probably
   */
  public static void toJSON(XMLizable object, File file) throws SAXException {
    toJSON(object, file, false);
  }

  /**
   * 
   * @param object - the instance of an XMLizable to serialize
   * @param file where the output goes
   * @param isFormattedOutput true for pretty printing
   * @throws SAXException - wrapping an IOException, probably
   */
  public static void toJSON(XMLizable object, File file, boolean isFormattedOutput) throws SAXException {
    try {
      JsonGenerator jg = new JsonFactory().createGenerator(file, JsonEncoding.UTF8);
      toJSON(object, jg, isFormattedOutput);
    } catch (IOException e) {
      throw new SAXException(e);
    }
  }

}
