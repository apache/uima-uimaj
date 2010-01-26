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

package org.apache.uima.caseditor.editor.util;

import java.util.Random;

/**
 * <p>
 * The IDGenerator generate unique IDs.
 * </p>
 * <p>
 * Attention: The maximal number of generateable IDs is Long.MAX_VALUE. If there are more than
 * Long.MAX_VALUE calls to nextUniqueID() an IllegalStateException will be thrown.
 * </p>
 */
public class IDGenerator {
  private long sStartValue;

  /**
   * The unique id that will be incremented.
   */
  private long sUniqueID;

  /**
   * The singleton instance.
   */
  private static IDGenerator sIdGeneratorInstance;

  /**
   * Call <code>IDGenerator.getInstance()</code> to retrieve an instance of this class. Must not be
   * instantiated outside this class, singleton pattern.
   */
  private IDGenerator() {
    sStartValue = new Random().nextLong();
    sUniqueID = sStartValue + 1;
  }

  /**
   * Retrieve the next unique ID.
   *
   * @throws IllegalStateException -
   *           if there are more than Long.MAX_VALUE calls to
   *           <code>IDGenerator.nextUniqueID()</code>
   * @return - the unique id
   */
  public byte[] nextUniqueID() {
    if (sUniqueID == sStartValue) {
      throw new IllegalStateException("The ID Generator is out of IDs");
    }

    byte[] id = new byte[] { (byte) (sUniqueID >> 56), (byte) (sUniqueID >> 48),
            (byte) (sUniqueID >> 40), (byte) (sUniqueID >> 32), (byte) (sUniqueID >> 24),
            (byte) (sUniqueID >> 16), (byte) (sUniqueID >> 8), (byte) sUniqueID, };

    sUniqueID++;
    return id;
  }

  /**
   * Retrieves the only instance of the IDGenerator.
   *
   * @return - the instance of the IDGenerator
   */
  public static IDGenerator getInstance() {
    if (sIdGeneratorInstance == null) {
      sIdGeneratorInstance = new IDGenerator();
    }

    return sIdGeneratorInstance;
  }
}
