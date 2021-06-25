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
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaSerializable;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.XmiSerializationSharedData.NameMultiValue;
import org.apache.uima.cas.impl.XmiSerializationSharedData.OotsElementData;
import org.apache.uima.internal.util.I18nUtil;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.internal.util.XmlAttribute;
import org.apache.uima.internal.util.XmlElementName;
import org.apache.uima.internal.util.XmlElementNameAndContents;
import org.apache.uima.internal.util.function.Runnable_withSaxException;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.CommonList;
import org.apache.uima.jcas.cas.CommonPrimitiveArray;
import org.apache.uima.jcas.cas.EmptyList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.FloatList;
import org.apache.uima.jcas.cas.IntegerList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.NonEmptyFloatList;
import org.apache.uima.jcas.cas.NonEmptyIntegerList;
import org.apache.uima.jcas.cas.NonEmptyList;
import org.apache.uima.jcas.cas.NonEmptyStringList;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.util.impl.Constants;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XMI CAS deserializer. Used to read in a CAS from XML Metadata Interchange (XMI) format.
 */
public class XmiCasDeserializer {

  private final static boolean IS_NEW_FS = true;
  private final static boolean IS_EXISTING_FS = false;

  // SofaFS type
  private final static int sofaTypeCode = TypeSystemConstants.sofaTypeCode;

  private final static Pattern whiteSpace = Pattern.compile("\\s+");

  private static final String ID_ATTR_NAME = "xmi:id";

  public class XmiCasDeserializerHandler extends DefaultHandler {
    // ///////////////////////////////////////////////////////////////////////
    // Internal states for the parser.

    // Expect the start of the XML document.
    private static final int DOC_STATE = 0;

    // At the top level. Expect a FS, or document text element, or the end of the
    // XML input.
    private static final int FS_STATE = 1;

    // Inside a FS. Expect features, or the end of the FS.
    private static final int FEAT_STATE = 2;

    // Inside a feature element. We expect the feature value.
    private static final int FEAT_CONTENT_STATE = 3;

    // Inside an element with the XMI namespace - indicating content that's
    // not part of the typesystem and should be ignored.
    private static final int IGNORING_XMI_ELEMENTS_STATE = 4;

    // Inside a reference feature element (e.g. <feat href="#1").
    // We expect no content, just the end of the element.
    private static final int REF_FEAT_STATE = 5;

    // End parser states.
    // ///////////////////////////////////////////////////////////////////////

    // For error message printing, if the Locator object can't provide source
    // of XML input.
    private static final String unknownXMLSource = "<unknown>";

    // SAX locator. Used for error message generation.
    private Locator locator;

    // The CAS we're filling.
    private CASImpl casBeingFilled;

    // What we expect next.
    // @formatter:off
    /**
     * Next expected content
     *   Values: (*) means valid state for "startElement"
     *     * DOC_STATE - start of the XML document
     *     * FS_STATE - start of a Feature Structure, or document text element, or the end of the XML input.
     *     * FEAT_STATE - features encoded as sub-elements, or end of the Feature Structure
     *     FEAT_CONTENT_STATE - the feature value for a feature
     *                          called via "characters" callback
     *                          accumulates chars to "buffer"
     *     * IGNORING_XMI_ELEMENTS_STATE - 
     *     REF_FEAT_STATE - inside <feat href= "#1"). expect only end of element
     */
    // @formatter:on
    private int state;

    // StringBuffer to accumulate text.
    private StringBuilder buffer;

    // The most recently created FS. Needed for embedded feature values.
    private TOP currentFs;

    // The type of the most recently created FS. Needed for arrays, also
    // useful for embedded feature values.
    private TypeImpl currentType;

    // the ID and values of arrays are stored on startElement, then used on
    // endElement to actually create the array. This is because in the case of
    // String arrays serialized with the values as child elements, we can't create
    // the array until we've seen all of the child elements.
    private int currentArrayId;

    private List<String> currentArrayElements;

    // Used for keeping track of multi-valued features read from subelements.
    // Keys are feature names, values are ArrayLists of strings,
    // where each String is one of the values to be assigned to the feature.
    private Map<String, ArrayList<String>> multiValuedFeatures = new TreeMap<>();

    // Store IndexRepositories in a vector;
    private List<FSIndexRepository> indexRepositories;

    // and views too
    private List<CAS> views;

    // // utilities for handling CAS list types
    // private ListUtils listUtils;

    // true if unknown types should be ignored; false if they should cause an error
    boolean lenient;

    // number of oustanding startElement events that we are ignoring
    // we add 1 when an ignored element starts and subtract 1 when an ignored
    // element ends
    private int ignoreDepth = 0;

    // map from namespace prefixes to URIs. Allows namespace resolution even
    // with a non-namespace-enabled SAX parser.
    private Map<String, String> nsPrefixToUriMap = new HashMap<>();

    // container for data shared between the XmiCasSerialier and
    // XmiDeserializer, to support things such as consistency of IDs across
    // multiple serializations. This is also where the map from xmi:id to
    // FS address is stored.
    private XmiSerializationSharedData sharedData;

    // number of Sofas found so far
    private int nextSofaNum;

    // used for merging multiple XMI CASes into one CAS object.
    private int mergePoint;

    // Current out-of-typesystem element, if any
    private OotsElementData outOfTypeSystemElement = null;

    /**
     * This is a list of deferred FSs to be processed. It is added to when a FS which is
     * <ul>
     * <li>a subtype of AnnotationBase</li>
     * <li>has its Sofa ref to some XmiId which is unknown at the time</li>
     * </ul>
     * is encountered. These FSs are processed later, at the end of FS processing, when all Sofas
     * have been deserialized.
     * 
     * It is not out-of-type-system data, but is reusing that data structure - sorry for the
     * confusion
     * 
     * This element is set non-null only when this mode is enabled.
     */
    private List<OotsElementData> deferredFSs = null;

    private OotsElementData deferredFsElement = null;

    /** normally false; set true when processing deferred FSs */
    private boolean processingDeferredFSs = false;

    /** normally false, set true when processing deferred FSs child elements */
    private boolean isDoingDeferredChildElements = false;

    /**
     * local map from xmi:id to FS address, used when merging multiple XMI CASes into one CAS
     * object.
     */
    private Map<Integer, TOP> localXmiIdToFs = new HashMap<>();

    // if mergepoint is set, are preexisting FS allowed, disallowed or ignored.
    AllowPreexistingFS allowPreexistingFS;

    // When deserializing delta CAS preexisting FS, keep track of features that
    // have been deserialized. This is then compared to the all features for the
    // type and features that are not in the xmi are set to null.
    IntVector featsSeen = null;

    // set this flag if preexisting FS is encountered when deserializing
    // delta cas View referencing disallowed preexisting FS member.
    // The preexisting members are ignored and deserialization allowed
    // to complete so that the CAS being filled is not corrupted.
    // An exception is thrown at the end.
    // NOTE: Since preexisting FSs are serialized first, when deserializing
    // of delta CAS with a disallowed preexisting FS, the error will be
    // caught and reported before any updates are made to the CAS being filled.

    boolean disallowedViewMemberEncountered;

    /**
     * a list by view of FSs to be added to the indexes
     */
    final private DeferredIndexUpdates toBeAdded = new DeferredIndexUpdates();
    /**
     * a list by view of FSs to be removed from the indexes
     */
    final private DeferredIndexUpdates toBeRemoved = new DeferredIndexUpdates();

    /**
     * Deferred Set of feature value assignments to do after all FSs are deserialized,
     */
    final private List<Runnable_withSaxException> fixupToDos = new ArrayList<>();

    final private List<Runnable> uimaSerializableFixups = new ArrayList<>();

    /**
     * Creates a SAX handler used for deserializing an XMI CAS.
     * 
     * @param aCAS
     *          CAS to deserialize into
     * @param lenient
     *          if true, unknown types/features result in an exception. If false, unknown
     *          types/features are ignored.
     * @param sharedData
     *          data structure used to allow the XmiCasSerializer and XmiCasDeserializer to share
     *          information.
     * @param mergePoint
     *          used to support merging multiple XMI CASes. If the mergePoint is negative, "normal"
     *          deserialization will be done, meaning the target CAS will be reset and the entire
     *          XMI content will be deserialized. If the mergePoint is nonnegative (including 0),
     *          the target CAS will not be reset, and only Feature Structures whose xmi:id is
     *          strictly greater than the mergePoint value will be deserialized.
     */
    private XmiCasDeserializerHandler(CASImpl aCAS, boolean lenient,
            XmiSerializationSharedData sharedData, int mergePoint,
            AllowPreexistingFS allowPreexistingFS) {
      this.casBeingFilled = aCAS.getBaseCAS();
      this.lenient = lenient;
      this.sharedData = sharedData != null ? sharedData : new XmiSerializationSharedData();
      this.mergePoint = mergePoint;
      this.allowPreexistingFS = allowPreexistingFS;
      this.featsSeen = null;
      this.disallowedViewMemberEncountered = false;
      if (mergePoint < 0) {
        // If not merging, reset the CAS.
        // Necessary to get Sofas to work properly.
        casBeingFilled.resetNoQuestions();

        // clear ID mappings stored in the SharedData (from previous deserializations)
        this.sharedData.clearIdMap();
        // new Sofas start at 2
        this.nextSofaNum = 2;
      } else {
        this.nextSofaNum = this.casBeingFilled.getViewCount() + 1;
      }
      this.buffer = new StringBuilder();
      this.indexRepositories = new ArrayList<>();
      this.views = new ArrayList<>();
      indexRepositories.add(this.casBeingFilled.getBaseIndexRepository());
      // There should always be another index for the Initial View
      indexRepositories
              .add(this.casBeingFilled.getView(CAS.NAME_DEFAULT_SOFA).getIndexRepository());
      // add an entry to indexRepositories for each Sofa in the CAS (which can only happen if
      // a mergePoint was specified)
      FSIterator<Sofa> sofaIter = this.casBeingFilled.getSofaIterator();
      while (sofaIter.hasNext()) {
        SofaFS sofa = sofaIter.next();
        if (sofa.getSofaRef() == 1) {
          casBeingFilled.registerInitialSofa();
        } else {
          // add indexRepo for views other than the initial view
          // the position in this indexed collection must be equal to the sofa's sofaNum (==
          // sofaRef)
          // The sofas are not necessarily in the sofaRef order.
          Misc.setWithExpand(indexRepositories, sofa.getSofaRef(),
                  casBeingFilled.getSofaIndexRepository(sofa));
        }
      }
    }

