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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.XmiSerializationSharedData.OotsElementData;
import org.apache.uima.cas.impl.XmiSerializationSharedData.XmiArrayElement;
import org.apache.uima.internal.util.IntStack;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.XmlAttribute;
import org.apache.uima.internal.util.XmlElementName;
import org.apache.uima.internal.util.XmlElementNameAndContents;
import org.apache.uima.internal.util.rb_trees.IntRedBlackTree;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * XMI CAS serializer. Used to write out a CAS in an XML Metadata Interchange (XMI) format. Create a
 * serializer from a type system, then encode individual CASes by writing to a SAX content handler.
 * This class is thread safe.
 */
public class XmiCasSerializer {
  // Special "type class" codes for list types. The LowLevelCAS.ll_getTypeClass() method
  // returns type classes for primitives and arrays, but not lists (which are just ordinary FS types
  // as far as the CAS is concerned). The XMI serialization treats lists specially, however, and
  // so needs its own type codes for these.
  public static final int TYPE_CLASS_INTLIST = 101;

  public static final int TYPE_CLASS_FLOATLIST = 102;

  public static final int TYPE_CLASS_STRINGLIST = 103;

  public static final int TYPE_CLASS_FSLIST = 104;

  // number of children of current element
  private int numChildren;

  /**
   * Gets the number of children of the current element. This is guranteed to be set correctly at
   * the time when startElement is called. Needed for streaming Vinci serialization.
   * <p>
   * NOTE: this method will not work if there are simultaneously executing calls to
   * XmiCasSerializer.serialize. Use it only with a dedicated XmiCasSerializer instance that is not
   * shared betwen threads.
   * 
   * @return the number of children of the current element
   */
  public int getNumChildren() {
    return numChildren;
  }

  /**
   * Use an inner class to hold the data for serializing a CAS. Each call to serialize() creates its
   * own instance.
   * 
   * 
   */
  private class XmiCasDocSerializer {

    // Where the output goes.
    private ContentHandler ch;

    // optional error handler, mainly so we can send warnings
    private ErrorHandler eh = null;

    // The CAS we're serializing.
    private CASImpl cas;

    // Any FS reference we've touched goes in here.
    private IntRedBlackTree visited;

    // All FSs that are in an index somewhere.
    private IntVector indexedFSs;

    // The current queue for FSs to write out.
    private IntStack queue;

    // SofaFS type
    // private int sofaTypeCode;

    // Annotation type
    // private int annotationTypeCode;

    private final AttributesImpl emptyAttrs = new AttributesImpl();

    private AttributesImpl workAttrs = new AttributesImpl();

    private static final String cdataType = "CDATA";

    // For debug statistics.
    private int fsCount = 0;

    // utilities for dealing with CAS list types
    private ListUtils listUtils;

    // holds the addresses of Array and List FSs that we have encountered
    private IntRedBlackTree arrayAndListFSs;

    private XmiSerializationSharedData sharedData;

    private XmlElementName[] xmiTypeNames; // array, indexed by type code, giving XMI names for

    // each type

    private Map nsUriToPrefixMap = new HashMap();

    private Set nsPrefixesUsed = new HashSet();

    /**
     * Whether the serializer neeeds to check for filtered-out types/features. Set to true if type
     * system of CAS does not match type system that was passed to constructor of serializer.
     */
    boolean isFiltering;

    private XmiCasDocSerializer(ContentHandler ch, ErrorHandler eh, CASImpl cas,
            XmiSerializationSharedData sharedData) {
      super();
      this.ch = ch;
      this.eh = eh;
      this.cas = cas;
      this.visited = new IntRedBlackTree();
      this.queue = new IntStack();
      this.indexedFSs = new IntVector();
      // this.sofaTypeCode = cas.getTypeSystemImpl().getTypeCode(CAS.TYPE_NAME_SOFA);
      // this.annotationTypeCode = cas.getTypeSystemImpl().getTypeCode(CAS.TYPE_NAME_ANNOTATION);
      this.listUtils = new ListUtils(cas, logger, eh);
      this.arrayAndListFSs = new IntRedBlackTree();
      this.sharedData = sharedData;
      this.isFiltering = filterTypeSystem != null && filterTypeSystem != cas.getTypeSystemImpl();
    }

    // TODO: internationalize
    private void reportWarning(String message) throws SAXException {
      logger.log(Level.WARNING, message);
      if (this.eh != null) {
        this.eh.warning(new SAXParseException(message, null));
      }
    }

    /**
     * Check if we've seen this address before.
     * 
     * @param addr
     *          The address.
     * @return <code>true</code> iff we've seen the address before.
     */
    private boolean isVisited(int addr) {
      return visited.containsKey(addr);
    }

    /**
     * Starts serialization
     */
    private void serialize() throws SAXException {
      // populate nsUriToPrefixMap and xmiTypeNames structures based on CAS 
      // type system, and out of typesytem data if any
      initTypeAndNamespaceMappings();

      int iElementCount = 1; // start at 1 to account for special NULL object

      enqueueIncoming(); //make sure we enqueue every FS that was deserialized into this CAS
      enqueueIndexed();
      enqueueFeaturesOfIndexed();
      iElementCount += indexedFSs.size();
      iElementCount += queue.size();

      FSIndex sofaIndex = cas.getBaseCAS().indexRepository.getIndex(CAS.SOFA_INDEX_NAME);
      iElementCount += (sofaIndex.size()); // one View element per sofa
      if (this.sharedData != null) {
        iElementCount += this.sharedData.getOutOfTypeSystemElements().size();
      }
      
      workAttrs.clear();
      computeNamespaceDeclarationAttrs(workAttrs);
      workAttrs.addAttribute(XMI_NS_URI, XMI_VERSION_LOCAL_NAME, XMI_VERSION_QNAME, "CDATA",
              XMI_VERSION_VALUE);

      startElement(XMI_TAG, workAttrs, iElementCount);
      writeNullObject(); // encodes 1 element
      encodeIndexed(); // encodes indexedFSs.size() element
      encodeQueued(); // encodes queue.size() elements
      serializeOutOfTypeSystemElements(); //encodes sharedData.getOutOfTypeSystemElements().size() elements
      writeViews(); // encodes cas.sofaCount + 1 elements
      endElement(XMI_TAG);
    }

