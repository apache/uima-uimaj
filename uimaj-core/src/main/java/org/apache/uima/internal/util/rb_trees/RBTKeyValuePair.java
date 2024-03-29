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

/**
 * Part of map&lt;int, T&gt; RedBlackTree
 * 
 * Represents a key-value pair of a red-black tree.
 * 
 * 
 * @version $Id: RBTKeyValuePair.java,v 1.1 2001/12/12 18:01:07 goetz Exp $
 */
public class RBTKeyValuePair {
  private int key;

  private Object value;

  RBTKeyValuePair(int key, Object value) {
    this.key = key;
    this.value = value;
  }

  public int getKey() {
    return key;
  }

  public Object getValue() {
    return value;
  }

}
