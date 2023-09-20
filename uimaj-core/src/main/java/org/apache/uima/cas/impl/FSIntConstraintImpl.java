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
    codes = new IntVector();
    values = new IntVector();
  }

  @Override
  public boolean match(int j) {
    final int max = codes.size();
    for (int i = 0; i < max; i++) {
      switch (codes.get(i)) {
        case LT: {
          if (j >= values.get(i)) {
            return false;
          }
          break;
        }
        case LEQ: {
          if (j > values.get(i)) {
            return false;
          }
          break;
        }
        case EQ: {
          if (j != values.get(i)) {
            return false;
          }
          break;
        }
        case GEQ: {
          if (j < values.get(i)) {
            return false;
          }
          break;
        }
        case GT: {
          if (j <= values.get(i)) {
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
  @Override
  public void eq(int i) {
    codes.add(EQ);
    values.add(i);
  }

  /**
   * Require int value to be less than <code>i</code>.
   * 
   * @param i
   *          Matched value must be less than this.
   */
  @Override
  public void lt(int i) {
    codes.add(LT);
    values.add(i);
  }

  /**
   * Require int value to be less than or equal to <code>i</code>.
   * 
   * @param i
   *          Matched value must be less than or equal to this.
   */
  @Override
  public void leq(int i) {
    codes.add(LEQ);
    values.add(i);
  }

  /**
   * Require int value to be greater than <code>i</code>.
   * 
   * @param i
   *          Matched value must be greater than this.
   */
  @Override
  public void gt(int i) {
    codes.add(GT);
    values.add(i);
  }

  /**
   * Require int value to be greater than or equal to <code>i</code>.
   * 
   * @param i
   *          Matched value must be greater than or equal to this.
   */
  @Override
  public void geq(int i) {
    codes.add(GEQ);
    values.add(i);
  }

  @Override
  public String toString() {
    if (codes.size() == 1) {
      return FSFloatConstraintImpl.toString(codes.get(0)) + " "
              + Integer.toString(values.get(0));
    }
    StringBuffer buf = new StringBuffer();
    buf.append("( ");
    for (int i = 0; i < codes.size(); i++) {
      buf.append(FSFloatConstraintImpl.toString(codes.get(i)));
      buf.append(' ');
      buf.append(Integer.toString(values.get(i)));
      buf.append(' ');
    }
    buf.append(')');
    return buf.toString();
  }

}
