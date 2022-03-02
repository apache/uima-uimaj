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
package org.apache.uima.fit.component.initialize;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.collections4.SetUtils.unmodifiableSet;
import static org.apache.uima.fit.component.initialize.ConfigurationParameterInitializer.initialize;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.testAes.Annotator1;
import org.apache.uima.fit.factory.testAes.ParameterizedAE;
import org.apache.uima.fit.factory.testAes.ParameterizedAE.EnumValue;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.impl.ConfigurationParameterSettings_impl;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

public class ConfigurationParameterInitializerTest extends ComponentTestBase {

  public static class PrimitiveTypesInjection {
    private @ConfigurationParameter int intValue;

    private @ConfigurationParameter boolean booleanValue;

    private @ConfigurationParameter String stringValue;

    private @ConfigurationParameter float floatValue;

    private @ConfigurationParameter double doubleValue;
  }

  @Test
  public void thatPrimitiveTypesCanBeInjected() throws Exception {
    PrimitiveTypesInjection target = new PrimitiveTypesInjection();

    initialize(target, "intValue", 1, "booleanValue", true, "stringValue", "test", "floatValue",
            1.234f, "doubleValue", 1.234d);

    assertThat(target.intValue).isEqualTo(1);
    assertThat(target.booleanValue).isEqualTo(true);
    assertThat(target.stringValue).isEqualTo("test");
    assertThat(target.floatValue).isEqualTo(1.234f);
    assertThat(target.doubleValue).isEqualTo(1.234d);
  }

  public static class PrimitiveArraysInjection {
    private @ConfigurationParameter int[] intValues;

    private @ConfigurationParameter boolean[] booleanValues;

    private @ConfigurationParameter String[] stringValues;

    private @ConfigurationParameter float[] floatValues;

    private @ConfigurationParameter double[] doubleValues;
  }

  @Test
  public void thatPrimitiveArraysCanBeInjected() throws Exception {
    PrimitiveArraysInjection target = new PrimitiveArraysInjection();

    initialize(target, "intValues", new int[] { 1, 2, 3 }, "booleanValues",
            new boolean[] { true, false, true }, "stringValues",
            new String[] { "test1", "test2", "test3" }, "floatValues",
            new float[] { 1.234f, 2.468f, 3.456f }, "doubleValues",
            new double[] { 1.234d, 2.468d, 3.456d });

    assertThat(target.intValues).containsExactly(1, 2, 3);
    assertThat(target.booleanValues).containsExactly(true, false, true);
    assertThat(target.stringValues).containsExactly("test1", "test2", "test3");
    assertThat(target.floatValues).containsExactly(1.234f, 2.468f, 3.456f);
    assertThat(target.doubleValues).containsExactly(1.234d, 2.468d, 3.456d);
  }

  @Test
  public void thatPrimitiveArraysCanBeInjectedAsLists() throws Exception {
    PrimitiveArraysInjection target = new PrimitiveArraysInjection();

    initialize(target, "intValues", asList(1, 2, 3), "booleanValues", asList(true, false, true),
            "stringValues", asList("test1", "test2", "test3"), "floatValues",
            asList(1.234f, 2.468f, 3.456f), "doubleValues", asList(1.234d, 2.468d, 3.456d));

    assertThat(target.intValues).containsExactly(1, 2, 3);
    assertThat(target.booleanValues).containsExactly(true, false, true);
    assertThat(target.stringValues).containsExactly("test1", "test2", "test3");
    assertThat(target.floatValues).containsExactly(1.234f, 2.468f, 3.456f);
    assertThat(target.doubleValues).containsExactly(1.234d, 2.468d, 3.456d);
  }

  @Test
  public void thatPrimitiveArraysCanBeInjectedAsValues() throws Exception {
    PrimitiveArraysInjection target = new PrimitiveArraysInjection();

    initialize(target, "intValues", 1, "booleanValues", true, "stringValues", "test", "floatValues",
            1.234f, "doubleValues", 1.234d);

    assertThat(target.intValues).containsExactly(1);
    assertThat(target.booleanValues).containsExactly(true);
    assertThat(target.stringValues).containsExactly("test");
    assertThat(target.floatValues).containsExactly(1.234f);
    assertThat(target.doubleValues).containsExactly(1.234d);
  }

  public static class FileInjection {
    private @ConfigurationParameter File file;

    private @ConfigurationParameter File fileFromString;

    private @ConfigurationParameter File[] fileArray;

    private @ConfigurationParameter File[] fileArrayFromString;

    private @ConfigurationParameter List<File> fileList;

    private @ConfigurationParameter List<File> fileFromStringList;

    private @ConfigurationParameter Set<File> fileSet;
  }

