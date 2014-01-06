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

package org.apache.uima.collection.impl.cpm.utils;

import org.apache.uima.UIMAFramework;
import org.apache.uima.internal.util.JavaTimer;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.UimaTimer;

/**
 * Creates an instance of UimaTimer
 * 
 * 
 */
public class TimerFactory {
  private static UimaTimer timer = null;

  /**
   * Instantiate UimaTimer object from a given class
   * 
   * @param aClassName -
   *          UimaTimer implemetation class
   */
  public TimerFactory(String aClassName) {
    try {
      initialize(aClassName);
    } catch (Exception e) {
      if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
        UIMAFramework.getLogger(TimerFactory.class).logrb(Level.CONFIG,
                TimerFactory.class.getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_java_timer__CONFIG", new Object[] { Thread.currentThread().getName() });
      }
      timer = new JavaTimer();
    }
  }

  /**
   * Returns instance of {@link UimaTimer}
   * 
   * @return UimaTimer instance
   */
  public static UimaTimer getTimer() {
    if (timer == null) {
      if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
        UIMAFramework.getLogger(TimerFactory.class).logrb(Level.CONFIG,
                TimerFactory.class.getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_java_timer__CONFIG", new Object[] { Thread.currentThread().getName() });
      }
      timer = new JavaTimer();
    }

    return timer;

  }

  /**
   * Instantiates dynamically a UimaTimer object
   * 
   * @param aClassName -
   *          class implementing UimaTimer
   * 
   * @throws ResourceInitializationException -
   */
  private void initialize(String aClassName) throws ResourceInitializationException {

    if (aClassName != null) {

      try {
        Class currentClass = Class.forName(aClassName);
        Object anObject = currentClass.newInstance();
        if (anObject instanceof UimaTimer) {
          timer = (UimaTimer) anObject;
          if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
            UIMAFramework.getLogger(TimerFactory.class).logrb(Level.CONFIG,
                    TimerFactory.class.getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_show_timer_class__CONFIG",
                    new Object[] { Thread.currentThread().getName(), timer.getClass().getName() });
          }
          return;
        }
      } catch (ClassNotFoundException e) {
        throw new ResourceInitializationException(ResourceInitializationException.CLASS_NOT_FOUND,
                new Object[] { aClassName, "CPE" }, e);
      } catch (IllegalAccessException e) {
        throw new ResourceInitializationException(
                ResourceInitializationException.COULD_NOT_INSTANTIATE, new Object[] { aClassName,
                    "CPE" }, e);
      } catch (InstantiationException e) {
        throw new ResourceInitializationException(
                ResourceInitializationException.COULD_NOT_INSTANTIATE, new Object[] { aClassName,
                    "CPE" }, e);
      }

      if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
        UIMAFramework.getLogger(TimerFactory.class).logrb(Level.CONFIG,
                TimerFactory.class.getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_java_timer__CONFIG", new Object[] { Thread.currentThread().getName() });
      }
      timer = new JavaTimer();
    }
  }
}
