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

package org.apache.uima.examples.opennlp.annotator;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import opennlp.tools.lang.english.NameFinder;
import opennlp.tools.namefind.NameFinderME;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.examples.opennlp.EntityAnnotation;
import org.apache.uima.examples.opennlp.Sentence;
import org.apache.uima.examples.opennlp.Token;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * UIMA wrapper for the OpenNLP named entity recognizer. The entity models and corresponding
 * annotation type classes are specified as parameters. The document in the CAS is analyzed and a
 * named entity annotation is created for each entity found. We assume that any entity annotation
 * type class inherits from org.apache.uima.examples.opennlp.annotator.EntityAnnotation.
 */
public class NEDetector extends JCasAnnotator_ImplBase {

  /* Model directory parameter name. */
  private static final String MODEL_DIR_PARAM = "ModelDirectory";

  /* Mappings array parameter name. */
  private static final String MAPPINGS_PARAM = "EntityTypeMappings";

  /* Name to use for this Analysis Engine component. */
  private static final String COMPONENT_NAME = "OpenNLP NE Detector";

  /* Array of named entity finders that will be run. */
  private NameFinder[] nameFinders;

  /*
   * Array of labels for the named entity finders, derived from the corresponding model file names
   * (i.e., the filename minus ".bin.gz").
   */
  private String[] nameFinderLabels;

  /*
   * Hash that maps named entity finder labels to the constructor for the corresponding annotation
   * type class.
   */
  private Hashtable labelMap = new Hashtable();

  /* Number of named entity finders. */
  private int numNefs = 0;

  /* Array of constructors for the annotation type classes. */
  private Constructor[] neAnnotationMakers;

  /**
   * A simple filename filter class to list all of the named entity model files in the model
   * directory.
   */
  private class modelFileFilter implements FilenameFilter {

    /*
     * (non-Javadoc)
     * 
     * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
     */
    public boolean accept(File dir, String name) {
      return name.endsWith(".bin.gz");
    }
  }

