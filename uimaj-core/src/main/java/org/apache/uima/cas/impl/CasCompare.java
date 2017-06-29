package org.apache.uima.cas.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.Pair;
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

public class CasCompare {

  /**
   * Compare 2 CASes, with perhaps different type systems.
   * If the type systems are different, construct a type mapper and use that
   *   to selectively ignore types or features not in other type system
   *   
   * The Mapper is from CAS1 -&gt; CAS2  
   * 
   * When computing the things to compare from CAS1, filter to remove
   * feature structures not reachable via indexes or refs
   *   
   * The index definitions are not compared.
   * The indexes are used to locate the FSs to be compared.
   *   
   * @param c1 CAS to compare
   * @param c2 CAS to compare
   * @return true if equal (for types / features in both)
   */
  
  public static boolean compareCASes(CASImpl c1, CASImpl c2) {
    return new CasCompare(c1, c2).compareCASes();
  }
   
  private static class Prev {
    final ArrayList<TOP> fsList = new ArrayList<>();
    int cycleLen = -1;
    int cycleStart = -1;
    TOP prevCompareTop;
    
    void clear() {
      fsList.clear();
      cycleLen = -1;
      cycleStart = -1;
      prevCompareTop = null;
    }
    
    void rmvLast(TOP fs) {
      int toBeRemoved = fsList.size() - 1;
      if (toBeRemoved == usize()) {
        //debug
        if (cycleLen < 0) {
          System.out.println("debug cycleLen");
        }
        assert cycleLen >= 0;
        assert cycleStart >= 0;
        cycleLen = -1;
        cycleStart = -1;
      }
      fsList.remove(toBeRemoved);     
    }
    
    void addTop() {
      fsList.add(prevCompareTop);
      prevCompareTop = null;
    }
    
    void add(TOP fs) {
      if (cycleLen < 0) {
        int i = fsList.lastIndexOf(fs);
        if (i >= 0) {
          cycleLen = fsList.size() - i;
          cycleStart = i;
        }
      }
      fsList.add(fs);
    }
    
    int size() {
      return fsList.size();
    }
    
    int usize() {
      return cycleStart + cycleLen;
    }
  }
    
  /** 
   * Compare 2 CASes for equal
   */
    final private CASImpl c1;
    final private CASImpl c2;
    final private TypeSystemImpl ts1;      
    final private TypeSystemImpl ts2;
    
    private boolean compareStringArraysAsSets = false;
    private boolean compareArraysByElement = false;
    private boolean compareAll = false;
    /**
     * This is used for two things:
     *   First, used twice while sorting individual FS collections to be compared.
     *   Second, used when doing the comparison to break recursion if asked to compare the same two things while comaring them.
     */
    private final Set<Pair<TOP, TOP>> prevCompare = Collections.newSetFromMap(new HashMap<>());
    private final Prev prev1 = new Prev();
    private final Prev prev2 = new Prev();
    
//    private final Set<Pair<TOP, TOP>> miscompares = Collections.newSetFromMap(new HashMap<>());
          
    private TOP fs1, fs2;
    private boolean isSrcCas;  // used for sorting with a CAS, to differentiate between src and target CASes
    final private StringBuilder mismatchSb = new StringBuilder();
    private boolean inSortContext = false;
    
    private boolean isTypeMapping;
    private final CasTypeSystemMapper typeMapper;
          
  public CasCompare(CASImpl c1, CASImpl c2) {
    this.c1 = c1;
    this.c2 = c2;
    ts1 = c1.getTypeSystemImpl();
    ts2 = c2.getTypeSystemImpl();
    typeMapper = ts1.getTypeSystemMapper(ts2);
    isTypeMapping = (null != typeMapper);
  }
    
  public void compareStringArraysAsSets(boolean v) {
    compareStringArraysAsSets = v;
  }
  
  public void compareArraysByElement(boolean v) {
    compareArraysByElement = v;
  }
  
  public void compareAll(boolean v) {
    compareAll = v;
  }
  
