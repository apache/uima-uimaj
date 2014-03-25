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

package org.apache.uima.resource.metadata;

/**
 * Binds an {@link org.apache.uima.resource.ExternalResourceDependency} to an
 * {@link org.apache.uima.resource.ExternalResourceDescription}. The biding has two parts - the
 * {@link #getKey() key}, which indicates the resource dependency being bound, and the
 * {@link #getResourceName() resource name}, which identifies the resource to which that dependency
 * is bound.
 * 
 * 
 */
public interface ExternalResourceBinding extends MetaDataObject {

  public final static ExternalResourceBinding[] EMPTY_RESOURCE_BINDINGS = new ExternalResourceBinding[0];
  /**
   * Retrieves the key that identifies the
   * {@link org.apache.uima.resource.ExternalResourceDependency} being bound. If this binding is
   * declared in a primitive component, this is exactly the same key as is specified in the
   * {@link org.apache.uima.resource.ExternalResourceDependency}.
   * <p>
   * Within an aggregate, a slash-separated name is used to identify which component the binding
   * applies to. For example, if an Aggregate AE contains an annotator with key
   * <code>annotator1</code> which declares a resource dependency <code>myResource</code>, that
   * Aggregate AE could binding that resource dependency by using the key
   * <code>annotator1/myResource</code>.
   * 
   * @return the key for this resource binding.
   */
  public String getKey();

  /**
   * Sets the key that identifies the {@link org.apache.uima.resource.ExternalResourceDependency}
   * being bound. If this binding is declared in a primitive component, this is exactly the same key
   * as is specified in the {@link org.apache.uima.resource.ExternalResourceDependency}.
   * <p>
   * Within an aggregate, a slash-separated name is used to identify which component the binding
   * applies to. For example, if an Aggregate AE contains an annotator with key
   * <code>annotator1</code> which declares a resource dependency <code>myResource</code>, that
   * Aggregate AE could binding that resource dependency by using the key
   * <code>annotator1/myResource</code>.
   * 
   * @param aKey
   *          the key for this resource binding.
   */
  public void setKey(String aKey);

  /**
   * Retrieves the name of the actual Resource instance that will satisfy this dependency. This name
   * must match one of the names specified in an
   * {@link org.apache.uima.resource.ExternalResourceDescription} within the enclosing
   * {@link ResourceManagerConfiguration} object.
   * 
   * @return the name of the resource satisfying this dependency.
   */
  public String getResourceName();

  /**
   * Sets the name of the actual Resource instance that will satisfy this dependency. This name must
   * match one of the names specified in an
   * {@link org.apache.uima.resource.ExternalResourceDescription} within the enclosing
   * {@link ResourceManagerConfiguration} object.
   * 
   * @param aName
   *          the name of the resource satisfying this dependency.
   */
  public void setResourceName(String aName);

}
