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

import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_DoubleRef;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_HeapRef;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Int;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_LongRef;

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
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.uima.UimaSerializable;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.CASImpl.FsChange;
import org.apache.uima.cas.impl.CommonSerDes.Header;
import org.apache.uima.cas.impl.FSsTobeAddedback.FSsTobeAddedbackSingle;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;
import org.apache.uima.internal.util.Int2ObjHashMap;
import org.apache.uima.internal.util.IntListIterator;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.PositiveIntSet;
import org.apache.uima.internal.util.PositiveIntSet_impl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.AutoCloseableNoException;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.CasLoadMode;
import org.apache.uima.util.impl.DataIO;
import org.apache.uima.util.impl.OptimizeStrings;
import org.apache.uima.util.impl.SerializationMeasures;

// @formatter:off
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
// @formatter:on
public class BinaryCasSerDes6 implements SlotKindsConstants {

  private static final String EMPTY_STRING = "";

  private static final boolean TRACE_SER = false;
  private static final boolean TRACE_DES = false;

  private static final boolean TRACE_MOD_SER = false;
  private static final boolean TRACE_MOD_DES = false;

  private static final boolean TRACE_STR_ARRAY = false;

  /**
   * Compression alternatives
   */

  public enum CompressLevel {
    None(Deflater.NO_COMPRESSION), Fast(Deflater.BEST_SPEED), Default(
            Deflater.DEFAULT_COMPRESSION), Best(Deflater.BEST_COMPRESSION),;

    final public int lvl;

    CompressLevel(int lvl) {
      this.lvl = lvl;
    }
  }

  public enum CompressStrat {
    Default(Deflater.DEFAULT_STRATEGY), Filtered(Deflater.FILTERED), HuffmanOnly(
            Deflater.HUFFMAN_ONLY),;

    final public int strat;

    CompressStrat(int strat) {
      this.strat = strat;
    }
  }

  // @formatter:off
  /**
   * Info reused for 
   *   1) multiple serializations of same cas to multiple targets (a speedup), or
   *   2) for delta cas serialization, where it represents the fsStartIndex info before any mods
   *      were done which could change that info, or 
   *   3) for deserializing with a delta cas, where it represents the fsStartIndex info at the time
   *      the CAS was serialized out..
   * Reachable FSs and Sequence maps
   */
  // @formatter:on
  public static class ReuseInfo {
    // @formatter:off
    /**
     * kept to avoid recomputation in the use case:
     *   - serialize to target 1, serialize same to target 2, etc.
     *   - Delta serialization (uses reuse info saved during initial deserialization)
     *   - Delta deserialization 
     *   if Null, recomputed when needed
     * foundFSs used to test if fsRef needs to be serialized   
     */
    // @formatter:on
    final private PositiveIntSet foundFSs;
    final private List<TOP> fssToSerialize; // ordered list of FSs found in indexes or linked from
                                            // other found FSs

    // @formatter:off
    /**
     * Multiple uses:
     *   a) avoid recomputation when multiple serializations of same CAS to multiple targets
     *   b) remembers required mapping for processing delta cas serializations and deserializations conversion of tgt seq # to src addr
     */
    // @formatter:on
    final private CasSeqAddrMaps fsStartIndexes;

    private ReuseInfo(PositiveIntSet foundFSs, List<TOP> fssToSerialize,
            CasSeqAddrMaps fsStartIndexes) {
      this.foundFSs = foundFSs;
      this.fssToSerialize = fssToSerialize;
      this.fsStartIndexes = fsStartIndexes;
    }
  }

  public ReuseInfo getReuseInfo() {
    return new ReuseInfo(foundFSs, fssToSerialize, fsStartIndexes);
  }

  /**
   * Things set up for one instance of this class
   */
  private TypeSystemImpl srcTs;
  final private TypeSystemImpl tgtTs;
  final private CompressLevel compressLevel;
  final private CompressStrat compressStrategy;

  /*****************************************************
   * Things for both serialization and Deserialization
   *****************************************************/
  final private CASImpl cas; // cas being serialized or deserialized into
  final private BinaryCasSerDes bcsd; // common binary ser/des code
  // private int[] heap; // main heap, can't be final because grow replaces it
  final private StringHeap stringHeapObj; // needed for compression encoding/decoding
  // final private LongHeap longHeapObj;
  // final private ShortHeap shortHeapObj;
  // final private ByteHeap byteHeapObj;
  //
  private int nextFsId;
  // private int heapEnd; // set when deserializing
  // private int totalMappedHeapSize = 0; // heapEnd - heapStart, but with FS that don't exist in
  // the target type system deleted

  final private boolean isSerializingDelta; // if true, there is a marker indicating the start
                                            // spot(s)
  private boolean isDelta;
  private boolean isReadingDelta;
  final private MarkerImpl mark; // the mark to serialize from

  /**
   * maps from src id &lt;-&gt; tgt id For deserialization: if src type not exist, tgt to src is 0
   */
  final private CasSeqAddrMaps fsStartIndexes;
  final private boolean reuseInfoProvided;
  final private boolean doMeasurements; // if true, doing measurements

  private OptimizeStrings os;
  private boolean only1CommonString; // true if only one common string

  private boolean isTsIncluded; // type system used for the serialization
  private boolean isTsiIncluded; // types plus index definition, used to reset the cas

  // private TypeInfo typeInfo; // type info for the current type being serialized/deserialized
  // // always the "src" typeInfo I think, except for compareCas use
  final private CasTypeSystemMapper typeMapper;

  /**
   * This is the used version of isTypeMapping, normally == to isTypeMappingCmn But compareCASes
   * sets this false temporarily while setting up the compare
   */
  private boolean isTypeMapping;

  // /**
  // * hold previous instance of FS by typecode, for compression calculation
  // */
  // final private TOP[] prevFsByType;
  // /**
  // * previous FS serialized or deserialized, of the current type, may be null
  // */
  // private TOP prevFs;

  // @formatter:off
  /**
   * Hold prev instance of FS which have non-array FSRef slots, to allow 
   * computing these to match case where a 0 value is used because of type filtering
   * and also to allow for forward references.
   * 
   * Note: we can't use the actual prev FS, because for type filtering, it may not exist!
   * and even if it exists, it may not be fixed up (forward ref not yet deserialized)
   * 
   *   for each target typecode, only set if the type has 1 or more non-array fsref
   *   set only for non-filtered domain types
   *     set only for non-0 values
   *       if fsRef is to filtered type, value serialized will be 0, but this slot not set
   *       On deserialization: if value is 0, skip setting 
   * first index: key is type code
   * 2nd index: key is slot-offset number (0-based)
   * 
   * Also used for array refs sometimes, for the 1st entry in the array
   *   - feature slot 0 is used for this when reading (not when writing - could be made more uniform)
   */
  // @formatter:on
  final private int[][] prevHeapInstanceWithIntValues;

  // @formatter:off
  /**
   * Hold prev values of "long" slots, by type, for instances of FS which are non-arrays containing 
   *   slots which have long values, used for differencing 
   *   - not using the actual FS instance, because during deserialization, these may not be 
   *     deserialized due to type filtering
   *   set only for non-filtered domain types
   *     set only for non-0 values
   *       if fsRef is to filtered type, value serialized will be 0, but this slot not set
   *       On deserialization: if value is 0, skip setting 
   * first index: key is type code
   * 2nd index: key is slot-offset number (0-based) 
   */
  // @formatter:on
  final private Int2ObjHashMap<long[], long[]> prevFsWithLongValues;

  /**
   * ordered set of FSs found in indexes or linked from other found FSs. used to control
   * loops/recursion when locating things
   */
  private PositiveIntSet foundFSs;

  /**
   * ordered set of FSs found in indexes or linked from other found FSs, which are below the mark.
   * used to control loops/recursion when locating things
   */
  private PositiveIntSet foundFSsBelowMark;

  /**
   * FSs being serialized. For delta, just the deltas above the delta line. Constructed from indexed
   * plus reachable, above the delta line.
   */
  private List<TOP> fssToSerialize;

  /**
   * Set of FSes on which UimaSerializable _save_to_cas_data has already been called.
   */
  private PositiveIntSet uimaSerializableSavedToCas;

  /**
   * FSs being processed, including below-the-line deltas.
   */
  final private List<TOP> toBeScanned = new ArrayList<>();
  // private HashSetInt ffssBelowMark; // sorted fss's found below the mark
  // final private int[] typeCodeHisto = new int[ts.getTypeArraySize()];

  final private boolean debugEOF = false;
  /*********************************
   * Things for just serialization
   *********************************/
  private DataOutputStream serializedOut; // where to write out the serialized result

  final private SerializationMeasures sm; // null or serialization measurements
  final private ByteArrayOutputStream[] baosZipSources = new ByteArrayOutputStream[NBR_SLOT_KIND_ZIP_STREAMS]; // lazily
                                                                                                               // created,
                                                                                                               // indexed
                                                                                                               // by
                                                                                                               // SlotKind.i
  final private DataOutputStream[] dosZipSources = new DataOutputStream[NBR_SLOT_KIND_ZIP_STREAMS]; // lazily
                                                                                                    // created,
                                                                                                    // indexed
                                                                                                    // by
                                                                                                    // SlotKind.i

  // speedups

  // any use of these means caller handles measurement
  // some of these are never used, because the current impl
  // is using the _i form to get measurements done
  // private DataOutputStream arrayLength_dos;
  // private DataOutputStream heapRef_dos;
  // private DataOutputStream int_dos;
  private DataOutputStream byte_dos;
  // private DataOutputStream short_dos;
  private DataOutputStream typeCode_dos;
  private DataOutputStream strOffset_dos;
  private DataOutputStream strLength_dos;
  // private DataOutputStream long_High_dos;
  // private DataOutputStream long_Low_dos;
  private DataOutputStream float_Mantissa_Sign_dos;
  private DataOutputStream float_Exponent_dos;
  private DataOutputStream double_Mantissa_Sign_dos;
  private DataOutputStream double_Exponent_dos;
  private DataOutputStream fsIndexes_dos;
  // private DataOutputStream strChars_dos;
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

  /**
   * the "fixups" for relative heap refs actions set slot values
   */
  final private List<Runnable> fixupsNeeded = new ArrayList<>();
  final private List<Runnable> uimaSerializableFixups = new ArrayList<>();
  // /** hold on to FS prior to getting them indexed to prevent them from being GC'd */
  // final private List<TOP> preventFsGc = new ArrayList<>();

  /**
   * Deferred actions to set Feature Slots of feature structures. the deferrals needed when
   * deserializing a subtype of AnnotationBase before the sofa is known Also for Sofa creation where
   * some fields are final
   */
  final private List<Runnable> singleFsDefer = new ArrayList<>();

  /** used for deferred creation */
  private int sofaNum;
  private String sofaName;
  private String sofaMimeType;
  private Sofa sofaRef;

  /** the FS being deserialized */
  private TOP currentFs;

  private boolean isUpdatePrevOK; // false if shouldn't update prev value because written value was
                                  // 0

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

  // used when reading v2 style
  private int lastArrayLength;

  /**
   * Setup to serialize or deserialize using binary compression, with (optional) type mapping and
   * only processing reachable Feature Structures
   * 
   * @param aCas
   *          required - refs the CAS being serialized or deserialized into
   * @param mark
   *          if not null is the serialization mark for delta serialization. Unused for
   *          deserialization.
   * @param tgtTs
   *          if not null is the target type system. - For serialization - this is a subset of the
   *          CASs TS - for deserialization, is the type system of the serialized data being read.
   * @param rfs
   *          For delta serialization - must be not null, and the saved value after deserializing
   *          the original before any modifications / additions made. For normal serialization - can
   *          be null, but if not, is used in place of re-calculating, for speed up For delta
   *          deserialization - must not be null, and is the saved value after serializing to the
   *          service For normal deserialization - must be null
   * @param doMeasurements
   *          if true, measurements are done (on serialization)
   * @param compressLevel
   *          if not null, specifies enum instance for compress level
   * @param compressStrategy
   *          if not null, specifies enum instance for compress strategy
   * @throws ResourceInitializationException
   *           if the target type system is incompatible with the source type system
   */

  public BinaryCasSerDes6(AbstractCas aCas, MarkerImpl mark, TypeSystemImpl tgtTs, ReuseInfo rfs,
          boolean doMeasurements, CompressLevel compressLevel, CompressStrat compressStrategy)
          throws ResourceInitializationException {
    this(aCas, mark, tgtTs, false, false, rfs, doMeasurements, compressLevel, compressStrategy);
  }

  private BinaryCasSerDes6(AbstractCas aCas, MarkerImpl mark, TypeSystemImpl tgtTs, boolean storeTS,
          boolean storeTSI, ReuseInfo rfs, boolean doMeasurements, CompressLevel compressLevel,
          CompressStrat compressStrategy) throws ResourceInitializationException {
    cas = ((CASImpl) ((aCas instanceof JCas) ? ((JCas) aCas).getCas() : aCas)).getBaseCAS();
    bcsd = cas.getBinaryCasSerDes();

    srcTs = cas.getTypeSystemImpl();
    this.mark = mark;

    if (null != mark && !mark.isValid()) {
      throw new CASRuntimeException(CASRuntimeException.INVALID_MARKER, "Invalid Marker.");
    }

    this.doMeasurements = doMeasurements;
    sm = doMeasurements ? new SerializationMeasures() : null;

    isDelta = isSerializingDelta = (mark != null);
    typeMapper = srcTs.getTypeSystemMapper(tgtTs);
    isTypeMapping = (null != typeMapper);
    isTsIncluded = storeTS;
    isTsiIncluded = storeTSI;

    // heap = cas.getHeap().heap;
    // heapEnd = cas.getHeap().getCellsUsed();
    nextFsId = isSerializingDelta ? mark.getNextFSId() : 0;
    //
    stringHeapObj = new StringHeap();
    // longHeapObj = cas.getLongHeap();
    // shortHeapObj = cas.getShortHeap();
    // byteHeapObj = cas.getByteHeap();

    // prevFsByType = new TOP[srcTs.getTypeArraySize()];
    int sz = Math.max(srcTs.getTypeArraySize(), (tgtTs == null) ? 0 : tgtTs.getTypeArraySize());
    prevHeapInstanceWithIntValues = new int[sz][];
    prevFsWithLongValues = new Int2ObjHashMap<>(long[].class);

    this.compressLevel = compressLevel;
    this.compressStrategy = compressStrategy;
    reuseInfoProvided = (rfs != null);
    if (reuseInfoProvided) {
      foundFSs = rfs.foundFSs; // broken for serialization - not reused
      fssToSerialize = rfs.fssToSerialize; // broken for serialization - not reused
      // TODO figure out why there's a copy for next
      fsStartIndexes = rfs.fsStartIndexes.copy();
    } else {
      foundFSs = null;
      fssToSerialize = null;
      fsStartIndexes = new CasSeqAddrMaps();
    }
    this.tgtTs = tgtTs;
  }

