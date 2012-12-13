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

package org.uimafit.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;
import org.uimafit.ComponentTestBase;
import org.uimafit.descriptor.SofaCapability;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.testing.util.HideOutput;
import org.uimafit.type.Token;
import org.uimafit.util.JCasUtil;

/**
 * I initially thought that the behavior of mapping the default view to another yet-to-be-created
 * view might be different for sofa aware and sofa unaware components. So the tests are run on using
 * an analysis engine of both kinds.
 * 
 * @author Philip Ogren
 * 
 */
public class ViewCreatorAnnotatorTest extends ComponentTestBase {

	@Test
	public void testViewCreatorAnnotator() throws ResourceInitializationException,
			AnalysisEngineProcessException, CASException {
		AnalysisEngine viewCreator = AnalysisEngineFactory.createPrimitive(
				ViewCreatorAnnotator.class, typeSystemDescription,
				ViewCreatorAnnotator.PARAM_VIEW_NAME, "myView");
		viewCreator.process(jCas);
		JCas myView = jCas.getView("myView");
		assertNotNull(myView);
		myView.setDocumentText("my view text");
	}

	/**
	 * This test basically demonstrates that the default view does not need to be initialized
	 * because it is done automatically.
	 */
	@Test
	public void testDefaultView() throws ResourceInitializationException,
			AnalysisEngineProcessException {
		AnalysisEngine engine = AnalysisEngineFactory.createPrimitive(SofaAwareAnnotator.class,
				typeSystemDescription);
		engine.process(jCas);
		assertEquals("some", JCasUtil.selectByIndex(jCas, Token.class, 0).getCoveredText());

		engine = AnalysisEngineFactory.createPrimitive(SofaUnawareAnnotator.class,
				typeSystemDescription);
		jCas.reset();
		engine.process(jCas);
		assertEquals("some", JCasUtil.selectByIndex(jCas, Token.class, 0).getCoveredText());
	}

	/**
	 * This test demonstrates the bad behavior that occurs when you try to map the default view to
	 * some other view without initializing that other view first. This is the behavior that
	 * SofaInitializerAnnotator addresses.
	 */
	@Test(expected = AnalysisEngineProcessException.class)
	public void testOtherViewAware() throws ResourceInitializationException,
			AnalysisEngineProcessException {
		AnalysisEngineDescription description = AnalysisEngineFactory.createPrimitiveDescription(
				SofaAwareAnnotator.class, typeSystemDescription);
		AnalysisEngine engine = AnalysisEngineFactory.createAnalysisEngine(description, "myView");
		HideOutput hider = new HideOutput();
		engine.process(jCas);
		hider.restoreOutput();
	}

	@Test(expected = AnalysisEngineProcessException.class)
	public void testOtherViewUnaware() throws ResourceInitializationException,
			AnalysisEngineProcessException {
		AnalysisEngineDescription description = AnalysisEngineFactory.createPrimitiveDescription(
				SofaUnawareAnnotator.class, typeSystemDescription);
		AnalysisEngine engine = AnalysisEngineFactory.createAnalysisEngine(description, "myView");
		engine.process(jCas);
	}

	/**
	 * This test demonstrates that running the viewCreator is doing the right thing (i.e.
	 * initializing the view "myView")
	 */
	@Test
	public void testSofaInitializer() throws ResourceInitializationException,
			AnalysisEngineProcessException, CASException {
		AnalysisEngineDescription description = AnalysisEngineFactory.createPrimitiveDescription(
				SofaAwareAnnotator.class, typeSystemDescription);
		AnalysisEngine engine = AnalysisEngineFactory.createAnalysisEngine(description, "myView");
		AnalysisEngine viewCreator = AnalysisEngineFactory.createPrimitive(
				ViewCreatorAnnotator.class, typeSystemDescription,
				ViewCreatorAnnotator.PARAM_VIEW_NAME, "myView");
		viewCreator.process(jCas);
		engine.process(jCas);
		assertEquals("some", JCasUtil.selectByIndex(jCas.getView("myView"), Token.class, 0)
				.getCoveredText());

		// here I run again with viewCreator running twice to make sure it
		// does the right thing when the view
		// has already been created
		jCas.reset();
		viewCreator.process(jCas);
		viewCreator.process(jCas);
		engine.process(jCas);
		assertEquals("some", JCasUtil.selectByIndex(jCas.getView("myView"), Token.class, 0)
				.getCoveredText());

		description = AnalysisEngineFactory.createPrimitiveDescription(SofaUnawareAnnotator.class,
				typeSystemDescription);
		engine = AnalysisEngineFactory.createAnalysisEngine(description, "myView");
		jCas.reset();
		viewCreator.process(jCas);
		engine.process(jCas);
		assertEquals("some", JCasUtil.selectByIndex(jCas.getView("myView"), Token.class, 0)
				.getCoveredText());

		jCas.reset();
		viewCreator.process(jCas);
		viewCreator.process(jCas);
		engine.process(jCas);
		assertEquals("some", JCasUtil.selectByIndex(jCas.getView("myView"), Token.class, 0)
				.getCoveredText());
	}

	@SofaCapability(inputSofas = CAS.NAME_DEFAULT_SOFA)
	public static class SofaAwareAnnotator extends JCasAnnotator_ImplBase {

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			JCas view;
			try {
				view = jCas.getView(CAS.NAME_DEFAULT_SOFA);
			}
			catch (CASException e) {
				throw new AnalysisEngineProcessException(e);
			}

			view.setDocumentText("some text");
			Token token = new Token(view, 0, 4);
			token.addToIndexes();
		}

	}

	public static class SofaUnawareAnnotator extends JCasAnnotator_ImplBase {

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			jCas.setDocumentText("some text");
			Token token = new Token(jCas, 0, 4);
			token.addToIndexes();
		}

	}

}
