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

package org.apache.uima.pear.insd.edit;

import java.io.File;
import java.io.IOException;

import org.apache.uima.pear.PearException;
import org.apache.uima.pear.nature.ProjectCustomizer;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.xml.sax.SAXException;

import org.apache.uima.pear.tools.InstallationDescriptor;
import org.apache.uima.pear.tools.InstallationDescriptorHandler;

/**
 * 
 * Handles the PEAR installation descriptor (install.xml)
 * 
 * 
 * 
 */
public class PearInstallationDescriptor {

  public static final String INSTALLATION_DESCRIPTOR_PATH = "metadata/install.xml";

  /**
   * @param currentContainer
   *          An IProject with the UIMA nature
   * @param insd
   *          The installation descriptor object
   * @throws CoreException
   *           if there is problem accessing the corresponding resource
   * @throws IOException
   *           if there is problem writing to the corresponding resource
   */
  public static void saveInstallationDescriptor(IContainer currentContainer,
          InstallationDescriptor insd) throws CoreException, IOException {
    IFile installFile = currentContainer.getFile(new Path(INSTALLATION_DESCRIPTOR_PATH));
    installFile.setContents(InstallationDescriptorHandler.getInstallationDescriptorAsStream(insd),
            false, true, null);
  }

  /**
   * 
   * Returns an InstallationDescriptor instance representing the installation descriptor of the
   * given project. If not existent, it return and empty InstallationDescriptor instance.
   * 
   * @param currentContainer
   *          An IProject with the UIMa Nature
   * @return InstallationDescriptor The Installation descriptor object
   * @throws IOException
   *           if there is problem writing to the xml file of the installation descriptor
   * @throws SAXException
   *           if there is problem parsing the xml of the installation descriptor
   * @throws CoreException
   *           if there is problem with resources
   * @throws PearException
   *           if there is problem with PEAR related operations
   * 
   */
  public static InstallationDescriptor getInstallationDescriptor(IContainer currentContainer)
          throws IOException, SAXException, CoreException, PearException {
    InstallationDescriptor insd = new InstallationDescriptor();
    InstallationDescriptorHandler insdh = new InstallationDescriptorHandler();
    IFile installFile = currentContainer.getFile(new Path(INSTALLATION_DESCRIPTOR_PATH));
    if (installFile.exists()) {
      insdh.parse(installFile.getContents());
      insd = insdh.getInstallationDescriptor();
    } else
      createInstallationDescriptor(currentContainer, insd, false);
    return insd;
  }

  /**
   * Create the PEAR installation descriptor file (install.xml) using a given InstallationDescriptor
   * instance.
   * 
   * @param newContainer
   *          An IProject with the UIMA nature
   * @param insd
   *          an instance of Installationdescriptor
   * @param overrideContentIfExist
   *          if true overrides any existing install.xml
   * @throws PearException
   *           If a problem occurs.
   */
  public static void createInstallationDescriptor(IContainer newContainer,
          InstallationDescriptor insd, boolean overrideContentIfExist) throws PearException {
    try {
      ProjectCustomizer.createFile(newContainer, INSTALLATION_DESCRIPTOR_PATH,
              InstallationDescriptorHandler.getInstallationDescriptorAsStream(insd),
              overrideContentIfExist);
    } catch (Throwable e) {
      PearException subEx = new PearException("The Installation Descriptor ("
              + INSTALLATION_DESCRIPTOR_PATH + ") could not be created/saved properly.", e);
      throw subEx;
    }
  }

  /**
   * adds the $main_root/ macro as a prefix to the given string
   * 
   * @param s
   *          a String representing a relative path to the project root
   * @return The String with the macro
   */
  public static String addMacro(String s) {
    String macro = "$main_root";
    if (s == null || s.length() == 0)
      return "";
    else {
      if (s.startsWith("/") || s.startsWith("\\"))
        s = macro + s;
      else
        s = macro + File.separator + s;
      return s;
    }
  }

}