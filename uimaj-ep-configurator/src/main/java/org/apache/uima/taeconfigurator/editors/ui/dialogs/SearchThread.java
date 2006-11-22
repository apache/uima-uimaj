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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.swt.widgets.Display;

import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;

public class SearchThread implements Runnable /* extends Thread */{
  private String m_fileNameSearch, m_inputTypeSearch, m_outputTypeSearch, m_projectToSearch;

  FindComponentDialog m_dialog;

  private AbstractSection m_aggregateSection;

  private List m_matchingDelegateComponentDescriptors;

  private List m_matchingDelegateComponentDescriptions;

  int m_nWhichStatusMsg;

  String m_statusMsg;

  private String[] m_componentHeaders;

  public SearchThread(FindComponentDialog dialog, AbstractSection aggregateSection,
                  String fileNameSearch, String inputTypeSearch, String outputTypeSearch,
                  String projectToSearch, String[] componentHeaders) {

    m_dialog = dialog;
    m_aggregateSection = aggregateSection;
    m_fileNameSearch = fileNameSearch;
    m_inputTypeSearch = inputTypeSearch;
    m_outputTypeSearch = outputTypeSearch;
    m_projectToSearch = projectToSearch;
    m_componentHeaders = componentHeaders;
  }

  private boolean m_bDieNow = false;

  private boolean m_bDone = false;

  public void setDieNow() {
    m_bDieNow = true;
  }

  public boolean getDieNow() {
    return m_bDieNow;
  }

  public boolean isDone() {
    return m_bDone;
  }

  public void run() {
    m_matchingDelegateComponentDescriptors = new ArrayList();
    m_matchingDelegateComponentDescriptions = new ArrayList();

    getDelegateComponentsByInputOutputTypes(m_projectToSearch);

    m_bDone = true;
  }

  private void testOneResource(IResource resource) {
    switch (resource.getType()) {
      case IResource.FILE:
        if (resource.getName().toLowerCase().endsWith(".xml")
                        // exclude potentially many data files, not descriptors
                        && !resource.getName().toLowerCase().endsWith(".txt.xml")
                        && (m_fileNameSearch == null || resource.getName()
                                        .matches(m_fileNameSearch))) {
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

  private boolean delegateComponentMatchesCapabilityReqs(ResourceCreationSpecifier rs,
                  String inputTypeSearch, String outputTypeSearch) {

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

  private static final boolean INPUT = true;

  private static final boolean OUTPUT = false;

  private boolean matchCapabilitiesTo(Capability[] capabilities, String search, boolean isInput) {
    if (null == search)
      return true;
    for (int i = 0; i < capabilities.length; i++) {
      TypeOrFeature[] typeOrFeatures = isInput ? capabilities[i].getInputs() : capabilities[i]
                      .getOutputs();
      if (null != typeOrFeatures) {
        for (int j = 0; j < typeOrFeatures.length; j++) {
          if (typeOrFeatures[j].getName().matches(search)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public List getMatchingDelegateComponentDescriptors() {
    return m_matchingDelegateComponentDescriptors;
  }

  public List getMatchingDelegateComponentDescriptions() {
    return m_matchingDelegateComponentDescriptions;
  }

  private void setStatusMsg(int nWhich, String msg) {
    m_nWhichStatusMsg = nWhich;
    m_statusMsg = msg;

    if (m_dialog.getStatusLabel1().isDisposed())
      return;
    Display display = m_dialog.getStatusLabel1().getDisplay();
    display.syncExec(new Runnable() {
      public void run() {
        if (m_nWhichStatusMsg == 1) {
          m_dialog.getStatusLabel1().setText(m_statusMsg);
        } else {
          m_dialog.getStatusLabel2().setText(m_statusMsg);
        }
      }
    });

  }

  private String getBriefDisplayVersion(String filePathName) {
    if (filePathName == null) {
      return null;
    }
    filePathName = AbstractSection.maybeShortenFileName(filePathName);
    return filePathName;
  }

}
