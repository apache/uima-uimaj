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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.transform.OutputKeys;

import org.apache.uima.UimaContext;
import org.apache.uima.UimaSerializable;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.Pair;
import org.apache.uima.internal.util.StringUtils;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * XCAS serializer. Create a serializer from a type system, then encode individual CASes by writing
 * to a SAX content handler. This class is thread safe. *
 */
public class XCASSerializer {

  private int numChildren;

  public int getNumChildren() {
    return numChildren;
  }

  /**
   * Use an inner class to hold the data for serializing a CAS. Each call to serialize() creates its
   * own instance.
   */
  private class XCASDocSerializer {

    // Where the output goes.
    // private SAXDocStack xmlStack;
    private ContentHandler ch;

    // The CAS we're serializing.
    private CASImpl cas;

    /**
     * Any FS reference we've touched goes in here. value is index repo (first one?), or
     * MULTIPLY_INDEXED
     */
    private final Map<TOP, Integer> queued = new IdentityHashMap<>();

    private static final int NOT_INDEXED = -1;

    private static final int MULTIPLY_INDEXED = -2;

    private static final int INVALID_INDEX = -3;

    /**
     * Any FS indexed in more than one IR goes in here, the value is the associated duplicate key,
     * Key is used to index into dupVectors
     */
    private final Map<TOP, Integer> duplicates = new IdentityHashMap<>();

    /**
     * A key identifying a particular FS indexed in multiple indexes. Starts a 0, incr by 1 for each
     * new FS discovered to be indexed in more than one IR
     */
    int numDuplicates;

    /**
     * list of IntVectors holding lists of repo numbers. Indexed by the key above, for fss that are
     * in multiple index repos
     */
    final List<IntVector> dupVectors = new ArrayList<>();

    // next 2 are a pair; the first is a fs, the 2nd is the index repo its indexed in
    /** list of FSs that are in an index somewhere. */
    private final List<TOP> indexedFSs = new ArrayList<>();

    /** Specific IndexRepository for indexed FSs */
    private final IntVector indexReps = new IntVector();

    /** The current queue for FSs to write out. */
    private final Deque<TOP> queue = new ArrayDeque<>();

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
      this.ch = ch;
      this.cas = cas;
      numDuplicates = 0;
    }

    /**
     * Add an address to the queue.
     * 
     * @param fs_id
     *          The address.
     * @return <code>false</code> iff we've seen this address before.
     */
    private boolean enqueue(TOP fs) {
      if (KEY_ONLY_MATCH == isQueued(fs, INVALID_INDEX)) {
        return false;
      }
      int typeCode = fs._getTypeCode();
      // at this point we don't know if this FS is indexed
      queued.put(fs, NOT_INDEXED);
      queue.push(fs);
      final int typeClass = classifyType(fs._getTypeImpl());
      if (typeClass == LowLevelCAS.TYPE_CLASS_FS) {
        if (mOutOfTypeSystemData != null) {
          enqueueOutOfTypeSystemFeatures(fs);
        }
        enqueueFeatures(fs, typeCode);
      } else if (typeClass == LowLevelCAS.TYPE_CLASS_FSARRAY) {
        enqueueFSArray((FSArray) fs);
      }
      return true;
    }

    /**
     * Same as enqueue, but for indexed FSs.
     * 
     * @param fs_id
     *          The address to enqueue.
     */
    private void enqueueIndexed(TOP fs, int indexRep) {
      int status = isQueued(fs, indexRep);
      switch (status) {
        case KEY_NOT_FOUND: // most common case, key not found
          queued.put(fs, indexRep);
          indexedFSs.add(fs);
          indexReps.add(indexRep);
          break;

        case KEY_AND_VALUE_MATCH: // next most common, FS already queued
          break;
        case KEY_ONLY_MATCH: // key is there, indexRep not
          int prevIndex = queued.get(fs);
          if (NOT_INDEXED == prevIndex) {
            // this fs_id added from a previously found reference
            queued.put(fs, indexRep); // set with given index
            break;
          }
          if (MULTIPLY_INDEXED == prevIndex) {
            // this fs already indexed more than once
            int thisDup = duplicates.get(fs);
            dupVectors.get(thisDup).add(indexRep);
            break;
          }
          // first time we notice this FS is indexed in multiple indexes
          duplicates.put(fs, numDuplicates);
          dupVectors.add(new IntVector());
          dupVectors.get(numDuplicates).add(prevIndex);
          dupVectors.get(numDuplicates).add(indexRep);
          numDuplicates++;
          queued.put(fs, MULTIPLY_INDEXED); // mark this fs_id as multiply indexed
          break;
      }
      return;
    }

