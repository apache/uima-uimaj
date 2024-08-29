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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.apache.uima.UimaSerializable;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.impl.CommonSerDes.Header;
import org.apache.uima.cas.impl.CommonSerDes.Reading;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;
import org.apache.uima.internal.util.Int2ObjHashMap;
import org.apache.uima.internal.util.IntListIterator;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.Obj2IntIdentityHashMap;
import org.apache.uima.internal.util.SerializationUtils;
import org.apache.uima.internal.util.function.Consumer_T_int_withIOException;
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
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasLoadMode;

// @formatter:off
/**
 * Binary (mostly non compressed) CAS deserialization The methods in this class were originally part
 * of the CASImpl, and were moved here to this class for v3
 * 
 * Binary non compressed CAS serialization is in class CASSerializer, but that class uses routines
 * and data structures in this class.
 * 
 * There is one instance of this class per CAS (shared by all views of that CAS), created at the
 * same time the CAS is created.
 * 
 * This instance also holds data needed for binary serialization, and deserialization. For binary
 * delta deserialization, it uses the data computed on a previous serialization, or, if none, it
 * re-computes it. See scanAllFSsForBinarySerialization method.
 * 
 * The data is computed lazily, and reset with cas reset.
 * 
 * Lifecycle: 
 *   created when a CAS (any view) is first created, as part of the shared view data for that CAS.
 *   never re-created.
 * 
 * Data created when non-delta serializing, in case needed when delta-deserializing later:
 *     xxxAuxAddr2fsa maps aux arrays to FSs
 *     heaps and nextXXXHeapAddrAfterMark (in this case mark is the end).
 * 
 * Reset:
 * 
 * Instance Data:
 *   baseCas - ref to the corresponding CAS (final)
 *   tsi - the CAS's type system impl (can change; each use sets it from CAS API)
 * 
 *   heaps - there is 1 main heap, and 4 aux heaps (Byte, Short, Long, and String
 *     Some uses of this class require these be materialized. (May be input or output)
 * 
 *   for Delta deserialization:
 *     5 ints - representing the first free address in the above 5 heaps, after the mark
 * 
 *   For delta deserialization: Maps for Aux arrays representing updatable arrays (not String):
 *     From starting addr in the aux array to the corresponding V3 FS object    
 */
//@formatter:on
public class BinaryCasSerDes {

  private static final boolean TRACE_DESER = false;

  private static final boolean SOFA_IN_NORMAL_ORDER = false;
  private static final boolean SOFA_AHEAD_OF_NORMAL_ORDER = true;

  /**
   * The offset for the array length cell. An array consists of length+2 number of cells, where the
   * first cell contains the type, the second one the length, and the rest the actual content of the
   * array.
   */
  private static final int arrayLengthFeatOffset = 1;

  /**
   * The number of cells we need to skip to get to the array contents. That is, if we have an array
   * starting at addr, the first cell is at addr+arrayContentOffset.
   */
  private static final int arrayContentOffset = 2;

  private final CASImpl baseCas; // must be the base cas
  private TypeSystemImpl tsi;

  // this can't be an instance field - there may be multiple threads sharing this
  // final private CommonSerDesSequential csds;

  Heap heap;
  ByteHeap byteHeap;
  ShortHeap shortHeap;
  LongHeap longHeap;
  StringHeap stringHeap;

  /**
   * These next are for delta (de)serialization, and identify the first slot in the aux or string
   * tables for new FS data when there's a mark set.
   * 
   * These values are read by CASSerializer when doing delta serialization, and set at the end of a
   * matching binary deserialization.
   * 
   * When serializing a delta, the heaps used are storing just the delta, so any numbers for offsets
   * they yield are adjusted by adding these, so that when the delta is deserialized (and these
   * augment the existing heaps), the references are correct with respect to the deserialized heap
   * model.
   */
  int nextHeapAddrAfterMark;
  int nextStringHeapAddrAfterMark;
  int nextByteHeapAddrAfterMark;
  int nextShortHeapAddrAfterMark;
  int nextLongHeapAddrAfterMark;

  // relocated to CommonSerDesSequential
  // /**
  // * a map from a fs to its addr in the modeled heap
  // *
  // * created during serialization
  // * used during serialization to create addr info for index info serialization
  // *
  // * For delta, the addr is the modeled addr for the full CAS including both above and below the
  // line.
  // */
  // final Obj2IntIdentityHashMap<TOP> fs2addr;
  //
  // /**
  // * a map from a fs addr to the V3 FS
  // * created when serializing (non-delta), deserializing (non-delta)
  // * augmented when deserializing(delta)
  // * used when deserializing (delta and non-delta)
  // * retained after deserializing (in case of subsequent delta (multiple) deserializations being
  // combined)
  // *
  // * For delta, the addr is the modeled addr for the full CAS including both above and below the
  // line.
  // *
  // */
  // final Int2ObjHashMap<TOP, TOP> addr2fs;

  // /**
  // * a map from a fs array of boolean/byte/short/long/double to its addr in the modeled aux heap
  // * the ending address (if needed) is this addr plus the length (from the fs array)
  // * Needed for delta modification deserialization
  // */
  // final Obj2IntIdentityHashMap<TOP> fsa2auxAddr = new Obj2IntIdentityHashMap<>(TOP.class,
  // TOP.singleton);

//@formatter:off
  /**
   * Map from an aux addr starting address for an array of boolean/byte/short/long/double to the V3 FS.
   *   key = simulated starting address in aux heap for the array
   *   value = FS having that array
   * When deserializing a modification, used to find the v3 FS and the offset in the array to modify.
   * 
   * created when serializing (in case receive delta deser back).
   * created when delta deserializing if not available from previous serialization.
   * updated when delta deserializing.
   * reset at end of delta deserializings because multiple mods not supported
   */
//@formatter:on
  private final Int2ObjHashMap<TOP, TOP> byteAuxAddr2fsa = new Int2ObjHashMap<>(TOP.class);
  private final Int2ObjHashMap<TOP, TOP> shortAuxAddr2fsa = new Int2ObjHashMap<>(TOP.class);
  private final Int2ObjHashMap<TOP, TOP> longAuxAddr2fsa = new Int2ObjHashMap<>(TOP.class);

  /**
   * used to calculate total heap size
   */
  boolean isBeforeV3 = false;

  public BinaryCasSerDes(CASImpl baseCAS) {
    baseCas = baseCAS;
  }

  // *********************************
  // D e s e r i a l i z e r s
  // *********************************

  /**
   * Deserializer for Java-object serialized instance of CASSerializer.
   * 
   * @param ser
   *          - The instance to convert back to a CAS
   */
  public void reinit(CASSerializer ser) {
    baseCas.resetNoQuestions();
    reinit(ser.getHeapMetadata(), ser.getHeapArray(), ser.getStringTable(), ser.getFSIndex(),
            ser.getByteArray(), ser.getShortArray(), ser.getLongArray());
  }

  /**
   * This is for deserializing (never delta) from a serialized java object representation or maybe
   * from the JNI bridge
   * 
   * both callers do a cas reset of some kind
   * 
   * @param heapMetadata
   *          -
   * @param heapArray
   *          -
   * @param stringTable
   *          -
   * @param fsIndex
   *          -
   * @param byteHeapArray
   *          -
   * @param shortHeapArray
   *          -
   * @param longHeapArray
   *          -
   */
  void reinit(int[] heapMetadata, int[] heapArray, String[] stringTable, int[] fsIndex,
          byte[] byteHeapArray, short[] shortHeapArray, long[] longHeapArray) {

    // for non Delta case, not held on to compare with compress form 4, which does cas.getCsds() or
    // cas.newCsds() which saves it in cas.svd
    CommonSerDesSequential csds = new CommonSerDesSequential(baseCas);
    csds.setup(null, 1);

    heap = new Heap();
    byteHeap = new ByteHeap();
    shortHeap = new ShortHeap();
    longHeap = new LongHeap();
    stringHeap = new StringHeap();

    createStringTableFromArray(stringTable);

    heap.reinit(heapMetadata, heapArray);
    if (byteHeapArray != null) {
      byteHeap.reinit(byteHeapArray);
    }
    if (shortHeapArray != null) {
      shortHeap.reinit(shortHeapArray);
    }
    if (longHeapArray != null) {
      longHeap.reinit(longHeapArray);
    }

    createFSsFromHeaps(false, 1, csds); // false means not delta

    reinitIndexedFSs(fsIndex, false, i -> csds.addr2fs.get(i));
  }

  public CASImpl setupCasFromCasMgrSerializer(CASMgrSerializer casMgrSerializer) {

    if (null != casMgrSerializer) {
      TypeSystemImpl ts = casMgrSerializer.getTypeSystem();
      baseCas.svd.clear(); // does all clearing except index repositories which will be wiped out
      baseCas.installTypeSystemInAllViews(ts);
      baseCas.commitTypeSystem();

      // reset index repositories -- wipes out Sofa index
      baseCas.indexRepository = casMgrSerializer.getIndexRepository(baseCas);
      baseCas.indexRepository.commit();

      // get handle to existing initial View
      CASImpl initialView = baseCas.getInitialView();

      // throw away all other View information as the CAS definition may have
      // changed
      baseCas.svd.sofa2indexMap.clear();
      baseCas.svd.sofaNbr2ViewMap.clear();
      baseCas.svd.viewCount = 0;

      // freshen the initial view
      initialView.refreshView(baseCas, null);
      baseCas.svd.setViewForSofaNbr(1, initialView);
      baseCas.svd.viewCount = 1;
    }

    return baseCas;
  }

