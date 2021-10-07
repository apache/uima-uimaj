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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.Marker;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CasSerializerSupport.CasDocSerializer;
import org.apache.uima.cas.impl.CasSerializerSupport.CasSerializerSupportSerialize;
import org.apache.uima.cas.impl.XmiSerializationSharedData.OotsElementData;
import org.apache.uima.cas.impl.XmiSerializationSharedData.XmiArrayElement;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.XmlAttribute;
import org.apache.uima.internal.util.XmlElementName;
import org.apache.uima.internal.util.XmlElementNameAndContents;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.CommonList;
import org.apache.uima.jcas.cas.EmptyStringList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * CAS serializer for XMI format; writes a CAS in the XML Metadata Interchange (XMI) format.
 *  
 * To use, 
 *   - create an instance of this class, 
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
 *   <li>(S) where to put the output - an OutputStream</li>
 *   <li>(I,S) a type system - (default null) if supplied, it is used to "filter" types and features that are serialized.  If provided, only 
 *   those that exist in the passed in type system are included in the serialization</li>
 *   <li>(I,S) a flag for prettyprinting - default false (no prettyprinting)</li>
 *   <li>(I) (optional) If supplied, a map used to generate a "schemaLocation" attribute in the XMI
 *          output. This argument must be a map from namespace URIs to the schema location for
 *          that namespace URI.
 *   <li>(S) (optional) if supplied XmiSerializationSharedData representing FeatureStructures
 *       that were set aside when deserializing, and are to be "merged" back in when serializing
 *   <li>(S) a Marker (default: null) if supplied, where the separation between "new" and previously
 *       exisiting FeatureStructures are in the CAS; causes "delta" serialization, where only the 
 *       new and changed FeatureStructures are serialized.
 * </ul>
 * 
 * Once the XmiCasSerializer instance is configured, the serialize method is called
 * to serialized a CAS to an output.
 * 
 * Instances of this class must be used on only one thread while configuration is being done;
 * afterwards, multiple threads may use the configured instance, to call serialize.
 */
public class XmiCasSerializer {
  
  static final char [] URIPFX = new char[] {'h','t','t','p',':','/','/','/'};
  
  static final char [] URISFX = new char[] {'.','e','c','o','r','e'};

  private static final String CDATA_TYPE = "CDATA";

  public static final String XMLNS_NS_URI = "http://www.w3.org/2000/xmlns/";

  public static final String XMI_NS_URI = "http://www.omg.org/XMI";

  public static final String XSI_NS_URI = "http://www.w3.org/2001/XMLSchema-instance";

  public static final String XMI_NS_PREFIX = "xmi";
  
  public static final String ID_ATTR_NAME = "xmi:id";

  public static final String XMI_TAG_LOCAL_NAME = "XMI";

  public static final String XMI_TAG_QNAME = "xmi:XMI";
  
  public static final XmlElementName XMI_TAG = new XmlElementName(XMI_NS_URI, XMI_TAG_LOCAL_NAME,
          XMI_TAG_QNAME);
    
  public static final String XMI_VERSION_LOCAL_NAME = "version";

  public static final String XMI_VERSION_QNAME = "xmi:version";

  public static final String XMI_VERSION_VALUE = "2.0";

  /** Namespace URI to use for UIMA types that have no namespace (the "default pacakge" in Java) */
  public static final String DEFAULT_NAMESPACE_URI = "http:///uima/noNamespace.ecore"; 

//  public final static boolean XML1_1_SUPPORTED;  // assuming xml 1.1 is always supported in today's (2020) java's
//  static {
//    boolean v;
//    try {
//      v = SAXParserFactory.newInstance().getFeature("http://xml.org/sax/features/xml-1.1");
//    } catch (SAXNotRecognizedException | SAXNotSupportedException
//        | ParserConfigurationException e) {
//      v = false;
//    }
//    XML1_1_SUPPORTED = v;
//  }

  public final static String SYSTEM_LINE_FEED;
  static {
      String lf = System.getProperty("line.separator");
      SYSTEM_LINE_FEED = (lf == null) ? "\n" : lf;
  }

  public final static char[] INT_TO_HEX = "0123456789ABCDEF".toCharArray();
  
  private final CasSerializerSupport css = new CasSerializerSupport();
  
