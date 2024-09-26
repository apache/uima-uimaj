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
package org.apache.uima.fit.maven;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.ResourceMetaDataFactory;
import org.apache.uima.fit.internal.EnhancedClassFile;
import org.apache.uima.fit.maven.util.Util;
import org.codehaus.plexus.util.FileUtils;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.thoughtworks.qdox.model.JavaSource;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;

/**
 * Enhance UIMA components with automatically generated uimaFIT annotations.
 */
@Mojo(name = "enhance", defaultPhase = PROCESS_CLASSES, requiresDependencyResolution = TEST, requiresDependencyCollection = TEST)
public class EnhanceMojo extends AbstractMojo {
  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  @Component
  private BuildContext buildContext;

  private ClassLoader componentLoader;

  /**
   * Override component description in generated descriptors.
   */
  @Parameter(defaultValue = "false", required = true)
  private boolean overrideComponentDescription;

  /**
   * Override version in generated descriptors.
   * 
   * @see #componentVersion
   */
  @Parameter(defaultValue = "false", required = true)
  private boolean overrideComponentVersion;

  /**
   * Version to use in generated descriptors.
   */
  @Parameter(defaultValue = "${project.version}", required = false)
  private String componentVersion;

  /**
   * Override vendor in generated descriptors.
   * 
   * @see #componentVendor
   */
  @Parameter(defaultValue = "false", required = true)
  private boolean overrideComponentVendor;

  /**
   * Vendor to use in generated descriptors.
   */
  @Parameter(defaultValue = "${project.organization.name}", required = false)
  private String componentVendor;

  /**
   * Override copyright in generated descriptors.
   * 
   * @see #componentCopyright
   */
  @Parameter(defaultValue = "false", required = true)
  private boolean overrideComponentCopyright;

  /**
   * Copyright to use in generated descriptors.
   */
  @Parameter(required = false)
  private String componentCopyright;

  /**
   * Source file encoding.
   */
  @Parameter(defaultValue = "${project.build.sourceEncoding}", required = true)
  private String encoding;

  /**
   * Generate a report of missing meta data in {@code $ project.build.directory}
   * /uimafit-missing-meta-data-report.txt}
   */
  @Parameter(defaultValue = "true", required = true)
  private boolean generateMissingMetaDataReport;

  /**
   * Fail on missing meta data. This setting has no effect unless
   * {@code generateMissingMetaDataReport} is enabled.
   */
  @Parameter(defaultValue = "false", required = true)
  private boolean failOnMissingMetaData;

  /**
   * Constant name prefixes used for parameters and external resources, e.g. "PARAM_".
   */
  @Parameter(required = false)
  private String[] parameterNameConstantPrefixes = { "PARAM_" };

  /**
   * Constant name prefixes used for parameters and external resources, e.g. "KEY_", and "RES_".
   */
  @Parameter(required = false)
  private String[] externalResourceNameConstantPrefixes = { "KEY_", "RES_" };

  /**
   * Scope threshold to include. The default is "compile" (which implies compile, provided and
   * system dependencies). Can also be changed to "test" (which implies all dependencies).
   */
  @Parameter(defaultValue = "compile", required = true)
  private String includeScope;

  /**
   * Skip plugin execution.
   */
  @Parameter(property = "uima-enhance.skip", defaultValue = "false", required = true)
  private boolean skip;

  /**
   * Skip plugin execution only during incremental builds (e.g. triggered from m2e).
   */
  @Parameter(defaultValue = "false", required = true)
  private boolean skipDuringIncrementalBuilds;

  /**
   * Start of a line containing a class name in the missing meta data report file
   */
  private static final String MARK_CLASS = "Class:";

  /**
   * Marker that no missing meta data report was found.
   */
  private static final String MARK_NO_MISSING_META_DATA = "No missing meta data was found.";

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (isSkipped()) {
      return;
    }

    // Get the compiled classes from this project
    String[] files = FileUtils.getFilesFromExtension(project.getBuild().getOutputDirectory(),
            new String[] { "class" });
    componentLoader = Util.getClassloader(project, getLog(), includeScope);

    // Set up class pool with all the project dependencies and the project classes themselves
    ClassPool classPool = new ClassPool(true);
    classPool.appendClassPath(new LoaderClassPath(componentLoader));

