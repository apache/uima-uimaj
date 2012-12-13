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

import java.io.IOException;

import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.component.initialize.ConfigurationParameterInitializer;
import org.apache.uima.fit.component.initialize.ExternalResourceInitializer;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.util.ExtendedLogger;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Base class for CAS collection readers which initializes itself based on annotations.
 *
 * @author Richard Eckart de Castilho
 */
@OperationalProperties(outputsNewCases=true)
public abstract class CasCollectionReader_ImplBase extends CollectionReader_ImplBase {
	private ExtendedLogger logger;
	
	@Override
	public ExtendedLogger getLogger() {
		if (logger == null) {
			logger = new ExtendedLogger(getUimaContext());
		}
		return logger;
	}
	
	@Override
	// This method should not be overwritten. Overwrite initialize(UimaContext) instead.
	public final void initialize() throws ResourceInitializationException {
		ConfigurationParameterInitializer.initialize(this, getUimaContext());
		ExternalResourceInitializer.initialize(getUimaContext(), this);
		initialize(getUimaContext());
	}

	/**
	 * This method should be overwritten by subclasses.
	 */
	public void initialize(final UimaContext context) throws ResourceInitializationException {
		// Nothing by default
	}

	public void close() throws IOException {
		// Nothing by default
	}
}
