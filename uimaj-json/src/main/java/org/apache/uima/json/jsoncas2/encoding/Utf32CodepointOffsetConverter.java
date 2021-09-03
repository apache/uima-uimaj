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
package org.apache.uima.json.jsoncas2.encoding;

import static java.util.Arrays.fill;

public class Utf32CodepointOffsetConverter implements OffsetConverter {
  public static final int UNMAPPED = Integer.MIN_VALUE;

  private final int[] internalToExternal;
  private final int[] externalToInternal;

  public Utf32CodepointOffsetConverter(String aString) {
    if (aString == null) {
      internalToExternal = null;
      externalToInternal = null;
      return;
    }

    int codePointCount = aString.codePointCount(0, aString.length());
    externalToInternal = new int[codePointCount + 1];
    fill(externalToInternal, UNMAPPED);

    int codeUnitCount = aString.length();
    internalToExternal = new int[codeUnitCount + 1];
    fill(internalToExternal, UNMAPPED);

    int cpi = 0;
    int cui = 0;
    while (cui < aString.length()) {
      int cp = aString.codePointAt(cui);
      externalToInternal[cpi] = cui;
      internalToExternal[cui] = cpi;

      cpi++;
      cui += Character.charCount(cp);
    }

    externalToInternal[cpi] = cui;
    internalToExternal[cui] = cpi;
  }

  @Override
  public int mapExternal(int aOffset) {
    if (externalToInternal == null) {
      return aOffset;
    }

    if (aOffset >= externalToInternal.length) {
      return UNMAPPED;
    }

    return externalToInternal[aOffset];
  }

  @Override
  public int mapInternal(int aOffset) {
    if (internalToExternal == null) {
      return aOffset;
    }

    if (aOffset >= internalToExternal.length) {
      return UNMAPPED;
    }

    return internalToExternal[aOffset];
  }
}
