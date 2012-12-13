/*
 Copyright 2009-2010	Regents of the University of Colorado.
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.metadata.SofaMapping;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.SofaMappingFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.junit.Test;
import org.uimafit.component.NoOpAnnotator;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.factory.testAes.Annotator1;
import org.uimafit.factory.testAes.Annotator2;
import org.uimafit.factory.testAes.Annotator3;
import org.uimafit.factory.testAes.Annotator4;
import org.uimafit.factory.testAes.ParameterizedAE;
import org.uimafit.factory.testAes.ViewNames;
import org.uimafit.type.Sentence;
import org.uimafit.type.Token;

/**
 * @author Steven Bethard, Philip Ogren
 */

public class AnalysisEngineFactoryTest extends ComponentTestBase {

	@Test
	public void testViewAE() throws Exception {
		AnalysisEngineDescription aed = AnalysisEngineFactory.createPrimitiveDescription(
				Annotator4.class, typeSystemDescription);
		AnalysisEngine ae = AnalysisEngineFactory.createAnalysisEngine(aed, "A");

		JCas aView = jCas.createView("A");
		tokenBuilder.buildTokens(aView, "'Verb' is a noun!?");
		ae.process(jCas);
		assertEquals("'Verb' is a noun!?", jCas.getView("A").getDocumentText());
		assertEquals("NN", JCasUtil.selectByIndex(aView, Token.class, 0).getPos());
	}

	@Test
	public void testCreateAnalysisEngineFromPath() throws UIMAException, IOException {
		AnalysisEngine engine = AnalysisEngineFactory
				.createAnalysisEngineFromPath("src/main/resources/org/uimafit/component/NoOpAnnotator.xml");
		assertNotNull(engine);
	}

	@Test
	public void testProcess1() throws UIMAException, IOException {
		jCas = AnalysisEngineFactory.process(NoOpAnnotator.class.getName(), "There is no excuse!");

		assertEquals("There is no excuse!", jCas.getDocumentText());
	}

	@Test
	public void testProcess2() throws UIMAException, IOException {
		jCas = AnalysisEngineFactory.process(NoOpAnnotator.class.getName(),
				"src/test/resources/data/docs/A.txt");

		assertEquals("Aaa Bbbb Cc Dddd eeee ff .", jCas.getDocumentText());
	}

	@Test
	public void testCreateAnalysisEngineWithPrioritizedTypes() throws UIMAException {
		String[] prioritizedTypeNames = new String[] { "org.uimafit.type.Token",
				"org.uimafit.type.Sentence" };
		AnalysisEngine engine = AnalysisEngineFactory.createPrimitive(
				org.uimafit.component.NoOpAnnotator.class, typeSystemDescription,
				prioritizedTypeNames, (Object[]) null);

		typePriorities = engine.getAnalysisEngineMetaData().getTypePriorities();
		assertEquals(1, typePriorities.getPriorityLists().length);
		TypePriorityList typePriorityList = typePriorities.getPriorityLists()[0];
		assertEquals(2, typePriorityList.getTypes().length);
		assertEquals("org.uimafit.type.Token", typePriorityList.getTypes()[0]);
		assertEquals("org.uimafit.type.Sentence", typePriorityList.getTypes()[1]);

		jCas = engine.newJCas();
		tokenBuilder.buildTokens(jCas, "word");
		FSIterator<Annotation> tokensInSentence = jCas.getAnnotationIndex().subiterator(
				JCasUtil.selectByIndex(jCas, Sentence.class, 0));
		assertFalse(tokensInSentence.hasNext());

		prioritizedTypeNames = new String[] { "org.uimafit.type.Sentence", "org.uimafit.type.Token" };
		engine = AnalysisEngineFactory.createPrimitive(
				org.uimafit.component.NoOpAnnotator.class, typeSystemDescription,
				prioritizedTypeNames, (Object[]) null);
		jCas = engine.newJCas();
		tokenBuilder.buildTokens(jCas, "word");
		tokensInSentence = jCas.getAnnotationIndex().subiterator(
				JCasUtil.selectByIndex(jCas, Sentence.class, 0));
		assertTrue(tokensInSentence.hasNext());

	}

