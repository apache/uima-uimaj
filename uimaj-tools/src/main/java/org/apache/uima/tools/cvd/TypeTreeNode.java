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

  /** The type. */
  private final Type type;

  /** The label. */
  private final String label;

  /** The size. */
  private final int size;

  /**
   * Constructor for TypeTreeNode.
   */
  public TypeTreeNode() {
    this.type = null;
    this.label = null;
    this.size = -1;
  }

  /**
   * Instantiates a new type tree node.
   *
   * @param type the type
   * @param label the label
   * @param size the size
   */
  public TypeTreeNode(Type type, String label, int size) {
    this.type = type;
    this.label = label;
    this.size = size;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
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
