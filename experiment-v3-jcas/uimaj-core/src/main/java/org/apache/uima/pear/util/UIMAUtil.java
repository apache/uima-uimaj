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

package org.apache.uima.pear.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.collection.CasInitializerDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.CustomResourceSpecifier;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;

/**
 * The <code>UIMAUtil</code> class provides convenient methods for handling UIMA specific objects.
 * 
 */

public class UIMAUtil {
  // UIMA component categories
  /**
   * Analysis Engine
   */
  public static final String ANALYSIS_ENGINE_CTG = "AE";

  /**
   * CAS Consumer
   */
  public static final String CAS_CONSUMER_CTG = "CC";

  /**
   * CAS Initializer
   */
  public static final String CAS_INITIALIZER_CTG = "CI";

  /**
   * Collection Reader
   */
  public static final String COLLECTION_READER_CTG = "CR";

  /**
   * CPE Configuration
   */
  public static final String CPE_CONFIGURATION_CTG = "CPE";

  /**
   * Type System
   */
  public static final String TYPE_SYSTEM_CTG = "TS";

  /**
   * Reusable Resource
   */
  public static final String REUSABLE_RESOURCE_CTG = "RR";

  // static attributes
  private static Hashtable<String, Exception> __errTableByUri = new Hashtable<String, Exception>();

  /**
   * Returns the last logged <code>Exception</code> object associated with a given XML descriptor
   * file.
   * 
   * @param xmlDescFile
   *          The given XML descriptor file.
   * @return The last logged <code>Exception</code> object associated with the given XML
   *         descriptor file.
   */
  public static Exception getLastErrorForXmlDesc(File xmlDescFile) {
    return __errTableByUri.get(xmlDescFile.getAbsolutePath());
  }

  /**
   * Returns the last logged <code>Exception</code> object associated with a given XML descriptor
   * URL.
   * 
   * @param xmlDescUrl
   *          The given XML descriptor URL.
   * @return The last logged <code>Exception</code> object associated with the given XML
   *         descriptor URL.
   */
  public static Exception getLastErrorForXmlDesc(URL xmlDescUrl) {
    return __errTableByUri.get(xmlDescUrl.toString());
  }

