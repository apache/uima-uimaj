/*
 Copyright 2009
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

package org.uimafit.factory;

import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.uimafit.factory.ConfigurationParameterFactory.canParameterBeSet;
import static org.uimafit.factory.ConfigurationParameterFactory.createConfigurationData;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.ArrayUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ConfigurableDataResourceSpecifier;
import org.apache.uima.resource.CustomResourceSpecifier;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.FileResourceSpecifier;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.ParameterizedDataResource;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.SharedResourceObject;
import org.apache.uima.resource.impl.ConfigurableDataResourceSpecifier_impl;
import org.apache.uima.resource.impl.ExternalResourceDependency_impl;
import org.apache.uima.resource.impl.ExternalResourceDescription_impl;
import org.apache.uima.resource.impl.FileResourceSpecifier_impl;
import org.apache.uima.resource.impl.Parameter_impl;
import org.apache.uima.resource.metadata.ExternalResourceBinding;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.resource.metadata.impl.ExternalResourceBinding_impl;
import org.apache.uima.resource.metadata.impl.ResourceManagerConfiguration_impl;
import org.apache.uima.resource.metadata.impl.ResourceMetaData_impl;
import org.apache.uima.util.InvalidXMLException;
import org.uimafit.descriptor.ExternalResource;
import org.uimafit.factory.ConfigurationParameterFactory.ConfigurationData;
import org.uimafit.util.ExtendedExternalResourceDescription_impl;

/**
 * Helper methods for external resources.
 *
 * @author Richard Eckart de Castilho
 */
public final class ExternalResourceFactory {
	public static final String PARAM_RESOURCE_NAME = "__UIMAFIT_RESOURCE_NAME__";
	
	/**
	 * Used to separate resource name from key for nested resource.
	 */
	public static final String PREFIX_SEPARATOR = "##";
	
	/**
	 * Counter used to create unique resource names.
	 */
	private final static AtomicLong DISAMBIGUATOR = new AtomicLong();

	private ExternalResourceFactory() {
		// This class is not meant to be instantiated
	}

	/**
	 * Create an external resource description for a custom resource. This is intended to
	 * be used together with ....
	 *
	 * @param aInterface
	 *            the interface the resource should implement.
	 * @param aParams
	 *            parameters passed to the resource when it is created.
	 * @return the description.
	 * @see CustomResourceSpecifier
	 */
	public static ExternalResourceDescription createExternalResourceDescription(
			Class<? extends Resource> aInterface, Object... aParams) {
		return createExternalResourceDescription(uniqueResourceKey(aInterface.getName()), aInterface, aParams);
	}
	
	/**
	 * Create an external resource description for a custom resource.
	 *
	 * @param aName
	 *            the name of the resource (the key).
	 * @param aInterface
	 *            the interface the resource should implement.
	 * @param aParams
	 *            parameters passed to the resource when it is created.
	 * @return the description.
	 * @see CustomResourceSpecifier
	 */
	public static ExternalResourceDescription createExternalResourceDescription(final String aName,
			Class<? extends Resource> aInterface, Object... aParams) {
		ConfigurationParameterFactory.ensureParametersComeInPairs(aParams);

		// Extract ExternalResourceDescriptions from configurationData
		List<ExternalResourceBinding> bindings = new ArrayList<ExternalResourceBinding>();
		List<ExternalResourceDescription> descs = new ArrayList<ExternalResourceDescription>();
		for (Entry<String, ExternalResourceDescription> res : extractExternalResourceParameters(
				aParams).entrySet()) {
			bindings.add(createExternalResourceBinding(res.getKey(), res.getValue()));
			descs.add(res.getValue());
		}
		
		List<Parameter> params = new ArrayList<Parameter>();
		if (aParams != null) {
			for (int i = 0; i < aParams.length / 2; i++) {
				if (aParams[i * 2 + 1] instanceof ExternalResourceDescription) {
					continue;
				}
				
				Parameter param = new Parameter_impl();
				param.setName((String) aParams[i * 2]);
				param.setValue((String) aParams[i * 2 + 1]);
				params.add(param);
			}
		}

		CustomResourceSpecifier spec = getResourceSpecifierFactory().createCustomResourceSpecifier();
		spec.setResourceClassName(aInterface.getName());
		spec.setParameters(params.toArray(new Parameter[params.size()]));

		ExtendedExternalResourceDescription_impl extRes = new ExtendedExternalResourceDescription_impl();
		extRes.setName(aName);
		extRes.setResourceSpecifier(spec);
		extRes.setExternalResourceBindings(bindings);
		extRes.setExternalResources(descs);

		return extRes;
	}
	
