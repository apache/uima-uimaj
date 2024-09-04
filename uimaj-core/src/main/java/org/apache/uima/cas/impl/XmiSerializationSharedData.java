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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.internal.util.Int2ObjHashMap;
import org.apache.uima.internal.util.Obj2IntIdentityHashMap;
import org.apache.uima.internal.util.XmlAttribute;
import org.apache.uima.internal.util.XmlElementName;
import org.apache.uima.internal.util.XmlElementNameAndContents;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.TOP;
import org.xml.sax.Attributes;

/**
 * A container for data that is shared between the {@link XmiCasSerializer} and the
 * {@link XmiCasDeserializer}. This has a number of uses:
 * <ul>
 * <li>Consistency of xmi:id values across serializations. If you pass an
 * <code>XmiSerializationSharedData</code> instance to the deserializer, the deserializer will store
 * information about the incoming xmi:id values. If you then pass the same
 * <code>XmiSerializationSharedData</code> object to the serializer when you attempt to serialize
 * the same CAS, all of the incoming FS will be serialized using the same xmi:id value that they had
 * when they were deserialized.</li>
 * <li>Support for "out-of-typesystem data". If you call the XMI deserializer with the
 * <code>lenient</code> parameter set to true, whenever it encounters an XMI element that doesn't
 * correspond to a type in the type system, it will populate the
 * <code>XmiSerializationSharedData</code> with information about these elements. If you then pass
 * the same <code>XmiSerializationSharedData</code> object to the serializer when you attempt to
 * serialize the same CAS, these out-of-typesystem FS will be reserialized without loss of
 * information. References between in-typesystem and out-of-typesystem FS (in either direction) are
 * maintained as well.</li>
 * <li>After calling the XmiCasSerializer and passing an <code>XmiSerializationSharedData</code>,
 * you can call the {@link #getMaxXmiId()} method to get the maximum xmi:id value in the serialized
 * CAS. This feature, along with the consistency of xmi:id values, allows merging multiple XMI
 * documents into a single CAS. See TODO.</li>
 * </ul>
 * 
 * <p>
 * Inner classes are used to hold information about Feature Structure elements, both for
 * out-of-typesystem data, and also when deserializing pre V3 xmi serializations where the Sofa FS
 * are not guaranteed to come before other Feature Structures that depend on them.
 * </p>
 */
public class XmiSerializationSharedData {
  /**
   * V3: FSs have an id - use that. (Assumes id's are internal ones)
   * 
   * A map from FeatureStructures to xmi:id. This is populated whenever an XMI element is serialized
   * or deserialized. It is used by the getXmiId() method, which is done to ensure a consistent ID
   * for each FS address across multiple serializations.
   */
  private Obj2IntIdentityHashMap<TOP> fsToXmiId = new Obj2IntIdentityHashMap<>(TOP.class,
          TOP._singleton);

  /**
   * A map from xmi:id to FeatureStructure address. This is populated whenever an XMI element is
   * serialized or deserialized. It is used by the getFsAddrForXmiId() method, necessary to support
   * merging multiple XMI CASes into the same CAS object.
   **/
  private Int2ObjHashMap<TOP, TOP> xmiIdToFs = new Int2ObjHashMap<>(TOP.class);

  /**
   * List of OotsElementData objects, each of which captures information about incoming XMI elements
   * that did not correspond to any type in the type system.
   */
  private List<OotsElementData> ootsFs = new ArrayList<>();

  /**
   * Map from the xmi:id (String) of a Sofa to a List of xmi:id's (Strings) for the
   * out-of-typesystem FSs that are members of that Sofa's view.
   */
  private Map<String, List<String>> ootsViewMembers = new HashMap<>();

  /**
   * Map from Feature Structures to OotsElementData object, capturing information about
   * out-of-typesystem features that were part of an in-typesystem FS. These include both features
   * not defined in the typesystem and features that are references to out-of-typesystem elements.
   * This information needs to be included when the FS is subsequently serialized.
   */
  private Map<TOP, OotsElementData> ootsFeatures = new IdentityHashMap<>();

  /**
   * V3: Key is FSArray Map from an FSArray to a list of {@link XmiArrayElement} objects, each of
   * which holds an index and an xmi:id for an out-of-typesystem array element.
   */
  private Map<FSArray, List<XmiArrayElement>> ootsArrayElements = new HashMap<>();

  /**
   * The maximum XMI ID used in the serialization. Used to generate unique IDs if needed.
   */
  private int maxXmiId = 0;

