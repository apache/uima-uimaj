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
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaSource;

public final class Util {

  private Util() {
    // No instances
  }

  public static JavaSource parseSource(String aSourceFile, String aEncoding) throws IOException {
    JavaDocBuilder builder = new JavaDocBuilder();
    builder.setEncoding(aEncoding);
    builder.addSource(new FileReader(aSourceFile));
    return builder.getSources()[0];
  }

  public static String getComponentDocumentation(JavaSource aAst, String aComponentTypeName) {
//    if (aComponentType.getName().contains("$")) {
//      // rec 2013-01-27: see comment on bindings resolving in ComponentDescriptionExtractor
//      getLog().warn(
//              "Inner classes not supported. Component description for [" + aComponentType.getName()
//                      + "] cannot be extracted. ");
//      return null;
//    }

    for (JavaClass clazz : aAst.getClasses()) {
      if (clazz.asType().getFullyQualifiedName().equals(aComponentTypeName)) {
        return postProcessJavaDoc(clazz.getComment());
      }
    }

    return null;
  }
  
  public static String postProcessJavaDoc(String aJavaDoc)
  {
    if (aJavaDoc == null) {
      return null;
    }
    else {
      return aJavaDoc.replaceAll("\\{@[^ ]+ ([^}]+)\\}", "$1");
    }
  }

  public static String getParameterDocumentation(JavaSource aAst, String aParameterField,
          String aParameterNameConstantField) {

    String javadoc = null;

    JavaClass clazz = aAst.getClasses()[0];
    // CASE 1: JavaDoc is located on parameter name constant
    if (aParameterNameConstantField != null) {
      JavaField field = clazz.getFieldByName(aParameterNameConstantField);
      if (field == null) {
        throw new IllegalArgumentException("Parameter name constant ["
                + aParameterNameConstantField + "] in class ["
                + clazz.asType().getFullyQualifiedName() + "]");
      }
      javadoc = field.getComment();
    }

    // CASE 2: JavaDoc is located on the parameter field itself
    if (javadoc == null) {
      JavaField field = clazz.getFieldByName(aParameterField);
      if (field == null) {
        throw new IllegalArgumentException("No parameter field [" + aParameterField
                + "] in class [" + clazz.asType().getFullyQualifiedName() + "]");
      }
      javadoc = field.getComment();
    }

    return postProcessJavaDoc(javadoc);
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
    // Loading this through the component class loader to make sure we really get the right
    // class instance.
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
