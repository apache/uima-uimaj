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
package org.apache.uima.util.impl;

import static org.apache.uima.cas.impl.SlotKinds.CAN_BE_NEGATIVE;
import static org.apache.uima.cas.impl.SlotKinds.IN_MAIN_HEAP;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_ArrayLength;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Byte;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Double_Exponent;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Double_Mantissa_Sign;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Float_Exponent;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Float_Mantissa_Sign;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_FsIndexes;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_HeapRef;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Int;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Long_High;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Long_Low;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_MainHeap;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Short;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_StrChars;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_StrLength;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_StrOffset;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_TypeCode;

import org.apache.uima.cas.impl.SlotKinds.SlotKind;


/**
 * Structure:

 *   StatDetail        
 *   
 *   str         has neh for offset, length, dictionary hits/misses
 *   
 *   indexedFs   has neh for diffs
 *   
 *   modHeap     named, has neh for diffs, heap for values
 */
public class SerializationMeasures {
  
  public static final int MAX_NBR_ENCODE_LENGTH = 10; // for long values taking 64 bits at 7 bits per byte
                                                      
  /** 
   * helper method to truncate printing of lots of trailing 0s
   * @param c
   * @return the index of the 1st 0 where all the rest are 0's, or the last index
   */
  private static int maxIndexToZeros(int[] c) {
    for (int i = c.length - 1; i >= 0; i--) {
      if (c[i] != 0) {
        return Math.min(i + 1, c.length - 1);
      }
    }
    return 1;
  }  
  
  /**
   * Statistical details
   *   There's instances of this class for
   *     - the main heap
   *     - the aux heaps
   *     - the string offsets, the string lengths
   *     
   * Heap: xxxx  [name-of-delta: [Total: &lt;TotalBytes&gt;(negative%)  Histo: a(neg%) b(neg%) c(neg%) d(neg%) e(neg%)]]
    *   2 styles: one uses only one counter, no delta  - used for byte, short, and long heaps
    *   other is for main heap, uses 4 deltas.
   *
   */
  public static class StatDetail {
    private final String name;
    public long original = -1;    // if set, use this, otherwise use countTotal * bytesPerCount
    final boolean canBeNegative;
    public final int[] c = new int[MAX_NBR_ENCODE_LENGTH]; 
    private final int[] cn; // negative counts
    private final int bytesPerCount;  // # bytes in source, per entry
    public int countTotal;  // plain count not weighted by number of bytes
    public int lengthTotal;  // count weighted by encodedLength
    
    // encoding variants
//    private long itemCount = 0;
//    public long hits = 0;  // misses = itemCount - hits
//    public long lt64 = 0;  // (non diff slot only) things coded outside of dictionary    
    public long diffEncoded = 0; // things not diff encoded = totalCount - diffEncoded
    public long valueLeDiff = 0; // things not diff encoded which could have been
//    public long total = 0;  
    
    // zip info
    public long beforeZip;   // should be same as lengthTotal;
    public long afterZip = -1;  // -1 means not zipped
    public long zipTime;
    public long deserializationTime;  // excluding unzipping
    
    public StatDetail(String name, 
                      boolean canBeNegative,
                      boolean inMainHeap,
                      int bytesPerCount) {
      this.canBeNegative = canBeNegative;
      this.bytesPerCount = bytesPerCount;
      this.name = name;
      if (canBeNegative) {
        cn = new int[MAX_NBR_ENCODE_LENGTH];
      } else {
        cn = null;
      }
      if (inMainHeap) {
        original = 0;  // main heap original computed outside of this mechanism
      }
    }
    
    public long getOriginal() {
      if (original == -1) {
        return countTotal * bytesPerCount;
      }
      else {
        return original;
      }
    }
    
    public void accum(StatDetail o) {
      for (int i = 0; i < c.length; i++) {
        c[i] += o.c[i];
        if (canBeNegative && (null != o.cn)) {
          cn[i] += o.cn[i];
        }
      }

      countTotal += o.countTotal;
      lengthTotal += o.lengthTotal;
      original = getOriginal();
      original += o.getOriginal();
      diffEncoded += o.diffEncoded;
      valueLeDiff += o.valueLeDiff;
      beforeZip += o.beforeZip;
      if (afterZip == -1) {
        afterZip = 0;
      }
      afterZip  += (o.afterZip == -1) ? o.beforeZip : o.afterZip;
      zipTime += o.zipTime;
      deserializationTime += o.deserializationTime;
    }
        
    public void incr(int encodedLength, boolean isNegative) {
      if (isNegative) {
        cn[encodedLength - 1] ++;
      }
      incr(encodedLength);
    }
    
