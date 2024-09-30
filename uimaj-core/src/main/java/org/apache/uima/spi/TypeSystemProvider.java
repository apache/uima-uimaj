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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;

/**
 * Allows the UIMA framework to discover globally available resources such as JCas classes, type
 * system descriptions, etc.
 * 
 * @see TypeSystemProvider_ImplBase
 */
@SuppressWarnings("deprecation")
public interface TypeSystemProvider extends TypeSystemDescriptionProvider, JCasClassProvider,
        FsIndexCollectionProvider, TypePrioritiesProvider {

  /**
   * @return the type system descriptions exported by this provider. The provider should resolve any
   *         imports in these before returning them. If possible, the provider should internally
   *         cache the parsed type systems instead of parsing them on every call to this method.
   */
  @Override
  default List<TypeSystemDescription> listTypeSystemDescriptions() {
    return Collections.emptyList();
  }

  /**
   * @return the FS index collections exported by this provider. The provider should resolve any
   *         imports in these before returning them. If possible, the provider should internally
   *         cache the parsed type systems instead of parsing them on every call to this method.
   */
  @Override
  default List<FsIndexCollection> listFsIndexCollections() {
    return Collections.emptyList();
  }

  /**
   * @return the type priorities exported by this provider. The provider should resolve any imports
   *         in these before returning them. If possible, the provider should internally cache the
   *         parsed type systems instead of parsing them on every call to this method.
   */
  @Override
  default List<TypePriorities> listTypePriorities() {
    return Collections.emptyList();
  }

  /**
   * @return the JCas classes exported by this provider. The provider should only supply JCas
   *         classes for types it exports via {@link #listTypeSystemDescriptions()}. If possible,
   *         the provider should internally cache the parsed type systems instead of parsing them on
   *         every call to this method.
   */
  @Override
  default List<Class<? extends TOP>> listJCasClasses() {
    return Collections.emptyList();
  }

  /**
   * Returns the URL for a resource exported by this provider by name. This is necessary if any
   * descriptors outside of this provider import resources owned by this provider by name. Note that
   * by convention, any descriptors returned by this provide via
   * {@link #listTypeSystemDescriptions()}, {@link #listTypePriorities()} and
   * {@link #listFsIndexCollections()} should already have resolved their imports, so for these
   * methods to access local imports, implementing this method is not required.
   * 
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