  /**
   * Deserializer for CASCompleteSerializer instances - includes type system and index definitions
   * Never delta
   * 
   * @param casCompSer
   *          -
   */
  public void reinit(CASCompleteSerializer casCompSer) {
    setupCasFromCasMgrSerializer(casCompSer.getCASMgrSerializer());

    // deserialize heap
    CASSerializer casSer = casCompSer.getCASSerializer();
    reinit(casSer.getHeapMetadata(), casSer.getHeapArray(), casSer.getStringTable(),
            casSer.getFSIndex(), casSer.getByteArray(), casSer.getShortArray(),
            casSer.getLongArray());

  }

  /**
   * Binary Deserialization Support An instance of this class is made for every reinit operation
   * doing delta deserialization Mainly used to convert addrs into the main heap into their
   * corresponding FS info
   *
   */
  private class BinDeserSupport {

    private TOP fs; // the current fs or null
    /**
     * The current start address for an FS having maybe multiple fields updated
     */
    private int fsStartAddr;
    /**
     * The current end address (1 beyond last slot)
     */
    private int fsEndAddr;
    /**
     * An array of all the starting indexes of the FSs on the old/prev heap (below the mark, for
     * delta CAS, plus one last one (one beyond the end)
     */
    private int[] fssAddrArray;
    /**
     * index into fssAddrArray
     */
    private int fssIndex;
    private int addrOfFsToBeAddedBack;
    private TOP fsToBeAddedBack; // the fs corresponding to the addrOfFsToBeAddedBack
    // // feature codes - there are exactly the same number as their are features
    // private int[] featCodes;
    private FSsTobeAddedback tobeAddedback = FSsTobeAddedback.createSingle();

    /**
     * add a previously removed fs back
     */
    private void maybeAddBack() {
      if (addrOfFsToBeAddedBack != fsStartAddr) {
        addBackIfRemoved();
      }
    }

    private void addBackIfRemoved() {
      if (fsToBeAddedBack != null) {
        tobeAddedback.addback(fsToBeAddedBack);
      }
    }

    /**
     * if not an array (array values can't be keys) remove if the feature being updated is in an
     * index key
     */
    private void maybeRemove(int heapAddr) {
      TypeImpl type = fs._getTypeImpl();
      boolean wasRemoved;
      if (!type.isArray()) {
        FeatureImpl feat = type.getFeatureImpls()[heapAddr - fsStartAddr - 1];
        wasRemoved = baseCas.checkForInvalidFeatureSetting(fs, feat.getCode(), tobeAddedback);
        addrOfFsToBeAddedBack = wasRemoved ? fsStartAddr : 0;
        fsToBeAddedBack = wasRemoved ? fs : null;
      }
    }

    /**
     * for Deserialization of Delta, when updating existing FSs, If the heap addr is for the next
     * FS, re-add the previous one to those indexes where it was removed, and then maybe remove the
     * new one (and remember which views to re-add to).
     */
    private void maybeAddBackAndRemoveFs(int heapAddr, Int2ObjHashMap<TOP, TOP> addr2fs) {
      if (fsStartAddr == -1) {
        fssIndex = -1;
        addrOfFsToBeAddedBack = -1;
        fs = fsToBeAddedBack = null;
        tobeAddedback.clear();
      }
      findCorrespondingFs(heapAddr, addr2fs); // sets fsStartAddr, end addr

      maybeAddBack();

      // if not an array (array values can't be keys)
      // remove if the feature being updated is in an index key
      maybeRemove(heapAddr);
    }

    /**
     * Given a heap addr which may be in the middle of a FS, find the FS it belongs to and set up
     * things in the bds. Special cases: if the addr is in the middle of an already setup FS, just
     * return The search is done using a binary search, with an exception to check the next item
     * (optimization)
     */
    private void findCorrespondingFs(int heapAddr, Int2ObjHashMap<TOP, TOP> addr2fs) {
      if (fsStartAddr < heapAddr && heapAddr < fsEndAddr) {
        return;
      }

      // search forward by 1 before doing binary search
      fssIndex++; // incrementing dense index into fssAddrArray for start addrs
      fsStartAddr = fssAddrArray[fssIndex]; // must exist
      if (fssIndex + 1 < fssAddrArray.length) { // handle edge case where prev was at the end
        fsEndAddr = fssAddrArray[fssIndex + 1]; // must exist
        if (fsStartAddr < heapAddr && heapAddr < fsEndAddr) {
          fs = addr2fs.get(fsStartAddr);
          return;
        }
      }

      int result;
      if (heapAddr > fsEndAddr) {
        // item is higher
        result = Arrays.binarySearch(fssAddrArray, fssIndex + 1, fssAddrArray.length, heapAddr);
      } else {
        result = Arrays.binarySearch(fssAddrArray, 0, fssIndex - 1, heapAddr);
      }

      // result must be negative - should never modify a type code slot
      assert (result < 0);
      fssIndex = (-result) - 2;
      fsStartAddr = fssAddrArray[fssIndex];
      fsEndAddr = fssAddrArray[fssIndex + 1];
      fs = addr2fs.get(fsStartAddr);
      assert (fsStartAddr < heapAddr && heapAddr < fsEndAddr);
    }

  }

  /**
   * see Blob Format in CASSerializer
   * 
   * This reads in and deserializes CAS data from a stream. Byte swapping may be needed if the blob
   * is from C++ -- C++ blob serialization writes data in native byte order.
   * 
   * Supports delta deserialization. For that, the the csds from the serialization event must be
   * used.
   * 
   * @param istream
   *          -
   * @return - the format of the input stream detected
   * @throws CASRuntimeException
   *           wraps IOException
   */
  public SerialFormat reinit(InputStream istream) throws CASRuntimeException {

    final DataInputStream dis = CommonSerDes.maybeWrapToDataInputStream(istream);

    try {
      Header h = CommonSerDes.readHeader(dis);
      return reinit(h, istream, null, CasLoadMode.DEFAULT, null, AllowPreexistingFS.allow, null);
    } catch (IOException e) {
      String msg = e.getMessage();
      if (msg == null) {
        msg = e.toString();
      }
      throw new CASRuntimeException(CASRuntimeException.BLOB_DESERIALIZATION, msg);
    }
  }

  /**
   * Deserialize a binary input stream, after reading the header, and optionally an externally
   * provided type system and index spec used in compressed form 6 serialization previously
   * 
   * This reads in and deserializes CAS data from a stream. Byte swapping may be needed if the blob
   * is from C++ -- C++ blob serialization writes data in native byte order.
   * 
   * The corresponding serialization code is in org.apache.uima.cas.impl.Serialization, also see
   * CasIOUtils
   * 
   * @param h
   *          -
   * @param istream
   *          -
   * @param casMgrSerializer
   *          null or the Java object representing the externally supplied type and maybe indexes
   *          definition (TSI)
   * @param casLoadMode
   *          DEFAULT or REINIT. REINIT required with compressed form 6 to reinitialize the cas's
   *          type system and index definition, for form 6.
   * @param f6
   *          only used for form 6 where an instance of BinaryCasSerDes6 has been initialized
   * @param allowPreexistingFS
   *          only used for form 6 delta deserialization
   * @param ts
   *          the type system
   * @return the format that was deserialized
   * @throws CASRuntimeException
   *           wraps IOException
   */
  public SerialFormat reinit(Header h, InputStream istream, CASMgrSerializer casMgrSerializer,
          CasLoadMode casLoadMode, BinaryCasSerDes6 f6, AllowPreexistingFS allowPreexistingFS,
          TypeSystemImpl ts) throws CASRuntimeException {

    final DataInputStream dis = CommonSerDes.maybeWrapToDataInputStream(istream);

    CASMgrSerializer embeddedCasMgrSerializer = maybeReadEmbeddedTSI(h, dis);

    if (!h.isForm6() || casLoadMode == CasLoadMode.REINIT) {
      setupCasFromCasMgrSerializer(
              (null != embeddedCasMgrSerializer && embeddedCasMgrSerializer.hasIndexRepository())
                      ? embeddedCasMgrSerializer
                      : casMgrSerializer);
    }

    if (!h.isForm6() && casLoadMode == CasLoadMode.LENIENT) {
      /** Lenient deserialization not support for input of type {0}. */
      throw new CASRuntimeException(CASRuntimeException.LENIENT_NOT_SUPPORTED,
              new Object[] { h.toString() });
    }

    try {
      final boolean delta = h.isDelta;

      if (h.getSeqVersionNbr() < 2 && delta) { // is version 2 and delta
        /** Deserializing a Version 2 Delta Cas into UIMA Version 3 not supported. */
        throw new CASRuntimeException(CASRuntimeException.DESERIALIZING_V2_DELTA_V3);
      }

      if (!delta) {
        baseCas.resetNoQuestions();
      }

      // isBeforeV3 adjusts binary type numbers, and is reset with the CAS when !delta, so it must
      // be set here.
      isBeforeV3 = !h.isV3 && h.getSeqVersionNbr() < 2;

      if (h.isCompressed) {
        if (TRACE_DESER) {
          System.out.format("BinDeser version = %d%n", h.v);
        }
        if (h.form4) {
          BinaryCasSerDes4 bcsd4 = new BinaryCasSerDes4(baseCas.getTypeSystemImpl(), false);
          bcsd4.deserialize(baseCas, dis, delta, h);
          return h.typeSystemIndexDefIncluded ? SerialFormat.COMPRESSED_TSI
                  : SerialFormat.COMPRESSED;
        } else {
          // is form 6
          CASMgrSerializer cms = (embeddedCasMgrSerializer != null) ? embeddedCasMgrSerializer
                  : casMgrSerializer;
          TypeSystemImpl tsRead = (cms != null) ? cms.getTypeSystem() : null;
          if (null != tsRead) {
            tsRead = tsRead.commit(baseCas.getJCasClassLoader()); // https://issues.apache.org/jira/browse/UIMA-5598
          }

          TypeSystemImpl ts_for_decoding = (tsRead != null && embeddedCasMgrSerializer != null)
                  ? tsRead // first choice: embedded - it's always correct
                  : (ts != null) // 2nd choice is passed in ts arg, either ts or f6.getTgtTs()
                          ? ts
                          : (f6 != null && f6.getTgtTs() != null) ? f6.getTgtTs() // this is the ts
                                                                                  // passed in via
                                                                                  // BinaryCasSerDes6
                                                                                  // constructor
                                  : tsRead; // last choice: the ts read from 2nd input to load() in
                                            // CasIOUtils

          try {
            BinaryCasSerDes6 bcsd = (f6 != null) ? new BinaryCasSerDes6(f6, ts_for_decoding)
                    : new BinaryCasSerDes6(baseCas, ts_for_decoding);
            bcsd.deserializeAfterVersion(dis, delta, AllowPreexistingFS.allow);
            return h.typeSystemIndexDefIncluded ? SerialFormat.COMPRESSED_FILTERED_TSI
                    : h.typeSystemIncluded ? SerialFormat.COMPRESSED_FILTERED_TS
                            : SerialFormat.COMPRESSED_FILTERED;
          } catch (ResourceInitializationException e) {
            throw new CASRuntimeException(
                    CASRuntimeException.DESERIALIZING_COMPRESSED_BINARY_UNSUPPORTED, null, e);
          }
        }
      }

      return binaryDeserialization(h);
    } catch (IOException e) {
      String msg = e.getMessage();
      if (msg == null) {
        msg = e.toString();
      }
      throw new CASRuntimeException(CASRuntimeException.BLOB_DESERIALIZATION, msg);
    }

  }