  /**
   * only called to set up for deserialization. clones existing f6, but changes the tgtTs (used to
   * decode)
   * 
   * @param f6
   *          -
   * @param tgtTs
   *          used for decoding
   * @throws ResourceInitializationException
   *           -
   */
  BinaryCasSerDes6(BinaryCasSerDes6 f6, TypeSystemImpl tgtTs)
          throws ResourceInitializationException {
    cas = f6.cas;
    bcsd = f6.bcsd;
    stringHeapObj = f6.stringHeapObj;
    nextFsId = f6.nextFsId;

    srcTs = f6.srcTs;
    this.tgtTs = tgtTs; // passed in argument !
    compressLevel = f6.compressLevel;
    compressStrategy = f6.compressStrategy;

    mark = f6.mark;
    if (null != mark && !mark.isValid()) {
      throw new CASRuntimeException(CASRuntimeException.INVALID_MARKER, "Invalid Marker.");
    }

    isDelta = isSerializingDelta = (mark != null);
    fsStartIndexes = f6.fsStartIndexes;
    reuseInfoProvided = f6.reuseInfoProvided;
    doMeasurements = f6.doMeasurements;
    sm = f6.sm;

    isTsIncluded = f6.isTsIncluded;
    isTsiIncluded = f6.isTsiIncluded;

    typeMapper = srcTs.getTypeSystemMapper(tgtTs);
    isTypeMapping = (null != typeMapper);
    prevHeapInstanceWithIntValues = f6.prevHeapInstanceWithIntValues;
    prevFsWithLongValues = f6.prevFsWithLongValues;
    foundFSs = f6.foundFSs;
    foundFSsBelowMark = f6.foundFSsBelowMark;
    fssToSerialize = f6.fssToSerialize;

  }

  /**
   * Setup to serialize (not delta) or deserialize (not delta) using binary compression, no type
   * mapping but only processing reachable Feature Structures
   * 
   * @param cas
   *          -
   * @throws ResourceInitializationException
   *           never thrown
   */
  public BinaryCasSerDes6(AbstractCas cas) throws ResourceInitializationException {
    this(cas, null, null, false, false, null, false, CompressLevel.Default, CompressStrat.Default);
  }

  /**
   * Setup to serialize (not delta) or deserialize (not delta) using binary compression, with type
   * mapping and only processing reachable Feature Structures
   * 
   * @param cas
   *          -
   * @param tgtTs
   *          -
   * @throws ResourceInitializationException
   *           if the target type system is incompatible with the source type system
   */
  public BinaryCasSerDes6(AbstractCas cas, TypeSystemImpl tgtTs)
          throws ResourceInitializationException {
    this(cas, null, tgtTs, false, false, null, false, CompressLevel.Default, CompressStrat.Default);
  }

  /**
   * Setup to serialize (maybe delta) or deserialize (maybe delta) using binary compression, with
   * type mapping and only processing reachable Feature Structures
   * 
   * @param cas
   *          -
   * @param mark
   *          -
   * @param tgtTs
   *          - for deserialization, is the type system of the serialized data being read.
   * @param rfs
   *          Reused Feature Structure information - required for both delta serialization and delta
   *          deserialization
   * @throws ResourceInitializationException
   *           if the target type system is incompatible with the source type system
   */
  public BinaryCasSerDes6(AbstractCas cas, MarkerImpl mark, TypeSystemImpl tgtTs, ReuseInfo rfs)
          throws ResourceInitializationException {
    this(cas, mark, tgtTs, false, false, rfs, false, CompressLevel.Default, CompressStrat.Default);
  }

  /**
   * Setup to serialize (maybe delta) or deserialize (maybe delta) using binary compression, with
   * type mapping and only processing reachable Feature Structures, output measurements
   * 
   * @param cas
   *          -
   * @param mark
   *          -
   * @param tgtTs
   *          - - for deserialization, is the type system of the serialized data being read.
   * @param rfs
   *          Reused Feature Structure information - speed up on serialization, required on delta
   *          deserialization
   * @param doMeasurements
   *          -
   * @throws ResourceInitializationException
   *           if the target type system is incompatible with the source type system
   */
  public BinaryCasSerDes6(AbstractCas cas, MarkerImpl mark, TypeSystemImpl tgtTs, ReuseInfo rfs,
          boolean doMeasurements) throws ResourceInitializationException {
    this(cas, mark, tgtTs, false, false, rfs, doMeasurements, CompressLevel.Default,
            CompressStrat.Default);
  }

  /**
   * Setup to serialize (not delta) or deserialize (maybe delta) using binary compression, no type
   * mapping and only processing reachable Feature Structures
   * 
   * @param cas
   *          -
   * @param rfs
   *          -
   * @throws ResourceInitializationException
   *           never thrown
   */
  public BinaryCasSerDes6(AbstractCas cas, ReuseInfo rfs) throws ResourceInitializationException {
    this(cas, null, null, false, false, rfs, false, CompressLevel.Default, CompressStrat.Default);
  }

  /**
   * Setup to serialize (not delta) or deserialize (maybe delta) using binary compression, no type
   * mapping, optionally storing TSI, and only processing reachable Feature Structures
   * 
   * @param cas
   *          -
   * @param rfs
   *          -
   * @param storeTS
   *          -
   * @param storeTSI
   *          -
   * @throws ResourceInitializationException
   *           never thrown
   */
  public BinaryCasSerDes6(AbstractCas cas, ReuseInfo rfs, boolean storeTS, boolean storeTSI)
          throws ResourceInitializationException {
    this(cas, null, null, storeTS, storeTSI, rfs, false, CompressLevel.Default,
            CompressStrat.Default);
  }

  // ********************************************************************************************
  // S e r i a l i z e r Class for sharing variables among routines
  // Class instantiated once per serialization
  // Multiple serializations in parallel supported, with multiple instances of this
  // ********************************************************************************************/

