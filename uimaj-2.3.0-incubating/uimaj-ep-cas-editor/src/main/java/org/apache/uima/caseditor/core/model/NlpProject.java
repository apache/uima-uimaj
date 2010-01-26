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
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.delta.INlpElementDelta;
import org.apache.uima.caseditor.editor.AnnotationStyle;
import org.apache.uima.caseditor.editor.EditorAnnotationStatus;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

/**
 * TODO: add comment
 */
public final class NlpProject extends AbstractNlpElement implements IProjectNature, INlpElement,
        IAdaptable {
  /**
   * The ID of the <code>NLPProject</code>
   */
  public static final String ID = "org.apache.uima.caseditor.NLPProject";

  private static final String DOT_CORPUS_FILENAME = ".corpus";

  private NlpModel mModel;

  private IProject mProject;

  private DotCorpusElement mDotCorpusElement;

  private Collection<CorpusElement> mCopora = new LinkedList<CorpusElement>();

  private Collection<CasProcessorFolder> mUimaSourceFolder = new LinkedList<CasProcessorFolder>();
  
  private boolean mDotCorpusMustBeSerialized;
  
  private boolean mIsDotCorpusDirty;

  private EditorAnnotationStatus mEditorAnnotationStatus;

  private TypesystemElement mTypesystem;

  /**
   * Initializes the current nlp project instance.
   * 
   * @param model
   */
  void setNlpModel(NlpModel model) {
    mModel = model;
  }

  /**
   * Initializes the current instance. This method is called before this instance can be used.
   * Currently it recognizes and loads all nlp resources.
   * 
   * Note: This method should only be called during static {@link NlpModel} creation.
   * 
   * @throws CoreException
   */
  void initialize() throws CoreException {
    loadDotCorpus();

    createCorpora();

    for (CorpusElement corpus : getCorpora()) {
      corpus.initialize();
    }

    createCasProcessorFolders();

    // for (CasProcessorFolder sourceFolder : getCasProcessorFolders()) {
    // sourceFolder.initialize();
    // }

    IFile typeSystemFile = getDotCorpus().getTypeSystemFile();

    if (typeSystemFile != null && typeSystemFile.exists()) {
      mTypesystem = new TypesystemElement(typeSystemFile, this);
    }

    if (getTypesystemElement() != null && getTypesystemElement().getTypeSystem() != null) {

      TypeSystem typeSystem = getTypesystemElement().getTypeSystem();

      Type annotationType = typeSystem.getType(CAS.TYPE_NAME_ANNOTATION);

      List<Type> displayTypes = typeSystem.getProperlySubsumedTypes(annotationType);

      // removes the document annotation
      displayTypes.remove(typeSystem.getType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION));

      mEditorAnnotationStatus = new EditorAnnotationStatus(annotationType.getName(), displayTypes);
    }
  }

  /**
   * Retrieves the name.
   */
  public String getName() {
    return mProject.getName();
  }

  /**
   * Retrieves the resource.
   */
  public IResource getResource() {
    return getProject();
  }

  /**
   * Retrieves the parent element {@link NlpModel}.
   */
  public INlpElement getParent() {
    return mModel;
  }

  /**
   * Not implemented, called to configure the project
   */
  public void configure() throws CoreException {
    // not implemented
  }

  /**
   * Not implemented, called to de-configure the project
   */
  public void deconfigure() throws CoreException {
    // not implemented
  }

  /**
   * Retrieves the {@link IProject}.
   */
  public IProject getProject() {
    return mProject;
  }

  /**
   * Sets the {@link IProject}.
   */
  public void setProject(IProject project) {
    mProject = project;
  }

  /**
   * Retrieves the {@link EditorAnnotationStatus} for the current project instance.
   * 
   * @return status
   */
  public EditorAnnotationStatus getEditorAnnotationStatus() {
    return mEditorAnnotationStatus;
  }

  /**
   * Sets the {@link EditorAnnotationStatus} for the current project instance.
   * 
   * @param status
   */
  public void setEditorAnnotationStatus(EditorAnnotationStatus status) {
    mEditorAnnotationStatus = status;
  }

  private void loadDotCorpus() {
    IResource dotCorpusResource = getProject().getFile(DOT_CORPUS_FILENAME);

    mDotCorpusElement = DotCorpusElement.createDotCorpus((IFile) dotCorpusResource, this);

    // TODO: What happens when there is a folder with the name ".corpus"
    // then load default .corpus ...
  }

  /**
   * Retrieves the corpora.
   * 
   * @return the corpus collection
   */
  public Collection<CorpusElement> getCorpora() {
    return mCopora;
  }

  private void createCorpora() {
    for (IFolder corpusFolderName : mDotCorpusElement.getCorpusFolderNameList()) {
      if (corpusFolderName.exists()) {
        CorpusElement corpusElement = new CorpusElement(this, corpusFolderName);

        mCopora.add(corpusElement);
      }
    }
  }

  /**
   * Retrieves all non-nlp resources of the current instance.
   * 
   * @return the resources
   * @throws CoreException
   */
  public IResource[] getResources() throws CoreException {
    IResource[] resources;

    resources = mProject.members();

    LinkedList<IResource> resourceList = new LinkedList<IResource>();

    for (IResource element : resources) {
      if (isSpecialResource(element)) {
        continue;
      }

      if (element instanceof IFolder) {
        if (mDotCorpusElement.isCorpusFolder((IFolder) element)) {
          continue;
        }

        if (mDotCorpusElement.isCasProcessorFolder((IFolder) element)) {
          continue;
        }
      }

      if (mTypesystem != null && mTypesystem.getResource().equals(element)) {
        continue;
      }

      resourceList.add(element);
    }

    IResource[] filteredResources = new IResource[resourceList.size()];

    return resourceList.toArray(filteredResources);
  }

  /**
   * Retrieves the parent element for the given resource.
   */
  @Override
  public INlpElement getParent(IResource resource) throws CoreException {
    INlpElement result = super.getParent(resource);

    if (result == null) {
      if (mDotCorpusElement != null) {
        if (mDotCorpusElement.getResource().equals(resource)) {
          return this;
        }
      }

      if (mTypesystem != null) {
        if (mTypesystem.getResource().equals(resource)) {
          return this;
        }
      }

      for (IResource candiadte : getResources()) {
        if (candiadte.equals(resource)) {
          return this;
        }
      }

      for (CorpusElement corpus : getCorpora()) {
        INlpElement element = corpus.getParent(resource);

        if (element != null) {
          return element;
        }
      }

      for (CasProcessorFolder sourceFolder : getCasProcessorFolders()) {
        INlpElement element = sourceFolder.getParent(resource);

        if (element != null) {
          return element;
        }
      }
    }

    return null;
  }

  /**
   * Searches the {@link INlpElement} for the given resource.
   */
  @Override
  public INlpElement findMember(IResource resource) {
    INlpElement result = super.findMember(resource);

    if (result == null) {
      if (DOT_CORPUS_FILENAME.equals(resource.getName())) {
        return mDotCorpusElement;
      } else if (mTypesystem != null && mTypesystem.findMember(resource) != null) {
        return mTypesystem.findMember(resource);
      }

      for (CasProcessorFolder sourceFolder : getCasProcessorFolders()) {
        boolean isElementFound = sourceFolder.findMember(resource) != null;

        if (isElementFound) {
          return sourceFolder.findMember(resource);
        }
      }

      for (CorpusElement corpus : getCorpora()) {
        boolean isElementFound = corpus.findMember(resource) != null;

        if (isElementFound) {
          return corpus.findMember(resource);
        }
      }
    }

    return result;
  }

  /**
   * Retrieves the UimaSourceFolder
   * 
   * @return the UimaSourceFolder
   */
  public Collection<CasProcessorFolder> getCasProcessorFolders() {
    return mUimaSourceFolder;
  }

  private void createCasProcessorFolders() throws CoreException {
    for (IFolder processorFolder : mDotCorpusElement.getCasProcessorFolders()) {
      if (processorFolder.exists()) {
        CasProcessorFolder processorElement = new CasProcessorFolder(processorFolder, this);

        mUimaSourceFolder.add(processorElement);
      }
    }
  }

  /**
   * Retrieves the {@link DotCorpusElement}.
   * 
   * @return the {@link DotCorpusElement}
   */
  public DotCorpusElement getDotCorpus() {

    Assert.isTrue(mDotCorpusElement != null);

    return mDotCorpusElement;
  }

  /**
   * Retrieves the {@link NlpProject}.
   */
  public NlpProject getNlpProject() {
    return this;
  }

  private boolean isSpecialResource(IResource resource) {
    String specialResource[] = { ".project", DOT_CORPUS_FILENAME };

    for (String element : specialResource) {
      if (resource.getName().equals(element)) {
        return true;
      }
    }

    return false;
  }

  private void updateAnnotationTypeColors() throws CoreException {
    Collection<AnnotationStyle> styles = getDotCorpus().getAnnotationStyles();
    
    if (getTypesystemElement() != null) {
      TypeSystem ts = getTypesystemElement().getTypeSystem();
      if (ts != null) {
        Collection<AnnotationStyle> newStyles = DefaultColors.assignColors(ts, styles);
        
        for (AnnotationStyle style : newStyles) {
          getDotCorpus().setStyle(style);
        }
        
        if (styles.size() != newStyles.size()) {
          mDotCorpusMustBeSerialized = true;
        }
      }
    }
  }
  
  /**
   * Adds a resource to the current project instance.
   */
  @Override
  public void addResource(INlpElementDelta delta, IResource resource) throws CoreException {
    if (resource instanceof IFile) {
      IFile file = (IFile) resource;

      if (DOT_CORPUS_FILENAME.equals(file.getName())) {
        mIsDotCorpusDirty = true;
      }
      // if type system is set, it could had been renamed
      else if (mDotCorpusElement.getTypeSystemFile() != null
              && (delta.getFlags() & IResourceDelta.MOVED_FROM) > 0
              && mDotCorpusElement.getTypeSystemFile().getFullPath().equals(
                      delta.getMovedFromPath())) {
        // rename type system in dot corpus
        mDotCorpusElement.setTypeSystemFilename(resource.getName());

        // dot corpus was changed and must be writen
        // do this after the delta processing

        Runnable serialze = new Runnable() {

          public void run() {
            try {
              mDotCorpusElement.serialize();
            } catch (CoreException e) {
              CasEditorPlugin.log(e);
            }
          }
        };

        ((NlpModel) getNlpProject().getParent()).asyncExcuteQueue(serialze);
      }
      // check if file is type system
      else if (mDotCorpusElement.getTypeSystemFile() != null
              && mDotCorpusElement.getTypeSystemFile().equals(resource)) {
        mTypesystem = new TypesystemElement((IFile) resource, this);
        
        updateAnnotationTypeColors();
      }
    } else if (resource instanceof IFolder) {
      // corpus
      if (mDotCorpusElement.isCorpusFolder((IFolder) resource)) {
        mCopora.add(new CorpusElement(getNlpProject(), (IFolder) resource));
      } else if (mDotCorpusElement.isCasProcessorFolder((IFolder) resource)) {
        mUimaSourceFolder.add(new CasProcessorFolder((IFolder) resource, getNlpProject()));
      }
    }
  }

  /**
   * Removes a resource form the current project instance.
   */
  @Override
  public void removeResource(INlpElementDelta delta, IResource resource) {
    if (resource instanceof IFile) {
      IFile file = (IFile) resource;

      if (DOT_CORPUS_FILENAME.equals(file.getName())) {
        mIsDotCorpusDirty = true;
      }

      if (mTypesystem != null && resource.equals(mTypesystem.getResource())) {
        mTypesystem = null;
      }
    }

    for (CasProcessorFolder sourceFolder : mUimaSourceFolder) {
      if (sourceFolder.getResource().equals(resource)) {
        mUimaSourceFolder.remove(sourceFolder);
        break;
      }
    }

    for (CorpusElement corpus : mCopora) {
      if (corpus.getResource().equals(resource)) {
        mCopora.remove(corpus);
        break;
      }
    }
  }

  @Override
  void changedResource(IResource resource, INlpElementDelta delta) throws CoreException {
    if (DOT_CORPUS_FILENAME.equals(resource.getName())) {
      mIsDotCorpusDirty = true;
    } else if (mTypesystem != null && resource.equals(mTypesystem.getResource())) {
      mTypesystem.changedResource(resource, delta);

      if (getTypesystemElement().getTypeSystem() != null) {
        updateAnnotationTypeColors();
        mEditorAnnotationStatus = new EditorAnnotationStatus(CAS.TYPE_NAME_ANNOTATION, null);
      }
    }
  }

  void postProcessResourceChanges() throws CoreException {
    if (mIsDotCorpusDirty) {
      mIsDotCorpusDirty = false;

      mDotCorpusElement = null;
      loadDotCorpus();
      
      mUimaSourceFolder.clear();
      mCopora.clear();

      mTypesystem = null;

      initialize();
      
      updateAnnotationTypeColors();
      
      CasEditorPlugin.getNlpModel().fireRefreshEvent(this);
    }
    
    if (mDotCorpusMustBeSerialized) {
      Runnable writeDotCorpus = new Runnable() {
        public void run() {
          try {
            getDotCorpus().serialize();
          } catch (CoreException e) {
            CasEditorPlugin.log(e);
          }
        }
      };
      CasEditorPlugin.getNlpModel().asyncExcuteQueue(writeDotCorpus);
    }
  }

  /**
   * Uses the {@link #getName()} to generate the hash code.
   */
  @Override
  public int hashCode() {
    return getName().hashCode();
  }

  /**
   * Tests if the given object is equal to the current instance.
   */
  @Override
  public boolean equals(Object obj) {
    boolean isEqual;

    if (obj != null && obj instanceof NlpProject) {
      NlpProject project = (NlpProject) obj;

      isEqual = getResource().equals(project.getResource());
    } else {
      isEqual = false;
    }

    return isEqual;
  }

  /**
   * Adds a NLP nature to a project.
   * 
   * @param project
   *          the project to add the nature
   * 
   * @throws CoreException
   */
  public static void addNLPNature(IProject project) throws CoreException {
    IProjectDescription description = project.getDescription();
    String[] natures = description.getNatureIds();
    String[] newNatures = new String[natures.length + 1];
    System.arraycopy(natures, 0, newNatures, 0, natures.length);
    newNatures[natures.length] = NlpProject.ID;
    description.setNatureIds(newNatures);
    project.setDescription(description, null);
  }

  /**
   * Retrieves the type system.
   * 
   * @return type system
   */
  public TypesystemElement getTypesystemElement() {
    return mTypesystem;
  }
}