  @Test
  public void thatFileObjectCanBeInjected() throws Exception {
    FileInjection target = new FileInjection();

    initialize(target, "file", new File("test"), "fileFromString", "test", "fileArray",
            new File[] { new File("test1"), new File("test2") }, "fileArrayFromString",
            new String[] { "test1", "test2" }, "fileList",
            asList(new File("test1"), new File("test2")), "fileSet",
            unmodifiableSet(new File("test1"), new File("test2")), "fileFromStringList",
            asList("test1", "test2"));

    assertThat(target.file).hasName("test");
    assertThat(target.fileFromString).hasName("test");
    assertThat(target.fileArray).extracting(File::getName).containsExactly("test1", "test2");
    assertThat(target.fileArrayFromString).extracting(File::getName).containsExactly("test1",
            "test2");
    assertThat(target.fileList).extracting(File::getName).containsExactly("test1", "test2");
    assertThat(target.fileFromStringList).extracting(File::getName).containsExactly("test1",
            "test2");
    assertThat(target.fileSet).extracting(File::getName).containsExactlyInAnyOrder("test1",
            "test2");
  }

  public static class ClassInjection {
    private @ConfigurationParameter Class<?> clazz;

    private @ConfigurationParameter Class<?> clazzFromString;

    private @ConfigurationParameter Class<?>[] clazzArray;

    private @ConfigurationParameter Class<?>[] clazzArrayFromString;

    private @ConfigurationParameter List<Class<?>> clazzList;

    private @ConfigurationParameter List<Class<?>> clazzListFromString;

    private @ConfigurationParameter Set<Class<?>> clazzSet;
  }

  @Test
  public void thatClassObjectCanBeInjected() throws Exception {
    ClassInjection target = new ClassInjection();

    initialize(target, "clazz", Integer.class, "clazzFromString", Integer.class.getName(),
            "clazzArray", new Class<?>[] { Integer.class, Boolean.class, Float.class },
            "clazzArrayFromString",
            new String[] { Integer.class.getName(), Boolean.class.getName(),
                Float.class.getName() },
            "clazzList", asList(Integer.class, Boolean.class, Float.class), "clazzListFromString",
            asList(Integer.class.getName(), Boolean.class.getName(), Float.class.getName()),
            "clazzSet", unmodifiableSet(Integer.class, Boolean.class, Float.class));

    assertThat(target.clazz).isEqualTo(Integer.class);
    assertThat(target.clazzFromString).isEqualTo(Integer.class);
    assertThat(target.clazzArray).containsExactly(Integer.class, Boolean.class, Float.class);
    assertThat(target.clazzArrayFromString).containsExactly(Integer.class, Boolean.class,
            Float.class);
    assertThat(target.clazzList).containsExactly(Integer.class, Boolean.class, Float.class);
    assertThat(target.clazzListFromString).containsExactly(Integer.class, Boolean.class,
            Float.class);
    assertThat(target.clazzSet).containsExactlyInAnyOrder(Integer.class, Boolean.class,
            Float.class);
  }

  public static class URIInjection {
    private @ConfigurationParameter URI uri;

    private @ConfigurationParameter URI uriFromString;

    private @ConfigurationParameter URI[] uriArray;

    private @ConfigurationParameter URI[] uriArrayFromString;

    private @ConfigurationParameter List<URI> uriList;

    private @ConfigurationParameter List<URI> uriListFromString;

    private @ConfigurationParameter Set<URI> uriSet;
  }

  @Test
  public void thatURICanBeInjected() throws Exception {
    URIInjection target = new URIInjection();

    initialize(target, "uri", URI.create("file:test"), "uriFromString", "file:test", "uriArray",
            new URI[] { URI.create("file:test1"), URI.create("file:test2"),
                URI.create("file:test3") },
            "uriArrayFromString", new String[] { "file:test1", "file:test2", "file:test3" },
            "uriList",
            asList(URI.create("file:test1"), URI.create("file:test2"), URI.create("file:test3")),
            "uriListFromString", asList("file:test1", "file:test2", "file:test3"), "uriSet",
            unmodifiableSet(URI.create("file:test1"), URI.create("file:test2"),
                    URI.create("file:test3")));

    assertThat(target.uri).isEqualTo(URI.create("file:test"));
    assertThat(target.uriFromString).isEqualTo(URI.create("file:test"));
    assertThat(target.uriArray).containsExactly(URI.create("file:test1"), URI.create("file:test2"),
            URI.create("file:test3"));
    assertThat(target.uriArrayFromString).containsExactly(URI.create("file:test1"),
            URI.create("file:test2"), URI.create("file:test3"));
    assertThat(target.uriList).containsExactly(URI.create("file:test1"), URI.create("file:test2"),
            URI.create("file:test3"));
    assertThat(target.uriListFromString).containsExactly(URI.create("file:test1"),
            URI.create("file:test2"), URI.create("file:test3"));
    assertThat(target.uriSet).containsExactlyInAnyOrder(URI.create("file:test1"),
            URI.create("file:test2"), URI.create("file:test3"));
  }

