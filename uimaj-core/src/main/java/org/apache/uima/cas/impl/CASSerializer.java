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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.apache.uima.UimaSerializable;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.Marker;
import org.apache.uima.cas.impl.CASImpl.FsChange;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.Obj2IntIdentityHashMap;
import org.apache.uima.internal.util.function.Consumer_T_withIOException;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.util.CasIOUtils;

/**
 * This object has 2 purposes.
 *   - it can hold a collection of individually Java-object-serializable objects representing a CAS +
 *     the list of FS's indexed in the CAS
 *     
 *   - it has special methods (versions of addCAS) to do a custom binary serialization (no compression) of a CAS + lists
 *     of its indexed FSs.  
 * 
 * One use of this class follows this form:
 * 
 * 1) create an instance of this class
 * 2) add a Cas to it (via addCAS methods)
 * 3) use the instance of this class as the argument to anObjectOutputStream.writeObject(anInstanceOfThisClass)
 *    In UIMA this is done in the SerializationUtils class; it appears to be used for SOAP and Vinci service adapters.
 * 
 * There are also custom serialization methods that serialize to outputStreams.
 * 
 * The format of the serialized data is in one of several formats:
 *   normal Java object serialization / custom binary serialization
 *
 *   The custom binary serialization is in several formats:
 *     full / delta:
 *       full - the entire cas
 *       delta - only differences from a previous "mark" are serialized
 *       
 *   This class only does uncompressed forms of custom binary serialization.    
 *     
 * This class is for internal use.  Some of the serialized formats are readable by the C++
 * implementation, and used for efficiently transferring CASes between Java frameworks and other ones.
 * Others are used with Vinci or SOAP to communicate to remote annotators.
 * 
 * To serialize the type definition and index specifications for a CAS
 * @see org.apache.uima.cas.impl.CASMgrSerializer
 * 
 * 
 */
public class CASSerializer implements Serializable {

  static final long serialVersionUID = -7972011651957420295L;

  static class AddrPlusValue {
    final int addr;   // heap or aux heap addr
    final long value;  // boolean, byte, short, long, double value
    AddrPlusValue(int addr, long value) {
      this.addr = addr;
      this.value = value;
    }
  }
  
  // The heap itself.
  public int[] heapArray = null;

  // Heap metadata. This is not strictly required, the heap can be
  // deserialized
  // without. Must be null if not used.
  public int[] heapMetaData = null;

  // The string table for strings that are feature structure values. Note that
  // the 0th position in the string table should be null and will be ignored.
  public String[] stringTable;

  // Special encoding of fs pseudo-addrs and counts by view incl. sofa and view counts
  public int[] fsIndex;

  public byte[] byteHeapArray;

  public short[] shortHeapArray;

  public long[] longHeapArray;

  /**
   * Constructor for CASSerializer.
   */
  public CASSerializer() {
    super();
  }

  /**
   * Serialize CAS data without heap-internal meta data. Currently used for serialization to C++.
   * 
   * @param casImpl The CAS to be serialized.
   */
  public void addNoMetaData(CASImpl casImpl) {
    addCAS(casImpl, false);
  }

  /**
   * Add the CAS to be serialized. Note that we need the implementation here, the interface is not
   * enough.
   * 
   * @param cas The CAS to be serialized.
   */
  public void addCAS(CASImpl cas) {
    addCAS(cas, true);
  }

  /**
   * Add the CAS to be serialized. 
   * 
   * @param cas The CAS to be serialized.
   * @param addMetaData - true to include metadata
   */
  public void addCAS(CASImpl cas, boolean addMetaData) {
    synchronized (cas.svd) {
      BinaryCasSerDes bcsd = cas.getBinaryCasSerDes();
      // next call runs the setup, which scans all the reachables
      final CommonSerDesSequential csds = BinaryCasSerDes4.getCsds(cas.getBaseCAS(), false);  // saves the csds in the cas
      bcsd.scanAllFSsForBinarySerialization(null, csds);  // no mark
      this.fsIndex = bcsd.getIndexedFSs(csds.fs2addr);  // must follow scanAll...
      
      if (addMetaData) {
        // some details about current main-heap specifications
        // not required to deserialize
        // not sent for C++
        // is 7 words long
        // not serialized by custom serializers, only by Java object serialization
        int heapsz = bcsd.heap.getCellsUsed();
        this.heapMetaData = new int[] {
          Heap.getRoundedSize(heapsz),  // a bit more than the size of the used heap
          heapsz,                       // the position of the next (unused) slot in the heap
          heapsz,
          0,
          0,
          1024,                                   // initial size
          0}; 
      }
      copyHeapsToArrays(bcsd);
    }
  }
    
