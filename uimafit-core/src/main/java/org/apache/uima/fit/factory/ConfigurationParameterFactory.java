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
package org.apache.uima.fit.factory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.fit.factory.ExternalResourceFactory.ResourceValueType;
import org.apache.uima.fit.internal.ReflectionUtil;
import org.apache.uima.fit.internal.propertyeditors.PropertyEditorUtil;
import org.apache.uima.resource.ConfigurableDataResourceSpecifier;
import org.apache.uima.resource.CustomResourceSpecifier;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.PearSpecifier;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.impl.Parameter_impl;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.impl.ConfigurationParameter_impl;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeMismatchException;

public final class ConfigurationParameterFactory {
  private ConfigurationParameterFactory() {
    // This class is not meant to be instantiated
  }

  /**
   * A mapping from Java class names to UIMA configuration parameter type names. Used by
   * setConfigurationParameters().
   */
  private static final Map<String, String> JAVA_UIMA_TYPE_MAP = new HashMap<String, String>();
  static {
    JAVA_UIMA_TYPE_MAP.put("boolean", ConfigurationParameter.TYPE_BOOLEAN);
    JAVA_UIMA_TYPE_MAP.put(Boolean.class.getName(), ConfigurationParameter.TYPE_BOOLEAN);
    JAVA_UIMA_TYPE_MAP.put("float", ConfigurationParameter.TYPE_FLOAT);
    JAVA_UIMA_TYPE_MAP.put(Float.class.getName(), ConfigurationParameter.TYPE_FLOAT);
    JAVA_UIMA_TYPE_MAP.put("double", ConfigurationParameter.TYPE_DOUBLE);
    JAVA_UIMA_TYPE_MAP.put(Double.class.getName(), ConfigurationParameter.TYPE_DOUBLE);
    JAVA_UIMA_TYPE_MAP.put("int", ConfigurationParameter.TYPE_INTEGER);
    JAVA_UIMA_TYPE_MAP.put(Integer.class.getName(), ConfigurationParameter.TYPE_INTEGER);
    JAVA_UIMA_TYPE_MAP.put("long", ConfigurationParameter.TYPE_LONG);
    JAVA_UIMA_TYPE_MAP.put(Long.class.getName(), ConfigurationParameter.TYPE_LONG);
    JAVA_UIMA_TYPE_MAP.put(String.class.getName(), ConfigurationParameter.TYPE_STRING);
  }

  /**
   * This method determines if the field is annotated with
   * {@link org.apache.uima.fit.descriptor.ConfigurationParameter}.
   * 
   * @param field
   *          the field to analyze
   * @return whether the field is marked as a configuration parameter
   */
  public static boolean isConfigurationParameterField(Field field) {
    return ReflectionUtil.isAnnotationPresent(field,
            org.apache.uima.fit.descriptor.ConfigurationParameter.class);
  }

