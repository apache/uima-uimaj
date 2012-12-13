/*
 Copyright 2009
 Ubiquitous Knowledge Processing (UKP) Lab
 Technische Universitaet Darmstadt
 All rights reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package org.uimafit.factory;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createAggregate;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createAggregateDescription;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.bindExternalResource;
import static org.apache.uima.fit.factory.ExternalResourceFactory.bindResource;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createDependencyAndBind;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.component.initialize.ConfigurationParameterInitializer;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.locator.JndiResourceLocator;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.SimpleNamedResourceManager;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ParameterizedDataResource;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;
import org.apache.uima.util.CasCreationUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.component.Resource_ImplBase;

/**
 * Test case for {@link ExternalResource} annotations.
 *
 * @author Richard Eckart de Castilho
 */
public class ExternalResourceFactoryTest extends ComponentTestBase {
	private static final String EX_URI = "http://dum.my";
	private static final String EX_FILE_1 = "src/test/resources/data/html/1.html";
	private static final String EX_FILE_3 = "src/test/resources/data/html/3.html";

	@BeforeClass
	public static void initJNDI() throws Exception
	{
		// Set up JNDI context to test the JndiResourceLocator
		final SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
		Properties deDict = new Properties();
		deDict.setProperty("Hans", "proper noun");
		builder.bind("dictionaries/german", deDict);
		builder.activate();
	}

	@Test
	public void testScanBind() throws Exception {
		// Create analysis enginge description
		AnalysisEngineDescription desc = createPrimitiveDescription(DummyAE.class);

		// Bind external resources
		bindResources(desc);

		// Test with the default resource manager implementation
		AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(desc);
		assertNotNull(ae);
	}

	@Test
	public void testDirectInjection() throws Exception {
		// Create analysis enginge description
		AnalysisEngineDescription desc = createPrimitiveDescription(DummyAE2.class);

		// Bind external resources for DummyAE
		bindResources(desc);

		// Bind external resources for DummyAE2 - necessary because autowiring is disabled
		bindExternalResource(desc, DummyAE2.RES_INJECTED_POJO1, "pojoName1");
		bindExternalResource(desc, DummyAE2.RES_INJECTED_POJO2, "pojoName2");

		// Create a custom resource manager that allows to inject any Java object as an external
		// dependency
		final Map<String, Object> externalContext = new HashMap<String, Object>();
		externalContext.put("pojoName1", "Just an injected POJO");
		externalContext.put("pojoName2", new AtomicInteger(5));

		SimpleNamedResourceManager resMgr = new SimpleNamedResourceManager();
		resMgr.setExternalContext(externalContext);
		assertFalse(resMgr.isAutoWireEnabled());

		AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(desc, resMgr, null);
		assertNotNull(ae);

		ae.process(ae.newJCas());
	}

	@Test
	public void testDirectInjectionAutowire() throws Exception {
		// Create analysis enginge description
		AnalysisEngineDescription desc = createPrimitiveDescription(DummyAE2.class);

		// Bind external resources for DummyAE
		bindResources(desc);

		// Create a custom resource manager that allows to inject any Java object as an external
		// dependency
		final Map<String, Object> externalContext = new HashMap<String, Object>();
		externalContext.put(DummyAE2.RES_INJECTED_POJO1, "Just an injected POJO");
		externalContext.put(DummyAE2.RES_INJECTED_POJO2, new AtomicInteger(5));

		SimpleNamedResourceManager resMgr = new SimpleNamedResourceManager();
		resMgr.setExternalContext(externalContext);
		resMgr.setAutoWireEnabled(true);
		assertTrue(resMgr.isAutoWireEnabled());

		AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(desc, resMgr, null);
		assertNotNull(ae);

		ae.process(ae.newJCas());
	}
	
	@Test
	public void testMultiBinding() throws Exception {
		ExternalResourceDescription extDesc = createExternalResourceDescription(
				DummyResource.class);
		
		// Binding external resource to each Annotator individually
		AnalysisEngineDescription aed1 = createPrimitiveDescription(MultiBindAE.class,
				MultiBindAE.RES_KEY, extDesc);
		AnalysisEngineDescription aed2 = createPrimitiveDescription(MultiBindAE.class,
				MultiBindAE.RES_KEY, extDesc);

		// Check the external resource was injected
		AnalysisEngineDescription aaed = createAggregateDescription(aed1, aed2);
		AnalysisEngine ae = createAggregate(aaed);
		ae.process(ae.newJCas());

		MultiBindAE.reset();
		
		// Check the external resource was injected
		SimplePipeline.runPipeline(CasCreationUtils.createCas(aaed.getAnalysisEngineMetaData()), aaed);
	}

