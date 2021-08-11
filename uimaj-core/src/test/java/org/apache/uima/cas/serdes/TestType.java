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
  /**
   * Test creates a CAS in-memory (or obtains it from somewhere custom), serializes it and compares
   * the serialized version to a reference file. This is used mainly to validate the serialization
   * mechanism.
   */
  SER_REF(null, "ser-ref", "ser-ref"), //

  /**
   * Test deserializes a CAS from storage, then serializes it again and then compares it to a
   * reference file. The reference file is typically different from the original file. This is
   * typically used when the input file has a different format than the output file (e.g. read from
   * XMI and write to JSON). It can also be used for cases where there are acceptable differences
   * between the input and the output even if they technically are in the same format (e.g.
   * ignorable whitespace in XMI files).
   */
  ONE_WAY("ser-ref", "one-way", "one-way"), //

  /**
   * Test uses uses the input file as the reference file. Optimally, a (de)serialization mechanism
   * should be fully round-trip capable - i.e. when it reads a file and writes it back out, the
   * output should be exactly the same as the input.
   */
  ROUND_TRIP("ser-ref", "round-trip", "ser-ref"), //

  /**
   * Test deserializes a CAS from storage, then <b>typically applies some additional logic</b>, then
   * serializes it again and then compares it to a reference file. This is used mainly to validate
   * the derserialization mechanism for special cases that cannot be covered by {@link #ONE_WAY} or
   * {@link #ROUND_TRIP}.
   */
  DES_REF(null, "des-ref", "des-ref"),

  /**
   * Test serializes a CAS object from memory and then de-serializes it again. The target folder is
   * used to store the intermediate representation for debugging purposes.
   */
  SER_DES(null, "ser-des", null);

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