  private Map<String, String> nsUriToSchemaLocationMap = null;
  
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
    css.filterTypeSystem = (TypeSystemImpl) ts;
    this.nsUriToSchemaLocationMap = nsUriToSchemaLocationMap;
    css.logger = UIMAFramework.getLogger(XmiCasSerializer.class);
    css.isFormattedOutput = isFormattedOutput;
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
    serialize(aCAS, aTargetTypeSystem, aStream, aPrettyPrint, aSharedData, aMarker, false);
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
   * @param useXml_1_1
   *          if true, the output serializer is set with the OutputKeys.VERSION to "1.1".         
   * @throws SAXException
   *           if a problem occurs during XMI serialization
   */
  public static void serialize(CAS aCAS, TypeSystem aTargetTypeSystem, OutputStream aStream, boolean aPrettyPrint, 
          XmiSerializationSharedData aSharedData, Marker aMarker, boolean useXml_1_1) 
          throws SAXException {
    XmiCasSerializer xmiCasSerializer = new XmiCasSerializer(aTargetTypeSystem);
    XMLSerializer sax2xml = new XMLSerializer(aStream, aPrettyPrint);
    if (useXml_1_1) {
      sax2xml.setOutputProperty(OutputKeys.VERSION,"1.1");
    }
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
    if (errorHandler != null) {
      css.setErrorHandler(errorHandler);
    }
    XmiDocSerializer ser = new XmiDocSerializer(contentHandler, ((CASImpl) cas).getBaseCAS(), sharedData, (MarkerImpl) marker);
    try {
      ser.cds.serialize();
    } catch (Exception e) {
      if (e instanceof SAXException) {
        throw (SAXException) e;
      } else {
        throw new UIMARuntimeException(e);
      }
    }  
    contentHandler.endDocument();
  }
  
  // this method just for testing - uses existing content handler and CasDocSerializer instance
  // package private for test case access
  void serialize(CAS cas, ContentHandler contentHandler, XmiDocSerializer ser) throws SAXException {
    contentHandler.startDocument();
    try {
      ser.cds.serialize();
    } catch (Exception e) {
      throw (SAXException) e;
    }
    contentHandler.endDocument();
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
    css.setPrettyPrint(pp);
    return this;
  }
    
  /**
   * pass in a type system to use for filtering what gets serialized;
   * only those types and features which are defined this type system are included.
   * @param ts the filter
   * @return the original instance, possibly updated
   */
  public XmiCasSerializer setFilterTypes(TypeSystemImpl ts) {
    css.setFilterTypes(ts);
    return this;
  }
  

  // not done here, done on serialize call, because typically changes for each call
//  /**
//   * set the Marker to specify delta cas serialization
//   * @param m - the marker
//   * @return the original instance, possibly updated
//   */
//  public XmiCasSerializer setDeltaCas(Marker m) {
//    css.setDeltaCas(m);
//    return this;
//  }
  
  /**
   * set an error handler to receive information about errors
   * @param eh the error handler
   * @return the original instance, possibly updated
   */
  public XmiCasSerializer setErrorHandler(ErrorHandler eh) {
    css.errorHandler = eh;
    return this;
  }
  
  class XmiDocSerializer extends CasSerializerSupportSerialize {
    
    private final CasDocSerializer cds;
    
    private final ContentHandler ch;
    
    private final AttributesImpl emptyAttrs = new AttributesImpl();

    private final AttributesImpl workAttrs = new AttributesImpl();

    // the number of children can't be easily computed, until serialization is attempted,
    // because the decision on whether to serialize arrays and lists "inline" or as separate
    // sub-elements is made at the point they're about to be serialized.
    
    // No one appears to be using this.  Vinci uses the number of children, but it's based on 
    // the XCASSerializer implementation, not this one.
    
//    // number of children of current element
//    private int numChildren;
//    /**
//     * Gets the number of children of the current element. This is guaranteed to be set correctly at
//     * the time when startElement is called. Needed for streaming Vinci serialization.
//     * 
//     * @return the number of children of the current element
//     */
//    protected int getNumChildren() {
//      return numChildren;
//    }
    
    private XmiDocSerializer(ContentHandler ch, CASImpl cas, XmiSerializationSharedData sharedData, MarkerImpl marker) {
      cds = css.new CasDocSerializer(ch, cas, sharedData, marker, this);
      this.ch = ch;
    }
    