  static CASMgrSerializer maybeReadEmbeddedTSI(Header h, DataInputStream dis) {
    if (h.isTypeSystemIncluded() || h.isTypeSystemIndexDefIncluded()) { // Load TS from CAS stream
      try {
        return SerializationUtils.deserializeCASMgrSerializer(dis);
      } catch (IOException e) {
        /** Unrecognized serialized CAS format */
        throw new CASRuntimeException(CASRuntimeException.UNRECOGNIZED_SERIALIZED_CAS_FORMAT, null,
                e);
      }
    }
    return null;
  }

  // **********************************************************
  // ------ NON COMPRESSED BINARY DESEERIALIZATION ------ *
  // For corresponding serialization code, see CASSerializer *
  // ***********************************************************
  /**
   * build a model of the heap, string and aux heaps. For delta deserialization, this is presumed to
   * be in response to a previous serialization for delta - these can be just for the new ones read
   * into these recreate / update V3 feature structures from this data
   * 
   * delta CAS supported use case: CAS(1) -> binary serialize -> binary deserialize -> CAS(2).
   * CAS(2) has mark set (before any new activity in deserialized CAS) CAS(2) has updates - new FSs,
   * and mods to existing ones CAS(2) -> delta binary ser -> delta binary deser -> CAS(1).
   * 
   * V3 supports the above scenario by retaining some information in CAS(2) at the end of the
   * initial deserialization, including the model heap size/cellsUsed. - this is needed to properly
   * do a compatible-with-v2 delta serialization.
   * 
   * delta CAS edge use cases not supported: serialize (not binary), then receive delta binary
   * serialization
   * 
   * Both v2 and v3 assume that the delta mark is set immediately after binary deserialization;
   * otherwise, subsequent binary deserialization of the delta will fail.
   * 
   * This method assumes a previous binary serialization was done, and the following data structures
   * are still valid (i.e. no CAS altering operations have been done) (these are reset: heap,
   * stringHeap, byteHeap, shortHeap, longHeap) csds, [string/byte/short/long]auxAddr2fs (for array
   * mods) nextHeapAddrAfterMark, next[string/byte/short/long]HeapAddrAfterMark
   * 
   * @param h
   *          the Header (read by the caller)
   * @return the format of the incoming serialized data
   */
  private SerialFormat binaryDeserialization(Header h) {

    final boolean delta = h.isDelta;

    final Reading r = h.reading;

    final DataInputStream dis = r.dis;

    final CommonSerDesSequential csds = BinaryCasSerDes4.getCsds(baseCas, delta);

    if (delta) {
      if (nextHeapAddrAfterMark == 0 || heap == null || heap.getCellsUsed() <= 1) {
        Misc.internalError(); // can't deserialize without a previous binary serialization for this
                              // CAS
      }
    } else {
      if (heap == null) {
        heap = new Heap();
      } else {
        heap.reset();
      }
      if (byteHeap == null) {
        byteHeap = new ByteHeap();
      } else {
        byteHeap.reset();
      }
      if (shortHeap == null) {
        shortHeap = new ShortHeap();
      } else {
        shortHeap.reset();
      }
      if (longHeap == null) {
        longHeap = new LongHeap();
      } else {
        longHeap.reset();
      }
      if (stringHeap == null) {
        stringHeap = new StringHeap();
      } else {
        stringHeap.reset();
      }
      clearDeltaOffsets();
    }

    try {
      // main fsheap
      final int fsheapsz = r.readInt();

      // reading the 0th (null) element, because that's what V2 did
      int startPos = 0;
      if (!delta) {
        heap.reinitSizeOnly(fsheapsz);
      } else {
        startPos = heap.getNextId();
        heap.grow(fsheapsz);
      }
      if (TRACE_DESER) {
        System.out.format("BinDes Plain %s startPos: %,d mainHeapSize: %d%n", delta ? "Delta" : "",
                startPos, fsheapsz);
      }

      // add new heap slots
      for (int i = startPos; i < fsheapsz + startPos; i++) {
        heap.heap[i] = r.readInt();
        // if (TRACE_DESER) {
        // if (i < 101 + startPos) {
        // if (i % 5 == 0) System.out.format("%n i: %4d ", i);
        // System.out.format("%,15d ", heap.heap[i]);
        // }
        // }
      }
      // if (TRACE_DESER) System.out.println("");

      // string heap
      int stringheapsz = r.readInt();

      final StringHeapDeserializationHelper shdh = new StringHeapDeserializationHelper();

      shdh.charHeap = new char[stringheapsz];
      for (int i = 0; i < stringheapsz; i++) {
        shdh.charHeap[i] = (char) r.readShort();
      }
      shdh.charHeapPos = stringheapsz;

      // word alignment
      if (stringheapsz % 2 != 0) {
        dis.readChar();
      }

      // string ref heap
      int refheapsz = r.readInt();

      refheapsz--;
      refheapsz = refheapsz / 2;
      refheapsz = refheapsz * 3;

      // read back into references consisting of three ints
      // --stringheap offset, length, stringlist offset
      shdh.refHeap = new int[StringHeapDeserializationHelper.FIRST_CELL_REF + refheapsz];

      dis.readInt(); // 0
      for (int i = shdh.refHeapPos; i < shdh.refHeap.length; i += StringHeapDeserializationHelper.REF_HEAP_CELL_SIZE) {
        shdh.refHeap[i + StringHeapDeserializationHelper.CHAR_HEAP_POINTER_OFFSET] = r.readInt();
        shdh.refHeap[i + StringHeapDeserializationHelper.CHAR_HEAP_STRLEN_OFFSET] = r.readInt();
        shdh.refHeap[i + StringHeapDeserializationHelper.STRING_LIST_ADDR_OFFSET] = 0;
      }
      shdh.refHeapPos = refheapsz + StringHeapDeserializationHelper.FIRST_CELL_REF;

      stringHeap.reinit(shdh, delta);

      final int fsmodssz2;
      final int[] modWords;
      // if delta, handle modified fs heap cells
      if (delta) {
        // Delta Binary Deserialization
        //
        // At this point, we have
        // - not yet converted the main heap array into FSs.
        // - not yet read in Aux Heaps (except for strings)
        //
        // So, we do this in 2 phases.
        // - This phase just reads in the data but does not act on it.
        // - Phase 2 happens after the FSs are created from the heap data.
        fsmodssz2 = 2 * r.readInt();
        modWords = new int[fsmodssz2];

        for (int i = 0; i < fsmodssz2; i++) {
          modWords[i] = r.readInt();
        }
        if (TRACE_DESER) {
          System.out.format("BinDes modified heap slot count: %,d%n", fsmodssz2 / 2);
        }
      } else {
        fsmodssz2 = 0; // not used but must be set to make "final" work
        modWords = null;
      }

      // indexed FSs
      int fsindexsz = r.readInt();
      int[] fsindexes = new int[fsindexsz];
      if (TRACE_DESER) {
        System.out.format("BinDes indexedFSs count: %,d%n", fsindexsz);
      }
      for (int i = 0; i < fsindexsz; i++) {
        fsindexes[i] = r.readInt();
        if (TRACE_DESER) {
          if (i % 5 == 0) {
            System.out.format("%n i: %5d ", i);
          }
          System.out.format("%15d ", fsindexes[i]);
        }
      }
      if (TRACE_DESER) {
        System.out.println("");
      }

      // byte heap
      int heapsz = r.readInt();
      if (TRACE_DESER) {
        System.out.format("BinDes ByteHeap size: %,d%n", heapsz);
      }

      if (!delta) {
        byteHeap.heap = new byte[Math.max(16, heapsz)]; // must be > 0
        dis.readFully(byteHeap.heap, 0, heapsz);
        byteHeap.heapPos = heapsz;
      } else {
        final int offset2startOfNewBytes = byteHeap.reserve(heapsz);
        dis.readFully(byteHeap.heap, offset2startOfNewBytes, heapsz);
      }
      // word alignment
      int align = (4 - (heapsz % 4)) % 4;
      BinaryCasSerDes6.skipBytes(dis, align);

      // short heap
      heapsz = r.readInt();
      if (TRACE_DESER) {
        System.out.format("BinDes ShortHeap size: %,d%n", heapsz);
      }

      if (!delta) {
        shortHeap.heap = new short[Math.max(16, heapsz)]; // must be > 0
        for (int i = 0; i < heapsz; i++) {
          shortHeap.heap[i] = r.readShort();
        }
        shortHeap.heapPos = heapsz;
      } else {
        final int pos = shortHeap.reserve(heapsz);
        final int end = pos + heapsz;
        for (int i = pos; i < end; i++) {
          shortHeap.addShort(r.readShort());
        }
      }
      // word alignment
      if (heapsz % 2 != 0) {
        dis.readShort();
      }

      // long heap
      heapsz = r.readInt();
      if (TRACE_DESER) {
        System.out.format("BinDes LongHeap size: %,d%n", heapsz);
      }

      if (!delta) {
        longHeap.heap = new long[Math.max(16, heapsz)]; // must be > 0
        for (int i = 0; i < heapsz; i++) {
          longHeap.heap[i] = r.readLong();
        }
        longHeap.heapPos = heapsz;
      } else {
        longHeap.reserve(heapsz);
        for (int i = 0; i < heapsz; i++) {
          longHeap.addLong(r.readLong());
        }
      }

      if (delta) {
        // The modifications are all to existing FSs.
        // The modifications consist of an address (offset in an aux array) which is an array
        // element.
        // We don't update the aux array, but instead update the actual FS below the line
        // representing the array.
        // To identify the fs, we use the xxAuxAddr2fsa sorted list forms and do a binary search to
        // find the item to update,
        // with a fast path for the same or next item.
        // Same - use case is multiple updates into the same array

        // modified Byte Heap
        heapsz = updateAuxArrayMods(r, byteAuxAddr2fsa, (ba, arrayIndex) -> {
          if (ba instanceof ByteArray) {
            ((ByteArray) ba).set(arrayIndex, dis.readByte());
          } else {
            ((BooleanArray) ba).set(arrayIndex, dis.readByte() == 1);
          }
        });

        // word alignment
        align = (4 - (heapsz % 4)) % 4;
        BinaryCasSerDes6.skipBytes(dis, align);

        // modified Short Heap
        heapsz = updateAuxArrayMods(r, shortAuxAddr2fsa, (sa, arrayIndex) -> {
          ((ShortArray) sa).set(arrayIndex, r.readShort());
        });

        // word alignment
        if (heapsz % 2 != 0) {
          dis.readShort();
        }

        // modified Long Heap
        updateAuxArrayMods(r, longAuxAddr2fsa, (la, arrayIndex) -> {
          if (la instanceof LongArray) {
            ((LongArray) la).set(arrayIndex, r.readLong());
          } else {
            ((DoubleArray) la).set(arrayIndex, CASImpl.long2double(r.readLong()));
          }
        });
      } // of delta - modified processing

      // *********************************************
      // Convert model heap added FS into real FS *
      // update addr2fs and fs2addr *
      // update byte/short/long/string auxAddr2fsa *
      // *********************************************

      // build the new FSs and record in addr2FSs
      createFSsFromHeaps(delta, startPos == 0 ? 1 : startPos, csds);

      if (delta) {
        final BinDeserSupport bds = new BinDeserSupport();

        bds.fssAddrArray = new int[csds.addr2fs.size() + 1]; // need one extra at the end
        IntListIterator it = csds.addr2fs.keyIterator();
        int iaa = 0;
        while (it.hasNext()) {
          bds.fssAddrArray[iaa++] = it.nextNvc();
        }
        // iaa at this point refs the last entry in the table
        bds.fssAddrArray[iaa] = heap.getCellsUsed();
        Arrays.sort(bds.fssAddrArray); // because addr2fs.keyIterator is arbitrary order due to hash
                                       // table impl
        assert (bds.fssAddrArray[iaa] == heap.getCellsUsed());

        bds.fsStartAddr = -1; // avoid initial addback of addback/remove pair.

        // loop over all heap modifications to existing FSs

        // first disable auto addbacks for index corruption - this routine is handling that
        baseCas.svd.disableAutoCorruptionCheck = true;

        try {
          for (int i = 0; i < modWords.length; i = i + 2) {
            final int heapAddrBeingModified = modWords[i];
            bds.maybeAddBackAndRemoveFs(heapAddrBeingModified, csds.addr2fs);
            updateHeapSlot(bds, heapAddrBeingModified, modWords[i + 1], csds.addr2fs);
            // heap.heap[heapAddrBeingModified] = r.readInt();
          }
          bds.addBackIfRemoved();
          bds.fssAddrArray = null; // free storage
        } finally {
          baseCas.svd.disableAutoCorruptionCheck = false;
        }
      }

      // update the indexes
      IntFunction<TOP> getFsFromAddr = i -> csds.addr2fs.get(i);
      reinitIndexedFSs(fsindexes, delta, getFsFromAddr);

      if (!delta) {
        setHeapExtents();
        csds.setHeapEnd(nextHeapAddrAfterMark);
      }

      // cleanup at the end of Binary Deserialization, both Delta and full
      // saved and not cleaned up
      // because needed by subsequent delta serialization:
      // fs2addr, addr2fs,
      // byte/short/longAuxAddr2fsa
      // next[xx]HeapAddrAfterMark

      // heaps cleaned up because after a full deser, a subsequent ser only populates these
      // with the new items.
      heap = null;
      stringHeap = null;
      byteHeap = null;
      shortHeap = null;
      longHeap = null;

      // cleared because only used for delta deser, for mods, and mods not allowed for multiple
      // deltas
      clearAuxAddr2fsa();
    } catch (IOException e) {
      String msg = e.getMessage();
      if (msg == null) {
        msg = e.toString();
      }
      throw new CASRuntimeException(CASRuntimeException.BLOB_DESERIALIZATION, msg);
    } finally {
      // safety - not currently logically needed, since this field is only used in the case it has
      // been set
      // (when there is no previous documentAnnotation, and we're adding a new one, and
      // it may not be indexed)
      //
      // If it is not indexed, removing this will allow the FS to garbage collected.
      baseCas.forAllViews(view -> view.set_deserialized_doc_annot_not_indexed(null));
    }

    return h.typeSystemIndexDefIncluded ? SerialFormat.BINARY_TSI : SerialFormat.BINARY;
  }

