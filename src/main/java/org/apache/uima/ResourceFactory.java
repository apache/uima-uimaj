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

import java.util.Map;

import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;

/**
 * A <code>ResourceFactory</code> is used to acquire instances of {@link Resource}s.
 * <p>
 * The method {@link #produceResource(Class,ResourceSpecifier,Map)} is used to produce an instance
 * of a <code>Resource</code>.  The input to this method
 * is a {@link ResourceSpecifier}, which contains all of the information that
 * this factory can use to acquire a reference to the resource.
 * <p>
 * Most applications will not need to interact with this interface.
 * Applications will generally create resources using the static methods on
 * the {@link org.apache.uima.UIMAFramework} class.
 * <p>
 * Note that multiple threads may attempt to manufacture resources
 * simultaneously, so implementations of this interface MUST be threadsafe.
 * <p>
 * Developers needing to provide their own <code>ResourceFactory</code> may
 * wish to consider using the provided 
 * {@link org.apache.uima.util.SimpleResourceFactory} implementation.
 *  
 *
 */
public interface ResourceFactory {
  /**
   * Produces an appropriate <code>Resource</code> instance from a <code>ResourceSpecifier</code>.
   * This version of <code>produceResource</code> takes a Map containing additional parameters to
   * be passed to the {@link Resource#initialize(ResourceSpecifier,Map)} method.
   * 
   * @param aResourceClass
   *          the class of resource to be produced (NOTE: this is intended to be a standard UIMA
   *          interface name such as "TextAnalysisEngine" or "ASB")
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   * @param aAdditionalParams
   *          a Map containing additional parameters to pass to the
   *          {@link Resource#initialize(ResourceSpecifier,Map)} method. May be <code>null</code>
   *          if there are no parameters.
   * 
   * @return a <code>Resource</code> instance. Returns <code>null</code> if this factory does
   *         not know how to create a Resource from the <code>ResourceSpecifier</code> provided.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource
   */
  public Resource produceResource(Class<? extends Resource> aResourceClass, ResourceSpecifier aSpecifier,
          Map<String, Object> aAdditionalParams) throws ResourceInitializationException;

}
