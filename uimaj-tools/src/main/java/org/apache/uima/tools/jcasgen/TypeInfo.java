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

package org.apache.uima.tools.jcasgen;


/**
 * The Class TypeInfo.
 */
public class TypeInfo {

  /** The xml name. */
  String xmlName;

  /** The java name with pkg. */
  String javaNameWithPkg;

  /** The java name. */
  String javaName; // name without package prefix if in this package

  /** The is array. */
  boolean isArray = false;

  /** The array el name with pkg. */
  String arrayElNameWithPkg;

  /** The used. */
  boolean used = false;

  /**
   * Instantiates a new type info.
   *
   * @param xmlName the xml name
   * @param javaName the java name
   */
  TypeInfo(String xmlName, String javaName) {
    this.xmlName = xmlName;
    this.javaNameWithPkg = javaName;
    this.javaName = Jg.removePkg(javaName);
    this.isArray = false;
    this.arrayElNameWithPkg = "";
  }

  /**
   * Instantiates a new type info.
   *
   * @param xmlName the xml name
   * @param javaName the java name
   * @param arrayElNameWithPkg the array el name with pkg
   */
  TypeInfo(String xmlName, String javaName, String arrayElNameWithPkg) {
    this(xmlName, javaName);
    if (null != arrayElNameWithPkg) {
      this.isArray = true;
      this.arrayElNameWithPkg = arrayElNameWithPkg;
    }
  }
}
