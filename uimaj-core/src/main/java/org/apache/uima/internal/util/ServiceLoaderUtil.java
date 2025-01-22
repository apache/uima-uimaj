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

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceLoaderUtil {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int MAX_BROKEN_SERVICES = 16;

  private static final WeakIdentityMap<ClassLoader, Map<Class<?>, List<?>>> cl_to_services = //
          WeakIdentityMap.newHashMap();

  public static void clearServiceCache() {
    synchronized (cl_to_services) {
      cl_to_services.clear();
    }
  }

  public static <T> Stream<T> loadServicesSafely(Class<T> aService) {
    var cl = ClassLoaderUtils.findClassLoader();
    return loadServicesSafely(aService, cl, null);
  }

  public static <T> Stream<T> loadServicesSafely(Class<T> aService, ClassLoader aClassLoader) {
    return loadServicesSafely(aService, aClassLoader, null);
  }

  static <T> Stream<T> loadServicesSafely(Class<T> aService, ClassLoader aClassLoader,
          Collection<Throwable> aErrorCollector) {

    Map<Class<?>, List<?>> servicesMap;

    synchronized (cl_to_services) {
      servicesMap = cl_to_services.get(aClassLoader);
      if (servicesMap == null) {
        servicesMap = new LinkedHashMap<>();
        cl_to_services.put(aClassLoader, servicesMap);
      }
    }

    synchronized (servicesMap) {
      @SuppressWarnings("unchecked")
      var services = (List<T>) servicesMap.get(aService);
      if (services == null) {
        var loader = ServiceLoader.load(aService, aClassLoader);
        services = StreamSupport.stream(
                new ServiceLoaderSpliterator<T>(aService, loader.iterator(), aErrorCollector),
                false).toList();
        servicesMap.put(aService, services);
      }
      return services.stream();
    }
  }

  private static class ServiceLoaderSpliterator<T> implements Spliterator<T> {

    private final Logger log;
    private final Iterator<T> serviceIterator;
    private final Class<T> service;
    private final Collection<Throwable> errorCollector;

    public ServiceLoaderSpliterator(Class<T> aService, Iterator<T> aIterator,
            Collection<Throwable> aErrorCollector) {
      serviceIterator = aIterator;
      service = aService;
      errorCollector = aErrorCollector;
      log = aErrorCollector == null ? LOG : null;
    }

    @Override
    public boolean tryAdvance(final Consumer<? super T> aAction) {
      int i = MAX_BROKEN_SERVICES;
      while (i-- > 0) {
        try {
          if (serviceIterator.hasNext()) {
            aAction.accept(serviceIterator.next());
            return true;
          }
        } catch (ServiceConfigurationError | LinkageError e) {
          handle(e);
        } catch (Throwable e) {
          handle(e);
          throw e;
        }
      }
      return false;
    }

    private void handle(Throwable e) {
      if (log != null) {
        log.warn("Unable to load service class for service {}", service, e);
      }
      if (errorCollector != null) {
        errorCollector.add(e);
      }
    }

    @Override
    public Spliterator<T> trySplit() {
      return null;
    }

    @Override
    public long estimateSize() {
      return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
      return NONNULL | IMMUTABLE;
    }
  }
}
