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

import org.apache.vinci.debug.Debug;
import org.apache.vinci.transport.util.XMLConverter;

/**
 * Frame is an abstract class that is intended to be extended to implement a simple & lean
 * (restricted) XML document model. A Frame is only capable of representing XML documents with no
 * attributes or processing instructions. Applications which require attributes should use the
 * org.apache.vinci.transport.document.AFrame document model instead.
 * 
 * Frame "decorates" its descendents with several type-safe adder methods for building the XML
 * document. It requires its descendents implement only a single generic adder method [add(String,
 * FrameComponent)] and a getter for retreiving fields of the document by their position
 * [getKeyValuePair(int)]. The Frame descendent QueryableFrame provides additional getter methods.
 * QueryableFrame should be extended instead of Frame if this more powerful query support is
 * necessary.
 * 
 * Frame also implements the Transportable interface and provides a default marshaller for
 * converting to and from XTalk wire format. The marshaller is pluggable to support marshalling to
 * and from other formats if so desired, or to support optimized (e.g. native) implementation.
 * 
 * Typically you will use VinciFrame, a concrete descendent of the (Queryable)Frame class, for most
 * of your code. You may however also wish to implement specialized Frame descendents for optimized
 * and/or type-safe handling of particular queries. For example, see ResolveResult and ServeonResult
 * (in package org.apache.vinci.transport.vns.client), which do this for the two most common VNS
 * queries. Automated stub generators may also wish to extend Frame to generate Java object to Vinci
 * XML document adapters.
 * 
 */
public abstract class Frame extends FrameComponent implements Transportable {

  static private final String XML_INDENT = "   ";

  static private FrameTransporter parser = new XTalkTransporter();

  protected Frame() {
  }

  /**
   * This method lets you replace the default XTalk marshaller with another one, for example if you
   * want to use a different wire format (such as XML/SOAP), or if you want to easily exploit an
   * optimized/native implementation.
   * 
   * @param transporter
   *          The new marshaller to plug in.
   */
  static public void setFrameTransporter(FrameTransporter transporter) {
    parser = transporter;
  }

  /**
   * Get the currently installed document marshaller.
   * 
   * @return The currently installed document marshaller.
   */
  static public FrameTransporter getFrameTransporter() {
    return parser;
  }