  public boolean compareCASes() {
    boolean allOk = true;
    final List<TOP> c1FoundFSs;
    final List<TOP> c2FoundFSs;
    final boolean savedIsTypeMapping= isTypeMapping;
    mismatchSb.setLength(0);

    try {
      
//        processIndexedFeatureStructures(c1, false);
      Predicate<TOP> includeFilter = isTypeMapping ? (fs -> isTypeInTgt(fs)) : null;
      // get just the indexed ones
      c1FoundFSs = new AllFSs(c1, null, includeFilter, isTypeMapping ? typeMapper : null)
                        .getAllFSsAllViews_sofas()
                        .getAllFSs();
      
//        c1FoundFSs = fssToSerialize;  // all reachable FSs, filtered by CAS1 -> CAS2 type systems.
      
//        processIndexedFeatureStructures(c2, false);
      c2FoundFSs = new AllFSs(c2, null, null, null)
                     .getAllFSsAllViews_sofas()
                     .getAllFSs(); // get just the indexed ones.
      

      // if type systems are "isEqual()" still need to map because of feature validation testing
          
        int i1 = 0;
        int i2 = 0;
        final int sz1 = c1FoundFSs.size();
        final int sz2 = c2FoundFSs.size();
        
    
 
        isSrcCas = true;   // avoids sorting on types/features not present in ts2
      sort(c1FoundFSs);
      
      isSrcCas = false;  // avoids sorting on types/features not present in ts1
      sort(c2FoundFSs);
     
//      miscompares.clear();
      
      while (i1 < sz1 && i2 < sz2) {
        fs1 = c1FoundFSs.get(i1);  // assumes the elements are in same order??
        fs2 = c2FoundFSs.get(i2);

        clearPrevFss();
        prev1.prevCompareTop = fs1;
        prev2.prevCompareTop = fs2;

        if (isTypeMapping) {
          // skip compares for types that are missing in the other type system
          final boolean typeMissingIn1 = typeMapper.mapTypeTgt2Src(fs2._getTypeImpl()) == null;
          final boolean typeMissingIn2 = typeMapper.mapTypeSrc2Tgt(fs1._getTypeImpl()) == null;
          if (!typeMissingIn1 && !typeMissingIn2) {
            if (0 != compareFss()) {
//                fs1 = c1FoundFSs.get(159);
//                fs2 = c1FoundFSs.get(161);
//                inSortContext = true;
//                isSrcCas = true;
//                System.out.println(" " + compareFss());
//                fs1 = c2FoundFSs.get(17);
//                fs2 = c2FoundFSs.get(18);
//                System.out.println(" " + compareFss());
              //debug
//                dmp3sh("c1", c1, c1FoundFSs, -12567);
//                dmp3sh("c2", c2, c2FoundFSs, -12567);
//              inSortContext = true; 
//              isSrcCas = false; 
//              System.out.println("debug " + sortCompare(c2FoundFSs.get(159), c2FoundFSs.get(160)));
              mismatchFsDisplay();
              if (!compareAll) return false;
              allOk = false;
              int tc = fs1._getTypeImpl().compareTo(fs2._getTypeImpl());
              if (tc < 0) {
                System.out.print("skiping first to align types ");
                while (tc < 0 && i1 < sz1) {
                  i1++;
                  tc = c1FoundFSs.get(i1)._getTypeImpl().compareTo(fs2._getTypeImpl());
                  System.out.print(".");
                }
                System.out.println("");
              } else if (tc > 0) {
                System.out.print("skiping second to align types ");
                while (tc > 0 && i2 < sz2) {
                  i2++;
                  tc = fs1._getTypeImpl().compareTo(c2FoundFSs.get(i2)._getTypeImpl());
                  System.out.print(".");
                }
                System.out.println("");
              }
            }
            i1++;
            i2++;
            continue;
          }
          if (typeMissingIn1 && typeMissingIn2) {
            Misc.internalError();
            i1++;
            i2++;
            continue;
          }
          if (typeMissingIn1) {
            System.out.println("debug - type missing in 1, but test fails for refs");
            i2++;
            continue;
          }
          if (typeMissingIn2) {
            Misc.internalError(); 
            i1++;
            continue;
          }
        } else {  // not type mapping
          if (0 != compareFss()) {
            mismatchFsDisplay();
            if (!compareAll) return false;
            allOk = false;
            int tc = fs1._getTypeImpl().compareTo(fs2._getTypeImpl());
            if (tc < 0) {
              System.out.print("skiping first to align types ");
              while (tc < 0 && i1 < sz1) {
                i1++;
                tc = c1FoundFSs.get(i1)._getTypeImpl().compareTo(fs2._getTypeImpl());
                System.out.print(".");
              }
              System.out.println("");
            } else if (tc > 0) {
              System.out.print("skiping second to align types ");
              while (tc > 0 && i2 < sz2) {
                i2++;
                tc = fs1._getTypeImpl().compareTo(c2FoundFSs.get(i2)._getTypeImpl());
                System.out.print(".");
              }
              System.out.println("");
            }
          }
          i1++;
          i2++;
          continue;
        }
      }
      
      if (i1 == sz1 && i2 == sz2) {
        return allOk;  // end
      }
      
      if (isTypeMapping) {
        if (i1 < sz1) {
          System.err.format("%,d Feature Structures in CAS1 with no matches in CAS2, e.g. %s%n",
              sz1 - i1, c1FoundFSs.get(i1));
          return false;
        }

        while (i2 < sz2) {
          TOP fs = c2FoundFSs.get(i2);
          if (isTypeMapping && typeMapper.mapTypeTgt2Src(fs._getTypeImpl()) != null) {  // not a complete test, misses refs
            return false;  // have more FSs in c2 than in c1
          }
          i2++;
        }
        return true;
      }
      
      // not type mapping, and number of FS didn't match
      if (i1 < sz1) {
        System.err.format("CAS1 had %,d additional Feature Structures, e.g.: %s%n", sz1 - i1, c1FoundFSs.get(i1));
      } else {
        System.err.format("CAS2 had %,d additional Feature Structures, e.g.: %s%n", sz2 - i2, c2FoundFSs.get(i2));
      }
      return false;
    } finally {
      isTypeMapping = savedIsTypeMapping;
      clearPrevFss();
    }
  }
  
