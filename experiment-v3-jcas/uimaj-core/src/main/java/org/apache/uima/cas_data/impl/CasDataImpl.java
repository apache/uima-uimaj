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

package org.apache.uima.cas_data.impl;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.cas_data.CasData;
import org.apache.uima.cas_data.FeatureStructure;


public class CasDataImpl implements CasData {
  
  private static final long serialVersionUID = 400887901813129040L;

  private List<FeatureStructure> fsList = null;

  /**
   * Initializes FeatureStructure list
   */
  public CasDataImpl() {
    fsList = new LinkedList<FeatureStructure>();
  }

  /**
   * Returns an iterator over FeatureStructure list.
   * 
   * @return an iterator over FeatureStructure list
   */
  public Iterator<FeatureStructure> getFeatureStructures() {
    return fsList.iterator();
  }

  /**
   * Adds a new FeatureStructure to the list
   * 
   * @param aFS -
   *          new FeatureStructure to be added
   */
  public void addFeatureStructure(FeatureStructure aFS) {
    fsList.add(aFS);
  }

  /**
   * Removes named FeatureStructure from the list
   * 
   * @param aFS -
   *          FeatureStructure to remove
   */
  public void removeFeatureStructure(FeatureStructure aFS) {
    fsList.remove(aFS);
  }

  public String toString() {
    return fsList.toString();
  }

}
