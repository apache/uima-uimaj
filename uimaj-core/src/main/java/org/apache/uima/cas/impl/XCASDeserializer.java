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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.StringUtils;
import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.internal.util.rb_trees.RedBlackTree;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XCAS Deserializer. Takes an XCAS and reads it into a CAS.
 */
public class XCASDeserializer {

  private static class FSInfo {

    final private int addr;

    final private IntVector indexRep;

    private FSInfo(int addr, IntVector indexRep) {
      super();
      this.addr = addr;
      this.indexRep = indexRep;
    }

  }

  private class XCASDeserializerHandler extends DefaultHandler {

    // ///////////////////////////////////////////////////////////////////////
    // Internal states for the parser.

    // Expect the start of the XML document.
    private static final int DOC_STATE = 0;

    // At the top level. Expect a FS, or the document text element, or the end of the
    // XML input.
    private static final int FS_STATE = 1;

    // Inside a FS. Expect features, or the end of the FS.
    private static final int FEAT_STATE = 2;

    // Inside FS. We have seen a _content attribute, and expect text.
    private static final int CONTENT_STATE = 3;

    // Inside a feature element. We expect the feature value.
    private static final int FEAT_CONTENT_STATE = 4;

    // Inside an array element. Expect array element value.
    private static final int ARRAY_ELE_CONTENT_STATE = 5;

    // Inside an array FS. Expect an array element, or the end of the FS.
    private static final int ARRAY_ELE_STATE = 6;

    // Inside the document text element. Expect the doc text.
    private static final int DOC_TEXT_STATE = 7;

    // Inside an Out-Of-Typesystem FS. Expect features, or the end of the FS.
    private static final int OOTS_FEAT_STATE = 8;

    // Inside an Out-Of-Typesystem FS. We have seen a _content attribute,
    // and expect text.
    private static final int OOTS_CONTENT_STATE = 9;

    // Default feature name for contents of an FS element, if not specified by _content attribute.
    private static final String DEFAULT_CONTENT_FEATURE = "value";

    // End parser states.
    // ///////////////////////////////////////////////////////////////////////

    private static final String reservedAttrPrefix = "_";

    // For error message printing, if the Locator object can't provide source
    // of XML input.
    private static final String unknownXMLSource = "<unknown>";

    // private long time;

    // SAX locator. Used for error message generation.
    private Locator locator;

    // The CAS we're filling.
    final private CASImpl cas;

    // Store FSs with ID in a search tree (for later reference resolution).
    final private RedBlackTree<FSInfo> fsTree;

    // Store IDless FSs in a vector;
    final private List<FSInfo> idLess;

    // What we expect next.
    private int state;

    // StringBuffer to accumulate text.
    private StringBuffer buffer;

    // The address of the most recently created FS. Needed for array elements
    // and embedded feature values.
    private int currentAddr;

    // The name of the content feature, if we've seen one.
    private String currentContentFeat = DEFAULT_CONTENT_FEATURE;

    // The current position when parsing array elements.
    private int arrayPos;

    // Stores out of type system data (APL)
    private OutOfTypeSystemData outOfTypeSystemData;

    // Current out of type system FS
    private FSData currentOotsFs;

    // SofaFS type
    final private int sofaTypeCode;

    // AnnotationBase type
    final private Type annotBaseType;

    // Store IndexRepositories in a vector;
    final private List<FSIndexRepository> indexRepositories;

    // and Views too
    final private List<CAS> views;

    // for processing v1.x format XCAS
    // map from sofa int values to id references
    final  private IntVector sofaRefMap;

    // map incoming _indexed values
    final private IntVector indexMap;

    // working with initial view
    private int nextIndex;

    private XCASDeserializerHandler(CASImpl aCAS, OutOfTypeSystemData ootsData) {
      super();
      this.cas = aCAS.getBaseCAS();
      // Reset the CAS.
      cas.resetNoQuestions();
      this.fsTree = new RedBlackTree<FSInfo>();
      this.idLess = new ArrayList<FSInfo>();
      this.buffer = new StringBuffer();
      this.outOfTypeSystemData = ootsData;
      this.indexRepositories = new ArrayList<FSIndexRepository>();
      this.views = new ArrayList<CAS>();
      // using the baseCas for indexing Sofas
      indexRepositories.add(this.cas.getBaseIndexRepository());
      // There should always be another index for the Initial View
      indexRepositories.add(this.cas.getView(CAS.NAME_DEFAULT_SOFA).getIndexRepository());
      this.sofaTypeCode = cas.ll_getTypeSystem().ll_getCodeForType(
              cas.getTypeSystem().getType(CAS.TYPE_NAME_SOFA));
      this.annotBaseType = this.cas.getAnnotationType();
      this.sofaRefMap = new IntVector();
      this.indexMap = new IntVector();
      // add entry for baseCAS ... point non-compliant annotations at first Sofa
      sofaRefMap.add(1);
      // add entry for baseCAS ... _indexed=0 stays in 0
      indexMap.add(0);
    }

