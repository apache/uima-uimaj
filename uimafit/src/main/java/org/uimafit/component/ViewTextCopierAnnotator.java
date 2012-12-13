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

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.jcas.JCas;

/**
 * This annotator will copy the text of one view to another view. It is required that source view
 * already exists. If the destination view does not yet exist, then it will be created. A use case
 * for this analysis engine is when you have a "gold view" which is populated with text and
 * gold-standard annotations. Now you want to run your system/pipeline on a "system view" so that at
 * the end you can compare the annotations in the system view with those in the gold view. It is
 * convenient to have an annotator that simply copies the text from the gold view to the system view
 * before running a pipeline on the system view.
 * 
 * @author Philip Ogren
 * 
 */

public class ViewTextCopierAnnotator extends JCasAnnotator_ImplBase {

	/**
	 * The parameter name for the name of the source view
	 */
	public static final String PARAM_SOURCE_VIEW_NAME = ConfigurationParameterFactory
			.createConfigurationParameterName(ViewTextCopierAnnotator.class, "sourceViewName");

	@ConfigurationParameter(mandatory = true)
	private String sourceViewName;

	/**
	 * The parameter name for the name of the destination view
	 */
	public static final String PARAM_DESTINATION_VIEW_NAME = ConfigurationParameterFactory
			.createConfigurationParameterName(ViewTextCopierAnnotator.class, "destinationViewName");

	@ConfigurationParameter(mandatory = true)
	private String destinationViewName;

	@Override
	public void process(final JCas jCas) throws AnalysisEngineProcessException {
		try {
			final JCas sourceView = jCas.getView(sourceViewName);
			JCas destinationView;
			try {
				destinationView = jCas.getView(destinationViewName);
			}
			catch (CASRuntimeException ce) {
				destinationView = jCas.createView(destinationViewName);
			}
			destinationView.setDocumentText(sourceView.getDocumentText());
		}
		catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

}
