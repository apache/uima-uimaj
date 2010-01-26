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

package org.apache.uima.caseditor.ui.property;


import org.apache.uima.caseditor.core.model.INlpElement;
import org.apache.uima.caseditor.core.model.NlpProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPropertyPage;

/**
 * TODO: add javadoc here
 */
public abstract class NlpProjectFieldEditorPage extends FieldEditorPreferencePage
        implements IWorkbenchPropertyPage {
  
    private IAdaptable mElement;

    private IPreferenceStore mPreferenceStore;

    NlpProjectFieldEditorPage(int style) {
        super(style);
    }

    public NlpProjectFieldEditorPage(String title, int style) {
          super(title, style);
    }

    public NlpProjectFieldEditorPage(String title, ImageDescriptor image,
            int style) {
        super(title, image, style);
    }

    /**
     * Retrieves the project of the currently selected NLPElement.
     *
     * @return - project of selected NLPElement.
     */
    protected NlpProject getProject()
    {
        return ((INlpElement) getElement()).getNlpProject();
    }

    @Override
    public void createControl(Composite parent)
    {
        if (isPropertyPage())
        {
            NlpProject nlpProject = getProject();

            mPreferenceStore = new DotCorpusPreferenceStore(
                    nlpProject.getDotCorpus());
        }

        super.createControl(parent);

        if (isPropertyPage())
        {
            // updateFieldEditors();
        }
    }

    @Override
    public IPreferenceStore getPreferenceStore()
    {
        IPreferenceStore result;

        if (isPropertyPage())
        {
            result = mPreferenceStore;
        }
        else
        {
            result = super.getPreferenceStore();
        }

        return result;
    }

    @Override
    protected void createFieldEditors()
    {
    }

    public IAdaptable getElement()
    {
        return mElement;
    }

    public void setElement(IAdaptable element)
    {
        mElement = element;
    }

    /**
     * Indicates if current is used as a property page.
     *
     * @return - true if property page
     */
    public boolean isPropertyPage()
    {
        return mElement != null;
    }
}