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

package org.apache.uima.cas_data.impl;

import java.util.Iterator;
import java.util.Vector;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas_data.CasData;
import org.apache.uima.cas_data.FeatureStructure;
import org.apache.uima.cas_data.FeatureValue;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.apache.uima.util.Level;

/**
 * Some utilities for dealing with CasData
 */
public class CasDataUtils {
  
  /**
   * resource bundle for log messages
   */
  private static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";

  /**
   * current class
   */
  private static final Class<CasDataUtils> CLASS_NAME = CasDataUtils.class;

  public static boolean hasFeature(CasData aCAS, String featureName) {
    Iterator<FeatureStructure> it = aCAS.getFeatureStructures();
    while (it.hasNext()) {
        FeatureStructure fs = it.next();
        FeatureValue fValue = fs.getFeatureValue(featureName);
        if (fValue != null) {
          return true;
        }
    }
    return false;
  }

  public static boolean hasFeatureStructure(CasData aCAS, String aName) {
    Iterator<FeatureStructure> it = aCAS.getFeatureStructures();
    while (it.hasNext()) {
        FeatureStructure fs = it.next();
        if (fs.getType().equals(aName)) {
          return true;
        }
    }
    return false;
  }

  public static void dumpFeatures(CasData aCAS) {
    Iterator<FeatureStructure> it = aCAS.getFeatureStructures();
    while (it.hasNext()) {
        FeatureStructure fs = it.next();
        UIMAFramework.getLogger(CLASS_NAME).logrb(Level.FINE, CLASS_NAME.getName(), "dumpFeatures",
                LOG_RESOURCE_BUNDLE, "UIMA_cas_feature_structure_type__FINE", fs.getType());

        String[] names = fs.getFeatureNames();
        for (int i = 0; names != null && i < names.length; i++) {
          FeatureValue fValue = fs.getFeatureValue(names[i]);
          if (fValue != null) {
            UIMAFramework.getLogger(CLASS_NAME).logrb(Level.FINE, CLASS_NAME.getName(),
                    "dumpFeatures", LOG_RESOURCE_BUNDLE, "UIMA_cas_feature_name__FINE",
                    new Object[] { names[i], fValue.toString() });
          }
        }
    }

  }

  public static String getFeatureValueByType(CasData aCAS, String featureName) {
    Iterator<FeatureStructure> it = aCAS.getFeatureStructures();
    String featureValue = null;
    while (it.hasNext()) {
        FeatureStructure fs = it.next();
        if (System.getProperty("DEBUG") != null)
          System.out.println("FeatureName::::::::::::::::::::::::::::::::::::::::::>"
                  + fs.getType() + " Searching For::" + featureName);
        if (featureName.equals(fs.getType())) {
          String[] names = fs.getFeatureNames();
          for (int i = 0; names != null && i < names.length; i++) {
            if (System.getProperty("DEBUG") != null)
              System.out.println("Feature Structure:::" + fs.getType() + " Has Value::" + names[i]);
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
    return featureValue;
  }

  public static String[] getFeatureStructureValues(CasData aCAS, String featureStructureName,
          String featureName) {
    Iterator<FeatureStructure> it = aCAS.getFeatureStructures();
    String featureValue = null;
    Vector<String> v = new Vector<String>();
    while (it.hasNext()) {
        FeatureStructure fs = it.next();
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
    String[] features = new String[v.size()];
    v.copyInto(features);
    return features;
  }

  public static String getFeatureValueByType(CasData aCAS, String aFeatureStructure,
          String featureName) {
    Iterator<FeatureStructure> it = aCAS.getFeatureStructures();
    String featureValue = null;
    while (it.hasNext()) {
      FeatureStructure fs = it.next();
      if (fs.getType().equals(aFeatureStructure)) {
        FeatureValue fValue = fs.getFeatureValue(featureName);
        if (fValue != null) {
          featureValue = fValue.toString();
          break;
        }
      }
    }
    return featureValue;
  }

  public static NameValuePair[] getCasDataFeatures(CasData aCasData, String aFeatureStructureName) {
    NameValuePair[] valuePairSet = null;
    Iterator<FeatureStructure> it = aCasData.getFeatureStructures();

    while (it.hasNext()) {
      FeatureStructure fs = it.next();
      if (fs.getType().equals(aFeatureStructureName)) {
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
          // System.out.println("DATACasUtils.getCasDataFeatures()-Name::"+valuePairSet[i].getName()+"
          // Value:::"+valuePairSet[i].getValue().toString());
        }
      }
    }
    return valuePairSet;
  }

}
