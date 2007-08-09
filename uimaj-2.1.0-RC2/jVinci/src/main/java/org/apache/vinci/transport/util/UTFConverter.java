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

import java.io.UTFDataFormatException;

/**
 * Provides utility methods for Java string <==> UTF-8 conversion. We don't use the default Java
 * methods for UTF-8 since they are non-standard and not as efficient as this implementation.
 */
public class UTFConverter {
  public static final String TRUE_VALUE = "true";

  public static final String FALSE_VALUE = "false";

  /**
   * Private Constructor
   */
  private UTFConverter() {
  }

  /**
   * Convert the UTF-8 contents of a byte array of UTF-8 bytes to a float.
   * 
   * @param bytearr
   *          Array of bytes.
   * @return float.
   * @throws UTFDataFormatException
   * @throws NumberFormatException
   */
  static public float convertUTFToFloat(byte[] bytearr) throws UTFDataFormatException,
          NumberFormatException {
    return Float.parseFloat(UTFConverter.convertUTFToString(bytearr));
  }

  /**
   * Convert the UTF-8 contents of a byte array to a double.
   * 
   * @param bytearr
   *          Array of bytes.
   * @return double.
   * @throws UTFDataFormatException
   * @throws NumberFormatException
   */
  static public double convertUTFToDouble(byte[] bytearr) throws UTFDataFormatException,
          NumberFormatException {
    return Double.parseDouble(UTFConverter.convertUTFToString(bytearr));
  }

  /**
   * Convert the UTF-8 contents of a byte array to a boolean.
   * 
   * @param bytearr
   *          Array of bytes.
   * @return boolean.
   * @throws UTFDataFormatException
   */
  static public boolean convertUTFToBool(byte[] bytearr) throws UTFDataFormatException {
    return TRUE_VALUE.equals(UTFConverter.convertUTFToString(bytearr));
  }

  /**
   * Convert the UTF-8 contents of a byte array to an int.
   * 
   * @param bytearr
   *          Array of bytes.
   * @return int.
   * @throws UTFDataFormatException
   * @throws NumberFormatException
   */
  static public int convertUTFToInt(byte[] bytearr) throws UTFDataFormatException,
          NumberFormatException {
    return Integer.parseInt(UTFConverter.convertUTFToString(bytearr));
  }

  /**
   * Convert the UTF-8 contents of a byte array to a long.
   * 
   * @param bytearr
   *          Array of bytes.
   * @return long.
   * @throws UTFDataFormatException
   * @throws NumberFormatException
   */
  static public long convertUTFToLong(byte[] bytearr) throws UTFDataFormatException,
          NumberFormatException {
    return Long.parseLong(UTFConverter.convertUTFToString(bytearr));
  }

  /**
   * Convert the UTF-8 contents of a byte array to a Java String.
   * 
   * @param bytearr
   *          Array of bytes.
   * @return String.
   * @throws UTFDataFormatException
   */
  static public String convertUTFToString(byte[] bytearr) throws UTFDataFormatException {
    char[] result = new char[bytearr.length];
    // ^^ We rely on the fact that the length of the string cannot exceed the length
    // of the underlying representation.
    int outputLength = convertUTFToString(bytearr, 0, bytearr.length, result); // pfh
    return new String(result, 0, outputLength);
  }

  /**
   * Convert the UTF-8 contents of a byte array to a Java String.
   * 
   * @param bytearr
   *          Array of bytes.
   * @param beginOffset
   *          Start offest to data in byte array.
   * @param inputLength
   *          Length of the data to convert.
   * @param result
   *          Character array containing the converted characters.
   * @return The length of the converted characters.
   * @throws UTFDataFormatException
   */
  static public int convertUTFToString(byte[] bytearr, final int beginOffset,
          final int inputLength, char[] result) throws UTFDataFormatException {
    int outputLength = 0;
    int count = beginOffset;
    int c1, c2, c3;
    int endOffset = inputLength + count;

    while (count < endOffset) {
      c1 = (bytearr[count++] & 0xff);
      switch (c1 >> 4) {
        case 0:
        case 1:
        case 2:
        case 3:
        case 4:
        case 5:
        case 6:
        case 7:
          result[outputLength++] = (char) c1;
          break;
        case 12:
        case 13:
          if (count + 1 > inputLength)
            throw new UTFDataFormatException();
          c2 = bytearr[count++];
          result[outputLength++] = (char) (((c1 & 0x1F) << 6) | (c2 & 0x3F));
          break;
        case 14:
          if (count + 2 > inputLength)
            throw new UTFDataFormatException();
          c2 = bytearr[count++];
          c3 = bytearr[count++];
          result[outputLength++] = (char) (((c1 & 0x0F) << 12) | ((c2 & 0x3F) << 6) | (c3 & 0x3F));
          break;
        default:
          throw new UTFDataFormatException();
      }
    }
    return outputLength;
  }

