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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;

/**
 * An example CasMultiplier, which merges text documents into larger ones. It attempts to merge all
 * of the segments that came from one original artifact. This is done by checking the "lastSegment"
 * feature of the SourceDocumentInformation FeatureStructure, which is expected to be populated by
 * the CollectionReader or CasMultiplier that produced the input CASes.
 * <p>
 * Limitations: if the lastSegment feature is never set to true by the component producing the input
 * CASes, the merger will never produce any output. Also, this implementation relies on the CASes
 * arriving in order, which could be a problem in a mulithreaded framework implementation. The order
 * requirement could be relieved by recording a segment number in the SourceDocumentInformation, but
 * that would also make this example more complicated.
 */
public class SimpleTextMerger extends JCasMultiplier_ImplBase {
  
  public static final String MESSAGE_DIGEST = "org.apache.uima.examples.casMultiplier.ExampleCasMultiplierMessages";
  
  public static final String MISSING_SOURCE_DOCUMENT_INFO = "missing_source_document_info";
  
  public static final String NO_NEXT_CAS = "no_next_cas";
  
  private StringBuffer mDocBuf = new StringBuffer();

  private JCas mMergedCas;

  private boolean mReadyToOutput = false;

  private String[] mAnnotationTypesToCopy;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent_ImplBase#initialize(org.apache.uima.UimaContext)
   */
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    mAnnotationTypesToCopy = (String[]) aContext.getConfigParameterValue("AnnotationTypesToCopy");
  }

  /*
   * (non-Javadoc)
   * 
   * @see JCasMultiplier_ImplBase#process(JCas)
   */
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    // procure a new CAS if we don't have one already
    if (mMergedCas == null) {
      mMergedCas = getEmptyJCas();
    }

    // append document text
    String docText = aJCas.getDocumentText();
    int prevDocLen = mDocBuf.length();
    mDocBuf.append(docText);

    // copy specified annotation types
    CasCopier copier = new CasCopier(aJCas.getCas(), mMergedCas.getCas());
    Set copiedIndexedFs = new HashSet(); // needed in case one annotation is in two indexes (could
    // happen if specified annotation types overlap)
    for (int i = 0; i < mAnnotationTypesToCopy.length; i++) {
      Type type = mMergedCas.getTypeSystem().getType(mAnnotationTypesToCopy[i]);
      FSIndex index = aJCas.getCas().getAnnotationIndex(type);
      Iterator iter = index.iterator();
      while (iter.hasNext()) {
        FeatureStructure fs = (FeatureStructure) iter.next();
        if (!copiedIndexedFs.contains(fs)) {
          Annotation copyOfFs = (Annotation) copier.copyFs(fs);
          // update begin and end
          copyOfFs.setBegin(copyOfFs.getBegin() + prevDocLen);
          copyOfFs.setEnd(copyOfFs.getEnd() + prevDocLen);
          mMergedCas.addFsToIndexes(copyOfFs);
          copiedIndexedFs.add(fs);
        }
      }
    }

    // get the SourceDocumentInformation FS, which indicates the sourceURI of the document
    // and whether the incoming CAS is the last segment
    FSIterator it = aJCas.getAnnotationIndex(SourceDocumentInformation.type).iterator();
    if (!it.hasNext()) {
      throw new AnalysisEngineProcessException(MESSAGE_DIGEST, MISSING_SOURCE_DOCUMENT_INFO,
              new Object[0]);
    }
    SourceDocumentInformation sourceDocInfo = (SourceDocumentInformation) it.next();
    if (sourceDocInfo.getLastSegment()) {
      // time to produce an output CAS
      // set the document text
      mMergedCas.setDocumentText(mDocBuf.toString());

      // add source document info to destination CAS
      SourceDocumentInformation destSDI = new SourceDocumentInformation(mMergedCas);
      destSDI.setUri(sourceDocInfo.getUri());
      destSDI.setOffsetInSource(0);
      destSDI.setLastSegment(true);
      destSDI.addToIndexes();

      mDocBuf = new StringBuffer();
      mReadyToOutput = true;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#hasNext()
   */
  public boolean hasNext() throws AnalysisEngineProcessException {
    return mReadyToOutput;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#next()
   */
  public AbstractCas next() throws AnalysisEngineProcessException {
    if (!mReadyToOutput) {
      throw new AnalysisEngineProcessException(MESSAGE_DIGEST, NO_NEXT_CAS, new Object[0]);
    }
    JCas casToReturn = mMergedCas;
    mMergedCas = null;
    mReadyToOutput = false;
    return casToReturn;
  }

}
