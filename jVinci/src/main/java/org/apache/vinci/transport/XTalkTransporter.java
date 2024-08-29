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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.vinci.transport.util.TransportableConverter;
import org.apache.vinci.transport.util.UTFConverter;

/**
 * Implements XTalk marshalling of Frames.
 */
public class XTalkTransporter implements FrameTransporter {

  private static final int OVERSIZE_KEY_LENGTH = 1024 * 1024 * 1024;

  public static final byte DOCUMENT_MARKER = (byte) 'X';

  public static final byte ELEMENT_MARKER = (byte) 'E';

  public static final byte PI_MARKER = (byte) 'p';

  public static final byte STRING_MARKER = (byte) 's';

  public static final byte VERSION_CODE = (byte) 0;

  private static final String OVERSIZE_FIELD = "Oversize field: ";

  /**
   * Parse the data-stream according to the XTalk protocol.
   * 
   * @param is
   *          input stream
   * @param f
   *          frame
   * @return If the first tag belongs to the Vinci namespace, then this tag/value combination is
   *         returned. Otherwise returns null. Should there be a non-null return, then the value
   *         object of the KeyValuePair can be either FrameLeaf or Frame.
   * @pre is != null
   * @pre f != null
   */
  @Override
  public KeyValuePair fromStream(InputStream is, Frame f) throws IOException, EOFException {
    char[] cbuffer = new char[128];
    byte[] buffer = new byte[128];
    int marker = is.read();
    if (marker == -1) {
      throw new EOFException();
    }
    if ((byte) marker != DOCUMENT_MARKER) {
      throw new IOException("Expected document marker: " + (char) marker);
    }
    return fromStreamWork(is, f, buffer, cbuffer);
  }

  /**
   * Once we know that this is an XTalk document, perform XTalk parsing.
   * 
   * @pre is != null
   * @pre f != null
   * 
   * @param is
   *          input stream
   * @param f
   *          frame
   * @return the key value pair
   * @throws IOException
   *           if the versions don't match, or have unexpected element marker
   */
  public KeyValuePair fromStreamWork(InputStream is, Frame f) throws IOException {
    return fromStreamWork(is, f, new byte[128], new char[128]);
  }

  public KeyValuePair fromStreamWork(InputStream is, Frame f, byte[] buffer, char[] cbuffer)
          throws IOException {
    int version = is.read();
    if ((byte) version != VERSION_CODE) {
      throw new IOException(
              "Xtalk version code doesn't match " + (int) VERSION_CODE + ": " + version);
    }
    int top_field_count = readInt(is);
    // Skip over intro PI's.
    int marker;
    while ((marker = is.read()) == PI_MARKER) {
      ignorePI(is);
      top_field_count--;
    }
    if ((byte) marker != ELEMENT_MARKER) {
      throw new IOException("Expected element marker: " + (char) marker);
    }
    KeyValuePair return_me = consumeRootChildren(is, f, buffer, cbuffer);
    top_field_count--;
    // Skip over trailing PI's
    while (top_field_count > 0) {
      marker = is.read();
      if ((byte) marker != 'p') {
        throw new IOException("Expected pi marker: " + (char) marker);
      }
      ignorePI(is);
      top_field_count--;
    }
    return return_me;
  }

  /**
   * 
   * @param is
   *          the input stream
   * @throws IOException
   *           passthru
   * 
   * @pre is != null
   */
  private Attributes consumeAttributes(InputStream is, byte[] buffer, char[] cbuffer)
          throws IOException {
    int attribute_count = readInt(is);
    if (attribute_count < 1) {
      return null;
    }
    Attributes map = new Attributes(attribute_count);
    for (int i = 0; i < attribute_count; i++) {
      String akey = consumeString(is, buffer, cbuffer);
      map.add(akey, consumeLeaf(is, map));
    }
    return map;
  }

  /**
   * 
   * @param is
   *          the input stream
   * @throws IOException
   *           passthru
   * 
   * @pre is != null
   */
  protected void ignorePI(InputStream is) throws IOException {
    ignoreString(is);
    ignoreString(is);
  }

  /**
   * 
   * @param is
   *          the input stream
   * @param f
   *          frame
   * @param buffer
   *          -
   * @param cbuffer
   *          -
   * @return key value
   * @throws IOException
   *           passthru
   * 
   * @pre is != null
   * @pre f != null
   */
  public KeyValuePair consumeRootChildren(InputStream is, Frame f, byte[] buffer, char[] cbuffer)
          throws IOException {
    consumeString(is, buffer, cbuffer); // ignore root tag name -- assume it's
    // always vinci:FRAME
    Attributes attributes = consumeAttributes(is, buffer, cbuffer);
    if (attributes != null) {
      f.setAttributes(attributes);
    }
    int field_count = readInt(is);
    KeyValuePair return_me = null;
    if (field_count != 0) {
      int marker = is.read();
      if ((byte) marker == ELEMENT_MARKER) {
        return_me = consumeRootElement(is, f, buffer, cbuffer);
        field_count--;
        if (field_count > 0) {
          marker = is.read();
        } else {
          return return_me;
        }
      }
      consumeChildren(is, f, field_count, marker, buffer, cbuffer);
    }
    return return_me;
  }

