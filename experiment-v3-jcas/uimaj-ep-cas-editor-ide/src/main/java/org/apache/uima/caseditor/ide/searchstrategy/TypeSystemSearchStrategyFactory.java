/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.caseditor.ide.searchstrategy;

import java.util.Map;
import java.util.TreeMap;

import org.apache.uima.caseditor.CasEditorPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

public class TypeSystemSearchStrategyFactory {

  private static final String SEARCH_STRATEGY_EXTENSION = "org.apache.uima.caseditor.ide.searchstrategy";

  private static TypeSystemSearchStrategyFactory instance;

  private Map<Integer, ITypeSystemSearchStrategy> searchStrategies = new TreeMap<Integer, ITypeSystemSearchStrategy>();

  private TypeSystemSearchStrategyFactory() {

    IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(
            SEARCH_STRATEGY_EXTENSION);

    for (IConfigurationElement element : config) {

      if ("searchStrategy".equals(element.getName())) {

        // extract id element
        String id = element.getAttribute("id");
        String priority = element.getAttribute("priority");

        Object searchStrategyObject;
        try {
          searchStrategyObject = element.createExecutableExtension("class");
        } catch (CoreException e) {
          CasEditorPlugin.log("Failed to load search strategy with id: " + id, e);
          searchStrategyObject = null;
        }

        if (searchStrategyObject instanceof ITypeSystemSearchStrategy) {
          searchStrategies.put(Integer.parseInt(priority),
                  (ITypeSystemSearchStrategy) searchStrategyObject);
        }
      }
    }

  }

  public static TypeSystemSearchStrategyFactory instance() {

    if (instance == null) {
      instance = new TypeSystemSearchStrategyFactory();
    }

    return instance;
  }

  public Map<Integer, ITypeSystemSearchStrategy> getSearchStrategies() {
    return searchStrategies;
  }

}
