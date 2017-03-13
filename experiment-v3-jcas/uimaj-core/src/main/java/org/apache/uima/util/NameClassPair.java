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

/**
 * A class that stores the name of an attribute and the Class of that attribute's value.
 * 
 * 
 */
public class NameClassPair implements java.io.Serializable {
  private static final long serialVersionUID = 3235061764523410003L;

  /**
   * A name
   */
  private String mName;

  /**
   * A class name
   */
  private String mClassName;

  /**
   * Creates a new <code>NameClassPair</code> with the specified name and class.
   * 
   * @param aName
   *          the name of an attribute
   * @param aClassName
   *          the name of the class of that attribute's value
   */
  public NameClassPair(String aName, String aClassName) {
    mName = aName;
    mClassName = aClassName;
  }

  /**
   * Gets the name of the attribute.
   * 
   * @return the name
   */
  public String getName() {
    return mName;
  }

  /**
   * Gets the class name of the attribute's value.
   * 
   * @return the class name
   */
  public String getClassName() {
    return mClassName;
  }

  /**
   * Determines if two NameClassPairs are equal. Two NameClassPairs are equal if both their Name and
   * ClassName properties are equal.
   * 
   * @return the class name
   */
  public boolean equals(Object aObj) {
    if (!(aObj instanceof NameClassPair)) {
      return false;
    }
    NameClassPair that = (NameClassPair) aObj;

    boolean nameMatch = this.getName() == null ? that.getName() == null : this.getName().equals(
            that.getName());

    boolean classNameMatch = this.getClassName() == null ? that.getClassName() == null : this
            .getClassName().equals(that.getClassName());

    return nameMatch && classNameMatch;
  }

  /**
   * Gets the hash code for this object. The hash codes of two NameClassPairs <code>x</code> and
   * <code>y</code> must be equal if <code>x.equals(y)</code> returns true;
   * 
   * @return the hash code for this object
   */
  public int hashCode() {
    // add the hash codes of the Name and ClassName properties
    int result = 0;
    if (mName != null) {
      result += mName.hashCode();
    }
    if (mClassName != null) {
      result += mClassName.hashCode();
    }
    return result;
  }

  /**
   * Gets string representation of this object; useful for debugging.
   * 
   * @return string representation of this object
   */
  public String toString() {
    return "(" + mName + "," + mClassName + ")";
  }
}
