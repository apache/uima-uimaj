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

package org.apache.uima.fit.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;
import org.uimafit.component.ViewTextCopierAnnotator;

/**
 * @author Philip Ogren
 * 
 */
public class ViewTextCopierAnnotatorTest extends ComponentTestBase {

	@Test
	public void testViewTextCopier() throws ResourceInitializationException,
			AnalysisEngineProcessException, CASException {

		String text = "sample text";
		String sourceViewName = "SourceView";
		String destinationViewName = "DestinationView";

		jCas.setDocumentText(text);
		AnalysisEngine viewCreator = AnalysisEngineFactory.createPrimitive(
				ViewTextCopierAnnotator.class, typeSystemDescription,
				ViewTextCopierAnnotator.PARAM_SOURCE_VIEW_NAME, CAS.NAME_DEFAULT_SOFA,
				ViewTextCopierAnnotator.PARAM_DESTINATION_VIEW_NAME, destinationViewName);
		viewCreator.process(jCas);
		JCas destinationView = jCas.getView(destinationViewName);
		assertNotNull(destinationView);
		assertEquals(text, destinationView.getDocumentText());

		jCas.reset();
		jCas.setDocumentText(text);
		jCas.createView(destinationViewName);
		viewCreator.process(jCas);
		destinationView = jCas.getView(destinationViewName);
		assertNotNull(destinationView);
		assertEquals(text, destinationView.getDocumentText());

		viewCreator = AnalysisEngineFactory.createPrimitive(ViewTextCopierAnnotator.class,
				typeSystemDescription, ViewTextCopierAnnotator.PARAM_SOURCE_VIEW_NAME,
				sourceViewName, ViewTextCopierAnnotator.PARAM_DESTINATION_VIEW_NAME,
				destinationViewName);
		jCas.reset();
		JCas sourceView = jCas.createView(sourceViewName);
		sourceView.setDocumentText(text);
		viewCreator.process(jCas);
		destinationView = jCas.getView(destinationViewName);
		assertNotNull(destinationView);
		assertEquals(text, destinationView.getDocumentText());
		assertNull(jCas.getDocumentText());
	}

	@Test(expected = AnalysisEngineProcessException.class)
	public void testExceptions() throws ResourceInitializationException,
			AnalysisEngineProcessException {

		String sourceViewName = "SourceView";
		String destinationViewName = "DestinationView";

		AnalysisEngine viewCreator = AnalysisEngineFactory.createPrimitive(
				ViewTextCopierAnnotator.class, typeSystemDescription,
				ViewTextCopierAnnotator.PARAM_SOURCE_VIEW_NAME, sourceViewName,
				ViewTextCopierAnnotator.PARAM_DESTINATION_VIEW_NAME, destinationViewName);
		viewCreator.process(jCas);
	}
}
