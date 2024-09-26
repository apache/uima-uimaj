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

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.analysis_engine.impl.TypeOrFeature_impl;
import org.apache.uima.fit.descriptor.LanguageCapability;
import org.apache.uima.fit.descriptor.MimeTypeCapability;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.internal.ReflectionUtil;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.impl.Capability_impl;

/**
 */

public final class CapabilityFactory {
  private CapabilityFactory() {
    // This class is not meant to be instantiated
  }

  /**
   * Creates a single capability consisting of the information in the {@link SofaCapability} and
   * {@link TypeCapability} annotations for the class.
   * 
   * @param componentClass
   *          a class with capability annotations
   * @return capabilities extracted from the class
   */
  public static Capability createCapability(Class<?> componentClass) {
    boolean sofaCapabilityPresent = ReflectionUtil.isAnnotationPresent(componentClass,
            SofaCapability.class);
    boolean typeCapabilityPresent = ReflectionUtil.isAnnotationPresent(componentClass,
            TypeCapability.class);
    boolean mimeTypeCapabilityPresent = ReflectionUtil.isAnnotationPresent(componentClass,
            MimeTypeCapability.class);
    boolean languageCapabilityPresent = ReflectionUtil.isAnnotationPresent(componentClass,
            LanguageCapability.class);

    // Skip if no capability annotations are present at all
    if (!sofaCapabilityPresent && !typeCapabilityPresent && !mimeTypeCapabilityPresent
            && !languageCapabilityPresent) {
      return null;
    }

    Capability capability = new Capability_impl();

    if (languageCapabilityPresent) {
      LanguageCapability annotation = ReflectionUtil.getAnnotation(componentClass,
              LanguageCapability.class);
      String[] languages = annotation.value();
      if (languages.length == 1 && languages[0].equals(LanguageCapability.NO_DEFAULT_VALUE)) {
        languages = new String[0];
      }
      capability.setLanguagesSupported(languages);
    }

    if (mimeTypeCapabilityPresent) {
      MimeTypeCapability annotation = ReflectionUtil.getAnnotation(componentClass,
              MimeTypeCapability.class);
      String[] mimeTypes = annotation.value();
      if (mimeTypes.length == 1 && mimeTypes[0].equals(MimeTypeCapability.NO_DEFAULT_VALUE)) {
        mimeTypes = new String[0];
      }
      capability.setMimeTypesSupported(mimeTypes);
    }

    if (sofaCapabilityPresent) {
      SofaCapability annotation = ReflectionUtil.getAnnotation(componentClass,
              SofaCapability.class);
      String[] inputSofas = annotation.inputSofas();
      if (inputSofas.length == 1 && inputSofas[0].equals(SofaCapability.NO_DEFAULT_VALUE)) {
        inputSofas = new String[0];
      }
      capability.setInputSofas(inputSofas);

      String[] outputSofas = annotation.outputSofas();
      if (outputSofas.length == 1 && outputSofas[0].equals(SofaCapability.NO_DEFAULT_VALUE)) {
        outputSofas = new String[0];
      }
      capability.setOutputSofas(outputSofas);
    }

    if (typeCapabilityPresent) {
      TypeCapability annotation = ReflectionUtil.getAnnotation(componentClass,
              TypeCapability.class);
      String[] inputTypesOrFeatureNames = annotation.inputs();
      capability.setInputs(createTypesOrFeatures(inputTypesOrFeatureNames));
      String[] outputTypesOrFeatureNames = annotation.outputs();
      capability.setOutputs(createTypesOrFeatures(outputTypesOrFeatureNames));
    }

    return capability;
  }

  private static TypeOrFeature[] createTypesOrFeatures(String[] typesOrFeatureNames) {
    if (typesOrFeatureNames.length == 1
            && typesOrFeatureNames[0].equals(TypeCapability.NO_DEFAULT_VALUE)) {
      return new TypeOrFeature[0];
    } else {
      List<TypeOrFeature> typesOrFeatures = new ArrayList<TypeOrFeature>();
      for (String name : typesOrFeatureNames) {
        TypeOrFeature tof = new TypeOrFeature_impl();
        tof.setName(name);
        if (name.indexOf(":") == -1) {
          tof.setType(true);
        } else {
          tof.setType(false);
        }
        typesOrFeatures.add(tof);
      }
      return typesOrFeatures.toArray(new TypeOrFeature[typesOrFeatures.size()]);
    }

  }
}