  public static class URLInjection {
    private @ConfigurationParameter URL URL;

    private @ConfigurationParameter URL URLFromString;

    private @ConfigurationParameter URL[] URLArray;

    private @ConfigurationParameter URL[] URLArrayFromString;

    private @ConfigurationParameter List<URL> URLList;

    private @ConfigurationParameter List<URL> URLListFromString;

    private @ConfigurationParameter Set<URL> URLSet;
  }

  @Test
  public void thatURLCanBeInjected() throws Exception {
    URLInjection target = new URLInjection();

    initialize(target, "URL", new URL("file:test"), "URLFromString", "file:test", "URLArray",
            new URL[] { new URL("file:test1"), new URL("file:test2"), new URL("file:test3") },
            "URLArrayFromString", new String[] { "file:test1", "file:test2", "file:test3" },
            "URLList", asList(new URL("file:test1"), new URL("file:test2"), new URL("file:test3")),
            "URLListFromString", asList("file:test1", "file:test2", "file:test3"), "URLSet",
            unmodifiableSet(new URL("file:test1"), new URL("file:test2"), new URL("file:test3")));

    assertThat(target.URL).isEqualTo(new URL("file:test"));
    assertThat(target.URLFromString).isEqualTo(new URL("file:test"));
    assertThat(target.URLArray).containsExactly(new URL("file:test1"), new URL("file:test2"),
            new URL("file:test3"));
    assertThat(target.URLArrayFromString).containsExactly(new URL("file:test1"),
            new URL("file:test2"), new URL("file:test3"));
    assertThat(target.URLList).containsExactly(new URL("file:test1"), new URL("file:test2"),
            new URL("file:test3"));
    assertThat(target.URLListFromString).containsExactly(new URL("file:test1"),
            new URL("file:test2"), new URL("file:test3"));
    assertThat(target.URLSet).containsExactlyInAnyOrder(new URL("file:test1"),
            new URL("file:test2"), new URL("file:test3"));
  }

  public static class PatternInjection {
    private @ConfigurationParameter Pattern pattern;

    private @ConfigurationParameter Pattern patternFromString;

    private @ConfigurationParameter Pattern[] patternArray;

    private @ConfigurationParameter Pattern[] patternArrayFromString;

    private @ConfigurationParameter List<Pattern> patternList;

    private @ConfigurationParameter List<Pattern> patternListFromString;
  }

  @Test
  public void thatPatternCanBeInjected() throws Exception {
    PatternInjection target = new PatternInjection();

    initialize(target, "pattern", compile("^test$"), "patternFromString", "^test$", "patternArray",
            new Pattern[] { compile("test1"), compile("test2"), compile("test3") },
            "patternArrayFromString", new String[] { "test1", "test2", "test3" }, "patternList",
            asList(compile("test1"), compile("test2"), compile("test3")), "patternListFromString",
            asList("test1", "test2", "test3"));

    assertThat(target.pattern).matches(p -> p.matcher("test").matches());
    assertThat(target.patternFromString).matches(p -> p.matcher("test").matches());
    assertThat(target.patternArray).hasSize(3);
    assertThat(target.patternArray[0]).matches(p -> p.matcher("test1").matches());
    assertThat(target.patternArray[1]).matches(p -> p.matcher("test2").matches());
    assertThat(target.patternArray[2]).matches(p -> p.matcher("test3").matches());
    assertThat(target.patternArrayFromString).hasSize(3);
    assertThat(target.patternArrayFromString[0]).matches(p -> p.matcher("test1").matches());
    assertThat(target.patternArrayFromString[1]).matches(p -> p.matcher("test2").matches());
    assertThat(target.patternArrayFromString[2]).matches(p -> p.matcher("test3").matches());
    assertThat(target.patternList).hasSize(3);
    assertThat(target.patternList.get(0)).matches(p -> p.matcher("test1").matches());
    assertThat(target.patternList.get(1)).matches(p -> p.matcher("test2").matches());
    assertThat(target.patternList.get(2)).matches(p -> p.matcher("test3").matches());
    assertThat(target.patternList).hasSize(3);
    assertThat(target.patternListFromString.get(0)).matches(p -> p.matcher("test1").matches());
    assertThat(target.patternListFromString.get(1)).matches(p -> p.matcher("test2").matches());
    assertThat(target.patternListFromString.get(2)).matches(p -> p.matcher("test3").matches());
  }

  public static class CharsetInjection {
    private @ConfigurationParameter Charset charset;

    private @ConfigurationParameter String charsetAsString;

    private @ConfigurationParameter Charset charsetFromString;

    private @ConfigurationParameter Charset[] charsetArray;

    private @ConfigurationParameter String[] charsetsAsStringArray;

    private @ConfigurationParameter Charset[] charsetArrayFromString;

