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

package org.apache.uima.caseditor.uima;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas_data.CasData;
import org.apache.uima.caseditor.core.model.CorpusElement;
import org.apache.uima.caseditor.core.model.DocumentElement;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.collection.impl.CasConverter;
import org.apache.uima.util.Progress;
import org.eclipse.core.runtime.CoreException;

/**
 * This is a collection reader which reads the documents from a {@link Collection} of
 * {@link CorpusElement}s.
 */
public class CorporaCollectionReader extends CollectionReader_ImplBase {

  private Iterator<DocumentElement> documentIterator;

  /**
   * Sets the <code>CorpusElement</code>s to be read.
   *
   * @param corpora
   */
  public void setCorpora(Collection<CorpusElement> corpora) {
    if (corpora != null) {
      List<DocumentElement> documents = new LinkedList<DocumentElement>();

      for (CorpusElement element : corpora) {
        documents.addAll(element.getDocuments());
      }

      documentIterator = documents.iterator();
    }
  }

  /**
   * Copies the next text with all annotation to the given cas object.
   *
   * @throws CollectionException -
   */
  public void getNext(CAS cas) throws CollectionException {
    DocumentElement document = documentIterator.next();

    CAS documentCas = null;

    try {
      documentCas = document.getDocument(false).getCAS();
    } catch (CoreException e) {
      // TODO Handle this exception well
      e.printStackTrace();
    }

    CasConverter converter = new CasConverter();

    CasData documentData = converter.casContainerToCasData(documentCas);

    converter.casDataToCasContainer(documentData, cas, true);
  }

  /**
   * Checks if there is one more cas available.
   */
  public boolean hasNext() {
    if (documentIterator != null) {
      return documentIterator.hasNext();
    } else {
      return false;
    }
  }

  /**
   * Currently not implemented
   *
   * @return Progress[] - just returns null
   */
  public Progress[] getProgress() {
    return new Progress[] {};
  }

  /**
   * Currently not implemented
   */
  public void close() {
    // currently not implemented
  }
}