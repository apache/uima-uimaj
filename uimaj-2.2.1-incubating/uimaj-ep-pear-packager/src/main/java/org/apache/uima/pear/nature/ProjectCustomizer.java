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

package org.apache.uima.pear.nature;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.uima.pear.PearException;
import org.apache.uima.pear.insd.edit.PearInstallationDescriptor;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;

import org.apache.uima.pear.tools.InstallationDescriptor;

/**
 * 
 * A util class to handle customization of IProject with the UIMA nature
 * 
 * 
 * 
 */
public class ProjectCustomizer {

  public static final String UIMA_NATURE_ID = "org.apache.uima.pear.UimaNature";

  // public static final String STORE_PATH = "metadata/store.xml";

  /**
   * Customizes an IProject with the UIMA nature using an InstallationDescriptor instance
   * 
   * @param container
   *          An IProject
   * @param insd
   *          An InstallationDescriptor
   * @throws PearException
   *           If a problem occurs
   */
  public static void customizeProject(IContainer container, InstallationDescriptor insd)
          throws PearException {
    createPearFolderStructure(container);
    PearInstallationDescriptor.createInstallationDescriptor(container, insd, false);
    // only add UIMA Nature to projects
    if (container.getType() == IResource.PROJECT) {
      addUIMANature((IProject) container);
    }
  }

  /**
   * Customizes an IProject with the UIMA nature using an empty installation descriptor.
   * 
   * @param project
   *          An IProject
   * @throws PearException
   *           If a problem occurs
   */
  public static void customizeProject(IProject project) throws PearException {
    InstallationDescriptor insd = new InstallationDescriptor();
    customizeProject(project, insd);
  }

  /**
   * Adds the UIMA nature to a project
   * 
   * @param project
   *          an IProject
   * @throws PearException
   *           If a problem occurs
   */
  public static void addUIMANature(IProject project) throws PearException {
    try {
      if (!project.hasNature(UIMA_NATURE_ID)) {
        IProjectDescription description = project.getDescription();
        String[] natures = description.getNatureIds();
        String[] newNatures = new String[natures.length + 1];
        System.arraycopy(natures, 0, newNatures, 0, natures.length);
        newNatures[natures.length] = UIMA_NATURE_ID;
        description.setNatureIds(newNatures);
        project.setDescription(description, null);
        project.close(null);
        project.open(null);
      }
    } catch (Throwable e) {
      PearException subEx = new PearException("The UIMA Nature could not be added properly.", e);
      throw subEx;
    }
  }

  /**
   * Creates the PEAR folder structure
   * 
   * @param container
   *          an IProject
   * @throws PearException
   *           if a problem occurs
   */
  public static void createPearFolderStructure(IContainer container) throws PearException {
    try {
      // Create PEAR Folder Structure - Required Elements always
      ProjectCustomizer.createFolder(container, "metadata");

      // Create PEAR Folder Structure - Optional Elements only for projects
      if (container.getType() == IResource.PROJECT) {
        ProjectCustomizer.createFolder(container, "src");
        ProjectCustomizer.createFolder(container, "bin");
        ProjectCustomizer.createFolder(container, "desc");
        ProjectCustomizer.createFolder(container, "lib");
        ProjectCustomizer.createFolder(container, "data");
        ProjectCustomizer.createFolder(container, "doc");
        ProjectCustomizer.createFolder(container, "conf");
        ProjectCustomizer.createFolder(container, "resources");
      }
    } catch (Throwable e) {
      PearException subEx = new PearException(
              "The PEAR folder structure could not be created properly.", e);
      throw subEx;
    }
  }

  /**
   * Creates a folder, if it does not exist, in a given container
   * 
   * @param container
   *          an IContainer
   * @param folderName
   *          The folder name
   * @return the handle (IFolder) to the folder
   * @throws PearException
   *           If a problem occurs
   */
  public static IFolder createFolder(IContainer container, String folderName) throws PearException {
    try {
      IFolder folder = container.getFolder(new Path(folderName));
      // If the folder does not exist, create and return, failure returns null
      if (!folder.exists()) {
        folder.create(true, true, null);
        return folder;
      } else {
        // return what already exisited
        return folder;
      }
    } catch (Throwable e) {
      PearException subEx = new PearException("folderName could not be created properly.", e);
      throw subEx;
    }
  }

  /**
   * Creates a file in a project with a string as content
   * 
   * @param project
   *          an IProject
   * @param fileName
   *          pathname relative to the project
   * @param s
   *          A string representing the content
   * @param overrideContentIfExist
   *          if true, overrides existing file
   * @return a handle to the file (IFile)
   * @throws PearException
   *           If a problem occurs
   */
  public static IFile createFile(IProject project, String fileName, String s,
          boolean overrideContentIfExist) throws PearException {
    InputStream is = null;
    try {
      is = new ByteArrayInputStream(s.getBytes("UTF-8"));
      return createFile(project, fileName, is, overrideContentIfExist);
    } catch (Throwable e) {
      PearException subEx = new PearException(fileName + " could not be created/saved properly.", e);
      throw subEx;
    }
  }

  /**
   * Creates a file in a project with an InputStream for the content
   * 
   * @param container
   *          an IProject
   * @param fileName
   *          pathname relative to the project
   * @param is
   *          An inputStream for the the content
   * @param overrideContentIfExist
   *          if true, overrides existing file
   * @return a handle to the file (IFile)
   * @throws PearException
   *           If a problem occurs
   */
  public static IFile createFile(IContainer container, String fileName, InputStream is,
          boolean overrideContentIfExist) throws PearException {
    try {
      createPearFolderStructure(container);
      // if we have a container
      if (container.exists()) {
        IFile newFile = container.getFile(new Path(fileName));

        // If the file does not exist, create with content, mark, and return
        if (!newFile.exists()) {
          newFile.create(is, false, null);
          return newFile;
        } else {
          if (overrideContentIfExist)
            newFile.setContents(is, true, true, null);
          // return what already exisited
          return newFile;
        }
      } else
        // return null as there is no container to place a file in
        return null;
    } catch (Throwable e) {
      PearException subEx = new PearException(fileName + " could not be created/saved properly.", e);
      throw subEx;
    }
  }

}