    @Override
    protected void initializeNamespaces() {
      /**
       * Populates nsUriToPrefixMap and typeCode2namespaceNames structures based on CAS type system.
       */
        
      cds.nsUriToPrefixMap.put(XMI_NS_URI, XMI_NS_PREFIX);

      //Add any namespace prefix mappings used by out of type system data.
      //Need to do this before the in-typesystem namespaces so that the prefix
      //used here are reserved and won't be reused for any in-typesystem namespaces.
           
      if (cds.sharedData != null) {
        Iterator<OotsElementData> ootsIter = cds.sharedData.getOutOfTypeSystemElements().iterator();
        while (ootsIter.hasNext()) {
          OotsElementData oed = ootsIter.next();
          String nsUri = oed.elementName.nsUri;                           //  http://... etc
          String qname = oed.elementName.qName;                           //    xxx:yyy
          String localName = oed.elementName.localName;                   //        yyy
          String prefix = qname.substring(0, qname.indexOf(localName)-1); // xxx
          cds.nsUriToPrefixMap.put(nsUri, prefix);
          cds.nsPrefixesUsed.add(prefix);
        }
      }

      /*
       * Convert x.y.z.TypeName to prefix-uri, TypeName, and ns:TypeName
       */
      Iterator<Type> it = cds.tsi.getTypeIterator();
      while (it.hasNext()) {
        TypeImpl t = (TypeImpl) it.next();
        // this also populates the nsUriToPrefix map
        cds.typeCode2namespaceNames[t.getCode()] = uimaTypeName2XmiElementName(t.getName());
      }
    }
    
    @Override
    protected void writeFeatureStructures(int iElementCount) throws Exception {
      workAttrs.clear();
      computeNamespaceDeclarationAttrs(workAttrs);
      workAttrs.addAttribute(XMI_NS_URI, XMI_VERSION_LOCAL_NAME, XMI_VERSION_QNAME, "CDATA",
          XMI_VERSION_VALUE);
      startElement(XMI_TAG, workAttrs, iElementCount);
      writeNullObject(); // encodes 1 element
      cds.encodeIndexed(); // encodes indexedFSs.size() element
      cds.encodeQueued(); // encodes queue.size() elements
      if (!cds.isDelta) {  // if delta, the out-of-type-system elements, are guaranteed not to be modified
        serializeOutOfTypeSystemElements(); //encodes sharedData.getOutOfTypeSystemElements().size() elements
      }

    }
    
    @Override
    protected void writeViews() throws Exception {
      cds.writeViewsCommons();  
    }
    
    @Override
    protected void writeEndOfSerialization() throws SAXException {
      endElement(XMI_TAG);
      endPrefixMappings();    
    }
    
    @Override
    protected void writeView(Sofa sofa, Collection<TOP> members) throws Exception {
      workAttrs.clear();
      // this call should never generate a new XmiId, it should just retrieve the existing one for the sofa
      String sofaXmiId = (sofa == null) ? null : cds.getXmiId(sofa);   
      if (sofaXmiId != null && sofaXmiId.length() > 0) {
        addAttribute(workAttrs, "sofa", sofaXmiId);
      }
      StringBuilder membersString = new StringBuilder();
      boolean isPastFirstElement = writeViewMembers(membersString, members);
      //check for out-of-typesystem members
      if (cds.sharedData != null) {
        List<String> ootsMembers = cds.sharedData.getOutOfTypeSystemViewMembers(sofaXmiId);
        writeViewMembers(membersString, ootsMembers, isPastFirstElement);
      }
      if (membersString.length() > 0) {
        workAttrs.addAttribute(
            "", 
            "members",
            "members",
            CDATA_TYPE, 
            membersString.toString());

//      addAttribute(workAttrs, "members", membersString.substring(0, membersString.length() - 1));
        if (membersString.length() > 0) {
          XmlElementName elemName = uimaTypeName2XmiElementName("uima.cas.View");
          startElement(elemName, workAttrs, 0);
          endElement(elemName);
        }
      }
    }
    
