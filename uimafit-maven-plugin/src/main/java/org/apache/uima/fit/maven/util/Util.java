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
package org.apache.uima.fit.maven.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public final class Util {

  private Util() {
    // No instances
  }

  public static CompilationUnit parseSource(String aSourceFile, String aEncoding)
          throws IOException {
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setSource(org.apache.commons.io.FileUtils.readFileToString(new File(aSourceFile),
            aEncoding).toCharArray());
    Map options = JavaCore.getOptions();
    options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
    parser.setCompilerOptions(options);
    return (CompilationUnit) parser.createAST(null);
  }

  /**
   * Get the class name for a class file.
   */
  public static String getClassName(MavenProject aProject, String aFile) {
    String base = aFile.substring(0, aFile.length() - 6);
    String clazzPath = base.substring(aProject.getBuild().getOutputDirectory().length() + 1);
    return clazzPath.replace("/", ".");
  }

  /**
   * Create a class loader which covers the classes compiled in the current project and all
   * dependencies.
   * 
   * @throws MojoExecutionException
   */
  public static URLClassLoader getClassloader(MavenProject aProject, Log aLog)
          throws MojoExecutionException {
    List<URL> urls = new ArrayList<URL>();
    try {
      for (Object object : aProject.getCompileClasspathElements()) {
        String path = (String) object;
        aLog.debug("Classpath entry: " + object);
        urls.add(new File(path).toURI().toURL());
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to assemble classpath: "
              + ExceptionUtils.getRootCauseMessage(e), e);
    } catch (DependencyResolutionRequiredException e) {
      throw new MojoExecutionException("Unable to resolve dependencies: "
              + ExceptionUtils.getRootCauseMessage(e), e);
    }

    for (Artifact dep : (Set<Artifact>) aProject.getDependencyArtifacts()) {
      try {
        if (dep.getFile() == null) {
          // Unresolved file because it is in the wrong scope (e.g. test?)
          continue;
        }
        aLog.debug("Classpath entry: " + dep.getGroupId() + ":" + dep.getArtifactId() + ":"
                + dep.getVersion() + " -> " + dep.getFile());
        urls.add(dep.getFile().toURI().toURL());
      } catch (Exception e) {
        throw new MojoExecutionException("Unable get dependency artifact location for "
                + dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion()
                + ExceptionUtils.getRootCauseMessage(e), e);
      }
    }
    return new URLClassLoader(urls.toArray(new URL[] {}), Util.class.getClassLoader());
  }

  public static boolean isComponent(ClassLoader aClassLoader, Class aClass)
          throws ClassNotFoundException {
    return getType(aClassLoader, aClass) != ComponentType.NONE;
  }

  /**
   * Determine what kind of class it is.
   * 
   * @param aClassLoader
   *          the class loader by which the component was loaded.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static ComponentType getType(ClassLoader aClassLoader, Class aClass)
          throws ClassNotFoundException {
    Class iCR = aClassLoader.loadClass("org.apache.uima.collection.CollectionReader");
    Class iAE = aClassLoader.loadClass("org.apache.uima.analysis_component.AnalysisComponent");
    if (iCR.isAssignableFrom(aClass)) {
      return ComponentType.COLLECTION_READER;
    } else if (iAE.isAssignableFrom(aClass)) {
      return ComponentType.ANALYSIS_ENGINE;
    } else {
      return ComponentType.NONE;
    }
  }
}