    // Set up map to keep a report per class.
    Multimap<String, String> reportData = LinkedHashMultimap.create();

    // Determine where to write the missing meta data report file
    File reportFile = new File(project.getBuild().getDirectory(),
            "uimafit-missing-meta-data-report.txt");

    // Read existing report
    if (generateMissingMetaDataReport) {
      readMissingMetaDataReport(reportFile, reportData);
    }

    // Remember the names of all examined components, whether processed or not.
    List<String> examinedComponents = new ArrayList<String>();

    int countAlreadyEnhanced = 0;
    int countEnhanced = 0;

    for (String file : files) {
      String clazzName = Util.getClassName(project, file);

      // Check if this is a UIMA component
      Class<?> clazz;
      try {
        clazz = componentLoader.loadClass(clazzName);

        // Do not process a class twice
        // UIMA-3853 workaround for IBM Java 8 beta 3
        if (clazz.getAnnotation(EnhancedClassFile.class) != null) {
          countAlreadyEnhanced++;
          getLog().debug("Class [" + clazzName + "] already enhanced");
          // Remember that class was examined
          examinedComponents.add(clazzName);
          continue;
        }

        // Only process UIMA components
        if (!Util.isComponent(componentLoader, clazz)) {
          continue;
        }
      } catch (ClassNotFoundException e) {
        getLog().warn("Cannot analyze class [" + clazzName + "]", e);
        continue;
      }

      // Remember that class was examined
      examinedComponents.add(clazzName);

      // Forget any previous missing meta data report we have on the class
      reportData.removeAll(clazzName);

      // Get the Javassist class
      CtClass ctClazz;
      try {
        ctClazz = classPool.get(clazzName);
      } catch (NotFoundException e) {
        throw new MojoExecutionException(
                "Class [" + clazzName + "] not found in class pool: " + getRootCauseMessage(e), e);
      }

      // Get the source file
      String sourceFile = getSourceFile(clazzName);

      // Try to extract parameter descriptions from JavaDoc in source file
      if (sourceFile != null) {
        countEnhanced++;
        getLog().debug("Enhancing class [" + clazzName + "]");

        // Parse source file so we can extract the JavaDoc
        JavaSource ast = parseSource(sourceFile);

        // Enhance meta data
        enhanceResourceMetaData(ast, clazz, ctClazz, reportData);

        // Enhance configuration parameters
        enhanceConfigurationParameter(ast, clazz, ctClazz, reportData);

        // Add the EnhancedClassFile annotation.
        markAsEnhanced(ctClazz);
      } else {
        getLog().warn("No source file found for class [" + clazzName + "]");
      }

      try {
        if (ctClazz.isModified()) {
          getLog().debug("Writing enhanced class [" + clazzName + "]");
          // Trying to work around UIMA-2611, see
          // http://stackoverflow.com/questions/13797919/javassist-add-method-and-invoke
          ctClazz.toBytecode();
          ctClazz.writeFile(project.getBuild().getOutputDirectory());
        } else {
          getLog().debug("No changes to class [" + clazzName + "]");
        }
      } catch (IOException e) {
        throw new MojoExecutionException(
                "Enhanced class [" + clazzName + "] cannot be written: " + getRootCauseMessage(e),
                e);
      } catch (CannotCompileException e) {
        throw new MojoExecutionException(
                "Enhanced class [" + clazzName + "] cannot be compiled: " + getRootCauseMessage(e),
                e);
      }
    }

    getLog().info("Enhanced " + countEnhanced + " class" + (countEnhanced != 1 ? "es" : "") + " ("
            + countAlreadyEnhanced + " already enhanced).");

