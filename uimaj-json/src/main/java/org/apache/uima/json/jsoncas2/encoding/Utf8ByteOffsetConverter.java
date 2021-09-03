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

import static java.lang.Character.charCount;
import static java.lang.Character.toChars;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.fill;

public class Utf8ByteOffsetConverter implements OffsetConverter {
  public static final int UNMAPPED = Integer.MIN_VALUE;

  private final int[] internalToExternal;
  private final int[] externalToInternal;

  public Utf8ByteOffsetConverter(String aString) {
    if (aString == null) {
      internalToExternal = null;
      externalToInternal = null;
      return;
    }

    int byteCount = aString.getBytes(UTF_8).length;
    externalToInternal = new int[byteCount + 1];
    fill(externalToInternal, UNMAPPED);

    int codeUnitCount = aString.length();
    internalToExternal = new int[codeUnitCount + 1];
    fill(internalToExternal, UNMAPPED);

    int cbi = 0;
    int cui = 0;
    while (cui < aString.length()) {
      externalToInternal[cbi] = cui;
      internalToExternal[cui] = cbi;

      int cp = aString.codePointAt(cui);
      cbi += String.valueOf(toChars(cp)).getBytes(UTF_8).length;
      cui += charCount(cp);
    }

    externalToInternal[cbi] = cui;
    internalToExternal[cui] = cbi;
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
