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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.delta.AbstractResourceDelta;
import org.apache.uima.caseditor.core.model.delta.INlpElementDelta;
import org.apache.uima.caseditor.core.model.delta.INlpModelChangeListener;
import org.apache.uima.caseditor.core.model.delta.Kind;
import org.apache.uima.caseditor.core.model.delta.NlpModelDeltaImpl;
import org.apache.uima.caseditor.core.util.EventDispatcher;
import org.apache.uima.caseditor.core.util.IEventHandler;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * The root element of nlp workbench. Contains NLPProjects Contains IProject s
 */
public final class NlpModel extends AbstractNlpElement {
  private final class ResourceChangeListener implements IResourceChangeListener {
    EventDispatcher<Kind, NlpModelDeltaImpl> mEventDispatcher =
            new EventDispatcher<Kind, NlpModelDeltaImpl>(new DefaultEventHandler());

    ResourceChangeListener() {
      mEventDispatcher.register(Kind.ADDED, new AddHandler(mEventDispatcher));

      mEventDispatcher.register(Kind.REMOVED, new RemoveHandler(mEventDispatcher));

      mEventDispatcher.register(Kind.CHANGED, new ChangedHandler(mEventDispatcher));
    }

    /**
     * Process the resource changes.
     */
    public void resourceChanged(IResourceChangeEvent event) {
      // process only post changes
      // and filter IWorkspaceRoot change events
      if (event.getType() != IResourceChangeEvent.POST_CHANGE
              || event.getResource() instanceof IWorkspaceRoot) {
        return;
      }

      final IResourceDelta rootDelta = event.getDelta();

      NlpModelDeltaImpl modelDelta = new NlpModelDeltaImpl(null, rootDelta);

      mEventDispatcher.notify(modelDelta.getKind(), modelDelta);

      for (NlpProject project : mNlpProjects) {
        try {
          project.postProcessResourceChanges();
        } catch (CoreException e) {
          handleExceptionDuringResoruceChange(e);
        }
      }

      for (INlpModelChangeListener modelListener : mModelChangeListeners) {
        modelListener.resourceChanged(modelDelta);
      }
    }
  }

  private static abstract class AbstractEventHandler implements IEventHandler<NlpModelDeltaImpl> {
    protected EventDispatcher<Kind, NlpModelDeltaImpl> mEventDispatcher;

    AbstractEventHandler(EventDispatcher<Kind, NlpModelDeltaImpl> handler) {
      mEventDispatcher = handler;
    }

    protected void processChildEvents(INlpElementDelta delta) {
      INlpElementDelta childDeltas[] = delta.getAffectedChildren();

      for (INlpElementDelta childDelta : childDeltas) {
        mEventDispatcher.notify(childDelta.getKind(), (NlpModelDeltaImpl) childDelta);
      }
    }
  }

  /**
   * The default handler is only called if an unknown event is handled. The event will be logged.
   */
  private static final class DefaultEventHandler implements IEventHandler<NlpModelDeltaImpl> {
    /**
     * Executes the current action.
     */
    public void handle(NlpModelDeltaImpl delta) {
      // TODO: implement this default event handler
      // unhandled events should be logged for debugging ...
    }
  }

  /**
   * Handles add events.
   */
  private static final class AddHandler extends AbstractEventHandler {
    protected AddHandler(EventDispatcher<Kind, NlpModelDeltaImpl> handler) {
      super(handler);
    }

    /**
     * Executes the current action.
     */
    public void handle(NlpModelDeltaImpl delta) {
      CasEditorPlugin.getNlpModel().addInternal(delta, delta.getResource());

      /*
       * Note: If a project is added, it is closed (isOpen() == false). Tough, the .project file is
       * then added to the closed project.
       */

      // set nlp element here
      delta.setNlpElement(CasEditorPlugin.getNlpModel().findMember(delta.getResource()));

      processChildEvents(delta);
    }
  }

