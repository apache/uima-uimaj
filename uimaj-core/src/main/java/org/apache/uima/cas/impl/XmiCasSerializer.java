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
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Marker;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.XmiSerializationSharedData.OotsElementData;
import org.apache.uima.cas.impl.XmiSerializationSharedData.XmiArrayElement;
import org.apache.uima.internal.util.IntStack;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.XmlAttribute;
import org.apache.uima.internal.util.XmlElementName;
import org.apache.uima.internal.util.XmlElementNameAndContents;
import org.apache.uima.internal.util.rb_trees.IntRedBlackTree;
import org.apache.uima.util.JsonContentHandlerJacksonWrapper;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;

/**
 * CAS serializer for XMI and JSON formats.
 * Writes a CAS in an XML Metadata Interchange (XMI) format, or in one of several JSON formats.
 *   
 * Note that some CAS structures, for instance, a List structure which has shared objects, 
 * will not be faithfully serialized by these serialization methods 
 * (because Lists are represented as an XMI formatted sequence or by JSON arrays, and shared 
 * substructure is lost).  If you need a completely faithful serialization, please use one of the
 * binary serialization methods. 
 * 
 * To use, 
 *   - create a serializer instance, 
 *   - optionally) configure the instance, and then 
 *   - call serialize on the instance, optionally passing in additional parameters.
 *   
 * After the 1st 2 steps, the serializer instance may be used for multiple calls (on multiple threads) to
 * the 3rd serialize step, if all calls use the same configuration.
 * 
 * There are "convenience" static serialize methods that do these three steps for common configurations.
 * 
 * Parameters can be configured in the XmiCasSerializer instance (I), and/or as part of the serialize(S) call.
 * 
 * The parameters that can be configured are:
 * <ul>
 *   <li>(S) The CAS to serialize
 *   <li>(I,S) whether to use Json or XMI/XML output
 *   <li>(S) where to put the output - an OutputStream</li>
 *   <li>(I,S) a type system - (default null) if supplied, it is used to "filter" types and features that are serialized.  If provided, only 
 *   those that exist in the passed in type system are included in the serialization</li>
 *   <li>(I,S) a flag for prettyprinting - default false (no prettyprinting)</li>
 *   <li>(I) (for XMI) (optional) If supplied, a map used to generate a "schemaLocation" attribute in the XMI
 *          output. This argument must be a map from namespace URIs to the schema location for
 *          that namespace URI.
 *   <li>(S) (for XMI) (optional) if supplied XmiSerializationSharedData representing FeatureStructures
 *       that were set aside when deserializing, and are to be "merged" back in when serializing
 *   <li>(S) a Marker (default: null) if supplied, where the separation between "new" and previously
 *       exisiting FeatureStructures are in the CAS; causes "delta" serialization, where only the 
 *       new and changed FeatureStructures are serailized.
 * </ul>
 * 
 * For Json serialization, additional configuration from the Jackson implementation can be configured
 * on 2 associated Jackson instances:  
 *   - JsonFactory 
 *   - JsonGenerator
 * using the standard Jackson methods on the associated JsonFactory instance; 
 * see the Jackson JsonFactory javadocs for details.
 * 
 * These 2 Jackson objects are settable/accessible from an instance of this class (XmiCasSerializer).
 * 
 * Once the XmiCasSerializer instance is configured, the serialize method is called
 * to serialized a CAS to an output.
 * 
 * Instances of this class must be used on only one thread while configuration is being done;
 * afterwards, multiple threads may use the configured instance, to call serialize.
 */
public class XmiCasSerializer {
  // Special "type class" codes for list types. The LowLevelCAS.ll_getTypeClass() method
  // returns type classes for primitives and arrays, but not lists (which are just ordinary FS types
  // as far as the CAS is concerned). The serialization treats lists specially, however, and
  // so needs its own type codes for these.
  public static final int TYPE_CLASS_INTLIST = 101;

  public static final int TYPE_CLASS_FLOATLIST = 102;

  public static final int TYPE_CLASS_STRINGLIST = 103;

  public static final int TYPE_CLASS_FSLIST = 104;
  
  private static final char [] URIPFX = new char[] {'h','t','t','p',':','/','/','/'};
  
  private static final char [] URISFX = new char[] {'.','e','c','o','r','e'};

  private static final char [] URI_JSON_NS_SFX = new char[] {'.','n','a','m','e','_','s','p','a','c','e'};

  private static final SerializedString FEATURE_REFS_NAME = new SerializedString("@featureRefs");
  
  private static final SerializedString SUPER_TYPES_NAME = new SerializedString("@superTypes");
  
  private static final String CDATA_TYPE = "CDATA";

  public static final String XMLNS_NS_URI = "http://www.w3.org/2000/xmlns/";

  public static final String XMI_NS_URI = "http://www.omg.org/XMI";

  public static final String XSI_NS_URI = "http://www.w3.org/2001/XMLSchema-instance";

  public static final String XMI_NS_PREFIX = "xmi";

  public static final String XMI_TAG_LOCAL_NAME = "XMI";

  public static final String XMI_TAG_QNAME = "xmi:XMI";
  
  private static final SerializedString JSON_CONTEXT_TAG_LOCAL_NAME = new SerializedString("@context");
  
  private static final SerializedString JSON_CAS_FEATURE_STRUCTURES = new SerializedString("@cas_feature_structures");
  
  private static final SerializedString JSON_CAS_VIEWS = new SerializedString("@cas_views");

  public static final XmlElementName XMI_TAG = new XmlElementName(XMI_NS_URI, XMI_TAG_LOCAL_NAME,
          XMI_TAG_QNAME);
  
  public static final SerializedString ID_ATTR_NAME = new SerializedString("xmi:id");
  
  private static final SerializedString JSON_ID_ATTR_NAME = new SerializedString("@id");
  
  private static final SerializedString JSON_TYPE_ATTR_NAME = new SerializedString("@type");
  
  private static final SerializedString JSON_COLLECTION_ATTR_NAME = new SerializedString("@collection");
  
  private static final SerializedString ZERO = new SerializedString("0");
  
  public static final String XMI_VERSION_LOCAL_NAME = "version";

  public static final String XMI_VERSION_QNAME = "xmi:version";

  public static final String XMI_VERSION_VALUE = "2.0";

  /** Namespace URI to use for UIMA types that have no namespace (the "default pacakge" in Java) */
  public static final String DEFAULT_NAMESPACE_URI = "http:///uima/noNamespace.ecore";
  
  private static int PP_LINE_LENGTH = 120;
  private static int PP_ELEMENTS = 30;  // number of elements to do before nl

  private static String[] EMPTY_STRING_ARRAY = new String[0];
  
  private final static Comparator<TypeImpl> COMPARATOR_SHORT_TYPENAME = new Comparator<TypeImpl>() {
    public int compare(TypeImpl object1, TypeImpl object2) {
      return object1.getShortName().compareTo(object2.getShortName());
    }
  };
  

  /**
   *  This enum describes the kinds of JSON formats used for serializing Feature Structures
   *  
   *    BY_ID_EMBED_TYPE:   
   *      { "123" : { "@type" : "type-name", feat : value, ... } 
   *             where 123 is the "ID", and type-name is the name of the type of the feature structure
   *    BY_TYPE_EMBED_ID:  
   *      { "type-name" : [ { "@id" : 123, feat : value ... }, 
   *                        { "@id" : 456, feat : value ... }
   *                        ...
   *                      ], 
   *        ...
   *      }
   *             all feature structures of a particular type are collected together, and 
   *             partially sorted (annotation subtypes are sorted in the normal begin-end order of the
   *             default annotation index)
   */
  public enum JsonCasFormat {
    BY_ID_EMBED_TYPE,    // outputs each FS as "nnn"  : { "@type" : "foo", features : values ... }
    BY_TYPE_EMBED_ID,    // outputs each FS as "type" : { "@id" : 123, features : values ... }
  }
  
  /**
   * The serialization can optionally include context information in addition to the feature structures.
   * 
   * This context information is specified, per used-type.
   * 
   * It can be further subdivided into 3 parts:
   *   1) what their super types are.  This is needed in case the receiver needs to iterate over
   *      a supertype (and all of its subtypes), e.g. an interator over all "Annotations".
   *   2) which of their features are references to other feature structures.  This is needed
   *      if the receiver wants to construct "graphs" of feature structures, with arbitrary links, back-links, cycles, etc.
   *   3) whether or not to include the map from short type names to their fully qualified equivalents.
   *
   */
  public enum JsonContextFormat {
    omitContext,        includeContext,
    omitSupertypes,     includeSuperTypes,
    omitFeatureRefs,    includeFeatureRefs,
    omitExpandedTypeNames, includeExpandedTypeNames,
  }
  
  private TypeSystemImpl filterTypeSystem;
  
  private MarkerImpl marker;
  
  private ErrorHandler eh = null;

  // UIMA logger, to which we may write warnings
  private Logger logger;

  private Map<String, String> nsUriToSchemaLocationMap = null;

  private boolean isFormattedOutput;
  
  private JsonFactory jsonFactory = null;

  private JsonCasFormat jsonCasFormat;  
  
  private boolean isWithContext = true;
  private boolean isWithSupertypes = true;
  private boolean isWithFeatureRefs = true;
  private boolean isWithExpandedTypeNames = true;
  private boolean isWithViews = true;
  private boolean isOmitDefaultValues = true;

  
  /***********************************************
   *         C O N S T R U C T O R S             *  
   ***********************************************/

  /**
   * Creates a new XmiCasSerializer.
   * 
   * @param ts
   *          An optional typeSystem (or null) to filter the types that will be serialized. If any CAS that is later passed to
   *          the <code>serialize</code> method that contains types and features that are not in
   *          this typesystem, the serialization will not contain instances of those types or values
   *          for those features. So this can be used to filter the results of serialization.
   *          A null value indicates that all types and features  will be serialized.
   */
  public XmiCasSerializer(TypeSystem ts) {
    this(ts, (Map<String, String>) null);
  }

  /**
   * Creates a new XmiCasSerializer.
   * 
   * @param ts
   *          An optional typeSystem (or null) to filter the types that will be serialized. If any CAS that is later passed to
   *          the <code>serialize</code> method that contains types and features that are not in
   *          this typesystem, the serialization will not contain instances of those types or values
   *          for those features. So this can be used to filter the results of serialization.
   * @param nsUriToSchemaLocationMap
   *          Map if supplied, this map is used to generate a "schemaLocation" attribute in the XMI
   *          output. This argument must be a map from namespace URIs to the schema location for
   *          that namespace URI.
   */
  
  public XmiCasSerializer(TypeSystem ts, Map<String, String> nsUriToSchemaLocationMap) {
    this(ts, nsUriToSchemaLocationMap, false);
  }

