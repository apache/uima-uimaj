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

package org.apache.uima.util;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

public class ConsumerCasUtils {
  public static int getIntFeatValue(CAS aCasView, String aTypeS, String aFeatS) {
    int result = 0;
    Type type = aCasView.getTypeSystem().getType(aTypeS);
    if (type != null) {
      FSIterator<AnnotationFS> idIter = aCasView.getAnnotationIndex(type).iterator();
      while (idIter.isValid()) {
        org.apache.uima.cas.FeatureStructure idFS = idIter.get();
        result = idFS
                .getIntValue(aCasView.getTypeSystem().getFeatureByFullName(aTypeS + ":" + aFeatS));
        idIter.moveToNext();
      }
    }
    return result;
  }

  public static String getStringFeatValue(CAS aCasView, String aTypeS, String aFeatS) {
    String result = null;
    Type type = aCasView.getTypeSystem().getType(aTypeS);
    if (type != null) {
      FSIterator<AnnotationFS> idIter = aCasView.getAnnotationIndex(type).iterator();
      while (idIter.isValid()) {
        org.apache.uima.cas.FeatureStructure idFS = idIter.get();
        result = idFS.getStringValue(aCasView.getTypeSystem().getFeatureByFullName(
                aTypeS + ":" + aFeatS));
        idIter.moveToNext();
      }
    }
    return result;
  }

  public static FeatureStructure getTcasFS(CAS aCasView, String aTypeS) {
    org.apache.uima.cas.FeatureStructure idFS = null;
    Type type = aCasView.getTypeSystem().getType(aTypeS);
    if (type != null) {
      FSIterator<AnnotationFS> idIter = aCasView.getAnnotationIndex(type).iterator();
      while (idIter.isValid()) {
        idFS = idIter.get();
        idIter.moveToNext();
      }
    }
    return idFS;
  }

}
