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

package org.apache.uima.caseditor.ui.corpusview;

import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.CopyFilesAndFoldersOperation;
import org.eclipse.ui.actions.CopyProjectOperation;
import org.eclipse.ui.part.ResourceTransfer;


final class PasteAction  extends ResourceAction
{

  private Shell mShell;

  private Clipboard mClipboard;

  /**
   * Initializes the current instance.
   *
   * @param shell
   * @param clipboard
   */
  public PasteAction(Shell shell, Clipboard clipboard) {
    super("Paste");

    if (shell == null || clipboard == null) {
      throw new IllegalArgumentException();
    }

    mShell = shell;
    mClipboard = clipboard;
  }

  /**
   * Locates the new parent resource for the pasted ones.
   *
   * If currently selected resource is a file, the parent of the file is returned.
   *
   * @return
   */
  private IContainer getNewParent() {
    List<IResource> selection = getSelectedResources();

    Assert.isTrue(selection.size() > 0);

    IContainer result;

    if (selection.get(0) instanceof IFile) {
      result = ((IFile) selection.get(0)).getParent();
    } else {
      result = (IContainer) selection.get(0);
    }

    return result;
  }

  @Override
  public void run() {
    IResource[] resourceData = (IResource[]) mClipboard.getContents(ResourceTransfer.getInstance());

    if (resourceData != null && resourceData.length > 0) {

      // resource always contains either projects or files and folders
      if (resourceData[0].getType() != IResource.PROJECT) {
        CopyFilesAndFoldersOperation operation = new CopyFilesAndFoldersOperation(mShell);
        operation.copyResources(resourceData, getNewParent());
      } else {
        for (IResource element : resourceData) {
            new CopyProjectOperation(mShell).copyProject((IProject) element);
        }
      }

      return;
    } else {

      String[] fileData = (String[]) mClipboard.getContents(FileTransfer.getInstance());

      if (fileData != null && fileData.length > 0) {
        new CopyFilesAndFoldersOperation(mShell).copyFiles(fileData, getNewParent());
      }
    }
  }


  @Override
  protected boolean updateSelection(IStructuredSelection selection) {

    IResource[] resources = (IResource[]) mClipboard
            .getContents(ResourceTransfer.getInstance());

    String[] files = (String[]) mClipboard.getContents(FileTransfer.getInstance());

    if (resources == null && files == null) {
      return false;
    }
//    else if (resources.length == 0 && files.length == 0) {
//      return false;
//    }

    if (resources != null ) {
    if (resources[0].getType() == IResource.PROJECT) {

      for (IResource resource : resources) {
        if (resource.getType() == IResource.PROJECT
                && !((IProject) resource).isOpen()) {
          return false;
        }
      }

      return true;
    }

    if (getSelectedNonResources().size() > 0) {
      return false;
    } else if (getSelectedResources().size() != 1) {
      return false;
    }

    // linked resources are not supported
    for (IResource resource : resources) {
      if (resource.isLinked()) {
        return false;
      }
    }

    // if parent is project make sure its open
    IResource parent = getNewParent();

    if (parent instanceof IProject) {
      IProject parentProject = (IProject) parent;

      if (!parentProject.isOpen()) {
        return false;
      }
    }

    // do not copy folder to itself
    for (IResource resource : resources) {
      if (resource.equals(parent)) {
        return false;
      }
    }
    }

    return true;
  }
}
