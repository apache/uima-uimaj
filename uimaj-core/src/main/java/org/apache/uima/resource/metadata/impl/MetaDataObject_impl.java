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

package org.apache.uima.resource.metadata.impl;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.UIMA_UnsupportedOperationException;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextHolder;
import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.resource.metadata.AllowedValue;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.util.ConcurrentHashMapWithProducer;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.NameClassPair;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLSerializer;
import org.apache.uima.util.XMLSerializer.CharacterValidatingContentHandler;
import org.apache.uima.util.XMLizable;
import org.apache.uima.util.impl.Settings_impl;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Abstract base class for all MetaDataObjects in the reference implementation. Provides basic
 * support for getting and setting property values given their names, using bean introspection and reflection.
 * <p>
 * Also provides the ability to write objects to XML and build objects from their DOM
 * representation, as required to implement the {@link XMLizable} interface, which is a
 * superinterface of {@link MetaDataObject}. In future versions, this could be replaced by a
 * non-proprietary XML binding solution such as JAXB or EMF.
 * <p>
 * 
 * The implementation for getting and setting property values uses the JavaBeans introspection API.
 * Therefore subclasses of this class must be valid JavaBeans and either use the standard naming
 * conventions for getters and setters. BeanInfo augmentation is ignored; the implementation here
 * uses the flag IGNORE_ALL_BEANINFO. See <a href="http://java.sun.com/docs/books/tutorial/javabeans/"> 
 * The Java Beans Tutorial</a> for more information.
 * 
 * To support XML Comments, which can occur between any sub-elements, including array values,
 * the "data" for all objects is stored in a pair of ArrayLists; one holds the "name" of the slot,
 * the other the value; comments are interspersed within this list where they occur.
 * 
 * To the extent possible, this should be the *only* data storage used for the xml element.  
 * Subclasses should access these elements on demand.  Data will be read into / written from this
 * representation; Cloning will copy this information.
 * 
 *    For getters that need to do some special initial processing, a global flag will be set whenever
 *    this base code changes the underlying value.
 */
public abstract class MetaDataObject_impl implements MetaDataObject {

  static final long               serialVersionUID     = 5876728533863334480L;

  private static String           PROP_NAME_SOURCE_URL = "sourceUrl";
  private static String           PROP_NAME_INFOSET    = "infoset";

  // note: AttributesImpl is just a "getter" for attributes, has no setter methods,
  // see javadocs for Attributes (sax)
  private static final Attributes EMPTY_ATTRIBUTES = new AttributesImpl();

  /*
   * Cache for Java Bean lookup
   * 
   *   Key: the Class object of the MetaDataObject, corresponds to some XML element
   *   Value: a list of objects, one for each "attribute", holding:
   *     the attribute name
   *     the reader (a Method)
   *     the writer (a Method)
   *     the java Class of the data type of this attribute <converted to a wrapper class for primitives>
   */

  public static class MetaDataAttr {
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      MetaDataAttr other = (MetaDataAttr) obj;
      if (clazz == null) {
        if (other.clazz != null)
          return false;
      } else if (!clazz.equals(other.clazz))
        return false;
      if (name == null) {
        if (other.name != null)
          return false;
      } else if (!name.equals(other.name))
        return false;
      return true;
    }

    final String name;
    final Method reader;
    final Method writer;
    final Class  clazz;

