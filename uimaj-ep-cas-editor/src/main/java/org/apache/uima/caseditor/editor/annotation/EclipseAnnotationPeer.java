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

package org.apache.uima.caseditor.editor.annotation;

import org.apache.uima.cas.text.AnnotationFS;

/**
 * This class is used to provide additional information about the {@link AnnotationFS}
 * object to the custom drawing strategies.
 */
public class EclipseAnnotationPeer extends org.eclipse.jface.text.source.Annotation {

  private final AnnotationFS annotation;

  /**
   * Initializes a new instance.
   *
   * @param annotation
   */
  public EclipseAnnotationPeer(AnnotationFS annotation) {
    super(annotation.getType().getName(), false, "");
    this.annotation = annotation;
    setText(annotation.getCoveredText());
  }

  /**
   * Retrieves the annotation.
   *
   * @return the annotation
   */
  public AnnotationFS getAnnotationFS() {
    return annotation;
  }
  
  @Override
  public int hashCode() {
    return annotation.hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    else if (obj instanceof EclipseAnnotationPeer) {
      EclipseAnnotationPeer peer = (EclipseAnnotationPeer) obj;
      
      return annotation.equals(peer.annotation);
    }
    else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return annotation.toString();
  }
}