  /**
   * V3: key is TOP, value is TOP
   * 
   * Map from FS of a non-shared multi-valued (Array/List) FS to the FS address of the encompassing
   * FS which has a feature whose value is this multi-valued FS. Used when deserializing a Delta CAS
   * to find and serialize the encompassing FS when the non-shared array/list FS is modified.
   */
  Map<TOP, TOP> nonsharedfeatureIdToFSId = new IdentityHashMap<>();
  // Int2IntHashMap nonsharedfeatureIdToFSId = new Int2IntHashMap();

  void addIdMapping(TOP fs, int xmiId) {
    fsToXmiId.put(fs, xmiId);
    xmiIdToFs.put(xmiId, fs);
    if (xmiId > maxXmiId) {
      maxXmiId = xmiId;
    }
  }

  public String getXmiId(TOP fs) {
    return Integer.toString(getXmiIdAsInt(fs));
  }

  /**
   * Gets the FS address that corresponds to the given xmi:id, in the most recent serialization or
   * deserialization.
   * 
   * @param xmiId
   *          an xmi:id from the most recent XMI CAS that was serialized or deserialized.
   * @return the FeatureStructure corresponding to that xmi:id, null if none.
   */
  public TOP getFsForXmiId(int xmiId) {
    return xmiIdToFs.get(xmiId);
  }

  int getXmiIdAsInt(TOP fs) {
    // see if we already have a mapping
    int xmiId = fsToXmiId.get(fs);
    if (xmiId == 0) {
      // to be sure we get a unique Id, increment maxXmiId and use that
      xmiId = ++maxXmiId;
      addIdMapping(fs, xmiId);
    }
    return xmiId;
  }

  /**
   * Gets the maximum xmi:id that has been generated or read so far.
   * 
   * @return the maximum xmi:id
   */
  public int getMaxXmiId() {
    return maxXmiId;
  }

  // /**
  // * Gets the FS address that corresponds to the given xmi:id, in the most
  // * recent serialization or deserialization.
  // *
  // * @param xmiId an xmi:id from the most recent XMI CAS that was serialized
  // * or deserialized.
  // * @return the FS address of the FeatureStructure corresponding to that
  // * xmi:id, -1 if none.
  // */
  // public int getFsAddrForXmiId(int xmiId) {
  // final int addr = xmiIdToFs.get(xmiId);
  // return addr == 0 ? -1 : addr;
  // }

  /**
   * Clears the ID mapping information that was populated in previous serializations or
   * deserializations. TODO: maybe a more general reset that resets other things?
   */
  public void clearIdMap() {
    fsToXmiId.clear();
    xmiIdToFs.clear();
    nonsharedfeatureIdToFSId.clear();
    maxXmiId = 0;
  }

  /**
   * Records information about an XMI element that was not an instance of any type in the type
   * system.
   * 
   * @param elemData
   *          information about the out-of-typesystem XMI element
   */
  public void addOutOfTypeSystemElement(OotsElementData elemData) {
    ootsFs.add(elemData);
    // check if we need to update max ID
    int xmiId = Integer.parseInt(elemData.xmiId);
    if (xmiId > maxXmiId) {
      maxXmiId = xmiId;
    }
  }

  /**
   * Gets a List of {@link org.apache.uima.cas.impl.XmiSerializationSharedData.OotsElementData}
   * objects, each of which describes an incoming XMI element that did not correspond to a Type in
   * the TypeSystem.
   * 
   * @return List of {@link org.apache.uima.cas.impl.XmiSerializationSharedData.OotsElementData}
   *         objects
   */
  public List<OotsElementData> getOutOfTypeSystemElements() {
    return Collections.unmodifiableList(ootsFs);
  }

  /**
   * Records that an out-of-typesystem XMI element should be a member of the specified view.
   * 
   * @param sofaXmiId
   *          xmi:id of a Sofa
   * @param memberXmiId
   *          xmi:id of an out-of-typesystem element that should be a member of the view for the
   *          given Sofa
   */
  public void addOutOfTypeSystemViewMember(String sofaXmiId, String memberXmiId) {
    List<String> membersList = ootsViewMembers.computeIfAbsent(sofaXmiId, k -> new ArrayList<>());
    membersList.add(memberXmiId);
  }

  /**
   * Gets a List of xmi:id's (Strings) of all out-of-typesystem XMI elements that are members of the
   * view with the given id.
   * 
   * @param sofaXmiId
   *          xmi:id of a Sofa
   * @return List of xmi:id's of members of the view for the given Sofa.
   */
  public List<String> getOutOfTypeSystemViewMembers(String sofaXmiId) {
    List<String> members = ootsViewMembers.get(sofaXmiId);
    return members == null ? null : Collections.unmodifiableList(members);
  }

