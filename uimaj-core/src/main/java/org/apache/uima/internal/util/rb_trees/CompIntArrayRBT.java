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

package org.apache.uima.internal.util.rb_trees;

import org.apache.uima.internal.util.IntComparator;

/**
 * Used for UIMA Set indexes
 * 
 * @deprecated Not used anymore. Will be removed in UIMA 4.
 * @forRemoval 4.0.0
 */
@Deprecated(since = "3.3.0")
public class CompIntArrayRBT extends IntArrayRBT {

  private IntComparator comp;

  public CompIntArrayRBT(IntComparator comp) {
    this(comp, default_size);
  }

  /**
   * Constructor for CompIntArrayRBT.
   * 
   * @param comp
   *          -
   * @param initialSize
   *          -
   */
  public CompIntArrayRBT(IntComparator comp, int initialSize) {
    super(initialSize);
    this.comp = comp;
  }

  @Override
  protected int compare(int v1, int v2) {
    return comp.compare(v1, v2);
  }

}
