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

package org.apache.uima.caseditor.editor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.ResourceSpecifierFactory;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.editor.util.StrictTypeConstraint;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.impl.ResourceManager_impl;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.FsIndexDescription_impl;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * This document implementation is based on an uima cas object.
 */
public class DocumentUimaImpl extends AbstractDocument {

  public static final String JAVA_NATURE = "org.eclipse.jdt.core.javanature";
  
  private CAS mCAS;

  private SerialFormat format = SerialFormat.XMI;

  private final String typeSystemText;

  /**
   * Initializes a new instance.
   * 
   * @param cas
   * @param casFile
   * @param typeSystemText
   *          type system string
   */
  public DocumentUimaImpl(CAS cas, IFile casFile, String typeSystemText) throws CoreException {
    mCAS = cas;

    this.typeSystemText = typeSystemText;

    setContent(casFile);
  }

  /**
   * Retrieves the {@link CAS}.
   */
  public CAS getCAS() {
    return mCAS;
  }

  @Override
  public String getTypeSystemText() {
    return typeSystemText;
  }

  /**
   * Internally removes an annotation from the {@link CAS}.
   * 
   * @param featureStructure
   */
  private void addFeatureStructureInternal(FeatureStructure featureStructure) {
    getCAS().getIndexRepository().addFS(featureStructure);
  }

  /**
   * Adds the given annotation to the {@link CAS}.
   */
  public void addFeatureStructure(FeatureStructure annotation) {
    addFeatureStructureInternal(annotation);

    fireAddedFeatureStructure(annotation);
  }

  public void addFeatureStructures(Collection<? extends FeatureStructure> annotations) {
    for (FeatureStructure annotation : annotations) {
      addFeatureStructureInternal(annotation);
    }

    if (annotations.size() > 0) {
      fireAddedFeatureStructure(annotations);
    }
  }

  /**
   * Internally removes an annotation from the {@link CAS}.
   * 
   * @param featureStructure
   */
  private void removeAnnotationInternal(FeatureStructure featureStructure) {
    getCAS().getIndexRepository().removeFS(featureStructure);
  }

  /**
   * Removes the annotations from the {@link CAS}.
   */
  public void removeFeatureStructure(FeatureStructure annotation) {
    removeAnnotationInternal(annotation);

    fireRemovedFeatureStructure(annotation);
  }

  /**
   * Removes the given annotations from the {@link CAS}.
   */
  public void removeFeatureStructures(Collection<? extends FeatureStructure> annotationsToRemove) {

    for (FeatureStructure annotationToRemove : annotationsToRemove) {
      removeAnnotationInternal(annotationToRemove);
    }

    if (annotationsToRemove.size() > 0) {
      fireRemovedFeatureStructure(annotationsToRemove);
    }
  }

  /**
   * Notifies clients about the changed annotation.
   */
  public void update(FeatureStructure annotation) {
    fireUpdatedFeatureStructure(annotation);
  }

  /**
   * Notifies clients about the changed annotation.
   */
  public void updateFeatureStructure(Collection<? extends FeatureStructure> annotations) {
    fireUpdatedFeatureStructure(annotations);
  }

  public void changed() {
    fireChanged();
  }

  /**
   * Retrieves annotations of the given type from the {@link CAS}.
   */
  public Collection<AnnotationFS> getAnnotations(Type type) {
    FSIndex<AnnotationFS> annotationIndex = mCAS.getAnnotationIndex(type);

    StrictTypeConstraint typeConstrain = new StrictTypeConstraint(type);

    FSIterator<AnnotationFS> strictTypeIterator = mCAS
            .createFilteredIterator(annotationIndex.iterator(), typeConstrain);

    return fsIteratorToCollection(strictTypeIterator);
  }

  static Collection<AnnotationFS> fsIteratorToCollection(FSIterator<AnnotationFS> iterator) {
    LinkedList<AnnotationFS> annotations = new LinkedList<AnnotationFS>();
    while (iterator.hasNext()) {
      AnnotationFS annotation = (AnnotationFS) iterator.next();

      annotations.addFirst(annotation);
    }

    return annotations;
  }

  /**
   * Retrieves the given type from the {@link TypeSystem}.
   */
  public Type getType(String type) {
    return getCAS().getTypeSystem().getType(type);
  }

  public void switchView(String viewName) {
    String oldViewName = mCAS.getViewName();

    mCAS = mCAS.getView(viewName);

    fireViewChanged(oldViewName, viewName);
  }

