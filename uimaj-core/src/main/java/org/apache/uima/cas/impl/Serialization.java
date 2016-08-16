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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Marker;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.impl.BinaryCasSerDes6.ReuseInfo;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * This class has no fields or instance methods, but instead 
 * has only static methods.
 * 
 * In spite of its name, it has static methods for both serializing and deserializing CASes.
 * 
 * It has 2 styles of Serialization / Deserialization 
 *   - one which makes use of various custom binary serialization methods, and
 *   - one which just converts CAS and related objects into other objects which
 *     in turn are serializable by normal Java Object serialization.
 * 
 * See also CasIOUtils, which has static methods for serialization and deserialization, including 
 * support for XMI and XCAS.
 *    
 */
public class Serialization {

  /***************************************************************
   * These methods convert a CAS to / from a serializable object * 
   * and vice-versa.                                             *
   * Some also handle type system and index definitions          *
   ***************************************************************/

  /**
   * Convert a CAS to a CASSerializer object.
   * This object used in testing , and also to pass things via the CPP JNI interface, and the Vinci protocol
   * @param cas the CAS which serves as the source for a new CASSerializer object
   * @return a corresponding CASSerializer object
   */
  public static CASSerializer serializeCAS(CAS cas) {
    CASSerializer ser = new CASSerializer();
    ser.addCAS((CASImpl) cas);
    return ser;
  }

  /**
   * Convert a CAS to a CASSerializer object.
   * This object used in testing
   * Excludes metadata about the CAS
   * @param cas the source for a new CASSerializer object
   * @return a corresponding CASSerializer object
   */

  public static CASSerializer serializeNoMetaData(CAS cas) {
    CASSerializer ser = new CASSerializer();
    ser.addNoMetaData((CASImpl) cas);
    return ser;
  }

  /**
   * Convert a Type System and Index Repository into a 
   * CASMgrSerializer object which can be serialized
   * 
   * @param casMgr the type system and index repo definitions
   * @return a serializable object version of these
   */
  public static CASMgrSerializer serializeCASMgr(CASMgr casMgr) {
    CASMgrSerializer ser = new CASMgrSerializer();
    ser.addTypeSystem((TypeSystemImpl) casMgr.getCAS().getTypeSystem());
    ser.addIndexRepository((FSIndexRepositoryImpl) ((CASImpl) casMgr.getCAS())
            .getBaseIndexRepository());
    return ser;
  }
  
  /**
   * Convert a Type System into a 
   * CASMgrSerializer object which can be serialized
   * 
   * @param casMgr the type system and index repo definitions
   * @return a serializable object version of these
   */
  public static CASMgrSerializer serializeCASMgrTypeSystemOnly(CASMgr casMgr) {
    CASMgrSerializer ser = new CASMgrSerializer();
    ser.addTypeSystem((TypeSystemImpl) casMgr.getCAS().getTypeSystem());
    return ser;
  }

  /**
   * Convert a CAS + the type system and index definitions into a
   * CASCompleteSerializer object
   * @param casMgr the source for a new CASCompleteSerializer object
   * @return a Java Object which is serializable and has both the type system, index definitions, and the CAS contents
   */
  public static CASCompleteSerializer serializeCASComplete(CASMgr casMgr) {
    return new CASCompleteSerializer((CASImpl) casMgr);
  }

  /**
   * Deserialize the data in a CASCompleteSerializer into an 
   * existing CAS
   * @param casCompSer the source for deserialization 
   * @param casMgr the CAS to receive the data
   */
  public static void deserializeCASComplete(CASCompleteSerializer casCompSer, CASMgr casMgr) {
    ((CASImpl) casMgr).reinit(casCompSer);
  }

  /**
   * Deserialize a type system and index repository definition and use to initialize
   * a new instance of a CAS.
   * @param ser the CAS to receive the type system
   * @return the initialized CAS loaded with the deserialized info about the CAS Type systen and Index repositories
   */
  public static CASMgr createCASMgr(CASMgrSerializer ser) {
    return new CASImpl(ser);
  }

  // public static CASMgr createCASMgr(CASMgrSerializer ser) {
  // return new CASImpl(ser);
  // }

  /**
   * Deserialize the data in a CASSerializer into an existing CAS,
   * return the currentview in that Cas.
   * @param casMgr the CAS Manager
   * @param casSer the serializer
   * @return the initialized CAS loaded with the deserialized data
   */
  public static CAS createCAS(CASMgr casMgr, CASSerializer casSer) {
    ((CASImpl) casMgr).reinit(casSer);
    return ((CASImpl) casMgr).getCurrentView();
  }

  
  /*******************************************************************************
   * Methods from here on do some form of custom serialization / deserialization *
   * with data streams, byte arrays, etc.                                        *   
   *******************************************************************************/
  