	/**
	 * Create an external resource description for a {@link SharedResourceObject}.
	 *
	 * @param aInterface
	 *            the interface the resource should implement.
	 * @param aUrl
	 *            the URL from which the resource is initialized.
	 * @param aParams
	 *            parameters passed to the resource when it is created.
	 * @return the description.
	 * @see ConfigurableDataResourceSpecifier
	 * @see SharedResourceObject
	 */
	public static ExternalResourceDescription createExternalResourceDescription(
			Class<? extends SharedResourceObject> aInterface, String aUrl, Object... aParams) {
		return createExternalResourceDescription(uniqueResourceKey(aInterface.getName()), 
				aInterface, aUrl, aParams);
	}

	/**
	 * Create an external resource description for a {@link SharedResourceObject}.
	 *
	 * @param aInterface
	 *            the interface the resource should implement.
	 * @param aUrl
	 *            the URL from which the resource is initialized.
	 * @param aParams
	 *            parameters passed to the resource when it is created.
	 * @return the description.
	 * @see ConfigurableDataResourceSpecifier
	 * @see SharedResourceObject
	 */
	public static ExternalResourceDescription createExternalResourceDescription(
			Class<? extends SharedResourceObject> aInterface, URL aUrl, Object... aParams) {
		return createExternalResourceDescription(uniqueResourceKey(aInterface.getName()), 
				aInterface, aUrl.toString(), aParams);
	}

	/**
	 * Create an external resource description for a {@link SharedResourceObject}.
	 *
	 * @param aInterface
	 *            the interface the resource should implement.
	 * @param aFile
	 *            the file from which the resource is initialized.
	 * @param aParams
	 *            parameters passed to the resource when it is created.
	 * @return the description.
	 * @see ConfigurableDataResourceSpecifier
	 * @see SharedResourceObject
	 */
	public static ExternalResourceDescription createExternalResourceDescription(
			Class<? extends SharedResourceObject> aInterface, File aFile, Object... aParams) {
		try {
			return createExternalResourceDescription(aInterface, aFile.toURI().toURL(), aParams);
		}
		catch (MalformedURLException e) {
			// This is something that usually cannot happen, so we degrade this to an 
			// IllegalArgumentException which is a RuntimeException that does not need to be caught.
			throw new IllegalArgumentException("File converts to illegal URL [" + aFile + "]");
		}
	}

	/**
	 * Create an external resource description for a {@link SharedResourceObject}.
	 *
	 * @param aName
	 *            the name of the resource (the key).
	 * @param aInterface
	 *            the interface the resource should implement.
	 * @param aUrl
	 *            the URL from which the resource is initialized.
	 * @param aParams
	 *            parameters passed to the resource when it is created.
	 * @return the description.
	 * @see ConfigurableDataResourceSpecifier
	 * @see SharedResourceObject
	 */
	public static ExternalResourceDescription createExternalResourceDescription(final String aName,
			Class<? extends SharedResourceObject> aInterface, String aUrl, Object... aParams) {
		ConfigurationData cfg = ConfigurationParameterFactory.createConfigurationData(aParams);
		ResourceMetaData_impl meta = new ResourceMetaData_impl();

		ConfigurationData reflectedConfigurationData = createConfigurationData(aInterface);
		ResourceCreationSpecifierFactory.setConfigurationParameters(meta,
				reflectedConfigurationData.configurationParameters,
				reflectedConfigurationData.configurationValues);
		ResourceCreationSpecifierFactory.setConfigurationParameters(meta,
				cfg.configurationParameters, cfg.configurationValues);

		ConfigurableDataResourceSpecifier_impl spec = new ConfigurableDataResourceSpecifier_impl();
		spec.setUrl(aUrl);
		spec.setMetaData(meta);

		ExtendedExternalResourceDescription_impl extRes = new ExtendedExternalResourceDescription_impl();
		extRes.setName(aName);
		extRes.setResourceSpecifier(spec);
		extRes.setImplementationName(aInterface.getName());
		
		return extRes;
	}