    private static final int KEY_AND_VALUE_MATCH = 1;

    private static final int KEY_ONLY_MATCH = -1;

    private static final int KEY_NOT_FOUND = 0;

    /**
     * Bad name; check if we've seen this (address, value) before.
     * 
     * @param fs
     *          The Feature Structure.
     * @param value
     *          The index repository
     * @return KEY_AND_VALUE_MATCH iff we've seen (address, value) before. KEY_NOT_FOUND iff the
     *         address has not been seen before. KEY_ONLY_MATCH iff the address has been seen before
     *         with a different value.
     */
    private int isQueued(TOP fs, int value) {
      Integer v = queued.get(fs);
      return (null == v) ? KEY_NOT_FOUND : (value == v) ? KEY_AND_VALUE_MATCH : KEY_ONLY_MATCH;
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
        int nextId = cas.getLastUsedFsId() + 1;
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
      Collection<Sofa> sofaCollection = cas.getBaseIndexRepositoryImpl()
              .<Sofa> getIndexedFSs(Sofa.class);
      int sofaCount = sofaCollection.size();
      if (sofaCount > 0) {
        Sofa[] allSofas = sofaCollection.toArray(new Sofa[sofaCount]);

        // XCAS requires sofas in order of id
        Arrays.sort(allSofas, (fs1, fs2) -> Integer.compare(fs1._id, fs2._id));
        enqueueArray(allSofas, 0);
      }

      // Get indexes for each SofaFS in the CAS
      for (int sofaNum = 1, numViews = cas.getViewCount(); sofaNum <= numViews; sofaNum++) {
        var viewIR = cas.getBaseCAS().getSofaIndexRepository(sofaNum);
        if (viewIR != null) {
          Collection<TOP> fssInView = viewIR.getIndexedFSs();
          if (!fssInView.isEmpty()) {
            enqueueCollection(fssInView, sofaNum);
          }
        }
      }
    }

    private void enqueueArray(TOP[] fss, int sofaNum) {
      for (TOP fs : fss) { // enqueues the fss for one view (incl view 0 - the base view
        enqueueIndexed(fs, sofaNum);
      }
    }

    private void enqueueCollection(Collection<TOP> fss, int sofaNum) {
      for (TOP fs : fss) {
        enqueueIndexed(fs, sofaNum);
      }
    }

    private void enqueueFeaturesOfIndexed() {
      for (TOP fs : indexedFSs) {
        int typeCode = fs._getTypeCode();
        final int typeClass = classifyType(fs._getTypeImpl());
        if (typeClass == LowLevelCAS.TYPE_CLASS_FS) {
          if (mOutOfTypeSystemData != null) {
            enqueueOutOfTypeSystemFeatures(fs);
          }
          enqueueFeatures(fs, typeCode);
        } else if (typeClass == LowLevelCAS.TYPE_CLASS_FSARRAY) {
          enqueueFSArray((FSArray) fs);
        }
      }
    }

    /*
     * Encode all other enqueued (non-indexed) FSs.
     * 
     */
    private void encodeQueued() throws IOException, SAXException {
      for (TOP item : queue) {
        encodeFS(item, null);
      }
    }

