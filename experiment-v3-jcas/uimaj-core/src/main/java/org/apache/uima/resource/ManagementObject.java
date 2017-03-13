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
package org.apache.uima.resource;

/**
 * Base interface for objects that expose a monitoring and management interface to a 
 * UIMA component or some part of the UIMA framework.
 * <p>
 * In this implementation, objects implementing this interface will always be JMX-compatible MBeans
 * that you can register with an MBeanServer. For information on JMX see <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/javax/management/package-summary.html">
 * http://java.sun.com/j2se/1.5.0/docs/api/javax/management/package-summary.html</a>
 */
public interface ManagementObject {
  /**
   * Gets a valid JMX MBean name that is unique among all ManagementObjects in this
   * JVM. (Technically, it is unique only among ManagementObjects objects loaded by the same
   * ClassLoader, which is whatever ClassLoader was used to load the UIMA Framework classes.)
   * <p>
   * If you are running with JRE 1.5, this is the name used to register this object with the
   * platform MBeanServer.
   * 
   * @return a unique MBean name
   */
  String getUniqueMBeanName();
}
