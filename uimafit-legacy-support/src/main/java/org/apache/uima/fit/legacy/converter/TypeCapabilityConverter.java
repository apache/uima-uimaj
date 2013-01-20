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

import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.legacy.AnnotationConverter;

public class TypeCapabilityConverter
        implements
        AnnotationConverter<org.uimafit.descriptor.TypeCapability, org.apache.uima.fit.descriptor.TypeCapability> {

  public TypeCapabilityConverter() {
    // Nothing to do
  }

  public TypeCapability convert(
          final org.uimafit.descriptor.TypeCapability aAnnotation) {
    return new TypeCapabilitySubstitute(aAnnotation);
  }

  public Class<org.uimafit.descriptor.TypeCapability> getLegacyType() {
    return org.uimafit.descriptor.TypeCapability.class;
  }
  
  public Class<TypeCapability> getModernType() {
    return TypeCapability.class;
  }
  
  @SuppressWarnings("serial")
  public class TypeCapabilitySubstitute extends
          AnnotationLiteral<TypeCapability> implements TypeCapability {

    private org.uimafit.descriptor.TypeCapability legacyAnnotation;
    
    public TypeCapabilitySubstitute(org.uimafit.descriptor.TypeCapability aAnnotation) {
      legacyAnnotation = aAnnotation;
    }

    public String[] inputs() {
      return legacyAnnotation.inputs();
    }

    public String[] outputs() {
      return legacyAnnotation.outputs();
    }
  }
}
