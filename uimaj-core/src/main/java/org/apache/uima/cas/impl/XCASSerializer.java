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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.internal.util.IntStack;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.StringUtils;
import org.apache.uima.internal.util.rb_trees.IntRedBlackTree;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * XCAS serializer. Create a serializer from a type system, then encode individual CASes by writing
 * to a SAX content handler. This class is thread safe.
 * 
 * 
 */
public class XCASSerializer {

  private int numChildren;

  public int getNumChildren() {
    return numChildren;
  }

  /**
   * Use an inner class to hold the data for serializing a CAS. Each call to serialize() creates its
   * own instance.
   * 
   * 
   */
  private class XCASDocSerializer {

    // Where the output goes.
    // private SAXDocStack xmlStack;
    private ContentHandler ch;

    // The CAS we're serializing.
    private CASImpl cas;

    // Any FS reference we've touched goes in here.
    private IntRedBlackTree queued;

    private static final int NOT_INDEXED = -1;

    private static final int MULTIPLY_INDEXED = -2;

    private static final int INVALID_INDEX = -3;

    // Any FS indexed in more than one IR goes in here
    private IntRedBlackTree duplicates;

    // Number of FS indexed in more than one IR
    int numDuplicates;

    // Vector of IntVectors for duplicates
    Vector<IntVector> dupVectors;

    // All FSs that are in an index somewhere.
    private IntVector indexedFSs;

    // Specific IndexRepository for indexed FSs
    private IntVector indexReps;

    // The current queue for FSs to write out.
    private IntStack queue;

    // SofaFS type
    private int sofaTypeCode;

    private final AttributesImpl emptyAttrs = new AttributesImpl();

    private AttributesImpl workAttrs = new AttributesImpl();

    private static final String cdataType = "CDATA";

    // For debug statistics.
    private int fsCount = 0;

    // Out-Of-TypeSystem Data to be included in produced XCAS. (APL)
    private OutOfTypeSystemData mOutOfTypeSystemData;

    // We write to a SAXDocStack, a simplified interface to a
    // ContentHandler.
    private XCASDocSerializer(ContentHandler ch, CASImpl cas) {
      super();
      this.ch = ch;
      this.cas = cas;
      this.queued = new IntRedBlackTree();
      this.duplicates = new IntRedBlackTree();
      this.numDuplicates = 0;
      this.dupVectors = new Vector<IntVector>();
      this.queue = new IntStack();
      this.indexedFSs = new IntVector();
      this.indexReps = new IntVector();
      this.sofaTypeCode = cas.ll_getTypeSystem().ll_getCodeForType(
              cas.getTypeSystem().getType(CAS.TYPE_NAME_SOFA));
    }

    /**
     * Add an address to the queue.
     * 
     * @param addr
     *          The address.
     * @return <code>false</code> iff we've seen this address before.
     */
    private boolean enqueue(int addr) {
      if (KEY_ONLY_MATCH == isQueued(addr, INVALID_INDEX)) {
        return false;
      }
      int heapVal = cas.getHeapValue(addr);
      // at this point we don't know if this FS is indexed
      queued.put(addr, NOT_INDEXED);
      queue.push(addr);
      final int typeClass = classifyType(heapVal);
      if (typeClass == LowLevelCAS.TYPE_CLASS_FS) {
        if (mOutOfTypeSystemData != null) {
          enqueueOutOfTypeSystemFeatures(addr);
        }
        enqueueFeatures(addr, heapVal);
      } else if (typeClass == LowLevelCAS.TYPE_CLASS_FSARRAY) {
        enqueueFSArray(addr);
      }
      return true;
    }