  /*************************************************************************************
   * S E R I A L I Z E
   * 
   * @param out
   *          -
   * @return null or serialization measurements (depending on setting of doMeasurements)
   * @throws IOException
   *           passthru
   *************************************************************************************/
  public SerializationMeasures serialize(Object out) throws IOException {
    if (isSerializingDelta && (tgtTs != null)) {
      throw new UnsupportedOperationException(
              "Can't do Delta Serialization with different target TS");
    }

    synchronized (cas.svd) {

      setupOutputStreams(out);

      if (doMeasurements) {
        // System.out.println(printCasInfo(cas));
        // sm.origAuxBytes = cas.getByteHeap().getSize();
        // sm.origAuxShorts = cas.getShortHeap().getSize() * 2;
        // sm.origAuxLongs = cas.getLongHeap().getSize() * 8;
        sm.totalTime = System.currentTimeMillis();
      }

      CommonSerDes.createHeader().form6().delta(isSerializingDelta).seqVer(2) // 2 == version 3 (or
                                                                              // later)
              .v3().typeSystemIncluded(isTsIncluded).typeSystemIndexDefIncluded(isTsiIncluded)
              .write(serializedOut);

      if (isTsIncluded || isTsiIncluded) {
        CasIOUtils.writeTypeSystem(cas, serializedOut, isTsiIncluded);
      }

      os = new OptimizeStrings(doMeasurements);

      uimaSerializableSavedToCas = new PositiveIntSet_impl(1024, 1, 1024);

      // *****************************************************************
      // Find all FSs to be serialized via the indexes
      // including those FSs referenced
      // For Delta Serialization - excludes those FSs below the line
      // *****************************************************************

      // @formatter:off
      /** 
       * Skip this, and use the reuse-info, only for the case of:
       *   - reuse info is provided, and its not a delta serialization.
       *   - This should only happen if the same identical CAS is being 
       *     serialized multiple times (being sent to multiple remote services, for instance)
       */
      // @formatter:on
      if (!reuseInfoProvided || isSerializingDelta) {
        // long start = System.currentTimeMillis();
        processIndexedFeatureStructures(cas, false /* compute ref'd FSs, no write */);
        // System.out.format("Time to enqueue reachable FSs: %,.3f seconds%n",
        // (System.currentTimeMillis() - start)/ 1000f);
      }

      // @formatter:off
      /***************************
       * Prepare to walk main heap
       * We prescan the main heap and
       *   1) identify any types that should be skipped
       *      building a source and target fsStartIndexes table
       *   2) add all strings to the string table, 
       *      for strings above the mark
       ***************************/
      // @formatter:on

      // scan thru all fs (above the line if delta) and create a map from
      // the src id to the tgt id (some types may be missing, so is not identity map).
      // Add all strings to string optimizer.
      // Note: for delta cas, this only picks up strings
      // referenced by FSs above the line

      // Note: does the UimaSerializable _save_to_cas_data call for above the line items
      initSrcTgtIdMapsAndStrings();

      // add remaining strings for this case:
      // deltaCas, FS below the line modified, modification is new string.
      // use the deltaCasMod scanning
      final SerializeModifiedFSs smfs = isSerializingDelta ? new SerializeModifiedFSs() : null;
      if (isSerializingDelta) {
        // Note: does the UimaSerializable _save_to_cas_data call for modified below the line items
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
      writeVnumber(control_dos, fssToSerialize.size()); // was totalMappedHeapSize

      // Arrays.fill(prevFsByType, null);
      Arrays.fill(prevHeapInstanceWithIntValues, null);
      prevFsWithLongValues.clear();

      /***************************
       * walk main heap
       ***************************/

      for (TOP fs : fssToSerialize) {

        final TypeImpl srcType = fs._getTypeImpl();
        final int tCode = srcType.getCode();
        final TypeImpl tgtType = isTypeMapping ? typeMapper.mapTypeSrc2Tgt(srcType) : srcType;
        assert (null != tgtType); // because those are not put on queue for serialization

        // prevFs = prevFsByType[tCode];

        if (TRACE_SER) {
          System.out.format("Ser: %,d adr: %,8d tCode: %,3d %13s tgtTypeCode: %,3d %n", fs._id,
                  fs._id, srcType.getCode(), srcType.getShortName(), tgtType.getCode());
        }

        writeVnumber(typeCode_dos, tgtType.getCode());

        if (fs instanceof CommonArrayFS) {
          serializeArray(fs);
        } else {
          if (isTypeMapping) {
            // Serialize out in the order the features are in the target
            for (FeatureImpl tgtFeat : tgtType.getFeatureImpls()) {
              FeatureImpl srcFeat = typeMapper.getSrcFeature(tgtType, tgtFeat);
              assert (srcFeat != null); // for serialization, target is never a superset of features
                                        // of src
              serializeByKind(fs, srcFeat);
            }
          } else { // not type mapping
            for (FeatureImpl srcFeat : srcType.getFeatureImpls()) {
              serializeByKind(fs, srcFeat);
            }
          }
        }

        // prevFsByType[tCode] = fs;
        if (doMeasurements) {
          sm.statDetails[typeCode_i].incr(DataIO.lengthVnumber(tCode));
          sm.mainHeapFSs++;
        }
      } // end of FSs above the line walk

      // write out views, sofas, indexes
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
  }

  private void serializeArray(TOP fs) throws IOException {
    final TypeImpl_array arrayType = (TypeImpl_array) fs._getTypeImpl();
    CommonArrayFS<?> a = (CommonArrayFS<?>) fs;
    final SlotKind arrayElementKind = arrayType.getComponentSlotKind();

    final int length = serializeArrayLength(a);
    // output values
    // special case 0 and 1st value
    if (length == 0) {
      if (arrayElementKind == SlotKind.Slot_HeapRef || arrayElementKind == SlotKind.Slot_Int) {
        updatePrevArray0IntValue(arrayType, 0);
      }
      return;
    }

    final int io = arrayElementKind.ordinal();

    int prev = 0;
    long longPrev;
    boolean isFirstElement = true;

    switch (arrayElementKind) {

      case Slot_HeapRef:
        prev = getPrevIntValue(arrayType.getCode(), 0);
        // FSArray prevFsArray = (FSArray) prevFs;
        // if (prevFsArray != null && prevFsArray.size() != 0) {
        // prev = getTgtSeqFromSrcFS(prevFsArray.get(0));
        // } // else use the preset 0 value

        for (TOP element : ((FSArray<?>) fs)._getTheArray()) {
          final int v = getTgtSeqFromSrcFS(element);
          writeDiff(io, v, prev);
          if (isUpdatePrevOK && isFirstElement) {
            updatePrevArray0IntValue(arrayType, v);
          }
          prev = v;
          isFirstElement = false;
        }
        break;

      case Slot_Int:
        prev = getPrevIntValue(arrayType.getCode(), 0);
        // IntegerArray prevIntArray = (IntegerArray) prevFs;
        // if (prevIntArray != null && prevIntArray.size() != 0) {
        // prev = prevIntArray.get(0);
        // }

        for (int element : ((IntegerArray) fs)._getTheArray()) {
          writeDiff(io, element, prev);
          if (isUpdatePrevOK && isFirstElement) {
            updatePrevArray0IntValue(arrayType, element);
          }
          isFirstElement = false;
          prev = element;
        }
        break;

      case Slot_Float:
        for (float item : ((FloatArray) fs)._getTheArray()) {
          writeFloat(CASImpl.float2int(item));
        }
        break;

      case Slot_StrRef:
        for (String item : ((StringArray) fs)._getTheArray()) {
          writeString(item);
        }
        break;

      case Slot_BooleanRef:
        for (boolean b : ((BooleanArray) fs)._getTheArray()) {
          byte_dos.write(b ? 1 : 0);
        }
        break;

      case Slot_ByteRef:
        byte_dos.write(((ByteArray) fs)._getTheArray(), 0, length);
        break;

      case Slot_ShortRef:
        for (short v : ((ShortArray) fs)._getTheArray()) {
          writeDiff(short_i, v, prev);
          prev = v;
        }
        break;

      case Slot_LongRef:
        longPrev = 0L;
        for (long v : ((LongArray) fs)._getTheArray()) {
          writeLong(v, longPrev);
          longPrev = v;
        }
        break;

      case Slot_DoubleRef:
        for (double v : ((DoubleArray) fs)._getTheArray()) {
          writeDouble(Double.doubleToRawLongBits(v));
        }
        break;

      default:
        Misc.internalError();
    } // end of switch
  }

  // @formatter:off
  /**
   * serialize one feature structure, which is
   *   guaranteed not to be null
   *   guaranteed to exist in target if there is type mapping
   * Caller iterates over target slots, but the feat arg is for the corresponding src feature
   * @param fs the FS whose slot "feat" is to be serialize
   * @param feat the corresponding source feature slot to serialize
   */
  // @formatter:on
  private void serializeByKind(TOP fs, FeatureImpl feat) throws IOException {
    SlotKind kind = feat.getSlotKind();
    switch (kind) {
      // Slot_Int, Slot_Float, Slot_Boolean, Slot_Byte, Slot_Short
      case Slot_Short:
        serializeDiffWithPrevTypeSlot(kind, fs, feat, fs._getShortValueNc(feat));
        break;
      case Slot_Int:
        serializeDiffWithPrevTypeSlot(kind, fs, feat, fs._getIntValueNc(feat));
        break;
      case Slot_HeapRef:
        // if ()
        serializeDiffWithPrevTypeSlot(kind, fs, feat,
                getTgtSeqFromSrcFS(fs._getFeatureValueNc(feat)));
        break;
      case Slot_Float:
        writeFloat(CASImpl.float2int(fs._getFloatValueNc(feat)));
        break;
      case Slot_Boolean:
        byte_dos.write(fs._getBooleanValueNc(feat) ? 1 : 0);
        break;
      case Slot_Byte:
        byte_dos.write(fs._getByteValueNc(feat));
        break;
      case Slot_StrRef:
        writeString(fs._getStringValueNc(feat));
        break;
      case Slot_LongRef:
        final TypeImpl ti = fs._getTypeImpl();
        final int offset = feat.getOffset();
        final long prevLong = getPrevLongValue(ti.getCode(), offset);
        final long vLong = fs._getLongValueNc(feat);
        writeLong(vLong, prevLong);
        updatePrevLongValue(ti, offset, vLong);
        break;

      case Slot_DoubleRef:
        writeDouble(Double.doubleToRawLongBits(fs._getDoubleValueNc(feat)));
        break;
      default:
        throw new RuntimeException("internal error");
    } // end of switch
  }

  private int serializeArrayLength(CommonArrayFS<?> array) throws IOException {
    final int length = array.size();
    writeVnumber(arrayLength_i, length);
    return length;
  }

  private void serializeDiffWithPrevTypeSlot(SlotKind kind, TOP fs, FeatureImpl feat, int newValue)
          throws IOException {
    final int prev = getPrevIntValue(fs._getTypeCode(), feat.getOffset());
    writeDiff(kind.ordinal(), newValue, prev);
    if (isUpdatePrevOK) {
      updatePrevIntValue(fs._getTypeImpl(), feat.getOffset(), newValue);
    }
  }

  /**
   * Called for non-arrays
   * 
   * @param ti
   *          the type
   * @param featOffset
   *          offset to the slot
   * @param newValue
   *          for heap refs, is the converted-from-addr-to-seq-number value
   */
  private void updatePrevIntValue(TypeImpl ti, final int featOffset, final int newValue) {
    final int[] featCache = initPrevIntValue(ti);
    featCache[featOffset] = newValue;
  }

  private void updatePrevLongValue(TypeImpl ti, final int featOffset, final long newValue) {
    final long[] featCache = initPrevLongValue(ti);
    featCache[featOffset] = newValue;
  }

  /**
   * version called for arrays, captures the 0th value
   */
  private void updatePrevArray0IntValue(TypeImpl ti, int newValue) {
    final int[] featCache = initPrevIntValue(ti);
    featCache[0] = newValue;
  }

  /**
   * Get and lazily initialize if needed the feature cache values for a type For Serializing, the
   * type belongs to the srcTs For Deserializing, the type belongs to the tgtTs
   * 
   * @param ti
   *          the type
   * @return the int feature cache
   */
  private int[] initPrevIntValue(TypeImpl ti) {
    int tcode = ti.getCode();
    final int[] featCache = prevHeapInstanceWithIntValues[tcode];
    if (null == featCache) {
      return prevHeapInstanceWithIntValues[tcode] = new int[ti.isArray() ? 1
              : ti.getNumberOfFeatures()];
    }
    return featCache;
  }

  /**
   * Get and lazily initialize if needed the long values for a type For Serializing and
   * Deserializing, the type belongs to the tgtTs
   * 
   * @param ti
   *          the type
   * @return the int feature cache
   */
  private long[] initPrevLongValue(TypeImpl ti) {
    int tcode = ti.getCode();
    long[] featCache = prevFsWithLongValues.get(tcode);
    if (null == featCache) {
      featCache = new long[ti.getNumberOfFeatures()];
      prevFsWithLongValues.put(tcode, featCache);
    }
    return featCache;
  }

  /**
   * For heaprefs this gets the previously serialized int value
   * 
   * @param typeCode
   *          the type code
   * @param featOffset
   *          true offset, 1 = first feature...
   * @return the previous int value for use in difference calculations
   */
  private int getPrevIntValue(int typeCode, int featOffset) {
    final int[] featCache = prevHeapInstanceWithIntValues[typeCode];
    if (null == featCache) {
      return 0;
    }
    return featCache[featOffset]; // for arrays, the offset is 0 to allow diffng from previous 0th
                                  // element
  }

  private long getPrevLongValue(int typeCode, int featOffset) {
    final long[] featCache = prevFsWithLongValues.get(typeCode);
    return (featCache == null) ? 0L : featCache[featOffset];
  }

  // @formatter:off
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
   *           passthru
   */
  // @formatter:on
  private void collectAndZip() throws IOException {
    ByteArrayOutputStream baosZipped = new ByteArrayOutputStream(4096);
    Deflater deflater = new Deflater(compressLevel.lvl, true);
    deflater.setStrategy(compressStrategy.strat);
    int nbrEntries = 0;

    List<Integer> idxAndLen = new ArrayList<>();

    for (int i = 0; i < baosZipSources.length; i++) {
      ByteArrayOutputStream baos = baosZipSources[i];
      if (baos != null) {
        nbrEntries++;
        dosZipSources[i].close();
        long startTime = System.currentTimeMillis();
        int zipBufSize = Math.max(1024, baos.size() / 100);
        deflater.reset();
        try (var cds = new DeflaterOutputStream(baosZipped, deflater, zipBufSize)) {
          baos.writeTo(cds);
        }
        idxAndLen.add(i);
        if (doMeasurements) {
          idxAndLen.add((int) (sm.statDetails[i].afterZip = deflater.getBytesWritten()));
          idxAndLen.add((int) (sm.statDetails[i].beforeZip = deflater.getBytesRead()));
          sm.statDetails[i].zipTime = System.currentTimeMillis() - startTime;
        } else {
          idxAndLen.add((int) deflater.getBytesWritten());
          idxAndLen.add((int) deflater.getBytesRead());
        }
      }
    } // end of for loop

    // @formatter:off
    /** 
     * format of serialized data, as DataOutputStream:
     *   - number of written kinds of sources (may be less than baosZipSources.length if some are not used)
     *   - Triples, for each non-null baosZipSources:
     *     - the index of the baosZipSource
     *     - the number of bytes in the deflated stream for this source
     *     - the number of uncompressed bytes for this stream
     *   - the compressed bytes for all the non-null baosZipSources streams, in order   
     */
    // @formatter:on
    serializedOut.writeInt(nbrEntries); // write number of entries
    for (int i = 0; i < idxAndLen.size();) {
      serializedOut.write(idxAndLen.get(i++));
      serializedOut.writeInt(idxAndLen.get(i++));
      serializedOut.writeInt(idxAndLen.get(i++));
    }
    baosZipped.writeTo(serializedOut); // write Compressed info
  }

  private void writeLong(long v, long prev) throws IOException {
    writeDiff(long_High_i, (int) (v >>> 32), (int) (prev >>> 32));
    writeDiff(long_Low_i, (int) v, (int) prev);
  }

  // @formatter:off
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
  // @formatter:on
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
      throw new RuntimeException(
              "Cannot serialize string of Integer.MAX_VALUE length - too large.");
    }

    final int offset = os.getOffset(indexOrSeq);
    final int length = encodeIntSign(s.length() + 1); // all lengths sign encoded because of above
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
      System.out.format("writeString length %,d offset %,d%n", length, offset);
    }
  }

  // @formatter:off
  /*
   * Need to support NAN sets, 
   * 0x7fc.... for NAN
   * 0xff8.... for NAN, negative infinity
   * 0x7f8     for NAN, positive infinity
   */
  // @formatter:on
  private void writeFloat(int raw) throws IOException {
    if (raw == 0) {
      writeUnsignedByte(float_Exponent_dos, 0);
      if (doMeasurements) {
        sm.statDetails[float_Exponent_i].incr(1);
      }
      return;
    }

    final int exponent = ((raw >>> 23) & 0xff) + 1; // because we reserve 0, see above
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
    int exponent = (int) ((raw >>> 52) & 0x7ff);
    exponent = exponent - 1023; // rebase so 1.0 = 0
    if (exponent >= 0) {
      exponent++; // skip "0", used above for 0 value
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

  // @formatter:off
  /**
   * Encoding:
   *    bit 6 = sign:   1 = negative
   *    bit 7 = delta:  1 = delta
   * @param kind selects the stream to write to
   * @param v  runs from iHeap + 3 to end of array
   * @param prev for difference encoding
   * sets isUpdatePrevOK true if ok to update prev, false if writing 0 for any reason, or max neg nbr
   * @returns possibly converted input value (converted if was heap ref to seq heap ref)
   * @throws IOException
   *           passthru
   */
  // @formatter:on
  private int writeDiff(int kind, int v, int prev) throws IOException {
    if (v == 0) {
      write0(kind);
      isUpdatePrevOK = false;
      return 0;
    }

    if (v == Integer.MIN_VALUE) { // special handling, because abs fails
      writeVnumber(kind, 2); // written as -0
      if (doMeasurements) {
        sm.statDetails[kind].diffEncoded++;
        sm.statDetails[kind].valueLeDiff++;
      }
      isUpdatePrevOK = false;
      return 0;
    }

    final int absV = Math.abs(v);
    if (((v > 0) && (prev > 0)) || ((v < 0) && (prev < 0))) {
      final int diff = v - prev; // guaranteed to not overflow because signs are the same
      // @formatter:off
//      // handle strange behavior after JIT where the Math.abs(0x7fffffff) gives Integer.MIN_VALUE
//      // for arguments v = 0xffffffff, and prev = Integer.MIN_VALUE
//      final int diff = (prev == Integer.MIN_VALUE) ?
//          // v is guaranteed to be negative
//          (v & 0x7fffffff) :
//          v - prev;  
//      final int absDiff = Math.abs(diff);
      // this seems to work around
      // @formatter:on
      final int absDiff = (diff < 0) ? -diff : diff;
      // @formatter:off
//      // debug failure in Math.abs
//      if (absDiff < 0) {
//        System.err.format("********* caught absdiff v = %s, prev = %s diff = %s absDiff = %s%n", 
//            Integer.toHexString(v),
//            Integer.toHexString(prev),
//            Integer.toHexString(diff),
//            Integer.toHexString(absDiff));
//      }
//      if (absV < 0) {
//        System.err.format("********* caught absv v = %s, absV = %s%n", 
//            Integer.toHexString(v),
//            Integer.toHexString(absV));
//      }
      // @formatter:on
      assert (absDiff >= 0);
      assert (absV >= 0);

      final long v2write = (absV <= absDiff) ? ((long) absV << 2) + ((v < 0) ? 2L : 0L)
              : ((long) absDiff << 2) + ((diff < 0) ? 3L : 1L);

      writeVnumber(kind, v2write);
      if (doMeasurements) {
        sm.statDetails[kind].diffEncoded++;
        sm.statDetails[kind].valueLeDiff += (absV <= absDiff) ? 1 : 0;
      }
      isUpdatePrevOK = true;
      return v;
    }
    // if get here, then the abs v value is always <= the abs diff value.
    writeVnumber(kind, ((long) absV << 2) + ((v < 0) ? 2 : 0));
    if (doMeasurements) {
      sm.statDetails[kind].diffEncoded++;
      sm.statDetails[kind].valueLeDiff++;
    }
    isUpdatePrevOK = true;
    return v;
  }

  private void write0(int kind) throws IOException {
    writeVnumber(kind, 0); // a speedup, not a new encoding
    if (doMeasurements) {
      sm.statDetails[kind].diffEncoded++;
      sm.statDetails[kind].valueLeDiff++;
    }
  }

  // @formatter:off
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
  // @formatter:on
  private class SerializeModifiedFSs {

    // final int[] modifiedMainHeapAddrs = toArrayOrINT0(cas.getModifiedFSHeapAddrs());

    // duplicate elimination done when change is added
    // the collection is not sorted
    final FsChange[] modifiedFSs = cas.getModifiedFSList();

    // final int[] modifiedByteHeapAddrs = toArrayOrINT0(cas.getModifiedByteHeapAddrs());
    // final int[] modifiedShortHeapAddrs = toArrayOrINT0(cas.getModifiedShortHeapAddrs());
    // final int[] modifiedLongHeapAddrs = toArrayOrINT0(cas.getModifiedLongHeapAddrs());

    // {sortModifications();} // a non-static initialization block

    // final int modMainHeapAddrsLength = cas.cleanupFsChanges(modifiedMainHeapAddrs);
    // final int modFSsLength = modifiedFSs.size();
    // final int modByteHeapAddrsLength = eliminateDuplicatesInMods(modifiedByteHeapAddrs);
    // final int modShortHeapAddrsLength = eliminateDuplicatesInMods(modifiedShortHeapAddrs);
    // final int modLongHeapAddrsLength = eliminateDuplicatesInMods(modifiedLongHeapAddrs);

    // ima - index into modified arrays
    // ixx, iPrevxxx - index in heap being changed
    // value comes via the main heap or aux heaps

    // int imaModMainHeap = 0;
    // int imaModByteRef = 0;
    // int imaModShortRef = 0;
    // int imaModLongRef = 0;

    // previous value - for things diff encoded
    private int vPrevModInt = 0;
    private int vPrevModHeapRef = 0;
    private short vPrevModShort = 0;
    private long vPrevModLong = 0;

    // @formatter:off
    /**
     * For Delta Serialization:
     * Add any strings below the line 
     * Assume: no TS mapping (because it's delta serialization)
     * Skips a modified item if in FS that isn't reachable
     */
    // @formatter:on
    private void addModifiedStrings() {
      // System.out.println("Enter addModifiedStrings");
      for (FsChange changedFs : modifiedFSs) {
        final TOP fs = changedFs.fs;
        final TypeImpl srcType = fs._getTypeImpl();
        // probably don't need this test, because change logging is done when a mark is set,
        // only for items below the line
        if ((isTypeMapping && null == typeMapper.mapTypeSrc2Tgt(srcType)) || !foundFSsBelowMark.contains(fs._id)) {
          // System.out.format(" skipping heap addr %,d%n", currentFsId);
          continue;
        }
        if (changedFs.arrayUpdates != null) {
          if (fs instanceof StringArray) {
            String[] strings = ((StringArray) fs)._getTheArray();
            IntListIterator it = changedFs.arrayUpdates.iterator();
            while (it.hasNext()) {
              os.add(strings[it.nextNvc()]);
            }
          }
        } else {
          if (fs instanceof UimaSerializable && !uimaSerializableSavedToCas.contains(fs._id)) {
            ((UimaSerializable) fs)._save_to_cas_data();
            uimaSerializableSavedToCas.add(fs._id);
          }
          final BitSet featuresModified = changedFs.featuresModified;
          int next = featuresModified.nextSetBit(0);
          FeatureImpl[] feats = fs._getTypeImpl().getFeatureImpls();
          while (next >= 0) {
            FeatureImpl srcFeat = feats[next];
            // add only those strings in slots that are in target type system
            if (isTypeMapping && typeMapper.getTgtFeature(srcType, srcFeat) == null) {
              continue; // skip - feature not in target type
            }
            if (srcFeat.getRangeImpl().isStringOrStringSubtype()) {
              os.add(fs._getStringValueNc(feats[next]));
            }
            next = featuresModified.nextSetBit(next + 1);
          }
        }
      }
    }

    private void serializeModifiedFSs() throws IOException {
      int nbrModifiedFSWritten = 0;
      // iterate over all modified feature structures
      int prevHeapSeq = 0;
      final int splitPoint = mark.nextFSId;
      for (FsChange fsChange : modifiedFSs) {
        final TOP fs = fsChange.fs;
        final TypeImpl srcType = fs._getTypeImpl();
        if (isTypeMapping && typeMapper.mapTypeSrc2Tgt(srcType) == null) {
          continue; // skip - type is not in target
        }
        final int id = fs._id;
        // perhaps part of this if test is not needed:
        // the id is probably guaranteed to be below the split point
        // because logging doesn't happen unless a change is below the mark
        if ((id >= splitPoint && !foundFSs.contains(id))
                || (id < splitPoint && !foundFSsBelowMark.contains(id))) {
          // although it was modified, it isn't going to be serialized because
          // it isn't indexed or referenced
          continue;
        }

        int v = fsStartIndexes.getTgtSeqFromSrcAddr(id);
        assert (v != -1);
        // System.out.format("debug ser mod, fsid: %,d after map %,d%n", id, v);
        // no isUpdatePrevOK here, to match what was serialized
        prevHeapSeq = writeDiff(fsIndexes_i, v, prevHeapSeq);
        writeModsForOneFs(fsChange);

        nbrModifiedFSWritten++;
      } // end of for loop over all modified FSs

      if (TRACE_MOD_SER) {
        System.out.format("trace writing mods, length mod list: %,d nbr written: %,d%n",
                modifiedFSs.length, nbrModifiedFSWritten);
      }

      // write out number of modified Feature Structures
      writeVnumber(control_dos, nbrModifiedFSWritten);
    } // end of method

    // @formatter:off
    /**
     * Write the modifications for one feature structure, based on the data in the fsChange
     *   - this is either an array or non-array (meaning changed Features)
     *     - array changes are written out in index order.
     *     - feature changes are written out in offset order.
     *     - sorting and elimination of duplicates happens when extracting info from fsChange
     * Not called if skipping writing because obj not reachable 
     * 
     * NOTE: the serialized values for the index are 0-based,
     *       vs. V2, which are base on the original offset in 
     *       various "heaps".
     *       - Because of this, 
     *          -- v2 deserialization can't read v3 serializations 
     *          -- v3 deserialization can   read v2 serializatoins, though.
     */
    // @formatter:on
    private void writeModsForOneFs(FsChange fsChange) throws IOException {
      TOP fs = fsChange.fs;
      TypeImpl ti = fs._getTypeImpl();

      if (fsChange.arrayUpdates != null) {
        int prevIndex = 0;
        writeVnumber(fsIndexes_dos, fsChange.arrayUpdates.size());
        IntListIterator it = fsChange.arrayUpdates.iterator();
        final SlotKind slotKind = ti.getComponentSlotKind();

        if (TRACE_MOD_SER) {
          System.out.format("trace ser mod array fsId: %,d nbrMods: %,d type: %s%n", fs._id,
                  fsChange.arrayUpdates.size(), ti.getShortName());
        }

        while (it.hasNext()) {
          int index = it.nextNvc();
          writeVnumber(fsIndexes_dos, index - prevIndex);
          prevIndex = index;

          if (TRACE_MOD_SER) {
            System.out.format("  tr se mod fsId: %,d offset: %,d%n", fs._id, index);
          }

          switch (slotKind) {
            case Slot_BooleanRef:
              writeUnsignedByte(byte_dos, ((BooleanArray) fs).get(index) ? 1 : 0);
              break;
            case Slot_ByteRef:
              writeUnsignedByte(byte_dos, ((ByteArray) fs).get(index));
              break;
            case Slot_ShortRef:
              final short vs = ((ShortArray) fs).get(index);
              writeDiff(int_i, vs, vPrevModShort);
              vPrevModShort = vs;
              break;
            case Slot_LongRef: {
              final long v = ((LongArray) fs).get(index);
              writeLong(v, vPrevModLong);
              vPrevModLong = v;
              break;
            }
            case Slot_DoubleRef: {
              final long v = Double.doubleToRawLongBits(((DoubleArray) fs).get(index));
              writeDouble(v);
              break;
            }
            case Slot_Int:
              vPrevModInt = writeDiff(int_i, ((IntegerArray) fs).get(index), vPrevModInt);
              break;
            case Slot_Float:
              writeFloat(CASImpl.float2int(((FloatArray) fs).get(index)));
              break;
            case Slot_HeapRef: {
              final int v = getTgtSeqFromSrcFS((TOP) ((FSArray) fs).get(index));
              vPrevModHeapRef = writeDiff(heapRef_i, v, vPrevModHeapRef);
              break;
            }
            case Slot_StrRef:
              writeString(((StringArray) fs).get(index));
              break;

            default:
              Misc.internalError();
          } // end of switch for array types
        } // end of loop for elements in array
        return;
      } // end of if array type

      // normal Feature mods, not array

      writeVnumber(fsIndexes_dos, fsChange.featuresModified.cardinality());
      int iPrevOffsetInFs = 0;

      if (TRACE_MOD_SER) {
        System.out.format("trace ser mod feats fsId: %,d nbrMods: %,d type: %s%n", fs._id,
                fsChange.featuresModified.cardinality(), ti.getShortName());
      }

      BitSet bs = fsChange.featuresModified;
      int offset = bs.nextSetBit(0);

      while (offset >= 0) {

        writeVnumber(fsIndexes_dos, offset - iPrevOffsetInFs);
        iPrevOffsetInFs = offset;

        final FeatureImpl fi = ti.getFeatureImpls()[offset];

        if (TRACE_MOD_SER) {
          System.out.format("  tr se mod fsId: %,d offset: %,d type: %s%n", fs._id, offset,
                  fi.getShortName());
        }

        final SlotKind slotKind = fi.getSlotKind();
        switch (slotKind) {
          case Slot_Boolean:
            byte_dos.write(fs._getBooleanValueNc(fi) ? 1 : 0);
            break;
          case Slot_Byte:
            byte_dos.write(fs._getByteValueNc(fi));
            break;
          case Slot_Short:
            vPrevModShort = (short) writeDiff(int_i, fs._getShortValueNc(fi), vPrevModShort);
            break;
          case Slot_Int:
            vPrevModInt = writeDiff(int_i, fs._getIntValueNc(fi), vPrevModInt);
            break;
          case Slot_LongRef: {
            long v = fs._getLongValueNc(fi);
            writeLong(v, vPrevModLong);
            vPrevModLong = v;
            break;
          }
          case Slot_Float:
            writeFloat(CASImpl.float2int(fs._getFloatValueNc(fi)));
            break;
          case Slot_DoubleRef:
            writeDouble(Double.doubleToRawLongBits(fs._getDoubleValueNc(fi)));
            break;
          case Slot_HeapRef: {
            final int v = getTgtSeqFromSrcFS(fs._getFeatureValueNc(fi));
            vPrevModHeapRef = writeDiff(heapRef_i, v, vPrevModHeapRef);
          }
            break;
          case Slot_StrRef:
            writeString(fs._getStringValueNc(fi));
            break;
          default:
            Misc.internalError();
        } // end of Switch
        offset = bs.nextSetBit(offset + 1);
      } // end of iterator over all features
    }
  } // end of class definition for SerializeModifiedFSs

  /*************************************************************************************
   * D E S E R I A L I Z E
   *************************************************************************************/
  /**
   * 
   * @param istream
   *          -
   * @throws IOException
   *           -
   */
  public void deserialize(InputStream istream) throws IOException {
    Header h = readHeader(istream); // side effect, sets deserIn

    if (isReadingDelta) {
      if (!reuseInfoProvided) {
        throw new UnsupportedOperationException(
                "Deserializing Delta Cas, but original not serialized from");
      }
    } else {
      cas.resetNoQuestions();
    }

    bcsd.reinit(h, deserIn, null, CasLoadMode.DEFAULT, this, AllowPreexistingFS.allow, null);
    // deserializeAfterVersion(deserIn, isReadingDelta, AllowPreexistingFS.allow);
  }

  /**
   * Version used by uima-as to read delta cas from remote parallel steps
   * 
   * @param istream
   *          input stream
   * @param aAllowPreexistingFS
   *          what to do if item already exists below the mark
   * @throws IOException
   *           passthru
   */
  public void deserialize(InputStream istream, AllowPreexistingFS aAllowPreexistingFS)
          throws IOException {
    Header h = readHeader(istream);

    if (isReadingDelta) {
      if (!reuseInfoProvided) {
        throw new UnsupportedOperationException(
                "Deserializing Delta Cas, but original not serialized from");
      }
    } else {
      throw new UnsupportedOperationException("Delta CAS required for this call");
    }

    bcsd.reinit(h, deserIn, null, CasLoadMode.DEFAULT, this, aAllowPreexistingFS, null);
  }

  public void deserializeAfterVersion(DataInputStream istream, boolean aIsDelta,
          AllowPreexistingFS aAllowPreexistingFS) throws IOException {

    allowPreexistingFS = aAllowPreexistingFS;
    if (aAllowPreexistingFS == AllowPreexistingFS.ignore) {
      throw new UnsupportedOperationException("AllowPreexistingFS.ignore not an allowed setting");
    }

    deserIn = istream;
    isDelta = isReadingDelta = aIsDelta;
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
    int nbrNewFSsInTarget = readVnumber(control_dis);
    // is nbr of FSs serialized (excluding mods) in v3
    // is totalMappedHeapSize in v2
    int totalMappedHeapSize = bcsd.isBeforeV3 ? nbrNewFSsInTarget : -1;
    if (bcsd.isBeforeV3) {
      nbrNewFSsInTarget = -1; // for safety
    }

    // stringTableOffset = isReadingDelta ? (stringHeapObj.getSize() - 1) : 0;
    nextFsId = isReadingDelta ? cas.peekNextFsId() : 0;

    // if (!isReadingDelta) {
    // heapObj.reinitSizeOnly(1);
    // heap = heapObj.heap;
    // }

    Arrays.fill(prevHeapInstanceWithIntValues, null);
    prevFsWithLongValues.clear();

    if (nextFsId == 0) {
      nextFsId = 1; // slot 0 not serialized, it's null / 0
    }

    // @formatter:off
    // For Delta CAS,
    //   Reuse previously computed map of addr <--> seq for existing FSs below mark line
    //                             map of seq(this CAS) <--> seq(incoming) 
    //                               that accounts for type code mismatch using typeMapper
    // note: rest of maps computed incrementally as we deserialize
    //   Two possibilities:  The CAS has a type, but the incoming is missing that type (services)
    //                       The incoming has a type, but the CAS is missing it - (deser from file)
    //     Below the merge line: only the 1st is possible
    //     Above the merge line: only the 2nd is possible
    // @formatter:on
    if (isReadingDelta) {
      if (!reuseInfoProvided) {
        throw new IllegalStateException("Reading Delta into CAS not serialized from");
      }
    }

    fixupsNeeded.clear();
    // preventFsGc.clear();
    // these two values are used when incrementing to the next before v3 heap addr
    TypeImpl tgtType;
    lastArrayLength = 0;
    /**********************************************************
     * Read in new FSs being deserialized and add them to heap
     **********************************************************/

    // @formatter:off
    // currentFsId is ID of next to be deserialized FS
    //   only incremented when something is "stored", not skipped
    // nextFsAddr only used for loop  termination , pre v3
    //   could be gt than fsId, because some FSs are "skipped" in deserialization
    // @formatter:n
    
    // currentFsId only used in error message
    for (int currentFsId = nextFsId, nbrFSs = 0, nextFsAddr = 1; bcsd.isBeforeV3
            ? nextFsAddr < totalMappedHeapSize
            : nbrFSs < nbrNewFSsInTarget; nbrFSs++, nextFsAddr += bcsd.isBeforeV3
                    ? tgtType.getFsSpaceReq(lastArrayLength)
                    : 0) {

      final int tgtTypeCode = readVnumber(typeCode_dis); // get type code
      // final int adjTgtTypeCode = tgtTypeCode + ((this.bcsd.isBeforeV3 && tgtTypeCode >
      // TypeSystemConstants.lastBuiltinV2TypeCode)
      // ? TypeSystemConstants.numberOfNewBuiltInsSinceV2
      // : 0);
      tgtType = (isTypeMapping ? tgtTs : srcTs).getTypeForCode(tgtTypeCode);
      if (tgtType == null) {
        /**
         * Deserializing Compressed Form 6, a type code: {0} has no corresponding type. currentFsId:
         * {1} nbrFSs: {2} nextFsAddr: {3}
         */
        throw new CASRuntimeException(CASRuntimeException.DESER_FORM_6_BAD_TYPE_CODE, tgtTypeCode,
                currentFsId, nbrFSs, nextFsAddr);
      }
      final TypeImpl srcType = isTypeMapping ? typeMapper.mapTypeCodeTgt2Src(tgtTypeCode) : tgtType;

      final boolean storeIt = (srcType != null);
      // A receiving client from a service always
      // has a superset of the service's types due to type merging so this
      // won't happen for that use case. But
      // a deserialize-from-file could hit this if the receiving type system
      // deleted a type.

      // The strategy for deserializing heap refs depends on finding
      // the prev value for that type. This must be done in the context
      // of the sending CAS's type system

      // SlotKind slotKind = srcType.slotKind;

      if (storeIt) {
        // we can skip the cache for prev values if the value will not be stored.
        // typeImpl = tgtType;
        initPrevIntValue(tgtType);
      }

      // typeInfo = storeIt ? srcTypeInfo : tgtTypeInfo; // if !storeIt, then srcTypeInfo is null.

      // fsStartIndexes.addSrcAddrForTgt(currentFsId, storeIt);

      if (TRACE_DES) {
        System.out.format("Des: fsnbr %,4d fsid %,4d adjTgtTypeCode: %,3d %13s srcTypeCode: %s%n",
                nbrFSs, cas.getLastUsedFsId() + 1, tgtTypeCode, tgtType.getShortName(),
                (null == srcType) ? "<null>" : Integer.toString(srcType.getCode()));
      }

      if (tgtType.isArray()) {
        readArray(storeIt, srcType, tgtType);

      } else {
          // @formatter:off
        /**
         * is not array, handle features
         * If storing the value, create the FS unless it's a Sofa or a subtype of AnnotationBase
         *   Those are deferred until the slots are known, because they're needed
         *   as part of the creation of the FS due to final values. 
         */
          // @formatter:on
        if (storeIt) {
          if (!srcTs.annotBaseType.subsumes(srcType) && // defer subtypes of AnnotationBase
                  !(srcTs.sofaType == srcType)) { // defer sofa types
            createCurrentFs(srcType, cas);
          } else {
            currentFs = null;
            singleFsDefer.clear();
            sofaRef = null;
            sofaNum = -1;
            sofaName = null;
            sofaMimeType = null;
          }
        }

        // is normal type with slots, not an array
        if (isTypeMapping && storeIt) {
          for (FeatureImpl tgtFeat : tgtType.getFeatureImpls()) {
            final FeatureImpl srcFeat = typeMapper.getSrcFeature(tgtType, tgtFeat);
            readByKind(currentFs, tgtFeat, srcFeat, storeIt, tgtType);
          }
        } else {
          for (FeatureImpl tgtFeat : tgtType.getFeatureImpls()) {
            readByKind(currentFs, tgtFeat, tgtFeat, storeIt, tgtType);
          }
        }

        if (currentFs == null) {

          // @formatter:off
          /**
           * Create single deferred FS
           *   Either: Sofa (has final fields) or
           *           Subtype of AnnotationBase - needs to be in the right view
           *   
           *   For the latter, handle document annotation specially
           */
            // @formatter:on

          if (srcTs.sofaType == srcType) {
            if (cas.hasView(sofaName)) {
              // sofa was already created, by an annotationBase subtype deserialized prior to this
              // one
              currentFs = (TOP) cas.getView(sofaName).getSofa();
            } else {
              currentFs = cas.createSofa(sofaNum, sofaName, sofaMimeType);
            }
          } else {

            CASImpl view = (null == sofaRef) ? cas.getInitialView() // https://issues.apache.org/jira/browse/UIMA-5588
                    : (CASImpl) cas.getView(sofaRef);

            // if (srcType.getCode() == TypeSystemConstants.docTypeCode) {
            // currentFs = view.getDocumentAnnotation(); // creates the document annotation if it
            // doesn't exist
            //
            // // we could remove this from the indexes until deserialization is over, but then,
            // other calls to getDocumentAnnotation
            // // would end up creating additional instances
            // } else {
            createCurrentFs(srcType, view);
            // }
          }
          if (srcType.getCode() == TypeSystemConstants.docTypeCode) {
            boolean wasRemoved = cas.removeFromCorruptableIndexAnyView(currentFs,
                    cas.getAddbackSingle());
            for (Runnable r : singleFsDefer) {
              r.run();
            }
            cas.addbackSingleIfWasRemoved(wasRemoved, currentFs);
          } else {
            for (Runnable r : singleFsDefer) {
              r.run();
            }
          }
        } // end of handling deferred current fs
      } // of not-an-array
      // if (storeIt) {
      // prevFsByType[srcType.getCode()] = currentFs; // make this one the "prev" one for subsequent
      // testing
      // //debug
      // assert(currentFs._id == currentFsId);
      // }
      // todo need to incr src heap by amt filtered (in case some slots missing,
      // need to incr tgt (for checking end) by unfiltered amount
      // need to fixup final heap to account for skipped slots
      // need to have read skip slots not present in src
      // targetHeapUsed += incrToNextFs(heap, currentFsId, tgtTypeInfo); // typeInfo is target type
      // info
      fsStartIndexes.addSrcFsForTgt(currentFs, storeIt);
      currentFsId += storeIt ? cas.lastV2IdIncr() : 0;
    } // end of for loop over items in main heap

    for (Runnable r : fixupsNeeded) {
      r.run();
    }

    for (Runnable r : uimaSerializableFixups) {
      r.run();
    }

    // process the index information
    readIndexedFeatureStructures();
    // for delta, process below-the-line updates
    if (isReadingDelta) {
      (new ReadModifiedFSs()).readModifiedFSs();
    }

    // preventFsGc.clear();

    closeDataInputs();
    // System.out.format("Deserialize took %,d ms%n", System.currentTimeMillis() - startTime1);
  }

  private void createCurrentFs(TypeImpl type, CASImpl view) {
    currentFs = view.createFS(type);
    if (currentFs instanceof UimaSerializable) {
      UimaSerializable ufs = (UimaSerializable) currentFs;
      uimaSerializableFixups.add(() -> ufs._init_from_cas_data());
    }
  }

  /**
   * @param srcType
   *          may be null if there's no source type for target when deserializing
   * @param tgtType
   *          the type being deserialized
   */
  private void readArray(boolean storeIt, TypeImpl srcType, TypeImpl tgtType) throws IOException {
    final int length = readArrayLength();
    lastArrayLength = length;
    final SlotKind slotKind = tgtType.getComponentSlotKind();

    final TOP fs = storeIt ? cas.createArray(srcType, length) : null;
    currentFs = fs;

    switch (slotKind) {
      case Slot_BooleanRef:
        if (storeIt) {
          for (int i = 0; i < length; i++) {
            ((BooleanArray) fs).set(i, byte_dis.readByte() == 1);
          }
        } else {
          skipBytes(byte_dis, length);
        }
        break;

      case Slot_ByteRef:
        readIntoByteArray(((ByteArray) fs)._getTheArray(), length, storeIt);
        break;

      case Slot_ShortRef: {
        readIntoShortArray(((ShortArray) fs)._getTheArray(), length, storeIt);
        break;
      }

      case Slot_Int: {
        IntegerArray ia = (IntegerArray) fs;
        int prev = getPrevIntValue(TypeSystemConstants.intArrayTypeCode, 0);
        for (int i = 0; i < length; i++) {
          int v = readDiff(Slot_Int, prev);
          prev = v;
          if (0 == i && isUpdatePrevOK && storeIt) {
            updatePrevArray0IntValue(ia._getTypeImpl(), v);
          }
          if (storeIt) {
            ia.set(i, v);
          }
        }
        break;
      }

      case Slot_LongRef:
        readIntoLongArray(((LongArray) fs)._getTheArray(), Slot_LongRef, length, storeIt);
        break;

      case Slot_Float: {
        final FloatArray fa = (FloatArray) fs;
        for (int i = 0; i < length; i++) {
          final int floatRef = readFloat();
          if (storeIt) {
            fa.set(i, Float.intBitsToFloat(floatRef));
          }
        }
        break;
      }

      case Slot_DoubleRef:
        // if (length == 0) {
        // System.out.println("debug deser Double Array len 0, fsId = " + fs._id);
        // }
        readIntoDoubleArray(((DoubleArray) fs)._getTheArray(), Slot_DoubleRef, length, storeIt);
        break;

      case Slot_HeapRef: {
        FSArray fsa = (FSArray) fs;
        TypeImpl_array arrayType = (TypeImpl_array) fsa._getTypeImpl();
        int prev = getPrevIntValue(arrayType.getCode(), 0);
        for (int i = 0; i < length; i++) {
          final int v = readDiff(Slot_HeapRef, prev);
          prev = v;
          if (0 == i && isUpdatePrevOK && storeIt) {
            updatePrevArray0IntValue(fsa._getTypeImpl(), v);
          }
          if (storeIt) {
            final int locali = i;
            maybeStoreOrDefer_slotFixups(v, refd_fs -> fsa.set(locali, refd_fs));
          }
        }
        break;
      }
      case Slot_StrRef: {
        StringArray sa = (StringArray) fs;
        for (int i = 0; i < length; i++) {
          String s = readString(storeIt);

          if (storeIt) {
            sa.set(i, s);
          }
        }
      }
        break;

      default:
        Misc.internalError();
    } // end of switch
  }

  private TOP getRefVal(int tgtSeq) {
    return (tgtSeq == 0) ? null : fsStartIndexes.getSrcFsFromTgtSeq(tgtSeq);
  }

  private int readArrayLength() throws IOException {
    return readVnumber(arrayLength_dis);
  }

  // @formatter:off
  /**
   * @param fs The feature structure to set feature value in, but may be null if it was deferred,
   *          - happens for Sofas and subtypes of AnnotationBase
   *            because those have "final" values
   *        For Sofa: these are the sofaid (String) and sofanum (int)
   *        For AnnotationBase : this is the sofaRef (and the view).  
   *                    
   * @param tgtFeat the Feature being read
   * @param srcFeat the Feature being set (may be null if the feature doesn't exist)
   * @param storeIt false causes storing of values to be skipped
   * @throws IOException passthru
   */
  // @formatter:on
  private void readByKind(TOP fs, FeatureImpl tgtFeat, FeatureImpl srcFeat, boolean storeIt,
          TypeImpl tgtType) throws IOException {
    final int tgtFeatOffset = tgtFeat.getOffset();
    if (srcFeat == null) {
      storeIt = false; // because feature doesn't exist in the source type system
    }

    final SlotKind kind = tgtFeat.getSlotKind();

    switch (kind) {

      case Slot_Int:
        int vi = readDiffIntSlot(storeIt, tgtFeatOffset, kind, tgtType);
        if (srcFeat == srcTs.sofaNum) {
          sofaNum = vi;
        } else {
          maybeStoreOrDefer(storeIt, fs, (lfs) -> lfs._setIntLikeValueNcNj(kind, srcFeat, vi));
        }
        break;

      case Slot_Short:
        int vs = readDiffIntSlot(storeIt, tgtFeatOffset, kind, tgtType);
        maybeStoreOrDefer(storeIt, fs, (lfs) -> lfs._setIntLikeValueNcNj(kind, srcFeat, vs));
        break;

      case Slot_HeapRef:
        final int vh = readDiffIntSlot(storeIt, tgtFeatOffset, kind, tgtType);
        if (srcTs.annotBaseSofaFeat == srcFeat) {
          sofaRef = (Sofa) getRefVal(vh); // if sofa hasn't yet been deserialized, will be null
        }

        if (srcTs.annotBaseSofaFeat != srcFeat || sofaRef == null) {
          // https://issues.apache.org/jira/browse/UIMA-5588
          maybeStoreOrDefer(storeIt, fs, (lfs) -> {

            // outer defer done if fs is null; it is a one-feature-structure defer for sofa or
            // subtypes of annotationbase

            // When the setting is done for this one feature structure (now or at the end of
            // deserializing features for it)
            // two cases: the ref'd value is known, or not.
            // - if not known, a fixup is added to
            if (tgtType.getCode() == TypeSystemConstants.sofaTypeCode) {
              if (tgtFeat.getCode() == TypeSystemConstants.sofaArrayFeatCode) { // sofaArrayFeatCode
                                                                                // is the ref to
                                                                                // array for sofa
                                                                                // data
                Sofa sofa = (Sofa) lfs;
                maybeStoreOrDefer_slotFixups(vh, ref_fs -> sofa.setLocalSofaData(ref_fs));
              }
            } else {
              maybeStoreOrDefer_slotFixups(vh, ref_fs -> lfs._setFeatureValueNcNj(srcFeat, ref_fs));
            }
          });
        }
        break;

      case Slot_Float:
        final int floatAsInt = readFloat();
        maybeStoreOrDefer(storeIt, fs,
                (lfs) -> lfs._setFloatValueNcNj(srcFeat, Float.intBitsToFloat(floatAsInt)));
        break;

      case Slot_Boolean:
      case Slot_Byte:
        final byte vByte = byte_dis.readByte();
        maybeStoreOrDefer(storeIt, fs, (lfs) -> lfs._setIntLikeValueNcNj(kind, srcFeat, vByte));
        break;

      case Slot_StrRef:
        final String vString = readString(storeIt);
        if (null == vString) {
          break; // null is the default value, no need to set it
        }
        if (storeIt) {
          if (tgtType.getCode() == TypeSystemConstants.sofaTypeCode) {
            if (srcFeat == srcTs.sofaId) {
              sofaName = vString;
              break;
            }
            if (srcFeat == srcTs.sofaMime) {
              maybeStoreOrDefer(storeIt, fs, lfs -> ((Sofa) lfs).setMimeType(vString));
              break;
            }
            if (srcFeat == srcTs.sofaUri) {
              maybeStoreOrDefer(storeIt, fs, lfs -> ((Sofa) lfs).setRemoteSofaURI(vString));
              break;
            }
            if (srcFeat == srcTs.sofaString) {
              maybeStoreOrDefer(storeIt, fs,
                      lfs -> ((Sofa) lfs).setLocalSofaDataNoDocAnnotUpdate(vString));
              break;
            }
          }
          // other user-defined custom sofa extended string features (if any)
          // as well as non-sofa FS features, are set by the following code
          maybeStoreOrDefer(storeIt, fs, (lfs) -> lfs._setStringValueNcNj(srcFeat, vString));
        }
        break;

      case Slot_LongRef:
        long prevLong = getPrevLongValue(tgtType.getCode(), tgtFeatOffset);
        long vl = readLongOrDouble(kind, prevLong);
        updatePrevLongValue(tgtType, tgtFeatOffset, vl);
        maybeStoreOrDefer(storeIt, fs, (lfs) -> lfs._setLongValueNcNj(srcFeat, vl));
        break;

      case Slot_DoubleRef:
        long vd = readDouble();
        maybeStoreOrDefer(storeIt, fs,
                (lfs) -> lfs._setDoubleValueNcNj(srcFeat, CASImpl.long2double(vd)));
        break;

      default:
        Misc.internalError();
    } // end of switch
  }

  private void maybeStoreOrDefer(boolean storeIt, TOP fs, Consumer<TOP> doStore) {
    if (storeIt) {
      if (null == fs) {
        singleFsDefer.add(() -> doStore.accept(currentFs));
      } else {
        doStore.accept(fs);
      }
    }
  }

  /**
   * FS Ref slots fixups
   */
  /**
   * FS Ref slots fixups
   * 
   * @param tgtSeq
   *          the int value of the target seq number
   * @param r
   *          is sofa-or-lfs.setFeatureValue-or-setLocalSofaData(TOP ref-d-fs)
   */
  private void maybeStoreOrDefer_slotFixups(final int tgtSeq, Consumer<TOP> r) {
    if (tgtSeq == 0) {
      r.accept(null);
      return;
    }
    TOP src = getRefVal(tgtSeq);
    if (src == null) {
      // need to do the getRefVal later when it's known
      // here are the two values of "r"
      // () -> sofa.setLocalSofaData(getRefVal(vh))
      // () -> lfs.setFeatureValue(srcFeat, getRefVal(vh))
      fixupsNeeded.add(() -> r.accept(getRefVal(tgtSeq)));
    } else {
      // sofa.setLocalSofaData(tgt);
      // lfs.setFeatureValue(srcFeat, src)
      r.accept(src);
    }
  }

  /**
   * process index information to re-index things
   */
  private void readIndexedFeatureStructures() throws IOException {
    final int nbrViews = readVnumber(control_dis);
    final int nbrSofas = readVnumber(control_dis);

    IntVector fsIndexes = new IntVector(nbrViews + nbrSofas + 100);
    fsIndexes.add(nbrViews);
    fsIndexes.add(nbrSofas);
    for (int i = 0; i < nbrSofas; i++) {
      final int tgtAddrOfSofa = readVnumber(control_dis);
      fsIndexes.add(tgtAddrOfSofa);
    }

    for (int i = 0; i < nbrViews; i++) {
      readFsxPart(fsIndexes); // added FSs
      if (isDelta) {
        readFsxPart(fsIndexes); // removed FSs
        readFsxPart(fsIndexes); // reindexed FSs
      }
    }

    IntFunction<TOP> getFsFromTgtAddr = i -> fsStartIndexes.getSrcFsFromTgtSeq(i);

    bcsd.reinitIndexedFSs(fsIndexes.getArray(), isDelta, getFsFromTgtAddr);
  }

  /**
   * Each FS index is sorted, and output is by delta
   */
  private void readFsxPart(IntVector fsIndexes) throws IOException {
    final int nbrEntries = readVnumber(control_dis);
    int nbrEntriesAdded = 0;
    final int indexOfNbrAdded = fsIndexes.size();
    fsIndexes.add(0); // a place holder, will be updated at end
    int prev = 0;

    for (int i = 0; i < nbrEntries; i++) {
      int v = readVnumber(fsIndexes_dis) + prev;
      prev = v;
      // if type filtering (source type doesn't exist)
      // skip this one
      if (fsStartIndexes.getSrcFsFromTgtSeq(v) != null) {
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

  private void readIntoByteArray(byte[] array, int length, boolean storeIt) throws IOException {
    if (storeIt) {
      byte_dis.readFully(array, 0, length);
    } else {
      skipBytes(byte_dis, length);
    }
  }

  private void readIntoShortArray(final short[] array, final int length, final boolean storeIt)
          throws IOException {
    if (storeIt) {
      short prev = 0;
      for (int i = 0; i < length; i++) {
        array[i] = prev = (short) (readDiff(short_dis, prev));
      }
    } else {
      skipBytes(short_dis, length * 2);
    }
  }

  private void readIntoLongArray(long[] array, SlotKind kind, int length, boolean storeIt)
          throws IOException {
    if (storeIt) {
      long prev = 0L;
      for (int i = 0; i < length; i++) {
        array[i] = prev = readLongOrDouble(kind, prev);
      }
    } else {
      if (kind == Slot_LongRef) {
        skipLong(length);
      } else {
        skipDouble(length);
      }
    }
  }

  private void readIntoDoubleArray(double[] array, SlotKind kind, int length, boolean storeIt)
          throws IOException {
    if (storeIt) {
      long prev = 0L;
      for (int i = 0; i < length; i++) {
        prev = readLongOrDouble(kind, prev);
        array[i] = CASImpl.long2double(prev);
      }
    } else {
      if (kind == Slot_LongRef) {
        skipLong(length);
      } else {
        skipDouble(length);
      }
    }
  }

  private int readDiff(SlotKind kind, int prev) throws IOException {
    return readDiff(getInputStream(kind), prev);
  }

  private int readDiffIntSlot(boolean storeIt, int featOffset, SlotKind kind, TypeImpl tgtType)
          throws IOException {
    int prev = getPrevIntValue(tgtType.getCode(), featOffset);
    int v = readDiff(kind, prev);
    if (isUpdatePrevOK) {
      updatePrevIntValue(tgtType, featOffset, v);
    }
    return v;
  }

  // returns 2 values: the 2nd value is a boolean indicating if the
  // value was encoded as a 0 or the max negative
  // in which case updating of the "prev" is skipped
  // 2nd value returned in global (sigh)
  private int readDiff(DataInput in, int prev) throws IOException {
    final long encoded = readVlong(in);
    isUpdatePrevOK = encoded != 0;
    if (!isUpdatePrevOK) {
      return 0;
    }
    final boolean isDeltaEncoded = (0 != (encoded & 1L));
    final boolean isNegative = (0 != (encoded & 2L));
    int v = (int) (encoded >>> 2);
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
    final long v = (((long) vh) << 32) | (0xffffffffL & (long) vl);
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

    return ((exponent - 1) << 23) | mants | ((isNegative) ? 0x80000000 : 0);
  }

  private int decodeIntSign(int v) {
    if (1 == (v & 1)) {
      return -(v >>> 1);
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
      exponent--;
    }
    exponent = exponent + 1023;
    long r = ((long) ((exponent) & 0x7ff)) << 52;
    final boolean isNegative = (1 == (mants & 1));
    mants = Long.reverse(mants >>> 1) >>> 12;
    r = r | mants | (isNegative ? 0x8000000000000000L : 0);
    return r;
  }

  private long readVlong(DataInput dis) throws IOException {
    return DataIO.readVlong(dis);
  }

  /**
   * @param storeIt
   *          true to store value, false to skip it
   * @return the string
   */
  private String readString(boolean storeIt) throws IOException {
    final int length = decodeIntSign(readVnumber(strLength_dis));

    if (0 == length) {
      return null;
    }

    if (1 == length) {
      // always store, in case later offset ref
      // if (storeIt) {
      stringHeapObj.addString(EMPTY_STRING);
      return (EMPTY_STRING);
      // } else {
      // return 0;
      // }
    }

    if (length < 0) { // in this case, -length is the slot index
      if (storeIt) {
        if (TRACE_STR_ARRAY) {
          System.out.format("Trace String Array Des ref to offset %,d%n", length);
        }
        return stringHeapObj.getStringForCode(-length);
      } else {
        return null;
      }
    }

    final int offset = readVnumber(strOffset_dis);
    final int segmentIndex = (only1CommonString) ? 0 : readVnumber(strSeg_dis);
    // need to store all strings, because an otherwise skipped one may be referenced
    // later as an offset into the string table
    // if (storeIt) {
    String s = readCommonString[segmentIndex].substring(offset, offset + length - 1);
    stringHeapObj.addString(s);
    return s;
    // } else {
    // return 0;
    // }
  }

  static void skipBytes(DataInputStream stream, int skipNumber) throws IOException {
    final int r = stream.skipBytes(skipNumber);
    if (r != skipNumber) {
      throw new IOException(String.format(
              "%d bytes skipped when %d was requested, causing out-of-synch while deserializing from stream %s",
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
    /** a map from target offsets to source offsets */
    private FeatureImpl[] tgtF2srcF;

    // next for managing index removes / readds
    private FSsTobeAddedbackSingle addbackSingle;

    // for handling aux heaps with type mapping which may skip some things in the target
    // An amount that needs to be added to the offset from target to account for
    // source types and features not in the target.
    //
    // Because this is only done for Delta CAS, it is guaranteed that the
    // target cannot contain types or features that are not in the source
    // (due to type merging)
    // int[] srcHeapIndexOffset;
    //
    // Iterator<AuxSkip>[] srcSkipIt; // iterator over skip points
    // AuxSkip[] srcNextSkipped; // next skipped
    // int[] srcNextSkippedIndex;

    private void readModifiedFSs() throws IOException {
      final int modFSsLength = readVnumber(control_dis);
      if (TRACE_MOD_DES) {
        System.out.format("trace des mod nbr mods = %,d%n", modFSsLength);
      }
      int prevSeq = 0;

      if ((modFSsLength > 0) && (allowPreexistingFS == AllowPreexistingFS.disallow)) {
        throw new CASRuntimeException(CASRuntimeException.DELTA_CAS_PREEXISTING_FS_DISALLOWED,
                String.format("%,d pre-existing Feature Structures modified", modFSsLength));
      }

      // if (isTypeMapping) {
      // for (int i = 0; i < AuxHeapsCount; i++) {
      // srcHeapIndexOffset[i] = 0;
      // srcSkipIt[i] = fsStartIndexes.skips.get(i).iterator();
      // srcNextSkipped[i] = (srcSkipIt[i].hasNext()) ? srcSkipIt[i].next() : null;
      // srcNextSkippedIndex[i] = (srcNextSkipped[i] == null) ? Integer.MAX_VALUE :
      // srcNextSkipped[i].skipIndex;
      // }
      // }

      for (int i = 0; i < modFSsLength; i++) {
        final int seqNbrModified = readDiff(fsIndexes_dis, prevSeq);
        // iHeap = readVnumber(fsIndexes_dis) + prevFs;
        prevSeq = seqNbrModified;
        // prevFs = iHeap;

        TOP fs = fsStartIndexes.getSrcFsFromTgtSeq(seqNbrModified);
        if (fs == null) {
          // never happen because in the delta CAS ts system use-case, the
          // target is always a subset of the source
          // due to type system merging
          throw Misc.internalError();
        }

        TypeImpl srcType = fs._getTypeImpl();
        if (isTypeMapping) {
          tgtF2srcF = typeMapper.getSrcFeatures(typeMapper.mapTypeSrc2Tgt(srcType));
        }

        final int numberOfModsInThisFs = readVnumber(fsIndexes_dis);

        if (TRACE_MOD_DES) {
          System.out.format("  %,d tr de mod fsId: %,d nbrMods: %,d type: %s%n", i, fs._id,
                  numberOfModsInThisFs, srcType.getShortName());
        }
        if (srcType.isAuxStoredArray()) {
          // @formatter:off
          /**************************************************
           * *** This strange split is to be compatible with v2 form 6 ***
           * 
           * handle aux byte, short, long array modifications
           *   Note: boolean stored in byte array
           *   Note: strings are heap-store-arrays
           **************************************************/
            // @formatter:on
          readModifiedAuxHeap(numberOfModsInThisFs, fs, srcType);
        } else {
          // https://issues.apache.org/jira/browse/UIMA-4100
          // cas.removeFromCorruptableIndexAnyView(iHeap, indexToDos);
          readModifiedMainHeap(numberOfModsInThisFs, fs, srcType);
        }
      }
    }

    // update the byte/short/long aux heap entries
    // for arrays only. Longs/Doubles have changes in the "main heap"
    /*
     * update the byte/short/long aux heap entries (not done in v3) No aux heap offset adjustments
     * needed since we get the accurate source start point from the source heap
     */
    private void readModifiedAuxHeap(int numberOfMods, TOP fs, TypeImpl srcType)
            throws IOException {
      int prevOffset = 0;
      // final int auxHeapIndex = heap[iHeap + 2];

      final SlotKind kind = srcType.getComponentSlotKind(); // get kind of element
      final BooleanArray booleanArray = (kind == SlotKind.Slot_BooleanRef) ? (BooleanArray) fs
              : null;
      final ByteArray byteArray = (kind == SlotKind.Slot_ByteRef) ? (ByteArray) fs : null;
      final ShortArray shortArray = (kind == SlotKind.Slot_ShortRef) ? (ShortArray) fs : null;
      final LongArray longArray = (kind == SlotKind.Slot_LongRef) ? (LongArray) fs : null;
      final DoubleArray doubleArray = (kind == SlotKind.Slot_DoubleRef) ? (DoubleArray) fs : null;

      for (int i2 = 0; i2 < numberOfMods; i2++) {
        final int offset = readVnumber(fsIndexes_dis) + prevOffset;
        prevOffset = offset;

        if (TRACE_MOD_DES) {
          System.out.format("  tr de mod array fsId: %,d for index: %,d%n", fs._id, offset);
        }

        switch (kind) {
          case Slot_BooleanRef:
            booleanArray.set(offset, byte_dis.readByte() == 1);
            break;
          case Slot_ByteRef:
            byteArray.set(offset, byte_dis.readByte());
            break;

          case Slot_ShortRef: {
            final short v = (short) readDiff(int_dis, vPrevModShort);
            vPrevModShort = v;
            shortArray.set(offset, v);
            break;
          }

          case Slot_LongRef: {
            final long v = readLongOrDouble(kind, vPrevModLong);
            vPrevModLong = v;
            longArray.set(offset, v);
            break;
          }

          case Slot_DoubleRef: {
            doubleArray.set(offset, CASImpl.long2double(readDouble()));
          }
            break;
          default:
            Misc.internalError();
        } // end of switch
      } // end of loop over all changed slots in the array
    }

    /**
     * This used for both int/float/string/fs arrays and int/float/string and other feature slots
     * Also used for Long/Double reading
     * 
     * @param numberOfMods
     *          number of modifications.
     * @param fs
     *          the modified feature structure
     * @param srcType
     *          the type of the modified feature structure
     */
    private void readModifiedMainHeap(int numberOfMods, TOP fs, TypeImpl srcType)
            throws IOException {
      int iPrevTgtOffsetInFs = 0;
      boolean wasRemoved = false; // set to true when removed from index to stop further testing
      addbackSingle = cas.getAddbackSingle();
      final boolean isArray = srcType.isArray();
      final FeatureImpl[] features = isArray ? null : srcType.getFeatureImpls();

      for (int i = 0; i < numberOfMods; i++) {
        // offset may be index or feature offset
        final int tgtOffsetInFs = readVnumber(fsIndexes_dis) + iPrevTgtOffsetInFs;
        iPrevTgtOffsetInFs = tgtOffsetInFs;

        // srcOffsetInFs is either array index or feature offset
        final int srcOffsetInFs = (!isArray && isTypeMapping) ? tgtF2srcF[tgtOffsetInFs].getOffset()
                : tgtOffsetInFs;

        // srcOffset must be >= 0 because if type mapping, and delta cas being deserialized,
        // all of the target features would have been merged into the source ones.
        assert (srcOffsetInFs >= 0);
        FeatureImpl srcFeat = (features == null) ? null : features[srcOffsetInFs];
        final SlotKind kind = srcType.isArray() ? srcType.getComponentSlotKind()
                : srcFeat.getSlotKind();
        // System.out.format("mainHeapModRead type: %s slot: %s%n", typeInfo, kind);

        if (!isArray && kind != SlotKind.Slot_HeapRef && !wasRemoved) {
          wasRemoved = cas.checkForInvalidFeatureSetting(fs, srcFeat.getCode(), addbackSingle);
        }

        if (TRACE_MOD_DES) {
          System.out.format(
                  "  tr de mod fsId: %,d for mod# %,d isArray: %s, indx-or-srcOffset: %,d type: %s%n",
                  iHeap, i, Boolean.toString(isArray), srcOffsetInFs,
                  (srcFeat == null) ? "null" : srcFeat.getShortName());
        }

        switch (kind) {

          case Slot_HeapRef: {
            final int tgtSeq = readDiff(heapRef_dis, prevModHeapRefTgtSeq);
            // System.out.format("debug deser mod heapRef prev %,d this %d%n", prevModHeapRefTgtSeq,
            // tgtSeq);
            prevModHeapRefTgtSeq = tgtSeq;
            final TOP v = getRefVal(tgtSeq);
            if (isArray) {
              ((FSArray) fs).set(srcOffsetInFs, v);
            } else {
              fs.setFeatureValue(srcFeat, v);
            }
          }
            break;

          case Slot_Int: {
            final int v = readDiff(int_dis, vPrevModInt);
            vPrevModInt = v;
            if (isArray) {
              ((IntegerArray) fs).set(srcOffsetInFs, v);
            } else {
              fs.setIntValue(srcFeat, v);
            }
          }
            break;

          case Slot_Short: {
            final short v = (short) readDiff(int_dis, vPrevModShort);
            vPrevModShort = v;
            // short arrays were not on main heap
            fs.setShortValue(srcFeat, v);
          }
            break;

          // can't be short array because that's on the aux heap

          case Slot_LongRef: {
            final long v = readLongOrDouble(kind, vPrevModLong);
            vPrevModLong = v;
            // long arrays were not on main heap
            fs.setLongValue(srcFeat, v);
          }
            break;

          case Slot_DoubleRef: {
            final long v = readDouble();
            // double arrays were not on main heap
            fs.setDoubleValue(srcFeat, CASImpl.long2double(v));
          }
            break;

          case Slot_Float: {
            float v = Float.intBitsToFloat(readFloat());
            if (isArray) {
              ((FloatArray) fs).set(srcOffsetInFs, v);
            } else {
              fs.setFloatValue(srcFeat, v);
            }
          }
            break;

          case Slot_StrRef: {
            String v = readString(true); // true means to store the result
            if (isArray) {
              ((StringArray) fs).set(srcOffsetInFs, v);
            } else {
              fs.setStringValue(srcFeat, v);
            }
          }
            break;

          case Slot_Boolean:
            fs.setBooleanValue(srcFeat, byte_dis.readByte() == 1);
            break;
          case Slot_Byte:
            fs.setByteValue(srcFeat, byte_dis.readByte());
            break;

          default:
            Misc.internalError();
        } // end of switch
      } // end of for loop over all FS

      cas.addbackSingleIfWasRemoved(wasRemoved, fs);
    }
  }

  // *******************************************************************
  // methods common to serialization / deserialization etc.
  // *******************************************************************

  // @formatter:off
  /*
   * This routine uses the same "scanning" to do two completely different things:
   *   The first thing is to generate an ordered set (by heap addr) 
   *   of all FSs that are to be serialized:
   *     because they are in some index, or
   *     are pointed to by something that is in some index (recursively)
   *   excluding those below the mark
   *   excluding those that are not in the target type system
   *   
   *   The second thing is to serialize out the index information.
   *   This step has to wait until the first time call has completed and 
   *   the fsStartIndexes instance has a chance to be built.
   * 
   * The cas is passed in so that the Compare can use this for two different CASes
   * 
   */
  // @formatter:on
  private void processIndexedFeatureStructures(final CASImpl cas1, boolean isWrite)
          throws IOException {
    if (!isWrite) {
      // always have form 6 do just reachables, to mimic what v2 did
      AllFSs allFSs;
      try (AutoCloseableNoException a = cas1.ll_enableV2IdRefs(false)) {
        allFSs = new AllFSs(cas1, mark, isTypeMapping ? fs -> isTypeInTgt(fs) : null,
                isTypeMapping ? typeMapper : null).getAllFSsAllViews_sofas_reachable();
        // AllFSs internally already causes _save_to_cas_data() to be called, so we have to add all
        // the FSes that are returned here to the uimaSerializableSavedToCas tracking set
        allFSs.getAllFSs().forEach(fs -> uimaSerializableSavedToCas.add(fs._id));
      }
      fssToSerialize = CASImpl.filterAboveMark(allFSs.getAllFSsSorted(), mark);
      foundFSs = allFSs.getAllNew();
      foundFSsBelowMark = allFSs.getAllBelowMark();
      return;
    }

    // if (doMeasurements) {
    // sm.statDetails[fsIndexes_i].original = fsIndexes.length * 4 + 1;
    // }
    writeVnumber(control_i, cas1.getNumberOfViews());
    writeVnumber(control_i, cas1.getNumberOfSofas());
    if (doMeasurements) {
      sm.statDetails[fsIndexes_i].incr(1); // an approximation - probably correct
      sm.statDetails[fsIndexes_i].incr(1);
    }

    // write or enqueue the sofas
    final FSIterator<Sofa> it = cas1.getSofaIterator();
    while (it.hasNext()) {
      Sofa sofa = it.nextNvc();
      // for delta only write new sofas
      if (!isSerializingDelta || mark.isNew(sofa)) {
        // never returns -1, because this is for the sofa fs, and that's never filtered
        final int v = getTgtSeqFromSrcFS(sofa);
        writeVnumber(control_i, v); // version 1

        if (doMeasurements) {
          sm.statDetails[fsIndexes_i].incr(DataIO.lengthVnumber(v));
        }
      }
    }
    TypeImpl topType = cas1.getTypeSystemImpl().getTopType();

    // write (id's only, for index info) and/or enqueue indexed FSs, either all, or (for delta
    // writes) the added/deleted/reindexed ones
    cas1.forAllViews(view -> {
      processFSsForView(true, // is enqueue
              isSerializingDelta ? view.indexRepository.getAddedFSs().stream()
                      : view.indexRepository.<TOP> getIndexedFSs(topType).stream());
      if (isSerializingDelta) {
        // for write/delta, write out (but don't enqueue) the deleted/reindexed FSs
        processFSsForView(false, view.indexRepository.getDeletedFSs().stream());
        processFSsForView(false, view.indexRepository.getReindexedFSs().stream());
      }
    });
  }

  /**
   * processes one view's worth of feature structures
   */
  private void processFSsForView(final boolean isEnqueue, Stream<TOP> fss) {
    // prev id and entries written as a captured value in context

    final int prevId = 0, entriesWritten = 1; // indexes into context
    // Stream<TOP> stream = (fssx instanceof FSIterator<?>)
    // ? ((FSIterator<TOP>)fssx).asStream()
    // : ((Set<TOP>)fssx).stream();

    final int[] context = { 0, 0 };
    fss.sorted(FeatureStructureImplC::compare).forEachOrdered(fs -> {
      // skip write if typemapping, and target type isn't there
      // if (fs._id == 199) {
      // System.out.println("debug write out fs id 199 as 119");
      // }
      if (isTypeInTgt(fs)) {

        final int tgtId = getTgtSeqFromSrcFS(fs);
        assert (tgtId > 0);
        final int delta = tgtId - context[prevId];
        context[prevId] = tgtId;

        try {
          writeVnumber(fsIndexes_dos, delta);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        context[entriesWritten]++;
        if (doMeasurements) {
          sm.statDetails[fsIndexes_i].incr(DataIO.lengthVnumber(delta));
        }
      } // end of conditional write

      if (isEnqueue) {
        enqueueFS(fs);
      }
    });
    try {
      writeVnumber(control_dos, context[entriesWritten]);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (doMeasurements) {
      sm.statDetails[typeCode_i].incr(DataIO.lengthVnumber(entriesWritten));
    }
  }

  /**
   * Add Fs to toBeProcessed and set foundxxx bit - skip this if doesn't exist in target type system
   */
  private void enqueueFS(TOP fs) {
    if (null == fs || !isTypeInTgt(fs)) {
      return;
    }

    final int id = fs._id;

    if (!isSerializingDelta || mark.isNew(fs)) { // separately track items below the line
      if (!foundFSs.contains(id)) {
        foundFSs.add(id);
        toBeScanned.add(fs);
      }
    } else {
      if (!foundFSsBelowMark.contains(id)) {
        foundFSsBelowMark.add(id);
        toBeScanned.add(fs);
      }
    }
  }

  private boolean isTypeInTgt(TOP fs) {
    return !isTypeMapping || (null != typeMapper.mapTypeSrc2Tgt(fs._getTypeImpl()));
  }

  // private boolean isTypeInTgt(int typecode) {
  // return !isTypeMapping || (null != typeMapper.mapTypeSrc2Tgt(typecode));
  // }

  // private void processRefedFSs() {
  // for (int i = 0; i < toBeScanned.size(); i++) {
  // enqueueFeatures(toBeScanned.get(i));
  // }
  // }

  // /**
  // * Enqueue all FSs reachable from features of the given FS.
  // */
  // private void enqueueFeatures(TOP fs) {
  // if (fs instanceof FSArray) {
  // for (TOP item : ((FSArray)fs)._getTheArray()) {
  // enqueueFS(item);
  // }
  // return;
  // }
  //
  // // not an FS Array
  // if (fs instanceof CommonArrayFS) {
  // return;
  // }
  //
  // final TypeImpl srcType = fs._getTypeImpl();
  // for (FeatureImpl srcFeat : srcType.getFeatureImpls()) {
  // if (isTypeMapping) {
  // FeatureImpl tgtFeat = typeMapper.getTgtFeature(srcType, srcFeat);
  // if (tgtFeat == null) {
  // continue; // skip enqueue if not in target
  // }
  // }
  // if (srcFeat.getRangeImpl().isRefType) {
  // enqueueFS(fs._getFeatureValueNc(srcFeat));
  // }
  // }
  // }

  // @formatter:off
  /**
   * Serializing:
   *   Called at beginning of serialize, scans whole CAS or just delta CAS
   *   If doing delta serialization, fsStartIndexes is passed in, pre-initialized with a copy of the map info below the line.
   */
  // @formatter:on
  private void initSrcTgtIdMapsAndStrings() {

    int nextTgtId = isSerializingDelta ? mark.nextFSId : 1;

    // for delta serialization - the iterator is only for things above the line.

    for (TOP fs : fssToSerialize) {
      TypeImpl srcType = fs._getTypeImpl();
      TypeImpl tgtType = isTypeMapping ? typeMapper.mapTypeSrc2Tgt(srcType) : srcType;
      final boolean isIncludedType = (tgtType != null);

      fsStartIndexes.addItemId(fs, nextTgtId, isIncludedType); // maps src heap to tgt seq

      if (isIncludedType) {
        if (fs instanceof UimaSerializable && !uimaSerializableSavedToCas.contains(fs._id)) {
          ((UimaSerializable) fs)._save_to_cas_data();
          uimaSerializableSavedToCas.add(fs._id);
        }

        // for features in type -
        // strings: accumulate those strings that are in the target, if optimizeStrings != null
        // strings either in array, or in individual values
        // byte (array), short (array), long/double (instance or array): record if entries in aux
        // array are skipped
        // (not in the target). Note the recording will be in a non-ordered manner (due to possible
        // updates by
        // previous delta deserialization)

        // add strings for included types (only when serializing)
        if (os != null) {
          addStringsFromFS(fs);
        }

        // Advance to next Feature Structure, in both source and target heap frame of reference
        nextTgtId++;
      }
    }
  }

  // @formatter:off
  /**
   * Add all the strings ref'd by this FS.
   *   - if it is a string array, do all the array items
   *   - else scan the features and do all string-valued features, in feature offset order
   * For delta, this isn't done here - another routine driven by FsChange info does this.
   */
  // @formatter:on
  private void addStringsFromFS(TOP fs) {
    if (fs instanceof StringArray) {
      for (String s : ((StringArray) fs)._getTheArray()) {
        os.add(s);
      }
      return;
    }

    for (FeatureImpl fi : fs._getTypeImpl().getFeatureImpls()) {
      if (fi.getRange() instanceof TypeImpl_string) {
        os.add(fs._getStringValueNc(fi));
      }
    }
  }

  /**
   * Compare 2 CASes, with perhaps different type systems. If the type systems are different,
   * construct a type mapper and use that to selectively ignore types or features not in other type
   * system
   * 
   * The Mapper is from CAS1 -&gt; CAS2
   * 
   * When computing the things to compare from CAS1, filter to remove feature structures not
   * reachable via indexes or refs
   * 
   * @param c1
   *          CAS to compare
   * @param c2
   *          CAS to compare
   * @return true if equal (for types / features in both)
   */

  public boolean compareCASes(CASImpl c1, CASImpl c2) {
    return new CasCompare(c1, c2).compareCASes();
  }

  /**
   * 
   * @param f
   *          can be a DataOutputStream, an OutputStream a File
   * @return a data output stream
   * @throws FileNotFoundException
   *           passthru
   */
  private static DataOutputStream makeDataOutputStream(Object f) throws FileNotFoundException {
    if (f instanceof DataOutputStream) {
      return (DataOutputStream) f;
    }
    if (f instanceof OutputStream) {
      return new DataOutputStream((OutputStream) f);
    }
    if (f instanceof File) {
      FileOutputStream fos = new FileOutputStream((File) f);
      BufferedOutputStream bos = new BufferedOutputStream(fos);
      return new DataOutputStream(bos);
    }
    throw new RuntimeException(
            String.format("Invalid class passed to method, class was %s", f.getClass().getName()));
  }

  // for debugging
  // String printCasInfo(CASImpl cas) {
  // int heapsz= cas.getHeap().getNextId() * 4;
  // StringHeapDeserializationHelper shdh = cas.getStringHeap().serialize();
  //
  // int charssz = shdh.charHeap.length * 2;
  // int strintsz = cas.getStringHeap().getSize() * 8;
  // int strsz = charssz + strintsz;
  // int fsindexessz = cas.getIndexedFSs().length * 4;
  // int bytessz = cas.getByteHeap().getSize();
  // int shortsz = cas.getShortHeap().getSize() * 2;
  // int longsz = cas.getLongHeap().getSize() * 8;
  // int total = heapsz + strsz + fsindexessz + bytessz + shortsz + longsz;
  // return String.format("CAS info before compression: totalSize(bytes): %,d%n" +
  // " mainHeap: %,d(%d%%)%n" +
  // " Strings: [%,d(%d%%): %,d chars %,d ints]%n" +
  // " fsIndexes: %,d(%d%%)%n" +
  // " byte/short/long Heaps: [%,d %,d %,d]",
  // total,
  // heapsz, (100L*heapsz)/total,
  // strsz, (100L*strsz)/ total,
  // charssz, strintsz,
  // fsindexessz, (100L*fsindexessz) / total,
  // bytessz, shortsz, longsz
  // );
  // }

  /********************************************
   * Set up Streams
   * 
   * @throws FileNotFoundException
   *           passthru
   ********************************************/
  private void setupOutputStreams(Object out) throws FileNotFoundException {

    serializedOut = makeDataOutputStream(out);

    setupOutputStreams(cas, baosZipSources, dosZipSources);

    // arrayLength_dos = dosZipSources[arrayLength_i];
    // heapRef_dos = dosZipSources[heapRef_i];
    // int_dos = dosZipSources[int_i];
    byte_dos = dosZipSources[byte_i];
    // short_dos = dosZipSources[short_i];
    typeCode_dos = dosZipSources[typeCode_i];
    strOffset_dos = dosZipSources[strOffset_i];
    strLength_dos = dosZipSources[strLength_i];
    // long_High_dos = dosZipSources[long_High_i];
    // long_Low_dos = dosZipSources[long_Low_i];
    float_Mantissa_Sign_dos = dosZipSources[float_Mantissa_Sign_i];
    float_Exponent_dos = dosZipSources[float_Exponent_i];
    double_Mantissa_Sign_dos = dosZipSources[double_Mantissa_Sign_i];
    double_Exponent_dos = dosZipSources[double_Exponent_i];
    fsIndexes_dos = dosZipSources[fsIndexes_i];
    // strChars_dos = dosZipSources[strChars_i];
    control_dos = dosZipSources[control_i];
    strSeg_dos = dosZipSources[strSeg_i];
  }

  static void setupOutputStreams(CASImpl cas, ByteArrayOutputStream[] baosZipSources,
          DataOutputStream[] dosZipSources) {

    final int[] estimatedZipSize = new int[NBR_SLOT_KIND_ZIP_STREAMS]; // one entry for each output
                                                                       // stream kind

    // estimate model:
    // 33% of space in strings, 33% in heap, 33% other == divide est by 3
    // compr ratio for heap is 98%, == divide est by 50
    // avg # of v2 heap slots per fs = 5
    // avg bytes per slot = 4 + 4(to cover for off heap things) = 8
    int compr = cas.getLastUsedFsId() * 5 * 8 / 3 / 50;
    int compr1000 = Misc.nextHigherPowerOfX(Math.max(512, compr / 1000), 32); // = 576
    // 2nd arg is the number of bytes in the byte output stream, initially
    estimatedZipSize[typeCode_i] = Math.max(512, compr / 4); // /4 for ~4 slots per fs = 140,288
    // estimatedZipSize[boolean_i] =compr1000;
    estimatedZipSize[byte_i] = compr1000; // 576
    estimatedZipSize[short_i] = compr1000; // 576
    estimatedZipSize[int_i] = Math.max(1024, compr1000); // 1024
    estimatedZipSize[arrayLength_i] = compr1000; // 576
    estimatedZipSize[float_Mantissa_Sign_i] = compr1000;
    estimatedZipSize[float_Exponent_i] = compr1000;
    estimatedZipSize[double_Mantissa_Sign_i] = compr1000;
    estimatedZipSize[double_Exponent_i] = compr1000;
    estimatedZipSize[long_High_i] = compr1000;
    estimatedZipSize[long_Low_i] = compr1000;
    estimatedZipSize[heapRef_i] = Math.max(1024, compr1000); // 1024
    estimatedZipSize[strOffset_i] = Math.max(512, compr / 4); // 140,288
    estimatedZipSize[strLength_i] = Math.max(512, compr / 4); // 140,288
    estimatedZipSize[fsIndexes_i] = Math.max(512, compr / 8); // /4 for ~4 slots/fs, / 2 for #
                                                              // indexed
    estimatedZipSize[strChars_i] = Math.max(512, compr / 4); // strings compress better
    estimatedZipSize[control_i] = 128;

    for (int i = 0; i < baosZipSources.length; i++) {
      setupOutputStream(i, estimatedZipSize[i], baosZipSources, dosZipSources);
    }
  }

  static private void setupOutputStream(int i, int size, ByteArrayOutputStream[] baosZipSources,
          DataOutputStream[] dosZipSources) {
    // set up output stream
    baosZipSources[i] = new ByteArrayOutputStream(size);
    dosZipSources[i] = new DataOutputStream(baosZipSources[i]);
  }

  private void setupReadStreams() throws IOException {
    /************************************************
     * Setup all the input streams with inflaters
     ************************************************/
    final int nbrEntries = deserIn.readInt(); // number of compressed streams
    IntVector idxAndLen = new IntVector(nbrEntries * 3);
    for (int i = 0; i < nbrEntries; i++) {
      idxAndLen.add(deserIn.readUnsignedByte()); // slot ordinal number
      idxAndLen.add(deserIn.readInt()); // compressed size, bytes
      idxAndLen.add(deserIn.readInt()); // decompressed size, bytes (not currently used)
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

  private void setupReadStream(int slotIndex, int bytesCompr, int bytesOrig) throws IOException {
    byte[] b = new byte[bytesCompr + 1];
    deserIn.readFully(b, 0, bytesCompr); // this leaves 1 extra 0 byte at the end
    // which may be required by Inflater with nowrap option - see Inflater javadoc

    // testing inflate speed
    // long startTime = System.currentTimeMillis();
    // inflater.reset();
    // inflater.setInput(b);
    // byte[] uncompressed = new byte[bytesOrig];
    // int uncompressedLength = 0;
    // try {
    // uncompressedLength = inflater.inflate(uncompressed);
    // } catch (DataFormatException e) {
    // throw new RuntimeException(e);
    // }
    // if (uncompressedLength != bytesOrig) {
    // throw new RuntimeException();
    // }
    // System.out.format("Decompress %s took %,d ms%n",
    // SlotKind.values()[slotIndex], System.currentTimeMillis() - startTime);
    //
    // dataInputs[slotIndex] = new DataInputStream(new ByteArrayInputStream(uncompressed));
    Inflater inflater = new Inflater(true);
    inflaters[slotIndex] = inflater; // save to be able to call end() when done.
    ByteArrayInputStream baiStream = new ByteArrayInputStream(b);
    int zipBufSize = Math.max(1 << 10, bytesCompr); // 32768 == 1<< 15. Tuned by trials on 2015
                                                    // intel i7
    // caches: L1 = 128KB L2 = 1M L3 = 6M
    // increasing the max causes cache dumping on this machine, and things slow down
    InflaterInputStream iis = new InflaterInputStream(baiStream, inflater, zipBufSize);
    // increasing the following buffer stream buffer size also seems to slow things down
    dataInputs[slotIndex] = new DataInputStream(new BufferedInputStream(iis, zipBufSize * 1));
  }

  private void closeDataInputs() {
    for (DataInputStream is : dataInputs) {
      if (null != is) {
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

  /*********************************************
   * HEADERS
   * 
   * @throws IOException
   *           passthru
   *********************************************/

  private Header readHeader(InputStream istream) throws IOException {
    deserIn = CommonSerDes.maybeWrapToDataInputStream(istream);
    Header h = CommonSerDes.readHeader(deserIn);

    if (!h.isCompressed) {
      throw new RuntimeException(
              "non-compressed invalid object passed to BinaryCasSerDes6 deserialize");
    }

    if (!h.form6) {
      throw new RuntimeException(String.format(
              "Wrong version: %d in input source passed to BinaryCasSerDes6 for deserialization",
              version));
    }

    isReadingDelta = isDelta = h.isDelta;
    return h;
  }

  /*
   * ******************************************* String info
   *********************************************/
  private void writeStringInfo() throws IOException {
    String[] commonStrings = os.getCommonStrings();
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
        float excess = (len / commonStrings[i].length()) - 1; // excess over length 1
        int encAs2 = (int) (excess * commonStrings[i].length());

        // simulate histo for all the chars, as 1 or 2 byte UTF8 encoding
        sm.statDetails[strChars_i].countTotal += commonStrings[i].length(); // total chars accum
        sm.statDetails[strChars_i].c[0] = commonStrings[i].length() - encAs2;
        sm.statDetails[strChars_i].c[1] = encAs2;
        sm.statDetails[strChars_i].lengthTotal += len; // total as UTF-8 encode
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
              + os.getSavedCharsSubstr() * 2 + commonStringsLength * 2;
      final int stringHeapStart = isSerializingDelta ? mark.nextFSId : 1;
      final int stringHeapEnd = stringHeapObj.getSize();
      sm.statDetails[strLength_i].original = (stringHeapEnd - stringHeapStart) * 4;
      sm.statDetails[strOffset_i].original = (stringHeapEnd - stringHeapStart) * 4;
    }

  }

  // @formatter:off
  /**
   * For Serialization only.
   * 
   * Map src FS to tgt seq number:
   *   fs == null -> 0
   *   type not in target -> 0
   *   map src fs._id to tgt seq
   * @return 0 or the mapped src id
   */
  // @formatter:on
  private int getTgtSeqFromSrcFS(TOP fs) {
    if (null == fs) {
      return 0;
    }
    if (isTypeMapping) {
      if (typeMapper.mapTypeSrc2Tgt(fs._getTypeImpl()) == null) {
        return 0;
      }
    }
    int v = fsStartIndexes.getTgtSeqFromSrcAddr(fs._id);
    assert (v != -1); // tgt must always be present at this point
    return v;
  }

  TypeSystemImpl getTgtTs() {
    return tgtTs;
  }

  // number of views: cas.getNumberOfViews()
  // number of sofas cas.getNumberOfSofas()
  // [sofa-1 ... sofa-n] cas.getSofaIterator()
  // number of FS indexed in View1 cas.getIndexRepository().getAllIndexedFS().ll_indexSize()
  // [FS-1 ... FS-n] cas.getIndexRepository().getAllIndexedFS()
}
