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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Marker;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CasSerializerSupport.CasDocSerializer;
import org.apache.uima.cas.impl.CasSerializerSupport.CasSerializerSupportSerialize;
import org.apache.uima.cas.impl.XmiSerializationSharedData.OotsElementData;
import org.apache.uima.cas.impl.XmiSerializationSharedData.XmiArrayElement;
import org.apache.uima.internal.util.XmlAttribute;
import org.apache.uima.internal.util.XmlElementName;
import org.apache.uima.internal.util.XmlElementNameAndContents;
import org.apache.uima.util.JsonContentHandlerJacksonWrapper;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;

/**
 * CAS serializer for JSON formats.
 * Writes a CAS in one of several JSON formats.
 *   
 * Delta serialization, which depends on a previous deserialization, is not supported (yet)
 * because JSON deserialization is not supported.
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
 * Parameters can be configured in this instance (I), and/or as part of the serialize(S) call.
 * 
 * The parameters that can be configured are:
 * <ul>
 *   <li>(S) The CAS to serialize
 *   <li>(S) where to put the output - an OutputStream, Writer, or File</li>
 *   <li>(I,S) a type system - (default null) if supplied, it is used to "filter" types and features that are serialized.  If provided, only 
 *   those that exist in the passed in type system are included in the serialization</li>
 *   <li>(I,S) a flag for prettyprinting - default false (no prettyprinting)</li>
 * </ul>
 * 
 * For Json serialization, additional configuration from the Jackson implementation can be configured
 * on 2 associated Jackson instances:  
 *   - JsonFactory 
 *   - JsonGenerator
 * using the standard Jackson methods on the associated JsonFactory instance; 
 * see the Jackson JsonFactory and JsonGenerator javadocs for details.
 * 
 * These 2 Jackson objects are settable/gettable from an instance of this class.
 * They are created if not supplied by the caller.
 * 
 * Once this instance is configured, the serialize method is called
 * to serialized a CAS to an output.
 * 
 * Instances of this class must be used on only one thread while configuration is being done;
 * afterwards, multiple threads may use the configured instance, to call serialize.
 */
public class JsonCasSerializer {

  private static final SerializedString FEATURE_REFS_NAME = new SerializedString("@featureRefs");
  
  private static final SerializedString SUPER_TYPES_NAME = new SerializedString("@superTypes");
    
  private static final SerializedString JSON_CONTEXT_TAG_LOCAL_NAME = new SerializedString("@context");
  
  private static final SerializedString JSON_CAS_FEATURE_STRUCTURES = new SerializedString("@cas_feature_structures");
  
  private static final SerializedString JSON_CAS_VIEWS = new SerializedString("@cas_views");
  
  private static final SerializedString JSON_ID_ATTR_NAME = new SerializedString("@id");
  
  private static final SerializedString JSON_TYPE_ATTR_NAME = new SerializedString("@type");
  
  private static final SerializedString JSON_COLLECTION_ATTR_NAME = new SerializedString("@collection");
  
  private static final SerializedString JSON_VIEW_ADDED = new SerializedString("added_members");
  private static final SerializedString JSON_VIEW_DELETED = new SerializedString("deleted_members");
  private static final SerializedString JSON_VIEW_REINDEXED = new SerializedString("reindexed_members");
  
  /**
   *  This enum describes the kinds of JSON formats used for serializing Feature Structures
   *  An individual Feature Structure is serialized as a JSON object, with feature-value pairs.
   *  
   *  The "type" and "id" are usually embedded as extra special features in the list of features, but
   *  if that is not wanted, they can be turned off:
   *  
   *    OMIT_ID
   *    OMIT_TYPE
   *  
   *  The type or ID can optionally be used as an key in an enclosing map, to make accessing things 
   *  by these keys straightforward.  If you want the serialization to include an enclosing map, 
   *  you pick from one (not both) of these:
   *  
   *    INDEX_ID
   *    INDEX_TYPE
   *  
   *  Without the enclosing map, the collection of feature structures is serialized as a JSON array of
   *  feature structure objects. 
   *  
   *  With the enclosing map, the key is the value of the index item, and the values are either
   *    
   *    a JSON ARRAY of feature structure objects, for KEY_BY_TYPE, 
   *        since there can be multiple feature structures of a particular type, or
   *        
   *    one feature structure object, for KEY_BY_ID, since each feature structure has a unique ID
   *       
   *  In the case of KEY_BY_TYPE, the feature structures are ordered like the UIMA built-in annotation index.
   *  
   *  Example of INDEX_ID:
   *      { "123" : { "@id" : 123, "@type" : "type-name", feat : value, ... } 
   *          
   *  Example of INDEX_TYPE:
   *      { "type-name" : [ { "@id" : 123, "@type" : "type-name", feat : value ... }, 
   *                        { "@id" : 456, "@type" : "type-name", feat : value ... }
   *                        ...
   *                      ], 
   *        ...
   *      }
   */
  public enum JsonCasFormat {
    OMIT_TYPE,
    OMIT_ID,
    INDEX_TYPE,    // outputs each FS as "nnn"  : { "@type" : "foo", features : values ... }
    INDEX_ID,    // outputs each FS as "type" : { "@id" : 123, features : values ... }
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
  