	/**
	 * Create an external resource description for a file addressable via an URL.
	 *
	 * @param aName
	 *            the name of the resource (the key).
	 * @param aUrl
	 *            a URL.
	 * @return the description.
	 * @see FileResourceSpecifier
	 */
	public static ExternalResourceDescription createExternalResourceDescription(final String aName,
			String aUrl) {
		ExternalResourceDescription extRes = new ExternalResourceDescription_impl();
		extRes.setName(aName);
		FileResourceSpecifier frs = new FileResourceSpecifier_impl();
		frs.setFileUrl(aUrl);
		extRes.setResourceSpecifier(frs);
		return extRes;
	}

	/**
	 * Create an external resource binding.
	 *
	 * @param aKey
	 *            the key to bind to.
	 * @param aResource
	 *            the resource to bind.
	 * @return the description.
	 */
	public static ExternalResourceBinding createExternalResourceBinding(final String aKey,
			final ExternalResourceDescription aResource) {
		return createExternalResourceBinding(aKey, aResource.getName());
	}

	/**
	 * Create an external resource binding.
	 *
	 * @param aKey
	 *            the key to bind to.
	 * @param aResourceKey
	 *            the resource key to bind.
	 * @return the description.
	 */
	public static ExternalResourceBinding createExternalResourceBinding(final String aKey,
			final String aResourceKey) {
		ExternalResourceBinding extResBind = new ExternalResourceBinding_impl();
		extResBind.setResourceName(aResourceKey);
		extResBind.setKey(aKey);
		return extResBind;
	}

	/**
	 * Creates an ExternalResourceDependency for a given key and interface
	 *
	 * @param aOptional
	 *            determines whether the dependency is optional
	 */
	public static ExternalResourceDependency createExternalResourceDependency(final String aKey,
			final Class<?> aInterface, final boolean aOptional) {
		ExternalResourceDependency dep = new ExternalResourceDependency_impl();
		dep.setInterfaceName(aInterface.getName());
		dep.setKey(aKey);
		dep.setOptional(aOptional);
		return dep;
	}

	/**
	 * Scan the given resource specifier for external resource dependencies and whenever a
	 * dependency is encounter that has the specified key, the resource will be bound.
	 * <p>
	 * <b>Caveat</b>: If you use this method, you may expect that {@link DataResource#getUrl()} or
	 * {@link DataResource#getUri()} will return the same URL that you have specified here. This may
	 * <b>NOT</b> be the case. UIMA will internally try to resolve the URL via a
	 * {@link ResourceManager}. If it cannot resolve a remove URL, this mechanism will think it may
	 * be a local file and will return some local path - or it may redirect it to some location as
	 * though fit by the {@link ResourceManager}.
	 *
	 * @param aDesc
	 *            a description.
	 * @param aKey
	 *            the key to bind to.
	 * @param aUrl
	 *            a URL.
	 * @see FileResourceSpecifier
	 */
	public static void bindResource(ResourceSpecifier aDesc, String aKey, URL aUrl)
			throws InvalidXMLException {
		bindResource(aDesc, aKey, aUrl.toString());
	}

	/**
	 * Scan the given resource specifier for external resource dependencies and whenever a
	 * dependency is encounter that has the specified key, the resource will be bound.
	 * <p>
	 * <b>Caveat</b>: If you use this method, you may expect that {@link DataResource#getUrl()} or
	 * {@link DataResource#getUri()} will return the URL of the file that you have specified here.
	 * This may <b>NOT</b> be the case. UIMA will internally try to resolve the URL via a
	 * {@link ResourceManager}. If it cannot resolve a remove URL, this mechanism will think it may
	 * be a local file and will return some local path - or it may redirect it to some location as
	 * though fit by the {@link ResourceManager}.
	 *
	 * @param aDesc
	 *            a description.
	 * @param aKey
	 *            the key to bind to.
	 * @param aFile
	 *            a file.
	 * @see FileResourceSpecifier
	 */
	public static void bindResource(ResourceSpecifier aDesc, String aKey, File aFile)
			throws InvalidXMLException {
		try {
			bindResource(aDesc, aKey, aFile.toURI().toURL());
		}
		catch (MalformedURLException e) {
			// This is something that usually cannot happen, so we degrade this to an 
			// IllegalArgumentException which is a RuntimeException that does not need to be caught.
			throw new IllegalArgumentException("File converts to illegal URL [" + aFile + "]");
		}
	}

