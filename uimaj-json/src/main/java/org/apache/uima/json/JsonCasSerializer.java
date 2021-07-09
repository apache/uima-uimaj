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

package org.apache.uima.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Marker;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CasSerializerSupport;
import org.apache.uima.cas.impl.CasSerializerSupport.CasDocSerializer;
import org.apache.uima.cas.impl.CasSerializerSupport.CasSerializerSupportSerialize;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.MarkerImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.cas.impl.XmiSerializationSharedData;
import org.apache.uima.cas.impl.XmiSerializationSharedData.XmiArrayElement;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.PositiveIntSet;
import org.apache.uima.internal.util.PositiveIntSet_impl;
import org.apache.uima.internal.util.XmlElementName;
import org.apache.uima.internal.util.function.IntConsumer_withIOException;
import org.apache.uima.internal.util.rb_trees.RedBlackTree;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.EmptyList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.NonEmptyFloatList;
import org.apache.uima.jcas.cas.NonEmptyIntegerList;
import org.apache.uima.jcas.cas.NonEmptyStringList;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.json.impl.JsonContentHandlerJacksonWrapper;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;

/**
 * <h2>CAS serializer for JSON formats.</h2>
 * <p>
 * Writes a CAS in a JSON format.
 * </p>
 * 
 * <p>
 * To use,
 * </p>
 * <ul>
 * <li>create an instance of this class,</li>
 * <li>(optionally) configure the instance, and then</li>
 * <li>call serialize on the instance, optionally passing in additional parameters.</li>
 * </ul>
 * 
 * <p>
 * After the 1st 2 steps, the serializer instance may be used for multiple calls (on multiple
 * threads) to the 3rd serialize step, if all calls use the same configuration.
 * </p>
 * 
 * <p>
 * There are "convenience" static serialize methods that do these three steps for common
 * configurations.
 * </p>
 * 
 * <p>
 * Parameters can be configured in this instance (I), and/or as part of the serialize(S) call.
 * </p>
 * 
 * <p>
 * The parameters that can be configured are:
 * </p>
 * <ul>
 * <li>(S) The CAS to serialize
 * <li>(S) where to put the output - an OutputStream, Writer, or File</li>
 * <li>(I,S) a type system - (default null) if supplied, it is used to "filter" types and features
 * that are serialized. If provided, only those that exist in the passed in type system are included
 * in the serialization</li>
 * <li>(I,S) a flag for prettyprinting - default false (no prettyprinting)</li>
 * </ul>
 * 
 * <p>
 * For Json serialization, additional configuration from the Jackson implementation can be
 * configured
 * </p>
 * on 2 associated Jackson instances:
 * <ul>
 * <li>JsonFactory</li>
 * <li>JsonGenerator</li>
 * </ul>
 * using the standard Jackson methods on the associated JsonFactory instance; see the Jackson
 * JsonFactory and JsonGenerator javadocs for details.
 * 
 * <p>
 * These 2 Jackson objects are settable/gettable from an instance of this class. They are created if
 * not supplied by the caller.
 * </p>
 * 
 * <p>
 * Once this instance is configured, the serialize method is called to serialized a CAS to an
 * output.
 * </p>
 * 
 * <p>
 * Instances of this class must be used on only one thread while configuration is being done;
 * afterwards, multiple threads may use the configured instance, to call serialize.
 * </p>
 */
public class JsonCasSerializer {

  private static final SerializedString CONTEXT_NAME = new SerializedString("_context");

  private static final SerializedString TYPE_SYSTEM_NAME = new SerializedString("_type_system");

  private static final SerializedString TYPES_NAME = new SerializedString("_types");

  private static final SerializedString ID_NAME = new SerializedString("_id");
  private static final SerializedString SUB_TYPES_NAME = new SerializedString("_subtypes");

  private static final SerializedString FEATURE_TYPES_NAME = new SerializedString("_feature_types");
  private static final SerializedString FEATURE_REFS_NAME = new SerializedString("_ref");
  private static final SerializedString FEATURE_ARRAY_NAME = new SerializedString("_array");
  private static final SerializedString FEATURE_BYTE_ARRAY_NAME = new SerializedString(
          "_byte_array");

  private static final SerializedString REFERENCED_FSS_NAME = new SerializedString(
          "_referenced_fss");
  private static final SerializedString VIEWS_NAME = new SerializedString("_views");

  private static final SerializedString TYPE_NAME = new SerializedString("_type");

  private static final SerializedString COLLECTION_NAME = new SerializedString("_collection");

  private static final SerializedString DELTA_CAS_NAME = new SerializedString("_delta_cas");

  private static final SerializedString ADDED_MEMBERS_NAME = new SerializedString("added_members");
  private static final SerializedString DELETED_MEMBERS_NAME = new SerializedString(
          "deleted_members");
  private static final SerializedString REINDEXED_MEMBERS_NAME = new SerializedString(
          "reindexed_members");

  /**
   * <p>
   * The serialization can optionally include context information in addition to the feature
   * structures.
   * </p>
   * 
   * <p>
   * This context information is specified, per used-type.
   * </p>
   * 
   * <p>
   * It can be further subdivided into 3 parts:
   * </p>
   * <ol>
   * <li>What their (used) subtypes are. This enables iterating over a type and all of its subtypes,
   * e.g. an iterator over all "Annotations".</li>
   * <li>whether or not to include the map from short type names to their fully qualified
   * equivalents.</li>
   * <li>Information to enable deserialization of some ambiguous values, depending on the range type
   * of a feature
   * </ol>
   * 
   * <p>
   * Some of these may be omitted, if not wanted. This enum allows specifying what to omit.
   * </p>
   *
   */
  public enum JsonContextFormat {
    omitContext, // omit the entire context
    omitSubtypes, omitExpandedTypeNames,
  }

  private final CasSerializerSupport css = new CasSerializerSupport();

  // for testing
  CasSerializerSupport getCss() {
    return css;
  }

  private JsonFactory jsonFactory = null;

  private boolean isDynamicEmbedding = true;
  private boolean isWithContext = true;
  private boolean isWithSubtypes = true;
  private boolean isWithExpandedTypeNames = true;
  private boolean isOmit0Values = false; // https://issues.apache.org/jira/browse/UIMA-4117