    private void writeViews() throws SAXException {
      // Get indexes for each SofaFS in the CAS
      int numViews = cas.getBaseSofaCount();
      String sofaXmiId = null;
      for (int sofaNum = 1; sofaNum <= numViews; sofaNum++) {
        FSIndexRepositoryImpl loopIR = (FSIndexRepositoryImpl) cas.getBaseCAS()
                .getSofaIndexRepository(sofaNum);
        if (sofaNum != 1 || cas.isInitialSofaCreated()) {
          FeatureStructureImpl sofa = (FeatureStructureImpl) cas.getView(sofaNum).getSofa();
          sofaXmiId = getXmiId((sofa).getAddress());
        }
        if (loopIR != null) {
          int[] fsarray = loopIR.getIndexedFSs();
          writeView(sofaXmiId, fsarray);
        }
      }
    }

    private void writeView(String sofaXmiId, int[] members) throws SAXException {
      workAttrs.clear();
      if (sofaXmiId != null && sofaXmiId.length() > 0) {
        addAttribute(workAttrs, "sofa", sofaXmiId);
      }
      StringBuffer membersString = new StringBuffer();
      for (int i = 0; i < members.length; i++) {
        String xmiId = getXmiId(members[i]);
        if (xmiId != null) // to catch filtered FS
        {
          membersString.append(xmiId).append(' ');
        }
      }
      //check for out-of-typesystem members
      if (this.sharedData != null) {
        List ootsMembers = this.sharedData.getOutOfTypeSystemViewMembers(sofaXmiId);
        if (ootsMembers != null) {
          Iterator iter = ootsMembers.iterator();
          while (iter.hasNext()) {
            membersString.append((String)iter.next()).append(' ');
          }
        }
      }
      if (membersString.length() > 0) {
        // remove trailing space before adding to attributes
        addAttribute(workAttrs, "members", membersString.substring(0, membersString.length() - 1));
      }
      XmlElementName elemName = uimaTypeName2XmiElementName("uima.cas.View");
      startElement(elemName, workAttrs, 0);
      endElement(elemName);
    }

    /**
     * Writes a special instance of dummy type uima.cas.NULL, having xmi:id=0. This is needed to
     * represent nulls in multi-valued references, which aren't natively supported in Ecore.
     * 
     */
    private void writeNullObject() throws SAXException {
      workAttrs.clear();
      addAttribute(workAttrs, ID_ATTR_NAME, "0");
      XmlElementName elemName = uimaTypeName2XmiElementName("uima.cas.NULL");
      startElement(elemName, workAttrs, 0);
      endElement(elemName);
    }

