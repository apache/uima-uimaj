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
//@formatter:off
/* Apache UIMA v3 - First created by JCasGen Sun Nov 08 21:56:09 CET 2020 */

package org.apache.uima.cas.test;


import org.apache.uima.UimaSerializableFSs;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureStructureImplC;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.TOP;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Holds the FSArray of the feature type that will be changed
 * Updated by JCasGen Sun Nov 08 21:56:09 CET 2020
 * XML source: ExampleCas/CustomSerializable.xml
 *
 * @generated
 */
public class FeatureMap extends TOP implements UimaSerializableFSs {

    /**
     * @generated
     * @ordered
     */
    @SuppressWarnings("hiding")
    public final static String _TypeName = "org.apache.uima.cas.test.FeatureMap";

    /**
     * @generated
     * @ordered
     */
    @SuppressWarnings("hiding")
    public final static int typeIndexID = JCasRegistry.register(FeatureMap.class);
    /**
     * @generated
     * @ordered
     */
    @SuppressWarnings("hiding")
    public final static int type = typeIndexID;

    /**
     * @return index of the type
     * @generated
     */
    @Override
    public int getTypeIndexID() {
        return typeIndexID;
    }


    /* *******************
     *   Feature Offsets *
     * *******************/

    public final static String _FeatName_features = "features";


    /* Feature Adjusted Offsets */
    private final static CallSite _FC_features = TypeSystemImpl.createCallSite(FeatureMap.class, "features");
    private final static MethodHandle _FH_features = _FC_features.dynamicInvoker();

    private HashMap<String, FeatureRecord> _features = new HashMap<>();

    /**
     * Never called.  Disable default constructor
     *
     * @generated
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    protected FeatureMap() {/* intentionally empty block */}

    /**
     * Internal - constructor used by generator
     *
     * @param casImpl the CAS this Feature Structure belongs to
     * @param type    the type of this Feature Structure
     * @generated
     */
    public FeatureMap(TypeImpl type, CASImpl casImpl) {
        super(type, casImpl);
        readObject();
    }

    /**
     * @param jcas JCas to which this Feature Structure belongs
     * @generated
     */
    public FeatureMap(JCas jcas) {
        super(jcas);
        readObject();
    }


    /**
     * <!-- begin-user-doc -->
     * Write your own initialization here
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    private void readObject() {/*default - does nothing empty block */}


    //*--------------*
    //* Feature: features

    /**
     * getter for features - gets This array contains possibly several features associated with the container.
     *
     * @return value of the feature
     * @generated
     */
    @SuppressWarnings("unchecked")
    public FSArray<FeatureRecord> getFeatures() {
        return (FSArray<FeatureRecord>) (_getFeatureValueNc(wrapGetIntCatchException(_FH_features)));
    }

    /**
     * setter for features - sets This array contains possibly several features associated with the container.
     *
     * @param v value to set into the feature
     * @generated
     */
    public void setFeatures(FSArray<FeatureRecord> v) {
        _setFeatureValueNcWj(wrapGetIntCatchException(_FH_features), v);
    }


    /**
     * indexed getter for features - gets an indexed value - This array contains possibly several features associated with the container.
     *
     * @param i index in the array to get
     * @return value of the element at index i
     * @generated
     */
    @SuppressWarnings("unchecked")
    public FeatureRecord getFeatures(int i) {
        return (FeatureRecord) (((FSArray<FeatureRecord>) (_getFeatureValueNc(wrapGetIntCatchException(_FH_features)))).get(i));
    }

    /**
     * indexed setter for features - sets an indexed value - This array contains possibly several features associated with the container.
     *
     * @param i index in the array to set
     * @param v value to set into the array
     * @generated
     */
    @SuppressWarnings("unchecked")
    public void setFeatures(int i, FeatureRecord v) {
        ((FSArray<FeatureRecord>) (_getFeatureValueNc(wrapGetIntCatchException(_FH_features)))).set(i, v);
    }

  public Map<String, FeatureRecord> asMap() {
    return Collections.unmodifiableMap(_features);
  }

  public Map<String, Double> asKeyValueMap() {
    return Collections.unmodifiableMap(_features.values().stream()
            .collect(Collectors.toMap(FeatureRecord::getName, FeatureRecord::getValue))
    );
  }

  public FeatureRecord put(String key, double value) {
    return _features.compute(key, (k, v) -> {
      if(v == null) v = new FeatureRecord(getJCas());
      v.setName(k);
      v.setValue(value);
      return v;
    });
  }

  public FeatureRecord put(FeatureRecord record) {
    record = _features.put(record.getName(), record);
    if(record != null) record.removeFromIndexes();
    return record;
  }

  public FeatureRecord get(String key) {
    return _features.get(key);
  }

  public Double getValue(String key) {
    FeatureRecord record = _features.get(key);
    return record != null ? record.getValue() : null;
  }

  public FeatureRecord remove(String key) {
    FeatureRecord record = _features.remove(key);
    if(record != null) record.removeFromIndexes();
    return record;
  }

  @Override
  public void _save_to_cas_data() {
    FSArray<FeatureRecord> records = null;
    int featureCount = _features.size();
    if(featureCount > 0) {
      records = new FSArray(this.getJCas(), featureCount);
      int i = 0;
      for (FeatureRecord record : _features.values()) {
        records.set(i++, record);
      }
    }
    setFeatures(records);
  }

  @Override
  public void _init_from_cas_data() {
    FSArray<FeatureRecord> records = getFeatures();
    if(records != null) {
      for (FeatureRecord record : records) {
        _features.put(record.getName(), record);
      }
    }
  }

  @Override
  public FeatureStructureImplC _superClone() {
    return clone();
  }

}
