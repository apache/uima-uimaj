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

import org.apache.uima.cas.FSIntConstraint;
import org.apache.uima.internal.util.IntVector;

/**
 * Implement the FSIntConstraint interface. Package private.
 * 
 * 
 * @version $Revision: 1.1 $
 */
class FSIntConstraintImpl implements FSIntConstraint {

  private static final long serialVersionUID = -4828985717069908575L;

  private static final int LT = 0;

  private static final int LEQ = 1;

  private static final int EQ = 2;

  private static final int GEQ = 3;

  private static final int GT = 4;

  private IntVector codes;

  private IntVector values;

  FSIntConstraintImpl() {
    this.codes = new IntVector();
    this.values = new IntVector();
  }

  public boolean match(int j) {
    final int max = this.codes.size();
    for (int i = 0; i < max; i++) {
      switch (this.codes.get(i)) {
        case LT: {
          if (j >= this.values.get(i)) {
            return false;
          }
          break;
        }
        case LEQ: {
          if (j > this.values.get(i)) {
            return false;
          }
          break;
        }
        case EQ: {
          if (j != this.values.get(i)) {
            return false;
          }
          break;
        }
        case GEQ: {
          if (j < this.values.get(i)) {
            return false;
          }
          break;
        }
        case GT: {
          if (j <= this.values.get(i)) {
            return false;
          }
          break;
        }
        default: {
          throw new Error("Internal error.");
          // assert false;
        }
      }
    }
    return true;
  }

  /**
   * Require int value to be equal <code>i</code>.
   * 
   * @param i
   *          Matched value must be equal to this.
   */
  public void eq(int i) {
    this.codes.add(EQ);
    this.values.add(i);
  }

  /**
   * Require int value to be less than <code>i</code>.
   * 
   * @param i
   *          Matched value must be less than this.
   */
  public void lt(int i) {
    this.codes.add(LT);
    this.values.add(i);
  }

  /**
   * Require int value to be less than or equal to <code>i</code>.
   * 
   * @param i
   *          Matched value must be less than or equal to this.
   */
  public void leq(int i) {
    this.codes.add(LEQ);
    this.values.add(i);
  }

  /**
   * Require int value to be greater than <code>i</code>.
   * 
   * @param i
   *          Matched value must be greater than this.
   */
  public void gt(int i) {
    this.codes.add(GT);
    this.values.add(i);
  }

  /**
   * Require int value to be greater than or equal to <code>i</code>.
   * 
   * @param i
   *          Matched value must be greater than or equal to this.
   */
  public void geq(int i) {
    this.codes.add(GEQ);
    this.values.add(i);
  }

  public String toString() {
    if (this.codes.size() == 1) {
      return FSFloatConstraintImpl.toString(this.codes.get(0)) + " "
              + Integer.toString(this.values.get(0));
    }
    StringBuffer buf = new StringBuffer();
    buf.append("( ");
    for (int i = 0; i < this.codes.size(); i++) {
      buf.append(FSFloatConstraintImpl.toString(this.codes.get(i)));
      buf.append(' ');
      buf.append(Integer.toString(this.values.get(i)));
      buf.append(' ');
    }
    buf.append(')');
    return buf.toString();
  }

}
