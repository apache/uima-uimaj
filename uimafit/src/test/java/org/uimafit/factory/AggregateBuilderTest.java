/* 
  Copyright 2010 Regents of the University of Colorado.  
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

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.FlowControllerFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.TypeSystemUtil;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;
import org.uimafit.component.NoOpAnnotator;
import org.uimafit.factory.testAes.Annotator1;
import org.uimafit.factory.testAes.Annotator2;
import org.uimafit.factory.testAes.Annotator3;
import org.uimafit.factory.testAes.FlowAE1;
import org.uimafit.factory.testAes.FlowAE2;
import org.uimafit.factory.testAes.FlowAE3;
import org.uimafit.factory.testAes.ReversableTestFlowController;
import org.uimafit.factory.testAes.ViewNames;

/**
 * @author Philip Ogren
 */
public class AggregateBuilderTest extends ComponentTestBase {

	@Test
	public void testAggregateBuilder() throws UIMAException, IOException {
		tokenBuilder.buildTokens(jCas, "Anyone up for a game of Foosball?");

		AggregateBuilder builder = new AggregateBuilder();
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(Annotator1.class,
				typeSystemDescription), ViewNames.PARENTHESES_VIEW, "A");
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(Annotator2.class,
				typeSystemDescription), ViewNames.SORTED_VIEW, "B",
				ViewNames.SORTED_PARENTHESES_VIEW, "C", ViewNames.PARENTHESES_VIEW, "A");
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(Annotator3.class,
				typeSystemDescription), ViewNames.INITIAL_VIEW, "B");
		AnalysisEngine aggregateEngine = builder.createAggregate();

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

		AnalysisEngineDescription aggregateDescription = builder.createAggregateDescription();
		builder = new AggregateBuilder();
		builder.add(aggregateDescription);
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(Annotator1.class,
				typeSystemDescription), ViewNames.PARENTHESES_VIEW, "PARENS");
		aggregateEngine = builder.createAggregate();

		jCas.reset();

		tokenBuilder.buildTokens(jCas, "Anyone up for a game of Foosball?");

		aggregateEngine.process(jCas);

		assertEquals("Anyone up for a game of Foosball?", jCas.getDocumentText());
		assertEquals("Any(o)n(e) (u)p f(o)r (a) g(a)m(e) (o)f F(oo)sb(a)ll?", jCas.getView("A")
				.getDocumentText());
		assertEquals("?AFaaabeeffgllmnnoooooprsuy", jCas.getView("B").getDocumentText());
		assertEquals("(((((((((())))))))))?AFaaabeeffgllmnnoooooprsuy", jCas.getView("C")
				.getDocumentText());
		assertEquals("yusrpooooonnmllgffeebaaaFA?", jCas.getView(ViewNames.REVERSE_VIEW)
				.getDocumentText());
		assertEquals("Any(o)n(e) (u)p f(o)r (a) g(a)m(e) (o)f F(oo)sb(a)ll?", jCas
				.getView("PARENS").getDocumentText());

	}

	@Test
	public void testAggregateBuilder2() throws UIMAException {
		tokenBuilder.buildTokens(jCas, "'Verb' is a noun!?");

		AggregateBuilder builder = new AggregateBuilder();
		String componentName1 = builder.add(AnalysisEngineFactory.createPrimitiveDescription(
				Annotator1.class, typeSystemDescription));
		String componentName2 = builder.add(AnalysisEngineFactory.createPrimitiveDescription(
				Annotator1.class, typeSystemDescription));
		String componentName3 = builder.add(AnalysisEngineFactory.createPrimitiveDescription(
				Annotator1.class, typeSystemDescription));

		assertEquals("org.uimafit.factory.testAes.Annotator1", componentName1);
		assertEquals("org.uimafit.factory.testAes.Annotator1.2", componentName2);
		assertEquals("org.uimafit.factory.testAes.Annotator1.3", componentName3);

		builder.addSofaMapping(componentName1, ViewNames.PARENTHESES_VIEW, "A");
		builder.addSofaMapping(componentName2, ViewNames.PARENTHESES_VIEW, "B");
		builder.addSofaMapping(componentName3, ViewNames.PARENTHESES_VIEW, "C");
		AnalysisEngineDescription aggregateEngineDescription = builder.createAggregateDescription();

		AnalysisEngine aggregateEngine = AnalysisEngineFactory
				.createAggregate(aggregateEngineDescription);

		aggregateEngine.process(jCas);

		assertEquals("'Verb' is a noun!?", jCas.getDocumentText());
		assertEquals("'V(e)rb' (i)s (a) n(ou)n!?", jCas.getView("A").getDocumentText());
		assertEquals("'V(e)rb' (i)s (a) n(ou)n!?", jCas.getView("B").getDocumentText());
		assertEquals("'V(e)rb' (i)s (a) n(ou)n!?", jCas.getView("C").getDocumentText());

	}

	@Test(expected = IllegalArgumentException.class)
	public void testOddNumberOfViewNames() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(Annotator1.class,
				typeSystemDescription), ViewNames.PARENTHESES_VIEW);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDuplicateComponentNames() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();
		builder.add("name", AnalysisEngineFactory.createPrimitiveDescription(Annotator1.class,
				typeSystemDescription));
		builder.add("name", AnalysisEngineFactory.createPrimitiveDescription(Annotator1.class,
				typeSystemDescription));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadSofaMapping() {
		AggregateBuilder builder = new AggregateBuilder();
		builder.addSofaMapping("name", ViewNames.PARENTHESES_VIEW, "A");
	}

	@Test
	public void testAggregateBuilderWithFlowController() throws UIMAException {
		tokenBuilder.buildTokens(jCas, "An honest man can never surrender an honest doubt.");

		AggregateBuilder builder = new AggregateBuilder();
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(FlowAE1.class,
				typeSystemDescription));
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(FlowAE2.class,
				typeSystemDescription));
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(FlowAE3.class,
				typeSystemDescription));

		FlowControllerDescription fcd = FlowControllerFactory
				.createFlowControllerDescription(ReversableTestFlowController.class);
		builder.setFlowControllerDescription(fcd);
		AnalysisEngine aggregateEngine = builder.createAggregate();
		aggregateEngine.process(jCas);

		String text = "An honest man can never surrender an honest doubt.";
		text = text.replaceAll("[aeiou]+", "($0)"); // this is what FlowAE1 does
		text = FlowAE2.sort(text);
		text = FlowAE3.reverse(text);

		assertEquals(text, TypeSystemUtil.getAnalyzedText(jCas));
		assertEquals("vuutttsssrrrrooonnnnnnnnmhheeeeeeddcbaaaA.)))))))))))))(((((((((((((",
				TypeSystemUtil.getAnalyzedText(jCas));

		fcd = FlowControllerFactory.createFlowControllerDescription(
				ReversableTestFlowController.class,
				ReversableTestFlowController.PARAM_REVERSE_ORDER, true);
		builder.setFlowControllerDescription(fcd);
		aggregateEngine = builder.createAggregate();

		jCas.reset();
		tokenBuilder.buildTokens(jCas, "An honest man can never surrender an honest doubt.");
		aggregateEngine.process(jCas);

		text = "An honest man can never surrender an honest doubt.";
		text = FlowAE3.reverse(text);
		text = FlowAE2.sort(text);
		text = text.replaceAll("[aeiou]+", "($0)"); // this is what FlowAE1 does

		assertEquals(text, TypeSystemUtil.getAnalyzedText(jCas));
		assertEquals(".A(aaa)bcdd(eeeeee)hhmnnnnnnnn(ooo)rrrrsssttt(uu)v",
				TypeSystemUtil.getAnalyzedText(jCas));

	}

}
