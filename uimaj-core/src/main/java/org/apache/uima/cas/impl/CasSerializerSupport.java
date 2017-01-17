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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.UimaSerializable;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.internal.util.XmlElementName;
import org.apache.uima.jcas.cas.CommonList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.NonEmptyList;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.Logger;
import org.apache.uima.util.MessageReport;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * CAS serializer support for XMI and JSON formats.
 * 
 * There are multiple use cases.
 *   1) normal - the consumer is independent of UIMA
 *        - (maybe) support for delta serialization
 *   2) service calls:  
 *        - support deserialization with out-of-type-system set-aside, and subsequent serialization with re-merging
 *        - guarantee of using same xmi:id's as were deserialized when serializing
 *        - support for delta serialization
 * 
 * There is an outer class (one instance per "configuration" - reusable after configuration, and
 * an inner class - one per serialize call.
 * 
 * These classes are the common parts of serialization between XMI and JSON, mainly having to do with
 *   1) enquuing the FS to be serialized
 *   2) serializing according to their types and features
 *     
 * 
 * Methods marked public are not for public use but are that way to permit
 * other users of this class in other packages to "see" these methods.
 *   
 *   XmiCasSerializer                              JsonCasSerializer
 *       Instance                                      Instance
 *      css ref -------&gt;   CasSerializerSupport   &lt;------ css ref
 *          
 *               
 *   XmiDocSerializer                                JsonDocSerializer            
 *       Instance                                       Instance 
 * (1 per serialize action)                         (1 per serialize action)
 *       cds ref -------&gt;     CasDocSerializer  &lt;-------   cds ref
 *                           csss points back
 *                      
 *                      
 * Construction:
 *   new Xmi/JsonCasSerializer 
 *      initializes css with new CasSerializerSupport
 *      
 *   serialize method creates a new Xmi/JsonDocSerializer inner class
 *      constructor creates a new CasDocSerializer,    
 *                      
 * Use Cases and Algorithms
 *   Support set-aside for out-of-type-system FS on deserialization (record in shareData)
 *     implies can't determine sharing status of things ref'd by features; need to depend on 
 *       multiple-refs-allowed flag.
 *       If multiple-refs found during serialization for feat marked non-shared, unshare these (make
 *         2 serializations, one or more inplace, for example.  
 *         Perhaps not considered an error.
 *     implies need (for non-delta case) to send all FSs that were deserialized - some may be ref'd by oots elements
 *       ** Could ** not do this if no oots elements, but could break some assumptions
 *       and this only would apply to non-delta - not worth doing
 *       
 *       
 *                      
 */

public class CasSerializerSupport {
   
  // Special "type class" codes for list types. The LowLevelCAS.ll_getTypeClass() method
  // returns type classes for primitives and arrays, but not lists (which are just ordinary FS types
  // as far as the CAS is concerned). The serialization treats lists specially, however, and
  // so needs its own type codes for these.
  public static final int TYPE_CLASS_INTLIST = 101;

  public static final int TYPE_CLASS_FLOATLIST = 102;

  public static final int TYPE_CLASS_STRINGLIST = 103;

  public static final int TYPE_CLASS_FSLIST = 104;
    
  public static int PP_LINE_LENGTH = 120;
  public static int PP_ELEMENTS = 30;  // number of elements to do before nl
  
  public static AtomicInteger errorCount = new AtomicInteger(0);
  
  /**
   * Comparator that just uses short name
   * Public for access by JsonCasSerializer where it's needed for a binary search
   * https://issues.apache.org/jira/browse/UIMA-5171
   */
  public final static Comparator<TypeImpl> COMPARATOR_SHORT_TYPENAME = new Comparator<TypeImpl>() {
    public int compare(TypeImpl object1, TypeImpl object2) {
      return object1.getShortName().compareTo(object2.getShortName());
    }
  };
   
  TypeSystemImpl filterTypeSystem;
    
  ErrorHandler errorHandler = null;

  // UIMA logger, to which we may write warnings
  Logger logger;

  public boolean isFormattedOutput;  // true for pretty printing
     
  /***********************************************
   *         C O N S T R U C T O R S             *  
   ***********************************************/

  public CasSerializerSupport() {}
  
  /********************************************************
   *   Routines to set/reset configuration                *
   ********************************************************/
  /**
   * set or reset the pretty print flag (default is false)
   * @param pp true to do pretty printing of output
   * @return the original instance, possibly updated
   */
  public CasSerializerSupport setPrettyPrint(boolean pp) {
    this.isFormattedOutput = pp;
    return this;
  }
  
  /**
   * pass in a type system to use for filtering what gets serialized;
   * only those types and features which are defined this type system are included.
   * @param ts the filter
   * @return the original instance, possibly updated
   */
  public CasSerializerSupport setFilterTypes(TypeSystemImpl ts) {
    this.filterTypeSystem = ts;
    return this;
  }
  
  // for testing
  public TypeSystemImpl getFilterTypes() {
    return filterTypeSystem;
  }
  
     // not done here, done on serialize call, different (typically) for each call
//  /**
//   * set the Marker to specify delta cas serialization
//   * @param m - the marker
//   * @return the original instance, possibly updated
//   */
//  public CasSerializerSupport setDeltaCas(Marker m, XmiSerializationSharedData sharedData) {
//    this.marker = (MarkerImpl) m;
//    this.sharedData = sharedData;
//    return this;
//  }
  
  /**
   * set an error handler to receive information about errors
   * @param eh the error handler
   * @return the original instance, possibly updated
   */
  public CasSerializerSupport setErrorHandler(ErrorHandler eh) {
    this.errorHandler = eh;
    return this;
  }
  
  
  /***********************************************
   * Methods used to serialize items
   * Separate implementations for JSON and Xmi
   *
   ***********************************************/
  public static abstract class CasSerializerSupportSerialize {
    
    abstract protected void initializeNamespaces();
        
    abstract protected void checkForNameCollision(XmlElementName xmlElementName);
        
    abstract protected void addNameSpace(XmlElementName xmlElementName);  

    abstract protected XmlElementName uimaTypeName2XmiElementName(String typeName);

    abstract protected void writeFeatureStructures(int elementCount) throws Exception;
    
    abstract protected void writeViews() throws Exception;
    
    abstract protected void writeView(Sofa sofa, Collection<TOP> members) throws Exception;
    
    abstract protected void writeView(Sofa sofa, Collection<TOP> added, Collection<TOP> deleted, Collection<TOP> reindexed) throws Exception;  
    
    /**
     * 
     * @param fs -
     * @param typeCode -
     * @return true if writing out referenced items (JSON)
     * @throws Exception -
     */
    abstract protected boolean writeFsStart(TOP fs, int typeCode) throws Exception;
    
    abstract protected void writeFs(TOP fs, int typeCode) throws Exception;
    
    abstract protected void writeListsAsIndividualFSs(TOP fs, int typeCode) throws Exception;
    
    abstract protected void writeArrays(TOP fsarray, int typeCode, int typeClass) throws Exception;
    
    abstract protected void writeEndOfIndividualFs() throws Exception;  
    
    abstract protected void writeEndOfSerialization() throws Exception;
    
    abstract protected void writeFsRef(TOP fs) throws Exception;
  }
  
  /**
   * Use an inner class to hold the data for serializing a CAS. Each call to serialize() creates its
   * own instance.
   * 
   * package private to allow a test case to access
   * not static to share the logger and the initializing values (could be changed) 
   */
  public class CasDocSerializer {

    // The CAS we're serializing.
    public final  CASImpl cas;
    
    public final TypeSystemImpl tsi;

    /** 
     * set of FSs that have been enqueued to be serialized
     *  Computed during "enqueue" phase, prior to encoding
     *  Used to prevent duplicate enqueuing
     */    
    public final Set<TOP> visited_not_yet_written = Collections.newSetFromMap(new IdentityHashMap<>()); 
    
    /**
     * set of FSs that have multiple references
     * This is for JSON which is computing the multi-refs, not depending on the setting in a feature.
     */
    public final Set<TOP> multiRefFSs; 
    
    /* *********************************************
     * FSs that need to be serialized because they're 
     *   a) in an index
     *   b) in the set of previously serialized FS which have ids (that is, they weren't previously embedded)
     *   c) (delta only) have a feature which has an embedded value some part of which changed (no id)
     *   
     *   d) the set of FSs that are reachable via FSrefs from the above 3 sets
     */
    
    public List<TOP> previouslySerializedFSs = null;
    
    public List<TOP> modifiedEmbeddedValueFSs = null;
    
    /**
     * Array of Lists of all FS that are indexed in some view (other than sofas).  Array indexed by view.
     */
    public final List<TOP>[] indexedFSs;

    /**
     * FSs not in an index, but only being serialized becaused they're referenced.  Exception: the sofa's are here.
     */
    private final Deque<TOP> queue;

    
    // utilities for dealing with CAS list types
//    public final ListUtils listUtils;
        
    public XmlElementName[] typeCode2namespaceNames; // array, indexed by type code, giving XMI names for each type
    
    private final BitSet typeUsed;  // identifies types being serialized, a subset of all possible types
        
    public boolean needNameSpaces = true; // may be false; currently for JSON only

    /**
     * map from a namespace expanded form to the namespace prefix, to identify potential collisions when
     *   generating a namespace string
     */
    public final Map<String, String> nsUriToPrefixMap = new HashMap<String, String>();
           
    /**
     * the set of all namespace prefixes used, to disallow some if they are 
     *   in use already in set-aside data (xmi serialization) being merged back in
     */
    public final Set<String> nsPrefixesUsed = new HashSet<String>();
    
    /**
     * Used to tell if a FS was created before or after mark.
     */
    public final MarkerImpl marker;

    /**
     * for Delta serialization, holds the info gathered from deserialization needed for delta serialization 
     * and for handling out-of-type-system data for both plain and delta serialization
     */
    public final XmiSerializationSharedData sharedData;

    /**
     * Whether the serializer needs to serialize only the deltas, that is, new FSs created after
     * mark represented by Marker object and preexisting FSs and Views that have been
     * modified. Set to true if Marker object is not null and CASImpl object of this serialize
     * matches the CASImpl in Marker object.
     */
    public final boolean isDelta;
    
    /**
     * Whether the serializer needs to check for filtered-out types/features. Set to true if type
     * system of CAS does not match type system that was passed to constructor of serializer.
     */
    public final boolean isFiltering;

    private TypeImpl[] sortedUsedTypes;
    
    private final ErrorHandler errorHandler2;
    
    public TypeSystemImpl filterTypeSystem_inner;
    
    // map to reduce string usage by reusing equal string representations; lives just for one serialize call
    private final Map<String, String> uniqueStrings = new HashMap<String, String>();

    public final boolean isFormattedOutput_inner;
    
    private final CasSerializerSupportSerialize csss;

    /***********************************************
     *         C O N S T R U C T O R S             *  
     ***********************************************/
    /**
     * 
     * @param ch -
     * @param cas -
     * @param sharedData -
     * @param marker -
     * @param csss -
     */
    public CasDocSerializer(ContentHandler ch, CASImpl cas, XmiSerializationSharedData sharedData, MarkerImpl marker, CasSerializerSupportSerialize csss) {
      this(ch, cas,sharedData, marker, csss, false);
    }
    
    public CasDocSerializer(ContentHandler ch, CASImpl cas, XmiSerializationSharedData sharedData, MarkerImpl marker, CasSerializerSupportSerialize csss, boolean trackMultiRefs) {
      this.cas = cas;
      this.csss = csss;
      this.sharedData = sharedData;

      // copy outer class values into final inner ones, to keep the outer thread-safe
      filterTypeSystem_inner = CasSerializerSupport.this.filterTypeSystem; 
      isFormattedOutput_inner = CasSerializerSupport.this.isFormattedOutput; 
      this.marker = marker;
      errorHandler2 = CasSerializerSupport.this.errorHandler;

      tsi = cas.getTypeSystemImpl();
      queue = new ArrayDeque<>();
      indexedFSs = (List<TOP>[]) new List<?>[cas.getViewCount()];  // number of views
//      listUtils = new ListUtils(cas, logger, errorHandler);
      typeUsed = new BitSet();

      isFiltering = filterTypeSystem_inner != null && filterTypeSystem_inner != tsi;
      if (marker != null && !marker.isValid()) {
  	    throw new CASRuntimeException(CASRuntimeException.INVALID_MARKER, "Invalid Marker.");
      }
      isDelta = marker != null;
      multiRefFSs = (trackMultiRefs) ? Collections.newSetFromMap(new IdentityHashMap<>()) : null;
    }
        
    // TODO: internationalize
    private void reportMultiRefWarning(FeatureImpl fi) throws SAXException {
      String message = String.format("Feature %s is marked multipleReferencesAllowed=false, but it has"
          + " multiple references.  These will be serialized in duplicate.", 
          fi.getName());
      MessageReport.decreasingWithTrace(errorCount, message, logger);
      if (this.errorHandler2 != null) {
        this.errorHandler2.warning(new SAXParseException(message, null));
      }
    }

    /**
     * Starts serialization
     * @throws Exception -
     */
    public void serialize() throws Exception {    
      typeCode2namespaceNames = new XmlElementName[tsi.getLargestTypeCode() + 1];
      
      // reset caches in case some things modified between calls to serialize for same instance of serializer
      sortedUsedTypes = null;
      typeUsed.clear();
      Arrays.fill(indexedFSs, null);
      queue.clear();
           
      csss.initializeNamespaces();
              
      int iElementCount = 1; // start at 1 to account for special NULL object

      enqueueIndexed();  // done first - to insure this has priority  

      enqueueIncoming(); //make sure we enqueue every FS that was deserialized into this CAS
                         // needed to support Out Of Typesystem data
      enqueueNonsharedMultivaluedFS();  // needed for delta serialization of modified embedded lists/arrays
      enqueueFeaturesOfIndexed(); // and incoming and modified embedded refs
      
      iElementCount += (previouslySerializedFSs == null) ? 0 : previouslySerializedFSs.size();
      iElementCount += (modifiedEmbeddedValueFSs == null) ? 0 : modifiedEmbeddedValueFSs.size();
      for (List<TOP> fss : indexedFSs) {  
        iElementCount += (fss == null) ? 0 : fss.size();
      }
      iElementCount += queue.size();

      FSIndex<TOP> sofaIndex = cas.getBaseCAS().indexRepository.getIndex(CAS.SOFA_INDEX_NAME);
      if (!isDelta) {
      	iElementCount += (sofaIndex.size()); // one View element per sofa
      	iElementCount += getElementCountForSharedData();
      } else {
        int numViews = cas.getViewCount();
        for (int sofaNum = 1; sofaNum <= numViews; sofaNum++) {
          FSIndexRepositoryImpl loopIR = (FSIndexRepositoryImpl) cas.getBaseCAS().getSofaIndexRepository(sofaNum);
          if (loopIR != null && loopIR.isModified()) {
            iElementCount++;
          }
        }
      }
      
      csss.writeFeatureStructures(iElementCount);
      
      csss.writeViews();
      
      csss.writeEndOfSerialization();
    }
    
    /**
     * 
     * @param sofaNum - starts at 1
     * @return the sofa FS, or null
     */
    public Sofa getSofa(int sofaNum) {  
      if (sofaNum != 1 || cas.isInitialSofaCreated()) { //skip if initial view && no Sofa yet
                                                        // all non-initial-views must have a sofa
        return ((CASImpl)cas.getView(sofaNum)).getSofaRef();
      }
      return null;
    }

    public void writeViewsCommons() throws Exception {
      // Get indexes for each SofaFS in the CAS
      int numViews = cas.getViewCount();
    
      
      for (int sofaNum = 1; sofaNum <= numViews; sofaNum++) {
        FSIndexRepositoryImpl loopIR = (FSIndexRepositoryImpl) cas.getBaseCAS().getSofaIndexRepository(sofaNum);
        final Sofa sofa = getSofa(sofaNum);
        if (loopIR != null) {
          if (!isDelta) {
            Collection<TOP> fsarray = loopIR.getIndexedFSs();
            csss.writeView(sofa, fsarray);
          } else { // is Delta Cas
        	  if (sofaNum != 1 && this.marker.isNew(sofa)) {
        	    // for views created after mark (initial view never is - it is always created with the CAS)
        	    // write out the view as new
        	    List<TOP> fsarray = loopIR.getIndexedFSs();
              csss.writeView(sofa, fsarray);
        	  } else if (loopIR.isModified()) {
        	    csss.writeView(sofa, loopIR.getAddedFSs(), loopIR.getDeletedFSs(), loopIR.getReindexedFSs());
          	}
          } 
        }
      }
    }                 
    
    // sort is by shortname of type
    public TypeImpl[] getSortedUsedTypes() {
      if (null == sortedUsedTypes) {
        sortedUsedTypes = new TypeImpl[typeUsed.cardinality()];
        int i = 0;
        for (TypeImpl ti : getUsedTypesIterable()) {
          sortedUsedTypes[i++] = ti;
        }
        Arrays.sort(sortedUsedTypes, COMPARATOR_SHORT_TYPENAME);     
      }
      return sortedUsedTypes;
    }
    
    private Iterable<TypeImpl> getUsedTypesIterable() {
      return new Iterable<TypeImpl>() {
        public Iterator<TypeImpl> iterator() {
          return new Iterator<TypeImpl>() {
            private int i = 0;
            
            public boolean hasNext() {
              return typeUsed.nextSetBit(i) >= 0;
            }

            public TypeImpl next() {
              final int next_i = typeUsed.nextSetBit(i);
              if (next_i < 0) {
                throw new NoSuchElementException();
              }
              i = next_i + 1;
              return (TypeImpl) tsi.ll_getTypeForCode(next_i);
            }

            public void remove() {
              throw new UnsupportedOperationException();
            } 
          };
        }
      };
    }
     
//    private StringPair[] getSortedPrefixUri() {
//      StringPair[] r = new StringPair[nsUriToPrefixMap.size()];
//      int i = 0;
//      for (Map.Entry<String,String> e : nsUriToPrefixMap.entrySet()) {
//        r[i++] = new StringPair(e.getValue(), e.getKey());
//      }
//      Arrays.sort(r);
//      return r;
//    }
    
    /**
     * Enqueues all FS that are stored in the sharedData's id map.
     * This map is populated during the previous deserialization.  This method
     * is used to make sure that all incoming FS are echoed in the next
     * serialization.  It is required if there are out-of-type FSs that 
     * are being merged back into the serialized form; those might
     * reference some of these.
     */
    private void enqueueIncoming() {
      if (sharedData == null)
        return;
      TOP[] fss = this.sharedData.getAndSortByIdAllFSsInIdMap();
      previouslySerializedFSs = new ArrayList<>();
      
      for (TOP fs : fss) {
        // skip enque for null and for unmodified fss if delta
        if (fs == null || 
            (isDelta && !marker.isModified(fs))) {
          continue;
        }
                
        // is the first instance, but skip if delta and not modified or above the line or filtered
        int typeCode = enqueueCommon(fs);
        if (typeCode == -1) {
          continue;
        }
        previouslySerializedFSs.add(fs);
      }
    }

     
    /**
     * add the indexed FSs onto the indexedFSs by view.
     * add the SofaFSs onto the by-ref queue
     */
    private void enqueueIndexed()  {
      FSIndexRepositoryImpl ir = (FSIndexRepositoryImpl) cas.getBaseCAS().getBaseIndexRepository();
      List<TOP> fss = ir.getIndexedFSs();  // only sofas
      try {
        for (TOP fs : fss) {
          enqueue(fs);  // put Sofa on by-ref queue
        }
      } catch (SAXException e) {
        throw new RuntimeException("Internal error - should never happen", e);
      }

      // FSIndex sofaIndex = cas.getBaseCAS().indexRepository.getIndex(CAS.SOFA_INDEX_NAME);
      // FSIterator iterator = sofaIndex.iterator();
      // // Get indexes for each SofaFS in the CAS
      // while (iterator.isValid())
      int numViews = cas.getViewCount();
      for (int sofaNum = 1; sofaNum <= numViews; sofaNum++) {
        // SofaFS sofa = (SofaFS) iterator.get();
        // int sofaNum = sofa.getSofaRef();
        // iterator.moveToNext();
        FSIndexRepositoryImpl loopIR = (FSIndexRepositoryImpl) cas.getBaseCAS()
                .getSofaIndexRepository(sofaNum);
        if (loopIR != null) {
          List<TOP> items = loopIR.getIndexedFSs();
          for (TOP item : items) {
            enqueueIndexedFs(sofaNum, item);
          }
        }
      }
    }
    
    /** 
     * When serializing Delta CAS,
     * enqueue encompassing FS of nonshared multivalued FS that have been modified.
     * The embedded nonshared-multivalued item could be a list or an array
     */
    private void enqueueNonsharedMultivaluedFS() {
      if (sharedData == null || !isDelta)
          return;
      TOP[] fss = sharedData.getNonsharedMulitValuedFSs();
      modifiedEmbeddedValueFSs = new ArrayList<>();
      
      for (TOP fs : fss) {
        if (marker.isModified(fs)) {
          TOP encompassingFs = sharedData.getEncompassingFS(fs);
          assert null != encompassingFs;
          if (-1 != enqueueCommonWithoutDeltaAndFilteringCheck(encompassingFs)) {  // only to set type used info and check if already enqueued
            modifiedEmbeddedValueFSs.add(encompassingFs);
          }
        }    
      }      
    }

    /**
     * Enqueue everything reachable from features of indexed FSs.
     */
    private void enqueueFeaturesOfIndexed() throws SAXException {
      if (null != previouslySerializedFSs) {
        enqueueFeaturesOfFSs(previouslySerializedFSs);
      }
      if (null != modifiedEmbeddedValueFSs) {
        enqueueFeaturesOfFSs(modifiedEmbeddedValueFSs);
      }
      for (List<TOP> fss : indexedFSs) {
        if (fss != null) {
          enqueueFeaturesOfFSs(fss);
        }
      }
    }
    
    private void enqueueFeaturesOfFSs(final List<TOP> fss) throws SAXException {
      for (TOP fs : fss) {
        enqueueFeatures(fs);
      }
    }

    int enqueueCommon(TOP fs) {
      return enqueueCommon(fs, true);
    }
    
    int enqueueCommonWithoutDeltaAndFilteringCheck(TOP fs) {
      return enqueueCommon(fs, false);
    }
    
    private int enqueueCommon(TOP fs, boolean doDeltaAndFilteringCheck) {
//      // debug 
//      if (null == fs) {
//        System.out.println("debug null fs");
//        new Throwable().printStackTrace();
//        int i = 0;
//        while (true) {
//          try {
//            Thread.sleep(10000);
//          } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//          }
//          i++;
//          if (i % 10 == 0) {
//            System.err.println("debug sleeping");
//          }
//        }
//      }

      
//      final int typeCode = cas.getHeapValue(addr);
//      assert(typeCode != 0);
      if (doDeltaAndFilteringCheck) {
        if (isDelta) {
          if (!marker.isNew(fs) && !marker.isModified(fs)) {
            return -1;
          }
        }
      
        if (isFiltering) {
          String typeName = fs._getTypeImpl().getName();
          if (filterTypeSystem_inner.getType(typeName) == null) {
            return -1; // this type is not in the target type system
          }
        }
      }
      
      // We set visited only if we're going to enqueue this.
      //   (In other words, please don't move this up in this method)
      //   This handles the use case:
      //   delta cas; element is not modified, but at some later point, we determine
      //   an embedded feature value (array or list) is modified, which requires we serialize out this
      //   fs as if it was modified.

      if (!visited_not_yet_written.add(fs)) {
        // was already visited; means this FS has multiple references, either from FS feature(s) or indexes or both
        if (null != multiRefFSs) {
          boolean wasAdded = multiRefFSs.add(fs);
          if (wasAdded) {
            queue.add(fs);  // if was in indexed set before, isn't in the queue set, but needs to be
          }
        }
        return -1;
      }
      
      final int typeCode = fs._getTypeCode();
      boolean alreadySet = typeUsed.get(typeCode);
      if (!alreadySet) {
        typeUsed.set(typeCode);

        String typeName = fs._getTypeImpl().getName();
        XmlElementName newXel = csss.uimaTypeName2XmiElementName(typeName);

        if (!needNameSpaces) {   // means if name spaces are not not always needed, then we have to check for collision
          csss.checkForNameCollision(newXel);   // executed for JSON code
        }        
        typeCode2namespaceNames[typeCode] = newXel;
      }  
      return typeCode;
    }    
    /*
     * Enqueues an indexed FS. Does NOT enqueue features at this point.
     * Doesn't enqueue non-modified FS when delta
     */
    void enqueueIndexedFs(int viewNumber, TOP fs) {
      if (enqueueCommon(fs) != -1) {
        List<TOP> fss = indexedFSs[viewNumber - 1];
        if (null == fss) {
          indexedFSs[viewNumber - 1] = fss = new ArrayList<>();
        }
        fss.add(fs);
      }
    }

    /**
     * Enqueue an FS, and everything reachable from it.
     * 
     * This call is recursive with enqueueFeatures, \
     * and an arbitrary long chain can get stack overflow error.
     * Probably should fix this someday. See https://issues.apache.org/jira/browse/UIMA-106
     * 
     * @param addr
     *          The FS address.
     * @throws SAXException 
     */
    private void enqueue(TOP fs) throws SAXException {  
      if (null == fs) {
        return;
      }
 
      int typeCode = enqueueCommon(fs);
      if (typeCode == -1) {
        return;  
      }
      queue.add(fs);
      enqueueFeatures(fs);
      // Also, for FSArrays enqueue the elements
      if (fs instanceof FSArray) { //TODO: won't get parameterized arrays? no, there are no parameterized arrays in the impl
        enqueueFSArrayElements((FSArray) fs);
      }
    }
            
    /**
     * 
     * @param curNode
     * @param featCode
     * @return true if OK, false if found cycle or multi-ref
     * @throws SAXException
     */
    private boolean isListElementsMultiplyReferenced(TOP listNode) throws SAXException {
      boolean foundCycle = false;
      CommonList curNode = (CommonList) listNode;
      while (curNode instanceof NonEmptyList) {  // stop on end or 0
        if (!visited_not_yet_written.add((TOP) curNode)) {
          foundCycle = true;
          break;
        }
        curNode = curNode.getCommonTail();
      }
      return foundCycle;
    }
    
    
    private boolean isMultiRef_enqueue(FeatureImpl fi, TOP featVal, boolean alreadyVisited, boolean isListNode, boolean isListFeat) throws SAXException {
      if (multiRefFSs == null) {
        
        // dynamic embedding (a JSON feature) is turned off - compute static embedding just for lists and arrays
        boolean multiRefAllowed = fi.isMultipleReferencesAllowed() || isListNode;
        if (!multiRefAllowed) {
          // two cases: a list or non-list
          // if a list, check/mark all the nodes in the list
          if ((isListFeat && isListElementsMultiplyReferenced(featVal)) ||
              (!isListFeat && alreadyVisited)) {
              reportMultiRefWarning(fi);              
          } else {
            if (!isListFeat) {  // already added visited for list nodes
              visited_not_yet_written.add(featVal);
            }
          }
          return false; // because static, multi-ref not allowed, no need to enqueue
        } else {  // is multiRefAllowed or in list node
          return true; // static, multi-ref allowed or in list node, enqueue
        }
      }
      
      // doing dynamic determination of multi-refs
      if (alreadyVisited) {
        return !multiRefFSs.contains(featVal); // enqueue in the "queue" section, first time this happens
      }
      return true;  // enqueue this item.  May or may not be eventually written embedded
                    // but we enqueue to track multi-use
    }

    /**
     * Enqueue all FSs reachable from features of the given FS.
     * 
     * @param addr
     *          address of an FS
     * @param typeCode
     *          type of the FS
     * @param insideListNode
     *          true iff the enclosing FS (addr) is a list type
     */
    private void enqueueFeatures(TOP fs) throws SAXException {
//    if (fs.id() == 65) {
//      System.out.println("debug 356");
//    }

      /**
       * Handle FSArrays
       */
      if (fs instanceof FSArray) {
        
        TOP[] theArray = ((FSArray)fs)._getTheArray();

        for (TOP elem : theArray) {
          if (isFiltering &&
              (null == filterTypeSystem_inner.getType(elem._getTypeImpl().getName()))) {
            continue;  // skip because not in filter type system
          }
          if (elem != null) {
            enqueue(elem);
          }
        }
        
        return;
      }
      
      
      boolean insideListNode = fs instanceof CommonList;

      if (fs instanceof UimaSerializable) {
        ((UimaSerializable)fs)._save_to_cas_data();
      }
      for (FeatureImpl fi : fs._getTypeImpl().getFeatureImpls()) {
        if (isFiltering && filterTypeSystem_inner.getFeatureByFullName(fi.getName()) == null) { 
          // skip features that aren't in the target type system
            continue;
        }
//        final int featAddr = addr + cas.getFeatureOffset(feat);
//        final int featVal = cas.getHeapValue(featAddr);
//        if (featVal == CASImpl.NULL) {      // null feature values do not refer to any other FS
//          continue;
//        }

        // enqueue behavior depends on range type of feature
        final int fsClass = fi.rangeTypeClass;
        switch (fsClass) {
          case LowLevelCAS.TYPE_CLASS_FS: {
            enqueue(fs.getFeatureValue(fi));
            break;
          }
          case LowLevelCAS.TYPE_CLASS_INTARRAY:
          case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
          case LowLevelCAS.TYPE_CLASS_STRINGARRAY:
          case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY:
          case LowLevelCAS.TYPE_CLASS_BYTEARRAY:
          case LowLevelCAS.TYPE_CLASS_SHORTARRAY:
          case LowLevelCAS.TYPE_CLASS_LONGARRAY:
          case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY:
          case LowLevelCAS.TYPE_CLASS_FSARRAY: {
            TOP array = fs.getFeatureValue(fi);  // can be null
            // we enqueue arrays if:
            //   when statically using multipleReferencesAllowed flag:
            //     when that says it's multiply referenced; 
            //       otherwise, we skip enqueueing it because it will
            //       be picked up when serializing the feature
            //   when dynamically computing multiple-refs: we enqueue it
            //   unless already enqueued, in order to pick up any multiple refs
            final boolean alreadyVisited = visited_not_yet_written.contains(array);
            if (isMultiRef_enqueue(fi, array, alreadyVisited, false, false)) {
              enqueue(array);  // will add to queue list 1st time multi-ref detected
            // otherwise, it is singly referenced (so far) and will be embedded
            //   (or has already been enqueued, in dynamic embedding mode), so don't enqueue
            } else if (array instanceof FSArray && !alreadyVisited) {
              // enqueue any FSs reachable from an FSArray
              enqueueFSArrayElements((FSArray) array);
            }
            break;
          }
          case TYPE_CLASS_INTLIST:
          case TYPE_CLASS_FLOATLIST:
          case TYPE_CLASS_STRINGLIST:
          case TYPE_CLASS_FSLIST: {
            // we enqueue lists if:
            //   when statically using multipleReferencesAllowed flag:
            //     when that says it's multiply referenced or 
            //               we're inside a list which was earlier multiply referenced 
            //       otherwise, we skip enqueueing it because it will
            //       be picked up when serializing the feature
            //   when dynamically computing multiple-refs: we enqueue it
            //   unless already enqueued, in order to pick up any multiple refs
            TOP startOfList_node = fs.getFeatureValue(fi);
            if (null == startOfList_node) {
              // the feature, whose type is one of the lists, has a null value, so there's nothing to enqueue
              break;
            }
            final boolean alreadyVisited = visited_not_yet_written.contains(startOfList_node);
            if (isMultiRef_enqueue(fi, startOfList_node, alreadyVisited, insideListNode, true)) {
              enqueue(startOfList_node);
            } else if (startOfList_node instanceof FSList && !alreadyVisited) {
              // also, we need to enqueue any FSs reachable from an FSList
              enqueueFSListElements((FSList) startOfList_node);
            }
            break;
          }
        }
      }  // end of loop over all features
    }

    /**
     * Enqueues all FS reachable from an FSArray.
     * 
     * @param addr
     *          Address of an FSArray
     */
    private void enqueueFSArrayElements(FSArray fsArray) throws SAXException {
       for (TOP elem : fsArray._getTheArray()) {
        if (elem != null) {
          enqueue(elem);
        }
      }
    }

    /**
     * Enqueues all Head values of FSList reachable from an FSList. 
     * This does NOT include the list nodes themselves.
     * 
     * @param addr
     *          Address of an FSList
     */
    private void enqueueFSListElements(FSList node) throws SAXException {
      node.walkList( n -> enqueue(((NonEmptyFSList)n).getHead()), null);
    }

    /*
     * Encode the indexed FS in the queue.
     */
    public void encodeIndexed() throws Exception {
      if (null != previouslySerializedFSs) {
        encodeFSs(previouslySerializedFSs);
      }
      if (null != modifiedEmbeddedValueFSs) {
        encodeFSs(modifiedEmbeddedValueFSs);
      }
      for (List<TOP> fss : indexedFSs) {
        if (fss != null) {
          encodeFSs(fss);
        }
      }
    }
    
    private void encodeFSs(final List<TOP> fss) throws Exception {
      for (TOP fs : fss) {
        encodeFS(fs);
      }
    }

    /*
     * Encode all other enqueued (non-indexed) FSs.
     * The queue is read out in FiFo order.
     * This insures that FsLists which are only 
     *   referenced via a single FS ref, get 
     *   encoded as [ x x x ] format rather than
     *   as individual FSs (because the individual
     *   items are also in the queue as items, but
     *   later).  The isWritten test prevents dupl writes
     */
    public void encodeQueued() throws Exception {
   
      for (TOP fs :  queue) {
        // for some serializers, things could be enqueued multiple times in the ref queue
        // so check if already written, and if so, skip
        //    Case where this happens: JSON serialization with dynamically determined single ref embedding
        //    - have to enqueue to check if multiple refs, even if embedding eventually
        if (visited_not_yet_written.contains(fs)) {
          if (null != multiRefFSs && !multiRefFSs.contains(fs)) {
            continue;  // skip writing embeddable item (for JSON dynamic embedding) from Q; will be written from reference
          }
          encodeFS(fs);
        }
      }
    }
    

//    public Integer[] collectAllFeatureStructures() {
//      final int indexedSize = indexedFSs.size();
//      final int qSize = queue.size();
//      final int rLen = indexedSize + queue.size();
//      Integer[] r = new Integer[rLen];
//      int i = 0;
//      for (; i < indexedSize; i++) {
//        r[i] = indexedFSs.get(i);
//      }
//      for (int j = 0; j < qSize; j++) {
//        r[i++] =  queue.get(j);
//      }
//      return r;
//    }
          
    /** 
     * Called for JSon Serialization
     * Sort a view, by type and then by begin/end asc/des for subtypes of Annotation,
     *  then by id
     */
    public final Comparator<TOP> sortFssByType = 
        new Comparator<TOP>() {
          public int compare(TOP fs1, TOP fs2) {
            int c = Integer.compare(fs1._getTypeImpl().getCode(), fs2._getTypeImpl().getCode());
            if (c != 0) {
              return c;
            }
//            final boolean hasSofa = tsi.subsumes(tsi.annotBaseTypeCode, typeCode1);
//            if (hasSofa) {
//              c = compareFeat(o1, o2, tsi.annotSofaFeatCode);
//              if (c != 0) {
//                return c;
//              }

            if (fs1 instanceof Annotation) {
              Annotation fs1a = (Annotation) fs1;
              Annotation fs2a = (Annotation) fs2;
              c = Integer.compare(fs1a.getBegin(), fs2a.getBegin());
              return (c != 0) ? c : Integer.compare(fs2a.getEnd(), fs1a.getEnd()); // reverse order
            }
            // not annotation
            return Integer.compare(fs1._id, fs2._id);  // return in @id order
          }
      };
      
    /**
     * Encode an individual FS.
     * 
     * Json has 2 encodings   
     *  For type:
     *  "typeName" : [ { "@id" : 123,  feat : value .... },
     *                 { "@id" : 456,  feat : value .... },
     *                 ...
     *               ],
     *      ... 
     *        
     *  For id:
     *  "nnnn" : {"@type" : typeName ; feat : value ...}
     *     
     *  For cases where the top level type is an array or list, there is
     *  a generated feature name, "@collection" whose value is 
     *  the list or array of values associated with that type.
     *   
     * @param fs the FS to be encoded.
     * @throws SAXException passthru
     */
    public void encodeFS(TOP fs) throws Exception {
      final int typeCode = fs._getTypeImpl().getCode();

      final int typeClass = classifyType(fs._getTypeImpl());
      boolean isIndexId = csss.writeFsStart(fs, typeCode);
      
      if (!isIndexId && multiRefFSs != null && multiRefFSs.contains(fs)) {
        csss.writeFsRef(fs);        
      } else {
        visited_not_yet_written.remove(fs);  // mark as written
        switch (typeClass) {
          case LowLevelCAS.TYPE_CLASS_FS: 
            csss.writeFs(fs, typeCode);
            break;
          
            
          case TYPE_CLASS_INTLIST:
          case TYPE_CLASS_FLOATLIST:
          case TYPE_CLASS_STRINGLIST:
          case TYPE_CLASS_FSLIST: 
            csss.writeListsAsIndividualFSs(fs, typeCode);
            break;
                  
          case LowLevelCAS.TYPE_CLASS_FSARRAY:
          case LowLevelCAS.TYPE_CLASS_INTARRAY:
          case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
          case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY:
          case LowLevelCAS.TYPE_CLASS_BYTEARRAY:
          case LowLevelCAS.TYPE_CLASS_SHORTARRAY:
          case LowLevelCAS.TYPE_CLASS_LONGARRAY:
          case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY:
          case LowLevelCAS.TYPE_CLASS_STRINGARRAY:
            csss.writeArrays(fs, typeCode, typeClass);
            break;
          
          default: 
            throw new RuntimeException("Error classifying FS type.");
        }
        
        csss.writeEndOfIndividualFs();
      }
    }

    int getElementCountForSharedData() {
      return (sharedData == null) ? 0 : sharedData.getOutOfTypeSystemElements().size();
    }    
    
    /**
     * Get the XMI ID to use for an FS.
     * 
     * @param fs the FS
     * @return XMI ID or null
     */
    public String getXmiId(TOP fs) {
      int v = getXmiIdAsInt(fs);
      return (v == 0) ? null : Integer.toString(v);
    }
    
    public int getXmiIdAsInt(TOP fs) {
      if (fs == null) {
        return 0;
      }
      if (isFiltering && null == filterTypeSystem_inner.getType(fs._getTypeImpl().getName())) { // return as null any references to types not in target TS
          return 0;
      }
      
      if (sharedData == null) {
        // in the absence of outside information, just use the FS address
        return fs._id;
      } else {
        return sharedData.getXmiIdAsInt(fs);
      }
      
    }

    public String getNameSpacePrefix(String uimaTypeName, String nsUri, int lastDotIndex) {
      // determine what namespace prefix to use
      String prefix = nsUriToPrefixMap.get(nsUri);
      if (prefix == null) {
        if (lastDotIndex != -1) { // have namespace 
          int secondLastDotIndex = uimaTypeName.lastIndexOf('.', lastDotIndex-1);
          prefix = uimaTypeName.substring(secondLastDotIndex + 1, lastDotIndex);
        } else {
          prefix = "noNamespace"; // is correct for older XMI standard too
        }
        // make sure this prefix hasn't already been used for some other namespace
        // including out-of-type-system types (for XmiCasSerializer)
        if (nsPrefixesUsed.contains(prefix)) {
          String basePrefix = prefix;
          int num = 2;
          while (nsPrefixesUsed.contains(basePrefix + num)) {
            num++;
          }
          prefix = basePrefix + num;
        }
        nsUriToPrefixMap.put(nsUri, prefix);
        nsPrefixesUsed.add(prefix);
      }
      return prefix;
    }
    /*
     *  convert to shared string, without interning, reduce GCs
     */
    public String getUniqueString(String s) { 
      String u = uniqueStrings.get(s);
      if (null == u) {
        u = s;
        uniqueStrings.put(s, s);
      }
      return u;
    }
    
    public String getTypeNameFromXmlElementName(XmlElementName xe) {
      final String nsUri = xe.nsUri;
      if (nsUri == null || nsUri.length() == 0) {
        throw new UnsupportedOperationException();
      }
      
      final int pfx = XmiCasSerializer.URIPFX.length;
      final int sfx = XmiCasSerializer.URISFX.length;
      
      String r = (nsUri.startsWith(XmiCasSerializer.DEFAULT_NAMESPACE_URI)) ? 
          "" :
          nsUri.substring(pfx, nsUri.length() - sfx);
      r = r.replace('/', '.');
      
      return r + xe.localName;
    }
    
    public boolean isStaticMultiRef(FeatureImpl fi) {
      return fi.isMultipleReferencesAllowed();
    }


  }  
  
  /**
   * Classifies a type. This returns an integer code identifying the type as one of the primitive
   * types, one of the array types, one of the list types, or a generic FS type (anything else).
   * <p>
   * The {@link LowLevelCAS#ll_getTypeClass(int)} method classifies primitives and array types,
   * but does not have a special classification for list types, which we need for XMI
   * serialization. Therefore, in addition to the type codes defined on {@link LowLevelCAS}, this
   * method can return one of the type codes TYPE_CLASS_INTLIST, TYPE_CLASS_FLOATLIST,
   * TYPE_CLASS_STRINGLIST, or TYPE_CLASS_FSLIST.
   * 
   * @param ti the type to classify
   * @return one of the TYPE_CLASS codes defined on {@link LowLevelCAS} or on this interface.
   */
  public static final int classifyType(TypeImpl ti) {
    switch (ti.getCode()) {
    case TypeSystemConstants.intListTypeCode: return TYPE_CLASS_INTLIST;
    case TypeSystemConstants.floatListTypeCode: return TYPE_CLASS_FLOATLIST;
    case TypeSystemConstants.stringListTypeCode: return TYPE_CLASS_STRINGLIST;
    case TypeSystemConstants.fsListTypeCode: return TYPE_CLASS_FSLIST;
    default : return TypeSystemImpl.getTypeClass(ti);
    }
  }
}
