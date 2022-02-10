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

package org.apache.uima.taeconfigurator.editors.ui.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.swt.widgets.Display;

/**
 * The Class SearchThread.
 */
public class SearchThread implements Runnable /* extends Thread */ {

  /** The m file name search. */
  private Pattern m_fileNameSearch;

  /** The m input type search. */
  private Pattern m_inputTypeSearch;

  /** The m output type search. */
  private Pattern m_outputTypeSearch;

  /** The m project to search. */
  private String m_projectToSearch;

  /** The m dialog. */
  FindComponentDialog m_dialog;

  /** The m aggregate section. */
  private AbstractSection m_aggregateSection;

  /** The m matching delegate component descriptors. */
  private List m_matchingDelegateComponentDescriptors;

  /** The m matching delegate component descriptions. */
  private List m_matchingDelegateComponentDescriptions;

  /** The m n which status msg. */
  int m_nWhichStatusMsg;

  /** The m status msg. */
  String m_statusMsg;

  /** The m component headers. */
  private String[] m_componentHeaders;

  /**
   * Instantiates a new search thread.
   *
   * @param dialog
   *          the dialog
   * @param aggregateSection
   *          the aggregate section
   * @param fileNameSearch
   *          the file name search
   * @param inputTypeSearch
   *          the input type search
   * @param outputTypeSearch
   *          the output type search
   * @param projectToSearch
   *          the project to search
   * @param componentHeaders
   *          the component headers
   */
  public SearchThread(FindComponentDialog dialog, AbstractSection aggregateSection,
          String fileNameSearch, String inputTypeSearch, String outputTypeSearch,
          String projectToSearch, String[] componentHeaders) {

    m_dialog = dialog;
    m_aggregateSection = aggregateSection;
    m_fileNameSearch = (null == fileNameSearch) ? null : Pattern.compile(fileNameSearch);
    m_inputTypeSearch = (null == inputTypeSearch) ? null : Pattern.compile(inputTypeSearch);
    m_outputTypeSearch = (null == outputTypeSearch) ? null : Pattern.compile(outputTypeSearch);
    m_projectToSearch = projectToSearch;
    m_componentHeaders = componentHeaders;
  }

  /** The m b die now. */
  private boolean m_bDieNow = false;

  /** The m b done. */
  private boolean m_bDone = false;

  /**
   * Sets the die now.
   */
  public void setDieNow() {
    m_bDieNow = true;
  }

  /**
   * Gets the die now.
   *
   * @return the die now
   */
  public boolean getDieNow() {
    return m_bDieNow;
  }

  /**
   * Checks if is done.
   *
   * @return true, if is done
   */
  public boolean isDone() {
    return m_bDone;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    m_matchingDelegateComponentDescriptors = new ArrayList();
    m_matchingDelegateComponentDescriptions = new ArrayList();

    getDelegateComponentsByInputOutputTypes(m_projectToSearch);

    m_bDone = true;
  }

  /**
   * Test one resource.
   *
   * @param resource
   *          the resource
   */
  private void testOneResource(IResource resource) {
    switch (resource.getType()) {
      case IResource.FILE:
        if (resource.getName().toLowerCase().endsWith(".xml")
                // exclude potentially many data files, not descriptors
                && !resource.getName().toLowerCase().endsWith(".txt.xml")
                && (m_fileNameSearch == null
                        || m_fileNameSearch.matcher(resource.getName()).find())) {
          String fileDescriptorRelPath = m_aggregateSection.editor
                  .getDescriptorRelativePath(resource.getLocation().toString());
          setStatusMsg(2, "Examining " + getBriefDisplayVersion(fileDescriptorRelPath));
          ResourceSpecifier rs = MultiPageEditor.getDelegateResourceSpecifier((IFile) resource,
                  m_componentHeaders);
          // rs == null if wrong kind of descriptor
          if (null == rs)
            return;
          if (!(rs instanceof ResourceCreationSpecifier)) // is a remote descriptor
            if (m_inputTypeSearch != null || m_outputTypeSearch != null)
              return; // don't find remote descriptors when types are wanted

          if (!(rs instanceof ResourceCreationSpecifier) || // is a remote descriptor
                  delegateComponentMatchesCapabilityReqs((ResourceCreationSpecifier) rs,
                          m_inputTypeSearch, m_outputTypeSearch)) {
            m_matchingDelegateComponentDescriptors.add(fileDescriptorRelPath);
            m_matchingDelegateComponentDescriptions.add(rs);
          }
        }
        break;
      case IResource.FOLDER:
        getDelegateComponentsByIOTypesBeginningAt((IFolder) resource);
        break;
    }

  }

