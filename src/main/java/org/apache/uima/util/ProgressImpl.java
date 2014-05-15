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
 * 
 * 
 */
public class ProgressImpl implements Progress, Cloneable {
  private static final long serialVersionUID = -1436588781274530622L;

  private String mUnitType = Progress.BYTES;

  private long mCompleted;

  private long mTotal;

  private boolean mApproximate;

  public ProgressImpl(int aCompleted, int aTotal, String aUnit, boolean aApproximate) {
    mUnitType = aUnit;
    mCompleted = aCompleted;
    mTotal = aTotal;
    mApproximate = aApproximate;
  }

  public ProgressImpl(int aCompleted, int aTotal, String aUnit) {
    this(aCompleted, aTotal, aUnit, false);
  }

  public long getCompleted() {
    return mCompleted;
  }

  public long getTotal() {
    return mTotal;
  }

  public String getUnit() {
    return mUnitType;
  }

  public void setCompleted(int aCompleted) {
    mCompleted = aCompleted;
  }

  public void setTotal(int aTotal) {
    mTotal = aTotal;
  }

  public boolean isApproximate() {
    return mApproximate;
  }

  public void increment(int aIncrement) {
    mCompleted += aIncrement;
  }

  public Object clone() throws CloneNotSupportedException {

    return super.clone();
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append(getCompleted());
    if (getTotal() >= 0) {
      buf.append(" of ").append(getTotal());
    }
    buf.append(' ').append(getUnit());
    if (isApproximate()) {
      buf.append(" (approximate)");
    }
    return buf.toString();
  }

}
