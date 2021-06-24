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

import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Int;

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
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.uima.UimaSerializable;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.Marker;
import org.apache.uima.cas.impl.CASImpl.FsChange;
import org.apache.uima.cas.impl.FSsTobeAddedback.FSsTobeAddedbackSingle;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;
import org.apache.uima.internal.util.Int2ObjHashMap;
import org.apache.uima.internal.util.IntListIterator;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.Obj2IntIdentityHashMap;
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
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.impl.DataIO;
import org.apache.uima.util.impl.OptimizeStrings;
import org.apache.uima.util.impl.SerializationMeasures;

// @formatter:off
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
 * Properties of Form 4:
 *   1) (Change from V2) Indexes are used to determine what gets serialized, because there's no "heap" to walk,
 *      unless the v2-id-mode is in effect.
 *      
 *   2) The number used for references to FSs is a sequentially incrementing one, starting at 1
 *       This allows better compression.
 *   
 *   
 */
// @formatter:on
public class BinaryCasSerDes4 implements SlotKindsConstants {
  private static final boolean TRACE_SER = false;
  private static final boolean TRACE_DES = false;

  private static final boolean TRACE_DOUBLE = false;
  // private static final boolean TRACE_INT = false;
  public static final int TYPECODE_COMPR = 8;

  // public static final boolean CHANGE_FS_REFS_TO_SEQUENTIAL = true; // currently unreferenced
  // may add more later - to specify differing trade-offs between speed and compression
  public enum Compression {
    None, Compress
  };

  public static final boolean IS_DIFF_ENCODE = true;
  public static final boolean CAN_BE_NEGATIVE = true;
  public static final boolean IGNORED = true;
  public static final boolean IN_MAIN_HEAP = true;

  // @formatter:off
  /*
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
  // @formatter:on

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

  /**
   * Things set up for one instance of this class, and reuse-able
   */
  final private TypeSystemImpl ts;
  final private boolean doMeasurements;

  final TypeImpl fsArrayType;

  /**
   * Things shared between serialization and deserialization
   */

  /**
   * 
   * @param ts
   *          the type system
   * @param doMeasurements
   *          - normally set this to false.
   */
  public BinaryCasSerDes4(TypeSystemImpl ts, boolean doMeasurements) {
    this.ts = ts;
    this.doMeasurements = doMeasurements;
    this.fsArrayType = ts.fsArrayType;
  }

  /**
   * 
   * @param cas
   *          CAS to serialize
   * @param out
   *          output object
   * @param trackingMark
   *          tracking mark (for delta serialization)
   * @param compressLevel
   *          -
   * @param compressStrategy
   *          -
   * @return null or serialization measurements (depending on setting of doMeasurements)
   * @throws IOException
   *           if the marker is invalid
   */
  public SerializationMeasures serialize(AbstractCas cas, Object out, Marker trackingMark,
          CompressLevel compressLevel, CompressStrat compressStrategy) throws IOException {
    SerializationMeasures sm = (doMeasurements) ? new SerializationMeasures() : null;
    CASImpl casImpl = (CASImpl) ((cas instanceof JCas) ? ((JCas) cas).getCas() : cas);
    if (null != trackingMark && !trackingMark.isValid()) {
      throw new CASRuntimeException(CASRuntimeException.INVALID_MARKER, "Invalid Marker.");
    }

    Serializer serializer = new Serializer(casImpl, makeDataOutputStream(out),
            (MarkerImpl) trackingMark, sm, compressLevel, compressStrategy, false);

    serializer.serialize();
    return sm;
  }

  public void serializeWithTsi(CASImpl casImpl, Object out) throws IOException {
    Serializer serializer = new Serializer(casImpl, makeDataOutputStream(out), null, null,
            CompressLevel.Default, CompressStrat.Default, true);
    serializer.serialize();
  }

  public SerializationMeasures serialize(AbstractCas cas, Object out, Marker trackingMark,
          CompressLevel compressLevel) throws IOException {
    return serialize(cas, out, trackingMark, compressLevel, CompressStrat.Default);
  }

  public SerializationMeasures serialize(AbstractCas cas, Object out, Marker trackingMark)
          throws IOException {
    return serialize(cas, out, trackingMark, CompressLevel.Default, CompressStrat.Default);
  }

  public SerializationMeasures serialize(AbstractCas cas, Object out) throws IOException {
    return serialize(cas, out, null);
  }

  public void deserialize(CASImpl cas, InputStream deserIn, boolean isDelta, CommonSerDes.Header h)
          throws IOException {
    DataInput in = (DataInput) deserIn;
    Deserializer deserializer = new Deserializer(cas, in, isDelta);
    deserializer.deserialize(h);
  }

  /**
   * Class instantiated once per serialization Multiple serializations in parallel supported, with
   * multiple instances of this
   */

  private class Serializer {
    final private DataOutputStream serializedOut; // where to write out the serialized result
    final private CASImpl baseCas; // cas being serialized
    final private BinaryCasSerDes bcsd;
    final private MarkerImpl mark; // the mark to serialize from

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

    // final private int[] heap; // main heap
    /** start of heap, in v2 pseudo-addr coordinates */
    private int heapStart;
    /** end of heap, in v2 pseudo-addr coordinates = addr of last + length of last */
    private int heapEnd;
    // final private LongHeap longHeapObj;
    // final private ShortHeap shortHeapObj;
    // final private ByteHeap byteHeapObj;

    final private boolean isDelta; // if true, there is a marker indicating the start spot(s)
    final private boolean isTsi; // true to include the type system and indexes definition
    final private boolean doMeasurement; // if true, doing measurements
    // final private ComprItemRefs fsStartIndexes = (CHANGE_FS_REFS_TO_SEQUENTIAL) ? new
    // ComprItemRefs() : null;
    // final private int[] typeCodeHisto = new int[ts.getTypeArraySize()];
    // final private Integer[] serializedTypeCode2Code = new Integer[ts.getTypeArraySize()]; //
    // needs to be Integer to get comparator choice
    // final private int[] estimatedZipSize = new int[NBR_SLOT_KIND_ZIP_STREAMS]; // one entry for
    // each output stream kind
    final private OptimizeStrings os;
    final private CompressLevel compressLevel;
    final private CompressStrat compressStrategy;

    // private int iPrevHeap; // 0 or heap addr of previous instance of current type
    /**
     * For differencing when reading and writing. Also used for arrays to difference the 0th
     * element.
     */
    final private TOP prevFsByType[];
    private TOP prevFs;

    private boolean only1CommonString; // true if only one common string

    // final private CommonCompressedSerialization ccs;

    // speedups

    // any use of these means caller handles measurement
    // some of these are never used, because the current impl
    // is using the _i form to get measurements done
    // final private DataOutputStream arrayLength_dos;
    // final private DataOutputStream heapRef_dos;
    // final private DataOutputStream int_dos;
    final private DataOutputStream byte_dos;
    // final private DataOutputStream short_dos;
    final private DataOutputStream typeCode_dos;
    final private DataOutputStream strOffset_dos;
    final private DataOutputStream strLength_dos;
    // final private DataOutputStream long_High_dos;
    // final private DataOutputStream long_Low_dos;
    final private DataOutputStream float_Mantissa_Sign_dos;
    final private DataOutputStream float_Exponent_dos;
    final private DataOutputStream double_Mantissa_Sign_dos;
    final private DataOutputStream double_Exponent_dos;
    final private DataOutputStream fsIndexes_dos;
    // final private DataOutputStream strChars_dos;
    final private DataOutputStream control_dos;
    final private DataOutputStream strSeg_dos;

    final private CommonSerDesSequential csds;
    /**
     * convert between FSs and "sequential" numbers This is for compression efficiency and also is
     * needed for backwards compatibility with v2 serialization forms, where index information was
     * written using "sequential" numbers Note: This may be identity map, but may not in the case
     * for V3 where some FSs are GC'd
     * 
     * Contrast with fs2addr and addr2fs in csds - these use the pseudo v2 addresses as the int
     */
    private final Obj2IntIdentityHashMap<TOP> fs2seq = new Obj2IntIdentityHashMap<>(TOP.class,
            TOP._singleton);
    // private final Int2ObjHashMap<TOP, TOP> seq2fs = new Int2ObjHashMap<>(TOP.class);

    /**
     * Set of FSes on which UimaSerializable _save_to_cas_data has already been called.
     */
    private PositiveIntSet uimaSerializableSavedToCas;

    /**
     * 
     * @param cas
     *          -
     * @param serializedOut
     *          -
     * @param mark
     *          -
     * @param sm
     *          -
     * @param compressLevel
     *          -
     * @param compressStrategy
     *          -
     */

    private Serializer(CASImpl cas, DataOutputStream serializedOut, MarkerImpl mark,
            SerializationMeasures sm, CompressLevel compressLevel, CompressStrat compressStrategy,
            boolean isTsi) {
      this.baseCas = cas.getBaseCAS();
      this.bcsd = cas.getBinaryCasSerDes();
      this.isDelta = (mark != null);
      // this.csds = getCsds(baseCas, isDelta);
      // this.ccs = new CommonCompressedSerialization(
      // new CommonSerDesTypeMap(cas.getTypeSystemImpl(), cas.getTypeSystemImpl()), // no type
      // mapping
      // mark);
      this.serializedOut = serializedOut;
      this.mark = mark;
      this.sm = sm;
      this.compressLevel = compressLevel;
      this.compressStrategy = compressStrategy;
      this.isTsi = isTsi;

      doMeasurement = (sm != null);

      // heap = cas.getHeap().heap;
      // heapEnd = cas.getHeap().getCellsUsed();

      //
      // stringHeapObj = cas.getStringHeap();
      // longHeapObj = cas.getLongHeap();
      // shortHeapObj = cas.getShortHeap();
      // byteHeapObj = cas.getByteHeap();

      os = new OptimizeStrings(doMeasurement);

      BinaryCasSerDes6.setupOutputStreams(baseCas, baosZipSources, dosZipSources);

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
      uimaSerializableSavedToCas = new PositiveIntSet_impl(1024, 1, 1024);

      this.prevFsByType = new TOP[ts.getTypeArraySize()];
      csds = getCsds(baseCas, isDelta);
      // getCsds() internally already causes _save_to_cas_data() to be called (via AllFSs), so we
      // have to add all the FSes that are returned here to the uimaSerializableSavedToCas tracking
      // set
      csds.getSortedFSs().stream().map(FeatureStructureImplC::_id)
              .forEach(uimaSerializableSavedToCas::add);
      assert null != csds;
    }

