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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.resource.ConfigurationManager;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.Session;
import org.apache.uima.resource.metadata.ConfigurationGroup;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.Settings;

/**
 * Convenience base class for Configuration Manager implementations. Subclasses just need to
 * implement the abstract methods
 * {@link #declareParameters(String, ConfigurationParameter[], ConfigurationParameterSettings, String, Settings)}
 * and {@link #lookupSharedParamNoLinks(String)}.
 * 
 * 
 */
public abstract class ConfigurationManagerImplBase implements ConfigurationManager {
  /**
   * Character that separates parameter name from group name in the parameter map.
   */
  protected static final char GROUP_SEPARATOR = '$';

  /**
   * Key under which to store configuration information in the Session object.
   */
  protected static final String SESSION_CONFIGURATION_KEY = "config";

  /**
   * Map from context name to ConfigurationParameterDeclarations for that context.
   * Not sync'd based on belief:
   *   setup of values must be complete before any reference occurs, even in multi-threaded context.
   *   The setup is done under a sync'd control to insure only one setup is done, and to
   *   publish the updated results to other threads
   */
  final private Map<String, ConfigurationParameterDeclarations> mContextNameToParamDeclsMap = 
      new HashMap<String, ConfigurationParameterDeclarations>();

  /**
   * Map the fully-qualified name of a parameter to the fully-qualified name of the parameter it is
   * linked to (from which it takes its value).
   * Not sync'd based on belief:
   *   setup of values must be complete before any reference occurs, even in multi-threaded context.
   *   The setup is done under a sync'd control to insure only one setup is done
   * 
   */
  final protected Map<String, String> mLinkMap = new HashMap<String, String>();

  /**
   * Set of parameters (fully qualified names) that explicitly declare overrides. This is used to
   * prevent implicit (name-based) overrides for these parameters.
   */
//  final private Set<String> mExplicitlyOverridingParameters = new HashSet<String>();