	@Test
	public void testAggregate() throws UIMAException {
		tokenBuilder.buildTokens(jCas, "Anyone up for a game of Foosball?");

		SofaMapping[] sofaMappings = new SofaMapping[] {
				SofaMappingFactory.createSofaMapping(Annotator1.class, ViewNames.PARENTHESES_VIEW,
						"A"),
				SofaMappingFactory.createSofaMapping(Annotator2.class, ViewNames.SORTED_VIEW, "B"),
				SofaMappingFactory.createSofaMapping(Annotator2.class,
						ViewNames.SORTED_PARENTHESES_VIEW, "C"),
				SofaMappingFactory.createSofaMapping(Annotator2.class, ViewNames.PARENTHESES_VIEW,
						"A"),
				SofaMappingFactory.createSofaMapping(Annotator3.class, ViewNames.INITIAL_VIEW, "B") };

		List<Class<? extends AnalysisComponent>> primitiveAEClasses = new ArrayList<Class<? extends AnalysisComponent>>();
		primitiveAEClasses.add(Annotator1.class);
		primitiveAEClasses.add(Annotator2.class);
		primitiveAEClasses.add(Annotator3.class);

		AnalysisEngine aggregateEngine = AnalysisEngineFactory.createAggregate(primitiveAEClasses,
				typeSystemDescription, null, sofaMappings);

		aggregateEngine.process(jCas);

		assertEquals("Anyone up for a game of Foosball?", jCas.getDocumentText());
		assertEquals("Any(o)n(e) (u)p f(o)r (a) g(a)m(e) (o)f F(oo)sb(a)ll?", jCas.getView("A")
				.getDocumentText());
		assertEquals("?AFaaabeeffgllmnnoooooprsuy", jCas.getView("B").getDocumentText());
		assertEquals("(((((((((())))))))))?AFaaabeeffgllmnnoooooprsuy", jCas.getView("C")
				.getDocumentText());
		assertEquals("yusrpooooonnmllgffeebaaaFA?", jCas.getView(ViewNames.REVERSE_VIEW)
				.getDocumentText());

	}

	@Test
	public void testAggregate2() throws UIMAException, IOException {
		tokenBuilder.buildTokens(jCas, "Anyone up for a game of Foosball?");

		SofaMapping[] sofaMappings = new SofaMapping[] {
				SofaMappingFactory.createSofaMapping("ann1", ViewNames.PARENTHESES_VIEW, "A"),
				SofaMappingFactory.createSofaMapping("ann2", ViewNames.SORTED_VIEW, "B"),
				SofaMappingFactory
						.createSofaMapping("ann2", ViewNames.SORTED_PARENTHESES_VIEW, "C"),
				SofaMappingFactory.createSofaMapping("ann2", ViewNames.PARENTHESES_VIEW, "A"),
				SofaMappingFactory.createSofaMapping("ann3", ViewNames.INITIAL_VIEW, "B") };

		List<AnalysisEngineDescription> primitiveDescriptors = new ArrayList<AnalysisEngineDescription>();
		primitiveDescriptors.add(AnalysisEngineFactory.createPrimitiveDescription(Annotator1.class,
				typeSystemDescription, (TypePriorities) null));
		primitiveDescriptors.add(AnalysisEngineFactory.createPrimitiveDescription(Annotator2.class,
				typeSystemDescription, (TypePriorities) null));
		primitiveDescriptors.add(AnalysisEngineFactory.createPrimitiveDescription(Annotator3.class,
				typeSystemDescription, (TypePriorities) null));

		List<String> componentNames = Arrays.asList("ann1", "ann2", "ann3");

		AnalysisEngine aggregateEngine = AnalysisEngineFactory.createAggregate(
				primitiveDescriptors, componentNames, typeSystemDescription, null, sofaMappings);

		aggregateEngine.process(jCas);

		assertEquals("Anyone up for a game of Foosball?", jCas.getDocumentText());
		assertEquals("Any(o)n(e) (u)p f(o)r (a) g(a)m(e) (o)f F(oo)sb(a)ll?", jCas.getView("A")
				.getDocumentText());
		assertEquals("?AFaaabeeffgllmnnoooooprsuy", jCas.getView("B").getDocumentText());
		assertEquals("(((((((((())))))))))?AFaaabeeffgllmnnoooooprsuy", jCas.getView("C")
				.getDocumentText());
		assertEquals("yusrpooooonnmllgffeebaaaFA?", jCas.getView(ViewNames.REVERSE_VIEW)
				.getDocumentText());

		JCasFactory.loadJCas(jCas, "src/test/resources/data/docs/test.xmi");
		AnalysisEngine ae1 = AnalysisEngineFactory.createPrimitive(NoOpAnnotator.class,
				typeSystemDescription);

		SimplePipeline.runPipeline(jCas, ae1, aggregateEngine);

	}

