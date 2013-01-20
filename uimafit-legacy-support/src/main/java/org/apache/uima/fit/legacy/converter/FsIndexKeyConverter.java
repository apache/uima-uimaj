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
package org.apache.uima.fit.legacy.converter;

import org.apache.uima.fit.descriptor.FsIndexKey;

public class FsIndexKeyConverter
        extends
        ContextlessAnnotationConverterBase<org.uimafit.descriptor.FsIndexKey, org.apache.uima.fit.descriptor.FsIndexKey> {

  public FsIndexKeyConverter() {
    // Nothing to do
  }

  @Override
  public FsIndexKey convert(
          final org.uimafit.descriptor.FsIndexKey aAnnotation) {
    return new FsIndexKeySubstitute(aAnnotation);
  }

  public Class<org.uimafit.descriptor.FsIndexKey> getLegacyType() {
    return org.uimafit.descriptor.FsIndexKey.class;
  }
  
  public Class<FsIndexKey> getModernType() {
    return FsIndexKey.class;
  }
  
  @SuppressWarnings("serial")
  public class FsIndexKeySubstitute extends
          AnnotationLiteral<FsIndexKey> implements FsIndexKey {

    private org.uimafit.descriptor.FsIndexKey legacyAnnotation;
    
    public FsIndexKeySubstitute(org.uimafit.descriptor.FsIndexKey aAnnotation) {
      legacyAnnotation = aAnnotation;
    }

    public String featureName() {
      return legacyAnnotation.featureName();
    }

    public int comparator() {
      return legacyAnnotation.comparator();
    }
  }
}
