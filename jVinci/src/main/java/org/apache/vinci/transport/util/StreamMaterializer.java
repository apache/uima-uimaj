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

package org.apache.vinci.transport.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple static adapter method for converting stream into byte[]
 */
public class StreamMaterializer {

  static private final int BUFFER_SIZE = 8192;

  /**
   * Utility class not meant to be instantiated.
   */
  private StreamMaterializer() {
  }

  /**
   * Bring the entire contents of the provided stream into a memory-resident byte array.
   * Does NOT close the stream.
   *
   * @exception IOException thrown by the input stream. 
   * 
   * @pre input_stream != null
   */
  public static final byte[] materialize(InputStream input_stream) throws IOException {
    ByteArrayOutputStream content = new ByteArrayOutputStream(BUFFER_SIZE);
    byte[] b = new byte[BUFFER_SIZE];
    int num_read = input_stream.read(b);
    while (num_read != -1) {
      content.write(b, 0, num_read);
      num_read = input_stream.read(b);
    }
    return content.toByteArray();
  }

}// class