    private @ConfigurationParameter List<Charset> charsetList;

    private @ConfigurationParameter List<String> charsetAsStringList;

    private @ConfigurationParameter List<Charset> charsetListFromString;

    private @ConfigurationParameter Set<Charset> charsetSet;
  }

  @Test
  public void thatCharsetCanBeInjected() throws Exception {
    CharsetInjection target = new CharsetInjection();

    initialize(target, "charset", UTF_8, "charsetAsString", UTF_8, "charsetFromString",
            UTF_8.toString(), "charsetArray", new Charset[] { UTF_8, UTF_16, US_ASCII },
            "charsetsAsStringArray", new Charset[] { UTF_8, UTF_16, US_ASCII },
            "charsetArrayFromString",
            new String[] { UTF_8.toString(), UTF_16.toString(), US_ASCII.toString() },
            "charsetList", asList(UTF_8, UTF_16, US_ASCII), "charsetAsStringList",
            asList(UTF_8, UTF_16, US_ASCII), "charsetListFromString",
            asList(UTF_8.toString(), UTF_16.toString(), US_ASCII.toString()), "charsetSet",
            unmodifiableSet(UTF_8, UTF_16, US_ASCII));

    assertThat(target.charset).isEqualTo(UTF_8);
    assertThat(target.charsetAsString).isEqualTo(UTF_8.toString());
    assertThat(target.charsetFromString).isEqualTo(UTF_8);
    assertThat(target.charsetArray).containsExactly(UTF_8, UTF_16, US_ASCII);
    assertThat(target.charsetsAsStringArray).containsExactly(UTF_8.toString(), UTF_16.toString(),
            US_ASCII.toString());
    assertThat(target.charsetArrayFromString).containsExactly(UTF_8, UTF_16, US_ASCII);
    assertThat(target.charsetList).containsExactly(UTF_8, UTF_16, US_ASCII);
    assertThat(target.charsetAsStringList).containsExactly(UTF_8.toString(), UTF_16.toString(),
            US_ASCII.toString());
    assertThat(target.charsetListFromString).containsExactly(UTF_8, UTF_16, US_ASCII);
    assertThat(target.charsetSet).containsExactlyInAnyOrder(UTF_8, UTF_16, US_ASCII);
  }

  private static class CustomClassWithStringConstructor {
    private String value;

    public CustomClassWithStringConstructor(String aValue) {
      value = aValue;
    }

    public String getValue() {
      return value;
    }
  }

  public static class CustomClassWithStringConstructorInjection {
    private @ConfigurationParameter CustomClassWithStringConstructor customFromString;

    private @ConfigurationParameter CustomClassWithStringConstructor[] customArrayFromString;

    private @ConfigurationParameter List<CustomClassWithStringConstructor> customListFromString;
  }

  @Test
  public void thatCustomClassWithStringConstructorObjectCanBeInjected() throws Exception {
    CustomClassWithStringConstructorInjection target = new CustomClassWithStringConstructorInjection();

    initialize(target, "customFromString", "test", "customArrayFromString",
            new String[] { "test1", "test2", "test3" }, "customListFromString",
            asList("test1", "test2", "test3"));

    assertThat(target.customFromString).extracting(CustomClassWithStringConstructor::getValue)
            .isEqualTo("test");
    assertThat(target.customArrayFromString).extracting(CustomClassWithStringConstructor::getValue)
            .containsExactly("test1", "test2", "test3");
    assertThat(target.customListFromString).extracting(CustomClassWithStringConstructor::getValue)
            .containsExactly("test1", "test2", "test3");

  }

  // --- Legacy unit tests below ---