    /**
     * version for out-of-type-system data being merged back in
     * not currently supported for JSON
     * @param sb - where output goes
     * @param members string representations of the out of type system ids
     * @param isPastFirstElement -
     * @return -
     */
    private StringBuilder writeViewMembers(StringBuilder sb, Collection<String> members, boolean isPastFirstElement) {
      if (members != null) {
        for (String member : members) {
          if (isPastFirstElement) {
            sb.append(' ');
          } else {
            isPastFirstElement = true;
          }
          sb.append(member);
        }
      }
      return sb;
    }

    
    private boolean writeViewMembers(StringBuilder sb, Collection<TOP> members) throws SAXException {
      boolean isPastFirstElement = false;
      int nextBreak = (((sb.length() - 1) / CasSerializerSupport.PP_LINE_LENGTH) + 1) * CasSerializerSupport.PP_LINE_LENGTH;
      for (TOP member : members) {
        int xmiId = cds.getXmiIdAsInt(member);
        if (xmiId != 0) { // to catch filtered FS         
          if (isPastFirstElement) {
            sb.append(' ');
          } else {
            isPastFirstElement = true;
          }
          sb.append(xmiId);
          if (cds.isFormattedOutput_inner && (sb.length() > nextBreak)) {
            sb.append(SYSTEM_LINE_FEED);
            nextBreak += CasSerializerSupport.PP_LINE_LENGTH; 
          }
        }
      }
      return isPastFirstElement;
    }

    private void writeViewForDeltas(String kind, Collection<TOP> deltaMembers) throws SAXException {
      StringBuilder sb = new StringBuilder();
      writeViewMembers(sb, deltaMembers);   
      if (sb.length() > 0) {
        addAttribute(workAttrs, kind, sb.toString());
      }
    }

    @Override
    protected void writeView(Sofa sofa, Collection<TOP> added, Collection<TOP> deleted, Collection<TOP> reindexed) throws SAXException {
      String sofaXmiId = cds.getXmiId(sofa);
      workAttrs.clear();
      if (sofaXmiId != null && sofaXmiId.length() > 0) {
        addAttribute(workAttrs, "sofa", sofaXmiId);
      }
      writeViewForDeltas("added_members", added);
      writeViewForDeltas("deleted_members", deleted);
      writeViewForDeltas("reindexed_members", reindexed);
            
      XmlElementName elemName = uimaTypeName2XmiElementName("uima.cas.View");
      startElement(elemName, workAttrs, 0);
      endElement(elemName);
    }
    
    /**
     * Writes a special instance of dummy type uima.cas.NULL, having xmi:id=0. This is needed to
     * represent nulls in multi-valued references, which aren't natively supported in Ecore.
     * @throws SAXException  -
     */
    void writeNullObject() throws SAXException {
      workAttrs.clear();
      addIdAttribute(workAttrs, "0");
      XmlElementName elemName = uimaTypeName2XmiElementName("uima.cas.NULL");
      startElement(elemName, workAttrs, 0);
      endElement(elemName);
    }        

    @Override
    protected void writeFs(TOP fs, int typeCode) throws SAXException {
      writeFsOrLists(fs, typeCode, false);
    }

    @Override
    protected void writeListsAsIndividualFSs(TOP fs, int typeCode) throws SAXException {
      writeFsOrLists((TOP)fs, typeCode, true);
    }

    private void writeFsOrLists(TOP fs, int typeCode, boolean isListAsFSs) throws SAXException {
      // encode features. this populates the attributes (workAttrs). It also
      // populates the child elements list with features that are to be encoded
      // as child elements (currently required for string arrays).
      List<XmlElementNameAndContents> childElements = encodeFeatures(fs, workAttrs, isListAsFSs);
      XmlElementName xmlElementName = cds.typeCode2namespaceNames[typeCode];
      startElement(xmlElementName, workAttrs, childElements.size());
      sendElementEvents(childElements);
      endElement(xmlElementName);
    }
  
    @Override
    protected void writeArrays(TOP fsArray, int typeCode, int typeClass) throws SAXException {
      XmlElementName xmlElementName = cds.typeCode2namespaceNames[typeCode];
      
      if (fsArray instanceof StringArray && ((StringArray)fsArray).size() != 0) {

        // string arrays are encoded as elements, in case they contain whitespace
        List<XmlElementNameAndContents> childElements = new ArrayList<>();
        stringArrayToElementList("elements", (StringArray) fsArray, childElements);
        startElement(xmlElementName, workAttrs, childElements.size());
        sendElementEvents(childElements);
        endElement(xmlElementName);        

      } else {
        // Saxon requirement? - can't omit (by using "") just one of localName & qName
        workAttrs.addAttribute("", "elements", "elements", "CDATA", arrayToString(fsArray, typeClass));
        startElement(xmlElementName, workAttrs, 0);
        endElement(xmlElementName);      
      }
    }
    