	/**
	 * Scan the given resource specifier for external resource dependencies and whenever a
	 * dependency is encounter that has the specified key, the resource will be bound.
	 * <p>
	 * <b>Caveat</b>: If you use this method, you may expect that {@link DataResource#getUrl()} or
	 * {@link DataResource#getUri()} will return the same URL that you have specified here. This is
	 * may <b>NOT</b> be the case. UIMA will internally try to resolve the URL via a
	 * {@link ResourceManager}. If it cannot resolve a remove URL, this mechanism will think it may
	 * be a local file and will return some local path - or it may redirect it to some location as
	 * though fit by the {@link ResourceManager}.
	 *
	 * @param aDesc
	 *            a description.
	 * @param aKey
	 *            the key to bind to.
	 * @param aUrl
	 *            a URL.
	 * @see FileResourceSpecifier
	 */
	public static void bindResource(ResourceSpecifier aDesc, String aKey, String aUrl)
			throws InvalidXMLException {
		ExternalResourceDescription extRes = createExternalResourceDescription(aKey, aUrl);
		bindResource(aDesc, aKey, extRes);
	}

	/**
	 * Scan the given resource specifier for external resource dependencies and whenever a
	 * dependency is encounter that has a key equal to the resource class name, the resource will be
	 * bound.
	 *
	 * @param aDesc
	 *            a description.
	 * @param aRes
	 *            the resource to bind.
	 * @param aParams
	 *            parameters passed to the resource when it is created.
	 * @see CustomResourceSpecifier
	 */
	public static void bindResource(ResourceSpecifier aDesc, Class<? extends Resource> aRes,
			String... aParams) throws InvalidXMLException {
		bindResource(aDesc, aRes, aRes, aParams);
	}

	/**
	 * Scan the given resource specifier for external resource dependencies and whenever a
	 * dependency is encounter that has a key equal to the API class name, the resource will be
	 * bound.
	 *
	 * @param aDesc
	 *            a description.
	 * @param aApi
	 *            the resource interface.
	 * @param aRes
	 *            the resource to bind.
	 * @param aParams
	 *            parameters passed to the resource when it is created.
	 * @see CustomResourceSpecifier
	 */
	public static void bindResource(ResourceSpecifier aDesc, Class<?> aApi,
			Class<? extends Resource> aRes, String... aParams) throws InvalidXMLException {
		// Appending a disambiguation suffix it possible to have multiple instances of the same
		// resource with different settings to different keys.
		ExternalResourceDescription extRes = createExternalResourceDescription(
				uniqueResourceKey(aRes.getName()), aRes, (Object[]) aParams);
		bindResource(aDesc, aApi.getName(), extRes);
	}

	/**
	 * Scan the given resource specifier for external resource dependencies and whenever a
	 * dependency is encountered that has a key equal to the resource class name, the resource will
	 * be bound.
	 *
	 * @param aDesc
	 *            a description.
	 * @param aRes
	 *            the resource to bind.
	 * @param aUrl
	 *            the URL from which the resource is initialized.
	 * @param aParams
	 *            parameters passed to the resource when it is created.
	 * @see SharedResourceObject
	 */
	public static void bindResource(ResourceSpecifier aDesc,
			Class<? extends SharedResourceObject> aRes, String aUrl, Object... aParams)
			throws InvalidXMLException {
		bindResource(aDesc, aRes, aRes, aUrl, aParams);
	}

	/**
	 * Scan the given resource specifier for external resource dependencies and whenever a
	 * dependency is encountered that has a key equal to the API class name, the resource will be
	 * bound.
	 *
	 * @param aDesc
	 *            a description.
	 * @param aApi
	 *            the resource interface.
	 * @param aRes
	 *            the resource to bind.
	 * @param aUrl
	 *            the URL from which the resource is initialized.
	 * @param aParams
	 *            parameters passed to the resource when it is created.
	 * @see SharedResourceObject
	 */
	public static void bindResource(ResourceSpecifier aDesc, Class<?> aApi,
			Class<? extends SharedResourceObject> aRes, String aUrl, Object... aParams)
			throws InvalidXMLException {
		bindResource(aDesc, aApi.getName(), aRes, aUrl, aParams);
	}

