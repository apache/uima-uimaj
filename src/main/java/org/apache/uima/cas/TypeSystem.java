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

package org.apache.uima.cas;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.uima.cas.impl.LowLevelTypeSystem;

/**
 * The interface to the type system. Used to access information about existing
 * {@link org.apache.uima.cas.Type types} and {@link org.apache.uima.cas.Feature features} using
 * their String identifiers. This is a pure access interface. Types and features are defined using
 * Component Descriptors, written in XML.
 * 
 * <p>
 * Get the type system from a {@link CAS CAS} object with {@link CAS#getTypeSystem getTypeSystem()}.
 * 
 * <p>
 * There are a few methods to list the existing types in a type system. Information about which
 * feature is appropriate for which type is available through the {@link Type Type} and
 * {@link Feature Feature} classes.
 * 
 * 
 */
public interface TypeSystem {

  /**
   * This is the character that separates a type name from a feature name. Ex.:
   * <code>uima.cas.Annotation:begin</code>.
   */
  public static final char FEATURE_SEPARATOR = ':';

  /**
   * This is the character that separates name spaces. Ex.: <code>uima.cas.Annotation</code>
   */
  public static final char NAMESPACE_SEPARATOR = '.';

  /**
   * Get a type object for a given type name. See documentation on <a href="./Type.html#names">type
   * names</a>.
   * 
   * @param typeName
   *          The name of the type.
   * @return A type object, or <code>null</code> if no such type exists.
   */
  Type getType(String typeName);

  /**
   * Obtain an array type with component type <code>componentType</code>.
   * 
   * @param componentType
   *          The type of the elements of the resulting array type. This can be any type, even
   *          another array type.
   * @return The array type with the corresponding component type.
   */
  Type getArrayType(Type componentType);

  /**
   * Get a feature object for a given name. See documentation on <a
   * href="./Feature.html#names">feature names</a>.
   * 
   * @param featureName
   *          The fully qualified name of the feature.
   * @return An feature object, or <code>null</code> if no such feature exists.
   */
  Feature getFeatureByFullName(String featureName);

  /**
   * Get an iterator over all types, in no particular order.
   * 
   * @return The iterator.
   */
  Iterator<Type> getTypeIterator();

  /**
   * Get the top type, i.e., the root of the type system.
   * 
   * @return The top type.
   */
  Type getTopType();

  /**
   * Get a vector of the types directly subsumed by a given type.
   * 
   * @param type
   *          The input type.
   * @return A vector of the directly subsumed types.
   * @deprecated Use {@link #getDirectSubtypes(Type) getDirectSubtypes(Type)} instead.
   */
  @Deprecated
  Vector<Type> getDirectlySubsumedTypes(Type type);

  /**
   * Get a List of the types directly subsumed by a given type.
   * 
   * @param type
   *          The input type.
   * @return A List of the directly subsumed types.
   */
  List<Type> getDirectSubtypes(Type type);

  /**
   * Return the list of all types subsumed by the input type. Note: the list does not include the
   * type itself.
   * 
   * @param type
   *          Input type.
   * @return The list of types subsumed by <code>type</code>.
   */
  List<Type> getProperlySubsumedTypes(Type type);

  /**
   * Get the parent type for input type.
   * 
   * @param type
   *          The type we want to know the parent of.
   * @return The parent type, or <code>null</code> for the top type.
   */
  Type getParent(Type type);

  /**
   * Does one type inherit from the other?
   * 
   * @param superType
   *          Supertype.
   * @param subType
   *          Subtype.
   * @return <code>true</code> iff <code>sub</code> inherits from <code>super</code>.
   */
  boolean subsumes(Type superType, Type subType);

  /**
   * Get a list of features, in no particular order.
   * 
   * @return An iterator over the features.
   */
  Iterator<Feature> getFeatures();

  /**
   * Create a type name space object for the name parameter.
   * 
   * @param name
   *          The name of the name space.
   * @return A <code>TypeNameSpace</code> object corresponding to <code>name</code>, or
   *         <code>null</code>, if <code>name</code> is not a legal type name space identifier.
   */
  TypeNameSpace getTypeNameSpace(String name);

  /**
   * Return the low-level view of this type system.
   * 
   * @return The {@link org.apache.uima.cas.impl.LowLevelTypeSystem LowLevelTypeSystem} version of
   *         this type system.
   */
  LowLevelTypeSystem getLowLevelTypeSystem();

}
