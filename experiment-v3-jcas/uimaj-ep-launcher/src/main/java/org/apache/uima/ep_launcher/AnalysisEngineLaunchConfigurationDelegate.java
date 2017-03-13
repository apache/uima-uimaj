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

package org.apache.uima.ep_launcher;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.apache.uima.ep_launcher.LauncherConstants.InputFormat;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.osgi.framework.Bundle;

/**
 * This delegate is responsible to to configure the VM and to create command line args which will be 
 * passed to the {@link RemoteLauncher}s main method.
 */
public class AnalysisEngineLaunchConfigurationDelegate extends JavaLaunchDelegate {

  @Override
  public String getMainTypeName(ILaunchConfiguration configuration) throws CoreException {
    return "org.apache.uima.ep_launcher.RemoteLauncher";
  }

  private String pluginIdToJarPath(String pluginId) throws IOException {
    Bundle bundle = LauncherPlugin.getDefault().getBundle(pluginId);

    URL url = bundle.getEntry("/");

    if (url == null)
      throw new IOException();

    return FileLocator.toFileURL(url).getFile();
  }

  private static void ensureResourceExists(IResource resource, String resourceName)
          throws CoreException {
    if (resource == null)
      throw new CoreException(new Status(IStatus.ERROR, LauncherPlugin.ID, "The " + resourceName
              + " does not exist!"));
  }
  
  @Override
  public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
    
    // Build the command line which is passed to the launched processed, in case the
    // parameters are not configured correctly (can only happen trough bugs in this plugin) then 
    // the launched process will fail
    
    StringBuilder cmdline = new StringBuilder();
    cmdline.append(RemoteLauncher.DESCRIPTOR_PARAM + " ");
    String descriptorPath = configuration.getAttribute(LauncherConstants.ATTR_DESCRIPTOR_NAME, "");
    IResource descriptor = ResourcesPlugin.getWorkspace().getRoot().findMember(descriptorPath);
    ensureResourceExists(descriptor, "Analysis Engine Descritpor");
    cmdline.append(descriptor.getLocation().toOSString() + " ");
    cmdline.append(RemoteLauncher.INPUT_RESOURCE_PARAM + " ");
    
    String inputResourcePath = configuration.getAttribute(LauncherConstants.ATTR_INPUT_NAME, "");
    IResource inputResource = ResourcesPlugin.getWorkspace().getRoot().findMember(inputResourcePath);
    ensureResourceExists(inputResource, "Input Resource");
    cmdline.append(inputResource.getLocation().toOSString() + " ");
    
    String formatName = configuration.getAttribute(LauncherConstants.ATTR_INPUT_FORMAT_NAME, " "); 
    cmdline.append(RemoteLauncher.INPUT_FORMAT_PARAM + " ");
    cmdline.append(formatName + " ");
    
    // if format equals PLAIN_TEXT
    if (InputFormat.PLAIN_TEXT.toString().equals(formatName)) {
      cmdline.append(RemoteLauncher.INPUT_ENCODING_PARAM + " ");
      cmdline.append(configuration.getAttribute(LauncherConstants.ATTR_INPUT_ENCODING_NAME, "") 
              + " ");
      
      cmdline.append(RemoteLauncher.INPUT_LANGUAGE_PARAM + " ");
      cmdline.append(configuration.getAttribute(LauncherConstants.ATTR_INPUT_LANGUAGE_NAME, "") + " "); 
      
    }
    
    cmdline.append(RemoteLauncher.INPUT_RECURSIVE_PARAM + " ");
    cmdline.append(configuration.getAttribute(LauncherConstants.ATTR_INPUT_RECURSIVELY_NAME, false) + " ");
    
