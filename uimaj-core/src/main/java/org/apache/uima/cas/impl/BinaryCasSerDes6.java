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

import static org.apache.uima.cas.impl.SlotKinds.SlotKind.NBR_SLOT_KIND_ZIP_STREAMS;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_ArrayLength;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_BooleanRef;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Byte;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_ByteRef;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Control;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_DoubleRef;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Double_Exponent;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Double_Mantissa_Sign;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Float_Exponent;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Float_Mantissa_Sign;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_FsIndexes;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_HeapRef;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Int;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_LongRef;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Long_High;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Long_Low;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_MainHeap;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Short;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_ShortRef;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_StrChars;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_StrLength;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_StrOffset;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_StrRef;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_StrSeg;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_TypeCode;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.impl.CommonSerDes.Header;
import org.apache.uima.cas.impl.FSsTobeAddedback.FSsTobeAddedbackSingle;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;
import org.apache.uima.cas.impl.TypeSystemImpl.TypeInfo;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.rb_trees.Int2IntRBT;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.CasLoadMode;
import org.apache.uima.util.impl.DataIO;
import org.apache.uima.util.impl.OptimizeStrings;
import org.apache.uima.util.impl.SerializationMeasures;

/**
 * User callable serialization and deserialization of the CAS in a compressed Binary Format
 * 
 * This serializes/deserializes the state of the CAS.  It has the capability to map type systems,
 * so the sending and receiving type systems do not have to be the same.
 *   - types and features are matched by name, and features must have the same range (slot kind)
 *   - types and/or features in one type system not in the other are skipped over
 * 
 * Header specifies to reader the format, and the compression level.
 * 
 * How to Serialize:  
 * 
 * 1) create an instance of this class
 *    a) if doing a delta serialization, pass in the mark and a ReuseInfo object that was created
 *       after deserializing this CAS initially.
 *    b) if serializaing to a target with a different type system, pass the target's type system impl object
 *       so the serialization can filter the types for the target.
 * 2) call serialize() to serialize the CAS
 * 3) If doing serialization to a target from which you expect to receive back a delta CAS,
 *    create a ReuseInfo object from this object and reuse it for deserializing the delta CAS.
 *    
 * TypeSystemImpl objects are lazily augmented by customized TypeInfo instances for each type encountered in 
 * serializing or deserializing.  These are preserved for future calls, so their setup / initialization is only
 * needed the first time.
 * 
 * TypeSystemImpl objects are also lazily augmented by typeMappers for individual different target typesystems;
 * these too are preserved and reused on future calls.
 * 
 * Compressed Binary CASes are designed to be "self-describing" - 
 * The format of the compressed binary CAS, including version info, 
 * is inserted at the beginning so that a proper deserialization method can be automatically chosen.  
 *     
 * Compressed Binary format implemented by this class supports type system mapping.  
 * Types in the source which are not in the target
 * (or vice versa) are omitted.  
 *   Types with "extra" features have their extra features omitted 
 *   (or on deserialization, they are set to their default value - null, or 0, etc.).
 *   Feature slots which hold references to types not in the target type system are replaced with 0 (null).  
 * 
 * How to Deserialize:
 * 
 * 1) get an appropriate CAS to deserialize into.  For delta CAS, it does not have to be empty, but it must
 *    be the originating CAS from which the delta was produced.
 * 2) If the case is one where the target type system == the CAS's, and the serialized for is not Delta,
 *    then, call aCAS.reinit(source).  Otherwise, create an instance of this class -%gt; xxx
 *    a) Assuming the object being deserialized has a different type system, 
 *       set the "target" type system to the TypeSystemImpl instance of the 
 *       object being deserialized.    
 *    a) if delta deserializing, pass in the ReuseInfo object created when the CAS was serialized 
 * 3) call xxx.deserialize(inputStream)
 * 
 * Compression/Decompression
 * Works in two stages:
 *   application of Zip/Unzip to particular sub-collections of CAS data, 
 *     grouped according to similar data distribution
 *   collection of like kinds of data (to make the zipping more effective)
 *   There can be up to ~20 of these collections, such as
 *      control info, float-exponents, string chars
 * Deserialization:
 *   Read all bytes, 
 *   create separate ByteArrayInputStreams for each segment
 *   create appropriate unzip data input streams for these
 *   
 *   Slow but expensive data: 
 *     extra type system info - lazily created and added to shared TypeSystemImpl object
 *       set up per type actually referenced
 *     mapper for type system - lazily created and added to shared TypeSystemImpl object
 *       in identity-map cache (size limit = 10 per source type system?) - key is target typesystemimpl.
 *   Defaulting:
 *     flags:  doMeasurements, compressLevel, CompressStrategy
 *   Per serialize call: cas, output, [target ts], [mark for delta]
 *   Per deserialize call: cas, input, [target ts], whether-to-save-info-for-delta-serialization
 *   
 *   CASImpl has instance method with defaulting args for serialization.
 *   CASImpl has reinit which works with compressed binary serialization objects
 *     if no type mapping
 *     If type mapping, (new BinaryCasSerDes6(cas, 
 *                                            marker-or-null, 
 *                                            targetTypeSystem (for stream being deserialized), 
 *                                            reuseInfo-or-null)
 *                                                .deserialize(in-stream)
 *     
 * Use Cases, filtering and delta
 *   **************************************************************************
 *   * (de)serialize * filter? * delta? * Use case
 *   **************************************************************************
 *   * serialize     *   N     *   N    * Saving a Cas, 
 *   *               *         *        * sending Cas to service with identical ts
 *   **************************************************************************
 *   * serialize     *   Y     *   N    * sending Cas to service with 
 *   *               *         *        * different ts (a guaranteed subset)
 *   **************************************************************************
 *   * serialize     *   N     *   Y    * returning Cas to client
 *   *               *         *        *   uses info saved when deserializing
 *   *               *         *        * (?? saving just a delta to disk??)
 *   **************************************************************************
 *   * serialize     *   Y     *   Y    * NOT SUPPORTED (not needed)  
 *   **************************************************************************
 *   * deserialize   *   N     *   N    * reading/(receiving) CAS, identical TS
 *   **************************************************************************
 *   * deserialize   *   Y     *   N    * reading/receiving CAS, different TS
 *   *               *         *        * ts not guaranteed to be superset
 *   *               *         *        * for "reading" case.
 *   **************************************************************************
 *   * deserialize   *   N     *   Y    * receiving CAS, identical TS 
 *   *               *         *        *   uses info saved when serializing
 *   **************************************************************************
 *   * deserialize   *   Y     *   Y    * receiving CAS, different TS (tgt a feature subset)
 *   *               *         *        *   uses info saved when serializing
 *   **************************************************************************
 */
public class BinaryCasSerDes6 {

  private static final int[] INT0 = new int[0];
  
  private static final boolean TRACE_SER = false;
  private static final boolean TRACE_DES = false;
  
  private static final boolean TRACE_STR_ARRAY = false;
  /**
   * Version of the serializer/deserializer, used to allow deserialization of 
   * older versions
   * 
   * Version 0 - initial SVN checkin
   * Version 1 - changes to support CasTypeSystemMapper 
   */
  private static final int VERSION = 1;  
  
  private static final long DBL_1 = Double.doubleToLongBits(1D);

  /**
   * Compression alternatives
   */
  
  public enum CompressLevel {
    None(   Deflater.NO_COMPRESSION),
    Fast(   Deflater.BEST_SPEED),
    Default(Deflater.DEFAULT_COMPRESSION),
    Best(   Deflater.BEST_COMPRESSION),
    ;
    final public int lvl;
    CompressLevel(int lvl) {
      this.lvl = lvl;
    }
  }
  
  public enum CompressStrat {
    Default(      Deflater.DEFAULT_STRATEGY),
    Filtered(     Deflater.FILTERED),
    HuffmanOnly(  Deflater.HUFFMAN_ONLY),
    ;
    final public int strat;
    CompressStrat(int strat) {
      this.strat = strat;
    }
  }
  /**
   * Info reused for 
   *   1) multiple serializations of same cas to multiple targets (a speedup), or
   *   2) for delta cas serialization, where it represents the fsStartIndex info before any mods
   *      were done which could change that info, or 
   *   3) for deserializing with a delta cas, where it represents the fsStartIndex info at the time
   *      the CAS was serialized out..
   * Reachable FSs and Sequence maps
   */
  public static class ReuseInfo {
    /**
     * kept to avoid recomputation in the use case:
     *   - serialize to target 1, serialize same to target 2, etc.
     *   - Delta serialization (uses reuse info saved during initial deserialization)
     *   - Delta deserialization 
     *   if Null, recomputed when needed
     * BitSet used to test if fsRef needs to be serialized   
     */
    final private BitSet foundFSs;
    final private int[] foundFSsArray; // ordered set of FSs found in indexes or linked from other found FSs
    
    /**
     * Multiple uses:
     *   a) avoid recomputation when multiple serializations of same CAS to multiple targets
     *   b) remembers required mapping for processing delta cas serializations and deserializations conversion of tgt seq # to src addr
     */
    final private CasSeqAddrMaps fsStartIndexes;
    
    private ReuseInfo(
        BitSet foundFSs,
        int[] foundFSsArray, 
        CasSeqAddrMaps fsStartIndexes) {
      this.foundFSs = foundFSs;
      this.foundFSsArray = foundFSsArray;
      this.fsStartIndexes = fsStartIndexes;
    }
  }
  
  public ReuseInfo getReuseInfo() {
    return new ReuseInfo(foundFSs, foundFSsArray, fsStartIndexes);
  }
    
  // speedups - ints for SlotKind ordinals
  final private static int arrayLength_i = Slot_ArrayLength.ordinal();
  final private static int heapRef_i = Slot_HeapRef.ordinal();
  final private static int int_i = Slot_Int.ordinal();
  final private static int byte_i = Slot_Byte.ordinal();
  final private static int short_i = Slot_Short.ordinal();
  final private static int typeCode_i = Slot_TypeCode.ordinal();
  final private static int strOffset_i = Slot_StrOffset.ordinal();
  final private static int strLength_i = Slot_StrLength.ordinal();
  final private static int long_High_i = Slot_Long_High.ordinal();
  final private static int long_Low_i = Slot_Long_Low.ordinal();
  final private static int float_Mantissa_Sign_i = Slot_Float_Mantissa_Sign.ordinal();
  final private static int float_Exponent_i = Slot_Float_Exponent.ordinal();
  final private static int double_Mantissa_Sign_i = Slot_Double_Mantissa_Sign.ordinal();
  final private static int double_Exponent_i = Slot_Double_Exponent.ordinal();
  final private static int fsIndexes_i = Slot_FsIndexes.ordinal();
  final private static int strChars_i = Slot_StrChars.ordinal();
  final private static int control_i = Slot_Control.ordinal();
  final private static int strSeg_i = Slot_StrSeg.ordinal();

  /**
   * Things set up for one instance of this class
   */
  private TypeSystemImpl ts;
  final private CompressLevel compressLevel;
  final private CompressStrat compressStrategy;  
  
  /**
   * Things that are used by common routines among serialization and deserialization
   */
  final private boolean isTypeMappingCmn;
  private CasTypeSystemMapper typeMapperCmn;

  /*****************************************************
   *  Things for both serialization and Deserialization
   *****************************************************/
  final private CASImpl cas;  // cas being serialized or deserialized into
  private int[] heap;           // main heap, can't be final because grow replaces it
  final private StringHeap stringHeapObj;
  final private LongHeap longHeapObj;
  final private ShortHeap shortHeapObj;
  final private ByteHeap byteHeapObj;

  private int heapStart;
  private int heapEnd;                 // set when deserializing   
  private int totalMappedHeapSize = 0; // heapEnd - heapStart, but with FS that don't exist in the target type system deleted    

  final private boolean isSerializingDelta;        // if true, there is a marker indicating the start spot(s)
        private boolean isDelta;
        private boolean isReadingDelta;
  final private MarkerImpl mark;  // the mark to serialize from

  
  final private CasSeqAddrMaps fsStartIndexes;
  final private boolean reuseInfoProvided;
  final private boolean doMeasurements;  // if true, doing measurements

  private OptimizeStrings os;
  private boolean only1CommonString;  // true if only one common string

  final private TypeSystemImpl tgtTs;

  private boolean isTsIncluded;   // type system used for the serialization
  private boolean isTsiIncluded;  // types plus index definition, used to reset the cas
  
  private TypeInfo typeInfo; // type info for the current type being serialized/deserialized
                             // always the "src" typeInfo I think, except for compareCas use
  final private CasTypeSystemMapper typeMapper;
  
  /**
   * This is the used version of isTypeMapping, normally == to isTypeMappingCmn
   *   But compareCASes sets this false temporarily while setting up the compare
   */
  private boolean isTypeMapping;

  final private int[] iPrevHeapArray; // index of previous instance of this typecode in heap, by typecode
  private int iPrevHeap;        // 0 or heap addr of previous instance of current type
  /**
   * Hold prev instance of FS which have non-array FSRef slots, to allow 
   * computing these to match case where a 0 value is used because of type filtering
   *   for each typecode, only set if the type has 1 or more non-array fsref
   *   set only for non-filtered domain types
   *     set only for non-0 values
   *       if fsRef is to filtered type, value serialized will be 0, but this slot not set
   *       On deserialization: if value is 0, skip setting 
   */
  final private int[] [] prevHeapInstanceWithIntValues;
  
  private BitSet foundFSs; // ordered set of FSs found in indexes or linked from other found FSs
  private BitSet foundFSsBelowMark; // for delta serialization use only
  private int[] foundFSsArray;  // sorted fss's being serialized.  For delta, just the deltas
  final private IntVector toBeScanned = new IntVector();
//  private HashSetInt ffssBelowMark;  // sorted fss's found below the mark
//  final private int[] typeCodeHisto = new int[ts.getTypeArraySize()]; 

  final private boolean debugEOF = false;
  /********************************* 
   * Things for just serialization  
   *********************************/
  private DataOutputStream serializedOut;  // where to write out the serialized result
  
  final private SerializationMeasures sm;  // null or serialization measurements
  final private ByteArrayOutputStream[] baosZipSources = new ByteArrayOutputStream[NBR_SLOT_KIND_ZIP_STREAMS];  // lazily created, indexed by SlotKind.i
  final private DataOutputStream[] dosZipSources = new DataOutputStream[NBR_SLOT_KIND_ZIP_STREAMS];      // lazily created, indexed by SlotKind.i
  private int[] savedAllIndexesFSs;  // speedup - avoid computing this twice
  
  final private int[] estimatedZipSize = new int[NBR_SLOT_KIND_ZIP_STREAMS]; // one entry for each output stream kind
  // speedups
  
  // any use of these means caller handles measurement
  // some of these are never used, because the current impl
  //   is using the _i form to get measurements done
//  private DataOutputStream arrayLength_dos;
//  private DataOutputStream heapRef_dos;
//  private DataOutputStream int_dos;
  private DataOutputStream byte_dos;
//  private DataOutputStream short_dos;
  private DataOutputStream typeCode_dos;
  private DataOutputStream strOffset_dos;
  private DataOutputStream strLength_dos;
//  private DataOutputStream long_High_dos;
//  private DataOutputStream long_Low_dos;
  private DataOutputStream float_Mantissa_Sign_dos;
  private DataOutputStream float_Exponent_dos;
  private DataOutputStream double_Mantissa_Sign_dos;
  private DataOutputStream double_Exponent_dos;
  private DataOutputStream fsIndexes_dos;
//  private DataOutputStream strChars_dos;
  private DataOutputStream control_dos;
  private DataOutputStream strSeg_dos;

  /********************************** 
   * Things for just deserialization  
   **********************************/

  private AllowPreexistingFS allowPreexistingFS;
  private DataInputStream deserIn;
  private int version;

  final private DataInputStream[] dataInputs = new DataInputStream[NBR_SLOT_KIND_ZIP_STREAMS];
  final private Inflater[] inflaters = new Inflater[NBR_SLOT_KIND_ZIP_STREAMS];

  private IntVector fixupsNeeded;  // for deserialization, the "fixups" for relative heap refs needed  
  private int stringTableOffset;
  
  /**
   * These indexes remember sharable common values in aux heaps
   * Values must be in aux heap, but not part of arrays there
   *   so that rules out boolean, byte, and shorts
   */
  private int longZeroIndex = -1; // also used for double 0 index
  private int double1Index = -1;
  
  private boolean isUpdatePrevOK; // false if shouldn't update prev value because written value was 0

  private String[] readCommonString;

  // speedups
  
  private DataInputStream arrayLength_dis;
  private DataInputStream heapRef_dis;
  private DataInputStream int_dis;
  private DataInputStream byte_dis;
  private DataInputStream short_dis;
  private DataInputStream typeCode_dis;
  private DataInputStream strOffset_dis;
  private DataInputStream strLength_dis;
  private DataInputStream long_High_dis;
  private DataInputStream long_Low_dis;
  private DataInputStream float_Mantissa_Sign_dis;
  private DataInputStream float_Exponent_dis;
  private DataInputStream double_Mantissa_Sign_dis;
  private DataInputStream double_Exponent_dis;
  private DataInputStream fsIndexes_dis;
  private DataInputStream strChars_dis;
  private DataInputStream control_dis;
  private DataInputStream strSeg_dis;


  /**
   * Setup to serialize or deserialize using binary compression, with (optional) type mapping and only processing reachable Feature Structures
   * @param aCas required - refs the CAS being serialized or deserialized into
   * @param mark if not null is the serialization mark for delta serialization.  Unused for deserialization.
   * @param tgtTs if not null is the target type system.  For serialization - this is a subset of the CASs TS
   * @param rfs For delta serialization - must be not null, and the saved value after deserializing the original
   *                                      before any modifications / additions made.
   *            For normal serialization - can be null, but if not, is used in place of re-calculating, for speed up
   *            For delta deserialization - must not be null, and is the saved value after serializing to the service
   *            For normal deserialization - must be null
   * @param doMeasurements if true, measurements are done (on serialization)
   * @param compressLevel if not null, specifies enum instance for compress level
   * @param compressStrategy if not null, specifies enum instance for compress strategy
   * @throws ResourceInitializationException if the target type system is incompatible with the source type system
   */
   
