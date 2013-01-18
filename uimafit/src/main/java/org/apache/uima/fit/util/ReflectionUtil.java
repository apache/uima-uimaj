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
package org.apache.uima.fit.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
public final class ReflectionUtil {

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
      };
    }
  }
  
  private ReflectionUtil() {
    // Library class
  }

  /**
   * Get all the fields for the class (and superclasses) of the passed in object
   * 
   * @param aObject
   *          any object will do
   * @return the fields for the class of the object
   */
  public static List<Field> getFields(final Object aObject) {
    return getFields(aObject.getClass());
  }

  /**
   * Get all the fields for this class and all of its superclasses
   * 
   * @param aClass
   *          any class will do
   * @return the fields for the class and all of its superclasses
   */
  public static List<Field> getFields(final Class<?> aClass) {
    Class<?> cls = aClass;
    final List<Field> fields = new ArrayList<Field>();
    while (!cls.equals(Object.class)) {
      final Field[] flds = cls.getDeclaredFields();
      fields.addAll(Arrays.asList(flds));
      cls = cls.getSuperclass();
    }
    return fields;
  }

  /**
   * Get the given field of the passed in object from its class or the first superclass that
   * declares it.
   * 
   * @param aObject
   *          any object will do
   * @return the fields for the class of the object
   */
  public static Field getField(final Object aObject, final String aName)
          throws NoSuchFieldException {
    return getField(aObject.getClass(), aName);
  }

  /**
   * Get the given field from the class or the first superclass that declares it.
   * 
   * @param aClass
   *          any class will do
   * @return the fields for the class of the object
   */
  public static Field getField(final Class<?> aClass, final String aName)
          throws NoSuchFieldException {
    try {
      return aClass.getDeclaredField(aName);
    } catch (NoSuchFieldException e) {
      if (aClass.getSuperclass() == null) {
        throw e;
      }

      return getField(aClass.getSuperclass(), aName);
    }
  }

  /**
   * Search for an annotation of the specified type starting on the given class and tracking back
   * the inheritance hierarchy. Only parent classes are tracked back, no implemented interfaces.
   * 
   * @param <T>
   *          the annotation type
   * @param aAnnotation
   *          the annotation class
   * @param aClass
   *          the class to start searching on
   * @return the annotation or {@code null} if it could not be found
   */
  public static <T extends Annotation> T getInheritableAnnotation(final Class<T> aAnnotation,
          final Class<?> aClass) {
    if (isAnnotationPresent(aClass, aAnnotation)) {
      return getAnnotation(aClass, aAnnotation);
    }

    if (aClass.getSuperclass() != null) {
      return getInheritableAnnotation(aAnnotation, aClass.getSuperclass());
    }

    return null;
  }

  /**
   * Equivalent to {@link AccessibleObject#isAnnotationPresent(Class)} but handles uimaFIT legacy
   * annotations.
   */
  public static boolean isAnnotationPresent(AccessibleObject aObject,
          Class<? extends Annotation> aAnnotationClass) {
    // First check if the desired annotation is present
    if (aObject.isAnnotationPresent(aAnnotationClass)) {
      return true;
    }
    
    // If not present, check if an equivalent legacy annotation is present
    return legacySupportPlugin.isAnnotationPresent(aObject, aAnnotationClass);
  }
  
  /**
   * Equivalent to {@link Class#isAnnotationPresent(Class)} but handles uimaFIT legacy
   * annotations.
   */
  public static boolean isAnnotationPresent(Class<?> aObject,
          Class<? extends Annotation> aAnnotationClass) {
    // First check if the desired annotation is present
    if (aObject.isAnnotationPresent(aAnnotationClass)) {
      return true;
    }
    
    // If not present, check if an equivalent legacy annotation is present
    return legacySupportPlugin.isAnnotationPresent(aObject, aAnnotationClass);
  }
  
  /**
   * Equivalent to {@link AccessibleObject#getAnnotation(Class)} but handles uimaFIT legacy
   * annotations.
   */
  public static <T extends Annotation> T getAnnotation(AccessibleObject aObject,
          Class<T> aAnnotationClass) 
  {
    T annotation = aObject.getAnnotation(aAnnotationClass);
    if (annotation == null) {
      annotation = legacySupportPlugin.getAnnotation(aObject, aAnnotationClass);
    }
    return annotation;
  }

  /**
   * Equivalent to {@link Class#getAnnotation(Class)} but handles uimaFIT legacy
   * annotations.
   */
  public static <T extends Annotation> T getAnnotation(Class<?> aObject,
          Class<T> aAnnotationClass) 
  {
    T annotation = aObject.getAnnotation(aAnnotationClass);
    if (annotation == null) {
      annotation = legacySupportPlugin.getAnnotation(aObject, aAnnotationClass);
    }
    return annotation;
  }
}
