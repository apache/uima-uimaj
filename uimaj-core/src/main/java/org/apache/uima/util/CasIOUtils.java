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

import static org.apache.uima.cas.impl.Serialization.serializeCAS;
import static org.apache.uima.cas.impl.Serialization.serializeCASMgr;
import static org.apache.uima.cas.impl.Serialization.serializeWithCompression;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.cas.impl.CommonSerDes;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.xml.sax.SAXException;

/**
 * a collection of static methods aimed at making it easy to 
 *  - save and load CASes, and to
 *  - optionally include their Type Systems and index definitions based on those type systems (abbreviated TSI). 
 *
 * There are several serialization formats supported; these are listed in the Java enum SerialFormat, 
 * together with their preferred file extension name.
 * 
 * The APIs for loading attempt to automatically use the appropriate deserializers, based on the input data format.  
 * To select the right deserializer, first, the file extension name (if available) is used:
 *   - xmi: XMI format
 *   - xcas: XCAS format
 *   - xml: XCAS format
 *   
 * If none of these apply, then the first few bytes of the input are examined to determine the format.
 * 
 * For loading, the inputs may be supplied as URLs or as InputStream.  
 * You can use Files or Paths by converting these to URLs:
 *    URL url = a_path.toUri().toURL();
 *    URL url = a_file.toUri().toURL();
 *    
 *  When loading, an optional lenient boolean flag may be specified.  
 *  It is observed only for the XMI and XCAS formats.
 *  If true, then types and/or features being deserialized which don't exist in the receiving CAS are silently ignored.
 *  
 *  When TSI is saved, it is either saved in the same destination (e.g. file or stream), or in a separate one.
 *    - One serialization format, SERIALIZED_TSI, supports saving the TSI in the same destination. 
 *      Other formats require the TSI to be saved to a separate OutputStream.
 *      
 *  Summary of the APIs for saving:
 *    save(CAS, OutputStream, SerialFormat)
 *    save(CAS, OutputStream, OutputStream, SerialFormat)  - extra outputStream for saving the TSI
 *    
 *  Note: there is no API for saving in COMPRESSED_FILTERED with a filtering type system; to do that, use the
 *  methods in Serialization.serializeWithCompression
 *  
 *  Summary of APIs for loading:
 *    load(URL        , CAS)
 *    load(InputStream, CAS)
 *    
 *    load(URL        , URL        , CAS, lenient_flag)   - the second URL is for loading a separately-stored TSI
 *    load(InputStream, InputStream, CAS, lenient_flag)
 *    
 *  You may specify the lenient_flag without the TSI input by setting the TSI input argument to null.
 *
 */

public class CasIOUtils {

  /**
   * Loads a Cas from a URL source. The format is determined from the file extension name and the content.
   * For formats of type SERIALIZED_TSI, the type system and index definitions are reset
   * Lenient is false; to use lenient loading, use the 4 argument form.
   * 
   * @param casUrl
   *          The url containing the CAS
   * @param aCAS
   *          The CAS that should be filled
   * @return the SerialFormat of the loaded CAS
   * @throws IOException
   *           - Problem loading from given URL
   */
  public static SerialFormat load(URL casUrl, CAS aCAS) throws IOException {

    return load(casUrl, null, aCAS, false);
  }

  /**
   * Loads a CAS from a URL source. The format is determined from the file extension name and the content.
   * For formats of type SERIALIZED_TSI, the type system and index definitions are read from the casUrl source;
   * the value of tsiInputStream is ignored.    
   * For other formats, if the tsiUrl is not null, type system and index definitions are read from that source.
   * 
   * To specify lenient loading, without specifying an additional type system and index definition source, 
   * pass null for the tsiUrl.
   * 
   * @param casUrl
   *          The url to deserialize the CAS from
   * @param tsiUrl
   *          The optional url to deserialize the type system and index definitions from
   * @param aCAS
   *          The CAS that should be filled
   * @param leniently
   *          for XCAS and XMI formats, ignore feature structures and features of non-existing types and/or features.
   *          ignored for other formats.
   * @return the SerialFormat of the loaded CAS
   * @throws IOException
   *           - Problem loading from given URL
   */
  public static SerialFormat load(URL casUrl, URL tsiUrl, CAS aCAS, boolean leniently)
          throws IOException {
    String path = casUrl.getPath().toLowerCase();

    if (path.endsWith(SerialFormat.XMI.getDefaultFileExtension())) {
      InputStream casIS = new BufferedInputStream(casUrl.openStream());
      try {
        XmiCasDeserializer.deserialize(casIS, aCAS, leniently);
        return SerialFormat.XMI;
      } catch (SAXException e) {
        throw new IOException(e);
      } finally {
        closeQuitely(casIS);
      }
    }
    
    if (path.endsWith(SerialFormat.XCAS.getDefaultFileExtension())
            || path.endsWith(".xml")) {
      InputStream casIS = new BufferedInputStream(casUrl.openStream());
      try {
        XCASDeserializer.deserialize(casIS, aCAS, leniently);
        return SerialFormat.XCAS;
      } catch (SAXException e) {
        throw new IOException(e);
      } finally {
        closeQuitely(casIS);
      }
    } 
  
    InputStream casIS = new BufferedInputStream(casUrl.openStream());
    InputStream tsIS = (tsiUrl == null) ? null : new BufferedInputStream(tsiUrl.openStream());
    try {
      return load(casIS, tsIS, aCAS, leniently);
    } finally {
      closeQuitely(casIS);
      closeQuitely(tsIS);
    }  
  }

