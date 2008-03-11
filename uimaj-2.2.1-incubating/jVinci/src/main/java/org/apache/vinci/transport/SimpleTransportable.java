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

package org.apache.vinci.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.vinci.debug.FatalException;

/**
 * SimpleTransportable is an immutable Transportable object that simply writes a byte buffer for its
 * toStream implementation. It can be used for creating lightweight copies of more heavy-weight
 * documents for high performance and simplified synchronization. Because it is immutable, the
 * fromStream() method of this class throws an unchecked exception if invoked.
 */
public final class SimpleTransportable implements Transportable {
  private final byte[] document;

  /**
   * Create a SimpleTransportable that is an immutable copy of the provided Transportable object.
   * 
   * @pre convert_me != null
   */
  public SimpleTransportable(Transportable convert_me) {
    try {
      ByteArrayOutputStream byte_out = new ByteArrayOutputStream();
      try {
        convert_me.toStream(byte_out);
      } finally {
        byte_out.close();
      }
      document = byte_out.toByteArray();
    } catch (IOException e) {
      // Since the IOException should only come from the underlying stream (which
      // in this case is a ByteArrayOutputStream), this should never happen.
      throw new FatalException(e);
    }
  }

  /**
   * @pre os != null
   */
  public void toStream(OutputStream os) throws IOException {
    os.write(document);
  }

  /**
   * Not implemented (throws UnsupportedOperationException if invoked) to preserve immutability.
   * 
   * @throws UnsupportedOperationException
   *           thrown unconditionally.
   */
  public KeyValuePair fromStream(InputStream os) throws IOException {
    throw new UnsupportedOperationException("not implemented");
  }

}// class
