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

import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.PositiveIntSet;
import org.apache.uima.internal.util.PositiveIntSet_impl;
import org.apache.uima.internal.util.XmlElementName;
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
 * Enqueuing:
 *   There are two styles
 *     - enqueueCommon: does **NOT** recursively enqueue features
 *     - enqueue: calls enqueueCommon and then recursively enqueues features
 *     
 *   enqueueCommon is called (bypassing enqueue) to defer scanning references 
 *   
 *   Order and target of enqueuing:
 *     - things in the index 
 *       -- put on "queue"
 *       -- first, the sofa's (which are the only things indexed in base view)
 *       -- next, for each view, for each item, the FSs, but **NOT** following any feature/array refs
 *     - things not in the index, but deserialized (incoming)
 *       -- put on previouslySerializedFSs, no recursive descent for features  
 *     - (delta) enqueueNonsharedMultivaluedFS (lists and arrays)
 *       -- put on modifiedEmbeddedValueFSs, no recursive descent for features
 *       
 *     - recursive descent for        
 *       -- things in previouslySerializedFSs,
 *       -- things in modifiedEmbeddedValueFSs
 *       -- things in the index
 *
 *       The recursive descent is recursive, and an arbitrary long chain can get stack overflow error.
 *       TODO Probably should fix this someday. See https://issues.apache.org/jira/browse/UIMA-106 *                      
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
    
    abstract protected void writeView(int sofaAddr, int[] members) throws Exception;
    
    abstract protected void writeView(int sofaAddr, int[] added, int[] deleted, int[] reindexed) throws Exception;  
    
    /**
     * 
     * @param addr -
     * @param typeCode -
     * @return true if writing out referenced items (JSON)
     * @throws Exception -
     */
    abstract protected boolean writeFsStart(int addr, int typeCode) throws Exception;
    
    abstract protected void writeFs(int addr, int typeCode) throws Exception;
    
    abstract protected void writeListsAsIndividualFSs(int addr, int typeCode) throws Exception;
    
    abstract protected void writeArrays(int addr, int typeCode, int typeClass) throws Exception;
    
    abstract protected void writeEndOfIndividualFs() throws Exception;  
    
    abstract protected void writeEndOfSerialization() throws Exception;
    
    abstract protected void writeFsRef(int addr) throws Exception;
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
     * set of FSs that have been visited and enqueued to be serialized
     *   - exception: arrays and lists which are "inline" are put into this set,
     *     but are not enqueued to be serialized.
     *     
     *   - FSs added to this, during "enqueue" phase, prior to encoding
     *     
     * uses:    
     *   - for Arrays and Lists, used to detect multi-refs
     *   - for Lists, used to detect loops
     *   - during enqueuing phase, prevent multiple enqueuings
     *   - during encoding phase, to prevent multiple encodings 
     *  
     *  Public for use by JsonCasSerializer
     */    
    public final PositiveIntSet_impl visited_not_yet_written; 
    
    /**
     * Set of array or list FSs referenced from features marked as multipleReferencesAllowed,
     *   - which have previously been serialized "inline"
     *   - which now need to be serialized as separate items
     *   
     * Set during enqueue scanning, to handle the case where the
     * "visited_not_yet_written" set may have already recorded that this FS is 
     * already processed for enqueueing, but it is an array or list item which was being
     * put "in-line" and no element is being written.
     * 
     * It has array or list elements where the item needs to be enqueued onto the "queue" list.
     * 
     * Use: limit the put-onto-queue list to one time
     */
    private final PositiveIntSet_impl enqueued_multiRef_arrays_or_lists = new PositiveIntSet_impl();

    /**
     * Set of FSs that have multiple references
     * Has an entry for each FS (not just array or list FSs) which is (from some point on) being serialized as a multi-ref,
     *   that is, is **not** being serialized (any more) using the special notation for arrays and lists
     *   or, for JSON, **not** being serialized using the embedded notation
     * This is for JSON which is computing the multi-refs, not depending on the setting in a feature.
     * This is also for xmi, to enable adding to "queue" (once) for each FSs of this kind.
     * 
     * Used: 
     *   - limit the number of times this is put onto the queue to 1.
     *   - skip encoding of items on "queue" if not in this Set (maybe not needed? 8/2017 mis)
     *   - serialize if not in indexed set, dynamic ref == true, and in this set (otherwise serialize only from ref)
     */
    public final PositiveIntSet multiRefFSs;
    
    /**
     * Set to true for JSON configuration of using dynamic multi-ref detection for arrays and lists
     */
    public final boolean isDynamicMultiRef;
    /* *********************************************
     * FSs that need to be serialized because they're 
     *   a) in an index
     *   b) in the set of previously serialized FS which have ids (that is, they weren't previously embedded)
     *   c) (delta only) have a feature which has an embedded value some part of which changed (no id)
     *   
     *   d) the set of FSs that are reachable via FSrefs from the above 3 sets
     */
    
    public IntVector previouslySerializedFSs = null;
    
    public IntVector modifiedEmbeddedValueFSs = null;
    
    // All FSs that are in an index somewhere.
    public final IntVector[] indexedFSs;

    // only referenced FSs.
    private final IntVector queue;

    
    // utilities for dealing with CAS list types
    public final ListUtils listUtils;
        
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
    
    private final ErrorHandler errorHandler;
    
    public TypeSystemImpl filterTypeSystem;
    
    // map to reduce string usage by reusing equal string representations; lives just for one serialize call
    private final Map<String, String> uniqueStrings = new HashMap<String, String>();

    public final boolean isFormattedOutput;
    
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
      filterTypeSystem = CasSerializerSupport.this.filterTypeSystem; 
      isFormattedOutput = CasSerializerSupport.this.isFormattedOutput; 
      this.marker = marker;
      errorHandler = CasSerializerSupport.this.errorHandler;

      tsi = cas.getTypeSystemImpl();
      visited_not_yet_written = new PositiveIntSet_impl();
      queue = new IntVector();
      indexedFSs = new IntVector[cas.getBaseSofaCount()];  // number of views
      listUtils = new ListUtils(cas, logger, errorHandler);
      typeUsed = new BitSet();

      isFiltering = filterTypeSystem != null && filterTypeSystem != tsi;
      if (marker != null && !marker.isValid()) {
  	    CASRuntimeException exception = new CASRuntimeException(
  	        CASRuntimeException.INVALID_MARKER, new String[] { "Invalid Marker." });
    	  throw exception;
      }
      isDelta = marker != null;
      multiRefFSs = new PositiveIntSet_impl();
      isDynamicMultiRef = trackMultiRefs;
    }
        
    // TODO: internationalize
    private void reportMultiRefWarning(int featCode) throws SAXException {
      String message = String.format("Feature %s is marked multipleReferencesAllowed=false, but it has"
          + " multiple references.  These will be serialized in duplicate.", 
          tsi.ll_getFeatureForCode(featCode).getName());
      MessageReport.decreasingWithTrace(errorCount, message, logger);
      if (this.errorHandler != null) {
        this.errorHandler.warning(new SAXParseException(message, null));
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
      queue.removeAllElements();
           
      csss.initializeNamespaces();
              
      int iElementCount = 1; // start at 1 to account for special NULL object

      enqueueIndexed();  // done first - to insure this has priority  

      enqueueIncoming(); //make sure we enqueue every FS that was deserialized into this CAS
                         // needed to support Out Of Typesystem data
      enqueueNonsharedMultivaluedFS();  // needed for delta serialization of modified embedded lists/arrays
      enqueueFeaturesOfIndexed(); // and incoming and modified embedded refs
      iElementCount += (previouslySerializedFSs == null) ? 0 : previouslySerializedFSs.size();
      iElementCount += (modifiedEmbeddedValueFSs == null) ? 0 : modifiedEmbeddedValueFSs.size();
      for (IntVector fss : indexedFSs) {
        iElementCount += (fss == null) ? 0 : fss.size();
      }
      iElementCount += queue.size();

      FSIndex<FeatureStructure> sofaIndex = cas.getBaseCAS().indexRepository.getIndex(CAS.SOFA_INDEX_NAME);
      if (!isDelta) {
      	iElementCount += (sofaIndex.size()); // one View element per sofa
      	iElementCount += getElementCountForSharedData();
      } else {
        int numViews = cas.getBaseSofaCount();
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
     * @return the addr of the sofa FS, or 0
     */
    public int getSofaAddr(int sofaNum) {  
      if (sofaNum != 1 || cas.isInitialSofaCreated()) { //skip if initial view && no Sofa yet
                                                        // all non-initial-views must have a sofa
        return ((CASImpl)cas.getView(sofaNum)).getSofaRef();
      }
      return 0;
    }

    public void writeViewsCommons() throws Exception {
      // Get indexes for each SofaFS in the CAS
      int numViews = cas.getBaseSofaCount();
      
      for (int sofaNum = 1; sofaNum <= numViews; sofaNum++) {
        FSIndexRepositoryImpl loopIR = (FSIndexRepositoryImpl) cas.getBaseCAS().getSofaIndexRepository(sofaNum);
        final int sofaAddr = getSofaAddr(sofaNum);
        if (loopIR != null) {
          if (!isDelta) {
            int[] fsarray = loopIR.getIndexedFSs();
            csss.writeView(sofaAddr, fsarray);
          } else { // is Delta Cas
        	  if (sofaNum != 1 && this.marker.isNew(sofaAddr)) {
        	    // for views created after mark (initial view never is - it is always created with the CAS)
        	    // write out the view as new
        	    int[] fsarray = loopIR.getIndexedFSs();
              csss.writeView(sofaAddr, fsarray);
        	  } else if (loopIR.isModified()) {
        	    csss.writeView(sofaAddr, loopIR.getAddedFSs(), loopIR.getDeletedFSs(), loopIR.getReindexedFSs());
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
      int[] fsAddrs = this.sharedData.getAllFsAddressesInIdMap();
      previouslySerializedFSs = new IntVector();
      for (int addr : fsAddrs) {
        // don't enqueue id 0 - this is the "null" fs, which is automatically serialized by xmi
        if (addr == 0 || 
            (isDelta && !marker.isModified(addr))) {
          continue;
        }
        
        // is the first instance, but skip if delta and not modified or above the line or filtered
        // skip enqueuing incoming FS if already enqueued 
//        if (visited_not_yet_written.contains(addr) && isOnlyEmbedded(addr)) {
//          continue;
//        }
        int typeCode = enqueueCommon(addr);
        if (typeCode == -1) {
          continue;
        }
        previouslySerializedFSs.add(addr);
      }
    }

     
    /**
     * add the indexed FSs onto the indexedFSs by view.
     * add the SofaFSs onto the by-ref queue
     */
    private void enqueueIndexed()  {
      FSIndexRepositoryImpl ir = (FSIndexRepositoryImpl) cas.getBaseCAS().getBaseIndexRepository();
      int[] fsarray = ir.getIndexedFSs();
      try {
        for (int fs : fsarray) {
          enqueueFsAndMaybeFeatures(fs);  // put on by-ref queue
        }
      } catch (SAXException e) {
        throw new RuntimeException("Internal error - should never happen", e);
      }

      // FSIndex sofaIndex = cas.getBaseCAS().indexRepository.getIndex(CAS.SOFA_INDEX_NAME);
      // FSIterator iterator = sofaIndex.iterator();
      // // Get indexes for each SofaFS in the CAS
      // while (iterator.isValid())
      int numViews = cas.getBaseSofaCount();
      for (int sofaNum = 1; sofaNum <= numViews; sofaNum++) {
        // SofaFS sofa = (SofaFS) iterator.get();
        // int sofaNum = sofa.getSofaRef();
        // iterator.moveToNext();
        FSIndexRepositoryImpl loopIR = (FSIndexRepositoryImpl) cas.getBaseCAS()
                .getSofaIndexRepository(sofaNum);
        if (loopIR != null) {
          fsarray = loopIR.getIndexedFSs();
          for (int fs : fsarray) {
            enqueueIndexedFs_only_not_features(sofaNum, fs);
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
      int[] fsAddrs = sharedData.getNonsharedMulitValuedFSs();
      modifiedEmbeddedValueFSs = new IntVector();
      for (int addr : fsAddrs) {
        if (marker.isModified(addr)) {
          int encompassingFs = sharedData.getEncompassingFS(addr);
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
      for (IntVector fss : indexedFSs) {
        if (fss != null) {
          enqueueFeaturesOfFSs(fss);
        }
      }
    }
    
    private void enqueueFeaturesOfFSs(final IntVector fss) throws SAXException {      
      final int max = fss.size();
      for (int i = 0; i < max; i++) {
        int addr = fss.get(i);
        int heapVal = cas.getHeapValue(addr);
        enqueueFeatures(addr, heapVal);
      }
    }

    int enqueueCommon(int addr) {
      return enqueueCommon(addr, true);
    }
    
    int enqueueCommonWithoutDeltaAndFilteringCheck(int addr) {
      return enqueueCommon(addr, false);
    }
    
    /**
     * 
     * @param addr
     * @param doDeltaAndFilteringCheck
     * @return true to have enqueue put onto "queue" and enqueue features
     */
    private int enqueueCommon(int addr, boolean doDeltaAndFilteringCheck) {

      final int typeCode = cas.getHeapValue(addr);
      assert(typeCode != 0);
      if (doDeltaAndFilteringCheck) {
        if (isDelta) {
          if (!marker.isNew(addr) && !marker.isModified(addr)) {
            return -1;
          }
        }
      
        if (isFiltering) {
          String typeName = tsi.ll_getTypeForCode(typeCode).getName();
          if (filterTypeSystem.getType(typeName) == null) {
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
     
      if (!visited_not_yet_written.add(addr)) {
        // was already visited; means this FS has multiple references, either from FS feature(s) or indexes or both
        // https://issues.apache.org/jira/browse/UIMA-5532
        if (isDynamicMultiRef || 
            isArrayOrList(typeCode)) {
          boolean wasAdded = multiRefFSs.add(addr);
          if (wasAdded) {
            queue.add(addr);  // if was in indexed set before, isn't in the queue set, but needs to be
          }
        }
        return -1;
      }
      
      boolean alreadySet = typeUsed.get(typeCode);
      if (!alreadySet) {
        typeUsed.set(typeCode);

        String typeName = tsi.ll_getTypeForCode(typeCode).getName();
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
    private void enqueueIndexedFs_only_not_features(int viewNumber, int addr) {
      if (enqueueCommon(addr) != -1) {
        IntVector fss = indexedFSs[viewNumber - 1];
        if (null == fss) {
          indexedFSs[viewNumber - 1] = fss = new IntVector();
        }
        fss.add(addr);
      }
    }

    /**
     * Enqueue an FS, and everything reachable from it.
     * 
     * This call is recursive with enqueueFeatures, 
     * and an arbitrary long chain can get stack overflow error.
     * Probably should fix this someday. See https://issues.apache.org/jira/browse/UIMA-106
     * 
     * @param addr
     *          The FS address.
     */
    private void enqueueFsAndMaybeFeatures(int addr) throws SAXException {    
      int typeCode = enqueueCommon(addr);
      if (typeCode == -1) {
        return;  
      }
      queue.add(addr);
      enqueueFeatures(addr, typeCode);
      // Also, for FSArrays enqueue the elements  -- not here, done by enqueueFeatures, 1 line above
//      if (cas.isFSArrayType(typeCode)) { //TODO: won't get parameterized arrays??
//        enqueueFSArrayElements(addr);
//      }
    }
    
    
    boolean isArrayOrList(int typeCode) {
      return
          isArrayType(typeCode) ||
          isListType(typeCode);
    }
    
    private boolean isArrayType(int typeCode) {
      return
          (typeCode == TypeSystemImpl.intArrayTypeCode) ||
          (typeCode == TypeSystemImpl.floatArrayTypeCode) ||
          (typeCode == TypeSystemImpl.stringArrayTypeCode) ||
          (typeCode == TypeSystemImpl.fsArrayTypeCode) ||
          (typeCode == TypeSystemImpl.booleanArrayTypeCode) ||
          (typeCode == TypeSystemImpl.byteArrayTypeCode) ||
          (typeCode == TypeSystemImpl.shortArrayTypeCode) ||
          (typeCode == TypeSystemImpl.longArrayTypeCode) ||
          (typeCode == TypeSystemImpl.doubleArrayTypeCode);
    }
    
    private boolean isListType(int typeCode) {
      return
          listUtils.isIntListType(typeCode) ||
          listUtils.isFloatListType(typeCode) ||
          listUtils.isStringListType(typeCode) ||
          listUtils.isFsListType(typeCode);
    }
    
    /**
     * For lists, 
     *   see if this is a plain list
     *     - no loops
     *     - no other refs to list elements from outside the list
     *     -- if so, return false;
     *     
     *   add all the elements of the list to visited_not_yet_written,
     *     noting if they've already been added 
     *     -- this indicates either a loop or another ref from outside,
     *     -- in either case, return true - t
     * @param curNode -
     * @param featCode -
     * @return false if no list element is multiply-referenced,
     *         true if there is a loop or another ref from outside the list, for 
     *         one or more list element nodes
     */
    private boolean isListElementsMultiplyReferenced(int listNode, int featCode) {
      int typeCode = cas.getHeapValue(listNode);  // could be end
      int neListType = listUtils.getNeListType(typeCode);
      int tailFeat = listUtils.getTailFeatCode(typeCode);
      boolean foundCycle = false;
      int curNode = listNode;
      while (typeCode == neListType) {  // stop on end or 0
        if (!visited_not_yet_written.add(curNode)) {
          foundCycle = true;
          break;
        }
        curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(tailFeat));
        typeCode = cas.getHeapValue(curNode);
      }
      return foundCycle;
    }
    
    /**
     * ordinary FSs referenced as features are not checked by this routine;
     * this is only called for FSlists of various kinds, and fs arrays of various kinds
     * 
     * Not all featValues should be enqueued;
     *   list or array features which are marked **NOT** multiple-refs-allowed 
     *     are serialized in-line
     *   for JSON, when using dynamicMultiRef (the default), list / array FSs 
     *     are serialized by ref (not in-line) if there are multiple refs to them
     *   
     *   for XMI and JSON, any FS ref marked as multiple-refs-allowed forces
     *     the item onto the ref "queue".
     *     
     *   (not handled here: ordinary FSs are serialized in-line in JSON with isDynamicMultiRef)
     *     
     * @param featCode - the feature, to look up the mulitRefAllowed flag
     * @param featVal - the List or Array element
     * @param alreadyVisited true if visited_not_yet_written contains featVal
     * @param isListNode - 
     * @param isListFeat - 
     * @return false if should skip enqueue because this array or list is being serialized inline
     * @throws SAXException
     */
    private boolean isMultiRef_enqueue(int featCode, int featVal, boolean alreadyVisited, boolean isListNode, boolean isListFeat) throws SAXException {
      if (!isDynamicMultiRef) {
        
        // not JSON dynamic embedding, or dynamic embedding is turned off - compute static embedding just for lists and arrays
        boolean multiRefAllowed = isStaticMultiRef(featCode) || isListNode;
        if (!multiRefAllowed) {
          // two cases: a list or non-list
          // if a list, all the nodes in the list for any being multiply referenced
          if ((isListFeat && isListElementsMultiplyReferenced(featVal, featCode)) ||
              (!isListFeat && alreadyVisited)) {
            // say: multi-ref not allowed, but discovered a multi-ref, will be serialized as separate item
            reportMultiRefWarning(featCode);              
          } else {
            // multi-ref not allowed, and this item is not multiply referenced (so far) 
            // expecting to serialize as embedded (if array or list, or JSON)
            if (!isListFeat) {  // already added visited for list nodes
              visited_not_yet_written.add(featVal);
            }
          }
          return false; // because static, multi-ref not allowed, no need to enqueue
        } else {  // is multiRefAllowed or in list node
          return true; // static, multi-ref allowed or in list node, enqueue
        }
      }
      
      // doing JSON dynamic determination of multi-refs
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
    private void enqueueFeatures(int addr, int typeCode) throws SAXException {
      
      /**
       * Handle FSArrays
       */
      if (typeCode == TypeSystemImpl.fsArrayTypeCode) {
        final int array_size = cas.ll_getArraySize(addr);
        int position = cas.getArrayStartAddress(addr);
        
        for (int j = 0; j < array_size; j++) {
          final int fsRef = cas.getHeapValue(position++);
          if (isFiltering) {
            String typeName = tsi.ll_getTypeForCode(cas.getHeapValue(fsRef)).getName();
            if (filterTypeSystem.getType(typeName) == null) {
              continue;  // don't enqueue this type because it's filtered out
            }
          }
          if (fsRef != CASImpl.NULL) {      // null feature values do not refer to any other FS
            enqueueFsAndMaybeFeatures(fsRef);
          }
        }
        return;
      }
      
      
      boolean insideListNode = listUtils.isListType(typeCode);
      int[] feats = tsi.ll_getAppropriateFeatures(typeCode);
      for (int feat : feats) {
        if (isFiltering) {
          // skip features that aren't in the target type system
          String fullFeatName = tsi.ll_getFeatureForCode(feat).getName();
          if (filterTypeSystem.getFeatureByFullName(fullFeatName) == null) {
            continue;
          }
        }
        final int featAddr = addr + cas.getFeatureOffset(feat);
        final int featVal = cas.getHeapValue(featAddr);
        if (featVal == CASImpl.NULL) {      // null feature values do not refer to any other FS
          continue;
        }

        // enqueue behavior depends on range type of feature
        final int fsClass = classifyType(tsi.range(feat));
        switch (fsClass) {
          case LowLevelCAS.TYPE_CLASS_FS: {
            enqueueFsAndMaybeFeatures(featVal);
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
            // we enqueue arrays if:
            //   when statically using multipleReferencesAllowed flag:
            //     when that says it's multiply referenced; 
            //       otherwise, we skip enqueueing it because it will
            //       be picked up when serializing the feature
            //   when dynamically computing multiple-refs: we enqueue it
            //   unless already enqueued, in order to pick up any multiple refs
            final boolean alreadyVisited = visited_not_yet_written.contains(featVal);
            if (isMultiRef_enqueue(feat, featVal, alreadyVisited, false, false)) {
              if (enqueued_multiRef_arrays_or_lists.add(featVal)) {  // only do this once per item
                enqueueFsAndMaybeFeatures(featVal);  // will add to queue list 1st time multi-ref detected
                                                // or JSON isDynamicEmbedding is on (whether or not multi-ref)
              } else {
                // for isDynamicMultiRef, this is the first time we detect multiple refs
                // do this here, because the enqueued_multiRef_arrays_or_lists.add above makes
                //   the 2nd and subsequent multi-ref things bypass the enqueue call.
                //   - only needed for isDynamicMultiRef, because only that returns true for isMultiRef_enqueue
                //     for the "first" instance, when it isn't yet known.
                if (isDynamicMultiRef) {
                  multiRefFSs.add(featVal);  
                }
              }

            // otherwise, it is singly referenced (so far) and will be embedded
            //   (or has already been enqueued, in dynamic embedding mode), so don't enqueue
            } else if (fsClass == LowLevelCAS.TYPE_CLASS_FSARRAY && !alreadyVisited) {
              // enqueue any FSs reachable from an FSArray
              enqueueFSArrayElements(featVal);
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
            final boolean alreadyVisited = visited_not_yet_written.contains(featVal);
            if (isMultiRef_enqueue(feat, featVal, alreadyVisited, insideListNode, true)) {
              if (enqueued_multiRef_arrays_or_lists.add(featVal)) {  // only do this once per item
                enqueueFsAndMaybeFeatures(featVal);
              } else {
                // for isDynamicMultiRef, this is the first time we detect multiple refs
                // do this here, because the enqueued_multiRef_arrays_or_lists.add above makes
                //   the 2nd and subsequent multi-ref things bypass the enqueue call.
                //   - only needed for isDynamicMultiRef, because only that returns true for isMultiRef_enqueue
                //     for the "first" instance, when it isn't yet known.
                if (isDynamicMultiRef) {
                  multiRefFSs.add(featVal);  
                }
              }
            } else if (fsClass == TYPE_CLASS_FSLIST && !alreadyVisited) {
              // also, we need to enqueue any FSs reachable from an FSList
              enqueueFSListElements(featVal);
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
    private void enqueueFSArrayElements(int addr) throws SAXException {
      final int size = cas.ll_getArraySize(addr);
      int pos = cas.getArrayStartAddress(addr);
      int val;
      for (int i = 0; i < size; i++) {
        val = cas.getHeapValue(pos);
        if (val != CASImpl.NULL) {
          enqueueFsAndMaybeFeatures(val);
        }
        ++pos;
      }
    }

    /**
     * Enqueues all FS reachable from an FSList. This does NOT include the list nodes themselves.
     * 
     * @param addr
     *          Address of an FSList
     */
    private void enqueueFSListElements(int addr) throws SAXException {
      int[] addrArray = listUtils.fsListToAddressArray(addr);
      for (int j = 0; j < addrArray.length; j++) {
        if (addrArray[j] != CASImpl.NULL) {
          enqueueFsAndMaybeFeatures(addrArray[j]);
        }
      }
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
      for (IntVector fss : indexedFSs) {
        if (fss != null) {
          encodeFSs(fss);
        }
      }
    }
    
    private void encodeFSs(final IntVector fss) throws Exception {
      final int max = fss.size();
      for (int i = 0; i < max; i++) {
        encodeFS(fss.get(i));
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
      int[] queueArray = queue.toArray();
      for (int addr : queueArray) {
        // for some serializers, things could be enqueued multiple times in the ref queue
        // so check if already written, and if so, skip
        //    Case where this happens: JSON serialization with dynamically determined single ref embedding
        //    - have to enqueue to check if multiple refs, even if embedding eventually
        if (visited_not_yet_written.contains(addr)) {
          
          // skip if JSON dynamically computing whether or not to embed things and there's only one item - it will be embedded instead
          if (isDynamicMultiRef && !multiRefFSs.contains(addr)) {
            continue;  // skip writing embeddable item (for JSON dynamic embedding) from Q; will be written from reference
          }
          encodeFS(addr);
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
  
    private int compareInts(int i1, int i2) {
      return (i1 == i2) ? 0 :
             (i1 >  i2) ? 1 : -1;
    }
    
    
    private int compareFeat(int o1, int o2, int featCode) {
      final int f1 = cas.ll_getIntValue(o1, featCode);
      final int f2 = cas.ll_getIntValue(o2, featCode);
      return compareInts(f1, f2);
    }
    
    /** 
     * sort a view, by type and then by begin/end asc/des for subtypes of Annotation,
     *  then by id
     */
    public final Comparator<Integer> sortFssByType = 
        new Comparator<Integer>() {
          public int compare(Integer o1, Integer o2) {
            final int typeCode1 = cas.getHeapValue(o1);
            final int typeCode2 = cas.getHeapValue(o2);
            int c = compareInts(typeCode1, typeCode2);
            if (c != 0) {
              return c;
            }
//            final boolean hasSofa = tsi.subsumes(tsi.annotBaseTypeCode, typeCode1);
//            if (hasSofa) {
//              c = compareFeat(o1, o2, tsi.annotSofaFeatCode);
//              if (c != 0) {
//                return c;
//              }
            final boolean isAnnot = tsi.subsumes(TypeSystemImpl.annotTypeCode, typeCode1);
            if (isAnnot) {
              c = compareFeat(o1, o2, TypeSystemImpl.startFeatCode);
              return (c != 0) ? c : compareFeat(o2, o1, TypeSystemImpl.endFeatCode);  // reverse order
            }
            // not sofa nor annotation
            return compareInts(o1, o2);  // return in @id order
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
     * @param addr
     *          The address to be encoded.
     * @throws SAXException passthru
     */
    public void encodeFS(int addr) throws Exception {
      final int typeCode = cas.getHeapValue(addr);

      final int typeClass = classifyType(typeCode);
      // for JSON, the items reachable via indexes are written first,
      //   and isIndexId = false
      //   The items reachable via refs are written next, and 
      //   isIndexId = true;
      boolean isIndexId = csss.writeFsStart(addr, typeCode);
      
      // write the id if needed for reference
      //   - if it is not ref'd via index, and JSON is computing dynamic refs, and it is multiply ref'd
      //   - skip if not JSON dynamic ref, or in index, or not multiply ref'd
      if (!isIndexId && isDynamicMultiRef && multiRefFSs.contains(addr)) {
        csss.writeFsRef(addr);        
      } else {
        visited_not_yet_written.remove(addr);  // mark as written
        switch (typeClass) {
          case LowLevelCAS.TYPE_CLASS_FS: 
            csss.writeFs(addr, typeCode);
            break;
          
            
          case TYPE_CLASS_INTLIST:
          case TYPE_CLASS_FLOATLIST:
          case TYPE_CLASS_STRINGLIST:
          case TYPE_CLASS_FSLIST: 
            csss.writeListsAsIndividualFSs(addr, typeCode);
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
            csss.writeArrays(addr, typeCode, typeClass);
            break;
          
          default: 
            throw new RuntimeException("Error classifying FS type.");
        }
        
        csss.writeEndOfIndividualFs();
      }
    }
    
    int filterType(int addr) {
      if (isFiltering) {
        String typeName = tsi.ll_getTypeForCode(cas.getHeapValue(addr)).getName();
        if (filterTypeSystem.getType(typeName) == null) {
          return 0;
        }
      }
      return addr;
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
     * @param type
     *          the type to classify
     * @return one of the TYPE_CLASS codes defined on {@link LowLevelCAS} or on this interface.
     */
    public final int classifyType(int type) {
      // For most most types
      if (listUtils.isIntListType(type)) {
        return TYPE_CLASS_INTLIST;
      }
      if (listUtils.isFloatListType(type)) {
        return TYPE_CLASS_FLOATLIST;
      }
      if (listUtils.isStringListType(type)) {
        return TYPE_CLASS_STRINGLIST;
      }
      if (listUtils.isFsListType(type)) {
        return TYPE_CLASS_FSLIST;
      }
      return cas.ll_getTypeClass(type);
    }

    int getElementCountForSharedData() {
      return (sharedData == null) ? 0 : sharedData.getOutOfTypeSystemElements().size();
    }    
    
    /**
     * Get the XMI ID to use for an FS.
     * 
     * @param addr
     *          address of FS
     * @return XMI ID. If addr == CASImpl.NULL, returns null
     */
    public String getXmiId(int addr) {
      int v = getXmiIdAsInt(addr);
      return (v == 0) ? null : Integer.toString(v);
    }
    
    public int getXmiIdAsInt(int addr) {
      if (addr == CASImpl.NULL) {
        return 0;
      }
      if (isFiltering) { // return as null any references to types not in target TS
        String typeName = tsi.ll_getTypeForCode(cas.getHeapValue(addr)).getName();
        if (filterTypeSystem.getType(typeName) == null) {
          return 0;
        }
      }
      
      if (sharedData == null) {
        // in the absence of outside information, just use the FS address
        return addr;
      } else {
        return sharedData.getXmiIdAsInt(addr);
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
    
    public boolean isStaticMultiRef(int featCode) {
      return tsi.ll_getFeatureForCode(featCode).isMultipleReferencesAllowed();
    }


  }  
}