  public BinaryCasSerDes6(
      AbstractCas aCas,
      MarkerImpl mark,
      TypeSystemImpl tgtTs,
      ReuseInfo rfs,
      boolean doMeasurements,
      CompressLevel compressLevel, 
      CompressStrat compressStrategy) throws ResourceInitializationException {
    this(aCas, mark, tgtTs, false, false, rfs, doMeasurements, compressLevel, compressStrategy);
  }
   
  private BinaryCasSerDes6(
      AbstractCas aCas,
      MarkerImpl mark,
      TypeSystemImpl tgtTs,
      boolean storeTS,
      boolean storeTSI,
      ReuseInfo rfs,
      boolean doMeasurements,
      CompressLevel compressLevel, 
      CompressStrat compressStrategy) throws ResourceInitializationException {
    cas = ((CASImpl) ((aCas instanceof JCas) ? ((JCas)aCas).getCas(): aCas)).getBaseCAS();
    
    this.ts = cas.getTypeSystemImpl();
    this.mark = mark;
    if (null != mark && !mark.isValid() ) {
      throw new CASRuntimeException(
                CASRuntimeException.INVALID_MARKER, new String[] { "Invalid Marker." });
    }

    this.doMeasurements = doMeasurements;
    this.sm = doMeasurements ? new SerializationMeasures() : null;
    
    isDelta = isSerializingDelta = (mark != null);
    typeMapperCmn = typeMapper = ts.getTypeSystemMapper(tgtTs);
    isTypeMappingCmn = isTypeMapping = (null != typeMapper);
    isTsIncluded = storeTS;
    isTsiIncluded = storeTSI;
    
    heap = cas.getHeap().heap;
    heapEnd = cas.getHeap().getCellsUsed();
    heapStart = isSerializingDelta ? mark.getNextFSId() : 0;
    
    stringHeapObj = cas.getStringHeap();
    longHeapObj   = cas.getLongHeap();
    shortHeapObj  = cas.getShortHeap();
    byteHeapObj   = cas.getByteHeap();
       
    iPrevHeapArray = new int[ts.getTypeArraySize()];
    prevHeapInstanceWithIntValues = new int[ts.getTypeArraySize()] [];

    this.compressLevel = compressLevel;
    this.compressStrategy = compressStrategy;
    reuseInfoProvided = (rfs != null);
    if (reuseInfoProvided) {
      foundFSs = rfs.foundFSs;
      foundFSsArray = rfs.foundFSsArray;
      fsStartIndexes = rfs.fsStartIndexes.copy();
    } else {
      foundFSs = null;
      foundFSsArray = null;
      fsStartIndexes = new CasSeqAddrMaps();
    }
    this.tgtTs = tgtTs;
  }
  
  BinaryCasSerDes6(BinaryCasSerDes6 f6, TypeSystemImpl tgtTs) throws ResourceInitializationException {
    cas = f6.cas;
    
    this.ts = cas.getTypeSystemImpl();
    this.mark = f6.mark;
    if (null != mark && !mark.isValid() ) {
      throw new CASRuntimeException(
                CASRuntimeException.INVALID_MARKER, new String[] { "Invalid Marker." });
    }

    this.doMeasurements = f6.doMeasurements;
    this.sm = doMeasurements ? new SerializationMeasures() : null;
    
    isDelta = isSerializingDelta = (mark != null);
    typeMapperCmn = typeMapper = ts.getTypeSystemMapper(tgtTs);
    isTypeMappingCmn = isTypeMapping = (null != typeMapper);
    isTsIncluded = f6.isTsIncluded;
    isTsiIncluded = f6.isTsiIncluded;
    
    heap = cas.getHeap().heap;
    heapEnd = cas.getHeap().getCellsUsed();
    heapStart = isSerializingDelta ? mark.getNextFSId() : 0;
    
    stringHeapObj = cas.getStringHeap();
    longHeapObj   = cas.getLongHeap();
    shortHeapObj  = cas.getShortHeap();
    byteHeapObj   = cas.getByteHeap();
       
    iPrevHeapArray = new int[ts.getTypeArraySize()];
    prevHeapInstanceWithIntValues = new int[ts.getTypeArraySize()] [];

    this.compressLevel = f6.compressLevel;
    this.compressStrategy = f6.compressStrategy;
    reuseInfoProvided = f6.reuseInfoProvided;
    foundFSs = f6.foundFSs;
    foundFSsArray = f6.foundFSsArray;
    fsStartIndexes = f6.fsStartIndexes;
    this.tgtTs = tgtTs;
  }
  
  /**
   * Setup to serialize (not delta) or deserialize (not delta) using binary compression, no type mapping but only processing reachable Feature Structures
   * @param cas -
   * @throws ResourceInitializationException never thrown 
   */
  public BinaryCasSerDes6(AbstractCas cas) throws ResourceInitializationException {
    this(cas, null, null, false, false, null, false, CompressLevel.Default, CompressStrat.Default);
  }
  
  /**
   * Setup to serialize (not delta) or deserialize (not delta) using binary compression, with type mapping and only processing reachable Feature Structures
   * @param cas -
   * @param tgtTs -
   * @throws ResourceInitializationException if the target type system is incompatible with the source type system
   */
  public BinaryCasSerDes6(AbstractCas cas, TypeSystemImpl tgtTs) throws ResourceInitializationException {
    this(cas, null, tgtTs, false, false, null, false, CompressLevel.Default, CompressStrat.Default);
  }

  /**
   * Setup to serialize (maybe delta) or deserialize (maybe delta) using binary compression, with type mapping and only processing reachable Feature Structures
   * @param cas -
   * @param mark -
   * @param tgtTs -
   * @param rfs Reused Feature Structure information - required for both delta serialization and delta deserialization
   * @throws ResourceInitializationException if the target type system is incompatible with the source type system
   */
  public BinaryCasSerDes6(AbstractCas cas, MarkerImpl mark, TypeSystemImpl tgtTs, ReuseInfo rfs) throws ResourceInitializationException {
    this(cas, mark, tgtTs, false, false, rfs, false, CompressLevel.Default, CompressStrat.Default);
  }
  
  /**
   * Setup to serialize (maybe delta) or deserialize (maybe delta) using binary compression, with type mapping and only processing reachable Feature Structures, output measurements
   * @param cas -
   * @param mark -
   * @param tgtTs -
   * @param rfs Reused Feature Structure information - speed up on serialization, required on delta deserialization
   * @param doMeasurements -
   * @throws ResourceInitializationException if the target type system is incompatible with the source type system
   */
  public BinaryCasSerDes6(AbstractCas cas, MarkerImpl mark, TypeSystemImpl tgtTs, ReuseInfo rfs, boolean doMeasurements) throws ResourceInitializationException {
    this(cas, mark, tgtTs, false, false, rfs, doMeasurements, CompressLevel.Default, CompressStrat.Default);
  }

  /**
   * Setup to serialize (not delta) or deserialize (maybe delta) using binary compression, no type mapping and only processing reachable Feature Structures
   * @param cas -
   * @param rfs -
   * @throws ResourceInitializationException never thrown
   */
  public BinaryCasSerDes6(AbstractCas cas, ReuseInfo rfs) throws ResourceInitializationException {
    this(cas, null, null, false, false, rfs, false, CompressLevel.Default, CompressStrat.Default);
  }

  /**
   * Setup to serialize (not delta) or deserialize (maybe delta) using binary compression, no type mapping, optionally storing TSI, and only processing reachable Feature Structures
   * @param cas -
   * @param rfs -
   * @param storeTS - 
   * @param storeTSI - 
   * @throws ResourceInitializationException never thrown
   */
  public BinaryCasSerDes6(AbstractCas cas, ReuseInfo rfs, boolean storeTS, boolean storeTSI) throws ResourceInitializationException {
    this(cas, null, null, storeTS, storeTSI, rfs, false, CompressLevel.Default, CompressStrat.Default);
  }

  /*********************************************************************************************
   * S e r i a l i z e r   Class for sharing variables among routines
   * Class instantiated once per serialization
   * Multiple serializations in parallel supported, with multiple instances of this
   *********************************************************************************************/


  /*************************************************************************************
   *   S E R I A L I Z E
   * @param out -
   * @return null or serialization measurements (depending on setting of doMeasurements)
   * @throws IOException passthru
   *************************************************************************************/
  public SerializationMeasures serialize(Object out) throws IOException {
    if (isSerializingDelta && (tgtTs != null)) {
      throw new UnsupportedOperationException("Can't do Delta Serialization with different target TS");
    }

    if (isTsIncluded && (tgtTs != null)) {
      throw new UnsupportedOperationException("Can't store a different target TS in the serialized form");
    }
    
    if (fsStartIndexes == null) {
      if (isSerializingDelta) {
        throw new UnsupportedOperationException("Serializing a delta requires valid ReuseInfo for Cas being serialized," +
        		" captured right after it was deserialized");
      }
      if (isReadingDelta) {
        throw new UnsupportedOperationException("Deserializing a delta requires valid ReuseInfo for Cas being deserialized into");
      }
    }
    
    setupOutputStreams(out);
    
    if (doMeasurements) {
      System.out.println(printCasInfo(cas));
      sm.origAuxBytes = cas.getByteHeap().getSize();
      sm.origAuxShorts = cas.getShortHeap().getSize() * 2;
      sm.origAuxLongs = cas.getLongHeap().getSize() * 8;
      sm.totalTime = System.currentTimeMillis();
    }

    CommonSerDes.createHeader()
    .form6()
    .delta(isSerializingDelta)
    .seqVer(0)
    .typeSystemIncluded(isTsIncluded)
    .typeSystemIndexDefIncluded(isTsiIncluded)
    .write(serializedOut);
 
    if (isTsIncluded || isTsiIncluded) {
      CasIOUtils.writeTypeSystem(cas, serializedOut, isTsiIncluded);
    }
 
    os = new OptimizeStrings(doMeasurements);
 
    /******************************************************************
     * Find all FSs to be serialized via the indexes
     *   including those FSs referenced  
     * For Delta Serialization - excludes those FSs below the line
     ******************************************************************/
    
    if (!reuseInfoProvided || isSerializingDelta) {
//      long start = System.currentTimeMillis();
      processIndexedFeatureStructures(cas, false /* compute ref'd FSs, no write */);
//      System.out.format("Time to enqueue reachable FSs: %,.3f seconds%n", (System.currentTimeMillis() - start)/ 1000f);
    }
    
    
    
    /***************************
     * Prepare to walk main heap
     * We prescan the main heap and
     *   1) identify any types that should be skipped
     *      building a source and target fsStartIndexes table
     *   2) add all strings to the string table, 
     *      for strings above the mark
     ***************************/
   
      // scan thru all fs and save their offsets in the heap
      // to allow conversion from addr to sequential fs numbers
      // Also, compute sequential maps for non-equal type systems
      // As a side effect, also add all strings that are included
      // in the target type system to the set to be optimized.
      //   Note: for delta cas, this only picks up strings 
      //   referenced by FSs above the line
    totalMappedHeapSize = initFsStartIndexes();
    if (heapStart == 0) {
      totalMappedHeapSize++;  // include the null at the start
      heapStart = 1;  // slot 0 not serialized, it's null / 0
    }
    
    // add remaining strings for this case:
    //   deltaCas, FS below the line modified, modification is new string.
    //   use the deltaCasMod scanning
    final SerializeModifiedFSs smfs = isSerializingDelta ? new SerializeModifiedFSs() : null;
    if (isSerializingDelta) {
      smfs.addModifiedStrings();
    }
    

    /**************************
     * Strings
     **************************/
    
    os.optimize();
    writeStringInfo();
    
    /***************************
     * Prepare to walk main heap
     ***************************/
    writeVnumber(control_dos, totalMappedHeapSize);  
    if (doMeasurements) {
      sm.statDetails[Slot_MainHeap.ordinal()].original = (1 + heapEnd - heapStart) * 4;      
    }
    
    Arrays.fill(iPrevHeapArray, 0);
    Arrays.fill(prevHeapInstanceWithIntValues, null);
    
    /***************************
     * walk main heap
     ***************************/

    int iHeap;
    
//    { // debug
//      IntListIterator dit = foundFSs.iterator();
//      int column = 0;
//      int[] va = new int[100];
//      while (dit.hasNext()) {
//        va[column++] = dit.next();
//        if (column == 100) {
//          column = 0;
//          for (int i = 0; i < 100; i++) {
//            System.err.format("%,8d ", va[i]);
//          }
//          System.err.println("");
//        }
//      }
//      for (int i = 0; i < column; i++) {
//        System.err.format("%9d ", va[i]);
//      }
//      System.err.println("");
//    }
    int fsid = 1;
    for (int fssi = 0; fssi < foundFSsArray.length; fssi++) {
      iHeap = foundFSsArray[fssi];
      if (isDelta && iHeap < mark.nextFSId) {
        continue;
      }
      final int tCode = heap[iHeap];  // get type code
      final int mappedTypeCode = isTypeMapping ? typeMapper.mapTypeCodeSrc2Tgt(tCode) : tCode;
      if (TRACE_SER) {
        System.out.format("Ser: %,d adr: %,8d tCode: %,3d %13s tgtTypeCode: %,3d %n", 
            fsid, iHeap, tCode, ts.getTypeInfo(tCode).type.getShortName(), mappedTypeCode);
      }
      fsid ++;
      if (mappedTypeCode == 0) { // means no corresponding type in target system
        continue;
      }

      typeInfo = ts.getTypeInfo(tCode);
      iPrevHeap = iPrevHeapArray[tCode];
      
      writeVnumber(typeCode_dos, mappedTypeCode);

      if (typeInfo.isHeapStoredArray) {
        serializeHeapStoredArray(iHeap);
      } else if (typeInfo.isArray) {
        serializeNonHeapStoredArray(iHeap);
      } else {
        if (isTypeMapping) {
          // Serialize out in the order the features are in the target
          final int[] tgtFeatOffsets2Src = typeMapper.getTgtFeatOffsets2Src(tCode);
          for (int i = 0; i < tgtFeatOffsets2Src.length; i++) {
            final int featOffsetInSrc = tgtFeatOffsets2Src[i] + 1;  // add one for origin 1
            if (featOffsetInSrc == 0) {
              throw new RuntimeException(); // never happen because for serialization, target is never a superset of features of src
            }
            serializeByKind(iHeap, featOffsetInSrc);
          }
        } else {
          final int nbrSlots_p_1 = typeInfo.slotKinds.length + 1;
          for (int i = 1; i < nbrSlots_p_1; i++) {
            serializeByKind(iHeap, i);
          }
        }
      }
    
      iPrevHeapArray[tCode] = iHeap;  // make this one the "prev" one for subsequent testing
      if (doMeasurements) {
        sm.statDetails[typeCode_i].incr(DataIO.lengthVnumber(tCode));
        sm.mainHeapFSs ++;
      }
    }  // end of heap walk
    
    processIndexedFeatureStructures(cas, true /* pass 2 */);

    if (isSerializingDelta) {
      smfs.serializeModifiedFSs();
    }

    collectAndZip();

    if (doMeasurements) {
      sm.totalTime = System.currentTimeMillis() - sm.totalTime;
    }
    return sm;
  }
          
  private void serializeHeapStoredArray(int iHeap) throws IOException {
    final int length = serializeArrayLength(iHeap);
    // output values
    // special case 0 and 1st value
    if (length == 0) {
      return;
    }
    SlotKind arrayElementKind = typeInfo.slotKinds[1];
    final int endi = iHeap + length + 2;
    switch (arrayElementKind) {
    //  NOTE: short, byte, boolean, long, double arrays not stored on the heap
    case Slot_HeapRef: case Slot_Int:
      {
        int prev = (iPrevHeap == 0) ? 0 : 
                   (heap[iPrevHeap + 1] == 0) ? 0 : // prev length is 0
                   getPrevIntValue(iHeap, 2);
//                    heap[iPrevHeap + 2];  // use prev array 1st element
        final int startIheap = iHeap + 2;
        for (int i = startIheap; i < endi; i++) {
          final int maybeConverted = writeIntOrHeapRef(arrayElementKind.ordinal(), i, prev);
          if (isUpdatePrevOK && (i == startIheap)) {
            updatePrevIntValue(iHeap, 2, maybeConverted);
          } 
          prev = maybeConverted;
        }
      }
      break;
    case Slot_Float: 
      for (int i = iHeap + 2; i < endi; i++) {
        writeFloat(heap[i]);
      }
      break;
    case Slot_StrRef:
      for (int i = iHeap + 2; i < endi; i++) {
        if (TRACE_STR_ARRAY) {
          System.out.format("Trace Str Array Ser: addr: %,d string=%s%n", i, stringHeapObj.getStringForCode(heap[i]));
        }
        writeString(stringHeapObj.getStringForCode(heap[i]));
      }
      break;
      
    default: throw new RuntimeException("internal error");
    } // end of switch    
  }
  
  private int writeIntOrHeapRef(int kind, int index, int prev) throws IOException {
    final int v = heap[index];
    return writeDiff(kind, v, prev);
  }
  
  private long writeLongFromHeapIndex(int index, long prev) throws IOException {
    final long v = longHeapObj.getHeapValue(heap[index]);      
    writeLong(v, prev); 
    return v;
  }
  
