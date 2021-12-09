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
 * Represents an element in a list of markup extents. Knows about the length of the extent, as well
 * as the depth of markup. Depth of markup means, how many annotations cover this extent?
 * 
 * 
 */
public class MarkupExtent {

  /** The start. */
  private int start;

  /** The end. */
  private int end;

  /** The markup depth. */
  private int markupDepth;

  
  /**
   * Instantiates a new markup extent.
   *
   * @param start the start
   * @param end the end
   * @param markupDepth the markup depth
   */
  public MarkupExtent(int start, int end, int markupDepth) {
    this.start = start;
    this.end = end;
    this.markupDepth = markupDepth;
  }

  /**
   * Gets the length.
   *
   * @return the length
   */
  public int getLength() {
    return this.end - this.start;
  }

  /**
   * Gets the markup depth.
   *
   * @return the markup depth
   */
  public int getMarkupDepth() {
    return this.markupDepth;
  }

  /**
   * Gets the end.
   *
   * @return int
   */
  public int getEnd() {
    return this.end;
  }

  /**
   * Gets the start.
   *
   * @return int
   */
  public int getStart() {
    return this.start;
  }

}
