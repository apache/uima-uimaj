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

package org.apache.uima.analysis_engine.impl;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_engine.AnalysisEngineManagement;

/**
 * Implements Monitoring/Management interface to an AnalysisEngine.
 * 
 */
public class AnalysisEngineManagementImpl implements AnalysisEngineManagementImplMBean,
        AnalysisEngineManagement {

  private static final long serialVersionUID = 1988620286191379887L;
 
  private static final Pattern RESERVED_CHAR_PATTERN = Pattern.compile("[\",=:*?]");
  
  static final DecimalFormat format = new DecimalFormat("0.##");

  /**
   * This static set is needed to keep track of what names we've already used for "root" MBeans
   * (those representing top-level AEs and CPEs).
   */
  private static Set usedRootNames = new HashSet();

  private String name;

  private long numProcessed;

  private long markedAnalysisTime;

  private long markedBatchProcessCompleteTime;

  private long markedCollectionProcessCompleteTime;

  private long markedServiceCallTime;

  private long analysisTime;

  private long batchProcessCompleteTime;

  private long collectionProcessCompleteTime;

  private long serviceCallTime;

  private Map components = new HashMap();

  private String uniqueMBeanName;

  public void reportAnalysisTime(long time) {
    analysisTime += time;
  }

  public void reportBatchProcessCompleteTime(long time) {
    batchProcessCompleteTime += time;
  }

  public void reportCollectionProcessCompleteTime(long time) {
    collectionProcessCompleteTime += time;
  }

  public void reportServiceCallTime(long time) {
    serviceCallTime += time;
  }

  public void incrementCASesProcessed() {
    numProcessed++;
  }

  public long getBatchProcessCompleteTime() {
    return batchProcessCompleteTime;
  }

  public long getCollectionProcessCompleteTime() {
    return collectionProcessCompleteTime;
  }

  public long getAnalysisTime() {
    return analysisTime;
  }

  public long getServiceCallTime() {
    return serviceCallTime;
  }

  /**
   * Internal use only. Used to implement backwards compatibility with the ProcessTrace interface.
   */
  public void mark() {
    markedAnalysisTime = analysisTime;
    markedBatchProcessCompleteTime = batchProcessCompleteTime;
    markedCollectionProcessCompleteTime = collectionProcessCompleteTime;
    markedServiceCallTime = serviceCallTime;
    // mark components also
    Iterator iter = components.values().iterator();
    while (iter.hasNext()) {
      AnalysisEngineManagementImpl component = (AnalysisEngineManagementImpl) iter.next();
      component.mark();
    }
  }

  /**
   * Internal use only. Used to implement backwards compatibility with the ProcessTrace interface.
   */
  public long getBatchProcessCompleteTimeSinceMark() {
    return batchProcessCompleteTime - markedBatchProcessCompleteTime;
  }

  /**
   * Internal use only. Used to implement backwards compatibility with the ProcessTrace interface.
   */
  public long getCollectionProcessCompleteTimeSinceMark() {
    return collectionProcessCompleteTime - markedCollectionProcessCompleteTime;
  }

  /**
   * Internal use only. Used to implement backwards compatibility with the ProcessTrace interface.
   */
  public long getAnalysisTimeSinceMark() {
    return analysisTime - markedAnalysisTime;
  }

  /**
   * Internal use only. Used to implement backwards compatibility with the ProcessTrace interface.
   */
  public long getServiceCallTimeSinceMark() {
    return serviceCallTime - markedServiceCallTime;
  }

  public long getNumberOfCASesProcessed() {
    return numProcessed;
  }

  public String getCASesPerSecond() {
    if (analysisTime == 0)
      return "0";
    float docsPerSecond = (float) numProcessed / analysisTime * 1000;
    return format.format(docsPerSecond);
  }

  public Map getComponents() {
    return Collections.unmodifiableMap(components);
  }

  public void addComponent(String key, AnalysisEngineManagementImpl component) {
    components.put(key, component);
  }

  public String getName() {
    return name;
  }

  public String getUniqueMBeanName() {
    return uniqueMBeanName;
  }

  public void resetStats() {
    numProcessed = 0;
    analysisTime = 0;
    batchProcessCompleteTime = 0;
    collectionProcessCompleteTime = 0;
    serviceCallTime = 0;
    markedAnalysisTime = 0;
    markedBatchProcessCompleteTime = 0;
    markedCollectionProcessCompleteTime = 0;
    markedServiceCallTime = 0;
    // reset components also
    Iterator iter = components.values().iterator();
    while (iter.hasNext()) {
      AnalysisEngineManagementImpl component = (AnalysisEngineManagementImpl) iter.next();
      component.resetStats();
    }
  }

  /**
   * Sets the name of this AnalyaisEngineManagement object, and also computes the unique MBean name
   * that can later be used to register this object with an MBeanServer.
   * 
   * @param aName
   *          the simple name of this AnalysisEngine (generally this is the <code>name</code>
   *          property from the AnalysisEngineMetaData)
   * @param aContext
   *          the UimaContext for this AnalysisEngine. Needed to compute the unique name, which is
   *          hierarchical
   * @param aCustomPrefix an optional prefix provided by the Application, which will be
   *          prepended to the name generated by UIMA.  If null, the prefix
   *          "org.apache.uima:" will be used.
   */
  public void setName(String aName, UimaContextAdmin aContext, String aCustomPrefix) {   
    // set the simple name
    name = aName;
    
    //determine the MBean name prefix we should use
    String prefix;
    if (aCustomPrefix == null) {
      prefix = "org.apache.uima:";
    } else {
      prefix = aCustomPrefix;
      if (!prefix.endsWith(":") && !prefix.endsWith(",")) {
        prefix += ",";
      }
    }
    
    // compute the unique name   
    // (first get the rootMBean and assign it a unique name if it doesn't already have one)
    AnalysisEngineManagementImpl rootMBean = (AnalysisEngineManagementImpl) aContext
            .getRootContext().getManagementInterface();
    if (rootMBean.getUniqueMBeanName() == null) {
      // try to find a unique name for the root MBean
      String baseRootName = rootMBean.getName();
      if (baseRootName == null) {
        baseRootName = "CPE"; // CPE's don't currently have names
      }
      String rootName = baseRootName;
      int i = 2;
      while (usedRootNames.contains(rootName)) {
        rootName = baseRootName + " " + i++;
      }
      usedRootNames.add(rootName);
      // create a propertly-formatted MBean name, using the specified prefix
      rootMBean.uniqueMBeanName = prefix + "name=" + escapeValue(rootName);
    }

    if (rootMBean != this) {
      // form the MBean name hierarchically starting from the root name
      String rootName = rootMBean.getUniqueMBeanName();
      // strip off the MBean name prefix to get just the simple name
      rootName = rootName.substring(prefix.length() + "name=".length());
      // form the hierarchical MBean name
      prefix += "p0=";
      //we add "Components" to the end of the rootName, but be aware of quoting issues
      if (rootName.endsWith("\"")) {
        prefix += rootName.substring(0,rootName.length() - 1) + " Components\",";
      }
      else {
        prefix += rootName + " Components,";
      }      
      uniqueMBeanName = makeMBeanName(prefix, aContext.getQualifiedContextName().substring(1), 1);
    }
  }

  /**
   * Recursive utility method for generating a hierarchical mbean name
   */
  private static String makeMBeanName(String prefix, String contextName, int depth) {
    int firstSlash = contextName.indexOf('/');
    if (firstSlash == contextName.length() - 1) {
      return prefix + "name=" + escapeValue(contextName.substring(0, contextName.length() - 1));
    } else {
      String newPrefix = prefix + "p" + depth + "=" + escapeValue(contextName.substring(0, firstSlash)
              + " Components") + ",";
      return makeMBeanName(newPrefix, contextName.substring(firstSlash + 1), depth + 1);
    }
  }
  
  /** Escapes the "value" part of a JMX name if necessary.  If the value
   * includes reserved characters (" , = : * ?) the value will be enclosed
   * in quotes and some characters (" ? * \) will be escaped with backslashes.
   */
  private static String escapeValue(String value) {
    if (RESERVED_CHAR_PATTERN.matcher(value).find()) {
      //must quote the value
      StringBuffer buf = new StringBuffer();
      buf.append('\"');
      //must escape special characters inside the quoted value
      for (int i = 0; i < value.length(); i++) {
        char c = value.charAt(i);
        if (c == '\"' || c =='\\' || c == '?' || c =='*') {
          buf.append('\\');
        }
        buf.append(c);
      }
      buf.append('\"');
      return buf.toString();
    } else {
      return value; //no escaping needed
    }
  }
}
