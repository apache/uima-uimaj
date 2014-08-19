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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.w3c.dom.Element;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * An interface to be implemented by UIMA classes that can be written as JSON.
 * 
 */
public interface JSONizable {
 
  /**
   * Writes this object's JSON representation as a string. Note that if you want to write the JSON to
   * a file or to a byte stream, it is highly recommended that you use {@link #toJSON(OutputStream)} 
   * instead, as it ensures that output is written in UTF-8 encoding, which is the default encoding 
   * that should be used for XML files.
   * <p>
   * 
   * @param aWriter
   *          a Writer to which the JSON string will be written
   * @throws SAXException 
   */
  public void toJSON(Writer aWriter) throws SAXException;
  
  /**
   * Writes this object's JSON representation using a Writer.
   * <p>
   * 
   * @param aWriter
   *          a Writer to which the JSON string will be written
   * @param isFormattedOutput set to true for prettyprinting, default is false
   * @throws SAXException 
   */
  public void toJSON(Writer aWriter, boolean isFormattedOutput) throws SAXException;

  /**
   * Writes this object's JSON representation to a Writer.
   * 
   * @param aOutputStream
   *          an OutputStream to which the JSON string will be written, in UTF-8 encoding.
   * @throws SAXException 
   */
  public void toJSON(OutputStream aOutputStream) throws SAXException;

  /**
   * Writes this object's JSON representation to an output stream.
   * 
   * @param aOutputStream
   *          an OutputStream to which the JSON string will be written, in UTF-8 encoding.
   * @param isFormattedOutput set to true for prettyprinting, default is false
   * @throws SAXException 
   */
public void toJSON(OutputStream aOutputStream, boolean isFormattedOutput) throws SAXException;

  /**
   * Writes this object's JSON representation to a file in UTF-8 encoding.
   * 
   * @param file
   *          a file to which the JSON string will be written, in UTF-8 encoding.
   * @throws SAXException 
   */
  public void toJSON(File file) throws SAXException;

  /**
   * Writes this object's JSON representation to a file in UTF-8 encoding.
   * 
   * @param file
   *          a file to which the JSON string will be written, in UTF-8 encoding.
   * @param isFormattedOutput set to true for prettyprinting, default is false
   * @throws SAXException 
   */
  public void toJSON(File file, boolean isFormattedOutput) throws SAXException;

}