  private void clearPrevFss() {
    prevCompare.clear();
    prev1.clear();
    prev2.clear();
  }
  
  /**
   * 
   * @return in sort context return -1 or 1.  In other context also return 0
   */
  private int compareFss() {
//    if (!inSortContext) {
//      Pair<TOP, TOP> pair = new Pair<>(fs1, fs2);
//      if (miscompares.contains(pair)) {
//        //debug
//        System.out.println("debug skipping compare of " + fs1._id + " " + fs2._id);
//        return -1; 
//      }
//    }
    TypeImpl ti1 = fs1._getTypeImpl();
    TypeImpl ti2 = fs2._getTypeImpl();  // even if not type mapping, may be "equal" but not ==
    int r = 0;
    
    if (!inSortContext && isTypeMapping) {
      ti2 = typeMapper.mapTypeTgt2Src(ti2);
    }

    r = ti1.compareTo(ti2);
    if (r != 0) {
      if (!inSortContext) mismatchFs("Different Types"); // types mismatch
      return r;
    }

//      if (!ti1.getName().equals(ti2.getName())) {
//          return mismatchFs("Type names miscompare"); // types mismatch
//        }
//      }
        
    if (ti1.isArray()) {
      return compareFssArray();
    } 

//      //debug
//      if (fs1._id == 70)  
//        System.out.println("debug");
    for (FeatureImpl fi1 : ti1.getFeatureImpls()) {
      if (inSortContext && isTypeMapping) {
        if (isSrcCas && typeMapper.getTgtFeature(ti1, fi1) == null) {
          continue; // skip tests for features not in target type system
                    // so when comparing CASs, the src value won't cause a miscompare
        }
        if (!isSrcCas && typeMapper.getSrcFeature(ti1,  fi1) == null) {
          continue; // types/features belong to target in this case
        }
      }
      FeatureImpl fi2 = (!inSortContext && isTypeMapping) ? typeMapper.getTgtFeature(ti1, fi1) : fi1;
      if (fi2 != null) {
        r = compareSlot(fi1, fi2);
        if (0 != r) {
          if (!inSortContext) mismatchFs(fi1, fi2);
          return r;
        }
      } // else we skip the compare - no slot in tgt for src
    }
    return inSortContext
             ? Integer.compare(fs1._id, fs2._id)
             : 0;
  }
        
//    private int compareFssArray() {
//      int r = compareFssArray((CommonArrayFS) fs1, (CommonArrayFS) fs2);
//      if (r != 0) {
//        if (!inSortContext) mismatchFs();
//      }
//      return r;
//    }
  