    private final void resetBuffer() {
      buffer.setLength(0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#startDocument()
     */
    @Override
    public void startDocument() throws SAXException {
      // Do setup work in the constructor.
      this.state = DOC_STATE;
      // System.out.println("Starting to read document.");
      // time = System.currentTimeMillis();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String,
     * java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String nameSpaceURI, String localName, String qualifiedName,
            Attributes attrs) throws SAXException {
      // org.apache.vinci.debug.Debug.p("startElement: " + qualifiedName);
      // if (attrs != null) {
      // for (int i=0; i<attrs.getLength(); i++) {
      // org.apache.vinci.debug.Debug.p("a: " + attrs.getQName(i) + " v: " + attrs.getValue(i));
      // }
      // }
      resetBuffer();
      switch (state) {
        case DOC_STATE: {
          // allow any root element name
          // extract xmlns:prefix=uri attributes into a map, which we can use to
          // resolve the prefixes even with a non-namespace-aware parser
          if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
              String attrName = attrs.getQName(i);
              if (attrName.startsWith("xmlns:")) {
                String prefix = attrName.substring(6);
                String uri = attrs.getValue(i);
                nsPrefixToUriMap.put(prefix, uri);
              }
            }
          }
          this.state = FS_STATE;
          break;
        }
        case FS_STATE: {
          // ignore elements with XMI prefix (such as XMI annotations)
          if (qualifiedName.startsWith("xmi")) {
            this.state = IGNORING_XMI_ELEMENTS_STATE;
            this.ignoreDepth++;
            return;
          }

          // if Delta CAS check if preexisting FS check if allowed
          if (this.mergePoint >= 0) {
            String id = attrs.getValue(ID_ATTR_NAME);
            if (id != null) {
              int idInt = Integer.parseInt(id);
              if (idInt > 0 && !this.isNewFS(idInt)) { // preexisting FS
                if (this.allowPreexistingFS == AllowPreexistingFS.ignore) { // skip elements whose
                                                                            // ID is <= mergePoint
                  this.state = IGNORING_XMI_ELEMENTS_STATE;
                  this.ignoreDepth++;
                  return;
                } else if (this.allowPreexistingFS == AllowPreexistingFS.disallow) { // fail
                  throw new CASRuntimeException(
                          CASRuntimeException.DELTA_CAS_PREEXISTING_FS_DISALLOWED,
                          ID_ATTR_NAME + "=" + id, nameSpaceURI, localName, qualifiedName);
                }
              }
            }
          }

          if (nameSpaceURI == null || nameSpaceURI.length() == 0) {
            // parser may not be namespace-enabled, so try to resolve NS ourselves
            int colonIndex = qualifiedName.indexOf(':');
            if (colonIndex != -1) {
              String prefix = qualifiedName.substring(0, colonIndex);
              nameSpaceURI = nsPrefixToUriMap.get(prefix);
              if (nameSpaceURI == null) {
                // unbound namespace. Rather than failing, just assume a reasonable default.
                nameSpaceURI = "http:///" + prefix + ".ecore";
              }
              localName = qualifiedName.substring(colonIndex + 1);
            } else // no prefix. Use default URI
            {
              nameSpaceURI = XmiCasSerializer.DEFAULT_NAMESPACE_URI;
            }
          }

          readFS(nameSpaceURI, localName, qualifiedName, attrs);

          multiValuedFeatures.clear();
          state = FEAT_STATE;
          break;
        }
        case FEAT_STATE: {
          // parsing a feature recorded as a child element
          // check for an "href" feature, used for references
          String href = attrs.getValue("href");
          if (href != null && href.startsWith("#")) {
            // for out-of-typesystem objects, there's special handling here
            // to keep track of the fact this was an href so we re-serialize
            // correctly.
            if (this.outOfTypeSystemElement != null) {
              XmlElementName elemName = new XmlElementName(nameSpaceURI, localName, qualifiedName);
              List<XmlAttribute> ootsAttrs = new ArrayList<>();
              ootsAttrs.add(new XmlAttribute("href", href));
              XmlElementNameAndContents elemWithContents = new XmlElementNameAndContents(elemName,
                      null, ootsAttrs);
              this.outOfTypeSystemElement.childElements.add(elemWithContents);
            } else {
              // In-typesystem FS, so we can forget this was an href and just add
              // the integer value, which will be interpreted as a reference later.
              // NOTE: this will end up causing it to be reserialized as an attribute
              // rather than an element, but that is not in violation of the XMI spec.
              ArrayList<String> valueList = this.multiValuedFeatures.computeIfAbsent(qualifiedName,
                      k -> new ArrayList<>());
              valueList.add(href.substring(1));
            }
            state = REF_FEAT_STATE;
          } else {
            // non-reference feature, expecting feature value as character content
            state = FEAT_CONTENT_STATE;
          }
          break;
        }
        case IGNORING_XMI_ELEMENTS_STATE: {
          ignoreDepth++;
          break;
        }
        default: {
          // If we're not in an element expecting state, raise an error.
          throw createException(XCASParsingException.TEXT_EXPECTED, qualifiedName);
        }
      }
    }

    /**
     * Read one FS, create a new FS or update an existing one
     * 
     * @param nameSpaceURI
     *          -
     * @param localName
     *          -
     * @param qualifiedName
     *          -
     * @param attrs
     *          -
     * @throws SAXException
     *           -
     */
    private void readFS(String nameSpaceURI, String localName, String qualifiedName,
            Attributes attrs) throws SAXException {
      String typeName = xmiElementName2uimaTypeName(nameSpaceURI, localName);

      currentType = (TypeImpl) ts.getType(typeName);
      if (currentType == null) {
        // ignore NULL type
        if ("uima.cas.NULL".equals(typeName)) {
          return;
        }
        // special processing for uima.cas.View (encodes indexed FSs)
        if ("uima.cas.View".equals(typeName)) {
          processDeferredFSs();
          processView(attrs.getValue("sofa"), attrs.getValue("members"));
          String added = attrs.getValue("added_members");
          String deleted = attrs.getValue("deleted_members");
          String reindexed = attrs.getValue("reindexed_members");
          processView(attrs.getValue("sofa"), added, deleted, reindexed);
          return;
        }
        // type is not in our type system
        if (!lenient) {
          throw createException(XCASParsingException.UNKNOWN_TYPE, typeName);
        } else {
          addToOutOfTypeSystemData(new XmlElementName(nameSpaceURI, localName, qualifiedName),
                  attrs);
        }
        return;
      } else if (currentType.isArray()) {

        // store ID and array values (if specified as attribute).
        // we will actually create the array later, in endElement.

        String idStr = attrs.getValue(ID_ATTR_NAME);
        currentArrayId = idStr == null ? -1 : Integer.parseInt(idStr);
        String elements = attrs.getValue("elements");

        // special parsing for byte arrays (they are serialized as a hex
        // string. And we create them here instead of parsing to a string
        // array, for efficiency.
        if (casBeingFilled.isByteArrayType(currentType)) {
          createOrUpdateByteArray(elements, currentArrayId, null);
        } else {
          if (elements != null) {
            String[] parsedElements = parseArray(elements);
            currentArrayElements = Arrays.asList(parsedElements);
          } else {
            currentArrayElements = null;
          }
        }
      } else {

        // not an array type

        final String idStr = attrs.getValue(ID_ATTR_NAME);
        final int xmiId = (idStr == null) ? -1 : Integer.parseInt(idStr);

        if (isNewFS(xmiId)) { // new FS so create it.
          final TOP fs;
          if (sofaTypeCode == currentType.getCode()) {

            // special way to create Sofas
            String sofaID = attrs.getValue(CAS.FEATURE_BASE_NAME_SOFAID);

            // get sofaNum
            int thisSofaNum = (sofaID.equals(CAS.NAME_DEFAULT_SOFA)
                    || sofaID.equals("_DefaultTextSofaName")) ? 1 // initial view Sofa always has
                                                                  // sofaNum = 1
                            : this.nextSofaNum++;

            // get the sofa's mimeType
            String sofaMimeType = attrs.getValue(CAS.FEATURE_BASE_NAME_SOFAMIME);

            if (sofaID.equals("_DefaultTextSofaName")) { // change old default Sofa name to current
                                                         // one
              sofaID = CAS.NAME_DEFAULT_SOFA;
            }
            fs = casBeingFilled.createSofa(thisSofaNum, sofaID, sofaMimeType);

            // // get sofaNum
            // String sofaNum = attrs.getValue(CAS.FEATURE_BASE_NAME_SOFANUM);
            // final int extSofaNum = Integer.parseInt(sofaNum);

            // // get the sofa's FeatureStructure id
            // final int sofaExtId = Integer.parseInt(attrs.getValue(XCASSerializer.ID_ATTR_NAME));
          } else { // not a sofa, not an array
            if (currentType.isAnnotationBaseType()) {

              // take pains to create FS in the right view.
              String extSofaRef = attrs.getValue(CAS.FEATURE_BASE_NAME_SOFA); // the xmiId of the
                                                                              // sofa
              CAS casView = null;
              if (extSofaRef == null || extSofaRef.length() == 0) {
                // this may happen for cases where the sofaref is written as
                // an embedded xml element, rather than as an attribute.
                // To allow for that case, treat as "deferred"
                doDeferFsOrThrow(idStr, nameSpaceURI, localName, qualifiedName, attrs);
                return;
              }
              Sofa sofa = (Sofa) maybeGetFsForXmiId(Integer.parseInt(extSofaRef));
              if (null != sofa) {
                casView = casBeingFilled.getView(sofa);
              }

              if (casView == null) {
                doDeferFsOrThrow(idStr, nameSpaceURI, localName, qualifiedName, attrs);
                return; // no further processing of this element when deferred. Subelements recorded
                        // though.
              } else {
                if (currentType.getCode() == TypeSystemConstants.docTypeCode) { // documentAnnotation
                  fs = casView.getDocumentAnnotation(); // gets existing one or creates a new one
                } else {
                  fs = casView.createFS(currentType); // not document annotation
                  if (currentFs instanceof UimaSerializable) {
                    UimaSerializable ufs = (UimaSerializable) currentFs;
                    uimaSerializableFixups.add(() -> ufs._init_from_cas_data());
                  }
                }
              }
            } else { // not annotationBase subtype
              fs = casBeingFilled.createFS(currentType);
              if (currentFs instanceof UimaSerializable) {
                UimaSerializable ufs = (UimaSerializable) currentFs;
                uimaSerializableFixups.add(() -> ufs._init_from_cas_data());
              }
            }
          } // end of not a sofa, not an array
          readFS(fs, attrs, IS_NEW_FS);
        } else { // preexisting
          if (this.allowPreexistingFS == AllowPreexistingFS.disallow) {
            throw new CASRuntimeException(CASRuntimeException.DELTA_CAS_PREEXISTING_FS_DISALLOWED,
                    ID_ATTR_NAME + "=" + idStr, nameSpaceURI, localName, qualifiedName);
          } else if (this.allowPreexistingFS == AllowPreexistingFS.allow) { // get the FS
            final TOP fs = getFsForXmiId(xmiId);
            // remove from indexes, and remember if was there, per view
            // (might be indexed in some views, not in others)
            // FSsTobeReindexed modifySafely = casBeingFilled.modifySafely()
            // casBeingFilled.removeFromCorruptableIndexAnyView(addr, toBeAddedBack);

            readFS(fs, attrs, IS_EXISTING_FS);
          } // otherwise ignore ( AllowPreexistingFS is not disallow nor allow)
        }
      } // end of not-an-array type
    }

