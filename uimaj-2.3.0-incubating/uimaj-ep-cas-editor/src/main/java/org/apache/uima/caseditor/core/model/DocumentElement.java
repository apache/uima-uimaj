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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.SoftReference;

import org.apache.uima.cas.CAS;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.delta.INlpElementDelta;
import org.apache.uima.caseditor.editor.DocumentFormat;
import org.apache.uima.caseditor.editor.DocumentUimaImpl;
import org.apache.uima.caseditor.editor.ICasDocument;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * The document element contains the uima cas document.
 */
public final class DocumentElement extends AbstractNlpElement implements IAdaptable {
  private CorpusElement mParent;

  private IFile mDocumentFile;

  /**
   * The working copy of the document. This instance is shared by everyone who wants to edit the
   * document.
   */
  private SoftReference<DocumentUimaImpl> mWorkingCopy = new SoftReference<DocumentUimaImpl>(null);

  private boolean isSavingWorkingCopy;

  /**
   * Initializes a new instance.
   * 
   * @param corpus
   * @param documentFile
   */
  DocumentElement(CorpusElement corpus, IFile documentFile) {

    if (corpus == null || documentFile == null) {
      throw new IllegalArgumentException("Parameters must not be null!");
    }

    mParent = corpus;
    mDocumentFile = documentFile;
  }

  /**
   * Retrieves the corresponding resource.
   */
  public IFile getResource() {
    return mDocumentFile;
  }

  /**
   * Retrieves the name.
   */
  public String getName() {
    return mDocumentFile.getName();
  }

  /**
   * Retrieves the parent.
   */
  public INlpElement getParent() {
    return mParent;
  }

  /**
   * Retrieves the working copy.
   * 
   * @throws CoreException
   * @return the working copy
   */
  public ICasDocument getDocument(boolean reload) throws CoreException {

    NlpProject project = (NlpProject) mParent.getParent();

    if (project.getTypesystemElement() == null) {
      mWorkingCopy = null;
      throw new CoreException(new Status(IStatus.ERROR, CasEditorPlugin.ID, 0,
              "Typesystem not available!", null));
    }

    DocumentUimaImpl document = mWorkingCopy.get();

    if (reload || document == null) {

      InputStream in = mDocumentFile.getContents();

      DocumentFormat format;

      if (getResource().getFileExtension().equalsIgnoreCase("xcas")) {
        format = DocumentFormat.XCAS;
      } else if (getResource().getFileExtension().equalsIgnoreCase("xmi")) {
        format = DocumentFormat.XMI;
      } else {
        throw new CoreException(new Status(IStatus.ERROR, CasEditorPlugin.ID, 0,
                "Unkown file extension!", null));
      }

      // TODO: check if this is correct this way
      CAS cas = project.getTypesystemElement().getCAS();

      document = new DocumentUimaImpl(cas, in, format);

      mWorkingCopy = new SoftReference<DocumentUimaImpl>(document);
    }

    return document;
  }

  /**
   * Writes the document element to the file system.
   * 
   * TODO: move it to the document, maybe the document gets not saved if the caller lost the
   * reference to it, before this call
   * 
   * @throws CoreException
   */
  public void saveDocument() throws CoreException {

    isSavingWorkingCopy = true;

    ByteArrayOutputStream outStream = new ByteArrayOutputStream(40000);

    ((DocumentUimaImpl) getDocument(false)).serialize(outStream);

    InputStream stream = new ByteArrayInputStream(outStream.toByteArray());

    mDocumentFile.setContents(stream, true, false, null);
  }

  /**
   * Retrieves the corresponding {@link NlpProject} instance.
   * 
   * @return the {@link NlpProject} instance
   */
  public NlpProject getNlpProject() {
    return (NlpProject) getParent().getParent();
  }

  /**
   * Not implemented.
   */
  @Override
  void addResource(INlpElementDelta delta, IResource resource) {
    // not needed here, there are no resources
  }

  @Override
  void changedResource(IResource resource, INlpElementDelta delta) {
    // TODO: What should happen if the document is changed externally
    // e.g. with a text editor ?

    // if saveDocument() was called, we receive a changedResource event
    // in this case do not remove a reference to the working copy, cause its in sync
    if (!isSavingWorkingCopy) {
      mWorkingCopy = new SoftReference<DocumentUimaImpl>(null);
    } else {
      isSavingWorkingCopy = false;
    }

  }

  /**
   * Not implemented.
   */
  @Override
  void removeResource(INlpElementDelta delta, IResource resource) {
    // not needed here, there are no resources
  }

  /**
   * Generates a hash code for the current instance.
   */
  @Override
  public int hashCode() {
    return getName().hashCode();
  }
}