  private void serializeNonHeapStoredArray(int iHeap) throws IOException {
    final int length = serializeArrayLength(iHeap);
    if (length == 0) {
      return;
    }
    SlotKind refKind = typeInfo.getSlotKind(2);
    switch (refKind) {
    case Slot_BooleanRef: case Slot_ByteRef:
      writeFromByteArray(refKind, heap[iHeap + 2], length);
      if (doMeasurements) {
        sm.statDetails[byte_i].incr(1);
        sm.origAuxByteArrayRefs += 4;
      }
      break; 
    case Slot_ShortRef:
      writeFromShortArray(heap[iHeap + 2], length);
      if (doMeasurements) {
        sm.origAuxShortArrayRefs += 4;
      }
      break; 
    case Slot_LongRef: case Slot_DoubleRef:
      writeFromLongArray(refKind, heap[iHeap + 2], length);
      if (doMeasurements) {
        sm.origAuxLongArrayRefs += 4;
      }
      break; 
    default:
      throw new RuntimeException();
    }
  }
  
  private void serializeByKind(int iHeap, int offset) throws IOException {
    SlotKind kind = typeInfo.getSlotKind(offset);      
    switch (kind) {
    //Slot_Int, Slot_Float, Slot_Boolean, Slot_Byte, Slot_Short
    case Slot_Int: case Slot_Short: case Slot_HeapRef:
      serializeDiffWithPrevTypeSlot(kind, iHeap, offset);
      break;
    case Slot_Float:
      writeFloat(heap[iHeap + offset]);
      break;
    case Slot_Boolean: case Slot_Byte:
      byte_dos.write(heap[iHeap + offset]);
      break;
    case Slot_StrRef: 
      writeString(stringHeapObj.getStringForCode(heap[iHeap + offset]));
      break;
    case Slot_LongRef: 
      writeLongFromHeapIndex(iHeap + offset, 
                (iPrevHeap == 0) ? 
                  0L : 
                  longHeapObj.getHeapValue(heap[iPrevHeap + offset]));
      break;
    case Slot_DoubleRef: 
      writeDouble(longHeapObj.getHeapValue(heap[iHeap + offset]));
      break;
    default: 
      throw new RuntimeException("internal error");
    } // end of switch
  }
  
  private int serializeArrayLength(int iHeap) throws IOException {
    final int length = heap[iHeap + 1];
    writeVnumber(arrayLength_i, length);
    return length;
  }
  
  private void serializeDiffWithPrevTypeSlot(SlotKind kind, int iHeap, int offset) throws IOException {
    final int prev = (iPrevHeap == 0) ? 0 : 
//      heap[iPrevHeap + offset];
      getPrevIntValue(iHeap, offset);
    final int newValue = heap[iHeap + offset];
    final int maybeConverted = writeDiff(kind.ordinal(), newValue, prev);
    if (isUpdatePrevOK) {
      updatePrevIntValue(iHeap, offset, maybeConverted);
    }
  }
  
  /**
   * 
   * @param iHeap index in the heap
   * @param offset offset to the slot
   * @param newValue for heap refs, is the converted-from-addr-to-seq-number value
   */
  private void updatePrevIntValue(final int iHeap, final int offset, final int newValue) {
    final int[] featCache = initPrevIntValue(iHeap); 
    featCache[offset -1] = newValue;
  }
  
  private int[] initPrevIntValue(final int iHeap) {
    final int[] featCache = prevHeapInstanceWithIntValues[heap[iHeap]];
    if (null == featCache) {
      return prevHeapInstanceWithIntValues[heap[iHeap]] = new int[typeInfo.slotKinds.length];
    }
    return featCache;
  }
  
  /**
   * 
   * @param iHeap index in the heap
   * @param offset true offset, 1 = first feature...
   * @return the previous int value for use in difference calculations
   */
  private int getPrevIntValue(final int iHeap, final int offset) {
    final int[] featCache = prevHeapInstanceWithIntValues[heap[iHeap]];
    if (null == featCache) {
      return 0;
    }
    return featCache[offset -1];
  }
  
  /**
   * Method:
   *   write with deflation into a single byte array stream
   *     skip if not worth deflating
   *     skip the Slot_Control stream
   *     record in the Slot_Control stream, for each deflated stream:
   *       the Slot index
   *       the number of compressed bytes
   *       the number of uncompressed bytes
   *   add to header:  
   *     nbr of compressed entries
   *     the Slot_Control stream size
   *     the Slot_Control stream
   *     all the zipped streams
   *
   * @throws IOException passthru
   */
  private void collectAndZip() throws IOException {
    ByteArrayOutputStream baosZipped = new ByteArrayOutputStream(4096);
    Deflater deflater = new Deflater(compressLevel.lvl, true);
    deflater.setStrategy(compressStrategy.strat);
    int nbrEntries = 0;
    
    List<Integer> idxAndLen = new ArrayList<Integer>();

    for (int i = 0; i < baosZipSources.length; i++) {
      ByteArrayOutputStream baos = baosZipSources[i];
      if (baos != null) {
        nbrEntries ++;
        dosZipSources[i].close();
        long startTime = System.currentTimeMillis();
        int zipBufSize = Math.max(1024, baos.size() / 100);
        deflater.reset();
        DeflaterOutputStream cds = new DeflaterOutputStream(baosZipped, deflater, zipBufSize);       
        baos.writeTo(cds);
        cds.close();
        idxAndLen.add(i);
        if (doMeasurements) {
          idxAndLen.add((int)(sm.statDetails[i].afterZip = deflater.getBytesWritten()));            
          idxAndLen.add((int)(sm.statDetails[i].beforeZip = deflater.getBytesRead()));
          sm.statDetails[i].zipTime = System.currentTimeMillis() - startTime;
        } else {
          idxAndLen.add((int)deflater.getBytesWritten());            
          idxAndLen.add((int)deflater.getBytesRead());
        }
      } 
    }
    serializedOut.writeInt(nbrEntries);                     // write number of entries
    for (int i = 0; i < idxAndLen.size();) {
      serializedOut.write(idxAndLen.get(i++));
      serializedOut.writeInt(idxAndLen.get(i++));
      serializedOut.writeInt(idxAndLen.get(i++));
    }
    baosZipped.writeTo(serializedOut);                      // write Compressed info
  }  
 
  private void writeLong(long v, long prev) throws IOException {
    writeDiff(long_High_i, (int)(v >>> 32), (int)(prev >>> 32));
    writeDiff(long_Low_i,  (int)v, (int)prev);    
  }

  /*
   * String encoding
   *   Length = 0 - used for null, no offset written
   *   Length = 1 - used for "", no offset written 
   *   Length > 0 (subtract 1): used for actual string length
   *   
   *   Length < 0 - use (-length) as slot index  (minimum is 1, slot 0 is NULL)
   *   
   *   For length > 0, write also the offset.
   */
  private void writeString(final String s) throws IOException {
    if (null == s) {
      writeVnumber(strLength_dos, 0);
      if (doMeasurements) {
        sm.statDetails[strLength_i].incr(1);
      }
      if (debugEOF) {
        System.out.format("writeString length null 0%n");
      }
      return;
    } 
    
    final int indexOrSeq = os.getIndexOrSeqIndex(s);
    if (indexOrSeq < 0) {
      final int v = encodeIntSign(indexOrSeq);
      writeVnumber(strLength_dos, v);
      if (doMeasurements) {
        sm.statDetails[strLength_i].incr(DataIO.lengthVnumber(v));
      }
      if (debugEOF) {
        System.out.format("writeString length %d%n", indexOrSeq);
      }
      return;
    }
    
    if (s.length() == 0) {
      writeVnumber(strLength_dos, encodeIntSign(1));
      if (doMeasurements) {
        sm.statDetails[strLength_i].incr(1);
      }
      if (debugEOF) {
        System.out.format("writeString length 0 as 1%n");
      }
      return;
    }
    
    if (s.length() == Integer.MAX_VALUE) {
      throw new RuntimeException("Cannot serialize string of Integer.MAX_VALUE length - too large.");
    }
    
    final int offset = os.getOffset(indexOrSeq);
    final int length = encodeIntSign(s.length() + 1);  // all lengths sign encoded because of above
    writeVnumber(strOffset_dos, offset);
    writeVnumber(strLength_dos, length);
    if (doMeasurements) {
      sm.statDetails[strOffset_i].incr(DataIO.lengthVnumber(offset));
      sm.statDetails[strLength_i].incr(DataIO.lengthVnumber(length));
    }
    if (!only1CommonString) {
      final int csi = os.getCommonStringIndex(indexOrSeq);
      writeVnumber(strSeg_dos, csi);
      if (doMeasurements) {
        sm.statDetails[strSeg_i].incr(DataIO.lengthVnumber(csi));
      }
    }
    if (debugEOF) {
      System.out.format("writeString length %,d offset %,d%n",
          length, offset);
    }
  }

  /*
   * Need to support NAN sets, 
   * 0x7fc.... for NAN
   * 0xff8.... for NAN, negative infinity
   * 0x7f8     for NAN, positive infinity
   * 
   * Because 0 occurs frequently, we reserve 
   * exp of 0 for the value 0
   *  
   */
  
  private void writeFloat(int raw) throws IOException {
    if (raw == 0) {
      writeUnsignedByte(float_Exponent_dos, 0);
      if (doMeasurements) {
        sm.statDetails[float_Exponent_i].incr(1);
      }
      return;
    }
   
    final int exponent = ((raw >>> 23) & 0xff) + 1;   // because we reserve 0, see above
    final int revMants = Integer.reverse((raw & 0x007fffff) << 9);  
    final int mants = (revMants << 1) + ((raw < 0) ? 1 : 0);
    writeVnumber(float_Exponent_dos, exponent); 
    writeVnumber(float_Mantissa_Sign_dos, mants);
    if (doMeasurements) {
      sm.statDetails[float_Exponent_i].incr(DataIO.lengthVnumber(exponent));
      sm.statDetails[float_Mantissa_Sign_i].incr(DataIO.lengthVnumber(mants));
    }
  }

  private void writeVnumber(int kind, int v) throws IOException {
    DataIO.writeVnumber(dosZipSources[kind], v);
    if (doMeasurements) {
      sm.statDetails[kind].incr(DataIO.lengthVnumber(v));
    }
  }
  
  private void writeVnumber(int kind, long v) throws IOException {
    DataIO.writeVnumber(dosZipSources[kind], v);
    if (doMeasurements) {
      sm.statDetails[kind].incr(DataIO.lengthVnumber(v));
    }
  }
  
  // this version doesn't do measurements, caller needs to do it
  private void writeVnumber(DataOutputStream s, int v) throws IOException {
    DataIO.writeVnumber(s, v);
  }
  
  // this version doesn't do measurements, caller needs to do it
  private void writeVnumber(DataOutputStream s, long v) throws IOException {
    DataIO.writeVnumber(s, v);
  }

  // this version doesn't do measurements, caller needs to do it    
  private void writeUnsignedByte(DataOutputStream s, int v) throws IOException {
    s.write(v);
  }

  private void writeDouble(long raw) throws IOException {
    if (raw == 0L) {
      writeVnumber(double_Exponent_dos, 0);
      if (doMeasurements) {
        sm.statDetails[double_Exponent_i].incr(1);
      }
      return;
    }
    int exponent = (int)((raw >>> 52) & 0x7ff);
    exponent = exponent - 1023; // rebase so 1.0 = 0
    if (exponent >= 0) {
      exponent ++; // skip "0", used above for 0 value
    }
    exponent = encodeIntSign(exponent);  
    final long revMants = Long.reverse((raw & 0x000fffffffffffffL) << 12);  
    final long mants = (revMants << 1) + ((raw < 0) ? 1 : 0);
    writeVnumber(double_Exponent_dos, exponent);
    writeVnumber(double_Mantissa_Sign_dos, mants);
    if (doMeasurements) {
      sm.statDetails[double_Exponent_i].incr(DataIO.lengthVnumber(exponent));
      sm.statDetails[double_Mantissa_Sign_i].incr(DataIO.lengthVnumber(mants));
    }
  }
  
  private int encodeIntSign(int v) {
    if (v < 0) {
      return ((-v) << 1) | 1;
    }
    return (v << 1);
  }

  /**
   * Encoding:
   *    bit 6 = sign:   1 = negative
   *    bit 7 = delta:  1 = delta
   * @param kind
   * @param i  runs from iHeap + 3 to end of array
   * sets isUpdatePrevOK true if ok to update prev, false if writing 0 for any reason, or max neg nbr
   * @returns possibly converted input value (converted if was heap ref to seq heap ref)
   * @throws IOException passthru 
   */
  private int writeDiff(int kind, int v, int prev) throws IOException {
    if (v == 0) {
      write0(kind);
      isUpdatePrevOK = false;
      return 0;
    }
    
    if (v == Integer.MIN_VALUE) { // special handling, because abs fails
      writeVnumber(kind, 2);      // written as -0
      if (doMeasurements) {
        sm.statDetails[kind].diffEncoded ++;
        sm.statDetails[kind].valueLeDiff ++;
      }
      isUpdatePrevOK = false;
      return 0;
    }
  
    // fsIndexes_i is for writing out modified FSs
    if ((kind == heapRef_i) || (kind == fsIndexes_i)) {
      if (!isInstanceInTgtTs(v)) {
        write0(kind);
        isUpdatePrevOK = false;
        return 0;
      }
      // for heap refs, we write out the seq # instead
      v = fsStartIndexes.getTgtSeqFromSrcAddr(v);
      if (v == -1) { // this ref goes to some fs not in target, substitute null
        if (kind == fsIndexes_i) {
          // can't happen - delta ser never done with a tgtTs different from srcTs
          throw new RuntimeException();
        }
        write0(kind); 
        isUpdatePrevOK = false;
        return 0;
      }
    }

    final int absV = Math.abs(v);
    if (((v > 0) && (prev > 0)) ||
        ((v < 0) && (prev < 0))) {
      final int diff = v - prev;  // guaranteed to not overflow because signs are the same
//      // handle strange behavior after JIT where the Math.abs(0x7fffffff) gives Integer.MIN_VALUE
//      // for arguments v = 0xffffffff, and prev = Integer.MIN_VALUE
//      final int diff = (prev == Integer.MIN_VALUE) ?
//          // v is guaranteed to be negative
//          (v & 0x7fffffff) :
//          v - prev;  
//      final int absDiff = Math.abs(diff);
      // this seems to work around
      final int absDiff = (diff < 0) ? -diff : diff; 
      // debug failure in Math.abs
      if (absDiff < 0) {
        System.err.format("********* caught absdiff v = %s, prev = %s diff = %s absDiff = %s%n", 
            Integer.toHexString(v),
            Integer.toHexString(prev),
            Integer.toHexString(diff),
            Integer.toHexString(absDiff));
      }
      if (absV < 0) {
        System.err.format("********* caught absv v = %s, absV = %s%n", 
            Integer.toHexString(v),
            Integer.toHexString(absV));
      }

      writeVnumber(kind, 
          (absV <= absDiff) ? 
              ((long)absV << 2)    + ((v < 0) ? 2L : 0L) :
              ((long)absDiff << 2) + ((diff < 0) ? 3L : 1L));
      if (doMeasurements) {
        sm.statDetails[kind].diffEncoded ++;
        sm.statDetails[kind].valueLeDiff += (absV <= absDiff) ? 1 : 0;
      }
      isUpdatePrevOK = true;
      return v;
    }
    // if get here, then the abs v value is always <= the abs diff value.
    writeVnumber(kind, ((long)absV << 2) + ((v < 0) ? 2 : 0));
    if (doMeasurements) {
      sm.statDetails[kind].diffEncoded ++;
      sm.statDetails[kind].valueLeDiff ++;
    }
    isUpdatePrevOK = true;
    return v;
  }

  private void write0(int kind) throws IOException {
    writeVnumber(kind, 0);  // a speedup, not a new encoding
    if (doMeasurements) {
      sm.statDetails[kind].diffEncoded ++;
      sm.statDetails[kind].valueLeDiff ++;
    }    
  }
  private void writeFromByteArray(SlotKind kind, int startPos, int length) throws IOException {
    byte_dos.write(byteHeapObj.heap, startPos, length);
  }

  private void writeFromLongArray(SlotKind kind, int startPos, int length) throws IOException {
    final long[] h = longHeapObj.heap;
    final int endPos = startPos + length;
    long prev = 0;
    for (int i = startPos; i < endPos; i++) {
      final long e = h[i];
      if (kind == Slot_DoubleRef) {
        writeDouble(e);
      } else {
        writeLong(e, prev);
        prev = e;
      }
    }
  }
  
  private void writeFromShortArray(int startPos, int length) throws IOException {
    final short[] h = shortHeapObj.heap;
    final int endPos = startPos + length;
    int prev = 0;
    for (int i = startPos; i < endPos; i++) {
      final short e = h[i];
      writeDiff(short_i, e, prev);
      prev = e;
    }
  }

  /******************************************************************************
   * Modified Values
   * Output:
   *   For each FS that has 1 or more modified values,
   *     write the heap addr converted to a seq # of the FS
   *     
   *     For all modified values within the FS:
   *       if it is an aux array element, write the index in the aux array and the new value
   *       otherwise, write the slot offset and the new value
   ******************************************************************************/
  private class SerializeModifiedFSs {

    final int[] modifiedMainHeapAddrs = toArrayOrINT0(cas.getModifiedFSHeapAddrs());
    final int[] modifiedFSs = toArrayOrINT0(cas.getModifiedFSList());
    final int[] modifiedByteHeapAddrs = toArrayOrINT0(cas.getModifiedByteHeapAddrs());
    final int[] modifiedShortHeapAddrs = toArrayOrINT0(cas.getModifiedShortHeapAddrs());
    final int[] modifiedLongHeapAddrs = toArrayOrINT0(cas.getModifiedLongHeapAddrs());

    {sortModifications();}  // a non-static initialization block
    
    final int modMainHeapAddrsLength = eliminateDuplicatesInMods(modifiedMainHeapAddrs);
    final int modFSsLength = eliminateDuplicatesInMods(modifiedFSs);
    final int modByteHeapAddrsLength = eliminateDuplicatesInMods(modifiedByteHeapAddrs);
    final int modShortHeapAddrsLength = eliminateDuplicatesInMods(modifiedShortHeapAddrs);
    final int modLongHeapAddrsLength = eliminateDuplicatesInMods(modifiedLongHeapAddrs);

