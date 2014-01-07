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

package org.apache.uima.tools.cvd;

/**
 * TODO: add type comment for <code>ArrayNode</code>.
 * 
 * 
 */
public class ArrayNode extends FSTreeNode {

  public static final int CUTOFF = 100;

  public static final int MULT = 10;

  private int start;

  private int end;

  
  public ArrayNode(int start, int end) {
    super();
    this.start = start;
    this.end = end;
  }

  public String toString() {
    return "[" + this.start + ".." + this.end + "]";
  }

  protected void initChildren() {
    // Does nothing.
  }

  // Compute the degree of i: (number of decimals of (i-1)) - 1.
  static int degree(int i) {
    if (i == 1) {
      // Avoid log10(0)
      return 0;
    }
    return (int) Math.log10(i - 1);
  }
  
  /**
   * @return int
   */
  public int getEnd() {
    return this.end;
  }

  /**
   * @return int
   */
  public int getStart() {
    return this.start;
  }
  

}