	/**
	 * Scan the given resource specifier for external resource dependencies and whenever a
	 * dependency with the given key is encountered the resource will be bound.
	 *
	 * @param aDesc
	 *            a description.
	 * @param aKey
	 *            the key to bind to.
	 * @param aRes
	 *            the resource to bind.
	 * @param aUrl
	 *            the URL from which the resource is initialized.
	 * @param aParams
	 *            parameters passed to the resource when it is created.
	 * @see SharedResourceObject
	 */
	public static void bindResource(ResourceSpecifier aDesc, String aKey,
			Class<? extends SharedResourceObject> aRes, String aUrl, Object... aParams)
			throws InvalidXMLException {
		ExternalResourceDescription extRes = createExternalResourceDescription(
				uniqueResourceKey(aRes.getName()), aRes, aUrl, aParams);
		bind((AnalysisEngineDescription) aDesc, aKey, extRes);
	}

	/**
	 * Scan the given resource specifier for external resource dependencies and whenever a
	 * dependency with the given key is encountered, the given resource is bound to it.
	 *
	 * @param aDesc
	 *            a description.
	 * @param aKey
	 *            the key to bind to.
	 * @param aRes
	 *            the resource to bind.
	 * @param aParams
	 *            parameters passed to the resource when it is created.
	 * @see CustomResourceSpecifier
	 */
	public static void bindResource(ResourceSpecifier aDesc, String aKey,
			Class<? extends Resource> aRes, String... aParams) throws InvalidXMLException {
		if (ParameterizedDataResource.class.isAssignableFrom(aRes)) {
			createDependency(aDesc, aKey, DataResource.class);
		}

		// Appending a disambiguation suffix it possible to have multiple instances of the same
		// resource with different settings to different keys.
		ExternalResourceDescription extRes = createExternalResourceDescription(
				uniqueResourceKey(aRes.getName()), aRes, (Object[]) aParams);
		bindResource(aDesc, aKey, extRes);
	}

	/**
	 * Scan the given resource specifier for external resource dependencies and whenever a
	 * dependency with the given key is encountered, the given resource is bound to it.
	 *
	 * @param aDesc
	 *            a description.
	 * @param aKey
	 *            the key to bind to.
	 * @param aResDesc
	 *            the resource description.
	 */
	public static void bindResource(ResourceSpecifier aDesc, String aKey,
			ExternalResourceDescription aResDesc) throws InvalidXMLException {
		// Dispatch
		if (aDesc instanceof AnalysisEngineDescription) {
			bind((AnalysisEngineDescription) aDesc, aKey, aResDesc);
		}
	}

	/**
	 * Create a new dependency for the specified resource and bind it. This method is helpful for
	 * UIMA components that do not use the uimaFIT {@link ExternalResource} annotation, because no
	 * external resource dependencies can be automatically generated by uimaFIT for such components.
	 *
	 * @param aDesc
	 *            a description.
	 * @param aKey
	 *            the key to bind to.
	 * @param aImpl
	 *            the resource implementation.
	 * @param aParams
	 *            additional parameters supported by the resource.
	 */
	public static void createDependencyAndBind(ResourceSpecifier aDesc, String aKey,
			Class<? extends Resource> aImpl, String... aParams)
			throws InvalidXMLException {
		Class<?> api = (ParameterizedDataResource.class.isAssignableFrom(aImpl)) ? DataResource.class
				: aImpl;
		createDependencyAndBind(aDesc, aKey, aImpl, api, aParams);
	}

	/**
	 * Create a new dependency for the specified resource and bind it. This method is helpful for
	 * UIMA components that do not use the uimaFIT {@link ExternalResource} annotation, because no
	 * external resource dependencies can be automatically generated by uimaFIT for such components.
	 *
	 * @param aDesc
	 *            a description.
	 * @param aKey
	 *            the key to bind to.
	 * @param aImpl
	 *            the resource implementation.
	 * @param aParams
	 *            additional parameters supported by the resource.
	 */
	public static void createDependencyAndBind(ResourceSpecifier aDesc, String aKey,
			Class<? extends Resource> aImpl, Class<?> aApi, String... aParams)
			throws InvalidXMLException {
		createDependency(aDesc, aKey, aApi);
		bindResource(aDesc, aKey, aImpl, aParams);
	}


