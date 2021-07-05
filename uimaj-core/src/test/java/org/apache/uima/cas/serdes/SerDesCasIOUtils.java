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

import static java.nio.file.Files.newOutputStream;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.SerialFormat.XMI_PRETTY;
import static org.apache.uima.util.TypeSystemUtil.typeSystem2TypeSystemDescription;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.CasLoadMode;
import org.assertj.core.internal.Failures;

public class SerDesCasIOUtils {
  public static void ser(CAS aSourceCas, Path aTargetCasFile, SerialFormat aFormat)
          throws Exception {
    try (OutputStream casTarget = Files.newOutputStream(aTargetCasFile)) {
      CasIOUtils.save(aSourceCas, casTarget, aFormat);
    }
  }

  public static void desser(CAS aBufferCas, Path aSourceCasPath, Path aTargetCasPath,
          SerialFormat aFormat, CasLoadMode aMode, CasLoadOptions... aOptions) throws Exception {
    // Deserialize the file into the buffer CAS
    try (InputStream casSource = Files.newInputStream(aSourceCasPath)) {
      if (asList(aOptions).contains(CasLoadOptions.WITH_TSI)) {
        throw new NotImplementedException("Not implemented yet...");
        // try (InputStream tsiSource = Files.newInputStream(aSourceCasPath)) {
        // CasIOUtils.load(casSource, tsiSource, aBufferCas, aMode);
        // }
      } else {
        CasIOUtils.load(casSource, null, aBufferCas, aMode);
      }
    }

    // Serialize the buffer CAS to the target file
    try (OutputStream casTarget = Files.newOutputStream(aTargetCasPath)) {
      CasIOUtils.save(aBufferCas, casTarget, aFormat);
    }
  }

  public static void serdes(CAS aSourceCas, CAS aTargetCas, SerialFormat aFormat, CasLoadMode aMode,
          CasLoadOptions... aOptions) throws Exception {
    // Serialize the CAS
    byte[] casBuffer;
    byte[] tsiBuffer;
    try (ByteArrayOutputStream casTarget = new ByteArrayOutputStream();
            ByteArrayOutputStream tsiTarget = new ByteArrayOutputStream()) {
      CasIOUtils.save(aSourceCas, casTarget, tsiTarget, aFormat);
      casBuffer = casTarget.toByteArray();
      tsiBuffer = tsiTarget.toByteArray();
    }

    // Deserialize the CAS
    try (ByteArrayInputStream casSource = new ByteArrayInputStream(casBuffer);
            ByteArrayInputStream tsiSource = new ByteArrayInputStream(tsiBuffer)) {
      if (asList(aOptions).contains(CasLoadOptions.WITH_TSI)) {
        CasIOUtils.load(casSource, tsiSource, aTargetCas, aMode);
      } else {
        CasIOUtils.load(casSource, null, aTargetCas, aMode);
      }
    }
  }

  public static void writeXmi(CAS aCas, Path aTarget) {
    // Additionally, serialize the data as XMI and also write the type system
    try (OutputStream out = newOutputStream(aTarget)) {
      CasIOUtils.save(aCas, out, XMI_PRETTY);
    } catch (Throwable e) {
      AssertionError error = Failures.instance().failure("Unable to create debug XMI from CAS");
      error.initCause(e);
      throw error;
    }
  }

  public static void writeTypeSystemDescription(CAS aCas, Path aTarget) {
    // Additionally, serialize the data as XMI and also write the type system
    try (OutputStream out = newOutputStream(aTarget)) {
      typeSystem2TypeSystemDescription(aCas.getTypeSystem()).toXML(out);
    } catch (Throwable e) {
      AssertionError error = Failures.instance()
              .failure("Unable to create debug typesystem from CAS");
      error.initCause(e);
      throw error;
    }
  }

  enum CasLoadOptions {
    WITH_TSI
  }
}
