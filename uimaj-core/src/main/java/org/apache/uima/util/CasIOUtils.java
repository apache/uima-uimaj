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

import static org.apache.uima.cas.impl.Serialization.deserializeCAS;
import static org.apache.uima.cas.impl.Serialization.deserializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeCAS;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeCASMgr;
import static org.apache.uima.cas.impl.Serialization.serializeWithCompression;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.cas.impl.CommonSerDes;
import org.apache.uima.cas.impl.CommonSerDes.Header;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

public class CasIOUtils {

  /**
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
   * 
   * @param casUrl
   *          The url containing the CAS
   * @param tsUrl
   *          The optional url containing the type system
   * @param aCAS
   *          The CAS that should be filled
   * @param leniently
   *          ignore feature structures of non-existing types
   * @return the SerialFormat of the loaded CAS
   * @throws IOException
   *           - Problem loading from given URL
   */
  public static SerialFormat load(URL casUrl, URL tsUrl, CAS aCAS, boolean leniently)
          throws IOException {
    SerialFormat result = SerialFormat.UNKNOWN;
    String path = casUrl.getPath().toLowerCase();

    if (path.endsWith(SerialFormat.XMI.getDefaultFileExtension())) {
      InputStream casIS = casUrl.openStream();
      try {
        XmiCasDeserializer.deserialize(casIS, aCAS, leniently);
        result = SerialFormat.XMI;
      } catch (SAXException e) {
        throw new IOException(e);
      } finally {
        closeQuitely(casIS);
      }
    } else if (path.endsWith(SerialFormat.XCAS.getDefaultFileExtension())
            || path.endsWith(".xml")) {
      InputStream casIS = casUrl.openStream();
      try {
        XCASDeserializer.deserialize(casIS, aCAS, leniently);
        result = SerialFormat.XCAS;
      } catch (SAXException e) {
        throw new IOException(e);
      } finally {
        closeQuitely(casIS);
      }
    } else {
      InputStream casIS = casUrl.openStream();
      InputStream tsIS = tsUrl == null ? null : tsUrl.openStream();
      result = loadBinary(casIS, tsIS, aCAS);
      closeQuitely(casIS);
      closeQuitely(tsIS);
    }
    return result;
  }


  /**
   * This method tries to guess the format of the input stream. It supports binary format and XMI
   * but not XCAS
   * 
   * @param casInputStream
   *          The input stream containing the CAS
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
   * This method tries to guess the format of the input stream. It supports binary format and XMI
   * but not XCAS
   * 
   * @param casInputStream
   *          The input stream containing the CAS
   * @param tsInputStream
   *          The optional input stream containing the type system
   * @param aCAS
   *          The CAS that should be filled
   * @param leniently
   *          ignore feature structures of non-existing types
   * @return the SerialFormat of the loaded CAS
   * @throws IOException
   *           - Problem loading from given InputStream
   * @throws IllegalArgumentException
   *           - when trying to load XCAS
   */
  public static SerialFormat load(InputStream casInputStream, InputStream tsInputStream, CAS aCAS,
          boolean leniently) throws IOException {
    BufferedInputStream bis = new BufferedInputStream(casInputStream);
    bis.mark(32);
    byte[] headerXml = new byte[16];
    bis.read(headerXml);
    bis.reset();
    String start = new String(headerXml);
    if (start.startsWith("<?xml ")) {
      try {
        XmiCasDeserializer.deserialize(bis, aCAS, leniently);
        return SerialFormat.XMI;
      } catch (SAXException e) {
        throw new IllegalArgumentException(
                "Error parsing XMI file. XCAS format not supported for InputStream. Please use File, Path or URL interface.");
      }
    }
    return loadBinary(bis, tsInputStream, aCAS);
  }

  /**
   * Read CAS from the specified stream.
   * 
   * @param is
   *          The input stream of the CAS
   * @param typeIS
   *          Optional stream from which typesystem information may be read. This is only used if
   *          the binary format read from the primary input stream does not already contain
   *          typesystem information.
   * @param aCAS
   *          the CAS in which the input stream will be deserialized
   * @return the SerialFormat of the loaded CAS
   * @throws IOException
   *           - Problem loading from given InputStream
   */
  private static SerialFormat loadBinary(InputStream is, InputStream typeIS, CAS aCAS)
          throws IOException {
    CASMgrSerializer casMgr = null;
    if (typeIS != null) {
      casMgr = readCasManager(typeIS);
    }

    return loadBinary(is, casMgr, aCAS);
  }

