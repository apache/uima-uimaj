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

package org.apache.uima.caseditor.editor;

import java.util.Collection;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;


/**
 * The <code>AnnotationDocument</code> adapts the annotation document to the eclipse Document
 * (needed for the editor).
 * 
 * Note: Before an instance can be used the document must be set.
 */
class AnnotationDocument extends Document implements ICasDocument {

  /** The m document. */
  private ICasDocument mDocument;

  /** The line length hint. */
  private int lineLengthHint;

  /**
   * Instantiates a new annotation document.
   */
  public AnnotationDocument() {
    IPreferenceStore prefStore = CasEditorPlugin.getDefault().getPreferenceStore();
    lineLengthHint = prefStore.getInt(AnnotationEditorPreferenceConstants.EDITOR_LINE_LENGTH_HINT);

    if (lineLengthHint == 0)
      lineLengthHint = 80;
  }

  /**
   * Transform text.
   *
   * @param text the text
   * @return the string
   */
  private String transformText(String text) {
    if (lineLengthHint != 0 && text != null) {
      return wrapWords(text, lineLengthHint);
    } else {
      if (text != null)
        return text;
      else
        return "";
    }
  }

  /**
   * Call is forwarded to the set document.
   * 
   * @return the text
   */
  private String getText() {

    String text = getCAS().getDocumentText();
    return transformText(text);
  }

  /**
   * Notifies listener about a document change.
   */
  private void fireDocumentChanged() {
    DocumentEvent ev = new DocumentEvent();
    ev.fDocument = this;
    fireDocumentChanged(ev);
  }

  // public void setLineLengthHint(int lineLengthHint) {
  // this.lineLengthHint = lineLengthHint;
  // }

  /**
   * Sets the document.
   *
   * @param element the new document
   */
  public void setDocument(ICasDocument element) {
    mDocument = element;

    set(getText());
  }

  /**
   * Gets the document.
   *
   * @return the document
   */
  public ICasDocument getDocument() {
    return mDocument;
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param annotation the annotation
   */
  @Override
  public void addFeatureStructure(FeatureStructure annotation) {
    mDocument.addFeatureStructure(annotation);

    fireDocumentChanged();
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param annotations the annotations
   */
  @Override
  public void addFeatureStructures(Collection<? extends FeatureStructure> annotations) {
    mDocument.addFeatureStructures(annotations);

    fireDocumentChanged();
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param annotation the annotation
   */
  @Override
  public void removeFeatureStructure(FeatureStructure annotation) {
    mDocument.removeFeatureStructure(annotation);

    fireDocumentChanged();
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param annotationsToRemove the annotations to remove
   */
  @Override
  public void removeFeatureStructures(Collection<? extends FeatureStructure> annotationsToRemove) {
    mDocument.removeFeatureStructures(annotationsToRemove);

    fireDocumentChanged();
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param annotation the annotation
   */
  @Override
  public void update(FeatureStructure annotation) {
    mDocument.update(annotation);

    fireDocumentChanged();
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param annotations the annotations
   */
  @Override
  public void updateFeatureStructure(Collection<? extends FeatureStructure> annotations) {
    mDocument.updateFeatureStructure(annotations);

    fireDocumentChanged();
  }

  /**
   * Called to notify that the whole document has been changed and must now synchronized.
   */
  @Override
  public void changed() {
    mDocument.changed();

    fireDocumentChanged();
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param type the type
   * @return the annotations
   */
  @Override
  public Collection<AnnotationFS> getAnnotations(Type type) {
    return mDocument.getAnnotations(type);
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param listener the listener
   */
  @Override
  public void addChangeListener(ICasDocumentListener listener) {
    mDocument.addChangeListener(listener);
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param listener the listener
   */
  @Override
  public void removeChangeListener(ICasDocumentListener listener) {
    mDocument.removeChangeListener(listener);
  }

  /**
   * Wrap words at next space after lineLengthHint chars in a line. If the line is shorter than
   * lineLengthHint nothing happens. The space char is replaced with an line feed char.
   *
   * @param textString the text string
   * @param lineLengthHint the line length hint
   * @return input text with line breaks
   */
  private String wrapWords(String textString, int lineLengthHint) {

    char text[] = textString.toCharArray();

    int charCounter = 0;

    for (int i = 0; i < text.length; i++) {

      if (text[i] == '\r' || text[i] == '\n') {
        charCounter = 0;
      }

      if (charCounter > lineLengthHint && text[i] == ' ') {
        text[i] = '\n';
        charCounter = 0;
      }

      charCounter++;
    }

    return new String(text);
  }

  /**
   * Call is forwarded to the set document.
   * 
   * @return the {@link CAS}
   */
  @Override
  public CAS getCAS() {
    return mDocument.getCAS();
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param type the type
   * @return the type
   */
  @Override
  public Type getType(String type) {
    return mDocument.getType(type);
  }

  @Override
  public void switchView(String viewName) {

    // TODO: Optimize the text retrieval and update notification handling
    // Currently the text must be changed before switchView is called ...

    // HACK:
    // Replace the text like set() would do,
    // but without sending out notifications.
    // The stop and resume notification methods do not yield
    // the desired effect
    String text = transformText(getCAS().getView(viewName).getDocumentText());
    getStore().set(text);
    getTracker().set(text);

    // Note: Sends out view update notification
    mDocument.switchView(viewName);

  }

  @Override
  public String getTypeSystemText() {
    if(mDocument != null) {
      return mDocument.getTypeSystemText();
    }
    return null;
  }
}
