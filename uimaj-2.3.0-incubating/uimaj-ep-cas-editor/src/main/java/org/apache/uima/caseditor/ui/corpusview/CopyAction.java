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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.part.ResourceTransfer;

/**
 * The CopyAction can copy IResources to the <code>clipboard</code>.
 */
final class CopyAction  extends ResourceAction {

  /**
   * OS clipboard
   */
  private Clipboard mClipboard;

  /**
   * Initializes the current instance.
   *
   * @param clipboard
   */
  public CopyAction(Clipboard clipboard) {
    super("Copy");

    if (clipboard == null) {
        throw new IllegalArgumentException();
    }

    mClipboard = clipboard;
  }

  /**
   * Starts the copy process.
   */
  @Override
  public void run() {
    List<IResource> selectedResources = getSelectedResources();

    List<String> fileNames = new LinkedList<String>();

    List<IResource> resources = new LinkedList<IResource>();

    for (IResource resource : selectedResources) {
      IPath location = resource.getLocation();

       if (location != null) {
        resources.add(resource);

        fileNames.add(location.toOSString());
      }
    }

    IResource[] resourcesArray = resources.toArray(new IResource[resources.size()]);

    String[] fileNamesArray = fileNames.toArray(new String[fileNames.size()]);

    try {
      mClipboard.setContents(new Object[] { resourcesArray, fileNamesArray }, new Transfer[] {
          ResourceTransfer.getInstance(), FileTransfer.getInstance() });
     } catch (SWTError e) {
      if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD) {
        throw e;
      }
    }
  }

  @Override
  protected boolean updateSelection(IStructuredSelection selection) {

    boolean result;

    int nonResources = getStructuredSelection().size() -
        getSelectedResources().size();

    if (nonResources > 0) {
      result = false;
    }
    else if (getSelectedResources().size() == 0) {
      result = false;
    }
    // currenlty project copy is not supported
    else if (selectionIsOfType(IResource.PROJECT)) {
      return false;
    }
    else if (!selectionIsOfType(IResource.PROJECT) &&
            !selectionIsOfType(IResource.FILE | IResource.FOLDER)) {
      result = false;
    }
    else if (!isAllHaveSameParent()) {
      result = false;
    }
    else {
      result = true;
    }

    return result;
  }

  private boolean isAllHaveSameParent() {
    List<IResource> resources = getSelectedResources();

    assert resources.size() > 0;

    // search for non identical parent
    IResource parent = resources.get(0).getParent();
    for (IResource resource : resources) {
      if (!resource.getParent().equals(parent)) {
        return false;
      }
    }

    return true;
  }
}
