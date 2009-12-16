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

package org.apache.vinci.transport;

import java.io.UTFDataFormatException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.vinci.transport.util.Base64Converter;
import org.apache.vinci.transport.util.Base64FormatException;
import org.apache.vinci.transport.util.UTFConverter;

/**
 * Class encapsulating leaf data from a Frame. Internally, leaf data is always represented as UTF-8.
 * Most people will never have to use this class directly unless implementing specialized Frame
 * document types.
 * 
 * While FrameLeaf is effectively an immutable class, any descendents that implement setAttributes
 * of the base class FrameComponent are not likely to be immutable.
 */

public class FrameLeaf extends FrameComponent {
  /**
   * If you call toString() on a FrameLeaf which contains binary data, you get this string as the
   * result.
   */
  static public final String NOT_UTF8_ERROR = "*** ERROR: Data not utf8 ***";

  private final byte[] data;

  /**
   * Create a frameleaf from existing UTF-8 (or true binary) data.
   * 
   * WARNING: Does not copy the array. Caller is responsible for ensuring the provided byte array
   * cannot be modified by external code.
   * 
   * @pre mydata != null
   */
  public FrameLeaf(byte[] mydata, boolean encode) {
    if (encode) {
      this.data = Base64Converter.convertBinaryToBase64(mydata);
    } else {
      // data must be valid UTF-8 unless using binary transport hack.
      this.data = mydata;
    }
  }

  /**
   * @pre mydata != null
   */
  public FrameLeaf(String mydata) {
    this.data = UTFConverter.convertStringToUTF(mydata);
  }

