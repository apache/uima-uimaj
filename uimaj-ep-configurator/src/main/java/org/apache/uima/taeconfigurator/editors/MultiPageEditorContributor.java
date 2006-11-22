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

package org.apache.uima.taeconfigurator.editors;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import org.apache.uima.taeconfigurator.Messages;
import org.apache.uima.taeconfigurator.PreferencePage;
import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;
import org.apache.uima.taeconfigurator.editors.xml.XMLEditor;

// import org.eclipse.jdt.launching.IVMRunner;

// import org.apache.uima.jcas.jcasgen.Prefs;

/**
 * Manages the installation/deinstallation of global actions for multi-page editors. Responsible for
 * the redirection of global actions to the active editor. Multi-page contributor replaces the
 * contributors for the individual editors in the multi-page editor.
 */
public class MultiPageEditorContributor extends MultiPageEditorActionBarContributor {
  private IEditorPart activeEditorPart;

  Action autoJCasAction;

  Action qualifiedTypesAction;

  Action runJCasGenAction;

  /**
   * Creates a multi-page contributor.
   */
  public MultiPageEditorContributor() {
    super();
    createActions();
  }

  /**
   * Returns the action registed with the given text editor.
   * 
   * @return IAction or null if editor is null.
   */
  protected IAction getAction(MultiPageEditorPart editor, String actionID) {
    ITextEditor txtEditor = ((MultiPageEditor) editor).getSourcePageEditor();
    return (txtEditor == null ? null : txtEditor.getAction(actionID));
  }

  protected IAction getAction1(ITextEditor editor, String actionID) {
    return (editor == null ? null : editor.getAction(actionID));
  }

  /*
   * (non-JavaDoc) Method declared in AbstractMultiPageEditorActionBarContributor.
   */

