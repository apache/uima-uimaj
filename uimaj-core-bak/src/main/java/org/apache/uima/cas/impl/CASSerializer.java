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

/**
 * Serialization for CAS. This serializes the state of the CAS, assuming that
 * the type and index information remains constant. <code>CASSerializer</code>
 * objects can be serialized with standard Java serialization.
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
     * Serialize CAS data without heap-internal meta data. Currently used for
     * serialization to TAF.
     * 
     * @param casImpl
     *            The CAS to be serialized.
     */
    public void addNoMetaData(CASImpl casImpl) {
        addCAS(casImpl, false);
    }

    /**
     * Add the CAS to be serialized. Note that we need the implementation here,
     * the interface is not enough.
     * 
     * @param cas
     *            The CAS to be serialized.
     */
    public void addCAS(CASImpl cas) {
        addCAS(cas, true);
    }

    /**
     * Add the CAS to be serialized. Note that we need the implementation here,
     * the interface is not enough.
     * 
     * @param cas
     *            The CAS to be serialized.
     */
    public void addCAS(CASImpl cas, boolean addMetaData) {
        this.fsIndex = cas.getIndexedFSs();
        final int heapSize = cas.heap.getCurrentTempSize();
        this.heapArray = new int[heapSize];
        System.arraycopy(cas.heap.heap, 0, this.heapArray, 0, heapSize);
        if (addMetaData) {
            this.heapMetaData = cas.heap.getMetaData();
        }
        this.stringTable = stringArrayListToArray(cas.getStringTable());
        
        final int byteHeapSize = cas.byteHeap.getSize();  
        this.byteHeapArray = new byte[byteHeapSize];
        System.arraycopy(cas.byteHeap.heap, 0, byteHeapArray, 0, byteHeapSize);  
            
        final int shortHeapSize = cas.shortHeap.getSize();
        this.shortHeapArray = new short[shortHeapSize];
        System.arraycopy(cas.shortHeap.heap, 0, shortHeapArray, 0, shortHeapSize); 
             
        final int longHeapSize = cas.longHeap.getSize();
        this.longHeapArray = new long[longHeapSize];
        System.arraycopy(cas.longHeap.heap, 0, longHeapArray, 0, longHeapSize);    
    }
    
    /**
     * Serializes the CAS data and writes it to the output stream.
     * ---------------------------------------------------------------------
     * Blob Format
     *
     * Element Size     Number of     Description
     *   (bytes)        Elements
     * ------------     ---------     --------------------------------
     *      4               1         Blob key = "UIMA" in utf-8
     *      4               1         Version (currently = 1)
     *      4               1         size of 32-bit FS Heap array = s32H
     *      4             s32H        32-bit FS heap array
     *      4               1         size of 16-bit string Heap array = sSH
     *      2              sSH        16-bit string heap array
     *      4               1         size of string Ref Heap array = sSRH
     *      4             2*sSRH      string ref offsets and lengths
     *      4               1         size of FS index array = sFSI
     *      4             sFSI        FS index array
     *      
     *      to be added
     *      4               1         size of 8-bit Heap array = s8H
     *      1              s8H        8-bit Heap array
     *      4               1         size of 16-bit Heap array = s16H
     *      2             s16H        16-bit Heap array
     *      4               1         size of 64-bit Heap array = s64H
     *      8             s64H        64-bit Heap array
     *---------------------------------------------------------------------
     * 
     * This reads in and deserializes CAS data from a stream.
     * Byte swapping may be needed is the blob is from TAF --
     * TAF blob serialization writes data in native byte order.
     *    
     * @param cas
     *            The CAS to be serialized.
     *        ostream 
     *            The output stream.
     */
    public void addCAS(CASImpl cas, OutputStream ostream) {
    	
        try {
        	
        	DataOutputStream dos = new DataOutputStream(ostream);
        	
        	//get the indexed FSs
            this.fsIndex = cas.getIndexedFSs();
            
            //output the key and version number
            
            byte[] uima = new byte[4];
            uima[3] = 85; //U
            uima[2] = 73; //I
            uima[1] = 77; //M
            uima[0] = 65; //A 
   
            ByteBuffer buf = ByteBuffer.wrap(uima);
            int key = buf.asIntBuffer().get();
                
            int version = 1;
			dos.writeInt(key);
			dos.writeInt(version);
			
			//output the FS heap
			final int heapSize = cas.heap.getCurrentTempSize();
			dos.writeInt(heapSize);
			for (int i=0; i<heapSize; i++) {
	        	dos.writeInt(cas.heap.heap[i]);
	        }
			
			//output the strings
			//strings in the StringList will be serialized out as if the
			//were in the string heap.
								
			//local array to hold ref heap to be serialized
			//String list reference in this local ref heap will be updated to be string heap references.
			int[] refheap = new int[cas.stringHeap.refHeapPos];
			for (int i=0;i< refheap.length;i++) {
				refheap[i] = cas.stringHeap.refHeap[i];
			}
					
			//compute the number of total size of data in stringHeap 
			//total size = char buffer length + length of strings in the string list;
			int stringHeapLength = cas.stringHeap.charHeapPos+1;
			for (int i=0; i < refheap.length; i+=3) {
				int ref = refheap[i+StringHeap.STRING_LIST_ADDR_OFFSET];
					//this is a string in the string list
				    //get length and add to total string heap length
				if (ref !=0) {				
					stringHeapLength += 1 + ((String) cas.stringHeap.stringList.get(ref)).length(); 
				}			
			}
						
			//word alignment
			if (stringHeapLength % 2  != 0) {
				dos.writeInt(stringHeapLength + 1);
			}
			else {
				dos.writeInt(stringHeapLength);
			}

			//write 0 as first char buffer
			dos.writeChar(0);
			
			//write the data in the stringheap
			if (cas.stringHeap.charHeapPos > 0)
				dos.writeChars( String.valueOf(cas.stringHeap.stringHeap) );
									
			
			//write out the data in the StringList and update the 
			//reference in the local ref heap.
			
			
			int pos = cas.stringHeap.charHeapPos+1;
			for (int i=0; i < refheap.length; i+=3) {
				int ref = refheap[i+StringHeap.STRING_LIST_ADDR_OFFSET];
				//this is a string in the string list
				if (ref !=0) {
					//update the ref					
					refheap[i+StringHeap.CHAR_HEAP_POINTER_OFFSET] = pos;
					//write out the chars in the string
					dos.writeChars((String)cas.stringHeap.stringList.get(ref));
					dos.writeChar(0); // null terminate each string
					//update pos
					pos += 1 + ((String) cas.stringHeap.stringList.get(ref)).length(); 
				}				
			}
			
			//word alignment
			if (stringHeapLength % 2  != 0) {
				dos.writeChar(0);
			}
			
			//write out the string ref heap
			//each reference consist of a offset into stringheap and a length
			int refheapsz = ((refheap.length-StringHeap.FIRST_CELL_REF)/StringHeap.REF_HEAP_CELL_SIZE)*2;
			refheapsz++;
			dos.writeInt(refheapsz);
			dos.writeInt(0);
			for (int i=StringHeap.FIRST_CELL_REF; i < refheap.length; i+=3) {
				dos.writeInt(refheap[i+StringHeap.CHAR_HEAP_POINTER_OFFSET]);
				dos.writeInt(refheap[i+StringHeap.CHAR_HEAP_STRLEN_OFFSET]);
			}
			
			//output the index FSs
			dos.writeInt(this.fsIndex.length);
			for (int i=0; i < this.fsIndex.length; i++) {				
				dos.writeInt(this.fsIndex[i]);			
			}
			//8bit heap
			int byteheapsz = 4 *((cas.byteHeap.getSize()+3)/4);
			dos.writeInt(byteheapsz);
			for (int i=0; i < cas.byteHeap.getSize(); i++) {
				dos.writeByte(cas.byteHeap.heap[i]);
			}
				
            //word alignment
			int align = byteheapsz - cas.byteHeap.getSize(); 
			for (int i=0; i < align; i++) {
					dos.writeByte(0);
			}	
			
			//16bit heap
			int shortheapsz = 2 *((cas.shortHeap.getSize()*2+2)/4);
			dos.writeInt(shortheapsz);
			for (int i=0; i < cas.shortHeap.getSize(); i++) {
				dos.writeShort(cas.shortHeap.heap[i]);
			}
				
            //word alignment
			align = shortheapsz - (cas.shortHeap.getSize()); 
			for (int i=0; i < align; i++) {
					dos.writeChar(0);
			}	
				
			//64bit heap
			int longheapsz = cas.longHeap.getSize();
			dos.writeInt(longheapsz);
			for (int i=0; i < cas.longHeap.getSize(); i++) {
				dos.writeLong(cas.longHeap.heap[i]);
			}       
		} catch (IOException e) {
			CASRuntimeException exception = new CASRuntimeException(CASRuntimeException.BLOB_SERIALIZATION);
			exception.addArgument(e.getMessage());
			throw exception;
		}        
        
    }
    
    /**
     * Method stringArrayListToArray.
     * 
     * @param arrayList
     * @return String[]
     */
    private String[] stringArrayListToArray(ArrayList arrayList) {
        final int max = arrayList.size();
        String[] ar = new String[max];
        for (int i = 0; i < max; i++) {
            ar[i] = (String) arrayList.get(i);
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
