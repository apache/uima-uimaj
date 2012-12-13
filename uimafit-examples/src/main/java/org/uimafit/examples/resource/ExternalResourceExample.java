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

package org.uimafit.examples.resource;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createAggregate;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createAggregateDescription;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.bindResource;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;
import org.junit.Test;
import org.uimafit.component.Resource_ImplBase;

/**
 * Example for the use of external resources with uimaFIT.
 *
 * @author Richard Eckart de Castilho
 */
public class ExternalResourceExample {
	/**
	 * Simple model that only stores the URI it was loaded from. Normally data would be loaded from
	 * the URI instead and made accessible through methods in this class. This simple example only
	 * allows to access the URI.
	 */
	public static final class SharedModel implements SharedResourceObject {
		private String uri;

		public void load(DataResource aData) throws ResourceInitializationException {
			uri = aData.getUri().toString();
		}

		public String getUri() {
			return uri;
		}
	}
	
	/**
	 * Example annotator that uses the share model object. In the process() we only test if the
	 * model was properly initialized by uimaFIT
	 */
	public static class Annotator extends org.uimafit.component.JCasAnnotator_ImplBase {
		final static String MODEL_KEY = "Model";
		@ExternalResource(key = MODEL_KEY)
		private SharedModel model;

		@Override
		public void process(JCas aJCas) throws AnalysisEngineProcessException {
			assertTrue(model.getUri().endsWith("somemodel.bin"));
			// Prints the instance ID to the console - this proves the same instance
			// of the SharedModel is used in both Annotator instances.
			System.out.println(getClass().getSimpleName() + ": " + model);
		}
	}

	/**
	 * JUnit test that illustrates how to configure the annotator with the shared model object
	 */
	@Test
	public void configureAnnotatorsIndividuallyExample() throws Exception {
		ExternalResourceDescription extDesc = createExternalResourceDescription(
				SharedModel.class, new File("somemodel.bin"));
		
		// Binding external resource to each Annotator individually
		AnalysisEngineDescription aed1 = createPrimitiveDescription(Annotator.class,
				Annotator.MODEL_KEY, extDesc);
		AnalysisEngineDescription aed2 = createPrimitiveDescription(Annotator.class,
				Annotator.MODEL_KEY, extDesc);

		// Check the external resource was injected
		AnalysisEngineDescription aaed = createAggregateDescription(aed1, aed2);
		AnalysisEngine ae = createAggregate(aaed);
		ae.process(ae.newJCas());
	}
	
	/**
	 * JUnit test that illustrates how to configure the Annotator with the SharedModel.
	 * You should avoid this approach unless you are absolutely sure you need this. For this
	 * approach to work it must be guaranteed that no two components in the aggregate use the
	 * same resource key for different resoures, e.g. one "model" for different kinds of models.
	 */
	@Test
	public void configureAggregatedExample() throws Exception {
		AnalysisEngineDescription aed1 = createPrimitiveDescription(Annotator.class);
		AnalysisEngineDescription aed2 = createPrimitiveDescription(Annotator.class);

		// Bind external resource to the aggregate
		AnalysisEngineDescription aaed = createAggregateDescription(aed1, aed2);
		bindResource(aaed, Annotator.MODEL_KEY, SharedModel.class, new File("somemodel.bin")
				.toURI().toURL().toString());

		// Check the external resource was injected
		AnalysisEngine ae = createAggregate(aaed);
		ae.process(ae.newJCas());
	}
	
	/**
	 * Simple example resource that can use another resource.
	 */
	public static class ChainableResource extends Resource_ImplBase {
		public final static String PARAM_CHAINED_RESOURCE = "chainedResource";
		@ExternalResource(key = PARAM_CHAINED_RESOURCE, mandatory=false)
		private ChainableResource chainedResource;

		@Override
		public void afterResourcesInitialized() {
			// init logic that requires external resources
			System.out.println(getClass().getSimpleName() + ": " + chainedResource);
		}
	}

	/**
	 * Example annotator that uses the resource. In the process() we only test if the
	 * model was properly initialized by uimaFIT
	 */
	public static class Annotator2 extends org.uimafit.component.JCasAnnotator_ImplBase {
		final static String MODEL_KEY = "Model";
		@ExternalResource(key = MODEL_KEY)
		private ChainableResource model;

		@Override
		public void process(JCas aJCas) throws AnalysisEngineProcessException {
			System.out.println(getClass().getSimpleName() + ": " + model);
		}
	}

	/**
	 * JUnit test that illustrates how to configure the annotator with a chainable resource
	 */
	@Test
	public void configureAnnotatorWithChainedResource() throws Exception {
		AnalysisEngineDescription aed = createPrimitiveDescription(Annotator2.class,
				Annotator2.MODEL_KEY, createExternalResourceDescription(
						ChainableResource.class,
						ChainableResource.PARAM_CHAINED_RESOURCE, createExternalResourceDescription(
								ChainableResource.class)));

		// Check the external resource was injected
		AnalysisEngine ae = createAggregate(aed);
		ae.process(ae.newJCas());
	}
}