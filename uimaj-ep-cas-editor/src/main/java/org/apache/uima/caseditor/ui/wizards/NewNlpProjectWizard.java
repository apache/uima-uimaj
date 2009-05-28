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

package org.apache.uima.caseditor.ui.wizards;


import org.apache.uima.caseditor.core.model.NlpProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * New wizard for nlp projects.
 */
final public class NewNlpProjectWizard extends Wizard implements INewWizard
{
    /**
     * The ID of the new nlp project wizard.
     */
    public static final String ID = "org.apache.uima.caseditor.wizards.NLPProjectWizard";

    private NewNlpProjectWizardPage mMainPage;

    public NewNlpProjectWizard() {
    }

    /**
     * Initializes the <code>NLPProjectWizard</code>.
     */
    public void init(IWorkbench workbench, IStructuredSelection selection)
    {
      setWindowTitle("New NLP project");
    }

    /**
     * Adds the project wizard page to the wizard.
     */
    @Override
    public void addPages()
    {
        mMainPage = new NewNlpProjectWizardPage();
        mMainPage.setTitle("Create a NLP project");
        mMainPage.setDescription("Create a NLP project in the workspace");
        addPage(mMainPage);
    }

    /**
     * Creates the nlp project.
     */
    @Override
    public boolean performFinish()
    {
        // TODO: only return true if everyting goes well
        IProject newNLPProject = mMainPage.getProjectHandle();

        createProject(newNLPProject, mMainPage.getLocationPath());

        try
        {
            NlpProject.addNLPNature(newNLPProject);
        }
        catch (CoreException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private static void createProject(IProject project, IPath location)
    {
        if (!project.exists())
        {
            IProjectDescription projectDescribtion = project.getWorkspace()
                    .newProjectDescription(project.getName());

            if (Platform.getLocation().equals(location))
            {
                location = null;
            }

            projectDescribtion.setLocation(location);

            try
            {
                project.create(projectDescribtion, null);
            }
            catch (CoreException e)
            {
                // TODO: show error message
                e.printStackTrace();
            }

        }

        if (!project.isOpen())
        {
            try
            {
                project.open(null);
            }
            catch (CoreException e)
            {
                // TODO: show error message
                e.printStackTrace();
            }
        }
    }
}