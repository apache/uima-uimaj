/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.uima.fit.component.initialize;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.fit.internal.ReflectionUtil;
import org.apache.uima.fit.internal.propertyeditors.PropertyEditorUtil;
import org.apache.uima.resource.ConfigurationManager;
import org.apache.uima.resource.CustomResourceSpecifier;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.beans.PropertyValue;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.DataBinder;
import org.springframework.validation.ObjectError;

/**
 * <p>
 * Initialize an instance of a class with fields that are annotated as
 * {@link ConfigurationParameter}s from the parameter values given in a {@link UimaContext}.
 * </p>
 * 
 */

public final class ConfigurationParameterInitializer {

  private ConfigurationParameterInitializer() {
    // Utility class
  }

  /**
   * Initialize a component from an {@link UimaContext} This code can be a little confusing because
   * the configuration parameter annotations are used in two contexts: in describing the component
   * and to initialize member variables from a {@link UimaContext}. Here we are performing the
   * latter task. It is important to remember that the {@link UimaContext} passed in to this method
   * may or may not have been derived using reflection of the annotations (i.e. using
   * {@link ConfigurationParameterFactory} via e.g. a call to a AnalysisEngineFactory.create
   * method). It is just as possible for the description of the component to come directly from an
   * XML descriptor file. So, for example, just because a configuration parameter specifies a
   * default value, this does not mean that the passed in context will have a value for that
   * configuration parameter. It should be possible for a descriptor file to specify its own value
   * or to not provide one at all. If the context does not have a configuration parameter, then the
   * default value provided by the developer as specified by the defaultValue element of the
   * {@link ConfigurationParameter} will be used. See comments in the code for additional details.
   * @param component
   *          the component to initialize.
   * @param context
   *          a UIMA context with configuration parameters.
   * @throws ResourceInitializationException
   *           if a failure occurs during initialization.
   */
  public static void initialize(final Object component, final UimaContext context)
          throws ResourceInitializationException {
    MutablePropertyValues values = new MutablePropertyValues();
    List<String> mandatoryValues = new ArrayList<String>();

    for (Field field : ReflectionUtil.getFields(component)) { // component.getClass().getDeclaredFields())
      if (ConfigurationParameterFactory.isConfigurationParameterField(field)) {
        org.apache.uima.fit.descriptor.ConfigurationParameter annotation = ReflectionUtil
                .getAnnotation(field, org.apache.uima.fit.descriptor.ConfigurationParameter.class);

        Object parameterValue;
        String parameterName = ConfigurationParameterFactory.getConfigurationParameterName(field);

        // Obtain either from the context - or - if the context does not provide the
        // parameter, check if there is a default value. Note there are three possibilities:
        // 1) Parameter present and set
        // 2) Parameter present and set to null (null value)
        // 3) Parameter not present (also provided as null value by UIMA)
        // Unfortunately we cannot make a difference between case 2 and 3 since UIMA does
        // not allow us to actually get a list of the parameters set in the context. We can
        // only get a list of the declared parameters. Thus we have to rely on the null
        // value.
        parameterValue = context.getConfigParameterValue(parameterName);
        if (parameterValue == null) {
          parameterValue = ConfigurationParameterFactory.getDefaultValue(field);
        }

        if (parameterValue != null) {
          values.addPropertyValue(field.getName(), parameterValue);
        }

        // TODO does this check really belong here? It seems that
        // this check is already performed by UIMA
        if (annotation.mandatory()) {
          mandatoryValues.add(field.getName());
        }
      }
    }

    DataBinder binder = new DataBinder(component) {
      @Override
      protected void checkRequiredFields(MutablePropertyValues mpvs) {
        String[] requiredFields = getRequiredFields();
        if (!ObjectUtils.isEmpty(requiredFields)) {
          Map<String, PropertyValue> propertyValues = new HashMap<String, PropertyValue>();
          PropertyValue[] pvs = mpvs.getPropertyValues();
          for (PropertyValue pv : pvs) {
            String canonicalName = PropertyAccessorUtils.canonicalPropertyName(pv.getName());
            propertyValues.put(canonicalName, pv);
          }
          for (String field : requiredFields) {
            PropertyValue pv = propertyValues.get(field);
            boolean empty = (pv == null || pv.getValue() == null);
            // For our purposes, empty Strings or empty String arrays do not count as
            // empty. Empty is only "null".
            // if (!empty) {
            // if (pv.getValue() instanceof String) {
            // empty = !StringUtils.hasText((String) pv.getValue());
            // }
            // else if (pv.getValue() instanceof String[]) {
            // String[] values = (String[]) pv.getValue();
            // empty = (values.length == 0 || !StringUtils.hasText(values[0]));
            // }
            // }
            if (empty) {
              // Use bind error processor to create FieldError.
              getBindingErrorProcessor()
                      .processMissingFieldError(field, getInternalBindingResult());
              // Remove property from property values to bind:
              // It has already caused a field error with a rejected value.
              if (pv != null) {
                mpvs.removePropertyValue(pv);
                propertyValues.remove(field);
              }
            }
          }
        }
      }
    };
    binder.initDirectFieldAccess();
    PropertyEditorUtil.registerUimaFITEditors(binder);
    binder.setRequiredFields(mandatoryValues.toArray(new String[mandatoryValues.size()]));
    binder.bind(values);
    if (binder.getBindingResult().hasErrors()) {
      StringBuilder sb = new StringBuilder();
      sb.append("Errors initializing [" + component.getClass() + "]");
      for (ObjectError error : binder.getBindingResult().getAllErrors()) {
        if (sb.length() > 0) {
          sb.append("\n");
        }
        sb.append(error.getDefaultMessage());
      }
      throw new IllegalArgumentException(sb.toString());
    }
  }