  private String typeSystemReference;

  // @formatter:off
  /***********************************************
   * C O N S T R U C T O R S                     *
   ***********************************************/
  // @formatter:on

  /**
   * Creates a new JsonCasSerializer
   */
  public JsonCasSerializer() {
  }

  // @formatter:off
  /**************************************************
   * J S O N                                        *
   **************************************************/

  /****************************************************
   *  Static JSON Serializer methods for convenience  *
   *                                                  *
   *    Note: these are named jsonSerialize           *
   *          The non-static methods                  *
   *                are named serializeJson           *
   ****************************************************/
  // @formatter:on

  /**
   * Serializes a CAS using JSON
   * 
   * @param aCAS
   *          CAS to serialize.
   * @param output
   *          a File, OutputStream or Writer to which to write the XMI document
   * 
   * @throws IOException
   *           if there was an IOException
   */
  public static void jsonSerialize(CAS aCAS, Object output) throws IOException {
    jsonSerialize(aCAS, null, output, false, null, null);
  }

  /**
   * Serializes a CAS to an output (File, OutputStream, XMI stream, or Writer). The supplied
   * typesystem filters the output
   * 
   * @param aCAS
   *          CAS to serialize.
   * @param aTargetTypeSystem
   *          type system used for filtering what gets serialized. Any types or features not in the
   *          target type system will not be serialized. A null value indicates no filtering, that
   *          is, that all types and features will be serialized.
   * @param output
   *          output (File, OutputStream, or Writer) to which to write the JSON document
   * 
   * @throws IOException
   *           if there was an IOException
   */
  public static void jsonSerialize(CAS aCAS, TypeSystem aTargetTypeSystem, Object output)
          throws IOException {
    jsonSerialize(aCAS, aTargetTypeSystem, output, false, null, null);
  }

  /**
   * Serializes a Delta CAS to an output (File, Writer, or OutputStream). This version of this
   * method allows many options to be configured.
   * 
   * 
   * @param aCAS
   *          CAS to serialize.
   * @param aTargetTypeSystem
   *          type system to which the produced XMI will conform. Any types or features not in the
   *          target type system will not be serialized. A null value indicates that all types and
   *          features will be serialized.
   * @param output
   *          File, Writer, or OutputStream to which to write the JSON document
   * @param aPrettyPrint
   *          if true the JSON output will be formatted with newlines and indenting. If false it
   *          will be unformatted.
   * @param aMarker
   *          an optional object used to determine which FeatureStructures and modifications were
   *          created after the mark was set. Used to serialize a Delta CAS consisting of only new
   *          FSs and views and preexisting FSs and Views that have been modified. If null, full
   *          serialization is done. See the JavaDocs for {@link Marker} for details.
   * @param sharedData
   *          optional, used for delta serialization (not yet supported)
   * @throws IOException
   *           if there was an IOException
   */
  public static void jsonSerialize(CAS aCAS, TypeSystem aTargetTypeSystem, Object output,
          boolean aPrettyPrint, Marker aMarker, XmiSerializationSharedData sharedData)
          throws IOException {
    JsonCasSerializer ser = new JsonCasSerializer();
    ser.setFilterTypes((TypeSystemImpl) aTargetTypeSystem);
    ser.setPrettyPrint(aPrettyPrint);
    ser.serialize(aCAS, output, sharedData, aMarker);
  }

  // @formatter:off
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
  // @formatter:on

  /**
   * Serialize a Cas to an Output, using configurations set on this instance. Constructs a
   * JsonContentHandlerJacksonWrapper, using configured JsonFactory and prettyprint settings if any
   * 
   * @param cas
   *          - the CAS to serialize
   * @param output
   *          - where the output goes, an OutputStream, Writer, or File
   * @throws IOException
   *           if there was an IOException
   */
  public void serialize(CAS cas, Object output) throws IOException {
    serialize(cas, output, null, null);
  }

  public void serialize(CAS cas, Object output, XmiSerializationSharedData sharedData,
          Marker marker) throws IOException {
    JsonContentHandlerJacksonWrapper jch;
    try {
      jch = new JsonContentHandlerJacksonWrapper(jsonFactory, output, css.isFormattedOutput);
    } catch (SAXException e) {
      throw new IOException(e);
    }
    serialize(cas, jch, sharedData, marker);
  }

  /**
   * Serialize a Cas to an Output configured in the passed in JsonContentHandlerJacksonWrapper
   * Constructs a new CasDocSerializer instance to do the serialization, configured using this
   * class's Delta marker setting (if any)
   * 
   * @param cas
   *          The CAS to serialize
   * @param jch
   *          the configured content handler
   * @throws IOException
   *           if there was an IOException
   */
  public void serialize(CAS cas, JsonContentHandlerJacksonWrapper jch) throws IOException {
    serialize(cas, jch, null, null);
  }

  public void serialize(CAS cas, JsonContentHandlerJacksonWrapper jch,
          XmiSerializationSharedData sharedData, Marker marker) throws IOException {
    JsonDocSerializer ser = new JsonDocSerializer(jch, ((CASImpl) cas).getBaseCAS(), sharedData,
            (MarkerImpl) marker);
    try {
      ser.cds.needNameSpaces = false;
      ser.cds.serialize();
    } catch (Exception e) {
      if (e instanceof IOException) {
        throw (IOException) e;
      }
      throw new RuntimeException(e);
    }
  }

  // @formatter:off
  /********************************************************
   *   Routines to set/reset configuration                *
   ********************************************************/
  // @formatter:on
  /**
   * set or reset the pretty print flag (default is false)
   * 
   * @param pp
   *          true to do pretty printing of output
   * @return the original instance, possibly updated
   */
  public JsonCasSerializer setPrettyPrint(boolean pp) {
    css.setPrettyPrint(pp);
    return this;
  }

  /**
   * set which JsonFactory instance to use; if null, a new instance is used this can be used to
   * preconfigure the JsonFactory instance
   * 
   * @param jsonFactory
   *          -
   * @return the original instance, possibly updated
   */
  public JsonCasSerializer setJsonFactory(JsonFactory jsonFactory) {
    this.jsonFactory = jsonFactory;
    return this;
  }

