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
package org.apache.uima.jcas.test;

import static java.util.Arrays.asList;
import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.impl.BuiltinTypeKinds;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCasFeatureOffsetTest {
  static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static final String BASE = "src/test/resources/JCasFeatureOffsetTest";

  TypeSystemDescription tdBase;
  TypeSystemDescription tdSubtype1;
  TypeSystemDescription tdSubtype2;
  TypeSystemDescription tdMerged;

  ClassLoader clSubtype1;
  ClassLoader clSubtype2;

  ResourceManager rmBase;
  ResourceManager rmSubtype1;
  ResourceManager rmSubtype2;

  @BeforeEach
  void setup() throws Exception {
    tdBase = loadTypeSystem("BaseDescriptor.xml");
    tdSubtype1 = loadTypeSystem("SubtypeDescriptor1.xml");
    tdSubtype2 = loadTypeSystem("SubtypeDescriptor2.xml");
    tdMerged = mergeTypeSystems(asList(tdBase, tdSubtype1, tdSubtype2));

    rmBase = UIMAFramework.newDefaultResourceManager();

    clSubtype1 = createScopedJCasClassKoader("SubtypeDescriptor1-jcas/classes");
    rmSubtype1 = UIMAFramework.newDefaultResourceManager();
    rmSubtype1.setExtensionClassLoader(clSubtype1, true);

    clSubtype2 = createScopedJCasClassKoader("SubtypeDescriptor2-jcas/classes");
    rmSubtype2 = UIMAFramework.newDefaultResourceManager();
    rmSubtype2.setExtensionClassLoader(clSubtype2, true);
  }

  @Test
  void thatPreMergedTypeSystemWorks() throws Exception {
    JCas jcas = createCas(tdMerged, null, null).getJCas();

    AnalysisEngine ae1 = createTestEngine(rmSubtype1);
    AnalysisEngine ae2 = createTestEngine(rmSubtype2);

    ae1.process(jcas);
    ae2.process(jcas);
  }

  @Test
  void thatNotPreMergedTypeSystemsWithIsolatedLoadersWorks() throws Exception {
    JCas jcas = createCas(tdBase, null, null).getJCas();

    AnalysisEngine ae1 = createTestEngine(rmSubtype1);
    AnalysisEngine ae2 = createTestEngine(rmSubtype2);

    ae1.process(jcas);
    ae2.process(jcas);
  }

  @Test
  void thatRebindingFeaturesToDifferentIndexFails() throws Exception {

    LOG.info("=== Initializing the JCas built-in classes");
    createCas().getJCas();

    // Bind callsite of featureX from rmSubtype1 to index 1
    createCas(tdMerged, null, null, null, rmSubtype1).getJCas();

    // Bind callsite of featureY from rmSubtype2 to index 2
    createCas(tdMerged, null, null, null, rmSubtype2).getJCas();

    // featureY from rmSubtype2 is bound to index 2 but in tdSubtype2, it would be index 1
    // because featureX does not exist here -> FAIL
    assertThatExceptionOfType(UIMARuntimeException.class).isThrownBy(() -> {
      createCas(tdSubtype2, null, null, null, rmSubtype2).getJCas();
    });
  }

  @Test
  void thatRebindingFeaturesToDifferentIndexWorksWithIsolation() throws Exception {

    LOG.info("=== Initializing the JCas built-in classes");
    createCas().getJCas();

    // Bind callsite of featureX from rmSubtype1 to index 1
    createCas(tdMerged, null, null, null, rmSubtype1).getJCas();

    // Bind callsite of featureY from rmSubtype2 to index 2
    createCas(tdMerged, null, null, null, rmSubtype2).getJCas();

    // Using an isolating classloader, we make sure that a new JCas wrapper class instance is
    // used that is not bound yet - and we should not re-use this classloader
    ClassLoader icl = new JCasIsolatingClassLoader(clSubtype2);
    ResourceManager otherRmSubtype2 = UIMAFramework.newDefaultResourceManager();
    otherRmSubtype2.setExtensionClassLoader(icl, true);

    assertThatNoException().isThrownBy(() -> {
      createCas(tdSubtype2, null, null, null, otherRmSubtype2).getJCas();
    });
  }

  private AnalysisEngine createTestEngine(ResourceManager rm1)
          throws ResourceInitializationException {
    AnalysisEngineDescription aed = getResourceSpecifierFactory().createAnalysisEngineDescription();
    aed.setPrimitive(true);
    aed.setImplementationName(TestEngine.class.getName());
    return UIMAFramework.produceAnalysisEngine(aed, rm1, null);
  }

  public static class TestEngine extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      // Nothing to do
    }
  }

  private TypeSystemDescription loadTypeSystem(String aPath)
          throws InvalidXMLException, IOException {
    return UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(new File(BASE + "/" + aPath)));
  }

  private URLClassLoader createScopedJCasClassKoader(String aPath) throws MalformedURLException {
    return new URLClassLoader(new URL[] { new File(BASE + "/" + aPath).toURI().toURL() });
  }

  public static class JCasIsolatingClassLoader extends ClassLoader {
    public JCasIsolatingClassLoader(ClassLoader aParent) {
      super(aParent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      synchronized (getClassLoadingLock(name)) {
        Class<?> alreadyLoadedClazz = findLoadedClass(name);
        if (alreadyLoadedClazz != null) {
          return alreadyLoadedClazz;
        }

        Class<?> clazz = getParent().loadClass(name);

        if (!TOP.class.isAssignableFrom(clazz)) {
          return clazz;
        }

        Object typeName;
        try {
          Field typeNameField = clazz.getField("_TypeName");
          typeName = typeNameField.get(null);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException
                | IllegalAccessException e) {
          return clazz;
        }

        if (BuiltinTypeKinds.creatableBuiltinJCas.contains(typeName)) {
          return clazz;
        }

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
          return cls;
        } catch (IOException ex) {
          throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
        }
      }
    }
  }
}
