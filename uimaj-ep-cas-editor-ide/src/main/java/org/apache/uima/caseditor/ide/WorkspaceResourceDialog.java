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

package org.apache.uima.caseditor.ide;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;

/**
 * 
 *
 */
public class WorkspaceResourceDialog 
{
    public static IResource getWorkspaceResourceElement (Shell shell, IResource root,
                                    String dialogTitle, String dialogMessage) 
    {
        IResource resource = null;
        
        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(shell, 
                new WorkbenchLabelProvider(), new WorkbenchContentProvider());
        dialog.setTitle(dialogTitle); 
        dialog.setMessage(dialogMessage); 
        dialog.setInput(root); 
        dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));
        int buttonId = dialog.open();
        if (buttonId == IDialogConstants.OK_ID) {
            resource = (IResource) dialog.getFirstResult();
            if (!resource.isAccessible()) {
                return null;
            }
            if (resource instanceof IContainer) {
            }
            String arg = resource.getFullPath().toString();
            // String fileLoc = VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression("workspace_loc", arg); //$NON-NLS-1$
            // Trace.trace("resource.getFullPath().toString():" + arg);
            // Trace.trace(fileLoc);
            // selectedElement = resource.getLocation().toOSString();
        }
        return resource;
    }


    public static IResource getWorkspaceResourceElement (Shell shell) 
    {
        IResource resource = null;
        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(shell, 
                new WorkbenchLabelProvider(), new WorkbenchContentProvider());
        dialog.setTitle("Select Cpe descriptor"); 
        dialog.setMessage("Select Cpe Xml descriptor file"); 
        dialog.setInput(ResourcesPlugin.getWorkspace().getRoot()); 
        dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));
        int buttonId = dialog.open();
        if (buttonId == IDialogConstants.OK_ID) {
            resource = (IResource) dialog.getFirstResult();
            if (!resource.isAccessible()) {
                return null;
            }
            String arg = resource.getFullPath().toString();
            // String fileLoc = VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression("workspace_loc", arg); //$NON-NLS-1$
            // Trace.trace(fileLoc);
            // selectedElement = resource.getLocation().toOSString();
        }
        return resource;
    }
    
}