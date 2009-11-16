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

import org.apache.uima.resource.metadata.MetaDataObject;

/**
 * A <code>ResourceSpecifier</code> contains information that can be used acquire a reference to a
 * {@link Resource}, whether that is done by instantiating the resource locally or locating an
 * existing resource available as a service.
 * <p>
 * It is the job of the {@link org.apache.uima.ResourceFactory} to locate or create the
 * {@link Resource} that is specified by a <code>ResourceSpecifier</code>.
 * <p>
 * This interface itself does nothing. It serves as a common parent for different types of Resource
 * Specifiers.
 * 
 * 
 */
public interface ResourceSpecifier extends MetaDataObject {
}
