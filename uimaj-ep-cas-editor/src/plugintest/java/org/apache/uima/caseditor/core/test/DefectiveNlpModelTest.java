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

import org.apache.uima.caseditor.editor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.AnnotatorElement;
import org.apache.uima.caseditor.core.model.ConsumerElement;
import org.apache.uima.caseditor.core.model.DocumentElement;
import org.apache.uima.caseditor.core.model.TypesystemElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.junit.Before;
import org.junit.Test;

/**
 * This test produces invalid model elements and tests the nlp model for the
 * specified behavior.
 */
public class DefectiveNlpModelTest {
  private TestProject mProject;

  /**
   * Initializes the current instance.
   */
  public DefectiveNlpModelTest() {
    mProject = new TestProject();
  }

  /**
   * @throws CoreException
   */
  @Before
  public void clearWorkspace() throws CoreException {
    WorkspaceUtil.clearWorkspace();
  }

  @Test
  public void testInvalidDotCorpus() throws CoreException {
    mProject.createProject();
    mProject.createProjectContent();

    // create invalid dot corpus
    mProject.getDotCorpus().create(new ByteArrayInputStream(new byte[0]), true, null);

    // check for default dotCorpus file
    assertTrue(CasEditorPlugin.getNlpModel().findMember(mProject.getDotCorpus()) != null);

    // check that no content elements exist
    for (IResource resource : mProject.getResources()) {
      assertTrue(CasEditorPlugin.getNlpModel().findMember(resource) == null);
    }
  }

  @Test
  public void testFolderDotCorpus() throws CoreException {
    mProject.createProject();
    mProject.createProjectContent();

    // create invalid dot corpus
    IFolder dotCorpusFolder = mProject.getProject().getFolder(".corpus");
    dotCorpusFolder.create(true, true, null);

    // check for default dotCorpus file
    assertTrue(CasEditorPlugin.getNlpModel().findMember(mProject.getDotCorpus()) != null);

    // check that no content elements exist
    for (IResource resource : mProject.getResources()) {
      assertTrue(CasEditorPlugin.getNlpModel().findMember(resource) == null);
    }
  }

  @Test(expected=CoreException.class)
  public void testInvalidDocument() throws CoreException {
    mProject.createProject();
    mProject.createProjectContent();
    mProject.createDotCorpus();

    mProject.getDocument().setContents(new ByteArrayInputStream(new byte[0]), true, true, null);

    // throws a core exception since the document cannot be parsed
    ((DocumentElement) CasEditorPlugin.getNlpModel().findMember(mProject.getDocument()))
            .getDocument(true);
  }

  @Test
  public void testInvalidAnnotator() throws CoreException {
    mProject.createProject();
    mProject.createProjectContent();
    mProject.createDotCorpus();

    mProject.getAnnotatorFile()
            .setContents(new ByteArrayInputStream(new byte[0]), true, true, null);

    assertTrue(((AnnotatorElement) CasEditorPlugin.getNlpModel().findMember(
            mProject.getAnnotatorFile())).getAnnotatorConfiguration() == null);
  }

  @Test
  public void testInvalidConsumer() throws CoreException {
    mProject.createProject();
    mProject.createProjectContent();
    mProject.createDotCorpus();

    mProject.getConsumerFile().setContents(new ByteArrayInputStream(new byte[0]), true, true, null);

    assertTrue(((ConsumerElement) CasEditorPlugin.getNlpModel()
            .findMember(mProject.getConsumerFile())).getConsumerConfiguration() == null);
  }

  @Test
  public void testInvalidTypesystem() throws CoreException {
    mProject.createProject();
    mProject.createProjectContent();
    mProject.createDotCorpus();

    mProject.getTypesystem().setContents(new ByteArrayInputStream(new byte[0]), true, true, null);

    assertTrue(((TypesystemElement) CasEditorPlugin.getNlpModel().findMember(
            mProject.getTypesystem())).getTypeSystem() == null);
  }

  @Test
  public void testFileForCorpusFolder() throws CoreException {
    mProject.createProject();
    mProject.createDotCorpus();

    IFile corpusFile = mProject.getProject().getFile("corpus");
    corpusFile.create(new ByteArrayInputStream(new byte[0]), true, null);

    // check that model and project creation did not failed
    assertTrue(CasEditorPlugin.getNlpModel().findMember(mProject.getProject()) != null);
  }

  @Test
  public void testFileForSourceFolder() throws CoreException {
    mProject.createProject();
    mProject.createDotCorpus();

    IFile corpusFile = mProject.getProject().getFile("UimaSourceFolder");
    corpusFile.create(new ByteArrayInputStream(new byte[0]), true, null);

    // check that model and project creation did not failed
    assertTrue(CasEditorPlugin.getNlpModel().findMember(mProject.getProject()) != null);
  }
}