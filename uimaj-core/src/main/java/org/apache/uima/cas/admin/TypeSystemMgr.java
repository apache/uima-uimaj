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
 * <p>Writable version of type system.</p>
 * 
 * <p>Public API for the UIMA Type System during pipe-line startup, while
 * type system is being constructed from merge of type specifications of components.</p>
 * 
 * <p>For use by applications, not for use by annotator components (because they work with 
 * merged type system).</p>
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
   * Note: A subtype may define a feature that is also defined by a supertype. 
   *   If the supertype definition is already present, then the subtype definition is 
   *   "merged":  using the merging criteria: ranges must match.
   *      Different isMultipleReferencesAllowed settings are OK;
   *      The supertype's setting takes precedence.
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
   * Commit the type system, and load JCas Classes from the UIMA Framework's classloader. 
   * The type system will be locked and no longer writable. 
   * 
   * WARNING: Users
   * should not call this, but instead call ((CASImpl) theAssociatedCAS).commitTypeSystem() in order
   * to set up the parts of the CAS that should be set up when the type system is committed.
   * 
   * WARNING: This API will use the UIMA Framework's class loader to find a load JCas classes.
   * If you have JCas classes under some other class loader you wish to use (perhaps you are 
   * setting a ResourceManager's extension classpath, which creates a class loader), use the
   * commit which takes a class loader as an argument, and pass in the class loader where the
   * JCas classes are.  
   * 
   * @return the committed type system.  Note that this may be the same object as "this" or a 
   *         different (but equal) object.  Type systems are cached and recreating the exact same type system
   *         repeatedly will return the original one.
   */
  TypeSystem commit();
  
  /**
   * Commit the type system, and load JCas classes from the passed in classloader. 
   * The type system will be locked and no longer writable. 
   * 
   * WARNING: Users
   * should not call this, but instead call ((CASImpl) theAssociatedCAS).commitTypeSystem() in order
   * to set up the parts of the CAS that should be set up when the type system is committed.
   * 
   * @param cl the JCas class loader
   *  
   * @return the committed type system.  Note that this may be the same object as "this" or a 
   *         different (but equal) object.  Type systems are cached and recreating the exact same type system
   *         repeatedly will return the original one.
   */
  TypeSystem commit(ClassLoader cl);

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
