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

package org.apache.uima.caseditor.editor.util;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.uima.cas.text.AnnotationFS;

/**
 * Checks two annotations for equality.
 */
public class AnnotationComparator implements Comparator<AnnotationFS>, Serializable  {
  
  private static final long serialVersionUID = 1L;

  /**
   * Compares the given annotations a and b. This implementations only compares the begin index.
   */
  public int compare(AnnotationFS a, AnnotationFS b) {
    return a.getBegin() - b.getBegin();
  }
}