    // ima           - index into modified arrays
    // ixx, iPrevxxx - index in heap being changed
    //                 value comes via the main heap or aux heaps
      
      int imaModMainHeap = 0;
      int imaModByteRef = 0;
      int imaModShortRef = 0;
      int imaModLongRef = 0;
 
      // previous value - for things diff encoded
    int vPrevModInt = 0;
    int vPrevModHeapRef = 0;
    short vPrevModShort = 0;
    long vPrevModLong = 0;
    
    int iHeap;
    TypeInfo typeInfo;   
    
    /**
     * For Delta Serialization:
     * Add any strings below the line 
     * Assume: no TS mapping (because it's delta serialization)
     */
    private void addModifiedStrings() {
//      System.out.println("Enter addModifiedStrings");
      for (int i = 0; i < modFSsLength; i++) {
        iHeap = modifiedFSs[i];
        // skip if no longer indexed-reachable change
        if (!foundFSsBelowMark.get(iHeap)) {
//          System.out.format("  skipping heap addr %,d%n", iHeap);
          continue;        
        }
        final int tCode = heap[iHeap];
        final TypeInfo typeInfo = ts.getTypeInfo(tCode);
//        System.out.format("  maybe adding string ");
        addStringFromFS(typeInfo, iHeap, tCode);
      }
//      System.out.println("Exit addModifiedStrings");
    }
    
    private void serializeModifiedFSs() throws IOException {
      int skipped = 0;
      // iterate over all modified feature structures
      /**
       * Theorems about these data
       *   1) Assumption: if an AuxHeap array is modified, its heap FS is in the list of modFSs
       *   2) FSs with AuxHeap values have increasing ref values into the Aux heap as FS addr increases
       *      (because the ref is not updateable).
       *   3) Assumption: String array element modifications are main heap slot changes
       *      and recorded as such
       */
      int prevHeapSeq = 0;
      final int splitPoint = mark.nextFSId;
      for (int i = 0; i < modFSsLength; i++) {
        iHeap = modifiedFSs[i];
        final boolean skipping = ((iHeap >= splitPoint) && !foundFSs.get(iHeap)) ||
                                 ((iHeap < splitPoint) && !foundFSsBelowMark.get(iHeap));
        final int tCode = heap[iHeap];
        typeInfo = ts.getTypeInfo(tCode);
        
        // write out the address of the modified FS
        // will convert to seq# internally
        if (!skipping) {
          prevHeapSeq = writeDiff(fsIndexes_i, iHeap, prevHeapSeq);
        }
        // delay updating prevHeapSeq until end of "for" loop - no longer done
        
        /**************************************************
         * handle aux byte, short, long array modifications
         **************************************************/
        if (typeInfo.isArray && (!typeInfo.isHeapStoredArray)) {
          writeAuxHeapMods(skipping);  // not used for long/double slot mods, only for arrays         
        } else { 
          writeMainHeapMods(skipping);   // includes long/double mods - the main heap value is changed
        }  // end of processing 1 modified FS
        if (skipping) {
          skipped ++;
        } 
      }  // end of for loop over all modified FSs
      // write out number of modified Feature Structures
      writeVnumber(control_dos, modFSsLength - skipped);

    }  // end of method
    
    // sort and remove duplicates
    private void sortModifications() {
      Arrays.sort(modifiedMainHeapAddrs);
      Arrays.sort(modifiedFSs);
      Arrays.sort(modifiedByteHeapAddrs);
      Arrays.sort(modifiedShortHeapAddrs);
      Arrays.sort(modifiedLongHeapAddrs);
    }
    
    private int eliminateDuplicatesInMods(final int[] sorted) {
      int length = sorted.length;
      if (length < 2) {
        return length;
      }
      
      int prev = sorted[0];
      int to = 1;
      for(int from = 1; from < length; from++) {
        int s = sorted[from];
        if (s == prev) {
          continue;
        }
        prev = s;
        sorted[to] = s;
        to++;
      }    
      return to;  // to is length
    }

    private int countModifiedSlotsInFs(int fsLength) {
      return countModifiedSlots(iHeap, fsLength, modifiedMainHeapAddrs, imaModMainHeap, modMainHeapAddrsLength);
    }
    
    /**
     * For arrays of boolean/byte, short, long/double, 
     *   the heap+1 is the length, 
     *   the heap+2 is the index of the first element in the aux array
     * @param modifiedAddrs
     * @param indexInModAddrs
     * @param length
     * @return for a particular array, the number of modified slots (>= 1)
     */
    private int countModifiedSlotsInAuxHeap(int[] modifiedAddrs, int indexInModAddrs, int length) {
      return countModifiedSlots(heap[iHeap + 2], heap[iHeap + 1], modifiedAddrs, indexInModAddrs, length);
    }
    
    private int countModifiedSlots(int firstAddr, int length, int[] modifiedAddrs, int indexInModAddrs, int modAddrsLength) {
      if (0 == length) {
        throw new RuntimeException();  // can't happen
      }
      final int nextAddr = firstAddr + length;
      int nextModAddr = modifiedAddrs[indexInModAddrs]; 
      if ((firstAddr > nextModAddr) ||
          (nextModAddr >= nextAddr)) {
        throw new RuntimeException(); // never happen - must have one slot at least modified in this fs          
      }
      int i = 1;
      for (;; i++) {
        if ((indexInModAddrs + i) == modAddrsLength) {
          break;
        }
        nextModAddr = modifiedAddrs[indexInModAddrs + i];
        if (nextModAddr >= nextAddr) {
          break;
        }
      }
      return i;
    }
    
    private void writeMainHeapMods(final boolean skipping) throws IOException {
      final int fsLength = incrToNextFs(heap, iHeap, typeInfo);
      final int numberOfModsInFs = countModifiedSlotsInFs(fsLength);
      if (!skipping) {
        writeVnumber(fsIndexes_dos, numberOfModsInFs);
      }
      int iPrevOffsetInFs = 0;

      for (int i = 0; i < numberOfModsInFs; i++) {
        final int nextMainHeapIndex = modifiedMainHeapAddrs[imaModMainHeap++];
        if (skipping) {
          continue;
        }
        final int offsetInFs = nextMainHeapIndex - iHeap;
        
        writeVnumber(fsIndexes_dos, offsetInFs - iPrevOffsetInFs);
        iPrevOffsetInFs = offsetInFs;
        
//        if (typeInfo.isArray && (typeInfo.getSlotKind(2) == Slot_StrRef)) {
//          System.out.println("writing string array mod");
//        }
        final SlotKind kind = typeInfo.getSlotKind(typeInfo.isArray ? 2 : offsetInFs);
//        System.out.format("mainHeapModWrite type: %s slot: %s%n", typeInfo, kind);
        
        switch (kind) {
        case Slot_HeapRef:
          vPrevModHeapRef = writeIntOrHeapRef(heapRef_i, nextMainHeapIndex, vPrevModHeapRef);
          break;
        case Slot_Int:
          vPrevModInt = writeIntOrHeapRef(int_i, nextMainHeapIndex, vPrevModInt);
          break;
        case Slot_Short:
          vPrevModShort = (short)writeIntOrHeapRef(int_i, nextMainHeapIndex, vPrevModShort);
          break;
        case Slot_LongRef:
          vPrevModLong = writeLongFromHeapIndex(nextMainHeapIndex, vPrevModLong); 
          break;
        case Slot_Byte: case Slot_Boolean:
          byte_dos.write(heap[nextMainHeapIndex]);
          break;
        case Slot_Float:
          writeFloat(heap[nextMainHeapIndex]);
          break;
        case Slot_StrRef:
          writeString(stringHeapObj.getStringForCode(heap[nextMainHeapIndex]));
          break;
        case Slot_DoubleRef:
          writeDouble(longHeapObj.getHeapValue(heap[nextMainHeapIndex]));
          break;
        default:
          throw new RuntimeException();
        }

      }  // end of looping for all modified slots in this FS
    }
    
    private void writeAuxHeapMods(final boolean skipping) throws IOException {
      final int auxHeapIndex = heap[iHeap + 2];
      int iPrevOffsetInAuxArray = 0;
      
      final SlotKind kind = typeInfo.getSlotKind(2);  // get kind of element
      final boolean isAuxByte = ((kind == Slot_BooleanRef) || (kind == Slot_ByteRef));
      final boolean isAuxShort = (kind == Slot_ShortRef);
      final boolean isAuxLong = ((kind == Slot_LongRef) || (kind == Slot_DoubleRef));
      
      if (!(isAuxByte || isAuxShort || isAuxLong)) {
        throw new RuntimeException();  // never happen
      }
      
      final int[] modXxxHeapAddrs = isAuxByte  ? modifiedByteHeapAddrs :
                                    isAuxShort ? modifiedShortHeapAddrs :
                                                 modifiedLongHeapAddrs;
      final int modXxxHeapAddrsLength = isAuxByte  ? modByteHeapAddrsLength :
                                        isAuxShort ? modShortHeapAddrsLength :
                                                     modLongHeapAddrsLength;
      int imaModXxxRef = isAuxByte  ? imaModByteRef :
                         isAuxShort ? imaModShortRef : 
                                      imaModLongRef;
      
      final int numberOfModsInAuxHeap = countModifiedSlotsInAuxHeap(modXxxHeapAddrs, imaModXxxRef, modXxxHeapAddrsLength);
      if (!skipping) {
        writeVnumber(fsIndexes_dos, numberOfModsInAuxHeap);
      }
      
      /**
       * for each modified slot in the AUX array, write
       *   - the index of that slot relative to the start of the array (0-based)
       *   - the new value
       */
      for (int i = 0; i < numberOfModsInAuxHeap; i++) {
        final int nextModAuxIndex = modXxxHeapAddrs[imaModXxxRef++];
        final int offsetInAuxArray = nextModAuxIndex - auxHeapIndex;
        if (!skipping) {
          writeVnumber(fsIndexes_dos, offsetInAuxArray - iPrevOffsetInAuxArray);
          iPrevOffsetInAuxArray = offsetInAuxArray;
          
          if (isAuxByte) {
            writeUnsignedByte(byte_dos, byteHeapObj.getHeapValue(nextModAuxIndex));
          } else if (isAuxShort) {
            final short v = shortHeapObj.getHeapValue(nextModAuxIndex);
            writeDiff(int_i, v, vPrevModShort);
            vPrevModShort = v;
          } else {
            long v = longHeapObj.getHeapValue(nextModAuxIndex);
            if (kind == Slot_LongRef) {
              writeLong(v, vPrevModLong);
              vPrevModLong = v;    
            } else {
              writeDouble(v);
            }
          }
        }
        
        if (isAuxByte) {
          imaModByteRef++;
        } else if (isAuxShort) {
          imaModShortRef++;
        } else {
          imaModLongRef++;
        }
        
      }
    }
  } // end of class definition for SerializeModifiedFSs
          
  /*************************************************************************************
   *   D E S E R I A L I Z E
   *************************************************************************************/   
  /**
   * 
   * @param istream -
   * @throws IOException -
   */
  public void deserialize(InputStream istream) throws IOException {
    Header h = readHeader(istream);  // side effect, sets deserIn

    if (isReadingDelta) {
      if (!reuseInfoProvided) {
        throw new UnsupportedOperationException("Deserializing Delta Cas, but original not serialized from");
      }
    } else {
      cas.resetNoQuestions();
    }
      
    cas.reinit(h, deserIn, null, CasLoadMode.DEFAULT, this, AllowPreexistingFS.allow, null);
//    deserializeAfterVersion(deserIn, isReadingDelta, AllowPreexistingFS.allow);
  }
  
  /**
   * Version used by uima-as to read delta cas from remote parallel steps
   * @param istream input stream
   * @param allowPreexistingFS what to do if item already exists below the mark
   * @throws IOException passthru
   */
  public void deserialize(InputStream istream, AllowPreexistingFS allowPreexistingFS) throws IOException {
    Header h = readHeader(istream);

    if (isReadingDelta) {
      if (!reuseInfoProvided) {
        throw new UnsupportedOperationException("Deserializing Delta Cas, but original not serialized from");
      }
    } else {
      throw new UnsupportedOperationException("Delta CAS required for this call");
    }

    cas.reinit(h, deserIn, null, CasLoadMode.DEFAULT, this, allowPreexistingFS, null);
  }
  
  
  public void deserializeAfterVersion(DataInputStream istream, boolean isDelta, AllowPreexistingFS allowPreexistingFS) throws IOException {

    this.allowPreexistingFS = allowPreexistingFS;
    if (allowPreexistingFS == AllowPreexistingFS.ignore) {
      throw new UnsupportedOperationException("AllowPreexistingFS.ignore not an allowed setting");
    }

    deserIn = istream;
    this.isDelta = isReadingDelta = isDelta;
    setupReadStreams();
    
    /************************************************
     * Read in the common string(s)
     ************************************************/
    int lenCmnStrs = readVnumber(strChars_dis);
    readCommonString = new String[lenCmnStrs];
    for (int i = 0; i < lenCmnStrs; i++) {
      readCommonString[i] = DataIO.readUTFv(strChars_dis);
    }
    only1CommonString = lenCmnStrs == 1;
    /***************************
     * Prepare to walk main heap
     ***************************/
    int heapUsedInTarget = readVnumber(control_dis);         
    final Heap heapObj = cas.getHeap();
    
    heapStart = isReadingDelta ? heapObj.getNextId() : 0;
    stringTableOffset = isReadingDelta ? (stringHeapObj.getSize() - 1) : 0;
    
    if (!isReadingDelta) {
      heapObj.reinitSizeOnly(1);
      heap = heapObj.heap;
    }
    
    Arrays.fill(iPrevHeapArray, 0);
    Arrays.fill(prevHeapInstanceWithIntValues, null);

    if (heapStart == 0) {
      heapStart = 1;  // slot 0 not serialized, it's null / 0
    }

    // For Delta CAS,
    //   Reuse previously computed map of addr <--> seq for existing FSs below mark line
    //                             map of seq(this CAS) <--> seq(incoming) 
    //                               that accounts for type code mismatch using typeMapper
    // note: rest of maps computed incrementally as we deserialize
    //   Two possibilities:  The CAS has a type, but the incoming is missing that type (services)
    //                       The incoming has a type, but the CAS is missing it - (deser from file)
    //     Below the merge line: only the 1st is possible
    //     Above the merge line: only the 2nd is possible

    if (isReadingDelta) {
      if (!reuseInfoProvided) {
        throw new IllegalStateException("Reading Delta into CAS not serialized from");
      }
    }

    fixupsNeeded = new IntVector(Math.max(16, heapObj.getCellsUsed() / 10));

    /**********************************************************
     * Read in new FSs being deserialized and add them to heap
     **********************************************************/
    for (int iHeap = heapStart, targetHeapUsed = isReadingDelta ? 0 : 1; targetHeapUsed < heapUsedInTarget;) {
      if (iHeap != heapObj.getNextId()) {
        throw new RuntimeException();
      }
      final int tgtTypeCode = readVnumber(typeCode_dis); // get type code
      final int srcTypeCode = isTypeMapping ? typeMapper.mapTypeCodeTgt2Src(tgtTypeCode) : tgtTypeCode;
      
      final boolean storeIt = (srcTypeCode != 0);
      // A receiving client from a service always
      // has a superset of the service's types due to type merging so this
      // won't happen for that use case. But 
      // a deserialize-from-file could hit this if the receiving type system
      // deleted a type.
    
      // The strategy for deserializing heap refs depends on finding
      // the prev value for that type.  This must be done in the context 
      // of the sending CAS's type system
    
      // typeInfo is Target Type Info
      final TypeInfo tgtTypeInfo = isTypeMapping ? tgtTs.getTypeInfo(tgtTypeCode) :
                                 ts.getTypeInfo(srcTypeCode);
      final TypeInfo srcTypeInfo = 
        (!isTypeMapping) ? tgtTypeInfo : 
        storeIt ?       ts.getTypeInfo(srcTypeCode) : 
                        null;
      if (storeIt) { 
        typeInfo = tgtTypeInfo;
        initPrevIntValue(iHeap);  // note "typeInfo" a hidden parameter - ugly...
      }
      if (TRACE_DES) {
        System.out.format("Des: addr %,5d tgtTypeCode: %,3d %13s srcTypeCode: %,3d%n", iHeap, tgtTypeCode, tgtTypeInfo.type.getShortName(),  srcTypeCode);
      }

//      if (srcTypeInfo == null) {
//        typeInfo = null;  // debugging
//      }
      typeInfo = storeIt ? srcTypeInfo : tgtTypeInfo; // if !storeIt, then srcTypeInfo is null.
      
      fsStartIndexes.addSrcAddrForTgt(iHeap, storeIt);
      if (storeIt) {
        iPrevHeap = iPrevHeapArray[srcTypeCode];  // will be ignored for non-existant type
      }
      if (typeInfo.isHeapStoredArray) {
        readHeapStoredArray(iHeap, storeIt, heapObj, srcTypeCode);
      } else if (typeInfo.isArray) {
        if (storeIt) {
          heapObj.add(3, srcTypeCode);
          heap = heapObj.heap;
        }
        readNonHeapStoredArray(iHeap, storeIt);
      } else {
        if (storeIt) {
          cas.ll_createFS(srcTypeCode);
          heap = heapObj.heap;
        }
        // is normal type with slots
        if (isTypeMapping && storeIt) {
          final int[] tgtFeatOffsets2Src = typeMapper.getTgtFeatOffsets2Src(srcTypeCode);
          for (int i = 0; i < tgtFeatOffsets2Src.length; i++) {
            final int featOffsetInSrc = tgtFeatOffsets2Src[i] + 1;
            SlotKind kind = tgtTypeInfo.slotKinds[i];  // target kind , may not exist in src
            readByKind(iHeap, featOffsetInSrc, kind, storeIt);
          }
        } else {
          for (int i = 1; i < typeInfo.slotKinds.length + 1; i++) {
            SlotKind kind = typeInfo.getSlotKind(i);
            readByKind(iHeap, i, kind, storeIt);
          }
        }
      }
      if (storeIt) {
        iPrevHeapArray[srcTypeCode] = iHeap;  // make this one the "prev" one for subsequent testing
      }
//       todo need to incr src heap by amt filtered (in case some slots missing, 
//                 need to incr tgt (for checking end) by unfiltered amount
//                 need to fixup final heap to account for skipped slots
//                 need to have read skip slots not present in src
      targetHeapUsed += incrToNextFs(heap, iHeap, tgtTypeInfo);  // typeInfo is target type info
      iHeap += storeIt ? incrToNextFs(heap, iHeap, srcTypeInfo) : 0;
    }
    
    final int end = fixupsNeeded.size();
    for (int i = 0; i < end; i++) {
      final int heapAddrToFix = fixupsNeeded.get(i);
      heap[heapAddrToFix] = fsStartIndexes.getSrcAddrFromTgtSeq(heap[heapAddrToFix]);
    }        
    
    readIndexedFeatureStructures();

    if (isReadingDelta) {
      (new ReadModifiedFSs()).readModifiedFSs();
    }

    closeDataInputs();
//      System.out.format("Deserialize took %,d ms%n", System.currentTimeMillis() - startTime1);
  }
  