    /**
     * @param workAttrs2
     */
    private void computeNamespaceDeclarationAttrs(AttributesImpl workAttrs2) {
      Iterator it = nsUriToPrefixMap.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry entry = (Map.Entry) it.next();
        String nsUri = (String) entry.getKey();
        String prefix = (String) entry.getValue();
        // write attribute
        workAttrs.addAttribute(null, prefix, "xmlns:" + prefix, "CDATA", nsUri);
      }
      // also add schemaLocation if specified
      if (nsUriToSchemaLocationMap != null) {
        // write xmlns:xsi attribute
        workAttrs.addAttribute(null, "xsi", "xmlns:xsi", "CDATA", XSI_NS_URI);

        // write xsi:schemaLocation attributaiton
        StringBuffer buf = new StringBuffer();
        it = nsUriToSchemaLocationMap.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry entry = (Map.Entry) it.next();
          buf.append(entry.getKey()).append(' ').append(entry.getValue()).append(' ');
        }
        workAttrs.addAttribute(null, "xsi", "xsi:schemaLocation", "CDATA", buf.toString());
      }
    }

    /**
     * Enqueues all FS that are stored in the XmiSerializationSharedData's id map.
     * This map is populated during the previous deserialization.  This method
     * is used to make sure that all incoming FS are echoed in the next
     * serialization.
     */
    private void enqueueIncoming() {
      if (this.sharedData == null)
        return;
      
      int[] fsAddrs = this.sharedData.getAllFsAddressesInIdMap();
      for (int i = 0; i < fsAddrs.length; i++) {
        enqueueIndexedFs(fsAddrs[i]);
      }
    }
    
    /**
     * Push the indexed FSs onto the queue.
     */
    private void enqueueIndexed() {
      FSIndexRepositoryImpl ir = (FSIndexRepositoryImpl) cas.getBaseCAS().getBaseIndexRepository();
      int[] fsarray = ir.getIndexedFSs();
      for (int k = 0; k < fsarray.length; k++) {
        enqueueIndexedFs(fsarray[k]);
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
          for (int k = 0; k < fsarray.length; k++) {
            enqueueIndexedFs(fsarray[k]);
          }
        }
      }
    }

    /**
     * Enqueue everything reachable from features of indexed FSs.
     */
    private void enqueueFeaturesOfIndexed() throws SAXException {
      final int max = indexedFSs.size();
      for (int i = 0; i < max; i++) {
        int addr = indexedFSs.get(i);
        int heapVal = cas.getHeapValue(addr);
        enqueueFeatures(addr, heapVal);
      }
    }

    /**
     * Enqueues an indexed FS. Does NOT enqueue features at this point.
     */
    private void enqueueIndexedFs(int addr) {
      if (isVisited(addr)) {
        return;
      }
      if (isFiltering) {
        String typeName = cas.getTypeSystemImpl().getTypeName(cas.getHeapValue(addr));
        if (filterTypeSystem.getType(typeName) == null) {
          return; // this type is not in the target type system
        }
      }
      visited.put(addr, addr);
      indexedFSs.add(addr);
    }

    /**
     * Enqueue an FS, and everything reachable from it.
     * 
     * @param addr
     *          The FS address.
     */
    private void enqueue(int addr) throws SAXException {
      if (isVisited(addr)) {
        return;
      }
      int typeCode = cas.getHeapValue(addr);
      if (isFiltering) {
        String typeName = cas.getTypeSystemImpl().getTypeName(typeCode);
        if (filterTypeSystem.getType(typeName) == null) {
          return; // this type is not in the target type system
        }
      }
      visited.put(addr, addr);
      queue.push(addr);
      enqueueFeatures(addr, typeCode);

      // Also, for FSArrays enqueue the elements
      if (cas.isFSArrayType(typeCode)) {
        enqueueFSListElements(addr);
      }
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
      boolean insideListNode = listUtils.isListType(typeCode);
      int[] feats = cas.getTypeSystemImpl().ll_getAppropriateFeatures(typeCode);
      int featAddr, featVal, fsClass;
      for (int i = 0; i < feats.length; i++) {
        if (isFiltering) {
          // skip features that aren't in the target type system
          String fullFeatName = cas.getTypeSystemImpl().ll_getFeatureForCode(feats[i]).getName();
          if (filterTypeSystem.getFeatureByFullName(fullFeatName) == null) {
            continue;
          }
        }
        featAddr = addr + cas.getFeatureOffset(feats[i]);
        featVal = cas.getHeapValue(featAddr);
        if (featVal == CASImpl.NULL) {
          continue;
        }

        // enqueue behavior depends on range type of feature
        fsClass = classifyType(cas.getTypeSystemImpl().range(feats[i]));
        switch (fsClass) {
          case LowLevelCAS.TYPE_CLASS_FS: {
            enqueue(featVal);
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
            // we only enqueue arrays as first-class objects if the feature has
            // multipleReferencesAllowed = true
            if (cas.getTypeSystemImpl().ll_getFeatureForCode(feats[i]).isMultipleReferencesAllowed()) {
              enqueue(featVal);
            } else if (fsClass == LowLevelCAS.TYPE_CLASS_FSARRAY) {
              // but we do need to enqueue any FSs reachable from an FSArray
              enqueueFSArrayElements(featVal);
            }
            break;
          }
          case TYPE_CLASS_INTLIST:
          case TYPE_CLASS_FLOATLIST:
          case TYPE_CLASS_STRINGLIST:
          case TYPE_CLASS_FSLIST: {
            // we only enqueue lists as first-class objects if the feature has
            // multipleReferencesAllowed = true
            // OR if we're already inside a list node (this handles the tail feature correctly)
            if (cas.getTypeSystemImpl().ll_getFeatureForCode(feats[i]).isMultipleReferencesAllowed() || insideListNode) {
              enqueue(featVal);
            } else if (fsClass == TYPE_CLASS_FSLIST) {
              // also, we need to enqueue any FSs reachable from an FSList
              enqueueFSListElements(featVal);
            }
            break;
          }
        }
      }
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
          enqueue(val);
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
          enqueue(addrArray[j]);
        }
      }
    }

    /**
     * Encode the indexed FS in the queue.
     * 
     * @throws IOException
     * @throws SAXException
     */
    private void encodeIndexed() throws SAXException {
      final int max = indexedFSs.size();
      for (int i = 0; i < max; i++) {
        encodeFS(indexedFSs.get(i));
      }
    }

    /**
     * Encode all other enqueued (non-indexed) FSs.
     * 
     * @throws XMLException
     * @throws IOException
     * @throws SAXException
     */
    private void encodeQueued() throws SAXException {
      int addr;
      while (!queue.empty()) {
        addr = queue.pop();
        encodeFS(addr);
      }
    }

    /**
     * Encode an individual FS.
     * 
     * @param addr
     *          The address to be encoded.
     * @throws SAXException
     */
    private void encodeFS(int addr) throws SAXException {
      ++fsCount;
      workAttrs.clear();

      // Add ID attribute. We do this for every FS, since otherwise we would
      // have to do a complete traversal of the heap to find out which FSs is
      // actually referenced.
      addAttribute(workAttrs, ID_ATTR_NAME, getXmiId(addr));

      // generate the XMI name for the type (uses a precomputed array so we don't
      // recompute the same name multiple times).
      int typeCode = cas.getHeapValue(addr);
      XmlElementName xmlElementName = xmiTypeNames[typeCode];

      // Call special code according to the type of the FS (special treatment
      // for arrays and lists).
      final int typeClass = classifyType(typeCode);
      switch (typeClass) {
        case LowLevelCAS.TYPE_CLASS_FS:
        case TYPE_CLASS_INTLIST:
        case TYPE_CLASS_FLOATLIST:
        case TYPE_CLASS_STRINGLIST:
        case TYPE_CLASS_FSLIST: {

          // encode features. this populates the attributes (workAttrs). It also
          // populates the child elements list with features that are to be encoded
          // as child elements (currently required for string arrays).
          List childElements = encodeFeatures(addr, workAttrs,
                  (typeClass != LowLevelCAS.TYPE_CLASS_FS));
          startElement(xmlElementName, workAttrs, childElements.size());
          sendElementEvents(childElements);
          endElement(xmlElementName);
          break;
        }
        case LowLevelCAS.TYPE_CLASS_FSARRAY:
        case LowLevelCAS.TYPE_CLASS_INTARRAY:
        case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
        case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY:
        case LowLevelCAS.TYPE_CLASS_BYTEARRAY:
        case LowLevelCAS.TYPE_CLASS_SHORTARRAY:
        case LowLevelCAS.TYPE_CLASS_LONGARRAY:
        case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY: {
          workAttrs.addAttribute("", "", "elements", "CDATA", arrayToString(addr, typeClass));
          startElement(xmlElementName, workAttrs, 0);
          endElement(xmlElementName);
          break;
        }
        case LowLevelCAS.TYPE_CLASS_STRINGARRAY: {
          // string arrays are encoded as elements, in case they contain whitespace
          List childElements = new ArrayList();
          stringArrayToElementList("elements", addr, childElements);

          startElement(xmlElementName, workAttrs, childElements.size());
          sendElementEvents(childElements);
          endElement(xmlElementName);
          break;
        }
        default: {
          throw new SAXException("Error classifying FS type.");
        }
      }
    }

    /**
     * Get the XMI ID to use for an FS.
     * 
     * @param addr
     *          address of FS
     * @return XMI ID. If addr == CASImpl.NULL, returns null
     */
    private String getXmiId(int addr) {
      if (addr == CASImpl.NULL) {
        return null;
      }
      if (isFiltering) // return as null any references to types not in target TS
      {
        String typeName = cas.getTypeSystemImpl().getTypeName(cas.getHeapValue(addr));
        if (filterTypeSystem.getType(typeName) == null) {
          return null;
        }

      }
      if (this.sharedData == null) {
        // in the absence of outside information, just use the FS address
        return Integer.toString(addr);
      } else {
        return this.sharedData.getXmiId(addr);
      }
    }

    /**
     * Generate startElement, characters, and endElement SAX events.
     * 
     * @param elements
     *          a list of XMLElementNameAndContents objects representing the elements to generate
     * @throws SAXException
     */
    private void sendElementEvents(List elements) throws SAXException {
      Iterator childIter = elements.iterator();
      while (childIter.hasNext()) {
        XmlElementNameAndContents elem = (XmlElementNameAndContents) childIter.next();
        if (elem.contents != null) {
          startElement(elem.name, emptyAttrs, 1);
          addText(elem.contents);
        } else {
          startElement(elem.name, emptyAttrs, 0);
        }
        endElement(elem.name);
      }
    }

    /**
     * Encode features of a regular (non-array) FS.
     * 
     * @param addr
     *          Address of the FS
     * @param attrs
     *          SAX Attributes object, to which we will add attributes
     * @param insideListNode
     *          true iff this FS is a List type.
     * 
     * @return a List of XmlElementNameAndContents objects, each of which represents an element that
     *         should be added as a child of the FS
     */
    private List encodeFeatures(int addr, AttributesImpl attrs, boolean insideListNode)
            throws SAXException {
      List childElements = new ArrayList();
      int heapValue = cas.getHeapValue(addr);
      int[] feats = cas.getTypeSystemImpl().ll_getAppropriateFeatures(heapValue);
      int featAddr, featVal, fsClass;
      String featName, attrValue;
      // boolean isSofa = false;
      // if (sofaTypeCode == heapValue)
      // {
      // // set isSofa flag to apply SofaID mapping and to store sofaNum->xmi:id mapping
      // isSofa = true;
      // }
      for (int i = 0; i < feats.length; i++) {
        if (isFiltering) {
          // skip features that aren't in the target type system
          String fullFeatName = cas.getTypeSystemImpl().ll_getFeatureForCode(feats[i]).getName();
          if (filterTypeSystem.getFeatureByFullName(fullFeatName) == null) {
            continue;
          }
        }

        featAddr = addr + cas.getFeatureOffset(feats[i]);
        featVal = cas.getHeapValue(featAddr);
        featName = cas.getTypeSystemImpl().ll_getFeatureForCode(feats[i]).getShortName();
        fsClass = classifyType(cas.getTypeSystemImpl().range(feats[i]));
        switch (fsClass) {
          case LowLevelCAS.TYPE_CLASS_INT:
          case LowLevelCAS.TYPE_CLASS_FLOAT:
          case LowLevelCAS.TYPE_CLASS_BOOLEAN:
          case LowLevelCAS.TYPE_CLASS_BYTE:
          case LowLevelCAS.TYPE_CLASS_SHORT:
          case LowLevelCAS.TYPE_CLASS_LONG:
          case LowLevelCAS.TYPE_CLASS_DOUBLE: {
            attrValue = cas.getFeatureValueAsString(addr, feats[i]);
            break;
          }
          case LowLevelCAS.TYPE_CLASS_STRING: {
            if (featVal == CASImpl.NULL) {
              attrValue = null;
              break;
            }
            attrValue = cas.getStringForCode(featVal);
            break;
          }
            // Arrays
          case LowLevelCAS.TYPE_CLASS_INTARRAY:
          case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
          case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY:
          case LowLevelCAS.TYPE_CLASS_BYTEARRAY:
          case LowLevelCAS.TYPE_CLASS_SHORTARRAY:
          case LowLevelCAS.TYPE_CLASS_LONGARRAY:
          case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY:
          case LowLevelCAS.TYPE_CLASS_FSARRAY: {
            // If the feature has multipleReferencesAllowed = true, serialize as any other FS.
            // If false, serialize as a multi-valued property.
            if (cas.getTypeSystemImpl().ll_getFeatureForCode(feats[i]).isMultipleReferencesAllowed()) {
              attrValue = getXmiId(featVal);
            } else {
              attrValue = arrayToString(featVal, fsClass);
            }
            break;
          }
            // special case for StringArrays, which stored values as child elements rather
            // than attributes.
          case LowLevelCAS.TYPE_CLASS_STRINGARRAY: {
            // If the feature has multipleReferencesAllowed = true, serialize as any other FS.
            // If false, serialize as a multi-valued property.
            if (cas.getTypeSystemImpl().ll_getFeatureForCode(feats[i]).isMultipleReferencesAllowed()) {
              attrValue = getXmiId(featVal);
            } else {
              stringArrayToElementList(featName, featVal, childElements);
              attrValue = null;
            }
            break;
          }
            // Lists
          case TYPE_CLASS_INTLIST:
          case TYPE_CLASS_FLOATLIST:
          case TYPE_CLASS_FSLIST: {
            // If the feature has multipleReferencesAllowed = true OR if we're already
            // inside another list node (i.e. this is the "tail" feature), serialize as a normal FS.
            // Otherwise, serialize as a multi-valued property.
            if (cas.getTypeSystemImpl().ll_getFeatureForCode(feats[i]).isMultipleReferencesAllowed() || insideListNode) {
              attrValue = getXmiId(featVal);
            } else {
              attrValue = listToString(featVal, fsClass);
            }
            break;
          }
            // special case for StringLists, which stored values as child elements rather
            // than attributes.
          case TYPE_CLASS_STRINGLIST: {
            if (cas.getTypeSystemImpl().ll_getFeatureForCode(feats[i]).isMultipleReferencesAllowed() || insideListNode) {
              attrValue = getXmiId(featVal);
            } else {
              // it is not safe to use a space-separated attribute, which would
              // break for strings containing spaces. So use child elements instead.
              String[] array = listUtils.stringListToStringArray(featVal);
              if (array.length > 0 && !arrayAndListFSs.put(featVal, featVal)) {
                reportWarning("Warning: multiple references to a ListFS.  Reference identity will not be preserved.");
              }
              for (int j = 0; j < array.length; j++) {
                childElements.add(new XmlElementNameAndContents(new XmlElementName(null, featName,
                        featName), array[j]));
              }
              attrValue = null;
            }
            break;
          }
          default: // Anything that's not a primitive type, array, or list.
          {
            attrValue = getXmiId(featVal);
            break;
          }
        }
        if (attrValue != null && featName != null) {
          addAttribute(attrs, featName, attrValue);
        }
      }
      
      //add out-of-typesystem features, if any
      if (this.sharedData != null) {
        OotsElementData oed = this.sharedData.getOutOfTypeSystemFeatures(addr);
        if (oed != null) {
          //attributes
          Iterator attrIter = oed.attributes.iterator();
          while (attrIter.hasNext()) {
            XmlAttribute attr = (XmlAttribute)attrIter.next();
            addAttribute(workAttrs, attr.name, attr.value);
          }
          //child elements
          childElements.addAll(oed.childElements);
        }
      }
      return childElements;
    }

    private void addText(String text) throws SAXException {
      ch.characters(text.toCharArray(), 0, text.length());
    }

    private void addAttribute(AttributesImpl attrs, String attrName, String attrValue) {
      attrs.addAttribute(null, null, attrName, cdataType, attrValue);
    }

    private void startElement(XmlElementName name, Attributes attrs, int aNumChildren)
            throws SAXException {
      XmiCasSerializer.this.numChildren = aNumChildren;
      // don't include NS URI here. That causes XMI serializer to
      // include the xmlns attribute in every element. Instead we
      // explicitly added these attributes to the root element.
      ch.startElement(""/* name.nsUri */, name.localName, name.qName, attrs);
    }

    private void endElement(XmlElementName name) throws SAXException {
      ch.endElement(name.nsUri, name.localName, name.qName);
    }

    /**
     * @param featName
     * @param addr
     * @param resultList
     * @throws SAXException
     */
    private void stringArrayToElementList(String featName, int addr, List resultList)
            throws SAXException {
      if (addr == CASImpl.NULL) {
        return;
      }

      // it is not safe to use a space-separated attribute, which would
      // break for strings containing spaces. So use child elements instead.
      final int size = cas.ll_getArraySize(addr);
      if (size > 0 && !arrayAndListFSs.put(addr, addr)) {
        reportWarning("Warning: multiple references to a String array.  Reference identity will not be preserved.");
      }
      int pos = cas.getArrayStartAddress(addr);
      for (int j = 0; j < size; j++) {
        String s = cas.getStringForCode(cas.getHeapValue(pos));
        resultList.add(new XmlElementNameAndContents(new XmlElementName(null, featName, featName),
                s));
        ++pos;
      }
    }

    private String arrayToString(int addr, int arrayType) throws SAXException {
      if (addr == CASImpl.NULL) {
        return null;
      }

      StringBuffer buf = new StringBuffer();
      final int size = cas.ll_getArraySize(addr);
      if (size > 0 && !arrayAndListFSs.put(addr, addr)) {
        reportWarning("Warning: multiple references to an array.  Reference identity will not be preserved in XMI.");
      }
      String elemStr = null;
      if (arrayType == LowLevelCAS.TYPE_CLASS_FSARRAY) {
        int pos = cas.getArrayStartAddress(addr);
        List ootsArrayElementsList = this.sharedData == null ? null : 
                this.sharedData.getOutOfTypeSystemArrayElements(addr);
        int ootsIndex = 0;
        for (int j = 0; j < size; j++) {
          int heapValue = cas.getHeapValue(pos++);
          elemStr = null;
          String xmiId = getXmiId(heapValue);
          if (xmiId != null) {
            elemStr = xmiId;
          } else {
            // special NULL object with xmi:id=0 is used to represent
            // a null in an FSArray
            elemStr = "0";
            // However, this null array element might have been a reference to an 
            //out-of-typesystem FS, so check the ootsArrayElementsList
            if (ootsArrayElementsList != null) {
              while (ootsIndex < ootsArrayElementsList.size()) {
                XmiArrayElement arel =(XmiArrayElement)ootsArrayElementsList.get(ootsIndex++);
                if (arel.index == j) {
                  elemStr = arel.xmiId;
                  break;
                }                
              }
            }
          }
          if (buf.length() > 0) {
            buf.append(' ');
          }
          buf.append(elemStr);
        }
        return buf.toString();
      } else if (arrayType == LowLevelCAS.TYPE_CLASS_BYTEARRAY) {
        // special case for byte arrays: serialize as hex digits
        ByteArrayFS byteArrayFS = new ByteArrayFSImpl(addr, cas);
        int len = byteArrayFS.size();
        for (int i = 0; i < len; i++) {
          byte b = byteArrayFS.get(i);
          // this test is necessary to generate a leading zero where necessary
          if ((b & 0xF0) == 0) {
            buf.append('0').append(Integer.toHexString(b).toUpperCase());
          } else {
            buf.append(Integer.toHexString(0xFF & b).toUpperCase());
          }
        }
        return buf.toString();
      } else {
        CommonArrayFS fs;
        String[] fsvalues;

        switch (arrayType) {
          case LowLevelCAS.TYPE_CLASS_INTARRAY:
            fs = new IntArrayFSImpl(addr, cas);
            break;
          case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
            fs = new FloatArrayFSImpl(addr, cas);
            break;
          case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY:
            fs = new BooleanArrayFSImpl(addr, cas);
            break;
          case LowLevelCAS.TYPE_CLASS_SHORTARRAY:
            fs = new ShortArrayFSImpl(addr, cas);
            break;
          case LowLevelCAS.TYPE_CLASS_LONGARRAY:
            fs = new LongArrayFSImpl(addr, cas);
            break;
          case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY:
            fs = new DoubleArrayFSImpl(addr, cas);
            break;
          default: {
            fs = null;
          }
        }

        if (arrayType == LowLevelCAS.TYPE_CLASS_STRINGARRAY) {
          StringArrayFS strFS = new StringArrayFSImpl(addr, cas);
          fsvalues = strFS.toArray();
        } else {
          fsvalues = fs.toStringArray();
        }

        for (int i = 0; i < fsvalues.length; i++) {
          if (buf.length() > 0) {
            buf.append(' ');
          }
          buf.append(fsvalues[i]);
        }
        return buf.toString();
      }

    }

    /**
     * Converts a CAS ListFS to its string representation for use in multi-valued XMI properties.
     * 
     * @param addr
     *          address of the CAS ListFS
     * @param arrayType
     *          type of the List (defined by constants on this class)
     * 
     * @return String representation of the array
     * @throws SAXException
     */
    private String listToString(int addr, int arrayType) throws SAXException {
      if (addr == CASImpl.NULL) {
        return null;
      }
      StringBuffer buf = new StringBuffer();
      String[] array = null;
      switch (arrayType) {
        case TYPE_CLASS_INTLIST:
          array = listUtils.intListToStringArray(addr);
          break;
        case TYPE_CLASS_FLOATLIST:
          array = listUtils.floatListToStringArray(addr);
          break;
        case TYPE_CLASS_STRINGLIST:
          array = listUtils.stringListToStringArray(addr);
          break;
        case TYPE_CLASS_FSLIST:
          array = listUtils.fsListToXmiIdStringArray(addr, sharedData);
          break;
      }
      if (array.length > 0 && !arrayAndListFSs.put(addr, addr)) {
        reportWarning("Warning: multiple references to a ListFS.  Reference identity will not be preserved.");
      }
      for (int j = 0; j < array.length; j++) {
        buf.append(array[j]);
        if (j < array.length - 1) {
          buf.append(' ');
        }
      }
      return buf.toString();
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
    private final int classifyType(int type) {
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

    /**
     * Populates nsUriToPrefixMap and xmiTypeNames structures based on CAS type system.
     */
    private void initTypeAndNamespaceMappings() {
      nsUriToPrefixMap.put(XMI_NS_URI, XMI_NS_PREFIX);
      xmiTypeNames = new XmlElementName[cas.getTypeSystemImpl().getLargestTypeCode() + 1];

      //Add any namespace prefix mappings used by out of type system data.
      //Need to do this before the in-typesystem namespaces so that the prefix
      //used here are reserved and won't be reused for any in-typesystem namespaces.
      if (this.sharedData != null) {
        Iterator ootsIter = this.sharedData.getOutOfTypeSystemElements().iterator();
        while (ootsIter.hasNext()) {
          OotsElementData oed = (OotsElementData)ootsIter.next();
          String nsUri = oed.elementName.nsUri;
          String qname = oed.elementName.qName;
          String localName = oed.elementName.localName;
          String prefix = qname.substring(0, qname.indexOf(localName)-1);
          nsUriToPrefixMap.put(nsUri, prefix);
          nsPrefixesUsed.add(prefix);
        }
      }
      
      Iterator it = cas.getTypeSystemImpl().getTypeIterator();
      while (it.hasNext()) {
        TypeImpl t = (TypeImpl) it.next();
        xmiTypeNames[t.getCode()] = uimaTypeName2XmiElementName(t.getName());
        // this also populats the nsUriToPrefix map
      }
    }

    /**
     * Converts a UIMA-style dotted type name to the element name that should be used in the XMI
     * serialization. The XMI element name consists of three parts - the Namespace URI, the Local
     * Name, and the QName (qualified name).
     * 
     * @param uimaTypeName
     *          a UIMA-style dotted type name
     * @return a data structure holding the three components of the XML element name
     */
    private XmlElementName uimaTypeName2XmiElementName(String uimaTypeName) {
      // split uima type name into namespace and short name
      String namespace, shortName, nsUri;
      int lastDotIndex = uimaTypeName.lastIndexOf('.');
      if (lastDotIndex == -1) // no namespace
      {
        namespace = null;
        shortName = uimaTypeName;
        nsUri = DEFAULT_NAMESPACE_URI;
      } else {
        namespace = uimaTypeName.substring(0, lastDotIndex);
        shortName = uimaTypeName.substring(lastDotIndex + 1);
        nsUri = "http:///" + namespace.replace('.', '/') + ".ecore";
      }

      // determine what namespace prefix to use
      String prefix = (String) nsUriToPrefixMap.get(nsUri);
      if (prefix == null) {
        if (namespace != null) {
          int secondLastDotIndex = namespace.lastIndexOf('.');
          prefix = namespace.substring(secondLastDotIndex + 1);
        } else {
          prefix = "noNamespace";
        }
        // make sure this prefix hasn't already been used for some other namespace
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

      return new XmlElementName(nsUri, shortName, prefix + ':' + shortName);
    }
    
    /**
     * Serializes all of the out-of-typesystem elements that were recorded
     * in the XmiSerializationSharedData during the last deserialization.
     */
    private void serializeOutOfTypeSystemElements() throws SAXException {
      if (this.sharedData == null)
        return;
      Iterator it = this.sharedData.getOutOfTypeSystemElements().iterator();
      while (it.hasNext()) {
        OotsElementData oed = (OotsElementData)it.next();
        workAttrs.clear();
        // Add ID attribute
        addAttribute(workAttrs, ID_ATTR_NAME, oed.xmiId);

        // Add other attributes
        Iterator attrIt = oed.attributes.iterator();
        while (attrIt.hasNext()) {
          XmlAttribute attr = (XmlAttribute) attrIt.next();
          addAttribute(workAttrs, attr.name, attr.value);
        }
        
        // serialize element
        startElement(oed.elementName, workAttrs, oed.childElements.size());
        
        //serialize features encoded as child elements
        Iterator childElemIt = oed.childElements.iterator();
        while (childElemIt.hasNext()) {
          XmlElementNameAndContents child = (XmlElementNameAndContents)childElemIt.next();
          workAttrs.clear();
          Iterator attrIter = child.attributes.iterator();
          while (attrIter.hasNext()) {
            XmlAttribute attr =(XmlAttribute)attrIter.next();
            addAttribute(workAttrs, attr.name, attr.value);
          }
          
          if (child.contents != null) {
            startElement(child.name, workAttrs, 1);
            addText(child.contents);
          }
          else {
            startElement(child.name, workAttrs, 0);            
          }
          endElement(child.name);
        }
        
        endElement(oed.elementName);
      }
    } 
  }

  public static final String XMI_NS_URI = "http://www.omg.org/XMI";

  public static final String XSI_NS_URI = "http://www.w3.org/2001/XMLSchema-instance";

  public static final String XMI_NS_PREFIX = "xmi";

  public static final String XMI_TAG_LOCAL_NAME = "XMI";

  public static final String XMI_TAG_QNAME = "xmi:XMI";

  public static final XmlElementName XMI_TAG = new XmlElementName(XMI_NS_URI, XMI_TAG_LOCAL_NAME,
          XMI_TAG_QNAME);

  public static final String INDEXED_ATTR_NAME = "_indexed";

  public static final String ID_ATTR_NAME = "xmi:id";

  public static final String XMI_VERSION_LOCAL_NAME = "version";

  public static final String XMI_VERSION_QNAME = "xmi:version";

  public static final String XMI_VERSION_VALUE = "2.0";

  /** Namespace URI to use for UIMA types that have no namespace (the "default pacakge" in Java) */
  public static final String DEFAULT_NAMESPACE_URI = "http:///uima/noNamespace.ecore";

  private TypeSystemImpl filterTypeSystem;

  // UIMA logger, to which we may write warnings
  private Logger logger;

  private Map nsUriToSchemaLocationMap = null;

  /**
   * Creates a new XmiCasSerializer.
   * 
   * @param ts
   *          the TypeSystem of CASes that will be serialized. If any CAS that is later passed to
   *          the <code>serialize</code> method that contains types and features that are not in
   *          this typesystem, the serialization will not contain instances of those types or values
   *          for those features. So this can be used to filter the results of serialization.
   * @param nsUriToSchemaLocation
   *          Map if supplied, this map is used to generate a "schemaLocation" attribute in the XMI
   *          output. This argument must be a map from namespace URIs to the schema location for
   *          that namespace URI.
   */
  public XmiCasSerializer(TypeSystem ts, Map nsUriToSchemaLocationMap) {
    super();
    // System.out.println("Creating serializer for type system.");
    this.filterTypeSystem = (TypeSystemImpl) ts;
    this.nsUriToSchemaLocationMap = nsUriToSchemaLocationMap;
    this.logger = UIMAFramework.getLogger(XmiCasSerializer.class);
  }

  /**
   * Creates a new XmiCasSerializer.
   * 
   * @param ts
   *          the TypeSystem of CASes that will be serialized. If any CAS that is later passed to
   *          the <code>serialize</code> method that contains types and features that are not in
   *          this typesystem, the serialization will not contain instances of those types or values
   *          for those features. So this can be used to filter the results of serialization.
   *          A null value indicates that all types and features  will be serialized.
   */
  public XmiCasSerializer(TypeSystem ts) {
    this(ts, (Map) null);
  }

  /**
   * Creates a new XmiCasSerializer.
   * 
   * @param ts
   *          the TypeSystem of CASes that will be serialized. If any CAS that is later passed to
   *          the <code>serialize</code> method that contains types and features that are not in
   *          this typesystem, the serialization will not contain instances of those types or values
   *          for those features. So this can be used to filter the results of serialization.
   * @param uimaContext
   *          not used
   * @param nsUriToSchemaLocation
   *          Map if supplied, this map is used to generate a "schemaLocation" attribute in the XMI
   *          output. This argument must be a map from namespace URIs to the schema location for
   *          that namespace URI.
   * 
   * @deprecated Use {@link #XmiCasSerializer(TypeSystem, Map)} instead. The UimaContext reference
   *             is never used by this implementation.
   */
  public XmiCasSerializer(TypeSystem ts, UimaContext uimaContext, Map nsUriToSchemaLocationMap) {
    this(ts, nsUriToSchemaLocationMap);
  }

  /**
   * Creates a new XmiCasSerializer.
   * 
   * @param ts
   *          the TypeSystem of CASes that will be serialized. If any CAS that is later passed to
   *          the <code>serialize</code> method that contains types and features that are not in
   *          this typesystem, the serialization will not contain instances of those types or values
   *          for those features. So this can be used to filter the results of serialization.
   * @param uimaContext
   *          not used
   * 
   * @deprecated Use {@link #XmiCasSerializer(TypeSystem)} instead. The UimaContext reference is
   *             never used by this implementation.
   */
  public XmiCasSerializer(TypeSystem ts, UimaContext uimaContext) {
    this(ts);
  }

  /**
   * Write the CAS data to a SAX content handler.
   * 
   * @param cas
   *          The CAS to be serialized.
   * @param contentHandler
   *          The SAX content handler the data is written to. should be inserted into the XCAS
   *          output
   * 
   * @throws IOException
   * @throws SAXException
   */
  public void serialize(CAS cas, ContentHandler contentHandler) throws SAXException {
    this.serialize(cas, contentHandler, null);
  }

  /**
   * Write the CAS data to a SAX content handler.
   * 
   * @param cas
   *          The CAS to be serialized.
   * @param contentHandler
   *          The SAX content handler the data is written to. should be inserted into the XCAS
   *          output
   * 
   * @throws IOException
   * @throws SAXException
   */
  public void serialize(CAS cas, ContentHandler contentHandler, ErrorHandler errorHandler)
          throws SAXException {
    contentHandler.startDocument();
    XmiCasDocSerializer ser = new XmiCasDocSerializer(contentHandler, errorHandler, ((CASImpl) cas)
            .getBaseCAS(), null);
    ser.serialize();
    contentHandler.endDocument();
  }

  /**
   * Write the CAS data to a SAX content handler.
   * 
   * @param cas
   *          The CAS to be serialized.
   * @param contentHandler
   *          The SAX content handler the data is written to. should be inserted into the XCAS
   *          output
   * @param sharedData
   *          data structure used to allow the XmiCasSerializer and XmiCasDeserializer to share
   *          information.
   * @throws IOException
   * @throws SAXException
   */
  public void serialize(CAS cas, ContentHandler contentHandler, ErrorHandler errorHandler,
          XmiSerializationSharedData sharedData) throws SAXException {
    contentHandler.startDocument();
    XmiCasDocSerializer ser = new XmiCasDocSerializer(contentHandler, errorHandler, ((CASImpl) cas)
            .getBaseCAS(), sharedData);
    ser.serialize();
    contentHandler.endDocument();
  }

  /**
   * Serializes a CAS to an XMI stream.
   * 
   * @param aCAS
   *          CAS to serialize.
   * @param aStream
   *          output stream to which to write the XMI document
   * 
   * @throws SAXException
   *           if a problem occurs during XMI serialization
   * @throws IOException
   *           if an I/O failure occurs
   */
  public static void serialize(CAS aCAS, OutputStream aStream) throws SAXException {
    serialize(aCAS, null, aStream, false, null);
  }

  /**
   * Serializes a CAS to an XMI stream. Allows a TypeSystem to be specified, to which the produced
   * XMI will conform. Any types or features not in the target type system will not be serialized.
   * 
   * @param aCAS
   *          CAS to serialize.
   * @param aTargetTypeSystem
   *          type system to which the produced XMI will conform. Any types or features not in the
   *          target type system will not be serialized.  A null value indicates that all types and features
   *          will be serialized.
   * @param aStream
   *          output stream to which to write the XMI document
   * 
   * @throws SAXException
   *           if a problem occurs during XMI serialization
   * @throws IOException
   *           if an I/O failure occurs
   */
  public static void serialize(CAS aCAS, TypeSystem aTargetTypeSystem, OutputStream aStream)
          throws SAXException {
    serialize(aCAS, aTargetTypeSystem, aStream, false, null);
  }
  
  /**
   * Serializes a CAS to an XMI stream.  This version of this method allows many options to be configured.
   * 
   * @param aCAS
   *          CAS to serialize.
   * @param aTargetTypeSystem
   *          type system to which the produced XMI will conform. Any types or features not in the
   *          target type system will not be serialized.  A null value indicates that all types and features
   *          will be serialized.
   * @param aStream
   *          output stream to which to write the XMI document
   * @param aPrettyPrint
   *          if true the XML output will be formatted with newlines and indenting.  If false it will be unformatted.
   * @param aSharedData
   *          an optional container for data that is shared between the {@link XmiCasSerializer} and the {@link XmiCasDeserializer}.
   *          See the JavaDocs for {@link XmiSerializationSharedData} for details.
   * 
   * @throws SAXException
   *           if a problem occurs during XMI serialization
   * @throws IOException
   *           if an I/O failure occurs
   */
  public static void serialize(CAS aCAS, TypeSystem aTargetTypeSystem, OutputStream aStream, boolean aPrettyPrint, 
          XmiSerializationSharedData aSharedData)
          throws SAXException {
    XmiCasSerializer xmiCasSerializer = new XmiCasSerializer(aTargetTypeSystem);
    XMLSerializer sax2xml = new XMLSerializer(aStream, aPrettyPrint);
    xmiCasSerializer.serialize(aCAS, sax2xml.getContentHandler(), null, aSharedData);
  }  
}
