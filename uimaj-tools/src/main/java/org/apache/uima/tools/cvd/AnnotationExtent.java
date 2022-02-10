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

import javax.swing.text.Style;

/**
 * The Class AnnotationExtent.
 */
public class AnnotationExtent {

  /** The start. */
  private int start;

  /** The end. */
  private int end;

  /** The style. */
  private Style style;

  /**
   * Instantiates a new annotation extent.
   *
   * @param start
   *          the start
   * @param end
   *          the end
   * @param style
   *          the style
   */
  public AnnotationExtent(int start, int end, Style style) {
    this.start = start;
    this.end = end;
    this.style = style;
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
   * Gets the style.
   *
   * @return the style
   */
  public Style getStyle() {
    return this.style;
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