  private final CasSerializerSupport css = new CasSerializerSupport();

  private JsonFactory jsonFactory = null;

  private final EnumSet<JsonCasFormat> jsonCasFormat = EnumSet.noneOf(JsonCasFormat.class);
  
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
   * Creates a new XmiCasSerializer
   */
  public JsonCasSerializer() {
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
   * @throws IOException if there was an IOException
   */
  public static void jsonSerialize(CAS aCAS, Object output) throws IOException {  
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
   * @throws IOException if there was an IOException 
   */
  public static void jsonSerialize(CAS aCAS, TypeSystem aTargetTypeSystem, Object output)
          throws IOException {
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
   *          an optional container for data that is shared between the {@link JsonCasSerializer} and the {@link XmiCasDeserializer}.
   *          See the JavaDocs for {@link XmiSerializationSharedData} for details.
   * @param aMarker
   *        an optional object used to determine which FeatureStructures and modifications were created after
   *          the mark was set. Used to serialize a Delta CAS consisting of only new FSs and views and
   *          preexisting FSs and Views that have been modified.  If null, full serialization is done.        
   *          See the JavaDocs for {@link Marker} for details.
   * @throws IOException if there was an IOException
   */
  public static void jsonSerialize(CAS aCAS, TypeSystem aTargetTypeSystem, Object output, boolean aPrettyPrint, Marker aMarker)
          throws IOException {
    JsonCasSerializer ser = new JsonCasSerializer();
    ser.setFilterTypes((TypeSystemImpl)aTargetTypeSystem);
    ser.setPrettyPrint(aPrettyPrint);
    ser.setDeltaCas(aMarker);  
    ser.serialize(aCAS, output);
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
   *          instance.jsonFormat(EnumSet.of(x, y, z)) - default is none of the settings
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
   * @throws IOException if there was an IOException
   */
  public void serialize(CAS cas, Object output) throws IOException {
    JsonContentHandlerJacksonWrapper jch;
    try {
      jch = new JsonContentHandlerJacksonWrapper(jsonFactory, output, css.isFormattedOutput);
    } catch (SAXException e) {
      throw new IOException(e);
    }
    serialize(cas, jch);
  }
  
  /**
   * Serialize a Cas to an Output configured in the passed in JsonContentHandlerJacksonWrapper
   *   Constructs a new CasDocSerializer instance to do the serialization, 
   *      configured using this class's Delta marker setting (if any)
   * @param cas The CAS to serialize
   * @param jch the configured content handler
   * @throws IOException if there was an IOException 
   */
  public void serialize(CAS cas, JsonContentHandlerJacksonWrapper jch) throws IOException {
      JsonDocSerializer ser = new JsonDocSerializer(jch, ((CASImpl) cas).getBaseCAS());
      try {
        ser.cds.needNameSpaces = false;
        ser.cds.serialize();
      } catch (Exception e) {
        throw (IOException) e;
      }
  }
  
  /********************************************************
   *   Routines to set/reset configuration                *
   ********************************************************/
  /**
   * set or reset the pretty print flag (default is false)
   * @param pp true to do pretty printing of output
   * @return the original instance, possibly updated
   */
  public JsonCasSerializer setPrettyPrint(boolean pp) {
    css.setPrettyPrint(pp);
    return this;
  }
  
  /**
   * set which JsonFactory instance to use; if null, a new instance is used
   *   this can be used to preconfigure the JsonFactory instance
   * @param jsonFactory -
   * @return the original instance, possibly updated
   */
  public JsonCasSerializer setJsonFactory(JsonFactory jsonFactory) {
    this.jsonFactory = jsonFactory;
    return this;
  }
  
  /**
   * pass in a type system to use for filtering what gets serialized;
   * only those types and features which are defined this type system are included.
   * @param ts the filter
   * @return the original instance, possibly updated
   */
  public JsonCasSerializer setFilterTypes(TypeSystemImpl ts) {
    css.setFilterTypes(ts);
    return this;
  }
  
  /**
   * set the Marker to specify delta cas serialization
   * @param m - the marker
   * @return the original instance, possibly updated
   */
  public JsonCasSerializer setDeltaCas(Marker m) {
    css.setDeltaCas(m);
    return this;
  }
  
  /**
   * set an error handler to receive information about errors
   * @param eh the error handler
   * @return the original instance, possibly updated
   */
  public JsonCasSerializer setErrorHandler(ErrorHandler eh) {
    css.setErrorHandler(eh);
    return this;
  }
  
  /**
   * adds the style of Json formatting desired.
   * @param format specifies the style
   * @return the original instance, possibly updated
   */
  public JsonCasSerializer jsonCasFormatEnable(JsonCasFormat format) {
    jsonCasFormat.add(format);
    if (format == JsonCasFormat.INDEX_ID) {
      jsonCasFormat.remove(JsonCasFormat.INDEX_TYPE);
    } else if (format == JsonCasFormat.INDEX_TYPE) {
      jsonCasFormat.remove(JsonCasFormat.INDEX_ID);
    }
    return this;
  }
  
  /**
   * removes the style of Json formatting desired.
   * @param format specifies the style
   * @return the original instance, possibly updated
   */
  public JsonCasSerializer jsonCasFormatDisable(JsonCasFormat format) {
    jsonCasFormat.remove(format);
    return this;
  }
  
  /**
   * sets which Json context format to use when serializing
   * @param format the format to use for the serialization
   *   Specifying the context flag also specifies all 3 subflags
   *   Specifying one of the subflags as true sets the context flag to true if it isn't already
   * @return the original instance, possibly updated
   */
  public JsonCasSerializer setJsonContext(JsonContextFormat format) {
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
  public JsonCasSerializer setCasViews() {
    return setCasViews(true);
  }
  
  /**
   * Sets whether or not to include which Feature Structures were indexed, by view
   * @param includeViews true to include the index information
   * @return the original instance, possibly updated
   */
  public JsonCasSerializer setCasViews(boolean includeViews) {
    isWithViews = includeViews;
    return this;
  }
  
  public JsonCasSerializer setOmitDefaultValues(boolean omitDefaultValues) {
    isOmitDefaultValues = omitDefaultValues;
    return this;
  }
  
  class JsonDocSerializer extends CasSerializerSupportSerialize {
    
    private final CasDocSerializer cds;
    
    private final JsonContentHandlerJacksonWrapper jch;

    private final JsonGenerator jg;
       
    private final Set<Type> jsonRecordedSuperTypes = new HashSet<Type>();

    private final Map<String, SerializedString> serializedStrings = new HashMap<String, SerializedString>();
    
    private final Map<String, XmlElementName> usedTypeName2XmlElementName; 
   
    private int lastEncodedTypeCode;
    
    private final boolean isOmitDefaultValues;
    
    private final boolean isWithContext;

    private final boolean isWithExpandedTypeNames;

    private final boolean isWithFeatureRefs;

    private final boolean isWithSupertypes;

    private final boolean isWithViews;
    
    private final boolean isWithContextOrViews;
    
    private final boolean omitType;
    private final boolean omitId;
    private final boolean indexType;
    private final boolean indexId;

    private JsonDocSerializer(ContentHandler ch, CASImpl cas) {
      cds = css.new CasDocSerializer(ch, cas, null, this);
      this.isOmitDefaultValues = JsonCasSerializer.this.isOmitDefaultValues; 
      boolean tempIsWithContext = JsonCasSerializer.this.isWithContext; 
      isWithExpandedTypeNames = JsonCasSerializer.this.isWithExpandedTypeNames; 
      isWithFeatureRefs = JsonCasSerializer.this.isWithFeatureRefs; 
      isWithSupertypes = JsonCasSerializer.this.isWithSupertypes; 
      isWithViews = JsonCasSerializer.this.isWithViews; 
      jch = (JsonContentHandlerJacksonWrapper) ch;
      jg = jch.getJsonGenerator();
      isWithContext = (tempIsWithContext && !isWithSupertypes && !isWithFeatureRefs && !isWithExpandedTypeNames) ? 
          false : tempIsWithContext;
      isWithContextOrViews = isWithContext || isWithViews;
      usedTypeName2XmlElementName = new HashMap<String, XmlElementName>(cds.tsi.getNumberOfTypes());

      omitType = JsonCasSerializer.this.jsonCasFormat.contains(JsonCasFormat.OMIT_TYPE);
      omitId =   JsonCasSerializer.this.jsonCasFormat.contains(JsonCasFormat.OMIT_ID);
      indexId   = JsonCasSerializer.this.jsonCasFormat.contains(JsonCasFormat.INDEX_ID);
      indexType = indexId ? false : JsonCasSerializer.this.jsonCasFormat.contains(JsonCasFormat.INDEX_TYPE);
    }
    
    protected void initializeNamespaces() {
      if (cds.sharedData != null &&
          (null != cds.sharedData.getOutOfTypeSystemElements() ||
           cds.sharedData.hasOutOfTypeSystemArrayElements())) {
        throw new UnsupportedOperationException("Can't do JSON serialization "
            + "if there are out-of-type-system elements,"
            + " because there's no type information available (needed for @context)");
      }
    }

    protected void writeViews() throws Exception {
      if (!isWithViews) {
        return;
      }
      jch.writeNlJustBeforeNext();
      jg.writeFieldName(JSON_CAS_VIEWS);
      jg.writeStartObject();        
    
      cds.writeViewsCommons(); // encodes cas.sofaCount + 1 elements
      jg.writeEndObject();  // and end of views property 
    }
   
    protected void writeFeatureStructures(int elementCount /* not used */ ) throws Exception{
      jch.withoutNl();  // set up prettyprint mode so this class controls it
      if (isWithContextOrViews) {
        jg.writeStartObject();  // container for context, fss, and views
      }
      if (isWithContext) {
        serializeJsonLdContext();
      }
      if (isWithContextOrViews) {
        jch.writeNlJustBeforeNext();       
        jg.writeFieldName(JSON_CAS_FEATURE_STRUCTURES);
      }
      
      if (indexId || indexType) {
        jg.writeStartObject();  // either outer object, or object of JSON_CAS_FEATURE_STRUCTURES
      } else {
        jg.writeStartArray();
      }
      
      if (indexType) { 
        Integer[] allFss = cds.collectAllFeatureStructures();
        Arrays.sort(allFss, cds.sortFssByType);
        encodeAllFss(allFss);
      } else {
        cds.encodeIndexed();
        cds.encodeQueued();
      }
      
      // out of type system data not supported - no type info for @context available
//      if (!cds.isDelta) {  // if delta, the out-of-type-system elements, are guaranteed not to be modified
//        serializeOutOfTypeSystemElements(); //encodes sharedData.getOutOfTypeSystemElements().size() elements
//      }
      
      if (indexId || indexType) {
        jg.writeEndObject();  // either outer object, or object of JSON_CAS_FEATURE_STRUCTURES
      } else {
        jg.writeEndArray();
      }
    }
    
    protected void writeEndOfSerialization() throws IOException {
      if (isWithContextOrViews) {
        jg.writeEndObject(); // wrapper of @context and cas
      }
      jg.flush();
    }
   
    
    protected void writeView(int sofaAddr, int[] members) throws IOException {
      jch.writeNlJustBeforeNext();
      String xmiId = (0 == sofaAddr) ? "0" : cds.getXmiId(sofaAddr);
      jg.writeArrayFieldStart(xmiId);
      writeViewMembers(members);
      //check for out-of-typesystem members
      if (cds.sharedData != null) {
        String sofaXmiId = cds.getXmiId(sofaAddr);
        List<String> ootsMembers = cds.sharedData.getOutOfTypeSystemViewMembers(sofaXmiId);
        jch.writeNlJustBeforeNext();
        writeViewMembers(ootsMembers);
      }

      jg.writeEndArray();
    }
    
    private void writeViewForDeltas(SerializedString kind, int[] deltaMembers) throws IOException {
      jg.writeFieldName(kind);
      jg.writeStartArray();
      writeViewMembers(deltaMembers);   
      jg.writeEndArray();
    }
    
    protected void writeView(int sofaAddr, int[] added, int[] deleted, int[] reindexed) throws IOException {
      jch.writeNlJustBeforeNext();
      jg.writeFieldName(Integer.toString(sofaAddr));
      jg.writeStartObject();
      writeViewForDeltas(JSON_VIEW_ADDED, added);
      writeViewForDeltas(JSON_VIEW_DELETED, deleted);
      writeViewForDeltas(JSON_VIEW_REINDEXED, reindexed);      
      jg.writeEndObject();
    }
    
    private void writeViewMembers(int[] members) throws IOException {
      int nextBreak = CasSerializerSupport.PP_ELEMENTS;
      int i = 0;
      for (int member : members) {
        int xmiId = cds.getXmiIdAsInt(member);
        if (xmiId == 0) {
          continue;
        }
        if (i++ > nextBreak) {
          jch.writeNlJustBeforeNext();
          nextBreak += CasSerializerSupport.PP_ELEMENTS;
        }
        jg.writeNumber(xmiId);
      }
    }

    /**
     * version for oots data
     */
    
    private void writeViewMembers(List<String> members) throws IOException {
      int nextBreak = CasSerializerSupport.PP_ELEMENTS;
      int i = 0;
      for (String xmiId : members) {
        if (null == xmiId || xmiId.length() == 0) {
          continue;
        }
        if (i++ > nextBreak) {
          jch.writeNlJustBeforeNext();
          nextBreak += CasSerializerSupport.PP_ELEMENTS;
        }
        jg.writeNumber(Integer.parseInt(xmiId));
      }
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
     *       "@featureRefs" : [ "featName1", "featName2"],
     *       "@featureRefsIfSingle" : [ "featName1", ...] }
     *   
     *   @featureRefs: the named feature values are a number or an array of numbers, all of which
     *                 are to be interpreted as an ID of another serialized Feature Structure (unless 0, 
     *                 which is like a null reference)
     *   @featureRefsIfSingle: the named feature values, if a single number, are to be interpreted
     *                 as an ID of another serialized feature (unless 0).
     *                 If the value is an array, then the array of values is the value of that slot,
     *                 not (if numbers) references to other Feature Structures.
     *   
     *    superType values are longNames
     *    if no featureRefs, omit
     * @throws IOException 
     */
    
    private void serializeJsonLdContext() throws IOException {  
      //   @context :  { n : v ... }}
      jg.writeFieldName(JSON_CONTEXT_TAG_LOCAL_NAME);
//      jch.setDoNL();
      jg.writeStartObject();
      
//      if (needNameSpaces) {
//        serializeNameSpaceInfo();
//      }
      
      for (TypeImpl ti : cds.getSortedUsedTypes()) {
        jch.writeNlJustBeforeNext();      
        jg.writeFieldName(getSerializedTypeName(ti.getCode()));
        jg.writeStartObject();
        jch.writeNlJustBeforeNext();
        if (isWithExpandedTypeNames) {
          jg.writeFieldName(JSON_ID_ATTR_NAME);  // form for using SerializedString
          jg.writeString(ti.getName());
        }
        if (isWithFeatureRefs) {
          addJsonFeatRefs(ti);
        }
        if (isWithSupertypes) {
          addJsonSuperTypes(ti);
        }
        jg.writeEndObject();  // end of one type
      }
      jg.writeEndObject();  // end of context
    }
    
    /**
     * Adds the ", @featureRefs" : [ "featName1", "featName2"] 
     * for features that are being serialized as references. 
     * 
     * @param type the range type of the Feature 
     * @throws IOException 
     */
    private void addJsonFeatRefs(TypeImpl type) throws IOException {
      final int typeCode = type.getCode();
      final int[] feats = cds.tsi.ll_getAppropriateFeatures(typeCode);
      boolean started = false;
      for (int featCode : feats) {
        final int fsClass = cds.classifyType(cds.tsi.range(featCode));
        
        if (isRefToFS(fsClass, featCode)) {
          if (!started) {
            jch.writeNlJustBeforeNext();
            jg.writeFieldName(FEATURE_REFS_NAME);
            jg.writeStartArray();
            started = true; 
          }
          jg.writeString(getSerializedString(cds.tsi.ll_getFeatureForCode(featCode).getShortName()));          
        }        
      }
      if (started) {
        jg.writeEndArray();
      }      
    }
    
    /**
     * Add superType chain to required type, up to point already covered
     * @throws IOException 
     */
    private void addJsonSuperTypes(TypeImpl ti) throws IOException {
      jch.writeNlJustBeforeNext();
      jg.writeFieldName(SUPER_TYPES_NAME);
      jg.writeStartArray();
      for (TypeImpl parent = (TypeImpl) ti.getSuperType(); 
           parent != null; 
           parent = (TypeImpl) parent.getSuperType()) {
        jg.writeString(parent.getName());
        if (!jsonRecordedSuperTypes.add(parent)) {
          break;  // if this item already output
        }
      }
      jg.writeEndArray();
    }

    private void encodeAllFss(Integer[] allFss) throws IOException {
      lastEncodedTypeCode = -1;
      try {
        for (Integer i : allFss) {
          cds.encodeFS(i);
        }
      } catch (Exception e) {
        throw (IOException) e;
      }
      if (lastEncodedTypeCode != -1) {
        jg.writeEndArray();
      }
    }
    
    private SerializedString getSerializedTypeName(int typeCode) {
      XmlElementName xe = cds.typeCode2namespaceNames[typeCode];
      return getSerializedString(cds.needNameSpaces ? xe.qName : xe.localName);
    }
    
    private SerializedString getSerializedString(String s) {
      SerializedString ss = serializedStrings.get(s);
      if (ss == null) {
        ss = new SerializedString(s);
        serializedStrings.put(s, ss);
      }
      return ss;
    }

    protected void enqueueIncoming() {}
    
    protected void enqueueNonsharedMultivaluedFS() {}
    
    protected boolean checkForNameCollision(XmlElementName xmlElementName) {
      XmlElementName xel    = usedTypeName2XmlElementName.get(xmlElementName.localName);
      if (xel != null) {
        if (xel.nsUri.equals(xmlElementName.nsUri)) {
          return false;  // don't need name spaces yet
        } else {
          usedTypeName2XmlElementName.clear();  // not needed anymore
          return true; // collision - need name space
        }
      }
      usedTypeName2XmlElementName.put(xmlElementName.localName, xmlElementName);
      return false;
    }
    
    protected void writeFsStart(int addr, int typeCode) throws IOException {

      if (indexId) {
        jch.writeNlJustBeforeNext();
        jg.writeFieldName(cds.getXmiId(addr));
        jg.writeStartObject();  // start of feat : value
      } else if (indexType) { // fs's as arrays under typeName
        if (typeCode != lastEncodedTypeCode) {
          if (lastEncodedTypeCode != -1) {
            // close off previous Array
            jg.writeEndArray();
          }
          lastEncodedTypeCode = typeCode;
          jch.writeNlJustBeforeNext();
          jg.writeFieldName(getSerializedTypeName(typeCode));
          jg.writeStartArray();
        }
        jch.writeNlJustBeforeNext();
        jg.writeStartObject();  // start of feat : value
      } else { // no index
        jch.writeNlJustBeforeNext();
        jg.writeStartObject();
      }
    }
    
    private void maybeWriteIdFeat(int addr) throws IOException {
      if (!omitId) {
        jg.writeFieldName(JSON_ID_ATTR_NAME);
        jg.writeNumber(cds.getXmiIdAsInt(addr));
      }
    }
    
    private void maybeWriteTypeFeat(int typeCode) throws IOException {
      if (!omitType) {
        jg.writeFieldName(JSON_TYPE_ATTR_NAME);
        jg.writeString(getSerializedTypeName(typeCode));
      }
    }

    protected void writeFs(int addr, int typeCode) throws IOException {
      writeFsOrLists(addr, typeCode, false);
    }
    
    protected void writeListsAsIndividualFSs(int addr, int typeCode) throws IOException {
      writeFsOrLists(addr, typeCode, true);
    }
    
    private void writeFsOrLists(int addr, int typeCode, boolean isListAsFSs) throws IOException {
      final int[] feats = cds.tsi.ll_getAppropriateFeatures(typeCode);
      
      maybeWriteIdFeat(addr);
      maybeWriteTypeFeat(typeCode);
      
      for (final int featCode : feats) {

        if (cds.isFiltering) {
          // skip features that aren't in the target type system
          String fullFeatName = cds.tsi.ll_getFeatureForCode(featCode).getName();
          if (cds.filterTypeSystem.getFeatureByFullName(fullFeatName) == null) {
            continue;
          }
        }
        
        final String featName = cds.tsi.ll_getFeatureForCode(featCode).getShortName();
        final int featAddr = addr + cds.cas.getFeatureOffset(featCode);
        final int featValRaw = cds.cas.getHeapValue(featAddr);
        final int featureClass = cds.classifyType(cds.tsi.range(featCode));
        
//        jg.writeFieldName(getSerializedString(featName)); // not done here, because if null feat can be omitted
        
        switch (featureClass) {
        
        case LowLevelCAS.TYPE_CLASS_BYTE:
        case LowLevelCAS.TYPE_CLASS_SHORT:  
        case LowLevelCAS.TYPE_CLASS_INT:
          if (featValRaw == 0 && isOmitDefaultValues) continue;
          jg.writeFieldName(getSerializedString(featName));
          jg.writeNumber(featValRaw);
          break;

        case LowLevelCAS.TYPE_CLASS_FS:
          if (featValRaw == 0/* && isOmitDefaultValues*/) continue;
          jg.writeFieldName(getSerializedString(featName));
          jg.writeNumber(cds.getXmiIdAsInt(featValRaw));
          break;
  
        case LowLevelCAS.TYPE_CLASS_LONG:
          final long longVal = cds.cas.ll_getLongValue(featValRaw);
          if (longVal == 0L && isOmitDefaultValues) continue;
          jg.writeFieldName(getSerializedString(featName));
          jg.writeNumber(longVal);
          break;
          
        case LowLevelCAS.TYPE_CLASS_FLOAT:
          final float floatVal = CASImpl.int2float(featValRaw);
          if (floatVal == 0.F && isOmitDefaultValues) continue;
          jg.writeFieldName(getSerializedString(featName));
          jg.writeNumber(floatVal);
          break;
          
        case LowLevelCAS.TYPE_CLASS_DOUBLE:
          final double doubleVal = cds.cas.ll_getDoubleValue(addr, featCode);
          if (doubleVal == 0L && isOmitDefaultValues) continue;
          jg.writeFieldName(getSerializedString(featName));
          jg.writeNumber(doubleVal);
          break;
          
        case LowLevelCAS.TYPE_CLASS_BOOLEAN:
          jg.writeFieldName(getSerializedString(featName));
          jg.writeBoolean(cds.cas.ll_getBooleanValue(addr, featCode));           
          break; 
        
        case LowLevelCAS.TYPE_CLASS_STRING:
          if (featValRaw == 0 /*&& isOmitDefaultValues*/) continue; 
          jg.writeFieldName(getSerializedString(featName));
          jg.writeString(cds.cas.getStringForCode(featValRaw));
          break; 
            
        // all other fields (arrays, lists, fsRefs) can be null and are omitted if so  
        default: 
          if (featValRaw != CASImpl.NULL /*|| !isOmitDefaultValues*/) {
            
            jg.writeFieldName(getSerializedString(featName));

            if (featureClass == LowLevelCAS.TYPE_CLASS_INTARRAY ||
                featureClass == LowLevelCAS.TYPE_CLASS_FLOATARRAY ||
                featureClass == LowLevelCAS.TYPE_CLASS_BOOLEANARRAY ||
                featureClass == LowLevelCAS.TYPE_CLASS_BYTEARRAY ||
                featureClass == LowLevelCAS.TYPE_CLASS_SHORTARRAY ||
                featureClass == LowLevelCAS.TYPE_CLASS_LONGARRAY ||
                featureClass == LowLevelCAS.TYPE_CLASS_DOUBLEARRAY ||
                featureClass == LowLevelCAS.TYPE_CLASS_STRINGARRAY ||
                featureClass == LowLevelCAS.TYPE_CLASS_FSARRAY) {
              
              if (cds.tsi.ll_getFeatureForCode(featCode).isMultipleReferencesAllowed()) {
                jg.writeNumber(cds.getXmiIdAsInt(featValRaw));
              } else {
                writeJsonArrayValues(featValRaw, featureClass);
              }
              
            } else if (featureClass == CasSerializerSupport.TYPE_CLASS_INTLIST ||
                       featureClass == CasSerializerSupport.TYPE_CLASS_FLOATLIST ||
                       featureClass == CasSerializerSupport.TYPE_CLASS_STRINGLIST ||
                       featureClass == CasSerializerSupport.TYPE_CLASS_FSLIST) {

              if (isListAsFSs || cds.tsi.ll_getFeatureForCode(featCode).isMultipleReferencesAllowed()) {
                jg.writeNumber(cds.getXmiIdAsInt(featValRaw));
              } else {
                writeJsonListValues(featValRaw);
              }              
            } else {  // is error
              throw new RuntimeException("Invalid State, featureClass was "+ featureClass);
            }
          }  // end of default case with non-null values
        }  // end of switch
      } // end of loop over all features
    }
    
    protected void writeArrays(int addr, int typeCode, int typeClass) throws IOException {
      maybeWriteIdFeat(addr);
      maybeWriteTypeFeat(typeCode);

      jg.writeFieldName(JSON_COLLECTION_ATTR_NAME);            
      writeJsonArrayValues(addr, typeClass);
    }
    
    protected void writeEndOfIndividualFs() throws IOException {
      jg.writeEndObject();
    }

    // writes a set of values in a JSON array
    // or null if the reference to the UIMA array is actually null
    // 0 length arrays are written as [] 
    private void writeJsonArrayValues(int addr, int arrayType) throws IOException {
      if (addr == CASImpl.NULL) {
        jg.writeNull();
        return;
      }
      
      final int size = cds.cas.ll_getArraySize(addr);

      jg.writeStartArray();
      int pos = cds.cas.getArrayStartAddress(addr);
      
      if (arrayType == LowLevelCAS.TYPE_CLASS_FSARRAY) {
      
        List<XmiArrayElement> ootsArrayElementsList = cds.sharedData == null ? null : 
          cds.sharedData.getOutOfTypeSystemArrayElements(addr);
        int ootsIndex = 0;

        for (int j = 0; j < size; j++) {  // j used to id the oots things
          int heapValue = cds.cas.getHeapValue(pos++);

          if (heapValue == CASImpl.NULL) {
            // this null array element might have been a reference to an 
            // out-of-typesystem FS, which, when deserialized, was replaced with NULL,
            // so check the ootsArrayElementsList
            boolean found = false;
            if (ootsArrayElementsList != null) {
              
              while (ootsIndex < ootsArrayElementsList.size()) {
                XmiArrayElement arel = ootsArrayElementsList.get(ootsIndex++);
                if (arel.index == j) {
                  jg.writeNumber(Integer.parseInt(arel.xmiId));
                  found = true;
                  break;
                }                
              }
            }
            if (!found) {
              jg.writeNumber(0);
            }
            
          // else, not null FS ref  
          } else {
            if (cds.isFiltering) { // return as null any references to types not in target TS
              String typeName = cds.tsi.ll_getTypeForCode(cds.cas.getHeapValue(addr)).getName();
              if (cds.filterTypeSystem.getType(typeName) == null) {
                heapValue = CASImpl.NULL;
              }
            }
            jg.writeNumber(cds.getXmiIdAsInt(heapValue));
          }
        } // end of loop over all refs in FS array
        
      } else if (arrayType == LowLevelCAS.TYPE_CLASS_BYTEARRAY) {
        // special case for byte arrays: 
        // serialize using standard JACKSON/JSON binary serialization
        // lazy - doing extra copy to avoid figuring out the impl details
        ByteArrayFS byteArrayFS = new ByteArrayFSImpl(addr, cds.cas);
        int length = byteArrayFS.size();
        byte[] byteArray = new byte[length];
        byteArrayFS.copyToArray(0, byteArray, 0, length);
      
        jg.writeBinary(byteArray);
        
      } else {
        for (int i = 0; i < size; i++) {
          if (arrayType == LowLevelCAS.TYPE_CLASS_BOOLEANARRAY) {
            jg.writeBoolean(cds.cas.ll_getBooleanArrayValue(addr, i));
          } else if (arrayType == LowLevelCAS.TYPE_CLASS_STRINGARRAY) {
            jg.writeString(cds.cas.ll_getStringArrayValue(addr, i));
//          } else if (arrayType == LowLevelCAS.TYPE_CLASS_BYTEARRAY) {
//            jg.writeNumber(cds.cas.ll_getByteArrayValue(addr, i));
          } else if (arrayType == LowLevelCAS.TYPE_CLASS_SHORTARRAY) {
            jg.writeNumber(cds.cas.ll_getShortArrayValue(addr, i));
          } else if (arrayType == LowLevelCAS.TYPE_CLASS_INTARRAY) {
            jg.writeNumber(cds.cas.ll_getIntArrayValue(addr, i));
          } else if (arrayType == LowLevelCAS.TYPE_CLASS_LONGARRAY) {
            jg.writeNumber(cds.cas.ll_getLongArrayValue(addr, i));
          } else if (arrayType == LowLevelCAS.TYPE_CLASS_FLOATARRAY) {
            jg.writeNumber(cds.cas.ll_getFloatArrayValue(addr, i));
          } else {
            jg.writeNumber(cds.cas.ll_getDoubleArrayValue(addr, i));
          }
        }
      }
      jg.writeEndArray();
    }
    
    // a null ref is written as null
    // an empty list is written as []
    /**
     * Only called if no sharing of list nodes exists.
     * Only called for list nodes referred to by Feature value slots in some non-list FS.

     * @param curNode the address of the start of the list
     * @throws IOException
     */
    private void writeJsonListValues(int curNode) throws IOException {
      if (curNode == CASImpl.NULL) {
//        jg.writeNull();
//        return;
        throw new RuntimeException("never happen");
      }
      final ListUtils listUtils = cds.listUtils;
      final int startNodeType = cds.cas.getHeapValue(curNode);

      int headFeat = listUtils.getHeadFeatCode(startNodeType);
      int tailFeat = listUtils.getTailFeatCode(startNodeType);
      int neListType = listUtils.getNeListType(startNodeType);  // non-empty
           
      jg.writeStartArray();
      while (curNode != CASImpl.NULL) { 
         
        final int curNodeType = cds.cas.getHeapValue(curNode);
        if (curNodeType != neListType) { // if not "non-empty"
          break;  // would be the end element.  a 0 is also treated as an end element
        }
        
        final int val = cds.cas.getHeapValue(curNode + cds.cas.getFeatureOffset(headFeat));

        if (curNodeType == listUtils.neStringListType) {
          jg.writeString(cds.cas.getStringForCode(val));
        } else if (curNodeType == listUtils.neFloatListType) {
          jg.writeNumber(CASImpl.int2float(val));
        } else if (curNodeType == listUtils.neFsListType) {
          jg.writeNumber(cds.getXmiIdAsInt(val));
        } else {  // for ints 
          jg.writeNumber(val);
        }

        curNode = cds.cas.getHeapValue(curNode + cds.cas.getFeatureOffset(tailFeat));
      }
      jg.writeEndArray();
    
    }
    
    /**
     * Return true if the range of the feature is a fsRef, or a 
     * collection of fsRefs (array or list), where the feature is marked
     * multiple references allowed.
     *     
     * Return false for other primitives, or collections (arrays or lists) of primitives
     * where the collection (feature) is marked as multipleReferencesAllowed = false
     *  
     *   
     * @param fsClass the class of the feature
     * @param featCode the feature code
     * @return true if the serialization is being done by having the value of this feature be a reference id
     *              or (in the case of embedded collections, where the collection items are feature references
     */
   
    private boolean isRefToFS(int fsClass, int featCode) {
      switch (fsClass) {
        case LowLevelCAS.TYPE_CLASS_FS: 
        case LowLevelCAS.TYPE_CLASS_FSARRAY: 
        case CasSerializerSupport.TYPE_CLASS_FSLIST:
          return true;
          
        case LowLevelCAS.TYPE_CLASS_INTARRAY:
        case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
        case LowLevelCAS.TYPE_CLASS_STRINGARRAY:
        case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY:
        case LowLevelCAS.TYPE_CLASS_BYTEARRAY:
        case LowLevelCAS.TYPE_CLASS_SHORTARRAY:
        case LowLevelCAS.TYPE_CLASS_LONGARRAY:
        case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY:
        case CasSerializerSupport.TYPE_CLASS_INTLIST:
        case CasSerializerSupport.TYPE_CLASS_FLOATLIST:
        case CasSerializerSupport.TYPE_CLASS_STRINGLIST: 
          // we have refs only if the feature has
          // multipleReferencesAllowed = true
          return cds.tsi.ll_getFeatureForCode(featCode).isMultipleReferencesAllowed();   
        
        default:  // for primitives
          return false;
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
    protected XmlElementName uimaTypeName2XmiElementName(String uimaTypeName) {
      // split uima type name into namespace and short name
      String shortName;
      final int lastDotIndex = uimaTypeName.lastIndexOf('.');
      if (lastDotIndex == -1) { // no namespace
        shortName = uimaTypeName;
      } else {
        shortName = uimaTypeName.substring(lastDotIndex + 1);
      }
      // convert short name to shared string, without interning, reduce GCs
      shortName = cds.getUniqueString(shortName);

      // determine what namespace prefix to use
      String prefix = cds.getNameSpacePrefix(uimaTypeName, uimaTypeName, lastDotIndex);

      return new XmlElementName(uimaTypeName, shortName, cds.getUniqueString(prefix + ':' + shortName));
    }
    
  }

}
