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
package org.apache.uima.cas.serdes;

import java.nio.file.Path;
import java.nio.file.Paths;

public enum TestType {
  ROUND_TRIP("ser-ref", "round-trip", "ser-ref"), //
  SER_REF(null, "ser-ref", "ser-ref"), //
  ONE_WAY("ser-ref", "one-way", "one-way");

  private String sourceFolderName;
  private String referenceFolderName;
  private String targetFolderName;

  TestType(String aSourceFolderName, String aTargetFolderName, String aReferenceFolderName) {
    sourceFolderName = aSourceFolderName;
    referenceFolderName = aReferenceFolderName;
    targetFolderName = aTargetFolderName;
  }

  public String getSourceFolderName() {
    if (sourceFolderName == null) {
      throw new IllegalStateException("Test type " + this + " does not define a source folder");
    }

    return sourceFolderName;
  }

  public String getReferenceFolderName() {
    return referenceFolderName;
  }

  public String getTargetFolderName() {
    return targetFolderName;
  }

  public Path getSourceFolder(Class<?> aTestClass) {
    return Paths.get("src", "test", "resources", aTestClass.getSimpleName(), getSourceFolderName());
  }

  public Path getTargetFolder(Class<?> aTestClass) {
    return Paths.get("target", "test-output", aTestClass.getSimpleName(), getTargetFolderName());
  }

  public Path getReferenceFolder(Class<?> aTestClass) {
    return Paths.get("src", "test", "resources", aTestClass.getSimpleName(),
            getReferenceFolderName());
  }
}
