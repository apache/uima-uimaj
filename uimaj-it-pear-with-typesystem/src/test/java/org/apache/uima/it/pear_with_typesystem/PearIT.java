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
package org.apache.uima.it.pear_with_typesystem;

import static java.util.Arrays.asList;
import static java.util.regex.Pattern.quote;
import static org.apache.uima.UIMAFramework.newDefaultResourceManager;
import static org.apache.uima.UIMAFramework.produceAnalysisEngine;
import static org.apache.uima.pear.tools.PackageInstaller.installPackage;
import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_component.Annotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.internal.util.Class_TCCL;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.UIMAClassLoader;
import org.apache.uima.it.pear_with_typesystem.spi.JCasClassProviderForTesting;
import org.apache.uima.it.pear_with_typesystem.type.ComplexAnnotation;
import org.apache.uima.it.pear_with_typesystem.type.ComplexAnnotationSubtype;
import org.apache.uima.it.pear_with_typesystem.type.SimpleAnnotation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.pear.tools.PackageBrowser;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.impl.ResourceManager_impl;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.spi.JCasClassProvider;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PearIT
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private File pear;
    private TypeSystemDescription tsd;
    private ClassLoader rootCl;
    private CAS cas;

    @BeforeAll
    static void setupClass() throws Exception
    {
        CasCreationUtils.createCas(); // Just make sure that the JCas system is initialized

        LOG.info("-- Base runtime context --------------------------------------------------");
        LOG.info("{} loaded using {}", SimpleAnnotation.class,
                SimpleAnnotation.class.getClassLoader());
        LOG.info("{} loaded using {}", ComplexAnnotation.class,
                ComplexAnnotation.class.getClassLoader());
    }

    @BeforeEach
    void setup() throws Exception
    {
        rootCl = getClass().getClassLoader();
        pear = new File("target/uimaj-it-pear-with-typesystem.pear");
        tsd = UIMAFramework.getXMLParser().parseTypeSystemDescription(new XMLInputSource(
                "src/main/resources/org/apache/uima/it/pear_with_typesystem/type/typeSystemDescriptor.xml"));
        cas = createCas(tsd, null, null, null);
    }

    /**
     * PEAR use {@link ComplexAnnotation} directly - we assume it is in the PEAR's local classpath
     * and available at compile time.
     */
    @Test
    void testScenario1(@TempDir File aTemp) throws Exception
    {
        LOG.info("-- PEAR context ----------------------------------------------------------");
        var pearAnnotator = installPear(aTemp, Scenario1TestAnnotator.class,
                (globalCL, pearCL) -> pearCL //
                        .hidingSPI(JCasClassProvider.class, JCasClassProviderForTesting.class) //
                        .redefining(Scenario1TestAnnotator.class) //
                        // Types included in the PEAR:
                        .redefining(SimpleAnnotation.class) //
                        .redefining(ComplexAnnotation.class) //
                        // Types provided from the global/pipeline level
                        .delegating(ComplexAnnotationSubtype.class, globalCL));

        pearAnnotator.process(cas);

        // Verify that the FS created inside the PEAR via the CAS interface correctly appears
        assertThat(cas.select(ComplexAnnotation.class).asList()) //
                .hasSize(1) //
                .allSatisfy(complexAnnotation -> assertThat(complexAnnotation)
                        .isInstanceOf(ComplexAnnotation.class));

        pearAnnotator.getUimaContextAdmin().getResourceManager().destroy();
    }

    /**
     * PEAR cannot use {@link ComplexAnnotation} directly - we assume it is not in the PEAR's local
     * classpath and not available at compile time.
     * <p>
     * PEAR can use {@code cas.createFS('X').setFeatureValue('a', 'A')} though via the CAS
     * interface.
     */
    @Test
    void testScenario2(@TempDir File aTemp) throws Exception
    {
        LOG.info("-- PEAR context ----------------------------------------------------------");
        var pearAnnotator = installPear(aTemp, Scenario2TestAnnotator.class,
                (globalCL, pearCL) -> pearCL //
                        .hidingSPI(JCasClassProvider.class, JCasClassProviderForTesting.class) //
                        .redefining(Scenario2TestAnnotator.class) //
                        // Types included in the PEAR:
                        .redefining(SimpleAnnotation.class) //
                        // Types provided from the global/pipeline level
                        .delegating(ComplexAnnotation.class, globalCL) //
                        .delegating(ComplexAnnotationSubtype.class, globalCL));

        pearAnnotator.process(cas);

        // Verify that the FS created inside the PEAR via the CAS interface correctly appears
        assertThat(cas.select(ComplexAnnotation.class).asList()) //
                .hasSize(1) //
                .allSatisfy(complexAnnotation -> assertThat(complexAnnotation)
                        .isInstanceOf(ComplexAnnotation.class));

        pearAnnotator.getUimaContextAdmin().getResourceManager().destroy();
    }

    /**
     * PEAR can use {@link ComplexAnnotation} directly - we assume it is in the PEAR's local
     * classpath and available at compile time.
     * <p>
     * However, PEAR does not know about {@link ComplexAnnotationSubtype}. Yet, it will find that
     * the CAS contains an annotation of that type.
     */
    @Test
    void testScenario3(@TempDir File aTemp) throws Exception
    {
        var complexAnnotationSubtype = new ComplexAnnotationSubtype(cas.getJCas(), 0, 0);
        complexAnnotationSubtype.addToIndexes();

        LOG.info("-- PEAR context ----------------------------------------------------------");
        var pearAnnotator = installPear(aTemp, Scenario3TestAnnotator.class,
                (globalCL, pearCL) -> pearCL //
                        .hidingSPI(JCasClassProvider.class, JCasClassProviderForTesting.class) //
                        .redefining(Scenario3TestAnnotator.class) //
                        // Types included in the PEAR:
                        .redefining(ComplexAnnotation.class) //
                        .redefining(SimpleAnnotation.class) //
                        // Types provided from the global/pipeline level
                        .delegating(ComplexAnnotationSubtype.class, globalCL));

        assertThatExceptionOfType(AnalysisEngineProcessException.class) //
                .isThrownBy(() -> pearAnnotator.process(cas)) //
                .withRootCauseInstanceOf(ClassCastException.class);

        pearAnnotator.getUimaContextAdmin().getResourceManager().destroy();
    }

    private AnalysisEngine installPear(File aTemp,
            Class<? extends Annotator_ImplBase> aMainComponent,
            BiFunction<ClassLoader, IsolatingUIMAClassloader, UIMAClassLoader> clConfigurer)
        throws Exception, InvalidXMLException, IOException, ResourceInitializationException
    {
        var packageBrowser = installPackage(aTemp, pear, false, false);
        reconfigureMainComponent(packageBrowser, aMainComponent);
        var pearSpecifier = UIMAFramework.getXMLParser()
                .parsePearSpecifier(new XMLInputSource(packageBrowser.getComponentPearDescPath()));
        var resMgrForPear = new IsolatingResourceManager_impl(clConfigurer);
        var pearAnnotator = produceAnalysisEngine(pearSpecifier, resMgrForPear, null);
        return pearAnnotator;
    }

    private void reconfigureMainComponent(PackageBrowser aPackageBrowser,
            Class<? extends Annotator_ImplBase> aMainComponent)
        throws Exception
    {
        var mainComponentDescPath = aPackageBrowser.getInstallationDescriptor()
                .getMainComponentDesc();
        var in = new XMLInputSource(mainComponentDescPath);
        var specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
        var analysisEngineDescription = (AnalysisEngineDescription) specifier;
        analysisEngineDescription.setAnnotatorImplementationName(aMainComponent.getName());
        try (var os = new FileOutputStream(mainComponentDescPath)) {
            analysisEngineDescription.toXML(os);
        }

        LOG.info("Changed main component to {}", aMainComponent.getName());
    }

    private final class IsolatingResourceManager_impl
        extends ResourceManager_impl
    {
        private BiFunction<ClassLoader, IsolatingUIMAClassloader, UIMAClassLoader> clConfigurer;

        public IsolatingResourceManager_impl(
                BiFunction<ClassLoader, IsolatingUIMAClassloader, UIMAClassLoader> aClConfigurer)
        {
            clConfigurer = aClConfigurer;
        }

        public IsolatingResourceManager_impl(Map<String, Object> aResourceMap,
                Map<String, ResourceRegistration> aInternalResourceRegistrationMap,
                Map<String, Class<?>> aParameterizedResourceImplClassMap,
                Map<String, Class<?>> aInternalParameterizedResourceImplClassMap,
                Map<List<Object>, Object> aParameterizedResourceInstanceMap)
        {
            super(aResourceMap, aInternalResourceRegistrationMap,
                    aParameterizedResourceImplClassMap, aInternalParameterizedResourceImplClassMap,
                    aParameterizedResourceInstanceMap);
        }

        @Override
        public synchronized void setExtensionClassPath(String aClasspath, boolean aResolveResource)
            throws MalformedURLException
        {
            var parentCL = Class_TCCL.get_parent_cl();
            var uimaCL = clConfigurer.apply(parentCL, new IsolatingUIMAClassloader(
                    "PEAR Classloader", parentCL, Misc.classpath2urls(aClasspath)));
            setExtensionClassLoader(uimaCL, aResolveResource);
        }

        @Override
        public ResourceManager_impl copy()
        {
            var rm = new IsolatingResourceManager_impl(mResourceMap,
                    mInternalResourceRegistrationMap, mParameterizedResourceImplClassMap,
                    mInternalParameterizedResourceImplClassMap, mParameterizedResourceInstanceMap);

            rm.clConfigurer = clConfigurer;

            try {
                // non-final fields init
                var uimaCLField = ResourceManager_impl.class.getDeclaredField("uimaCL");
                uimaCLField.setAccessible(true);
                uimaCLField.set(rm, uimaCLField.get(this));

                var importCacheField = ResourceManager_impl.class.getDeclaredField("importCache");
                importCacheField.setAccessible(true);
                importCacheField.set(rm, importCacheField.get(this));

                var importUrlsCacheField = ResourceManager_impl.class
                        .getDeclaredField("importUrlsCache");
                importUrlsCacheField.setAccessible(true);
                importUrlsCacheField.set(rm, importUrlsCacheField.get(this));
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }

            return rm;
        }
    }

    public interface JCasCreator
    {
        JCas createJCas(ClassLoader cl, TypeSystemDescription tsd);
    }

    public static class JCasCreatorImpl
        implements JCasCreator
    {

        @Override
        public JCas createJCas(ClassLoader cl, TypeSystemDescription tsd)
        {
            try {
                var resMgr = newDefaultResourceManager();
                resMgr.setExtensionClassLoader(cl, false);
                var cas = (CASImpl) createCas(tsd, null, null, null, resMgr);
                cas.setJCasClassLoader(cl);
                return cas.getJCas();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    class IsolatingUIMAClassloader
        extends UIMAClassLoader
    {
        private final Set<String> hideResourcesPatterns = new HashSet<>();
        private final Map<String, ClassLoader> resourceDelegates = new LinkedHashMap<>();

        private final Set<String> hideClassesPatterns = new HashSet<>();
        private final Set<String> redefineClassesPatterns = new HashSet<>();
        private final Map<String, ClassLoader> classDelegates = new LinkedHashMap<>();
        private final String id;

        private Map<String, Class<?>> loadedClasses = new HashMap<>();

        public IsolatingUIMAClassloader(String aName, ClassLoader aParent, URL... aClasspath)
        {
            super(aClasspath, aParent);
            id = aName;
        }

        @Override
        public void close() throws IOException
        {
            if (getParent() instanceof UIMAClassLoader uimaClassLoader) {
                uimaClassLoader.close();
            }

            super.close();
        }

        @SafeVarargs
        public final <T> IsolatingUIMAClassloader delegatingSPI(ClassLoader aDelegate,
                Class<T> aServiceInterface, Class<? extends T>... aServiceProviders)
        {
            delegatingResources(quote("META-INF/services/" + aServiceInterface.getName()),
                    aDelegate);

            for (var serviceProvider : aServiceProviders) {
                delegating(serviceProvider, aDelegate);
            }

            return this;
        }

        @SafeVarargs
        public final <T> IsolatingUIMAClassloader hidingSPI(Class<T> aServiceInterface,
                Class<? extends T>... aServiceProviders)
        {
            hidingResources(quote("META-INF/services/" + aServiceInterface.getName()));

            for (var serviceProvider : aServiceProviders) {
                hiding(serviceProvider);
            }

            return this;
        }

        public IsolatingUIMAClassloader hidingResources(String... aPatterns)
        {
            hideResourcesPatterns.addAll(asList(aPatterns));
            return this;
        }

        public IsolatingUIMAClassloader delegatingResources(String pattern, ClassLoader delegate)
        {
            resourceDelegates.put(pattern, delegate);
            return this;
        }

        public IsolatingUIMAClassloader hiding(Package... aPackages)
        {
            for (var pack : aPackages) {
                hideClassesPatterns.add(quote(pack.getName()) + "\\..*");
            }
            return this;
        }

        public IsolatingUIMAClassloader hiding(Class<?>... aClasses)
        {
            for (var clazz : aClasses) {
                hideClassesPatterns.add(quote(clazz.getName()));
            }
            return this;
        }

        public IsolatingUIMAClassloader hiding(String... aPatterns)
        {
            hideClassesPatterns.addAll(asList(aPatterns));
            return this;
        }

        public IsolatingUIMAClassloader redefining(Package... aPackages)
        {
            for (var pack : aPackages) {
                redefineClassesPatterns.add(quote(pack.getName()) + "\\..*");
            }
            return this;
        }

        public IsolatingUIMAClassloader redefining(Class<?>... aClasses)
        {
            for (var clazz : aClasses) {
                redefineClassesPatterns.add(quote(clazz.getName()));
            }
            return this;
        }

        public IsolatingUIMAClassloader redefining(String... aPatterns)
        {
            redefineClassesPatterns.addAll(asList(aPatterns));
            return this;
        }

        public IsolatingUIMAClassloader delegating(Class<?> aClass, ClassLoader aDelegate)
        {
            classDelegates.put(quote(aClass.getName()), aDelegate);
            return this;
        }

        public IsolatingUIMAClassloader delegating(String pattern, ClassLoader delegate)
        {
            classDelegates.put(pattern, delegate);
            return this;
        }

        @Override
        public String toString()
        {
            var sb = new StringBuilder();
            sb.append("[");
            sb.append(id);
            sb.append(", loaded=");
            sb.append(loadedClasses.size());
            sb.append("]");
            return sb.toString();
        }

        @Override
        public URL getResource(String aName)
        {
            LOG.debug("[{}] looking up resource: {}", id, aName);

            var delegate = resourceDelegates.entrySet().stream() //
                    .filter(e -> aName.matches(e.getKey())) //
                    .map(Entry::getValue) //
                    .findFirst();
            if (delegate.isPresent()) {
                LOG.debug("[{}] delegagted resource access: {} -> {}", id, aName, delegate.get());
                return delegate.get().getResource(aName);
            }

            if (hideResourcesPatterns.stream().anyMatch(aName::matches)) {
                LOG.debug("[{}] prevented access to hidden resource: {}", id, aName);
                return null;
            }

            return super.findResource(aName);
        }

        @Override
        public Enumeration<URL> getResources(String aName) throws IOException
        {
            LOG.debug("[{}] looking up resources: {}", id, aName);

            var delegate = resourceDelegates.entrySet().stream() //
                    .filter(e -> aName.matches(e.getKey())) //
                    .map(Entry::getValue) //
                    .findFirst();
            if (delegate.isPresent()) {
                LOG.debug("[{}] delegagted resources access: {} -> {}", id, aName, delegate.get());
                return delegate.get().getResources(aName);
            }

            if (hideResourcesPatterns.stream().anyMatch(aName::matches)) {
                LOG.debug("[{}] prevented access to hidden resources: {}", id, aName);
                return Collections.emptyEnumeration();
            }

            return super.findResources(aName);
        }

        @Override
        protected Class<?> loadClass(String aName, boolean aResolve) throws ClassNotFoundException
        {
            synchronized (getClassLoadingLock(aName)) {
                LOG.debug("[{}] looking up class: {}", id, aName);

                var delegate = classDelegates.entrySet().stream() //
                        .filter(e -> aName.matches(e.getKey())) //
                        .map(Entry::getValue) //
                        .findFirst();
                if (delegate.isPresent()) {
                    LOG.debug("[{}] delegagted access: {} -> {}", id, aName, delegate.get());
                    return delegate.get().loadClass(aName);
                }

                if (hideClassesPatterns.stream().anyMatch(aName::matches)) {
                    LOG.debug("[{}] prevented access to hidden class: {}", id, aName);
                    throw new ClassNotFoundException(aName);
                }

                if (redefineClassesPatterns.stream().anyMatch(aName::matches)) {
                    Class<?> loadedClass = loadedClasses.get(aName);
                    if (loadedClass != null) {
                        return loadedClass;
                    }

                    LOG.debug("[{}] redefining class: {}", id, aName);

                    String internalName = aName.replace(".", "/") + ".class";
                    URL url = getParent().getResource(internalName);
                    if (url == null) {
                        throw new ClassNotFoundException(aName);
                    }

                    try {
                        // Disable JAR cache so JUnit can delete the temporary folder after the test
                        var urlConnection = url.openConnection();
                        urlConnection.setDefaultUseCaches(false);

                        try (InputStream is = urlConnection.getInputStream()) {
                            var buffer = new ByteArrayOutputStream();
                            is.transferTo(buffer);
                            byte[] bytes = buffer.toByteArray();
                            Class<?> cls = defineClass(aName, bytes, 0, bytes.length);
                            if (cls.getPackage() == null) {
                                int packageSeparator = aName.lastIndexOf('.');
                                if (packageSeparator != -1) {
                                    String packageName = aName.substring(0, packageSeparator);
                                    definePackage(packageName, null, null, null, null, null, null,
                                            null);
                                }
                            }
                            loadedClasses.put(aName, cls);
                            return cls;
                        }
                    }
                    catch (IOException ex) {
                        throw new ClassNotFoundException(
                                "Cannot load resource for class [" + aName + "]", ex);
                    }
                }

                return super.loadClass(aName, aResolve);
            }
        }
    }
}