  /**
   * Convert a Java String to UTF-8.
   * 
   * @param inputString
   *          String to convert.
   * @return array of UTF-8 bytes.
   */
  static public byte[] convertStringToUTF(String inputString) {
    int resultLength = calculateUTFLength(inputString);
    byte[] resultArray = new byte[resultLength];
    convertStringToUTF(inputString, resultArray);
    return resultArray;
  }

  /**
   * Convert a String from a character array to UTF-8.
   * 
   * @param inputArray
   *          Array of characters to convert.
   * @param startOffset
   *          Start offset in character array.
   * @param endOffset
   *          One past the last character in the array.
   * @return A byte array with the converted result.
   */
  static public byte[] convertStringToUTF(char[] inputArray, int startOffset, int endOffset) // pfh
  {
    int resultLength = calculateUTFLength(inputArray, startOffset, endOffset);
    byte[] resultArray = new byte[resultLength];
    convertStringToUTF(inputArray, startOffset, endOffset, resultArray);
    return resultArray;
  }

  /**
   * Calculate the UTF-8 length of a character array.
   * 
   * @param inputArray
   *          Array of characters.
   * @param startOffset
   *          Start offset of the data in the character array.
   * @param endOffset
   *          One past the last character in the array.
   * @return The number of bytes in the UTF-8 representation.
   */
  static public int calculateUTFLength(char[] inputArray, int startOffset, int endOffset) // pfh
  {
    int resultLength = 0;

    // First calculate the length of the result
    for (int i = startOffset; i < endOffset; i++) {
      int c = inputArray[i];
      if ((c >= 0x0000) && (c <= 0x007F))
        resultLength++;
      else if (c > 0x07FF)
        resultLength += 3;
      else
        resultLength += 2;
    }
    return resultLength;
  }

  /**
   * Calculate the UTF-8 length of a Java String.
   * 
   * @param inputString
   *          The String to calculate the length of.
   * @return The number of bytes in the UTF-8 representation.
   */
  static public int calculateUTFLength(String inputString) {
    int resultLength = 0;
    final int inputLength = inputString.length();
    // First calculate the length of the result
    for (int i = 0; i < inputLength; i++) {
      int c = inputString.charAt(i);
      if ((c >= 0x0000) && (c <= 0x007F))
        resultLength++;
      else if (c > 0x07FF)
        resultLength += 3;
      else
        resultLength += 2;
    }
    return resultLength;
  }

  /**
   * Convert the given char[] input into UTF-8 and place in the destination buffer. This method
   * assumes the destination buffer is big enough to hold the output.
   * 
   * @param inputArray
   *          Array of characters to convert.
   * @param startOffset
   *          Start offset in character array.
   * @param endOffset
   *          One past the last character in the array.
   * @param resultArray
   *          Byte array containing the converted characters.
   * @return The number of characters in the UTF-8 representation.
   */
  static public int convertStringToUTF(char[] inputArray, int startOffset, int endOffset,
          byte[] resultArray) {
    int resultLength = 0;
    resultLength = 0;
    // Now populate the result array
    for (int i = startOffset; i < endOffset; i++) {
      int c = inputArray[i];
      if ((c >= 0x0000) && (c <= 0x007F))
        resultArray[resultLength++] = (byte) c;
      else if (c > 0x07FF) {
        resultArray[resultLength++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
        resultArray[resultLength++] = (byte) (0x80 | ((c >> 6) & 0x3F));
        resultArray[resultLength++] = (byte) (0x80 | (c & 0x3F));
      } else {
        resultArray[resultLength++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
        resultArray[resultLength++] = (byte) (0x80 | (c & 0x3F));
      }
    }
    return resultLength;
  }

  /**
   * Convert the given char[] input into UTF-8 and place in the destination buffer. This method
   * assumes the destination buffer is big enough to hold the output.
   * 
   * @param inputString
   *          String to convert.
   * @param resultArray
   *          Byte array containing the converted characters.
   * @return the number of characters in the UTF-8 representation.
   */
  static public int convertStringToUTF(String inputString, byte[] resultArray) {
    int resultLength = 0;
    final int inputLength = inputString.length();
    // Now populate the result array
    for (int i = 0; i < inputLength; i++) {
      int c = inputString.charAt(i);
      if ((c >= 0x0000) && (c <= 0x007F))
        resultArray[resultLength++] = (byte) c;
      else if (c > 0x07FF) {
        resultArray[resultLength++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
        resultArray[resultLength++] = (byte) (0x80 | ((c >> 6) & 0x3F));
        resultArray[resultLength++] = (byte) (0x80 | (c & 0x3F));
      } else {
        resultArray[resultLength++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
        resultArray[resultLength++] = (byte) (0x80 | (c & 0x3F));
      }
    }
    return resultLength;
  }
}
