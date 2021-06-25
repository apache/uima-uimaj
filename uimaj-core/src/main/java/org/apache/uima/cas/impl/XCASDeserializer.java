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
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaSerializable;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.Pair;
import org.apache.uima.internal.util.StringUtils;
import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.internal.util.rb_trees.RedBlackTree;
import org.apache.uima.jcas.cas.CommonPrimitiveArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.Sofa;
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
 * XCAS Deserializer. Takes an XCAS and reads it into a CAS.
 */
public class XCASDeserializer {

 // @formatter:off
  /**
   * Feature Structure plus all the indexes it is indexed in
   *    indexRep -> indexMap -> indexRepositories -> indexRepository or
   *    indexRep             -> indexRepositories -> indexRepository
   *
   * (2nd if indexMap size == 1)
   */
 // @formatter:on
  private static class FSInfo {

    final private TOP fs;

    final private IntVector indexRep;

    private FSInfo(TOP fs, IntVector indexRep) {
      this.fs = fs;
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

    // SofaFS type
    static final private int sofaTypeCode = TypeSystemConstants.sofaTypeCode;

    // private long time;

    // SAX locator. Used for error message generation.
    private Locator locator;

    // The CAS we're filling.
    final private CASImpl cas;

    // Store FSs with ID in a search tree (for later reference resolution).
    /**
     * Map from extId to FSInfo (including fs)
     */
    final private RedBlackTree<FSInfo> fsTree;

    // Store IDless FSs in a vector;
    final private List<FSInfo> idLess;

    final private List<Runnable> fixupToDos = new ArrayList<>();
    final private List<Runnable> uimaSerializableFixups = new ArrayList<>();

    // What we expect next.
    private int state;

    // StringBuffer to accumulate text.
    private StringBuffer buffer;

    // The most recently created FS. Needed for array elements
    // and embedded feature values.
    private TOP currentFs;

    // The name of the content feature, if we've seen one.
    private String currentContentFeat = DEFAULT_CONTENT_FEATURE;

    // The current position when parsing array elements.
    private int arrayPos;

    // Stores out of type system data (APL)
    private OutOfTypeSystemData outOfTypeSystemData;

    // Current out of type system FS
    private FSData currentOotsFs;

    /** map from index -> indexRepository, [0] = base view, [1] initial view, [2 +] other views */
    final private List<FSIndexRepository> indexRepositories;

    /** map for index -> cas views, */
    final private List<CAS> views;

    // for processing v1.x format XCAS
    // map from sofaNum int values to external id references
    final private IntVector sofaRefMap;

    // map incoming _indexed values
    /**
     * Map external SofaNum -> internal sofaNum 
     * 
     * internal sofaNums also used to index indexRepositories -> ref to FsIndexRepositoryImpl
     */
    final private IntVector indexMap;

    // working with initial view
    private int nextIndex;

    private TOP highestIdFs = null;

    /** the fsId read from the _id attribute */
    private int fsId;

    private XCASDeserializerHandler(CASImpl aCAS, OutOfTypeSystemData ootsData) {
      this.cas = aCAS.getBaseCAS();
      // Reset the CAS.
      cas.resetNoQuestions();
      this.fsTree = new RedBlackTree<>();
      this.idLess = new ArrayList<>();
      this.buffer = new StringBuffer();
      this.outOfTypeSystemData = ootsData;
      this.indexRepositories = new ArrayList<>();
      this.views = new ArrayList<>();
      // using the baseCas for indexing Sofas
      indexRepositories.add(this.cas.getBaseIndexRepository());
      // There should always be another index for the Initial View
      indexRepositories.add(this.cas.getView(CAS.NAME_DEFAULT_SOFA).getIndexRepository());
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
      // get the FeatureStructure id
      fsId = Integer.parseInt(attrs.getValue(XCASSerializer.ID_ATTR_NAME));

      if (qualifiedName.equals("uima.cas.SofA")) {
        qualifiedName = "uima.cas.Sofa"; // fix for XCAS written with pre-public version of Sofas
      }

      String typeName = getCasTypeName(qualifiedName);
      TypeImpl type = ts.getType(typeName);

      if (type == null) {
        if (this.outOfTypeSystemData == null) {
          throw createException(XCASParsingException.UNKNOWN_TYPE, typeName);
        } else {
          // add this FS to out-of-typesystem data - this also sets the
          // parser state appropriately (APL)
          addToOutOfTypeSystemData(typeName, attrs);
        }
      } else {
        if (type.isArray()) {
          readArray(type, attrs);
          return;
        }
        readFS(type, attrs, true);
      }
    }

    /**
     * 
     * @param addr
     * @param attrs
     * @param toIndex
     *          Special hack to accommodate document annotation, which is already in the index.
     * @throws SAXParseException
     *           passthru
     */
    private void readFS(final TypeImpl type, Attributes attrs, boolean toIndex)
            throws SAXParseException {
      final int typecode = type.getCode();
      final TOP fs;

      if (sofaTypeCode == typecode) {
        // Special handling for Sofas

        // get SofaID - the view name or the string _InitialView
        String sofaID = attrs.getValue(CAS.FEATURE_BASE_NAME_SOFAID);
        if (sofaID.equals("_DefaultTextSofaName")) {
          sofaID = CAS.NAME_DEFAULT_SOFA;
        }
        final boolean isInitialView = sofaID.equals(CAS.NAME_DEFAULT_SOFA);

        // get sofaNum
        String sofaNum = attrs.getValue(CAS.FEATURE_BASE_NAME_SOFANUM);
        final int extSofaNum = Integer.parseInt(sofaNum);

        // get the sofa's FeatureStructure id
        // final int sofaExtId = Integer.parseInt(attrs.getValue(XCASSerializer.ID_ATTR_NAME));

        // create some maps to handle v1 format XCAS ...
        // ... where the sofa feature of annotations was an int not a ref

        // for v1 and v2 formats, create the index map
        // ***we assume Sofas are always received in Sofanum order***
        // Two scenarios ... the initial view is the first sofa, or not.
        // If not, the _indexed values need to be remapped to leave room for the initial view,
        // which may or may not be in the received CAS.
        if (this.indexMap.size() == 1) {
          if (isInitialView) {
            // the first Sofa an initial view
            if (extSofaNum == 2) {
              // this sofa was mapped to the initial view
              this.indexMap.add(-1); // for this CAS, there should not be a sofanum = 1
              this.indexMap.add(1); // map 2 to 1
              this.nextIndex = 2;
            } else {
              this.indexMap.add(1);
              this.nextIndex = 2;
            }
          } else {
            if (extSofaNum > 1) {
              // the first Sofa not initial, but sofaNum > 1
              // must be a v2 format, and sofaNum better be 2
              this.indexMap.add(1);
              assert (extSofaNum == 2);
              this.indexMap.add(2);
              this.nextIndex = 3;
            } else {
              // must be v1 format
              this.indexMap.add(2);
              this.nextIndex = 3;
            }
          }
        } else {
          // this is the case for the 2nd and subsequent Sofas
          // if the new Sofa is the initial view, always map to 1
          if (isInitialView) {
            // the initial view is not the first
            // if v2 format, space already reserved in mapping
            if (this.indexMap.size() == extSofaNum) {
              // v1 format, add mapping for initial view
              this.indexMap.add(1);
            }
          } else {
            this.indexMap.add(this.nextIndex);
            this.nextIndex++;
          }

        }

        // Now update the mapping from annotation int to ref values
        if (this.sofaRefMap.size() == extSofaNum) {
          // Sofa received in sofaNum order, add new one
          this.sofaRefMap.add(fsId);
        } else if (this.sofaRefMap.size() > extSofaNum) {
          // new Sofa has lower sofaNum than last one
          this.sofaRefMap.set(extSofaNum, fsId);
        } else {
          // new Sofa has skipped ahead more than 1
          this.sofaRefMap.setSize(extSofaNum + 1);
          this.sofaRefMap.set(extSofaNum, fsId);
        }

        // get the sofa's mimeType
        String sofaMimeType = attrs.getValue(CAS.FEATURE_BASE_NAME_SOFAMIME);
        String finalSofaId = sofaID;
        fs = maybeCreateWithV2Id(fsId,
                () -> cas.createSofa(this.indexMap.get(extSofaNum), finalSofaId, sofaMimeType));
      } else { // not a Sofa
        if (type.isAnnotationBaseType()) {

          // take pains to create FS in the right view.
          // the XCAS external form sometimes uses "sofa" and sometimes uses "_ref_sofa"
          // - these have different semantics:
          // -- sofa = value is the sofaNum
          // -- _ref_sofa = value is the external ID of the associated sofa feature structure
          String extSofaNum = attrs.getValue(CAS.FEATURE_BASE_NAME_SOFA);
          CAS casView;
          if (extSofaNum != null) {
            casView = cas.getView((this.indexMap.size() == 1) ? 1 // case of no Sofa, but view ref =
                                                                  // 1 = _InitialView
                    : this.indexMap.get(Integer.parseInt(extSofaNum)));
          } else {
            String extSofaRefString = attrs
                    .getValue(XCASSerializer.REF_PREFIX + CAS.FEATURE_BASE_NAME_SOFA);
            if (null == extSofaRefString || extSofaRefString.length() == 0) {
              throw createException(XCASParsingException.SOFA_REF_MISSING);
            }
            casView = cas.getView((Sofa) (fsTree.get(Integer.parseInt(extSofaRefString)).fs));
          }
          if (type.getCode() == TypeSystemConstants.docTypeCode) {
            fs = maybeCreateWithV2Id(fsId, () -> casView.getDocumentAnnotation());
            // fs = casView.getDocumentAnnotation();
            cas.removeFromCorruptableIndexAnyView(fs, cas.getAddbackSingle());
          } else {
            fs = maybeCreateWithV2Id(fsId, () -> casView.createFS(type));
            if (currentFs instanceof UimaSerializable) {
              UimaSerializable ufs = (UimaSerializable) currentFs;
              uimaSerializableFixups.add(() -> ufs._init_from_cas_data());
            }
          }
        } else { // not an annotation base
          fs = maybeCreateWithV2Id(fsId, () -> cas.createFS(type));
          if (currentFs instanceof UimaSerializable) {
            UimaSerializable ufs = (UimaSerializable) currentFs;
            uimaSerializableFixups.add(() -> ufs._init_from_cas_data());
          }
        }
      }

      // Hang on to FS for setting content feature (things coded as child xml elements)
      this.currentFs = fs;
      int extId = -1;
      IntVector indexRep = new IntVector(1); // empty means not indexed

   // @formatter:off
      /****************************************************************
       * Loop for all feature specs                                   *
       *   - handle features with _ reserved prefix, including _ref_  *
       *   - handle features without "_" prefix:                      *
       *      - if not Sofa                                           *
       ****************************************************************/
   // @formatter:on
      for (int i = 0; i < attrs.getLength(); i++) {
        final String attrName = attrs.getQName(i);
        String attrValue = attrs.getValue(i);
        if (attrName.startsWith(reservedAttrPrefix)) {
          if (attrName.equals(XCASSerializer.ID_ATTR_NAME)) {
            try {
              extId = Integer.parseInt(attrValue);
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
            handleFeature(type, fs, attrName, attrValue, false);
          }
        } else {
          if (sofaTypeCode == typecode) {
            if (attrName.equals(CAS.FEATURE_BASE_NAME_SOFAID)
                    && attrValue.equals("_DefaultTextSofaName")) {
              // fixup old default Sofa name to new one
              attrValue = CAS.NAME_DEFAULT_SOFA;
            }
          }

          if (!type.isAnnotationBaseType() || !attrName.equals(CAS.FEATURE_BASE_NAME_SOFA)) {
            // skip the setting the sofa feature for Annotation base subtypes - this is set from the
            // view
            // otherwise handle the feature
            handleFeature(type, fs, attrName, attrValue, false);
          }
        }
      }

      if (type.getCode() == TypeSystemConstants.docTypeCode) {
        cas.addbackSingle(fs);
      }

      if (sofaTypeCode == typecode) {
        Sofa sofa = (Sofa) fs;
        cas.getBaseIndexRepository().addFS(sofa);
        CAS view = cas.getView(sofa);
        // internal sofaNum == 1 always means initial sofa
        if (sofa.getSofaRef() == 1) {
          cas.registerInitialSofa();
        } else {
          // add indexRepo for views other than the initial view
          indexRepositories.add(cas.getSofaIndexRepository(sofa));
        }
        ((CASImpl) view).registerView(sofa);
        views.add(view);
      }
      FSInfo fsInfo = new FSInfo(fs, indexRep);
      if (extId < 0) {
        idLess.add(fsInfo);
      } else {
        fsTree.put(extId, fsInfo);
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
      final int finalSize = size;
      TOP fs = maybeCreateWithV2Id(fsId, () -> cas.createArray(type, finalSize));
      // TOP fs = cas.createArray(type, size);

      FSInfo fsInfo = new FSInfo(fs, indexRep);
      if (id >= 0) {
        fsTree.put(id, fsInfo);
      } else {
        idLess.add(fsInfo);
      }
      // Hang on to those for setting array values.
      this.currentFs = fs;
      this.arrayPos = 0;

      this.state = ARRAY_ELE_STATE;
    }

    // The definition of a null value. Any other value must be in the expected
    // format.
    private final boolean emptyVal(String val) {
      return ((val == null) || (val.length() == 0));
    }

    // Create a feature value from a string representation.
    private void handleFeature(TOP fs, String featName, String featVal, boolean lenient)
            throws SAXParseException {
      Type type = fs._getTypeImpl();
      handleFeature(type, fs, featName, featVal, lenient);
    }

    private void handleFeature(final Type type, TOP fs, String featName, String featValIn,
            boolean lenient) throws SAXParseException {
      // The FeatureMap approach is broken because it assumes feature short names
      // are unique. This is my quick fix. -APL
      // final FeatureImpl feat = (FeatureImpl) featureMap.get(featName);

      // handle v1.x format annotations, mapping int to ref values
      final String featVal = (featName.equals("sofa") && ((TypeImpl) type).isAnnotationBaseType())
              ? Integer.toString(this.sofaRefMap
                      .get(((Sofa) fsTree.get(Integer.parseInt(featValIn)).fs).getSofaNum()))
              : featValIn;

      // handle v1.x sofanum values, remapping so that _InitialView always == 1
      // Bypassed in v3 of UIMA because sofa was already created with the right sofanum
      // if (featName.equals(CAS.FEATURE_BASE_NAME_SOFAID) && (fs instanceof Sofa)) {
      // Sofa sofa = (Sofa) fs;
      // int sofaNum = sofa.getSofaNum();
      // sofa._setIntValueNcNj(Sofa._FI_sofaNum, this.indexMap.get(sofaNum));
      //
      //// Type sofaType = ts.sofaType;
      //// final FeatureImpl sofaNumFeat = (FeatureImpl) sofaType
      //// .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_SOFANUM);
      //// int sofaNum = cas.getFeatureValue(addr, sofaNumFeat.getCode());
      //// cas.setFeatureValue(addr, sofaNumFeat.getCode(), this.indexMap.get(sofaNum));
      // }

      String realFeatName = getRealFeatName(featName);

      final FeatureImpl feat = (FeatureImpl) type.getFeatureByBaseName(realFeatName);
      if (feat == null) { // feature does not exist in typesystem
        if (outOfTypeSystemData != null) {
          // Add to Out-Of-Typesystem data (APL)
          List<Pair<String, Object>> ootsAttrs = outOfTypeSystemData.extraFeatureValues
                  .computeIfAbsent(fs, k -> new ArrayList<>());
          ootsAttrs.add(new Pair(featName, featVal));
        } else if (!lenient) {
          throw createException(XCASParsingException.UNKNOWN_FEATURE, featName);
        }
      } else {
        // feature is not null
        if (feat.getRangeImpl().isRefType) {
          // queue up a fixup action to be done
          // after the external ids get properly associated with
          // internal ones.

          fixupToDos.add(() -> finalizeRefValue(Integer.parseInt(featVal), fs, feat));
        } else { // is not a ref type.
          CASImpl.setFeatureValueFromStringNoDocAnnotUpdate(fs, feat, featVal);
        }

      }
    }

    private String getRealFeatName(String featName) {
      return featName.startsWith(XCASSerializer.REF_PREFIX)
              ? featName.substring(XCASSerializer.REF_PREFIX.length())
              : featName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    @Override
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
        case FEAT_STATE: {
          this.state = FS_STATE;
          break;
        }
        case CONTENT_STATE: {
          // Set the value of the content feature.
          if (!isAllWhitespace(buffer)) {
            try {
              handleFeature(currentFs, currentContentFeat, buffer.toString(), true);
            } catch (XCASParsingException x) {
              // Not sure why we are calling handleFeature for WF content
            }
          }
          this.state = FS_STATE;
          break;
        }
        case FEAT_CONTENT_STATE: {
          // Create a feature value from an element.
          handleFeature(currentFs, qualifiedName, buffer.toString(), false);
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
          Sofa newSofa = (Sofa) maybeCreateWithV2Id(1, () -> cas.createInitialSofa("text"));
          // Sofa newSofa = cas.createInitialSofa("text");
          CASImpl initialView = cas.getInitialView();
          initialView.registerView(newSofa);
          // Set the document text without creating a documentAnnotation
          initialView.setDocTextFromDeserializtion(buffer.toString());

          // and assume the new Sofa is at location 1!
          int id = 1;
          this.sofaRefMap.add(id);

          // and register the id for this Sofa
          FSInfo fsInfo = new FSInfo(newSofa, new IntVector());
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
      if (currentFs instanceof CommonPrimitiveArray) {
        CommonPrimitiveArray fsa = (CommonPrimitiveArray) currentFs;
        if (arrayPos >= fsa.size()) {
          throw createException(XCASParsingException.EXCESS_ARRAY_ELE);
        }
        try {
          if (!emptyVal(content)) {
            fsa.setArrayValueFromString(arrayPos, content);
          }
        } catch (NumberFormatException e) {
          throw createException(XCASParsingException.INTEGER_EXPECTED, content);
        }
      } else {
        // is ref array (FSArray)
        if (content != null && content.length() > 0) {
          final FSArray fsa = (FSArray) currentFs;
          final int pos = arrayPos;
          final int extId = Integer.parseInt(content);

          fixupToDos.add(() -> finalizeArrayRefValue(extId, pos, fsa));
        }
      }
      ++arrayPos;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    @Override
    public void endDocument() throws SAXException {
      // time = System.currentTimeMillis() - time;
      // System.out.println("Done reading xml data in " + new TimeSpan(time));
      // System.out.println(
      // "Resolving references for id data (" + fsTree.size() + ").");
      // time = System.currentTimeMillis();
      // Resolve references, index.
      for (Runnable fixup : fixupToDos) {
        fixup.run();
      }
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
        AutoCloseable ac = view.protectIndexes();
        try {
          ((CASImpl) view).updateDocumentAnnotation();
        } finally {
          try {
            ac.close();
          } catch (Exception e) {
            Misc.internalError();
          }
        }
      }
      // for (int i = 0; i < views.size(); i++) {
      // ((CASImpl) views.get(i)).updateDocumentAnnotation();
      // }
      for (Runnable r : uimaSerializableFixups) {
        r.run();
      }
    }

    private void finalizeFS(FSInfo fsInfo) {
      finalizeAddToIndexes(fsInfo); // must be done after fixes the sofa refs
    }

    /**
     * Common code run at finalize time, to set ref values and handle out-of-typesystem data
     * 
     * @param extId
     *          the external ID identifying either a deserialized FS or an out-of-typesystem
     *          instance
     * @param fs
     *          Feature Structure whose fi reference feature is to be set with a value derived from
     *          extId and FSinfo
     * @param fi
     *          the featureImpl
     */
    private void finalizeRefValue(int extId, TOP fs, FeatureImpl fi) {
      FSInfo fsInfo = fsTree.get(extId);
      if (fsInfo == null) {

        // this feature may be a ref to an out-of-typesystem FS.
        // add it to the Out-of-typesystem features list (APL)
        if (extId != 0 && outOfTypeSystemData != null) {
          List<Pair<String, Object>> ootsAttrs = outOfTypeSystemData.extraFeatureValues
                  .computeIfAbsent(fs, k -> new ArrayList<>());
          String featFullName = fi.getName();
          int separatorOffset = featFullName.indexOf(TypeSystem.FEATURE_SEPARATOR);
          String featName = "_ref_" + featFullName.substring(separatorOffset + 1);
          ootsAttrs.add(new Pair(featName, Integer.toString(extId)));
        }
        CASImpl.setFeatureValueMaybeSofa(fs, fi, null);
      } else {
        // the sofa ref in annotationBase is set when the fs is created, not here
        if (fi.getCode() != TypeSystemConstants.annotBaseSofaFeatCode) {
          if (fs instanceof Sofa) {
            // special setters for sofa values
            Sofa sofa = (Sofa) fs;
            switch (fi.getRangeImpl().getCode()) {
              case TypeSystemConstants.sofaArrayFeatCode:
                sofa.setLocalSofaData(fsInfo.fs);
                break;
              default:
                throw new CASRuntimeException(UIMARuntimeException.INTERNAL_ERROR);
            }
            return;
          }

          // handle case where feature is xyz[] (an array ref, not primitive) but the value of fs is
          // FSArray
          ts.fixupFSArrayTypes(fi.getRangeImpl(), fsInfo.fs);
          CASImpl.setFeatureValueMaybeSofa(fs, fi, fsInfo.fs);
        }
      }
    }

    /**
     * Same as above, but specialized for array values, not feature slot values
     * 
     * @param extId
     * @param extId
     *          the external ID identifying either a deserialized FS or an out-of-typesystem
     *          instance
     * @param pos
     *          the index in the array
     * @return the TOP instance to be set as the value in an array or as the value of a feature.
     * @return
     */
    private void finalizeArrayRefValue(int extId, int pos, FSArray fs) {
      FSInfo fsInfo = fsTree.get(extId);

      if (fsInfo == null) {

        // this element may be a ref to an out-of-typesystem FS.
        // add it to the Out-of-typesystem array elements list (APL)
        if (extId != 0 && outOfTypeSystemData != null) {
          List<ArrayElement> ootsElements = outOfTypeSystemData.arrayElements.computeIfAbsent(fs,
                  k -> new ArrayList<>());
          // the "value" of the reference is the ID, but we prefix with a letter to indicate
          // that this ID refers to an array OOTS FS
          ArrayElement ootsElem = new ArrayElement(pos, "a" + Integer.toString(extId));
          ootsElements.add(ootsElem);
        }
        fs.set(pos, null);
      } else {
        fs.set(pos, fsInfo.fs);
      }
    }

    private void finalizeAddToIndexes(final FSInfo fsInfo) {
      if (fsInfo.indexRep.size() >= 0) {
        // Now add FS to all specified index repositories
        for (int i = 0; i < fsInfo.indexRep.size(); i++) {
          if (indexMap.size() == 1) {
            ((FSIndexRepositoryImpl) indexRepositories.get(fsInfo.indexRep.get(i)))
                    .addFS(fsInfo.fs);
          } else {
            ((FSIndexRepositoryImpl) indexRepositories.get(indexMap.get(fsInfo.indexRep.get(i))))
                    .addFS(fsInfo.fs);
          }
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
      for (Map.Entry<String, Object> entry : aFS.featVals.entrySet()) {
        String attrName = entry.getKey();
        if (attrName.startsWith("_ref_")) {
          int val = Integer.parseInt((String) entry.getValue());
          if (val >= 0) // negative numbers represent null and are left unchanged
          {
            // attempt to locate target in type system
            FSInfo fsValInfo = fsTree.get(val);
            if (fsValInfo != null) {
              // entry.setValue(Integer.toString(fsValInfo.fs._id));
              entry.setValue(fsValInfo.fs);
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
      for (List<Pair<String, Object>> attrs : outOfTypeSystemData.extraFeatureValues.values()) {
        for (Pair<String, Object> p : attrs) {
          String sv = (p.u instanceof String) ? (String) p.u : "";
          if (p.t.startsWith("_ref_")) {
            int val = Integer.parseInt(sv);
            if (val >= 0) // negative numbers represent null and are left unchanged
            {
              // attempt to locate target in type system
              FSInfo fsValInfo = fsTree.get(val);
              if (fsValInfo != null) {
                p.u = fsValInfo.fs;
              } else
              // out of type system - remap by prepending letter
              {
                p.u = "a" + val;
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
      // System.out.println("Setting document locator.");
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

    /*
     * Adds a feature structure to the out-of-typesystem data, and sets the parser's state
     * appropriately. (APL)
     */
    private void addToOutOfTypeSystemData(String typeName, Attributes attrs)
            throws XCASParsingException {
      if (this.outOfTypeSystemData != null) {
        FSData fsData = new FSData();
        fsData.type = typeName;
        fsData.indexRep = null; // not indexed
        String attrName, attrValue;
        for (int i = 0; i < attrs.getLength(); i++) {
          attrName = attrs.getQName(i);
          attrValue = attrs.getValue(i);
          if (attrName.startsWith(reservedAttrPrefix)) {
            if (attrName.equals(XCASSerializer.ID_ATTR_NAME)) {
              fsData.id = attrValue;
            } else if (attrName.equals(XCASSerializer.CONTENT_ATTR_NAME)) {
              this.currentContentFeat = attrValue;
            } else if (attrName.equals(XCASSerializer.INDEXED_ATTR_NAME)) {
              fsData.indexRep = attrValue;
            } else {
              fsData.featVals.put(attrName, attrValue);
            }
          } else {
            fsData.featVals.put(attrName, attrValue);
          }
        }
        this.outOfTypeSystemData.fsList.add(fsData);
        this.currentOotsFs = fsData;
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
        arrayVals = Constants.EMPTY_STRING_ARRAY;
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

    TOP maybeCreateWithV2Id(int id, Supplier<TOP> create) {
      if (cas.is_ll_enableV2IdRefs()) {
        cas.set_reuseId(id);
        try {
          TOP fs = create.get();
          if (highestIdFs == null) {
            highestIdFs = fs;
          } else if (highestIdFs._id < fs._id) {
            highestIdFs = fs; // for setting up getNextId at end
          }
          return fs;
        } finally {
          cas.set_reuseId(0); // in case of error throw
        }
      } else {
        return create.get();
      }
    }
  }

  final private TypeSystemImpl ts;

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
   * @param uimaContext
   *          the UIMA Context for the deserialization
   */
  public XCASDeserializer(TypeSystem ts, UimaContext uimaContext) {
    this.ts = (TypeSystemImpl) ts;
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
   * @param aReader
   *          Reader from which to read the XCAS XML document
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
  public static void deserialize(Reader aReader, CAS aCAS, boolean aLenient)
          throws SAXException, IOException {
    deserialize(new InputSource(aReader), aCAS, aLenient);
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
    deserialize(new InputSource(aStream), aCAS, aLenient);
  }

  public static void deserialize(InputSource aSource, CAS aCAS, boolean aLenient)
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
    xmlReader.parse(aSource);

    CASImpl casImpl = ((CASImpl) aCAS.getLowLevelCAS());
    if (casImpl.is_ll_enableV2IdRefs()) {
      TOP highest_fs = ((XCASDeserializerHandler) handler).highestIdFs;

      casImpl.setLastUsedFsId(highest_fs._id);
      casImpl.setLastFsV2Size(highest_fs._getTypeImpl().getFsSpaceReq(highest_fs));
    }
  }
}
