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

package org.apache.uima.cas.text;

import org.apache.uima.cas.AnnotationBaseFS;

/**
 * Interface for Annotation Feature Structures.
 * 
 * 
 */
public interface AnnotationFS extends AnnotationBaseFS {

  /**
   * Get the start position of the annotation as character offset into the text. The smallest
   * possible start position is <code>0</code>, the offset of the first character in the text.
   * 
   * @return The start position.
   */
  int getBegin();

  /**
   * Get the end position of the annotation as character offset into the text. The end position
   * points at the first character after the annotation, such that
   * <code>(getEnd()-getBegin()) == getCoveredText().length()</code>.
   * 
   * @return The end position.
   */
  int getEnd();

  /**
   * Get the text covered by an annotation as a string. If <code>docText</code> is your document
   * text and <code>annot</code> an annotation, then <code>
   * annot.getCoveredText().equals(docText.substring(annot.getBegin(), 
   * annot.getEnd()))</code>.
   * 
   * @return String
   */
  String getCoveredText();

}
