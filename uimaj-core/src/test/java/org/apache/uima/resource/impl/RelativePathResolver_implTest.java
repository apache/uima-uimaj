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

import static java.lang.String.join;
import static java.lang.System.getProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newOutputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.uima.test.junit_extension.JUnitExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RelativePathResolver_implTest {

  private final static String PATH_SEP = getProperty("path.separator");

  private RelativePathResolver_impl sut;

  @BeforeEach
  void setup() {
    sut = new RelativePathResolver_impl();
  }

  @Nested
  class UrlBasedTests {

    @Test
    void thatPathUrlsAreNotModifiable() throws Exception {
      sut.setDataPathElements(new URL("file:foo"), new URL("file:bar"));

      assertThatExceptionOfType(UnsupportedOperationException.class)
              .as("Path elements should not be modifiable")
              .isThrownBy(() -> sut.getDataPathUrls().add(new URL("file:blah")));
    }

    @SuppressWarnings("deprecation")
    @Test
    void testSetSetDataPathUrls(@TempDir Path temp) throws Exception {
      var zipFile = temp.resolve("test.zip");
      createZipWithTextFiles(zipFile, "bar/bar.txt");

      var zipBaseUrl = new URL("jar:" + zipFile.toUri() + "!/");
      var expected = new URL[] { new URL("file:foo"), zipBaseUrl };
      sut.setDataPathElements(expected);

      assertThat(sut.getDataPathElements()) //
              .containsExactly("foo");

      assertThat(sut.getDataPath()) //
              .isEqualTo("foo");

      assertThat(sut.getDataPathUrls()).containsExactly(expected);

      assertThat(sut.resolveRelativePath("bar/bar.txt"))
              .isEqualTo(new URL(zipBaseUrl, "bar/bar.txt"));
      assertThat(sut.resolveRelativePath("foo/bar.txt")).isNull();
    }

    public static void createZipWithTextFiles(Path aZipPath, String... aFileNames)
            throws IOException {
      createDirectories(aZipPath.getParent());

      try (var zipOut = new ZipOutputStream(newOutputStream(aZipPath))) {
        for (var fileName : aFileNames) {
          zipOut.putNextEntry(new ZipEntry(fileName));
          zipOut.write(("This is the content of " + fileName).getBytes(UTF_8));
          zipOut.closeEntry();
        }
      }
    }
  }

  @Nested
  class FileBasedTests {
    final String element1 = "/this/is/a/test";
    final String element2 = "/another/test";
    final String[] expectedElements = { element1, element2 };
    final String expectedPath = join(PATH_SEP, expectedElements);

    @SuppressWarnings("deprecation")
    @Test
    void testSetDataPath() throws Exception {
      sut.setDataPath(expectedPath);

      assertThatGettersReturnTheRightValues(sut);
    }

    @Test
    void testSetDataPathElements() throws Exception {
      sut.setDataPathElements(expectedElements);

      assertThatGettersReturnTheRightValues(sut);
    }

    @Test
    void testSetDataPathElementsAsFiles() throws Exception {
      var expectedElementFiles = Stream.of(expectedElements).map(File::new).toArray(File[]::new);

      sut.setDataPathElements(expectedElementFiles);

      assertThatGettersReturnTheRightValues(sut);
    }

    @Deprecated(since = "3.6.0")
    @Test
    void thatPathElementsAreNotModifiable() throws Exception {
      sut.setDataPathElements("foo", "bar");

      assertThatExceptionOfType(UnsupportedOperationException.class)
              .as("Path elements should not be modifiable")
              .isThrownBy(() -> sut.getDataPathElements().add("blah"));
    }

    @SuppressWarnings("deprecation")
    void assertThatGettersReturnTheRightValues(RelativePathResolver_impl aResolver) {

      assertThat(aResolver.getDataPathElements()) //
              .containsExactly(expectedElements);

      assertThat(aResolver.getDataPath()) //
              .isEqualTo(expectedPath);

      assertThat(aResolver.getDataPathUrls())
              .containsExactlyElementsOf(Stream.of(expectedElements).map(this::toUrl).toList());
    }

    @SuppressWarnings("deprecation")
    URL toUrl(String path) {
      try {
        return new File(path).toURL();
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException(e);
      }
    }
  }

  @Test
  void testResolveRelativePathUsingString() throws Exception {
    assertThat(sut.resolveRelativePath("file:test/relativePathTest.dat")) //
            .as("File should not be found") //
            .isNull();

    // specify path
    var path = JUnitExtension.getFile("ResourceTest/subdir").getAbsolutePath();
    sut.setDataPathElements(path);

    var absUrl = sut.resolveRelativePath("file:test/relativePathTest.dat");
    assertThat(absUrl) //
            .as("now file should be found") //
            .isNotNull();

    // try resolving an absolute path even with no data path
    sut.setDataPathElements("");

    assertThat(sut.resolveRelativePath(absUrl.toString())).isEqualTo(absUrl);
  }

  @SuppressWarnings("deprecation")
  @Test
  void testResolveRelativePathUsingUrl() throws Exception {
    assertThat(sut.resolveRelativePath(new URL("file:test/relativePathTest.dat"))) //
            .as("File should not be found") //
            .isNull();

    // specify path
    var path = JUnitExtension.getFile("ResourceTest/subdir").getAbsolutePath();
    sut.setDataPathElements(path);

    var absUrl = sut.resolveRelativePath(new URL("file:test/relativePathTest.dat"));
    assertThat(absUrl) //
            .as("now file should be found") //
            .isNotNull();

    // try resolving an absolute path even with no data path
    sut.setDataPathElements("");

    assertThat(sut.resolveRelativePath(absUrl)).isEqualTo(absUrl);
  }
}
