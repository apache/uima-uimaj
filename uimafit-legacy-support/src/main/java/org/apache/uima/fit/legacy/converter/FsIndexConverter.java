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

import org.apache.uima.fit.descriptor.FsIndex;
import org.apache.uima.fit.descriptor.FsIndexKey;
import org.apache.uima.jcas.cas.TOP;

public class FsIndexConverter
        extends
        ContextlessAnnotationConverterBase<org.uimafit.descriptor.FsIndex, org.apache.uima.fit.descriptor.FsIndex> {

  public FsIndexConverter() {
    // Nothing to do
  }

  @Override
  public FsIndex convert(
          final org.uimafit.descriptor.FsIndex aAnnotation) {
    return new FsIndexSubstitute(aAnnotation);
  }

  public Class<org.uimafit.descriptor.FsIndex> getLegacyType() {
    return org.uimafit.descriptor.FsIndex.class;
  }
  
  public Class<FsIndex> getModernType() {
    return FsIndex.class;
  }
  
  @SuppressWarnings("serial")
  public class FsIndexSubstitute extends
          AnnotationLiteral<FsIndex> implements FsIndex {

    private org.uimafit.descriptor.FsIndex legacyAnnotation;

    public FsIndexSubstitute(org.uimafit.descriptor.FsIndex aAnnotation) {
      legacyAnnotation = aAnnotation;
    }

    public String label() {
      return legacyAnnotation.label();
    }

    public String typeName() {
      return legacyAnnotation.typeName();
    }

    public Class<? extends TOP> type() {
      return legacyAnnotation.type();
    }

    public String kind() {
      return legacyAnnotation.kind();
    }

    public FsIndexKey[] keys() {
      FsIndexKey[] result = new FsIndexKey[legacyAnnotation.keys().length];
      FsIndexKeyConverter conv = new FsIndexKeyConverter();
      int i = 0;
      for (org.uimafit.descriptor.FsIndexKey k : legacyAnnotation.keys()) {
        result[i] = conv.convert(k);
        i++;
      }
      return result;
    }

    public boolean typePriorities() {
      return legacyAnnotation.typePriorities();
    }
  }
}
