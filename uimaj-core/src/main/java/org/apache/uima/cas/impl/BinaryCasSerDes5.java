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

import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.NBR_SLOT_KIND_ZIP_STREAMS;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_ArrayLength;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_BooleanRef;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_Byte;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_ByteRef;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_Control;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_DoubleRef;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_Double_Exponent;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_Double_Mantissa_Sign;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_Float_Exponent;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_Float_Mantissa_Sign;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_FsIndexes;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_HeapRef;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_Int;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_LongRef;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_Long_High;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_Long_Low;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_MainHeap;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_Short;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_ShortRef;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_StrChars;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_StrLength;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_StrOffset;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_StrRef;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_StrSeg;
import static org.apache.uima.cas.impl.BinaryCasSerDes5.SlotKind.Slot_TypeCode;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Marker;
import org.apache.uima.cas.impl.TypeSystemImpl.TypeInfo;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.impl.DataIO;
import org.apache.uima.util.impl.OptimizeStrings;
import org.apache.uima.util.impl.SerializationMeasures;

/**
 * User callable serialization and deserialization of the CAS in a compressed Binary Format
 * 
 * This serializes/deserializes the state of the CAS, assuming that the type
 * information remains constant.
 * 
 * Header specifies to reader the format, and the compression level.
 * 
 * How to Serialize:  
 * 
 * 1) create an instance of this class, specifying some options that don't change very much
 * 2) call serialize(CAS) to serialize the cas * 
 * 
 * You can reuse the instance for a different CAS (as long as the type system is the same);
 * this will save setup time.
 * 
 * This class lazily constructs customized TypeInfo instances for each type encountered in serializing.
 * These are preserved across multiple serialization calls, so their setup / initialization is only
 * needed the first time.
 * 
 * The form of the binary CAS is inserted at the beginning so that receivers can do the
 * proper deserialization.  
 *     
 * Binary format requires that the exact same type system be used when deserializing 
 * 
 * How to Deserialize:
 * 
 * 1) get an appropriate CAS to deserialize into.  For delta CAS, it does not have to be empty.
 * 2) call CASImpl: cas.reinit(inputStream)  This is the existing method
 *    for binary deserialization, and it now handles this compressed version, too.
 *    Delta cas is also supported.
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
 *   create separate ByteArrayInputStreams for each segment, sharing byte bfr
 *   create appropriate unzip data input streams for these
 *   
 * API design
 *   Slow but expensive data: 
 *     extra type system info - lazily created and added to shared TypeSystemImpl object
 *       set up per type actually referenced
 *     mapper for type system - lazily created and added to shared TypeSystemImpl object
 *       in identity-map cache (size limit = 10?) - key is target typesystemimpl.
 *   Defaulting:
 *     flags:  doMeasurement, compressLevel, CompressStrategy
 *     Defaulting set in call to create instance of this class
 *   Per serialize call: cas, output, [target ts], [mark for delta]
 *   Per deserialize call: cas, input, [target ts]
 *   
 *   CASImpl has instance method with defaulting args for serialization.
 *   CASImpl has reinit which works with compressed binary serialization objects
 *     if no type mapping
 *     If type mapping, (new BinaryCasSerDes4(sourceTypeSystem)).deserialize(in-steam, [targetTypeSystem])
 *     
 * Use Cases, filtering and delta
 *   **************************************************************************
 *   * (de)serialize * filter? * delta? * comment
 *   **************************************************************************
 *   * serialize     *   N     *   N    * Saving a Cas, 
 *   *               *         *        * sending Cas to svc with identical ts
 *   **************************************************************************
 *   * serialize     *   Y     *   N    * sending Cas to svc with 
 *   *               *         *        * different ts (a subset)
 *   **************************************************************************
 *   * serialize     *   N     *   Y    * returning Cas to client
 *   *               *         *        * (?? saving just a delta to disk??)
 *   **************************************************************************
 *   * serialize     *   Y     *   Y    * NOT SUPPORTED (not needed)  
 *   **************************************************************************
 *   * deserialize   *   N     *   N    * reading/(recv) CAS, identical TS
 *   **************************************************************************
 *   * deserialize   *   Y     *   N    * reading/(recv) CAS, different TS
 *   **************************************************************************
 *   * deserialize   *   N     *   Y    * recv CAS, identical TS, 
 *   **************************************************************************
 *   * deserialize   *   Y     *   Y    * recv CAS, different TS (tgt a subset)
 *   **************************************************************************
 */
public class BinaryCasSerDes5 {

  /**
   * Version of the serializer, used to allow deserialization of 
   * older versions
   * 
   * Version 0 - initial SVN checkin
   * Version 1 - changes to support CasTypeSystemMapper 
   */
  private static final int VERSION = 1;  

  // next must be set to true if you want the cas type system mapping to work
  public static final boolean CHANGE_FS_REFS_TO_SEQUENTIAL = true;
  // may add more later - to specify differing trade-offs between speed and compression
  public static final boolean IS_DIFF_ENCODE = true;
  public static final boolean CAN_BE_NEGATIVE = true;
  public static final boolean IGNORED = true;
  public static final boolean IN_MAIN_HEAP = true;
  
  private static final long DBL_1 = Double.doubleToLongBits(1D);

  /**
   * Aux heaps 
   */
  public enum AuxHeap {ByteAH, ShortAH, LongAH,};
  final static int AuxHeapsCount = AuxHeap.values().length;

