/*
 Copyright 2011
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
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitive;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.bindExternalResource;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.resource.CustomResourceSpecifier;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.metadata.ExternalResourceBinding;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.uimafit.component.CasAnnotator_ImplBase;
import org.uimafit.component.ExternalResourceAware;
import org.uimafit.factory.testRes.TestExternalResource;
import org.uimafit.factory.testRes.TestSharedResourceObject;
import org.xml.sax.SAXException;

/**
 * @author Richard Eckart de Castilho
 * @author Shuo Yang
 */
public class AnalysisEngineFactoryExternalResourceTest {
	/**
	 * Test simple injection.
	 */
	@Test
	public void resource_testInjection() throws Exception {
		AnalysisEngineDescription aeDesc = saveLoad(createPrimitiveDescription(
				TestAnalysisEngineWithResource.class,
				TestAnalysisEngineWithResource.PARAM_RESOURCE,
				createExternalResourceDescription(TestExternalResource.class,
						TestExternalResource.PARAM_VALUE, TestExternalResource.EXPECTED_VALUE)));
		
		AnalysisEngine ae = createPrimitive(aeDesc);
		ae.process(ae.newCAS());
	}

	/**
	 * Test shared simple injection.
	 */
	@Test
	public void resource_testSharedInjection() throws Exception {
		ExternalResourceDescription resDesc = createExternalResourceDescription(
				TestExternalResource.class,
				TestExternalResource.PARAM_VALUE, TestExternalResource.EXPECTED_VALUE);
		
		AnalysisEngineDescription aeDesc1 = saveLoad(createPrimitiveDescription(
				TestAnalysisEngineWithResource.class,
				TestAnalysisEngineWithResource.PARAM_RESOURCE, resDesc));

		AnalysisEngineDescription aeDesc2 = saveLoad(createPrimitiveDescription(
				TestAnalysisEngineWithResource.class,
				TestAnalysisEngineWithResource.PARAM_RESOURCE, resDesc));

		dumpResourceConfiguration(aeDesc1);
		dumpResourceConfiguration(aeDesc2);
		
		AnalysisEngine ae1 = createPrimitive(aeDesc1);
		AnalysisEngine ae2 = createPrimitive(aeDesc2);
		ae1.process(ae1.newCAS());
		ae2.process(ae2.newCAS());
		
		AnalysisEngine ae3 = createAggregate(createAggregateDescription(aeDesc1, aeDesc2));
		ae3.process(ae3.newCAS());
	}

	/**
	 * Test simple nesting.
	 */
	@Test
	public void resource_testSimpleNesting() throws Exception {
		AnalysisEngineDescription aeDesc = saveLoad(createPrimitiveDescription(
				TestAnalysisEngineWithResource.class,
				TestAnalysisEngineWithResource.PARAM_RESOURCE,
				createExternalResourceDescription(TestExternalResource2.class,
						TestExternalResource.PARAM_VALUE, TestExternalResource.EXPECTED_VALUE,
						TestExternalResource2.PARAM_RESOURCE, createExternalResourceDescription(
								TestExternalResource.class,
								TestExternalResource.PARAM_VALUE, TestExternalResource.EXPECTED_VALUE))));

		AnalysisEngine ae = createPrimitive(aeDesc);
		ae.process(ae.newCAS());
	}

	/**
	 * Test simple nesting.
	 */
	@Test
	public void resource_testSharedSimpleNesting() throws Exception {
		ExternalResourceDescription resDesc = createExternalResourceDescription(
				TestExternalResource2.class,
				TestExternalResource.PARAM_VALUE, TestExternalResource.EXPECTED_VALUE,
				TestExternalResource2.PARAM_RESOURCE, createExternalResourceDescription(
						TestExternalResource.class,
						TestExternalResource.PARAM_VALUE, TestExternalResource.EXPECTED_VALUE));	
		
		AnalysisEngineDescription aeDesc1 = saveLoad(createPrimitiveDescription(
				TestAnalysisEngineWithResource.class,
				TestAnalysisEngineWithResource.PARAM_RESOURCE, resDesc));

		dumpResourceConfiguration(aeDesc1);
		
		AnalysisEngineDescription aeDesc2 = saveLoad(createPrimitiveDescription(
				TestAnalysisEngineWithResource.class,
				TestAnalysisEngineWithResource.PARAM_RESOURCE, resDesc));

		dumpResourceConfiguration(aeDesc1);
		dumpResourceConfiguration(aeDesc2);
		
		AnalysisEngine ae1 = createPrimitive(aeDesc1);
		AnalysisEngine ae2 = createPrimitive(aeDesc2);
		ae1.process(ae1.newCAS());
		ae2.process(ae2.newCAS());
		
		AnalysisEngine ae3 = createAggregate(createAggregateDescription(aeDesc1, aeDesc2));
		ae3.process(ae3.newCAS());
	}

