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

package org.apache.uima.collection.impl.cpm.vinci;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas_data.CasData;
import org.apache.uima.cas_data.FeatureStructure;
import org.apache.uima.cas_data.FeatureValue;
import org.apache.uima.cas_data.PrimitiveValue;
import org.apache.uima.cas_data.impl.CasDataToXCas;
import org.apache.uima.cas_data.impl.FeatureStructureImpl;
import org.apache.uima.collection.impl.cpm.Constants;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.ConfigurableFeature;
import org.apache.uima.collection.impl.cpm.utils.FeatureMap;
import org.apache.uima.collection.impl.cpm.utils.Filter;
import org.apache.uima.collection.impl.cpm.vinci.cas_data.VinciPrimitiveValue;
import org.apache.uima.internal.util.StringUtils;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.apache.uima.util.Level;
import org.apache.uima.util.XMLSerializer;

public class DATACasUtils {
  public static String getXCASasString(CasData aCasData, String[] keysToFilter) throws Exception {
    CasDataToXCas generator = new CasDataToXCas();
    generator.setTypesToFilter(keysToFilter);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    XMLSerializer sax2xml = new XMLSerializer(baos);
    generator.setContentHandler(sax2xml.getContentHandler());

    generator.generateXCas(aCasData);

    return new String(baos.toByteArray());
  }

  /**
   * 
   * @param dataCas
   * @param featureType
   * @param featureName
   * @param featureValue
   */
  public static void addFeatureStructure(CasData dataCas, String featureType, String featureName,
          String featureValue) {
    FeatureStructure vfs = new FeatureStructureImpl();
    vfs.setType(featureType);
    PrimitiveValue pv = new VinciPrimitiveValue(featureValue);
    vfs.setFeatureValue(featureName, pv);
    dataCas.addFeatureStructure(vfs);
  }

  /**
   * 
   * @param aDataCas
   * @return true if the data cas is empty
   */
  public static boolean isCasEmpty(CasData aDataCas) {
    Iterator it = aDataCas.getFeatureStructures();
    if (!it.hasNext()) {
      return true;
    }
    return false;
  }

  /**
   * 
   * @param dataCas
   * @param featureType
   * @param featureName
   * @param featureValue
   */
  public static void addFeature(CasData dataCas, String featureType, String featureName,
          String featureValue) {
    Iterator it = dataCas.getFeatureStructures();
    while (it.hasNext()) {
      FeatureStructure fs = (FeatureStructure) it.next();
      if (fs.getType().equals(featureType)) {
        PrimitiveValue pv = new VinciPrimitiveValue(featureValue);
        fs.setFeatureValue(featureName, pv);
      }
    }
  }

  /**
   * 
   * @param aDataCas
   * @return the byte count
   * @throws Exception -
   */
  public static long getByteCount(CasData aDataCas) throws Exception {
    long byteCount = 0;
    Iterator it = aDataCas.getFeatureStructures();
    while (it.hasNext()) {
      FeatureStructure fs = (FeatureStructure) it.next();
      FeatureValue value = null;
      String[] keys = fs.getFeatureNames();

      for (int i = 0; i < keys.length; i++) {
        value = fs.getFeatureValue(keys[i]);
        if (value == null) {
          continue;
        }
        byteCount += value.toString().length();
      }
    }
    return byteCount;
  }

