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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.ExternalOverrideSettings;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.impl.ExternalOverrideSettings_impl;
import org.apache.uima.util.Level;

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
   */
  private Map<String, Object> mSharedParamMap = Collections.synchronizedMap(new HashMap<String, Object>());

  /**
   * External Overrides when top-level engine has the root context ... map not needed.
   */
  private ExternalOverrideSettings mRootSettings;
  
  /**
   * Map of External Overrides when have multiple top-level engines, e.g. under CPE
   */
  private Map<String,ExternalOverrideSettings> mSettingsMap;
  
  /**
   * Set the External Overrides Settings object for the specified top-level context
   * 
   * @param topContextName
   * @param aSettings
   */
  public void setExternalOverrideSettings(String topContextName, ExternalOverrideSettings aSettings) {
    mSettingsMap.put(topContextName, aSettings);
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.impl.ConfigurationManagerImplBase#declareParameters(java.lang.String,
   *      org.apache.uima.resource.metadata.ConfigurationParameter[],
   *      org.apache.uima.resource.metadata.ConfigurationParameterSettings, java.lang.String,
   *      java.lang.String)
   */
  protected void declareParameters(String aGroupName, ConfigurationParameter[] aParams,
          ConfigurationParameterSettings aSettings, String aContextName, String aParentContextName) {
    super.declareParameters(aGroupName, aParams, aSettings, aContextName, aParentContextName);
    // iterate over config. param _declarations_ and build mSharedParamNap
    if (aParams != null) {

      ExternalOverrideSettings settings = getExternalOverrideSettings(aContextName);
      for (int i = 0; i < aParams.length; i++) {
        String qname = makeQualifiedName(aContextName, aParams[i].getName(), aGroupName);
        String from = "";

        // get the actual setting and store it in the Map (even if it's a null value)
        // If it has an external name that has been set, use it instead and remove any link
        // to an overriding parameter. The external name in a delegate takes precedence,
        // even over an external name in the aggregate.
        Object paramValue = aSettings.getParameterValue(aGroupName, aParams[i].getName());
        String extName = aParams[i].getExternalOverrideName();
        if (extName != null && settings != null) {
          String propValue = settings.resolveExternalName(extName);
          if (propValue != null) {
            Object result = createParam(propValue, aParams[i].getType(), aParams[i].isMultiValued());
            if (result != null) {
              paramValue = result;
              mLinkMap.remove(qname);
              from = "(overridden from " + extName + ")"; 
            }
          }
        }
        mSharedParamMap.put(qname, paramValue);
        
        // Log parameter & value & how it was found ... could do when validate them?
        Object realValue = paramValue;
        if (from.length() == 0) {
          String linkedTo = qname;
          while (getLink(linkedTo) != null) {
            linkedTo = getLink(linkedTo);
          }
          if (linkedTo != qname && lookup(linkedTo) != null) {
            realValue = lookup(linkedTo);
            from = "(overridden from " + linkedTo + ")";
          }
        }
        if (realValue == null) {
          continue;
        }
        if (aParams[i].isMultiValued()) {
          Object[] array = (Object[]) realValue;
          realValue = Arrays.toString(array);
        }
        UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
                "declareParameters", LOG_RESOURCE_BUNDLE, "UIMA_parameter_set__CONFIG",
                new Object[] { aParams[i].getName(), aContextName, realValue, from });
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
  private Object createParam(String value, String paramType, boolean isArray) throws NumberFormatException {
    try {
      if (paramType.equals(ConfigurationParameter.TYPE_BOOLEAN)) {
        return createParamForClass(value, isArray, Boolean.class);
      } else if (paramType.equals(ConfigurationParameter.TYPE_INTEGER)) {
        return createParamForClass(value, isArray, Integer.class);
      } else if (paramType.equals(ConfigurationParameter.TYPE_FLOAT)) {
        return createParamForClass(value, isArray, Float.class);
      } else { // Must be a string 
        return createParamForClass(value, isArray, String.class);
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new NumberFormatException("Failed to convert " + value + " to " + paramType);
    }
  }
  
  // String does not have a valueOf(String) method so use this trivial class instead
  static class StringX {
    public static String valueOf(String s) {
      return s;
    }
  }

  /*
   * Convert the string to the appropriate object, or array of.
   * Suppress the warnings about the casts.
   */
  @SuppressWarnings("unchecked")
  private <T> Object createParamForClass(String value, boolean isArray, Class<T> clas) throws Exception {
    Method valOf = null;
    try {
      valOf = clas.getMethod("valueOf", String.class);
    } catch (NoSuchMethodException e) {
      valOf = StringX.class.getMethod("valueOf", String.class);
    }
    if (isArray) {
      String[] tokens = value.split(",");   // NOTE: could improve and allow escape chars
      T[] result = (T[]) Array.newInstance(clas, tokens.length);
      for (int i = 0; i < tokens.length; ++i) {
        result[i] = (T) valOf.invoke(null, tokens[i].trim());
      }
      return result;
    } else {
      return valOf.invoke(null, value);
    }
  }

  /**
   * If the first Analysis Engine load the External Override Settings
   * May start with the root context "/" or may get multiple top-level contexts (e.g. from CPE)
   * 
   * @param contextName for this engine, "/" or "/name/" or "/name/name/..."
   * @param metadata
   * @param resourceManager
   * @throws ResourceConfigurationException
   */
  public void setupExternalOverrideSettings(String contextName, ResourceMetaData metadata,
          ResourceManager resourceManager) throws ResourceConfigurationException {
    // If have already create a root entry then quit smartly
    if (mRootSettings != null) {
        return;         // Setup already done
    }
    ExternalOverrideSettings eos;
    if (contextName.length() > 1) {
      contextName = contextName.split("/")[1]; // Use part up to 2nd slash
      if (mSettingsMap != null) {
        eos = mSettingsMap.get(contextName);
        if (eos != null) {
          return;         // Setup already done
        }
      } else {
        mSettingsMap = new ConcurrentHashMap<String, ExternalOverrideSettings>();
      }
    }

    // Must be creating the first Analysis Engine for this context
    eos = ((AnalysisEngineMetaData)metadata).getOperationalProperties().getExternalOverrideSettings();
    if (eos == null) {
      // Create empty one when none provided in the top descriptor
      // ??? with: eos = UIMAFramework.getResourceSpecifierFactory().createExternalOverrideSettings(); 
      eos = new ExternalOverrideSettings_impl();  // Simpler than using the factory
    }
    eos.resolveImports(resourceManager);

    // Save setup results
    if (contextName.equals("/")) {
      mRootSettings = eos;
    } else {
      mSettingsMap.put(contextName, eos);
    }
  }

  /**
   * Get External Overrides Settings object for the specified context
   * If don't have a root value then should have a map and the context must be at least "/name/"
   * but setup will not have been called if a non-AE resource
   * 
   * @param aContextName
   * @return settings (should not be null as called after parameters setup)
   */
  private ExternalOverrideSettings getExternalOverrideSettings(String aContextName) {
    if (mRootSettings != null) {
      return mRootSettings;
    }
    if (mSettingsMap == null) {
      return null;
    }
    aContextName = aContextName.split("/")[1]; 
    return mSettingsMap.get(aContextName);
  }

  public String getExternalParameter(String context, String name) {
    ExternalOverrideSettings settings = getExternalOverrideSettings(context);
    return settings == null ? null : settings.resolveExternalName(name);
  }
}