  /**
   * Determines the default value of an annotated configuration parameter. The returned value is not
   * necessarily the value that the annotated member variable will be instantiated with in
   * ConfigurationParameterInitializer which does extra work to convert the UIMA configuration
   * parameter value to comply with the type of the member variable.
   * 
   * @param field
   *          the field to analyze
   * @return the default value
   */
  public static Object getDefaultValue(Field field) {
    if (isConfigurationParameterField(field)) {
      org.apache.uima.fit.descriptor.ConfigurationParameter annotation = ReflectionUtil
              .getAnnotation(field, org.apache.uima.fit.descriptor.ConfigurationParameter.class);

      String[] stringValue = annotation.defaultValue();
      if (stringValue.length == 1 && stringValue[0]
              .equals(org.apache.uima.fit.descriptor.ConfigurationParameter.NO_DEFAULT_VALUE)) {
        return null;
      }

      String valueType = getConfigurationParameterType(field);
      boolean isMultiValued = isMultiValued(field);

      if (!isMultiValued) {
        if (ConfigurationParameter.TYPE_BOOLEAN.equals(valueType)) {
          return Boolean.parseBoolean(stringValue[0]);
        } else if (ConfigurationParameter.TYPE_FLOAT.equals(valueType)) {
          return Float.parseFloat(stringValue[0]);
        } else if (ConfigurationParameter.TYPE_DOUBLE.equals(valueType)) {
          return Double.parseDouble(stringValue[0]);
        } else if (ConfigurationParameter.TYPE_INTEGER.equals(valueType)) {
          return Integer.parseInt(stringValue[0]);
        } else if (ConfigurationParameter.TYPE_LONG.equals(valueType)) {
          return Long.parseLong(stringValue[0]);
        } else if (ConfigurationParameter.TYPE_STRING.equals(valueType)) {
          return stringValue[0];
        }
        throw new UIMA_IllegalArgumentException(
                UIMA_IllegalArgumentException.METADATA_ATTRIBUTE_TYPE_MISMATCH,
                new Object[] { valueType, "type" });
      } else {
        if (ConfigurationParameter.TYPE_BOOLEAN.equals(valueType)) {
          Boolean[] returnValues = new Boolean[stringValue.length];
          for (int i = 0; i < stringValue.length; i++) {
            returnValues[i] = Boolean.parseBoolean(stringValue[i]);
          }
          return returnValues;
        } else if (ConfigurationParameter.TYPE_FLOAT.equals(valueType)) {
          Float[] returnValues = new Float[stringValue.length];
          for (int i = 0; i < stringValue.length; i++) {
            returnValues[i] = Float.parseFloat(stringValue[i]);
          }
          return returnValues;
        } else if (ConfigurationParameter.TYPE_DOUBLE.equals(valueType)) {
          Double[] returnValues = new Double[stringValue.length];
          for (int i = 0; i < stringValue.length; i++) {
            returnValues[i] = Double.parseDouble(stringValue[i]);
          }
          return returnValues;
        } else if (ConfigurationParameter.TYPE_INTEGER.equals(valueType)) {
          Integer[] returnValues = new Integer[stringValue.length];
          for (int i = 0; i < stringValue.length; i++) {
            returnValues[i] = Integer.parseInt(stringValue[i]);
          }
          return returnValues;
        } else if (ConfigurationParameter.TYPE_LONG.equals(valueType)) {
          Long[] returnValues = new Long[stringValue.length];
          for (int i = 0; i < stringValue.length; i++) {
            returnValues[i] = Long.parseLong(stringValue[i]);
          }
          return returnValues;
        } else if (ConfigurationParameter.TYPE_STRING.equals(valueType)) {
          return stringValue;
        }
        throw new UIMA_IllegalArgumentException(
                UIMA_IllegalArgumentException.METADATA_ATTRIBUTE_TYPE_MISMATCH,
                new Object[] { valueType, "type" });

      }

    } else {
      throw new IllegalArgumentException("field is not annotated with annotation of type "
              + org.apache.uima.fit.descriptor.ConfigurationParameter.class.getName());
    }
  }

  private static String getConfigurationParameterType(Field field) {
    Class<?> parameterClass = field.getType();
    String parameterClassName;
    if (parameterClass.isArray()) {
      parameterClassName = parameterClass.getComponentType().getName();
    } else if (Collection.class.isAssignableFrom(parameterClass)) {
      ParameterizedType collectionType = (ParameterizedType) field.getGenericType();
      parameterClassName = ((Class<?>) (collectionType.getActualTypeArguments()[0])).getName();
    } else {
      parameterClassName = parameterClass.getName();
    }
    String parameterType = JAVA_UIMA_TYPE_MAP.get(parameterClassName);
    if (parameterType == null) {
      return ConfigurationParameter.TYPE_STRING;
    }
    return parameterType;
  }

  private static boolean isMultiValued(Field field) {
    Class<?> parameterClass = field.getType();
    if (parameterClass.isArray()) {
      return true;
    } else if (Collection.class.isAssignableFrom(parameterClass)) {
      return true;
    }
    return false;
  }

