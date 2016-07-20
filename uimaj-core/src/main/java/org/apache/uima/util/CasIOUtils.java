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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.cas.impl.CommonSerDes;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

public class CasIOUtils {

  public static final byte[] UIMA_TS_HEADER = new byte[] { 'U', 'I', 'M', 'A', 'T', 'S' };

  public static final byte[] UIMA_HEADER = new byte[] { 'U', 'I', 'M', 'A' };

  /**
   * 
   * @param casPath
   *          The path containing the CAS
   * @param aCAS
   *          The CAS that should be filled
   * @throws IOException
   */
  public static SerialFormat load(Path casPath, CAS aCAS) throws IOException {

    return load(casPath, null, aCAS, false);
  }

  /**
   * 
   * @param casPath
   *          The path containing the CAS
   * @param tsPath
   *          The optional path containing the type system
   * @param aCAS
   *          The CAS that should be filled
   * @param lentiently
   *          ignore feature structures of non-existing types
   * @throws IOException
   */
  public static SerialFormat load(Path casPath, Path tsPath, CAS aCAS, boolean leniently)
          throws IOException {

    URL casUrl = casPath.toUri().toURL();
    URL tsUrl = tsPath == null ? null : tsPath.toUri().toURL();
    return load(casUrl, tsUrl, aCAS, leniently);
  }

  /**
   * 
   * @param casFile
   *          The file containing the CAS
   * @param aCAS
   *          The CAS that should be filled
   * @throws IOException
   */
  public static SerialFormat load(File casFile, CAS aCAS) throws IOException {

    return load(casFile, null, aCAS, false);
  }

  /**
   * 
   * @param casFile
   *          The file containing the CAS
   * @param tsFile
   *          The optional file containing the type system
   * @param aCAS
   *          The CAS that should be filled
   * @param lentiently
   *          ignore feature structures of non-existing types
   * @throws IOException
   */
  public static SerialFormat load(File casFile, File tsFile, CAS aCAS, boolean lentiently)
          throws IOException {

    URL casUrl = casFile.toURI().toURL();
    URL tsUrl = tsFile == null ? null : tsFile.toURI().toURL();
    return load(casUrl, tsUrl, aCAS, lentiently);
  }

  /**
   * 
   * @param casUrl
   *          The url containing the CAS
   * @param aCAS
   *          The CAS that should be filled
   * @throws IOException
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
   * @param lentiently
   *          ignore feature structures of non-existing types
   * @throws IOException
   */
  public static SerialFormat load(URL casUrl, URL tsUrl, CAS aCAS, boolean lentietly)
          throws IOException {
    String path = casUrl.getPath().toLowerCase();
    if (path.endsWith(".xmi")) {
      try {
        XmiCasDeserializer.deserialize(casUrl.openStream(), aCAS, lentietly);
        return SerialFormat.XMI;
      } catch (SAXException e) {
        throw new IOException(e);
      }
    } else if (path.endsWith(".xcas") || path.endsWith(".xml")) {
      try {
        XCASDeserializer.deserialize(casUrl.openStream(), aCAS, lentietly);
        return SerialFormat.XCAS;
      } catch (SAXException e) {
        throw new IOException(e);
      }
    }
    return loadBinary(casUrl.openStream(), tsUrl == null ? null : tsUrl.openStream(), aCAS);
  }

  /**
   * This method tries to guess the format of the input stream. It supports binary format and XMI
   * but not XCAS
   * 
   * @param casInputStream
   *          The input stream containing the CAS
   * @param aCAS
   *          The CAS that should be filled
   * @throws IOException
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
   * @param lentiently
   *          ignore feature structures of non-existing types
   * @throws IOException
   */
  public static SerialFormat load(InputStream casInputStream, InputStream tsInputStream,
          CAS aCAS, boolean lentiently) throws IOException {
    BufferedInputStream bis = new BufferedInputStream(casInputStream);
    bis.mark(32);
    byte[] headerXml = new byte[16];
    bis.read(headerXml);
    bis.reset();
    String start = new String(headerXml);
    if (start.startsWith("<?xml ")) {
      try {
        XmiCasDeserializer.deserialize(bis, aCAS, lentiently);
        return SerialFormat.XMI;
      } catch (SAXException e) {
        throw new IOException(e);
      }
    }
    return loadBinary(bis, tsInputStream, aCAS);
  }

