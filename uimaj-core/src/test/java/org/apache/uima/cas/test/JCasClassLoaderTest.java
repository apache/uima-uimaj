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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.uima.analysis_component.Annotator_ImplBase;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
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
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.Before;
import org.junit.Test;

public class JCasClassLoaderTest {
  
  public static final String TYPE_NAME_TOKEN = Token.class.getName();
  public static final String TYPE_NAME_ARRAY_HOST = "uima.testing.ArrayHost";
  public static final String FEAT_NAME_ARRAY_HOST_VALUES = "values";
    
  public static Class casTokenClassViaClassloader;
  public static Class casTokenClassViaCas;
  public static Class addTokenAETokenClass;
  public static Class fetchTokenAETokenClass;
  public static Class indexedTokenClass;
  public static boolean fetchThrowsClassCastException;

  public static Class tokenClassAddedToArray;
  public static Class tokenClassFetchedFromArray;

  @Before
  public void setup()
  {
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
  public void thatCASCanBeDefinedWithoutJCasWrappersAndTheyComeInWithAnnotatorsViaClasspath() throws Exception {
    ClassLoader rootCl = getClass().getClassLoader();

    // We do not want the CAS to know the Token JCas wrapper when it gets initialized
    IsolatingClassloader clForCas = new IsolatingClassloader("CAS", rootCl)
            .hiding("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");
    
    File cpBase = new File("target/test-output/JCasClassLoaderTest/classes");
    File cpPackageBase = new File(cpBase, "org/apache/uima/cas/test");
    cpPackageBase.mkdirs();
    FileUtils.copyFile(
            new File("target/test-classes/org/apache/uima/cas/test/Token.class"), 
            new File(cpPackageBase, "Token.class"));
    FileUtils.copyFile(
            new File("target/test-classes/org/apache/uima/cas/test/JCasClassLoaderTest$AddATokenAnnotator.class"), 
            new File(cpPackageBase, "JCasClassLoaderTest$AddATokenAnnotator.class"));
    FileUtils.copyFile(
            new File("target/test-classes/org/apache/uima/cas/test/JCasClassLoaderTest$FetchTheTokenAnnotator.class"), 
            new File(cpPackageBase, "JCasClassLoaderTest$FetchTheTokenAnnotator.class"));
    
    JCas jcas = makeJCas(clForCas);
    AnalysisEngine addATokenAnnotator = makeAnalysisEngine(AddATokenAnnotator.class, cpBase);
    AnalysisEngine fetchTheTokenAnnotator = makeAnalysisEngine(FetchTheTokenAnnotator.class, cpBase);
    
    jcas.setDocumentText("test");
    
    addATokenAnnotator.process(jcas);
    fetchTheTokenAnnotator.process(jcas);
    
    try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
      softly.assertThat(casTokenClassViaClassloader).isNull();
      softly.assertThat(casTokenClassViaCas).isSameAs(Annotation.class);
      softly.assertThat(addTokenAETokenClass).isNotNull();
      softly.assertThat(fetchTokenAETokenClass).isNotNull();
      softly.assertThat(addTokenAETokenClass)
          .as("AddTokenAnnotator and FetchTokenAnnotator use different Token wrappers")
          .isNotEqualTo(fetchTokenAETokenClass);
      softly.assertThat(indexedTokenClass)
          .as("JCas in FetchTokenAnnotator provides Token JCas wrapper from FetchTokenAnnotator CL")
          .isEqualTo(fetchTokenAETokenClass);
      softly.assertThat(fetchThrowsClassCastException)
          .as("Classcast exception thrown when trying to retrieve Token from index")
          .isFalse();
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
  public void thatCASCanBeDefinedWithoutJCasWrappersAndTheyComeInWithAnnotatorsViaClassloader() throws Exception {
    ClassLoader rootCl = getClass().getClassLoader();

    // We do not want the CAS to know the Token JCas wrapper when it gets initialized
    IsolatingClassloader clForCas = new IsolatingClassloader("CAS", rootCl)
            .hiding("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    ClassLoader clForAddATokenAnnotator = new IsolatingClassloader("AddATokenAnnotator", rootCl)
            .redefining("^.*AddATokenAnnotator$")
            .redefining("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    ClassLoader clForFetchTheTokenAnnotator = new IsolatingClassloader("FetchTheTokenAnnotator",
            rootCl)
            .redefining("^.*FetchTheTokenAnnotator$")
            .redefining("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    JCas jcas = makeJCas(clForCas);
    AnalysisEngine addATokenAnnotator = makeAnalysisEngine(AddATokenAnnotator.class,
            clForAddATokenAnnotator);
    AnalysisEngine fetchTheTokenAnnotator = makeAnalysisEngine(FetchTheTokenAnnotator.class,
            clForFetchTheTokenAnnotator);
    
    jcas.setDocumentText("test");
    
    addATokenAnnotator.process(jcas);
    fetchTheTokenAnnotator.process(jcas);
    
    try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
      softly.assertThat(casTokenClassViaClassloader).isNull();
      softly.assertThat(casTokenClassViaCas).isSameAs(Annotation.class);
      softly.assertThat(addTokenAETokenClass).isNotNull();
      softly.assertThat(fetchTokenAETokenClass).isNotNull();
      softly.assertThat(addTokenAETokenClass)
          .as("AddTokenAnnotator and FetchTokenAnnotator use different Token wrappers")
          .isNotEqualTo(fetchTokenAETokenClass);
      softly.assertThat(indexedTokenClass)
          .as("JCas in FetchTokenAnnotator provides Token JCas wrapper from FetchTokenAnnotator CL")
          .isEqualTo(fetchTokenAETokenClass);
      softly.assertThat(fetchThrowsClassCastException)
          .as("Classcast exception thrown when trying to retrieve Token from index")
          .isFalse();
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
   * {@link ResourceManager} of the engines to load the JCas wrapper classes. So each of the analysis
   * engines should use its own version of the JCas wrappers to access the CAS. In particular, they
   * should not use the global JCas wrappers which were known to the JCas when it was first
   * initialized.
   */
  @Test
  public void thatAnnotatorsCanLocallyUseDifferentJCasWrappers() throws Exception {
    ClassLoader rootCl = getClass().getClassLoader();

    IsolatingClassloader clForCas = new IsolatingClassloader("CAS", rootCl);

    ClassLoader clForAddATokenAnnotator = new IsolatingClassloader("AddATokenAnnotator", rootCl)
            .redefining("^.*AddATokenAnnotator$")
            .redefining("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    ClassLoader clForFetchTheTokenAnnotator = new IsolatingClassloader("FetchTheTokenAnnotator",
            rootCl)
            .redefining("^.*FetchTheTokenAnnotator$")
            .redefining("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    JCas jcas = makeJCas(clForCas);
    AnalysisEngine addATokenAnnotator = makeAnalysisEngine(AddATokenAnnotator.class,
            clForAddATokenAnnotator);
    AnalysisEngine fetchTheTokenAnnotator = makeAnalysisEngine(FetchTheTokenAnnotator.class,
            clForFetchTheTokenAnnotator);
    
    jcas.setDocumentText("test");
    
    addATokenAnnotator.process(jcas);
    fetchTheTokenAnnotator.process(jcas);

    try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
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
      softly.assertThat(indexedTokenClass)
          .as("JCas in FetchTokenAnnotator provides Token JCas wrapper from FetchTokenAnnotator CL")
          .isEqualTo(fetchTokenAETokenClass);
      softly.assertThat(fetchThrowsClassCastException)
          .as("Classcast exception thrown when trying to retrieve Token from index")
          .isFalse();
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
   * {@link ResourceManager} of the engines to load the JCas wrapper classes. Since the JCas wrappers
   * are loaded through the same classloader by both engines, it they should have the same type in
   * both annotators.
   */
  @Test
  public void thatTypeSystemCanComeFromItsOwnClassLoader() throws Exception {
    ClassLoader rootCl = getClass().getClassLoader();

    IsolatingClassloader clForCas = new IsolatingClassloader("CAS", rootCl)
            .hiding("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    ClassLoader clForTS = new IsolatingClassloader("TS", rootCl)
            .redefining("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    ClassLoader clForAddATokenAnnotator = new IsolatingClassloader("AddATokenAnnotator", rootCl)
            .redefining("^.*AddATokenAnnotator$")
            .delegating("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*", clForTS);

    ClassLoader clForFetchTheTokenAnnotator = new IsolatingClassloader("FetchTheTokenAnnotator",
            rootCl)
            .redefining("^.*FetchTheTokenAnnotator$")
            .delegating("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*", clForTS);

    JCas jcas = makeJCas(clForCas);
    AnalysisEngine addATokenAnnotator = makeAnalysisEngine(AddATokenAnnotator.class,
            clForAddATokenAnnotator);
    AnalysisEngine fetchTheTokenAnnotator = makeAnalysisEngine(FetchTheTokenAnnotator.class,
            clForFetchTheTokenAnnotator);
    
    jcas.setDocumentText("test");
    
    addATokenAnnotator.process(jcas);
    fetchTheTokenAnnotator.process(jcas);
    
    try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
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
      softly.assertThat(indexedTokenClass)
          .as("JCas in FetchTokenAnnotator provides Token JCas wrapper from FetchTokenAnnotator CL")
          .isEqualTo(fetchTokenAETokenClass);
      softly.assertThat(fetchThrowsClassCastException)
          .as("Classcast exception thrown when trying to retrieve Token from index")
          .isFalse();
    }
  }
  
  @Test
  public void thatFSArraySpliteratorReturnsProperJCasWrapper() throws Exception {
    ClassLoader rootCl = getClass().getClassLoader();

    IsolatingClassloader clForCas = new IsolatingClassloader("CAS", rootCl)
            .hiding("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    ClassLoader clForCreators = new IsolatingClassloader("Creators", rootCl)
            .hiding("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*")
            .redefining("^.*AddATokenAnnotatorNoJCas$")
            .redefining("^.*AddTokenToArrayAnnotatorNoJCas$");

    ClassLoader clForAccessors = new IsolatingClassloader("Accessors", rootCl)
            .redefining("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*")
            .redefining("^.*FetchTokenFromArrayViaSpliteratorAnnotator$");

    TypeSystemDescription tsd = mergeTypeSystems(
            asList(loadTokensAndSentencesTS(), makeArrayTestTS()));

    JCas jcas = makeJCas(clForCas, tsd);
    AnalysisEngine addATokenAnnotator = makeAnalysisEngine(AddATokenAnnotatorNoJCas.class,
            clForCreators);
    AnalysisEngine addTokenToArrayAnnotator = makeAnalysisEngine(
            AddTokenToArrayAnnotatorNoJCas.class, clForCreators);
    AnalysisEngine fetchTokenFromArrayViaSpliteratorAnnotator = makeAnalysisEngine(
            FetchTokenFromArrayViaSpliteratorAnnotator.class, clForAccessors);

    jcas.setDocumentText("test");

    addATokenAnnotator.process(jcas);
    addTokenToArrayAnnotator.process(jcas);
    fetchTokenFromArrayViaSpliteratorAnnotator.process(jcas);

    try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
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
  public void thatFSArrayToArrayReturnsProperJCasWrapper() throws Exception {
    ClassLoader rootCl = getClass().getClassLoader();

    IsolatingClassloader clForCas = new IsolatingClassloader("CAS", rootCl)
            .hiding("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    ClassLoader clForCreators = new IsolatingClassloader("Creators", rootCl)
            .hiding("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*")
            .redefining("^.*AddATokenAnnotatorNoJCas$")
            .redefining("^.*AddTokenToArrayAnnotatorNoJCas$");

    ClassLoader clForAccessors = new IsolatingClassloader("Accessors", rootCl)
            .redefining("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*")
            .redefining("^.*FetchTokenFromArrayViaToArrayAnnotator$");

    TypeSystemDescription tsd = mergeTypeSystems(
            asList(loadTokensAndSentencesTS(), makeArrayTestTS()));

    JCas jcas = makeJCas(clForCas, tsd);
    AnalysisEngine addATokenAnnotator = makeAnalysisEngine(AddATokenAnnotatorNoJCas.class,
            clForCreators);
    AnalysisEngine addTokenToArrayAnnotator = makeAnalysisEngine(
            AddTokenToArrayAnnotatorNoJCas.class, clForCreators);
    AnalysisEngine fetchTokenFromArrayViaSpliteratorAnnotator = makeAnalysisEngine(
            FetchTokenFromArrayViaToArrayAnnotator.class, clForAccessors);

    jcas.setDocumentText("test");

    addATokenAnnotator.process(jcas);
    addTokenToArrayAnnotator.process(jcas);
    fetchTokenFromArrayViaSpliteratorAnnotator.process(jcas);

    try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
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

  public static Class<?> loadTokenClass(ClassLoader cl)
  {
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

  private TypeSystemDescription makeArrayTestTS() throws InvalidXMLException, IOException
  {
      TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
      TypeDescription arrayHost = tsd.addType(TYPE_NAME_ARRAY_HOST, "", TYPE_NAME_ANNOTATION);
      arrayHost.addFeature(FEAT_NAME_ARRAY_HOST_VALUES, "", TYPE_NAME_FS_ARRAY,
              TYPE_NAME_ANNOTATION, true);
      return tsd;
  }

  private TypeSystemDescription loadTokensAndSentencesTS() throws InvalidXMLException, IOException
  {
      return getXMLParser().parseTypeSystemDescription(new XMLInputSource(
              new File("src/test/resources/CASTests/desc/TokensAndSentencesTS.xml")));
  }
  
  /**
   * Creates a new JCas and sets it up so it uses the given classloader to load its JCas wrappers.
   */
  private JCas makeJCas(IsolatingClassloader cl)
          throws Exception {
    return makeJCas(cl, loadTokensAndSentencesTS());
  }
  
  private JCas makeJCas(IsolatingClassloader cl, TypeSystemDescription tsd)
          throws Exception {
    cl.redefining("^.*JCasCreatorImpl$");
    Class jcasCreatorClass = cl.loadClass(JCasCreatorImpl.class.getName());
    JCasCreator creator = (JCasCreator) jcasCreatorClass.newInstance();
    return creator.createJCas(cl, tsd);
  }

  /**
   * Creates a new analysis engine from the given class using the given classloader and sets it up
   * so it using the given classloader to load its JCas wrappers.
   */
  private AnalysisEngine makeAnalysisEngine(Class<? extends Annotator_ImplBase> aeClass,
          ClassLoader cl) throws ResourceInitializationException, MalformedURLException {
    ResourceManager resMgr = newDefaultResourceManager();
    resMgr.setExtensionClassLoader(cl, false);
    printTokenClassLoaderInfo("AE creation: " + aeClass.getSimpleName(), resMgr.getExtensionClassLoader());
    AnalysisEngineDescription desc = getResourceSpecifierFactory()
            .createAnalysisEngineDescription();
    desc.setAnnotatorImplementationName(aeClass.getName());
    desc.setPrimitive(true);
    return produceAnalysisEngine(desc, resMgr, null);
  }

  /**
   * Creates a new analysis engine from the given class using the given classpath and sets it up
   * so it using the given classpath to load its JCas wrappers.
   */
  private AnalysisEngine makeAnalysisEngine(Class<? extends Annotator_ImplBase> aeClass,
          File... classPath) throws ResourceInitializationException, MalformedURLException {
    String cp = Stream.of(classPath).map(Object::toString).collect(joining(File.pathSeparator));
    ResourceManager resMgr = newDefaultResourceManager();
    resMgr.setExtensionClassPath(cp, false);
    printTokenClassLoaderInfo("AE creation: " + aeClass.getSimpleName(), resMgr.getExtensionClassLoader());
    AnalysisEngineDescription desc = getResourceSpecifierFactory()
            .createAnalysisEngineDescription();
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
  
  public static interface JCasCreator {
    JCas createJCas(ClassLoader cl, TypeSystemDescription tsd);
  }
  
  public static class JCasCreatorImpl implements JCasCreator {

    @Override
    public JCas createJCas(ClassLoader cl, TypeSystemDescription tsd)
    {
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
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class AddATokenAnnotatorNoJCas extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      AnnotationFS token = aJCas.getCas().createAnnotation(
              aJCas.getTypeSystem().getType(TYPE_NAME_TOKEN), 0,
              aJCas.getDocumentText().length());
      addTokenAETokenClass = token.getClass();
      System.out.printf("[AE runtime: %s] CAS class loader: %s%n", getClass().getName(), aJCas.getCasImpl().getJCasClassLoader());
      System.out.printf("[AE runtime: %s] AE class loader: %s%n", getClass().getName(), getClass().getClassLoader());
      System.out.printf("[AE runtime: %s] %s %d loaded by %s%n", getClass().getName(), addTokenAETokenClass.getName(),
              addTokenAETokenClass.hashCode(), addTokenAETokenClass.getClassLoader());
      aJCas.getCas().addFsToIndexes(token);
    }
  }

  public static class AddTokenToArrayAnnotatorNoJCas extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {

      AnnotationFS token = (AnnotationFS) aJCas
              .select(aJCas.getTypeSystem().getType(TYPE_NAME_TOKEN))
              .single();
      tokenClassAddedToArray = token.getClass();
      
      AnnotationFS arrayHost = aJCas.getCas()
              .createAnnotation(aJCas.getTypeSystem().getType(TYPE_NAME_ARRAY_HOST), 0, 0);
      FSArray array = new FSArray<>(aJCas, 1);
      array.set(0, token);
      arrayHost.setFeatureValue(
              arrayHost.getType().getFeatureByBaseName(FEAT_NAME_ARRAY_HOST_VALUES), array);
      aJCas.getCas().addFsToIndexes(arrayHost);
    }
  }

  public static class FetchTokenFromArrayViaSpliteratorAnnotator extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      FeatureStructure arrayHost = aJCas.select(aJCas.getTypeSystem().getType(TYPE_NAME_ARRAY_HOST))
              .single();
      
      FSArray array = (FSArray) arrayHost.getFeatureValue(
              arrayHost.getType().getFeatureByBaseName(FEAT_NAME_ARRAY_HOST_VALUES));
      
      tokenClassFetchedFromArray = StreamSupport.stream(array.spliterator(), false)
              .findFirst()
              .get().getClass();
    }
  }

  public static class FetchTokenFromArrayViaToArrayAnnotator extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      FeatureStructure arrayHost = aJCas.select(aJCas.getTypeSystem().getType(TYPE_NAME_ARRAY_HOST))
              .single();
      
      FSArray array = (FSArray) arrayHost.getFeatureValue(
              arrayHost.getType().getFeatureByBaseName(FEAT_NAME_ARRAY_HOST_VALUES));
      
      Class withEmptyTemplate = array.toArray(new TOP[0])[0].getClass();
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
      Object casToken = aJCas.getAllIndexedFS(Token.class).get();
      System.out.printf("[AE runtime CAS: %s] %s %d %n", getClass().getName(),
              casToken.getClass().getName(), casToken.getClass().hashCode());
      indexedTokenClass = casToken.getClass();
      
      try {
        List<Token> tokens = new ArrayList<>();
        aJCas.getAllIndexedFS(Token.class).forEachRemaining(tokens::add);
      }
      catch (ClassCastException e) {
        fetchThrowsClassCastException = true;
        e.printStackTrace();
      }
    }
  }

  /**
   * Special ClassLoader that helps us modeling different class loader topologies.
   */
  public static class IsolatingClassloader extends ClassLoader {

    private final Set<String> hideClassesPatterns = new HashSet<>();
    private final Set<String> redefineClassesPatterns = new HashSet<>();
    private final Map<String, ClassLoader> delegates = new LinkedHashMap<>();
    private final String id;

    private Map<String, Class<?>> loadedClasses = new HashMap<>();

    public IsolatingClassloader(String name, ClassLoader parent) {
      super(parent);

      id = name;
    }
    
    public IsolatingClassloader hiding(String... patterns)
    {
      hideClassesPatterns.addAll(asList(patterns));
      return this;
    }

    public IsolatingClassloader redefining(String... patterns)
    {
      redefineClassesPatterns.addAll(asList(patterns));
      return this;
    }

    public IsolatingClassloader delegating(String pattern, ClassLoader delegate)
    {
      delegates.put(pattern, delegate);
      return this;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      sb.append(id);
      sb.append(", loaded=");
      sb.append(loadedClasses.size());
      sb.append("]");
      return sb.toString();
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      synchronized (getClassLoadingLock(name)) {
        Optional<ClassLoader> delegate = delegates.entrySet().stream()
                .filter(e -> name.matches(e.getKey())).map(Entry::getValue).findFirst();
        if (delegate.isPresent()) {
          return delegate.get().loadClass(name);
        }
        
        if (hideClassesPatterns.stream().anyMatch(name::matches)) {
          System.out.printf("[%s] prevented access to hidden class: %s%n", id, name);
          throw new ClassNotFoundException(name);
        }

        if (redefineClassesPatterns.stream().anyMatch(name::matches)) {
          Class<?> loadedClass = loadedClasses.get(name);
          if (loadedClass != null) {
            return loadedClass;
          }
          
          System.out.printf("[%s] redefining class: %s%n", id, name);

          String internalName = name.replace(".", "/") + ".class";
          InputStream is = getParent().getResourceAsStream(internalName);
          if (is == null) {
            throw new ClassNotFoundException(name);
          }

          try {
            byte[] bytes = IOUtils.toByteArray(is);
            Class<?> cls = defineClass(name, bytes, 0, bytes.length);
            if (cls.getPackage() == null) {
              int packageSeparator = name.lastIndexOf('.');
              if (packageSeparator != -1) {
                String packageName = name.substring(0, packageSeparator);
                definePackage(packageName, null, null, null, null, null, null, null);
              }
            }
            loadedClasses.put(name, cls);
            return cls;
          } catch (IOException ex) {
            throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
          }
        }

        return super.loadClass(name, resolve);
      }
    }
  }
}