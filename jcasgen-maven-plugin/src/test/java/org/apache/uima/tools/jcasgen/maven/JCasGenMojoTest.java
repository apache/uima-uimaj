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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.util.ArrayList;

import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Rule;
import org.junit.Test;

public class JCasGenMojoTest {

  public @Rule MojoRule rule = new MojoRule();

  @Test
  public void testInvalidFeature() throws Exception {
    assertThatExceptionOfType(Exception.class).isThrownBy(() -> {
      this.test("invalidFeature");
    }).withMessage(
            "JCasGen: The feature name 'type', specified in Type 'type.span.Sentence' is reserved. Please choose another name.");
  }

  @Test
  public void testSimple() throws Exception {
    test("simple", "type.span.Sentence", "type.span.Token", "type.relation.Dependency");
  }

  @Test
  public void testClasspath() throws Exception {
    test("classpath", "type.span.Sentence", "type.span.Token", "type.relation.Dependency");
  }

  @Test
  public void testWildcard() throws Exception {
    test("wildcard", "type.span.Sentence", "type.span.Token");
  }

  @Test
  public void testExclude() throws Exception {
    test("exclude", "type.span.Sentence");
  }

  private void test(String projectName, String... types) throws Exception {

    var projectSourceDirectory = new File("src/test/resources/" + projectName);
    var projectDirectory = new File("target/project-" + projectName + "-test");

    // Stage project to target folder
    FileUtils.copyDirectoryStructure(projectSourceDirectory, projectDirectory);

    var project = rule.readMavenProject(projectDirectory);

    // copy resources
    File source = new File(projectDirectory, "src/main/resources");
    if (source.exists()) {
      FileUtils.copyDirectoryStructure(source, new File(project.getBuild().getOutputDirectory()));
    }

    // load the Mojo
    var generate = (JCasGenMojo) rule.lookupConfiguredMojo(project, "generate");
    assertThat(generate).isNotNull();

    // set the MavenProject on the Mojo (AbstractMojoTestCase does not do this by default)
    rule.setVariableValueToObject(generate, "project", project);

    // execute the Mojo
    generate.execute();

    // check that the Java files have been generated
    File jCasGenDirectory = new File(project.getBasedir(), "target/generated-sources/jcasgen");

    // Record all the files that were generated
    var ds = new DirectoryScanner();
    ds.setBasedir(jCasGenDirectory);
    ds.setIncludes(new String[] { "**/*.java" });
    ds.scan();

    var files = new ArrayList<File>();
    for (var scannedFile : ds.getIncludedFiles()) {
      files.add(new File(ds.getBasedir(), scannedFile));
    }

    for (var type : types) {
      var wrapperFile = new File(jCasGenDirectory + "/" + type.replace('.', '/') + ".java");
      // no _type files in v3
      // File typeFile = new File(jCasGenDirectory + "/" + type.replace('.', '/') + "_Type.java");

      assertThat(files).contains(wrapperFile);
      // no _type files in v3
      // Assert.assertTrue(files.contains(typeFile));

      files.remove(wrapperFile);
      // files.remove(typeFile);
    }

    // check that no extra files were generated
    assertThat(files).isEmpty();

    // check that the generated sources are on the compile path
    assertThat(project.getCompileSourceRoots()).contains(jCasGenDirectory.getAbsolutePath());
  }
}
