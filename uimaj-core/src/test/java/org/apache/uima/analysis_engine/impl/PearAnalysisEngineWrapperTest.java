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
package org.apache.uima.analysis_engine.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.uima.pear.tools.PackageBrowser;
import org.apache.uima.pear.tools.PackageInstaller;
import org.apache.uima.pear.util.FileUtil;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.PearSpecifier;
import org.apache.uima.resource.impl.Parameter_impl;
import org.apache.uima.resource.impl.PearSpecifier_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.junit.Assert;

import junit.framework.TestCase;

public class PearAnalysisEngineWrapperTest extends TestCase {

  private final static String PARAMETER_NAME = "StringParam";

  private final static String PARAMETER_VALUE = "test";

  private final static String PARAMETER_VALUE_OVERRIDE = "testOverride";

  private PearAnalysisEngineWrapper pearAnalysisEngineWrapper;

  private File tempInstallDirectory = null;

  private PackageBrowser installedPearPackage;

  public PearAnalysisEngineWrapperTest(String arg0) {

    super(arg0);
  }

  @Override
  protected void setUp() throws Exception {

    this.pearAnalysisEngineWrapper = new PearAnalysisEngineWrapper();

    this.tempInstallDirectory = this.createInstallationDirectory();

    this.installedPearPackage = this.installPearPackage();
  }

  @Override
  protected void tearDown() throws Exception {
    this.removeInstallationDirectory();
  }

  public void testInitializeWithOverride() throws Exception {

    PearSpecifier pearSpecifier = this.createPearSpecifierWithParameters();

    boolean initialized = this.pearAnalysisEngineWrapper.initialize(pearSpecifier,
            new HashMap<String, Object>());

    Assert.assertTrue("Pear was not initialized", initialized);

    String stringParamValue = (String) this.pearAnalysisEngineWrapper
            .getConfigParameterValue(PearAnalysisEngineWrapperTest.PARAMETER_NAME);

    Assert.assertEquals("The value of StringParam was not overridden",
            PearAnalysisEngineWrapperTest.PARAMETER_VALUE_OVERRIDE, stringParamValue);
  }

  private PearSpecifier createPearSpecifierWithParameters() {

    Parameter parameterStringParam = new Parameter_impl();
    parameterStringParam.setName(PearAnalysisEngineWrapperTest.PARAMETER_NAME);
    parameterStringParam.setValue(PearAnalysisEngineWrapperTest.PARAMETER_VALUE_OVERRIDE);

    PearSpecifier_impl pearSpecifier_impl = new PearSpecifier_impl();
    pearSpecifier_impl.setPearPath(this.installedPearPackage.getRootDirectory().toString());
    pearSpecifier_impl.setParameters(new Parameter[] { parameterStringParam });
    return pearSpecifier_impl;
  }

  public void testInitializeWithoutOverride() throws Exception {

    PearSpecifier pearSpecifier = this.createPearSpecifierWithoutParameters();

    boolean initialized = this.pearAnalysisEngineWrapper.initialize(pearSpecifier,
            new HashMap<String, Object>());

    Assert.assertTrue("Pear was not initialized", initialized);

    String stringParamValue = (String) this.pearAnalysisEngineWrapper
            .getConfigParameterValue(PearAnalysisEngineWrapperTest.PARAMETER_NAME);

    Assert.assertEquals("The value of StringParam has changed",
            PearAnalysisEngineWrapperTest.PARAMETER_VALUE, stringParamValue);
  }

  private PearSpecifier createPearSpecifierWithoutParameters() {

    PearSpecifier_impl pearSpecifier_impl = new PearSpecifier_impl();
    pearSpecifier_impl.setPearPath(this.installedPearPackage.getRootDirectory().toString());
    return pearSpecifier_impl;
  }

  private File createInstallationDirectory() throws IOException {

    File tempDirectory = File.createTempFile("pear_verification_test_", "tmp");
    if (tempDirectory.delete()) {

      if (!tempDirectory.mkdirs()) {
        throw new IllegalStateException(
                "Tmp directory (" + tempDirectory + ") could not be created");
      }
    }

    return tempDirectory;
  }

  private PackageBrowser installPearPackage() {
    File pearFile = JUnitExtension.getFile("pearTests/analysisEngineWithParameters.pear");
    Assert.assertNotNull("analysisEngine.pear file not found", pearFile);

    return PackageInstaller.installPackage(this.tempInstallDirectory, pearFile, false);
  }

  private void removeInstallationDirectory() throws IOException {
    if (this.tempInstallDirectory != null) {
      FileUtil.deleteDirectory(this.tempInstallDirectory);
    }
  }
}