    String outputFolderPath = configuration.getAttribute(LauncherConstants.ATTR_OUTPUT_FOLDER_NAME, "");
    // zero length string means that is is not set
    if (outputFolderPath.length() != 0) {
      IResource outputFolder = ResourcesPlugin.getWorkspace().getRoot().findMember(outputFolderPath);
      
      ensureResourceExists(outputFolder, "Output Folder");
      
		  cmdline.append(RemoteLauncher.OUTPUT_FOLDER_PARAM + " ");
		  cmdline.append(outputFolder.getLocation().toOSString() + " ");
		  
		  // Do not delete the output folder if it is the Workspace Root or a Project
		  // It should not be possible to set it to one of both, but in case something goes wrong
		  // it should be double checked
		  if (!(outputFolder instanceof IWorkspaceRoot || outputFolder instanceof IProject)) {
			  cmdline.append(RemoteLauncher.OUTPUT_CLEAR_PARAM + " ");
			  cmdline.append(configuration.getAttribute(LauncherConstants.ATTR_OUTPUT_CLEAR_NAME, false));
		  }
    }

    String pgmArgs = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, "");
    if (pgmArgs != null) {
      cmdline.append(' ').append(pgmArgs);
    }

    return cmdline.toString();
  }

  
  /**
   * Adds the launcher and uima core jar to the class path,
   * depending on normal mode or PDE development mode.
   */
  @Override
  public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
    
    // The class path already contains the jars which are specified in the Classpath tab
    
    List<String> extendedClasspath = new ArrayList<String>();
    Collections.addAll(extendedClasspath, super.getClasspath(configuration));
    
    // Normal mode, add the launcher plugin and uima runtime jar to the classpath
    try {
      if (!Platform.inDevelopmentMode()) {     
        // Add this plugin jar to the classpath 
        extendedClasspath.add(pluginIdToJarPath(LauncherPlugin.ID)); }
      else {
        // When running inside eclipse with PDE in development mode the plugins
        // are not installed inform of jar files and the classes must be loaded
        // from the target/classes folder or target/org.apache.uima.runtime.*.jar file
        extendedClasspath.add(pluginIdToJarPath(LauncherPlugin.ID) + "target/classes");
      }
      // UIMA jar should be added the end of the class path, because user uima jars
      // (maybe a different version) should appear first on the class path

      // Add org.apache.uima.runtime jar to class path
      Bundle bundle = LauncherPlugin.getDefault().getBundle("org.apache.uima.runtime");
      
      // Ignore the case when runtime bundle does not exist ...
      if (bundle != null) {
        // find entries: starting point, pattern, whether or not to recurse
        //   all the embedded jars are at the top level, no recursion needed
        //   All the jars are not needed - only the uimaj core one
        //     any other jars will be provided by the launching project's class path
        //     uimaj-core provided because the launcher itself needs uimaj-core classes
        //  Found empirically that recursion is need to find the jar in development mode
        Enumeration<?> jarEnum = bundle.findEntries("/", "uimaj-core*.jar", Platform.inDevelopmentMode());
        while (jarEnum != null && jarEnum.hasMoreElements()) {
          URL element = (URL) jarEnum.nextElement();
          extendedClasspath.add(FileLocator.toFileURL(element).getFile());
        }
      }        
      // adds things like the top level metainf info, 
      // probably not required in most cases
      extendedClasspath.add(pluginIdToJarPath("org.apache.uima.runtime"));
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, LauncherPlugin.ID, IStatus.OK, 
              "Failed to compose classpath!", e));
    }
    
    // Dump classpath
//    for (String cp : extendedClasspath) {
//      System.out.println("Uima Launcher CP entry: " + cp);
//    }
    return extendedClasspath.toArray(new String[extendedClasspath.size()]);
  }
  
  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
          IProgressMonitor monitor) throws CoreException {
    
    super.launch(configuration, mode, launch, monitor);
    
    // This method is called from a worker thread, so it seems 
    // safe to block this tread until the VM terminates
    while (!launch.isTerminated()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.interrupted();
      }
    }
    
    String outputFolderPath = configuration.getAttribute(LauncherConstants.ATTR_OUTPUT_FOLDER_NAME, "");
    // zero length string means that is is not set
    if (outputFolderPath.length() != 0) {
      // If the output directory is set and inside the workspace it will be refreshed
      IResource result = ResourcesPlugin.getWorkspace().getRoot().findMember(outputFolderPath);
      
      if (result != null)
          result.refreshLocal(IResource.DEPTH_INFINITE, null);
    }
  }
}
