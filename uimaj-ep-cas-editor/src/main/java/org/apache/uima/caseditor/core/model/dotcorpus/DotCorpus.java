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

package org.apache.uima.caseditor.core.model.dotcorpus;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.uima.cas.Type;
import org.apache.uima.caseditor.editor.AnnotationStyle;

/**
 * This class contains all project specific configuration parameters. Note: Use DotCorpusSerialzer
 * to read or write an instance of this class to or from a byte stream.
 */
public class DotCorpus {
  /**
   * The default value for editor line length hint
   */
  public static final int EDITOR_LINE_LENGTH_HINT_DEFAULT = 80;

  /**
   * Name of the type system file
   */
  private String mTypeSystemFileName;

  /**
   * names of the corpus folders
   */
  private Set<String> mCorpusFolders = new HashSet<String>();

  /**
   * Names of the configuration source folders
   */
  private Set<String> mCasProcessorFolders = new HashSet<String>();

  /**
   * Length hint of the lines in the editor.
   */
  private int mEditorLineLengthHint = EDITOR_LINE_LENGTH_HINT_DEFAULT;

  /**
   * Maps style names to style objects.
   */
  private HashMap<String, AnnotationStyle> mStyleMap = new HashMap<String, AnnotationStyle>();

  /**
   * Retrieves type system name parameter.
   * 
   * @return type system name parameter
   */
  public String getTypeSystemFileName() {
    return mTypeSystemFileName;
  }

  /**
   * Sets type system name parameter.
   * 
   * @param name
   *          type system name parameter
   */
  public void setTypeSystemFilename(String name) {
    mTypeSystemFileName = name;
  }

  /**
   * Retrieves the uima config folder name parameter.
   * 
   * @return uima config folder name parameter.
   */
  public Collection<String> getCasProcessorFolderNames() {
    return Collections.unmodifiableCollection(mCasProcessorFolders);
  }

  /**
   * Sets the uima config folder name parameter.
   * 
   * @param folder
   *          uima config folder name parameter.
   */
  public void addCasProcessorFolder(String folder) {
    mCasProcessorFolders.add(folder);
  }

  public void removeCasProcessorFolder(String folder) {
    mCasProcessorFolders.remove(folder);
  }

  /**
   * Adds a corpus folder
   * 
   * @param name
   */
  public void addCorpusFolder(String name) {
    mCorpusFolders.add(name);
  }

  /**
   * Removes the given corpus folder.
   * 
   * @param name
   */
  public void removeCorpusFolder(String name) {
    mCorpusFolders.remove(name);
  }

  /**
   * Retrieves the list of all corpus folders.
   * 
   * @return corpus folder list
   */
  public Collection<String> getCorpusFolderNameList() {
    return Collections.unmodifiableCollection(mCorpusFolders);
  }

  /**
   * Retrieves the editor line length hint parameter.
   * 
   * @return line length hint
   */
  public int getEditorLineLengthHint() {
    return mEditorLineLengthHint;
  }

  /**
   * Sets the editor line length hint parameter.
   * 
   * @param lineLengthHint
   */
  public void setEditorLineLength(int lineLengthHint) {
    mEditorLineLengthHint = lineLengthHint;
  }

  /**
   * Retrieves the annotation styles.
   * 
   * @return - the annotation styles
   */
  public Collection<AnnotationStyle> getAnnotationStyles() {
    return mStyleMap.values();
  }

  /**
   * Adds an AnnotationStyle. TODO: move style stuff to nlp project
   * 
   * @param style
   */
  public void setStyle(AnnotationStyle style) {

    boolean isDefaultAnnotation = AnnotationStyle.DEFAULT_COLOR.equals(style.getColor())
            && AnnotationStyle.DEFAULT_STYLE.equals(style.getStyle())
            && AnnotationStyle.DEFAULT_LAYER == style.getLayer();

    if (isDefaultAnnotation) {
      mStyleMap.remove(style.getAnnotation());
    } else {
      mStyleMap.put(style.getAnnotation(), style);
    }
  }

  /**
   * Removes an AnnotationStyle for the given name, does nothing if not existent.
   * 
   * @param name
   */
  public void removeStyle(String name) {
    mStyleMap.remove(name);
  }

  /**
   * Retrieves the AnnotationStyle for the given type or null if not available.
   * 
   * @param type
   * @return the requested style or null if none
   */
  public AnnotationStyle getAnnotation(Type type) {
    AnnotationStyle style = mStyleMap.get(type.getName());

    if (style == null) {
      style = new AnnotationStyle(type.getName(), AnnotationStyle.DEFAULT_STYLE,
              AnnotationStyle.DEFAULT_COLOR, 0);
    }

    return style;
  }

  /**
   * Always returns hash code 0.
   */
  @Override
  public int hashCode() {
    return 0;
  }
  
  /**
   * Checks if the given object is equal to the current instance.
   */
  @Override
  public boolean equals(Object obj) {

    boolean result;

    if (obj == this) {
      result = true;
    } else if (obj instanceof DotCorpus) {

      DotCorpus corpus = (DotCorpus) obj;

      result = isEqual(mTypeSystemFileName, corpus.mTypeSystemFileName)
              && isEqual(mCorpusFolders, corpus.mCorpusFolders)
              && isEqual(mCasProcessorFolders, corpus.mCasProcessorFolders)
              && isEqual(mStyleMap, corpus.mStyleMap)
              && isEqual(mEditorLineLengthHint, corpus.mEditorLineLengthHint);
    } else {
      result = false;
    }

    return result;
  }

  /**
   * Compares two objects for equality.
   * 
   * @param a
   *          the first object or null
   * @param b
   *          the second object or null
   * @return a.equals(b) or true if both null
   */
  private static boolean isEqual(Object a, Object b) {
    boolean result;

    if (a != null && b != null) {
      result = a.equals(b);
    } else {
      if (a == null && b == null) {
        result = true;
      } else {
        result = false;
      }
    }

    return result;
  }
}
