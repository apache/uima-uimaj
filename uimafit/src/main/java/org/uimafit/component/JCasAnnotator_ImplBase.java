/*
 Copyright 2010
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
package org.uimafit.component;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.initialize.ConfigurationParameterInitializer;
import org.apache.uima.fit.component.initialize.ExternalResourceInitializer;
import org.apache.uima.fit.util.ExtendedLogger;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Base class for JCas annotators which initializes itself based on annotations.
 *
 * @author Richard Eckart de Castilho
 */
public abstract class JCasAnnotator_ImplBase extends
		org.apache.uima.analysis_component.JCasAnnotator_ImplBase {
	private ExtendedLogger logger;
	
	public ExtendedLogger getLogger() {
		if (logger == null) {
			logger = new ExtendedLogger(getContext());
		}
		return logger;
	}
	
	@Override
	public void initialize(final UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		ConfigurationParameterInitializer.initialize(this, context);
		ExternalResourceInitializer.initialize(context, this);
	}
}