    private void doDeferFsOrThrow(String idStr, String nameSpaceURI, String localName,
            String qualifiedName, Attributes attrs) throws XCASParsingException {
      if (processingDeferredFSs) {
        throw createException(XCASParsingException.SOFA_REF_MISSING);
      }
      if (this.deferredFSs == null) {
        this.deferredFSs = new ArrayList<>();
      }
      this.deferredFsElement = new OotsElementData(idStr,
              new XmlElementName(nameSpaceURI, localName, qualifiedName),
              (locator == null) ? 0 : locator.getLineNumber(),
              (locator == null) ? 0 : locator.getColumnNumber());

      deferredFSs.add(this.deferredFsElement);
      // This next call isn't about oots data, it's reusing that to store the attributes with the
      // deferred thing.
      addOutOfTypeSystemAttributes(this.deferredFsElement, attrs);
    }

    /**
     * Handles the processing of a cas:View element in the XMI. The cas:View element encodes indexed
     * FSs.
     * 
     * @param sofa
     *          xmi:id of the sofa for this view, null indicates base CAS "view"
     * @param membersString
     *          whitespace-separated string of FS addresses. Each FS is to be added to the specified
     *          sofa's index repository The adding takes place after FSs are finalized, to enable
     *          checking the sofa refs are OK https://issues.apache.org/jira/browse/UIMA-4099
     */
    private void processView(String sofa, String membersString) throws SAXParseException {
      // TODO: this requires View to come AFTER all of its members
      if (membersString != null) {
        final int sofaXmiId = (sofa == null) ? 1 : Integer.parseInt(sofa);
        FSIndexRepositoryImpl indexRep = getIndexRepo(sofa, sofaXmiId);
        final boolean newview = (sofa == null) ? false : isNewFS(sofaXmiId);

        final List<TOP> todo = toBeAdded.getTodos(indexRep);

        // TODO: optimize by going straight to int[] without going through
        // intermediate String[]?
        String[] members = parseArray(membersString);

        for (int i = 0; i < members.length; i++) {
          int xmiId = Integer.parseInt(members[i]);
          // special handling for merge operations ...
          if (!newview && !isNewFS(xmiId)) {
            // a pre-existing FS is indexed in a pre-existing view
            if (this.allowPreexistingFS == AllowPreexistingFS.ignore) {
              // merging with full CAS: ignore anything below the high water mark
              continue;
            }
            if (this.allowPreexistingFS == AllowPreexistingFS.disallow) {
              // merging with delta CAS: flag it
              this.disallowedViewMemberEncountered = true;
              continue;
            }
          }
          // have to map each ID to its "real" address (TODO: optimize?)
          TOP fs = maybeGetFsForXmiId(xmiId);
          // indexRep.addFS(addr); // can't do now because sofa ref not yet fixed up
          if (fs != null) {
            todo.add(fs); // https://issues.apache.org/jira/browse/UIMA-4099
          } else {
            if (!lenient) {
              if (xmiId == 0)
                report0xmiId(); // debug
              throw createException(XCASParsingException.UNKNOWN_ID, Integer.toString(xmiId));
            } else {
              // unknown view member may be an OutOfTypeSystem FS
              this.sharedData.addOutOfTypeSystemViewMember(sofa, members[i]);
            }
          }
        }
      }
    }

    /**
     * @param sofaXmiIdAsString
     *          xmiId
     * @param sofaNum
     *          1 if sofa null, or the sofa Xmi Id
     * @return the FS Repository associated with the sofa xmiId
     * @throws XCASParsingException
     */
    private FSIndexRepositoryImpl getIndexRepo(String sofaXmiIdAsString, int sofaXmiId)
            throws XCASParsingException {
      // a view with no Sofa will be added to the 1st, _InitialView, index

      if (sofaXmiIdAsString == null) {
        return (FSIndexRepositoryImpl) indexRepositories.get(1);
      }
      // translate sofa's xmi:id into its sofanum
      Sofa sofa = (Sofa) maybeGetFsForXmiId(sofaXmiId);
      if (null == sofa) {
        if (sofaXmiId == 0)
          report0xmiId(); // debug
        throw createException(XCASParsingException.UNKNOWN_ID, Integer.toString(sofaXmiId));
      }
      return (FSIndexRepositoryImpl) indexRepositories.get(sofa.getSofaNum());
    }

    /**
     * Handles the processing of a cas:View element in the XMI. The cas:View element encodes indexed
     * FSs.
     * 
     * @param sofa
     *          xmi:id of the sofa for this view, null indicates base CAS "view"
     * @param membersString
     *          whitespace-separated string of FS addresses. Each FS is to be added to the specified
     *          sofa's index repository
     */
    private void processView(String sofa, String addmembersString, String delmemberString,
            String reindexmemberString) throws SAXParseException {
      // TODO: this requires View to come AFTER all of its members
      if (addmembersString != null) {
        processView(sofa, addmembersString);
      }
      if (delmemberString == null && reindexmemberString == null) {
        return;
      }

      final int sofaXmiId = (sofa == null) ? 1 : Integer.parseInt(sofa);
      FSIndexRepositoryImpl indexRep = getIndexRepo(sofa, sofaXmiId);

      // int sofaNum = 1;
      // FSIndexRepositoryImpl indexRep = null;
      //
      // if (sofa != null) {
      // // translate sofa's xmi:id into its sofanum
      // int sofaXmiId = Integer.parseInt(sofa);
      // int sofaAddr = getFsForXmiId(sofaXmiId);
      // sofaNum = casBeingFilled.getFeatureValue(sofaAddr, TypeSystemConstants.sofaNumFeatCode);
      // }
      // indexRep = (FSIndexRepositoryImpl) indexRepositories.get(sofaNum);

      // TODO: optimize by going straight to int[] without going through
      // intermediate String[]?
      if (delmemberString != null) {
        List<TOP> localRemoves = toBeRemoved.getTodos(indexRep);
        String[] members = parseArray(delmemberString);
        for (int i = 0; i < members.length; i++) {
          int xmiId = Integer.parseInt(members[i]);
          if (!isNewFS(xmiId)) { // preexisting FS
            if (this.allowPreexistingFS == AllowPreexistingFS.disallow) {
              this.disallowedViewMemberEncountered = true; // ignore but flag it.
              continue;
            } else if (this.allowPreexistingFS == AllowPreexistingFS.ignore) {
              continue; // ignore
            }
          }
          // have to map each ID to its "real" address (TODO: optimize?)
          TOP fs = maybeGetFsForXmiId(xmiId);
          // indexRep.removeFS(addr); // can't do now because sofa ref not yet fixed up
          if (fs != null) {
            localRemoves.add(fs); // https://issues.apache.org/jira/browse/UIMA-4099
          } else {
            if (!lenient) {
              if (xmiId == 0)
                report0xmiId(); // debug
              throw createException(XCASParsingException.UNKNOWN_ID, Integer.toString(xmiId));
            } else {
              // unknown view member may be an OutOfTypeSystem FS
              this.sharedData.addOutOfTypeSystemViewMember(sofa, members[i]);
            }
          }
        } // end of for loop over members
      }
      // if (reindexmemberString != null) {
      // String[] members = parseArray(reindexmemberString);
      // for (int i = 0; i < members.length; i++) {
      // int id = Integer.parseInt(members[i]);
      // if (!isNewFS(id)) { //preexising FS
      // if (this.allowPreexistingFS == AllowPreexistingFS.disallow) {
      // this.disallowedViewMemberEncountered = true; //ignore but flag it.
      // continue;
      // } else if (this.allowPreexistingFS == AllowPreexistingFS.ignore) {
      // continue;
      // }
      // }
      // // have to map each ID to its "real" address (TODO: optimize?)
      // //TODO: currently broken, can't use XmiSerializationSharedData for
      // //this id mapping when merging, need local map
      // try {
      // int addr = getFsForXmiId(id);
      // indexRep.removeFS(addr);
      // indexRep.addFS(addr);
      // } catch (NoSuchElementException e) {
      // if (!lenient) {
      // throw createException(XCASParsingException.UNKNOWN_ID, Integer.toString(id));
      // } else {
      // //unknown view member may be an OutOfTypeSystem FS
      // this.sharedData.addOutOfTypeSystemViewMember(sofa, members[i]);
      // }
      // }
      // }
      // }
    }

    private void readFS(final TOP fs, Attributes attrs, final boolean isNewFs) throws SAXException {
      // Hang on to FS for setting content feature (things coded as child xml elements)
      this.currentFs = fs;
      String attrName, attrValue;
      final TypeImpl type = fs._getTypeImpl();
      final int typeCode = type.getCode();

      // if (fs instanceof Sofa) {
      //
      // // NOTE: thisSofaNum set to external value for existing sofas, might be different from
      // internal one?
      //
      // String sofaID = attrs.getValue(CAS.FEATURE_BASE_NAME_SOFAID);
      // if (sofaID.equals(CAS.NAME_DEFAULT_SOFA) || sofaID.equals("_DefaultTextSofaName")) {
      // // initial view Sofa always has sofaNum = 1
      // thisSofaNum = 1;
      // } else {
      // if (isNewFs) {
      // thisSofaNum = ((Sofa)fs).getSofaNum();
      // } else {
      // thisSofaNum = Integer.parseInt(attrs.getValue(CAS.FEATURE_BASE_NAME_SOFANUM));
      // }
      // }
      // } else {
      // thisSofaNum = 0;
      // }

      // //is it a new FS
      // try {
      // id = Integer.parseInt(attrs.getValue(ID_ATTR_NAME));
      // } catch (NumberFormatException e) {
      // throw createException(XCASParsingException.ILLEGAL_ID, attrs.getValue(ID_ATTR_NAME));
      // }

      this.featsSeen = null;

      // before looping over all features for this FS,
      // remove this FS if
      // it's not new or
      // it's a new documentAnnotation (because that's automatically indexed)
      // from all the indexes.
      // we do this once, before the feature setting loop, because that loop may set a sofa Ref
      // which is
      // invalid (to be fixed up later). But the removal code needs a valid sofa ref.
      try {
        if (!isNewFs || fs._getTypeCode() == TypeSystemConstants.docTypeCode) {
          casBeingFilled.removeFromCorruptableIndexAnyView(fs, casBeingFilled.getAddbackSingle());
          // else clause not needed because caller does ll_createFS which sets this anyways
          // } else {
          // // need this to prevent using sofa ref before it's set
          // casBeingFilled.setCacheNotInIndex(fsAddr); // new FSs are not indexed (yet)
        }

        // before looping over features, set the xmi to fs correspondence for this FS, in case a
        // feature does a self reference
        String idStr = attrs.getValue(ID_ATTR_NAME);
        final int extId;
        try {
          extId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
          throw createException(XCASParsingException.ILLEGAL_ID, idStr);
        }
        addFsToXmiId(fs, extId);

        // set up feats seen for existing, non-sofa FSs
        this.featsSeen = (sofaTypeCode != typeCode && !isNewFs) ? new IntVector(attrs.getLength())
                : null;

        // loop over all attributes in the xml for this FS
        for (int i = 0; i < attrs.getLength(); i++) {
          attrName = attrs.getQName(i);
          attrValue = attrs.getValue(i);
          if (attrName.equals(ID_ATTR_NAME)) {
            continue;
          }

          int featCode = handleFeatureFromName(type, fs, attrName, attrValue, isNewFs);
          // if processing delta cas preexisting FS, keep track of features that have
          // been deserialized.
          if (this.featsSeen != null && featCode != -1) {
            this.featsSeen.add(featCode);
          }
        } // end of all features loop
      } finally {
        if (!isNewFs || fs._getTypeCode() == TypeSystemConstants.docTypeCode) {
          casBeingFilled.addbackSingle(fs);
        }
      }

      if (sofaTypeCode == typeCode && isNewFs) {
        // If a Sofa, create CAS view to get new indexRepository
        Sofa sofa = (Sofa) fs;
        // also add to indexes so we can retrieve the Sofa later
        casBeingFilled.getBaseIndexRepository().addFS(sofa);
        CAS view = casBeingFilled.getView(sofa);
        if (sofa.getSofaRef() == 1) {
          casBeingFilled.registerInitialSofa();
        } else {
          // add indexRepo for views other than the initial view
          indexRepositories.add(casBeingFilled.getSofaIndexRepository(sofa));
        }
        ((CASImpl) view).registerView(sofa);
        views.add(view);
      }
    }

