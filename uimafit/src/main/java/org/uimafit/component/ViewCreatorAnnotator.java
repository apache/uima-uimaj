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
 * This annotator can be placed at/near the beginning of a pipeline to ensure that a particular view
 * is created before it is used further downstream. It will create a view for the view name
 * specified by the configuration parameter PARAM_VIEW_NAME if it doesn't exist. One place this is
 * useful is if you are using an annotator that uses the default view and you have mapped the
 * default view into a different view via a sofa mapping. The default view is created automatically
 * - but if you have mapped the default view to some other view, then the view provided to your
 * annotator (when it asks for the default view) will not be created unless you have explicitly
 * created it.
 * 
 * @author Philip Ogren
 * 
 */
public class ViewCreatorAnnotator extends JCasAnnotator_ImplBase {

	/**
	 * The parameter name for the name of the viewed to be created by this annotator
	 */
	public static final String PARAM_VIEW_NAME = ConfigurationParameterFactory
			.createConfigurationParameterName(ViewCreatorAnnotator.class, "viewName");

	@ConfigurationParameter(mandatory = true)
	private String viewName;

	@Override
	public void process(final JCas aJCas) throws AnalysisEngineProcessException {
		createViewSafely(aJCas, viewName);
	}

	/**
	 * Provides a simple call that allows you to safely create a view if it has not been created
	 * yet. If the view already exists, it is ok to call this method anyways without worrying about
	 * checking for this yet.
	 * 
	 * @return true if the view was created as a result of calling this method. false if the view
	 *         already existed.
	 */
	public static JCas createViewSafely(final JCas aJCas, final String aViewName)
			throws AnalysisEngineProcessException {
		try {
			try {
				return aJCas.getView(aViewName);
			}
			catch (CASRuntimeException ce) {
				return aJCas.createView(aViewName);
			}
		}
		catch (CASException ce) {
			throw new AnalysisEngineProcessException(ce);
		}
	}
}
