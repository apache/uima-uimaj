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

import java.util.List;

import org.apache.uima.cas.text.AnnotationFS;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.swt.graphics.Point;

/**
 * TODO: move this class to external file.
 */
class AnnotationInformationProvider implements IInformationProvider, IInformationProviderExtension {

  /** The m editor. */
  private AnnotationEditor mEditor;

  /**
   * Instantiates a new annotation information provider.
   *
   * @param editor
   *          the editor
   */
  AnnotationInformationProvider(AnnotationEditor editor) {
    mEditor = editor;
  }

  /**
   * TODO: add comment.
   *
   * @param textViewer
   *          the text viewer
   * @param offset
   *          the offset
   * @return the region
   */
  @Override
  public IRegion getSubject(ITextViewer textViewer, int offset) {
    Point selection = textViewer.getTextWidget().getSelection();

    int length = selection.y - selection.x;
    return new Region(offset, length);
  }

  /**
   * TODO: add comment.
   *
   * @param textViewer
   *          the text viewer
   * @param subject
   *          the subject
   * @return null
   */
  @Override
  public String getInformation(ITextViewer textViewer, IRegion subject) {
    return null;
  }

  /**
   * TODO: add comment.
   *
   * @param textViewer
   *          the text viewer
   * @param subject
   *          the subject
   * @return the selected annotation
   */
  @Override
  public Object getInformation2(ITextViewer textViewer, IRegion subject) {
    List<AnnotationFS> selection = mEditor.getSelectedAnnotations();

    if (selection != null && selection.size() > 0) {
      return selection.get(0);
    } else {
      return null;
    }
  }
}