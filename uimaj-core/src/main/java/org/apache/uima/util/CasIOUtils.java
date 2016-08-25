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
import java.util.Arrays;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.impl.AllowPreexistingFS;
import org.apache.uima.cas.impl.BinaryCasSerDes4;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.cas.impl.CommonSerDes;
import org.apache.uima.cas.impl.CommonSerDes.Header;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.xml.sax.SAXException;

/**
 * <p>A collection of static methods aimed at making it easy to</p>
 * <ul>
 *   <li>save and load CASes, and to</li>
 *   <li>optionally include the CAS's Type System (abbreviated TS (only available for Compressed Form 6)) and optionally also include the CAS's indexes definition.</li>
 *   <li>The combinaton of Type System and Indexes definition is called TSI.
 *     <ul>
 *       <li>The TSI's purpose: to replace the CAS's existing type system and index definition.</li>
 *       <li>The TS's purpose: to specify the type system used in the serialized data for format Compressed Form 6, in order to allow deserializing into some other type system in the CAS, leniently.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>TSI information can be</p>
 * <ul>
 *   <li>embedded</li>
 *   <li>externally supplied (via another input source to the load)</li>
 *   <li>both embedded and externally supplied.&nbsp; In this case the embedded takes precedence.</li>
 * </ul>
 *
 * <p>TS information is available embedded, for COMPRESSED_FILTERED_TS format,
 *    and also from embedded or external TSI information (since it also contains the type system information).</p>
 *
 * <p>When an external TSI is supplied while loading Compressed Form 6,</p>
 * <ul>
 *   <li>for COMPRESSED_FILTERED_TS
 *     <ul>
 *       <li>it uses the embedded TS for decoding</li>
 *       <li>it uses the external TSI to replace the CAS's existing type system and index definition if CasLoadMode == REINIT.</li>
 *     </ul>
 *   </li>
 *   <li>for COMPRESSED_FILTERED_TSI
 *     <ul>
 *       <li>the external TSI is ignored, the embedded one overrides, but otherwise operates as above.</li> 
 *     </ul>
 *   </li>
 *   <li>for COMPRESSED_FILTERED
 *     <ul>
 *       <li>the external TSI's type system part is used for decoding.</li>
 *       <li>if CasLoadMode == REINIT, the external TSI is also used to replace the CAS's existing type system and index definition.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>Compressed Form 6 loading decoding type system is picked from these sources, in this order:</p>
 * <ul>
 *   <li>a passed in type system</li>
 *   <li>an embedded TS or TSI</li>
 *   <li>an external TSI</li>
 *   <li>the CAS's type system</li>
 * </ul>
 *
 * <p>The serialization formats supported here are specified in the SerialFormat enum.</p>
 *
 * <p>The <code>load </code>api's automatically use the appropriate deserializers, based on the input data format.</p>
 *
 * <p>Loading inputs may be supplied as URLs or as an appropriately buffered InputStream.</p>
 *
 * <p>Note: you can use Files or Paths by converting these to URLs:</p>
 * <ul>
 *   <li><code>URL url = a_path.toUri().toURL();</code></li>
 *   <li><code>URL url = a_file.toUri().toURL();</code></li>
 * </ul>
 *
 * <p>When loading, an optional CasLoadMode enum value maybe specified to indicate</p>
 * <ul>
 *   <li>LENIENT loading - used with XCas and XMI data data sources to silently ignore types and features present in the serialized form, but not in the receiving type system.</li>
 *   <li>REINIT - used with Compressed Form 6 loading to indicate that&nbsp; if no embedded TSI information is available, the external TSI is to be used to replace the CAS's existing type system and index definition.</li>
 * </ul>
 *
 * <p style="padding-left: 30px;">For more details, see the Javadocs for CasLoadMode.</p>
 *
 * <p>When TS or TSI information is saved, it is either saved in the same destination (e.g. file or stream), or in a separate one.</p>
 * <ul>
 *   <li>The serialization formats ending in _TSI and _TS support saving the TSI (or TS) in the same destination.</li>
 *   <li>The save APIs for other formats can optionally also save the TSI into a separate (second) OutputStream.</li>
 * </ul>
 *
 * <p>Summary of APIs for saving:</p>
 * <pre style="padding-left: 30px;">
 *   <code>save(aCAS, outputStream, aSerialFormat)</code>
 *   <code>save(aCAS, outputStream, tsiOutputStream, aSerialFormat)</code></pre>
 *
 * <p>Summary of APIs for loading:</p>
 * <pre style="padding-left: 30px;">
 *   <code>load(aURL&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; , aCas)</code>
 *   <code>load(inputStream, aCas)</code>
 *   <code>load(inputStream, aCas, typeSystem)</code> // typeSystem used for decoding Compressed Form 6
 *   <code>load(inputStream, tsiInputStream, aCas)</code></pre>
 * <pre style="padding-left: 30px;">
 *   <code>load(aURL&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; , tsiURL&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; , aCAS, casLoadMode)&nbsp;&nbsp; - the second URL is for loading a separately-stored TSI</code>
 *   <code>load(inputStream, tsiInputStream, aCAS, aCasLoadMode)</code>
 *   <code>load(aURL&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; , tsiURL&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; , aCAS, lenient)&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; - lenient is used to set the CasLoadMode to LENIENT or DEFAULT</code>
 *   <code>load(inputStream, tsiInputStream, aCAS, lenient)</code></pre>
 */