  private void readNonHeapStoredArray(int iHeap, boolean storeIt) throws IOException {

    final int length = readArrayLength();
    if (storeIt) {
      heap[iHeap + 1] = length;
    }
    if (length == 0) {
      return;
    }
    SlotKind refKind = typeInfo.getSlotKind(2);
    switch (refKind) {
    case Slot_BooleanRef: case Slot_ByteRef:
      final int byteRef =  readIntoByteArray(length, storeIt);
      if (storeIt) {
        heap[iHeap + 2] = byteRef;
      }
      break; 
    case Slot_ShortRef:
      final int shortRef = readIntoShortArray(length, storeIt);
      if (storeIt) {
        heap[iHeap + 2] = shortRef;
      }
      break; 
    case Slot_LongRef: case Slot_DoubleRef:
      final int longDblRef = readIntoLongArray(refKind, length, storeIt);
      if (storeIt) {
        heap[iHeap + 2] = longDblRef;
      }
      break; 
    default:
      throw new RuntimeException();
    }
  }
  
  private int readArrayLength() throws IOException {
    return readVnumber(arrayLength_dis);
  }

  private void readHeapStoredArray(int iHeap, final boolean storeIt, final Heap heapObj, final int srcTypeCode) throws IOException {
    final int length = readArrayLength();
    if (storeIt) {
      heapObj.add(2 + length, srcTypeCode);
      heap = heapObj.heap;
      heap[iHeap + 1] = length;
    }
    // output values
    // special case 0 and 1st value
    if (length == 0) {
      return;
    }
    SlotKind arrayElementKind = typeInfo.slotKinds[1];
    final int endi = iHeap + length + 2;
    switch (arrayElementKind) {
    case Slot_HeapRef: case Slot_Int:
      {
        int prev = (iPrevHeap == 0) ? 0 : 
                   (heap[iPrevHeap + 1] == 0) ? 0 : // prev array length = 0
//                    heap[iPrevHeap + 2]; // prev array 0th element
                    getPrevIntValue(iHeap, 2);
        final int startIheap = iHeap + 2;
        for (int i = startIheap; i < endi; i++) {
          final int v = readDiff(arrayElementKind, prev);
          prev = v;
          if (startIheap == i && isUpdatePrevOK && storeIt) {
            updatePrevIntValue(iHeap, 2, v);
          }
          if (storeIt) {
            heap[i] = v;
            if (arrayElementKind == Slot_HeapRef) {
              fixupsNeeded.add(i);
//              System.out.format("debug adding to fixup, slot = %,d heapValue = %,d array%n", i, v);
            }
          }
        }
      }
      break;
    case Slot_Float: 
      for (int i = iHeap + 2; i < endi; i++) {
        final int floatRef = readFloat();
        if (storeIt) {
          heap[i] = floatRef;
        }
      }
      break;
    case Slot_StrRef:
      for (int i = iHeap + 2; i < endi; i++) {
        final int strRef = readString(storeIt);
        if (TRACE_STR_ARRAY) {
          System.out.format("Trace String Array Des addr: %,d storeIt=%s, string=%s%n", i, storeIt ? "Y" : "N", stringHeapObj.getStringForCode(strRef));
        }
        if (storeIt) {
          heap[i] = strRef; 
        }
      }
      break;
      
    default: throw new RuntimeException("internal error");
    } // end of switch    
  }
  
  /**
   *       
   * @param iHeap index in the heap
   * @param offset can be -1 - in which case read, but don't store
   * @throws IOException passthru
   */
  private void readByKind(int iHeap, int offset, SlotKind kind, boolean storeIt) throws IOException {
    
    if (offset == 0) {
      storeIt = false;
    }
    switch (kind) {
    case Slot_Int: case Slot_Short:
      readDiffWithPrevTypeSlot(kind, iHeap, offset, storeIt);
      break;
    case Slot_Float:
      final int floatAsInt = readFloat();
      if (storeIt) {
        heap[iHeap + offset] = floatAsInt;
      }
      break;
    case Slot_Boolean: case Slot_Byte:
      final byte vByte = byte_dis.readByte();
      if (storeIt) {
        heap[iHeap + offset] = vByte;
      }
      break;
    case Slot_HeapRef:
      readDiffWithPrevTypeSlot(kind, iHeap, offset, storeIt);
      if (storeIt) {
        fixupsNeeded.add(iHeap + offset);
      }
//      System.out.format("debug adding to fixup, slot = %,d heapValue = %,d readByKind%n", iHeap + offset, heap[iHeap + offset]);
      break;
    case Slot_StrRef: 
      final int vStrRef = readString(storeIt);
      if (storeIt) {
        heap[iHeap + offset] = vStrRef;
      }
      break;
    case Slot_LongRef: {
      long v = readLongOrDouble(kind, (!storeIt || (iPrevHeap == 0)) ? 0L : longHeapObj.getHeapValue(heap[iPrevHeap + offset]));
      if (v == 0L) {
        if (longZeroIndex == -1) {
          longZeroIndex = longHeapObj.addLong(0L);
        }
        if (storeIt) {
          heap[iHeap + offset] = longZeroIndex;
        }
      } else {
        if (storeIt) {
          heap[iHeap + offset] = longHeapObj.addLong(v);
        }
      }
      break;
    }
    case Slot_DoubleRef: {
      long v = readDouble();
      if (v == 0L) {
        if (longZeroIndex == -1) {
          longZeroIndex = longHeapObj.addLong(0L);
        }
        if (storeIt) {
          heap[iHeap + offset] = longZeroIndex;
        }
      } else if (v == DBL_1) {
        if (double1Index == -1) {
          double1Index = longHeapObj.addLong(DBL_1);
        }
        if (storeIt) {
          heap[iHeap + offset] = double1Index;
        }
      } else {
        if (storeIt) {
          heap[iHeap + offset] = longHeapObj.addLong(v);
        }
      }
      break;
    }
    default: 
      throw new RuntimeException("internal error");                
    } // end of switch
  }

  private void readIndexedFeatureStructures() throws IOException {
    final int nbrViews = readVnumber(control_dis);
    final int nbrSofas = readVnumber(control_dis);

    IntVector fsIndexes = new IntVector(nbrViews + nbrSofas + 100);
    fsIndexes.add(nbrViews);
    fsIndexes.add(nbrSofas);
    for (int i = 0; i < nbrSofas; i++) {
      final int realAddrOfSofa = fsStartIndexes.getSrcAddrFromTgtSeq(readVnumber(control_dis));
      fsIndexes.add(realAddrOfSofa);
    }
      
    for (int i = 0; i < nbrViews; i++) {
      readFsxPart(fsIndexes);     // added FSs
      if (isDelta) {
        readFsxPart(fsIndexes);   // removed FSs
        readFsxPart(fsIndexes);   // reindexed FSs
      }
    }
    
    if (isDelta) {
      // getArray avoids copying.
      // length is too long, but extra is never accessed
      cas.reinitDeltaIndexedFSs(fsIndexes.getArray());
    } else {
      cas.reinitIndexedFSs(fsIndexes.getArray());
    }
  }

  /**
   * Each FS index is sorted, and output is by delta 
   */
  private void readFsxPart(IntVector fsIndexes) throws IOException {
    final int nbrEntries = readVnumber(control_dis);
    int nbrEntriesAdded = 0;
    final int indexOfNbrAdded = fsIndexes.size();
    fsIndexes.add(0);  // a place holder, will be updated at end      
    int prev = 0;
    
    for (int i = 0; i < nbrEntries; i++) {
      int v = readVnumber(fsIndexes_dis) + prev;
      prev = v;
      v = fsStartIndexes.getSrcAddrFromTgtSeq(v);
      if (v > 0) {  // if not, no src type for this type in tgtTs
        nbrEntriesAdded++;
        fsIndexes.add(v);
      }
    }
    fsIndexes.set(indexOfNbrAdded, nbrEntriesAdded);
  } 

  
  private DataInput getInputStream(SlotKind kind) {
    return dataInputs[kind.ordinal()];
  }

  private int readVnumber(DataInputStream dis) throws IOException {
    return DataIO.readVnumber(dis);
  }

  private long readVlong(DataInputStream dis) throws IOException {
    return DataIO.readVlong(dis);
  }

  private int readIntoByteArray(int length, boolean storeIt) throws IOException { 
    if (storeIt) {
      final int startPos = byteHeapObj.reserve(length);
      byte_dis.readFully(byteHeapObj.heap, startPos, length);
      return startPos;
    } else {
      skipBytes(byte_dis, length);
      return 0;
    }
  }

  private int readIntoShortArray(int length, boolean storeIt) throws IOException {
    if (storeIt) {
      final int startPos = shortHeapObj.reserve(length);
      final short[] h = shortHeapObj.heap;
      final int endPos = startPos + length;
      short prev = 0;
      for (int i = startPos; i < endPos; i++) {
        h[i] = prev = (short)(readDiff(short_dis, prev));
      }
      return startPos;
    } else {
      skipBytes(short_dis, length * 2);
      return 0;
    }
  }
  
  private int readIntoLongArray(SlotKind kind, int length, boolean storeIt) throws IOException {
    if (storeIt) {
      final int startPos = longHeapObj.reserve(length);
      final long[] h = longHeapObj.heap;
      final int endPos = startPos + length;
      long prev = 0;
      for (int i = startPos; i < endPos; i++) {
        h[i] = prev = readLongOrDouble(kind, prev);
      }
      return startPos;
    } else {
      if (kind == Slot_LongRef) {
        skipLong(length);
      } else {
        skipDouble(length);
      }
      return 0;
    }
  }

  private void readDiffWithPrevTypeSlot(
      SlotKind kind, 
      int iHeap, 
      int offset,
      boolean storeIt) throws IOException {
    int v;
    if (storeIt) {
      int prev = (iPrevHeap == 0) ? 0 : 
//        heap[iPrevHeap + offset];
        getPrevIntValue(iHeap, offset);
      heap[iHeap + offset] = v = readDiff(kind, prev);
    } else {
      v = readDiff(kind, 0);
    }
    if (storeIt && isUpdatePrevOK) {
      updatePrevIntValue(iHeap, offset, v);
    }
  }

  private int readDiff(SlotKind kind, int prev) throws IOException {
    return readDiff(getInputStream(kind), prev);
  }
  
  // returns 2 values: the 2nd value is a boolean indicating if the
  //   value was encoded as a 0 or the max negative
  //   in which case updating of the "prev" is skipped
  //   2nd value returned in global (sigh)
  private int readDiff(DataInput in, int prev) throws IOException {
    final long encoded = readVlong(in);
    isUpdatePrevOK = encoded != 0;
    if (!isUpdatePrevOK) {
      return 0;
    }
    final boolean isDeltaEncoded = (0 != (encoded & 1L));
    final boolean isNegative = (0 != (encoded & 2L));
    int v = (int)(encoded >>> 2);
    if (isNegative) {
      if (v == 0) {
        isUpdatePrevOK = false;
        return Integer.MIN_VALUE;
      }
      v = -v;
    }
    if (isDeltaEncoded) {
      v = v + prev;
    }
    return v;
  }
      
  private long readLongOrDouble(SlotKind kind, long prev) throws IOException {
    if (kind == Slot_DoubleRef) {
      return readDouble();
    }
  
    final int vh = readDiff(long_High_dis, (int) (prev >>> 32));
    final int vl = readDiff(long_Low_dis, (int) prev);
    final long v = (((long)vh) << 32) | (0xffffffffL & (long)vl);
    return v;
  }
  
  private void skipLong(final int length) throws IOException {
    for (int i = 0; i < length; i++) {
      skipBytes(long_High_dis, 8);
      skipBytes(long_Low_dis, 8);
    }
  }
  
  private void skipDouble(final int length) throws IOException {
    for (int i = 0; i < length; i++) {
      readDouble();
    }
  }
     
  private int readFloat() throws IOException {
    final int exponent = readVnumber(float_Exponent_dis);  
    if (exponent == 0) {
      return 0;
    }
    int mants = readVnumber(float_Mantissa_Sign_dis);
    final boolean isNegative = (mants & 1) == 1;
    mants = mants >>> 1;
    // the next parens needed to get around eclipse / java bug
    mants = (Integer.reverse(mants) >>> 9);
        
    return ((exponent - 1) << 23) |
           mants | 
           ((isNegative) ? 0x80000000 : 0);        
  }
     
  private int decodeIntSign(int v) {
    if (1 == (v & 1)) {
      return - (v >>> 1);
    }
    return v >>> 1;
  }
  
  private long readDouble() throws IOException {
    int exponent = readVnumber(double_Exponent_dis);
    if (exponent == 0) {
      return 0L;
    }
    long mants = readVlong(double_Mantissa_Sign_dis);
    return decodeDouble(mants, exponent);
  }
  
  private long decodeDouble(long mants, int exponent) {
    exponent = decodeIntSign(exponent);
    if (exponent > 0) {
      exponent --;  
    }
    exponent = exponent + 1023; 
    long r = ((long)((exponent) & 0x7ff)) << 52;
    final boolean isNegative = (1 == (mants & 1));
    mants = Long.reverse(mants >>> 1) >>> 12;
    r = r | mants | (isNegative ? 0x8000000000000000L : 0);
    return r;
  }
          
  private long readVlong(DataInput dis) throws IOException {
    return DataIO.readVlong(dis);
  }
    
  private int readString(boolean storeIt) throws IOException {
    final int length = decodeIntSign(readVnumber(strLength_dis));
    if (debugEOF) {
//      System.out.format("readString length = %,d%n", length);
    }
    if (0 == length) {
      return 0;
    }
    if (1 == length) {
      // always store, in case later offset ref
//      if (storeIt) {
        return stringHeapObj.addString("");
//      } else {
//        return 0;
//      }
    }
    
    if (length < 0) {  // in this case, -length is the slot index
      if (storeIt) {
        if (TRACE_STR_ARRAY) {
          System.out.format("Trace String Array Des ref to offset %,d%n", length);
        }
        return stringTableOffset - length;
      } else {
        return 0;
      }
    }
    final int offset = readVnumber(strOffset_dis);
    final int segmentIndex = (only1CommonString) ? 0 :
      readVnumber(strSeg_dis);
    if (debugEOF) {
      System.out.format("readString offset = %,d%n", offset);
    }
    // need to store all strings, because an otherwise skipped one may be referenced
    //   later as an offset into the string table
//    if (storeIt) {
      String s =  readCommonString[segmentIndex].substring(offset, offset + length - 1);
      return stringHeapObj.addString(s);
//    } else {
//      return 0;
//    }
  }
  
  static void skipBytes(DataInputStream stream, int skipNumber) throws IOException {
    final int r = stream.skipBytes(skipNumber);
    if (r != skipNumber) {
      throw new IOException(String.format("%d bytes skipped when %d was requested, causing out-of-synch while deserializing from stream %s",
          r, skipNumber, stream));
    }
  }

  /******************************************************************************
   * Modified Values
   * 
   * Modified heap values need fsStartIndexes conversion
   ******************************************************************************/

  private class ReadModifiedFSs {
    
    // previous value - for things diff encoded
    private int vPrevModInt = 0;
    private int prevModHeapRefTgtSeq = 0;
    private short vPrevModShort = 0;
    private long vPrevModLong = 0;
    private int iHeap;
    private TypeInfo typeInfo;
    private int[] tgtF2srcF;
    
    // next for managing index removes / readds
    private boolean wasRemoved;
    private FSsTobeAddedbackSingle addbackSingle;
    private int[] featCodes;
    
    // for handling aux heaps with type mapping which may skip some things in the target
    //   An amount that needs to be added to the offset from target to account for
    //   source types and features not in the target.
    //
    // Because this is only done for Delta CAS, it is guaranteed that the 
    //   target cannot contain types or features that are not in the source
    //   (due to type merging)
//      int[] srcHeapIndexOffset; 
//      
//      Iterator<AuxSkip>[] srcSkipIt;  // iterator over skip points
//      AuxSkip[] srcNextSkipped;  // next skipped
//      int[] srcNextSkippedIndex;

