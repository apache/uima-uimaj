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

package org.apache.vinci.transport.util;

/**
 * Provides utility methods for Binary <=> Base64 conversion.
 */
public class Base64Converter {

  /**
   * If you ask for line-breaks, this is the maximum line length used.
   */
  static public final int     LINE_LENGTH = 70;

  static private final byte[] B64_CODE    = { (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F',
      (byte) 'G', (byte) 'H', (byte) 'I', (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N', (byte) 'O',
      (byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T', (byte) 'U', (byte) 'V', (byte) 'W', (byte) 'X',
      (byte) 'Y', (byte) 'Z', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f', (byte) 'g',
      (byte) 'h', (byte) 'i', (byte) 'j', (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n', (byte) 'o', (byte) 'p',
      (byte) 'q', (byte) 'r', (byte) 's', (byte) 't', (byte) 'u', (byte) 'v', (byte) 'w', (byte) 'x', (byte) 'y',
      (byte) 'z', (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7',
      (byte) '8', (byte) '9', (byte) '+', (byte) '/' };

  /**
   * Utility class not meant to be instantiated.
   */
  private Base64Converter() {
  }

  /**
   * @pre from != null
   */
  static public byte[] convertBinaryToBase64(byte[] from) {
    return convertBinaryToBase64(from, from.length, true);
  }

  /**
   * @pre convert_me != null
   */
  static public String convertStringToBase64String(String convert_me) {
    return new String(convertBinaryToBase64(convert_me.getBytes()));
  }

  /**
   * @pre base64 != null
   */
  static public String convertBase64StringToString(String base64) throws Base64FormatException {
    return new String(convertBase64ToBinary(base64.getBytes()));
  }

  /**
   * @pre count <= from.length
   */
  static public byte[] convertBinaryToBase64(byte[] from, final int count, boolean line_breaks) {
    int size = calculateBase64OutputSize(count, line_breaks);
    byte[] tmp = new byte[size];
    int used = 0;

    for (int done = 0; done < count; done += 3) {
      b64encodeOctet(tmp, used, from, done, count - done);
      used += 4;
    }

    if (line_breaks) {
      byte[] to = new byte[size];
      int pos = 0;
      for (int breaks = 0; breaks < used; breaks += LINE_LENGTH) {
        int length = used - breaks;
        if (length > LINE_LENGTH) {
          length = LINE_LENGTH;
        }
        System.arraycopy(tmp, breaks, to, pos, length);
        pos += length;
        to[pos++] = (byte) '\n';
      }
      //Debug.Assert(pos == to.length);
      return to;
    } else {
      //Debug.Assert(used == tmp.length);
      return tmp;
    }
  }

  /**
   * Calculates the size of the resulting Base64 string returned by this class for
   * a binary byte array of the specified length. Includes carriage returns and all.
   */
  static public int calculateBase64OutputSize(int input_size, boolean line_breaks) {
    int q = input_size / 3;
    if (input_size % 3 != 0) {
      q += 1;
    }
    // Output is always a multiple of 4 characters.
    q *= 4;
    // Factor in the extra length needed for line breaks.
    if (line_breaks) {
      q += (q / LINE_LENGTH) + (q % LINE_LENGTH == 0 ? 0 : 1);
    }
    return q;
  }

  /**
   * @pre input != null
   */
  static public byte[] convertBase64ToBinary(byte[] input) throws Base64FormatException {
    return convertBase64ToBinary(input, input.length);
  }

  /**
   * @pre input != null
   * @pre input_size <= input.length
   */
  static public byte[] convertBase64ToBinary(byte[] input, final int input_size) throws Base64FormatException {
    int output_size = calculateBinaryOutputSize(input, input_size);
    byte[] output = new byte[output_size];
    int pos = 0;
    int i = 0;
    while (pos + 4 <= input_size) {
      pos = b64decodeOctet(input, pos, output, 3 * i, input_size);
      i++;
    }
    return output;
  }

  /**
   * Calculate the number of bytes encoded by a given Base64 input.
   *
   * @pre input != null
   * @pre input_size <= input.length
   */
  static public int calculateBinaryOutputSize(byte[] input, final int input_size) throws Base64FormatException {
    int output_size = 0;
    for (int i = 0; i + 4 <= input_size;) {
      i = consumeInvalidDigits(input, i, input_size);
      i++;
      i = consumeInvalidDigits(input, i, input_size);
      i++;
      i = consumeInvalidDigits(input, i, input_size);
      if (input[i] == (byte) '=') {
        output_size += 1;
        break;
      }
      i++;
      i = consumeInvalidDigits(input, i, input_size);
      if (input[i] == (byte) '=') {
        output_size += 2;
        break;
      }
      i++;
      output_size += 3;
    }
    return output_size;
  }

  static private int consumeInvalidDigits(byte[] in, int off, int max_offset) throws Base64FormatException {
    if (off >= max_offset) {
      throw new Base64FormatException("short read");
    }
    while (!b64validDigit(in[off])) {
      off++;
      if (off >= max_offset) {
        throw new Base64FormatException("short read");
      }
    }
    return off;
  }

  /**
   * @pre in != null
   * @pre out != null
   */
  static private int b64decodeOctet(byte[] in, int in_offset, byte[] out, int out_offset, int max_offset)
      throws Base64FormatException {
    int A;
    int B;
    int C;
    int D;
    in_offset = consumeInvalidDigits(in, in_offset, max_offset);
    A = in[in_offset++];
    if (A < 0) {
      throw new Base64FormatException();
    }
    in_offset = consumeInvalidDigits(in, in_offset, max_offset);
    B = in[in_offset++];
    if (B < 0) {
      throw new Base64FormatException();
    }
    in_offset = consumeInvalidDigits(in, in_offset, max_offset);
    C = in[in_offset++];
    in_offset = consumeInvalidDigits(in, in_offset, max_offset);
    D = in[in_offset++];

    A = b64decodeDigit(A);
    A <<= 2;
    B = b64decodeDigit(B);
    A |= (B >> 4);
    out[out_offset] = (byte) A;

    B <<= 4;
    C = b64decodeDigit(C);
    if (C < 0) {
      return max_offset; // we are done
    }
    B |= C >> 2;
    out[out_offset + 1] = (byte) B;
    C <<= 6;
    D = b64decodeDigit(D);
    if (D < 0) {
      return max_offset; // we are done
    }
    C |= D;
    out[out_offset + 2] = (byte) C;
    return in_offset;
  }

  static private int b64decodeDigit(int c) {
    if (c >= (byte) 'A' && c <= (byte) 'Z') {
      return c - (byte) 'A';
    } else if (c >= (byte) 'a' && c <= (byte) 'z') {
      return c - (byte) 'a' + 26;
    } else if (c >= (byte) '0' && c <= (byte) '9') {
      return c - (byte) '0' + 52;
    } else if (c == (byte) '+') {
      return 62;
    } else if (c == (byte) '/') {
      return 63;
    } else if (c == (byte) '=') {
      return -2;
    }

    return -1;
  }

  static private boolean b64validDigit(byte a) {
    if (a >= (byte) 'A' && a <= (byte) 'Z') {
      return true;
    }
    if (a >= (byte) 'a' && a <= (byte) 'z') {
      return true;
    }
    if (a >= (byte) '0' && a <= (byte) '9') {
      return true;
    }
    if (a == (byte) '/' || a == (byte) '+' || a == (byte) '=') {
      return true;
    }
    return false;
  }

  static private void b64encodeOctet(byte[] to, int to_offset, byte[] from, int from_offset, int count) {
    int A = 0;
    int B = 0;
    int C = 0;
    int D = 0;
    int tmp = 0;

    A = from[from_offset];
    if (A < 0) {
      A += 256;
    }
    A >>= 2;

    B = from[from_offset];
    if (B < 0) {
      B += 256;
    }
    B &= 3;
    B <<= 4;

    if (count > 1) {
      tmp = from[from_offset + 1] < 0 ? (from[from_offset + 1]) + 256 : from[from_offset + 1];
      B |= tmp >> 4;
      C = from[from_offset + 1];
      if (C < 0) {
        C += 256;
      }
      C &= 15;
      C <<= 2;
    }

    if (count > 2) {
      tmp = from[from_offset + 2] < 0 ? (from[from_offset + 2]) + 256 : from[from_offset + 2];
      C |= tmp >> 6;
      D = from[from_offset + 2];
      if (D < 0) {
        D += 256;
      }
      D &= 0x3F;
    }

    to[to_offset] = b64codes(A);
    to[to_offset + 1] = b64codes(B);

    if (count > 1) {
      to[to_offset + 2] = b64codes(C);
    } else {
      to[to_offset + 2] = (byte) '=';
    }

    if (count > 2) {
      to[to_offset + 3] = b64codes(D);
    } else {
      to[to_offset + 3] = (byte) '=';
    }
  }

  static private byte b64codes(int which) {
    if (which < 0) {
      which += 256;
    }
    return B64_CODE[which];
  }

  /*	public static void main(String[] args) throws Base64FormatException {
   int to = 256;
   if (args.length > 0) {
   to = Integer.parseInt(args[0]);
   }
   System.out.println("TESTING case: " + to);
   byte[] input = new byte[to];
   for (int i=0; i<to; i++) {
   input[i] = (byte)i;
   }
   byte[] output = convertBinaryToBase64(input);
   String output_me = new String(output, 0, output.length);
   System.out.println("Encoded string:\n" + output_me);
   input = convertBase64ToBinary(output);
   for (int i=0; i<to; i++) {
   if (input[i] >= 0) {
   Debug.Assert(i == (int)input[i]);
   }
   else {
   Debug.Assert(i == ((int)input[i]) + 256);
   }
   }
   if (to != 0 && args.length > 0) {
   // Test all cases all the way down to 0 length arrays
   args[0] = Integer.toString(to-1);
   main(args);
   }
   }*/
} // class