  /**
   * Creates a new XmiCasSerializer
   * @param ts
   *          An optional typeSystem (or null) to filter the types that will be serialized. If any CAS that is later passed to
   *          the <code>serialize</code> method that contains types and features that are not in
   *          this typesystem, the serialization will not contain instances of those types or values
   *          for those features. So this can be used to filter the results of serialization.
   * @param nsUriToSchemaLocationMap
   *          Map if supplied, this map is used to generate a "schemaLocation" attribute in the XMI
   *          output. This argument must be a map from namespace URIs to the schema location for
   *          that namespace URI.
   * @param isFormattedOutput true makes serialization pretty print
   */
  public XmiCasSerializer(TypeSystem ts, Map<String, String> nsUriToSchemaLocationMap, boolean isFormattedOutput) {
    this.filterTypeSystem = (TypeSystemImpl) ts;
    this.nsUriToSchemaLocationMap = nsUriToSchemaLocationMap;
    this.logger = UIMAFramework.getLogger(XmiCasSerializer.class);
    this.isFormattedOutput = isFormattedOutput;
  }

  /**
   * Creates a new XmiCasSerializer.
   * 
   * @param ts
   *          An optional typeSystem (or null) to filter the types that will be serialized. If any CAS that is later passed to
   *          the <code>serialize</code> method that contains types and features that are not in
   *          this typesystem, the serialization will not contain instances of those types or values
   *          for those features. So this can be used to filter the results of serialization.
   * @param uimaContext
   *          not used
   * @param nsUriToSchemaLocationMap
   *          Map if supplied, this map is used to generate a "schemaLocation" attribute in the XMI
   *          output. This argument must be a map from namespace URIs to the schema location for
   *          that namespace URI.
   * 
   * @deprecated Use {@link #XmiCasSerializer(TypeSystem, Map)} instead. The UimaContext reference
   *             is never used by this implementation.
   */
  @Deprecated
  public XmiCasSerializer(TypeSystem ts, UimaContext uimaContext, Map<String, String> nsUriToSchemaLocationMap) {
    this(ts, nsUriToSchemaLocationMap);
  }

  /**
   * Creates a new XmiCasSerializer.
   * 
   * @param ts
   *          An optional typeSystem (or null) to filter the types that will be serialized. If any CAS that is later passed to
   *          the <code>serialize</code> method that contains types and features that are not in
   *          this typesystem, the serialization will not contain instances of those types or values
   *          for those features. So this can be used to filter the results of serialization.
   * @param uimaContext
   *          not used
   * 
   * @deprecated Use {@link #XmiCasSerializer(TypeSystem)} instead. The UimaContext reference is
   *             never used by this implementation.
   */
  @Deprecated
  public XmiCasSerializer(TypeSystem ts, UimaContext uimaContext) {
    this(ts);
  }

  
  /***************************************************
   *  Static XMI Serializer methods for convenience  *  
   ***************************************************/
  
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
   */
  public static void serialize(CAS aCAS, TypeSystem aTargetTypeSystem, OutputStream aStream, boolean aPrettyPrint, 
          XmiSerializationSharedData aSharedData)
          throws SAXException {
    serialize(aCAS, aTargetTypeSystem, aStream, aPrettyPrint, aSharedData, null);
  }  
  
  /**
   * Serializes a Delta CAS to an XMI stream.  This version of this method allows many options to be configured.
   *     
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
   * @param aMarker
   *          an optional object that is used to filter and serialize a Delta CAS containing only
   *          those FSs and Views created after Marker was set and preexisting FSs and views that were modified.
   *          See the JavaDocs for {@link Marker} for details.
   * @throws SAXException
   *           if a problem occurs during XMI serialization
   */
  public static void serialize(CAS aCAS, TypeSystem aTargetTypeSystem, OutputStream aStream, boolean aPrettyPrint, 
          XmiSerializationSharedData aSharedData, Marker aMarker)
          throws SAXException {
    XmiCasSerializer xmiCasSerializer = new XmiCasSerializer(aTargetTypeSystem);
    XMLSerializer sax2xml = new XMLSerializer(aStream, aPrettyPrint);
    xmiCasSerializer.serialize(aCAS, sax2xml.getContentHandler(), null, aSharedData, aMarker);
  } 
  
  /***************************************************
   *       non-static XMI Serializer methods         * 
   *  To use: first make an instance of this class
   *    and set any configuration info needed
   *  Then call these methods
   *  
   *  The serialize calls are thread-safe
   ***************************************************/
  
  /**
   * Write the CAS data to a SAX content handler.
   * 
   * @param cas
   *          The CAS to be serialized.
   * @param contentHandler
   *          The SAX content handler the data is written to.
   * 
   * @throws SAXException if there was a SAX exception
   */
  public void serialize(CAS cas, ContentHandler contentHandler) throws SAXException {
    this.serialize(cas, contentHandler, (ErrorHandler) null);
  }

  /**
   * Write the CAS data to a SAX content handler.
   * 
   * @param cas
   *          The CAS to be serialized.
   * @param contentHandler
   *          The SAX content handler the data is written to.
   * @param errorHandler the SAX Error Handler to use
   * 
   * @throws SAXException if there was a SAX exception
   */
  public void serialize(CAS cas, ContentHandler contentHandler, ErrorHandler errorHandler)
          throws SAXException {
    serialize(cas, contentHandler, errorHandler, null, null);
  }

  /**
   * Write the CAS data to a SAX content handler.
   * 
   * @param cas
   *          The CAS to be serialized.
   * @param contentHandler
   *          The SAX content handler the data is written to.
   * @param sharedData
   *          data structure used to allow the XmiCasSerializer and XmiCasDeserializer to share
   *          information.
   * @param errorHandler the SAX Error Handler to use
   * 
   * @throws SAXException if there was a SAX exception
   */
  public void serialize(CAS cas, ContentHandler contentHandler, ErrorHandler errorHandler,
          XmiSerializationSharedData sharedData) throws SAXException {
    serialize(cas, contentHandler, errorHandler, sharedData, null);
  }  
  
  /**
   * Write the CAS data to a SAX content handler.
   * 
   * @param cas
   *          The CAS to be serialized.
   * @param contentHandler
   *          The SAX content handler the data is written to.
   * @param errorHandler the SAX Error Handler to use
   * @param sharedData
   *          data structure used to allow the XmiCasSerializer and XmiCasDeserializer to share
   *          information.
   * @param marker
   *        an object used to filter the FSs and Views to determine if these were created after
   *          the mark was set. Used to serialize a Delta CAS consisting of only new FSs and views and
   *          preexisting FSs and Views that have been modified.
   *          
   * @throws SAXException if there was a SAX exception
   */
  public void serialize(CAS cas, ContentHandler contentHandler, ErrorHandler errorHandler,
          XmiSerializationSharedData sharedData, Marker marker) throws SAXException {
    
    contentHandler.startDocument();
    CasDocSerializer ser = new CasDocSerializer(contentHandler, errorHandler, ((CASImpl) cas)
            .getBaseCAS(), sharedData, (MarkerImpl) marker);
    ser.serialize();  
    contentHandler.endDocument();
  }
  
  // this method just for testing - uses existing content handler and casdocserializer instance
  // package private for test case access
  void serialize(CAS cas, ContentHandler contentHandler, CasDocSerializer ser) throws SAXException {
    contentHandler.startDocument();
    ser.serialize();
    contentHandler.endDocument();
  }
  
  /**************************************************
   *                  J S O N                       *
   **************************************************/
  
  /****************************************************
   *  Static JSON Serializer methods for convenience  *
   *                                                  *
   *    Note: these are named jsonSerialize           *
   *          The non-static methods                  *
   *                are named serializeJson           *
   ****************************************************/
  
  /**
   * Serializes a CAS using JSON
   * 
   * @param aCAS
   *          CAS to serialize.
   * @param output
   *          a File, OutputStream or Writer to which to write the XMI document
   * 
   * @throws SAXException if there was an IOException (which is wrapped as a SAXException)
   */
  public static void jsonSerialize(CAS aCAS, Object output) throws SAXException {  
    jsonSerialize(aCAS, null, output, false, null);
  }

  /**
   * Serializes a CAS to an output (File, OutputStream, XMI stream, or Writer). 
   * The supplied typesystem filters the output
   * 
   * @param aCAS
   *          CAS to serialize.
   * @param aTargetTypeSystem
   *          type system used for filtering what gets serialized. Any types or features not in the
   *          target type system will not be serialized.  A null value indicates no filtering, that is, 
   *          that all types and features will be serialized.
   * @param output 
   *          output (File, OutputStream, or Writer) to which to write the JSON document
   * 
   * @throws SAXException if there was an IOException (which is wrapped as a SAXException)
   */
  public static void jsonSerialize(CAS aCAS, TypeSystem aTargetTypeSystem, Object output)
          throws SAXException {
    jsonSerialize(aCAS, aTargetTypeSystem, output, false, null);
  }

  /**
   * Serializes a Delta CAS to an output (File, Writer, or OutputStream).  
   * This version of this method allows many options to be configured.
   *    
   *    
   * @param aCAS
   *          CAS to serialize.
   * @param aTargetTypeSystem
   *          type system to which the produced XMI will conform. Any types or features not in the
   *          target type system will not be serialized.  A null value indicates that all types and features
   *          will be serialized.
   * @param output
   *          File, Writer, or OutputStream to which to write the JSON document
   * @param aPrettyPrint
   *          if true the JSON output will be formatted with newlines and indenting.  If false it will be unformatted.
   * @param aSharedData
   *          an optional container for data that is shared between the {@link XmiCasSerializer} and the {@link XmiCasDeserializer}.
   *          See the JavaDocs for {@link XmiSerializationSharedData} for details.
   * @param aMarker
   *        an optional object used to determine which FeatureStructures and modifications were created after
   *          the mark was set. Used to serialize a Delta CAS consisting of only new FSs and views and
   *          preexisting FSs and Views that have been modified.  If null, full serialization is done.        
   *          See the JavaDocs for {@link Marker} for details.
   * @throws SAXException if there was an IOException (which is wrapped as a SAXException)
   */
  public static void jsonSerialize(CAS aCAS, TypeSystem aTargetTypeSystem, Object output, boolean aPrettyPrint, Marker aMarker)
          throws SAXException {
    XmiCasSerializer casSerializer = new XmiCasSerializer(aTargetTypeSystem, null, aPrettyPrint);
    casSerializer.setDeltaCas(aMarker);  // set or reset the delta marker flag
    casSerializer.serializeJson(aCAS, output);
  } 
  
  /**
   * Write the CAS data to a SAX content handler.
   * 
   * @param cas
   *          The CAS to be serialized.
   * @param contentHandler
   *          The SAX content handler the data is written to, an instance of JsonContentHandlerWrappingJackson
   * @param errorHandler the Error Handler to use, or null
   * @param marker
   *        an optional object used to determine which FeatureStructures and modifications were created after
   *          the mark was set. Used to serialize a Delta CAS consisting of only new FSs and views and
   *          preexisting FSs and Views that have been modified.  If null, full serialization is done.        
   *          See the JavaDocs for {@link Marker} for details.
   * @throws SAXException if there was an IOException (which is wrapped as a SAXException)
   */
  public void jsonSerialize(CAS cas, ContentHandler contentHandler, ErrorHandler errorHandler, Marker marker) 
      throws SAXException {
    CasDocSerializer ser = new CasDocSerializer(contentHandler, errorHandler, ((CASImpl) cas)
            .getBaseCAS(), null, (MarkerImpl) marker);
    ser.serialize();
  }
    
