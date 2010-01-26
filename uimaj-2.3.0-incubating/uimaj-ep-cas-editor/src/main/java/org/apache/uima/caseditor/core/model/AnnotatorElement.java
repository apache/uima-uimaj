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

import org.apache.uima.UIMAFramework;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.delta.INlpElementDelta;
import org.apache.uima.caseditor.core.uima.AnnotatorConfiguration;
import org.apache.uima.caseditor.core.util.MarkerUtil;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * TODO: add javadoc here
 */
public class AnnotatorElement extends AbstractNlpElement {
  private CasProcessorFolder mParent;

  private IFile mAnnotatorResource;

  private AnnotatorConfiguration mAnnotatorConfig;

  /**
   * Initializes the current instance.
   * 
   * @param config
   * @param annotatorFile
   */
  AnnotatorElement(CasProcessorFolder config, IFile annotatorFile) {
    mParent = config;
    mAnnotatorResource = annotatorFile;

    mAnnotatorConfig = createAnnotatorConfiguration();
  }

  /**
   * Retrieves the {@link AnnotatorConfiguration}.
   * 
   * @return the {@link AnnotatorConfiguration}
   */
  public AnnotatorConfiguration getAnnotatorConfiguration() {
    return mAnnotatorConfig;
  }

  private AnnotatorConfiguration createAnnotatorConfiguration() {

    Runnable clearMarkers = new Runnable() {
      public void run() {
        try {
          MarkerUtil.clearMarkers(mAnnotatorResource, MarkerUtil.PROBLEM_MARKER);
        } catch (CoreException e) {
          CasEditorPlugin.log(e);
        }
      }
    };
    ((NlpModel) getNlpProject().getParent()).asyncExcuteQueue(clearMarkers);

    XMLInputSource inAnnotator;
    try {
      String dataPath = ((IFolder) mParent.getResource()).getLocation().toOSString();
      inAnnotator = new XMLInputSource(mAnnotatorResource.getContents(), new File(dataPath));
    } catch (final CoreException e2) {
      Runnable createMarker = new Runnable() {
        public void run() {
          try {
            MarkerUtil.clearMarkers(mAnnotatorResource, e2.getMessage());
          } catch (CoreException e) {
            CasEditorPlugin.log(e);
          }
        }
      };
      ((NlpModel) getNlpProject().getParent()).asyncExcuteQueue(createMarker);

      return null;
    }

    ResourceSpecifier specifier;
    try {
      specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(inAnnotator);
    } catch (final InvalidXMLException e) {
      Runnable createMarker = new Runnable() {
        public void run() {
          try {
            MarkerUtil.createMarker(mAnnotatorResource, e.getMessage());
          } catch (CoreException e2) {
            CasEditorPlugin.log(e2);
          }
        }
      };
      ((NlpModel) getNlpProject().getParent()).asyncExcuteQueue(createMarker);

      return null;
    }

    // TODO: refactor here
    AnnotatorConfiguration annotatorConfiguration = new AnnotatorConfiguration(this, specifier);

    // TODO: cast is unsafe !!!
    annotatorConfiguration.setBaseFolder((IFolder) mParent.getResource());

    return annotatorConfiguration;
  }

  // private void createMarker()

  @Override
  void addResource(INlpElementDelta delta, IResource resource) {
    // just do nothing, no children
  }

  @Override
  void changedResource(IResource resource, INlpElementDelta delta) throws CoreException {
    mAnnotatorConfig = createAnnotatorConfiguration();
  }

  @Override
  void removeResource(INlpElementDelta delta, IResource resource) {
    // just do nothing, no children
  }

  /**
   * Retrieves the name.
   */
  public String getName() {
    return getResource().getName();
  }

  /**
   * Retrieves the nlp projects.
   */
  public NlpProject getNlpProject() {
    return getParent().getNlpProject();
  }

  /**
   * Retrieves the parent.
   */
  public INlpElement getParent() {
    return mParent;
  }

  /**
   * Retrieves the underlying resource.
   */
  public IResource getResource() {
    return mAnnotatorResource;
  }
}