    public void incr(int encodedLength) {
      c[encodedLength - 1] ++;    
      countTotal ++;
      lengthTotal += encodedLength;
    }
    
    /**
     * v is the number of bytes to incr counter 0 by
     * @param v -
     */
    public void incrNoCompression(int v) {
      c[bytesPerCount - 1] += v;
      countTotal += v;
      lengthTotal += v * bytesPerCount;
    }
    
    public String toString() {
      long tot = lengthTotal;
      if (tot == 0) {
        return String.format("Item: %25s%n", name);
      }
      String diff = (0 < diffEncoded) ?
          String.format(
              "%n                                                                  DiffEncoded(%%, %%v<diff): %,d(%.1f%% %.1f%%)", 
              diffEncoded, percent(diffEncoded, countTotal), percent(valueLeDiff, diffEncoded)) :
          "";
      String zp = (afterZip == -1) ? "" :
        String.format(" afterZip: %,7d(%4.1f%%), %,3d ms", afterZip, percent(afterZip, beforeZip), zipTime);
      
      String dt = (deserializationTime == 0) ? "" :
        String.format(" Deserialization time: %f", deserializationTime/1000F);
      
      StringBuilder sb = new StringBuilder();
      // find max index to include = first non-zero from end,  + 1
      int maxToInclude = maxIndexToZeros(c);
      for (int i = 0; i <= maxToInclude; i++) {
        sb.append((canBeNegative) ? 
            String.format(" %,d(%,d)", c[i], cn[i]) :
            String.format(" %,d", c[i]));
      }
      String totPct = (original == 0) ? 
          String.format("LengthTot: %,d", lengthTotal) :
          String.format("LengthTot: %,d(%.1f%%)", lengthTotal, percentCompr(lengthTotal));
      String histoDetails = String.format("[%s  Histo:%s]", totPct, sb);
      return String.format("Item: %25s %s %s %s %s%n",
            name, zp, dt, histoDetails, diff);
    }
    
    private float percentCompr(long totCompr) {
      return percent(totCompr, ((original == -1) || (original == 0)) ? countTotal * bytesPerCount : original);
    }
  }
  
  /** 
   * each instance of this class remembers a set of statDetail instances to
   * do bulk operations against that set of the statistics
   */
  public class AllStatDetails {
    final StatDetail[] allStatDetails;
    StatDetail aggr;
    final String name;
    
    public AllStatDetails (String aggrName, StatDetail ... someHeaps) {
      name = aggrName;
      allStatDetails = new StatDetail[someHeaps.length];
      aggr = new StatDetail(aggrName, CAN_BE_NEGATIVE, IN_MAIN_HEAP, 1);
      int i = 0;
      for (StatDetail sd : someHeaps) {
        allStatDetails[i++] = sd;
        aggr.accum(sd);
      }      
    }
    
    public AllStatDetails (String aggrName, SlotKind ... kinds) {
      this(aggrName, toStatDetails(kinds));
    }
        
    public void accum(AllStatDetails o) {
      for (int i = 0; i < allStatDetails.length; i++) {
        allStatDetails[i].accum(o.allStatDetails[i]);
      }
    }
    
    public void aggregate() {
      aggr = new StatDetail(name, CAN_BE_NEGATIVE, ! IN_MAIN_HEAP, 1);
      for (StatDetail sd : allStatDetails) {
        aggr.accum(sd);
      }            
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      for (StatDetail h : allStatDetails) {
        sb.append(h.toString());
      }
      return sb.toString();
    }
  }
       
  private static float percent(long a, long b) {
    if (a == 0) {
      return 0F;
    }
    if (b == 0) {
      return 100F;
    }
      
    return ((100F * a)/ b);
  }
        
  // all measures in counts or bytes
  public int header = 0;
  public long origAuxByteArrayRefs = 0;  // in bytes (incl boolean), 1 entry usually = 4 bytes
  public long origAuxShortArrayRefs = 0;
  public long origAuxLongArrayRefs = 0;
  public long origAuxBytes = 0;  // includes booleans, in bytes
  public long origAuxShorts = 0;  //in bytes
  public long origAuxLongs = 0;  // includes doubles, in bytes
  
  public long mainHeapFSs = 0;      // count of all feature structures
  
  public int stringsNbrCommon = 0;
  public long stringsCommonChars = 0;
  public long stringsSavedExact = 0;
  public long stringsSavedSubstr = 0;
  
  public long totalTime = 0;

    
  public final StatDetail[] statDetails = new StatDetail[SlotKind.values().length];
  {
    for (SlotKind kind : SlotKind.values()) {
      statDetails[kind.ordinal()] = new StatDetail(kind.toString(), 
                                           kind.canBeNegative,
                                           kind.inMainHeap,
                                           kind.elementSize);
    }
  }
  
