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

package org.apache.uima.cas;

/**
 * A feature structure constraint parser factory.
 * 
 * @deprecated The Constraint Parser is not supported in externally released versions of UIMA
 */
public class ConstraintParserFactory {

  /**
   * 
   */
  private ConstraintParserFactory() {
    super();
  }

  public static ConstraintParser getDefaultConstraintParser() throws ClassNotFoundException,
                  InstantiationException, IllegalAccessException {
    return getConstraintParser("org.apache.uima.cas.constraint_compiler.SableConstraintParser");
  }

  public static ConstraintParser getConstraintParser(String className)
                  throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    ClassLoader loader = getAvailableClassLoader();
    if (loader == null) {
      throw new ClassNotFoundException("No class loader accessible.");
    }
    Class cpClass = loader.loadClass(className);
    return (ConstraintParser) cpClass.newInstance();
  }

  private static ClassLoader getAvailableClassLoader() {
    ClassLoader loader = ConstraintParserFactory.class.getClassLoader();
    if (loader == null) {
      loader = Thread.currentThread().getContextClassLoader();
    }
    return loader;
  }

}
