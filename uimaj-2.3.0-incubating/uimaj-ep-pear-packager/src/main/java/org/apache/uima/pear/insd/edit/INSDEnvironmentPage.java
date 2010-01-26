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

package org.apache.uima.pear.insd.edit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.uima.pear.*;
import org.apache.uima.pear.insd.edit.vars.VarVal;
import org.apache.uima.pear.insd.edit.vars.VarValList;
import org.apache.uima.pear.insd.edit.vars.VarValViewerHandler;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import org.apache.uima.pear.tools.InstallationDescriptor;
import org.apache.uima.pear.tools.InstallationDescriptorHandler;

/**
 * 
 * This is wizard page to edit PEAR Installation Environment parameters
 * 
 * 
 * 
 */

public class INSDEnvironmentPage extends WizardPage implements InsdConstants {

  public Combo osCombo;

  public Combo jdkVersionCombo;

  public VarValViewerHandler viewer;

  public VarValList envVarList = new VarValList();

  InstallationDescriptor insd;

  IContainer currentContainer;

  Group gr;

  Group gr1;

  ArrayList MissingLibraries = new ArrayList();

  /**
   * Constructor
   * 
   * @param currentContainer
   *          An IProject with the UIMA Nature
   * @param insd
   *          The installation Descriptor object
   * @param wizardData
   *          a hash table with shared information between wizard pages
   */
  public INSDEnvironmentPage(IContainer currentContainer, InstallationDescriptor insd,
          Hashtable wizardData) {
    super("wizardPage");
    setTitle("UIMA - Installation Descriptor - Installation Environment");
    setDescription("Set the installation environment parameters"
            + " and the system properties (e.g. classpath) for your component.\n"
            + "Note: ClassPath entries must start with $main_root/");
    this.insd = insd;
    this.currentContainer = currentContainer;
  }

  /**
   * See IDialogPage#createControl(Composite)
   */
  public void createControl(Composite parent) {

    try {

      Composite container = new Composite(parent, SWT.NULL);

      FormLayout formLayout = new FormLayout();
      container.setLayout(formLayout);

      gr = new Group(container, SWT.NONE);
      gr.setText("Environment Options");
      FormData data = new FormData();
      data.width = 450;
      data.left = new FormAttachment(0, 10);
      data.top = new FormAttachment(0, 10);
      gr.setLayoutData(data);

      GridLayout grLayout = new GridLayout();
      grLayout.numColumns = 2;
      grLayout.verticalSpacing = 4;
      gr.setLayout(grLayout);

      osCombo = addCombo(gr, "Operating System:");
      jdkVersionCombo = addCombo(gr, "JDK level (Minimum):");

      gr1 = new Group(container, SWT.NONE);
      gr1.setText("System Properties");
      data = new FormData();
      data.width = 450;
      data.height = 150;
      data.left = new FormAttachment(0, 10);
      data.top = new FormAttachment(gr, 20, SWT.BOTTOM);
      gr1.setLayoutData(data);

      initialize();
      viewer = new VarValViewerHandler(gr1, VarVal.fieldNames, 3, envVarList);
      initializeCombos();

      dialogChanged();
      setControl(container);
    } catch (Throwable e) {
      PearException subEx = new PearException(
              "The operation failed because the wizard's pages could not be initialized properly.",
              e);
      subEx.openErrorDialog(getShell());
      this.dispose();
    }
  }

  private String getFirstItem(String tokens) {
    String firstItem = "";
    if (tokens != null && tokens.trim().length() > 0) {
      StringTokenizer st = new StringTokenizer(tokens, "\n");
      while (st.hasMoreTokens()) {
        String nextToken = st.nextToken();
        if (nextToken != null && nextToken.trim().length() > 0) {
          firstItem = nextToken;
          break;
        }
      }
    }
    return firstItem;
  }