  void setHeapExtents() {
    // We subtract 1 because the when creating the new entries in these heaps, the first one is
    // at location 1, not 0, due to how the heaps are set up,
    // but they are written from location 1, so when deserialized, they appear at an
    // offset of 1 less.
    nextHeapAddrAfterMark = heap.getCellsUsed(); // not really used except for sanity check
    nextStringHeapAddrAfterMark = stringHeap.getSize() - 1;
    nextByteHeapAddrAfterMark = byteHeap.getSize() - 1;
    nextShortHeapAddrAfterMark = shortHeap.getSize() - 1;
    nextLongHeapAddrAfterMark = longHeap.getSize() - 1;
  }

  /**
   * Called 3 times to process non-compressed binary deserialization of aux array modifications -
   * once for byte/boolean, short, and long/double
   */
  int updateAuxArrayMods(Reading r, Int2ObjHashMap<TOP, TOP> auxAddr2fsa,
          Consumer_T_int_withIOException<TOP> setter) throws IOException {
    final int heapsz = r.readInt();
    if (heapsz > 0) {
      final int[] tempHeapAddrs = new int[heapsz];
      final int[] sortedArrayAddrs = auxAddr2fsa.getSortedKeys();
      int sortedArrayAddrsIndex = 0;

      for (int i = 0; i < heapsz; i++) {
        tempHeapAddrs[i] = r.readInt();
      }

      for (int i = 0; i < heapsz; i++) {
        sortedArrayAddrsIndex = getSortedArrayAddrsIndex(sortedArrayAddrs, tempHeapAddrs[i],
                sortedArrayAddrsIndex);
        final int arrayStart = sortedArrayAddrs[sortedArrayAddrsIndex];
        TOP fs = auxAddr2fsa.get(arrayStart);
        final int arrayIndex = tempHeapAddrs[i] - arrayStart;
        setter.accept(fs, arrayIndex);
        // EXAMPLE of setter:
        // if (la instanceof LongArray) {
        // ((LongArray)la).set(arrayIndex, r.readLong());
        // } else {
        // ((DoubleArray)la).set(arrayIndex, CASImpl.long2double(r.readLong()));
        // }
      }
    }
    return heapsz;
  }

  // /**
  // * gets number of views, number of sofas,
  // * For all sofas,
  // * adds them to the index repo in the base index
  // * registers the sofa
  // * insures initial view created
  // * for all views:
  // * does the view action and updates the documentannotation
  // * @param fsIndex the index info except for the actual list of FSs to reindex
  // * @param fss the lists of FSs to reindex (concatenated add/remove, or just adds if not delta)
  // * @param viewAction
  // */
  // void reinitIndexedFSs_common(int[] fsIndex, List<TOP> fss, DeserBinaryIndexes viewAction) {
  // // Add FSs to index repository for base CAS
  // int numViews = fsIndex[0];
  // int loopLen = fsIndex[1]; // number of sofas, not necessarily the same as
  // // number of views
  // // because the initial view may not have a sofa
  // for (int i = 0; i < loopLen; i++) { // iterate over all the sofas,
  // baseCas.indexRepository.addFS(fss.get(i)); // add to base index
  // }
  //
  //
  // baseCas.forAllSofas(sofa -> {
  // String id = sofa.getSofaID();
  // if (CAS.NAME_DEFAULT_SOFA.equals(id)) { // _InitialView
  // baseCas.registerInitialSofa();
  // baseCas.addSofaViewName(id);
  // }
  // // next line the getView as a side effect
  // // checks for dupl sofa name, and if not,
  // // adds the name to the sofaNameSet
  // ((CASImpl) baseCas.getView(sofa)).registerView(sofa);
  // });
  //
  // baseCas.getInitialView(); // done for side effect of creating the initial view if not present
  // // must be done before the next line, because it sets the
  // // viewCount to 1.
  // baseCas.setViewCount(numViews); // total number of views
  //
  // int fsIndexIdx = 2;
  // for (int viewNbr = 1; viewNbr <= numViews; viewNbr++) {
  // CASImpl view = (viewNbr == 1) ? (CASImpl) baseCas.getInitialView() : (CASImpl)
  // baseCas.getView(viewNbr);
  // if (view != null) {
  // fsIndexIdx += (1 + viewAction.apply(fsIndexIdx, view));
  // view.updateDocumentAnnotation(); // noop if sofa local data string == null
  // } else {
  // fsIndexIdx += 1;
  // }
  // }
  // }

