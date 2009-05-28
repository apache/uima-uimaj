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
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.caseditor.editor.util.Span;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;

/**
 * The <code>AnnotationDocument</code> adapts the annotation document to the eclipse Document
 * (needed for the editor).
 *
 * Note: Before an instance can be used the  document must be set.
 */
public class AnnotationDocument extends Document implements ICasDocument {

  private ICasDocument mDocument;

  private int lineLengthHint = 80;

  public AnnotationDocument() {
  }

  public void setLineLengthHint(int lineLengthHint) {
    this.lineLengthHint = lineLengthHint;
  }

  /**
   * @param element
   */
  public void setDocument(ICasDocument element) {
    mDocument = element;

    set(getText());
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param annotation
   */
  public void addFeatureStructure(FeatureStructure annotation) {
    mDocument.addFeatureStructure(annotation);

    fireDocumentChanged();
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param annotations
   */
  public void addFeatureStructures(Collection<FeatureStructure> annotations) {
    mDocument.addFeatureStructures(annotations);

    fireDocumentChanged();
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param annotations
   */
  public void addAnnotations(Collection<AnnotationFS> annotations) {
    mDocument.addAnnotations(annotations);

    fireDocumentChanged();
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param annotation
   */
  public void removeFeatureStructure(FeatureStructure annotation) {
    mDocument.removeFeatureStructure(annotation);

    fireDocumentChanged();
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param annotationsToRemove
   */
  public void removeFeatureStructures(Collection<FeatureStructure> annotationsToRemove) {
    mDocument.removeFeatureStructures(annotationsToRemove);

    fireDocumentChanged();
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param annotationsToRemove
   */
  public void removeAnnotations(Collection<AnnotationFS> annotationsToRemove) {
    mDocument.removeAnnotations(annotationsToRemove);

    fireDocumentChanged();
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param annotation
   */
  public void update(FeatureStructure annotation) {
    mDocument.update(annotation);

    fireDocumentChanged();
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param annotations
   */
  public void updateFeatureStructure(Collection<FeatureStructure> annotations) {
    mDocument.updateFeatureStructure(annotations);

    fireDocumentChanged();
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param annotations
   */
  public void updateAnnotations(Collection<AnnotationFS> annotations) {
    mDocument.updateAnnotations(annotations);

    fireDocumentChanged();
  }

  /**
   * Called to notify that the whole document has been changed and
   * must now synchronized.
   */
  public void changed() {
    mDocument.changed();

    fireDocumentChanged();
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param type
   * @return the annotations
   */
  public Collection<AnnotationFS> getAnnotations(Type type) {
    return mDocument.getAnnotations(type);
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param annotationType
   * @return the view map
   */
  public Map<Integer, AnnotationFS> getView(Type annotationType) {
    return mDocument.getView(annotationType);
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param listener
   */
  public void addChangeListener(ICasDocumentListener listener) {
    mDocument.addChangeListener(listener);
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param listener
   */
  public void removeChangeListener(ICasDocumentListener listener) {
    mDocument.removeChangeListener(listener);
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param type
   * @param span
   * @return the annotations
   */
  public Collection<AnnotationFS> getAnnotation(Type type, Span span) {
    return mDocument.getAnnotation(type, span);
  }

  /**
   * Notifies listener about a document change.
   */
  public void fireDocumentChanged() {
    DocumentEvent ev = new DocumentEvent();
    ev.fDocument = this;
    fireDocumentChanged(ev);
  }

  /**
   * Wrap words at next space after lineLengthHint chars in a line.
   * If the line is shorter than lineLengthHint nothing happens.
   * The space char is replaced with an line feed char.
   *
   * @param textString
   * @param lineLengthHint
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
   * @return the text
   */
  public String getText() {

    if (lineLengthHint != 0) {
      return wrapWords(mDocument.getText(), lineLengthHint);
    } else {
      return mDocument.getText();
    }
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param start
   * @param end
   * 
   * @return the text
   */
  public String getText(int start, int end) {
    return getText().substring(start, end);
  }

  /**
   * Call is forwarded to the set document.
   *
   * @return the {@link CAS}
   */
  public CAS getCAS() {
    return mDocument.getCAS();
  }

  /**
   * Call is forwarded to the set document.
   *
   * @param type
   * @return the type
   */
  public Type getType(String type) {
    return mDocument.getType(type);
  }
}
