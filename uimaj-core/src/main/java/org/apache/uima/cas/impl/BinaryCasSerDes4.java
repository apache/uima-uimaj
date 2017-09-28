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

import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.NBR_SLOT_KIND_ZIP_STREAMS;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_ArrayLength;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_Boolean;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_BooleanRef;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_Byte;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_ByteRef;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_Control;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_DoubleRef;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_Double_Exponent;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_Double_Mantissa_Sign;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_Float;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_Float_Exponent;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_Float_Mantissa_Sign;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_FsIndexes;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_HeapRef;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_Int;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_LongRef;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_Long_High;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_Long_Low;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_MainHeap;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_Short;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_ShortRef;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_StrChars;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_StrLength;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_StrOffset;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_StrRef;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_StrSeg;
import static org.apache.uima.cas.impl.BinaryCasSerDes4.SlotKind.Slot_TypeCode;

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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Marker;
import org.apache.uima.cas.impl.FSsTobeAddedback.FSsTobeAddedbackSingle;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasIOUtils;
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
 */
public class BinaryCasSerDes4 {
  
  private static final boolean TRACE_SER = false;
  private static final boolean TRACE_DES = false;
  
  public static final int TYPECODE_COMPR = 8;
  public static final boolean CHANGE_FS_REFS_TO_SEQUENTIAL = true;
  // may add more later - to specify differing trade-offs between speed and compression
  public enum Compression {None, Compress};  
  public static final boolean IS_DIFF_ENCODE = true;
  public static final boolean CAN_BE_NEGATIVE = true;
  public static final boolean IGNORED = true;
  public static final boolean IN_MAIN_HEAP = true;
  
  private static final long DBL_1 = Double.doubleToLongBits(1D);

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

    public final int i;
    public final boolean isDiffEncode;
    public final boolean canBeNegative;
    public final boolean inMainHeap;
    public final int elementSize;
    
    public static final int NBR_SLOT_KIND_ZIP_STREAMS;
    static {NBR_SLOT_KIND_ZIP_STREAMS = Slot_StrRef.i;}
    
