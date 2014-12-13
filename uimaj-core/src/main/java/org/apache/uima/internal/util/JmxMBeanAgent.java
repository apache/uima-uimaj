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

package org.apache.uima.internal.util;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ManagementObject;
import org.apache.uima.util.Level;

/**
 * Utility class for registering MBeans with a JMX MBeanServer. This allows AnalysisEngine
 * performance stats to be monitored through JMX, for example.
 */
public class JmxMBeanAgent {
  /**
   * Register an MBean with the MBeanServer.
   * 
   * @param aMBean
   *          the MBean to register
   * @param aMBeanServerO
   *          server to register with. If null, the platform MBeanServer will be used if we are
   *          running under Java 1.5. Earlier versions of Java did not have a platform MBeanServer;
   *          in that case, this method will do nothing.
   */
  public static void registerMBean(ManagementObject aMBean, Object aMBeanServerO) {
    MBeanServer aMBeanServer = (MBeanServer)aMBeanServerO;
    if (!jmxAvailable) // means we couldn't find the required classes and methods
    {
      return;
    }

    if (aMBeanServer == null) {
      if (platformMBeanServer != null) {
        aMBeanServer = platformMBeanServer;
      } else {
        UIMAFramework.getLogger().logrb(Level.CONFIG, JmxMBeanAgent.class.getName(),
                "registerMBean", LOG_RESOURCE_BUNDLE,
                "UIMA_JMX_platform_mbean_server_not_available__CONFIG");
        return;
      }
    }
    
    try {
      // Now register the MBean. But, check for name collisions first. It is possible
      // that we are trying to register the same MBean twice (this happens for pools of AEs that
      // all share an identical UimaContext). If that happens, we only register the
      // MBean the first time and just skip the registration each subsequent time.
//      Object mbeanName = objectNameConstructor.newInstance(new Object[] { aMBean
//              .getUniqueMBeanName() });
      ObjectName mbeanName = new ObjectName(aMBean.getUniqueMBeanName());
      // synchronize to prevent multiple threads from initializing same thing UIMA-1247
      synchronized (aMBeanServer) {
        if (!(aMBeanServer.isRegistered(mbeanName))) {
        
//        if (!(((Boolean) isRegistered.invoke(aMBeanServer, new Object[] { mbeanName }))
//                .booleanValue())) {
          aMBeanServer.registerMBean(aMBean, mbeanName);
//          registerMBean.invoke(aMBeanServer, new Object[] { aMBean, mbeanName });
        }
      }
    } catch (Exception e) {
      // don't fail catastrophically if we can't register with JMX. Just log a warning and continue.
      UIMAFramework.getLogger()
              .logrb(Level.WARNING, JmxMBeanAgent.class.getName(), "registerMBean",
                      LOG_RESOURCE_BUNDLE, "UIMA_JMX_failed_to_register_mbean__WARNING", e);
      return;
    }
  }

  /**
   * Unregister an MBean from the MBeanServer.
   * 
   * @param aMBean
   *          the MBean to register
   * @param aMBeanServerO
   *          server to unregister from. If null, the platform MBeanServer will be used if we are
   *          running under Java 1.5. Earlier versions of Java did not have a platform MBeanServer;
   *          in that case, this method will do nothing.
   */
  public static void unregisterMBean(ManagementObject aMBean, Object aMBeanServerO) {
    MBeanServer aMBeanServer = (MBeanServer)aMBeanServerO;
    if (!jmxAvailable) // means we couldn't find the required classes and methods
    {
      return;
    }

    if (aMBeanServer == null) {
      if (platformMBeanServer != null) {
        aMBeanServer = platformMBeanServer;
      } else {
        UIMAFramework.getLogger().logrb(Level.CONFIG, JmxMBeanAgent.class.getName(),
                "unregisterMBean", LOG_RESOURCE_BUNDLE,
                "UIMA_JMX_platform_mbean_server_not_available__CONFIG");
        return;
      }
    }

    try {
      // Now unregister the MBean.
      String mbeanName = aMBean.getUniqueMBeanName();
      if (mbeanName != null) // guards against uninitialized AE instances
      {
        ObjectName objName = new ObjectName(mbeanName);
        if (aMBeanServer.isRegistered(objName)) {
          aMBeanServer.unregisterMBean(objName);
        }
//        Object objName = objectNameConstructor.newInstance(new Object[] { mbeanName });
//        if (((Boolean) isRegistered.invoke(aMBeanServer, new Object[] { objName })).booleanValue()) {
//          unregisterMBean.invoke(aMBeanServer, new Object[] { objName });
//        }
      }
    } catch (Exception e) {
      // don't fail catastrophically if we can't unregister. Just log a warning and continue.
      UIMAFramework.getLogger().logrb(Level.WARNING, JmxMBeanAgent.class.getName(),
              "unregisterMBean", LOG_RESOURCE_BUNDLE,
              "UIMA_JMX_failed_to_unregister_mbean__WARNING", e);
      return;
    }
  }

  /** Class and Method handles for reflection */
//  private static Class mbeanServerClass;
//
//  private static Class objectNameClass;

//  private static Constructor objectNameConstructor;
//
//  private static Method isRegistered;
//
//  private static Method registerMBean;
//
//  private static Method unregisterMBean;

  /**
   * Set to true if we can find the required JMX classes and methods
   */
  private static boolean jmxAvailable = true;

  /**
   * The platform MBean server
   * This is available since Java 1.5
   */
  private static MBeanServer platformMBeanServer;

  /** Get class/method handles */
  static {
//    try {
//      mbeanServerClass = Class.forName("javax.management.MBeanServer");
//      objectNameClass = Class.forName("javax.management.ObjectName");
//      objectNameConstructor = objectNameClass.getConstructor(new Class[] { String.class });
//      isRegistered = mbeanServerClass.getMethod("isRegistered", new Class[] { objectNameClass });
//      registerMBean = mbeanServerClass.getMethod("registerMBean", new Class[] { Object.class,
//          objectNameClass });
//      unregisterMBean = mbeanServerClass.getMethod("unregisterMBean",
//              new Class[] { objectNameClass });
//      jmxAvailable = true;
//    } catch (Exception e) {
//      // JMX not available
//      jmxAvailable = false;
//    }

    // try to get platform MBean Server (Java 1.5 only)
//    try {
//      Class managementFactory = Class.forName("java.lang.management.ManagementFactory");
//      Method getPlatformMBeanServer = managementFactory.getMethod("getPlatformMBeanServer",
//              new Class[0]);
//      platformMBeanServer = getPlatformMBeanServer.invoke(null, null);
//    } catch (Exception e) {
//      platformMBeanServer = null;
//    }
    platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
  }
  

  /**
   * resource bundle for log messages
   */
  private static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";

}