  /**
   * pass in a type system to use for filtering what gets serialized; only those types and features
   * which are defined this type system are included.
   * 
   * @param ts
   *          the filter
   * @return the original instance, possibly updated
   */
  public JsonCasSerializer setFilterTypes(TypeSystemImpl ts) {
    css.setFilterTypes(ts);
    return this;
  }

  public JsonCasSerializer setTypeSystemReference(String reference) {
    typeSystemReference = reference;
    return this;
  }

  // not done here, done on serialize call, because typically changes for each call
  // /**
  // * set the Marker to specify delta cas serialization
  // * forces static embedding mode
  // * @param m - the marker
  // * @return the original instance, possibly updated
  // */
  // public JsonCasSerializer setDeltaCas(Marker m, XmiSerializationSharedData sharedData) {
  // css.setDeltaCas(m);
  // setStaticEmbedding(); // delta requires static embedding mode
  // return this;
  // }

  /**
   * set an error handler to receive information about errors
   * 
   * @param eh
   *          the error handler
   * @return the original instance, possibly updated
   */
  public JsonCasSerializer setErrorHandler(ErrorHandler eh) {
    css.setErrorHandler(eh);
    return this;
  }

  /**
   * Sets static embedding mode
   * 
   * @return the original instance, possibly updated
   */
  public JsonCasSerializer setStaticEmbedding() {
    isDynamicEmbedding = false;
    return this;
  }

  /**
   * sets which Json context format to use when serializing
   * 
   * @param format
   *          the format to use for the serialization Specifying the context flag also specifies all
   *          3 subflags Specifying one of the subflags as true sets the context flag to true if it
   *          isn't already
   * @return the original instance, possibly updated
   */
  public JsonCasSerializer setJsonContext(JsonContextFormat format) {
    switch (format) {
      case omitContext:
        isWithContext = false;
        isWithSubtypes = false;
        isWithExpandedTypeNames = false;
        break;

      case omitSubtypes:
        isWithSubtypes = false;
        break;

      case omitExpandedTypeNames:
        isWithExpandedTypeNames = false;
        break;
    }
    return this;
  }

  public JsonCasSerializer setOmit0Values(boolean omitDefaultValues) {
    isOmit0Values = omitDefaultValues;
    return this;
  }

  private static class MapType2Subtypes extends RedBlackTree<IntVector> {
    /**
     * 
     * @param type
     *          main type
     * @param subtype
     *          subtype of main type
     * @return true if added, false if already was there
     */
    boolean addSubtype(int type, int subtype) {
      IntVector iv = get(type);
      if (null == iv) {
        iv = new IntVector();
        iv.add(subtype);
        put(type, iv);
        return true;
      }
      if (iv.contains(subtype)) {
        return false;
      }
      iv.add(subtype);
      return true;
    }
  }

  class JsonDocSerializer extends CasSerializerSupportSerialize {

    private final CasDocSerializer cds;

    private final JsonContentHandlerJacksonWrapper jch;

    private final JsonGenerator jg;

    private final Map<String, SerializedString> serializedStrings = new HashMap<>();

    private final Map<String, XmlElementName> usedTypeName2XmlElementName;

    private final MapType2Subtypes mapType2Subtypes = new MapType2Subtypes();

    private final List<TypeImpl> parentTypesWithNoInstances = new ArrayList<>();

    private int lastEncodedTypeCode;

    private boolean startedReferencedFSs;

    private final boolean isOmitDefaultValues;

    private final boolean isWithContext;

    private final boolean isWithSubtypes;

    private boolean indexId; // true causes fs to be listed as "id" : { ...}, false as "type" : [
                             // {...}

    private boolean isEmbedded = false; // true for embedded FSs, causes _type to be included

    private boolean isEmbeddedFromFsFeature; // used for NL formatting, false if embedded due to
                                             // Array or List

    private boolean startedFeatureTypes;

    private JsonDocSerializer(ContentHandler ch, CASImpl cas, XmiSerializationSharedData sharedData,
            MarkerImpl marker) {
      cds = css.new CasDocSerializer(ch, cas, sharedData, marker, this,
              JsonCasSerializer.this.isDynamicEmbedding);
      this.isOmitDefaultValues = JsonCasSerializer.this.isOmit0Values;
      isWithSubtypes = JsonCasSerializer.this.isWithSubtypes;
      jch = (JsonContentHandlerJacksonWrapper) ch;
      jg = jch.getJsonGenerator();
      isWithContext = JsonCasSerializer.this.isWithContext || isWithSubtypes
              || isWithExpandedTypeNames;
      usedTypeName2XmlElementName = new HashMap<>(cds.tsi.getNumberOfTypes());
    }

    @Override
    protected void initializeNamespaces() {
      if (cds.sharedData != null && (null != cds.sharedData.getOutOfTypeSystemElements()
              || cds.sharedData.hasOutOfTypeSystemArrayElements())) {
        throw new UnsupportedOperationException(
                "Can't do JSON serialization " + "if there are out-of-type-system elements,"
                        + " because there's no type information available (needed for _context)");
      }
    }

    @Override
    protected void writeViews() throws Exception {
      if (!cds.isDelta) {
        return;
      }
      jch.writeNlJustBeforeNext();
      jg.writeFieldName(DELTA_CAS_NAME);
      jg.writeStartObject();

      cds.writeViewsCommons(); // encodes cas.sofaCount + 1 elements
      jg.writeEndObject(); // and end of views property
    }