  /**
   * Serialize a CAS including what's indexed, to an output stream
   * Uses uncompressed binary serialization
   * @param cas the CAS to serialize
   * @param ostream the output stream
   */
  public static void serializeCAS(CAS cas, OutputStream ostream) {
    CASSerializer ser = new CASSerializer();
    ser.addCAS((CASImpl) cas, ostream);
  }

  /**
   * Deserialize a CAS, in various binary formats, into an existing CAS
   *   Note: this form supports deserializing the following binary representations:
   *     - plain (uncompressed)
   *     - compressed, no type filtering (form 4), Delta and not-delta
   *     - compressed, no type filtering (form 6), not-delta only.
   *     If this form encounters a non-conforming kind of input, it will throw a runtime exception.  
   * @param cas the CAS to deserialize into.  If the incoming representation is a Delta Cas, then the receiving CAS is not reset, but is added to.
   * @param istream the input stream
   * @return The form of the serialized CAS (from its header)
   */
  public static SerialFormat deserializeCAS(CAS cas, InputStream istream) {
    return ((CASImpl) cas).reinit(istream);
  }

  /**
   * Serializes CAS data added or modified after the tracking Marker was created and writes it
   * to the output stream in Delta CAS format
   * using uncompressed binary format
   * @param cas the Cas to serialize
   * @param ostream the output stream
   * @param mark the cas mark (for delta CASes)
   */
  public static void serializeCAS(CAS cas, OutputStream ostream, Marker mark) {
  	if (!mark.isValid() ) {
  	  throw new CASRuntimeException(CASRuntimeException.INVALID_MARKER);
  	}
  	CASSerializer ser = new CASSerializer();
  	ser.addCAS((CASImpl) cas, ostream, mark);
  }

  /*******************************************************************************
   * Methods from here on use some form of compression                           *
   *******************************************************************************/
  
  /**
   * Serialize in compressed binary form 4
   * @param cas the CAS to serialize
   * @param out - an OutputStream, a DataOutputStream, or a File
   * @throws IOException if IO exception
   */
  public static void serializeWithCompression(CAS cas, Object out) throws IOException {
    (new BinaryCasSerDes4(((CASImpl)cas).getTypeSystemImpl(), false)).serialize(cas, out);
  }
  
  /**
   * Serialize in compress binary form 4, only the delta part of a CAS
   * @param cas the CAS to serialize
   * @param out - an OutputStream, a DataOutputStream, or a File
   * @param marker identifying where the delta starts
   * @throws IOException if IO exception
   */  
  public static void serializeWithCompression(CAS cas, Object out, Marker marker) throws IOException {
    (new BinaryCasSerDes4(((CASImpl)cas).getTypeSystemImpl(), false)).serialize(cas, out, marker);
  }
  
  /**
   * Serialize in compressed binary with type filtering
   * This method can use type filtering to omit sending those types and/or features not present in the target type system.
   *   - To omit type filtering, use null for the target type system
   * It also only sends those feature structures which are reachable either from an index or references from other reachable feature structures.
   * 
   * @param cas the CAS to serialize
   * @param out an OutputStream, a DataOutputStream, or a File
   * @param includeTS true to serialize the type system
   * @param includeTSI true to serialize the type system and the indexes definition
   * @return information to be used on subsequent serializations (to save time) or deserializations (for receiving delta CASs), or reserializations (if sending delta CASs)
   * @throws IOException if IO exception
   * @throws ResourceInitializationException if target type system is incompatible with this CAS's type system
   */  
  public static ReuseInfo serializeWithCompression(CAS cas, Object out, boolean includeTS, boolean includeTSI) throws IOException, ResourceInitializationException {
    BinaryCasSerDes6 bcs = new BinaryCasSerDes6(cas, null, includeTS, includeTSI);
    bcs.serialize(out);
    return bcs.getReuseInfo();
  }
  
  /**
   * Serialize in compressed binary with type filtering
   * This method can use type filtering to omit sending those types and/or features not present in the target type system.
   *   - To omit type filtering, use null for the target type system
   * It also only sends those feature structures which are reachable either from an index or references from other reachable feature structures.
   * 
   * @param cas the CAS to serialize
   * @param out an OutputStream, a DataOutputStream, or a File
   * @param tgtTypeSystem null or a target TypeSystem, which must be mergable with this CAS's type system
   * @return information to be used on subsequent serializations (to save time) or deserializations (for receiving delta CASs), or reserializations (if sending delta CASs)
   * @throws IOException if IO exception
   * @throws ResourceInitializationException if target type system is incompatible with this CAS's type system
   */  
  public static ReuseInfo serializeWithCompression(CAS cas, Object out, TypeSystem tgtTypeSystem) throws IOException, ResourceInitializationException {
    BinaryCasSerDes6 bcs = new BinaryCasSerDes6(cas, (TypeSystemImpl) tgtTypeSystem);
    bcs.serialize(out);
    return bcs.getReuseInfo();
  }
  