  public final AllStatDetails allSlots =  
      new AllStatDetails("AllSlotKinds", 
          Slot_ArrayLength,
          Slot_HeapRef,
          Slot_Int,
          Slot_Byte,        // used only for arrays
          Slot_Short,       // used only for arrays
          Slot_TypeCode,
          Slot_StrOffset,
          Slot_StrLength,
          Slot_StrChars,
          Slot_Long_High,
          Slot_Long_Low,
          Slot_Float_Mantissa_Sign,
          Slot_Float_Exponent,
          Slot_Double_Mantissa_Sign,
          Slot_Double_Exponent,
          Slot_FsIndexes); 
  public final AllStatDetails strSlots = 
    new AllStatDetails("Strings",
        Slot_StrOffset,
        Slot_StrLength,
        Slot_StrChars);
  
//  public final ModHeaps modHeaps = new ModHeaps(modMainHeap, modByteHeap, modShortHeap, modLongHeap);
//  public final Str  strings = new Str(strOffsets, strLengths);
//  public final IndexedFSs indexedFSs = new IndexedFSs();
//  

  public SerializationMeasures() {
  }
  
  StatDetail[] toStatDetails(SlotKind[] kinds) {
    StatDetail[] sds= new StatDetail[kinds.length];
    int i = 0;
    for(SlotKind k : kinds) {
      sds[i++] = statDetails[k.ordinal()];
    }
    return sds;
  }

  /**
   * accumulate results for multiple files
   * @param o -
   */
  public void accum(SerializationMeasures o) {
    int i = 0;
    for (StatDetail sd : o.statDetails) {
      statDetails[i++].accum(sd);
    }
    origAuxByteArrayRefs += o.origAuxByteArrayRefs;
    origAuxShortArrayRefs += o.origAuxShortArrayRefs;
    origAuxLongArrayRefs += o.origAuxLongArrayRefs;
    header += o.header;
    mainHeapFSs += o.mainHeapFSs; 
    
    stringsNbrCommon += o.stringsNbrCommon;
    stringsCommonChars += o.stringsCommonChars;
    stringsSavedExact += o.stringsSavedExact;
    stringsSavedSubstr += o.stringsSavedSubstr;
  }
  
  public String toString() {
    // Strings
    
    long origStringChars = statDetails[Slot_StrChars.ordinal()].getOriginal();
    long origStringObjs = statDetails[Slot_StrLength.ordinal()].getOriginal() * 2;
    long origStringsTot = origStringChars +   // space for the chars 
            origStringObjs +                  // space for the offset and length
            (origStringObjs / 2);             // space for the refs to the string heap 
    
    
    allSlots.aggregate();
    strSlots.aggregate();
    
    long allOrig = statDetails[Slot_MainHeap.ordinal()].original +
                   origStringChars + origStringObjs +
                   origAuxBytes + 
                   origAuxShorts +
                   origAuxLongs;
                   
    long allB4Z = allSlots.aggr.lengthTotal;
    long strB4Z = strSlots.aggr.lengthTotal;
    
    long allTotZ = allSlots.aggr.afterZip;
    long strTotZ = strSlots.aggr.afterZip;       
    
    return String.format(
        "Summary: withZip: %,d(%.1f%%), without: %,d(%.1f%%)  zipTime: %,d ms  totalSerTime: %,d ms%n" +
        "  nonStrgs: withZip: %,d(%.1f%%), without: %,d(%.1f%%)%n" +
        "  Strings:  withZip: %,d(%.1f%%), without: %,d(%.1f%%)%n" +
    		"  MainHeap TotFS: %,d, StrCmnChars: %,d(%.1f%%), StrSavedExact: %,d  StrSavedSubstr: %,d%n" +
        "%s%n",
       allTotZ, percent(allTotZ, allOrig), allB4Z, percent(allB4Z, allOrig), allSlots.aggr.zipTime, totalTime,
       
       allTotZ - strTotZ, percent(allTotZ - strTotZ, allOrig - origStringsTot),
       allB4Z - strB4Z,   percent(allB4Z - strB4Z,   allOrig - origStringsTot),
       
       strTotZ, percent(strTotZ, origStringsTot),
       strB4Z,  percent(strB4Z,  origStringsTot),
            
       mainHeapFSs, stringsCommonChars, percent(stringsCommonChars, statDetails[Slot_StrChars.ordinal()].original),
       stringsSavedExact, stringsSavedSubstr,
       allSlots.toString()
    );
  }
 
}
