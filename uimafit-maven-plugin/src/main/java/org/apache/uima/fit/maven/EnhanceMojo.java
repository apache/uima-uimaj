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
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.fit.maven.javadoc.JavadocTextExtractor;
import org.apache.uima.fit.maven.javadoc.ParameterDescriptionExtractor;
import org.apache.uima.fit.maven.util.Util;
import org.apache.uima.fit.util.ReflectionUtil;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.sonatype.plexus.build.incremental.BuildContext;

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
   * Override version in generated descriptors.
   * 
   * @see #componentVersion
   */
  @Parameter(defaultValue="true", required=true)
  private boolean overrideComponentVersion;

  /**
   * Version to use in generated descriptors.
   */
  @Parameter(defaultValue="${project.version}", required=false)
  private String componentVersion;

  /**
   * Override vendor in generated descriptors.
   * 
   * @see #componentVendor
   */
  @Parameter(defaultValue="true", required=true)
  private boolean overrideComponentVendor;

  /**
   * Vendor to use in generated descriptors.
   */
  @Parameter(defaultValue="${project.organization.name}", required=false)
  private String componentVendor;

  /**
   * Source file encoding.
   */
  @Parameter(defaultValue="${project.build.sourceEncoding}", required=true)
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
        if (!Util.isComponent(componentLoader, clazz)) {
          continue;
        }
      }
      catch (ClassNotFoundException e) {
        getLog().warn("Cannot analyze class [" + clazzName + "]", e);
        continue;
      } 
      
      // Get the Javassist class
      CtClass ctClazz;
      try {
        ctClazz = classPool.get(clazzName);
//        if (ctClazz.isFrozen()) {
//          getLog().info("Ignoring frozen class [" + clazzName + "]");
//          continue;
//        }
      }
      catch(NotFoundException e) {
        throw new MojoExecutionException("Class [" + clazzName + "] not found in class pool: " + 
                ExceptionUtils.getRootCauseMessage(e), e);
      }
      ClassFile classFile = ctClazz.getClassFile();
      ConstPool constPool = classFile.getConstPool();
      
      // Get the source file
      String sourceFile = getSourceFile(clazzName);
      
      // Get the parameter name constants
      Map<String, Field> nameFields = getParameterConstants(clazz);
      
      // Try to extract parameter descriptions from JavaDoc in source file
      if (sourceFile != null) {
        getLog().info("Enhancing class [" + clazzName + "]");
        
        // Parse source file so we can extract the JavaDoc
        CompilationUnit ast = parseSource(sourceFile);
        
        // Fetch configuration parameters from the @ConfigurationParameter annotations in the
        // compiled class
        
        for (Field field : ReflectionUtil.getFields(clazz)) {
          // Is this a configuration parameter?
          if (!ConfigurationParameterFactory.isConfigurationParameterField(field)) {
            continue;
          }

//          // Extract configuration parameter information from the uimaFIT annotation
//          // We only want to override if the description is not set yet.
          ConfigurationParameter p = ConfigurationParameterFactory.createPrimitiveParameter(field);
//          if (StringUtils.isNotBlank(p.getDescription())) {
//            continue;
//          }
          
          // Extract JavaDoc for this parameter from the source file
          String pdesc = getParameterDocumentation(ast, field, nameFields.get(p.getName()));
          if (pdesc == null) {
            getLog().warn("No description found for parameter [" + p.getName() + "]");
            continue;
          }
          
          // Update the "description" field of the annotation
          try {
            CtField ctField = ctClazz.getField(field.getName());
            AnnotationsAttribute annoAttr = (AnnotationsAttribute) ctField.getFieldInfo()
                    .getAttribute(AnnotationsAttribute.visibleTag);
            
            // Locate and update annotation
            if (annoAttr != null) {
              Annotation[] annotations = annoAttr.getAnnotations();
              for (Annotation a : annotations) {
                if (a.getTypeName().equals("org.apache.uima.fit.descriptor.ConfigurationParameter")
                        || a.getTypeName().equals("org.uimafit.descriptor.ConfigurationParameter")) {
                  if (a.getMemberValue("description") == null) {
                    a.addMemberValue("description", new StringMemberValue(pdesc, constPool));
                    getLog().info("Enhanced description of parameter [" + p.getName() + "]");
                    // Replace updated annotation
                    annoAttr.addAnnotation(a);
                  } else {
                    getLog().info(
                            "Not enhancing parameter [" + p.getName()
                                    + "] which already has a description");

                  }
                }
              }
            }
            
            // Replace annotations
            ctField.getFieldInfo().addAttribute(annoAttr);
          } catch (NotFoundException e) {
            throw new MojoExecutionException("Field [" + field.getName()
                    + "] not found in byte code: " + ExceptionUtils.getRootCauseMessage(e), e);
          }
        }
      } else {
        getLog().warn("No source file found for class [" + clazzName + "]");
      }
      
      try {
        ctClazz.writeFile(project.getBuild().getOutputDirectory());
      }
      catch (IOException e) {
        throw new MojoExecutionException("Enhanced class [" + clazzName + "] cannot be written: " + 
                ExceptionUtils.getRootCauseMessage(e), e);
        
      } catch (CannotCompileException e) {
        throw new MojoExecutionException("Enhanced class [" + clazzName + "] cannot be compiled: " + 
                ExceptionUtils.getRootCauseMessage(e), e);
      }
    }
  }
  
  /**
   * Get a map of parameter name to parameter name constant field, e.g. ("value",
   * Field("PARAM_VALUE")).
   */
  private Map<String, Field> getParameterConstants(Class aClazz)
  {
    Map<String, Field> result = new HashMap<String, Field>();
    for (Field f : aClazz.getFields()) {
      if (!f.getName().startsWith("PARAM_")) {
        continue;
      }
      try {
        String parameterName = (String) f.get(null);
        result.put(parameterName, f);
      }
      catch (IllegalAccessException e) {
        getLog().warn(
                "Unable to access parameter name constant field [" + f.getName() + "]: "
                        + ExceptionUtils.getRootCauseMessage(e), e);
      }
    }
    return result;
  }
  
  private CompilationUnit parseSource(String aSourceFile) throws MojoExecutionException
  {
    try {
      return Util.parseSource(aSourceFile, encoding);
    }
    catch (IOException e) {
      throw new MojoExecutionException("Unable to parse source file [" + aSourceFile + "]: "
              + ExceptionUtils.getRootCauseMessage(e), e);
    }
  }

  /**
   * Get the source file for the given class.
   * 
   * @return The path to the source file or {@code null} if no source file was found.
   */
  private String getSourceFile(String aClassName)
  {
    String sourceName = aClassName.replace('.', '/') + ".java";
    
    for (String root : (List<String>) project.getCompileSourceRoots())
    {
      File f = new File(root, sourceName);
      if (f.exists()) {
        return f.getPath();
      }
    }
    return null;
  }
  
  private String getParameterDocumentation(CompilationUnit aAst, Field aParameter,
          Field aParameterNameConstant) {
    
    // Generate JavaDoc related annotations
    ParameterDescriptionExtractor visitor = new ParameterDescriptionExtractor(aParameter.getName(),
            (aParameterNameConstant != null) ? aParameterNameConstant.getName() : null);
    aAst.accept(visitor);

    if (visitor.getJavadoc() != null) {
      JavadocTextExtractor textExtractor = new JavadocTextExtractor();
      visitor.getJavadoc().accept(textExtractor);
      // getLog().info(
      // "Description found for parameter [" + aParameter + "]: " + textExtractor.getText());
      return textExtractor.getText();
    } else {
      return null;
    }
  }
}