    // The definition of a null value. Any other value must be in the expected
    // format.
    private final boolean emptyVal(String val) {
      return ((val == null) || (val.length() == 0));
    }

    /**
     * Deserialize one feature called from readFS 751 called from processDeferred, to handle
     * features specified as child elements
     * 
     * @param type
     *          -
     * @param fs
     *          the FS
     * @param featName
     *          the feature name
     * @param featVal
     *          the value of the feature
     * @param isNewFS
     *          true if this is a new FS
     * @return feature code or -1 if no feature in this type system
     * @throws SAXException
     *           if Type doesn't have the feature, and we're not in lenient mode
     */
    private int handleFeatureFromName(final TypeImpl type, TOP fs, String featName, String featVal,
            final boolean isNewFS) throws SAXException {
      final FeatureImpl feat = (FeatureImpl) type.getFeatureByBaseName(featName);
      if (feat == null) {
        if (!this.lenient) {
          throw createException(XCASParsingException.UNKNOWN_FEATURE, featName);
        } else {
          // this logic mimics the way version 2 did this.
          if (isDoingDeferredChildElements) {
            ArrayList<String> featValAsArrayList = new ArrayList<>(1);
            featValAsArrayList.add(featVal);
            sharedData.addOutOfTypeSystemChildElements(fs, featName, featValAsArrayList);
          } else {
            sharedData.addOutOfTypeSystemAttribute(fs, featName, featVal);
          }
        }
        return -1;
      }

      // Sofa FS
      // only update Sofa data features and mime type feature. skip other features.
      // skip Sofa data features if Sofa data is already set.
      // these features may not be modified.
      if ((fs instanceof Sofa) && !isNewFS) {
        if (featName.equals(CAS.FEATURE_BASE_NAME_SOFAID)
                || featName.equals(CAS.FEATURE_BASE_NAME_SOFANUM)) {
          return feat.getCode();
        } else if (((Sofa) fs).isSofaDataSet()) {
          return feat.getCode();
        }
      }

      handleFeatSingleValue(fs, feat, featVal);
      return feat.getCode();
    }

    /**
     * called from endElement after collecting non-byte array element instances into a string list
     * for a particular array or list feature (excluding oots and deferred FSs)
     * 
     * @param type
     *          -
     * @param fs
     *          -
     * @param featName
     *          -
     * @param featVals
     *          -
     * @return the feature code of the featName arg, or -1 if feature not found (oots) and lenient
     * @throws SAXException
     */
    private int handleFeatMultiValueFromName(final Type type, TOP fs, String featName,
            ArrayList<String> featVals) throws SAXException {
      final FeatureImpl feat = (FeatureImpl) type.getFeatureByBaseName(featName);
      if (feat == null) {
        if (!this.lenient) {
          throw createException(XCASParsingException.UNKNOWN_FEATURE, featName);
        } else {
          sharedData.addOutOfTypeSystemChildElements(fs, featName, featVals);
        }
        return -1;
      }
      handleFeatMultiValue(fs, feat, featVals);
      return feat.getCode();
    }

    /*
     * Set a CAS feature from an XMI attribute. Includes setting Sofa features that are settable one
     * time, and sofa mime type
     * 
     * @param addr address of FS containing the feature
     * 
     * @param featCode code of feature to set
     * 
     * @param featVal string representation of the feature value
     * 
     * @throws SAXException -
     */
    private void handleFeatSingleValue(TOP fs, FeatureImpl fi, String featVal) throws SAXException {
      if ((fs instanceof AnnotationBase)
              && (fi.getCode() == TypeSystemConstants.annotBaseSofaFeatCode)) {
        // the sofa feature is set when the FS is created, can't be set separately
        return;
      }

      final int rangeClass = fi.rangeTypeClass;

      switch (rangeClass) {
        case LowLevelCAS.TYPE_CLASS_INT:
        case LowLevelCAS.TYPE_CLASS_BYTE:
        case LowLevelCAS.TYPE_CLASS_SHORT:
        case LowLevelCAS.TYPE_CLASS_LONG:
        case LowLevelCAS.TYPE_CLASS_BOOLEAN:
        case LowLevelCAS.TYPE_CLASS_FLOAT:
        case LowLevelCAS.TYPE_CLASS_DOUBLE:
        case LowLevelCAS.TYPE_CLASS_STRING: {
          CASImpl.setFeatureValueFromStringNoDocAnnotUpdate(fs, fi, featVal);
          break;
        }
        case LowLevelCAS.TYPE_CLASS_FS:
          deserializeFsRef(featVal, fi, fs);
          break;

        // For array types and list features, there are two kinds of serializations.
        // If the feature has multipleReferencesAllowed = true, then it should have been
        // serialized as a normal FS. If it has multipleReferencesAllowed = false, then
        // it should have been serialized as a multi-valued property.
        case LowLevelCAS.TYPE_CLASS_INTARRAY:
        case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
        case LowLevelCAS.TYPE_CLASS_STRINGARRAY:
        case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY:
        case LowLevelCAS.TYPE_CLASS_BYTEARRAY:
        case LowLevelCAS.TYPE_CLASS_SHORTARRAY:
        case LowLevelCAS.TYPE_CLASS_LONGARRAY:
        case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY:
        case LowLevelCAS.TYPE_CLASS_FSARRAY: {
          if (fi.isMultipleReferencesAllowed()) {
            deserializeFsRef(featVal, fi, fs);
            break;
          }

          // is encoded with the feature reference
          // Do the multivalued property deserialization.
          // However, byte arrays have a special serialization (as hex digits)
          if (rangeClass == LowLevelCAS.TYPE_CLASS_BYTEARRAY) {
            ByteArray existingByteArray = (ByteArray) fs.getFeatureValue(fi); // might be null

            ByteArray byteArray = createOrUpdateByteArray(featVal, -1, existingByteArray);
            if (byteArray != existingByteArray) {
              CASImpl.setFeatureValueMaybeSofa(fs, fi, byteArray);
            }
          } else { // not ByteArray, but encoded locally
            String[] arrayVals = parseArray(featVal);
            handleFeatMultiValue(fs, fi, Arrays.asList(arrayVals));
          }
          break;
        }
        // For list types, we do the same as for array types UNLESS we're dealing with
        // the tail feature of another list node. In that case we do the usual FS deserialization.
        case CasSerializerSupport.TYPE_CLASS_INTLIST:
        case CasSerializerSupport.TYPE_CLASS_FLOATLIST:
        case CasSerializerSupport.TYPE_CLASS_STRINGLIST:
        case CasSerializerSupport.TYPE_CLASS_FSLIST: {
          if (fi.isMultipleReferencesAllowed()) {
            // do the usual FS deserialization
            deserializeFsRef(featVal, fi, fs);
          } else { // do the multivalued property deserialization, like arrays
            String[] arrayVals = parseArray(featVal);
            handleFeatMultiValue(fs, fi, Arrays.asList(arrayVals));
          }
          break;
        }
        default: {
          Misc.internalError(); // this should be an exhaustive case block
        }
      }
    }

    private void deserializeFsRef(String featVal, FeatureImpl fi, TOP fs) {
      if (featVal == null || featVal.length() == 0) {
        CASImpl.setFeatureValueMaybeSofa(fs, fi, null);
      } else {
        int xmiId = Integer.parseInt(featVal);
        TOP tgtFs = maybeGetFsForXmiId(xmiId);
        if (null == tgtFs) {
          fixupToDos.add(() -> finalizeRefValue(xmiId, fs, fi));
        } else {
          CASImpl.setFeatureValueMaybeSofa(fs, fi, tgtFs);
          ts.fixupFSArrayTypes(fi.getRangeImpl(), tgtFs);
        }
      }
    }

    /**
     * Parse an XMI multi-valued attribute into a String array, by splitting on whitespace.
     * 
     * @param val
     *          XMI attribute value
     * @return an array with each array value as an element
     */
    private String[] parseArray(String val) {
      String[] arrayVals;
      val = val.trim();
      if (emptyVal(val)) {
        arrayVals = Constants.EMPTY_STRING_ARRAY;
      } else {
        arrayVals = whiteSpace.split(val);
      }
      return arrayVals;
    }

    /*
     * Set a CAS feature from an array of Strings. This supports the XMI syntax where each value is
     * listed as a separate subelement.
     * 
     * used for arrays and lists
     * 
     * NOTE: we deserialized arrays and lists encoded this way, even if the type system indicates
     * the feature is multi-ref-allowed. (in this case, although allowed, it is not multi-refed)
     * 
     * @param addr address of FS containing the feature
     * 
     * @param fi the feature being set
     * 
     * @param featVals List of Strings, each String representing one value for the feature
     * 
     * @throws SAXException -
     */
    private void handleFeatMultiValue(TOP fs, FeatureImpl fi, List<String> featVals)
            throws SAXException {
      final int rangeCode = fi.rangeTypeClass;
      switch (rangeCode) {
        case LowLevelCAS.TYPE_CLASS_INT:
        case LowLevelCAS.TYPE_CLASS_FLOAT:
        case LowLevelCAS.TYPE_CLASS_STRING:
        case LowLevelCAS.TYPE_CLASS_BOOLEAN:
        case LowLevelCAS.TYPE_CLASS_BYTE:
        case LowLevelCAS.TYPE_CLASS_SHORT:
        case LowLevelCAS.TYPE_CLASS_LONG:
        case LowLevelCAS.TYPE_CLASS_DOUBLE:
        case LowLevelCAS.TYPE_CLASS_FS:
          if (featVals.size() != 1) {
            throw new SAXParseException(I18nUtil.localizeMessage(
                    UIMAException.STANDARD_MESSAGE_CATALOG, Locale.getDefault(),
                    "multiple_values_unexpected", new Object[] { fi.getName() }), locator);
          } else {
            handleFeatSingleValue(fs, fi, featVals.get(0));
          }
          break;
        case LowLevelCAS.TYPE_CLASS_INTARRAY:
        case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
        case LowLevelCAS.TYPE_CLASS_STRINGARRAY:
        case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY:
        case LowLevelCAS.TYPE_CLASS_BYTEARRAY:
        case LowLevelCAS.TYPE_CLASS_SHORTARRAY:
        case LowLevelCAS.TYPE_CLASS_LONGARRAY:
        case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY:
        case LowLevelCAS.TYPE_CLASS_FSARRAY: {
          CommonArrayFS existingArray = (CommonArrayFS) fs.getFeatureValue(fi);
          CommonArrayFS casArray = createOrUpdateArray(fi.getRangeImpl(), featVals, -1,
                  existingArray);
          if (existingArray != casArray) {
            CASImpl.setFeatureValueMaybeSofa(fs, fi, (TOP) casArray);
          }
          // add to nonshared fs to encompassing FS map
          if (!fi.isMultipleReferencesAllowed()) { // ! multiple refs => value is not shared
            addNonsharedFSToEncompassingFSMapping((TOP) casArray, fs);
          }
          break;
        }

        case CasSerializerSupport.TYPE_CLASS_INTLIST:
        case CasSerializerSupport.TYPE_CLASS_FLOATLIST:
        case CasSerializerSupport.TYPE_CLASS_STRINGLIST:
        case CasSerializerSupport.TYPE_CLASS_FSLIST: {
          if (featVals == null) {
            fs.setFeatureValue(fi, null);
          } else if (featVals.size() == 0) {
            fs.setFeatureValue(fi, casBeingFilled.emptyList(rangeCode));
          } else {
            CommonList existingList = (CommonList) fs.getFeatureValue(fi);
            CommonList theList = createOrUpdateList(fi.getRangeImpl(), featVals, -1, existingList);
            if (existingList != theList) {
              fs.setFeatureValue(fi, theList);
            }

            // add to nonshared fs to encompassing FS map, for all elements
            if (!fi.isMultipleReferencesAllowed()) {
              CommonList node = theList;
              while (node != null && (node instanceof NonEmptyList)) {
                addNonsharedFSToEncompassingFSMapping((TOP) node, fs);
                node = node.getCommonTail();
              }
            }
          }
          break;
        }
        default:
          assert false; // this should be an exhaustive case block
      }
    }

