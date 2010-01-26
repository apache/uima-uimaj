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

package org.apache.uima.caseditor.ui.corpusview;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.caseditor.core.model.INlpElement;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

/**
 * Utility class for the corpus explorer
 */
final class CorpusExplorerUtil {

  /**
   * Avoids instantiation of this utility class
   */
  private CorpusExplorerUtil() {
    // overridden to change access to private
  }

  static IStructuredSelection convertNLPElementsToResources(IStructuredSelection selection) {

    List<Object> newSelectionList = new LinkedList<Object>();

    for (Iterator<?> elements = selection.iterator(); elements.hasNext();) {
      Object element = elements.next();

      if (element instanceof INlpElement) {
        INlpElement nlpElement = (INlpElement) element;
        newSelectionList.add(nlpElement.getResource());
      } else {
        newSelectionList.add(element);
      }
    }

    return new StructuredSelection(newSelectionList);
  }

  /**
   * TODO: Replace this method, the name is very ugly.
   * 
   * @param selection
   * @return true if NLPProject or NonNLPResource is included
   */
  static boolean isContaingOnlyNlpElements(IStructuredSelection selection) {
    boolean isContaingOnlyNlpElements = true;

    for (Iterator<?> resources = selection.iterator(); resources.hasNext()
            && isContaingOnlyNlpElements;) {
      Object resource = resources.next();

      if (!(resource instanceof INlpElement)) {
        isContaingOnlyNlpElements = false;
      }
    }

    return isContaingOnlyNlpElements;
  }
}
