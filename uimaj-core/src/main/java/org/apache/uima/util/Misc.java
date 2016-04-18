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

package org.apache.uima.util;


public class Misc {

  /**
   * 
   * @param name of property
   * @return true if property is defined, or is defined and set to anything 
   * except "false"; false if property is not defined, or is defined and set to
   * "false".
   */
  public static boolean getNoValueSystemProperty(String name) {
    return !System.getProperty(name, "false").equals("false");
  }
  
//  public static void main(String[] args) {
//    System.out.println("should be false - not defined: " + getNoValueSystemProperty("foo"));
//    System.setProperty("foo", "");
//    System.out.println("should be true - defined, 0 len str value: " + getNoValueSystemProperty("foo"));
//    System.setProperty("foo", "true");
//    System.out.println("should be true - defined, true value: " + getNoValueSystemProperty("foo"));
//    System.setProperty("foo", "zzz");
//    System.out.println("should be true - defined, zzz value: " + getNoValueSystemProperty("foo"));
//    System.setProperty("foo", "false");
//    System.out.println("should be false - defined, false value: " + getNoValueSystemProperty("foo"));
//  }
}
