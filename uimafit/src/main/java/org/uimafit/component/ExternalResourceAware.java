/*
 Copyright 2012
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

import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.factory.ExternalResourceFactory;

/**
 * Allows an external resource to use the {@link ExternalResource} annotation on member variables
 * to gain access to other external resources.
 * 
 * @author Richard Eckart de Castilho
 */
public interface ExternalResourceAware {
	/**
	 * Get the name of the resource. This is set by {@link 
	 * ExternalResourceFactory#bindExternalResource(org.apache.uima.resource.ResourceCreationSpecifier, 
	 * String, org.apache.uima.resource.ExternalResourceDescription) bindExternalResource()} as the
	 * parameter {@link ExternalResourceFactory#PARAM_RESOURCE_NAME PARAM_RESOURCE_NAME}. 
	 * <br/>
	 * <b>It is mandatory that any resource implementing this interface declares the configuration
	 * parameter {@link ExternalResourceFactory#PARAM_RESOURCE_NAME PARAM_RESOURCE_NAME}.</b>
	 * 
	 * @return the resource name.
	 */
	String getResourceName();
	
	
	/**
	 * Called after the external resources have been initialized.
	 */
	void afterResourcesInitialized();
}
