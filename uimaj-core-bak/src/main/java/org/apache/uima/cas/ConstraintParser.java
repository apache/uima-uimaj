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

package org.apache.uima.cas;

import java.io.IOException;

/**
 * A parser for feature structure constraint expressions.
 * 
 * 
 * @deprecated The Constraint Parser is not supported in externally released versions of UIMA
 */
public interface ConstraintParser {

  /**
   * Parse a string representing a feature structure match constraint.
   * @param s The input string.
   * @return The corresponding match constraint.
   * @throws IOException If there is a problem with the input string.
   * @throws ParsingException If the input string is not a well-formed string
   * representation of a match constraint.
   */
  public FSMatchConstraint parse(String s)
    throws IOException, ParsingException;

  /**
   * Parse a string representing a feature structure match constraint.
   * @param s The input string.
   * @param src For better error messages: an input source, such as a file
   * name (can be null).
   * @param line For better error messages: a line offset.
   * @param col For better error messages: a column offset.
   * @return The corresponding match constraint.
   * @throws IOException If there is a problem with the input string.
   * @throws ParsingException If the input string is not a well-formed string
   * representation of a match constraint.
   */
  public FSMatchConstraint parse(
    String s,
    String src,
    int line,
    int col)
    throws IOException, ParsingException;

}
