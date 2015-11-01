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
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl.BinDeserSupport;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Binary (mostly non compressed) CAS serialization and deserialization
 */
public class BinaryCasSerDes {

  public BinaryCasSerDes() {
    // TODO Auto-generated constructor stub
  }
  
  public void reinit(CASSerializer ser) {
    if (this != this.svd.baseCAS) {
      this.svd.baseCAS.reinit(ser);
      return;
    }
    this.resetNoQuestions();
    reinit(ser.getHeapMetadata(), ser.getHeapArray(), ser.getStringTable(), ser.getFSIndex(), ser
        .getByteArray(), ser.getShortArray(), ser.getLongArray());
  }

  void reinit(int[] heapMetadata, int[] heapArray, String[] stringTable, int[] fsIndex,
      byte[] byteHeapArray, short[] shortHeapArray, long[] longHeapArray) {
    createStringTableFromArray(stringTable);
    this.getHeap().reinit(heapMetadata, heapArray);
    if (byteHeapArray != null) {
      this.getByteHeap().reinit(byteHeapArray);
    }
    if (shortHeapArray != null) {
      this.getShortHeap().reinit(shortHeapArray);
    }
    if (longHeapArray != null) {
      this.getLongHeap().reinit(longHeapArray);
    }

    reinitIndexedFSs(fsIndex);
  }
  
  void reinit(int[] heapMetadata, int[] heapArray, String[] stringTable, int[] fsIndex,
      byte[] byteHeapArray, short[] shortHeapArray, long[] longHeapArray) {
    createStringTableFromArray(stringTable);
    this.getHeap().reinit(heapMetadata, heapArray);
    if (byteHeapArray != null) {
      this.getByteHeap().reinit(byteHeapArray);
    }
    if (shortHeapArray != null) {
      this.getShortHeap().reinit(shortHeapArray);
    }
    if (longHeapArray != null) {
      this.getLongHeap().reinit(longHeapArray);
    }

    reinitIndexedFSs(fsIndex);
  }

  /* *********************************
   *      D e s e r i a l i z e r s 
   ***********************************/
  
  public void reinit(CASCompleteSerializer casCompSer) {
    if (this != this.svd.baseCAS) {
      this.svd.baseCAS.reinit(casCompSer);
      return;
    }
    TypeSystemImpl ts = casCompSer.getCASMgrSerializer().getTypeSystem();
    this.svd.casMetadata = ts.casMetadata;
    this.tsi = null; // reset cache
    commitTypeSystem();

    // reset index repositories -- wipes out Sofa index
    this.indexRepository = casCompSer.getCASMgrSerializer().getIndexRepository(this);
    this.indexRepository.commit();

    // get handle to existing initial View
    CAS initialView = this.getInitialView();

    // throw away all other View information as the CAS definition may have
    // changed
    this.svd.sofa2indexMap.clear();
    this.svd.sofaNbr2ViewMap.clear();
    this.svd.viewCount = 0;

    // freshen the initial view
    ((CASImpl) initialView).refreshView(this.svd.baseCAS, null);
    setViewForSofaNbr(1, initialView);
    this.svd.viewCount = 1;

    // deserialize heap
    CASSerializer casSer = casCompSer.getCASSerializer();
    reinit(casSer.getHeapMetadata(), casSer.getHeapArray(), casSer.getStringTable(), casSer
        .getFSIndex(), casSer.getByteArray(), casSer.getShortArray(), casSer.getLongArray());

    // we also need to throw away the JCAS. A new JCAS will be created on
    // the next
    // call to getJCas(). As with the CAS, we are counting on the fact that
    // this happens only in a service, where JCAS handles are not held on
    // to.
    this.jcas = null;
    // this.sofa2jcasMap.clear();
    
    clearTrackingMarks();
  }

  /**
   * Binary Deserializaion Support
   * An instance of this class is made for every reinit operation 
   *
   */
  private class BinDeserSupport {
    
    private int fsStartAddr;
    private int fsEndAddr;
    /**
     * An array of all the starting indexes of the FSs on the old/prev heap
     * (below the mark, for delta CAS, plus one last one (one beyond the end)
     */
    private int[] fssAddrArray;
    private int fssIndex;
    private int lastRemovedFsAddr;
    // feature codes - there are exactly the same number as their are features
    private int[] featCodes;
    private FSsTobeAddedback tobeAddedback = FSsTobeAddedback.createSingle();
  }
    
