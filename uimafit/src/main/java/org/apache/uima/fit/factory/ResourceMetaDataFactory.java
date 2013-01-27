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
package org.apache.uima.fit.factory;

import org.apache.uima.fit.util.ReflectionUtil;
import org.apache.uima.resource.metadata.ResourceMetaData;

public final class ResourceMetaDataFactory {

  private ResourceMetaDataFactory() {
    // This class is not meant to be instantiated
  }

  /**
   * Adds meta data from a {@link org.apache.uima.fit.descriptor.ResourceMetaData} annotation to the
   * given meta data object if such an annotation is present on the component class. If no
   * annotation is present, default values are be added.
   * 
   * @param aMetaData
   *          the meta data object to configure.
   * @param aComponentClass
   *          the class that may carry the {@link org.apache.uima.fit.descriptor.ResourceMetaData}
   *          annotation
   */
  public static void configureResourceMetaData(ResourceMetaData aMetaData, Class<?> aComponentClass) {
    org.apache.uima.fit.descriptor.ResourceMetaData componentAnno = ReflectionUtil
            .getInheritableAnnotation(org.apache.uima.fit.descriptor.ResourceMetaData.class,
                    aComponentClass);

    if (componentAnno == null) {
      // Default handling if no annotation is present.
      if (aComponentClass.getPackage() != null) {
        aMetaData.setVendor(aComponentClass.getPackage().getName());
      }
      aMetaData.setName(aComponentClass.getName());
      aMetaData.setDescription(Defaults.DEFAULT_DESCRIPTION);
      aMetaData.setVersion(Defaults.DEFAULT_VERSION);
    } else {
      // If annotation is present, use it
      // Annotation values cannot be null, but we want to avoid empty strings in the meta data,
      // thus we set to null when the value is empty.
      aMetaData.setCopyright(emptyAsNull(componentAnno.copyright()));
      aMetaData.setDescription(emptyAsNull(componentAnno.description()));
      aMetaData.setName(emptyAsNull(componentAnno.name()));
      aMetaData.setVendor(emptyAsNull(componentAnno.vendor()));
      aMetaData.setVersion(emptyAsNull(componentAnno.version()));
    }
  }
  
  private static String emptyAsNull(String aString) {
    if (aString == null || aString.length() == 0) {
      return null;
    }
    else {
      return aString;
    }
  }
}
