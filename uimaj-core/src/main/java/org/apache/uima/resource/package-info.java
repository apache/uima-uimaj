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
/**
 * Interfaces for <code>Resources</code> and <code>ResourceSpecifiers</code>.
 * <p>
 * {@link org.apache.uima.resource.Resource} is the general term for all UIMA components
 * that can be acquired and used by an application (or by other resources).
 * <p>
 * <code>Resource</code>s may be co-located with their client or distributed
 * as services.  This is made transparent to the client.
 * </p>
 * <p>
 * A {@link org.apache.uima.resource.ResourceSpecifier} contains information that can be
 * used to acquire a reference to a <code>Resource</code>, whether that is done by
 * instantiating the resource locally or locating an existing resource
 * available as a service.
 * </p>
 * <p>
 * The {@link org.apache.uima.ResourceFactory} takes a <code>ResourceSpecifier</code> and
 * returns an instance of the specified <code>Resource</code>.  Again, this can be
 * done by creating the instance or by locating an existing instance.
 * </p>
 * <p>
 * Most applications will not need to deal with this abstract
 * <code>Resource</code> interface.  UIMA Developers who need to introduce
 * new types of Resources, however, will need to implement the interfaces
 * in this package.
 * </p>
 */
package org.apache.uima.resource;