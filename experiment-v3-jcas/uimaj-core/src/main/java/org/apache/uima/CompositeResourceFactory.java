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

package org.apache.uima;

import org.apache.uima.resource.ResourceSpecifier;

/**
 * A type of {@link ResourceFactory} that produces resources by delegating to other Resource
 * Factories.
 * <p>
 * Resource Factories are registered with the composite factory by calling the
 * {@link #registerFactory(Class,ResourceFactory)} method. The type of
 * {@link org.apache.uima.resource.ResourceSpecifier} that the factory can handle is passed to this
 * method. In the event that more than one <code>ResourceFactory</code> is registered for the same
 * Resource Specifier class, the most recently registered factory will be tried first.
 * 
 * 
 */
public interface CompositeResourceFactory extends ResourceFactory {
  /**
   * Registers a ResourceFactory with this composite factory.
   * 
   * @param aResourceSpecifierInterface
   *          the subinterface of {@link org.apache.uima.resource.ResourceSpecifier} that the
   *          factory can handle
   * @param aFactory
   *          the factory used to create resources from resource specifiers of the given type
   */
  public void registerFactory(Class<? extends ResourceSpecifier> aResourceSpecifierInterface, ResourceFactory aFactory);
}
