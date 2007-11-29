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

package org.apache.uima.resource;

/**
 * The <code>SharedResourceObject</code> interface must be implemented by all classes that provide
 * access to resource data. Object that implement this interface may be made accessible to
 * Annotators via the {@link org.apache.uima.resource.ResourceManager}.
 * <p>
 * This interface's {@link #load(DataResource)} method is called by the ResourceManager after the
 * <code>SharedResourceObject</code> has been instantiated. A {@link DataResource} is passes as a
 * parameter to this method. The implementation of the <code>load</code> method should read the
 * data from the <code>DataResource</code> and use that data to initialize this object.
 * 
 * 
 */
public interface SharedResourceObject {

  /**
   * Called by the {@link org.apache.uima.resource.ResourceManager} after this object has been
   * instantiated. The implementation of this method should read the data from the specified
   * <code>DataResource</code> and use that data to initialize this object.
   * 
   * @param aData
   *          a <code>DataResource</code> that provides access to the data for this resource
   *          object.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurs during loading.
   */
  public void load(DataResource aData) throws ResourceInitializationException;
}