    /**
     * Called only for non-shared lists where all the list items serialized with the feature
     * 
     * @param listType
     *          -
     * @param values
     *          - guaranteed to have at least one.
     * @param existingList
     *          - the existing list head ref
     * @return the existingList or a new list
     */
    private CommonList createOrUpdateList(TypeImpl listType, List<String> values, int xmiId,
            CommonList existingList) {
      if (existingList != null) {
        updateExistingList(values, existingList);
        return existingList;
      } else {
        return createListFromStringValues(values,
                casBeingFilled.emptyListFromTypeCode(listType.getCode()));
      }
    }

    /**
     * Create or update an array in the CAS
     * 
     * If the array is an FSArray, and the elements are not yet deserialized, a lambda expression is
     * put on a "todo" list to be executed after all the FSs are deserialized, to set the value
     * later.
     * 
     * @param arrayType
     *          CAS type for the array
     * @param values
     *          List of strings, each representing an element in the array
     * @param xmiId
     *          xmi:id assigned to the array object.
     * @param existingArray
     *          preexisting non-shared array when processing a Delta CAS.
     * @return the new or updated-existing FS for the array
     * @throws XCASParsingException
     */
    private CommonArrayFS createOrUpdateArray(TypeImpl arrayType, List<String> values, int xmiId,
            CommonArrayFS existingArray) throws XCASParsingException {
      if (values == null) {
        return null;
      }

      final int arrayLen = values.size();
      final CommonArrayFS resultArray;

      if (existingArray != null) { // values are local to feature (nonshared), preexisting
        if (arrayLen == 0) {
          resultArray = (existingArray.size() == 0) ? existingArray
                  : (CommonArrayFS) casBeingFilled.createArray(arrayType, 0);
        } else {
          if (existingArray.size() == arrayLen) {
            updateExistingArray(values, existingArray);
            resultArray = existingArray;
          } else {
            resultArray = createNewArray(arrayType, values);
          }
        }

      } else if (xmiId == -1 || // values are local to feature (nonshared), no preexisting
              isNewFS(xmiId)) { // values are with FS, but it's above the line (new)
        resultArray = createNewArray(arrayType, values);

      } else { // values are with FS, below the line
        existingArray = (CommonArrayFS) getFsForXmiId(xmiId);
        if (existingArray.size() == arrayLen) {
          updateExistingArray(values, existingArray);
          resultArray = existingArray;
        } else {
          resultArray = createNewArray(arrayType, values);
        }
      }

      TOP newOrUpdated = (TOP) resultArray;
      addFsToXmiId(newOrUpdated, xmiId);
      return resultArray;
    }

    /**
     * Create an array in the CAS.
     * 
     * @param arrayType
     *          CAS type code for the array
     * @param values
     *          List of strings, each containing the value of an element of the array.
     * @return a reference to the array FS
     */
    private CommonArrayFS createNewArray(TypeImpl type, List<String> values) {
      final int sz = values.size();
      CommonArrayFS fs = (sz == 0) ? casBeingFilled.emptyArray(type)
              : (CommonArrayFS) casBeingFilled.createArray(type, sz);
      if (fs instanceof FSArray) {
        final FSArray fsArray = (FSArray) fs;
        for (int i = 0; i < sz; i++) {
          maybeSetFsArrayElement(values, i, fsArray);
        }
      } else {
        CommonPrimitiveArray fsp = (CommonPrimitiveArray) fs;
        for (int i = 0; i < sz; i++) {
          fsp.setArrayValueFromString(i, values.get(i));
        }
      }
      return fs;
    }

    /**
     * Update existing array. The size has already been checked to be equal, but could be 0
     * 
     * @param arrayType
     * @param values
     * @param existingArray
     */
    private void updateExistingArray(List<String> values, CommonArrayFS existingArray) {
      final int sz = values.size();

      if (existingArray instanceof FSArray) {
        final FSArray fsArray = (FSArray) existingArray;
        for (int i = 0; i < sz; i++) {

          String featVal = values.get(i);
          if (emptyVal(featVal)) { // can be empty if JSON
            fsArray.set(i, null);

          } else {
            maybeSetFsArrayElement(values, i, fsArray);
            final int xmiId = Integer.parseInt(featVal);
            final int pos = i;
            TOP tgtFs = maybeGetFsForXmiId(xmiId);
            if (null == tgtFs) {
              fixupToDos.add(() -> finalizeFSArrayRefValue(xmiId, fsArray, pos));
            } else {
              fsArray.set(i, tgtFs);
            }
          }
        }
        return;
      }

      CommonPrimitiveArray existingPrimitiveArray = (CommonPrimitiveArray) existingArray;
      for (int i = 0; i < sz; i++) {
        existingPrimitiveArray.setArrayValueFromString(i, values.get(i));
      }
    }

    /**
     * existingList guaranteed non-null, but could be EmptyList instance values could be null or
     * empty
     * 
     * Return the existing list or a replacement for it which might be an emptylist
     */
    private CommonList updateExistingList(List<String> values, CommonList existingList) {
      if (values == null || values.size() == 0) {
        if (existingList instanceof EmptyList) {
          return existingList;
        } else {
          return existingList.emptyList();
        }
      }

      // values exists, has 1 or more elements
      final int valLen = values.size();

      if (existingList instanceof EmptyList) {
        return createListFromStringValues(values, (EmptyList) existingList);
      }

      // existingList is non-empty
      if (existingList instanceof FSList) {
        FSList node = (FSList) existingList;
        NonEmptyFSList prevNode = null;

        for (int i = 0; i < valLen; i++) {
          if (node instanceof EmptyList) { // never true initially due to above logic

            prevNode.setTail(createListFromStringValues(values, i, (EmptyList) node));
            return existingList;
          }

          NonEmptyFSList neNode = (NonEmptyFSList) node;
          maybeSetFsListHead(values.get(i), neNode);

          prevNode = (NonEmptyFSList) node;
          node = prevNode.getTail();
        }

        // got to the end of the values, but the existing list has more elements
        // truncate the existing list
        prevNode.setTail(existingList.emptyList());
        return existingList;
      }

      // non FS Array cases
      CommonList node = existingList;
      CommonList prevNode = null;

      for (int i = 0; i < valLen; i++) {
        if (node instanceof EmptyList) { // never true initially due to above logic
          prevNode.setTail(createListFromStringValues(values, i, (EmptyList) node));
          return existingList;
        }

        node.set_headFromString(values.get(i));

        prevNode = node;
        node = node.getCommonTail();
      }

      // got to the end of the values, but the existing list has more elements
      // truncate the existing list
      prevNode.setTail(existingList.emptyList());
      return existingList;
    }

    private void maybeSetFsArrayElement(List<String> values, int i, FSArray fsArray) {
      String featVal = values.get(i);
      if (emptyVal(featVal)) { // can be empty if JSON
        fsArray.set(i, null);
      } else {
        final int xmiId = Integer.parseInt(featVal);
        final int pos = i;
        TOP tgtFs = maybeGetFsForXmiId(xmiId);
        if (null == tgtFs) {
          fixupToDos.add(() -> finalizeFSArrayRefValue(xmiId, fsArray, pos));
        } else {
          fsArray.set(i, tgtFs);
        }
      }
    }

    private void maybeSetFsListHead(String featVal, NonEmptyFSList neNode) {
      if (emptyVal(featVal)) { // can be empty if JSON
        neNode.setHead(null);
      } else {
        final int xmiId = Integer.parseInt(featVal);
        TOP tgtFs = maybeGetFsForXmiId(xmiId);
        if (null == tgtFs) {
          fixupToDos.add(() -> finalizeFSListRefValue(xmiId, neNode));
        } else {
          neNode.setHead(tgtFs);
        }
      }
    }

    CommonList createListFromStringValues(List<String> stringValues, EmptyList emptyNode) {
      return createListFromStringValues(stringValues, 0, emptyNode);
    }

    /**
     * There are two variants - one for primitives, and one for values which are refs to FS.
     * 
     * For both variants, the items in the list are always single-reachable from their previous
     * nodes; that is, there are no loops or other sharing of nodes. If there were, this would not
     * be serialized as a simple list of values.
     * 
     * For the refs to FS case, the head references cannot be to any node in the list except the 1st
     * one. This is because the other nodes have no xmiIds in the serialized representation. If some
     * reference was to this, then the list would be serialized in the alternate format.
     * 
     * For head references to Feature Structures not known (not yet serialized, or
     * out-of-type-system), a null value is used, and a fixupToDos entry is added to fix this up
     * after all Feature Structures have been serialized. (same as FSArray deserialization)
     * 
     * @param stringValues
     *          - the primitive values in string representation, or the xmiIds for references
     * @param startPos
     *          - where in the list of values to start (in case we're adding to an existing list)
     * @param emptyNode
     *          - the last node in the list, the empty node that terminates it
     * @return the first node in the list of nodes (could be the empty node for an empty list)
     */
    private CommonList createListFromStringValues(List<String> stringValues, int startPos,
            EmptyList emptyNode) {
      /**
       * List is created backwards, from end, so last created node is the head of the list
       */

      if (emptyNode instanceof FSList) {

        FSList n = (FSList) emptyNode;

        for (int i = stringValues.size() - 1; i >= startPos; i--) {
          final String v = stringValues.get(i);
          final NonEmptyFSList nn = n.pushNode();
          maybeSetFsListHead(v, nn);
          n = nn;
        }
        return n;

      } else if (emptyNode instanceof IntegerList) {

        IntegerList n = (IntegerList) emptyNode;

        for (int i = stringValues.size() - 1; i >= startPos; i--) {
          final String v = stringValues.get(i);
          final NonEmptyIntegerList nn = n.push(Integer.parseInt(v));
          n = nn;
        }
        return n;

      } else if (emptyNode instanceof FloatList) {

        FloatList n = (FloatList) emptyNode;

        for (int i = stringValues.size() - 1; i >= startPos; i--) {
          final String v = stringValues.get(i);
          final NonEmptyFloatList nn = n.push(Float.parseFloat(v));
          n = nn;
        }
        return n;

      } else {

        StringList n = (StringList) emptyNode;

        for (int i = stringValues.size() - 1; i >= startPos; i--) {
          final String v = stringValues.get(i);
          final NonEmptyStringList nn = n.push(v);
          n = nn;
        }
        return n;

      }

    }