  /**
   * --------------------------------------------------------------------- see
   * Blob Format in CASSerializer
   * 
   * This reads in and deserializes CAS data from a stream. Byte swapping may be
   * needed if the blob is from C++ -- C++ blob serialization writes data in
   * native byte order.
   * 
   * @param istream -
   * @return -
   * @throws CASRuntimeException wraps IOException
   */

  public SerialFormat reinit(InputStream istream) throws CASRuntimeException {
    if (this != this.svd.baseCAS) {
      return this.svd.baseCAS.reinit(istream);
    }
   
    final DataInputStream dis = (istream instanceof DataInputStream) ?  
       (DataInputStream) istream : new DataInputStream(istream);

    final BinDeserSupport bds = new BinDeserSupport();
    try {
      // key
      // determine if byte swap if needed based on key
      byte[] bytebuf = new byte[4];
      bytebuf[0] = dis.readByte(); // U
      bytebuf[1] = dis.readByte(); // I
      bytebuf[2] = dis.readByte(); // M
      bytebuf[3] = dis.readByte(); // A

      final boolean swap = (bytebuf[0] != 85);

      // version      
      // version bit in 2's place indicates this is in delta format.
      final int version = readInt(dis, swap);      
      final boolean delta = ((version & 2) == 2);
      
      if (!delta) {
        this.resetNoQuestions();
      }
      
      if (0 != (version & 4)) {
        final int compressedVersion = readInt(dis, swap);
        if (compressedVersion == 0) {
          (new BinaryCasSerDes4(this.getTypeSystemImpl(), false)).deserialize(this, dis, delta);
          return SerialFormat.COMPRESSED;
        } else {
//          throw new CASRuntimeException(CASRuntimeException.DESERIALIZING_COMPRESSED_BINARY_UNSUPPORTED);
          // Only works for cases where the type systems match, and delta is false.
          try {
            (new BinaryCasSerDes6(this)).deserializeAfterVersion(dis, delta, AllowPreexistingFS.allow);
          } catch (ResourceInitializationException e) {
            throw new CASRuntimeException(CASRuntimeException.DESERIALIZING_COMPRESSED_BINARY_UNSUPPORTED, null, e);
          }
          return SerialFormat.COMPRESSED_FILTERED;
        }
      }
      
      // main fsheap
      final int fsheapsz = readInt(dis, swap);
      
      int startPos = 0;
      if (!delta) {
        this.getHeap().reinitSizeOnly(fsheapsz);
      } else {
      startPos = this.getHeap().getNextId();
      this.getHeap().grow(fsheapsz);
      }
            
      // add new heap slots
      for (int i = startPos; i < fsheapsz+startPos; i++) {
        this.getHeap().heap[i] = readInt(dis, swap);
      }
      
      // string heap
      int stringheapsz = readInt(dis, swap);

      final StringHeapDeserializationHelper shdh = new StringHeapDeserializationHelper();
      
      shdh.charHeap = new char[stringheapsz];
      for (int i = 0; i < stringheapsz; i++) {
        shdh.charHeap[i] = (char) readShort(dis, swap);
      }
      shdh.charHeapPos = stringheapsz;

      // word alignment
      if (stringheapsz % 2 != 0) {
        dis.readChar();
      }

      // string ref heap
      int refheapsz = readInt(dis, swap);

      refheapsz--;
      refheapsz = refheapsz / 2;
      refheapsz = refheapsz * 3;

      // read back into references consisting to three ints
      // --stringheap offset,length, stringlist offset
      shdh.refHeap = new int[StringHeapDeserializationHelper.FIRST_CELL_REF + refheapsz];

      dis.readInt(); // 0
      for (int i = shdh.refHeapPos; i < shdh.refHeap.length; i += StringHeapDeserializationHelper.REF_HEAP_CELL_SIZE) {
        shdh.refHeap[i + StringHeapDeserializationHelper.CHAR_HEAP_POINTER_OFFSET] = readInt(dis, swap);
        shdh.refHeap[i + StringHeapDeserializationHelper.CHAR_HEAP_STRLEN_OFFSET] = readInt(dis, swap);
        shdh.refHeap[i + StringHeapDeserializationHelper.STRING_LIST_ADDR_OFFSET] = 0;
      }
      shdh.refHeapPos = refheapsz + StringHeapDeserializationHelper.FIRST_CELL_REF;
      
      this.getStringHeap().reinit(shdh, delta);
      
      //if delta, handle modified fs heap cells
      if (delta) {
        
        final int heapsize = this.getHeap().getNextId();
        
        // compute table of ints which correspond to FSs in the existing heap
        // we need this because the list of modifications is just arbitrary single words on the heap
        //   at arbitrary boundaries
        IntVector fss = new IntVector(Math.max(128, heapsize >> 6));
        int fsAddr;
        for (fsAddr = 1; fsAddr < heapsize; fsAddr = getNextFsHeapAddr(fsAddr)) {          
          fss.add(fsAddr);
        }
        fss.add(fsAddr);  // add trailing value
        bds.fssAddrArray = fss.toArray();
        
        int fsmodssz = readInt(dis, swap);
        bds.fsStartAddr = -1;        
        
        // loop over all heap modifications to existing FSs
        
        // first disable auto addbacks for index corruption - this routine is handling that
        svd.fsTobeAddedbackSingleInUse = true;  // sorry, a bad hack...
        try {
          for (int i = 0; i < fsmodssz; i++) {
            final int heapAddrBeingModified = readInt(dis, swap);
            maybeAddBackAndRemoveFs(heapAddrBeingModified, bds);       
            this.getHeap().heap[heapAddrBeingModified] = readInt(dis, swap);
          }
          bds.tobeAddedback.addback(bds.lastRemovedFsAddr);
          bds.fssAddrArray = null;  // free storage
        } finally {
          svd.fsTobeAddedbackSingleInUse = false;
        }
      }

      // indexed FSs
      int fsindexsz = readInt(dis, swap);
      int[] fsindexes = new int[fsindexsz];
      for (int i = 0; i < fsindexsz; i++) {
        fsindexes[i] = readInt(dis, swap);
      }

      // build the index
      if (delta) {
      reinitDeltaIndexedFSs(fsindexes);  
      } else {
        reinitIndexedFSs(fsindexes);
      }
      
      // byte heap
      int heapsz = readInt(dis, swap);

      if (!delta) {
        this.getByteHeap().heap = new byte[Math.max(16, heapsz)]; // must be > 0
        dis.readFully(this.getByteHeap().heap, 0, heapsz);
        this.getByteHeap().heapPos = heapsz;
      }  else {
        for (int i=0; i < heapsz; i++) {
          this.getByteHeap().addByte(dis.readByte());
        }
      }
      // word alignment
      int align = (4 - (heapsz % 4)) % 4;
      BinaryCasSerDes6.skipBytes(dis, align);

      // short heap
      heapsz = readInt(dis, swap);
      
      if (!delta) {
        this.getShortHeap().heap = new short[Math.max(16, heapsz)]; // must be > 0
        for (int i = 0; i < heapsz; i++) {
          this.getShortHeap().heap[i] = readShort(dis, swap);
        }
        this.getShortHeap().heapPos = heapsz;
      } else {
        for (int i = 0; i < heapsz; i++) {
          this.getShortHeap().addShort(readShort(dis, swap));
        }
      }
      // word alignment
      if (heapsz % 2 != 0) {
        dis.readShort();
      }

      // long heap
      heapsz = readInt(dis, swap);
      
      if (!delta) {
        this.getLongHeap().heap = new long[Math.max(16, heapsz)]; // must be > 0
        for (int i = 0; i < heapsz; i++) {
          this.getLongHeap().heap[i] = readLong(dis, swap);
        }
        this.getLongHeap().heapPos = heapsz;
      } else {
        for (int i = 0; i < heapsz; i++) {
          this.getLongHeap().addLong(readLong(dis, swap));
        }
      }
      
      if (delta)  {
          //modified Byte Heap
        heapsz = readInt(dis, swap);
        if (heapsz > 0) {
          int[] heapAddrs = new int[heapsz];
          for (int i = 0; i < heapsz; i++) {
            heapAddrs[i] = readInt(dis, swap);
          }
          for (int i = 0; i < heapsz; i++) {
            this.getByteHeap().heap[heapAddrs[i]] = dis.readByte();
          }
        }
        // word alignment
        align = (4 - (heapsz % 4)) % 4;
        BinaryCasSerDes6.skipBytes(dis, align);
        
        //modified Short Heap
        heapsz = readInt(dis, swap);
        if (heapsz > 0) {
          int[] heapAddrs = new int[heapsz];
          for (int i = 0; i < heapsz; i++) {
            heapAddrs[i] = readInt(dis, swap);
          }
          for (int i = 0; i < heapsz; i++) {
            this.getShortHeap().heap[heapAddrs[i]] = readShort(dis, swap);
          }
        }
        
        // word alignment
        if (heapsz % 2 != 0) {
          dis.readShort();
        }
      
        //modified Long Heap
        heapsz = readInt(dis, swap);
        if (heapsz > 0) {
          int[] heapAddrs = new int[heapsz];
          for (int i = 0; i < heapsz; i++) {
            heapAddrs[i] = readInt(dis, swap);
          }
          for (int i = 0; i < heapsz; i++) {
            this.getLongHeap().heap[heapAddrs[i]] = readLong(dis, swap);
          }
        }
      } // of delta - modified processing
    } catch (IOException e) {
      String msg = e.getMessage();
      if (msg == null) {
        msg = e.toString();
      }
      CASRuntimeException exception = new CASRuntimeException(
          CASRuntimeException.BLOB_DESERIALIZATION, new String[] { msg });
      throw exception;
    }
    return SerialFormat.BINARY;
  }