    /**
     * Same as enqueue, but for indexed FSs.
     * 
     * @param addr
     *          The address to enqueue.
     */
    private void enqueueIndexed(int addr, int indexRep) {
      int status = isQueued(addr, indexRep);
      switch (status) {
        case KEY_NOT_FOUND: // most common case, key not found
          queued.put(addr, indexRep);
          indexedFSs.add(addr);
          indexReps.add(indexRep);
          break;

        case KEY_AND_VALUE_MATCH: // next most common, FS already queued
          break;
        case KEY_ONLY_MATCH: // key is there, indexRep not
          int prevIndex = queued.get(addr);
          if (NOT_INDEXED == prevIndex) {
            // this addr added from a previously found reference
            queued.put(addr, indexRep); // set with given index
            break;
          }
          if (MULTIPLY_INDEXED == prevIndex) {
            // this addr already indexed more than once
            int thisDup = duplicates.get(addr);
            dupVectors.get(thisDup).add(indexRep);
            break;
          }
          // duplicate index detected!
          duplicates.put(addr, numDuplicates);
          dupVectors.add(new IntVector());
          dupVectors.get(numDuplicates).add(prevIndex);
          dupVectors.get(numDuplicates).add(indexRep);
          numDuplicates++;
          queued.put(addr, MULTIPLY_INDEXED); // mark this addr as multiply indexed
          break;
      }
      return;
    }

    /**
     * Bad name; check if we've seen this (address, value) before.
     * 
     * @param addr
     *          The address.
     * @param value
     *          The index repository
     * @return KEY_AND_VALUE_MATCH iff we've seen (address, value) before. KEY_NOT_FOUND iff the
     *         address has not been seen before. KEY_ONLY_MATCH iff the address has been seen before
     *         with a different value.
     */
    private static final int KEY_AND_VALUE_MATCH = 1;

    private static final int KEY_ONLY_MATCH = -1;

    private static final int KEY_NOT_FOUND = 0;

    private int isQueued(int addr, int value) {
      return containsKeyValuePair(this.queued, addr, value);
    }

    // returns
    // KEY_AND_VALUE_MATCH = 1;
    // KEY_ONLY_MATCH = -1;
    // KEY_NOT_FOUND = 0;
    private final int containsKeyValuePair(IntRedBlackTree rbt, int key, int value) {
      if (rbt.containsKey(key)) {
        if (rbt.get(key) == value) {
          return KEY_AND_VALUE_MATCH;
        }
        return KEY_ONLY_MATCH;
      }
      return KEY_NOT_FOUND;
    }

    /*
     * Version of serialize which also includes OutOfTypeSystemData (obtained from previous
     * deserialization) in the produced XCAS.
     * 
     */
    private void serialize(boolean encodeDoc, OutOfTypeSystemData outOfTypeSystemData)
            throws IOException, SAXException {
      mOutOfTypeSystemData = outOfTypeSystemData;

      int iElementCount = 0;

      enqueueIndexed();
      enqueueFeaturesOfIndexed();
      if (outOfTypeSystemData != null) {
        // Queues out of type system data.
        int nextId = cas.getHeap().getCellsUsed();
        Iterator<FSData> it = outOfTypeSystemData.fsList.iterator();
        while (it.hasNext()) {
          FSData fs = it.next();
          String newId = Integer.toString(nextId++);
          outOfTypeSystemData.idMap.put(fs.id, newId);
          fs.id = newId;
        }
        iElementCount += outOfTypeSystemData.fsList.size();
        enqueueOutOfTypeSystemData(outOfTypeSystemData);
      }
      iElementCount += indexedFSs.size();
      iElementCount += queue.size();

      AttributesImpl rootAttrs = new AttributesImpl();
      rootAttrs.addAttribute("", VERSION_ATTR, VERSION_ATTR, cdataType, CURRENT_VERSION);
      startElement(casTagName, rootAttrs, iElementCount);

      // continue with serialization
      encodeIndexed(); // encodes indexedFSs.size() elements
      encodeQueued(); // encodes queue.size() elements
      if (outOfTypeSystemData != null) {
        // encodes aData.fsList.size() elements
        serializeOutOfTypeSystemData(outOfTypeSystemData);
      }
      endElement(casTagName);
    }

    private void addText(String text) throws SAXException {
      ch.characters(text.toCharArray(), 0, text.length());
    }