	/**
	 * Test deeper nesting level.
	 */
	@Test
	public void resource_testDeeperNesting() throws Exception {
		ExternalResourceDescription resDesc2 = createExternalResourceDescription(
				TestExternalResource.class,
				TestExternalResource.PARAM_VALUE, TestExternalResource.EXPECTED_VALUE);

		ExternalResourceDescription resDesc = createExternalResourceDescription(
				TestExternalResource2.class,
				TestExternalResource2.PARAM_RESOURCE, resDesc2,
				TestExternalResource.PARAM_VALUE, TestExternalResource.EXPECTED_VALUE);

		AnalysisEngineDescription aeDesc = saveLoad(createPrimitiveDescription(
				TestAnalysisEngineWithResource.class,
				TestAnalysisEngineWithResource.PARAM_RESOURCE,
				createExternalResourceDescription(TestExternalResource2.class,
						TestExternalResource.PARAM_VALUE, TestExternalResource.EXPECTED_VALUE,
						TestExternalResource2.PARAM_RESOURCE, resDesc)));

		dumpResourceConfiguration(aeDesc);

		AnalysisEngine ae = createPrimitive(aeDesc);
		ae.process(ae.newCAS());
	}

	/**
	 * Test self-injection
	 */
	@Test
	public void resource_testSelfInjection() throws Exception {
		ExternalResourceDescription resDesc = createExternalResourceDescription(
				TestExternalResource2.class,
				TestExternalResource.PARAM_VALUE, TestExternalResource.EXPECTED_VALUE);
		bindExternalResource(resDesc, TestExternalResource2.PARAM_RESOURCE, resDesc);

		AnalysisEngineDescription aeDesc = saveLoad(createPrimitiveDescription(
				TestAnalysisEngineWithResource.class,
				TestAnalysisEngineWithResource.PARAM_RESOURCE, resDesc));
		
		dumpResourceConfiguration(aeDesc);
		
		AnalysisEngine ae = createPrimitive(aeDesc);
		ae.process(ae.newCAS());
	}

	/**
	 * Test self-injection
	 */
	@Test
	public void resource_testDoubleSelfInjection() throws Exception {
		ExternalResourceDescription resDesc = createExternalResourceDescription(
				TestExternalResource2.class,
				TestExternalResource.PARAM_VALUE, TestExternalResource.EXPECTED_VALUE);
		bindExternalResource(resDesc, TestExternalResource2.PARAM_RESOURCE, resDesc);

		AnalysisEngineDescription aeDesc1 = saveLoad(createPrimitiveDescription(
				TestAnalysisEngineWithResource.class,
				TestAnalysisEngineWithResource.PARAM_RESOURCE, resDesc));

		AnalysisEngineDescription aeDesc2 = saveLoad(createPrimitiveDescription(
				TestAnalysisEngineWithResource.class,
				TestAnalysisEngineWithResource.PARAM_RESOURCE, resDesc));

		dumpResourceConfiguration(aeDesc1);
		dumpResourceConfiguration(aeDesc2);
		
		AnalysisEngine ae1 = createPrimitive(aeDesc1);
		AnalysisEngine ae2 = createPrimitive(aeDesc2);
		ae1.process(ae1.newCAS());
		ae2.process(ae2.newCAS());
		
		AnalysisEngine ae3 = createAggregate(createAggregateDescription(aeDesc1, aeDesc2));
		ae3.process(ae3.newCAS());
	}
	
	/**
	 * Test simple injection.
	 */
	@Test
	public void sharedObject_testInjection() throws Exception {
		AnalysisEngineDescription aeDesc = saveLoad(createPrimitiveDescription(
				TestAnalysisEngineWithSharedResourceObject.class,
				TestAnalysisEngineWithSharedResourceObject.PARAM_RESOURCE,
				createExternalResourceDescription(TestSharedResourceObject.class, "http://dumm.my",
						TestSharedResourceObject.PARAM_VALUE, TestSharedResourceObject.EXPECTED_VALUE)));
		
		AnalysisEngine ae = createPrimitive(aeDesc);
		ae.process(ae.newCAS());
	}

	/**
	 * Test shared simple injection.
	 */
	@Test
	public void sharedObject_testSharedInjection() throws Exception {
		ExternalResourceDescription resDesc = createExternalResourceDescription(
				TestSharedResourceObject.class, "http://dumm.my",
				TestSharedResourceObject.PARAM_VALUE, TestSharedResourceObject.EXPECTED_VALUE);
		
		AnalysisEngineDescription aeDesc1 = saveLoad(createPrimitiveDescription(
				TestAnalysisEngineWithSharedResourceObject.class,
				TestAnalysisEngineWithSharedResourceObject.PARAM_RESOURCE, resDesc));

		AnalysisEngineDescription aeDesc2 = saveLoad(createPrimitiveDescription(
				TestAnalysisEngineWithSharedResourceObject.class,
				TestAnalysisEngineWithSharedResourceObject.PARAM_RESOURCE, resDesc));

		dumpResourceConfiguration(aeDesc1);
		dumpResourceConfiguration(aeDesc2);
		
		AnalysisEngine ae1 = createPrimitive(aeDesc1);
		AnalysisEngine ae2 = createPrimitive(aeDesc2);
		ae1.process(ae1.newCAS());
		ae2.process(ae2.newCAS());
		
		AnalysisEngine ae3 = createAggregate(createAggregateDescription(aeDesc1, aeDesc2));
		ae3.process(ae3.newCAS());
	}

