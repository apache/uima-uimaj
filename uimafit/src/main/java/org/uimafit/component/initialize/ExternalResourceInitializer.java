/*
 Copyright 2009-2010
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
package org.uimafit.component.initialize;

import static org.uimafit.factory.ExternalResourceFactory.PREFIX_SEPARATOR;
import static org.uimafit.factory.ExternalResourceFactory.createExternalResourceDependency;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.SharedResourceObject;
import org.apache.uima.resource.impl.ResourceManager_impl;
import org.uimafit.component.ExternalResourceAware;
import org.uimafit.descriptor.ExternalResource;
import org.uimafit.descriptor.ExternalResourceLocator;
import org.uimafit.util.ReflectionUtil;

/**
 * Configurator class for {@link ExternalResource} annotations.
 *
 * @author Richard Eckart de Castilho
 */
public class ExternalResourceInitializer {
	
	private static final Object INITIALIZED = new Object();
	private static Map<Object, Object> initializedResources = new WeakHashMap<Object, Object>();
	
	/**
	 * Configure a component from the given context.
	 *
	 * @param <T>
	 *            the component type.
	 * @param context
	 *            the UIMA context.
	 * @param object
	 *            the component.
	 * @throws ResourceInitializationException
	 *             if the external resource cannot be configured.
	 */
	public static <T> void initialize(UimaContext context, T object)
			throws ResourceInitializationException {
		configure(context, object.getClass(), object.getClass(), object,
				getResourceDeclarations(object.getClass()));
	}
	
	/**
	 * Helper method for recursively configuring super-classes.
	 *
	 * @param <T>
	 *            the component type.
	 * @param context
	 *            the context containing the resource bindings.
	 * @param baseCls
	 *            the class on which configuration started.
	 * @param cls
	 *            the class currently being configured.
	 * @param object
	 *            the object being configured.
	 * @param dependencies
	 *            the dependencies.
	 * @throws ResourceInitializationException
	 *             if required resources could not be bound.
	 */
	private static <T> void configure(UimaContext context, Class<?> baseCls, Class<?> cls,
			T object, Map<String, ExternalResourceDependency> dependencies)
			throws ResourceInitializationException {
		if (cls.getSuperclass() != null) {
			configure(context, baseCls, cls.getSuperclass(), object, dependencies);
		}
		else {
			// Try to initialize the external resources only once, not for each step of the
			// class hierarchy of a component.
			initializeNestedResources(context);
		}
		
		for (Field field : cls.getDeclaredFields()) {
			if (!field.isAnnotationPresent(ExternalResource.class)) {
				continue;
			}

			// Get the resource key. If it is a nested resource, also get the prefix.
			String key = getKey(field);
			if (object instanceof ExternalResourceAware) {
				String prefix = ((ExternalResourceAware) object).getResourceName();
				if (prefix != null) {
					key = prefix + PREFIX_SEPARATOR + key;
				}
			}
			
			// Obtain the resource
			Object value;
			try {
				value = context.getResourceObject(key);
			}
			catch (ResourceAccessException e) {
				throw new ResourceInitializationException(e);
			}
			if (value instanceof ExternalResourceLocator) {
				value = ((ExternalResourceLocator) value).getResource();
			}

			// Sanity checks
			if (value == null && isMandatory(field)) {
				throw new ResourceInitializationException(new IllegalStateException(
						"Mandatory resource [" + key + "] is not set on [" + baseCls + "]"));
			}

			// Now record the setting and optionally apply it to the given
			// instance.
			if (value != null) {
				field.setAccessible(true);
				try {
					field.set(object, value);
				}
				catch (IllegalAccessException e) {
					throw new ResourceInitializationException(e);
				}
				field.setAccessible(false);
			}
		}		
	}
	
	/**
	 * Scan the context and initialize external resources injected into other external resources.
	 * 
	 * @param aContext the UIMA context.
	 */
	private static void initializeNestedResources(UimaContext aContext)
			throws ResourceInitializationException {
		List<ExternalResourceAware> awareResources = new ArrayList<ExternalResourceAware>();
		
		// Initialize the resources - each resource must only be initialized once. We remember
		// if a resource has already been initialized in a weak hash map, so we automatically
		// forget about resources that are garbage collected.
		for (Object r : getResources(aContext)) {
			synchronized (initializedResources) {
				if (r instanceof ExternalResourceAware && !initializedResources.containsKey(r)) {
					// Already mark the resource as initialized so we do not run into an 
					// endless recursive loop when initialize() is called again.
					initializedResources.put(r, INITIALIZED);
					initialize(aContext, r);
					awareResources.add((ExternalResourceAware) r);
				}
			}
		}
		
		// Notify the resources after everything has been configured
		for (ExternalResourceAware res : awareResources) {
			res.afterResourcesInitialized();
		}
	}
	
