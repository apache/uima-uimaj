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

package org.apache.uima.caseditor.editor.styleview;

import org.apache.uima.cas.Type;
import org.apache.uima.caseditor.editor.AnnotationEditor;
import org.eclipse.core.runtime.IAdaptable;

/**
 * The Class AnnotationTypeNode.
 */
public class AnnotationTypeNode implements IAdaptable {

  /** The editor. */
  private AnnotationEditor editor;

  /** The type. */
  private Type type;

  /**
   * Instantiates a new annotation type node.
   *
   * @param editor
   *          the editor
   * @param type
   *          the type
   */
  AnnotationTypeNode(AnnotationEditor editor, Type type) {
    this.editor = editor;
    this.type = type;
  }

  /**
   * Gets the editor.
   *
   * @return the editor
   */
  public AnnotationEditor getEditor() {
    return editor;
  }

  /**
   * Gets the annotation type.
   *
   * @return the annotation type
   */
  public Type getAnnotationType() {
    return type;
  }

  @Override
  public boolean equals(Object obj) {

    if (obj instanceof AnnotationTypeNode) {

      AnnotationTypeNode typeNode = (AnnotationTypeNode) obj;

      return type.equals(typeNode.type);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return type.hashCode();
  }

  @Override
  public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {

    if (AnnotationTypeNode.class.equals(adapter)) {
      return this;
    }

    return null;
  }
}