  // @formatter:off
  /**
   * This routine is used by several of the deserializers.
   *   Each one may have a different way to go from the addr to the fs
   *     e.g. Compressed form 6: fsStartIndexes.getSrcFsFromTgtSeq(...)
   *          plain binary:      addr2fs.get(...)
   * 
   * gets number of views, number of sofas,
   * For all sofas, 
   *   adds them to the index repo in the base index
   *   registers the sofa
   * insures initial view created
   * for all views:
   *   does the view action and updates the documentannotation
   * 
   * @param fsIndex
   *          - array of fsRefs and counts, for sofas, and all views
   * @param isDeltaMods
   *          - true for calls which are for delta mods - these have adds/removes
   */
  // @formatter:on
  void reinitIndexedFSs(int[] fsIndex, boolean isDeltaMods, IntFunction<TOP> getFsFromAddr) {
    int idx = reinitIndexedFSsSofas(fsIndex, isDeltaMods, getFsFromAddr);
    reinitIndexedFSs(fsIndex, isDeltaMods, getFsFromAddr, fsIndex[0], idx);
  }

  void reinitIndexedFSs(int[] fsIndex, boolean isDeltaMods, IntFunction<TOP> getFsFromAddr,
          IntFunction<TOP> getSofaFromAddr) {
    int idx = reinitIndexedFSsSofas(fsIndex, isDeltaMods, getSofaFromAddr);
    reinitIndexedFSs(fsIndex, isDeltaMods, getFsFromAddr, fsIndex[0], idx);
  }

  int reinitIndexedFSsSofas(int[] fsIndex, boolean isDeltaMods, IntFunction<TOP> getFsFromAddr) {
    int numSofas = fsIndex[1]; // number of sofas, not necessarily the same as number of views
                               // (initial view may not have a sofa)
    int idx = 2;
    int end1 = 2 + numSofas;
    for (; idx < end1; idx++) { // iterate over all the sofas,
      baseCas.indexRepository.addFS(getFsFromAddr.apply(fsIndex[idx])); // add to base index
    }
    return idx;
  }

  void reinitIndexedFSs(int[] fsIndex, boolean isDeltaMods, IntFunction<TOP> getFsFromAddr,
          int numViews, int idx) {
    baseCas.forAllSofas(sofa -> {
      String id = sofa.getSofaID();
      if (CAS.NAME_DEFAULT_SOFA.equals(id)) { // _InitialView
        baseCas.registerInitialSofa();
        baseCas.addSofaViewName(id);
      }
      // next line the getView as a side effect
      // checks for dupl sofa name, and if not,
      // adds the name to the sofaNameSet
      ((CASImpl) baseCas.getView(sofa)).registerView(sofa);
    });

    baseCas.getInitialView(); // done for side effect of creating the initial view if not present
    // must be done before the next line, because it sets the
    // viewCount to 1.
    baseCas.setViewCount(numViews); // total number of views

    for (int viewNbr = 1; viewNbr <= numViews; viewNbr++) { // <= because starting at 1
      CASImpl view = (viewNbr == 1) ? (CASImpl) baseCas.getInitialView()
              : (CASImpl) baseCas.getView(viewNbr);
      FSIndexRepositoryImpl ir = (view == null) ? null : view.indexRepository;

      int length = fsIndex[idx++];
      reinitDeltaIndexedFSsInner(ir, fsIndex, idx, length, true, getFsFromAddr); // adds
      idx += length;

      if (isDeltaMods) {
        length = fsIndex[idx++];
        reinitDeltaIndexedFSsInner(ir, fsIndex, idx, length, false, getFsFromAddr); // removes
        idx += length;

        // skip the reindex - this isn't done here https://issues.apache.org/jira/browse/UIMA-4100
        // but we need to run the loop to read over the items in the input stream
        length = fsIndex[idx++];
        idx += length;
      }
      if (view != null) {
        view.updateDocumentAnnotation(); // noop if sofa local data string == null
      }
    } // end of loop for all views
  }

  // void reinitDeltaIndexedFSs(int[] fsIndex, IntFunction<TOP> getFsFromAddr) {
  //
  // reinitIndexedFSs_common(fsIndex, (loopStart, view) -> {
  // // for all views
  //
  // FSIndexRepositoryImpl ir = view.indexRepository;
  // loopStart = reinitDeltaIndexedFSsInner(ir, fsIndex, loopStart, true, getFsFromAddr); // adds
  // loopStart = reinitDeltaIndexedFSsInner(ir, fsIndex, loopStart, false, getFsFromAddr); //
  // removes
  //
  // // skip the reindex - this isn't done here https://issues.apache.org/jira/browse/UIMA-4100
  // // but we need to run the loop to read over the items in the input stream
  // return fsIndex[loopStart]; // return loopLen
  // });
  // }

  /**
   * Given a list of FSs and a starting index and length: iterate over the FSs, and add or remove
   * that from the indexes.
   * 
   * @param ir
   *          index repository
   * @param fsindexes
   *          the list having the fss
   * @param idx
   *          the starting index
   * @param length
   *          the length
   * @param isAdd
   *          true to add, false to remove
   */
  void reinitDeltaIndexedFSsInner(FSIndexRepositoryImpl ir, int[] fsindexes, int idx, int length,
          boolean isAdd, IntFunction<TOP> getFsFromAddr) {
    if (ir == null) {
      return;
    }
    final int end1 = idx + length;
    // add FSs to index
    for (; idx < end1; idx++) {
      TOP fs = getFsFromAddr.apply(fsindexes[idx]);
      if (isAdd) {
        ir.addFS(fs);
      } else {
        ir.removeFS(fs);
      }
    }
  }

  /**
   * given an aux address representing an element of an array, find the start of the array Fast path
   * for the same as before array. binary search of subsequent ones (the addresses in the
   * serializations are not sorted.)
   * 
   * @param sortedStarts
   *          the sorted array of start addresses
   * @param auxAddr
   *          the address being updated
   * @param currentStart
   *          the last value found for fast path
   * @return index into the sortedStarts
   */
  private int getSortedArrayAddrsIndex(int[] sortedArrayAddrs, int auxAddr,
          int sortedArrayAddrsIndex) {
    int curStart = sortedArrayAddrs[sortedArrayAddrsIndex];
    int nextStart = ((sortedArrayAddrsIndex + 1) == sortedArrayAddrs.length) ? Integer.MAX_VALUE
            : sortedArrayAddrs[sortedArrayAddrsIndex + 1];
    if (auxAddr >= curStart && auxAddr < nextStart) {
      return sortedArrayAddrsIndex; // in same array
    }
    int v = Arrays.binarySearch(sortedArrayAddrs, auxAddr);
    // e.g. 1 2 3 6 9 14 < sorted array addrs, key = 4, return the index of 3 = 2
    // binary search: insertion point is index of "6" = 3.
    // binary search returns -insertionpoint -1 = -3 -1 == -4
    // extraction code (below) returns: 4 -2 = 2;
    return (v >= 0) ? v : (-v) - 2;
  }

  /******************************************************
   * Serialization support *
   ******************************************************/

  // IndexedFSs format:
  // number of views
  // number of sofas
  // [sofa-1 ... sofa-n]
  // number of FS indexed in View1 [ views in order of view number ]
  // [FS-1 ... FS-n]
  // etc.
  int[] getIndexedFSs(Obj2IntIdentityHashMap<TOP> fs2addr) {
    IntVector v = new IntVector();
    Collection<TOP> fss;

    int numViews = baseCas.getViewCount();
    v.add(numViews);

    // Get sofas
    fss = baseCas.indexRepository.getIndexedFSs();

    addIdsToIntVector(fss, v, fs2addr);

    // Get indexes for each view in the CAS
    baseCas.forAllViews(view -> addIdsToIntVector(view.getIndexedFSs(), v, fs2addr));
    return v.toArray();
  }

  void addIdsToIntVector(Collection<TOP> fss, IntVector v, Obj2IntIdentityHashMap<TOP> fs2addr) {
    v.add(fss.size());
    // for testing, fs2addr may be null, in which case, use the fsid instead
    if (null == fs2addr) {
      for (TOP fs : fss) {
        v.add(fs._id);
      }
    } else {
      for (TOP fs : fss) {
        v.add(fs2addr.get(fs));
      }
    }
  }

  void addIdsToIntVector(Set<TOP> fss, IntVector v, Obj2IntIdentityHashMap<TOP> fs2addr) {
    v.add(fss.size());
    for (TOP fs : fss) {
      v.add(fs2addr.get(fs));
    }
  }

