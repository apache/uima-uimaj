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
package org.apache.uima.fit.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;

import org.apache.uima.resource.ResourceInitializationException;

/**
 * INTERNAL API - Legacy support.
 */
public final class LegacySupport {

  private LegacySupport() {
    // No instances
  }

  private static LegacySupportPlugin legacySupportPlugin;

  // Initialize legacy support once on startup.
  static {
    try {
      Class<?> plc = Class.forName("org.apache.uima.fit.legacy.LegacySupportPluginImpl");
      legacySupportPlugin = (LegacySupportPlugin) plc.newInstance();
    } catch (IllegalAccessException e) {
      // Cannot access legacy support for some reason, where to log this?
    } catch (ClassNotFoundException e) {
      // Legacy support not available.
    } catch (InstantiationException e) {
      // Some other odd reason the plugin cannot be instantiated. Again, where to log this?
    }

    // If no legacy support is available, instantiate a dummy.
    if (legacySupportPlugin == null) {
      legacySupportPlugin = new LegacySupportPlugin() {
        public boolean isAnnotationPresent(AccessibleObject aObject,
                Class<? extends Annotation> aAnnotationClass) {
          return false;
        }

        public <L extends Annotation, M extends Annotation> M getAnnotation(
                AccessibleObject aObject, Class<M> aAnnotationClass) {
          return null;
        }

        public boolean isAnnotationPresent(Class<?> aObject,
                Class<? extends Annotation> aAnnotationClass) {
          return false;
        }

        public <L extends Annotation, M extends Annotation> M getAnnotation(Class<?> aObject,
                Class<M> aAnnotationClass) {
          return null;
        }

        public String[] scanTypeDescriptors(MetaDataType aType)
                throws ResourceInitializationException {
          return new String[0];
        }
      };
    }
  }
  
  /**
   * Get legacy support instance. Never returns {@code null}. If no legacy support plug-in is 
   * available, a dummy plug-in is returned.
   */
  public static LegacySupportPlugin getInstance() {
    return legacySupportPlugin;
  }
}