	/**
	 * Create a new dependency for the specified resource and bind it. This method is helpful for
	 * UIMA components that do not use the uimaFIT {@link ExternalResource} annotation, because no
	 * external resource dependencies can be automatically generated by uimaFIT for such components.
	 *
	 * @param aDesc
	 *            a description.
	 * @param aKey
	 *            the key to bind to.
	 * @param aApi
	 *            the resource API.
	 */
	public static void createDependency(ResourceSpecifier aDesc, String aKey, Class<?> aApi)
			throws InvalidXMLException {
		ExternalResourceDependency[] deps = getExternalResourceDependencies(aDesc);
		if (deps == null) {
			deps = new ExternalResourceDependency[] {};
		}

		// Check if the resource dependency is already present
		boolean found = false;
		for (ExternalResourceDependency dep : deps) {
			if (dep.getKey().equals(aKey)) {
				found = true;
				break;
			}
		}

		// If not, create one
		if (!found) {
			setExternalResourceDependencies(aDesc, (ExternalResourceDependency[]) ArrayUtils.add(
					deps, createExternalResourceDependency(aKey, aApi, false)));		}
	}
	
	/**
	 * Convenience method to set the external resource dependencies on a resource specifier. 
	 * Unfortunately different methods need to be used for different sub-classes.
	 * 
	 * @throws IllegalArgumentException if the sub-class passed is not supported.
	 */
	private static void setExternalResourceDependencies(
			ResourceSpecifier aDesc, ExternalResourceDependency[] aDependencies) {
		if (aDesc instanceof CollectionReaderDescription) {
			((CollectionReaderDescription) aDesc).setExternalResourceDependencies(aDependencies);
		}
		else if (aDesc instanceof AnalysisEngineDescription) {
			((AnalysisEngineDescription) aDesc).setExternalResourceDependencies(aDependencies);
		}
		else {
			throw new IllegalArgumentException("Resource specified cannot have external resource dependencies");
		}
	}

	/**
	 * Convenience method to get the external resource dependencies from a resource specifier. 
	 * Unfortunately different methods need to be used for different sub-classes.
	 * 
	 * @throws IllegalArgumentException if the sub-class passed is not supported.
	 */
	private static ExternalResourceDependency[] getExternalResourceDependencies(
			ResourceSpecifier aDesc) {
		if (aDesc instanceof CollectionReaderDescription) {
			return ((CollectionReaderDescription) aDesc).getExternalResourceDependencies();
		}
		else if (aDesc instanceof AnalysisEngineDescription) {
			return ((AnalysisEngineDescription) aDesc).getExternalResourceDependencies();
		}
		else {
			throw new IllegalArgumentException("Resource specified cannot have external resource dependencies");
		}
	}
	/**
	 * Create a new dependency for the specified resource and bind it. This method is helpful for
	 * UIMA components that do not use the uimaFIT {@link ExternalResource} annotation, because no
	 * external resource dependencies can be automatically generated by uimaFIT for such components.
	 *
	 * @param aDesc
	 *            a description.
	 * @param aKey
	 *            the key to bind to.
	 * @param aImpl
	 *            the resource implementation.
	 * @param aUrl
	 *            the resource URL.
	 * @param aParams
	 *            additional parameters supported by the resource.
	 */
	public static void createDependencyAndBind(AnalysisEngineDescription aDesc, String aKey,
			Class<? extends SharedResourceObject> aImpl, String aUrl, Object... aParams)
			throws InvalidXMLException {
		if (aDesc.getExternalResourceDependency(aKey) == null) {
			ExternalResourceDependency[] deps = aDesc.getExternalResourceDependencies();
			if (deps == null) {
				deps = new ExternalResourceDependency[] {};
			}
			aDesc.setExternalResourceDependencies((ExternalResourceDependency[]) ArrayUtils.add(
					deps, createExternalResourceDependency(aKey, aImpl, false)));
		}
		bindResource(aDesc, aKey, aImpl, aUrl, aParams);
	}

	/**
	 * Scan the given resource specifier for external resource dependencies and whenever a
	 * dependency with the given key is encountered, the given resource is bound to it.
	 *
	 * @param aDesc
	 *            a description.
	 * @param aKey
	 *            the key to bind to.
	 * @param aResDesc
	 *            the resource description.
	 */
	private static void bind(AnalysisEngineDescription aDesc, String aKey,
			ExternalResourceDescription aResDesc) throws InvalidXMLException {
		// Recursively address delegates
		if (!aDesc.isPrimitive()) {
			for (Object delegate : aDesc.getDelegateAnalysisEngineSpecifiers().values()) {
				bindResource((ResourceSpecifier) delegate, aKey, aResDesc);
			}
		}

		// Bind if necessary
		for (ExternalResourceDependency dep : aDesc.getExternalResourceDependencies()) {
			if (aKey.equals(dep.getKey())) {
				bindExternalResource(aDesc, aKey, aResDesc);
			}
		}
	}

