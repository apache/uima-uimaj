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

package org.apache.uima.examples.cas;

import java.util.Arrays;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

/**
 * An example annotator that discovers Person Titles in text and classifies them into three
 * categories - Civilian (e.g. Mr.,Ms.), Military (e.g. Lt. Col.) , and Government (e.g. Gov.,
 * Sen.). The titles are detected using simple string matching. The strings that are matched are
 * determined by the <code>CivilianTitles</code>, <code>MilitaryTitles</code>, and
 * <code>GovernmentTitles</code> configuration parameters.
 * <p>
 * If the <code>ContainingAnnotationType</code> parameter is specified, this annotator will only
 * look for titles within existing annotations of that type. This feature can be used, for example,
 * to only match person titles within existing Person Name annotations, discovered by some annotator
 * that has run previously.
 * 
 * 
 */
public class PersonTitleAnnotator extends CasAnnotator_ImplBase {
  /**
   * The Type of Annotation that we will be creating when we find a match.
   */
  private Type mPersonTitleType;

  /**
   * The Annotation Type within which we will search for Person Titles (optional).
   */
  private Type mContainingType;

  /**
   * The Feature representing the kind of PersonTitle - civilian, military, or government.
   */
  private Feature mPersonTitleKindFeature;

  /**
   * The list of civilian titles, read from the CivilianTitles configuration parameter.
   */
  private String[] mCivilianTitles;

  /**
   * The list of military titles, read from the MilitaryTitles configuration parameter.
   */
  private String[] mMilitaryTitles;

  /**
   * The list of government titles, read from the GovernmentTitles configuration parameter.
   */
  private String[] mGovernmentTitles;