  /**
   * 
   * @param is
   *          the input stream
   * @param f
   *          the Frame
   * @param buffer
   *          -
   * @param cbuffer
   *          -
   * @return key value
   * @throws IOException
   *           passthru
   * 
   * @pre is != null
   * @pre f != null
   */
  public KeyValuePair consumeRootElement(InputStream is, Frame f, byte[] buffer, char[] cbuffer)
          throws IOException {
    // This code returns the first ELEMENT as the KeyValuePair header, if its
    // tag
    // is from the Vinci namespace.
    String tag_name = consumeString(is, buffer, cbuffer);
    Attributes attributes = consumeAttributes(is, buffer, cbuffer);
    int sub_field_count = readInt(is);
    KeyValuePair return_me = null;
    FrameComponent value = null;
    if (sub_field_count == 0) {
      value = f.createSubFrame(tag_name, 0);
      if (tag_name.startsWith(TransportConstants.VINCI_NAMESPACE)) {
        return_me = new KeyValuePair(tag_name, new VinciFrame());
        // ^^ Note that the value of the returned keyval must always be of type
        // "VinciFrame" so we cannot simply return the "native" frame in case it
        // it is of a different type.
      }
    } else {
      int sub_marker = is.read();
      if (sub_field_count == 1 && (byte) sub_marker == STRING_MARKER) {
        value = consumeLeaf(is, f);
        if (tag_name.startsWith(TransportConstants.VINCI_NAMESPACE)) {
          return_me = new KeyValuePair(tag_name, value);
        }
      } else {
        value = f.createSubFrame(tag_name, sub_field_count);
        if (tag_name.startsWith(TransportConstants.VINCI_NAMESPACE)) {
          Frame pre_value = new VinciFrame();
          consumeChildren(is, pre_value, sub_field_count, sub_marker, buffer, cbuffer);
          return_me = new KeyValuePair(tag_name, pre_value);
          TransportableConverter.convert(pre_value, (Frame) value);
        } else {
          consumeChildren(is, (Frame) value, sub_field_count, sub_marker, buffer, cbuffer);
        }
      }
    }
    if (attributes != null) {
      value.setAttributes(attributes);
    }
    f.add(tag_name, value);
    return return_me;
  }

  /**
   * 
   * @param is
   *          input stream
   * @param f
   *          frame
   * @param field_count
   *          field count
   * @param marker
   *          marker
   * @param buffer
   *          -
   * @param cbuffer
   *          -
   * @throws IOException
   *           passthru
   * 
   * @pre is != null
   * @pre f != null
   */
  public void consumeChildren(InputStream is, Frame f, int field_count, int marker, byte[] buffer,
          char[] cbuffer) throws IOException {
    while (field_count > 0) {
      switch ((byte) marker) {
        case PI_MARKER:
          ignorePI(is);
          break;
        case STRING_MARKER:
          f.add(TransportConstants.PCDATA_KEY, consumeLeaf(is, f));
          break;
        case ELEMENT_MARKER:
          String tag_name = consumeString(is, buffer, cbuffer);
          Attributes attributes = consumeAttributes(is, buffer, cbuffer);
          int sub_field_count = readInt(is);
          FrameComponent value = null;
          if (sub_field_count == 0) {
            value = f.createSubFrame(tag_name, sub_field_count);
          } else {
            int sub_marker = is.read();
            if (sub_field_count == 1 && (byte) sub_marker == STRING_MARKER) {
              value = consumeLeaf(is, f);
            } else {
              value = f.createSubFrame(tag_name, sub_field_count);
              consumeChildren(is, (Frame) value, sub_field_count, sub_marker, buffer, cbuffer);
            }
          }
          if (attributes != null) {
            value.setAttributes(attributes);
          }
          f.add(tag_name, value);
          break;
        default:
          throw new IOException("Unexpected marker while parsing children: " + (char) marker);
      }
      field_count--;
      if (field_count > 0) {
        marker = is.read();
      }
    }
  }

  /**
   * 
   * @param is
   *          input stream
   * @throws IOException
   *           an EOFException if number to be skipped is bigger than the stream
   * 
   * @pre is != null
   */
  private static void ignoreString(InputStream is) throws IOException {
    long skip_me = readInt(is);
    long count = 0;
    long skipped = count;
    do {
      count = is.skip(skip_me - skipped);
      if (count < 0) {
        throw new EOFException();
      }
      skipped += count;
    } while (skipped < skip_me);
  }

