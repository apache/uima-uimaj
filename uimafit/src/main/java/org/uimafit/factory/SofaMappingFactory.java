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

import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.metadata.SofaMapping;
import org.apache.uima.analysis_engine.metadata.impl.SofaMapping_impl;

/**
 * @author Philip Ogren
 */
public final class SofaMappingFactory {
	private SofaMappingFactory() {
		// This class is not meant to be instantiated
	}

	/**
	 * This method creates a sofa mapping which is useful for mapping view names in individual
	 * components used in aggregate analysis engines to the view names used by the aggregate.
	 * 
	 * WARNING: in version 0.9.12 the ordering of the parameters was changed! The order used to be
	 * aggregateSofaName, componentKey, componentSofaName. This was changed because it seemed an
	 * unnatural ordering.
	 * 
	 * @param componentKey
	 *            the key/name used by the aggregate analysis engine for the component whose view is
	 *            being mapped.
	 * @param componentSofaName
	 *            the sofa name used by the the component
	 * @param aggregateSofaName
	 *            the view name that the component name is mapped to and used by the aggregate
	 *            analysis engine
	 * @return a sofa mapping with the componentSofaName mapped to the aggregateSofaName
	 */
	public static SofaMapping createSofaMapping(String componentKey, String componentSofaName,
			String aggregateSofaName) {

		SofaMapping sofaMapping = new SofaMapping_impl();
		sofaMapping.setComponentKey(componentKey);
		sofaMapping.setComponentSofaName(componentSofaName);
		sofaMapping.setAggregateSofaName(aggregateSofaName);
		return sofaMapping;
	}

	/**
	 * create a sofa mapping using the component class rather than the component name
	 */
	public static SofaMapping createSofaMapping(Class<? extends AnalysisComponent> componentClass,
			String componentSofaName, String aggregateSofaName) {

		return createSofaMapping(componentClass.getName(), componentSofaName, aggregateSofaName);
	}

}
