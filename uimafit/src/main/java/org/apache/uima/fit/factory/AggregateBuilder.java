/*
 Copyright 2010 Regents of the University of Colorado.
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

package org.apache.uima.fit.factory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.metadata.SofaMapping;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;

/**
 * This builder makes it easier to create an aggregate analysis engine. A typical use-case would
 * involve initializing this builder with your preferred type system and type priorities (the latter
 * may be null). This is followed by adding analysis engine descriptions one at a time until done.
 * This makes it easy to have runtime decisions determine how the aggregate engine should be built.
 * Finally, one of the create methods are called and an AnalysisEngine or AnalysisEngineDescription
 * is returned.
 *
 * <p>This is an example taken from our test cases:</p>
 *
 * <p><blockquote><pre>
 * import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;
 *
 * AggregateBuilder builder = new AggregateBuilder();
 * builder.add(createPrimitiveDescription(Annotator1.class, typeSystemDescription),
 *     ViewNames.PARENTHESES_VIEW, "A");
 * builder.add(createPrimitiveDescription(Annotator2.class, typeSystemDescription),
 *     ViewNames.SORTED_VIEW, "B",
 *     ViewNames.SORTED_PARENTHESES_VIEW, "C",
 *     ViewNames.PARENTHESES_VIEW, "A");
 * builder.add(createPrimitiveDescription(Annotator3.class, typeSystemDescription),
 *     ViewNames.INITIAL_VIEW, "B");
 * AnalysisEngine aggregateEngine = builder.createAggregate();
 * </pre></blockquote></p>
 *
 * @author Philip Ogren
 */
public class AggregateBuilder {

	List<String> componentNames = new ArrayList<String>();

	List<SofaMapping> sofaMappings = new ArrayList<SofaMapping>();

	List<AnalysisEngineDescription> analysisEngineDescriptions = new ArrayList<AnalysisEngineDescription>();

	TypeSystemDescription typeSystemDescription;

	TypePriorities typePriorities;

	FlowControllerDescription flowControllerDescription;

	/**
	 * The default no-args constructor calls
	 * {@link AggregateBuilder#AggregateBuilder(TypeSystemDescription, TypePriorities, FlowControllerDescription)}
	 * with null-valued args.
	 */
	public AggregateBuilder() {
		this(null, null, null);
	}

	/**
	 * Instantiate an AggregateBuilder with a given type system, type priorities, and flow
	 * controller. Generally, speaking it suffices to use the no arguments constructor
	 *
	 * @param typeSystemDescription
	 *            this can be instantiated using {@link TypeSystemDescriptionFactory}
	 * @param typePriorities
	 *            this can be instantiated using {@link TypePrioritiesFactory}
	 * @param flowControllerDescription
	 *            this can be instantiated using {@link FlowControllerFactory}
	 */
	public AggregateBuilder(TypeSystemDescription typeSystemDescription,
			TypePriorities typePriorities, FlowControllerDescription flowControllerDescription) {
		this.typeSystemDescription = typeSystemDescription;
		this.typePriorities = typePriorities;
		this.flowControllerDescription = flowControllerDescription;
	}

	/**
	 * This method simply calls {@link #add(String, AnalysisEngineDescription, String...)} using the
	 * result of {@link AnalysisEngineDescription#getAnnotatorImplementationName()} for the
	 * component name
	 *
	 * @return the name of the component generated for the {@link AnalysisEngineDescription}
	 */
	public String add(AnalysisEngineDescription aed, String... viewNames) {
		String componentName = aed.getAnalysisEngineMetaData().getName();
		if (componentName == null || componentName.equals("")) {
			if (aed.isPrimitive()) {
				componentName = aed.getAnnotatorImplementationName();
			}
			else {
				componentName = "aggregate";
			}
		}
		if (componentNames.contains(componentName)) {
			componentName = componentName + "." + (componentNames.size() + 1);
		}
		add(componentName, aed, viewNames);
		return componentName;
	}

	/**
	 * @param componentName
	 *            the name of the component to add
	 * @param aed
	 *            an analysis engine description to add to the aggregate analysis engine
	 * @param viewNames
	 *            pairs of view names corresponding to a componentSofaName followed by the
	 *            aggregateSofaName that it is mapped to. An even number of names must be passed in
	 *            or else an IllegalArgumentException will be thrown. See
	 *            {@link SofaMappingFactory#createSofaMapping(String, String, String)}
	 */
	public void add(String componentName, AnalysisEngineDescription aed, String... viewNames) {
		if (componentNames.contains(componentName)) {
			throw new IllegalArgumentException("the component name '" + componentName
					+ "' has already been used for another added analysis engine description.");
		}
		if (viewNames != null && viewNames.length % 2 != 0) {
			throw new IllegalArgumentException(
					"an even number of view names is required (as "
							+ "component view name, aggregate view name pairs) for the AggregateBuilder.add "
							+ "method. " + viewNames.length + " view names passed: "
							+ Arrays.asList(viewNames));
		}

		analysisEngineDescriptions.add(aed);
		componentNames.add(componentName);

		if (viewNames != null) {
			for (int i = 0; i < viewNames.length; i += 2) {
				sofaMappings.add(SofaMappingFactory.createSofaMapping(componentName, viewNames[i],
						viewNames[i + 1]));
			}
		}
	}

	/**
	 * Provide a sofa mapping for a component from the component's view to the aggregate view.
	 *
	 * @param componentName
	 *            the name of the component
	 * @param componentViewName
	 *            the name of the component view
	 * @param aggregateViewName
	 *            the name of the aggregate view to map the component view to.
	 */
	public void addSofaMapping(String componentName, String componentViewName,
			String aggregateViewName) {
		if (componentNames.contains(componentName)) {
			sofaMappings.add(SofaMappingFactory.createSofaMapping(componentName, componentViewName,
					aggregateViewName));
		}
		else {
			throw new IllegalArgumentException("No component with the name '" + componentName
					+ "' has been added to this builder.  Sofa mappings may only be added for "
					+ "components that have been added to this builder. ");
		}
	}

	/**
	 * Set the flow controller description of the aggregate engine created by this builder.
	 *
	 * @param flowControllerDescription
	 *            see {@link FlowControllerFactory}
	 */
	public void setFlowControllerDescription(FlowControllerDescription flowControllerDescription) {
		this.flowControllerDescription = flowControllerDescription;
	}

	/**
	 * This method simply delegates to
	 * {@link AnalysisEngineFactory#createAggregate(List, TypeSystemDescription, TypePriorities, SofaMapping[], Object...)}
	 * with the data collected by this builder.
	 *
	 * @return an aggregate analysis engine
	 */
	public AnalysisEngine createAggregate() throws ResourceInitializationException {
		return AnalysisEngineFactory.createAggregate(analysisEngineDescriptions, componentNames,
				typeSystemDescription, typePriorities,
				sofaMappings.toArray(new SofaMapping[sofaMappings.size()]),
				flowControllerDescription);
	}

	/**
	 * This method simply delegates to
	 * {@link AnalysisEngineFactory#createAggregateDescription(List, TypeSystemDescription, TypePriorities, SofaMapping[], Object...)}
	 * with the data collected by this builder.
	 *
	 * @return a description of an aggregate analysis engine
	 */
	public AnalysisEngineDescription createAggregateDescription()
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createAggregateDescription(analysisEngineDescriptions,
				componentNames, typeSystemDescription, typePriorities,
				sofaMappings.toArray(new SofaMapping[sofaMappings.size()]),
				flowControllerDescription);
	}
}
