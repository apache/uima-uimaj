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

import java.util.Iterator;

import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;


public class NewlineResegmenter extends JCasMultiplier_ImplBase {
  JCas mCurrentInputCas;

  String mDoc;

  int mCurIndex = 0;

  StringBuffer mBuf = new StringBuffer();

  JCas[] mJCases = new JCas[2];

  int mActiveJCas = 0;

  boolean mHasNext;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.CasSegmenter_ImplBase#process(org.apache.uima.cas.CAS)
   */
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    mCurrentInputCas = aJCas;
    mDoc = aJCas.getDocumentText();
    readToNextNewline();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#hasNext()
   */
  public boolean hasNext() throws AnalysisEngineProcessException {
    return mHasNext;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#next()
   */
  public AbstractCas next() throws AnalysisEngineProcessException {
    // we should already have a JCas ready to return
    JCas toReturn = mJCases[mActiveJCas];
    mJCases[mActiveJCas] = null;
    assert toReturn != null;

    // now go read the next segment into our other JCas
    mActiveJCas ^= 1;
    if (mCurIndex < mDoc.length()) {
      readToNextNewline();
    } else {
      // end of the input document
      mCurIndex = 0;
      mHasNext = false;
    }

    return toReturn;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.JCasSegmenter_ImplBase#getCasInstancesRequired()
   */
  public int getCasInstancesRequired() {
    return 2;
  }

  private void readToNextNewline() {
    // does this doc contain a newline?
    int nlIndex = mDoc.indexOf('\n', mCurIndex);
    if (nlIndex == -1) // no newline
    {
      // append entire rest of doc to active buffer
      int begin = mBuf.length(); // record start offset of new text
      mBuf.append(mDoc.substring(mCurIndex));
      if (mJCases[mActiveJCas] == null) {
        mJCases[mActiveJCas] = getEmptyJCas();
      }
      // add SourceDocumentInformation to active JCas
      SourceDocumentInformation sdi = new SourceDocumentInformation(mJCases[mActiveJCas]);
      sdi.setBegin(begin);
      sdi.setEnd(mBuf.length());
      sdi.setUri(getCasSourceUri(mCurrentInputCas));
      sdi.addToIndexes();
      mHasNext = false; // we need to see another input CAS before we can create output
      mCurIndex = 0;
    } else // yes, newline
    {
      // append doc up to newline
      int begin = mBuf.length(); // record start offset of new text
      mBuf.append(mDoc.substring(mCurIndex, nlIndex));
      if (mJCases[mActiveJCas] == null) {
        mJCases[mActiveJCas] = getEmptyJCas();
      }
      // add SourceDocumentInformation to active JCas
      SourceDocumentInformation sdi = new SourceDocumentInformation(mJCases[mActiveJCas]);
      sdi.setBegin(begin);
      sdi.setEnd(mBuf.length());
      sdi.setUri(getCasSourceUri(mCurrentInputCas));
      sdi.addToIndexes();
      // set doc text
      mJCases[mActiveJCas].setDocumentText(mBuf.toString());
      mBuf.setLength(0);
      mCurIndex = nlIndex + 1;
      mHasNext = true; // ready to output!
    }
  }

  private String getCasSourceUri(JCas jcas) {
    Iterator<Annotation> iter = jcas.getJFSIndexRepository().getAnnotationIndex(SourceDocumentInformation.type)
            .iterator();
    if (iter.hasNext()) {
      SourceDocumentInformation sdi = (SourceDocumentInformation) iter.next();
      return sdi.getUri();
    } else {
      return "unknown";
    }
  }
}