  /*************************************************************************************
   * Multi-step api
   * 
   *   1) Create an instance of this class and use for configuration, specifying or defaulting
   *          type system to use for filtering (default - no filtering)
   *          prettyprinting (default - false)
   *             
   *       1b) Do any additional wanted configuration on the instance of this class
   *          instance.prettyPrint(true/false);
   *          instance.useJsonFactory(factory)
   *          instance.filterTypes(typeSystem)
   *          instance.errorHandler(errorHandler)
   *          instance.jsonFormat(JsonCasFormat)  - default is BY_ID_EMBED_TYPE
   *          
   *          instance.getGenerator() to further configure the generator if the defaults are not what is wanted.
   *                    
   *   2) call its serializeJson method, passing in the CAS, and an output (Writer/Outputstream/File)
   *               
   *************************************************************************************/
  
  /**
   * Serialize a Cas to an Output, using configurations set on this instance.
   *   Constructs a JsonContentHandlerJacksonWrapper, using configured JsonFactory and prettyprint settings if any
   * @param cas - the CAS to serialize
   * @param output - where the output goes, an OutputStream, Writer, or File
   * @throws SAXException if there was an IOException (which is wrapped as a SAXException)
   */
  public void serializeJson(CAS cas, Object output) throws SAXException {
    JsonContentHandlerJacksonWrapper jch = 
        new JsonContentHandlerJacksonWrapper(jsonFactory, output, isFormattedOutput);
    serializeJson(cas, jch);
  }
  
  /**
   * Serialize a Cas to an Output configured in the passed in JsonContentHandlerJacksonWrapper
   *   Constructs a new CasDocSerializer instance to do the serialization, 
   *      configured using this class's Delta marker setting (if any)
   * @param cas The CAS to serialize
   * @param jch the configured content handler
   * @throws SAXException if there was an IOException (which is wrapped as a SAXException)
   */
  public void serializeJson(CAS cas, JsonContentHandlerJacksonWrapper jch) throws SAXException {
      CasDocSerializer cds = new CasDocSerializer(jch, eh, ((CASImpl) cas).getBaseCAS(), null, marker);
      cds.needNameSpaces = false;  
      cds.serialize();
  }
  
  /********************************************************
   *   Routines to set/reset configuration                *
   ********************************************************/
  /**
   * set or reset the pretty print flag (default is false)
   * @param pp true to do pretty printing of output
   * @return the original instance, possibly updated
   */
  public XmiCasSerializer setPrettyPrint(boolean pp) {
    this.isFormattedOutput = pp;
    return this;
  }
  
  /**
   * set which JsonFactory instance to use; if null, a new instance is used
   *   this can be used to preconfigure the JsonFactory instance
   * @param jsonFactory -
   * @return the original instance, possibly updated
   */
  public XmiCasSerializer setJsonFactory(JsonFactory jsonFactory) {
    this.jsonFactory = jsonFactory;
    return this;
  }
  
  /**
   * pass in a type system to use for filtering what gets serialized;
   * only those types and features which are defined this type system are included.
   * @param ts the filter
   * @return the original instance, possibly updated
   */
  public XmiCasSerializer setFilterTypes(TypeSystemImpl ts) {
    this.filterTypeSystem = ts;
    return this;
  }
  
  /**
   * set the Marker to specify delta cas serialization
   * @param m - the marker
   * @return the original instance, possibly updated
   */
  public XmiCasSerializer setDeltaCas(Marker m) {
    this.marker = (MarkerImpl) m;
    return this;
  }
  
  /**
   * set an error handler to receive information about errors
   * @param eh the error handler
   * @return the original instance, possibly updated
   */
  public XmiCasSerializer setErrorHandler(ErrorHandler eh) {
    this.eh = eh;
    return this;
  }
  
  /**
   * sets the style of Json formatting desired.
   * @param format specifies the style
   * @return the original instance, possibly updated
   */
  public XmiCasSerializer setJsonCasFormat(JsonCasFormat format) {
    jsonCasFormat = format;
    return this;
  }
  
  /**
   * sets which Json context format to use when serializing
   * @param format the format to use for the serialization
   *   Specifying the context flag also specifies all 3 subflags
   *   Specifying one of the subflags as true sets the context flag to true if it isn't already
   * @return the original instance, possibly updated
   */
  public XmiCasSerializer setJsonContext(JsonContextFormat format) {
    switch (format) {
    case omitContext: 
      isWithContext = false;            
      isWithSupertypes = false;                                                  
      isWithFeatureRefs = false;
      isWithExpandedTypeNames = false; break;
    case includeContext: 
      isWithContext = true;
      isWithSupertypes = true;
      isWithFeatureRefs = true;
      isWithExpandedTypeNames = true; break;
                                                        
    case omitSupertypes: 
      isWithSupertypes = false; break;
    case includeSuperTypes: 
      isWithSupertypes = true; 
      isWithContext = true; break;
      
    
    case omitFeatureRefs: 
      isWithFeatureRefs = false; break;
    case includeFeatureRefs: 
      isWithFeatureRefs = true;
      isWithContext = true; break;
      
    case omitExpandedTypeNames:
      isWithExpandedTypeNames = false; break;
    case includeExpandedTypeNames: 
      isWithExpandedTypeNames = true; 
      isWithContext = true; break;                             
    }
    return this;
  }
  
  /**
   * Causes the index information to be included in the serialization
   * @return the original instance, possibly updated
   */
  public XmiCasSerializer setCasViews() {
    return setCasViews(true);
  }
  
  /**
   * Sets whether or not to include which Feature Structures were indexed, by view
   * @param includeViews true to include the index information
   * @return the original instance, possibly updated
   */
  public XmiCasSerializer setCasViews(boolean includeViews) {
    isWithViews = includeViews;
    return this;
  }
  
  public XmiCasSerializer setOmitDefaultValues(boolean omitDefaultValues) {
    isOmitDefaultValues = omitDefaultValues;
    return this;
  }
  
  /**
   * Use an inner class to hold the data for serializing a CAS. Each call to serialize() creates its
   * own instance.
   * 
   * package private to allow a test case to access
   * 
   */
  class CasDocSerializer {

    // Where the output goes.
    private final ContentHandler ch;

    // The CAS we're serializing.
    private final  CASImpl cas;
    
    private final TypeSystemImpl tsi;

    // Any FS reference we've touched goes in here.
    private final IntRedBlackTree visited;

    // All FSs that are in an index somewhere.
    private final IntVector indexedFSs;

    // The current queue for FSs to write out.
    private final IntStack queue;

    // SofaFS type
    // private int sofaTypeCode;

    // Annotation type
    // private int annotationTypeCode;

    private final AttributesImpl emptyAttrs = new AttributesImpl();

    private final AttributesImpl workAttrs = new AttributesImpl();


    // For debug statistics.
//    private int fsCount = 0;

    // utilities for dealing with CAS list types
    private final ListUtils listUtils;

    // holds the addresses of Array and List FSs that we have encountered
    private final IntRedBlackTree arrayAndListFSs;

    private final XmiSerializationSharedData sharedData;

    private XmlElementName[] xmiTypeNames; // array, indexed by type code, giving XMI names for each type
    
    private final BitSet typeUsed;
    
    private boolean needNameSpaces = true; // may be false for JSON only

    private final Map<String, String> nsUriToPrefixMap = new HashMap<String, String>();
       
    // lives just for one serialize call
    private final Map<String, String> uniqueStrings = new HashMap<String, String>();
    
    private final Set<String> nsPrefixesUsed = new HashSet<String>();
    
    /**
     * Used to tell if a FS was created before or after mark.
     */
    private final MarkerImpl marker;

    /**
     * Whether the serializer needs to check for filtered-out types/features. Set to true if type
     * system of CAS does not match type system that was passed to constructor of serializer.
     */
    private final boolean isFiltering;

    /**
     * Whether the serializer needs to serialize only the deltas, that is, new FSs created after
     * mark represented by Marker object and preexisting FSs and Views that have been
     * modified. Set to true if Marker object is not null and CASImpl object of this serialize
     * matches the CASImpl in Marker object.
     */
    private final boolean isDelta;
    
    private final boolean isJson;
    
    private final boolean isOmitDefaultValues;

    private final JsonContentHandlerJacksonWrapper jch;
    
    private final JsonGenerator jg;

    private final SerializedString idAttrName;

    private final Set<Type> jsonRecordedSuperTypes;

    private TypeImpl[] sortedUsedTypes;
    
    // number of children of current element
    private int numChildren;
    /**
     * Gets the number of children of the current element. This is guaranteed to be set correctly at
     * the time when startElement is called. Needed for streaming Vinci serialization.
     * <p>
     * NOTE: this method will not work if there are simultaneously executing calls to
     * XmiCasSerializer.serialize. Use it only with a dedicated XmiCasSerializer instance that is not
     * shared between threads.
     * 
     * Doesn't appear to be used (August 2014) except in test case
     * 
     * @return the number of children of the current element
     */
    public int getNumChildren() {
      return numChildren;
    }

    private final ErrorHandler eh;

    private int lastEncodedTypeCode;
    
    public final Map<String, XmlElementName> usedTypeName2XmlElementName;
    
    private final Map<String, SerializedString> serializedStrings;

    private TypeSystemImpl filterTypeSystem;

    private final boolean isFormattedOutput;

    private final JsonCasFormat jsonCasFormat;
    
    private final boolean isWithContext;

    private final boolean isWithExpandedTypeNames;

    private final boolean isWithFeatureRefs;

    private final boolean isWithSupertypes;

    private final boolean isWithViews;
    
    private final boolean isWithContextOrViews;