	private static void bindResources(AnalysisEngineDescription desc) throws Exception {
		bindResource(desc, DummyResource.class);
		bindResource(desc, DummyAE.RES_KEY_1, ConfigurableResource.class,
				ConfigurableResource.PARAM_VALUE, "1");
		bindResource(desc, DummyAE.RES_KEY_2, ConfigurableResource.class,
				ConfigurableResource.PARAM_VALUE, "2");
		bindResource(desc, DummyAE.RES_KEY_3, ParametrizedResource.class,
				ParametrizedResource.PARAM_EXTENSION, ".lala");
		bindResource(desc, DummySharedResourceObject.class, EX_URI,
				DummySharedResourceObject.PARAM_VALUE,"3");
		// An undefined URL may be used if the specified file/remote URL does not exist or if
		// the network is down.
		bindResource(desc, DummyAE.RES_SOME_URL, new File(EX_FILE_1).toURI().toURL());
		bindResource(desc, DummyAE.RES_SOME_OTHER_URL, new File(EX_FILE_3).toURI().toURL());
		bindResource(desc, DummyAE.RES_SOME_FILE, new File(EX_FILE_1));
		bindResource(desc, DummyAE.RES_JNDI_OBJECT, JndiResourceLocator.class,
				JndiResourceLocator.PARAM_NAME, "dictionaries/german");
		createDependencyAndBind(desc, "legacyResource", DummySharedResourceObject.class, EX_URI,
				DummySharedResourceObject.PARAM_VALUE,"3");
	}

	public static class DummyAE extends JCasAnnotator_ImplBase {
		@ExternalResource
		DummyResource r;

		static final String RES_KEY_1 = "Key1";
		@ExternalResource(key = RES_KEY_1)
		ConfigurableResource configRes1;

		static final String RES_KEY_2 = "Key2";
		@ExternalResource(key = RES_KEY_2)
		ConfigurableResource configRes2;

		static final String RES_KEY_3 = "Key3";

		@ExternalResource
		DummySharedResourceObject sharedObject;

		static final String RES_SOME_URL = "SomeUrl";
		@ExternalResource(key = RES_SOME_URL)
		DataResource someUrl;

		static final String RES_SOME_OTHER_URL = "SomeOtherUrl";
		@ExternalResource(key = RES_SOME_OTHER_URL)
		DataResource someOtherUrl;

		static final String RES_SOME_FILE = "SomeFile";
		@ExternalResource(key = RES_SOME_FILE)
		DataResource someFile;

		static final String RES_JNDI_OBJECT = "JndiObject";
		@ExternalResource(key = RES_JNDI_OBJECT)
		Properties jndiPropertes;

		@Override
		public void process(JCas aJCas) throws AnalysisEngineProcessException {
			assertNotNull(r);

			assertNotNull(configRes1);
			assertEquals("1", configRes1.getValue());

			assertNotNull(configRes2);
			assertEquals("2", configRes2.getValue());

			try {
				DataResource configuredResource = (DataResource) getContext().getResourceObject(RES_KEY_3,
						new String[] { ConfigurableDataResource.PARAM_URI, "http://dum.my/conf" });
				assertNotNull(configuredResource);
				assertEquals("http://dum.my/conf.lala", configuredResource.getUri().toString());
			}
			catch (ResourceAccessException e) {
				throw new AnalysisEngineProcessException(e);
			}

			assertNotNull(sharedObject);
			assertEquals("3", sharedObject.getValue());

			assertNotNull(sharedObject);
			assertEquals(EX_URI, sharedObject.getUrl().toString());

			assertNotNull(jndiPropertes);
			assertEquals("proper noun", jndiPropertes.get("Hans"));

			assertNotNull(someUrl);
			assertEquals(new File(EX_FILE_1).toURI().toString(), someUrl.getUri().toString());

			assertNotNull(someOtherUrl);
			assertEquals(new File(EX_FILE_3).toURI().toString(), someOtherUrl.getUri().toString());

			assertTrue(someFile.getUrl().toString().startsWith("file:"));
			assertTrue("URL [" + someFile.getUrl() + "] should end in [" + EX_FILE_1 + "]",
					someFile.getUrl().toString().endsWith(EX_FILE_1));

			try {
				assertNotNull(getContext().getResourceObject("legacyResource"));
			}
			catch (ResourceAccessException e) {
				throw new AnalysisEngineProcessException(e);
			}
		}
	}