	/**
	 * Test self-injection
	 */
	@Test
	public void sharedObject_testSelfInjection() throws Exception {
		ExternalResourceDescription resDesc = createExternalResourceDescription(
				TestSharedResourceObject2.class, "http://dumm.my",
				TestSharedResourceObject.PARAM_VALUE, TestSharedResourceObject.EXPECTED_VALUE);
		bindExternalResource(resDesc, TestSharedResourceObject2.PARAM_RESOURCE, resDesc);

		AnalysisEngineDescription aeDesc = saveLoad(createPrimitiveDescription(
				TestAnalysisEngineWithSharedResourceObject.class,
				TestAnalysisEngineWithSharedResourceObject.PARAM_RESOURCE, resDesc));
		
		dumpResourceConfiguration(aeDesc);
		
		AnalysisEngine ae = createPrimitive(aeDesc);
		ae.process(ae.newCAS());
	}

	public static class TestExternalResource2 extends TestExternalResource {
		public final static String PARAM_RESOURCE = "resource2";
		@ExternalResource(key = PARAM_RESOURCE)
		private TestExternalResource resource;

		@Override
		public void afterResourcesInitialized() {
			System.out.println(getClass().getSimpleName()+".afterResourcesInitialized()");
			// Ensure the External Resource is bound
			assertNotNull(resource);
			if (this != resource) {
				resource.assertConfiguredOk();
			}
			assertConfiguredOk();
		}
	}

	public static class TestSharedResourceObject2 extends TestSharedResourceObject implements
			ExternalResourceAware {
		public final static String PARAM_RESOURCE = "resource2";
		@ExternalResource(key = PARAM_RESOURCE)
		private TestSharedResourceObject resource;

		@ConfigurationParameter(name=ExternalResourceFactory.PARAM_RESOURCE_NAME)
		private String resourceName;

		public void afterResourcesInitialized() {
			System.out.println(getClass().getSimpleName()+".afterResourcesInitialized()");
			// Ensure the External Resource is bound
			assertNotNull(resource);
			if (this != resource) {
				resource.assertConfiguredOk();
			}
			assertConfiguredOk();
		}

		public String getResourceName() {
			return resourceName;
		}
	}

	public static class TestAnalysisEngineWithResource extends CasAnnotator_ImplBase {

		public final static String PARAM_RESOURCE = "resource";
		@ExternalResource(key = PARAM_RESOURCE)
		private TestExternalResource resource;
		
		@Override
		public void process(CAS aCAS) throws AnalysisEngineProcessException {
			System.out.println(getClass().getSimpleName()+".process()");
			assertNotNull(resource);
			resource.assertConfiguredOk();
		}
	}

	public static class TestAnalysisEngineWithSharedResourceObject extends CasAnnotator_ImplBase {

		public final static String PARAM_RESOURCE = "resource";
		@ExternalResource(key = PARAM_RESOURCE)
		private TestSharedResourceObject resource;
		
		@Override
		public void process(CAS aCAS) throws AnalysisEngineProcessException {
			System.out.println(getClass().getSimpleName()+".process()");
			assertNotNull(resource);
			resource.assertConfiguredOk();
		}
	}

	AnalysisEngineDescription saveLoad(AnalysisEngineDescription aDesc)
			throws InvalidXMLException, SAXException, IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		aDesc.toXML(bos);
		return UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
				new XMLInputSource(new ByteArrayInputStream(bos.toByteArray()), null));
	}
	
	private void dumpResourceConfiguration(ResourceCreationSpecifier aSpec)
	{
		System.out.println("-- begin resource configuration");
		for (ExternalResourceBinding b : aSpec.getResourceManagerConfiguration()
				.getExternalResourceBindings()) {
			System.out.printf("Binding : %s -> %s %n", b.getKey(), b.getResourceName());
		}

		for (ExternalResourceDescription r : aSpec.getResourceManagerConfiguration()
				.getExternalResources()) {
			if (r.getImplementationName() != null) {
				System.out.printf("Resource: %s -> %s %n", r.getName(), r.getImplementationName());
			}
			else {
				System.out
						.printf("Resource: %s -> %s %n", r.getName(), ((CustomResourceSpecifier) r
								.getResourceSpecifier()).getResourceClassName());
			}
		}
		System.out.println("-- end resource configuration");
	}

	@Rule
	public TestName name = new TestName();

	@Before
	public void printSeparator()
	{
		System.out.println("\n=== " + name.getMethodName() + " =====================");
	}
}
