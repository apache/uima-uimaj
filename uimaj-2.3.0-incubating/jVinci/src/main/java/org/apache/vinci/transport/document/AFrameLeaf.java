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

package org.apache.vinci.transport.document;

import org.apache.vinci.transport.Attributes;
import org.apache.vinci.transport.FrameLeaf;

/**
 * This class extends FrameLeaf with ability to set attributes.
 */
public class AFrameLeaf extends FrameLeaf {

  private Attributes a = null;

  public void setAttributes(Attributes s) {
    a = s;
  }

  public Attributes getAttributes() {
    return a;
  }

  public Attributes createAttributes() {
    if (a == null) {
      a = new Attributes();
    }
    return a;
  }

  public AFrameLeaf(byte[] data, boolean encode) {
    super(data, encode);
  }

  public AFrameLeaf(String mystring) {
    super(mystring);
  }

  public AFrameLeaf(String[] mystring) {
    super(mystring);
  }

  public AFrameLeaf(float myfloat) {
    super(myfloat);
  }

  public AFrameLeaf(float[] myfloat) {
    super(myfloat);
  }

  public AFrameLeaf(double mydouble) {
    super(mydouble);
  }

  public AFrameLeaf(double[] mydouble) {
    super(mydouble);
  }

  public AFrameLeaf(int myint) {
    super(myint);
  }

  public AFrameLeaf(int[] myint) {
    super(myint);
  }

  public AFrameLeaf(long mylong) {
    super(mylong);
  }

  public AFrameLeaf(long[] mylong) {
    super(mylong);
  }

  public AFrameLeaf(boolean bool) {
    super(bool);
  }

}