  /**
   * Performs initialization logic. This implementation just reads values for the configuration
   * parameters.
   * 
   * @see org.apache.uima.analysis_engine.annotator.BaseAnnotator#initialize(AnnotatorContext)
   */
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);

    // read configuration parameter values
    mCivilianTitles = (String[]) getContext().getConfigParameterValue("CivilianTitles");
    mMilitaryTitles = (String[]) getContext().getConfigParameterValue("MilitaryTitles");
    mGovernmentTitles = (String[]) getContext().getConfigParameterValue("GovernmentTitles");

    // write log messages
    Logger logger = getContext().getLogger();
    logger.log(Level.CONFIG, "PersonTitleAnnotator initialized");
    logger.log(Level.CONFIG, "CivilianTitles = " + Arrays.asList(mCivilianTitles));
    logger.log(Level.CONFIG, "MilitaryTitles = " + Arrays.asList(mMilitaryTitles));
    logger.log(Level.CONFIG, "GovernmentTitles = " + Arrays.asList(mGovernmentTitles));
  }

  /**
   * Called whenever the CAS type system changes. Acquires references to Types and Features.
   * 
   * @see org.apache.uima.analysis_engine.annotator.BaseAnnotator#typeSystemInit(TypeSystem)
   */
  public void typeSystemInit(TypeSystem aTypeSystem) throws AnalysisEngineProcessException {
    // Get a reference to the "PersonTitle" Type
    mPersonTitleType = aTypeSystem.getType("example.PersonTitle");
    if (mPersonTitleType == null) {
      throw new AnalysisEngineProcessException(AnnotatorInitializationException.TYPE_NOT_FOUND,
              new Object[] { getClass().getName(), "example.PersonTitle" });
    }

    // Get a reference to the "Kind" Feature
    mPersonTitleKindFeature = mPersonTitleType.getFeatureByBaseName("Kind");
    if (mPersonTitleKindFeature == null) {
      throw new AnalysisEngineProcessException(AnnotatorInitializationException.FEATURE_NOT_FOUND,
              new Object[] { getClass().getName(), "example.PersonTitle:Kind" });
    }

    // Get the value for the "ContainingType" parameter if there is one
    String containingTypeName = (String) getContext().getConfigParameterValue(
            "ContainingAnnotationType");
    if (containingTypeName != null) {
      mContainingType = aTypeSystem.getType(containingTypeName);
      if (mContainingType == null) {
        throw new AnalysisEngineProcessException(AnnotatorInitializationException.TYPE_NOT_FOUND,
                new Object[] { getClass().getName(), containingTypeName });
      }
    }
  }

  /**
   * Annotates a document. This annotator searches for person titles using simple string matching.
   * 
   * @param aCAS
   *          CAS containing document text and previously discovered annotations, and to which new
   *          annotations are to be written.
   * 
   * @see CasAnnotator_ImplBase#process(CAS)
   */
  public void process(CAS aCAS) throws AnalysisEngineProcessException {
    try {
      // If the ResultSpec doesn't include the PersonTitle type, we have
      // nothing to do.
      if (!getResultSpecification().containsType("example.PersonTitle",aCAS.getDocumentLanguage())) {
        return;
      }

      if (mContainingType == null) {
        // Search the whole document for PersonTitle annotations
        String text = aCAS.getDocumentText();
        annotateRange(aCAS, text, 0);
      } else {
        // Search only within annotations of type mContainingType

        // Get an iterator over the annotations of type mContainingType.
        FSIterator it = aCAS.getAnnotationIndex(mContainingType).iterator();
        // Loop over the iterator.
        while (it.isValid()) {
          // Get the next annotation from the iterator
          AnnotationFS annot = (AnnotationFS) it.get();
          // Get text covered by this annotation
          String coveredText = annot.getCoveredText();
          // Get begin position of this annotation
          int annotBegin = annot.getBegin();
          // search for matches within this
          annotateRange(aCAS, coveredText, annotBegin);
          // Advance the iterator.
          it.moveToNext();
        }
      }
    } catch (Exception e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

  /**
   * A utility method that searches a part of the document for Person Titles.
   * 
   * @param aCAS
   *          the CAS in which to create new annotations
   * @param aText
   *          the substring of the document text within which to search
   * @param aBeginPos
   *          the position of this substring relative to the start of the document
   */
  protected void annotateRange(CAS aCAS, String aText, int aBeginPos) {
    // Search for each of the three types of titles
    annotateRange(aCAS, aText, aBeginPos, "Civilian", mCivilianTitles);
    annotateRange(aCAS, aText, aBeginPos, "Military", mMilitaryTitles);
    annotateRange(aCAS, aText, aBeginPos, "Government", mGovernmentTitles);
  }

  /**
   * A utility method that searches a part of the document for a specific kind of Person Title.
   * 
   * @param aCAS
   *          the CAS in which to create new annotations
   * @param aText
   *          the substring of the document text within which to search
   * @param aBeginPos
   *          the position of this substring relative to the start of the document
   * @param aTitleType
   *          the type of title to look for. This becomes the value of the <code>Kind</code>
   *          feature.
   * @param aTitles
   *          the exact strings to look for in the document
   * 
   */
  protected void annotateRange(CAS aCAS, String aText, int aBeginPos, String aTitleType,
          String[] aTitles) {
    // Loop over the matchStrings.
    for (int i = 0; i < aTitles.length; i++) {
      // logger.log("Looking for string: " + matchStrings[i]);
      // Find a first match, if it exists.
      int start = aText.indexOf(aTitles[i]);
      // Keep going while there are matches in the text.
      while (start >= 0) {
        // Set the end position (start + length of string).
        int end = start + aTitles[i].length();
        // Compute absolute position of annotation in document
        int absStart = aBeginPos + start;
        int absEnd = aBeginPos + end;
        // Write log message
        getContext().getLogger().log(Level.FINER,
                "Found \"" + aTitles[i] + "\" at (" + absStart + "," + absEnd + ")");
        // Create a new annotation for the most recently discovered match.
        createAnnotation(aCAS, absStart, absEnd, aTitleType);
        // Look for the next match, starting after the previous match.
        start = aText.indexOf(aTitles[i], end);
      }
    }
  }

  /**
   * Creates an PersonTitle annotation in the CAS.
   * 
   * @param aCAS
   *          the CAS in which to create the annotation
   * @param aBeginPos
   *          the begin position of the annotation relative to the start of the document
   * @param aEndPos
   *          the end position of the annotation relative to the start of the document. (Note that,
   *          as in the Java string functions, the end position is one past the last character in
   *          the annotation, so that (end - begin) = length.
   * @param aTitleType
   *          the type of person title. This becomes the value of the <code>Kind</code> feature.
   */
  protected void createAnnotation(CAS aCAS, int aBeginPos, int aEndPos, String aTitleType) {
    AnnotationFS title = aCAS.createAnnotation(mPersonTitleType, aBeginPos, aEndPos);
    // Set the "kind" feature if it's part of the ResultSpec
    if (getResultSpecification().containsFeature("example.PersonTitle:Kind",aCAS.getDocumentLanguage())) {
      title.setStringValue(mPersonTitleKindFeature, aTitleType);
    }
    // Add the annotation to the index.
    aCAS.getIndexRepository().addFS(title);
  }

}
