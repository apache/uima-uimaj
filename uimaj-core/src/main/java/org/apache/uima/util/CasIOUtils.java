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
import org.apache.uima.cas.impl.AllowPreexistingFS;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.cas.impl.CommonSerDes;
import org.apache.uima.cas.impl.CommonSerDes.Header;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.xml.sax.SAXException;

/**
 * a collection of static methods aimed at making it easy to 
 *  - save and load CASes, and to
 *  - optionally include their Type Systems (abbreviated TS) 
 *    and perhaps also their index definitions based on those type systems (abbreviated TSI). 
 *
 *    * The TSI's purpose: these are used to replace a CAS's existing type system and index definition.
 *      
 *    * The TS's purpose: these are only used with Compressed Form 6 to specify the type system used in the serialized data,
 *      in order to allow deserializing into some other type system in the CAS, leniently.
 *      
 *      ** Note: this use does **not** replace the CAS's type system, like the TSI use does.
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
 * Note: You can use Files or Paths by converting these to URLs:
 *    URL url = a_path.toUri().toURL();
 *    URL url = a_file.toUri().toURL();
 *    
 * When loading, an optional CasLoadMode enum value may be specified.  See the Javadocs for that for details.
 *  
 * When TS or TSI information is saved, it is either saved in the same destination (e.g. file or stream), or in a separate one.
 *   - The serialization formats ending in _TSI support saving the TSI in the same destination. 
 *     The save APIs for other formats can optionally also save the TSI into a separate (second) OutputStream.
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
 *    load(URL        , URL        , CAS, CasLoadMode)   - the second URL is for loading a separately-stored TSI
 *    load(InputStream, InputStream, CAS, CasLoadMode)
 *    
 *  When loading, a TSI or a TS may be available.  If available, it is used for one of two purposes:
 *    - except for Compressed Form 6, it must be a TSI and 
 *      it is used to reset the CAS to have the specified type system and index specification
 *    - for Compressed Form 6 (only) it is used to specify the type system of the serialized data, to enable
 *      form 6's lenient deserialization. 
 */

public class CasIOUtils {

  /**
   * Loads a Cas from a URL source. The format is determined from the file extension name and the content.
   * For formats ending with _TSI, the type system and index definitions are reset.
   * CasLoadMode is DEFAULT.  To specify this explicitly, use the 4 argument form.
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

    return load(casUrl, null, aCAS, CasLoadMode.DEFAULT);
  }

  /**
   * Loads a CAS from a URL source. The format is determined from the content.
   * 
   * If the value of tsiUrl is non-null, it is read.
   *   If values from both the tsiUrl and embedded values are available, the tsiUrl value is used.    
   * 
   * @param casUrl
   *          The url to deserialize the CAS from
   * @param tsiUrl
   *          The optional url to deserialize the type system and index definitions from
   * @param aCAS
   *          The CAS that should be filled
   * @param casLoadMode specifies how to handle reinitialization and lenient loading
   *          see the Javadocs for CasLoadMode
   * @return the SerialFormat of the loaded CAS
   * @throws IOException Problem loading
   */
  public static SerialFormat load(URL casUrl, URL tsiUrl, CAS aCAS, CasLoadMode casLoadMode)
          throws IOException {
    InputStream casIS = new BufferedInputStream(casUrl.openStream());
    InputStream tsIS = (tsiUrl == null) ? null : new BufferedInputStream(tsiUrl.openStream());
    try {
      return load(casIS, tsIS, aCAS, casLoadMode);
    } finally {
      closeQuitely(casIS);
      closeQuitely(tsIS);
    }  
  }
  
