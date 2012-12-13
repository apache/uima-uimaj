/*
 Copyright 2009-2010	Regents of the University of Colorado.
 All rights reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package org.uimafit.factory;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.analysis_engine.impl.TypeOrFeature_impl;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.impl.Capability_impl;
import org.uimafit.descriptor.SofaCapability;
import org.uimafit.descriptor.TypeCapability;

/**
 * @author Philip Ogren
 */

public final class CapabilityFactory {
	private CapabilityFactory() {
		// This class is not meant to be instantiated
	}

	/**
	 * Creates a single capability consisting of the information in the {@link SofaCapability} and
	 * {@link TypeCapability} annotations for the class.
	 */
	public static Capability createCapability(Class<?> componentClass) {
		if (!componentClass.isAnnotationPresent(SofaCapability.class)
				&& !componentClass.isAnnotationPresent(TypeCapability.class)) {
			return null;
		}

		Capability capability = new Capability_impl();

		if (componentClass.isAnnotationPresent(SofaCapability.class)) {
			SofaCapability annotation = componentClass.getAnnotation(SofaCapability.class);
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

		if (componentClass.isAnnotationPresent(TypeCapability.class)) {
			TypeCapability annotation = componentClass.getAnnotation(TypeCapability.class);
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
		}
		else {
			List<TypeOrFeature> typesOrFeatures = new ArrayList<TypeOrFeature>();
			for (String name : typesOrFeatureNames) {
				TypeOrFeature tof = new TypeOrFeature_impl();
				tof.setName(name);
				if (name.indexOf(":") == -1) {
					tof.setType(true);
				}
				else {
					tof.setType(false);
				}
				typesOrFeatures.add(tof);
			}
			return typesOrFeatures.toArray(new TypeOrFeature[typesOrFeatures.size()]);
		}

	}
}