  /**
   * Loads a Cas from a URL source. The format is determined from the content.
   * For formats of type SERIALIZED_TSI, the type system and index definitions are reset.
   * Lenient is false; to use lenient loading, use the 4 argument form.
   * 
   * @param casInputStream
   *          The input stream containing the CAS.  Caller should buffer this appropriately.
   * @param aCAS
   *          The CAS that should be filled
   * @return the SerialFormat of the loaded CAS
   * @throws IOException
   *           - Problem loading from given InputStream
   */
  public static SerialFormat load(InputStream casInputStream, CAS aCAS) throws IOException {
    return load(casInputStream, null, aCAS, false);
  }

  /**
   * Loads a CAS from a URL source. The format is determined from the content.
   * For formats of type SERIALIZED_TSI, the type system and index definitions are read from the casUrl source;
   * the value of tsiInputStream is ignored.
   * For other formats, if the tsiUrl is not null, type system and index definitions are read from that source.
   * 
   * To specify lenient loading, without specifying an additional type system and index definition source, 
   * pass null for the tsiInputStream.
   * 
   * @param casInputStream
   *          The input stream containing the CAS. Caller should buffer this appropriately.
   * @param tsiInputStream
   *          The optional input stream containing the type system. Caller should buffer this appropriately.
   * @param aCAS
   *          The CAS that should be filled
   * @param leniently
   *          for XCAS and XMI formats, ignore feature structures and features of non-existing types and/or features.
   *          ignored for other formats.
   * @return the SerialFormat of the loaded CAS
   * @throws IOException
   *           - Problem loading from given InputStream
   * @throws IllegalArgumentException
   *           - when trying to load XCAS
   */
  public static SerialFormat load(InputStream casInputStream, InputStream tsiInputStream, CAS aCAS,
          boolean leniently) throws IOException {

    if (!casInputStream.markSupported()) {
      casInputStream = new BufferedInputStream(casInputStream);
    }
    
    if (tsiInputStream != null && !tsiInputStream.markSupported()) {
      tsiInputStream = new BufferedInputStream(tsiInputStream);
    }
    
    CASImpl casImpl = (CASImpl) aCAS;
    /** scan the first part of the file for known formats */
    casInputStream.mark(6);
    byte[] firstPartOfFile = new byte[6];
    int bytesReadCount = casInputStream.read(firstPartOfFile);
    casInputStream.reset();
    String start = new String(firstPartOfFile, 0, bytesReadCount, "UTF-8").toLowerCase();

    if (start.startsWith("<?xml ")) {  // could be XCAS or XMI
      try {
        return XmlCasDeserializer.deserialize(casInputStream, aCAS, leniently);
      } catch (SAXException e) {
        throw new UIMARuntimeException(e);
      }
    }
    
    DataInputStream deserIn = CommonSerDes.maybeWrapToDataInputStream(casInputStream);
    if (CommonSerDes.isBinaryHeader(deserIn)) {   
      return casImpl.reinit(casInputStream);
    } else {
      // is a Java Object serialization, with or without a type system
      ObjectInputStream ois = new ObjectInputStream(casInputStream);
      try {
        Object o = ois.readObject();
        if (o instanceof CASSerializer) {
          casImpl = readCasManager(casImpl, tsiInputStream);  // maybe install type system and index def   
          casImpl.reinit((CASSerializer) o);                  // deserialize from object
          return SerialFormat.SERIALIZED;
        } else if (o instanceof CASCompleteSerializer) {
          // with a type system use that, ignore any supplied via tsiInputStream
          casImpl.reinit((CASCompleteSerializer) o);
          return SerialFormat.SERIALIZED_TSI;
        } else {
          /**Unrecognized serialized CAS format*/
          throw new CASRuntimeException(CASRuntimeException.UNRECOGNIZED_SERIALIZED_CAS_FORMAT);  
        }
      } catch (ClassNotFoundException e) {
        /**Unrecognized serialized CAS format*/
        throw new CASRuntimeException(CASRuntimeException.UNRECOGNIZED_SERIALIZED_CAS_FORMAT);
      }       
    }
  }