  /**
   * Loads a CAS from a URL source. The format is determined from the content.
   * 
   * If the value of tsiUrl is non-null, it is read.
   *   If values from both the tsiUrl and embedded values are available, the tsiUrl value is used.    
   * 
   * @param casUrl
   *          The url to deserialize the CAS from
   * @param tsiUrl
   *          The optional url to deserialize the type system and index definitions from
   * @param aCAS
   *          The CAS that should be filled
   * @param leniently true means do lenient loading
   * @return the SerialFormat of the loaded CAS
   * @throws IOException Problem loading
   */
  public static SerialFormat load(URL casUrl, URL tsiUrl, CAS aCAS, boolean leniently)
      throws IOException {
    return load(casUrl, tsiUrl, aCAS, leniently ? CasLoadMode.LENIENT : CasLoadMode.DEFAULT);
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
    return load(casInputStream, null, aCAS, CasLoadMode.DEFAULT);
  }

  /**
   * Loads a CAS from an Input Stream. The format is determined from the content.
   * For formats of type SERIALIZED_TSI or COMPRESSED_FILTERED_TSI, 
   * the type system and index definitions are read from the cas input source;
   * the value of tsiInputStream is ignored.
   * For other formats, if the tsiInputStream is not null, 
   * type system and index definitions are read from that source.
   * 
   * @param casInputStream -
   * @param tsiInputStream -
   * @param aCAS -
   * @return -
   * @throws IOException -
   */
  public static SerialFormat load(InputStream casInputStream, InputStream tsiInputStream, CAS aCAS) throws IOException {
    return load(casInputStream, tsiInputStream, aCAS, CasLoadMode.DEFAULT);
  }

  public static SerialFormat load(InputStream casInputStream, InputStream tsiInputStream, CAS aCAS, boolean leniently) throws IOException {
    return load(casInputStream, tsiInputStream, aCAS, leniently ? CasLoadMode.LENIENT : CasLoadMode.DEFAULT);
  }