	@Test
	public void testReflectPrimitiveDescription() throws ResourceInitializationException {
		AnalysisEngineDescription aed = AnalysisEngineFactory.createPrimitiveDescription(
				Annotator2.class, typeSystemDescription, typePriorities);
		Capability[] capabilities = aed.getAnalysisEngineMetaData().getCapabilities();
		assertEquals(1, capabilities.length);
		String[] inputSofas = capabilities[0].getInputSofas();
		assertArrayEquals(new String[] { CAS.NAME_DEFAULT_SOFA, ViewNames.PARENTHESES_VIEW },
				inputSofas);
		String[] outputSofas = capabilities[0].getOutputSofas();
		assertArrayEquals(
				new String[] { ViewNames.SORTED_VIEW, ViewNames.SORTED_PARENTHESES_VIEW },
				outputSofas);

		aed = AnalysisEngineFactory.createPrimitiveDescription(ParameterizedAE.class,
				typeSystemDescription, typePriorities);
		capabilities = aed.getAnalysisEngineMetaData().getCapabilities();
		assertEquals(1, capabilities.length);
		inputSofas = capabilities[0].getInputSofas();
		assertArrayEquals(new String[] { CAS.NAME_DEFAULT_SOFA }, inputSofas);
		outputSofas = capabilities[0].getOutputSofas();
		assertArrayEquals(new String[] {}, outputSofas);

		testConfigurationParameter(aed, ParameterizedAE.PARAM_STRING_1,
				ConfigurationParameter.TYPE_STRING, true, false, "pineapple");
		testConfigurationParameter(aed, ParameterizedAE.PARAM_STRING_2,
				ConfigurationParameter.TYPE_STRING, false, true,
				new String[] { "coconut", "mango" });
		testConfigurationParameter(aed, ParameterizedAE.PARAM_STRING_3,
				ConfigurationParameter.TYPE_STRING, false, false, null);
		testConfigurationParameter(aed, ParameterizedAE.PARAM_STRING_4,
				ConfigurationParameter.TYPE_STRING, true, true, new String[] { "apple" });
		testConfigurationParameter(aed, ParameterizedAE.PARAM_STRING_5,
				ConfigurationParameter.TYPE_STRING, false, true, new String[] { "" });

		testConfigurationParameter(aed, ParameterizedAE.PARAM_BOOLEAN_1,
				ConfigurationParameter.TYPE_BOOLEAN, true, false, Boolean.FALSE);
		testConfigurationParameter(aed, ParameterizedAE.PARAM_BOOLEAN_2,
				ConfigurationParameter.TYPE_BOOLEAN, false, false, null);
		testConfigurationParameter(aed, ParameterizedAE.PARAM_BOOLEAN_3,
				ConfigurationParameter.TYPE_BOOLEAN, true, true,
				new Boolean[] { true, true, false });
		testConfigurationParameter(aed, ParameterizedAE.PARAM_BOOLEAN_4,
				ConfigurationParameter.TYPE_BOOLEAN, true, true,
				new Boolean[] { true, false, true });
		testConfigurationParameter(aed, ParameterizedAE.PARAM_BOOLEAN_5,
				ConfigurationParameter.TYPE_BOOLEAN, true, true, new Boolean[] { false });

		testConfigurationParameter(aed, ParameterizedAE.PARAM_INT_1,
				ConfigurationParameter.TYPE_INTEGER, true, false, 0);
		testConfigurationParameter(aed, ParameterizedAE.PARAM_INT_2,
				ConfigurationParameter.TYPE_INTEGER, false, false, 42);
		testConfigurationParameter(aed, ParameterizedAE.PARAM_INT_3,
				ConfigurationParameter.TYPE_INTEGER, false, true, new Integer[] { 42, 111 });
		testConfigurationParameter(aed, ParameterizedAE.PARAM_INT_4,
				ConfigurationParameter.TYPE_INTEGER, true, true, new Integer[] { 2 });

		testConfigurationParameter(aed, ParameterizedAE.PARAM_FLOAT_1,
				ConfigurationParameter.TYPE_FLOAT, true, false, 0.0f);
		testConfigurationParameter(aed, ParameterizedAE.PARAM_FLOAT_2,
				ConfigurationParameter.TYPE_FLOAT, false, false, 3.1415f);
		testConfigurationParameter(aed, ParameterizedAE.PARAM_FLOAT_3,
				ConfigurationParameter.TYPE_FLOAT, true, false, null);
		testConfigurationParameter(aed, ParameterizedAE.PARAM_FLOAT_4,
				ConfigurationParameter.TYPE_FLOAT, false, true, null);
		testConfigurationParameter(aed, ParameterizedAE.PARAM_FLOAT_5,
				ConfigurationParameter.TYPE_FLOAT, false, true, new Float[] { 0.0f, 3.1415f,
						2.7182818f });
		testConfigurationParameter(aed, ParameterizedAE.PARAM_FLOAT_6,
				ConfigurationParameter.TYPE_FLOAT, true, true, null);
		testConfigurationParameter(aed, ParameterizedAE.PARAM_FLOAT_7,
				ConfigurationParameter.TYPE_FLOAT, true, true, new Float[] { 1.1111f, 2.2222f,
						3.333f });

		AnalysisEngine ae = AnalysisEngineFactory.createPrimitive(aed,
				ParameterizedAE.PARAM_FLOAT_3, 3.1415f, ParameterizedAE.PARAM_FLOAT_6,
				new Float[] { 2.71828183f }, "file2", "foo/bar");
		Object paramValue = ae.getAnalysisEngineMetaData().getConfigurationParameterSettings()
				.getParameterValue(ParameterizedAE.PARAM_FLOAT_3);
		assertEquals(paramValue, 3.1415f);
		paramValue = ae.getAnalysisEngineMetaData().getConfigurationParameterSettings()
				.getParameterValue(ParameterizedAE.PARAM_FLOAT_6);
		assertEquals(((Float[]) paramValue)[0].floatValue(), 2.71828183f, 0.00001f);

	}

