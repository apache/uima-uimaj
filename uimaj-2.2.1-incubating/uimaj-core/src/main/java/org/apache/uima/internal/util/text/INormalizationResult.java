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

public interface INormalizationResult {

  /**
   * Returns the char[] holding the normalized text.
   * 
   * <p>
   * <b>Note</b>: do not keep a handle to the char[] between calls to <code>normalize()</code>,
   * since the char[] buffer may be reallocated.
   * 
   * @return The char[] containing the normalized text processed by
   *         {@link AbstractCustomNormalizer#normalize(char[],int,int)}.
   */
  public char[] getBuffer();

  /**
   * @return The index within the char[] ({@link #getBuffer()}), where the first normalized
   *         character has been written.
   * 
   */
  public int getStartIndex();

  /**
   * @return The index+1 of the last normalized character in the char[] ({@link #getBuffer()}).
   *         This index may be used as start index for retrieving the untouched content after the
   *         normalized text (if more is following).
   */
  public int getStopIndex();

  /**
   * Package friendy. Only meant for {@link AbstractCustomNormalizer}.
   */
  public void setBuffer(char[] cs);

  /**
   * Package friendy. Only meant for {@link AbstractCustomNormalizer}.
   */
  public void setStartIndex(int i);

  /**
   * Package friendy. Only meant for {@link AbstractCustomNormalizer}.
   */
  public void setStopIndex(int i);

  boolean ensureLength(int len);

}
