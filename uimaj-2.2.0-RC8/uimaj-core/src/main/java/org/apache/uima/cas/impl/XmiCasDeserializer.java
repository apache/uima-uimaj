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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.XmiSerializationSharedData.OotsElementData;
import org.apache.uima.internal.util.I18nUtil;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.XmlAttribute;
import org.apache.uima.internal.util.XmlElementName;
import org.apache.uima.internal.util.XmlElementNameAndContents;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * XMI CAS deserializer. Used to read in a CAS from XML Metadata Interchange (XMI) format.
 */
public class XmiCasDeserializer {

  private class XmiCasDeserializerHandler extends DefaultHandler {
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

    private String ID_ATTR_NAME = "xmi:id";

    // SAX locator. Used for error message generation.
    private Locator locator;

    // The CAS we're filling.
    private CASImpl casBeingFilled;

    // Store address of every FS we've deserialized, since we need to back
    // and apply fix-ups afterwards.
    private IntVector deserializedFsAddrs;

    // Store a separate vector of FSList nodes that were deserialized from multivalued properties.
    // These are special because their "head" feature needs remapping but their "tail" feature
    // doesn't.
    private IntVector fsListNodesFromMultivaluedProperties;

    // What we expect next.
    private int state;

    // StringBuffer to accumulate text.
    private StringBuffer buffer;

    // The address of the most recently created FS. Needed for embedded
    // feature values.
    private int currentAddr;

    // The type of the most recently created FS. Needed for arrays, also
    // useful for embedded feature values.
    private TypeImpl currentType;

    // the ID and values of arrays are stored on startElement, then used on
    // endElement to actually create the array. This is because in the case of
    // String arrays serialized with the values as child elements, we can't create
    // the array until we've seen all of the child elements.
    private int currentArrayId;

    private List currentArrayElements;

    // Used for keeping track of multi-valued features read from subelements.
    // Keys are feature names, values are ArrayLists of strings,
    // where each String is one of the values to be assigned to the feature.
    private Map multiValuedFeatures = new TreeMap();

    // SofaFS type
    private int sofaTypeCode;

    // Sofa number feature code
    private int sofaNumFeatCode;

    // Annotation:sofa feature code
    private int sofaFeatCode;

    // Store IndexRepositories in a vector;
    private ArrayList indexRepositories;

    // and views too
    private ArrayList views;

    // utilities for handling CAS list types
    private ListUtils listUtils;

    // type of each feature, according to constants below
    private int[] featureType;

    // true if unknown types should be ignored; false if they should cause an error
    boolean lenient;

    // number of oustanding startElement events that we are ignoring
    // we add 1 when an ignored element starts and subtract 1 when an ignored
    // element ends
    private int ignoreDepth = 0;

    // map from namespace prefixes to URIs. Allows namespace resolution even
    // with a non-namespace-enabled SAX parser.
    private HashMap nsPrefixToUriMap = new HashMap();

    // container for data shared between the XmiCasSerialier and
    // XmiDeserializer, to support things such as consistency of IDs across
    // multiple serializations.  This is also where the map from xmi:id to
    // FS address is stored.
    private XmiSerializationSharedData sharedData;

    // number of Sofas found so far
    private int nextSofaNum;
    
    //used for merging multiple XMI CASes into one CAS object.
    private int mergePoint;
    
    //Current out-of-typesystem element, if any
    private OotsElementData outOfTypeSystemElement = null;

    /**
     * Creates a SAX handler used for deserializing an XMI CAS.
     * @param aCAS CAS to deserialize into
     * @param lenient if true, unknown types/features result in an
     *   exception.  If false, unknown types/features are ignored.
     * @param sharedData data structure used to allow the XmiCasSerializer and 
     *   XmiCasDeserializer to share information.
     * @param mergePoint used to support merging multiple XMI CASes.  If the
     *   mergePoint is negative, "normal" deserialization will be done,
     *   meaning the target CAS will be reset and the entire XMI content will
     *   be deserialized.  If the mergePoint is nonnegative (including 0), the 
     *   target CAS will not be reset, and only Feature Structures whose
     *   xmi:id is strictly greater than the mergePoint value will be
     *   deserialized. 
     */
    private XmiCasDeserializerHandler(CASImpl aCAS, boolean lenient,
            XmiSerializationSharedData sharedData, int mergePoint) {
      super();
      this.casBeingFilled = aCAS.getBaseCAS();
      this.lenient = lenient;
      this.sharedData = 
        sharedData != null ? sharedData : new XmiSerializationSharedData();
      this.mergePoint = mergePoint;
      if (mergePoint < 0) {
        //If not merging, reset the CAS. 
        //Necessary to get Sofas to work properly.
        casBeingFilled.resetNoQuestions();
        
        // clear ID mappings stored in the SharedData (from previous deserializations)
        this.sharedData.clearIdMap();
      }
      this.deserializedFsAddrs = new IntVector();
      this.fsListNodesFromMultivaluedProperties = new IntVector();
      this.buffer = new StringBuffer();
      this.indexRepositories = new ArrayList();
      this.views = new ArrayList();
      indexRepositories.add(this.casBeingFilled.getBaseIndexRepository());
      // There should always be another index for the Initial View
      indexRepositories.add(this.casBeingFilled.getView(CAS.NAME_DEFAULT_SOFA).getIndexRepository());
      //add an entry to indexRepositories for each Sofa in the CAS (which can only happen if
      //a mergePoint was specified)
      FSIterator sofaIter = this.casBeingFilled.getSofaIterator();
      while(sofaIter.hasNext()) {
        SofaFS sofa = (SofaFS)sofaIter.next();
        if (sofa.getSofaRef() == 1) {
          casBeingFilled.registerInitialSofa();
        } else {
          // add indexRepo for views other than the initial view
          indexRepositories.add(casBeingFilled.getSofaIndexRepository(sofa));
        }        
      }      
      final TypeSystemImpl tsOfReceivingCas = casBeingFilled.getTypeSystemImpl();
      this.sofaTypeCode = tsOfReceivingCas.ll_getCodeForTypeName(CAS.TYPE_NAME_SOFA);
      this.sofaNumFeatCode = tsOfReceivingCas.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_SOFANUM);
      this.sofaFeatCode = tsOfReceivingCas.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_SOFA);
      this.nextSofaNum = 2;
      this.listUtils = new ListUtils(casBeingFilled, UIMAFramework.getLogger(XmiCasDeserializer.class), null);

