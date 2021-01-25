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
package org.apache.uima.cas.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * states the CAS can be in
 */
public enum CasState {
   UIMA_AS_WAIT_4_RESPONSE,   // when in this state, uima-as is awaiting response from a remote,
                              // any attempt "release" this cas will throw an exception
   READ_ONLY,              // multi-threaded access for reading allowed, no updating 
   NO_ACCESS,              // no reading or writing (except by selected thread)
   ;
  
  static final boolean return_false() { return false; }
  static final boolean return_true() { return true; }
  /** 
   * @param thread the thread which is permitted to update the CAS
   * @return true if the thread == current thread, false otherwise to block access
   */
  static final boolean isSameThread(Thread thread) { return Thread.currentThread() == thread; }
  
  static final MethodHandle produce_one_thread_access_test(Thread thread) {
    MethodHandle mh;
    try {
      mh = MethodHandles.lookup().findStatic(CasState.class, "isSameThread", MethodType.methodType(boolean.class, Thread.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return mh.bindTo(thread);      
  }

  
}