  private void outputStringHeap(
      DataOutputStream dos, 
      CASImpl cas, 
      StringHeapDeserializationHelper shdh,
      BinaryCasSerDes bcsd) throws IOException {
    // output the strings

    // compute the number of total size of data in stringHeap
    // total size = char buffer length + length of strings in the string list;
    int stringHeapLength = shdh.charHeapPos;
    int stringListLength = 0;
    for (int i = 0; i < shdh.refHeap.length; i += 3) {
      int ref = shdh.refHeap[i + StringHeapDeserializationHelper.STRING_LIST_ADDR_OFFSET];
      // this is a string in the string list
      // get length and add to total string heap length
      if (ref != 0) {
        // terminate each string with a null
        stringListLength += 1 + bcsd.stringHeap.getStringForCode(ref).length();
      }
    }

    int stringTotalLength = stringHeapLength + stringListLength;
    if (stringHeapLength == 0 && stringListLength > 0) {
      // nothing from stringHeap
      // add 1 for the null at the beginning
      stringTotalLength += 1;
    }
    dos.writeInt(stringTotalLength);

    // write the data in the stringheap, if there is any
    if (stringTotalLength > 0) {
      if (shdh.charHeapPos > 0) {
        dos.writeChars(String.valueOf(shdh.charHeap, 0, shdh.charHeapPos));
      } else {
        // no stringheap data
        // if there is data in the string lists, write a leading 0
        if (stringListLength > 0) {
          dos.writeChar(0);
        }
      }

      // word alignment
      if (stringTotalLength % 2 != 0) {
        dos.writeChar(0);
      }
    }

    // write out the string ref heap
    // each reference consist of a offset into stringheap and a length
    int refheapsz = ((shdh.refHeap.length - StringHeapDeserializationHelper.FIRST_CELL_REF) / StringHeapDeserializationHelper.REF_HEAP_CELL_SIZE) * 2;
    refheapsz++;
    dos.writeInt(refheapsz);
    dos.writeInt(0);
    for (int i = StringHeapDeserializationHelper.FIRST_CELL_REF; i < shdh.refHeap.length; i += 3) {
      dos.writeInt(shdh.refHeap[i + StringHeapDeserializationHelper.CHAR_HEAP_POINTER_OFFSET]);
      dos.writeInt(shdh.refHeap[i + StringHeapDeserializationHelper.CHAR_HEAP_STRLEN_OFFSET]);
    }
  }
  
  void addTsiCAS(CASImpl cas, OutputStream ostream) {
    
  }

  /**
   * Serializes the CAS data and writes it to the output stream.
   * --------------------------------------------------------------------- 
   * Blob         Format    Element 
   * Size         Number of Description 
   * (bytes)      Elements 
   * ------------ --------- -------------------------------- 
   * 4            1         Blob key = "UIMA" in utf-8 
   * 4            1         Version (currently = 1) 
   * 4            1         size of 32-bit FS Heap array = s32H 
   * 4            s32H      32-bit FS heap array 
   * 4            1         size of 16-bit string Heap array = sSH  
   * 2            sSH       16-bit string heap array 
   * 4            1         size of string Ref Heap zrray = sSRH 
   * 4            2*sSRH    string ref offsets and lengths 
   * 4            1         size of FS index array = sFSI 
   * 4            sFSI      FS index array
   * 4            1         size of 8-bit Heap array = s8H  
   * 1            s8H       8-bit Heap array 
   * 4            1         size of 16-bit Heap array = s16H 
   * 2            s16H      16-bit Heap array 
   * 4            1         size of 64-bit Heap array = s64H 
   * 8            s64H      64-bit Heap array
   * ---------------------------------------------------------------------
   * 
   * @param cas  The CAS to be serialized. ostream The output stream.
   * @param ostream -
   */
  public void addCAS(CASImpl cas, OutputStream ostream) {
      addCAS(cas, ostream, false);
  }
  
