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
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Marker;

/**
 * Serialization for CAS. This serializes the state of the CAS, assuming that the type and index
 * information remains constant. <code>CASSerializer</code> objects can be serialized with
 * standard Java serialization.
 * 
 * @see org.apache.uima.cas.impl.CASMgrSerializer
 * 
 * 
 */
public class CASSerializer implements Serializable {

  static final long serialVersionUID = -7972011651957420295L;

  // The heap itself.
  public int[] heapArray = null;

  // Heap metadata. This is not strictly required, the heap can be
  // deserialized
  // without. Must be null if not used.
  public int[] heapMetaData = null;

  // The string table for strings that are feature structure values. Note that
  // the 0th position in the string table should be null and will be ignored.
  public String[] stringTable;

  // All FSs in any index.
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
   * @param casImpl
   *                The CAS to be serialized.
   */
  public void addNoMetaData(CASImpl casImpl) {
    addCAS(casImpl, false);
  }

  /**
   * Add the CAS to be serialized. Note that we need the implementation here, the interface is not
   * enough.
   * 
   * @param cas
   *                The CAS to be serialized.
   */
  public void addCAS(CASImpl cas) {
    addCAS(cas, true);
  }

  /**
   * Add the CAS to be serialized. Note that we need the implementation here, the interface is not
   * enough.
   * 
   * @param cas
   *                The CAS to be serialized.
   */
  public void addCAS(CASImpl cas, boolean addMetaData) {
    this.fsIndex = cas.getIndexedFSs();
    final int heapSize = cas.getHeap().getCellsUsed();
    this.heapArray = new int[heapSize];
    System.arraycopy(cas.getHeap().heap, 0, this.heapArray, 0, heapSize);
    if (addMetaData) {
      this.heapMetaData = cas.getHeap().getMetaData();
    }
    this.stringTable = stringArrayListToArray(cas.getStringTable());

    final int byteHeapSize = cas.getByteHeap().getSize();
    this.byteHeapArray = new byte[byteHeapSize];
    System.arraycopy(cas.getByteHeap().heap, 0, this.byteHeapArray, 0, byteHeapSize);

    final int shortHeapSize = cas.getShortHeap().getSize();
    this.shortHeapArray = new short[shortHeapSize];
    System.arraycopy(cas.getShortHeap().heap, 0, this.shortHeapArray, 0, shortHeapSize);

    final int longHeapSize = cas.getLongHeap().getSize();
    this.longHeapArray = new long[longHeapSize];
    System.arraycopy(cas.getLongHeap().heap, 0, this.longHeapArray, 0, longHeapSize);
  }