  /**
   * Initialize the Annotator.
   * 
   * @see JCasAnnotator_ImplBase#initialize(UimaContext)
   */
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);

    try {
      String[] mappingStrings = null;

      mappingStrings = (String[]) aContext.getConfigParameterValue(MAPPINGS_PARAM);
      if (mappingStrings == null) {
        throw new AnnotatorConfigurationException();
      }
      loadMappings(mappingStrings);

      String modelDirName = (String) aContext.getConfigParameterValue(MODEL_DIR_PARAM);

      File modelDir = new File(modelDirName);
      if (!modelDir.isDirectory()) {
        throw new AnnotatorConfigurationException();
      }
      File[] modelFiles = modelDir.listFiles(new modelFileFilter());
      numNefs = modelFiles.length;
      nameFinders = new NameFinder[numNefs];
      nameFinderLabels = new String[numNefs];
      neAnnotationMakers = new Constructor[numNefs];
      for (int i = 0; i < numNefs; i++) {
        String modelName = modelFiles[i].getName();
        System.out.print("Loading model: " + modelName + "...");
        nameFinders[i] = new NameFinder(new SuffixSensitiveGISModelReader(modelFiles[i]).getModel());
        int nameStart = modelName.lastIndexOf(System.getProperty("file.separator")) + 1;
        int nameEnd = modelName.indexOf('.', nameStart);
        if (nameEnd == -1) {
          nameEnd = modelName.length();
        }
        nameFinderLabels[i] = modelName.substring(nameStart, nameEnd);
        Constructor annotationMaker;
        if ((annotationMaker = (Constructor) labelMap.get(nameFinderLabels[i])) == null) {
          throw new AnnotatorConfigurationException();
        }
        neAnnotationMakers[i] = annotationMaker;
        System.out.println("done");
      }

    } catch (Exception e) {
      throw new ResourceInitializationException(e);
    }
  }

  /**
   * Processes the entity type mappaings parameter. The constructor for each class identified in the
   * array is loaded and stored in the mapping hashtable, using the label provided in the parameter
   * as the key.
   * 
   * @param mappingStrings
   *          Array of mapping strings of the form "labe,class"
   * @throws AnnotatorConfigurationException
   */
  private void loadMappings(String[] mappingStrings) throws AnnotatorConfigurationException {
    // populate the mappings hash table (key: entity label,CAS Annotation Type
    // Constructor)
    for (int i = 0; i < mappingStrings.length; i++) {
      String[] mappingPair = mappingStrings[i].split(",");
      if (mappingPair.length < 2)
        throw new AnnotatorConfigurationException();

      String modelName = mappingPair[0];
      String className = mappingPair[1];

      Constructor annotationConstructor;
      // get the name of the JCAS type with this name
      Class annotationClass;
      try {
        annotationClass = Class.forName(className);
        // get the constructor for that JCAS type
        annotationConstructor = annotationClass.getConstructor(new Class[] { JCas.class });
      } catch (Exception e) {
        throw new AnnotatorConfigurationException(e);
      }
      labelMap.put(modelName, annotationConstructor);
    }
  }

  /**
   * Process a CAS.
   * 
   * @see JCasAnnotator_ImplBase#process(JCas)
   */
  public void process(JCas aJCas) throws AnalysisEngineProcessException {

    ArrayList tokenList = new ArrayList();
    ArrayList wordList = new ArrayList();
    List finderTags;

    AnnotationIndex sentenceIndex = aJCas.getAnnotationIndex(Sentence.type);
    AnnotationIndex tokenIndex = aJCas.getAnnotationIndex(Token.type);

    // iterate over Sentences
    FSIterator sentenceIterator = sentenceIndex.iterator();
    while (sentenceIterator.hasNext()) {
      Sentence sentence = (Sentence) sentenceIterator.next();

      tokenList.clear();
      wordList.clear();

      // iterate over Tokens
      FSIterator tokenIterator = tokenIndex.subiterator(sentence);
      while (tokenIterator.hasNext()) {
        Token token = (Token) tokenIterator.next();

        tokenList.add(token);
        wordList.add(token.getCoveredText());
      }

      for (int i = 0; i < numNefs; i++) {
        Constructor annotationMaker = neAnnotationMakers[i];
        finderTags = nameFinders[i].find(wordList, Collections.EMPTY_MAP);

        boolean inTag = false;
        int tagStart = 0;
        int tagEnd = 0;
        for (int j = 0; j < finderTags.size(); j++) {
          String tag = (String) finderTags.get(j);

          if (inTag) {
            // check for end tags
            if (tag.equals(NameFinderME.START) || tag.equals(NameFinderME.OTHER)) {
              // make annotation
              tagEnd = j - 1;
              Token startToken = (Token) tokenList.get(tagStart);
              Token endToken = (Token) tokenList.get(tagEnd);
              makeEntityAnnotation(annotationMaker, aJCas, startToken.getBegin(), endToken.getEnd());
              inTag = false;
            }
          }
          if (!inTag) {
            // check for start tags
            if (tag.equals(NameFinderME.START)) {
              tagStart = j;
              inTag = true;
            }
          }
        }
      }
    }
  }

  /**
   * Create a new EntityAnnotation using the supplied Constructor. The start, end, and componentId
   * features are set.
   * 
   * @param annotationMaker
   *          Constructor to create the EntityAnnotation object
   * @param jCas
   *          The JCas in which to create the new annotation
   * @param start
   *          Start of annotation span
   * @param end
   *          End of annotation span
   * @throws AnnotatorProcessException
   */
  private void makeEntityAnnotation(Constructor annotationMaker, JCas jCas, int start, int end)
          throws AnalysisEngineProcessException {
    try {
      EntityAnnotation entityAnnot = (EntityAnnotation) annotationMaker
              .newInstance(new Object[] { jCas });
      entityAnnot.setBegin(start);
      entityAnnot.setEnd(end);
      entityAnnot.setComponentId(COMPONENT_NAME);
      entityAnnot.addToIndexes();
    } catch (Exception e) {
      throw new AnalysisEngineProcessException(e);
    }
  }
}