  /**
   * Read CAS from the specified stream.
   * 
   * @param is
   *          The input stream of the CAS
   * @param casMgr
   *          Optional CASMgrSerializer. This is only used if the binary format read from the
   *          primary input stream does not already contain typesystem information.
   * @param aCAS
   *          the CAS in which the input stream will be deserialized
   * @return the SerialFormat of the loaded CAS
   * @throws IOException
   *           - Problem loading from given InputStream
   */
  private static SerialFormat loadBinary(InputStream is, CASMgrSerializer casMgr, CAS aCAS)
          throws IOException {
    try {
      TypeSystemImpl ts = null;
      DataInputStream dis = CommonSerDes.maybeWrapToDataInputStream(is);
      Header header = CommonSerDes.readHeader(dis);
      dis.reset();
      // Check if this is original UIMA CAS format or an extended format with type system

      // If it is UIMA with type system format, read the type system
      if (header.isForm6() && header.isTypeSystemIncluded()) {
        // read additional header again
        CommonSerDes.readHeader(dis);
        ObjectInputStream ois = new ObjectInputStream(dis);
        CASMgrSerializer casMgrSerializer = (CASMgrSerializer) ois.readObject();
        ts = casMgrSerializer.getTypeSystem();
        ts.commit();
      }

      if (ts != null) {
        // Only format 6 can have type system information
        deserializeCAS(aCAS, dis, ts, null);
        return SerialFormat.COMPRESSED_FILTERED_TS;
      } else {

        // Check if this is a UIMA binary CAS stream
        if (header.isForm4()) {
          deserializeCAS(aCAS, dis);
          return SerialFormat.COMPRESSED;
        } else if (header.isForm6()) {
          if (ts == null && casMgr != null) {
            // If there was not type system in the file but one is set, then load it
            ts = casMgr.getTypeSystem();
            ts.commit();
          }
          deserializeCAS(aCAS, dis, ts, null);
          return SerialFormat.COMPRESSED_FILTERED;
        } else if (header.getSeqVersionNbr() == 1) {
          deserializeCAS(aCAS, dis);
          return SerialFormat.BINARY;
        } else {
          // read additional header again
          ObjectInputStream ois = new ObjectInputStream(dis);
          Object object = ois.readObject();
          if (object instanceof CASCompleteSerializer) {
            CASCompleteSerializer serializer = (CASCompleteSerializer) object;
            deserializeCASComplete(serializer, (CASImpl) aCAS);
            return SerialFormat.SERIALIZED_TS;
          } else if (object instanceof CASSerializer) {
            CASCompleteSerializer serializer;
            if (casMgr != null) {
              // Annotations and CAS metadata saved separately
              serializer = new CASCompleteSerializer();
              serializer.setCasMgrSerializer(casMgr);
              serializer.setCasSerializer((CASSerializer) object);
            } else {
              // Expecting that CAS is already initialized as required
              serializer = serializeCASComplete((CASImpl) aCAS);
              serializer.setCasSerializer((CASSerializer) object);
            }
            deserializeCASComplete(serializer, (CASImpl) aCAS);
            return SerialFormat.SERIALIZED;
          } else {
            throw new IOException("Unknown serialized object found with type ["
                    + object.getClass().getName() + "]");
          }
        }

      }
    } catch (ResourceInitializationException e) {
      throw new IOException(e);
    } catch (ClassNotFoundException e) {
      throw new IOException(e);
    } finally {
      closeQuitely(is);
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
   *          The output stream for the CAS
   * @param typeOS
   *          Optional output stream for type system information. Only used if the format does not
   *          support storing typesystem information directly in the main output file.
   * @param format
   *          The SerialFormat in which the CAS should be stored.
   * @throws IOException
   *           - Problem saving to the given InputStream
   */
  public static void save(CAS aCas, OutputStream docOS, OutputStream typeOS, SerialFormat format)
          throws IOException {
    boolean typeSystemWritten = false;
    DataOutputStream dos = CommonSerDes.maybeWrapToDataOutputStream(docOS);
    try {
      switch (format) {
        case XMI:
          XmiCasSerializer.serialize(aCas, docOS);
          break;
        case XCAS:
          XCASSerializer xcasSerializer = new XCASSerializer(aCas.getTypeSystem());
          XMLSerializer xmlSerialzer = new XMLSerializer(docOS, true);
          xcasSerializer.serialize(aCas, xmlSerialzer.getContentHandler());
          break;
        case SERIALIZED:
        // Java-serialized CAS without type system
        {
          CASSerializer serializer = new CASSerializer();
          serializer.addCAS((CASImpl) aCas);
          ObjectOutputStream objOS = new ObjectOutputStream(docOS);
          objOS.writeObject(serializer);
          objOS.flush();
        }
          break;
        case SERIALIZED_TS:
        // Java-serialized CAS with type system
        {
          ObjectOutputStream objOS = new ObjectOutputStream(docOS);
          CASCompleteSerializer serializer = serializeCASComplete((CASImpl) aCas);
          objOS.writeObject(serializer);
          objOS.flush();
          typeSystemWritten = true; // Embedded type system
        }
          break;
        case BINARY:
          // Java-serialized CAS without type system
          serializeCAS(aCas, docOS);
          break;
        case COMPRESSED:
          // Binary compressed CAS without type system (form 4)
          serializeWithCompression(aCas, docOS);
          break;
        case COMPRESSED_FILTERED:
          // Binary compressed CAS (form 6)
          serializeWithCompression(aCas, docOS, aCas.getTypeSystem());
          break;
        case COMPRESSED_FILTERED_TS:
          // Binary compressed CAS (form 6)
          // ... with embedded Java-serialized type system
          Header additionalHeader = CommonSerDes.createHeader().form6().typeSystemIncluded();
          additionalHeader.write(dos);
          writeTypeSystem(aCas, docOS);
          typeSystemWritten = true; // Embedded type system
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

    if (typeOS != null && !typeSystemWritten) {
      writeTypeSystem(aCas, typeOS);
      typeSystemWritten = true;
    }
  }

  private static CASMgrSerializer readCasManager(InputStream aIs) throws IOException {
    CASMgrSerializer casMgrSerializer;

    try {
      ObjectInputStream is = new ObjectInputStream(aIs);
      casMgrSerializer = (CASMgrSerializer) is.readObject();
    } catch (ClassNotFoundException e) {
      throw new IOException(e);
    }

    return casMgrSerializer;
  }

  private static void writeTypeSystem(CAS aCas, OutputStream aOS) throws IOException {
    ObjectOutputStream typeOS = new ObjectOutputStream(aOS);
    CASMgrSerializer casMgrSerializer = serializeCASMgr((CASImpl) aCas);
    typeOS.writeObject(casMgrSerializer);
    typeOS.flush();
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
