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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.apache.uima.ep_launcher.LauncherConstants.InputFormat;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
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

  @Override
  public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
    
    // Build the command line which is passed to the launched processed, in case the
    // parameters are not configured correctly (can only happen trough bugs in this plugin) then 
    // the launched process will fail
    
    StringBuilder cmdline = new StringBuilder();
    cmdline.append(RemoteLauncher.DESCRIPTOR_PARAM + " ");
    String descriptorPath = configuration.getAttribute(LauncherConstants.ATTR_DESCRIPTOR_NAME, "");
    IResource descriptor = ResourcesPlugin.getWorkspace().getRoot().findMember(descriptorPath);
    cmdline.append(descriptor.getLocation().toOSString() + " ");
    cmdline.append(RemoteLauncher.INPUT_RESOURCE_PARAM + " ");
    
    String inputResourcePath = configuration.getAttribute(LauncherConstants.ATTR_INPUT_NAME, "");
    IResource inputResource = ResourcesPlugin.getWorkspace().getRoot().findMember(inputResourcePath);
    cmdline.append(inputResource.getLocation().toOSString() + " ");
    
    String formatName = configuration.getAttribute(LauncherConstants.ATTR_INPUT_FORMAT_NAME,
           " "); 
    cmdline.append(RemoteLauncher.INPUT_FORMAT_PARAM + " ");
    cmdline.append(formatName + " ");
    
    // if format equals PLAIN_TEXT
    if (InputFormat.PLAIN_TEXT.toString().equals(formatName)) {
      cmdline.append(RemoteLauncher.INPUT_ENCODING_PARAM + " ");
      cmdline.append(configuration.getAttribute(LauncherConstants.ATTR_INPUT_ENCODING_NAME, "") 
              + " ");
    }
    
    cmdline.append(RemoteLauncher.INPUT_RECURSIVE_PARAM + " ");
    cmdline.append(configuration.getAttribute(LauncherConstants.ATTR_INPUT_RECURSIVELY_NAME, false) + " ");
    cmdline.append(RemoteLauncher.OUTPUT_FOLDER_PARAM + " ");
    String outputFolderPath = configuration.getAttribute(LauncherConstants.ATTR_OUTPUT_FOLDER_NAME, "");
    IResource outputFolder = ResourcesPlugin.getWorkspace().getRoot().findMember(outputFolderPath);
    cmdline.append(outputFolder.getLocation().toOSString() + " ");
    cmdline.append(RemoteLauncher.OUTPUT_CLEAR_PARAM + " ");
    cmdline.append(configuration.getAttribute(LauncherConstants.ATTR_OUTPUT_CLEAR_NAME, false));
    
    return cmdline.toString();
  }

  
  @Override
  public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
    
    List<String> extendedClasspath = new ArrayList<String>();
    Collections.addAll(extendedClasspath, super.getClasspath(configuration));
    
    // Normal mode, add the launcher plugin and uima runtime jar to the classpath
    if (!Platform.inDevelopmentMode()) {
      try {
        // Add this plugin jar to the classpath 
        extendedClasspath.add(pluginIdToJarPath(LauncherPlugin.ID));
        
        // UIMA jar should be added the end of the class path, because user uima jars
        // (maybe a different version) should appear first on the class path
        extendedClasspath.add(pluginIdToJarPath("org.apache.uima.runtime"));
      } catch (IOException e) {
        throw new CoreException(new Status(IStatus.ERROR, LauncherPlugin.ID, IStatus.OK, 
                "Failed to compose classpath!", e));
      }
    }
    // When running inside eclipse with PDE in development mode the plugins
    // are not installed inform of jar files and the classes must be loaded
    // from the target/classes folder or target/org.apache.uima.runtime.*.jar file
    else {
      try {
        // Add classes folder of this plugin to class path
        extendedClasspath.add(pluginIdToJarPath(LauncherPlugin.ID) + "target/classes");
        
        // Add org.apache.uima.runtime jar to class path
        Bundle bundle = LauncherPlugin.getDefault().getBundle("org.apache.uima.runtime");
        
        // Ignore the case when runtime bundle does not exist ...
        if (bundle != null) {
          Enumeration<?> jarEnum = bundle.findEntries("/", "*.jar", true);
          while (jarEnum != null && jarEnum.hasMoreElements()) {
            URL element = (URL) jarEnum.nextElement();
            extendedClasspath.add(FileLocator.toFileURL(element).getFile());
          }
        }
      } catch (IOException e) {
        throw new CoreException(new Status(IStatus.ERROR, LauncherPlugin.ID, IStatus.OK, 
                "Failed to compose classpath!", e));
      }
    }
    
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
    
    // It is assumed that the working directory is the project directory,
    // otherwise the refresh will not occur
    File workingDir = getWorkingDirectory(configuration);
    IResource result = ResourcesPlugin.getWorkspace().getRoot().findMember(workingDir.getName());
    if (result != null)
        result.refreshLocal(IResource.DEPTH_INFINITE, null);
  }
}
