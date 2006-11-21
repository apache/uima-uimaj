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

import java.rmi.dgc.VMID;
import java.rmi.server.UID;

/**
 * Utility class for generating UUIDs. This implementation currently uses RMI's VMID and UID
 * objects.
 * 
 * 
 */
public abstract class UUIDGenerator {

  /**
   * Generates a UUID.
   * 
   * @return the UUID
   */
  public static String generate() {
    String uuid = mHostId + new UID().toString();
    // System.out.println("UUID: " + uuid);
    return uuid;
  }

  private static String mHostId;

  // static initializer
  static {
    String vmid = new VMID().toString();
    // System.out.println("VMID = " + vmid);
    // host ID appears to be first part of VMID - the rest of the VMID is
    // repeated in each UID
    int colonOffset = vmid.indexOf(':');
    if (colonOffset > 0) {
      mHostId = vmid.substring(0, colonOffset + 1);
    } else {
      mHostId = vmid;
    }
  }

}