    if (generateMissingMetaDataReport) {
      // Remove any classes from the report that are no longer part of the build
      List<String> deletedClasses = new ArrayList<String>(reportData.keySet());
      deletedClasses.removeAll(examinedComponents);
      reportData.removeAll(deletedClasses);

      // Write updated report
      writeMissingMetaDataReport(reportFile, reportData);

      if (failOnMissingMetaData && !reportData.isEmpty()) {
        throw new MojoFailureException("Component meta data missing. A report of the missing "
                + "meta data can be found in " + reportFile);
      }
    }
  }

  private boolean isSkipped() {
    if (skipDuringIncrementalBuilds && buildContext.isIncremental()) {
      getLog().info("Enhancement of UIMA component classes skipped in incremental build.");
      return true;
    }

    if (skip) {
      getLog().info("Enhancement of UIMA component classes skipped.");
      return true;
    }

    return false;
  }

  /**
   * Add the EnhancedClassFile annotation.
   */
  private void markAsEnhanced(CtClass aCtClazz) {
    ClassFile classFile = aCtClazz.getClassFile();
    ConstPool constPool = classFile.getConstPool();

    AnnotationsAttribute annoAttr = (AnnotationsAttribute) classFile
            .getAttribute(AnnotationsAttribute.visibleTag);

    // Create annotation attribute if it does not exist
    if (annoAttr == null) {
      annoAttr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
    }

    // Create annotation if it does not exist
    Annotation a = new Annotation(EnhancedClassFile.class.getName(), constPool);

    // Replace annotation
    annoAttr.addAnnotation(a);

    // Replace annotation attribute
    classFile.addAttribute(annoAttr);
  }

  /**
   * Enhance resource meta data
   */
  private void enhanceResourceMetaData(JavaSource aAST, Class<?> aClazz, CtClass aCtClazz,
          Multimap<String, String> aReportData) {
    ClassFile classFile = aCtClazz.getClassFile();
    ConstPool constPool = classFile.getConstPool();

    AnnotationsAttribute annoAttr = (AnnotationsAttribute) classFile
            .getAttribute(AnnotationsAttribute.visibleTag);

    // Create annotation attribute if it does not exist
    if (annoAttr == null) {
      annoAttr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
    }

    // Create annotation if it does not exist
    Annotation a = annoAttr.getAnnotation(ResourceMetaData.class.getName());
    if (a == null) {
      a = new Annotation(ResourceMetaData.class.getName(), constPool);
      // Add a name, otherwise there will be none in the generated descriptor.
      a.addMemberValue("name",
              new StringMemberValue(ResourceMetaDataFactory.getDefaultName(aClazz), constPool));
    }

    // Update description from JavaDoc
    String doc = Util.getComponentDocumentation(aAST, aClazz.getName());
    enhanceMemberValue(a, "description", doc, overrideComponentDescription,
            ResourceMetaDataFactory.getDefaultDescription(aClazz), constPool, aReportData, aClazz);

    // Update version
    enhanceMemberValue(a, "version", componentVersion, overrideComponentVersion,
            ResourceMetaDataFactory.getDefaultVersion(aClazz), constPool, aReportData, aClazz);

    // Update vendor
    enhanceMemberValue(a, "vendor", componentVendor, overrideComponentVendor,
            ResourceMetaDataFactory.getDefaultVendor(aClazz), constPool, aReportData, aClazz);

    // Update copyright
    enhanceMemberValue(a, "copyright", componentCopyright, overrideComponentCopyright,
            ResourceMetaDataFactory.getDefaultCopyright(aClazz), constPool, aReportData, aClazz);

    // Replace annotation
    annoAttr.addAnnotation(a);

    // Replace annotation attribute
    classFile.addAttribute(annoAttr);
  }

  /**
   * Set a annotation member value if no value is present, if the present value is the default
   * generated by uimaFIT or if a override is active.
   * 
   * @param aAnnotation
   *          an annotation
   * @param aName
   *          the name of the member value
   * @param aNewValue
   *          the value to set
   * @param aOverride
   *          set value even if it is already set
   * @param aDefault
   *          default value set by uimaFIT - if the member has this value, it is considered unset
   */
  private void enhanceMemberValue(Annotation aAnnotation, String aName, String aNewValue,
          boolean aOverride, String aDefault, ConstPool aConstPool,
          Multimap<String, String> aReportData, Class<?> aClazz) {
    String value = getStringMemberValue(aAnnotation, aName);
    boolean isEmpty = value.length() == 0;
    boolean isDefault = value.equals(aDefault);

    if (isEmpty || isDefault || aOverride) {
      if (aNewValue != null) {
        aAnnotation.addMemberValue(aName, new StringMemberValue(aNewValue, aConstPool));
        getLog().debug("Enhanced component meta data [" + aName + "]");
      } else {
        getLog().debug("No meta data [" + aName + "] found");
        aReportData.put(aClazz.getName(), "No meta data [" + aName + "] found");
      }
    } else {
      getLog().debug("Not overwriting component meta data [" + aName + "]");
    }
  }

  private String getStringMemberValue(Annotation aAnnotation, String aValue) {
    MemberValue v = aAnnotation.getMemberValue(aValue);
    if (v == null) {
      return "";
    } else {
      return ((StringMemberValue) v).getValue();
    }
  }

  /**
   * Enhance descriptions in configuration parameters.
   */
  private void enhanceConfigurationParameter(JavaSource aAST, Class<?> aClazz, CtClass aCtClazz,
          Multimap<String, String> aReportData) throws MojoExecutionException {
    // Get the parameter name constants
    Map<String, String> parameterNameFields = getParameterConstants(aClazz,
            parameterNameConstantPrefixes);
    Map<String, String> resourceNameFields = getParameterConstants(aClazz,
            externalResourceNameConstantPrefixes);

    // Fetch configuration parameters from the @ConfigurationParameter annotations in the
    // compiled class. We only need the fields in the class itself. Superclasses should be
    // enhanced by themselves.
    for (Field field : aClazz.getDeclaredFields()) {
      final String pname;
      final String type;
      final String pdesc;

      // Is this a configuration parameter?
      if (ConfigurationParameterFactory.isConfigurationParameterField(field)) {
        type = "parameter";
        // Extract configuration parameter information from the uimaFIT annotation
        pname = ConfigurationParameterFactory.createPrimitiveParameter(field).getName();
        // Extract JavaDoc for this resource from the source file
        pdesc = Util.getParameterDocumentation(aAST, field.getName(),
                parameterNameFields.get(pname));
      }

      // Is this an external resource?
      else if (ExternalResourceFactory.isExternalResourceField(field)) {
        type = "external resource";
        // Extract resource key from the uimaFIT annotation
        pname = ExternalResourceFactory.createResourceDependency(field).getKey();
        // Extract JavaDoc for this resource from the source file
        pdesc = Util.getParameterDocumentation(aAST, field.getName(),
                resourceNameFields.get(pname));
      } else {
        continue;
      }

      if (pdesc == null) {
        String msg = "No description found for " + type + " [" + pname + "]";
        getLog().debug(msg);
        aReportData.put(aClazz.getName(), msg);
        continue;
      }

      // Update the "description" field of the annotation
      try {
        CtField ctField = aCtClazz.getField(field.getName());
        AnnotationsAttribute annoAttr = (AnnotationsAttribute) ctField.getFieldInfo()
                .getAttribute(AnnotationsAttribute.visibleTag);

        // Locate and update annotation
        if (annoAttr != null) {
          Annotation[] annotations = annoAttr.getAnnotations();

          // Update existing annotation
          for (Annotation a : annotations) {
            if (a.getTypeName()
                    .equals(org.apache.uima.fit.descriptor.ConfigurationParameter.class.getName())
                    || a.getTypeName()
                            .equals(org.apache.uima.fit.descriptor.ExternalResource.class.getName())
                    || a.getTypeName().equals("org.uimafit.descriptor.ConfigurationParameter")
                    || a.getTypeName().equals("org.uimafit.descriptor.ExternalResource")) {
              if (a.getMemberValue("description") == null) {
                a.addMemberValue("description",
                        new StringMemberValue(pdesc, aCtClazz.getClassFile().getConstPool()));
                getLog().debug("Enhanced description of " + type + " [" + pname + "]");
                // Replace updated annotation
                annoAttr.addAnnotation(a);
              } else {
                // Extract configuration parameter information from the uimaFIT
                // annotation
                // We only want to override if the description is not set yet.
                getLog().debug("Not overwriting description of " + type + " [" + pname + "] ");
              }
            }
          }
        }

        // Replace annotations
        ctField.getFieldInfo().addAttribute(annoAttr);
      } catch (NotFoundException e) {
        throw new MojoExecutionException("Field [" + field.getName() + "] not found in byte code: "
                + ExceptionUtils.getRootCauseMessage(e), e);
      }
    }
  }

  /**
   * Get a map of parameter name to parameter name constant field, e.g. ("value",
   * Field("PARAM_VALUE")).
   */
  private Map<String, String> getParameterConstants(Class<?> aClazz, String[] aPrefixes) {
    Map<String, String> result = new HashMap<String, String>();
    for (Field f : aClazz.getFields()) {
      boolean hasPrefix = false;
      // Check if any of the registered prefixes matches
      for (String prefix : aPrefixes) {
        if (f.getName().startsWith(prefix)) {
          hasPrefix = true;
          break;
        }
      }

      // If none matched, continue
      if (!hasPrefix) {
        continue;
      }

      // If one matched, record the field
      try {
        String parameterName = (String) f.get(null);
        result.put(parameterName, f.getName());
      } catch (IllegalAccessException e) {
        getLog().warn("Unable to access name constant field [" + f.getName() + "]: "
                + ExceptionUtils.getRootCauseMessage(e), e);
      }
    }
    return result;
  }

  private JavaSource parseSource(String aSourceFile) throws MojoExecutionException {
    try {
      return Util.parseSource(aSourceFile, encoding);
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to parse source file [" + aSourceFile + "]: "
              + ExceptionUtils.getRootCauseMessage(e), e);
    }
  }

  /**
   * Get the source file for the given class.
   * 
   * @return The path to the source file or {@code null} if no source file was found.
   */
  @SuppressWarnings("unchecked")
  private String getSourceFile(String aClassName) {
    String sourceName = aClassName.replace('.', '/') + ".java";

    for (String root : (List<String>) project.getCompileSourceRoots()) {
      File f = new File(root, sourceName);
      if (f.exists()) {
        return f.getPath();
      }
    }
    return null;
  }

  /**
   * Write a report on any meta data missing from components.
   */
  private void writeMissingMetaDataReport(File aReportFile, Multimap<String, String> aReportData)
          throws MojoExecutionException {
    String[] classes = aReportData.keySet().toArray(new String[aReportData.keySet().size()]);
    Arrays.sort(classes);

    PrintWriter out = null;
    FileUtils.mkdir(aReportFile.getParent());
    try {
      out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(aReportFile), encoding));

      if (classes.length > 0) {
        for (String clazz : classes) {
          out.printf("%s %s%n", MARK_CLASS, clazz);
          Collection<String> messages = aReportData.get(clazz);
          if (messages.isEmpty()) {
            out.printf("  No problems");
          } else {
            for (String message : messages) {
              out.printf("  %s%n", message);
            }
          }
          out.printf("%n");
        }
      } else {
        out.printf("%s%n", MARK_NO_MISSING_META_DATA);
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to write missing meta data report to [" + aReportFile
              + "]" + ExceptionUtils.getRootCauseMessage(e), e);
    } finally {
      IOUtils.closeQuietly(out);
    }
  }

  /**
   * Read the missing meta data report from a previous run.
   */
  private void readMissingMetaDataReport(File aReportFile, Multimap<String, String> aReportData)
          throws MojoExecutionException {
    if (!aReportFile.exists()) {
      // Ignore if the file is missing
      return;
    }

    LineIterator i = null;
    try {
      String clazz = null;
      i = IOUtils.lineIterator(new FileInputStream(aReportFile), encoding);
      while (i.hasNext()) {
        String line = i.next();
        // Report say there is no missing meta data
        if (line.startsWith(MARK_NO_MISSING_META_DATA)) {
          return;
        }
        // Line containing class name
        if (line.startsWith(MARK_CLASS)) {
          clazz = line.substring(MARK_CLASS.length()).trim();
        } else if (StringUtils.isBlank(line)) {
          // Empty line, ignore
        } else {
          // Line containing a missing meta data instance
          if (clazz == null) {
            throw new MojoExecutionException("Missing meta data report has invalid format.");
          }
          aReportData.put(clazz, line.trim());
        }
      }
    } catch (IOException e) {
      throw new MojoExecutionException(
              "Unable to read missing meta data report: " + ExceptionUtils.getRootCauseMessage(e),
              e);
    } finally {
      LineIterator.closeQuietly(i);
    }
  }
}