  /**
   * 
   * @param aCAS
   * @param aFilterList
   * @return true if this cas should be analyzed
   */
  public static boolean shouldAnalyzeCAS(CasData aCAS, LinkedList aFilterList) {

    for (int i = 0; aFilterList != null && i < aFilterList.size(); i++) {
      Filter.Expression filterExpression = (Filter.Expression) aFilterList.get(i);
      String featureValue = getFeatureValueByType(aCAS, filterExpression.getLeftPart().get());
      // This evaluates if the Feature with a given name exist
      if (filterExpression.getRightPart() == null) {
        // The first check is to see if the feature exists in the CAS. In this case,
        // the featureValue must NOT be null.
        // The second check is to see if the the feature does NOT exist in the CAS. In
        // this case, the feature MUST be null.

        if ((filterExpression.getOperand() == null && featureValue == null || featureValue.trim()
                .length() == 0)
                || // this means that the feature must exist in CAS
                ("!".equals(filterExpression.getOperand().getOperand()) && featureValue != null) // this
        // means
        // that
        // the
        // feature
        // must
        // not
        // be
        // present
        // in
        // CAS
        ) {
          return false;
        }
      } else {
        // evaluate if the feature equals specified value
        if ("=".equals(filterExpression.getOperand().getOperand())) {
          if (!filterExpression.getRightPart().get().equals(featureValue)) {
            return false;
          }
        }
        // evaluate if the feature doesnt equal specified value
        else if ("!=".equals(filterExpression.getOperand().getOperand())) {
          if (filterExpression.getRightPart().get().equals(featureValue)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /**
   * 
   * @param aKey
   * @param dropKeyList
   * @return true if this key is in the dropKeyList
   */
  public static boolean dropIt(String aKey, String[] dropKeyList) {
    for (int i = 0; aKey != null && dropKeyList != null && i < dropKeyList.length
            && dropKeyList[i] != null; i++) {
      if (dropKeyList[i].equals(aKey)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 
   * @param aKey
   * @param typeList
   * @return true if tbd 
   */
  public static boolean isValidType(String aKey, String[] typeList) {

    if (aKey.indexOf(org.apache.uima.collection.impl.cpm.Constants.SHORT_DASH_TERM) > -1) {
      aKey = StringUtils.replaceAll(aKey,
              org.apache.uima.collection.impl.cpm.Constants.SHORT_DASH_TERM,
              org.apache.uima.collection.impl.cpm.Constants.LONG_DASH_TERM);
    }
    if (aKey.indexOf(org.apache.uima.collection.impl.cpm.Constants.SHORT_COLON_TERM) > -1) {
      aKey = StringUtils.replaceAll(aKey,
              org.apache.uima.collection.impl.cpm.Constants.SHORT_COLON_TERM,
              org.apache.uima.collection.impl.cpm.Constants.LONG_COLON_TERM);
    }

    for (int i = 0; aKey != null && typeList != null && i < typeList.length && typeList[i] != null; i++) {
      if (typeList[i].equals(aKey)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 
   * @param aCAS
   * @param featureName
   * @return true if
   */
  public static boolean hasFeature(CasData aCAS, String featureName) {
    Iterator it = aCAS.getFeatureStructures();
    while (it.hasNext()) {
      Object object = it.next();
      if (object instanceof FeatureStructure) {
        FeatureStructure fs = (FeatureStructure) object;
        FeatureValue fValue = fs.getFeatureValue(featureName);
        if (fValue != null) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * 
   * @param aCAS
   * @param aName
   * @return true if tbd
   */
  public static boolean hasFeatureStructure(CasData aCAS, String aName) {
    Iterator it = aCAS.getFeatureStructures();
    while (it.hasNext()) {
      Object object = it.next();
      if (object instanceof FeatureStructure) {
        FeatureStructure fs = (FeatureStructure) object;
        String type = StringUtils.replaceAll(fs.getType(), Constants.LONG_COLON_TERM,
                Constants.SHORT_COLON_TERM);
        if (type.equals(aName)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * 
   * @param aCAS
   */
  public static void dumpFeatures(CasData aCAS) {
    Iterator it = aCAS.getFeatureStructures();
    while (it.hasNext()) {
      Object object = it.next();
      if (object instanceof FeatureStructure) {
        FeatureStructure fs = (FeatureStructure) object;
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(DATACasUtils.class).logrb(Level.FINEST,
                  DATACasUtils.class.getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_show_cas_fs_type__FINEST",
                  new Object[] { Thread.currentThread().getName(), fs.getType() });

        }
        String[] names = fs.getFeatureNames();
        for (int i = 0; names != null && i < names.length; i++) {
          FeatureValue fValue = fs.getFeatureValue(names[i]);
          if (fValue != null) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(DATACasUtils.class)
                      .logrb(
                              Level.FINEST,
                              DATACasUtils.class.getName(),
                              "process",
                              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                              "UIMA_CPM_show_cas_fs_value__FINEST",
                              new Object[] { Thread.currentThread().getName(), names[i],
                                  fValue.toString() });
            }
          }
        }
      }
    }

  }

  /**
   * 
   * @param aCAS
   * @param featureName
   * @return true if tbd
   */
  public static String getFeatureValueByType(CasData aCAS, String featureName) {
    Iterator it = aCAS.getFeatureStructures();
    String featureValue = null;
    while (it.hasNext()) {
      Object object = it.next();
      if (object instanceof FeatureStructure) {
        FeatureStructure fs = (FeatureStructure) object;
        if (System.getProperty("SHOWFEATURES") != null) {
          UIMAFramework.getLogger(DATACasUtils.class).logrb(Level.FINEST,
                  DATACasUtils.class.getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_search_cas_by_value__FINEST",
                  new Object[] { Thread.currentThread().getName(), featureName, fs.getType() });
        }
        if (featureName.equals(fs.getType())) {
          String[] names = fs.getFeatureNames();
          for (int i = 0; names != null && i < names.length; i++) {
            if (System.getProperty("SHOWFEATURES") != null) {
              UIMAFramework.getLogger(DATACasUtils.class).logrb(Level.FINEST,
                      DATACasUtils.class.getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_show_type_value__FINEST",
                      new Object[] { Thread.currentThread().getName(), names[i], fs.getType() });
            }

          }

          if ("uima.cpm.DocumentText".equals(featureName) || "UTF8:UTF8Content".equals(featureName)) {
            FeatureValue fValue = fs.getFeatureValue("value");
            if (fValue == null) {
              return null;
            }
            return fValue.toString();
          } else if ("Detag:DetagContent".equals(featureName)) {
            FeatureValue fValue = fs.getFeatureValue("Doc:SpannedText");
            if (fValue == null) {
              return null;
            }
            return fValue.toString();

          }
          FeatureValue fValue = fs.getFeatureValue(featureName);
          if (fValue != null) {
            featureValue = fValue.toString();
            break;
          }
        }
      }
    }
    return featureValue;
  }

  /**
   * 
   * @param aCAS
   * @param featureStructureName
   * @param featureName
   * @return tbd
   */
  public static String[] getFeatureStructureValues(CasData aCAS, String featureStructureName,
          String featureName) {
    Iterator it = aCAS.getFeatureStructures();
    String featureValue = null;
    Vector v = new Vector();
    while (it.hasNext()) {
      Object object = it.next();
      if (object instanceof FeatureStructure) {
        FeatureStructure fs = (FeatureStructure) object;
        if (featureStructureName.equals(fs.getType())) {
          String[] names = fs.getFeatureNames();
          for (int i = 0; names != null && i < names.length; i++) {
            if (names[i].equals(featureName)) {
              FeatureValue fValue = fs.getFeatureValue(featureName);
              if (fValue != null) {
                featureValue = fValue.toString();
                v.add(featureValue);
              }
            }
          }

        }
      }
    }
    String[] features = new String[v.size()];
    v.copyInto(features);
    return features;
  }

  /**
   * 
   * @param aCAS
   * @param aFeatureStructure
   * @param featureName
   * @return tbd
   */
  public static String getFeatureValueByType(CasData aCAS, String aFeatureStructure,
          String featureName) {
    Iterator it = aCAS.getFeatureStructures();
    String featureValue = null;
    while (it.hasNext()) {
      Object object = it.next();

      if (object instanceof FeatureStructureImpl) {
        FeatureStructureImpl fs = (FeatureStructureImpl) object;

        if (fs.getType().equals(aFeatureStructure)) {
          FeatureValue fValue = fs.getFeatureValue(featureName);
          if (fValue != null) {
            featureValue = fValue.toString();
            break;
          }
        }
      }
    }
    return featureValue;
  }

  /**
   * 
   * @param aDataCas
   * @param aFeatureMap
   */
  public static void remapFeatureTypes(CasData aDataCas, FeatureMap aFeatureMap) {
    ConfigurableFeature cf = null;

    Iterator it = aDataCas.getFeatureStructures();
    while (it.hasNext()) {
      Object object = it.next();
      if (object instanceof FeatureStructure) {
        FeatureStructure fs = (FeatureStructure) object;
        if ((cf = aFeatureMap.get(fs.getType())) != null) {
          fs.setType(cf.getNewFeatureName());
          if (cf.attributeListSize() > 0) {
            String[] featureNameList = fs.getFeatureNames();
            for (int i = 0; featureNameList != null && i < featureNameList.length; i++) {
              if (cf.getOldAttributeValue(featureNameList[i]) != null) {
                FeatureValue fv = fs.getFeatureValue(featureNameList[i]);
                // fs.removeFeature(featureNameList[i]);
                fs.setFeatureValue(cf.getNewAttributeValue(featureNameList[i]), fv);
              }
            }
          }
        }
      }
    }
  }

  /**
   * 
   * @param aCasData
   * @param aFeatureStructureName
   * @return tbd
   */
  public static NameValuePair[] getCasDataFeatures(CasData aCasData, String aFeatureStructureName) {
    NameValuePair[] valuePairSet = null;
    Iterator it = aCasData.getFeatureStructures();

    while (it.hasNext()) {
      Object object = it.next();
      if (object instanceof FeatureStructure
              && ((FeatureStructure) object).getType().equals(aFeatureStructureName)) {
        FeatureStructure fs = (FeatureStructure) object;
        String[] featureNames = fs.getFeatureNames();
        if (featureNames == null) {
          // return empty set
          return new NameValuePair[0];
        }
        valuePairSet = new NameValuePair[featureNames.length];
        for (int i = 0; i < featureNames.length; i++) {
          valuePairSet[i] = new NameValuePair_impl();
          valuePairSet[i].setName(featureNames[i]);
          valuePairSet[i].setValue(fs.getFeatureValue(featureNames[i]).toString());
        }
      }
    }
    return valuePairSet;
  }
}
