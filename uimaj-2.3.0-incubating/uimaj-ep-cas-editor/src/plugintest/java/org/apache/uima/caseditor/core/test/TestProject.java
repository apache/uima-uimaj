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

package org.apache.uima.caseditor.core.test;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.NlpProject;
import org.apache.uima.caseditor.core.model.dotcorpus.DotCorpus;
import org.apache.uima.caseditor.core.model.dotcorpus.DotCorpusSerializer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

/**
 */
public class TestProject {
  private IProject mProject;

  private IFile mDotCorpus;

  private IFolder mCorpusFolder;

  private IFolder mProcessorFolder;

  private IFile mTypesystem;

  private IFile mDocument;

  private IFile mAnnotatorFile;

  private IFile mConsumerFile;

  private IResource mContentResources[];

  public TestProject() {
    mProject = ResourcesPlugin.getWorkspace().getRoot().getProject("JUnitTestProject");

    mDotCorpus = mProject.getFile(".corpus");

    mCorpusFolder = mProject.getFolder("corpus");

    mProcessorFolder = mProject.getFolder("UimaSourceFolder");

    mTypesystem = mProject.getFile("Typesystem.xml");
    mDocument = mCorpusFolder.getFile("Document.xcas");
    mAnnotatorFile = mProcessorFolder.getFile("Annotator.ann");
    mConsumerFile = mProcessorFolder.getFile("Consumer.con");

    mContentResources = new IResource[] { mCorpusFolder, mProcessorFolder, mDocument,
        mAnnotatorFile, mConsumerFile };
  }

  IFile getDotCorpus() {
    return mDotCorpus;
  }

  IResource[] getResources() {
    return mContentResources;
  }

  IFile getAnnotatorFile() {
    return mAnnotatorFile;
  }

  IFile getConsumerFile() {
    return mConsumerFile;
  }

  IFolder getCorpusFolder() {
    return mCorpusFolder;
  }

  IFile getDocument() {
    return mDocument;
  }

  IFolder getProcessorFolder() {
    return mProcessorFolder;
  }

  IFile getTypesystem() {
    return mTypesystem;
  }

  IProject getProject() {
    return mProject;
  }

  /**
   * Creates an empty project with nlp nature.
   *
   * @throws CoreException
   */
  void createProject() throws CoreException {
    mProject.create(null);

    mProject.open(null);

    NlpProject.addNLPNature(mProject);
  }

  /**
   * Creates the Nlp Project content.
   *
   * @throws CoreException
   */
  void createProjectContent() throws CoreException {
    // create corpus folder
    mCorpusFolder.create(true, true, null);

    // create source folder
    mProcessorFolder.create(true, true, null);

    // create a typesystem file here ...
    mTypesystem.create(getClass().getResourceAsStream("/org/apache/uima/caseditor/core/test/Typesystem.xml"),
            true, null);

    // create annotator.ann
    mAnnotatorFile.create(getClass().getResourceAsStream("/org/apache/uima/caseditor/core/test/Annotator.ann"),
            true, null);

    // create consumer.con
    mConsumerFile.create(getClass().getResourceAsStream("/org/apache/uima/caseditor/core/test/Consumer.con"),
            true, null);

    // create a document
    mDocument.create(getClass().getResourceAsStream("/org/apache/uima/caseditor/core/test/Document.xcas"), true,
            null);
  }

  /**
   *
   * @throws CoreException
   */
  void createDotCorpus() throws CoreException {
    DotCorpus dotCorpus = new DotCorpus();

    dotCorpus.addCorpusFolder(mCorpusFolder.getName());
    dotCorpus.addCasProcessorFolder(mProcessorFolder.getName());
    dotCorpus.setTypeSystemFilename(mTypesystem.getName());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DotCorpusSerializer.serialize(dotCorpus, out);

    InputStream in = new ByteArrayInputStream(out.toByteArray());

    mDotCorpus.create(in, true, null);
  }

  void removeProject() throws CoreException {
    mProject.delete(true, null);
  }

  void removeProjectContent() throws CoreException {
    mDocument.delete(true, null);
    mCorpusFolder.delete(true, null);
    mAnnotatorFile.delete(true, null);
    mConsumerFile.delete(true, null);
    mProcessorFolder.delete(true, null);
  }

  void validateNlpProject() {
    // TODO: add nlp project validation
    for (IResource resource : mContentResources) {
      assertTrue(resource.getName() + " does not exist!", (CasEditorPlugin.getNlpModel().findMember(
              resource) != null));
    }
  }
}
