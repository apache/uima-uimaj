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
package org.apache.uima.cas.test;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.apache.uima.UIMAFramework.getXMLParser;
import static org.apache.uima.UIMAFramework.newDefaultResourceManager;
import static org.apache.uima.UIMAFramework.produceAnalysisEngine;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_FS_ARRAY;
import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_component.Annotator_ImplBase;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.impl.PrimitiveAnalysisEngine_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.spi.JCasClassProviderForTesting;
import org.apache.uima.test.IsolatingClassloader;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import x.y.z.Token;
import x.y.z.TokenType;

public class JCasClassLoaderTest {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static final String TYPE_NAME_TOKEN = Token.class.getName();
  public static final String TYPE_NAME_ARRAY_HOST = "uima.testing.ArrayHost";
  public static final String FEAT_NAME_ARRAY_HOST_VALUES = "values";

  public static Class<?> casTokenClassViaClassloader;
  public static Class<?> casTokenClassViaCas;
  public static Class<?> addTokenAETokenClass;
  public static Class<?> fetchTokenAETokenClass;
  public static Class<?> indexedTokenClass;
  public static boolean fetchThrowsClassCastException;

  public static Class<?> tokenClassAddedToArray;
  public static Class<?> tokenClassFetchedFromArray;

  @BeforeEach
  public void setup() {
    casTokenClassViaClassloader = null;
    casTokenClassViaCas = null;
    addTokenAETokenClass = null;
    fetchTokenAETokenClass = null;
    indexedTokenClass = null;
    fetchThrowsClassCastException = false;

    tokenClassAddedToArray = null;
    tokenClassFetchedFromArray = null;
  }