public class CasIOUtils {

  /**
   * Loads a Cas from a URL source. 
   * For SerialFormats ending with _TSI except for COMPRESSED_FILTERED_TSI, 
   * the CAS's type system and indexes definition are replaced.
   * CasLoadMode is DEFAULT.
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
   * If the value of tsiUrl is null it is ignored.
   * 
   * @param casUrl
   *          The url to deserialize the CAS from
   * @param tsiUrl
   *          null or an optional url to deserialize the type system and index definitions from
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
   * For SerialFormats ending with _TSI except for COMPRESSED_FILTERED_TSI, 
   * the CAS's type system and indexes definition are replaced.
   * CasLoadMode is set according to the leniently flag.
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
   * Loads a Cas from an Input Stream. The format is determined from the content.
   * For SerialFormats ending with _TSI except for COMPRESSED_FILTERED_TSI, 
   * the CAS's type system and indexes definition are replaced.
   * CasLoadMode is DEFAULT.
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
   * 
   * For SerialFormats ending with _TSI the embedded value is used instead of any supplied external TSI information.
   * TSI information is available either via embedded value, or if a non-null input is passed for tsiInputStream.
   * 
   * If TSI information is available, the CAS's type system and indexes definition are replaced,
   * except for SerialFormats COMPRESSED_FILTERED, COMPRESSED_FILTERED_TS, and COMPRESSED_FILTERED_TSI.
   *
   * The CasLoadMode is DEFAULT.
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

  /**
   * Loads a CAS from an Input Stream. The format is determined from the content.
   * 
   * For SerialFormats ending with _TSI the embedded value is used instead of any supplied external TSI information.
   * TSI information is available either via embedded value, or if a non-null input is passed for tsiInputStream.
   * 
   * If TSI information is available, the CAS's type system and indexes definition are replaced,
   * except for SerialFormats COMPRESSED_FILTERED, COMPRESSED_FILTERED_TS, and COMPRESSED_FILTERED_TSI.
   *
   * The CasLoadMode is set to LENIENT if the leniently flag is true; otherwise it is set to DEFAULT.
   * 
   * @param casInputStream -
   * @param tsiInputStream -
   * @param aCAS -
   * @param leniently - 
   * @return -
   * @throws IOException -
   */
  public static SerialFormat load(InputStream casInputStream, InputStream tsiInputStream, CAS aCAS, boolean leniently) throws IOException {
    return load(casInputStream, tsiInputStream, aCAS, leniently ? CasLoadMode.LENIENT : CasLoadMode.DEFAULT);
  }

  /**
   * Loads a CAS from an Input Stream. The format is determined from the content.
   * For formats of ending in _TSI SERIALIZED_TSI or COMPRESSED_FILTERED_TSI, 
   * the type system and index definitions are read from the cas input source;
   * the value of tsiInputStream is ignored.
   * 
   * For other formats, if the tsiInputStream is not null, 
   * type system and index definitions are read from that source.
   * 
   * If TSI information is available, the CAS's type system and indexes definition are replaced,
   * except for SerialFormats COMPRESSED_FILTERED, COMPRESSED_FILTERED_TS, and COMPRESSED_FILTERED_TSI.
   * 
   *   If the CasLoadMode == REINIT, then the TSI information is also used for these 3 formats to replace the CAS's definitions.
   *   
   * @param casInputStream
   *          The input stream containing the CAS, appropriately buffered.
   * @param tsiInputStream
   *          The optional input stream containing the type system, appropriately buffered. 
   *          This is only used if it is non null and 
   *            -  the casInputStream does not already come with an embedded CAS Type System and Index Definition, or 
   *            -  the serial format is COMPRESSED_FILTERED_TSI
   * @param aCAS
   *          The CAS that should be filled
   * @param casLoadMode specifies loading alternative like lenient and reinit, see CasLoadMode.
   * @return the SerialFormat of the loaded CAS
   * @throws IOException
   *           - Problem loading from given InputStream
   */
  public static SerialFormat load(InputStream casInputStream, InputStream tsiInputStream, CAS aCAS,
          CasLoadMode casLoadMode) throws IOException {
    return load(casInputStream, tsiInputStream, aCAS, casLoadMode, null);
  }
  
