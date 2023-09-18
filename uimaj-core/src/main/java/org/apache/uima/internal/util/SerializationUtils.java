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

package org.apache.uima.internal.util;

import static java.io.ObjectInputFilter.allowFilter;
import static java.io.ObjectInputFilter.Status.UNDECIDED;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Set;

import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.impl.CASSerializer;

/**
 * Serialize and Deserialize arbitrary objects to/from byte arrays, using standard Java object
 * serialization/deserialization support.
 * 
 * Used in the Vinci transport to serialize/deserialize CASSerializer objects or
 * CASCompleteSerializer objects (includes type system and index definitions).
 * 
 * This class is abstract only to prevent instantiation. All the methods are static.
 */
public final class SerializationUtils {

  private static final Set<Class<?>> CAS_MGR_SERIALIZER_SAFE_CLASSES = Set.of( //
          CASMgrSerializer.class, //
          String.class);
  private static final Set<Class<?>> CAS_SERIALIZER_SAFE_CLASSES = Set.of( //
          CASSerializer.class, //
          String.class);
  private static final Set<Class<?>> CAS_COMPLETE_SERIALIZER_SAFE_CLASSES = Set.of( //
          CASCompleteSerializer.class, //
          String.class, //
          CASMgrSerializer.class, //
          CASSerializer.class);

  private SerializationUtils() {
    // No instances
  }

  /**
   * Serializes an object to a byte array.
   * 
   * @param aObject
   *          object to serialize
   * 
   * @return <code>aObject</code> encoded as a byte array. If <code>aObject</code> is
   *         <code>null</code>, <code>null</code> is returned.
   * 
   * @throws IOException
   *           if an I/O error occurs
   */
  public static byte[] serialize(Serializable aObject) throws IOException {
    if (aObject == null) {
      return null;
    }

    try (var byteStream = new ByteArrayOutputStream();
            var objStream = new ObjectOutputStream(byteStream)) {
      objStream.writeObject(aObject);
      objStream.flush();
      return byteStream.toByteArray();
    }
  }

  /**
   * Deserializes a supported object from a byte array. Supported objects are
   * {@link CASMgrSerializer}, {@link CASSerializer}, {@link CASCompleteSerializer}.
   * 
   * @param aBytes
   *          byte array to read from
   * 
   * @return The <code>Object</code> deserialized from <code>aBytes</code>. If <code>aBytes</code>
   *         is <code>null</code>, <code>null</code> is returned.
   * 
   * @throws IOException
   *           if an I/O error occurs
   * @throws ClassNotFoundException
   *           if a required class could not be found
   */
  public static Object deserialize(byte[] aBytes) throws IOException, ClassNotFoundException {

    Object object;

    try {
      object = deserialize(aBytes,
              concat(concat(CAS_SERIALIZER_SAFE_CLASSES.stream(),
                      CAS_COMPLETE_SERIALIZER_SAFE_CLASSES.stream()),
                      CAS_MGR_SERIALIZER_SAFE_CLASSES.stream()).collect(toSet()));
    } catch (IOException e) {
      if (e.getCause() instanceof ClassNotFoundException) {
        throw (ClassNotFoundException) e.getCause();
      }

      throw e;
    }

    if (object != null && !(object instanceof CASMgrSerializer || object instanceof CASSerializer
            || object instanceof CASCompleteSerializer)) {
      throw new IOException("Unexpected object type: [" + object.getClass().getName() + "]");
    }

    return object;
  }

  /**
   * Deserializes a {@link CASSerializer} or {@link CASCompleteSerializer} from a byte array.
   * 
   * @param aIs
   *          stream to read from
   * 
   * @return The <code>Object</code> deserialized from <code>aBytes</code>. If <code>aBytes</code>
   *         is <code>null</code>, <code>null</code> is returned.
   * 
   * @throws IOException
   *           if an I/O error occurs
   */
  public static Object deserializeCASSerializerOrCASCompleteSerializer(InputStream aIs)
          throws IOException {
    var object = deserialize(aIs, concat(CAS_SERIALIZER_SAFE_CLASSES.stream(),
            CAS_COMPLETE_SERIALIZER_SAFE_CLASSES.stream()).collect(toSet()));

    if (object != null
            && !(object instanceof CASSerializer || object instanceof CASCompleteSerializer)) {
      throw new IOException("Unexpected object type: [" + object.getClass().getName() + "]");
    }

    return object;
  }