	private void testConfigurationParameter(AnalysisEngineDescription aed, String parameterName,
			String parameterType, boolean mandatory, boolean multiValued, Object parameterValue) {
		ConfigurationParameterDeclarations cpd = aed.getMetaData()
				.getConfigurationParameterDeclarations();
		ConfigurationParameter cp = cpd.getConfigurationParameter(null, parameterName);
		assertNotNull("Parameter [" + parameterName + "] does not exist!", cp);
		assertEquals("Parameter [" + parameterName + "] has wrong name", parameterName,
				cp.getName());
		assertEquals("Parameter [" + parameterName + "] has wrong type", parameterType,
				cp.getType());
		assertEquals("Parameter [" + parameterName + "] has wrong mandatory flag", mandatory,
				cp.isMandatory());
		assertEquals("Parameter [" + parameterName + "] has wrong multi-value flag", multiValued,
				cp.isMultiValued());
		ConfigurationParameterSettings cps = aed.getMetaData().getConfigurationParameterSettings();
		Object actualValue = cps.getParameterValue(parameterName);
		if (parameterValue == null) {
			assertNull(actualValue);
		}
		else if (!multiValued) {
			if (parameterType.equals(ConfigurationParameter.TYPE_FLOAT)) {
				assertEquals(((Float) parameterValue).floatValue(),
						((Float) actualValue).floatValue(), .001f);
			}
			else {
				assertEquals(parameterValue, actualValue);
			}
		}
		else {
			assertEquals(Array.getLength(parameterValue), Array.getLength(actualValue));
			for (int i = 0; i < Array.getLength(parameterValue); ++i) {
				assertEquals(Array.get(parameterValue, i), Array.get(actualValue, i));
			}
		}

	}