    private void readModifiedFSs() throws IOException {
      final int modFSsLength = readVnumber(control_dis);
      int prevSeq = 0;
      
      if ((modFSsLength > 0) && (allowPreexistingFS == AllowPreexistingFS.disallow)) {
        CASRuntimeException e = new CASRuntimeException(
          CASRuntimeException.DELTA_CAS_PREEXISTING_FS_DISALLOWED,
            new String[] {String.format("%,d pre-existing Feature Structures modified", modFSsLength)});
        throw e;
      }
      
//        if (isTypeMapping) {
//          for (int i = 0; i < AuxHeapsCount; i++) {
//            srcHeapIndexOffset[i] = 0;
//            srcSkipIt[i] = fsStartIndexes.skips.get(i).iterator();
//            srcNextSkipped[i] = (srcSkipIt[i].hasNext()) ? srcSkipIt[i].next() : null;
//            srcNextSkippedIndex[i] = (srcNextSkipped[i] == null) ? Integer.MAX_VALUE : srcNextSkipped[i].skipIndex;
//          }
//        }
               
      for (int i = 0; i < modFSsLength; i++) {
        final int seqNbrModified = readDiff(fsIndexes_dis, prevSeq);
//          iHeap = readVnumber(fsIndexes_dis) + iPrevHeap;
        prevSeq = seqNbrModified;
//          iPrevHeap = iHeap;

        iHeap = fsStartIndexes.getSrcAddrFromTgtSeq(seqNbrModified);
        if (iHeap < 1) {
          // never happen because in the delta CAS ts system use-case, the 
          //   target is always a subset of the source
          //   due to type system merging
          throw new RuntimeException("never happen");
        }
        final int tCode = heap[iHeap];
        typeInfo = ts.getTypeInfo(tCode);
        if (isTypeMapping) {
          tgtF2srcF = typeMapper.getTgtFeatOffsets2Src(tCode);
        }
        
        final int numberOfModsInThisFs = readVnumber(fsIndexes_dis); 

        if (typeInfo.isArray && (!typeInfo.isHeapStoredArray)) { 
          /**************************************************
           * handle aux byte, short, long array modifications
           *   Note: boolean stored in byte array
           *   Note: strings are heap-store-arrays
           **************************************************/
           readModifiedAuxHeap(numberOfModsInThisFs);
        } else {
          // https://issues.apache.org/jira/browse/UIMA-4100
          featCodes = cas.getTypeSystemImpl().ll_getAppropriateFeatures(tCode);
//          cas.removeFromCorruptableIndexAnyView(iHeap, indexToDos);
          try {
            wasRemoved = false;
            readModifiedMainHeap(numberOfModsInThisFs);
          } finally {
            cas.addbackSingle(iHeap);
          }   
        }
      }
    }
    
    // update the byte/short/long aux heap entries
    // for arrays
    /*
     * update the byte/short/long aux heap entries
     * Only called for arrays
     * No aux heap offset adjustments needed since we get
     *   the accuract source start point from the source heap
     */
    private void readModifiedAuxHeap(int numberOfMods) throws IOException {
      int prevOffset = 0;      
      final int auxHeapIndex = heap[iHeap + 2];
      final SlotKind kind = typeInfo.getSlotKind(2);  // get kind of element
      final boolean isAuxByte = ((kind == Slot_BooleanRef) || (kind == Slot_ByteRef));
      final boolean isAuxShort = (kind == Slot_ShortRef);
      final boolean isAuxLong = ((kind == Slot_LongRef) || (kind == Slot_DoubleRef));
      if (!(isAuxByte | isAuxShort | isAuxLong)) {
        throw new RuntimeException();  // never happen
      }
      
      for (int i2 = 0; i2 < numberOfMods; i2++) {
        final int offset = readVnumber(fsIndexes_dis) + prevOffset;
        prevOffset = offset;
        
        if (isAuxByte) {
          byteHeapObj.setHeapValue(byte_dis.readByte(), auxHeapIndex + offset);
        } else if (isAuxShort) {
          final short v = (short)readDiff(int_dis, vPrevModShort);
          vPrevModShort = v;
          shortHeapObj.setHeapValue(v, auxHeapIndex + offset);
        } else {
          final long v = readLongOrDouble(kind, vPrevModLong);
          if (kind == Slot_LongRef) {
            vPrevModLong = v;
          }
          longHeapObj.setHeapValue(v, auxHeapIndex + offset);
        }    
      }
    }
    
    private void readModifiedMainHeap(int numberOfMods) throws IOException {
      
      int iPrevTgtOffsetInFs = 0;
      wasRemoved = false;  // set to true when removed from index to stop further testing
      addbackSingle = cas.getAddbackSingle();
      addbackSingle.clear();

      
      for (int i = 0; i < numberOfMods; i++) {
        final int tgtOffsetInFs = readVnumber(fsIndexes_dis) + iPrevTgtOffsetInFs;
        iPrevTgtOffsetInFs = tgtOffsetInFs;
        final int srcOffsetInFs = isTypeMapping ? tgtF2srcF[tgtOffsetInFs] : tgtOffsetInFs;
        if (srcOffsetInFs < 0) {
          // never happen because if type mapping, and delta cas being deserialized,
          //   all of the target features would have been merged into the source ones.
          throw new RuntimeException();
        }
        final SlotKind kind = typeInfo.getSlotKind(typeInfo.isArray ? 2 : srcOffsetInFs);
//        System.out.format("mainHeapModRead type: %s slot: %s%n", typeInfo, kind);
        switch (kind) {
        case Slot_HeapRef: {
            final int tgtSeq = readDiff(heapRef_dis, prevModHeapRefTgtSeq);
            prevModHeapRefTgtSeq = tgtSeq;
            final int v = fsStartIndexes.getSrcAddrFromTgtSeq(tgtSeq);
            // can never be 0 - because is delta and tgt ts is always a subset of src one
            heap[iHeap + srcOffsetInFs] = v;
          }
          break;
        case Slot_Int: {
            final int v = readDiff(int_dis, vPrevModInt);
            vPrevModInt = v;
            heap[iHeap + srcOffsetInFs] = v;
            maybeRemove(srcOffsetInFs);
          }
          break;
        case Slot_Short: {
            final int v = readDiff(int_dis, vPrevModShort);
            vPrevModShort = (short)v;
            heap[iHeap + srcOffsetInFs] = v;
          }
          break;
        case Slot_LongRef: {
            final long v = readLongOrDouble(kind, vPrevModLong);
            vPrevModLong = v;
            heap[iHeap + srcOffsetInFs] = longHeapObj.addLong(v);
          }
          break;
        case Slot_DoubleRef: {
            final long v = readDouble();
            heap[iHeap + srcOffsetInFs] = longHeapObj.addLong(v);
          }
          break;
        case Slot_Byte: case Slot_Boolean:
          heap[iHeap + srcOffsetInFs] = byte_dis.readByte();
          break;
        case Slot_Float:
          heap[iHeap + srcOffsetInFs] = readFloat();
          maybeRemove(srcOffsetInFs);
          break;
        case Slot_StrRef:
          heap[iHeap + srcOffsetInFs] = readString(true);
          maybeRemove(srcOffsetInFs);
          break;
       default:
          throw new RuntimeException();
        }
      }   
    }
    
    private void maybeRemove(int srcOffsetInFs) {
      if (!typeInfo.isHeapStoredArray && !wasRemoved) {
        wasRemoved |= cas.removeFromCorruptableIndexAnyView(iHeap, addbackSingle, featCodes[srcOffsetInFs - 1]);
      }
    }    
    
  }
  
  

  /* *******************************************************************
   * methods common to serialization / deserialization etc.
   ********************************************************************/
  
  
  private static int incrToNextFs(int[] heap, int iHeap, TypeInfo typeInfo) {
    if (typeInfo.isHeapStoredArray) {
      return 2 + heap[iHeap + 1];
    } else {
      return 1 + typeInfo.slotKinds.length;
    }
  }

  /*
   * This routine uses the same "scanning" to do two completely different things:
   *   The first thing is to generate an ordered set (by heap addr) 
   *   of all FSs that are to be serialized:
   *     because they are in some index, or
   *     are pointed to by something that is in some index (recursively)
   *   excluding those below the mark
   *   
   *   The second thing is to serialize out the index information.
   *   This step has to wait until the first time call has completed and 
   *   the fsStartIndexes instance has a chance to be built.
   * 
   * The cas is passed in so that the Compare can use this for two different CASes
   * 
   */
  private void processIndexedFeatureStructures(CASImpl cas, boolean isWrite) throws IOException {
    if (!isWrite) {
      foundFSs = new BitSet(Math.max(1024, cas.getHeap().getCellsUsed()));
      foundFSsBelowMark = isSerializingDelta ? new BitSet(mark.nextByteHeapAddr) : null;
    }
    final int[] fsIndexes = isWrite ? 
                              // this alternative collects just the new FSs above the line
                              (isSerializingDelta ? cas.getDeltaIndexedFSs(mark) : savedAllIndexesFSs) :
                              // this alternative picks up the following use case:
                              //   A modification of something below the line now has a new fs ref to something
                              //   above the line, not otherwise referenced
                                cas.getIndexedFSs();  
    if (!isWrite) {
      savedAllIndexesFSs = fsIndexes;
      toBeScanned.removeAllElements();
    }
    final int nbrViews = fsIndexes[0];
    final int nbrSofas = fsIndexes[1];

    if (isWrite) {
      if (doMeasurements) {
        sm.statDetails[fsIndexes_i].original = fsIndexes.length * 4 + 1;      
      }
      writeVnumber(control_i, nbrViews);
      writeVnumber(control_i, nbrSofas);
      if (doMeasurements) {
        sm.statDetails[fsIndexes_i].incr(1); // an approximation - probably correct
        sm.statDetails[fsIndexes_i].incr(1);
      }
    }
        
    int fi = 2;
    
    final int end1 = nbrSofas + 2;
    for (; fi < end1; fi++) {
//      writeVnumber(control_i, fsIndexes[fi]);  // version 0
      final int addrSofaFs = fsIndexes[fi];
      if (isWrite) {
        // never returns -1, because this is for the sofa fs, and that's built-in
        final int v = fsStartIndexes.getTgtSeqFromSrcAddr(addrSofaFs);
        writeVnumber(control_i, v);    // version 1
         
        if (doMeasurements) {
          sm.statDetails[fsIndexes_i].incr(DataIO.lengthVnumber(v));
        }
      } else {
        enqueueFS(addrSofaFs);  //sofa fs's always in the type system
      }
    }
    
    heap = cas.getHeap().heap;   // referred to in processFsxPart
    for (int vi = 0; vi < nbrViews; vi++) {
      fi = processFsxPart(fsIndexes, fi, true, isWrite);    // added FSs
      if (isWrite && isSerializingDelta) {
        fi = processFsxPart(fsIndexes, fi, false, true);  // removed FSs
        fi = processFsxPart(fsIndexes, fi, false, true);  // reindexed FSs
      }
    }
    
    processRefedFSs();
    
    if (!isWrite) {
      final int fsslen = foundFSs.cardinality();
      foundFSsArray = new int[fsslen];
      final int len = foundFSs.length();
    
      for (int b = 0, i = 0; b < len; b++, i++) {
        b = foundFSs.nextSetBit(b);
        foundFSsArray[i] = b;
      }
//      final IntPointerIterator foundFSsIteratorx = foundFSs.pointerIterator();
//      foundFSsIteratorx.moveToFirst();
//      final int fsslen = foundFSs.size();
//      foundFSsArray = new int[fsslen];
//      for (int i = 0; i < fsslen; i++) {
//        foundFSsArray[i] = foundFSsIteratorx.get();
//        foundFSsIteratorx.inc();
//      }
//      Arrays.sort(foundFSsArray);
    }
    return;
  }

  private int processFsxPart(
      final int[] fsIndexes, 
      final int fsNdxStart,
      final boolean isDoingEnqueue, 
      final boolean isWrite) throws IOException {
    int ix = fsNdxStart;
    final int nbrEntries = fsIndexes[ix++];
    final int end = ix + nbrEntries;
    // version 0
//      writeVnumber(fsIndexes_dos, nbrEntries);  // number of entries
    //version 1: the list is filtered by the tgt type, and may be smaller;
    //  it is written at the end, into the control_dos stream
//      if (doMeasurements) {
//        sm.statDetails[typeCode_i].incr(DataIO.lengthVnumber(nbrEntries));
//      }
    
    final int[] ia = new int[nbrEntries];
    // Arrays are sorted, because order doesn't matter to the logic, but
    // sorted arrays can be compressed via diff encoding better
    System.arraycopy(fsIndexes, ix, ia, 0, nbrEntries);
    Arrays.sort(ia);
   
    int prev = 0;
    int entriesWritten = 0;  // can be less than nbrEntries if type mapping excludes some types in target
    
    for (int i = 0; i < ia.length; i++) {
      final int fsAddr = ia[i];
      if (isWrite) {
        if (isTypeMapping && (0 == typeMapper.mapTypeCodeSrc2Tgt(heap[fsAddr]))) {
          continue;
        }
        final int tgtV = fsStartIndexes.getTgtSeqFromSrcAddr(fsAddr);
        if (tgtV == -1) {
          throw new RuntimeException();
        }
        final int delta = tgtV - prev;
        entriesWritten++;
        writeVnumber(fsIndexes_dos, delta);
        if (doMeasurements) {
          sm.statDetails[fsIndexes_i].incr(DataIO.lengthVnumber(delta));
        }
        prev = tgtV;
      } else { 
        if (isDoingEnqueue) {
          enqueueFS(fsAddr);
        }
      }
    }
    if (isWrite) {
      writeVnumber(control_dos, entriesWritten);  // version 1  
      if (doMeasurements) {
        sm.statDetails[typeCode_i].incr(DataIO.lengthVnumber(entriesWritten));
      }
    }
    return end;
  } 

  private void enqueueFS(int fsAddr) {
    if (!isInstanceInTgtTs(fsAddr)) {
      return;
    }

    if (0 != fsAddr) {
      boolean added;
      if (fsAddr >= heapStart) { // separately track items below the line
        added = !foundFSs.get(fsAddr);
        if (added) {
          foundFSs.set(fsAddr);
          toBeScanned.add(fsAddr);
        }
      } else {
        added = !foundFSsBelowMark.get(fsAddr);
        if (added) {
          foundFSsBelowMark.set(fsAddr);
          toBeScanned.add(fsAddr);
        }
      }
    }
  }
  
  private boolean isInstanceInTgtTs(int fsAddr) {
    return !isTypeMapping || (0 != typeMapper.mapTypeCodeSrc2Tgt(heap[fsAddr]));
  }
  
  private void processRefedFSs() {
    for (int i = 0; i < toBeScanned.size(); i++) {
      enqueueFeatures(toBeScanned.get(i));
    }
  }
  
  
  /**
   * Enqueue all FSs reachable from features of the given FS.
   */
  private void enqueueFeatures(int addr) {
    final int tCode = heap[addr];
    final TypeInfo typeInfo = ts.getTypeInfo(tCode);
    final SlotKind[] kinds = typeInfo.slotKinds;
    
    if (typeInfo.isHeapStoredArray && (Slot_HeapRef == kinds[1])) {
      // fs array, add elements
      final int length = heap[addr + 1];
      for (int i = 0; i < length; i++) {
        enqueueFS(heap[addr + 2 + i]);
      }
      return;
    }
    
    // not an FS Array
    if (typeInfo.isArray) {
      return;
    }
  
    if (isTypeMapping) {
      final int[] tgtFeatOffsets2Src = typeMapper.getTgtFeatOffsets2Src(tCode);
//      if (tgtFeatOffsets2Src == null ) {
//        System.out.println("debug caught");
//      }
      for (int i = 0; i < tgtFeatOffsets2Src.length; i++) {
        final int featOffsetInSrc = tgtFeatOffsets2Src[i] + 1;  // add one for origin 1
        if (featOffsetInSrc == 0) {
          throw new RuntimeException(); // never happen because for serialization, target is never a superset of features of src
        }
        if (kinds[featOffsetInSrc - 1] == Slot_HeapRef) {
          enqueueFS(heap[addr + featOffsetInSrc]);
        }
      }
    } else {
      for (int i = 1; i < typeInfo.slotKinds.length + 1; i++) {
        if (kinds[i - 1] == Slot_HeapRef) {
          enqueueFS(heap[addr + i]);
        }
      }
    }
  }
  
  
  /**
   * Serializing:
   *   Called at beginning of serialize, scans whole CAS or just delta CAS
   *   If doing delta serialization, fsStartIndexes is passed in, pre-initialized with a copy of the map info below the line.
   * @return amount of heap used in target, side effect: set up fsStartIndexes (for both src and tgt)
   */
  private int initFsStartIndexes () {
   
    final boolean isTypeMapping = isTypeMappingCmn;
    final CasTypeSystemMapper typeMapper = typeMapperCmn;
    
    int tgtHeapUsed = 0;
    int nextTgtHeap = isSerializingDelta ? mark.nextFSId : 1;
    
    // for delta serialization - the iterator is only for things above the line.

    for (int i = 0; i < foundFSsArray.length; i++) {
      final int iSrcHeap = foundFSsArray[i];
      // for delta serialization, no type mapping is supported, 
      // however, some created FSs above the line may not be "reachable" and 
      // therefore, skipped. 
      final int iTgtHeap = nextTgtHeap;
      final int tCode = heap[iSrcHeap];
      final int tgtTypeCode = isTypeMapping ? typeMapper.mapTypeCodeSrc2Tgt(tCode) : tCode;
      final boolean isIncludedType = (tgtTypeCode != 0);
      
      // record info for type
      fsStartIndexes.addItemAddr(iSrcHeap, iTgtHeap, isIncludedType);  // maps src heap to tgt seq
      
      // for features in type - 
      //    strings: accumulate those strings that are in the target, if optimizeStrings != null
      //      strings either in array, or in individual values
      //    byte (array), short (array), long/double (instance or array): record if entries in aux array are skipped
      //      (not in the target).  Note the recording will be in a non-ordered manner (due to possible updates by
      //       previous delta deserialization)
      final TypeInfo srcTypeInfo = ts.getTypeInfo(tCode);
      final TypeInfo tgtTypeInfo = (isTypeMapping && isIncludedType) ? 
          typeMapper.tsTgt.get().getTypeInfo(tgtTypeCode) : 
          srcTypeInfo;
      
      
      // add strings for included types (only when serializing)
      if (isIncludedType && (os != null)) { 
        // skip if delta and fs is below the line
        // Well, we can't do that - it may be that the fs is below the line, but the string slot 
        //   has been updated  (modified).
        // Well, this code is only called for FSs above the line... So need another method
        //   to pick up those modified strings - see addModifiedStrings;
            
        // next test only true if tgtTypeInfo.slotKinds[1] == Slot_StrRef
        // because this is the built-in type string array which is final
        addStringFromFS(srcTypeInfo, iSrcHeap, tCode);
      }
            
      // Advance to next Feature Structure, in both source and target heap frame of reference
      if (isIncludedType) {
        final int deltaTgtHeap = incrToNextFs(heap, iSrcHeap, tgtTypeInfo);
        nextTgtHeap += deltaTgtHeap;
//        if (iSrcHeap >= heapStart) {  // don't use up tgt heap if delta, and below the mark
                                        // with current design is always true.
          tgtHeapUsed += deltaTgtHeap;
//        }
      }
    }
    
    return tgtHeapUsed;  // side effect: set up fsStartIndexes
  } 