  private int compareFssArray() {
    CommonArrayFS a1 = (CommonArrayFS) fs1;
    CommonArrayFS a2 = (CommonArrayFS) fs2;
    int r;
    
    int len1 = a1.size();
    int len2 = a2.size();
    r = Integer.compare(len1,  len2);
    if (r != 0) {
      if (!inSortContext) mismatchFs();
      return r;
    }
    
    if (inSortContext && !compareArraysByElement) {
      // quick approximate comparison of arrays, for sort purposes
      return Integer.compare(((FeatureStructureImplC)fs1)._id, 
                             ((FeatureStructureImplC)fs2)._id);
    }
    TypeImpl ti = ((FeatureStructureImplC)a1)._getTypeImpl();
    SlotKind kind = ti.getComponentSlotKind();
    
    switch(kind) {
    case Slot_BooleanRef: r = compareAllArrayElements(len1, i -> Boolean.compare(
                                                                    ((BooleanArray)a1).get(i),
                                                                    ((BooleanArray)a2).get(i)));
      break;
    case Slot_ByteRef:    r = compareAllArrayElements(len1, i -> Byte.compare(
                                                                    ((ByteArray   )a1).get(i),
                                                                    ((ByteArray   )a2).get(i)));
      break;
    case Slot_ShortRef:   r = compareAllArrayElements(len1, i -> Short.compare(
                                                                    ((ShortArray  )a1).get(i),
                                                                    ((ShortArray  )a2).get(i)));
      break;
    case Slot_Int:     r = compareAllArrayElements(len1, i -> Integer.compare(
                                                                    ((IntegerArray)a1).get(i),
                                                                    ((IntegerArray)a2).get(i)));
      break;
    case Slot_LongRef:  r = compareAllArrayElements(len1, i -> Long.compare(
                                                                    ((LongArray   )a1).get(i),
                                                                    ((LongArray   )a2).get(i)));
      break;

    // don't compare floats / doubles directly - because two "equal" NaN are defined to miscompare
    case Slot_Float: r = compareAllArrayElements(len1, i -> Integer.compare(
                                                                    CASImpl.float2int(((FloatArray  )a1).get(i)), 
                                                                    CASImpl.float2int(((FloatArray  )a2).get(i))));
      break;

    // don't compare floats / doubles directly - because two "equal" NaN are defined to miscompare
    case Slot_DoubleRef: r = compareAllArrayElements(len1, i -> Long.compare(
                                                                    CASImpl.double2long(((DoubleArray)a1).get(i)), 
                                                                    CASImpl.double2long(((DoubleArray)a2).get(i))));
      break;
    case Slot_HeapRef: r = compareAllArrayElements(len1, i -> compareRefs(
                                                                    ((FSArray)a1).get(i), 
                                                                    ((FSArray)a2).get(i), null));
      break;
    case Slot_StrRef: r = compareStringArraysAsSets
                            ? compareAsSets((StringArray) a1, (StringArray) a2)
                            : compareAllArrayElements(len1, i -> compareStringsWithNull(
                                                                    ((StringArray)a1).get(i), 
                                                                    ((StringArray)a2).get(i)));
      break;
    default: 
      Misc.internalError(); r = 0;  // only to avoid a compile error
    }
    
    return r;
  }
          
  private int compareSlot(FeatureImpl fi1, FeatureImpl fi2) {
    SlotKind kind = fi1.getSlotKind();
    switch (kind) {
    case Slot_Int: return Integer.compare(fs1._getIntValueNc(fi1), fs2._getIntValueNc(fi2)); 
    case Slot_Short: return Short.compare(fs1._getShortValueNc(fi1), fs2._getShortValueNc(fi2));
    case Slot_Boolean: return Boolean.compare(fs1._getBooleanValueNc(fi1), fs2._getBooleanValueNc(fi2));
    case Slot_Byte: return Byte.compare(fs1._getByteValueNc(fi1), fs2._getByteValueNc(fi2));
          // don't compare floats / doubles directly - the NaN is defined to miscompare
    case Slot_Float: return Integer.compare(CASImpl.float2int(fs1._getFloatValueNc(fi1)), CASImpl.float2int(fs2._getFloatValueNc(fi2)));
    case Slot_HeapRef: return compareRefs(fs1._getFeatureValueNc(fi1), fs2._getFeatureValueNc(fi2), fi1);
    case Slot_StrRef: return compareStringsWithNull(fs1._getStringValueNc(fi1), fs2._getStringValueNc(fi2));
    case Slot_LongRef: return Long.compare(fs1._getLongValueNc(fi1), fs2._getLongValueNc(fi2));
          // don't compare floats / doubles directly - the NaN is defined to miscompare
    case Slot_DoubleRef: return Long.compare(Double.doubleToRawLongBits(fs1._getDoubleValueNc(fi1)), Double.doubleToRawLongBits(fs2._getDoubleValueNc(fi2)));
    default: Misc.internalError(); return 0;     
    }
  }
  