  /**
   * Records an out-of-typesystem attribute that belongs to an in-typesystem FS. This will be added
   * to the attributes when that FS is reserialized.
   * 
   * @param fs
   *          the FS
   * @param featName
   *          name of the feature
   * @param featVal
   *          value of the feature, as a string
   */
  public void addOutOfTypeSystemAttribute(TOP fs, String featName, String featVal) {
    OotsElementData oed = ootsFeatures.get(fs);
    if (oed == null) {
      oed = new OotsElementData(null, null, -1, -1);
      ootsFeatures.put(fs, oed);
    }
    oed.attributes.add(new XmlAttribute(featName, featVal));
  }

  /**
   * Records out-of-typesystem child elements that belong to an in-typesystem FS. These will be
   * added to the child elements when that FS is reserialized.
   * 
   * @param fs
   *          the FS
   * @param featName
   *          name of the feature (element tag name)
   * @param featVals
   *          values of the feature, as a List of strings
   */
  public void addOutOfTypeSystemChildElements(TOP fs, String featName, ArrayList<String> featVals) {
    OotsElementData oed = ootsFeatures.get(fs);
    if (oed == null) {
      oed = new OotsElementData(null, null, -1, -1);
      ootsFeatures.put(fs, oed);
    }
    addOutOfTypeSystemFeature(oed, featName, featVals);
  }

  public static void addOutOfTypeSystemFeature(OotsElementData oed, String featName,
          ArrayList<String> featVals) {
    oed.multiValuedFeatures.add(new NameMultiValue(featName, featVals));
    XmlElementName elemName = new XmlElementName("", featName, featName);
    for (String val : featVals) {
      oed.childElements.add(new XmlElementNameAndContents(elemName, val));
    }
  }

  /**
   * Gets information about out-of-typesystem features that belong to an in-typesystem FS.
   * 
   * @param fs
   *          the FS
   * @return object containing information about out-of-typesystem features (both attributes and
   *         child elements)
   */
  public OotsElementData getOutOfTypeSystemFeatures(TOP fs) {
    return ootsFeatures.get(fs);
  }

  /**
   * Get all FS Addresses that have been added to the id map.
   * 
   * @return an array containing all the FS addresses
   */
  public TOP[] getAndSortByIdAllFSsInIdMap() {
    TOP[] keys = fsToXmiId.getKeys();
    Arrays.sort(keys, (fs1, fs2) -> Integer.compare(fs1._id, fs2._id));
    return keys;
  }

  /**
   * Gets information about out-of-typesystem array elements.
   * 
   * @param fsarray
   *          an FSArray
   * @return a List of {@link org.apache.uima.cas.impl.XmiSerializationSharedData.XmiArrayElement}
   *         objects, each of which holds the index and xmi:id of an array element that is a
   *         reference to an out-of-typesystem FS.
   */
  public List<XmiArrayElement> getOutOfTypeSystemArrayElements(FSArray fsarray) {
    return ootsArrayElements.get(fsarray);
  }

  public boolean hasOutOfTypeSystemArrayElements() {
    return ootsArrayElements != null && ootsArrayElements.size() > 0;
  }

  /**
   * Records an out-of-typesystem array element in the XmiSerializationSharedData.
   * 
   * @param fsarray
   *          The FSArray
   * @param index
   *          index into array
   * @param xmiId
   *          xmi:id of the out-of-typesystem element that is the value at the given index
   */
  public void addOutOfTypeSystemArrayElement(FSArray fsarray, int index, int xmiId) {
    List<XmiArrayElement> list = ootsArrayElements.computeIfAbsent(fsarray, k -> new ArrayList<>());
    list.add(new XmiArrayElement(index, Integer.toString(xmiId)));
  }

  /**
   * Add mapping between the address of FS that is the value of a non-shared multi-valued feature of
   * a FeatureStructure.
   * 
   * @param nonsharedFS
   *          - The non-shared Feature Structure having a multi-valued feature value
   * @param fs
   *          - the encompassing Feature Structure
   */
  public void addNonsharedRefToFSMapping(TOP nonsharedFS, TOP fs) {
    nonsharedfeatureIdToFSId.put(nonsharedFS, fs);
  }

  /**
   * 
   * @return the non-shared featureId to FS Id key set
   */
  public TOP[] getNonsharedMulitValuedFSs() {
    return getSortedKeys(nonsharedfeatureIdToFSId);
  }

