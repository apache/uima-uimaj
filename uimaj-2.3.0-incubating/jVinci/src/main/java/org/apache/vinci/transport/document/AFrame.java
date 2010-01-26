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

import java.io.IOException;

import org.apache.vinci.transport.Attributes;
import org.apache.vinci.transport.Frame;
import org.apache.vinci.transport.FrameComponent;
import org.apache.vinci.transport.FrameLeaf;
import org.apache.vinci.transport.ServiceDownException;
import org.apache.vinci.transport.ServiceException;
import org.apache.vinci.transport.Transportable;
import org.apache.vinci.transport.TransportableFactory;
import org.apache.vinci.transport.VNSException;
import org.apache.vinci.transport.VinciClient;
import org.apache.vinci.transport.VinciFrame;
import org.apache.vinci.transport.util.TransportableConverter;

/**
 * This class is a VinciFrame with extensions for support of XML attributes.
 * 
 * This class provides a set of "aadd" decorator methods that are almost exactly the same as the
 * "fadd" methods of the Frame class, except they return an empty set of attributes that can then be
 * populated. See the "main()" method for an example of how to use this class to easily create
 * documents with attributes.
 */

public class AFrame extends VinciFrame {
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

  private static TransportableFactory aFrameFactory = new TransportableFactory() {
    public Transportable makeTransportable() {
      return new AFrame();
    }
  };

  /**
   * Get a TransportableFactory that creates new AFrames.
   */
  static public TransportableFactory getAFrameFactory() {
    return aFrameFactory;
  }

  public AFrame() {
    this(10);
  }

  /**
   * @pre capacity >= 0
   */
  public AFrame(int capacity) {
    super(capacity);
  }

  /**
   * Create an AFrame that is a (deep) copy of the given transportable.
   * 
   * @pre t != null
   */
  public static AFrame toAFrame(Transportable t) {
    return (AFrame) TransportableConverter.convert(t, getAFrameFactory());
  }

  /**
   * Override the createSubFrame to create an AFrame of precise capacity.
   * 
   * @pre tag_name != null
   * @pre initialCapacity >= 0
   */
  public Frame createSubFrame(String tag_name, int initialCapacity) {
    return new AFrame(initialCapacity);
  }

  /**
   * Override the createFrameLeaf to create an AFrameLeaf so that leaf values can have attributes.
   * 
   * @pre array != null
   */
  public FrameLeaf createFrameLeaf(byte[] array) {
    return new AFrameLeaf(array, false);
  }

  /**
   * Convenience method for fetching sub-frames when their type is known to be AFrame
   * 
   * @param key
   *          The key identifying the value to retrieve.
   * @exception ClassCastException
   *              if the value was not of type AFrame.
   * @return The requested value, or null if the specified key does not exist.
   * 
   * @pre key != null
   */
  public AFrame fgetAFrame(String key) {
    return (AFrame) fgetFirst(key);
  }

  /**
   * Get the attributes associated with a particular key. If there is more than one matching key,
   * then the attributes of only the first matching key are returned.
   * 
   * @param key
   *          The key whose attributes to fetch.
   * @return The (possibly empty) set of attributes associated with the key, or null (which
   *         indicates no such key).
   * 
   * @pre key != null
   */
  public Attributes aget(String key) {
    FrameComponent comp = fgetFirst(key);
    if (comp != null) {
      Attributes not_null = comp.getAttributes();
      if (not_null == null) {
        return new Attributes();
      } else {
        return not_null;
      }
    }
    return null;
  }

  /**
   * Decorator method for adding float-valued tags.
   * 
   * @param key
   *          The key to be associated with the value.
   * @return The (empty) set of attributes associated with the added key.
   * 
   * @pre key != null
   */
  public Attributes aadd(String key, float val) {
    AFrameLeaf l = new AFrameLeaf(val);
    add(key, l);
    return l.createAttributes();
  }

  /**
   * Decorator method for adding float-array valued tags.
   * 
   * @param key
   *          The key to be associated with the value.
   * @return The (empty) set of attributes associated with the added key.
   * 
   * @pre key != null
   */
  public Attributes aadd(String key, float[] val) {
    if (val != null) {
      AFrameLeaf l = new AFrameLeaf(val);
      add(key, l);
      return l.createAttributes();
    }
    return null;
  }

