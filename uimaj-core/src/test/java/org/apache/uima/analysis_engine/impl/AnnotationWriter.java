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

package org.apache.uima.analysis_engine.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.ProcessTrace;


/**
 * The AnnotationWriter class writes specified annotations to an output file. The encoding of the
 * output file is UTF-8
 */

public class AnnotationWriter extends CasConsumer_ImplBase implements CasConsumer {
  // output file
  File outFile;

  // output file writer
  OutputStreamWriter fileWriter;

  // only used by test case
  static boolean typeSystemInitCalled = false;

  /**
   * Initializes this CAS Consumer with the parameters specified in the descriptor.
   * 
   * @throws ResourceInitializationException
   *           if there is error in initializing the resources
   */
  public void initialize() throws ResourceInitializationException {

    File testBaseDir = JUnitExtension.getFile("TextAnalysisEngineImplTest").getParentFile();
    this.outFile = new File(testBaseDir, "CpmOutput.txt");

    try {
      this.fileWriter = new OutputStreamWriter(new FileOutputStream(this.outFile, false), "UTF-8");
    } catch (Exception e) {
      throw new ResourceInitializationException(e);
    }
    typeSystemInitCalled = false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.CasConsumer_ImplBase#typeSystemInit(org.apache.uima.cas.TypeSystem)
   */
  public void typeSystemInit(TypeSystem arg0) throws ResourceInitializationException {
    typeSystemInitCalled = true;
  }

  /**
   * print the cas content to the output file
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
    try {
      // iterate and print annotations
      FSIterator<AnnotationFS> typeIterator = aCAS.getCurrentView().getAnnotationIndex().iterator();

      for (typeIterator.moveToFirst(); typeIterator.isValid(); typeIterator.moveToNext()) {
        AnnotationFS annot = typeIterator.get();

        this.fileWriter.write(annot.getCoveredText());
        this.fileWriter.write(System.getProperty("line.separator"));
        this.fileWriter.write(annot.toString());
      }
      this.fileWriter.flush();
    } catch (Exception ex) {
      ex.printStackTrace();
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
    if (this.fileWriter != null) {
      this.fileWriter.close();
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
    //do nothing
  }

  /**
   * Called if clean up is needed in case of exit under error conditions.
   * 
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy() {
    if (this.fileWriter != null) {
      try {
        this.fileWriter.close();
      } catch (IOException e) {
        // ignore IOException on destroy
      }
    }
  }

}