	@Test
	public void testPrimitiveDescription() throws ResourceInitializationException {

		AnalysisEngineDescription aed = AnalysisEngineFactory.createPrimitiveDescription(
				NoOpAnnotator.class, typeSystemDescription);
		assertNotNull(aed);
		// assertEquals("org.uimafit.type.TypeSystem",
		// aed.getAnalysisEngineMetaData().getTypeSystem().getImports()[0].getName());
	}

	/**
	 * Test that a {@link OperationalProperties} annotation on an ancestor of a analysis engine
	 * class is found and taken into account.
	 */
	@Test
	public void testComponentAnnotationOnAncestor() throws Exception {
		AnalysisEngineDescription desc1 = AnalysisEngineFactory.createPrimitiveDescription(
				PristineAnnotatorClass.class, (Object[]) null);
		assertTrue(
				"Multiple deployment should be allowed on "
						+ desc1.getAnnotatorImplementationName(), desc1.getAnalysisEngineMetaData()
						.getOperationalProperties().isMultipleDeploymentAllowed());

		AnalysisEngineDescription desc2 = AnalysisEngineFactory.createPrimitiveDescription(
				UnannotatedAnnotatorClass.class, (Object[]) null);
		assertFalse(
				"Multiple deployment should be prohibited on "
						+ desc2.getAnnotatorImplementationName(), desc2.getAnalysisEngineMetaData()
						.getOperationalProperties().isMultipleDeploymentAllowed());

		AnalysisEngineDescription desc3 = AnalysisEngineFactory.createPrimitiveDescription(
				AnnotatedAnnotatorClass.class, (Object[]) null);
		assertTrue(
				"Multiple deployment should be allowed  on "
						+ desc3.getAnnotatorImplementationName(), desc3.getAnalysisEngineMetaData()
						.getOperationalProperties().isMultipleDeploymentAllowed());
	}

	/*
	 * This test case illustrates that UIMA throws an exception unless the multipleDeploymentAllowed
	 * flag is properly set to false when mixing multi-deployment and non-multi-deployment AEs.
	 */
	@Test(expected = ResourceInitializationException.class)
	public void testAAEMultipleDeploymentPolicyProblem() throws Exception {
		{
			AnalysisEngineDescription desc1 = AnalysisEngineFactory.createPrimitiveDescription(
					PristineAnnotatorClass.class, (Object[]) null);
			assertTrue(
					"Multiple deployment should be allowed on "
							+ desc1.getAnnotatorImplementationName(), desc1
							.getAnalysisEngineMetaData().getOperationalProperties()
							.isMultipleDeploymentAllowed());

			AnalysisEngineDescription desc2 = AnalysisEngineFactory.createPrimitiveDescription(
					UnannotatedAnnotatorClass.class, (Object[]) null);
			assertFalse(
					"Multiple deployment should be prohibited on "
							+ desc2.getAnnotatorImplementationName(), desc2
							.getAnalysisEngineMetaData().getOperationalProperties()
							.isMultipleDeploymentAllowed());

			AnalysisEngineDescription aae = AnalysisEngineFactory.createAggregateDescription(desc1,
					desc2);
			aae.getAnalysisEngineMetaData().getOperationalProperties()
					.setMultipleDeploymentAllowed(true);
			UIMAFramework.produceAnalysisEngine(aae);
		}
	}

