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

package org.apache.uima.resource.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.stream.Stream;

import org.apache.uima.test.junit_extension.JUnitExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RelativePathResolver_implTest {

  private final static String PATH_SEP = System.getProperty("path.separator");
  private RelativePathResolver_impl sut;

  private final static String[] expectedElements = { "/this/is/a/test", "/another/test" };
  private final static String expectedPath = String.join(PATH_SEP, expectedElements);

  @BeforeEach
  public void setup() {
    sut = new RelativePathResolver_impl();
  }

  @Test
  public void thatPathElementsAreNotModifiable() throws Exception {
    sut.setDataPathElements(expectedPath);

    assertThatExceptionOfType(UnsupportedOperationException.class)
            .as("Path elements should not be modifiable")
            .isThrownBy(() -> sut.getDataPathElements().add("blah"));
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testSetDataPath() throws Exception {
    sut.setDataPath(expectedPath);

    assertThatGettersReturnTheRightValues(sut);
  }

  @Test
  public void testSetDataPathElements() throws Exception {
    sut.setDataPathElements(expectedElements);

    assertThatGettersReturnTheRightValues(sut);
  }

  @Test
  public void testSetDataPathElementsAsFiles() throws Exception {
    File[] expectedElementFiles = Stream.of(expectedElements).map(File::new).toArray(File[]::new);

    sut.setDataPathElements(expectedElementFiles);

    assertThatGettersReturnTheRightValues(sut);
  }

  @Test
  public void testResolveRelativePath() throws Exception {
    // file should not be found
    assertThat(sut.resolveRelativePath(new URL("file:test/relativePathTest.dat"))) //
            .as("File should not be found") //
            .isNull();

    // specify path
    String path = JUnitExtension.getFile("ResourceTest/subdir").getAbsolutePath();
    sut.setDataPathElements(path);

    URL absUrl = sut.resolveRelativePath(new URL("file:test/relativePathTest.dat"));
    assertThat(absUrl) //
            .as("now file should be found") //
            .isNotNull();

    // try resolving an absolute path even with no data path
    sut.setDataPathElements("");

    assertThat(sut.resolveRelativePath(absUrl)).isEqualTo(absUrl);
  }

  @SuppressWarnings("deprecation")
  private void assertThatGettersReturnTheRightValues(RelativePathResolver_impl aResolver) {
    assertThat(aResolver.getDataPathElements()) //
            .containsExactly(expectedElements);

    assertThat(aResolver.getDataPath()) //
            .isEqualTo(expectedPath);

    assertThat(aResolver.getBaseUrls())
            .containsExactly(Stream.of(expectedElements).map(this::toUrl).toArray(URL[]::new));
  }

  @SuppressWarnings("deprecation")
  private URL toUrl(String path) {
    try {
      return new File(path).toURL();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