    /**
     * Form 4 serialization is tied to the layout of V2 Feature Structures in heaps. It does not
     * walk the indexes to serialize just those FSs that are reachable.
     * 
     * For V3, it scans the CASImpl.id2fs information and serializes those (except those which have
     * been GC'd). The seq numbers of the target incrementing sequentially will be different from
     * the source id's if some FSs were GC'd.
     * 
     * To determine for delta what new strings and new
     *
     * @throws IOException
     */
    private void serialize() throws IOException {

      synchronized (baseCas.svd) {

        // if (doMeasurement) {
        // System.out.println(printCasInfo(baseCas));
        // sm.origAuxBytes = baseCas.getByteHeap().getSize();
        // sm.origAuxShorts = baseCas.getShortHeap().getSize() * 2;
        // sm.origAuxLongs = baseCas.getLongHeap().getSize() * 8;
        // sm.totalTime = System.currentTimeMillis();
        // }

        /************************
         * Write standard header
         ************************/
        CommonSerDes.createHeader().v3().seqVer(2) // 0 - original, 1 - UIMA-4743, 2 - v3
                .form4().delta(isDelta).typeSystemIndexDefIncluded(isTsi).write(serializedOut);

        if (isTsi) {
          CasIOUtils.writeTypeSystem(baseCas, serializedOut, true);
        }

        if (TRACE_SER)
          System.out.println("Form4Ser start, delta: " + (isDelta ? "true" : "false"));
        /*******************************************************************************
         * Setup tables that map to v2 "addresses" - needed for backwards compatibility fs2addr -
         * feature structure to address addr2fs - address to feature structure sortedFSs - sorted by
         * addr (sorted by id)
         *******************************************************************************/
        final int origHeapEnd = csds.getHeapEnd(); // csds guaranteed non-null by constructor
        if (isDelta) {
          csds.setup(mark, origHeapEnd); // add additional above the line items to csds
        } // otherwise was initialized when initially set up

        /**
         * prepare fs < -- > seq maps done for entire cas (in the case of a mark)
         */
        fs2seq.clear();
        // seq2fs.clear();
        int seq = 1; // origin 1

        final List<TOP> localSortedFSs = csds.getSortedFSs();
        for (TOP fs : localSortedFSs) {
          fs2seq.put(fs, seq++);
          // seq2fs.put(seq++, fs);
          if (fs instanceof UimaSerializable && !uimaSerializableSavedToCas.contains(fs._id)) {
            ((UimaSerializable) fs)._save_to_cas_data();
            uimaSerializableSavedToCas.add(fs._id);
          }
        }

        // the sort order is on the id (e.g. creation order)
        List<TOP> newSortedFSs = CASImpl.filterAboveMark(csds.getSortedFSs(), mark); // returns all
                                                                                     // if mark not
                                                                                     // set

        // *************************
        // Strings
        // For delta, to determine "new" strings that should be serialized,
        // use the same method as used in Binary (plain) serialization.
        // *************************
        for (TOP fs : newSortedFSs) {
          extractStrings(fs);
        }

        if (isDelta) {
          FsChange[] fssModified = baseCas.getModifiedFSList();

          // also add in all modified strings
          for (FsChange fsChange : fssModified) {
            if (fsChange.fs instanceof UimaSerializable
                    && !uimaSerializableSavedToCas.contains(fsChange.fs._id)) {
              ((UimaSerializable) fsChange.fs)._save_to_cas_data();
              uimaSerializableSavedToCas.add(fsChange.fs._id);
            }
            extractStringsFromModifications(fsChange);
          }
        }

        os.optimize();

        writeStringInfo();

        /***************************
         * Prepare to walk main heap
         ***************************/
        heapEnd = csds.getHeapEnd();

        heapStart = isDelta ? origHeapEnd : 0;
        //
        //
        // if (isDelta) {
        // // edge case - delta serializing with no new fs
        // heapStart = (null == firstFS) ? heapEnd : csds.fs2addr.get(firstFS);
        // } else {
        // heapStart = 0; // not 1, in order to match v2 semantics
        // // is switched to 1 later
        // }

        // if (isDelta) {
        // // debug
        // for (TOP fs : csds.sortedFSs) {
        // System.out.format("debug heapAddr: %,d type: %s%n", csds.fs2addr.get(fs),
        // fs._getTypeImpl().getShortName());
        // if (csds.fs2addr.get(fs) == 439) {
        // System.out.println("debug");
        // }
        // }
        // System.out.format("debug End of debug scan, heapStart: %,d heapEnd: %,d%n%n", heapStart,
        // heapEnd);
        // }

        if (TRACE_SER)
          System.out.println("Form4Ser heapstart: " + heapStart + "  heapEnd: " + heapEnd);

        writeVnumber(control_dos, heapEnd - heapStart); // used for delta heap size to grow the CAS
                                                        // and ending condition on deser loop
        if (TRACE_SER)
          System.out.println("Form4Ser heapstart: " + heapStart + "  heapEnd: " + heapEnd);
        Arrays.fill(prevFsByType, null);

        // if (heapStart == 0) {
        // heapStart = 1; // slot 0 not serialized, it's null / 0
        // }

        // scan thru all fs and save their offsets in the heap
        // to allow conversion from addr to sequential fs numbers
        // initFsStartIndexes(fsStartIndexes, heap, heapStart, heapEnd, typeCodeHisto);

        /***************************
         * walk all fs's For delta, just those above the line
         ***************************/
        for (TOP fs : newSortedFSs) {
          writeFs(fs);
        }

        if (TRACE_SER)
          System.out.println("Form4Ser writing index info");
        serializeIndexedFeatureStructures(csds);

        if (isDelta) {
          if (TRACE_SER)
            System.out.println("Form4Ser writing modified FSs");
          (new SerializeModifiedFSs(csds)).serializeModifiedFSs();
        }

        collectAndZip();

        if (doMeasurement) {
          sm.totalTime = System.currentTimeMillis() - sm.totalTime;
        }
      }
    }

    /**
     * Write the compressed string table(s)
     * 
     * @throws IOException
     */
    private void writeStringInfo() throws IOException {
      String[] commonStrings = os.getCommonStrings();
      writeVnumber(strChars_i, commonStrings.length);
      DataOutputStream out = dosZipSources[strChars_i];
      for (int i = 0; i < commonStrings.length; i++) {
        int startPos = doMeasurements ? out.size() : 0;
        DataIO.writeUTFv(commonStrings[i], out);
        // approximate histogram
        if (doMeasurements) {
          // len is utf-8 encoding
          float len = out.size() - startPos;
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
        // long commonStringsLength = 0;
        // sm.stringsNbrCommon = commonStrings.length;
        // int r = 0;
        // for (int i = 0; i < commonStrings.length; i++) {
        // r += DataIO.lengthUTFv(commonStrings[i]);
        // commonStringsLength += commonStrings[i].length();
        // }
        // sm.stringsCommonChars = r;
        //
        // sm.stringsSavedExact = os.getSavedCharsExact() * 2;
        // sm.stringsSavedSubstr = os.getSavedCharsSubstr() * 2;
        // sm.statDetails[strChars_i].original = os.getSavedCharsExact() * 2
        // + os.getSavedCharsSubstr() * 2
        // + commonStringsLength * 2;
        // final int stringHeapStart = isSerializingDelta ? mark.nextFSId : 1;
        // final int stringHeapEnd = stringHeapObj.getSize();
        // sm.statDetails[strLength_i].original = (stringHeapEnd - stringHeapStart) * 4;
        // sm.statDetails[strOffset_i].original = (stringHeapEnd - stringHeapStart) * 4;
      }

    }

    private void writeFs(TOP fs) throws IOException {
      TypeImpl type = fs._getTypeImpl();
      int typeCode = type.getCode();
      writeVnumber(typeCode_dos, typeCode);

      prevFs = prevFsByType[typeCode];

      if (type.isArray()) {
        serializeArray(fs);
      } else {
        for (FeatureImpl feat : type.getFeatureImpls()) {
          serializeByKind(fs, feat);
        }
      }

      prevFsByType[typeCode] = fs;
      // if (doMeasurement) {
      // sm.statDetails[typeCode_i].incr(DataIO.lengthVnumber(tCode));
      // sm.mainHeapFSs ++;
      // }
    }

    private void serializeIndexedFeatureStructures(final CommonSerDesSequential csds)
            throws IOException {
      // fsIndexes already have the modelled address conversion
      int[] fsIndexes = isDelta ? bcsd.getDeltaIndexedFSs(mark, csds.fs2addr)
              : bcsd.getIndexedFSs(csds.fs2addr);
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
        writeVnumber(control_i, fsIndexes[fi]); // not converted to sequential

        if (doMeasurement) {
          sm.statDetails[fsIndexes_i].incr(DataIO.lengthVnumber(fsIndexes[fi]));
        }
      }

      for (int vi = 0; vi < nbrViews; vi++) {
        fi = compressFsxPart(fsIndexes, fi, csds); // added FSs
        if (isDelta) {
          fi = compressFsxPart(fsIndexes, fi, csds); // removed FSs
          fi = compressFsxPart(fsIndexes, fi, csds); // reindexed FSs
        }
      }
    }

    private int compressFsxPart(int[] fsIndexes, int fsNdxStart, final CommonSerDesSequential csds)
            throws IOException {
      int ix = fsNdxStart;
      final int nbrEntries = fsIndexes[ix++];
      final int end = ix + nbrEntries;
      writeVnumber(fsIndexes_dos, nbrEntries); // number of entries
      if (doMeasurement) {
        sm.statDetails[typeCode_i].incr(DataIO.lengthVnumber(nbrEntries));
      }

      final int[] ia = new int[nbrEntries];
      for (int i = ix, t = 0; i < end; i++, t++) {
        ia[t] = fs2seq(csds.addr2fs.get(fsIndexes[i])); // convert "addr" to "seq" offset
      }
      // System.arraycopy(fsIndexes, ix, ia, 0, nbrEntries);
      Arrays.sort(ia);

      int prev = 0;
      for (int i = 0; i < ia.length; i++) {
        int v = ia[i];
        writeVnumber(fsIndexes_dos, v - prev);
        if (doMeasurement) {
          sm.statDetails[fsIndexes_i].incr(DataIO.lengthVnumber(v - prev));
        }
        prev = v;
      }
      return end;
    }