	/**
	 * Create a new external resource binding.
	 *
	 * @param aResMgrCfg
	 *            the resource manager to create the binding in.
	 * @param aBindTo
	 *            what key to bind to.
	 * @param aRes
	 *            the resource that should be bound.
	 */
	public static void bindExternalResource(ResourceManagerConfiguration aResMgrCfg,
			String aBindTo, ExternalResourceDescription aRes) {
		// Create a map of all bindings
		Map<String, ExternalResourceBinding> bindings = new HashMap<String, ExternalResourceBinding>();
		for (ExternalResourceBinding b : aResMgrCfg.getExternalResourceBindings()) {
			bindings.put(b.getKey(), b);
		}
		
		// Create a map of all resources
		Map<String, ExternalResourceDescription> resources = new HashMap<String, ExternalResourceDescription>();
		for (ExternalResourceDescription r : aResMgrCfg.getExternalResources()) {
			resources.put(r.getName(), r);
		}
		
		// For the current resource, add resource and binding
		ExternalResourceBinding extResBind = createExternalResourceBinding(aBindTo, aRes);
		bindings.put(extResBind.getKey(), extResBind);
		resources.put(aRes.getName(), aRes);
		
		// Handle nested resources
		bindNestedResources(aRes, bindings, resources);
		
		// Commit everything to the resource manager configuration
		aResMgrCfg.setExternalResourceBindings(bindings.values().toArray(
				new ExternalResourceBinding[bindings.size()]));
		aResMgrCfg.setExternalResources(resources.values().toArray(
				new ExternalResourceDescription[resources.size()]));
	}
	
	/**
	 * Create a new external resource binding.
	 * 
	 * @param aRes
	 *            the resource to bind to
	 * @param aBindTo
	 *            what key to bind to.
	 * @param aNestedRes
	 *            the resource that should be bound.
	 */
	public static void bindExternalResource(ExternalResourceDescription aRes, String aBindTo,
			ExternalResourceDescription aNestedRes) {
		if (!(aRes instanceof ExtendedExternalResourceDescription_impl)) {
			throw new IllegalArgumentException(
					"Nested resources are only supported on instances of [" + 
					ExtendedExternalResourceDescription_impl.class.getName() + "] which" + 
					"can be created with uimaFIT's createExternalResourceDescription() methods.");
		}

		ExtendedExternalResourceDescription_impl extRes = (ExtendedExternalResourceDescription_impl) aRes;

		// Create a map of all bindings
		Map<String, ExternalResourceBinding> bindings = new HashMap<String, ExternalResourceBinding>();
		for (ExternalResourceBinding b : extRes.getExternalResourceBindings()) {
			bindings.put(b.getKey(), b);
		}
		
		// Create a map of all resources
		Map<String, ExternalResourceDescription> resources = new HashMap<String, ExternalResourceDescription>();
		for (ExternalResourceDescription r : extRes.getExternalResources()) {
			resources.put(r.getName(), r);
		}
		
		// For the current resource, add resource and binding
		ExternalResourceBinding extResBind = createExternalResourceBinding(aBindTo, aNestedRes);
		bindings.put(extResBind.getKey(), extResBind);
		resources.put(aRes.getName(), aRes);
		
		// Handle nested resources
		bindNestedResources(aRes, bindings, resources);
		
		// Commit everything to the resource manager configuration
		extRes.setExternalResourceBindings(bindings.values());
		extRes.setExternalResources(resources.values());
		
	}
	
