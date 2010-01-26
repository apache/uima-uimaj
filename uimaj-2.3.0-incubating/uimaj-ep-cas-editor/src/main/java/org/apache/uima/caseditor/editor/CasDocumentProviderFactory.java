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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

class CasDocumentProviderFactory {

  private static final String CAS_EDITOR_EXTENSION = "org.apache.uima.caseditor.editor";

  private static CasDocumentProviderFactory instance;

  private CasDocumentProvider documentProvider;

  CasDocumentProviderFactory() {

    IConfigurationElement[] config =
            Platform.getExtensionRegistry().getConfigurationElementsFor(CAS_EDITOR_EXTENSION);

    for (IConfigurationElement e : config) {

      if ("provider".equals(e.getName())) {

        Object o;
        try {
          o = e.createExecutableExtension("class");
        } catch (CoreException e1) {
          // TODO: Log error, extension point was not specified correctly !!!
          e1.printStackTrace();
          o = null;
        }

        if (o instanceof CasDocumentProvider) {
          documentProvider = (CasDocumentProvider) o;
        }
      }
    }

  }

  CasDocumentProvider getDocumentProvider() {
    return documentProvider;
  }

  static CasDocumentProviderFactory instance() {

    if (instance == null) {
      instance = new CasDocumentProviderFactory();
    }

    return instance;
  }
}