  @Test
  public void testInitialize() throws ResourceInitializationException, SecurityException {

    ResourceInitializationException rie = null;
    try {
      AnalysisEngineFactory.createEngine(ParameterizedAE.class, typeSystemDescription);
    } catch (ResourceInitializationException e) {
      rie = e;
    }
    assertNotNull(rie);
    AnalysisEngine engine = AnalysisEngineFactory.createEngine(ParameterizedAE.class,
            typeSystemDescription, ParameterizedAE.PARAM_FLOAT_3, 1.234f,
            ParameterizedAE.PARAM_FLOAT_6, new Float[] { 1.234f, 0.001f }, "file2", "foo/bar",
            "files9", new File[] { new File("test/data/file"), new File("test/data/file2") },
            ParameterizedAE.PARAM_STRING_9, "singleelementarray", "files10",
            new File("test/data/file"), "booleans8", true);

    ParameterizedAE component = new ParameterizedAE();
    component.initialize(engine.getUimaContext());
    assertEquals("pineapple", component.getString1());
    assertArrayEquals(new String[] { "coconut", "mango" }, component.getString2());
    assertEquals(null, component.getString3());
    assertArrayEquals(new String[] { "apple" }, component.getString4());
    assertArrayEquals(new String[] { "" }, component.getString5());
    assertEquals(3, component.getStrings6().size());
    assertTrue(component.getStrings6().contains("kiwi fruit"));
    assertTrue(component.getStrings6().contains("grape"));
    assertTrue(component.getStrings6().contains("pear"));
    assertNull(component.getStrings7());
    assertEquals(1, component.getStrings8().size());
    assertTrue(component.getStrings8().contains("cherry"));
    assertTrue(component.getStrings9().contains("singleelementarray"));
    assertEquals(0, component.getStrings10().size());

    assertFalse(component.isBoolean1());

    NullPointerException npe = null;
    try {
      assertFalse(component.isBoolean2());
    } catch (NullPointerException e) {
      npe = e;
    }
    assertNotNull(npe);

    assertFalse(component.isBoolean2b());

    assertTrue(component.getBoolean3()[0]);
    assertTrue(component.getBoolean3()[1]);
    assertFalse(component.getBoolean3()[2]);
    assertTrue(component.boolean4[0]);
    assertFalse(component.boolean4[1]);
    assertTrue(component.boolean4[2]);
    assertFalse(component.getBoolean5()[0]);
    assertEquals(4, component.getBooleans6().size());
    assertTrue(component.getBooleans6().get(0));
    assertTrue(component.getBooleans6().get(1));
    assertTrue(component.getBooleans6().get(2));
    assertFalse(component.getBooleans6().get(3));
    assertTrue(component.getBooleans7().get(0));
    assertTrue(component.getBooleans8().get(0));

    assertEquals(0, component.getInt1());
    assertEquals(42, component.getInt2());
    assertEquals(42, component.getInt3()[0]);
    assertEquals(111, component.getInt3()[1]);
    assertEquals(Integer.valueOf(2), component.getInt4()[0]);
    assertEquals(1, component.getInts5().size());
    assertEquals(2, component.getInts5().get(0).intValue());
    assertEquals(5, component.getInts6().size());
    assertEquals(1, component.getInts6().get(0).intValue());
    assertEquals(2, component.getInts6().get(1).intValue());
    assertEquals(3, component.getInts6().get(2).intValue());
    assertEquals(4, component.getInts6().get(3).intValue());
    assertEquals(5, component.getInts6().get(4).intValue());

    assertEquals(0.0f, component.getFloat1(), 0.001f);
    assertEquals(3.1415f, component.getFloat2(), 0.001f);
    assertEquals(1.234f, component.getFloat3(), 0.001f);
    assertNull(component.getFloat4());
    assertEquals(0f, component.getFloat5()[0], 0.001f);
    assertEquals(3.1415f, component.getFloat5()[1], 0.001f);
    assertEquals(2.7182818f, component.getFloat5()[2], 0.001f);
    assertEquals(1.234f, component.getFloat6()[0], 0.001f);
    assertEquals(0.001f, component.getFloat6()[1], 0.001f);
    assertEquals(1.1111f, component.getFloat7()[0], 0.001f);
    assertEquals(2.2222f, component.getFloat7()[1], 0.001f);
    assertEquals(3.3333f, component.getFloat7()[2], 0.001f);

    assertEquals(EnumValue.ENUM_1, component.getEnum1());
    assertArrayEquals(new EnumValue[] { EnumValue.ENUM_1, EnumValue.ENUM_2 }, component.getEnum2());
    assertEquals(asList(EnumValue.ENUM_1, EnumValue.ENUM_2), component.getEnum3());
    assertEquals(new File("test/data/file"), component.getFile1());
    assertEquals(new File("test/data/file"), component.getFile1b());
    assertEquals(new File("foo/bar"), component.getFile2());
    assertNull(component.getFiles3());
    assertArrayEquals(new File[] { new File("test/data/file") }, component.getFiles4());
    assertArrayEquals(new File[] { new File("test/data/file"), new File("test/data/file2") },
            component.getFiles5());
    assertNull(component.getFiles6());
    assertEquals(1, component.getFiles7().size());
    assertEquals(new File("test/data/file"), component.getFiles7().get(0));
    assertEquals(2, component.getFiles8().size());
    assertEquals(new File("test/data/file"), component.getFiles8().get(0));
    assertEquals(new File("test/data/file2"), component.getFiles8().get(1));
    assertEquals(2, component.getFiles9().size());
    assertEquals(new File("test/data/file"), component.getFiles9().get(0));
    assertEquals(new File("test/data/file2"), component.getFiles9().get(1));
    assertEquals(new File("test/data/file"), component.getFiles10().get(0));

    assertNull(component.getRegex1());
    assertTrue(component.getRegex2().matcher("This is uimaFIT calling!").matches());

    engine = AnalysisEngineFactory.createEngine(ParameterizedAE.class, typeSystemDescription,
            ParameterizedAE.PARAM_FLOAT_3, 1.234f, ParameterizedAE.PARAM_FLOAT_6,
            new Float[] { 1.234f, 0.001f }, ParameterizedAE.PARAM_STRING_1, "lime",
            ParameterizedAE.PARAM_STRING_2, new String[] { "banana", "strawberry" },
            ParameterizedAE.PARAM_STRING_3, "cherry", ParameterizedAE.PARAM_STRING_4,
            new String[] { "raspberry", "blueberry", "blackberry" }, ParameterizedAE.PARAM_STRING_5,
            new String[] { "a" }, ParameterizedAE.PARAM_BOOLEAN_1, true,
            ParameterizedAE.PARAM_BOOLEAN_2, true, ParameterizedAE.PARAM_BOOLEAN_3,
            new boolean[] { true, true, false }, ParameterizedAE.PARAM_BOOLEAN_4,
            new Boolean[] { true, false, false }, ParameterizedAE.PARAM_BOOLEAN_5,
            new Boolean[] { true }, ParameterizedAE.PARAM_INT_1, 0, ParameterizedAE.PARAM_INT_2, 24,
            ParameterizedAE.PARAM_INT_3, new int[] { 5 }, "file1", "foo1/bar1", "file1b",
            "foo1b/bar1b", "file2", "foo2/bar2", "files3",
            new String[] { "C:\\Documents and Settings\\Philip\\My Documents\\", "/usr/local/bin" },
            "files4", new String[0], "files5", new String[] { "foos/bars" }, "files6",
            new String[] { "C:\\Documents and Settings\\Philip\\My Documents\\", "/usr/local/bin" },
            "files7", new String[0], "files8", new String[] { "foos/bars" }, "files9",
            Arrays.asList(new File("test/data/file"), new File("test/data/file2")));
    component = new ParameterizedAE();
    component.initialize(engine.getUimaContext());
    assertEquals("lime", component.getString1());
    assertArrayEquals(new String[] { "banana", "strawberry" }, component.getString2());
    assertEquals("cherry", component.getString3());
    assertArrayEquals(new String[] { "raspberry", "blueberry", "blackberry" },
            component.getString4());
    assertArrayEquals(new String[] { "a" }, component.getString5());
    assertTrue(component.isBoolean1());
    assertTrue(component.isBoolean2());
    assertTrue(component.getBoolean3()[0]);
    assertTrue(component.getBoolean3()[1]);
    assertFalse(component.getBoolean3()[2]);
    assertTrue(component.boolean4[0]);
    assertFalse(component.boolean4[1]);
    assertFalse(component.boolean4[2]);
    assertTrue(component.getBoolean5()[0]);
    assertEquals(0, component.getInt1());
    assertEquals(24, component.getInt2());
    assertEquals(5, component.getInt3()[0]);

    assertEquals(new File("foo1/bar1"), component.getFile1());
    assertEquals(new File("foo1b/bar1b"), component.getFile1b());
    assertEquals(new File("foo2/bar2"), component.getFile2());
    assertArrayEquals(new File[] { new File("C:\\Documents and Settings\\Philip\\My Documents\\"),
        new File("/usr/local/bin") }, component.getFiles3());
    assertEquals(0, component.getFiles4().length);
    assertArrayEquals(new File[] { new File("foos/bars") }, component.getFiles5());
    assertEquals(2, component.getFiles6().size());
    assertEquals(new File("C:\\Documents and Settings\\Philip\\My Documents\\"),
            component.getFiles6().get(0));
    assertEquals(new File("/usr/local/bin"), component.getFiles6().get(1));
    assertEquals(0, component.getFiles7().size());
    assertEquals(1, component.getFiles8().size());
    assertEquals(new File("foos/bars"), component.getFiles8().get(0));
    assertEquals(2, component.getFiles9().size());
    assertEquals(new File("test/data/file"), component.getFiles9().get(0));
    assertEquals(new File("test/data/file2"), component.getFiles9().get(1));

    engine = AnalysisEngineFactory.createEngine(ParameterizedAE.class, typeSystemDescription,
            ParameterizedAE.PARAM_FLOAT_3, 1.234f, ParameterizedAE.PARAM_FLOAT_6,
            new Float[] { 1.234f, 0.001f }, ParameterizedAE.PARAM_BOOLEAN_1, true,
            ParameterizedAE.PARAM_BOOLEAN_3, new boolean[3], ParameterizedAE.PARAM_FLOAT_5,
            new float[] { 1.2f, 3.4f }, "file2", "foo2/bar2");
    component = new ParameterizedAE();
    component.initialize(engine.getUimaContext());
    assertFalse(component.getBoolean3()[0]);
    assertFalse(component.getBoolean3()[1]);
    assertFalse(component.getBoolean3()[2]);
    assertEquals(component.getFloat5()[0], 1.2f, 0.001f);
    assertEquals(component.getFloat5()[1], 3.4f, 0.001f);

    rie = null;
    try {
      engine = AnalysisEngineFactory.createEngine(ParameterizedAE.class, typeSystemDescription,
              ParameterizedAE.PARAM_FLOAT_3, 1.234f, ParameterizedAE.PARAM_FLOAT_6,
              new Float[] { 1.234f, 0.001f }, ParameterizedAE.PARAM_STRING_1, true);
    } catch (ResourceInitializationException e) {
      rie = e;
    }
    assertNotNull(rie);

  }