  /**
   * Read CAS from the specified stream.
   * 
   * @param is
   *          The input stream of the CAS
   * @param aCAS
   *          the CAS in which the inpout stream will be deserialized
   * @throws IOException
   */
  public static SerialFormat loadBinary(InputStream is, CAS aCAS) throws IOException {
    return loadBinary(is, (CASMgrSerializer) null, aCAS);
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
   * @throws IOException
   */
  public static SerialFormat loadBinary(InputStream is, InputStream typeIS, CAS aCAS)
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
   * @throws IOException
   */
  public static SerialFormat loadBinary(InputStream is, CASMgrSerializer casMgr, CAS aCAS)
          throws IOException {
    try {
      BufferedInputStream bis = new BufferedInputStream(is);
      TypeSystemImpl ts = null;

      // Check if this is original UIMA CAS format or an extended format with type system
      bis.mark(32);
      DataInputStream dis = new DataInputStream(bis);

      byte[] header = new byte[UIMA_TS_HEADER.length];
      dis.read(header);

      // If it is UIMA with type system format, read the type system
      if (Arrays.equals(header, UIMA_TS_HEADER)) {
        ObjectInputStream ois = new ObjectInputStream(bis);
        CASMgrSerializer casMgrSerializer = (CASMgrSerializer) ois.readObject();
        ts = casMgrSerializer.getTypeSystem();
        ts.commit();
      } else {
        bis.reset();
      }

      if (ts != null) {
        // Only format 6 can have type system information
        deserializeCAS(aCAS, bis, ts, null);
        return SerialFormat.COMPRESSED_FILTERED_TS;
      } else {

        // Check if this is a UIMA binary CAS stream
        byte[] header4 = new byte[UIMA_HEADER.length];
        dis.read(header4);

        if (header4[0] != 'U') {
          // ArrayUtils.reverse(header4);
          for (int i = 0; i < header4.length / 2; i++) {
            byte temp = header4[i];
            header4[i] = header4[header4.length - i - 1];
            header4[header4.length - i - 1] = temp;
          }
        }

        // Peek into the version
        int version = dis.readInt();
        int version1 = dis.readInt();
        bis.reset();

        if (Arrays.equals(header4, UIMA_HEADER)) {
          // It is a binary CAS stream

          if ((version & 4) == 4 && (version1 != 0)) {
            // This is a form 6
            if (ts == null && casMgr != null) {
              // If there was not type system in the file but one is set, then load it
              ts = casMgr.getTypeSystem();
              ts.commit();
            }
            deserializeCAS(aCAS, bis, ts, null);
            return SerialFormat.COMPRESSED_FILTERED;
          } else {
            // This is a form 0 or 4
            deserializeCAS(aCAS, bis);
            if (version == 4) {
              return SerialFormat.COMPRESSED;
            }
            return SerialFormat.BINARY;
          }
        } else {
          // If it is not a UIMA binary CAS stream and not xml, assume it is output from
          // SerializedCasWriter
          ObjectInputStream ois = new ObjectInputStream(bis);
          Object object = ois.readObject();
          if (object instanceof CASCompleteSerializer) {
            CASCompleteSerializer serializer = (CASCompleteSerializer) object;
            deserializeCASComplete(serializer, (CASImpl) aCAS);
            return SerialFormat.SERILALIZED_TS;
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
            return SerialFormat.SERILALIZED;
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
      if (is != null) {
        is.close();
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
   * @param formatName
   *          The format string in which the CAS should be stored.
   * @throws IOException
   */
  public static void save(CAS aCas, OutputStream docOS, String formatName) throws IOException {
    SerialFormat format = SerialFormat.valueOf(formatName);
    save(aCas, docOS, null, format);
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
   */
  public static void save(CAS aCas, OutputStream docOS, SerialFormat format)
          throws IOException {
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
   */
  public static void save(CAS aCas, OutputStream docOS, OutputStream typeOS,
          SerialFormat format) throws IOException {
    boolean typeSystemWritten = false;
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
        case SERILALIZED:
        // Java-serialized CAS without type system
        {
          CASSerializer serializer = new CASSerializer();
          serializer.addCAS((CASImpl) aCas);
          ObjectOutputStream objOS = new ObjectOutputStream(docOS);
          objOS.writeObject(serializer);
          objOS.flush();
        }
          break;
        case SERILALIZED_TS:
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
          writeHeader(docOS);
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

    // To support writing to ZIPs, the type system must be written separately from the CAS data
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

  private static void writeHeader(OutputStream aOS) throws IOException {
    DataOutputStream dataOS = new DataOutputStream(aOS);
    dataOS.write(UIMA_TS_HEADER);
    dataOS.flush();
  }

  private static void writeTypeSystem(CAS aCas, OutputStream aOS) throws IOException {
    ObjectOutputStream typeOS = new ObjectOutputStream(aOS);
    CASMgrSerializer casMgrSerializer = serializeCASMgr((CASImpl) aCas);
    typeOS.writeObject(casMgrSerializer);
    typeOS.flush();
  }
}
