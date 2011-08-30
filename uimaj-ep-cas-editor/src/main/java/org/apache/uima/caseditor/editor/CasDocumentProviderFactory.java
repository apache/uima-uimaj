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

package org.apache.uima.caseditor.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.caseditor.CasEditorPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IEditorInput;

/**
 * Factory class to produce and lookup an appropriate document provider.
 */
class CasDocumentProviderFactory {

  // TODO: Change to: .documentProviders
  private static final String CAS_EDITOR_EXTENSION = "org.apache.uima.caseditor.editor";

  private static CasDocumentProviderFactory instance;

  // map class_name to provider
  private Map<String, CasDocumentProvider> documentProviders = new HashMap<String, CasDocumentProvider>();
  
  CasDocumentProviderFactory() {

    IConfigurationElement[] config =
            Platform.getExtensionRegistry().getConfigurationElementsFor(CAS_EDITOR_EXTENSION);

    for (IConfigurationElement element : config) {

      if ("provider".equals(element.getName())) {

        // extract id element
        String id = element.getAttribute("id");
        String inputType = element.getAttribute("inputType");
        
        Object documentProviderObject;
        try {
          documentProviderObject = element.createExecutableExtension("class");
        } catch (CoreException e) {
          CasEditorPlugin.log("Failed to load doucment provider with id: " + id, e);
          documentProviderObject = null;
        }
        
        if (documentProviderObject instanceof CasDocumentProvider) {
          documentProviders.put(inputType, (CasDocumentProvider) documentProviderObject);
        }
      }
    }

  }

  /**
   * Looks up a document provider for the provided editor input.
   * The editor input type must be cast-able to the specified inputType.
   * The implementation tries first to map class types, and then interface types.
   * 
   * @param input 
   * 
   * @return
   */
  CasDocumentProvider getDocumentProvider(IEditorInput input) {

    // A class can have many types, they are defined by super classes
    // and implemented interfaces
    
    // First try to match the input type to the editor input type
    // or one of its super class
    List<Class<?>> classList = new ArrayList<Class<?>>();
    
    for (Class<?> inputClass = input.getClass(); inputClass != null;) {
      classList.add(inputClass);
      inputClass = inputClass.getSuperclass();
    }
    
    CasDocumentProvider provider = null;
    for (Class<?> inputClass : classList) {
      provider = documentProviders.get(inputClass.getName());
      
      if (provider != null)
        return provider;
    }
    
    // Now try to match an implemented interface to the input type
    // either of the editor input class or for one of its super classes
    if (provider == null) {
      for (Class<?> inputClass : classList) {
        for (Class<?> inputClassInterface : inputClass.getInterfaces()) {
          provider = documentProviders.get(inputClassInterface.getName());
          
          if (provider != null)
            return provider;
        }
      }
    }
    
    return provider;
  }

  static CasDocumentProviderFactory instance() {

    if (instance == null) {
      instance = new CasDocumentProviderFactory();
    }

    return instance;
  }
}
