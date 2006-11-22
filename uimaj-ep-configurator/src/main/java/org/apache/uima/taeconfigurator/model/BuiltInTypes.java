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

package org.apache.uima.taeconfigurator.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.text.TCAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.FeatureDescription_impl;
import org.apache.uima.resource.metadata.impl.TypeDescription_impl;
import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.util.CasCreationUtils;

/**
 * Gets and caches an array list of all the built-in tcas types
 */
public class BuiltInTypes extends AbstractModelPart {

  public static final TypeSystem typeSystem;

  public static final Map typeDescriptions = new TreeMap();

  static {
    TCAS tcas = null;
    try {
      tcas = CasCreationUtils.createTCas((TypeSystemDescription) null, null,
                      new FsIndexDescription[0], casCreateProperties);

    } catch (ResourceInitializationException e1) {
      throw new InternalErrorCDE("invalid ResourceInitializationException", e1);
    }
    ((CASImpl) tcas).commitTypeSystem();
    typeSystem = tcas.getTypeSystem();

    for (Iterator it = typeSystem.getTypeIterator(); it.hasNext();) {
      Type type = (Type) it.next();
      String typeName = type.getName();
      TypeDescription td = new TypeDescription_impl();
      td.setName(typeName);
      Type parent = typeSystem.getParent(type);
      td.setSupertypeName(null == parent ? null : parent.getName());
      List fs = type.getFeatures();
      FeatureDescription[] fds = null;
      if (null != fs) {
        List validFs = new ArrayList();
        for (int i = 0; i < fs.size(); i++) {
          Feature f = (Feature) fs.get(i);
          String fName = f.getName();
          String fTypeName = fName.substring(0, fName.indexOf(':'));
          if (typeName.equals(fTypeName))
            validFs.add(f);
        }
        fds = new FeatureDescription[validFs.size()];
        for (int i = 0; i < fds.length; i++) {
          fds[i] = new FeatureDescription_impl();
          Feature f = (Feature) validFs.get(i);
          fds[i].setName(f.getShortName());
          fds[i].setRangeTypeName(f.getRange().getName());
        }
      }
      td.setFeatures(fds);
      typeDescriptions.put(typeName, td);
    }
  }

  BuiltInTypes() {
    super(null);
  }
}
