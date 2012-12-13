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

package org.uimafit.examples.tutorial.ex6;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createAnalysisEngineDescription;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.bindResource;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.util.JCasUtil.select;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.examples.tutorial.type.Meeting;
import org.uimafit.examples.tutorial.type.UimaMeeting;

/**
 * Example annotator that iterates over Meeting annotations and annotates a
 * meeting as a UimaMeeting if a UIMA acronym occurs in close proximity to that
 * meeting. When combined in an aggregate TAE with the UimaAcronymAnnotator,
 * demonstrates the use of the ResourceManager to share data between annotators.
 * 
 * @author unknown
 */
@TypeCapability(inputs = "org.apache.uima.tutorial.Meeting", outputs = "org.apache.uima.tutorial.UimaMeeting")
public class UimaMeetingAnnotator extends JCasAnnotator_ImplBase {
	static final String RESOURCE_UIMA_TERM_TABLE = "UimaTermTable";

	@ExternalResource(key = RESOURCE_UIMA_TERM_TABLE)
	private StringMapResource mMap;

	/**
	 * @see AnalysisComponent#initialize(UimaContext)
	 */
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		try {
			// get a reference to the String Map Resource
			mMap = (StringMapResource) getContext().getResourceObject("UimaTermTable");
		}
		catch (ResourceAccessException e) {
			throw new ResourceInitializationException(e);
		}
	}

	/**
	 * @see JCasAnnotator_ImplBase#process(JCas)
	 */
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		// get document text
		String text = aJCas.getDocumentText();

		// We iterate over all Meeting annotations, and if we determine that
		// the topic of a meeting is UIMA-related, we create a UimaMeeting
		// annotation. We add each UimaMeeting annotation to a list, and then
		// later go back and add these to the CAS indexes. We need to do this
		// because it's not allowed to add to an index that you're currently
		// iterating over.
		List<UimaMeeting> uimaMeetings = new ArrayList<UimaMeeting>();

		for (Meeting meeting : select(aJCas, Meeting.class)) {
			// get span of text within 50 chars on either side of meeting
			// (window size should probably be a config. param)
			int begin = meeting.getBegin() - 50;
			int end = meeting.getEnd() + 50;
			if (begin < 0) {
				begin = 0;
			}
			if (end > text.length()) {
				end = text.length();
			}
			String window = text.substring(begin, end);

			// look for UIMA acronyms within this window
			StringTokenizer tokenizer = new StringTokenizer(window, " \t\n\r.<.>/?\";:[{]}\\|=+()!");
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				// look up token in map to see if it is an acronym
				if (mMap.get(token) != null) {
					// create annotation
					UimaMeeting annot = new UimaMeeting(aJCas, meeting.getBegin(), meeting.getEnd());
					annot.setRoom(meeting.getRoom());
					annot.setDate(meeting.getDate());
					annot.setStartTime(meeting.getStartTime());
					annot.setEndTime(meeting.getEndTime());
					// Add annotation to a list, to be later added to the
					// indexes.
					// We need to do this because it's not allowed to add to an
					// index that you're currently iterating over.
					uimaMeetings.add(annot);
					break;
				}
			}
		}

		for (UimaMeeting meeting : uimaMeetings) {
			meeting.addToIndexes();
		}
	}

	public static void main(String[] args) throws Exception {
		File outputDirectory = new File("src/main/resources/org/uimafit/tutorial/ex6/");
		outputDirectory.mkdirs();

		TypeSystemDescription tsd = createTypeSystemDescription("org.uimafit.tutorial.type.TypeSystem");
		AnalysisEngineDescription aed = createPrimitiveDescription(UimaMeetingAnnotator.class, tsd);

		aed.toXML(new FileOutputStream(new File(outputDirectory, "UimaMeetingAnnotator.xml")));

		AggregateBuilder builder = new AggregateBuilder();
		builder.add(createAnalysisEngineDescription("org.uimafit.tutorial.ex6.UimaAcronymAnnotator"));
		builder.add(createAnalysisEngineDescription("org.uimafit.tutorial.ex6.UimaMeetingAnnotator"));
		AnalysisEngineDescription aggregate = builder.createAggregateDescription();

		ExternalResourceDescription erd = createExternalResourceDescription("UimaAcronymTableFile", StringMapResource_impl.class,
				"file:org/uimafit/tutorial/ex6/uimaAcronyms.txt");

		// bindResource(aggregate,
		// UimaAcronymAnnotator.class.getName()+"/"+UimaAcronymAnnotator.RESOURCE_ACRONYM_TABLE,
		// erd);
		bindResource(aggregate, RESOURCE_UIMA_TERM_TABLE, erd); // UimaMeetingAnnotator.class.getName()+"/"+

		// bindResource(aggregate, "UimaAcronymTableFile", erd);
		// bindResource(aggregate, RESOURCE_UIMA_TERM_TABLE, erd);

		aggregate.toXML(new FileOutputStream(new File(outputDirectory, "UimaMeetingDetectorTAE.xml")));
	}
}
