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

import java.io.File;
import java.io.InputStream;

import org.apache.uima.ResourceSpecifierFactory;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.delta.INlpElementDelta;
import org.apache.uima.caseditor.core.util.MarkerUtil;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.FsIndexDescription_impl;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * TODO: add javadoc
 */
public class TypesystemElement extends AbstractNlpElement {
  
  private IFile mTypesytemFile;

  private NlpProject mProject;

  TypesystemElement(IFile resource, NlpProject parent) {
    mTypesytemFile = resource;
    mProject = parent;

    getCAS();
  }

  public TypeSystem getTypeSystem() {

    CAS cas = getCAS();

    if (cas != null) {
      return cas.getTypeSystem();
    }

    return null;
  }

  /**
   * Retrieves the {@link CAS}.
   * 
   * @return the {@link CAS} or null if there is an error in the type system.
   */
  public CAS getCAS() {
    Runnable clearMarkers = new Runnable() {
      public void run() {
        try {
          MarkerUtil.clearMarkers(mTypesytemFile, MarkerUtil.PROBLEM_MARKER);
        } catch (CoreException e) {
          CasEditorPlugin.log(e);
        }
      }
    };
    ((NlpModel) mProject.getParent()).asyncExcuteQueue(clearMarkers);

    try {
      return getCASInternal();
    } catch (final CoreException e) {
      Runnable createMarker = new Runnable() {
        public void run() {
          try {
            MarkerUtil.createMarker(mTypesytemFile, e.getMessage());
          } catch (CoreException e2) {
            CasEditorPlugin.log(e2);
          }
        }
      };
      ((NlpModel) mProject.getParent()).asyncExcuteQueue(createMarker);

      return null;
    }
  }

  private CAS getCASInternal() throws CoreException {
    ResourceSpecifierFactory resourceSpecifierFactory = UIMAFramework.getResourceSpecifierFactory();

    IFile extensionTypeSystemFile = mTypesytemFile;

    InputStream inTypeSystem;

    if (extensionTypeSystemFile != null && extensionTypeSystemFile.exists()) {
      inTypeSystem = extensionTypeSystemFile.getContents();
    } else {
      return null;
    }

    XMLInputSource xmlTypeSystemSource = new XMLInputSource(inTypeSystem, new File(""));

    XMLParser xmlParser = UIMAFramework.getXMLParser();

    TypeSystemDescription typeSystemDesciptor;

    try {
      typeSystemDesciptor = (TypeSystemDescription) xmlParser.parse(xmlTypeSystemSource);

      typeSystemDesciptor.resolveImports();
    } catch (InvalidXMLException e) {

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

  /**
   * Retrieves the name.
   */
  public String getName() {
    return "Typesystem";
  }

  /**
   * Retrieves the nlp project.
   */
  public NlpProject getNlpProject() {
    return mProject;
  }

  /**
   * Retrieves the parent.
   */
  public INlpElement getParent() {
    return mProject;
  }

  /**
   * Retrieves the resource.
   */
  public IResource getResource() {
    return mTypesytemFile;
  }

  @Override
  void changedResource(IResource resource, INlpElementDelta delta) throws CoreException {
    getCAS();
  }

  @Override
  void addResource(INlpElementDelta delta, IResource resource) throws CoreException {
    // not needed here, there are no child resources
  }

  @Override
  void removeResource(INlpElementDelta delta, IResource resource) throws CoreException {
    // not needed here, there are no child resources
  }
}
