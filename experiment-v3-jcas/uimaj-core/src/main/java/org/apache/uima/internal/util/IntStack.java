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
 * A stack of integers. Follows {@link java.util.Stack Stack} pretty closely, except that push()
 * returns the position of the added element in the stack. Inherits from IntVector, so those methods
 * are also available.
 */
public class IntStack extends IntVector {

  private static final long serialVersionUID = 6396213817151546621L;

  /** Creates an empty stack. */
  public IntStack() {
    super();
  }

  /** Creates an empty stack.
   * @param capacity - 
   * */
  public IntStack(int capacity) {
    super(capacity);
  }

  /**
   * Creates an empty stack with specified capacity, growth_factor and multiplication limit
   * @param capacity -
   * @param growth_factor -
   * @param multiplication_limit -
   */
  public IntStack(int capacity, int growth_factor, int multiplication_limit) {
    super(capacity, growth_factor, multiplication_limit);
  }

  /**
   * Push a new element on the stack.
   * 
   * @param i
   *          The element to be pushed.
   * @return The position of <code>i</code> after it's been added.
   */
  public int push(int i) {
    this.add(i);
    return this.pos - 1;
  }

  /**
   * Pop an element from the stack.
   * 
   * @return The popped element.
   */
  public int pop() {
    --this.pos;
    return this.array[this.pos];
  }

  /**
   * Look at the topmost element in the stack.
   * 
   * @return The top element.
   */
  public int peek() {
    return this.array[this.pos - 1];
  }

  /**
   * Check if stack is empty.
   * 
   * @return <code>true</code>, if stack is empty; <code>false</code>, else.
   */
  public boolean empty() {
    return (this.pos == 0);
  }

  /**
   * Clears the stack. The amount of space reserved for this stack remains unchanged.
   */
  public void clear() {
    this.pos = 0;
  }

}
