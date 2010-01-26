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
import org.apache.uima.caseditor.core.TaeError;
import org.apache.uima.caseditor.core.model.ConsumerElement;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.eclipse.core.resources.IFolder;

/**
 * TODO: add javadoc here
 */
public final class CasConsumerConfiguration {
  private CasConsumerDescription casConsumerDescriptor;

  private IFolder resourceBasePath;

  private ConsumerElement consumerElement;

  /**
   * Initializes a new instance.
   * 
   * @param element
   * 
   * @param descriptor
   */
  public CasConsumerConfiguration(ConsumerElement element, CasConsumerDescription descriptor) {
    casConsumerDescriptor = descriptor;
    consumerElement = element;
  }

  /**
   * Retrieves the name.
   * 
   * @return the name
   */
  public String getName() {
    ProcessingResourceMetaData trainerMetaData = casConsumerDescriptor.getCasConsumerMetaData();

    return trainerMetaData.getName();
  }

  /**
   * Sets the base folder.
   * 
   * @param baseFolder
   */
  public void setBaseFolder(IFolder baseFolder) {
    resourceBasePath = baseFolder;
  }

  public IFolder getBaseFolder() {
    return resourceBasePath;
  }

  /**
   * Creates the consumer.
   * 
   * @return the consumer
   */
  public CasConsumer createConsumer() {
    ResourceManager resourceManager = UIMAFramework.newDefaultResourceManager();

    if (resourceBasePath.getLocation() != null) {
      try {
        resourceManager.setExtensionClassPath(resourceBasePath.getLocation().toOSString(), true);
        resourceManager.setDataPath(resourceBasePath.getLocation().toOSString());
      } catch (MalformedURLException e) {
        // this should never happen
        throw new TaeError("Unexpected exception", e);
      }
    }

    try {
      CasConsumer consumer =
              UIMAFramework.produceCasConsumer(casConsumerDescriptor, resourceManager, null);

      return consumer;
    } catch (ResourceInitializationException e) {

      e.printStackTrace();
      return null;
    }
  }

  /**
   * Retrieves the {@link ConsumerElement}.
   * 
   * @return the {@link ConsumerElement}
   */
  public ConsumerElement getConsumerElement() {
    return consumerElement;
  }
}