    @Override
    protected void writeFeatureStructures(int elementCount /* not used */ ) throws Exception {
      jch.withoutNl(); // set up prettyprint mode so this class controls it

      jg.writeStartObject(); // container for (maybe) context, fss (2 parts), and (maybe) delta view
                             // info

      if (isWithContext) {
        serializeJsonLdContext();
      }

      jch.writeNlJustBeforeNext();

      // write the reachable from indexes FS
      indexId = false;

      jg.writeFieldName(VIEWS_NAME);
      jg.writeStartObject();

      final List<TOP>[] byViewByTypeFSs = sortByViewType();

      for (int viewNbr = 1; viewNbr <= byViewByTypeFSs.length; viewNbr++) {
        // viewNbr starts at 1
        lastEncodedTypeCode = -1;
        final List<TOP> fssInView = byViewByTypeFSs[viewNbr - 1];
        final Sofa sofa = cds.getSofa(viewNbr);
        if (sofa == null && fssInView.size() == 0) {
          continue; // skip non-existent initial view with no sofa and no elements
        }
        jch.writeNlJustBeforeNext();
        String viewName = (null == sofa) ? CAS.NAME_DEFAULT_SOFA : sofa.getSofaID();
        jg.writeFieldName(viewName); // view namne
        jg.writeStartObject();
        for (TOP fs : fssInView) {
          cds.encodeFS(fs);
        }
        if (lastEncodedTypeCode != -1) {
          jg.writeEndArray(); // of array of types under a fs
        }
        jg.writeEndObject();
      }

      jg.writeEndObject(); // end of value for _views

      // write the non-embeddable referenced FSs

      indexId = true;
      startedReferencedFSs = false;
      cds.encodeQueued();
      if (startedReferencedFSs) {
        jg.writeEndObject(); // of all referenced FSs
      }

    }

    @Override
    protected void writeEndOfSerialization() throws IOException {
      jg.writeEndObject(); // wrapper of _context and cas
      jg.flush();
    }

    // sort the by-view by-type set
    // previously Serialized
    /**
     * @return the List<TOP>[] returned by cds.indexedFSs, but with each view sorted by type
     */
    private List<TOP>[] sortByViewType() {

      @SuppressWarnings("unchecked")
      final List<TOP>[] r = new List[cds.indexedFSs.length];

      int i = 0;
      for (final List<TOP> fss : cds.indexedFSs) {
        r[i] = (fss == null) ? Collections.EMPTY_LIST : (List<TOP>) ((ArrayList<TOP>) fss).clone();
        r[i++].sort(cds.sortFssByType);
      }
      return r;
    }

    @Override
    protected void writeView(Sofa sofa, Collection<TOP> members) throws IOException {
      jch.writeNlJustBeforeNext();
      String sofaXmiId = (null == sofa) ? "0" : cds.getXmiId(sofa);
      jg.writeArrayFieldStart(sofaXmiId);
      writeViewMembers(members);
      // check for out-of-typesystem members
      if (cds.sharedData != null) {
        List<String> ootsMembers = cds.sharedData.getOutOfTypeSystemViewMembers(sofaXmiId);
        jch.writeNlJustBeforeNext();
        writeViewMembers(ootsMembers);
      }

      jg.writeEndArray();
    }

    private void writeViewForDeltas(SerializedString kind, Collection<TOP> deltaMembers)
            throws IOException {
      jg.writeFieldName(kind);
      jg.writeStartArray();
      writeViewMembers(deltaMembers);
      jg.writeEndArray();
    }

    @Override
    protected void writeView(Sofa sofa, Collection<TOP> added, Collection<TOP> deleted,
            Collection<TOP> reindexed) throws IOException {
      jch.writeNlJustBeforeNext();
      jg.writeFieldName(cds.getXmiId(sofa));
      jg.writeStartObject();
      writeViewForDeltas(ADDED_MEMBERS_NAME, added);
      writeViewForDeltas(DELETED_MEMBERS_NAME, deleted);
      writeViewForDeltas(REINDEXED_MEMBERS_NAME, reindexed);
      jg.writeEndObject();
    }

