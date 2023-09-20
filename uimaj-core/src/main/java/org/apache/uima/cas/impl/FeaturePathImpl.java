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

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeaturePath;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeClass;
import org.apache.uima.cas.impl.TypeSystemUtils.PathValid;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.cas.TOP;

/**
 * Implementation of the feature path interface.
 */
class FeaturePathImpl implements FeaturePath {

  private static final String MESSAGE_DIGEST = "org.apache.uima.cas.impl.annot_impl";
  private static final String FEATURE_PATH_SEPARATOR = "/";
  private static final String BUILT_IN_FUNCTION_SEPARATOR = ":";

  private static final byte NO_BUILT_IN_FUNCTION = 0;
  private static final byte FUNCTION_COVERED_TEXT = 1;
  private static final byte FUNCTION_ID = 2;
  private static final byte FUNCTION_TYPE_NAME = 3;

  private static final String FUNCTION_NAME_COVERED_TEXT = "coveredtext()";
  private static final String FUNCTION_NAME_ID = "fsid()";
  private static final String FUNCTION_NAME_TYPE_NAME = "typename()";

  private static final TOP FEATURE_PATH_FAILED = new TOP();

  /**
   * The path's builtInFunction, or 0
   */
  private byte builtInFunction = 0;
  private String originalBuiltInName = null;

  // featurePath element names
  final private ArrayList<String> featurePathElementNames = new ArrayList<>();

  private boolean pathStartsWithSlash = true;
  /**
   * FeatureImpl array corresponding to feature path names. This can change for each evaluation of
   * this FeaturePath instance against a different starting Feature Structure. It can be pre-set-up
   * using typeInit. It has values for the first names in the featurePathElementNames which are
   * always valid for a given starting type (set into boundBaseType).
   */
  final private ArrayList<FeatureImpl> boundFeatures = new ArrayList<>();

  private FeatureImpl targetFeature; // set to the last value of boundFeatures
  private TypeImpl targetType; // set to type of range of last found feature, works when there are
                               // no features

