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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.apache.uima.internal.util.ServiceLoaderUtil.loadServicesSafely;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import org.apache.uima.spi.TypeSystemProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServiceLoaderUtilTest {

  @Test
  void testLoadingNonExistingService(@TempDir File aTemp) throws Exception {
    writeStringToFile(new File(aTemp, "META-INF/services/" + TypeSystemProvider.class.getName()),
            "ClassThatDoesNotExist", UTF_8);

    var cl = new URLClassLoader(new URL[] { aTemp.toURL() });

    var errors = new ArrayList<Throwable>();
    var services = new ArrayList<TypeSystemProvider>();
    loadServicesSafely(TypeSystemProvider.class, cl, errors).forEach(services::add);
    assertThat(services).isEmpty();
    assertThat(errors).hasSize(1);
  }

  @Test
  void testLoadingService(@TempDir File aTemp) throws Exception {
    writeStringToFile(new File(aTemp, "META-INF/services/" + TypeSystemProvider.class.getName()),
            UIMAClassLoaderTest_TypeSystemProvider.class.getName(), UTF_8);

    var cl = new URLClassLoader(new URL[] { aTemp.toURL() }, getClass().getClassLoader());

    var errors = new ArrayList<Throwable>();
    var services = new ArrayList<TypeSystemProvider>();
    loadServicesSafely(TypeSystemProvider.class, cl, errors).forEach(services::add);
    assertThat(services) //
            .hasSize(1) //
            .extracting(t -> t.getClass(), t -> t.getClass().getClassLoader()) //
            .containsExactly(tuple(UIMAClassLoaderTest_TypeSystemProvider.class,
                    getClass().getClassLoader()));
    assertThat(errors).isEmpty();
  }
}
