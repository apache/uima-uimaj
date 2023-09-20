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

package org.apache.uima.internal.util;

public class Pair<T, U> {

  public final T t;
  /** updatable but don't update if used as key in hashtable */
  public U u;

  public Pair(T t, U u) {
    this.t = t;
    this.u = u;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((t == null) ? 0 : t.hashCode());
    result = prime * result + ((u == null) ? 0 : u.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }
    Pair other = (Pair) obj;
    if (t == null) {
      if (other.t != null) {
        return false;
      }
    } else if (!t.equals(other.t)) {
      return false;
    }
    if (u == null) {
      if (other.u != null) {
        return false;
      }
    } else if (!u.equals(other.u)) {
      return false;
    }
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Pair [t=" + t + ", u=" + u + "]";
  }

}
