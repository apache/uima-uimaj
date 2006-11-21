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

package org.apache.uima.internal.util;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TemplatesHandler;
import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import org.apache.uima.UIMARuntimeException;

/**
 * Provides access to XInclude processing functionality, which is implemented using an XSL
 * stylesheet.
 * 
 * 
 */
public final class XIncluder {
  /**
   * Utility class - has private constructor
   */
  private XIncluder() {
  }

  /**
   * Creates a new TransformerHandler (implements SAX Content Handler) that processes XIncludes. Use
   * the {@link TransformerHandler#setResult(javax.xml.transform.Result)} method to specify where
   * the output of the transform is sent.
   * 
   * @return a new XInclude Handler
   * 
   * @throws TransformerConfigurationException
   *           if the XInclude transformer could not be constructed
   */
  public static TransformerHandler newXIncludeHandler() throws TransformerConfigurationException {
    return newXIncludeHandler(false);
  }

  /**
   * Creates a new TransformerHandler (implements SAX Content Handler) that processes XIncludes. Use
   * the {@link TransformerHandler#setResult(javax.xml.transform.Result)} method to specify where
   * the output of the transform is sent.
   * <p>
   * If <code>aLeavePlaceholderElements</code> is set to true, this will leave &lt;xincluded
   * href=""&gt; elements at the point where each xinclude occurred. The included content will then
   * be the children of the &lt;xincluded&gt; element. This option allows for retaining information
   * about the source of xincluded elements when necessary (for example, when editing an XML file
   * containing xincludes).
   * 
   * @return a new XInclude Handler
   * 
   * @throws TransformerConfigurationException
   *           if the XInclude transformer could not be constructed
   */
  public static TransformerHandler newXIncludeHandler(boolean aLeavePlaceholderElements)
                  throws TransformerConfigurationException {
    // Xinclude resolution is not supported in the Xalan bundled with Java 1.5.
    // If we're in that situation, just return the identity transformer. Later,
    // unresolved xi:includes will be reported and a meaningful error message generated.
    if (xincludesDisabledDueToIncorrectXalanVersion) {
      return sTFactory.newTransformerHandler(); // identity transformer
    }

    TransformerHandler handler = sTFactory.newTransformerHandler(getXIncludeTemplates());
    if (aLeavePlaceholderElements) {
      handler.getTransformer().setParameter("leavePlaceholderElements", "true");
    }
    return handler;
  }

  /**
   * Get the XSL Templates to be used for XInclude Processing. The first time this method is called,
   * the xinclude.xsl stylesheet will be parsed to obtain the templates.
   * 
   * @return XSL Templates for XInclude processing
   */
  protected static Templates getXIncludeTemplates() {
    if (sXIncludeTemplates == null) {
      try {
        // Create a Templates ContentHandler to handle parsing of the
        // stylesheet.
        TemplatesHandler templatesHandler = sTFactory.newTemplatesHandler();

        // Parse the xinclude.xsl stylesheet
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        XMLReader reader = factory.newSAXParser().getXMLReader();

        reader.setContentHandler(templatesHandler);
        InputStream xslStream = XIncluder.class.getResourceAsStream("/xinclude.xsl");
        if (xslStream == null) {
          throw new UIMARuntimeException(new FileNotFoundException("xinclude.xsl"));
        }
        try {
          reader.parse(new InputSource(xslStream));
        } finally {
          xslStream.close();
        }
        // Get the Templates object (generated during the parsing of the stylesheet)
        // from the TemplatesHandler.
        sXIncludeTemplates = templatesHandler.getTemplates();
      } catch (Exception e) {
        throw new UIMARuntimeException(e);
      }
    }
    return sXIncludeTemplates;
  }

  /**
   * SAX Transformer Factory - used to create XSL Transformers for XInclude processing.
   */
  private static SAXTransformerFactory sTFactory;

  private static String debug;

  // The below is a workaround for the problem that Java 1.5's built-in Xalan doesn't
  // handle the stylesheet that we're using for XInclude resolution (because it uses
  // EXSLT dynamic extension functions). So if we're running under Java 1.5 and we
  // can't load the correct Xalan TransformerFactory, we disable XInclude processing.
  private static boolean xincludesDisabledDueToIncorrectXalanVersion = false;
  static {
    // check Java version - Java 1.5's built-in Xalan does not handle our XInclude styleshhet
    String javaVersion = System.getProperty("java.version");
    debug = System.getProperty("uima.debug.XIncluder");
    if (javaVersion.startsWith("1.5")) {
      final String XALAN_FACTORY_CLASSNAME = "org.apache.xalan.processor.TransformerFactoryImpl";
      dPrint("running Java 1.5, loading transformer class: " + XALAN_FACTORY_CLASSNAME);
      try {
        // see if proper Xalan class exists
        if (null != debug) {
          URL url = XIncluder.class.getResource('/' + XALAN_FACTORY_CLASSNAME.replace('.', '/')
                          + ".class");
          if (null == url) {
            dPrint("getResource returned a null URL");
          } else {
            dPrint("Loading TransformerFactoryImpl from " + url.toString());
          }
        }

        Class transformerFactoryClass = Class.forName(XALAN_FACTORY_CLASSNAME, true,
                        XIncluder.class.getClassLoader());
        dPrint("Loaded the TransformerFactoryImpl class OK");
        sTFactory = (SAXTransformerFactory) transformerFactoryClass.newInstance();
        dPrint("Created the TransformerFactoryClass instance OK");
      } catch (ClassNotFoundException e) {
        dPrint("Can't find the correct TransformerFactory class.  Disabling xincludes.");
        xincludesDisabledDueToIncorrectXalanVersion = true;
        sTFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
      } catch (InstantiationException e) {
        if (null != debug) {
          e.printStackTrace();
        }
        throw new UIMARuntimeException(e);
      } catch (IllegalAccessException e) {
        if (null != debug) {
          e.printStackTrace();
        }
        throw new UIMARuntimeException(e);
      }
    } else {
      dPrint("Not Java 1.5 - loading the TransformerFactoryImpl normally");
      sTFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
    }
  }

  private static void dPrint(String s) {
    if (null != debug)
      System.err.println("XIncluder debug: " + s);
  }

  /**
   * XSL Templates for XInclude processing - parsed from xinclude.xsl file in first call to
   * getXIncludeTemplates method.
   */
  private static Templates sXIncludeTemplates = null;
}
