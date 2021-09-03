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
package org.apache.uima.json.jsoncas2.encoding;

import static java.lang.Character.getName;
import static java.lang.Character.toChars;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.javacrumbs.jsonunit.jsonpath.JsonPathAdapter.inPath;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.serdes.datasuites.ProgrammaticallyCreatedCasDataSuite;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.json.jsoncas2.JsonCas2Serializer;
import org.apache.uima.json.jsoncas2.mode.OffsetConversionMode;
import org.junit.jupiter.api.Test;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;

public class Utf8ByteOffsetConverterTest {
  @Test
  public void thatMappingExternalToInternalWorks() {
    final int U = Utf8ByteOffsetConverter.UNMAPPED;
    int[] externalToInternal = {
        // Smily
        0, U, U, U,
        // " This "
        2, 3, 4, 5, 6, 7,
        // Female light-skinned turban emoji
        8, U, U, U, 10, U, U, U, 12, U, U, 13, U, U, 14, U, U,
        // " is "
        15, 16, 17, 18,
        // Telephone sign
        19, U, U,
        // " a "
        20, 21, 22,
        // Dark skinned bearded emoji
        23, U, U, U, 25, U, U, U, 27, U, U, 28, U, U, 29, U, U,
        // " test "
        30, 31, 32, 33, 34, 35,
        // Ghost emoji
        36, U, U, U,
        // EOS
        38 };
    String text = "🥳 This 👳🏻‍♀️ is ✆ a 🧔🏾‍♂️ test 👻";
    int external_length = text.getBytes(UTF_8).length;
    Utf8ByteOffsetConverter conv = new Utf8ByteOffsetConverter(text);
    for (int n = 0; n < externalToInternal.length; n++) {
      int mn = externalToInternal[n];
      if (n < external_length) {
        if (mn != Utf8ByteOffsetConverter.UNMAPPED) {
          int cp = text.codePointAt(mn);
          String s = String.valueOf(toChars(cp));
          int w = s.getBytes(UTF_8).length;
          System.out.printf("%2d->%2d: U+%05X (%d) %s %s%n", n, mn, cp, w, s, getName(cp));
        }
      } else {
        System.out.printf("%2d->%2d: EOS%n", n, mn);
      }

      assertThat(conv.mapExternal(n)).isEqualTo(mn);
    }
  }

  @Test
  public void thatMappingInternalToExternalWorks() {
    final int U = Utf8ByteOffsetConverter.UNMAPPED;
    int[] internalToExternal = {
        // Smily
        0, U,
        // " This "
        4, 5, 6, 7, 8, 9,
        // Female light-skinned turban emoji
        10, U, 14, U, 18, 21, 24,
        // " is "
        27, 28, 29, 30,
        // Telephone sign
        31,
        // " a "
        34, 35, 36,
        // Dark skinned bearded emoji
        37, U, 41, U, 45, 48, 51,
        // " test "
        54, 55, 56, 57, 58, 59,
        // Ghost emoji
        60, U,
        // EOS
        64 };
    String text = "🥳 This 👳🏻‍♀️ is ✆ a 🧔🏾‍♂️ test 👻";
    int cul = text.length();
    Utf8ByteOffsetConverter conv = new Utf8ByteOffsetConverter(text);
    for (int n = 0; n < internalToExternal.length; n++) {
      int mn = internalToExternal[n];
      if (n < cul) {
        if (mn != Utf8ByteOffsetConverter.UNMAPPED) {
          int cp = text.codePointAt(n);
          String s = String.valueOf(toChars(cp));
          int w = String.valueOf(Character.toChars(cp)).getBytes(UTF_8).length;
          // System.out.printf("%2d->%2d: U+%05X (%d) %s %s%n", n, mn, cp, w, s, getName(cp));
        }
      } else {
        // System.out.printf("%2d->%2d: EOS%n", n, mn);
      }

      assertThat(conv.mapInternal(n)).isEqualTo(mn);
    }
  }

  @Test
  public void thatSerializationWithMappingWorks() throws Exception {
    JsonCas2Serializer ser = new JsonCas2Serializer();
    ser.setOffsetConversionMode(OffsetConversionMode.UTF_8);

    CAS cas = ProgrammaticallyCreatedCasDataSuite.casWithEmojiUnicodeTextAndAnnotations();

    String casJson;
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      ser.serialize(cas, os);
      casJson = new String(os.toByteArray(), UTF_8);
    }

    String expected = String.join(",",
            "{'%ID':1,'%TYPE':'uima.cas.Sofa','sofaNum':1,'sofaID':'_InitialView','mimeType':'text','sofaString':'🥳 This 👳🏻‍♀️ is ✆ a 🧔🏾‍♂️ test 👻'}",
            "{'%ID':2,'%TYPE':'uima.tcas.Annotation','@sofa':1,'begin':0,'end':4}",
            "{'%ID':3,'%TYPE':'uima.tcas.Annotation','@sofa':1,'begin':5,'end':9}",
            "{'%ID':4,'%TYPE':'uima.tcas.Annotation','@sofa':1,'begin':10,'end':27}",
            "{'%ID':5,'%TYPE':'uima.tcas.Annotation','@sofa':1,'begin':28,'end':30}",
            "{'%ID':6,'%TYPE':'uima.tcas.Annotation','@sofa':1,'begin':31,'end':34}",
            "{'%ID':7,'%TYPE':'uima.tcas.Annotation','@sofa':1,'begin':35,'end':36}",
            "{'%ID':8,'%TYPE':'uima.tcas.Annotation','@sofa':1,'begin':37,'end':54}",
            "{'%ID':9,'%TYPE':'uima.tcas.Annotation','@sofa':1,'begin':55,'end':59}",
            "{'%ID':10,'%TYPE':'uima.tcas.Annotation','@sofa':1,'begin':60,'end':64}",
            "{'%ID':11,'%TYPE':'uima.tcas.DocumentAnnotation','@sofa':1,'begin':0,'end':64,'language':'x-unspecified'}");
    expected = "[" + expected + "]";

    JsonAssertions.assertThatJson(inPath(casJson, "$.%FEATURE_STRUCTURES[*])")) //
            .isEqualTo(expected);
  }

  private static void createAnnotatedText(CAS aCas, StringBuilder aBuffer, String aText,
          String... aSuffix) {
    int begin = aBuffer.length();
    aBuffer.append(aText);
    AnnotationFS a = aCas.createAnnotation(aCas.getAnnotationType(), begin, aBuffer.length());
    aCas.addFsToIndexes(a);
    for (String s : aSuffix) {
      aBuffer.append(s);
    }
  }
}