  /**
   * The Type used as the starting type for path validation This must be non-null if boundFeatures
   * size > 0;
   */
  private TypeImpl boundBaseType = null;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#addFeature(org.apache.uima.cas.Feature)
   */
  @Override
  public void addFeature(Feature feat) {

    // check if currently feature path ends with a built-in function
    if (builtInFunction > 0) {
      throw new CASRuntimeException(MESSAGE_DIGEST, "INVALID_FEATURE_PATH_SYNTAX_ADD",
              new Object[] { getFeaturePathString(), feat.getShortName() });
    }

    // add feature to feature path
    featurePathElementNames.add(feat.getShortName());
    boundFeatures.add((FeatureImpl) feat);

    // if current featurePath was already initialized we cannot guarantee that
    // the path is still ever valid so we have to evaluate the path on the
    // fly.
    if (boundBaseType != null && PathValid.NEVER == TypeSystemUtils
            .isPathValid(boundBaseType, featurePathElementNames)) {
      boundBaseType = null; // can't be used for this path
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#getFeature(int)
   */
  @Override
  public FeatureImpl getFeature(int i) {
    return (size() == boundFeatures.size()) ? boundFeatures.get(i) : null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#size()
   */
  @Override
  public int size() {
    return featurePathElementNames.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#initialize(java.lang.String)
   */
  @Override
  public void initialize(String featurePath) throws CASException {

    builtInFunction = NO_BUILT_IN_FUNCTION;
    originalBuiltInName = null;

    // throw exception if featurePath is null
    if (featurePath == null) {
      throw new CASException(MESSAGE_DIGEST, "INVALID_FEATURE_PATH_SYNTAX",
              new Object[] { featurePath, "null for a feature path" });
    }

    pathStartsWithSlash = featurePath.startsWith("/"); // v2 compatibility

    // check featurePath for invalid character sequences
    if (featurePath.indexOf("//") > -1) { // two forward slashes in a
                                          // row is invalid
      // invalid featurePath syntax
      throw new CASException(MESSAGE_DIGEST, "INVALID_FEATURE_PATH_SYNTAX",
              new Object[] { featurePath, "//" });
    }

    featurePathElementNames.clear();
    // parse feature path into path elements
    StringTokenizer tokenizer = new StringTokenizer(featurePath, FEATURE_PATH_SEPARATOR);
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      // check if there are more tokens available, if we are at the last
      // token we have to check for built-in functions
      if (tokenizer.hasMoreTokens()) {
        featurePathElementNames.add(token);
      } else {
        // we have the last token, check for built-in functions
        int index = -1;
        if ((index = token.indexOf(BUILT_IN_FUNCTION_SEPARATOR)) != -1) {
          if (index > 0) {
            // we have a built-in function that is separated with a ":"
            featurePathElementNames.add(token.substring(0, index));
          }
          // get built-in function
          originalBuiltInName = token.substring(index + 1);
          String builtInFunctionName = originalBuiltInName.toLowerCase();
          if (builtInFunctionName.equals(FUNCTION_NAME_COVERED_TEXT)) {
            builtInFunction = FUNCTION_COVERED_TEXT;
          } else if (builtInFunctionName.equals(FUNCTION_NAME_ID)) {
            builtInFunction = FUNCTION_ID;
          } else if (builtInFunctionName.equals(FUNCTION_NAME_TYPE_NAME)) {
            builtInFunction = FUNCTION_TYPE_NAME;
          } else {
            throw new CASException(MESSAGE_DIGEST, "INVALID_FEATURE_PATH_SYNTAX",
                    new Object[] { featurePath, builtInFunctionName });
          }
        } else {
          featurePathElementNames.add(token);
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#typeInit(org.apache.uima.cas.Type)
   */
  @Override
  public void typeInit(Type typeAtStartOfFeaturePath) throws CASException {

    boundBaseType = (TypeImpl) typeAtStartOfFeaturePath;

    // do feature path type initialization only if a featurePath is available
    if (featurePathElementNames.size() > 0) {

      // LowLevelTypeSystem llTypeSystem = ((TypeImpl) featurePathType)
      // .getTypeSystem().getLowLevelTypeSystem();

      // store featurePathType
      boundBaseType = (TypeImpl) typeAtStartOfFeaturePath;

      // validate featurePath for given type
      if (PathValid.NEVER == TypeSystemUtils.isPathValid(typeAtStartOfFeaturePath,
              featurePathElementNames)) {
        // invalid featurePath - throw an configuration exception
        throw new CASException(MESSAGE_DIGEST, "ERROR_VALIDATE_FEATURE_PATH",
                new Object[] { getFeaturePathString(), typeAtStartOfFeaturePath.getName() });
      } else {
        // is ALWAYS or POSSIBLE.
        // ALWAYS means all features are available at the top-most type
        // POSSIBLE means one or more features is not present at the top-most type, but are
        // available in
        // one or more subtypes.

        boundFeatures.clear(); // reset
        // object
        TypeImpl currentType = (TypeImpl) typeAtStartOfFeaturePath;
        // iterate over all featurePathNames and store the resolved CAS
        // feature in the boundFeatures list, until one not found
        for (String featName : featurePathElementNames) {
          FeatureImpl fi = currentType.getFeatureByBaseName(featName);
          if (fi != null) {
            boundFeatures.add(fi);
            currentType = fi.getRangeImpl();
          } else {
            break;
          }
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#getFeaturePath()
   */
  @Override
  public String getFeaturePath() {
    return getFeaturePathString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#getBooleanValue(org.apache.uima.cas. FeatureStructure)
   */
  @Override
  public Boolean getBooleanValue(FeatureStructure fs) {
    TOP tgtFs = getTargetFs((TOP) fs);
    return (tgtFs == FEATURE_PATH_FAILED) ? null : tgtFs.getBooleanValue(targetFeature);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#getByteValue(org.apache.uima.cas. FeatureStructure)
   */
  @Override
  public Byte getByteValue(FeatureStructure fs) {
    TOP tgtFs = getTargetFs((TOP) fs);
    return (tgtFs == FEATURE_PATH_FAILED) ? null : tgtFs.getByteValue(targetFeature);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#getDoubleValue(org.apache.uima.cas. FeatureStructure)
   */
  @Override
  public Double getDoubleValue(FeatureStructure fs) {
    TOP tgtFs = getTargetFs((TOP) fs);
    return (tgtFs == FEATURE_PATH_FAILED) ? null : tgtFs.getDoubleValue(targetFeature);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#getFloatValue(org.apache.uima.cas. FeatureStructure)
   */
  @Override
  public Float getFloatValue(FeatureStructure fs) {
    TOP tgtFs = getTargetFs((TOP) fs);
    return (tgtFs == FEATURE_PATH_FAILED) ? null : tgtFs.getFloatValue(targetFeature);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#getFSValue(org.apache.uima.cas. FeatureStructure)
   */
  @Override
  public FeatureStructure getFSValue(FeatureStructure fs) {
    TOP tgtFs = getTargetFs((TOP) fs);
    return (tgtFs == FEATURE_PATH_FAILED) ? null : tgtFs;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#getIntValue(org.apache.uima.cas. FeatureStructure)
   */
  @Override
  public Integer getIntValue(FeatureStructure fs) {
    TOP tgtFs = getTargetFs((TOP) fs);
    return (tgtFs == FEATURE_PATH_FAILED) ? null : tgtFs.getIntValue(targetFeature);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#getLongValue(org.apache.uima.cas. FeatureStructure)
   */
  @Override
  public Long getLongValue(FeatureStructure fs) {
    TOP tgtFs = getTargetFs((TOP) fs);
    return (tgtFs == FEATURE_PATH_FAILED) ? null : tgtFs.getLongValue(targetFeature);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#getShortValue(org.apache.uima.cas. FeatureStructure)
   */
  @Override
  public Short getShortValue(FeatureStructure fs) {
    TOP tgtFs = getTargetFs((TOP) fs);
    return (tgtFs == FEATURE_PATH_FAILED) ? null : tgtFs.getShortValue(targetFeature);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#getStringValue(org.apache.uima.cas. FeatureStructure)
   */
  @Override
  public String getStringValue(FeatureStructure fs) {
    TOP tgtFs = getTargetFs((TOP) fs);
    return (tgtFs == FEATURE_PATH_FAILED) ? null : tgtFs.getStringValue(targetFeature);
  }

  // /*
  // * (non-Javadoc)
  // *
  // * @see
  // * org.apache.uima.cas.FeaturePath#getJavaObjectValue(org.apache.uima.cas.
  // * FeatureStructure)
  // */
  // @Override
  // public Object getJavaObjectValue(FeatureStructure fs) {
  // TOP tgtFs = getTargetFs((TOP) fs);
  // return (tgtFs == FEATURE_PATH_FAILED) ? null : tgtFs.getJavaObjectValue(targetFeature);
  // }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#getType(org.apache.uima.cas. FeatureStructure)
   */
  @Override
  public Type getType(FeatureStructure fs) {
    TOP tgtFs = getTargetFs((TOP) fs);
    return (tgtFs == FEATURE_PATH_FAILED) ? null : targetType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#getTypClass(org.apache.uima.cas. FeatureStructure)
   * 
   * @deprecated use getTypeClass instead (spelling correction)
   */
  @Deprecated
  @Override
  public TypeClass getTypClass(FeatureStructure fs) {
    TypeImpl type = (TypeImpl) getType(fs);
    return (type == null) ? null : TypeClass.values()[TypeSystemImpl.getTypeClass(type)];
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#getTypClass(org.apache.uima.cas. FeatureStructure)
   */
  @Override
  public TypeClass getTypeClass(FeatureStructure fs) {
    TypeImpl type = (TypeImpl) getType(fs);
    return (type == null) ? null : TypeClass.values()[TypeSystemImpl.getTypeClass(type)];
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#getValueAsString(org.apache.uima.cas. FeatureStructure)
   */
  @Override
  public String ll_getValueAsString(int fsRef, LowLevelCAS llCas) {
    TOP fs = llCas.ll_getFSForRef(fsRef);
    return getValueAsString(fs);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FeaturePath#ll_getValueAsString(int,
   * org.apache.uima.cas.impl.LowLevelCAS)
   */
  @Override
  public String getValueAsString(FeatureStructure fs) {
    TOP tgtFs = getTargetFs((TOP) fs);
    if ((tgtFs == FEATURE_PATH_FAILED) || (targetType == null)) {
      return null;
    }
    switch (TypeSystemImpl.getTypeClass(targetType)) {
      case LowLevelCAS.TYPE_CLASS_INVALID:
        return null;

      case LowLevelCAS.TYPE_CLASS_STRING:
      case LowLevelCAS.TYPE_CLASS_BOOLEAN:
      case LowLevelCAS.TYPE_CLASS_BYTE:
      case LowLevelCAS.TYPE_CLASS_SHORT:
      case LowLevelCAS.TYPE_CLASS_INT:
      case LowLevelCAS.TYPE_CLASS_LONG:
      case LowLevelCAS.TYPE_CLASS_FLOAT:
      case LowLevelCAS.TYPE_CLASS_DOUBLE:
        verifyNoBuiltInFunction();
        return tgtFs.getFeatureValueAsString(targetFeature);

      case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY:
      case LowLevelCAS.TYPE_CLASS_BYTEARRAY:
      case LowLevelCAS.TYPE_CLASS_SHORTARRAY:
      case LowLevelCAS.TYPE_CLASS_INTARRAY:
      case LowLevelCAS.TYPE_CLASS_LONGARRAY:
      case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
      case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY:
      case LowLevelCAS.TYPE_CLASS_STRINGARRAY:
      case LowLevelCAS.TYPE_CLASS_FSARRAY:
        if (builtInFunction > NO_BUILT_IN_FUNCTION) {
          return evaluateBuiltInFunction(tgtFs);
        }
        return ((CommonArrayFS) tgtFs).getValuesAsCommaSeparatedString();

      case LowLevelCAS.TYPE_CLASS_FS:
        if (tgtFs == null) {
          return null;
        }
        if (builtInFunction > NO_BUILT_IN_FUNCTION) {
          return evaluateBuiltInFunction(tgtFs);
        }
        return tgtFs.toString();
    } // end of switch
    return null;
  }

  /**
   * Method that throws the CASRuntimeException for an unsupported built-in function
   * 
   * @param typeName
   *          type name that does not support the built-in function
   */
  private void throwBuiltInFunctionException(String typeName) {
    // get built-in function name
    String functionName = null;
    if (builtInFunction == FUNCTION_COVERED_TEXT) {
      functionName = FUNCTION_NAME_COVERED_TEXT;
    } else if (builtInFunction == FUNCTION_ID) {
      functionName = FUNCTION_NAME_ID;
    } else if (builtInFunction == FUNCTION_TYPE_NAME) {
      functionName = FUNCTION_NAME_TYPE_NAME;
    }
    // throw runtime exception
    throw new CASRuntimeException(MESSAGE_DIGEST, "BUILT_IN_FUNCTION_NOT_SUPPORTED",
            new Object[] { functionName, typeName });
  }

  /**
   * evaluate the built-in function for the returned FeatureStructure
   * 
   * @param returnFS
   *          FeatureStructure that is returned
   * 
   * @return Returns the built-in function value for the given FS.
   */
  private String evaluateBuiltInFunction(TOP returnFS) {
    if (builtInFunction == FUNCTION_COVERED_TEXT) {
      if (returnFS instanceof AnnotationFS) {
        return ((AnnotationFS) returnFS).getCoveredText();
      } else {
        throw new CASRuntimeException(MESSAGE_DIGEST, "BUILT_IN_FUNCTION_NOT_SUPPORTED",
                new Object[] { FUNCTION_NAME_COVERED_TEXT, returnFS.getType().getName() });
      }
    } else if (builtInFunction == FUNCTION_ID) {
      return Integer.toString(returnFS._id);

    } else if (builtInFunction == FUNCTION_TYPE_NAME) {
      return returnFS.getType().getName();
    }
    return null;
  }

//@formatter:off
  /**
   * evaluates the internal feature path for the given FeatureStructure
   * 
   * returns
   * 
   * 
   * @param fs
   *          FeatureStructure to use as the starting point for the feature path
   * 
   * @return
   *   the Feature Structure result, 
   *   or the Feature Structure to run feature or array extraction on,
   *   or the Feature Structure to run a built-in function on
   *   or null (meaning no fs was found that matched the path).
   * 
   *         For 0 element feature paths, this is the same as the argument.
   * 
   *         For null fs, the returned value is null;
   * 
   */
//@formatter:on
  private TOP getTargetFs(TOP fs) {

    if (null == fs) {
      return FEATURE_PATH_FAILED;
    }

    if (featurePathElementNames.size() == 0) {
      targetType = fs._getTypeImpl();
      return fs;
    }

    // we have a feature path that must be evaluated

    if (boundBaseType == null || !boundBaseType.subsumes(fs._getTypeImpl())) {
      boundFeatures.clear(); // reset if supplied FS not the one the features were calculated for.
      boundBaseType = fs._getTypeImpl();
    }

    // set current FS values
    TOP currentFs = fs;

    TypeImpl rangeType = null;
    int rangeTypeClass = -1;

    // resolve feature path value
    for (int i = 0; i < featurePathElementNames.size(); i++) {
      if (currentFs == null) {
        return FEATURE_PATH_FAILED;
      }

      if (i < boundFeatures.size()) {
        targetFeature = boundFeatures.get(i);
        /*
         * It is possible that the previously bound feature isn't valid for this FS. This can happen
         * if a type hierarchy defines 2 different features for two different subtypes of type Tt
         * with the same feature name.
         * 
         * So we check if this bound feature is appropriate for the current FS
         */
        if (!((TypeImpl) targetFeature.getDomain()).subsumes(currentFs._getTypeImpl())) {
          setTargetFeature(currentFs, i);
        }
      } else {
        setTargetFeature(currentFs, i);
      }

      // switch feature type class
      // currentRangeTypeCode = llCas.ll_getTypeSystem().ll_getRangeType(targetFeatureCode);
      targetType = rangeType = targetFeature.getRangeImpl();
      rangeTypeClass = TypeSystemImpl.getTypeClass(rangeType);

      switch (rangeTypeClass) {
        case LowLevelCAS.TYPE_CLASS_STRING:
        case LowLevelCAS.TYPE_CLASS_INT:
        case LowLevelCAS.TYPE_CLASS_BOOLEAN:
        case LowLevelCAS.TYPE_CLASS_BYTE:
        case LowLevelCAS.TYPE_CLASS_DOUBLE:
        case LowLevelCAS.TYPE_CLASS_FLOAT:
        case LowLevelCAS.TYPE_CLASS_LONG:
        case LowLevelCAS.TYPE_CLASS_SHORT:
        case LowLevelCAS.TYPE_CLASS_INVALID:
          return currentFs; // is the fs which has the feature which is the primitive value

        case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY:
        case LowLevelCAS.TYPE_CLASS_BYTEARRAY:
        case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY:
        case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
        case LowLevelCAS.TYPE_CLASS_FSARRAY:
        case LowLevelCAS.TYPE_CLASS_INTARRAY:
        case LowLevelCAS.TYPE_CLASS_LONGARRAY:
        case LowLevelCAS.TYPE_CLASS_SHORTARRAY:
        case LowLevelCAS.TYPE_CLASS_STRINGARRAY:
          return currentFs.getFeatureValue(targetFeature);

        case LowLevelCAS.TYPE_CLASS_FS:
          currentFs = currentFs.getFeatureValue(targetFeature);
          if (currentFs == null) {
            if (i == (featurePathElementNames.size() - 1)) {
              // at the last element, keep targetType == to the range type
            } else {
              //@formatter:off
            /*
             * not at the last element, so terminating the feature path prematurely.
             * There are 2 cases:
             *   - the PathValid is POSSIBLE 
             *   - the PathValid is ALWAYS 
             */
              //@formatter:on
              PathValid pathValid = TypeSystemUtils.isPathValid(boundBaseType,
                      featurePathElementNames);
              if (pathValid == PathValid.POSSIBLE) {
                targetType = null; // following v2 design here
              }
            }
            return null;
          }
          break;
        default:
          throw new CASRuntimeException(UIMARuntimeException.INTERNAL_ERROR);
      } // end of switch
    } // end of loop over all items in feature path

    return currentFs;
  }

  private void setTargetFeature(TOP currentFs, int i) {
    targetFeature = currentFs._getTypeImpl().getFeatureByBaseName(featurePathElementNames.get(i));
    if (targetFeature == null) {
      throw new CASRuntimeException(MESSAGE_DIGEST, "INVALID_FEATURE_PATH_FEATURE_NOT_DEFINED",
              new Object[] { getFeaturePathString(), currentFs._getTypeImpl().getName(),
                  featurePathElementNames.get(i) });
    }
    boundFeatures.add(targetFeature); // cache for future use
  }

  private void verifyNoBuiltInFunction() {
    if (builtInFunction > NO_BUILT_IN_FUNCTION) {
      throwBuiltInFunctionException(targetFeature.getRangeImpl().getName());
    }
  }

  private String getFeaturePathString() {
    StringBuilder sb = new StringBuilder();
    if (featurePathElementNames.size() == 0) {
      if (pathStartsWithSlash) {
        sb.append('/');
      }
    } else {
      for (String s : featurePathElementNames) {
        sb.append('/').append(s);
      }
    }
    appendBuiltInFunction(sb);
    return sb.toString();
  }

  private void appendBuiltInFunction(StringBuilder sb) {
    if (builtInFunction > 0) {
      sb.append(':').append(originalBuiltInName); // because capitalization could be different
    }
  }
}