  /**
   * Two uses cases supported:
   *   - comparing for sorting (within on type system)
   *      -- goal is to be able to compare two CASes
   *         --- ordering must guarantee that the equal FSs appear in the
   *         --- same order
   *   - comparing two FSs (maybe from different CASes)
   *     -- supporting missing types and features 
   *        -- happens when the two type systems are different
   *        -- the missing types and features are ignored in the comparison
   * 
   * Different reference chains
   *   This compare routine may be called recursively
   *     - use case: FS(a) has slot which is ref to 
   *                   FS(b) which has slot which is ref to
   *                     FS(c) 
   *       -- the type of a, b, c may all be different.
   *   
   *   Two reference chains for the two arguments may have different structures
   *     - Difference in two ways:  
   *       -- length of unique (same fs_id) FSs
   *       -- length of loop (if one exists at the end reached so far)
   *       
   *   IMPORTANT: the following 2 chains have different lengths, but this
   *   won't be discovered if the recursive descent stops too soon:
   *     - a -> b -> c  ( -> b )
   *     - c -> b ( -> c)
   *   At the 2nd recursion, we have b vs b, but haven't explored the chain 
   *   deeply enough to know the first one has length 3, and the 2nd length 2.            
   *       
   * Meaning of comparision of two refs:  
   *   - recursively defined
   *   - captures notion of reference chain differences
   *     -- means if two refs compare 0, the result may still be
   *        non-0 if the reference chains to get to these are different
   *       -- first compare on length of unique FSs
   *       -- if ==, compare on length of loop
   *   - if comparing (use case 2, two different type systems) with 
   *     type not existing in other type system, skip (treat as 0).
   * 
   * Complexities: the type rfs1 may not be in the target type system.
   *   For this case - treat rfs2 == null as "equal", rfs2 != null as not equal (always gt)
   *   Is assymetrical (?) - same logic isn't applied for reverse case.
   * @param rfs1 -
   * @param rfs2 -
   * @param fi -
   * @return -
   */
  private int compareRefs(final TOP rfs1, final TOP rfs2,  FeatureImpl fi) {
    if (rfs1 == null) {
      if (rfs2 != null) {
        return (!inSortContext && isTypeMapping &&
                typeMapper.mapTypeTgt2Src(rfs2._getTypeImpl()) == null)
                  ? 0   // no source type for this target type, treat as equal
                  : -1;
      }
      return 0;  // both are null.  no loops in ref chain possible 
    }

    // rfs1 != null at this point
    
     if (rfs2 == null) {
      return (!inSortContext && isTypeMapping &&
              typeMapper.mapTypeSrc2Tgt(rfs1._getTypeImpl()) == null)
                ? 0 // no target type for this target type, treat as equal
                : 1;  
    }
    
    // both are not null
    // next commented out to enable finding length of chain      
//      if (inSortContext && rfs1._id == rfs2._id) {  
//        return compareRefResult(rfs1, rfs2);
//      }
    
    // not in sort context, or ids mismatch, but might have the same "value"
    // do a recursive check 
    
    Pair<TOP, TOP> refs = new Pair<TOP, TOP>(rfs1, rfs2);
     if (!prevCompare.add(refs)) {  // if set already had the element
      return compareRefResult(rfs1, rfs2); // stop recursion, return based on loops
    }
      
    // need special handling to detect cycles lengths that are back to the original
    if (prev1.prevCompareTop != null) {
      prev1.addTop();
      prev2.addTop();
    }
    
    prev1.add(rfs1);
    prev2.add(rfs2);
    
    TOP savedFs1 = fs1;
    TOP savedFs2 = fs2;
    
    fs1 = rfs1;
    fs2 = rfs2;
    try {
      return compareFss();
    } finally {
      prevCompare.remove(refs);   
      prev1.rmvLast(rfs1);
      prev2.rmvLast(rfs2);
      
      fs1 = savedFs1;
      fs2 = savedFs2;   
    }
  }
  
