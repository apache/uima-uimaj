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

import org.apache.uima.fit.descriptor.SofaCapability;

public class SofaCapabilityConverter
        extends
        ContextlessAnnotationConverterBase<org.uimafit.descriptor.SofaCapability, org.apache.uima.fit.descriptor.SofaCapability> {

  public SofaCapabilityConverter() {
    // Nothing to do
  }

  @Override
  public SofaCapability convert(
          final org.uimafit.descriptor.SofaCapability aAnnotation) {
    return new SofaCapabilitySubstitute(aAnnotation);
  }

  public Class<org.uimafit.descriptor.SofaCapability> getLegacyType() {
    return org.uimafit.descriptor.SofaCapability.class;
  }
  
  public Class<SofaCapability> getModernType() {
    return SofaCapability.class;
  }
  
  @SuppressWarnings("serial")
  public class SofaCapabilitySubstitute extends
          AnnotationLiteral<SofaCapability> implements SofaCapability {

    private org.uimafit.descriptor.SofaCapability legacyAnnotation;
    
    public SofaCapabilitySubstitute(org.uimafit.descriptor.SofaCapability aAnnotation) {
      legacyAnnotation = aAnnotation;
    }

    public String[] inputSofas() {
      String[] values = legacyAnnotation.inputSofas();
      if (values.length == 1
              && org.uimafit.descriptor.SofaCapability.NO_DEFAULT_VALUE.equals(values[0])) {
        return new String[] { SofaCapability.NO_DEFAULT_VALUE };
      } else {
        return values;
      }
    }

    public String[] outputSofas() {
      String[] values = legacyAnnotation.outputSofas();
      if (values.length == 1
              && org.uimafit.descriptor.SofaCapability.NO_DEFAULT_VALUE.equals(values[0])) {
        return new String[] { SofaCapability.NO_DEFAULT_VALUE };
      } else {
        return values;
      }
    }
  }
}
