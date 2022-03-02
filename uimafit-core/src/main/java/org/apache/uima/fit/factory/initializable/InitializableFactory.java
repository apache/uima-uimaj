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
package org.apache.uima.fit.factory.initializable;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.internal.ClassLoaderUtils;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Please see {@link Initializable} for a description of how this class is intended to be used.
 *
 * @see Initializable
 */
public final class InitializableFactory {
  private InitializableFactory() {
    // This class is not meant to be instantiated
  }

  /**
   * Provides a way to create an instance of T. If the class specified by className implements
   * {@link Initializable}, then the UimaContext provided here will be passed to its initialize
   * method.
   *
   * @param <T>
   *          the interface type
   * @param context
   *          the UIMA context containing the parameter settings
   * @param className
   *          the name of a class implementing {@link Initializable}
   * @param superClass
   *          a class to which the initializable class is cast
   * @return a new initialized instance of the initializable class cast as the specified class
   * @throws ResourceInitializationException
   *           if there was a problem during initialization or instantiation
   */
  public static <T> T create(UimaContext context, String className, Class<T> superClass)
          throws ResourceInitializationException {
    Class<? extends T> cls;
    try {
      ClassLoader cl = ClassLoaderUtils.findClassloader(context);
      cls = Class.forName(className, true, cl).asSubclass(superClass);
    } catch (Exception e) {
      throw new ResourceInitializationException(new IllegalStateException(
              "classname = " + className + " superClass = " + superClass.getName(), e));
    }
    return create(context, cls);
  }

  /**
   * @param <T>
   *          the interface type
   * @param className
   *          the name of a class implementing {@link Initializable}
   * @param superClass
   *          a class to which the initializable class is cast
   * @return a new initialized instance of the initializable class cast as the specified class
   * @throws ResourceInitializationException
   *           if there was a problem casting the class
   */
  public static <T> Class<? extends T> getClass(String className, Class<T> superClass)
          throws ResourceInitializationException {
    try {
      ClassLoader cl = ClassLoaderUtils.findClassloader();
      return Class.forName(className, true, cl).asSubclass(superClass);
    } catch (Exception e) {
      throw new ResourceInitializationException(new IllegalStateException(
              "classname = " + className + " superClass = " + superClass.getName(), e));
    }
  }

  /**
   * @param <T>
   *          the interface type
   * @param context
   *          the UIMA context containing the parameter settings
   * @param cls
   *          the class implementing {@link Initializable}
   * @return a new initialized instance of the initializable class cast as the specified class
   * @throws ResourceInitializationException
   *           if there was a problem during initialization or instantiation
   */
  public static <T> T create(UimaContext context, Class<? extends T> cls)
          throws ResourceInitializationException {
    T instance;
    try {
      instance = cls.newInstance();
    } catch (Exception e) {
      throw new ResourceInitializationException(e);
    }
    initialize(instance, context);
    return instance;
  }

  /**
   * @param object
   *          an instance of a class implementing {@link Initializable}
   * @param context
   *          the UIMA context containing the parameter settings
   * @throws ResourceInitializationException
   *           if there was a problem during initialization
   */
  public static void initialize(Object object, UimaContext context)
          throws ResourceInitializationException {
    if (object instanceof Initializable) {
      ((Initializable) object).initialize(context);
    }
  }
}
