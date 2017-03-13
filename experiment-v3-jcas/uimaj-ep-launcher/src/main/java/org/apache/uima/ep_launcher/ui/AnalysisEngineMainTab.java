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

package org.apache.uima.ep_launcher.ui;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.apache.uima.ep_launcher.LauncherConstants;
import org.apache.uima.ep_launcher.LauncherConstants.InputFormat;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

/**
 * The Analysis Engine Main Tab is responsible to display/edit
 * the UIMA related settings, such as project, descriptor file,
 * input folders, etc. 
 */
// TODO: Add an icon for the main tab
public class AnalysisEngineMainTab extends JavaLaunchTab {

  private Text projectText;
  
  private Text descriptorText;
  
  private Text inputText;
  private Button recursivelyButton;
  
  private Button casButton;
  private Button plainTextButton;
  private Combo encodingCombo;
  private Text languageText;
  
  private Text outputFolderText;
  private Button clearFolderButton;
  
  private IWorkspaceRoot getWorkspaceRoot() {
    return ResourcesPlugin.getWorkspace().getRoot();
  }
  
  private IProject getSelectedProject() {
    IResource project = ResourcesPlugin.getWorkspace().getRoot().findMember(projectText.getText());
    if (project instanceof IProject) {
      return (IProject)  project;
    }
    
    return null;
  }
  
  private IContainer getContainer(String path) {
	Path containerPath = new Path(path);
	IResource resource =  getWorkspaceRoot().findMember(containerPath);
	if (resource instanceof IContainer)
	  return (IContainer) resource;
    
    return null;
  }
  
  public void createControl(Composite composite) {
    
    Composite projectComposite = new Composite(composite, SWT.NONE);
    GridLayout projectGridLayout = new GridLayout();
    projectGridLayout.numColumns = 1;
    projectGridLayout.horizontalSpacing = SWT.FILL;
    projectComposite.setLayout(projectGridLayout);
    
    // Project Group
    Group projectGroup = new Group(projectComposite, SWT.None);
    projectGroup.setText("Project:");
    
    GridData projectGroupData = new GridData();
    projectGroupData.grabExcessHorizontalSpace = true;
    projectGroupData.horizontalAlignment = SWT.FILL;
    projectGroup.setLayoutData(projectGroupData);
    
    GridLayout projectGroupLayout = new GridLayout(2, false);
    projectGroup.setLayout(projectGroupLayout);
    
    projectText = new Text(projectGroup, SWT.BORDER);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).
            grab(true, false).applyTo(projectText);
    projectText.addModifyListener(new ModifyListener() {
      
      public void modifyText(ModifyEvent event) {
        updateLaunchConfigurationDialog();
      }
    });
    
