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


import org.apache.uima.caseditor.editor.editview.EditView;
import org.apache.uima.caseditor.editor.fsview.FeatureStructureBrowserView;
import org.apache.uima.caseditor.editor.styleview.AnnotationStyleView;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * This <code>PerspectiveFactory</code> generates the initial layout
 * for the NLP perspective.
 */
public class CasEditorPerspectiveFactory implements IPerspectiveFactory {
  
  /**
   * ID of the perspective factory. Use this ID for example in the plugin.xml
   * file.
   *
   * Note: This id should also be changed, but that will break existing
   * perspectives, and might confuse users.
   */
  public static String ID = "org.apache.uima.caseditor.perspective.NLP";

  /**
   * Define the initial layout of the Cas Editor Perspective
   */
  public void createInitialLayout(IPageLayout layout) {
    defineActions(layout);
    defineLayout(layout);
  }

  private void defineActions(IPageLayout layout) {

    // add "show views"
    layout.addShowViewShortcut("org.eclipse.ui.navigator.ProjectExplorer");
    layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
    layout.addShowViewShortcut(AnnotationStyleView.ID);

    
    // add "open perspective"
    layout.addPerspectiveShortcut(CasEditorPerspectiveFactory.ID);
  }

  private void defineLayout(IPageLayout layout) {
    String editorArea = layout.getEditorArea();

    // left views
    IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT,
            0.19f, editorArea);
    left.addView("org.eclipse.ui.navigator.ProjectExplorer");

    // right views
    IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT,
            0.70f, editorArea);

    right.addView(IPageLayout.ID_OUTLINE);
    right.addView(FeatureStructureBrowserView.ID);
    
    IFolderLayout rightBottomCorner  = layout.createFolder("rightBottomCorner", IPageLayout.BOTTOM,
            0.75f, "right");
    rightBottomCorner.addView(AnnotationStyleView.ID);

    // bottom views
    IFolderLayout rightBottom = layout.createFolder("rightBottom",
            IPageLayout.BOTTOM, 0.75f, editorArea);

    rightBottom.addView(EditView.ID);

    IFolderLayout leftBottom = layout.createFolder("leftBottom",
            IPageLayout.RIGHT, 0.5f, EditView.ID);

    leftBottom.addView(EditView.ID_2);
  }
}