  // populates the Vector of matchingAnalysisEngineDescriptors and
  /**
   * Gets the delegate components by input output types.
   *
   * @param projectToSearch
   *          the project to search
   * @return the delegate components by input output types
   */
  // matchingAnalysisEngineDescriptions
  private void getDelegateComponentsByInputOutputTypes(String projectToSearch) {

    IWorkspace workspace = TAEConfiguratorPlugin.getWorkspace();
    IProject[] projects = workspace.getRoot().getProjects();
    if (projectToSearch.equals(FindComponentDialog.ALL_PROJECTS)) {
      setStatusMsg(1, "0 of " + projects.length + " projects processed.");
    }

    for (int i = 0; i < projects.length; i++) {
      try {
        if (projectToSearch.equals(FindComponentDialog.ALL_PROJECTS)
                || projects[i].getName().equals(projectToSearch)) {

          if (projectToSearch.equals(FindComponentDialog.ALL_PROJECTS)) {
            setStatusMsg(2, "Looking in " + projects[i].getName() + "....");
          } else {
            setStatusMsg(2, "Searching " + projects[i].getName() + " for matching TAEs...");
          }

          IResource[] projectContents = projects[i].members();
          for (int j = 0; j < projectContents.length; j++) {
            testOneResource(projectContents[j]);
          }

          if (projectToSearch.equals(FindComponentDialog.ALL_PROJECTS)) {
            setStatusMsg(1, (i + 1) + " of " + projects.length + " projects processed.");
          }
        }
      } catch (Exception ex) {
        System.out.println(ex.getMessage());
      }
    }
  }

  // populates the Vector of matchingAnalysisEngineDescriptors and
  /**
   * Gets the delegate components by IO types beginning at.
   *
   * @param beginFolder
   *          the begin folder
   * @return the delegate components by IO types beginning at
   */
  // matchingAnalysisEngineDescriptions
  private void getDelegateComponentsByIOTypesBeginningAt(IFolder beginFolder) {

    if (m_bDieNow) {
      return;
    }

    try {
      for (int i = 0; i < beginFolder.members().length; i++) {
        testOneResource(beginFolder.members()[i]);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Delegate component matches capability reqs.
   *
   * @param rs
   *          the rs
   * @param inputTypeSearch
   *          the input type search
   * @param outputTypeSearch
   *          the output type search
   * @return true, if successful
   */
  private boolean delegateComponentMatchesCapabilityReqs(ResourceCreationSpecifier rs,
          Pattern inputTypeSearch, Pattern outputTypeSearch) {

    if (inputTypeSearch == null && outputTypeSearch == null) {
      return true;
    }

    Capability[] capabilities = AbstractSection.getCapabilities(rs);
    if (capabilities == null || capabilities.length == 0) {
      return false;
    }

    boolean inputSatisfied = matchCapabilitiesTo(capabilities, inputTypeSearch, INPUT);
    boolean outputSatisfied = matchCapabilitiesTo(capabilities, outputTypeSearch, OUTPUT);
    return inputSatisfied && outputSatisfied;
  }

  /** The Constant INPUT. */
  private static final boolean INPUT = true;

  /** The Constant OUTPUT. */
  private static final boolean OUTPUT = false;

  /**
   * Match capabilities to.
   *
   * @param capabilities
   *          the capabilities
   * @param search
   *          the search
   * @param isInput
   *          the is input
   * @return true, if successful
   */
  private boolean matchCapabilitiesTo(Capability[] capabilities, Pattern search, boolean isInput) {
    if (null == search)
      return true;
    for (int i = 0; i < capabilities.length; i++) {
      TypeOrFeature[] typeOrFeatures = isInput ? capabilities[i].getInputs()
              : capabilities[i].getOutputs();
      if (null != typeOrFeatures) {
        for (int j = 0; j < typeOrFeatures.length; j++) {
          if (search.matcher(typeOrFeatures[j].getName()).find()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Gets the matching delegate component descriptors.
   *
   * @return the matching delegate component descriptors
   */
  public List getMatchingDelegateComponentDescriptors() {
    return m_matchingDelegateComponentDescriptors;
  }

  /**
   * Gets the matching delegate component descriptions.
   *
   * @return the matching delegate component descriptions
   */
  public List getMatchingDelegateComponentDescriptions() {
    return m_matchingDelegateComponentDescriptions;
  }

  /**
   * Sets the status msg.
   *
   * @param nWhich
   *          the n which
   * @param msg
   *          the msg
   */
  private void setStatusMsg(int nWhich, String msg) {
    m_nWhichStatusMsg = nWhich;
    m_statusMsg = msg;

    if (m_dialog.getStatusLabel1().isDisposed())
      return;
    Display display = m_dialog.getStatusLabel1().getDisplay();
    display.syncExec(new Runnable() {
      @Override
      public void run() {
        if (m_nWhichStatusMsg == 1) {
          m_dialog.getStatusLabel1().setText(m_statusMsg);
        } else {
          m_dialog.getStatusLabel2().setText(m_statusMsg);
        }
      }
    });

  }

  /**
   * Gets the brief display version.
   *
   * @param filePathName
   *          the file path name
   * @return the brief display version
   */
  private String getBriefDisplayVersion(String filePathName) {
    if (filePathName == null) {
      return null;
    }
    filePathName = AbstractSection.maybeShortenFileName(filePathName);
    return filePathName;
  }

}
