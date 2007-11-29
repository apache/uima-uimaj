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

import org.apache.uima.pear.PearException;
import org.apache.uima.pear.PearPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class UimaNature implements IProjectNature {

  private IProject project;

  /**
   * constructor.
   */
  public UimaNature() {
    super();
  }

  /**
   * 
   * @see org.eclipse.core.resources.IProjectNature#configure()
   */
  public void configure() throws CoreException {
    try {
      ProjectCustomizer.customizeProject(project);
    } catch (PearException e) {
      e.printStackTrace();
      e.openErrorDialog(PearPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow()
              .getShell());
    }
  }

  /**
   * 
   * 
   * @see org.eclipse.core.resources.IProjectNature#deconfigure()
   */
  public void deconfigure() throws CoreException {
  }

  /**
   * Returns local reference to associated project
   * 
   * @see org.eclipse.core.resources.IProjectNature#getProject()
   */
  public IProject getProject() {
    return project;
  }

  /**
   * Sets local reference to associated project.
   * 
   * @see org.eclipse.core.resources.IProjectNature#setProject(IProject)
   */
  public void setProject(IProject value) {
    project = value;
  }

}