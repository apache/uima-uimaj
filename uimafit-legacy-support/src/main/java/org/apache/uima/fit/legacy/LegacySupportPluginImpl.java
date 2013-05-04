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
package org.apache.uima.fit.legacy;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.fit.internal.LegacySupportPlugin;
import org.apache.uima.fit.internal.MetaDataType;
import org.apache.uima.fit.legacy.converter.ConfigurationParameterConverter;
import org.apache.uima.fit.legacy.converter.ExternalResourceConverter;
import org.apache.uima.fit.legacy.converter.FsIndexCollectionConverter;
import org.apache.uima.fit.legacy.converter.FsIndexConverter;
import org.apache.uima.fit.legacy.converter.FsIndexKeyConverter;
import org.apache.uima.fit.legacy.converter.NoConversionConverter;
import org.apache.uima.fit.legacy.converter.OperationalPropertiesConverter;
import org.apache.uima.fit.legacy.converter.SofaCapabilityConverter;
import org.apache.uima.fit.legacy.converter.TypeCapabilityConverter;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.factory.FsIndexFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;

/**
 * Legacy support plug in for the Google Code version of uimaFIT.
 */
public class LegacySupportPluginImpl implements LegacySupportPlugin {
  private Map<Class<? extends Annotation>, AnnotationConverter<?,?>> converterRegistry;
  
  public LegacySupportPluginImpl() {
    register(new ConfigurationParameterConverter());
    register(new ExternalResourceConverter());
    register(new FsIndexConverter());
    register(new FsIndexCollectionConverter());
    register(new FsIndexKeyConverter());
    register(new OperationalPropertiesConverter());
    register(new SofaCapabilityConverter());
    register(new TypeCapabilityConverter());
  }

  public boolean isAnnotationPresent(AccessibleObject aObject,
          Class<? extends Annotation> aAnnotationClass) {
    Class<? extends Annotation> legacyType = getLegacyType(aAnnotationClass);
    if (legacyType != null) {
      return aObject.isAnnotationPresent(legacyType);
    }
    else {
      return false;
    }
  }

  public boolean isAnnotationPresent(Class<?> aObject,
          Class<? extends Annotation> aAnnotationClass) {
    Class<? extends Annotation> legacyType = getLegacyType(aAnnotationClass);
    if (legacyType != null) {
      return aObject.isAnnotationPresent(legacyType);
    }
    else {
      return false;
    }
  }

  public <L extends Annotation, M extends Annotation> M getAnnotation(AccessibleObject aObject,
          Class<M> aAnnotationClass) {
    // Get converter
    AnnotationConverter<L, M> converter = getConverter(aAnnotationClass);
    // Find legacy annotation
    L legacyAnnotation = aObject.getAnnotation(converter.getLegacyType());
    if (legacyAnnotation != null) {
      // If legacy annotation is present, convert it to a modern annotation
      return converter.convert(aObject, legacyAnnotation);
    } else {
      return null;
    }
  }

  public <L extends Annotation, M extends Annotation> M getAnnotation(Class<?> aObject,
          Class<M> aAnnotationClass) {
    // Get converter
    AnnotationConverter<L, M> converter = getConverter(aAnnotationClass);
    // Find legacy annotation
    L legacyAnnotation = aObject.getAnnotation(converter.getLegacyType());
    if (legacyAnnotation != null) {
      // If legacy annotation is present, convert it to a modern annotation
      return converter.convert(aObject, legacyAnnotation);
    } else {
      return null;
    }
  }

  /**
   * Get a converter for the given modern type.
   * 
   * @param aModernType a modern annotation type.
   * @return a converter. This method never returns {@code null}.
   */
  @SuppressWarnings("unchecked")
  private <L extends Annotation, M extends Annotation> AnnotationConverter<L, M> getConverter(Class<M> aModernType)
  {
    AnnotationConverter<?,?> conv = converterRegistry.get(aModernType);
    if (conv == null) {
      conv = NoConversionConverter.getInstance();
    }
    return (AnnotationConverter<L, M>) conv;
  }

  private <L extends Annotation, M extends Annotation> Class<L> getLegacyType(Class<M> aModernType)
  {
    AnnotationConverter<L, M> converter = getConverter(aModernType);
    if (converter != null) {
      return converter.getLegacyType();
    }
    else {
      return null;
    }
  }

  /**
   * Register a new converter.
   */
  private void register(AnnotationConverter<?,?> aConverter)
  {
    if (converterRegistry == null) {
      converterRegistry = new HashMap<Class<? extends Annotation>, AnnotationConverter<?,?>>();
    }
    converterRegistry.put(aConverter.getModernType(), aConverter);
  }

  public String[] scanTypeDescriptors(MetaDataType aType) throws ResourceInitializationException {
    switch (aType) {
      case FS_INDEX:
        return FsIndexFactory.scanIndexDescriptors();
      case TYPE_SYSTEM:
        return TypeSystemDescriptionFactory.scanTypeDescriptors();
      default:
        return new String[0];
    }
  }
}
