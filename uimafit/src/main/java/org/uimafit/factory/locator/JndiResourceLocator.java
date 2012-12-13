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
package org.uimafit.factory.locator;

import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.uimafit.component.Resource_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.descriptor.ExternalResourceLocator;

/**
 * Locate an object via JNDI.
 *
 * @author Richard Eckart de Castilho
 */
public class JndiResourceLocator extends Resource_ImplBase implements ExternalResourceLocator {
	public static final String PARAM_NAME = "Name";
	@ConfigurationParameter(name = PARAM_NAME, mandatory = true)
	private String jndiName;

	private Object resource;

	@SuppressWarnings("rawtypes")
	@Override
	public boolean initialize(ResourceSpecifier aSpecifier, Map aAdditionalParams)
			throws ResourceInitializationException {
		if (!super.initialize(aSpecifier, aAdditionalParams)) {
			return false;
		}

		try {
			InitialContext ctx = new InitialContext();
			resource = ctx.lookup(jndiName);
		}
		catch (NamingException e) {
			throw new ResourceInitializationException(e);
		}
		return true;
	}

	public Object getResource() {
		return resource;
	}
}