  void reinitIndexedFSs(int[] fsIndex) {
    // Add FSs to index repository for base CAS
    int numViews = fsIndex[0];
    int loopLen = fsIndex[1]; // number of sofas, not necessarily the same as
    // number of views
    // because the initial view may not have a sofa
    for (int i = 2; i < loopLen + 2; i++) { // iterate over all the sofas,
      this.indexRepository.addFS(fsIndex[i]); // add to base index
    }
    int loopStart = loopLen + 2;

    FSIterator<SofaFS> iterator = this.svd.baseCAS.getSofaIterator();
    final Feature idFeat = getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFAID);
    // Add FSs to index repository for each View
    while (iterator.isValid()) {
      SofaFS sofa = iterator.get();
      String id = ll_getStringValue(((FeatureStructureImpl) sofa).getAddress(),
          ((FeatureImpl) idFeat).getCode());
      if (CAS.NAME_DEFAULT_SOFA.equals(id)) {
        this.registerInitialSofa();
        this.svd.sofaNameSet.add(id);
      }
      // next line the getView as a side effect
      // checks for dupl sofa name, and if not,
      // adds the name to the sofaNameSet
      ((CASImpl) this.getView(sofa)).registerView(sofa);

      iterator.moveToNext();
    }
    getInitialView();  // done for side effect of creating the initial view.
    // must be done before the next line, because it sets the
    // viewCount to 1.
    this.svd.viewCount = numViews; // total number of views
    