  /**
   * Returning because recursion detected a loop.
   * 
   * @param rfs1 -
   * @param rfs2 -
   * @return - -1 if ref chain 1 length < 2 or is the same length but loop length 1 < 2
   *            1 if ref chaing 1 length > 2 or is the same length but loop length 1 > 2
   *            0 if ref chain lengths are the same and loop length is the same
   *             unless: inSortContext:
   *               case:   x -> y -> x          y -> x -> y   
   *               break tie using parent ._id guarenteed not to be equal 
   */
  private int compareRefResult(TOP rfs1, TOP rfs2) {
    if (prev1.size() > 0) {  // always true if have any recursion, false otherwise
      prev1.add(rfs1);
      prev2.add(rfs2);
      
      int r = Integer.compare(prev1.usize(), prev2.usize());
      if (r != 0) {
        return r;
      }
      r = Integer.compare(prev1.cycleLen, prev2.cycleLen);
      if (r != 0) {
        return r;
      }
      // don't handle case here
      //   of x -> y -> z   vs 
      //      w -> y -> z, sorting
      //        case of x -> y -> z -> x    vs a -> b -> c -> a   sorting or not sorting, comparing 
//        if (inSortContext) {
//          return Integer.compare(fs1._id, fs2._id);  // must be parent.
//        }
    }
    return 0;
  }
      
  private int compareAllArrayElements(int len, IntUnaryOperator c) {
    int r = 0;
    for (int i = 0; i < len; i++) {
      r = c.applyAsInt(i);
      if (r != 0) {
        if (!inSortContext) mismatchFs("Comparing array of length " + len);
        return r;
      }
    }
    return inSortContext ? Integer.compare(fs1._id, fs2._id) : 0;
  }
  
  private int compareAsSets(StringArray a1, StringArray a2) {
    String[] a1a = a1._getTheArray().clone();
    String[] a2a = a2._getTheArray().clone();
    Arrays.sort(a1a);
    Arrays.sort(a2a);
    final String[] a1s = a1a;
    final String[] a2s = a2a;
    return compareAllArrayElements(a1a.length, i -> compareStringsWithNull(a1s[i], a2s[i]));
  }
  
//    private boolean areStringsEqual(String s1, String s2) {
//      if (null == s1) {
//        return null == s2;
//      }
//      return (null == s2) ? false : s1.equals(s2);
//    }     
  
  private int compareStringsWithNull(String s1, String s2) {
    if (null == s1) {
      return (null == s2) ? 0 : -1;
    }
    if (null == s2) {
      return 1;
    }
    return s1.compareTo(s2);
  }
  
//    private int skipOverTgtFSsNotInSrc(
//        int[] heap, int heapEnd, int nextFsIndex, CasTypeSystemMapper typeMapper) {
//      final TypeSystemImpl ts = typeMapper.tsTgt;
//      for (; nextFsIndex < heapEnd;) {
//        final int tCode = heap[nextFsIndex];
//        if (typeMapper.mapTypeCodeTgt2Src(tCode) != 0) { 
//          break;
//        }
//        nextFsIndex += incrToNextFs(heap, nextFsIndex, ts.getTypeInfo(tCode));
//      }
//      return nextFsIndex;
//    }
//    
//    public void initSrcTgtIdMapsAndStringsCompare () {
//
//      int iTgtHeap = isTypeMapping ? skipOverTgtFSsNotInSrc(c2heap, c2end, 1, typeMapper) : 1;
//      
//      
//      for (int iSrcHeap = 1; iSrcHeap < c1end;) {
//        final int tCode = c1heap[iSrcHeap];
//        final int tgtTypeCode = isTypeMapping ? typeMapper.mapTypeCodeSrc2Tgt(tCode) : tCode;
//        final boolean isIncludedType = (tgtTypeCode != 0);
//        
//        // record info for type
//        fsStartIndexes.addItemId(iSrcHeap, iTgtHeap, isIncludedType);  // maps src heap to tgt seq
//        
//        // for features in type - 
//        //    strings: accumulate those strings that are in the target, if optimizeStrings != null
//        //      strings either in array, or in individual values
//        //    byte (array), short (array), long/double (instance or array): record if entries in aux array are skipped
//        //      (not in the target).  Note the recording will be in a non-ordered manner (due to possible updates by
//        //       previous delta deserialization)
//        final TypeInfo srcTypeInfo = ts1.getTypeInfo(tCode);
//        final TypeInfo tgtTypeInfo = (isTypeMapping && isIncludedType) ? ts2.getTypeInfo(tgtTypeCode) : srcTypeInfo;
//              
//        // Advance to next Feature Structure, in both source and target heap frame of reference
//        if (isIncludedType) {
//          final int deltaTgtHeap = incrToNextFs(c1heap, iSrcHeap, tgtTypeInfo);
//          iTgtHeap += deltaTgtHeap;
//          if (isTypeMapping) {
//            iTgtHeap = skipOverTgtFSsNotInSrc(c2heap, c2end, iTgtHeap, typeMapper);
//          }
//        }
//        iSrcHeap += incrToNextFs(c1heap, iSrcHeap, srcTypeInfo);
//      }
//    } 

