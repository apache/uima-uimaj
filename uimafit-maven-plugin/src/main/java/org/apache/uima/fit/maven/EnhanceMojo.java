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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.fit.factory.ResourceMetaDataFactory;
import org.apache.uima.fit.maven.util.Util;
import org.apache.uima.fit.util.EnhancedClassFile;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.codehaus.plexus.util.FileUtils;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.thoughtworks.qdox.model.JavaSource;

/**
 * Enhance UIMA components with automatically generated uimaFIT annotations.
 */
@Mojo(name = "enhance", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE, requiresDependencyCollection = ResolutionScope.COMPILE)
public class EnhanceMojo extends AbstractMojo {
  @Component
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
   * @see #component
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

  public void execute() throws MojoExecutionException {
    // Get the compiled classes from this project
    String[] files = FileUtils.getFilesFromExtension(project.getBuild().getOutputDirectory(),
            new String[] { "class" });

    componentLoader = Util.getClassloader(project, getLog());

    // Set up class pool with all the project dependencies and the project classes themselves
    ClassPool classPool = new ClassPool(true);
    classPool.appendClassPath(new LoaderClassPath(componentLoader));

    for (String file : files) {
      String clazzName = Util.getClassName(project, file);

      // Check if this is a UIMA component
      Class clazz;
      try {
        clazz = componentLoader.loadClass(clazzName);
        
        // Do not process a class twice
        if (clazz.isAnnotationPresent(EnhancedClassFile.class)) {
          getLog().info("Class [" + clazzName + "] already enhanced");
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

      // Get the Javassist class
      CtClass ctClazz;
      try {
        ctClazz = classPool.get(clazzName);
      } catch (NotFoundException e) {
        throw new MojoExecutionException("Class [" + clazzName + "] not found in class pool: "
                + ExceptionUtils.getRootCauseMessage(e), e);
      }

      // Get the source file
      String sourceFile = getSourceFile(clazzName);

      // Try to extract parameter descriptions from JavaDoc in source file
      if (sourceFile != null) {
        getLog().info("Enhancing class [" + clazzName + "]");

        // Parse source file so we can extract the JavaDoc
        JavaSource ast = parseSource(sourceFile);

        // Enhance meta data
        enhanceResourceMetaData(ast, clazz, ctClazz);

        // Enhance configuration parameters
        enhanceConfigurationParameter(ast, clazz, ctClazz);
        
        // Add the EnhancedClassFile annotation.
        markAsEnhanced(ctClazz);
      } else {
        getLog().warn("No source file found for class [" + clazzName + "]");
      }

      try {
        if (ctClazz.isModified()) {
          getLog().info("Writing enhanced class [" + clazzName + "]");
          // Trying to work around UIMA-2611, see 
          // http://stackoverflow.com/questions/13797919/javassist-add-method-and-invoke
          ctClazz.toBytecode(); 
          ctClazz.writeFile(project.getBuild().getOutputDirectory());
        }
        else {
          getLog().info("No changes to class [" + clazzName + "]");
        }
      } catch (IOException e) {
        throw new MojoExecutionException("Enhanced class [" + clazzName + "] cannot be written: "
                + ExceptionUtils.getRootCauseMessage(e), e);

      } catch (CannotCompileException e) {
        throw new MojoExecutionException("Enhanced class [" + clazzName + "] cannot be compiled: "
                + ExceptionUtils.getRootCauseMessage(e), e);
      }
    }
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
  private void enhanceResourceMetaData(JavaSource aAST, Class aClazz, CtClass aCtClazz)
          throws MojoExecutionException {
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
      a.addMemberValue("name", new StringMemberValue(
              ResourceMetaDataFactory.getDefaultName(aClazz), constPool));
    }

    // Update description from JavaDoc
    String doc = Util.getComponentDocumentation(aAST, aClazz.getName());
    enhanceMemberValue(a, "description", doc, overrideComponentDescription,
            ResourceMetaDataFactory.getDefaultDescription(aClazz), constPool);

    // Update version
    enhanceMemberValue(a, "version", componentVersion, overrideComponentVersion,
            ResourceMetaDataFactory.getDefaultVersion(aClazz), constPool);

    // Update vendor
    enhanceMemberValue(a, "vendor", componentVendor, overrideComponentVendor,
            ResourceMetaDataFactory.getDefaultVendor(aClazz), constPool);

    // Update copyright
    enhanceMemberValue(a, "copyright", componentCopyright, overrideComponentCopyright,
            ResourceMetaDataFactory.getDefaultVendor(aClazz), constPool);

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
          boolean aOverride, String aDefault, ConstPool aConstPool) {
    String value = getStringMemberValue(aAnnotation, aName);
    boolean isEmpty = value.length() == 0;
    boolean isDefault = value.equals(aDefault);

    if (isEmpty || isDefault || aOverride) {
      if (aNewValue != null) {
        aAnnotation.addMemberValue(aName, new StringMemberValue(aNewValue, aConstPool));
        getLog().info("Enhanced component meta data [" + aName + "]");
      }
      else {
        getLog().warn("No meta data [" + aName + "] found");
      }
    } else {
      getLog().info("Not overwriting component meta data [" + aName + "]");
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
  private void enhanceConfigurationParameter(JavaSource aAST, Class aClazz, CtClass aCtClazz)
          throws MojoExecutionException {
    // Get the parameter name constants
    Map<String, Field> nameFields = getParameterConstants(aClazz);

    // Fetch configuration parameters from the @ConfigurationParameter annotations in the
    // compiled class. We only need the fields in the class itself. Superclasses should be
    // enhanced by themselves.
    for (Field field : aClazz.getDeclaredFields()) {
      // Is this a configuration parameter?
      if (!ConfigurationParameterFactory.isConfigurationParameterField(field)) {
        continue;
      }

      // Extract configuration parameter information from the uimaFIT annotation
      ConfigurationParameter p = ConfigurationParameterFactory.createPrimitiveParameter(field);

      // Extract JavaDoc for this parameter from the source file
      String pdesc = Util.getParameterDocumentation(aAST, field.getName(),
              nameFields.get(p.getName()).getName());
      if (pdesc == null) {
        getLog().warn("No description found for parameter [" + p.getName() + "]");
        continue;
      }

      // Update the "description" field of the annotation
      try {
        CtField ctField = aCtClazz.getField(field.getName());
        AnnotationsAttribute annoAttr = (AnnotationsAttribute) ctField.getFieldInfo().getAttribute(
                AnnotationsAttribute.visibleTag);

        // Locate and update annotation
        if (annoAttr != null) {
          Annotation[] annotations = annoAttr.getAnnotations();

          // Update existing annotation
          for (Annotation a : annotations) {
            if (a.getTypeName().equals(
                    org.apache.uima.fit.descriptor.ConfigurationParameter.class.getName())
                    || a.getTypeName().equals("org.uimafit.descriptor.ConfigurationParameter")) {
              if (a.getMemberValue("description") == null) {
                a.addMemberValue("description", new StringMemberValue(pdesc, aCtClazz
                        .getClassFile().getConstPool()));
                getLog().info("Enhanced description of parameter [" + p.getName() + "]");
                // Replace updated annotation
                annoAttr.addAnnotation(a);
              } else {
                // Extract configuration parameter information from the uimaFIT annotation
                // We only want to override if the description is not set yet.
                getLog().info("Not overwriting description of parameter [" + p.getName() + "] ");
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
  private Map<String, Field> getParameterConstants(Class aClazz) {
    Map<String, Field> result = new HashMap<String, Field>();
    for (Field f : aClazz.getFields()) {
      if (!f.getName().startsWith("PARAM_")) {
        continue;
      }
      try {
        String parameterName = (String) f.get(null);
        result.put(parameterName, f);
      } catch (IllegalAccessException e) {
        getLog().warn(
                "Unable to access parameter name constant field [" + f.getName() + "]: "
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
}
