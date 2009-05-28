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

package org.apache.uima.caseditor.ui;


import org.apache.uima.caseditor.editor.editview.EditView;
import org.apache.uima.caseditor.editor.fsview.FeatureStructureBrowserView;
import org.apache.uima.caseditor.ui.corpusview.CorpusExplorerView;
import org.apache.uima.caseditor.ui.wizards.NewNlpProjectWizard;
import org.apache.uima.caseditor.ui.wizards.WizardNewFileCreation;
import org.apache.uima.caseditor.ui.wizards.WizardNewFolderCreation;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * This <code>PerspectiveFactory</code> generates the initial layout
 * for the NLP perspective.
 */
public class NlpPerspectiveFactory implements IPerspectiveFactory
{
  /**
   * ID of the perspective factory. Use this ID for example in the plugin.xml
   * file.
   */
  public static String ID = "Annotator.perspective.NLP";

  /**
   * Define the initial layout of the nlp perspective
   */
  public void createInitialLayout(IPageLayout layout)
  {
    defineActions(layout);
    defineLayout(layout);
  }

  private void defineActions(IPageLayout layout)
  {
    // add "new wizards"
    layout.addNewWizardShortcut(NewNlpProjectWizard.ID);
    //        layout.addNewWizardShortcut(NewCorpusWizard.ID);
    layout.addNewWizardShortcut(WizardNewFolderCreation.ID);
    layout.addNewWizardShortcut(WizardNewFileCreation.ID);

    // layout.addNewWizardShortcut("Annotator.NewDocumentWizard");

    // add "show views"
    layout.addShowViewShortcut(CorpusExplorerView.ID);
    layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);

    // add "open perspective"
    layout.addPerspectiveShortcut(NlpPerspectiveFactory.ID);
  }

  private void defineLayout(IPageLayout layout)
  {
    String editorArea = layout.getEditorArea();

    // left views
    IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT,
            0.19f, editorArea);
    left.addView(CorpusExplorerView.ID);

    // right views
    IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT,
            0.70f, editorArea);

    right.addView(IPageLayout.ID_OUTLINE);
    right.addView(FeatureStructureBrowserView.ID);
    right.addView("org.eclipse.pde.runtime.LogView");

    // bottom views
    IFolderLayout rightBottom = layout.createFolder("rightBottom",
            IPageLayout.BOTTOM, 0.75f, editorArea);

    rightBottom.addView(EditView.ID);

    IFolderLayout leftBottom = layout.createFolder("leftBottom",
            IPageLayout.RIGHT, 0.5f, EditView.ID);

    leftBottom.addView(EditView.ID_2);
  }
}
