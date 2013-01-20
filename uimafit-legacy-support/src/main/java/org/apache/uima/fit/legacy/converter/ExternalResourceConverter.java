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

import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.resource.Resource;

public class ExternalResourceConverter
        extends
        ContextlessAnnotationConverterBase<org.uimafit.descriptor.ExternalResource, org.apache.uima.fit.descriptor.ExternalResource> {

  public ExternalResourceConverter() {
    // Nothing to do
  }

  @Override
  public ExternalResource convert(
          final org.uimafit.descriptor.ExternalResource aAnnotation) {
    return new ExternalResourceSubstitute(aAnnotation);
  }

  public Class<org.uimafit.descriptor.ExternalResource> getLegacyType() {
    return org.uimafit.descriptor.ExternalResource.class;
  }
  
  public Class<ExternalResource> getModernType() {
    return ExternalResource.class;
  }
  
  @SuppressWarnings("serial")
  public class ExternalResourceSubstitute extends
          AnnotationLiteral<ExternalResource> implements ExternalResource {

    private org.uimafit.descriptor.ExternalResource legacyAnnotation;
    
    public ExternalResourceSubstitute(org.uimafit.descriptor.ExternalResource aAnnotation) {
      legacyAnnotation = aAnnotation;
    }

    public String key() {
      return legacyAnnotation.key();
    }

    public Class<? extends Resource> api() {
      return legacyAnnotation.api();
    }

    public boolean mandatory() {
      return legacyAnnotation.mandatory();
    }
  }
}
