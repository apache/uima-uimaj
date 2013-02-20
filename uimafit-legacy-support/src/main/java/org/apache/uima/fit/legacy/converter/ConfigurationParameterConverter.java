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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;

import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.legacy.AnnotationConverter;

public class ConfigurationParameterConverter
        implements
        AnnotationConverter<org.uimafit.descriptor.ConfigurationParameter, org.apache.uima.fit.descriptor.ConfigurationParameter> {

  public ConfigurationParameterConverter() {
    // Nothing to do
  }

  public ConfigurationParameter convert(AccessibleObject aContext,
          org.uimafit.descriptor.ConfigurationParameter aAnnotation) {
    return new ConfigurationParameterSubstitute(aAnnotation, (Field) aContext);
  } 

  public ConfigurationParameter convert(Class<?> aContext,
          org.uimafit.descriptor.ConfigurationParameter aAnnotation) {
    throw new UnsupportedOperationException("Annotation is not permitted on classes");
  }

  public Class<org.uimafit.descriptor.ConfigurationParameter> getLegacyType() {
    return org.uimafit.descriptor.ConfigurationParameter.class;
  }
  
  public Class<ConfigurationParameter> getModernType() {
    return ConfigurationParameter.class;
  }
  
  @SuppressWarnings("serial")
  public class ConfigurationParameterSubstitute extends
          AnnotationLiteral<ConfigurationParameter> implements ConfigurationParameter {

    private org.uimafit.descriptor.ConfigurationParameter legacyAnnotation;
    
    private Field field;
    
    public ConfigurationParameterSubstitute(
            org.uimafit.descriptor.ConfigurationParameter aAnnotation, Field aField) {
      legacyAnnotation = aAnnotation;
      field = aField;
    }
    
    /**
     * Legacy uimaFIT used the class name + field name as default value.
     */
    public String name() {
      if (org.uimafit.descriptor.ConfigurationParameter.USE_FIELD_NAME.equals(legacyAnnotation
              .name())) {
        return field.getDeclaringClass().getName() + "." + field.getName();

      } else {
        return legacyAnnotation.name();
      }
    }

    public String description() {
      return legacyAnnotation.description();
    }

    public boolean mandatory() {
      return legacyAnnotation.mandatory();
    }

    public String[] defaultValue() {
      String[] values = legacyAnnotation.defaultValue();
      if (values.length == 1
              && org.uimafit.descriptor.ConfigurationParameter.NO_DEFAULT_VALUE.equals(values[0])) {
        return new String[] { ConfigurationParameter.NO_DEFAULT_VALUE };
      } else {
        return values;
      }
    }
  }
}
