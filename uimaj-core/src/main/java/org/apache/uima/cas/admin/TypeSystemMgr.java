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

package org.apache.uima.cas.admin;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;

/**
 * Writable version of type system.
 * 
 * 
 */
public interface TypeSystemMgr extends TypeSystem {

  /**
   * Add a new type to the type system.
   * 
   * @param typeName
   *          The name of the new type.
   * @param mother
   *          The type node under which the new type should be attached. This must not be null.
   * @return The new type, or <code>null</code> if <code>typeName</code> is already in use (so
   *         check for null return values).
   * @exception CASAdminException
   *              If <code>typeName</code> is not a legal type name, type system is locked, or
   *              <code>mother</code> is inheritance final.
   */
  Type addType(String typeName, Type mother) throws CASAdminException;

  /**
   * Inherit from String. The only way you can inherit from String is by providing a restriction to
   * a set of strings that are possible values for features of this type. This restriction will be
   * checked when such feature values are set. Note that you can not introduce any features on such
   * types, nor can you subtype them any further.
   * 
   * @param typeName
   *          The name of the type to be created.
   * @param stringList
   *          The list of legal string values for this string type.
   * @return The resulting type, or <code>null</code> if the type is already defined.
   * @throws CASAdminException
   *           If the type system is locked.
   */
  Type addStringSubtype(String typeName, String[] stringList) throws CASAdminException;

  /**
   * Add an feature to the type system.
   * 
   * @param featureName
   *          The name of the new feature.
   * @param domainType
   *          The type that defines the domain of the feature.
   * @param rangeType
   *          The type that defines the range of the feature.
   * @return The new feature object, or <code>null</code> if <code>featureName</code> is already
   *         in use for <code>domainType</code> with the same range (if the range is different, an
   *         exception is thrown).
   * @exception CASAdminException
   *              If <code>featureName</code> is not a legal feature name, the type system is
   *              locked or <code>domainType</code> is feature final. Also if
   *              <code>featureName</code> has already been defined on <code>domainType</code>
   *              (or a supertype) with a different range than <code>rangeType</code>.
   */
  Feature addFeature(String featureName, Type domainType, Type rangeType) throws CASAdminException;

  /**
   * Add an feature to the type system.
   * 
   * @param featureName
   *          The name of the new feature.
   * @param domainType
   *          The type that defines the domain of the feature.
   * @param rangeType
   *          The type that defines the range of the feature.
   * @param multipleReferencesAllowed
   *          If the <code>rangeType</code> is an array type, you can use this flag to enforce
   *          that the feature value is not referenced anywhere else. This is currently only used
   *          for XMI serialization. Defaults to <code>true</code>.
   * @return The new feature object, or <code>null</code> if <code>featureName</code> is already
   *         in use for <code>domainType</code> with the same range (if the range is different, an
   *         exception is thrown).
   * @exception CASAdminException
   *              If <code>featureName</code> is not a legal feature name, the type system is
   *              locked or <code>domainType</code> is feature final. Also if
   *              <code>featureName</code> has already been defined on <code>domainType</code>
   *              (or a supertype) with a different range than <code>rangeType</code>.
   */
  Feature addFeature(String featureName, Type domainType, Type rangeType,
          boolean multipleReferencesAllowed) throws CASAdminException;

  /**
   * Commit the type system. The type system will be locked and no longer writable. WARNING: Users
   * should not call this, but instead call ((CASImpl) theAssociatedCAS).commitTypeSystem() in order
   * to set up the parts of the CAS that should be set up when the type system is committed.
   */
  void commit();

  /**
   * Check if this instance has been committed.
   * 
   * @return <code>true</code> iff this instance has been committed.
   */
  boolean isCommitted();

  /**
   * Make type feature final in the sense that no new features can be added to this type. Note that
   * making a type feature final automatically makes all ancestors of that type feature final as
   * well.
   * 
   * @param type
   *          The type to be made feature final.
   */
  void setFeatureFinal(Type type);

  /**
   * Block any further inheritance from this type. Does not mean that the type can have no
   * sub-types, just that no new ones can be introduced.
   * 
   * @param type the type to block subtypes on
   */
  void setInheritanceFinal(Type type);

}