  /**
   * Loads a CAS from a URL source. The format is determined from the content.
   * For formats of type SERIALIZED_TSI or COMPRESSED_FILTERED_TSI, 
   * the type system and index definitions are read from the cas input source;
   * the value of tsiInputStream is ignored.
   * For other formats, if the tsiInputStream is not null, 
   * type system and index definitions are read from that source.
   * 
   * @param casInputStream
   *          The input stream containing the CAS, appropriately buffered.
   * @param tsiInputStream
   *          The optional input stream containing the type system, appropriately buffered. 
   *          This is only used if the casInputStream does not already come 
   *          with an embedded CAS Type System and Index Definition, and is non-null.
   * @param aCAS
   *          The CAS that should be filled
   * @param casLoadMode specifies loading alternative like lenient and reinit, see CasLoadMode.
   *          For XCAS and XMI formats, ignore feature structures and features of non-existing types and/or features.
   *          For Compressed Form 6, if true, the tsiInputStream is used only to supply the Type System for the serialized form;
   *            the CAS type system is not altered.
   *          For other formats, ignored.
   * @return the SerialFormat of the loaded CAS
   * @throws IOException
   *           - Problem loading from given InputStream
   */
  public static SerialFormat load(InputStream casInputStream, InputStream tsiInputStream, CAS aCAS,
          CasLoadMode casLoadMode) throws IOException {

    if (!casInputStream.markSupported()) {
      casInputStream = new BufferedInputStream(casInputStream);
    }
    
    CASImpl casImpl = (CASImpl) aCAS;

    // scan the first part of the file for known formats
    casInputStream.mark(6);
    byte[] firstPartOfFile = new byte[6];
    int bytesReadCount = casInputStream.read(firstPartOfFile);
    casInputStream.reset();
    String start = new String(firstPartOfFile, 0, bytesReadCount, "UTF-8").toLowerCase();

    if (start.startsWith("<?xml ")) {  // could be XCAS or XMI
      try {
        casImpl.setupCasFromCasMgrSerializer(readCasManager(tsiInputStream));
        // next call decides on XMI or XCAS via content
        return XmlCasDeserializer.deserializeR(casInputStream, aCAS, casLoadMode == CasLoadMode.LENIENT);
      } catch (SAXException e) {
        throw new UIMARuntimeException(e);
      }
    }
    
    //  Not an XML file, decode as binary file
    DataInputStream deserIn = CommonSerDes.maybeWrapToDataInputStream(casInputStream);
    if (CommonSerDes.isBinaryHeader(deserIn)) {
      
      /*******************************************
       * Binary, Compressed Binary (form 4 or 6)
       ******************************************/
      Header h = CommonSerDes.readHeader(deserIn);
      return casImpl.reinit(h, casInputStream, readCasManager(tsiInputStream), casLoadMode, null, AllowPreexistingFS.allow);
//      TypeSystemImpl ts = null;
//      
//      
//      if (h.isTypeSystemIncluded() || h.isTypeSystemIndexDefIncluded()) { // Load TS from CAS stream
//        try {
//          ObjectInputStream ois = new ObjectInputStream(deserIn);
//          embeddedCasMgrSerializer = (CASMgrSerializer) ois.readObject();
//        } catch (ClassNotFoundException e) {
//          /**Unrecognized serialized CAS format*/
//          throw new CASRuntimeException(CASRuntimeException.UNRECOGNIZED_SERIALIZED_CAS_FORMAT);
//        }
//      }
//      
//      maybeReinit(
//          (null == casMgrSerializer && h.isTypeSystemIndexDefIncluded()) 
//            ? embeddedCasMgrSerializer 
//            : casMgrSerializer, 
//          casLoadMode, casImpl);
//        
//      if (!h.isForm6() && casLoadMode == CasLoadMode.LENIENT) {
//        /**Lenient deserialization not support for input of type {0}.*/
//        throw new CASRuntimeException(CASRuntimeException.LENIENT_NOT_SUPPORTED, new Object[] {h.toString()});
//      }
//      
//      return casImpl.reinit(h, casInputStream, ts);

    
    } else {
      
      /******************************
       * Java Object Serialization
       ******************************/
      ObjectInputStream ois = new ObjectInputStream(casInputStream);
      try {
        Object o = ois.readObject();
        if (o instanceof CASSerializer) {
          casImpl.setupCasFromCasMgrSerializer(readCasManager(tsiInputStream));
          casImpl.reinit((CASSerializer) o); // deserialize from object
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

  
//  private static CASMgrSerializer maybeGetExternalTSI(InputStream tsiInputStream, CASImpl casImpl) throws IOException {
//    return (tsiInputStream != null) ? readCasManager(tsiInputStream)
//    CASMgrSerializer casMgrSerializer = null;
//    if (tsiInputStream != null) {   
//      casMgrSerializer = readCasManager(tsiInputStream);  
//      casImpl.setupCasFromCasMgrSerializer(casMgrSerializer);
//    }  
//    return casMgrSerializer;
//  }
  
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
          serializeWithCompression(aCas, docOS, false, false);
          break;
        case COMPRESSED_FILTERED_TS:
          // Binary compressed CAS (form 6)
          // ... with embedded Java-serialized type system
          serializeWithCompression(aCas, docOS, true, false);
          typeSystemWritten = true; // Embedded type system
          break;
        case COMPRESSED_FILTERED_TSI:
          // Binary compressed CAS (form 6)
          // ... with embedded Java-serialized type system
          serializeWithCompression(aCas, docOS, false, true);
          typeSystemWritten = true; // Embedded type system
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

    // Write type system to the separate stream only if it has not alreay been embedded into the
    // main stream
    if (tsiOS != null && !typeSystemWritten) {
      writeTypeSystem(aCas, tsiOS);
    }
  }

  private static CASMgrSerializer readCasManager(InputStream tsiInputStream) throws IOException {
    try {
      if (null == tsiInputStream) {
        return null;
      }
      ObjectInputStream is = new ObjectInputStream(tsiInputStream);
      return (CASMgrSerializer) is.readObject();
    } catch (ClassNotFoundException e) {
      throw new IOException(e);
    }    
  }
  
//  private static void maybeReinit(CASMgrSerializer casMgrSerializer, CasLoadMode casLoadMode, CASImpl cas) {
//    if (casLoadMode == CasLoadMode.REINIT && casMgrSerializer != null) {
//      cas.setupCasFromCasMgrSerializer(casMgrSerializer);
//    }
//  }
  
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