    Button browseProject = new Button(projectGroup, SWT.NONE);
    browseProject.setText("Browse ...");
    browseProject.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        ILabelProvider labelProvider = new WorkbenchLabelProvider();

        ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(),
                labelProvider);
        dialog.setTitle("Project Selection");
        dialog.setMessage("Select a project");
        dialog.setElements(getWorkspaceRoot().getProjects());
        IProject project = getSelectedProject();
        if (project != null) {
          dialog.setInitialSelections(new Object[] { project });
        }
        
        if (dialog.open() == Window.OK) {
          IProject selectedProject = (IProject) dialog.getFirstResult();
          projectText.setText(selectedProject.getName());
        }
      }
    });
    
    // Descriptor Group
    Group descriptorGroup = new Group(projectComposite, SWT.None);
    descriptorGroup.setText("Descriptor:");
    
    GridData descriptorGroupData = new GridData();
    descriptorGroupData.grabExcessHorizontalSpace = true;
    descriptorGroupData.horizontalAlignment = SWT.FILL;
    descriptorGroup.setLayoutData(projectGroupData);
    
    GridLayout descriptorGroupLayout = new GridLayout(2, false);
    descriptorGroup.setLayout(descriptorGroupLayout);
    
    descriptorText = new Text(descriptorGroup, SWT.BORDER);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).
            grab(true, false).applyTo(descriptorText);
    descriptorText.addModifyListener(new ModifyListener() {
      
      public void modifyText(ModifyEvent event) {
        updateLaunchConfigurationDialog();
      }
    });
    Button browseDescriptor = new Button(descriptorGroup, SWT.NONE);
    browseDescriptor.setText("Browse ...");
    browseDescriptor.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(),
                new WorkbenchLabelProvider(), new WorkbenchContentProvider());
        dialog.setTitle("Select descriptor");
        dialog.setMessage("Select descriptor");
        dialog.setInput(getWorkspaceRoot());
        dialog.setInitialSelection(getWorkspaceRoot().findMember(descriptorText.getText()));
        if (dialog.open() == IDialogConstants.OK_ID) {
          IResource resource = (IResource) dialog.getFirstResult();
          if (resource != null) {
            String fileLoc = resource.getFullPath().toString();
            descriptorText.setText(fileLoc);
          }
        }
      }
    });
    
    // Input Resource Group
    Group inputResourceGroup = new Group(projectComposite, SWT.None);
    inputResourceGroup.setText("Input Resource:");
    
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).
            grab(true, false).applyTo(inputResourceGroup);
    
    GridLayout inputResourceGroupLayout = new GridLayout(2, false);
    inputResourceGroup.setLayout(inputResourceGroupLayout);
    
    inputText = new Text(inputResourceGroup, SWT.BORDER);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).
            grab(true, false).applyTo(inputText);
    inputText.addModifyListener(new ModifyListener() {
      
      public void modifyText(ModifyEvent event) {
        updateLaunchConfigurationDialog();
      }
    });
    
    Button browseInputResource = new Button(inputResourceGroup, SWT.NONE);
    browseInputResource.setText("Browse ...");
    browseInputResource.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(),
                new WorkbenchLabelProvider(), new WorkbenchContentProvider());
        dialog.setTitle("Select input folder or file");
        dialog.setMessage("Select input folder or file");
        dialog.setInput(getSelectedProject());
        dialog.setInitialSelection(getWorkspaceRoot().findMember(inputText.getText()));
        if (dialog.open() == IDialogConstants.OK_ID) {
          IResource resource = (IResource) dialog.getFirstResult();
          if (resource != null) {
            String fileLoc = resource.getFullPath().toString();
            inputText.setText(fileLoc);
          }
        }
      }
    });
    
    recursivelyButton = new Button(inputResourceGroup, SWT.CHECK);
    recursivelyButton.setText("Recursively, read all files under each directory");
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).
            grab(true, false).applyTo(recursivelyButton);
    recursivelyButton.addSelectionListener(new SelectionListener() {
      
      public void widgetSelected(SelectionEvent event) {
        updateLaunchConfigurationDialog();
      }
      
      public void widgetDefaultSelected(SelectionEvent event) {
      }
    });
    
    Group inputFormatGroup = new Group(projectComposite, SWT.None);
    inputFormatGroup.setText("Input Format:");
    
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).
            grab(true, false).applyTo(inputFormatGroup);
    
    GridLayout inputFormatGroupLayout = new GridLayout(4, false);
    inputFormatGroup.setLayout(inputFormatGroupLayout);
    
    casButton = new Button(inputFormatGroup, SWT.RADIO);
    casButton.setText("CASes (XMI or XCAS format)");
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).
            grab(true, false).span(4, 1).applyTo(casButton);
    casButton.addSelectionListener(new SelectionListener() {
      
      public void widgetSelected(SelectionEvent event) {
        updateLaunchConfigurationDialog();
      }
      
      public void widgetDefaultSelected(SelectionEvent event) {
      }
    });
    
    plainTextButton = new Button(inputFormatGroup, SWT.RADIO);
    GridDataFactory.swtDefaults().align(SWT.LEFT, SWT.CENTER).
            grab(false, false).applyTo(plainTextButton);
    plainTextButton.addSelectionListener(new SelectionListener() {
      
      public void widgetSelected(SelectionEvent event) {
        encodingCombo.setEnabled(plainTextButton.getSelection());
        languageText.setEnabled(plainTextButton.getSelection());
        updateLaunchConfigurationDialog();
      }
      
      public void widgetDefaultSelected(SelectionEvent event) {
      }
    });
    plainTextButton.setText("Plain Text, encoding:");
    
    encodingCombo = new Combo(inputFormatGroup, SWT.NONE);
    GridDataFactory.swtDefaults().align(SWT.LEFT, SWT.CENTER).
        grab(false, false).applyTo(encodingCombo);
    
    encodingCombo.addModifyListener(new ModifyListener() {
      
      public void modifyText(ModifyEvent event) {
        updateLaunchConfigurationDialog();
      }
    });
  
    String defaultEncoding = Charset.defaultCharset().displayName();
    
    Set<String> charsets = new HashSet<String>();
    charsets.add("US-ASCII");
    charsets.add("ISO-8859-1");
    charsets.add("UTF-8");
    charsets.add("UTF-16BE");
    charsets.add("UTF-16LE");
    charsets.add("UTF-16");
    charsets.add(defaultEncoding);
    
    encodingCombo.setItems(charsets.toArray(new String[charsets.size()]));
    // Will be enabled by initializeForm if format is plain text
    encodingCombo.setEnabled(false);
    
    // Add language label
    Label languageLabel = new Label(inputFormatGroup, SWT.NONE);
    languageLabel.setText("Language:");
    
    // Add language text field
   languageText = new Text(inputFormatGroup, SWT.BORDER);
   GridDataFactory.swtDefaults().hint(250, SWT.DEFAULT).align(SWT.LEFT, SWT.CENTER).
            grab(true, false).applyTo(languageText);
    
   languageText.addModifyListener(new ModifyListener() {
     
     public void modifyText(ModifyEvent event) {
       updateLaunchConfigurationDialog();
     }
   });
   
    // Output Folder
    Group outputFolderGroup = new Group(projectComposite, SWT.None);
    outputFolderGroup.setText("Output Folder:");
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).
            grab(true, false).applyTo(outputFolderGroup);
    GridLayout outputFolderGroupLayout = new GridLayout(2, false);
    outputFolderGroup.setLayout(outputFolderGroupLayout);
    outputFolderText = new Text(outputFolderGroup, SWT.BORDER);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).
            grab(true, false).applyTo(outputFolderText);
    outputFolderText.addModifyListener(new ModifyListener() {
      
      public void modifyText(ModifyEvent event) {
        updateLaunchConfigurationDialog();
      }
    });
    
    Button browseOutputFolderButton = new Button(outputFolderGroup, SWT.NONE);
    browseOutputFolderButton.setText("Browse ...");
    browseOutputFolderButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
     // TODO: Only select elements within project
        String currentContainerString = outputFolderText.getText();
        IContainer currentContainer = getContainer(currentContainerString);
        ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(),
                currentContainer, false, "Select output folder");
        dialog.showClosedProjects(false);
        dialog.open();
        Object[] results = dialog.getResult();
        if ((results != null) && (results.length > 0) && (results[0] instanceof IPath)) {
          IPath path = (IPath) results[0];
          String containerName = path.toOSString();
          outputFolderText.setText(containerName);
        }
      }
    });
    
    clearFolderButton = new Button(outputFolderGroup, SWT.CHECK);
    clearFolderButton.setText("Clear the output folder");
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).
            grab(true, false).applyTo(clearFolderButton);
    clearFolderButton.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent event) {
        updateLaunchConfigurationDialog();
      }
      
      public void widgetDefaultSelected(SelectionEvent event) {
      }
    });
    setControl(projectComposite);
  }

  public String getName() {
    return "Main";
  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig) {
    
    setErrorMessage(null);
    
    
    // Project must be set, check that project does exist
    String projectName = projectText.getText();
    
    IResource projectResource = ResourcesPlugin.getWorkspace().getRoot().findMember(projectName);
    if (!(projectResource instanceof IProject)) {
      setErrorMessage("Project does not exist!");
      return false;
    }
    
    // Descriptor must be set and valid file
    IResource descriptorResource = getWorkspaceRoot().findMember(descriptorText.getText());
    if (!(descriptorResource instanceof IFile)) {
      setErrorMessage("Descriptor must be an existing file!");
      return false;
    }
    
    // Input folder or file must be set
    IResource inputResource = getWorkspaceRoot().findMember(inputText.getText());
    if (inputResource == null) {
      setErrorMessage("Input resource must be an existing file or folder!");
      return false;
    }
    
    // Validate the input encoding
    if (plainTextButton.getSelection()) {
      String inptuEncoding = encodingCombo.getText();
      
      boolean isEncodingValid;
      try {
        isEncodingValid = Charset.isSupported(inptuEncoding);
      }
      catch (IllegalCharsetNameException e) {
        isEncodingValid = false;
      }
      
      if (!isEncodingValid) {
        setErrorMessage("Invalid input format encoding!");
        return false;
      }
    }
    
    // Validate output folder
    if (outputFolderText.getText().length() > 0) {
      IResource outputResource = getWorkspaceRoot().findMember(outputFolderText.getText());
      if (!(outputResource instanceof IFolder)) {
        setErrorMessage("The output folder must be a valid folder or not be set!");
        return false;
      }
    }
        
    return super.isValid(launchConfig);
  }
  
  public void performApply(ILaunchConfigurationWorkingCopy config) {
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
            (String) projectText.getText());
    
    config.setAttribute(LauncherConstants.ATTR_DESCRIPTOR_NAME, descriptorText.getText());
    
    config.setAttribute(LauncherConstants.ATTR_INPUT_NAME, inputText.getText());
    config.setAttribute(LauncherConstants.ATTR_INPUT_RECURSIVELY_NAME, 
            Boolean.valueOf(recursivelyButton.getSelection()));
    
    String formatName;
    if (casButton.getSelection()) {
      formatName = InputFormat.CAS.toString();
    }
    else if (plainTextButton.getSelection()) {
      formatName = InputFormat.PLAIN_TEXT.toString();
    }
    else {
      throw new IllegalStateException("One button must always be selected!");
    }

    config.setAttribute(LauncherConstants.ATTR_INPUT_FORMAT_NAME, formatName);
    
    config.setAttribute(LauncherConstants.ATTR_INPUT_ENCODING_NAME, encodingCombo.getText());
    
    config.setAttribute(LauncherConstants.ATTR_INPUT_LANGUAGE_NAME, languageText.getText());
    
    config.setAttribute(LauncherConstants.ATTR_OUTPUT_FOLDER_NAME, outputFolderText.getText());
    config.setAttribute(LauncherConstants.ATTR_OUTPUT_CLEAR_NAME,
            Boolean.valueOf(clearFolderButton.getSelection()));
  }

  public void setDefaults(ILaunchConfigurationWorkingCopy config) {
    config.setAttribute(LauncherConstants.ATTR_INPUT_RECURSIVELY_NAME, false);
    config.setAttribute(LauncherConstants.ATTR_INPUT_FORMAT_NAME, InputFormat.CAS.toString());
    config.setAttribute(LauncherConstants.ATTR_INPUT_LANGUAGE_NAME, "x-unspecified");
    config.setAttribute(LauncherConstants.ATTR_OUTPUT_CLEAR_NAME, false);
  }
  
  @Override
  public void initializeFrom(ILaunchConfiguration config) {
    
    // TODO: Log errors if reading fails?
    
    // write values to launch configuration ...
    try {
      projectText.setText(config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""));
    } catch (CoreException e) {
      projectText.setText("");
    }
    
    try {
      descriptorText.setText(config.getAttribute(LauncherConstants.ATTR_DESCRIPTOR_NAME, ""));
    } catch (CoreException e) {
      descriptorText.setText("");
    }
    
    try {
      inputText.setText(config.getAttribute(LauncherConstants.ATTR_INPUT_NAME, ""));
    } catch (CoreException e) {
      inputText.setText("");
    }
    
    // recursive button
    try {
      recursivelyButton.setSelection((Boolean) config.getAttribute(
              LauncherConstants.ATTR_INPUT_RECURSIVELY_NAME, false));
    } catch (CoreException e) {
      recursivelyButton.setSelection(false);
    }
    
    // Format buttons
    String formatName;
    try {
      formatName = config.getAttribute(LauncherConstants.ATTR_INPUT_FORMAT_NAME, InputFormat.CAS.toString());
    } catch (CoreException e) {
      formatName = InputFormat.CAS.toString();
    }
    
    if (InputFormat.CAS.toString().equals(formatName)) {
      casButton.setSelection(true);
    }
    else if (InputFormat.PLAIN_TEXT.toString().equals(formatName)) {
      plainTextButton.setSelection(true);
      encodingCombo.setEnabled(true);
      languageText.setEnabled(true);
      
      String language;
      try {
        language = config.getAttribute(LauncherConstants.ATTR_INPUT_LANGUAGE_NAME, "x-unspecified");
      } catch (CoreException e) {
        language = "x-unspecified";
      }
      
      languageText.setText(language);
    }
    
    // Always remember the input encoding, even so plain text is not selected,
    // it might be convenient for the user
    String inputEncoding = Charset.defaultCharset().displayName();
    try {
      inputEncoding = config.getAttribute(LauncherConstants.ATTR_INPUT_ENCODING_NAME, inputEncoding);
    } catch (CoreException e) {
    }
    
    encodingCombo.setText(inputEncoding);
    
    // output folder
    try {
      outputFolderText.setText(config.getAttribute(LauncherConstants.ATTR_OUTPUT_FOLDER_NAME, ""));
    } catch (CoreException e) {
      outputFolderText.setText("");
    }
    
    // clear folder button
    try {
      clearFolderButton.setSelection((Boolean) config.getAttribute(
              LauncherConstants.ATTR_OUTPUT_CLEAR_NAME, false));
    } catch (CoreException e) {
      clearFolderButton.setSelection(false);
    }
    
    super.initializeFrom(config);
  }
}
