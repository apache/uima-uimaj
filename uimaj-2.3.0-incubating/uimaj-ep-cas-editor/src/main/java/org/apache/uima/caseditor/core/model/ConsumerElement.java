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
import org.apache.uima.caseditor.core.uima.CasConsumerConfiguration;
import org.apache.uima.caseditor.core.util.MarkerUtil;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * The ConsumerELement contains the uima consumer descriptor.
 */
public class ConsumerElement extends AbstractNlpElement {
  private CasProcessorFolder mParent;

  private IFile mConsumerResource;

  private CasConsumerConfiguration mConsumerConfiguration;

  ConsumerElement(CasProcessorFolder config, IFile consumer) throws CoreException {
    mParent = config;
    mConsumerResource = consumer;
    mConsumerConfiguration = createConsumerConfiguration();
  }

  /**
   * Retrieves the {@link CasConsumerConfiguration}.
   * 
   * @return the configuration
   */
  public CasConsumerConfiguration getConsumerConfiguration() {
    return mConsumerConfiguration;
  }

  /**
   * @return the configuration
   * 
   * @throws CoreException
   */
  private CasConsumerConfiguration createConsumerConfiguration() throws CoreException {
    Runnable clearMarkers = new Runnable() {
      public void run() {
        try {
          MarkerUtil.clearMarkers(mConsumerResource, MarkerUtil.PROBLEM_MARKER);
        } catch (CoreException e) {
          CasEditorPlugin.log(e);
        }
      }
    };
    ((NlpModel) getNlpProject().getParent()).asyncExcuteQueue(clearMarkers);

    String dataPath = ((IFolder) mParent.getResource()).getLocation().toOSString();
    XMLInputSource inCasConsumer =
            new XMLInputSource(mConsumerResource.getContents(), new File(dataPath));

    XMLParser xmlParser = UIMAFramework.getXMLParser();
    CasConsumerDescription casConsumerDesciptor;

    try {
      // TODO: this throws a class cast exception if the file has an other descriptor, check it
      casConsumerDesciptor = (CasConsumerDescription) xmlParser.parse(inCasConsumer);
    } catch (final InvalidXMLException e) {
      Runnable createMarker = new Runnable() {
        public void run() {
          try {
            MarkerUtil.createMarker(mConsumerResource, e);
          } catch (CoreException e2) {
            CasEditorPlugin.log(e2);
          }
        }
      };
      ((NlpModel) getNlpProject().getParent()).asyncExcuteQueue(createMarker);

      return null;
    } catch (ClassCastException e) {
      Runnable createMarker = new Runnable() {
        public void run() {
          try {
            MarkerUtil.createMarker(mConsumerResource, "This file must contain a cas consumer!");
          } catch (CoreException e2) {
            CasEditorPlugin.log(e2);
          }
        }
      };
      ((NlpModel) getNlpProject().getParent()).asyncExcuteQueue(createMarker);

      return null;
    }

    CasConsumerConfiguration trainerConfiguration =
            new CasConsumerConfiguration(this, casConsumerDesciptor);

    trainerConfiguration.setBaseFolder((IFolder) getParent().getResource());

    return trainerConfiguration;
  }

  @Override
  void addResource(INlpElementDelta delta, IResource resource) {
    // just do nothing, no children
  }

  @Override
  void changedResource(IResource resource, INlpElementDelta delta) throws CoreException {
    mConsumerConfiguration = createConsumerConfiguration();
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
   * Retrieves the parent {@link NlpProject} instance.
   */
  public NlpProject getNlpProject() {
    return getParent().getNlpProject();
  }

  /**
   * Retrieves the direct parent.
   */
  public INlpElement getParent() {
    return mParent;
  }

  /**
   * Retrieves the {@link IResource} object belonging to the current instance.
   */
  public IResource getResource() {
    return mConsumerResource;
  }
}
