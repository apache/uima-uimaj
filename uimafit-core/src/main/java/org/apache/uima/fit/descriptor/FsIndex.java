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
package org.apache.uima.fit.descriptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.uima.fit.factory.FsIndexFactory;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;

/**
 * @see FsIndexDescription
 * @see FsIndexFactory
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FsIndex {
  /**
   * @see FsIndexDescription#KIND_SORTED
   */
  public static final String KIND_SORTED = FsIndexDescription.KIND_SORTED;

  /**
   * @see FsIndexDescription#KIND_SET
   */
  public static final String KIND_SET = FsIndexDescription.KIND_SET;

  /**
   * @see FsIndexDescription#KIND_BAG
   */
  public static final String KIND_BAG = FsIndexDescription.KIND_BAG;

  /**
   * @see FsIndexDescription#getLabel()
   * 
   * @return the label of this index
   */
  String label();

  /**
   * The type of the index as name. As an alternative, the index can be defined using a class with
   * {@link #type()}. One method or the other must be used to set the index type.
   * 
   * @see FsIndexDescription#getTypeName()
   * 
   * @return the type name for this index
   */
  String typeName() default NO_NAME_TYPE_SET;

  /**
   * The type of the index as class. As an alternative, the index can be defined using a type name
   * with {@link #typeName()}. One method or the other must be used to set the index type.
   * 
   * @see FsIndexDescription#getTypeName()
   * 
   * @return the type for this index
   */
  Class<? extends TOP> type() default NoClassSet.class;

  /**
   * @see FsIndexDescription#getKind()
   * 
   * @return the kind of index
   */
  String kind() default KIND_BAG;

  /**
   * @see FsIndexDescription#getKeys()
   * 
   * @return the keys for this index
   */
  FsIndexKey[] keys() default {};

  /**
   * @see FsIndexKeyDescription#isTypePriority()
   * 
   * @return true if and only if this is a type priority key
   */
  boolean typePriorities() default true;

  /**
   * Indicates that no type has been set.
   */
  public static final class NoClassSet extends TOP { /* Nothing */
  }

  /**
   * Indicated that no type name has been set.
   */
  public static final String NO_NAME_TYPE_SET = "org.apache.uima.fit.descriptor.FsIndex.NO_NAME_TYPE_SET";

}