    /**
     * Encode an individual FS.
     * 
     * @param fs_id
     *          The address to be encoded.
     * @param isIndexed
     *          If the FS is indexed or not.
     * @throws IOException
     *           passthru
     * @throws SAXException
     *           passthru
     */
    private void encodeFS(TOP fs, IntVector indexRep) throws IOException, SAXException {
      ++fsCount;

      workAttrs.clear();
      // Create an element with the type name as tag.
      // xmlStack.pushElementNode(getTypeName(fs_id));
      // Add indexed info.

      // if (sofaTypeCode == cas.getHeapValue(fs_id) &&
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
      // xmlStack.addAttribute(ID_ATTR_NAME, Integer.toString(fs_id));
      addAttribute(workAttrs, ID_ATTR_NAME, Integer.toString(fs._id));
      final int typeClass = classifyType(fs._getTypeImpl());
      // Call special code according to the type of the FS (special
      // treatment
      // for arrays).
      String[] data = null;
      String typeName = getTypeName(fs);
      switch (typeClass) {
        case LowLevelCAS.TYPE_CLASS_FS: {
          encodeFeatures(fs, workAttrs);
          if (mOutOfTypeSystemData != null) {
            encodeOutOfTypeSystemFeatures(fs, workAttrs); // APL
          }
          String xcasElementName = getXCasElementName(typeName);
          startElement(xcasElementName, workAttrs, 0);
          // xmlStack.commitNode();
          endElement(xcasElementName);
          return;
        }
        case LowLevelCAS.TYPE_CLASS_INTARRAY: {
          data = ((IntegerArray) fs).toStringArray();
          break;
        }
        case LowLevelCAS.TYPE_CLASS_FLOATARRAY: {
          data = ((FloatArray) fs).toStringArray();
          break;
        }
        case LowLevelCAS.TYPE_CLASS_STRINGARRAY: {
          data = ((StringArray) fs).toArray();
          break;
        }
        case LowLevelCAS.TYPE_CLASS_FSARRAY: {
          encodeFSArray((FSArray) fs, workAttrs);
          return;
        }
        case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY: {
          data = ((BooleanArray) fs).toStringArray();
          break;
        }
        case LowLevelCAS.TYPE_CLASS_BYTEARRAY: {
          data = ((ByteArray) fs).toStringArray();
          break;
        }
        case LowLevelCAS.TYPE_CLASS_SHORTARRAY: {
          data = ((ShortArray) fs).toStringArray();
          break;
        }
        case LowLevelCAS.TYPE_CLASS_LONGARRAY: {
          data = ((LongArray) fs).toStringArray();
          break;
        }
        case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY: {
          data = ((DoubleArray) fs).toStringArray();
          break;
        }
        default: {
          // Internal error.
          throw new RuntimeException("Internal error: classifying FS type.");
        }
      } // end of switch
      // common code for most of the cases
      encodePrimitiveTypeArrayFS(data, typeName, workAttrs);
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

    private void encodeFSArray(FSArray fs, AttributesImpl attrs) throws SAXException {
      String typeName = fs._getTypeImpl().getName();
      final int size = fs.size();
      // int pos = cas.getArrayStartAddress(fs_id);
      // xmlStack.addAttribute(ARRAY_SIZE_ATTR, Integer.toString(size));
      // xmlStack.commitNode();
      addAttribute(attrs, ARRAY_SIZE_ATTR, Integer.toString(size));
      if (typeName.endsWith(TypeSystemImpl.ARRAY_TYPE_SUFFIX)) {
        typeName = CASImpl.TYPE_NAME_FS_ARRAY;
      }
      startElement(typeName, attrs, size);
      for (int i = 0; i < size; i++) {
        String val = null;
        // xmlStack.pushTextNode(ARRAY_ELEMENT_TAG);
        // xmlStack.commitNode();
        TOP element = (TOP) fs.get(i);
        if (null == element && mOutOfTypeSystemData != null) {
          // This array element may have been a reference to an OOTS FS.

          List<ArrayElement> ootsElems = mOutOfTypeSystemData.arrayElements.get(fs);
          if (ootsElems != null) {
            Iterator<ArrayElement> iter = ootsElems.iterator();
            // TODO: iteration could be slow for large arrays
            while (iter.hasNext()) {
              ArrayElement ootsElem = iter.next();
              if (ootsElem.index == i) {
                val = mOutOfTypeSystemData.idMap.get(ootsElem.value);
                break;
              }
            }
          }
        } else if (null != element) {
          val = Integer.toString(element._id);
        }

        if (val != null) {
          startElement(ARRAY_ELEMENT_TAG, emptyAttrs, 1);
          addText(val);
        } else {
          startElement(ARRAY_ELEMENT_TAG, emptyAttrs, 0);
        }
        // xmlStack.popNode();
        endElement(ARRAY_ELEMENT_TAG);
      }

      endElement(typeName);
    }

    private void enqueueFSArray(FSArray fs) {
      TOP[] theArray = fs._getTheArray();
      for (TOP element : theArray) {
        if (element != null) {
          enqueue(element);
        }
      }
    }

    /*
     * Encode features of a regular (non-array) FS.
     */
    private void encodeFeatures(TOP fs, AttributesImpl attrs) {
      TypeImpl ti = fs._getTypeImpl();

      for (FeatureImpl fi : ti.getFeatureImpls()) {
        String attrValue;
        if (fi.getRangeImpl().isRefType) {
          TOP v = fs.getFeatureValue(fi);
          attrValue = (null == v) ? null : Integer.toString(v._id);
        } else {
          attrValue = fs.getFeatureValueAsString(fi);
        }
        if (attrValue != null) {
          addAttribute(attrs, featureNames[fi.getCode()], attrValue);
        }
      }
    }

    private void enqueueFeatures(TOP fs, int heapValue) {
      TypeImpl ti = fs._getTypeImpl();

      if (fs instanceof UimaSerializable) {
        ((UimaSerializable) fs)._save_to_cas_data();
      }
      for (FeatureImpl fi : ti.getFeatureImpls()) {
        if (fi.getRangeImpl().isRefType) {
          TOP v = fs.getFeatureValue(fi);
          if (null != v) {
            enqueue(v);
          }
        }
      }
    }

    /*
     * Encode Out-Of-TypeSystem Features.
     */
    private void encodeOutOfTypeSystemFeatures(TOP fs, AttributesImpl attrs) {
      List<Pair<String, Object>> attrList = mOutOfTypeSystemData.extraFeatureValues.get(fs);
      if (attrList != null) {
        for (Pair<String, Object> p : attrList) {
          String sv = (p.u instanceof String) ? (String) p.u : "";
          // remap ID if necessary
          if (p.t.startsWith(REF_PREFIX)) {
            if (sv.startsWith("a")) { // reference to OOTS FS
              // - remap
              p.u = sv = mOutOfTypeSystemData.idMap.get(sv);
            }
          }
          addAttribute(attrs, p.t, sv);
        }
      }
    }

    /*
     * Encode Out-Of-TypeSystem Features.
     */
    private void enqueueOutOfTypeSystemFeatures(TOP fs) {
      List<Pair<String, Object>> attrList = mOutOfTypeSystemData.extraFeatureValues.get(fs);
      if (attrList != null) {
        Iterator<Pair<String, Object>> it = attrList.iterator();
        while (it.hasNext()) {
          Pair<String, Object> p = it.next();
          String sv = (p.u instanceof String) ? (String) p.u : "";
          // remap ID if necessary
          if (p.t.startsWith(REF_PREFIX)) {
            // references whose ID starts with the character 'a' are references to out of type
            // system FS. All other references should be to in-typesystem FS, which we need to
            // enqueue.
            if (p.u instanceof TOP) {
              enqueue((TOP) p.u);
              // enqueue(cas.getFsFromId_checked(Integer.parseInt(attr[1])));
            }
          }
        }
      }
    }

    private final String getTypeName(TOP fs) {
      return fs.getType().getName();
    }

    /**
     * classify the type, without distinguishng list types
     * 
     * @param ti
     *          the type
     * @return the classification
     */
    private final int classifyType(TypeImpl ti) {
      return TypeSystemImpl.getTypeClass(ti);
    }

    /*
     * Produces XCAS from Out-Of-Typesystem data. (APL)
     */
    private void enqueueOutOfTypeSystemData(OutOfTypeSystemData aData) {
      for (FSData fs : aData.fsList) {
        for (Entry<String, Object> entry : fs.featVals.entrySet()) {
          String attrName = entry.getKey();
          if (attrName.startsWith(REF_PREFIX)) {
            Object attrVal = entry.getValue();
            // references whose ID starts with the character 'a' are references to out of type
            // system FS. All other references should be to in-typesystem FS, which we need to
            // enqueue.
            if (attrVal instanceof TOP /* String && !((String)attrVal).startsWith("a") */) {
              enqueue((TOP) attrVal);
              // enqueue(cas.getFsFromId_checked(Integer.parseInt(attrVal)));
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
        for (Entry<String, Object> entry : fs.featVals.entrySet()) {
          String attrName = entry.getKey();
          Object attrVal = entry.getValue();
          if (attrName.startsWith(REF_PREFIX)) {
            if (attrVal instanceof String && ((String) attrVal).startsWith("a")) {
              // "a" prefix indicates a reference from one OOTS FS
              // to another OOTS FS;
              // we need to remap those IDs to the actual IDs used
              // in the XCAS
              attrVal = mOutOfTypeSystemData.idMap.get(attrVal);
            }
          }
          addAttribute(workAttrs, attrName,
                  (attrVal instanceof TOP) ? Integer.toString(((TOP) attrVal)._id)
                          : (String) attrVal);
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
      return StringUtils.replaceAll(StringUtils.replaceAll(aTagName, ":", "_colon_"), "-",
              "_dash_");
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

  // Create own cache of feature names because of _ref_ prefixes.
  private String[] featureNames;

  // name of tag to contain document text
  private String docTypeName = DEFAULT_DOC_TYPE_NAME;

  // value of _content attribute for document text element
  private String docTextFeature = DEFAULT_DOC_TEXT_FEAT;

  public XCASSerializer(TypeSystem ts, UimaContext uimaContext) {
    // System.out.println("Creating serializer for type system.");
    this.ts = (TypeSystemImpl) ts;
    // Create feature name cache.
    final int featArraySize = this.ts.getNumberOfFeatures() + 1;
    featureNames = new String[featArraySize];
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
      featureNames[feat.getCode()] = featName;
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
   * @throws IOException
   *           passed thru
   * @throws SAXException
   *           passed thru
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
   * @throws IOException
   *           passed thru
   * @throws SAXException
   *           passed thru
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
   * @throws IOException
   *           passed thru
   * @throws SAXException
   *           passed thru
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
    serialize(aCAS, aStream, isFormattedOutput, false);
  }

  /**
   * Serializes an XCAS to a stream.
   * 
   * @param aCAS
   *          CAS to serialize.
   * @param aStream
   *          output stream to which to write the XCAS XML document
   * @param isFormattedOutput
   *          if true the XCAS will be serialized formatted *
   * @param useXml_1_1
   *          if true, the output serializer is set with the OutputKeys.VERSION to "1.1".
   * @throws SAXException
   *           if a problem occurs during XCAS serialization
   * @throws IOException
   *           if an I/O failure occurs
   */
  public static void serialize(CAS aCAS, OutputStream aStream, boolean isFormattedOutput,
          boolean useXml_1_1) throws SAXException, IOException {
    XCASSerializer xcasSerializer = new XCASSerializer(aCAS.getTypeSystem());
    XMLSerializer sax2xml = new XMLSerializer(aStream, isFormattedOutput);
    if (useXml_1_1) {
      sax2xml.setOutputProperty(OutputKeys.VERSION, "1.1");
    }
    xcasSerializer.serialize(aCAS, sax2xml.getContentHandler());
  }
}
