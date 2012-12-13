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
package org.uimafit.factory.testRes;

import static org.junit.Assert.assertEquals;

import org.apache.uima.fit.component.initialize.ConfigurationParameterInitializer;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;

/**
 * @author Richard Eckart de Castilho
 */
public class TestSharedResourceObject implements SharedResourceObject {
	public static final String EXPECTED_VALUE = "expected value";
	
	public final static String PARAM_VALUE = "value";
	@ConfigurationParameter(name = PARAM_VALUE)
	private String value;

	public void assertConfiguredOk() {
		System.out.println(getClass().getSimpleName()+".assertConfiguredOk()");
		// Ensure normal parameters get passed to External Resource
		assertEquals(EXPECTED_VALUE, value);
	}
	
	public void load(DataResource aData) throws ResourceInitializationException {
		ConfigurationParameterInitializer.initialize(this, aData);
	}
}