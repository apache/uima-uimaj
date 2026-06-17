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
package org.apache.uima.jcas.impl;

import static java.lang.System.getProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;

/**
 * Reproduces <a href="https://github.com/apache/uima-uimaj/issues/234">issue #234</a>: when a
 * built-in Annotation cover class is touched before any CAS exists, the type system records a wrong
 * (zero) jcasType for it and Annotation's feature callsites are never updated. Subsequent
 * {@code getCasType} / {@code setBegin} calls then fail.
 *
 * <p>
 * The bug only manifests in a fresh JVM where nothing has yet initialized {@link Annotation}. Class
 * initialization is once-per-JVM, so running this in the shared Surefire JVM (where other tests
 * have already created CASes and thereby initialized Annotation normally) would not reproduce it.
 * The test therefore forks a dedicated JVM running {@link #main(String[])} and asserts on its exit
 * code.
 */
public class LoadingBuiltinAnnotationBeforeCasTest {

  /**
   * Reproducer body. Must run in a fresh JVM (see the {@code @Test} below). Throws on failure so
   * the JVM exits non-zero.
   */
  public static void main(String[] args) throws Exception {
    // Trigger Annotation's <clinit> before any TypeSystemImpl/CAS exists -- the bug repro hinge.
    Class.forName(Annotation.class.getName());

    var tsd = new TypeSystemDescription_impl();
    var jcas = CasCreationUtils.createCas(tsd, null, null).getJCas();

    // (1) wrong jcasRegisteredTypes slot
    jcas.getCasType(Annotation.type);

    // (2) Annotation._FC_begin callsite stuck at -1
    jcas.setDocumentText("hello");
  }

  @Test
  void thatLoadingAnnotationBeforeCasDoesNotBreakTypeSystemManagement() throws Exception {
    // Fork the same JVM that is running this test. ProcessHandle is best-effort, hence the
    // fallback; the fallback path works on Windows too because CreateProcess auto-resolves .exe.
    var java = ProcessHandle.current().info().command()
            .orElse(getProperty("java.home") + "/bin/java");

    var pb = new ProcessBuilder(java, "-cp", getProperty("java.class.path"), getClass().getName());
    pb.redirectErrorStream(true);
    var p = pb.start();

    // Drain the subprocess output on a daemon thread so that a deadlocked subprocess that never
    // emits EOF can't block this thread from reaching the timed waitFor below.
    var buf = new ByteArrayOutputStream();
    var drain = new Thread(() -> {
      try {
        p.getInputStream().transferTo(buf);
      } catch (Exception ignored) {
        // Subprocess died mid-transfer; whatever we captured is what we report.
      }
    }, "forked-jvm-output-drain");
    drain.setDaemon(true);
    drain.start();

    if (!p.waitFor(60, TimeUnit.SECONDS)) {
      p.destroyForcibly();
      drain.join(5_000);
      fail("forked JVM did not exit within 60s; partial output was:%n%s",
              buf.toString(StandardCharsets.UTF_8));
    }
    drain.join(5_000);

    assertThat(p.exitValue())
            .as("forked JVM exited non-zero; subprocess output was:%n%s",
                    buf.toString(StandardCharsets.UTF_8))
            .isZero();
  }
}
