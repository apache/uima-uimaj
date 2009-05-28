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

package org.apache.uima.caseditor.ui.model;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.Images;
import org.apache.uima.caseditor.core.model.CasProcessorFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * The <code>IWorkbenchAdapter</code> for the
 * <code>UIMASourceFolder</code>.
 */
class ProcessorFolderAdapter extends
        AbstractElementAdapter
{
    /**
     * Retrieves the children of the current <code>UIMASourceFolder</code>.
     */
    public Object[] getChildren(Object o)
    {
        CasProcessorFolder sourceFolder = (CasProcessorFolder) o;

        Collection<Object> childs = new LinkedList<Object>();

        childs.addAll(sourceFolder.getAnnotators());

        childs.addAll(sourceFolder.getConsumers());

        try
        {
            childs.addAll(sourceFolder.getNonNlpResources());
        }
        catch (CoreException e)
        {
        	// sorry this fails if the resource was already removed
        }

        return childs.toArray();
    }

    /**
     * Retrieves the <code>ImageDescriptor</code> for uima folder.
     */
    public ImageDescriptor getImageDescriptor(Object object)
    {
        return CasEditorPlugin.getTaeImageDescriptor(
                Images.MODEL_PROCESSOR_FOLDER);
    }
}