      // populate feature type table
      this.featureType = new int[tsOfReceivingCas.getNumberOfFeatures() + 1];
      FeatureImpl feat;
      Iterator it = tsOfReceivingCas.getFeatures();
      while (it.hasNext()) {
        feat = (FeatureImpl) it.next();
        featureType[feat.getCode()] = classifyType(tsOfReceivingCas.range(feat.getCode()));
      }
    }

    private final void resetBuffer() {
      this.buffer = new StringBuffer();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#startDocument()
     */
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
     *      java.lang.String, org.xml.sax.Attributes)
     */
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
          // if we're doing merging, skip elements whose ID is <= mergePoint
          if (this.mergePoint >= 0) {
            String id = attrs.getValue(ID_ATTR_NAME);
            if (id != null) {
              int idInt = Integer.parseInt(id);
              if (idInt <= this.mergePoint) {
                this.state = IGNORING_XMI_ELEMENTS_STATE;
                this.ignoreDepth++;
                return;
              }
            }
          }
          if (nameSpaceURI == null || nameSpaceURI.length() == 0) {
            // parser may not be namespace-enabled, so try to resolve NS ourselves
            int colonIndex = qualifiedName.indexOf(':');
            if (colonIndex != -1) {
              String prefix = qualifiedName.substring(0, colonIndex);
              nameSpaceURI = (String) nsPrefixToUriMap.get(prefix);
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
          //parsing a feature recorded as a child element
          //check for an "href" feature, used for references
          String href = attrs.getValue("href");
          if (href != null && href.startsWith("#")) {           
            //for out-of-typesystem objects, there's special handling here
            //to keep track of the fact this was an href so we re-serialize
            //correctly.
            if (this.outOfTypeSystemElement != null) {
              XmlElementName elemName = new XmlElementName(nameSpaceURI, localName, qualifiedName);
              List ootsAttrs = new ArrayList();
              ootsAttrs.add(new XmlAttribute("href", href));
              XmlElementNameAndContents elemWithContents = new XmlElementNameAndContents(elemName, null, ootsAttrs);
              this.outOfTypeSystemElement.childElements.add(elemWithContents);
            }
            else {
              //In-typesystem FS, so we can forget this was an href and just add
              //the integer value, which will be interpreted as a reference later.
              //NOTE: this will end up causing it to be reserialized as an attribute
              //rather than an element, but that is not in violation of the XMI spec.
              ArrayList valueList = (ArrayList) this.multiValuedFeatures.get(qualifiedName);
              if (valueList == null) {
                valueList = new ArrayList();
                this.multiValuedFeatures.put(qualifiedName, valueList);
              }
              valueList.add(href.substring(1));
            }                         
            state = REF_FEAT_STATE;
          }
          else {
            //non-reference feature, expecting feature value as character content
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

    // Create a new FS.
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
          processView(attrs.getValue("sofa"), attrs.getValue("members"));
          return;
        }
        // type is not in our type system
        if (!lenient) {
          throw createException(XCASParsingException.UNKNOWN_TYPE, typeName);
        } else {
          addToOutOfTypeSystemData(
              new XmlElementName(nameSpaceURI, localName, qualifiedName), attrs);                  
        }
        return;
      } else if (casBeingFilled.isArrayType(currentType)) {
        // store ID and array values (if specified as attribute).
        // we will actually create the array later, in endElement.
        String idStr = attrs.getValue(ID_ATTR_NAME);
        currentArrayId = idStr == null ? -1 : Integer.parseInt(idStr);
        String elements = attrs.getValue("elements");

        // special parsing for byte arrays (they are serialized as a hex
        // string. And we create them here instead of parsing to a string
        // array, for efficiency.
        if (casBeingFilled.isByteArrayType(currentType)) {
          createByteArray(elements, currentArrayId);
        } else {
          if (elements != null) {
            String[] parsedElements = parseArray(elements);
            currentArrayElements = Arrays.asList(parsedElements);
          } else {
            currentArrayElements = null;
          }
        }
      } else {
        final int addr = casBeingFilled.ll_createFS(currentType.getCode());
        readFS(addr, attrs);
      }
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
    private void processView(String sofa, String membersString) {
      // TODO: this requires View to come AFTER all of its members
      if (membersString != null) {
        // a view with no Sofa will be added to the 1st, _InitialView, index
        int sofaNum = 1;
        if (sofa != null) {
          // translate sofa's xmi:id into its sofanum
          int sofaXmiId = Integer.parseInt(sofa);
          int sofaAddr = getFsAddrForXmiId(sofaXmiId);
          sofaNum = casBeingFilled.getFeatureValue(sofaAddr, sofaNumFeatCode);
        }
        FSIndexRepositoryImpl indexRep = (FSIndexRepositoryImpl) indexRepositories.get(sofaNum);

        // TODO: optimize by going straight to int[] without going through
        // intermediate String[]?
        String[] members = parseArray(membersString);
        for (int i = 0; i < members.length; i++) {
          int id = Integer.parseInt(members[i]);
          //if merging, don't try to index anything below the merge point
          if (id <= this.mergePoint) {
            continue;
          }
          // have to map each ID to its "real" address (TODO: optimize?)
          try {
            int addr = getFsAddrForXmiId(id);
            indexRep.addFS(addr);
          } catch (NoSuchElementException e) {
            if (!lenient) {
              throw e;
            }
            else {
              //unknown view member may be an OutOfTypeSystem FS
              this.sharedData.addOutOfTypeSystemViewMember(sofa, members[i]);
            }
          }
        }
      }
    }

    /**
     * 
     * @param addr
     * @param attrs
     * @throws SAXParseException
     */
    private void readFS(final int addr, Attributes attrs) throws SAXParseException {
      // Hang on to address for handle features encoded as child elements
      this.currentAddr = addr;
      int id = -1;
      String attrName, attrValue;
      final int typeCode = casBeingFilled.getHeapValue(addr);
      final Type type = casBeingFilled.getTypeSystemImpl().ll_getTypeForCode(typeCode);
      int thisSofaNum = 0;

      if (sofaTypeCode == typeCode) {
        String sofaID = attrs.getValue(CAS.FEATURE_BASE_NAME_SOFAID);
        if (sofaID.equals(CAS.NAME_DEFAULT_SOFA) || sofaID.equals("_DefaultTextSofaName")) {
          // initial view Sofa always has sofaNum = 1
          thisSofaNum = 1;
        } else {
          thisSofaNum = this.nextSofaNum++;
        }
      }

      for (int i = 0; i < attrs.getLength(); i++) {
        attrName = attrs.getQName(i);
        attrValue = attrs.getValue(i);
        if (attrName.equals(ID_ATTR_NAME)) {
          try {
            id = Integer.parseInt(attrValue);
          } catch (NumberFormatException e) {
            throw createException(XCASParsingException.ILLEGAL_ID, attrValue);
          }
        } else {
          if (sofaTypeCode == typeCode && attrName.equals(CAS.FEATURE_BASE_NAME_SOFAID)) {
            if (attrValue.equals("_DefaultTextSofaName")) {
              // First change old default Sofa name into the new one
              attrValue = CAS.NAME_DEFAULT_SOFA;
            }
          } else if (sofaTypeCode == typeCode && attrName.equals(CAS.FEATURE_BASE_NAME_SOFANUM)) {
            attrValue = Integer.toString(thisSofaNum);
          }
          handleFeature(type, addr, attrName, attrValue);
        }
      }
      if (sofaTypeCode == typeCode) {
        // If a Sofa, create CAS view to get new indexRepository
        SofaFS sofa = (SofaFS) casBeingFilled.createFS(addr);
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
      deserializedFsAddrs.add(addr);
      if (id > 0) {
        sharedData.addIdMapping(addr, id);
      }
    }

    // The definition of a null value. Any other value must be in the expected
    // format.
    private final boolean emptyVal(String val) {
      return ((val == null) || (val.length() == 0));
    }

    private void handleFeature(final Type type, int addr, String featName, String featVal) throws SAXParseException {
      final FeatureImpl feat = (FeatureImpl) type.getFeatureByBaseName(featName);
      if (feat == null) {
        if (!this.lenient) {
          throw createException(XCASParsingException.UNKNOWN_FEATURE, featName);
        }
        else {
          sharedData.addOutOfTypeSystemAttribute(addr, featName, featVal);
        }
        return;
      }
      handleFeature(addr, feat.getCode(), featVal);
    }

    private void handleFeature(final Type type, int addr, String featName, List featVals) throws SAXParseException {
      final FeatureImpl feat = (FeatureImpl) type.getFeatureByBaseName(featName);
      if (feat == null) {
        if (!this.lenient) {
          throw createException(XCASParsingException.UNKNOWN_FEATURE, featName);
        }
        else {
          sharedData.addOutOfTypeSystemChildElements(addr, featName, featVals);
        }
        return;
      }
      handleFeature(addr, feat.getCode(), featVals);
    }

    /**
     * Set a CAS feature from an XMI attribute.
     * 
     * @param addr
     *          address of FS containing the feature
     * @param featCode
     *          code of feature to set
     * @param featVal
     *          string representation of the feature value
     * @throws SAXParseException
     */
    private void handleFeature(int addr, int featCode, String featVal) throws SAXParseException {
      switch (featureType[featCode]) {
        case LowLevelCAS.TYPE_CLASS_INT: {
          try {
            if (!emptyVal(featVal)) {
              if (featCode == sofaFeatCode) {
                // special handling for "sofa" feature of annotation. Need to change
                // it from a sofa reference into a sofa number
                int sofaXmiId = Integer.parseInt(featVal);
                int sofaAddr = getFsAddrForXmiId(sofaXmiId);
                int sofaNum = casBeingFilled.getFeatureValue(sofaAddr, sofaNumFeatCode);
                casBeingFilled.setFeatureValue(addr, featCode, sofaNum);
              } else {
                casBeingFilled.setFeatureValue(addr, featCode, Integer.parseInt(featVal));
              }
            }
          } catch (NumberFormatException e) {
            throw createException(XCASParsingException.INTEGER_EXPECTED, featVal);
          }
          break;
        }
        case LowLevelCAS.TYPE_CLASS_FLOAT:
        case LowLevelCAS.TYPE_CLASS_BOOLEAN:
        case LowLevelCAS.TYPE_CLASS_BYTE:
        case LowLevelCAS.TYPE_CLASS_SHORT:
        case LowLevelCAS.TYPE_CLASS_LONG:
        case LowLevelCAS.TYPE_CLASS_DOUBLE: {
          try {
            if (!emptyVal(featVal)) {
              casBeingFilled.setFeatureValueFromString(addr, featCode, featVal);
              // cas.setFloatValue(addr, featCode, Float.parseFloat(featVal));
            }
          } catch (NumberFormatException e) {
            throw createException(XCASParsingException.FLOAT_EXPECTED, featVal);
          }
          break;
        }
        case LowLevelCAS.TYPE_CLASS_STRING: {
          if (featVal != null) // do not use empty value since that would filter out ""
          {
            casBeingFilled.setStringValue(addr, featCode, featVal);
          }
          break;
        }
        case LowLevelCAS.TYPE_CLASS_FS: {
          try {
            if (!emptyVal(featVal)) {
              casBeingFilled.setFeatureValue(addr, featCode, Integer.parseInt(featVal));
            }
          } catch (NumberFormatException e) {
            throw createException(XCASParsingException.INTEGER_EXPECTED, featVal);
          }
          break;
        }

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
          if (ts.ll_getFeatureForCode(featCode).isMultipleReferencesAllowed()) {
            // do the usual FS deserialization
            try {
              if (!emptyVal(featVal)) {
                casBeingFilled.setFeatureValue(addr, featCode, Integer.parseInt(featVal));
              }
            } catch (NumberFormatException e) {
              throw createException(XCASParsingException.INTEGER_EXPECTED, featVal);
            }
          } else {
            // Do the multivalued property deserialization.
            // However, byte arrays have a special serialization (as hex digits)
            if (featureType[featCode] == LowLevelCAS.TYPE_CLASS_BYTEARRAY) {
              int casArray = createByteArray(featVal, -1);
              casBeingFilled.setFeatureValue(addr, featCode, casArray);
            } else {
              String[] arrayVals = parseArray(featVal);
              handleFeature(addr, featCode, Arrays.asList(arrayVals));
            }
          }
          break;
        }
          // For list types, we do the same as for array types UNLESS we're dealing with
          // the tail feature of another list node. In that case we do the usual FS deserialization.
        case XmiCasSerializer.TYPE_CLASS_INTLIST:
        case XmiCasSerializer.TYPE_CLASS_FLOATLIST:
        case XmiCasSerializer.TYPE_CLASS_STRINGLIST:
        case XmiCasSerializer.TYPE_CLASS_FSLIST: {
          if (ts.ll_getFeatureForCode(featCode).isMultipleReferencesAllowed()) {
            // do the usual FS deserialization
            try {
              if (!emptyVal(featVal)) {
                casBeingFilled.setFeatureValue(addr, featCode, Integer.parseInt(featVal));
              }
            } catch (NumberFormatException e) {
              throw createException(XCASParsingException.INTEGER_EXPECTED, featVal);
            }
          } else // do the multivalued property deserialization, like arrays
          {
            String[] arrayVals = parseArray(featVal);
            handleFeature(addr, featCode, Arrays.asList(arrayVals));
          }
          break;
        }
        default: {
          assert false; // this should be an exhaustive case block
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
        arrayVals = new String[0];
      } else {
        arrayVals = val.split("\\s+");
      }
      return arrayVals;
    }

    /**
     * Set a CAS feature from an array of Strings. This supports the XMI syntax where each value is
     * listed as a separate subelement.
     * 
     * @param addr
     *          address of FS containing the feature
     * @param featCode
     *          code of feature to set
     * @param featVals
     *          List of Strings, each String representing one value for the feature
     * @throws SAXParseException
     */
    private void handleFeature(int addr, int featCode, List featVals) throws SAXParseException {
      switch (featureType[featCode]) {
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
                    "multiple_values_unexpected",
                    new Object[] { ts.ll_getFeatureForCode(featCode).getName() }), locator);
          } else {
            handleFeature(addr, featCode, (String) featVals.get(0));
          }
          break;
        case LowLevelCAS.TYPE_CLASS_INTARRAY:
        case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
        case LowLevelCAS.TYPE_CLASS_STRINGARRAY:
        case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY:
        case LowLevelCAS.TYPE_CLASS_BYTEARRAY:
        case LowLevelCAS.TYPE_CLASS_SHORTARRAY:
        case LowLevelCAS.TYPE_CLASS_LONGARRAY:
        case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY: {
          int casArray = createArray(casBeingFilled.getTypeSystemImpl().range(featCode), featVals, -1);
          casBeingFilled.setFeatureValue(addr, featCode, casArray);
          break;
        }
        case LowLevelCAS.TYPE_CLASS_FSARRAY: {
          int casArray = createArray(casBeingFilled.getTypeSystemImpl().range(featCode), featVals, -1);
          casBeingFilled.setFeatureValue(addr, featCode, casArray);
          break;
        }
        case XmiCasSerializer.TYPE_CLASS_INTLIST: {
          int listFS = listUtils.createIntList(featVals);
          casBeingFilled.setFeatureValue(addr, featCode, listFS);
          break;
        }
        case XmiCasSerializer.TYPE_CLASS_FLOATLIST: {
          int listFS = listUtils.createFloatList(featVals);
          casBeingFilled.setFeatureValue(addr, featCode, listFS);
          break;
        }
        case XmiCasSerializer.TYPE_CLASS_STRINGLIST: {
          int listFS = listUtils.createStringList(featVals);
          casBeingFilled.setFeatureValue(addr, featCode, listFS);
          break;
        }
        case XmiCasSerializer.TYPE_CLASS_FSLIST: {
          // this call, in addition to creating the list in the CAS, also
          // adds each list node ID to the fsListNodesFromMultivaluedProperties list.
          // We need this so we can go back through later and reset the addresses of the
          // "head" features of these lists nodes (but not reset the tail features).
          int listFS = listUtils.createFsList(featVals, fsListNodesFromMultivaluedProperties);
          casBeingFilled.setFeatureValue(addr, featCode, listFS);
          break;
        }
        default: {
          assert false; // this should be an exhaustive case block
        }
      }
    }

    /**
     * Create an array in the CAS
     * 
     * @param arrayType
     *          CAS type code for the array
     * @param values
     *          List of strings, each representing an element in the array
     * @param xmiId
     *          xmi:id assigned to the array object
     * @return
     */
    private int createArray(int arrayType, List values, int xmiId) {

      FeatureStructureImplC fs;
      if (casBeingFilled.isIntArrayType(arrayType)) {
        fs = (FeatureStructureImplC) casBeingFilled.createIntArrayFS(values.size());
      } else if (casBeingFilled.isFloatArrayType(arrayType)) {
        fs = (FeatureStructureImplC) casBeingFilled.createFloatArrayFS(values.size());        
      } else if (casBeingFilled.isStringArrayType(arrayType)) {
        fs = (FeatureStructureImplC) casBeingFilled.createStringArrayFS(values.size());                
      } else if (casBeingFilled.isBooleanArrayType(arrayType)) {
        fs = (FeatureStructureImplC) casBeingFilled.createBooleanArrayFS(values.size());
      } else if (casBeingFilled.isByteArrayType(arrayType)) {
        fs = (FeatureStructureImplC) casBeingFilled.createByteArrayFS(values.size());
      } else if (casBeingFilled.isShortArrayType(arrayType)) {
        fs = (FeatureStructureImplC) casBeingFilled.createShortArrayFS(values.size());
      } else if (casBeingFilled.isLongArrayType(arrayType)) {
        fs = (FeatureStructureImplC) casBeingFilled.createLongArrayFS(values.size());
      } else if (casBeingFilled.isDoubleArrayType(arrayType)) {
        fs = (FeatureStructureImplC) casBeingFilled.createDoubleArrayFS(values.size());
      } else {
        fs = (FeatureStructureImplC) casBeingFilled.createArrayFS(values.size());
      }
      int casArray = fs.getAddress();
      for (int i = 0; i < values.size(); i++) {
        String stringVal = (String) values.get(i);
        casBeingFilled.setArrayValueFromString(casArray, i, stringVal);
      }

      deserializedFsAddrs.add(casArray);
      if (xmiId > 0) {
        sharedData.addIdMapping(casArray, xmiId);
      }
      return casArray;
    }

    /**
     * Create a byte array in the CAS.
     * 
     * @param arrayType
     *          CAS type code for the array
     * @param hexString
     *          value of the byte array as a hex string
     * @return
     */
    private int createByteArray(String hexString, int xmiId) {
      int arrayLen = hexString.length() / 2;
      ByteArrayFS fs = casBeingFilled.createByteArrayFS(arrayLen);
      for (int i = 0; i < arrayLen; i++) {
        byte high = hexCharToByte(hexString.charAt(i * 2));
        byte low = hexCharToByte(hexString.charAt(i * 2 + 1));
        byte b = (byte) ((high << 4) | low);
        fs.set(i, b);
      }

      int arrayAddr = ((FeatureStructureImpl) fs).getAddress();
      deserializedFsAddrs.add(arrayAddr);
      if (xmiId > 0) {
        sharedData.addIdMapping(arrayAddr, xmiId);
      }
      return arrayAddr;
    }

    private byte hexCharToByte(char c) {
      if ('0' <= c && c <= '9')
        return (byte) (c - '0');
      else if ('A' <= c && c <= 'F')
        return (byte) (c - 'A' + 10);
      else if ('1' <= c && c <= 'f')
        return (byte) (c - '1' + 10);
      else
        throw new NumberFormatException("Invalid hex char: " + c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] chars, int start, int length) throws SAXException {
      switch (this.state) {
        case FEAT_CONTENT_STATE:
          buffer.append(chars, start, length);
          break;
        default:
      }
    }

    boolean isAllWhitespace(StringBuffer b) {
      final int len = b.length();
      for (int i = 0; i < len; i++) {
        if (!Character.isWhitespace(b.charAt(i))) {
          return false;
        }
      }
      return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
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
          ArrayList valueList = (ArrayList) this.multiValuedFeatures.get(qualifiedName);
          if (valueList == null) {
            valueList = new ArrayList();
            this.multiValuedFeatures.put(qualifiedName, valueList);
          }
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
          if (this.outOfTypeSystemElement != null) {
            if (!this.multiValuedFeatures.isEmpty()) {
              Iterator iter = this.multiValuedFeatures.entrySet().iterator();
              while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String featName = (String) entry.getKey();
                List featVals = (List) entry.getValue();
                addOutOfTypeSystemFeature(outOfTypeSystemElement, featName, featVals);
              }
            }
            this.outOfTypeSystemElement = null;
          }
          else if (currentType != null) {
            if (casBeingFilled.isArrayType(currentType) && !casBeingFilled.isByteArrayType(currentType)) {
              // create the array now. elements may have been provided either as
              // attributes or child elements, but not both.
              // BUT - not byte arrays! They are created immediately, to avoid
              // the overhead of parsing into a String array first
              if (currentArrayElements == null) // were not specified as attributes
              {
                currentArrayElements = (List) this.multiValuedFeatures.get("elements");
                if (currentArrayElements == null) {
                  currentArrayElements = Collections.EMPTY_LIST;
                }
              }
              createArray(currentType.getCode(), currentArrayElements, currentArrayId);
            } else if (!this.multiValuedFeatures.isEmpty()) {
              Iterator iter = this.multiValuedFeatures.entrySet().iterator();
              while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String featName = (String) entry.getKey();
                List featVals = (List) entry.getValue();
                handleFeature(currentType, currentAddr, featName, featVals);
              }
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
    public void endDocument() throws SAXException {
      // Resolve ID references, and add FSs to indexes
      for (int i = 0; i < deserializedFsAddrs.size(); i++) {
        finalizeFS(deserializedFsAddrs.get(i));
      }
      for (int i = 0; i < fsListNodesFromMultivaluedProperties.size(); i++) {
        remapFSListHeads(fsListNodesFromMultivaluedProperties.get(i));
      }
      // time = System.currentTimeMillis() - time;
      // System.out.println("Done in " + new TimeSpan(time));

      for (int i = 0; i < views.size(); i++) {
        ((CASImpl) views.get(i)).updateDocumentAnnotation();
      }
    }

    /**
     * Adds this FS to the appropriate index, and applies ID remappings. For each nonprimitive,
     * non-multivalued-property feature, we need to update the feature value to point to the correct
     * heap address of the target FS.
     * 
     * @param fsInfo
     */
    private void finalizeFS(int addr) {
      final int type = casBeingFilled.getHeapValue(addr);
      if (casBeingFilled.isArrayType(type)) {
        finalizeArray(type, addr);
        return;
      }
      // remap IDs for all nonprimtive, non-multivalued-property features
      int[] feats = casBeingFilled.getTypeSystemImpl().ll_getAppropriateFeatures(type);
      Feature feat;
      for (int i = 0; i < feats.length; i++) {
        feat = ts.ll_getFeatureForCode(feats[i]);
        int typeCode = ts.ll_getRangeType(feats[i]);
        if (casBeingFilled.ll_isRefType(typeCode)
                && (featureType[feats[i]] == LowLevelCAS.TYPE_CLASS_FS || feat
                        .isMultipleReferencesAllowed())) {
          int featVal = casBeingFilled.getFeatureValue(addr, feats[i]);
          if (featVal != CASImpl.NULL) {
            int fsValAddr = CASImpl.NULL;
            try {
              fsValAddr = getFsAddrForXmiId(featVal);
            } catch (NoSuchElementException e) {
              if (!lenient) {
                throw e;
              }
              else {
                // we may not have deserialized the value of this feature because it 
                // was of unknown type.  We set it to null, and record in the
                // out-of-typesystem data.
                this.sharedData.addOutOfTypeSystemAttribute(
                        addr, feat.getShortName(), Integer.toString(featVal));
              }
            }
            casBeingFilled.setFeatureValue(addr, feats[i], fsValAddr);
          }
        }
      }
    }

    /**
     * Rempas ID for the "head" feature of NonEmptyFSList, but not the "tail" feature. Used for
     * FSList nodes deserialized from multi-valued properties, which already have their tail set
     * correctly.
     * 
     * @param i
     */
    private void remapFSListHeads(int addr) {
      final int type = casBeingFilled.getHeapValue(addr);
      if (!listUtils.isFsListType(type))
        return;
      int[] feats = casBeingFilled.getTypeSystemImpl().ll_getAppropriateFeatures(type);
      if (feats.length == 0)
        return;
      int headFeat = feats[0];
      int featVal = casBeingFilled.getFeatureValue(addr, headFeat);
      if (featVal != CASImpl.NULL) {
        int fsValAddr = CASImpl.NULL;
        try {
          fsValAddr = getFsAddrForXmiId(featVal);
        } catch (NoSuchElementException e) {
          if (!lenient) {
            throw e;
          }
          else {
            //this may be a reference to an out-of-typesystem FS
            this.sharedData.addOutOfTypeSystemAttribute(addr, CAS.FEATURE_BASE_NAME_HEAD, Integer.toString(featVal));
          }
        }
        casBeingFilled.setFeatureValue(addr, headFeat, fsValAddr);
      }
    }

    /**
     * Walk an array, remapping IDs. If called on a primitive array,this method does nothing.
     * 
     * @param type
     *          CAS type code for the array
     * @param addr
     *          address of the array
     */
    private void finalizeArray(int type, int addr) {
      if (!casBeingFilled.isFSArrayType(type)) {
        // Nothing to do.
        return;
      }
      final int size = casBeingFilled.ll_getArraySize(addr);
      for (int i = 0; i < size; i++) {
        int arrayVal = casBeingFilled.getArrayValue(addr, i);
        if (arrayVal != CASImpl.NULL) {
          int arrayValAddr = CASImpl.NULL;
          try {
            arrayValAddr = getFsAddrForXmiId(arrayVal);
          } catch (NoSuchElementException e) {
            if (!lenient) {
              throw e;
            }
            else {  
              // the array element may be out of typesystem.  In that case set it
              // to null, but record the id so we can add it back on next serialization.
              this.sharedData.addOutOfTypeSystemArrayElement(addr, i, arrayVal);
            }
          }
          casBeingFilled.setArrayValue(addr, i, arrayValAddr);
        }
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
    public void error(SAXParseException e) throws SAXException {
      throw e;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    public void fatalError(SAXParseException e) throws SAXException {
      throw e;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
     */
    public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {
      // Since we're not validating, we don't need to do anything; this won't
      // be called.
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
     */
    public void setDocumentLocator(Locator loc) {
      // System.out.println("Setting document locator.");
      this.locator = loc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    public void warning(SAXParseException e) throws SAXException {
      throw e;
    }

    /**
     * Classifies a type. This returns an integer code identifying the type as one of the primitive
     * types, one of the array types, one of the list types, or a generic FS type (anything else).
     * <p>
     * The {@link LowLevelCAS#ll_getTypeClass(int)} method classifies primitives and array types,
     * but does not have a special classification for list types, which we need for XMI
     * serialization. Therefore, in addition to the type codes defined on {@link LowLevelCAS}, this
     * method can return one of the type codes TYPE_CLASS_INTLIST, TYPE_CLASS_FLOATLIST,
     * TYPE_CLASS_STRINGLIST, or TYPE_CLASS_FSLIST defined on {@link XmiCasSerializer} interface.
     * 
     * @param type
     *          the type to classify
     * @return one of the TYPE_CLASS codes defined on {@link LowLevelCAS} or on the
     *         {@link XmiCasSerializer} interface.
     */
    private final int classifyType(int type) {
      // For most most types
      if (listUtils.isIntListType(type)) {
        return XmiCasSerializer.TYPE_CLASS_INTLIST;
      }
      if (listUtils.isFloatListType(type)) {
        return XmiCasSerializer.TYPE_CLASS_FLOATLIST;
      }
      if (listUtils.isStringListType(type)) {
        return XmiCasSerializer.TYPE_CLASS_STRINGLIST;
      }
      if (listUtils.isFsListType(type)) {
        return XmiCasSerializer.TYPE_CLASS_FSLIST;
      }
      return casBeingFilled.ll_getTypeClass(type);
    }
    
    /**
     * Gets the FS address into which the XMI element with the given ID
     * was deserialized.  This method supports merging multiple XMI documents
     * into a single CAS, by checking the XmiSerializationSharedData
     * structure to get the address of elements that were skipped during this
     * deserialization but were deserialized during a previous deserialization.
     * 
     * @param xmiId
     * @return
     */
    private int getFsAddrForXmiId(int xmiId) {
      int addr = sharedData.getFsAddrForXmiId(xmiId);
      if (addr > 0)
        return addr;
      else
        throw new java.util.NoSuchElementException();
    }
    
    /**
     * Adds a feature sturcture to the out-of-typesystem data.  Also sets the
     * this.outOfTypeSystemElement field, which is referred to later if we have to
     * handle features recorded as child elements.
     */
    private void addToOutOfTypeSystemData(XmlElementName xmlElementName, Attributes attrs)
            throws XCASParsingException {
      this.outOfTypeSystemElement = new OotsElementData();
      this.outOfTypeSystemElement.elementName = xmlElementName;
      String attrName, attrValue;
      for (int i = 0; i < attrs.getLength(); i++) {
        attrName = attrs.getQName(i);
        attrValue = attrs.getValue(i);
        if (attrName.equals(ID_ATTR_NAME)) {
          this.outOfTypeSystemElement.xmiId = attrValue;
        }
        else {
          this.outOfTypeSystemElement.attributes.add(
                  new XmlAttribute(attrName, attrValue));
        }
      }
      this.sharedData.addOutOfTypeSystemElement(this.outOfTypeSystemElement);
    }    

    /**
     * Adds a feature to the out-of-typesystem features list.
     * @param ootsElem object to which to add the feature
     * @param featName name of feature
     * @param featVals feature values, as a list of strings
     */
    private void addOutOfTypeSystemFeature(OotsElementData ootsElem, String featName, List featVals) {
      Iterator iter = featVals.iterator();
      XmlElementName elemName = new XmlElementName(null,featName,featName);
      while (iter.hasNext()) {
        ootsElem.childElements.add(new XmlElementNameAndContents(elemName, (String)iter.next()));
      }
    } 
  }
  
  private TypeSystemImpl ts;

  private Map xmiNamespaceToUimaNamespaceMap = new HashMap();

  /**
   * Create a new deserializer from a type system. Note: all CAS arguments later supplied to
   * <code>getXCASHandler()</code> must have this type system as their type system.
   * 
   * @param ts
   *          The type system of the CASes to be deserialized.
   */
  public XmiCasDeserializer(TypeSystem ts, UimaContext uimaContext) {
    super();
    this.ts = (TypeSystemImpl) ts;
  }

  public XmiCasDeserializer(TypeSystem ts) {
    this(ts, null);
  }

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
    return new XmiCasDeserializerHandler((CASImpl) cas, lenient, null, -1);
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
   * 
   * @return The <code>DefaultHandler</code> to pass to the SAX parser.
   */
  public DefaultHandler getXmiCasHandler(CAS cas, boolean lenient,
          XmiSerializationSharedData sharedData) {
    return new XmiCasDeserializerHandler((CASImpl) cas, lenient, sharedData, -1);
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
   * @return The <code>DefaultHandler</code> to pass to the SAX parser.
   */
  public DefaultHandler getXmiCasHandler(CAS cas, boolean lenient,
          XmiSerializationSharedData sharedData, int mergePoint) {
    return new XmiCasDeserializerHandler((CASImpl) cas, lenient, sharedData, mergePoint);
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
   *          input stream from which to read the XCMI document
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
   *          input stream from which to read the XCMI document
   * @param aCAS
   *          CAS into which to deserialize. This CAS must be set up with a type system that is
   *          compatible with that in the XMI
   * @param aLenient
   *          if true, unknown Types will be ignored. If false, unknown Types will cause an
   *          exception. The default is false.
   * @param aSharedData
   *          an optional container for data that is shared between the {@link XmiCasSerializer} and the 
   *          {@link XmiCasDeserializer}.  See the JavaDocs for {@link XmiSerializationSharedData} for details.
   * 
   * @throws SAXException
   *           if an XML Parsing error occurs
   * @throws IOException
   *           if an I/O failure occurs
   */
  public static void deserialize(InputStream aStream, CAS aCAS, boolean aLenient,
          XmiSerializationSharedData aSharedData)
          throws SAXException, IOException {
    deserialize(aStream, aCAS, aLenient, aSharedData, -1);
  }
  
  /**
   * Deserializes a CAS from XMI.  This version of this method supports merging multiple XMI documents into a single CAS.
   * 
   * @param aStream
   *          input stream from which to read the XCMI document
   * @param aCAS
   *          CAS into which to deserialize. This CAS must be set up with a type system that is
   *          compatible with that in the XMI
   * @param aLenient
   *          if true, unknown Types will be ignored. If false, unknown Types will cause an
   *          exception. The default is false.
   * @param aSharedData
   *          a container for data that is shared between the {@link XmiCasSerializer} and the {@link XmiCasDeserializer}.
   *          See the JavaDocs for {@link XmiSerializationSharedData} for details.
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
    XMLReader xmlReader = XMLReaderFactory.createXMLReader();
    XmiCasDeserializer deser = new XmiCasDeserializer(aCAS.getTypeSystem());
    ContentHandler handler = deser.getXmiCasHandler(aCAS, aLenient, aSharedData, aMergePoint);
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
    String uimaNamespace = (String) xmiNamespaceToUimaNamespaceMap.get(nsUri);
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
}
