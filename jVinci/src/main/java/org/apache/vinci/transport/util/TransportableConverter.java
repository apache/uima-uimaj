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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.vinci.debug.FatalException;
import org.apache.vinci.transport.Transportable;
import org.apache.vinci.transport.TransportableFactory;

/**
 * Provides utility method for converting between differing transportable types.
 */
public class TransportableConverter {

  /**
   * Utility class -- not meant to be instantiated.
   */
  private TransportableConverter() {
  }

  /**
   * Convert a transportable to the type of transportable returned by a factory.
   * 
   * @param convert_me The transportable to get converted. Must implement toStream().
   * @param factory The factory used to create the return Transportable, which must implement fromStream().
   * 
   * @pre convert_me != null
   * @pre factory != null
   */
  static public Transportable convert(Transportable convert_me, TransportableFactory factory) {
    Transportable return_me = factory.makeTransportable();
    convert(convert_me, return_me);
    return return_me;
  }

  /**
   * Copy the contents of one transportable into the other transportable.
   * 
   * @param convert_me The transportable to get converted. Must implement toStream().
   * @param into_me The transportable to get populated. Must implement fromStream().
   * 
   * @pre convert_me != null
   * @pre into_me != null
   */
  static public void convert(Transportable convert_me, Transportable into_me) {
    try {
      ByteArrayOutputStream byte_out = new ByteArrayOutputStream();
      try {
        convert_me.toStream(byte_out);
      } finally {
        byte_out.close();
      }
      ByteArrayInputStream byte_in = new ByteArrayInputStream(byte_out.toByteArray());
      try {
        byte_out = null; // allow GC
        into_me.fromStream(byte_in);
      } finally {
        byte_in.close();
      }
    } catch (IOException e) {
      throw new FatalException(e); // this should not arise.
    }
  }

} //class
