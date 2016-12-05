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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.uima.UIMARuntimeException;

/**
 * Common de/serialization 
 */
public class CommonSerDes {
  
  int version1;
  int version2;
  
  boolean isDelta;
  boolean isCompressed;
  
  /*********************************************
   * HEADERS
   * Serialization versioning
   *   There are 1 or 2 words used for versioning.
   *     Compressed formats and plain formats with bit xx on in first word use 2nd word
   *     
   *   First word:
   *   
   *     - bit in 0x01 position: on for binary non-delta (redundant)   
   *     - bit in 0x02 position: on means delta, off - not delta
   *     - bit in 0x04 position: on means compressed, off means plain binary
   *     - bit in 0x08 position: on means type system + indexes def included
   *     - bit in 0x10 position: on means type system (only) included
   *     - bits  0xF0 reserved
   *     
   *     - byte in 0xFF 00 position: 
   *               a sequential version number, incrementing (starting w/ 0)
   *     
   *         Form 4:  0 = original (UIMA v2)
   *                  1 = fixes to original found during V3 development
   *                  2 = V3
   *                       
   *     - byte in 0xFF 00 00  position: special flags with some shared meaning
   *       -- bit 0x01 00 00: V3 formats
   *         
   *   Second word:
   *     - bit in 0x01 position: on means form6, off = form 4 
   *********************************************/
  
  public static class Header {
    boolean isDelta;
    boolean isCompressed;
    boolean form4;
    boolean form6;
    boolean typeSystemIncluded;  // for form 6, TS only
    boolean typeSystemIndexDefIncluded;
    byte seqVersionNbr;
    boolean isV3;
    boolean swap;
    int v;      // for error messages

    
    Reading reading;
    
    /* **********  BUILDERS ************/
    public Header delta() {isDelta = true;  return this; }
    public Header delta(boolean v2) {isDelta = v2;  return this; }
    public Header form4() {isCompressed = form4 = true; form6 = false; return this; }
    public Header form6() {isCompressed = form6 = true; form4 = false; return this; }
    public Header typeSystemIncluded(boolean f) {typeSystemIncluded = f; return this; }
    public Header typeSystemIndexDefIncluded(boolean f) {typeSystemIndexDefIncluded = f; return this; }
    public Header seqVer(int v2) { assert (v2 >= 0 && v2 < 256); seqVersionNbr = (byte)v2; return this; }
    public Header v3() {isV3 = true; return this; }
    
    
    public void write(DataOutputStream dos) throws IOException {
      v = (!isCompressed && !isDelta) ? 1 : 0;
      if (isDelta) v |= 0x02;
      if (isCompressed) v |= 0x04;
      if (typeSystemIndexDefIncluded) v |= 0x08;
      if (typeSystemIncluded) v |= 0x10;
      v |= (seqVersionNbr << 8);
      if (isV3) v |= 0x010000;
      
      byte[] uima = new byte[4];
      uima[0] = 85; // U
      uima[1] = 73; // I
      uima[2] = 77; // M
      uima[3] = 65; // A

      ByteBuffer buf = ByteBuffer.wrap(uima);
      int key = buf.asIntBuffer().get();

      dos.writeInt(key);
      dos.writeInt(v);
      
      if (isCompressed) {
        dos.writeInt(form6 ? 1 : 0);
      }
      
    }
    
    /* ******** Header Properties **********/
    public boolean isDelta() {
      return isDelta;
    }
    public boolean isCompressed() {
      return isCompressed;
    }
    public boolean isForm4() {
      return form4;
    }
    public boolean isForm6() {
      return form6;
    }
    public boolean isTypeSystemIndexDefIncluded() {
      return typeSystemIndexDefIncluded;
    }
    public boolean isTypeSystemIncluded() {
      return typeSystemIncluded;
    }    
    public byte getSeqVersionNbr() {
      return seqVersionNbr;
    }
    public boolean isV3() {
      return isV3;
    }

    
  }
  
  public static Header createHeader() {
    return new Header();
  }
  
  public static boolean isBinaryHeader(DataInputStream dis) {
    dis.mark(4);
    byte[] bytebuf = new byte[4];
    try {
      bytebuf[0] = dis.readByte(); // U
      bytebuf[1] = dis.readByte(); // I
      bytebuf[2] = dis.readByte(); // M
      bytebuf[3] = dis.readByte(); // A
      String s = new String(bytebuf, "UTF-8");
      return s.equals("UIMA") || s.equals("AMIU");
    } catch (IOException e) {
      return false;
    } finally {
      try {
        dis.reset();
      } catch (IOException e) {
        throw new UIMARuntimeException(e);
      }
    }
  }

  public static Header readHeader(DataInputStream dis) throws IOException {

    Header h = new Header();
    // key
    // determine if byte swap if needed based on key
    byte[] bytebuf = new byte[4];
    bytebuf[0] = dis.readByte(); // U
    bytebuf[1] = dis.readByte(); // I
    bytebuf[2] = dis.readByte(); // M
    bytebuf[3] = dis.readByte(); // A

    h.swap = (bytebuf[0] != 85);
    Reading r = new Reading(dis, h.swap);
    h.reading = r;

    int v = h.v = r.readInt();  // h.v for error message use
    
    h.isDelta = (v & 2) != 0;
    h.isCompressed = (v & 4) != 0;
    h.typeSystemIndexDefIncluded = (v & 8) != 0;
    h.typeSystemIncluded = (v & 16) != 0;
    h.seqVersionNbr = (byte) ((v & 0xFF00) >> 8);
    h.isV3 = (v & 0x010000) != 0;
    
    if (h.isCompressed) {
      v = r.readInt();
      h.form4 = v == 0;
      h.form6 = v == 1;
    } 
    
    return h;
  }

  public static DataOutputStream maybeWrapToDataOutputStream(OutputStream os) {
    if (os instanceof DataOutputStream) {
      return (DataOutputStream) os;
    }
    return new DataOutputStream(os);
  }
  
  public static DataInputStream maybeWrapToDataInputStream(InputStream os) {
    if (os instanceof DataInputStream) {
      return (DataInputStream) os;
    }
    return new DataInputStream(os);
  }

  /** 
   * byte swapping reads of integer forms
   */
 
  public static class Reading {
    final DataInputStream dis;
    final boolean swap;
    
    Reading(DataInputStream dis, boolean swap) {
      this.dis = dis;
      this.swap = swap;
    }
    
    long readLong() throws IOException {
      long v = dis.readLong();
      return swap ? Long.reverseBytes(v) : v;
    }
    
    int readInt() throws IOException {
      int v = dis.readInt();
      return swap ? Integer.reverseBytes(v) : v;
    }
    
    short readShort() throws IOException {
      short v = dis.readShort();
      return swap ? Short.reverseBytes(v) : v;
    }

  }
  
}