  /**
   * Write the CAS in the specified format.
   * 
   * @param aCas
   *          The CAS that should be serialized and stored
   * @param docOS
   *          The output stream for the CAS
   * @param format
   *          The SerialFormat in which the CAS should be stored.
   * @throws IOException
   *           - Problem saving to the given InputStream
   */
  public static void save(CAS aCas, OutputStream docOS, SerialFormat format) throws IOException {
    save(aCas, docOS, null, format);
  }

  /**
   * Write the CAS in the specified format. If the format does not include typesystem information
   * and the optional output stream of the typesystem is specified, then the typesystem information
   * is written there.
   * 
   * @param aCas
   *          The CAS that should be serialized and stored
   * @param docOS
   *          The output stream for the CAS, with appropriate buffering
   * @param tsiOS
   *          Optional output stream for type system information. Only used if the format does not
   *          support storing typesystem information directly in the main output file.
   * @param format
   *          The SerialFormat in which the CAS should be stored.
   * @throws IOException
   *           - Problem saving to the given InputStream
   */
  public static void save(CAS aCas, OutputStream docOS, OutputStream tsiOS, SerialFormat format)
          throws IOException {
    boolean typeSystemWritten = false;
    try {
      switch (format) {
        case XMI:
          XmiCasSerializer.serialize(aCas, docOS);
          break;
        case XCAS:
          XCASSerializer.serialize(aCas, docOS, true); // true = formatted output
          break;
        case SERIALIZED:
          writeJavaObject(Serialization.serializeCAS(aCas), docOS);
          break;
        case SERIALIZED_TSI:
          writeJavaObject(Serialization.serializeCASComplete((CASMgr) aCas), docOS);
          typeSystemWritten = true; // Embedded type system
          break;
        case BINARY:              // Java-serialized CAS without type system
          serializeCAS(aCas, docOS);
          break;
        case COMPRESSED:          // Binary compressed CAS without type system (form 4)
          serializeWithCompression(aCas, docOS);
          break;
        case COMPRESSED_FILTERED: // Binary compressed CAS (form 6)
          serializeWithCompression(aCas, docOS, aCas.getTypeSystem());
          break;
        default:
          throw new IllegalArgumentException("Unknown format [" + format.name()
                  + "]. Must be one of: " + SerialFormat.values());
      }
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }

    if (tsiOS != null && !typeSystemWritten) {
      writeTypeSystem(aCas, tsiOS);
    }
  }

  /**
   * Takes a serialized version of the type system and index definitions as represented by
   * the Java object serialization of the class CASMgrSerializer, and reads it, and uses it 
   * to reset the provided CAS to this new definition.
   * 
   * @param cas the CAS to reset
   * @param aIs the stream having the serialized CASMgrSerializer
   * @return the initial view of the new cas with the type and index definitions installed and set up.
   * @throws IOException
   */
  private static CASImpl readCasManager(CAS cas, InputStream aIs) throws IOException {
    if (null == aIs) {
      return (CASImpl) cas;
    }
    CASMgrSerializer casMgrSerializer;
    CASImpl casImpl = (CASImpl) cas;

    try {
      ObjectInputStream is = new ObjectInputStream(aIs);
      casMgrSerializer = (CASMgrSerializer) is.readObject();
      return casImpl.setupCasFromCasMgrSerializer(casImpl, casMgrSerializer);
    } catch (ClassNotFoundException e) {
      throw new IOException(e);
    }    
  }

  private static void writeJavaObject(Object o, OutputStream aOS) throws IOException {
    ObjectOutputStream tsiOS = new ObjectOutputStream(aOS);
    tsiOS.writeObject(o);
    tsiOS.flush();
  }
  
  private static void writeTypeSystem(CAS aCas, OutputStream aOS) throws IOException {
    writeJavaObject(serializeCASMgr((CASImpl) aCas), aOS);
  }
  
  private static void closeQuitely(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException e) {
        // do nothing
      }
    }
  }
  
}
