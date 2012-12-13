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

package org.uimafit.spring;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.impl.ResourceManager_impl;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.util.Level;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Allowing UIMA components to access beans from a {@link ApplicationContext context}.
 *
 * @author Richard Eckart de Castilho
 */
public class SpringContextResourceManager
	extends ResourceManager_impl
	implements ApplicationContextAware
{
	private ApplicationContext context;
	private boolean autowireEnabled = false;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void initializeExternalResources(ResourceManagerConfiguration aConfiguration,
			String aQualifiedContextName, java.util.Map<String, Object> aAdditionalParams)
			throws ResourceInitializationException {

		for (String name : BeanFactoryUtils.beanNamesIncludingAncestors(context)) {
			Object registration = mInternalResourceRegistrationMap.get(name);

			if (registration == null) {
				try {
					// Register resource
					// ResourceRegistration unfortunately is package private
					Object reg = newInstance(
							"org.apache.uima.resource.impl.ResourceManager_impl$ResourceRegistration",
							Object.class, context.getBean(name),
							ExternalResourceDescription.class, null,
							String.class, aQualifiedContextName);
					((Map) mInternalResourceRegistrationMap).put(name, reg);

					// Perform binding
					if (isAutowireEnabled()) {
						mResourceMap.put(aQualifiedContextName + name, context.getBean(name));
					}
				}
				catch (Exception e1) {
					throw new ResourceInitializationException(e1);
				}
			}
			else {
				try {
					Object desc = getFieldValue(registration, "description");

					if (desc != null) {
						String definingContext = getFieldValue(registration, "definingContext");

						if (aQualifiedContextName.startsWith(definingContext)) {
							UIMAFramework.getLogger().logrb(
									Level.CONFIG,
									ResourceManager_impl.class.getName(),
									"initializeExternalResources",
									LOG_RESOURCE_BUNDLE,
									"UIMA_overridden_resource__CONFIG",
									new Object[] { name, aQualifiedContextName,
											definingContext });
						}
						else {
							UIMAFramework.getLogger().logrb(
									Level.WARNING,
									ResourceManager_impl.class.getName(),
									"initializeExternalResources",
									LOG_RESOURCE_BUNDLE,
									"UIMA_duplicate_resource_name__WARNING",
									new Object[] { name, definingContext,
											aQualifiedContextName });
						}
					}
				}
				catch (Exception e1) {
					throw new ResourceInitializationException(e1);
				}
			}
		}

		super.initializeExternalResources(aConfiguration, aQualifiedContextName, aAdditionalParams);
	}

	public void setApplicationContext(ApplicationContext aApplicationContext)
		throws BeansException
	{
		context = aApplicationContext;
	}

	/**
	 * Instantiate a non-visible class.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T> T newInstance(String aClassName, Object... aArgs) throws ResourceInitializationException {
		Constructor constr = null;
		try {
			Class<?> cl = Class.forName(aClassName);

			List<Class> types = new ArrayList<Class>();
			List<Object> values = new ArrayList<Object>();
			for (int i = 0; i < aArgs.length; i += 2) {
				types.add((Class) aArgs[i]);
				values.add(aArgs[i+1]);
			}

			constr = cl.getDeclaredConstructor(types.toArray(new Class[types.size()]));
			constr.setAccessible(true);
			return (T) constr.newInstance(values.toArray(new Object[values.size()]));
		}
		catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
		finally {
			if (constr != null) {
				constr.setAccessible(false);
			}
		}
	}

	/**
	 * Get a field value from a non-visible field.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T getFieldValue(Object aObject, String aFieldName)
	{
		return (T) PropertyAccessorFactory.forDirectFieldAccess(aObject).getPropertyValue(aFieldName);
	}

	public void setAutowireEnabled(boolean aAutowireEnabled)
	{
		autowireEnabled = aAutowireEnabled;
	}

	public boolean isAutowireEnabled()
	{
		return autowireEnabled;
	}
}