  private void addStringFromFS(TypeInfo srcTypeInfo, int iSrcHeap, int tCode) {
    final int markStringHeap = (isDelta) ? mark.getNextStringHeapAddr() : 0;
    if (srcTypeInfo.isHeapStoredArray && (srcTypeInfo.slotKinds[1] == Slot_StrRef)) {
      for (int i = 0; i < heap[iSrcHeap + 1]; i++) {
        // this bit of strange logic depends on the fact that all new and updated strings
        // are "added" at the end of the string heap in the current impl
        final int strHeapIndex = heap[iSrcHeap + 2 + i];
        if (strHeapIndex >= markStringHeap) {
          os.add(stringHeapObj.getStringForCode(strHeapIndex));
//          System.out.format("addStringFromFS:  %s%n", stringHeapObj.getStringForCode(strHeapIndex));
        } else {
//          System.out.format("addStringFromFS: skipping add of str %s%n", stringHeapObj.getStringForCode(strHeapIndex));
        }
        
      }
    } else {
      final int[] strOffsets = srcTypeInfo.strRefOffsets;  // slot x (numbered 0 corresponding to 1st feature slot after type code) appears as x + 1
      final boolean[] fSrcInTgt = isTypeMapping ? typeMapper.getFSrcInTgt(tCode) : null;
      for (int i = 0; i < strOffsets.length; i++ ) {
        int srcOffset = strOffsets[i];  // offset to slot having str ref
        // add only those strings in slots that are in target
        if (!isTypeMapping || fSrcInTgt[srcOffset - 1]) {  //to convert to 0 based indexing
          final int strHeapIndex = heap[iSrcHeap + srcOffset];
          // this bit of strange logic depends on the fact that all new and updated strings
          // are "added" at the end of the string heap in the current impl
          if (strHeapIndex >= markStringHeap) {
            os.add(stringHeapObj.getStringForCode(strHeapIndex));
          }
        }
      }
    }
  }
  /**
   * Compare 2 CASes, with perhaps different type systems.
   * If the type systems are different, construct a type mapper and use that
   *   to selectively ignore types or features not in other type system
   *   
   * The Mapper filters C1 -%gt; C2.  
   * 
   * Compare only feature structures reachable via indexes or refs
   *   The order must match
   * 
   * @param c1 CAS to compare
   * @param c2 CAS to compare
   * @return true if equal (for types / features in both)
   */
  
  public boolean compareCASes(CASImpl c1, CASImpl c2) {
    return new CasCompare(c1, c2).compareCASes();
  }
  
  private class CasCompare {
    /** 
     * Compare 2 CASes for equal
     * The layout of refs to aux heaps does not have to match
     */
      final private CASImpl c1;
      final private CASImpl c2;
      final private TypeSystemImpl ts1;      
      final private TypeSystemImpl ts2;
      final private Heap c1HO;
      final private Heap c2HO;
      final private int[] c1heap;
      final private int[] c2heap;
      
      private TypeInfo typeInfo;
      private int c1heapIndex;
      private int c2heapIndex;
      
      final private Int2IntRBT addr2seq1;
      final private Int2IntRBT addr2seq2;
            
    public CasCompare(CASImpl c1, CASImpl c2) {
      this.c1 = c1;
      this.c2 = c2;
      ts1 = c1.getTypeSystemImpl();
      ts2 = c2.getTypeSystemImpl();
      c1HO = c1.getHeap();
      c2HO = c2.getHeap();
      // note: heap global var used in some subroutines
      //   may have changed since setup of this instance
      c1heap = c1HO.heap;
      c2heap = c2HO.heap;
      addr2seq1 = new Int2IntRBT(Math.max(1000, c1heap.length/100));
      addr2seq2 = new Int2IntRBT(Math.max(1000, c2heap.length/100));
    }
      
    public boolean compareCASes() {
      final int[] c1FoundFSs;
      final int[] c2FoundFSs;
      try {
        heapStart = 0;  // referenced by the following method
        ts = ts1;
        processIndexedFeatureStructures(c1, false);
        c1FoundFSs = foundFSsArray;
        boolean savedIsTypeMapping = isTypeMapping;
        // next because while traversing the c2, if we used type mapping to go from c1 to c2, 
        // c2 isn't aware of type mapping.
        // This assumes c1 is the "src" and c2 is the "target"
        isTypeMapping = false;
        ts = ts2;
        processIndexedFeatureStructures(c2, false);
        ts = null; // catch errors
        isTypeMapping = savedIsTypeMapping;
        c2FoundFSs = foundFSsArray;
      } catch (IOException e) {
        throw new RuntimeException(e);  // never happen
      }

      heap = c1heap;  // note: the processIndexedFeatureStructures call reset this to their cas parm's heap
      
      for (int i = 0; i < c1FoundFSs.length; i++) {
        final int v = c1FoundFSs[i];
//        System.out.format("compare 1: seq = %,d addr=%,d%n", i, v);
        addr2seq1.put(v, i);
      }
      for (int i = 0; i < c2FoundFSs.length; i++) {
        final int v = c2FoundFSs[i];
//        System.out.format("compare 2: seq = %,d addr=%,d%n", i, v);
        addr2seq2.put(v, i);
      }
            
//      initFsStartIndexesCompare();
      
      // Iterating over both CASes
      //   If c1 is past end, verify all c2 instances up to its end are not in c1
      //   If c2 is past end, verify all c1 instances up to its end are not in c1
      //   If c1's instance type exists in c2, compare & advance both iterators
      //   If c1's instance type doesn't exist in c2, advance c1 and continue
      //   If c2's instance type doesn't exist in c1, advance c2 and continue
      
      
//      final int endHeapSeqSrc = fsStartIndexes.getNbrOfItems();
//      c1heapIndex = 1;
//      c2heapIndex = 1;
//      boolean pastEnd1 = false;
//      boolean pastEnd2 = false;      
      
//      while (c1Iterator.isValid() && c2Iterator.isValid()) {
      int i1 = 0;
      int i2 = 0;
      while (i1 < c1FoundFSs.length && i2 < c2FoundFSs.length) {
        c1heapIndex = c1FoundFSs[i1];
        c2heapIndex = c2FoundFSs[i2];
        if (isTypeMapping) {
          final int tCode1_2 = typeMapper.mapTypeCodeSrc2Tgt(c1heap[c1heapIndex]);
          final int tCode2_1 = typeMapper.mapTypeCodeTgt2Src(c2heap[c2heapIndex]);
          if ((tCode1_2 != 0) && (tCode2_1 != 0)) {
            if (!compareFss()) {
              return false;
            }
            i1++;
            i2++;
            continue;
          }
          if ((tCode1_2 == 0) && (tCode2_1 == 0)) {
            i1++;
            i2++;
            continue;
          }
          if ((tCode1_2 == 0) && (tCode2_1 != 0)) {
            i1++;
            continue;
          }
          if ((tCode1_2 != 0) && (tCode2_1 == 0)) {
            i2++;
            continue;
          }
        } else {  // not type mapping
          if (!compareFss()) {
            return false;
          }
          i1++;
          i2++;
          continue;
        }
      }
      
      if (i1 >= c1FoundFSs.length && i2 >= c2FoundFSs.length) {
        return true;  // end, everything compared
      }
      if (isTypeMapping) {
        while (i1 < c1FoundFSs.length) {
          c1heapIndex = c1FoundFSs[i1];
          if (typeMapper.mapTypeCodeSrc2Tgt(c1heap[c1heapIndex]) != 0) {
            return false;  // have more FSs in c1 than in c2
          }
          i1++;
        }
        while (i2 < c2FoundFSs.length) {
          c2heapIndex = c2FoundFSs[i2];
          if (typeMapper.mapTypeCodeTgt2Src(c2heap[c2heapIndex]) != 0) {
            return false;  // have more FSs in c2 than in c1
          }
          i2++;
        }
      }
      return true;
    }

    private boolean compareFss() {
      int tCode = c1heap[c1heapIndex];
      typeInfo = ts1.getTypeInfo(tCode);
      final int tCodeTgt = c2heap[c2heapIndex];
      int tCodeTgtInSrc = isTypeMapping ? typeMapper.mapTypeCodeTgt2Src(tCodeTgt) : tCodeTgt; 
      if (tCode != tCodeTgtInSrc) {  
        return mismatchFs();   // types mismatch
      }
      if (typeInfo.isArray) {
        return compareFssArray();
      } else {
        if (isTypeMapping) {
          final int[] srcSlots = typeMapper.getTgtFeatOffsets2Src(tCode);
          final int len = srcSlots.length;
          for (int i = 0; i < len; i++) {
            if (!compareSlot(srcSlots[i] + 1, i + 1)) {
              return mismatchFs(srcSlots[i], i);
            }
          }
        } else {
          for (int i = 1; i < typeInfo.slotKinds.length + 1; i++) {
            if (!compareSlot(i, i)) {
              return mismatchFs();
            }
          }
        }
        return true;
      }
    }
      
    private boolean compareFssArray() {
      int len1 = c1heap[c1heapIndex + 1];
      int len2 = c2heap[c2heapIndex + 1];
      if (len1 != len2) {
        return mismatchFs();
      }
      for (int i = 0; i < len1; i++) {
        SlotKind kind = typeInfo.getSlotKind(2);
        if (typeInfo.isHeapStoredArray) {
          if (kind == Slot_StrRef) {
            if (! compareStrings(c1.getStringForCode(c1heap[c1heapIndex + 2 + i]),
                                 c2.getStringForCode(c2heap[c2heapIndex + 2 + i]))) {
              return mismatchFs();
            }
          } else if (kind == Slot_HeapRef) {
            final int c1ref = c1heap[c1heapIndex + 2 + i];
            final int c2ref = c2heap[c2heapIndex + 2 + i];
            if (!isInstanceInTgtTs(c1ref)) {
              // source ref is for type not in target.  Target value should be 0
              return (c2ref == 0);
            }
            if ((c1ref != 0) && 
                (c2ref != 0) && 
                (addr2seq1.getMostlyClose(c1ref) != addr2seq2.getMostlyClose(c2ref))) {
              return mismatchFs();
            }
          } else if (c1heap[c1heapIndex + 2 + i] != c2heap[c2heapIndex + 2 + i]) {
            return mismatchFs();
          }
        } else {  // not heap stored array
          switch (kind) {
          case Slot_BooleanRef: case Slot_ByteRef:
            if (c1.getByteHeap().getHeapValue(c1heap[c1heapIndex + 2] + i) !=
                c2.getByteHeap().getHeapValue(c2heap[c2heapIndex + 2] + i)) {
              return mismatchFs(); 
            }
            break;
          case Slot_ShortRef:
            if (c1.getShortHeap().getHeapValue(c1heap[c1heapIndex + 2] + i) !=
                c2.getShortHeap().getHeapValue(c2heap[c2heapIndex + 2] + i)) {
              return mismatchFs();
            }
            break;
          case Slot_LongRef: case Slot_DoubleRef: {
            if (c1.getLongHeap().getHeapValue(c1heap[c1heapIndex + 2] + i)  !=
                c2.getLongHeap().getHeapValue(c2heap[c2heapIndex + 2] + i)) {
              return mismatchFs();
            }
            break;
          }
          default: throw new RuntimeException("internal error");
          }
        }
      } // end of for
      return true;
    }
    
    private boolean compareSlot(int offsetSrc, int offsetTgt) {
      SlotKind kind = typeInfo.getSlotKind(offsetSrc);
      switch (kind) {
      case Slot_Int: case Slot_Short: case Slot_Boolean: case Slot_Byte: 
      case Slot_Float: 
        return c1heap[c1heapIndex + offsetSrc] == c2heap[c2heapIndex + offsetTgt];
      case Slot_HeapRef: {
        final int c1ref = c1heap[c1heapIndex + offsetSrc];
        final int c2ref = c2heap[c2heapIndex + offsetTgt];
        return diagnoseMiscompareHeapRef(c1ref, c2ref, offsetSrc);
//        if (!isInstanceInTgtTs(c1ref)) {
//          // source ref is for type not in target.  Target value should be 0
//          return (c2ref == 0);
//        }
//        return ((c1ref == 0) && (c2ref == 0)) ||
//               ((c1ref != 0) && (c2ref != 0) && 
//                (addr2seq1.get(c1ref) == addr2seq2.get(c2ref)));
      }
      case Slot_StrRef:
        return compareStrings(c1.getStringForCode(c1heap[c1heapIndex + offsetSrc]),
                              c2.getStringForCode(c2heap[c2heapIndex + offsetTgt]));
      case Slot_LongRef: case Slot_DoubleRef:
        return c1.getLongHeap().getHeapValue(c1heap[c1heapIndex + offsetSrc]) ==
               c2.getLongHeap().getHeapValue(c2heap[c2heapIndex + offsetTgt]);
      default: throw new RuntimeException("internal error");      
      }
    }
    
    // debug
    
    private boolean diagnoseMiscompareHeapRef(int c1ref, int c2ref, int offsetSrc) {
      if (!isInstanceInTgtTs(c1ref)) {
        // source ref is for type not in target.  Target value should be 0
        if (c2ref != 0) {
          System.err.format("HeapRef original %,d is for a type not in target, target should have 0 but has %,d%n", c1ref, c2ref);
          return false;
        }
        return true;
      }
      if (c1ref == 0) {
        final int prevC1Ref = c1heap[c1heapIndex + offsetSrc];
        if (prevC1Ref != 0){
          System.err.format("HeapRef original c1Ref = %,d but instance not in target ts, so set to 0", prevC1Ref);
          return false;
        }
        return true;
      }
      // c1ref != 0 at this point
      if (c2ref == 0) {
        System.err.format("heapRef one is 0, other not: c1Ref = %,d c2Ref = %,d%n", c1ref, c2ref);
        return false;
      }
    
      final int seq1 = addr2seq1.getMostlyClose(c1ref);
      final int seq2 = addr2seq2.getMostlyClose(c2ref);
      
      if (seq1 != seq2) {
        System.err.format("heapRef seq1 not match seq2.  c1ref = %,d seq1 = %,d   c2ref= %,d seq2 = %,d%n", c1ref, seq1, c2ref, seq2);
        return false;
      }
      return true;
    }
    
    private boolean compareStrings(String s1, String s2) {
      if ((null == s1) && (null == s2)) {
        return true;
      }
      if (null == s1) {
        return false;
      }
      return s1.equals(s2);
    }
     
    
//    private int skipOverTgtFSsNotInSrc(
//        int[] heap, int heapEnd, int nextFsIndex, CasTypeSystemMapper typeMapper) {
//      final TypeSystemImpl ts = typeMapper.tsTgt;
//      for (; nextFsIndex < heapEnd;) {
//        final int tCode = heap[nextFsIndex];
//        if (typeMapper.mapTypeCodeTgt2Src(tCode) != 0) { 
//          break;
//        }
//        nextFsIndex += incrToNextFs(heap, nextFsIndex, ts.getTypeInfo(tCode));
//      }
//      return nextFsIndex;
//    }
//    
//    public void initFsStartIndexesCompare () {
//
//      int iTgtHeap = isTypeMapping ? skipOverTgtFSsNotInSrc(c2heap, c2end, 1, typeMapper) : 1;
//      
//      
//      for (int iSrcHeap = 1; iSrcHeap < c1end;) {
//        final int tCode = c1heap[iSrcHeap];
//        final int tgtTypeCode = isTypeMapping ? typeMapper.mapTypeCodeSrc2Tgt(tCode) : tCode;
//        final boolean isIncludedType = (tgtTypeCode != 0);
//        
//        // record info for type
//        fsStartIndexes.addItemAddr(iSrcHeap, iTgtHeap, isIncludedType);  // maps src heap to tgt seq
//        
//        // for features in type - 
//        //    strings: accumulate those strings that are in the target, if optimizeStrings != null
//        //      strings either in array, or in individual values
//        //    byte (array), short (array), long/double (instance or array): record if entries in aux array are skipped
//        //      (not in the target).  Note the recording will be in a non-ordered manner (due to possible updates by
//        //       previous delta deserialization)
//        final TypeInfo srcTypeInfo = ts1.getTypeInfo(tCode);
//        final TypeInfo tgtTypeInfo = (isTypeMapping && isIncludedType) ? ts2.getTypeInfo(tgtTypeCode) : srcTypeInfo;
//              
//        // Advance to next Feature Structure, in both source and target heap frame of reference
//        if (isIncludedType) {
//          final int deltaTgtHeap = incrToNextFs(c1heap, iSrcHeap, tgtTypeInfo);
//          iTgtHeap += deltaTgtHeap;
//          if (isTypeMapping) {
//            iTgtHeap = skipOverTgtFSsNotInSrc(c2heap, c2end, iTgtHeap, typeMapper);
//          }
//        }
//        iSrcHeap += incrToNextFs(c1heap, iSrcHeap, srcTypeInfo);
//      }
//    } 