  /**
   * Handles remove events..
   */
  private static final class RemoveHandler extends AbstractEventHandler {
    RemoveHandler(EventDispatcher<Kind, NlpModelDeltaImpl> handler) {
      super(handler);
    }

    /**
     * Executes the current action.
     */
    public void handle(NlpModelDeltaImpl delta) {
      // set nlp element here
      delta.setNlpElement(CasEditorPlugin.getNlpModel().findMember(delta.getResource()));

      processChildEvents(delta);

      if (delta.isNlpElement()) {
        INlpElement parent = delta.getNlpElement().getParent();

        if (parent != null) {
          CasEditorPlugin.getNlpModel().removeInternal(delta, delta.getResource());
        }
      }
    }
  }

  /**
   * Handles change events.
   */
  private static final class ChangedHandler extends AbstractEventHandler {
    ChangedHandler(EventDispatcher<Kind, NlpModelDeltaImpl> handler) {
      super(handler);
    }

    /**
     * Executes the current action.
     */
    public void handle(final NlpModelDeltaImpl delta) {
      if (delta.getResource() instanceof IProject) {
        final IResourceDelta resourceDelta = delta.getResourceDelta();

        final IProject project = (IProject) delta.getResource();

        if ((resourceDelta.getFlags() & IResourceDelta.OPEN) != 0) {
          if (project.isOpen()) {
            // add the nlp project
            delta.setKind(Kind.ADDED);
            mEventDispatcher.notify(delta.getKind(), delta);

            // remove the old resource project
            IResourceDelta removeProjectResourceDelta = new AbstractResourceDelta() {
              @Override
              public IResource getResource() {
                return project;
              }

              @Override
              public int getKind() {
                return IResourceDelta.REMOVED;
              }

              @Override
              public IPath getFullPath() {
                return resourceDelta.getFullPath();
              }

              @Override
              public IPath getProjectRelativePath() {
                return resourceDelta.getProjectRelativePath();
              }

              @Override
              public IResourceDelta[] getAffectedChildren() {
                return new IResourceDelta[] {};
              }
            };

            NlpModelDeltaImpl removeProjectDelta =
                    new NlpModelDeltaImpl((NlpModelDeltaImpl) delta.getParent(),
                    removeProjectResourceDelta);

            NlpModelDeltaImpl parent = (NlpModelDeltaImpl) delta.getParent();

            parent.addChild(removeProjectDelta);

            return;
          } else {
            // remove the nlp project
            delta.setKind(Kind.REMOVED);
            mEventDispatcher.notify(delta.getKind(), delta);

            // add the closed project resource
            IResourceDelta removeProjectResourceDelta = new AbstractResourceDelta() {
              @Override
              public IResource getResource() {
                return project;
              }

              @Override
              public int getKind() {
                return IResourceDelta.ADDED;
              }

              @Override
              public IPath getFullPath() {
                return resourceDelta.getFullPath();
              }

              @Override
              public IPath getProjectRelativePath() {
                return resourceDelta.getProjectRelativePath();
              }

              @Override
              public IResourceDelta[] getAffectedChildren() {
                return new IResourceDelta[] {};
              }
            };

            NlpModelDeltaImpl removeProjectDelta =
                    new NlpModelDeltaImpl((NlpModelDeltaImpl) delta.getParent(),
                    removeProjectResourceDelta);

            NlpModelDeltaImpl parent = (NlpModelDeltaImpl) delta.getParent();

            parent.addChild(removeProjectDelta);
            return;
          }
        }
        // indicates that a nature was added to the project
        else if ((resourceDelta.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
          if (project.isOpen()) {
            IResourceDelta removeProjectResourceDelta = new AbstractResourceDelta() {
              @Override
              public IResource getResource() {
                return project;
              }

              @Override
              public int getKind() {
                return IResourceDelta.REMOVED;
              }

              @Override
              public IPath getFullPath() {
                return resourceDelta.getFullPath();
              }

              @Override
              public IPath getProjectRelativePath() {
                return resourceDelta.getProjectRelativePath();
              }

              @Override
              public IResourceDelta[] getAffectedChildren() {
                return new IResourceDelta[] {};
              }
            };

            NlpModelDeltaImpl removeProjectDelta =
                    new NlpModelDeltaImpl((NlpModelDeltaImpl) delta.getParent(),
                    removeProjectResourceDelta);

            NlpModelDeltaImpl parent = (NlpModelDeltaImpl) delta.getParent();

            parent.addChild(removeProjectDelta);

            // change this event to add event type and reprocess it
            delta.setKind(Kind.ADDED);
            mEventDispatcher.notify(delta.getKind(), delta);

          }
        }
      }

      // set nlp element here
      delta.setNlpElement(CasEditorPlugin.getNlpModel().findMember(delta.getResource()));

      processChildEvents(delta);

      INlpElement parent;
      try {
        parent = CasEditorPlugin.getNlpModel().getParent(delta.getResource());

        if (parent != null) {
          CasEditorPlugin.getNlpModel().changeInternal(delta);
        }
      } catch (CoreException e) {
        CasEditorPlugin.log(e);
      }
    }
  }