    private void serializeArray(TOP fs) throws IOException {
      final int length = serializeArrayLength(fs);
      // special case 0 and 1st value
      if (length == 0) {
        return;
      }
      final TypeImpl type = fs._getTypeImpl();

      // output values

      SlotKind arrayElementKind = type.getComponentSlotKind();
      switch (arrayElementKind) {
        case Slot_HeapRef: {
          int prev = getPrevArray0HeapRef();
          for (TOP item : ((FSArray) fs)._getTheArray()) {
            int v = fs2seq(item);
            writeDiff(arrayElementKind.ordinal(), v, prev);
            prev = v;
          }
          break;
        }
        case Slot_Int: {
          int prev = getPrevArray0Int();
          for (int item : ((IntegerArray) fs)._getTheArray()) {
            writeDiff(arrayElementKind.ordinal(), item, prev);
            prev = item;
          }
          break;
        }

        case Slot_ShortRef: {
          int prev = 0;
          for (int item : ((ShortArray) fs)._getTheArray()) {
            writeDiff(short_i, item, prev);
            prev = item;
          }
          break;
        }
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
          for (boolean item : ((BooleanArray) fs)._getTheArray()) {
            byte_dos.write(item ? 1 : 0);
          }
          break;

        case Slot_ByteRef:
          byte_dos.write(((ByteArray) fs)._getTheArray());
          break;

        case Slot_LongRef: {
          long prev = 0;
          for (long item : ((LongArray) fs)._getTheArray()) {
            writeLong(item, prev);
            prev = item;
          }
          break;
        }

        case Slot_DoubleRef:
          for (double item : ((DoubleArray) fs)._getTheArray()) {
            writeDouble(CASImpl.double2long(item));
          }
          break;

        default:
          Misc.internalError();
      } // end of switch
    }

    private int getPrevArray0HeapRef() {
      if (isNoPrevArrayValue((CommonArrayFS) prevFs))
        return 0;
      return fs2seq((TOP) ((FSArray) prevFs).get(0));
    }

    private int getPrevArray0Int() {
      if (isNoPrevArrayValue((CommonArrayFS) prevFs))
        return 0;
      return ((IntegerArray) prevFs).get(0);
    }

    private boolean isNoPrevArrayValue(CommonArrayFS prevCommonArray) {
      return prevCommonArray == null || prevCommonArray.size() == 0;
    }

    private void serializeByKind(TOP fs, FeatureImpl feat) throws IOException {
      SlotKind kind = feat.getSlotKind();
      switch (kind) {
        case Slot_Int: {
          final int prev = (prevFs == null) ? 0 : prevFs._getIntValueNc(feat);
          final int v = fs._getIntValueNc(feat);
          // if (TRACE_INT) System.out.format("writeInt value: %,d prev: %,d%n", v, prev);
          writeDiff(kind.ordinal(), v, prev);
          break;
        }

        case Slot_Short:
          writeDiff(kind.ordinal(), fs._getShortValueNc(feat),
                  (prevFs == null) ? 0 : prevFs._getShortValueNc(feat));
          break;

        case Slot_HeapRef:
          final TOP ref = fs._getFeatureValueNc(feat);
          writeDiff(kind.ordinal(), fs2seq(ref),
                  (prevFs == null) ? 0 : fs2seq(prevFs._getFeatureValueNc(feat)));
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
          writeLong(fs._getLongValueNc(feat), (prevFs == null) ? 0L : prevFs._getLongValueNc(feat));
          break;

        case Slot_DoubleRef:
          writeDouble(CASImpl.double2long(fs._getDoubleValueNc(feat)));
          break;

        default:
          Misc.internalError();
      } // end of switch
    }

    private int serializeArrayLength(TOP fs) throws IOException {
      int length = ((CommonArrayFS) fs).size();
      writeVnumber(arrayLength_i, length);
      return length;
    }