  public void setActiveEditor(IEditorPart part) {
    if (activeEditorPart == part)
      return;

    if (null == part)
      return;
    activeEditorPart = part;

    IActionBars actionBars = getActionBars();
    if (actionBars != null) {

      MultiPageEditorPart editor = (MultiPageEditorPart) part;

      actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), getAction(editor,
                      ITextEditorActionConstants.DELETE));
      actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), getAction(editor,
                      ITextEditorActionConstants.UNDO));
      actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), getAction(editor,
                      ITextEditorActionConstants.REDO));
      actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(), getAction(editor,
                      ITextEditorActionConstants.CUT));
      actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), getAction(editor,
                      ITextEditorActionConstants.COPY));
      actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), getAction(editor,
                      ITextEditorActionConstants.PASTE));
      actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), getAction(editor,
                      ITextEditorActionConstants.SELECT_ALL));
      actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(), getAction(editor,
                      ITextEditorActionConstants.FIND));
      actionBars.setGlobalActionHandler(IDEActionFactory.BOOKMARK.getId(), getAction(editor,
                      IDEActionFactory.BOOKMARK.getId()));
      actionBars.updateActionBars();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.part.MultiPageEditorActionBarContributor#setActivePage(org.eclipse.ui.IEditorPart)
   */
  public void setActivePage(IEditorPart part) {

    IActionBars actionBars = getActionBars();
    if (actionBars != null) {

      ITextEditor textEditor = (part instanceof XMLEditor) ? (ITextEditor) part : null;

      actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), getAction1(textEditor,
                      ITextEditorActionConstants.DELETE));
      actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), getAction1(textEditor,
                      ITextEditorActionConstants.UNDO));
      actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), getAction1(textEditor,
                      ITextEditorActionConstants.REDO));
      actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(), getAction1(textEditor,
                      ITextEditorActionConstants.CUT));
      actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), getAction1(textEditor,
                      ITextEditorActionConstants.COPY));
      actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), getAction1(textEditor,
                      ITextEditorActionConstants.PASTE));
      actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), getAction1(textEditor,
                      ITextEditorActionConstants.SELECT_ALL));
      actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(), getAction1(textEditor,
                      ITextEditorActionConstants.FIND));
      actionBars.setGlobalActionHandler(IDEActionFactory.BOOKMARK.getId(), getAction1(textEditor,
                      IDEActionFactory.BOOKMARK.getId()));
      actionBars.updateActionBars();
    }
  }

  private void createActions() {

    autoJCasAction = new Action() {
      public void run() {
        TAEConfiguratorPlugin plugin = TAEConfiguratorPlugin.getDefault();
        Preferences prefs = plugin.getPluginPreferences();
        boolean bAutoJCasGen = !prefs.getBoolean(PreferencePage.P_JCAS); //$NON-NLS-1$
        autoJCasAction.setChecked(bAutoJCasGen);
        prefs.setValue(PreferencePage.P_JCAS, bAutoJCasGen); //$NON-NLS-1$
      }
    };

    runJCasGenAction = new Action() {
      public void run() {
        ((MultiPageEditor) activeEditorPart).doJCasGenChkSrc(null); // don't know how to get
                                                                    // progress monitor
      }
    };

    qualifiedTypesAction = new Action() {
      public void run() {
        TAEConfiguratorPlugin plugin = TAEConfiguratorPlugin.getDefault();
        Preferences prefs = plugin.getPluginPreferences();
        boolean bFullyQualifiedTypeNames = !prefs
                        .getBoolean(PreferencePage.P_SHOW_FULLY_QUALIFIED_NAMES); //$NON-NLS-1$
        qualifiedTypesAction.setChecked(bFullyQualifiedTypeNames);
        prefs.setValue(PreferencePage.P_SHOW_FULLY_QUALIFIED_NAMES, bFullyQualifiedTypeNames); //$NON-NLS-1$

        // mark all pages as stale for all editors, since this is a global setting
        IWorkbenchPage[] pages = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPages();
        for (int i = 0; i < pages.length; i++) {
          IWorkbenchPart[] editors = pages[i].getEditors();
          for (int j = 0; j < editors.length; j++) {
            if (editors[j] != null && editors[j] instanceof MultiPageEditor) {
              ((MultiPageEditor) editors[j]).markAllPagesStale();
            }
          }
        }

      }
    };

    autoJCasAction.setText(Messages.getString("MultiPageEditorContributor.autoGenJCas")); //$NON-NLS-1$
    autoJCasAction.setChecked(getAutoJCasGen()); //$NON-NLS-1$

    qualifiedTypesAction.setText(Messages.getString("MultiPageEditorContributor.showFullNames")); //$NON-NLS-1$
    qualifiedTypesAction.setChecked(getUseQualifiedTypes()); //$NON-NLS-1$

    runJCasGenAction.setText("Run JCasGen");
  }

  public void contributeToMenu(IMenuManager manager) {

    IMenuManager menu = new MenuManager("&UIMA"); //$NON-NLS-1$
    manager.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, menu);
    menu.add(runJCasGenAction);
    IMenuManager settingsMenu = new MenuManager("Settings"); //$NON-NLS-1$
    menu.add(settingsMenu);
    settingsMenu.add(autoJCasAction);
    settingsMenu.add(qualifiedTypesAction);
  }

  public static boolean getAutoJCasGen() {
    return getUimaPrefBoolean(PreferencePage.P_JCAS, true);
  }

  public static boolean getUseQualifiedTypes() {
    return getUimaPrefBoolean(PreferencePage.P_SHOW_FULLY_QUALIFIED_NAMES, true);
  }

  public static int getXMLindent() {
    return getUimaPrefInt(PreferencePage.P_XML_TAB_SPACES, 2);
  }

  public static String getCDEVnsHost() {
    return getUimaPrefString(PreferencePage.P_VNS_HOST, "localhost");
  }

  public static String getCDEVnsPort() {
    return getUimaPrefString(PreferencePage.P_VNS_PORT, "9000");
  }

  public static void setVnsHost(String v) {
    System.setProperty("VNS_HOST", v);
  }

  public static void setVnsPort(String v) {
    System.setProperty("VNS_PORT", v);
  }

  public static void setVnsHost() {
    setVnsHost(getCDEVnsHost());
  }

  public static void setVnsPort() {
    setVnsPort(getCDEVnsPort());
  }

  private static String getUimaPrefString(String key, String defaultValue) {
    TAEConfiguratorPlugin plugin = TAEConfiguratorPlugin.getDefault();
    Preferences prefs = plugin.getPluginPreferences();
    boolean isDefault = prefs.isDefault(key);
    if (isDefault)
      prefs.setDefault(key, defaultValue);
    return prefs.getString(key);
  }

  private static boolean getUimaPrefBoolean(String key, boolean defaultValue) {
    TAEConfiguratorPlugin plugin = TAEConfiguratorPlugin.getDefault();
    Preferences prefs = plugin.getPluginPreferences();
    boolean isDefault = prefs.isDefault(key);
    if (isDefault)
      prefs.setDefault(key, defaultValue);
    return prefs.getBoolean(key);
  }

  private static int getUimaPrefInt(String key, int defaultValue) {
    TAEConfiguratorPlugin plugin = TAEConfiguratorPlugin.getDefault();
    Preferences prefs = plugin.getPluginPreferences();
    boolean isDefault = prefs.isDefault(key);
    if (isDefault)
      prefs.setDefault(key, defaultValue);
    return prefs.getInt(key);
  }

}
