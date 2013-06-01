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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

public class JCasGenMojoTest extends AbstractMojoTestCase {

  public void testSimple() throws Exception {
    this.test("simple", "type.span.Sentence", "type.span.Token", "type.relation.Dependency");
  }

  public void testClasspath() throws Exception {
    this.test("classpath", "type.span.Sentence", "type.span.Token", "type.relation.Dependency");
  }

  public void testWildcard() throws Exception {
    this.test("wildcard", "type.span.Sentence", "type.span.Token");
  }

  public void testExclude() throws Exception {
    this.test("exclude", "type.span.Sentence");
  }

  public void test(String projectName, String... types) throws Exception {

    File projectDirectory = getTestFile("src/test/resources/" + projectName);
    File buildDirectory = getTestFile("target/project-" + projectName + "-test");
    File pomFile = new File(projectDirectory, "/pom.xml");
    assertNotNull(pomFile);
    assertTrue(pomFile.exists());

    // create the MavenProject from the pom.xml file
    MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
    ProjectBuildingRequest buildingRequest = executionRequest.getProjectBuildingRequest();
    ProjectBuilder projectBuilder = this.lookup(ProjectBuilder.class);
    MavenProject project = projectBuilder.build(pomFile, buildingRequest).getProject();
    assertNotNull(project);

    // set the base directory (or it will write to src/test/resources/)
    Build build = project.getModel().getBuild();
    build.setDirectory(buildDirectory.getPath());
    File outputDirectory = new File(buildDirectory, "target/classes");
    build.setOutputDirectory(outputDirectory.getPath());

    // copy resources
    File source = new File(projectDirectory, "src/main/resources");
    if (source.exists()) {
      FileUtils.copyDirectoryStructure(source, outputDirectory);
    }

    // load the Mojo
    JCasGenMojo generate = (JCasGenMojo) this.lookupConfiguredMojo(project, "generate");
    assertNotNull(generate);

    // set the MavenProject on the Mojo (AbstractMojoTestCase does not do this by default)
    setVariableValueToObject(generate, "project", project);

    // execute the Mojo
    generate.execute();

    // check that the Java files have been generated
    File jCasGenDirectory = new File(buildDirectory, "generated-sources/jcasgen");
    
    // Record all the files that were generated
    DirectoryScanner ds = new DirectoryScanner();
    ds.setBasedir(jCasGenDirectory);
    ds.setIncludes(new String[] { "**/*.java" });
    ds.scan();
    List<File> files = new ArrayList<File>();
    for (String scannedFile : ds.getIncludedFiles()) {
      files.add(new File(ds.getBasedir(), scannedFile));
    }
    
    for (String type : types) {
      File wrapperFile = new File(jCasGenDirectory + "/" + type.replace('.', '/') + ".java");
      File typeFile = new File(jCasGenDirectory + "/" + type.replace('.', '/') + "_Type.java");
      
      Assert.assertTrue(files.contains(wrapperFile));
      Assert.assertTrue(files.contains(typeFile));
      
      files.remove(wrapperFile);
      files.remove(typeFile);
    }
    
    // check that no extra files were generated
    Assert.assertTrue(files.isEmpty());

    // check that the generated sources are on the compile path
    Assert.assertTrue(project.getCompileSourceRoots().contains(jCasGenDirectory.getAbsolutePath()));
  }
}