	public static final class DummyAE2 extends DummyAE {
		static final String RES_INJECTED_POJO1 = "InjectedPojo1";
		@ExternalResource(key = RES_INJECTED_POJO1)
		String injectedString;

		static final String RES_INJECTED_POJO2 = "InjectedPojo2";
		@ExternalResource(key = RES_INJECTED_POJO2)
		Number injectedAtomicInt;

		@Override
		public void process(JCas aJCas) throws AnalysisEngineProcessException {
			super.process(aJCas);
			assertEquals("Just an injected POJO", injectedString);
			assertEquals(5, injectedAtomicInt.intValue());
		}
	}
	
	/**
	 * Example annotator that uses the share model object. In the process() we only test if the
	 * model was properly initialized by uimaFIT
	 */
	public static class MultiBindAE extends org.uimafit.component.JCasAnnotator_ImplBase {
		static int prevHashCode = -1;
		
		static final String RES_KEY = "Res";
		@ExternalResource(key = RES_KEY)
		DummyResource res;

		@Override
		public void process(JCas aJCas) throws AnalysisEngineProcessException {
			if (prevHashCode == -1) {
				prevHashCode = res.hashCode();
			}
			else {
				assertEquals(prevHashCode, res.hashCode());
			}
			
			System.out.println(getClass().getSimpleName() + ": " + res);
		}
		
		public static void reset()
		{
			prevHashCode = -1;
		}
	}

	public static final class DummyResource extends Resource_ImplBase {
		// Nothing
	}

	public static final class ConfigurableResource extends Resource_ImplBase {
		public static final String PARAM_VALUE = "Value";
		@ConfigurationParameter(name = PARAM_VALUE, mandatory = true)
		private String value;

		public String getValue() {
			return value;
		}
	}

	public static final class ConfigurableDataResource extends Resource_ImplBase implements DataResource {
		public static final String PARAM_URI = "Uri";
		@ConfigurationParameter(name = PARAM_URI, mandatory = true)
		private String uri;

		public static final String PARAM_EXTENSION = "Extension";
		@ConfigurationParameter(name = PARAM_EXTENSION, mandatory = true)
		private String extension;

		public InputStream getInputStream() throws IOException {
			return null;
		}

		public URI getUri() {
			return URI.create(uri+extension);
		}

		public URL getUrl() {
			return null;
		}
	}

	public static final class ParametrizedResource extends Resource_ImplBase implements
			ParameterizedDataResource {
		public static final String PARAM_EXTENSION = "Extension";
		@ConfigurationParameter(name = PARAM_EXTENSION, mandatory = true)
		private String extension;

		public DataResource getDataResource(String[] aParams)
				throws ResourceInitializationException {
			List<String> params = new ArrayList<String>(Arrays.asList(aParams));
			params.add(ConfigurableDataResource.PARAM_EXTENSION);
			params.add(extension);
			ExternalResourceDescription desc = ExternalResourceFactory.createExternalResourceDescription(
					null, ConfigurableDataResource.class, params.toArray(new String[params.size()]));
			return (DataResource) UIMAFramework.produceResource(desc.getResourceSpecifier(), null);
		}
	}

	public static final class DummySharedResourceObject implements SharedResourceObject {
		public static final String PARAM_VALUE = "Value";
		@ConfigurationParameter(name = PARAM_VALUE, mandatory = true)
		private String value;

		private URI uri;

		public void load(DataResource aData) throws ResourceInitializationException {
			ConfigurationParameterInitializer.initialize(this, aData);
			assertEquals(EX_URI, aData.getUri().toString());
			uri = aData.getUri();
		}

		public URI getUrl() {
			return uri;
		}

		public String getValue() {
			return value;
		}
	}
}
