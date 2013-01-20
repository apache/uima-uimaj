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

import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.legacy.AnnotationConverter;

public class OperationalPropertiesConverter
        implements
        AnnotationConverter<org.uimafit.descriptor.OperationalProperties, org.apache.uima.fit.descriptor.OperationalProperties> {

  public OperationalPropertiesConverter() {
    // Nothing to do
  }

  public OperationalProperties convert(
          final org.uimafit.descriptor.OperationalProperties aAnnotation) {
    return new OperationalPropertiesSubstitute(aAnnotation);
  }

  public Class<org.uimafit.descriptor.OperationalProperties> getLegacyType() {
    return org.uimafit.descriptor.OperationalProperties.class;
  }
  
  public Class<OperationalProperties> getModernType() {
    return OperationalProperties.class;
  }
  
  @SuppressWarnings("serial")
  public class OperationalPropertiesSubstitute extends
          AnnotationLiteral<OperationalProperties> implements OperationalProperties {

    private org.uimafit.descriptor.OperationalProperties legacyAnnotation;
    
    public OperationalPropertiesSubstitute(org.uimafit.descriptor.OperationalProperties aAnnotation) {
      legacyAnnotation = aAnnotation;
    }

    public boolean multipleDeploymentAllowed() {
      return legacyAnnotation.multipleDeploymentAllowed();
    }

    public boolean modifiesCas() {
      return legacyAnnotation.modifiesCas();
    }

    public boolean outputsNewCases() {
      return legacyAnnotation.outputsNewCases();
    }
  }
}