  private Set<INlpModelChangeListener> mModelChangeListeners =
          new HashSet<INlpModelChangeListener>();

  private IWorkspaceRoot mWorkspaceRoot;

  private List<NlpProject> mNlpProjects;

  private ExecutorService mAsynchronExecutor;

  private ResourceChangeListener mListener;

  /**
   * Initializes a new instance.
   * 
   * @throws CoreException
   */
  public NlpModel() throws CoreException {
    mAsynchronExecutor = Executors.newSingleThreadExecutor();

    mWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

    mNlpProjects = new LinkedList<NlpProject>();

    createNlpProjects();

    createModelSynchronizer();
  }

  private void handleExceptionDuringResoruceChange(CoreException e) {
    CasEditorPlugin.log(e);
  }

  void changeInternal(INlpElementDelta delta) {
    IResource resource = delta.getResource();

    IResource parent = resource.getParent();

    AbstractNlpElement parentElement = (AbstractNlpElement) findMember(parent);

    if (parentElement != null) {
      try {
        parentElement.changedResource(resource, delta);
      } catch (CoreException e) {
        handleExceptionDuringResoruceChange(e);
      }
    }
  }

  void addInternal(INlpElementDelta delta, IResource resource) {
    // TODO: why is this possible ?
    if (resource.equals(mWorkspaceRoot)) {
      return;
    }

    IResource parent = resource.getParent();

    AbstractNlpElement parentElement = (AbstractNlpElement) findMember(parent);

    if (parentElement != null) {
      try {
        parentElement.addResource(delta, resource);
      } catch (CoreException e) {
        handleExceptionDuringResoruceChange(e);
      }
    }
  }

  @Override
  void addResource(INlpElementDelta delta, IResource resource) throws CoreException {
    if (resource instanceof IProject) {
      IProject project = (IProject) resource;

      createNlpProject(project);

    }
  }

  void removeInternal(INlpElementDelta delta, IResource resource) {
    IResource parent = resource.getParent();

    AbstractNlpElement parentElement = (AbstractNlpElement) findMember(parent);

    if (parentElement != null) {
      try {
        parentElement.removeResource(delta, resource);
      } catch (CoreException e) {
        handleExceptionDuringResoruceChange(e);
      }
    }
  }

  @Override
  void removeResource(INlpElementDelta delta, IResource resource) {
    for (NlpProject project : mNlpProjects) {
      if (project.getResource().equals(resource)) {
        mNlpProjects.remove(project);
        break;
      }
    }
  }

  /**
   * Retrieves all nlp projects.
   * 
   * @return the nlp projects
   */
  public NlpProject[] getNlpProjects() {
    return mNlpProjects.toArray(new NlpProject[mNlpProjects.size()]);
  }

  private void createNlpProject(IProject project) throws CoreException {
    if (project.isOpen()) {
      if (project.hasNature(NlpProject.ID)) {
        NlpProject nlpProject = (NlpProject) project.getNature(NlpProject.ID);

        nlpProject.setNlpModel(this);

        nlpProject.initialize();

        mNlpProjects.add(nlpProject);
      }
    }
  }