  /**
   * Identifies a given UIMA component/resource category based on its XML descriptor. If succeeded,
   * returns appropriate UIMA component category ID, otherwise returns <code>null</code>. If the
   * UIMA component category cannot be identified based on the given XML descriptor file, the
   * associated UIMA exception is logged and can be retrieved later by using the
   * <code>getLastErrorForXmlDesc()</code> method.
   * 
   * @param xmlDescFile
   *          The given component XML descriptor file.
   * @return UIMA component category ID or <code>null</code>, if the category cannot be identified
   *         based on the given XML descriptor file.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static synchronized String identifyUimaComponentCategory(File xmlDescFile)
          throws IOException {
    return identifyUimaComponentCategory(xmlDescFile, null);
  }

  /**
   * Identifies a given UIMA component/resource category based on its XML descriptor. If succeeded,
   * returns appropriate UIMA component category ID, otherwise returns <code>null</code>. If the
   * UIMA component category cannot be identified based on the given XML descriptor file, the
   * associated UIMA exception is logged and can be retrieved later by using the
   * <code>getLastErrorForXmlDesc()</code> method.
   * 
   * @param xmlDescUrl
   *          The given component XML descriptor URL.
   * @return UIMA component category ID or <code>null</code>, if the category cannot be identified
   *         based on the given XML descriptor file.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static synchronized String identifyUimaComponentCategory(URL xmlDescUrl)
          throws IOException {
    return identifyUimaComponentCategory(null, xmlDescUrl);
  }

  /**
   * Internal method that identifies a given UIMA component/resource category based on its XML
   * descriptor, passed as File or URL. If succeeded, returns appropriate UIMA component category
   * ID, otherwise returns <code>null</code>. If the UIMA component category cannot be identified
   * based on the given XML descriptor file, the associated UIMA exception is logged and can be
   * retrieved later by using the <code>getLastErrorForXmlDesc()</code> method.
   * 
   * @param xmlDescFile
   *          The given component XML descriptor file.
   * @param xmlDescUrl
   *          The given component XML descriptor URL.
   * @return UIMA component category ID or <code>null</code>, if the category cannot be identified
   *         based on the given XML descriptor file.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  private static synchronized String identifyUimaComponentCategory(File xmlDescFile, URL xmlDescUrl)
          throws IOException {
    String uimaCompCtg = null;
    XMLInputSource xmlSource = null;
    String xmlDescUri = (xmlDescFile != null) ? xmlDescFile.getAbsolutePath() : xmlDescUrl.toString();
    try {
      // clean error message
      __errTableByUri.remove(xmlDescUri);
      // get XMLParser
      XMLParser xmlParser = UIMAFramework.getXMLParser();
      // create XML source
      xmlSource = (xmlDescFile != null) ? new XMLInputSource(xmlDescFile) : new XMLInputSource(
              xmlDescUrl);
      // parse XML source and create resource specifier
      ResourceSpecifier resourceSpec = null;
      try {
        resourceSpec = xmlParser.parseResourceSpecifier(xmlSource);
      } catch (UIMAException err) {
				__errTableByUri.put( xmlDescUri, err );
      } catch (UIMARuntimeException exc) {
				__errTableByUri.put( xmlDescUri, exc );
      }
      if (resourceSpec != null) { // AE | CR | CI | CC | CustomResourceSpecifier ?
        // identify UIMA resource category
        if (resourceSpec instanceof AnalysisEngineDescription) {
          uimaCompCtg = ANALYSIS_ENGINE_CTG;
        } else if (resourceSpec instanceof CollectionReaderDescription) {
          uimaCompCtg = COLLECTION_READER_CTG;
        } else if (resourceSpec instanceof CasInitializerDescription) {
          uimaCompCtg = CAS_INITIALIZER_CTG;
        } else if (resourceSpec instanceof CasConsumerDescription) {
          uimaCompCtg = CAS_CONSUMER_CTG;
        } else if (resourceSpec instanceof CustomResourceSpecifier) {
           // try to treat custom resource specifiers as AE
           uimaCompCtg = ANALYSIS_ENGINE_CTG;
        }
      }
      if (uimaCompCtg == null) { // CPE ?
        // refresh XML source object
        try {
          xmlSource.getInputStream().close();
        } catch (Exception e) {
        }
        xmlSource = (xmlDescFile != null) ? new XMLInputSource(xmlDescFile) : new XMLInputSource(xmlDescUrl);
        try {
          // try parsing CPE configuration
          xmlParser.parseCpeDescription(xmlSource);
          uimaCompCtg = CPE_CONFIGURATION_CTG;
          __errTableByUri.remove( xmlDescUri );
        } catch (UIMAException err) {
          __errTableByUri.put( xmlDescUri, err );
        } catch (UIMARuntimeException exc) {
          __errTableByUri.put( xmlDescUri, exc );
        }
      }
      if (uimaCompCtg == null) { // TS ?
        // refresh XML source object
        try {
          xmlSource.getInputStream().close();
        } catch (Exception e) {
        }
        xmlSource = (xmlDescFile != null) ? new XMLInputSource(xmlDescFile) : new XMLInputSource(xmlDescUrl);
        try {
          // try parsing TS description
          xmlParser.parseTypeSystemDescription(xmlSource);
          uimaCompCtg = TYPE_SYSTEM_CTG;
          __errTableByUri.remove( xmlDescUri );
        } catch (UIMAException err) {
          __errTableByUri.put( xmlDescUri, err );
        } catch (UIMARuntimeException exc) {
          __errTableByUri.put( xmlDescUri, exc );
        }
      }
      if (uimaCompCtg == null) { // RR ?
        // refresh XML source object
        try {
          xmlSource.getInputStream().close();
        } catch (Exception e) {
        }
        xmlSource = (xmlDescFile != null) ? new XMLInputSource(xmlDescFile) : new XMLInputSource(xmlDescUrl);
        try {
          // try parsing RES manager configuration
          xmlParser.parseResourceManagerConfiguration(xmlSource);
          uimaCompCtg = REUSABLE_RESOURCE_CTG;
          __errTableByUri.remove( xmlDescUri );
        } catch (UIMAException err) {
          __errTableByUri.put( xmlDescUri, err );
        } catch (UIMARuntimeException exc) {
          __errTableByUri.put( xmlDescUri, exc );
        }
      }
    } catch (IOException exc) {
      throw exc;
    } catch (Exception err) {
      __errTableByUri.put( xmlDescUri, err );
    } finally {
      if (xmlSource != null && xmlSource.getInputStream() != null)
        try {
          xmlSource.getInputStream().close();
        } catch (Exception e) {
        }
    }
    return uimaCompCtg;
  }
}
