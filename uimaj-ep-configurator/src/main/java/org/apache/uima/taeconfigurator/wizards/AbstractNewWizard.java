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

package org.apache.uima.taeconfigurator.wizards;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Create a new file resource in the provided container. If the container resource (a folder or a
 * project) is selected in the workspace when the wizard is opened, it will accept it as the target
 * container. If a sample multi-page editor is registered for the same extension, it will be able to
 * open it.
 * 
 * Following Eclipse conventions, the new Wizard will actually create the resource in the file
 * system and in the Eclipse resource space, with initial contents, and then open the resource with
 * the CDE.
 */

public abstract class AbstractNewWizard extends Wizard implements INewWizard {

  // common parts of empty xml descriptors for Wizards to use
  // 0 = name of component (e.g. type name, type priority name, ae descriptor name)
  // 1 = parts at end of partial descriptor
  // 2 = outer descriptor name
  // 3 = metadata element name
  // 4 = implname element name (implementationName or annotatorImplementationName
  // 5 = "<primitive>true</primitive>\n"

  /** The Constant XMLNS_PART. */
  // for explanation of this strange code, see JDK bug 6447475 found by findbugs
  public static final String XMLNS_PART;
  static {
    XMLNS_PART = "xmlns=\"http://uima.apache.org/resourceSpecifier\"";
  }

  /** The Constant COMMON_HEADER. */
  public static final String COMMON_HEADER;
  static {
    COMMON_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "<{2} " + XMLNS_PART + ">\n";
  }

  /** The Constant COMMON_NDVV. */
  public static final String COMMON_NDVV;
  static {
    COMMON_NDVV = "    <name>{0}</name>\n" // 1 = name of component (e.g. type name, type priority
                                           // name, ae descriptor name)
            + "    <description></description>\n" + "    <version>1.0</version>\n"
            + "    <vendor></vendor>\n";
  }

  /** The Constant COMMON_FULL_DESCRIPTOR. */
  public static final String COMMON_FULL_DESCRIPTOR;
  static {
    COMMON_FULL_DESCRIPTOR = COMMON_HEADER
            + "  <frameworkImplementation>org.apache.uima.java</frameworkImplementation>\n" + "{5}" // 5
                                                                                                    // =
                                                                                                    // ""
                                                                                                    // or
                                                                                                    // "<primitive>true</primitive>\n"
            + "  <{4}></{4}>\n" // 4 = implname element name (implementationName or
                                // annotatorImplementationName
            + "  <{3}>\n" // 3 = metadata element name
            + COMMON_NDVV + "    <configurationParameters></configurationParameters>\n"
            + "    <configurationParameterSettings></configurationParameterSettings>\n"
            + "    <typeSystemDescription></typeSystemDescription>\n"
            + "    <typePriorities></typePriorities>\n"
            + "    <fsIndexCollection></fsIndexCollection>\n" + "    <capabilities>\n"
            + "      <capability>\n" + "        <inputs></inputs>\n"
            + "        <outputs></outputs>\n"
            + "        <languagesSupported></languagesSupported>\n" + "      </capability>\n"
            + "    </capabilities>\n" + "  </{3}>\n"
            + "  <externalResourceDependencies></externalResourceDependencies>\n"
            + "  <resourceManagerConfiguration></resourceManagerConfiguration>\n" + "</{2}>";
  }

  /** The common partial descriptor. */
  public static String COMMON_PARTIAL_DESCRIPTOR;
  static {
    COMMON_PARTIAL_DESCRIPTOR = COMMON_HEADER + COMMON_NDVV + "{1}" + "</{2}>\n";
  }

  /** The page. */
  protected AbstractNewWizardPage page;

  /** The selection. */
  protected ISelection selection;

  /** The window title. */
  private String windowTitle;

