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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.UIMA_UnsupportedOperationException;
import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.NameClassPair;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLSerializer;
import org.apache.uima.util.XMLSerializer.CharacterValidatingContentHandler;
import org.apache.uima.util.XMLizable;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
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
 * conventions for getters and setters or else provide a BeanInfo class. See <a
 * href="http://java.sun.com/docs/books/tutorial/javabeans/"> The Java Beans Tutorial</a> for more
 * information.
 * 
 * 
 */
public abstract class MetaDataObject_impl implements MetaDataObject {

  static final long serialVersionUID = 5876728533863334480L;

  private static String PROP_NAME_SOURCE_URL = "sourceUrl";
  private static String PROP_NAME_INFOSET = "infoset";

  private static final Attributes EMPTY_ATTRIBUTES = new AttributesImpl();

  // Class level cache (static) for introspection - 30x speedup in CDE for large descriptor
  private static transient Map<Class<? extends MetaDataObject_impl>, PropertyDescriptor[]> mPropertyDescriptorsMap = 
    Collections.synchronizedMap(new IdentityHashMap<Class<? extends MetaDataObject_impl>, PropertyDescriptor[]>());  

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
   * Returns a list of <code>NameClassPair</code> objects indicating the attributes of this object
   * and the Classes of the attributes' values. For primitive types, the wrapper classes will be
   * returned (e.g. <code>java.lang.Integer</code> instead of int).
   * 
   * Several subclasses override this, to add additional items to the list.
   * 
   * @see org.apache.uima.resource.MetaDataObject#listAttributes()
   */
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