  /**
   * Deserializes a {@link CASCompleteSerializer} from a byte array.
   * 
   * @param aBytes
   *          byte array to read from
   * 
   * @return The <code>Object</code> deserialized from <code>aBytes</code>. If <code>aBytes</code>
   *         is <code>null</code>, <code>null</code> is returned.
   * 
   * @throws IOException
   *           if an I/O error occurs
   */
  public static CASCompleteSerializer deserializeCASCompleteSerializer(byte[] aBytes)
          throws IOException {
    var object = deserialize(aBytes, CAS_COMPLETE_SERIALIZER_SAFE_CLASSES);

    if (object != null && !(object instanceof CASCompleteSerializer)) {
      throw new IOException("Unexpected object type: [" + object.getClass().getName() + "]");
    }

    return (CASCompleteSerializer) object;
  }

  /**
   * Deserializes a {@link CASSerializer} from a byte array.
   * 
   * @param aBytes
   *          byte array to read from
   * 
   * @return The <code>Object</code> deserialized from <code>aBytes</code>. If <code>aBytes</code>
   *         is <code>null</code>, <code>null</code> is returned.
   * 
   * @throws IOException
   *           if an I/O error occurs
   */
  public static CASSerializer deserializeCASSerializer(byte[] aBytes) throws IOException {

    var object = deserialize(aBytes, CAS_SERIALIZER_SAFE_CLASSES);

    if (object != null && !(object instanceof CASSerializer)) {
      throw new IOException("Unexpected object type: [" + object.getClass().getName() + "]");
    }

    return (CASSerializer) object;
  }

  /**
   * Deserializes a {@link CASSerializer} from a byte array.
   * 
   * @param aBytes
   *          byte array to read from
   * 
   * @return The <code>Object</code> deserialized from <code>aBytes</code>. If <code>aBytes</code>
   *         is <code>null</code>, <code>null</code> is returned.
   * 
   * @throws IOException
   *           if an I/O error occurs
   */
  public static CASMgrSerializer deserializeCASMgrSerializer(byte[] aBytes) throws IOException {
    var object = deserialize(aBytes, CAS_MGR_SERIALIZER_SAFE_CLASSES);

    if (object != null && !(object instanceof CASMgrSerializer)) {
      throw new IOException("Unexpected object type: [" + object.getClass().getName() + "]");
    }

    return (CASMgrSerializer) object;
  }

  /**
   * Deserializes a {@link CASMgrSerializer} from an {@link InputStream}.
   * 
   * @param aIn
   *          stream read from
   * 
   * @return The <code>Object</code> deserialized from <code>aBytes</code>. If <code>aBytes</code>
   *         is <code>null</code>, <code>null</code> is returned.
   * 
   * @throws IOException
   *           if an I/O error occurs
   */
  public static CASMgrSerializer deserializeCASMgrSerializer(InputStream aIn) throws IOException {
    var object = deserialize(aIn, CAS_MGR_SERIALIZER_SAFE_CLASSES);

    if (object != null && !(object instanceof CASMgrSerializer)) {
      throw new IOException("Unexpected object type: [" + object.getClass().getName() + "]");
    }

    return (CASMgrSerializer) object;
  }

  private static <T> T deserialize(byte[] aBytes, Set<Class<?>> aFilter) throws IOException {
    if (aBytes == null) {
      return null;
    }

    try (var is = new ByteArrayInputStream(aBytes)) {
      return deserialize(is, aFilter);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T deserialize(InputStream aIs, Set<Class<?>> aSafeClasses) throws IOException {
    var ois = new ObjectInputStream(aIs);
    var f = ObjectInputFilter.rejectUndecidedClass(allowFilter(aSafeClasses::contains, UNDECIDED));
    ois.setObjectInputFilter(f);
    try {
      return (T) ois.readObject();
    } catch (ClassNotFoundException e) {
      throw new IOException("Unexpected deserialization error", e);
    }
  }
}