  /**
   * This test simulates an environment as it could exist when using e.g. PEARs. We use a vanilla
   * JCas and the analysis engines each use local JCas wrappers which are provided as an extra
   * classpath passed to the resource manager.
   * 
   * <ul>
   * <li>JCas does not know JCas wrappers for the {@code Token} type.</li>
   * <li>AddATokenAnnotator uses a local version of the {@code Token} type.</li>
   * <li>FetchTheTokenAnnotator uses a local version of the {@code Token} type.</li>
   * </ul>
   * 
   * The expectation here is that at the moment when the JCas is passed to the analysis engines,
   * {@link PrimitiveAnalysisEngine_impl#callAnalysisComponentProcess(CAS) it is reconfigured} using
   * {@link CASImpl#switchClassLoaderLockCasCL(ClassLoader)} to use the classloader defined in the
   * {@link ResourceManager} of the engines to load the JCas wrapper classes. So each of the anlysis
   * engines should use its own version of the JCas wrappers to access the CAS.
   */
  @Test
  void thatCASCanBeDefinedWithoutJCasWrappersAndTheyComeInWithAnnotatorsViaClasspath()
          throws Exception {
    var rootCl = getClass().getClassLoader();

    // We do not want the CAS to know the Token JCas wrapper when it gets initialized
    var clForCas = new IsolatingClassloader("CAS", rootCl)
            .hiding("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    var cpBase = new File("target/test-output/JCasClassLoaderTest/classes");
    var cpPackageBase = new File(cpBase, "org/apache/uima/cas/test");
    cpPackageBase.mkdirs();
    copyFile(new File("target/test-classes/org/apache/uima/cas/test/Token.class"),
            new File(cpPackageBase, "Token.class"));
    copyFile(new File(
            "target/test-classes/org/apache/uima/cas/test/JCasClassLoaderTest$AddATokenAnnotator.class"),
            new File(cpPackageBase, "JCasClassLoaderTest$AddATokenAnnotator.class"));
    copyFile(new File(
            "target/test-classes/org/apache/uima/cas/test/JCasClassLoaderTest$FetchTheTokenAnnotator.class"),
            new File(cpPackageBase, "JCasClassLoaderTest$FetchTheTokenAnnotator.class"));

    var jcas = makeJCas(clForCas);
    var addATokenAnnotator = makeAnalysisEngine(AddATokenAnnotator.class, cpBase);
    var fetchTheTokenAnnotator = makeAnalysisEngine(FetchTheTokenAnnotator.class, cpBase);

    jcas.setDocumentText("test");

    addATokenAnnotator.process(jcas);
    fetchTheTokenAnnotator.process(jcas);

    try (var softly = new AutoCloseableSoftAssertions()) {
      softly.assertThat(casTokenClassViaClassloader).isNull();
      softly.assertThat(casTokenClassViaCas).isSameAs(Annotation.class);
      softly.assertThat(addTokenAETokenClass).isNotNull();
      softly.assertThat(fetchTokenAETokenClass).isNotNull();
      softly.assertThat(addTokenAETokenClass)
              .as("AddTokenAnnotator and FetchTokenAnnotator use different Token wrappers")
              .isNotEqualTo(fetchTokenAETokenClass);
      softly.assertThat(indexedTokenClass).as(
              "JCas in FetchTokenAnnotator provides Token JCas wrapper from FetchTokenAnnotator CL")
              .isEqualTo(fetchTokenAETokenClass);
      softly.assertThat(fetchThrowsClassCastException)
              .as("Classcast exception thrown when trying to retrieve Token from index").isFalse();
    }
  }

  /**
   * This test simulates an environment as it could exist when using e.g. OSGI. We use a vanilla
   * JCas and the analysis engines each use local JCas wrappers which are provided via a classloader
   * that is provided to the resource manager.
   * 
   * <ul>
   * <li>JCas does not know JCas wrappers for the {@code Token} type.</li>
   * <li>AddATokenAnnotator uses a local version of the {@code Token} type.</li>
   * <li>FetchTheTokenAnnotator uses a local version of the {@code Token} type.</li>
   * </ul>
   * 
   * The expectation here is that at the moment when the JCas is passed to the analysis engines,
   * {@link PrimitiveAnalysisEngine_impl#callAnalysisComponentProcess(CAS) it is reconfigured} using
   * {@link CASImpl#switchClassLoaderLockCasCL(ClassLoader)} to use the classloader defined in the
   * {@link ResourceManager} of the engines to load the JCas wrapper classes. So each of the anlysis
   * engines should use its own version of the JCas wrappers to access the CAS.
   */
  @Test
  void thatCASCanBeDefinedWithoutJCasWrappersAndTheyComeInWithAnnotatorsViaClassloader()
          throws Exception {
    var rootCl = getClass().getClassLoader();

    // We do not want the CAS to know the Token JCas wrapper when it gets initialized
    var clForCas = new IsolatingClassloader("CAS", rootCl)
            .hiding("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    var clForAddATokenAnnotator = new IsolatingClassloader("AddATokenAnnotator", rootCl)
            .redefining("^.*AddATokenAnnotator$")
            .redefining("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    var clForFetchTheTokenAnnotator = new IsolatingClassloader("FetchTheTokenAnnotator", rootCl)
            .redefining("^.*FetchTheTokenAnnotator$")
            .redefining("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    var jcas = makeJCas(clForCas);
    var addATokenAnnotator = makeAnalysisEngine(AddATokenAnnotator.class, clForAddATokenAnnotator);
    var fetchTheTokenAnnotator = makeAnalysisEngine(FetchTheTokenAnnotator.class,
            clForFetchTheTokenAnnotator);

    jcas.setDocumentText("test");

    addATokenAnnotator.process(jcas);
    fetchTheTokenAnnotator.process(jcas);

    try (var softly = new AutoCloseableSoftAssertions()) {
      softly.assertThat(casTokenClassViaClassloader).isNull();
      softly.assertThat(casTokenClassViaCas).isSameAs(Annotation.class);
      softly.assertThat(addTokenAETokenClass).isNotNull();
      softly.assertThat(fetchTokenAETokenClass).isNotNull();
      softly.assertThat(addTokenAETokenClass)
              .as("AddTokenAnnotator and FetchTokenAnnotator use different Token wrappers")
              .isNotEqualTo(fetchTokenAETokenClass);
      softly.assertThat(indexedTokenClass).as(
              "JCas in FetchTokenAnnotator provides Token JCas wrapper from FetchTokenAnnotator CL")
              .isEqualTo(fetchTokenAETokenClass);
      softly.assertThat(fetchThrowsClassCastException)
              .as("Classcast exception thrown when trying to retrieve Token from index").isFalse();
    }
  }

  /**
   * This test simulates an environment as it could exist when using e.g. PEARs. The CAS has access
   * to JCas wrappers, but the analysis engines bring in their own.
   * 
   * <ul>
   * <li>JCas knows the global JCas wrappers for the {@code Token} type.</li>
   * <li>AddATokenAnnotator uses a local version of the {@code Token} type.</li>
   * <li>FetchTheTokenAnnotator uses a local version of the {@code Token} type.</li>
   * </ul>
   * 
   * The expectation here is that at the moment when the JCas is passed to the analysis engines,
   * {@link PrimitiveAnalysisEngine_impl#callAnalysisComponentProcess(CAS) it is reconfigured} using
   * {@link CASImpl#switchClassLoaderLockCasCL(ClassLoader)} to use the classloader defined in the
   * {@link ResourceManager} of the engines to load the JCas wrapper classes. So each of the
   * analysis engines should use its own version of the JCas wrappers to access the CAS. In
   * particular, they should not use the global JCas wrappers which were known to the JCas when it
   * was first initialized.
   */
  @Test
  void thatAnnotatorsCanLocallyUseDifferentJCasWrappers() throws Exception {
    var rootCl = getClass().getClassLoader();

    var clForCas = new IsolatingClassloader("CAS", rootCl);

    var clForAddATokenAnnotator = new IsolatingClassloader("AddATokenAnnotator", rootCl)
            .redefining("^.*AddATokenAnnotator$")
            .redefining("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    var clForFetchTheTokenAnnotator = new IsolatingClassloader("FetchTheTokenAnnotator", rootCl)
            .redefining("^.*FetchTheTokenAnnotator$")
            .redefining("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    var jcas = makeJCas(clForCas);
    var addATokenAnnotator = makeAnalysisEngine(AddATokenAnnotator.class, clForAddATokenAnnotator);
    var fetchTheTokenAnnotator = makeAnalysisEngine(FetchTheTokenAnnotator.class,
            clForFetchTheTokenAnnotator);

    jcas.setDocumentText("test");

    addATokenAnnotator.process(jcas);
    fetchTheTokenAnnotator.process(jcas);

    try (var softly = new AutoCloseableSoftAssertions()) {
      softly.assertThat(casTokenClassViaClassloader).isNotNull();
      softly.assertThat(casTokenClassViaCas)
              .as("System-level Token wrapper loader and Token wrapper in the CAS are the same")
              .isSameAs(Token.class);
      softly.assertThat(addTokenAETokenClass).isNotNull();
      softly.assertThat(fetchTokenAETokenClass).isNotNull();
      softly.assertThat(casTokenClassViaClassloader)
              .as("JCas and AddTokenAnnotator use different Token wrappers")
              .isNotEqualTo(addTokenAETokenClass);
      softly.assertThat(casTokenClassViaClassloader)
              .as("JCas and FetchTokenAnnotator use different Token wrappers")
              .isNotEqualTo(fetchTokenAETokenClass);
      softly.assertThat(addTokenAETokenClass)
              .as("AddTokenAnnotator and FetchTokenAnnotator use different Token wrappers")
              .isNotEqualTo(fetchTokenAETokenClass);
      softly.assertThat(indexedTokenClass).as(
              "JCas in FetchTokenAnnotator provides Token JCas wrapper from FetchTokenAnnotator CL")
              .isEqualTo(fetchTokenAETokenClass);
      softly.assertThat(fetchThrowsClassCastException)
              .as("Classcast exception thrown when trying to retrieve Token from index").isFalse();
    }
  }

  /**
   * Here we try to simulate a situation as it could happen e.g. in an OSGI environment where the
   * type system, the JCas and the analysis engines all use different classloaders.
   * 
   * <ul>
   * <li>JCas does not know JCas wrappers for the {@code Token} type.</li>
   * <li>The JCas wrappers are provided through a dedicated class loader</li>
   * <li>AddATokenAnnotator uses the type classloader to acces the JCas wrapper for
   * {@code Token}</li>
   * <li>FetchTheTokenAnnotator uses the type classloader to acces the JCas wrapper for
   * {@code Token}</li>
   * </ul>
   * 
   * The expectation here is that at the moment when the JCas is passed to the analysis engines,
   * {@link PrimitiveAnalysisEngine_impl#callAnalysisComponentProcess(CAS) it is reconfigured} using
   * {@link CASImpl#switchClassLoaderLockCasCL(ClassLoader)} to use the classloader defined in the
   * {@link ResourceManager} of the engines to load the JCas wrapper classes. Since the JCas
   * wrappers are loaded through the same classloader by both engines, it they should have the same
   * type in both annotators.
   */
  @Test
  void thatTypeSystemCanComeFromItsOwnClassLoader() throws Exception {
    var rootCl = getClass().getClassLoader();

    var clForCas = new IsolatingClassloader("CAS", rootCl)
            .hiding("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    var clForTS = new IsolatingClassloader("TS", rootCl)
            .redefining("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    var clForAddATokenAnnotator = new IsolatingClassloader("AddATokenAnnotator", rootCl)
            .redefining(AddATokenAnnotator.class)
            .delegating("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*", clForTS);

    var clForFetchTheTokenAnnotator = new IsolatingClassloader("FetchTheTokenAnnotator", rootCl)
            .redefining(FetchTheTokenAnnotator.class)
            .delegating("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*", clForTS);

    var jcas = makeJCas(clForCas);
    var addATokenAnnotator = makeAnalysisEngine(AddATokenAnnotator.class, clForAddATokenAnnotator);
    var fetchTheTokenAnnotator = makeAnalysisEngine(FetchTheTokenAnnotator.class,
            clForFetchTheTokenAnnotator);

    jcas.setDocumentText("test");

    addATokenAnnotator.process(jcas);
    fetchTheTokenAnnotator.process(jcas);

    try (var softly = new AutoCloseableSoftAssertions()) {
      softly.assertThat(casTokenClassViaClassloader).isNull();
      softly.assertThat(casTokenClassViaCas).isSameAs(Annotation.class);
      softly.assertThat(addTokenAETokenClass).isNotNull();
      softly.assertThat(fetchTokenAETokenClass).isNotNull();
      softly.assertThat(casTokenClassViaClassloader)
              .as("JCas and AddTokenAnnotator use different Token wrappers")
              .isNotEqualTo(addTokenAETokenClass);
      softly.assertThat(casTokenClassViaClassloader)
              .as("JCas and FetchTokenAnnotator use different Token wrappers")
              .isNotEqualTo(fetchTokenAETokenClass);
      softly.assertThat(addTokenAETokenClass)
              .as("AddTokenAnnotator and FetchTokenAnnotator use different Token wrappers")
              .isEqualTo(fetchTokenAETokenClass);
      softly.assertThat(indexedTokenClass).as(
              "JCas in FetchTokenAnnotator provides Token JCas wrapper from FetchTokenAnnotator CL")
              .isEqualTo(fetchTokenAETokenClass);
      softly.assertThat(fetchThrowsClassCastException)
              .as("Classcast exception thrown when trying to retrieve Token from index").isFalse();
    }
  }

  @Test
  void thatFSArraySpliteratorReturnsProperJCasWrapper() throws Exception {
    var rootCl = getClass().getClassLoader();

    var clForCas = new IsolatingClassloader("CAS", rootCl)
            .hiding("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    var clForCreators = new IsolatingClassloader("Creators", rootCl)
            .hiding("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*")
            .redefining(AddATokenAnnotatorNoJCas.class)
            .redefining(AddTokenToArrayAnnotatorNoJCas.class);

    var clForAccessors = new IsolatingClassloader("Accessors", rootCl)
            .redefining("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*")
            .redefining(FetchTokenFromArrayViaSpliteratorAnnotator.class);

    var tsd = mergeTypeSystems(asList(loadTokensAndSentencesTS(), makeArrayTestTS()));

    var jcas = makeJCas(clForCas, tsd);
    var addATokenAnnotator = makeAnalysisEngine(AddATokenAnnotatorNoJCas.class, clForCreators);
    var addTokenToArrayAnnotator = makeAnalysisEngine(AddTokenToArrayAnnotatorNoJCas.class,
            clForCreators);
    var fetchTokenFromArrayViaSpliteratorAnnotator = makeAnalysisEngine(
            FetchTokenFromArrayViaSpliteratorAnnotator.class, clForAccessors);

    jcas.setDocumentText("test");

    addATokenAnnotator.process(jcas);
    addTokenToArrayAnnotator.process(jcas);
    fetchTokenFromArrayViaSpliteratorAnnotator.process(jcas);

    try (var softly = new AutoCloseableSoftAssertions()) {
      softly.assertThat(casTokenClassViaClassloader).isNull();
      softly.assertThat(casTokenClassViaCas).isSameAs(Annotation.class);
      softly.assertThat(addTokenAETokenClass).isNotNull();
      softly.assertThat(casTokenClassViaClassloader)
              .as("JCas and AddTokenAnnotator use different Token wrappers")
              .isNotEqualTo(addTokenAETokenClass);
      softly.assertThat(tokenClassFetchedFromArray.getName())
              .as("FSArray spliterator returns proper Token wrapper").isEqualTo(TYPE_NAME_TOKEN);
    }
  }

  @Test
  void thatFSArrayToArrayReturnsProperJCasWrapper() throws Exception {
    var rootCl = getClass().getClassLoader();

    var clForCas = new IsolatingClassloader("CAS", rootCl)
            .hiding("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    var clForCreators = new IsolatingClassloader("Creators", rootCl)
            .hiding("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*")
            .redefining(AddATokenAnnotatorNoJCas.class)
            .redefining(AddTokenToArrayAnnotatorNoJCas.class);

    var clForAccessors = new IsolatingClassloader("Accessors", rootCl)
            .redefining("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*")
            .redefining(FetchTokenFromArrayViaToArrayAnnotator.class);

    var tsd = mergeTypeSystems(asList(loadTokensAndSentencesTS(), makeArrayTestTS()));

    var jcas = makeJCas(clForCas, tsd);
    var addATokenAnnotator = makeAnalysisEngine(AddATokenAnnotatorNoJCas.class, clForCreators);
    var addTokenToArrayAnnotator = makeAnalysisEngine(AddTokenToArrayAnnotatorNoJCas.class,
            clForCreators);
    var fetchTokenFromArrayViaSpliteratorAnnotator = makeAnalysisEngine(
            FetchTokenFromArrayViaToArrayAnnotator.class, clForAccessors);

    jcas.setDocumentText("test");

    addATokenAnnotator.process(jcas);
    addTokenToArrayAnnotator.process(jcas);
    fetchTokenFromArrayViaSpliteratorAnnotator.process(jcas);

    try (var softly = new AutoCloseableSoftAssertions()) {
      softly.assertThat(casTokenClassViaClassloader).isNull();
      softly.assertThat(casTokenClassViaCas).isSameAs(Annotation.class);
      softly.assertThat(addTokenAETokenClass).isNotNull();
      softly.assertThat(casTokenClassViaClassloader)
              .as("JCas and AddTokenAnnotator use different Token wrappers")
              .isNotEqualTo(addTokenAETokenClass);
      softly.assertThat(tokenClassFetchedFromArray.getName())
              .as("FSArray toArray returns proper Token wrapper").isEqualTo(TYPE_NAME_TOKEN);
    }
  }

  @Test
  void thatFeatureRangeClassRedefinedInPearDoesNotCauseProblems(@TempDir File aTemp)
          throws Exception {
    var tsd = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            new XMLInputSource("src/test/java/org/apache/uima/jcas/test/generatedx.xml"));

    LOG.info("-- Base runtime context --------------------------------------------------");
    LOG.info("{} loaded using {}", Token.class, Token.class.getClassLoader());
    LOG.info("{} loaded using {}", TokenType.class, TokenType.class.getClassLoader());

    LOG.info(
            "-- JCas/PEAR-like classloader context ----------------------------------------------");
    var rootCl = getClass().getClassLoader();
    var clForCas = new IsolatingClassloader("CAS Classloader", rootCl) //
            .redefining(TokenType.class) //
            .redefining(JCasClassProviderForTesting.class) //
            .redefining(JCasCreator.class);

    var jcasCreatorClass = clForCas.loadClass(JCasCreatorImpl.class.getName());
    var creator = (JCasCreator) jcasCreatorClass.getDeclaredConstructor().newInstance();
    var jcas = creator.createJCas(clForCas, tsd);
    var cas = jcas.getCas();

    var t = cas.createFS(cas.getTypeSystem().getType(Token.class.getName()));
    var tt = cas.createFS(cas.getTypeSystem().getType(TokenType.class.getName()));

    LOG.info("{} loaded using {}", t.getClass(), t.getClass().getClassLoader());
    LOG.info("{} loaded using {}", tt.getClass(), tt.getClass().getClassLoader());

    assertThat(t.getClass().getClassLoader()) //
            .isSameAs(Token.class.getClassLoader());

    assertThat(tt.getClass().getClassLoader()) //
            .isSameAs(clForCas);

    t.setFeatureValue(t.getType().getFeatureByBaseName(Token._FeatName_ttype), tt);
  }

  public static Class<?> loadTokenClass(ClassLoader cl) {
    try {
      return cl.loadClass(TYPE_NAME_TOKEN);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  public static void printTokenClassLoaderInfo(String context, ClassLoader cl) {
    Class<?> clazz = loadTokenClass(cl);
    if (clazz != null) {
      System.out.printf("[%s] %s %d loaded by %s%n", context, clazz.getName(), clazz.hashCode(),
              clazz.getClassLoader());
    } else {
      System.out.printf("[%s] %s NOT AVAILABLE %n", context, Token.class.getName());
    }
  }

  private TypeSystemDescription makeArrayTestTS() throws InvalidXMLException, IOException {
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    TypeDescription arrayHost = tsd.addType(TYPE_NAME_ARRAY_HOST, "", TYPE_NAME_ANNOTATION);
    arrayHost.addFeature(FEAT_NAME_ARRAY_HOST_VALUES, "", TYPE_NAME_FS_ARRAY, TYPE_NAME_ANNOTATION,
            true);
    return tsd;
  }

  private TypeSystemDescription loadTokensAndSentencesTS() throws InvalidXMLException, IOException {
    return getXMLParser().parseTypeSystemDescription(new XMLInputSource(
            new File("src/test/resources/CASTests/desc/TokensAndSentencesTS.xml")));
  }

  /**
   * Creates a new JCas and sets it up so it uses the given classloader to load its JCas wrappers.
   */
  private JCas makeJCas(IsolatingClassloader cl) throws Exception {
    return makeJCas(cl, loadTokensAndSentencesTS());
  }

  private JCas makeJCas(IsolatingClassloader cl, TypeSystemDescription tsd) throws Exception {
    cl.redefining(JCasCreatorImpl.class);
    var jcasCreatorClass = cl.loadClass(JCasCreatorImpl.class.getName());
    var declaredConstructor = jcasCreatorClass.getDeclaredConstructor();
    declaredConstructor.setAccessible(true);
    var creator = (JCasCreator) declaredConstructor.newInstance();
    return creator.createJCas(cl, tsd);
  }

  /**
   * Creates a new analysis engine from the given class using the given classloader and sets it up
   * so it using the given classloader to load its JCas wrappers.
   */
  private AnalysisEngine makeAnalysisEngine(Class<? extends Annotator_ImplBase> aeClass,
          ClassLoader cl) throws ResourceInitializationException, MalformedURLException {
    var resMgr = newDefaultResourceManager();
    resMgr.setExtensionClassLoader(cl, false);

    printTokenClassLoaderInfo("AE creation: " + aeClass.getSimpleName(),
            resMgr.getExtensionClassLoader());

    var desc = getResourceSpecifierFactory().createAnalysisEngineDescription();
    desc.setAnnotatorImplementationName(aeClass.getName());
    desc.setPrimitive(true);
    return produceAnalysisEngine(desc, resMgr, null);
  }

  /**
   * Creates a new analysis engine from the given class using the given classpath and sets it up so
   * it using the given classpath to load its JCas wrappers.
   */
  private AnalysisEngine makeAnalysisEngine(Class<? extends Annotator_ImplBase> aeClass,
          File... classPath) throws ResourceInitializationException, MalformedURLException {
    var cp = Stream.of(classPath).map(Object::toString).collect(joining(File.pathSeparator));
    var resMgr = newDefaultResourceManager();
    resMgr.setExtensionClassPath(cp, false);

    printTokenClassLoaderInfo("AE creation: " + aeClass.getSimpleName(),
            resMgr.getExtensionClassLoader());

    var desc = getResourceSpecifierFactory().createAnalysisEngineDescription();
    desc.setAnnotatorImplementationName(aeClass.getName());
    desc.setPrimitive(true);
    return produceAnalysisEngine(desc, resMgr, null);
  }

  public static class AddATokenAnnotator extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      addTokenAETokenClass = Token.class;
      System.out.printf("%s class loader: %s%n", getClass().getName(), getClass().getClassLoader());
      System.out.printf("[AE runtime: %s] %s %d %n", getClass().getName(),
              addTokenAETokenClass.getName(), addTokenAETokenClass.hashCode());

      new Token(aJCas, 0, aJCas.getDocumentText().length()).addToIndexes();
    }
  }

  public interface JCasCreator {
    JCas createJCas(ClassLoader cl, TypeSystemDescription tsd);
  }

  public static class JCasCreatorImpl implements JCasCreator {

    @Override
    public JCas createJCas(ClassLoader cl, TypeSystemDescription tsd) {
      try {
        printTokenClassLoaderInfo("JCas creation", cl);
        casTokenClassViaClassloader = loadTokenClass(cl);
        ResourceManager resMgr = newDefaultResourceManager();
        resMgr.setExtensionClassLoader(cl, false);
        CASImpl cas = (CASImpl) createCas(tsd, null, null, null, resMgr);
        cas.setJCasClassLoader(cl);

        casTokenClassViaCas = cas
                .createAnnotation(cas.getTypeSystem().getType(TYPE_NAME_TOKEN), 0, 0).getClass();

        return cas.getJCas();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class AddATokenAnnotatorNoJCas extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      var token = aJCas.getCas().createAnnotation(aJCas.getTypeSystem().getType(TYPE_NAME_TOKEN), 0,
              aJCas.getDocumentText().length());
      addTokenAETokenClass = token.getClass();
      System.out.printf("[AE runtime: %s] CAS class loader: %s%n", getClass().getName(),
              aJCas.getCasImpl().getJCasClassLoader());
      System.out.printf("[AE runtime: %s] AE class loader: %s%n", getClass().getName(),
              getClass().getClassLoader());
      System.out.printf("[AE runtime: %s] %s %d loaded by %s%n", getClass().getName(),
              addTokenAETokenClass.getName(), addTokenAETokenClass.hashCode(),
              addTokenAETokenClass.getClassLoader());
      aJCas.getCas().addFsToIndexes(token);
    }
  }

  public static class AddTokenToArrayAnnotatorNoJCas extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {

      var token = (AnnotationFS) aJCas.select(aJCas.getTypeSystem().getType(TYPE_NAME_TOKEN))
              .single();
      tokenClassAddedToArray = token.getClass();

      var arrayHost = aJCas.getCas()
              .createAnnotation(aJCas.getTypeSystem().getType(TYPE_NAME_ARRAY_HOST), 0, 0);
      var array = new FSArray<>(aJCas, 1);
      array.set(0, token);
      arrayHost.setFeatureValue(
              arrayHost.getType().getFeatureByBaseName(FEAT_NAME_ARRAY_HOST_VALUES), array);
      aJCas.getCas().addFsToIndexes(arrayHost);
    }
  }

  public static class FetchTokenFromArrayViaSpliteratorAnnotator extends JCasAnnotator_ImplBase {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      FeatureStructure arrayHost = aJCas.select(aJCas.getTypeSystem().getType(TYPE_NAME_ARRAY_HOST))
              .single();

      var array = (FSArray) arrayHost.getFeatureValue(
              arrayHost.getType().getFeatureByBaseName(FEAT_NAME_ARRAY_HOST_VALUES));

      tokenClassFetchedFromArray = StreamSupport.stream(array.spliterator(), false).findFirst()
              .get().getClass();
    }
  }

  public static class FetchTokenFromArrayViaToArrayAnnotator extends JCasAnnotator_ImplBase {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      var arrayHost = aJCas.select(aJCas.getTypeSystem().getType(TYPE_NAME_ARRAY_HOST)).single();

      var array = (FSArray) arrayHost.getFeatureValue(
              arrayHost.getType().getFeatureByBaseName(FEAT_NAME_ARRAY_HOST_VALUES));

      var withEmptyTemplate = array.toArray(new TOP[0])[0].getClass();
      tokenClassFetchedFromArray = array.toArray(new TOP[1])[0].getClass();
      assertThat(tokenClassFetchedFromArray).isSameAs(withEmptyTemplate);
    }
  }

  public static class FetchTheTokenAnnotator extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      fetchTokenAETokenClass = Token.class;
      System.out.printf("%s class loader: %s%n", getClass().getName(), getClass().getClassLoader());
      System.out.printf("[AE runtime: %s] %s %d %n", getClass().getName(), Token.class.getName(),
              Token.class.hashCode());
      var casToken = aJCas.getAllIndexedFS(Token.class).get();
      System.out.printf("[AE runtime CAS: %s] %s %d %n", getClass().getName(),
              casToken.getClass().getName(), casToken.getClass().hashCode());
      indexedTokenClass = casToken.getClass();

      try {
        var tokens = new ArrayList<Token>();
        aJCas.getAllIndexedFS(Token.class).forEachRemaining(tokens::add);
      } catch (ClassCastException e) {
        fetchThrowsClassCastException = true;
        e.printStackTrace();
      }
    }
  }
}