  @Test
  public void testInitialize2() throws ResourceInitializationException {
    AnalysisEngine engine = AnalysisEngineFactory.createEngine(Annotator1.class,
            typeSystemDescription);
    assertEquals(1, engine.getAnalysisEngineMetaData().getCapabilities().length);
  }

  @Test
  public void testInitialize3() throws FileNotFoundException, IOException, UIMAException {
    // here we test an optional parameter that is missing from the
    // configuration to ensure that it is filled in with the default value
    AnalysisEngine aed = AnalysisEngineFactory
            .createEngineFromPath("src/test/resources/data/descriptor/DefaultValueAE1.xml");
    DefaultValueAE1 ae = new DefaultValueAE1();
    ae.initialize(aed.getUimaContext());
    assertEquals("green", ae.color);

    // here we test a mandatory parameter that is missing from the
    // configuration and ensure that an exception is thrown because
    // no default value is given in the configuration parameter annotation.
    ResourceInitializationException rie = null;
    try {
      aed = AnalysisEngineFactory
              .createEngineFromPath("src/test/resources/data/descriptor/DefaultValueAE2.xml");
    } catch (ResourceInitializationException e) {
      rie = e;
    }
    assertNotNull(rie);
  }

  /**
   * If a parameter value is set to null, that is as good as if it was not set at all. If a default
   * value is specified, it should be used.
   */
  @Test
  public void testParameterSetToNull() throws Exception {
    AnalysisEngine aed = AnalysisEngineFactory.createEngine(DefaultValueAE1.class,
            DefaultValueAE1.PARAM_COLOR, null);
    DefaultValueAE1 ae = new DefaultValueAE1();
    ae.initialize(aed.getUimaContext());
    assertEquals("green", ae.color);
  }