  public void addCAS(CASImpl cas, OutputStream ostream, boolean includeTsi) {
    synchronized (cas.svd) {
      final BinaryCasSerDes bcsd = cas.getBinaryCasSerDes();
      
      // next call runs the setup, which scans all the reachables
      // these may have changed since this was previously computed due to updates in the CAS    
      final CommonSerDesSequential csds = BinaryCasSerDes4.getCsds(cas.getBaseCAS(), false);  // saves the csds in the cas, used for possible future delta deser
      bcsd.scanAllFSsForBinarySerialization(null, csds);  // no mark
      
      try {
  
        DataOutputStream dos = new DataOutputStream(ostream);
  
        // get the indexed FSs for all views
        this.fsIndex = bcsd.getIndexedFSs(csds.fs2addr);
  
        // output the key and version number
        CommonSerDes.createHeader()
          .seqVer(2)  // 0 original, 1 UIMA-4743 2 for uima v3
          .typeSystemIndexDefIncluded(includeTsi)
          .v3()
          .write(dos);
  
       if (includeTsi) {
          CasIOUtils.writeTypeSystem(cas, ostream, true);
        }
              
        // output the FS heap
        final int heapSize = bcsd.heap.getCellsUsed();
        dos.writeInt(heapSize);
        // writing the 0th (null) element, because that's what V2 did
        final int[] vs = bcsd.heap.heap;
        for (int i = 0; i < heapSize; i++) {
          dos.writeInt(vs[i]);
        }
  
        // output the strings
        StringHeapDeserializationHelper shdh = bcsd.stringHeap.serialize();
  
        outputStringHeap(dos, cas, shdh, bcsd);
  //      // compute the number of total size of data in stringHeap
  //      // total size = char buffer length + length of strings in the string list;
  //      int stringHeapLength = shdh.charHeapPos;
  //      int stringListLength = 0;
  //      for (int i = 0; i < shdh.refHeap.length; i += 3) {
  //        int ref = shdh.refHeap[i + StringHeapDeserializationHelper.STRING_LIST_ADDR_OFFSET];
  //        // this is a string in the string list
  //        // get length and add to total string heap length
  //        if (ref != 0) {
  //          // terminate each string with a null
  //          stringListLength += 1 + cas.getStringHeap().getStringForCode(ref).length();
  //        }
  //      }
  //
  //      int stringTotalLength = stringHeapLength + stringListLength;
  //      if (stringHeapLength == 0 && stringListLength > 0) {
  //        // nothing from stringHeap
  //        // add 1 for the null at the beginning
  //        stringTotalLength += 1;
  //      }
  //      dos.writeInt(stringTotalLength);
  //
  //      // write the data in the stringheap, if there is any
  //      if (stringTotalLength > 0) {
  //        if (shdh.charHeapPos > 0) {
  //          dos.writeChars(String.valueOf(shdh.charHeap, 0, shdh.charHeapPos));
  //        } else {
  //          // no stringheap data
  //          // if there is data in the string lists, write a leading 0
  //          if (stringListLength > 0) {
  //            dos.writeChar(0);
  //          }
  //        }
  //
  //        // word alignment
  //        if (stringTotalLength % 2 != 0) {
  //          dos.writeChar(0);
  //        }
  //      }
  //
  //      // write out the string ref heap
  //      // each reference consist of a offset into stringheap and a length
  //      int refheapsz = ((shdh.refHeap.length - StringHeapDeserializationHelper.FIRST_CELL_REF) / StringHeapDeserializationHelper.REF_HEAP_CELL_SIZE) * 2;
  //      refheapsz++;
  //      dos.writeInt(refheapsz);
  //      dos.writeInt(0);
  //      for (int i = StringHeapDeserializationHelper.FIRST_CELL_REF; i < shdh.refHeap.length; i += 3) {
  //        dos.writeInt(shdh.refHeap[i + StringHeapDeserializationHelper.CHAR_HEAP_POINTER_OFFSET]);
  //        dos.writeInt(shdh.refHeap[i + StringHeapDeserializationHelper.CHAR_HEAP_STRLEN_OFFSET]);
  //      }
  
        // output the index FSs
        dos.writeInt(this.fsIndex.length);
        for (int i = 0; i < this.fsIndex.length; i++) {
          dos.writeInt(this.fsIndex[i]);
        }
  
        // 8bit heap
        final int byteheapsz = bcsd.byteHeap.getSize();
        dos.writeInt(byteheapsz);
        dos.write(bcsd.byteHeap.heap, 0, byteheapsz);
  
        // word alignment
        int align = (4 - (byteheapsz % 4)) % 4;
        for (int i = 0; i < align; i++) {
          dos.writeByte(0);
        }
  
        // 16bit heap
        final int shortheapsz = bcsd.shortHeap.getSize();
        dos.writeInt(shortheapsz);
        final short[] sh = bcsd.shortHeap.heap;
        for (int i = 0; i < shortheapsz; i++) {
          dos.writeShort(sh[i]);
        }
  
        // word alignment
        if (shortheapsz % 2 != 0) {
          dos.writeShort(0);
        }
  
        // 64bit heap
        int longheapsz = bcsd.longHeap.getSize();
        dos.writeInt(longheapsz);
        final long[] lh = bcsd.longHeap.heap;
        for (int i = 0; i < longheapsz; i++) {
          dos.writeLong(lh[i]);
        }
      } catch (IOException e) {
        throw new CASRuntimeException(CASRuntimeException.BLOB_SERIALIZATION, e.getMessage());
      }
      
      bcsd.setHeapExtents();
      // non delta serialization
      csds.setHeapEnd(bcsd.nextHeapAddrAfterMark);
    }
  }

  
  /**
   * Serializes only new and modified FS and index operations made after
   * the tracking mark is created.
   * Serializes CAS data in binary Delta format described below and writes it to the output stream.
   * 
   * ElementSize NumberOfElements Description
   * ----------- ---------------- ---------------------------------------------------------
   * 4				   1				        Blob key = "UIMA" in utf-8 (byte order flag)
   * 4				   1				        Version (1 = complete cas, 2 = delta cas)
   * 4				   1				        size of 32-bit heap array = s32H
   * 4           s32H             32-bit FS heap array (new elements) 
   * 4           1 				        size of 16-bit string Heap array = sSH 
   * 2 			     sSH 				      16-bit string heap array (new strings)
   * 4 				   1 				        size of string Ref Heap array = sSRH 
   * 4 			     2*sSRH				    string ref offsets and lengths (for new strings)
   * 4           1        				number of modified, preexisting 32-bit modified FS heap elements = sM32H
   * 4			     2*sM32H          32-bit heap offset and value (preexisting cells modified)	 
   * 4 	         1 	        			size of FS index array = sFSI 
   * 4		       sFSI 	    			FS index array in Delta format
   * 4 		 	  	 1 			        	size of 8-bit Heap array = s8H 
   * 1 			     s8H 			      	8-bit Heap array (new elements)
   * 4 			  	 1 			        	size of 16-bit Heap array = s16H
   * 2 			     s16H 				    16-bit Heap array (new elements) 
   * 4 			  	 1 			        	size of 64-bit Heap array = s64H 
   * 8 			     s64H 				    64-bit Heap array (new elements)
   * 4				   1			        	number of modified, preexisting 8-bit heap elements = sM8H
   * 4			     sM8H             8-bit heap offsets (preexisting cells modified)
   * 1			     sM8H             8-bit heap values  (preexisting cells modified)
   * 4			  	 1				        number of modified, preexisting 16-bit heap elements = sM16H
   * 4			     sM16H            16-bit heap offsets (preexisting cells modified)
   * 2			     sM16H            16-bit heap values  (preexisting cells modified)
   * 4			  	 1				        number of modified, preexisting 64-bit heap elements = sM64H
   * 4			     sM64H            64-bit heap offsets (preexisting cells modified)
   * 2			     sM64H            64-bit heap values  (preexisting cells modified)
   * 
   * 
   * @param cas -
   * @param ostream -
   * @param trackingMark -
   */
  public void addCAS(CASImpl cas, OutputStream ostream, Marker trackingMark) {
    if (!trackingMark.isValid() ) {
      throw new CASRuntimeException(CASRuntimeException.INVALID_MARKER, "Invalid Marker.");
    }
    MarkerImpl mark = (MarkerImpl) trackingMark;
    synchronized (cas.svd) {
      final BinaryCasSerDes bcsd = cas.getBinaryCasSerDes();
      // next call runs the setup, which scans all the reachables
      final CommonSerDesSequential csds = BinaryCasSerDes4.getCsds(cas.getBaseCAS(), true);  // saves the csds in the cas
      // because the output is only the new elements, this populates the arrays with only the new elements
      //   Note: all heaps reserve slot 0 for null, real data starts at position 1
      List<TOP> all = bcsd.scanAllFSsForBinarySerialization(mark, csds);
  
      //  if (csds.getHeapEnd() == 0) {
      //  System.out.println("debug");
      //}
      final Obj2IntIdentityHashMap<TOP> fs2auxOffset = new Obj2IntIdentityHashMap<>(TOP.class, TOP._singleton);
  
      int byteOffset = 1;
      int shortOffset = 1;
      int longOffset = 1;
      
      // scan all below mark and set up maps from aux array FSs to the offset to where the array starts in the modelled aux heap
      for (TOP fs : all) {
        if (trackingMark.isNew(fs)) {
          break;
        }
        if (fs instanceof CommonArrayFS) {
          CommonArrayFS ca = (CommonArrayFS) fs;
          SlotKind kind = fs._getTypeImpl().getComponentSlotKind();
          switch (kind) {
          case Slot_BooleanRef:
          case Slot_ByteRef :
            fs2auxOffset.put(fs, byteOffset);
            byteOffset += ca.size();
            break;
          case Slot_ShortRef:
            fs2auxOffset.put(fs,  shortOffset);
            shortOffset += ca.size();
            break;
          case Slot_LongRef:
          case Slot_DoubleRef:
            fs2auxOffset.put(fs, longOffset);
            longOffset += ca.size();
            break;
          default:
          } // end of switch
        } // end of if commonarray
        else {  // fs has feature slots 
          // model long and double refs which use up the long aux heap for 1 cell
          TypeImpl ti = fs._getTypeImpl();
          longOffset += ti.getNbrOfLongOrDoubleFeatures();
        }
      } // end of for
      
      try {
        DataOutputStream dos = new DataOutputStream(ostream);
  
        // get the indexed FSs
        this.fsIndex = bcsd.getDeltaIndexedFSs(mark, csds.fs2addr);
   
        CommonSerDes.createHeader()
          .delta()
          .seqVer(2)  // 1 for UIMA-4743 2 for uima v3
          .v3()
          .write(dos);
        
        // output the new FS heap cells
        
        final int heapSize = bcsd.heap.getCellsUsed() - 1; // first entry (null) is not written
        
        // Write heap - either the entire heap, or for delta, just the new part
        
        dos.writeInt(heapSize);
        final int[] vs = bcsd.heap.heap;
        for (int i = 1; i <= heapSize; i++) { // <= because heapsize is 1 less than cells used, but we start at index 1
          dos.writeInt(vs[i]);
        }
  
        // convert v3 change-logging to v2 form, setting the chgXX-addr and chgXX-values lists.
        // we do this before the strings or aux arrays are written out, because this 
        // could make additions to those.
  
        // addresses are in terms of modeled v2 arrays, as absolute addr in the aux arrays, and values
        List<AddrPlusValue> chgMainAvs = new ArrayList<>();
        List<AddrPlusValue> chgByteAvs = new ArrayList<>();
        List<AddrPlusValue> chgShortAvs = new ArrayList<>();
        List<AddrPlusValue> chgLongAvs = new ArrayList<>();
  
        scanModifications(bcsd, csds, cas.getModifiedFSList(), fs2auxOffset,
            chgMainAvs, chgByteAvs, chgShortAvs, chgLongAvs);
  
        // output the new strings
        StringHeapDeserializationHelper shdh = bcsd.stringHeap.serialize(1);
  
        outputStringHeap(dos, cas, shdh, bcsd);
        
        //output modified FS Heap cells
        // this is output in a way that is the total number of slots changed == 
        //   the sum over all fsChanges of
        //     for each fsChange, the number of slots (heap-sited-array or feature) modified
        final int modHeapSize = chgMainAvs.size();
        dos.writeInt(modHeapSize);  //num modified
        for (AddrPlusValue av : chgMainAvs) {
          dos.writeInt(av.addr);        
          dos.writeInt((int)av.value);        
        }
  
        // output the index FSs
        dos.writeInt(this.fsIndex.length); 
        for (int i = 0; i < this.fsIndex.length; i++) {
          dos.writeInt(this.fsIndex[i]);
        }
        
        // 8bit heap new
        int byteheapsz = bcsd.byteHeap.getSize() - 1;
        dos.writeInt(byteheapsz);
        dos.write(bcsd.byteHeap.heap, 1, byteheapsz);  // byte 0 not used
        
        // word alignment
        int align = (4 - (byteheapsz % 4)) % 4;
        for (int i = 0; i < align; i++) {
          dos.writeByte(0);
        }
  
        // 16bit heap new
        int shortheapsz = bcsd.shortHeap.getSize() - 1;
        dos.writeInt(shortheapsz);
        for (int i = 1; i <= shortheapsz; i++) {  // <= in test because we're starting at 1
          dos.writeShort(bcsd.shortHeap.heap[i]);
        }
  
        // word alignment
        if (shortheapsz % 2 != 0) {
          dos.writeShort(0);
        }
  
        // 64bit heap new 
        int longheapsz = bcsd.longHeap.getSize() - 1;
        dos.writeInt(longheapsz);
        for (int i = 1; i <= longheapsz; i++) {
          dos.writeLong(bcsd.longHeap.heap[i]);
        }
        
        // 8bit heap modified cells
        writeMods(chgByteAvs, dos, av -> dos.writeByte((byte)av.value));
  
        // word alignment
        align = (4 - (chgByteAvs.size() % 4)) % 4;
        for (int i = 0; i < align; i++) {
          dos.writeByte(0);
        }
  
        // 16bit heap modified cells
        writeMods(chgShortAvs, dos, av -> dos.writeShort((short)av.value));
  
        // word alignment
        if (chgShortAvs.size() % 2 != 0) {
          dos.writeShort(0);
        }
  
        // 64bit heap modified cells
        writeMods(chgLongAvs, dos, av -> dos.writeLong(av.value));     
        
      } catch (IOException e) {
        throw new CASRuntimeException(CASRuntimeException.BLOB_SERIALIZATION, e.getMessage());
      }
    }
  }
  