    // private void serializeDiffWithPrevTypeSlot(SlotKind kind, TOP fs, FeatureImpl feat) throws
    // IOException {
    // int prev = (prevFs == null) ? 0 : fs.setIntLikeValue(slotKind, fi, v);getheap[iPrevHeap +
    // offset];
    // writeDiff(kind.ordinal(), heap[iHeap + offset], prev);
    // }

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
          DeflaterOutputStream cds = new DeflaterOutputStream(baosZipped, deflater, zipBufSize);
          baos.writeTo(cds);
          cds.close();
          idxAndLen.add(i);
          if (doMeasurement) {
            idxAndLen.add((int) (sm.statDetails[i].afterZip = deflater.getBytesWritten()));
            idxAndLen.add((int) (sm.statDetails[i].beforeZip = deflater.getBytesRead()));
            sm.statDetails[i].zipTime = System.currentTimeMillis() - startTime;
          } else {
            idxAndLen.add((int) deflater.getBytesWritten());
            idxAndLen.add((int) deflater.getBytesRead());
          }
        }
      }
      serializedOut.writeInt(nbrEntries); // write number of entries
      for (int i = 0; i < idxAndLen.size();) {
        serializedOut.write(idxAndLen.get(i++));
        serializedOut.writeInt(idxAndLen.get(i++));
        serializedOut.writeInt(idxAndLen.get(i++));
      }
      baosZipped.writeTo(serializedOut); // write Compressed info
    }

    // private DataOutputStream getZipStream(SlotKind kind) {
    // DataOutputStream dos = dosZipSources[kind.i];
    // if (null == dos) {
    // dos = setupOutputStream(kind);
    // }
    // return dos;
    // }

    private void writeLong(long v, long prev) throws IOException {
      writeDiff(long_High_i, (int) (v >>> 32), (int) (prev >>> 32));
      writeDiff(long_Low_i, (int) v, (int) prev);
    }

    // @formatter:off
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
    // @formatter:on
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
        throw new RuntimeException(
                "Cannot serialize string of Integer.MAX_VALUE length - too large.");
      }

      final int offset = os.getOffset(indexOrSeq);
      final int length = encodeIntSign(s.length() + 1); // all lengths sign encoded because of above
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

    // @formatter:off
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
    // @formatter:on
    private void writeFloat(int raw) throws IOException {
      if (raw == 0) {
        writeUnsignedByte(float_Exponent_dos, 0);
        if (doMeasurement) {
          sm.statDetails[float_Exponent_i].incr(1);
        }
        return;
      }

      final int exponent = ((raw >>> 23) & 0xff) + 1; // because we reserve 0, see above
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
      if (TRACE_DOUBLE) {
        System.out.format("write Double: raw = %,d, exponent = %,d, mantissa + lowbit sign: %,d%n",
                raw, exponent, mants);
      }
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

    // @formatter:off
    /**
     * Encoding:
     *    bit 6 = sign:   1 = negative
     *    bit 7 = delta:  1 = delta
     * @param kind the kind of slot
     * @param i  runs from iHeap + 3 to end of array
     * @throws IOException passthru
     */
    // @formatter:on
    private void writeDiff(int kind, int v, int prev) throws IOException {
      if (v == 0) {
        writeVnumber(kind, 0); // a speedup, not a new encoding
        if (doMeasurement) {
          sm.statDetails[kind].diffEncoded++;
          sm.statDetails[kind].valueLeDiff++;
        }
        return;
      }

      if (v == Integer.MIN_VALUE) { // special handling, because abs fails
        writeVnumber(kind, 2); // written as -0
        if (doMeasurement) {
          sm.statDetails[kind].diffEncoded++;
          sm.statDetails[kind].valueLeDiff++;
        }
        return;
      }

      final int absV = Math.abs(v);
      if (((v > 0) && (prev > 0)) || ((v < 0) && (prev < 0))) {
        final int diff = v - prev; // guaranteed not to overflow
        // Math.abs of Integer.MIN_VALUE + 1 sometimes (after jit?) (on some JVMs) gives wrong
        // annswer
        // failure observed on IBM Java 7 SR1 and SR2 3/28/2013 schor
        // failure only observed when running entire suite of uimaj-core tests via eclipse - mvn
        // test doesn't fail
        // final int absDiff = Math.abs(diff);
        // this seems to work around
        final int absDiff = (diff < 0) ? -diff : diff;
        writeVnumber(kind, (absV <= absDiff) ? ((long) absV << 2) + ((v < 0) ? 2L : 0L)
                : ((long) absDiff << 2) + ((diff < 0) ? 3L : 1L));
        if (doMeasurement) {
          sm.statDetails[kind].diffEncoded++;
          sm.statDetails[kind].valueLeDiff += (absV <= absDiff) ? 1 : 0;
        }
        return;
      }
      // if get here, then the abs v value is always <= the abs diff value.
      writeVnumber(kind, ((long) absV << 2) + ((v < 0) ? 2 : 0));
      if (doMeasurement) {
        sm.statDetails[kind].diffEncoded++;
        sm.statDetails[kind].valueLeDiff++;
      }
    }

    /**
     * add strings to the optimizestrings object
     * 
     * If delta, only process for fs's that are new; modified string values picked up when scanning
     * FsChange items
     * 
     * @param fs
     *          feature structure
     */
    private void extractStrings(TOP fs) {
      if (isDelta && !mark.isNew(fs)) {
        return;
      }
      TypeImpl type = fs._getTypeImpl();

      if (type.isArray()) {
        if (type.getComponentSlotKind() == SlotKind.Slot_StrRef) {
          for (String s : ((StringArray) fs)._getTheArray()) {
            os.add(s);
          }
        }
      } else { // end of is-array
        for (FeatureImpl feat : type.getFeatureImpls()) {
          if (feat.getSlotKind() == SlotKind.Slot_StrRef) {
            os.add(fs._getStringValueNc(feat));
          }
        } // end of iter over all features
      } // end of if-is-not-array
    }

    /**
     * For delta, for each fsChange element, extract any strings
     * 
     * @param fsChange
     */
    private void extractStringsFromModifications(FsChange fsChange) {
      final TOP fs = fsChange.fs;
      final TypeImpl type = fs._getTypeImpl();
      if (fsChange.arrayUpdates != null) {
        if (type.getComponentSlotKind() == SlotKind.Slot_StrRef) {
          String[] sa = ((StringArray) fs)._getTheArray();
          fsChange.arrayUpdates.forAllInts(index -> {
            os.add(sa[index]);
          });
        } // end of is string array
      } else { // end of is array
        BitSet fm = fsChange.featuresModified;
        for (int offset = fm.nextSetBit(0); offset >= 0; offset = fm.nextSetBit(offset + 1)) {
          FeatureImpl feat = type.getFeatureImpls()[offset];
          if (feat.getSlotKind() == SlotKind.Slot_StrRef) {
            os.add(fs._getStringValueNc(feat));
          }
        } // end of iter over features
      } // end of is-not-array
    }

    // *****************************************************************************
    // Modified Values
    // Output:
    // For each FS that has 1 or more modified values,
    // write the heap addr of the FS
    //
    // For all modified values within the FS:
    // if it is an aux array element, write the index in the individual array instance and the new
    // value
    // otherwise, write the slot offset and the new value
    // *****************************************************************************
    public class SerializeModifiedFSs {

      // previous value - for things diff encoded
      int vPrevModInt = 0;
      int vPrevModHeapRef = 0;
      short vPrevModShort = 0;
      long vPrevModLong = 0;

      final CommonSerDesSequential csds;

      public SerializeModifiedFSs(CommonSerDesSequential csds) {
        this.csds = csds;
      }

      private void serializeModifiedFSs() throws IOException {

        int iPrevAddr = 0;

        FsChange[] fsChanges = baseCas.getModifiedFSList();
        // write out number of modified Feature Structures
        writeVnumber(control_dos, fsChanges.length);
        // iterate over all modified feature structures
        // @formatter:off
        /*
         * Theorems about these data
         *   1) Assumption: if an AuxHeap array is modified, its heap FS is in the list of modFSs
         *   2) FSs with AuxHeap values have increasing ref values into the Aux heap as FS addr increases
         *      (because the ref is not updateable).
         *   3) Assumption: String array element modifications are main heap slot changes
         *      and recorded as such
         */
        // @formatter:on

        for (FsChange fsChange : fsChanges) {

          TOP fs = fsChange.fs;
          TypeImpl ti = fs._getTypeImpl();
          final int addr = csds.fs2addr.get(fs);
          if (addr == 0) { // https://issues.apache.org/jira/browse/UIMA-5194
            // need to write a dummy entry because we already outputted the number of changes
            writeVnumber(fsIndexes_dos, 0);
            // don't update iPrevAddr
            // NOTE: modify corresponding deserialization code to detect this convention
            continue;
          }
          // write out the address of the modified FS
          writeVnumber(fsIndexes_dos, addr - iPrevAddr);
          // delay updating iPrevAddr until end of "for" loop

          /**************************************************
           * handle aux byte, short, long array modifications
           **************************************************/
          if (ti.isArray() && !ti.isHeapStoredArray()) {
            writeAuxHeapMods(fsChange);
          } else {
            writeMainHeapMods(fsChange);
          } // end of processing 1 modified FS
          iPrevAddr = addr;
        } // end of for loop over all modified FSs
      } // end of method

      private void writeMainHeapMods(FsChange fsChange) throws IOException {
        int nbrOfMods = (fsChange.arrayUpdates == null) ? fsChange.featuresModified.cardinality()
                : fsChange.arrayUpdates.size();
        writeVnumber(fsIndexes_dos, nbrOfMods);

        final TOP fs = fsChange.fs;

        if (fsChange.arrayUpdates == null) {
          FeatureImpl[] features = fs._getTypeImpl().getFeatureImpls();
          int iPrevOffsetInFs = 0;
          final BitSet bs = fsChange.featuresModified;
          for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            FeatureImpl feat = features[i];
            // next +1 to conform to v2 encoding of feat offsets
            writeVnumber(fsIndexes_dos, i + 1 - iPrevOffsetInFs);
            iPrevOffsetInFs = i + 1;

            final SlotKind kind = feat.getSlotKind();
            final int kindi = kind.ordinal();

            switch (kind) {
              case Slot_Boolean:
                byte_dos.write(fs._getBooleanValueNc(feat) ? 1 : 0);
                break;

              case Slot_Byte:
                byte_dos.write(fs._getByteValueNc(feat));
                break;

              case Slot_Short: {
                final short v = fs._getShortValueNc(feat);
                writeDiff(kindi, v, vPrevModShort);
                vPrevModShort = v;
                break;
              }

              case Slot_Int: {
                final int v = fs._getIntValueNc(feat);
                writeDiff(kindi, v, vPrevModInt);
                vPrevModInt = v;
                break;
              }

              case Slot_Float:
                writeFloat(CASImpl.float2int(fs._getFloatValueNc(feat)));
                break;

              case Slot_LongRef: {
                long v = fs._getLongValueNc(feat);
                writeLong(v, vPrevModLong);
                vPrevModLong = v;
                break;
              }

              case Slot_DoubleRef:
                writeDouble(CASImpl.double2long(fs._getDoubleValueNc(feat)));
                break;

              case Slot_HeapRef: {
                int v = fs2seq(fs._getFeatureValueNc(feat)); // v2 writes it this way
                writeDiff(kindi, v, vPrevModHeapRef);
                vPrevModHeapRef = v;
                break;
              }

              case Slot_StrRef:
                writeString(fs._getStringValueNc(feat));
                break;

              default:
                Misc.internalError();

            } // end of switch
          } // end of looping for all modified slots in this FS
        } else { // end of processing of features
          // heap stored arrays
          TypeImpl type = fs._getTypeImpl();
          SlotKind kind = type.getComponentSlotKind();
          int kindi = kind.ordinal();

          IntListIterator it = fsChange.arrayUpdates.iterator();
          while (it.hasNext()) {
            int i = it.nextNvc();
            // write the offset of the of the modified entry
            // from the beginning of the fs addr
            // i is already the 0 based offset, make it a 2 based one
            // to account for the type code and length in v2 layout
            writeVnumber(fsIndexes_dos, i + 2);

            switch (kind) {
              case Slot_Int: {
                final int v = ((IntegerArray) fs).get(i);
                writeDiff(kindi, v, vPrevModInt);
                vPrevModInt = v;
                break;
              }

              case Slot_Float:
                writeFloat(CASImpl.float2int(((FloatArray) fs).get(i)));
                break;

              case Slot_StrRef:
                writeString(((StringArray) fs).get(i));
                break;

              case Slot_HeapRef:
                int v = fs2seq((TOP) ((FSArray) fs).get(i));
                writeDiff(kindi, v, vPrevModHeapRef);
                vPrevModHeapRef = v;
                break;

              default:
                Misc.internalError();
            } // end of switch
          } // end of iteration over all changed slots in one array
        } // end of if statement for processing arrays
      } // end of method

      private void writeAuxHeapMods(FsChange fsChange) throws IOException {
        final TOP fs = fsChange.fs;
        final TypeImpl type = fs._getTypeImpl();

        int iPrevOffset = 0;

        final SlotKind kind = type.getComponentSlotKind();

        writeVnumber(fsIndexes_dos, fsChange.arrayUpdates.size());

        IntListIterator it = fsChange.arrayUpdates.iterator();
        while (it.hasNext()) {
          int i = it.nextNvc();

          writeVnumber(fsIndexes_dos, i - iPrevOffset);
          iPrevOffset = i;

          switch (kind) {
            case Slot_BooleanRef:
              byte_dos.write(((BooleanArray) fs).get(i) ? 1 : 0);
              break;
            case Slot_ByteRef:
              byte_dos.write(((ByteArray) fs).get(i));
              break;
            case Slot_ShortRef: {
              short v = ((ShortArray) fs).get(i);
              writeDiff(int_i, v, vPrevModShort);
              vPrevModShort = v;
              break;
            }

            case Slot_LongRef: {
              long v = ((LongArray) fs).get(i);
              writeLong(v, vPrevModLong);
              vPrevModLong = v;
              break;
            }

            case Slot_DoubleRef: {
              double v = ((DoubleArray) fs).get(i);
              writeDouble(CASImpl.double2long(v));
              break;
            }

            default:
              Misc.internalError();
          } // end of switch
        } // end of iteration over items changed in the array
      } // end of method
    } // end of class definition for SerializeModifiedFSs

    private int fs2seq(TOP fs) {
      return (fs == null) ? 0 : fs2seq.get(fs);
    }

    // private TOP seq2fs(int s) {
    // return (s == 0) ? null : seq2fs.get(s);
    // }

    // private int fs2addr(TOP fs) {
    // return (fs == null) ? 0 : csds.fs2addr.get(fs);
    // }

  } // end of class definition for Serializer

  /**
   * Class instantiated once per deserialization Multiple deserializations in parallel supported,
   * with multiple instances of this
   */
  private class Deserializer {

    final private CASImpl baseCas; // cas being deserialized into
    final private CASImpl ivCas; // initial view cas - where by default new fs are created
    final private BinaryCasSerDes bcsd;
    final private CommonSerDesSequential csds;
    final private DataInput deserIn;

    final private DataInputStream[] dataInputs = new DataInputStream[NBR_SLOT_KIND_ZIP_STREAMS];
    private Inflater[] inflaters = new Inflater[NBR_SLOT_KIND_ZIP_STREAMS];

    /** the FS being deserialized */
    private TOP currentFs;

    /**
     * Deferred actions to set Feature Slots of feature structures. the deferrals needed when
     * deserializing a subtype of AnnotationBase before the sofa is known Also for Sofa creation
     * where some fields are final
     */
    final private List<Runnable> singleFsDefer = new ArrayList<>();

    /** used for deferred creation */
    private int sofaNum;
    private String sofaName;
    private Sofa sofaRef;

    // private int[] heap; // main heap
    private int heapStart;
    private int heapEnd;

    /**
     * the "fixups" for relative heap refs actions set slot values
     */
    final private List<Runnable> fixupsNeeded = new ArrayList<>();
    final private List<Runnable> uimaSerializableFixups = new ArrayList<>();

    final private StringHeap stringHeapObj = new StringHeap();
    // private LongHeap longHeapObj;
    // private ShortHeap shortHeapObj;
    // private ByteHeap byteHeapObj;
    //
    // private int stringTableOffset;

    final private boolean isDelta; // if true, a delta is being deserialized
    private String[] readCommonString;

    // private TypeInfo typeInfo; // type info for the current type being serialized

    // private int iPrevHeap; // 0 or heap addr of previous instance of current type

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

    // @formatter:off
    /**
     * For differencing when reading. Also used for arrays to difference the 0th element.
     * 
     * Can't use real fs for heap refs - may be forward refs not yet fixedup
     * 
     * Hold prev instance of FS which have FSRef slots
     * 
     *   for each target typecode, only set if the type 
     *     - has 1 or more non-array fsref
     *     - is a (subtype of) FSArray
     *   set for both 0 and non-0 values !! Different from form6
     * first index: key is type code
     * 2nd index: key is slot-offset number (0-based)
     * 
     * Also used for array refs, for the 1st entry in the array - feature slot 0 is used for this
     * when reading (not when writing - could be made more uniform)
     */
    // @formatter:on
    final private int[][] prevFsRefsByType = new int[ts.getTypeArraySize()][];
    private int[] prevFsRefs;

    /**
     * Used for differencing, except for HeapRef values which use above
     */
    final private TOP[] prevFsByType = new TOP[ts.getTypeArraySize()];
    private TOP prevFs;

    /**
     * convert between FSs and "sequential" numbers Note: This may be identity map, but may not in
     * the case for V3 where some FSs are GC'd
     */
    // private final Obj2IntIdentityHashMap<TOP> fs2seq = new Obj2IntIdentityHashMap<TOP>(TOP.class,
    // TOP.singleton);
    private final Int2ObjHashMap<TOP, TOP> seq2fs = new Int2ObjHashMap<>(TOP.class);

    /**
     * Called after header was read and determined that this was a compressed binary
     * 
     * @param cas
     *          CAS
     * @param deserIn
     *          input data
     * @throws IOException
     *           passthru
     */
    Deserializer(CASImpl cas, DataInput deserIn, boolean isDelta) throws IOException {
      this.baseCas = cas.getBaseCAS();
      this.ivCas = baseCas.getInitialView();
      this.bcsd = cas.getBinaryCasSerDes();
      this.csds = getCsds(baseCas, isDelta);
      this.deserIn = deserIn;
      this.isDelta = isDelta;

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

    private void deserialize(CommonSerDes.Header h) throws IOException {
      if (TRACE_DES)
        System.out.println("Form4Deser starting");

      // fs2seq.clear();
      seq2fs.clear();
      /************************************************
       * Setup all the input streams with inflaters
       ************************************************/
      // long startTime1 = System.currentTimeMillis();
      stringHeapObj.reset();

      /************************************************
       * Read in the common string(s)
       ************************************************/
      int lenCmnStrs = readVnumber(strChars_dis);
      readCommonString = new String[lenCmnStrs];
      for (int i = 0; i < lenCmnStrs; i++) {
        readCommonString[i] = DataIO.readUTFv(strChars_dis);
      }
      only1CommonString = lenCmnStrs == 1;
      // **************************
      // Prepare to walk main heap
      // The csds must be either empty (for receiving non- delta)
      // or the same as when the CAS was previous sent out (for receiving delta)
      // **************************

      int seq = 1;
      for (TOP fs : csds.getSortedFSs()) { // only non-empty if delta; and then it's from prev
                                           // serialization
        // fs2seq.put(fs, seq);
        seq2fs.put(seq++, fs);
      }

      int deltaHeapSize = readVnumber(control_dis);

      heapStart = isDelta ? csds.getHeapEnd() : 0;

      // stringTableOffset = isDelta ? (stringHeapObj.getSize() - 1) : 0;

      // if (isDelta) {
      // heapObj.grow(deltaHeapSize);
      // } else {
      // heapObj.reinitSizeOnly(deltaHeapSize);
      // }

      heapEnd = heapStart + deltaHeapSize;
      // heap = heapObj.heap;

      for (int[] ia : prevFsRefsByType) {
        if (ia != null)
          Arrays.fill(ia, 0);
      }

      if (heapStart == 0) {
        heapStart = 1; // slot 0 not serialized, it's null / 0
      }

      // if (CHANGE_FS_REFS_TO_SEQUENTIAL && (heapStart > 1)) {
      // initFsStartIndexes(fsStartIndexes, heap, 1, heapStart, null);
      // }
      // fixupsNeeded = new IntVector(Math.max(16, heap.length / 10));

      // @formatter:off
      /*******************************
       * walk main heap - deserialize
       *   FS Creation:
       *     - creatCurrentFs -> createFs
       *     - createSofa
       *     - createArray
       *******************************/
      // @formatter:on
      TypeImpl type;
      int arraySize = 0;
      Arrays.fill(prevFsByType, null);

      if (TRACE_DES)
        System.out.println("Form4Deser heapStart: " + heapStart + "  heapEnd: " + heapEnd);
      for (int iHeap = heapStart; iHeap < heapEnd; iHeap += type.getFsSpaceReq(arraySize)) {
        final int typeCode = readVnumber(typeCode_dis);
        // final int adjTypeCode = typeCode + ((this.bcsd.isBeforeV3 && typeCode >
        // TypeSystemConstants.lastBuiltinV2TypeCode)
        // ? TypeSystemConstants.numberOfNewBuiltInsSinceV2
        // : 0);
        type = ts.getTypeForCode(typeCode);

        prevFs = prevFsByType[typeCode]; // could be null;
        prevFsRefs = getPrevFsRef(type); // null or int[], only for things having fsrefs (array or
                                         // not)

        if (type.isArray()) {
          currentFs = readArray(iHeap, type);
          arraySize = ((CommonArrayFS) currentFs).size();
        } else {
          if (!ts.annotBaseType.subsumes(type) && // defer subtypes of AnnotationBase
                  !(ts.sofaType == type)) { // defer sofa types
            createCurrentFs(type, ivCas);
          } else {
            currentFs = null;
            singleFsDefer.clear();
            sofaRef = null;
            sofaNum = -1;
            sofaName = null;
          }
          for (FeatureImpl feat : type.getFeatureImpls()) {
            readByKind(feat, type);
          }
          // for (int i = 1; i < typeInfo.slotKinds.length + 1; i++) {
          // readByKind(iHeap, i);
          // }
        }

        if (currentFs == null) {

          // @formatter:off
          /*
           * Create single deferred FS 
           * Either: Sofa (has final fields) or 
           *         Subtype of AnnotationBase - needs to be in the right view
           * 
           * For the latter, handle document annotation specially
           */
          // @formatter:on
          if (ts.sofaType == type) {
            if (baseCas.hasView(sofaName)) {
              // sofa was already created, by an annotationBase subtype deserialized prior to this
              // one
              currentFs = (TOP) baseCas.getView(sofaName).getSofa();
            } else {
              currentFs = baseCas.createSofa(sofaNum, sofaName, null);
            }
          } else {

            CASImpl view = (null == sofaRef) ? baseCas.getInitialView() // https://issues.apache.org/jira/browse/UIMA-5588
                    : baseCas.getView(sofaRef);

            // if (type.getCode() == TypeSystemConstants.docTypeCode) {
            // currentFs = view.getDocumentAnnotation(); // creates the document annotation if it
            // doesn't exist
            // // we could remove this from the indexes until deserialization is over, but then,
            // other calls to getDocumentAnnotation
            // // would end up creating additional instances
            // } else {
            createCurrentFs(type, view);
            // }
          }
          if (type.getCode() == TypeSystemConstants.docTypeCode) {
            boolean wasRemoved = baseCas.checkForInvalidFeatureSetting(currentFs,
                    baseCas.getAddbackSingle());
            for (Runnable r : singleFsDefer) {
              r.run();
            }
            baseCas.addbackSingleIfWasRemoved(wasRemoved, currentFs);
          } else {
            for (Runnable r : singleFsDefer) {
              r.run();
            }
          }
        }

        assert (currentFs != null);
        // System.out.format("Adding %,d to csds%n", iHeap);
        // if (isDelta) {
        // System.out.format("debug adding iHeap: %,d afterAdd: %,d%n", iHeap, iHeap +
        // nextHeapAddrAfterMark);
        // }
        csds.addFS(currentFs, iHeap);
        int s2 = 1 + seq2fs.size();
        // fs2seq.put(currentFs, s2); // 1 origin to match v2
        seq2fs.put(s2, currentFs);

        prevFsByType[typeCode] = currentFs;
      }
      csds.setHeapEnd(heapEnd);

      // if (TRACE_DES) System.out.println("Form4Deser running deferred fixups after all FSs
      // deserialized");
      for (Runnable r : fixupsNeeded) {
        r.run();
      }

      for (Runnable r : uimaSerializableFixups) {
        r.run();
      }

      if (TRACE_DES)
        System.out.println("Form4Deser indexing FSs");
      readIndexedFeatureStructures();

      if (isDelta) {
        if (TRACE_DES)
          System.out.println("Form4Deser modifying existing FSs");
        (new ReadModifiedFSs()).readModifiedFSs();
      }

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

    private TOP readArray(int iHeap, TypeImpl type) throws IOException {
      final int length = readArrayLength();
      TOP fs = ivCas.createArray(type, length); // create in default view - initial view (iv)cas
      if (length == 0) {
        return fs;
      }

      SlotKind refKind = type.getComponentSlotKind();
      switch (refKind) {

        case Slot_BooleanRef: {
          boolean[] ba = ((BooleanArray) fs)._getTheArray();
          for (int i = 0; i < length; i++) {
            ba[i] = byte_dis.readByte() == 1;
          }
          break;
        }

        case Slot_ByteRef:
          readIntoByteArray(((ByteArray) fs)._getTheArray());
          break;

        case Slot_ShortRef:
          readIntoShortArray(((ShortArray) fs)._getTheArray());
          break;

        case Slot_Int: {
          final int[] ia = ((IntegerArray) fs)._getTheArray();
          int prev = getPrevIntValue(refKind, null);
          for (int i = 0; i < length; i++) {
            int v = readDiff(Slot_Int, prev);
            prev = v;
            if (i == 0) {
              savePrevHeapRef(type.getCode(), 1, 0, v);
            }
            ia[i] = v;
          }
          break;
        }

        case Slot_LongRef:
          readIntoLongArray(((LongArray) fs)._getTheArray());
          break;

        case Slot_Float: {
          final float[] fa = ((FloatArray) fs)._getTheArray();
          for (int i = 0; i < length; i++) {
            final int floatRef = readFloat();
            fa[i] = Float.intBitsToFloat(floatRef);
          }
          break;
        }

        case Slot_DoubleRef:
          readIntoDoubleArray(((DoubleArray) fs)._getTheArray());
          ;
          break;

        case Slot_HeapRef: {
          final TOP[] a = ((FSArray) fs)._getTheArray();
          int prev = getPrevIntValue(refKind, null);
          for (int i = 0; i < a.length; i++) {
            final int v = readDiff(SlotKind.Slot_HeapRef, prev);
            prev = v;
            if (i == 0) {
              savePrevHeapRef(type.getCode(), 1, 0, v);
            }
            final int local_i = i; // needed for lambda closure
            maybeStoreOrDefer_slotFixups(v, refd_fs -> a[local_i] = refd_fs);
          }
          break;
        }

        case Slot_StrRef: {
          String[] sa = ((StringArray) fs)._getTheArray();
          for (int i = 0; i < length; i++) {
            sa[i] = readString();
          }
        }
          break;

        default:
          Misc.internalError();
      }
      return fs;
    }

    private int readArrayLength() throws IOException {
      return readVnumber(arrayLength_dis);
    }

    /**
     * If the fs is null, accumulate fixup operations, otherwise directly set this
     * 
     * @param fs
     *          - null or the fs whose slots are to be set
     * @param feat
     * @param type
     * @throws IOException
     */
    private void readByKind(FeatureImpl feat, TypeImpl type) throws IOException {
      SlotKind kind = feat.getSlotKind();

      switch (kind) {
        case Slot_Int: {
          final int i = readDiffWithPrevTypeSlot(kind, feat);
          if (feat == ts.sofaNum) {
            sofaNum = i;
          } else {
            maybeStoreOrDefer((lfs) -> lfs._setIntValueNcNj(feat, i));
          }
          break;
        }

        case Slot_Short: {
          final int i = readDiffWithPrevTypeSlot(kind, feat);
          maybeStoreOrDefer(lfs -> lfs._setIntLikeValueNcNj(kind, feat, i));
          break;
        }

        case Slot_Float: {
          final int i = readFloat();
          maybeStoreOrDefer(lfs -> lfs._setFloatValueNcNj(feat, CASImpl.int2float(i)));
          break;
        }

        case Slot_Boolean: {
          final byte i = byte_dis.readByte();
          maybeStoreOrDefer(lfs -> lfs._setBooleanValueNcNj(feat, i == 1));
          break;
        }

        case Slot_Byte: {
          final byte i = byte_dis.readByte();
          maybeStoreOrDefer(lfs -> lfs._setByteValueNcNj(feat, i));
          break;
        }

        case Slot_HeapRef:
          final int vh = readDiffWithPrevTypeSlot(kind, feat);
          if (ts.annotBaseSofaFeat == feat) {
            sofaRef = (Sofa) seq2fs(vh); // if sofa hasn't yet been deserialized, will be null
            // use case: create annot , without sofa - causes create sofa
            // but binary serialization keeps creation order
          }
          if (ts.annotBaseSofaFeat != feat || sofaRef == null) {
            https: // issues.apache.org/jira/browse/UIMA-5588
            maybeStoreOrDefer(lfs -> {
              // in addition to deferring if currentFs is null,
              // heap refs may need deferring if forward refs
              // Also, special case the setting of sofaArray data; set FeatureValue doesn't work.

              if (feat == ts.sofaArray) {
                maybeStoreOrDefer_slotFixups(vh, ref_fs -> ((Sofa) lfs).setLocalSofaData(ref_fs));
              } else {
                maybeStoreOrDefer_slotFixups(vh, ref_fs -> lfs._setFeatureValueNcNj(feat, ref_fs));
              }
            });
          }
          break;

        case Slot_StrRef: {
          String s = readString();
          if (null == s)
            break; // null is default, no need to store it
          if (ts.sofaType.subsumes(type)) {
            if (feat == ts.sofaId) {
              sofaName = s;
              break;
            }
            if (feat == ts.sofaMime) {
              maybeStoreOrDefer(lfs -> ((Sofa) lfs).setMimeType(s));
              break;
            }
            if (feat == ts.sofaUri) {
              maybeStoreOrDefer(lfs -> ((Sofa) lfs).setRemoteSofaURI(s));
              break;
            }
            if (feat == ts.sofaString) {
              maybeStoreOrDefer(lfs -> ((Sofa) lfs).setLocalSofaDataNoDocAnnotUpdate(s));
              break;
            }
          }
          // other user-defined custom sofa extended string features (if any)
          // as well as non-sofa FS features, are set by the following code
          maybeStoreOrDefer(lfs -> lfs._setStringValueNcNj(feat, s));
          break;
        }

        case Slot_LongRef: {
          final long prevLong = (prevFs == null) ? 0L : prevFs._getLongValueNc(feat);
          long v = readLongOrDouble(kind, prevLong);
          maybeStoreOrDefer(lfs -> lfs._setLongValueNcNj(feat, v));
          break;
        }

        case Slot_DoubleRef: {
          long v = readDouble();
          maybeStoreOrDefer(lfs -> lfs._setDoubleValueNcNj(feat, CASImpl.long2double(v)));
          break;
        }

        default:
          Misc.internalError();
      } // end of switch
    }

    private void readIndexedFeatureStructures() throws IOException {
      final int nbrViews = readVnumber(control_dis);
      final int nbrSofas = readVnumber(control_dis);

      // fsIndexes is collection of FSs represented by sequentially incrementing numbers
      IntVector fsIndexes = new IntVector(nbrViews + nbrSofas + 100);
      fsIndexes.add(nbrViews);
      fsIndexes.add(nbrSofas);
      for (int i = 0; i < nbrSofas; i++) {
        fsIndexes.add(readVnumber(control_dis)); // this is the v2 addr style
      }

      for (int i = 0; i < nbrViews; i++) {
        readFsxPart(fsIndexes); // added FSs
        if (isDelta) {
          readFsxPart(fsIndexes); // removed FSs
          readFsxPart(fsIndexes); // reindexed FSs
        }
      }

      bcsd.reinitIndexedFSs(fsIndexes.getArray(), isDelta, i -> seq2fs.get(i), // written on
                                                                               // separate line for
                                                                               // Eclipse breakpoint
                                                                               // control
              i -> csds.addr2fs.get(i) // https://issues.apache.org/jira/browse/UIMA-5593
      );
    }

    /**
     * Maybe defers setting features for a Feature Structure if the FS isn't created yet (perhaps
     * because it needs a sofa ref, not yet read)
     * 
     * @param fs
     *          - the Feature Structure or null if not yet created
     * @param storeAction
     */
    private void maybeStoreOrDefer(Consumer<TOP> storeAction) {
      if (null == currentFs) {
        singleFsDefer.add(() -> storeAction.accept(currentFs));
      } else {
        storeAction.accept(currentFs);
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
      TOP src = seq2fs(tgtSeq);
      if (src == null) {
        // need to do the getRefVal later when it's known
        // here are the two values of "r"
        // () -> sofa.setLocalSofaData(getRefVal(vh))
        // () -> lfs.setFeatureValue(srcFeat, getRefVal(vh))
        fixupsNeeded.add(() -> r.accept(seq2fs(tgtSeq)));
      } else {
        // sofa.setLocalSofaData(tgt);
        // lfs.setFeatureValue(srcFeat, src)
        r.accept(src);
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
        // v = csds.fs2addr.get(seq2fs(v)); // v is the seq form of a ref (incr by 1)
        // v is a sequentially incrementing ref to a FS
        fsIndexes.add(v);
      }
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
      int zipBufSize = Math.max(1024, bytesCompr);
      InflaterInputStream iis = new InflaterInputStream(baiStream, inflater, zipBufSize);
      dataInputs[slotIndex] = new DataInputStream(new BufferedInputStream(iis, zipBufSize));
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

    private DataInput getInputStream(SlotKind kind) {
      return dataInputs[kind.ordinal()];
    }

    private int readVnumber(DataInputStream dis) throws IOException {
      return DataIO.readVnumber(dis);
    }

    private long readVlong(DataInputStream dis) throws IOException {
      return DataIO.readVlong(dis);
    }

    private void readIntoByteArray(byte[] ba) throws IOException {
      byte_dis.readFully(ba);
    }

    private void readIntoShortArray(short[] sa) throws IOException {
      short prev = 0;
      for (int i = 0; i < sa.length; i++) {
        sa[i] = prev = (short) (readDiff(short_dis, prev));
      }
    }

    private void readIntoDoubleArray(double[] da) throws IOException {
      for (int i = 0; i < da.length; i++) {
        da[i] = CASImpl.long2double(readDouble());
      }
    }

    private void readIntoLongArray(long[] la) throws IOException {
      long prev = 0;
      for (int i = 0; i < la.length; i++) {
        la[i] = prev = readLongOrDouble(SlotKind.Slot_LongRef, prev);
      }
    }

    // @formatter:off
    /**
     * Difference with previously deserialized value of corresponding slot of
     * previous FS for this type.
     *   Special handling: if the slot is a heap ref, we can't use the prevFs
     *   because the value may be a forward reference, not yet deserialized, and
     *   therefore unknown.
     *     For this case, we preserve the actual deserialized value in a lazyly 
     *     constructed prevFsRef and use that.
     *     For arrays, only the prev 0 value is used (if available - otherwise 0 is used)
     * @param kind - the slot kind being deserialized
     * @param feat - the feature (null for arrays)
     * @return - the previous value, for differencing
     * @throws IOException
     */
    // @formatter:on
    private int readDiffWithPrevTypeSlot(SlotKind kind, FeatureImpl feat) throws IOException {
      int prev = getPrevIntValue(kind, feat);
      int v = readDiff(kind, prev);
      // if (feat.getShortName().equals("akofAint")) System.out.format("debug prev: %,d v: %,d%n",
      // prev, v);
      // if (TRACE_INT && kind == SlotKind.Slot_Int) System.out.format("readInt value: %,d prev:
      // %,d%n", v, prev);

      if (kind == SlotKind.Slot_HeapRef) {
        TypeImpl type = (TypeImpl) feat.getDomain();
        savePrevHeapRef(type.getCode(), type.getNumberOfFeatures(), feat.getOffset(), v);
      }
      // for non heap refs, no need to save the value - the fs itself
      // saves it.
      return v;
    }

    /**
     * Common code for feature offset and array
     * 
     * @param kind
     * @param feat
     *          feature or null for array access
     * @return
     */
    private int getPrevIntValue(SlotKind kind, FeatureImpl feat) {
      if (kind == SlotKind.Slot_HeapRef) {
        return (prevFsRefs == null) ? 0 : prevFsRefs[(feat == null) ? 0 : feat.getOffset()];
      }
      return (prevFs == null) ? 0 : prevFs._getIntLikeValue(kind, feat);
    }

    private void savePrevHeapRef(int typecode, int nbrOfSlots, int offset, int v) {
      if (prevFsRefs == null) {
        prevFsRefsByType[typecode] = prevFsRefs = new int[nbrOfSlots];
      }
      prevFsRefs[offset] = v;
    }

    private int readDiff(SlotKind kind, int prev) throws IOException {
      return readDiff(getInputStream(kind), prev);
    }

    private int readDiff(DataInput in, int prev) throws IOException {
      final long encoded = readVlong(in);
      final boolean isDelta1 = (0 != (encoded & 1L));
      final boolean isNegative = (0 != (encoded & 2L));
      int v = (int) (encoded >>> 2);
      if (isNegative) {
        if (v == 0) {
          return Integer.MIN_VALUE;
        }
        v = -v;
      }
      if (isDelta1) {
        v = v + prev;
      }
      return v;
    }

    private long readLongOrDouble(SlotKind kind, long prev) throws IOException {
      if (kind == SlotKind.Slot_DoubleRef) {
        return readDouble();
      }

      final int vh = readDiff(long_High_dis, (int) (prev >>> 32));
      final int vl = readDiff(long_Low_dis, (int) prev);
      final long v = (((long) vh) << 32) | (0xffffffffL & vl);
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

      long raw = decodeDouble(mants, exponent);
      if (TRACE_DOUBLE) {
        System.out.format("read Double: raw = %,d, exponent = %,d, mantissa + lowbit sign: %,d%n",
                raw, exponent, mants);
      }
      return raw;
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

    private String readString() throws IOException {
      int length = decodeIntSign(readVnumber(strLength_dis));
      if (0 == length) {
        return null;
      }
      if (1 == length) {
        stringHeapObj.addString("");
        return ("");
      }

      if (length < 0) { // in this case, -length is the slot index
        return /* stringTableOffset */ stringHeapObj.getStringForCode(-length);
      }
      int offset = readVnumber(strOffset_dis);
      int segmentIndex = (only1CommonString) ? 0 : readVnumber(strSeg_dis);
      String s = readCommonString[segmentIndex].substring(offset, offset + length - 1);
      stringHeapObj.addString(s);
      return s;
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

      // next for managing index removes / readds
      private boolean wasRemoved;
      private FSsTobeAddedbackSingle addbackSingle;

      private void readModifiedFSs() throws IOException {
        final int modFSsLength = readVnumber(control_dis);
        int iPrevHeap = 0;

        for (int i = 0; i < modFSsLength; i++) {
          iHeap = readVnumber(fsIndexes_dis) + iPrevHeap;
          // convention for a skipped entry: written as 0
          boolean isSkippedEntry = iHeap == iPrevHeap;
          if (isSkippedEntry) {
            continue;
          } else {
            iPrevHeap = iHeap;
          }
          TOP fs = csds.addr2fs.get(iHeap);
          assert (fs != null);
          TypeImpl type = fs._getTypeImpl();

          final int numberOfModsInThisFs = readVnumber(fsIndexes_dis);

          /**************************************************
           * handle aux byte, short, long array modifications
           **************************************************/
          if (type.isArray() && (!type.isHeapStoredArray())) {
            readModifiedAuxHeap(numberOfModsInThisFs, fs, type);
          } else {
            // https://issues.apache.org/jira/browse/UIMA-4100
            // see if any of the mods are keys
            // baseCas.removeFromCorruptableIndexAnyView(iHeap, indexToDos);
            try {
              readModifiedMainHeap(numberOfModsInThisFs, fs, type);
            } finally {
              baseCas.addbackSingle(fs);
            }
          }
        }
      }

      private void readModifiedAuxHeap(int numberOfMods, TOP fs, TypeImpl type) throws IOException {
        int prevOffset = 0;

        final SlotKind kind = type.getComponentSlotKind(); // get kind of element

        for (int i2 = 0; i2 < numberOfMods; i2++) {
          final int offset = readVnumber(fsIndexes_dis) + prevOffset;
          prevOffset = offset;

          switch (kind) {

            case Slot_BooleanRef:
              ((BooleanArray) fs).set(offset, byte_dis.readByte() == 1);
              break;

            case Slot_ByteRef:
              ((ByteArray) fs).set(offset, byte_dis.readByte());
              break;

            case Slot_ShortRef: {
              final short v = (short) readDiff(int_dis, vPrevModShort);
              vPrevModShort = v;
              ((ShortArray) fs).set(offset, v);
              break;
            }

            case Slot_LongRef: {
              final long v = readLongOrDouble(kind, vPrevModLong);
              vPrevModLong = v;
              ((LongArray) fs).set(offset, v);
              break;
            }

            case Slot_DoubleRef:
              ((DoubleArray) fs).set(offset, CASImpl.long2double(readDouble()));
              break;

            default:
              Misc.internalError();
          } // end of switch
        } // end of for loop over all items in this array
      } // end of method

      private void readModifiedMainHeap(int numberOfMods, TOP fs, TypeImpl type)
              throws IOException {
        final boolean isArray = type.isArray();
        int iPrevOffsetInFs = 0;
        final FeatureImpl[] features = isArray ? null : type.getFeatureImpls();

        wasRemoved = false; // set to true when removed from index to stop further testing
        addbackSingle = baseCas.getAddbackSingle();

        for (int i = 0; i < numberOfMods; i++) {
          final int offsetInFs = readVnumber(fsIndexes_dis) + iPrevOffsetInFs; // this is encoded in
                                                                               // v2 style, -1 for
                                                                               // feat offset, -2
                                                                               // for array indexes
          iPrevOffsetInFs = offsetInFs;

          FeatureImpl feat = (features == null) ? null : features[offsetInFs - 1]; // -1 because v2
                                                                                   // records it
                                                                                   // this way

          final SlotKind kind = isArray ? type.getComponentSlotKind() : feat.getSlotKind();

          if (!isArray && kind != SlotKind.Slot_HeapRef && !wasRemoved) {
            wasRemoved = baseCas.checkForInvalidFeatureSetting(fs, feat.getCode(), addbackSingle);
          }

          switch (kind) {

            case Slot_Boolean:
              fs.setBooleanValue(feat, byte_dis.readByte() == 1);
              break;
            case Slot_Byte:
              fs.setByteValue(feat, byte_dis.readByte());
              break;

            case Slot_Short: {
              final short v = (short) readDiff(short_dis, vPrevModShort);
              vPrevModShort = v;
              fs.setShortValue(feat, v);
              break;
            }

            // can't be short array because that's on the aux heap

            case Slot_Int: {
              final int v = readDiff(int_dis, vPrevModInt);
              vPrevModInt = v;
              if (isArray) {
                ((IntegerArray) fs).set(offsetInFs - 2, v); // - 2 to conform to v2 numbering for
                                                            // arrays
              } else {
                fs.setIntValue(feat, v);
              }
            }
              break;

            case Slot_LongRef: {
              final long v = readLongOrDouble(kind, vPrevModLong);
              vPrevModLong = v;
              // long arrays were not on main heap
              fs.setLongValue(feat, v);
              break;
            }

            case Slot_Float: {
              float v = Float.intBitsToFloat(readFloat());
              if (isArray) {
                ((FloatArray) fs).set(offsetInFs - 2, v);
              } else {
                fs.setFloatValue(feat, v);
              }
            }
              break;

            case Slot_DoubleRef: {
              final long v = readDouble();
              // double arrays were not on main heap
              fs.setDoubleValue(feat, CASImpl.long2double(v));
              break;
            }

            case Slot_StrRef:
              String s = readString();
              if (isArray) {
                ((StringArray) fs).set(offsetInFs - 2, s);
              } else {
                fs.setStringValue(feat, s);
              }

              break;

            case Slot_HeapRef: {
              int v = readDiff(heapRef_dis, vPrevModHeapRef);
              vPrevModHeapRef = v;

              final TOP ref_fs = seq2fs(v); // v2 stores these this way
              // assert(ref_fs != null); // it could be a modification which set the slot to null
              if (isArray) {
                ((FSArray) fs).set(offsetInFs - 2, ref_fs);
              } else {
                fs.setFeatureValue(feat, ref_fs);
              }
            }
              break;

            default:
              Misc.internalError();
          } // end of switch
        } // end of for loop over all items for this FS
      } // end of ReadModifiedMainHeap

    } // end of ReadModifiedFs class

    /**
     * lazy initialization of the prevFsRef info FSArray - only need slot 0 non-array - need all the
     * slots
     */
    private int[] getPrevFsRef(TypeImpl type) {
      if (fsArrayType.subsumes(type)) {
        int[] cache = prevFsRefsByType[type.getCode()];
        if (null == cache) {
          prevFsRefsByType[type.getCode()] = cache = new int[] { 0 };
        }
        return cache;
      }

      if (type.isArray())
        return null; // all arrays except fsArray (see above) don't have fs refs

      int[] cache = prevFsRefsByType[type.getCode()];
      if (null == cache && type.hasRefFeature) { // skip allocating if no refs
        prevFsRefsByType[type.getCode()] = cache = new int[type.getNumberOfFeatures()];
      }
      return cache;
    }

    // private int fs2seq(TOP fs) {
    // return (fs == null) ? 0 : fs2seq.get(fs);
    // }

    private TOP seq2fs(int s) {
      return (s == 0) ? null : seq2fs.get(s);
    }

    // private TOP addr2fs(int s) {
    // return (s == 0) ? null : csds.addr2fs.get(s);
    // }
  }

  // ******************************************************************
  // methods common to serialization / deserialization etc.
  // ******************************************************************

  // private int incrToNextFs(int[] heap, int iHeap, TypeInfo typeInfo) {
  // if (typeInfo.isHeapStoredArray) {
  // return 2 + heap[iHeap + 1];
  // } else {
  // return 1 + typeInfo.slotKinds.length;
  // }
  // }

  // private void initFsStartIndexes (final ComprItemRefs fsStartIndexes, final int[] heap, int
  // heapStart, int heapEnd, int[] histo) {
  // for (int iHeap = 1; iHeap < heapEnd;) {
  // fsStartIndexes.addItemAddr(iHeap);
  // final int tCode = heap[iHeap];
  // if ((null != histo) && (iHeap >= heapStart)) {
  // histo[tCode] ++;
  // }
  // TypeInfo typeInfo = getTypeInfo(tCode);
  // iHeap += incrToNextFs(heap, iHeap, typeInfo);
  // }
  // fsStartIndexes.finishSetup();
  // }

  // public CasCompare getCasCompare() {
  // return new CasCompare();
  // }
  //
  // public class CasCompare {
  //
  // /**
  // * Trampolines to CasCompare
  // * There's no reliable way to get the set of FSs for 2 different form4 CASs, since the
  // * method used is to take the FSs from the id2fs weakReferences, and therefore some
  // * unreferenced items may appear in one and not the other.
  // * @param c1 a cas to compare
  // * @param c2 the cas to compare to
  // * @return true if they compare equal
  // */
  // public boolean compareCASes(CASImpl c1, CASImpl c2) {
  // return org.apache.uima.cas.impl.CasCompare.compareCASes(c1, c2);
  // }
  // }

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

  static CommonSerDesSequential getCsds(CASImpl cas, boolean isDelta) {
    CommonSerDesSequential tmpCsds = cas.getCsds();
    // 3 cases:
    // is delta, have good csds - use it without getting a new one
    // is delta, but existing csds is null or is empty - make a new one and set it up
    // is not delta: make a nw one and set it up

    if (!isDelta || (null == tmpCsds || tmpCsds.isEmpty())) {
      tmpCsds = cas.newCsds();
      tmpCsds.setup(null, 1);
    } else {
      assert null != tmpCsds;
    }

    return tmpCsds;
  }

  // /**
  // * Create and set up a new Csds for a CAS.
  // * Called whenever needed, after CAS has been updated
  // * with possible new FSs via indexes or references, since previous csds was computed
  // *
  // * This is not needed, because the existing method above would
  // * compute new ones except for the case of a delta serialization with one computed already from
  // the previous deserialization.
  // * - any new FSs are above the line and are found
  // * - the data in the csds are for data below the line, and that data is fixed
  // * -- because it includes all data below the line (referenced or not).
  // * -- there is no way to go from non-referenced to referenced via some update.
  // *
  // * @param cas -
  // * @return a newly computed csds with fs <-> addr tables, heapend number
  // */
  // static CommonSerDesSequential getNewCsds(CASImpl cas) {
  // CommonSerDesSequential tmpCsds = cas.newCsds();
  // tmpCsds.setup(null, 1);
  // return tmpCsds;
  // }

  // public String printCasInfo(CASImpl cas) {
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

  // public void setDeserCas(CASImpl cas) {
  // deserCas = cas;
  // }

  // private TypeInfo getTypeInfo(int typeCode) {
  // if (null == typeInfoArray[typeCode]) {
  // initTypeInfoArray(typeCode);
  // }
  // return typeInfoArray[typeCode];
  // }

  // private void initTypeInfoArray(int typeCode) {
  // TypeImpl type = (TypeImpl) ts.ll_getTypeForCode(typeCode);
  // typeInfoArray[typeCode] = new TypeInfo(type, ts);
  // }

  // private static class TypeInfo {
  // // constant data about a particular type
  // public final TypeImpl type; // for debug
  // public final SlotKind[] slotKinds;
  // public final int[] strRefOffsets;
  //
  // public final boolean isArray;
  // public final boolean isHeapStoredArray; // true if array elements are stored on the main heap
  // // memory while compressing/decompressing
  // public int iPrevHeap; // index of where this fs type occurred in the heap previously
  //
  // public TypeInfo(TypeImpl type, TypeSystemImpl ts) {
  //
  // this.type = type;
  // List<Feature> features = type.getFeatures();
  //
  // isArray = type.isArray(); // feature structure array types named type-of-fs[]
  // isHeapStoredArray = (type == ts.intArrayType) ||
  // (type == ts.floatArrayType) ||
  // (type == ts.fsArrayType) ||
  // (type == ts.stringArrayType) ||
  // (TypeSystemImpl.isArrayTypeNameButNotBuiltIn(type.getName()));
  //
  // final ArrayList<Integer> strRefsTemp = new ArrayList<Integer>();
  // // set up slot kinds
  // if (isArray) {
  // // slotKinds has 2 slots: 1st is for array length, 2nd is the slotkind for the array element
  // SlotKind arrayKind;
  // if (isHeapStoredArray) {
  // if (type == ts.intArrayType) {
  // arrayKind = Slot_Int;
  // } else if (type == ts.floatArrayType) {
  // arrayKind = Slot_Float;
  // } else if (type == ts.stringArrayType) {
  // arrayKind = Slot_StrRef;
  // } else {
  // arrayKind = Slot_HeapRef;
  // }
  // } else {
  //
  // // array, but not heap-store-array
  // if (type == ts.booleanArrayType ||
  // type == ts.byteArrayType) {
  // arrayKind = Slot_ByteRef;
  // } else if (type == ts.shortArrayType) {
  // arrayKind = Slot_ShortRef;
  // } else if (type == ts.longArrayType) {
  // arrayKind = Slot_LongRef;
  // } else if (type == ts.doubleArrayType) {
  // arrayKind = Slot_DoubleRef;
  // } else {
  // throw new RuntimeException("never get here");
  // }
  // }
  //
  // slotKinds = new SlotKind[] {Slot_ArrayLength, arrayKind};
  // strRefOffsets = null;
  //
  // } else {
  //
  // // set up slot kinds for non-arrays
  // ArrayList<SlotKind> slots = new ArrayList<SlotKind>();
  // int i = -1;
  // for (Feature feat : features) {
  // i++;
  // TypeImpl slotType = (TypeImpl) feat.getRange();
  //
  // if (slotType == ts.stringType || (slotType instanceof TypeImpl_string)) {
  // slots.add(Slot_StrRef);
  // strRefsTemp.add(i);
  // } else if (slotType == ts.intType) {
  // slots.add(Slot_Int);
  // } else if (slotType == ts.booleanType) {
  // slots.add(Slot_Boolean);
  // } else if (slotType == ts.byteType) {
  // slots.add(Slot_Byte);
  // } else if (slotType == ts.shortType) {
  // slots.add(Slot_Short);
  // } else if (slotType == ts.floatType) {
  // slots.add(Slot_Float);
  // } else if (slotType == ts.longType) {
  // slots.add(Slot_LongRef);
  // } else if (slotType == ts.doubleType) {
  // slots.add(Slot_DoubleRef);
  // } else {
  // slots.add(Slot_HeapRef);
  // }
  // } // end of for loop
  // slotKinds = slots.toArray(new SlotKind[slots.size()]);
  // // convert to int []
  // strRefOffsets = new int[strRefsTemp.size()];
  // for (int i2 = 0; i2 < strRefOffsets.length; i2++) {
  // strRefOffsets[i2] = strRefsTemp.get(i2);
  // }
  // }
  // }
  //
  // public SlotKind getSlotKind(int offset) {
  // if (0 == offset) {
  // return Slot_TypeCode;
  // }
  // return slotKinds[offset - 1];
  // }
  //
  // @Override
  // public String toString() {
  // return type.toString();
  // }
  //
  // }

  // /**
  // * An iterator-like object for Feature Structures on the heap
  // * next() returns in order of ascending heap addresses those
  // * that correspond to string references
  // *
  // * Returns -1 if no more string refs in this fs
  // *
  // * Not currently used, but save in case String
  // * update impl changes to no-longer always add
  // * new ref to end of string heap
  // */
  // private static class FsStringRefs {
  //
  // final boolean isStrArray;
  // int offset = 0;
  // final int length;
  // final int iHeap;
  // final int[] strRefOffsets;
  //
  // FsStringRefs(TypeInfo typeInfo, int[] heap, int iHeap) {
  // this.iHeap = iHeap;
  // isStrArray = (typeInfo.isHeapStoredArray &&
  // typeInfo.getSlotKind(2) == Slot_StrRef);
  //
  // if (isStrArray) {
  // length = heap[iHeap + 1];
  // strRefOffsets = null;
  // } else {
  // strRefOffsets = typeInfo.strRefOffsets;
  // length = strRefOffsets.length;
  // }
  // }
  //
  //
  // int next() {
  // if (offset < length) {
  // return iHeap + ((isStrArray) ? (2 + offset++) : strRefOffsets[offset++]);
  // } else {
  // return -1;
  // }
  // }
  // }

  /*
   * debugging and dumping
   */

  public static void dumpCas(CASImpl cas) {
    CommonSerDesSequential csds = new CommonSerDesSequential(cas);
    csds.setup(null, 1);

    for (TOP fs : csds.getSortedFSs()) {
      System.out.format("debug heapAddr: %,d type: %s%n", csds.fs2addr.get(fs),
              fs._getTypeImpl().getShortName());
      // if (csds.fs2addr.get(fs) == 439) {
      // System.out.format("debug, fs: %s%n", fs);
      // }
    }
    System.out.format("debug heapend: %,d%n", csds.getHeapEnd());
  }

}
