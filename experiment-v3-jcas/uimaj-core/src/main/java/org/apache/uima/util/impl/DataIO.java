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
package org.apache.uima.util.impl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

/**
 * Methods for working with Data during I/O
 */
public class DataIO {
  
  
  public static final Charset UTF8 = Charset.forName("UTF-8");  // use with String is a java 6, not 5, feature
  public static final String UTF8_FAST = "UTF-8"; // for faster impls
  private static final int SIGNED_INT_VALUE_0x80 = 0x80;
  private static final int MASK_LOW_7 = 0x7f;
  private static final long MASK_LOW_7_LONG = 0x7fL;
  private static final long TOP_LONG_BIT = 0x8000000000000000L;
 
  private static ThreadLocal<CharsetDecoder> DECODER = new ThreadLocal<CharsetDecoder>();
  
  
  public static String decodeUTF8(ByteBuffer in, final int length) {
    // First try fast path - assume chars in 0-127
    fastPath: do {
      if (in.hasArray()) {
        byte[] backingArray = in.array();
        int offset = in.arrayOffset() + in.position();
        if (offset + length > backingArray.length) {
          break fastPath;
        }
//        char[] ca = new char[length];
        // string builder approach avoids copying the char array object
        StringBuilder sb = new StringBuilder(length);
        sb.setLength(length);
        for (int i = 0; i < length; i++) {
          byte b = backingArray[offset + i];
          if (b < 0) { // give up and do it the other way
            break fastPath;
          }
          sb.setCharAt(i, (char)b);
        }
        in.position(in.position() + length);
        return sb.toString();  // doesn't copy the string char array
      } 
    } while (false); // not a real do loop - do only once
  
    CharsetDecoder decoder = DECODER.get();
    if (null == decoder) {
      decoder = UTF8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE);
      DECODER.set(decoder);       
    }
    ByteBuffer partToDecode = in.slice();
    partToDecode.limit(length);
    CharBuffer cb;
    try {
      cb = decoder.decode(partToDecode);
      in.position(in.position() + length);
    } catch (CharacterCodingException e) {
      // should never happen
      throw new RuntimeException(e);
    }
    return cb.toString();
  }
  
  /***************************************************************************************
   * For DataOutput, DataInput
   ***************************************************************************************/
  /**
   * Similar to writeUTF, but ok for strings &gt; 32K bytes long and better for strings &lt; 127
   * string utf-8 length must be &le; Integer.MAX_VALUE - 1 
   * @param string the string to write
   * @param out the output sink
   * @throws IOException passthru
   */
  public static void writeUTFv(String string, DataOutput out) throws IOException {
    if (null == string) {
      out.write(0);
      return;
    }
    byte[] bb = string.getBytes(UTF8_FAST);
    if (bb.length > (Integer.MAX_VALUE - 1)) {
      throw new RuntimeException(String.format("String UTF-8 representation too long, was %,d", bb.length));
    }
    writeVnumber(out, bb.length + 1);  // 0 reserved for null
    out.write(bb);
  }
  
  public static String readUTFv(DataInput in) throws IOException {
    int length = readVnumber(in) - 1;
    if (-1 == length) {
      return null;
    }
    byte[] bb = new byte[length];
    in.readFully(bb);
//    return new String(bb, UTF8_FAST);
    return decodeUTF8(ByteBuffer.wrap(bb), length);
  }

  public static long lengthUTFv(String string) throws UnsupportedEncodingException {
    if (null == string) {
      return 1;
    }
    byte[] bb = string.getBytes(UTF8_FAST);
    if (bb.length > (Integer.MAX_VALUE - 1)) {
      throw new RuntimeException(String.format("String UTF-8 representation too long, was %,d", bb.length));
    }
    int r = lengthVnumber(bb.length + 1);
    return r + bb.length;
  }
  
  /**
   * DataOutputStream writeShort with checking of argument
   * @param out the output sink
   * @param v the value to write
   * @throws IOException passthru
   */
  public static void writeShort(DataOutput out, int v) throws IOException {
    if (v > Short.MAX_VALUE ||
        v < Short.MIN_VALUE) {  
      throw new RuntimeException(String.format(
          "Trying to write int %,d as a short but it doesn't fit", v));
    }
    out.writeShort(v);
  }
 
  /**
   * DataOutputStream writeByte with checking of argument
   * @param out output sink 
   * @param v the value to write
   * @throws IOException passthru
   */
  public static void writeByte(DataOutput out, int v) throws IOException {
    if (v > Byte.MAX_VALUE || 
        v < Byte.MIN_VALUE) {
      throw new RuntimeException(String.format(
          "Trying to write int %,d as a byte but it doesn't fit", v));
    }
    out.write(v);
  }

  /**
   * Write lower 8 bits 
   * @param out output sink
   * @param v the value to write
   * @throws IOException passthru
   */
  public static void writeUnsignedByte(DataOutput out, int v) throws IOException {
    out.write(v);
  }

  /**
   * write a positive or negative number, optimized for fewer bytes near 0
   *   sign put in low order bit, rest of number converted to positive and shifted left 1
   *   max negative written as - 0.
   * @param out output sink
   * @param v the value to write
   * @throws IOException passthru
   */
  // special handling for MIN_VALUE because
  // Math.abs of it "fails".  We instead code it as
  // "-0", a code point not otherwise in use
  public static void writeVPNnumber(DataOutput out, int v) throws IOException {
    if (v == Integer.MIN_VALUE) {
      writeVnumber(out, 1);
    } else {
      if (v < 0) {
        writeVnumber(out, (((long)Math.abs(v)) << 1) | 1);
      } else {
        writeVnumber(out, v << 1);
      }
    }
  }
 
  // special handling for MIN_VALUE because
  // Math.abs of it "fails".  We instead code it as
  // "-0", a code point not otherwise in use
  public static void writeVPNnumber(DataOutput out, long v) throws IOException {
    if (v == Long.MIN_VALUE) {
      writeVnumber(out, 1);
    } else {
      if (v < 0) {
        writeVnumber(out, (Math.abs(v) << 1) | 1);
      } else {
        writeVnumber(out, v << 1);
      }
    }
  }
  // special handling for MIN_VALUE because
  // Math.abs of it "fails".  We instead code it as
  // "-0", a code point not otherwise in use
  public static int lengthVPNnumber(int v) {
    if (v == Integer.MIN_VALUE) {
      return 1;
    } else {
      if (v < 0) {
        return lengthVnumber(((long)(Math.abs(v)) << 1));
      } else {
        return lengthVnumber(v << 1);
      }
    }
  }
  // special handling for MIN_VALUE because
  // Math.abs of it "fails".  We instead code it as
  // "-0", a code point not otherwise in use
  public static int lengthVPNnumber(long v) {
    if (v == Long.MIN_VALUE) {
      return 1;
    } else {
      if (v < 0) {
        return lengthVnumber((Math.abs(v) << 1));
      } else {
        return lengthVnumber(v << 1);
      }
    }
  }

  /**
   * Write a positive number with the fewest bytes possible
   * up to 127 written as a byte
   * high order bit on means get another byte
   * 
   * Note: value treated as unsigned 32 bit int
   * 
   * @param out output sink
   * @param v the value to write
   * @throws IOException passthru
   */
  public static void writeVnumber(final DataOutput out, final int v) throws IOException {
    if ((v >= 0) && v < 128) {
      out.write(v);  // fast path
    } else {
      writeVnumber1(out, v);
    }
  }
  
  private static void writeVnumber1(final DataOutput out, int v) throws IOException {
    if (v < 0) {
      throw new RuntimeException("never happen");
    }
    for (int i = 0; i < 5; i++) {
      int outByte = v & MASK_LOW_7;
      if (v < SIGNED_INT_VALUE_0x80) {
        out.write(v);
        return;
      }
      out.write(outByte | SIGNED_INT_VALUE_0x80);
      v = v >>> 7;
    }
  }
  
  public static int lengthVnumber(int v) {
    int r = 1;
    for (int i = 0; i < 5; i++) {
      if (v < SIGNED_INT_VALUE_0x80) {
        return r;
      }
      v = v >>> 7;
      r++;
    }
    throw new RuntimeException("Never get here");
  }

  public static int readVnumber(final DataInput in) throws IOException {
    int raw = in.readUnsignedByte();
    if (raw < 0x80) {   // fast path
      return raw;
    }
    int result = raw & MASK_LOW_7;
    int shift = 7;
    
    for (int i = 1; i < 5; i++) {
      raw = in.readUnsignedByte();
      result |= (raw & MASK_LOW_7) << shift;
      if (raw < SIGNED_INT_VALUE_0x80) {
        return result;
      }
      shift += 7;
    }
    throw new IllegalStateException("Invalid input deserializing Vnumber");   
  }

  /**
   * Write a positive long with the fewest bytes possible; up to 127 written as a byte, high order
   * bit on means get another byte.
   * 
   * @param out output sink
   * @param v the value to write is never negative
   * @throws IOException passthru
   */
  public static void writeVnumber(final DataOutput out, final long v) throws IOException {
    if ((v >= 0) && v < 128) {
      out.write((int)v);  // fast path
    } else {
      writeVnumber1(out, v);
    }
  }
  
  private static void writeVnumber1(final DataOutput out, long v) throws IOException {
    if (v < 0) {
      throw new RuntimeException("never happen");
    }
    for (int i = 0; i < 9; i++) {
      if (v < SIGNED_INT_VALUE_0x80) {
        out.write((int) v);
        return;
      }
      int outByte = (int)(v & MASK_LOW_7_LONG);
      out.write(outByte | SIGNED_INT_VALUE_0x80);
      v = v >>> 7;
    }
  }
  
  public static int lengthVnumber(long v) {
    int r = 1;
    for (int i = 0; i < 9; i++) {
      if (v < SIGNED_INT_VALUE_0x80) {
        return r;
      }
      v = v >>> 7;
      r++;
    }
    throw new RuntimeException("Never get here");
  }


  public static long readVlong(final DataInput in) throws IOException {
    long raw = in.readUnsignedByte();
    if (raw < 0x80) {   // fast path
      return raw;
    }

    long result = raw & MASK_LOW_7_LONG;
    int shift = 7;
    for (int i = 1; i < 9; i++) {
      raw = in.readUnsignedByte();
      result |= (raw & MASK_LOW_7_LONG) << shift;
      if (raw < 128) {
        return result;
      }
      shift += 7;
    }
    throw new IllegalStateException("Invalid input deserializing Vlong");
  }
  
  public static long readRestOfVlong(DataInput in, int firstByte) throws IOException {
    if (firstByte < 0x80) {
      return firstByte;
    }
    long result = firstByte ^ 0x80;  // turn off high bit
    int shift = 7;
    for (int i = 1; i < 9; i++) {
      long raw = in.readUnsignedByte();
      result |= (raw & MASK_LOW_7_LONG) << shift;
      if (raw < 128) {
        return result;
      }
      shift += 7;
    }
    throw new IllegalStateException("Invalid input deserializing Vlong");
    
  }

  public static void writeByteArray(DataOutput out, byte[] v) throws IOException {
    writeVnumber(out, v.length);
    out.write(v);
  }

  public static byte[] readByteArray(DataInput in) throws IOException {
    int size = readVnumber(in);
    byte[] result = new byte[size];
    in.readFully(result);
    return result;
  }

  /**
   * write array preceded by its length
   * @param out output sink
   * @param v the value to write
   * @throws IOException passthru
   */
  public static void writeIntArray(DataOutput out, int[] v) throws IOException {
    writeVnumber(out, v.length);
    for (int vi : v) {
      out.writeInt(vi);
    }
  }
  
  public static int[] readIntArray(DataInput in) throws IOException {
    int size = readVnumber(in);
    int[] result = new int[size];
    for (int i = 0; i < size; i++) {
      result[i] = in.readInt();
    }
    return result;
  }
  
  /**
   * Write delta encoded value, for increasing values
   * @param out output sink
   * @param v the value to write
   * @throws IOException passthru
   */
  public static void writeIntArrayDelta(DataOutput out, int[] v) throws IOException {
    writeVnumber(out, v.length);
    int prev = 0;
    for (int vi : v) {
      writeVnumber(out, vi - prev);
      prev = vi;
    }
  }
  
  public static int[] readIntArrayDelta(DataInput in) throws IOException {
    int size = readVnumber(in);
    int prev = 0;
    int[] result = new int[size];
    for (int i = 0; i < size; i++) {
      result[i] = prev + readVnumber(in);
      prev = result[i];
    }
    return result;
  }

  public static void writeLongArray(DataOutput out, long[] v) throws IOException {
    // java doesn't support arrays longer than Integer.MAX_VALUE, even on 64-bit platforms
    writeVnumber(out, v.length);
    for (long vi : v)
      out.writeLong(vi);
  }
  
  public static long[] readLongArray(DataInput in) throws IOException {
    int size = readVnumber(in);
    long[] v = new long[size];
    for (int i = 0; i < size; ++i)
      v[i] = in.readLong();
    return v;
  }
  
  public static void writeLongArrayDelta(DataOutput out, long[] v) throws IOException {
    // java doesn't support arrays longer than Integer.MAX_VALUE, even on 64-bit platforms
    writeVnumber(out, v.length);
    long prev = 0;
    for (long vi : v) {
      writeVnumber(out, vi - prev);
      prev = vi;
    }
  }
  
  public static long[] readLongArrayDelta(DataInput in) throws IOException {
    int size = readVnumber(in);
    long[] v = new long[size];
    long prev = 0;
    for (int i=0; i<size; ++i) {
      v[i] = prev + readVlong(in);
      prev = v[i];
    }
    return v;
  }  
  
  public static int readUnsignedByte(DataInput in) throws IOException {
    int r =  in.readUnsignedByte();
    if (r < 0) {
      throw new IOException("Premature EOF");
    }
    return r;
  }
  
}
