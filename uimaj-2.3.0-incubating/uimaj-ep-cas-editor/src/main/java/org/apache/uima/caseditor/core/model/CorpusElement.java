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

package org.apache.uima.caseditor.core.model;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.uima.caseditor.core.model.delta.INlpElementDelta;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IDocument;

/**
 * The CorpusElement is a container for {@link IDocument}s.
 * 
 * TODO: do not include defective elements!
 */
public final class CorpusElement extends AbstractNlpElement implements INlpElement, IAdaptable {
  private NlpProject mParentElement;

  private IFolder mCorpusFolder;

  private Collection<DocumentElement> mDocuments = new LinkedList<DocumentElement>();

  /**
   * Creates a new <code>CorpusElement</code> object.
   * 
   * @param nlpProject
   * @param corpusFolder
   */
  CorpusElement(NlpProject nlpProject, IFolder corpusFolder) {
    mParentElement = nlpProject;
    mCorpusFolder = corpusFolder;
  }

  void initialize() throws CoreException {
    createDocuments();
  }

  private IFolder getCorpusFolder() {
    return mCorpusFolder;
  }

  /**
   * Checks if the current document still exists.
   * 
   * @return true if exists
   */
  public boolean exists() {
    return getCorpusFolder() != null;
  }

  /**
   * Retrieves all documents contained in the current corpus instance.
   * 
   * @return the documents
   */
  public Collection<DocumentElement> getDocuments() {
    return mDocuments;
  }

  /**
   * Returns all <code>DocumentElement</code>s inside this corpus.
   * 
   * @throws CoreException
   */
  private void createDocuments() throws CoreException {
    IResource[] resources = getCorpusFolder().members();

    for (IResource resource : resources) {
      if (resource instanceof IFile) {
        mDocuments.add(new DocumentElement(this, (IFile) resource));
      }
    }
  }

  /**
   * Retrieves the resource of the current instance.
   */
  public IResource getResource() {
    return getCorpusFolder();
  }

  /**
   * Retrieves the parent of the current instance.
   */
  public INlpElement getParent() {
    return mParentElement;
  }

  /**
   * Retrieves the parent for the given resource or null if not found.
   */
  @Override
  public INlpElement getParent(IResource resource) throws CoreException {
    INlpElement result = super.getParent(resource);

    if (result == null) {
      for (DocumentElement document : getDocuments()) {
        INlpElement element = document.getParent(resource);

        if (element != null) {
          result = element;
          break;
        }
      }
    }

    return result;
  }

  /**
   * Retrieves the name of the current instance.
   * 
   * @return the name
   */
  public String getName() {
    return getCorpusFolder().getName();
  }

  /**
   * Retrieves the nlp element for the given resource. If contained by the current element or one of
   * its children.
   */
  @Override
  public INlpElement findMember(IResource resource) {
    if (mCorpusFolder.equals(resource)) {
      return this;
    }

    if (!exists()) {
      return null;
    }

    Collection<DocumentElement> documents = getDocuments();

    for (DocumentElement document : documents) {
      boolean isElementFound = document.findMember(resource) != null;

      if (isElementFound) {
        return document.findMember(resource);
      }
    }

    return null;
  }

  /**
   * Retrieves the top level project.
   */
  public NlpProject getNlpProject() {
    return (NlpProject) getParent();
  }

  /**
   * Adds the given resource.
   * 
   * @param resource
   */
  @Override
  void addResource(INlpElementDelta delta, IResource resource) {
    if (resource instanceof IFile) {
      mDocuments.add(new DocumentElement(this, (IFile) resource));
    }
  }

  @Override
  void changedResource(IResource resource, INlpElementDelta delta) {
    for (DocumentElement document : getDocuments()) {
      if (document.getResource().equals(resource)) {
        document.changedResource(resource, delta);
        break;
      }
    }
  }

  /**
   * Removes the given resource.
   * 
   * @param resource
   */
  @Override
  void removeResource(INlpElementDelta delta, IResource resource) {
    for (DocumentElement document : mDocuments) {
      if (document.getResource().equals(resource)) {
        mDocuments.remove(document);
        break;
      }
    }
  }

  /**
   * Uses the getName().hashCode().
   */
  @Override
  public int hashCode() {
    return getName().hashCode();
  }

  /**
   * Checks if the given object is equal to the current instance.
   */
  @Override
  public boolean equals(Object obj) {
    boolean result;

    if (obj != null && obj instanceof CorpusElement) {
      CorpusElement element = (CorpusElement) obj;

      result = getResource().equals(element.getResource());
    } else {
      result = false;
    }

    return result;
  }
}