  // Delta IndexedFSs format:
  // number of views
  // number of sofas - new
  // [sofa-1 ... sofa-n]
  // number of new FS add in View1 .. n, in view number order
  // [FS-1 ... FS-n]
  // number of FS removed from View1
  // [FS-1 ... FS-n]
  // number of FS reindexed in View1
  // [FS-1 ... FS-n]
  // etc.
  int[] getDeltaIndexedFSs(MarkerImpl mark, Obj2IntIdentityHashMap<TOP> fs2addr) {
    IntVector v = new IntVector();

    int numViews = baseCas.getViewCount();
    v.add(numViews);

    // Get the new Sofa FS
    IntVector newSofas = new IntVector();

    baseCas.indexRepository.walkIndexedFSs(fs -> {
      if (mark.isNew(fs)) {
        newSofas.add(fs2addr.get(fs));
      }
    });

    v.add(newSofas.size());
    v.add(newSofas.getArray(), 0, newSofas.size());

    // Get indexes for each view in the CAS
    for (int viewNbr = 1; viewNbr <= numViews; viewNbr++) {
      FSIndexRepositoryImpl loopIndexRep = (FSIndexRepositoryImpl) baseCas
              .getSofaIndexRepository(viewNbr);
      Set<TOP> fssAdded, fssDeleted, fssReindexed;
      if (loopIndexRep != null) {
        fssAdded = loopIndexRep.getAddedFSs();
        fssDeleted = loopIndexRep.getDeletedFSs();
        fssReindexed = loopIndexRep.getReindexedFSs();
      } else {
        fssAdded = Collections.emptySet();
        fssDeleted = Collections.emptySet();
        fssReindexed = Collections.emptySet();
      }
      addIdsToIntVector(fssAdded, v, fs2addr);
      addIdsToIntVector(fssDeleted, v, fs2addr);
      addIdsToIntVector(fssReindexed, v, fs2addr);
    }
    return v.toArray();
  }

  void createStringTableFromArray(String[] stringTable) {
    // why a new heap instead of reseting the old one???
    // this.stringHeap = new StringHeap();
    stringHeap.reset();
    for (int i = 1; i < stringTable.length; i++) {
      stringHeap.addString(stringTable[i]);
    }
  }

  public static int getFsSpaceReq(TOP fs, TypeImpl type) {
    // use method in type; pass in array size if array
    return type.getFsSpaceReq(fs);
  }

  // private long swap8(DataInputStream dis, byte[] buf) throws IOException {
  //
  // buf[7] = dis.readByte();
  // buf[6] = dis.readByte();
  // buf[5] = dis.readByte();
  // buf[4] = dis.readByte();
  // buf[3] = dis.readByte();
  // buf[2] = dis.readByte();
  // buf[1] = dis.readByte();
  // buf[0] = dis.readByte();
  // ByteBuffer bb = ByteBuffer.wrap(buf);
  // return bb.getLong();
  // }
  //
  // private int swap4(DataInputStream dis, byte[] buf) throws IOException {
  // buf[3] = dis.readByte();
  // buf[2] = dis.readByte();
  // buf[1] = dis.readByte();
  // buf[0] = dis.readByte();
  // ByteBuffer bb = ByteBuffer.wrap(buf);
  // return bb.getInt();
  // }
  //
  // private char swap2(DataInputStream dis, byte[] buf) throws IOException {
  // buf[1] = dis.readByte();
  // buf[0] = dis.readByte();
  // ByteBuffer bb = ByteBuffer.wrap(buf, 0, 2);
  // return bb.getChar();
  // }

  // @formatter:off
  /**
   * Called when serializing a cas, or deserializing a delta CAS, if not saved in that case from a previous 
   * binary serialization (in that case, the scan is done as if it is doing a non-delta serialization).
   * 
   * Initialize the serialization model for binary serialization in CASSerializer from a CAS
   * Do 2 scans, each by walking all the reachable FSs
   *   - The first one processes all fs (including for delta, those below the line)
   *      -- computes the fs to addr map and its inverse, based on the size of each FS.
   *      -- done by CommonSerDesSequential class's "setup" method
   * 
   *   - The second one computes the values of the main and aux heaps and string heaps except for delta mods
   *      -- for delta, the heaps only have "new" values that binary serialization will write out as arrays
   *         --- mods are computed from FsChange info and added to the appropriate heaps, later  
   *
   *         - for byte/short/long/string array use, compute auxAddr2fsa maps. 
   *           This is used when deserializing delta mod info, to locate the fs to update
   * 
   * For delta serialization, the heaps are populated only with the new values.
   *   - Values "nextXXHeapAddrAfterMark" are added to main heap refs to aux heaps and to string tables,
   *     so they are correct after deserialization does delta deserialization and adds the aux heap and string heap
   *     info to the existing heaps.
   * 
   *     This is also done for the main heap refs, so that refs to existing FSs below the line and above the line
   *     are treated uniformly.
   * 
   * The results must be retained for the use case of subsequently receiving back a delta cas.
   * 
   * @param mark null or the mark to use for separating the new from from the previously existing 
   *        used by delta cas.
   * @return null or for delta, all the found FSs
   */
  // @formatter:on
  List<TOP> scanAllFSsForBinarySerialization(MarkerImpl mark, CommonSerDesSequential csds) {
    final boolean isMarkSet = mark != null;

    List<TOP> all = null;
    int prevHeapEnd = csds.getHeapEnd(); // used if mark is set
    if (isMarkSet) {

      all = csds.setup(mark, csds.getHeapEnd()); // add new stuff to existing csds
    } // otherwise, it's set up already, using null, 1 as the arguments, when getCsds() is called

    // For delta, these heaps will start at 1, and only hold new items
    heap = new Heap(isMarkSet ? (1 + csds.getHeapEnd() - prevHeapEnd) : csds.getHeapEnd());
    byteHeap = new ByteHeap();
    shortHeap = new ShortHeap();
    longHeap = new LongHeap();
    stringHeap = new StringHeap();

    if (!isMarkSet) {
      clearDeltaOffsets(); // set nextXXheapAfterMark to 0;
      clearAuxAddr2fsa();
    }

    List<TOP> itemsToExtract = csds.getSortedFSs();
    // isMarkSet ? CASImpl.filterAboveMark(csds.getSortedFSs(), mark) : csds.getSortedFSs();
    for (TOP fs : itemsToExtract) {
      if (!isMarkSet || mark.isNew(fs)) {
        // skip extraction for FSs below the mark.
        // - updated slots will update aux heaps when delta mods are processed
        extractFsToV2Heaps(fs, isMarkSet, csds.fs2addr);
      }
    }

    return all;
  }

  // /**
  // * to support serializing addr in aux arrays for modifications below the mark,
  // * scan to compute the starting address of each array that's below the mark
  // * and build maps from Array FSs to aux array starting addresses
  // */
  // void scanAllFSsForBinaryDeltaSerialization(MarkerImpl mark, CommonSerDesSequential csds) {
  //
  // }
  //
  /**
   * called in fs._id order to populate heaps from all FSs.
   * 
   * For delta cas, only called for new above-the-line FSs
   * 
   * @param fs
   *          Feature Structure to use to set heaps
   * @param isMarkSet
   *          true if mark is set, used to compute first
   */
  private void extractFsToV2Heaps(TOP fs, boolean isMarkSet, Obj2IntIdentityHashMap<TOP> fs2addr) {
    TypeImpl type = fs._getTypeImpl();
    // pos is the pos in the new heaps; for delta it needs adjustment if written out
    int pos = heap.add(getFsSpaceReq(fs, type), type.getCode());

    if (type.isArray()) {

      // next slot is the length
      final int length = ((CommonArrayFS<?>) fs).size();
      heap.heap[pos + arrayLengthFeatOffset] = length;
      // next slot are the values
      int i = pos + arrayContentOffset;

      switch (type.getComponentSlotKind()) {

        case Slot_Int:
          System.arraycopy(((IntegerArray) fs)._getTheArray(), 0, heap.heap,
                  pos + arrayContentOffset, length);
          break;

        case Slot_Float:
          for (float v : ((FloatArray) fs)._getTheArray()) {
            heap.heap[i++] = CASImpl.float2int(v);
          }
          break;

        case Slot_StrRef:
          for (String s : ((StringArray) fs)._getTheArray()) {
            int strAddr = stringHeap.addString(s);
            // strAddr is the offset in the new str table; for delta, needs adjustment
            heap.heap[i++] = (strAddr == 0) ? 0 : nextStringHeapAddrAfterMark + strAddr;
          }
          break;

        case Slot_BooleanRef: {
          int baAddr = byteHeap.addBooleanArray(((BooleanArray) fs)._getTheArray());
          heap.heap[i] = nextByteHeapAddrAfterMark + baAddr;
          byteAuxAddr2fsa.put(nextByteHeapAddrAfterMark + baAddr, fs);
          // // hack to find first above-the-mark ref
          // if (isMarkSet && baAddr < nextByteHeapAddrAfterMark) {
          // nextByteHeapAddrAfterMark = baAddr;
          // }
        }
          break;

        case Slot_ByteRef: {
          int baAddr = byteHeap.addByteArray(((ByteArray) fs)._getTheArray());
          heap.heap[i] = nextByteHeapAddrAfterMark + baAddr;
          byteAuxAddr2fsa.put(nextByteHeapAddrAfterMark + baAddr, fs);
          // // hack to find first above-the-mark ref
          // if (isMarkSet && baAddr < nextByteHeapAddrAfterMark) {
          // nextByteHeapAddrAfterMark = baAddr;
          // }
        }
          break;
        case Slot_ShortRef: {
          int saAddr = shortHeap.addShortArray(((ShortArray) fs)._getTheArray());
          heap.heap[i] = nextShortHeapAddrAfterMark + saAddr;
          shortAuxAddr2fsa.put(nextShortHeapAddrAfterMark + saAddr, fs);
        }
          break;

        case Slot_LongRef: {
          int laAddr = longHeap.addLongArray(((LongArray) fs)._getTheArray());
          heap.heap[i] = nextLongHeapAddrAfterMark + laAddr;
          longAuxAddr2fsa.put(nextLongHeapAddrAfterMark + laAddr, fs);
          break;
        }
        case Slot_DoubleRef: {
          int laAddr = longHeap.addDoubleArray(((DoubleArray) fs)._getTheArray());
          heap.heap[i] = nextLongHeapAddrAfterMark + laAddr;
          longAuxAddr2fsa.put(nextLongHeapAddrAfterMark + laAddr, fs);
          break;
        }
        case Slot_HeapRef:
          for (TOP fsitem : ((FSArray<?>) fs)._getTheArray()) {
            heap.heap[i++] = fs2addr.get(fsitem);
          }
          break;

        default:
          Misc.internalError();
      } // end of switch
    } else { // end of is-array
      if (fs instanceof UimaSerializable) {
        ((UimaSerializable) fs)._save_to_cas_data();
      }
      int i = pos + 1;
      for (FeatureImpl feat : type.getFeatureImpls()) {
        switch (feat.getSlotKind()) {
          case Slot_Boolean:
            heap.heap[i++] = fs._getBooleanValueNc(feat) ? 1 : 0;
            break;
          case Slot_Byte:
            heap.heap[i++] = fs._getByteValueNc(feat);
            break;
          case Slot_Short:
            heap.heap[i++] = fs._getShortValueNc(feat);
            break;
          case Slot_Int:
            heap.heap[i++] = fs._getIntValueNc(feat);
            break;
          case Slot_Float:
            heap.heap[i++] = CASImpl.float2int(fs._getFloatValueNc(feat));
            break;
          case Slot_LongRef: {
            int lAddr = longHeap.addLong(fs._getLongValueNc(feat));
            heap.heap[i++] = nextLongHeapAddrAfterMark + lAddr;
            break;
          }
          case Slot_DoubleRef: {
            int lAddr = longHeap.addLong(CASImpl.double2long(fs._getDoubleValueNc(feat)));
            heap.heap[i++] = nextLongHeapAddrAfterMark + lAddr;
            break;
          }
          case Slot_StrRef: {
            int sAddr = stringHeap.addString(fs._getStringValueNc(feat));
            heap.heap[i++] = (sAddr == 0) ? 0 : nextStringHeapAddrAfterMark + sAddr; // is 0 if
                                                                                     // string is
                                                                                     // null
            break;
          }
          case Slot_HeapRef:
            heap.heap[i++] = fs2addr.get(fs._getFeatureValueNc(feat));
            break;
          default:
            Misc.internalError();
        } // end of switch
      } // end of iter over all features
    } // end of if-is-not-array
  }