  /**
   * Instantiates a new abstract new wizard.
   *
   * @param windowTitle
   *          the window title
   */
  public AbstractNewWizard(String windowTitle) {
    setDialogSettings(TAEConfiguratorPlugin.getDefault().getDialogSettings());
    setNeedsProgressMonitor(true);
    setForcePreviousAndNextButtons(false);
    this.windowTitle = windowTitle;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.wizard.Wizard#performFinish()
   */
  /**
   * Called when 'Finish' button is pressed in the wizard. Create an operation and run it using
   * wizard as execution context.
   *
   * @return true, if successful
   */
  @Override
  public boolean performFinish() {
    final String containerName = page.getContainerName();
    final String fileName = page.getFileName();
    IRunnableWithProgress op = new IRunnableWithProgress() {
      @Override
      public void run(IProgressMonitor monitor) throws InvocationTargetException {
        try {
          doFinish(containerName, fileName, monitor);
        } catch (CoreException e) {
          throw new InvocationTargetException(e);
        } finally {
          monitor.done();
        }
      }
    };
    try {
      getContainer().run(true, false, op);
    } catch (InterruptedException e) {
      return false;
    } catch (InvocationTargetException e) {
      Throwable realException = e.getTargetException();
      MessageDialog.openError(getShell(), "Error", realException.getMessage());
      return false;
    }
    return true;
  }

  /**
   * Gets the prototype descriptor.
   *
   * @param name
   *          the name
   * @return the prototype descriptor
   */
  public abstract String getPrototypeDescriptor(String name);

  /**
   * Do finish.
   *
   * @param containerName
   *          the container name
   * @param fileName
   *          the file name
   * @param monitor
   *          the monitor
   * @throws CoreException
   *           the core exception
   */
  void doFinish(String containerName, String fileName, IProgressMonitor monitor)
          throws CoreException {
    // create a sample file
    monitor.beginTask("Creating " + fileName, 2);
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IResource resource = root.findMember(new Path(containerName));
    if (!resource.exists() || !(resource instanceof IContainer)) {
      throwCoreException("Container \"" + containerName + "\" does not exist.");
    }
    IContainer container = (IContainer) resource;
    final IFile file = container.getFile(new Path(fileName));
    if (file.exists()) {
      throwCoreException("New Wizard creating file '" + fileName + "', but it already exists.");
    } else {
      int indexOfXml = file.getName().toLowerCase().indexOf(".xml");
      String name = (indexOfXml > 0) ? file.getName().substring(0, indexOfXml) : file.getName();
      String descriptor = getPrototypeDescriptor(name);

      PrintWriter printWriter = null;
      FileOutputStream fileOutputStream = null;
      InputStream stream = null;

      try {
        try {
          fileOutputStream = new FileOutputStream(file.getLocation().toOSString());
        } catch (FileNotFoundException e) {
          throw new InternalErrorCDE("unexpected Exception", e);
        }
        printWriter = new PrintWriter(fileOutputStream);
        printWriter.println(descriptor);
      } finally {
        if (null != printWriter)
          printWriter.close();
        if (null != fileOutputStream)
          try {
            fileOutputStream.close();
          } catch (IOException e1) {
          }
      }

      stream = new ByteArrayInputStream(descriptor.getBytes());
      file.create(stream, true, monitor);
      try {
        stream.close();
      } catch (IOException e1) {
      }
    }

    monitor.worked(1);
    monitor.setTaskName("Starting editor for new descriptor...");
    getShell().getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        IWorkbenchPage page1 = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
          page1.openEditor(new FileEditorInput(file), "taeconfigurator.editors.MultiPageEditor");
        } catch (PartInitException e) {
        }
      }
    });
    monitor.worked(2);
  }

  /**
   * Throw core exception.
   *
   * @param message
   *          the message
   * @throws CoreException
   *           the core exception
   */
  private void throwCoreException(String message) throws CoreException {
    IStatus status = new Status(IStatus.ERROR, "DescEditor", IStatus.OK, message, null);
    throw new CoreException(status);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
   * org.eclipse.jface.viewers.IStructuredSelection)
   */
  @Override
  public void init(IWorkbench workbench, IStructuredSelection pSelection) {
    selection = pSelection;
    setWindowTitle(windowTitle);
  }

}
