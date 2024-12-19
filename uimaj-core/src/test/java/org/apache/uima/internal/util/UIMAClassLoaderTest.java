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

package org.apache.uima.internal.util;

import static java.util.Arrays.asList;
import static java.util.Collections.list;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.util.zip.ZipFile;

import org.apache.uima.UIMAFramework;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UIMAClassLoaderTest {

  private String testClassPath;

  @BeforeEach
  public void setUp() throws Exception {

    testClassPath = JUnitExtension.getFile("ClassLoaderTest/classLoadingTest.jar")
            .getAbsolutePath();
  }

  @Test
  void testSimpleRsrcMgrCLassLoaderCreation() throws Exception {
    var rsrcMgr = UIMAFramework.newDefaultResourceManager();

    assertThat(rsrcMgr.getExtensionClassLoader()) //
            .as("Freshly created resource manager has no extension classloader") //
            .isNull();

    rsrcMgr.setExtensionClassPath("../this/is/a/simple/test.jar", false);

    assertThat(rsrcMgr.getExtensionClassLoader()) //
            .as("After setting an extension classpath, there is an extension classloader") //
            .isInstanceOf(UIMAClassLoader.class);

    var cl = (UIMAClassLoader) rsrcMgr.getExtensionClassLoader();
    var o = cl.getClassLoadingLockForTesting("someString");
    var o2 = cl.getClassLoadingLockForTesting("s2");
    assertThat(o != o2).isTrue();
    assertThat(cl != o).isTrue();
  }

  @Test
  void testAdvancedRsrcMgrCLassLoaderCreation() throws Exception {
    var rsrcMgr = UIMAFramework.newDefaultResourceManager();

    assertThat(rsrcMgr.getExtensionClassLoader()).isNull();

    rsrcMgr.setExtensionClassPath("../this/is/a/simple/test.jar", true);

    assertThat(rsrcMgr.getExtensionClassLoader()).isNotNull();
  }

  @Test
  void testSimpleClassloadingSampleString() throws Exception {
    var cl = new UIMAClassLoader(testClassPath, this.getClass().getClassLoader());

    var testClass = cl.loadClass("org.apache.uima.internal.util.ClassloadingTestClass");

    assertThat(testClass).isNotNull();
    assertThat(testClass.getClassLoader()).isEqualTo(cl);

    testClass = cl.loadClass("org.apache.uima.flow.impl.AnalysisSequenceCapabilityNode");

    assertThat(testClass).isNotNull();
    assertThat(testClass.getClassLoader()).isEqualTo(this.getClass().getClassLoader());
  }

  @Test
  void testParallelClassLoading() throws Exception {
    final UIMAClassLoader cl = new UIMAClassLoader(testClassPath, this.getClass().getClassLoader());
    final Class<?>[] loadedClasses = new Class<?>[Misc.numberOfCores];

    MultiThreadUtils.Run2isb callable = new MultiThreadUtils.Run2isb() {
      @Override
      public void call(int threadNumber, int repeatNumber, StringBuilder sb) throws Exception {
        loadedClasses[threadNumber] = cl
                .loadClass("org.apache.uima.internal.util.ClassloadingTestClass");
      }
    };

    MultiThreadUtils.tstMultiThread("MultiThreadLoading", Misc.numberOfCores, 1, callable,
            MultiThreadUtils.emptyReset);
    var c = loadedClasses[0];
    for (int i = 1; i < Misc.numberOfCores; i++) {
      assertThat(loadedClasses[i]).isEqualTo(c);
    }
  }

  @Test
  void testSimpleClassloadingSampleURL() throws Exception {
    var urlClasspath = new URL[] { new File(testClassPath).toURL() };
    UIMAClassLoader cl = new UIMAClassLoader(urlClasspath, this.getClass().getClassLoader());

    var testClass = cl.loadClass("org.apache.uima.internal.util.ClassloadingTestClass");

    assertThat(testClass).isNotNull();
    assertThat(testClass.getClassLoader()).isEqualTo(cl);

    testClass = cl.loadClass("org.apache.uima.flow.impl.AnalysisSequenceCapabilityNode");

    assertThat(testClass).isNotNull();
    assertThat(testClass.getClassLoader()).isEqualTo(this.getClass().getClassLoader());
  }

  @Test
  void testAdvancedClassloadingSampleString() throws Exception {
    var cl = new UIMAClassLoader(testClassPath, this.getClass().getClassLoader());

    var testClass = cl.loadClass("org.apache.uima.internal.util.ClassloadingTestClass");

    assertThat(testClass).isNotNull();
    assertThat(testClass.getClassLoader()).isEqualTo(cl);

    testClass = cl.loadClass("org.apache.uima.flow.impl.AnalysisSequenceCapabilityNode");

    assertThat(testClass).isNotNull();
    assertThat(testClass.getClassLoader()).isEqualTo(this.getClass().getClassLoader());
  }

  @Test
  void testAdvancedClassloadingSampleURL() throws Exception {
    var urlClasspath = new URL[] { new File(testClassPath).toURL() };
    var cl = new UIMAClassLoader(urlClasspath, this.getClass().getClassLoader());

    var testClass = cl.loadClass("org.apache.uima.internal.util.ClassloadingTestClass");

    assertThat(testClass).isNotNull();
    assertThat(testClass.getClassLoader()).isEqualTo(cl);

    testClass = cl.loadClass("org.apache.uima.flow.impl.AnalysisSequenceCapabilityNode");

    assertThat(testClass).isNotNull();
    assertThat(testClass.getClassLoader()).isEqualTo(this.getClass().getClassLoader());
  }

  @Test
  void testLoadingSpiImplementationThroughUIMAClassLoaderIsBlocked() throws Exception {
    var urlClasspath = new URL[] { new File(testClassPath).toURL() };
    var cl = new UIMAClassLoader(urlClasspath, this.getClass().getClassLoader());

    var spiConfigurationFile = "META-INF/services/org.apache.uima.spi.TypeSystemProvider";
    var spiImplementationClass = "org.apache.uima.internal.util.UIMAClassLoaderTest_TypeSystemProvider";
    var filesToCheck = asList(spiConfigurationFile,
            spiImplementationClass.replace('.', '/') + ".class");

    try (var zipFile = new ZipFile(testClassPath)) {
      for (var filePath : filesToCheck) {
        var entry = zipFile.getEntry(filePath);
        assertThat(entry).as("File %s should exist in the JAR", filePath).isNotNull();
      }
    }

    var testClass = cl.loadClass(spiImplementationClass);
    assertThat(testClass).isNotNull();
    assertThat(testClass.getClassLoader()).isEqualTo(this.getClass().getClassLoader());

    var spiConfigurationFiles = list(cl.getResources(spiConfigurationFile));
    assertThat(spiConfigurationFiles).isEmpty();
  }
}