  // @formatter:off
  /**
   * Given the deserialized main heap, byte heap, short heap, long heap and string heap,
   *   a) create the corresponding FSs, populating a
   *   b) addr2fs    map, key = fsAddr, value = FS
   *   c) auxAddr2fs map, key = aux Array Start addr, 
   *                      value = FS corresponding to that primitive bool/byte/short/long/double array 
   * 
   * For some use cases, the byte / short / long heaps have not yet been initialized.
   *   - when data is available, deserialization will update the values in the fs directly
   * 
   * Each new fs created augments the addr2fs map.
   *   - forward fs refs are put into deferred update list  deferModFs
   * Each new fs created which is a Boolean/Byte/Short/Long/Double array updates auxAddr2fsa map
   *   if the aux data is not available (update is put on deferred list).   deferModByte  deferModShort  deferModLong
   * Each new fs created which has a slot referencing a long/double not yet read in creates a 
   *   deferred update specifying the fs, the slot, indexed by the addr in the aux table. 
   *      see deferModStr  deferModLong   deferModDouble   
   * Notes:  
   *   Subtypes of AnnotationBase created in the right view
   *     DocumentAnnotation - update out-of-indexes
   * 
   * FSs not subtypes of AnnotationBase are **all** associated with the initial view.
   * 
   *   Delta serialization: this routine adds just the new (above-the-line) FSs, and augments existing addr2fs and auxAddr2fsa
   */
  // @formatter:on
  private void createFSsFromHeaps(boolean isDelta, int startPos, CommonSerDesSequential csds) {
    final int heapsz = heap.getCellsUsed();
    final Int2ObjHashMap<TOP, TOP> addr2fs = csds.addr2fs;
    tsi = baseCas.getTypeSystemImpl();
    TOP fs;
    TypeImpl type;
    CASImpl initialView = baseCas.getInitialView(); // creates if needed

    List<Runnable> fixups4forwardFsRefs = new ArrayList<>();
    List<Runnable> fixups4UimaSerialization = new ArrayList<>();

    for (int heapIndex = startPos; heapIndex < heapsz; heapIndex += getFsSpaceReq(fs, type)) {
      // int typecode = heap.heap[heapIndex];
      // if (isBeforeV3 && typecode > TypeSystemConstants.lastBuiltinV2TypeCode) {
      // typecode = typecode + TypeSystemConstants.numberOfNewBuiltInsSinceV2;
      // }
      type = tsi.getTypeForCode(heap.heap[heapIndex]);
      if (type == null) {
        throw new CASRuntimeException(CASRuntimeException.deserialized_type_not_found,
                heap.heap[heapIndex]);
      }
      if (type.isArray()) {
        final int len = heap.heap[heapIndex + arrayLengthFeatOffset];

        fs = baseCas.createArray(type, len);
        csds.addFS(fs, heapIndex);

        if (len > 0) {
          final int bhi = heap.heap[heapIndex + arrayContentOffset];
          final int hhi = heapIndex + arrayContentOffset;

          switch (type.getComponentSlotKind()) {

            case Slot_BooleanRef: {
              boolean[] ba = ((BooleanArray) fs)._getTheArray();
              for (int ai = 0; ai < len; ai++) {
                ba[ai] = byteHeap.heap[bhi + ai] == (byte) 1;
              }
              break;
            }

            case Slot_ByteRef:
              System.arraycopy(byteHeap.heap, bhi, ((ByteArray) fs)._getTheArray(), 0, len);
              break;

            case Slot_ShortRef:
              System.arraycopy(shortHeap.heap, bhi, ((ShortArray) fs)._getTheArray(), 0, len);
              break;

            case Slot_LongRef:
              System.arraycopy(longHeap.heap, bhi, ((LongArray) fs)._getTheArray(), 0, len);
              break;

            case Slot_DoubleRef: {
              double[] da = ((DoubleArray) fs)._getTheArray();
              for (int ai = 0; ai < len; ai++) {
                da[ai] = CASImpl.long2double(longHeap.heap[bhi + ai]);
              }
              break;
            }

            case Slot_Int:
              System.arraycopy(heap.heap, hhi, ((IntegerArray) fs)._getTheArray(), 0, len);
              break;

            case Slot_Float: {
              float[] fa = ((FloatArray) fs)._getTheArray();
              for (int ai = 0; ai < len; ai++) {
                fa[ai] = CASImpl.int2float(heap.heap[hhi + ai]);
              }
              break;
            }

            case Slot_StrRef: {
              String[] sa = ((StringArray) fs)._getTheArray();
              for (int ai = 0; ai < len; ai++) {
                sa[ai] = stringHeap.getStringForCode(heap.heap[hhi + ai]);
              }
              break;
            }

            case Slot_HeapRef: {
              TOP[] fsa = ((FSArray<?>) fs)._getTheArray();
              for (int ai = 0; ai < len; ai++) {
                int a = heap.heap[hhi + ai];
                if (a == 0) {
                  continue;
                }
                TOP item = addr2fs.get(a);
                if (item != null) {
                  fsa[ai] = item;
                } else {
                  final int aiSaved = ai;
                  final int addrSaved = a;
                  fixups4forwardFsRefs.add(() -> {
                    fsa[aiSaved] = addr2fs.get(addrSaved);
                  });
                }
              }
              break;
            }

            default:
              Misc.internalError();

          } // end of switch
        }
      } else { // end of arrays
               // start of normal non-array
        CASImpl view = null;
        boolean isSofa = false;
        boolean documentAnnotationPreviouslyIndexed = false;
        if (type.isAnnotationBaseType()) {
          Sofa sofa = getSofaFromAnnotBase(heapIndex, stringHeap, addr2fs, csds); // creates sofa if
                                                                                  // needed and
                                                                                  // exists (forward
                                                                                  // ref case)
          view = (sofa == null) ? baseCas.getInitialView() : baseCas.getView(sofa);
          if (type == tsi.docType) {
            Annotation documentAnnotationPrevious = view.getDocumentAnnotationNoCreate();
            if (documentAnnotationPrevious == null) {
              // document annotation not present
              fs = view.createDocumentAnnotationNoRemoveNoIndex(0); // create but don't index
              view.set_deserialized_doc_annot_not_indexed((Annotation) fs); // for use by other code
                                                                            // that sets length, if
                                                                            // this is not indexed
              // documentAnnotationPreviouslyIndex == false, preset above
            } else {
              fs = documentAnnotationPrevious;
              // remove from Corruptable indexes, because we'll be updating it.
              view.removeFromCorruptableIndexAnyView(fs, view.getAddbackSingle());
              documentAnnotationPreviouslyIndexed = true;
            }
          } else {
            fs = view.createFS(type);
            if (fs instanceof UimaSerializable) {
              final UimaSerializable ufs = (UimaSerializable) fs;
              fixups4UimaSerialization.add(() -> ufs._init_from_cas_data());
            }
          }
        } else if (type == tsi.sofaType) {
          fs = makeSofaFromHeap(heapIndex, stringHeap, csds, SOFA_IN_NORMAL_ORDER); // creates Sofa
                                                                                    // if not
                                                                                    // already
                                                                                    // created due
                                                                                    // to
                                                                                    // annotationbase
                                                                                    // code above
          isSofa = true;
        } else {
          fs = initialView.createFS(type);
          if (fs instanceof UimaSerializable) {
            final UimaSerializable ufs = (UimaSerializable) fs;
            fixups4UimaSerialization.add(() -> ufs._init_from_cas_data());
          }
        }
        if (!isSofa) { // if it was a sofa, other code added or pended it
          csds.addFS(fs, heapIndex);
        }

        for (final FeatureImpl feat : type.getFeatureImpls()) {
          SlotKind slotKind = feat.getSlotKind();
          switch (slotKind) {
            case Slot_Boolean:
            case Slot_Byte:
            case Slot_Short:
            case Slot_Int:
            case Slot_Float:
              if (!isSofa || feat != tsi.sofaNum) {
                fs._setIntLikeValueNcNj(slotKind, feat, heapFeat(heapIndex, feat));
              }
              break;

            case Slot_LongRef:
              fs._setLongValueNcNj(feat, longHeap.heap[heapFeat(heapIndex, feat)]);
              break;
            case Slot_DoubleRef:
              fs._setDoubleValueNcNj(feat,
                      CASImpl.long2double(longHeap.heap[heapFeat(heapIndex, feat)]));
              break;
            case Slot_StrRef: {
              String s = stringHeap.getStringForCode(heapFeat(heapIndex, feat));
              if (updateStringFeature(fs, feat, s, fixups4forwardFsRefs)) {
                fs._setStringValueNcNj(feat, s);
              }
              break;
            }

            case Slot_HeapRef: {
              final TOP finalFs = fs;
              if (feat == tsi.annotBaseSofaFeat) {
                break; // already set
              }
              setFeatOrDefer(heapIndex, feat, fixups4forwardFsRefs, item -> {
                if (feat == tsi.sofaArray) {
                  ((Sofa) finalFs).setLocalSofaData(item);
                } else {
                  finalFs._setFeatureValueNcNj(feat, item);
                }
              }, addr2fs);
              break;
            }

            default:
              Misc.internalError();
          } // end of switch
        } // end of for-loop-over-all-features

        if (type == tsi.docType && documentAnnotationPreviouslyIndexed) {
          view.addbackSingle(fs);
        }
      } // end of non-array, normal fs
    } // end of loop over all fs in main array

    for (Runnable r : fixups4forwardFsRefs) {
      r.run();
    }

    for (Runnable r : fixups4UimaSerialization) {
      r.run();
    }
  }