  /**
   * Initialize a component from a map.
   * 
   * @param component
   *          the component to initialize.
   * @param map
   *          a UIMA context with configuration parameters.
   * @see #initialize(Object, UimaContext)
   */
  public static void initialize(final Object component, final Map<String, Object> map)
          throws ResourceInitializationException {
    UimaContextAdmin context = UIMAFramework.newUimaContext(UIMAFramework.getLogger(),
            UIMAFramework.newDefaultResourceManager(), UIMAFramework.newConfigurationManager());
    ConfigurationManager cfgMgr = context.getConfigurationManager();
    cfgMgr.setSession(context.getSession());
    for (Entry<String, Object> e : map.entrySet()) {
      cfgMgr.setConfigParameterValue(context.getQualifiedContextName() + e.getKey(), e.getValue());
    }
    initialize(component, context);
  }

  /**
   * Initialize a component from a {@link CustomResourceSpecifier}.
   * 
   * @param component
   *          the component to initialize.
   * @param spec
   *          a resource specifier.
   * @see #initialize(Object, UimaContext)
   */
  public static void initialize(Object component, ResourceSpecifier spec)
          throws ResourceInitializationException {
    initialize(component, ConfigurationParameterFactory.getParameterSettings(spec));
  }

  /**
   * Initialize a component from a {@link CustomResourceSpecifier}.
   * 
   * @param component
   *          the component to initialize.
   * @param parameters
   *          a list of parameters.
   * @see #initialize(Object, UimaContext)
   */
  public static void initialize(Object component, Parameter... parameters)
          throws ResourceInitializationException {
    Map<String, Object> params = new HashMap<String, Object>();
    for (Parameter p : parameters) {
      params.put(p.getName(), p.getValue());
    }
    initialize(component, params);
  }

  /**
   * Initialize a component from a {@link ResourceMetaData}.
   * 
   * @param component
   *          the component to initialize.
   * @param parameters
   *          a list of parameters.
   * @see #initialize(Object, UimaContext)
   */
  public static void initialize(Object component, NameValuePair... parameters)
          throws ResourceInitializationException {
    Map<String, Object> params = new HashMap<String, Object>();
    for (NameValuePair p : parameters) {
      params.put(p.getName(), p.getValue());
    }
    initialize(component, params);
  }

  /**
   * Initialize a component from a {@link ResourceMetaData}.
   * 
   * @param component
   *          the component to initialize.
   * @param dataResource
   *          a data resource with configuration meta data.
   * @see #initialize(Object, UimaContext)
   */
  public static void initialize(Object component, DataResource dataResource)
          throws ResourceInitializationException {
    ResourceMetaData metaData = dataResource.getMetaData();
    ConfigurationParameterSettings settings = metaData.getConfigurationParameterSettings();
    initialize(component, settings.getParameterSettings());
  }
}
