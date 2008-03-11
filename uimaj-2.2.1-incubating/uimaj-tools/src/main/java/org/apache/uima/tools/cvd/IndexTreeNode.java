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

package org.apache.uima.tools.cvd;

import org.apache.uima.cas.Type;

/**
 * Class comment for IndexTreeNode.java goes here.
 * 
 * 
 */
public class IndexTreeNode {

  private final String name;

  private final Type type;

  private final int size;

  public IndexTreeNode(String name, Type type, int size) {
    super();
    this.name = name;
    this.type = type;
    this.size = size;
  }

  /**
   * Constructor for IndexTreeNode.
   */
  public IndexTreeNode() {
    super();
    this.name = null;
    this.type = null;
    this.size = 0;
  }

  public String toString() {
    return "<html><font color=green>" + this.name + "</font> [" + this.size + "]</html>";
  }

  /**
   * Returns the name.
   * 
   * @return String
   */
  public String getName() {
    return this.name;
  }

  /**
   * Returns the type.
   * 
   * @return Type
   */
  public Type getType() {
    return this.type;
  }

  public boolean equals(Object o) {
    return (this == o);
  }

  public int hashCode() {
    return super.hashCode();
  }

}
