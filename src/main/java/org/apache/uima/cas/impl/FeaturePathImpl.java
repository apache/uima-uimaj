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

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeaturePath;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeClass;
import org.apache.uima.cas.impl.TypeSystemUtils.PathValid;
import org.apache.uima.cas.text.AnnotationFS;

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

   private byte builtInFunction = 0;

   // featurePath string, separated by "/"
   private String featurePathString;

   // featurePath element names
   private ArrayList<String> featurePathElementNames;

   // featurePath element features
   private ArrayList<Feature> featurePathElements;

   // featurePath low level element features
   private ArrayList<Integer> ll_featurePathElements;

   private Type featurePathBaseType;

   private int featurePathBaseTypeCode;

   /**
    * Constructor to create a new featurePath object
    */
   public FeaturePathImpl() {
      this.featurePathElementNames = new ArrayList<String>();
      this.featurePathElements = new ArrayList<Feature>();
      this.ll_featurePathElements = null;
      this.featurePathBaseType = null;
      this.featurePathBaseTypeCode = 0;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#addFeature(org.apache.uima.cas.Feature)
    */
   public void addFeature(Feature feat) {

      // check if currently feature path ends with a built-in function
      if (this.builtInFunction > 0) {
         throw new CASRuntimeException(MESSAGE_DIGEST,
               "INVALID_FEATURE_PATH_SYNTAX_ADD", new Object[] {
                     this.featurePathString, feat.getShortName() });
      }

      // add feature to feature path
      this.featurePathElementNames.add(feat.getShortName());
      if (this.featurePathString == null) {
         this.featurePathString = FEATURE_PATH_SEPARATOR + feat.getShortName();
      } else {
         this.featurePathString = this.featurePathString
               + FEATURE_PATH_SEPARATOR + feat.getShortName();
      }
      this.featurePathElements.add(feat);

      // if current featurePath was already initialized we cannot guarantee that
      // the path is still ever valid so we have to evaluate the path on the
      // fly.
      if (this.ll_featurePathElements != null) {

         // check if featurePath is still always valid
         PathValid pathValid = TypeSystemUtils.isPathValid(
               this.featurePathBaseType, this.featurePathElementNames);
         if (PathValid.ALWAYS == pathValid) {
            LowLevelTypeSystem llTypeSystem = ((TypeImpl) this.featurePathBaseType)
                  .getTypeSystem().getLowLevelTypeSystem();
            this.ll_featurePathElements.add(llTypeSystem
                  .ll_getCodeForFeature(feat));
         } else {
            this.ll_featurePathElements = null;
         }

      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#getFeature(int)
    */
   public Feature getFeature(int i) {
      if (this.featurePathElementNames.size() == this.featurePathElements
            .size()) {
         return this.featurePathElements.get(i);
      } else {
         return null;
      }

   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#size()
    */
   public int size() {
      return this.featurePathElementNames.size();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#initialize(java.lang.String)
    */
   public void initialize(String featurePath) throws CASException {

      this.featurePathString = featurePath;
      this.builtInFunction = NO_BUILT_IN_FUNCTION;

      // throw exception if featurePath is null
      if (featurePath == null) {
         throw new CASException(MESSAGE_DIGEST, "INVALID_FEATURE_PATH_SYNTAX",
               new Object[] { featurePath, "null for a feature path" });
      }

      // check featurePath for invalid character sequences
      if (this.featurePathString.indexOf("//") > -1) {
         // invalid featurePath syntax
         throw new CASException(MESSAGE_DIGEST, "INVALID_FEATURE_PATH_SYNTAX",
               new Object[] { this.featurePathString, "//" });
      }

      // parse feature path into path elements
      StringTokenizer tokenizer = new StringTokenizer(this.featurePathString,
            FEATURE_PATH_SEPARATOR);
      while (tokenizer.hasMoreTokens()) {
         String token = tokenizer.nextToken();
         // check if there are more tokens available, if we are at the last
         // token we have to check for built-in functions
         if (tokenizer.hasMoreTokens()) {
            this.featurePathElementNames.add(token);
         } else {
            // we have the last token, check for built-in functions
            int index = -1;
            if ((index = token.indexOf(BUILT_IN_FUNCTION_SEPARATOR)) != -1) {
               if (index > 0) {
                  // we have a built-in function that is separated with a ":"
                  this.featurePathElementNames.add(token.substring(0, index));
               }
               // get built-in function
               String builtInFunctionName = token.substring(index + 1)
                     .toLowerCase();
               if (builtInFunctionName.equals(FUNCTION_NAME_COVERED_TEXT)) {
                  this.builtInFunction = FUNCTION_COVERED_TEXT;
               } else if (builtInFunctionName.equals(FUNCTION_NAME_ID)) {
                  this.builtInFunction = FUNCTION_ID;
               } else if (builtInFunctionName.equals(FUNCTION_NAME_TYPE_NAME)) {
                  this.builtInFunction = FUNCTION_TYPE_NAME;
               } else {
                  throw new CASException(MESSAGE_DIGEST,
                        "INVALID_FEATURE_PATH_SYNTAX", new Object[] {
                              this.featurePathString, builtInFunctionName });
               }
            } else {
               this.featurePathElementNames.add(token);
            }
         }
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#typeInit(org.apache.uima.cas.Type)
    */
   public void typeInit(Type featurePathType) throws CASException {

      // do feature path type initialization only if a featurePath is available
      if (this.featurePathElementNames.size() > 0) {

         LowLevelTypeSystem llTypeSystem = ((TypeImpl) featurePathType)
               .getTypeSystem().getLowLevelTypeSystem();

         // store featurePathType
         this.featurePathBaseType = featurePathType;
         this.featurePathBaseTypeCode = llTypeSystem
               .ll_getCodeForType(featurePathType);

         // validate featurePath for given type
         PathValid pathValid = TypeSystemUtils.isPathValid(featurePathType,
               this.featurePathElementNames);
         if (PathValid.NEVER == pathValid) {
            // invalid featurePath - throw an configuration exception
            throw new CASException(MESSAGE_DIGEST,
                  "ERROR_VALIDATE_FEATURE_PATH", new Object[] {
                        this.featurePathString, featurePathType.getName() });
         } else if (PathValid.ALWAYS == pathValid) {
            // the featurePath is always valid, so we can resolve and cache the
            // path elements
            this.ll_featurePathElements = new ArrayList<Integer>();
            this.featurePathElements = new ArrayList<Feature>(); // reset
            // object
            Type currentType = featurePathType;
            // iterate over all featurePathNames and store the resolved CAS
            // feature in the featurePathElements list
            for (int i = 0; i < this.featurePathElementNames.size(); i++) {
               // get feature
               Feature feature = currentType
                     .getFeatureByBaseName(this.featurePathElementNames.get(i));
               // store feature code
               this.ll_featurePathElements.add(llTypeSystem
                     .ll_getCodeForFeature(feature));
               this.featurePathElements.add(feature);

               // get current feature type to resolve the next feature name
               currentType = feature.getRange();
            }
         }
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#getFeaturePath()
    */
   public String getFeaturePath() {
      return this.featurePathString;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#getBooleanValue(org.apache.uima.cas.FeatureStructure)
    */
   public Boolean getBooleanValue(FeatureStructure fs) {
      if (fs != null) {
         LowLevelCAS llCas = fs.getCAS().getLowLevelCAS();
         FeaturePathValue featurePathValue = getValue(llCas.ll_getFSRef(fs),
               llCas);

         if ((featurePathValue != null)
               && (featurePathValue.getTypeClass() == LowLevelCAS.TYPE_CLASS_BOOLEAN)) {
            return featurePathValue.getBooleanValue();
         }
      }
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#getByteValue(org.apache.uima.cas.FeatureStructure)
    */
   public Byte getByteValue(FeatureStructure fs) {
      if (fs != null) {
         LowLevelCAS llCas = fs.getCAS().getLowLevelCAS();
         FeaturePathValue featurePathValue = getValue(llCas.ll_getFSRef(fs),
               llCas);

         if ((featurePathValue != null)
               && (featurePathValue.getTypeClass() == LowLevelCAS.TYPE_CLASS_BYTE)) {
            return featurePathValue.getByteValue();
         }
      }
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#getDoubleValue(org.apache.uima.cas.FeatureStructure)
    */
   public Double getDoubleValue(FeatureStructure fs) {
      if (fs != null) {
         LowLevelCAS llCas = fs.getCAS().getLowLevelCAS();
         FeaturePathValue featurePathValue = getValue(llCas.ll_getFSRef(fs),
               llCas);

         if ((featurePathValue != null)
               && (featurePathValue.getTypeClass() == LowLevelCAS.TYPE_CLASS_DOUBLE)) {
            return featurePathValue.getDoubleValue();
         }
      }
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#getFloatValue(org.apache.uima.cas.FeatureStructure)
    */
   public Float getFloatValue(FeatureStructure fs) {
      if (fs != null) {
         LowLevelCAS llCas = fs.getCAS().getLowLevelCAS();
         FeaturePathValue featurePathValue = getValue(llCas.ll_getFSRef(fs),
               llCas);

         if ((featurePathValue != null)
               && (featurePathValue.getTypeClass() == LowLevelCAS.TYPE_CLASS_FLOAT)) {
            return featurePathValue.getFloatValue();
         }
      }
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#getFSValue(org.apache.uima.cas.FeatureStructure)
    */
   public FeatureStructure getFSValue(FeatureStructure fs) {
      if (fs != null) {
         LowLevelCAS llCas = fs.getCAS().getLowLevelCAS();
         FeaturePathValue featurePathValue = getValue(llCas.ll_getFSRef(fs),
               llCas);

         if ((featurePathValue != null)
               && ((featurePathValue.getTypeClass() == LowLevelCAS.TYPE_CLASS_FS)
                     || (featurePathValue.getTypeClass() == LowLevelCAS.TYPE_CLASS_BYTEARRAY)
                     || (featurePathValue.getTypeClass() == LowLevelCAS.TYPE_CLASS_DOUBLEARRAY)
                     || (featurePathValue.getTypeClass() == LowLevelCAS.TYPE_CLASS_FLOATARRAY)
                     || (featurePathValue.getTypeClass() == LowLevelCAS.TYPE_CLASS_FSARRAY)
                     || (featurePathValue.getTypeClass() == LowLevelCAS.TYPE_CLASS_INTARRAY)
                     || (featurePathValue.getTypeClass() == LowLevelCAS.TYPE_CLASS_LONGARRAY)
                     || (featurePathValue.getTypeClass() == LowLevelCAS.TYPE_CLASS_SHORTARRAY)
                     || (featurePathValue.getTypeClass() == LowLevelCAS.TYPE_CLASS_STRINGARRAY) || (featurePathValue
                     .getTypeClass() == LowLevelCAS.TYPE_CLASS_BOOLEANARRAY))) {
            return featurePathValue.getFs();
         }
      }
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#getIntValue(org.apache.uima.cas.FeatureStructure)
    */
   public Integer getIntValue(FeatureStructure fs) {
      if (fs != null) {
         LowLevelCAS llCas = fs.getCAS().getLowLevelCAS();
         FeaturePathValue featurePathValue = getValue(llCas.ll_getFSRef(fs),
               llCas);

         if ((featurePathValue != null)
               && (featurePathValue.getTypeClass() == LowLevelCAS.TYPE_CLASS_INT)) {
            return featurePathValue.getIntValue();
         }
      }
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#getLongValue(org.apache.uima.cas.FeatureStructure)
    */
   public Long getLongValue(FeatureStructure fs) {
      if (fs != null) {
         LowLevelCAS llCas = fs.getCAS().getLowLevelCAS();
         FeaturePathValue featurePathValue = getValue(llCas.ll_getFSRef(fs),
               llCas);

         if ((featurePathValue != null)
               && (featurePathValue.getTypeClass() == LowLevelCAS.TYPE_CLASS_LONG)) {
            return featurePathValue.getLongValue();
         }
      }
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#getShortValue(org.apache.uima.cas.FeatureStructure)
    */
   public Short getShortValue(FeatureStructure fs) {
      if (fs != null) {
         LowLevelCAS llCas = fs.getCAS().getLowLevelCAS();
         FeaturePathValue featurePathValue = getValue(llCas.ll_getFSRef(fs),
               llCas);

         if ((featurePathValue != null)
               && (featurePathValue.getTypeClass() == LowLevelCAS.TYPE_CLASS_SHORT)) {
            return featurePathValue.getShortValue();
         }
      }
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#getStringValue(org.apache.uima.cas.FeatureStructure)
    */
   public String getStringValue(FeatureStructure fs) {
      if (fs != null) {
         LowLevelCAS llCas = fs.getCAS().getLowLevelCAS();
         FeaturePathValue featurePathValue = getValue(llCas.ll_getFSRef(fs),
               llCas);

         if ((featurePathValue != null)
               && (featurePathValue.getTypeClass() == LowLevelCAS.TYPE_CLASS_STRING)) {
            return featurePathValue.getStringValue();
         }
      }
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#getType(org.apache.uima.cas.FeatureStructure)
    */
   public Type getType(FeatureStructure fs) {
      if (fs != null) {
         LowLevelCAS llCas = fs.getCAS().getLowLevelCAS();
         FeaturePathValue featurePathValue = getValue(llCas.ll_getFSRef(fs),
               llCas);

         if (featurePathValue != null) {
            return featurePathValue.getFeatureType();
         }
      }
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#getTypClass(org.apache.uima.cas.FeatureStructure)
    */
   public TypeClass getTypClass(FeatureStructure fs) {
      if (fs != null) {
         LowLevelCAS llCas = fs.getCAS().getLowLevelCAS();
         FeaturePathValue featurePathValue = getValue(llCas.ll_getFSRef(fs),
               llCas);

         if (featurePathValue != null) {
            switch (featurePathValue.getTypeClass()) {
            case LowLevelCAS.TYPE_CLASS_STRING:
               return TypeClass.TYPE_CLASS_STRING;
            case LowLevelCAS.TYPE_CLASS_INT:
               return TypeClass.TYPE_CLASS_INT;
            case LowLevelCAS.TYPE_CLASS_BOOLEAN:
               return TypeClass.TYPE_CLASS_BOOLEAN;
            case LowLevelCAS.TYPE_CLASS_BYTE:
               return TypeClass.TYPE_CLASS_BYTE;
            case LowLevelCAS.TYPE_CLASS_DOUBLE:
               return TypeClass.TYPE_CLASS_DOUBLE;
            case LowLevelCAS.TYPE_CLASS_FLOAT:
               return TypeClass.TYPE_CLASS_FLOAT;
            case LowLevelCAS.TYPE_CLASS_LONG:
               return TypeClass.TYPE_CLASS_LONG;
            case LowLevelCAS.TYPE_CLASS_SHORT:
               return TypeClass.TYPE_CLASS_SHORT;
            case LowLevelCAS.TYPE_CLASS_INVALID:
               return TypeClass.TYPE_CLASS_INVALID;
            case LowLevelCAS.TYPE_CLASS_FS:
               return TypeClass.TYPE_CLASS_FS;
            case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY:
               return TypeClass.TYPE_CLASS_BOOLEANARRAY;
            case LowLevelCAS.TYPE_CLASS_BYTEARRAY:
               return TypeClass.TYPE_CLASS_BYTEARRAY;
            case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY:
               return TypeClass.TYPE_CLASS_DOUBLEARRAY;
            case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
               return TypeClass.TYPE_CLASS_FLOATARRAY;
            case LowLevelCAS.TYPE_CLASS_FSARRAY:
               return TypeClass.TYPE_CLASS_FSARRAY;
            case LowLevelCAS.TYPE_CLASS_INTARRAY:
               return TypeClass.TYPE_CLASS_INTARRAY;
            case LowLevelCAS.TYPE_CLASS_LONGARRAY:
               return TypeClass.TYPE_CLASS_LONGARRAY;
            case LowLevelCAS.TYPE_CLASS_SHORTARRAY:
               return TypeClass.TYPE_CLASS_SHORTARRAY;
            case LowLevelCAS.TYPE_CLASS_STRINGARRAY:
               return TypeClass.TYPE_CLASS_STRINGARRAY;
            }
         }
      }
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#getValueAsString(org.apache.uima.cas.FeatureStructure)
    */
   public String getValueAsString(FeatureStructure fs) {
      if (fs != null) {
         LowLevelCAS llCas = fs.getCAS().getLowLevelCAS();
         return ll_getValueAsString(llCas.ll_getFSRef(fs), llCas);
      } else {
         return null;
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.uima.cas.FeaturePath#ll_getValueAsString(int,
    *      org.apache.uima.cas.impl.LowLevelCAS)
    */
   public String ll_getValueAsString(int fsRef, LowLevelCAS llCas) {
      FeaturePathValue featurePathValue = getValue(fsRef, llCas);
      if (featurePathValue != null) {
         switch (featurePathValue.getTypeClass()) {
         case LowLevelCAS.TYPE_CLASS_STRING:
            // check if we have a built-in function
            if (this.builtInFunction > NO_BUILT_IN_FUNCTION) {
               throwBuiltInFunctionException(featurePathValue.getFeatureType()
                     .getName());
            }
            return featurePathValue.getStringValue();
         case LowLevelCAS.TYPE_CLASS_INT:
            // check if we have a built-in function
            if (this.builtInFunction > NO_BUILT_IN_FUNCTION) {
               throwBuiltInFunctionException(featurePathValue.getFeatureType()
                     .getName());
            }
            return Integer.toString(featurePathValue.getIntValue());
         case LowLevelCAS.TYPE_CLASS_BOOLEAN:
            // check if we have a built-in function
            if (this.builtInFunction > NO_BUILT_IN_FUNCTION) {
               throwBuiltInFunctionException(featurePathValue.getFeatureType()
                     .getName());
            }
            return Boolean.toString(featurePathValue.getBooleanValue());
         case LowLevelCAS.TYPE_CLASS_BYTE:
            // check if we have a built-in function
            if (this.builtInFunction > NO_BUILT_IN_FUNCTION) {
               throwBuiltInFunctionException(featurePathValue.getFeatureType()
                     .getName());
            }
            return Byte.toString(featurePathValue.getByteValue());
         case LowLevelCAS.TYPE_CLASS_DOUBLE:
            // check if we have a built-in function
            if (this.builtInFunction > NO_BUILT_IN_FUNCTION) {
               throwBuiltInFunctionException(featurePathValue.getFeatureType()
                     .getName());
            }
            return Double.toString(featurePathValue.getDoubleValue());
         case LowLevelCAS.TYPE_CLASS_FLOAT:
            // check if we have a built-in function
            if (this.builtInFunction > NO_BUILT_IN_FUNCTION) {
               throwBuiltInFunctionException(featurePathValue.getFeatureType()
                     .getName());
            }
            return Float.toString(featurePathValue.getFloatValue());
         case LowLevelCAS.TYPE_CLASS_LONG:
            // check if we have a built-in function
            if (this.builtInFunction > NO_BUILT_IN_FUNCTION) {
               throwBuiltInFunctionException(featurePathValue.getFeatureType()
                     .getName());
            }
            return Long.toString(featurePathValue.getLongValue());
         case LowLevelCAS.TYPE_CLASS_SHORT:
            // check if we have a built-in function
            if (this.builtInFunction > NO_BUILT_IN_FUNCTION) {
               throwBuiltInFunctionException(featurePathValue.getFeatureType()
                     .getName());
            }
            return Short.toString(featurePathValue.getShortValue());
         case LowLevelCAS.TYPE_CLASS_INVALID:
            return null;
         case LowLevelCAS.TYPE_CLASS_FS: {
            FeatureStructure returnFS = featurePathValue.getFs();
            if (returnFS == null) {
               return null;
            } else {
               // check if we have a built-in function
               if (this.builtInFunction > NO_BUILT_IN_FUNCTION) {
                  return evaluateBuiltInFunction(returnFS);
               } else {
                  return returnFS.toString();
               }
            }
         }
         case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY: {
            BooleanArrayFS returnFS = (BooleanArrayFS) featurePathValue.getFs();
            if (returnFS == null) {
               return null;
            } else {
               // check if we have a built-in function
               if (this.builtInFunction > NO_BUILT_IN_FUNCTION) {
                  return evaluateBuiltInFunction(returnFS);
               } else {
                  return convertToString(returnFS.toStringArray());
               }
            }
         }
         case LowLevelCAS.TYPE_CLASS_BYTEARRAY: {
            ByteArrayFS returnFS = (ByteArrayFS) featurePathValue.getFs();
            if (returnFS == null) {
               return null;
            } else {
               // check if we have a built-in function
               if (this.builtInFunction > NO_BUILT_IN_FUNCTION) {
                  return evaluateBuiltInFunction(returnFS);
               } else {
                  return convertToString(returnFS.toStringArray());
               }
            }
         }
         case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY: {
            DoubleArrayFS returnFS = (DoubleArrayFS) featurePathValue.getFs();
            if (returnFS == null) {
               return null;
            } else {
               // check if we have a built-in function
               if (this.builtInFunction > NO_BUILT_IN_FUNCTION) {
                  return evaluateBuiltInFunction(returnFS);
               } else {
                  return convertToString(returnFS.toStringArray());
               }
            }
         }
         case LowLevelCAS.TYPE_CLASS_FLOATARRAY: {
            FloatArrayFS returnFS = (FloatArrayFS) featurePathValue.getFs();
            if (returnFS == null) {
               return null;
            } else {
               return convertToString(returnFS.toStringArray());
            }
         }
         case LowLevelCAS.TYPE_CLASS_FSARRAY: {
            ArrayFS returnFS = (ArrayFS) featurePathValue.getFs();
            if (returnFS == null) {
               return null;
            } else {
               // check if we have a built-in function
               if (this.builtInFunction > NO_BUILT_IN_FUNCTION) {
                  return evaluateBuiltInFunction(returnFS);
               } else {
                  return convertToString(returnFS.toStringArray());
               }
            }
         }
         case LowLevelCAS.TYPE_CLASS_INTARRAY: {
            IntArrayFS returnFS = (IntArrayFS) featurePathValue.getFs();
            if (returnFS == null) {
               return null;
            } else {
               // check if we have a built-in function
               if (this.builtInFunction > NO_BUILT_IN_FUNCTION) {
                  return evaluateBuiltInFunction(returnFS);
               } else {
                  return convertToString(returnFS.toStringArray());
               }
            }
         }
         case LowLevelCAS.TYPE_CLASS_LONGARRAY: {
            LongArrayFS returnFS = (LongArrayFS) featurePathValue.getFs();
            if (returnFS == null) {
               return null;
            } else {
               // check if we have a built-in function
               if (this.builtInFunction > NO_BUILT_IN_FUNCTION) {
                  return evaluateBuiltInFunction(returnFS);
               } else {
                  return convertToString(returnFS.toStringArray());
               }
            }
         }
         case LowLevelCAS.TYPE_CLASS_SHORTARRAY: {
            ShortArrayFS returnFS = (ShortArrayFS) featurePathValue.getFs();
            if (returnFS == null) {
               return null;
            } else {
               // check if we have a built-in function
               if (this.builtInFunction > NO_BUILT_IN_FUNCTION) {
                  return evaluateBuiltInFunction(returnFS);
               } else {
                  return convertToString(returnFS.toStringArray());
               }
            }
         }
         case LowLevelCAS.TYPE_CLASS_STRINGARRAY: {
            StringArrayFS returnFS = (StringArrayFS) featurePathValue.getFs();
            if (returnFS == null) {
               return null;
            } else {
               // check if we have a built-in function
               if (this.builtInFunction > NO_BUILT_IN_FUNCTION) {
                  return evaluateBuiltInFunction(returnFS);
               } else {
                  return convertToString(returnFS.toStringArray());
               }
            }
         }
         }
      }
      return null;
   }

   /**
    * Method that throws the CASRuntimeException for an unsupported built-in
    * function
    * 
    * @param typeName
    *           type name that does not support the built-in function
    */
   private void throwBuiltInFunctionException(String typeName) {
      // get built-in function name
      String functionName = null;
      if (this.builtInFunction == FUNCTION_COVERED_TEXT) {
         functionName = FUNCTION_NAME_COVERED_TEXT;
      } else if (this.builtInFunction == FUNCTION_ID) {
         functionName = FUNCTION_NAME_ID;
      } else if (this.builtInFunction == FUNCTION_TYPE_NAME) {
         functionName = FUNCTION_NAME_TYPE_NAME;
      }
      // throw runtime exception
      throw new CASRuntimeException(MESSAGE_DIGEST,
            "BUILT_IN_FUNCTION_NOT_SUPPORTED", new Object[] { functionName,
                  typeName });
   }

   /**
    * evaluate the built-in function for the returned FeatureStructure
    * 
    * @param returnFS
    *           FeatureStructure that is returned
    * 
    * @return Returns the built-in function value for the given FS.
    */
   private String evaluateBuiltInFunction(FeatureStructure returnFS) {
      if (this.builtInFunction == FUNCTION_COVERED_TEXT) {
         if (returnFS instanceof AnnotationFS) {
            return ((AnnotationFS) returnFS).getCoveredText();
         } else {
            throw new CASRuntimeException(MESSAGE_DIGEST,
                  "BUILT_IN_FUNCTION_NOT_SUPPORTED", new Object[] {
                        FUNCTION_NAME_COVERED_TEXT,
                        returnFS.getType().getName() });
         }
      } else if (this.builtInFunction == FUNCTION_ID) {
         return Integer.toString(returnFS.getCAS().getLowLevelCAS()
               .ll_getFSRef(returnFS));

      } else if (this.builtInFunction == FUNCTION_TYPE_NAME) {
         return returnFS.getType().getName();
      }
      return null;
   }

   /**
    * Converts a string array to a comma separated string.
    * 
    * @param array
    *           array to convert
    * 
    * @return returns comma separated string of the given string array
    */
   private static String convertToString(String[] array) {
      StringBuffer buffer = new StringBuffer();
      for (int i = 0; i < array.length; i++) {
         if (i > 0) {
            buffer.append(',');
         }
         buffer.append(array[i]);
      }
      return buffer.toString();
   }

   /**
    * evaluates the internal feature path for the given FeatureStructure data.
    * It returns the FeaturePathValue object that contains the value of the
    * feature path with some meta data information.
    * 
    * @param fsRef
    *           FeatureStructure to evaluate the feature path
    * 
    * @param llCas
    *           LowLevelCAS for the fsRef
    * 
    * @return Returns a featurePathValue object or null if the feature path
    *         value was not set
    */
   private FeaturePathValue getValue(int fsRef, LowLevelCAS llCas) {

      // featurePathValue
      FeaturePathValue featurePathValue = new FeaturePathValue();

      // handle special case where no featurePath was specified
      // return current FS as value
      if (this.featurePathElementNames.size() == 0) {
         if (fsRef == LowLevelCAS.NULL_FS_REF) {
            return null;
         } else {
            featurePathValue.setFs(fsRef, llCas);
            int typeCode = llCas.ll_getFSRefType(fsRef, true);
            featurePathValue.setTypeClass(llCas.ll_getTypeClass(typeCode));
            featurePathValue.setFeatureType(llCas.ll_getTypeSystem()
                  .ll_getTypeForCode(typeCode));
            return featurePathValue;
         }
      } else {
         // we have a feature path that must be evaluated

         // check if further featurePath elements are possible
         boolean noFurtherElementsPossible = false;

         // set current FS values
         int currentFsRef = fsRef;
         int currentFeatureCode = 0;
         int currentRangeTypeCode = 0;

         // resolve feature path value
         for (int i = 0; i < this.featurePathElementNames.size(); i++) {
            // if we had in the last iteration a primitive feature or a FS that
            // was not set, the feature path is not valid for this annotation.
            if (noFurtherElementsPossible) {
               // check if the currentFS is null and the featurePath is not set
               // for this FeatureStructure.
               if (currentFsRef == LowLevelCAS.NULL_FS_REF) {
                  // featurePath maybe valid but not set
                  return null;
               } else {
                  // we had a primitive feature within the featurePath, so the
                  // featurePath is invalid
                  throw new CASRuntimeException(MESSAGE_DIGEST,
                        "INVALID_FEATURE_PATH", new Object[] {
                              this.featurePathString,
                              this.featurePathElementNames.get(i - 1) });
               }
            }
            boolean isInitSubType = false;

            // check current FS type for FeaturePath base type
            if (this.featurePathBaseTypeCode > 0) {
               isInitSubType = llCas.ll_getTypeSystem()
                     .ll_subsumes(this.featurePathBaseTypeCode,
                           llCas.ll_getFSRefType(fsRef, true));
            }
            // get the Feature for the current featurePath element. If the
            // featurePath is always valid the featurePath Feature elements are
            // cached, otherwise the feature names must be resolved by name
            if (isInitSubType && (this.ll_featurePathElements != null)) {
               // use cached Feature element
               currentFeatureCode = this.ll_featurePathElements.get(i);
            } else {
               // get current Type from feature type code
               int fsRefTypeCode = llCas.ll_getFSRefType(fsRef, true);
               Type currentType = llCas.ll_getTypeSystem().ll_getTypeForCode(
                     fsRefTypeCode);

               // resolve Feature by name
               Feature feature = currentType
                     .getFeatureByBaseName(this.featurePathElementNames.get(i));
               // if feature is null the feature was not defined
               if (feature == null) {
                  throw new CASRuntimeException(MESSAGE_DIGEST,
                        "INVALID_FEATURE_PATH_FEATURE_NOT_DEFINED",
                        new Object[] { this.featurePathString,
                              currentType.getName(),
                              this.featurePathElementNames.get(i) });
               }
               // get feature code
               currentFeatureCode = llCas.ll_getTypeSystem()
                     .ll_getCodeForFeature(feature);

            }

            // switch feature type class
            currentRangeTypeCode = llCas.ll_getTypeSystem().ll_getRangeType(
                  currentFeatureCode);
            switch (llCas.ll_getTypeClass(currentRangeTypeCode)) {
            case LowLevelCAS.TYPE_CLASS_STRING:
               featurePathValue.setStringValue(llCas.ll_getStringValue(
                     currentFsRef, currentFeatureCode, true));
               noFurtherElementsPossible = true;
               break;
            case LowLevelCAS.TYPE_CLASS_INT:
               featurePathValue.setIntValue(llCas.ll_getIntValue(currentFsRef,
                     currentFeatureCode, true));
               noFurtherElementsPossible = true;
               break;
            case LowLevelCAS.TYPE_CLASS_BOOLEAN:
               featurePathValue.setBooleanValue(llCas.ll_getBooleanValue(
                     currentFsRef, currentFeatureCode, true));
               noFurtherElementsPossible = true;
               break;
            case LowLevelCAS.TYPE_CLASS_BYTE:
               featurePathValue.setByteValue(llCas.ll_getByteValue(
                     currentFsRef, currentFeatureCode, true));
               noFurtherElementsPossible = true;
               break;
            case LowLevelCAS.TYPE_CLASS_DOUBLE:
               featurePathValue.setDoubleValue(llCas.ll_getDoubleValue(
                     currentFsRef, currentFeatureCode, true));
               noFurtherElementsPossible = true;
               break;
            case LowLevelCAS.TYPE_CLASS_FLOAT:
               featurePathValue.setFloatValue(llCas.ll_getFloatValue(
                     currentFsRef, currentFeatureCode, true));
               noFurtherElementsPossible = true;
               break;
            case LowLevelCAS.TYPE_CLASS_LONG:
               featurePathValue.setLongValue(llCas.ll_getLongValue(
                     currentFsRef, currentFeatureCode, true));
               noFurtherElementsPossible = true;
               break;
            case LowLevelCAS.TYPE_CLASS_SHORT:
               featurePathValue.setShortValue(llCas.ll_getShortValue(
                     currentFsRef, currentFeatureCode, true));
               noFurtherElementsPossible = true;
               break;
            case LowLevelCAS.TYPE_CLASS_INVALID:
               noFurtherElementsPossible = true;
               break;
            case LowLevelCAS.TYPE_CLASS_FS:
               currentFsRef = llCas.ll_getRefValue(currentFsRef,
                     currentFeatureCode, true);
               if (currentFsRef == LowLevelCAS.NULL_FS_REF) {
                  // FS value not set - feature path cannot return a valid value
                  noFurtherElementsPossible = true;
                  featurePathValue.setFs(LowLevelCAS.NULL_FS_REF, null);
               } else {
                  featurePathValue.setFs(currentFsRef, llCas);
               }
               break;
            case LowLevelCAS.TYPE_CLASS_BOOLEANARRAY:
            case LowLevelCAS.TYPE_CLASS_BYTEARRAY:
            case LowLevelCAS.TYPE_CLASS_DOUBLEARRAY:
            case LowLevelCAS.TYPE_CLASS_FLOATARRAY:
            case LowLevelCAS.TYPE_CLASS_FSARRAY:
            case LowLevelCAS.TYPE_CLASS_INTARRAY:
            case LowLevelCAS.TYPE_CLASS_LONGARRAY:
            case LowLevelCAS.TYPE_CLASS_SHORTARRAY:
            case LowLevelCAS.TYPE_CLASS_STRINGARRAY:
               currentFsRef = llCas.ll_getRefValue(currentFsRef,
                     currentFeatureCode, true);
               featurePathValue.setFs(currentFsRef, llCas);
               noFurtherElementsPossible = true;
               break;
            }
         } // end of loop

         // set feature path value meta data
         featurePathValue.setTypeClass(llCas.ll_getTypeClass(currentRangeTypeCode));
         featurePathValue.setFeatureType(llCas.ll_getTypeSystem()
               .ll_getTypeForCode(currentRangeTypeCode));
         return featurePathValue;
      }
   }
}

/**
 * internal FeaturePathValue class. The class is used to internally handle the
 * featurePath value. The class has members for each CAS type.
 */
class FeaturePathValue {
   private int intValue = 0;

   private boolean booleanValue = false;

   private byte byteValue = 0;

   private double doubleValue = 0.0;

   private float floatValue = 0.0f;

   private long longValue = 0;

   private short shortValue = 0;

   private String stringValue = null;

   private int featureTypeClass = 0;

   private Type featureType = null;

   private int fsRef = LowLevelCAS.NULL_FS_REF;

   private LowLevelCAS llCas = null;

   public int getIntValue() {
      return this.intValue;
   }

   public void setIntValue(int intValue) {
      this.intValue = intValue;
   }

   public boolean getBooleanValue() {
      return this.booleanValue;
   }

   public void setBooleanValue(boolean booleanValue) {
      this.booleanValue = booleanValue;
   }

   public byte getByteValue() {
      return this.byteValue;
   }

   public void setByteValue(byte byteValue) {
      this.byteValue = byteValue;
   }

   public double getDoubleValue() {
      return this.doubleValue;
   }

   public void setDoubleValue(double doubleValue) {
      this.doubleValue = doubleValue;
   }

   public float getFloatValue() {
      return this.floatValue;
   }

   public void setFloatValue(float floatValue) {
      this.floatValue = floatValue;
   }

   public long getLongValue() {
      return this.longValue;
   }

   public void setLongValue(long longValue) {
      this.longValue = longValue;
   }

   public short getShortValue() {
      return this.shortValue;
   }

   public void setShortValue(short shortValue) {
      this.shortValue = shortValue;
   }

   public String getStringValue() {
      return this.stringValue;
   }

   public void setStringValue(String stringValue) {
      this.stringValue = stringValue;
   }

   /**
    * Returns the type class of the feature path value.
    * 
    * @return Returns the type code of the feature path value
    */
   public int getTypeClass() {
      return this.featureTypeClass;
   }

   public void setTypeClass(int typeClass) {
      this.featureTypeClass = typeClass;
   }

   public FeatureStructure getFs() {
      if (this.fsRef != LowLevelCAS.NULL_FS_REF && this.llCas != null) {
         return this.llCas.ll_getFSForRef(this.fsRef);
      } else {
         return null;
      }
   }

   public void setFs(int fsRef, LowLevelCAS llCas) {
      this.fsRef = fsRef;
      this.llCas = llCas;
   }

   /**
    * Returns the type of the feature path value
    * 
    * @return Returns the type of the feature path value
    */
   public Type getFeatureType() {
      return this.featureType;
   }

   public void setFeatureType(Type featureType) {
      this.featureType = featureType;
   }
}