  private void createNlpProjects() throws CoreException {
    IProject[] projects = mWorkspaceRoot.getProjects();

    for (IProject project : projects) {
      createNlpProject(project);
    }
  }

  /**
   * Retrieves the resource projects in the workspace without a nlp nature.
   * 
   * @return array of projects
   * @throws CoreException
   */
  public IProject[] getNonNlpProjects() throws CoreException {
    IProject[] allProjects = mWorkspaceRoot.getProjects();

    LinkedList<IProject> otherProjectList = new LinkedList<IProject>();

    for (int i = 0; i < allProjects.length; i++) {
      if (allProjects[i].isOpen()) {
        if (!allProjects[i].hasNature(NlpProject.ID)) {
          otherProjectList.add(allProjects[i]);
        }
      } else {
        otherProjectList.add(allProjects[i]);
      }
    }

    IProject[] otherProjects = new IProject[otherProjectList.size()];

    return otherProjectList.toArray(otherProjects);
  }

  /**
   * Retrieves the resource.
   */
  public IResource getResource() {
    return mWorkspaceRoot;
  }

  /**
   * Retrieves the name.
   */
  public String getName() {
    return mWorkspaceRoot.getName();
  }

  /**
   * Retrieves the parent of the given resource.
   */
  @Override
  public INlpElement getParent(IResource resource) throws CoreException {
    INlpElement result = super.getParent(resource);

    if (result == null) {
      for (NlpProject project : getNlpProjects()) {
        result = project.getParent(resource);

        if (result != null) {
          return result;
        }
      }

      for (IProject project : getNonNlpProjects()) {
        if (project.equals(resource)) {
          return this;
        }
      }
    }

    return result;
  }

  /**
   * Search the {@link INlpElement} for the given resource.
   */
  @Override
  public INlpElement findMember(IResource resource) {
    INlpElement result = super.findMember(resource);

    if (result == null) {
      for (NlpProject project : getNlpProjects()) {
        boolean isNlpElement = project.findMember(resource) != null;

        if (isNlpElement) {
          return project.findMember(resource);
        }
      }
    }

    return result;
  }

  /**
   * Checks if the given element is a {@link INlpElement}.
   * 
   * @param resource
   * @return true if it is a {@link INlpElement} otherwise false.
   */
  public boolean isNlpElement(IResource resource) {
    return findMember(resource) != null;
  }

  /**
   * The {@link NlpModel} does not have a parent.
   * 
   * @return null
   */
  public INlpElement getParent() {
    return null;
  }

  /**
   * Always returns null.
   */
  public NlpProject getNlpProject() {
    return null;
  }

  /**
   * Registers a model change listener.
   * 
   * @param listener
   */
  public void addNlpModelChangeListener(INlpModelChangeListener listener) {
    mModelChangeListeners.add(listener);
  }

  /**
   * Removes a model change listener.
   * 
   * @param listener
   */
  public void removeNlpModelChangeListener(INlpModelChangeListener listener) {
    mModelChangeListeners.remove(listener);
  }

  void createModelSynchronizer() {
    mListener = new ResourceChangeListener();

    ResourcesPlugin.getWorkspace().addResourceChangeListener(mListener);
  }

  /**
   * Represent this object as a human readable string.
   */
  @Override
  public String toString() {
    return getResource().toString();
  }

  void fireRefreshEvent(INlpElement element) {
    for (INlpModelChangeListener listener : mModelChangeListeners) {
      listener.refresh(element);
    }
  }

  /**
   * Asynchrony executes the workers on the queue. This method can be used to post resource tree
   * changes inside the event handler, where the resource tree is locked.
   * 
   * @param worker
   */
  public void asyncExcuteQueue(Runnable worker) {
    mAsynchronExecutor.submit(worker);
  }

  /**
   * Destroy the current instance, for testing only.
   */
  public void destroyForTesting() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(mListener);
  }
}
