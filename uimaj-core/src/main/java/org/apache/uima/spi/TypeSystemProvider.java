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
package org.apache.uima.spi;

import static java.util.Optional.empty;

import java.net.URL;
import java.util.Optional;

import org.apache.uima.resource.metadata.Import;

public interface TypeSystemProvider extends TypeSystemDescriptionProvider, JCasClassProvider,
        FsIndexCollectionProvider, TypePrioritiesProvider {

  /**
   * @param aName
   *          the name of the type system. This should be something like
   *          {@code some.package.TypeSystem} just as you would put it into the
   *          {@link Import#setName(String)} of a {@link Import} for a name-based import.
   * @return the type system description(s) exported from the given location. The provider should
   *         resolve any imports in the type systems before returning them.
   * 
   * @apiNote For backwards compatibility, this is currently a default method, so implementations do
   *          not have to provide it - although by-name importing type systems provided via SPIs
   *          will not work then unless the type systems are also simply available via the
   *          classpath. However, in the next major version, the default implementation will be
   *          removed so implementers have to provide it.a
   * @forRemoval 4.0.0 (just the default implementation)
   * @since 3.6.0
   */
  default Optional<URL> findResourceUrl(String aName) {
    return empty();
  }
}
