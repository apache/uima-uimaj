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

package org.apache.uima.examples.cpe;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.collection.base_cpm.CasObjectProcessor;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

/**
 * An example of CAS Consumer. <br>
 * AnnotationPrinter prints to an output file all annotations in the CAS. <br>
 * Parameters needed by the AnnotationPrinter are
 * <ol>
 * <li> "outputFile" : file to which the output files should be written.</li>
 * </ol>
 * <br>
 * These parameters are set in the initialize method to the values specified in the descriptor file.
 * <br>
 * These may also be set by the application by using the setConfigParameterValue methods.
 * 
 * 
 */

public class AnnotationPrinter extends CasConsumer_ImplBase implements CasObjectProcessor {
  File outFile;

  FileWriter fileWriter;

  public AnnotationPrinter() {
  }

  /**
   * Initializes this CAS Consumer with the parameters specified in the descriptor.
   * 
   * @throws ResourceInitializationException
   *           if there is error in initializing the resources
   */
  public void initialize() throws ResourceInitializationException {

    // extract configuration parameter settings
    String oPath = (String) getUimaContext().getConfigParameterValue("outputFile");

    // Output file should be specified in the descriptor
    if (oPath == null) {
      throw new ResourceInitializationException(
              ResourceInitializationException.CONFIG_SETTING_ABSENT, new Object[] { "outputFile" });
    }
    // If specified output directory does not exist, try to create it
    outFile = new File(oPath.trim());
    if (outFile.getParentFile() != null && !outFile.getParentFile().exists()) {
      if (!outFile.getParentFile().mkdirs())
        throw new ResourceInitializationException(
                ResourceInitializationException.RESOURCE_DATA_NOT_VALID, new Object[] { oPath,
                    "outputFile" });
    }
    try {
      fileWriter = new FileWriter(outFile);
    } catch (IOException e) {
      throw new ResourceInitializationException(e);
    }
  }

  /**
   * Processes the CasContainer which was populated by the TextAnalysisEngines. <br>
   * In this case, the CAS index is iterated over selected annotations and printed out into an
   * output file
   * 
   * @param aCAS
   *          CasContainer which has been populated by the TAEs
   * 
   * @throws ResourceProcessException
   *           if there is an error in processing the Resource
   * 
   * @see org.apache.uima.collection.base_cpm.CasObjectProcessor#processCas(CAS)
   */
  public synchronized void processCas(CAS aCAS) throws ResourceProcessException {
    JCas jcas;
    try {
      jcas = aCAS.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    boolean titleP = false;
    String docUri = null;
    Iterator it = jcas.getAnnotationIndex(SourceDocumentInformation.type).iterator();
    if (it.hasNext()) {
      SourceDocumentInformation srcDocInfo = (SourceDocumentInformation) it.next();
      docUri = srcDocInfo.getUri();
    }

    // iterate and print annotations
    Iterator annotationIter = jcas.getAnnotationIndex().iterator();
    while (annotationIter.hasNext()) {
      Annotation annot = (Annotation) annotationIter.next();
      if (titleP == false) {
        try {
          fileWriter.write("\n\n<++++NEW DOCUMENT++++>\n");
          if (docUri != null)
            fileWriter.write("DOCUMENT URI:" + docUri + "\n");
          fileWriter.write("\n");
        } catch (IOException e) {
          throw new ResourceProcessException(e);
        }
        titleP = true;
      }
      // get the text that is enclosed within the annotation in the CAS
      String aText = annot.getCoveredText();
      aText = aText.replace('\n', ' ');
      aText = aText.replace('\r', ' ');
      // System.out.println( annot.getType().getName() + " "+aText);
      try {
        fileWriter.write(annot.getType().getName() + " " + aText + "\n");
        fileWriter.flush();
      } catch (IOException e) {
        throw new ResourceProcessException(e);
      }
    }
  }

  /**
   * Called when a batch of processing is completed.
   * 
   * @param aTrace
   *          ProcessTrace object that will log events in this method.
   * @throws ResourceProcessException
   *           if there is an error in processing the Resource
   * @throws IOException
   *           if there is an IO Error
   * 
   * @see org.apache.uima.collection.CasConsumer#batchProcessComplete(ProcessTrace)
   */
  public void batchProcessComplete(ProcessTrace aTrace) throws ResourceProcessException,
          IOException {
    // nothing to do in this case as AnnotationPrinter doesnot do
    // anything cumulatively
  }

  /**
   * Called when the entire collection is completed.
   * 
   * @param aTrace
   *          ProcessTrace object that will log events in this method.
   * @throws ResourceProcessException
   *           if there is an error in processing the Resource
   * @throws IOException
   *           if there is an IO Error
   * @see org.apache.uima.collection.CasConsumer#collectionProcessComplete(ProcessTrace)
   */
  public void collectionProcessComplete(ProcessTrace aTrace) throws ResourceProcessException,
          IOException {
    if (fileWriter != null) {
      fileWriter.close();
    }
  }

  /**
   * Reconfigures the parameters of this Consumer. <br>
   * This is used in conjunction with the setConfigurationParameterValue to set the configuration
   * parameter values to values other than the ones specified in the descriptor.
   * 
   * @throws ResourceConfigurationException
   *           if the configuration parameter settings are invalid
   * 
   * @see org.apache.uima.resource.ConfigurableResource#reconfigure()
   */
  public void reconfigure() throws ResourceConfigurationException {
    super.reconfigure();
    // extract configuration parameter settings
    String oPath = (String) getUimaContext().getConfigParameterValue("outputFile");
    File oFile = new File(oPath.trim());
    // if output file has changed, close exiting file and open new
    if (!oFile.equals(this.outFile)) {
      this.outFile = oFile;
      try {
        fileWriter.close();

        // If specified output directory does not exist, try to create it
        if (oFile.getParentFile() != null && !oFile.getParentFile().exists()) {
          if (!oFile.getParentFile().mkdirs())
            throw new ResourceConfigurationException(
                    ResourceInitializationException.RESOURCE_DATA_NOT_VALID, new Object[] { oPath,
                        "outputFile" });
        }
        fileWriter = new FileWriter(oFile);
      } catch (IOException e) {
        throw new ResourceConfigurationException();
      }
    }
  }

  /**
   * Called if clean up is needed in case of exit under error conditions.
   * 
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy() {
    if (fileWriter != null) {
      try {
        fileWriter.close();
      } catch (IOException e) {
        // ignore IOException on destroy
      }
    }
  }

}
