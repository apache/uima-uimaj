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

package org.apache.uima.resource.impl;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.util.Level;
import org.apache.uima.util.Settings;

/**
 * Basic standalone Configuration Manager implmentation.
 * 
 */
public class ConfigurationManager_impl extends ConfigurationManagerImplBase {

  /**
   * resource bundle for log messages
   */
  protected static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";
  
  /**
   * Map containing configuration parameter values and links for parameter values shared by all
   * sessions.
   * 
   * Can't (currently) be a concurrentHashMap because it stores nulls
   */
  private Map<String, Object> mSharedParamMap = Collections.synchronizedMap(new HashMap<String, Object>());

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.impl.ConfigurationManagerImplBase#declareParameters(java.lang.String,
   * org.apache.uima.resource.metadata.ConfigurationParameter[],
   * org.apache.uima.resource.metadata.ConfigurationParameterSettings, java.lang.String, java.lang.String)
   */
  protected void declareParameters(String aGroupName, ConfigurationParameter[] aParams,
          ConfigurationParameterSettings aSettings, String aContextName, Settings aExternalOverrides)
          throws ResourceConfigurationException {
    super.declareParameters(aGroupName, aParams, aSettings, aContextName, aExternalOverrides);
    // iterate over config. param _declarations_ and build mSharedParamNap
    if (aParams != null) {
      for (int i = 0; i < aParams.length; i++) {
        String qname = makeQualifiedName(aContextName, aParams[i].getName(), aGroupName);
        String from = "";

        // get the actual setting and store it in the Map (even if it's a null value)
        // If it has an external name that has been set, use it instead and remove any link
        // to an overriding parameter. The external name in a delegate takes precedence,
        // even over an external name in the aggregate.
        Object paramValue = aSettings.getParameterValue(aGroupName, aParams[i].getName());
        String extName = aParams[i].getExternalOverrideName();
        if (extName != null && aExternalOverrides != null) {
          if (aParams[i].isMultiValued()) {
            String[] propValues = aExternalOverrides.getSettingArray(extName);
            if (propValues != null) {
              paramValue = createParams(propValues, aParams[i].getType());
              mLinkMap.remove(qname);
              from = "(overridden from " + extName + ")";
            }
          } else {
            String propValue = aExternalOverrides.getSetting(extName);
            if (propValue != null) {
              paramValue = createParam(propValue, aParams[i].getType());
              mLinkMap.remove(qname);
              from = "(overridden from " + extName + ")";
            }
          }
        }
        mSharedParamMap.put(qname, paramValue);
        
        // Log parameter & value & how it was found ... could do when validate them?
        if (UIMAFramework.getLogger(this.getClass()).isLoggable(Level.CONFIG)) {
          Object realValue = paramValue;
          if (from.length() == 0) {
            String linkedTo = qname;
            while (getLink(linkedTo) != null) {
              linkedTo = getLink(linkedTo);
            }
            if ((!linkedTo.equals(qname)) && lookup(linkedTo) != null) {
              realValue = lookup(linkedTo);
              from = "(overridden from " + linkedTo + ")";
            }
          }
          if (realValue == null) {
            continue;
          }
          if (aParams[i].isMultiValued()) {
            try {
              Object[] array = (Object[]) realValue;
              realValue = Arrays.toString(array);
            } catch (ClassCastException e) {
              // Ignore errors caused by failure to validate parameter settings (see Jira 3123)
            }
          }
          UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(), "declareParameters",
                  LOG_RESOURCE_BUNDLE, "UIMA_parameter_set__CONFIG",
                  new Object[] { aParams[i].getName(), aContextName, realValue, from });
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.impl.ConfigurationManagerImplBase#lookupSharedParamNoLinks(java.lang.String)
   */
  protected Object lookupSharedParamNoLinks(String aCompleteName) {
    return mSharedParamMap.get(aCompleteName);
  }
  
  /*
   * Create the appropriate type of parameter object from the value of the external override 
   */
  private Object createParam(String value, String paramType) throws ResourceConfigurationException {
    if (paramType.equals(ConfigurationParameter.TYPE_BOOLEAN)) {
      return createParamForClass(value, Boolean.class);
    } else if (paramType.equals(ConfigurationParameter.TYPE_INTEGER)) {
      return createParamForClass(value, Integer.class);
    } else if (paramType.equals(ConfigurationParameter.TYPE_FLOAT)) {
      return createParamForClass(value, Float.class);
    } else { // Must be a string
      return value;
    }
  }
  
  private Object createParams(String[] values, String paramType) {
    if (paramType.equals(ConfigurationParameter.TYPE_BOOLEAN)) {
      return createParamsForClass(values, Boolean.class);
    } else if (paramType.equals(ConfigurationParameter.TYPE_INTEGER)) {
      return createParamsForClass(values, Integer.class);
    } else if (paramType.equals(ConfigurationParameter.TYPE_FLOAT)) {
      return createParamsForClass(values, Float.class);
    } else { // Must be a string
      return values;
    }
  }
  
  /*
   * Convert the string to the appropriate object
   */
  private <T> Object createParamForClass(String value, Class<T> clas) throws ResourceConfigurationException {
    Method valOf;
    try {
      valOf = clas.getMethod("valueOf", String.class);
      return valOf.invoke(null, value);
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      if (e.getCause() instanceof NumberFormatException) {
        // External override value "{0}" is not an integer
        throw new ResourceConfigurationException(ResourceConfigurationException.EXTERNAL_OVERRIDE_NUMERIC_ERROR, 
                new Object[] { value });
      }
      e.printStackTrace();
      throw new ResourceConfigurationException(e);
    }

  }
  
  /*
   * Convert the array of strings to the appropriate array of objects.
   * Suppress the warnings about the casts.
   */
  @SuppressWarnings("unchecked")
  private <T> Object createParamsForClass(String[] values, Class<T> clas) {
    Method valOf;
    try {
      valOf = clas.getMethod("valueOf", String.class);
      T[] result = (T[]) Array.newInstance(clas, values.length);
      for (int i = 0; i < values.length; ++i) {
        result[i] = (T) valOf.invoke(null, values[i]);
      }
      return result;
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException
            | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalArgumentException(e.getCause());
    }
  }
  
}
