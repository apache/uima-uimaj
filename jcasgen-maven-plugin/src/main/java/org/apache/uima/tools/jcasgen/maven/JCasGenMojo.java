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
package org.apache.uima.tools.jcasgen.maven;

import static java.util.Arrays.asList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.Import_impl;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.tools.jcasgen.IError;
import org.apache.uima.tools.jcasgen.IProgressMonitor;
import org.apache.uima.tools.jcasgen.Jg;
import org.apache.uima.util.InvalidXMLException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.IOUtil;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.xml.sax.SAXException;

/**
 * Applies JCasGen to create Java files from XML type system descriptions.
 * 
 * Note that by default this runs at the process-resources phase because it requires the XML
 * descriptor files to already be at the appropriate places on the classpath, and the
 * generate-resources phase runs before resources are copied.
 */
@Mojo(name = "generate", defaultPhase = PROCESS_RESOURCES, requiresDependencyResolution = COMPILE, threadSafe = true)
public class JCasGenMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  @Component
  private BuildContext buildContext;

  /**
   * Type system descriptors to be included in JCas generation.
   */
  @Parameter(required = true)
  private String[] typeSystemIncludes;

  /**
   * Type system descriptors to be excluded in JCas generation.
   */
  @Parameter(required = false)
  private String[] typeSystemExcludes;

  /**
   * The directory where the generated sources will be written.
   */
  @Parameter(defaultValue = "${project.build.directory}/generated-sources/jcasgen", required = true)
  private File outputDirectory;

  /**
   * Generate JCas wrappers only for current project.
   */
  @Parameter(defaultValue = "false", required = true)
  private boolean limitToProject;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    // add the generated sources to the build
    if (!this.outputDirectory.exists()) {
      this.outputDirectory.mkdirs();
      this.buildContext.refresh(this.outputDirectory);
    }
    this.project.addCompileSourceRoot(this.outputDirectory.getPath());

    // assemble the classpath
    StringBuilder classpathBuilder = new StringBuilder();

    // Source roots
    for (String element : this.project.getCompileSourceRoots()) {
      if (classpathBuilder.length() > 0) {
        classpathBuilder.append(File.pathSeparatorChar);
      }
      classpathBuilder.append(element);
      getLog().debug("JCasGen: Adding source root to classpath '" + element + "'");
    }

    // Resource roots
    for (Resource element : this.project.getResources()) {
      if (classpathBuilder.length() > 0) {
        classpathBuilder.append(File.pathSeparatorChar);
      }
      classpathBuilder.append(element.getDirectory());
      getLog().debug("JCasGen: Adding resource root to classpath '" + element.getDirectory() + "'");
    }

    // Dependencies
    List<String> elements;
    try {
      elements = this.project.getCompileClasspathElements();
    } catch (DependencyResolutionRequiredException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }

    for (String element : elements) {
      if (classpathBuilder.length() > 0) {
        classpathBuilder.append(File.pathSeparatorChar);
      }
      classpathBuilder.append(element);
      getLog().debug("JCasGen: Adding dependency to classpath '" + element + "'");
    }
    String classpath = classpathBuilder.toString();

    // Locate the files to include
    DirectoryScanner ds = new DirectoryScanner();
    ds.setIncludes(typeSystemIncludes);
    ds.setExcludes(typeSystemExcludes);
    ds.setBasedir(project.getBasedir());
    ds.setCaseSensitive(true);
    getLog().debug("JCasGen: Scanning for descriptors in '" + ds.getBasedir() + "'");
    ds.scan();

    // Create a merged type system and check if any of the files has a delta
    TypeSystemDescription typeSystem = new TypeSystemDescription_impl();
    List<Import> imports = new ArrayList<>();
    boolean contextDelta = false;
    for (String descriptorLocation : ds.getIncludedFiles()) {
      File descriptorFile = new File(ds.getBasedir(), descriptorLocation);
      this.getLog().info("JCasGen: Found descriptor '" + descriptorFile.getAbsolutePath() + "'");
      Import imp = new Import_impl();
      // setLocation takes a string which must be a URL
      // https://issues.apache.org/jira/browse/UIMA-2983
      URL url;
      try {
        url = descriptorFile.toURI().toURL();
      } catch (MalformedURLException e) {
        throw new RuntimeException(e); // this should never happen for files
      }
      imp.setLocation(url.toString());
      imports.add(imp);

      contextDelta |= this.buildContext.hasDelta(new File(ds.getBasedir(), descriptorLocation));
    }
    Import[] importArray = new Import[imports.size()];
    typeSystem.setImports(imports.toArray(importArray));

    // Save type system to a file so we can pass it to the Jg
    // Do this before resolving the imports
    OutputStream typeSystemOs = null;
    File typeSystemFile;
    try {
      typeSystemFile = new File(project.getBuild().getDirectory(), "jcasgen/typesystem.xml");
      getLog().debug("JCasGen: Writing master descriptor to in '" + typeSystemFile + "'");
      typeSystemFile.getParentFile().mkdirs();
      typeSystemOs = new FileOutputStream(typeSystemFile);
      typeSystem.toXML(typeSystemOs);
    } catch (IOException e) {
      throw new MojoExecutionException(e.getMessage(), e.getCause());
    } catch (SAXException e) {
      throw new MojoExecutionException(e.getMessage(), e.getCause());
    } finally {
      IOUtil.close(typeSystemOs);
    }

    // skip JCasGen if there are no changes in the type system file or the files it
    // references hasDelta resolves the imports!
    if (!contextDelta && !this.hasDelta(typeSystem, classpath)) {
      this.getLog().info("JCasGen: Skipped, since no type system changes were detected");
      return;
    }

    List<String> args = new ArrayList<>();
    if (limitToProject) {
      File limitToDirectory = project.getBasedir().getAbsoluteFile();
      getLog().info("JCasGen: Limiting generation to descriptors in '" + limitToDirectory + "'");
      args.addAll(asList("-limitToDirectory", limitToDirectory.toString()));
    }
    args.addAll(asList("-jcasgeninput", typeSystemFile.getAbsolutePath(), "-jcasgenoutput",
            this.outputDirectory.getAbsolutePath(), "-jcasgenclasspath", classpath));

    // run JCasGen to generate the Java sources
    Jg jCasGen = new Jg();
    try {
      jCasGen.main0(args.toArray(new String[args.size()]), null, new JCasGenProgressMonitor(),
              new JCasGenErrors());
    } catch (JCasGenException e) {
      throw new MojoExecutionException(e.getMessage(), e.getCause());
    }

    // signal that the output directory has changed
    this.buildContext.refresh(this.outputDirectory);
  }

  private class JCasGenProgressMonitor implements IProgressMonitor {

    public JCasGenProgressMonitor() {
    }

    @Override
    public void done() {
    }

    @Override
    public void beginTask(String name, int totalWorked) {
    }

    @Override
    public void subTask(String message) {
      getLog().info("JCasGen: " + message);
    }

    @Override
    public void worked(int work) {
    }
  }

  private class JCasGenErrors implements IError {

    public JCasGenErrors() {
    }

    /*
     * called by the common JCasGen code when it detects an error If no exception, the "exception"
     * parameter is null
     */
    @Override
    public void newError(int severity, String message, Exception exception) {
      String fullMessage = "JCasGen: " + message;
      if (severity == IError.INFO) {
        getLog().info(fullMessage, exception);
      } else if (severity == IError.WARN) {
        getLog().warn(fullMessage, exception);
      } else if (severity == IError.ERROR) {
        String m = fullMessage;
        if (exception != null) {
          m = m + "\nException: " + exception.getMessage();
        }
        throw new JCasGenException(m, exception);
      } else {
        throw new UnsupportedOperationException("Unknown severity level: " + severity);
      }
    }
  }

  private static class JCasGenException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public JCasGenException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  private boolean hasDelta(TypeSystemDescription typeSystemDescription, String classpath) {
    // load the type system and resolve the imports using the classpath
    // TypeSystemDescription typeSystemDescription;
    try {
      // XMLInputSource in = new XMLInputSource(typeSystemFile.toURI().toURL());
      // typeSystemDescription = UIMAFramework.getXMLParser().parseTypeSystemDescription(in);
      ResourceManager resourceManager = UIMAFramework.newDefaultResourceManager();
      resourceManager.setExtensionClassPath(classpath, true);
      typeSystemDescription.resolveImports(resourceManager);
      // on any exception, the type system was invalid, so assume no files have changed
    } catch (InvalidXMLException e) {
      return false;
    } catch (MalformedURLException e) {
      return false;
    }
    // catch (IOException e) {
    // return false;
    // }

    File buildOutputDirectory = new File(this.project.getBuild().getOutputDirectory());

    // map each resource from its target location to its source location
    Map<File, File> targetToSource = new HashMap<>();
    for (Resource resource : this.project.getResources()) {
      File resourceDir = new File(resource.getDirectory());
      if (resourceDir.exists()) {

        // scan for the resource files
        List<String> includes = resource.getIncludes();
        if (includes.isEmpty()) {
          includes = Arrays.asList("**");
        }
        List<String> excludes = resource.getExcludes();
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(resourceDir);
        scanner.setIncludes(includes.toArray(new String[includes.size()]));
        scanner.setExcludes(excludes.toArray(new String[excludes.size()]));
        scanner.scan();

        // map each of the resources from its target location to its source location
        String targetPath = resource.getTargetPath();
        for (String filePath : scanner.getIncludedFiles()) {
          File sourceFile = new File(resourceDir, filePath);
          File baseDirectory = targetPath != null ? new File(buildOutputDirectory, targetPath)
                  : buildOutputDirectory;
          File targetFile = new File(baseDirectory, filePath);
          targetToSource.put(targetFile, sourceFile);
        }
      }
    }

    // search through the type system description for source files that have changed
    for (TypeDescription type : typeSystemDescription.getTypes()) {
      URL typeSystemURL = type.getSourceUrl();
      if (typeSystemURL != null) {
        File targetFile;
        try {
          targetFile = new File(typeSystemURL.toURI());
          // for any type system source that is not a File, assume it has not changed
        } catch (URISyntaxException e) {
          continue;
        } catch (IllegalArgumentException e) {
          continue;
        }
        File sourceFile = targetToSource.get(targetFile);
        if (sourceFile != null) {
          // for any type system file that is also a resource file, return true if it has
          // changed
          if (this.buildContext.hasDelta(sourceFile)) {
            this.getLog().info("Type system file " + sourceFile + " has changed");
            return true;
          }
          // for any type system file that is in the same project, return true if it has
          // changed
          if (targetFile.getAbsolutePath()
                  .startsWith(this.project.getBasedir().getAbsolutePath())) {
            if (this.buildContext.hasDelta(targetFile)) {
              this.getLog().info("Type system file " + sourceFile + " has changed");
              return true;
            }
          }
        }
      }
    }
    // no type system files have changed
    return false;
  }
}