    private boolean mismatchFs() {
      System.err.format("Mismatched Feature Structures:%n %s%n %s%n", 
          dumpHeapFs(c1, c1heapIndex, ts1), dumpHeapFs(c2, c2heapIndex, ts2));
      return false;
    }

    private boolean mismatchFs(int i1, int i2) {
      System.err.format("Mismatched Feature Structures in srcSlot %d, tgtSlot %d%n %s%n %s%n", 
          i1, i2, dumpHeapFs(c1, c1heapIndex, ts1), dumpHeapFs(c2, c2heapIndex, ts2));
      return false;
    }

    private StringBuilder dumpHeapFs(CASImpl cas, final int iHeap, final TypeSystemImpl ts) {
      StringBuilder sb = new StringBuilder();
      typeInfo = ts.getTypeInfo(cas.getHeap().heap[iHeap]);
      sb.append("Heap Addr: ").append(iHeap).append(' ');
      sb.append(typeInfo).append(' ');
  
      if (typeInfo.isHeapStoredArray) {
        sb.append(dumpHeapStoredArray(cas, iHeap));
      } else if (typeInfo.isArray) {
        sb.append(dumpNonHeapStoredArray(cas, iHeap));
      } else {
        sb.append("   Slots:\n");
        for (int i = 1; i < typeInfo.slotKinds.length + 1; i++) {
          sb.append("  ").append(typeInfo.getSlotKind(i)).append(": ")
              .append(dumpByKind(cas, i, iHeap)).append('\n');
        }
      }
      return sb;
    }
    
    private StringBuilder dumpHeapStoredArray(CASImpl cas, final int iHeap) {
      StringBuilder sb = new StringBuilder();
      int[] heap = cas.getHeap().heap;
      final int length = heap[iHeap + 1];
      sb.append("Array Length: ").append(length).append('[');
      SlotKind arrayElementKind = typeInfo.slotKinds[1];
      switch (arrayElementKind) {
      case Slot_HeapRef: case Slot_Int: case Slot_Short: case Slot_Byte: 
      case Slot_Boolean: case Slot_Float:
        for (int i = iHeap + 2; i < iHeap + length + 2; i++) {
          if (i > iHeap + 2) {
            sb.append(", ");
          }
          sb.append(heap[i]);
        }
        break;   
      case Slot_StrRef:
        StringHeap sh = cas.getStringHeap();
        for (int i = iHeap + 2; i < iHeap + length + 2; i++) {
          if (i > iHeap + 2) {
            sb.append(", ");
          }
          sb.append(sh.getStringForCode(heap[i]));        
        }
        break;
      default: throw new RuntimeException("internal error");
      }
      sb.append("] ");
      return sb;
    }
  
    private StringBuilder dumpNonHeapStoredArray(CASImpl cas, final int iHeap) {
      StringBuilder sb = new StringBuilder();
      int[] heap = cas.getHeap().heap;
      final int length = heap[iHeap + 1];
      sb.append("Array Length: ").append(length).append('[');
      SlotKind arrayElementKind = typeInfo.slotKinds[1];
      
      for (int i = 0; i < length; i++) {
        if (i > 0) {
          sb.append(", ");
        }
        switch (arrayElementKind) {
        case Slot_BooleanRef: case Slot_ByteRef:
          sb.append(cas.getByteHeap().getHeapValue(heap[iHeap + 2 + i]));
          break;
        case Slot_ShortRef:
          sb.append(cas.getShortHeap().getHeapValue(heap[iHeap + 2 + i]));
          break;
        case Slot_LongRef: case Slot_DoubleRef: {
          long v = cas.getLongHeap().getHeapValue(heap[iHeap + 2 + i]);
          if (arrayElementKind == Slot_DoubleRef) {
            sb.append(Double.longBitsToDouble(v));
          } else {
            sb.append(String.format("%,d", v));
          }
          break;
        }
        default: throw new RuntimeException("internal error");
        }
      }
      sb.append("] ");
      return sb;      
    }
  
    private StringBuilder dumpByKind(CASImpl cas, int offset, final int iHeap) {
      StringBuilder sb = new StringBuilder();
      int[] heap = cas.getHeap().heap;
      SlotKind kind = typeInfo.getSlotKind(offset);
      switch (kind) {
      case Slot_Int:
        return sb.append(heap[iHeap + offset]);
      case Slot_Short: 
        return sb.append((short)heap[iHeap + offset]);
      case Slot_Byte: 
        return sb.append((byte)heap[iHeap + offset]);
      case Slot_Boolean:  
        return sb.append(((heap[iHeap + offset]) == 0) ? false : true);
      case Slot_Float: {
        int v = heap[iHeap + offset];
        return sb.append(Float.intBitsToFloat(v)).append(' ').append(Integer.toHexString(v));
      }
      case Slot_HeapRef:
        return sb.append("HeapRef[").append(heap[iHeap + offset]).append(']');
      case Slot_StrRef:
        return sb.append(cas.getStringForCode(heap[iHeap + offset]));
      case Slot_LongRef:
        return sb.append(String.format("%,d", cas.getLongHeap().getHeapValue(heap[iHeap + offset])));
      case Slot_DoubleRef: {
        long v = cas.getLongHeap().getHeapValue(heap[iHeap + offset]);
        return sb.append(Double.longBitsToDouble(v)).append(' ').append(Long.toHexString(v));
      }
      default: throw new RuntimeException("internal error");      
      }
    }
  }
  
  /**
   * 
   * @param f can be a DataOutputStream,
   *                 an OutputStream
   *                 a File
   * @return a data output stream
   * @throws FileNotFoundException passthru
   */
  private static DataOutputStream makeDataOutputStream(Object f) throws FileNotFoundException {
    if (f instanceof DataOutputStream) {
      return (DataOutputStream)f;
    }
    if (f instanceof OutputStream) {
      return new DataOutputStream((OutputStream)f);
    }
    if (f instanceof File) {
      FileOutputStream fos = new FileOutputStream((File)f);
      BufferedOutputStream bos = new BufferedOutputStream(fos);
      return new DataOutputStream(bos); 
    }
    throw new RuntimeException(String.format("Invalid class passed to method, class was %s", f.getClass().getName()));
  }
  
  // for debugging
  String printCasInfo(CASImpl cas) {
    int heapsz= cas.getHeap().getNextId() * 4;
    StringHeapDeserializationHelper shdh = cas.getStringHeap().serialize();
    
    int charssz = shdh.charHeap.length  * 2;
    int strintsz = cas.getStringHeap().getSize() * 8;
    int strsz = charssz + strintsz;
    int fsindexessz = cas.getIndexedFSs().length * 4;
    int bytessz = cas.getByteHeap().getSize();
    int shortsz = cas.getShortHeap().getSize() * 2;
    int longsz = cas.getLongHeap().getSize() * 8;
    int total = heapsz + strsz + fsindexessz + bytessz + shortsz + longsz;
    return String.format("CAS info before compression: totalSize(bytes): %,d%n" +
        "  mainHeap: %,d(%d%%)%n" +
        "  Strings: [%,d(%d%%): %,d chars %,d ints]%n" +
        "  fsIndexes: %,d(%d%%)%n" +
        "  byte/short/long Heaps: [%,d %,d %,d]",
      total, 
      heapsz, (100L*heapsz)/total, 
      strsz, (100L*strsz)/ total,
      charssz, strintsz,
      fsindexessz, (100L*fsindexessz) / total,
      bytessz, shortsz, longsz
        );     
  }

  /********************************************
   * Set up Streams
   * @throws FileNotFoundException passthru
   ********************************************/
  private void setupOutputStreams(Object out) throws FileNotFoundException { 
    serializedOut = makeDataOutputStream(out);

    // estimate model:
    //   33% of space in strings, 33% in heap, 33% other
    //   compr ratio for heap is 98%
    int compr = (heapEnd - heapStart) * 8 / 3 / 50;
    int compr1000 = Math.max(512, compr/1000);
    // 2nd arg is the number of bytes in the byte output stream, initially
    estimatedZipSize[typeCode_i] = Math.max(512, compr/4);  // /4 for ~4 slots per fs 
//      estimatedZipSize[boolean_i] =compr1000;
    estimatedZipSize[byte_i] = compr1000;
    estimatedZipSize[short_i] = compr1000;
    estimatedZipSize[int_i] = Math.max(1024, compr1000);
    estimatedZipSize[arrayLength_i] = compr1000;
    estimatedZipSize[float_Mantissa_Sign_i] = compr1000;
    estimatedZipSize[float_Exponent_i] = compr1000;
    estimatedZipSize[double_Mantissa_Sign_i] = compr1000;
    estimatedZipSize[double_Exponent_i] = compr1000;
    estimatedZipSize[long_High_i] = compr1000;
    estimatedZipSize[long_Low_i] = compr1000;
    estimatedZipSize[heapRef_i] = Math.max(1024, compr1000);
    estimatedZipSize[strOffset_i] = Math.max(512, compr/4);
    estimatedZipSize[strLength_i] = Math.max(512, compr/4);
    estimatedZipSize[fsIndexes_i] = Math.max(512, compr/8);  // /4 for ~4 slots/fs, / 2 for # indexed
    estimatedZipSize[strChars_i] = Math.max(512, compr/4); // strings compress better
    estimatedZipSize[control_i] = 128;
    
    for (int i = 0; i < baosZipSources.length; i++) {
      setupOutputStream(i);
    }
    // below must follow the setupOutputStream calls above
    
//    arrayLength_dos = dosZipSources[arrayLength_i];
//    heapRef_dos = dosZipSources[heapRef_i];
//    int_dos = dosZipSources[int_i];
    byte_dos = dosZipSources[byte_i];
//    short_dos = dosZipSources[short_i];
    typeCode_dos = dosZipSources[typeCode_i];
    strOffset_dos = dosZipSources[strOffset_i];
    strLength_dos = dosZipSources[strLength_i];
//    long_High_dos = dosZipSources[long_High_i];
//    long_Low_dos = dosZipSources[long_Low_i];
    float_Mantissa_Sign_dos = dosZipSources[float_Mantissa_Sign_i];
    float_Exponent_dos = dosZipSources[float_Exponent_i];
    double_Mantissa_Sign_dos = dosZipSources[double_Mantissa_Sign_i];
    double_Exponent_dos = dosZipSources[double_Exponent_i];
    fsIndexes_dos = dosZipSources[fsIndexes_i];
//    strChars_dos = dosZipSources[strChars_i];
    control_dos = dosZipSources[control_i];
    strSeg_dos = dosZipSources[strSeg_i];
  } 
  
  private DataOutputStream setupOutputStream(int i) {
    // set up output stream
    int size = estimatedZipSize[i];
    baosZipSources[i] = new ByteArrayOutputStream(size);
    return dosZipSources[i] = new DataOutputStream(baosZipSources[i]); 
  }

  private void setupReadStreams() throws IOException {
    /************************************************
     * Setup all the input streams with inflaters
     ************************************************/
    final int nbrEntries = deserIn.readInt();  // number of compressed streams    
    IntVector idxAndLen = new IntVector(nbrEntries * 3);
    for (int i = 0; i < nbrEntries; i++) {
      idxAndLen.add(deserIn.readUnsignedByte());  // slot ordinal number
      idxAndLen.add(deserIn.readInt());           // compressed size, bytes
      idxAndLen.add(deserIn.readInt());           // decompressed size, bytes (not currently used)
    }
    
    for (int i = 0; i < idxAndLen.size();) {
      setupReadStream(idxAndLen.get(i++), idxAndLen.get(i++), idxAndLen.get(i++));
    }

    arrayLength_dis = dataInputs[arrayLength_i];
    heapRef_dis = dataInputs[heapRef_i];
    int_dis = dataInputs[int_i];
    byte_dis = dataInputs[byte_i];
    short_dis = dataInputs[short_i];
    typeCode_dis = dataInputs[typeCode_i];
    strOffset_dis = dataInputs[strOffset_i];
    strLength_dis = dataInputs[strLength_i];
    long_High_dis = dataInputs[long_High_i];
    long_Low_dis = dataInputs[long_Low_i];
    float_Mantissa_Sign_dis = dataInputs[float_Mantissa_Sign_i];
    float_Exponent_dis = dataInputs[float_Exponent_i];
    double_Mantissa_Sign_dis = dataInputs[double_Mantissa_Sign_i];
    double_Exponent_dis = dataInputs[double_Exponent_i];
    fsIndexes_dis = dataInputs[fsIndexes_i];
    strChars_dis = dataInputs[strChars_i];
   
    control_dis = dataInputs[control_i];
    strSeg_dis = dataInputs[strSeg_i];
  }
  
  private void setupReadStream(
      int slotIndex, 
      int bytesCompr,
      int bytesOrig) throws IOException {
    byte[] b = new byte[bytesCompr + 1];
    deserIn.readFully(b, 0, bytesCompr);  // this leaves 1 extra 0 byte at the end
    // which may be required by Inflater with nowrap option - see Inflater javadoc
    
    // testing inflate speed
//      long startTime = System.currentTimeMillis();
//      inflater.reset();
//      inflater.setInput(b);
//      byte[] uncompressed = new byte[bytesOrig];
//      int uncompressedLength = 0;
//      try {
//        uncompressedLength = inflater.inflate(uncompressed);
//      } catch (DataFormatException e) {
//        throw new RuntimeException(e);
//      }
//      if (uncompressedLength != bytesOrig) {
//        throw new RuntimeException();
//      }
//      System.out.format("Decompress %s took %,d ms%n", 
//          SlotKind.values()[slotIndex], System.currentTimeMillis() - startTime); 
//      
//      dataInputs[slotIndex] = new DataInputStream(new ByteArrayInputStream(uncompressed));
    Inflater inflater = new Inflater(true);
    inflaters[slotIndex] = inflater;  // save to be able to call end() when done. 
    ByteArrayInputStream baiStream = new ByteArrayInputStream(b);      
    int zipBufSize = Math.max(1 << 10, bytesCompr); // 32768 == 1<< 15.  Tuned by trials on 2015 intel i7
     // caches: L1 = 128KB    L2 = 1M     L3 = 6M
     // increasing the max causes cache dumping on this machine, and things slow down
    InflaterInputStream iis = new InflaterInputStream(baiStream, inflater, zipBufSize);
    // increasing the following buffer stream buffer size also seems to slow things down
    dataInputs[slotIndex] = new DataInputStream(new BufferedInputStream(iis, zipBufSize * 1 ));
  }
  
  private void closeDataInputs() {
    for (DataInputStream is : dataInputs) {
      if (null != is){
        try {
          is.close();
        } catch (IOException e) {
        }
      }
    }
    // release any space inflater holding on to
    for (Inflater inflater : inflaters) {
      if (null != inflater) {
        inflater.end();
      }
    }
  }
  
  private Header readHeader(InputStream istream) throws IOException {
    deserIn = CommonSerDes.maybeWrapToDataInputStream(istream);
    Header h = CommonSerDes.readHeader(deserIn);
    if (!h.isCompressed) {
      throw new RuntimeException(
          "non-compressed invalid object passed to BinaryCasSerDes6 deserialize");
    }
    
    if (!h.form6) {
      throw new RuntimeException(String.format("Wrong version: %x in input source passed to BinaryCasSerDes6 for deserialization", h.v));
    }
    
    isReadingDelta = h.isDelta;
    return h;
  }
  
  /* *******************************************
   * String info
   *********************************************/
  private void writeStringInfo() throws IOException {
    String [] commonStrings = os.getCommonStrings();
    writeVnumber(strChars_i, commonStrings.length);
    for (int i = 0; i < commonStrings.length; i++) {
      int startPos = dosZipSources[strChars_i].size();
      DataIO.writeUTFv(commonStrings[i], dosZipSources[strChars_i]);
      // approximate histogram
      if (doMeasurements) {
        // len is utf-8 encoding
        float len = dosZipSources[strChars_i].size() - startPos;
        // if len == chars, then all got coded as 1 byte
        // if len > chars, some were utf-8 coded as 2 bytes
        float excess = (len / commonStrings[i].length()) - 1;  // excess over length 1
        int encAs2 = (int)(excess * commonStrings[i].length());
        
        // simulate histo for all the chars, as 1 or 2 byte UTF8 encoding 
        sm.statDetails[strChars_i].countTotal += commonStrings[i].length(); // total chars accum
        sm.statDetails[strChars_i].c[0] = commonStrings[i].length() - encAs2;
        sm.statDetails[strChars_i].c[1] = encAs2;
        sm.statDetails[strChars_i].lengthTotal += len;  // total as UTF-8 encode
      }
    }
    
    only1CommonString = commonStrings.length == 1;

    if (doMeasurements) {
      long commonStringsLength = 0;
      sm.stringsNbrCommon = commonStrings.length;
      int r = 0;
      for (int i = 0; i < commonStrings.length; i++) {
        r += DataIO.lengthUTFv(commonStrings[i]);
        commonStringsLength += commonStrings[i].length();
      }
      sm.stringsCommonChars = r;

      sm.stringsSavedExact = os.getSavedCharsExact() * 2;
      sm.stringsSavedSubstr = os.getSavedCharsSubstr() * 2;
      sm.statDetails[strChars_i].original = os.getSavedCharsExact() * 2
                               + os.getSavedCharsSubstr() * 2
                               + commonStringsLength * 2;
      final int stringHeapStart = isSerializingDelta ? mark.nextStringHeapAddr : 1;
      final int stringHeapEnd = stringHeapObj.getSize();
      sm.statDetails[strLength_i].original = (stringHeapEnd - stringHeapStart) * 4;
      sm.statDetails[strOffset_i].original = (stringHeapEnd - stringHeapStart) * 4;
    }

  }
  
  private int[] toArrayOrINT0(IntVector v) {
    if (null ==  v) {
      return INT0;
    }
    return v.toArray();
  }

  /**
   * @return the tgtTs
   */
  TypeSystemImpl getTgtTs() {
    return tgtTs;
  }
}