  /**
   * Consume a string from the input stream. TODO: Make a faster version that exploits work buffers
   * to reduce allocations to a single string object.
   * 
   * @param is
   *          input stream
   * @return the string
   * @throws IOException
   *           if key is longer than limit
   * 
   * @pre is != null
   */
  public static String consumeString(InputStream is) throws IOException {
    int utflen = readInt(is);
    if (utflen > OVERSIZE_KEY_LENGTH) {
      throw new IOException(OVERSIZE_FIELD + utflen);
    }
    byte[] bytearr = new byte[utflen];
    readFully(bytearr, is);
    return UTFConverter.convertUTFToString(bytearr);
  }

  public static String consumeString(InputStream is, byte[] buffer, char[] cbuffer)
          throws IOException {
    int utflen = readInt(is);
    if (utflen > OVERSIZE_KEY_LENGTH) {
      throw new IOException(OVERSIZE_FIELD + utflen);
    }
    if (buffer.length < utflen) {
      byte[] bytearr = new byte[utflen];
      readFully(bytearr, is);
      return UTFConverter.convertUTFToString(bytearr);
    } else {
      readFully(buffer, utflen, is);
      int charlen = UTFConverter.convertUTFToString(buffer, 0, utflen, cbuffer);
      return new String(cbuffer, 0, charlen);
    }
  }

  /**
   * Consume the string of bytesToRead utf-8 bytes. assumes buffers are big enough to hold
   * bytesToRead bytes/chars
   * 
   * @param is
   *          input stream
   * @param byteBuf
   *          a byte buffer
   * @param charBuf
   *          a character buffer
   * @param bytesToRead
   *          the number of bytes to read
   * @return the length of the converted characters
   * @throws IOException
   *           passthru
   */
  public static int consumeCharacters(InputStream is, byte[] byteBuf, char[] charBuf,
          int bytesToRead) throws IOException {
    readFully(byteBuf, bytesToRead, is);
    return UTFConverter.convertUTFToString(byteBuf, 0, bytesToRead, charBuf);
  }

  /**
   * 
   * @param is
   *          input stream
   * @param f
   *          frame
   * @throws IOExcetion
   *           if the key length is too large
   * 
   * @pre is != null
   * @pre f != null
   */
  private static FrameLeaf consumeLeaf(InputStream is, Frame f) throws IOException {
    int utflen = readInt(is);
    if (utflen > OVERSIZE_KEY_LENGTH) {
      throw new IOException(OVERSIZE_FIELD + utflen);
    }
    byte[] bytearr = new byte[utflen];
    readFully(bytearr, is);
    return f.createFrameLeaf(bytearr);
  }

  public static final byte[] HEADER = { DOCUMENT_MARKER, VERSION_CODE, 0, 0, 0, 1, ELEMENT_MARKER };

  /**
   * @param os
   *          output stream
   * @param f
   *          frame
   * @throws IOException
   *           passthru
   * @pre os != null
   * @pre f != null
   */
  @Override
  public void toStream(OutputStream os, Frame f) throws IOException {
    byte[] workbuf = new byte[256]; // reduces allocations in string sending
    os.write(HEADER);
    stringToBin("vinci:FRAME", os, workbuf);
    if (f.getAttributes() != null) {
      attributesToBin(os, f.getAttributes(), workbuf);
    } else {
      writeInt(0, os); // no attributes
    }
    elementToBin(os, f, workbuf);
  }

  /**
   * 
   * @param os
   *          output stream
   * @param f
   *          frame
   * @param workbuf
   *          -
   * @throws IOException
   *           passthru
   * 
   * @pre os != null
   * @pre f != null
   */
  public void elementToBin(OutputStream os, Frame f, byte[] workbuf) throws IOException {
    writeInt(f.getKeyValuePairCount(), os);
    KeyValuePair keyVal = null;
    int total = f.getKeyValuePairCount();
    for (int i = 0; i < total; i++) {
      keyVal = f.getKeyValuePair(i);
      if (keyVal.key.equals(TransportConstants.PCDATA_KEY)) { // PCDATA type
        // string
        os.write(STRING_MARKER);
        byte[] data = ((FrameLeaf) keyVal.value).getData();
        writeInt(data.length, os);
        os.write(data);
      } else {
        os.write(ELEMENT_MARKER);
        stringToBin(keyVal.key, os, workbuf);
        Attributes a = keyVal.value.getAttributes();
        if (a != null) {
          attributesToBin(os, keyVal.value.getAttributes(), workbuf);
        } else {
          writeInt(0, os); // no attributes
        }
        if (keyVal.isValueALeaf()) {
          writeInt(1, os);
          os.write(STRING_MARKER);
          byte[] data = keyVal.getValueAsLeaf().getData();
          writeInt(data.length, os);
          os.write(data);
        } else {
          elementToBin(os, keyVal.getValueAsFrame(), workbuf);
        }
      }
    }
  }