	/**
	 * Get all resources declared in the context.
	 */
	private static Collection<?> getResources(UimaContext aContext)
			throws ResourceInitializationException	{
		if (!(aContext instanceof UimaContextAdmin)) {
			return Collections.emptyList();
		}
		
		ResourceManager resMgr = ((UimaContextAdmin) aContext).getResourceManager();
		if (!(resMgr instanceof ResourceManager_impl)) {
			// Unfortunately there is not official way to access the list of resources. Thus we
			// have to rely on the UIMA implementation details and access the internal resource
			// map via reflection. If the resource manager is not derived from the default 
			// UIMA resource manager, then we cannot really do anything here.
			throw new IllegalStateException("Unsupported resource manager implementation ["
					+ resMgr.getClass() + "]");
		}
		
		Field resourceMapField = null;
		try {
			// Fetch the list of resources
			resourceMapField = ReflectionUtil.getField(resMgr, "mResourceMap");
			resourceMapField.setAccessible(true);			
			@SuppressWarnings("unchecked")
			Map<String, Object> resources = (Map<String, Object>) resourceMapField.get(resMgr);
			
			return resources.values();
		}
		catch (SecurityException e) {
			throw new ResourceInitializationException(e);
		}
		catch (NoSuchFieldException e) {
			throw new ResourceInitializationException(e);
		}
		catch (IllegalArgumentException e) {
			throw new ResourceInitializationException(e);
		}
		catch (IllegalAccessException e) {
			throw new ResourceInitializationException(e);
		}
		finally {
			if (resourceMapField != null) {
				resourceMapField.setAccessible(false);
			}
		}
	}
	
	public static <T> Map<String, ExternalResourceDependency> getResourceDeclarations(Class<?> cls)
			throws ResourceInitializationException {
		Map<String, ExternalResourceDependency> deps = new HashMap<String, ExternalResourceDependency>();
		getResourceDeclarations(cls, cls, deps);
		return deps;
	}

	private static <T> void getResourceDeclarations(Class<?> baseCls, Class<?> cls,
			Map<String, ExternalResourceDependency> dependencies)
			throws ResourceInitializationException {
		if (cls.getSuperclass() != null) {
			getResourceDeclarations(baseCls, cls.getSuperclass(), dependencies);
		}

		for (Field field : cls.getDeclaredFields()) {
			if (!field.isAnnotationPresent(ExternalResource.class)) {
				continue;
			}

			if (dependencies.containsKey(getKey(field))) {
				throw new ResourceInitializationException(new IllegalStateException("Key ["
						+ getKey(field) + "] may only be used on a single field."));
			}

			dependencies.put(
					getKey(field),
					createExternalResourceDependency(getKey(field), getApi(field),
							!isMandatory(field)));
		}
	}

	/**
	 * Determine if the field is mandatory.
	 *
	 * @param field
	 *            the field to bind.
	 * @return whether the field is mandatory.
	 */
	private static boolean isMandatory(Field field) {
		return field.getAnnotation(ExternalResource.class).mandatory();
	}

	/**
	 * Get the binding key for the specified field. If no key is set, use the field class name as
	 * key.
	 *
	 * @param field
	 *            the field to bind.
	 * @return the binding key.
	 */
	private static String getKey(Field field) {
		ExternalResource cpa = field.getAnnotation(ExternalResource.class);
		String key = cpa.key();
		if (key.length() == 0) {
			key = field.getType().getName();
		}
		return key;
	}

	/**
	 * Get the type of class/interface a resource has to implement to bind to the annotated field.
	 * If no API is set, get it from the annotated field type.
	 *
	 * @param field
	 *            the field to bind.
	 * @return the API type.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Class<? extends Resource> getApi(Field field) {
		ExternalResource cpa = field.getAnnotation(ExternalResource.class);
		Class<? extends Resource> api = cpa.api();
		// If no api is specified, look at the annotated field
		if (api == Resource.class) {
			if (Resource.class.isAssignableFrom(field.getType())
					|| SharedResourceObject.class.isAssignableFrom(field.getType())) {
				// If no API is set, check if the field type is already a resource type
				api = (Class<? extends Resource>) field.getType();
			}
			else {
				// If the field does not have a resource type, assume whatever. This allows to use
				// a resource locator without having to specify the api parameter. It also allows
				// to directly inject Java objects - yes, I know that Object does not extend
				// Resource - REC, 2011-03-25
				api = (Class) Object.class;
			}
		}
		return api;
	}
}
