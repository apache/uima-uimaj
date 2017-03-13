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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.UriUtils;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.SAXException;

/**
 * A simple CAS consumer that generates XCAS (XML representation of the CAS) files in the
 * filesystem. The output files are written using the UTF-8 charset.
 * <p>
 * This CAS Consumer takes one parameters:
 * <ul>
 * <li><code>OutputDirectory</code> - path to directory into which output files will be written</li>
 * </ul>
 * <p>
 * The XML descriptor for this collection reader is stored in the uima.jar file as
 * <code>org/apache/uima/util/XCasWriterCasConsumer.xml</code>. It can be accessed via the static
 * method {@link #getDescription()}, which parses the descriptor and returns a
 * {@link CasConsumerDescription} object.
 * 
 * 
 */
public class XCasWriterCasConsumer extends CasConsumer_ImplBase {
  /**
   * Name of configuration parameter that must be set to the path of a directory into which the
   * output files will be written.
   */
  public static final String PARAM_OUTPUTDIR = "OutputDirectory";

  private File mOutputDir;

  private int mDocNum;

  public void initialize() throws ResourceInitializationException {
    mDocNum = 0;
    mOutputDir = new File(((String) getConfigParameterValue(PARAM_OUTPUTDIR)).trim());
    if (!mOutputDir.exists()) {
      mOutputDir.mkdirs();
    }
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
    FSIterator it = jcas.getAnnotationIndex(SourceDocumentInformation.type).iterator();
    File outFile = null;
    if (it.hasNext()) {
      SourceDocumentInformation fileLoc = (SourceDocumentInformation) it.next();
      File inFile;
      try {
        // handle blanks in path
        // https://issues.apache.org/jira/browse/UIMA-1748
        // use 3 arg form of URI Constructor to properly quote any otherwise illegal chars such as blank
        // https://issues.apache.org/jira/browse/UIMA-2097
        URI uri = UriUtils.quote(fileLoc.getUri());
        inFile = new File(uri);

        String outFileName = inFile.getName();
        if (fileLoc.getOffsetInSource() > 0) {
          outFileName += fileLoc.getOffsetInSource();
        }
        outFile = new File(mOutputDir, outFileName);
      } catch (URISyntaxException e) {
        // invalid URL, use default processing below
      }
    }
    if (outFile == null) {
      outFile = new File(mOutputDir, "doc" + mDocNum++);
    }
    // serialize XCAS and write to output file
    try {
      writeXCas(jcas.getCas(), outFile);
    } catch (IOException e) {
      throw new ResourceProcessException(e);
    } catch (SAXException e) {
      throw new ResourceProcessException(e);
    }
  }

  /**
   * Serialize a CAS to a file in XCAS format
   * 
   * @param aCas
   *          CAS to serialize
   * @param name
   *          output file
   * 
   * @throws IOException
   *           if an I/O failure occurs
   * @throws SAXException
   *           if an error occurs generating the XML text
   */
  private void writeXCas(CAS aCas, File name) throws IOException, SAXException {
    FileOutputStream out = null;

    try {
      out = new FileOutputStream(name);
      XCASSerializer ser = new XCASSerializer(aCas.getTypeSystem());
      XMLSerializer sax2xml = new XMLSerializer(out, false);
      ser.serialize(aCas, sax2xml.getContentHandler());
    } finally {
      if (out != null) {
        out.close();
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
    InputStream descStream = XCasWriterCasConsumer.class
            .getResourceAsStream("XCasWriterCasConsumer.xml");
    return UIMAFramework.getXMLParser().parseCasConsumerDescription(
            new XMLInputSource(descStream, null));
  }

  public static URL getDescriptorURL() {
    return XCasWriterCasConsumer.class.getResource("XCasWriterCasConsumer.xml");
  }
}