    private void writeViewMembers(Collection<TOP> members) throws IOException {
      int nextBreak = CasSerializerSupport.PP_ELEMENTS;
      int i = 0;
      for (TOP member : members) {
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

    /*
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
     * <h2>JSON: serialize context info</h2>
     * 
     * <p>
     * The context has several parts.
     * <p>
     * The typeSystemReference is an optional URI to a type system that is written out.
     * <p>
     * The types part is organized by the type hierarchy, starting with the uima.cas.TOP type. There
     * is an entry for each type which has 1 or more serailized instances, and also for all
     * supertypes of those types. The entry is a JSON key-value pair "short-type-name" : {...}.
     * </p>
     * 
     * <p>
     * The information for each type has 3 sections:
     * </p>
     * <ol>
     * <li>_subtypes - a JSON map of key-value pairs, keyed by the short type-name of used subtypes
     * of this type. If this type has no used subtypes, this element is omitted. The value is an
     * instance of this structure, for that type.</li>
     * 
     * <li>_id - the fully qualified UIMA type name</li>
     * 
     * <li>@featureTypes - a map with keys being specific features of the type that need extra
     * information about their contents, and the value being that extra information.</li>
     * </ol>
     * 
     * RANGE_IDs specify the type of the value of a feature. There are currently 2 kinds:
     * 
     * <ul>
     * <li>"@featureByteArray" - indicates the string value should be decoded as a base64 binary
     * encoded byte array</li>
     * <li>"{ "@featureRef" : "short_type_name" } - indicates the number or array of numbers should
     * be interpreted as a reference to a FS having this number (or array of numbers) as its id(s).
     * 0 is interpreted as a null reference. The type of the FS being referred to is of type
     * "short_type_name" or a subtype.</li>
     * </ul>
     * 
     * @throws IOException
     */

    private void serializeJsonLdContext() throws IOException {
      jg.writeFieldName(CONTEXT_NAME);
      jg.writeStartObject();

      if (typeSystemReference != null) {
        jch.writeNlJustBeforeNext();
        jg.writeFieldName(TYPE_SYSTEM_NAME);
        jg.writeString(typeSystemReference);
      }

      collectUsedSubtypes();

      jch.writeNlJustBeforeNext();
      jg.writeFieldName(TYPES_NAME);
      jg.writeStartObject();

      for (TypeImpl ti : cds.getSortedUsedTypes()) {
        jch.writeNlJustBeforeNext();
        jg.writeFieldName(getSerializedTypeName(ti));
        jg.writeStartObject();
        if (isWithExpandedTypeNames) {
          jg.writeFieldName(ID_NAME); // form for using SerializedString
          jg.writeString(ti.getName());
        }
        addJsonFeatContext(ti);
        if (isWithSubtypes) {
          addJsonSubtypes(ti);
        }
        jg.writeEndObject(); // end of one type
      }

      // write out contexts for types in the supertype chain which have no instances
      for (final TypeImpl ti : parentTypesWithNoInstances) {
        jch.writeNlJustBeforeNext();
        jg.writeFieldName(getSerializedTypeName(ti));
        jg.writeStartObject();
        XmlElementName xe = cds.typeCode2namespaceNames[ti.getCode()];

        if (isWithExpandedTypeNames) {
          jg.writeFieldName(ID_NAME); // form for using SerializedString
          jg.writeString(xe.nsUri);
        }

        addJsonFeatContext(ti);
        if (isWithSubtypes) {
          addJsonSubtypes(ti);
        }
        jg.writeEndObject(); // end of one type

      }

      jg.writeEndObject(); // end of _types

      jg.writeEndObject(); // end of _context
    }

    /**
     * _feature_types : { "featName" : "_ref" or "_byte_array, ... }
     * 
     * @param type
     *          the type for which to generate the feature context info
     * @throws IOException
     */
    private void addJsonFeatContext(TypeImpl type) throws IOException {
      final FeatureImpl[] feats = type.getFeatureImpls();
      startedFeatureTypes = false;

      for (FeatureImpl feat : feats) {
        final int fsClass = CasSerializerSupport.classifyType(feat.getRangeImpl());
        SerializedString featKind = featureTypeLabel(fsClass);
        if (null != featKind) {
          maybeDoStartFeatureTypes();
          jg.writeFieldName(getSerializedString(feat.getShortName()));
          jg.writeString(featKind);
        }
      }
      if (startedFeatureTypes) {
        jg.writeEndObject();
      }
    }

    private void maybeDoStartFeatureTypes() throws IOException {
      if (!startedFeatureTypes) {
        jch.writeNlJustBeforeNext();
        jg.writeFieldName(FEATURE_TYPES_NAME);
        jg.writeStartObject();
        startedFeatureTypes = true;
      }
    }

    private SerializedString getShortFeatureName(FeatureImpl feat) {
      return getSerializedString(feat.getShortName());
    }

    /**
     * Add subtype information for used types limited to used subtypes
     * 
     * @throws IOException
     */
    private void addJsonSubtypes(TypeImpl ti) throws IOException {
      IntVector iv = mapType2Subtypes.get(ti.getCode());
      if (null != iv && iv.size() > 0) {
        jch.writeNlJustBeforeNext();
        jg.writeFieldName(SUB_TYPES_NAME);
        jg.writeStartArray();

        TypeSystemImpl tsi = ti.getTypeSystem();
        for (int typeCode : iv.toArray()) {
          jg.writeString(getSerializedTypeName(tsi.getTypeForCode(typeCode)));
        }
        jg.writeEndArray();
      }
    }

    private void collectUsedSubtypes() {
      final TypeImpl[] tiArray = cds.getSortedUsedTypes();

      for (TypeImpl ti : tiArray) { // all used types
        int subtypeCode = ti.getCode();

        // loop up the super chain for this type,
        // add parent -> subtype entries (until try to add one that's already there)

        for (TypeImpl parent = (TypeImpl) ti
                .getSuperType(); parent != null; parent = (TypeImpl) parent.getSuperType()) {
          final int parentCode = parent.getCode();
          // next comparator must match the one used for sorting the tiArray
          // https://issues.apache.org/jira/browse/UIMA-5171
          // if parent not contained in tiArray
          if (Arrays.binarySearch(tiArray, parent,
                  CasSerializerSupport.COMPARATOR_SHORT_TYPENAME) < 0) {
            if (!parentTypesWithNoInstances.contains(parent)) {
              parentTypesWithNoInstances.add(parent);
            }
          }
          boolean wasAdded = mapType2Subtypes.addSubtype(parentCode, subtypeCode);
          if (!wasAdded) {
            break;
          }
          subtypeCode = parentCode;
        }
      }
    }

    private SerializedString getSerializedTypeName(TypeImpl ti) {
      XmlElementName xe = cds.typeCode2namespaceNames[ti.getCode()];
      if (null == xe) {
        // happens for supertypes which have no instantiations
        String typeName = ti.getName();
        xe = uimaTypeName2XmiElementName(typeName);
        checkForNameCollision(xe);
        cds.typeCode2namespaceNames[ti.getCode()] = xe;
      }
      return getSerializedString(xe.qName);
    }

    private SerializedString getSerializedString(String s) {
      SerializedString ss = serializedStrings.get(s);
      if (ss == null) {
        ss = new SerializedString(s);
        serializedStrings.put(s, ss);
      }
      return ss;
    }

    /*
     * keep map from short type name to XmlElementName (full name, namespace, etc) This map starts
     * out empty first use of type puts entry in first use of type with different full name adds
     * namespace to both
     */
    @Override
    protected void checkForNameCollision(XmlElementName xmlElementName) {
      XmlElementName xel = usedTypeName2XmlElementName.get(xmlElementName.localName);
      if (xel != null) {
        if (xel.nsUri.equals(xmlElementName.nsUri)) { // nsUri is the fully qualified name
          return; // don't need name spaces yet, or have already added them for this item
        } else {
          addNameSpace(xel);
          addNameSpace(xmlElementName);
          // usedTypeName2XmlElementName.clear(); // not needed anymore
          return;
        }
      }
      usedTypeName2XmlElementName.put(xmlElementName.localName, xmlElementName);
      return;
    }

    @Override
    protected boolean writeFsStart(TOP fs, int typeCode) throws IOException {
      if (isEmbedded) {
        if (!isEmbeddedFromFsFeature) {
          jch.writeNlJustBeforeNext(); // if from feature, already did nl
        }
        jg.writeStartObject();
      } else if (indexId) {
        if (!startedReferencedFSs) {
          jch.writeNlJustBeforeNext();
          jg.writeFieldName(REFERENCED_FSS_NAME);
          jg.writeStartObject();
          startedReferencedFSs = true;
        }
        jch.writeNlJustBeforeNext();
        jg.writeFieldName(cds.getXmiId(fs));
        jg.writeStartObject(); // start of feat : value
      } else { // fs's as arrays under typeName
        if (typeCode != lastEncodedTypeCode) {
          if (lastEncodedTypeCode != -1) {
            // close off previous Array
            jg.writeEndArray();
          }
          lastEncodedTypeCode = typeCode;
          jch.writeNlJustBeforeNext();
          jg.writeFieldName(getSerializedTypeName(fs._getTypeImpl()));
          jg.writeStartArray();
        }
        // if we're not going to write the actual FS here,
        // and are just going to write the ref,
        // skip the start object
        if (!cds.isDynamicMultiRef || !cds.multiRefFSs.contains(fs)) {
          jch.writeNlJustBeforeNext();
          jg.writeStartObject(); // start of feat : value
        }
      }
      return indexId;
    }

    @Override
    protected void writeFsRef(TOP fs) throws Exception {
      jg.writeNumber(cds.getXmiIdAsInt(fs));
    }

    // private void maybeWriteIdFeat(int addr) throws IOException {
    // if (!omitId) {
    // jg.writeFieldName(ID_NAME);
    // jg.writeNumber(cds.getXmiIdAsInt(addr));
    // }
    // }

    private void maybeWriteTypeFeat(TypeImpl ti) throws IOException {
      if (indexId || isEmbedded) {
        jg.writeFieldName(TYPE_NAME);
        jg.writeString(getSerializedTypeName(ti));
      }
    }

    @Override
    protected void writeFs(TOP fs, int typeCode) throws IOException {
      writeFsOrLists(fs, fs._getTypeImpl(), false);
    }

    @Override
    protected void writeListsAsIndividualFSs(TOP fs, int typeCode) throws IOException {
      writeFsOrLists(fs, fs._getTypeImpl(), true);
    }

    private void writeFsOrLists(TOP fs, TypeImpl ti, boolean isListAsFSs) throws IOException {
      final FeatureImpl[] feats = ti.getFeatureImpls();

      // maybeWriteIdFeat(addr);
      maybeWriteTypeFeat(ti);

      for (final FeatureImpl feat : feats) {

        if (cds.isFiltering) {
          // skip features that aren't in the target type system
          String fullFeatName = feat.getName();
          if (cds.filterTypeSystem_inner.getFeatureByFullName(fullFeatName) == null) {
            continue;
          }
        }

        // final int featAddr = addr + cds.cas.getAdjustedFeatureOffset(featCode);
        // final int featValRaw = cds.cas.getHeapValue(featAddr);
        final int featureClass = CasSerializerSupport.classifyType(feat.getRangeImpl());
        final SerializedString shortName = getSerializedString(feat.getShortName());

        switch (featureClass) {

          case LowLevelCAS.TYPE_CLASS_BYTE:
            writeNumeric(feat, fs._getByteValueNc(feat));
            break;
          case LowLevelCAS.TYPE_CLASS_SHORT:
            writeNumeric(feat, fs._getShortValueNc(feat));
            break;
          case LowLevelCAS.TYPE_CLASS_INT:
            writeNumeric(feat, fs._getIntValueNc(feat));
            break;
          case LowLevelCAS.TYPE_CLASS_LONG:
            writeNumeric(feat, fs._getLongValueNc(feat));
            break;

          case LowLevelCAS.TYPE_CLASS_FS: {
            TOP ref = fs._getFeatureValueNc(feat);
            if (ref == null /* && isOmitDefaultValues */) {
              continue;
            }
            writeFsOrRef(ref, feat); // writes nl before embedded fs
            break;
          }

          case LowLevelCAS.TYPE_CLASS_FLOAT:
            final float floatVal = fs._getFloatValueNc(feat);
            if (floatVal == 0.F && isOmitDefaultValues) {
              continue;
            }
            jg.writeFieldName(shortName);
            jg.writeNumber(floatVal);
            break;

          case LowLevelCAS.TYPE_CLASS_DOUBLE:
            final double doubleVal = fs._getDoubleValueNc(feat);
            if (doubleVal == 0L && isOmitDefaultValues) {
              continue;
            }
            jg.writeFieldName(shortName);
            jg.writeNumber(doubleVal);
            break;

          case LowLevelCAS.TYPE_CLASS_BOOLEAN:
            jg.writeFieldName(shortName);
            jg.writeBoolean(fs._getBooleanValueNc(feat));
            break;

          case LowLevelCAS.TYPE_CLASS_STRING: {
            String s = fs._getStringValueNc(feat);
            if (s == null /* && isOmitDefaultValues */) {
              continue;
            }
            jg.writeFieldName(shortName);
            jg.writeString(s);
            break;
          }

          case LowLevelCAS.TYPE_CLASS_INTARRAY:
          case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
          case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY:
          case LowLevelCAS.TYPE_CLASS_BYTEARRAY:
          case LowLevelCAS.TYPE_CLASS_SHORTARRAY:
          case LowLevelCAS.TYPE_CLASS_LONGARRAY:
          case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY:
          case LowLevelCAS.TYPE_CLASS_STRINGARRAY:
          case LowLevelCAS.TYPE_CLASS_FSARRAY:
            writeArray(fs, feat, featureClass);
            break;

          case CasSerializerSupport.TYPE_CLASS_INTLIST:
          case CasSerializerSupport.TYPE_CLASS_FLOATLIST:
          case CasSerializerSupport.TYPE_CLASS_STRINGLIST:
          case CasSerializerSupport.TYPE_CLASS_FSLIST:
            writeList(fs, feat, featureClass, isListAsFSs);
            break;
          default:
            Misc.internalError();
        } // end of switch
      } // end of loop over all features
    }

    private void writeNumeric(FeatureImpl fi, long v) throws IOException {
      if (v == 0 && isOmitDefaultValues) {
        return;
      }
      jg.writeFieldName(getShortFeatureName(fi));
      jg.writeNumber(v);
    }

    private void writeArray(TOP fs, FeatureImpl fi, int featureClass) throws IOException {
      assert (fs != null);
      TOP array = fs._getFeatureValueNc(fi);
      if (array == null) {
        return;
      }

      jg.writeFieldName(getShortFeatureName(fi));
      if (isDynamicOrStaticMultiRef(fi, array)) {
        jg.writeNumber(cds.getXmiIdAsInt(array));
      } else {
        writeJsonArrayValues(array, featureClass);
      }
    }

    private void writeList(TOP fs, FeatureImpl fi, int featureClass, boolean isListAsFSs)
            throws IOException {
      assert (fs != null);
      TOP list = fs._getFeatureValueNc(fi);
      if (list == null) {
        return;
      }

      jg.writeFieldName(getShortFeatureName(fi));
      if (isDynamicOrStaticMultiRef(fi, list, isListAsFSs)) {
        jg.writeNumber(cds.getXmiIdAsInt(list));
      } else {
        writeJsonListValues(list);
      }
    }

    // @formatter:off
    /**
     * for arrays and lists,
     * recursively write one FS, 
     *    as actual FS, 
     *    if dynamic embedding and single ref
     *  OR, just write the reference id
     *  If trying to write the null FS (due to filtering for instance), write 0
     * @param addr
     * @throws IOException
     */
    // @formatter:on
    private void writeFsOrRef(TOP fs) throws IOException {
      if (fs == null || !cds.isDynamicMultiRef || cds.multiRefFSs.contains(fs)) {
        jg.writeNumber(cds.getXmiIdAsInt(fs));
      } else {
        isEmbeddedFromFsFeature = false;
        writeEmbeddedFs(fs);
      }
    }

    private void writeEmbeddedFs(TOP fs) throws IOException {
      boolean savedEmbedded = isEmbedded;
      try {
        isEmbedded = true;
        cds.encodeFS(fs);
      } catch (Exception e) {
        if (e instanceof IOException) {
          throw (IOException) e;
        }
        throw new RuntimeException(e);
      } finally {
        isEmbedded = savedEmbedded;
      } // embed
    }

    private void writeFsOrRef(TOP fs, FeatureImpl fi) throws IOException {
      if (fs == null || !cds.isDynamicMultiRef || cds.multiRefFSs.contains(fs)) {
        jg.writeFieldName(getShortFeatureName(fi));
        jg.writeNumber(cds.getXmiIdAsInt(fs));
      } else {
        jch.writeNlJustBeforeNext();
        jg.writeFieldName(getShortFeatureName(fi));
        isEmbeddedFromFsFeature = true;
        // Use cases: can write embed, which has embed, which has non-embed
        // once hit non-embed, this flag would be turned off,
        // But it's only tested at the beginning of writeEmbeddedFs, so subsequent fields reset this
        // This flag only used to control new lines for embedded case
        writeEmbeddedFs(fs);
        isEmbeddedFromFsFeature = false; // restore default
      }
    }

    /**
     * Write FSArrays
     */
    @Override
    protected void writeArrays(TOP fs, int typeCode, int typeClass) throws IOException {
      // maybeWriteIdFeat(addr);
      maybeWriteTypeFeat(fs._getTypeImpl());

      jg.writeFieldName(COLLECTION_NAME);
      writeJsonArrayValues(fs, typeClass);
    }

    @Override
    protected void writeEndOfIndividualFs() throws IOException {
      jg.writeEndObject();
    }

    // writes a set of values in a JSON array
    // or null if the reference to the UIMA array is actually null
    // 0 length arrays are written as []
    // Note: FSs can be embedded for FS Arrays
    private void writeJsonArrayValues(TOP array, int arrayType) throws IOException {
      if (array == null) {
        jg.writeNull();
        return;
      }

      cds.visited_not_yet_written.remove(array);
      CommonArrayFS ca = (CommonArrayFS) array;
      final int array_size = ca.size();

      if (arrayType == LowLevelCAS.TYPE_CLASS_BYTEARRAY) {
        // special case for byte arrays:
        // serialize using standard JACKSON/JSON binary serialization
        // (doing extra copy to avoid figuring out the impl details)
        ByteArray ba = (ByteArray) array;
        jg.writeBinary(ba._getTheArray());

      } else {
        jg.writeStartArray();
        // int pos = cds.cas.getArrayStartAddress(addr);

        switch (arrayType) {
          case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY: {
            boolean[] a = ((BooleanArray) array)._getTheArray();
            writeArrayElements(array_size, i -> jg.writeBoolean(a[i]));
            break;
          }
          case LowLevelCAS.TYPE_CLASS_BYTEARRAY: {
            ByteArray ba = (ByteArray) array;
            jg.writeBinary(ba._getTheArray());
            break;
          }
          case LowLevelCAS.TYPE_CLASS_SHORTARRAY: {
            short[] a = ((ShortArray) array)._getTheArray();
            writeArrayElements(array_size, i -> jg.writeNumber(a[i]));
            break;
          }
          case LowLevelCAS.TYPE_CLASS_INTARRAY: {
            int[] a = ((IntegerArray) array)._getTheArray();
            writeArrayElements(array_size, i -> jg.writeNumber(a[i]));
            break;
          }
          case LowLevelCAS.TYPE_CLASS_LONGARRAY: {
            long[] a = ((LongArray) array)._getTheArray();
            writeArrayElements(array_size, i -> jg.writeNumber(a[i]));
            break;
          }
          case LowLevelCAS.TYPE_CLASS_FLOATARRAY: {
            float[] a = ((FloatArray) array)._getTheArray();
            writeArrayElements(array_size, i -> jg.writeNumber(a[i]));
            break;
          }
          case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY: {
            double[] a = ((DoubleArray) array)._getTheArray();
            writeArrayElements(array_size, i -> jg.writeNumber(a[i]));
            break;
          }
          case LowLevelCAS.TYPE_CLASS_STRINGARRAY: {
            String[] a = ((StringArray) array)._getTheArray();
            writeArrayElements(array_size, i -> jg.writeString(a[i]));
            break;
          }
          case LowLevelCAS.TYPE_CLASS_FSARRAY:
            writeFSArray(array, array_size);
            break;
          default:
            Misc.internalError();
        } // end of switch

        jg.writeEndArray();
      }
    }

    private void writeArrayElements(final int size, IntConsumer_withIOException ic)
            throws IOException {
      for (int i = 0; i < size; i++) {
        ic.accept(i);
      }
    }

    private void writeFSArray(TOP array, int array_size) throws NumberFormatException, IOException {
      FSArray fsArray = (FSArray) array;

      List<XmiArrayElement> ootsArrayElementsList = cds.sharedData == null ? null
              : cds.sharedData.getOutOfTypeSystemArrayElements(fsArray);
      int ootsIndex = 0;
      TOP[] fsItems = fsArray._getTheArray();

      for (int j = 0; j < array_size; j++) { // j used to id the oots things
        TOP fsItem = fsItems[j]; // int heapValue = cds.cas.getHeapValue(pos++);

        if (fsItem == null) {
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
            String typeName = fsItem._getTypeImpl().getName();
            if (cds.filterTypeSystem_inner.getType(typeName) == null) {
              fsItem = null;
            }
          }
          writeFsOrRef(fsItem); // allow embedding in array
        }
      } // end of loop over all refs in FS array
    }

    // a null ref is written as null
    // an empty list is written as []
    /**
     * Only called if no sharing of list nodes exists (except for non-dynamic case) Only called for
     * list nodes referred to by Feature value slots in some FS.
     * 
     * @param curNode
     *          the address of the start of the list
     * @throws IOException
     */
    private void writeJsonListValues(TOP curNode) throws IOException {
      if (curNode == null) {
        Misc.internalError();
      }

      final PositiveIntSet visited = new PositiveIntSet_impl();

      jg.writeStartArray();
      FeatureStructure nextNode = null;
      while (curNode != null) {

        cds.visited_not_yet_written.remove(curNode);
        if (curNode instanceof EmptyList) {
          break; // would be the end element. a 0 is also treated as an end element
        }

        if (!visited.add(curNode._id())) {
          break; // loop detected, stop. no error report here, would be reported earlier during
                 // enqueue
        }

        // final int val = cds.cas.getHeapValue(curNode +
        // cds.cas.getAdjustedFeatureOffset(headFeat));

        if (curNode instanceof NonEmptyStringList) {
          NonEmptyStringList l = (NonEmptyStringList) curNode;
          jg.writeString(l.getHead());
          nextNode = l.getCommonTail();
        } else if (curNode instanceof NonEmptyFloatList) {
          NonEmptyFloatList l = (NonEmptyFloatList) curNode;
          jg.writeNumber(l.getHead());
          nextNode = l.getCommonTail();
        } else if (curNode instanceof NonEmptyFSList) {
          NonEmptyFSList l = (NonEmptyFSList) curNode;
          writeFsOrRef(l); // maybe embed
          nextNode = l.getCommonTail();
        } else { // for ints
          NonEmptyIntegerList l = (NonEmptyIntegerList) curNode;
          jg.writeNumber(l.getHead());
          nextNode = l.getCommonTail();
        }
        curNode = (TOP) nextNode;
      }
      jg.writeEndArray();
    }

    /**
     * Return null or a string representing the type of the feature
     * 
     * 
     * @param fsClass
     *          the class of the feature
     * @param featCode
     *          the feature code
     * @return _ref, _array, _byte_array, or null
     */

    private SerializedString featureTypeLabel(int fsClass) {
      switch (fsClass) {
        case LowLevelCAS.TYPE_CLASS_FS:
        case LowLevelCAS.TYPE_CLASS_FSARRAY:
        case CasSerializerSupport.TYPE_CLASS_FSLIST:
          return FEATURE_REFS_NAME;

        case LowLevelCAS.TYPE_CLASS_INTARRAY:
        case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
        case LowLevelCAS.TYPE_CLASS_STRINGARRAY:
        case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY:
        case LowLevelCAS.TYPE_CLASS_SHORTARRAY:
        case LowLevelCAS.TYPE_CLASS_LONGARRAY:
        case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY:
        case CasSerializerSupport.TYPE_CLASS_INTLIST:
        case CasSerializerSupport.TYPE_CLASS_FLOATLIST:
        case CasSerializerSupport.TYPE_CLASS_STRINGLIST:
          // we have refs only if the feature has
          // multipleReferencesAllowed = true
          return FEATURE_ARRAY_NAME;

        case LowLevelCAS.TYPE_CLASS_BYTEARRAY:
          return FEATURE_BYTE_ARRAY_NAME;

        default: // for primitives
          return null;
      }
    }

    /**
     * Converts a UIMA-style dotted type name to the element name that should be used in the
     * serialization. The XMI element name consists of three parts - the Namespace URI, the Local
     * Name, and the QName (qualified name).
     * 
     * @param uimaTypeName
     *          a UIMA-style dotted type name
     * @return a data structure holding the three components of the XML element name
     */
    @Override
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

      return new XmlElementName(uimaTypeName, shortName, shortName); // use short name for qname
                                                                     // until namespaces needed
    }