    private String replaceInvalidXmlChars(String aString) {
      // first do a scan, so we don't have to change anything if there are
      // no
      // bad charactes
      boolean controlCharFound = false;
      for (int i = 0; i < aString.length(); i++) {
        if (!isValidXmlChar(aString.charAt(i))) {
          controlCharFound = true;
          break;
        }
      }
      if (!controlCharFound) {
        return aString;
      }

      // bad character was found, do another pass and replace all bad
      // chars
      char[] chars = aString.toCharArray();
      for (int i = 0; i < chars.length; i++) {
        if (!isValidXmlChar(chars[i])) {
          // replace invalid XML char with unicode replacement char
          chars[i] = 0xFFFD;
        }
      }
      return new String(chars);
    }

    private boolean isValidXmlChar(char c) {
      return (c >= 0x20 && c < 0xFFFE) || c == 0x09 || c == 0x0A || c == 0x0D;
    }

    private void addAttribute(AttributesImpl attrs, String attrName, String attrValue) {
      // special case: if attrName is "sofaString", we need to check for
      // invalid
      // XML characters in the data, and replace them
      if (CAS.FEATURE_BASE_NAME_SOFASTRING.equals(attrName)) {
        attrValue = replaceInvalidXmlChars(attrValue);
      }
      attrs.addAttribute("", attrName, attrName, cdataType, attrValue);
    }

    private void startElement(String tag, Attributes attrs, int num) throws SAXException {
      numChildren = num;
      // Saxon requirement? Can't set just one of localName & qName to ""
      ch.startElement("", tag, tag, attrs);
    }

    private void endElement(String tag) throws SAXException {
      ch.endElement("", "", tag);
    }

    /*
     * Encode the indexed FS in the queue.
     */
    private void encodeIndexed() throws IOException, SAXException {
      final int max = indexedFSs.size();
      for (int i = 0; i < max; i++) {
        if (MULTIPLY_INDEXED != queued.get(indexedFSs.get(i))) {
          IntVector iv = new IntVector(1);
          iv.add(indexReps.get(i));
          encodeFS(indexedFSs.get(i), iv);
        } else {
          int thisDup = duplicates.get(indexedFSs.get(i));
          encodeFS(indexedFSs.get(i), dupVectors.get(thisDup));
        }
      }
    }

    /**
     * Push the indexed FSs onto the queue.
     */
    private void enqueueIndexed() {
      FSIndexRepositoryImpl ir = (FSIndexRepositoryImpl) cas.getBaseCAS().getBaseIndexRepository();
      int[] fsarray = ir.getIndexedFSs();
      for (int k = 0; k < fsarray.length; k++) {
        enqueueIndexed(fsarray[k], 0);
      }

      // Get indexes for each SofaFS in the CAS
      int numViews = cas.getBaseSofaCount();
      for (int sofaNum = 1; sofaNum <= numViews; sofaNum++) {
        FSIndexRepositoryImpl loopIR = (FSIndexRepositoryImpl) cas.getBaseCAS()
                .getSofaIndexRepository(sofaNum);
        if (loopIR != null) {
          fsarray = loopIR.getIndexedFSs();
          for (int k = 0; k < fsarray.length; k++) {
            enqueueIndexed(fsarray[k], sofaNum);
          }
        }
      }
    }

    private void enqueueFeaturesOfIndexed() {
      final int max = indexedFSs.size();
      for (int i = 0; i < max; i++) {
        int addr = indexedFSs.get(i);
        int heapVal = cas.getHeapValue(addr);
        final int typeClass = classifyType(heapVal);
        if (typeClass == LowLevelCAS.TYPE_CLASS_FS) {
          if (mOutOfTypeSystemData != null) {
            enqueueOutOfTypeSystemFeatures(addr);
          }
          enqueueFeatures(addr, heapVal);
        } else if (typeClass == LowLevelCAS.TYPE_CLASS_FSARRAY) {
          enqueueFSArray(addr);
        }
      }
    }

    /*
     * Encode all other enqueued (non-indexed) FSs.
     * 
     */
    private void encodeQueued() throws IOException, SAXException {
      int addr;
      while (!queue.empty()) {
        addr = queue.pop();
        encodeFS(addr, null);
      }
    }

