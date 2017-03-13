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
 * A description of a CAS feature structure index. This implements <code>MetaDataObject</code>,
 * which implements {@link org.apache.uima.util.XMLizable}, so it can be serialized to and
 * deserialized from an XML element.
 * 
 * 
 */
public interface FsIndexDescription extends MetaDataObject {

  public final static FsIndexDescription[] EMPTY_FS_INDEX_DESCRIPTIONS = new FsIndexDescription[0];
  /**
   * Gets the label of this index. This is the identifier used to retrieve the index from the CAS's
   * Index Repository.
   * 
   * @return the label of this index.
   */
  public String getLabel();

  /**
   * Sets the label of this index. This is the identifier used to retrieve the index from the CAS's
   * Index Repository.
   * 
   * @param aLabel
   *          the label of this index.
   */
  public void setLabel(String aLabel);

  /**
   * Gets the Type name for this index. This determines what type of FeatureStructures are contained
   * in the index.
   * 
   * @return the type name for this index
   */
  public String getTypeName();

  /**
   * Sets the Type name for this index. This determines what type of FeatureStructures are contained
   * in the index.
   * 
   * @param aTypeName
   *          the type name for this index
   */
  public void setTypeName(String aTypeName);

  /**
   * Gets the "kind" of index. There are currently three kinds of indexes - "sorted", "set", and
   * "bag" (see {@link org.apache.uima.cas.FSIndex} for definitions). If this is <code>null</code>,
   * "sorted" is assumed as the default.
   * 
   * @return the kind of index
   */
  public String getKind();

  /**
   * Sets the "kind" of index. There are currently three kinds of indexes - sorted, set, and bag
   * (see {@link org.apache.uima.cas.FSIndex} for definitions). If this is <code>null</code>,
   * "sorted" is assumed as the default.
   * 
   * @param aKind
   *          the kind of index
   */
  public void setKind(String aKind);

  /**
   * Gets the keys for this index. The keys determine the ordering of FeatureStructures in this
   * index.
   * 
   * @return the keys for this index
   */
  public FsIndexKeyDescription[] getKeys();

  /**
   * Sets the keys for this index. The keys determine the ordering of FeatureStructures in this
   * index.
   * 
   * @param aKeys
   *          the keys for this index
   */
  public void setKeys(FsIndexKeyDescription[] aKeys);

  /**
   * Identifies a Sorted index.
   * 
   * @see org.apache.uima.cas.FSIndex
   */
  public static final String KIND_SORTED = "sorted";

  /**
   * Identifies a Set index.
   * 
   * @see org.apache.uima.cas.FSIndex
   */
  public static final String KIND_SET = "set";

  /**
   * Identifies a Bag index.
   * 
   * @see org.apache.uima.cas.FSIndex
   */
  public static final String KIND_BAG = "bag";
}
