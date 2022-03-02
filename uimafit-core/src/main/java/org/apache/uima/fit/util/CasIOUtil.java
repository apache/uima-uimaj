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
package org.apache.uima.fit.util;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasIOUtils;
import org.xml.sax.SAXException;

/**
 * Convenience methods for loading and saving CAS to disk.
 * 
 * @deprecated Since UIMA 2.9.0 the core class {@link CasIOUtils} should be used.
 */
@Deprecated
public class CasIOUtil {

  private CasIOUtil() {
    // This class is not meant to be instantiated
  }

  /**
   * This method loads the contents of an XMI or XCAS file into the given CAS. The file type is
   * detected by the extension.
   * 
   * @param aCas
   *          the target CAS
   * @param aFile
   *          the file to read from
   * @throws IOException
   *           if there is a problem reading the file
   * @deprecated Use {@link CasIOUtils#load(java.net.URL, CAS)} instead.
   */
  @Deprecated
  public static void readCas(CAS aCas, File aFile) throws IOException {
    String lowerCaseFileName = aFile.getName().toLowerCase();
    if (lowerCaseFileName.endsWith(".xmi")) {
      CasIOUtil.readXmi(aCas, aFile);
    } else if (lowerCaseFileName.endsWith(".xcas")) {
      CasIOUtil.readXCas(aCas, aFile);
    } else {
      throw new IllegalArgumentException("Unknown file extension: [" + aFile + "] ");
    }
  }

  /**
   * 
   * @param aCas
   *          the target CAS
   * @param aFile
   *          the file to read from
   * @throws IOException
   *           if there is a problem reading the file
   * @deprecated Use {@link CasIOUtils#load(java.net.URL, CAS)} instead.
   */
  @Deprecated
  public static void readXmi(CAS aCas, File aFile) throws IOException {
    InputStream is = null;
    try {
      is = new FileInputStream(aFile);
      XmiCasDeserializer.deserialize(is, aCas);
    } catch (SAXException e) {
      IOException ioe = new IOException(e.getMessage());
      ioe.initCause(e);
      throw ioe; // NOPMD
      // If we were using Java 1.6 and add the wrapped exception to the IOException
      // constructor, we would not get a warning here
    } finally {
      closeQuietly(is);
    }
  }

  /**
   * 
   * @param aCas
   *          the source CAS
   * @param aFile
   *          the file to write to
   * @throws IOException
   *           if there is a problem writing the file
   * @deprecated Use {@link CasIOUtils#save(CAS, OutputStream, org.apache.uima.cas.SerialFormat)}
   *             with {@link SerialFormat#XMI} instead.
   */
  @Deprecated
  public static void writeXmi(CAS aCas, File aFile) throws IOException {
    OutputStream os = null;
    try {
      os = new FileOutputStream(aFile);
      XmiCasSerializer.serialize(aCas, os);
    } catch (SAXException e) {
      IOException ioe = new IOException(e.getMessage());
      ioe.initCause(e);
      throw ioe; // NOPMD
      // If we were using Java 1.6 and add the wrapped exception to the IOException
      // constructor, we would not get a warning here
    } finally {
      closeQuietly(os);
    }
  }

  /**
   * @param aCas
   *          the target CAS
   * @param aFile
   *          the file to read from
   * @throws IOException
   *           if there is a problem reading the file
   * @deprecated Use {@link CasIOUtils#load(java.net.URL, CAS)} instead.
   */
  @Deprecated
  public static void readXCas(CAS aCas, File aFile) throws IOException {
    InputStream is = null;
    try {
      is = new FileInputStream(aFile);
      XCASDeserializer.deserialize(is, aCas);
    } catch (SAXException e) {
      IOException ioe = new IOException(e.getMessage());
      ioe.initCause(e);
      throw ioe; // NOPMD
      // If we were using Java 1.6 and add the wrapped exception to the IOException
      // constructor, we would not get a warning here
    } finally {
      closeQuietly(is);
    }
  }

  /**
   * 
   * @param aCas
   *          the source CAS
   * @param aFile
   *          the file to write to
   * @throws IOException
   *           if there is a problem writing the file
   * @deprecated Use {@link CasIOUtils#save(CAS, OutputStream, org.apache.uima.cas.SerialFormat)}
   *             with {@link SerialFormat#XCAS} instead.
   */
  @Deprecated
  public static void writeXCas(CAS aCas, File aFile) throws IOException {
    OutputStream os = null;
    try {
      os = new FileOutputStream(aFile);
      XCASSerializer.serialize(aCas, os);
    } catch (SAXException e) {
      IOException ioe = new IOException(e.getMessage());
      ioe.initCause(e);
      throw ioe; // NOPMD
      // If we were using Java 1.6 and add the wrapped exception to the IOException
      // constructor, we would not get a warning here
    } finally {
      closeQuietly(os);
    }
  }

  /**
   * This method loads the contents of an XMI or XCAS file into the given CAS. The file type is
   * detected by the extension.
   * 
   * @param aJCas
   *          the target JCas
   * @param aFile
   *          the file to read from
   * @throws IOException
   *           if there is a problem reading the file
   * @deprecated Use {@link CasIOUtils#load(java.net.URL, CAS)} instead.
   */
  @Deprecated
  public static void readJCas(JCas aJCas, File aFile) throws IOException {
    CasIOUtil.readCas(aJCas.getCas(), aFile);
  }

  /**
   * 
   * @param aJCas
   *          the target JCas
   * @param aFile
   *          the file to read from
   * @throws IOException
   *           if there is a problem reading the file
   * @deprecated Use {@link CasIOUtils#load(java.net.URL, CAS)} instead.
   */
  @Deprecated
  public static void readXmi(JCas aJCas, File aFile) throws IOException {
    CasIOUtil.readXmi(aJCas.getCas(), aFile);
  }

  /**
   * 
   * @param aJCas
   *          the source JCas
   * @param aFile
   *          the file to write to
   * @throws IOException
   *           if there is a problem writing the file
   * @deprecated Use {@link CasIOUtils#save(CAS, OutputStream, org.apache.uima.cas.SerialFormat)}
   *             with {@link SerialFormat#XMI} instead.
   */
  @Deprecated
  public static void writeXmi(JCas aJCas, File aFile) throws IOException {
    CasIOUtil.writeXmi(aJCas.getCas(), aFile);
  }

  /**
   * 
   * @param aJCas
   *          the target JCas
   * @param aFile
   *          the file to read from
   * @throws IOException
   *           if there is a problem reading the file
   * @deprecated Use {@link CasIOUtils#load(java.net.URL, CAS)} instead.
   */
  @Deprecated
  public static void readXCas(JCas aJCas, File aFile) throws IOException {
    CasIOUtil.readXCas(aJCas.getCas(), aFile);
  }

  /**
   * 
   * @param aJCas
   *          the source JCas
   * @param aFile
   *          the file to write to
   * @throws IOException
   *           if there is a problem writing the file
   * @deprecated Use {@link CasIOUtils#save(CAS, OutputStream, org.apache.uima.cas.SerialFormat)}
   *             with {@link SerialFormat#XCAS} instead.
   */
  @Deprecated
  public static void writeXCas(JCas aJCas, File aFile) throws IOException {
    CasIOUtil.writeXCas(aJCas.getCas(), aFile);
  }
}
