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

package org.apache.uima.examples.casMultiplier;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * An example CasMultiplier, which breaks large text documents into smaller segments. The minimum
 * size of the segments as determined by the "SegmentSize" configuration parameter, but the break
 * between segments will always occur at the next newline character, so segments will not be exactly
 * that size.
 */
public class SimpleTextSegmenter extends JCasMultiplier_ImplBase {
  private String mDoc;

  private int mPos;

  private int mSegmentSize;

  private String mDocUri;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent_ImplBase#initialize(org.apache.uima.UimaContext)
   */
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    mSegmentSize = ((Integer) aContext.getConfigParameterValue("SegmentSize")).intValue();
  }

  /*
   * (non-Javadoc)
   * 
   * @see JCasMultiplier_ImplBase#process(JCas)
   */
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    mDoc = aJCas.getDocumentText();
    mPos = 0;
    // retreive the filename of the input file from the CAS so that it can be added
    // to each segment
    FSIterator it = aJCas.getAnnotationIndex(SourceDocumentInformation.type).iterator();
    if (it.hasNext()) {
      SourceDocumentInformation fileLoc = (SourceDocumentInformation) it.next();
      mDocUri = fileLoc.getUri();
    } else {
      mDocUri = null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#hasNext()
   */
  public boolean hasNext() throws AnalysisEngineProcessException {
    return mPos < mDoc.length();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#next()
   */
  public AbstractCas next() throws AnalysisEngineProcessException {
    int breakAt = mPos + mSegmentSize;
    if (breakAt > mDoc.length())
      breakAt = mDoc.length();
    // search for the next newline character. Note: this example segmenter implementation
    // assumes that the document contains many newlines. In the worst case, if this segmenter
    // is runon a document with no newlines, it will produce only one segment containing the
    // entire document text. A better implementation might specify a maximum segment size as
    // well as a minimum.
    while (breakAt < mDoc.length() && mDoc.charAt(breakAt - 1) != '\n')
      breakAt++;

    JCas jcas = getEmptyJCas();
    try {
      jcas.setDocumentText(mDoc.substring(mPos, breakAt));
      // if original CAS had SourceDocumentInformation, also add SourceDocumentInformatio
      // to each segment
      if (mDocUri != null) {
        SourceDocumentInformation sdi = new SourceDocumentInformation(jcas);
        sdi.setUri(mDocUri);
        sdi.setOffsetInSource(mPos);
        sdi.setDocumentSize(breakAt - mPos);
        sdi.addToIndexes();

        if (breakAt == mDoc.length()) {
          sdi.setLastSegment(true);
        }
      }

      mPos = breakAt;
      return jcas;
    } catch (Exception e) {
      jcas.release();
      throw new AnalysisEngineProcessException(e);
    }
  }

}