    /***********************************************
     *         C O N S T R U C T O R               *  
     ***********************************************/    
    private CasDocSerializer(ContentHandler ch, ErrorHandler eh, CASImpl cas,
            XmiSerializationSharedData sharedData, MarkerImpl marker) {
      super();
      // copy outer class values into final inner ones, to keep the outer thread-safe
      this.filterTypeSystem = XmiCasSerializer.this.filterTypeSystem; 
      this.isFormattedOutput = XmiCasSerializer.this.isFormattedOutput; 
      this.jsonCasFormat = (null == XmiCasSerializer.this.jsonCasFormat) ?
          JsonCasFormat.BY_ID_EMBED_TYPE : XmiCasSerializer.this.jsonCasFormat; 
      this.isOmitDefaultValues = XmiCasSerializer.this.isOmitDefaultValues; 
      boolean tempIsWithContext = XmiCasSerializer.this.isWithContext; 
      this.isWithExpandedTypeNames = XmiCasSerializer.this.isWithExpandedTypeNames; 
      this.isWithFeatureRefs = XmiCasSerializer.this.isWithFeatureRefs; 
      this.isWithSupertypes = XmiCasSerializer.this.isWithSupertypes; 
      this.isWithViews = XmiCasSerializer.this.isWithViews; 

      this.ch = ch;
      this.eh = eh;
      this.cas = cas;
      this.tsi = cas.getTypeSystemImpl();
      this.visited = new IntRedBlackTree();
      this.queue = new IntStack();
      this.indexedFSs = new IntVector();
      // this.sofaTypeCode = tsi.getTypeCode(CAS.TYPE_NAME_SOFA);
      // this.annotationTypeCode = tsi.getTypeCode(CAS.TYPE_NAME_ANNOTATION);
      this.listUtils = new ListUtils(cas, logger, eh);
      this.arrayAndListFSs = new IntRedBlackTree();
      this.sharedData = sharedData;
      this.isFiltering = filterTypeSystem != null && filterTypeSystem != tsi;
      this.marker = marker;
      if (marker != null && !marker.isValid()) {
  	    CASRuntimeException exception = new CASRuntimeException(
  	        CASRuntimeException.INVALID_MARKER, new String[] { "Invalid Marker." });
    	  throw exception;
      }
      isDelta = marker != null;
      usedTypeName2XmlElementName = new HashMap<String, XmlElementName>(tsi.getNumberOfTypes());
      isJson = ch instanceof JsonContentHandlerJacksonWrapper;
      serializedStrings = isJson ? new HashMap<String, SerializedString>() : null;
      jch = isJson ? (JsonContentHandlerJacksonWrapper) ch : null;
      idAttrName = isJson ? JSON_ID_ATTR_NAME : ID_ATTR_NAME;
      typeUsed = new BitSet();
      jsonRecordedSuperTypes = (isJson) ? new HashSet<Type>() : null;
      jg = isJson ? jch.getJsonGenerator() : null;
      isWithContext = (tempIsWithContext && !isWithSupertypes && !isWithFeatureRefs && !isWithExpandedTypeNames) ? 
                        false : tempIsWithContext;
      isWithContextOrViews = isWithContext || isWithViews;
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
     * @return <code>true</code> if we've seen the address before.
     */
    private boolean isVisited(int addr) {
      return visited.containsKey(addr);
    }

    /**
     * Starts serialization
     * @throws SAXException 
     */
    private void serialize() throws SAXException {
      try {
       
        // populate nsUriToPrefixMap and xmiTypeNames structures based on CAS 
        // type system, and out of typesystem data if any
        if (isJson) {
          xmiTypeNames = new XmlElementName[tsi.getLargestTypeCode() + 1];
        } else {
          initTypeAndNamespaceMappings();
        }
        
        int iElementCount = 1; // start at 1 to account for special NULL object
  
        enqueueIncoming(); //make sure we enqueue every FS that was deserialized into this CAS
        enqueueIndexed();
        enqueueNonsharedMultivaluedFS();
        enqueueFeaturesOfIndexed();
        iElementCount += indexedFSs.size();
        iElementCount += queue.size();
  
        FSIndex<FeatureStructure> sofaIndex = cas.getBaseCAS().indexRepository.getIndex(CAS.SOFA_INDEX_NAME);
        if (!isDelta) {
        	iElementCount += (sofaIndex.size()); // one View element per sofa
        	if (this.sharedData != null) {
        	  iElementCount += this.sharedData.getOutOfTypeSystemElements().size();
        	}
        } else {
          int numViews = cas.getBaseSofaCount();
          for (int sofaNum = 1; sofaNum <= numViews; sofaNum++) {
            FSIndexRepositoryImpl loopIR = (FSIndexRepositoryImpl) cas.getBaseCAS().getSofaIndexRepository(sofaNum);
            if (loopIR != null && loopIR.isModified()) {
              iElementCount++;
            }
          }
        }
  
        if (isJson) {
          jch.withoutNl();  // set up prettyprint mode so this class controls it
          if (isWithContextOrViews) {
            jgWriteStartObject();  // container for context, fss, and views
          }
          if (isWithContext) {
            serializeJsonLdContext();
          }
          if (isWithContextOrViews) {
            jch.writeNlJustBeforeNext();       
            jgWriteFieldName(JSON_CAS_FEATURE_STRUCTURES);
          }
          
          jgWriteStartObject();  // either outer object, or object of JSON_CAS_FEATURE_STRUCTURES
          
          if (jsonCasFormat == JsonCasFormat.BY_TYPE_EMBED_ID) { 
            Integer[] allFss = collectAllFeatureStructures();
            Arrays.sort(allFss, sortFssByType);
            encodeAllFss(allFss);
          } else {
            encodeIndexed();
            encodeQueued();
          }
          jgWriteEndObject();
  
        } else {
          workAttrs.clear();
          computeNamespaceDeclarationAttrs(workAttrs);
          workAttrs.addAttribute(XMI_NS_URI, XMI_VERSION_LOCAL_NAME, XMI_VERSION_QNAME, "CDATA",
              XMI_VERSION_VALUE);
          startElement(XMI_TAG, workAttrs, iElementCount);
          writeNullObject(); // encodes 1 element
          encodeIndexed(); // encodes indexedFSs.size() element
          encodeQueued(); // encodes queue.size() elements
          if (!isDelta) {
        	  serializeOutOfTypeSystemElements(); //encodes sharedData.getOutOfTypeSystemElements().size() elements
          }
        }
        if (isWithViews) {
          if (isJson) {
            jch.writeNlJustBeforeNext();
            jgWriteFieldName(JSON_CAS_VIEWS);
            jgWriteStartObject();        
          }
          writeViews(); // encodes cas.sofaCount + 1 elements
          if (isJson) {
            jgWriteEndObject();  // and end of views property
          }
        }
        
        if (isJson) {
          if (isWithContextOrViews) {
            jgWriteEndObject(); // wrapper of @context and cas
          }
          jgFlush();
        } else {
          endElement(XMI_TAG);
          endPrefixMappings();
        }
      } catch (SAXException e) {
        if (isJson) {
          jgFlush(); // make debugging failures easier by allowing a look at what got done so far
        }
        throw e;
      }
    }

    private void writeViews() throws SAXException {
      // Get indexes for each SofaFS in the CAS
      int numViews = cas.getBaseSofaCount();
      String sofaXmiId = null;
      FeatureStructureImpl sofa = null;
      
      for (int sofaNum = 1; sofaNum <= numViews; sofaNum++) {
        FSIndexRepositoryImpl loopIR = (FSIndexRepositoryImpl) cas.getBaseCAS().getSofaIndexRepository(sofaNum);
        if (sofaNum != 1 || cas.isInitialSofaCreated()) { //skip if initial view && no Sofa yet
                                                          // all non-initial-views must have a sofa
          sofa = (FeatureStructureImpl) cas.getView(sofaNum).getSofa();
          sofaXmiId = getXmiId((sofa).getAddress());
        }
        if (loopIR != null) {
          if (!isDelta) {
            int[] fsarray = loopIR.getIndexedFSs();
            writeView(sofaXmiId, fsarray);
          } else { // is Delta Cas
        	  if (sofaNum != 1 && this.marker.isNew(sofa.getAddress())) {
        	    // for views created after mark (initial view never is - it is always created with the CAS)
        	    // write out the view as new
        	    int[] fsarray = loopIR.getIndexedFSs();
              writeView(sofaXmiId, fsarray);
        	  } else if (loopIR.isModified()) {
        	    writeView(sofaXmiId, loopIR.getAddedFSs(), loopIR.getDeletedFSs(), loopIR.getReindexedFSs());
          	}
          } 
        }
      }
    }

    private void writeView(String sofaXmiId, int[] members) throws SAXException {
      workAttrs.clear();
      if (isJson) {
        jch.writeNlJustBeforeNext();
        jgWriteArrayFieldStart((sofaXmiId != null && sofaXmiId.length() > 0) ? sofaXmiId : "0");    
      } else if (sofaXmiId != null && sofaXmiId.length() > 0) {
        addAttribute(workAttrs, "sofa", sofaXmiId);
      }
      StringBuilder membersString = isJson ? null : new StringBuilder();
      boolean isPastFirstElement = writeViewMembers(membersString, members);
      //check for out-of-typesystem members
      if (this.sharedData != null) {
        List<String> ootsMembers = this.sharedData.getOutOfTypeSystemViewMembers(sofaXmiId);
        writeViewMembers(membersString, ootsMembers, isPastFirstElement);
      }
      if (isJson) {
        jgWriteEndArray();
      } else if (membersString.length() > 0) {
        workAttrs.addAttribute(
            "", 
            "members",
            "members",
            isJson ? "array" : CDATA_TYPE, 
            membersString.toString());

//      addAttribute(workAttrs, "members", membersString.substring(0, membersString.length() - 1));
        if (membersString.length() > 0) {
          XmlElementName elemName = uimaTypeName2XmiElementName("uima.cas.View");
          startElement(elemName, workAttrs, 0);
          endElement(elemName);
        }
      }
    }
    

    private boolean writeViewMembers(StringBuilder sb, int[] members) throws SAXException {
      boolean isPastFirstElement = false;
      int nextBreak = isJson ? PP_ELEMENTS : (((sb.length() - 1) / PP_LINE_LENGTH) + 1) * PP_LINE_LENGTH;
      for (int i = 0; i < members.length; i++) {
        if (isJson) {
          int id = getXmiIdAsNumber(members[i]);
          if (id == 0) {
            continue;
          }
          if (i > nextBreak) {
            jch.writeNlJustBeforeNext();
            nextBreak += PP_ELEMENTS;
          }
          jgWriteNumber(id);
        } else {
          String xmiId = getXmiId(members[i]);
          if (xmiId != null) { // to catch filtered FS
           
            if (isPastFirstElement) {
              sb.append(isJson ? (isFormattedOutput ? ", " : ",") : " ");
            } else {
              isPastFirstElement = true;
            }
            sb.append(xmiId);
            if (isJson && isFormattedOutput && (sb.length() > nextBreak)) {
              sb.append(JsonContentHandlerJacksonWrapper.SYSTEM_LINE_FEED);
              nextBreak += PP_LINE_LENGTH; 
            }
          }
        }
      }
      return isPastFirstElement;
    }
    
    /**
     * version for out-of-type-system data being merged back in
     * not currently supported for JSON
     * @param sb - where output goes
     * @param members string representations of the out of type system ids
     * @param isPastFirstElement -
     * @return
     */
    private StringBuilder writeViewMembers(StringBuilder sb, List<String> members, boolean isPastFirstElement) {
      if (members != null) {
        for (String member : members) {
          if (isPastFirstElement) {
            sb.append(isJson ? (isFormattedOutput ? ", " : ",") : " ");
          } else {
            isPastFirstElement = true;
          }
          sb.append(member);
        }
      }
      return sb;
    }
    
    private void writeViewForDeltas(String kind, int[] deltaMembers) throws SAXException {
      if (isJson) {
        jgWriteArrayFieldStart(kind);
      }
      StringBuilder sb = isJson ? null : new StringBuilder();
      writeViewMembers(sb, deltaMembers);   
      if (isJson) {
        jgWriteEndArray();
      } else if (sb.length() > 0) {
        addAttribute(workAttrs, kind, sb.toString());
      }
    }
       
    private void writeView(String sofaXmiId, int[] added, int[] deleted, int[] reindexed) throws SAXException {
        workAttrs.clear();
        if (isJson) {
          jch.writeNlJustBeforeNext();
          if (sofaXmiId != null && sofaXmiId.length() > 0) {
            jgWriteFieldName(sofaXmiId);
          } else {
            jgWriteFieldName(ZERO);
          }
          jgWriteStartObject();
        } else if (sofaXmiId != null && sofaXmiId.length() > 0) {
          addAttribute(workAttrs, "sofa", sofaXmiId);
        }
        writeViewForDeltas("added_members", added);
        writeViewForDeltas("deleted_members", deleted);
        writeViewForDeltas("reindexed_members", reindexed);
        
        if (isJson) {
          jgWriteEndObject();
        } else {      
          XmlElementName elemName = uimaTypeName2XmiElementName("uima.cas.View");
          startElement(elemName, workAttrs, 0);
          endElement(elemName);
        }
      }

    /**
     * Writes a special instance of dummy type uima.cas.NULL, having xmi:id=0. This is needed to
     * represent nulls in multi-valued references, which aren't natively supported in Ecore.
     * @throws SAXException 
     * 
     */
    private void writeNullObject() throws SAXException {
      workAttrs.clear();
      addAttribute(workAttrs, ID_ATTR_NAME.getValue(), "0");
      XmlElementName elemName = uimaTypeName2XmiElementName("uima.cas.NULL");
      startElement(elemName, workAttrs, 0);
      endElement(elemName);
    }
   
    /**
     * Adds the ", _featureRefs" : [ "featName1", "featName2"] 
     * to the string builder for features that are being serialized
     * as references. 
     * 
     *   This includes arrays and lists marked with <multipleReferencesAllowed>
     *   
     * @param type the range type of the Feature 
     * @throws SAXException 
     */
    private void addJsonFeatRefs(TypeImpl type) throws SAXException {
      final int typeCode = type.getCode();
      final int[] feats = tsi.ll_getAppropriateFeatures(typeCode);
      boolean started = false;
      for (int featCode : feats) {
        final int fsClass = classifyType(tsi.range(featCode));
        final boolean isRef = isRefToFS(typeCode, featCode, fsClass);
        if (isRef) {
          if (!started) {
            jch.writeNlJustBeforeNext();
            jgWriteFieldName(FEATURE_REFS_NAME);
            jgWriteStartArray();
            started = true; 
          }
          jgWriteString(getSerializedString(tsi.ll_getFeatureForCode(featCode).getShortName()));          
        }
      }
      if (started) {
        jgWriteEndArray();
      }
    }
      
//    /**
//     * Only call this if needNameSpaces
//     * Serialize two kinds of namespace info
//     *   1)   simpleTypeName  => uri form  (in case no need for namespaces)
//     *   2)   shortname => uri up to last "/" beofre simple type name
//     *          for cases where simple type names need disambiguation
//     *   Only need this for used types  
//     * @param types
//     * @throws SAXException 
//     */
//    private void serializeNameSpaceInfo() throws SAXException {
//      if (isFormattedOutput) {
//      StringPair[] sortedPrefixUri = getSortedPrefixUri();
//      for (StringPair sp : sortedPrefixUri) {
//        jch.writeNlJustBeforeNext();
//        jg.writeFieldName(sp.s1);
//        jg.writeString(sp.s2);
//      }
//      } else {
//        for (Map.Entry<String,String> e : nsUriToPrefixMap.entrySet()) {
//          jch.writeNlJustBeforeNext();
//          jg.writeFieldName(e.getValue());
//          jg.writeString(e.getKey());
//        }
//      }
//    }
    
    /**
     * Add superType chain to required type, up to point already covered
     * @throws SAXException 
     */
    private void addJsonSuperTypes(TypeImpl ti) throws SAXException {
      jch.writeNlJustBeforeNext();
      jgWriteFieldName(SUPER_TYPES_NAME);
      jgWriteStartArray();
      for (TypeImpl parent = (TypeImpl) ti.getSuperType(); 
           parent != null; 
           parent = (TypeImpl) parent.getSuperType()) {
        jgWriteString(parent.getName());
        if (!jsonRecordedSuperTypes.add(parent)) {
          break;  // if this item already output
        }
      }
      jgWriteEndArray();
    }
    
    
    // sort is by shortname of type
    private TypeImpl[] getSortedUsedTypes() {
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
    /**
     * JSON: serialize context info
     * 
     *    (for namespaces - may be omitted if not needed for disambiguation)
     *    "nameSpacePrefix" : IRI-for-namespace
     *    
     *    (for each used type)
     *    "typeName - or nameSpace:typeName" : { 
     *       "@id" : expanded-name-as-IRI,
     *       "@superTypes" : [xxx, yyy, zzz, uima.cas.TOP],
     *       "@featureRefs" : [ "featName1", "featName2"] }
     *   
     *    superType values are longNames
     *    if no featureRefs, omit
     * @throws SAXException 
     */
    
    private void serializeJsonLdContext() throws SAXException {  
      //   @context :  { n : v ... }}
      jgWriteFieldName(JSON_CONTEXT_TAG_LOCAL_NAME);
//      jch.setDoNL();
      jgWriteStartObject();
      
//      if (needNameSpaces) {
//        serializeNameSpaceInfo();
//      }
      
      for (TypeImpl ti : getSortedUsedTypes()) {
        jch.writeNlJustBeforeNext();      
        jgWriteFieldName(getSerializedTypeName(xmiTypeNames[ti.getCode()]));
        jgWriteStartObject();
        jch.writeNlJustBeforeNext();
        if (isWithExpandedTypeNames) {
          jgWriteFieldName(JSON_ID_ATTR_NAME);  // form for using SerializedString
          jgWriteString(ti.getName());
        }
        if (isWithFeatureRefs) {
          addJsonFeatRefs(ti);
        }
        if (isWithSupertypes) {
          addJsonSuperTypes(ti);
        }
        jgWriteEndObject();  // end of one type
      }
      jgWriteEndObject();  // end of context
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
    /*    
     * @param workAttrs2 where to put the attributes and values
     * @throws SAXException
     */
    private void computeNamespaceDeclarationAttrs(AttributesImpl workAttrs2) throws SAXException {
      Iterator<Map.Entry<String, String>> it = nsUriToPrefixMap.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<String, String> entry = it.next();
        
           // key = e.g.  http:///....
           // value = e.g.   "xmi" or last name part (plus int to disambiguate)
        String nsUri = entry.getKey();
        String prefix = entry.getValue();
        
        // write attribute
        workAttrs.addAttribute(XMLNS_NS_URI, 
                               prefix, 
                               "xmlns:" + prefix, 
                               "CDATA", 
                               nsUri);  
        ch.startPrefixMapping(prefix, nsUri);
      }
      // also add schemaLocation if specified
      if (nsUriToSchemaLocationMap != null) {
        // write xmlns:xsi attribute
        workAttrs.addAttribute(XMLNS_NS_URI, "xsi", "xmlns:xsi", "CDATA", XSI_NS_URI);
        ch.startPrefixMapping("xsi", XSI_NS_URI);
        
        // write xsi:schemaLocation attributaiton
        StringBuilder buf = new StringBuilder();
        it = nsUriToSchemaLocationMap.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry<String, String> entry = it.next();
          buf.append(entry.getKey()).append(' ').append(entry.getValue()).append(' ');
        }
        workAttrs.addAttribute(XSI_NS_URI, "xsi", "xsi:schemaLocation", "CDATA", buf.toString());
      }
      
    }
    
    private void endPrefixMappings() throws SAXException {
      Iterator<Map.Entry<String, String>> it = nsUriToPrefixMap.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<String, String> entry = it.next();
        String prefix = entry.getValue();
        ch.endPrefixMapping(prefix);
      }
      if (nsUriToSchemaLocationMap != null) {
        ch.endPrefixMapping("xsi");
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
        if (isDelta && !marker.isModified(fsAddrs[i])) {
    	    continue;
    	}
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
     * When serializing Delta CAS,
     * enqueue encompassing FS of nonshared multivalued FS that have been modified.
     * 
     */
    private void enqueueNonsharedMultivaluedFS() {
	  if (this.sharedData == null || !isDelta)
	      return;
	  int[] fsAddrs = this.sharedData.getNonsharedMulitValuedFSs();
	  for (int i = 0; i < fsAddrs.length; i++) {
	    if (!marker.isModified(fsAddrs[i])) {
	  	  continue; 
	    } else {
	      int encompassingFS = this.sharedData.getEncompassingFS(fsAddrs[i]);
	      if (isVisited(encompassingFS)) {
	        continue;
	      }
	      visited.put(encompassingFS, encompassingFS);
	      indexedFSs.add(encompassingFS);
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

    private int enqueueCommon(int addr) {
      if (isVisited(addr)) {
        return -1;
      }
      if (isDelta) {
        if (!marker.isNew(addr) && !marker.isModified(addr)) {
          return -1;
        }
      }
      
      final int typeCode = cas.getHeapValue(addr);

      if (isFiltering) {
        String typeName = tsi.ll_getTypeForCode(typeCode).getName();
        if (filterTypeSystem.getType(typeName) == null) {
          return -1; // this type is not in the target type system
        }
      }
      boolean alreadySet = typeUsed.get(typeCode);
      if (!alreadySet) {
        typeUsed.set(typeCode);

        String typeName = tsi.ll_getTypeForCode(typeCode).getName();
        XmlElementName newXel = uimaTypeName2XmiElementName(typeName);

        if (!needNameSpaces) {
          XmlElementName xel    = usedTypeName2XmlElementName.get(newXel.localName);
          if (xel != null) {
            if (!xel.nsUri.equals(newXel.nsUri)) {
              needNameSpaces = true;
              usedTypeName2XmlElementName.clear();  // not needed anymore
            }
          } else {
            usedTypeName2XmlElementName.put(newXel.localName, newXel);
          }
        }        
        xmiTypeNames[typeCode] = newXel;
      }
        
      visited.put(addr, addr);
      return typeCode;
    }    
    /*
     * Enqueues an indexed FS. Does NOT enqueue features at this point.
     */
    private void enqueueIndexedFs(int addr) {
      int typeCode = enqueueCommon(addr);
      if (typeCode == -1) {
        return;
      }
      indexedFSs.add(addr);
    }

    /**
     * Enqueue an FS, and everything reachable from it.
     * 
     * @param addr
     *          The FS address.
     */
    private void enqueue(int addr) throws SAXException {
      int typeCode = enqueueCommon(addr);
      if (typeCode == -1) {
        return;  
      }
      queue.push(addr);
      enqueueFeatures(addr, typeCode);
      // Also, for FSArrays enqueue the elements
      if (cas.isFSArrayType(typeCode)) { //TODO: won't get parameterized arrays??
        enqueueFSArrayElements(addr);
      }
    }
    
    /**
     * A feature which is a ref to another FS has two subcases:
     *   a) one where the referenced FS is not a built-in array or list; in this case this method returns true.
     *   b) one where the referenced FS is to a built-in array or list.  
     *       In this case, 
     *        if the feature is marked multi-ref-allowed, then it is serialized as a ref, and this method returns true.
     *        Otherwise, the serialization will be in the form [ x, x, x ]. 
     *          In this case, this method returns true if the "x" range is a fsRef, false otherwise.
     *        
     * @param featCode the feature code
     * @param fsClass the class of the feature
     * @return true if the serialization is being done by having the value of this feature be a reference id
     *              or (in the case of embedded collections, where the collection items are feature references
     */
    private boolean isRefToFS(int typeCode, int featCode, int fsClass) {
      switch (fsClass) {
        case LowLevelCAS.TYPE_CLASS_FS: return true;
        
        case LowLevelCAS.TYPE_CLASS_FSARRAY: 
        case TYPE_CLASS_FSLIST: {
          return true;   // no matter if multi-ref is allowed, to indicate (if m-r-a is false, that the itesm are refs
        }
        case LowLevelCAS.TYPE_CLASS_INTARRAY:
        case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
        case LowLevelCAS.TYPE_CLASS_STRINGARRAY:
        case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY:
        case LowLevelCAS.TYPE_CLASS_BYTEARRAY:
        case LowLevelCAS.TYPE_CLASS_SHORTARRAY:
        case LowLevelCAS.TYPE_CLASS_LONGARRAY:
        case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY:
        case TYPE_CLASS_INTLIST:
        case TYPE_CLASS_FLOATLIST:
        case TYPE_CLASS_STRINGLIST: {
          // we have refs only if the feature has
          // multipleReferencesAllowed = true
          return tsi.ll_getFeatureForCode(featCode).isMultipleReferencesAllowed();
        }
      }
      return false;
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
      int[] feats = tsi.ll_getAppropriateFeatures(typeCode);
      int featAddr, featVal, fsClass;
      for (int i = 0; i < feats.length; i++) {
        if (isFiltering) {
          // skip features that aren't in the target type system
          String fullFeatName = tsi.ll_getFeatureForCode(feats[i]).getName();
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
        fsClass = classifyType(tsi.range(feats[i]));
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
            if (tsi.ll_getFeatureForCode(feats[i]).isMultipleReferencesAllowed()) {
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
            if (tsi.ll_getFeatureForCode(feats[i]).isMultipleReferencesAllowed() || insideListNode) {
              enqueue(featVal);
            } else if (fsClass == TYPE_CLASS_FSLIST) {
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

    /*
     * Encode the indexed FS in the queue.
     */
    private void encodeIndexed() throws SAXException {
      final int max = indexedFSs.size();
      for (int i = 0; i < max; i++) {
        encodeFS(indexedFSs.get(i));
      }
    }

    /*
     * Encode all other enqueued (non-indexed) FSs.
     */
    private void encodeQueued() throws SAXException {
      int addr;
      while (!queue.empty()) {
        addr = queue.pop();
        encodeFS(addr);
      }
    }
    
    private void encodeAllFss(Integer[] allFss) throws SAXException {
      lastEncodedTypeCode = -1;
      for (Integer i : allFss) {
        encodeFS(i);
      }
      if (lastEncodedTypeCode != -1) {
        jgWriteEndArray();
      }
    }

    private Integer[] collectAllFeatureStructures() {
      final int indexedSize = indexedFSs.size();
      int rLen = indexedSize + queue.size();
      Integer[] r = new Integer[rLen];
      int i = 0;
      for (; i < indexedSize; i++) {
        r[i] = indexedFSs.get(i);
      }
      while (!queue.empty()) {
        r[i++] = queue.pop(); 
      }
      return r;
    }
    
    private int compareInts(int i1, int i2) {
      return (i1 == i2) ? 0 :
             (i1 >  i2) ? 1 : -1;
    }
    
    
    private int compareFeat(int o1, int o2, int featCode) {
      final int f1 = cas.ll_getIntValue(o1, tsi.annotSofaFeatCode);
      final int f2 = cas.ll_getIntValue(o2, tsi.annotSofaFeatCode);
      return compareInts(f1, f2);
    }
    
    private final Comparator<Integer> sortFssByType = 
        new Comparator<Integer>() {
          public int compare(Integer o1, Integer o2) {
            final int typeCode1 = cas.getHeapValue(o1);
            final int typeCode2 = cas.getHeapValue(o2);
            int c = compareInts(typeCode1, typeCode2);
            if (c != 0) {
              return c;
            }
            final boolean hasSofa = tsi.subsumes(tsi.annotBaseTypeCode, typeCode1);
            if (hasSofa) {
              c = compareFeat(o1, o2, tsi.annotSofaFeatCode);
              if (c != 0) {
                return c;
              }
              final boolean isAnnot = tsi.subsumes(tsi.annotTypeCode, typeCode1);
              if (isAnnot) {
                c = compareFeat(o1, o2, tsi.startFeatCode);
                return (c != 0) ? c : compareFeat(o2, o1, tsi.endFeatCode);  // reverse order
              }
            }
            // not sofa nor annotation
            return compareInts(o1, o2);  // return in @id order
          }
      };
      
    private SerializedString getSerializedTypeName(XmlElementName xe) {
      return getSerializedString(needNameSpaces ? xe.qName : xe.localName);
    }

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
    private void encodeFS(int addr) throws SAXException {
//      ++fsCount;
      int typeCode = cas.getHeapValue(addr);
      // generate the XMI name for the type (uses a precomputed array so we don't
      // recompute the same name multiple times).
      XmlElementName xmlElementName = xmiTypeNames[typeCode];
      workAttrs.clear();

      // Add ID attribute. We do this for every FS, since otherwise we would
      // have to do a complete traversal of the heap to find out which FSs is
      // actually referenced.
      if (isJson) {
        if (jsonCasFormat == JsonCasFormat.BY_ID_EMBED_TYPE) {
          jch.writeNlJustBeforeNext();
          jgWriteFieldName(Integer.toString(addr));
          jgWriteStartObject();  // start of feat : value
          jgWriteFieldName(JSON_TYPE_ATTR_NAME);
          jgWriteString(getSerializedTypeName(xmlElementName));
        } else { // fs's as arrays under typeName
          if (typeCode != lastEncodedTypeCode) {
            if (lastEncodedTypeCode != -1) {
              // close off previous Array
              jgWriteEndArray();
            }
            lastEncodedTypeCode = typeCode;
            jch.writeNlJustBeforeNext();
            jgWriteFieldName(getSerializedTypeName(xmlElementName));
            jgWriteStartArray();
          }
          jch.writeNlJustBeforeNext();
          jgWriteStartObject();  // start of feat : value
          jgWriteFieldName(JSON_ID_ATTR_NAME);
          jgWriteNumber(addr);
        }
      } else {
        addAttribute(workAttrs, idAttrName.getValue(), getXmiId(addr));
      }

      // Call special code according to the type of the FS (special treatment
      // for arrays and lists).
      final int typeClass = classifyType(typeCode);
      switch (typeClass) {
        case LowLevelCAS.TYPE_CLASS_FS:
        case TYPE_CLASS_INTLIST:
        case TYPE_CLASS_FLOATLIST:
        case TYPE_CLASS_STRINGLIST:
        case TYPE_CLASS_FSLIST: {
          if (isJson) {
            if (typeClass == LowLevelCAS.TYPE_CLASS_FS) {
              jsonWriteFeatures(addr, typeCode);
            } else {   // for Json Lists
              jgWriteFieldName(JSON_COLLECTION_ATTR_NAME);
              writeJsonListValues(addr);
            }
          } else {

            // encode features. this populates the attributes (workAttrs). It also
            // populates the child elements list with features that are to be encoded
            // as child elements (currently required for string arrays).
            List<XmlElementNameAndContents> childElements = encodeFeatures(addr, workAttrs,
                    (typeClass != LowLevelCAS.TYPE_CLASS_FS));
            startElement(xmlElementName, workAttrs, childElements.size());
            sendElementEvents(childElements);
            endElement(xmlElementName);
          }
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
          if (isJson) {
            jgWriteFieldName(JSON_COLLECTION_ATTR_NAME);            
            writeJsonArrayValues(addr, typeClass);
          } else {
            workAttrs.addAttribute("", "", "elements", "CDATA", arrayToString(addr, typeClass));
            startElement(xmlElementName, workAttrs, 0);
            endElement(xmlElementName);
          }
          break;
        }
        case LowLevelCAS.TYPE_CLASS_STRINGARRAY: {
          if (isJson) {
            jgWriteFieldName(JSON_COLLECTION_ATTR_NAME);
            writeJsonArrayValues(addr, typeClass);
          } else {
            // string arrays are encoded as elements, in case they contain whitespace
            List<XmlElementNameAndContents> childElements = new ArrayList<XmlElementNameAndContents>();
            stringArrayToElementList("elements", addr, childElements);
            startElement(xmlElementName, workAttrs, childElements.size());
            sendElementEvents(childElements);
            endElement(xmlElementName);
          }
          break;
        }
        default: {
          throw new SAXException("Error classifying FS type.");
        }
      }
    
      // end of FS encoding
      if (isJson) {
        jgWriteEndObject();
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
        String typeName = tsi.ll_getTypeForCode(cas.getHeapValue(addr)).getName();
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
    
    private int getXmiIdAsNumber(int addr) {
      if (addr == CASImpl.NULL) {
        return 0;
      }
      if (isFiltering) // return as null any references to types not in target TS
      {
        String typeName = tsi.ll_getTypeForCode(cas.getHeapValue(addr)).getName();
        if (filterTypeSystem.getType(typeName) == null) {
          return 0;
        }

      }
      if (this.sharedData == null) {
        // in the absence of outside information, just use the FS address
        return addr;
      } else {
        return Integer.parseInt(this.sharedData.getXmiId(addr));
      }   
    }

    /**
     * Generate startElement, characters, and endElement SAX events.
     * Only for StringArray and StringList kinds of things
     * Only called for XMI (not JSON)
     * 
     * @param elements
     *          a list of XmlElementNameAndContents objects representing the elements to generate
     * @throws SAXException passthru
     */
    private void sendElementEvents(List<? extends XmlElementNameAndContents> elements) throws SAXException {
      Iterator<? extends XmlElementNameAndContents> childIter = elements.iterator();
      final boolean isNotEmpty = childIter.hasNext();
      if (isJson && isNotEmpty) {
        // for non-empty arrays, write [   ....  ]
        jgWriteStartArray();
      }
      while (childIter.hasNext()) {
        XmlElementNameAndContents elem = childIter.next();
        if (isJson) {
          jgWriteString(elem.contents);   // writes null values as JSON null        
        } else {
          if (elem.contents != null) {
            startElement(elem.name, emptyAttrs, 1);
            addText(elem.contents);
          } else {
            startElement(elem.name, emptyAttrs, 0);
          }
          endElement(elem.name);
        }
      }
      
      if (isJson && isNotEmpty) {
        jgWriteEndArray();
      }
    }
    
    private void jsonWriteFeatures(int addr, int typeCode) throws SAXException {
      final int[] feats = tsi.ll_getAppropriateFeatures(typeCode);
      
      for (int i = 0; i < feats.length; i++) {
        final int featCode = feats[i];

        if (isFiltering) {
          // skip features that aren't in the target type system
          String fullFeatName = tsi.ll_getFeatureForCode(featCode).getName();
          if (filterTypeSystem.getFeatureByFullName(fullFeatName) == null) {
            continue;
          }
        }
        
        final String featName = tsi.ll_getFeatureForCode(featCode).getShortName();
        final int featAddr = addr + cas.getFeatureOffset(feats[i]);
        final int featValRaw = cas.getHeapValue(featAddr);
        
        final int fsClass = classifyType(tsi.range(featCode));
        
        switch (fsClass) {
        case LowLevelCAS.TYPE_CLASS_BYTE:
        case LowLevelCAS.TYPE_CLASS_SHORT:  
        case LowLevelCAS.TYPE_CLASS_INT:
        case LowLevelCAS.TYPE_CLASS_LONG:
        case LowLevelCAS.TYPE_CLASS_FLOAT:
        case LowLevelCAS.TYPE_CLASS_DOUBLE:
        case LowLevelCAS.TYPE_CLASS_BOOLEAN:
            
          if (fsClass == LowLevelCAS.TYPE_CLASS_BYTE ||
              fsClass == LowLevelCAS.TYPE_CLASS_SHORT ||
              fsClass == LowLevelCAS.TYPE_CLASS_INT) {

            if (featValRaw == 0 && isOmitDefaultValues) continue;
            jgWriteFieldName(getSerializedString(featName));
            jgWriteNumber(featValRaw);
         
          } else if (fsClass == LowLevelCAS.TYPE_CLASS_LONG) {
            
            final long longVal = cas.ll_getLongValue(featValRaw);
            if (longVal == 0L && isOmitDefaultValues) continue;
            jgWriteFieldName(getSerializedString(featName));
            jgWriteNumber(longVal);
         
          } else if (fsClass == LowLevelCAS.TYPE_CLASS_FLOAT) {
            
            final float floatVal = CASImpl.int2float(featValRaw);
            if (floatVal == 0.F && isOmitDefaultValues) continue;
            jgWriteFieldName(getSerializedString(featName));
            jgWriteNumber(floatVal);
         
          } else if (fsClass == LowLevelCAS.TYPE_CLASS_DOUBLE) {
            
            final double doubleVal = cas.ll_getDoubleValue(addr, featCode);
            if (doubleVal == 0L && isOmitDefaultValues) continue;
            jgWriteFieldName(getSerializedString(featName));
            jgWriteNumber(doubleVal);

          } else {
            jgWriteFieldName(getSerializedString(featName));
            jgWriteBoolean(cas.ll_getBooleanValue(addr, featCode));
          }
          break; 
          
        // all other fields (String, arrays, lists, fsRefs) can be null and are omitted if so  
        default: 
          if (featValRaw != CASImpl.NULL) {
          
            jgWriteFieldName(getSerializedString(featName));
            
            if (fsClass == LowLevelCAS.TYPE_CLASS_INTARRAY ||
                fsClass == LowLevelCAS.TYPE_CLASS_FLOATARRAY ||
                fsClass == LowLevelCAS.TYPE_CLASS_BOOLEANARRAY ||
                fsClass == LowLevelCAS.TYPE_CLASS_BYTEARRAY ||
                fsClass == LowLevelCAS.TYPE_CLASS_SHORTARRAY ||
                fsClass == LowLevelCAS.TYPE_CLASS_LONGARRAY ||
                fsClass == LowLevelCAS.TYPE_CLASS_DOUBLEARRAY ||
                fsClass == LowLevelCAS.TYPE_CLASS_STRINGARRAY ||
                fsClass == LowLevelCAS.TYPE_CLASS_FSARRAY) {
              
              if (tsi.ll_getFeatureForCode(featCode).isMultipleReferencesAllowed()) {
                jgWriteNumber(featValRaw);
              } else {
                writeJsonArrayValues(featValRaw, fsClass);
              }
              
            } else if (fsClass == TYPE_CLASS_INTLIST ||
                       fsClass == TYPE_CLASS_FLOATLIST ||
                       fsClass == TYPE_CLASS_STRINGLIST ||
                       fsClass == TYPE_CLASS_FSLIST) {

              if (tsi.ll_getFeatureForCode(featCode).isMultipleReferencesAllowed()) {
                jgWriteNumber(featValRaw);
              } else {
                writeJsonListValues(featValRaw);
              }
            } else if (fsClass == LowLevelCAS.TYPE_CLASS_STRING) {
              jgWriteString(cas.getStringForCode(featValRaw));
            } else {  // is fsRef
              jgWriteNumber(featValRaw);
            }
          }  // end of default case
          break;
        }  // end of switch
      } // end of loop over all features
    }
    
    /**
     * Encode features of a regular (non-array) FS.
     * Only called for XMI, not JSON
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
     * @throws SAXException passthru
     */
    private List<XmlElementNameAndContents> encodeFeatures(int addr, AttributesImpl attrs, boolean insideListNode)
            throws SAXException {
      List<XmlElementNameAndContents> childElements = new ArrayList<XmlElementNameAndContents>();
      int heapValue = cas.getHeapValue(addr);
      int[] feats = tsi.ll_getAppropriateFeatures(heapValue);
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
          String fullFeatName = tsi.ll_getFeatureForCode(feats[i]).getName();
          if (filterTypeSystem.getFeatureByFullName(fullFeatName) == null) {
            continue;
          }
        }

        featAddr = addr + cas.getFeatureOffset(feats[i]);
        featVal = cas.getHeapValue(featAddr);
        featName = tsi.ll_getFeatureForCode(feats[i]).getShortName();
        fsClass = classifyType(tsi.range(feats[i]));
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
            if (tsi.ll_getFeatureForCode(feats[i]).isMultipleReferencesAllowed()) {
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
            if (tsi.ll_getFeatureForCode(feats[i]).isMultipleReferencesAllowed()) {
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
            if (tsi.ll_getFeatureForCode(feats[i]).isMultipleReferencesAllowed() || insideListNode) {
              attrValue = getXmiId(featVal);
            } else {
              attrValue = listToString(featVal, fsClass);
            }
            break;
          }
            // special case for StringLists, which stored values as child elements rather
            // than attributes.
          case TYPE_CLASS_STRINGLIST: {
            if (tsi.ll_getFeatureForCode(feats[i]).isMultipleReferencesAllowed() || insideListNode) {
              attrValue = getXmiId(featVal);
            } else {
              // it is not safe to use a space-separated attribute, which would
              // break for strings containing spaces. So use child elements instead.
              String[] array = listUtils.anyListToStringArray(featVal, null);
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
          addAttribute(attrs, featName, attrValue, null);
        }
      }
      
      //add out-of-typesystem features, if any
      if (this.sharedData != null) {
        OotsElementData oed = this.sharedData.getOutOfTypeSystemFeatures(addr);
        if (oed != null) {
          //attributes
          Iterator<XmlAttribute> attrIter = oed.attributes.iterator();
          while (attrIter.hasNext()) {
            XmlAttribute attr = attrIter.next();
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
      addAttribute(attrs, attrName, attrValue, CDATA_TYPE);
    }

    // type info for attributes uses string values taken from
    //   http://www.w3.org/TR/xmlschema-2/
    //     decimal string boolean 
    private void addAttribute(AttributesImpl attrs, String attrName, String attrValue, String type) {
      final int index = attrName.lastIndexOf(':') + 1;
      attrs.addAttribute("", attrName.substring(index), attrName, type, attrValue);
    }

        
    private void startElement(XmlElementName name, Attributes attrs, int aNumChildren)
            throws SAXException {
      numChildren = aNumChildren;
      // don't include NS URI here. That causes XMI serializer to
      // include the xmlns attribute in every element. Instead we
      // explicitly added these attributes to the root element.
      ch.startElement(
          ""/* name.nsUri */, 
          name.localName, 
          needNameSpaces ? name.qName : name.localName, 
          attrs);
    }

    private void endElement(XmlElementName name) throws SAXException {
//      if (name == null) {
//        ch.endElement(null, null, null);
//      } else {
        ch.endElement(name.nsUri, name.localName, name.qName);
//      }
    }

    private void stringArrayToElementList(String featName, int addr, List<? super XmlElementNameAndContents> resultList)
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

    // writes a set of values in a JSON array
    // or null if the reference to the UIMA array is actually null
    // 0 length arrays are written as [] 
    private void writeJsonArrayValues(int addr, int arrayType) throws SAXException {
      if (addr == CASImpl.NULL) {
        jgWriteNull();
        return;
      }
      
      final int size = cas.ll_getArraySize(addr);
      if (size > 0 && !arrayAndListFSs.put(addr, addr)) {
        reportWarning("Warning: multiple references to an array.  Reference identity will not be preserved in XMI.");
      }

      jgWriteStartArray();
      int pos = cas.getArrayStartAddress(addr);
      final int endPos = pos + size;
      if (arrayType == LowLevelCAS.TYPE_CLASS_FSARRAY) {
        for (; pos < endPos; pos++) {
          int heapValue = cas.getHeapValue(pos);
          if (heapValue != CASImpl.NULL && isFiltering) { // return as null any references to types not in target TS
            String typeName = tsi.ll_getTypeForCode(cas.getHeapValue(addr)).getName();
            if (filterTypeSystem.getType(typeName) == null) {
              heapValue = CASImpl.NULL;
            }
          }
          if (heapValue == CASImpl.NULL) {
            jgWriteNull();
          } else {
            jgWriteNumber(heapValue);
          }
        }
      } else {
        for (int i = 0; i < size; i++) {
          if (arrayType == LowLevelCAS.TYPE_CLASS_BOOLEANARRAY) {
            jgWriteBoolean(cas.ll_getBooleanArrayValue(addr, i));
          } else if (arrayType == LowLevelCAS.TYPE_CLASS_STRINGARRAY) {
            jgWriteString(cas.ll_getStringArrayValue(addr, i));
          } else if (arrayType == LowLevelCAS.TYPE_CLASS_BYTEARRAY) {
            jgWriteNumber(cas.ll_getByteArrayValue(addr, i));
          } else if (arrayType == LowLevelCAS.TYPE_CLASS_SHORTARRAY) {
            jgWriteNumber(cas.ll_getShortArrayValue(addr, i));
          } else if (arrayType == LowLevelCAS.TYPE_CLASS_INTARRAY) {
            jgWriteNumber(cas.ll_getIntArrayValue(addr, i));
          } else if (arrayType == LowLevelCAS.TYPE_CLASS_LONGARRAY) {
            jgWriteNumber(cas.ll_getLongArrayValue(addr, i));
          } else if (arrayType == LowLevelCAS.TYPE_CLASS_FLOATARRAY) {
            jgWriteNumber(cas.ll_getFloatArrayValue(addr, i));
          } else {
            jgWriteNumber(cas.ll_getDoubleArrayValue(addr, i));
          }
        }
      }
      jgWriteEndArray();
    }
    
    // a null ref is written as null
    // an empty list is written as []
    private void writeJsonListValues(int curNode) throws SAXException {
      if (curNode == CASImpl.NULL) {
        jgWriteNull();
        return;
      }
      final int startNodeType = cas.getHeapValue(curNode);

      int headFeat = listUtils.getHeadFeatCode(startNodeType);
      int tailFeat = listUtils.getTailFeatCode(startNodeType);
      int neListType = listUtils.getNeListType(startNodeType);
      
      IntRedBlackTree visited = new IntRedBlackTree();
      boolean foundCycle = false;
      
      jgWriteStartArray();
      while (curNode != CASImpl.NULL) { 
       
        foundCycle |= !visited.put(curNode, curNode);
        
        final int curNodeType = cas.getHeapValue(curNode);
        if (curNodeType != neListType) {
          break;  // would be the end element.  a 0 is also treated as an end element
        }
        
        final int val = cas.getHeapValue(curNode + cas.getFeatureOffset(headFeat));

        if (curNodeType == listUtils.neStringListType) {
          jgWriteString(cas.getStringForCode(val));
        } else if (curNodeType == listUtils.neFloatListType) {
          jgWriteNumber(CASImpl.int2float(val));
        } else {  // for ints and fsrefs
          jgWriteNumber(val);
        }

        curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(tailFeat));
      }
      jgWriteEndArray();
    
      if (foundCycle) {
        // next will issue error message with the partial array
        listUtils.anyListToStringArray(curNode, null);
      }
    }
    
    // only called for XMI.  For Json, see writeJsonArrayValues
    private String arrayToString(int addr, int arrayType) throws SAXException {
      if (addr == CASImpl.NULL) {
        return null;
      }

      StringBuilder buf = new StringBuilder();
      final int size = cas.ll_getArraySize(addr);
      if (size > 0 && !arrayAndListFSs.put(addr, addr)) {
        reportWarning("Warning: multiple references to an array.  Reference identity will not be preserved in XMI.");
      }
      String elemStr = null;
      if (arrayType == LowLevelCAS.TYPE_CLASS_FSARRAY) {
        int pos = cas.getArrayStartAddress(addr);
        List<XmiArrayElement> ootsArrayElementsList = this.sharedData == null ? null : 
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
                XmiArrayElement arel = ootsArrayElementsList.get(ootsIndex++);
                if (arel.index == j) {
                  elemStr = arel.xmiId;
                  break;
                }                
              }
            }
          }
          if (buf.length() > 0) {
            buf.append(isJson ? (isFormattedOutput ? ", " : ",") : " ");
          }
          buf.append(elemStr);
        }
        return buf.toString();
      } else if (arrayType == LowLevelCAS.TYPE_CLASS_BYTEARRAY && !isJson) {
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
          case LowLevelCAS.TYPE_CLASS_BYTEARRAY:
            fs = new ByteArrayFSImpl(addr, cas);
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
            buf.append(isJson ? (isFormattedOutput ? ", " : ",") : " ");
          }
          if (isJson && arrayType == LowLevelCAS.TYPE_CLASS_STRINGARRAY) {
            buf.append('"').append(fsvalues[i]).append('"');
          } else {
            buf.append(fsvalues[i]);
          }
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
     * @throws SAXException passthru
     */
    private String listToString(int addr, int arrayType) throws SAXException {
      if (addr == CASImpl.NULL) {
        return null;
      }
      StringBuilder buf = new StringBuilder();
      String[] array = EMPTY_STRING_ARRAY;
      array = listUtils.anyListToStringArray(addr, sharedData);
      if (array.length > 0 && !arrayAndListFSs.put(addr, addr)) {
        reportWarning("Warning: multiple references to a ListFS.  Reference identity will not be preserved.");
      }
      for (int j = 0; j < array.length; j++) {
        String v = array[j];
        buf.append(v);
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
     * For XMI only, not JSON
     * Populates nsUriToPrefixMap and xmiTypeNames structures based on CAS type system.
     */
    private void initTypeAndNamespaceMappings() {
      
      xmiTypeNames = new XmlElementName[tsi.getLargestTypeCode() + 1];
      nsUriToPrefixMap.put(XMI_NS_URI, XMI_NS_PREFIX);

      //Add any namespace prefix mappings used by out of type system data.
      //Need to do this before the in-typesystem namespaces so that the prefix
      //used here are reserved and won't be reused for any in-typesystem namespaces.
           
      if (this.sharedData != null) {
        Iterator<OotsElementData> ootsIter = this.sharedData.getOutOfTypeSystemElements().iterator();
        while (ootsIter.hasNext()) {
          OotsElementData oed = ootsIter.next();
          String nsUri = oed.elementName.nsUri;                           //  http://... etc
          String qname = oed.elementName.qName;                           //    xxx:yyy
          String localName = oed.elementName.localName;                   //        yyy
          String prefix = qname.substring(0, qname.indexOf(localName)-1); // xxx
          nsUriToPrefixMap.put(nsUri, prefix);
          nsPrefixesUsed.add(prefix);
        }
      }
      /*
       * Convert x.y.z.TypeName to prefix-uri, TypeName, and ns:TypeName
       */
      Iterator<Type> it = tsi.getTypeIterator();
      while (it.hasNext()) {
        TypeImpl t = (TypeImpl) it.next();
        // this also populates the nsUriToPrefix map
        xmiTypeNames[t.getCode()] = uimaTypeName2XmiElementName(t.getName());
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
      String shortName, nsUri;
      final int lastDotIndex = uimaTypeName.lastIndexOf('.');
      if (lastDotIndex == -1) // no namespace
      {
//        namespace = null;
        shortName = uimaTypeName;
        nsUri = DEFAULT_NAMESPACE_URI;
      } else {
//        namespace = uimaTypeName.substring(0, lastDotIndex);
        shortName = uimaTypeName.substring(lastDotIndex + 1);
        char[] sb = new char[lastDotIndex + (isJson ? (URIPFX.length + URI_JSON_NS_SFX.length): 
                                                      (URIPFX.length + URISFX.length))];
        System.arraycopy(URIPFX, 0, sb, 0, URIPFX.length);  // http:///
        int i = 0;
        for (; i < lastDotIndex; i++) {                     // http:///uima/tcas  or http:///org/a/b/c
          char c = uimaTypeName.charAt(i);
          sb[URIPFX.length + i] = ( c == '.') ? '/' : c;
        }
        if (isJson) {
          System.arraycopy(URI_JSON_NS_SFX, 0, sb, URIPFX.length + i, URI_JSON_NS_SFX.length);  // http:///uima/tcas.ecore
        } else {
          System.arraycopy(URISFX, 0, sb, URIPFX.length + i, URISFX.length);  // http:///uima/tcas.name_space
        }
        nsUri = getUniqueString(new String(sb));
        
//        nsUri = "http:///" + namespace.replace('.', '/') + ".ecore"; 
      }
      // convert short name to shared string, without interning, reduce GCs
      shortName = getUniqueString(shortName);

      // determine what namespace prefix to use
      String prefix = getNameSpacePrefix(uimaTypeName, nsUri, lastDotIndex);

      return new XmlElementName(nsUri, shortName, getUniqueString(prefix + ':' + shortName));
    }

    private String getNameSpacePrefix(String uimaTypeName, String nsUri, int lastDotIndex) {
      // determine what namespace prefix to use
      String prefix = (String) nsUriToPrefixMap.get(nsUri);
      if (prefix == null) {
        if (lastDotIndex != -1) { // have namespace 
          int secondLastDotIndex = uimaTypeName.lastIndexOf('.', lastDotIndex-1);
          prefix = uimaTypeName.substring(secondLastDotIndex + 1, lastDotIndex);
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
      return prefix;
    }
    /*
     *  convert to shared string, without interning, reduce GCs
     */
    private String getUniqueString(String s) { 
      String u = uniqueStrings.get(s);
      if (null == u) {
        u = s;
        uniqueStrings.put(s, s);
      }
      return u;
    }
    
    /**
     * Serializes all of the out-of-typesystem elements that were recorded
     * in the XmiSerializationSharedData during the last deserialization.
     */
    private void serializeOutOfTypeSystemElements() throws SAXException {
      if (this.marker != null)
            return;
      if (this.sharedData == null)
        return;
      Iterator<OotsElementData> it = this.sharedData.getOutOfTypeSystemElements().iterator();
      while (it.hasNext()) {
        OotsElementData oed = it.next();
        workAttrs.clear();
        // Add ID attribute
        addAttribute(workAttrs, idAttrName.getValue(), oed.xmiId);

        // Add other attributes
        Iterator<XmlAttribute> attrIt = oed.attributes.iterator();
        while (attrIt.hasNext()) {
          XmlAttribute attr = attrIt.next();
          addAttribute(workAttrs, attr.name, attr.value);
        }
        
        // serialize element
        startElement(oed.elementName, workAttrs, oed.childElements.size());
        
        //serialize features encoded as child elements
        Iterator<XmlElementNameAndContents> childElemIt = oed.childElements.iterator();
        while (childElemIt.hasNext()) {
          XmlElementNameAndContents child = childElemIt.next();
          workAttrs.clear();
          Iterator<XmlAttribute> attrIter = child.attributes.iterator();
          while (attrIter.hasNext()) {
            XmlAttribute attr =attrIter.next();
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
    
    /*********************************
     *   JSON Jackson generator calls
     *   that convert IOException to
     *   SAXExceptions
     * @throws SAXException wraps IOException
     *********************************/
    
    private void jgWriteStartObject() throws SAXException {
      try {
        jg.writeStartObject();
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }

    private void jgWriteEndObject() throws SAXException {
      try {
        jg.writeEndObject();
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }
    
    private void jgWriteFieldName(SerializedString s) throws SAXException {
      try {
        jg.writeFieldName(s);
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }
    
    private void jgWriteFieldName(int i) throws SAXException {
      try {
        jg.writeFieldName(Integer.toString(i));
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }

    private void jgWriteFieldName(String s) throws SAXException {
      try {
        jg.writeFieldName(s);
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }

    private void jgWriteString(String s) throws SAXException {
      try {
        if (s == null) {
          jg.writeNull();
        } else {
          jg.writeString(s);
        }
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }

    private void jgWriteString(SerializedString s) throws SAXException {
      try {
        jg.writeString(s);
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }
    
    private void jgWriteNumber(int n) throws SAXException {
      try {
        jg.writeNumber(n);
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }
 
    private void jgWriteNumber(long n) throws SAXException {
      try {
        jg.writeNumber(n);
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }

    private void jgWriteNumber(float n) throws SAXException {
      try {
        jg.writeNumber(n);
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }

    private void jgWriteNumber(double n) throws SAXException {
      try {
        jg.writeNumber(n);
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }

    private void jgWriteNumber(byte n) throws SAXException {
      try {
        jg.writeNumber(n);
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }

    private void jgWriteArrayFieldStart(String fieldName) throws SAXException {
      try {
        jg.writeArrayFieldStart(fieldName);
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }
 
    private void jgWriteStartArray() throws SAXException {
      try {
        jg.writeStartArray();
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }

    private void jgWriteEndArray() throws SAXException {
      try {
        jg.writeEndArray();
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }
    
    private void jgWriteNull() throws SAXException {
      try {
        jg.writeNull();
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }
    
    private void jgWriteBoolean(boolean state) throws SAXException {
      try {
        jg.writeBoolean(state);
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }

    private void jgFlush() throws SAXException {
      try {
        jg.flush();
      } catch (IOException e) {
        throw new SAXException(e);
      }
    }

    private SerializedString getSerializedString(String s) {
      SerializedString ss = serializedStrings.get(s);
      if (ss == null) {
        ss = new SerializedString(s);
        serializedStrings.put(s, ss);
      }
      return ss;
    }
    
  }

  // for testing
  public CasDocSerializer getTestCasDocSerializer(ContentHandler ch, CASImpl cas) {
    return new CasDocSerializer(ch, null, cas, null, null);
  }  
  

}