    SlotKind(boolean isDiffEncode, 
             boolean canBeNegative, 
             int elementSize,
             boolean inMainHeap) {
      this.i = this.ordinal();
      this.isDiffEncode = isDiffEncode;
      this.canBeNegative = isDiffEncode ? true : canBeNegative;
      this.elementSize = elementSize; 
      this.inMainHeap = inMainHeap;
    }
  }
  
  /**
   * Things set up for one instance of this class, and
   * reuse-able
   */
  final private TypeInfo [] typeInfoArray;  // lazy initialization of elements
  final private TypeSystemImpl ts;  // for debugging
  final private boolean doMeasurements;
  
  // speedups
  final private static int arrayLength_i = Slot_ArrayLength.i;
  final private static int heapRef_i = Slot_HeapRef.i;
  final private static int int_i = Slot_Int.i;
  final private static int byte_i = Slot_Byte.ordinal();
  final private static int short_i = Slot_Short.i;
  final private static int typeCode_i = Slot_TypeCode.i;
  final private static int strOffset_i = Slot_StrOffset.i;
  final private static int strLength_i = Slot_StrLength.i;
  final private static int long_High_i = Slot_Long_High.i;
  final private static int long_Low_i = Slot_Long_Low.i;
  final private static int float_Mantissa_Sign_i = Slot_Float_Mantissa_Sign.i;
  final private static int float_Exponent_i = Slot_Float_Exponent.i;
  final private static int double_Mantissa_Sign_i = Slot_Double_Mantissa_Sign.i;
  final private static int double_Exponent_i = Slot_Double_Exponent.i;
  final private static int fsIndexes_i = Slot_FsIndexes.i;
  final private static int strChars_i = Slot_StrChars.i;
  final private static int control_i = Slot_Control.i;
  final private static int strSeg_i = Slot_StrSeg.i;
  
  /**
   * 
   * @param ts the type system
   * @param doMeasurements - normally set this to false. 
   */
  public BinaryCasSerDes4(TypeSystemImpl ts, boolean doMeasurements) {
    this.ts = ts;
    this.doMeasurements = doMeasurements;

    typeInfoArray = new TypeInfo[(ts.getTypeArraySize())];
  }

  /**
   * 
   * @param cas CAS to serialize
   * @param out output object
   * @param trackingMark tracking mark (for delta serialization)
   * @param compressLevel -
   * @param compressStrategy - 
   * @return null or serialization measurements (depending on setting of doMeasurements)
   * @throws IOException if the marker is invalid
   */
  public SerializationMeasures serialize(AbstractCas cas, Object out, Marker trackingMark,
      CompressLevel compressLevel, CompressStrat compressStrategy) throws IOException {
    SerializationMeasures sm = (doMeasurements) ? new SerializationMeasures() : null;
    CASImpl casImpl = (CASImpl) ((cas instanceof JCas) ? ((JCas)cas).getCas(): cas);
    if (null != trackingMark && !trackingMark.isValid() ) {
      throw new CASRuntimeException(
                CASRuntimeException.INVALID_MARKER, new String[] { "Invalid Marker." });
    }
    
    Serializer serializer = new Serializer(
        casImpl, makeDataOutputStream(out), (MarkerImpl) trackingMark, sm,
        compressLevel, compressStrategy, false);
   
    serializer.serialize();
    return sm;
  }
  
  public void serializeWithTsi(CASImpl casImpl, Object out) throws IOException {
    Serializer serializer = new Serializer(
        casImpl, makeDataOutputStream(out), null, null, CompressLevel.Default, CompressStrat.Default, true);
    serializer.serialize();
  }
  
  public SerializationMeasures serialize(AbstractCas cas, Object out, Marker trackingMark,
      CompressLevel compressLevel) throws IOException {
    return serialize(cas, out,trackingMark, compressLevel, CompressStrat.Default);
  }
  
  public SerializationMeasures serialize(AbstractCas cas, Object out, Marker trackingMark) throws IOException {
    return serialize(cas, out,trackingMark, CompressLevel.Default, CompressStrat.Default);
  }

  public SerializationMeasures serialize(AbstractCas cas, Object out) throws IOException {
    return serialize(cas, out, null);
  }

  public void deserialize(CASImpl cas, InputStream deserIn, boolean isDelta) throws IOException {
    DataInput in;
    if (deserIn instanceof DataInputStream) {
      in = (DataInputStream)deserIn;
    } else {
      in = new DataInputStream(deserIn);
    }
    Deserializer deserializer = new Deserializer(cas, in, isDelta);    
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
    final private StringHeap stringHeapObj;
    final private LongHeap longHeapObj;
    final private ShortHeap shortHeapObj;
    final private ByteHeap byteHeapObj;

    final private boolean isDelta;        // if true, there is a marker indicating the start spot(s)
    final private boolean isTsi;          // true to include the type system and indexes definition
    final private boolean doMeasurement;  // if true, doing measurements
    final private ComprItemRefs fsStartIndexes = (CHANGE_FS_REFS_TO_SEQUENTIAL) ? new ComprItemRefs() : null;
    final private int[] typeCodeHisto = new int[ts.getTypeArraySize()]; 
    final private Integer[] serializedTypeCode2Code = new Integer[ts.getTypeArraySize()]; // needs to be Integer to get comparator choice
    final private int[] estimatedZipSize = new int[NBR_SLOT_KIND_ZIP_STREAMS]; // one entry for each output stream kind
    final private OptimizeStrings os;
    final private CompressLevel compressLevel;
    final private CompressStrat compressStrategy;
    
    // typeInfo is local to this serialization instance to permit multiple threads
    private TypeInfo typeInfo; // type info for the current type being serialized
    private int iPrevHeap;        // 0 or heap addr of previous instance of current type
    private boolean only1CommonString;  // true if only one common string
    
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

    private Serializer(CASImpl cas, DataOutputStream serializedOut, MarkerImpl mark,
                       SerializationMeasures sm,
                       CompressLevel compressLevel,
                       CompressStrat compressStrategy,
                       boolean isTsi) {
      this.cas = cas;
      this.serializedOut = serializedOut;
      this.mark = mark;
      this.sm = sm;
      this.compressLevel = compressLevel;
      this.compressStrategy = compressStrategy;
      this.isTsi = isTsi;
      isDelta = (mark != null);
      doMeasurement = (sm != null);
      
      heap = cas.getHeap().heap;
      heapEnd = cas.getHeap().getCellsUsed();
      heapStart = isDelta ? mark.getNextFSId() : 0;
      
      stringHeapObj = cas.getStringHeap();
      longHeapObj   = cas.getLongHeap();
      shortHeapObj  = cas.getShortHeap();
      byteHeapObj   = cas.getByteHeap();
     
      os = new OptimizeStrings(doMeasurement);
      
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
      
      CommonSerDes.createHeader()
        .form4()
        .delta(isDelta)
        .typeSystemIndexDefIncluded(isTsi)
        .write(serializedOut);

      if (isTsi) {
        CasIOUtils.writeTypeSystem(cas, serializedOut, true);    
      }
      
      if (TRACE_SER) System.out.println("Form4Ser start, delta: " + (isDelta ? "true" : "false"));

      if (doMeasurement) {
        sm.header = 12;
      }
           
      /**************************
       * Strings
       **************************/
      int stringHeapStart = isDelta ? mark.nextStringHeapAddr : 1;
      int stringHeapEnd = stringHeapObj.getSize();
 
      for (int i = stringHeapStart; i < stringHeapEnd; i++) {
        os.add(stringHeapObj.getStringForCode(i));
      }
      
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
      writeVnumber(control_dos, heapEnd - heapStart);  
      if (TRACE_SER) System.out.println("Form4Ser heapstart: " + heapStart + "  heapEnd: " + heapEnd);
      
      if (doMeasurement) {
        sm.statDetails[Slot_MainHeap.i].original = (1 + heapEnd - heapStart) * 4;      
      }
      
      // debug - for delta
//      if (isDelta) {
//        int[] heap = cas.getHeap().heap;
//        for (int iHeap =1; iHeap < heapEnd; iHeap += incrToNextFs(heap, iHeap, typeInfo)) {
//          int tCode = heap[iHeap];  // get type code      
//          typeInfo = getTypeInfo(tCode);
//          System.out.format("debug heapAddr: %,d type: %s%n", iHeap, typeInfo.type.getShortName());
//          if (iHeap == 439) {
//            System.out.println("debug");
//          }
//        }
//        System.out.format("debug heapStart: %,d heapEnd: %,d", heapStart, heapEnd);
//      }
      
      
      resetIprevious();

      if (heapStart == 0) {
        heapStart = 1;  // slot 0 not serialized, it's null / 0
      }

      if (CHANGE_FS_REFS_TO_SEQUENTIAL) {
        // scan thru all fs and save their offsets in the heap
        // to allow conversion from addr to sequential fs numbers
        initFsStartIndexes(fsStartIndexes, heap, heapStart, heapEnd, typeCodeHisto);
        
        for (int i = ts.getTypeArraySize() - 1; i >= 0; i--) {
          serializedTypeCode2Code[i] = i;
        }
        
        // set typeCode2serializeCode so that the 0th element is the typeCode with the highest frequency, etc.
        Arrays.sort(serializedTypeCode2Code, 0, serializedTypeCode2Code.length, new Comparator<Integer>() {
          public int compare(Integer o1, Integer o2) {
            return (typeCodeHisto[o1] > typeCodeHisto[o2]) ? -1 :
                   (typeCodeHisto[o1] < typeCodeHisto[o2]) ? 1 : 0;
          }
        });
        
//        for (int i = 0; i < serializedTypeCode2Code.length; i++) {
//          int tCode = serializedTypeCode2Code[i];
//          int c = typeCodeHisto[tCode];
//          if (c > 0) {
//            System.out.format("%2d %,9d instance of Type %s%n", i, c, typeInfoArray[tCode]);
//          }
//        }
        
      }

      
      
      /***************************
       * walk main heap
       ***************************/

      for (int iHeap = heapStart; iHeap < heapEnd; iHeap += incrToNextFs(heap, iHeap, typeInfo)) {
        int tCode = heap[iHeap];  // get type code      
        typeInfo = getTypeInfo(tCode);
        iPrevHeap = typeInfo.iPrevHeap;
        
        writeVnumber(typeCode_dos, tCode);

        if (typeInfo.isHeapStoredArray) {
          serializeHeapStoredArray(iHeap);
        } else if (typeInfo.isArray) {
          serializeNonHeapStoredArray(iHeap);
        } else {
          for (int i = 1; i < typeInfo.slotKinds.length + 1; i++) {
            serializeByKind(iHeap, i);
          }
        }
      
        typeInfo.iPrevHeap = iHeap;  // make this one the "prev" one for subsequent testing
        if (doMeasurement) {
          sm.statDetails[typeCode_i].incr(DataIO.lengthVnumber(tCode));
          sm.mainHeapFSs ++;
        }
      }  // end of heap walk
      
      if (TRACE_SER) System.out.println("Form4Ser writing index info");
      serializeIndexedFeatureStructures();

      if (isDelta) {
        if (TRACE_SER) System.out.println("Form4Ser writing modified FSs");
        (new SerializeModifiedFSs()).serializeModifiedFSs();
      }

      collectAndZip();
      
      if (doMeasurement) {
        sm.totalTime = System.currentTimeMillis() - sm.totalTime;
      }
    }
    
    
    private void serializeIndexedFeatureStructures() throws IOException {
      int[] fsIndexes = isDelta ? cas.getDeltaIndexedFSs(mark) : cas.getIndexedFSs();
      if (doMeasurement) {
        sm.statDetails[fsIndexes_i].original = fsIndexes.length * 4 + 1;      
      }
      int nbrViews = fsIndexes[0];
      int nbrSofas = fsIndexes[1];
      writeVnumber(control_i, nbrViews);
      writeVnumber(control_i, nbrSofas);
      
      if (doMeasurement) {
        sm.statDetails[fsIndexes_i].incr(1); // an approximation - probably correct
        sm.statDetails[fsIndexes_i].incr(1);
      }
      
      int fi = 2;
      final int end1 = nbrSofas + 2;
      for (; fi < end1; fi++) {
        writeVnumber(control_i, fsIndexes[fi]);  // not converted to sequential
        
        if (doMeasurement) {
          sm.statDetails[fsIndexes_i].incr(DataIO.lengthVnumber(fsIndexes[fi]));
        }
      }
       
      for (int vi = 0; vi < nbrViews; vi++) {
        fi = compressFsxPart(fsIndexes, fi);    // added FSs  // compress converts to sequential
        if (isDelta) {
          fi = compressFsxPart(fsIndexes, fi);  // removed FSs
          fi = compressFsxPart(fsIndexes, fi);  // reindexed FSs
        }
      }      
    }

    private int compressFsxPart(int[] fsIndexes, int fsNdxStart) throws IOException {
      int ix = fsNdxStart;
      int nbrEntries = fsIndexes[ix++];
      int end = ix + nbrEntries;
      writeVnumber(fsIndexes_dos, nbrEntries);  // number of entries
      if (doMeasurement) {
        sm.statDetails[typeCode_i].incr(DataIO.lengthVnumber(nbrEntries));
      }
      
      final int[] ia = new int[nbrEntries];
      System.arraycopy(fsIndexes, ix, ia, 0, nbrEntries);
      Arrays.sort(ia);
     
      int prev = 0;
      
      for (int i = 0; i < ia.length; i++) {
        int v = ia[i];
        if (CHANGE_FS_REFS_TO_SEQUENTIAL) {
          v = fsStartIndexes.getItemIndex(v);
        }
        writeVnumber(fsIndexes_dos, v - prev);
        if (doMeasurement) {
          sm.statDetails[fsIndexes_i].incr(DataIO.lengthVnumber(v - prev));
        }
        prev = v;
        
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
                     (heap[iPrevHeap + 1] == 0) ? 0 :
                      heap[iPrevHeap + 2]; 
          for (int i = iHeap + 2; i < endi; i++) {
            prev = writeIntOrHeapRef(arrayElementKind.i, i, prev);
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
      writeDiff(kind.i, heap[iHeap + offset], prev);
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
     *   Length &gt; 0 (subtract 1): used for actual string length
     *   
     *   Length &lt; 0 - use (-length) as slot index  (minimum is 1, slot 0 is NULL)
     *   
     *   For length &gt; 0, write also the offset.
     *   
     * @throws IOException passthru  
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
     * @param raw the number to write
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
     * @param kind the kind of slot
     * @param i  runs from iHeap + 3 to end of array
     * @throws IOException passthru
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
    
      if (CHANGE_FS_REFS_TO_SEQUENTIAL && (kind == heapRef_i)) {
        v = fsStartIndexes.getItemIndex(v);
        if (prev != 0) {
          prev = fsStartIndexes.getItemIndex(prev);
        }
      }

      final int absV = Math.abs(v);
      if (((v > 0) && (prev > 0)) ||
          ((v < 0) && (prev < 0))) {
        final int diff = v - prev;  // guaranteed not to overflow
//      Math.abs of Integer.MIN_VALUE + 1 sometimes (after jit?) (on some JVMs) gives wrong annswer
        // failure observed on IBM Java 7 SR1 and SR2  3/28/2013 schor
        // failure only observed when running entire suite of uimaj-core tests via eclipse - mvn test doesn't fail
//        final int absDiff = Math.abs(diff);
        // this seems to work around
        final int absDiff = (diff < 0) ? -diff : diff; 
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
     *     write the heap addr of the FS
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

      {sortModifications();}
      
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
        iPrevHeap = 0;   
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
          typeInfo = getTypeInfo(tCode);
          
          // write out the address of the modified FS
          writeVnumber(fsIndexes_dos, iHeap - iPrevHeap);
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
    
    final private CASImpl cas;  // cas being deserialized into
    final private DataInput deserIn;

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
     * Cache sharable common values in aux heaps
     * Values must be in aux heap, but not part of arrays there
     *   so that rules out boolean, byte, and shorts
     */
    private int longZeroIndex = -1; // also used for double 0 indix
    private int double1Index = -1;

    final private boolean isDelta;        // if true, a delta is being deserialized
    final private ComprItemRefs fsStartIndexes = (CHANGE_FS_REFS_TO_SEQUENTIAL) ? new ComprItemRefs() : null;
    private String[] readCommonString;

    private TypeInfo typeInfo; // type info for the current type being serialized

    private int iPrevHeap;        // 0 or heap addr of previous instance of current type
    private boolean only1CommonString;

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
     * @param cas CAS
     * @param deserIn input data
     * @throws IOException passthru
     */
    Deserializer(CASImpl cas, DataInput deserIn, boolean isDelta) throws IOException {
      this.cas = cas;
      this.deserIn = deserIn;
      this.isDelta = isDelta;
      
      stringHeapObj = cas.getStringHeap();
      longHeapObj   = cas.getLongHeap();
      shortHeapObj  = cas.getShortHeap();
      byteHeapObj   = cas.getByteHeap();

//      deserIn.readInt();    // reserved to record additional version info  // already read before calling
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
    
    private void deserialize() throws IOException {
      if (TRACE_DES) System.out.println("Form4Deser starting");
     
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

      resetIprevious();
      
      if (heapStart == 0) {
        heapStart = 1;  // slot 0 not serialized, it's null / 0
      }

      if (CHANGE_FS_REFS_TO_SEQUENTIAL && (heapStart > 1)) {
        initFsStartIndexes(fsStartIndexes, heap, 1, heapStart, null);
      }
      fixupsNeeded = new IntVector(Math.max(16, heap.length / 10));

      /***************************
       * walk main heap
       ***************************/

      if (TRACE_DES) System.out.println("Form4Deser heapStart: " + heapStart + "  heapEnd: " + heapEnd);
      for (int iHeap = heapStart; iHeap < heapEnd; iHeap += incrToNextFs(heap, iHeap, typeInfo)) {
        if (CHANGE_FS_REFS_TO_SEQUENTIAL) {
          fsStartIndexes.addItemAddr(iHeap);
        }        
        int tCode = heap[iHeap] = readVnumber(typeCode_dis); // get type code      
        typeInfo = getTypeInfo(tCode);
        iPrevHeap = typeInfo.iPrevHeap;

        if (typeInfo.isHeapStoredArray) {
          readHeapStoredArray(iHeap);
        } else if (typeInfo.isArray) {
          readNonHeapStoredArray(iHeap);
        } else {
          for (int i = 1; i < typeInfo.slotKinds.length + 1; i++) {
            readByKind(iHeap, i);
          }
        }
        
        typeInfo.iPrevHeap = iHeap;  // make this one the "prev" one for subsequent testing
      }
      
      if (CHANGE_FS_REFS_TO_SEQUENTIAL) {
        fsStartIndexes.finishSetup();
        final int end = fixupsNeeded.size();
        for (int i = 0; i < end; i++) {
          final int heapAddrToFix = fixupsNeeded.get(i);
          heap[heapAddrToFix] = fsStartIndexes.getItemAddr(heap[heapAddrToFix]);
        }        
      }
      
      if (TRACE_DES) System.out.println("Form4Deser indexing FSs");
      readIndexedFeatureStructures();

      if (isDelta) {
        if (TRACE_DES) System.out.println("Form4Deser modifying existing FSs");
        (new ReadModifiedFSs()).readModifiedFSs();
      }

      closeDataInputs();
//      System.out.format("Deserialize took %,d ms%n", System.currentTimeMillis() - startTime1);
    }
    
    private void readNonHeapStoredArray(int iHeap) throws IOException {
      final int length = readArrayLength(iHeap);
      if (length == 0) {
        return;
      }
      SlotKind refKind = typeInfo.getSlotKind(2);
      switch (refKind) {
      case Slot_BooleanRef: case Slot_ByteRef:
        heap[iHeap + 2] = readIntoByteArray(length);
        break; 
      case Slot_ShortRef:
        heap[iHeap + 2] = readIntoShortArray(length);
        break; 
      case Slot_LongRef: 
      case Slot_DoubleRef:
        heap[iHeap + 2] = readIntoLongArray(refKind, length);
        break;
        
      default:
        throw new RuntimeException();
      }
    }
    
    private int readArrayLength(int iHeap) throws IOException {
      return heap[iHeap + 1] = readVnumber(arrayLength_dis);
    }

    private void readHeapStoredArray(int iHeap) throws IOException {
      final int length = readArrayLength(iHeap);
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
                     (heap[iPrevHeap + 1] == 0) ? 0 :
                      heap[iPrevHeap + 2]; 
          for (int i = iHeap + 2; i < endi; i++) {
            final int v = heap[i] = readDiff(arrayElementKind, prev);
            prev = v;
            if (arrayElementKind == Slot_HeapRef) {
              fixupsNeeded.add(i);
            }
          }
        }
        break;
      case Slot_Float: 
        for (int i = iHeap + 2; i < endi; i++) {
          heap[i] = readFloat();
        }
        break;
      case Slot_StrRef:
        for (int i = iHeap + 2; i < endi; i++) {
          heap[i] = readString();
        }
        break;
        
      default: throw new RuntimeException("internal error");
      } // end of switch    
    }
          
    private void readByKind(int iHeap, int offset) throws IOException {
      SlotKind kind = typeInfo.getSlotKind(offset);
      
      switch (kind) {
      case Slot_Int: case Slot_Short:
        readDiffWithPrevTypeSlot(kind, iHeap, offset);
        break;
      case Slot_Float:
        heap[iHeap + offset] = readFloat();
        break;
      case Slot_Boolean: case Slot_Byte:
        heap[iHeap + offset] = byte_dis.readByte();
        break;
      case Slot_HeapRef:
        readDiffWithPrevTypeSlot(kind, iHeap, offset);
        if (kind == Slot_HeapRef) {
          fixupsNeeded.add(iHeap + offset);
        }
        break;
      case Slot_StrRef: 
        heap[iHeap + offset] = readString();
        break;
      case Slot_LongRef: {
        long v = readLong(kind, (iPrevHeap == 0) ? 0L : longHeapObj.getHeapValue(heap[iPrevHeap + offset]));
        if (v == 0L) {
          if (longZeroIndex == -1) {
            longZeroIndex = longHeapObj.addLong(0L);
          }
          heap[iHeap + offset] = longZeroIndex;
        } else {
          heap[iHeap + offset] = longHeapObj.addLong(v);
        }
        break;
      }
      case Slot_DoubleRef: {
        long v = readDouble();
        if (v == 0L) {
          if (longZeroIndex == -1) {
            longZeroIndex = longHeapObj.addLong(0L);
          }
          heap[iHeap + offset] = longZeroIndex;
        } else if (v == DBL_1) {
          if (double1Index == -1) {
            double1Index = longHeapObj.addLong(DBL_1);
          }
          heap[iHeap + offset] = double1Index;
        } else {
          heap[iHeap + offset] = longHeapObj.addLong(v);
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
        // length is too long, but is never accessed
        cas.reinitDeltaIndexedFSs(fsIndexes.getArray());
      } else {
        cas.reinitIndexedFSs(fsIndexes.getArray());
      }
    }

    /*
     * Each FS index is sorted, and output is by delta 
     */
    private void readFsxPart(IntVector fsIndexes) throws IOException {
      final int nbrEntries = readVnumber(fsIndexes_dis);
      fsIndexes.add(nbrEntries);      
      int prev = 0;
      
      for (int i = 0; i < nbrEntries; i++) {
        int v = readVnumber(fsIndexes_dis) + prev;
        prev = v;
        if (CHANGE_FS_REFS_TO_SEQUENTIAL) {
          v = fsStartIndexes.getItemAddr(v);
        }
        fsIndexes.add(v);
      }
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
      int zipBufSize = Math.max(1024, bytesCompr);
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
      return dataInputs[kind.i];
    }

    private int readVnumber(DataInputStream dis) throws IOException {
      return DataIO.readVnumber(dis);
    }

    private long readVlong(DataInputStream dis) throws IOException {
      return DataIO.readVlong(dis);
    }

    private int readIntoByteArray(int length) throws IOException {
      int startPos = byteHeapObj.reserve(length);
      byte_dis.readFully(byteHeapObj.heap, startPos, length);
      return startPos;
    }

    private int readIntoShortArray(int length) throws IOException {
      final int startPos = shortHeapObj.reserve(length);
      final short[] h = shortHeapObj.heap;
      final int endPos = startPos + length;
      short prev = 0;
      for (int i = startPos; i < endPos; i++) {
        h[i] = prev = (short)(readDiff(short_dis, prev));
      }
      return startPos;   
    }
    
    private int readIntoLongArray(SlotKind kind, int length) throws IOException {
      final int startPos = longHeapObj.reserve(length);
      final long[] h = longHeapObj.heap;
      final int endPos = startPos + length;
      long prev = 0;
      for (int i = startPos; i < endPos; i++) {
        h[i] = prev = readLong(kind, prev);
      }
      return startPos;   
    }

    private void readDiffWithPrevTypeSlot(SlotKind kind, int iHeap, int offset) throws IOException {
      int prev = (iPrevHeap == 0) ? 0 : heap[iPrevHeap + offset];
      heap[iHeap + offset] = readDiff(kind, prev);
    }

    private int readDiff(SlotKind kind, int prev) throws IOException {
      return readDiff(getInputStream(kind), prev);
    }
    
    private int readDiff(DataInput in, int prev) throws IOException {
      final long encoded = readVlong(in);
      final boolean isDelta = (0 != (encoded & 1L));
      final boolean isNegative = (0 != (encoded & 2L));
      int v = (int)(encoded >>> 2);
      if (isNegative) {
        if (v == 0) {
          return Integer.MIN_VALUE;
        }
        v = -v;
      }
      if (isDelta) {
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
      
    private int readString() throws IOException {
      int length = decodeIntSign(readVnumber(strLength_dis));
      if (0 == length) {
        return 0;
      }
      if (1 == length) {
        return stringHeapObj.addString("");
      }
      
      if (length < 0) {  // in this case, -length is the slot index
        return stringTableOffset - length;
      }
      int offset = readVnumber(strOffset_dis);
      int segmentIndex = (only1CommonString) ? 0 :
        readVnumber(strSeg_dis);
      String s =  readCommonString[segmentIndex].substring(offset, offset + length - 1);
      return stringHeapObj.addString(s);
    }

    /******************************************************************************
     * Modified Values
     * 
     * Modified heap values need fsStartIndexes conversion
     ******************************************************************************/

    private class ReadModifiedFSs {
      
      // previous value - for things diff encoded
      private int vPrevModInt = 0;
      private int vPrevModHeapRef = 0;
      private short vPrevModShort = 0;
      private long vPrevModLong = 0;
      private int iHeap;
      private TypeInfo typeInfo;
      
      // next for managing index removes / readds
      private boolean wasRemoved;
      private FSsTobeAddedbackSingle addbackSingle;
      private int[] featCodes;

      private void readModifiedFSs() throws IOException {
        final int modFSsLength = readVnumber(control_dis);
        iPrevHeap = 0;
                 
        for (int i = 0; i < modFSsLength; i++) {
          iHeap = readVnumber(fsIndexes_dis) + iPrevHeap;
          iPrevHeap = iHeap;
  
          final int tCode = heap[iHeap];
          typeInfo = getTypeInfo(tCode);
          
          final int numberOfModsInThisFs = readVnumber(fsIndexes_dis); 
  
          /**************************************************
           * handle aux byte, short, long array modifications
           **************************************************/
          if (typeInfo.isArray && (!typeInfo.isHeapStoredArray)) {
            readModifiedAuxHeap(numberOfModsInThisFs);
          } else {
            // https://issues.apache.org/jira/browse/UIMA-4100
            // see if any of the mods are keys
            featCodes = cas.getTypeSystemImpl().ll_getAppropriateFeatures(tCode);
//            cas.removeFromCorruptableIndexAnyView(iHeap, indexToDos);
            try {
              wasRemoved = false;
              readModifiedMainHeap(numberOfModsInThisFs);
            } finally {
              cas.addbackSingle(iHeap);
            }
          }
        }
      }
      
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
        int iPrevOffsetInFs = 0;
        
        wasRemoved = false;  // set to true when removed from index to stop further testing
        addbackSingle = cas.getAddbackSingle();
        addbackSingle.clear();
        

        for (int i = 0; i < numberOfMods; i++) {
          final int offsetInFs = readVnumber(fsIndexes_dis) + iPrevOffsetInFs;
          iPrevOffsetInFs = offsetInFs;
          final SlotKind kind = typeInfo.getSlotKind(typeInfo.isArray ? 2 : offsetInFs);
          
          switch (kind) {
          case Slot_HeapRef: {
              int v = readDiff(heapRef_dis, vPrevModHeapRef);
              vPrevModHeapRef = v;
              if (CHANGE_FS_REFS_TO_SEQUENTIAL) {
                v = fsStartIndexes.getItemAddr(v);
              }
              heap[iHeap + offsetInFs] = v;
            }
            break;
          case Slot_Int: {
              final int v = readDiff(int_dis, vPrevModInt);
              vPrevModInt = v;
              heap[iHeap + offsetInFs] = v;
              maybeRemove(offsetInFs);
            }
            break;
          case Slot_Short: {
              final int v = readDiff(int_dis, vPrevModShort);
              vPrevModShort = (short)v;
              heap[iHeap + offsetInFs] = v;
            }
            break;
          case Slot_LongRef: case Slot_DoubleRef: {
              final long v = readLong(kind, vPrevModLong);
              if (kind == Slot_LongRef) {
                vPrevModLong = v;
              }
              heap[iHeap + offsetInFs] = longHeapObj.addLong(v);
            }
            break;
          case Slot_Byte: case Slot_Boolean:
            heap[iHeap + offsetInFs] = byte_dis.readByte();
            break;
          case Slot_Float:
            heap[iHeap + offsetInFs] = readFloat();
            maybeRemove(offsetInFs);
            break;
          case Slot_StrRef:
            heap[iHeap + offsetInFs] = readString();
            maybeRemove(offsetInFs);
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
  }

  /********************************************************************
   * methods common to serialization / deserialization etc.
   ********************************************************************/
  
  private int incrToNextFs(int[] heap, int iHeap, TypeInfo typeInfo) {
    if (typeInfo.isHeapStoredArray) {
      return 2 + heap[iHeap + 1];
    } else {
      return 1 + typeInfo.slotKinds.length;
    }
  }

  
  private void initFsStartIndexes (final ComprItemRefs fsStartIndexes, final int[] heap, int heapStart, int heapEnd, int[] histo) {
    for (int iHeap = 1; iHeap < heapEnd;) {
      fsStartIndexes.addItemAddr(iHeap);
      final int tCode = heap[iHeap];
      if ((null != histo) && (iHeap >= heapStart)) {
        histo[tCode] ++;
      }
      TypeInfo typeInfo = getTypeInfo(tCode);
      iHeap += incrToNextFs(heap, iHeap, typeInfo);
    }
    fsStartIndexes.finishSetup();
  }  

  private void resetIprevious() {
    for (int i = 1; i < typeInfoArray.length; i++) {
      TypeInfo typeInfo = typeInfoArray[i];  // skip 0 which is null
      if (null != typeInfo) {
        typeInfo.iPrevHeap = 0;
      }
    }
  } 

  // this method is required, instead of merely making
  // a "new" instance, so that
  // the containing instance of BinaryCasSerDes4 can be
  // accessed for the type info
  
  public CasCompare getCasCompare() {
    return new CasCompare();
  }
  
  public class CasCompare {
    /** 
     * Compare 2 CASes for equal
     * The layout of refs to aux heaps does not have to match
     */
      private CASImpl c1;
      private CASImpl c2;
      private Heap c1HO;
      private Heap c2HO;
      private int[] c1heap;
      private int[] c2heap;
      private TypeInfo typeInfo;
      private int iHeap;
      
    public boolean compareCASes(CASImpl c1, CASImpl c2) {
      this.c1 = c1;
      this.c2 = c2;
      c1HO = c1.getHeap();
      c2HO = c2.getHeap();
      final int endi = c1HO.getCellsUsed();
      final int end2 = c2HO.getCellsUsed();
      if (endi != end2) {
        System.err.format("CASes have different heap cells used: %,d %,d%n", endi, end2);
      }
      c1heap = c1HO.heap;
      c2heap = c2HO.heap;
      
      final ComprItemRefs fsStartIndexes = new ComprItemRefs();
      initFsStartIndexes(fsStartIndexes, c1heap, 1, endi, null);
      
      final int endsi = fsStartIndexes.getNbrOfItems();
      for (int i = 1; i < endsi; i++) {
        iHeap = fsStartIndexes.getItemAddr(i);
//        System.out.println("");
        if (!compareFss()) {
          return false;
        }
      }
      
      int[] ifs1 = c1.getIndexedFSs();
      int[] ifs2 = c2.getIndexedFSs();
      
      return Arrays.equals(ifs1, ifs2);
    }

    private boolean compareFss() {
      int tCode = c1heap[iHeap];
      typeInfo = getTypeInfo(tCode);
      if (tCode != c2heap[iHeap]) {
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
      int len1 = c1heap[iHeap + 1];
      int len2 = c2heap[iHeap + 1];
      if (len1 != len2) {
        return false;
      }
      for (int i = 0; i < len1; i++) {
        SlotKind kind = typeInfo.getSlotKind(2);
        if (typeInfo.isHeapStoredArray) {
          if (kind == Slot_StrRef) {
            if (! compareStrings(c1.getStringForCode(c1heap[iHeap + 2 + i]),
                                 c2.getStringForCode(c2heap[iHeap + 2 + i]))) {
              return mismatchFs();
            }
          } else if (c1heap[iHeap + 2 + i] != c2heap[iHeap + 2 + i]) {
            return mismatchFs();
          }
        } else {  // not heap stored array
          switch (kind) {
          case Slot_BooleanRef: case Slot_ByteRef:
            if (c1.getByteHeap().getHeapValue(c1heap[iHeap + 2] + i) !=
                c2.getByteHeap().getHeapValue(c2heap[iHeap + 2] + i)) {
              return mismatchFs(); 
            }
            break;
          case Slot_ShortRef:
            if (c1.getShortHeap().getHeapValue(c1heap[iHeap + 2] + i) !=
                c2.getShortHeap().getHeapValue(c2heap[iHeap + 2] + i)) {
              return mismatchFs();
            }
            break;
          case Slot_LongRef: case Slot_DoubleRef: {
            if (c1.getLongHeap().getHeapValue(c1heap[iHeap + 2] + i)  !=
                c2.getLongHeap().getHeapValue(c2heap[iHeap + 2] + i)) {
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
        return c1heap[iHeap + offset] == c2heap[iHeap + offset];
      case Slot_StrRef:
        return compareStrings(c1.getStringForCode(c1heap[iHeap + offset]),
                              c2.getStringForCode(c2heap[iHeap + offset]));
      case Slot_LongRef: case Slot_DoubleRef:
        return c1.getLongHeap().getHeapValue(c1heap[iHeap + offset]) ==
               c2.getLongHeap().getHeapValue(c2heap[iHeap + offset]);
      default: throw new RuntimeException("internal error");      
      }
    }
    
    private boolean compareStrings(String s1, String s2) {
      if (null == s1) {
        return null == s2;
      }
      return s1.equals(s2);
    }
     
    private boolean mismatchFs() {
      System.err.format("Mismatched Feature Structures:%n %s%n %s%n", 
          dumpHeapFs(c1), dumpHeapFs(c2));
      return false;
    }
    
    private StringBuilder dumpHeapFs(CASImpl cas) {
      StringBuilder sb = new StringBuilder();
      typeInfo = getTypeInfo(cas.getHeap().heap[iHeap]);
      sb.append(typeInfo);
  
      if (typeInfo.isHeapStoredArray) {
        sb.append(dumpHeapStoredArray(cas));
      } else if (typeInfo.isArray) {
        sb.append(dumpNonHeapStoredArray(cas));
      } else {
        sb.append("   Slots:\n");
        for (int i = 1; i < typeInfo.slotKinds.length + 1; i++) {
          sb.append("  ").append(typeInfo.getSlotKind(i)).append(": ")
              .append(dumpByKind(cas, i)).append('\n');
        }
      }
      return sb;
    }
    
    private StringBuilder dumpHeapStoredArray(CASImpl cas) {
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
  
    private StringBuilder dumpNonHeapStoredArray(CASImpl cas) {
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
  
    private StringBuilder dumpByKind(CASImpl cas, int offset) {
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
    
  private TypeInfo getTypeInfo(int typeCode) {
    if (null == typeInfoArray[typeCode]) {
      initTypeInfoArray(typeCode);
    }
    return typeInfoArray[typeCode];
  }
  
  private void initTypeInfoArray(int typeCode) {
    TypeImpl type = (TypeImpl) ts.ll_getTypeForCode(typeCode);
    typeInfoArray[typeCode] = new TypeInfo(type, ts);
  }

  
  private static class TypeInfo {
 // constant data about a particular type  
    public final TypeImpl type;   // for debug
    public final SlotKind[] slotKinds; 
    public final int[] strRefOffsets;
    
    public final boolean isArray;
    public final boolean isHeapStoredArray;  // true if array elements are stored on the main heap
    // memory while compressing/decompressing
    public int iPrevHeap;   // index of where this fs type occurred in the heap previously

    public TypeInfo(TypeImpl type, TypeSystemImpl ts) {
      
      this.type = type;
      List<Feature> features = type.getFeatures();

      isArray = type.isArray();  // feature structure array types named type-of-fs[]
      isHeapStoredArray = (type == ts.intArrayType) ||
                          (type == ts.floatArrayType) ||
                          (type == ts.fsArrayType) ||
                          (type == ts.stringArrayType) ||
                          (TypeSystemImpl.isArrayTypeNameButNotBuiltIn(type.getName()));

      final ArrayList<Integer> strRefsTemp = new ArrayList<Integer>();
      // set up slot kinds
      if (isArray) {
        // slotKinds has 2 slots: 1st is for array length, 2nd is the slotkind for the array element
        SlotKind arrayKind;
        if (isHeapStoredArray) {
          if (type == ts.intArrayType) {
            arrayKind = Slot_Int;
          } else if (type == ts.floatArrayType) {
            arrayKind = Slot_Float;
          } else if (type == ts.stringArrayType) {
            arrayKind = Slot_StrRef;
          } else {
            arrayKind = Slot_HeapRef;
          }
        } else { 
          
          // array, but not heap-store-array
          if (type == ts.booleanArrayType ||
              type == ts.byteArrayType) {
            arrayKind = Slot_ByteRef;
          } else if (type == ts.shortArrayType) {
            arrayKind = Slot_ShortRef;
          } else if (type == ts.longArrayType) {
            arrayKind = Slot_LongRef;
          } else if (type == ts.doubleArrayType) {
            arrayKind = Slot_DoubleRef;
          } else {
            throw new RuntimeException("never get here");
          }
        }
        
        slotKinds = new SlotKind[] {Slot_ArrayLength, arrayKind};
        strRefOffsets = null;
        
      } else {
        
        // set up slot kinds for non-arrays
        ArrayList<SlotKind> slots = new ArrayList<SlotKind>();
        int i = -1;
        for (Feature feat : features) {
          i++;
          TypeImpl slotType = (TypeImpl) feat.getRange();
          
          if (slotType == ts.stringType || (slotType instanceof StringTypeImpl)) {
            slots.add(Slot_StrRef);
            strRefsTemp.add(i); 
          } else if (slotType == ts.intType) {
            slots.add(Slot_Int);
          } else if (slotType == ts.booleanType) {
            slots.add(Slot_Boolean);
          } else if (slotType == ts.byteType) {
            slots.add(Slot_Byte);
          } else if (slotType == ts.shortType) {
            slots.add(Slot_Short);
          } else if (slotType == ts.floatType) {
            slots.add(Slot_Float);
          } else if (slotType == ts.longType) {
            slots.add(Slot_LongRef);
          } else if (slotType == ts.doubleType) {
            slots.add(Slot_DoubleRef);
          } else {
            slots.add(Slot_HeapRef);
          } 
        } // end of for loop 
        slotKinds = slots.toArray(new SlotKind[slots.size()]);
        // convert to int []
        strRefOffsets = new  int[strRefsTemp.size()];
        for (int i2 = 0; i2 < strRefOffsets.length; i2++) {
          strRefOffsets[i2] = strRefsTemp.get(i2);
        }
      }
    }
        
    public SlotKind getSlotKind(int offset) { 
      if (0 == offset) {
        return Slot_TypeCode;
      }
      return slotKinds[offset - 1];
    }

    @Override
    public String toString() {
      return type.toString();
    }
    
  }
  
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
   * Manage the conversion of Items (FSrefs or String offsets) to relative index number
   * 
   * Map from int to int
   *  Fs:
   *   key = index into heap, value = fs index  <<< a search
   *   key = fs index, value = index into heap  <<< just an array ref
   *  StrOffset: 
   *   key = string offset, value = str index  <<< a search
   *   key = str index, value = string offset (index into strings)  <<< just an array ref
   *   
   *   take advantage: both keys / indexes monotonically increasing
   *                   most refs nearby
   *                   spacing fairly uniform
   *                   
   *     Do modified binary search - 
   *       - estimate first probe:  avg of % & current loc
   * 
   * 
   * Lifecycle:
   *   1) create an instance
   *   2) fill
   *   3) finish
   *   4) do gets
   *   gc
   */
  private static class ComprItemRefs {
    
    final private IntVector itemIndexToAddr = new IntVector();  // item is feature structure or string segment
    // can't use Int2IntHashMap here because 0 is stored and used as a value
    final private Map<Integer, Integer>  itemAddrToIndex = new HashMap<Integer, Integer>();
    
    public ComprItemRefs() {
      addItemAddr(0);
    }
          
    public void addItemAddr(int v) {
      int i = itemIndexToAddr.size();
      itemIndexToAddr.add(v);
      itemAddrToIndex.put(v, i);
    }
    
    public int getNbrOfItems() {
      return itemIndexToAddr.size();
    }
    
    /**
     * call after fsAddrs is loaded
     * Currently has no purpose due to change
     * of internal impl
     */
    public void finishSetup() {    
    }
            
    public int getItemAddr(int index) {
      return itemIndexToAddr.get(index);
    }
    
    public int getItemIndex(int itemAddr) {
      return itemAddrToIndex.get(itemAddr);      
    }
  }
  
}
