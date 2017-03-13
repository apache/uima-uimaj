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

import org.apache.uima.cas.FSStringConstraint;

/**
 * Implement the FSStringConstraint interface. Package private.
 * 
 * 
 * @version $Revision: 1.1 $
 */
class FSStringConstraintImpl implements FSStringConstraint {

  private static final long serialVersionUID = -7167266553385439718L;

  private String string;

  /**
   * Constructor is package private.
   */
  FSStringConstraintImpl() {
    super();
    this.string = "";
  }

  public void equals(String s) {
    this.string = s;
  }

  public boolean match(String s) {
    if (this.string == null) {
      return (s == null);
    }
    return (this.string.equals(s));
  }

  public String toString() {
    // need to escape quotes and backslashes
    StringBuffer buf = new StringBuffer();
    buf.append("= \"");
    for (int i = 0; i < this.string.length(); i++) {
      char c = this.string.charAt(i);
      switch (c) {
        case '"':
          buf.append("\\\"");
          break;
        case '\\':
          buf.append("\\\\");
          break;
        default:
          buf.append(c);
      }
    }
    buf.append('"');
    return buf.toString();
  }

}
