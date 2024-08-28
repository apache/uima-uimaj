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
package org.apache.uima.test;

import static java.util.Arrays.asList;
import static java.util.regex.Pattern.quote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special ClassLoader that helps us modeling different class loader topologies.
 */
public class IsolatingClassloader extends ClassLoader {

  private final Logger log;

  private final Set<String> hideClassesPatterns = new HashSet<>();
  private final Set<String> redefineClassesPatterns = new HashSet<>();
  private final Map<String, ClassLoader> delegates = new LinkedHashMap<>();
  private final String id;

  private Map<String, Class<?>> loadedClasses = new HashMap<>();

  public IsolatingClassloader(String name, ClassLoader parent) {
    this(name, null, parent);
  }

  public IsolatingClassloader(String name, Logger aLog, ClassLoader parent) {
    super(parent);

    log = aLog != null ? aLog : LoggerFactory.getLogger(getClass());
    id = name;
  }

  public IsolatingClassloader hiding(Package... packages) {
    for (var pack : packages) {
      hideClassesPatterns.add(quote(pack.getName()) + "\\..*");
    }
    return this;
  }

  public IsolatingClassloader hiding(Class<?>... classes) {
    for (var clazz : classes) {
      hideClassesPatterns.add(quote(clazz.getName()));
    }
    return this;
  }

  public IsolatingClassloader hiding(String... patterns) {
    hideClassesPatterns.addAll(asList(patterns));
    return this;
  }

  public IsolatingClassloader redefining(Package... packages) {
    for (var pack : packages) {
      redefineClassesPatterns.add(quote(pack.getName()) + "\\..*");
    }
    return this;
  }

  public IsolatingClassloader redefining(Class<?>... classes) {
    for (var clazz : classes) {
      redefineClassesPatterns.add(quote(clazz.getName()));
    }
    return this;
  }

  public IsolatingClassloader redefining(String... patterns) {
    redefineClassesPatterns.addAll(asList(patterns));
    return this;
  }

  public IsolatingClassloader delegating(String pattern, ClassLoader delegate) {
    delegates.put(pattern, delegate);
    return this;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    sb.append(id);
    sb.append(", loaded=");
    sb.append(loadedClasses.size());
    sb.append("]");
    return sb.toString();
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
      var delegate = delegates.entrySet().stream() //
              .filter(e -> name.matches(e.getKey())) //
              .map(Entry::getValue) //
              .findFirst();
      if (delegate.isPresent()) {
        return delegate.get().loadClass(name);
      }

      if (hideClassesPatterns.stream().anyMatch(name::matches)) {
        log.debug("[{}] prevented access to hidden class: {}", id, name);
        throw new ClassNotFoundException(name);
      }

      if (redefineClassesPatterns.stream().anyMatch(name::matches)) {
        Class<?> loadedClass = loadedClasses.get(name);
        if (loadedClass != null) {
          return loadedClass;
        }

        log.debug("[{}] redefining class: {}", id, name);

        String internalName = name.replace(".", "/") + ".class";
        InputStream is = getParent().getResourceAsStream(internalName);
        if (is == null) {
          throw new ClassNotFoundException(name);
        }

        try {
          var buffer = new ByteArrayOutputStream();
          is.transferTo(buffer);
          byte[] bytes = buffer.toByteArray();
          Class<?> cls = defineClass(name, bytes, 0, bytes.length);
          if (cls.getPackage() == null) {
            int packageSeparator = name.lastIndexOf('.');
            if (packageSeparator != -1) {
              String packageName = name.substring(0, packageSeparator);
              definePackage(packageName, null, null, null, null, null, null, null);
            }
          }
          loadedClasses.put(name, cls);
          return cls;
        } catch (IOException ex) {
          throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
        }
      }

      return super.loadClass(name, resolve);
    }
  }
}