    public MetaDataAttr(String name, Method reader, Method writer, Class clazz) {
      this.name = name;
      this.reader = reader;
      this.writer = writer;
      this.clazz = clazz;
    }
  }

  private static final List<MetaDataAttr> EMPTY_ATTRIBUTE_LIST = Collections.emptyList();

  // Cache for Java Bean info lookup
  // Class level cache (static) for introspection - 30x speedup in CDE for large descriptor
  // holds "filtered" set of Java Bean Info
  private static final transient ConcurrentHashMapWithProducer<Class<? extends MetaDataObject_impl>, MetaDataAttr[]> 
      class2attrsMap =
          new ConcurrentHashMapWithProducer<Class<? extends MetaDataObject_impl>, MetaDataAttr[]>();
  // holds the unfiltered set of Java Bean Info
  private static final transient ConcurrentHashMapWithProducer<Class<? extends MetaDataObject_impl>, MetaDataAttr[]> 
      class2attrsMapUnfiltered =
          new ConcurrentHashMapWithProducer<Class<? extends MetaDataObject_impl>, MetaDataAttr[]>();

  /**
   * methods used for serializing
   *
   */
  public interface Serializer {
    void outputStartElement(Node node,
             String nameSpace, String localName, String qname, Attributes attributes) throws SAXException;
    void outputEndElement(Node node, String aNamespace,
        String localname, String qname) throws SAXException;

    void outputStartElementForArrayElement(Node node,
        String nameSpace, String localName, String qname, Attributes attributes) throws SAXException;
    void outputEndElementForArrayElement(Node node, String aNamespace,
        String localname, String qname) throws SAXException;
    
    void insertNl();
    boolean shouldBeSkipped(PropertyXmlInfo propInfo, Object val, MetaDataObject_impl mdo);
    boolean startElementProperty();

    void deleteNodeStore();
    boolean indentChildElements(XmlizationInfo info, MetaDataObject_impl mdo);
    void saveAndAddNodeStore(Node infoset);
    void addNodeStore();
    
    void writeDelayedStart(String name) throws SAXException;

    void writeSimpleValue(Object val) throws SAXException;
    void writeSimpleValueWithTag(String className, Object value, Node node) throws SAXException;
    
    boolean shouldEncloseInArrayElement(Class propClass);
    boolean isArrayHasIndentableElements(Object array);
    
    void maybeStartArraySymbol() throws SAXException;
    void maybeEndArraySymbol() throws SAXException;

    Node findMatchingSubElement(String elementName);
  }
   
  /**
   * Information, kept globally (by thread) for one serialization
   *   Inherited by some custom impls, e.g. TypeOrFeature_impl
   */
  public static class SerialContext {
    
    final public  ContentHandler ch;
    final public Serializer serializer;
    public SerialContext(ContentHandler ch, Serializer serializer) {
      this.ch = ch;
      this.serializer = serializer;
    }
  }
  
  /**
   * Keeps the serialContext by thread
   *   set when starting to serialize
   *   cleared at the end (in finally clause) to prevent memory leaks
   *   Inherited by some custom impls, e.g. TypeOrFeature_impl
  */
  public static final ThreadLocal<SerialContext> serialContext = new ThreadLocal<SerialContext>();
  
  public static SerialContext getSerialContext(ContentHandler ch) {
    SerialContext sc = serialContext.get();
    if (null == serialContext.get()) {
      sc = new SerialContext(ch, getSerializerFromContentHandler(ch));
      serialContext.set(sc);
    }
    return sc;
  }
  
  private transient URL mSourceUrl;

  // This is only used if we are capturing comments and ignorable whitespace in the XML
  private transient Node infoset = null; // by default, set to null

  public void setInfoset(Node infoset) {
    this.infoset = infoset;
  }

  public Node getInfoset() {
    return infoset;
  }

  /**
   * Creates a new <code>MetaDataObject_impl</code> with null attribute values
   */
  public MetaDataObject_impl() {
  }

  /**
   * Override this method to include additional attributes
   * @return additional attributes
   */
  public List<MetaDataAttr> getAdditionalAttributes() {
    return EMPTY_ATTRIBUTE_LIST;
  }

  /**
   * Like getAttributes, but doesn't filter the attributes.  
   * Design is only for backwards compatibility.  Unfiltered version
   * used only by getAttributeValue and setAttributeValue
   * @return an unfiltered array of Attribute objects associated with this class
   */
  MetaDataAttr[] getUnfilteredAttributes() {
    final Class<? extends MetaDataObject_impl> clazz = this.getClass();
    MetaDataAttr[] attrs = class2attrsMapUnfiltered.get(clazz);
    if (null == attrs) {
      getAttributesFromBeans(clazz);
    }
    return class2attrsMapUnfiltered.get(clazz);
  }

  /**
   * On first call, looks up the information using JavaBeans introspection, but then
   * caches the result for subsequent calls.
   * 
   * Any class which wants to add additional parameters needs to implement / override
   * getAdditionalParameters.
   * @return an array of Attribute objects associated with this class
   */
  MetaDataAttr[] getAttributes() {
    final Class<? extends MetaDataObject_impl> clazz = this.getClass();
    MetaDataAttr[] attrs = class2attrsMap.get(clazz);
    if (null == attrs) {
      getAttributesFromBeans(clazz);
    }
    return class2attrsMap.get(clazz);
  }

  private void getAttributesFromBeans(final Class<? extends MetaDataObject_impl> clazz) {
    PropertyDescriptor[] pds;
    try {
      pds = Introspector.getBeanInfo(clazz, Introspector.IGNORE_ALL_BEANINFO).getPropertyDescriptors();
    } catch (IntrospectionException e) {
      throw new UIMARuntimeException(e);
    }
    ArrayList<MetaDataAttr> resultList = new ArrayList<MetaDataAttr>(pds.length);
    ArrayList<MetaDataAttr> resultListUnfiltered = new ArrayList<MetaDataAttr>(pds.length);
    for (PropertyDescriptor pd : pds) {
      String propName = pd.getName();
      Class<?> propClass = pd.getPropertyType();
      // translate primitive types (int, boolean, etc.) to wrapper classes
      if (null != propClass && propClass.isPrimitive()) {
        propClass = getWrapperClass(propClass);
      }
      MetaDataAttr mda = new MetaDataAttr(propName, pd.getReadMethod(), pd.getWriteMethod(), propClass);
      resultListUnfiltered.add(mda);
      // only include properties with read and write methods,
      // and don't include the SourceUrl property, which is for
      // internal bookkeeping and shouldn't affect object equality
      // and don't include infoset, which is for internal bookkeeping
      // related to comments and whitespace
        if (pd.getReadMethod() != null && pd.getWriteMethod() != null
                && !pd.getName().equals(PROP_NAME_SOURCE_URL)
                && !pd.getName().equals(PROP_NAME_INFOSET)) {
        resultList.add(mda);
      }
    }
    resultList.addAll(getAdditionalAttributes());
    resultListUnfiltered.addAll(getAdditionalAttributes());
    MetaDataAttr[] attrs = resultList.toArray(new MetaDataAttr[resultList.size()]);
    MetaDataAttr[] otherAttrs = class2attrsMap.putIfAbsent(clazz, attrs);

    attrs = resultListUnfiltered.toArray(new MetaDataAttr[resultListUnfiltered.size()]);
    otherAttrs = class2attrsMapUnfiltered.putIfAbsent(clazz, attrs);
    attrs = (otherAttrs != null) ? otherAttrs : attrs;
  }

  /**
   * Returns a list of <code>NameClassPair</code> objects indicating the attributes of this object
   * and the String names of the Classes of the attributes' values. 
   *   For primitive types, the wrapper classes will be
   * returned (e.g. <code>java.lang.Integer</code> instead of int).
   * 
   * Several subclasses override this, to add additional items to the list.
   * 
   * @see org.apache.uima.resource.metadata.MetaDataObject#listAttributes()
   * @deprecated - use getAttributes
   */
  @Deprecated
  public List<NameClassPair> listAttributes() {

    try {
      PropertyDescriptor[] props = getPropertyDescriptors();
      List<NameClassPair> resultList = new ArrayList<NameClassPair>(props.length);
      for (int i = 0; i < props.length; i++) {
        // only list properties with read and write methods,
        // and don't include the SourceUrl property, which is for
        // internal bookkeeping and shouldn't affect object equality
        // and don't include infoset, which is for internal bookkeeping
        // related to comments and whitespace
        if (props[i].getReadMethod() != null && props[i].getWriteMethod() != null
                && !props[i].getName().equals(PROP_NAME_SOURCE_URL)
                && !props[i].getName().equals(PROP_NAME_INFOSET)) {
          String propName = props[i].getName();
          Class propClass = props[i].getPropertyType();
          // translate primitive types (int, boolean, etc.) to wrapper classes
          if (propClass.isPrimitive()) {
            propClass = getWrapperClass(propClass);
          }
          resultList.add(new NameClassPair(propName, propClass.getName()));
        }
      }
      return resultList;
    } catch (IntrospectionException e) {
      throw new UIMARuntimeException(e);
    }
  }

  private Object getAttributeValue(MetaDataAttr attr) {
    final Method reader = attr.reader;
    if (reader != null) {
      try {
        return reader.invoke(this);
      } catch (Exception e) {
        throw new UIMARuntimeException(e);
      }
    }
    return null;
  }

  /**
   * @see org.apache.uima.resource.metadata.MetaDataObject#getAttributeValue(String)
   */
  public Object getAttributeValue(String aName) {
    try {
      MetaDataAttr[] attrs = getUnfilteredAttributes();
      for (MetaDataAttr attr : attrs) {
        if (attr.name.equals(aName)) {
          Method reader = attr.reader;
          if (reader != null) {
            return reader.invoke(this);
          }
        }
      }
      return null;
    } catch (Exception e) {
      throw new UIMARuntimeException(e);
    }
  }

  /**
   * Gets the Class of the given attribute's value. For primitive types, the wrapper classes will be
   * returned (e.g. <code>java.lang.Integer</code> instead of int).
   * 
   * @param aName
   *          name of an attribute
   * 
   * @return Class of value that may be assigned to the named attribute. Returns <code>null</code>
   *         if there is no attribute with the given name.
   */
  public Class getAttributeClass(String aName) {
    MetaDataAttr[] attrList = getAttributes();
    for (MetaDataAttr attr : attrList) {
      if (attr.name.equals(aName)) {
        return attr.clazz;
      }
    }
    return null;
  }

  /**
   * Returns whether this object is modifiable. MetaDataObjects are modifiable by default.
   * 
   * @see org.apache.uima.resource.metadata.MetaDataObject#isModifiable()
   */
  public boolean isModifiable() {
    return true;
  }

  private void setAttributeValue(MetaDataAttr attr, Object aValue) {
    Method writer = attr.writer;
    if (writer != null) {
      try {
        writer.invoke(this, aValue);
      } catch (IllegalArgumentException e) {
        throw new UIMA_IllegalArgumentException(
                UIMA_IllegalArgumentException.METADATA_ATTRIBUTE_TYPE_MISMATCH, new Object[] {
                    aValue, attr.name }, e);
      } catch (Exception e) {
        throw new UIMARuntimeException(e);
      }
    }
  }

  /**
   * @see org.apache.uima.resource.metadata.MetaDataObject#setAttributeValue(String, Object)
   */
  public void setAttributeValue(String aName, Object aValue) {
    try {
      MetaDataAttr[] attrs = getUnfilteredAttributes();
      for (MetaDataAttr attr : attrs) {
        if (attr.name.equals(aName)) {
          Method writer = attr.writer;
          if (writer != null) {
            try {
              writer.invoke(this, new Object[] { aValue });
            } catch (IllegalArgumentException e) {
              throw new UIMA_IllegalArgumentException(
                      UIMA_IllegalArgumentException.METADATA_ATTRIBUTE_TYPE_MISMATCH, new Object[] {
                          aValue, aName }, e);
            }
          } else {
            throw new UIMA_UnsupportedOperationException(
                    UIMA_UnsupportedOperationException.NOT_MODIFIABLE, new Object[] { aName,
                        this.getClass().getName() });
          }
        }
      }
    } catch (UIMA_IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new UIMARuntimeException(e);
    }
  }

  /**
   * Gets the relative path base used to resolve imports. This is equal to the sourceUrl of this
   * object, if known (i.e. if the object was parsed from an XML file or if setSourceUrl was
   * explicitly called). If the source URL is not known, the value of the user.dir System property
   * is returned.
   * 
   * @return the base URL for resolving relative paths in this object
   */
  public URL getRelativePathBase() {
    if (mSourceUrl != null) {
      return mSourceUrl;
    } else {
      try {
        return new File(System.getProperty("user.dir")).toURL();
      } catch (MalformedURLException e) {
        try {
          return new URL("file:/");
        } catch (MalformedURLException e1) {
          // assert false;
          return null;
        }
      }
    }
  }

  /**
   * Gets the URL from which this object was parsed. When this object is parsed from an XML file,
   * this is set by the parser to the URL of the source file XML file. If the object has been
   * created by some other method, the source URL will not be known, and this method will return
   * null.
   * <p>
   * This setting is used to resolve imports and is also included in exception messages to indicate
   * the source of the problem.
   * 
   * @return the source URL from which this object was parsed
   */
  public URL getSourceUrl() {
    return mSourceUrl;
  }

  /**
   * If the sourceURL of this object is non-null, returns its string representation. If it is null,
   * returns "&lt;unknown&gt;". Useful for error messages.
   * 
   * @return the source URL as a string, or "&lt;unknown&gt;"
   */
  public String getSourceUrlString() {
    return mSourceUrl != null ? mSourceUrl.toString() : "<unknown>";
  }

  /**
   * Sets the source URL of this object, only if that URL is currently set to null. This is used
   * internally to update null relative base paths before doing import resolution, without
   * overriding user-specified settings.
   * 
   * @param aUrl
   *          the location of the XML file from which this object was parsed
   */
  public void setSourceUrlIfNull(URL aUrl) {
    if (mSourceUrl == null) {
      setSourceUrl(aUrl);
    }
  }

  /**
   * Sets the URL from which this object was parsed. Typically only the XML parser sets this. This
   * recursively sets the source URL of all descendants of this object.
   * 
   * Recursion doesn't happen for sub arrays/maps of maps or arrays.
   * 
   * @param aUrl
   *          the location of the XML file from which this object was parsed
   */
  public void setSourceUrl(URL aUrl) {
    mSourceUrl = aUrl;

    // set recursively on subobjects
    MetaDataAttr[] attrs = getAttributes();
    for (MetaDataAttr attr : attrs) {
      Object val = getAttributeValue(attr);
      if (val instanceof MetaDataObject_impl) {
        ((MetaDataObject_impl) val).setSourceUrl(aUrl);
      } else if (val != null && val.getClass().isArray()) {
        Object[] arrayVal = (Object[]) val;
        for (Object item : arrayVal) {
          if (item instanceof MetaDataObject_impl) {
            ((MetaDataObject_impl) item).setSourceUrl(aUrl);
          }
        }
      } else if (val instanceof Map) {
        Collection values = ((Map) val).values();
        for (Object value : values) {
          if (value instanceof MetaDataObject_impl) {
            ((MetaDataObject_impl) value).setSourceUrl(aUrl);
          }
        }
      }
    }
  }

  /**
   * @see org.apache.uima.resource.metadata.MetaDataObject#clone()
   * multi-core: could be cloning while another thread is modifying?
   */
  public Object clone() {
    // System.out.println("MetaDataObject_impl: clone");
    MetaDataObject_impl clone = null;
    try {
      // do the default cloning
      clone = (MetaDataObject_impl) super.clone();
    } catch (CloneNotSupportedException e) {
      // assert false : "All MetaDataObjects are Cloneable";
      throw new UIMARuntimeException(e);
    }

    // now clone all values that are MetaDataObjects
    MetaDataAttr[] attrs = getAttributes();
    for (MetaDataAttr attr : attrs) {
      // String attrName = ncp.name;
      Object val = getAttributeValue(attr);
      if (val instanceof MetaDataObject) {
        Object clonedVal = ((MetaDataObject) val).clone();
        clone.setAttributeValue(attr, clonedVal);
      } else if (val != null && val.getClass().isArray()) {
        // clone the array, and clone any MetaDataObjects in the array
        Class componentType = val.getClass().getComponentType();
        int length = Array.getLength(val);
        Object arrayClone = Array.newInstance(componentType, length);
        for (int j = 0; j < length; j++) {
          Object component = Array.get(val, j);
          if (component instanceof MetaDataObject) {
            component = ((MetaDataObject) component).clone();
          }
          Array.set(arrayClone, j, component);
        }
        clone.setAttributeValue(attr, arrayClone);
      }
    }
    return clone;
  }

  /**
   * Dump this metadata object's attributes and values to a String. This is useful for debugging.
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append(getClass().getName()).append(": \n");
    MetaDataAttr[] attrList = getAttributes();
    for (MetaDataAttr attr : attrList) {
      buf.append(attr.name + " = ");
      Object val = getAttributeValue(attr);
      if (val == null)
        buf.append("NULL");
      else if (val instanceof Object[]) {
        Object[] array = (Object[]) val;
        buf.append("Array{");
        for (int j = 0; j < array.length; j++) {
          buf.append(j).append(": ").append(array[j].toString()).append('\n');
        }
        buf.append("}\n");
      } else
        buf.append(val.toString());
      buf.append('\n');
    }
    return buf.toString();
  }

  /**
   * Determines if this object is equal to another. Two MetaDataObjects are equivalent if they share
   * the same attributes and the same values for those attributes.
   * 
   * @param aObj
   *          object with which to compare this object
   * 
   * @return true if and only if this object is equal to <code>aObj</code>
   */
  public boolean equals(Object aObj) {
    if (!(aObj instanceof MetaDataObject_impl)) {
      return false;
    }
    MetaDataObject_impl mdo = (MetaDataObject_impl) aObj;
    // get the attributes (and classes) for the two objects

    MetaDataAttr[] theseAttrs = this.getAttributes();
    MetaDataAttr[] thoseAttrs = mdo.getAttributes();
    // attribute lists must be same length
    if (theseAttrs.length != thoseAttrs.length) {
      return false;
    }
    // iterate through all attributes in this object
    List<MetaDataAttr> thoseAttrsAsList = Arrays.asList(thoseAttrs);
    for (MetaDataAttr attr : theseAttrs) {
      // other object must contain this attribute
      if (!thoseAttrsAsList.contains(attr)) {
        return false;
      }
      // get values and test equivalency
      Object val1 = this.getAttributeValue(attr);
      Object val2 = mdo.getAttributeValue(attr);
      if (!valuesEqual(val1, val2)) {
        return false;
      }
    }
    // if we get this far, objects are equal
    return true;
  }

  /**
   * Compare 2 values for equality.  
   * Reason val1.equals(val2) is not used:
   *   If val1 is of type Object[], the equal test is object identity equality, not
   *      element by element identity.
   *   So we use Arrays.equals or deepEquals instead.
   * 
   * @param val1
   * @param val2
   * @return true if equal
   */
  private boolean valuesEqual(Object val1, Object val2) {
    if (val1 == null) {
      return val2 == null;
    }

    if (val1.getClass().isArray()) {
      if (val2.getClass() != val1.getClass()) {
        return false;
      }
      // some of this may not be necessary - it depends what kind of array values are actually used
      // The "if" statements below are in guessed order of frequency of occurance
      if (val1 instanceof String[])  return Arrays.equals((String[])val1,  (String[])val2);
      // deepEquals handles arrays whose elements are arrays
      if (val1 instanceof Object[])  return Arrays.deepEquals((Object[])val1, (Object[])val2);
      if (val1 instanceof int[])     return Arrays.equals((int[])val1,     (int[])val2);
      if (val1 instanceof float[])   return Arrays.equals((float[])val1,   (float[])val2);
      if (val1 instanceof double[])  return Arrays.equals((double[])val1,  (double[])val2);
      if (val1 instanceof boolean[]) return Arrays.equals((boolean[])val1, (boolean[])val2);
      if (val1 instanceof byte[])    return Arrays.equals((byte[])val1,    (byte[])val2);
      if (val1 instanceof short[])   return Arrays.equals((short[])val1,   (short[])val2);
      if (val1 instanceof long[])    return Arrays.equals((long[])val1,    (long[])val2);
      return Arrays.equals((char[]) val1, (char[]) val2);
    }

    if (val1 instanceof Map) {// only need this to handle Maps w/ array vals
      if (!(val2 instanceof Map)) {
        return false;
      }
      if (((Map) val1).size() != ((Map) val2).size()) {
        return false;
      }

      if (val1.getClass() != val2.getClass()) {
        return false;
      }

      Set entrySet1 = ((Map) val1).entrySet();
      Iterator it = entrySet1.iterator();
      while (it.hasNext()) {
        Map.Entry entry = (Map.Entry) it.next();
        Object subval1 = ((Map) val1).get(entry.getKey());
        Object subval2 = ((Map) val2).get(entry.getKey());
        if (!valuesEqual(subval1, subval2)) {
          return false;
        }
      }
      // for Map values, get here if all values in the map are equal
      return true;
    }
    // not an instance of Map
    return val1.equals(val2);
  }

  /**
   * Gets the hash code for this object. The hash codes of two NameClassPairs <code>x</code> and
   * <code>y</code> must be equal if <code>x.equals(y)</code> returns true;
   * 
   * @return the hash code for this object
   */
  public int hashCode() {
    int hashCode = 0;

    // add the hash codes of all attributes
    MetaDataAttr[] attrs = getAttributes();
    for (MetaDataAttr attr : attrs) {
//      String attrName = attr.name;
      Object val = getAttributeValue(attr);
      if (val != null) {
        if (val instanceof Object[]) {
          hashCode += Arrays.hashCode((Object[]) val);
        } else if (val instanceof Map) {// only need to do this to handle Maps w/ array vals
          // note doesn't handle maps whose values are maps.
          Set<Map.Entry> entrySet = ((Map) val).entrySet();
          for (Map.Entry entry : entrySet) {
            hashCode += entry.getKey().hashCode();
            Object subval = entry.getValue();
            if (subval instanceof Object[]) {
              hashCode += Arrays.hashCode((Object[]) subval);
            } else {
              hashCode += subval.hashCode();
            }
          }
        } else {
          hashCode += val.hashCode();
        }
      }
    }
    return hashCode;
  }

  /**
   * Writes out this object's XML representation.
   * 
   * @param aWriter
   *          a Writer to which the XML string will be written
   */
  public void toXML(Writer aWriter) throws SAXException, IOException {
    toXML(new XMLSerializer(aWriter));
  }

  /**
   * Writes out this object's XML representation.
   * 
   * @param aOutputStream
   *          an OutputStream to which the XML string will be written
   */
  public void toXML(OutputStream aOutputStream) throws SAXException, IOException {
    toXML(new XMLSerializer(aOutputStream));
  }

  private void toXML(XMLSerializer sax2xml) throws SAXException, IOException {
    ContentHandler contentHandler = sax2xml.getContentHandler();
    contentHandler.startDocument();
    toXML(contentHandler, true);  // no reason to create a new content handler
    contentHandler.endDocument();
  }

  /**
   * This is called internally, also for JSon serialization
   * @see org.apache.uima.util.XMLizable#toXML(ContentHandler)
   */
  public void toXML(ContentHandler aContentHandler) throws SAXException {
    toXML(aContentHandler, false);
  }

  /**
   * @see org.apache.uima.util.XMLizable#toXML(org.xml.sax.ContentHandler, boolean)
   * 
   * This is called internally, also for JSon serialization
   * If this is the first call to serialize, create a serialContext (and clean up afterwards)
   * Other callers (e.g. JSON) must set the serialContext first before calling
   */
  public void toXML(ContentHandler aContentHandler, boolean aWriteDefaultNamespaceAttribute)
          throws SAXException {
    if (null == serialContext.get()) {
      getSerialContext(aContentHandler);  
      try {
        toXMLcommon(aWriteDefaultNamespaceAttribute);
      } finally {
        serialContext.remove();
      }
    } else {
      toXMLcommon(aWriteDefaultNamespaceAttribute);
    }
  }
    
  private static Serializer getSerializerFromContentHandler(ContentHandler aContentHandler) {
    return (aContentHandler instanceof CharacterValidatingContentHandler) ? 
        new MetaDataObjectSerializer_indent((CharacterValidatingContentHandler) aContentHandler) : 
        new MetaDataObjectSerializer_plain(aContentHandler);
  }
    
  private void toXMLcommon(boolean aWriteDefaultNamespaceAttribute) 
      throws SAXException {    
    XmlizationInfo inf = getXmlizationInfo();
    final Serializer serializer = serialContext.get().serializer;
    
    // write the element's start tag
    // get attributes (can be provided by subclasses)
    AttributesImpl attrs = getXMLAttributes();
    
    if (aWriteDefaultNamespaceAttribute && inf.namespace != null) {
//      attrs.addAttribute("", "xmlns", "xmlns", "xs:string", inf.namespace);  // NOTE:  Saxon appears to ignore this ??
      // this is the way to add a default namespace, correctly.  Works with Saxon and non-Saxon
      ((MetaDataObjectSerializer_plain)serializer).startPrefixMapping("", inf.namespace); 
    }
    
    // start element
    serializer.outputStartElement(infoset, inf.namespace, inf.elementTagName, inf.elementTagName, attrs);
    serializer.saveAndAddNodeStore(infoset);     // https://issues.apache.org/jira/browse/UIMA-3477

    // write child elements
    try {
      final boolean insertNl = serializer.indentChildElements(inf, this);
      for (PropertyXmlInfo propInf : inf.propertyInfo) {
        if (insertNl) {
          serializer.insertNl();
        }
        writePropertyAsElement(propInf, inf.namespace);
      }
    } finally {
      serializer.deleteNodeStore();
    }
    
    // end element
    serializer.outputEndElement(infoset, inf.namespace, inf.elementTagName, inf.elementTagName);
  }
  
  /**
   * Called by the {@link #toXML(ContentHandler, boolean)} method to get the XML attributes that will be
   * written as part of the element's tag. By default this method returns an empty Attributes
   * object. Subclasses may override it in order to write attributes to the XML.
   * 
   * @return an object defining the attributes to be written to the XML
   */
  protected AttributesImpl getXMLAttributes() {
    return new AttributesImpl();
  }

  /**
   * To be implemented by subclasses to return information describing how to represent this object
   * in XML.
   * 
   * @return information defining this object's XML representation
   */
  protected abstract XmlizationInfo getXmlizationInfo();

  /**
   * Looks in this class's XmlizationInfo for a property with the given XML element name.
   * 
   * @param aXmlElementName
   *          the unqualified name of an XML element
   * 
   * @return information on the property that corresponds to the given element name,
   *         <code>null</code> if none.
   */
  protected PropertyXmlInfo getPropertyXmlInfo(String aXmlElementName) {
    PropertyXmlInfo[] inf = getXmlizationInfo().propertyInfo;
    for (int i = 0; i < inf.length; i++) {
      if (aXmlElementName.equals(inf[i].xmlElementName))
        return inf[i];
    }
    return null;
  }

  public boolean valueIsNullOrEmptyArray(Object val) {
    return (val == null) || (val.getClass().isArray() && ((Object[]) val).length == 0);
  }

  /**
   * Utility method used to write a property out as an XML element.
   * 
   * @param aPropInfo
   *          information on how to represent the property in XML
   * @param aNamespace
   *          XML namespace URI for this object
   *          representation
   * @throws SAXException -
   */
  protected void writePropertyAsElement(PropertyXmlInfo aPropInfo, String aNamespace) throws SAXException {
    final SerialContext sc = serialContext.get();
    final Serializer serializer = sc.serializer;
    // get value of property
    Object val = getAttributeValue(aPropInfo.propertyName);
    // if null or empty array, check to see if we're supposed to omit it
    if (serializer.shouldBeSkipped(aPropInfo, val, this)) {
      return;
    }

    // if XML element name was supplied, write a tag
    final String elementName = aPropInfo.xmlElementName;
    Node elementNode = null;

    if (null != elementName) { // can be null in this case:
      // <fixedFlow> <== after outputting this,
      // there is no <array> conntaining:
      // <node>A</node>
      // <node>B</node>

//      elementNode = findMatchingSubElement(aContentHandler, aPropInfo.xmlElementName);
      elementNode = getMatchingNode(sc, aPropInfo.xmlElementName);
      if (serializer.startElementProperty()) {  // skip for JSON
        serializer.outputStartElement(elementNode, aNamespace, aPropInfo.xmlElementName, aPropInfo.xmlElementName, EMPTY_ATTRIBUTES);
      }
      serializer.addNodeStore();
    }
    
    // get class of property
    Class propClass = getAttributeClass(aPropInfo.propertyName);

    try {
    // if value is null then write nothing
    if (val != null) {
      if (aPropInfo.xmlElementName != null) {
        serializer.writeDelayedStart(aPropInfo.xmlElementName);
      }
      
      // if value is an array then we have to treat that specially
      if (val.getClass().isArray()) {
      writeArrayPropertyAsElement(aPropInfo.propertyName, propClass, val,
              aPropInfo.arrayElementTagName, aNamespace);
      } else {
      // if value is an XMLizable object, call its toXML method
        if (val instanceof XMLizable) {
          ((XMLizable) val).toXML(sc.ch);
        }
        // else, if property's class is java.lang.Object, attempt to write
        // it as a primitive
        else if (propClass == Object.class) {
          writePrimitiveValue(val);
        } else {
          // assume attribute's class is known (e.g. String, Integer), so it
          // is not necessary to write the class name to the XML. Just write
          // the string representation of the object
          // XMLUtils.writeNormalizedString(val.toString(), aWriter, true);
          
          serializer.writeSimpleValue(val);
        }
      }
    }
    } finally {
      if (null != elementName) {
        serializer.deleteNodeStore();
        // if XML element name was supplied, end the element that we started
        if (serializer.startElementProperty()) {  // skip for JSON
          serializer.outputEndElement(elementNode, aNamespace, aPropInfo.xmlElementName, aPropInfo.xmlElementName);
        }
      }
    }
  }

  /**
   * Utility method used to write an array property out as an XML element.
   * 
   * @param aPropName
   *          name of the attribute
   * @param aPropClass
   *          class of the attribute
   * @param aValue
   *          value (guaranteed to be an array and non-null)
   * @param aArrayElementTagName
   *          name of tag to be assigned to each element of the array. May be <code>null</code>,
   *          in which case each element will be assigned a value appropriate to its class.
   * @param aNamespace
   *          the XML namespace URI for this object
   * @throws SAXException -         
   */
  protected void writeArrayPropertyAsElement(String aPropName, Class aPropClass, Object aValue,
          String aArrayElementTagName, String aNamespace)
          throws SAXException {
    final SerialContext sc = serialContext.get();
    final Serializer serializer = sc.serializer;

    // if aPropClass is generic Object, reader won't know whether to expect
    // an array, so we tell it be writing an "array" element here.
    
    Node arraySubElement = null;
    if (serializer.shouldEncloseInArrayElement(aPropClass)) {
      arraySubElement = getMatchingNode(sc, "array");
      serializer.outputStartElement(arraySubElement, aNamespace, "array", "array", EMPTY_ATTRIBUTES);
      serializer.addNodeStore();      
    }
    
    try {
      // iterate through elements of the array (at this point we don't allow
      // nested arrays here
      serializer.maybeStartArraySymbol();  // for JSON      
      if (serializer.isArrayHasIndentableElements(aValue)) {
        serializer.insertNl();  // for JSON
      }
      
      for (final Object curElem : ((Object[]) aValue)) {
        Node matchingArrayElement = getMatchingNode(sc, aArrayElementTagName);
        
        // if a particular array element tag has been specified, write it
        //   (skipped if JSON)
        serializer.outputStartElementForArrayElement(matchingArrayElement, aNamespace, aArrayElementTagName,
            aArrayElementTagName, EMPTY_ATTRIBUTES);
        
        if (curElem instanceof AllowedValue) {
          ((XMLizable)curElem).toXML(sc.ch);
        } else if (curElem instanceof XMLizable) {
          // if attribute's value is an XMLizable object, call its toXML method
          serializer.insertNl();
          ((XMLizable) curElem).toXML(sc.ch);
  
        } else if (aArrayElementTagName == null) {    // else, attempt to write it as a primitive
          writePrimitiveValue(curElem);               // write <tag>value</tag> or {"tag" : "value"}
        } else {                          
          serializer.writeSimpleValue(curElem);          // don't include the type - just write the value
        }
        
        serializer.outputEndElementForArrayElement(matchingArrayElement, 
            aNamespace, aArrayElementTagName, aArrayElementTagName);
      } // end of for loop over all elements of array
      
      serializer.maybeEndArraySymbol(); // for JSON
    } finally {
      if (serializer.shouldEncloseInArrayElement(aPropClass)) {
        serializer.deleteNodeStore(); 
        serializer.outputEndElement(arraySubElement, aNamespace, "array", "array");
      }
    }    
  }

  /**
   * Utility method for writing to XML an property whose value is a <code>Map</code> with
   * <code>String</code> keys and <code>XMLizable</code> values.
   * 
   * @param aPropName
   *          name of the property to write to XML
   * @param aXmlElementName
   *          name of the XML element for the property, <code>null</code> if none
   * @param aKeyXmlAttribute
   *          name of the XML attribute for the key
   * @param aValueTagName
   *          XML element tag name to use for each entry in the Map
   * @param aOmitIfNull
   *          if true, null or empty map will not be written at all, if false, null or empty map
   *          will be written as an empty element
   * @param aNamespace
   *          namespace for this object
   * @throws SAXException  passthru       
   */
  protected void writeMapPropertyToXml(String aPropName, String aXmlElementName,
          String aKeyXmlAttribute, String aValueTagName, boolean aOmitIfNull, String aNamespace) 
              throws SAXException {
    final SerialContext sc = serialContext.get();
    final Serializer serializer = sc.serializer;
    // get map
    @SuppressWarnings("unchecked")
    Map<String, Object> theMap = (Map<String, Object>) getAttributeValue(aPropName);
    Node matchingNode = getMatchingNode(sc, aXmlElementName);

    // if map is empty handle appropriately
    if (theMap == null || theMap.isEmpty()) {
      if (!aOmitIfNull && aXmlElementName != null) {
        serializer.outputStartElement(matchingNode, aNamespace, aXmlElementName, aXmlElementName, EMPTY_ATTRIBUTES);        
        serializer.outputEndElement(matchingNode, aNamespace, aXmlElementName, aXmlElementName);
      }
    } else {  // map is not empty
      // write start tag for attribute if desired
      serializer.outputStartElement(matchingNode, aNamespace, aXmlElementName, aXmlElementName, EMPTY_ATTRIBUTES);
      serializer.addNodeStore();

      try {
        // iterate over entries in the Map
        for (Map.Entry<String, Object> curEntry : theMap.entrySet()) {
          String key = curEntry.getKey();

          // write a tag for the value, with a "key" attribute
          AttributesImpl attrs = new AttributesImpl();
          attrs.addAttribute("", aKeyXmlAttribute, aKeyXmlAttribute, "", key); // nulls not OK - must use ""
          Node innerMatchingNode = getMatchingNode(sc, aValueTagName);
          serializer.outputStartElement(innerMatchingNode, aNamespace, aValueTagName, aValueTagName, attrs);

          // write the value (must be XMLizable or an array of XMLizable)
          Object val = curEntry.getValue();
          if (val.getClass().isArray()) {
            Object[] arr = (Object[]) val;
            for (int j = 0; j < arr.length; j++) {
              XMLizable elem = (XMLizable) arr[j];
              elem.toXML(sc.ch);
            }
          } else {
            serializer.addNodeStore();
            try {
              ((XMLizable) val).toXML(sc.ch);
            } finally {
              serializer.deleteNodeStore();
            }
          }

          // write end tag for the value
          serializer.outputEndElement(innerMatchingNode, aNamespace, aValueTagName, aValueTagName);
        }
      } finally {
        serializer.deleteNodeStore();
        // if we wrote start tag for attribute, now write end tag
        serializer.outputEndElement(matchingNode, aNamespace, aXmlElementName, aXmlElementName);
      }
    }
  }

  /**
   * Initializes this object from its XML DOM representation. This method is typically called from
   * the {@link XMLParser}.
   * 
   * @param aElement
   *          the XML element that represents this object.
   * @param aParser
   *          a reference to the UIMA <code>XMLParser</code>. The
   *          {@link XMLParser#buildObject(Element)} method can be used to construct sub-objects.
   * 
   * @throws InvalidXMLException
   *           if the input XML element does not specify a valid object
   */
  public final void buildFromXMLElement(Element aElement, XMLParser aParser)
          throws InvalidXMLException {
    buildFromXMLElement(aElement, aParser, new XMLParser.ParsingOptions(true));
  }

  /**
   * Initializes this object from its XML DOM representation. This method is typically called from
   * the {@link XMLParser}.
   * 
   * It is overridden by specific Java impl classes to provide additional
   * defaulting (e.g. see AnalysisEngineDescription_impl)
   * 
   * @param aElement
   *          the XML element that represents this object.
   * @param aParser
   *          a reference to the UIMA <code>XMLParser</code>. The
   *          {@link XMLParser#buildObject(Element)} method can be used to construct sub-objects.
   * @param aOptions
   *          option settings
   * 
   * @throws InvalidXMLException
   *           if the input XML element does not specify a valid object
   */
  public void buildFromXMLElement(Element aElement, XMLParser aParser,
          XMLParser.ParsingOptions aOptions) throws InvalidXMLException {
    // check element type
    if (!aElement.getTagName().equals(getXmlizationInfo().elementTagName))
      throw new InvalidXMLException(InvalidXMLException.INVALID_ELEMENT_TYPE, new Object[] {
          getXmlizationInfo().elementTagName, aElement.getTagName() });

    if (aOptions.preserveComments) {
      infoset = aElement;
    }

    // get child elements, each of which represents a property
    List<String> foundProperties = new ArrayList<String>();
    NodeList childNodes = aElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node curNode = childNodes.item(i);
      if (curNode instanceof Element) {
        Element curElem = (Element) curNode;
        String elemName = curElem.getTagName();

        // look up this name in this class's XmlizationInfo
        PropertyXmlInfo propXmlInfo = getPropertyXmlInfo(elemName);
        if (propXmlInfo != null) {
          // read the property's value from this element
          readPropertyValueFromXMLElement(propXmlInfo, curElem, aParser, aOptions);
          foundProperties.add(elemName);
        } else {
          // There is no property that matches the XML element.
          // We have a couple of special cases to check before concluding that
          // the XML is invalid.

          // (1) if this class has only one property, we do not require
          // specifying the property name as an XML element.
          if (getXmlizationInfo().propertyInfo.length == 1) {
            readPropertyValueFromXMLElement(getXmlizationInfo().propertyInfo[0], aElement, aParser,
                    aOptions);
          } else {
            // (2) if an object can be constructed from the unknown element,
            // attempt to assign this object to any null-valued property
            // that can accept it.
            readUnknownPropertyValueFromXMLElement(curElem, aParser, aOptions, foundProperties);
          }
        }
      }
    }
  }

  /**
   * Utility method to read an attribute's value from its DOM representation.
   * 
   * @param aPropXmlInfo
   *          information about the property to read
   * @param aElement
   *          DOM element to read from
   * @param aParser
   *          parser to use to construct complex values
   * @param aOptions
   *          option settings
   * @throws InvalidXMLException -         
   */
  protected void readPropertyValueFromXMLElement(PropertyXmlInfo aPropXmlInfo, Element aElement,
          XMLParser aParser, XMLParser.ParsingOptions aOptions) throws InvalidXMLException {
    String propName = aPropXmlInfo.propertyName;
    // get class of the property
    Class propClass = getAttributeClass(propName);
    // if we expect an array, treat that specially
    if (propClass.isArray()) {
      readArrayPropertyValueFromXMLElement(aPropXmlInfo, propClass, aElement, aParser, aOptions);
    } else if (propClass == String.class) // special processing to handle env vars
    {
      String text = XMLUtils.getText(aElement, aOptions.expandEnvVarRefs);
      setAttributeValue(propName, text);
    } else {
      // attempt to get the first child element, which represents the object
      // that is the value of this attribute (there may not be one for a
      // primitive type)
      Element objElem = XMLUtils.getFirstChildElement(aElement);
      if (objElem != null) {
        // is it an array? (This can happen if aAttrClass is generic Object)
        if (objElem.getTagName().equals("array")) {
          readArrayPropertyValueFromXMLElement(aPropXmlInfo, Object.class, objElem, aParser,
                  aOptions);
        } else {
          setAttributeValue(propName, aParser.buildObjectOrPrimitive(objElem, aOptions));
        }
      } else {
        // get text of the element and check for null
        String text = XMLUtils.getText(aElement, aOptions.expandEnvVarRefs);
        if (!text.equals("")) {
          try {
            // not null - attempt to construct an appropriate object from this String
            // class must have a constructor that takes a String parameter
            Constructor constructor = propClass.getConstructor(new Class[] { String.class });
            // construct the object
            Object val = constructor.newInstance(new Object[] { text });
            setAttributeValue(propName, val);
          } catch (Exception e) {
            throw new InvalidXMLException(InvalidXMLException.UNKNOWN_ELEMENT,
                    new Object[] { aElement.getTagName() }, e);
          }
        }
      }
    }
  }

  /**
   * Utility method to read an array property's value from its DOM representation.
   * 
   * @param aPropXmlInfo
   *          information about the property to read
   * @param aPropClass
   *          class of the property's value
   * @param aElement
   *          DOM element representing the entire array
   * @param aParser
   *          parser to use to construct complex values
   * @param aOptions
   *          option settings
   * @throws InvalidXMLException -         
   */
  protected void readArrayPropertyValueFromXMLElement(PropertyXmlInfo aPropXmlInfo,
          Class aPropClass, Element aElement, XMLParser aParser, XMLParser.ParsingOptions aOptions)
          throws InvalidXMLException {
    Constructor primitiveElementStringConstructor = null; // may be used to build objects from
    // strings

    // get all child nodes (note not all may be elements)
    NodeList elems = aElement.getChildNodes();
    int numChildren = elems.getLength();

    // iterate through children, and for each element construct a value,
    // adding it to a list
    List valueList = new ArrayList();
    for (int i = 0; i < numChildren; i++) {
      Node curNode = elems.item(i);
      if (curNode instanceof Element) {
        Element curElem = (Element) curNode;

        // does the PropertyXmlInfo specify the expected tag name?
        if (aPropXmlInfo.arrayElementTagName != null) {
          if (curElem.getTagName().equals(aPropXmlInfo.arrayElementTagName)) {
            // get text of element
            String elemText = XMLUtils.getText(curElem, aOptions.expandEnvVarRefs);

            // create appropriate primitive value
            try {
              // must have a constructor that takes a String parameter
              if (primitiveElementStringConstructor == null) {
                primitiveElementStringConstructor = aPropClass.getComponentType().getConstructor(
                    new Class[] { String.class });
              }
              // construct the object and add to list
              valueList.add(primitiveElementStringConstructor
                      .newInstance(new Object[] { elemText }));
            } catch (Exception e) {
              throw new InvalidXMLException(e);
            }
          } else
            // element type does not match
            throw new InvalidXMLException(InvalidXMLException.INVALID_ELEMENT_TYPE, new Object[] {
                aPropXmlInfo.arrayElementTagName, curElem.getTagName() });
        } else {
          // array element type is not specified, try defaults
          valueList.add(aParser.buildObjectOrPrimitive(curElem, aOptions));
        }
      }
    }

    // initialize an appropriate array of the same length as the valueList,
    // and copy the values
    Class componentType = Object.class;
    if (!(aPropClass == Object.class)) {
      componentType = aPropClass.getComponentType();
      // verify that objects are of appropriate type
      Iterator i = valueList.iterator();
      while (i.hasNext()) {
        Object curObj = i.next();
        if (!componentType.isAssignableFrom(curObj.getClass())) {
          throw new InvalidXMLException(InvalidXMLException.INVALID_CLASS, new Object[] {
              componentType, curObj.getClass() });
        }
      }
    } else {
      // attribute class is generic object, so we don't know what type of
      // array to create. Try to infer it from the values - if they are all
      // of the same type, use that, else use Object[].
      Iterator i = valueList.iterator();
      while (i.hasNext()) {
        Object curObj = i.next();
        if (componentType == Object.class) {
          componentType = curObj.getClass();
        } else if (componentType != curObj.getClass()) {
          componentType = Object.class;
          break;
        }
      }
    }

    Object array = Array.newInstance(componentType, valueList.size());
    valueList.toArray((Object[]) array);

    // assign this array as the value of the attribute
    setAttributeValue(aPropXmlInfo.propertyName, array);
  }

  /**
   * Utility method that attempts to read a property value from an XML element even though it is not
   * known to which property the value should be assigned. If an object can be constructed from the
   * XML element, it will be assigned to any unasigned property that can accept it.
   * 
   * @param aElement
   *          DOM element to read from
   * @param aParser
   *          parser to use to construct complex values
   * @param aOptions -
   * @param aKnownPropertyNames
   *          List of propertiees that we've already values for (these values will not be
   *          overwritten)
   * 
   * @throws InvalidXMLException
   *           if no acceptable object is described by aElement
   */
  protected void readUnknownPropertyValueFromXMLElement(Element aElement, XMLParser aParser,
      XMLParser.ParsingOptions aOptions, List<String> aKnownPropertyNames) throws InvalidXMLException {
    boolean success = false;
    try {
      Object valueObj = aParser.buildObjectOrPrimitive(aElement, aOptions);

      if (valueObj != null) {
        // find a property that can accept this type, which we have not already
        // read a value for
        PropertyXmlInfo[] props = getXmlizationInfo().propertyInfo;
        for (int i = 0; i < props.length; i++) {
          String propName = props[i].propertyName;
          Class<?> propClass = getAttributeClass(propName);
          if (propClass.isAssignableFrom(valueObj.getClass())) {
            // check if we have already read a value for this attribute
            if (!aKnownPropertyNames.contains(propName)) {
              // set value
              setAttributeValue(propName, valueObj);
              success = true;
              break;
            }
          } else if (propClass.isArray()
                  && propClass.getComponentType().isAssignableFrom(valueObj.getClass())) {
            // it's an array - get current value and append
            Object curVal = getAttributeValue(propName);
            int curLen = curVal == null ? 0 : Array.getLength(curVal);
            Object newVal = Array.newInstance(propClass.getComponentType(), curLen + 1);
            if (curLen > 0) {
              System.arraycopy(curVal, 0, newVal, 0, curLen);
            }
            Array.set(newVal, curLen, valueObj);
            // set value
            setAttributeValue(propName, newVal);
            success = true;
            break;
          }
        }
      }
    } catch (Exception e) {
      if (e instanceof InvalidXMLException) {
        throw (InvalidXMLException) e;
      } else {
        throw new InvalidXMLException(e);
      }
    }

    // throw exception if we did not succeed
    if (!success) {
      throw new InvalidXMLException(InvalidXMLException.UNKNOWN_ELEMENT, new Object[] { aElement
              .getTagName() });
    }
  }

  /**
   * Utility method for reading from XML an attribute whose value is a <code>Map</code> with
   * <code>String</code> keys and <code>XMLizable</code> (or an array of these) values.
   * 
   * @param aPropName
   *          name of the property to read from XML
   * @param aElement
   *          element to read from
   * @param aKeyXmlAttribute
   *          XML attribute for the key
   * @param aValueTagName
   *          XML element tag name for each entry in the map
   * @param aParser
   *          parser to use to build sub-objects
   * @param aOptions
   *          parsing option settings
   * @param aValueIsArray
   *          true if the value of the map entires is an array. This method only supports
   *          homogeneous arrays.
   * @throws InvalidXMLException -         
   */
  protected void readMapPropertyFromXml(String aPropName, Element aElement,
          String aKeyXmlAttribute, String aValueTagName, XMLParser aParser,
          XMLParser.ParsingOptions aOptions, boolean aValueIsArray) throws InvalidXMLException {
    // get the Map to which we add entries (it should already exist)
    Map<String, Object> theMap = (Map<String, Object>) getAttributeValue(aPropName);

    // get all child nodes
    NodeList childNodes = aElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node curNode = childNodes.item(i);
      if (curNode instanceof Element) {
        Element curElem = (Element) curNode;
        // check element tag name
        if (!curElem.getTagName().equals(aValueTagName)) {
          throw new InvalidXMLException(InvalidXMLException.INVALID_ELEMENT_TYPE, new Object[] {
              aValueTagName, curElem.getTagName() });
        }
        // get the key attribute
        String key = curElem.getAttribute(aKeyXmlAttribute);
        if (key.equals("")) {
          throw new InvalidXMLException(InvalidXMLException.REQUIRED_ATTRIBUTE_MISSING,
                  new Object[] { "key", aValueTagName });
        }
        // build value object
        Object val = null;
        if (!aValueIsArray) {
          Element valElem = XMLUtils.getFirstChildElement(curElem);
          if (valElem == null) {
            throw new InvalidXMLException(InvalidXMLException.ELEMENT_NOT_FOUND, new Object[] {
                "(any)", aValueTagName });
          }
          val = aParser.buildObject(valElem, aOptions);
        } else // array
        {
          ArrayList<XMLizable> vals = new ArrayList<>();
          NodeList arrayNodes = curElem.getChildNodes();
          for (int j = 0; j < arrayNodes.getLength(); j++) {
            Node curArrayNode = arrayNodes.item(j);
            if (curArrayNode instanceof Element) {
              Element valElem = (Element) curArrayNode;
              vals.add(aParser.buildObject(valElem));
            }
          }
          if (!vals.isEmpty()) {
            val = Array.newInstance(vals.get(0).getClass(), vals.size());
            vals.toArray((Object[]) val);
          }
        }

        // add object to the map
        theMap.put(key, val);
      }
    }
  }

  /**
   * Gets the wrapper class corresponding to the given primitive type. For example,
   * <code>java.lang.Integer</code> is the wrapper class for the primitive type <code>int</code>.
   * 
   * @param aPrimitiveType
   *          <code>Class</code> object representing a primitive type
   * 
   * @return <code>Class</code> object representing the wrapper type for
   *     <code>PrimitiveType</code>.  If <code>aPrimitiveType</code> is not
   *     a primitive type, it is itself returned.
   */
  protected static Class getWrapperClass(Class aPrimitiveType) {
    if (Integer.TYPE.equals(aPrimitiveType))
      return Integer.class;
    else if (Short.TYPE.equals(aPrimitiveType))
      return Short.class;
    else if (Long.TYPE.equals(aPrimitiveType))
      return Long.class;
    else if (Byte.TYPE.equals(aPrimitiveType))
      return Byte.class;
    else if (Character.TYPE.equals(aPrimitiveType))
      return Character.class;
    else if (Float.TYPE.equals(aPrimitiveType))
      return Float.class;
    else if (Double.TYPE.equals(aPrimitiveType))
      return Double.class;
    else if (Boolean.TYPE.equals(aPrimitiveType))
      return Boolean.class;
    else
      return aPrimitiveType;
  }
  

  /**
   * Utility method that introspects this bean and returns a list of <code>PropertyDescriptor</code>s
   * for its properties.
   * <p>
   * The JavaBeans introspector is used, with the IGNORE_ALL_BEANINFO flag. This saves on
   * initialization time by preventing the introspector from searching for nonexistent BeanInfo
   * classes for all the MetaDataObjects.
   * 
   * Caching needed, this method is called for every access to a field, and introspection doesn't cache
   * (from observation... although the javadocs say otherwise (as of Java6 10/2011 - both IBM and Sun)
   * 
   * @return the <code>PropertyDescriptors</code> for all properties introduced by subclasses of
   *         <code>MetaDataObject_impl</code>.
   * 
   * @throws IntrospectionException if introspection fails
   * @deprecated - use getAttributes instead
   */
  @Deprecated
  // never called, we hope. No longer caches anything.
  protected PropertyDescriptor[] getPropertyDescriptors() throws IntrospectionException {
    return Introspector.getBeanInfo(this.getClass(), Introspector.IGNORE_ALL_BEANINFO).getPropertyDescriptors();
  }

  // This next method moved here from XMLUtils, but left there because it's public.
  // The version here handles comments and ignorablewhitespace
  /**
   * Writes a standard XML representation of the specified Object, in the form:<br>
   * <code>&lt;className&gt;string value%lt;/className%gt;</code>
   * <p>
   * where <code>className</code> is the object's java class name without the package and made
   * lowercase, e.g. "string","integer", "boolean" and <code>string value</code> is the result of
   * <code>Object.toString()</code>.
   * <p>
   * This is intended to be used for Java Strings and wrappers for primitive value classes (e.g. Integer, Boolean).
   * 
   * For JSON, the value is output as {  java-class name : value } unless the value is just a string, in which case 
   *   we output just the string
   * 
   * @param aObj
   *          the object to write
   * @param aContentHandler
   *          the SAX ContentHandler to which events will be sent
   * 
   * @throws SAXException
   *           if the ContentHandler throws an exception
   */
  private void writePrimitiveValue(Object aObj)
      throws SAXException {
    // final Attributes EMPTY_ATTRIBUTES = new AttributesImpl();
    final SerialContext sc = serialContext.get();
    final Serializer serializer = sc.serializer;

    String className = aObj.getClass().getName();
    int lastDotIndex = className.lastIndexOf(".");
    if (lastDotIndex > -1)
      className = className.substring(lastDotIndex + 1).toLowerCase();    
    Node node = getMatchingNode(sc, className);
    
    serializer.writeSimpleValueWithTag(className, aObj, node);
  }

  protected Node getMatchingNode(SerialContext serialContext, String name) {
    return (infoset == null) ? null : serialContext.serializer.findMatchingSubElement(name);
  }
  

  /*
   * UIMA-5274 Resolve any ${variable} entries in the string.
   * Returns null if the expansion fails, i.e. a missing variable, or if no settings have been loaded.
   * Logs a warning if settings have been loaded but an entry is missing.
   */
  protected String resolveSettings(String text) {
    UimaContext uimaContext = UimaContextHolder.getContext();
    if (uimaContext != null) {
      Settings_impl settings = (Settings_impl) uimaContext.getExternalOverrides();
      if (settings != null) {
        try {
          return settings.resolve(text);
        } catch (Exception e) {
          UIMAFramework.getLogger(this.getClass()).log(Level.WARNING, e.toString());
        }
      }
    }
    return null;
  }
  
  