  private void setFeatOrDefer(int heapIndex, FeatureImpl feat, List<Runnable> fixups4forwardFsRefs,
          Consumer<TOP> setter, Int2ObjHashMap<TOP, TOP> addr2fs) {
    int a = heapFeat(heapIndex, feat);
    if (a == 0) {
      return;
    }
    TOP item = addr2fs.get(a);
    if (item != null) {
      setter.accept(item);
    } else {
      fixups4forwardFsRefs.add(() -> setter.accept(addr2fs.get(a)));
    }
    // example of setter code
    // if (feat == tsi.sofaArray) {
    // ((Sofa)fs).setLocalSofaData(item);
    // } else {
    // fs.setFeatureValue(feat, item);
    // }
  }

  private int heapFeat(int nextFsAddr, FeatureImpl feat) {
    return heap.heap[nextFsAddr + 1 + feat.getOffset()];
  }

  private Sofa getSofaFromAnnotBase(int annotBaseAddr, StringHeap stringHeap2,
          Int2ObjHashMap<TOP, TOP> addr2fs, CommonSerDesSequential csds) {
    int sofaAddr = heapFeat(annotBaseAddr, tsi.annotBaseSofaFeat);
    if (0 == sofaAddr) {
      return null;
    }
    // get existing sofa or create sofa
    return makeSofaFromHeap(sofaAddr, stringHeap2, csds, SOFA_AHEAD_OF_NORMAL_ORDER);
  }

  private Sofa makeSofaFromHeap(int sofaAddr, StringHeap stringHeap2, CommonSerDesSequential csds,
          boolean isUnordered) {
    TOP sofa = csds.addr2fs.get(sofaAddr);
    if (sofa != null) {
      return (Sofa) sofa;
    }
    // create sofa
    int sofaNum = heapFeat(sofaAddr, tsi.sofaNum);
    String sofaName = stringHeap2.getStringForCode(heapFeat(sofaAddr, tsi.sofaId));
    sofa = baseCas.createSofa(sofaNum, sofaName, null);
    if (isUnordered) {
      csds.addFSunordered(sofa, sofaAddr);
    } else {
      csds.addFS(sofa, sofaAddr);
    }
    return (Sofa) sofa;
  }

  // @formatter:off
  /**
   * Doing updates for delta cas for existing objects.
   * Cases:
   *   - item in heap-stored-array = update the corresponding item in the FS
   *   - non-ref in feature slot - update the corresponding feature
   *   - ref (to long/double value, to string)
   *       -- these always reference entries in long/string tables that are new (above the line)
   *       -- these have already been deserialized
   *   - ref (to main heap) - can update this directly       
   *   NOTE: entire aux arrays never have their refs to the aux heaps updated, for 
   *           arrays of boolean, byte, short, long, double
   *   NOTE: Slot updates for FS refs always point to addr which are in the addr2fs table or are 0 (null),
   *           because if the ref is to a new one, those have been already deserialized by this point, and
   *                   if the ref is to a below-the-line one, those are already put into the addr2fs table
   * @param bds - helper data
   * @param slotAddr - the main heap slot addr being updated
   * @param slotValue - the new value
   */
  // @formatter:on
  private void updateHeapSlot(BinDeserSupport bds, int slotAddr, int slotValue,
          Int2ObjHashMap<TOP, TOP> addr2fs) {
    TOP fs = bds.fs;
    TypeImpl type = fs._getTypeImpl();
    if (type.isArray()) {
      // only heap stored arrays have mod updates.
      final int hsai = slotAddr - bds.fsStartAddr - arrayContentOffset; // heap stored array index
      switch (type.getComponentSlotKind()) {
        // heap stored arrays
        case Slot_Int:
          ((IntegerArray) fs).set(hsai, slotValue);
          break;
        case Slot_Float:
          ((FloatArray) fs).set(hsai, CASImpl.int2float(slotValue));
          break;
        case Slot_StrRef:
          ((StringArray) fs).set(hsai, stringHeap.getStringForCode(slotValue));
          break;
        case Slot_HeapRef:
          ((FSArray) fs).set(hsai, addr2fs.get(slotValue));
          break;
        default:
          Misc.internalError();
      } // end of switch for component type of arrays
    } else { // end of arrays
      // is plain fs with fields
      final int offset0 = slotAddr - bds.fsStartAddr - 1; // 0 based offset of feature, -1 for type
                                                          // code word
      FeatureImpl feat = type.getFeatureImpls()[offset0];
      SlotKind slotKind = feat.getSlotKind();
      switch (slotKind) {
        case Slot_Boolean:
        case Slot_Byte:
        case Slot_Short:
        case Slot_Int:
        case Slot_Float:
          fs._setIntLikeValue(slotKind, feat, slotValue);
          break;

        case Slot_LongRef:
          fs.setLongValue(feat, longHeap.getHeapValue(slotValue));
          break;
        case Slot_DoubleRef:
          fs.setDoubleValue(feat, CASImpl.long2double(longHeap.getHeapValue(slotValue)));
          break;
        case Slot_StrRef: {
          String s = stringHeap.getStringForCode(slotValue);
          if (updateStringFeature(fs, feat, s, null)) {
            fs.setStringValue(feat, stringHeap.getStringForCode(slotValue));
          }
          break;
        }

        case Slot_HeapRef:
          fs.setFeatureValue(feat, addr2fs.get(slotValue));
          break;
        default:
          Misc.internalError();
      }
    }
  }

  /**
   * @return true if caller needs to do an appropriate fs._setStringValue...
   */
  private boolean updateStringFeature(TOP fs, FeatureImpl feat, String s,
          List<Runnable> fixups4forwardFsRefs) {
    if (null == s) {
      return false; // null is the default value, no need to set it
    }
    if (fs instanceof Sofa) {
      if (feat == tsi.sofaId) {
        return false; // do nothing, this value was already used
      }
      Sofa sofa = (Sofa) fs;
      if (feat == tsi.sofaMime) {
        sofa.setMimeType(s);
        return false;
      }
      if (feat == tsi.sofaUri) {
        sofa.setRemoteSofaURI(s);
        return false;
      }
      if (feat == tsi.sofaString) {
        if (fixups4forwardFsRefs != null) {
          // has to be deferred because it updates docAnnot which might not be deser yet.
          // TODO no longer needed, calls the version which doesn't update docAnnot 9/2017
          Sofa capturedSofa = sofa;
          String capturedString = s;
          fixups4forwardFsRefs
                  .add(() -> capturedSofa.setLocalSofaDataNoDocAnnotUpdate(capturedString));
        } else {
          sofa.setLocalSofaData(s);
        }
        return false;
      }
    }
    return true; // fs._setStringValueNcNj(feat, s);
  }

  CASImpl getCas() {
    return baseCas;
  }

  private void clearDeltaOffsets() {
    nextHeapAddrAfterMark = 0;
    nextStringHeapAddrAfterMark = 0;
    nextByteHeapAddrAfterMark = 0;
    nextShortHeapAddrAfterMark = 0;
    nextLongHeapAddrAfterMark = 0;
  }

  private void clearAuxAddr2fsa() {
    byteAuxAddr2fsa.clear();
    shortAuxAddr2fsa.clear();
    longAuxAddr2fsa.clear();
  }

  /**
   * called by cas reset
   */
  public void clear() {
    clearDeltaOffsets();
    clearAuxAddr2fsa();
    heap = null;
    byteHeap = null;
    shortHeap = null;
    longHeap = null;
    stringHeap = null;
    isBeforeV3 = false;
  }
}