  public static class AuxSkip implements Comparable<AuxSkip> {
    final int skipIndex;
    final int skipSize;
    public AuxSkip(int index, int size) {
      skipIndex = index;
      skipSize = size;
    }
    public int compareTo(AuxSkip o) {
      return (skipIndex < o.skipIndex) ? -1 : (skipIndex > o.skipIndex) ? 1 : 0; 
    }
  }

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
   * The kinds of slots that can exist 
   *   an index for getting type-code specific values, 
   *   flag - whether or not they should be diff encoded
   *   flag - if they can be negative (and need their sign moved)
   *   
   * Some are real slots in the heap; others are descriptions of
   *   parts of values, eg. float exponent
   *   
   * Difference encoding costs 1 bit.  
   *   Measurements show it can lessen zip's effectiveness 
   *     (especially for single byte values (?)),
   *     probably because it causes more dispersion in 
   *     the value kinds.  
   *   Because of this 2-fold cost (1 bit and less zip),
   *     differencing being tried only for multi-byte 
   *     values (short, int, long), and heap refs
   *     - for array values, diff is with prev array value
   *       (for 1st value in array, diff is with prev FeatureStructure
   *       of the same type in the heap's 1st value if it exists
   *     - for non-array values or 1st array value, diff is with
   *       prev heap value for same type in heap  
   * 
   *   Not done for float parts - exponent too short, and
   *     mantissa too random.
   * 
   * CanBeNegative
   *   Many values are only positive e.g., array lengths
   *   Some values can be negative
   *     (all difference-encoded things can be negative)
   *   Represent as 1 bit + positive number, sign bit in 
   *     least sig. bit position.  This allows the
   *     bits to cluster closer to 0 on the positive side,
   *     which can make for fewer bytes to represent the number.
   */

  /**
   * Define all the slot kinds.
   */
  public enum SlotKind {
    Slot_ArrayLength(! IS_DIFF_ENCODE, ! CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
    Slot_HeapRef(    IS_DIFF_ENCODE,             IGNORED, 4, IN_MAIN_HEAP),
    Slot_Int(        IS_DIFF_ENCODE,             IGNORED, 4, IN_MAIN_HEAP),
    Slot_Byte(       ! IS_DIFF_ENCODE, ! CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
    Slot_Short(      IS_DIFF_ENCODE,             IGNORED, 4, IN_MAIN_HEAP),
    Slot_TypeCode(   ! IS_DIFF_ENCODE, ! CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),

    Slot_StrOffset(  ! IS_DIFF_ENCODE, ! CAN_BE_NEGATIVE, 4, !IN_MAIN_HEAP),
    Slot_StrLength(  ! IS_DIFF_ENCODE, ! CAN_BE_NEGATIVE, 4, !IN_MAIN_HEAP),
    Slot_Long_High(    IS_DIFF_ENCODE,           IGNORED, 0, !IN_MAIN_HEAP),
    Slot_Long_Low (    IS_DIFF_ENCODE,           IGNORED, 0, !IN_MAIN_HEAP),

    // the next are not actual slot kinds, but instead
    // are codes used to control encoding of Floats and Doubles.
    Slot_Float_Mantissa_Sign( ! IS_DIFF_ENCODE, CAN_BE_NEGATIVE, 0, !IN_MAIN_HEAP),
    // exponent is 8 bits, and shifted in the expectation
    // that many values may be between 1 and 0 (e.g., normalized values)
    //   -- so sign moving is needed
    Slot_Float_Exponent(      ! IS_DIFF_ENCODE, CAN_BE_NEGATIVE, 0, !IN_MAIN_HEAP),
    
    Slot_Double_Mantissa_Sign(! IS_DIFF_ENCODE, CAN_BE_NEGATIVE, 0, !IN_MAIN_HEAP),
    Slot_Double_Exponent(     ! IS_DIFF_ENCODE, CAN_BE_NEGATIVE, 0, !IN_MAIN_HEAP),
    Slot_FsIndexes(             IS_DIFF_ENCODE,         IGNORED, 4, !IN_MAIN_HEAP),
    
    Slot_StrChars(            IGNORED,          IGNORED, 2, !IN_MAIN_HEAP),
    
    Slot_Control(             IGNORED,          IGNORED, 0, !IN_MAIN_HEAP),
    Slot_StrSeg(              ! IS_DIFF_ENCODE, ! CAN_BE_NEGATIVE, 0, ! IN_MAIN_HEAP),
    
    // the next slots are not serialized
    Slot_StrRef(     IS_DIFF_ENCODE,             IGNORED, 4, IN_MAIN_HEAP),
    Slot_BooleanRef( ! IS_DIFF_ENCODE, ! CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
    Slot_ByteRef(    IS_DIFF_ENCODE,             IGNORED, 4, IN_MAIN_HEAP),
    Slot_ShortRef(   IS_DIFF_ENCODE,             IGNORED, 4, IN_MAIN_HEAP),
    Slot_LongRef(    IS_DIFF_ENCODE,             IGNORED, 4, IN_MAIN_HEAP),
    Slot_DoubleRef(  IS_DIFF_ENCODE,             IGNORED, 4, IN_MAIN_HEAP),
    Slot_Float(      ! IS_DIFF_ENCODE, ! CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
    Slot_Boolean(    ! IS_DIFF_ENCODE, ! CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
    // next used to capture original heap size
    Slot_MainHeap(   IGNORED,          IGNORED,           4, !IN_MAIN_HEAP),

    ;

    public final boolean isDiffEncode;
    public final boolean canBeNegative;
    public final boolean inMainHeap;
    public final int elementSize;
    
    public static final int NBR_SLOT_KIND_ZIP_STREAMS;
    static {NBR_SLOT_KIND_ZIP_STREAMS = Slot_StrRef.ordinal();}
    
    SlotKind(boolean isDiffEncode, 
             boolean canBeNegative, 
             int elementSize,
             boolean inMainHeap) {
      this.isDiffEncode = isDiffEncode;
      this.canBeNegative = isDiffEncode ? true : canBeNegative;
      this.elementSize = elementSize; 
      this.inMainHeap = inMainHeap;
    }
  }
  
  private static AuxHeap getAuxHeapFromSlotKind(SlotKind k) {
    if ((k == Slot_ByteRef) || (k == Slot_BooleanRef)) {
      return AuxHeap.ByteAH;
    }
    if (k == Slot_ShortRef) {
      return AuxHeap.ShortAH;
    }
    if (k == Slot_LongRef || k == Slot_DoubleRef) {
      return AuxHeap.LongAH;
    }
    return null;
  }
  
  // speedups
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
   * Things set up for one instance of this class, and
   * reuse-able
   */
  final private TypeSystemImpl ts;
  final private boolean doMeasurements;
  final private CompressLevel compressLevel;
  final private CompressStrat compressStrategy;  
  
  /**
   * Things that are used by common routines among serialization and deserialization
   */
  private boolean isTypeMappingCmn;
  private CasTypeSystemMapper typeMapperCmn;
  /**
   * 
   * @param ts Type System (the source type system)
   * @param doMeasurements true if measurements should be collected
   * @param compressLevel 
   * @param compressStrategy
   */
  public BinaryCasSerDes5(TypeSystemImpl ts, boolean doMeasurements, 
      CompressLevel compressLevel, CompressStrat compressStrategy) {
    this.ts = ts;
    this.doMeasurements = doMeasurements;
    this.compressLevel = compressLevel;
    this.compressStrategy = compressStrategy;
  }
  
  public BinaryCasSerDes5(TypeSystemImpl ts) {
    this(ts, false, CompressLevel.Default, CompressStrat.Default);
  }

  /**
   * @param cas
   * @param out
   * @param trackingMark
   * @return null or serialization measurements (depending on setting of doMeasurements)
   * @throws IOException
   */
  public SerializationMeasures serialize(
      AbstractCas cas, 
      Object out, 
      Marker trackingMark,
      TypeSystemImpl tgtTs
      ) throws IOException {
    SerializationMeasures sm = (doMeasurements) ? new SerializationMeasures() : null;
    CASImpl casImpl = (CASImpl) ((cas instanceof JCas) ? ((JCas)cas).getCas(): cas);
    if (null != trackingMark && !trackingMark.isValid() ) {
      throw new CASRuntimeException(
                CASRuntimeException.INVALID_MARKER, new String[] { "Invalid Marker." });
    }
    
    Serializer serializer = new Serializer(
        casImpl, makeDataOutputStream(out), (MarkerImpl) trackingMark, tgtTs, sm);
   
    serializer.serialize();
    return sm;
  }

  public SerializationMeasures serialize(AbstractCas cas, Object out, TypeSystemImpl tgtTs) throws IOException {
    return serialize(cas, out, null, tgtTs);
  }
  
  public SerializationMeasures serialize(AbstractCas cas, Object out) throws IOException {
    return serialize(cas, out, null, null);
  }
  /**
   * Use to deserialize compressed file, no type system mapping
   * @param cas
   * @param istream
   * @throws IOException
   */
  public void deserialize(CASImpl cas, InputStream istream) throws IOException {
    deserialize(cas, istream, null);
  }
  /**
   * Use to deserialize compressed file, with type system mapping
   * @param cas
   * @param istream
   * @param tgtTs
   * @throws IOException
   */
  public void deserialize(CASImpl cas, InputStream istream, TypeSystemImpl tgtTs) throws IOException {
    final DataInputStream dis = (istream instanceof DataInputStream) ?  
        (DataInputStream) istream : new DataInputStream(istream);

     // key
     // determine if byte swap if needed based on key
     byte[] bytebuf = new byte[4];
     bytebuf[0] = dis.readByte(); // U
     bytebuf[1] = dis.readByte(); // I
     bytebuf[2] = dis.readByte(); // M
     bytebuf[3] = dis.readByte(); // A

     // version      
     // version bit in 2's place indicates this is in delta format.
     final int version = dis.readInt();      
     final boolean delta = ((version & 2) == 2);
     
     cas = cas.getBaseCAS();
     if (!delta) {
       cas.resetNoQuestions();
     }
     
     if (0 == (version & 4)) {
       throw new RuntimeException("non-compressed invalid object passed to BinaryCasSerDes4 deserialize");
     }
     deserialize(cas, istream, delta, tgtTs); 
  }
  /**
   * Called from CASImpl reinit if format of file is compressed
   *   No support for type system mapping
   * @param cas
   * @param deserIn
   * @param isDelta
   * @throws IOException
   */
  public void deserialize(
      CASImpl cas, 
      InputStream deserIn, 
      boolean isDelta
      ) throws IOException {
    deserialize(cas, deserIn, isDelta, null);
  }
  /**
   * Used when need to do type system mapping, and have already read header
   * @param cas
   * @param deserIn
   * @param isDelta
   * @param tgtTs
   * @throws IOException
   */
  private void deserialize(
      CASImpl cas, 
      InputStream deserIn, 
      boolean isDelta, 
      TypeSystemImpl tgtTs) throws IOException {
    DataInput in;
    if (deserIn instanceof DataInputStream) {
      in = (DataInputStream)deserIn;
    } else {
      in = new DataInputStream(deserIn);
    }
    Deserializer deserializer = new Deserializer(cas, in, isDelta, tgtTs);    
    deserializer.deserialize();
  }

  /**
   * Class instantiated once per serialization
   * Multiple serializations in parallel supported, with
   * multiple instances of this
   */
  
  private class Serializer {
    final private DataOutputStream serializedOut;  // where to write out the serialized result
    final private CASImpl cas;  // cas being serialized
    final private MarkerImpl mark;  // the mark to serialize from
    
    final private SerializationMeasures sm;  // null or serialization measurements
    final private ByteArrayOutputStream[] baosZipSources = new ByteArrayOutputStream[NBR_SLOT_KIND_ZIP_STREAMS];  // lazily created, indexed by SlotKind.i
    final private DataOutputStream[] dosZipSources = new DataOutputStream[NBR_SLOT_KIND_ZIP_STREAMS];      // lazily created, indexed by SlotKind.i

    final private int[] heap;           // main heap
    private int heapStart;
    final private int heapEnd;
    // heapEnd - heapStart, but with FS that don't exist in the target type system deleted
    private int totalMappedHeapSize = 0;    
    final private StringHeap stringHeapObj;
    final private LongHeap longHeapObj;
    final private ShortHeap shortHeapObj;
    final private ByteHeap byteHeapObj;

    final private boolean isDelta;        // if true, there is a marker indicating the start spot(s)
    final private boolean doMeasurement;  // if true, doing measurements
    final private ComprItemRefs fsStartIndexes = (CHANGE_FS_REFS_TO_SEQUENTIAL) ? new ComprItemRefs() : null;
    final private int[] typeCodeHisto = new int[ts.getTypeArraySize()]; 
//    final private Integer[] serializedTypeCode2Code = new Integer[ts.getTypeArraySize()]; // needs to be Integer to get comparator choice
    final private int[] estimatedZipSize = new int[NBR_SLOT_KIND_ZIP_STREAMS]; // one entry for each output stream kind
    final private OptimizeStrings os;

    // typeInfo is local to this serialization instance to permit multiple threads
    private TypeInfo typeInfo; // type info for the current type being serialized
    final private int[] iPrevHeapArray; // index of previous instance of this typecode in heap, by typecode
    private int iPrevHeap;        // 0 or heap addr of previous instance of current type
    private boolean only1CommonString;  // true if only one common string
    
    final private CasTypeSystemMapper typeMapper;
    final private boolean isTypeMapping;
    
    // speedups
    
    // any use of these means caller handles measurement
    // some of these are never used, because the current impl
    //   is using the _i form to get measurements done
    final private DataOutputStream arrayLength_dos;
    final private DataOutputStream heapRef_dos;
    final private DataOutputStream int_dos;
    final private DataOutputStream byte_dos;
    final private DataOutputStream short_dos;
    final private DataOutputStream typeCode_dos;
    final private DataOutputStream strOffset_dos;
    final private DataOutputStream strLength_dos;
    final private DataOutputStream long_High_dos;
    final private DataOutputStream long_Low_dos;
    final private DataOutputStream float_Mantissa_Sign_dos;
    final private DataOutputStream float_Exponent_dos;
    final private DataOutputStream double_Mantissa_Sign_dos;
    final private DataOutputStream double_Exponent_dos;
    final private DataOutputStream fsIndexes_dos;
    final private DataOutputStream strChars_dos;
    final private DataOutputStream control_dos;
    final private DataOutputStream strSeg_dos;

    private Serializer(
        CASImpl cas, 
        DataOutputStream serializedOut, 
        MarkerImpl mark,
        TypeSystemImpl tgtTs,
        SerializationMeasures sm) {
      this.cas = cas;
      this.serializedOut = serializedOut;
      this.mark = mark;
      this.sm = sm;
      isDelta = (mark != null);
      doMeasurement = (sm != null);
      typeMapperCmn = typeMapper = ts.getTypeSystemMapper(tgtTs);
      isTypeMappingCmn = isTypeMapping = (null != typeMapper);
      
      heap = cas.getHeap().heap;
      heapEnd = cas.getHeap().getCellsUsed();
      heapStart = isDelta ? mark.getNextFSId() : 0;
      
      stringHeapObj = cas.getStringHeap();
      longHeapObj   = cas.getLongHeap();
      shortHeapObj  = cas.getShortHeap();
      byteHeapObj   = cas.getByteHeap();
     
      os = new OptimizeStrings(doMeasurement);
      
      iPrevHeapArray = new int[ts.getTypeArraySize()];

      setupOutputStreams();   
      arrayLength_dos = dosZipSources[arrayLength_i];
      heapRef_dos = dosZipSources[heapRef_i];
      int_dos = dosZipSources[int_i];
      byte_dos = dosZipSources[byte_i];
      short_dos = dosZipSources[short_i];
      typeCode_dos = dosZipSources[typeCode_i];
      strOffset_dos = dosZipSources[strOffset_i];
      strLength_dos = dosZipSources[strLength_i];
      long_High_dos = dosZipSources[long_High_i];
      long_Low_dos = dosZipSources[long_Low_i];
      float_Mantissa_Sign_dos = dosZipSources[float_Mantissa_Sign_i];
      float_Exponent_dos = dosZipSources[float_Exponent_i];
      double_Mantissa_Sign_dos = dosZipSources[double_Mantissa_Sign_i];
      double_Exponent_dos = dosZipSources[double_Exponent_i];
      fsIndexes_dos = dosZipSources[fsIndexes_i];
      strChars_dos = dosZipSources[strChars_i];
      control_dos = dosZipSources[control_i];
      strSeg_dos = dosZipSources[strSeg_i];
    }
    
    private void setupOutputStreams() {
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

    }
    
    private void serialize() throws IOException {   

      if (doMeasurement) {
        System.out.println(printCasInfo(cas));
        sm.origAuxBytes = cas.getByteHeap().getSize();
        sm.origAuxShorts = cas.getShortHeap().getSize() * 2;
        sm.origAuxLongs = cas.getLongHeap().getSize() * 8;
        sm.totalTime = System.currentTimeMillis();
      }

      /******************
       * Process Header  
       * Standardized    
       ******************/
      // encode: bits 7 6 5 4 3 2 1 0
      //                        0 0 1 = no delta, no compression
      //                        0 1 - = delta, no compression
      //                        1 d - = compression, w/wo delta
      int version = 4 | ((isDelta) ? 2 : 0);
      CASSerializer.outputVersion(version, serializedOut);
        
      serializedOut.writeInt(VERSION);  // reserved for future version info
      if (doMeasurement) {
        sm.header = 12;
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
      totalMappedHeapSize = initFsStartIndexes(
          fsStartIndexes, heap, heapStart, heapEnd, typeCodeHisto, os, stringHeapObj, mark, false);
      if (heapStart == 0) {
        totalMappedHeapSize++;  // include the null at the start
      }

        // compute histogram of frequencies of source cas type codes
//        for (int i = ts.getTypeArraySize() - 1; i >= 0; i--) {
//          serializedTypeCode2Code[i] = i;
//        }
//        
//        // set typeCode2serializeCode so that the 0th element is the typeCode with the highest frequency, etc.
//        Arrays.sort(serializedTypeCode2Code, 0, serializedTypeCode2Code.length, new Comparator<Integer>() {
//          public int compare(Integer o1, Integer o2) {
//            return (typeCodeHisto[o1] > typeCodeHisto[o2]) ? -1 :
//                   (typeCodeHisto[o1] < typeCodeHisto[o2]) ? 1 : 0;
//          }
//        });
//        
//        for (int i = 0; i < serializedTypeCode2Code.length; i++) {
//          int tCode = serializedTypeCode2Code[i];
//          int c = typeCodeHisto[tCode];
//          if (c > 0) {
//            System.out.format("%2d %,9d instance of Type %s%n", i, c, typeInfoArray[tCode]);
//          }
//        }
        

      /**************************
       * Strings
       **************************/
      int stringHeapStart = isDelta ? mark.nextStringHeapAddr : 1;
      int stringHeapEnd = stringHeapObj.getSize();
      
      // the following loop was changed to instead add strings
      // that are part of the pre-scan, in order to exclude those
      // strings that are excluded due to the typeMapper
 
//      for (int i = stringHeapStart; i < stringHeapEnd; i++) {
//        os.add(stringHeapObj.getStringForCode(i));
//      }
      
      // also add in all modified strings
      // with current design, all modified strings are guaranteed
      // to be above the mark, so this code is commented out
//    addModStr:
//      if (isDelta) {
//        serializerForModifiedFSs.sortModifications();
//        final int[] mods = serializerForModifiedFSs.modifiedMainHeapAddrs;
//        final int modLen = serializerForModifiedFSs.modMainHeapAddrsLength;
//        int nextMod = 0;
//        if (modLen > 0) {
// 
//          for (int iHeap = 1; iHeap < heapStart; iHeap += incrToNextFs(heap, iHeap, typeInfo)) {
//            typeInfo = getTypeInfo(heap[iHeap]);
//            FsStringRefs fsStringRefs = new FsStringRefs(typeInfo, heap, iHeap);
//            for (int nextRef = fsStringRefs.next(); nextRef >= 0; nextRef = fsStringRefs.next()) {
//              while (nextRef > mods[nextMod]) {
//                nextMod ++;
//                if (nextMod == modLen) {
//                  break addModStr;
//                }
//              }
//              if (nextRef == mods[nextMod]) {
//                os.add(stringHeapObj.getStringForCode(heap[nextRef]));
//                nextMod ++;
//                if (nextMod == modLen) {
//                  break addModStr;
//                }
//              } 
//            }
//          }   
//        }
//      }

      os.optimize();
      String [] commonStrings = os.getCommonStrings();
      writeVnumber(strChars_i, commonStrings.length);
      for (int i = 0; i < commonStrings.length; i++) {
        int startPos = dosZipSources[strChars_i].size();
        DataIO.writeUTFv(commonStrings[i], dosZipSources[strChars_i]);
        // approximate histogram
        if (doMeasurement) {
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

      if (doMeasurement) {
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
        sm.statDetails[strLength_i].original = (stringHeapEnd - stringHeapStart) * 4;
        sm.statDetails[strOffset_i].original = (stringHeapEnd - stringHeapStart) * 4;
      }
      
      /***************************
       * Prepare to walk main heap
       ***************************/
      writeVnumber(control_dos, totalMappedHeapSize);  
      if (doMeasurement) {
        sm.statDetails[Slot_MainHeap.ordinal()].original = (1 + heapEnd - heapStart) * 4;      
      }
      
      Arrays.fill(iPrevHeapArray, 0);

      if (heapStart == 0) {
        heapStart = 1;  // slot 0 not serialized, it's null / 0
      }

//      if (CHANGE_FS_REFS_TO_SEQUENTIAL) {
//        // scan thru all fs and save their offsets in the heap
//        // to allow conversion from addr to sequential fs numbers
//        initFsStartIndexes(fsStartIndexes, heap, heapStart, heapEnd, typeCodeHisto);
//        
//        for (int i = ts.getTypeArraySize() - 1; i >= 0; i--) {
//          serializedTypeCode2Code[i] = i;
//        }
//        
//        // set typeCode2serializeCode so that the 0th element is the typeCode with the highest frequency, etc.
//        Arrays.sort(serializedTypeCode2Code, 0, serializedTypeCode2Code.length, new Comparator<Integer>() {
//          public int compare(Integer o1, Integer o2) {
//            return (typeCodeHisto[o1] > typeCodeHisto[o2]) ? -1 :
//                   (typeCodeHisto[o1] < typeCodeHisto[o2]) ? 1 : 0;
//          }
//        });
        
//        for (int i = 0; i < serializedTypeCode2Code.length; i++) {
//          int tCode = serializedTypeCode2Code[i];
//          int c = typeCodeHisto[tCode];
//          if (c > 0) {
//            System.out.format("%2d %,9d instance of Type %s%n", i, c, typeInfoArray[tCode]);
//          }
//        }
        
//      }

      
      
      /***************************
       * walk main heap
       ***************************/

      for (int iHeap = heapStart; iHeap < heapEnd; iHeap += incrToNextFs(heap, iHeap, typeInfo)) {
        final int tCode = heap[iHeap];  // get type code
        final int mappedTypeCode = isTypeMapping ? typeMapper.mapTypeCodeSrc2Tgt(tCode) : tCode;
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
            for (int i = 1; i < typeInfo.slotKinds.length + 1; i++) {
              serializeByKind(iHeap, i);
            }
          }
        }
      
        iPrevHeapArray[tCode] = iHeap;  // make this one the "prev" one for subsequent testing
        if (doMeasurement) {
          sm.statDetails[typeCode_i].incr(DataIO.lengthVnumber(tCode));
          sm.mainHeapFSs ++;
        }
      }  // end of heap walk
      
      serializeIndexedFeatureStructures();

      if (isDelta) {
        (new SerializeModifiedFSs()).serializeModifiedFSs();
      }

      collectAndZip();
      
      if (doMeasurement) {
        sm.totalTime = System.currentTimeMillis() - sm.totalTime;
      }
    }
    
    
    private void serializeIndexedFeatureStructures() throws IOException {
      final int[] fsIndexes = isDelta ? cas.getDeltaIndexedFSs(mark) : cas.getIndexedFSs();
      if (doMeasurement) {
        sm.statDetails[fsIndexes_i].original = fsIndexes.length * 4 + 1;      
      }
      final int nbrViews = fsIndexes[0];
      final int nbrSofas = fsIndexes[1];
      writeVnumber(control_i, nbrViews);
      writeVnumber(control_i, nbrSofas);
      
      if (doMeasurement) {
        sm.statDetails[fsIndexes_i].incr(1); // an approximation - probably correct
        sm.statDetails[fsIndexes_i].incr(1);
      }
      
      int fi = 2;
      final int end1 = nbrSofas + 2;
      for (; fi < end1; fi++) {
//      writeVnumber(control_i, fsIndexes[fi]);  // version 0
        final int v = fsStartIndexes.getTgtSeqFromSrcAddr(fsIndexes[fi]);
        writeVnumber(control_i, v);    // version 1
        
        if (doMeasurement) {
          sm.statDetails[fsIndexes_i].incr(DataIO.lengthVnumber(v));
        }
      }
       
      for (int vi = 0; vi < nbrViews; vi++) {
        fi = compressFsxPart(fsIndexes, fi);    // added FSs
        if (isDelta) {
          fi = compressFsxPart(fsIndexes, fi);  // removed FSs
          fi = compressFsxPart(fsIndexes, fi);  // reindexed FSs
        }
      }      
    }

    private int compressFsxPart(int[] fsIndexes, int fsNdxStart) throws IOException {
      int ix = fsNdxStart;
      final int nbrEntries = fsIndexes[ix++];
      final int end = ix + nbrEntries;
      // version 0
//      writeVnumber(fsIndexes_dos, nbrEntries);  // number of entries
      //version 1: the list is filtered by the tgt type, and may be smaller;
      //  it is written at the end, into the control_dos stream
//      if (doMeasurement) {
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
        final int v = ia[i];
        final int tgtV = fsStartIndexes.getTgtSeqFromSrcAddr(v);
        if (tgtV == 0) {
          continue; // skip - the target doesn't have this Fs
        }
        final int delta = tgtV - prev;
        entriesWritten++;
        writeVnumber(fsIndexes_dos, delta);
        if (doMeasurement) {
          sm.statDetails[fsIndexes_i].incr(DataIO.lengthVnumber(delta));
        }
        prev = tgtV;
      }
      writeVnumber(control_dos, entriesWritten);  // version 1  
      if (doMeasurement) {
        sm.statDetails[typeCode_i].incr(DataIO.lengthVnumber(entriesWritten));
      }

      return end;
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
      case Slot_HeapRef: case Slot_Int: case Slot_Short:
        {
          int prev = (iPrevHeap == 0) ? 0 : 
                     (heap[iPrevHeap + 1] == 0) ? 0 : // prev length is 0
                      heap[iPrevHeap + 2];  // use prev array 1st element
          for (int i = iHeap + 2; i < endi; i++) {
            prev = writeIntOrHeapRef(arrayElementKind.ordinal(), i, prev);
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
          writeString(stringHeapObj.getStringForCode(heap[i]));
        }
        break;
        
      default: throw new RuntimeException("internal error");
      } // end of switch    
    }
    
    private int writeIntOrHeapRef(int kind, int index, int prev) throws IOException {
      final int v = heap[index];
      writeDiff(kind, v, prev);
      return v;
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
        if (doMeasurement) {
          sm.statDetails[byte_i].incr(1);
          sm.origAuxByteArrayRefs += 4;
        }
        break; 
      case Slot_ShortRef:
        writeFromShortArray(heap[iHeap + 2], length);
        if (doMeasurement) {
          sm.origAuxShortArrayRefs += 4;
        }
        break; 
      case Slot_LongRef: case Slot_DoubleRef:
        writeFromLongArray(refKind, heap[iHeap + 2], length);
        if (doMeasurement) {
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
      int prev = (iPrevHeap == 0) ? 0 : heap[iPrevHeap + offset];
      writeDiff(kind.ordinal(), heap[iHeap + offset], prev);
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
     * @throws IOException 
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
          if (doMeasurement) {
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
    
//    private DataOutputStream getZipStream(SlotKind kind) {
//      DataOutputStream dos = dosZipSources[kind.i];
//      if (null == dos) {
//        dos = setupOutputStream(kind);
//      }
//      return dos;
//    }
    
    public DataOutputStream setupOutputStream(int i) {
      // set up output stream
      int size = estimatedZipSize[i];
      baosZipSources[i] = new ByteArrayOutputStream(size);
      return dosZipSources[i] = new DataOutputStream(baosZipSources[i]); 
    }

    private void writeLong(long v, long prev) throws IOException {
      writeDiff(long_High_i, (int)(v >>> 32), (int)(prev >>> 32));
      writeDiff(long_Low_i,  (int)v, (int)prev);    
    }

    /**
     * String encoding
     *   Length = 0 - used for null, no offset written
     *   Length = 1 - used for "", no offset written 
     *   Length > 0 (subtract 1): used for actual string length
     *   
     *   Length < 0 - use (-length) as slot index  (minimum is 1, slot 0 is NULL)
     *   
     *   For length > 0, write also the offset.
     *   
     */
    private void writeString(final String s) throws IOException {
      if (null == s) {
        writeVnumber(strLength_dos, 0);
        if (doMeasurement) {
          sm.statDetails[strLength_i].incr(1);
        }
        return;
      } 
      
      int indexOrSeq = os.getIndexOrSeqIndex(s);
      if (indexOrSeq < 0) {
        final int v = encodeIntSign(indexOrSeq);
        writeVnumber(strLength_dos, v);
        if (doMeasurement) {
          sm.statDetails[strLength_i].incr(DataIO.lengthVnumber(v));
        }
        return;
      }
      
      if (s.length() == 0) {
        writeVnumber(strLength_dos, encodeIntSign(1));
        if (doMeasurement) {
          sm.statDetails[strLength_i].incr(1);
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
      if (doMeasurement) {
        sm.statDetails[strOffset_i].incr(DataIO.lengthVnumber(offset));
        sm.statDetails[strLength_i].incr(DataIO.lengthVnumber(length));
      }
      if (!only1CommonString) {
        final int csi = os.getCommonStringIndex(indexOrSeq);
        writeVnumber(strSeg_dos, csi);
        if (doMeasurement) {
          sm.statDetails[strSeg_i].incr(DataIO.lengthVnumber(csi));
        }
      }
    }

    /**
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
        if (doMeasurement) {
          sm.statDetails[float_Exponent_i].incr(1);
        }
        return;
      }
     
      final int exponent = ((raw >>> 23) & 0xff) + 1;   // because we reserve 0, see above
      final int revMants = Integer.reverse((raw & 0x007fffff) << 9);  
      final int mants = (revMants << 1) + ((raw < 0) ? 1 : 0);
      writeVnumber(float_Exponent_dos, exponent); 
      writeVnumber(float_Mantissa_Sign_dos, mants);
      if (doMeasurement) {
        sm.statDetails[float_Exponent_i].incr(DataIO.lengthVnumber(exponent));
        sm.statDetails[float_Mantissa_Sign_i].incr(DataIO.lengthVnumber(mants));
      }
    }

    private void writeVnumber(int kind, int v) throws IOException {
      DataIO.writeVnumber(dosZipSources[kind], v);
      if (doMeasurement) {
        sm.statDetails[kind].incr(DataIO.lengthVnumber(v));
      }
    }
    
    private void writeVnumber(int kind, long v) throws IOException {
      DataIO.writeVnumber(dosZipSources[kind], v);
      if (doMeasurement) {
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
        if (doMeasurement) {
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
      if (doMeasurement) {
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
     * @throws IOException 
     */
    private void writeDiff(int kind, int v, int prev) throws IOException {
      if (v == 0) {
        writeVnumber(kind, 0);  // a speedup, not a new encoding
        if (doMeasurement) {
          sm.statDetails[kind].diffEncoded ++;
          sm.statDetails[kind].valueLeDiff ++;
        }
        return;
      }
      
      if (v == Integer.MIN_VALUE) { // special handling, because abs fails
        writeVnumber(kind, 2);      // written as -0
        if (doMeasurement) {
          sm.statDetails[kind].diffEncoded ++;
          sm.statDetails[kind].valueLeDiff ++;
        }
        return;
      }
    
      if ((kind == heapRef_i) || (kind == fsIndexes_i)) {
        // for heap refs, we write out the seq # instead
        v = fsStartIndexes.getTgtSeqFromSrcAddr(v);
        if (prev != 0) {
          prev = fsStartIndexes.getTgtSeqFromSrcAddr(prev);
        }
      }

      final int absV = Math.abs(v);
      if (((v > 0) && (prev > 0)) ||
          ((v < 0) && (prev < 0))) {
        final int diff = v - prev;  // guaranteed not to overflow
        final int absDiff = Math.abs(diff);
        writeVnumber(kind, 
            (absV <= absDiff) ? 
                ((long)absV << 2)    + ((v < 0) ? 2L : 0L) :
                ((long)absDiff << 2) + ((diff < 0) ? 3L : 1L));
        if (doMeasurement) {
          sm.statDetails[kind].diffEncoded ++;
          sm.statDetails[kind].valueLeDiff += (absV <= absDiff) ? 1 : 0;
        }
        return;
      }
      // if get here, then the abs v value is always <= the abs diff value.
      writeVnumber(kind, ((long)absV << 2) + ((v < 0) ? 2 : 0));
      if (doMeasurement) {
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
    public class SerializeModifiedFSs {

      final int[] modifiedMainHeapAddrs = cas.getModifiedFSHeapAddrs().toArray();
      final int[] modifiedFSs = cas.getModifiedFSList().toArray();
      final int[] modifiedByteHeapAddrs = cas.getModifiedByteHeapAddrs().toArray();
      final int[] modifiedShortHeapAddrs = cas.getModifiedShortHeapAddrs().toArray();
      final int[] modifiedLongHeapAddrs = cas.getModifiedLongHeapAddrs().toArray();

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
      
      private void serializeModifiedFSs() throws IOException {
        // write out number of modified Feature Structures
        writeVnumber(control_dos, modFSsLength);
        // iterate over all modified feature structures
        /**
         * Theorems about these data
         *   1) Assumption: if an AuxHeap array is modified, its heap FS is in the list of modFSs
         *   2) FSs with AuxHeap values have increasing ref values into the Aux heap as FS addr increases
         *      (because the ref is not updateable).
         *   3) Assumption: String array element modifications are main heap slot changes
         *      and recorded as such
         */
        
        for (int i = 0; i < modFSsLength; i++) {
          iHeap = modifiedFSs[i];     
          final int tCode = heap[iHeap];
          typeInfo = ts.getTypeInfo(tCode);
          
          // write out the address of the modified FS
          // will convert to seq# internally
          writeDiff(fsIndexes_i, iHeap, iPrevHeap);
          // delay updating iPrevHeap until end of "for" loop
          
          /**************************************************
           * handle aux byte, short, long array modifications
           **************************************************/
          if (typeInfo.isArray && (!typeInfo.isHeapStoredArray)) {
            writeAuxHeapMods();           
          } else { 
            writeMainHeapMods(); 
          }  // end of processing 1 modified FS
          iPrevHeap = iHeap;
        }  // end of for loop over all modified FSs
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
      
      private void writeMainHeapMods() throws IOException {
        final int fsLength = incrToNextFs(heap, iHeap, typeInfo);
        final int numberOfModsInFs = countModifiedSlotsInFs(fsLength);
        writeVnumber(fsIndexes_dos, numberOfModsInFs);
        int iPrevOffsetInFs = 0;

        for (int i = 0; i < numberOfModsInFs; i++) {
          final int nextMainHeapIndex = modifiedMainHeapAddrs[imaModMainHeap++];
          final int offsetInFs = nextMainHeapIndex - iHeap;
          
          writeVnumber(fsIndexes_dos, offsetInFs - iPrevOffsetInFs);
          iPrevOffsetInFs = offsetInFs;
          
          final SlotKind kind = typeInfo.getSlotKind(typeInfo.isArray ? 2 : offsetInFs);
          
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
      
      private void writeAuxHeapMods() throws IOException {
        final int auxHeapIndex = heap[iHeap + 2];
        int iPrevOffsetInAuxArray = 0;
        
        final SlotKind kind = typeInfo.getSlotKind(2);  // get kind of element
        final boolean isAuxByte = ((kind == Slot_BooleanRef) || (kind == Slot_ByteRef));
        final boolean isAuxShort = (kind == Slot_ShortRef);
        final boolean isAuxLong = ((kind == Slot_LongRef) || (kind == Slot_DoubleRef));
        
        if (!(isAuxByte | isAuxShort | isAuxLong)) {
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
        writeVnumber(fsIndexes_dos, numberOfModsInAuxHeap);
        
        for (int i = 0; i < numberOfModsInAuxHeap; i++) {
          final int nextModAuxIndex = modXxxHeapAddrs[imaModXxxRef++];
          final int offsetInAuxArray = nextModAuxIndex - auxHeapIndex;
          
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
    
  }  // end of class definition for Serializer
  
  /**
   * Class instantiated once per deserialization
   * Multiple deserializations in parallel supported, with
   * multiple instances of this
   */
  private class Deserializer {
    
    final private CASImpl cas;  // cas being serialized
    final private DataInput deserIn;
    final private int version;

    final private DataInputStream[] dataInputs = new DataInputStream[NBR_SLOT_KIND_ZIP_STREAMS];
    private Inflater[] inflaters = new Inflater[NBR_SLOT_KIND_ZIP_STREAMS];

    private int[] heap;           // main heap
    private int heapStart;
    private int heapEnd;
    
    private IntVector fixupsNeeded;  // for deserialization, the "fixups" for relative heap refs needed
    private StringHeap stringHeapObj;
    private LongHeap longHeapObj;
    private ShortHeap shortHeapObj;
    private ByteHeap byteHeapObj;
    
    private int stringTableOffset;
    
    /**
     * These indexes remember sharable common values in aux heaps
     * Values must be in aux heap, but not part of arrays there
     *   so that rules out boolean, byte, and shorts
     */
    private int longZeroIndex = -1; // also used for double 0 index
    private int double1Index = -1;

    final private boolean isDelta;        // if true, a delta is being deserialized
    final private ComprItemRefs fsStartIndexes = (CHANGE_FS_REFS_TO_SEQUENTIAL) ? new ComprItemRefs() : null;
    private String[] readCommonString;

    private TypeInfo typeInfo; // type info for the current type being serialized

    /**
     * Content is addr; converted to seq before use as diff for heap ref
     */
    final private int[] iPrevHeapArray; // index of previous instance of this typecode in heap, by typecode
    private int iPrevHeap;        // 0 or heap addr of previous instance of current type
    private boolean only1CommonString;

    final private CasTypeSystemMapper typeMapper;
    final private boolean isTypeMapping;
    final private TypeSystemImpl tgtTs;

    // speedups
    
    final private DataInputStream arrayLength_dis;
    final private DataInputStream heapRef_dis;
    final private DataInputStream int_dis;
    final private DataInputStream byte_dis;
    final private DataInputStream short_dis;
    final private DataInputStream typeCode_dis;
    final private DataInputStream strOffset_dis;
    final private DataInputStream strLength_dis;
    final private DataInputStream long_High_dis;
    final private DataInputStream long_Low_dis;
    final private DataInputStream float_Mantissa_Sign_dis;
    final private DataInputStream float_Exponent_dis;
    final private DataInputStream double_Mantissa_Sign_dis;
    final private DataInputStream double_Exponent_dis;
    final private DataInputStream fsIndexes_dis;
    final private DataInputStream strChars_dis;
    final private DataInputStream control_dis;
    final private DataInputStream strSeg_dis;

    /**
     * Called after header was read and determined that
     * this was a compressed binary 
     * @param cas
     * @param deserIn
     * @throws IOException 
     */
    Deserializer(CASImpl cas, DataInput deserIn, boolean isDelta, TypeSystemImpl tgtTs) throws IOException {
      this.cas = cas;
      this.deserIn = deserIn;
      this.isDelta = isDelta;
      typeMapperCmn = typeMapper = ts.getTypeSystemMapper(tgtTs);
      isTypeMappingCmn = isTypeMapping = (null != typeMapper);
      this.tgtTs = (tgtTs == null) ? ts : tgtTs;
      
      stringHeapObj = cas.getStringHeap();
      longHeapObj   = cas.getLongHeap();
      shortHeapObj  = cas.getShortHeap();
      byteHeapObj   = cas.getByteHeap();

      version = deserIn.readInt();    // version of the compressed serializer
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

      iPrevHeapArray = new int[ts.getTypeArraySize()];

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
    
    private void deserialize() throws IOException {
      
      /************************************************
       * Setup all the input streams with inflaters
       ************************************************/
//      long startTime1 = System.currentTimeMillis();
      
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
      int deltaHeapSize = readVnumber(control_dis);         
      final Heap heapObj = cas.getHeap();
      
      heapStart = isDelta ? heapObj.getNextId() : 0;
      stringTableOffset = isDelta ? (stringHeapObj.getSize() - 1) : 0;
      
      if (isDelta) {
        heapObj.grow(deltaHeapSize);
      } else {
        heapObj.reinitSizeOnly(deltaHeapSize);
      } 
      
      heapEnd = heapStart + deltaHeapSize; 
      heap = heapObj.heap;

      Arrays.fill(iPrevHeapArray, 0);
      
      if (heapStart == 0) {
        heapStart = 1;  // slot 0 not serialized, it's null / 0
      }

      // Compute addr <--> seq for existing FSs below mark line
      // and seq(this CAS) <--> seq(incoming) that accounts for type code mismatch using typeMapper
      // note: rest of maps computed incrementally as we deserialize
      //   Two possibilities:  The CAS has a type, but the incoming is missing that type (services)
      //                       The incoming has a type, but the CAS is missing it - (deser from file)
      //     Below the merge line: only the 1st is possible
      //     Above the merge line: only the 2nd is possible

      if (isDelta) { 
        // scan current source being added to / merged into
        initFsStartIndexes(fsStartIndexes, heap, 1, heapStart, null, null, null, null, false);
      }

      fixupsNeeded = new IntVector(Math.max(16, heap.length / 10));

      /***************************
       * walk main heap
       ***************************/
      for (int iHeap = heapStart; iHeap < heapEnd;) {
        final int tgtTypeCode = readVnumber(typeCode_dis); // get type code
        final int tCode = isTypeMapping ? typeMapper.mapTypeCodeTgt2Src(tgtTypeCode) : tgtTypeCode;
        final boolean storeIt = (tCode != 0);
        fsStartIndexes.addSrcAddrForTgt(iHeap, storeIt);
          // A receiving client from a service always
          // has a superset of the service's types due to type merging so this
          // won't happen for that use case. But 
          // a deserialize-from-file could hit this if the receiving type system
          // deleted a type.
        
          // The strategy for deserializing heap refs depends on finding
          // the prev value for that type.  This can be skipped for 
          // types being skipped because they don't exist in the source
        
        typeInfo = tgtTs.getTypeInfo(tgtTypeCode);
        iPrevHeap = iPrevHeapArray[tCode];  // will be ignored for non-existant type

        if (typeInfo.isHeapStoredArray) {
          readHeapStoredArray(iHeap, storeIt);
        } else if (typeInfo.isArray) {
          readNonHeapStoredArray(iHeap, storeIt);
        } else {
          if (isTypeMapping) {
            final int[] tgtFeatOffsets2Src = typeMapper.getTgtFeatOffsets2Src(tCode);
            for (int i = 0; i < tgtFeatOffsets2Src.length; i++) {
              final int featOffsetInSrc = tgtFeatOffsets2Src[i] + 1;
              SlotKind kind = typeInfo.getSlotKind(i+1);
              readByKind(iHeap, featOffsetInSrc, kind, storeIt);
            }
          } else {
            for (int i = 1; i < typeInfo.slotKinds.length + 1; i++) {
              SlotKind kind = typeInfo.getSlotKind(i);
              readByKind(iHeap, i, kind, storeIt);
            }
          }
        }
        if (tCode != 0) {
          iPrevHeapArray[tCode] = iHeap;  // make this one the "prev" one for subsequent testing
        }
        iHeap += (0 == tCode) ? 0 : incrToNextFs(heap, iHeap, typeInfo);
      }
      
      final int end = fixupsNeeded.size();
      for (int i = 0; i < end; i++) {
        final int heapAddrToFix = fixupsNeeded.get(i);
        heap[heapAddrToFix] = fsStartIndexes.getSrcAddrFromTgtSeq(heap[heapAddrToFix]);
      }        
      
      readIndexedFeatureStructures();

      if (isDelta) {
        (new ReadModifiedFSs()).readModifiedFSs();
      }

      closeDataInputs();
//      System.out.format("Deserialize took %,d ms%n", System.currentTimeMillis() - startTime1);
    }
    
    private void readNonHeapStoredArray(int iHeap, boolean storeIt) throws IOException {
      final int length = readArrayLength(iHeap, storeIt);
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
    
    private int readArrayLength(int iHeap, boolean storeIt) throws IOException {
      final int v =  readVnumber(arrayLength_dis);
      if (storeIt) {
        heap[iHeap + 1] = v;
      }
      return v;
    }

    private void readHeapStoredArray(int iHeap, boolean storeIt) throws IOException {
      final int length = readArrayLength(iHeap, storeIt);
      // output values
      // special case 0 and 1st value
      if (length == 0) {
        return;
      }
      SlotKind arrayElementKind = typeInfo.slotKinds[1];
      final int endi = iHeap + length + 2;
      switch (arrayElementKind) {
      case Slot_HeapRef: case Slot_Int: case Slot_Short:
        {
          int prev = (iPrevHeap == 0) ? 0 : 
                     (heap[iPrevHeap + 1] == 0) ? 0 : // prev array length = 0
                      heap[iPrevHeap + 2]; // prev array 0th element
          for (int i = iHeap + 2; i < endi; i++) {
            final int v = readDiff(arrayElementKind, prev);
            if (storeIt) {
              heap[i] = v;
              if (arrayElementKind == Slot_HeapRef) {
                fixupsNeeded.add(i);
              }
            }
            prev = v;
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
     * @param iHeap
     * @param offset can be -1 - in which case read, but don't store
     * @throws IOException
     */
    private void readByKind(int iHeap, int offset, SlotKind kind, boolean storeIt) throws IOException {
      
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
        fixupsNeeded.add(iHeap + offset);
        break;
      case Slot_StrRef: 
        final int vStrRef = readString(storeIt);
        if (storeIt) {
          heap[iHeap + offset] = vStrRef;
        }
        break;
      case Slot_LongRef: {
        long v = readLong(kind, (iPrevHeap == 0) ? 0L : longHeapObj.getHeapValue(heap[iPrevHeap + offset]));
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
        fsIndexes.add(readVnumber(control_dis));
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
      int zipBufSize = Math.max(32768, bytesCompr);
      InflaterInputStream iis = new InflaterInputStream(baiStream, inflater, zipBufSize);
      dataInputs[slotIndex] = new DataInputStream(new BufferedInputStream(iis, zipBufSize));
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
        byte_dis.skipBytes(length);
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
        short_dis.skipBytes(length * 2);
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
          h[i] = prev = readLong(kind, prev);
        }
        return startPos;
      } else {
        skipLong(length);
        return 0;
      }
    }

    private void readDiffWithPrevTypeSlot(
        SlotKind kind, 
        int iHeap, 
        int offset,
        boolean storeIt) throws IOException {
      if (storeIt) {
        int prev = (iPrevHeap == 0) ? 0 : heap[iPrevHeap + offset];
        heap[iHeap + offset] = readDiff(kind, prev);
      } else {
        readDiff(kind, 0);
      }
    }

    private int readDiff(SlotKind kind, int prev) throws IOException {
      return readDiff(getInputStream(kind), prev);
    }
    
    private int readDiff(DataInput in, int prev) throws IOException {
      final long encoded = readVlong(in);
      final boolean isDeltaEncoded = (0 != (encoded & 1L));
      final boolean isNegative = (0 != (encoded & 2L));
      int v = (int)(encoded >>> 2);
      if (isNegative) {
        if (v == 0) {
          return Integer.MIN_VALUE;
        }
        v = -v;
      }
      if (isDeltaEncoded) {
        v = v + prev;
      }
      return v;

    }
        
    private long readLong(SlotKind kind, long prev) throws IOException {
      if (kind == Slot_DoubleRef) {
        return readDouble();
      }
    
      final int vh = readDiff(long_High_dis, (int) (prev >>> 32));
      final int vl = readDiff(long_Low_dis, (int) prev);
      final long v = (((long)vh) << 32) | (0xffffffffL & (long)vl);
      return v;
    }
    
    private void skipLong(int length) throws IOException {
      for (int i = 0; i < length; i++) {
        long_High_dis.skipBytes(8);
        long_Low_dis.skipBytes(8);
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
      int length = decodeIntSign(readVnumber(strLength_dis));
      if (0 == length) {
        return 0;
      }
      if (1 == length) {
        if (storeIt) {
          return stringHeapObj.addString("");
        } else {
          return 0;
        }
      }
      
      if (length < 0) {  // in this case, -length is the slot index
        if (storeIt) {
          return stringTableOffset - length;
        } else {
          return 0;
        }
      }
      int offset = readVnumber(strOffset_dis);
      int segmentIndex = (only1CommonString) ? 0 :
        readVnumber(strSeg_dis);
      if (storeIt) {
        String s =  readCommonString[segmentIndex].substring(offset, offset + length - 1);
        return stringHeapObj.addString(s);
      } else {
        return 0;
      }
    }

    /******************************************************************************
     * Modified Values
     * 
     * Modified heap values need fsStartIndexes conversion
     ******************************************************************************/

    class ReadModifiedFSs {
      
      // previous value - for things diff encoded
      int vPrevModInt = 0;
      int prevModHeapRefTgtSeq = 0;
      short vPrevModShort = 0;
      long vPrevModLong = 0;
      int iHeap;
      TypeInfo typeInfo;
      int[] tgtF2srcF;
      
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
            // never happen because delta CAS ts system case, the 
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
            readModifiedMainHeap(numberOfModsInThisFs);
          }
        }
      }
      
      // update the byte/short/long aux heap entries
      // for arrays
      /**
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
            final long v = readLong(kind, vPrevModLong);
            if (kind == Slot_LongRef) {
              vPrevModLong = v;
            }
            longHeapObj.setHeapValue(v, auxHeapIndex + offset);
          }    
        }
      }
      
      private void readModifiedMainHeap(int numberOfMods) throws IOException {
        int iPrevTgtOffsetInFs = 0;
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
          
          switch (kind) {
          case Slot_HeapRef: {
              final int tgtSeq = readDiff(heapRef_dis, prevModHeapRefTgtSeq);
              prevModHeapRefTgtSeq = tgtSeq;
              final int v = fsStartIndexes.getSrcAddrFromTgtSeq(tgtSeq);
              heap[iHeap + srcOffsetInFs] = v;
            }
            break;
          case Slot_Int: {
              final int v = readDiff(int_dis, vPrevModInt);
              vPrevModInt = v;
              heap[iHeap + srcOffsetInFs] = v;
            }
            break;
          case Slot_Short: {
              final int v = readDiff(int_dis, vPrevModShort);
              vPrevModShort = (short)v;
              heap[iHeap + srcOffsetInFs] = v;
            }
            break;
          case Slot_LongRef: case Slot_DoubleRef: {
              final long v = readLong(kind, vPrevModLong);
              if (kind == Slot_LongRef) {
                vPrevModLong = v;
              }
              heap[iHeap + srcOffsetInFs] = longHeapObj.addLong(v);
            }
            break;
          case Slot_Byte: case Slot_Boolean:
            heap[iHeap + tgtOffsetInFs] = byte_dis.readByte();
            break;
          case Slot_Float:
            heap[iHeap + tgtOffsetInFs] = readFloat();
            break;
          case Slot_StrRef:
            heap[iHeap + tgtOffsetInFs] = readString(true);
            break;
         default:
            throw new RuntimeException();
          }
        }   
      }
    }
  }

  /********************************************************************
   * methods common to serialization / deserialization etc.
   ********************************************************************/
  
  private static int incrToNextFs(int[] heap, int iHeap, TypeInfo typeInfo) {
    if (typeInfo.isHeapStoredArray) {
      return 2 + heap[iHeap + 1];
    } else {
      return 1 + typeInfo.slotKinds.length;
    }
  }

  
  /**
   * Serializing:
   *   Called at beginning, scans whole CAS
   * Deserializing:
   *   Called at beginning if doing delta CAS, scans old CAS up to mark
   * @param fsStartIndexes
   * @param srcHeap
   * @param srcHeapStart
   * @param srcHeapEnd
   * @param histo
   * @param optimizeStrings
   * @param stringHeapObj
   * @return amount of heap used in target, side effect: set up fsStartIndexes (for both src and tgt)
   */
  private int initFsStartIndexes (
      final ComprItemRefs fsStartIndexes, 
      final int[] srcHeap, 
      final int srcHeapStart,   // might be 0, might be 1, might be start of delta TODO check 0/1?
      final int srcHeapEnd, 
      final int[] histo,
      final OptimizeStrings optimizeStrings,     // null when deserializing or comparing CASes 
      final StringHeap stringHeapObj, 
      final MarkerImpl mark,
      final boolean isCompareCall) {
   
    final boolean isTypeMapping = isTypeMappingCmn;
    final CasTypeSystemMapper typeMapper = typeMapperCmn;
    
    int tgtHeapUsed = 0;
    int markStringHeap = (mark == null) ? 0 : mark.getNextStringHeapAddr();
    
    for (int iSrcHeap = 1, iTgtHeap = 1; iSrcHeap < srcHeapEnd;) {
      final int tCode = srcHeap[iSrcHeap];
      final int tgtTypeCode = isTypeMapping ? typeMapper.mapTypeCodeSrc2Tgt(tCode) : tCode;
      final boolean isIncludedType = (tgtTypeCode != 0);
      
      // record info for type
      fsStartIndexes.addItemAddr(iSrcHeap, iTgtHeap, isIncludedType, isCompareCall);  // maps src heap to tgt seq

      // maybe do histogram of typecodes
      if ((null != histo) && (iSrcHeap >= srcHeapStart)) {
        histo[tCode] ++;
      }
      
      // for features in type - 
      //    strings: accumulate those strings that are in the target, if optimizeStrings != null
      //      strings either in array, or in individual values
      //    byte (array), short (array), long/double (instance or array): record if entries in aux array are skipped
      //      (not in the target).  Note the recording will be in a non-ordered manner (due to possible updates by
      //       previous delta deserialization)
      final TypeInfo srcTypeInfo = ts.getTypeInfo(tCode);
      final TypeInfo tgtTypeInfo = (isTypeMapping && isIncludedType) ? typeMapper.tsTgt.getTypeInfo(tgtTypeCode) : srcTypeInfo;
      
      
      // add strings for included types (only when serializing)
      if (isIncludedType && (optimizeStrings != null)) { 

        // next test only true if tgtTypeInfo.slotKinds[1] == Slot_StrRef
        // because this is the built-in type string array which is final
        if (srcTypeInfo.isHeapStoredArray && (srcTypeInfo.slotKinds[1] == Slot_StrRef)) {
          for (int i = 0; i < srcHeap[iSrcHeap + 1]; i++) {
            // this bit of strange logic depends on the fact that all new and updated strings
            // are "added" at the end of the string heap in the current impl
            final int strHeapIndex = srcHeap[iSrcHeap + 2 + i];
            if (strHeapIndex >= markStringHeap) {
              optimizeStrings.add(stringHeapObj.getStringForCode(strHeapIndex));
            }
          }
        } else {
          final int[] strOffsets = srcTypeInfo.strRefOffsets;
          final boolean[] fSrcInTgt = isTypeMapping ? typeMapper.getFSrcInTgt(tCode) : null;
          for (int i = 0; i < strOffsets.length; i++ ) {
            int srcOffset = strOffsets[i];  // offset to slot having str ref
            // add only those strings in slots that are in target
            if (!isTypeMapping || fSrcInTgt[srcOffset]) {
              final int strHeapIndex = srcHeap[iSrcHeap + strOffsets[i]];
              // this bit of strange logic depends on the fact that all new and updated strings
              // are "added" at the end of the string heap in the current impl
              if (strHeapIndex >= markStringHeap) {
                optimizeStrings.add(stringHeapObj.getStringForCode(strHeapIndex));
              }
            }
          }
        }
      }
      
      // add "skip" entries for non-included type's
      //   features which are stored in the Aux heap array
      if (isTypeMapping) {
        if (isIncludedType && !srcTypeInfo.isHeapStoredArray ) {
          // scan features for omitted slot which is a long or double
          final boolean[] fSrcInTgt = typeMapper.getFSrcInTgt(tCode);
          for (int iSrcFeat = 0; iSrcFeat < srcTypeInfo.slotKinds.length; iSrcFeat++) {
            // for each feature slot, in a normal included type,
            // if the target doesn't have this feature, and it's a long/double, add this to the set of skipped slots in the aux array 
            if (!fSrcInTgt[iSrcFeat] && (
                (srcTypeInfo.slotKinds[iSrcFeat] == SlotKind.Slot_DoubleRef) ||
                (srcTypeInfo.slotKinds[iSrcFeat] == SlotKind.Slot_LongRef))) { 
              fsStartIndexes.recordSkippedAuxHeap(AuxHeap.LongAH, srcHeap[iSrcHeap + iSrcFeat + 1], 1);
            }
          }
        } else if (!isIncludedType) {
          // if the src Type is not in the target, and the src Type is a ref to one of the aux arrays
          if (!srcTypeInfo.isHeapStoredArray) {
            // is an array of boolean, byte, short, long or double
            final int skipStart = srcHeap[iSrcHeap + 2];
            final int skipSize  = srcHeap[iSrcHeap + 1];
            final AuxHeap auxHeap = getAuxHeapFromSlotKind(srcTypeInfo.slotKinds[1]);
            fsStartIndexes.recordSkippedAuxHeap(auxHeap, skipStart, skipSize);
          }
        }
      }
      
      // Advance to next Feature Structure, in both source and target heap frame of reference
      if (isIncludedType) {
        final int deltaTgtHeap = incrToNextFs(srcHeap, iSrcHeap, tgtTypeInfo);
        iTgtHeap += deltaTgtHeap;
        if (iSrcHeap >= srcHeapStart) {
          tgtHeapUsed += deltaTgtHeap;
        }
      }
      iSrcHeap += incrToNextFs(srcHeap, iSrcHeap, srcTypeInfo);
    }
    
    if (isTypeMapping) {
      // sort the skip information
      for (List<AuxSkip> skips : fsStartIndexes.skips) {
        Collections.sort(skips);
      }
    }
    return tgtHeapUsed;  // side effect: set up fsStartIndexes
  } 


  /**
   * Compare 2 CASes, with perhaps different type systems.
   * If the type systems are different, construct a type mapper and use that
   *   to selectively ignore types or features not in other type system
   * 
   * @param c1 CAS to compare
   * @param c2 CAS to compare
   * @return true if equal (for types / features in both)
   */
  public static boolean compareCASes(CASImpl c1, CASImpl c2) {
    return (new CasCompare(c1, c2)).compareCASes();
  }
  
  public static class CasCompare {
    /** 
     * Compare 2 CASes for equal
     * The layout of refs to aux heaps does not have to match
     */
      final private CASImpl c1;
      final private CASImpl c2;
      final private TypeSystemImpl ts1;      
      final private TypeSystemImpl ts2;
      final private boolean isTypeMapping;
      final private CasTypeSystemMapper typeMapper;
      final private Heap c1HO;
      final private Heap c2HO;
      final private int[] c1heap;
      final private int[] c2heap;
      final private int c1end;
      final private int c2end;
      final private ComprItemRefs fsStartIndexes = new ComprItemRefs();
      
      private TypeInfo typeInfo;
      private int seqHeapSrc;
      private int c1heapIndex;
      private int c2heapIndex;
            
    public CasCompare(CASImpl c1, CASImpl c2) {
      this.c1 = c1;
      this.c2 = c2;
      ts1 = c1.getTypeSystemImpl();
      ts2 = c2.getTypeSystemImpl();
      isTypeMapping = (ts1 != ts2);
      typeMapper = isTypeMapping ? new CasTypeSystemMapper(ts1, ts2) : null;
      c1HO = c1.getHeap();
      c2HO = c2.getHeap();
      c1heap = c1HO.heap;
      c2heap = c2HO.heap;
      c1end = c1HO.getCellsUsed();
      c2end = c2HO.getCellsUsed();
    }
      
    public boolean compareCASes() {
      if ((c1end != c2end) && (!isTypeMapping)) {
        System.err.format("CASes have different heap cells used: %,d %,d%n", c1end, c2end);
        return false;
      }
      
      ComprItemRefs fsStartIndexes = new ComprItemRefs();
      initFsStartIndexesCompare();
      
//      final int endHeapSeqSrc = fsStartIndexes.getNbrOfItems();
      c1heapIndex = 1;
      c2heapIndex = 1;
      seqHeapSrc = 1;
      for (; c1heapIndex < c1end;) {
        if (!advanceOverNonIncluded(1)) {  // 1 for c1, break if at end
          break;
        }
        if (!advanceOverNonIncluded(2)) {  // 2 for c2, break if at end
          break;
        }
        if (!compareFss()) {
          return false;
        }
      }
      
      int[] ifs1 = c1.getIndexedFSs();
      int[] ifs2 = c2.getIndexedFSs();
      
      return Arrays.equals(ifs1, ifs2);
    }

    private boolean compareFss() {
      int tCode = c1heap[c1heapIndex];
      typeInfo = ts1.getTypeInfo(tCode);
      if (tCode != c2heap[c2heapIndex]) {
        return mismatchFs();
      }
      if (typeInfo.isArray) {
        return compareFssArray();
      } else {
        for (int i = 1; i < typeInfo.slotKinds.length + 1; i++) {
          if (!compareSlot(i)) {
            return mismatchFs();
          }
        }
        return true;
      }
    }
      
    private boolean compareFssArray() {
      int len1 = c1heap[c1heapIndex + 1];
      int len2 = c2heap[c2heapIndex + 1];
      if (len1 != len2) {
        return false;
      }
      for (int i = 0; i < len1; i++) {
        SlotKind kind = typeInfo.getSlotKind(2);
        if (typeInfo.isHeapStoredArray) {
          if (kind == Slot_StrRef) {
            if (! compareStrings(c1.getStringForCode(c1heap[c1heapIndex + 2 + i]),
                                 c2.getStringForCode(c2heap[c2heapIndex + 2 + i]))) {
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
                c1.getLongHeap().getHeapValue(c1heap[c2heapIndex + 2] + i)) {
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
    
    private boolean compareSlot(int offset) {
      SlotKind kind = typeInfo.getSlotKind(offset);
      switch (kind) {
      case Slot_Int: case Slot_Short: case Slot_Boolean: case Slot_Byte: 
      case Slot_Float: case Slot_HeapRef:
        return c1heap[c1heapIndex + offset] == c2heap[c2heapIndex + offset];
      case Slot_StrRef:
        return compareStrings(c1.getStringForCode(c1heap[c1heapIndex + offset]),
                              c2.getStringForCode(c2heap[c2heapIndex + offset]));
      case Slot_LongRef: case Slot_DoubleRef:
        return c1.getLongHeap().getHeapValue(c1heap[c1heapIndex + offset]) ==
               c2.getLongHeap().getHeapValue(c2heap[c2heapIndex + offset]);
      default: throw new RuntimeException("internal error");      
      }
    }
    
    private boolean compareStrings(String s1, String s2) {
      if ((null == s1) && (null == s2)) {
        return true;
      }
      return s1.equals(s2);
    }
     
    /**
     * @param id
     * @return true if found an included type
     */
    private boolean advanceOverNonIncluded(int id) {
      if (!isTypeMapping) {
        return true;
      }
      final boolean src2tgt = (id == 1);
      final TypeSystemImpl ts = src2tgt ? ts1 : ts2;
      final int[] heap = src2tgt ? c1heap : c2heap;
            int index  = src2tgt ? c1heapIndex : c2heapIndex;
      final int end = src2tgt ? c1end : c2end;
      for (; index >= end;) {      
        final int tCode = heap[index];    
        if (typeMapper.mapTypeCode2Other(tCode, src2tgt) != 0) {
          if (src2tgt) {
            c1heapIndex = index;
          } else {
            c2heapIndex = index;
          }
          return true;
        }
        index += incrToNextFs(heap, index, ts.getTypeInfo(tCode));
      }
      if (src2tgt) {
        c1heapIndex = index;
      } else {
        c2heapIndex = index;
      }
      return false;  
    }
    
    private int skipOverTgtFSsNotInSrc(
        int[] heap, int heapEnd, int nextFsIndex, CasTypeSystemMapper typeMapper) {
      final TypeSystemImpl ts = typeMapper.tsTgt;
      for (; nextFsIndex < heapEnd;) {
        final int tCode = heap[nextFsIndex];
        if (typeMapper.mapTypeCodeTgt2Src(tCode) != 0) { 
          break;
        }
        nextFsIndex += incrToNextFs(heap, nextFsIndex, ts.getTypeInfo(tCode));
      }
      return nextFsIndex;
    }
    
    public void initFsStartIndexesCompare () {
         
      final boolean isCompareCall = true;
      int iTgtHeap = isTypeMapping ? skipOverTgtFSsNotInSrc(c2heap, c2end, 1, typeMapper) : 1;
      
      
      for (int iSrcHeap = 1; iSrcHeap < c1end;) {
        final int tCode = c1heap[iSrcHeap];
        final int tgtTypeCode = isTypeMapping ? typeMapper.mapTypeCodeSrc2Tgt(tCode) : tCode;
        final boolean isIncludedType = (tgtTypeCode != 0);
        
        // record info for type
        fsStartIndexes.addItemAddr(iSrcHeap, iTgtHeap, isIncludedType, isCompareCall);  // maps src heap to tgt seq
        
        // for features in type - 
        //    strings: accumulate those strings that are in the target, if optimizeStrings != null
        //      strings either in array, or in individual values
        //    byte (array), short (array), long/double (instance or array): record if entries in aux array are skipped
        //      (not in the target).  Note the recording will be in a non-ordered manner (due to possible updates by
        //       previous delta deserialization)
        final TypeInfo srcTypeInfo = ts1.getTypeInfo(tCode);
        final TypeInfo tgtTypeInfo = (isTypeMapping && isIncludedType) ? ts2.getTypeInfo(tgtTypeCode) : srcTypeInfo;
              
        // Advance to next Feature Structure, in both source and target heap frame of reference
        if (isIncludedType) {
          final int deltaTgtHeap = incrToNextFs(c1heap, iSrcHeap, tgtTypeInfo);
          iTgtHeap += deltaTgtHeap;
          if (isTypeMapping) {
            iTgtHeap = skipOverTgtFSsNotInSrc(c2heap, c2end, iTgtHeap, typeMapper);
          }
        }
        iSrcHeap += incrToNextFs(c1heap, iSrcHeap, srcTypeInfo);
      }
    } 

    private boolean mismatchFs() {
      System.err.format("Mismatched Feature Structures:%n %s%n %s%n", 
          dumpHeapFs(c1, c1heapIndex, ts1), dumpHeapFs(c2, c2heapIndex, ts2));
      return false;
    }
    
    private StringBuilder dumpHeapFs(CASImpl cas, final int iHeap, final TypeSystemImpl ts) {
      StringBuilder sb = new StringBuilder();
      typeInfo = ts.getTypeInfo(cas.getHeap().heap[iHeap]);
      sb.append(typeInfo);
  
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
   * @return
   * @throws FileNotFoundException
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
  
  public String printCasInfo(CASImpl cas) {
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
  
//  public void setDeserCas(CASImpl cas) {
//    deserCas = cas;
//  }
    
//  /**
//   * An iterator-like object for Feature Structures on the heap
//   * next() returns in order of ascending heap addresses those
//   * that correspond to string references
//   * 
//   * Returns -1 if no more string refs in this fs
//   * 
//   * Not currently used, but save in case String 
//   * update impl changes to no-longer always add
//   * new ref to end of string heap
//   */                  
//  private static class FsStringRefs {
//    
//    final boolean isStrArray;
//    int offset = 0;
//    final int length;
//    final int iHeap;
//    final int[] strRefOffsets;
//    
//    FsStringRefs(TypeInfo typeInfo, int[] heap, int iHeap) {
//      this.iHeap = iHeap;
//      isStrArray = (typeInfo.isHeapStoredArray && 
//                    typeInfo.getSlotKind(2) == Slot_StrRef);
//         
//      if (isStrArray) {
//        length = heap[iHeap + 1];
//        strRefOffsets = null;
//      } else {
//        strRefOffsets = typeInfo.strRefOffsets;
//        length = strRefOffsets.length;
//      }        
//    }
//    
//  
//    int next() {
//      if (offset < length) {
//        return iHeap + ((isStrArray) ? (2 + offset++) : strRefOffsets[offset++]);
//      } else {
//        return -1;
//      }
//    }
//  }
  

  /**
   * Manage the conversion of Items (FSrefs) to relative sequential index number, and back 
   * Manage the difference in two type systems
   *   both size of the FSs and
   *   handling excluded types
   * 
   * During serialization, these maps are constructed before serialization.
   * During deserialization, these maps are constructed while things are being deserialized, and
   *   then used in a "fixup" call at the end.
   *   This allows for forward references.
   *   
   * In addition to heap mappings between src/tgt, addr and sequential number, there are also mappings
   * computed for the case where the type systems do not match to account for holes in the aux heaps.
   * These holes are significant (to preserve and compute with) only when deserializing a delta cas,
   *   because then the input includes aux heap addresses relative to the target, which must be converted
   *   to equivalent addresses in the source being deserialized into.  
   *   
   * Maps from int to int
   *   address to/from sequential index for feature structures
   *   sequential index to/from sequential index for casTypeSystemMapping
   *   target index in aux heaps to source index
   */
  private static class ComprItemRefs {
    
    /**
     * map from a target FS sequence nbr to a source address.
     *   value is 0 if the target instance doesn't exist in the source
     *     (this doesn't occur for receiving remote CASes back
     *      (because src ts is always a superset of tgt ts),
     *      but can occur while deserializing from Disk.
     */
    final private IntVector tgtSeq2SrcAddr = new IntVector();
    
    /**
     * (Not Used, currently)
     * map from a source seq number to a target seq number.
     * value is -1 if the source FS is not in the target
     */
    final private IntVector srcSeq2TgtSeq = new IntVector();
    
    /**
     * (Not Used, currently)
     * map from a target seq number to a target address.
     */
    final private IntVector tgtSeq2TgtAddr = new IntVector();  // used for comparing
    
    /**
     * map from source address to target sequence number.
     * if source is not in target, value = -1;
     */
    final private Map<Integer, Integer>  srcAddr2TgtSeq = new HashMap<Integer, Integer>();
    
    /**
     * info needed to do a map from target aux heap to source aux heap
     * Used when applying delta modifications "below the line" to these elements
     *   Assumes any target ts element exists in source ts, so target is a subset
     *   (due to type merging, when delta cas is used to return updates from service)
     */
    
  
    /**
     * Indexed by AuxHeap kind: 
     */

    final private List<List<AuxSkip>> skips = new ArrayList<List<AuxSkip>>(AuxHeap.values().length);
    
    { // initialize instance block
      for (int i = 0; i < skips.size(); i++) {
        skips.add(new ArrayList<AuxSkip>());
      }
    }
   
    private int nextTgt = 0;

    public ComprItemRefs() {
      addItemAddr(0, 0, true, true);
    }
          
    /**
     * Add a new FS address - done during prescan of source
     * @param addr
     * @param inTarget true if this type is in the target
     */
    public void addItemAddr(int srcAddr, int tgtAddr, boolean inTarget, boolean isCompareCall) {
      int i = nextTgt;
      if (inTarget) {
        tgtSeq2SrcAddr.add(srcAddr);
        tgtSeq2TgtAddr.add(tgtAddr);
      }
      srcAddr2TgtSeq.put(srcAddr, inTarget ? i : 0);
//      // debug
//      if (srcAddr < 525) {
//        System.out.format("Adding to srcAddr2TgtSeq: addr: %d tgtSeq: %d, type=%s%n", srcAddr, inTarget ? i : 0, 
//           );
//      }
      srcSeq2TgtSeq.add(inTarget ? nextTgt++ : 0);
    }
    
    /**
     * record skipped entries in an Aux heap
     * @param auxHeap which heap this is for
     * @param srcSkipIndex the index of the first skipped slot in the src heap
     * @param srcSkipSize the number of entries skipped
     */
    public void recordSkippedAuxHeap(AuxHeap auxHeap, int srcSkipIndex, int srcSkipSize) {
      skips.get(auxHeap.ordinal()).add(new AuxSkip(srcSkipIndex, srcSkipSize));
    }
    
    /**
     * Called during deserialize to incrementally add 
     * @param srcAddr
     * @param inSrc
     */
    public void addSrcAddrForTgt(int srcAddr, boolean inSrc) {
      if (inSrc) {
        srcAddr2TgtSeq.put(srcAddr, nextTgt);
        srcSeq2TgtSeq.add(nextTgt);
        tgtSeq2SrcAddr.add(srcAddr);
      }
      tgtSeq2TgtAddr.add(-1);  // not used I hope - need to check TODO
      nextTgt++;
    }
                   
    public int getSrcAddrFromTgtSeq(int seq) {
      return tgtSeq2SrcAddr.get(seq);
    }

    public int getTgtAddrFromTgtSeq(int seq) {
      return tgtSeq2TgtAddr.get(seq);
    }

//    public int getMappedItemAddr(int index) {
//      if (null == typeMapper) {
//        return tgtIndexToSeq.get(index);
//      } else {
//        return tgtItemIndexToAddr.get(index);
//      }
//    }
    
    public int getTgtSeqFromSrcAddr(int itemAddr) {
      return srcAddr2TgtSeq.get(itemAddr);      
    }
    
    public int getNumberSrcFss() {
      return srcAddr2TgtSeq.size();
    }
  }
  
}