  private Combo addCombo(Composite parent, String strLabel) {
    Label label = new Label(parent, SWT.NULL);
    label.setText(strLabel);

    Combo text = new Combo(parent, SWT.BORDER | SWT.SINGLE);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    // gd.grabExcessHorizontalSpace = true;
    gd.widthHint = 100;
    text.setLayoutData(gd);

    text.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });
    return text;
  }

  private void initializeCombos() {

    String selectedOS = "";
    String selectedJdkVersion = "";

    selectedOS = getFirstItem(insd.getOSSpecs().getProperty(InstallationDescriptorHandler.NAME_TAG));
    selectedJdkVersion = getFirstItem(insd.getToolkitsSpecs().getProperty(
            InstallationDescriptorHandler.JDK_VERSION_TAG));

    String[] items = { "Windows", "Linux", "Windows and Linux", "AIX" };
    String selectedItem = selectedOS;
    osCombo.setItems(items);
    if (selectedItem != null && !selectedItem.trim().equals(""))
      osCombo.select(osCombo.indexOf(selectedItem));
    else
      osCombo.select(osCombo.indexOf(items[0]));

    items = new String[] { "1.4.0" };
    selectedItem = selectedJdkVersion;
    jdkVersionCombo.setItems(items);
    if (selectedItem != null && !selectedItem.trim().equals(""))
      jdkVersionCombo.select(jdkVersionCombo.indexOf(selectedItem));
    else
      jdkVersionCombo.select(jdkVersionCombo.indexOf(items[0]));

  }

  private void initialize() {
    try {
      envVarList = new VarValList();
      Collection actionInfos = insd
              .getInstallationActions(InstallationDescriptor.ActionInfo.SET_ENV_VARIABLE_ACT);

      Iterator itr = actionInfos.iterator();
      String classPath = null;
      boolean classPathDefined = false;
      while (itr.hasNext()) {
        InstallationDescriptor.ActionInfo actionInfo = (InstallationDescriptor.ActionInfo) itr
                .next();
        String varName = actionInfo.params.getProperty(InstallationDescriptorHandler.VAR_NAME_TAG);
        String varValue = null;
        if (varName != null && varName.trim().equalsIgnoreCase("classpath")) {
          classPath = actionInfo.params.getProperty(InstallationDescriptorHandler.VAR_VALUE_TAG);
          varValue = getInitialClassPath(classPath);
          classPathDefined = true;
        } else
          varValue = actionInfo.params.getProperty(InstallationDescriptorHandler.VAR_VALUE_TAG);

        if (varName != null && varName.trim().length() > 0 && varValue != null) {
          VarVal varVal = new VarVal(varName, varValue);
          envVarList.addTableRow(varVal);
        }
      }
      if (!classPathDefined) {
        VarVal varVal = new VarVal("CLASSPATH", getInitialClassPath(""));
        envVarList.addTableRow(varVal);
      }
    } catch (Throwable e) {
      PearException subEx = new PearException(
              "Error. Cause: the environment page could not be initialized properly.", e);
      subEx.openErrorDialog(getShell());
      this.dispose();
    }
  }

  private String getInitialClassPath(String classPath) {
    // do nothing if the container is not a project
    if (currentContainer.getType() != IResource.PROJECT) {
      return classPath;
    }

    try {
      IJavaProject javaProject = JavaCore.create((IProject) currentContainer);
      // if java project
      if (javaProject != null && javaProject.exists()) {

        MissingLibraries = new ArrayList();

        // get class path
        IClasspathEntry[] classPathEntries = javaProject.getResolvedClasspath(true);
        ArrayList resultStringEntries = tokenizeClassPath(classPath);

        // add classPathEntries
        addToClassPath(classPathEntries, resultStringEntries);

        // add output Location
        System.out.println("Output Location (normal): "
                + javaProject.getOutputLocation().toOSString());
        System.out.println("Output Location (relative): "
                + javaProject.getOutputLocation().makeRelative().toOSString());
        System.out.println("Output Location (absolute): "
                + javaProject.getOutputLocation().makeAbsolute().toOSString());
        String outputLocation = "$main_root/"
                + javaProject.getOutputLocation().makeRelative().removeFirstSegments(1)
                        .toOSString();
        outputLocation = outputLocation.replace('\\', '/');
        outputLocation = outputLocation.trim();
        System.out.println("Output Location (to class path): " + outputLocation);

        if (!contain(resultStringEntries, outputLocation)) {
          resultStringEntries.add(0, outputLocation);
          System.out.println("\tadded Output Location to ClassPath: " + outputLocation);
        }

        // convert class path a to String
        classPath = convertToString(resultStringEntries, ";");

        System.out.println("CLASSPATH: " + classPath);

        // warn about required projects (if any) javaProject.getRequiredProjectNames();
        if (MissingLibraries != null && MissingLibraries.size() > 0) {

          StringBuffer sb = new StringBuffer();
          Iterator itr = MissingLibraries.iterator();
          while (itr.hasNext())
            sb.append("\n- ").append((String) itr.next());
          MessageDialog
                  .openWarning(
                          getShell(),
                          "Missing class path entries",
                          "The following class path entries corresponds to resources not included in your project. Please make sure all the required class path resources (except JRE and UIMA jars) are included in this project and in the PEAR class path (in the environment page of the wizard):"
                                  + sb.toString());
        }
      }
    } catch (Throwable e) {
      MessageDialog
              .openWarning(
                      getShell(),
                      "Class Path Initialization",
                      "The class path could not be initialized properly. Please edit the class path variable in the environment page of the wizard.");
      e.printStackTrace();
    }

    return classPath;
  }

  /*
   * Tokenized a class path string and returns an ArrayList of entries.
   * 
   */
  private ArrayList tokenizeClassPath(String classPath) {
    // tokenize and get an array list of String entries
    ArrayList resultStringEntries = new ArrayList();
    if (classPath != null && !classPath.trim().equals("")) {
      String[] result = classPath.split("\\;");
      for (int i = 0; i < result.length; i++) {
        resultStringEntries.add(result[i].trim());
      }
    }
    return resultStringEntries;
  }

  /*
   * Adds class path entries to a class path if not already included.
   * 
   */
  private ArrayList addToClassPath(IClasspathEntry[] classPathEntries, ArrayList resultStringEntries) {
    if (classPathEntries != null) {
      for (int i = 0; i < classPathEntries.length; i++)
        addToClassPath(classPathEntries[i], resultStringEntries);
    }
    return resultStringEntries;
  }

  /*
   * converts an ArrayList of String to one String separated by a delimiter
   */
  private String convertToString(ArrayList stringEntries, String delim) {
    StringBuffer sb = new StringBuffer();
    Iterator itr = stringEntries.iterator();
    while (itr.hasNext()) {
      sb.append((String) itr.next()).append(";");
    }
    return sb.toString();
  }

  /*
   * Adds an entry to a class path if not already included. Returns an ArrayList of String objects
   * representing the class path entries
   */
  private ArrayList addToClassPath(IClasspathEntry classPathEntry, ArrayList classPath) {

    int kind = classPathEntry.getEntryKind();
    IPath path = classPathEntry.getPath();

    System.out.println("ClassEntry:");
    System.out.println("\tOriginal : " + path.toOSString());
    System.out.println("\tRelative : " + path.makeRelative().toOSString());
    System.out.println("\tAbsolute : " + path.makeAbsolute().toOSString());

    switch (kind) {
      case IClasspathEntry.CPE_LIBRARY:
        path = classPathEntry.getPath();
        boolean inProject = currentContainer.getFullPath().makeAbsolute().isPrefixOf(
                path.makeAbsolute());

        System.out.println("\tProject (full absolute): "
                + currentContainer.getFullPath().makeAbsolute());
        System.out.println(inProject ? "\tinProject" : "Not in Project");

        String temp = "";
        if (inProject) {
          temp = "$main_root/" + path.makeRelative().removeFirstSegments(1).toOSString();
          temp = temp.trim();
          temp = temp.replace('\\', '/').trim();
          if (!contain(classPath, temp)) {
            classPath.add(temp);
            System.out.println("\tadded to ClassPath: " + temp);
          }
        } else {
          temp = path.makeAbsolute().toOSString();
          temp = temp.trim();
          // MissingLibraries.add(temp);

          // if in another project in the workspace
          // -> warn : warnAboutMissingLibraries = true;
          // if external in file system
          // if non jre and non uima -> warn: warnAboutMissingLibraries = true;
        }

        break;
      default:
        System.out.println("\t**Non-CPE_LIBRARY class path entry: " + path.toOSString());
        // MissingLibraries.add("Dependency on: " + path.makeAbsolute().toOSString().trim());
    }

    return classPath;
  }

  /*
   * Returns true if the ArrayList of Strings contain the given string (not necessarly the same
   * object)
   */
  private boolean contain(ArrayList classPath, String classPathEntryString) {
    boolean contain = false;

    if (classPathEntryString != null) {
      Iterator itr = classPath.iterator();
      while (itr.hasNext()) {
        if (classPathEntryString.equalsIgnoreCase((String) itr.next())) {
          contain = true;
          break;
        }
      }
    }
    return contain;
  }

  /**
   * Ensures that all required field are set.
   */
  private void dialogChanged() {
    gr.setEnabled(true);
    osCombo.setEnabled(true);
    jdkVersionCombo.setEnabled(true);
    gr1.setEnabled(true);
    viewer.table.setEnabled(true);
    viewer.add.setEnabled(true);
    viewer.delete.setEnabled(true);
    updateStatus(null);
  }

  private void updateStatus(String message) {
    setErrorMessage(message);
    setPageComplete(message == null);
  }

}