    // @formatter:off
    /**
     * Create a byte array in the CAS.
     * 
     * Treatment of null and empty.  
     *   The existing value can be null.
     *     - if not null, it has a size(), which may be 0.
     *   The incoming values may be:
     *     - null
     *     - a 0 length string
     *     - a string of some length (must be even number of bytes, else an error)
     *   
     *   If the incoming value is null - return null
     *   If the incoming value is 0 length string - return a 0 length ByteArray.
     *   If the incoming value's length matches the length of the existing ByteArray,
     *     - replace the value.  
     *   If the incoming value's length doesn't match, return a new ByteArray.
     *   
     * Cases:
     *   definitions: 
     *     - non-shared / shared : "non-shared" - meaning it is encoded at the spot of the feature in the serialization
     *     - fs already exists, meaning we're updating the array value (delta cas update below the line, by matching xmi:id's )
     * 
     *   case 1: the existingArray is null, or the incoming array is null or 0-length string
     *     - the incoming array replaces the existing value (if any), including setting 
     *       it to null.
     *   
     *   case 1: values are encoded in place at feature reference spot (implies non-shared)
     *     - fs exists
     *         if lengths the same, update the existing byte array, else throw exception - can't update size of existing one
     *     - fs is new                                                    make a new one
     *     
     *   case 2: values are encoded as a separate feature structure 
     *     - fs exists
     *         if lengths the same, update the existing byte array, else throw exception - can't update size of existing one
     *     - fs is new                                                    make a new one
     *     
     * @param hexString
     *          value of the byte array as a hex string
     * @param xmiId
     *          xmiId - this will be -1 if this is a non-shared (encoded locally with the Feature) byte array FS.
     * @param existingFs 
     *          the existing ByteArrayFS used when processing a Delta CAS.
     * @return the new or updated-existing FS for the array
     * @throws XCASParsingException 
     */
    // @formatter:on
    private ByteArray createOrUpdateByteArray(String hexString, int xmiId, ByteArray existingArray)
            throws XCASParsingException {
      if (hexString == null) {
        return null;
      }

      if ((hexString.length() & 1) != 0) {
        throw createException(XCASParsingException.BYTE_ARRAY_LENGTH_NOT_EVEN);
      }

      int arrayLen = hexString.length() / 2;
      final ByteArray fs;

      if (existingArray != null) {
        if (arrayLen == 0) {
          return (existingArray.size() == 0) ? existingArray
                  : (ByteArray) casBeingFilled.createByteArrayFS(0);
        }
        fs = (existingArray.size() == arrayLen) ? existingArray
                : (ByteArray) casBeingFilled.createByteArrayFS(arrayLen);

      } else if (xmiId == -1 || // values are local to feature (nonshared), no preexisting
              isNewFS(xmiId)) { // values are with FS, but it's above the line (new)
        fs = (ByteArray) casBeingFilled.createByteArrayFS(arrayLen);

      } else { // values are with FS, below the line
        existingArray = (ByteArray) getFsForXmiId(xmiId);
        fs = (existingArray.size() == arrayLen) ? existingArray
                : (ByteArray) casBeingFilled.createByteArrayFS(arrayLen);
      }

      for (int i = 0; i < arrayLen; i++) {
        byte high = hexCharToByte(hexString.charAt(i * 2));
        byte low = hexCharToByte(hexString.charAt(i * 2 + 1));
        byte b = (byte) ((high << 4) | low);
        fs.set(i, b);
      }

      addFsToXmiId(fs, xmiId);
      return fs;
    }

