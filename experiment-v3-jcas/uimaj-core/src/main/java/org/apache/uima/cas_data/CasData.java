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

package org.apache.uima.cas_data;

import java.io.Serializable;
import java.util.Iterator;

import org.apache.uima.cas.ArrayFS;

/**
 * Interface for directly accessing and manipulating the data in the Common Analysis Structure.
 * <p>
 * The CAS Data is a collection of {@link FeatureStructure}s, where each FeatureStructure has an
 * optional ID, a type (represented as a string), and a collection of features, which are
 * attribute-value pairs. Feature names are strings, and their values may be primitives (String,
 * integer, or float) or references, via ID, to one or more other FeatureStructures. Circular
 * references are allowed.
 * <p>
 * Note on Arrays: Arrays are represented by {@link ArrayFS}, which is a subtype of
 * {@link FeatureStructure}. Arrays are NOT primitive values. See {@link FeatureStructure} for more
 * information.
 * <p>
 * FeatureStructures also have a property <code>indexed</code>, which determines whether the
 * FeatureStructure should be added to the CAS's indexes if the CAS Data is converted to a CAS
 * Object. The CasData itself does not provide indexes.
 * 
 * 
 */
public interface CasData extends Serializable {
  /**
   * Get an iterator over all top-level FeatureStructures, in no particular order.
   * 
   * @return an iterator over {@link FeatureStructure} objects.
   */
  public Iterator<FeatureStructure> getFeatureStructures();

  /**
   * Adds a FeatureStructure to the list of top-level FeatureStructures contained in this CasData.
   * 
   * @param aFS
   *          the FeatureStructure to be added
   */
  public void addFeatureStructure(FeatureStructure aFS);

  /**
   * Removes a FeatureStructure to this CasData. Note that this only removes the FeatureStructure
   * from the top-level list. If the FeatureStructure is a value of a feature, it is will still be
   * accessible through that path.
   * 
   * @param aFS
   *          the FeatureStructure to be removed
   */
  public void removeFeatureStructure(FeatureStructure aFS);
}