    /**
     * Encode an individual FS.
     * 
     * @param addr
     *          The address to be encoded.
     * @param isIndexed
     *          If the FS is indexed or not.
     * @throws IOException passthru
     * @throws SAXException passthru
     */
    private void encodeFS(int addr, IntVector indexRep) throws IOException, SAXException {
      ++fsCount;
      workAttrs.clear();
      // Create an element with the type name as tag.
      // xmlStack.pushElementNode(getTypeName(addr));
      // Add indexed info.

      // if (sofaTypeCode == cas.getHeapValue(addr) &&
      // cas.isBackwardCompatibleCas()) {
      // // Don't encode sofaFS if old style application
      // return;
      // }

      if (indexRep != null) {
        if (indexRep.size() == 1) {
          // xmlStack.addAttribute(INDEXED_ATTR_NAME, TRUE_VALUE);
          addAttribute(workAttrs, INDEXED_ATTR_NAME, Integer.toString(indexRep.get(0)));
        } else {
          StringBuilder multIndex = new StringBuilder(); 
          multIndex.append(Integer.toString(indexRep.get(0)));
          for (int mi = 1; mi < indexRep.size(); mi++) {
            multIndex.append(' ').append(Integer.toString(indexRep.get(mi)));
          }
          addAttribute(workAttrs, INDEXED_ATTR_NAME, multIndex.toString());
        }
      }
      // Add ID attribute. We do this for every FS, since otherwise we
      // would
      // have to do a complete traversal of the heap to find out which FSs
      // is
      // actually referenced.
      // xmlStack.addAttribute(ID_ATTR_NAME, Integer.toString(addr));
      addAttribute(workAttrs, ID_ATTR_NAME, Integer.toString(addr));
      final int typeClass = classifyType(cas.getHeapValue(addr));
      // Call special code according to the type of the FS (special
      // treatment
      // for arrays).
      switch (typeClass) {
        case LowLevelCAS.TYPE_CLASS_FS: {
          String typeName = getTypeName(addr);
          encodeFeatures(addr, workAttrs);
          if (mOutOfTypeSystemData != null) {
            encodeOutOfTypeSystemFeatures(addr, workAttrs); // APL
          }
          String xcasElementName = getXCasElementName(typeName);
          startElement(xcasElementName, workAttrs, 0);
          // xmlStack.commitNode();
          endElement(xcasElementName);
          break;
        }
        case LowLevelCAS.TYPE_CLASS_INTARRAY: {
          IntArrayFSImpl fs = new IntArrayFSImpl(addr, cas);
          String[] data = fs.toStringArray();
          encodePrimitiveTypeArrayFS(data, getTypeName(addr), workAttrs);
          // encodeIntArray(addr, workAttrs);
          break;
        }
        case LowLevelCAS.TYPE_CLASS_FLOATARRAY: {
          FloatArrayFSImpl fs = new FloatArrayFSImpl(addr, cas);
          String[] data = fs.toStringArray();
          encodePrimitiveTypeArrayFS(data, getTypeName(addr), workAttrs);
          // encodeFloatArray(addr, workAttrs);
          break;
        }
        case LowLevelCAS.TYPE_CLASS_STRINGARRAY: {
          StringArrayFSImpl fs = new StringArrayFSImpl(addr, cas);
          String[] data = fs.toArray();
          encodePrimitiveTypeArrayFS(data, getTypeName(addr), workAttrs);
          // encodeStringArray(addr, workAttrs);
          break;
        }
        case LowLevelCAS.TYPE_CLASS_FSARRAY: {
          encodeFSArray(addr, workAttrs);
          break;
        }
        case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY: {
          BooleanArrayFSImpl fs = new BooleanArrayFSImpl(addr, cas);
          String[] data = fs.toStringArray();
          encodePrimitiveTypeArrayFS(data, getTypeName(addr), workAttrs);
          break;
        }
        case LowLevelCAS.TYPE_CLASS_BYTEARRAY: {
          ByteArrayFSImpl fs = new ByteArrayFSImpl(addr, cas);
          String[] data = fs.toStringArray();
          encodePrimitiveTypeArrayFS(data, getTypeName(addr), workAttrs);
          break;
        }
        case LowLevelCAS.TYPE_CLASS_SHORTARRAY: {
          ShortArrayFSImpl fs = new ShortArrayFSImpl(addr, cas);
          String[] data = fs.toStringArray();
          encodePrimitiveTypeArrayFS(data, getTypeName(addr), workAttrs);
          break;
        }
        case LowLevelCAS.TYPE_CLASS_LONGARRAY: {
          LongArrayFSImpl fs = new LongArrayFSImpl(addr, cas);
          String[] data = fs.toStringArray();
          encodePrimitiveTypeArrayFS(data, getTypeName(addr), workAttrs);
          break;
        }
        case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY: {
          DoubleArrayFSImpl fs = new DoubleArrayFSImpl(addr, cas);
          String[] data = fs.toStringArray();
          encodePrimitiveTypeArrayFS(data, getTypeName(addr), workAttrs);
          break;
        }
        default: {
          // Internal error.
          System.err.println("Error classifying FS type.");
        }
      }
      // xmlStack.popNode();

    }