    private byte hexCharToByte(char c) {
      if ('0' <= c && c <= '9')
        return (byte) (c - '0');
      else if ('A' <= c && c <= 'F')
        return (byte) (c - 'A' + 10);
      else if ('a' <= c && c <= 'f')
        return (byte) (c - 'a' + 10);
      else
        throw new NumberFormatException("Invalid hex char: " + c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    @Override
    public void characters(char[] chars, int start, int length) throws SAXException {
      switch (this.state) {
        case FEAT_CONTENT_STATE:
          buffer.append(chars, start, length);
          break;
        default:
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String,
     * java.lang.String)
     */
    @Override
    public void endElement(String nsURI, String localName, String qualifiedName)
            throws SAXException {
      switch (this.state) {
        case DOC_STATE: {
          // Do nothing.
          break;
        }
        case FS_STATE: {
          this.state = DOC_STATE;
          break;
        }
        case FEAT_CONTENT_STATE: {
          // We have just processed one of possibly many values for a feature.
          // Store this value in the multiValuedFeatures map for later use.
          ArrayList<String> valueList = this.multiValuedFeatures.computeIfAbsent(qualifiedName,
                  k -> new ArrayList<>());
          valueList.add(buffer.toString());

          // go back to the state where we're expecting a feature
          this.state = FEAT_STATE;
          break;
        }
        case REF_FEAT_STATE: {
          this.state = FEAT_STATE;
          break;
        }
        case FEAT_STATE: {
          // end of FS. Process multi-valued features or array elements that were
          // encoded as subelements
          if (this.outOfTypeSystemElement != null || this.deferredFsElement != null) {
            if (!this.multiValuedFeatures.isEmpty()) {
              for (Map.Entry<String, ArrayList<String>> entry : this.multiValuedFeatures
                      .entrySet()) {
                String featName = entry.getKey();
                ArrayList<String> featVals = entry.getValue();
                XmiSerializationSharedData.addOutOfTypeSystemFeature(
                        (outOfTypeSystemElement == null) ? deferredFsElement
                                : outOfTypeSystemElement,
                        featName, featVals);
              }
            }
            this.outOfTypeSystemElement = this.deferredFsElement = null;

          } else if (currentType != null) {

            if (currentType.isArray()
                    && currentType.getCode() != TypeSystemConstants.byteArrayTypeCode) {
              // create the array now. elements may have been provided either as
              // attributes or child elements, but not both.
              // BUT - not byte arrays! They are created immediately, to avoid
              // the overhead of parsing into a String array first
              if (currentArrayElements == null) // were not specified as attributes
              {
                currentArrayElements = this.multiValuedFeatures.get("elements");
                if (currentArrayElements == null) {
                  currentArrayElements = Collections.emptyList();
                }
              }
              createOrUpdateArray(currentType, currentArrayElements, currentArrayId, null);

            } else if (!this.multiValuedFeatures.isEmpty()) {
              for (Map.Entry<String, ArrayList<String>> entry : this.multiValuedFeatures
                      .entrySet()) {
                String featName = entry.getKey();
                ArrayList<String> featVals = entry.getValue();
                int featcode = handleFeatMultiValueFromName(currentType, currentFs, featName,
                        featVals);
                if (featcode != -1 && this.featsSeen != null) {
                  this.featsSeen.add(featcode);
                }
              }
            }

            // if this is a preexisting FS which is not a Sofa FS,
            // set the features that were not deserialized to null.
            if (sofaTypeCode != currentType.getCode() && this.featsSeen != null) {
              for (FeatureImpl fi : currentType.getFeatureImpls()) {
                if (!(fi.getName().equals(CAS.FEATURE_FULL_NAME_SOFA))) {
                  if (!this.featsSeen.contains(fi.getCode())) {
                    CASImpl.setFeatureValueFromStringNoDocAnnotUpdate(currentFs, fi, null);
                  }
                }
              }
              this.featsSeen = null; // set for every feature instance being deserialized
            }

          }
          this.state = FS_STATE;
          break;
        }

        case IGNORING_XMI_ELEMENTS_STATE: {
          ignoreDepth--;
          if (ignoreDepth == 0) {
            this.state = FS_STATE;
          }
          break;
        }
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    @Override
    public void endDocument() throws SAXException {

      processDeferredFSs();

      // Resolve ID references
      for (Runnable_withSaxException todo : fixupToDos) {
        todo.run();
      }

      // add FSs to indexes
      // These come from the add list
      // https://issues.apache.org/jira/browse/UIMA-4099
      for (Entry<FSIndexRepositoryImpl, List<TOP>> e : toBeAdded.entrySet()) {
        FSIndexRepositoryImpl indexRep = e.getKey();
        final List<TOP> todo = e.getValue();
        for (TOP fs : todo) {
          indexRep.addFS(fs);
        }
      }

      // remove FSs from indexes
      for (Entry<FSIndexRepositoryImpl, List<TOP>> e : toBeRemoved.entrySet()) {
        FSIndexRepositoryImpl indexRep = e.getKey();
        final List<TOP> todo = e.getValue();
        for (TOP fs : todo) {
          indexRep.removeFS(fs);
        }
      }

      // time = System.currentTimeMillis() - time;
      // System.out.println("Done in " + new TimeSpan(time));

      for (CAS view : views) {
        ((CASImpl) view).updateDocumentAnnotation();
      }

      // check if disallowed fs encountered]
      if (this.disallowedViewMemberEncountered) {
        throw new CASRuntimeException(CASRuntimeException.DELTA_CAS_PREEXISTING_FS_DISALLOWED,
                "Preexisting FS view member encountered.");
      }

      for (Runnable r : uimaSerializableFixups) {
        r.run();
      }

    }

    private void finalizeRefValue(int xmiId, TOP fs, FeatureImpl fi) throws XCASParsingException {
      TOP tgtFs = maybeGetFsForXmiId(xmiId);
      if (null == tgtFs && xmiId != 0) { // https://issues.apache.org/jira/browse/UIMA-5446
        if (!lenient) {
          throw createException(XCASParsingException.UNKNOWN_ID, Integer.toString(xmiId));
        } else {
          // the element may be out of typesystem. In that case set it
          // to null, but record the id so we can add it back on next serialization.
          this.sharedData.addOutOfTypeSystemAttribute(fs, fi.getShortName(),
                  Integer.toString(xmiId));
          CASImpl.setFeatureValueMaybeSofa(fs, fi, null);
        }
      } else {
        CASImpl.setFeatureValueMaybeSofa(fs, fi, tgtFs);
        ts.fixupFSArrayTypes(fi.getRangeImpl(), tgtFs);
      }
    }

    private void finalizeFSListRefValue(int xmiId, NonEmptyFSList neNode)
            throws XCASParsingException {
      TOP tgtFs = maybeGetFsForXmiId(xmiId);
      if (null == tgtFs && xmiId != 0) { // https://issues.apache.org/jira/browse/UIMA-5446
        if (!lenient) {
          throw createException(XCASParsingException.UNKNOWN_ID, Integer.toString(xmiId));
        } else {
          // the element may be out of typesystem. In that case set it
          // to null, but record the id so we can add it back on next serialization.
          this.sharedData.addOutOfTypeSystemAttribute(neNode, CAS.FEATURE_BASE_NAME_HEAD,
                  Integer.toString(xmiId));
          neNode.setHead(null);
        }
      } else {
        neNode.setHead(tgtFs);
      }
    }

    private void finalizeFSArrayRefValue(int xmiId, FSArray fsArray, int index)
            throws XCASParsingException {
      TOP tgtFs = maybeGetFsForXmiId(xmiId);
      if (null == tgtFs && xmiId != 0) { // https://issues.apache.org/jira/browse/UIMA-5446
        if (!lenient) {
          throw createException(XCASParsingException.UNKNOWN_ID, Integer.toString(xmiId));
        } else {
          // the array element may be out of typesystem. In that case set it
          // to null, but record the id so we can add it back on next serialization.
          this.sharedData.addOutOfTypeSystemArrayElement(fsArray, index, xmiId);
          fsArray.set(index, null);
        }
      } else {
        fsArray.set(index, tgtFs);
      }
    }

    private XCASParsingException createException(int code) {
      XCASParsingException e = new XCASParsingException(code);
      String source = unknownXMLSource;
      String line = unknownXMLSource;
      String col = unknownXMLSource;
      if (locator != null) {
        source = locator.getSystemId();
        if (source == null) {
          source = locator.getPublicId();
        }
        if (source == null) {
          source = unknownXMLSource;
        }
        line = Integer.toString(locator.getLineNumber());
        col = Integer.toString(locator.getColumnNumber());
      }
      e.addArgument(source);
      e.addArgument(line);
      e.addArgument(col);
      return e;
    }

    private XCASParsingException createException(int code, String arg) {
      XCASParsingException e = createException(code);
      e.addArgument(arg);
      return e;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    @Override
    public void error(SAXParseException e) throws SAXException {
      throw e;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      throw e;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
     */
    @Override
    public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {
      // Since we're not validating, we don't need to do anything; this won't
      // be called.
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
     */
    @Override
    public void setDocumentLocator(Locator loc) {
      // System.out.println("debug DEBUG Setting document locator.");
      this.locator = loc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    @Override
    public void warning(SAXParseException e) throws SAXException {
      throw e;
    }

    private void addFsToXmiId(TOP fs, int xmiId) {
      if (xmiId > 0) {
        if (mergePoint < 0) {
          // if we are not doing a merge, update the map in the XmiSerializationSharedData
          sharedData.addIdMapping(fs, xmiId);
        } else {
          // if we're doing a merge, we can't update the shared map because we could
          // have duplicate xmi:id values in the different parts of the merge.
          // instead we keep a local mapping used only within this deserialization.
          localXmiIdToFs.put(xmiId, fs);
        }
      }
    }

    /*
     * Gets the FS address into which the XMI element with the given ID was deserialized. This
     * method supports merging multiple XMI documents into a single CAS, by checking the
     * XmiSerializationSharedData structure to get the address of elements that are below the
     * mergePoint and are expected to already be present in the CAS.
     */
    private TOP getFsForXmiId(int xmiId) {
      TOP r = maybeGetFsForXmiId(xmiId);
      if (r == null) {
        throw new NoSuchElementException();
      }
      return r;
    }

    private TOP maybeGetFsForXmiId(int xmiId) {
      // first check shared data (but if we're doing a merge, do so only
      // for xmi:ids below the merge point)
      if (mergePoint < 0 || !isNewFS(xmiId)) { // not merging or merging but is below the line
        TOP fs = sharedData.getFsForXmiId(xmiId);
        if (fs != null) {
          return fs;
        } else {
          return null;
        }
      } else {
        // if we're merging, then we use a local id map for FSs above the
        // merge point, since each of the different XMI CASes being merged
        // can use these same ids for different FSs.
        return localXmiIdToFs.get(xmiId);
        // if (localAddr != null) {
        // return localAddr.intValue();
        // } else {
        // throw new java.util.NoSuchElementException();
        // }
      }
    }

    /*
     * Adds a feature sturcture to the out-of-typesystem data. Also sets the
     * this.outOfTypeSystemElement field, which is referred to later if we have to handle features
     * recorded as child elements.
     */
    private void addToOutOfTypeSystemData(XmlElementName xmlElementName, Attributes attrs)
            throws XCASParsingException {
      String xmiId = attrs.getValue(ID_ATTR_NAME);
      this.outOfTypeSystemElement = new OotsElementData(xmiId, xmlElementName);
      addOutOfTypeSystemAttributes(this.outOfTypeSystemElement, attrs);
      this.sharedData.addOutOfTypeSystemElement(this.outOfTypeSystemElement);
    }

    private boolean isNewFS(int id) {
      return (id > this.mergePoint);
    }

    private void addNonsharedFSToEncompassingFSMapping(TOP nonsharedFS, TOP encompassingFS) {
      // System.out.println("addNonsharedFSToEncompassingFSMapping" + nonsharedFS + " " +
      // encompassingFS);
      this.sharedData.addNonsharedRefToFSMapping(nonsharedFS, encompassingFS);
    }

    private void processDeferredFSs() throws SAXException {
      if (null == deferredFSs) {
        return;
      }

      processingDeferredFSs = true;
      // Reminder: not really out of type system info - just reusing that data structure for
      // deferred items
      List<OotsElementData> localDeferredFSs = deferredFSs;
      deferredFSs = null;

      for (OotsElementData deferredFs : localDeferredFSs) {
        List<XmlAttribute> attrs = deferredFs.attributes;

        // promote the sofa ref (if any) from child element to attribute
        for (XmlElementNameAndContents childElement : deferredFs.childElements) {
          if (childElement.name.qName.equals(CAS.FEATURE_BASE_NAME_SOFA)) {
            attrs.add(new XmlAttribute(CAS.FEATURE_BASE_NAME_SOFA, childElement.contents));
            break;
          }
        }

        attrs.add(new XmlAttribute(ID_ATTR_NAME, deferredFs.xmiId)); // other use (for oots data)
                                                                     // wants this attribute
                                                                     // excluded

        readFS(deferredFs.elementName.nsUri, deferredFs.elementName.localName,
                deferredFs.elementName.qName, deferredFs.getAttributes());
        try {
          isDoingDeferredChildElements = true;
          for (NameMultiValue nmv : deferredFs.multiValuedFeatures) {
            int featcode = handleFeatMultiValueFromName(currentType, currentFs, nmv.name,
                    nmv.values);
            if (featcode != -1 && this.featsSeen != null) {
              this.featsSeen.add(featcode);
            }
          } // end of for loop over child elements
        } finally {
          isDoingDeferredChildElements = false;
        }

      }
    }
  }

  private TypeSystemImpl ts;

  private Map<String, String> xmiNamespaceToUimaNamespaceMap = new HashMap<>();

  /**
   * Create a new deserializer from a type system.
   * <p>
   * Note: all CAS arguments later supplied to <code>getXCASHandler()</code> must have this type
   * system as their type system.
   * 
   * @param ts
   *          The type system of the CASes to be deserialized.
   * @param uimaContext
   *          the UIMA Context to use for the deserialization
   */
  public XmiCasDeserializer(TypeSystem ts, UimaContext uimaContext) {
    this.ts = (TypeSystemImpl) ts;
  }

  public XmiCasDeserializer(TypeSystem ts) {
    this(ts, null);
  }

  // @formatter:off
  /* ========================================================= */
  /*      getters for Xmi Cas Handler                          */
  /*   Arguments:                                              */
  /*     cas                                                   */
  /*     lenient                                               */
  /*     sharedData                                            */
  /*     mergePoint (or -1)                                    */
  /*     allow preexisting                                     */
  /*                                                           */
  /* existing variants:                                        */
  /*     cas                                                   */
  /*     cas, lenient                                          */
  /*     cas, lenient, sharedData                              */
  /*     cas, lenient, sharedData, mergePoint                  */
  /*     cas, lenient, sharedData, mergePoint, allow           */
  /* ========================================================= */
  // @formatter:on

  /**
   * Create a default handler for deserializing a CAS from XMI.
   * 
   * @param cas
   *          This CAS will be used to hold the data deserialized from the XMI
   * 
   * @return The <code>DefaultHandler</code> to pass to the SAX parser.
   */
  public DefaultHandler getXmiCasHandler(CAS cas) {
    return getXmiCasHandler(cas, false);
  }

  /**
   * Create a default handler for deserializing a CAS from XMI. By default this is not lenient,
   * meaning that if the XMI references Types that are not in the Type System, an Exception will be
   * thrown. Use {@link XmiCasDeserializer#getXmiCasHandler(CAS,boolean)} to turn on lenient mode
   * and ignore any unknown types.
   * 
   * @param cas
   *          This CAS will be used to hold the data deserialized from the XMI
   * @param lenient
   *          if true, unknown Types will be ignored. If false, unknown Types will cause an
   *          exception. The default is false.
   * 
   * @return The <code>DefaultHandler</code> to pass to the SAX parser.
   */
  public DefaultHandler getXmiCasHandler(CAS cas, boolean lenient) {
    return getXmiCasHandler(cas, lenient, null);
  }

  /**
   * Create a default handler for deserializing a CAS from XMI.
   * 
   * @param cas
   *          This CAS will be used to hold the data deserialized from the XMI
   * @param lenient
   *          if true, unknown Types will be ignored. If false, unknown Types will cause an
   *          exception. The default is false.
   * @param sharedData
   *          data structure used to allow the XmiCasSerializer and XmiCasDeserializer to share
   *          information.
   * 
   * @return The <code>DefaultHandler</code> to pass to the SAX parser.
   */
  public DefaultHandler getXmiCasHandler(CAS cas, boolean lenient,
          XmiSerializationSharedData sharedData) {
    return getXmiCasHandler(cas, lenient, sharedData, -1);
  }

  /**
   * Create a default handler for deserializing a CAS from XMI.
   * 
   * @param cas
   *          This CAS will be used to hold the data deserialized from the XMI
   * @param lenient
   *          if true, unknown Types will be ignored. If false, unknown Types will cause an
   *          exception. The default is false.
   * @param sharedData
   *          data structure used to allow the XmiCasSerializer and XmiCasDeserializer to share
   *          information.
   * @param mergePoint
   *          (represented as an xmiId, not an fsAddr) used to support merging multiple XMI CASes.
   *          If the mergePoint is negative, "normal" deserialization will be done, meaning the
   *          target CAS will be reset and the entire XMI content will be deserialized. If the
   *          mergePoint is nonnegative (including 0), the target CAS will not be reset, and only
   *          Feature Structures whose xmi:id is strictly greater than the mergePoint value will be
   *          deserialized.
   * @return The <code>DefaultHandler</code> to pass to the SAX parser.
   */
  public DefaultHandler getXmiCasHandler(CAS cas, boolean lenient,
          XmiSerializationSharedData sharedData, int mergePoint) {
    return getXmiCasHandler(cas, lenient, sharedData, mergePoint, AllowPreexistingFS.ignore);
  }

  /**
   * Create a default handler for deserializing a CAS from XMI. By default this is not lenient,
   * meaning that if the XMI references Types that are not in the Type System, an Exception will be
   * thrown. Use {@link XmiCasDeserializer#getXmiCasHandler(CAS,boolean)} to turn on lenient mode
   * and ignore any unknown types.
   * 
   * @param cas
   *          This CAS will be used to hold the data deserialized from the XMI
   * @param lenient
   *          if true, unknown Types will be ignored. If false, unknown Types will cause an
   *          exception. The default is false.
   * @param sharedData
   *          data structure used to allow the XmiCasSerializer and XmiCasDeserializer to share
   *          information.
   * @param mergePoint
   *          used to support merging multiple XMI CASes. If the mergePoint is negative, "normal"
   *          deserialization will be done, meaning the target CAS will be reset and the entire XMI
   *          content will be deserialized. If the mergePoint is nonnegative (including 0), the
   *          target CAS will not be reset, and only Feature Structures whose xmi:id is strictly
   *          greater than the mergePoint value will be deserialized.
   * @param allow
   *          indicates what action to do if a pre-existing FS is found
   * @return The <code>DefaultHandler</code> to pass to the SAX parser.
   */
  public DefaultHandler getXmiCasHandler(CAS cas, boolean lenient,
          XmiSerializationSharedData sharedData, int mergePoint, AllowPreexistingFS allow) {
    return new XmiCasDeserializerHandler((CASImpl) cas, lenient, sharedData, mergePoint, allow);
  }

  /**
   * Deserializes a CAS from XMI.
   * 
   * @param aStream
   *          input stream from which to read the XMI document
   * @param aCAS
   *          CAS into which to deserialize. This CAS must be set up with a type system that is
   *          compatible with that in the XMI
   * 
   * @throws SAXException
   *           if an XML Parsing error occurs
   * @throws IOException
   *           if an I/O failure occurs
   */
  public static void deserialize(InputStream aStream, CAS aCAS) throws SAXException, IOException {
    XmiCasDeserializer.deserialize(aStream, aCAS, false, null, -1);
  }

  /**
   * Deserializes a CAS from XMI.
   * 
   * @param aStream
   *          input stream from which to read the XMI document
   * @param aCAS
   *          CAS into which to deserialize. This CAS must be set up with a type system that is
   *          compatible with that in the XMI
   * @param aLenient
   *          if true, unknown Types will be ignored. If false, unknown Types will cause an
   *          exception. The default is false.
   * 
   * @throws SAXException
   *           if an XML Parsing error occurs
   * @throws IOException
   *           if an I/O failure occurs
   */
  public static void deserialize(InputStream aStream, CAS aCAS, boolean aLenient)
          throws SAXException, IOException {
    deserialize(aStream, aCAS, aLenient, null, -1);
  }

  /**
   * Deserializes a CAS from XMI.
   * 
   * @param aStream
   *          input stream from which to read the XMI document
   * @param aCAS
   *          CAS into which to deserialize. This CAS must be set up with a type system that is
   *          compatible with that in the XMI
   * @param aLenient
   *          if true, unknown Types will be ignored. If false, unknown Types will cause an
   *          exception. The default is false.
   * @param aSharedData
   *          an optional container for data that is shared between the {@link XmiCasSerializer} and
   *          the {@link XmiCasDeserializer}. See the JavaDocs for
   *          {@link XmiSerializationSharedData} for details.
   * 
   * @throws SAXException
   *           if an XML Parsing error occurs
   * @throws IOException
   *           if an I/O failure occurs
   */
  public static void deserialize(InputStream aStream, CAS aCAS, boolean aLenient,
          XmiSerializationSharedData aSharedData) throws SAXException, IOException {
    deserialize(aStream, aCAS, aLenient, aSharedData, -1);
  }

  /**
   * Deserializes a CAS from XMI. This version of this method supports merging multiple XMI
   * documents into a single CAS.
   * 
   * @param aStream
   *          input stream from which to read the XMI document
   * @param aCAS
   *          CAS into which to deserialize. This CAS must be set up with a type system that is
   *          compatible with that in the XMI
   * @param aLenient
   *          if true, unknown Types will be ignored. If false, unknown Types will cause an
   *          exception. The default is false.
   * @param aSharedData
   *          a container for data that is shared between the {@link XmiCasSerializer} and the
   *          {@link XmiCasDeserializer}. See the JavaDocs for {@link XmiSerializationSharedData}
   *          for details.
   * @param aMergePoint
   *          used to support merging multiple XMI CASes. If the mergePoint is negative, "normal"
   *          deserialization will be done, meaning the target CAS will be reset and the entire XMI
   *          content will be deserialized. If the mergePoint is nonnegative (including 0), the
   *          target CAS will not be reset, and only Feature Structures whose xmi:id is strictly
   *          greater than the mergePoint value will be deserialized.
   * @throws SAXException
   *           if an XML Parsing error occurs
   * @throws IOException
   *           if an I/O failure occurs
   */
  public static void deserialize(InputStream aStream, CAS aCAS, boolean aLenient,
          XmiSerializationSharedData aSharedData, int aMergePoint)
          throws SAXException, IOException {
    XMLReader xmlReader = XMLUtils.createXMLReader();
    XmiCasDeserializer deser = new XmiCasDeserializer(aCAS.getTypeSystem());
    ContentHandler handler = deser.getXmiCasHandler(aCAS, aLenient, aSharedData, aMergePoint);
    xmlReader.setContentHandler(handler);
    xmlReader.parse(new InputSource(aStream));
  }

//@formatter:off
  /**
   * Deserializes a CAS from XMI. This version of this method supports deserializing XMI document
   * containing only deltas. The Delta CAS XMI is in the same form as a complete CAS XMI but only
   * consists of new and modified FSs and updates to Views.
   * 
   * This API is for reducing the overhead associated with serialization when calling a remote
   * service. The service can send back only the deltas which are deserialized into the original
   * outgoing CAS.
   * 
   * 
   * @param aStream
   *          input stream from which to read the XMI document
   * @param aCAS
   *          CAS into which to deserialize. This CAS must be set up with a type system that is
   *          compatible with that in the XMI
   * @param aLenient
   *          if true, unknown Types will be ignored. If false, unknown Types will cause an
   *          exception. The default is false.
   * @param aSharedData
   *          a container for data that is shared between the {@link XmiCasSerializer} and the
   *          {@link XmiCasDeserializer}. See the JavaDocs for {@link XmiSerializationSharedData}
   *          for details.
   * @param aMergePoint
   *          used to support merging multiple XMI CASes. If the mergePoint is negative, "normal"
   *          deserialization will be done, meaning the target CAS will be reset and the entire XMI
   *          content will be deserialized. If the mergePoint is nonnegative (including 0), the
   *          target CAS will not be reset, and only Feature Structures whose xmi:id is strictly
   *          greater than the mergePoint value will be deserialized.
   * @param allowPreexistingFS
   *            used when deserializing delta CAS whether to allow, disallow or
   *            ignore elements representign preexisting FSs or preexisting 
   *            FSs updates in View element.
   *            if IGNORE, FSs below the mergePoint are ignored and only new FSs are processed.
   *            if ALLOW,  FSs below the mergePoint are processed as well as new FSs.
   *            if DISALLOW FSs below mergePoint will cause serialization to fail. FSs below
   *               the mergePoint referenced in View element will be flagged as an error condition
   *               and will not modifiy the CAS being filled and an exception reporting this will
   *               be thrown at the end of deserialization.
   * 
   *
   * @throws SAXException
   *           if an XML Parsing error occurs
   * @throws IOException
   *           if an I/O failure occurs
   * 
   * NOTES:
   *     It is expected that Delta CAS serialization will serialize 
   *     modified preexisting FSs first so that disallowed preexisting
   *     FSs are detected at the start and the CAS being filled is
   *     left untouched.  If disallowed prexisting FS is encountered in
   *     the View element, the FS is ignored and the deserialization completes
   *           but throws an exception at the end.
   * 
   *     Possible performance issue with StringListFS. 
   *     When processing String, StringArrayFS and StringListFS features of a preexisting FS, 
   *     the string value in the CAS is updated only if it is not equal to the incoming string value.
   *     Processing of a StringListFS where a new string value has been inserted, all subsequent
   *     strings in the list will be updated with new strings.   
   */
//@formatter:on
  public static void deserialize(InputStream aStream, CAS aCAS, boolean aLenient,
          XmiSerializationSharedData aSharedData, int aMergePoint,
          AllowPreexistingFS allowPreexistingFS) throws SAXException, IOException {
    XMLReader xmlReader = XMLUtils.createXMLReader();
    XmiCasDeserializer deser = new XmiCasDeserializer(aCAS.getTypeSystem());
    ContentHandler handler = deser.getXmiCasHandler(aCAS, aLenient, aSharedData, aMergePoint,
            allowPreexistingFS);
    xmlReader.setContentHandler(handler);
    xmlReader.parse(new InputSource(aStream));
  }

  /**
   * Converts an XMI element name to a UIMA-style dotted type name.
   * 
   * @param nsUri
   *          the namespace URI of the XMI element
   * @param localName
   *          the local name of the XMI element
   * 
   * @return the UIMA type name corresponding to the XMI element name
   */
  private String xmiElementName2uimaTypeName(String nsUri, String localName) throws SAXException {
    // check map first to see if we've already computed the namespace mapping
    String uimaNamespace = xmiNamespaceToUimaNamespaceMap.get(nsUri);
    if (uimaNamespace == null) {
      // check for the special "no-namespace" URI, which is used for UIMA types with no namespace
      if (XmiCasSerializer.DEFAULT_NAMESPACE_URI.equals(nsUri)) {
        uimaNamespace = "";
      } else {
        // Our convention is that the UIMA namespace is the URI path, with leading slashes
        // removed, trailing ".ecore" removed, and internal slashes converted to dots
        java.net.URI uri;
        try {
          uri = new URI(nsUri);
        } catch (URISyntaxException e) {
          throw new SAXException(e);
        }
        String path = uri.getPath();
        while (path.startsWith("/")) {
          path = path.substring(1);
        }
        if (path.endsWith(".ecore")) {
          path = path.substring(0, path.length() - 6);
        }
        uimaNamespace = path.replace('/', '.') + '.'; // include trailing dot for convenience
      }
      xmiNamespaceToUimaNamespaceMap.put(nsUri, uimaNamespace);
    }
    return uimaNamespace + localName;
  }

  /*
   * Adds a feature sturcture to the out-of-typesystem data. Also sets the
   * this.outOfTypeSystemElement field, which is referred to later if we have to handle features
   * recorded as child elements.
   */
  private void addOutOfTypeSystemAttributes(OotsElementData ootsElem, Attributes attrs) {

    for (int i = 0; i < attrs.getLength(); i++) {
      String attrName = attrs.getQName(i);
      String attrValue = attrs.getValue(i);
      if (!attrName.equals(ID_ATTR_NAME)) {
        ootsElem.attributes.add(new XmlAttribute(attrName, attrValue));
      }
    }
  }

  private void report0xmiId() {
    Throwable t = new Throwable();
    System.err.println("Debug 0 xmiId encountered where not expected");
    t.printStackTrace();
  }
}