  /**
   * Delta Serialize in compressed form, with type filtering
   * This method can use type filtering to omit sending those types and/or features not present in the target type system.
   *   - To omit type filtering, use null for the target type system
   * It also only sends those feature structures which are reachable either from an index or references from other reachable feature structures.
   *
   * @param cas the CAS to serialize
   * @param out an OutputStream, a DataOutputStream, or a File
   * @param tgtTypeSystem null or a target TypeSystem, which must be mergable with this CAS's type system
   * @param mark null or where the mark is in the CAS. If not null, indicates doing a delta CAS serialization
   * @param reuseInfo if mark is not null, this parameter is required 
   *                  and must have been computed when the original deserialization (of the CAS now being serialized as a delta CAS) was done
   * @throws IOException if IO exception
   * @throws ResourceInitializationException if the target type system and the CAS's type system can't be merged
   */
  public static void serializeWithCompression(CAS cas, Object out, TypeSystem tgtTypeSystem, Marker mark, ReuseInfo reuseInfo) throws IOException, ResourceInitializationException {
    BinaryCasSerDes6 bcs = new BinaryCasSerDes6(cas, (MarkerImpl) mark, (TypeSystemImpl) tgtTypeSystem, reuseInfo);
    bcs.serialize(out);
  }  
  
  /**
   * Deserialize a CAS, in various binary formats, into an existing CAS
   *   Note: this form supports deserializing the following binary representations:
   *     - compressed, type filtering (form 6), delta and not-delta.
   *   
   * @param cas the CAS to deserialize into.  If the incoming representation is a Delta Cas, then the receiving CAS is not reset, but is added to.
   * @param istream the input stream
   * @param tgtTypeSystem The typeSystem of the serialized form of the CAS; must be compatible with the type system of the receiving cas.
   * @param reuseInfo If delta CAS is being received and form 6 compression is being used, then this must be the reuseInfo captured when the
   *                  original CAS (being updated by the delta coming in) was sent out.
   * @return The instance of BinaryCasSerDes6 used for deserialization
   * @throws IOException if IO exception
   * @throws ResourceInitializationException if the target type system and the CAS's type system can't be merged
   */
  public static BinaryCasSerDes6 deserializeCAS(CAS cas, InputStream istream, TypeSystem tgtTypeSystem, ReuseInfo reuseInfo) throws IOException, ResourceInitializationException {
    BinaryCasSerDes6 bcs = new BinaryCasSerDes6(cas, null, (TypeSystemImpl) tgtTypeSystem, reuseInfo);
    bcs.deserialize(istream);
    return bcs;
  }
  
  /**
   * Deserialize a CAS, in various binary formats, into an existing CAS
   *   Note: this form supports deserializing the following binary representations:
   *     - compressed, type filtering (form 6), delta and not-delta.
   *   
   * @param cas the CAS to deserialize into.  If the incoming representation is a Delta Cas, then the receiving CAS is not reset, but is added to.
   * @param istream the input stream 
   * @param tgtTypeSystem The typeSystem of the serialized form of the CAS; must be compatible with the type system of the receiving cas.
   * @param reuseInfo If delta CAS is being received and form 6 compression is being used, then this must be the reuseInfo captured when the
   *                  original CAS (being updated by the delta coming in) was sent out.
   * @param allowPreexisting used to control what happens when a delta cas is modifying Feature Structures below the line
   * @return The instance of BinaryCasSerDes6 used for deserialization
   * @throws IOException if IO exception
   * @throws ResourceInitializationException if the target type system and the CAS's type system can't be merged
   */

  public static BinaryCasSerDes6 deserializeCAS(CAS cas, InputStream istream, TypeSystem tgtTypeSystem, ReuseInfo reuseInfo, AllowPreexistingFS allowPreexisting) throws IOException, ResourceInitializationException {
    BinaryCasSerDes6 bcs = new BinaryCasSerDes6(cas, null, (TypeSystemImpl) tgtTypeSystem, reuseInfo);
    bcs.deserialize(istream, allowPreexisting);
    return bcs;
  }

}
