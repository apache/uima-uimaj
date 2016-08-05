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
import java.io.OutputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.xml.sax.SAXException;

/**
 * This class is an older class with just two static methods which forward to methods in XmiCasSerializer.
 * 
 * Consider using XmiCasSerializer or CasIOUtils instead.
 * 
 * Serializes a CAS to inline XML format. The output format conforms to the XML Metadata Interchange
 * (XMI) format, an OMG standard.
 * <p>
 * For more options, see {@link XmiCasSerializer}.
 */
public abstract class XmlCasSerializer {  // abstract only to prevent instantiation, only has static methods

  /**
   * Serializes a CAS to XMI format and writes it to an output stream.
   * 
   * @param aCAS
   *          CAS to serialize.
   * @param aStream
   *          output stream to which to write the XMI document
   * 
   * @throws SAXException
   *           if a problem occurs during XMI serialization
   * @throws IOException
   *           if an I/O failure occurs
   */
  public static void serialize(CAS aCAS, OutputStream aStream) throws SAXException, IOException {
    XmiCasSerializer.serialize(aCAS, aStream);
  }

  /**
   * Serializes a CAS to XMI format and writes it to an output stream. Allows a TypeSystem to be
   * specified, to which the produced XMI will conform. Any types or features not in the target type
   * system will not be serialized.
   * 
   * @param aCAS
   *          CAS to serialize.
   * @param aTargetTypeSystem
   *          type system to which the produced XMI will conform. Any types or features not in the
   *          target type system will not be serialized.
   *          
   * @param aStream
   *          output stream to which to write the XMI document
   * 
   * @throws SAXException
   *           if a problem occurs during XMI serialization
   * @throws IOException
   *           if an I/O failure occurs
   */
  public static void serialize(CAS aCAS, TypeSystem aTargetTypeSystem, OutputStream aStream)
          throws SAXException, IOException {
    XmiCasSerializer.serialize(aCAS, aTargetTypeSystem, aStream);
  }
}