  /**
   * Sends a string over, without the type byte.
   * 
   * @param str
   *          the string to send
   * @param os
   *          the output stream
   * @throws IOException
   *           passthru
   * 
   * @pre str != null
   * @pre os != null
   */
  public static void stringToBin(String str, OutputStream os) throws IOException {
    byte[] write_me = UTFConverter.convertStringToUTF(str);
    writeInt(write_me.length, os);
    os.write(write_me);
  }

  /**
   * Sends a string as utf8, using the temporary buffer if it is big enough to avoid allocating new
   * memory.
   * 
   * @param str
   *          the string to send
   * @param os
   *          the output stream
   * @param buffer
   *          a buffer
   * @throws IOException
   *           passthru
   */
  public static void stringToBin(String str, OutputStream os, byte[] buffer) throws IOException {
    byte[] newbuf;
    if (buffer.length < str.length() * 3) {
      int len = UTFConverter.calculateUTFLength(str);
      if (buffer.length < len) {
        // Buffer is too small, create a bigger temporary one.
        newbuf = new byte[len];
      } else {
        newbuf = buffer;
      }
    } else {
      newbuf = buffer;
    }
    int newlen = UTFConverter.convertStringToUTF(str, newbuf);
    writeInt(newlen, os);
    os.write(newbuf, 0, newlen);
  }

  public static void stringToBin(char[] str, int begin, int len, OutputStream os)
          throws IOException {
    byte[] write_me = UTFConverter.convertStringToUTF(str, begin, len);
    writeInt(write_me.length, os);
    os.write(write_me);
  }

  public static void stringToBin(char[] str, int begin, int len, OutputStream os, byte[] buffer)
          throws IOException {
    byte[] newbuf;
    if (buffer.length < (len - begin) * 3) {
      int byteslen = UTFConverter.calculateUTFLength(str, begin, len);
      if (buffer.length < byteslen) {
        // buffer is too small, create a bigger temporary one.
        newbuf = new byte[byteslen];
      } else {
        newbuf = buffer;
      }
    } else {
      newbuf = buffer;
    }
    int newlen = UTFConverter.convertStringToUTF(str, begin, len, newbuf);
    writeInt(newlen, os);
    os.write(newbuf, 0, newlen);
  }

  /**
   * 
   * @param write_me
   *          the integer to write
   * @param out
   *          the output stream
   * @throws IOException
   *           passthru
   * 
   * @pre out != null
   */
  public static void writeInt(int write_me, OutputStream out) throws IOException {
    out.write(write_me >>> 24);
    out.write(write_me >>> 16);
    out.write(write_me >>> 8);
    out.write(write_me);
  }

  /**
   * 
   * @param in
   *          the input stream
   * @return the integer
   * @throws IOException
   *           if any of the pieces read are negative
   * 
   * @pre in != null
   */
  public static int readInt(InputStream in) throws IOException {
    int c1 = in.read();
    int c2 = in.read();
    int c3 = in.read();
    int c4 = in.read();
    if ((c1 | c2 | c3 | c4) < 0) {
      throw new EOFException();
    }
    return (c1 << 24) + (c2 << 16) + (c3 << 8) + c4;
  }

  /**
   * 
   * @param b
   *          the byte array to read into
   * @param in
   *          the input stream
   * @throws IOException
   *           passthru
   * 
   * @pre b != null
   * @pre in != null
   */
  public static void readFully(byte[] b, InputStream in) throws IOException {
    readFully(b, b.length, in);
  }

  public static void readFully(byte[] b, int length, InputStream in) throws IOException {
    int read_so_far = 0;
    while (read_so_far < length) {
      int count = in.read(b, read_so_far, length - read_so_far);
      if (count < 0) {
        throw new EOFException();
      }
      read_so_far += count;
    }
  }

  /**
   * 
   * @param os
   *          the output stream
   * @param attributes
   *          attributes
   * @param workbuf
   *          a working buffer
   * @throws IOException
   *           passthru
   * 
   * @pre os != null
   * @pre attributes != null
   */
  public void attributesToBin(OutputStream os, Attributes attributes, byte[] workbuf)
          throws IOException {
    int size = attributes.getKeyValuePairCount();
    writeInt(size, os);
    for (int i = 0; i < size; i++) {
      KeyValuePair k = attributes.getKeyValuePair(i);
      stringToBin(k.key, os, workbuf);
      byte[] write_me = ((FrameLeaf) k.value).getData();
      writeInt(write_me.length, os);
      os.write(write_me);
    }
  }

} // class
