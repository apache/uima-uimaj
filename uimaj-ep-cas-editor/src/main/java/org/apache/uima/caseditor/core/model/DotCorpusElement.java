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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.apache.uima.cas.Type;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.delta.INlpElementDelta;
import org.apache.uima.caseditor.core.model.dotcorpus.DotCorpus;
import org.apache.uima.caseditor.core.model.dotcorpus.DotCorpusSerializer;
import org.apache.uima.caseditor.core.util.MarkerUtil;
import org.apache.uima.caseditor.editor.AnnotationStyle;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * The <code>DotCorpus</code> is responsible to load/store the project dependent configuration. It
 * has several methods to set and retrieve configuration parameters.
 */
public class DotCorpusElement extends AbstractNlpElement {
  private DotCorpus mDotCorpus;

  private IFile mResource;

  private NlpProject mNlpProject;

  /**
   * The creation of the DotCorpus class is controlled by the factory method
   * <code>createDefaultDotCorpus(...)</code>.
   * 
   * @param resource
   *          internal use only
   * @param nlpProject
   *          internal use only
   */
  private DotCorpusElement(IFile resource, NlpProject nlpProject) {
    mResource = resource;
    mNlpProject = nlpProject;
    mDotCorpus = new DotCorpus();
  }

  /**
   * Retrieves the name of the type system file.
   * 
   * @return - type system file name or null if no set
   */
  public IFile getTypeSystemFile() {
    IFile result;

    if (mDotCorpus.getTypeSystemFileName() != null) {
      result = getFile(mDotCorpus.getTypeSystemFileName());
    } else {
      result = null;
    }

    return result;
  }

  /**
   * Returns true if the given file is the type system file.
   * 
   * @param file
   * @return true if type system file otherwise false
   */
  public boolean isTypeSystemFile(IFile file) {
    return getTypeSystemFile().equals(file);
  }

  /**
   * Sets the type system file name.
   * 
   * @param filename
   *          type system file name
   */
  public void setTypeSystemFilename(String filename) {
    mDotCorpus.setTypeSystemFilename(filename);
  }

  /**
   * Retrieves the corpus folder names.
   * 
   * @return - corpus folder names
   */
  public Collection<IFolder> getCorpusFolderNameList() {
    Collection<IFolder> corpusFolders = new LinkedList<IFolder>();

    for (String corpusFolderString : mDotCorpus.getCorpusFolderNameList()) {
      corpusFolders.add(getFolder(corpusFolderString));
    }

    return Collections.unmodifiableCollection(corpusFolders);
  }

  /**
   * Adds a corpus folder.
   * 
   * @param name
   *          the corpus folder.
   */
  public void addCorpusFolder(String name) {
    mDotCorpus.addCorpusFolder(name);
  }

  /**
   * Removes the given corpus folder;
   * 
   * @param folder
   */
  public void removeCorpusFolder(IFolder folder) {
    mDotCorpus.removeCorpusFolder(folder.getName());
  }

