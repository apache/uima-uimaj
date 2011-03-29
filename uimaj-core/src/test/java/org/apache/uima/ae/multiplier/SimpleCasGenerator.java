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

package org.apache.uima.ae.multiplier;

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

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.CasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

/**
 * An example CasMultiplier, which generates the specified number of output CASes.
 */
public class SimpleCasGenerator extends CasMultiplier_ImplBase {
  private int mCount;

  private int nToGen;

  private String text;

  long docCount = 0;
  
  public static String lastDocument;

  public static ResultSpecification lastResultSpec;
  
  public static synchronized String getLastDocument() {
    return lastDocument;  
  }
  
  public static synchronized ResultSpecification getLastResultSpec() {
    return lastResultSpec;
  }

  /*
   * (non-Javadoc)
   * 
   * @seeorg.apache.uima.analysis_component.AnalysisComponent_ImplBase#initialize(org.apache.uima.
   * UimaContext)
   */
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    lastDocument = null;
    lastResultSpec = null;

    this.nToGen = ((Integer) aContext.getConfigParameterValue("NumberToGenerate")).intValue();
    FileInputStream fis = null;
    try {
      String filename = ((String) aContext.getConfigParameterValue("InputFile")).trim();
      File file = null;
      try {
        URL url = this.getClass().getClassLoader().getResource(filename);
//        System.out.println("************ File::::" + url.getPath());
        // open input stream to file
        file = new File(url.getPath());
      } catch (Exception e) {
        file = new File(filename);
      }
      fis = new FileInputStream(file);
      byte[] contents = new byte[(int) file.length()];
      fis.read(contents);
      text = new String(contents);
    } catch (Exception e) {
      throw new ResourceInitializationException(e);
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (Exception e) {
        }
      }
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see JCasMultiplier_ImplBase#process(JCas)
   */
  public void process(CAS aCas) throws AnalysisEngineProcessException {
    // set static fields to contain document text, result spec,
    // and value of StringParam configuration parameter.
    lastDocument = aCas.getDocumentText();
    lastResultSpec = getResultSpecification();
    this.mCount = 0;
    this.docCount = 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#hasNext()
   */
  public boolean hasNext() throws AnalysisEngineProcessException {
    return this.mCount < this.nToGen;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#next()
   */
  public AbstractCas next() throws AnalysisEngineProcessException {

    CAS cas = getEmptyCAS();
    /*
     * int junk = this.gen.nextInt(); if ((junk & 1) != 0) { cas.setDocumentText(this.mDoc1); } else
     * { cas.setDocumentText(this.mDoc2); }
     */
    if (docCount == 0 && UIMAFramework.getLogger().isLoggable(Level.FINE)) {
      System.out.println("Initializing CAS with a Document of Size:" + text.length());
    }
    docCount++;
    if (UIMAFramework.getLogger().isLoggable(Level.FINE))
      System.out.println("CasMult creating document#" + docCount);
    cas.setDocumentText(this.text);
    this.mCount++;
    return cas;
  }

}
