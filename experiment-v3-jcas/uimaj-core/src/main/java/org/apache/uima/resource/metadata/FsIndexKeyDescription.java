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

package org.apache.uima.resource.metadata;

/**
 * A description of a key defining a CAS feature structure index. This implements
 * <code>MetaDataObject</code>, which implements {@link org.apache.uima.util.XMLizable}, so it
 * can be serialized to and deserialized from an XML element.
 * 
 * 
 */
public interface FsIndexKeyDescription extends MetaDataObject {
  /**
   * Comparator that orders FeatureStructures according to the standard order of their key features.
   * For integer and float values, this is the standard linear order, and for strings it is
   * lexicographic order.
   */
  public static final int STANDARD_COMPARE = 0;

  /**
   * Comparator that orders FeatureStructures according to the reverse order of their key features
   * (the opposite order as that defined by STANDARD_COMPARE.
   */
  public static final int REVERSE_STANDARD_COMPARE = 1;

  /**
   * Gets whether this is a special "type priority" key. A type priority key indicates that the
   * ordering of FeatureStructures in the index is based on the {@link TypePriorities} defined for
   * that Analysis Engine.
   * <p>
   * Type priority keys ignore any values assigned to the {@link #getFeatureName() featureName} and
   * {@link #getComparator() comparator} properties.
   * 
   * @return true if and only if this is a type priority key
   */
  public boolean isTypePriority();

  /**
   * Gets whether this is a special "type priority" key. A type priority key indicates that the
   * ordering of FeatureStructures in the index is based on the {@link TypePriorities} defined for
   * that Analysis Engine.
   * <p>
   * Type priority keys ignore any values assigned to the {@link #getFeatureName() featureName} and
   * {@link #getComparator() comparator} properties.
   * 
   * @param aTypePriority
   *          true if and only if this is a type priority key
   */
  public void setTypePriority(boolean aTypePriority);

  /**
   * Gets the name of the key's Feature. FeatureStructures will be ordered in the index based on the
   * value of this Feature.
   * 
   * @return the name of this key's Feature
   */
  public String getFeatureName();

  /**
   * Sets the name of the key's Feature. FeatureStructures will be ordered in the index based on the
   * value of this Feature.
   * 
   * @param aName
   *          the name of this key's Feature
   */
  public void setFeatureName(String aName);

  /**
   * Gets the comparator for this key. This determines the ordering of FeatureStructures in the
   * index. Valid values for this property are defined by constants on this interface.
   * 
   * @return this key's comparator
   */
  public int getComparator();

  /**
   * Sets the comparator for this key. This determines the ordering of FeatureStructures in the
   * index. Valid values for this property are defined by constants on this interface.
   * 
   * @param aComparator
   *          this key's comparator
   */
  public void setComparator(int aComparator);

}