    private final void resetBuffer() {
      // this.buffer.delete(0, this.buffer.length());
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
          if (!qualifiedName.equals(XCASSerializer.casTagName)) {
            throw createException(XCASParsingException.WRONG_ROOT_TAG, qualifiedName);
          }
          this.state = FS_STATE;
          break;
        }
        case FS_STATE: {
          this.currentContentFeat = DEFAULT_CONTENT_FEATURE;
          if (qualifiedName.equals(getDocumentTypeName())) {
            readDocument(attrs);
          } else {
            readFS(qualifiedName, attrs);
          }
          break;
        }
        case ARRAY_ELE_STATE: {
          readArrayElement(qualifiedName, attrs);
          break;
        }
        default: {
          // If we're not in an element expecting state, raise an error.
          throw createException(XCASParsingException.TEXT_EXPECTED, qualifiedName);
        }
      }
    }

    // Get ready to read document text.
    private void readDocument(Attributes attrs) {
      this.state = DOC_TEXT_STATE;
    }

    // Get ready to read array element.
    private void readArrayElement(String eleName, Attributes attrs) throws SAXParseException {
      if (!eleName.equals(XCASSerializer.ARRAY_ELEMENT_TAG)) {
        throw createException(XCASParsingException.ARRAY_ELE_EXPECTED, eleName);
      }
      if (attrs.getLength() > 0) {
        throw createException(XCASParsingException.ARRAY_ELE_ATTRS);
      }
      this.state = ARRAY_ELE_CONTENT_STATE;
      // resetBuffer();
    }

    // Create a new FS.
    private void readFS(String qualifiedName, Attributes attrs) throws SAXParseException {
      String typeName = getCasTypeName(qualifiedName);
      TypeImpl type = (TypeImpl) ts.getType(typeName);
      if (type == null) {
        if (typeName.equals("uima.cas.SofA")) {
          // temporary fix for XCAS written with pre-public version of Sofas
          type = (TypeImpl) ts.getType("uima.cas.Sofa");
        }
      }
      if (type == null) {
        if (this.outOfTypeSystemData == null) {
          throw createException(XCASParsingException.UNKNOWN_TYPE, typeName);
        } else {
          // add this FS to out-of-typesystem data - this also sets the
          // parser state appropriately (APL)
          addToOutOfTypeSystemData(typeName, attrs);
        }
      } else {
        if (cas.isArrayType(type.getCode())) {
          readArray(type, attrs);
          return;
        }
        final int addr = cas.ll_createFS(type.getCode());
        readFS(addr, attrs, true);
      }
    }

    /**
     * 
     * @param addr
     * @param attrs
     * @param toIndex
     *          Special hack to accommodate document annotation, which is already in the index.
     * @throws SAXParseException passthru
     */
    private void readFS(final int addr, Attributes attrs, boolean toIndex) throws SAXParseException {
      // Hang on address for setting content feature
      this.currentAddr = addr;
      int id = -1;
      IntVector indexRep = new IntVector(1); // empty means not indexed
      String attrName, attrValue;
      final int heapValue = cas.getHeapValue(addr);
      final Type type = cas.ll_getTypeSystem().ll_getTypeForCode(cas.ll_getFSRefType(addr));

      // Special handling for Sofas
      if (sofaTypeCode == heapValue) {
        // create some maps to handle v1 format XCAS ...
        // ... where the sofa feature of annotations was an int not a ref

        // determine if this is the one and only initial view Sofa
        boolean isInitialView = false;
        String sofaID = attrs.getValue(CAS.FEATURE_BASE_NAME_SOFAID);
        if (sofaID.equals("_DefaultTextSofaName")) {
          sofaID = CAS.NAME_DEFAULT_SOFA;
        }
//        if (uimaContext != null) {
//          // Map incoming SofaIDs
//          sofaID = uimaContext.mapToSofaID(sofaID).getSofaID();
//        }
        if (sofaID.equals(CAS.NAME_DEFAULT_SOFA)) {
          isInitialView = true;
        }
        // get the sofaNum
        String sofaNum = attrs.getValue(CAS.FEATURE_BASE_NAME_SOFANUM);
        int thisSofaNum = Integer.parseInt(sofaNum);

        // get the sofa's FeatureStructure id
        int sofaFsId = Integer.parseInt(attrs.getValue(XCASSerializer.ID_ATTR_NAME));

        // for v1 and v2 formats, create the index map
        // ***we assume Sofas are always received in Sofanum order***
        // Two scenarios ... the initial view is the first sofa, or not.
        // If not, the _indexed values need to be remapped to leave room for the initial view,
        // which may or may not be in the received CAS.
        if (this.indexMap.size() == 1) {
          if (isInitialView) {
            // the first Sofa an initial view
            if (thisSofaNum == 2) {
              // this sofa was mapped to the initial view
              this.indexMap.add(-1); // for this CAS, there should not be a sofanum = 1
              this.indexMap.add(1); // map 2 to 1
              this.nextIndex = 2;
            } else {
              this.indexMap.add(1);
              this.nextIndex = 2;
            }
          } else {
            if (thisSofaNum > 1) {
              // the first Sofa not initial, but sofaNum > 1
              // must be a v2 format, and sofaNum better be 2
              this.indexMap.add(1);
              assert (thisSofaNum == 2);
              this.indexMap.add(2);
              this.nextIndex = 3;
            } else {
              // must be v1 format
              this.indexMap.add(2);
              this.nextIndex = 3;
            }
          }
        } else {
          // if the new Sofa is the initial view, always map to 1
          if (isInitialView) {
            // the initial view is not the first
            // if v2 format, space already reserved in mapping
            if (this.indexMap.size() == thisSofaNum) {
              // v1 format, add mapping for initial view
              this.indexMap.add(1);
            }
          } else {
            this.indexMap.add(this.nextIndex);
            this.nextIndex++;
          }
        }

        // Now update the mapping from annotation int to ref values
        if (this.sofaRefMap.size() == thisSofaNum) {
          // Sofa received in sofaNum order, add new one
          this.sofaRefMap.add(sofaFsId);
        } else if (this.sofaRefMap.size() > thisSofaNum) {
          // new Sofa has lower sofaNum than last one
          this.sofaRefMap.set(thisSofaNum, sofaFsId);
        } else {
          // new Sofa has skipped ahead more than 1
          this.sofaRefMap.setSize(thisSofaNum + 1);
          this.sofaRefMap.set(thisSofaNum, sofaFsId);
        }

      }

      for (int i = 0; i < attrs.getLength(); i++) {
        attrName = attrs.getQName(i);
        attrValue = attrs.getValue(i);
        if (attrName.startsWith(reservedAttrPrefix)) {
          if (attrName.equals(XCASSerializer.ID_ATTR_NAME)) {
            try {
              id = Integer.parseInt(attrValue);
            } catch (NumberFormatException e) {
              throw createException(XCASParsingException.ILLEGAL_ID, attrValue);
            }
          } else if (attrName.equals(XCASSerializer.CONTENT_ATTR_NAME)) {
            this.currentContentFeat = attrValue;
            // this.state = CONTENT_STATE; APL-6/28/04 - removed, see below
          } else if (attrName.equals(XCASSerializer.INDEXED_ATTR_NAME)) {
            // if (attrValue.equals(XCASSerializer.TRUE_VALUE) && toIndex)
            String[] arrayvals = parseArray(attrValue);
            for (int s = 0; s < arrayvals.length; s++) {
              indexRep.add(Integer.parseInt(arrayvals[s]));
            }
          } else {
            handleFeature(type, addr, attrName, attrValue, false);
          }
        } else {
          if (sofaTypeCode == heapValue) {
            if (attrName.equals(CAS.FEATURE_BASE_NAME_SOFAID)) {
              if (attrValue.equals("_DefaultTextSofaName")) {
                // First change old default Sofa name into the new one
                attrValue = CAS.NAME_DEFAULT_SOFA;
              }
//              if (uimaContext != null) {
//                // Map incoming SofaIDs
//                attrValue = uimaContext.mapToSofaID(attrValue).getSofaID();
//              }
            }
          }
          handleFeature(type, addr, attrName, attrValue, false);
        }
      }

      if (sofaTypeCode == heapValue) {
        // If a Sofa, create CAS view to get new indexRepository
        SofaFS sofa = (SofaFS) cas.createFS(addr);
        // also add to indexes so we can retrieve the Sofa later
        cas.getBaseIndexRepository().addFS(sofa);
        CAS view = cas.getView(sofa);
        if (sofa.getSofaRef() == 1) {
          cas.registerInitialSofa();
        } else {
          // add indexRepo for views other than the initial view
          indexRepositories.add(cas.getSofaIndexRepository(sofa));
        }
        ((CASImpl) view).registerView(sofa);
        views.add(view);
      }
      FSInfo fsInfo = new FSInfo(addr, indexRep);
      if (id < 0) {
        idLess.add(fsInfo);
      } else {
        fsTree.put(id, fsInfo);
      }
      // Set the state; we're either expecting features, or _content.
      // APL - 6/28/04 - even if _content attr is not specified, we can still have content, which
      // would
      // be assigned to the "value" feature, as per XCAS spec. FEAT_STATE did not really seem to be
      // working, anyway.
      this.state = CONTENT_STATE;
      // if (this.state != CONTENT_STATE)
      // {
      // this.state = FEAT_STATE;
      // }
    }

    // Create a new array FS.
    private void readArray(TypeImpl type, Attributes attrs) throws SAXParseException {
      String attrName, attrVal;
      // No entries in indexRep means not indexed
      IntVector indexRep = new IntVector();
      int size = 0;
      int id = -1;
      for (int i = 0; i < attrs.getLength(); i++) {
        attrName = attrs.getQName(i);
        attrVal = attrs.getValue(i);
        if (attrName.equals(XCASSerializer.ID_ATTR_NAME)) {
          try {
            id = Integer.parseInt(attrVal);
          } catch (NumberFormatException e) {
            throw createException(XCASParsingException.ILLEGAL_ID, attrVal);
          }
        } else if (attrName.equals(XCASSerializer.ARRAY_SIZE_ATTR)) {
          try {
            size = Integer.parseInt(attrVal);
            if (size < 0) {
              throw createException(XCASParsingException.ILLEGAL_ARRAY_SIZE, attrVal);
            }
          } catch (NumberFormatException e) {
            throw createException(XCASParsingException.INTEGER_EXPECTED, attrVal);
          }
        } else if (attrName.equals(XCASSerializer.INDEXED_ATTR_NAME)) {
          String[] arrayvals = parseArray(attrVal);
          for (int s = 0; s < arrayvals.length; s++) {
            indexRep.add(Integer.parseInt(arrayvals[s]));
          }
        } else {
          throw createException(XCASParsingException.ILLEGAL_ARRAY_ATTR, attrName);
        }
      }
      FeatureStructureImpl fs;
      if (cas.isIntArrayType(type)) {
        fs = (FeatureStructureImpl) cas.createIntArrayFS(size);        
      } else if (cas.isFloatArrayType(type)) {
        fs = (FeatureStructureImpl) cas.createFloatArrayFS(size);                
      } else if (cas.isStringArrayType(type)) {
        fs = (FeatureStructureImpl) cas.createStringArrayFS(size);                
      } else if (cas.isBooleanArrayType(type)) {
        fs = (FeatureStructureImpl) cas.createBooleanArrayFS(size);
      } else if (cas.isByteArrayType(type)) {
        fs = (FeatureStructureImpl) cas.createByteArrayFS(size);
      } else if (cas.isShortArrayType(type)) {
        fs = (FeatureStructureImpl) cas.createShortArrayFS(size);
      } else if (cas.isLongArrayType(type)) {
        fs = (FeatureStructureImpl) cas.createLongArrayFS(size);
      } else if (cas.isDoubleArrayType(type)) {
        fs = (FeatureStructureImpl) cas.createDoubleArrayFS(size);
      } else {
        fs = (FeatureStructureImpl) cas.createArrayFS(size);
      }

      final int addr = fs.getAddress();
      FSInfo fsInfo = new FSInfo(addr, indexRep);
      if (id >= 0) {
        fsTree.put(id, fsInfo);
      } else {
        idLess.add(fsInfo);
      }
      // Hang on to those for setting array values.
      this.currentAddr = addr;
      this.arrayPos = 0;

      this.state = ARRAY_ELE_STATE;
    }

    // The definition of a null value. Any other value must be in the expected
    // format.
    private final boolean emptyVal(String val) {
      return ((val == null) || (val.length() == 0));
    }

    // Create a feature value from a string representation.
    private void handleFeature(int addr, String featName, String featVal, boolean lenient)
            throws SAXParseException {
      int typeCode = cas.ll_getFSRefType(addr);
      Type type = cas.ll_getTypeSystem().ll_getTypeForCode(typeCode);
      handleFeature(type, addr, featName, featVal, lenient);
    }

    private void handleFeature(final Type type, int addr, String featName, String featVal,
            boolean lenient) throws SAXParseException {
      // The FeatureMap approach is broken because it assumes feature short names
      // are unique. This is my quick fix. -APL
      // final FeatureImpl feat = (FeatureImpl) featureMap.get(featName);

      // handle v1.x format annotations, mapping int to ref values
      if (featName.equals("sofa") && ts.subsumes(this.annotBaseType, type)) {
        featVal = Integer.toString(this.sofaRefMap.get(Integer.parseInt(featVal)));
      }

      // handle v1.x sofanum values, remapping so that _InitialView always == 1
      if (featName.equals(CAS.FEATURE_BASE_NAME_SOFAID)
              && this.sofaTypeCode == cas.getHeapValue(addr)) {
        Type sofaType = ts.ll_getTypeForCode(this.sofaTypeCode);
        final FeatureImpl sofaNumFeat = (FeatureImpl) sofaType
                .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_SOFANUM);
        int sofaNum = cas.getFeatureValue(addr, sofaNumFeat.getCode());
        cas.setFeatureValue(addr, sofaNumFeat.getCode(), this.indexMap.get(sofaNum));
      }

      String realFeatName;
      if (featName.startsWith(XCASSerializer.REF_PREFIX)) {
        realFeatName = featName.substring(XCASSerializer.REF_PREFIX.length());
      } else {
        realFeatName = featName;
      }
      final FeatureImpl feat = (FeatureImpl) type.getFeatureByBaseName(realFeatName);
      // System.out.println("DEBUG - Feature map result: " + featName + " = " + feat.getName());
      if (feat == null) { // feature does not exist in typesystem
        if (outOfTypeSystemData != null) {
          // Add to Out-Of-Typesystem data (APL)
          Integer addrInteger = Integer.valueOf(addr);
          List<String[]> ootsAttrs = outOfTypeSystemData.extraFeatureValues.get(addrInteger);
          if (ootsAttrs == null) {
            ootsAttrs = new ArrayList<String[]>();
            outOfTypeSystemData.extraFeatureValues.put(addrInteger, ootsAttrs);
          }
          ootsAttrs.add(new String[] { featName, featVal });
        } else if (!lenient) {
          throw createException(XCASParsingException.UNKNOWN_FEATURE, featName);
        }
      } else {
        if (cas.ll_isRefType(ts.range(feat.getCode()))) {
          cas.setFeatureValue(addr, feat.getCode(), Integer.parseInt(featVal));
        } else {
          cas.setFeatureValueFromString(addr, feat.getCode(), featVal);
        }

      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] chars, int start, int length) throws SAXException {
      switch (this.state) {
        case DOC_TEXT_STATE:
        case CONTENT_STATE:
        case OOTS_CONTENT_STATE:
        case ARRAY_ELE_CONTENT_STATE:
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
        case FEAT_STATE: {
          this.state = FS_STATE;
          break;
        }
        case CONTENT_STATE: {
          // Set the value of the content feature.
          if (!isAllWhitespace(buffer)) {
            try {
              handleFeature(currentAddr, currentContentFeat, buffer.toString(), true);
            } catch (XCASParsingException x) {
              // Not sure why we are calling handleFeature for WF content
            }
          }
          this.state = FS_STATE;
          break;
        }
        case FEAT_CONTENT_STATE: {
          // Create a feature value from an element.
          handleFeature(currentAddr, qualifiedName, buffer.toString(), false);
          this.state = FEAT_STATE;
          break;
        }
        case ARRAY_ELE_CONTENT_STATE: {
          // Create an array value.
          addArrayElement(buffer.toString());
          this.state = ARRAY_ELE_STATE;
          break;
        }
        case ARRAY_ELE_STATE: {
          this.state = FS_STATE;
          break;
        }
        case DOC_TEXT_STATE: {
          // Assume old style CAS with one text Sofa
          SofaFS newSofa = cas.createInitialSofa("text");
          CASImpl tcas = (CASImpl) cas.getInitialView();
          tcas.registerView(newSofa);
          // Set the document text without creating a documentAnnotation
          tcas.setDocTextFromDeserializtion(buffer.toString());

          // and assume the new Sofa is at location 1!
          int addr = 1;
          int id = 1;
          this.sofaRefMap.add(id);

          // and register the id for this Sofa
          FSInfo fsInfo = new FSInfo(addr, new IntVector());
          fsTree.put(id, fsInfo);

          this.state = FS_STATE;
          break;
        }
        case OOTS_CONTENT_STATE: {
          // Set the value of the content feature.
          if (!isAllWhitespace(buffer)) {
            // Set the value of the content feature.
            currentOotsFs.featVals.put(currentContentFeat, buffer.toString());
          }
          this.state = FS_STATE;
          break;
        }
        case OOTS_FEAT_STATE: {
          this.state = FS_STATE;
          break;
        }
      }
    }

    private void addArrayElement(String content) throws SAXParseException {
      if (arrayPos >= cas.ll_getArraySize(currentAddr)) {
        throw createException(XCASParsingException.EXCESS_ARRAY_ELE);
      }
      try {
        if (!emptyVal(content)) {
          if (cas.isArrayType(cas.getHeap().heap[currentAddr])) {
            cas.setArrayValueFromString(currentAddr, arrayPos, content);
          } else {
            System.out.println(" not a known array type ");
          }
        }
      } catch (NumberFormatException e) {
        throw createException(XCASParsingException.INTEGER_EXPECTED, content);
      }

      ++arrayPos;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    public void endDocument() throws SAXException {
      // time = System.currentTimeMillis() - time;
      // System.out.println("Done reading xml data in " + new TimeSpan(time));
      // System.out.println(
      // "Resolving references for id data (" + fsTree.size() + ").");
      // time = System.currentTimeMillis();
      // Resolve references, index.
      for (FSInfo fsInfo : fsTree) {
        finalizeFS(fsInfo);
      }
      // time = System.currentTimeMillis() - time;
      // System.out.println("Done in " + new TimeSpan(time));
      // System.out.println(
      // "Resolving references for non-id data (" + idLess.size() + ").");
      // time = System.currentTimeMillis();
      for (int i = 0; i < idLess.size(); i++) {
        finalizeFS(idLess.get(i));
      }
      // time = System.currentTimeMillis() - time;
      // System.out.println("Done in " + new TimeSpan(time));

      // also finalize Out-Of-TypeSystem FSs and features (APL)
      if (outOfTypeSystemData != null) {
        for (FSData fsData : outOfTypeSystemData.fsList) {
          finalizeOutOfTypeSystemFS(fsData);
        }
        finalizeOutOfTypeSystemFeatures();
      }

      for (CAS view : views) {
        ((CASImpl)view).updateDocumentAnnotation();
      }
//      for (int i = 0; i < views.size(); i++) {       
//        ((CASImpl) views.get(i)).updateDocumentAnnotation();
//      }
    }

    private void finalizeFS(FSInfo fsInfo) {
      final int addr = fsInfo.addr;
      final int type = cas.getHeapValue(addr);
      if (cas.isArrayType(type)) {
        finalizeArray(type, addr, fsInfo);
        finalizeAddToIndexes(fsInfo, addr);
        return;
      }
      
      int[] feats = cas.getTypeSystemImpl().ll_getAppropriateFeatures(type);
      int feat;
      FSInfo fsValInfo;
      for (int i = 0; i < feats.length; i++) {
        feat = feats[i];
        if (cas.ll_isRefType(ts.range(feats[i]))) {
          int featVal = cas.getFeatureValue(addr, feat);
          fsValInfo = fsTree.get(featVal);
          if (fsValInfo == null) {
            cas.setFeatureValue(addr, feat, CASImpl.NULL);
            // this feature may be a ref to an out-of-typesystem FS.
            // add it to the Out-of-typesystem features list (APL)
            if (featVal != 0 && outOfTypeSystemData != null) {
              Integer addrInteger = Integer.valueOf(addr);
              List<String[]> ootsAttrs = outOfTypeSystemData.extraFeatureValues.get(addrInteger);
              if (ootsAttrs == null) {
                ootsAttrs = new ArrayList<String[]>();
                outOfTypeSystemData.extraFeatureValues.put(addrInteger, ootsAttrs);
              }
              String featFullName = ts.ll_getFeatureForCode(feat).getName();
              int separatorOffset = featFullName.indexOf(TypeSystem.FEATURE_SEPARATOR);
              String featName = "_ref_" + featFullName.substring(separatorOffset + 1);
              ootsAttrs.add(new String[] { featName, Integer.toString(featVal) });
            }
          } else {
            cas.setFeatureValue(addr, feat, fsValInfo.addr);
          }
        }
      }
      finalizeAddToIndexes(fsInfo, addr);  // must be done after above fixes the sofa refs
    }

    private void finalizeAddToIndexes(final FSInfo fsInfo, final int addr) {
      if (fsInfo.indexRep.size() >= 0) {
        // Now add FS to all specified index repositories
        for (int i = 0; i < fsInfo.indexRep.size(); i++) {
          if (indexMap.size() == 1) {
            ((FSIndexRepositoryImpl) indexRepositories.get(fsInfo.indexRep.get(i))).addFS(addr);
          } else {
            ((FSIndexRepositoryImpl) indexRepositories.get(indexMap.get(fsInfo.indexRep.get(i))))
                    .addFS(addr);
          }
        }
      }
    }
    
    private void finalizeArray(int type, int addr, FSInfo fsInfo) {
      if (!cas.isFSArrayType(type)) {
        // Nothing to do.
        return;
      }
      final int size = cas.ll_getArraySize(addr);
      FSInfo fsValInfo;
      for (int i = 0; i < size; i++) {
        int arrayVal = cas.getArrayValue(addr, i);
        fsValInfo = fsTree.get(arrayVal);
        if (fsValInfo == null) {
          cas.setArrayValue(addr, i, CASImpl.NULL);
          // this element may be a ref to an out-of-typesystem FS.
          // add it to the Out-of-typesystem array elements list (APL)
          if (arrayVal != 0 && outOfTypeSystemData != null) {
            Integer arrayAddrInteger = Integer.valueOf(addr);
            List<ArrayElement> ootsElements = outOfTypeSystemData.arrayElements.get(arrayAddrInteger);
            if (ootsElements == null) {
              ootsElements = new ArrayList<ArrayElement>();
              outOfTypeSystemData.arrayElements.put(arrayAddrInteger, ootsElements);
            }
            // the "value" of the refrence is the ID, but we prefix with a letter to indicate
            // that this ID refers to an OOTS FS
            ArrayElement ootsElem = new ArrayElement(i, "a" + Integer.toString(arrayVal));
            ootsElements.add(ootsElem);
          }
        } else {
          cas.setArrayValue(addr, i, fsValInfo.addr);
        }
      }
    }

    /*
     * Finalizes an Out Of Type System FS by assigning a unique ID (prepending a letter) and
     * remapping ID references appropriately (both In-Type-System and Out-Of-TypeSystem refs).
     */
    private void finalizeOutOfTypeSystemFS(FSData aFS) {
      // make ID unique by prefixing a letter
      aFS.id = 'a' + aFS.id;
      // remap ref features
      for (Map.Entry<String, String> entry : aFS.featVals.entrySet()) {
        String attrName =  entry.getKey();
        if (attrName.startsWith("_ref_")) {
          int val = Integer.parseInt(entry.getValue());
          if (val >= 0) // negative numbers represent null and are left unchanged
          {
            // attempt to locate target in type system
            FSInfo fsValInfo = fsTree.get(val);
            if (fsValInfo != null) {
              entry.setValue(Integer.toString(fsValInfo.addr));
            } else
            // out of type system - remap by prepending letter
            {
              entry.setValue("a" + val);
            }
          }
        }

      }
    }

    /*
     * Finalizes the Out Of Type System features (extra features on in-typesystem types).
     */
    private void finalizeOutOfTypeSystemFeatures() {
      // remap ref features
      for (List<String[]> attrs : outOfTypeSystemData.extraFeatureValues.values()) {
        for (String[] attr : attrs) {
          if (attr[0].startsWith("_ref_")) {
            int val = Integer.parseInt(attr[1]);
            if (val >= 0) // negative numbers represent null and are left unchanged
            {
              // attempt to locate target in type system
              FSInfo fsValInfo = fsTree.get(val);
              if (fsValInfo != null) {
                attr[1] = Integer.toString(fsValInfo.addr);
              } else
              // out of type system - remap by prepending letter
              {
                attr[1] = "a" + val;
              }
            }
          }
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

    /*
     * Adds a feature sturcture to the out-of-typesystem data, and sets the parser's state
     * appropriately. (APL)
     */
    private void addToOutOfTypeSystemData(String typeName, Attributes attrs)
            throws XCASParsingException {
      if (this.outOfTypeSystemData != null) {
        FSData fs = new FSData();
        fs.type = typeName;
        fs.indexRep = null; // not indexed
        String attrName, attrValue;
        for (int i = 0; i < attrs.getLength(); i++) {
          attrName = attrs.getQName(i);
          attrValue = attrs.getValue(i);
          if (attrName.startsWith(reservedAttrPrefix)) {
            if (attrName.equals(XCASSerializer.ID_ATTR_NAME)) {
              fs.id = attrValue;
            } else if (attrName.equals(XCASSerializer.CONTENT_ATTR_NAME)) {
              this.currentContentFeat = attrValue;
            } else if (attrName.equals(XCASSerializer.INDEXED_ATTR_NAME)) {
              fs.indexRep = attrValue;
            } else {
              fs.featVals.put(attrName, attrValue);
            }
          } else {
            fs.featVals.put(attrName, attrValue);
          }
        }
        this.outOfTypeSystemData.fsList.add(fs);
        this.currentOotsFs = fs;
        // Set the state; we're ready to accept the "content" feature,
        // if one is specified
        this.state = OOTS_CONTENT_STATE;
      }
    }

    /**
     * Parse a multi-valued attribute into a String array, by splitting on whitespace.
     * 
     * @param val
     *          attribute value
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
     * Gets the CAS type name corresponding to an XCAS tag name. The type name is usually equal to
     * the tag name, but the characters : and - are translated into the sequences _colon_ and
     * _dash_, respectively.
     * 
     * @param aTagName
     *          XCAS tag name
     * @return CAS type name corresponding to this tag
     */
    private String getCasTypeName(String aTagName) {
      if (aTagName.indexOf(':') == -1 && aTagName.indexOf('-') == -1) {
        return aTagName;
      } else {
        // Note: This is really slow so we avoid if possible. -- RJB
        return StringUtils.replaceAll(StringUtils.replaceAll(aTagName, ":", "_colon_"), "-",
                "_dash_");
      }
    }
  }

  final private TypeSystemImpl ts;

  final private UimaContext uimaContext;

  // private HashMap featureMap; -APL
  // ///private int[] featureType;

  // name of tag to contain document text
  private String docTypeName = XCASSerializer.DEFAULT_DOC_TYPE_NAME;

  /**
   * Create a new deserializer from a type system. Note: all CAS arguments later supplied to
   * <code>getXCASHandler()</code> must have this type system as their type system.
   * 
   * @param ts
   *          The type system of the CASes to be deserialized.
   * @param uimaContext the UIMA Context for the deserialization
   */
  public XCASDeserializer(TypeSystem ts, UimaContext uimaContext) {
    super();
    this.ts = (TypeSystemImpl) ts;
    this.uimaContext = uimaContext;
    // this.featureMap = new HashMap(); - APL
  }

  public XCASDeserializer(TypeSystem ts) {
    this(ts, null);
  }

  /**
   * Create a default handler for deserializing an XCAS into the <code>cas</code> parameter.
   * <p>
   * Warning: for efficiency reasons, the deserializer does not do much type checking for features
   * and their values. It is expected that the incoming XCAS conforms to the type system provided.
   * If it doesn't, the results are undefined.
   * 
   * @param cas
   *          This CAS will be used to hold the data of the serialized XCAS.
   * @return The <code>DefaultHandler</code> to pass to the SAX parser.
   */
  public DefaultHandler getXCASHandler(CAS cas) {
    return getXCASHandler(cas, null);
  }

  /**
   * Create a default handler for deserializing an XCAS into the <code>cas</code> parameter. This
   * version causes the deserializer to store out-of-typesystem data for later use. (APL)
   * <p>
   * Warning: for efficiency reasons, the deserializer does not do much type checking for features
   * and their values. It is expected that the incoming XCAS conforms to the type system provided.
   * If it doesn't, the results are undefined.
   * 
   * @param cas
   *          This CAS will be used to hold the data of the serialized XCAS.
   * @param outOfTypeSystemData
   *          An object that stores FSs that do not conform to the CAS's type system
   * @return The <code>DefaultHandler</code> to pass to the SAX parser.
   */
  public DefaultHandler getXCASHandler(CAS cas, OutOfTypeSystemData outOfTypeSystemData) {
    return new XCASDeserializerHandler((CASImpl) cas, outOfTypeSystemData);
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
   * {@link XCASSerializer#DEFAULT_DOC_TYPE_NAME XCASSerializer.DEFAULT_DOC_TYPE_NAME}.
   * 
   * @param aDocTypeName
   *          the document type name
   */
  public void setDocumentTypeName(String aDocTypeName) {
    docTypeName = aDocTypeName;
  }

  /**
   * Deserializes an XCAS from a stream. By default this is not lenient, meaning that if the XCAS
   * references Types that are not in the Type System, an Exception will be thrown. Use
   * {@link XCASDeserializer#deserialize(InputStream,CAS,boolean)} to turn on lenient mode and
   * ignore any unknown types.
   * 
   * @param aStream
   *          input stream from which to read the XCAS XML document
   * @param aCAS
   *          CAS into which to deserialize. This CAS must be set up with a type system that is
   *          compatible with that in the XCAS
   * 
   * @throws SAXException
   *           if an XML Parsing error occurs
   * @throws IOException
   *           if an I/O failure occurs
   */
  public static void deserialize(InputStream aStream, CAS aCAS) throws SAXException, IOException {
    XCASDeserializer.deserialize(aStream, aCAS, false);
  }

  /**
   * Deserializes an XCAS from a stream.
   * 
   * @param aStream
   *          input stream from which to read the XCAS XML document
   * @param aCAS
   *          CAS into which to deserialize. This CAS must be set up with a type system that is
   *          compatible with that in the XCAS.
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
    XMLReader xmlReader = XMLUtils.createXMLReader();
    XCASDeserializer deser = new XCASDeserializer(aCAS.getTypeSystem());
    ContentHandler handler;
    if (aLenient) {
      handler = deser.getXCASHandler(aCAS, new OutOfTypeSystemData());
    } else {
      handler = deser.getXCASHandler(aCAS);
    }
    xmlReader.setContentHandler(handler);
    xmlReader.parse(new InputSource(aStream));
  }

}
