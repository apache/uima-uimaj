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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface required by Frame to support marshalling.
 * 
 * Concrete implementations can marshall using any desired wire format. For example, the
 * XTalkTransporter is an implementation that uses XTalk, the default Vinci wire format.
 */
public interface FrameTransporter {
  /**
   * Marshall the frame to the input stream.
   * 
   * @param is The stream where the Frame is written.
   * @param f The Frame to be marshalled.
   */
  KeyValuePair fromStream(InputStream is, Frame f) throws IOException, EOFException;

  /**
   * Populate the (empty) frame from the stream contents.
   * 
   * @param os The stream from where the data is read.
   * @param f The Frame to be populated from the stream.
   */
  void toStream(OutputStream os, Frame f) throws IOException;
}