    private void encodePrimitiveTypeArrayFS(String[] data, String typeName, AttributesImpl attrs)
            throws SAXException {

      addAttribute(attrs, ARRAY_SIZE_ATTR, Integer.toString(data.length));
      startElement(typeName, attrs, data.length);

      for (int i = 0; i < data.length; i++) {
        startElement(ARRAY_ELEMENT_TAG, emptyAttrs, 1);
        addText(data[i] == null ? "" : data[i]);
        endElement(ARRAY_ELEMENT_TAG);
      }
      endElement(typeName);
    }

    private void encodeFSArray(int addr, AttributesImpl attrs) throws SAXException {
      final String typeName = getTypeName(addr);
      final int size = cas.ll_getArraySize(addr);
      int pos = cas.getArrayStartAddress(addr);
      // xmlStack.addAttribute(ARRAY_SIZE_ATTR, Integer.toString(size));
      // xmlStack.commitNode();
      addAttribute(attrs, ARRAY_SIZE_ATTR, Integer.toString(size));
      startElement(typeName, attrs, size);
      for (int i = 0; i < size; i++) {
        String val = null;
        // xmlStack.pushTextNode(ARRAY_ELEMENT_TAG);
        // xmlStack.commitNode();
        int heapVal = cas.getHeapValue(pos);
        if (heapVal == CASImpl.NULL && mOutOfTypeSystemData != null) {
          // This array element may have been a reference to an OOTS
          // FS.
          List<ArrayElement> ootsElems = mOutOfTypeSystemData.arrayElements.get(Integer.valueOf(addr));
          if (ootsElems != null) {
            Iterator<ArrayElement> iter = ootsElems.iterator();
            // TODO: iteration could be slow for large arrays
            while (iter.hasNext())
            {
              ArrayElement ootsElem = iter.next();
              if (ootsElem.index == i) {
                val = mOutOfTypeSystemData.idMap.get(ootsElem.value);
                break;
              }
            }
          }
        } else if (heapVal != CASImpl.NULL) {
          val = Integer.toString(heapVal);
        }

        if (val != null) {
          startElement(ARRAY_ELEMENT_TAG, emptyAttrs, 1);
          addText(val);
        } else {
          startElement(ARRAY_ELEMENT_TAG, emptyAttrs, 0);
        }
        // xmlStack.popNode();
        endElement(ARRAY_ELEMENT_TAG);
        ++pos;
      }

      endElement(typeName);
    }