	@Test
	public void testAAEMultipleDeploymentPolicy() throws Exception {
		{
			AnalysisEngineDescription desc1 = AnalysisEngineFactory.createPrimitiveDescription(
					PristineAnnotatorClass.class, (Object[]) null);
			assertTrue(
					"Multiple deployment should be allowed on "
							+ desc1.getAnnotatorImplementationName(), desc1
							.getAnalysisEngineMetaData().getOperationalProperties()
							.isMultipleDeploymentAllowed());

			AnalysisEngineDescription desc2 = AnalysisEngineFactory.createPrimitiveDescription(
					UnannotatedAnnotatorClass.class, (Object[]) null);
			assertFalse(
					"Multiple deployment should be prohibited on "
							+ desc2.getAnnotatorImplementationName(), desc2
							.getAnalysisEngineMetaData().getOperationalProperties()
							.isMultipleDeploymentAllowed());

			AnalysisEngineDescription aae = AnalysisEngineFactory.createAggregateDescription(desc1,
					desc2);
			UIMAFramework.produceAnalysisEngine(aae);

			assertFalse("Multiple deployment should be prohibited on AAE", aae
					.getAnalysisEngineMetaData().getOperationalProperties()
					.isMultipleDeploymentAllowed());
		}

		{
			AnalysisEngineDescription desc1 = AnalysisEngineFactory.createPrimitiveDescription(
					PristineAnnotatorClass.class, (Object[]) null);
			assertTrue(
					"Multiple deployment should be allowed on "
							+ desc1.getAnnotatorImplementationName(), desc1
							.getAnalysisEngineMetaData().getOperationalProperties()
							.isMultipleDeploymentAllowed());

			AnalysisEngineDescription desc3 = AnalysisEngineFactory.createPrimitiveDescription(
					AnnotatedAnnotatorClass.class, (Object[]) null);
			assertTrue(
					"Multiple deployment should be allowed  on "
							+ desc3.getAnnotatorImplementationName(), desc3
							.getAnalysisEngineMetaData().getOperationalProperties()
							.isMultipleDeploymentAllowed());

			AnalysisEngineDescription aae = AnalysisEngineFactory.createAggregateDescription(desc1,
					desc3);
			UIMAFramework.produceAnalysisEngine(aae);

			assertTrue("Multiple deployment should be prohibited on AAE", aae
					.getAnalysisEngineMetaData().getOperationalProperties()
					.isMultipleDeploymentAllowed());
		}
	}

	public static class PristineAnnotatorClass extends JCasAnnotator_ImplBase {
		@Override
		public void process(JCas aJCas) throws AnalysisEngineProcessException {
			// Dummy
		}
	}

	@org.apache.uima.fit.descriptor.OperationalProperties(multipleDeploymentAllowed = false)
	public static class AncestorClass extends JCasAnnotator_ImplBase {
		@Override
		public void process(JCas aJCas) throws AnalysisEngineProcessException {
			// Dummy
		}
	}

	public static class UnannotatedAnnotatorClass extends AncestorClass {
		// Dummy
	}

	@org.apache.uima.fit.descriptor.OperationalProperties(multipleDeploymentAllowed = true)
	public static class AnnotatedAnnotatorClass extends UnannotatedAnnotatorClass {
		// Vessel for the annotation
	}

	@Test
	public void testIssue5a() throws ResourceInitializationException {
		AnalysisEngineFactory.createPrimitiveDescription(ParameterizedAE.class,
				typeSystemDescription);
	}

	@Test(expected = ResourceInitializationException.class)
	public void testIssue5b() throws ResourceInitializationException {
		AnalysisEngineFactory.createPrimitive(ParameterizedAE.class, typeSystemDescription);
	}

}