  /**
   * This method generates the default name of a configuration parameter that is defined by an
   * {@link org.apache.uima.fit.descriptor.ConfigurationParameter} annotation when no name is given
   * 
   * @param field
   *          the field to analyze
   * @return the parameter name
   */
  public static String getConfigurationParameterName(Field field) {
    if (isConfigurationParameterField(field)) {
      org.apache.uima.fit.descriptor.ConfigurationParameter annotation = ReflectionUtil
              .getAnnotation(field, org.apache.uima.fit.descriptor.ConfigurationParameter.class);
      String name = annotation.name();
      if (name.equals(org.apache.uima.fit.descriptor.ConfigurationParameter.USE_FIELD_NAME)) {
        name = field.getName();
      }
      return name;
    }
    return null;
  }

  /**
   * A factory method for creating a ConfigurationParameter from a given field definition
   * 
   * @param field
   *          the field to analyze
   * @return the configuration parameter.
   */
  public static ConfigurationParameter createPrimitiveParameter(Field field) {
    if (isConfigurationParameterField(field)) {
      org.apache.uima.fit.descriptor.ConfigurationParameter annotation = ReflectionUtil
              .getAnnotation(field, org.apache.uima.fit.descriptor.ConfigurationParameter.class);
      String name = getConfigurationParameterName(field);
      boolean multiValued = isMultiValued(field);
      String parameterType = getConfigurationParameterType(field);
      return createPrimitiveParameter(name, parameterType, annotation.description(), multiValued,
              annotation.mandatory());
    } else {
      throw new IllegalArgumentException("field is not annotated with annotation of type "
              + org.apache.uima.fit.descriptor.ConfigurationParameter.class.getName());
    }
  }

  /**
   * A factory method for creating a ConfigurationParameter object.
   * 
   * @param name
   *          the parameter name
   * @param parameterClass
   *          the parameter class
   * @param parameterDescription
   *          the parameter description
   * @param isMandatory
   *          whether the parameter is mandatory
   * @return the configuration parameter
   */
  public static ConfigurationParameter createPrimitiveParameter(String name,
          Class<?> parameterClass, String parameterDescription, boolean isMandatory) {
    String parameterClassName;
    if (parameterClass.isArray()) {
      parameterClassName = parameterClass.getComponentType().getName();
    } else {
      parameterClassName = parameterClass.getName();
    }

    String parameterType = JAVA_UIMA_TYPE_MAP.get(parameterClassName);
    if (parameterType == null) {
      // If we cannot map the type, we'll try to convert it to a String
      parameterType = ConfigurationParameter.TYPE_STRING;
    }
    return createPrimitiveParameter(name, parameterType, parameterDescription,
            parameterClass.isArray(), isMandatory);
  }