    private void enqueueFSArray(int addr) {
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

    /*
     * Encode features of a regular (non-array) FS.
     */
    private void encodeFeatures(int addr, AttributesImpl attrs) {
      int heapValue = cas.getHeapValue(addr);
      int[] feats = ts.ll_getAppropriateFeatures(heapValue);
      int featAddr, featVal;
      String featName, attrValue;
//      boolean nameMapping = false;
//      if (sofaTypeCode == heapValue) {
//        // set flag for SofaID mapping
//        nameMapping = true;
//      }

      for (int i = 0; i < feats.length; i++) {
        featAddr = addr + cas.getFeatureOffset(feats[i]);
        featVal = cas.getHeapValue(featAddr);
        featName = featureNames[feats[i]];
        if (!cas.ll_isRefType(ts.range(feats[i]))) {
          attrValue = cas.getFeatureValueAsString(addr, feats[i]);
//          if (nameMapping && featName.equals(CAS.FEATURE_BASE_NAME_SOFAID) && uimaContext != null) {
//            // map absolute SofaID to that expected by Component
//            attrValue = uimaContext.mapSofaIDToComponentSofaName(attrValue);
//          }
        } else {
          if (featVal == CASImpl.NULL) {
            attrValue = null;
          } else {
            attrValue = Integer.toString(featVal);
          }
        }

        if (attrValue != null && featName != null) {
          addAttribute(attrs, featName, attrValue);
        }
      }
    }

    private void enqueueFeatures(int addr, int heapValue) {
      int[] feats = ts.ll_getAppropriateFeatures(heapValue);
      int featAddr, featVal;

      for (int i = 0; i < feats.length; i++) {
        featAddr = addr + cas.getFeatureOffset(feats[i]);
        featVal = cas.getHeapValue(featAddr);
        if (cas.ll_isRefType(ts.range(feats[i]))) {
          if (featVal == CASImpl.NULL) {
            // break;
          } else {
            enqueue(featVal);
          }

        }
      }
    }

    /*
     * Encode Out-Of-TypeSystem Features.
     */
    private void encodeOutOfTypeSystemFeatures(int addr, AttributesImpl attrs) {
      List<String[]> attrList = mOutOfTypeSystemData.extraFeatureValues.get(Integer.valueOf(addr));
      if (attrList != null) {
        for (String[] attr : attrList) {
          // remap ID if necessary
          if (attr[0].startsWith("_ref_")) {
            if (attr[1].startsWith("a")) { // reference to OOTS FS
              // - remap
              attr[1] = mOutOfTypeSystemData.idMap.get(attr[1]);
            }
          }
          addAttribute(attrs, attr[0], attr[1]);
        }
      }
    }

    /*
     * Encode Out-Of-TypeSystem Features.
     */
    private void enqueueOutOfTypeSystemFeatures(int addr) {
      List<String[]> attrList = mOutOfTypeSystemData.extraFeatureValues.get(Integer.valueOf(addr));
      if (attrList != null) {
        Iterator<String[]> it = attrList.iterator();
        while (it.hasNext()) {
          String[] attr = it.next();
          // remap ID if necessary
          if (attr[0].startsWith("_ref_")) {
            // references whose ID starts with the character 'a' are references to out of type
            // system FS. All other references should be to in-typesystem FS, which we need to
            // enqueue.
            if (!attr[1].startsWith("a")) {
              enqueue(Integer.parseInt(attr[1]));
            }
          }
        }
      }
    }

    private final String getTypeName(int addr) {
      return ts.ll_getTypeForCode(cas.getHeapValue(addr)).getName();
    }

    private final int classifyType(int type) {
      return cas.ll_getTypeClass(type);
    }

    /*
     * Produces XCAS from Out-Of-Typesystem data. (APL)
     */
    private void enqueueOutOfTypeSystemData(OutOfTypeSystemData aData) {
      for (FSData fs : aData.fsList) {
        for (Map.Entry<String, String> entry : fs.featVals.entrySet()) {
          String attrName = entry.getKey();
          if (attrName.startsWith("_ref_")) {
            String attrVal = entry.getValue();
            // references whose ID starts with the character 'a' are references to out of type
            // system FS. All other references should be to in-typesystem FS, which we need to
            // enqueue.
            if (!attrVal.startsWith("a")) {
              enqueue(Integer.parseInt(attrVal));
            }
          }
        }
      }
    }

    private void serializeOutOfTypeSystemData(OutOfTypeSystemData aData) throws SAXException {
      for (FSData fs : aData.fsList) {
        workAttrs.clear();
        // Add indexed info.
        if (fs.indexRep != null) {
          // xmlStack.addAttribute(INDEXED_ATTR_NAME, TRUE_VALUE);
          addAttribute(workAttrs, INDEXED_ATTR_NAME, fs.indexRep);
        }
        // Add ID attribute (remap to new unique integer ID).
        addAttribute(workAttrs, ID_ATTR_NAME, fs.id);

        // Add other attributes (remap OOTS refs)
        for (Map.Entry<String, String> entry : fs.featVals.entrySet()) {
          String attrName = entry.getKey();
          String attrVal = entry.getValue();
          if (attrName.startsWith("_ref_")) {
            if (attrVal.startsWith("a")) {
              // "a" prefix indicates a reference from one OOTS FS
              // to another OOTS FS;
              // we need to remap those IDs to the actual IDs used
              // in the XCAS
              attrVal = mOutOfTypeSystemData.idMap.get(attrVal);
            }
          }
          addAttribute(workAttrs, attrName, attrVal);
        }
        // send events
        String xcasElementName = getXCasElementName(fs.type);
        startElement(xcasElementName, workAttrs, 0);
        endElement(xcasElementName);
      }
    }

  }

