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

package org.apache.uima.tools.components;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.CasToInlineXml;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.SAXException;

/**
 * A simple CAS consumer that generates inline XML and writes it to a file. UTF-8 encoding is used.
 * <p>
 * This CAS Consumer takes two parameters:
 * <ul>
 * <li><code>OutputDirectory</code> - path to directory into which output files will be written</li>
 * <li><code>OutputFilter</code> (optional) - an FSMatchConstraint which annotations must match
 * in order to be included in the output. If omitted, all annotations will be included in the
 * output.</li>
 * </ul>
 * <p>
 * The XML descriptor for this collection reader is stored in the uima-core.jar file as
 * <code>org/apache/uima/util/InlineXmlCasConsumer.xml</code>. It can be accessed via the static
 * method {@link #getDescription()}, which parses the descirptor and returns a
 * {@link CasConsumerDescription} object.
 * 
 * 
 */
public class InlineXmlCasConsumer extends CasConsumer_ImplBase {
  /**
   * Name of configuration parameter that must be set to the path of a directory into which the
   * output files will be written.
   */
  public static final String PARAM_OUTPUTDIR = "OutputDirectory";

  /**
   * Optional configuration parameter that specifies XCAS output files
   */
  public static final String PARAM_XCAS = "XCAS";

  private File mOutputDir;

  private CasToInlineXml cas2xml;

  private int mDocNum;

  private String mXCAS;
  
  private boolean mTEXT;

  public void initialize() throws ResourceInitializationException {
    mDocNum = 0;
    mOutputDir = new File(((String) getConfigParameterValue(PARAM_OUTPUTDIR)).trim());
    if (!mOutputDir.exists()) {
      mOutputDir.mkdirs();
    }
    cas2xml = new CasToInlineXml();
    mXCAS = (String) getConfigParameterValue(PARAM_XCAS);
    mTEXT = !("xcas".equalsIgnoreCase(mXCAS) || "xmi".equalsIgnoreCase(mXCAS));
  }

  /**
   * Processes the CasContainer which was populated by the TextAnalysisEngines. <br>
   * In this case, the CAS is converted to XML and written into the output file .
   * 
   * @param aCAS
   *          a CAS which has been populated by the Analysis Engines
   * 
   * @throws ResourceProcessException
   *           if there is an error in processing the Resource
   * 
   * @see org.apache.uima.collection.base_cpm.CasObjectProcessor#processCas(org.apache.uima.cas.CAS)
   */
  public void processCas(CAS aCAS) throws ResourceProcessException {
    JCas jcas;
    try {
      jcas = aCAS.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    // retrieve the filename of the input file from the CAS
    File outFile = null;
    boolean hasDefaultView = false;

    if (mTEXT) {
      // get the default View if it exists
      try {
        jcas = aCAS.getView(CAS.NAME_DEFAULT_SOFA).getJCas();
        hasDefaultView = true;
        FSIterator it = jcas.getAnnotationIndex(SourceDocumentInformation.type).iterator();
        if (it.hasNext()) {
          // get the output file name from the annotation in the CAS ...
          // ... note this is a little flakey if processing an XCAS file,
          // which could have such an annotation with a different name than the input XCAS file!
          // So we don't do this if XCAS output is specified.
          SourceDocumentInformation fileLoc = (SourceDocumentInformation) it.next();
          File inFile;
          inFile = new File(new URL(fileLoc.getUri()).getPath());
          outFile = new File(mOutputDir, inFile.getName());
        }
      } catch (CASRuntimeException e) {
        // default Sofa name does not exist, use default processing below
      } catch (CASException e) {
        // invalid something (??), use default processing below
      } catch (MalformedURLException e1) {
        // invalid URL, use default processing below
      }
    }
    if (outFile == null) {
      // default processing, create a name for the output file
      outFile = new File(mOutputDir, "doc" + mDocNum++);
    }
    // convert CAS to xml format and write to output file in UTF-8
    FileOutputStream outStream = null;
    try {
      outStream = new FileOutputStream(outFile);
      if (hasDefaultView) {
        String xmlAnnotations = cas2xml.generateXML(aCAS);
        outStream.write(xmlAnnotations.getBytes("UTF-8"));
      } else {
        XMLSerializer xmlSer = new XMLSerializer(outStream, false);
        if (mXCAS.equalsIgnoreCase("xcas")) {
          XCASSerializer ser = new XCASSerializer(aCAS.getTypeSystem());
          ser.serialize(aCAS, xmlSer.getContentHandler());
        }
        else {
          XmiCasSerializer ser = new XmiCasSerializer(aCAS.getTypeSystem());
          ser.serialize(aCAS, xmlSer.getContentHandler());
        }
      }
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    } catch (IOException e) {
      throw new ResourceProcessException(e);
    } catch (SAXException e) {
      throw new ResourceProcessException(e);
    } finally {
      if (outStream != null) {
        try {
          outStream.close();
        } catch (IOException e) {
          getLogger().log(Level.WARNING, e.getMessage(), e);
        }
      }
    }
  }

  /**
   * Parses and returns the descriptor for this collection reader. The descriptor is stored in the
   * uima.jar file and located using the ClassLoader.
   * 
   * @return an object containing all of the information parsed from the descriptor.
   * 
   * @throws InvalidXMLException
   *           if the descriptor is invalid or missing
   */
  public static CasConsumerDescription getDescription() throws InvalidXMLException {
    InputStream descStream = InlineXmlCasConsumer.class
            .getResourceAsStream("InlineXmlCasConsumer.xml");
    return UIMAFramework.getXMLParser().parseCasConsumerDescription(
            new XMLInputSource(descStream, null));
  }
  
  public static URL getDescriptorURL() {
    return InlineXmlCasConsumer.class.getResource("InlineXmlCasConsumer.xml");
  }
}
