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

package org.apache.uima.pear.util;

import static org.apache.uima.pear.tools.InstallationController.TestStatus.TEST_SUCCESSFUL;
import static org.apache.uima.test.junit_extension.JUnitExtension.getFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.pear.tools.InstallationController.TestStatus;
import org.apache.uima.pear.tools.InstallationTester;
import org.apache.uima.pear.tools.PackageBrowser;
import org.apache.uima.pear.tools.PackageInstaller;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test the pear installation verification
 */
public class PearInstallationVerificationTest {
  @Test
  public void testAePearVerification(@TempDir File temp) throws Exception {
    assertThatPearInstalls(getFile("pearTests/analysisEngine.pear"), temp);
  }

  @Test
  public void testCcPearVerification(@TempDir File temp) throws Exception {
    assertThatPearInstalls(getFile("pearTests/casConsumer.pear"), temp);
  }

  @Test
  public void testTsPearVerification(@TempDir File temp) throws Exception {
    assertThatPearInstalls(getFile("pearTests/typeSystem.pear"), temp);
  }

  // TODO: create testcases for ci, cr, cpe pear packages

  @Test
  public void thatSpecialXmlCharactersInTargetPathDoNotBreakInstallation(@TempDir File temp)
          throws Exception {
    File folder = new File(temp, "!'&");
    folder.mkdirs();
    assertThatPearInstalls(getFile("pearTests/analysisEngine.pear"),
            // on windows, can't use these chars
            // <>:"/\|?*
            folder);
  }

  private void assertThatPearInstalls(File pearFile, File targetDir) throws InvalidXMLException,
          ResourceInitializationException, UIMARuntimeException, UIMAException, IOException {
    assertThat(pearFile).as("PEAR file %s not found", pearFile).isNotNull();

    // Install PEAR package without verification
    PackageBrowser instPear = PackageInstaller.installPackage(targetDir, pearFile, false);

    // Check package browser
    assertThat(instPear).as("PackageBrowser is null").isNotNull();

    InstallationTester installTester = new InstallationTester(instPear);
    TestStatus status = installTester.doTest();

    assertThat(status.getRetCode()).isEqualTo(TEST_SUCCESSFUL);
  }
}
