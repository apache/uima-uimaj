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

import java.util.Vector;

import org.apache.uima.cas.FSFloatConstraint;
import org.apache.uima.internal.util.IntVector;

/**
 * Implement the FSFloatConstraint interface. Package private.
 * 
 * 
 * @version $Revision: 1.2 $
 */
class FSFloatConstraintImpl implements FSFloatConstraint {

  private static final long serialVersionUID = 4649271745827863437L;

  private static final int LT = 0;

  private static final int LEQ = 1;

  private static final int EQ = 2;

  private static final int GEQ = 3;

  private static final int GT = 4;

  private IntVector codes;

  private Vector<Float> values;

  FSFloatConstraintImpl() {
    codes = new IntVector();
    values = new Vector<>();
  }

  @Override
  public boolean match(float f) {
    final int max = codes.size();
    for (int i = 0; i < max; i++) {
      switch (codes.get(i)) {
        case LT: {
          if (f >= values.get(i)) {
            return false;
          }
          break;
        }
        case LEQ: {
          if (f > values.get(i)) {
            return false;
          }
          break;
        }
        case EQ: {
          if (f != values.get(i)) {
            return false;
          }
          break;
        }
        case GEQ: {
          if (f < values.get(i)) {
            return false;
          }
          break;
        }
        case GT: {
          if (f <= values.get(i)) {
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
   * Require float value to be equal <code>f</code>.
   * 
   * @param f
   *          Matched value must be equal to this.
   */
  @Override
  public void eq(float f) {
    codes.add(EQ);
    values.add(f);
  }

  /**
   * Require float value to be less than <code>f</code>.
   * 
   * @param f
   *          Matched value must be less than this.
   */
  @Override
  public void lt(float f) {
    codes.add(LT);
    values.add(f);
  }

  /**
   * Require float value to be less than or equal to <code>f</code>.
   * 
   * @param f
   *          Matched value must be less than or equal to this.
   */
  @Override
  public void leq(float f) {
    codes.add(LEQ);
    values.add(f);
  }

  /**
   * Require float value to be greater than <code>f</code>.
   * 
   * @param f
   *          Matched value must be greater than this.
   */
  @Override
  public void gt(float f) {
    codes.add(GT);
    values.add(f);
  }

  /**
   * Require float value to be greater than or equal to <code>f</code>.
   * 
   * @param f
   *          Matched value must be greater than or equal to this.
   */
  @Override
  public void geq(float f) {
    codes.add(GEQ);
    values.add(f);
  }

  @Override
  public String toString() {
    if (codes.size() == 1) {
      return toString(codes.get(0)) + " " + values.get(0).toString();
    }
    StringBuilder buf = new StringBuilder();
    buf.append("( ");
    for (int i = 0; i < codes.size(); i++) {
      buf.append(toString(codes.get(i)));
      buf.append(' ');
      buf.append(values.get(i).toString());
      buf.append(' ');
    }
    buf.append(')');
    return buf.toString();
  }

  static final String toString(int comp) {
    switch (comp) {
      case LEQ: {
        return "<=";
      }
      case LT: {
        return "<";
      }
      case EQ: {
        return "=";
      }
      case GEQ: {
        return ">=";
      }
      case GT: {
        return ">";
      }
      default: {
        return "";
      }
    }
  }

}
