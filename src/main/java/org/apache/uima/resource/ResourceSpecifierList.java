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

import java.util.List;

/**
 * A type of <code>ResourceSpecifier</code> that is an aggregate of other
 * <code>ResourceSpecifier</code>s. When attempting to produce a resource using a
 * <code>ResourceSpecifierList</code>, the <code>ResourceFactory</code> will try each
 * constituent <code>ResourceSpecifier</code>, in order. The first <code>Resource</code> that
 * is successfully produced will be returned to the caller.
 * <p>
 * <code>ResourceSpecifierList</code> allows applications to attempt to locate a resource and
 * then, if that fails, to construct a new instance of the resource.
 * 
 * 
 */
public interface ResourceSpecifierList extends ResourceSpecifier {

  /**
   * Retrieves the constituent <code>ResourceSpecifiers</code> that comprise this aggregate
   * <code>ResourceSpecifierList</code>.
   * 
   * @return an unmodifiable List of {@link ResourceSpecifier}s.
   */
  public List<ResourceSpecifier> getResourceSpecifiers();
}
