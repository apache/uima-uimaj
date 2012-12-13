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

package org.uimafit.spring.util;

import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.impl.PrimitiveAnalysisEngine_impl;
import org.apache.uima.resource.Resource;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

/**
 * @author Richard Eckart de Castilho
 */
public class ResourceInitializationUtil {
	/**
	 * Initialize an existing object as a Spring bean.
	 */
	public static <T> T initializeBean(AutowireCapableBeanFactory aBeanFactory, T aBean,
			String aName) {
		@SuppressWarnings("unchecked")
		T wrappedBean = (T) aBeanFactory.initializeBean(aBean, aName);
		aBeanFactory.autowireBean(aBean);
		return wrappedBean;
	}

	/**
	 * Handle Spring-initialization of resoures produced by the UIMA framework.
	 */
	public static Resource initResource(Resource aResource,
			ApplicationContext aApplicationContext) {
		AutowireCapableBeanFactory beanFactory = aApplicationContext
				.getAutowireCapableBeanFactory();

		if (aResource instanceof PrimitiveAnalysisEngine_impl) {
			PropertyAccessor pa = PropertyAccessorFactory.forDirectFieldAccess(aResource);

			// Access the actual AnalysisComponent and initialize it
			AnalysisComponent analysisComponent = (AnalysisComponent) pa
					.getPropertyValue("mAnalysisComponent");
			initializeBean(beanFactory, analysisComponent, aResource.getMetaData().getName());
			pa.setPropertyValue("mAnalysisComponent", analysisComponent);

			return aResource;
		}
		else {
			return (Resource) beanFactory
					.initializeBean(aResource, aResource.getMetaData().getName());
		}
	}

}
