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
package org.uimafit.component;

import java.util.Map;

import org.apache.uima.fit.component.initialize.ConfigurationParameterInitializer;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.util.ExtendedLogger;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;

/**
 * Base class for external resources which initializes itself based on annotations.
 *
 * @author Richard Eckart de Castilho
 */
public abstract class Resource_ImplBase extends org.apache.uima.resource.Resource_ImplBase
		implements ExternalResourceAware {

	private ExtendedLogger logger;
	
	@ConfigurationParameter(name=ExternalResourceFactory.PARAM_RESOURCE_NAME, mandatory=false)
	private String resourceName;
	
	@Override
	public ExtendedLogger getLogger() {
		if (logger == null) {
			logger = new ExtendedLogger(getUimaContext());
		}
		return logger;
	}
	
	@Override
	public boolean initialize(final ResourceSpecifier aSpecifier,
			final Map<String, Object> aAdditionalParams) throws ResourceInitializationException {
		if (!super.initialize(aSpecifier, aAdditionalParams)) {
			return false;
		}

		ConfigurationParameterInitializer.initialize(this, aSpecifier);
		// We cannot call ExternalResourceInitializer.initialize() because the 
		// ResourceManager_impl has not added the resources to the context yet.
		// Resource initialization is handled by ExternalResourceInitializer.initialize()
		// when it is called on the first pipeline component.
		
		return true;
	}
	
	public String getResourceName() {
		return resourceName;
	}
	
	public void afterResourcesInitialized() {
		// Per default nothing is done here.
	}
}
