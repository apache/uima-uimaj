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
package org.apache.uima.fit.internal;

import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;

/**
 * INTERNAL API - Helper resource used when an {@link ExternalResource} annotation is used on an
 * array or collection field. 
 */
public class ResourceList extends Resource_ImplBase {
  public static final String ELEMENT_KEY = "ELEMENT";
  
  public static final String PARAM_SIZE = "size";
  @ConfigurationParameter(name = PARAM_SIZE, mandatory = true)
  private int size;

  public ResourceList() {
    // Nothing to do
  }
  
  public int getSize() {
    return size;
  }
}