  /**
   * Decorator method for adding double valued tags.
   * 
   * @param key
   *          The key to be associated with the value.
   * @return The (empty) set of attributes associated with the added key.
   * 
   * @pre key != null
   */
  public Attributes aadd(String key, double val) {
    AFrameLeaf l = new AFrameLeaf(val);
    add(key, l);
    return l.createAttributes();
  }

  /**
   * Decorator method for adding double-array valued tags.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The array to add. The array is immediately converted to string representation.
   * @return The (empty) set of attributes associated with the added key.
   * 
   * @pre key != null
   */
  public Attributes aadd(String key, double[] val) {
    if (val != null) {
      AFrameLeaf l = new AFrameLeaf(val);
      add(key, l);
      return l.createAttributes();
    }
    return null;
  }

  /**
   * Decorator method for adding int valued tags.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The int to add.
   * @return The (empty) set of attributes associated with the key.
   * 
   * @pre key != null
   */
  public Attributes aadd(String key, int val) {
    AFrameLeaf l = new AFrameLeaf(val);
    add(key, l);
    return l.createAttributes();
  }

  /**
   * Decorator method for adding int-array valued tags.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The array to add. The array is immediately converted to string representation.
   * @return The set of attributes associated with the key.
   * 
   * @pre key != null
   */
  public Attributes aadd(String key, int[] val) {
    if (val != null) {
      AFrameLeaf l = new AFrameLeaf(val);
      add(key, l);
      return l.createAttributes();
    }
    return null;
  }

  /**
   * Decorator method for adding long valued tags.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The long value to add.
   * @return The (empty) set of attributes associated with the key.
   * 
   * @pre key != null
   */
  public Attributes aadd(String key, long val) {
    AFrameLeaf l = new AFrameLeaf(val);
    add(key, l);
    return l.createAttributes();
  }

  /**
   * Decorator method for adding long-array valued tags.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The array to add. The array is immediately converted to string representation.
   * @return The (empty) set of attributes associated with the key.
   * 
   * @pre key != null
   */
  public Attributes aadd(String key, long[] val) {
    if (val != null) {
      AFrameLeaf l = new AFrameLeaf(val);
      add(key, l);
      return l.createAttributes();
    }
    return null;
  }

  /**
   * Decorator method for adding String valued tags.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The string to add.
   * @return The (empty) set of attributes associated with the added key.
   * 
   * @pre key != null
   */
  public Attributes aadd(String key, String val) {
    if (val != null) {
      AFrameLeaf l = new AFrameLeaf(val);
      add(key, l);
      return l.createAttributes();
    }
    return null;
  }

  /**
   * Decorator method for adding binary valued tags. Encodes the data in Base64.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The data to be encoded and added to this frame.
   * @return The (empty) set of attributes associated with the added key.
   * 
   * @pre key != null
   */
  public Attributes aadd(String key, byte[] val) {
    if (val != null) {
      AFrameLeaf l = new AFrameLeaf(val, true);
      add(key, l);
      return l.createAttributes();
    }
    return null;
  }

  /**
   * Decorator method for adding boolean valued tags.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The boolean value to add.
   * @return The (empty) set of attributes associated with the added key.
   * 
   * @pre key != null
   */
  public Attributes aadd(String key, boolean val) {
    AFrameLeaf l = new AFrameLeaf(val);
    add(key, l);
    return l.createAttributes();
  }

  /**
   * Decorator method for adding Frame-valued tags.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The sub-frame to add. Note this frame is not copied.
   * @return The (empty) set of attributes associated with the added key.
   * 
   * @pre key != null
   */
  public Attributes aadd(String key, AFrame val) {
    if (val != null) {
      add(key, val);
      return val.createAttributes();
    }
    return null;
  }

  /**
   * Decorator method for adding a valueless tag.
   * 
   * @param key
   *          The key name.
   * @return The (empty) set of attributes associated with the added key.
   * 
   * @pre key != null
   */
  public Attributes aadd(String key) {
    AFrame sub = new AFrame(0);
    add(key, sub);
    return sub.createAttributes();
  }