	/**
	 * Helper method to recursively bind resources bound to resources.
	 * 
	 * @param aRes resource.
	 * @param aBindings bindings already made.
	 * @param aResources resources already bound.
	 */
	private static void bindNestedResources(ExternalResourceDescription aRes,
			Map<String, ExternalResourceBinding> aBindings,
			Map<String, ExternalResourceDescription> aResources)	{
		// Handle nested resources
		if (aRes instanceof ExtendedExternalResourceDescription_impl) {
			ExtendedExternalResourceDescription_impl extRes = (ExtendedExternalResourceDescription_impl) aRes;
			
			// Tell the external resource its name. This is needed in order to find the resources
			// bound to this resource later on. Set only if the resource supports this parameter.
			// Mind that supporting this parameter is mandatory for resource implementing 
			// ExternalResourceAware.
			if (canParameterBeSet(extRes.getResourceSpecifier(), PARAM_RESOURCE_NAME)) {
				ConfigurationParameterFactory.setParameter(extRes.getResourceSpecifier(),
						PARAM_RESOURCE_NAME, aRes.getName());
			}
			
			// Create a map of all resources
			Map<String, ExternalResourceDescription> res = new HashMap<String, ExternalResourceDescription>();
			for (ExternalResourceDescription r : extRes.getExternalResources()) {
				res.put(r.getName(), r);
			}
			
			// Bind nested resources
			for (ExternalResourceBinding b : extRes.getExternalResourceBindings()) {
				// Avoid re-prefixing the resource name
				String key = b.getKey();
				if (!key.startsWith(aRes.getName() + PREFIX_SEPARATOR)) {
					key = aRes.getName() + PREFIX_SEPARATOR + b.getKey();
				}
				// Avoid unnecessary binding and an infinite loop when a resource binds to itself
				if (!aBindings.containsKey(key)) {
					// Mark the current binding as processed so we do not recurse
					aBindings.put(key, b);
					ExternalResourceDescription nestedRes = res.get(b.getResourceName());
					aResources.put(nestedRes.getName(), nestedRes);
					bindNestedResources(nestedRes, aBindings, aResources);
					// Set the proper key on the binding.
					b.setKey(key);
				}
			}
		}
	}
	
	/**
	 * Create a new external resource binding.
	 *
	 * @param aDesc
	 *            the specifier to create the binding in.
	 * @param aBindTo
	 *            what key to bind to.
	 * @param aRes
	 *            the resource that should be bound.
	 */
	public static void bindExternalResource(ResourceCreationSpecifier aDesc,
			String aBindTo, ExternalResourceDescription aRes) {
		ResourceManagerConfiguration resMgrCfg = aDesc.getResourceManagerConfiguration();
		if (resMgrCfg == null) {
			resMgrCfg = new ResourceManagerConfiguration_impl();
			aDesc.setResourceManagerConfiguration(resMgrCfg);
		}
		
		bindExternalResource(resMgrCfg, aBindTo, aRes);
	}

	/**
	 * Create a new external resource binding.
	 *
	 * @param aResMgrCfg
	 *            the resource manager to create the binding in.
	 * @param aBindTo
	 *            what key to bind to.
	 * @param aRes
	 *            the resource that should be bound.
	 */
	public static void bindExternalResource(ResourceManagerConfiguration aResMgrCfg,
			String aBindTo, String aRes) {
		ExternalResourceBinding extResBind = createExternalResourceBinding(aBindTo, aRes);
		aResMgrCfg.addExternalResourceBinding(extResBind);
	}

	/**
	 * Create a new external resource binding.
	 *
	 * @param aDesc
	 *            the specifier to create the binding in.
	 * @param aBindTo
	 *            what key to bind to.
	 * @param aRes
	 *            the resource that should be bound.
	 */
	public static void bindExternalResource(ResourceCreationSpecifier aDesc,
			String aBindTo, String aRes) {
		ResourceManagerConfiguration resMgrCfg = aDesc.getResourceManagerConfiguration();
		if (resMgrCfg == null) {
			resMgrCfg = new ResourceManagerConfiguration_impl();
			aDesc.setResourceManagerConfiguration(resMgrCfg);
		}

		bindExternalResource(resMgrCfg, aBindTo, aRes);
	}
	
	static String uniqueResourceKey(String aKey)
	{
		return aKey + '-' + DISAMBIGUATOR.getAndIncrement();
	}
	
	/**
	 * Extracts the external resource from the configuration parameters and nulls out these
	 * parameters. Mind that the array passed to this method is modified by the method.
	 * 
	 * @param configurationData the configuration parameters.
	 * @return extRes the external resource parameters.
	 */
	protected static Map<String, ExternalResourceDescription> extractExternalResourceParameters(
			final Object[] configurationData) {
		if (configurationData == null) {
			return Collections.emptyMap();
		}
	
		Map<String, ExternalResourceDescription> extRes = new HashMap<String, ExternalResourceDescription>();
		for (int i = 0; i < configurationData.length - 1; i += 2) {
			String key = (String) configurationData[i];
			Object value = configurationData[i + 1];

			// Store External Resource parameters separately
			if (value instanceof ExternalResourceDescription) {
				ExternalResourceDescription description = (ExternalResourceDescription) value;
				extRes.put(key, description);
			}
		}
		
		return extRes;
	}
}
