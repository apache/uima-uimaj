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

package org.uimafit.component.xwriter;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.initialize.ConfigurationParameterInitializer;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.fit.factory.initializable.Initializable;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * This is a very simple implementation of XWriterFileNamer that generates file names based on a
 * prefix string and a incrementing counter.
 * 
 * @author Philip Ogren
 */

public class IntegerFileNamer implements XWriterFileNamer, Initializable {

	/**
	 * The parameter name for the configuration parameter that specifies a fixed prefix for all
	 * returned file names.
	 */
	public static final String PARAM_PREFIX = ConfigurationParameterFactory
			.createConfigurationParameterName(IntegerFileNamer.class, "prefix");
	@ConfigurationParameter(description = "specify a prefix that is prepended to all returned file names", defaultValue="")
	private String prefix;

	int i = 1;

	public String nameFile(JCas jCas) {
		return prefix + i++;
	}

	public void initialize(UimaContext context) throws ResourceInitializationException {
		ConfigurationParameterInitializer.initialize(this, context);
	}
}