//  /*****************************************
//   * JSON support *
//   * 
//   * The main API takes a JsonContentHandlerJacksonWrapper instance
//   * 
//   * The other APIs take combinations of - the output(Writer, File, or OutputStream) - the prettyprint flag
//   * 
//   * @throws IOException
//   * 
//   * ***************************************/
//
//  public void toJSON(Writer aWriter) throws SAXException {
//    toJSON(aWriter, false);
//  }
//
//  public void toJSON(Writer aWriter, boolean isFormattedOutput) throws SAXException {
//    try {
//      JsonGenerator jg = new JsonFactory().createGenerator(aWriter);
//      toJSON(jg, isFormattedOutput);
//    } catch (IOException e) {
//      throw new SAXException(e);
//    }
//
//  }
//
//  public void toJSON(JsonGenerator jg, boolean isFormattedOutput) throws SAXException {
//    JsonContentHandlerJacksonWrapper jch = new JsonContentHandlerJacksonWrapper(jg, isFormattedOutput);
//    jch.withoutNl();
//    toXML(jch);
//    try {
//      jg.flush();
//    } catch (IOException e) {
//      throw new SAXException(e);
//    }
//  }
//
//  /**
//   * Writes out this object's JSON representation.
//   * 
//   * @param aOutputStream
//   *          an OutputStream to which the XML string will be written
//   * @throws SAXException 
//   */
//  public void toJSON(OutputStream aOutputStream) throws SAXException {
//    toJSON(aOutputStream, false);
//  }
//
//  public void toJSON(OutputStream aOutputStream, boolean isFormattedOutput) throws SAXException {
//    try {
//      JsonGenerator jg = new JsonFactory().createGenerator(aOutputStream);
//      toJSON(jg, isFormattedOutput);
//    } catch (IOException e) {
//      throw new SAXException(e);
//    }
//  }
//
//  public void toJSON(File file) throws SAXException {
//    toJSON(file, false);
//  }
//
//  public void toJSON(File file, boolean isFormattedOutput) throws SAXException {
//    try {
//      JsonGenerator jg = new JsonFactory().createGenerator(file, JsonEncoding.UTF8);
//      toJSON(jg, isFormattedOutput);
//      jg.close();
//    } catch (IOException e) {
//      throw new SAXException(e);
//    }
//  }

