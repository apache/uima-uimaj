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

package org.apache.uima.internal.util;

/**
 * Simple binary tree class.
 */
public class BinaryTree {

  private BinaryTree mother;

  private BinaryTree left;

  private BinaryTree right;

  private Object value;

  public BinaryTree() {
    mother = null;
    left = null;
    right = null;
    value = null;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public BinaryTree newLeftDtr() {
    left = new BinaryTree();
    left.mother = this;
    return left;
  }

  public BinaryTree newRightDtr() {
    right = new BinaryTree();
    right.mother = this;
    return right;
  }

  public BinaryTree getLeftDtr() {
    return left;
  }

  public BinaryTree getRightDtr() {
    return right;
  }

  public BinaryTree getMother() {
    return mother;
  }

  public Object getValue() {
    return value;
  }

}