  /**
   * Returns true if the given folder is a corpus folder.
   * 
   * @param folder
   * @return - true if corpus folder otherwise false
   */
  public boolean isCorpusFolder(IFolder folder) {
    boolean result = false;

    for (IFolder candidate : getCorpusFolderNameList()) {
      if (candidate.equals(folder)) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Retrieves the annotation styles.
   * 
   * @return - the annotation styles
   */
  Collection<AnnotationStyle> getAnnotationStyles() {
    return mDotCorpus.getAnnotationStyles();
  }

  /**
   * Adds an AnnotationStyle. TODO: move style stuff to nlp project
   * 
   * @param style
   */
  public void setStyle(AnnotationStyle style) {
    mDotCorpus.setStyle(style);
  }

  /**
   * Removes an AnnotationStyle for the given name, does nothing if not existent.
   * 
   * @param name
   */
  public void removeStyle(String name) {
    mDotCorpus.removeStyle(name);
  }

  /**
   * Retrieves the AnnotationStyle for the given type or null if not available.
   * 
   * @param type
   * @return the requested style or null if none
   */
  public AnnotationStyle getAnnotation(Type type) {
    return mDotCorpus.getAnnotation(type);
  }

  /**
   * Retrieves the config folder name.
   * 
   * @return - config folder name
   */
  public Collection<IFolder> getCasProcessorFolders() {

    Collection<IFolder> casProcessorFolders = new LinkedList<IFolder>();

    for (String corpusFolderString : mDotCorpus.getCasProcessorFolderNames()) {
      casProcessorFolders.add(getFolder(corpusFolderString));
    }

    return Collections.unmodifiableCollection(casProcessorFolders);
  }

  public boolean isCasProcessorFolder(IFolder folder) {
    boolean result = false;

    for (IFolder candidate : getCasProcessorFolders()) {
      if (candidate.equals(folder)) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Returns true if the given folder is a uima config folder.
   * 
   * @param folder
   * @return true if is config folder
   */
  public void addCasProcessorFolder(String folder) {
    mDotCorpus.addCasProcessorFolder(folder);
  }

  /**
   * Sets the config folder name
   * 
   * @param name
   *          the new name
   */
  public void removeCasProcessorFolder(String name) {
    mDotCorpus.removeCasProcessorFolder(name);
  }

  private IFile getFile(String name) {
    IProject project = (IProject) getNlpProject().getResource();

    return project.getFile(name);
  }

  private IFolder getFolder(String name) {
    IProject project = (IProject) getNlpProject().getResource();

    return project.getFolder(name);
  }

  /**
   * Retrieves the line length hint of the editor.
   * 
   * @return line length hint of the current editor, 0 means disabled
   */
  public int getEditorLineLengthHint() {
    return mDotCorpus.getEditorLineLengthHint();
  }

  /**
   * Sets the line length hint of the current editor.
   * 
   * @param lineLengthHint
   *          line length hint of the current editor, 0 means disabled
   */
  public void setEditorLineLengthHint(int lineLengthHint) {
    mDotCorpus.setEditorLineLength(lineLengthHint);
  }

  /**
   * Serializes the <code>DotCorpus</code> instance to the given <code>IFile</code>.
   * 
   * @throws CoreException
   */
  public void serialize() throws CoreException {
    ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();

    DotCorpusSerializer.serialize(mDotCorpus, outBuffer);

    if (mResource.exists()) {
      mResource.setContents(new ByteArrayInputStream(outBuffer.toByteArray()), true, true, null);
    } else {
      mResource.create(new ByteArrayInputStream(outBuffer.toByteArray()), true, null);
    }
  }

  /**
   * Always returns hash code 0.
   */
  @Override
  public int hashCode() {
    return 0;
  }
  
  /**
   * Test for equality with another object.
   */
  @Override
  public boolean equals(Object object) {

    boolean result;

    if (this == object) {
      result = true;
    } else if (object instanceof DotCorpusElement) {

      DotCorpusElement element = (DotCorpusElement) object;

      result =
              mDotCorpus.equals(element.mDotCorpus) && mResource.equals(element.mResource)
                      && mNlpProject.equals(element.mNlpProject);
    } else {
      result = false;
    }

    return result;
  }

  /**
   * Creates a new <code>DotCorpus</code> object from an <code>IFile</code> object. If creation
   * fails, the default dotCorpus is returned and if possible markers are added to the invalid
   * .corpus resource.
   * 
   * @param file
   * @param project
   * @return - the new <code>DotCorpus</code> instance.
   */
  static DotCorpusElement createDotCorpus(final IFile file, NlpProject project) {
    DotCorpusElement dotCorpusElement = new DotCorpusElement(file, project);

    if (file.exists()) {
      Runnable clearMarkers = new Runnable() {
        public void run() {
          try {
            MarkerUtil.clearMarkers(file, MarkerUtil.PROBLEM_MARKER);
          } catch (CoreException e) {
            CasEditorPlugin.log(e);
          }
        }
      };
      ((NlpModel) project.getParent()).asyncExcuteQueue(clearMarkers);

      try {
        dotCorpusElement.mDotCorpus = DotCorpusSerializer.parseDotCorpus(file.getContents());
      } catch (final CoreException e) {
        Runnable createMarker = new Runnable() {
          public void run() {
            try {
              MarkerUtil.createMarker(file, e.getMessage());
            } catch (CoreException e2) {
              CasEditorPlugin.log(e2);
            }
          }
        };
        ((NlpModel) project.getParent()).asyncExcuteQueue(createMarker);
      }
    }

    return dotCorpusElement;
  }

  /**
   * Retrieves the parent.
   */
  public INlpElement getParent() {
    return mNlpProject;
  }

  /**
   * Retrieves the dot corpus resource.
   */
  public IResource getResource() {
    return mResource;
  }

  /**
   * Retrieves the nlp project
   */
  public NlpProject getNlpProject() {
    return mNlpProject;
  }

  /**
   * Retrieves the dot corpus name
   */
  public String getName() {
    // TODO: this name is not hardcoded .... look at NlpProject
    return ".corpus";
  }

  /**
   * Not implemented.
   */
  @Override
  void addResource(INlpElementDelta delta, IResource resource) {
    // not needed here, there are no child resources
  }

  /**
   * Not implemented.
   */
  @Override
  void removeResource(INlpElementDelta delta, IResource resource) {
    // not needed here, there are no child resources
  }
}