    for (int viewNbr = 1; viewNbr <= numViews; viewNbr++) {
      CAS view = (viewNbr == 1) ? getInitialView() : getView(viewNbr);
      if (view != null) {
        FSIndexRepositoryImpl loopIndexRep = (FSIndexRepositoryImpl) getSofaIndexRepository(viewNbr);
        loopLen = fsIndex[loopStart];
        for (int i = loopStart + 1; i < loopStart + 1 + loopLen; i++) {
          loopIndexRep.addFS(fsIndex[i]);
        }
        loopStart += loopLen + 1;
          ((CASImpl) view).updateDocumentAnnotation();
      } else {
        loopStart += 1;
      }
    }
  }
  
  /**
   * Adds the SofaFSs to the base view
   * Assumes "cas" refers to the base cas
   *
   * Processes "adds", "removes" and "reindexes" for all views
   *
   * @param fsIndex - array of fsRefs and counts, for sofas, and all views
   */
  void reinitDeltaIndexedFSs(int[] fsIndex) {
    assert(this.svd.baseCAS == this);  
    // Add Sofa FSs to index repository for base CAS
    int numViews = fsIndex[0]; // total number of views
    int loopLen = fsIndex[1]; // number of sofas, not necessarily the same as number of views (initial view could be missing a Sofa)
    // add Sofa FSs to base view number of views. Should only contain new Sofas.
    for (int i = 2; i < loopLen + 2; i++) { // iterate over all the sofas,
      this.indexRepository.addFS(fsIndex[i]); // add to base index
    }
    int loopStart = loopLen + 2;

    FSIterator<SofaFS> iterator = this.getSofaIterator();
    final int idFeatCode = ((FeatureImpl)getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFAID)).getCode();
    
    // Register all Sofas
    while (iterator.isValid()) {
      SofaFS sofa = iterator.get();
      String id = ll_getStringValue(((FeatureStructureImpl) sofa).getAddress(), idFeatCode);
      if (CAS.NAME_DEFAULT_SOFA.equals(id)) {
        this.registerInitialSofa();
        this.svd.sofaNameSet.add(id);
      }
      // next line the getView as a side effect
      // checks for dupl sofa name, and if not,
      // adds the name to the sofaNameSet
      ((CASImpl) this.getView(sofa)).registerView(sofa);

      iterator.moveToNext();
    }
    
    this.svd.viewCount = numViews; // total number of views

    for (int viewNbr = 1; viewNbr <= numViews; viewNbr++) {
      CAS view = (viewNbr == 1) ? getInitialView() : getView(viewNbr);
      if (view != null) {
        
        // for all views
        
        FSIndexRepositoryImpl loopIndexRep = (FSIndexRepositoryImpl) getSofaIndexRepository(viewNbr);
        loopLen = fsIndex[loopStart];
        
        // add FSs to index
        
        for (int i = loopStart + 1; i < loopStart + 1 + loopLen; i++) {
          loopIndexRep.addFS(fsIndex[i]);
        }
        
        // remove FSs from indexes
        
        loopStart += loopLen + 1;
        loopLen = fsIndex[loopStart];
        for (int i = loopStart + 1; i < loopStart + 1 + loopLen; i++) {
          loopIndexRep.removeFS(fsIndex[i]);
        }
        
        // skip the reindex - this isn't done here https://issues.apache.org/jira/browse/UIMA-4100
        // but we need to run the loop to read over the items in the input stream
        loopStart += loopLen + 1;
        loopLen = fsIndex[loopStart];
//        for (int i = loopStart + 1; i < loopStart + 1 + loopLen; i++) {
//          loopIndexRep.removeFS(fsIndex[i]);
//          loopIndexRep.addFS(fsIndex[i]);
//        }
        loopStart += loopLen + 1;
        ((CASImpl) view).updateDocumentAnnotation();
      } else {
        loopStart += 1;
      }
    }
  }

  // IndexedFSs format:
  // number of views
  // number of sofas
  // [sofa-1 ... sofa-n]
  // number of FS indexed in View1
  // [FS-1 ... FS-n]
  // etc.
  int[] getIndexedFSs() {
    IntVector v = new IntVector();
    int[] fsLoopIndex;

    int numViews = getBaseSofaCount();
    v.add(numViews);

    // Get indexes for base CAS
    Stream<FeatureStructure> indexedFSs = getBaseCAS().indexRepository.getIndexedFSs();
    
    
    fsLoopIndex = this.svd.baseCAS.indexRepository.getIndexedFSs();
    v.add(fsLoopIndex.length);
    v.add(fsLoopIndex, 0, fsLoopIndex.length);
//    for (int k = 0; k < fsLoopIndex.length; k++) {
//      v.add(fsLoopIndex[k]);
//    }

    // Get indexes for each SofaFS in the CAS
    for (int sofaNum = 1; sofaNum <= numViews; sofaNum++) {
      FSIndexRepositoryImpl loopIndexRep = (FSIndexRepositoryImpl) this.svd.baseCAS
          .getSofaIndexRepository(sofaNum);
      if (loopIndexRep != null) {
        fsLoopIndex = loopIndexRep.getIndexedFSs();
      } else {
        fsLoopIndex = INT0;
      }
      v.add(fsLoopIndex.length);
      for (int k = 0; k < fsLoopIndex.length; k++) {
        v.add(fsLoopIndex[k]);
      }
    }
    return v.toArray();
  }
  
 
  
  //Delta IndexedFSs format:
  // number of views
  // number of sofas - new
  // [sofa-1 ... sofa-n]
  // number of new FS add in View1
  // [FS-1 ... FS-n]
  // number of  FS removed from View1
  // [FS-1 ... FS-n]
  //number of  FS reindexed in View1
  // [FS-1 ... FS-n]
  // etc.
  int[] getDeltaIndexedFSs(MarkerImpl mark) {
    IntVector v = new IntVector();
    int[] fsLoopIndex;
    int[] fsDeletedFromIndex;
    int[] fsReindexed;

    int numViews = getBaseSofaCount();
    v.add(numViews);

    // Get indexes for base CAS
    fsLoopIndex = this.svd.baseCAS.indexRepository.getIndexedFSs();
    // Get the new Sofa FS
    IntVector newSofas = new IntVector();
    for (int k = 0; k < fsLoopIndex.length; k++) {
      if ( mark.isNew(fsLoopIndex[k]) ) {
        newSofas.add(fsLoopIndex[k]);
      }
    }
    
    v.add(newSofas.size());
    v.add(newSofas.getArray(), 0, newSofas.size());
//    for (int k = 0; k < newSofas.size(); k++) {
//      v.add(newSofas.get(k));
//    }

    // Get indexes for each view in the CAS
    for (int sofaNum = 1; sofaNum <= numViews; sofaNum++) {
      FSIndexRepositoryImpl loopIndexRep = (FSIndexRepositoryImpl) this.svd.baseCAS
          .getSofaIndexRepository(sofaNum);
      if (loopIndexRep != null) {
        fsLoopIndex = loopIndexRep.getAddedFSs();
        fsDeletedFromIndex = loopIndexRep.getDeletedFSs();
        fsReindexed = loopIndexRep.getReindexedFSs();
      } else {
        fsLoopIndex = INT0;
        fsDeletedFromIndex = INT0;
        fsReindexed = INT0;
      }
      v.add(fsLoopIndex.length);
      v.add(fsLoopIndex, 0, fsLoopIndex.length);
//      for (int k = 0; k < fsLoopIndex.length; k++) {
//        v.add(fsLoopIndex[k]);
//      }
      v.add(fsDeletedFromIndex.length);
      v.add(fsDeletedFromIndex, 0, fsDeletedFromIndex.length);
//      for (int k = 0; k < fsDeletedFromIndex.length; k++) {
//        v.add(fsDeletedFromIndex[k]);
//      }
      v.add(fsReindexed.length);
      v.add(fsReindexed, 0, fsReindexed.length);
//      for (int k = 0; k < fsReindexed.length; k++) {
//        v.add(fsReindexed[k]);
//      }
    }
    return v.toArray();
  }

  void createStringTableFromArray(String[] stringTable) {
    // why a new heap instead of reseting the old one???
    // this.stringHeap = new StringHeap();
    this.getStringHeap().reset();
    for (int i = 1; i < stringTable.length; i++) {
      this.getStringHeap().addString(stringTable[i]);
    }
  }

  /**
   * for Deserialization of Delta, when updating existing FSs,
   * If the heap addr is for the next FS, re-add the previous one to those indexes where it was removed,
   * and then maybe remove the new one (and remember which views to re-add to).
   * @param heapAddr
   */
  private void maybeAddBackAndRemoveFs(int heapAddr, final BinDeserSupport bds) {
    if (bds.fsStartAddr == -1) {
      bds.fssIndex = -1;
      bds.lastRemovedFsAddr = -1;
      bds.tobeAddedback.clear();
    }
    findCorrespondingFs(heapAddr, bds); // sets fsStartAddr, end addr
    if (bds.lastRemovedFsAddr != bds.fsStartAddr) {
      bds.tobeAddedback.addback(bds.lastRemovedFsAddr);
      if (bds.featCodes.length == 0) {
        // is array
        final int typeCode = getTypeCode(bds.fsStartAddr);
        assert(getTypeSystemImpl().ll_isArrayType(typeCode));
      } else {
        int featCode = bds.featCodes[heapAddr - (bds.fsStartAddr + 1)];
        removeFromCorruptableIndexAnyView(bds.lastRemovedFsAddr = bds.fsStartAddr, bds.tobeAddedback, featCode);
      }
    }
  }

  private void findCorrespondingFs(int heapAddr, final BinDeserSupport bds) {
    if (bds.fsStartAddr < heapAddr && heapAddr < bds.fsEndAddr) {
      return;
    }
    
    // search forward by 1 before doing binary search
    bds.fssIndex ++;  // incrementing dense index into fssAddrArray for start addrs
    bds.fsStartAddr = bds.fssAddrArray[bds.fssIndex];  // must exist
    if (bds.fssIndex + 1 < bds.fssAddrArray.length) { // handle edge case where prev was at the end
      bds.fsEndAddr = bds.fssAddrArray[bds.fssIndex + 1];  // must exist
      if (bds.fsStartAddr < heapAddr && heapAddr < bds.fsEndAddr) {
        bds.featCodes = getTypeSystemImpl().ll_getAppropriateFeatures(getTypeCode(bds.fsStartAddr));
        return;
      }
    }
    
    int result;
    if (heapAddr > bds.fsEndAddr) {
      // item is higher
      result = Arrays.binarySearch(bds.fssAddrArray, bds.fssIndex + 1, bds.fssAddrArray.length, heapAddr);
    } else {
      result = Arrays.binarySearch(bds.fssAddrArray,  0, bds.fssIndex - 1, heapAddr);
    }
    
    // result must be negative - should never modify a type code slot
    assert (result < 0);
    bds.fssIndex = (-result) - 2;
    bds.fsStartAddr = bds.fssAddrArray[bds.fssIndex];
    bds.fsEndAddr = bds.fssAddrArray[bds.fssIndex + 1];
    bds.featCodes = getTypeSystemImpl().ll_getAppropriateFeatures(getTypeCode(bds.fsStartAddr));  
    assert(bds.fsStartAddr < heapAddr && heapAddr < bds.fsEndAddr);
  }
  
  private int getNextFsHeapAddr(int fsAddr) {
    final TypeSystemImpl tsi = getTypeSystemImpl();
    final int typeCode = getTypeCode(fsAddr);
    final Type type = tsi.ll_getTypeForCode(typeCode);
    //debug
//    if (tsi.ll_getTypeForCode(typeCode) == null) {
//      System.out.println("debug, typeCode = "+ typeCode);
//    }
    final boolean isHeapStoredArray = (typeCode == TypeSystemImpl.intArrayTypeCode) || (typeCode == TypeSystemImpl.floatArrayTypeCode)
        || (typeCode == TypeSystemImpl.fsArrayTypeCode) || (typeCode == TypeSystemImpl.stringArrayTypeCode)
        || (TypeSystemImpl.isArrayTypeNameButNotBuiltIn(type.getName()));
    if (isHeapStoredArray) {
      return fsAddr + 2 + getHeapValue(fsAddr + 1);
    } else if (type.isArray()) {
      return fsAddr + 3;  // for the aux ref and the length
    } else {
      return fsAddr + this.svd.casMetadata.fsSpaceReq[typeCode];
    }    
  }
  
  private long readLong(DataInputStream dis, boolean swap) throws IOException {
    long v = dis.readLong();
    return swap ? Long.reverseBytes(v) : v;
  }
  
  private int readInt(DataInputStream dis, boolean swap) throws IOException {
    int v = dis.readInt();
    return swap ? Integer.reverseBytes(v) : v;
  }
  
  private short readShort(DataInputStream dis, boolean swap) throws IOException {
    short v = dis.readShort();
    return swap ? Short.reverseBytes(v) : v;
  }

//  private long swap8(DataInputStream dis, byte[] buf) throws IOException {
//
//    buf[7] = dis.readByte();
//    buf[6] = dis.readByte();
//    buf[5] = dis.readByte();
//    buf[4] = dis.readByte();
//    buf[3] = dis.readByte();
//    buf[2] = dis.readByte();
//    buf[1] = dis.readByte();
//    buf[0] = dis.readByte();
//    ByteBuffer bb = ByteBuffer.wrap(buf);
//    return bb.getLong();
//  }
//
//  private int swap4(DataInputStream dis, byte[] buf) throws IOException {
//    buf[3] = dis.readByte();
//    buf[2] = dis.readByte();
//    buf[1] = dis.readByte();
//    buf[0] = dis.readByte();
//    ByteBuffer bb = ByteBuffer.wrap(buf);
//    return bb.getInt();
//  }
//
//  private char swap2(DataInputStream dis, byte[] buf) throws IOException {
//    buf[1] = dis.readByte();
//    buf[0] = dis.readByte();
//    ByteBuffer bb = ByteBuffer.wrap(buf, 0, 2);
//    return bb.getChar();
//  }

}