  /**
   * This is a hack method which allows you to add binary-valued tags to Frames in a manner such
   * that there is no textual encoding overhead of that binary data. This is NOT necessarily
   * XTalk-1.0 compatible which formally requires only UTF-8, but it still works. Binary data added
   * using this method can be retrieved using QueryableFrame/VinciFrame getter method
   * fgetTrueBinary(String).
   * 
   * WARNING: if the default XTalk parser is replaced with another one, applications that depend on
   * this method may break!
   * 
   * NOTE: This method should only be used when performance hit of Base64 encoding binary data [as
   * performed by aadd(String, byte[])] is unacceptable.
   * 
   * @param key
   *          The key to be associated with the value.
   * @param val
   *          The byte array to be added to the frame. Note the array is NOT copied or converted in
   *          any way.
   * @return The (empty) set of attributes associated with the added key.
   * 
   * @pre key != null
   */
  public Attributes aaddTrueBinary(String key, byte[] val) {
    if (val != null) {
      AFrameLeaf l = new AFrameLeaf(val, false);
      add(key, l);
      return l.createAttributes();
    }
    return null;
  }

  /**
   * @pre in != null
   * @pre service_name != null
   * 
   * @throws IllegalStateException
   *           if VNS_HOST is not specified.
   */
  static public AFrame rpc(Transportable in, String service_name) throws IOException,
          ServiceException, ServiceDownException, VNSException {
    return (AFrame) VinciClient.sendAndReceive(in, service_name, getAFrameFactory());
  }

  /**
   * @pre in != null
   * @pre service_name != null
   * @pre socket_timeout >= 0
   * 
   * @throws IllegalStateException
   *           if VNS_HOST is not specified.
   */
  static public AFrame rpc(Transportable in, String service_name, int socket_timeout)
          throws IOException, ServiceException, ServiceDownException, VNSException {
    return (AFrame) VinciClient
            .sendAndReceive(in, service_name, getAFrameFactory(), socket_timeout);
  }

  /**
   * @pre in != null
   * @pre service_name != null
   * @pre socket_timeout >= 0
   * 
   * WARNING: This method relies on JDK-1.4 specific functions. USE IT ONLY if you don't need to
   * maintain JDK1.3 compatability.
   * 
   * @throws IllegalStateException
   *           if VNS_HOST is not specified.
   */
  static public AFrame rpc(Transportable in, String service_name, int socket_timeout,
          int connect_timeout) throws IOException, ServiceException, ServiceDownException,
          VNSException {
    return (AFrame) VinciClient.sendAndReceive(in, service_name, getAFrameFactory(),
            socket_timeout, connect_timeout);
  }

  /*
   * public static void main(String[] args) { // EXAMPLE of creating a document with attributes.
   * AFrame f = new AFrame(); f.setAttributes((Attributes)new Attributes().fadd("foo", "bar")); //
   * Once we create the empty AFrame, we can call "aadd" to add a key/value pair and // retrieve an
   * empty set of attributes that we can then populate with attributes that will // be associated
   * with the just-added key. Note that the attribute list object is itself a // special type of
   * VinciFrame, thus we can use the standard "fadd()" methods to create the // attribute list.
   * f.aadd("KEY", "value1") // Add XML element and retrieve the attribute list object
   * .fadd("attr1", "aval1") // Add an attribute for the tag. .fadd("attr2", "aval2") // Add another
   * attribute for the tag. .fadd("attr3", "you get the point");
   *  // Note that with fadd() you can potentially create a duplicate attribute (an XML no-no), //
   * so the "idiot proof" method is to instead use fset(): f.aadd("VALUELESS_KEY") // Add a
   * valueless tag that has a couple of attributes .fset("foo", "bar") .fset("foo2", 2) .fset("foo",
   * "replaced bar");
   *  // Here's how to create a nested AFrame: AFrame nested = new AFrame();
   * nested.aadd("NESTED_KEY", 1234.567) .fadd("nested", "attribute"); f.aadd("NESTED_FRAME",
   * nested) .fadd("attributename", new int[] {1,2,3});
   *  // We can use either fadd() or aadd() to add attributeless tags, though fadd is // preferrable
   * since it avoids creating the attributes object. nested.fadd("ATTRIBUTELESS_KEY", "done with
   * fadd()"); nested.aadd("ANOTHER_ATTRIBUTELESS_KEY", "done with aadd()");
   * 
   * System.out.println(f.toXML()); System.out.println();
   *  // Querying key values: String foo_value = f.aget("VALUELESS_KEY").fgetString("foo");
   * System.out.println("Attribute value of foo from VALUELESS_KEY: " + foo_value);
   * System.out.println("TEST: " + toAFrame(f).toXML()); }
   */
}
