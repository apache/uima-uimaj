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
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;

/**
 * Internal class that holds "meta" information about a CAS
 * This object is shared by all CASes that have the same typeSystemImpl.
 * 
 * It is accessible to classes in the cas.impl package, only.
 */

class CASMetadata {
  
  final TypeSystemImpl ts;
  /**
   * Holds generators to create Java objects, either JCas ones or plain ones
   * No longer initialized from the XYZ_Type classes' generators, but initialized with 
   * standard one from initFSClassRegistry(), and
   * JCas ones from instantiateJCas_Types
   */
  final FSClassRegistry fsClassRegistry;
  

  // ///////////////////////////////////////////////////////
  // Data structures for type checking and feature encoding

  // For each feature, what the offset from the start of the FS is.
  // That is, this will always be a number > 0. If you have the
  // address a of a structure of type t, then you can get the value of
  // feature f by getting (the value of) a+featureOffset[f] from the
  // heap. If f is not appropriate for t, anything can happen
  // (including an ArrayIndexOutOfBoundsException).
  int[] featureOffset;

  // For each type, how large structures of that type are. This will
  // also be > 0 for each type (since you need to store the type at a
  // minimum.
  int[] fsSpaceReq;

  // For each type, remember if it's a regular type that can be created
  // with CAS.createFS() or not. Exceptions are built-in types float, int and
  // string, as well as arrays.
  boolean[] creatableType;

  // ///////////////////////////////////////////////////////
  // Properties of types.

  // Those types can not be created with CAS.createFS().
  final private static String[] nonCreatableTypes = { CAS.TYPE_NAME_INTEGER, CAS.TYPE_NAME_FLOAT,
      CAS.TYPE_NAME_STRING, CAS.TYPE_NAME_ARRAY_BASE, CAS.TYPE_NAME_FS_ARRAY,
      CAS.TYPE_NAME_INTEGER_ARRAY, CAS.TYPE_NAME_FLOAT_ARRAY, CAS.TYPE_NAME_STRING_ARRAY,
      CAS.TYPE_NAME_SOFA, CAS.TYPE_NAME_BYTE, CAS.TYPE_NAME_BYTE_ARRAY, CAS.TYPE_NAME_BOOLEAN,
      CAS.TYPE_NAME_BOOLEAN_ARRAY, CAS.TYPE_NAME_SHORT, CAS.TYPE_NAME_SHORT_ARRAY,
      CAS.TYPE_NAME_LONG, CAS.TYPE_NAME_LONG_ARRAY, CAS.TYPE_NAME_DOUBLE,
      CAS.TYPE_NAME_DOUBLE_ARRAY };
 
  
  
  CASMetadata(TypeSystemImpl ts, FSClassRegistry fsClassRegistry) {
    this.ts = ts;
    this.fsClassRegistry = fsClassRegistry;
  }
  
  CASMetadata(TypeSystemImpl ts) {
    this.ts = ts;
    this.fsClassRegistry = new FSClassRegistry(ts);
  }


  // called when type system is "committed"
  //   - all types are known
  //   - no new types will be added
  //       -- exception: array types
  void setupFeaturesAndCreatableTypes() {
    // Compute feature offsets.
    computeFeatureOffsets();
    // Compute FS space requirements.
    final int numTypes = ts.getNumberOfTypes();
    this.fsSpaceReq = new int[numTypes + 1];
    for (int i = 1; i <= numTypes; i++) {
      this.fsSpaceReq[i] = ts.ll_getAppropriateFeatures(i).length + 1;
    }
    // Initialize the non-creatable types info.
    initCreatableTypeTable();
  }

  // Compute the feature offsets
  private final void computeFeatureOffsets() {
    final int numFeats = ts.getNumberOfFeatures();
    this.featureOffset = new int[numFeats + 1];
    Type startType = ts.getTopType();
    // Recursively compute the offsets, starting at the top. Initial offset
    // is 0.
    computeFeatureOffsets(startType, 0);
  }

  // Compute the offsets for features of a type. The offset parameter
  // specifies
  // how many offset values have already been used.
  private final void computeFeatureOffsets(Type t, int offset) {
    // Find all features for which the input type is the domain type.
    List<Feature> allFeats = t.getFeatures();
    ArrayList<Feature> introFeats = new ArrayList<Feature>();
    final int numAllFeats = allFeats.size();
    Feature feat;
    for (int i = 0; i < numAllFeats; i++) {
      feat = allFeats.get(i);
      if (feat.getDomain() == t) {
        introFeats.add(feat);
      }
    }
    // For each feature for which the input type is the domain, assign an
    // offset
    // arbitrarily, starting with the input offset + 1.
    int featCode;
    final int numFeats = introFeats.size();
    for (int i = 0; i < numFeats; i++) {
      featCode = ((FeatureImpl) introFeats.get(i)).getCode();
      this.featureOffset[featCode] = offset + 1 + i;
    }
    // Call routine recursively for all subtypes. Increment input offset by
    // number of features introduced on this type.
    Vector<Type> immediateSubtypes = ts.getDirectlySubsumedTypes(t);
    final int numTypes = immediateSubtypes.size();
    for (int i = 0; i < numTypes; i++) {
      computeFeatureOffsets(immediateSubtypes.get(i), offset + numFeats);
    }
  }

  private void initCreatableTypeTable() {
    this.creatableType = new boolean[ts.getTypeArraySize()];
    Arrays.fill(this.creatableType, true);
    int typeCode;
    for (int i = 0; i < nonCreatableTypes.length; i++) {
      typeCode = ((TypeImpl) ts.getType(nonCreatableTypes[i])).getCode();
      for (int subType = ts.getSmallestType(); subType < this.creatableType.length; subType++) {
        if (ts.subsumes(typeCode, subType)) {
          this.creatableType[subType] = false;
        }
      }
    }
  }

}