  /**
   * This load variant can be used for loading Form 6 compressed CASes where the 
   * type system to use to deserialize is provided as an argument.  It can also load other formats,
   * where its behavior is identical to load(casInputStream, aCas).
   *
   * Loads a CAS from an Input Stream. The format is determined from the content.
   * For SerialFormats of ending in _TSI SERIALIZED_TSI or COMPRESSED_FILTERED_TSI, 
   * the type system and index definitions are read from the cas input source;
   * the value of typeSystem is ignored.
   * 
   * For COMPRESSED_FILTERED_xxx formats, if the typeSystem is not null, 
   * the typeSystem is used for decoding.
   * 
   * If embedded TSI information is available, the CAS's type system and indexes definition are replaced,
   * except for SerialFormats COMPRESSED_FILTERED, COMPRESSED_FILTERED_TS, and COMPRESSED_FILTERED_TSI.
   * 
   *   To replace the CAS's type system and indexes definition for these, use a load form which 
   *   has the CasLoadMode argument, and set this to REINIT.
   *     
   * @param casInputStream
   *          The input stream containing the CAS, appropriately buffered.
   * @param aCAS
   *          The CAS that should be filled
   * @param typeSystem the type system to use for decoding the serialized form, must be non-null         
   * @return the SerialFormat of the loaded CAS
   * @throws IOException Problem loading from given InputStream   
   */
  public static SerialFormat load(InputStream casInputStream, CAS aCAS, TypeSystem typeSystem) throws IOException {
    return load(casInputStream, null, aCAS, CasLoadMode.DEFAULT, (TypeSystemImpl) typeSystem);
  }
  
  private static SerialFormat load(InputStream casInputStream, InputStream tsiInputStream, CAS aCAS,
      CasLoadMode casLoadMode, TypeSystemImpl typeSystem) throws IOException {

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
      return casImpl.reinit(h, casInputStream, readCasManager(tsiInputStream), casLoadMode, null, AllowPreexistingFS.allow, typeSystem);
    
    } else {
      
      /******************************
       * Java Object loading
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
        case BINARY_TSI:              // Java-serialized CAS without type system
          CASSerializer ser = new CASSerializer();
          ser.addCAS((CASImpl) aCas, docOS, true);
          break;
        case COMPRESSED:          // Binary compressed CAS without type system (form 4)
          serializeWithCompression(aCas, docOS);
          break;
        case COMPRESSED_TSI:          // Binary compressed CAS without type system (form 4)
          new BinaryCasSerDes4((TypeSystemImpl)aCas.getTypeSystem(), false).serializeWithTsi((CASImpl) aCas, docOS);
          break;
        case COMPRESSED_FILTERED: // Binary compressed CAS (form 6)
          serializeWithCompression(aCas, docOS, false, false);
          break;
        case COMPRESSED_FILTERED_TS:
          serializeWithCompression(aCas, docOS, true, false);
          typeSystemWritten = true; // Embedded type system
          break;
        case COMPRESSED_FILTERED_TSI:
          serializeWithCompression(aCas, docOS, false, true);
          typeSystemWritten = true; // Embedded type system
          break;
        default:
          StringBuilder sb = new StringBuilder();
          for (SerialFormat sf : SerialFormat.values()) {
            sb = sb.append(sf.toString()).append(", ");
          }
          throw new IllegalArgumentException("Unknown format [" + format.name()
                  + "]. Must be one of: " + sb.toString());
      }
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }

    // Write type system to the separate stream only if it has not already been embedded into the
    // main stream
    if (tsiOS != null && !typeSystemWritten) {
      writeTypeSystem(aCas, tsiOS, true);
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
    
  private static void writeJavaObject(Object o, OutputStream aOS) throws IOException {
    ObjectOutputStream tsiOS = new ObjectOutputStream(aOS);
    tsiOS.writeObject(o);
    tsiOS.flush();
  }
  
  public static void writeTypeSystem(CAS aCas, OutputStream aOS, boolean includeIndexDefs) throws IOException {
    writeJavaObject(includeIndexDefs 
                        ? Serialization.serializeCASMgr((CASImpl) aCas)
                        : Serialization.serializeCASMgrTypeSystemOnly((CASImpl) aCas)
                      , aOS);
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
