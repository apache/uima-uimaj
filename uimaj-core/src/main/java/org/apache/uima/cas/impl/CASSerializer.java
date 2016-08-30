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

import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Marker;
import org.apache.uima.util.CasIOUtils;

/**
 * This object has 2 purposes.
 *   - it can hold a collection of individually Java-object-serializable objects representing a CAS +
 *     the list of FS's indexed in the CAS
 *     
 *   - it has special methods to do a custom binary serialization (no compression) of a CAS + lists
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
   * Add the CAS to be serialized. Note that we need the implementation here, the interface is not
   * enough.
   * 
   * @param cas The CAS to be serialized.
   * @param addMetaData -
   */
  public void addCAS(CASImpl cas, boolean addMetaData) {
    this.fsIndex = cas.getIndexedFSs();
    final int heapSize = cas.getHeap().getCellsUsed();
    this.heapArray = new int[heapSize];
    System.arraycopy(cas.getHeap().heap, 0, this.heapArray, 0, heapSize);
    if (addMetaData) {
      // some details about current main-heap specifications
      // not required to deserialize
      // not sent for C++
      // is 7 words long
      // not serialized by custom serializers, only by Java object serialization
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
    
  private void outputStringHeap(DataOutputStream dos, CASImpl cas, StringHeapDeserializationHelper shdh) throws IOException {
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
  try {

      DataOutputStream dos = new DataOutputStream(ostream);

      // output the key and version number
      CommonSerDes.createHeader()
      .seqVer(1)     // not a delta, set version 1 for UIMA-4743 to inform receiver that we can handle version 1 format
      .typeSystemIndexDefIncluded(includeTsi)
      .write(dos);
      
      if (includeTsi) {
        CasIOUtils.writeTypeSystem(cas, ostream, true);
      }

      // get the indexed FSs
      this.fsIndex = cas.getIndexedFSs();
     
      // output the FS heap
      final int heapSize = cas.getHeap().getCellsUsed();
      dos.writeInt(heapSize);
      for (int i = 0; i < heapSize; i++) {
        dos.writeInt(cas.getHeap().heap[i]);
      }

      // output the strings
      StringHeapDeserializationHelper shdh = cas.getStringHeap().serialize();

      outputStringHeap(dos, cas, shdh);
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

    try {
      if (!trackingMark.isValid() ) {
        CASRuntimeException exception = new CASRuntimeException(
    		          CASRuntimeException.INVALID_MARKER, new String[] { "Invalid Marker." });
        throw exception;
      }
      MarkerImpl mark = (MarkerImpl) trackingMark;
      DataOutputStream dos = new DataOutputStream(ostream);

      // get the indexed FSs
      this.fsIndex = cas.getDeltaIndexedFSs(mark);
      
      CommonSerDes.createHeader()
      .delta()                                // is delta
      .seqVer(1)
       .write(dos);    

      // output the new FS heap cells
      final int heapSize = cas.getHeap().getCellsUsed() - mark.nextFSId;
      
      dos.writeInt(heapSize);
      for (int i = mark.nextFSId; i < cas.getHeap().getCellsUsed(); i++) {
        dos.writeInt(cas.getHeap().heap[i]);
      }

      // output the new strings
      StringHeapDeserializationHelper shdh = cas.getStringHeap().serialize(mark.nextStringHeapAddr);

      outputStringHeap(dos, cas, shdh);
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
//      byte[] byteValues = new byte[byteHeapModifiedAddrs.length];
      dos.writeInt(byteHeapModifiedAddrs.length);
      for (int i=0; i < byteHeapModifiedAddrs.length; i++) {
      	dos.writeInt(byteHeapModifiedAddrs[i]);
//      	byteValues[i] = cas.getByteHeap().getHeapValue(byteHeapModifiedAddrs[i]);
      }
      for (int i=0; i < byteHeapModifiedAddrs.length; i++) {  
    	  dos.writeByte(cas.getByteHeap().getHeapValue(byteHeapModifiedAddrs[i]));
	    }

      // word alignment
      align = (4 - (byteHeapModifiedAddrs.length % 4)) % 4;
      for (int i = 0; i < align; i++) {
        dos.writeByte(0);
      }
      
      // 16 bit heap modified cells
      int[] shortHeapModifiedAddrs = cas.getModifiedShortHeapAddrs().toArray();
      short[] shortValues = new short[shortHeapModifiedAddrs.length];
      dos.writeInt(shortHeapModifiedAddrs.length);
      for (int i=0; i < shortHeapModifiedAddrs.length; i++) {
        int modifiedAddr = shortHeapModifiedAddrs[i]; 
        dos.writeInt(modifiedAddr);
      	shortValues[i] = cas.getShortHeap().getHeapValue(modifiedAddr);
      }
      for (int i=0; i < shortValues.length; i++) {  
    	  dos.writeShort(cas.getShortHeap().getHeapValue(shortHeapModifiedAddrs[i]));
	  }
      
      // word alignment
      if (shortHeapModifiedAddrs.length % 2 != 0) {
        dos.writeShort(0);
      }
      
      // 64 bit heap modified cells
      int[] longHeapModifiedAddrs = cas.getModifiedLongHeapAddrs().toArray(); // https://issues.apache.org/jira/browse/UIMA-4743
      long[] longValues = new long[longHeapModifiedAddrs.length];
      dos.writeInt(longHeapModifiedAddrs.length);
      for (int i=0; i < longHeapModifiedAddrs.length; i++) {
        int modifiedAddr = longHeapModifiedAddrs[i];
        dos.writeInt(modifiedAddr);
        longValues[i] = cas.getLongHeap().getHeapValue(modifiedAddr);
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