  /**
   * This method does NOT support null values in the array.
   * 
   * @pre mystring != null
   * @pre { for (int i = 0; i < mystring.length; i++) $assert(mystring[i] != null, "array elements
   *      are non-null"); }
   */
  public FrameLeaf(String[] mystring) {
    String separator = "#";
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < mystring.length; i++) {
      if (mystring[i].indexOf(separator) != -1) {
        separator += String.valueOf((int) (Math.random() * 10));
        i--;
      }
    }
    if (separator.length() > 1) {
      separator += "#";
      buf.append(separator);
    }
    for (int i = 0; i < mystring.length; i++) {
      if (i != 0) {
        buf.append(separator);
      }
      buf.append(mystring[i]);
    }
    this.data = UTFConverter.convertStringToUTF(buf.toString());
  }

  public FrameLeaf(float myfloat_) {
    this(Float.toString(myfloat_));
  }

  /**
   * @pre myfloat != null
   */
  public FrameLeaf(float[] myfloat) {
    StringBuffer add_me = new StringBuffer();
    for (int i = 0; i < myfloat.length; i++) {
      add_me.append(Float.toString(myfloat[i]));
      if (i != myfloat.length - 1) {
        add_me.append(' ');
      }
    }
    this.data = UTFConverter.convertStringToUTF(add_me.toString());
  }

  public FrameLeaf(double myfloat) {
    this(Double.toString(myfloat));
  }

  /**
   * @pre mydouble != null
   */
  public FrameLeaf(double[] mydouble) {
    StringBuffer add_me = new StringBuffer();
    for (int i = 0; i < mydouble.length; i++) {
      /** @pre mydouble[i] != null */
      add_me.append(Double.toString(mydouble[i]));
      if (i != mydouble.length - 1) {
        add_me.append(' ');
      }
    }
    this.data = UTFConverter.convertStringToUTF(add_me.toString());
  }

  public FrameLeaf(int myint_) {
    this(Integer.toString(myint_));
  }

  /**
   * @pre myint != null
   */
  public FrameLeaf(int[] myint) {
    StringBuffer add_me = new StringBuffer();
    for (int i = 0; i < myint.length; i++) {
      /** @pre myint[i] != null */
      add_me.append(Integer.toString(myint[i]));
      if (i != myint.length - 1) {
        add_me.append(' ');
      }
    }
    this.data = UTFConverter.convertStringToUTF(add_me.toString());
  }

  public FrameLeaf(long myint) {
    this(Long.toString(myint));
  }

  /**
   * @pre mylong != null
   */
  public FrameLeaf(long[] mylong) {
    StringBuffer add_me = new StringBuffer();
    for (int i = 0; i < mylong.length; i++) {
      /** @pre mylong[i] != null */
      add_me.append(Long.toString(mylong[i]));
      if (i != mylong.length - 1) {
        add_me.append(' ');
      }
    }
    this.data = UTFConverter.convertStringToUTF(add_me.toString());
  }

  public FrameLeaf(boolean bool) {
    this(bool ? TransportConstants.TRUE_VALUE : TransportConstants.FALSE_VALUE);
  }

  public String toString() {
    try {
      return UTFConverter.convertUTFToString(data);
    } catch (UTFDataFormatException e) {
      // E-frame data should ALWAYS be valid UTF
      // ^^ Except due to hack methods for transporting pure binary without B64 overhead
      return NOT_UTF8_ERROR;
    }
  }

  public String[] toStringArray() {
    String work = toString();
    if (work.indexOf('#') == -1) {
      if (work.length() > 0) {
        String[] return_me = new String[1];
        return_me[0] = work;
        return return_me;
      } else {
        return new String[0];
      }
    } else if (work.charAt(0) != '#') {
      // This would be so much easier with JDK1.4 split()
      int size = 0;
      for (int i = 0; i < work.length(); i++) {
        if (work.charAt(i) == '#') {
          size++;
        }
      }
      size++;
      String[] return_me = new String[size];
      int begin = 0;
      int end = 0;
      for (int i = 0; i < size - 1; i++) {
        end = work.indexOf('#', begin + 1);
        return_me[i] = work.substring(begin, end);
        begin = end + 1;
      }
      return_me[size - 1] = work.substring(begin);
      return return_me;
    } else {
      // This would be so much easier with JDK1.4 split()
      int end = work.indexOf('#', 1);
      if (end == -1) {
        throw new LeafCastException("Not a string array: " + toString());
      }
      String separator = work.substring(0, end + 1);
      int begin = end + 1;
      List strings = new ArrayList();
      while ((end = work.indexOf(separator, begin)) != -1) {
        strings.add(work.substring(begin, end));
        begin = end + separator.length();
      }
      strings.add(work.substring(begin));
      String[] return_me = new String[strings.size()];
      for (int i = 0; i < return_me.length; i++) {
        return_me[i] = strings.get(i).toString();
      }
      return return_me;
    }
  }

  /**
   * Get the raw (usually UTF-8) frame data.
   */
  public byte[] getData() {
    return data;
  }

  /**
   * Converts the B64 encoded data to binary and returns it.
   * 
   * @exception LeafCastException
   *              if the data was not base64 encoded.
   */
  public byte[] toBytes() {
    try {
      return Base64Converter.convertBase64ToBinary(data);
    } catch (Base64FormatException e) {
      throw new LeafCastException("Not base64: " + e.getMessage());
    }
  }

  /**
   * Converts the UTF-8 data to a Java long type.
   * 
   * @exception LeafCastException
   *              if the data could not be converted to long.
   */
  public long toLong() {
    try {
      return UTFConverter.convertUTFToLong(data);
    } catch (UTFDataFormatException e) {
      throw new LeafCastException(NOT_UTF8_ERROR);
    } catch (NumberFormatException e) {
      throw new LeafCastException("Not an integer: " + toString());
    }
  }

  /**
   * Converts the UTF-8 data to a Java array of longs.
   * 
   * @exception LeafCastException
   *              if the data could not be convered to a long array.
   */
  public long[] toLongArray() {
    try {
      String array_string = UTFConverter.convertUTFToString(data);
      StringTokenizer tokenizer = new StringTokenizer(array_string);
      List tokens = new ArrayList();
      while (tokenizer.hasMoreTokens()) {
        tokens.add(tokenizer.nextToken());
      }
      long[] return_me = new long[tokens.size()];
      for (int i = 0; i < tokens.size(); i++) {
        return_me[i] = Long.parseLong((String) (tokens.get(i)));
      }
      return return_me;
    } catch (UTFDataFormatException e) {
      throw new LeafCastException(NOT_UTF8_ERROR);
    } catch (NumberFormatException e) {
      throw new LeafCastException("Array contains non-long: " + toString());
    }
  }

  /**
   * Converts the UTF-8 data to a Java int type.
   * 
   * @exception LeafCastException
   *              if the data could not be converted to int.
   */
  public int toInt() {
    try {
      return UTFConverter.convertUTFToInt(data);
    } catch (UTFDataFormatException e) {
      throw new LeafCastException(NOT_UTF8_ERROR);
    } catch (NumberFormatException e) {
      throw new LeafCastException("Not an integer: " + toString());
    }
  }

  /**
   * Converts the UTF-8 data to a Java array of ints.
   * 
   * @exception LeafCastException
   *              if the data could not be convered to an int array.
   */
  public int[] toIntArray() {
    try {
      String array_string = UTFConverter.convertUTFToString(data);
      StringTokenizer tokenizer = new StringTokenizer(array_string);
      List tokens = new ArrayList();
      while (tokenizer.hasMoreTokens()) {
        tokens.add(tokenizer.nextToken());
      }
      int[] return_me = new int[tokens.size()];
      for (int i = 0; i < tokens.size(); i++) {
        return_me[i] = Integer.parseInt((String) (tokens.get(i)));
      }
      return return_me;
    } catch (UTFDataFormatException e) {
      throw new LeafCastException(NOT_UTF8_ERROR);
    } catch (NumberFormatException e) {
      throw new LeafCastException("Array contains non-integer: " + toString());
    }
  }

  /**
   * Converts the UTF-8 data to a Java float type.
   * 
   * @exception LeafCastException
   *              if the data could not be converted to float.
   */
  public float toFloat() {
    try {
      return UTFConverter.convertUTFToFloat(data);
    } catch (UTFDataFormatException e) {
      throw new LeafCastException(NOT_UTF8_ERROR);
    } catch (NumberFormatException e) {
      throw new LeafCastException("Not a float: " + toString());
    }
  }

  /**
   * Converts the UTF-8 data to a Java array of float.
   * 
   * @exception LeafCastException
   *              if the data could not be convered to a float array.
   */
  public float[] toFloatArray() {
    try {
      String array_string = UTFConverter.convertUTFToString(data);
      StringTokenizer tokenizer = new StringTokenizer(array_string);
      List tokens = new ArrayList();
      while (tokenizer.hasMoreTokens()) {
        tokens.add(tokenizer.nextToken());
      }
      float[] return_me = new float[tokens.size()];
      for (int i = 0; i < tokens.size(); i++) {
        return_me[i] = Float.parseFloat((String) (tokens.get(i)));
      }
      return return_me;
    } catch (UTFDataFormatException e) {
      throw new LeafCastException(NOT_UTF8_ERROR);
    } catch (NumberFormatException e) {
      throw new LeafCastException("Array contains non-float: " + toString());
    }
  }

  /**
   * Converts the UTF-8 data to a Java double type.
   * 
   * @exception LeafCastException
   *              if the data could not be converted to double.
   */
  public double toDouble() {
    try {
      return UTFConverter.convertUTFToDouble(data);
    } catch (UTFDataFormatException e) {
      throw new LeafCastException(NOT_UTF8_ERROR);
    } catch (NumberFormatException e) {
      throw new LeafCastException("Not a double: " + toString());
    }
  }

  /**
   * Converts the UTF-8 data to a Java array of double.
   * 
   * @exception LeafCastException
   *              if the data could not be convered to a double array.
   */
  public double[] toDoubleArray() {
    try {
      String array_string = UTFConverter.convertUTFToString(data);
      StringTokenizer tokenizer = new StringTokenizer(array_string);
      List tokens = new ArrayList();
      while (tokenizer.hasMoreTokens()) {
        tokens.add(tokenizer.nextToken());
      }
      double[] return_me = new double[tokens.size()];
      for (int i = 0; i < tokens.size(); i++) {
        return_me[i] = Double.parseDouble((String) (tokens.get(i)));
      }
      return return_me;
    } catch (UTFDataFormatException e) {
      throw new LeafCastException(NOT_UTF8_ERROR);
    } catch (NumberFormatException e) {
      throw new LeafCastException("Array contains non-double: " + toString());
    }
  }

  /**
   * Converts the UTF-8 data to a Java boolean.
   * 
   * @exception LeafCastException
   *              if the underlying data was not utf-8 (which in general should not happen).
   */
  public boolean toBoolean() {
    try {
      return UTFConverter.convertUTFToBool(data);
    } catch (UTFDataFormatException e) {
      throw new LeafCastException(NOT_UTF8_ERROR);
    }
  }

}
