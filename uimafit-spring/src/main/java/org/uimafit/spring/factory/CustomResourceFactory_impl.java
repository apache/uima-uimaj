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

package org.uimafit.spring.factory;

import static org.uimafit.spring.util.ResourceInitializationUtil.initResource;

import java.util.Map;

import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author Richard Eckart de Castilho
 */
public class CustomResourceFactory_impl
	extends org.apache.uima.impl.CustomResourceFactory_impl
	implements ApplicationContextAware
{
	private ApplicationContext applicationContext;

	public void setApplicationContext(ApplicationContext aApplicationContext)
		throws BeansException
	{
		applicationContext = aApplicationContext;
	}

	@Override
	public Resource produceResource(Class<? extends Resource> aResourceClass,
			ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
		throws ResourceInitializationException
	{
		Resource resource = super.produceResource(aResourceClass, aSpecifier, aAdditionalParams);
		return initResource(resource, applicationContext);
	}
}
