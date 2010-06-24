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

package org.apache.uima.caseditor.core.test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

/**
 */
class WorkspaceUtil {
  /**
   * Deletes all projects inside the workspace.
   *
   * Will be run after every test.
   *
   * @throws CoreException
   *           is thrown if deletion goes wrong
   */
  static void clearWorkspace() throws CoreException {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

    IProject projects[] = root.getProjects();

    for (IProject project : projects) {
      project.delete(true, null);
    }
  }

  static boolean containsIFolder(IResource resources[], String name) {
    for (IResource resource : resources) {
      if (name.equals(resource.getName())) {
        return true;
      }
    }

    return false;
  }
}