  /**
   * Serializes the CAS data and writes it to the output stream.
   * --------------------------------------------------------------------- Blob Format
   * 
   * Element Size Number of Description (bytes) Elements ------------ ---------
   * -------------------------------- 4 1 Blob key = "UIMA" in utf-8 4 1 Version (currently = 1) 4 1
   * size of 32-bit FS Heap array = s32H 4 s32H 32-bit FS heap array 4 1 size of 16-bit string Heap
   * array = sSH 2 sSH 16-bit string heap array 4 1 size of string Ref Heap array = sSRH 4 2*sSRH
   * string ref offsets and lengths 4 1 size of FS index array = sFSI 4 sFSI FS index array
   * 
   * 4 1 size of 8-bit Heap array = s8H 1 s8H 8-bit Heap array 4 1 size of 16-bit Heap array = s16H
   * 2 s16H 16-bit Heap array 4 1 size of 64-bit Heap array = s64H 8 s64H 64-bit Heap array
   * ---------------------------------------------------------------------
   * 
   * This reads in and deserializes CAS data from a stream. Byte swapping may be needed is the blob
   * is from C++ -- C++ blob serialization writes data in native byte order.
   * 
   * @param cas
   *                The CAS to be serialized. ostream The output stream.
   */
  public void addCAS(CASImpl cas, OutputStream ostream) {

    try {

      DataOutputStream dos = new DataOutputStream(ostream);

      // get the indexed FSs
      this.fsIndex = cas.getIndexedFSs();

      // output the key and version number

      byte[] uima = new byte[4];
      uima[0] = 85; // U
      uima[1] = 73; // I
      uima[2] = 77; // M
      uima[3] = 65; // A

      ByteBuffer buf = ByteBuffer.wrap(uima);
      int key = buf.asIntBuffer().get();

      int version = 1;
      dos.writeInt(key);
      dos.writeInt(version);

      // output the FS heap
      final int heapSize = cas.getHeap().getCellsUsed();
      dos.writeInt(heapSize);
      for (int i = 0; i < heapSize; i++) {
        dos.writeInt(cas.getHeap().heap[i]);
      }

      // output the strings
      StringHeapDeserializationHelper shdh = cas.getStringHeap().serialize();

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
          stringListLength += 1 + cas.getStringHeap().getStringForCode(ref).length();
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

      // output the index FSs
      dos.writeInt(this.fsIndex.length);
      for (int i = 0; i < this.fsIndex.length; i++) {
        dos.writeInt(this.fsIndex[i]);
      }

      // 8bit heap
      int byteheapsz = cas.getByteHeap().getSize();
      dos.writeInt(byteheapsz);
      for (int i = 0; i < cas.getByteHeap().getSize(); i++) {
        dos.writeByte(cas.getByteHeap().heap[i]);
      }

      // word alignment
      int align = (4 - (byteheapsz % 4)) % 4;
      for (int i = 0; i < align; i++) {
        dos.writeByte(0);
      }

      // 16bit heap
      int shortheapsz = cas.getShortHeap().getSize();
      dos.writeInt(shortheapsz);
      for (int i = 0; i < cas.getShortHeap().getSize(); i++) {
        dos.writeShort(cas.getShortHeap().heap[i]);
      }

      // word alignment
      if (shortheapsz % 2 != 0) {
        dos.writeShort(0);
      }

      // 64bit heap
      int longheapsz = cas.getLongHeap().getSize();
      dos.writeInt(longheapsz);
      for (int i = 0; i < cas.getLongHeap().getSize(); i++) {
        dos.writeLong(cas.getLongHeap().heap[i]);
      }
    } catch (IOException e) {
      CASRuntimeException exception = new CASRuntimeException(
          CASRuntimeException.BLOB_SERIALIZATION, new String[] { e.getMessage() });
      throw exception;
    }

  }

  
  /**
   * Serializes only new and modified FS and index operations made after
   * the tracking mark is created.
   * Serizlizes CAS data in binary Delta format described below and writes it to the output stream.
   * 
   * ElementSize NumberOfElements Description
   * ----------- ---------------- ---------------------------------------------------------
   * 4				1				Blob key = "UIMA" in utf-8 (byte order flag)
   * 4				1				Version (1 = complete cas, 2 = delta cas)
   * 4				1				size of 32-bit heap array = s32H
   * 4            s32H              32-bit FS heap array (new elements) 
   * 4              1 				size of 16-bit string Heap array = sSH 
   * 2 			   sSH 				16-bit string heap array (new strings)
   * 4 				1 				size of string Ref Heap array = sSRH 
   * 4 			2*sSRH				string ref offsets and lengths (for new strings)
   * 4              1				number of modified, preexisting 32-bit modified FS heap elements = sM32H
   * 4			2*sM32H             32-bit heap offset and value (preexisting cells modified)	 
   * 4 	            1 				size of FS index array = sFSI 
   * 4			  sFSI 				FS index array in Delta format
   * 4 				1 				size of 8-bit Heap array = s8H 
   * 1 			  s8H 				8-bit Heap array (new elements)
   * 4 				1 				size of 16-bit Heap array = s16H
   * 2 			  s16H 				16-bit Heap array (new elements) 
   * 4 				1 				size of 64-bit Heap array = s64H 
   * 8 			  s64H 				64-bit Heap array (new elements)
   * 4				1				number of modified, preexisting 8-bit heap elements = sM8H
   * 4			  sM8H              8-bit heap offsets (preexisting cells modified)
   * 1			  sM8H              8-bit heap values  (preexisting cells modified)
   * 4				1				number of modified, preexisting 16-bit heap elements = sM16H
   * 4			  sM16H             16-bit heap offsets (preexisting cells modified)
   * 2			  sM16H             16-bit heap values  (preexisting cells modified)
   * 4				1				number of modified, preexisting 64-bit heap elements = sM64H
   * 4			  sM64H             64-bit heap offsets (preexisting cells modified)
   * 2			  sM64H             64-bit heap values  (preexisting cells modified)
   * 
   * 
   * @param cas
   * @param ostream
   * @param trackingMark
   */
  public void addCAS(CASImpl cas, OutputStream ostream, Marker trackingMark) {

    try {
      MarkerImpl mark = (MarkerImpl) trackingMark;
      DataOutputStream dos = new DataOutputStream(ostream);

      // get the indexed FSs
      this.fsIndex = cas.getDeltaIndexedFSs(mark);
      
      // output the key and version number

      byte[] uima = new byte[4];
      uima[0] = 85; // U
      uima[1] = 73; // I
      uima[2] = 77; // M
      uima[3] = 65; // A

      ByteBuffer buf = ByteBuffer.wrap(uima);
      int key = buf.asIntBuffer().get();

      int version = 2;    //1 = current full serialization; 2 = delta format 
                          //perhaps this should be split into 2 bytes for version and 2 bytes for format.
      dos.writeInt(key);
      dos.writeInt(version);

      // output the new FS heap cells
      final int heapSize = cas.getHeap().getCellsUsed() - mark.nextFSId;
      
      dos.writeInt(heapSize);
      for (int i = mark.nextFSId; i < cas.getHeap().getCellsUsed(); i++) {
        dos.writeInt(cas.getHeap().heap[i]);
      }

      // output the new strings
      StringHeapDeserializationHelper shdh = cas.getStringHeap().serialize(mark.nextStringHeapAddr);

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
          stringListLength += 1 + cas.getStringHeap().getStringForCode(ref).length();
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

      //output modified FS Heap cells
      int[] fsHeapModifiedAddrs = cas.getModifiedFSHeapAddrs().toArray();
      dos.writeInt(fsHeapModifiedAddrs.length);  //num modified
      for (int i=0; i < fsHeapModifiedAddrs.length; i++) {
    	  dos.writeInt(fsHeapModifiedAddrs[i]);
    	  dos.writeInt(cas.getHeapValue(fsHeapModifiedAddrs[i]));
      }
      
      // output the index FSs
      dos.writeInt(this.fsIndex.length);
      for (int i = 0; i < this.fsIndex.length; i++) {
        dos.writeInt(this.fsIndex[i]);
      }

      // 8bit heap new
      int byteheapsz = cas.getByteHeap().getSize() - mark.nextByteHeapAddr;
      dos.writeInt(byteheapsz);
      for (int i = mark.nextByteHeapAddr; i < cas.getByteHeap().getSize(); i++) {
        dos.writeByte(cas.getByteHeap().heap[i]);
      }

      // word alignment
      int align = (4 - (byteheapsz % 4)) % 4;
      for (int i = 0; i < align; i++) {
        dos.writeByte(0);
      }

      // 16bit heap new
      int shortheapsz = cas.getShortHeap().getSize() - mark.nextShortHeapAddr;
      dos.writeInt(shortheapsz);
      for (int i = mark.nextShortHeapAddr; i < cas.getShortHeap().getSize(); i++) {
        dos.writeShort(cas.getShortHeap().heap[i]);
      }

      // word alignment
      if (shortheapsz % 2 != 0) {
        dos.writeShort(0);
      }

      // 64bit heap new 
      int longheapsz = cas.getLongHeap().getSize() - mark.nextLongHeapAddr;
      dos.writeInt(longheapsz);
      for (int i = mark.nextLongHeapAddr; i < cas.getLongHeap().getSize(); i++) {
        dos.writeLong(cas.getLongHeap().heap[i]);
      }
      
      // 8 bit heap modified cells
      int[] byteHeapModifiedAddrs = cas.getModifiedByteHeapAddrs().toArray();
      byte[] byteValues = new byte[byteHeapModifiedAddrs.length];
      dos.writeInt(byteHeapModifiedAddrs.length);
      for (int i=0; i < byteHeapModifiedAddrs.length; i++) {
    	dos.writeInt(byteHeapModifiedAddrs[i]);
    	byteValues[i] = cas.getByteHeap().getHeapValue(byteHeapModifiedAddrs[i]);
      }
      for (int i=0; i < byteValues.length; i++) {  
    	  dos.writeByte(cas.getByteHeap().getHeapValue(byteHeapModifiedAddrs[i]));
	  }
      
      // word alignment
      align = (4 - (byteheapsz % 4)) % 4;
      for (int i = 0; i < align; i++) {
        dos.writeByte(0);
      }
      
      // 16 bit heap modified cells
      int[] shortHeapModifiedAddrs = cas.getModifiedShortHeapAddrs().toArray();
      short[] shortValues = new short[shortHeapModifiedAddrs.length];
      dos.writeInt(shortHeapModifiedAddrs.length);
      for (int i=0; i < shortHeapModifiedAddrs.length; i++) {
    	dos.writeShort(shortHeapModifiedAddrs[i]);
    	shortValues[i] = cas.getShortHeap().getHeapValue(shortHeapModifiedAddrs[i]);
      }
      for (int i=0; i < shortValues.length; i++) {  
    	  dos.writeShort(cas.getShortHeap().getHeapValue(shortHeapModifiedAddrs[i]));
	  }
      
      // word alignment
      if (shortheapsz % 2 != 0) {
        dos.writeShort(0);
      }
      
      // 64 bit heap modified cells
      int[] longHeapModifiedAddrs = cas.getModifiedShortHeapAddrs().toArray();
      long[] longValues = new long[longHeapModifiedAddrs.length];
      dos.writeInt(longHeapModifiedAddrs.length);
      for (int i=0; i < longHeapModifiedAddrs.length; i++) {
    	dos.writeShort(longHeapModifiedAddrs[i]);
    	longValues[i] = cas.getLongHeap().getHeapValue(longHeapModifiedAddrs[i]);
      }
      for (int i=0; i < longValues.length; i++) {  
    	  dos.writeLong(cas.getLongHeap().getHeapValue(longHeapModifiedAddrs[i]));
	  }
      
    } catch (IOException e) {
      CASRuntimeException exception = new CASRuntimeException(
          CASRuntimeException.BLOB_SERIALIZATION, new String[] { e.getMessage() });
      throw exception;
    }

  }

  /**
   * Method stringArrayListToArray.
   * 
   * @param arrayList
   * @return String[]
   */
  private String[] stringArrayListToArray(ArrayList<String> arrayList) {
    final int max = arrayList.size();
    String[] ar = new String[max];
    for (int i = 0; i < max; i++) {
      ar[i] = arrayList.get(i);
    }
    return ar;
  }

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

}
