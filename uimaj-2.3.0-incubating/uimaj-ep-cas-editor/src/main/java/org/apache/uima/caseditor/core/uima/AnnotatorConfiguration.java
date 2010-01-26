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

package org.apache.uima.caseditor.core.uima;

import java.net.MalformedURLException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.caseditor.core.TaeError;
import org.apache.uima.caseditor.core.model.AnnotatorElement;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.eclipse.core.resources.IFolder;

/**
 * TODO: add java doc here
 */
public class AnnotatorConfiguration {
  private ResourceSpecifier mDescriptor;

  private IFolder mResourceBasePath;

  private final AnnotatorElement element;

  /**
   * Initializes the instance.
   * 
   * @param descriptor
   */
  public AnnotatorConfiguration(AnnotatorElement element, ResourceSpecifier descriptor) {
    this.element = element;
    mDescriptor = descriptor;
  }

  public AnnotatorElement getAnnotatorElement() {
    return element;
  }

  /**
   * Only text analysis engines are supported.
   * 
   * @return the text analysis engine
   * @throws ResourceInitializationException
   */
  public AnalysisEngine createAnnotator() throws ResourceInitializationException {
    ResourceManager resourceManager = UIMAFramework.newDefaultResourceManager();

    if (mResourceBasePath.getLocation() != null) {
      try {
        resourceManager.setDataPath(mResourceBasePath.getLocation().toOSString());
      } catch (MalformedURLException e) {
        // this will not happen
        throw new TaeError("Unexpexted exceptioon", e);
      }
    }

    return UIMAFramework.produceAnalysisEngine(mDescriptor, resourceManager, null);
  }

  public IFolder getBaseFolder() {
    return mResourceBasePath;
  }

  /**
   * Sets the base folder
   * 
   * @param baseFolder
   */
  public void setBaseFolder(IFolder baseFolder) {
    mResourceBasePath = baseFolder;
  }
}