  /**
   * Current session. Used to store parameter settings done by the
   * settings via API tae.setConfigParameterValue(...)
   * 
   * can be set by multiple threads, but ought to be set to the same session object
   */
  private volatile Session mSession = null;

//  /**
//   * Holds the externalOverrideSettings from the top-level Analysis Engine
//   */
//  protected OperationalProperties mOperationalProperties = null;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ConfigurationManager#setSession(org.apache.uima.resource.Session)
   */
  public void setSession(Session aSession) {
    mSession = aSession;
  }
         
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ConfigurationManager#createContext(java.lang.String,
   *      org.apache.uima.resource.metadata.ResourceMetaData)
   *      
   * Could be called multiple times on different threads - first one does the context creation
   */
  public synchronized void createContext(String aContextName, ResourceMetaData aResourceMetaData, Settings externalOverrides)
          throws ResourceConfigurationException {
    if (mContextNameToParamDeclsMap.containsKey(aContextName)) {
      return;
    }
    // first internally validate settings in the ResourceMetaData (catches data type problems,
    // settings for undefined parameters, etc.)
    aResourceMetaData.validateConfigurationParameterSettings();

    // Iterate through all declared parameters, calling abstract method to allow
    // concrete ConfigurationManager implementations to set up data structures to
    // provide access to the parameter values
    ConfigurationParameterDeclarations paramDecls = aResourceMetaData
            .getConfigurationParameterDeclarations();
    ConfigurationParameterSettings settings = aResourceMetaData.getConfigurationParameterSettings();

    // parameters in no group
    ConfigurationParameter[] paramsInNoGroup = paramDecls.getConfigurationParameters();
    if (paramsInNoGroup.length > 0) // group-less parameters
    {
      declareParameters(null, paramsInNoGroup, settings, aContextName, externalOverrides);
    }

    // parameter groups
    ConfigurationGroup[] groups = paramDecls.getConfigurationGroups();
    if (groups != null) {
      for (int i = 0; i < groups.length; i++) {
        String[] names = groups[i].getNames();
        {
          for (int j = 0; j < names.length; j++) {
            // common params
            ConfigurationParameter[] commonParams = paramDecls.getCommonParameters();
            if (commonParams != null) {
              declareParameters(names[j], commonParams, settings, aContextName, externalOverrides);
            }
            // params in group
            ConfigurationParameter[] params = groups[i].getConfigurationParameters();
            if (params != null) {
              declareParameters(names[j], params, settings, aContextName, externalOverrides);
            }
          }
        }
      }
    }

    // store parameter declarations in map for later access
    mContextNameToParamDeclsMap.put(aContextName, paramDecls);

    // validate
    validateConfigurationParameterSettings(aContextName);
  }
  
  

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ConfigurationManager#getConfigParameterValue(java.lang.String)
   */
  public Object getConfigParameterValue(String aQualifiedParameterName) {
    // try to look up parameter in no group
    Object val = lookup(aQualifiedParameterName);

    if (val != null) {
      return val;
    }

    // if that fails, look up in default group if one is defined
    String defaultGroup = null;
    ConfigurationParameterDeclarations decls = mContextNameToParamDeclsMap
            .get(computeParentContextName(aQualifiedParameterName));
    if (decls != null) {
      defaultGroup = decls.getDefaultGroupName();
    }
    if (defaultGroup != null) {
      return getConfigParameterValue(aQualifiedParameterName, defaultGroup);
    } else {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ConfigurationManager#getConfigParameterValue(java.lang.String,java.lang.String)
   */
  public Object getConfigParameterValue(String aQualifiedParameterName, String aGroupName) {
    // get parameter search strategy for this context
    ConfigurationParameterDeclarations decls = mContextNameToParamDeclsMap
            .get(computeParentContextName(aQualifiedParameterName));
    if (decls != null) {
      return getConfigParameterValue(aQualifiedParameterName, aGroupName,
              decls.getSearchStrategy(), decls.getDefaultGroupName());
    } else {
      return getConfigParameterValue(aQualifiedParameterName, aGroupName, null, null);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ConfigurationManager#setConfigParameterValue(java.lang.String,
   *      java.lang.Object)
   */
  public void setConfigParameterValue(String aQualifiedParamName, Object aValue) {
    // see if there is the specified parameter is linked; if so, set the linked parameter instead
    // String linkedTo = getLink(aQualifiedParamName);
    // String paramName = linkedTo != null ? linkedTo : aQualifiedParamName;
    // setSessionParam(paramName, aValue);
    setSessionParam(aQualifiedParamName, aValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ConfigurationManager#setConfigParameterValue(java.lang.String,
   *      java.lang.String, java.lang.Object)
   */
  public void setConfigParameterValue(String aQualifiedParamName, String aGroupName, Object aValue) {
    if (aGroupName == null) {
      setConfigParameterValue(aQualifiedParamName, aValue);
    } else {
      String completeName = aQualifiedParamName + GROUP_SEPARATOR + aGroupName;
      // see if there is the specified parameter is linked; if so, set the linked parameter instead
      // String linkedTo = getLink(completeName);
      // String paramName = linkedTo != null ? linkedTo : completeName;
      // setSessionParam(paramName, aValue);
      setSessionParam(completeName, aValue);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ConfigurationManager#reconfigure(java.lang.String,
   *      org.apache.uima.resource.metadata.ConfigurationParameterDeclarations)
   */
  public void reconfigure(String aContextName) throws ResourceConfigurationException {
    // This ConfigurationManager implementation sets parameter immediately on the calls to
    // setConfigParameterValue.
    // This method only does validation
    this.validateConfigurationParameterSettings(aContextName);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ConfigurationManager#getConfigParameterDeclarations(java.lang.String)
   */
  public ConfigurationParameterDeclarations getConfigParameterDeclarations(String aContextName) {
    return mContextNameToParamDeclsMap.get(aContextName);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ConfigurationManager#getCurrentConfigParameterSettings(java.lang.String,
   *      org.apache.uima.resource.metadata.ConfigurationParameterDeclarations)
   */
  public ConfigurationParameterSettings getCurrentConfigParameterSettings(String aContextName) {
    // get declarations
    ConfigurationParameterDeclarations decls = mContextNameToParamDeclsMap
            .get(aContextName);

    ConfigurationParameterSettings settings = UIMAFramework.getResourceSpecifierFactory()
            .createConfigurationParameterSettings();

    ConfigurationParameter[] paramsInNoGroup = decls.getConfigurationParameters();
    if (paramsInNoGroup.length > 0) // no groups declared
    {
      settings.setParameterSettings(getParamSettings(null, paramsInNoGroup, aContextName));
    } else
    // groups declared
    {
      ConfigurationGroup[] groups = decls.getConfigurationGroups();
      if (groups != null) {
        for (int i = 0; i < groups.length; i++) {
          String[] names = groups[i].getNames();
          {
            for (int j = 0; j < names.length; j++) {
              // common params
              NameValuePair[] commonParamSettings = getParamSettings(names[j], decls
                      .getCommonParameters(), aContextName);
              NameValuePair[] specificParamSettings = getParamSettings(names[j], groups[i]
                      .getConfigurationParameters(), aContextName);
              NameValuePair[] mergedSettings = new NameValuePair[commonParamSettings.length
                      + specificParamSettings.length];
              System.arraycopy(commonParamSettings, 0, mergedSettings, 0,
                      commonParamSettings.length);
              System.arraycopy(specificParamSettings, 0, mergedSettings,
                      commonParamSettings.length, specificParamSettings.length);
              settings.getSettingsForGroups().put(names[j], mergedSettings);
            }
          }
        }
      }
    }

    return settings;
  }

  /**
   * Does a direct lookup of a complete name, including the group. Follows links but does not do any
   * fallback processing.  An external name definition overrides all
   * 
   * @param aCompleteName
   *          complete name, of the form context/parameter$group
   * 
   * @return value of parameter, null if none
   */
  protected Object lookup(String aCompleteName) {
    // resolve link
    String linkedTo = getLink(aCompleteName);
    if (linkedTo != null) {
      Object val = lookup(linkedTo);
      if (val != null) {
        return val;
      }
    }
    // there is no overriding value, so look up the parameter directly
    // look up in session param map first
    Object val = getSessionParam(aCompleteName);
    // if null use shared param map
    if (val == null) {
      val = lookupSharedParamNoLinks(aCompleteName);
    }
    return val;
  }

  /**
   * Called during creation of a new context. Declares parameters, optionally in a group. Concrete
   * subclasses will likely want to override this to set up any necessary data structures.
   * 
   * @param aGroupName
   *          name of parameter group, null if none
   * @param aParams
   *          parameter declarations
   * @param aSettings
   *          settings for parameters
   * @param aContextName
   *          name of context containing this parameter
   * @param aExternalOverrides
   *          settings for parameters with external overrides 
   * @throws ResourceConfigurationException passthru
   */
  protected void declareParameters(String aGroupName, ConfigurationParameter[] aParams,
          ConfigurationParameterSettings aSettings, String aContextName, Settings aExternalOverrides)
          throws ResourceConfigurationException {
    // iterate over config. param _declarations_
    if (aParams != null) {
      for (int i = 0; i < aParams.length; i++) {
        ConfigurationParameter param = aParams[i];
        String qname = makeQualifiedName(aContextName, param.getName(), aGroupName);
        // if this parameter explicitly overrides others, enter those parameter links in the map
        String[] overrides = param.getOverrides();
        for (int j = 0; j < overrides.length; j++) {
          mLinkMap.put(makeQualifiedName(aContextName, overrides[j], aGroupName), qname);
        }
      }
    }
  }

  /**
   * Gets the name of the parameter to which a given parameter is linked.
   * 
   * @param aCompleteName
   *          complete name, of the form context/parameter$group
   * 
   * @return if the parameter with name <code>aCompleteName</code> is linked to another parameter,
   *         the complete name of that parameter is returned. Otherwise, null is returned.
   */
  protected String getLink(String aCompleteName) {
    return mLinkMap.get(aCompleteName);
  }

  /**
   * Looks up the value of a shared parameter, but does NOT follow links. Concrete subclasses must
   * implement this to do the actual retrieval of configuration parameter values.
   * 
   * @param aCompleteName
   *          complete name, of the form context/parameter$group
   * 
   * @return value of parameter, or null if no value assigned
   */
  protected abstract Object lookupSharedParamNoLinks(String aCompleteName);

  /**
   * Utility method that gets a NameValuePair array containing the specific parameter settings.
   * 
   * @param aGroupName
   *          name of group containing params, may be null
   * @param aParams
   *          array of parameters to look up
   * @param aContextName
   *          context containing parameters
   * 
   * @return array containing settings of the specific parameters
   */
  private NameValuePair[] getParamSettings(String aGroupName, ConfigurationParameter[] aParams,
          String aContextName) {
    List<NameValuePair> result = new ArrayList<NameValuePair>();
    // iterate over config. param _declarations_
    if (aParams != null) {
      for (int i = 0; i < aParams.length; i++) {
        ConfigurationParameter param = aParams[i];
        NameValuePair nvp = UIMAFramework.getResourceSpecifierFactory().createNameValuePair();
        nvp.setName(param.getName());
        // look up value in context
        String qualifiedName = this.makeQualifiedName(aContextName, param.getName(), aGroupName);
        nvp.setValue(this.lookup(qualifiedName));
        result.add(nvp);
      }
    }
    NameValuePair[] resultArr = new NameValuePair[result.size()];
    result.toArray(resultArr);
    return resultArr;
  }

  /**
   * Creates a qualified parameter name. This consists of the context name followed by the param
   * name (separate by a slash), followed by a $ character and the group name (if a group is
   * specified).
   * 
   * @param aContextName
   *          the name of the context containing this parameter
   * @param aParamName
   *          the configuration parameter name
   * @param aGroupName
   *          the name of the group containining the parameter, null if none
   * 
   * @return the qualified parameter name
   */
  protected String makeQualifiedName(String aContextName, String aParamName, String aGroupName) {
    String name = aContextName + aParamName;
    if (aGroupName != null) {
      name += GROUP_SEPARATOR + aGroupName;
    }
    return name;
  }

  /**
   * Get the name of the parent of the given context
   * 
   * @param aContextName
   *          context name
   * 
   * @return name of parent context
   */
  private String computeParentContextName(String aContextName) {
    String nameWithoutSlash = aContextName.substring(0, aContextName.length() - 1);
    int lastSlash = nameWithoutSlash.lastIndexOf('/');
    if (lastSlash == -1) // root context
    {
      return null;
    } else {
      return aContextName.substring(0, lastSlash + 1);
    }
  }

  /**
   * Validates configuration parameter settings. This method checks to make sure that all required
   * parameters have values and that the values are of the correct types.
   * 
   * @throws ResourceConfigurationException
   *           if the configuration parameter settings are invalid
   */
  private void validateConfigurationParameterSettings(String aContext)
          throws ResourceConfigurationException {
    // get declarations
    ConfigurationParameterDeclarations decls = mContextNameToParamDeclsMap
            .get(aContext);
    // check that all required parameters have values
    ConfigurationParameter[] params = decls.getConfigurationParameters();
    if (params.length > 0) {
      // check for mandatory values
      validateConfigurationParameterSettings(aContext, params, null);
    } else {
      ConfigurationParameter[] commonParams = decls.getCommonParameters();
      ConfigurationGroup[] groups = decls.getConfigurationGroups();
      if (groups != null) {
        for (int i = 0; i < groups.length; i++) {
          // check for mandatory values
          ConfigurationParameter[] paramsInGroup = groups[i].getConfigurationParameters();
          String[] names = groups[i].getNames();
          for (int j = 0; j < names.length; j++) {
            if (paramsInGroup != null) {
              validateConfigurationParameterSettings(aContext, paramsInGroup, names[j]);
            }
            if (commonParams != null) {
              validateConfigurationParameterSettings(aContext, commonParams, names[j]);
            }
          }
        }
      }
    }
  }

  /**
   * Validates configuration parameter settings within a group.
   * 
   * @param aContext
   *          name of context containing the parameter settings
   * @param aParams
   *          parameter declarations
   * @param aGroup
   *          name of group to validate, null if none
   * 
   * @throws ResourceConfigurationException
   *           if the configuration parameter settings are invalid
   */
  private void validateConfigurationParameterSettings(String aContext,
          ConfigurationParameter[] aParams, String aGroupName)
          throws ResourceConfigurationException {
    for (int i = 0; i < aParams.length; i++) {
      // get value
      Object val = this.getConfigParameterValue(aContext + aParams[i].getName(), aGroupName);
      // is required value missing?
      if (val == null && aParams[i].isMandatory()) {
        if (aGroupName != null) {
          throw new ResourceConfigurationException(
                  ResourceConfigurationException.MANDATORY_VALUE_MISSING_IN_GROUP, new Object[] {
                      aParams[i].getName(), aGroupName, aContext });
        } else {
          throw new ResourceConfigurationException(
                  ResourceConfigurationException.MANDATORY_VALUE_MISSING, new Object[] {
                      aParams[i].getName(), aContext });
        }
      }
      // check datatype
      validateConfigurationParameterDataTypeMatch(aParams[i], val, aContext);
    }
  }

  /**
   * Validate that a value is of an appropriate data type for assignment to the given parameter.
   * 
   * @param aParam
   *          configuration parameter
   * @param aValue
   *          candidate value
   * @param aContextName
   *          name of context, used to generate error message
   * 
   * @throws ResourceConfigurationException
   *           if the data types do not match
   */
  private void validateConfigurationParameterDataTypeMatch(ConfigurationParameter aParam,
          Object aValue, String aContextName) throws ResourceConfigurationException {
    if (aValue != null) {
      Class<?> valClass = aValue.getClass();
      if (aParam.isMultiValued() && !valClass.isArray()) {
        throw new ResourceConfigurationException(ResourceConfigurationException.ARRAY_REQUIRED,
                new Object[] { aParam.getName(), aContextName });
      }

      if (!valClass.equals(getParameterExpectedValueClass(aParam))) {
        throw new ResourceConfigurationException(
                ResourceConfigurationException.PARAMETER_TYPE_MISMATCH, new Object[] {
                    aContextName, valClass.getName(), aParam.getName(), aParam.getType() });
      }
    }
  }

  /**
   * Get the Java class of the expected value of the specified parameter.
   * 
   * @param aParam
   *          configuration parameter declaration information
   * 
   * @return the expected value class
   */
  protected Class<?> getParameterExpectedValueClass(ConfigurationParameter aParam) {
    String paramType = aParam.getType();
    if (aParam.isMultiValued()) {
      if (ConfigurationParameter.TYPE_STRING.equals(paramType)) {
        return String[].class;
      } else if (ConfigurationParameter.TYPE_BOOLEAN.equals(paramType)) {
        return Boolean[].class;
      } else if (ConfigurationParameter.TYPE_INTEGER.equals(paramType)) {
        return Integer[].class;
      } else if (ConfigurationParameter.TYPE_FLOAT.equals(paramType)) {
        return Float[].class;
      }
    } else
    // single-valued
    {
      if (ConfigurationParameter.TYPE_STRING.equals(paramType)) {
        return String.class;
      } else if (ConfigurationParameter.TYPE_BOOLEAN.equals(paramType)) {
        return Boolean.class;
      } else if (ConfigurationParameter.TYPE_INTEGER.equals(paramType)) {
        return Integer.class;
      } else if (ConfigurationParameter.TYPE_FLOAT.equals(paramType)) {
        return Float.class;
      }
    }
    // assert false;
    throw new UIMARuntimeException();
  }

  /**
   * Utility method for getting a configuration parameter.
   * 
   * @param aGroupName
   *          name of config group containing parameter
   * @param aParamName
   *          name of parameter
   * @param aSearchStrategy
   *          search strategy to use. Valid values are defined as constants on the
   *          {@link ConfigurationParameterDeclarations} interface.
   * @param aDefaultGroup
   *          default group to use, if search strategy requires it
   * 
   * @return the value of the specified parameter, <code>null</code> if none
   */
  private Object getConfigParameterValue(String aQualifiedParameterName, String aGroupName,
          String aSearchStrategy, String aDefaultGroup) {
    if (ConfigurationParameterDeclarations.SEARCH_STRATEGY_DEFAULT_FALLBACK.equals(aSearchStrategy)) {
      // try in specified group then in default group
      Object value = getConfigParameterValue(aQualifiedParameterName, aGroupName,
              ConfigurationParameterDeclarations.SEARCH_STRATEGY_NONE, null);
      if (value != null) {
        return value;
      } else {
        return getConfigParameterValue(aQualifiedParameterName, aDefaultGroup,
                ConfigurationParameterDeclarations.SEARCH_STRATEGY_NONE, null);
      }
    } else if (ConfigurationParameterDeclarations.SEARCH_STRATEGY_LANGUAGE_FALLBACK
            .equals(aSearchStrategy)) {
      // try in specified group first
      Object value = getConfigParameterValue(aQualifiedParameterName, aGroupName,
              ConfigurationParameterDeclarations.SEARCH_STRATEGY_NONE, null);

      while (value == null && aGroupName != null) {
        // truncate group name at the last _ or - character
        int lastUnderscore = aGroupName.lastIndexOf('_');
        int lastHyphen = aGroupName.lastIndexOf('-');
        int truncateAt = (lastUnderscore > lastHyphen) ? lastUnderscore : lastHyphen;
        if (truncateAt == -1) {
          aGroupName = null;
        } else {
          aGroupName = aGroupName.substring(0, truncateAt);
        }
        if (aGroupName != null) {
          value = getConfigParameterValue(aQualifiedParameterName, aGroupName,
                  ConfigurationParameterDeclarations.SEARCH_STRATEGY_NONE, null);
        }
      }
      // if we still haen't found a value, try the default group
      if (value == null) {
        value = getConfigParameterValue(aQualifiedParameterName, aDefaultGroup,
                ConfigurationParameterDeclarations.SEARCH_STRATEGY_NONE, null);
      }
      return value;
    } else
    // default - no fallback
    {
      // just to direct look up in the specified group
      return lookup(aGroupName == null ? aQualifiedParameterName : (aQualifiedParameterName
              + GROUP_SEPARATOR + aGroupName));
    }
  }

  /**
   * Gets a parameter value from the Session Parameter Map.
   * 
   * @param aCompleteName
   *          complete parameter name to look up
   * 
   * @return a session-specific value for the given parameter, if any
   * 
   * @throws UIMA_IllegalStateException
   *           if no Session has been set
   */
  @SuppressWarnings("unchecked")
  private Object getSessionParam(String aCompleteName) {
    if (mSession != null) {
      Map<String, Object> m = (Map<String, Object>) mSession.get(SESSION_CONFIGURATION_KEY);
      if (m != null) {
        return m.get(aCompleteName);
      }
    }
    return null;
  }

  /**
   * Sets a parameter value in the Session Parameter Map.
   * 
   * @param aCompleteName
   *          complete parameter name to look up
   * @param aValue
   *          value for the parameter
   * 
   * @throws UIMA_IllegalStateException
   *           if no Session has been set
   */
  @SuppressWarnings("unchecked")
  private void setSessionParam(String aCompleteName, Object aValue) {
    if (mSession == null) {
      throw new UIMA_IllegalStateException();
    } else {
      Map<String, Object> m = (Map<String, Object>) mSession.get(SESSION_CONFIGURATION_KEY);
      if (m == null) {
        m = Collections.synchronizedMap(new HashMap<String, Object>());
        mSession.put(SESSION_CONFIGURATION_KEY, m);
      }
      m.put(aCompleteName, aValue);
    }
  }

}
