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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_engine.AnalysisEngineManagement;
import org.apache.uima.util.ConcurrentHashMapWithProducer;

/**
 * Implements Monitoring/Management interface to an AnalysisEngine.
 * 
 */
public class AnalysisEngineManagementImpl 
    implements AnalysisEngineManagementImplMBean, AnalysisEngineManagement {

  private static final long serialVersionUID = 1988620286191379887L;
  
  private static final Pattern RESERVED_CHAR_PATTERN = Pattern.compile("[\",=:*?]");
  
  static final DecimalFormat format = new DecimalFormat("0.##");

  /**
   * This static set is needed to keep track of what names we've already used for "root" MBeans
   * (those representing top-level AEs and CPEs).
   */
  private final static ConcurrentHashMapWithProducer<String, AtomicInteger> usedRootNames = new ConcurrentHashMapWithProducer<String, AtomicInteger>();

  private String name;

  private final AtomicLong numProcessed = new AtomicLong(0);

  private final AtomicLong markedAnalysisTime = new AtomicLong(0);

  private final AtomicLong markedBatchProcessCompleteTime = new AtomicLong(0);

  private final AtomicLong markedCollectionProcessCompleteTime = new AtomicLong(0);

  private final AtomicLong markedServiceCallTime = new AtomicLong(0);

  private final AtomicLong analysisTime = new AtomicLong(0);

  private final AtomicLong batchProcessCompleteTime = new AtomicLong(0);

  private final AtomicLong collectionProcessCompleteTime = new AtomicLong(0);

  private final AtomicLong serviceCallTime = new AtomicLong(0);

  private final Map<String, AnalysisEngineManagement> components = Collections.synchronizedMap(new LinkedHashMap<String, AnalysisEngineManagement>());

  private String uniqueMBeanName;

  
  private State status = State.Initializing;  // Initial AE state
  
  private long threadId = Thread.currentThread().getId();  // Thread id which created this instance  
  private long initializationTime;
  
  public long getInitializationTime() {
    return initializationTime;
  }
  
  public void setInitializationTime(long initializationTime) {
    this.initializationTime = initializationTime;
  }
  public long getThreadId() {
    return threadId;
  }
 
  public String getState() {
    return this.status.toString();
  }
  
  public void setState(State state) {
    this.status = state;
  }

  
  public void reportAnalysisTime(long time) {
    analysisTime.addAndGet(time);
  }

  public void reportBatchProcessCompleteTime(long time) {
    batchProcessCompleteTime.addAndGet(time);
  }

  public void reportCollectionProcessCompleteTime(long time) {
    collectionProcessCompleteTime.addAndGet(time);
  }

  public void reportServiceCallTime(long time) {
    serviceCallTime.addAndGet(time);
  }

  public void incrementCASesProcessed() {
    numProcessed.incrementAndGet();
  }

  public long getBatchProcessCompleteTime() {
    return batchProcessCompleteTime.get();
  }

  public long getCollectionProcessCompleteTime() {
    return collectionProcessCompleteTime.get();
  }

  public long getAnalysisTime() {
  	return analysisTime.get() + serviceCallTime.get();
  }

  public long getServiceCallTime() {
    return serviceCallTime.get();
  }

  /**
   * Internal use only. Used to implement backwards compatibility with the ProcessTrace interface.
   */
  public void mark() {
    markedAnalysisTime.set(analysisTime.get());
    markedBatchProcessCompleteTime.set(batchProcessCompleteTime.get());
    markedCollectionProcessCompleteTime.set(collectionProcessCompleteTime.get());
    markedServiceCallTime.set(serviceCallTime.get());
    // mark components also
    for (AnalysisEngineManagement component : components.values()) {
      ((AnalysisEngineManagementImpl)component).mark();
    }
  }

  /**
   * Internal use only. Used to implement backwards compatibility with the ProcessTrace interface.
   * @return Batch Process Complete time since mark
   */
  public long getBatchProcessCompleteTimeSinceMark() {
    return batchProcessCompleteTime.get() - markedBatchProcessCompleteTime.get();
  }

  /**
   * Internal use only. Used to implement backwards compatibility with the ProcessTrace interface.
   * @return Collection Process Complete time since mark
   */
  public long getCollectionProcessCompleteTimeSinceMark() {
    return collectionProcessCompleteTime.get() - markedCollectionProcessCompleteTime.get();
  }

  /**
   * Internal use only. Used to implement backwards compatibility with the ProcessTrace interface.
   * @return Analysis time since mark
   */
  public long getAnalysisTimeSinceMark() {
    return analysisTime.get() - markedAnalysisTime.get();
  }

  /**
   * Internal use only. Used to implement backwards compatibility with the ProcessTrace interface.
   * @return service call time since mark
   */
  public long getServiceCallTimeSinceMark() {
    return serviceCallTime.get() - markedServiceCallTime.get();
  }

  public long getNumberOfCASesProcessed() {
    return numProcessed.get();
  }

  public String getCASesPerSecond() {
    long analysisTime = getAnalysisTime();
    if (analysisTime == 0)
      return "0";
    float docsPerSecond = (float) numProcessed.get() / analysisTime * 1000;
    return format.format(docsPerSecond);
  }

  public Map<String, AnalysisEngineManagement> getComponents() {
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
    numProcessed.set(0);
    analysisTime.set(0);
    batchProcessCompleteTime.set(0);
    collectionProcessCompleteTime.set(0);
    serviceCallTime.set(0);
    markedAnalysisTime.set(0);
    markedBatchProcessCompleteTime.set(0);
    markedCollectionProcessCompleteTime.set(0);
    markedServiceCallTime.set(0);
    // reset components also
    for (AnalysisEngineManagement component : components.values()) {
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
      rootMBean.uniqueMBeanName = prefix + "name=" + escapeValue(getRootName(rootMBean.getName()));
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
  
  static private final Callable<AtomicInteger> produceAtomicInteger = new Callable<AtomicInteger>() {  
    @Override
    public AtomicInteger call() throws Exception{
      return new AtomicInteger(1);
    }
  };
  
  /**
   * 
   * @param baseRootName
   * @return the baseRootName (or CPE if null) plus a suffix 2, 3, ... to be unique
   */
  // package private for testing
  static String getRootName(String baseRootName) {
    if (baseRootName == null) {
      baseRootName = "CPE"; // CPE's don't currently have names
    }
    AtomicInteger suffix;
    try {
      suffix = usedRootNames.get(baseRootName, produceAtomicInteger);
    } catch (Exception e) {
      throw new RuntimeException(e); // never happen.
    }
    int suffixI = suffix.getAndIncrement();
    return (suffixI == 1) ? baseRootName : baseRootName + suffixI;
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
