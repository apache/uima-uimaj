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

package org.apache.uima.examples.xmi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.ecore.UimaTypeSystem2Ecore;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.TypeSystemUtil;
import org.apache.uima.util.XMLSerializer;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.xml.sax.SAXException;

/**
 * A variation of the XmiWriterCasConsumer that also saves the Type System to an Ecore file and
 * links all of the XMI files to it via their schemaLocation attribute. This allows integration with
 * the Eclipse Modeling Framework (EMF). This class requires the EMF libraries common.jar,
 * ecore.jar, and ecore.xmi.jar to be in the classpath.
 * <p>
 * This CAS Consumer takes two parameters:
 * <ul>
 * <li><code>OutputDirectory</code> - path to directory into which output files will be written</li>
 * <li><code>WriteEcoreTypeSystem</code> - if true, writes the type system to an Ecore file and
 * links all of the XMI files to it via their schemaLocation attribute. Requires EMF libraries in
 * the classpath if this option is set to true.</li>
 * </ul>
 */
public class XmiEcoreCasConsumer extends CasConsumer_ImplBase {
  /**
   * Name of configuration parameter that must be set to the path of a directory into which the
   * output files will be written.
   */
  public static final String PARAM_OUTPUTDIR = "OutputDirectory";

  public static final String PARAM_WRITE_ECORE_TYPESYSTEM = "WriteEcoreTypeSystem";

  private File mOutputDir;

  private int mDocNum;

  private boolean writeEcoreTypeSystem;

  private boolean isModelGenerated = false;

  private Map schemaLocationMap = null;

  public void initialize() throws ResourceInitializationException {
    mDocNum = 0;
    mOutputDir = new File((String) getConfigParameterValue(PARAM_OUTPUTDIR));
    if (!mOutputDir.exists()) {
      mOutputDir.mkdirs();
    }
    writeEcoreTypeSystem = Boolean.TRUE.equals(getUimaContext().getConfigParameterValue(
            PARAM_WRITE_ECORE_TYPESYSTEM));
  }

  /**
   * Processes a CAS. In this case, the CAS is converted to XMI and written into the output file .
   * 
   * @param aCAS
   *          The CAS to write to XMI
   * 
   * @throws ResourceProcessException
   *           if there is an error in processing the Resource
   * 
   * @see org.apache.uima.collection.base_cpm.CasObjectProcessor#processCas(org.apache.uima.cas.CAS)
   */
  public void processCas(CAS aCAS) throws ResourceProcessException {
    String modelFileName = null;

    JCas jcas;
    try {
      jcas = aCAS.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    // retreive the filename of the input file from the CAS
    FSIterator it = jcas.getAnnotationIndex(SourceDocumentInformation.type).iterator();
    File outFile = null;
    if (it.hasNext()) {
      SourceDocumentInformation fileLoc = (SourceDocumentInformation) it.next();
      File inFile;
      try {
        inFile = new File(new URL(fileLoc.getUri()).getPath());
        String outFileName = inFile.getName();
        if (fileLoc.getOffsetInSource() > 0) {
          outFileName += ("_" + fileLoc.getOffsetInSource());
        }
        outFileName += ".xmi";
        outFile = new File(mOutputDir, outFileName);
        modelFileName = mOutputDir.getAbsolutePath() + "/" + inFile.getName() + ".ecore";
      } catch (MalformedURLException e1) {
        // invalid URL, use default processing below
      }
    }
    if (outFile == null) {
      outFile = new File(mOutputDir, "doc" + mDocNum++);
    }
    // serialize XCAS and write to output file
    try {
      writeXmi(jcas.getCas(), outFile, modelFileName);
    } catch (IOException e) {
      throw new ResourceProcessException(e);
    } catch (SAXException e) {
      throw new ResourceProcessException(e);
    }
  }

  /**
   * Serialize a CAS to a file in XMI format
   * 
   * @param aCas
   *          CAS to serialize
   * @param name
   *          output file
   * @throws SAXException
   * @throws Exception
   * 
   * @throws ResourceProcessException
   */
  private void writeXmi(CAS aCas, File name, String modelFileName) throws IOException, SAXException {
    FileOutputStream out = null;

    try {
      // Generate E-core for type system, but only once
      if (writeEcoreTypeSystem && !isModelGenerated) {
        TypeSystemDescription tsDesc = TypeSystemUtil.typeSystem2TypeSystemDescription(aCas
                .getTypeSystem());
        // register default resource factory
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*",
                new XMIResourceFactoryImpl());
        ResourceSet resourceSet = new ResourceSetImpl();
        URI outputURI = URI.createFileURI(new File(mOutputDir, "typesystem.ecore")
                .getAbsolutePath());
        Resource outputResource = resourceSet.createResource(outputURI);
        schemaLocationMap = new HashMap();
        try {
          UimaTypeSystem2Ecore
                  .uimaTypeSystem2Ecore(tsDesc, outputResource, null, schemaLocationMap);
        } catch (InvalidXMLException e) {
          // this should not happen. TypeSystemUtil.typeSystem2TypeSystemDescription
          // should never produce an invalid TypeSystemDescription!
          throw new UIMARuntimeException(e);
        }
        outputResource.save(null);
        isModelGenerated = true;
      }

      // write XMI
      out = new FileOutputStream(name);
      XmiCasSerializer ser = new XmiCasSerializer(aCas.getTypeSystem(), schemaLocationMap);
      XMLSerializer xmlSer = new XMLSerializer(out, false);
      ser.serialize(aCas, xmlSer.getContentHandler());
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }
}