    /**
     * Called to generate a new namespace prefix and add it to this element - due to a collision
     * 
     * @param xmlElementName
     */
    @Override
    protected void addNameSpace(XmlElementName xmlElementName) {
      if (xmlElementName.qName.equals(xmlElementName.localName)) { // may have already had namespace
                                                                   // added
        // split uima type name into namespace and short name
        String uimaTypeName = xmlElementName.nsUri;
        String shortName = xmlElementName.localName;
        final int lastDotIndex = uimaTypeName.lastIndexOf('.');

        // determine what namespace prefix to use
        String prefix = cds.getNameSpacePrefix(uimaTypeName, uimaTypeName, lastDotIndex);
        xmlElementName.qName = cds.getUniqueString(prefix + ':' + shortName);
      }
    }

    private boolean isDynamicOrStaticMultiRef(FeatureImpl fi, TOP fs) {
      return (!cds.isDynamicMultiRef) ? cds.isStaticMultiRef(fi) : cds.multiRefFSs.contains(fs);
    }

    private boolean isDynamicOrStaticMultiRef(FeatureImpl fi, TOP fs, boolean isListAsFSs) {
      return (!cds.isDynamicMultiRef) ? (isListAsFSs || cds.isStaticMultiRef(fi))
              : cds.multiRefFSs.contains(fs);
    }

  }

}
