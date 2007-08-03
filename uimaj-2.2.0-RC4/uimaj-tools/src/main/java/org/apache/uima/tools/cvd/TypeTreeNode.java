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
 * Class comment for TypeTreeNode.java goes here.
 * 
 * 
 */
public class TypeTreeNode {

  private final Type type;

  private final String label;

  private final int size;

  /**
   * Constructor for TypeTreeNode.
   */
  public TypeTreeNode() {
    super();
    this.type = null;
    this.label = null;
    this.size = -1;
  }

  public TypeTreeNode(Type type, String label, int size) {
    super();
    this.type = type;
    this.label = label;
    this.size = size;
  }

  public String toString() {
    return "<html><font color=blue>" + this.type.getName() + "</font> [" + this.size + "]</html>";
  }

  /**
   * Returns the label.
   * 
   * @return String
   */
  public String getLabel() {
    return this.label;
  }

  /**
   * Returns the type.
   * 
   * @return Type
   */
  public Type getType() {
    return this.type;
  }

}