  /**
   * Add a tagged value to this frame (value is either a Frame or FrameLeaf). Frames that support
   * marshalling from a stream must implement this method.
   * 
   * While the method is conceptually abstract, I provide a default error-only implementation in
   * cases where marshalling the document to a stream is not necessary (e.g. the document is used as
   * an input source only).
   * 
   * @param tag
   *          The tag name with which to associate the value.
   * @param val
   *          The (Frame | FrameLeaf) value to associate with the tag.
   * @throws UnsupportedOperationException
   */
  /* abstract */public void add(String tag, FrameComponent val) {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the specified KeyValue pair. Frames that support marshalling to (but not from) a stream
   * via the Transportable interface must implement this method. We chose this simple iteration
   * method over providing an interator object since it doesn't require allocating any new objects.
   * 
   * While conceptually abstract, I provide a default error-only implementation to avoid the need to
   * define it in cases where alternative getters are provided for querying the document, and the
   * object is never marshalled to a stream.
   * 
   * @param which
   *          The index of the KeyValuePair to retrieve.
   * @throws UnsupportedOperationException
   * @return The requested KeyValuePair.
   */
  /* abstract */public KeyValuePair getKeyValuePair(int which) {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the number of key/value pairs within this frame. Frames that support marshalling to (but
   * not from) a stream via the Transportable interface must implement this method.
   * 
   * While conceptually abstract, I provide a default error-only implementation to avoid the need to
   * define it in cases where alternative getters are provided for querying the document, and the
   * object is never marshalled to a stream.
   * 
   * @throws UnsupportedOperationException
   * @return The total number of key/value pairs in this frame.
   */
  /* abstract */public int getKeyValuePairCount() {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Populate this document using the given InputStream and the installed marshaller.
   * 
   * @param is
   *          The input stream to read from.
   * @throws IOException
   *           Can come from the underlying input stream.
   * @throws UnsupportedOperationException
   *           if this document model does not support key addition.
   * @return The first key/value pair encountered, if it comes from the Vinci namespace.
   * 
   * @pre is != null
   */
  public KeyValuePair fromStream(InputStream is) throws IOException, EOFException {
    return parser.fromStream(is, this);
  }

  /**
   * Write this document to the given output stream using the installed marshaller.
   * 
   * @param os
   *          The stream to where the document is written.
   * @throws IOException
   *           Can come from the underlying output stream.
   * @throws UnsupportedOperationException
   *           if this document model does not support key iteration.
   * 
   * @pre os != null
   */
  public void toStream(OutputStream os) throws IOException {
    parser.toStream(os, this);
  }

  /**
   * Represent the document as a string (equivlent to toXML()).
   * 
   * @return The document in String format.
   * @throws UnsupportedOperationException
   *           if this document model does not support key iteration.
   */
  public String toString() {
    return toXML();
  }

  /**
   * Convert the document to XML. Performs limited pretty printing -- if you don't want any pretty
   * printing, use toRawXML().
   * 
   * @return The document in XML format as a string.
   * @throws UnsupportedOperationException
   *           if this document model does not support key iteration.
   */
  public String toXML() {
    return toXML(new StringBuffer()).toString();
  }

  /**
   * Convert the document to XML without any pretting printing.
   * 
   * @return The document in XML format as a string.
   * @throws UnsupportedOperationException
   *           if this document model does not support key iteration.
   * @since 2.1.2
   */
  public String toRawXML() {
    return toRawXML(new StringBuffer()).toString();
  }

  /**
   * Convert the document to XML without any pretting printing.
   * 
   * @param buf
   *          The StringBuffer to append the document text to.
   * @return The StringBuffer that was provided as input
   * @throws UnsupportedOperationException
   *           if this document model does not support key iteration.
   * @since 2.1.2
   */
  public StringBuffer toRawXML(StringBuffer buf) {
    buf.append("<vinci:FRAME").append(attributeString(this));
    if (getAttributes() == null || getAttributes().fgetFirst("xmlns:vinci") == null) {
      // Make sure vinci namespace declaration exists since it is implicitly assumed.
      buf.append(" xmlns:vinci=\"").append(TransportConstants.VINCI_NAMESPACE_URI).append('\"');
    }
    buf.append(">");
    toRawXMLWork(buf);
    buf.append("</vinci:FRAME>");
    return buf;
  }

  public void toRawXMLWork(StringBuffer rval) {
    KeyValuePair keyVal = null;
    int total = getKeyValuePairCount();
    for (int i = 0; i < total; i++) {
      keyVal = getKeyValuePair(i);
      boolean pcdata = TransportConstants.PCDATA_KEY.equals(keyVal.key);
      if (!pcdata) {
        rval.append('<').append(keyVal.key).append(attributeString(keyVal.value));
        if (keyVal.value instanceof Frame && ((Frame) keyVal.value).getKeyValuePairCount() == 0) {
          rval.append("/>");
          continue;
        } else {
          rval.append('>');
        }
      }
      if (keyVal.value instanceof FrameLeaf) {
        XMLConverter.convertStringToXMLString(((FrameLeaf) keyVal.value).toString(), rval);
      } else {
        ((Frame) keyVal.value).toRawXMLWork(rval);
      }
      if (!pcdata) {
        rval.append("</").append(keyVal.key).append('>');
      }
    }
  }

  /**
   * Append the document, in XML format, to the provided StringBuffer.
   * 
   * @param buf
   *          The StringBuffer where the document is appended.
   * @return The same StringBuffer provided to this method using the buf argument.
   * @throws UnsupportedOperationException
   *           if this document model does not support key iteration.
   * 
   * @pre buf != null
   */
  public StringBuffer toXML(StringBuffer buf) {
    buf.append("<vinci:FRAME").append(attributeString(this));
    if (getAttributes() == null || getAttributes().fgetFirst("xmlns:vinci") == null) {
      // Make sure vinci namespace declaration exists since it is implicitly assumed.
      buf.append(" xmlns:vinci=\"").append(TransportConstants.VINCI_NAMESPACE_URI).append('\"');
    }
    buf.append(">\n");
    // ^^ Note that Frames do not support XML namespaces, however the above xml
    // namespace is in some sense "implicit" for every Vinci document.
    toXML(buf, 0);
    buf.append("</vinci:FRAME>\n");
    return buf;
  }

  /**
   * Factory method used by fromStream when it needs to create a frame leaf. Default implementation
   * creates a regular FrameLeaf.
   * 
   * @return the created FrameLeaf.
   * 
   * @pre array != null
   */
  public FrameLeaf createFrameLeaf(byte[] array) {
    return new FrameLeaf(array, false);
  }

  /**
   * Factory method used by fromStream when it needs to create a sub-frame. Default implementation
   * creates a subframe of the same type as the current frame.
   * 
   * @throws UnsupportedOperationException
   *           if the getClass().newInstance() call on this object results in an exception.
   * @return the created sub-frame.
   * 
   * @pre tag_name != null
   * @pre initialCapacity >= 0
   */
  public Frame createSubFrame(String tag_name, int initialCapacity) {
    try {
      return (Frame) this.getClass().newInstance();
    }
    // Neither error case should take place, so we handle them by converting them to
    // unchecked exceptions.
    catch (Exception e) {
      Debug.reportException(e);
      throw new UnsupportedOperationException("createSubFrame() failed: " + e);
    }
  }

  /**
   * Helper method used by toXML()
   * 
   * @pre c != null
   */
  private String attributeString(FrameComponent c) {
    Attributes attributes = c.getAttributes();
    if (attributes == null) {
      return "";
    }
    StringBuffer buf = new StringBuffer(80);
    for (int i = 0; i < attributes.getKeyValuePairCount(); i++) {
      KeyValuePair k = attributes.getKeyValuePair(i);
      buf.append(" ");
      buf.append(k.key);
      buf.append("=\"");
      XMLConverter.simpleConvertStringToXMLString(k.value.toString(), buf);
      buf.append('\"');
    }
    return buf.toString();
  }

  /**
   * Helper method for toXML(StringBuffer).
   * 
   * @pre rval != null
   * @pre offset >= 0
   */
  protected void toXML(StringBuffer rval, int offset) {
    KeyValuePair keyVal = null;
    int total = getKeyValuePairCount();
    for (int i = 0; i < total; i++) {
      keyVal = getKeyValuePair(i);
      for (int j = 0; j <= offset; j++) {
        rval.append(XML_INDENT);
      }
      boolean pcdata = TransportConstants.PCDATA_KEY.equals(keyVal.key);
      if (!pcdata) {
        rval.append('<').append(keyVal.key).append(attributeString(keyVal.value));
        if (keyVal.value instanceof Frame && ((Frame) keyVal.value).getKeyValuePairCount() == 0) {
          rval.append("/>\n");
          continue;
        } else {
          rval.append('>');
        }
      }
      if (keyVal.value instanceof FrameLeaf) {
        XMLConverter.convertStringToXMLString(((FrameLeaf) keyVal.value).toString(), rval);
      } else {
        rval.append('\n');
        ((Frame) keyVal.value).toXML(rval, offset + 1);
        for (int j = 0; j <= offset; j++) {
          rval.append(XML_INDENT);
        }
      }
      if (!pcdata) {
        rval.append("</").append(keyVal.key).append('>');
      }
      rval.append('\n');
    }
  }

  /**
   * Decorator method for adding float-valued tags.
   * 
   * @param key
   *          The key to be associated with the value.
   * @return This frame
   * @throws UnsupportedOperationException
   *           if this document model doesn't support key addition.
   * 
   * @pre key != null
   */
  public Frame fadd(String key, float val) {
    add(key, new FrameLeaf(val));
    return this;
  }

  /**
   * Decorator method for adding float-array valued tags.
   * 
   * @param key
   *          The key to be associated with the value.
   * @return This frame
   * @throws UnsupportedOperationException
   *           if this document model doesn't support key addition.
   * 
   * @pre key != null
   */
  public Frame fadd(String key, float[] val) {
    if (val != null) {
      add(key, new FrameLeaf(val));
    }
    return this;
  }

  /**
   * Decorator method for adding double valued tags.
   * 
   * @param key
   *          The key to be associated with the value.
   * @throws UnsupportedOperationException
   *           if this document model doesn't support key addition.
   * @return This frame
   * 
   * @pre key != null
   */
  public Frame fadd(String key, double val) {
    add(key, new FrameLeaf(val));
    return this;
  }

  /**
   * Decorator method for adding double-array valued tags. Adding a null array results in a no-op.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The array to add. The array is immediately converted to string representation.
   * @return This frame
   * @throws UnsupportedOperationException
   *           if this document model doesn't support key addition.
   * 
   * @pre key != null
   */
  public Frame fadd(String key, double[] val) {
    if (val != null) {
      add(key, new FrameLeaf(val));
    }
    return this;
  }

  /**
   * Decorator method for adding int valued tags.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The int to add.
   * @return This frame
   * @throws UnsupportedOperationException
   *           if this document model doesn't support key addition.
   * 
   * @pre key != null
   */
  public Frame fadd(String key, int val) {
    add(key, new FrameLeaf(val));
    return this;
  }

  /**
   * Decorator method for adding int-array valued tags. Adding a null value results in a no-op.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The array to add. The array is immediately converted to string representation.
   * @return This frame
   * @throws UnsupportedOperationException
   *           if this document model doesn't support key addition.
   * 
   * @pre key != null
   */
  public Frame fadd(String key, int[] val) {
    if (val != null) {
      add(key, new FrameLeaf(val));
    }
    return this;
  }

  /**
   * Decorator method for adding long valued tags.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The long value to add.
   * @return This frame.
   * @throws UnsupportedOperationException
   *           if this document model doesn't support key addition.
   * 
   * @pre key != null
   */
  public Frame fadd(String key, long val) {
    add(key, new FrameLeaf(val));
    return this;
  }

  /**
   * Decorator method for adding long-array valued tags. Adding a null value results in a no-op.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The array to add. The array is immediately converted to string representation.
   * @return This frame.
   * @throws UnsupportedOperationException
   *           if this document model doesn't support key addition.
   * 
   * @pre key != null
   */
  public Frame fadd(String key, long[] val) {
    if (val != null) {
      add(key, new FrameLeaf(val));
    }
    return this;
  }

  /**
   * Decorator method for adding String valued tags.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The string to add.
   * @return This frame.
   * @throws UnsupportedOperationException
   *           if this document model doesn't support key addition.
   * 
   * @pre key != null
   */
  public Frame fadd(String key, String val) {
    if (val != null) {
      add(key, new FrameLeaf(val));
    }
    return this;
  }

  /**
   * Decorator method for adding String-array valued tags. Adding a null value results in a no-op.
   * 
   * This implementation will use the '#' char as the string separator. If the '#' char is used in
   * some string, then the implementation generates a separator that is not contained by any string
   * and prepends the separator to the string for extraction. The generated separator always will
   * begin and end with the character '#'. This allows arbitrary string arrays to be sent as a
   * single string without separator conflicts.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The string to add.
   * @throws UnsupportedOperationException
   *           if this document model doesn't support key addition.
   * @return This frame.
   * 
   * @pre key != null
   * @pre { for (int i = 0; i < val.length; i++) $assert(val[i] != null, "array elements are
   *      non-null"); }
   */
  public Frame fadd(String key, String[] val) {
    if (val != null) {
      add(key, new FrameLeaf(val));
    }
    return this;
  }

  /**
   * Decorator method for adding binary valued tags. Encodes the data in Base64. Adding a null value
   * results in a no-op.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The binary data to add (will be Base64 encoded).
   * @return This frame.
   * @throws UnsupportedOperationException
   *           if this document model doesn't support key addition.
   * 
   * @pre key != null
   */
  public Frame fadd(String key, byte[] val) {
    if (val != null) {
      add(key, new FrameLeaf(val, true));
    }
    return this;
  }

  /**
   * Decorator method for adding boolean valued tags.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The boolean value to add.
   * @return This frame.
   * @throws UnsupportedOperationException
   *           if this document model doesn't support key addition.
   * 
   * @pre key != null
   */
  public Frame fadd(String key, boolean val) {
    add(key, new FrameLeaf(val));
    return this;
  }

  /**
   * Decorator method for adding Frame-valued tags. Adding a null value results in a no-op.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The sub-frame to add. Note this frame is not copied.
   * @return This frame.
   * @throws UnsupportedOperationException
   *           if this document model doesn't support key addition.
   * 
   * @pre key != null
   */
  public Frame fadd(String key, Frame val) {
    if (val != null) {
      add(key, val);
    }
    return this;
  }

  /**
   * Decorator method for adding a valueless tag.
   * 
   * @param key
   *          The key name.
   * @return This frame.
   * @throws UnsupportedOperationException
   *           if this document model doesn't support key addition, or creation of empty sub-frames.
   * 
   * @pre key != null
   */
  public Frame fadd(String key) {
    add(key, createSubFrame(key, 0));
    return this;
  }

  /**
   * This is a hack method which allows you to add binary-valued tags to Frames in a manner such
   * that there is no textual encoding overhead of that binary data. This is NOT necessarily
   * XTalk-1.0 compatible which formally requires only UTF-8, but it still works. Binary data added
   * using this method can be retrieved using QueryableFrame/VinciFrame getter method
   * fgetTrueBinary(String). Adding a null value results in a no-op.
   * 
   * WARNING: if the default XTalk parser is replaced with another one, applications that depend on
   * this method may break!
   * 
   * WARNING #2: This method should only be used when performance hit of Base64 encoding binary data
   * [as performed by fadd(String, byte[])] is unacceptable.
   * 
   * WARNING #3: The byte array is NOT copied, thus it is up to the caller to ensure that the byte
   * array cannot be modified by external code after passed in to this object.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The byte array to be added to the frame. Note the array is NOT copied or converted in
   *          any way.
   * @return This frame.
   * @throws UnsupportedOperationException
   *           if this document model doesn't support key addition.
   * 
   * @pre key != null
   */
  public Frame faddTrueBinary(String key, byte[] val) {
    if (val != null) {
      add(key, new FrameLeaf(val, false));
    }
    return this;
  }
}