  private void writeMods(
      List<AddrPlusValue> avs, 
      DataOutputStream dos, 
      Consumer_T_withIOException<AddrPlusValue> writeValue) throws IOException  {
    int size = avs.size();
    dos.writeInt(size);
    for (AddrPlusValue av : avs) {
      dos.writeInt(av.addr);
    }
    for (AddrPlusValue av : avs) {   
      writeValue.accept(av);
    }   
  }
  /**
   * The offset in the modeled heaps:
   * @param index the 0-based index into the array
   * @param fs the feature structure representing the array
   * @return the addr into an aux array or main heap
   */
  private static int convertArrayIndexToAuxHeapAddr(BinaryCasSerDes bcsd, int index, TOP fs, Obj2IntIdentityHashMap<TOP> fs2auxOffset) {
    int offset = fs2auxOffset.get(fs);
    assert offset > 0;
    return offset; 
  }

  private static int convertArrayIndexToMainHeapAddr(int index, TOP fs, Obj2IntIdentityHashMap<TOP> fs2addr) {
    return fs2addr.get(fs) + 2 + index;
  }

  
  /**
   * Scan the v3 fsChange info and produce
   * v2 style info into chgXxxAddr, chgXxxValue
   * 
   * A prescan approach is needed in order to write the number of modifications preceding the 
   *   write of the values (which unfortunately were written to the same stream in V2).
   * @param bcsd holds the model needed for v2 aux arrays
   * @param cas the cas to use for the delta serialization
   * @param chgMainHeapAddr an ordered collection of changed addresses as an array for the main heap
   * @param chgByteAddr an ordered collection of changed addresses as an array for the aux byte heap
   * @param chgShortAddr an ordered collection of changed addresses as an array for the aus short heap
   * @param chgLongAddr an ordered collection of changed addresses as an array for the aux long heap
   * 
   * @param chgMainHeapValue corresponding values
   */
  static void scanModifications(BinaryCasSerDes bcsd, CommonSerDesSequential csds, FsChange[] fssModified, 
      Obj2IntIdentityHashMap<TOP> fs2auxOffset,
      List<AddrPlusValue> chgMainAvs, 
      List<AddrPlusValue> chgByteAvs, 
      List<AddrPlusValue> chgShortAvs, 
      List<AddrPlusValue> chgLongAvs 
      ) {

    // scan the sorted mods to precompute the various change items:
    //   changed main heap: addr and new slot value
    //   for aux heaps: new values.  
    //     Note: the changed main heap values point to these (and also to new string values)
    //     -- for byte (and boolean), short, long
    //   for aux heaps: changed (updated) values: the addr(s) followed by the values
    final Obj2IntIdentityHashMap<TOP> fs2addr = csds.fs2addr;
    for (FsChange fsChange : fssModified) {
      final TOP fs = fsChange.fs;
      final TypeImpl type = fs._getTypeImpl();
      if (fsChange.arrayUpdates != null) {
        switch(type.getComponentSlotKind()) {
        
        case Slot_BooleanRef: 
          fsChange.arrayUpdates.forAllInts(index -> {
            chgByteAvs.add(new AddrPlusValue(convertArrayIndexToAuxHeapAddr(bcsd, index, fs, fs2auxOffset), 
                ((BooleanArray)fs).get(index) ? 1 : 0));
          }); 
          break;
        
        case Slot_ByteRef:
          fsChange.arrayUpdates.forAllInts(index -> {
            chgByteAvs.add(new AddrPlusValue(convertArrayIndexToAuxHeapAddr(bcsd, index, fs, fs2auxOffset), 
                ((ByteArray)fs).get(index)));
          }); 
          break;

        case Slot_ShortRef:
          fsChange.arrayUpdates.forAllInts(index -> {
            chgShortAvs.add(new AddrPlusValue(convertArrayIndexToAuxHeapAddr(bcsd, index, fs, fs2auxOffset), 
                ((ShortArray)fs).get(index)));
          }); 
          break;
        
        case Slot_LongRef:
          fsChange.arrayUpdates.forAllInts(index -> {
            chgLongAvs.add(new AddrPlusValue(convertArrayIndexToAuxHeapAddr(bcsd, index, fs, fs2auxOffset), 
                ((LongArray)fs).get(index)));
          }); 
          break;

        case Slot_DoubleRef:
          fsChange.arrayUpdates.forAllInts(index -> {
            chgLongAvs.add(new AddrPlusValue(convertArrayIndexToAuxHeapAddr(bcsd, index, fs, fs2auxOffset), 
                CASImpl.double2long(((DoubleArray)fs).get(index))));
          }); 
          break;
        
        // heap stored arrays
        case Slot_Int:
          fsChange.arrayUpdates.forAllInts(index -> {
            chgMainAvs.add(new AddrPlusValue(convertArrayIndexToMainHeapAddr(index, fs, fs2addr), 
                ((IntegerArray)fs).get(index)));
          });
          break;

        case Slot_Float:
          fsChange.arrayUpdates.forAllInts(index -> {
            chgMainAvs.add(new AddrPlusValue(convertArrayIndexToMainHeapAddr(index, fs, fs2addr),
                CASImpl.float2int(((FloatArray)fs).get(index))));
          });
          break;

        case Slot_StrRef:
          fsChange.arrayUpdates.forAllInts(index -> {
            int v = bcsd.nextStringHeapAddrAfterMark + bcsd.stringHeap.addString(((StringArray)fs).get(index));
            chgMainAvs.add(new AddrPlusValue(convertArrayIndexToMainHeapAddr(index, fs, fs2addr), v));
          });
          break;

        case Slot_HeapRef:
          fsChange.arrayUpdates.forAllInts(index -> {
            TOP tgtFs = (TOP)((FSArray)fs).get(index);
            chgMainAvs.add(new AddrPlusValue(convertArrayIndexToMainHeapAddr(index, fs, fs2addr), fs2addr.get(tgtFs)));
          });
          break;

        default: Misc.internalError();
        } // end of switch
      } else { // end of if-array
        // 1 or more features modified
        if (fs instanceof UimaSerializable) {
          ((UimaSerializable)fs)._save_to_cas_data();
        }
        BitSet fm = fsChange.featuresModified;
        int offset = fm.nextSetBit(0);
        while (offset >= 0) {
          int addr = csds.fs2addr.get(fs) + offset + 1;  // skip over type code);
          int value = 0;

          FeatureImpl feat = type.getFeatureImpls()[offset];

          switch (feat.getSlotKind()) {
          case Slot_Boolean: value = fs._getBooleanValueNc(feat) ? 1 : 0; break;
            
          case Slot_Byte:    value = fs._getByteValueNc(feat); break;
          case Slot_Short:   value = fs._getShortValueNc(feat); break;
          case Slot_Int:     value = fs._getIntValueNc(feat); break;
          case Slot_Float:   value = CASImpl.float2int(fs._getFloatValueNc(feat)); break;
          case Slot_LongRef: {
            value = bcsd.nextLongHeapAddrAfterMark + bcsd.longHeap.addLong(fs._getLongValueNc(feat));
            break;
          }
          case Slot_DoubleRef: {
            value = bcsd.nextLongHeapAddrAfterMark + bcsd.longHeap.addLong(CASImpl.double2long(fs._getDoubleValueNc(feat)));
            break;
          }
          case Slot_StrRef: {
            value = bcsd.nextStringHeapAddrAfterMark + bcsd.stringHeap.addString(fs._getStringValueNc(feat));
            break;
          }
          case Slot_HeapRef: value = fs2addr.get(fs._getFeatureValueNc(feat)); break;
          default: Misc.internalError();
          } // end of switch
          
          chgMainAvs.add(new AddrPlusValue(addr, value));
          
          offset = fm.nextSetBit(offset + 1);
        } // loop over changed feature offsets         
      } // end of features-modified case
    } // end of for all fsChanges
  }
  
//  /**
//   * Serialize with compression
//   * Target is not constrained to the C++ format
//   * For non delta serialization, pass marker with 0 as values
//   * @throws IOException 
//   */
//
//  public void serialize(CASImpl cas, OutputStream ostream, Marker marker) throws IOException {
//    if (marker != null && !marker.isValid() ) {
//      CASRuntimeException exception = new CASRuntimeException(
//                CASRuntimeException.INVALID_MARKER, new String[] { "Invalid Marker." });
//      throw exception;
//    }
//    MarkerImpl mark = (MarkerImpl) marker;
//    DataOutputStream dos = new DataOutputStream(ostream);
//
//    this.fsIndex = cas.getDeltaIndexedFSs(mark);
//    outputVersion(3 , dos);
//    
//    // output the new FS heap cells
//    final int heapSize = cas.getHeap().getCellsUsed() - mark.nextFSId);
//    compressHeapOut(dos, cas, heapSize, mark)
//
//    // output the new strings
//
//  }
  
  int[] getHeapMetadata() {
    return this.heapMetaData;
  }

  int[] getHeapArray() {
    return this.heapArray;
  }

  String[] getStringTable() {
    return this.stringTable;
  }

  int[] getFSIndex() {
    return this.fsIndex;
  }

  byte[] getByteArray() {
    return this.byteHeapArray;
  }

  short[] getShortArray() {
    return this.shortHeapArray;
  }

  long[] getLongArray() {
    return this.longHeapArray;
  }
    
  private void copyHeapsToArrays(BinaryCasSerDes bcsd) {
    this.heapArray = bcsd.heap.toArray();
    this.byteHeapArray = bcsd.byteHeap.toArray();
    this.shortHeapArray = bcsd.shortHeap.toArray();
    this.longHeapArray = bcsd.longHeap.toArray();
    this.stringTable = bcsd.stringHeap.toArray();
  }
}
