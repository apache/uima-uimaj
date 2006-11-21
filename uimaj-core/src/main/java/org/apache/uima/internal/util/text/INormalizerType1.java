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

package org.apache.uima.internal.util.text;

public interface INormalizerType1 {

  /**
   * Turns a range of characters to its normalized form.
   * 
   * <p>
   * This version of the API may modify the input buffer. If you don't want your input char array to
   * be modified, use
   * {@link #normalize(char[], int, int, NormalizationResult) normalize(char[], int, int, NormalizationResult)}.
   * 
   * @param text
   *          the character array to normalize
   * @param from
   *          starting offset within the character array (inclusive)
   * @param to
   *          ending position within the character array (non-inclusive)
   * @param language
   *          The human language of the text; do special processing for some languages.
   * @return the normalized array.
   */
  public INormalizationResult normalize(char[] text, int from, int length);

  public INormalizationResult normalize(char[] text, int from, int length,
                  boolean cjRemoveWhitespace);

}