  /**
   * If a parameter value is set to null, that is as good as if it was not set at all. If it is
   * mandatory, an exception has to be thrown.
   */
  @Test
  public void testMandatoryParameterSetToNull() throws Exception {
    assertThatExceptionOfType(ResourceInitializationException.class) //
            .isThrownBy(() -> createEngine( //
                    DefaultValueAE2.class, //
                    DefaultValueAE2.PARAM_COLOR, null));
  }

  /**
   * If a parameter value is set to enum value.
   */
  @Test
  public void testParameterSetEnumExplicitly() throws Exception {
    AnalysisEngine aed = AnalysisEngineFactory.createEngine(DefaultValueAE3.class,
            DefaultValueAE3.PARAM_COLOR, Color.RED);
    DefaultValueAE3 ae = new DefaultValueAE3();
    ae.initialize(aed.getUimaContext());
    assertEquals(Color.RED, ae.color);
  }

  /**
   * If a parameter value is set to enum value via default.
   */
  @Test
  public void testParameterSetEnumDefault() throws Exception {
    AnalysisEngine aed = AnalysisEngineFactory.createEngine(DefaultValueAE3.class);
    DefaultValueAE3 ae = new DefaultValueAE3();
    ae.initialize(aed.getUimaContext());
    assertEquals(Color.GREEN, ae.color);
  }

  /**
   * Test that a parameter not supported by UIMA produces an error.
   */
  @Test
  public void testNonUimaCompatibleParameterValue() throws Exception {
    assertThatExceptionOfType(IllegalArgumentException.class) //
            .isThrownBy(() -> createEngine( //
                    DefaultValueAE2.class, null, //
                    DefaultValueAE2.PARAM_COLOR, new Point(1, 2)));
  }

  /**
   * Check that an Analysis Engine created from a descriptor declaring optional parameters but not
   * setting them actually uses the default values declared in the Java annotation
   */
  @Test
  public void testUnsetOptionalParameter() throws Exception {
    AnalysisEngineDescription aed = AnalysisEngineFactory
            .createEngineDescription(DefaultValueAE1.class, (Object[]) null);
    // Remove the settings from the descriptor, but leave the declarations.
    // The settings are already filled with default values by createPrimitiveDescription,
    // but here we want to simulate loading a descriptor without settings from a file.
    // The file of course would declare the parameters optional and thus the settings
    // for the optional parameters would be empty. We expect that a default value from the
    // annotation is used in this case.
    aed.getMetaData().setConfigurationParameterSettings(new ConfigurationParameterSettings_impl());
    AnalysisEngine template = UIMAFramework.produceAnalysisEngine(aed);
    DefaultValueAE1 ae = new DefaultValueAE1();
    ae.initialize(template.getUimaContext());
    assertEquals("green", ae.color);
  }