  /**
   * Convert a value so it can be injected into a UIMA component. UIMA only supports several
   * parameter types. If the value is not of these types, this method can be used to coerce the
   * value into a supported type (typically String). It is also used to convert primitive arrays to
   * object arrays when necessary.
   * 
   * @param param
   *          the configuration parameter.
   * @param aValue
   *          the parameter value.
   * @return the converted value.
   */
  static Object convertParameterValue(ConfigurationParameter param, Object aValue) {
    Object value = aValue;
    if (aValue == null) {
      return null;
    }

    if (value.getClass().isArray() && value.getClass().getComponentType().isPrimitive()) {
      if ("boolean".equals(value.getClass().getComponentType().getName())) {
        return ArrayUtils.toObject((boolean[]) value);
      }

      if ("int".equals(value.getClass().getComponentType().getName())) {
        return ArrayUtils.toObject((int[]) value);
      }

      if ("long".equals(value.getClass().getComponentType().getName())) {
        return ArrayUtils.toObject((long[]) value);
      }

      if ("float".equals(value.getClass().getComponentType().getName())) {
        return ArrayUtils.toObject((float[]) value);
      }

      if ("double".equals(value.getClass().getComponentType().getName())) {
        return ArrayUtils.toObject((double[]) value);
      }
    }

    Class<?> classForParameter = getClassForParameterType(param.getType());
    if (value.getClass().isArray() || value instanceof Collection) {
      classForParameter = Array.newInstance(classForParameter, 0).getClass();
    }

    try {
      SimpleTypeConverter converter = new SimpleTypeConverter();
      PropertyEditorUtil.registerUimaFITEditors(converter);
      value = converter.convertIfNecessary(value, classForParameter);
      return value;
    } catch (TypeMismatchException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

  /**
   * Gets the expected Java class for the given parameter type name.
   * 
   * @param paramType
   *          parameter type name from ConfigurationParameterDeclarations
   * 
   * @return expected Java class for parameter values of this type
   */
  static Class<?> getClassForParameterType(String paramType) {
    if (paramType == null) {
      throw new IllegalArgumentException("Parameter type cannot be null");
    }

    switch (paramType) {
      case ConfigurationParameter.TYPE_STRING:
        return String.class;
      case ConfigurationParameter.TYPE_BOOLEAN:
        return Boolean.class;
      case ConfigurationParameter.TYPE_INTEGER:
        return Integer.class;
      case ConfigurationParameter.TYPE_LONG:
        return Long.class;
      case ConfigurationParameter.TYPE_FLOAT:
        return Float.class;
      case ConfigurationParameter.TYPE_DOUBLE:
        return Double.class;
      default:
        throw new IllegalArgumentException("Unsupported parameter type [" + paramType + "]");
    }
  }

  /**
   * A factory method for creating a ConfigurationParameter object.
   * 
   * @param name
   *          the parameter name
   * @param parameterType
   *          the parameter type
   * @param parameterDescription
   *          the parameter description
   * @param isMultiValued
   *          whether the parameter is multi-valued
   * @param isMandatory
   *          whether the parameter is mandatory
   * @return the configuration parameter
   */
  public static ConfigurationParameter createPrimitiveParameter(String name, String parameterType,
          String parameterDescription, boolean isMultiValued, boolean isMandatory) {
    ConfigurationParameter param = new ConfigurationParameter_impl();
    param.setName(name);
    param.setType(parameterType);
    param.setDescription(parameterDescription);
    param.setMultiValued(isMultiValued);
    param.setMandatory(isMandatory);
    return param;
  }

  /**
   * Analyze a component for parameters and default values, merge that with parameter values
   * specified, potentially adding extra parameters. Set the merged result into the provided
   * descriptor.
   * 
   * @param desc
   *          the descriptor into which to merge the parameters
   * @param componentClass
   *          the component class which will be analyzed for parameters. Must match the
   *          implementationName set in the descriptor.
   * @param configurationParameters
   *          additional parameter names
   * @param configurationValues
   *          additional parameters values
   */
  public static void setParameters(ResourceCreationSpecifier desc, Class<?> componentClass,
          ConfigurationParameter[] configurationParameters, Object[] configurationValues) {
    ConfigurationData reflectedConfigurationData = ConfigurationParameterFactory
            .createConfigurationData(componentClass);
    ResourceCreationSpecifierFactory.setConfigurationParameters(desc,
            reflectedConfigurationData.configurationParameters,
            reflectedConfigurationData.configurationValues);
    if (configurationParameters != null) {
      ResourceCreationSpecifierFactory.setConfigurationParameters(desc, configurationParameters,
              configurationValues);
    }
  }

  /**
   * This method converts configuration data provided as an array of objects and returns a
   * {@link ConfigurationData} object. This should only be used to prepare values supplied in a
   * factory method call for merging with existing parameter declarations, e.g. extracted from a
   * class using {@link #createConfigurationData(Class)}.
   * 
   * @param configurationData
   *          configuration parameters as (name, value) pairs, so there should always be an even
   *          number of parameters.
   * @return the configuration data
   */
  public static ConfigurationData createConfigurationData(Object... configurationData) {
    if (configurationData == null) {
      return new ConfigurationData(new ConfigurationParameter[0], new Object[0]);
    }

    ensureParametersComeInPairs(configurationData);

    int numberOfParameters = configurationData.length / 2;
    List<ConfigurationParameter> configurationParameters = new ArrayList<ConfigurationParameter>();
    List<Object> configurationValues = new ArrayList<Object>();

    for (int i = 0; i < numberOfParameters; i++) {
      String name = (String) configurationData[i * 2];
      Object value = configurationData[i * 2 + 1];

      if (value == null || ExternalResourceFactory
              .getResourceParameterType(value) != ResourceValueType.NO_RESOURCE) {
        continue;
      }

      ConfigurationParameter param = ConfigurationParameterFactory.createPrimitiveParameter(name,
              value.getClass(), null, false);
      configurationParameters.add(param);
      configurationValues.add(ConfigurationParameterFactory.convertParameterValue(param, value));
    }
    return new ConfigurationData(
            configurationParameters
                    .toArray(new ConfigurationParameter[configurationParameters.size()]),
            configurationValues.toArray());
  }

  /**
   * This method creates configuration data for a given class definition using reflection and the
   * configuration parameter annotation
   * 
   * @param componentClass
   *          the class to analyze
   * @return the configuration settings extracted from the class
   */
  public static ConfigurationData createConfigurationData(Class<?> componentClass) {
    List<ConfigurationParameter> configurationParameters = new ArrayList<ConfigurationParameter>();
    List<Object> configurationValues = new ArrayList<Object>();

    for (Field field : ReflectionUtil.getFields(componentClass)) {
      if (ConfigurationParameterFactory.isConfigurationParameterField(field)) {
        configurationParameters.add(ConfigurationParameterFactory.createPrimitiveParameter(field));
        configurationValues.add(ConfigurationParameterFactory.getDefaultValue(field));
      }
    }

    return new ConfigurationData(
            configurationParameters
                    .toArray(new ConfigurationParameter[configurationParameters.size()]),
            configurationValues.toArray(new Object[configurationValues.size()]));
  }

  /**
   * A simple class for storing an array of configuration parameters along with an array of the
   * values that will fill in those configuration parameters
   */
  public static class ConfigurationData {
    public ConfigurationParameter[] configurationParameters;

    public Object[] configurationValues;

    /**
     * @param configurationParameters
     *          the configuration parameters
     * @param configurationValues
     *          the configuration parameter values
     */
    public ConfigurationData(ConfigurationParameter[] configurationParameters,
            Object[] configurationValues) {
      this.configurationParameters = configurationParameters;
      this.configurationValues = configurationValues;
    }

  }

  /**
   * This method adds configuration parameter information to the specifier given the provided
   * configuration data
   * 
   * @param specifier
   *          the specified to add the parameters to
   * @param configurationData
   *          should consist of name value pairs.
   */
  public static void addConfigurationParameters(ResourceCreationSpecifier specifier,
          Object... configurationData) {
    ConfigurationData cdata = ConfigurationParameterFactory
            .createConfigurationData(configurationData);
    ResourceCreationSpecifierFactory.setConfigurationParameters(specifier,
            cdata.configurationParameters, cdata.configurationValues);
  }

  /**
   * Provides a mechanism to add configuration parameter information to a specifier for the given
   * classes. This method may be useful in situations where a class definition has annotated
   * configuration parameters that you want to include in the given specifier.
   * 
   * @param specifier
   *          the specified to add the parameters to
   * @param dynamicallyLoadedClasses
   *          the classes to analyze and extract parameter information from
   */
  public static void addConfigurationParameters(ResourceCreationSpecifier specifier,
          List<Class<?>> dynamicallyLoadedClasses) {
    for (Class<?> dynamicallyLoadedClass : dynamicallyLoadedClasses) {
      ConfigurationData reflectedConfigurationData = ConfigurationParameterFactory
              .createConfigurationData(dynamicallyLoadedClass);
      ResourceCreationSpecifierFactory.setConfigurationParameters(specifier,
              reflectedConfigurationData.configurationParameters,
              reflectedConfigurationData.configurationValues);
    }
  }

  /**
   * Provides a mechanism to add configuration parameter information to a specifier for the given
   * classes. This method may be useful in situations where a class definition has annotated
   * configuration parameters that you want to include in the given specifier
   * 
   * @param specifier
   *          the specified to add the parameters to
   * @param dynamicallyLoadedClasses
   *          the classes to analyze and extract parameter information from
   */
  public static void addConfigurationParameters(ResourceCreationSpecifier specifier,
          Class<?>... dynamicallyLoadedClasses) {
    for (Class<?> dynamicallyLoadedClass : dynamicallyLoadedClasses) {
      ConfigurationData reflectedConfigurationData = ConfigurationParameterFactory
              .createConfigurationData(dynamicallyLoadedClass);
      ResourceCreationSpecifierFactory.setConfigurationParameters(specifier,
              reflectedConfigurationData.configurationParameters,
              reflectedConfigurationData.configurationValues);
    }
  }

  /**
   * Adds a single configuration parameter name value pair to a specifier
   * 
   * @param specifier
   *          the specifier to add the parameter setting to
   * @param name
   *          the name of the parameter
   * @param value
   *          the parameter value
   */
  public static void addConfigurationParameter(ResourceCreationSpecifier specifier, String name,
          Object value) {
    ConfigurationData cdata = ConfigurationParameterFactory.createConfigurationData(name, value);
    ResourceCreationSpecifierFactory.setConfigurationParameters(specifier,
            cdata.configurationParameters, cdata.configurationValues);

  }

  /**
   * Helper method to make sure configuration parameter lists have always pairs of name/values.
   * 
   * @param configurationData
   *          the configuration parameters.
   */
  static void ensureParametersComeInPairs(Object[] configurationData) {
    if (configurationData != null && configurationData.length % 2 != 0) {
      throw new IllegalArgumentException(
              "Parameter arguments have to " + "come in key/value pairs, but found odd number of "
                      + "arguments [" + configurationData.length + "]");
    }
  }

  /**
   * Fetches the parameter settings from the given resource specifier.
   * 
   * @param spec
   *          a resource specifier.
   * @return the parameter settings.
   */
  public static Map<String, Object> getParameterSettings(ResourceSpecifier spec) {
    Map<String, Object> settings = new HashMap<String, Object>();
    if (spec instanceof CustomResourceSpecifier) {
      for (Parameter p : ((CustomResourceSpecifier) spec).getParameters()) {
        settings.put(p.getName(), p.getValue());
      }
    } else if (spec instanceof PearSpecifier) {
      PearSpecifier pearSpec = ((PearSpecifier) spec);
      // Legacy parameters that only support string values.
      Parameter[] parameters = pearSpec.getParameters();

      if (parameters != null) {
        for (Parameter parameter : parameters) {
          settings.put(parameter.getName(), parameter.getValue());
        }
      }

      // Parameters supporting arbitrary objects as values
      NameValuePair[] pearParameters = pearSpec.getPearParameters();

      if (pearParameters != null) {
        for (NameValuePair pearParameter : pearParameters) {
          settings.put(pearParameter.getName(), pearParameter.getValue());
        }
      }
    } else if (spec instanceof ResourceCreationSpecifier) {
      for (NameValuePair p : ((ResourceCreationSpecifier) spec).getMetaData()
              .getConfigurationParameterSettings().getParameterSettings()) {
        settings.put(p.getName(), p.getValue());
      }
    } else if (spec instanceof ConfigurableDataResourceSpecifier) {
      for (NameValuePair p : ((ResourceCreationSpecifier) spec).getMetaData()
              .getConfigurationParameterSettings().getParameterSettings()) {
        settings.put(p.getName(), p.getValue());
      }
    } else {
      throw new IllegalArgumentException(
              "Unsupported resource specifier class [" + spec.getClass() + "]");
    }
    return settings;
  }

  /**
   * Sets the specified parameter in the given resource specifier. If the specified is a
   * {@link CustomResourceSpecifier} an exception is thrown if the parameter value not a String.
   * 
   * @param aSpec
   *          a resource specifier.
   * @param name
   *          the parameter name.
   * @param value
   *          the parameter value.
   * @throws IllegalArgumentException
   *           if the value is not of a supported type for the given specifier.
   */
  public static void setParameter(ResourceSpecifier aSpec, String name, Object value) {
    if (aSpec instanceof CustomResourceSpecifier) {
      if (!(value instanceof String || value == null)) {
        throw new IllegalArgumentException("Value must be a string");
      }
      CustomResourceSpecifier spec = (CustomResourceSpecifier) aSpec;

      // If the parameter is already there, update it
      boolean found = false;
      for (Parameter p : spec.getParameters()) {
        if (p.getName().equals(name)) {
          p.setValue((String) value);
          found = true;
        }
      }

      // If the parameter is not there, add it
      if (!found) {
        Parameter[] params = new Parameter[spec.getParameters().length + 1];
        System.arraycopy(spec.getParameters(), 0, params, 0, spec.getParameters().length);
        params[params.length - 1] = new Parameter_impl();
        params[params.length - 1].setName(name);
        params[params.length - 1].setValue((String) value);
        spec.setParameters(params);
      }
    } else if (aSpec instanceof PearSpecifier) {
      PearSpecifier spec = (PearSpecifier) aSpec;

      boolean found = false;

      // Check modern parameters and if the parameter is present there, update it
      NameValuePair[] parameters = spec.getPearParameters();
      for (NameValuePair p : parameters) {
        if (p.getName().equals(name)) {
          p.setValue(value);
          found = true;
        }
      }

      // Check legacy parameters and if the parameter is present there, update it
      Parameter[] legacyParameters = spec.getParameters();
      if (legacyParameters != null) {
        for (Parameter p : legacyParameters) {
          if (p.getName().equals(name)) {
            p.setValue((String) value);
            found = true;
          }
        }
      }

      // If the parameter is not there, add it
      if (!found) {
        NameValuePair[] params = new NameValuePair[parameters.length + 1];
        System.arraycopy(parameters, 0, params, 0, parameters.length);
        params[params.length - 1] = new NameValuePair_impl();
        params[params.length - 1].setName(name);
        params[params.length - 1].setValue(value);
        spec.setPearParameters(params);
      }
    } else if (aSpec instanceof ResourceCreationSpecifier) {
      ResourceMetaData md = ((ResourceCreationSpecifier) aSpec).getMetaData();

      ConfigurationParameter param = md.getConfigurationParameterDeclarations()
              .getConfigurationParameter(null, name);
      if (param == null) {
        throw new IllegalArgumentException("Cannot set undeclared parameter [" + name + "]");
      }

      md.getConfigurationParameterSettings().setParameterValue(name,
              convertParameterValue(param, value));
    } else if (aSpec instanceof ConfigurableDataResourceSpecifier) {
      ResourceMetaData md = ((ConfigurableDataResourceSpecifier) aSpec).getMetaData();

      ConfigurationParameter param = md.getConfigurationParameterDeclarations()
              .getConfigurationParameter(null, name);
      if (param == null) {
        throw new IllegalArgumentException("Cannot set undeclared parameter [" + name + "]");
      }

      md.getConfigurationParameterSettings().setParameterValue(name,
              convertParameterValue(param, value));
    } else {
      throw new IllegalArgumentException(
              "Unsupported resource specifier class [" + aSpec.getClass() + "]");
    }
  }

  /**
   * Check if the given parameter can be set on the provided specifier. Some specifier types require
   * parameters to be declared before they can be set.
   * 
   * @param aSpec
   *          the specifier to test
   * @param name
   *          the parameter to be tested
   * @return if the parameter can be set
   */
  public static boolean canParameterBeSet(ResourceSpecifier aSpec, String name) {
    if (aSpec instanceof CustomResourceSpecifier) {
      return true;
    } else if (aSpec instanceof ResourceCreationSpecifier) {
      ResourceMetaData md = ((ResourceCreationSpecifier) aSpec).getMetaData();
      return md.getConfigurationParameterDeclarations().getConfigurationParameter(null,
              name) != null;
    } else if (aSpec instanceof ConfigurableDataResourceSpecifier) {
      ResourceMetaData md = ((ConfigurableDataResourceSpecifier) aSpec).getMetaData();
      return md.getConfigurationParameterDeclarations().getConfigurationParameter(null,
              name) != null;
    } else {
      return false;
    }
  }
}
