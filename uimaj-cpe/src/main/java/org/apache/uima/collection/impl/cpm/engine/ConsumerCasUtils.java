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

package org.apache.uima.collection.impl.cpm.engine;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;

/**
 * 
 * 
 * 
 * 
 */
public class ConsumerCasUtils {
  /**
   * Returns an int value of a given Feature Structure
   * 
   * @param aCasView -
   *          CAS instance to retrieve data from
   * @param aTypeS -
   *          Feature Type
   * @param aFeatS -
   *          Feature Structure
   * 
   * @return - feature value as int
   */
  public static int getIntFeatValue(CAS aCasView, String aTypeS, String aFeatS) {
    int result = 0;
    FSIterator idIter = aCasView.getAnnotationIndex(aCasView.getTypeSystem().getType(aTypeS)).iterator();
    while (idIter != null && idIter.isValid()) {
      org.apache.uima.cas.FeatureStructure idFS = idIter.get();
      result = idFS.getIntValue(aCasView.getTypeSystem().getFeatureByFullName(aTypeS + ":" + aFeatS));
      idIter.moveToNext();
    }
    return result;
  }

  /**
   * Returns a string value of a given Feature Structure
   * 
   * @param aCasView -
   *          CAS view to retrieve data from
   * @param aTypeS -
   *          Feature Type
   * @param aFeatS -
   *          Feature Structure
   * 
   * @return feature value as string
   */
  public static String getStringFeatValue(CAS aCasView, String aTypeS, String aFeatS) {
    String result = null;
    FSIterator idIter = aCasView.getAnnotationIndex(aCasView.getTypeSystem().getType(aTypeS))
            .iterator();
    while (idIter != null && idIter.isValid()) {
      org.apache.uima.cas.FeatureStructure idFS = idIter.get();
      result = idFS.getStringValue(aCasView.getTypeSystem().getFeatureByFullName(
              aTypeS + ":" + aFeatS));
      idIter.moveToNext();
    }
    return result;
  }

  /**
   * Returns a Feature Structure of a given type
   * 
   * @param aCasView -
   *          CAS instance to retrieve data from
   * @param aTypeS -
   *          Feature Type
   * 
   * @return the first Feature Structure of a given type
   */
  public static FeatureStructure getTcasFS(CAS aCasView, String aTypeS) {
    org.apache.uima.cas.FeatureStructure idFS = null;
    FSIterator idIter = aCasView.getAnnotationIndex(aCasView.getTypeSystem().getType(aTypeS)).iterator();
    while (idIter != null && idIter.isValid()) {
      idFS = idIter.get();
      idIter.moveToNext();
    }
    return idFS;
  }

}