  public static class DefaultValueAE1 extends JCasAnnotator_ImplBase {
    public static final String PARAM_COLOR = "color";

    @ConfigurationParameter(defaultValue = "green", mandatory = false)
    private String color;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
      super.initialize(aContext);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      /* do nothing */
    }
  }

  public static class DefaultValueAE2 extends JCasAnnotator_ImplBase {
    public static final String PARAM_COLOR = "color";

    @ConfigurationParameter(mandatory = true)
    private String color;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
      super.initialize(aContext);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      /* do nothing */
    }
  }

  public static class DefaultValueAE3 extends JCasAnnotator_ImplBase {
    public static final String PARAM_COLOR = "color";

    @ConfigurationParameter(defaultValue = "GREEN", mandatory = false)
    private Color color;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
      super.initialize(aContext);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      /* do nothing */
    }
  }

  @Test
  public void testEnumDefaultValue() {
    try {
      AnalysisEngine aed = AnalysisEngineFactory.createEngine(DefaultEnumValueAE.class,
              (Object[]) null);
      DefaultEnumValueAE ae = new DefaultEnumValueAE();
      ae.initialize(aed.getUimaContext());
      assertEquals(Color.GREEN, ae.color);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public enum Color {
    RED, GREEN, BLUE
  }

  public static class DefaultEnumValueAE extends JCasAnnotator_ImplBase {
    @ConfigurationParameter(defaultValue = "GREEN")
    private Color color;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
      super.initialize(aContext);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      /* do nothing */
    }
  }

  public static class DefaultLocaleValueAE extends JCasAnnotator_ImplBase {
    @ConfigurationParameter(name = "L1", defaultValue = "US")
    public Locale locale1;

    @ConfigurationParameter(name = "L2")
    public Locale locale2;

    @ConfigurationParameter(name = "L3", mandatory = false)
    public Locale locale3;

    @ConfigurationParameter(name = "L4", mandatory = false)
    public Locale locale4;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      /* do nothing */
    }
  }

  @Test
  public void testLocaleParams() throws Exception {
    AnalysisEngine aed = AnalysisEngineFactory.createEngine(DefaultLocaleValueAE.class, "L2",
            "en-CA", "L3", "CANADA_FRENCH", "L4", "zh");
    DefaultLocaleValueAE ae = new DefaultLocaleValueAE();
    ae.initialize(aed.getUimaContext());
    assertEquals(Locale.US, ae.locale1);
    assertEquals(new Locale("en", "CA"), ae.locale2);
    assertEquals(Locale.CANADA_FRENCH, ae.locale3);
    assertEquals(new Locale("zh"), ae.locale4);

    aed = AnalysisEngineFactory.createEngine(DefaultLocaleValueAE.class, "L1",
            "es-ES-Traditional_WIN", "L2", "CHINA", "L3", "es", "L4", "en-CA");
    ae = new DefaultLocaleValueAE();
    ae.initialize(aed.getUimaContext());
    assertEquals(new Locale("es", "ES", "Traditional_WIN"), ae.locale1);
    assertEquals(Locale.CHINA, ae.locale2);
    assertEquals(new Locale("es"), ae.locale3);
    assertEquals(new Locale("en", "CA"), ae.locale4);

    aed = AnalysisEngineFactory.createEngine(DefaultLocaleValueAE.class, "L1", "", "L2", "", "L3",
            null);
    ae = new DefaultLocaleValueAE();
    ae.initialize(aed.getUimaContext());
    assertEquals(Locale.getDefault(), ae.locale1);
    assertEquals(Locale.getDefault(), ae.locale2);
    assertEquals(null, ae.locale3);
    assertEquals(null, ae.locale4);

  }

  /**
   * This main method creates the descriptor files used in testInitialize3. If I weren't lazy I
   * would figure out how to programmatically remove the configuration parameter corresponding to
   * 'color'. As it is, however, the parameter must be manually removed (I used the Component
   * Descriptor Editor to do this.) This point is moot anyways because I am checking in the
   * generated descriptor files and there is no reason to run this main method in the future.
   */
  public static void main(String[] args)
          throws ResourceInitializationException, FileNotFoundException, SAXException, IOException {
    AnalysisEngineDescription aed = AnalysisEngineFactory
            .createEngineDescription(DefaultValueAE1.class, (Object[]) null);
    aed.toXML(new FileOutputStream("src/test/resources/data/descriptor/DefaultValueAE1.xml"));
    aed = AnalysisEngineFactory.createEngineDescription(DefaultValueAE2.class, (Object[]) null);
    aed.toXML(new FileOutputStream("src/test/resources/data/descriptor/DefaultValueAE2.xml"));
  }

}