  /**
   * @see org.apache.uima.resource.MetaDataObject#getAttributeValue(String)
   */
  public Object getAttributeValue(String aName) {
    try {
      PropertyDescriptor[] props = getPropertyDescriptors();
      for (int i = 0; i < props.length; i++) {
        if (props[i].getName().equals(aName)) {
          Method reader = props[i].getReadMethod();
          if (reader != null) {
            return reader.invoke(this, new Object[0]);
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
    try {
      // note: listAttributes() is overridden in some subclasses to add additional items
      List<NameClassPair> attrList = listAttributes();
      for (NameClassPair ncp : attrList) {
        if (ncp.getName().equals(aName)) {
          return Class.forName(ncp.getClassName()); 
        }
      }
      return null;
    } catch (Exception e) {
      throw new UIMARuntimeException(e);
    }
  }

  /**
   * Returns whether this object is modifiable. MetaDataObjects are modifiable by default.
   * 
   * @see org.apache.uima.resource.MetaDataObject#isModifiable()
   */
  public boolean isModifiable() {
    return true;
  }

  /**
   * @see org.apache.uima.resource.MetaDataObject#setAttributeValue(String, Object)
   */
  public void setAttributeValue(String aName, Object aValue) {
    try {
      PropertyDescriptor[] props = getPropertyDescriptors();
      for (int i = 0; i < props.length; i++) {
        if (props[i].getName().equals(aName)) {
          Method writer = props[i].getWriteMethod();
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
   * returns "&lt;unknown>". Useful for error messages.
   * 
   * @return the source URL as a string, or "&lt;unknown>"
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
   * @param aUrl
   *          the location of the XML file from which this object was parsed
   */
  public void setSourceUrl(URL aUrl) {
    mSourceUrl = aUrl;

    // set recursively on subobjects
    List<NameClassPair> attrs = listAttributes();
    Iterator<NameClassPair> i = attrs.iterator();
    while (i.hasNext()) {
      String attrName = ((NameClassPair) i.next()).getName();
      Object val = getAttributeValue(attrName);
      if (val instanceof MetaDataObject_impl) {
        ((MetaDataObject_impl) val).setSourceUrl(aUrl);
      } else if (val != null && val.getClass().isArray()) {
        int len = Array.getLength(val);
        for (int j = 0; j < len; j++) {
          Object arrayElem = Array.get(val, j);
          if (arrayElem instanceof MetaDataObject_impl) {
            ((MetaDataObject_impl) arrayElem).setSourceUrl(aUrl);
          }
        }
      } else if (val instanceof Map) {
        Iterator valIter = ((Map) val).values().iterator();
        while (valIter.hasNext()) {
          Object mapVal = valIter.next();
          if (mapVal instanceof MetaDataObject_impl) {
            ((MetaDataObject_impl) mapVal).setSourceUrl(aUrl);
          }
        }
      }
    }
  }

  /**
   * @see org.apache.uima.resource.MetaDataObject#clone()
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
    List<NameClassPair> attrs = listAttributes();
    Iterator<NameClassPair> i = attrs.iterator();
    while (i.hasNext()) {
      String attrName = ((NameClassPair) i.next()).getName();
      Object val = getAttributeValue(attrName);
      if (val instanceof MetaDataObject) {
        Object clonedVal = ((MetaDataObject) val).clone();
        clone.setAttributeValue(attrName, clonedVal);
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
        clone.setAttributeValue(attrName, arrayClone);
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
    List<NameClassPair> attrList = listAttributes();
    Iterator<NameClassPair> i = attrList.iterator();
    while (i.hasNext()) {
      NameClassPair ncp = (NameClassPair) i.next();
      buf.append(ncp.getName() + " = ");
      Object val = getAttributeValue(ncp.getName());
      if (val == null)
        buf.append("NULL");
      else if (val instanceof Object[]) {
        Object[] array = (Object[]) val;
        buf.append("Array{");
        for (int j = 0; j < array.length; j++) {
          buf.append(j).append(": ").append(array[j].toString()).append("\n");
        }
        buf.append("}\n");
      } else
        buf.append(val.toString());
      buf.append("\n");
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
    if (!(aObj instanceof MetaDataObject)) {
      return false;
    }
    MetaDataObject mdo = (MetaDataObject) aObj;
    // get the attributes (and classes) for the two objects
    List<NameClassPair> theseAttrs = this.listAttributes();
    List<NameClassPair> thoseAttrs = mdo.listAttributes();
    // attribute lists must be same length
    if (theseAttrs.size() != thoseAttrs.size()) {
      return false;
    }
    // iterate through all attributes in this object
    Iterator<NameClassPair> i = theseAttrs.iterator();
    while (i.hasNext()) {
      NameClassPair ncp = i.next();
      // other object must contain this attribute
      if (!thoseAttrs.contains(ncp)) {
        return false;
      }
      // get values and test equivalency
      Object val1 = this.getAttributeValue(ncp.getName());
      Object val2 = mdo.getAttributeValue(ncp.getName());
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
   * @return
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
                                     return Arrays.equals((char[])val1,    (char[])val2);
    }
    
    if (val1 instanceof Map) {// only need this to handle Maps w/ array vals 
      if (!(val2 instanceof Map)) {
        return false;
      }
      if (((Map)val1).size() != ((Map)val2).size()) {
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
   * </code>y</code> must be equal if <code>x.equals(y)</code> returns true;
   * 
   * @return the hash code for this object
   */
  public int hashCode() {
    int hashCode = 0;

    // add the hash codes of all attributes
    List<NameClassPair> attrs = listAttributes();
    Iterator<NameClassPair> i = attrs.iterator();
    while (i.hasNext()) {
      String attrName = ((NameClassPair) i.next()).getName();
      Object val = getAttributeValue(attrName);
      if (val != null) {
        if (val instanceof Object[]) {
          Object[] arr = (Object[]) val;
          for (int j = 0; j < arr.length; j++) {
            if (arr[j] != null) {
              hashCode += arr[j].hashCode();
            }
          }
        } else if (val instanceof Map) // only need to do this to handle Maps w/ array vals
        {
          Set entrySet = ((Map) val).entrySet();
          Iterator it = entrySet.iterator();
          while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            hashCode += entry.getKey().hashCode();
            Object subval = entry.getValue();
            if (subval instanceof Object[]) {
              Object[] arr = (Object[]) subval;
              for (int j = 0; j < arr.length; j++) {
                if (arr[j] != null) {
                  hashCode += arr[j].hashCode();
                }
              }
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
   * @see org.apache.uima.util.XMLizable#toXML(java.net.ContentHandler)
   */
  public void toXML(ContentHandler aContentHandler) throws SAXException {
    toXML(aContentHandler, false);
  }

  /**
   * @see org.apache.uima.util.XMLizable#toXML(org.xml.sax.ContentHandler, boolean)
   */
  public void toXML(ContentHandler aContentHandler, boolean aWriteDefaultNamespaceAttribute)
          throws SAXException {
    XmlizationInfo inf = getXmlizationInfo();
    
    // write the element's start tag
    // get attributes (can be provided by subclasses)
    AttributesImpl attrs = getXMLAttributes();
    // add default namespace attr if desired
    if (aWriteDefaultNamespaceAttribute) {
      if (inf.namespace != null) {
        attrs.addAttribute("", "xmlns", "xmlns", null, inf.namespace);
      }
    }

    // start element
    outputStartElement(aContentHandler, infoset, inf.namespace, inf.elementTagName, inf.elementTagName, attrs);    
    CharacterValidatingContentHandler cc = (CharacterValidatingContentHandler) aContentHandler;
    cc.setLastOutputNode(infoset);
    cc.lastOutputNodeAddLevel();
    // write child elements
    try {
      for (int i = 0; i < inf.propertyInfo.length; i++) {
        PropertyXmlInfo propInf = inf.propertyInfo[i];
        writePropertyAsElement(propInf, inf.namespace, aContentHandler);
      }
    } finally {
      cc.lastOutputNodeClearLevel();
    }
     
    // end element
    outputEndElement(aContentHandler, infoset, inf.namespace, inf.elementTagName, inf.elementTagName);
  }

  /**
   * Called by the {@link toXML(Writer,String)} method to get the XML attributes that will be
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

  /**
   * Utility method used to write a property out as an XML element.
   * 
   * @param aPropInfo
   *          information on how to represent the property in XML
   * @param aNamespace
   *          XML namespace URI for this object
   * @param aContentHandler
   *          content handler to which this object will send events that describe its XML
   *          representation
   */
  protected void writePropertyAsElement(PropertyXmlInfo aPropInfo, String aNamespace,
          ContentHandler aContentHandler) throws SAXException {
    // get value of property
    Object val = getAttributeValue(aPropInfo.propertyName);

    // if null or empty array, check to see if we're supposed to omit it
    if (aPropInfo.omitIfNull
            && (val == null || (val.getClass().isArray() && ((Object[]) val).length == 0)))
      return;

    // if XML element name was supplied, write a tag
    Node elementNode = findMatchingSubElement(aContentHandler, aPropInfo.xmlElementName);
    outputStartElement(aContentHandler, elementNode, aNamespace, aPropInfo.xmlElementName, aPropInfo.xmlElementName,
              EMPTY_ATTRIBUTES);
    CharacterValidatingContentHandler cc = (CharacterValidatingContentHandler) aContentHandler;
    cc.lastOutputNodeAddLevel();
    // get class of property
    Class propClass = getAttributeClass(aPropInfo.propertyName);

    try {
    // if value is null then write nothing
    if (val != null) {
      // if value is an array then we have to treat that specially
      if (val.getClass().isArray()) {
        writeArrayPropertyAsElement(aPropInfo.propertyName, propClass, val,
                aPropInfo.arrayElementTagName, aNamespace, aContentHandler);
      } else {
        // if value is an XMLizable object, call its toXML method
        if (val instanceof XMLizable) {
          ((XMLizable) val).toXML(aContentHandler);
        }
        // else, if property's class is java.lang.Object, attempt to write
        // it as a primitive
        else if (propClass == Object.class) {
          XMLUtils.writePrimitiveValue(val, aContentHandler);
        } else {
          // assume attribute's class is known (e.g. String, Integer), so it
          // is not necessary to write the class name to the XML. Just write
          // the string representation of the object
          // XMLUtils.writeNormalizedString(val.toString(), aWriter, true);
          String valStr = val.toString();
          aContentHandler.characters(valStr.toCharArray(), 0, valStr.length());
        }
      }
    }
    } finally {
      cc.lastOutputNodeClearLevel();
    }

    // if XML element name was supplied, end the element that we started
    outputEndElement(aContentHandler, elementNode, aNamespace, aPropInfo.xmlElementName, aPropInfo.xmlElementName);
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
   * @param aContentHandler
   *          the ContentHandler to which this object will write events that describe its XML
   *          representation
   */
  protected void writeArrayPropertyAsElement(String aPropName, Class aPropClass, Object aValue,
          String aArrayElementTagName, String aNamespace, ContentHandler aContentHandler)
          throws SAXException {
    // if aPropClass is generic Object, reader won't know whether to expect
    // an array, so we tell it be writing an "array" element here.
    Node arraySubElement = findMatchingSubElement(aContentHandler, "array");
    if (aPropClass == Object.class) {
      outputStartElement(aContentHandler, arraySubElement, aNamespace, "array", "array", EMPTY_ATTRIBUTES);
    }

    // iterate through elements of the array (at this point we don't allow
    // nested arrays here
    int len = ((Object[]) aValue).length;
    CharacterValidatingContentHandler cc = (CharacterValidatingContentHandler) aContentHandler;
    cc.lastOutputNodeAddLevel();
    try {
      for (int i = 0; i < len; i++) {
        Object curElem = Array.get(aValue, i);
        Node matchingArrayElement = findMatchingSubElement(aContentHandler, aArrayElementTagName);
        
        // if a particular array element tag has been specified, write it
        outputStartElement(aContentHandler, matchingArrayElement, aNamespace, aArrayElementTagName, aArrayElementTagName,
                  EMPTY_ATTRIBUTES);
  
        // if attribute's value is an XMLizable object, call its toXML method
        if (curElem instanceof XMLizable) {
          ((XMLizable) curElem).toXML(aContentHandler);
        }
        // else, attempt to write it as a primitive
        else {
          if (aArrayElementTagName == null) {
            // need to include the type, e.g. <string>
            XMLUtils.writePrimitiveValue(curElem, aContentHandler);
          } else {
            // don't include the type - just write the value
            String valStr = curElem.toString();
            aContentHandler.characters(valStr.toCharArray(), 0, valStr.length());
          }
        }
  
        // if we started an element, end it
        outputEndElement(aContentHandler, matchingArrayElement, aNamespace, aArrayElementTagName, aArrayElementTagName);
      }
    } finally {
      cc.lastOutputNodeClearLevel();
    }

    // if we started an "Array" element, end it
    if (aPropClass == Object.class) {
      outputEndElement(aContentHandler, arraySubElement, aNamespace, "array", "array");
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
   * @param aKeyXmlAttributeName
   *          name of the XML attribute for the key
   * @param aValueTagName
   *          XML element tag name to use for each entry in the Map
   * @param aOmitIfNull
   *          if true, null or empty map will not be written at all, if false, null or empty map
   *          will be written as an empty element
   * @param aNamespace
   *          namespace for this object
   * @param aContentHandler
   *          ContentHandler to which this object will send events describing its XML representation
   */
  protected void writeMapPropertyToXml(String aPropName, String aXmlElementName,
          String aKeyXmlAttribute, String aValueTagName, boolean aOmitIfNull, String aNamespace,
          ContentHandler aContentHandler) throws SAXException {
    // get map
    @SuppressWarnings("unchecked")
    Map<String, Object> theMap = (Map<String, Object>) getAttributeValue(aPropName);
    Node matchingNode = findMatchingSubElement(aContentHandler, aXmlElementName);
   
    // if map is empty handle appropriately
    if (theMap == null || theMap.isEmpty()) {
      if (!aOmitIfNull && aXmlElementName != null) {
        outputStartElement(aContentHandler, matchingNode, aNamespace, aXmlElementName, aXmlElementName, EMPTY_ATTRIBUTES);        
        outputEndElement(aContentHandler, matchingNode, aNamespace, aXmlElementName, aXmlElementName);
      }
    } else {
      // write start tag for attribute if desired
      outputStartElement(aContentHandler, matchingNode, aNamespace, aXmlElementName, aXmlElementName, EMPTY_ATTRIBUTES);        

      CharacterValidatingContentHandler cc = (CharacterValidatingContentHandler) aContentHandler;
      cc.lastOutputNodeAddLevel();
      try {
        // iterate over entries in the Map
        for (Map.Entry<String, Object> curEntry : theMap.entrySet()) {
          String key = curEntry.getKey();
  
          // write a tag for the value, with a "key" attribute
          AttributesImpl attrs = new AttributesImpl();
          attrs.addAttribute("", aKeyXmlAttribute, aKeyXmlAttribute, null, key); // are these nulls OK?
          Node innerMatchingNode = findMatchingSubElement(aContentHandler, aValueTagName);
          outputStartElement(aContentHandler, innerMatchingNode, aNamespace, aValueTagName, aValueTagName, attrs);      
 
          // write the value (must be XMLizable or an array of XMLizable)
          Object val = curEntry.getValue();
          if (val.getClass().isArray()) {
            Object[] arr = (Object[]) val;
            for (int j = 0; j < arr.length; j++) {
              XMLizable elem = (XMLizable) arr[j];
              elem.toXML(aContentHandler);
            }
          } else {
            cc.lastOutputNodeAddLevel();
            try {
            ((XMLizable) val).toXML(aContentHandler);
            } finally {
              cc.lastOutputNodeClearLevel();              
            }
          }
  
          // write end tag for the value
          outputEndElement(aContentHandler, innerMatchingNode, aNamespace, aValueTagName, aValueTagName);
        }
      } finally {
        cc.lastOutputNodeClearLevel();
      }

      // if we wrote start tag for attribute, now write end tag
      outputEndElement(aContentHandler, matchingNode, aNamespace, aXmlElementName, aXmlElementName);
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
          Class propClass = getAttributeClass(propName);
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
   * <code>String</code> keys and <code>XMLizable</code> values.
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
   */
  protected void readMapPropertyFromXml(String aPropName, Element aElement,
          String aKeyXmlAttribute, String aValueTagName, XMLParser aParser,
          XMLParser.ParsingOptions aOptions, boolean aValueIsArray) throws InvalidXMLException {
    // get the Map to which we add entries (it should already exist)
    Map theMap = (Map) getAttributeValue(aPropName);

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
          ArrayList vals = new ArrayList();
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
   * @return <code>Class</object> object representing the wrapper type for
   *     <code>PrimitiveType</code>.  If <code>aPrimitiveType</code> is not
   *     a primitive type, it is itself returned.
   */
  protected Class getWrapperClass(Class aPrimitiveType) {
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
   * @throw IntrospectionException if introspection fails
   */
  protected PropertyDescriptor[] getPropertyDescriptors() throws IntrospectionException { 
    PropertyDescriptor[] pd = mPropertyDescriptorsMap.get(this.getClass());
    if (null == pd) {
      pd = Introspector.getBeanInfo(this.getClass(), Introspector.IGNORE_ALL_BEANINFO).getPropertyDescriptors();
      mPropertyDescriptorsMap.put(this.getClass(), pd);
    }
    return pd;
  }

  
  /**
   * Heuristics for comment and whitespace processing
   * 
   * Example:
   *    <!-- at top -->
   * <a>   <!-- same line -->
   *   <b/>
   *   <d> <!-- cmt --> <e/> </d>
   *   <c/>  <!-- same line -->
   *   <!-- unusual case, following final one at a level -->
   * </a>  <!-- same line -->
   *   <!-- at bottom -->
   *
   * Each element has 2 calls: 
   *     startElement, endElement
   *   Surround these with:
   *     maybeOutputCommentsBefore
   *     maybeOutputCommentsAfter  
   *   
   * Detect top level (by fact that parent is null), and for top level:
   *   collect all above -> output before startelement  
   *     BUT, note that the sax parser doesn't do callbacks for text (blank lines) before the
   *       start element, so all we can collect are the comment lines.
   *   collect all below -> output after endelement
   *   
   * For normal element node, "start":
   *   --> output before element
   *     collect all prev white space siblings up to the one that contains the first newline 
   *         because the prev white space siblings before and including that one will
   *         have been outputted as part of the previous start or end tag's "after element" processing
   *         
   *       if no nl assume comments go with previous element, and skip here
   *       (stop looking if get null for getPreviousSibling())
   *       (stop looking if get other than comment or ignorable whitespace)
   *         (ignorable whitespace not always distinguishable from text that is whitespace?)
   *         
   *   --> output after element:
   *     if children:    eg:  <start> <!-- cmt --> 
   *       collect all up to and including first nl before first Element child
   *         (stop at first Element node; if no nl, then the source had multiple elements on one line:
   *            associate the comments and whitespace with previous (and output them).
   *          
   *     if no children: - means it's written 
   *          <xxx/> or 
   *          <xxx></xxx>  
   *              Note:  <xxx>   something  </xxx> not possible, because then it would have some text children
   *       output nothing - after comments will be done following endElement call
   *       
   * For normal element node, "end":
   *   --> output before element
   *       collect all after last child Element; skip all up to first nl (assume before that, the comment goes with last child node)
   *       if no nl (e.g.   </lastChild> <!--  cmt -->  </elementBeingEnded> )
   *         assume comments go with previous element, and skip here
   *       (stop looking if get null for getNextSibling())
   *       (stop looking if get Element)
   *       
   *     if no element children - output nothing  
   *   --> output after element    
   *       if this element has no successor sibling elements
   *         collect all up to the null
   *       else  
   *         collect all up to and including first nl from getNextSibling().
   *           (stop at first Element)
   *           
   * For implied element nodes (no Java model object corresponding)
   * We have only the "parent" node, and the element name.  Try to do matching on the element name
   * In this case, we always are working with the children in the Dom infoset; we have a last-outputted reference
   *   Scan from last-outputted, to find element match, and then use that element as the "root".       
   *    
   */
  private static final char[] blanks = new char[80];
  static {Arrays.fill(blanks, ' ');}
  private void outputIndent(int indent, ContentHandler contentHandler) throws SAXException {
    contentHandler.ignorableWhitespace(blanks, 0, Math.min(80, indent));
  }
  
  /**
   * CoIw = Comment or IgnorableWhitespace
   * 
   */
  
  private void maybeOutputCoIwBeforeStart(ContentHandler contentHandler, Node node) throws SAXException {
    int indent = 0;
    if (contentHandler instanceof CharacterValidatingContentHandler) {
      indent = ((CharacterValidatingContentHandler) contentHandler).getIndent();;
    }
   
    if (null == node) {
      if (contentHandler instanceof CharacterValidatingContentHandler) {
        CharacterValidatingContentHandler cvch = (CharacterValidatingContentHandler)contentHandler;
        if (!cvch.prevNL) { 
          outputNL(contentHandler);
          outputIndent(indent, contentHandler);
        }
      }
      return;
    }
    if (node.getParentNode() instanceof Document) {
       
       // Special handling for top node:
       //   The SAX parser doesn't do callbacks for whitespace that come before the top node.
       //   It does do callbacks for comments, though.
          
       //   For this case, we do (one time) insert of "nl" as follows:
       //     1 nl before top element
       //     1 nl before each preceeding comment
       
      outputNL(contentHandler);
      
      for (Node c = node.getParentNode().getFirstChild(); c != node; c = c.getNextSibling()) {
        if (c instanceof Comment) {
          outputCoIw(contentHandler, c);
          outputNL(contentHandler);
        }
      }
      return;
    }
    for (Node p = getFirstPrevCoIw(node); p != node; p = p.getNextSibling()) {
      outputCoIw(contentHandler, p);
    }
  }
 
  private void maybeOutputCoIwAfterStart(ContentHandler contentHandler, Node node) throws SAXException {
    if (contentHandler instanceof CharacterValidatingContentHandler) {
      ((CharacterValidatingContentHandler)contentHandler).nextIndent();
    }
    if (null == node || (!hasElementChildNode(node))) {
      if (contentHandler instanceof CharacterValidatingContentHandler) {
        ((CharacterValidatingContentHandler)contentHandler).prevNL = false;
      }
      return;
    }

    outputCoIwAfterElement(node.getFirstChild(), contentHandler);
  }
  
  /**
   * Output comments and ignorable whitespace after an element.
   * Comments following an element can either be grouped with the preceeding element or with the following one.
   *   e.g.    <element>   <!--comment 1-->
   *             <!--comment 2-->
   *             <!--comment 3-->
   *             <subelement>
   *             
   *   We arbitrarily group comment 1 with the element, and comment 2 and 3 with the subelement.
   *   This is for purposes of when they get processed and put out.
   *   This also affects what happens when new elements are "inserted" by an editor.
   *   
   * This routine outputs only the whitespace and comment on the same line (e.g., 
   * it stops after outputting the ignorable whitespace that contains a nl.)
   * If find text which is not whitespace, don't output anything.
   *   Use case: 
   *      <someElement>  some text
   *         
   * @param startNode - the node corresponding to the start or end element just outputted
   * @param contentHandler
   * @throws DOMException
   * @throws SAXException
   */
  private void outputCoIwAfterElement(Node startNode, ContentHandler contentHandler) throws DOMException, SAXException {
    if (null != startNode) {
      // scan for last node to output
      // we do this first so we output nothing if have non-blank text somewhere
      Node lastNode = null;
      for (Node n = startNode;
                (n != null) && 
                ((n instanceof Comment) || (n instanceof Text));  // keep going as long as have non-Element nodes
                n = n.getNextSibling()) {
        if ((n instanceof Text) && !isWhitespaceText(n)) {
          return;  // catch case <someElement>   some text
        }
        lastNode = n;
      }
      if (null == lastNode) {
        return;
      }
      for (Node n = startNode;; n = n.getNextSibling()) { 
        outputCoIw(contentHandler, n);
        if (hasNewline(n)) {          
          if (contentHandler instanceof CharacterValidatingContentHandler) {
            ((CharacterValidatingContentHandler) contentHandler).prevNL = true;
          }
          return;  // return after outputting up to and including 1st new line 
        }
        if (n == lastNode) {
          return;
        }
      }
    }
    if (contentHandler instanceof CharacterValidatingContentHandler) {
      ((CharacterValidatingContentHandler) contentHandler).prevNL = false;
    }
  }
  
  private void maybeOutputCoIwBeforeEnd(ContentHandler contentHandler, Node node) throws SAXException {
    int indent = 0;
    if (contentHandler instanceof CharacterValidatingContentHandler) {
      indent = ((CharacterValidatingContentHandler) contentHandler).prevIndent();
    }
    
    if (null == node || (!hasElementChildNode(node))) {
      if (null == node && contentHandler instanceof CharacterValidatingContentHandler) {
        CharacterValidatingContentHandler cvch = (CharacterValidatingContentHandler)contentHandler;
        if (cvch.prevWasEndElement) { 
          outputNL(contentHandler);
          outputIndent(indent, contentHandler);
        } 
      }
      return;
    }
    
    Node n = node.getLastChild();
    Node np = null;
    boolean newlineFound = false;
    for (Node p = n; p != null && !(p instanceof Element) && (p.getNodeType() != Node.ATTRIBUTE_NODE); p = p.getPreviousSibling()) {
      if (hasNewline(p)) {
        newlineFound = true;
      }
      np = p;
    }
    if (!newlineFound) {
      return;
    }
    for (Node o = skipUpToFirstAfterNL(np); o != null; o = o.getNextSibling()) {
      outputCoIw(contentHandler, o);
    }
  }
  
  private void maybeOutputCoIwAfterEnd(ContentHandler contentHandler, Node node) throws SAXException {
    if (null == node) {
      return;
    }
    outputCoIwAfterElement(node.getNextSibling(), contentHandler);
  }

  /**
   * Scan from last output node the child nodes, looking for a matching element.
   * Side effect if found - set lastoutput node to the found one.
   * @param contentHandler
   * @param elementName
   * @return null (if no match) or matching node
   */
  protected Node findMatchingSubElement(ContentHandler contentHandler, String elementName) {
    if (null == infoset || null == elementName) {
      return null;
    }
    CharacterValidatingContentHandler c = (CharacterValidatingContentHandler) contentHandler;
    Node lastOutput = c.getLastOutputNode();
    Node n = null;
    
    if (lastOutput == null) {
      lastOutput = c.getLastOutputNodePrevLevel();
      if (lastOutput == null) {
        return null;
      }
      n = lastOutput.getFirstChild();
    } else {
      n = lastOutput.getNextSibling();
    }
    for (; n != null; n = n.getNextSibling()) {
      if ((n instanceof Element) && 
          elementName.equals(((Element)n).getTagName())) {
        c.setLastOutputNode(n);
        return n;
      }
    }
    return null;
  }
  
  /**
   * Scan backwards from argument node, continuing until get something other than
   * comment or ignorable whitespace.
   * Return the first node after a nl 
   *   If no nl found, return original node
   * 
   * NOTE: never called with original == the top node
   * @param r - guaranteed non-null
   * @return
   */
  private Node getFirstPrevCoIw(Node original) {
    boolean newlineFound = false;
    Node p = original; // tracks one behind r
    for (Node r = p.getPreviousSibling(); isCoIw(r); r = r.getPreviousSibling()) {
      if (hasNewline(r)) {
        newlineFound = true;
      }
      p = r;
    }
    if (!newlineFound) {
      return original;
    }
    return skipUpToFirstAfterNL(p);
  }
  
  /**
   * Skip nodes going forwards until find one with a nl, then return the one following
   * @param n must not be null, and there must be a NL in the siblings
   * @return node following the one with a new line 
   */
  private Node skipUpToFirstAfterNL(Node n) {
    while (!hasNewline(n)) {
      n = n.getNextSibling();
    }
    return n.getNextSibling();
  }
  
  private boolean hasNewline(Node n) {
    if (n instanceof Comment) {
      return false;
    }
    CharacterData c = (CharacterData) n;
    return (-1) != c.getData().indexOf('\n');
  }
    
  private boolean hasElementChildNode(Node n) {
    for (Node c = n.getFirstChild(); (c != null); c = c.getNextSibling()) {
      if (c instanceof Element) {
        return true;
      }
    }
    return false;
  }

  private void outputCoIw(ContentHandler contentHandler, Node p) throws DOMException, SAXException {
    if (p instanceof Comment) {
      Comment c = (Comment)p;
      ((LexicalHandler)contentHandler).comment(c.getData().toCharArray(), 0, c.getLength());
    } else {
      String s = p.getTextContent();
      contentHandler.characters(s.toCharArray(), 0, s.length());    
    }
    
  }
  
  /**
   * Dom parsers if not operating in validating mode can't distinguish between
   * ignorable white space and non-ignorable white space.
   * 
   * So we use a heuristic instead - we see if the text is whitespace only, and if so,
   * we consider it to be ignorable white space.
   * 
   * @param n
   * @return
   */
  private boolean isCoIw(Node n) {
    return (n != null) && ((n instanceof Comment) || isWhitespaceText(n));
  }
  
  private boolean isWhitespaceText(Node n) {
    if (!(n instanceof Text)) {
      return false;
    }
    Text t = (Text) n;
    String s = t.getData();
    for (int i = 0; i < s.length(); i++) {
      if (!Character.isWhitespace(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }
  
  protected void outputStartElement(ContentHandler aContentHandler, 
                                  Node node, 
                                  String aNamespace, 
                                  String localname, 
                                  String qname, 
                                  Attributes attributes) throws SAXException {
    if (null == localname) {
      return;
    }
    maybeOutputCoIwBeforeStart(aContentHandler, node);
    aContentHandler.startElement(aNamespace, localname, qname, attributes);
    maybeOutputCoIwAfterStart(aContentHandler, node);
  }
  
  protected void outputEndElement(ContentHandler aContentHandler, 
                                Node node,
                                String aNamespace, 
                                String localname, 
                                String qname) throws SAXException {
    if (null == localname) {
      return;
    }
    maybeOutputCoIwBeforeEnd(aContentHandler, node);
    aContentHandler.endElement(aNamespace, localname, qname);
    maybeOutputCoIwAfterEnd(aContentHandler, node);
  }
  
  private static final char[] nlca = new char[] {'\n'};
  
  private void outputNL(ContentHandler contentHandler) throws SAXException {
    contentHandler.ignorableWhitespace(nlca, 0, 1); 
    if (contentHandler instanceof CharacterValidatingContentHandler) {
      ((CharacterValidatingContentHandler) contentHandler).prevNL = true;
    }
  }

  // skip forward over any attribute nodes
//  private Node skipAttrNodes(Node node) {
//    while (node.getNodeType() == Node.ATTRIBUTE_NODE) {
//      Node next = node.getNextSibling();
//      if (next == null) {
//        return node;  // return last good node if only attribute nodes
//      }
//      node = next;
//    }
//    return node;
//  }
}