  /**
   * Gets the XCAS element name for a CAS type name. The element name is usually the same as the
   * type name, but the sequences _colon_ and _dash_ are translated to the characters : and -,
   * respectively.
   * 
   * @param aCasTypeName
   *          CAS type name
   * @return XCAS element name for this type name
   */
  private String getXCasElementName(String aTagName) {
    if (aTagName.indexOf(':') == -1 && aTagName.indexOf('-') == -1) {
      return aTagName;
    } else {
      // Note: This is really slow so we avoid if possible. -- RJB
      return StringUtils
              .replaceAll(StringUtils.replaceAll(aTagName, ":", "_colon_"), "-", "_dash_");
    }
  }

  public static final String casTagName = "CAS";

  public static final String VERSION_ATTR = "version";
  
  public static final String CURRENT_VERSION = "2";

  public static final String DEFAULT_DOC_TYPE_NAME = "uima.tcas.Document";

  public static final String DEFAULT_DOC_TEXT_FEAT = "text";

  public static final String INDEXED_ATTR_NAME = "_indexed";

  public static final String REF_PREFIX = "_ref_";

  public static final String ID_ATTR_NAME = "_id";

  public static final String CONTENT_ATTR_NAME = "_content";

  public static final String ARRAY_SIZE_ATTR = "size";

  public static final String ARRAY_ELEMENT_TAG = "i";

  public static final String TRUE_VALUE = "true";
  
  private TypeSystemImpl ts;

  private UimaContext uimaContext;

  // Create own cache of feature names because of _ref_ prefixes.
  private String[] featureNames;

  // name of tag to contain document text
  private String docTypeName = DEFAULT_DOC_TYPE_NAME;

  // value of _content attribute for document text element
  private String docTextFeature = DEFAULT_DOC_TEXT_FEAT;

  public XCASSerializer(TypeSystem ts, UimaContext uimaContext) {
    super();
    // System.out.println("Creating serializer for type system.");
    this.ts = (TypeSystemImpl) ts;
    this.uimaContext = uimaContext;
    // Create feature name cache.
    final int featArraySize = this.ts.getNumberOfFeatures() + 1;
    this.featureNames = new String[featArraySize];
    FeatureImpl feat;
    String featName;
    Iterator<Feature> it = this.ts.getFeatures();
    while (it.hasNext()) {
      feat = (FeatureImpl) it.next();
      if (feat.getRange().isPrimitive()) {
        featName = feat.getShortName();
      } else {
        featName = REF_PREFIX + feat.getShortName();
      }
      this.featureNames[feat.getCode()] = featName;
    }
  }

  public XCASSerializer(TypeSystem ts) {
    this(ts, null);
  }

  /**
   * Write the CAS data to a SAX content handler.
   * 
   * @param cas
   *          The CAS to be serialized.
   * @param contentHandler
   *          The SAX content handler the data is written to.
   * @throws IOException passed thru
   * @throws SAXException passed thru
   */
  public void serialize(CAS cas, ContentHandler contentHandler) throws IOException, SAXException {
    serialize(cas, contentHandler, true);
  }