//  public void toJSON(ContentHandler aCh) throws SAXException {
//    XmlizationInfo inf = getXmlizationInfo();
//    final boolean isJson = aCh instanceof JsonContentHandlerJacksonWrapper;
//    final JsonContentHandlerJacksonWrapper jch = isJson ? (JsonContentHandlerJacksonWrapper) aCh : null;
//    final JsonGenerator jg = isJson ? jch.getJsonGenerator() : null;
//
//
//    // write the element's start tag
//    // get attributes (can be provided by subclasses)
//    AttributesImpl attrs = getXMLAttributes();
//
//    if (valueIsEmpty(inf, attrs)) {
//      return;
//    }
//
//    // start element
//
//    try {
//      outputStartElement(aCh, infoset, inf.namespace, inf.elementTagName, inf.elementTagName, attrs);
//    } catch (SAXException e) {
//      throw new RuntimeException(e);   // should never happen
//    }
//
//    // write child elements
//    try {
//      if (isJson) {
//        if (inf.propertyInfo.length > 1) {
//          jch.writeNlJustBeforeNext();
//        }
//      }
//      for (int i = 0; i < inf.propertyInfo.length; i++) {
//        PropertyXmlInfo propInf = inf.propertyInfo[i];
//        writePropertyAsElement(propInf, inf.namespace, aCh);
//      }
//    } catch (SAXException e) {
//      throw new RuntimeException(e);  // should never happen
//    }
//
//    // end element
//    outputEndElement(jch, infoset, inf.namespace, inf.elementTagName, inf.elementTagName);
//  }

//  private boolean valueIsEmpty(XmlizationInfo inf, AttributesImpl attrs) {
//    for (PropertyXmlInfo propInf : inf.propertyInfo) {
//      Object val = getAttributeValue(propInf.propertyName);
//      if (!valueIsNullOrEmptyArray(val)) {
//        return false;
//      }
//    }
//    for (int i = 0; i < attrs.getLength(); i++) {
//      String val = attrs.getValue(i);
//      if (val != null && (!val.equals(""))) {
//        return false;
//      }
//    }
//    return true;
//  }

  // private String maybeMakeJsonString(Object aObj, boolean isJson) {
  // if (isJson && aObj instanceof String) {
  // return JSONSerializer.makeQuotedString((String) aObj);
  // }
  // return aObj.toString();
  // }

}