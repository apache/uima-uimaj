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
          String propValue = aExternalOverrides.lookUp(extName);
          if (propValue != null) {
            Object result = createParam(propValue, aParams[i].getType(), aParams[i].isMultiValued());
            if (result == null) {
              throw new NumberFormatException("Array mismatch assigning value of " + extName + " ('" + propValue
                      + "') to " + aParams[i].getName());
            }
            paramValue = result;
            mLinkMap.remove(qname);
            from = "(overridden from " + extName + ")";
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
        UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(), "declareParameters",
                LOG_RESOURCE_BUNDLE, "UIMA_parameter_set__CONFIG",
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
    boolean arrayValue = value.length() > 0 && value.charAt(0) == '[' && value.charAt(value.length()-1) == ']';
    if (arrayValue ^ isArray) {
      return null;  // Caller throws exception
    }
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
      throw new NumberFormatException("Failed to convert '" + value + "' to " + paramType);
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
   * Tokenize arrays on ',' but if should have been escaped put the ',' back
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
      value = value.substring(1, value.length()-1);
      if (value.length() == 0) {          // If an empty string create a 0-length array 
        return (T[]) Array.newInstance(clas, 0);
      }
      String[] tokens = value.split(",");
      int nTokens = tokens.length;
      int i;
      for (i = 0; i < tokens.length - 1; ++i) {
        if (endsWithEscape(tokens[i])) {
          tokens[i+1] = tokens[i] + "," + tokens[i+1];
          tokens[i] = null;
          --nTokens;
        }
      }
      if (endsWithEscape(tokens[i])) {
        tokens[i] += ",";
      }
      T[] result = (T[]) Array.newInstance(clas, nTokens);
      i = 0;
      for (String token : tokens) {
        if (token != null) {
          result[i++] = (T) valOf.invoke(null, escape(token.trim()));
        }
      }
      return result;
    } else {
      return valOf.invoke(null, escape(value));
    }
  }

  // Finally process any escapes by replacing \x by x
  private String escape(String token) {
    int next = token.indexOf('\\');
    if (next < 0) {
      return token;
    }
    StringBuilder result = new StringBuilder(token.length());
    int last = 0;
    // For each '\' found copy up to it and restart the search after the next char
    while (next >= 0) {
      result.append(token.substring(last, next));
      last = next + 1;
      next = token.indexOf('\\', last + 1);
    }
    result.append(token.substring(last));
    return result.toString();
  }
  
  private boolean endsWithEscape(String line) {
    int i = line.length();
    while (i > 0 && line.charAt(i-1) == '\\') {
      --i;
    }
    // If change in i is odd then ended with an unescaped \ 
    return ((line.length() - i) % 2 != 0);
  }

}