  private TOP[] getSortedKeys(Map<TOP, ?> map) {
    TOP[] keys = map.keySet().toArray(new TOP[map.size()]);
    Arrays.sort(keys, (fs1, fs2) -> Integer.compare(fs1._id, fs2._id));
    return keys;
  }

  /**
   * 
   * @param nonsharedFS
   *          a nonsharedFS
   * @return the encompassing FS or null if not found
   */
  public TOP getEncompassingFS(TOP nonsharedFS) {
    return nonsharedfeatureIdToFSId.get(nonsharedFS);
  }

  /**
   * For debugging purposes only.
   */
  // void checkForDups() {
  // BitSet ids = new BitSet();
  // IntListIterator iter = fsToXmiId.keyIterator();
  // while (iter.hasNext()) {
  // int xmiId = iter.next();
  // if (ids.get(xmiId)) {
  // throw new RuntimeException("Duplicate ID " + xmiId + "!");
  // }
  // ids.set(xmiId);
  // }
  // }

  /**
   * For debugging purposes only.
   */
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    TOP[] keys = getAndSortByIdAllFSsInIdMap();
    for (TOP fs : keys) {
      buf.append(fs._id).append(": ").append(fsToXmiId.get(fs)).append('\n');
    }
    return buf.toString();
  }

  /**
   * <p>
   * Data structure holding all information about an XMI element containing an out-of-typesystem FS.
   * </p>
   * 
   * <p>
   * Also used to hold information for deferring deserialization of subtypes of AnnotationBase when
   * the sofa is not yet known
   * </p>
   * 
   */
  public static class OotsElementData {
    /**
     * xmi:id of the element
     */
    final String xmiId;

    /**
     * Name of the element, including XML namespace.
     */
    final XmlElementName elementName;

    /**
     * List of XmlAttribute objects each holding name and value of an attribute.
     */
    public final List<XmlAttribute> attributes = new ArrayList<>();

    /**
     * List of XmlElementNameAndContents objects each describing one of the child elements
     * representing features of this out-of-typesystem element.
     */
    final List<XmlElementNameAndContents> childElements = new ArrayList<>();

    final ArrayList<NameMultiValue> multiValuedFeatures = new ArrayList<>();

    final int lineNumber;

    final int colNumber;

    public OotsElementData(String xmiId, XmlElementName elementName) {
      this(xmiId, elementName, -1, -1);
    }

    public OotsElementData(String xmiId, XmlElementName elementName, int lineNumber,
            int colNumber) {
      this.xmiId = xmiId;
      this.elementName = elementName;
      this.lineNumber = lineNumber;
      this.colNumber = colNumber;
    }

    public Attributes getAttributes() {
      return new Attributes() {

        @Override
        public int getLength() {
          return attributes.size();
        }

        @Override
        public String getURI(int index) {
          throw new UnsupportedOperationException();
        }

        @Override
        public String getLocalName(int index) {
          throw new UnsupportedOperationException();
        }

        @Override
        public String getQName(int index) {
          return attributes.get(index).name;
        }

        @Override
        public String getType(int index) {
          throw new UnsupportedOperationException();
        }

        @Override
        public String getValue(int index) {
          return attributes.get(index).value;
        }

        @Override
        public int getIndex(String uri, String localName) {
          throw new UnsupportedOperationException();
        }

        @Override
        public int getIndex(String qName) {
          int i = 0;
          for (XmlAttribute attr : attributes) {
            if (attr.name.equals(qName)) {
              return i;
            }
            i++;
          }
          return -1;
        }

        @Override
        public String getType(String uri, String localName) {
          throw new UnsupportedOperationException();
        }

        @Override
        public String getType(String qName) {
          throw new UnsupportedOperationException();
        }

        @Override
        public String getValue(String uri, String localName) {
          throw new UnsupportedOperationException();
        }

        @Override
        public String getValue(String qName) {
          for (XmlAttribute attr : attributes) {
            if (attr.name.equals(qName)) {
              return attr.value;
            }
          }
          return null;
        }
      };
    }
  }

  /**
   * Data structure holding the index and the xmi:id of an array or list element that is a reference
   * to an out-of-typesystem FS.
   */
  public static class XmiArrayElement {
    public final int index;

    public final String xmiId;

    XmiArrayElement(int index, String xmiId) {
      this.index = index;
      this.xmiId = xmiId;
    }
  }

  public static class NameMultiValue {
    public final String name;
    public final ArrayList<String> values;

    NameMultiValue(String name, ArrayList<String> values) {
      this.name = name;
      this.values = values;
    }
  }
}