  /**
   * Sets the content. The XCAS {@link InputStream} gets parsed.
   */
  private void setContent(IFile casFile) throws CoreException {

    IPreferenceStore store = CasEditorPlugin.getDefault().getPreferenceStore();
    boolean withPartialTypesystem = store
            .getBoolean(AnnotationEditorPreferenceConstants.ANNOTATION_EDITOR_PARTIAL_TYPESYSTEM);

    URI uri = casFile.getLocationURI();
    if (casFile.isLinked()) {
      uri = casFile.getRawLocationURI();
    }
    File file = EFS.getStore(uri).toLocalFile(0, new NullProgressMonitor());
    try {
      format = CasIOUtils.load(file.toURI().toURL(), null, mCAS, withPartialTypesystem);
    } catch (IOException e) {
      throwCoreException(e);
    }

  }

  private void throwCoreException(Exception e) throws CoreException {
    String message = e.getMessage() != null ? e.getMessage() : "";
    IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);
    throw new CoreException(s);
  }

  /**
   * Serializes the {@link CAS} to the given {@link OutputStream} in the XCAS format.
   */
  public void serialize(OutputStream out) throws CoreException {
    try {
      CasIOUtils.save(mCAS, out, format);
    } catch (IOException e) {
      throwCoreException(e);
    }
  }
  
  public static CAS getVirginCAS(IFile typeSystemFile) throws CoreException {
    ResourceSpecifierFactory resourceSpecifierFactory = UIMAFramework.getResourceSpecifierFactory();

    IFile extensionTypeSystemFile = typeSystemFile;

    InputStream inTypeSystem;

    if (extensionTypeSystemFile != null && extensionTypeSystemFile.exists()) {
      inTypeSystem = extensionTypeSystemFile.getContents();
    } else {
      return null;
    }

    XMLInputSource xmlTypeSystemSource = new XMLInputSource(inTypeSystem,
            extensionTypeSystemFile.getLocation().toFile());

    XMLParser xmlParser = UIMAFramework.getXMLParser();

    TypeSystemDescription typeSystemDesciptor;

    try {
      typeSystemDesciptor = (TypeSystemDescription) xmlParser.parse(xmlTypeSystemSource);

      IProject project = typeSystemFile.getProject();
      ClassLoader classLoader = getProjectClassLoader(project);
      
      ResourceManager resourceManager = null;
      if(classLoader != null) {
        resourceManager = new ResourceManager_impl(classLoader);
      } else {
        resourceManager = UIMAFramework.newDefaultResourceManager();
      }
      
      String dataPath = project
              .getPersistentProperty((new QualifiedName("", "CDEdataPath")));
      if (dataPath != null) {
        resourceManager.setDataPath(dataPath);
      }
      typeSystemDesciptor.resolveImports(resourceManager);
    } catch (InvalidXMLException e) {
      String message = e.getMessage() != null ? e.getMessage() : "";
      IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);
      throw new CoreException(s);
    } catch (MalformedURLException e) {
      String message = e.getMessage() != null ? e.getMessage() : "";
      IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);
      throw new CoreException(s);
    }

    TypePriorities typePriorities = resourceSpecifierFactory.createTypePriorities();

    FsIndexDescription indexDesciptor = new FsIndexDescription_impl();
    indexDesciptor.setLabel("TOPIndex");
    indexDesciptor.setTypeName("uima.cas.TOP");
    indexDesciptor.setKind(FsIndexDescription.KIND_SORTED);

    CAS cas;
    try {
      cas = CasCreationUtils.createCas(typeSystemDesciptor, typePriorities,
              new FsIndexDescription[] { indexDesciptor });
    } catch (ResourceInitializationException e) {
      String message = e.getMessage() != null ? e.getMessage() : "";
      IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);
      throw new CoreException(s);
    }

    return cas;
  }

  public static ClassLoader getProjectClassLoader(IProject project) throws CoreException {
    IProjectNature javaNature = project.getNature(JAVA_NATURE);
    if (javaNature != null) {
      JavaProject javaProject = (JavaProject) JavaCore.create(project);
      
      String[] runtimeClassPath = JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
      List<URL> urls = new ArrayList<>();
      for (int i = 0; i < runtimeClassPath.length; i++) {
        String cp = runtimeClassPath[i];
        try {
          urls.add(Paths.get(cp).toUri().toURL());
        } catch (MalformedURLException e) {
          CasEditorPlugin.log(e);
        }
      }
      return new URLClassLoader(urls.toArray(new URL[0]));
    } 
    return null;
  }

}
