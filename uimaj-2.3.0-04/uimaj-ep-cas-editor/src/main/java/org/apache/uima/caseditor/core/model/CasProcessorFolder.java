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
import org.apache.uima.caseditor.core.uima.AnnotatorConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;

/**
 * The UimaSourceFolder contains folders, each of these folders can contain UIMA consumer or
 * annotator configurations.
 */
public class CasProcessorFolder extends AbstractNlpElement implements IAdaptable {
  
  private static final String CONSUMER_DESCRIPTOR_ID = "org.apache.uima.caseditor.CasConsumerDescriptor";
  private static final String ANALYSIS_ENGINE_DESCRIPTOR_ID = "org.apache.uima.caseditor.AnalysisEngineDescriptor";
  
  private IFolder mConfigFolder;

  private NlpProject mProject;

  private Collection<AnnotatorElement> mAnnotators;

  private Collection<ConsumerElement> mConsumers;

  /**
   * Initializes a new instance.
   * 
   * @param configFolder
   * @param sourceFolder
   * @param project
   * @throws CoreException
   */
  CasProcessorFolder(IFolder configFolder, NlpProject project) throws CoreException {
    mConfigFolder = configFolder;
    mProject = project;
    createAnnotatorConfigurations();
    createConsumerConfigurations();
  }

  /**
   * Retrieves the {@link AnnotatorConfiguration}.
   * 
   * @return the {@link AnnotatorConfiguration}
   */
  public Collection<AnnotatorElement> getAnnotators() {
    return mAnnotators;
  }

  private boolean isConsumerDescriptorFile(IResource resource) throws CoreException {
    
    boolean isConsumerDescritporFile = false;
    
    if (resource instanceof IFile) {
      IContentDescription contentDescription = ((IFile) resource).getContentDescription();
     
      if (contentDescription != null) {
        IContentType contentType = contentDescription.getContentType();
        
        isConsumerDescritporFile = 
                contentType != null && CONSUMER_DESCRIPTOR_ID.equals(contentType.getId());
      }
    }
    
    return isConsumerDescritporFile;
  }

  private boolean isAnalysisEngineDescriptorFile(IResource resource) throws CoreException {
    
    boolean isAnalysisEngineDescriptorFile = false;
    
    if (resource instanceof IFile) {
      IContentDescription contentDescription = ((IFile) resource).getContentDescription();
      
      if (contentDescription != null) {
        IContentType contentType = contentDescription.getContentType();
        
        isAnalysisEngineDescriptorFile = 
                contentType != null && ANALYSIS_ENGINE_DESCRIPTOR_ID.equals(contentType.getId());

      }
    }
    
    return isAnalysisEngineDescriptorFile;
  }  
  
  private void createAnnotatorConfigurations() throws CoreException {
    mAnnotators = new LinkedList<AnnotatorElement>();

    for (IResource resource : mConfigFolder.members()) {
      if (isAnalysisEngineDescriptorFile(resource)) {
        AnnotatorElement annotator = new AnnotatorElement(this, (IFile) resource);
        mAnnotators.add(annotator);
      }
    }
  }

  /**
   * Retrieves the consumers.
   * 
   * @return consumers
   */
  public Collection<ConsumerElement> getConsumers() {
    return mConsumers;
  }

  private void createConsumerConfigurations() throws CoreException {
    mConsumers = new LinkedList<ConsumerElement>();

    for (IResource resource : mConfigFolder.members()) {
      if (isConsumerDescriptorFile(resource)) {
        IFile consumerFile = (IFile) resource;

        ConsumerElement consumer = new ConsumerElement(this, consumerFile);
        mConsumers.add(consumer);
      }
    }
  }

  /**
   * Retrieves all contained {@link IFile} and {@link IFolder} resources.
   * 
   * @return {@link IFile}s and {@link IFolder}s
   * @throws CoreException
   */
  public Collection<IResource> getNonNlpResources() throws CoreException {
    
    Collection<IResource> resources = new LinkedList<IResource>();

    for (IResource candidate : mConfigFolder.members()) {
      if (isConsumerDescriptorFile(candidate) || isAnalysisEngineDescriptorFile(candidate)) {
        continue;
      }

      resources.add(candidate);
    }

    return resources;
  }

  /**
   * Retrieves the {@link NlpProject}.
   * 
   * @return the {@link NlpProject}
   */
  public NlpProject getNlpProject() {
    return mProject;
  }

  /**
   * Searches for members of the given resource.
   */
  @Override
  public INlpElement findMember(IResource resource) {
    if (getResource().equals(resource)) {
      return this;
    }

    Collection<ConsumerElement> consumers = getConsumers();

    for (ConsumerElement consumer : consumers) {
      boolean isElementFound = consumer.findMember(resource) != null;

      if (isElementFound) {
        return consumer.findMember(resource);
      }
    }

    Collection<AnnotatorElement> annotators = getAnnotators();

    for (AnnotatorElement annotator : annotators) {
      boolean isElementFound = annotator.findMember(resource) != null;

      if (isElementFound) {
        return annotator.findMember(resource);
      }
    }

    return null;
  }

  /**
   * Retrieves the parent.
   * 
   * @return the parent
   */
  public INlpElement getParent() {
    return mProject;
  }

  /**
   * Retrieves the resource.
   */
  public IResource getResource() {
    return mConfigFolder;
  }

  /**
   * Retrieves the parent of the given resource.
   */
  @Override
  public INlpElement getParent(IResource resource) throws CoreException {
    INlpElement result = super.getParent(resource);

    for (IResource member : mConfigFolder.members()) {
      if (member.equals(resource)) {
        result = this;
      }
    }

    return result;
  }

  /**
   * Retrieves the name.
   */
  public String getName() {
    return mConfigFolder.getName();
  }

  /**
   * Adds a consumer or analysis engine descriptor to the CAS processor folder.
   */
  @Override
  void addResource(INlpElementDelta delta, IResource resource) throws CoreException {

    if (isConsumerDescriptorFile(resource)) {
      mConsumers.add(new ConsumerElement(this, (IFile) resource));
    } else if (isAnalysisEngineDescriptorFile(resource)) {
      mAnnotators.add(new AnnotatorElement(this, (IFile) resource));
    }
  }

  @Override
  void changedResource(IResource resource, INlpElementDelta delta) throws CoreException {
    if (isConsumerDescriptorFile(resource)) {
      for (ConsumerElement consumer : mConsumers) {
        if (consumer.getResource().equals(resource)) {
          consumer.changedResource(resource, delta);
          break;
        }
      }
    } else if (isAnalysisEngineDescriptorFile(resource)) {
      for (AnnotatorElement annotator : mAnnotators) {
        if (annotator.getResource().equals(resource)) {
          annotator.changedResource(resource, delta);
          break;
        }
      }
    }
  }

  /**
   * Not implemented.
   */
  @Override
  void removeResource(INlpElementDelta delta, IResource resource) throws CoreException {
    if (isConsumerDescriptorFile(resource)) {
      for (ConsumerElement consumer : mConsumers) {
        if (consumer.getResource().equals(resource)) {
          mConsumers.remove(consumer);
          break;
        }
      }
    } else if (isAnalysisEngineDescriptorFile(resource)) {
      for (AnnotatorElement annotator : mAnnotators) {
        if (annotator.getResource().equals(resource)) {
          mAnnotators.remove(annotator);
          break;
        }
      }
    }
  }
}