    private void endPrefixMappings() throws SAXException {
      Iterator<Map.Entry<String, String>> it = cds.nsUriToPrefixMap.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<String, String> entry = it.next();
        String prefix = entry.getValue();
        ch.endPrefixMapping(prefix);
      }
      if (nsUriToSchemaLocationMap != null) {
        ch.endPrefixMapping("xsi");
      }
    }

    
    /*    
     * @param workAttrs2 where to put the attributes and values
     * @throws SAXException
     */
    private void computeNamespaceDeclarationAttrs(AttributesImpl workAttrs2) throws SAXException {
      Iterator<Map.Entry<String, String>> it = cds.nsUriToPrefixMap.entrySet().iterator();
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
    
    /**
     * Serializes all of the out-of-typesystem elements that were recorded
     * in the XmiSerializationSharedData during the last deserialization.
     */
    private void serializeOutOfTypeSystemElements() throws SAXException {
      if (cds.marker != null)
            return;
      if (cds.sharedData == null)
        return;
      Iterator<OotsElementData> it = cds.sharedData.getOutOfTypeSystemElements().iterator();
      while (it.hasNext()) {
        OotsElementData oed = it.next();
        workAttrs.clear();
        // Add ID attribute
        addIdAttribute(workAttrs, oed.xmiId);

        // Add other attributes
        Iterator<XmlAttribute> attrIt = oed.attributes.iterator();
        while (attrIt.hasNext()) {
          XmlAttribute attr = attrIt.next();
          addAttribute(workAttrs, attr.name, attr.value);
        }
        // debug
        if (oed.elementName.qName.endsWith("[]")) {
          Misc.internalError(new Exception("XMI Cas Serialization: out of type system data has type name ending with []"));
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
     * @throws SAXException passthru
     */
    private List<XmlElementNameAndContents> encodeFeatures(TOP fs, AttributesImpl attrs, boolean insideListNode)
            throws SAXException {
      List<XmlElementNameAndContents> childElements = new ArrayList<>();
//      int heapValue = cds.cas.getHeapValue(addr);
//      int[] feats = cds.tsi.ll_getAppropriateFeatures(heapValue);

      String  attrValue;
      // boolean isSofa = false;
      // if (sofaTypeCode == heapValue)
      // {
      // // set isSofa flag to apply SofaID mapping and to store sofaNum->xmi:id mapping
      // isSofa = true;
      // }
      for (final FeatureImpl fi : fs._getTypeImpl().getFeatureImpls()) {

        if (cds.isFiltering) {
          // skip features that aren't in the target type system
          String fullFeatName = fi.getName();
          if (cds.filterTypeSystem_inner.getFeatureByFullName(fullFeatName) == null) {
            continue;
          }
        }

        final String featName = fi.getShortName();
        final int featureValueClass = fi.rangeTypeClass;
        
        switch (featureValueClass) {
        
        case LowLevelCAS.TYPE_CLASS_BYTE:
        case LowLevelCAS.TYPE_CLASS_SHORT:
        case LowLevelCAS.TYPE_CLASS_INT:
        case LowLevelCAS.TYPE_CLASS_LONG:
        case LowLevelCAS.TYPE_CLASS_FLOAT:
        case LowLevelCAS.TYPE_CLASS_DOUBLE: 
        case LowLevelCAS.TYPE_CLASS_BOOLEAN:
          attrValue = fs.getFeatureValueAsString(fi);
          break;
        
        case LowLevelCAS.TYPE_CLASS_STRING:
          attrValue = fs.getFeatureValueAsString(fi);
          break;

          // Arrays
        case LowLevelCAS.TYPE_CLASS_INTARRAY:
        case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
        case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY:
        case LowLevelCAS.TYPE_CLASS_BYTEARRAY:
        case LowLevelCAS.TYPE_CLASS_SHORTARRAY:
        case LowLevelCAS.TYPE_CLASS_LONGARRAY:
        case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY:
        case LowLevelCAS.TYPE_CLASS_FSARRAY: 
          if (cds.isStaticMultiRef(fi)) {
            attrValue = cds.getXmiId(fs.getFeatureValue(fi));
          } else {
            attrValue = arrayToString(fs.getFeatureValue(fi), featureValueClass);
          }
          break;
        
          // special case for StringArrays, which stored values as child elements rather
          // than attributes.
        case LowLevelCAS.TYPE_CLASS_STRINGARRAY: 
          StringArray stringArray = (StringArray) fs.getFeatureValue(fi);
          if (cds.isStaticMultiRef(fi)) {
            attrValue = cds.getXmiId(stringArray);
          } else if (stringArray != null && stringArray.size() == 0) {
            attrValue = "";  //https://issues.apache.org/jira/browse/UIMA-5558
          } else {
            stringArrayToElementList(featName, (StringArray) fs.getFeatureValue(fi), childElements);
            attrValue = null;
          }
          break;
        
          // Lists
        case CasSerializerSupport.TYPE_CLASS_INTLIST:
        case CasSerializerSupport.TYPE_CLASS_FLOATLIST:
        case CasSerializerSupport.TYPE_CLASS_FSLIST: 
          TOP startNode = fs.getFeatureValue(fi);
          if (insideListNode || cds.isStaticMultiRef(fi)) {
            // If the feature has multipleReferencesAllowed = true OR if we're already
            // inside another list node (i.e. this is the "tail" feature), serialize as a normal FS.
            // Otherwise, serialize as a multi-valued property.
//            if (cds.isStaticMultRef(feats[i]) ||
//                cds.embeddingNotAllowed.contains(featVal) ||
//                insideListNode) {
            
            attrValue = cds.getXmiId(startNode);
          } else {
            attrValue = listToString((CommonList) fs.getFeatureValue(fi));
          }
          break;
        
          // special case for StringLists, which stored values as child elements rather
          // than attributes.
        case CasSerializerSupport.TYPE_CLASS_STRINGLIST: 
          if (insideListNode || cds.isStaticMultiRef(fi)) {
            attrValue = cds.getXmiId(fs.getFeatureValue(fi));
          } else {
            // it is not safe to use a space-separated attribute, which would
            // break for strings containing spaces. So use child elements instead.
            StringList stringList = (StringList) fs.getFeatureValue(fi);
            if (stringList == null) {
              attrValue = null;
            } else {
              if (stringList instanceof EmptyStringList) {
                attrValue = "";
              } else {
                List<String> listOfStrings = stringList.anyListToStringList(null, cds);
//              if (array.length > 0 && !arrayAndListFSs.put(featVal, featVal)) {
//                reportWarning("Warning: multiple references to a ListFS.  Reference identity will not be preserved.");
//              }
                for (String string : listOfStrings) {
                  childElements.add(new XmlElementNameAndContents(new XmlElementName("", featName,
                          featName), string));
                }
                attrValue = null;
              }
            }
          }
          break;
        
        default: // Anything that's not a primitive type, array, or list.
            attrValue = cds.getXmiId(fs.getFeatureValue(fi));
            break;
          
        } // end of switch
        
        if (attrValue != null && featName != null) {
          addAttribute(attrs, featName, attrValue, "");
        }
      } // end of for loop over all features
      
      //add out-of-typesystem features, if any
      if (cds.sharedData != null) {
        OotsElementData oed = cds.sharedData.getOutOfTypeSystemFeatures(fs);
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
    
    /**
     * Not called for StringArray
     * @param fsIn -
     * @param arrayType -
     * @return -
     * @throws SAXException -
     */
    private String arrayToString(TOP fsIn, int arrayType) throws SAXException {
      if (fsIn == null) {
        return null;
      }

      StringBuilder buf = new StringBuilder();
      CommonArrayFS fs = (CommonArrayFS) fsIn;
      String elemStr = null;
      
      // FS arrays: handle shared data items
      if (fs instanceof FSArray) {
        List<XmiArrayElement> ootsArrayElementsList = cds.sharedData == null ? null : 
                                                      cds.sharedData.getOutOfTypeSystemArrayElements((FSArray) fs);
        int ootsIndex = 0;

        int j = -1;
        for (TOP elemFS : ((FSArray)fs)._getTheArray()) {
          j++;
          if (elemFS == null) { // null case
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
            
          } else {  // not null
            String xmiId = cds.getXmiId(elemFS);
            if (cds.isFiltering) { // return as null any references to types not in target TS
              String typeName = elemFS._getTypeImpl().getName();
              if (cds.filterTypeSystem_inner.getType(typeName) == null) {
                xmiId = "0";
              }
            }
            elemStr = xmiId;
          }
          
          if (buf.length() > 0) {
            buf.append(' ');
          }
          buf.append(elemStr);
        }  // end of loop over FS Array elements
        
        return buf.toString();
        
      } else if (fs instanceof ByteArray) {
        
        // special case for byte arrays: serialize as hex digits 
        byte[] ba = ((ByteArray) fs)._getTheArray();
        
        char[] r = new char[ba.length * 2];
        
        int i = 0;
        for (byte b : ba) {
          r[i++] = INT_TO_HEX[(b & 0xF0) >>> 4];
          r[i++] = INT_TO_HEX[b & 0x0F];
        }
        return new String(r);
      } else {
        // is not FSarray, is not ByteArray, is not String Array
//        CommonArrayFS fs;
//        String[] fsvalues;
//
//        switch (arrayType) {
//          case LowLevelCAS.TYPE_CLASS_INTARRAY:
//            fs = new IntArrayFSImpl(addr, cds.cas);
//            break;
//          case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
//            fs = new FloatArrayFSImpl(addr, cds.cas);
//            break;
//          case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY:
//            fs = new BooleanArrayFSImpl(addr, cds.cas);
//            break;
//          case LowLevelCAS.TYPE_CLASS_SHORTARRAY:
//            fs = new ShortArrayFSImpl(addr, cds.cas);
//            break;
//          case LowLevelCAS.TYPE_CLASS_LONGARRAY:
//            fs = new LongArrayFSImpl(addr, cds.cas);
//            break;
//          case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY:
//            fs = new DoubleArrayFSImpl(addr, cds.cas);
//            break;
//          case LowLevelCAS.TYPE_CLASS_BYTEARRAY:
//            fs = new ByteArrayFSImpl(addr, cds.cas);
//            break;
//          default: {
//            return "";
//          }
//        }

//        if (arrayType == LowLevelCAS.TYPE_CLASS_STRINGARRAY) {   // this method never called for StringArrays
//          StringArrayFS strFS = new StringArrayFSImpl(addr, cds.cas);
//          fsvalues = strFS.toArray();
//        } else {
        String[] fsvalues = fs.toStringArray();
//        }

        for (String s : fsvalues) {
          if (buf.length() > 0) {
            buf.append(' ');
          }
          buf.append(s);
        }
        return buf.toString();
      }
    }
    
    private void stringArrayToElementList(
        String featName, 
        StringArray stringArray, 
        List<? super XmlElementNameAndContents> resultList) {
      if (stringArray == null) {
        return;
      }
      // it is not safe to use a space-separated attribute, which would
      // break for strings containing spaces. So use child elements instead.
      
      for (String s : stringArray._getTheArray()) {
        resultList.add(new XmlElementNameAndContents(new XmlElementName("", featName, featName),
            s));
      }
    }


    /**
     * Converts a CAS List of Int, Float, or FsRefs to its string representation for use in multi-valued XMI properties.
     * Only called if no sharing of list nodes exists.
     * Only called for list nodes referred to by Feature value slots in some non-list FS.
     * 
     * @param curNode
     *          address of the CAS ListFS
     * 
     * @return String representation of the array, or null if passed in CASImpl.NULL
     * @throws SAXException passthru
     */
    private String listToString(CommonList fs) throws SAXException {
      if (fs == null) {
        return null;  // different from ""
      }
      final StringBuilder sb = new StringBuilder();
      fs.anyListToOutput(cds.sharedData, cds, s -> {if (sb.length() > 0) {
                                                     sb.append(' ').append(s);
                                                    } else {
                                                     sb.append(s);
                                                    }});
      return sb.toString();
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
      while (childIter.hasNext()) {
        XmlElementNameAndContents elem = childIter.next();
        if (elem.contents != null) {
          startElement(elem.name, emptyAttrs, 1);
          addText(elem.contents);
        } else {
          startElement(elem.name, emptyAttrs, 0);
        }
        endElement(elem.name);
      }      
    }

    
    private void startElement(XmlElementName name, Attributes attrs, int aNumChildren)
        throws SAXException {
      // Previously the NS URI was omitted, claiming:	
      //    >>> That causes XMI serializer to include the xmlns attribute in every element <<<
      // But without it Saxon omits process namespaces
      ch.startElement(
          name.nsUri, 
          name.localName, 
          name.qName, 
          attrs);
    }
    
    private void endElement(XmlElementName name) throws SAXException {
    //  if (name == null) {
    //    ch.endElement(null, null, null);
    //  } else {
        ch.endElement(name.nsUri, name.localName, name.qName);
    //  }
    }
    
    private void addAttribute(AttributesImpl attrs, String attrName, String attrValue) {
      addAttribute(attrs, attrName, attrValue, CDATA_TYPE);
    }

    // type info for attributes uses string values taken from
    //   http://www.w3.org/TR/xmlschema-2/
    //     decimal string boolean 
    private void addAttribute(AttributesImpl attrs, String attrName, String attrValue, String type) {
      // Provide identical values for the qName & localName (although Javadocs indicate that both can be omitted!)
      attrs.addAttribute("", attrName, attrName, type, attrValue);
      // Saxon throws an exception if either omitted:
      //     "Saxon requires an XML parser that reports the QName of each element"
      //     "Parser configuration problem: namespsace reporting is not enabled"
      // The IBM JRE implementation produces bad xml if the qName is omitted,
      //     but handles a missing localName correctly
    }

    private void addIdAttribute(AttributesImpl attrs, String attrValue) {
      attrs.addAttribute(XMI_NS_URI, "id", ID_ATTR_NAME, CDATA_TYPE, attrValue);
    }

    private void addText(String text) throws SAXException {
      ch.characters(text.toCharArray(), 0, text.length());
    }
    
    @Override
    protected void checkForNameCollision(XmlElementName xmlElementName) {}

    @Override
    protected void addNameSpace(XmlElementName xmlElementName) {};

    @Override
    protected boolean writeFsStart(TOP fs, int typeCode /* ignored */) {
      workAttrs.clear();
      addIdAttribute(workAttrs, cds.getXmiId(fs));
      return false;  // ignored
    }
   
    /**
     * Converts a UIMA-style dotted type name to the element name that should be used in the XMI
     * serialization. The XMI element name consists of three parts - the Namespace URI, the Local
     * Name, and the QName (qualified name).
     * Namespace URI = http:///uima/noNamespace.ecore or
     *                 http:///uima/package/name/with/slashes.ecore
     *   
     * 
     * @param uimaTypeName
     *          a UIMA-style dotted type name
     * @return a data structure holding the three components of the XML element name
     */
    @Override
    protected XmlElementName uimaTypeName2XmiElementName(String uimaTypeName) {
      if (uimaTypeName.endsWith(TypeSystemImpl.ARRAY_TYPE_SUFFIX)) {
        // can't write out xyz[] as the qname.  Use FSArray instead
        uimaTypeName = CASImpl.TYPE_NAME_FS_ARRAY;
      }

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
        char[] sb = new char[lastDotIndex + URIPFX.length + URISFX.length];
        System.arraycopy(URIPFX, 0, sb, 0, URIPFX.length);  // http:///
        int i = 0;
        for (; i < lastDotIndex; i++) {                     // http:///uima/tcas  or http:///org/a/b/c
          char c = uimaTypeName.charAt(i);
          sb[URIPFX.length + i] = ( c == '.') ? '/' : c;
        }
        System.arraycopy(URISFX, 0, sb, URIPFX.length + i, URISFX.length);  // http:///uima/tcas.name_space
        nsUri = cds.getUniqueString(new String(sb));
        
//        nsUri = "http:///" + namespace.replace('.', '/') + ".ecore"; 
      }
      // convert short name to shared string, without interning, reduce GCs
      shortName = cds.getUniqueString(shortName);

      // determine what namespace prefix to use
      String prefix = cds.getNameSpacePrefix(uimaTypeName, nsUri, lastDotIndex);
      // debug
      return new XmlElementName(nsUri, shortName, cds.getUniqueString(prefix + ':' + shortName));
    }

    @Override
    protected void writeEndOfIndividualFs() {}

    @Override
    protected void writeFsRef(TOP fs) throws Exception {} // only for JSON, not used here
     
  }
        
//  // for testing
//  public XmiDocSerializer getTestXmiDocSerializer(ContentHandler ch, CASImpl cas) {
//    return new XmiDocSerializer(ch, cas, null);
//  }  
  
}