  /**
   * Write the CAS data to a SAX content handler.
   * 
   * @param cas
   *          The CAS to be serialized.
   * @param contentHandler
   *          The SAX content handler the data is written to.
   * @param encodeDoc
   *          If set to false, no uima.tcas.Document structure will be created, and the document
   *          text will not be serialized.
   * @throws IOException passed thru
   * @throws SAXException passed thru
   */
  public void serialize(CAS cas, ContentHandler contentHandler, boolean encodeDoc)
          throws IOException, SAXException {
    serialize(cas, contentHandler, encodeDoc, null);
  }

  /**
   * Write the CAS data to a SAX content handler.
   * 
   * @param cas
   *          The CAS to be serialized.
   * @param contentHandler
   *          The SAX content handler the data is written to.
   * @param encodeDoc
   *          If set to false, no uima.tcas.Document structure will be created, and the document
   *          text will not be serialized.
   * @param outOfTypeSystemData
   *          data not part of the CAS type system, which should be inserted into the XCAS output
   * 
   * @throws IOException passed thru
   * @throws SAXException passed thru
   */
  public void serialize(CAS cas, ContentHandler contentHandler, boolean encodeDoc,
          OutOfTypeSystemData outOfTypeSystemData) throws IOException, SAXException {
    contentHandler.startDocument();
    XCASDocSerializer ser = new XCASDocSerializer(contentHandler, ((CASImpl) cas).getBaseCAS());
    ser.serialize(encodeDoc, outOfTypeSystemData);
    contentHandler.endDocument();
    // System.out.println("Done serializing " + ser.fsCount + " FSs.");
  }

  /**
   * Gets the name of the type representing the document. This will become the name of the XML
   * element that will hold the document text.
   * 
   * @return the document type name
   */
  public String getDocumentTypeName() {
    return docTypeName;
  }

  /**
   * Gets the name of the type representing the document. This will become the name of the XML
   * element that will hold the document text. If not set, defaults to
   * {@link #DEFAULT_DOC_TYPE_NAME}.
   * 
   * @param aDocTypeName
   *          the document type name
   */
  public void setDocumentTypeName(String aDocTypeName) {
    docTypeName = aDocTypeName;
  }

  /**
   * Gets the name of the feature holding the documeng text. This will become the value of the
   * _content attribute on the document element.
   * 
   * @return the document text feature
   */
  public String getDocumentTextFeature() {
    return docTextFeature;
  }

  /**
   * Sets the name of the feature holding the documeng text. This will become the value of the
   * _content attribute on the document element. If not set, defaults to
   * {@link #DEFAULT_DOC_TEXT_FEAT}. If set to null, no _content attribute will be emitted.
   * 
   * @param aDocTextFeature
   *          the document text feature
   */
  public void setDocumentTextFeature(String aDocTextFeature) {
    docTextFeature = aDocTextFeature;
  }

  /**
   * Serializes an XCAS to a stream.
   * 
   * @param aCAS
   *          CAS to serialize.
   * @param aStream
   *          output stream to which to write the XCAS XML document
   * 
   * @throws SAXException
   *           if a problem occurs during XCAS serialization
   * @throws IOException
   *           if an I/O failure occurs
   */
  public static void serialize(CAS aCAS, OutputStream aStream) throws SAXException, IOException {
    XCASSerializer.serialize(aCAS, aStream, false);
  }

  /**
   * Serializes an XCAS to a stream.
   * 
   * @param aCAS
   *          CAS to serialize.
   * @param aStream
   *          output stream to which to write the XCAS XML document
   * @param isFormattedOutput
   *          if true the XCAS will be serialized formatted
   * 
   * @throws SAXException
   *           if a problem occurs during XCAS serialization
   * @throws IOException
   *           if an I/O failure occurs
   */
  public static void serialize(CAS aCAS, OutputStream aStream, boolean isFormattedOutput)
          throws SAXException, IOException {
    XCASSerializer xcasSerializer = new XCASSerializer(aCAS.getTypeSystem());
    XMLSerializer sax2xml = new XMLSerializer(aStream, isFormattedOutput);
    xcasSerializer.serialize(aCAS, sax2xml.getContentHandler());
  }

}