  private void mismatchFsDisplay() {
    System.err.println(mismatchSb.toString());
    mismatchSb.setLength(0);
  }
  
  private void mismatchFs() {
    mismatchSb.append(String.format("Mismatched Feature Structures:%n %s%n %s%n", fs1, fs2));
//    // debug
//    System.out.println("adding to miscompares: " + fs1._id + " " + fs2._id);
//    miscompares.add(new Pair<>(fs1, fs2));
  }

//    private boolean mismatchFs(int i1, int i2) {
//      System.err.format("Mismatched Feature Structures in srcSlot %d, tgtSlot %d%n %s%n %s%n", 
//          i1, i2, dumpHeapFs(c1, c1heapIndex, ts1), dumpHeapFs(c2, c2heapIndex, ts2));
//      return false;
//    }
  
//    private void mismatchFs(Feature fi) {
//      mismatchSb.append(String.format("Mismatched Feature Structures in feature %s%n %s%n %s%n", 
//          fi.getShortName(), fs1, fs2));
//    }
  
  private void mismatchFs(Feature fi, Feature fi2) {
    String mapmsg = fi.equals(fi2) 
                      ? ""
                      : "which mapped to target feature " + fi2.getShortName() + " ";
    mismatchSb.append(String.format("Mismatched Feature Structures in feature %s %s%n %s%n %s%n", 
        fi.getShortName(), mapmsg, fs1, fs2));
//    miscompares.add(new Pair<>(fs1, fs2));
  }
  
  private void mismatchFs(String msg) {
//      //debug
//      if (fs1._id == fs2._id) {
//        System.out.println("debug");
//        throw new RuntimeException();
//      }
    mismatchSb.append(String.format("Mismatched Feature Structures, %s%n %s%n %s%n", 
        msg, fs1, fs2));
//    miscompares.add(new Pair<>(fs1, fs2));
  }
      
  private void sort(List<TOP> fss) {
    inSortContext = true;
    try {
      Collections.sort(fss,  
          (afs1, afs2) -> sortCompare(afs1, afs2));
    } finally {
      inSortContext = false;
    }
  }
  
  /**
   * Used for sorting within one type system, for two instances of the same type
   * 
   * Uses field isSrcCas (boolean) to differentiate when being used to sort for srcCas vs tgtCas
   * 
   * When sorting where type mapping is happening between source and target CASs, skip compares for
   * features which are not in the opposite CAS.
   * 
   * @param scFs1 -
   * @param scFs2 -
   * @return -
   */
  private int sortCompare(TOP scFs1, TOP scFs2) {        
    // do before sorting
    clearPrevFss();
//    miscompares.clear();
    
    prev1.prevCompareTop = fs1 = scFs1;
    prev2.prevCompareTop = fs2 = scFs2;
    int r = compareFss();
    prev1.prevCompareTop = null;
    prev2.prevCompareTop = null;
    return r;
  }
    
  private boolean isTypeInTgt(TOP fs) {
    return !isTypeMapping || (null != typeMapper.mapTypeSrc2Tgt(fs._getTypeImpl()));
  }

}
